"""Verify Gradle output metadata and signatures, then collect distributable APKs."""

import hashlib
import json
import os
import shutil
import subprocess
from pathlib import Path

ABIS = {"armeabi-v7a", "arm64-v8a", "x86", "x86_64", "universal"}


def collect(root, env):
    flavor, build_type = env["BUILD_FLAVOR"], env["BUILD_TYPE"]
    if flavor not in {"other", "otherLegacy"} or build_type not in {"Debug", "Release"}:
        raise ValueError("Unexpected Android variant")
    props = dict(line.split("=", 1) for line in (root / "version.properties").read_text().splitlines()
                 if "=" in line and not line.startswith("#"))
    apk_dir = root / "app/build/outputs/apk" / flavor / build_type.lower()
    metadata = json.loads((apk_dir / "output-metadata.json").read_text())
    if metadata["applicationId"] != "io.nekohasekai.sfa":
        raise ValueError("Unexpected application ID")
    apks, seen = [], set()
    for element in metadata["elements"]:
        if (element["versionCode"] != int(props["VERSION_CODE"])
                or element["versionName"] != props["VERSION_NAME"]):
            raise ValueError("APK metadata does not match the generated version")
        filters = element["filters"]
        abi = filters[0]["value"] if len(filters) == 1 and filters[0]["filterType"] == "ABI" else "universal"
        if (filters and abi == "universal") or abi not in ABIS or abi in seen:
            raise ValueError("Unexpected or duplicate APK filter")
        seen.add(abi)
        name = element["outputFile"]
        if Path(name).name != name or not name.endswith(".apk"):
            raise ValueError("Unexpected APK filename")
        apk = apk_dir / name
        if not apk.is_file() or apk.stat().st_size == 0:
            raise ValueError(f"Missing APK: {name}")
        apks.append((apk, abi))
    if seen != ABIS:
        raise ValueError("Expected four ABI-specific APKs and one universal APK")

    verifier = Path(env["ANDROID_HOME"]) / "build-tools/36.0.0/apksigner"
    for apk, _ in apks:
        subprocess.run([str(verifier), "verify", "--verbose", str(apk)], check=True)

    dist = root / "dist"
    dist.mkdir(exist_ok=True)
    checksums, artifacts = [], []
    for apk, abi in sorted(apks):
        destination = dist / apk.name
        shutil.copyfile(apk, destination)
        with destination.open("rb") as stream:
            digest = hashlib.file_digest(stream, "sha256").hexdigest()
        checksums.append(f"{digest}  {apk.name}\n")
        artifacts.append({"file": apk.name, "abi": abi, "sha256": digest, "size": apk.stat().st_size})
    (dist / "SHA256SUMS").write_text("".join(checksums), encoding="utf-8")
    (dist / "build-metadata.json").write_text(json.dumps({
        "version_name": props["VERSION_NAME"], "version_code": int(props["VERSION_CODE"]),
        "application_id": metadata["applicationId"], "flavor": flavor, "build_type": build_type,
        "app_commit": env["GITHUB_SHA"], "core_commit": env["CORE_REF"],
        "go_version": props["GO_VERSION"], "run_id": env["GITHUB_RUN_ID"],
        "run_attempt": env["GITHUB_RUN_ATTEMPT"],
        "build_url": f"{env['GITHUB_SERVER_URL']}/{env['GITHUB_REPOSITORY']}/actions/runs/{env['GITHUB_RUN_ID']}",
        "apks": artifacts,
    }, indent=2) + "\n", encoding="utf-8")
    with open(env["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
        summary.write(f"\n### {flavor} / {build_type}\n\n"
                      f"{len(apks)} signed APKs verified; download the matching SFA artifact. "
                      "Use the universal APK if unsure of your device ABI.\n")


if __name__ == "__main__":
    collect(Path(__file__).resolve().parents[2], os.environ)
