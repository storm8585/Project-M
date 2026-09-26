import os
from pathlib import Path

PLATFORMS = {
    "armeabi-v7a": "android/arm",
    "arm64-v8a": "android/arm64",
    "x86": "android/386",
    "x86_64": "android/amd64",
}


def load_config(root):
    properties = {}
    for line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith(("#", "!")):
            key, value = line.split("=", 1)
            if key.strip() in properties:
                raise ValueError(f"Duplicate Gradle property: {key.strip()}")
            properties[key.strip()] = value.strip()
    abis = tuple(value.strip() for value in properties.get("buildAbis", "").split(","))
    if not abis or any(abi not in PLATFORMS for abi in abis) or len(set(abis)) != len(abis):
        raise ValueError("buildAbis must contain unique supported Android ABIs")
    universal = properties.get("buildUniversalApk", "")
    if universal not in {"true", "false"}:
        raise ValueError("buildUniversalApk must be true or false")
    return abis, universal == "true"


def build_outputs(root):
    abis, _ = load_config(root)
    return {"abis": ",".join(abis), "platforms": ",".join(PLATFORMS[abi] for abi in abis)}


if __name__ == "__main__":
    outputs = build_outputs(Path(__file__).resolve().parents[2])
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write("".join(f"{key}={value}\n" for key, value in outputs.items()))
