"""Reject incomplete or incorrectly targeted libbox AARs, including cached ones."""

import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile

ABIS = {"armeabi-v7a", "arm64-v8a", "x86", "x86_64"}
ANDROID = "{http://schemas.android.com/apk/res/android}"


def check(path, minimum):
    with ZipFile(path) as archive:
        broken = archive.testzip()
        if broken:
            raise ValueError(f"{path}: corrupt entry {broken}")
        for entry in ("classes.jar", *(f"jni/{abi}/libbox.so" for abi in sorted(ABIS))):
            if archive.getinfo(entry).file_size == 0:
                raise ValueError(f"{path}: empty {entry}")
        manifest = ET.fromstring(archive.read("AndroidManifest.xml"))
        sdk = manifest.find("uses-sdk")
        if sdk is None or sdk.get(f"{ANDROID}minSdkVersion") != str(minimum):
            raise ValueError(f"{path}: expected minSdkVersion {minimum}")
    print(f"Verified {path.name}: API {minimum}, all four ABIs")


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[2]
    check(root / "app/libs/libbox.aar", 24)
    check(root / "app/libs/libbox-legacy.aar", 21)
