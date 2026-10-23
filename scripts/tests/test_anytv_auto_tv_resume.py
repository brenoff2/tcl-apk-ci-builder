#!/usr/bin/env python3
"""Network loss must not set manual-disconnect flag, otherwise saved TVs stop reconnecting."""
import unittest
from pathlib import Path

class SavedTvAutoReconnect(unittest.TestCase):
    def test_discovery_loss_is_involuntary(self):
        viewmodel = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TvRemoteViewModel.kt").read_text()
        start = viewmodel.index("override fun onDeviceRemoved(")
        end = viewmodel.index("override fun onDiscoveryFailed(", start)
        on_removed = viewmodel[start:end]
        self.assertIn("disconnectCurrentDeviceInternal(manual = false)", on_removed)
        self.assertNotIn("disconnectCurrentDevice()", on_removed)
        self.assertIn("scheduleAutoReconnect(delayMs = 500L)", viewmodel)
        self.assertIn("prefs.getString(Constant.HOST, null)", viewmodel)
        self.assertIn("prefs.getBoolean(Constant.PREF_MANUAL_DISCONNECT, false)", viewmodel)

    def test_user_disconnect_is_still_manual(self):
        viewmodel = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app/TvRemoteViewModel.kt").read_text()
        self.assertIn("fun disconnectCurrentDevice() {", viewmodel)
        self.assertIn("disconnectCurrentDeviceInternal(manual = true)", viewmodel)
