# AnyTV — Discovery Home V3: especificação de produto e execução

**Estado:** DESIGN_REVIEW — proposta derivada do pedido de 2026-10-21; não implantada nem homologada. **Escopo:** substituir a navegação de catálogo iniciada por provedor pela **descoberta global**, preservando remote, LAN, Bluetooth, catálogo atual, ativação e abertura de títulos.

## 1. Modelo mental e arquitetura de informação

**Entrada Streaming = Descobrir**, não escolha de um streaming. Um filtro opcional de plataformas altera o acervo apresentado, mas não condiciona a pesquisa a uma plataforma. Serviços instalados na TV têm páginas próprias acessíveis pela aba **Plataformas**.

Navegação superior: **Descobrir | Filmes | Séries | Plataformas | Minha lista**. Busca unificada visível em todas as abas. Filtros em chips: **Todos** (padrão), Netflix, Prime Video, Disney+, Max, Apple TV etc. Só marcar **Instalado na TV** quando o AnyTV Connect confirmar inventário de apps. Não inferir assinatura ativa a partir da instalação.

**Início Descobrir**:
1. Hero editorial dinâmico: imagem de fundo, título, sinopse curta, ano, gêneros, **nota TMDB com votos**, indicação da disponibilidade BR e ações **Detalhes**, **Ver trailer** e **Onde assistir**. Escolha baseada em fonte real, nunca em curadoria inventada.
2. Carrosséis horizontais: **Em alta nesta semana** (trending), **Estreias recentes** (releases/datas reais), **Populares** (popularidade, NÃO afirmar visualizações), **Mais bem avaliados** (nota + volume mínimo de votos), **Para você descobrir** (recomendações por títulos vistos/favoritos se houver sinal local; senão rotular **Sugestões para explorar**, sem falsa personalização), **Séries em destaque**, **Por plataforma**.
3. Entradas de ação rápidas: **Surpreenda-me**, filtros de gêneros, nota mínima, ano e tipo.
4. Estados vazios e offline: cache recente com data da atualização ou mensagem clara; nunca fabricar posições/títulos.

**Detalhe de filme/série**: backdrop e poster, título, ano, classificação etária quando houver fonte, nota/total de votos, resumo, gêneros, duração se disponível, elenco quando autorizado pela fonte, **Trailer oficial** (idioma pt-BR, fallback en; só se vídeo existente), **Onde assistir no Brasil** distinguindo assinatura, aluguel, compra e disponibilidade desconhecida; seleção de aplicativo confirmado; seções **Semelhantes** e **Você também pode gostar** (similar/recommendations). Ação **Abrir na TV** reutiliza exatamente `AnyTvPersonalLinks.resolve` e `TclTvAppApi.openTitle/searchTitle`.

**Página da plataforma**: nome e ícone do aplicativo TV real, botão **Abrir na TV**, destaques recentes, populares, bem avaliados e filmes/séries do catálogo confirmado no Brasil; filtros adicionais. Não apresentar uma plataforma como disponível quando não estiver instalada; distinguir disponibilidade de catálogo e aplicativo.

## 2. Fonte dos dados e semântica

- TMDB: trending day/week, movie now_playing/upcoming (datas conforme região), tv on_the_air/airing_today, discover ordenado por popularity.desc e vote_average.desc com `vote_count.gte` mínimo; search multi ou movie/tv; details, videos, similar, recommendations, watch/providers.
- `vote_average` e `vote_count` devem aparecer como **Nota TMDB**, não IMDb. Arredondar a 1 casa, ocultar avaliação ausente; não comparar séries e filmes sem explicitar fonte.
- `popular` e `trending` são sinais TMDB, **não** dados reais de “mais assistidos”. Rotular **Populares no TMDB** e **Em alta**, a menos que uma fonte licenciada forneça métricas reais de audiência.
- Trailer: TMDB retorna metadados (site/key/type/official), não hospeda vídeo; abrir URL segura para origem permitida (YouTube oficial, quando confirmada) por Intent seguro ou player compatível, sem autoplay, download ou bypass.
- Disponibilidade via TMDB/JustWatch região **BR**, distinta de app instalado; o provedor pode ter janelas e planos diferentes. Nunca afirmar acesso à assinatura pessoal só pelo catálogo.
- Personalização: favoritos e histórico local **com consentimento**, evitar transmissão de hábitos ao Worker até existir contrato de privacidade. Sem histórico, seção de sugestões editoriais genéricas.

**APIs:** o backend já oferece `GET /v1/catalog/{route}` com allowlist restrita. Antes de lançar UI: expandir rotas com validação estrita (trending, lists, videos, related, ratings/metadata e consulta de disponibilidade), limite de page, media, language, region, provider e `sort_by`; aplicar cache/cota/rate-limit; manter token TMDB e credenciais apenas no Worker. Não abrir proxy genérico. Nenhuma mudança no endpoint autenticado `/v1/resolve`.

## 3. Contrato visual Compose

Layouts responsivos no celular: hero com proporção panorâmica e gradiente/overlay, sessão de carrosséis, cartões verticais poster 2:3, nota legível, rótulo do provedor apenas se confirmado; loading placeholders, vazio/erro com retry; detalhes em sheet/página full-screen. Sem dependência de telas hard-coded de Netflix etc. Acessibilidade: textos pt-BR, contraste, fontes escaláveis, sem trailers automáticos, navegação por leitor de tela e toques grandes. Dark mode coerente com o AnyTV atual.

## 4. Entregas incrementais e gates

| ID | Entrega | Aceitação/evidência |
| --- | --- | --- |
| D01 | Contrato UI e taxonomy de seções | revisão em GitHub; sem aprovação presumida |
| D02 | Worker catálogo global com allowlist, testes e cache | CI de contrato, fontes reais e auditoria de cotas; zero regressão da rota atual |
| D03 | Compose início Descobrir e filtros independentes de plataforma | build, teste de 3 listas e busca global; não exigir seleção de app |
| D04 | Detalhes: nota, trailer oficial, relacionados/recomendações | filme e série reais; trailers ausentes tratados, link validado |
| D05 | Página individual por streaming com disponibilidade BR | provider no catálogo ≠ instalado; lista real da TV |
| D06 | Integração e QA físico | abrir título na TCL, não regredir AnyTV Connect, remoto, Bluetooth, OTA e favoritos |
| D07 | Release assinada e OTA | **novo versionCode**, mesma identidade cert2026, SHA e instalação confirmados |

**Ordem:** D02 + D03 + D04 em PR sem deploy automático → D05 → D06 → D07. Critérios de bloqueio: falta de fonte verificável, indisponibilidade do trailer, API quotas, falha de segurança ou regressão. Publicar só com aprovações aplicáveis. A tela anterior permanece instalada até release testada.

## 5. Compatibilidade e localização canônica

- Tela atual: `overrides/TclStreamingScreens.kt`; catálogo usa `StreamingCatalog.discover` com `selected` provider obrigatório e abre títulos pelo fluxo preservado.
- Backend: `anytv-resolver/src/worker.mjs` e seus testes; credenciais no Cloudflare.
- Fluxo de construção: `scripts/prepare_tcl.py` copia os overrides; não inventar uma segunda árvore de fontes.
- Estado atual: **nenhum layout novo aplicado ao APK**. Este documento estabelece o contrato e o menor conjunto de alterações técnicas; evidência de implantação depende de commit, CI, deploy controlado e verificação física.

## 6. Fontes upstream verificadas em 2026-10-21

- https://developer.themoviedb.org/docs/popularity-and-trending — trending ≠ popularidade nem visualizações.
- https://developer.themoviedb.org/reference/discover-movie — avaliação, popularidade, datas, poster.
- https://developer.themoviedb.org/reference/tv-series-videos — metadados de trailer; vídeos são de terceiros.
