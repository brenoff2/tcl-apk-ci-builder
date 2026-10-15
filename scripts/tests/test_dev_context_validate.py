from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

class InitialDocumentationTests(unittest.TestCase):
    def test_required_context_documents_exist(self):
        for rel in ["AGENTS.md", "docs/dev-context/STATUS.md", "docs/dev-context/DECISIONS.md", "docs/dev-context/CHANGELOG_WORK.md"]:
            with self.subTest(file=rel):
                self.assertTrue((ROOT / rel).is_file(), f"missing {rel}")

    def test_agent_rules_link_authoritative_sources(self):
        content = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
        for item in ["docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md", "docs/CONTRATO_FUNCIONAL_TCL.md", "docs/dev-context/STATUS.md", "feat/tcl-v22-updates-back-bluetooth", "Superpowers"]:
            self.assertIn(item, content)
        self.assertIn("nao e carregado automaticamente", content.lower())

    def test_status_separates_build_signature_ota_device(self):
        status = (ROOT / "docs/dev-context/STATUS.md").read_text(encoding="utf-8")
        for field in ["repository", "branch", "observed_commit", "observed_at_utc", "build_status", "signature_status", "ota_status", "device_test_status", "next_action"]:
            self.assertIn(field + "=", status)
        self.assertIn("2.9.4", status)
        self.assertIn("2.1", status)
        self.assertIn("STAGING_WAITING_FOR_SIGNED_ZIP_NO_OTA_CHANGE", status)

if __name__ == "__main__":
    unittest.main()
