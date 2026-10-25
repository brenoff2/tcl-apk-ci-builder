# AnyTV — correções pós-2.9.31, ainda em desenvolvimento

**Estado verificável em 25/10/2026:** código registrado na branch DEV `feat/anytv-p0-device-proof-verifier-20261020`; **não publicado como nova OTA**. A versão oficial anterior continua **2.9.31/code60**. PR #3 permanece draft/sem merge. Não alterar Worker, D1, companion TCL ou APK oficial sem passar nos gates.

## Pedido e implementação
- **Busca auxiliar:** retirada das páginas de provedor e gênero. Mantida exclusivamente em Favoritos/Minha Lista, Séries acompanhadas e Assistidos, com lupa e X.
- **Filmes/Séries:** ambos ativos quando `homeContentType=all`; desativar Filmes deixa TV, desativar Séries deixa filmes, e reativar retorna ao conjunto. Impedido estado vazio.
- **Gêneros:** eliminada altura fixa sob os títulos para reduzir lacuna visual.
- **Filtro de assinaturas:** botões Marcar/Desmarcar todos receberam contraste. As seções editoriais não devem sumir; o filtro de provedores exige confirmação de oferta de assinatura BR por título. Página específica de gênero e detalhe respeita a seleção, inclusive filtros de streamings.
- **Sessões:** substituídos títulos TMDB por nomes legíveis; Filmes e séries mais assistidos e Mais bem avaliados combinam resultados de filmes e séries; paginação das duas fontes preservada. **A origem é ranking TMDB de popularidade/avaliação, e não medição auditada de visualizações.**
- **Gêneros das seções selecionadas na Principal:** as listas combinam movie/tv com os IDs TMDB de cada mídia.
- **Visualização:** Grade/Detalhado agora por ícones GridView/ViewList.
- **Detalhado:** segunda linha única F/S · ano · streamings BR carregados · gêneros; sinopse; atalho 'Trailer e mais detalhes' abre o mesmo pop-up da imagem.
- **Anime:** chip específico, id interno -2; consulta `discover/movie` e `discover/tv` com `with_genres=16&with_original_language=ja`, somada aos provedores BR quando selecionados. Critério explícito: **animação de idioma original japonês**, não apenas toda a categoria Animação. Não inferir que 100% são anime sem marcação semântica adicional.

## Evidências e limites
- Código Kotlin: `overrides/TclStreamingScreens.kt`; testes: `scripts/tests/test_streaming_screens_contract.py`.
- Última execução unsigned de QA: [#40212008037](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40212008037); testes UI/segurança passaram na etapa inicial, compilação Gradle permanecia em andamento na última consulta.
- Execução anterior [#40211829027](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40211829027) falhou ao resolver dependências Kotlin/Gradle nos repositórios externos; não comprovou erro de compilação Kotlin.
- **Não declarar build final/assinatura/OTA nova ou QA físico** sem nova comprovação. Não promover 7/11 = 63%, M08/M09 IN_QA, F0–F8 NAO_HOMOLOGADO.

## Próximo gate
Verificar a conclusão da última compilação; se falhar, ler logs e corrigir apenas causa comprovada; atualizar contratos conforme especificações aprovadas; obter CI completo verde, comparar regressões de navegação/filtros, e somente então preparar nova versão assinada pelo fluxo protegido. Preservar OTA 2.9.31 até haver aprovação de publicação e readback byte a byte.
