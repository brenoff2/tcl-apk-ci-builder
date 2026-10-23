**ENTREGA NOVA IDENTIDADE — 23/10/2026 (VERIFICADA):** AnyTV V2 2.9.13/code42 distribuido como APK do pacote INDEPENDENTE `com.tclremote.livre.v2`. APK assinado GitHub Actions run `39998890413` SUCCESS, SHA256 `c407701f449645d9ee76bdc7a9600f7bc2dfbcc01b047010e95a567006058cb9`, certificado novo SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. Canal GitHub exclusivo V2: `updates/android/v2/current.apk` na branch OTA, blob `24aaa6c53d29810fca8303fdaf0b45fc6764a91d`, commit `4187a24b9129bee64bb535a2e2e7865640ba80e5`. O workflow validou APK, pacote, versão, cert e READBACK BYTE-A-BYTE `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`. A identidade V2 foi criada e seu backup AES-GCM persistido em `signing-recovery/v2/phone-signer.aesgcm`, manifesto público e cert `identity.json`, `phone-cert.pem`, dev HEAD no bootstrap `db6fa8a...`. Secret operacional privado `ANYTV_SIGNER_V2_RECOVERY_HEX` no GitHub Actions. Prova de recuperação `39998409528` SUCCESS, `ANYTV_V2_SIGNER_GITHUB_ENCRYPTED_READBACK_AND_RECOVERY_PASS`. **NÃO substitui a instalação antiga** sem chave/lineage; instalar em paralelo e reconfigurar pareamento da TCL. OTA original `updates/android/cert2026/current.apk` mantém 2.9.12/code41, blob `44c766b433ddb370f4589b87843f1edfd9cd7b7a`. Teste físico TCL V2 NÃO HOMOLOGADO. Segredo secundário `ANYTV_SIGNER_V2_ESCROW_HEX` consta no repositório, mas a segunda cópia do signer cifrada por ele ainda NÃO foi implementada (ferramenta bloqueou), logo NÃO tratar como redundância pronta. Consultar `docs/decisions/ADR-0004-NEW-ANDROID-SIGNER-V2-GITHUB-CUSTODY.md` na branch dev. 

> **TRANSIÇÃO VIGENTE 23/10/2026:** Ler [Handoff integral](docs/handoffs/2026-10-23-anytv-full-transition.md), [Ponto atual](docs/handoffs/CURRENT_HANDOFF.md) e [ADR-0003 assinatura única](docs/decisions/ADR-0003-ANDROID-CERT2026-GITHUB-ONLY.md). OTA Android 2.9.12; 2.9.13 build/signed mas NÃO publicada; secret GitHub Actions não comprovado no runner atual (falha histórica 39963496459); TLS TCL NÃO homologado.\n\n# AnyTV — GitHub CI, assinatura e OTA

**Regra atual de acesso e assinatura:** `docs/ASSINATURA_E_ATUALIZACAO.md` é a única instrução operacional. Verificar MCP GitHub direto e MCP Business OS Hub v2 antes de diagnosticar bloqueio; erro histórico de Actions não prova indisponibilidade presente do secret. Nunca usar ausência de `gh` no Windows como evidência de acesso GitHub.

O código, os APKs compilados e a publicação do canal Android são gerenciados no GitHub. A atualização do aplicativo lê diretamente `updates/android/cert2026/current.apk` pela rota HTTPS do GitHub.

## Fluxo único de publicação (Android cert2026)

Referência operacional de MCP/Secret: `docs/ASSINATURA_E_ATUALIZACAO.md`.

1. A compilação gera o APK não assinado em **GitHub Actions Artifacts**.
2. Com a credencial original acessível e a release aprovada, registra-se `.github/anytv-phone-release-request.json` na branch OTA com `candidate_run_id`, `expected_version_code` e `intent=publish-cert2026-phone`. Apenas o push desse arquivo aciona o publicador. Mudanças no workflow não publicam; não se usa `workflow_dispatch` fora da branch padrão.
3. O publicador `.github/workflows/publish-anytv-phone-github-only.yml` autentica o artefato do build no GitHub, recupera a identidade de assinatura **dentro do runner** a partir de `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin` e do GitHub Actions Secret `ANYTV_RECOVERY_SECRET_2026`.
4. O publicador valida SHA-256, `com.tclremote.livre`, `versionCode`, assinatura v2 e certificado cert2026, grava apenas `updates/android/cert2026/current.apk` e confirma o hash remoto.
5. Teste funcional na TCL permanece gate separado: CI/apksigner não demonstram pareamento real.

Se o segredo não estiver acessível ao runner, o workflow falha **antes de substituir a OTA**. Jamais gravar a credencial privada em arquivos versionados, logs, configurações públicas ou APK.

## Estado verificado em 2026-10-23

- Candidato Android 2.9.13 / código 42: compilação GitHub Actions `39891891049` aprovada e APK assinado localmente com a identidade original.
- Publicador oficial genérico: commit `281a37ab2839c6c8c16e5d871b69a684f3f26270`; execuções `39960928074` e `39963496459` falharam no gate de acesso ao segredo `ANYTV_RECOVERY_SECRET_2026`, antes de assinar ou publicar.
- Canal OTA publicado: permanece 2.9.12 / código 41; **2.9.13 não publicada**.
- O problema de TLS com a TCL não foi homologado como corrigido. Nenhuma alteração no canal da TV.
- Os publicadores anteriores do Android foram **excluidos das duas branches de trabalho**, mas permanecem recuperaveis no historico Git. Somente `.github/workflows/publish-anytv-phone-github-only.yml` grava a OTA cert2026 do telefone. Publicadores exclusivos da TV foram preservados.
- O verificador canônico `.github/workflows/verify-anytv-update-endpoint.yml` foi aprovado no GitHub Actions run `39963760834`; verificou assinatura do APK atual e readback remoto byte a byte.

## Desenvolvimento

Fonte Android TV Remote Apache-2.0; scripts e overrides de configuração no repositório. Branch OTA do telefone: `feat/tcl-v22-updates-back-bluetooth`. Branch de desenvolvimento: `feat/anytv-p0-device-proof-verifier-20261020`.
