# PROJETO ANYTV — TRANSIÇÃO COMPLETA PARA NOVO CHAT (25/10/2026)

> **ESTADO DE RETOMADA — verificado via MCP GitHub:** repositório privado `brenoff2/tcl-apk-ci-builder`, branch DEV `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 **draft, sem merge**. Última OTA **2.9.31/code60**, publicada em `feat/tcl-v22-updates-back-bluetooth:updates/android/v2/current.apk`; commit da OTA **`481d498`**, release Actions **#40208593028 SUCCESS**, hash do APK **`177b2f422b55858177e661855aea8215119b8a0c0917748de732345167799690`**. Existe **candidata DEV posterior**, documentação de referência `docs/qa/ANYTV_2932_FILTER_COMPOSITION_DEV_20261025.md`, Actions **#40215965755 SUCCESS** (unsigned, Gradle e testes), **NÃO assinada/publicada**. Não confundir os dois estados. Antes de mudar código, resolver HEAD exato da branch novamente, pois este arquivo é mais um commit documental.

## 1. Objetivo e limites invioláveis
AnyTV é app Android `com.tclremote.livre.v2` para controle TCL/Android TV e descoberta de filmes/séries TMDB com comprovação da disponibilidade de streaming no Brasil. Preservar controle, pareamento, marcas originais de serviços, pôsteres originais, 3 por linha completos 2:3 com ContentScale.Fit, hero com título legível/sombra, Cinema BR somente por prova factual, favoritos e assistidos independentes, persistência robusta, lotes 24 e recuperação parcial dos filmes antigos. Não inventar streaming, aluguel, sala de cinema, disponibilidade, gênero, trailer ou título. Priorizar pedidos exatos do usuário sobre refatores ou designs improvisados; não modificar telas aprovadas sem necessidade.

**Publicação:** em fases anteriores usuário autorizou explicitamente versões 2.9.26–2.9.31. **Não presumir autorização para uma nova versão 2.9.32/code61**, nem deploy de Worker, D1 ou companion TCL, nem merge PR #3. Usar primeiro candidate unsigned e resultados concretos; só publicar quando houver autorização aplicável e gates de release com assinatura/certificado original e readback HTTPS.

## 2. Cânone e ordem de leitura
- `AGENTS.md`
- `docs/STATUS.md` (nota DEV 2932 no topo)
- `docs/handoffs/CURRENT_HANDOFF.md`
- `docs/handoffs/NEXT_CHAT_FIRST_MESSAGE.md`
- `docs/roadmap/PROJECT_PROGRESS.json`
- **Este handoff** `docs/handoffs/2026-10-25-ANYTV-TRANSICAO-COMPLETA-APOS-2932-DEV.md`
- Handoff anterior integral `docs/handoffs/2026-10-25-ANYTV-TRANSICAO-INTEGRAL-POS-7E4B.md` para história pre-2.9.26
- `docs/qa/ANYTV_2932_FILTER_COMPOSITION_DEV_20261025.md`
- `docs/qa/ANYTV_2931_CODE60_FILTERS_LAYOUT_RELEASE_20261025.md`
- `docs/qa/ANYTV_V2_2.9.25_DEVICE_HOMOLOGATION_PLAN_20261025.md` e demais relatórios vinculados.
- Código UI: `overrides/TclStreamingScreens.kt` e `overrides/TclApprovedHomeScreen.kt`; testes: `scripts/tests/test_streaming_screens_contract.py`, `test_approved_visual_contract.py`, `test_tcl_v22_contract.py`; workflows: `.github/workflows/anytv-v2-candidate-compile.yml`, `release-anytv-phone-v2-new-package.yml`.

## 3. Evolução e decisões visuais consolidadas
- 2.9.25/code54 foi baseline histórica; 2.9.26/code55 mudou `Mostrar mais 24` para `Mostrar mais`, hero e rótulos de listas.
- 2.9.27/code56: capa 2:3 e 3 colunas, controles coração/assistido sobre a arte, estrela amarela/nota sombreada na arte, título card até 2 linhas, F/S+ano+ícones de serviços verificados, busca bibliotecas, barra vermelha antes de sessões.
- 2.9.28/code57: restaurado hero geral da Principal, **retirado TUDO de Cinema da Principal** (Cinema permanece em página própria); nota no **canto superior ESQUERDO** do poster e hero; títulos e espaçamentos normalizados.
- 2.9.29/code58: retirado fundo circular dos ícones e aplicado contraste próprio grafite; Cinema sem hero próprio divergente, usando padrão do mesmo hero principal; fonte e espaços comuns.
- 2.9.30/code59: gênero preservado ao alternar Filmes/Séries e entrar em streaming; IDs movie/tv resolvidos por catálogo, sem fallback silencioso para conteúdo de outro gênero; Cinema BR é categoria especial separada.
- 2.9.31/code60: títulos variam Filmes/Séries/Filmes e séries conforme contexto; Grade/Detalhado recuperado para gêneros; removido subtítulo desnecessário `Android TV` do topo do controle; gênero textual abaixo do título de cards e dentro do hero com fontes discretas; lupa e X nas buscas pessoais. Última OTA oficial.

### Regras de UI preservadas
1. Páginas Principal, Cinema, gênero e streaming usam identidade única — mesma família de fontes, cards, hero, paddings, barra vermelha e lista. Principal tem carrossel geral de destaques, **não** blocos/slide Cinema.
2. Cartões com estrela amarela e nota no canto superior esquerdo, coração/assistido no canto superior direito, sombras grafite sem badge circular, linha de metadados F/S, ano, serviços BR reais, título até 2 linhas e gênero logo abaixo com tipografia secundária. Nenhum corte indevido do pôster.
3. Hero compartilhado: título e dados SOBRE imagem, sombra/gradiente e gênero textual, sem duplicar F/serviços fora da arte.
4. Botões Filmes e Séries devem se comportar como **dois toggles combináveis**, ambos ativos por padrão; desativar um exibe somente o outro; nunca ambos desativados.
5. Gênero é filtro acumulativo e página. Ao entrar em provedor, preserva gênero e mídia; provedor filtra somente títulos com disponibilidade BR verificada. Se gênero não existir naquela mídia, não ampliar para catálogo irrestrito.
6. Busca **auxiliar** apenas em Favoritos, Séries acompanhadas/Em andamento, Filmes e séries assistidos. **Não** mostrar busca auxiliar nas páginas de streaming nem gênero. Busca deve incluir lupa visível, X para limpar e filtrar instantaneamente.
7. Grade/Detalhado: botões em formato de **ícones à esquerda**. Grade com 3 pôsteres/linha. Detalhado: título, segunda linha F/S+ano+streaming+gênero, depois sinopse e no rodapé atalhos **Trailer** e **Mais detalhes** com mesmo modal do pôster, sem inventar link.
8. Filtros de serviços: marcar/desmarcar todos legíveis; selecionar serviços restringe aos serviços marcados com oferta BR comprovada. Filtro não deve fazer seções editoriais da Principal simplesmente sumirem; preservar seção ou estado vazio explicativo.
9. Editorial não mostrar rótulos internos `TMDB`. Popularidade com título aprovado **Filmes e séries mais assistidos**, agregando filmes e séries; blocos escolhidos pelo usuário devem misturar os dois tipos conforme seleção.
10. Anime: filtro específico para anime (animação + idioma original japonês, usando dados reais movie/tv), separado de animação genérica.
11. Nunca confundir CI/screenshot anterior com homologação física da nova OTA.

## 4. Ponto técnico atual após último pedido (22? verificar DATA/HEAD)
O **último pedido ativo** do usuário, ainda não homologado, foi:
1. Remover busca auxiliar em páginas de streaming e gênero (só permanecer nas bibliotecas).
2. Substituir seleção exclusiva Filme/Série por dois filtros combináveis independentes (ambos ativos = misturados, só Filme = filmes, só Série = séries).
3. Aproximar gênero do título dos cards sem grande vão vertical.
4. Legibilidade de Marcar/Desmarcar todos; filtrar de fato provedores selecionados e excluir títulos de serviços desmarcados.
5. Não perder sessões da Principal após aplicar filtro de serviços.
6. Remover rótulos como `Populares TMDB`; usar `Filmes e séries mais assistidos` com ambas as mídias.
7. Sessões personalizadas da Principal também com filmes/séries misturados.
8. Grade/Detalhado como ícones, toda a linha à esquerda.
9. Detalhado: segunda linha F/S, ano, serviços e gênero; ao fim links Trailer e Mais detalhes, com modal idêntico ao clique no poster.
10. Filtro Anime específico.

**Importante sobre código DEV:** o relatório `ANYTV_2932_FILTER_COMPOSITION_DEV_20261025.md` registra que antes de commits recentes parte de Grade/Detalhado, campos do Detalhado, Anime e popularidade mista **já estava na branch DEV**, e os commits `cf489d0`, `cd605f4`, `0c90b22`, `40f7b86`, `1e4e1a2`, `2d81526`, `d88b544`, `3e44795` trataram mídia/filtro, fallback, seções vazias, verificação BR com concorrência de 4, contraste e Cinema na Principal. CI **#40215965755** SUCCESS unsigned. **NÃO concluir que os 10 critérios estão visualmente aprovados em dispositivos**: verificar diffs e execução atual, sobretudo filtro Anime, presença de busca apenas nas bibliotecas, estado dos dois toggles, layout detalhado e dados BR.
Não iniciar novo módulo ou redesenhar telas; primeiro uma matriz de auditoria critério→função→teste→evidência e fechar regressões reais em blocos coerentes.

## 5. QA, integridade e publicação
- Última OTA comprovada: **2.9.31/code60**, commit OTA `481d498`, SHA256 `177b2f422b55858177e661855aea8215119b8a0c0917748de732345167799690`. Não anunciar 2.9.32 sem prova.
- DEV posterior: CI unsigned `40215965755` SUCCESS, **sem assinatura**. Consulte workflow, artefato e HEAD atual; se houver commits posteriores, reler e não atribuir CI antigo ao código novo.
- Worker publicado continua sem rota `movie/{id}` da DEV, salvo prova posterior. Não fazer deploy automático. D1 e companion sem alteração autorizada.
- `docs/roadmap/PROJECT_PROGRESS.json`: **7/11=63%**, M08/M09 IN_QA, M10/M11 BACKLOG, F0–F8 NAO_HOMOLOGADO. Não alterar gates por build.
- Android/TCL físicos não homologados. Plano físico existente deve ser mantido, com adendo para candidatas novas.
- Proteger `updates/android/cert2026/current.apk` (legado) e `updates/android/v2/current.apk` até release aprovada com mesmo pacote/certificado V2 e versionCode maior; exigir hash/apksigner e readback remoto byte a byte.
- PR #3 sem merge; não publicar Worker, mudar D1, companion ou segredos.

## 6. Próximos passos executáveis, sem ciclos de "continue"
1. Resolver o **HEAD exato** da branch DEV via MCP GitHub e PR #3; ler arquivos canônicos acima, mudanças após `3e44795` e QA 2932.
2. Verificar linha a linha os 10 critérios da última solicitação em `TclStreamingScreens.kt` e testes; registrar quais estão implementados, parciais e não cobertos.
3. Corrigir apenas lacunas concretas com commits atômicos; preservar 3 colunas, hero e Cinema BR, favoritos/assistidos e TCL.
4. Rodar contratos, Gradle unsigned, análise de segurança e CI; ler logs em caso de falha; não ajustar teste para ocultar regressão.
5. Produzir relatório QA DEV com SHA, run, resultado e itens sem prova física; atualizar `AGENTS.md`, `docs/STATUS.md`, `CURRENT_HANDOFF.md`, `NEXT_CHAT_FIRST_MESSAGE.md` e roadmap sem elevar gate.
6. Solicitar autorização específica antes de 2.9.32/code61 e qualquer OTA/Worker/D1/companion. Se autorizado, executar release assinada, manter legado, verificar hash e readback.
7. Após publicação autorizada, fazer homologação Android/TCL com prints e passos interativos antes de promover gates.

## 7. Preferências de execução
Responder PT-BR, fazer progresso real por turno sem exigir dezenas de "continue"; consulta GitHub-first, alterações mínimas e coerentes, testes e commits, checkpoints duráveis. NÃO repetir perguntas já respondidas. Não afirmar tarefa concluída sem evidência. Não divulgar segredos. Documento de transição equivale a roteiro, não prova de funcionamento físico.
