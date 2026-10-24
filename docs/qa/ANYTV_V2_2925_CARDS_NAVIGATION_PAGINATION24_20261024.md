> **READBACK FINAL DA RODADA (24/10/2026):** [Actions #40101131043](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40101131043) **SUCCESS** para código funcional `f3b021519b8a42cf5472bdd5982fc06254b71f20`: (1) contratos de UI/links/segurança SUCCESS, (2) Gradle assembleRelease SUCCESS, (3) identidade do APK V2 unsigned SUCCESS. [Device-proof #40101131025](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40101131025) SUCCESS. O workflow de auditoria continua produzindo **code53 unsigned**, não uma release 2.9.25/code54. **Sem assinatura, OTA, atualização do companion, teste físico ou merge.** Última OTA assinada permanece 2.9.24/code53. Progresso 7/11 (63%), gates F0–F8 NAO_HOMOLOGADO.

# AnyTV 2.9.25 candidata — cartões, "Ver mais" e 24 títulos por lote

**Data:** 2026-10-24. **Escopo:** somente cliente Android AnyTV V2 na branch DEV do PR #3. **Publicação:** nenhuma.

## Resultado codificado

1. **Cartões internos:** pôster da grade de gêneros corrigido de 135dp para **158dp**, mesma altura dos cartões da capa. Pôster na lista de gênero ampliado de 92×132dp para **116×158dp**. Cards usam 3 linhas de título/42dp já existentes. A renderização física permanece sem homologação.
2. **Ver mais:** removido o estado `expandedHomeShelf` e a expansão vertical em três colunas abaixo da seção. Para prateleiras `Por gênero:`, o botão localiza o ID de gênero no catálogo e seleciona a **página de gênero existente** (com hero, Em breve, lista/grade e `Mostrar mais 24`). Para Cinema, abre a página Cinema existente. As demais prateleiras, que não têm gênero TMDB único (destaques de serviço, tendências, favoritos, estreias), abrem uma **página interna exclusiva da seção** com botão Voltar; a expansão na capa foi removida.
3. **24 títulos:** `ANYTV_LIST_BATCH = 24` controla lotes das prateleiras, resultados por gênero, Cinema brasileiro e internacional, provedores, busca, prévias editoriais, recomendações e seção Em breve (quando a fonte disponibiliza esse número).
4. **TMDB retorna páginas menores:** `StreamingCatalog.catalogPage24` reúne páginas-fonte e só depois filtra/deduplica e recorta 24 títulos. Ao solicitar o próximo lote, reconstrói a sequência a partir das páginas-fonte cacheadas, preservando ordem e evitando perder títulos entre limites de 20→24 mesmo quando filmes futuros são filtrados. A janela máxima permitida pelo Worker é de 50 páginas da fonte; a última página lógica parcial é 42. Não cria novos endpoints, quotas ou tabelas.
5. **Minha lista e Assistidos:** registros locais preservados inclusive se TMDB não informar a data do título. A capa apresenta até 24 por seção; a página da biblioteca exibe `Mostrar mais 24` para o restante, sem apagar registros.

## Verificações necessárias

- CI candidato Android: `scripts/tests/test_streaming_screens_contract.py`, regressões visuais, Gradle `assembleRelease`, identidade unsigned.
- Teste no aparelho: modo grade/lista por gênero, cartões sem corte, clique em Ver mais e retorno, página Cinema, prateleiras de serviço, duas ou mais cargas de 24 sem repetição e última página parcial.
- Testar `Minha lista` com mais de 24 favoritos/assistidos e títulos sem data (sem perda de registros).
- Testar desempenho e carga de rede em filtros que removem muitos títulos, já que o agrupamento 24 solicita páginas-fonte adicionais.
- Companion TCL e comunicação/reprodução real: **NÃO_TESTADO** nesta correção; nenhuma alteração do APK TV.

## Estados e proteção

- Branch DEV `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 **draft**, sem merge.
- Última OTA V2 assinada permanece **2.9.24/code53**. O workflow de auditoria ainda compila código novo com versionCode53 **unsigned**; isso não equivale a uma release 2.9.25/code54.
- **Sem** alteração de assinatura, Worker Cloudflare, D1, código de ativação, channel OTA, certificado ou companion.
- P0 formal 7/11 (63%), gates F0–F8 não homologados; testes físicos pendentes.

**Upstream:** `harimoradiya/androidtvremote` fixado em `16c68f0bb9d23f929ee8240f105fdb9000262d85`; padrão de janela-fonte TMDB 20 encontrado em `ShukeBta/MediaStationGo` (`internal/service/discover.go`). Código existente do AnyTV foi adaptado sem adicionar dependência.
