#!/usr/bin/env python3
"""Contract tests for the screenshot-approved remote preview.

Source-level coverage only; device hardware and rendering still require QA.
"""
import os
import unittest
from pathlib import Path

APP = Path("source/app")
KOTLIN = APP / "src/main/java/com/hari/androidtvremote/ui/app"
NAV = APP / "src/main/java/com/hari/androidtvremote/navigation/AppNavGraph.kt"


@unittest.skipUnless(os.environ.get("TCL_APPROVED_UI") == "1", "approved UI mode only")
class ApprovedUiContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.skin = (KOTLIN / "TclApprovedHomeScreen.kt").read_text()
        cls.nav = NAV.read_text()
        cls.custom = (KOTLIN / "RemoteCustomization.kt").read_text()
        cls.screen = (KOTLIN / "RemoteScreen.kt").read_text()
        cls.gradle = (APP / "build.gradle.kts").read_text()

    def test_side_by_side_install_does_not_replace_17(self):
        self.assertIn('applicationId = "com.tclremote.livre.preview"', self.gradle)
        self.assertNotIn('applicationId = "com.tclremote.livre"', self.gradle)
        self.assertIn("versionCode = 19", self.gradle)

    def test_has_all_screenshot_sections(self):
        for section in ("TclApprovedHeader(", '"Aplicativos"', '"Ver todos  ›"',
                        '"Reprodução"', "PlaybackTransportBar(",
                        "TclSideRocker(", "RemotePadStage(", "TclQuickAction(",
                        "TclBottomTabs("):
            with self.subTest(section=section):
                self.assertIn(section, self.skin)

    def test_all_actions_connected_to_real_callbacks(self):
        for callback in ("onPower()", "onOpenSettings", "onOpenDiscovery",
                         "onQuickApp(id)", "onRemoteKey(key)", "onVolumeUp()",
                         "onVolumeDown()", "onKeyboardText", "onKeyboardBackspace",
                         "onKeyboardEnter", "onToggleVoice()", "onCyclePadMode()",
                         "onTabSelected(HomeTab.Cast)"):
            with self.subTest(callback=callback):
                self.assertIn(callback, self.skin)
        for enum in ("KEYCODE_HOME", "KEYCODE_BACK", "KEYCODE_CHANNEL_UP",
                     "KEYCODE_CHANNEL_DOWN", "KEYCODE_VOLUME_MUTE"):
            self.assertIn(enum, self.skin)

    def test_favorites_route_and_persistence_not_bypassed(self):
        self.assertIn("AppShortcutStripSquare(", self.skin)
        self.assertIn("Constant.PREF_REMOTE_APP_ORDER", self.skin)
        self.assertIn("Screen.QuickLaunchOrder.route", self.nav)
        self.assertIn("TclApprovedHomeScreen(", self.nav)
        self.assertIn('RemoteShortcutApp("disney_plus"', self.custom)
        self.assertIn('RemoteShortcutApp("iptv"', self.custom)
        self.assertNotIn("TclInstalledAppsBar(", self.skin)

    def test_no_overlap_regression_and_cast_still_available(self):
        self.assertIn(".verticalScroll(rememberScrollState())", self.skin)
        self.assertIn(".coerceAtLeast(0.dp).coerceAtMost(215.dp)", self.skin)
        self.assertIn("HomeScreen(", self.skin)
        self.assertIn("RemotePadMode.Touchpad -> RemotePadMode.NumberPad", self.nav)
        self.assertIn("internal fun KeyboardDialog(", self.screen)
        self.assertIn("internal fun RemotePadStage(", self.screen)


if __name__ == "__main__":
    unittest.main()
