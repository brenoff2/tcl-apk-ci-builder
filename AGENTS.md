# AnyTV — execução e continuidade para a branch OTA

**ACESSO GITHUB/ASSINATURA — REGRA OPERACIONAL MAIS RECENTE (23/10/2026):** antes de afirmar falta de acesso, consultar MCP GitHub direto E MCP Business OS Hub v2 → provider GitHub; nenhum diagnóstico deve se basear no terminal Windows. A decisão vigente e o diagnóstico preciso de Actions Secrets estão SOMENTE em `docs/ASSINATURA_E_ATUALIZACAO.md`. A falha 39963496459 é histórica; secret no runner continua NÃO VERIFICADO. Não executar release sem aprovação.

Esta branch é o canal de distribuição, NÃO branch de desenvolvimento. Não fazer merge em main ou alterar o canal TV automaticamente.

Antes de agir, ler obrigatoriamente:
- docs/handoffs/2026-10-23-anytv-full-transition.md (história completa e decisões)
- docs/handoffs/CURRENT_HANDOFF.md (estado vigente)
- docs/decisions/ADR-0003-ANDROID-CERT2026-GITHUB-ONLY.md (assinatura e OTA únicas)
- README.md, docs/ASSINATURA_E_ATUALIZACAO.md, signing-recovery/README.md
- Branch dev feat/anytv-p0-device-proof-verifier-20261020: AGENTS.md, docs/VISION.md, docs/STATUS.md, docs/roadmap/PROJECT_PROGRESS.json, docs/operations/PROJECT_CONTINUITY_PROTOCOL.md.

Identidades: telefone com.tclremote.livre/cert2026; TV companion e canal próprio não devem ser alterados pela OTA telefone. OTA telefone atual 2.9.12/code41. Candidata 2.9.13/code42 compilada/assinada mas NÃO publicada, TLS TCL não homologado.

Publicador ÚNICO: .github/workflows/publish-anytv-phone-github-only.yml; verificador de exclusividade/readback: .github/workflows/verify-anytv-update-endpoint.yml. Proibidos fluxos alternativos, exportação de segredo ou chaves em texto claro. Na execução histórica 39963496459 o segredo ANYTV_RECOVERY_SECRET_2026 não chegou ao runner; o estado ATUAL não foi auditado, e não se deve declarar resolução sem prova de descriptografia no Actions. Verificador do canal 39963760834 SUCCESS. Preservar progressos 7/11 sem homologação humana em F0-F8 até prova específica.


## Revisão de execução — 23/10/2026

O workflow único de assinatura foi corrigido: `.github/workflows/publish-anytv-phone-github-only.yml` recebe pedidos **apenas** via push de `.github/anytv-phone-release-request.json` na branch OTA, com ID de build CI e `versionCode` explícitos. Commit de workflow/documentação não publica. A credencial `ANYTV_RECOVERY_SECRET_2026` agora existe no ambiente **somente** do passo de assinatura, e o passo recusa valor ausente ou inválido. Código atualizado nos commits `bc73a647` e `ae2b438e`; verificador ajustado em `cf0e0f56` e `ef2eddc4`. Nenhum pedido de release JSON foi criado; nenhuma nova OTA autorizada/publicada. O segredo continua indisponível ao runner nas últimas execuções de publicação observadas. CI da revisão deve ser checada antes de considerar concluído.
