# Cartographer v2 — modo isolado, status e limites

**Status em 15/10/2026: CARTOGRAPHER_BLOCKED neste executor.** O checkout de execucao nao tem Bun e nao tem resolucao de DNS externo; a CLI nao foi instalada nem executada. O mapa inicial em `CODEBASE_MAP.md` foi revisado manualmente lendo codigo e estrutura reais no GitHub, **nao e um grafo produzido por Cartographer**.

Fonte oficial: https://github.com/kingbootoshi/cartographer
Revisao upstream avaliada para planejamento: `a62d16981b6aa1f5f6ef56701c49b81a16a8e30a`. Verificar upstream SHA e licenca antes de instalar.

A implementacao futura deve executar `scripts/dev_context/cartographer_offline.sh CARTOGRAPHER_CHECKOUT ANYTV_CHECKOUT OUTPUT_DIR` em um executor isolado, com Bun, checkout limpo sem segredos, source SHA fixado e diretorio de saida externo ao produto. O script recusa saida interna ao repo, argumentos incompletos e ausencia de Bun; indexa e valida a atualidade com `verify --fresh`.

Nao rodar na Contabo de producao sem plano operacional proprio. Nao importar `.cartographer/graph.sqlite` ou dados brutos ao GitHub por padrao, nem disponibilizar diretórios com chaves/certificados privados para indexacao. Nunca expor `mcp` stdio como shell remoto. Uma integracao interativa com ChatGPT Web exige outra etapa de arquitetura e seguranca.

**Fallback aceito pelo plano:** um mapa manual verificado no GitHub, com `source_commit` explicito, cuja atualidade deve ser validada contra a branch antes de trabalhar. Marcar `CARTOGRAPHER_BLOCKED` ate um indice real ter sido produzido, verificado e revisado.
