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
        self.assertIn("OfficialTvIcon(app, 56.dp)", self.screen)
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
        self.assertIn('Text(selected?.name ?: "Início"', self.screen)
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
        self.assertIn('StreamingOfferIcons(item, offerCache, displayed,', self.screen)
        self.assertIn('StreamingOfferIcons(current, offerCache, displayed', self.screen)
        self.assertIn('StreamingCatalog.appHasOffer(app, it)', self.screen)
        self.assertIn('Nenhuma oferta de assinatura confirmada no Brasil.', self.screen)
        self.assertNotIn('displayed.filter { StreamingCatalog.streaming(it) }.forEach { app ->\n                                FilterChip(', self.screen)

    def test_streaming_brands_compact_deduplicated_and_not_addon_channels(self):
        self.assertIn('distinctBy { provider(it.name) ?: norm(it.name) }', self.screen)
        self.assertIn('normalized.contains("channel")', self.screen)
        self.assertIn('normalized.contains("canal")', self.screen)
        self.assertIn('OfficialTvIcon(installed, 20.dp)', self.screen)
        self.assertIn('fontSize = 11.sp, maxLines = 1', self.screen)
        self.assertIn('lineHeight = 14.sp', self.screen)

    def test_compact_home_search_platforms_and_customizable_genre_lists(self):
        for piece in (
            'TclHeaderAction(Icons.Filled.Home, "Início: filmes e séries"',
            'showGlobalSearch = !showGlobalSearch',
            'if (showGlobalSearch)',
            'OfficialTvIcon(app, 56.dp)',
            'suspend fun discoveryGenre(id: Int)',
            'with_genres=$id&page=$page',
            'StreamingCatalog.genres(token, "movie")',
            'selectedHomeGenre = kind.id',
            'Text("⚙ Listas", fontSize = 11.sp)',
            'putString("anytv_home_genres"',
            'choices.filter { it.id in filterGenreIds ||',
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
            'showMyStreamingSettings = true', 'Text("Filtros de descoberta")',
            'Text("Streamings"', 'Text("Gêneros"',
            'myServices = editingMyServices', 'filterGenreIds = editingFilterGenres',
            'StreamingCatalog.discover(token, name, kind',
            'activeServiceNames.forEach { name ->',
            'val filteredShelves = visibleShelves',
            'val uniqueShelves = filteredShelves.mapNotNull',
            '"Marcar todos"', '"Desmarcar todos"',
            'editingFilterGenres = if (allGenreIds.isNotEmpty()',
            'val requested = if (selectedHomeGenre == 0)',
            'choices.filter { it.id in filterGenreIds',
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
        self.assertIn('AnyTvCompactChip("Filmes", !library && contentType == "movie")', self.screen)
        self.assertIn('onHome = { selected = null; homeLibrary = false', self.screen)
        self.assertIn('onWatching = { selected = null; homeLibrary = true', self.screen)
        self.assertIn('onWatched = { selected = null; homeLibrary = true', self.screen)
        self.assertNotIn('listOf("all" to "Tudo"', self.screen)
        self.assertNotIn('label = { Text("Início", fontSize = 11.sp) }', self.screen)
        self.assertNotIn('FilterChip(selected = media == "movie" && !showSaved', self.screen)

    def test_library_global_saves_watch_progress_and_deduplicates(self):
        for fragment in (
            'packageName.ifBlank { "library" }',
            'fun toggleFilmWatched(id: Int)',
            '"anytv_watched_movies"', '"anytv_watched_episodes"',
            'fun toggleWatched(id: Int, season: Int, episode: Int)',
            '"Filmes para assistir"', '"Séries em andamento"',
            '"Filmes já assistidos"', 'val filteredShelves',
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
        self.assertIn('OfficialTvIcon(installed, 20.dp)', self.screen)
        self.assertIn('lineHeight = 14.sp', self.screen)
        self.assertIn('Spacer(Modifier.height(5.dp))', self.screen)
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
        self.assertIn('modifier = Modifier.fillMaxWidth().height(75.dp)', self.screen)
        self.assertIn('Text(movie.name, fontSize = 12.sp', self.screen)
        self.assertIn('Text("Ver ›", fontSize = 10.sp', self.screen)
        self.assertLess(
            self.apps.index('OfficialTvIcon(app = app, size = 56.dp)'),
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
        self.assertIn('OfficialTvIcon(app, 56.dp)', self.screen)
        self.assertIn('OfficialTvIcon(installed, 20.dp)', self.screen)

    def test_watched_and_tracking_history_does_not_depend_on_heart(self):
        self.assertIn('fun restoreTrackedTitle(media: String, id: Int)', self.screen)
        self.assertIn('StreamingDiskCache.previousTitle(media, id)', self.screen)
        self.assertIn('val history = watchedFilms.mapNotNull', self.screen)
        self.assertIn('val trackedIds = trackedSeries +', self.screen)
        self.assertIn('val watching = trackedIds.mapNotNull', self.screen)
        self.assertIn('"Filmes já assistidos", history', self.screen)
        self.assertIn('"Séries em andamento", watching', self.screen)

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
        self.assertIn('Text("Verificar autorização de links"', self.screen)
        self.assertIn('AnyTvPersonalResult.ActivationRequired ->', self.screen)
        self.assertIn('AnyTvPersonalResult.ResolverUnavailable ->', self.screen)
        self.assertIn('AnyTvDeviceIdentity.RESOLVER_BASE + "/v1/device/status"', resolver)
        self.assertIn("AnyTvDeviceIdentity.attachProof(context, conn, url.toURI().rawPath)", resolver)
        self.assertIn("overrides/AnyTvDeviceIdentity.kt", preparer)
        self.assertIn("StreamingCatalog.titleDeepLink(token, title, packageName)", self.screen)
        self.assertNotIn("AndroidKeyStore", resolver)
        self.assertNotIn("X-API-Key", resolver)
        self.assertNotIn("tokenDraft", self.screen)
        self.assertNotIn("personalKeyDraft", self.screen)

    def test_unprovisioned_activation_prompt_is_removed(self):
        identity = Path("overrides/AnyTvDeviceIdentity.kt").read_text(encoding="utf-8")
        for old in ("Ativar links diretos neste dispositivo",
                    "showDeviceActivation", 'Text("Código de ativação")',
                    "activationDraft", "AnyTvDeviceIdentity.activate(context, submitted)"):
            self.assertNotIn(old, self.screen)
        self.assertIn('connection.responseCode == 201', identity)
        self.assertIn('activationCode.matches(Regex(', identity)
        self.assertNotIn('putString("activation', identity)

    def test_full_screen_visual_consistency_and_empty_pages(self):
        self.assertIn('Surface(modifier = Modifier.fillMaxSize()', self.screen)
        self.assertIn('shape = RoundedCornerShape(0.dp)', self.screen)
        self.assertIn('color = MaterialTheme.colorScheme.background', self.screen)
        self.assertIn('horizontalArrangement = Arrangement.End', self.screen)
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
        self.assertIn('collected.distinctBy {', self.screen)
        self.assertIn('successfulRequests == 0', self.screen)

    def test_same_32dp_icons_and_system_bars_and_banner_ratio(self):
        home = Path("overrides/TclApprovedHomeScreen.kt").read_text(encoding="utf-8")
        self.assertIn('internal fun TclHeaderAction(', home)
        self.assertIn('modifier = Modifier.size(32.dp)', home)
        self.assertIn('TclHeaderAction(Icons.Filled.FilterList', self.screen)
        self.assertIn('val deviceBrand = remember(tvName)', home)
        self.assertIn('Text(deviceBrand', home)
        self.assertIn('windowInsetsPadding(WindowInsets.safeDrawing)', home)
        self.assertIn('window.navigationBarColor = systemBg.toArgb()', home)
        self.assertIn('window.statusBarColor = systemBg.toArgb()', home)
        self.assertIn('window.navigationBarColor = systemBg.toArgb()', home)
        self.assertIn('Modifier.fillMaxHeight().weight(0.3f)', self.screen)
        self.assertIn('Modifier.fillMaxHeight().weight(0.7f)', self.screen)
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
        self.assertIn('Text(selected?.name ?: "Início"', self.screen)
        self.assertIn('if (selected != null && !homeLibrary)', self.screen)
        self.assertIn('LaunchedEffect(selected?.packageName, homeContentType)', self.screen)
        self.assertIn('myServiceShelves[key] = it.titles', self.screen)
        self.assertIn('HorizontalPager(', self.screen)
        self.assertIn('Modifier.padding(top = 16.dp, bottom = 9.dp)', self.screen)
        self.assertNotIn('} else {\\n                    val app = selected!!', self.screen)

    def test_double_row_real_pagination_and_mixed_highlights(self):
        for snippet in (
            'suspend fun discoveryGenrePage(id: Int, media: String, page: Int,',
            'page < payload.optInt("total_pages", page)',
            'genreMore[key] = it.more',
            'items(shelf.chunked(2))',
            'StreamingCatalog.discoveryGenrePage(',
            'items(genreTitles, key =',
            'if (!homeLibrary && selected == null && selectedHomeGenre > 0)',
            '"Mostrar mais 20"',
            'genreShelves[genreKey] =',
            'val films = discoveryShelves["Em alta nesta semana"]',
            'val shows = discoveryShelves["Séries em destaque"]',
            'for (i in 0 until 10)',
            'Triple("Novidades em séries", "tv/on_the_air", "tv")',
            'Triple("Em breve · Filmes", "movie/upcoming", "movie")',
            'Text(if (item.media == "tv") "S" else "F"',
            'scheduleAnyTvPremiereReminder(context, item)',
            'class AnyTvReleaseReminderReceiver',
        ):
            with self.subTest(snippet=snippet):
                self.assertIn(snippet, self.screen)
        self.assertNotIn('items(shelf.take(12))', self.screen)
        self.assertNotIn('"Mostrar mais " + heading', self.screen)
        self.assertIn('Color(0xFFD62828) else Color(0xFF1769E0)', self.screen)
        self.assertIn('includeFontPadding = false', self.screen)
        self.assertIn('textAlign = TextAlign.Center', self.screen)
        self.assertIn('Text(item.name, maxLines = 3, lineHeight = 14.sp,', self.screen)
        self.assertIn('modifier = Modifier.fillMaxWidth().height(42.dp)', self.screen)
        self.assertIn('Reserve a category/provider row', self.screen)
        self.assertNotIn('if (item.votes > 0 || offerings.isNotEmpty())', self.screen)
        self.assertIn('modifier = Modifier.width(116.dp)', self.screen)
        self.assertNotIn('Text("★ "', self.screen)
        for snippet in (
            'genreViewMode == "list"',
            'genreViewMode == "grid"',
            'items(genreTitles.chunked(3))',
            'title.overview.take(160)',
            'Text("▶ Ver trailer")',
            'private fun AnyTvCardMetaRow(',
            'fun toggleFavorite(title: StreamingTitle, packageName: String)',
            'toggleFilmWatched(title.id)',
            'toggleSeriesTracking(title.id)',
            'val sorting = when (sort)',
            '"&sort_by=$sorting&with_genres=$id&page=$page"',
            'val genreKey = "$selectedGenreKind:$selectedHomeGenre:$genreSort"',
            'Text(if (genreLoading[genreKey] == true)',
            '}.take(10)',
            'previewProviders[movie.id]',
            'contentDescription = "Canal: " + offer.name',
            'color = Color(0xFFB7BCC5)',
            'contentAlignment = Alignment.CenterStart',
        ):
            with self.subTest(snippet=snippet):
                self.assertIn(snippet, self.screen)

    def test_three_row_cards_and_icon_only_genre_selector(self):
        self.assertIn('AnyTvGenreViewIcon(Icons.Filled.ViewList, "Lista",', self.screen)
        self.assertIn('AnyTvGenreViewIcon(Icons.Filled.GridView, "Grade de três colunas",',
                      self.screen)
        self.assertNotIn('AnyTvCompactChip("Lista",', self.screen)
        self.assertNotIn('AnyTvCompactChip("Grade 3",', self.screen)
        self.assertIn('Icon(Icons.Filled.Favorite,', self.screen)
        self.assertIn('tint = if (favorite) Color(0xFFE04B57) else neutral', self.screen)
        self.assertIn('tint = if (marked) Color(0xFF1CB96B) else neutral', self.screen)
        self.assertNotIn('AnyTvGenreTitleActions(', self.screen)
        self.assertNotIn('onTrailer =', self.screen)
        self.assertIn('Text("▶ Ver trailer")', self.screen)
        self.assertEqual(self.screen.count('showRating = false'), 3)
        self.assertIn('val unique = shelf.distinctBy', self.screen)
        self.assertIn('Keep cards visible when marked', self.screen)
        for marker, until, metadata, offers, title in (
            ('items(genreTitles.chunked(3))',
             'repeat(3 - group.size)', 'AnyTvCardMetaRow(title,',
             'StreamingOfferIcons(title, offerCache, displayed,',
             'Text(title.name, maxLines = 3,'),
            ('items(genreTitles, key =',
             'HorizontalDivider(color =', 'AnyTvCardMetaRow(title,',
             'StreamingOfferIcons(title, offerCache, displayed,',
             'Text(title.name, maxLines = 3,'),
            ('items(shelf.chunked(2))',
             'if (heading.startsWith("Em breve"))',
             'AnyTvCardMetaRow(item,',
             'StreamingOfferIcons(item, offerCache, displayed,',
             'Text(item.name, maxLines = 3,'),
        ):
            portion = self.screen.split(marker, 1)[1].split(until, 1)[0]
            self.assertTrue(0 <= portion.find(metadata) < portion.find(offers) <
                            portion.find(title), marker)
        self.assertIn('modifier = Modifier.fillMaxWidth().height(42.dp)', self.screen)

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

if __name__ == "__main__":
    unittest.main()
