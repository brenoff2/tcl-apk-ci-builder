# AnyTV — GitHub CI, assinatura e OTA

O código, os APKs compilados e a publicação do canal Android são gerenciados no GitHub. A atualização do aplicativo lê diretamente `updates/android/cert2026/current.apk` pela rota HTTPS do GitHub.

## Fluxo único de publicação (Android cert2026)

1. A compilação gera o APK não assinado em **GitHub Actions Artifacts**.
2. O publicador `.github/workflows/publish-anytv-phone-github-only.yml` autentica o artefato do build no GitHub, recupera a identidade de assinatura **dentro do runner** a partir de `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin` e do GitHub Actions Secret `ANYTV_RECOVERY_SECRET_2026`.
3. O publicador valida SHA-256, `com.tclremote.livre`, `versionCode`, assinatura v2 e certificado cert2026, grava apenas `updates/android/cert2026/current.apk` e confirma o hash remoto.
4. Teste funcional na TCL permanece gate separado: CI/apksigner não demonstram pareamento real.

Se o segredo não estiver acessível ao runner, o workflow falha **antes de substituir a OTA**. Jamais gravar a credencial privada em arquivos versionados, logs, configurações públicas ou APK.

## Estado verificado em 2026-10-23

- Candidato Android 2.9.13 / código 42: compilação GitHub Actions `39891891049` aprovada e APK assinado localmente com a identidade original.
- Publicador GitHub-only: commit `129e86a7d01af0260f15a36af52a4a05cc8d6e22`; execução `39955533005` falhou porque o GitHub Actions não recebeu um segredo de recuperação.
- Canal OTA publicado: permanece 2.9.12 / código 41; **2.9.13 não publicada**.
- O problema de TLS com a TCL não foi homologado como corrigido. Nenhuma alteração no canal da TV.

## Desenvolvimento

Fonte Android TV Remote Apache-2.0; scripts e overrides de configuração no repositório. Branch OTA do telefone: `feat/tcl-v22-updates-back-bluetooth`. Branch de desenvolvimento: `feat/anytv-p0-device-proof-verifier-20261020`.
