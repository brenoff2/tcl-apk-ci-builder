#!/usr/bin/env python3
"""Contract checks on prepared LG webOS launch points integration."""
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

    def test_live_launcher_only(self):
        self.assertIn("service.getAppList(object : Launcher.AppListListener", self.vm)
        self.assertIn("val appId = item.id?.trim().orEmpty()", self.vm)
        self.assertIn("service.launchApp(id, object : Launcher.AppLaunchListener", self.vm)
        self.assertIn("id !in _uiState.value.lgWebOsApps.map { it.id }", self.vm)
        self.assertIn("if (activeDeviceId != deviceId) return", self.vm)

    def test_ui_and_refresh(self):
        self.assertIn("onLgAppsRefresh = tvRemoteViewModel::refreshLgWebOsApps", self.nav)
        self.assertIn("onLgAppLaunch = tvRemoteViewModel::launchLgWebOsApp", self.nav)
        self.assertIn("LgWebOsAppsBar(", self.home)
        self.assertIn("apps = sessionState.lgWebOsApps", self.home)
        self.assertNotIn("apps = emptyList(), host = \"\", pairing = \"\"", self.home)
        self.assertIn("LaunchedEffect(deviceId)", self.bar)
        self.assertIn("lgWebOsAppsError", self.vm)

    def test_favorites_and_tcl_separation(self):
        self.assertIn("favorites:$deviceId", self.bar)
        self.assertIn("putStringSet(prefKey, favorites.toSet())", self.bar)
        self.assertIn("} else TclInstalledAppsBar(", self.home)
        self.assertIn("if (id !in _uiState.value.lgWebOsApps.map", self.vm)

if __name__ == "__main__":
    unittest.main()
