# AnyTV permanent signing recovery (2026-10-15)

This repository stores **only** an AES-256-GCM encrypted PKCS#12 keystore backup at `ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin`.

The separate decryption secret is stored privately in the user's Google Drive folder `AnyTV - Assinaturas Privadas - Nao Compartilhar` as `ANYTV_RECOVERY_SECRET_2026_PRIVADO.txt`. **Never commit that secret or plaintext keystores, nor print them in CI logs.**

File structure: 9-byte ASCII `ANYTVAES1`, 12-byte nonce, AES-GCM ciphertext + 16-byte tag; 256-bit key in separate recovery file (hex). Decrypt with Python cryptography AESGCM and extract the in-memory ZIP into a private working directory. ZIP holds `anytv-phone-migration.p12`, `anytv-tv-migration.p12`, `phone.password`, `tv.password`, `IDENTIDADES.json`.

Signing identities: phone SHA-256 `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`; TV SHA-256 `0a5427237a2445c42520236469563dabceda6d43310fe4e6aea6af8ddffc223c`.

**Critical:** These are NEW certificate identities, requiring a one-time uninstall/reinstall from the previous phone/TV certificates. Preserve user app data before removing the old installations. Future releases must use the same keys, verify the certificate fingerprints, package names, versionCode and APK v2 signing, and must not rotate keys again. The old branch's `current.apk` paths belong to the old installed identities and must NOT be silently overwritten with these new-cert APKs.
