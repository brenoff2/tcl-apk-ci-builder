"""Contract tests for TCL ADB phone-only DEV candidate. No device connection."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "overrides/TclAdbInstalledApps.kt"
PATCH = ROOT / "scripts/apply_anytv_tcl_adb.py"


class PhoneAdbContract(unittest.TestCase):
    def test_opt_in_and_companion_fallback(self):
        s = PATCH.read_text()
        self.assertIn('adb_enabled:', s)
        self.assertIn('if (useAdb) adbApps.inventory(host) else TclTvAppApi.apps(host, pairing)', s)
        self.assertIn('if (useAdb) adbApps.launch(host, app.packageName)', s)
        self.assertIn('if (!useAdb) storePair(pairing)', s)
        self.assertNotIn('adb_enabled:", true', s)

    def test_adb_requires_tv_authorization(self):
        s = SOURCE.read_text()
        self.assertIn('AdbConnection.create(socket, keyPair())', s)
        self.assertIn('Autorize o AnyTV', s)
        self.assertIn('context.filesDir', s)
        self.assertIn('InetSocketAddress(host, 5555)', s)
        self.assertNotIn('adb tcpip 5555', s)
        self.assertNotIn('adb root', s)

    def test_device_truth_not_catalog(self):
        s = SOURCE.read_text()
        self.assertIn('pm list packages', s)
        self.assertIn('android.intent.category.LEANBACK_LAUNCHER', s)
        self.assertIn('parseLaunchablePackages', s)
        self.assertIn('getPackageArchiveInfo', s)
        self.assertIn('splitSourceDirs', s)
        self.assertNotIn('com.netflix.ninja', s)
        self.assertNotIn('com.spotify.tv.android', s)

    def test_scope_and_bounded_data(self):
        s = SOURCE.read_text()
        for phrase in ('maxShellBytes', 'maxApkBytes', 'maxApksPerApp',
                       'file.writeBytes', 'files.forEach { runCatching { it.delete() } }',
                       'require(packagePattern.matches(packageName))'):
            self.assertIn(phrase, s)
        self.assertNotIn('pm uninstall', s)
        self.assertNotIn('pm install', s)

    def test_overlay_only_in_dev(self):
        s = PATCH.read_text()
        self.assertIn('copyfile("overrides/TclAdbInstalledApps.kt"', s)
        self.assertIn('implementation("com.tananaev:adblib:1.3")', s)
        self.assertNotIn('updates/android/', s)
        self.assertNotIn('signingConfig', s)


if __name__ == "__main__":
    unittest.main()
