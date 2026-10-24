# AnyTV — QA residual de código após OTA 2.9.25 / code54

Data: 24/10/2026. Repositório: `brenoff2/tcl-apk-ci-builder`. Branch DEV: `feat/anytv-p0-device-proof-verifier-20261020`. PR [#3](https://github.com/brenoff2/tcl-apk-ci-builder/pull/3) DRAFT, sem merge. Último commit funcional auditado: [`9ac3529f`](https://github.com/brenoff2/tcl-apk-ci-builder/commit/9ac3529f7949e9d721b6ef875bd3791f237d1c61). Última OTA oficial assinada: 2.9.25-AnyTV / versionCode 54, pacote `com.tclremote.livre.v2`, run [#40106161667](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40106161667) SUCCESS; atualização física não confirmada. A revisão DEV posterior não está distribuída.

## Diagnóstico e correção cirúrgica

1. **Ver mais limitado a 24**: a pagina aberta por `openedSection` usava `uniqueShelves` truncada em 24, mas o botão de carregamento adicional só aparecia em `homeLibrary`. Agora o fluxo reaproveita `StreamingCatalog.catalogPage24`, pagina fontes editoriais e secções de provedores por 24, preserva IDs sem duplicação, filtros de estreias e a distinção cinema/streaming. Atualização de seção ignora a resposta de rede que chega depois de mudar de página. As páginas de **Em breve · Streaming** (validação BR individual) e outras categorias não mapeadas ainda não têm paginação adicional homologada; não declarar cobertura universal.
2. **Assistidos sem dados restauráveis**: o `toggleFilmWatched(id)` gravava apenas ID; com cache expirado e sem favorito, um filme poderia desaparecer de Assistidos. A rotina agora recebe `StreamingTitle` e usa o snapshot `saved_title_movie_` já persistido e não expurgado, conservando poster, nome, ano e rating/votos reais ao marcar o cartão; ambas entradas (cartão e ficha) foram ajustadas.
3. **Lote inicial filtrado**: em `Ver mais`, não exigir 24 cartões ainda visíveis para oferecer a próxima página, pois títulos removidos pelo filtro poderiam bloquear a paginação. Mantido `sectionMore`, limite de página e deduplicação.
4. **Não alterados**: `AnyTvPosterCard` (aspecto 2:3, `ContentScale.Fit`), grade três colunas, logos originais, cabeçalho do controle, D-pad, IME, companion, Worker/D1, assinatura, canal V2 e canal legado.

## Evidências automatizadas

- Candidato original pós-unificação, [CI #40114130735](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40114130735): SUCCESS no código `b4fecc63`; não cobre as correções residuais.
- Primeira rodada de ajustes, [CI #40119408922](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40119408922): FAILURE em contrato textual legado `toggleFilmWatched(id: Int)`, atualizado ao tipo `StreamingTitle`. Os demais 52 testes dessa etapa passaram. Não confundir com erro Gradle.
- Nova execução para revisão `9ac3529f`: [CI unsigned #40119630330](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40119630330) **COMPLETED/SUCCESS: 53 contratos Streaming, 2 de credenciais, 11 visuais e Gradle BUILD SUCCESSFUL (4m41s)**, com contratos e compile Gradle, **SEM ASSINAR/PUBLICAR**. Prova isolada [#40119630288](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40119630288): SUCCESS.
- Testes de regressão em `scripts/tests/test_streaming_screens_contract.py` PASS. CI #40119630330 confirmou `assembleRelease` SUCCESS, package `com.tclremote.livre.v2`, versionCode 54, versionName `2.9.25-AnyTV`; artefato **unsigned** `anytv-v2-2925-code54-unsigned-audit` ID **12570905996**. Sem assinatura e sem OTA nova.

## Matriz de homologação física (não executada)

| Superfície | Evidência estática | Evidência no telefone |
| --- | --- | --- |
| Home / Ação / gêneros | cartão único e grade 3, `ContentScale.Fit` | **NAO_TESTADO** |
| Cinema BR, internacional e Em breve | fontes e seções independentes verificadas no código | **NAO_TESTADO** |
| Favoritos e Assistidos | armazenamento local e snapshot durável corrigido | **NAO_TESTADO** (reinício, retrocompatibilidade) |
| Continuando | IDs e séries persistentes, busca própria | **NAO_TESTADO** |
| Ver mais e Mostrar mais 24 | carregamento de página adicional implementado nas fontes mapeadas | **NAO_TESTADO** (rede, scroll, deduplicação) |
| Busca global e histórica | overlay preto e 3 colunas preservados | **NAO_TESTADO** |
| Cartão pequeno em telas estreitas | 2:3 e metadados presentes no código | **NAO_TESTADO** (sem corte, toque, texto longo, ícones) |
| Hero cinematográfico, filtros e sombra/blur | gradiente está no código; sombra visual não homologada | **NAO_TESTADO** |
| Pareamento, TLS, IME, apps e abertura TCL | integração não modificada neste lote | **NAO_TESTADO** |

**Gates**: F0–F8 = NAO_HOMOLOGADO; M08/M09 = IN_QA; M10/M11 = BACKLOG; progresso 7/11 = 63%. Nenhum novo release/signer/OTA/merge autorizado ou realizado. Próxima release futura só com autorização expressa, fluxo oficial e versionCode maior que 54.

## Readback final — 24/10/2026

- [CI #40119630330](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40119630330) COMPLETED/SUCCESS: 53+2+11 testes PASS; Gradle SUCCESS 4m41s; identidade code54 e upload artifact #12570905996 verificados.
- [Prova isolada #40119630288](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40119630288) SUCCESS.
- Documentação posterior não alterou o código compilado. Celular/TCL seguem **NAO_TESTADO**, F0–F8 NAO_HOMOLOGADO. OTA assinada 2.9.25/code54 permanece vigente.
