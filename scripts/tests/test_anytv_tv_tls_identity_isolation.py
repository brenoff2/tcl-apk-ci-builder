"""Regression: Android TV TLS must not offer Cloudflare's P-256 key."""
import unittest
from pathlib import Path

ROOT = Path("source/app/src/main/java/com/hari/androidtvremote")
class TvTlsIdentityIsolation(unittest.TestCase):
    def test_rsa_alias_pinned(self):
        s=(ROOT/"androidLib/ssl/KeyStoreManager.java").read_text()
        self.assertIn('SERVER_IDENTITY_ALIAS = "androidtv-local"',s)
        self.assertIn('javax.net.ssl.X509ExtendedKeyManager()',s)
        self.assertIn('isRsa(type)',s)
        self.assertIn('return SERVER_IDENTITY_ALIAS.equals(alias) ? rsa.getPrivateKey(alias) : null;',s)
        self.assertIn('return SERVER_IDENTITY_ALIAS.equals(alias) ? rsa.getCertificateChain(alias) : null;',s)
        self.assertIn('TV RSA identity unavailable; keystore unchanged',s)
    def test_key_material_not_mutated(self):
        s=(ROOT/"androidLib/ssl/KeyStoreManager.java").read_text()
        isolated=s.split('public KeyManager[] getKeyManagers()',1)[1].split('public TrustManager[] getTrustManagers()',1)[0]
        for forbidden in ('clearKeyStore(', 'setKeyEntry(', 'deleteEntry(', 'generateKeyPair(', 'KeyPairGenerator.'):
            self.assertNotIn(forbidden, isolated)
    def test_cloudflare_p256_is_independent(self):
        s=(ROOT/"ui/app/AnyTvDeviceIdentity.kt").read_text()
        self.assertIn('ALIAS = "anytv-resolver-p256-v1"',s)
        self.assertIn('KEY_ALGORITHM_EC',s)
        self.assertIn('RESOLVER_BASE',s)
    def test_tv_protocol_still_rsa_and_ports(self):
        remote=(ROOT/"androidLib/AndroidRemoteTv.java").read_text()
        challenge=(ROOT/"androidLib/pairing/PairingChallengeResponse.java").read_text()
        self.assertIn('new RemoteSession(host, 6466',remote)
        self.assertIn('mPairingSession.pair(host, 6467',remote)
        self.assertIn('Only supports RSA public keys',challenge)
if __name__ == "__main__":
    unittest.main()
