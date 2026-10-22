#!/usr/bin/env python3
"""Release contracts for the Android TV secure re-pair recovery path."""
import unittest
from pathlib import Path

ROOT = Path("source/app/src/main/java/com/hari/androidtvremote")


class SecurePairingRecovery(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.android_remote = (ROOT / "androidLib/AndroidRemoteTv.java").read_text()
        cls.view_model = (ROOT / "ui/app/TvRemoteViewModel.kt").read_text()
        cls.keystore = (ROOT / "androidLib/ssl/KeyStoreManager.java").read_text()

    def test_tls_reconnect_runs_existing_secure_pairing_flow(self):
        reconnect = self.android_remote.split("public void reconnect(", 1)[1].split("public void disconnect(", 1)[0]
        self.assertIn("public void onSslError()", reconnect)
        self.assertIn("androidTvListener.onSecretRequested();", reconnect)
        self.assertNotIn("SSL certificate error. Re-pair the device.", reconnect)

    def test_fresh_pairing_reports_ssl_errors_instead_of_hanging(self):
        fresh = self.android_remote.split("public void connect(", 1)[1].split("public void reconnect(", 1)[0]
        self.assertIn("notifyOnError(\"Secure pairing handshake failed. Please retry.\");", fresh)
        self.assertIn("mPairingSession.pair(host, 6467", fresh)

    def test_repair_is_bounded_and_preserves_other_app_data(self):
        reconnect = self.view_model.split("private fun reconnectRemote(", 1)[1].split("private fun createRemoteListener(", 1)[0]
        self.assertIn("connectionWatchdogJob?.cancel()", reconnect)
        self.assertIn("autoReconnectInFlight.set(false)", reconnect)
        self.assertIn("remove(Constant.HOST)", reconnect)
        self.assertIn("putBoolean(Constant.PIN, false)", reconnect)
        self.assertIn("_navigateToDiscoveryTrigger.value = true", reconnect)
        self.assertIn("connectRemote(", reconnect)
        self.assertIn("if (allowFreshFallback)", reconnect)
        self.assertIn("Pairing", self.view_model)
        self.assertNotIn("KeyStoreManager().clear()", self.view_model)
        self.assertIn('return true;', self.keystore)  # AndroidKeyStore preserved


if __name__ == "__main__":
    unittest.main()
