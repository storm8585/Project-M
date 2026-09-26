import base64
import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from typing import Any
from unittest import mock
from zipfile import ZipFile


def load(name):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(name + ".py"))
    if spec is None or spec.loader is None:
        raise ImportError(f"Cannot load helper module {name}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


check_native, signing, package = map(load, ("check_native", "prepare_signing", "package"))
ABIS = ("armeabi-v7a", "arm64-v8a", "x86", "x86_64", "universal")


class SigningTests(unittest.TestCase):
    def credentials(self):
        return dict(zip(signing.SECRET_KEYS, (base64.b64encode(b"keystore").decode(),
                                             " p=é\n", "🔑", "\\:\t")))

    def test_none(self):
        self.assertIsNone(signing.signing_files({}))
        self.assertIsNone(signing.signing_files(dict.fromkeys(signing.SECRET_KEYS, "")))

    def test_partial(self):
        for key in signing.SECRET_KEYS:
            env = self.credentials()
            del env[key]
            with self.subTest(missing=key), self.assertRaisesRegex(ValueError, "all four"):
                signing.signing_files(env)

    def test_invalid_base64(self):
        for value in ("%%%", "YQ", "é", " \n"):
            env = dict(self.credentials(), ANDROID_KEYSTORE_BASE64=value)
            with self.subTest(value=value), self.assertRaises(ValueError):
                signing.signing_files(env)

    def test_valid_unicode_properties(self):
        env = self.credentials()
        env["ANDROID_KEYSTORE_BASE64"] = " \n" + env["ANDROID_KEYSTORE_BASE64"] + "\t"
        keystore, properties = signing.signing_files(env)
        self.assertEqual(keystore, b"keystore")
        self.assertEqual(properties, "KEYSTORE_PASS=\\u0020\\u0070\\u003d\\u00e9\\u000a\n"
                         "ALIAS_NAME=\\ud83d\\udd11\nALIAS_PASS=\\u005c\\u003a\\u0009\n")
        self.assertTrue(properties.isascii())

    def test_main_preserves_existing_local_properties(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "app").mkdir()
            properties = root / "local.properties"
            original = b"sdk.dir=/developer/android-sdk\n"
            properties.write_bytes(original)
            output, summary = root / "output", root / "summary"
            env = dict(self.credentials(), GITHUB_OUTPUT=str(output), GITHUB_STEP_SUMMARY=str(summary))
            with (
                mock.patch.object(signing, "__file__", str(root / ".github/scripts/prepare_signing.py")),
                mock.patch.dict(signing.os.environ, env, clear=True),
                self.assertRaisesRegex(ValueError, "Signing files already exist"),
            ):
                signing.main()
            self.assertEqual(properties.read_bytes(), original)
            self.assertFalse((root / "app/release.keystore").exists())
            self.assertFalse(output.exists())
            self.assertFalse(summary.exists())


class TempTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)


class NativeTests(TempTests):
    def aar(self, api, abis=ABIS[:4], empty=None):
        path = self.root / "libbox.aar"
        with ZipFile(path, "w") as archive:
            archive.writestr("classes.jar", b"" if empty == "classes.jar" else b"synthetic classes")
            for abi in abis:
                entry = f"jni/{abi}/libbox.so"
                archive.writestr(entry, b"" if empty == entry else b"synthetic native code")
            archive.writestr("AndroidManifest.xml", '<manifest xmlns:android="'
                             'http://schemas.android.com/apk/res/android">'
                             f'<uses-sdk android:minSdkVersion="{api}"/></manifest>')
        return path

    def test_correct_api_and_all_abis(self):
        for api in (21, 24):
            with self.subTest(api=api), mock.patch("builtins.print"):
                check_native.check(self.aar(api), api, ABIS[:4])

    def test_missing_abi(self):
        for missing in ABIS[:4]:
            with self.subTest(abi=missing), self.assertRaises(ValueError):
                check_native.check(self.aar(24, [abi for abi in ABIS[:4] if abi != missing]), 24, ABIS[:4])

    def test_armv7_only(self):
        for api in (21, 24):
            with self.subTest(api=api), mock.patch("builtins.print"):
                check_native.check(self.aar(api, ABIS[:1]), api, ABIS[:1])

    def test_extra_abi(self):
        for extra in (*ABIS[1:4], "unsupported"):
            with self.subTest(abi=extra), self.assertRaises(ValueError):
                check_native.check(self.aar(24, (*ABIS[:1], extra)), 24, ABIS[:1])

    def test_empty_expected_file(self):
        for entry in ("classes.jar", "jni/armeabi-v7a/libbox.so"):
            with self.subTest(entry=entry), self.assertRaisesRegex(ValueError, "empty"):
                check_native.check(self.aar(24, ABIS[:1], empty=entry), 24, ABIS[:1])

    def test_wrong_sdk(self):
        for actual, expected in ((21, 24), (24, 21)):
            with self.subTest(actual=actual), self.assertRaisesRegex(ValueError, "minSdkVersion"):
                check_native.check(self.aar(actual), expected, ABIS[:4])


class PackageTests(TempTests):
    def setUp(self):
        super().setUp()
        self.env = {
            "BUILD_FLAVOR": "other", "BUILD_TYPE": "Debug", "ANDROID_HOME": str(self.root / "sdk"),
            "GITHUB_SHA": "a" * 40, "CORE_REF": "b" * 40, "GITHUB_RUN_ID": "42",
            "GITHUB_RUN_ATTEMPT": "2", "GITHUB_SERVER_URL": "https://github.com",
            "GITHUB_REPOSITORY": "example/sfa", "GITHUB_STEP_SUMMARY": str(self.root / "summary"),
        }
        (self.root / "version.properties").write_text(
            "VERSION_CODE=1000042\nVERSION_NAME=1.2.3-ci.42\nGO_VERSION=go1.26.8\n", encoding="utf-8")
        patcher = mock.patch.object(package.subprocess, "run")
        self.verify_process = patcher.start()
        self.addCleanup(patcher.stop)
        self.fixture()

    def fixture(self, abis=ABIS):
        selected = ",".join(abi for abi in abis if abi != "universal")
        universal = "true" if "universal" in abis else "false"
        (self.root / "gradle.properties").write_text(
            f"buildAbis={selected}\nbuildUniversalApk={universal}\n", encoding="utf-8")
        self.apk_dir = (self.root / "app/build/outputs/apk" / self.env["BUILD_FLAVOR"]
                        / self.env["BUILD_TYPE"].lower())
        self.apk_dir.mkdir(parents=True, exist_ok=True)
        self.metadata: dict[str, Any] = {"applicationId": "io.nekohasekai.sfa", "elements": []}
        for abi in abis:
            name = f"app-{abi}.apk"
            (self.apk_dir / name).write_bytes(abi.encode())
            self.metadata["elements"].append({
                "versionCode": 1000042, "versionName": "1.2.3-ci.42", "outputFile": name,
                "filters": [] if abi == "universal" else [{"filterType": "ABI", "value": abi}],
            })
        self.write_metadata()

    def write_metadata(self):
        (self.apk_dir / "output-metadata.json").write_text(json.dumps(self.metadata), encoding="utf-8")

    def test_collect_metadata_checksums_provenance_and_signatures(self):
        self.assert_collection(ABIS)

    def test_collect_armv7_only(self):
        self.assert_collection(ABIS[:1])

    def assert_collection(self, abis):
        for flavor, build_type in (("other", "Debug"), ("otherLegacy", "Release")):
            with self.subTest(flavor=flavor, build_type=build_type):
                self.env.update(BUILD_FLAVOR=flavor, BUILD_TYPE=build_type)
                self.fixture(abis)
                (self.root / "summary").write_text("", encoding="utf-8")
                self.verify_process.reset_mock()
                package.collect(self.root, self.env)
                verifier = str(self.root / "sdk/build-tools/36.0.0/apksigner")
                self.assertEqual(self.verify_process.call_args_list, [mock.call(
                    [verifier, "verify", "--verbose", str(self.apk_dir / f"app-{abi}.apk")],
                    check=True) for abi in abis])
                dist, artifacts, sums = self.root / "dist", [], []
                for abi in sorted(abis):
                    name, payload = f"app-{abi}.apk", abi.encode()
                    digest = hashlib.sha256(payload).hexdigest()
                    self.assertEqual((dist / name).read_bytes(), payload)
                    artifacts.append({"file": name, "abi": abi, "sha256": digest, "size": len(payload)})
                    sums.append(f"{digest}  {name}\n")
                self.assertEqual((dist / "SHA256SUMS").read_text(), "".join(sums))
                self.assertEqual(json.loads((dist / "build-metadata.json").read_text()), {
                    "version_name": "1.2.3-ci.42", "version_code": 1000042,
                    "application_id": "io.nekohasekai.sfa", "flavor": flavor, "build_type": build_type,
                    "app_commit": "a" * 40, "core_commit": "b" * 40, "go_version": "go1.26.8",
                    "run_id": "42", "run_attempt": "2", "apks": artifacts,
                    "build_url": "https://github.com/example/sfa/actions/runs/42",
                })
                self.assertEqual({path.name for path in dist.iterdir()},
                                 {item["file"] for item in artifacts} | {"SHA256SUMS", "build-metadata.json"})
                summary = (self.root / "summary").read_text()
                self.assertIn(f"### {flavor} / {build_type}", summary)
                for abi in abis:
                    if abi != "universal":
                        self.assertIn(abi, summary)
                if "universal" not in abis:
                    self.assertNotIn("universal", summary.lower())

    def test_missing_output(self):
        for failure in ("metadata", "abi", "apk", "empty"):
            with self.subTest(failure=failure):
                self.fixture()
                apk = self.apk_dir / self.metadata["elements"][0]["outputFile"]
                if failure == "metadata":
                    (self.apk_dir / "output-metadata.json").unlink()
                elif failure == "abi":
                    self.metadata["elements"].pop()
                    self.write_metadata()
                elif failure == "apk":
                    apk.unlink()
                else:
                    apk.write_bytes(b"")
                with self.assertRaises((ValueError, FileNotFoundError)):
                    package.collect(self.root, self.env)
                self.verify_process.assert_not_called()
                self.assertFalse((self.root / "dist").exists())

    def test_missing_armv7_output(self):
        self.fixture(ABIS[:1])
        self.metadata["elements"].clear()
        self.write_metadata()
        with self.assertRaises(ValueError):
            package.collect(self.root, self.env)
        self.verify_process.assert_not_called()
        self.assertFalse((self.root / "dist").exists())

    def test_unwanted_abi_or_universal(self):
        for unwanted in ABIS[1:]:
            with self.subTest(abi=unwanted):
                self.fixture((ABIS[0], unwanted))
                (self.root / "gradle.properties").write_text(
                    "buildAbis=armeabi-v7a\nbuildUniversalApk=false\n", encoding="utf-8")
                with self.assertRaises(ValueError):
                    package.collect(self.root, self.env)
                self.verify_process.assert_not_called()
                self.assertFalse((self.root / "dist").exists())

    def test_malformed_filters(self):
        for filters in (
            [{"filterType": "DENSITY", "value": "armeabi-v7a"}],
            [{"filterType": "ABI", "value": "armeabi-v7a"},
             {"filterType": "ABI", "value": "arm64-v8a"}],
            [{"filterType": "ABI", "value": "unsupported"}],
            [{"filterType": "ABI", "value": "universal"}],
        ):
            with self.subTest(filters=filters):
                self.fixture()
                self.metadata["elements"][0]["filters"] = filters
                self.write_metadata()
                with self.assertRaises(ValueError):
                    package.collect(self.root, self.env)
                self.verify_process.assert_not_called()
                self.assertFalse((self.root / "dist").exists())

    def test_duplicate_filter(self):
        self.metadata["elements"].append(dict(self.metadata["elements"][0]))
        self.write_metadata()
        with self.assertRaises(ValueError):
            package.collect(self.root, self.env)
        self.verify_process.assert_not_called()
        self.assertFalse((self.root / "dist").exists())

    def test_mismatched_version(self):
        for key, value in (("versionCode", 1000043), ("versionName", "1.2.3-ci.43")):
            with self.subTest(key=key):
                self.fixture()
                self.metadata["elements"][-1][key] = value
                self.write_metadata()
                with self.assertRaisesRegex(ValueError, "does not match the generated version"):
                    package.collect(self.root, self.env)
                self.verify_process.assert_not_called()
                self.assertFalse((self.root / "dist").exists())


if __name__ == "__main__":
    unittest.main()
