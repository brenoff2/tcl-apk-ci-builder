"""Regression contract: the remote must open the catalogue's ONLY real menu."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
REMOTE = ROOT / "overrides/TclApprovedHomeScreen.kt"
CATALOG = ROOT / "overrides/TclStreamingScreens.kt"
BAR = ROOT / "overrides/TclInstalledApps.kt"

class RemoteMenuRouteTests(unittest.TestCase):
    def test_remote_header_routes_to_shared_catalogue_menu(self):
        remote = REMOTE.read_text(encoding="utf-8")
        self.assertIn('onMenu = {\n                            globalNavRoute = "menu"\n                            globalNavRequest += 1\n                        }', remote)
        self.assertNotIn('remoteMenuOpen', remote)
        self.assertNotIn('Dialog(onDismissRequest = { remoteMenuOpen', remote)

    def test_catalogue_opens_same_drawer_on_menu_entry(self):
        catalog = CATALOG.read_text(encoding="utf-8")
        self.assertIn('var menuOpen by remember(initialRoute) {\n        mutableStateOf(initialRoute == "menu")\n    }', catalog)
        self.assertIn('AnyTvUnifiedDrawer(onClose = { menuOpen = false }', catalog)
        self.assertIn('"account", "help" -> accountArea = route', catalog)
        self.assertIn('"settings" -> {', catalog)
        self.assertIn('onOpenAppSettings()', catalog)

    def test_shared_settings_routes_to_existing_native_settings_screen(self):
        remote = REMOTE.read_text(encoding="utf-8")
        catalog = CATALOG.read_text(encoding="utf-8")
        bar = BAR.read_text(encoding="utf-8")
        self.assertIn('onOpenAppSettings = onOpenSettings', remote)
        self.assertIn('onOpenAppSettings: () -> Unit = {}', bar)
        self.assertIn('onOpenAppSettings = onOpenAppSettings', bar)
        self.assertIn('onOpenAppSettings: () -> Unit = {}', catalog)
        self.assertIn('"settings" -> {', catalog)
        self.assertIn('onDismiss()', catalog)
        self.assertIn('onOpenAppSettings()', catalog)
        self.assertNotIn('"account", "help", "settings" -> accountArea = route', catalog)

    def test_remote_menu_route_reaches_real_streaming_surface(self):
        bar = BAR.read_text(encoding="utf-8")
        self.assertIn('LaunchedEffect(globalNavRequest) {', bar)
        self.assertIn('if (globalNavRequest > 0) {', bar)
        self.assertIn('streamingEntryRoute = globalNavRoute', bar)
        self.assertIn('showStreaming = true', bar)
        self.assertIn('initialRoute = streamingEntryRoute,', bar)
        self.assertIn('streamingEntryRoute = globalNavRoute', bar)
        self.assertIn('streamingEntryRoute = "catalogue"', bar)
        self.assertIn('AnyTvHomeFilmBanner(onOpenStreaming = {', bar)

if __name__ == "__main__":
    unittest.main()
