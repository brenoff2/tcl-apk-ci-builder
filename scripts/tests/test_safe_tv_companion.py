#!/usr/bin/env python3
"""TV companion permission and feature regression tests."""
import re
import unittest
from pathlib import Path

APP = Path("tv-source/app")
SRC = APP / "src/main/java/dk/fez/bluetoothquickconnect"


class SafeTvCompanionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = (APP / "src/main/AndroidManifest.xml").read_text()
        cls.screen = (SRC / "MainActivity.java").read_text()
        cls.bridge = (SRC / "TvAppBridge.java").read_text()
        cls.service = (SRC / "TclCatalogService.java").read_text()
        cls.gradle = (APP / "build.gradle.kts").read_text()

    def test_no_accessibility_or_sensitive_package_permissions(self):
        self.assertNotIn("accessibility", self.manifest.lower())
        self.assertNotIn("BLUETOOTH", self.manifest)
        for forbidden in ["QUERY_ALL_PACKAGES", "SYSTEM_ALERT_WINDOW",
                          "BIND_ACCESSIBILITY_SERVICE", "READ_SMS",
                          "BIND_NOTIFICATION_LISTENER_SERVICE", "REQUEST_INSTALL_PACKAGES"]:
            self.assertNotIn(forbidden, self.manifest)
        self.assertNotIn("AccessibilityService", self.service)
        self.assertFalse((SRC / "RemoteButtonService.java").exists())
        self.assertFalse(list((APP / "src/main/res/xml").glob("*")))

    def test_foreground_service_user_visible_and_not_exported(self):
        self.assertIn('android:foregroundServiceType="specialUse"', self.manifest)
        self.assertIn('android:exported="false"', self.manifest)
        self.assertIn("NotificationChannel", self.service)
        self.assertIn("startForeground(", self.service)
        self.assertIn("START_STICKY", self.service)
        self.assertIn("stopService(", self.screen)

    def test_original_tv_inventory_and_launch_preserved(self):
        self.assertIn("PackageManager", self.bridge)
        self.assertIn("Intent.CATEGORY_LEANBACK_LAUNCHER", self.bridge)
        self.assertIn("getLaunchIntentForPackage", self.bridge)
        self.assertIn('parts[1].equals("/apps")', self.bridge)
        self.assertIn('parts[1].equals("/launch")', self.bridge)
        self.assertIn("X-TCL-Pair", self.bridge)
        self.assertIn("installedApps()", self.bridge)
        self.assertIn("TvAppBridge.pairingCode(this)", self.screen)

    def test_package_and_version_and_no_old_java(self):
        self.assertIn('applicationId = "com.tclremote.tvcompanion"', self.gradle)
        self.assertIn("versionCode = 3", self.gradle)
        self.assertIn('versionName = "1.2-Sem-Acessibilidade"', self.gradle)
        actual = {p.name for p in SRC.glob("*.java")}
        self.assertEqual(
            actual, {"MainActivity.java", "TclCatalogService.java", "TvAppBridge.java"}
        )


if __name__ == "__main__":
    unittest.main()
