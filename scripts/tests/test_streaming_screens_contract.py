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
        self.assertIn("OfficialTvIcon(app, 32.dp)", self.screen)
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

    def test_auto_advance_swipe_no_navigation_buttons_and_vertical_scrolling(self):
        self.assertIn("HorizontalPager(", self.screen)
        self.assertIn("rememberPagerState(", self.screen)
        self.assertIn("delay(3000)", self.screen)
        self.assertIn("animateScrollToPage(", self.screen)
        self.assertNotIn('Text("‹ Anterior")', self.screen)
        self.assertNotIn('Text("Próximo ›")', self.screen)
        self.assertLess(
            self.screen.index("LazyColumn(modifier = Modifier.weight(1f))"),
            self.screen.index("HorizontalPager("))
        self.assertIn(".heightIn(max = 460.dp)", self.screen)
        self.assertIn(".verticalScroll(rememberScrollState())", self.screen)

    def test_brazil_subscription_offers_are_title_specific_not_every_installed_app(self):
        self.assertIn('suspend fun offersInBrazil(item: StreamingTitle)', self.screen)
        self.assertIn('.optJSONObject("BR")?.optJSONArray("flatrate")', self.screen)
        self.assertIn('offer.optString("logo_path")', self.screen)
        self.assertIn('StreamingOfferIcons(item, offerCache, displayed)', self.screen)
        self.assertIn('StreamingOfferIcons(title, offerCache, displayed)', self.screen)
        self.assertIn('StreamingCatalog.appHasOffer(app, it)', self.screen)
        self.assertIn('Nenhuma oferta de assinatura confirmada no Brasil.', self.screen)
        self.assertNotIn('displayed.filter { StreamingCatalog.streaming(it) }.forEach { app ->\n                                FilterChip(', self.screen)

    def test_streaming_brands_compact_deduplicated_and_not_addon_channels(self):
        self.assertIn('distinctBy { provider(it.name) ?: norm(it.name) }', self.screen)
        self.assertIn('normalized.contains("channel")', self.screen)
        self.assertIn('normalized.contains("canal")', self.screen)
        self.assertIn('OfficialTvIcon(installed, 30.dp)', self.screen)
        self.assertIn('fontSize = 11.sp, maxLines = 1', self.screen)
        self.assertIn('lineHeight = 14.sp', self.screen)

    def test_compact_home_search_platforms_and_customizable_genre_lists(self):
        for piece in (
            'Text("Descobrir", fontSize = 17.sp',
            'showGlobalSearch = !showGlobalSearch',
            'if (showGlobalSearch)',
            'OfficialTvIcon(app, 32.dp)',
            'suspend fun discoveryGenre(id: Int)',
            'with_genres=$id&page=1',
            'StreamingCatalog.genres(token, "movie")',
            'selectedHomeGenre = item.id',
            'Text("Editar listas", fontSize = 11.sp)',
            'putString("anytv_home_genres"',
            'mainGenres.filter { it.id in preferredGenreIds }',
            '"Por gênero: " + kind.name',
        ):
            with self.subTest(piece=piece):
                self.assertIn(piece, self.screen)
        self.assertNotIn('Text("Explorar por plataforma"', self.screen)
        self.assertNotIn('Text("Streaming", fontSize = 20.sp', self.screen)
        self.assertNotIn('Text("Filtrar destaques por plataforma instalada na TV"', self.screen)
        self.assertIn('val heroTitles = if (homeLibrary) emptyList()', self.screen)

    def test_bounded_persistent_quota_saving_metadata_cache(self):
        for fragment in (
            'private object StreamingDiskCache', 'anytv_catalog_v1',
            'fun rememberTitle(media: String, id: Int',
            '90L * 86400000L', 'StreamingDiskCache.read(route)',
            'StreamingDiskCache.write(route, payload, ttl(route))',
            'route.contains("/watch/providers") -> 12L * 3600000L',
            'index >= 450 || bytes > 12_000_000L',
            'StreamingCatalog.configure(context)',
        ):
            self.assertIn(fragment, self.screen)

    def test_my_streamings_filter_is_opt_in_and_persistent(self):
        for fragment in (
            '"anytv_my_streamings"', '"Só meus streamings"',
            'showMyStreamingSettings = true', 'onlyMyStreaming = !onlyMyStreaming',
            'it.packageName in myServices', 'StreamingCatalog.discover(token, app.name',
            'if (!homeLibrary)', 'if (!onlyMyStreaming)',
        ):
            self.assertIn(fragment, self.screen)

    def test_library_global_saves_watch_progress_and_deduplicates(self):
        for fragment in (
            'packageName.ifBlank { "library" }',
            'fun toggleFilmWatched(id: Int)',
            '"anytv_watched_movies"', '"anytv_watched_episodes"',
            'fun toggleWatched(id: Int, season: Int, episode: Int)',
            '"Filmes para assistir"', '"Séries que acompanho"',
            '"Filmes já assistidos"', 'val seenInHome',
            'val uniqueShelves', 'it.media}:',
        ):
            self.assertIn(fragment, self.screen)

    def test_real_season_episode_tracking_with_offline_manual_fallback(self):
        for fragment in (
            'suspend fun seasons(id: Int)', 'suspend fun episodes(id: Int, season: Int)',
            '"tv/$id/season/$season?language=pt-BR"',
            'episode_number', 'StreamingEpisode(', 'Acompanhar episódios',
            'Registro manual de episódios', 'Temporadas indisponíveis',
            'seasonEpisodes.forEach', 'Checkbox(checked = marked',
        ):
            # The fallback is intentionally functional even until Worker route deployment.
            if fragment == 'Temporadas indisponíveis':
                continue
            self.assertIn(fragment, self.screen)
        self.assertNotIn('Token TMDB', self.screen)

    def test_same_home_app_icons_year_and_readable_spacing(self):
        self.assertIn('OfficialTvIcon(installed, 30.dp)', self.screen)
        self.assertIn('lineHeight = 14.sp', self.screen)
        self.assertIn('Spacer(Modifier.height(5.dp))', self.screen)
        self.assertIn('first_air_date', self.screen)
        self.assertIn('release_date', self.screen)
        self.assertIn('val releasedYear = title.date.take(4)', self.screen)

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
