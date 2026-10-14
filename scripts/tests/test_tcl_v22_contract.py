#!/usr/bin/env python3
import unittest
from pathlib import Path

APP = Path("source/app")
SRC = APP / "src/main/java"
UI = SRC / "com/hari/androidtvremote/ui/app"
TV = Path("tv-source/app")
TVSRC = TV / "src/main/java/dk/fez/bluetoothquickconnect"


class PhoneUpdatesAndBack(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.skin = (UI / "TclApprovedHomeScreen.kt").read_text()
        cls.settings = (UI / "SettingsScreen.kt").read_text()
        cls.manifest = (APP / "src/main/AndroidManifest.xml").read_text()
        cls.apk = (APP / "build.gradle.kts").read_text()
        cls.updater = (SRC / "tcl/shared/TclUpdateManager.java").read_text()

    def test_back_is_tv_key_not_android_exit(self):
        self.assertIn("BackHandler(enabled = isConnected && !showKeyboard", self.skin)
        self.assertIn("onRemoteKey(Remotemessage.RemoteKeyCode.KEYCODE_BACK)", self.skin)

    def test_updated_remote_ui_is_not_stretched(self):
        self.assertIn("val padHeight = padWidth", self.skin)
        self.assertIn("val rockerWidth = if (maxWidth < 340.dp) 46.dp else 57.dp", self.skin)
        self.assertNotIn('Text("Reprodução"', self.skin)
        self.assertIn("fun TclLiquidFill(", self.skin)
        self.assertIn("fluidLevel = sessionState.volumeFraction", self.skin)
        self.assertIn("sessionState.isMuted", self.skin)
        self.assertIn("A slim toolbar", self.skin)

    def test_update_button_and_signed_install(self):
        self.assertIn('title = "Buscar atualizações"', self.settings)
        self.assertIn("TclUpdateManager.check(activity)", self.settings)
        self.assertIn("REQUEST_INSTALL_PACKAGES", self.manifest)
        self.assertIn('android:exported="false"', self.manifest)
        self.assertIn("getPackageArchiveInfo(", self.updater)
        self.assertIn("getApkContentsSigners()", self.updater)
        self.assertIn("MessageDigest.isEqual(currentSigner(installed), currentSigner(next))", self.updater)
        self.assertIn("PackageInstaller.SessionParams.USER_ACTION_REQUIRED", self.updater)
        self.assertIn("digest.substring(7).equals(sha256File(apk))", self.updater)
        self.assertNotIn("SilentInstall", self.updater)

    def test_single_package_higher_version(self):
        self.assertIn('applicationId = "com.tclremote.livre"', self.apk)
        self.assertIn("versionCode = 22", self.apk)


class TvUpdateAndBluetooth(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.main = (TVSRC / "MainActivity.java").read_text()
        cls.bt = (TVSRC / "BluetoothRecoverAction.java").read_text()
        cls.bridge = (TVSRC / "TvAppBridge.java").read_text()
        cls.manifest = (TV / "src/main/AndroidManifest.xml").read_text()
        cls.gradle = (TV / "build.gradle.kts").read_text()
        cls.updater = (TV / "src/main/java/tcl/shared/TclUpdateManager.java").read_text()

    def test_tv_buttons_readable_dpad_focus(self):
        self.assertIn("state_focused", self.main)
        self.assertIn("dp(76)", self.main)
        self.assertIn("setTextSize(TypedValue.COMPLEX_UNIT_SP, 20)", self.main)
        self.assertIn('action("Buscar atualizações"', self.main)
        self.assertIn("Color.rgb(255, 230, 86)", self.main)

    def test_bluetooth_real_sequence_and_status(self):
        self.assertIn('invoke(proxy, "disconnect", device)', self.bt)
        self.assertIn('invoke(proxy, "connect", device)', self.bt)
        self.assertIn("BluetoothProfile.STATE_CONNECTED", self.bt)
        self.assertIn('finish.accept("api_restricted")', self.bt)
        self.assertNotIn("DeviceSettingsFallback.open(", self.bt)
        self.assertIn('parts[1].equals("/headphones/status")', self.bridge)
        self.assertIn("headphoneStatus = result", self.bridge)

    def test_tv_inplace_update_support(self):
        self.assertIn("versionCode = 5", self.gradle)
        self.assertIn('applicationId = "com.tclremote.tvcompanion"', self.gradle)
        self.assertIn("REQUEST_INSTALL_PACKAGES", self.manifest)
        self.assertNotIn("BIND_ACCESSIBILITY_SERVICE", self.manifest)
        self.assertIn("TV_CERT_A", self.updater)
        self.assertIn("TV_CERT_B", self.updater)
        self.assertIn("TclInstallResultReceiver", self.manifest)


if __name__ == "__main__":
    unittest.main()
