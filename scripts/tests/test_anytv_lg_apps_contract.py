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
        self.assertIn('contentId.matches(Regex("[0-9]{6,9}"))', script)
        self.assertIn('onLaunchLgNetflixTitle: (String) -> Unit = {}', catalog)
        self.assertIn('StreamingCatalog.titleDeepLink(token, title, "com.netflix.ninja")', catalog)
        self.assertIn('StreamingCatalog.titleAvailableInBrazil(', catalog)
        self.assertIn('onLaunchLgNetflixTitle(netflixId)', catalog)
        # Upstream ConnectSDK's Netflix deep-link builder is a legacy /movies/ route.
        # Never send a TMDB TV/series identifier to that movie-specific launcher.
        self.assertIn('if (title.media == "movie" && anyTvServiceKey(app.name) == "netflix")', catalog)
        self.assertIn('if (netflixId != null)', catalog)
        self.assertIn('onLaunchLgApp(lgId)', catalog)

    def test_tcl_companion_remains_separate(self):
        self.assertIn("} else TclInstalledAppsBar(", self.home)
        self.assertNotIn("TclTvAppApi.apps(", self.bar)
        self.assertNotIn("TclTvAppApi.open(", self.bar)
        self.assertIn("onLaunch(app.id)", self.bar)


if __name__ == "__main__":
    unittest.main()
