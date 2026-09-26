"""Prepare optional release signing without writing secret values to Actions logs."""

import base64
import binascii
import os
import sys
from pathlib import Path

SECRET_KEYS = (
    "ANDROID_KEYSTORE_BASE64", "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD",
)


def signing_files(env):
    values = [env.get(key, "") for key in SECRET_KEYS]
    if not any(values):
        return None
    if not all(values):
        raise ValueError("Set all four ANDROID signing secrets, or leave all four unset")
    try:
        keystore = base64.b64decode("".join(values[0].split()), validate=True)
    except (ValueError, binascii.Error):
        raise ValueError("ANDROID_KEYSTORE_BASE64 is not valid base64") from None
    if not keystore:
        raise ValueError("The release keystore is empty")

    def escape(value):
        # Properties.load(InputStream) understands Java UTF-16 escapes, not UTF-8.
        encoded = value.encode("utf-16-be")
        return "".join(f"\\u{int.from_bytes(encoded[i:i + 2], 'big'):04x}"
                       for i in range(0, len(encoded), 2))

    properties = "".join(f"{key}={escape(value)}\n" for key, value in zip(
        ("KEYSTORE_PASS", "ALIAS_NAME", "ALIAS_PASS"), values[1:]))
    return keystore, properties


def main():
    root = Path(__file__).resolve().parents[2]
    files = signing_files(os.environ)
    build_type = "Debug"
    if files is not None:
        destinations = ((root / "app/release.keystore", files[0]),
                        (root / "local.properties", files[1].encode("ascii")))
        # Check both destinations before creating either signing file.
        if any(path.exists() or path.is_symlink() for path, _ in destinations):
            raise ValueError("Signing files already exist")
        created = []
        try:
            for path, content in destinations:
                with path.open("xb") as stream:
                    created.append(path)
                    path.chmod(0o600)
                    stream.write(content)
        except OSError:
            for path in created:
                path.unlink(missing_ok=True)
            raise
        build_type = "Release"
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"build_type={build_type}\n")
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
        summary.write(f"\nSigning: **{build_type}**"
                      + (" (no release credentials configured).\n" if files is None else ".\n"))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError):
        # Do not include the exception: some encoding errors can contain secrets.
        print("::error::Signing preparation failed. Check all four signing secrets, "
              "base64 encoding, and ensure signing files do not already exist.", file=sys.stderr)
        sys.exit(1)
