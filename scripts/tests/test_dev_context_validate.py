from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from scripts.dev_context.validate import validate_context, parse_status

ROOT=Path(__file__).resolve().parents[2]

class DocumentationTests(unittest.TestCase):
    def test_required_context_documents_exist(self):
        for rel in ("AGENTS.md","docs/dev-context/STATUS.md","docs/dev-context/DECISIONS.md","docs/dev-context/CHANGELOG_WORK.md"):
            with self.subTest(path=rel): self.assertTrue((ROOT/rel).is_file(),rel)

    def test_agent_rules_link_authoritative_sources(self):
        text=(ROOT/"AGENTS.md").read_text(encoding="utf-8")
        for s in ("docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md","docs/CONTRATO_FUNCIONAL_TCL.md",
                  "docs/dev-context/STATUS.md","feat/tcl-v22-updates-back-bluetooth","Superpowers"):
            self.assertIn(s,text)
        self.assertIn("nao e carregado automaticamente",text.lower())

    def test_status_separates_build_signature_ota_device(self):
        text=(ROOT/"docs/dev-context/STATUS.md").read_text(encoding="utf-8")
        for f in ("repository","branch","observed_commit","observed_at_utc","build_status",
                  "signature_status","ota_status","device_test_status","next_action"):
            self.assertIn(f+"=",text)
        self.assertIn("2.9.4",text)
        self.assertIn("2.1",text)
        self.assertIn("STAGING_WAITING_FOR_SIGNED_ZIP_NO_OTA_CHANGE",text)

class ValidatorTests(unittest.TestCase):
    def fixture(self):
        d=TemporaryDirectory()
        self.addCleanup(d.cleanup)
        root=Path(d.name)
        (root/"docs/dev-context").mkdir(parents=True)
        for name in ("docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md","docs/CONTRATO_FUNCIONAL_TCL.md",
                     "docs/dev-context/DECISIONS.md","docs/dev-context/CHANGELOG_WORK.md"):
            (root/name).write_text("source",encoding="utf-8")
        (root/"AGENTS.md").write_text("docs/dev-context/STATUS.md docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md docs/CONTRATO_FUNCIONAL_TCL.md")
        (root/"docs/dev-context/STATUS.md").write_text("""<!-- context-state
repository=brenoff2/tcl-apk-ci-builder
branch=feat/tcl-v22-updates-back-bluetooth
observed_commit=0884ead0e62ed4a1ee773ce4769f579ba33f7a15
observed_at_utc=2026-10-15T23:00:00Z
build_status=CI_BUILT_UNSIGNED
signature_status=UNVERIFIED
ota_status=ANDROID_2.9.4_TV_2.1
device_test_status=UNVERIFIED
next_action=Review signed artifacts
end-context-state -->

- §build_status§: https://github.com/example/build
- §signature_status§: https://github.com/example/signature
- §ota_status§: https://github.com/example/current
- §device_test_status§: UNVERIFIED
""".replace('§',chr(96)),encoding="utf-8")
        return root

    def change(self,root,a,b):
        p=root/"docs/dev-context/STATUS.md"
        p.write_text(p.read_text(encoding="utf-8").replace(a,b),encoding="utf-8")

    def test_valid_context_passes(self):
        self.assertEqual([],validate_context(self.fixture(),expected_branch="feat/tcl-v22-updates-back-bluetooth"))

    def test_rejects_wrong_branch(self):
        self.assertTrue(any("branch mismatch" in e for e in validate_context(self.fixture(),expected_branch="main")))

    def test_requires_independent_release_evidence(self):
        root=self.fixture()
        self.change(root,"https://github.com/example/signature","PUBLISHED")
        self.assertTrue(any("signature_status" in e for e in validate_context(root)))

    def test_missing_commit_or_evidence_fails(self):
        root=self.fixture()
        self.change(root,"observed_commit=0884ead0e62ed4a1ee773ce4769f579ba33f7a15","observed_commit=unknown")
        self.assertTrue(any("observed_commit" in e for e in validate_context(root)))

    def test_duplicate_keys_fail(self):
        root=self.fixture()
        self.change(root,"branch=feat/tcl-v22-updates-back-bluetooth",
                    "branch=feat/tcl-v22-updates-back-bluetooth\nbranch=main")
        self.assertTrue(any("duplicate" in e for e in validate_context(root)))

    def test_unverified_evidence_with_explanation_passes(self):
        root=self.fixture()
        self.change(root,"§device_test_status§: UNVERIFIED".replace('§',chr(96)),
                    "§device_test_status§: UNVERIFIED, aguardando dispositivo".replace('§',chr(96)))
        self.assertEqual([],validate_context(root))

    def test_rejects_stale_map_in_strict_mode(self):
        root=self.fixture()
        path=root/"docs/dev-context/CODEBASE_MAP.md"
        path.write_text("source_commit=0000000000000000000000000000000000000000\n")
        self.assertTrue(any("stale map" in e for e in validate_context(root,strict_map=True)))

    def test_strict_mode_rejects_missing_map(self):
        root=self.fixture()
        self.assertTrue(any("missing" in e for e in validate_context(root,strict_map=True)))

if __name__=="__main__":
    unittest.main()
