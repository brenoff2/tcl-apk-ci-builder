import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]

class AccountEntryContract(unittest.TestCase):
    def test_guest_entry_has_real_persistent_choice(self):
        screen = (ROOT / "overrides/AnyTvAccountEntry.kt").read_text(encoding="utf-8")
        self.assertIn('Text("Continuar sem login"', screen)
        self.assertIn('Text("Entrar ou criar conta"', screen)
        self.assertIn('putString(MODE, "guest").commit()', screen)
        self.assertIn("onContinueAsGuest: () -> Unit", screen)
        self.assertIn("onSignIn: () -> Unit", screen)
        self.assertNotIn("password", screen.lower())
    def test_guest_entry_included_in_build_but_auth_not_faked(self):
        script = (ROOT / "scripts/apply_anytv_brand.py").read_text(encoding="utf-8")
        self.assertIn('copyfile("overrides/AnyTvAccountEntry.kt"', script)
        self.assertNotIn("AnyTvAccountEntry(onContinueAsGuest", script)

if __name__ == "__main__":
    unittest.main()
