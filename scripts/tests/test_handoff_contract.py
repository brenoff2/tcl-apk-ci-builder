from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]

class HandoffContractTests(unittest.TestCase):
    def test_handoff_has_5_cases_and_no_automatic_success(self):
        text=(ROOT/'docs/dev-context/HANDOFF_CANARY.md').read_text(encoding='utf-8')
        for label in ('branch','OTA','source','map','alteracao'):
            self.assertIn(label,text)
        self.assertIn('PENDENTE',text)
        self.assertIn('NAO_EXECUTADO',text)

    def test_ci_has_read_only_permissions_and_no_publish(self):
        text=(ROOT/'.github/workflows/dev-context-check.yml').read_text(encoding='utf-8')
        self.assertIn('pull_request:',text)
        self.assertIn('contents: read',text)
        self.assertIn('validate.py',text)
        self.assertIn('test_project_bootstrap.py',text)
        self.assertNotIn('workflow_dispatch:',text)
        self.assertNotIn('contents: write',text)
        self.assertNotIn('secrets.',text)
        self.assertNotIn('docker',text.lower())

if __name__=='__main__': unittest.main()
