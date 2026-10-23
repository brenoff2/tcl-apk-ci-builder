# ANYTV — regras persistentes de execução

**ACESSO GITHUB/ASSINATURA — REGRA OPERACIONAL MAIS RECENTE (23/10/2026):** antes de afirmar falta de acesso, consultar MCP GitHub direto E MCP Business OS Hub v2 → provider GitHub; nenhum diagnóstico deve se basear no terminal Windows. A decisão vigente e o diagnóstico preciso de Actions Secrets estão SOMENTE em `docs/ASSINATURA_E_ATUALIZACAO.md`. A falha 39963496459 é histórica; secret no runner continua NÃO VERIFICADO. Não executar release sem aprovação.

1. GitHub `brenoff2/tcl-apk-ci-builder` é o cânone. Código, OTA e logs comprovados precedem narrativas de chats. Ler `docs/handoffs/CURRENT_HANDOFF.md`, HEAD e `docs/roadmap/PROJECT_PROGRESS.json` antes de agir.
2. Preservar visão, app Android, AnyTV Connect, UX aprovada, descoberta de apps reais, Bluetooth, streaming e canais de assinatura `cert2026`. Não recomeçar o projeto.
3. Papéis de estratégia, arquitetura, engenharia, QA e segurança são responsabilidades coordenadas; não simular auditorias independentes.
4. Para engenharia relevante: pesquisar upstream, decidir ADOPT/ADAPT/BUILD/BLOCK, aplicar diff mínimo, testar, observar e fazer readback. Evitar CI sem resultado de produto.
5. `feat/anytv-p0-device-proof-verifier-20261020` é branch de trabalho (PR #3); `feat/tcl-v22-updates-back-bluetooth` é canal OTA. Sem merge em `main` nem alteração destrutiva sem aprovação aplicável.
6. Nunca colocar chaves, senhas, códigos individuais de ativação, segredos ou dados de clientes no GitHub. Não trocar certificados existentes. Mudanças em Cloudflare/D1 devem preservar bindings e dados e ter rollback.
7. Diferenciar desenvolvido, testado, publicado, instalado, homologado e operacional. CI de software não demonstra funcionamento de TV física.
8. Só atualizar percentual com evidência. Não criar arquivos vazios. `factory.yaml`, `factory.lock.yaml` e `ci-policy.yaml` ficam dispensados até existir necessidade operacional comprovada.
9. Terminar respostas de execução com **Acompanhamento — ANYTV**: progresso marcos; fases F0–F8 com contagens/percentuais e gates; última evidência, trabalho atual, próximo aceite, bloqueios e alternativa. Estado não aferido não deve ser inventado.

## Continuidade padronizada e ponto atual (23/10/2026)

Antes de toda nova conversa, ler **docs/handoffs/CURRENT_HANDOFF.md** e, prioritariamente, **docs/handoffs/2026-10-23-anytv-full-transition.md**. O antigo plano de 2.9.9 como próxima OTA é HISTÓRICO. A OTA do telefone em produção é **2.9.12 (code41)**; a 2.9.13 (code42) está compilada e assinada, mas ainda não publicada nem aprovada na TCL.

**Decisão ADR-0003**: só o workflow `.github/workflows/publish-anytv-phone-github-only.yml` pode publicar `updates/android/cert2026/current.apk`; verifica artefato CI, certificado cert2026, versionCode e readback. Não criar métodos concorrentes, URLs temporárias, arquivos de chave privados no GitHub ou releases sem OTA validada. Os publicadores antigos do telefone foram removidos das branches ativas, e os da TV preservados. CI de exclusividade e leitura remota `39963760834` PASS.

**Bloqueio verificado**: no GitHub Actions `39963496459`, o segredo privado `ANYTV_RECOVERY_SECRET_2026` não estava acessível ao runner. O backup criptografado no GitHub existe, e assinatura local com cert2026 já funcionou; não significa que chave privada esteja perdida. Configuração do segredo só por ação administrativa autorizada, sem copiar valor em conversa/commits. Usuário determinou UMA única rota permanente, sem Drive no updater ou publicador.

**Regressão física não resolvida**: AnyTV 2.9.8 controlava a TCL; 2.9.10–2.9.12 mostraram erros TLS/SSL; 2.9.11 alterou marcadores HOST/PIN. 2.9.13 restringe mTLS da TV à RSA local em vez de EC Cloudflare, mas é HIPÓTESE, não reparo comprovado; exigir observação do aparelho. Preservar dados e certificados.

## Fontes operacionais obrigatórias
docs/handoffs/2026-10-23-anytv-full-transition.md; docs/handoffs/CURRENT_HANDOFF.md; docs/decisions/ADR-0003-ANDROID-CERT2026-GITHUB-ONLY.md; docs/ASSINATURA_E_ATUALIZACAO.md; docs/roadmap/PROJECT_PROGRESS.json; docs/operations/PROJECT_CONTINUITY_PROTOCOL.md. A investigação e inventário de releases históricos permanecem em docs/operations/ apenas para auditoria, não como rota de entrega.


## Revisão de execução — 23/10/2026

O workflow único de assinatura foi corrigido na branch OTA: `.github/workflows/publish-anytv-phone-github-only.yml` aceita pedidos **apenas** por push de `.github/anytv-phone-release-request.json` com ID de CI aprovado e `versionCode` explícito. Alterar workflow/documentação não dispara publicação. Credencial `ANYTV_RECOVERY_SECRET_2026` injetada apenas no passo de assinatura, nunca no job inteiro. Commits do workflow OTA `bc73a647` e `ae2b438e`; verificação automatizada `ef2eddc4`. Não foi criado pedido de release, o segredo ainda não foi comprovado no runner, a OTA continua 2.9.12/code41 e a 2.9.13/code42 não está publicada nem homologada na TCL. Consultar a execução mais recente do verificador e não elevar gates sem evidência.
