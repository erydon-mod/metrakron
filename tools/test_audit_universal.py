"""Focused classless-bundle regression tests; all ZIP/JAR fixtures stay in memory."""
import io
from pathlib import Path
import unittest
from unittest.mock import patch
import zipfile

from tools import audit_universal as audit


def archive_bytes(entries):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        for name, contents in entries.items():
            archive.writestr(zipfile.ZipInfo(name, audit.FIXED_ZIP_TIME), contents)
    return buffer.getvalue()


class ClasslessOuterBundleTest(unittest.TestCase):
    def test_classless_outer_accepts_real_nested_jar_bytes_with_classes(self):
        nested = archive_bytes({
            "fabric.mod.json": b'{"id":"metrakron"}',
            "com/oliver/metrakron/Example.class": bytes.fromhex("cafebabe0000003d"),
        })
        outer = archive_bytes({
            "fabric.mod.json": b'{"id":"metrakron_bundle"}',
            "META-INF/jars/metrakron-adapter.jar": nested,
        })
        with zipfile.ZipFile(io.BytesIO(outer)) as bundle:
            audit.require_classless_outer(bundle)
            # This positive control really contains a class in a nested ZIP.
            with zipfile.ZipFile(io.BytesIO(bundle.read("META-INF/jars/metrakron-adapter.jar"))) as adapter:
                self.assertIn("com/oliver/metrakron/Example.class", adapter.namelist())

    def test_actual_audit_rejects_loose_classes_in_every_outer_location(self):
        locations = (
            "Unexpected.class",
            "com/oliver/metrakron/Unexpected.class",
            "assets/metrakron/Unexpected.class",
            "META-INF/versions/17/Unexpected.class",
            "META-INF/jars/Unexpected.class",
            "META-INF/jars/subdirectory/Unexpected.class",
            "META-INF/jars/adapter.jar/Unexpected.class",
        )
        for name in locations:
            with self.subTest(entry=name):
                payload = archive_bytes({name: bytes.fromhex("cafebabe0000003d")})
                with zipfile.ZipFile(io.BytesIO(payload)) as bundle:
                    # Supply an in-memory archive at the file boundary. The real audit
                    # must reject its class before unrelated metadata/assets are read.
                    with patch.object(Path, "is_file", return_value=True), \
                            patch.object(audit.zipfile, "ZipFile", return_value=bundle):
                        with self.assertRaisesRegex(ValueError, "classless outer bundle unexpectedly contains classes") as error:
                            audit.audit(Path("in-memory-bundle.jar"), "1.1.0")
                self.assertIn(name, str(error.exception))

    def test_guard_allows_resource_names_that_only_contain_class_as_a_directory(self):
        payload = archive_bytes({"documentation.class/example.txt": b"plain resource"})
        with zipfile.ZipFile(io.BytesIO(payload)) as bundle:
            audit.require_classless_outer(bundle)


if __name__ == "__main__":
    unittest.main()
