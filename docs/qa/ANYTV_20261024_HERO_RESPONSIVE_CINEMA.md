# AnyTV — QA residual do hero, Cinema e responsividade (24/10/2026)

**Escopo:** somente DEV, PR #3 draft `feat/anytv-p0-device-proof-verifier-20261020`. Sem merge, nova assinatura, OTA, Worker, companion ou instalação física. **Última versão assinada e publicada:** V2 2.9.25-AnyTV/code54, pacote `com.tclremote.livre.v2`, Actions #40106161667 SUCCESS, branch de OTA `feat/tcl-v22-updates-back-bluetooth` commit `dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112`. A revisão DEV tem apenas APK unsigned de auditoria, jamais substituir a versão assinada no telefone.

## Causas verificadas no código original

1. No hero editorial do Streaming, `Text(current.name)` e `StreamingOfferIcons` vinham após o `Box` da imagem, enquanto a referência aprovada exigia nome e dados **sobre** a imagem. O pager mostrava caracteres de bolinhas em uma faixa escura.
2. O cabeçalho `AnyTvStreamNavBar` posicionava as duas chips de conteúdo e **seis botões de 38 dp** em uma única `Row`, sem composição responsiva. Apenas os seis botões já somam 228 dp, mais 25 dp de intervalos, antes das chips e dos 20 dp de margem da tela: risco estrutural de extrapolar a largura de telefones menores.
3. A paginação residual de `Ver mais` da rodada anterior tinha correção para lotes, mas as novas páginas de um provedor podiam deixar de passar o gênero ativo a `StreamingCatalog.discover`, misturando resultados ao clicar Mostrar mais 24.
4. O hero independente `AnyTvCinemaHero` incluía só o nome sobre a imagem, sem ano real nem o selo azul Cinema esperado no visual aprovado.

## Correções cirúrgicas

- [Commit 08cd4807](https://github.com/brenoff2/tcl-apk-ci-builder/commit/08cd4807f9e73ecd354dfbd02cddfbf5f0a97cee): nome do hero dentro da imagem, gradiente vertical de contraste, sombra/blur nativo do `TextStyle`, ano real e avaliação apenas com votos, tag azul Cinema apenas para filmes confirmados pela fonte BR, indicadores discretos de progresso. O banner compacto da home recebeu a mesma sombra de texto sem trocar imagens, ícones ou ações.
- No mesmo commit, o cabeçalho passa a usar `BoxWithConstraints`, reorganizando chips e os seis **mesmos** `TclHeaderAction` em duas linhas em viewport com largura menor que 390dp; em telas amplas, mantém a composição horizontal. A faixa de ações pode deslizar em larguras extremas. Paginação de provedores preserva o gênero existente.
- [Commit 8d479b5a](https://github.com/brenoff2/tcl-apk-ci-builder/commit/8d479b5ab1d81fa72519f0d5e89cd6760ae1f8d1): no hero de Cinema BR, ano real quando presente, nome com sombra e selo Cinema azul no fundo original; a imagem permanece panorâmica e o toque continua abrindo a ficha.
- O componente `AnyTvPosterCard` (pôster 2:3, `ContentScale.Fit`, três por linha), marcas F/S, logomarcas originais de provedores, API TLS/IME, D1, dispositivos e assinaturas **não foram modificados**.

**Evidência upstream:** Google Android Developers orienta `BoxWithConstraints` para composição responsiva com base em `maxWidth` (https://developer.android.com/develop/ui/compose/layouts/basics) e `TextStyle(shadow = Shadow(...))` para sombra do texto (https://developer.android.com/develop/ui/compose/text/style-text?hl=pt-br). Decisão: ADAPT os mecanismos oficiais de Compose sobre os elementos AnyTV existentes, sem nova dependência ou redesign.

## Readback automatizado

- [CI unsigned #40121432431](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40121432431): **SUCCESS** no código `08cd4807`; **56 testes de Streaming, 2 de credenciais, 11 visuais**, Gradle `BUILD SUCCESSFUL` em **4m32s**, identidade V2/code54 e upload de artefato de auditoria **#12571288622**.
- [Device proof isolado #40121432377](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40121432377): **SUCCESS**.
- **CI final do código 8d479b5a:** [#40121842745](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40121842745) — `COMPLETED/SUCCESS: 57 testes Streaming, 2 de credenciais, 11 visuais, Gradle BUILD SUCCESSFUL (5m27s), identidade V2/code54 unsigned verificada, artefato #12571029713`. **Prova isolada** [#40121842751](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40121842751) — `COMPLETED/SUCCESS`. O status final é inserido só após leitura LIVE.
- Tests acrescentados em `scripts/tests/test_streaming_screens_contract.py` verificam a posição real do nome dentro do hero, metadados condicionais, sombra, indicadores, cabeçalho responsivo, gênero da paginação e label de Cinema BR. **Testes de contrato e build não substituem renderização física**.

## QA físico ainda não executado

| Área | Critério de aceite no aparelho | Estado |
| --- | --- | --- |
| Streaming e Cinema BR | Nome/ano/nota legíveis sobre hero claro e escuro, blue Cinema correto, indicadores discretos, sem corte do título | NAO_TESTADO |
| Largura 320-390 dp, fonte ampliada, landscape | Ícones visíveis ou roláveis, fechar/filtros acessíveis, sem clipping de botões | NAO_TESTADO |
| Gênero Ação, Cinema, Favoritos, Assistidos, Continuando, busca, Ver mais | Três pôsteres inteiros 2:3, metadata/ícones originais, toque/scroll | NAO_TESTADO |
| Paginação e filtros | Mostrar mais 24 sem trocar provedor/gênero, sem IDs duplicados, resultados estáveis após filtro | NAO_TESTADO |
| Dados locais | Favoritos, assistidos, séries e busca resistem a fechar/reabrir aplicativo | NAO_TESTADO |
| TCL física | Companion realmente instalado, TLS, pareamento, IME, volume/canal e abertura de apps/ficha real | NAO_TESTADO |

**Dependência preexistente identificada, não alterada:** filmes marcados como assistidos nas versões antigas podem ter somente IDs e nenhum snapshot. A recuperação por `movie/{id}` não pode ser simplesmente copiada de séries: o Worker atualmente permite `tv/{id}` mas **não** `movie/{id}` em `catalogRequest`; abrir rota de backend exige revisão, CI e autorização para deploy. Os filmes marcados no código DEV novo passam a criar snapshot durável. Não alegar recuperação retroativa universal.

**Escopo restante:** `Em breve · Streaming` e seções sem fonte de paginação dedicada não têm expansão universal comprovada além do primeiro lote. QA de renderização no Android/TCL bloqueado por falta de acesso aos aparelhos e por não haver nova versão assinada publicada. Não instalar unsigned code54 sobre signed code54.

**Marcos e gates:** 7/11 = 63%; M08 e M09 IN_QA; M10 e M11 BACKLOG; F0–F8 todos NAO_HOMOLOGADO. Nenhum aceite físico promovido, nenhuma publicação ou merge.

## Readback final

O workflow #40121842745 executado no commit funcional 8d479b5a concluiu **SUCCESS** (57+2+11 testes, Gradle 5m27s, package V2 code54, artifact unsigned #12571029713). A prova isolada #40121842751 concluiu **SUCCESS**. Nenhum APK assinado novo ou OTA; verificações físicas continuam pendentes.
