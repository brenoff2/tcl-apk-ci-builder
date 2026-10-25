# AnyTV — composição de filtros e layout (candidata DEV pós-2.9.31)

**25/10/2026; esta é uma revisão de desenvolvimento, NÃO uma OTA homologada.**

## Critérios do usuário
- Busca auxiliar só em Favoritos, Acompanhando e Assistidos; não em streaming ou gênero.
- Botões Filmes e Séries são filtros combináveis: ambos selecionados mostram ambos; desmarcar um restringe ao outro, sem permitir tela sem mídia.
- Gêneros sob os títulos ficam imediatamente abaixo, sem altura fixa excessiva.
- Controle Marcar/Desmarcar todos do modal usa texto legível; seleção de serviços restringe catálogo à oferta BR comprovada, sem apresentar serviços desmarcados.
- Seções da Principal não somem ao filtrar serviços, mesmo quando não há resultados com direito BR confirmado; nesse caso informam ausência, sem inventar oferta.
- Sessão editorial Filmes e séries mais assistidos combina fontes movie/popular e tv/popular, com labels sem sufixos internos do TMDB. Listas escolhidas pelo usuário devem tratar filmes e séries.
- Grade e Detalhado por ícones alinhados à esquerda; Detalhado com F/S, ano, serviço e gênero na segunda linha, sinopse e atalho Trailer e mais detalhes, abrindo o mesmo modal do pôster.
- Filtro Anime específico é rota diferenciada, buscando animação com idioma original japonês em filmes e séries.

## Implementações evidenciadas na branch
- `overrides/TclStreamingScreens.kt` já continha ícones Grade/Detalhado, metadados do Detalhado, modal compartilhado, rota Anime, bloco Anime e mistura de populações filmes/séries no editorial.
- `cf489d0`: corrigiu o chip Filmes/Séries em páginas de streaming para ler `homeContentType` (em vez de `media` residual) e impediu consulta de gênero não resolvido, evitando ampliar indevidamente a busca.
- `cd605f4`: teste que exige a mesma fonte de verdade de mídia na Principal e no streaming.
- `0c90b22`: preserva as seções editoriais da Principal quando há filtro de serviços, restringindo títulos às ofertas BR confirmadas; exibe estado vazio quando não há confirmações, em vez de ocultar a seção.
- `40f7b86`: teste de preservação das seções e restrição de ofertas.
- Branch de desenvolvimento `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 draft, sem merge.

## Limites e validação
- Candidato CI unsigned **#40215965755**: [GitHub Actions](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40215965755) **COMPLETED/SUCCESS**; contratos, regressões, Gradle e identidade unsigned verificados. Este artefato não foi assinado nem publicado.
- Nenhum deploy Worker, D1, companion TCL ou OTA de APK realizado nesta etapa. O botão de atualização continuará oferecendo a última versão oficial até nova autorização de publicação. Última OTA confirmada no histórico: **2.9.31/code60**.
- Necessária homologação física no telefone para conferir chips combinados, filtros persistidos, busca auxiliar apenas nas bibliotecas, ícones Grade/Detalhado, contraste, anime e serviços BR.
- Permanecem gates **7/11 (63%)**, M08/M09 IN_QA, F0–F8 NAO_HOMOLOGADO, até prova física.

## Checkpoint pós-CI
- Commits adicionais: `1e4e1a2` (ofertas BR até 4 consultas concorrentes), `2d81526` (contraste do Marcar/Desmarcar todos), `d88b544` (Cinema ausente da Principal também no modo Filmes), `3e44795` (contrato de Cinema alinhado ao requisito); teste final `40215965755` PASS.
- `docs/handoffs/CURRENT_HANDOFF.md` mantém a referência a esta revisão. Nenhuma evidência física foi recebida para estas mudanças.
