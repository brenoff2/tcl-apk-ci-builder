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

class SignInScreenContract(unittest.TestCase):
    def test_sign_in_uses_real_callbacks_not_mock_success(self):
        src = (ROOT / "overrides/AnyTvSignInScreen.kt").read_text(encoding="utf-8")
        for name in ("onSignIn", "onSignUp", "onGoogle", "onResetPassword", "onGuest"):
            self.assertIn(name, src)
        self.assertIn("PasswordVisualTransformation()", src)
        self.assertNotIn("signInSucceeded = true", src)
        self.assertNotIn("SharedPreferences", src)
        script = (ROOT / "scripts/apply_anytv_brand.py").read_text(encoding="utf-8")
        self.assertIn('copyfile("overrides/AnyTvSignInScreen.kt"', script)

class AccountSessionContract(unittest.TestCase):
    def test_gateway_never_authenticates_by_local_email(self):
        src = (ROOT / "overrides/AnyTvAccountSession.kt").read_text(encoding="utf-8")
        self.assertIn("interface AnyTvAccountGateway", src)
        self.assertIn("verifiedUserId: String?", src)
        self.assertIn("guestPreviouslySelected -> AnyTvAccountSession.Guest", src)
        self.assertIn("data class SignedIn(val userId: String)", src)
        self.assertNotIn("password = ", src)
        script = (ROOT / "scripts/apply_anytv_brand.py").read_text(encoding="utf-8")
        self.assertIn('copyfile("overrides/AnyTvAccountSession.kt"', script)

if __name__ == "__main__":
    unittest.main()
