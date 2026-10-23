# ANYTV V2 — RUNBOOK DE ASSINATURA E RESTAURAÇÃO (fonte atual)

> **Atualizado 23/10/2026 após release 2.9.17/code46.** Este runbook prevalece, PARA O PACOTE V2, sobre quaisquer instruções históricas do cert2026. NÃO revela segredo nem é um comando para executar bootstrap. Completo em [handoff atual](../handovers/2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md).

## Qual identidade devo usar? (resposta definitiva)

**Sempre a mesma identidade Android V2 já estabelecida:**

- Aplicativo Android instalado: `com.tclremote.livre.v2`.
- Certificado SHA-256: `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`.
- Material cifrado no repositório GitHub, na **branch DEV** `feat/anytv-p0-device-proof-verifier-20261020`: `signing-recovery/v2/phone-signer.aesgcm` (PKCS12 cifrado; Git blob `3552f69853ed2f626644114c07ed6a0ec822ce38`), `signing-recovery/v2/phone-cert.pem` (PÚBLICO), `signing-recovery/v2/identity.json` (fingerprint e contrato).
- Segredo de recuperação **já configurado** no GitHub Actions: `secrets.ANYTV_SIGNER_V2_RECOVERY_HEX` (Repository Secret, 64 dígitos hex / 256 bits), disponível ao workflow como environment variable **somente em execução autorizada**. O segredo nunca é legível pela API/UI depois de salvo; não solicitar seu valor ao usuário nem colocá-lo no chat, workflow literal, commit ou logs.
- Fluxo usado em produção: `.github/workflows/release-anytv-phone-v2-new-package.yml` (branch DEV). Gatilho por push em `.github/anytv-phone-v2-build-request.json`.
- Canal OTA do próprio V2: branch `feat/tcl-v22-updates-back-bluetooth`, arquivo `updates/android/v2/current.apk`, URL raw correspondente. **Última publicação comprovada 2.9.17/code46**, run `40021931010` SUCCESS, SHA256 APK `0a80dd4cabf16c5c67a36520dd2e2dadda52b77c28e902c0d7c0beb95bdfc475`.
- O usuário instalou e conectou o V2; novas versões **devem atualizar por cima do V2**, nunca exigir reinstalação ou novo certificado.

## O que aconteceu com a chave antiga?

A identidade **cert2026** é outra assinatura SHA256 `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`, pacote `com.tclremote.livre`, OTA `updates/android/cert2026/current.apk` (última 2.9.12/code41). A tentativa antiga de recuperar a credencial para novas versões causou grande desgaste e o usuário **expressamente determinou criar uma identidade nova**. O arquivo Google Drive `ANYTV_RECOVERY_SECRET_2026_PRIVADO.txt` é OBSOLETO/REJEITADO e NÃO faz parte da nova linha. A migração in-place do pacote antigo para assinatura nova é incompatível sem uma linhagem de assinatura válida assinada também pelo certificado antigo; por isso o V2 tem ID de pacote diferente. Mantenha as duas OTAs isoladas. Não alterar o companion TV.

## Como foi gerada, protegida e testada a chave atual (NÃO repetir)

1. O GitHub Actions `39998409528` gerou **dentro do runner** par RSA-3072 e certificado X.509, serializou um keystore PKCS12 e o criptografou com AES-256-GCM usando derivação da chave de recuperação fornecida por Repository Secret.
2. Formato do backup `phone-signer.aesgcm`: prefixo ASCII `ANYTVV2G1` + nonce 12 B + AES-GCM ciphertext com tag. SHA256 do ciphertext no `identity.json`: `be7ac4686ca6dd6817ba4a662095f71313470663c802f0b0ba22cd6a023b9f5b`.
3. A senha do keystore é derivada deterministicamente no runner: `base64url(HMAC-SHA256(recovery_bytes, "AnyTV-Phone-V2-PKCS12-Pass-v1"))`. O valor da senha NUNCA é exposto nem versionado.
4. O bootstrap fez commit APENAS do arquivo criptografado, certificado público e manifesto; baixou o arquivo do próprio GitHub e fez decrypt, abriu o PKCS12 e verificou assinatura contra a chave pública, `ANYTV_V2_SIGNER_GITHUB_ENCRYPTED_READBACK_AND_RECOVERY_PASS`.
5. Release run `40021931010` confirmou `ANYTV_V2_ENCRYPTED_BACKUP_RECOVERED_WITH_GITHUB_ACTIONS_SECRET` e `ANYTV_V2_SIGNER_VALIDATED_0a80dd4...`; publicou e verificou `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`.
6. O bootstrap `.github/workflows/bootstrap-anytv-phone-v2-identity.yml` é **ONE-TIME HISTÓRICO**. **NÃO acioná-lo**; o script tem gates que impedem rebootstrap sobre arquivos existentes.

## Procedimento para o próximo release (ex.: 2.9.18/code47)

1. Preparar o código DEV e testes de interface/protocolo/reconexão; obter status verde real e resolver bloqueios de escrita autorizadamente.
2. Abrir workflow existente `.github/workflows/release-anytv-phone-v2-new-package.yml`, preservar os passos de recuperação/apksigner/checksum/branch OTA. Ajustar **somente** versionName, versionCode, nomes dos APKs, checks de versionCode e comparação monotônica (para code47, antiga OTA code46).
3. Atualizar `.github/anytv-phone-v2-build-request.json` para `expected_version_code=47`, `release_version=2.9.18-AnyTV` e escopo real aprovado. O push aciona o workflow.
4. O runner lê `secrets.ANYTV_SIGNER_V2_RECOVERY_HEX`, decripta o BACKUP EXISTENTE em diretório privado temporário, deriva a senha, verifica `identity.json`, assina o unsigned com `apksigner` e confere certificado público e versão/pacote. Chaves/senhas só vivem no runner e são eliminadas ao final; nunca trafegar valores de segredo por ferramentas do chat.
5. Só o pipeline verde publica `updates/android/v2/current.apk` na branch OTA. Verificar `GitHub Actions SUCCESS`, run, SHA256, Git blob, cert, versionCode >46, readback HTTP byte a byte e que `updates/android/cert2026/current.apk` não mudou.
6. Homologar pelo botão **Buscar atualizações** no V2 instalado e testar com TV física. O teste de CI não prova sozinho reprodução de vídeo ou estado de links.

## Problemas e contingência

- Se `ANYTV_SIGNER_V2_RECOVERY_HEX` estiver ausente no runner, **NÃO criar chave nova**: falhar fechado e corrigir o acesso ao mesmo Secret por controles oficiais do GitHub. Nunca usar arquivo de Drive.
- Se APK assinado tiver fingerprint divergente, versionCode errado ou package errado: **NÃO PUBLICAR**.
- Uma segunda chave chamada escrow chegou a ser provisionada, mas **não existe backup secundário criptografado por ela** (`signing-recovery/v2/phone-signer-escrow.aesgcm` ausente). A recuperação principal já foi provada, mas **a contingência independente contra perda do GitHub Secret ainda NÃO está pronta**. Qualquer plano de contingência exige custódia segura, aprovação e prova de readback; nunca registrar segredo aqui.
- **Não copiar a chave privada/PKCS12 em claro para Git, Drive, logs ou conversa.** O arquivo cifrado pertence à DEV; a OTA só precisa do APK já assinado.

## Evidências e fontes

- [ADR assinatura V2](https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/anytv-p0-device-proof-verifier-20261020/docs/decisions/ADR-0004-NEW-ANDROID-SIGNER-V2-GITHUB-CUSTODY.md) (DEV).
- [Manifesto da identidade V2](https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/anytv-p0-device-proof-verifier-20261020/signing-recovery/v2/identity.json).
- [Workflow de assinatura e publicação V2](https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/anytv-p0-device-proof-verifier-20261020/.github/workflows/release-anytv-phone-v2-new-package.yml).
- [Bootstrap de auditoria, só histórico](https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/anytv-p0-device-proof-verifier-20261020/.github/workflows/bootstrap-anytv-phone-v2-identity.yml).
- [Handoff integral atual](../handovers/2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md).
