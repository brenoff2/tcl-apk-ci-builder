# AnyTV — QA funcional: selo Cinema BR e paginação por provedor

**Data:** 24/10/2026. **Projeto:** AnyTV Android V2. **Escopo:** PR #3 `feat/anytv-p0-device-proof-verifier-20261020` (DEV, draft), somente app Android e contratos. **Sem** merge, release, assinatura, OTA, deploy Worker/D1, mudanças na TCL ou geração de imagens. **Fonte publicada intocada:** 2.9.25-AnyTV, `com.tclremote.livre.v2`, versionCode 54, OTA `feat/tcl-v22-updates-back-bluetooth` commit `dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112`.

## Erros comprovados no código anterior

1. `AnyTvOneRowPreview` deduzia estado em cartaz no Brasil com `heading.contains("Cinema")`: títulos de `Em breve · Cinema` e `Cinema pelo mundo · seleção internacional` recebiam selo **Cinema** sem prova de `movie/now_playing?region=BR`. O cabeçalho da prateleira **não é** prova de lançamento BR.
2. `Destaques <provedor> · Séries` consultava originalmente `discover/tv` com gênero `0`, mas sua página dedicada `Mostrar mais 24` enviava `selectedHomeGenre` (gênero de filmes). Isto podia produzir seção de séries vazia ou com recorte diferente da página 1.
3. Em página individual de serviço, `Por gênero: ... → Ver mais` executava `selected = null`, redirecionando ao gênero **global** e descartando o provedor, além de não ter rota própria para a paginação de 24.

## Correções aplicadas em DEV

- [a92c3703](https://github.com/brenoff2/tcl-apk-ci-builder/commit/a92c3703f48c13eb86b954bf6ecc7c0dda80f6cb): indicador de Cinema deixa de derivar do texto dos trilhos internacionais/futuros; apenas o `cinemaIds` confirmado pela fonte BR ou a seção BR autêntica permitem a etiqueta. Paginação das prateleiras de provedores espelha exatamente o filtro inicial: filme usa gênero de filmes selecionado, série usa `0` quando a origem assim fez.
- [6183135d](https://github.com/brenoff2/tcl-apk-ci-builder/commit/6183135d6d87662638414a1671c818e084d512b4): `Por gênero: ... → Ver mais` aberto a partir de um streaming permanece na página do serviço e resolve o `Triple(nome, media, genero)` correspondente ao primeiro lote para buscar as próximas páginas, com 24 por lote e sem mistura entre serviços. A navegação de gênero global continua como antes.
- Alterados **somente** `overrides/TclStreamingScreens.kt` e `scripts/tests/test_streaming_screens_contract.py`. Grade 3×, poster 2:3/`ContentScale.Fit`, `AnyTvPosterCard`, Cinema BR, hero, nomes, ano, rating, ações, logos, provider badges originais, TV remote, assinatura e backend permanecem preservados.

## Verificação (readback GitHub)

- [CI unsigned conjunto #40123511774](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40123511774): **COMPLETED/SUCCESS — 59 testes Streaming, 2 testes de credenciais, 11 testes visuais, Gradle BUILD SUCCESSFUL (4m47s), identidade V2/code54 verificada, artefato unsigned #12571982902**.
- [Prova isolada #40123511804](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40123511804): **COMPLETED/SUCCESS**.
- Execução intermediária [#40123338242](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40123338242), originada do commit a92c3703: **CANCELLED pela nova revisao de codigo; contratos haviam passado, mas o build intermediario nao foi considerado homologado**. A execução final do commit 6183135d prevalece na homologação desta rodada.
- Testes de regressão: falso selo BR por nome de trilho; mesma combinação `media/gênero` entre página 1 e página 2 de provedores; continuidade do streaming ao abrir `Ver mais` de gênero do próprio provedor.
- Padrões verificados contra a documentação TMDB: `/discover/movie` (https://developer.themoviedb.org/reference/discover-movie), `/discover/tv` (https://developer.themoviedb.org/reference/discover-tv) e regiões (https://developer.themoviedb.org/docs/region-support). Decisão GitHub-first: **ADAPT** o fluxo de consulta já existente; não criar fonte de direitos, etiquetas nem API adicional.

## Limites e próximos gates

- **QA físico Android e TCL: NAO_TESTADO**. Validar em aparelho real: "Em breve · Cinema" sem selo indevido, selo azul apenas em filmes realmente da listagem BR, por gênero dentro da Netflix/Prime/etc. permanece no provedor ao tocar `Ver mais` e `Mostrar mais 24`, com scroll, nome, ano, ícones e três posters íntegros.
- **Pendente preexistente**: títulos assistidos antigos salvos apenas por ID podem não ter snapshot; abrir `movie/{id}` no Worker para recuperação exigiria novo escopo e deploy autorizado. **Em breve · Streaming** sem paginação global comprovada permanece fora deste lote; não afirmar cobertura de 24 adicionais nesse caso.
- **Não publicar** novo APK code54 unsigned sobre 2.9.25/code54 assinado. Eventual release futura exige aprovação explícita, nova versãoCode >54 e certificado V2 original. Progresso 7/11 (63%) **inalterado**, M08/M09 IN_QA, M10/M11 BACKLOG, gates F0–F8 NAO_HOMOLOGADO.


## Readback final 24/10/2026

Run de compilacao unsigned #40123511774 executado sobre **6183135d**: COMPLETED/SUCCESS; **59+2+11** contratos PASS; Gradle BUILD SUCCESSFUL em **4m47s**; identificacao de pacote `com.tclremote.livre.v2` / code54 verificada; APK unsigned de auditoria **#12571982902** armazenado. Prova isolada #40123511804 SUCCESS. O build intermediario #40123338242 foi cancelado pelo commit de consolidacao. Nenhum novo release ou teste fisico.
