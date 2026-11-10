#!/usr/bin/env python3
"""LG webOS launch-point cache and TCL-parity presentation contract."""
import unittest
from pathlib import Path

ROOT = Path("source/app/src/main/java/com/hari/androidtvremote")


class LgAppsContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.vm = (ROOT / "ui/app/TvRemoteViewModel.kt").read_text()
        cls.nav = (ROOT / "navigation/AppNavGraph.kt").read_text()
        cls.home = (ROOT / "ui/app/TclApprovedHomeScreen.kt").read_text()
        cls.bar = (ROOT / "ui/app/LgWebOsAppsBar.kt").read_text()

    def test_launcher_ids_only(self):
        self.assertIn("service.getAppList(object : Launcher.AppListListener", self.vm)
        self.assertIn("val appId = item.id?.trim().orEmpty()", self.vm)
        self.assertIn("service.launchApp(id, object : Launcher.AppLaunchListener", self.vm)
        self.assertIn("id !in _uiState.value.lgWebOsApps.map { it.id }", self.vm)
        self.assertIn("activeDeviceId != deviceId", self.vm)

    def test_cache_restored_only_for_connected_lg(self):
        self.assertIn("private fun restoreLgWebOsApps(deviceId: String)", self.vm)
        self.assertIn("restoreLgWebOsApps(connectableDevice.id)", self.vm)
        self.assertIn('prefs.getString("anytv_lg_apps_" + deviceId, null)', self.vm)
        self.assertIn("JSONArray(saved)", self.vm)
        self.assertIn('putString("anytv_lg_apps_" + deviceId, saved.toString())', self.vm)
        self.assertIn("lgWebOsAppsDeviceId = deviceId", self.vm)
        self.assertIn("if (cached == null) refreshLgWebOsApps()", self.vm)
        self.assertIn("if (state.lgWebOsAppsDeviceId == deviceId)", self.vm)
        self.assertNotIn("lgWebOsAppsError = null, lgWebOsApps = emptyList()", self.vm)

    def test_timeout_stale_callback_and_explicit_refresh(self):
        self.assertIn("val requestId = ++lgAppsRequestSeq", self.vm)
        self.assertIn("requestId != lgAppsRequestSeq", self.vm)
        self.assertIn("connectionToken != connectionGeneration", self.vm)
        self.assertIn("delay(10_000L)", self.vm)
        self.assertIn("lgWebOsAppsBusy = false", self.vm)
        self.assertIn("onLgAppsRefresh = tvRemoteViewModel::refreshLgWebOsApps", self.nav)

    def test_same_home_layout_and_picker_as_tcl(self):
        self.assertIn("onLgAppLaunch = tvRemoteViewModel::launchLgWebOsApp", self.nav)
        self.assertIn("LgWebOsAppsBar(", self.home)
        self.assertIn("apps = sessionState.lgWebOsApps", self.home)
        self.assertIn("AnyTvHomeFilmBanner(onOpenStreaming = onOpenStreaming)", self.bar)
        self.assertIn("onOpenStreaming = { lgStreaming = true }", Path("scripts/apply_anytv_lg_apps.py").read_text())
        self.assertIn('Text("Editar"', self.bar)
        self.assertIn("Checkbox(checked = app.id in draft", self.bar)
        self.assertIn('Text("Salvar")', self.bar)
        self.assertIn("getStringSet(prefKey, null)", self.bar)
        self.assertIn("putStringSet(prefKey, favorites.toSet())", self.bar)
        self.assertNotIn("LaunchedEffect(deviceId)", self.bar)

    def test_banner_is_navigation_only_not_title_launch(self):
        catalog = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclStreamingScreens.kt").read_text()
        self.assertIn("Box(modifier = Modifier.fillMaxSize().clickable(onClick = onOpenStreaming)", catalog)
        self.assertNotIn("onOpenTitle(movie)", catalog)
        self.assertNotIn("onOpenTitle = { title -> selectedBannerTitle = title }", self.bar)
        self.assertIn("AnyTvHomeFilmBanner(onOpenStreaming = onOpenStreaming)", self.bar)
        self.assertIn("onOpenStreaming = { lgStreaming = true }", self.home)
        self.assertIn("if (lgStreaming && sessionState.connectedDevice?.type == \"LG webOS\")", self.home)
        self.assertIn("TclStreamingScreens(", self.home)
        self.assertNotIn("service.launchNetflix(", self.bar)
        self.assertNotIn("launchAppWithInfo(", self.bar)

    def test_webos_inventory_reaches_streaming_and_youtube_is_not_hidden(self):
        script = Path("scripts/apply_anytv_lg_apps.py").read_text()
        catalog = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclStreamingScreens.kt").read_text()
        self.assertIn('TvOfficialApp("lg:" + app.id, app.name, app.icon)', script)
        self.assertIn('apps = sessionState.lgWebOsApps.map { app ->', script)
        self.assertNotIn('apps = emptyList(), host = "", pairing = ""', self.home)
        self.assertIn('val selectedApps = if (!hasUserFavorites) allApps', self.bar)
        self.assertNotIn('allApps.take(6)', self.bar)
        self.assertIn('www.gstatic.com/youtube/img/branding/favicon/favicon_144x144.png', self.bar)
        self.assertIn('LgAppIcon(LgWebOsApp(', catalog)
        self.assertIn('Nenhum aplicativo recebido da TV', catalog)

    def test_lg_title_action_uses_webos_launcher_not_tcl_pairing(self):
        script = Path("scripts/apply_anytv_lg_apps.py").read_text()
        catalog = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclStreamingScreens.kt").read_text()
        self.assertIn("isLgWebOs = true,", script)
        self.assertIn("onLaunchLgApp = onLgAppLaunch,", script)
        self.assertIn('showStatus("Conecte a LG para abrir o aplicativo."', script)
        self.assertIn('showStatus("A sessão webOS está desconectada.', script)
        self.assertIn('showStatus("Aplicativo não identificado nesta LG.', script)
        self.assertIn("onLaunchLgApp: (String) -> Unit = {}", catalog)
        title_handler = catalog.split("fun searchOnTv(title: StreamingTitle, packageName: String)", 1)[1]
        lg_handler, tcl_handler = title_handler.split("if (host.isBlank() || pairing.isBlank())", 1)
        self.assertIn("if (isLgWebOs)", lg_handler)
        self.assertIn('val lgId = app.packageName.removePrefix("lg:")', lg_handler)
        self.assertIn('onLaunchLgApp(lgId)', lg_handler)
        self.assertIn("Ainda não há deep link validado", lg_handler)
        self.assertIn("Não foi confirmado um ID Netflix", lg_handler)
        self.assertIn("TclTvAppApi.openTitle(", tcl_handler)
        self.assertIn('} else if (!isLgWebOs &&', catalog)
        self.assertIn("Aplicativos da LG (disponibilidade do título não confirmada)", catalog)

    def test_same_playback_remote_and_offer_logos_across_lg_tcl(self):
        catalog = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclStreamingScreens.kt").read_text()
        lg = catalog.split("fun searchOnTv(title: StreamingTitle, packageName: String)", 1)[1].split("if (host.isBlank() || pairing.isBlank())", 1)[0]
        self.assertIn("showPlaybackRemote = true", lg)
        self.assertIn("remoteMinimized = false", lg)
        self.assertIn('onLaunchLgApp(lgId)', lg)
        self.assertIn('if (offer.logo.startsWith("/"))', catalog)
        self.assertIn('model = "https://image.tmdb.org/t/p/w92" + offer.logo', catalog)
        self.assertIn('installed?.packageName?.startsWith("lg:") == true', catalog)
        self.assertIn("LgAppIcon(LgWebOsApp(", catalog)
        self.assertIn("if (showPlaybackRemote)", catalog)
        self.assertIn("TclResponsivePad(", catalog)
        self.assertIn("PlaybackTransportBar(onRemoteKey = send)", catalog)

    def test_netflix_content_id_is_verified_and_uses_existing_webos_launcher(self):
        script = Path("scripts/apply_anytv_lg_apps.py").read_text()
        catalog = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclStreamingScreens.kt").read_text()
        self.assertIn('onLgNetflixTitle = tvRemoteViewModel::launchLgNetflixTitle', script)
        self.assertIn('onLaunchLgNetflixTitle = onLgNetflixTitle', script)
        self.assertIn('service.launchNetflix(contentId, object : Launcher.AppLaunchListener', script)
        self.assertIn('contentId.matches(Regex("[0-9]{6,8}"))', script)
        self.assertIn('?.takeIf { it.matches(Regex("[0-9]{6,8}")) }', catalog)
        self.assertIn('([0-9]{6,8})', catalog)
        self.assertNotIn('[0-9]{6,9}', catalog)
        self.assertIn('onLaunchLgNetflixTitle: (String) -> Unit = {}', catalog)
        self.assertIn('StreamingCatalog.titleDeepLink(token, title, "com.netflix.ninja")', catalog)
        self.assertIn('StreamingCatalog.titleAvailableInBrazil(', catalog)
        self.assertIn('onLaunchLgNetflixTitle(netflixId)', catalog)
        # Upstream ConnectSDK's Netflix deep-link builder is a legacy /movies/ route.
        # Never send a TMDB TV/series identifier to that movie-specific launcher.
        self.assertIn('if (title.media == "movie" && anyTvServiceKey(app.name) == "netflix")', catalog)
        self.assertIn('if (netflixId != null)', catalog)
        self.assertIn('onLaunchLgApp(lgId)', catalog)

    def test_lg_reconnect_invalidates_previous_launch_status_and_callbacks(self):
        script = Path("scripts/apply_anytv_lg_apps.py").read_text()
        restore = script.split("private fun restoreLgWebOsApps(deviceId: String)", 1)[1]
        restore = restore.split("fun refreshLgWebOsApps()", 1)[0]
        self.assertIn("lgLaunchRequestSeq += 1", restore)
        self.assertIn("setLgLaunchStatus(null)", restore)
        self.assertIn("restoreLgWebOsApps(connectableDevice.id)", script)
        self.assertIn("private fun setLgLaunchStatus(status: String?)", script)
        self.assertIn("requestId == lgLaunchRequestSeq", script)

    def test_lg_launcher_outcomes_are_visible_and_do_not_claim_playback(self):
        script = Path("scripts/apply_anytv_lg_apps.py").read_text()
        catalog = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclStreamingScreens.kt").read_text()
        self.assertIn('lgLaunchStatus: String? = null', script)
        self.assertIn('lgLaunchStatus = sessionState.lgLaunchStatus,', script)
        self.assertIn('lgLaunchStatus: String? = null', catalog)
        self.assertIn('if (isLgWebOs && lgLaunchStatus != null)', catalog)
        self.assertIn('Text(lgLaunchStatus, fontSize = 12.sp', catalog)
        self.assertIn('setLgLaunchStatus("LG: comando enviado; aguardando resposta.")', script)
        self.assertIn('setLgLaunchStatus("LG: comando aceito; aplicativo não confirmado visualmente.")', script)
        self.assertIn('setLgLaunchStatus("LG: comando Netflix aceito; título não confirmado.")', script)
        self.assertIn('setLgLaunchStatus("LG: comando rejeitado pela TV.")', script)
        self.assertIn('setLgLaunchStatus("LG: comando Netflix rejeitado.")', script)
        self.assertIn('setLgLaunchStatus("LG: sem resposta em 10 segundos; confira a conexão webOS.")', script)
        self.assertIn('requestId == lgLaunchRequestSeq', script)
        self.assertIn('activeDeviceId == deviceId', script)
        self.assertNotIn('showStatus("Aplicativo aberto na LG."', script)
        self.assertIn('A ficha e a reprodução ainda não foram confirmadas.', catalog)
        self.assertNotIn('Ficha Netflix solicitada na LG. Confirme na TV.', script)

    def test_lg01_lg10_qa_cases_have_code_regression_coverage(self):
        """The plan stays pending; this test only checks static implementation guards."""
        qa = Path("docs/qa/ANYTV_V2_2.9.25_DEVICE_HOMOLOGATION_PLAN_20261025.md").read_text()
        script = Path("scripts/apply_anytv_lg_apps.py").read_text()
        catalog = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclStreamingScreens.kt").read_text()
        source = script + "\n" + catalog + "\n" + self.vm + "\n" + self.bar + "\n" + self.home
        guards = {
            "LG01": ('TvOfficialApp("lg:" + app.id, app.name, app.icon)', 'LgAppIcon(LgWebOsApp(', 'offer.logo.startsWith("/")'),
            "LG02": ('if (title.media == "movie" && anyTvServiceKey(app.name) == "netflix")',
                     'titleAvailableInBrazil(', 'onLaunchLgNetflixTitle(netflixId)',
                     'showPlaybackRemote = true'),
            "LG03": ('if (netflixId != null)', 'onLaunchLgApp(lgId)',
                     'Não foi confirmado um ID Netflix'),
            "LG04": ('title.media == "movie"', 'service.launchNetflix(contentId'),
            "LG05": ('onLaunchLgApp(lgId)', 'Ainda não há deep link validado'),
            "LG06": ('onLaunchLgApp(lgId)', 'it.packageName.startsWith("lg:")'),
            "LG07": ('LG: sem TV conectada; não enviado.',
                     'LG: aplicativo ausente do inventário; não enviado.'),
            "LG08": ('LG: comando rejeitado pela TV.',
                     'LG: sem resposta em 10 segundos; confira a conexão webOS.',
                     'requestId == lgLaunchRequestSeq'),
            "LG09": ('LG: comando aceito; aplicativo não confirmado visualmente.',
                     'lgLaunchStatus = sessionState.lgLaunchStatus,'),
            "LG10": ('TclTvAppApi.openTitle(', '} else TclInstalledAppsBar(',
                     'PlaybackTransportBar(onRemoteKey = send)'),
        }
        for case_id, fragments in guards.items():
            with self.subTest(case=case_id):
                self.assertIn("| " + case_id + " |", qa)
                self.assertIn("| PENDENTE |", qa.split("| " + case_id + " |", 1)[1].split("\n", 1)[0])
                for fragment in fragments:
                    self.assertIn(fragment, source)
        self.assertEqual(len(guards), 10)

    def test_lg_launch_status_is_visible_in_foreground_remote_dialog(self):
        catalog = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclStreamingScreens.kt").read_text()
        remote = catalog.split("    if (showPlaybackRemote) {", 1)[1]
        self.assertIn("Dialog(onDismissRequest = { remoteMinimized = true }", remote)
        self.assertIn('if (isLgWebOs && lgLaunchStatus != null)', remote)
        self.assertIn('Text(lgLaunchStatus, fontSize = 12.sp', remote)
        self.assertIn('TclResponsivePad(', remote)
        self.assertIn('PlaybackTransportBar(onRemoteKey = send)', remote)

    def test_lg05_lg06_do_not_invent_youtube_or_provider_deep_links(self):
        catalog = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TclStreamingScreens.kt").read_text()
        handler = catalog.split("fun searchOnTv(title: StreamingTitle, packageName: String)", 1)[1]
        lg = handler.split("if (host.isBlank() || pairing.isBlank())", 1)[0]
        self.assertIn('if (title.media == "movie" && anyTvServiceKey(app.name) == "netflix")', lg)
        fallback = lg.split('} else {\n                    onLaunchLgApp(lgId)', 1)[1]
        self.assertIn("Ainda não há deep link validado", fallback)
        self.assertNotIn("launchYouTube(", lg)
        self.assertNotIn("TclTvAppApi.openTitle(", lg)
        self.assertNotIn("StreamingCatalog.titleDeepLink(", fallback)
        self.assertNotIn("onLaunchLgNetflixTitle(", fallback)

    def test_lg07_lg09_callback_result_is_not_playback_or_raw_sdk_error(self):
        script = Path("scripts/apply_anytv_lg_apps.py").read_text()
        methods = script.split("    fun launchLgNetflixTitle(contentId: String) {", 1)[1]
        methods = methods.split("'''\npatch(vm,", 1)[0]
        self.assertIn('setLgLaunchStatus("LG: comando enviado; aguardando resposta.")', methods)
        self.assertIn("requestId == lgLaunchRequestSeq", methods)
        self.assertIn('LG: comando Netflix aceito; título não confirmado.', methods)
        self.assertIn('LG: comando aceito; aplicativo não confirmado visualmente.', methods)
        self.assertIn('LG: comando rejeitado pela TV.', methods)
        self.assertIn('LG: sem resposta em 10 segundos; confira a conexão webOS.', script)
        self.assertNotIn("error?.message", methods)
        self.assertNotIn("Aplicativo aberto na LG.", methods)
        self.assertNotIn("reprodução iniciada", methods)

    def test_tcl_companion_remains_separate(self):
        self.assertIn("} else TclInstalledAppsBar(", self.home)
        self.assertNotIn("TclTvAppApi.apps(", self.bar)
        self.assertNotIn("TclTvAppApi.open(", self.bar)
        self.assertIn("onLaunch(app.id)", self.bar)


if __name__ == "__main__":
    unittest.main()
