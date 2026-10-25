# AnyTV — auditoria incremental pós-transição (25/10/2026)

## Identidade e proveniência
- Branch DEV: `feat/anytv-p0-device-proof-verifier-20261020`; PR #3 DRAFT, sem merge.
- HEAD de entrada: `8e1e3d51b5518b95d3f7c87057e888fbd9a523db`.
- Correção de código: `1e835dc733c0e9c842cdd1186da00937f88ef3e2`.
- Última OTA assinada: 2.9.31/code60, commit `481d498`; esta auditoria não publica OTA.
- CI anterior: #40215965755 SUCCESS no SHA funcional `3e447952`. Não atribuir esse resultado ao commit novo.
- CI novo disparado automaticamente: #40218890248 (candidate unsigned) e #40218890258 (device proof), ambos IN_PROGRESS na consulta. Resultado FINAL ainda NÃO TESTADO.

## Auditoria dos dez requisitos (leitura estática do código e dos QA anteriores)
1. Busca auxiliar apenas em bibliotecas: **PARCIAL / verificar UI real**. QA pós-2.9.31 descreve remoção das buscas auxiliares em gêneros/provedores, mas ainda existe um ícone de busca global no cabeçalho compartilhado (`TclStreamingScreens.kt`, aproximadamente linha 2296), cujo escopo exato deve ser diferenciado da busca auxiliar antes de remover.
2. Filmes/Séries combináveis: **IMPLEMENTADO NO CÓDIGO / QA físico pendente**. Estado `homeContentType=all` e filtros de mídia associados; contratos anteriores passaram.
3. Gênero próximo ao título: **IMPLEMENTADO NO CÓDIGO / QA visual pendente**. `AnyTvPosterCard` renderiza `genreLabel` imediatamente após texto do título.
4. Seleção de streamings/contraste: **IMPLEMENTADO PARCIALMENTE / QA BR fim-a-fim pendente**. Modal tem Marcar/Desmarcar todos e consulta de ofertas BR; verificar cenários sem oferta e cache.
5. Seções da Principal preservadas ao filtrar: **IMPLEMENTADO EM DEV / QA visual pendente**. Commit `0c90b22` e regressão associada; confirmar sem falsos positivos de oferta.
6. Popularidade mista e rótulos: **IMPLEMENTADO EM DEV / sem prova visual**. Código combina `movie/popular` e `tv/popular` em `Filmes e séries mais assistidos`. Observação factual: a fonte mede popularidade TMDB, não audiência comprovada.
7. Seções personalizadas mistas: **PARCIAL / revisar paginação e IDs de gênero movie/tv**. Fonte registra composição por mídias, mas cenários de paginação/seleção precisam ser confirmados.
8. Ícones Grade/Detalhado à esquerda: **IMPLEMENTADO NO CÓDIGO / QA visual pendente**. Ícones `GridView` e `ViewList` na linha esquerda.
9. Detalhado: **IMPLEMENTADO NO CÓDIGO / QA visual pendente**. Metadados, sinopse e ação `Trailer e mais detalhes` reusando `details = title`.
10. Anime separado: **IMPLEMENTADO POR HEURÍSTICA / QA semântica pendente**. Animação + idioma original japonês (`with_genres=16&with_original_language=ja`), não garantia universal de classificação anime.

## Regressão real encontrada e corrigida
O bloco de exclusão `ANYTV_CINEMA_SHELF` na Principal exigia `homeContentType == "all"`, contrariando a regra de não exibir Cinema na Principal quando somente Filmes está ativo. Foi removida APENAS essa condição, preservando a página exclusiva de Cinema, outras seções, streamings, TCL e persistência. Arquivo: `overrides/TclStreamingScreens.kt`. Código confirmado por readback no commit `1e835dc`.

## Gates e limites
- Build unsigned do commit alterado: **EM ANDAMENTO na consulta**; até resultado final, NÃO TESTADO.
- Assinatura, instalação no celular, homologação TCL e OTA: **NÃO EXECUTADOS**.
- Worker/D1/companion/segredos/updater: **NÃO ALTERADOS**.
- Progresso: **7/11 (63%)**. M08/M09 IN_QA; M10/M11 BACKLOG; F0–F8 NAO_HOMOLOGADO.
- Antes de liberar candidata 2.9.32/code61: confirmar CI do novo SHA, regressões das dez exigências e aprovação explícita para assinatura/publicação.
