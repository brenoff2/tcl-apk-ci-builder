# AnyTV — auditoria de fonte após transição (25/10/2026)

## Proveniência e alcance
- Repositório: `brenoff2/tcl-apk-ci-builder`; branch DEV `feat/anytv-p0-device-proof-verifier-20261020`; baseline confirmada por compare do GitHub com `460f9b2fc4c344c22e86c219e5f67512440d2569` (identical, ahead=0, behind=0) **antes deste registro**.
- PR #3 mantido draft. Código inspecionado por fetch do GitHub: `overrides/TclStreamingScreens.kt` (blob `1ab5608fd70d343e8f6b963ebb358f73298d6e61`), `scripts/tests/test_streaming_screens_contract.py` (blob `387eb3aeb3583e95a40ea62f92c04d202a64796f`), `anytv-resolver/src/worker.mjs`, `anytv-resolver/src/worker.test.mjs` (blob `355a24e80cfc5d4da8c7af5a4b508ec1004ef901`).
- Verificação desta rodada = **leitura e revisão estática de trechos afetados**, não compilação, execução de testes, HTTP ao Worker nem homologação física. Todos esses níveis nesta rodada: **NAO_TESTADO**.

## Constatações verificadas na fonte
1. A restauração em `LaunchedEffect(watchedFilms, favorites, homeLibrary, librarySection, watchedMoviesRetry)` seleciona até 12 IDs positivos/distintos, tenta cache local, consulta `StreamingCatalog.lookupMovie` no backend e salva sucessos reais em `finally` por `preserveWatchedMovieTitles`. IDs ausentes não são excluídos do histórico.
2. HTTP 404 é distinguido por texto de exceção e excluído **apenas da tentativa desta sessão**; demais IDs do mesmo lote prosseguem. HTTP 400 marca `movieDetailRouteUnsupported` e encerra a tentativa; demais erros interrompem o lote sem excluir IDs. **Risco a validar:** contrato dependente da formatação da mensagem `(HTTP 404)`/`(HTTP 400)` da camada HTTP; acrescentar teste comportamental antes de alterar a lógica.
3. O botão `Recuperar mais 12` só aparece quando há IDs sem snapshot, há pendência que não seja 404 nesta sessão, não está restaurando e a rota não foi marcada como incompatível. O gatilho `watchedMoviesRetry++` reinicia a tentativa.
4. `preserveWatchedMovieTitles` aceita somente ID positivo, mídia movie e nome não vazio, grava JSON em `anytv_watched_movie_titles_v1` e `restoreTrackedTitle` considera favoritos, snapshot e cache; não há inferência de nome/poster para IDs sem resposta.
5. A primeira página de gênero e Cinema usa `genreLoading[key]=true` e `finally { genreLoading[key]=false }`; `genreErrors` dirige a ação `Tentar novamente` quando ainda não existe prateleira. Cancelamento de coroutine é relançado. A troca de filtros altera chaves de efeito; testes de navegação sob corridas/cancelamento ainda são necessários.
6. O Worker DEV aceita consulta controlada de `movie/{id}`, com 404 específico de filme não encontrado e sem cache negativo. `worker.test.mjs` cobre sequências 404/200, segredo backend, cache, rejeição de rotas/queries inválidas e método POST. A fonte publicada descrita no canônico **não** contém essa rota.

## Matriz residual para testes executáveis
- R01: IDs [inexistente, válido] no mesmo lote; validar HTTP 404 seguido por recuperação correta e gravação única.
- R02: 13–25 IDs legados; confirmar limite de 12 por tentativa, botão manual e retomada depois de nova entrada na aba.
- R03: cancelar a coroutine depois de um sucesso; reiniciar Activity/app e confirmar snapshot e IDs históricos intactos.
- R04: simular Worker antigo HTTP 400; garantir uma única tentativa de rota na sessão, sem apagar histórico e sem falso botão.
- R05: timeout/503 antes e após sucesso parcial; assegurar persistência, reentrada/retry e sem spin infinito.
- R06: falhar primeira página de Filmes/Séries/Cinema, trocar filtro durante requisição e repetir; nenhuma prateleira de outro gênero/provedor deve ser exibida.
- R07: regressão visual de três pôsteres 2:3 completos, hero legível, selo azul Cinema apenas BR e controles TCL, com aparelho real.

## Resultado e gates
**Análise estática concluída; nenhum defeito novo confirmado por execução.** Não editar código com base apenas em hipótese. CI #40169560233 e #40169560291 são históricos já registrados, não reexecuções desta rodada. OTA oficial 2.9.25/code54 e Worker publicado permanecem congelados; nenhum merge, deploy, assinatura, OTA, migração D1 ou companion. Projeto mantém 7/11 (63%), M08/M09 IN_QA, M10/M11 BACKLOG, F0–F8 NAO_HOMOLOGADO. Plano físico vigente: `docs/qa/ANYTV_V2_2.9.25_DEVICE_HOMOLOGATION_PLAN_20261025.md`.

## Nota documental
O QA `ANYTV_20261025_WATCHED_RETRY_GENRE_FAILURE.md` **já** informa corretamente que #40169458180 FAILED e #40169560233 SUCCESS no HEAD conferido; não repetir a errata do handoff como alteração necessária.
