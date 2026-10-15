#!/usr/bin/env python3
"""AnyTV streaming screens: source and integration tests; real TV tests still required."""
import unittest
from pathlib import Path

UI = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app")

class StreamingScreens(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.screen = (UI / "TclStreamingScreens.kt").read_text(encoding="utf-8")
        cls.apps = (UI / "TclInstalledApps.kt").read_text(encoding="utf-8")
        cls.gradle = Path("source/app/build.gradle.kts").read_text()

    def test_real_installed_apps_are_the_source(self):
        self.assertIn('request(host, code, "/apps")', self.apps)
        self.assertIn("TclStreamingScreens(apps = allApps", self.apps)
        self.assertIn("OfficialTvIcon(app, 48.dp)", self.screen)
        self.assertNotIn("listOf(TvOfficialApp(", self.screen)
        self.assertIn('"favorites:$deviceKey"', self.apps)

    def test_tmdb_catalog_uses_only_live_region_specific_results(self):
        for target in (
            "api.themoviedb.org/3/", "watch_region=BR",
            "with_watch_providers=$providerId", "with_watch_monetization_types=flatrate",
            "Authorization", "Bearer $token", "JustWatch"
        ):
            self.assertIn(target, self.screen)
        self.assertNotIn("fakeResults", self.screen)
        self.assertNotIn("sampleCatalog", self.screen)

    def test_no_unverified_playback_claim(self):
        self.assertIn("TclTvAppApi.open(host, pairing, app.packageName)", self.screen)
        self.assertIn("Não existe deep link universal", self.screen)
        self.assertNotIn("startPlaybackOnTv", self.screen)

    def test_search_genres_grid_and_favorites(self):
        for fragment in ("search/$media?", "genre/$media/list", "with_genres=$genre",
                         "offeredInBrazil(", "StreamingFavorite(", "streaming_favorites",
                         "LazyVerticalGrid(", "Localizar título na TCL"):
            self.assertIn(fragment, self.screen)
        self.assertIn("searchTitle(", self.apps)

    def test_update_identity_is_29(self):
        self.assertIn('applicationId = "com.tclremote.livre"', self.gradle)
        self.assertIn("versionCode = 29", self.gradle)

if __name__ == "__main__":
    unittest.main()
