# AnyTV — Em breve e estabilidade visual dos cartões (DEV)

Data: 25/10/2026. Branch `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 draft, sem merge. Última versão publicada verificada: 2.9.32/code61, OTA `e7cd7d1`. Mudanças abaixo ainda NÃO publicadas como atualização.

## Defeito e causa
- Cinema exibiu Em breve usando `movie/upcoming` e data futura confirmada pelo TMDB, independentemente de ofertas.
- A seção geral também buscava `movie/upcoming` e `discover/tv`, mas descartava **todos** os títulos sem um provedor de assinatura BR já confirmado. Lançamentos futuros muitas vezes não possuem essa informação; a seção podia ficar vazia. Ausência de oferta não é ausência de lançamento.
- A altura da faixa mudava porque os nomes dos títulos ocupavam uma ou duas linhas, e o gênero era o último elemento variável do cartão.

## Alterações implementadas
- `5b3f97a`: `discoveryUpcomingStreamingPage` intercala filmes e séries com data futura real, sem condicionar a listagem geral à existência antecipada de assinatura BR. Nenhum catálogo, filme ou serviço inventado. O filtro de streamings selecionados continua dependendo de ofertas BR verificadas pelo mecanismo já existente; quando não confirmadas, o filtro mantém estado vazio.
- `5b3f97a`: cartão compartilhado exibe primeiro o gênero (uma linha reservada), em seguida F/S, ano e serviços, e título por último com **duas linhas reservadas**; poster 2:3/ícones/estrela preservados. A altura dos títulos deixa de variar com uma ou duas linhas reais.
- `d0dc3c7`: testes específicos para intercalar as fontes futuras e exigir layout de cartão com gênero primeiro e título em duas linhas.
- `e4e515d`: atualização pontual de contrato obsoleto após unificação de Em alta.
- `3e461ffd` e `e91f988`: seção editorial renomeada para **Em breve · Filmes e séries**, evitando sugerir que toda novidade tenha oferta de streaming confirmada. Contratos textuais alinhados.

## Verificação / gates
- Contratos de fonte adicionados; CI para o commit da mudança foi disparado automaticamente, mas o resultado FINAL ainda não havia sido obtido no checkpoint.
- Candidato unsigned; não equivale a APK assinado, OTA ou homologação física. Não promover 7/11 (63%), M08/M09 nem F0-F8.
- Nenhum Worker, D1, companion, certificado ou canal OTA alterado. Não iniciar release de produção enquanto CI não estiver verde.

## Limitações de escopo ainda não homologadas
- Alterações de paginação dupla por gênero e editoriais mistos da rodada anterior exigem compilação integral atual e QA física.
- Ao selecionar assinaturas, a ausência de ofertas BR verificadas para um lançamento futuro pode deixar a lista filtrada vazia; isso é comportamento consciente e honesto, diferente de ocultá-lo na descoberta geral.
