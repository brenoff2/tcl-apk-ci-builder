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
        self.assertNotIn('Text("Cartaz original"', self.catalog)
        self.assertIn('"https://image.tmdb.org/t/p/w780" + title.poster', self.catalog)
        self.assertIn("title.poster", self.catalog)
        self.assertIn("credits", self.worker)

    def test_rating_is_not_cached_as_48h_catalogue_or_stale_offline_value(self):
        start = self.catalog.index('    private fun get(token: String, route: String): JSONObject {')
        end = self.catalog.index('    suspend fun traktRating(', start)
        getter = self.catalog[start:end]
        self.assertIn('val remoteRating = route.startsWith("ratings/trakt/")', getter)
        self.assertIn('if (!remoteRating) StreamingDiskCache.read(route)', getter)
        self.assertIn('if (!remoteRating) StreamingDiskCache.write(route, payload, ttl(route))', getter)
        self.assertIn('if (remoteRating) throw failure', getter)
        self.assertIn('StreamingDiskCache.stale(route) ?: throw failure', getter)
        self.assertIn('max-age=900', self.worker)

    def test_detail_parses_only_valid_trakt_rating_and_shows_unavailable(self):
        self.assertIn('get("backend", "ratings/trakt/$media/$id")', self.catalog)
        self.assertIn('data.optString("source") == "trakt"', self.catalog)
        self.assertIn('votes > 0 && it.isFinite() && it in 0.0..10.0', self.catalog)
        self.assertIn('detailTraktRating = runCatching { StreamingCatalog.traktRating(item.media, item.id) }', self.catalog)
        self.assertNotIn('Trakt: nota indisponível', self.catalog)
        self.assertIn('val traktScore = detailTraktRating?.let {', self.catalog)
        self.assertIn('StreamingCatalog.cast(item.media, item.id)', self.catalog)
        self.assertIn('minOf(cast.length(), 12)', self.catalog)

    def test_detail_modal_is_scrolling_with_compact_actions_and_single_scores(self):
        start = self.catalog.index("    val title = details\n    if (title != null) {")
        end = self.catalog.index("    // Overlay stays available", start)
        detail = self.catalog[start:end]
        self.assertNotIn("            title = {", detail)
        self.assertIn("heightIn(max = 560.dp)", detail)
        self.assertEqual(detail.count("TMDB ★ "), 1)
        self.assertNotIn("TMDB: ", detail)
        self.assertIn("detailTraktRating?.let {", detail)
        self.assertNotIn("Trakt: nota indisponível", detail)
        self.assertIn('Text("Controle da TV", fontSize = 12.sp)', detail)
        self.assertNotIn("Abrir controle da TV para escolher perfil", detail)
        self.assertIn('contentDescription = "Controle da TV"', detail)
        self.assertIn('"Abrir na " + tvName.ifBlank { "TV conectada" }', detail)
        self.assertIn('OutlinedButton(onClick = {', detail)
        self.assertNotIn("A TV poderá abrir a busca do título", detail)
        self.assertNotIn("Ofertas informadas pelo TMDB/JustWatch", detail)
        self.assertNotIn('Text("Cartaz original"', detail)
        self.assertIn("Modifier.width(190.dp).height(285.dp)", detail)
        self.assertIn("containerColor = if (chosen) Color(0xFFC50038)", detail)

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
