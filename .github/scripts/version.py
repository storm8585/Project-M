"""Generate workspace-only Android versions and GitHub Actions outputs (stdlib only)."""

import os
import re
import sys
from pathlib import Path

MAX_CODE = 2_100_000_000
NUMBER = r"(?:0|[1-9][0-9]*)"
SEMVER = re.compile(
    rf"({NUMBER})\.({NUMBER})\.({NUMBER})(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?"
)
SHA = re.compile(r"[0-9a-f]{40}")
REQUIRED = ("VERSION_CODE", "VERSION_NAME", "GO_VERSION", "LIBBOX_REF", "CI_VERSION_CODE_BASE")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def code_number(value, label, minimum=0):
    require(isinstance(value, str) and re.fullmatch(NUMBER, value) is not None,
            f"{label} must be a decimal integer without leading zeros")
    require(len(value) <= 10 and minimum <= int(value) <= MAX_CODE,
            f"{label} is outside the allowed version-code range")
    return int(value)


def generate(source, env):
    """Return (rewritten properties, outputs); identical source/inputs are idempotent."""
    lines = source.splitlines(keepends=True)
    props, positions = {}, {}
    for index, line in enumerate(lines):
        if "=" not in line or line.lstrip().startswith(("#", "!")):
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        if key in REQUIRED:
            require(key not in props, f"duplicate property: {key}")
            props[key], positions[key] = value.strip(), index
    for key in REQUIRED:
        require(bool(props.get(key)), f"missing property: {key}")

    base_version = props["VERSION_NAME"]
    match = SEMVER.fullmatch(base_version)
    if match is None:
        raise ValueError("VERSION_NAME must be strict SemVer without build metadata")
    major, minor, patch, prerelease = match.groups()
    if prerelease:
        require(all(not part.isdigit() or part == "0" or not part.startswith("0")
                    for part in prerelease.split(".")),
                "VERSION_NAME numeric prerelease identifiers must not have leading zeros")
    tracked = code_number(props["VERSION_CODE"], "VERSION_CODE")
    base = code_number(props["CI_VERSION_CODE_BASE"], "CI_VERSION_CODE_BASE", 1)
    run_number = code_number(env.get("GITHUB_RUN_NUMBER"), "GITHUB_RUN_NUMBER", 1)
    code = base + run_number
    require(tracked < code <= MAX_CODE,
            "generated VERSION_CODE must exceed tracked VERSION_CODE and be <= 2100000000")
    require(SHA.fullmatch(props["LIBBOX_REF"]) is not None,
            "LIBBOX_REF must be an exact 40-character lowercase hexadecimal SHA")
    source_sha = env.get("GITHUB_SHA", "")
    require(SHA.fullmatch(source_sha) is not None,
            "GITHUB_SHA must be an exact 40-character lowercase hexadecimal SHA")
    go_version = props["GO_VERSION"]
    require(re.fullmatch(r"go[0-9]+\.[0-9]+(?:\.[0-9]+)?(?:(?:beta|rc)[0-9]+)?", go_version)
            is not None, "GO_VERSION must be a Go version with a go prefix")
    ref = env.get("GITHUB_REF", "")
    require(re.fullmatch(r"refs/(?:heads|tags|pull)/[^\s\x00-\x1f\x7f]+", ref) is not None,
            "GITHUB_REF must be a nonempty heads, tags, or pull ref")
    if ref.startswith("refs/tags/v"):
        require(ref == f"refs/tags/v{base_version}", "version tag must exactly match tracked VERSION_NAME")
        name = base_version
    elif prerelease:
        name = f"{base_version}.ci.{run_number}"
    else:
        # A development build after a stable release must sort above that release.
        name = f"{major}.{minor}.{int(patch) + 1}-ci.{run_number}"

    outputs = {
        "version_code": str(code), "version_name": name, "base_version": base_version,
        "go_version": go_version[2:], "core_ref": props["LIBBOX_REF"], "source_sha": source_sha,
    }
    for key, value in (("VERSION_CODE", str(code)), ("VERSION_NAME", name)):
        index = positions[key]
        lines[index] = re.sub(r"(?<==)[^\r\n]*", lambda _, replacement=value: replacement, lines[index], count=1)
    return "".join(lines), outputs


def run(version_file, env):
    with version_file.open(encoding="utf-8", newline="") as stream:
        source = stream.read()
    rewritten, outputs = generate(source, env)
    output_path = env.get("GITHUB_OUTPUT", "")
    require(bool(output_path) and not any(c in output_path for c in "\r\n\0"),
            "GITHUB_OUTPUT must be a nonempty file path")
    # All inputs are validated before either destination is opened for writing.
    with Path(output_path).open("a", encoding="utf-8", newline="") as output:
        with version_file.open("w", encoding="utf-8", newline="") as stream:
            stream.write(rewritten)
        output.write("".join(f"{key}={value}\n" for key, value in outputs.items()))


def main():
    try:
        run(Path(__file__).resolve().parents[2] / "version.properties", os.environ)
    except ValueError as error:
        print(f"version: {error}", file=sys.stderr)
        return 1
    except (OSError, UnicodeError):
        print("version: unable to read or write version.properties or GITHUB_OUTPUT", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
