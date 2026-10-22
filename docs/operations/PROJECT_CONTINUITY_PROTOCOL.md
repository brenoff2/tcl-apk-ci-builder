# ANYTV — Protocolo de continuidade auditável

Estado: ADAPT da metodologia da Fábrica de Projetos. Não significa teste independente aprovado ou OTA publicada. Referência metodológica: `brenoff2/chatgpt-project-factory`, branch `feat/g1-core-vertical-slice`, especialmente `AGENTS.md`, `docs/operations/PROGRESS_REPORTING.md` e `docs/architecture/2026-10-16-factory-master-design.md`.

## Recuperação obrigatória no início de cada chat
1. Verificar HEAD de `brenoff2/tcl-apk-ci-builder` nas branches desenvolvimento `feat/anytv-p0-device-proof-verifier-20261020` (PR #3) e OTA `feat/tcl-v22-updates-back-bluetooth`. `main` não substitui essas referências.
2. Ler `AGENTS.md`, `docs/VISION.md`, `docs/STATUS.md`, `docs/handoffs/CURRENT_HANDOFF.md`, `docs/roadmap/MASTER_ROADMAP.md`, `docs/roadmap/PROJECT_PROGRESS.json`, `docs/decisions/ADR-0001.md`.
3. Preservar contratos: `docs/CONTRATO_FUNCIONAL_TCL.md`, `docs/ANYTV_STREAMING_MATRIX.md`, `docs/ANYTV_STREAMING_SCREENS.md`, `docs/ANYTV_DISCOVERY_HOME_V3.md`, `docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md`, código `overrides/common/TclUpdateManager.java`.
4. Conferir últimos commits e runs; readback no GitHub do binário OTA; estado Worker/D1 quando pertinente. Reconciliar divergências entre status antigo e execução real.
5. Retomar a próxima ação executável, não repetir perguntas nem refazer assinatura/build já existentes.

## Identidade e arquitetura preservadas
AnyTV Android `com.tclremote.livre`, AnyTV Connect TCL/Android TV, LAN, Bluetooth, pareamento, inventário real de apps, favoritos, streaming Discovery V3 e Worker/D1. Discovery V3 é descoberta global, hero, carrosséis, busca, filtros de plataformas instaladas, notas e trailers. Não presumir assinatura de streaming nem garantir deep link de título sem suporte real. Preservar certificados cert2026, canal OTA Android e TV e ativação. Sem merge em `main` ou alteração colateral no canal TV.

## Comportamento da Fábrica adaptado ao AnyTV
- Diretor ChatGPT único, especialidades sequenciais e verificáveis, sem subagentes fictícios.
- Pesquisa GitHub-first nas mudanças técnicas; decisão `ADOPT/ADAPT/BUILD/BLOCK`, seleção de skills por capacidade, compatibilidade, segurança e evidências. Skills aplicáveis: `github-first-engineering`, `karpathy-coding`, `karpathy-global`; não instalar dependências arbitrárias.
- Diff mínimo, contratos e decisões versionados, operações com readback, retries idempotentes e rollback.
- Estados diferenciados: desenvolvido, compilado, assinado, publicado, instalado, homologado. CI verde nunca equivale a prova física.
- Fases F0–F8, marcos M01–M11 e progresso são exclusivos do AnyTV. Não importar porcentagens da Fábrica. Última evidência = 7/11 marcos técnicos (63%), 0 gates humanos homologados, conforme `PROJECT_PROGRESS.json`.
- A política de CI externa da Fábrica é específica daquele repositório. O AnyTV utiliza GitHub Actions para compilação/publicação; nenhuma migração automática de CI.
- Atualizar STATUS, handoff, roadmap e ADR apenas diante de mudanças reais; ao finalizar, incluir acompanhamento F0–F8 e bloqueios com alternativas.

## Pendência operacional PRIORITÁRIA — Android 2.9.9 Discovery V3
Build GitHub Actions `39699948020` aprovado, versão 2.9.9 (`versionCode=38`), APK assinado cert2026, 28.347.236 bytes e SHA-256 `56142e5f3e33d54f1be41c5f702b0d8e1bf00bff206e3c011438a5c2648a2a93`. Cópia assinada em Drive: `AnyTV - Releases/android/AnyTV_2.9.9_Discovery_V3_CERT2026.apk`, ID `1NadF93dOMSJHkjJlvQWyUPQ2u8hwjqHN`. O Drive NÃO é a OTA.
O updater lê `updates/android/cert2026/current.apk` na branch OTA `feat/tcl-v22-updates-back-bluetooth`. Na última leitura, esse arquivo ainda era Android 2.9.8, 28.175.005 bytes, Git blob `51066c6b97a4828d9e17a04dd5b5779dee18286a`. Logo **2.9.9 não foi publicada pelo updater**.
Workflow comprovado anterior: `.github/workflows/publish-anytv-cert2026-update-298.yml`. Workflow candidato 2.9.9: `.github/workflows/publish-anytv-cert2026-update-299.yml`. Não publicar APK unsigned. Houve estágio PARCIAL de metadados em `.github/anytv-299-patch/part-00.txt`, commit `df6a315a2873cdfed924534c7217bfe52329a324` na branch OTA; não é reconstrução completa.
Bloqueio observado: ferramenta GitHub não transferiu binário assinado e ambiente container sem acesso Git. Próxima ação: localizar meio AUTORIZADO de disponibilizar o APK assinado ao runner, publicar o binário exato como `current.apk`, conferir SHA remoto e testar o botão Buscar atualizações no Android. Não contornar restrições de credenciais ou dados privados.

## Prova de transferência entre conversas (ainda NÃO EXECUTADA)
Canário independente deve recuperar, sem histórico do chat: identidade, duas branches e seus HEADs, arquitetura, decisões, 7/11 F0–F8, localização do updater, versão/hash atual e esperado, bloqueio e ação seguinte. Só marcar `PASS` após registrar evidências da conversa nova em `docs/handoffs/CONTINUITY_CHECK.json`. Não criar atestado fictício.
