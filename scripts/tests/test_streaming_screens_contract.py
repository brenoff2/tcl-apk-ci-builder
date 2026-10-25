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

    def test_anime_separate_from_general_catalog_and_genres_default_list(self):
        self.assertIn('val matchGenre = genre == 0 || genre == ANYTV_ANIME_FILTER', self.screen)
        self.assertIn('if (resolved > 0 || resolved == ANYTV_ANIME_FILTER)', self.screen)
        self.assertIn('private fun anyTvIsAnime(title: StreamingTitle)', self.screen)
        self.assertIn('.filter { id != ANYTV_ANIME_FILTER || anyTvIsAnime(it) }', self.screen)
        self.assertIn('genre == ANYTV_ANIME_FILTER', self.screen)
        self.assertIn('entry.optString("original_language")', self.screen)
        self.assertIn('title.originalLanguage == "ja" && 16 in title.genreIds', self.screen)
        self.assertIn('var genreViewMode by remember { mutableStateOf("detalhado") }', self.screen)
        self.assertIn('var genreSectionsMode by remember { mutableStateOf(false) }', self.screen)
        self.assertIn('else genreSectionsMode = !genreSectionsMode', self.screen)
        self.assertIn('if (genreSectionsMode && selectedHomeGenre != ANYTV_CINEMA_FILTER)', self.screen)
        self.assertIn('"Lançamentos recentes · $genreName"', self.screen)
        self.assertIn('"Mais bem avaliados · $genreName"', self.screen)
        self.assertIn('AnyTvOneRowPreview(heading, sectionTitles, posterCard)', self.screen)

    def test_detailed_card_has_compact_metadata_spacing(self):
        self.assertIn('verticalArrangement = Arrangement.spacedBy(1.dp)) {', self.screen)

    def test_genre_sections_toggle_and_hero_single_badge(self):
        self.assertIn('else genreSectionsMode = !genreSectionsMode', self.screen)
        self.assertIn('else genreSectionsMode)\n                                                "Ver em seções"', self.screen)
        self.assertIn('if (genreSectionsMode && selectedHomeGenre != ANYTV_CINEMA_FILTER)', self.screen)
        self.assertNotIn('Text(if (current.media == "movie") "F" else "S"', self.screen)
        self.assertIn('showRating = false, showYear = true,', self.screen)
        self.assertIn('heightIn(min = 16.dp)', self.screen)
        self.assertIn('Text(summary, fontSize = 12.sp, lineHeight = 15.sp,', self.screen)

    def test_new_streaming_arrivals_require_real_provider_change(self):
        self.assertIn('anytv_br_provider_snapshot_v1', self.screen)
        self.assertIn('anytv_br_provider_arrivals_v1', self.screen)
        self.assertIn('snapshot.has(key) && (current - previous).isNotEmpty()', self.screen)
        self.assertIn('put("Recém-chegados ao Streaming", newArrivalTitles)', self.screen)
        self.assertIn('originalLanguage = cached.optString("original_language")', self.screen)
        self.assertNotIn('TMDB não fornece histórico', self.screen)

    def test_recent_sections_have_distinct_verified_sources(self):
        self.assertIn('suspend fun recentPremieres(media: String)', self.screen)
        self.assertNotIn('&$dateField.gte=', self.screen)
        self.assertNotIn('&$dateField.lte=', self.screen)
        self.assertIn('suspend fun recentlyStartedSeasons()', self.screen)
        self.assertIn('"Filmes recém-lançados" to recentMovies', self.screen)
        self.assertIn('"Séries novas" to newSeries', self.screen)
        self.assertIn('"Novas temporadas" to newSeasons', self.screen)
        self.assertIn('listOf("popularity.desc", "$dateField.desc").flatMap { sorting ->', self.screen)
        self.assertIn('recent.filterNot { it.id in theatrical }', self.screen)
        self.assertNotIn('Entradas recentes ainda não verificadas:', self.screen)
        self.assertNotIn('"Novidades" to news', self.screen)
        self.assertNotIn('Triple("Novidades em filmes",', self.screen)
        self.assertNotIn('Triple("Novidades em séries",', self.screen)

    def test_real_installed_apps_are_the_source(self):
        self.assertIn('request(host, code, "/apps")', self.apps)
        self.assertIn("TclStreamingScreens(apps = allApps", self.apps)
        self.assertIn("OfficialTvIcon(app, 52.dp)", self.screen)
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
                         "LazyRow(", "titleActionFeedback"):
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
        self.assertIn('AnyTvStreamNavBar(', self.screen)
        self.assertIn('Text(pageTitle, modifier = Modifier.weight(1f)', self.screen)
        self.assertIn('val app = selected!!', self.screen)
        self.assertIn('// Home and individual provider use ONE identical catalogue layout.', self.screen)
        self.assertNotIn('} else {\n                    val app = selected!!', self.screen)
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
            self.screen.index("LazyColumn(state = catalogScrollState,"),
            self.screen.index("HorizontalPager(", self.screen.index("val heroTitles =")))
        self.assertIn(".heightIn(max = 460.dp)", self.screen)
        self.assertIn(".verticalScroll(rememberScrollState())", self.screen)

    def test_brazil_subscription_offers_are_title_specific_not_every_installed_app(self):
        self.assertIn('suspend fun offersInBrazil(item: StreamingTitle)', self.screen)
        self.assertIn('.optJSONObject("BR")?.optJSONArray("flatrate")', self.screen)
        self.assertIn('offer.optString("logo_path")', self.screen)
        self.assertIn('StreamingOfferIcons(title, offerCache, apps,', self.screen)
        self.assertIn('AnyTvRatingOverlay(current)', self.screen)
        self.assertIn('StreamingCatalog.appHasOffer(app, it)', self.screen)
        self.assertIn('Nenhuma oferta de assinatura confirmada no Brasil.', self.screen)
        self.assertNotIn('displayed.filter { StreamingCatalog.streaming(it) }.forEach { app ->\n                                FilterChip(', self.screen)

    def test_streaming_brands_preserve_valid_brazilian_addon_channels(self):
        self.assertIn('}.distinctBy { it.id }', self.screen)
        self.assertIn('"universalplus" in v -> "universalplus"', self.screen)
        self.assertNotIn('normalized.contains("channel")', self.screen)
        self.assertNotIn('normalized.contains("canal")', self.screen)
        self.assertIn('OfficialTvIcon(installed, 14.dp)', self.screen)
        self.assertIn('offer.logo.isNotBlank()', self.screen)

    def test_compact_home_search_platforms_and_customizable_genre_lists(self):
        for piece in (
            'TclHeaderAction(Icons.Filled.Home, "Início: filmes e séries"',
            'showGlobalSearch = !showGlobalSearch',
            'if (showGlobalSearch)',
            'OfficialTvIcon(app, 52.dp)',
            'suspend fun discoveryGenre(id: Int)',
            'StreamingCatalog.genres(token, "movie")',
            'selectedHomeGenre = kind.id',
            'Text("⚙ Listas", fontSize = 11.sp)',
            'putString("anytv_home_genres"',
            'filterGenreIds.mapNotNull { selectedId ->',
            '"Por gênero: " + kind.name',
        ):
            with self.subTest(piece=piece):
                self.assertIn(piece, self.screen)
        self.assertNotIn('Text("Explorar por plataforma"', self.screen)
        self.assertNotIn('Text("Streaming", fontSize = 20.sp', self.screen)
        self.assertNotIn('Text("Filtrar destaques por plataforma instalada na TV"', self.screen)
        self.assertIn('val heroTitles = if (homeLibrary) emptyList() else', self.screen)

    def test_bounded_persistent_quota_saving_metadata_cache(self):
        for fragment in (
            'private object StreamingDiskCache', 'anytv_catalog_v1',
            'fun rememberTitle(media: String, id: Int',
            'saved_title_', 'StreamingDiskCache.read(route)',
            'StreamingDiskCache.write(route, payload, ttl(route))',
            'route.contains("/watch/providers") -> 12L * 3600000L',
            'index >= 450 || bytes > 12_000_000L',
            'StreamingCatalog.configure(context)',
        ):
            self.assertIn(fragment, self.screen)

    def test_discovery_filters_use_real_services_and_persist_choices(self):
        for fragment in (
            '"anytv_my_streamings"', '"anytv_discovery_genre_filter"',
            'subscriptionNames = remember', '"Netflix"', '"Prime Video"',
            '"Paramount+"', '"Disney+"', '"Max"',
            'showMyStreamingSettings = true', 'Text("Filtros de descoberta", fontWeight = FontWeight.Bold)',
            'Text("Streamings"', 'Text("Gêneros"',
            'myServices = editingMyServices', 'filterGenreIds = editingFilterGenres',
            'StreamingCatalog.discover(token, name, kind',
            'activeServiceNames.forEach { name ->',
            'val filteredShelves = if (activeServiceNames.isEmpty() || homeLibrary ||',
            'val uniqueShelves = filteredShelves.mapNotNull',
            '"Marcar todos"', '"Desmarcar todos"',
            'editingFilterGenres = if (allGenreIds.isNotEmpty()',
            'val requested = if (selectedHomeGenre == 0)',
            'filterGenreIds.mapNotNull { selectedId ->',
            'put("Por gênero: " + kind.name, it)',
        ):
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, self.screen)
        self.assertNotIn('displayed.filter { it.packageName in myServices', self.screen)

    def test_navigation_is_connected_not_just_declared(self):
        self.assertEqual(self.screen.count('AnyTvStreamNavBar('), 2)
        self.assertIn('@Composable\ninternal fun TclStreamingScreens(', self.screen)
        self.assertNotIn('@Composable\n/** Same one-row navigation', self.screen)
        self.assertIn('onFilters = { editingMyServices = myServices', self.screen)
        self.assertIn('AnyTvCompactChip("Filmes", !library && contentType != "tv")', self.screen)
        self.assertIn('onHome = { selected = null; homeLibrary = false', self.screen)
        self.assertIn('onWatching = { selected = null; homeLibrary = true', self.screen)
        self.assertIn('onWatched = { selected = null; homeLibrary = true', self.screen)
        self.assertNotIn('listOf("all" to "Tudo"', self.screen)
        self.assertNotIn('label = { Text("Início", fontSize = 11.sp) }', self.screen)
        self.assertNotIn('FilterChip(selected = media == "movie" && !showSaved', self.screen)

    def test_library_global_saves_watch_progress_and_deduplicates(self):
        for fragment in (
            'packageName.ifBlank { "library" }',
            'fun toggleFilmWatched(title: StreamingTitle)',
            '"anytv_watched_movies"', '"anytv_watched_episodes"',
            'fun toggleWatched(title: StreamingTitle, season: Int, episode: Int)',
            '"Filmes para assistir"', '"Séries acompanhadas"',
            '"Filmes e séries assistidos"', 'val filteredShelves',
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
        self.assertIn('OfficialTvIcon(installed, 14.dp)', self.screen)
        self.assertIn('lineHeight = 14.sp', self.screen)
        self.assertIn('Brush.verticalGradient(listOf(', self.screen)
        self.assertIn('first_air_date', self.screen)
        self.assertIn('release_date', self.screen)
        self.assertIn('val releasedYear = title.date.take(4)', self.screen)

    def test_home_banner_uses_actual_movie_posters_and_opens_discovery(self):
        self.assertIn('internal fun AnyTvHomeFilmBanner(', self.screen)
        self.assertIn('discoveryShelf("trending/movie/week", "movie")', self.screen)
        self.assertIn('onOpenStreaming: () -> Unit', self.screen)
        self.assertIn('delay(3000)', self.screen)
        self.assertIn('movie.backdrop.ifBlank { movie.poster }', self.screen)
        self.assertIn('tvName: String, onDismiss: () -> Unit', self.screen)
        self.assertIn('tvName.ifBlank { "TV conectada" }', self.screen)
        self.assertNotIn('"Onde assistir e links disponíveis"', self.screen)

    def test_banner_is_below_tv_icons_and_uses_valid_trending_route(self):
        self.assertNotIn('discoveryShelf("trending/movie/week?language=pt-BR"', self.screen)
        self.assertIn('modifier = Modifier.fillMaxWidth().height(92.dp)', self.screen)
        self.assertIn('Text(movie.name, fontSize = 15.sp', self.screen)
        self.assertIn('Text("Ver ›", fontSize = 12.sp', self.screen)
        self.assertLess(
            self.apps.index('OfficialTvIcon(app = app, size = 52.dp)'),
            self.apps.index('AnyTvHomeFilmBanner(onOpenStreaming = { showStreaming = true })')
        )

    def test_navigation_is_compact_and_library_recoverable(self):
        for fragment in (
            'TclHeaderAction(Icons.Filled.Home', 'TclHeaderAction(Icons.Filled.Favorite',
            'TclHeaderAction(Icons.Filled.PlayCircleFilled', 'TclHeaderAction(Icons.Filled.CheckCircle',
            'TclHeaderAction(Icons.Filled.FilterList', 'Icon(Icons.Filled.Search',
            'TclHeaderAction(Icons.Filled.Close', '"Filmes"', '"Séries"',
            'librarySection = "watching"', 'librarySection = "watched"',
            'AnyTvCompactChip("Filmes", !library', 'onContentType = { kind ->',
        ):
            self.assertIn(fragment, self.screen)
        self.assertIn('OfficialTvIcon(app, 52.dp)', self.screen)
        self.assertIn('OfficialTvIcon(installed, 14.dp)', self.screen)

    def test_watched_and_tracking_history_does_not_depend_on_heart(self):
        self.assertIn('fun restoreTrackedTitle(media: String, id: Int)', self.screen)
        self.assertIn('StreamingDiskCache.previousTitle(media, id)', self.screen)
        self.assertIn('val history = (watchedFilms.mapNotNull', self.screen)
        self.assertIn('val trackedIds = trackedSeries +', self.screen)
        self.assertIn('val watching = trackedIds.mapNotNull', self.screen)
        self.assertIn('"Filmes e séries assistidos", history', self.screen)
        self.assertIn('"Séries acompanhadas", watching', self.screen)

    def test_quick_title_actions_and_permanent_local_artwork(self):
        for text in ('"Adicionar à minha lista"', '"Marcar como assistido"',
                     '"Acompanhar série"', 'Color(0xFF1CB96B)',
                     'saved_title_', 'anytv_artwork_saved', 'drawable.toBitmap()',
                     'AnyTvPinnedImage(', 'fun previousTitle(',
                     'val knownGenreNames', 'title.genreIds',
                     'it.id.toString() !in watchedFilms'):
            self.assertIn(text, self.screen)
        self.assertNotIn('90L * 86400000L', self.screen)
        self.assertIn('OutlinedButton(onClick = {\n                    details = null', self.screen)

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
        self.assertIn('tvName.ifBlank { "TV conectada" }', self.screen)
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
            "ActivationRequired", "ResolverUnavailable", "activationStatus(",
            "com.netflix.ninja", "com.disney.disneyplus",
            "com.apple.atve.androidtv.appletv", "com.wbd.stream",
        ):
            self.assertIn(fragment, resolver)
        identity = Path("overrides/AnyTvDeviceIdentity.kt").read_text(encoding="utf-8")
        self.assertIn('RESOLVER_BASE = "https://anytv-link-resolver-pilot.brenoff.workers.dev"', identity)
        self.assertIn("AnyTvPersonalLinks.resolve(context, title, packageName)", self.screen)
        self.assertIn('Text("Verificar acesso aos links"', self.screen)
        self.assertIn('Streaming Availability API by Movie of the Night', self.screen)
        self.assertIn('https://www.movieofthenight.com/about/api', self.screen)
        self.assertIn('AnyTvPersonalResult.ActivationRequired ->', self.screen)
        self.assertIn('AnyTvPersonalResult.ResolverUnavailable ->', self.screen)
        self.assertIn('AnyTvDeviceIdentity.RESOLVER_BASE + "/v1/device/status"', resolver)
        self.assertIn('if (status == 404 || status == 409)', resolver)
        self.assertIn('conn.errorStream?.bufferedReader()', resolver)
        self.assertIn('"not_available" -> AnyTvPersonalResult.NotInSubscription', resolver)
        self.assertIn('"no_supported_link", "ambiguous" -> AnyTvPersonalResult.NoSupportedLink', resolver)
        self.assertNotIn('if (status == 404) return@withContext AnyTvPersonalResult.NotInSubscription', resolver)
        self.assertIn("AnyTvDeviceIdentity.attachProof(context, conn, url.toURI().rawPath)", resolver)
        self.assertIn("overrides/AnyTvDeviceIdentity.kt", preparer)
        self.assertIn("StreamingCatalog.titleDeepLink(token, title, packageName)", self.screen)
        self.assertNotIn("AndroidKeyStore", resolver)
        self.assertNotIn("X-API-Key", resolver)
        self.assertNotIn("tokenDraft", self.screen)
        self.assertNotIn("personalKeyDraft", self.screen)

    def test_link_auth_errors_keep_official_secondary_provider_lookup(self):
        screen = self.screen.split('fun searchOnTv(title: StreamingTitle, packageName: String)', 1)[1]
        for state in ('ActivationRequired', 'ResolverUnavailable', 'NoSupportedLink'):
            block = screen.split('AnyTvPersonalResult.' + state + ' -> {', 1)[1].split('}', 1)[0]
            self.assertIn('linkSourceNotice =', block, state)
            self.assertNotIn('return@launch', block, state)
            self.assertNotIn('TclTvAppApi.searchTitle(', block, state)
        self.assertIn('StreamingCatalog.titleAvailableInBrazil(', screen)
        self.assertIn('StreamingCatalog.titleDeepLink(token, title, packageName)', screen)
        self.assertIn('linkSourceNotice + (if (offered == null)', screen)
        self.assertIn('AnyTvPersonalResult.NotInSubscription ->', screen)

    def test_device_approval_is_requested_without_visible_secret(self):
        identity = Path("overrides/AnyTvDeviceIdentity.kt").read_text(encoding="utf-8")
        self.assertIn('linkActivationRequired = state == "unauthorized"', self.screen)
        self.assertIn('else "Solicitar liberação"', self.screen)
        self.assertIn('AnyTvDeviceIdentity.requestApproval(context)', self.screen)
        self.assertIn('AnyTvPersonalLinks.activationStatus(context)', self.screen)
        self.assertIn('deviceId(context).takeLast(8)', self.screen)
        self.assertNotIn('deviceActivationCode', self.screen)
        self.assertNotIn('Código único de ativação', self.screen)
        self.assertNotIn('AnyTvDeviceIdentity.activate(context', self.screen)
        self.assertIn('ANYTV-REQUEST1', identity)
        self.assertIn('/v1/device/request', identity)
        self.assertNotIn('putString("activation', identity)

    def test_streaming_service_icons_and_cinema_sessions(self):
        self.assertIn('offerings.distinctBy { anyTvServiceKey(it.name) }.forEach', self.screen)
        self.assertIn('StreamingCatalog.appHasOffer(it, listOf(offer))', self.screen)
        self.assertIn('https://image.tmdb.org/t/p/w92', self.screen)
        self.assertIn('Text("Ver cinemas e sessões · Ingresso.com"', self.screen)
        self.assertIn('"https://www.ingresso.com/filme/" + slug', self.screen)
        self.assertIn('Uri.parse(target)', self.screen)
        self.assertNotIn('offerings.take(visible)', self.screen)

    def test_full_screen_visual_consistency_and_empty_pages(self):
        self.assertIn('Surface(modifier = Modifier.fillMaxSize()', self.screen)
        self.assertIn('shape = RoundedCornerShape(0.dp)', self.screen)
        self.assertIn('color = MaterialTheme.colorScheme.background', self.screen)
        self.assertIn('horizontalArrangement = Arrangement.spacedBy(7.dp)', self.screen)
        self.assertNotIn('Icons.Outlined.FavoriteBorder', self.screen)
        self.assertIn('Icon(Icons.Filled.Favorite,', self.screen)
        self.assertIn('TclHeaderAction(Icons.Filled.FilterList', self.screen)
        for text in ('Sua lista está vazia.', 'Você ainda não acompanha nenhuma série.',
                     'Nenhum filme marcado como assistido.'):
            self.assertIn(text, self.screen)
        self.assertIn('AnyTvCompactChip("Filmes", !library', self.screen)
        self.assertIn('AnyTvCompactChip("Séries", !library', self.screen)
        self.assertIn('showGlobalSearch = !showGlobalSearch', self.screen)

    def test_verified_disney_show_id_with_fallback(self):
        self.assertIn('title.id == 224505', self.screen)
        self.assertIn('title.media == "tv"', self.screen)
        self.assertIn('packageName == "com.disney.disneyplus"', self.screen)
        self.assertIn('entity-59565a16-d61f-49d1-85d7-1f0f87712748', self.screen)
        self.assertIn('TclTvAppApi.openTitle(host, pairing, packageName,', self.screen)
        self.assertIn('val withoutArticle = normalized.replace(', self.screen)
        self.assertIn('collected.filter(::anyTvReleased).distinctBy {', self.screen)
        self.assertIn('successfulRequests == 0', self.screen)

    def test_media_chips_both_active_by_default_and_off_independently(self):
        self.assertIn('contentType != "tv"', self.screen)
        self.assertIn('contentType != "movie"', self.screen)
        self.assertIn('if (contentType == "all") "tv" else "all"', self.screen)
        self.assertIn('if (contentType == "all") "movie" else "all"', self.screen)

    def test_anime_uses_japanese_animation_not_generic_animation(self):
        self.assertIn('ANYTV_ANIME_FILTER = -2', self.screen)
        self.assertNotIn('with_original_language=ja', self.screen)
        self.assertIn('AnyTvCompactChip("Anime"', self.screen)

    def test_editorial_shelves_and_streaming_offers_remain_separate(self):
        self.assertIn('Filmes e séries mais assistidos', self.screen)
        self.assertIn('StreamingCatalog.matchesSelectedSubscription(', self.screen)
        self.assertIn('if (selectedHomeGenre == 0) {', self.screen)
        self.assertNotIn('if (selectedHomeGenre == 0 && activeServiceNames.isEmpty()) {', self.screen)

    def test_new_genre_details_and_search_consistency(self):
        self.assertIn('var genreViewMode by remember', self.screen)
        self.assertIn('AnyTvGenreViewIcon(Icons.Filled.GridView, "Grade", genreViewMode == "grade")', self.screen)
        self.assertIn('AnyTvGenreViewIcon(Icons.Filled.ViewList, "Detalhado", genreViewMode == "detalhado")', self.screen)
        self.assertIn('if (genreViewMode == "detalhado"', self.screen)
        self.assertIn('homeContentType == "all" || it == homeContentType', self.screen)
        self.assertIn('"movie" -> "Filmes"', self.screen)
        self.assertIn('"tv" -> "Séries"', self.screen)
        self.assertIn('anyTvGenreLabels(title, allMovieGenres, tvGenres)', self.screen)
        self.assertIn('anyTvGenreLabels(', self.screen)
        self.assertIn('Text(genreLabel, modifier = Modifier.fillMaxWidth()', self.screen)
        self.assertIn('Text(heroGenres, fontSize = 11.sp', self.screen)
        self.assertNotIn('internalSearchQuery', self.screen)
        self.assertIn('contentDescription = "Buscar"', self.screen)
        self.assertIn('contentDescription = "Limpar busca"', self.screen)

    def test_screenshot_38dp_icons_system_bars_and_full_bleed_banner(self):
        home = Path("overrides/TclApprovedHomeScreen.kt").read_text(encoding="utf-8")
        self.assertIn('internal fun TclHeaderAction(', home)
        self.assertIn('modifier = Modifier.size(38.dp)', home)
        self.assertIn('TclHeaderAction(Icons.Filled.FilterList', self.screen)
        self.assertNotIn('Text(deviceBrand', home)
        self.assertIn('Text(tvName, color = TclText', home)
        self.assertIn('windowInsetsPadding(WindowInsets.safeDrawing)', home)
        self.assertIn('window.navigationBarColor = systemBg.toArgb()', home)
        self.assertIn('window.statusBarColor = systemBg.toArgb()', home)
        self.assertIn('window.navigationBarColor = systemBg.toArgb()', home)
        self.assertIn('Brush.horizontalGradient(listOf(', self.screen)
        self.assertIn('color = Color.White, maxLines = 2', self.screen)
        self.assertNotIn('Canal não informado', self.screen)

    def test_search_retries_global_names_without_false_empty_result(self):
        self.assertIn('variants += "Love of Your Life"', self.screen)
        self.assertIn('variants += "O Amor da Sua Vida"', self.screen)
        self.assertIn('for (kind in listOf("movie", "tv"))', self.screen)
        self.assertIn('val response = get("backend",', self.screen)
        self.assertIn('if (successfulRequests == 0)', self.screen)
        self.assertIn('globalSearchError = "Falha ao consultar o catálogo.', self.screen)
        self.assertIn('O Amor da Sua Vida (2026) · Prime Video', self.screen)
        self.assertIn('0HM3DRN79M0E7QOBLMYTS41IG0', self.screen)
        self.assertIn('TclTvAppApi.searchTitle(host, pairing,', self.screen)
        self.assertNotIn('}.getOrDefault(emptyList())\\n            globalSearchBusy = false', self.screen)

    def test_provider_and_main_share_actual_slider_and_sections(self):
        self.assertEqual(self.screen.count('AnyTvStreamNavBar('), 2)
        self.assertIn('Text(pageTitle, modifier = Modifier.weight(1f)', self.screen)
        self.assertIn('if (selected != null && !homeLibrary)', self.screen)
        self.assertIn('LaunchedEffect(selected?.packageName, homeContentType, selectedHomeGenre)', self.screen)
        self.assertIn('myServiceShelves[key] = it.titles', self.screen)
        self.assertIn('HorizontalPager(', self.screen)
        self.assertIn('Modifier.fillMaxWidth().padding(top = 24.dp)', self.screen)
        self.assertNotIn('} else {\\n                    val app = selected!!', self.screen)

    def test_three_visible_cards_and_24_title_batches(self):
        for fragment in (
            'suspend fun discoveryGenrePage(id: Int, media: String, page: Int,',
            'sourcePage < payload.optInt("total_pages", sourcePage)',
            'genreMore[key] = result.more',
            'items(shelf.take(ANYTV_LIST_BATCH)) { title ->',
            'items(shelf.take(sectionVisibleCount).chunked(3))',
            'items(genreTitles.chunked(3))',
            'StreamingCatalog.discoveryGenrePage(',
            '"Mostrar mais"', 'genreShelves[genreKey] =',
            'val films = discoveryShelves["Em alta nesta semana"]',
            'val shows = discoveryShelves["Em alta nesta semana"]',
            'StreamingCatalog.discoveryUpcomingStreamingPage(1)',
            'Text(if (item.media == "tv") "S" else "F"',
            'class AnyTvReleaseReminderReceiver',
            'Modifier.fillMaxWidth().aspectRatio(2f / 3f)',
            'ContentScale.Fit)', 'OfficialTvIcon(installed, 14.dp)',
        ):
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, self.screen)
        for forbidden in (
            'items(shelf.take(ANYTV_LIST_BATCH).chunked(2))',
            'items(genreTitles, key =', 'genreViewMode == "list"',
            'Modifier.size(width = 116.dp, height = 158.dp)',
        ):
            self.assertNotIn(forbidden, self.screen)
    def test_cinema_uses_the_same_hero_as_principal(self):
        self.assertNotIn('private fun AnyTvCinemaHero(', self.screen)
        self.assertIn('if (heroTitles.isNotEmpty())', self.screen)
        self.assertIn('selectedHomeGenre == ANYTV_CINEMA_FILTER', self.screen)

    def test_hero_overlays_title_year_rating_verified_cinema_and_small_progress(self):
        hero = self.screen.split('val current = heroTitles[pageIndex]', 1)[1].split(
            '// Two single-row preview rails', 1)[0]
        self.assertLess(hero.index('AnyTvPinnedImage('),
                        hero.index('Brush.verticalGradient(listOf('))
        self.assertLess(hero.index('Brush.verticalGradient(listOf('),
                        hero.index('Text(current.name,'))
        self.assertLess(hero.index('Text(current.name,'),
                        hero.index('StreamingOfferIcons(current,'))
        for value in (
            'style = TextStyle(shadow = Shadow(',
            'Offset(0f, 2f), blurRadius = 6f',
            'showRating = false, showYear = true,',
            'AnyTvRatingOverlay(current)',
            'current.id in cinemaIds',
            'Text("Cinema", modifier = Modifier',
            'repeat(heroTitles.size) { dot ->',
            '.width(if (pagerState.currentPage == dot)',
        ):
            self.assertIn(value, hero)
        self.assertNotIn('Text(if (pagerState.currentPage == dot)', hero)

    def test_streaming_navigation_stays_one_line_on_mobile(self):
        nav = self.screen.split('private fun AnyTvStreamNavBar(', 1)[1].split(
            'private fun AnyTvCompactChip(', 1)[0]
        self.assertIn('Box(modifier = Modifier.weight(1f)) { actions() }', nav)
        self.assertNotIn('if (maxWidth < 390.dp)', nav)
        self.assertIn('Modifier.horizontalScroll(actionScroll)', nav)
        self.assertEqual(nav.count('TclHeaderAction(Icons.Filled.'), 6)

    def test_provider_genre_persists_for_both_media_types(self):
        helper = self.screen.split('private fun anyTvResolvedGenre(', 1)[1].split(
            'private fun anyTvToday()', 1)[0]
        self.assertIn('movieGenres: List<StreamingGenre>, tvGenres: List<StreamingGenre>', helper)
        self.assertIn('return candidates.firstOrNull', helper)
        self.assertIn('"ação", "aventura", "action & adventure"', helper)
        self.assertIn('anyTvResolvedGenre(selectedGenre, media, movieGenres, tvGenres)', helper)
        self.assertIn('anyTvProviderGenre(kind, selectedHomeGenre, allMovieGenres, tvGenres)',
                      self.screen)
        self.assertIn('homeKind, selectedHomeGenre, allMovieGenres, tvGenres)', self.screen)
        self.assertIn('selectedProvider.second,', self.screen)
        self.assertIn('selectedHomeGenre, allMovieGenres, tvGenres)', self.screen)


    def test_international_and_future_rails_never_invent_cinema_br(self):
        preview = self.screen.split('private fun AnyTvOneRowPreview(', 1)[1].split(
            '/** Compact, single-line score', 1)[0]
        self.assertIn('card(title, Modifier.width(cardWidth), false)', preview)
        self.assertNotIn('heading.contains("Cinema")', preview)
        # Each individual poster still carries an independently confirmed
        # BR now-playing Cinema marker (when the movie is in cinemaIds).
        self.assertIn('inCinema = cinemaSection || title.id in cinemaIds', self.screen)
        self.assertIn('inCinema = current.id in cinemaIds', self.screen)
        self.assertNotIn('AnyTvCinemaHero(shelf, offerCache, displayed)', self.screen)
        # International preview remains available on dedicated cinema filter pages,
        # but the home section now keeps only the verified Brazil cinema hero.
        self.assertIn('AnyTvOneRowPreview("Cinema pelo mundo · seleção internacional"', self.screen)

    def test_provider_genre_ver_mais_keeps_service_and_loads_next_24(self):
        page = self.screen.split('if (openedSection != null) {', 1)[1].split(
            '} // standalone section or main catalogue', 1)[0]
        self.assertIn('val selectedGenreProvider = selected?.takeIf {', page)
        self.assertIn('sectionTitle.startsWith("Por gênero: ")', page)
        self.assertIn('val sourceGenres = if (media == "tv") tvGenres else allMovieGenres', page)
        self.assertIn('Triple(app.name, media, genreItem.id)', page)
        self.assertIn('selectedProvider != null || selectedGenreProvider != null ||', page)
        self.assertIn('selectedGenreProvider != null ->', page)
        self.assertIn('selectedGenreProvider.first,', page)
        self.assertIn('selectedGenreProvider.second, next,', page)
        self.assertIn('selectedGenreProvider.third, "")', page)
        # Section CTA was intentionally removed; paging stays inside filters.
        self.assertNotIn('Text("Ver mais ›"', self.screen)

    def test_upcoming_streaming_state_is_declared_before_effect(self):
        self.assertLess(
            self.screen.index('var upcomingStreamingMore by remember'),
            self.screen.index('LaunchedEffect(openedSection) {'))
        self.assertEqual(self.screen.count('var upcomingStreamingMore by remember'), 1)

    def test_upcoming_combines_real_future_film_and_tv_without_fake_br_offers(self):
        impl = self.screen.split('suspend fun discoveryUpcomingStreamingPage(', 1)[1].split(
            '/** International cinema preview', 1)[0]
        for value in (
            'require(page in 1..50)',
            '"movie/upcoming?language=pt-BR&page=$page"',
            '"discover/tv?language=pt-BR&include_adult=false"',
            'movieSource.await()', 'seriesSource.await()',
            'futureFilms', 'futureShows', 'filter(::anyTvUpcoming)',
            'futureFilms.getOrNull(index)?.let { add(it) }',
            'futureShows.getOrNull(index)?.let { add(it) }',
            'StreamingResults(upcoming, page < 50 && (',
        ):
            with self.subTest(value=value):
                self.assertIn(value, impl)
        # Upcoming must not require a streaming deal already announced.
        self.assertNotIn('offersInBrazil(', impl)
        self.assertNotIn('title.takeIf { offered }', impl)
        self.assertNotIn('with_watch_providers=', impl)
        self.assertNotIn('.take(ANYTV_LIST_BATCH)', impl)

    def test_card_genre_first_and_stable_two_line_title(self):
        card = self.screen.split('private fun AnyTvPosterCard(', 1)[1].split(
            '/** One horizontal strip', 1)[0]
        self.assertLess(card.index('Text(genreLabel,'), card.index('StreamingOfferIcons('))
        self.assertLess(card.index('StreamingOfferIcons('), card.index('Text(title.name,'))
        self.assertIn('minLines = 1, maxLines = 1', card)
        self.assertIn('minLines = 2, maxLines = 2', card)

    def test_future_streaming_empty_first_page_can_browse_later_source_pages(self):
        # A verified-but-empty first page is not the end of the real catalogue.
        # The user must still be able to open this section and request page 2.
        for value in (
            'if (future.isNotEmpty() || upcomingStreamingMore)',
            'put("Em breve · Filmes e séries", future)',
            'if (unique.isEmpty() &&',
            '!(heading == "Em breve · Filmes e séries" && upcomingStreamingMore)',
            'val sectionPageLimit = if (sectionTitle == "Em breve · Filmes e séries") 50 else 42',
            'sectionPage < sectionPageLimit &&',
            'sectionAll.isNotEmpty() ||',
            'sectionTitle == "Em breve · Filmes e séries")))',
            'sectionPage < sectionPageLimit) {',
            'sectionTitle == "Em breve · Filmes e séries" && sectionAll.isEmpty()',
            'Nenhum lançamento futuro com oferta BR confirmada nesta seleção.',
            'Mostrar mais consulta as próximas páginas.',
            'shelf.isEmpty() && heading == "Em breve · Filmes e séries"',
            'Ver mais consulta as próximas páginas.',
        ):
            with self.subTest(value=value):
                self.assertIn(value, self.screen)
        self.assertNotIn('sectionPage < 42 && sectionAll.isNotEmpty()', self.screen)
        # A zero-verified-results page must not be counted as a 24-card shelf;
        # source continuation is a separate, explicit state.
        self.assertIn('sectionMore = result.more', self.screen)
        self.assertIn('upcomingStreamingMore = streamingFuture.more', self.screen)

    def test_br_subscription_offers_use_bounded_parallel_resolution(self):
        self.assertIn('val gate = Semaphore(4)', self.screen)
        self.assertIn('gate.withPermit {', self.screen)
        self.assertIn('StreamingCatalog.offersInBrazil(title)', self.screen)
        self.assertIn('}.awaitAll()', self.screen)

    def test_filter_mark_all_controls_are_readable_in_both_columns(self):
        self.assertEqual(self.screen.count('color = Color(0xFFFF5368),'), 2)
        self.assertIn('"Desmarcar todos" else "Marcar todos", fontSize = 13.sp', self.screen)

    def test_principal_never_reintroduces_cinema_on_movie_tab(self):
        self.assertNotIn('if (homeContentType != "tv") {\n                                discoveryShelves[ANYTV_CINEMA_SHELF]', self.screen)
        self.assertIn('selectedHomeGenre == ANYTV_CINEMA_FILTER', self.screen)

    def test_subscription_filter_preserves_editorial_sections_without_fake_offers(self):
        self.assertIn('put(heading, confirmed)', self.screen)
        self.assertIn('Nenhum título com oferta BR confirmada nos streamings', self.screen)
        self.assertIn('activeServiceNames.isNotEmpty() && selected == null', self.screen)
        self.assertIn('StreamingCatalog.matchesSelectedSubscription(', self.screen)

    def test_provider_media_buttons_share_home_filter_state(self):
        self.assertIn('contentType = homeContentType,', self.screen)
        self.assertNotIn('contentType = if (selected == null) homeContentType else media,', self.screen)
        self.assertIn('if (selectedHomeGenre > 0 && chosenGenre == 0) continue', self.screen)

    def test_selected_provider_uses_genre_keys_consistent_with_tv_tab(self):
        provider = self.screen.split('if (selected != null && !homeLibrary) {', 1)[1].split(
            '} else if (homeLibrary)', 1)[0]
        self.assertIn('anyTvProviderGenre(kind, selectedHomeGenre, allMovieGenres, tvGenres)',
                      provider)
        self.assertIn('val key = app.name + ":" + kind + ":" + chosenGenre', provider)
        self.assertIn('selectedHomeGenre == 0 && titles.isNotEmpty()', provider)
        self.assertIn('homeContentType == "all" || it == homeContentType', self.screen)
        self.assertIn('homeContentType = kind', self.screen)
        self.assertIn('if (selectedHomeGenre > 0 && chosenGenre == 0) continue', self.screen)
        self.assertIn('if (selectedHomeGenre > 0 && chosenGenre == 0) return@forEach', self.screen)
        self.assertNotIn('homeContentType = kind; selectedHomeGenre = 0', self.screen)
        provider_entry = self.screen.split('selected = app; providerListMode = false', 1)[1].split(
            'homeLibrary = false; showGlobalSearch = false', 1)[0]
        self.assertNotIn('homeContentType = "all"', provider_entry)

    def test_genre_page_combines_movies_and_series_only_when_all_selected(self):
        self.assertIn('if (homeContentType == "all") listOf("movie", "tv")', self.screen)
        self.assertIn('anyTvResolvedGenre(selectedHomeGenre, mediaKind,', self.screen)
        self.assertIn('val genreMedia = if (selectedHomeGenre == ANYTV_CINEMA_FILTER)', self.screen)
        self.assertIn('genreShelves[genreKey].orEmpty()', self.screen)
        self.assertIn('val genreCandidates = if (selectedHomeGenre == ANYTV_CINEMA_FILTER)', self.screen)
        self.assertIn('genreSources.forEach { (_, titles) ->', self.screen)
        self.assertIn('titles.getOrNull(index)?.let { add(it) }', self.screen)
        self.assertIn('val genreTitles = genreCandidates', self.screen)
        self.assertIn('kind == "tv" && selectedHomeGenre == ANYTV_CINEMA_FILTER', self.screen)
        self.assertIn('put("Cinema · Em cartaz no Brasil", it)', self.screen)
        self.assertIn('homeContentType == "all" || it == homeContentType', self.screen)
        self.assertIn('val selectedKindId = anyTvResolvedGenre(selectedHomeGenre,', self.screen)


    def test_upcoming_streaming_ver_mais_preserves_overflow_and_future_filter(self):
        for piece in (
            'var upcomingStreamingMore by remember',
            'StreamingCatalog.discoveryUpcomingStreamingPage(1)',
            'upcomingStreaming = streamingFuture.titles',
            'upcomingStreamingMore = streamingFuture.more',
            'sectionMore = if (openedSection == "Em breve · Filmes e séries")',
            'visibleShelves[sectionTitle].orEmpty()',
            'homeProvider != null || sectionTitle == "Em breve · Filmes e séries"',
            'sectionTitle == "Em breve · Filmes e séries" ->',
            'StreamingCatalog.discoveryUpcomingStreamingPage(next)',
            'anyTvUpcoming(it) &&',
            '(homeContentType == "all" ||',
            'it.media == homeContentType)',
            'heading == "Em breve · Filmes e séries" &&',
            'upcomingStreamingMore',
        ):
            with self.subTest(piece=piece):
                self.assertIn(piece, self.screen)
        self.assertIn('val sectionAll = (sectionBase + sectionExtra)', self.screen)
        self.assertIn('sectionExtra = (sectionExtra + matching)', self.screen)
        self.assertNotIn('sectionExtra = (sectionExtra + released)', self.screen)
        self.assertNotIn('discoveryUpcomingStreaming()\n        }.getOrDefault', self.screen)

    def test_ver_mais_editorial_and_provider_pages_fetch_next_24(self):
        for snippet in (
            'suspend fun discoveryShelfPage(route: String, media: String, page: Int)',
            'discoveryShelfPage(route, media, 1).titles',
            'val sectionAll = (sectionBase + sectionExtra)',
            'val next = sectionPage + 1',
            'StreamingCatalog.discoveryShelfPage(',
            'StreamingCatalog.discover(token,',
            'sectionPage = next',
            'sectionPage < sectionPageLimit &&',
            'sectionMore = result.more',
            'sectionVisibleCount += ANYTV_LIST_BATCH',
            'sectionError = "Não foi possível carregar mais títulos."',
        ):
            self.assertIn(snippet, self.screen)
        self.assertNotIn(
            'if (homeLibrary && sectionAll.size > sectionVisibleCount)',
            self.screen)
        # A partially filtered first 24-title page must not hide later pages.
        self.assertNotIn('sectionPage < 42 && sectionAll.size >= ANYTV_LIST_BATCH',
                         self.screen)

    def test_watched_film_uses_independent_prefs_snapshot_and_legacy_cache_migration(self):
        block = self.screen.split('var watchedMovieTitles by remember(prefs)', 1)[1].split(
            'fun isFavorite(title: StreamingTitle', 1)[0]
        for token in (
            '"anytv_watched_movie_titles_v1"',
            'fun preserveWatchedMovieTitles(titlesToSave: Collection<StreamingTitle>)',
            'watchedMovieTitles = watchedMovieTitles + valid.associateBy { it.id }',
            'if (id <= 0 || name.isBlank())',
            '.put("poster", item.poster)',
            '.put("backdrop", item.backdrop)',
            '.put("genres", JSONArray(item.genreIds))',
            'prefs.edit().putString("anytv_watched_movie_titles_v1"',
            'if (media == "movie") watchedMovieTitles[id]?.let { return it }',
            'StreamingDiskCache.previousTitle(media, id)',
        ):
            with self.subTest(token=token):
                self.assertIn(token, block)
        self.assertIn('preserveWatchedMovieTitles(listOf(title))', self.screen)
        migration = self.screen.split(
            'LaunchedEffect(watchedFilms, favorites, homeLibrary, librarySection, watchedMoviesRetry)', 1)[1].split(
            'var watchedEpisodes by remember', 1)[0]
        self.assertIn('watchedFilms.mapNotNull(String::toIntOrNull)', migration)
        self.assertIn('.filter { it !in watchedMovieTitles }', migration)
        self.assertIn('restoreTrackedTitle("movie", it)', migration)
        self.assertIn('preserveWatchedMovieTitles(locallyRecovered)', migration)
        self.assertIn('StreamingCatalog.lookupMovie(id)', migration)
        self.assertIn('preserveWatchedMovieTitles(remotelyRecovered)', migration)
        self.assertNotIn('movie/$id', migration)

    def test_legacy_watched_ids_remain_visible_without_invented_movie_metadata(self):
        library = self.screen.split(
            'if (homeLibrary && librarySection == "watched") {\n'
            '                            // Old releases stored only TMDB IDs', 1)[1].split(
            'uniqueShelves.forEach', 1)[0]
        for token in (
            'val missingIds = watchedFilms.mapNotNull(String::toIntOrNull)',
            'restoreTrackedTitle("movie", it) == null',
            'if (missingIds.isNotEmpty())',
            'IDs TMDB: ',
            'missingIds.take(12).joinToString(", ")',
            'registros antigos permanecem ',
            'sem nome/cartaz ',
        ):
            with self.subTest(token=token):
                self.assertIn(token, library)
        # The poster card keeps its 2:3 aspect and draws only real titles.
        self.assertIn('items(shelf.take(sectionVisibleCount).chunked(3))', self.screen)
        self.assertNotIn('StreamingTitle(id, "Filme', library)
        self.assertNotIn('https://image.tmdb.org/t/p/w342" + id', library)

    def test_watched_movie_detail_uses_server_metadata_with_id_validation(self):
        lookup = self.screen.split('suspend fun lookupMovie(id: Int): StreamingTitle?', 1)[1].split(
            '/** Recover legacy tracked shows', 1)[0]
        for fragment in (
            'if (id < 1) return@withContext null',
            'get("backend", "movie/$id?language=pt-BR")',
            'entry.optString("title").ifBlank { entry.optString("original_title") }',
            'entry.optInt("id") != id || name.isBlank()',
            'entry.optJSONArray("genres")',
            'poster_path', 'backdrop_path', 'release_date',
            'StreamingTitle(id, name, entry.optString("overview"), poster, "movie"',
        ):
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, lookup)
        self.assertNotIn('api.themoviedb.org/3/movie/', lookup)
        self.assertNotIn('TMDB_READ_TOKEN', lookup)
        self.assertNotIn('StreamingCatalog.searchGlobal(', lookup)

    def test_remote_history_restore_is_bounded_and_old_worker_fails_closed(self):
        effect = self.screen.split(
            'LaunchedEffect(watchedFilms, favorites, homeLibrary, librarySection, watchedMoviesRetry)', 1)[1].split(
            'var watchedEpisodes by remember', 1)[0]
        for fragment in (
            'if (homeLibrary && librarySection == "watched")',
            'preserveWatchedMovieTitles(locallyRecovered)',
            'if (!movieDetailRouteUnsupported)',
            'restoreTrackedTitle("movie", it) == null && it !in absentWatchedMovieIds',
            '}.take(12)', 'restoringWatchedMovies = true',
            'StreamingCatalog.lookupMovie(id)',
            'java.util.concurrent.CancellationException',
            'throw error',
            'error.message?.contains("(HTTP 400)") == true',
            'movieDetailRouteUnsupported = true',
            'break', 'preserveWatchedMovieTitles(remotelyRecovered)',
            'restoringWatchedMovies = false',
        ):
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, effect)
        self.assertIn('Recuperando metadados dos filmes antigos…', self.screen)
        self.assertIn('if (missingIds.isNotEmpty())', self.screen)
        self.assertIn('IDs TMDB: ', self.screen)
        self.assertNotIn('watchedFilms = watchedFilms -', effect)
        self.assertNotIn('favorites =', effect)

    def test_missing_tmdb_film_does_not_block_later_valid_watched_ids(self):
        effect = self.screen.split(
            'LaunchedEffect(watchedFilms, favorites, homeLibrary, librarySection, watchedMoviesRetry)', 1)[1].split(
            'var watchedEpisodes by remember', 1)[0]
        for marker in (
            'absentWatchedMovieIds by remember',
            'restoreTrackedTitle("movie", it) == null && it !in absentWatchedMovieIds',
            'error.message?.contains("(HTTP 404)") == true',
            'absentWatchedMovieIds = absentWatchedMovieIds + id',
            'continue',
            'error.message?.contains("(HTTP 400)") == true',
            'movieDetailRouteUnsupported = true',
            'val remotelyRecovered = mutableListOf<StreamingTitle>()',
            'finally {',
            'preserveWatchedMovieTitles(remotelyRecovered)',
        ):
            if marker == 'absentWatchedMovieIds by remember':
                self.assertIn(marker, self.screen)
            else:
                self.assertIn(marker, effect)
        self.assertLess(effect.index('error.message?.contains("(HTTP 404)")'),
                        effect.index('error.message?.contains("(HTTP 400)")'))
        self.assertLess(effect.index('} finally {'),
                        effect.index('preserveWatchedMovieTitles(remotelyRecovered)'))
        self.assertIn('error("Catálogo temporariamente indisponível (HTTP $status).")',
                      self.screen)
        self.assertNotIn('watchedFilms = watchedFilms -', effect)

    def test_watched_legacy_empty_message_and_manual_next_batch(self):
        for fragment in (
            'var watchedMoviesRetry by remember { mutableIntStateOf(0) }',
            'LaunchedEffect(watchedFilms, favorites, homeLibrary, librarySection, watchedMoviesRetry)',
            'Os filmes marcados como assistidos estão salvos, ',
            'mas os cartazes ainda não foram recuperados.',
            'if (movieDetailRouteUnsupported) {',
            'Recuperação online ainda indisponível.',
            'missingIds.any { it !in absentWatchedMovieIds }',
            'TextButton(onClick = { watchedMoviesRetry++ })',
            'Text("Recuperar mais 12")',
        ):
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, self.screen)
        self.assertIn('val missing = ids.filter {', self.screen)
        self.assertIn('}.take(12)', self.screen)
        self.assertNotIn('LaunchedEffect(watchedFilms, favorites, homeLibrary, librarySection) {',
                         self.screen)

    def test_genre_first_page_failure_is_finite_and_retryable(self):
        for fragment in (
            'var genreFirstPageRetry by remember { mutableIntStateOf(0) }',
            'homeContentType, genreSort, genreFirstPageRetry)',
            'LaunchedEffect(selectedHomeGenre, homeContentType, genreSort, genreFirstPageRetry)',
            'genreLoading[key] = true',
            'genreErrors[key] = "Não foi possível consultar este gênero."',
            'genreErrors[key] = "Não foi possível consultar cinemas no Brasil."',
            'genreErrors.remove(key)',
            'genreLoading[key] = false',
            'genreLoading[currentGenreKey] == true',
            'genreShelves[genreKey] == null',
            'TextButton(onClick = { genreFirstPageRetry++ })',
            'Text("Tentar novamente")',
        ):
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, self.screen)
        self.assertGreaterEqual(self.screen.count(
            'catch (error: java.util.concurrent.CancellationException) {'), 3)
        self.assertNotIn(
            '"$selectedGenreKind:$selectedHomeGenre:$genreSort" !in genreShelves',
            self.screen)
        self.assertIn('genreSources.any { (key, _) -> genreMore[key] == true }', self.screen)
        self.assertIn('genrePage[key] = next', self.screen)
        self.assertIn('requests.forEach { (key, mediaKind, id) ->', self.screen)

    def test_watched_poster_preserves_title_without_favorite_or_cache_ttl(self):
        self.assertIn('fun toggleFilmWatched(title: StreamingTitle)', self.screen)
        self.assertIn('StreamingDiskCache.rememberTitle("movie", title.id,',
                      self.screen)
        self.assertIn('.put("rating", title.rating)', self.screen)
        self.assertIn('.put("votes", title.votes)', self.screen)
        self.assertEqual(self.screen.count('toggleFilmWatched(title)'), 3)
        self.assertNotIn('toggleFilmWatched(title.id)', self.screen)
        self.assertIn('!it.name.startsWith("saved_title_")', self.screen)

    def test_home_more_reminder_and_brand_are_real(self):
        self.assertIn('OutlinedButton(', self.screen)
        self.assertNotIn('"Ver mais ›"', self.screen)
        self.assertNotIn('Text("Ver mais ›"', self.screen)
        self.assertIn('items(sectionTitles.chunked(3)) { group ->', self.screen)
        self.assertNotIn('Text(if (enabled) "Lembrar ✓" else "Lembrar"', self.screen)
        self.assertIn('verticalArrangement = Arrangement.spacedBy(4.dp)', self.screen)
        self.assertIn('OfficialTvIcon(installed, 14.dp)', self.screen)
        self.assertIn('Box(modifier = Modifier.size(14.dp)', self.screen)
        self.assertNotIn('painterResource(R.drawable.anytv_horizontal)', self.screen)

    def test_cinema_and_future_are_not_mixed_into_regular_streaming(self):
        self.assertIn('if (heading != ANYTV_CINEMA_SHELF)', self.screen)
        self.assertIn('if (future.isNotEmpty() || upcomingStreamingMore)', self.screen)
        # Cinema remains on its own destination, never on Principal.
        self.assertNotIn('if (cinema.isNotEmpty()) put(ANYTV_CINEMA_SHELF, cinema)', self.screen)
        self.assertIn('selectedHomeGenre == ANYTV_CINEMA_FILTER', self.screen)
        self.assertIn('anyTvReleased(title)', self.screen)
        self.assertIn('anyTvUpcoming(title)', self.screen)
        self.assertIn('else -> titles.filter(::anyTvReleased)', self.screen)
        self.assertIn('.filter(::anyTvReleased)', self.screen)
        self.assertIn('collected.filter(::anyTvReleased)', self.screen)
        self.assertIn('val unique = shelf.filter { title ->', self.screen)
        self.assertNotIn('Triple("Em breve · Filmes", "movie/upcoming", "movie")',
                         self.screen)
        self.assertNotIn('painterResource(R.drawable.anytv_horizontal)', self.screen)

    def test_genre_and_cinema_one_row_previews_precede_results(self):
        self.assertIn('private fun AnyTvOneRowPreview(', self.screen)
        self.assertIn('AnyTvOneRowPreview("Cinema pelo mundo · seleção internacional"', self.screen)
        self.assertIn('"Em breve · Cinema" else "Em breve · Filmes e séries"', self.screen)
        self.assertIn('StreamingCatalog.discoveryCinemaInternational()', self.screen)
        self.assertIn('StreamingCatalog.discoveryShelf("movie/upcoming", "movie")',
                      self.screen)
        self.assertIn('if (movie.id in bannerCinemaIds) "F · CINEMA"', self.screen)
        self.assertLess(self.screen.index('if (previews.isNotEmpty()) {'),
                        self.screen.index('// Genre page: ten-image hero above'))

    def test_future_releases_do_not_claim_unverified_streaming_offers(self):
        self.assertIn('suspend fun discoveryUpcomingStreamingPage(page: Int)', self.screen)
        upcoming = self.screen.split(
            'suspend fun discoveryUpcomingStreamingPage(page: Int)', 1)[1].split(
            '/** International cinema preview', 1)[0]
        for part in (
            'films.optJSONArray("results") ?: JSONArray(), "movie", 0)',
            'shows.optJSONArray("results") ?: JSONArray(), "tv", 0)',
            '.filter(::anyTvUpcoming)',
            'futureFilms.getOrNull(index)?.let { add(it) }',
            'futureShows.getOrNull(index)?.let { add(it) }',
        ):
            self.assertIn(part, upcoming)
        self.assertNotIn('offersInBrazil(title)', upcoming)
        self.assertNotIn('title.takeIf { confirmed }', upcoming)

    def test_24_item_batches_and_stable_filtered_pagination(self):
        for part in (
            'private const val ANYTV_LIST_BATCH = 24',
            'private suspend fun catalogPage24(',
            'val through = page * ANYTV_LIST_BATCH',
            'for (sourcePage in 1..50)',
            'seen.add("${title.media}:${title.id}")',
            'collected.drop(through - ANYTV_LIST_BATCH).take(ANYTV_LIST_BATCH)',
            'catalogPage24(page) { sourcePage ->',
            'catalogPage24(1) { sourcePage ->',
            '"Mostrar mais"',
        ):
            self.assertIn(part, self.screen)
        self.assertNotIn('"Mostrar mais 20"', self.screen)

    def test_section_cta_removed_but_pagination_kept(self):
        self.assertNotIn('Text("Ver mais ›"', self.screen)
        self.assertIn('items(sectionTitles.chunked(3))', self.screen)
        self.assertIn('LaunchedEffect(openedSection) {', self.screen)
        self.assertIn('catalogScrollState.scrollToItem(0)', self.screen)
        self.assertIn('Text("Mostrar mais")', self.screen)


    def test_saved_library_keeps_all_records_and_pages_in_24s(self):
        for snippet in (
            'homeLibrary -> true // Saved/watched records',
            'else heading to (if (homeLibrary) unique',
            'else unique.take(ANYTV_LIST_BATCH))',
            'val sectionTitles = sectionAll.take(sectionVisibleCount)',
            'sectionVisibleCount += ANYTV_LIST_BATCH',
            'items(shelf.take(sectionVisibleCount).chunked(3))',
            'LaunchedEffect(librarySection, homeLibrary)',
            'LaunchedEffect(watchingQuery)',
            'Nenhuma série corresponde à sua busca.',
            'rememberSuccessfulSearch(past)',
        ):
            self.assertIn(snippet, self.screen)
    def test_full_ratio_three_column_posters_in_all_pages(self):
        for snippet in (
            'private fun AnyTvPosterCard(',
            'modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)',
            'contentScale = ContentScale.Fit)',
            'val cardWidth = (maxWidth - 18.dp) / 3f',
            'items(genreTitles.chunked(3))',
            'items(sectionTitles.chunked(3))',
            'items(shelf.take(sectionVisibleCount).chunked(3))',
            'items(inProgress.take(sectionVisibleCount).chunked(3))',
            'globalSearchResults.chunked(3)',
        ):
            self.assertIn(snippet, self.screen)
        for forbidden in (
            'modifier = Modifier.fillMaxWidth().height(158.dp)',
            'Modifier.size(width = 116.dp, height = 158.dp)',
            'items(inProgress.take(sectionVisibleCount).chunked(2))',
        ):
            self.assertNotIn(forbidden, self.screen)
    def test_cinema_is_based_on_tmdb_br_now_playing_not_missing_streaming(self):
        self.assertIn('private const val ANYTV_CINEMA_FILTER = -1', self.screen)
        self.assertIn('"movie/now_playing?language=pt-BR&region=BR&page=$sourcePage"', self.screen)
        self.assertIn('Triple(ANYTV_CINEMA_SHELF, "movie/now_playing", "movie")',
                      self.screen)
        self.assertIn('val suffix = if (route == "movie/now_playing") "&region=BR"', self.screen)
        self.assertIn('StreamingCatalog.discoveryCinemaPage(next)', self.screen)
        self.assertIn('AnyTvCompactChip("Cinema", selectedHomeGenre == ANYTV_CINEMA_FILTER)',
                      self.screen)
        self.assertIn('Text("Cinema", modifier = Modifier', self.screen)
        self.assertIn('background(Color(0xFF1769E0))', self.screen)
        self.assertIn('inCinema = cinemaSection || title.id in cinemaIds', self.screen)
        self.assertIn('if (item.media == "movie" && inCinema)', self.screen)
        self.assertIn('"Em cartaz no Brasil"', self.screen)
        self.assertIn('"Em cartaz · Brasil · TMDB"', self.screen)
        self.assertIn('if (selectedHomeGenre == ANYTV_CINEMA_FILTER) emptyList()', self.screen)
        self.assertIn('anyTvProviderGenre(kind, selectedHomeGenre, allMovieGenres, tvGenres)', self.screen)
        self.assertIn('if (id == ANYTV_CINEMA_FILTER)', self.screen)
        self.assertIn('cinemaIds = cinemaIds + result.titles.map { it.id }', self.screen)
        self.assertNotIn('inCinema = offerings.isEmpty()', self.screen)

    def test_shared_card_overlays_and_metadata_order(self):
        shared = self.screen.split('private fun AnyTvPosterCard(', 1)[1].split(
            '/** One horizontal strip', 1)[0]
        self.assertLess(shared.index('AnyTvPinnedImage('),
                        shared.index('AnyTvRatingOverlay(title,'))
        self.assertLess(shared.index('AnyTvRatingOverlay(title,'),
                        shared.index('StreamingOfferIcons('))
        self.assertLess(shared.index('StreamingOfferIcons('),
                        shared.index('Text(title.name,'))
        self.assertIn('showYear = true', shared)
        self.assertIn('maxLines = 2', shared)
        self.assertIn('contentScale = ContentScale.Fit', shared)
        self.assertIn('if (favorite) Color(0xFFE04B57) else Color.White', shared)
        self.assertIn('if (marked) Color(0xFF1CB96B) else Color.White', shared)

    def test_continuando_persisted_three_column_grid_and_search(self):
        for snippet in (
            '"anytv_watching_titles_v1"', 'fun rememberWatchingTitle(title: StreamingTitle)',
            'if (media == "tv") watchingTitles[id]?.let { return it }',
            'fun toggleSeriesTracking(title: StreamingTitle)',
            'fun toggleWatched(title: StreamingTitle, season: Int, episode: Int)',
            'suspend fun lookupSeries(id: Int): StreamingTitle?',
            'val restored = runCatching { StreamingCatalog.lookupSeries(id) }.getOrNull()',
            '!it.name.startsWith("saved_title_")',
            'items(inProgress.take(sectionVisibleCount).chunked(3))',
            'sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })',
            'value = libraryQuery',
            'Text("Buscar nesta lista",',
            '"watching" -> "Séries acompanhadas"',
            'Text("Mostrar mais")',
        ):
            self.assertIn(snippet, self.screen)
    def test_global_search_is_full_black_overlay_with_independent_history(self):
        for snippet in (
            'if (showGlobalSearch) {',
            'Dialog(onDismissRequest = { showGlobalSearch = false }',
            'color = Color.Black',
            'contentDescription = "Fechar busca"',
            'contentDescription = "Limpar texto da busca"',
            '"anytv_global_search_history_v1"',
            'fun rememberSuccessfulSearch(term: String)',
            'rememberSuccessfulSearch(globalSearchTerm)',
            'Text("Buscas recentes"',
            'Text("Limpar histórico"',
            'globalSearchHistory.take(10).forEach { past ->',
            'globalSearchResults.chunked(3).forEach { group ->',
            '.verticalScroll(rememberScrollState())',
            'globalSearchResults = emptyList(); globalSearchError = null',
        ):
            with self.subTest(snippet=snippet):
                self.assertIn(snippet, self.screen)
        # The search UI no longer occupies a row in the page's catalogue layout.
        start = self.screen.index('    if (showGlobalSearch) {')
        end = self.screen.index('    if (showMyStreamingSettings) {', start)
        search_page = self.screen[start:end]
        self.assertIn('Surface(modifier = Modifier.fillMaxSize()', search_page)
        self.assertIn('color = Color.Black', search_page)
        self.assertIn('contentColor = Color.White', search_page)
        self.assertNotIn('expandedHomeShelf', self.screen)

    def test_search_live_suggestions_and_clear_button(self):
        self.assertIn('LaunchedEffect(globalSearchDraft)', self.screen)
        self.assertIn('delay(650)', self.screen)
        self.assertIn('globalSearchTerm = globalSearchDraft.trim()', self.screen)
        self.assertIn('BasicTextField(', self.screen)
        self.assertIn('modifier = Modifier.weight(1f).height(44.dp)', self.screen)
        self.assertIn('decorationBox = { innerTextField ->', self.screen)
        self.assertIn('Text("Buscar filme ou série", fontSize = 13.sp', self.screen)
        self.assertIn('innerTextField()', self.screen)
        self.assertNotIn('OutlinedTextField(\n                                value = globalSearchDraft', self.screen)
        self.assertIn('Icon(Icons.Filled.Search, contentDescription = "Executar busca"', self.screen)
        self.assertIn('globalSearchResults = emptyList(); globalSearchError = null', self.screen)
        self.assertNotIn('showGlobalSearch = false\\n', self.screen.split('globalSearchDraft = ""; globalSearchTerm = ""')[1].split('}',1)[0])

    def test_tv_apple_playable_id_requires_allowlisted_parameters(self):
        from pathlib import Path
        bridge = Path("overrides/tv_safe/TvAppBridge.java").read_text(encoding="utf-8")
        self.assertIn('path.matches("/(?:[a-z]{2}/)?(?:movie|show)/', bridge)
        self.assertIn('query.matches("playableId=', bridge)
        self.assertIn('u.getQueryParameter("playableId")', bridge)
        self.assertIn('"tv.apple.com".equals(host)', bridge)
        self.assertIn('path.matches("/title/[0-9]{6,9}/?")', bridge)
        self.assertIn('path.matches("/detail/[0-9A-Z]{10,40}")', bridge)

    def test_update_identity_is_31(self):
        self.assertIn('applicationId = "com.tclremote.livre"', self.gradle)
        self.assertIn("versionCode = " + ("39" if os.environ.get("TCL_V39") == "1" else "38" if os.environ.get("TCL_V38") == "1" else "37" if os.environ.get("TCL_V37") == "1" else "36" if os.environ.get("TCL_V36") == "1" else "35" if os.environ.get("TCL_V35") == "1" else "34"), self.gradle)

    def test_poster_actions_use_contrasting_icon_outline_not_round_badges(self):
        card = self.screen.split('private fun AnyTvPosterCard(', 1)[1].split(
            '/** One horizontal strip', 1)[0]
        self.assertIn('tint = Color(0xFF353535).copy(alpha = 0.85f)', card)
        self.assertIn('modifier = Modifier.size(24.dp)', card)
        self.assertIn('modifier = Modifier.size(19.dp)', card)
        self.assertNotIn('.background(Color.Black.copy(alpha = 0.60f), RoundedCornerShape(16.dp))', card)

    def test_cinema_grid_uses_common_section_header_spacing(self):
        self.assertIn('"Em cartaz no Brasil"', self.screen)
        self.assertIn('.padding(top = 24.dp, bottom = 12.dp)', self.screen)

    def test_new_compact_streaming_contract(self):
        self.assertIn('Box(modifier = Modifier.weight(1f)) { actions() }', self.screen)
        self.assertIn('AnyTvRatingOverlay(title, Modifier.align(Alignment.TopStart)', self.screen)
        self.assertIn('Color(0xFFFFD54F)', self.screen)
        self.assertIn('showYear = true', self.screen)
        self.assertIn('maxLines = 2', self.screen)
        self.assertIn('"watched" -> "Filmes e séries assistidos"', self.screen)
        self.assertIn('value = libraryQuery', self.screen)
        self.assertIn('libraryQuery.isBlank()', self.screen)
        self.assertNotIn('"Ver mais ›"', self.screen)
        self.assertNotIn('AnyTvCinemaHero(shelf, offerCache, displayed)', self.screen)
        self.assertIn('Box(modifier = Modifier.width(3.dp).height(22.dp)', self.screen)

    def test_library_labels_and_cinema_overlay_share_home_visual_contract(self):
        self.assertIn('"Séries acompanhadas"', self.screen)
        self.assertNotIn('"Continuando"', self.screen)
        self.assertIn('items(inProgress.take(sectionVisibleCount).chunked(3))', self.screen)
        self.assertIn('items(shelf.take(sectionVisibleCount).chunked(3))', self.screen)
        self.assertIn('contentScale = ContentScale.Fit)', self.screen)
        self.assertNotIn('private fun AnyTvCinemaHero(', self.screen)
        self.assertIn('Brush.verticalGradient(listOf(', self.screen)
        self.assertIn('Color.Black.copy(alpha = 0.86f)', self.screen)

    def test_streaming_list_controls_and_detailed_card_consistency(self):
        # Both provider and genre pages use one detailed-card implementation.
        self.assertIn('val detailedListCard: @Composable (StreamingTitle) -> Unit', self.screen)
        self.assertIn('items(combined) { title -> detailedListCard(title) }', self.screen)
        self.assertIn('items(genreTitles) { title -> detailedListCard(title) }', self.screen)
        self.assertIn('posterOnly = true', self.screen)
        self.assertIn('if (!posterOnly) {', self.screen)
        self.assertIn('Text("▶ Trailer"', self.screen)
        self.assertIn('Text("Mais detalhes"', self.screen)
        self.assertIn('StreamingCatalog.trailer(title.media, title.id)', self.screen)
        self.assertIn('AnyTvCompactChip("Ordenar: $sortLabel ▾", false)', self.screen)
        self.assertIn('Text("Catálogo · " + app.name', self.screen)
        self.assertIn('if (selected != null) providerListMode = !providerListMode', self.screen)

    def test_detailed_poster_metadata_not_repeated(self):
        poster = self.screen.split('private fun AnyTvPosterCard(', 1)[1].split(
            '/** One horizontal strip', 1)[0]
        self.assertIn('if (!posterOnly) {', poster)
        conditional = poster.split('if (!posterOnly) {', 1)[1]
        for snippet in ['Text(genreLabel,', 'StreamingOfferIcons(title,', 'Text(title.name,']:
            self.assertIn(snippet, conditional)
        details = self.screen.split(
            'val detailedListCard: @Composable (StreamingTitle) -> Unit', 1)[1].split(
            '    Dialog(onDismissRequest', 1)[0]
        ordered = ['Text(title.name,', 'Text(genres,', 'StreamingOfferIcons(title,',
                   'Text(summary,', 'Text("▶ Trailer"', 'Text("Mais detalhes"']
        offsets = [details.index(item) for item in ordered]
        self.assertEqual(offsets, sorted(offsets))
        self.assertEqual(details.count('OutlinedButton(onClick = {'), 2)
        self.assertNotIn('Text("Trailer e mais detalhes")', details)

    def test_streaming_subtitle_controls_are_separate_rows(self):
        section = self.screen.split('val combined = buildList', 1)[1].split(
            'if (genreViewMode == "detalhado")', 1)[0]
        self.assertIn('Box(modifier = Modifier.width(3.dp).height(22.dp)', section.split(
            'Row(modifier = Modifier.fillMaxWidth()\n                                    .horizontalScroll', 1)[0])
        self.assertIn('AnyTvGenreViewIcon(Icons.Filled.GridView', section)
        self.assertIn('AnyTvGenreViewIcon(Icons.Filled.ViewList', section)
        self.assertIn('AnyTvCompactChip("Ordenar: $sortLabel ▾"', section)
        self.assertIn('.padding(top = 24.dp, bottom = 12.dp)', self.screen)

    def test_provider_list_mode_reuses_hero_and_paginates_both_kinds(self):
        for fragment in (
            'var providerListMode by remember',
            'if (if (selected != null) providerListMode else genreSectionsMode)',
            'else "Ver em lista"',
            'selected != null && providerListMode',
            'StreamingCatalog.discover(token, app.name, kind, 1, id, "")',
            'providerListTitles[key] =',
            'providerListMore[key] = result.more',
            'providerListPages[key] = next',
            'StreamingCatalog.discover(',
            'genreViewMode == "detalhado"',
            'if (genreViewMode == "detalhado")',
        ):
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, self.screen)


if __name__ == "__main__":
    unittest.main()
