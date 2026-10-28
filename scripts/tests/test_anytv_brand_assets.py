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
    "assets/anytv_icon.png": "990db3c0d4a4fe07e3765851dfb3e5455c849a4f852b8537caa881574495dcf6",
    "assets/anytv_logo.png": "a8747c3c456eb502c7b93a3fcca803b23631e6f2054edfef05715e5d39401790",
    "assets/anytv_horizontal.png": "a8747c3c456eb502c7b93a3fcca803b23631e6f2054edfef05715e5d39401790",
}

class ApprovedBrandAssets(unittest.TestCase):
    def test_exact_approved_bytes(self):
        for name, digest in EXPECTED.items():
            with self.subTest(asset=name):
                data = (ROOT / name).read_bytes()
                self.assertEqual(hashlib.sha256(data).hexdigest(), digest,
                                 "Brand asset not equal to the user-approved image")

    def test_no_change_to_package_or_signer(self):
        pipeline = (ROOT / ".github/workflows/anytv-v2-candidate-compile.yml").read_text()
        self.assertIn("ANYTV_SIGNER_V2_RECOVERY_HEX", pipeline)
        self.assertIn("ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS", pipeline)
        self.assertIn("release/anytv-v2.json", pipeline)

if __name__ == "__main__":
    unittest.main()
