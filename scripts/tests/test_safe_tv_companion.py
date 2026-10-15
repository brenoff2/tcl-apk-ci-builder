#!/usr/bin/env python3
"""TV companion permission and feature regression tests."""
import re
import os
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
        self.assertIn("android.permission.BLUETOOTH_CONNECT", self.manifest)
        self.assertNotIn("BLUETOOTH_SCAN", self.manifest)
        self.assertNotIn("BLUETOOTH_PRIVILEGED", self.manifest)
        for forbidden in ["QUERY_ALL_PACKAGES", "SYSTEM_ALERT_WINDOW",
                          "BIND_ACCESSIBILITY_SERVICE", "READ_SMS",
                          "BIND_NOTIFICATION_LISTENER_SERVICE"]:
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
        self.assertIn('parts[1].equals("/headphones")', self.bridge)
        self.assertIn("MappingStore.get(context, KeyEvent.KEYCODE_PROG_RED)", self.bridge)
        self.assertIn("BluetoothRecoverAction.recover(context, mapping", self.bridge)
        self.assertIn("requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}", self.screen)
        self.assertIn("MappingStore.save(this, KeyEvent.KEYCODE_PROG_RED", self.screen)
        if os.environ.get("TCL_TV_V14") == "1":
            self.assertIn("TclUpdateManager.check(this", self.screen)
            self.assertIn("android.R.attr.state_focused", self.screen)
            self.assertIn('"/headphones/status"', self.bridge)
            self.assertIn("REQUEST_INSTALL_PACKAGES", self.manifest)
            self.assertNotIn("BIND_ACCESSIBILITY_SERVICE", self.manifest)

    def test_provider_title_deep_link_is_pinned_and_does_not_claim_playback(self):
        self.assertIn('parts[1].equals("/title-open")', self.bridge)
        self.assertIn("ZOOTOPIA_2_LINK", self.bridge)
        self.assertIn("Intent.ACTION_VIEW", self.bridge)
        self.assertIn("deepLink.setPackage(pkg)", self.bridge)
        self.assertIn("alternate.setPackage(pkg)", self.bridge)
        self.assertIn('return "search";', self.bridge)
        self.assertIn("www.disneyplus.com", self.bridge)
        self.assertIn("validDisneyTitleLink(pkg, url)", self.bridge)
        self.assertNotIn('startPlayback', self.bridge)
        self.assertIn('android:name="com.disney.disneyplus"', self.manifest)

    def test_provider_link_validation(self):
        for marker in ("validProviderTitleLink(", "openProviderTitle(", "www.netflix.com", "app.primevideo.com", "getEncodedQuery()",
                       "www.primevideo.com", "tv.apple.com", "play.hbomax.com",
                       "nflx://www.netflix.com/title/", "direct.setPackage(pkg)"):
            self.assertIn(marker, self.bridge)
        for pkg in ("com.netflix.ninja", "com.disney.disneyplus",
                    "com.amazon.amazonvideo.livingroom", "com.apple.atve.androidtv.appletv",
                    "com.wbd.stream"):
            self.assertIn(pkg, self.bridge)
            self.assertIn('android:name="' + pkg + '"', self.manifest)

    def test_package_and_version_and_no_old_java(self):
        self.assertIn('applicationId = "com.tclremote.tvcompanion"', self.gradle)
        self.assertIn("versionCode = " + ("14" if os.environ.get("TCL_TV_V23") == "1" else "13" if os.environ.get("TCL_TV_V22") == "1" else "12" if os.environ.get("TCL_TV_V21") == "1" else "11" if os.environ.get("TCL_TV_V20") == "1" else "10" if os.environ.get("TCL_TV_V19") == "1" else "9" if os.environ.get("TCL_TV_V18") == "1" else "8" if os.environ.get("TCL_TV_V17") == "1" else "7" if os.environ.get("TCL_TV_V16") == "1" else "6" if os.environ.get("TCL_TV_V15") == "1" else "5" if os.environ.get("TCL_TV_V14") == "1" else "4"), self.gradle)
        self.assertIn('versionName = "' + ("2.3-AnyTV" if os.environ.get("TCL_TV_V23") == "1" else "2.2-AnyTV" if os.environ.get("TCL_TV_V22") == "1" else "2.1-AnyTV" if os.environ.get("TCL_TV_V21") == "1" else "2.0-AnyTV" if os.environ.get("TCL_TV_V20") == "1" else "1.9-AnyTV" if os.environ.get("TCL_TV_V19") == "1" else "1.8-AnyTV" if os.environ.get("TCL_TV_V18") == "1" else "1.7-AnyTV" if os.environ.get("TCL_TV_V17") == "1" else "1.6-AnyTV" if os.environ.get("TCL_TV_V16") == "1" else "1.5-AnyTV" if os.environ.get("TCL_TV_V15") == "1" else "1.4-Catalogo-Fones-Atualiza" if os.environ.get("TCL_TV_V14") == "1" else "1.3-Catalogo-Fones") + '"', self.gradle)
        actual = {p.name for p in SRC.glob("*.java")}
        self.assertEqual(actual, {
            "MainActivity.java", "TclCatalogService.java", "TvAppBridge.java",
            "BluetoothConnectAction.java", "BluetoothRecoverAction.java",
            "DeviceSettingsFallback.java", "MappingStore.java"
        })


if __name__ == "__main__":
    unittest.main()
