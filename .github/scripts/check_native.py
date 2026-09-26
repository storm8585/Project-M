"""Reject incomplete or incorrectly targeted libbox AARs, including cached ones."""

import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile

from abis import load_config

ANDROID = "{http://schemas.android.com/apk/res/android}"


def check(path, minimum, abis):
    with ZipFile(path) as archive:
        broken = archive.testzip()
        if broken:
            raise ValueError(f"{path}: corrupt entry {broken}")
        actual_abis = {
            parts[1] for name in archive.namelist()
            if len(parts := name.split("/")) == 3 and parts[0] == "jni" and parts[2] == "libbox.so"
        }
        if actual_abis != set(abis):
            raise ValueError(f"{path}: expected native ABIs {sorted(abis)}, got {sorted(actual_abis)}")
        for entry in ("classes.jar", *(f"jni/{abi}/libbox.so" for abi in sorted(abis))):
            if archive.getinfo(entry).file_size == 0:
                raise ValueError(f"{path}: empty {entry}")
        manifest = ET.fromstring(archive.read("AndroidManifest.xml"))
        sdk = manifest.find("uses-sdk")
        if sdk is None or sdk.get(f"{ANDROID}minSdkVersion") != str(minimum):
            raise ValueError(f"{path}: expected minSdkVersion {minimum}")
    print(f"Verified {path.name}: API {minimum}, ABIs {', '.join(sorted(abis))}")


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[2]
    abis, _ = load_config(root)
    check(root / "app/libs/libbox.aar", 24, abis)
    check(root / "app/libs/libbox-legacy.aar", 21, abis)
