import importlib.util
from pathlib import Path
import unittest

script = Path(__file__).resolve().parents[1] / "prepare_anytv_staging.py"
spec = importlib.util.spec_from_file_location("anytv_staging", script)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)


class StagingEndpoint(unittest.TestCase):
    def test_dedicated_staging_hostname(self):
        self.assertEqual(
            mod.validate("https://anytv-p0-staging-sandbox.workers.dev"),
            "https://anytv-p0-staging-sandbox.workers.dev",
        )

    def test_production_and_unsafe_urls_rejected(self):
        for url in [
            "https://anytv-link-resolver-pilot.brenoff.workers.dev",
            "http://anytv-p0-staging-sandbox.workers.dev",
            "https://anytv-p0-staging-sandbox.workers.dev.evil.test",
            "https://anytv-p0-staging-sandbox.workers.dev/resolve",
            "https://anytv-p0-staging-sandbox.workers.dev?key=secret",
            "https://user@anytv-p0-staging-sandbox.workers.dev",
            "https://anytv-p0-staging-sandbox.workers.dev:444",
        ]:
            with self.subTest(url=url), self.assertRaises(ValueError):
                mod.validate(url)


if __name__ == "__main__":
    unittest.main()
