> **TRANSIÇÃO VIGENTE 23/10/2026:** Ler [Handoff integral](docs/handoffs/2026-10-23-anytv-full-transition.md), [Ponto atual](docs/handoffs/CURRENT_HANDOFF.md) e [ADR-0003 assinatura única](docs/decisions/ADR-0003-ANDROID-CERT2026-GITHUB-ONLY.md). OTA Android 2.9.12; 2.9.13 build/signed mas NÃO publicada; segredo GitHub Actions indisponível ao runner; TLS TCL NÃO homologado.\n\n# AnyTV — GitHub CI, assinatura e OTA

O código, os APKs compilados e a publicação do canal Android são gerenciados no GitHub. A atualização do aplicativo lê diretamente `updates/android/cert2026/current.apk` pela rota HTTPS do GitHub.

## Fluxo único de publicação (Android cert2026)

1. A compilação gera o APK não assinado em **GitHub Actions Artifacts**.
2. O publicador `.github/workflows/publish-anytv-phone-github-only.yml` autentica o artefato do build no GitHub, recupera a identidade de assinatura **dentro do runner** a partir de `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin` e do GitHub Actions Secret `ANYTV_RECOVERY_SECRET_2026`.
3. O publicador valida SHA-256, `com.tclremote.livre`, `versionCode`, assinatura v2 e certificado cert2026, grava apenas `updates/android/cert2026/current.apk` e confirma o hash remoto.
4. Teste funcional na TCL permanece gate separado: CI/apksigner não demonstram pareamento real.

Se o segredo não estiver acessível ao runner, o workflow falha **antes de substituir a OTA**. Jamais gravar a credencial privada em arquivos versionados, logs, configurações públicas ou APK.

## Estado verificado em 2026-10-23

- Candidato Android 2.9.13 / código 42: compilação GitHub Actions `39891891049` aprovada e APK assinado localmente com a identidade original.
- Publicador oficial genérico: commit `281a37ab2839c6c8c16e5d871b69a684f3f26270`; execução `39960928074` falhou no gate de acesso ao segredo `ANYTV_RECOVERY_SECRET_2026`, antes de assinar ou publicar.
- Canal OTA publicado: permanece 2.9.12 / código 41; **2.9.13 não publicada**.
- O problema de TLS com a TCL não foi homologado como corrigido. Nenhuma alteração no canal da TV.
- Os publicadores anteriores do Android foram **excluidos das duas branches de trabalho**, mas permanecem recuperaveis no historico Git. Somente `.github/workflows/publish-anytv-phone-github-only.yml` grava a OTA cert2026 do telefone. Publicadores exclusivos da TV foram preservados.
- O verificador canônico `.github/workflows/verify-anytv-update-endpoint.yml` foi aprovado no GitHub Actions run `39962660246`; verificou assinatura do APK atual e readback remoto byte a byte.

## Desenvolvimento

Fonte Android TV Remote Apache-2.0; scripts e overrides de configuração no repositório. Branch OTA do telefone: `feat/tcl-v22-updates-back-bluetooth`. Branch de desenvolvimento: `feat/anytv-p0-device-proof-verifier-20261020`.
