#!/usr/bin/env python3
"""v2.1 source/protocol contract + responsive sizing tests.

Hardware behavior remains subject to a real TCL Android TV field test.
"""
import os
import unittest
from pathlib import Path

ROOT = Path("source/app")
BASE = ROOT / "src/main/java/com/hari/androidtvremote"
UI = BASE / "ui/app"
REMOTE = BASE / "androidLib/remote"


class TclV21Contract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.screen = (UI / "TclApprovedHomeScreen.kt").read_text()
        cls.pad = (UI / "TclResponsivePad.kt").read_text()
        cls.vm = (UI / "TvRemoteViewModel.kt").read_text()
        cls.session = (REMOTE / "RemoteSession.java").read_text()
        cls.manager = (REMOTE / "RemoteMessageManager.java").read_text()
        cls.nav = (BASE / "navigation/AppNavGraph.kt").read_text()
        cls.gradle = (ROOT / "build.gradle.kts").read_text()

    def test_one_real_phone_package_not_a_preview(self):
        self.assertIn('applicationId = "com.tclremote.livre"', self.gradle)
        self.assertNotIn("com.tclremote.livre.preview", self.gradle)
        self.assertIn("versionCode = " + ("35" if os.environ.get("TCL_V35") == "1" else "34" if os.environ.get("TCL_V34") == "1" else "33" if os.environ.get("TCL_V33") == "1" else "32" if os.environ.get("TCL_V32") == "1" else "31" if os.environ.get("TCL_V31") == "1" else "30" if os.environ.get("TCL_V30") == "1" else "29" if os.environ.get("TCL_V29") == "1" else "28" if os.environ.get("TCL_V28") == "1" else "27" if os.environ.get("TCL_V27") == "1" else "26" if os.environ.get("TCL_V26") == "1" else "25" if os.environ.get("TCL_V25") == "1" else "24" if os.environ.get("TCL_V24") == "1" else "23" if os.environ.get("TCL_V23") == "1" else "22" if os.environ.get("TCL_V22") == "1" else "21"), self.gradle)
        self.assertIn('versionName = "' + ("2.9.5-AnyTV" if os.environ.get("TCL_V34") == "1" else "2.9.4-AnyTV" if os.environ.get("TCL_V33") == "1" else "2.9.3-AnyTV" if os.environ.get("TCL_V32") == "1" else "2.9.2-AnyTV" if os.environ.get("TCL_V31") == "1" else "2.9.1-AnyTV" if os.environ.get("TCL_V30") == "1" else "2.9-AnyTV" if os.environ.get("TCL_V29") == "1" else "2.8-AnyTV" if os.environ.get("TCL_V28") == "1" else "2.7-AnyTV" if os.environ.get("TCL_V27") == "1" else "2.6-AnyTV" if os.environ.get("TCL_V26") == "1" else "2.5-AnyTV" if os.environ.get("TCL_V25") == "1" else "2.4-AnyTV" if os.environ.get("TCL_V24") == "1" else "2.3-TCL" if os.environ.get("TCL_V23") == "1" else "2.2-TCL" if os.environ.get("TCL_V22") == "1" else "2.1-TCL") + '"', self.gradle)

    def test_roomy_dpad_no_artificial_178dp_cap(self):
        self.assertIn("TclResponsivePad(", self.screen)
        self.assertIn("val padWidth = (maxWidth - rockerWidth * 2 - gap * 2)", self.screen)
        if os.environ.get("TCL_V22") == "1":
            self.assertIn("val padHeight = padWidth", self.screen)
            self.assertNotIn("padWidth * 1.24f", self.screen)
        else:
            self.assertIn("val padHeight = if (activePadMode == RemotePadMode.NumberPad)", self.screen)
            self.assertIn("padWidth * 1.24f", self.screen)
        self.assertNotIn("178.dp", self.screen)
        self.assertNotIn("val padSize =", self.screen)

    def test_four_number_rows_fit_on_small_screens(self):
        if os.environ.get("TCL_V22") == "1":
            settings = [
                (320, 8, 46, 5), (360, 12, 57, 8),
                (393, 12, 57, 8), (480, 12, 57, 8)
            ]
            for screen_width, padding, rocker, gap in settings:
                pad = min(screen_width - 2 * padding - 2 * rocker - 2 * gap, 280)
                height = pad
                with self.subTest(width=screen_width):
                    self.assertGreaterEqual(pad, 200)
                    self.assertEqual(pad, height)
                    self.assertGreaterEqual((height - 14 - 15) / 4, 43)
        else:
            for screen_width, padding, rocker, gap in [
                (320, 8, 41, 5), (360, 12, 45, 6),
                (393, 12, 45, 6), (480, 12, 45, 6)
            ]:
                pad = min(screen_width - 2 * padding - 2 * rocker - 2 * gap, 330)
                height = max(pad * 1.24, 252)
                with self.subTest(width=screen_width):
                    self.assertGreaterEqual(pad, 200)
                    self.assertGreaterEqual((height - 14 - 15) / 4, 55)
        for row in [
            'listOf("1", "2", "3")',
            'listOf("4", "5", "6")',
            'listOf("7", "8", "9")',
            'listOf("*", "0", "#")'
        ]:
            self.assertIn(row, self.pad)
        self.assertIn("modifier = Modifier.width(width).height(height)", self.pad)

    def test_real_tv_hold_sequence_and_cancel_release(self):
        for name in (
            "KEYCODE_DPAD_UP", "KEYCODE_DPAD_DOWN", "KEYCODE_DPAD_LEFT",
            "KEYCODE_DPAD_RIGHT", "KEYCODE_DPAD_CENTER"
        ):
            self.assertIn(name, self.pad)
        self.assertIn("withTimeoutOrNull(270L)", self.pad)
        self.assertIn("latestStart(key)", self.pad)
        self.assertIn("latestEnd(key)", self.pad)
        self.assertIn("withContext(NonCancellable)", self.pad)
        self.assertIn("if (elapsed < 430L) delay(430L - elapsed)", self.pad)
        self.assertIn("finally {", self.pad)
        self.assertIn("RemoteDirection.START_LONG", self.screen)
        self.assertIn("RemoteDirection.END_LONG", self.screen)
        self.assertIn("onKeyDirection = tvRemoteViewModel::sendHeldKey", self.nav)
        self.assertIn("Channel<() -> Unit>(Channel.UNLIMITED)", self.vm)

    def test_keyboard_live_typing_and_paste_not_clicking_selected_key(self):
        if os.environ.get("TCL_V27") == "1":
            self.assertIn("fun sendDraft()", self.screen)
            self.assertIn("onSendText(draft)", self.screen)
            self.assertNotIn("onAppendText(value.substring(previous.length))", self.screen)
        else:
            self.assertIn('Text("Digitar na TV ao vivo"', self.screen)
            self.assertIn("onAppendText(value.substring(previous.length))", self.screen)
            self.assertIn("onBackspace(previous.length - value.length)", self.screen)
        self.assertIn("onKeyboardText = tvRemoteViewModel::sendKeyboardText", self.nav)
        self.assertIn("onAppendText = onKeyboardText", self.screen)
        self.assertIn("keyAndTextWrites.trySend", self.vm)
        self.assertNotIn("text.forEach { character ->", self.vm)
        self.assertIn("sendImeTextUpdate(text)", self.vm)
        self.assertIn("val currentCounter = imeCounter", self.vm)
        self.assertNotIn("val currentCounter = ++imeCounter", self.vm)
        self.assertIn("int resolvedIme = syncedImeCounter >= 0 ? syncedImeCounter : 0;", self.session)
        self.assertIn("int resolvedField = syncedFieldCounter >= 0 ? syncedFieldCounter : 0;", self.session)
        self.assertIn("int cursor = Math.max(0, safeText.length() - 1)", self.manager)
        self.assertIn("mMessageManager.createImeBatchEditCompatV2(", self.session)
        self.assertIn("onSendText = { text: String ->", self.nav)
        self.assertNotIn("onEnter = onKeyboardEnter", self.screen)
        self.assertIn("onSendText = onSendText", self.screen)

    def test_ime_uses_one_batch_and_right_cursor_index(self):
        self.assertIn("byte[] combined = mMessageManager.createImeBatchEditCompatV2(", self.session)
        self.assertNotIn("byte[] part1 = mMessageManager.createImeText(", self.session)
        self.assertNotIn("byte[] part2 = mMessageManager.createImeBatchEditCompatV2(", self.session)
        self.assertIn("Math.max(0, safeText.length() - 1)", self.manager)
        self.assertIn("syncedFieldCounter >= 0", self.session)

    def test_all_previous_features_still_wired(self):
        self.assertIn("TclInstalledAppsBar(", self.screen)
        self.assertIn("PlaybackTransportBar(", self.screen)
        self.assertIn("TclTvAppApi.headphones(host, code)", self.screen)
        self.assertIn("onTabSelected(HomeTab.Cast)", self.screen)
        self.assertIn("MaterialTheme.colorScheme.primary", self.screen)


if __name__ == "__main__":
    unittest.main()
