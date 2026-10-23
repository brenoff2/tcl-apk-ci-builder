#!/usr/bin/env python3
"""Use only the TV's RSA mTLS identity. Never clear or export AndroidKeyStore keys."""
from pathlib import Path

p = Path("source/app/src/main/java/com/hari/androidtvremote/androidLib/ssl/KeyStoreManager.java")
src = p.read_text(encoding="utf-8")
old = """    public KeyManager[] getKeyManagers() throws GeneralSecurityException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(this.mKeyStore, "".toCharArray());
        return keyManagerFactory.getKeyManagers();
    }"""
new = """    // The Cloudflare resolver adds a separate EC/P-256 AndroidKeyStore alias.
    // Present only the Android TV RSA identity to the local TV TLS endpoints.
    public KeyManager[] getKeyManagers() throws GeneralSecurityException {
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(this.mKeyStore, "".toCharArray());
        javax.net.ssl.X509KeyManager selected = null;
        for (KeyManager km : factory.getKeyManagers()) {
            if (km instanceof javax.net.ssl.X509KeyManager) {
                selected = (javax.net.ssl.X509KeyManager) km;
                break;
            }
        }
        if (selected == null || selected.getPrivateKey(SERVER_IDENTITY_ALIAS) == null
                || selected.getCertificateChain(SERVER_IDENTITY_ALIAS) == null
                || selected.getCertificateChain(SERVER_IDENTITY_ALIAS).length == 0
                || !(selected.getCertificateChain(SERVER_IDENTITY_ALIAS)[0].getPublicKey()
                    instanceof java.security.interfaces.RSAPublicKey)) {
            throw new GeneralSecurityException("TV RSA identity unavailable; keystore unchanged");
        }
        final javax.net.ssl.X509KeyManager rsa = selected;
        return new KeyManager[]{new javax.net.ssl.X509ExtendedKeyManager() {
            private boolean isRsa(String type) { return "RSA".equalsIgnoreCase(type); }
            @Override public String[] getClientAliases(String type, java.security.Principal[] issuers) {
                return isRsa(type) ? new String[]{SERVER_IDENTITY_ALIAS} : null;
            }
            @Override public String chooseClientAlias(String[] types, java.security.Principal[] issuers,
                                                      java.net.Socket socket) {
                if (types != null) for (String type : types) if (isRsa(type)) return SERVER_IDENTITY_ALIAS;
                return null;
            }
            @Override public String chooseEngineClientAlias(String[] types, java.security.Principal[] issuers,
                                                            javax.net.ssl.SSLEngine engine) {
                return chooseClientAlias(types, issuers, null);
            }
            @Override public String[] getServerAliases(String type, java.security.Principal[] issuers) {
                return null;
            }
            @Override public String chooseServerAlias(String type, java.security.Principal[] issuers,
                                                      java.net.Socket socket) { return null; }
            @Override public String chooseEngineServerAlias(String type, java.security.Principal[] issuers,
                                                            javax.net.ssl.SSLEngine engine) { return null; }
            @Override public X509Certificate[] getCertificateChain(String alias) {
                return SERVER_IDENTITY_ALIAS.equals(alias) ? rsa.getCertificateChain(alias) : null;
            }
            @Override public java.security.PrivateKey getPrivateKey(String alias) {
                return SERVER_IDENTITY_ALIAS.equals(alias) ? rsa.getPrivateKey(alias) : null;
            }
        }};
    }"""
if src.count(old) != 1:
    raise RuntimeError("Source version mismatch; abort")
p.write_text(src.replace(old, new), encoding="utf-8")
print("TV_RSA_TLS_IDENTITY_PINNED_CLOUDFLARE_KEY_UNTOUCHED")
