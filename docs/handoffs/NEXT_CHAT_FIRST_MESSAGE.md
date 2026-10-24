# ANYTV — PRIMEIRA MENSAGEM PARA RETOMAR NO PRÓXIMO CHAT

**Checkpoint canônico: 23/10/2026. Projeto em andamento; NÃO RECOMEÇAR.** A versão publicada é **AnyTV V2 2.9.22 (versionCode 51)**. Os registros sobre a 2.9.12 e bloqueio do antigo `ANYTV_RECOVERY_SECRET_2026` no histórico são **obsoletos**, não pendências atuais.

> **CONTINUIDADE OBRIGATÓRIA.** Leia o GitHub canônico `brenoff2/tcl-apk-ci-builder`, desenvolvimento `feat/anytv-p0-device-proof-verifier-20261020`, distribuição OTA independente `feat/tcl-v22-updates-back-bluetooth`. Verifique primeiro `docs/roadmap/PROJECT_PROGRESS.json`, `AGENTS.md`, `docs/STATUS.md` e `docs/handoffs/CURRENT_HANDOFF.md`. Não refaça builds aprovados e não publique nova versão apenas para organizar documentação.

## Objetivo geral

Manter o aplicativo AnyTV para controlar televisão TCL por rede e Bluetooth, com pareamento, navegação, teclado, favoritos, lançador de streaming e catálogo de filmes/séries; preservar o app legado `com.tclremote.livre`, o companion TCL e os fluxos já validados. O aplicativo V2 separado `com.tclremote.livre.v2` deve receber OTA assinada sem alterar o canal `cert2026`.

## Situação técnica comprovada

- **OTA V2 publicada:** `2.9.22-AnyTV`, `versionCode 51`, `com.tclremote.livre.v2`. [GitHub Actions #40045953034](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40045953034): **SUCCESS, 32 etapas**, Kotlin/Android, testes IME, busca TMDB real, signer recuperado, APK assinado, publicado e conferido por bytes via HTTPS.
- **APK:** `updates/android/v2/current.apk` na branch OTA; Git blob `39084853e1956f4aaedb3d8443ade9304e497581`, tamanho **28.412.772 bytes**, SHA256 `44cab69ad0eb6c0110ff4180e6388f042e667d8b86dcd319e39d777973d85d94`; commit da publicação `0b57a842cf7b662f6a8a85a84429d01f4901fdd4`.
- **Assinatura V2:** certificado SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. **Canal legado preservado**: `updates/android/cert2026/current.apk`, blob `44c766b433ddb370f4589b87843f1edfd9cd7b7a`. Não confundir certificado V2 com cert2026.
- **Catálogo online:** Cloudflare Worker `anytv-link-resolver-pilot` versão `a6747819-e4da-422c-9766-a40151b87cd3` no tráfego 100%, deployment `a196075c-8299-4c8d-9054-6e52bb486505`; permite quatro modos validados de ordenação TMDB em descobertas de filmes/séries e conserva rate-limit/cache/segredos. **Não há limite mensal de 200 no TMDB**; a quota de 200/mês permanece isolada à resolução dos links (JustWatch), com contador próprio. [Live smoke #40045771887](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40045771887): SUCCESS.
- **UI V2 2.9.22:** busca 44dp com texto e cursor centralizados; página filtrada de gênero com lista detalhada (sinopse 160 caracteres, ano, nota, ícones BR, favorito, assistido/acompanhamento, trailer sob demanda), opção grade de três cartazes por linha, ordenação por popularidade/nota/mais recentes/título e `Mostrar mais 20` exclusivamente no gênero; 10 destaques e dots sobre banner; banner pequeno da home com título centrado, `Ver ›` fixo e cinza claro com logo confirmado. Seções horizontais da home mantêm duas fileiras com espaço reservado para três linhas de título.
- **Progresso formal:** `7/11 = 63%` dos marcos técnicos. **Todos os gates humanos F0–F8 continuam `NAO_HOMOLOGADO`**. A publicação técnica **não** comprova o layout/interações no aparelho.

## Próxima ação legítima (não é novo release automático)

Executar checklist específico [homologação real V2 2.9.22](../qa/ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md) no Android/TCL. Marcar PASS/FAIL por evidência, priorizando texto da busca, alternância lista/grade, ordenação global entre páginas, `Mostrar mais 20`, favorito/assistido, trailers, dots e alinhamento do banner. **Corrigir somente falhas reproduzidas** e publicar uma única versão nova se houver alterações no app. Não afirmar homologação física sem teste real nem solicitar repetição de decisões já registradas.

## Fontes para continuidade

- [Progresso canônico](../roadmap/PROJECT_PROGRESS.json)
- [Handoff atual](./CURRENT_HANDOFF.md)
- [Documentação operacional](../STATUS.md)
- [Registro cronológico integral](./2026-10-23-anytv-full-transition.md) — histórico, não substitui estado vigente
- [QA real no aparelho](../qa/ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md)
- [Run de publicação V2 2.9.22](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40045953034)
