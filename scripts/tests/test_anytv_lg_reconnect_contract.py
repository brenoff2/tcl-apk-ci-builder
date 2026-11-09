#!/usr/bin/env python3
"""LG session isolation, bounded retry and paired-vs-connected regression contract."""
import unittest
from pathlib import Path

ROOT = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app")


class LGReconnectContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.vm = (ROOT / "TvRemoteViewModel.kt").read_text()
        cls.discovery = (ROOT / "DeviceDiscoveryScreen.kt").read_text()
        cls.sdk_discovery = Path("source/ConnectSDK/src/main/java/com/connectsdk/discovery/DiscoveryManager.java").read_text()
        cls.lg_connection = cls.vm.split(
            "private fun connectLgWebOs(device: ConnectableDevice) {", 1
        )[1].split("private fun handleLgWebOsDisconnected()", 1)[0]

    def test_pairing_saved_independently_of_tcl(self):
        self.assertIn('putBoolean("anytv_lg_paired_" + connectableDevice.id, true)', self.vm)
        self.assertIn('prefs.getBoolean("anytv_lg_paired_" + device.id, false)', self.vm)
        self.assertIn('device.ipAddress == pairedHost && prefs.getBoolean(Constant.PIN, false)', self.vm)
        self.assertIn("device.isPaired", self.discovery)
        self.assertIn('"Reconnect"', self.discovery)

    def test_only_active_device_is_connected(self):
        self.assertIn('val isConnected = isDeviceConnected && connectedId == device.id', self.vm)
        self.assertNotIn('(!pairedHost.isNullOrBlank() && device.ipAddress == pairedHost)', self.vm)
        self.assertIn('connectedDevice = snapshot.firstOrNull { it.isConnected }', self.vm)

    def test_lg_reconnect_never_uses_android_tls_host(self):
        self.assertIn('private fun scheduleLgReconnect(targetId: String)', self.vm)
        self.assertIn('for (attempt in 1..6)', self.vm)
        self.assertIn('connectToDeviceInternal(targetId, userInitiated = false)', self.vm)
        self.assertIn('lgReconnectTarget = connectableDevice.id', self.vm)
        self.assertIn('if (lostId != null && lgReconnectEnabled) scheduleLgReconnect(lostId)', self.vm)
        self.assertIn('val host = prefs.getString(Constant.HOST, null)', self.vm)

    def test_ssdp_never_tears_down_live_lg_socket(self):
        self.assertIn('val lgActive = Constant.connectableDevice?.takeIf', self.vm)
        self.assertIn('val activeLg = Constant.connectableDevice?.takeIf', self.vm)
        self.assertIn('if (activeService?.isConnected == true) publishDeviceSnapshot()', self.vm)
        self.assertIn('if (socket?.isConnected != true) handleLgWebOsDisconnected()', self.vm)

    def test_active_webos_socket_survives_ssdp_expiry(self):
        self.assertIn(
            'com.connectsdk.service.WebOSTVService.ID.equals(serviceDescription.getServiceID())',
            self.sdk_discovery)
        self.assertIn('if (webOsService != null && webOsService.isConnected())', self.sdk_discovery)
        self.assertIn('Keeping active webOS socket despite expired SSDP announcement', self.sdk_discovery)
        # The upstream path still removes disconnected services, including TCL.
        self.assertIn('device.removeServiceWithId(serviceDescription.getServiceID());', self.sdk_discovery)

    def test_tcl_discovery_loss_is_not_manual_disconnect(self):
        # Explicit user disconnect remains manual=true in the existing public method.
        self.assertIn("private fun disconnectCurrentDeviceInternal(manual: Boolean)", self.vm)
        self.assertRegex(
            self.vm,
            r"else if \(activeDeviceId == device\.id\) \{\s*(?://[^\n]*\n\s*)?disconnectCurrentDeviceInternal\(manual = false\)"
        )
        self.assertNotRegex(
            self.vm,
            r"else if \(activeDeviceId == device\.id\) \{\s*disconnectCurrentDevice\(\)"
        )
        self.assertIn('prefs.edit { putBoolean(Constant.PREF_MANUAL_DISCONNECT, manual) }', self.vm)

    def test_stuck_handshake_expires_before_retry(self):
        self.assertIn('armLgConnectWatchdog(device, token, 12_000L', self.lg_connection)
        self.assertIn('delay(timeoutMs)', self.vm)
        self.assertIn('pendingDeviceId == device.id', self.vm)
        self.assertIn('connectionGeneration += 1', self.vm)
        self.assertIn('scheduleLgReconnect(device.id)', self.vm)
        self.assertIn('connectionWatchdogJob?.cancel()', self.vm)

    def test_human_pairing_has_separate_bounded_timeout(self):
        self.assertRegex(
            self.lg_connection,
            r'override fun onPairingRequired\([\s\S]*?'
            r'pendingDeviceId != device\.id[\s\S]*?'
            r'armLgConnectWatchdog\(device, token, 120_000L'
        )
        self.assertIn('autorização não confirmada em 2 minutos', self.lg_connection)

    def test_lg_listener_cleanup_across_all_terminal_paths(self):
        self.assertIn('private fun clearLgListener()', self.vm)
        self.assertIn('previousDevice.removeListener(previousListener)', self.vm)
        self.assertIn('lgListenerDevice = device', self.lg_connection)
        self.assertIn('lgListener = listener', self.lg_connection)
        self.assertRegex(
            self.lg_connection,
            r'override fun onConnectionFailed\([\s\S]*?'
            r'connectionWatchdogJob\?\.cancel\(\)[\s\S]*?'
            r'clearLgListener\(\)'
        )
        self.assertIn('clearLgListener()\n                device.disconnect()', self.vm)
        self.assertRegex(
            self.vm,
            r'private fun disconnectCurrentDeviceInternal\(manual: Boolean\) '
            r'\{[\s\S]*?cancelLgReconnect\(\)\s*clearLgListener\(\)'
        )
        self.assertRegex(
            self.vm,
            r'override fun onCleared\(\) \{[\s\S]*?'
            r'cancelLgReconnect\(\)\s*clearLgListener\(\)'
        )

    def test_manual_change_cancels_lg_retry(self):
        self.assertIn('if (userInitiated) {\n            // A deliberate TV selection supersedes', self.vm)
        self.assertIn('private fun disconnectCurrentDeviceInternal(manual: Boolean) {\n        // Explicit disconnect', self.vm)
        self.assertIn('override fun onCleared() {\n        super.onCleared()\n        cancelLgReconnect()', self.vm)


if __name__ == "__main__":
    unittest.main()
