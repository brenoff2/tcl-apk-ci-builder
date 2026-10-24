# AnyTV — QA da paginação Em breve · Streaming

**Data:** 24/10/2026. **Destino:** PR #3, branch `feat/anytv-p0-device-proof-verifier-20261020`, **draft**, somente DEV. **Mudança funcional:** [`c1ba0c10b199d7c7753db3a966d26b512cea7f1e`](https://github.com/brenoff2/tcl-apk-ci-builder/commit/c1ba0c10b199d7c7753db3a966d26b512cea7f1e) — `overrides/TclStreamingScreens.kt` e `scripts/tests/test_streaming_screens_contract.py`.

## Defeito anterior comprovado

A função `discoveryUpcomingStreaming()` consultava a lista inicial `movie/upcoming` e apenas `discover/tv?page=1`; validava a disponibilidade de assinatura BR em até 4 consultas paralelas e descartava resultados após os primeiros 24. A seção `Em breve · Streaming` não tinha uma rota de paginação no `Ver mais`. Assim, títulos futuros de páginas posteriores ficavam inacessíveis mesmo com resultados disponíveis no TMDB.

## Implementação cirúrgica

1. `discoveryUpcomingStreamingPage(page): StreamingResults` consulta `movie/upcoming?language=pt-BR&page=$page` e `discover/tv?language=pt-BR&include_adult=false&sort_by=first_air_date.desc&page=$page` com os endpoints **já autorizados** pelo Worker. O índice da página é validado entre 1 e 50 e a resposta `more` respeita `total_pages` de cada origem, sem suposições sobre catálogo.
2. Cada filme/série precisa de data futura real (`anyTvUpcoming`) e de oferta confirmada via `offersInBrazil` (região BR, `flatrate`, máximo de 4 chamadas simultâneas). Falha de verificação **não** vira disponibilidade positiva; mantém-se `false`.
3. Preserva todos os títulos confirmados na página 1, mesmo se existirem mais de 24: a prateleira continua exibindo até 24, mas a página dedicada `Ver mais` recupera a lista completa já verificada e revela mais 24 por clique. `Mostrar mais 24` consulta a página de origem seguinte quando esgota o buffer.
4. A página dedicada continua filtrando `anyTvUpcoming` + `homeContentType` (Filmes, Séries ou Todos). A regra anterior `anyTvReleased` permanece intacta para seções de títulos lançados. Títulos futuros do Cinema não ganham etiqueta BR sem `cinemaIds` confirmado.
5. O botão `Ver mais` para a seção aparece também quando existem poucas obras confirmadas, mas há outras páginas de origem a consultar. Nenhum novo serviço, chave, permissão, deploy ou redirecionamento foi acrescentado.

**Limites conscientes:** a fonte TMDB para `movie/upcoming` não equivale a anúncio de lançamento nas assinaturas brasileiras; só os registros com oferta BR confirmada aparecem. A cada página o cliente consulta as duas origens e pode executar até 40 verificações de provedor, limitadas a 4 simultâneas e auxiliadas pelo cache TTL. O app pode obter menos de 24 títulos adicionais quando não há direitos confirmados. É possível que nenhum filme futuro tenha oferta registrada hoje; isto não autoriza inventar título ou usar oferta internacional como substituta. O fluxo de recuperação retroativa de filmes antigos marcados apenas por ID permanece bloqueado pela ausência de `movie/{id}` na allowlist do Worker.

## Evidência e upstream

- Contratos adicionados verificam `page` entre 1 e 50, duas fontes com paginação, `total_pages`, filtragem por data futura e oferta BR, preservação de sobras da página 1, manutenção do filtro de mídia e inexistência da antiga limitação de 24 títulos no resultado do backend.
- TMDB oficial: [Discovery TV](https://developer.themoviedb.org/reference/discover-tv), [Discovery Movie](https://developer.themoviedb.org/reference/discover-movie) e [Region Support](https://developer.themoviedb.org/docs/region-support). A disponibilidade BR já é verificada pela rota `{media}/{id}/watch/providers` do próprio Worker.
- Decisão GitHub-first: **ADAPT** funções, filtros e allowlist existentes; **BLOCK** qualquer mudança no Worker de produção e qualquer promessa de oferta no Brasil não comprovada.

## Readback CI

- [Compile + contracts unsigned #40124990171](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40124990171): **COMPLETED/SUCCESS (62 contratos Streaming + 2 credenciais + 11 visuais; Gradle BUILD SUCCESSFUL 3m35s; identity V2/code54; unsigned artifact #12572691383)**.
- [Prova isolada #40124990149](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40124990149): **COMPLETED/SUCCESS**.
- Candidato é **unsigned** e code54; proibido substituir o APK assinado 2.9.25/code54, publicar OTA, marcar QA físico ou promover milestones em razão do build. OTA oficial e branch de distribuição `feat/tcl-v22-updates-back-bluetooth` continuam em `dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112`.

## QA manual ainda pendente

Em Android físico, conferir prateleira `Em breve · Streaming`, `Ver mais` mesmo com 1–6 itens, lotes subsequentes, ausência de duplicatas, mídia Filmes/Séries, títulos futuros verificados BR, rolagem, três pôsteres inteiros e indicadores originais. Repetir com o app offline e com falha de rede. Validar a TCL física separadamente: pareamento, continuidade, abertura de serviços. Nenhum desses testes físicos foi executado.

**Gates mantidos:** 7/11 = 63%, M08/M09 `IN_QA`, M10/M11 `BACKLOG`, F0–F8 `NAO_HOMOLOGADO`; PR #3 `DRAFT`, sem merge, sem deploy, sem nova assinatura ou OTA.

## Verificação final readback GitHub

Executado sobre o commit de teste **c8bbbb04** cujo código Android permanece **idêntico ao commit c1ba0c10**. [Build unsigned #40125105465](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40125105465): COMPLETED/SUCCESS; **62+2+11 testes PASS**, Gradle `BUILD SUCCESSFUL in 3m 35s`, `com.tclremote.livre.v2` / code54 verificado e artefato unsigned **#12572691383** armazenado. [Prova isolada #40125105419](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40125105419): SUCCESS. Primeiro run #40124990171 falhou apenas pelo contrato legado buscando nomes internos antigos; o teste foi atualizado, sem mutação do código compilado. Nenhum aparelho físico validado.
