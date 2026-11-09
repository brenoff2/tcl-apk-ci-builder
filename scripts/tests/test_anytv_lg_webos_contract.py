#!/usr/bin/env python3
"""Static assertions on the prepared LG proof; no physical-TV claim."""
import unittest
from pathlib import Path

ROOT = Path("source/app/src/main/java/com/hari/androidtvremote/ui/app")
class LgWebOsContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.vm = (ROOT / "TvRemoteViewModel.kt").read_text()
        cls.app = Path("source/app/src/main/java/com/hari/androidtvremote/App.kt").read_text()
        cls.home = (ROOT / "TclApprovedHomeScreen.kt").read_text()
        cls.workflow = Path(".github/workflows/anytv-lg-webos-unsigned-ci.yml").read_text()

    def test_discovery_and_selection(self):
        self.assertIn("device.isLgWebOsDevice()", self.vm)
        self.assertIn("getServiceByName(WebOSTVService.ID)", self.vm)
        self.assertIn('isLgWebOsDevice() -> "LG webOS"', self.vm)
        self.assertIn("connectLgWebOs(device)", self.vm)
        self.assertIn("WebOSTVService::class.java", self.app)
        self.assertIn("SSDPDiscoveryProvider::class.java", self.app)

    def test_tv_prompt_not_android_pairing_pin(self):
        self.assertIn("PairingType.FIRST_SCREEN", self.vm)
        self.assertIn("DiscoveryManager.PairingLevel.ON", self.vm)
        self.assertIn("Autorize a conexão do AnyTV na tela da LG.", self.vm)
        self.assertIn("pairingRequired = false", self.vm)
        self.assertIn("if (device.isLgWebOsDevice())", self.vm)

    def test_genuine_webos_commands(self):
        for key in ("KEYCODE_DPAD_UP", "KEYCODE_DPAD_DOWN",
                    "KEYCODE_DPAD_LEFT", "KEYCODE_DPAD_RIGHT",
                    "KEYCODE_DPAD_CENTER", "KEYCODE_BACK", "KEYCODE_HOME",
                    "KEYCODE_VOLUME_UP", "KEYCODE_VOLUME_DOWN",
                    "KEYCODE_CHANNEL_UP", "KEYCODE_CHANNEL_DOWN"):
            self.assertIn(key, self.vm)
        for call in ("keys?.up(listener)", "keys?.ok(listener)",
                     "keys?.home(listener)", "volume?.volumeUp(listener)",
                     "channels?.channelUp(listener)"):
            self.assertIn(call, self.vm)

    def test_no_tcl_companion_calls_for_lg(self):
        self.assertIn('sessionState.connectedDevice?.type == "LG webOS"', self.home)
        self.assertIn("} else TclInstalledAppsBar(", self.home)
        self.assertIn("TclStreamingScreens(", self.home)
        self.assertIn('if (sessionState.connectedDevice?.type == "LG webOS") remoteKey(it)', self.home)
        self.assertIn('apps = emptyList(), host = "", pairing = ""', self.home)
        self.assertIn("if (currentConnectableDevice()?.isLgWebOsDevice() == true)", self.vm)

    def test_lg_does_not_use_android_hold_or_tcl_auto_reconnect(self):
        self.assertIn("if (currentConnectableDevice()?.isLgWebOsDevice() == true)", self.vm)
        self.assertIn("if (direction == Remotemessage.RemoteDirection.START_LONG) sendLgWebOsKey(keyCode)", self.vm)
        self.assertIn("handleLgWebOsDisconnected()", self.vm)
        self.assertIn("if (device.isLgWebOsDevice()) handleLgWebOsDisconnected()", self.vm)
        self.assertIn("putBoolean(Constant.PREF_MANUAL_DISCONNECT, true)", self.vm)
        self.assertIn("if (!isActiveConnection(token) || !service.isConnected) return", self.vm)
        self.assertIn("(it.getServiceByName(WebOSTVService.ID) as? WebOSTVService)?.isConnected == true", self.vm)
        self.assertNotIn("it.id == activeDeviceId && it.isLgWebOsDevice() && it.isConnected", self.vm)
        self.assertIn("volume?.setMute(mute, listener)", self.vm)
        self.assertIn('if (isLg) null else tv?.ipAddress', self.home)
        self.assertIn("if (host.isNullOrBlank() || tv == null)", self.home)
        self.assertIn('Fones via companion disponíveis somente na TCL.', self.home)

    def test_lg_status_and_ui_have_required_import(self):
        self.assertIn("import androidx.compose.material3.Button", self.home)
        self.assertIn("Teclado LG requer sessão de entrada webOS; use o teclado da TV por enquanto.", self.vm)
        self.assertIn('showStatus("Comando não disponível na LG webOS.", isError = false)', self.vm)

    def test_independent_unsigned_ci(self):
        self.assertIn("scripts/apply_anytv_lg_webos.py", self.workflow)
        self.assertIn("versionCode = 95", self.workflow)
        self.assertIn("2.9.67-AnyTV", self.workflow)
        self.assertIn("assembleRelease", self.workflow)
        self.assertIn("candidate-NOT-INSTALL.txt", self.workflow)
        self.assertNotIn("git push origin HEAD:feat/tcl-v22-updates-back-bluetooth", self.workflow)
        self.assertNotIn("ANYTV_SIGNER_V2_RECOVERY_HEX", self.workflow)
        self.assertNotIn("apksigner sign", self.workflow)

if __name__ == "__main__":
    unittest.main()
