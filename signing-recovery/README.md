# AnyTV permanent signing recovery (2026-10-15)

This repository stores **only** an AES-256-GCM encrypted PKCS#12 keystore backup at `ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin`.

Production signing is performed **only in GitHub Actions** using the encrypted backup already tracked here and a separately configured GitHub Actions repository secret `ANYTV_RECOVERY_SECRET_2026` (the ONLY supported secret name). Repository secrets are injected into the runner, not stored as text files. If none is available, the release must fail before publishing. **Never commit the decryption secret, plaintext keystores or passwords, nor print them in CI logs.**

File structure: 9-byte ASCII `ANYTVAES1`, 12-byte nonce, AES-GCM ciphertext + 16-byte tag; 256-bit key in separate recovery file (hex). Decrypt with Python cryptography AESGCM and extract the in-memory ZIP into a private working directory. ZIP holds `anytv-phone-migration.p12`, `anytv-tv-migration.p12`, `phone.password`, `tv.password`, `IDENTIDADES.json`.

Signing identities: phone SHA-256 `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`; TV SHA-256 `0a5427237a2445c42520236469563dabceda6d43310fe4e6aea6af8ddffc223c`.

**Critical:** These are NEW certificate identities, requiring a one-time uninstall/reinstall from the previous phone/TV certificates. Preserve user app data before removing the old installations. Future releases must use the same keys, verify the certificate fingerprints, package names, versionCode and APK v2 signing, and must not rotate keys again. The old branch's `current.apk` paths belong to the old installed identities and must NOT be silently overwritten with these new-cert APKs.

## Processo canonico unico do celular

- Publicador exclusivo: `.github/workflows/publish-anytv-phone-github-only.yml`.
- Entrada: ID de execucao de compilacao Android bem-sucedida no GitHub Actions (branch `feat/anytv-p0-device-proof-verifier-20261020`).
- Fonte: artefato unsigned auditado e checksum fornecidos pelo proprio GitHub Actions. O publicador assina no runner, compara SHA/certificado e grava apenas `updates/android/cert2026/current.apk` apos garantir versionCode superior. O readback remoto deve bater byte a byte.
- Identidade assinante: backup AES-256-GCM acima, desbloqueado SOMENTE por `secrets.ANYTV_RECOVERY_SECRET_2026`; o segredo e senha nao devem aparecer em arquivos ou logs. Nenhum outro segredo, chave, metodo local ou URL temporaria e rota oficial.
- Diagnostico 2026-10-23: build 2.9.13 `39891891049` OK, mas run de publicacao `39960928074` BLOQUEADO antes de assinar porque a expressao do segredo resultou vazia. Isso significa configuracao de acesso ainda pendente, NAO falta da chave original ou novo certificado.
- Registro administrativo seguro: GitHub Settings > Secrets and variables > Actions > Repository secrets > `ANYTV_RECOVERY_SECRET_2026`; sem secreta no repo e sem exposicao do valor.
