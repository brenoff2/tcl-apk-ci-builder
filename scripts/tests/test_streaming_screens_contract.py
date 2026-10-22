#!/usr/bin/env python3
"""AnyTV streaming screens: source and integration tests; real TV tests still required."""
import os
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
            "anytv-link-resolver-pilot.brenoff.workers.dev/v1/catalog/",
            "watch_region=BR", "with_watch_providers=$providerId",
            "with_watch_monetization_types=flatrate", "JustWatch"
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
                         "LazyVerticalGrid(", "titleActionFeedback"):
            self.assertIn(fragment, self.screen)
        self.assertIn("searchTitle(", self.apps)

    def test_multi_service_id_crosswalk(self):
        for claim in ('P1874', 'P13902', 'P14462', 'P8055', 'P9586', 'P9751', 'P8298'):
            self.assertIn(claim, self.screen)
        self.assertIn('wikidata_id', self.screen)
        self.assertIn('"https://app.primevideo.com/detail?gti=" + it', self.screen)
        self.assertIn('titleDeepLink(token, title, packageName)', self.screen)
        self.assertIn('TclTvAppApi.openTitle(', self.screen)
        self.assertIn('request(host, code, "/title-open", body)', self.apps)
        self.assertIn('"detail_requested"', self.screen)

    def test_check_brazilian_subscription_before_remote_title_open(self):
        self.assertIn('suspend fun titleAvailableInBrazil(', self.screen)
        self.assertIn('offeredInBrazil(token, title, id)', self.screen)
        self.assertIn('if (offered == false)', self.screen)
        self.assertIn('if (offered == null)', self.screen)
        self.assertIn('return@launch', self.screen)
        self.assertIn('titleDeepLink(token, title, packageName)', self.screen)
        self.assertLess(self.screen.index('if (offered == false)'),
                        self.screen.index('titleDeepLink(token, title, packageName)', self.screen.index('fun searchOnTv(')))
        self.assertIn('Confira \'Onde assistir e links disponíveis\'', self.screen)

    def test_compact_catalog_layout_regression(self):
        self.assertIn("BasicTextField(value = searchDraft", self.screen)
        self.assertIn("Modifier.weight(1f).height(42.dp)", self.screen)
        self.assertIn("GridCells.Adaptive(94.dp)", self.screen)
        self.assertIn("GridItemSpan(maxLineSpan)", self.screen)
        self.assertIn('Text("ⓘ Fontes: TMDB / JustWatch"', self.screen)
        self.assertNotIn('Text("Catálogo brasileiro"', self.screen)
        self.assertNotIn('Text("Streaming", fontSize = 21.sp', self.screen)
        self.assertNotIn('Button(onClick = { openOnTv(app) }, modifier = Modifier.fillMaxWidth())', self.screen)

    def test_tv_16_missing_search_route_falls_back_to_launch(self):
        self.assertIn('path == "/title-search"', self.apps)
        self.assertIn('status == 404 || status == 405', self.apps)
        self.assertIn('if (mode == "search" || mode == "opened_app")', self.apps)
        self.assertIn('if (open(host, code, packageName)) "opened_app"', self.apps)
        self.assertIn('if (status == 401)', self.apps)

    def test_title_button_keeps_dialog_open_and_displays_true_result(self):
        self.assertIn('titleActionPending by remember', self.screen)
        self.assertIn('titleActionFeedback by remember', self.screen)
        self.assertIn('if (titleActionFeedback != null)', self.screen)
        self.assertIn('Text(if (titleActionPending) "Abrindo…" else "Abrir na TCL")', self.screen)
        self.assertIn('A conexão do controle remoto é independente', self.screen)
        self.assertIn('O AnyTV Connect instalado', self.screen)
        self.assertIn('Não foi possível abrir: ', self.screen)
        self.assertNotIn('details = null; detailsPackage = null\n                    if (pkg.isNotBlank()) searchOnTv', self.screen)

    def test_title_fallback_keeps_security_and_basic_launch(self):
        self.assertIn('val search = runCatching {', self.apps)
        self.assertIn('if (open(host, code, packageName)) "opened_app"', self.apps)
        self.assertIn('if (failure?.message?.contains("Código incorreto") == true) throw failure', self.apps)

    def test_licensed_source_offline_link_validation(self):
        # CI exercises the candidate commercial-source parser without API secrets.
        from scripts.audit_movieofthenight import inspect_link, collect_offers
        self.assertEqual(
            inspect_link("netflix", "https://www.netflix.com/title/80175798"),
            "candidate")
        self.assertEqual(
            inspect_link("disney", "https://www.disneyplus.com/browse/entity-8f08a8df-8b2f-49e1-9130-1e842b90f185"),
            "candidate")
        self.assertFalse(collect_offers({"streamingOptions": {"us": []}}, "prime")["found"])
        self.assertEqual(
            inspect_link("netflix", "https://evil.example/title/80175798"),
            "unsafe")

    def test_link_lookup_uses_backend_without_device_api_key(self):
        resolver = Path("overrides/AnyTvPersonalLinks.kt").read_text(encoding="utf-8")
        preparer = Path("scripts/prepare_tcl.py").read_text(encoding="utf-8")
        self.assertIn("overrides/AnyTvPersonalLinks.kt", preparer)
        for fragment in (
            'AnyTvDeviceIdentity.RESOLVER_BASE + "/v1/resolve/"',
            "validatedLink(", "NotInSubscription", "NoSupportedLink",
            "com.netflix.ninja", "com.disney.disneyplus",
            "com.apple.atve.androidtv.appletv", "com.wbd.stream",
        ):
            self.assertIn(fragment, resolver)
        identity = Path("overrides/AnyTvDeviceIdentity.kt").read_text(encoding="utf-8")
        self.assertIn('RESOLVER_BASE = "https://anytv-link-resolver-pilot.brenoff.workers.dev"', identity)
        self.assertIn("AnyTvPersonalLinks.resolve(context, title, packageName)", self.screen)
        self.assertIn("AnyTvDeviceIdentity.attachProof(context, conn, url.toURI().rawPath)", resolver)
        self.assertIn("overrides/AnyTvDeviceIdentity.kt", preparer)
        self.assertIn("StreamingCatalog.titleDeepLink(token, title, packageName)", self.screen)
        self.assertNotIn("AndroidKeyStore", resolver)
        self.assertNotIn("X-API-Key", resolver)
        self.assertNotIn("tokenDraft", self.screen)
        self.assertNotIn("personalKeyDraft", self.screen)

    def test_activation_ui_never_persists_codes_and_runs_off_main_thread(self):
        identity = Path("overrides/AnyTvDeviceIdentity.kt").read_text(encoding="utf-8")
        for fragment in (
            "Ativar links diretos neste dispositivo",
            "AnyTvDeviceIdentity.activate(context, submitted)",
            "withContext(Dispatchers.IO)",
            "activationDraft = \"\"",
        ):
            self.assertIn(fragment, self.screen)
        self.assertIn('connection.responseCode == 201', identity)
        self.assertIn('activationCode.matches(Regex(', identity)
        self.assertNotIn('putString("activation', identity)
        self.assertNotIn('putString("activation', self.screen)

    def test_tv_apple_playable_id_requires_allowlisted_parameters(self):
        from pathlib import Path
        bridge = Path("overrides/tv_safe/TvAppBridge.java").read_text(encoding="utf-8")
        self.assertIn('path.matches("/(?:[a-z]{2}/)?(?:movie|show)/', bridge)
        self.assertIn('query.matches("playableId=', bridge)
        self.assertIn('u.getQueryParameter("playableId")', bridge)
        self.assertIn('"tv.apple.com".equals(host)', bridge)

    def test_update_identity_is_31(self):
        self.assertIn('applicationId = "com.tclremote.livre"', self.gradle)
        self.assertIn("versionCode = " + ("39" if os.environ.get("TCL_V39") == "1" else "38" if os.environ.get("TCL_V38") == "1" else "37" if os.environ.get("TCL_V37") == "1" else "36" if os.environ.get("TCL_V36") == "1" else "35" if os.environ.get("TCL_V35") == "1" else "34"), self.gradle)

if __name__ == "__main__":
    unittest.main()
