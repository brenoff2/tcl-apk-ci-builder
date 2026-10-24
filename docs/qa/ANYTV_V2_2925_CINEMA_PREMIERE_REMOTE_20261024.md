# AnyTV V2 2.9.25 — nova rodada de UI Cinema, Em breve e controle (24/10/2026)

**Estado:** mudanças em DEV/PR #3, sem assinatura/OTA/homologação de dispositivo. Registro de implementação, não aceite final.

## Pedido e intervenção no código

1. **Cabeçalho Streaming** — removida a marca horizontal do topo do `Dialog` em `overrides/TclStreamingScreens.kt`. Marca, splash e launcher aprovados permanecem intactos em seus próprios componentes.
2. **Cinema isolado** — catálogo geral usa apenas lançados; a única prateleira de Cinema da capa consome `movie/now_playing?region=BR`, não ausência de ofertas. Filtro superior Cinema e a paginação dedicada seguem preservados. Filmes com indicador Cinema não são agrupados em prateleiras genéricas.
3. **Cinema na capa** — bloco próprio com `AnyTvCinemaHero`, `Cinema pelo mundo · seleção internacional`, `Em breve · Cinema` e, abaixo, a prateleira de filmes em cartaz no Brasil. O destaque do filme atual mostra informação Cinema no banner de entrada somente quando o ID foi encontrado na lista BR consultada.
4. **Cinema pelo mundo** — prévia editorial separada da lista brasileira, obtida da rota TMDB `movie/now_playing` sem `region=BR` (região padrão do TMDB). **Não é levantamento exaustivo de todos os países**. Não exibir cinema/sessões inventadas.
5. **Em breve** — novo coletor `discoveryUpcomingStreaming` mistura filmes e séries com data futura, mas exige uma oferta de assinatura `flatrate` confirmada no Brasil no catálogo do título. Filme apenas em `movie/upcoming` não basta para alegar lançamento no streaming. Se a fonte não confirmar, a prateleira poderá ficar vazia; o TMDB não é agenda comercial de estreias das plataformas.
6. **Gêneros** — logo depois do hero principal da tela filtrada aparece um carrossel secundário horizontal de uma linha, `Em breve · Streaming`, filtrado pelo gênero (incluindo nomes correspondentes para TV) e pelo tipo de conteúdo. No filtro Cinema aparecem `Em breve · Cinema` e `Cinema pelo mundo` em linhas únicas antes da lista brasileira paginada. A lista preserva `Mostrar mais 20` e lista/grade.
7. **Lançados x futuros** — comparador `anyTvReleased/anyTvUpcoming` com datas ISO na listagem/hero, descoberta por gênero, busca, provedor e recomendados. Itens sem data válida não são afirmados como lançados; cinema BR usa sua fonte regional própria. Favoritos/assistidos já salvos não são apagados.
8. **Volume e canal** — ícones de cabeçalho 23dp → 17dp, controles +/- 32/31dp → 24dp, espaçamento ampliado e labels menores em `TclApprovedHomeScreen.kt`. Removido `TclLiquidFill`: `sessionState.volumeFraction` não é medição comprovada do volume máximo/atual da TCL. Os comandos físicos Volume+/Volume-/Mute/Canal+/Canal- foram mantidos. Nenhuma porcentagem 0–100% é exibida sem telemetria fidedigna.

## Escopo, segurança e verificação

- Branch: `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 continua **draft**.
- Modificados: `overrides/TclStreamingScreens.kt`, `overrides/TclApprovedHomeScreen.kt`, `scripts/tests/test_streaming_screens_contract.py`, `scripts/tests/test_approved_visual_contract.py`.
- Nenhuma alteração em segredo, pacote, certificado, dados locais, Worker de produção, D1, updater, branch OTA, workflow oficial protegido, companion instalado ou merge.
- Workflows de candidato continuam compilando/rotulando o unsigned como **2.9.24/code53**; isto **não é release 2.9.25/code54**. Alterar workflow oficial code54 e assinar/publicar ainda depende de liberação legítima (tentativa anterior bloqueada pela política). Não contornar.
- Testes de contrato foram adaptados para separação das fontes, data de estreia, seleção Cinema, cabeçalho sem marca e rocker sem nível fictício. Resultado de CI desta rodada deve ser acrescentado por readback, sem inferir PASS de commit.
- Verificação no celular e TCL real: **NÃO_TESTADO**. A atualização e identificação real do companion 2.5/code16 permanecem pendentes.
- Progresso formal: **7/11 (63%)**, gates F0–F8 **NAO_HOMOLOGADO**, até provas físicas e critérios reais.

## Fontes técnicas usadas

- TMDB Discover Movie: https://developer.themoviedb.org/reference/discover-movie
- TMDB Discover TV: https://developer.themoviedb.org/reference/discover-tv
- Candidato e canônico anterior: `docs/handoffs/2026-10-24-ANYTV-TRANSICAO-INTEGRAL-2924-2925.md`
- QA anterior: `docs/qa/ANYTV_V2_2925_LINKS_CINEMA_20261024.md`
