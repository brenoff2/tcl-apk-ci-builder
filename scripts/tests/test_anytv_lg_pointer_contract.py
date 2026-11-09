#!/usr/bin/env python3
"""Contract for LG webOS pointer repairs; run against prepared upstream source."""
import unittest
from pathlib import Path

ROOT = Path("source/app/src/main/java/com/hari/androidtvremote")
WEBOS = Path("source/ConnectSDK/src/main/java/com/connectsdk/service/WebOSTVService.java")

class LgPointerContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.vm = (ROOT / "ui/app/TvRemoteViewModel.kt").read_text()
        cls.nav = (ROOT / "navigation/AppNavGraph.kt").read_text()
        cls.home = (ROOT / "ui/app/TclApprovedHomeScreen.kt").read_text()
        cls.pad = (ROOT / "ui/app/TclResponsivePad.kt").read_text()
        cls.webos = WEBOS.read_text()

    def test_direct_webos_service_for_dpad(self):
        self.assertIn("val keys = webOs.getKeyControl()", self.vm)
        self.assertIn("val volume = webOs.getVolumeControl()", self.vm)
        self.assertIn("service.connectMouse()", self.vm)
        self.assertIn("CONTROL_MOUSE_AND_KEYBOARD", self.vm)
        self.assertIn("DiscoveryManager.PairingLevel.ON", self.vm)
        self.assertIn("service.setPermissions(targetPermissions)", self.vm)
        self.assertIn("lg_webos_pointer_scope_v1_", self.vm)
        self.assertIn("sendLgWebOsKey(keyCode)", self.vm)

    def test_webos_pointer_socket_ready_and_recovers(self):
        self.assertIn("mouseSocket != null && mouseSocket.isConnected()", self.webos)
        self.assertIn("disconnectMouse();", self.webos)
        self.assertIn('sendSpecialKey("UP", listener)', self.webos)
        self.assertIn('sendSpecialKey("HOME", listener)', self.webos)

    def test_true_pointer_motion_through_home_navigation(self):
        self.assertIn("fun sendLgWebOsPointerMove(dx: Float, dy: Float)", self.vm)
        self.assertIn("fun sendLgWebOsPointerClick()", self.vm)
        self.assertIn("onLgPointerMove = tvRemoteViewModel::sendLgWebOsPointerMove", self.nav)
        self.assertIn("onLgPointerClick = tvRemoteViewModel::sendLgWebOsPointerClick", self.nav)
        self.assertIn("onLgPointerMove = onLgPointerMove", self.home)
        self.assertIn("isLgWebOs = sessionState.connectedDevice?.type", self.home)
        self.assertIn("detectDragGestures", self.pad)
        self.assertIn("latestMove(dragAmount.x", self.pad)
        self.assertIn("latestClick()", self.pad)
        self.assertIn("else RemotePadStage(", self.pad)  # TCL unchanged

if __name__ == "__main__":
    unittest.main()
