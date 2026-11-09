#!/usr/bin/env python3
"""LG lifecycle regression contract against patched prepared Android sources."""
import unittest
from pathlib import Path
ROOT = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app")
class LGReconnectContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.vm = (ROOT / "TvRemoteViewModel.kt").read_text()
        cls.discovery = (ROOT / "DeviceDiscoveryScreen.kt").read_text()

    def test_pairing_saved_independently_of_tcl(self):
        self.assertIn('putBoolean("anytv_lg_paired_" + connectableDevice.id, true)', self.vm)
        self.assertIn('prefs.getBoolean("anytv_lg_paired_" + device.id, false)', self.vm)
        self.assertIn('device.ipAddress == pairedHost && prefs.getBoolean(Constant.PIN, false)', self.vm)
        self.assertIn("device.isPaired", self.discovery)
        self.assertIn('"Reconnect"', self.discovery)

    def test_lg_reconnect_not_android_tls_host(self):
        self.assertIn('private fun scheduleLgReconnect(targetId: String)', self.vm)
        self.assertIn('for (attempt in 1..6)', self.vm)
        self.assertIn('connectToDeviceInternal(targetId, userInitiated = false)', self.vm)
        self.assertIn('lgReconnectTarget = connectableDevice.id', self.vm)
        self.assertIn('if (lostId != null && lgReconnectEnabled) scheduleLgReconnect(lostId)', self.vm)
        self.assertIn('val host = prefs.getString(Constant.HOST, null)', self.vm)
        self.assertIn('cancelLgReconnect()', self.vm)

    def test_ssdp_does_not_disconnect_live_socket(self):
        self.assertIn('if ((device.getServiceByName(WebOSTVService.ID) as? WebOSTVService)?.isConnected == true)', self.vm)
        self.assertIn('else handleLgWebOsDisconnected()', self.vm)
        self.assertIn('val lgActive = Constant.connectableDevice?.takeIf', self.vm)
        self.assertIn('if (lgActive != null) return lgActive', self.vm)

    def test_manual_change_cancels_lg_retry(self):
        self.assertIn('if (userInitiated) {\n            // A deliberate TV selection supersedes', self.vm)
        self.assertIn('private fun disconnectCurrentDeviceInternal(manual: Boolean) {\n        // Explicit disconnect', self.vm)
        self.assertIn('override fun onCleared() {\n        super.onCleared()\n        cancelLgReconnect()', self.vm)

if __name__ == "__main__":
    unittest.main()
