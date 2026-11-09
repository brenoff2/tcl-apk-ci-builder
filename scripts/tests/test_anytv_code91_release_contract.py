#!/usr/bin/env python3
"""Static fail-closed contract of manual signed code91 workflow, without invoking it."""
import re
import unittest
from pathlib import Path

PATH = Path(".github/workflows/anytv-v2-code91-signed-release.yml")
OLD = Path(".github/workflows/release-anytv-phone-v2-new-package.yml")

class Code91ReleaseContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.s = PATH.read_text(encoding="utf-8")
        cls.old = OLD.read_text(encoding="utf-8")

    def test_manual_only_and_strict_human_release_phrase(self):
        self.assertIn("workflow_dispatch:", self.s)
        self.assertNotRegex(self.s, r"(?m)^\s{0,2}push:")
        self.assertIn('test "$RELEASE_CONFIRMATION" = "PUBLISH_ANYTV_V2_CODE91"', self.s)
        self.assertIn('test "${{ github.event_name }}" = "workflow_dispatch"', self.s)
        self.assertIn("expected_source_head:", self.s)
        self.assertIn("expected_ota_head:", self.s)

    def test_inputs_not_interpolated_in_shell_commands(self):
        for name in ("release_confirmation", "expected_source_head", "expected_ota_head"):
            self.assertEqual(self.s.count("${{ inputs." + name + " }}"), 1)
        self.assertIn("^[0-9a-f]{40}$", self.s)
        self.assertIn("EXPECTED_SOURCE_HEAD:", self.s)

    def test_sign_and_version_invariants(self):
        self.assertIn("2.9.63-AnyTV", self.s)
        self.assertIn("versionCode='91'", self.s)
        self.assertIn("versionCode = 91", self.s)
        self.assertIn("com.tclremote.livre.v2", self.s)
        self.assertIn("ANYTV_SIGNER_V2_RECOVERY_HEX", self.s)
        self.assertIn("pkcs12.load_key_and_certificates", self.s)
        self.assertIn("cert.fingerprint(hashes.SHA256()).hex()==meta['certificate_sha256']", self.s)
        self.assertIn("apksigner", self.s)
        self.assertIn("Elenco principal", self.s)
        self.assertIn("movie/550/credits", self.s)
        self.assertIn("tv/1399/credits", self.s)

    def test_exact_ota_destination_and_idempotent_fail_closed(self):
        self.assertIn("updates/android/v2/current.apk", self.s)
        self.assertIn('test "$oldcode" -eq 90', self.s)
        self.assertIn("93e526e5a532724f139208ff042d1a92056f7c91", self.s)
        self.assertIn("ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS", self.s)
        self.assertIn("git push origin HEAD:feat/tcl-v22-updates-back-bluetooth", self.s)
        self.assertNotIn("git push --force", self.s)
        self.assertNotIn("updates/android/cert2026/current.apk ota-v2/", self.s)

    def test_signed_artifact_is_archived_before_ota_mutation(self):
        archive = self.s.index("      - name: Audit signed V2 app as independent artifact")
        publish = self.s.index("      - name: Publish V2 APK to independent GitHub channel")
        assert archive < publish, "signed artifact must exist before any OTA write"
        self.assertIn("signed-v2/AnyTV_V2_2.9.63_UPDATE.apk", self.s)
        self.assertIn("if-no-files-found: error", self.s)

    def test_release_stops_on_stale_ota_and_requires_signed_readback(self):
        self.assertIn('test "$current" = "$EXPECTED_OTA_HEAD"', self.s)
        self.assertIn('test "$oldcode" -eq 90', self.s)
        self.assertIn("git push origin HEAD:feat/tcl-v22-updates-back-bluetooth", self.s)
        self.assertIn("cmp -s signed-v2/remote.apk signed-v2/AnyTV_V2_2.9.63_UPDATE.apk", self.s)
        self.assertNotIn("git push --force", self.s)

    def test_recovery_code92_plan_preserves_android_anti_downgrade(self):
        plan = Path("docs/anytv-code92-recovery-plan.md").read_text(encoding="utf-8")
        for invariant in (
            "versionCode **90**", "versionCode **91**", "versionCode = 92",
            "com.tclremote.livre.v2", "RECOVERY_SOURCE_UNVERIFIED",
            "SIGNATURE_OR_VERSION_MISMATCH", "STALE_OTA_STOP",
            "PUBLISHED_UNVERIFIED", "NO_MAIN_WRITE", "NO_OTA_WRITE",
            "default_branch=main", "workflow_dispatch"
        ):
            self.assertIn(invariant, plan)
        self.assertIn("não recupera aparelhos já atualizados", plan)
        self.assertIn("não recupera a UI Android", plan)

    def test_old_workflow_is_distinct_and_untouched(self):
        self.assertIn("expected_version_code == 80", self.old)
        self.assertIn("2.9.51-AnyTV", self.old)
        self.assertNotIn("PUBLISH_ANYTV_V2_CODE91", self.old)

if __name__ == "__main__":
    unittest.main()
