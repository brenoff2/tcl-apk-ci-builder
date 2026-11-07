import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]

class AnyTvUxRegressions(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.account = (ROOT / "overrides/AnyTvAccountFlow.kt").read_text(encoding="utf-8")
        cls.catalog = (ROOT / "overrides/TclStreamingScreens.kt").read_text(encoding="utf-8")
        cls.installed = (ROOT / "overrides/TclInstalledApps.kt").read_text(encoding="utf-8")
        cls.home = (ROOT / "overrides/TclApprovedHomeScreen.kt").read_text(encoding="utf-8")
        cls.worker = (ROOT / "anytv-resolver/src/worker.mjs").read_text(encoding="utf-8")

    def test_guest_first_with_real_google_account_controls(self):
        self.assertIn("existingApp()", self.account)
        self.assertIn("showAccount = true", self.account)
        self.assertIn("gateway.restoreSession()", self.account)
        self.assertIn("gateway.sessionUpdates().collect", self.account)
        self.assertIn("gateway.signInWithGoogle()", self.account)
        self.assertIn("gateway.signOut()", self.account)
        self.assertNotIn("else -> AnyTvAccountEntry(", self.account)

    def test_real_actor_credits_and_original_poster(self):
        self.assertIn("StreamingCatalog.cast(item.media, item.id)", self.catalog)
        self.assertIn("Elenco principal", self.catalog)
        self.assertIn("actor.character", self.catalog)
        self.assertIn("Cartaz original", self.catalog)
        self.assertIn("title.poster", self.catalog)
        self.assertIn("credits", self.worker)

    def test_only_confirmed_streaming_preselected(self):
        self.assertIn("StreamingCatalog.appHasOffer(app, offers)", self.catalog)
        self.assertIn("eligible.singleOrNull()", self.catalog)
        self.assertIn("detailsPackage = preferred?.packageName", self.catalog)

    def test_remote_uses_existing_phone_tv_connection(self):
        self.assertIn("onRemoteKey = { remoteKey(it) }", self.home)
        self.assertIn("onRemoteKey = onRemoteKey", self.installed)
        self.assertIn("showPlaybackRemote = true", self.catalog)
        for key in ("KEYCODE_DPAD_UP", "KEYCODE_DPAD_DOWN", "KEYCODE_DPAD_LEFT",
                    "KEYCODE_DPAD_RIGHT", "KEYCODE_DPAD_CENTER", "KEYCODE_BACK",
                    "KEYCODE_HOME", "KEYCODE_MEDIA_PLAY_PAUSE"):
            self.assertIn(key, self.catalog)
        self.assertNotIn("ANYTV_SIGNER_V2_RECOVERY_HEX", self.catalog)

if __name__ == "__main__":
    unittest.main()
