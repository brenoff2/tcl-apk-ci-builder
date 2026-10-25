# AnyTV — QA: tentativas de recuperação e primeiro lote de gênero sem travamento

**Data:** 25/10/2026. **Branch DEV/PR #3:** `feat/anytv-p0-device-proof-verifier-20261020`, aberto em draft. **Código:** [182a97de](https://github.com/brenoff2/tcl-apk-ci-builder/commit/182a97de87cb8ea64695296aeeda4ca76f81d27f). **Arquivos funcionais alterados:** `overrides/TclStreamingScreens.kt` e `scripts/tests/test_streaming_screens_contract.py`. Nenhuma alteração de Worker, D1, companion TV, assinatura, publicação ou OTA.

## Causas verificadas na fonte anterior

1. Quando a pessoa havia marcado filmes assistidos em versões antigas, mas os metadados reais ainda não estivessem localmente disponíveis, o mapa de prateleiras ficava vazio. A tela exibia **"Nenhum filme marcado como assistido"**, apesar de `anytv_watched_movies` ainda preservar IDs reais e o aviso com IDs aparecer abaixo.
2. A recuperação de Assistidos `LaunchedEffect(watchedFilms, favorites, homeLibrary, librarySection)` consultava no máximo 12 IDs pendentes por entrada na aba. Depois de completar um lote, ou de encontrar uma indisponibilidade temporária, não havia ação para continuar/repetir sem mudar de aba. Isso podia deixar a restauração parcialmente bloqueada apesar de o Worker de DEV permitir a consulta.
3. O spinner de gênero usava como condição `selectedGenreKey !in genreShelves`. Uma falha na primeira chamada ao TMDB não inseria a chave em `genreShelves` e, para gêneros comuns, nem registrava erro. Assim, **o spinner nunca parava** enquanto a aba estivesse naquele gênero. A primeira página Cinema sofria do mesmo estado sem chave, apesar de já registrar erro.

## Alterações de código em DEV

- **Histórico confiável:** quando houver um ID positivo de filme marcado, o estado vazio informa que o histórico existe e ainda faltam cartazes, sem fingir que não há filmes assistidos. Se nunca houver ID positivo, mantém a mensagem normal.
- **Mais 12 sob demanda:** `watchedMoviesRetry` é uma chave explícita do `LaunchedEffect`; o botão discreto **Recuperar mais 12** só aparece para IDs ainda pendentes, sem operação em curso e com rota online suportada. O limite de até 12 IDs por tentativa, o tratamento especial de HTTP 400 do Worker antigo, a exclusão por sessão dos IDs HTTP 404 e a persistência de sucessos parciais em `finally` permanecem intactos.
- **Carregamento de gênero finito:** a primeira consulta de filmes, séries e Cinema marca `genreLoading[key]` como true e sempre termina em false no `finally`. O spinner é vinculado ao estado **real de carregamento**, e não à inexistência permanente da chave no cache. Erros são registrados, cancelamentos de coroutine são relançados, sucessos limpam erros e preservam paginação.
- **Tentar novamente:** `genreFirstPageRetry` reinicia a consulta da primeira página sob comando explícito, somente quando aquela página falhou e não está carregando. Páginas já válidas e paginação **Mostrar mais 24** mantêm suas listas e estado.
- **Contrato visual imutável:** poster inteiro 2:3, três colunas, ícones originais, Cinema BR, fundo azul do selo, título/ano com shadow, filtros e telas aprovadas não foram redesenhados.

## GitHub-first

**Problema:** estado Compose sem fim e recuperação de histórico sem retry no mesmo contexto. **Upstream consultado:** código oficial `androidx/androidx`, `compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Effects.kt`, cuja implementação associa `LaunchedEffect` a chaves usando `remember(*keys)`, reexecutando o efeito após mudança da chave. **Decisão: ADAPT**, reutilizando os efeitos e estados já existentes no aplicativo; sem framework, permissão, token ou rota nova.

## Evidência de CI

- [CI FINAL Android unsigned #40169560233](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40169560233): **COMPLETED/SUCCESS: 71 testes Streaming PASS, 2 credenciais PASS, 11 visuais PASS; Gradle BUILD SUCCESSFUL in 4m13s; identidade com.tclremote.livre.v2/versionCode54 verificada**. Artefato: **#12589154176 (anytv-v2-2925-code54-unsigned-audit, 23.289.396 bytes)**.
- [Prova isolada P0 final #40169560291](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40169560291): **COMPLETED/SUCCESS: 55 testes PASS**.
- Worker offline: **não houve mudança de código Worker** nesta etapa; último contrato Worker #40136181610 SUCCESS no commit 53c6b8d1; continua sem deploy em produção.

**Errata auditada:** execução anterior [#40169458180](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40169458180) terminou **FAILED** por contrato textual obsoleto; a correção de teste \`ffb2c8d8\` antecede o CI **#40169560233 SUCCESS**. [Checkpoint integral](../handoffs/2026-10-25-ANYTV-TRANSICAO-INTEGRAL-POS-7E4B.md).

## Limitações e testes ainda necessários

- O Worker em produção continua na versão **58fe4b33-6ee2-4841-8587-c28acaaad435**, sem `movie/{id}`; por isso a recuperação remota de IDs antigos no APK público NÃO está habilitada. Após resposta HTTP 400, o botão de recuperação online fica desativado para a sessão.
- Testar fisicamente no Android e na TCL: lista sem capa mas com ID real; botão de próximo lote após 12 metadados; sequência de IDs 404 seguida de válidos; Worker antigo com HTTP 400; queda de rede e retry; primeiro carregamento de gênero com erro e retry; Cinema BR; scroll, posters 2:3 e selos sem regressão; reinício do app.
- **OTA oficial assinada permanece AnyTV 2.9.25/code54**, branch `feat/tcl-v22-updates-back-bluetooth`, commit `dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112`, SHA256 `c53596d8f8210bf0a12769481796481a851d00da4c52f544120b85367dd17fe0`. Candidatos no PR #3 são **unsigned/code54**, inadequados à OTA. Não publicar APK/Worker, merge ou promover gates sem aprovação e QA físico.
- **Progresso formal 7/11 = 63%**, M08/M09 IN_QA, M10/M11 BACKLOG, F0–F8 NAO_HOMOLOGADO.

## Readback final GitHub em 25/10/2026

O commit Android funcional [182a97de](https://github.com/brenoff2/tcl-apk-ci-builder/commit/182a97de87cb8ea64695296aeeda4ca76f81d27f) preservou a fonte Kotlin na recompilação; o commit [ffb2c8d8](https://github.com/brenoff2/tcl-apk-ci-builder/commit/ffb2c8d85d7b2271129891936e15b85aed4c78c5) mudou **apenas** a asserção textual antiga de `genreMore[key] = it.more` para `genreMore[key] = result.more`. O CI intermediário [#40169458180](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40169458180) foi **FAILURE somente nessa expectativa de teste, antes da compilação Android**, e não é apresentado como sucesso. O CI novo [#40169560233](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40169560233) terminou **COMPLETED/SUCCESS**: **71 testes Streaming**, **2 credenciais**, **11 visuais**, Gradle **BUILD SUCCESSFUL in 4m13s**, verificação aapt de `com.tclremote.livre.v2`/code54 e artefato de auditoria **unsigned #12589154176** (23.289.396 bytes). A prova isolada [#40169560291](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40169560291) terminou **SUCCESS, 55 testes**, sem deploy. Nenhum resultado automatizado homologou aparelho físico.

**QA físico da OTA oficial assinada 2.9.25:** [plano atualizado](ANYTV_V2_2.9.25_DEVICE_HOMOLOGATION_PLAN_20261025.md), todos os passos PENDENTES. A versão anterior 2.9.22 permanece histórica.