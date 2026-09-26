import importlib.util
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("version", Path(__file__).with_name("version.py"))
if spec is None or spec.loader is None:
    raise ImportError("Cannot load version helper module")
version = importlib.util.module_from_spec(spec)
spec.loader.exec_module(version)

CORE = "b609f959f57ce34416c51c7b87ce4a76f2e1df56"
SOURCE = ("# Keep this comment\r\nVERSION_CODE=740\r\nVERSION_NAME=1.15.0-alpha.8\r\n"
          f"GO_VERSION=go1.26.8\r\nLIBBOX_REF={CORE}\r\nCI_VERSION_CODE_BASE=1000000\r\n"
          "\r\nOTHER = untouched=value")
ENV = {"GITHUB_RUN_NUMBER": "42", "GITHUB_SHA": "a" * 40, "GITHUB_REF": "refs/heads/main"}


def precedence(name):
    major, minor, patch, pre = version.SEMVER.fullmatch(name).groups()
    identifiers = tuple((0, int(p)) if p.isdigit() else (1, p) for p in (pre or "").split("."))
    return int(major), int(minor), int(patch), pre is None, identifiers


class VersionTests(unittest.TestCase):
    def test_prerelease_outputs_and_ordering(self):
        _, outputs = version.generate(SOURCE, ENV)
        self.assertEqual(outputs, {
            "version_code": "1000042", "version_name": "1.15.0-alpha.8.ci.42",
            "base_version": "1.15.0-alpha.8", "go_version": "1.26.8",
            "core_ref": CORE, "source_sha": "a" * 40,
        })
        self.assertLess(precedence(outputs["base_version"]), precedence(outputs["version_name"]))
        self.assertLess(precedence(outputs["version_name"]), precedence("1.15.0"))
        _, earlier = version.generate(SOURCE, dict(ENV, GITHUB_RUN_NUMBER="9"))
        self.assertLess(precedence(earlier["version_name"]), precedence(outputs["version_name"]))

    def test_stable_ordering(self):
        for stable, expected in (("1.2.3", "1.2.4-ci.42"), ("1.2.99", "1.2.100-ci.42")):
            with self.subTest(stable=stable):
                _, outputs = version.generate(SOURCE.replace("1.15.0-alpha.8", stable), ENV)
                self.assertEqual(outputs["version_name"], expected)
                self.assertLess(precedence(stable), precedence(expected))
                self.assertLess(precedence(expected), precedence(expected.split("-")[0]))

    def test_tags(self):
        for name in ("1.15.0-alpha.8", "1.2.3"):
            source = SOURCE.replace("1.15.0-alpha.8", name)
            _, outputs = version.generate(source, dict(ENV, GITHUB_REF=f"refs/tags/v{name}"))
            self.assertEqual(outputs["version_name"], name)
        with self.assertRaisesRegex(ValueError, "tag must exactly match"):
            version.generate(SOURCE, dict(ENV, GITHUB_REF="refs/tags/v1.15.0"))

    def test_malformed_versions(self):
        for name in ("", "1.2", "v1.2.3", "01.2.3", "1.02.3", "1.2.03", "1.2.3-01",
                     "1.2.3-alpha.01", "1.2.3-", "1.2.3-a..b", "1.2.3+a", "1.2.3-a_b"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                version.generate(SOURCE.replace("1.15.0-alpha.8", name), ENV)

    def test_code_bounds(self):
        _, outputs = version.generate(SOURCE, dict(ENV, GITHUB_RUN_NUMBER="2099000000"))
        self.assertEqual(outputs["version_code"], "2100000000")
        with self.assertRaises(ValueError):
            version.generate(SOURCE, dict(ENV, GITHUB_RUN_NUMBER="2099000001"))
        for base in ("698", "697", "0", "-1", "01", "2100000001", "x"):
            with self.subTest(base=base), self.assertRaises(ValueError):
                version.generate(SOURCE.replace("BASE=1000000", f"BASE={base}"), ENV)

    def test_invalid_environment(self):
        invalid = {"GITHUB_RUN_NUMBER": ("", "0", "-1", "01", "1.5", " 42", "x", "9" * 100),
                   "GITHUB_SHA": ("", "a" * 39, "A" * 40, "g" * 40, "a" * 40 + "\n"),
                   "GITHUB_REF": ("", "main", "refs/heads/", "refs/heads/a\nb")}
        for key, values in invalid.items():
            for value in (None, *values):
                env = dict(ENV)
                if value is None:
                    del env[key]
                else:
                    env[key] = value
                with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                    version.generate(SOURCE, env)

    def test_invalid_properties(self):
        for old, new in ((CORE, "main"), (CORE, CORE.upper()), ("go1.26.8", "1.26.8"),
                         ("VERSION_CODE=740", "VERSION_CODE=-1"),
                         ("LIBBOX_REF=", "IGNORED="), ("CI_VERSION_CODE_BASE=", "IGNORED=")):
            with self.subTest(old=old, new=new), self.assertRaises(ValueError):
                version.generate(SOURCE.replace(old, new), ENV)
        with self.assertRaisesRegex(ValueError, "duplicate"):
            version.generate(SOURCE + "\nVERSION_CODE=741", ENV)

    def test_idempotency_and_preservation(self):
        rewritten, outputs = version.generate(SOURCE, ENV)
        self.assertEqual(version.generate(SOURCE, ENV), (rewritten, outputs))
        self.assertEqual(rewritten, SOURCE.replace("VERSION_CODE=740", "VERSION_CODE=1000042")
                         .replace("VERSION_NAME=1.15.0-alpha.8", "VERSION_NAME=1.15.0-alpha.8.ci.42"))

    def test_workspace_and_output_writes(self):
        with tempfile.TemporaryDirectory() as directory:
            properties, output = Path(directory) / "version.properties", Path(directory) / "output"
            properties.write_bytes(SOURCE.encode())
            env = dict(ENV, GITHUB_OUTPUT=str(output))
            for overrides in ({"GITHUB_SHA": "invalid"}, {"GITHUB_OUTPUT": ""},
                              {"GITHUB_OUTPUT": "bad\0path"}):
                with self.assertRaises(ValueError):
                    version.run(properties, dict(env, **overrides))
                self.assertEqual(properties.read_bytes(), SOURCE.encode())
                self.assertFalse(output.exists())
            output.write_bytes(b"existing=value\n")
            version.run(properties, env)
            rewritten, outputs = version.generate(SOURCE, ENV)
            self.assertEqual(properties.read_bytes(), rewritten.encode())
            self.assertEqual(output.read_text(), "existing=value\n" +
                             "".join(f"{key}={value}\n" for key, value in outputs.items()))


if __name__ == "__main__":
    unittest.main()
