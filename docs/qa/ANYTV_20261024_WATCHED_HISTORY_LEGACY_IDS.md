# AnyTV — QA de Assistidos antigos sem metadados (DEV)

**Data:** 24/10/2026. **PR #3:** `feat/anytv-p0-device-proof-verifier-20261020` aberto em *draft*. **Commit de código:** [`4ade163d`](https://github.com/brenoff2/tcl-apk-ci-builder/commit/4ade163d0c881a61023d2b62e7e19849a95267da). Alterados exclusivamente `overrides/TclStreamingScreens.kt` e `scripts/tests/test_streaming_screens_contract.py`. **Não** há novos endpoints, permissão, Worker deploy, merge, assinatura, release ou OTA.

## Causa no código existente

O histórico de filmes assistidos já persistia os IDs em `anytv_watched_movies` e guardava snapshots dos títulos vistos na pasta privada `anytv_catalog_v1/saved_title_movie_<id>.json`. Porém, filmes de versões anteriores podiam existir **somente como IDs**, e `restoreTrackedTitle("movie", id)` retornava `null` se o favorito ou o arquivo local já não existissem. O `watchedFilms.mapNotNull` então omitiria completamente o registro na aba **Assistidos**, apesar de o ID continuar presente no armazenamento. Isto não significa que o usuário desmarcou o filme.

## Correção auditável

1. O aplicativo mantém um segundo snapshot durável de metadados **reais e conhecidos** em `anytv_watched_movie_titles_v1` (SharedPreferences): ID TMDB, nome, descrição, poster/backdrop, data, rating/votos e gêneros. Foi utilizado o mesmo padrão de recuperação já empregado em `anytv_watching_titles_v1` para séries. Salva em um lote por marcação, sem eliminar o cache privado existente.
2. A resolução do histórico agora consulta favorito válido, snapshot de filme nas preferências e, por último, arquivo local `saved_title_movie_<id>.json`. Nome em branco não é usado como identificação falsa.
3. Quando a pessoa abre **Assistidos**, filmes antigos ainda identificáveis por favoritos ou pelo cache são **migrados em lote** para as preferências de histórico, sem consultar uma rota inexistente do Worker. A migração evita uma gravação por registro e ignora dados sem nome real.
4. Se algum ID ainda não tiver metadados legítimos, **Assistidos exibe uma mensagem discreta** com quantidade e até 12 IDs TMDB originais, indicando que o nome/cartaz não está disponível no aparelho. Os IDs **não são removidos**, e não se fabrica nome, capa ou disponibilidade. Os posters conhecidos continuam em três colunas com proporção 2:3.
5. `movie/{id}` **não faz parte** da allowlist atual do Worker. Logo, a recuperação remota completa de títulos legados não é afirmada nem introduzida sem aprovação e deploy próprios. A UI evita esconder IDs reais durante essa limitação.

## Contratos adicionados e preservados

- Leitura/gravação de `anytv_watched_movie_titles_v1` com validação de nome/ID, poster, ano, metadados e gêneros.
- Marcação como assistido persiste snapshot verdadeiro; migração de arquivo/favorito usa escrita **em lote**, não um loop de gravações.
- Informação de IDs legados aparece somente em **Assistidos**, sem criar `StreamingTitle` inventado nem chamar endpoints não previstos. Histórico independente de Favoritos.
- Contratos anteriores preservam logos oficiais, poster inteiro 2:3, grade de três, streaming/filmes/séries, paginação de futuros por BR/assinatura real, selo Cinema BR derivado de `movie/now_playing?region=BR`, controles remotos TCL e versão oficial intactos.

## GitHub-first e verificação

**Problema:** impedir perda silenciosa de acesso a histórico ID-only. **Upstream:** mecanismo de persistência local de séries já presente no projeto e semântica oficial AndroidX `remember`/`LaunchedEffect`. **Decisão: ADAPT**, sem dependência adicional. **Verificação CI:** [compile/candidata unsigned #40129882058](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40129882058) **COMPLETED/SUCCESS: 66 contratos Streaming + 2 credenciais + 11 visuais; Gradle BUILD SUCCESSFUL em 3m13s; Android com.tclremote.livre.v2/versionCode54 verificado**; [prova isolada #40129882062](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40129882062) **COMPLETED/SUCCESS**. **Artefato unsigned:** #12574047328 (anytv-v2-2925-code54-unsigned-audit, 23.285.790 bytes).

## Limites, homologação e política de entrega

- Não há restauração automática dos nomes/cartazes que **jamais** tenham sido guardados em favorito, cache ou snapshot das preferências. O ID se mantém e fica visível com indicação honesta.
- **QA físico no telefone Android/TCL:** NÃO EXECUTADO. Testar 1) entrada em Assistidos com favorito ainda salvo, 2) remoção do favorito sem afetar registro assistido, 3) limpeza/expiração do cache de catálogo com preferências intactas, 4) registros legados ID-only com aviso, 5) grade de três imagens, seis ações principais e ícones originais, 6) reinício do app.
- Candidato é **unsigned, code54**, não instalável por OTA sobre a versão assinada. Publicado continua **AnyTV 2.9.25/code54**, branch `feat/tcl-v22-updates-back-bluetooth`, HEAD `dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112`. Qualquer publicação futura requer aprovação, code>54 e certificado original.
- **Gates inalterados:** 7/11 (63%), M08/M09 `IN_QA`, M10/M11 `BACKLOG`, F0–F8 `NAO_HOMOLOGADO`.

## Readback GitHub final em 24/10/2026

No commit funcional [4ade163d](https://github.com/brenoff2/tcl-apk-ci-builder/commit/4ade163d0c881a61023d2b62e7e19849a95267da), o [CI unsigned #40129882058](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40129882058) terminou **COMPLETED/SUCCESS**: logs mostram **66 testes Streaming**, **2 credenciais**, **11 contratos visuais**, Gradle `BUILD SUCCESSFUL in 3m 13s`, identidade `com.tclremote.livre.v2`/versionCode **54**, e artefato de auditoria unsigned **#12574047328** (23.285.790 bytes). A [prova isolada #40129882062](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40129882062) também terminou **COMPLETED/SUCCESS**. Esse build não contém assinatura OTA e não demonstra funcionamento no aparelho físico. Nenhum gate F0–F8 foi promovido.
