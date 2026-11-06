import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]

class AccountEntryContract(unittest.TestCase):
    def test_guest_entry_has_real_persistent_choice(self):
        screen = (ROOT / "overrides/AnyTvAccountEntry.kt").read_text(encoding="utf-8")
        self.assertIn('Text("Continuar sem login"', screen)
        self.assertIn('"Continuar com Google"', screen)
        self.assertIn('putString(MODE, "guest").commit()', screen)
        self.assertIn("onContinueAsGuest: () -> Unit", screen)
        self.assertIn("onSignIn: () -> Unit", screen)
        self.assertNotIn("PasswordVisualTransformation", screen)
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

class EntryHostContract(unittest.TestCase):
    def test_host_not_activated_without_live_auth(self):
        host = (ROOT / "overrides/AnyTvEntryHost.kt").read_text(encoding="utf-8")
        self.assertIn("AnyTvAccountEntry(", host)
        self.assertIn("AnyTvEntryPreference.selectGuest(context)", host)
        self.assertIn("onLoginRequested", host)
        self.assertIn("existingApp()", host)
        script = (ROOT / "scripts/apply_anytv_brand.py").read_text(encoding="utf-8")
        self.assertIn('copyfile("overrides/AnyTvEntryHost.kt"', script)
        self.assertNotIn("AnyTvEntryHost(", script)

class UpstreamAuthAdapterContract(unittest.TestCase):
    def test_upstream_auth_and_no_fake_login(self):
        src = (ROOT / "overrides/AnyTvSupabaseAuthGateway.kt").read_text(encoding="utf-8")
        for snippet in (
            "client.auth.signInWith(Email)", "client.auth.signUpWith(Email)",
            "client.auth.signInWith(Google)", "client.auth.signOut()",
            "client.auth.currentUserOrNull()", "client.auth.awaitInitialization()"
        ):
            self.assertIn(snippet, src)
        self.assertNotIn("service_role", src)
        self.assertNotIn("passwordHash", src)
        build = (ROOT / "scripts/apply_anytv_brand.py").read_text(encoding="utf-8")
        self.assertIn("auth-kt:3.8.0", build)
        self.assertIn("AnyTvSupabaseAuthGateway.kt", build)

class AuthClientFactoryContract(unittest.TestCase):
    def test_public_https_only_auth_client(self):
        src = (ROOT / "overrides/AnyTvAuthClientFactory.kt").read_text(encoding="utf-8")
        self.assertIn('parsed.scheme == "https"', src)
        self.assertIn("install(Auth)", src)
        self.assertIn("config.publishableKey", src)
        self.assertNotIn("service_role", src.replace("service-role", ""))
        self.assertNotIn("http://", src)
        script = (ROOT / "scripts/apply_anytv_brand.py").read_text(encoding="utf-8")
        self.assertIn('copyfile("overrides/AnyTvAuthClientFactory.kt"', script)

class AnyTvClientFactoryContract(unittest.TestCase):
    def test_official_sdk_has_dedicated_https_config(self):
        src = (ROOT / "overrides/AnyTvAuthClientFactory.kt").read_text(encoding="utf-8")
        self.assertIn("createSupabaseClient(", src)
        self.assertIn("install(Auth)", src)
        self.assertIn("install(Postgrest)", src)
        self.assertIn('parsed.scheme == "https"', src)
        build = (ROOT / "scripts/apply_anytv_brand.py").read_text(encoding="utf-8")
        self.assertEqual(build.count('implementation("io.github.jan-tennert.supabase:auth-kt:3.8.0")'), 1)
        self.assertEqual(build.count('implementation("io.github.jan-tennert.supabase:postgrest-kt:3.8.0")'), 1)

class GuestAuthFlowContract(unittest.TestCase):
    def test_guest_and_authenticated_paths_use_gateway(self):
        src = (ROOT / "overrides/AnyTvAccountFlow.kt").read_text(encoding="utf-8")
        self.assertIn("gateway.restoreSession()", src)
        self.assertIn("gateway.signInWithGoogle()", src)
        self.assertIn("gateway.sessionUpdates().collect", src)
        self.assertNotIn("gateway.signInWithEmail(", src)
        self.assertNotIn("gateway.signUpWithEmail(", src)
        self.assertIn("AnyTvEntryPreference.selectGuest(context)", src)
        self.assertIn("existingApp()", src)
        build = (ROOT / "scripts/apply_anytv_brand.py").read_text(encoding="utf-8")
        self.assertIn("AnyTvAccountFlow.kt", build)

class GoogleOAuthIntegrationContract(unittest.TestCase):
    def test_google_only_pkce_and_verified_session(self):
        client = (ROOT / "overrides/AnyTvAuthClientFactory.kt").read_text(encoding="utf-8")
        gateway = (ROOT / "overrides/AnyTvSupabaseAuthGateway.kt").read_text(encoding="utf-8")
        entry = (ROOT / "overrides/AnyTvAccountEntry.kt").read_text(encoding="utf-8")
        self.assertIn("flowType = FlowType.PKCE", client)
        self.assertIn('scheme = "com.tclremote.livre.v2"', client)
        self.assertIn('host = "auth"', client)
        self.assertIn("client.handleDeeplinks(intent)", gateway)
        self.assertIn("SessionStatus.Authenticated", gateway)
        self.assertIn("Continuar com Google", entry)
        self.assertIn('publishableKey.startsWith("sb_publishable_")', client)

    def test_android_build_applies_real_callback_without_enabling_unconfigured_auth(self):
        script = (ROOT / "scripts/apply_anytv_google_login.py").read_text(encoding="utf-8")
        build = (ROOT / "scripts/apply_anytv_brand.py").read_text(encoding="utf-8")
        public = (ROOT / "release/anytv-auth-public.json").read_text(encoding="utf-8")
        import json
        from urllib.parse import urlparse
        metadata = json.loads(public)
        self.assertEqual(set(metadata), {"apiUrl", "publishableKey"})
        self.assertTrue(all(isinstance(v, str) for v in metadata.values()))
        self.assertEqual(bool(metadata["apiUrl"]), bool(metadata["publishableKey"]))
        if metadata["apiUrl"]:
            self.assertEqual(urlparse(metadata["apiUrl"]).scheme, "https")
            self.assertTrue(metadata["publishableKey"].startswith("sb_publishable_"))
        self.assertIn('if not key.startswith("sb_publishable_")', script)
        self.assertIn('anyTvGoogleGateway?.handleAuthCallback(intent)', script)
        self.assertIn('android:host="auth"', script)
        self.assertIn('android:launchMode="singleTop"', script)
        self.assertIn('AnyTvAccountFlow(gateway = gateway, existingApp = existingApp)', script)
        self.assertIn('if (gateway == null) existingApp()', script)
        self.assertIn('runpy.run_path("scripts/apply_anytv_google_login.py"', build)

if __name__ == "__main__":
    unittest.main()
