# ANYTV — regras persistentes de execução

1. GitHub `brenoff2/tcl-apk-ci-builder` é o cânone. Código, OTA e logs comprovados precedem narrativas de chats. Ler `docs/handoffs/CURRENT_HANDOFF.md`, HEAD e `docs/roadmap/PROJECT_PROGRESS.json` antes de agir.
2. Preservar visão, app Android, AnyTV Connect, UX aprovada, descoberta de apps reais, Bluetooth, streaming e canais de assinatura `cert2026`. Não recomeçar o projeto.
3. Papéis de estratégia, arquitetura, engenharia, QA e segurança são responsabilidades coordenadas; não simular auditorias independentes.
4. Para engenharia relevante: pesquisar upstream, decidir ADOPT/ADAPT/BUILD/BLOCK, aplicar diff mínimo, testar, observar e fazer readback. Evitar CI sem resultado de produto.
5. `feat/anytv-p0-device-proof-verifier-20261020` é branch de trabalho (PR #3); `feat/tcl-v22-updates-back-bluetooth` é canal OTA. Sem merge em `main` nem alteração destrutiva sem aprovação aplicável.
6. Nunca colocar chaves, senhas, códigos individuais de ativação, segredos ou dados de clientes no GitHub. Não trocar certificados existentes. Mudanças em Cloudflare/D1 devem preservar bindings e dados e ter rollback.
7. Diferenciar desenvolvido, testado, publicado, instalado, homologado e operacional. CI de software não demonstra funcionamento de TV física.
8. Só atualizar percentual com evidência. Não criar arquivos vazios. `factory.yaml`, `factory.lock.yaml` e `ci-policy.yaml` ficam dispensados até existir necessidade operacional comprovada.
9. Terminar respostas de execução com **Acompanhamento — ANYTV**: progresso marcos; fases F0–F8 com contagens/percentuais e gates; última evidência, trabalho atual, próximo aceite, bloqueios e alternativa. Estado não aferido não deve ser inventado.
