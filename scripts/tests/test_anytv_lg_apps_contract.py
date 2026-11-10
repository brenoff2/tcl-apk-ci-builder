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
        self.assertIn("onOpenStreaming = { lgStreaming = true }", (ROOT.parents[3] / "scripts/apply_anytv_lg_apps.py").read_text())
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

    def test_tcl_companion_remains_separate(self):
        self.assertIn("} else TclInstalledAppsBar(", self.home)
        self.assertNotIn("TclTvAppApi.apps(", self.bar)
        self.assertNotIn("TclTvAppApi.open(", self.bar)
        self.assertIn("onLaunch(app.id)", self.bar)


if __name__ == "__main__":
    unittest.main()
