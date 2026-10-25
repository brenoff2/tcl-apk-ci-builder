# AnyTV V2 2.9.30/code59 — filtros de gênero por mídia e streaming

**Data:** 25/10/2026. **Status:** versão assinada publicada em OTA e verificada; homologação física pendente.

## Requisitos do usuário
1. Ao selecionar um gênero na Principal, tratá-lo como filtro e como página com título do gênero, apresentando filmes **e** séries se o filtro de mídia for Todos.
2. Ao alternar Filmes/Séries, **preservar** o gênero e exibir somente a mídia escolhida.
3. Ao entrar em um streaming, manter gênero e tipo de mídia selecionados; o gênero limita o catálogo daquele serviço, não leva à página global sem filtro.
4. Ícones Favoritos/Assistido com sombra suavizada/blur grafite, sem novo badge circular.

## Código e causa raiz
- `overrides/TclStreamingScreens.kt`: `onContentType` antes zerava `selectedHomeGenre`, e a entrada de provedor também zerava o gênero e forçava Todos. Gêneros de filmes e séries não compartilham necessariamente ID TMDB.
- Resolução de ID por mídia `anyTvResolvedGenre` usa listas reais `allMovieGenres` e `tvGenres`, com equivalências conhecidas por nome (e.g. Ação/Aventura e ficção/fantasia) e **sem cair para catálogo sem filtro quando não há correspondência**.
- Em `homeContentType == "all"`, a busca e a página do gênero incluem movies e tv; na seleção de mídia, a exibição restringe a essa mídia mantendo o gênero.
- Provedores e paginação utilizam a mesma resolução de ID; as prateleiras anteriores sem gênero não se misturam quando gênero está selecionado.
- Cinema BR usa o fluxo separado `movie:$ANYTV_CINEMA_FILTER:$genreSort` e mantém hero/prateleira; selecionar Séries enquanto Cinema está ativo remove apenas o filtro exclusivo de Cinema.
- Ícones possuem o mesmo shadow grafite em `Modifier.blur(1.dp)` sobre uma cópia de ícone, mantendo as cores existentes, sem fundo circular.
- Principais commits DEV: `5ee2b710`, `b49494de`, `86da85b6`, `eb9212c0`, `d6059d1b`, `9a185ee8`, `40ab9a3b`, `46835bc8`, `859e0d25`, `d55492f5`, `28e643db`; testes contratados e atualizados até `645cc026`. Branch de desenvolvimento `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 draft SEM merge.
- Não foi alterado Worker, D1, companion TCL, dados persistidos, certificado ou OTA legada.

## Evidência de CI e release
- CI DEV unsigned [#40205357035](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40205357035): **COMPLETED / SUCCESS**; testes de UI, link e segurança e compilação Android; compilação unsigned serve somente de auditoria.
- Release assinado [#40205466794](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40205466794): **COMPLETED / SUCCESS**; testes de streaming/segurança, Gradle release, identidade RSA, IME, TMDB live, assinatura V2 original, publicação OTA e verificação HTTPS byte a byte.
- `package=com.tclremote.livre.v2`, `versionName=2.9.30-AnyTV`, `versionCode=59`.
- SHA256 do APK assinado, validado no log: `041775056dfefc5b7db149774c28f52b3beb222e4bbea6fe15bf3d889c4c9517` (`ANYTV_V2_SIGNER_VALIDATED_...`).
- Commit OTA `6c49b2e`, branch `feat/tcl-v22-updates-back-bluetooth`, caminho `updates/android/v2/current.apk`.
- Compare GitHub OTA anterior `71afa4a` → atual: **somente `updates/android/v2/current.apk` alterado**; `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS` comprovado.
- Artifact `anytv-v2-2930-signed-update` id `12602764701`.
- Run inicial #40205137276 foi **cancelado ANTES de publicar** ao identificar regressão potencial em Cinema; regressão corrigida na fonte e versão final publicada no run #40205466794.

## Limites e acompanhamento
- Revalidar presencialmente Principal > gênero > Filmes/Séries; Streaming > gênero > Filmes/Séries; Cinema BR; qualidade dos ícones sobre pôsteres escuros e claros. Não afirmar homologação física por CI.
- Estado canônico permanece 7/11 (63%), M08/M09 IN_QA, M10/M11 BACKLOG e F0–F8 NAO_HOMOLOGADO.
- No aplicativo assinado V2: Configurações > Buscar atualizações. Não instalar artefato unsigned.
