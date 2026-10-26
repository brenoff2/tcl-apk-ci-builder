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
