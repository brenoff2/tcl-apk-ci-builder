# AnyTV — checkpoint de padronização visual DEV (25/10/2026)

## Pedido original e controle de escopo
Revisar e padronizar Home, Cinema, gêneros, provedores, Favoritos, Assistidos e séries acompanhadas, usando 3 pôsteres originais 2:3 por linha, metadados, ações e canais com o mesmo componente da Home. Destaques de Cinema devem ter nome/ano/cinema sobre a arte com sombra/gradiente legível. Renomear a área de séries em progresso sem alterar seu dado/persistência. Aprovação do usuário para corrigir e preparar atualização; publicação somente após compilação, assinatura correta, versionCode >54, gate de release e readback.

## Evidência desta rodada — código DEV
- Baseline antes da rodada: `1f72056df6686ee63c31722baca553a038e77fbf`; branch `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 draft.
- `29958770`: título, seção e ação de cabeçalho `Continuando` renomeados para `Séries acompanhadas`, sem modificar chave de persistência `watching`.
- `0ea9546e`: expectativas existentes atualizadas para a nova denominação.
- `508060c7`: Hero Cinema remove fundo preto em caixa somente sob título e usa gradiente integral inferior, com texto sobre imagem e `Shadow` já existente (sem redesenhar imagem).
- `5cfcff38`: contrato textual novo cobre denominação, compartilhamento da grade 3x e gradiente do hero de Cinema.
- Revisão direta de `overrides/TclStreamingScreens.kt`: `AnyTvPosterCard` é compartilhado por Home e biblioteca; imagem usa `aspectRatio(2f / 3f)` e `ContentScale.Fit`; Assistidos/Favoritos usam `chunked(3)`; Séries acompanhadas também usa `chunked(3)`; cinema internacional e futuro usam `AnyTvOneRowPreview` com largura de 3 cards visíveis. Essas verificações são ESTÁTICAS: capturas antigas da OTA não demonstram a aparência do candidato DEV.
- `AGENTS.md`, `docs/STATUS.md`, `docs/roadmap/PROJECT_PROGRESS.json` e plano de homologação física devem preservar gates até CI final e QA físico.

## Execução CI e limitações
- Workflow unsigned sem OTA `.github/workflows/anytv-v2-candidate-compile.yml` disparado para `5cfcff38997963f3ce7f897a188b1be2381544af`; run **#40184293156** ([Actions](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40184293156)); estado na última leitura: **PENDING**. Resultado do teste Gradle, contagem de contratos e artefato: **NAO_COMPROVADO** até leitura posterior.
- Checkout Windows via Chat On Steroids não chegou a HEAD válido; não foi usado para editar código nem para assinar APK. Edição efetiva foi realizada pelo MCP GitHub com SHAs e commits confirmados.
- Não houve nesta rodada: assinatura, alteração do package/versionCode, publicação OTA, deploy Worker, migração D1, alteração companion ou testes físicos. Não reportar release feita.
- Os erros de layout da OTA 2.9.25 nas imagens do usuário precisam de teste real depois de empacotar uma build assinada mais recente, pois o código DEV pode diferir muito do APK publicado.

## Pendências materiais antes da OTA
1. Aguardar resultado efetivo #40184293156 e ler logs; corrigir erros eventuais sem inflar gate.
2. Fazer regressão visual em Android físico com capturas comparando Home, Cinema, Filmes/Séries, Favoritos, Assistidos, Séries acompanhadas, gêneros, provedores e modal de filtros.
3. Após CI verde, preparar release aprovado com `versionCode>54`, mesma identidade de pacote e certificado V2 original; verificação da assinatura, hash e readback exato do canal OTA. Não usar unsigned para atualização.
4. Homologar funções TCL/pareamento e distinguir Worker publicado da rota `movie/{id}` DEV, sem mudar produção automaticamente.
5. Preservar 7/11=63%, M08/M09 IN_QA, M10/M11 BACKLOG e F0–F8 NAO_HOMOLOGADO.

**Status:** implementação DEV parcial, sem build/release comprovados nesta rodada.
