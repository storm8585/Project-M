import importlib.util
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("abis", Path(__file__).with_name("abis.py"))
if spec is None or spec.loader is None:
    raise ImportError("Cannot load ABI helper module")
abis = importlib.util.module_from_spec(spec)
spec.loader.exec_module(abis)


class AbiTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)

    def properties(self, source):
        (self.root / "gradle.properties").write_text(source, encoding="utf-8")

    def test_single_armv7(self):
        self.properties("buildAbis=armeabi-v7a\nbuildUniversalApk=false\n")
        self.assertEqual(abis.load_config(self.root), (("armeabi-v7a",), False))
        self.assertEqual(abis.build_outputs(self.root), {
            "abis": "armeabi-v7a", "platforms": "android/arm",
        })

    def test_all_abis_and_platforms(self):
        self.properties("buildAbis=armeabi-v7a,arm64-v8a,x86,x86_64\nbuildUniversalApk=true\n")
        self.assertEqual(abis.load_config(self.root), (
            ("armeabi-v7a", "arm64-v8a", "x86", "x86_64"), True,
        ))
        self.assertEqual(abis.build_outputs(self.root), {
            "abis": "armeabi-v7a,arm64-v8a,x86,x86_64",
            "platforms": "android/arm,android/arm64,android/386,android/amd64",
        })

    def test_order_and_whitespace(self):
        self.properties("\n  buildAbis = x86_64, armeabi-v7a , x86, arm64-v8a \t\n"
                        "\tbuildUniversalApk = false \t\norg.gradle.parallel=true\n")
        self.assertEqual(abis.load_config(self.root), (
            ("x86_64", "armeabi-v7a", "x86", "arm64-v8a"), False,
        ))
        self.assertEqual(abis.build_outputs(self.root), {
            "abis": "x86_64,armeabi-v7a,x86,arm64-v8a",
            "platforms": "android/amd64,android/arm,android/386,android/arm64",
        })

    def test_invalid_abi_lists(self):
        for value in ("", " \t", "mips", "android/arm", "ARMEABI-V7A", "armeabi-v7a,mips",
                      ",armeabi-v7a", "armeabi-v7a,", "armeabi-v7a,,x86",
                      "armeabi-v7a,armeabi-v7a", "x86, x86"):
            with self.subTest(value=value):
                self.properties(f"buildAbis={value}\nbuildUniversalApk=false\n")
                with self.assertRaisesRegex(ValueError, "buildAbis"):
                    abis.load_config(self.root)
                with self.assertRaises(ValueError):
                    abis.build_outputs(self.root)

    def test_booleans(self):
        for value, expected in (("true", True), ("false", False)):
            with self.subTest(value=value):
                self.properties(f"buildAbis=armeabi-v7a\nbuildUniversalApk={value}\n")
                self.assertEqual(abis.load_config(self.root), (("armeabi-v7a",), expected))
                self.assertEqual(abis.build_outputs(self.root), {
                    "abis": "armeabi-v7a", "platforms": "android/arm",
                })

    def test_invalid_booleans(self):
        for value in ("", " ", "True", "False", "TRUE", "FALSE", "1", "0", "yes", "no"):
            with self.subTest(value=value):
                self.properties(f"buildAbis=armeabi-v7a\nbuildUniversalApk={value}\n")
                with self.assertRaisesRegex(ValueError, "buildUniversalApk"):
                    abis.load_config(self.root)
                with self.assertRaises(ValueError):
                    abis.build_outputs(self.root)

    def test_required_settings(self):
        for source in ("", "buildAbis=armeabi-v7a\n", "buildUniversalApk=false\n"):
            with self.subTest(source=source):
                self.properties(source)
                with self.assertRaises(ValueError):
                    abis.load_config(self.root)
                with self.assertRaises(ValueError):
                    abis.build_outputs(self.root)

    def test_missing_properties_file(self):
        with self.assertRaises(FileNotFoundError):
            abis.load_config(self.root)

    def test_duplicate_settings(self):
        for setting in ("buildAbis=armeabi-v7a", "buildUniversalApk=false"):
            with self.subTest(setting=setting):
                self.properties("buildAbis=armeabi-v7a\nbuildUniversalApk=false\n"
                                f" {setting}\n")
                with self.assertRaisesRegex(ValueError, "Duplicate Gradle property"):
                    abis.load_config(self.root)


if __name__ == "__main__":
    unittest.main()
