**ENTREGA NOVA IDENTIDADE — 23/10/2026 (VERIFICADA):** AnyTV V2 2.9.13/code42 distribuido como APK do pacote INDEPENDENTE `com.tclremote.livre.v2`. APK assinado GitHub Actions run `39998890413` SUCCESS, SHA256 `c407701f449645d9ee76bdc7a9600f7bc2dfbcc01b047010e95a567006058cb9`, certificado novo SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. Canal GitHub exclusivo V2: `updates/android/v2/current.apk` na branch OTA, blob `24aaa6c53d29810fca8303fdaf0b45fc6764a91d`, commit `4187a24b9129bee64bb535a2e2e7865640ba80e5`. O workflow validou APK, pacote, versão, cert e READBACK BYTE-A-BYTE `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`. A identidade V2 foi criada e seu backup AES-GCM persistido em `signing-recovery/v2/phone-signer.aesgcm`, manifesto público e cert `identity.json`, `phone-cert.pem`, dev HEAD no bootstrap `db6fa8a...`. Secret operacional privado `ANYTV_SIGNER_V2_RECOVERY_HEX` no GitHub Actions. Prova de recuperação `39998409528` SUCCESS, `ANYTV_V2_SIGNER_GITHUB_ENCRYPTED_READBACK_AND_RECOVERY_PASS`. **NÃO substitui a instalação antiga** sem chave/lineage; instalar em paralelo e reconfigurar pareamento da TCL. OTA original `updates/android/cert2026/current.apk` mantém 2.9.12/code41, blob `44c766b433ddb370f4589b87843f1edfd9cd7b7a`. Teste físico TCL V2 NÃO HOMOLOGADO. Segredo secundário `ANYTV_SIGNER_V2_ESCROW_HEX` consta no repositório, mas a segunda cópia do signer cifrada por ele ainda NÃO foi implementada (ferramenta bloqueou), logo NÃO tratar como redundância pronta. Consultar `docs/decisions/ADR-0004-NEW-ANDROID-SIGNER-V2-GITHUB-CUSTODY.md` na branch dev. 

# HANDOFF ATUAL — ANYTV — 23/10/2026

**ACESSO GITHUB/ASSINATURA — REGRA OPERACIONAL MAIS RECENTE (23/10/2026):** antes de afirmar falta de acesso, consultar MCP GitHub direto E MCP Business OS Hub v2 → provider GitHub; nenhum diagnóstico deve se basear no terminal Windows. A decisão vigente e o diagnóstico preciso de Actions Secrets estão SOMENTE em `docs/ASSINATURA_E_ATUALIZACAO.md`. A falha 39963496459 é histórica; secret no runner continua NÃO VERIFICADO. Não executar release sem aprovação.

**MENSAGEM PRONTA PARA COPIAR NO PRÓXIMO CHAT:** [docs/handoffs/NEXT_CHAT_FIRST_MESSAGE.md](./NEXT_CHAT_FIRST_MESSAGE.md). Abrir com ela e depois ler integralmente o documento canônico abaixo; não depender de memória de conversa.\n\n**LER PRIMEIRO**: docs/handoffs/2026-10-23-anytv-full-transition.md (histórico integral, versões, decisões, assinatura, Cloudflare e próximos passos). Esta página é somente índice atual, substitui handoff de 21–22/10 desatualizado. Documentos precedentes permanecem no histórico Git.

## Objetivo
Preservar AnyTV Android com.tclremote.livre, AnyTV Connect da TCL, controle LAN/Bluetooth/pareamento, inventário de apps, streaming e Discovery V3, com um **único** atualizador GitHub-only e certificado cert2026 original, sem Drive no processo OTA. Não reiniciar o projeto nem criar rotas alternativas.

## Estado comprovado
- Branch desenvolvimento feat/anytv-p0-device-proof-verifier-20261020 (PR #3 draft); branch distribuição feat/tcl-v22-updates-back-bluetooth; main não deve receber merge automático.
- **OTA Android publicada: 2.9.12 (code41)** em updates/android/cert2026/current.apk; Git blob 44c766b433ddb370f4589b87843f1edfd9cd7b7a; 28.347.236 bytes, SHA-256 28c2881899bc9c6a79fcfd908d66528750534c89c7f334b8cc57096f80cf63f3. Verificador CI 39963760834 SUCCESS (assinatura cert2026, readback e única rota de escrita).
- **2.9.13 (code42)** candidata compilada (CI 39891891049 SUCCESS), APK assinado privado SHA256 fbbc1398465f27b2aadccef1d48be6e08e51af0b729aab694189ca8c06b3c1f5, ainda NÃO publicada, conexão TCL NÃO HOMOLOGADA. Patch RSA TLS: scripts/apply_anytv_tv_tls_alias_isolation.py.
- Pareamento TCL: versão **2.9.8 funcionava** segundo usuário. Erros SSL em 2.9.10, 2.9.11 e 2.9.12. Patch 2.9.11 apagou marcador HOST/PIN, rollback 2.9.12 não recupera marcador. Hipótese RSA TV x EC Cloudflare ainda sem confirmação no aparelho.
- **Assinatura não perdida**: backup cifrado signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin; certificado Android SHA256 2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f. A execução HISTÓRICA 39963496459 falhou no gate do segredo GitHub Actions ANYTV_RECOVERY_SECRET_2026; a disponibilidade ATUAL ainda não foi conferida pelo runner. Localmente a assinatura já funcionou. Não expor nem recriar chave.
- Publicador EXCLUSIVO: .github/workflows/publish-anytv-phone-github-only.yml (branch OTA); verificações .github/workflows/verify-anytv-update-endpoint.yml; workflows Android antigos removidos nas duas branches, TV preservada.
- Progresso formal P0 7/11 = 63% (marcos históricos), nove gates humanos NAO_HOMOLOGADO. Não inflar percentuais com builds recentes.

## Próxima ação executável
Conferir os HEADs atuais e logs. Regularizar por meio autorizado acesso do Actions ao segredo EXISTENTE ANYTV_RECOVERY_SECRET_2026 (não pedir valor no chat nem criar segundo caminho); reexecutar publicador único com CI candidata 39891891049; exigir versão superior, certificado e readback; depois usar Buscar atualizações no app e verificar fisicamente TLS/controle TCL. Sem declaração falsa de conclusão.

## Fontes autoritativas
docs/handoffs/2026-10-23-anytv-full-transition.md
docs/decisions/ADR-0003-ANDROID-CERT2026-GITHUB-ONLY.md
docs/ASSINATURA_E_ATUALIZACAO.md
docs/roadmap/PROJECT_PROGRESS.json
docs/STATUS.md
AGENTS.md
https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/39963760834


## Revisão de execução — 23/10/2026

O workflow único de assinatura foi corrigido: `.github/workflows/publish-anytv-phone-github-only.yml` recebe pedidos **apenas** via push de `.github/anytv-phone-release-request.json` na branch OTA, com ID de build CI e `versionCode` explícitos. Commit de workflow/documentação não publica. A credencial `ANYTV_RECOVERY_SECRET_2026` agora existe no ambiente **somente** do passo de assinatura, e o passo recusa valor ausente ou inválido. Código atualizado nos commits `bc73a647` e `ae2b438e`; verificador ajustado em `cf0e0f56` e `ef2eddc4`. Nenhum pedido de release JSON foi criado; nenhuma nova OTA autorizada/publicada. O segredo continua indisponível ao runner nas últimas execuções de publicação observadas. CI da revisão: run `39973828725` SUCCESS (exclusividade do publicador, certificado cert2026 da OTA atual e readback remoto). A assinatura de um NOVO APK continua NÃO TESTADA pelo Actions enquanto o secret não estiver disponível; nenhum gate físico foi aprovado.


## Tentativa de entrega da 2.9.13 — 23/10/2026

- Pedido de release criado na branch OTA: `.github/anytv-phone-release-request.json`, commit `1530c887aa394168f004ce942d7996d019c71f76`, apontando para CI `39891891049` e `expected_version_code: 42`.
- Execução REAL do único publicador: GitHub Actions `39988081281` (FAILURE). Checkout, leitura do pedido, download do artefato de CI, Android SDK e dependência de criptografia passaram; a etapa **Recover original signer and sign without exporting secrets** falhou porque o runner recebeu `RECOVERY_HEX` VAZIO, com erro `Repository Actions secret ANYTV_RECOVERY_SECRET_2026 missing or invalid. OTA unchanged.`
- Etapas de assinatura, validação, publicação e readback de NOVO APK não se concluíram; o publicador não alterou `updates/android/cert2026/current.apk`. OTA permanece `2.9.12/code41`, candidatа `2.9.13/code42` NÃO entregue pela rota OTA. Não fornecer artifact unsigned como APK instalável.
- Estado decisivo atualizado: **BLOCKED_ACTIONS_SIGNER_SECRET_NOT_IN_RUNNER** comprovado ao vivo no run `39988081281`; não confundir com falta de acesso ao repositório (ambos MCPs GitHub seguem disponíveis). A fonte privada de recuperação existe e não deve ser publicada no Git/chat/inputs/argumentos de tools. Não criar publicador alternativo nem nova identidade.
