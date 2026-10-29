#!/usr/bin/env python3
"""Verify that the exact user-approved AnyTV image assets are installed.

Only assets already used for launcher, splash and brand may be replaced.
Do not add logo into content or remote screens. No network or secrets.
"""
import hashlib
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EXPECTED = {
    "assets/anytv_icon.png": "03ce0d8333841de3df8e28459cea5d30a14b171dbcc61dd57682c81cc9c5d578",
    "assets/anytv_logo.png": "aecbba186c86bc07fc77f30ee97ce512ccfac551d9d7fd63283d9a453855affc",
    "assets/anytv_horizontal.png": "aecbba186c86bc07fc77f30ee97ce512ccfac551d9d7fd63283d9a453855affc",
}

class ApprovedBrandAssets(unittest.TestCase):
    def test_exact_approved_bytes(self):
        for name, digest in EXPECTED.items():
            with self.subTest(asset=name):
                data = (ROOT / name).read_bytes()
                self.assertEqual(hashlib.sha256(data).hexdigest(), digest,
                                 "Brand asset not equal to the user-approved image")

    def test_adaptive_launcher_contract(self):
        source = (ROOT / "scripts/apply_anytv_brand.py").read_text()
        for text in ('mipmap-anydpi-v26', 'ic_launcher_background.xml',
                     'ic_launcher_monochrome.png', 'android:color="#000000"',
                     'ic_launcher_round.xml'):
            with self.subTest(resource=text):
                self.assertIn(text, source)

    def test_no_change_to_package_or_signer(self):
        pipeline = (ROOT / ".github/workflows/anytv-v2-candidate-compile.yml").read_text()
        self.assertIn("ANYTV_SIGNER_V2_RECOVERY_HEX", pipeline)
        self.assertIn("ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS", pipeline)
        self.assertIn("release/anytv-v2.json", pipeline)

if __name__ == "__main__":
    unittest.main()
