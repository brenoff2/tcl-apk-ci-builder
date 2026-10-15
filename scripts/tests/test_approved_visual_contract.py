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

    def test_single_app_identity_and_version(self):
        expected = ("com.tclremote.livre" if os.environ.get("TCL_RELEASE_SINGLE_APP") == "1"
                    else "com.tclremote.livre.preview")
        self.assertIn('applicationId = "' + expected + '"', self.gradle)
        self.assertIn("versionCode = " + ("33" if os.environ.get("TCL_V33") == "1" else "32" if os.environ.get("TCL_V32") == "1" else "31" if os.environ.get("TCL_V31") == "1" else "30" if os.environ.get("TCL_V30") == "1" else "29" if os.environ.get("TCL_V29") == "1" else "28" if os.environ.get("TCL_V28") == "1" else "27" if os.environ.get("TCL_V27") == "1" else "26" if os.environ.get("TCL_V26") == "1" else "25" if os.environ.get("TCL_V25") == "1" else "24" if os.environ.get("TCL_V24") == "1" else "23" if os.environ.get("TCL_V23") == "1" else "22" if os.environ.get("TCL_V22") == "1" else "21" if os.environ.get("TCL_V21") == "1" else "20"), self.gradle)

    def test_has_all_screenshot_sections(self):
        sections = ["TclApprovedHeader(", "TclInstalledAppsBar(",
                    "PlaybackTransportBar(",
                    "TclSideRocker(", "TclQuickAction(",
                    "TclTextInputDialog("]
        sections.append("TclResponsivePad(" if os.environ.get("TCL_V21") == "1"
                        else "RemotePadStage(")
        for section in sections:
            with self.subTest(section=section):
                self.assertIn(section, self.skin)

    def test_all_actions_connected_to_real_callbacks(self):
        for callback in ("onPower()", "onOpenSettings", "onOpenDiscovery",
                         "onQuickApp = onQuickApp", "onRemoteKey(key)", "onVolumeUp()",
                         "onVolumeDown()", "onKeyboardText", "onKeyboardBackspace",
                         "onSendText", "onToggleVoice()", "onCyclePadMode()",
                         "onTabSelected(HomeTab.Cast)"):
            with self.subTest(callback=callback):
                self.assertIn(callback, self.skin)
        for enum in ("KEYCODE_HOME", "KEYCODE_BACK", "KEYCODE_CHANNEL_UP",
                     "KEYCODE_CHANNEL_DOWN", "KEYCODE_VOLUME_MUTE"):
            self.assertIn(enum, self.skin)

    def test_favorites_route_and_persistence_not_bypassed(self):
        self.assertIn("TclInstalledAppsBar(", self.skin)
        self.assertNotIn("AppShortcutStripSquare(", self.skin)
        self.assertIn("tvHost = sessionState.connectedDevice?.ipAddress", self.skin)
        self.assertIn("tvIdentity = sessionState.connectedDevice?.id", self.skin)
        catalog = (KOTLIN / "TclInstalledApps.kt").read_text()
        self.assertIn("LaunchedEffect(host, pairing, refresh)", catalog)
        self.assertIn('request(host, code, "/apps")', catalog)
        self.assertIn('"/launch"', catalog)
        self.assertIn('savedFavoriteIds(', catalog)
        self.assertIn('"favorites:$deviceKey"', catalog)
        self.assertIn('TvOfficialApp(', catalog)
        self.assertIn('OfficialTvIcon(', catalog)
        self.assertIn('Text("Ver todos  ›")', catalog)
        self.assertIn("TclApprovedHomeScreen(", self.nav)

    def test_no_fixed_bottom_bar_or_forced_dark_theme(self):
        self.assertNotIn("TclBottomTabs(", self.skin)
        self.assertNotIn("darkColorScheme(", self.skin)
        self.assertNotIn("bottomBar = {", self.skin)
        self.assertIn("MaterialTheme.colorScheme.background", self.skin)
        self.assertIn("MaterialTheme.colorScheme.primary", self.skin)
        self.assertIn("windowInsetsPadding(WindowInsets.safeDrawing)", self.skin)

    def test_keyboard_buffered_text_without_virtual_ok_click(self):
        if os.environ.get("TCL_V27") == "1":
            # Upstream IME may replace rather than append partial edits.
            self.assertIn("fun sendDraft()", self.skin)
            self.assertIn("onSendText(draft)", self.skin)
            self.assertNotIn("onAppendText(value.substring(previous.length))", self.skin)
        elif os.environ.get("TCL_V21") == "1":
            self.assertIn("onAppendText(value.substring(previous.length))", self.skin)
        else:
            self.assertIn("onSendText(draft)", self.skin)
        self.assertIn('onSendText = { text: String ->', self.nav)
        self.assertIn("tvRemoteViewModel.sendText(text, submit = false)", self.nav)
        self.assertNotIn("onEnter = onKeyboardEnter", self.skin)

    def test_pairing_survives_discovery_id_rotation_and_relaunch(self):
        catalog = (KOTLIN / "TclInstalledApps.kt").read_text()
        self.assertIn('putString("pair:$host", code)', catalog)
        self.assertIn('putString("pair:name:$nameKey", code)', catalog)
        self.assertIn("editor.commit()", catalog)
        self.assertIn("prefs.all.entries", catalog)
        self.assertIn("TclTvAppApi.apps(host, code)", catalog)
        self.assertIn('putString("favorites:$host", serialized)', catalog)

    def test_headphones_use_companion_not_remote_red_key(self):
        self.assertIn("TclTvAppApi.headphones(host, code)", self.skin)
        self.assertNotIn("KEYCODE_PROG_RED", self.skin)
        playback = (KOTLIN / "PlaybackTransportBar.kt").read_text()
        self.assertIn("onHeadphones()", playback)
        self.assertNotIn("KEYCODE_PROG_RED", playback)

    def test_no_overlap_regression_and_cast_still_available(self):
        self.assertIn("modifier = Modifier.weight(1f).fillMaxWidth()", self.skin)
        self.assertIn("coerceAtMost(stageHeight)", self.skin)
        if os.environ.get("TCL_V21") == "1":
            self.assertIn("TclResponsivePad(", self.skin)
            self.assertNotIn("178.dp", self.skin)
        else:
            self.assertIn(".coerceAtLeast(0.dp).coerceAtMost(if (compactHeight) 178.dp else 226.dp)", self.skin)
        self.assertIn("HomeScreen(", self.skin)
        self.assertIn("RemotePadMode.Touchpad -> RemotePadMode.NumberPad", self.nav)
        self.assertIn("internal fun KeyboardDialog(", self.screen)
        self.assertIn("internal fun RemotePadStage(", self.screen)


if __name__ == "__main__":
    unittest.main()
