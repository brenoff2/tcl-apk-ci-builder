# AnyTV — revisão da Principal, streaming e gêneros (25/10/2026)

## Fonte e segurança
Branch DEV `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 Draft. Base 2.9.32/code61 já publicada, sem confundir com novos commits DEV. Não fazer nova OTA até CI completo e validação de QA físico. Sem alteração de Worker, D1, companion, signer ou updater.

## Correções implementadas
- `7361df1` — hero principal restaurado de 150 dp para 210 dp (só altura). Página de gênero, no modo Filmes e Séries, intercala itens movie/tv em vez de concatenar todos os filmes e depois séries. Botão Mostrar mais consulta e atualiza páginas de todas as mídias elegíveis, usa os IDs de gênero de cada mídia e preserva o filtro Anime especial. Mantém confirmação de ofertas BR ao renderizar.
- `db38b71` — quatro editoriais principais com composição movie/tv: Em alta nesta semana, Novidades, Filmes e séries mais assistidos, Mais bem avaliados. As fontes são TMDB trending, now_playing/on_the_air, popular e top_rated; rotas de Mostrar mais usam ambas as mídias. Nomes dos títulos não implicam medição independente de visualizações.
- `ac359a6` — corrigiu três contratos textuais legados que exigiam nomes de variáveis/implementação antiga. Agora testes cobram os caminhos da paginação dupla e preservação Cinema; sem enfraquecer os invariantes.

## Diagnóstico do defeito mais grave
A tela de gênero juntava duas fontes por `flatMap`, agrupando filmes e depois séries. O controle Mostrar mais usava `selectedGenreKind`, que em modo all corresponde a movie, e `genreMore[genreKey]` consultava só uma chave. Assim, séries não avançavam e a listagem permanecia curta. Quando o usuário selecionava serviços, a filtragem de ofertas BR eliminava mais títulos. Corrigidas a composição e a paginação dual. O carregamento de ofertas confirmadas e a densidade de resultados em dispositivos exigem QA adicional.

## Pontos ainda pendentes
- Em breve: a seção atual só oferece resultados com assinatura BR confirmada; títulos futuros podem não ter oferta publicada, produzindo lista vazia. Ajustar a semântica editorial sem inventar disponibilidade e mantendo gêneros/mídias.
- Página interna de streaming com gênero selecionado: código ainda tem seções de provedor próprias, não equivale integralmente à tela específica de gênero. Exige alinhamento antes de concluir a demanda.
- Seção Por gênero da Principal: dados de ambas as mídias já são obtidos, mas ordenação é concatenação e ainda precisa intercalar e testar filtros.
- Seleção de streaming restringe títulos à oferta BR confirmada; mesmo após paginação, alguns gêneros podem ter menos itens verdadeiramente disponíveis.

## CI
- Run #40222227645: FAILURE nos testes textuais antigos (3 asserts que exigiam estrutura anterior), não foi falha de compilação Kotlin demonstrada; testes foram corrigidos com assertivas do novo comportamento.
- Execução posterior #40222289070 iniciada no SHA `db38b71`, resultado final pendente na primeira consulta; commit de teste `ac359a6` posterior requer CI específico.
- Nenhuma aprovação física Android/TCL. Progresso 7/11, F0–F8 NAO_HOMOLOGADO.
