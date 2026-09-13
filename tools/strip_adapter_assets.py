"""Remove shared assets only from collected universal adapters, not standalone builds."""
import os
import json
import copy
import sys
import tempfile
import zipfile
from pathlib import Path


def strip(path):
    path = Path(path)
    fd, temporary = tempfile.mkstemp(prefix=".assets-", suffix=".tmp", dir=path.parent)
    os.close(fd)
    try:
        with zipfile.ZipFile(path) as source, zipfile.ZipFile(temporary, "w") as target:
            metadata = json.loads(source.read("fabric.mod.json"))
            icon = metadata.get("icon")
            # Mod Menu resolves icons within the individual mod container, not the classpath.
            # Retain only its small icon under a local, non-resource-pack path.
            if isinstance(icon, str) and icon in source.namelist():
                info = copy.copy(source.getinfo(icon))
                data = source.read(icon)
                info.filename = "metrakron-icon.png"
                target.writestr(info, data)
                metadata["icon"] = info.filename
            for entry in source.infolist():
                if not entry.filename.startswith("assets/"):
                    data = ((json.dumps(metadata, indent=2) + "\n").encode("utf-8")
                            if entry.filename == "fabric.mod.json" else source.read(entry.filename))
                    target.writestr(entry, data)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


if __name__ == "__main__":
    strip(sys.argv[1])
