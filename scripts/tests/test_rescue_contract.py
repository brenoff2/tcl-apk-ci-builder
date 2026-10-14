#!/usr/bin/env python3
"""Source-level regression contract for the isolated TCL rescue build.

These tests run against the prepared upstream working tree, not the override
templates. They cannot prove behavior on a real TCL television.
"""
import re
import os
import unittest
from pathlib import Path

ROOT = Path("source")
APP = ROOT / "app"
KOTLIN = APP / "src/main/java/com/hari/androidtvremote/ui/app"


class RescueContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.screen = (KOTLIN / "RemoteScreen.kt").read_text(encoding="utf-8")
        cls.playback = (KOTLIN / "PlaybackTransportBar.kt").read_text(encoding="utf-8")
        cls.shortcut = (KOTLIN / "AppShortcutTiles.kt").read_text(encoding="utf-8")
        cls.gradle = (APP / "build.gradle.kts").read_text(encoding="utf-8")

    def test_identity_and_code(self):
        if os.environ.get("TCL_APPROVED_UI") == "1":
            if os.environ.get("TCL_RELEASE_SINGLE_APP") == "1":
                self.assertIn('applicationId = "com.tclremote.livre"', self.gradle)
                self.assertNotIn('applicationId = "com.tclremote.livre.preview"', self.gradle)
                self.assertIn('versionName = "' + ("2.3-TCL" if os.environ.get("TCL_V23") == "1" else "2.2-TCL" if os.environ.get("TCL_V22") == "1" else "2.1-TCL" if os.environ.get("TCL_V21") == "1" else "2.0-TCL") + '"', self.gradle)
            else:
                self.assertIn('applicationId = "com.tclremote.livre.preview"', self.gradle)
                self.assertIn('versionName = "2.0-Preview"', self.gradle)
            self.assertIn("versionCode = " + ("23" if os.environ.get("TCL_V23") == "1" else "22" if os.environ.get("TCL_V22") == "1" else "21" if os.environ.get("TCL_V21") == "1" else "20"), self.gradle)
        else:
            self.assertIn('applicationId = "com.tclremote.livre"', self.gradle)
            self.assertIn("versionCode = 18", self.gradle)
            self.assertIn('versionName = "1.8-Resgate-Teste"', self.gradle)

    def test_no_incomplete_catalog_in_rescue(self):
        self.assertIn("AppShortcutStripSquare(", self.screen)
        self.assertNotIn("TclInstalledAppsBar(", self.screen)
        if os.environ.get("TCL_APPROVED_UI") == "1":
            self.assertTrue((KOTLIN / "TclInstalledApps.kt").exists())
            self.assertIn("usesCleartextTraffic", (APP / "src/main/AndroidManifest.xml").read_text())
        else:
            self.assertFalse((KOTLIN / "TclInstalledApps.kt").exists())
            self.assertNotIn("usesCleartextTraffic", (APP / "src/main/AndroidManifest.xml").read_text())

    def test_top_level_controls_scroll_with_finite_deck(self):
        self.assertIn("val deckHeight = maxOf(rockerHeight, gridHeight)", self.screen)
        self.assertIn(".verticalScroll(rememberScrollState())", self.screen)
        self.assertIn(
            "Box(modifier = Modifier.fillMaxWidth().height(deckHeight))",
            self.screen,
        )
        self.assertNotIn(
            "Box(modifier = Modifier.weight(1f)) {\n"
            "                            RemoteControlDeck(",
            self.screen,
        )
        self.assertIn("val gridRows = (layoutConfig.gridButtons.size + 2) / 3", self.screen)

    def test_essential_remote_controls_remain(self):
        for function in (
            "RemotePadStage(", "RemoteControlDeck(", "wrappedOnVolumeUp",
            "wrappedOnVolumeDown", "onKeyboard = ::handleKeyboardOpen",
            "onSwitchPad = wrappedOnCyclePadMode",
        ):
            with self.subTest(function=function):
                self.assertIn(function, self.screen)
        for key in (
            "KEYCODE_HOME", "KEYCODE_BACK", "KEYCODE_CHANNEL_UP",
            "KEYCODE_CHANNEL_DOWN", "KEYCODE_VOLUME_MUTE",
        ):
            with self.subTest(key=key):
                self.assertIn(key, self.screen)

    def test_media_and_headphones_wired(self):
        self.assertIn("PlaybackTransportBar(onRemoteKey = wrappedOnRemoteKey)", self.screen)
        for key in (
            "KEYCODE_MEDIA_PLAY", "KEYCODE_MEDIA_PAUSE",
            "KEYCODE_MEDIA_STOP", "KEYCODE_MEDIA_FAST_FORWARD",
            "KEYCODE_MEDIA_REWIND", "KEYCODE_MEDIA_PREVIOUS",
        ):
            with self.subTest(key=key):
                self.assertIn(key, self.playback)
        self.assertIn("onHeadphones: (() -> Unit)? = null", self.playback)
        self.assertIn("onHeadphones()", self.playback)
        self.assertNotIn("KEYCODE_PROG_RED", self.playback)
        self.assertIn("TvCompanionSetupDialog(", self.shortcut)

    def test_no_firebase_gradle_runtime(self):
        self.assertNotIn("libs.firebase", self.gradle)
        self.assertNotIn("plugins.firebase", self.gradle)


if __name__ == "__main__":
    unittest.main()
