# AnyTV — QA do primeiro lote vazio e filtros por provedor (DEV)

**Data:** 24/10/2026. **Branch DEV/PR #3:** `feat/anytv-p0-device-proof-verifier-20261020` em *draft*. **Commit funcional:** [ef73ec03](https://github.com/brenoff2/tcl-apk-ci-builder/commit/ef73ec03d585a0ebf8e4561c9b646b6c026916b6). **Escopo:** apenas `overrides/TclStreamingScreens.kt` e `scripts/tests/test_streaming_screens_contract.py`; nenhum Worker, companion TV, publicação, assinatura, OTA ou imagem alterados.

## Causas verificadas na fonte Android

1. A paginação introduzida em `c1ba0c10` já buscava filmes e séries futuros com `offersInBrazil`, mas quando a *primeira* página retornava zero títulos com assinaturas confirmadas no Brasil (`future.isEmpty()`), a seção não era inserida em `visibleShelves`, desaparecia em `uniqueShelves`, e nunca oferecia `Ver mais`. Além disso, a tela dedicada exigia `sectionAll.isNotEmpty()` antes de consultar a próxima página; portanto, uma página intermediária com zero resultados bloqueava a navegação.
2. A busca inicial dos provedores aplicava o gênero selecionado somente a filmes, inclusive na aba **Séries** (`homeContentType = tv`). O catálogo de séries não respeitava o gênero TV escolhido. A página do provedor buscava sempre o gênero 0 ao mudar o chip, e a leitura de `myServiceShelves` usava chave incompatível; `Mostrar mais 24` podia aplicar outro gênero em relação à página inicial. O problema atingia tanto o streaming individual quanto `Destaques` configurados na tela inicial.

## Solução implementada

- `future.isNotEmpty() || upcomingStreamingMore` mantém a prateleira **Em breve · Streaming** navegável enquanto houver páginas de origem. A lista `uniqueShelves` preserva a exceção controlada para este único título de prateleira mesmo com zero cards. Em vez de um trilho sem cards, a tela informa que a primeira página não confirmou nenhuma assinatura BR e oferece **Ver mais**. A página dedicada mantém **Mostrar mais 24** inclusive quando a página atual não produziu títulos e usa `sectionPageLimit = 50`, alinhado à validação de `discoveryUpcomingStreamingPage` e ao Worker. Ela sinaliza ausência de ofertas confirmadas sem criar lançamentos falsos.
- A função pura `anyTvProviderGenre(media, contentType, selectedGenre)` centraliza um contrato: **Todos/Filmes → gênero de filme**, **Séries → gênero de série**, outro tipo → 0. O mesmo contrato é consumido no carregamento inicial de `myServiceShelves`, na tela de um provedor, nos trilhos `Destaques`, nas chaves de cache e na paginação de **Ver mais/Mostrar mais 24**. A coleta dependente de provedor agora responde à mudança de `selectedHomeGenre`; o provedor não muda e nenhum gênero de filme é aplicado silenciosamente em TV.
- Os posters 2:3 inteiros, três por linha, logos originais dos serviços, título/ano/nota, selo Cinema BR comprovado, gradiente, hero, apps instalados e ações aprovadas não foram redesenhados.
- Não foi solicitada nenhuma nova chave, permissão, API, endpoint, serviço ou protocolo. A decisão GitHub-first é **ADAPT** a lógica existente de consulta e estado; falta de licença BR continua `false`, nunca um título inventado.

## Contratos e evidências

- O arquivo `scripts/tests/test_streaming_screens_contract.py` inclui regressões que exigem a visibilidade da prateleira mesmo com zero títulos, a preservação do botão em páginas vazias, o teto de 50 páginas do endpoint de futuros, mensagens honestas e consistência das chaves e dos gêneros entre página inicial, provedor, Filmes, Séries, Todos e páginas seguintes.
- [CI intermediário unsigned #40127722728](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40127722728): **COMPLETED/FAILURE**, por asserção textual desatualizada de teste após ajustar parênteses; esta execução não concluiu a compilação Android. **Não usar como prova de build.**
- [CI unsigned final #40127825662](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40127825662): **COMPLETED/SUCCESS (64 testes Streaming + 2 credenciais + 11 visuais; Gradle BUILD SUCCESSFUL 4m41s; identidade V2/code54; artefato unsigned #12574245289)**.
- [Prova isolada final #40127825663](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40127825663): **COMPLETED/SUCCESS**.
- Upstream: referência ao estado `LaunchedEffect` e `remember` no repositório oficial `androidx/androidx` e regras de `page`/discover/watch-provider do TMDB já implementadas na allowlist do Worker e no contrato de catálogo do projeto.

## QA físico e riscos restantes

- **Android/TCL físicos: NAO_TESTADOS** nesta rodada. Validar a seção futura sem títulos BR na página 1 (mensagem + botão), páginas intermediárias vazias, filtro Filmes/Séries, `Ver mais`, retorno, atualização de poster 2:3 e metadados. Validar Netflix/Prime/Disney etc. com gênero de filmes, gênero de séries e sequência de 24 sem misturar serviços.
- **Histórico legado:** entradas antigas de filmes assistidos armazenadas apenas como ID podem continuar invisíveis sem metadados salvos; resolver integralmente exige endpoint de leitura `movie/{id}` no Worker atual (não permitido na allowlist) e implantação autorizada. Não foi inventado título nem incluída dependência quebrada no Android.
- **Releases:** OTA assinada vigente **2.9.25/code54**, `com.tclremote.livre.v2`, branch de distribuição `feat/tcl-v22-updates-back-bluetooth` SHA `dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112` intocada. Este candidato continua *unsigned* com code54, não instalável por OTA sobre release assinado sem nova versãoCode, certificado e aprovação.
- **Gates:** M08/M09 `IN_QA`, M10/M11 `BACKLOG`, 7/11 (63%), F0–F8 `NAO_HOMOLOGADO`; `PR #3 DRAFT`, sem merge, sem assinatura ou deploy.

## Readback final — 24/10/2026

[Compilação unsigned #40127825662](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40127825662): COMPLETED/SUCCESS no commit de teste `229b7567`, com fonte Kotlin **idêntica** ao commit funcional `ef73ec03`: **64 contratos Streaming**, **2 credenciais**, **11 contratos visuais**, Gradle **BUILD SUCCESSFUL in 4m41s**, identidade `com.tclremote.livre.v2` versionCode **54** validada e artefato de auditoria unsigned **#12574245289** registrado. [Prova isolada #40127825663](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40127825663): SUCCESS. CI intermediário #40127722728 falhou apenas em contrato textual desatualizado após adicionar parênteses à expressão de preservação de histórico — corrigido em `229b7567` sem mutação do aplicativo. **Nenhum teste físico/OTA/merge/deploy.**
