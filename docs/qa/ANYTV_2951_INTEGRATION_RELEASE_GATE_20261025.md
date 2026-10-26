## Verificacao Trakt/Watchmode e CI — 25/10/2026
- Cloudflare Worker `anytv-link-resolver-pilot`: leitura via API oficial confirmou secrets `TRAKT_CLIENT_ID`, `TMDB_READ_TOKEN`, `ANYTV_MOVIE_API_KEY`, autenticacao `ANYTV_RESOLVER_AUTH_MODE=device` e flag de ativacao device; segredo Trakt CONFIGURADO mas endpoint PUBLICADO/LIVE NAO_COMPROVADO. Não registrar valor da credencial nos arquivos.
- Foi adicionado ao workflow isolado `.github/workflows/anytv-p0-device-proof-ci.yml` a verificacao de sintaxe e execucao dos testes `src/watchmode.test.mjs`, commit `ac7d1e8b5e2234d99a09b06136b6c1b165c0711a`, blob `15aa103bba6428393290be5cf7f6afa7f9830027`. Testes e CI ainda aguardam resultado verificavel nesta etapa.
- Watchmode: chave nao confirmada no Worker, `ANYTV_WATCHMODE_ENABLED` nao configurada; adaptador apenas DEV e sem prova de HTTP real. Nao realizar deploy, nao atribuir status ATIVO ou compilar/publicar APK enquanto os gates de integracao e curadoria estiverem abertos.

## Watchmode DEV — adapter implementado, nao ativado (25/10/2026)
- Codigo novo `anytv-resolver/src/watchmode.mjs` (commit `eff12cbe8206c0dfa1fcf9de4e901b0266594598`); roteamento em `src/worker.mjs` (commit `0395ab417947a74713f9bfd53a6a1c7978b80886`), endpoint `GET /v1/catalog/watchmode/{movie|tv}/{tmdbId}/sources`; testes `src/watchmode.test.mjs` (commit `2c9d3979c1fadb1e91c12d776c735375ab7f54ed`) adicionados a `npm test` (commit `8e58fd2e520a5ce45a23e45dbc4bbaa98d176bd9`).
- Gate de seguranca: `ANYTV_WATCHMODE_ENABLED=true`, `WATCHMODE_API_KEY` em segredo Worker, modo `ANYTV_RESOLVER_AUTH_MODE=device` e prova de dispositivo; devolve apenas ofertas BR do tipo `sub`, sem links de aluguel/compra. Nao existe chave registrada nesta entrega e nenhum deploy foi executado.
- **Estado: IMPLEMENTADO_EM_DEV, TESTES_ESCRITOS_NAO_EXECUTADOS, DEPLOY_NAO_EXECUTADO, INTEGRACAO_LIVE_NAO_VERIFICADA, APK_NAO_GERADO.** Antes de liberar: verificar contrato de IDs TMDB no endpoint sources com API real, configurar credencial/país, aplicar quota duravel de 2500 créditos mensais e politica de cache 30 dias, validar atribuicao na UI, compatibilidade com cliente Android e manter gate Trakt/QA.
- Nenhuma alteracao em signer, build-request, OTA, chave publica ou fluxo comercial.

## Decisão aprovada — Watchmode Developer não comercial (25/10/2026)
- O usuário AUTORIZOU EXPRESSAMENTE a integração Watchmode para uso **não comercial**, com plano Developer gratuito; não é apenas possibilidade futura. Integrar ao AnyTV em DEV, priorizando disponibilidade brasileira e dados de catálogo elegíveis ao plano, sem pagamento nem deploy automático.
- Respeitar limites oficiais: 2.500 créditos/mês, no máximo três países escolhidos, atribuição visível Watchmode quando seus dados forem exibidos, cache gratuito renovado/excluído em até 30 dias; sem presumir deep links móveis ou endpoints premium. Não copiar ou distribuir imagens de terceiros por presunção de licença.
- Guardar API key APENAS no Cloudflare Worker como segredo; nunca no APK, GitHub ou logs. Requer emissão/configuração legítima da chave e HTTP live para declarar ATIVO; enquanto isso, INTEGRACAO_APROVADA, ATIVACAO_PENDENTE.
- Se o projeto virar comercial, antes de uso comercial decidir entre contratar plano com direitos adequados ou desabilitar/remover Watchmode. Não contratar nenhum plano sem autorização específica.
- Trakt continua previsto e obrigatório para o gate da próxima APK. Preservar signer/OTA e não gerar release antes da homologação exigida.
- Fonte contratual verificada: https://api.watchmode.com/ e https://api.watchmode.com/tc .

# AnyTV — Gate da próxima APK (pós 2.9.50) — 25/10/2026

Status: BLOQUEADA PARA RELEASE. Não alterar OTA/assinatura ou gerar APK de distribuição até cumprir gates.

## Escopo exigido pelo usuário
Concluir pendências editoriais verificáveis e **ativar, testar e homologar Trakt e Watchmode** antes de declarar uma próxima APK pronta. O objetivo é integração real, não apenas código opcional nem testes de presença textual.

## Evidência inicial
- Fonte Android: `overrides/TclStreamingScreens.kt`, motor `anyTvCurate` e seleção de títulos; desde 2.9.50 alterações DEV filtram assistidos e ampliam fontes de temporadas.
- Worker: `anytv-resolver/src/worker.mjs` exporta `traktSignalsRequest` em `/v1/catalog/signals/trakt/{movie|tv}`, retornando 503 `trakt_not_configured` se `TRAKT_CLIENT_ID` não estiver presente. A existência do código não comprova deploy nem credencial no Worker live.
- Busca do código do Worker não encontrou integração Watchmode (`watchmode`/`WATCHMODE`), portanto NAO_INTEGRADO.
- Trakt: https://developer.trakt.tv/docs/api-use-policy permite Standard gratuito inclusive comercial, sujeita a atribuição, restrições de coleta e rate limits; https://developer.trakt.tv/docs/rate-limiting documenta 429/Retry-After.
- Watchmode: https://api.watchmode.com/ anuncia Developer gratuito apenas não-comercial; Startup comercial US$349/mês (preço consultado em 25/10/2026). **Nenhum gasto/contratação aprovado.**

## Gates obrigatórios, não substituíveis por suposições
1. Verificar SHA HEAD, PR #3 e última OTA live com readback; preservar `com.tclremote.livre.v2`, signer, `updates/android/cert2026/current.apk`.
2. Curadoria: amostra REAL, com ID, nome, mídia, datas, votos, nota, gênero, provedor e tipo de assinatura BR; Principal + pelo menos três streamings, contagens até 50 e duplicação/recência; corrigir problemas encontrados e regressões.
3. Trakt: registrar aplicativo legítimo, configurar Client ID como segredo somente no Worker, verificar termos/atribuição, testar 200 real nos dois endpoints, degradação sem sinal, cache e 429. Deploy condicionado à autorização operacional aplicável.
4. Watchmode: integração APROVADA no plano Developer não comercial, sem custo; obter credencial legítima, confirmar Brasil entre até 3 países, atribuição, cache <= 30 dias e orçamento de 2500 créditos/mês; implementar backend sem chave no APK, testes offline e HTTP real. Em eventual uso comercial, contratar licença adequada com autorização expressa OU desabilitar/remover Watchmode antes do uso comercial.
5. Executar testes Kotlin/Python/Node relevantes e compilação Android unsigned em CI; distinguir testes de fonte de testes comportamentais. Não promover sem relatórios PASS do commit específico.
6. Validar interface e conteúdo no dispositivo, incluindo desempenho, navegação, favoritos, assistidos e seções de catálogo. QA_FISICO NAO_TESTADO até evidência.
7. Só depois de tudo aprovado: novo versionCode >79, assinatura com identidade original, publicação OTA V2 expressamente autorizada, readback imutável. Não tocar na OTA legada nem fazer merge automático.
8. Atualizar no mesmo ciclo AGENTS, STATUS, DECISIONS, CODEBASE_MAP e checkpoint canônico; guardar links de commit/workflow/relatório.

## Estado desta auditoria
TRAKT: CODIGO_PREPARADO, ATIVACAO_NAO_COMPROVADA.
WATCHMODE: NAO_IMPLEMENTADO; LICENCA_COMERCIAL_NAO_APROVADA.
CURADORIA_REAL: NAO_AUDITADA; CI/BUILD desta entrega: NAO_TESTADO.
APK nova: NAO_GERADA; OTA: NAO_ALTERADA.

Bloqueios de terceiros (credenciais ou contratação) não devem ser falsamente descritos como resolvidos. Seguir com demais verificações e melhorias sem publicar release.
