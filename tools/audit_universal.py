#!/usr/bin/env python3
"""Strict, standard-library audit for the deterministic Metrakron bundle."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import re
import struct
import sys
import zipfile
from pathlib import Path


EXPECTED = {
    "1.18.2": {"java": 17, "loader": "0.16.10"},
    "1.19.2": {"java": 17, "loader": "0.16.10"},
    "1.19.4": {"java": 17, "loader": "0.16.10"},
    "1.20.1": {"java": 17, "loader": "0.16.10"},
    "1.20.4": {"java": 17, "loader": "0.16.10"},
    "1.20.6": {"java": 21, "loader": "0.16.10"},
    "1.21.1": {"java": 21, "loader": "0.16.10"},
    "1.21.4": {"java": 21, "loader": "0.16.10"},
    "1.21.8": {"java": 21, "loader": "0.16.13"},
    "1.21.11": {"java": 21, "loader": "0.17.3"},
    "26.1.2": {"java": 25, "loader": "0.18.4"},
    "26.2": {"java": 25, "loader": "0.19.3"},
}
OUTER_LOADER_MINIMUM = "0.16.10"
JAVA_CLASS_MAJOR = {17: 61, 21: 65, 25: 69}
FIXED_ZIP_TIME = (1980, 2, 1, 0, 0, 0)
ALLOWED_ADAPTER_ZIP_TIMES = {
    (1980, 1, 1, 0, 0, 0),
    FIXED_ZIP_TIME,
}
EXPECTED_CONTACT = {
    "homepage": "https://erydon.co.uk",
    "sources": "https://github.com/erydon-mod/metrakron",
    "issues": "https://github.com/erydon-mod/metrakron/issues",
}
REQUIRED_LEGAL_ENTRIES = {
    "LICENSE",
    "LICENSES/CC-BY-SA-4.0.txt",
    "LICENSES/OFL-1.1.txt",
    "THIRD_PARTY_NOTICES.md",
    "ASSET_PROVENANCE.md",
}
REQUIRED_INNER_ENTRIES = {
    "fabric.mod.json",
    "metrakron.mixins.json",
    "com/oliver/metrakron/MetrakronClient.class",
    "com/oliver/metrakron/mixin/ReadyScreenMixin.class",
    "com/oliver/metrakron/overlay/DesktopOverlayMain.class",
    "assets/metrakron/font/cinzel.ttf",
    "assets/metrakron/textures/gui/cinzel_atlas.png",
    "assets/metrakron/textures/gui/cinzel_timer_atlas.png",
    "assets/metrakron/textures/gui/cinzel_timer_atlas.properties",
    "assets/metrakron/textures/gui/nerium_panel.png",
    "assets/metrakron/textures/gui/bronze_matte.png",
} | REQUIRED_LEGAL_ENTRIES


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def json_entry(archive: zipfile.ZipFile, name: str) -> dict:
    return json.loads(archive.read(name).decode("utf-8"))


def class_major(data: bytes, name: str) -> int:
    require(len(data) >= 8, f"Truncated class file: {name}")
    magic, _minor, major = struct.unpack(">IHH", data[:8])
    require(magic == 0xCAFEBABE, f"Invalid class header: {name}")
    return major


def audit_adapter(
    data: bytes,
    expected_version: str,
    minecraft: str,
    java: int,
    loader: str,
) -> dict:
    with zipfile.ZipFile(io.BytesIO(data)) as adapter:
        listed_names = adapter.namelist()
        require(len(listed_names) == len(set(listed_names)), f"{minecraft} adapter has duplicate ZIP entries")
        require(
            all(info.date_time in ALLOWED_ADAPTER_ZIP_TIMES for info in adapter.infolist()),
            f"{minecraft} adapter has non-deterministic ZIP timestamps",
        )
        names = set(listed_names)
        missing = sorted(REQUIRED_INNER_ENTRIES - names)
        require(not missing, f"{minecraft} adapter is missing: {', '.join(missing)}")
        require(
            "com/oliver/metrakron/mixin/TitleScreenMixin.class" not in names,
            f"{minecraft} contains the retired title-only startup hook",
        )

        metadata = json_entry(adapter, "fabric.mod.json")
        require(metadata.get("id") == "metrakron", f"{minecraft} has the wrong mod id")
        require(metadata.get("version") == expected_version, f"{minecraft} has the wrong mod version")
        require(metadata.get("contact") == EXPECTED_CONTACT, f"{minecraft} has incomplete public contact links")
        require(
            "metrakron_universal_adapter" in metadata.get("provides", []),
            f"{minecraft} does not provide the universal adapter alias",
        )
        depends = metadata.get("depends", {})
        require(depends.get("minecraft") == minecraft, f"{minecraft} has a non-exact Minecraft dependency")
        require(depends.get("java") == f">={java}", f"{minecraft} has the wrong Java dependency")
        require(
            depends.get("fabricloader") == f">={loader}",
            f"{minecraft} has the wrong Loader dependency",
        )
        require("fabric-api" in depends, f"{minecraft} does not require Fabric API")

        mixins = json_entry(adapter, "metrakron.mixins.json")
        require("ReadyScreenMixin" in mixins.get("client", []), f"{minecraft} omits ReadyScreenMixin")
        require(
            mixins.get("compatibilityLevel") == f"JAVA_{java}",
            f"{minecraft} mixin Java level is incorrect",
        )

        class_versions = [
            class_major(adapter.read(name), name)
            for name in names
            if name.endswith(".class")
        ]
        require(class_versions, f"{minecraft} contains no classes")
        maximum = max(class_versions)
        require(
            maximum <= JAVA_CLASS_MAJOR[java],
            f"{minecraft} contains class major {maximum}, above Java {java}",
        )
        return {
            "minecraft": minecraft,
            "java": java,
            "loader": loader,
            "classes": len(class_versions),
            "maximumClassMajor": maximum,
        }


def project_version() -> str:
    properties = Path(__file__).resolve().parents[1] / "stonecutter.properties.toml"
    match = re.search(r'^mod\.version\s*=\s*"([^"]+)"\s*$', properties.read_text(encoding="utf-8"), re.MULTILINE)
    require(match is not None, "Missing mod.version in stonecutter.properties.toml")
    return match.group(1)


def audit(bundle_path: Path, expected_version: str | None = None) -> dict:
    expected_version = expected_version or project_version()
    require(bundle_path.is_file(), f"Bundle does not exist: {bundle_path}")
    with zipfile.ZipFile(bundle_path) as bundle:
        names = bundle.namelist()
        require(len(names) == len(set(names)), "Bundle contains duplicate ZIP entries")
        require(
            all(info.date_time == FIXED_ZIP_TIME for info in bundle.infolist()),
            "Bundle contains non-deterministic ZIP timestamps",
        )
        require(
            not any(name.endswith(".class") and not name.startswith("META-INF/jars/") for name in names),
            "The classless outer bundle unexpectedly contains classes",
        )
        missing_legal = sorted(REQUIRED_LEGAL_ENTRIES - set(names))
        require(not missing_legal, f"Outer bundle is missing: {', '.join(missing_legal)}")

        metadata = json_entry(bundle, "fabric.mod.json")
        manifest = json_entry(bundle, "META-INF/metrakron/adapters.json")
        version = metadata.get("version")
        require(metadata.get("id") == "metrakron_bundle", "Outer mod id is incorrect")
        require(version == expected_version, f"Outer version must be {expected_version}")
        require(metadata.get("contact") == EXPECTED_CONTACT, "Outer bundle has incomplete public contact links")
        require(manifest.get("metrakronVersion") == version, "Outer and manifest versions differ")
        require(metadata.get("depends", {}).get("minecraft") == list(EXPECTED), "Outer Minecraft union differs")
        require(
            metadata.get("depends", {}).get("fabricloader") == f">={OUTER_LOADER_MINIMUM}",
            "Outer Loader minimum is wrong",
        )
        require(
            metadata.get("depends", {}).get("metrakron_universal_adapter") == version,
            "Outer adapter dependency is not exact",
        )

        adapters = manifest.get("adapters", [])
        require(len(adapters) == len(EXPECTED), "Manifest has the wrong adapter count")
        require(
            [adapter.get("minecraft") for adapter in adapters] == list(EXPECTED),
            "Manifest adapters are missing, duplicated, or out of deterministic order",
        )
        declared_paths = [item.get("file") for item in metadata.get("jars", [])]
        manifest_paths = [item.get("file") for item in adapters]
        require(declared_paths == manifest_paths, "Outer and audit-manifest nested paths differ")
        actual_nested_paths = sorted(
            name for name in names if name.startswith("META-INF/jars/") and name.endswith(".jar")
        )
        require(
            actual_nested_paths == sorted(manifest_paths),
            "Bundle contains missing or undeclared nested adapter JARs",
        )

        audited = []
        for item in adapters:
            minecraft = item["minecraft"]
            expected = EXPECTED[minecraft]
            java = expected["java"]
            loader = expected["loader"]
            require(item.get("java") == java, f"{minecraft} manifest Java level is wrong")
            require(item.get("loader") == loader, f"{minecraft} manifest Loader minimum is wrong")
            nested_path = item["file"]
            require(nested_path in names, f"Missing nested adapter {nested_path}")
            data = bundle.read(nested_path)
            digest = hashlib.sha256(data).hexdigest()
            require(digest == item.get("sha256"), f"{minecraft} SHA-256 does not match its manifest")
            result = audit_adapter(data, version, minecraft, java, loader)
            result["sha256"] = digest
            audited.append(result)

    return {
        "bundle": str(bundle_path.resolve()),
        "sha256": hashlib.sha256(bundle_path.read_bytes()).hexdigest(),
        "version": version,
        "adapters": audited,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("bundle", type=Path)
    parser.add_argument("--expected-version", help="Expected release version; defaults to the project version")
    args = parser.parse_args()
    try:
        result = audit(args.bundle, args.expected_version)
    except (OSError, KeyError, ValueError, zipfile.BadZipFile, json.JSONDecodeError) as error:
        print(f"Universal bundle audit failed: {error}", file=sys.stderr)
        return 1
    print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
