# AnyTV — execução e continuidade para a branch OTA

Esta branch é o canal de distribuição, NÃO branch de desenvolvimento. Não fazer merge em main ou alterar o canal TV automaticamente.

Antes de agir, ler obrigatoriamente:
- docs/handoffs/2026-10-23-anytv-full-transition.md (história completa e decisões)
- docs/handoffs/CURRENT_HANDOFF.md (estado vigente)
- docs/decisions/ADR-0003-ANDROID-CERT2026-GITHUB-ONLY.md (assinatura e OTA únicas)
- README.md, docs/ASSINATURA_E_ATUALIZACAO.md, signing-recovery/README.md
- Branch dev feat/anytv-p0-device-proof-verifier-20261020: AGENTS.md, docs/VISION.md, docs/STATUS.md, docs/roadmap/PROJECT_PROGRESS.json, docs/operations/PROJECT_CONTINUITY_PROTOCOL.md.

Identidades: telefone com.tclremote.livre/cert2026; TV companion e canal próprio não devem ser alterados pela OTA telefone. OTA telefone atual 2.9.12/code41. Candidata 2.9.13/code42 compilada/assinada mas NÃO publicada, TLS TCL não homologado.

Publicador ÚNICO: .github/workflows/publish-anytv-phone-github-only.yml; verificador de exclusividade/readback: .github/workflows/verify-anytv-update-endpoint.yml. Proibidos fluxos alternativos, exportação de segredo ou chaves em texto claro. O segredo privado ANYTV_RECOVERY_SECRET_2026 está inacessível ao runner GitHub Actions (última falha 39963496459); não declarar isso resolvido sem CI verde. Verificador do canal 39963760834 SUCCESS. Preservar progressos 7/11 sem homologação humana em F0-F8 até prova específica.
