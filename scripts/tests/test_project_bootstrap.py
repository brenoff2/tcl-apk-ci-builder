from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]

class ChatGPTBootstrapContract(unittest.TestCase):
    def test_project_instructions_require_readback(self):
        t=(ROOT/'docs/dev-context/CHATGPT_PROJECT_INSTRUCTIONS.md').read_text(encoding='utf-8')
        for x in ('AGENTS.md','STATUS.md','GitHub','SHA'):
            self.assertIn(x,t)
        self.assertIn('nao e ativado automaticamente',t.lower())

    def test_project_instructions_forbid_unverified_completion(self):
        t=(ROOT/'docs/dev-context/CHATGPT_PROJECT_INSTRUCTIONS.md').read_text(encoding='utf-8')
        self.assertIn('NAO_TESTADO',t)
        self.assertIn('OTA',t)

    def test_proposals_default_to_candidate(self):
        t=(ROOT/'.github/ISSUE_TEMPLATE/ai-development-improvement.md').read_text(encoding='utf-8')
        self.assertIn('CANDIDATA',t)
        self.assertIn('Evidencia',t)
        self.assertIn('Aprovacao',t)
        self.assertNotIn('status: APROVADA',t)

    def test_improvement_policy_requires_approval_for_route_changes(self):
        t=(ROOT/'docs/dev-context/IMPROVEMENT_POLICY.md').read_text(encoding='utf-8')
        self.assertIn('autorizacao',t)
        self.assertIn('rota',t)
        self.assertIn('rollback',t)

if __name__=='__main__': unittest.main()
