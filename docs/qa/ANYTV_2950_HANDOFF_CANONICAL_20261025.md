## 26/10/2026 — verificacao segura de preview e efeito editorial Trakt
- Readback oficial Cloudflare: versao candidata Trakt `f927d4e0-c4fd-4a30-a5e3-12910a9664a9` permanece STAGED, etag `edcb31819f13b037b47b9dc513ea3d936024c23c55cd0355a854050cbc7b1e9e`; producao continua `88779b1b-9dbd-4dc6-9884-eff52769ded2` a 100%; Version URLs `previews_enabled=false`.
- API oficial Cloudflare nao ofereceu endpoint de execucao privada direta de versao preparada. Documentacao oficial indica URLs de versao publicas quando habilitadas; nao habilitar sem Access ou outro isolamento. Nao criar aplicativo de Access sem politica de identidade validada; nenhum trafego/OTA alterado.
- Revisao do Android `overrides/TclStreamingScreens.kt` confirmou `anyTvEditorialScore` usa peso logaritmico de watchers Trakt (maximo 12 pontos) e `anyTvCurate` repassa mapa de sinais para ordenar resultados reais. A inspecao de teste fonte detectou somente verificacoes de presenca da integracao; QA editorial/TCL permanece pendente. Nao declarar Trakt funcional live sem ver resposta de endpoints movie e tv.

## Gate de preview protegido — 26/10/2026
- Cloudflare API oficial confirmou a versao candidata `f927d4e0-c4fd-4a30-a5e3-12910a9664a9` armazenada e producao 100% na versao `88779b1b-9dbd-4dc6-9884-eff52769ded2`, sem mudanca de trafego.
- Verificacao Cloudflare Access `GET /accounts/{account_id}/access/apps` retornou 10 aplicativos, nenhum correspondente a AnyTV, dominio brenoff.workers.dev ou anytv-link-resolver-pilot. A configuracao do Worker segue `previews_enabled=false`. Conforme documentacao oficial, Version URLs ficam publicamente acessiveis quando habilitadas sem Access; por isso preview nao foi ativado nem endpoint exposto.
- Proximo requisito para homologacao HTTP sem expor preview: criar uma politica de Access restritiva para URL de versao OU usar ambiente de teste isolado com credenciais e permissoes proprias; validar entao movie e tv, resultados e rollback. Nao habilitar version URL sem controle de acesso, nao executar deploy produtivo as cegas. CI GitHub ja PASS; HTTPS live e QA TCL ainda nao realizados. Watchmode fora do escopo.

## Cloudflare Trakt-only — versao STAGED SEM TRAFEGO (25/10/2026, UTC 26/10)
- Upload via API oficial `POST /workers/scripts/anytv-link-resolver-pilot/versions` concluiu HTTP 200: nova versao `f927d4e0-c4fd-4a30-a5e3-12910a9664a9` (source api, criada 2026-10-26T02:59:59Z). Contem 4 modulos da branch DEV, com `worker.mjs` Trakt-only, e `keep_bindings` para plain_text/secret_text/d1.
- Readback API da nova versao confirmou bindings originais `ANYTV_DEVICE_ACTIVATION_ENABLED`, `ANYTV_MOVIE_API_KEY`, `ANYTV_RESOLVER_AUTH_MODE`, `DB`, `TMDB_READ_TOKEN`, `TRAKT_CLIENT_ID`, `WATCHMODE_API_KEY`; Watchmode permanece armazenado como segredo mas CODIGO NAO usa. Tres modulos `device-activation.mjs`, `device-proof.mjs`, `device-request.mjs` foram comparados por tamanho e hash FNV com prod, iguais; `worker.mjs` e modificado para Trakt.
- DEPLOY NAO FEITO: API confirmou producao inalterada `88779b1b-9dbd-4dc6-9884-eff52769ded2` a 100% e apta a rollback. Configuracao Cloudflare `subdomain.enabled=true, previews_enabled=false`; URL de versao nao disponivel sem habilitar previews. NAO alterar previews/Access automaticamente nem fazer canary nao homologado. Worker staging nao equivale a teste HTTP real.
- Trakt-only CI previamente PASS run 40259987249. Gate final segue dependente de smoke HTTP real de movie e tv, verificacao de comportamento editorial Android e TCL, assinatura e OTA preservadas. NENHUMA APK criada.

## Trakt live-gate independente do CI — 25/10/2026
- Criado script read-only `anytv-resolver/scripts/check-trakt-live.mjs`, commit `955ffacf92a688bef6cbccbf68b4743e71a1aa13`: valida GET HTTPS movie e tv e contrato source/kind/results/id/watchers. Exige `ANYTV_WORKER_ORIGIN` no ambiente de execucao, nunca envia Client ID nem segredo e nao altera Cloudflare. Nao faz parte do unit CI automaticamente; somente verificação pos-deploy. Ainda NAO_EXECUTADO em ambiente com acesso HTTP funcional.
- Readback confirmado do Worker atual `88779b1b-9dbd-4dc6-9884-eff52769ded2`: multipart de 4 modulos, sem `trakt-api-key`. Portanto LIVE_TRAKT_NOT_DEPLOYED, apesar de secret e testes CI PASS. Liberacao de APK permanece bloqueada até deploy seguro, smoke live e QA TCL. Watchmode fora do escopo.

## Auditoria runtime Cloudflare: Trakt nao implantado — 25/10/2026 (UTC 26/10)
- Cloudflare API oficial leu versao de producao 88779b1b-9dbd-4dc6-9884-eff52769ded2 (100% do trafego), etag do script `ead5ef5a0897670cdf6070a85220693ca0dfef7eff327a4811189d0388fa26f7`; GET `/content/v2` retornou multipart de 31108 caracteres com partes `worker.mjs`, `device-proof.mjs`, `device-request.mjs`, `device-activation.mjs`. Verificacoes no conteudo: contem catalogo e dispositivo, NAO contem `trakt-api-key` nem rota Trakt, NAO contem Watchmode. Portanto, TRAKT **NAO IMPLANTADO** apesar de `TRAKT_CLIENT_ID` constar como Secret.
- GitHub DEV ja possui rota Trakt e CI P0 PASS no run 40259987249; diferenca entre branch e Worker ativo confirmada. Nao substituir producao sem conservar os modulos e bindings atuais e definir rollback pela versao `88779b1b-9dbd-4dc6-9884-eff52769ded2`; release gate segue fechado ate teste live e QA editorial/TCL. Nenhum deploy executado nesta auditoria.

## Cloudflare LIVE — leitura verificada 25/10/2026 (UTC 26/10)
- API oficial Cloudflare Worker `anytv-link-resolver-pilot`: `TRAKT_CLIENT_ID` e `WATCHMODE_API_KEY` presentes como `secret_text`, sem exibir valores. Watchmode fora de escopo da entrega, NAO ativar.
- Deployment mais recente ID `e94c3bad-bcac-4b40-bad9-a317d7967563`, versao `88779b1b-9dbd-4dc6-9884-eff52769ded2` a 100%, source `dash`, criado 2026-10-26T02:01:56Z. NAO comprovado que contem commit GitHub da branch DEV, portanto nao afirmar Trakt publicado.
- HTTP real `GET /v1/catalog/signals/trakt/movie` e `/tv`: tentativas sem sucesso por falha DNS da ferramenta e do runtime (`Temporary failure in name resolution`), resultado FUNCIONAL NAO_COMPROVADO. Nao fazer deploy ou OTA sem correspondencia conteudo/versao + homologacao. CI PASS comprovado run 40259987249, commit 8ee169ad.

## CI Trakt-only COMPROVADO — 25/10/2026 (run UTC 26/10)
- API GitHub Actions oficial confirmou run 40259987249 `https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40259987249` status completed/conclusion success, workflow P0 device-proof, head `8ee169ad3f85cf4eb3ff06020c15adc21f79acc8` na branch DEV. Job 121370058446 success e steps `Trakt signal adapter regression (no external API credits)` + `Resolver and device-proof regressions` + `Syntax` + D1 smoke todos success.
- Importante: CI foi confirmado para esse head; nenhum teste HTTP real contra Trakt/Worker publicado ocorreu nesta validacao, nenhum deploy/OTA e APK gerados. Trakt secret e Watchmode secret constam como nomes de bindings do Worker via readback Cloudflare, mas Watchmode permanece FORA DO ESCOPO e nao deve ser ativado.

## Trakt-only: dependencia removida — 25/10/2026
- `catalogRequest` agora despacha `signals/trakt/{movie|tv}` ANTES do gate TMDB_READ_TOKEN/DB; endpoint Trakt depende somente de `TRAKT_CLIENT_ID`, nao exige OAuth/login/codigo do usuario. Codigo commit `5fb495efa5eda99025ab5369c44893a95422b17a`; regressao pelo roteador real para sinais TV com apenas Client ID e erro fail-closed sem chave: `a86639b231d30f9f400fa0f48746bb76b3eb2978`.
- Watchmode FORA do release segundo decisao atual do usuario; nao reintroduzir sua rota. Live Worker ainda NAO_DEPLOYED/NÃO_VERIFICADO nesta etapa; tentativa de consultar dominio resultou em erro DNS do ambiente. Testes de unidade/CI sem PASS confirmado. Nova APK/OTA NAO publicada.
- Proximo gate: CI real com logs e resposta live dos endpoints /v1/catalog/signals/trakt/movie e /tv; verificar efeito editorial no Android, regressao e QA TCL antes de novo release.

## DECISAO SUPERSEDE ANTERIORES — 25/10/2026: ENTREGA TRAKT-ONLY
- Usuario decidiu implementar somente Trakt nesta entrega. WATCHMODE ESTA FORA DO ESCOPO DA APK, DA ATIVACAO DO WORKER E DOS GATES DE RELEASE; instrucoes historicas contraditorias sobre obrigatoriedade Watchmode estao REVOGADAS. Secret Watchmode eventualmente existente nao autoriza seu uso; nao consultar a API. Arquivo experimental `src/watchmode.mjs` e teste retidos para estudo futuro, mas rota ingress removida de `src/worker.mjs` (commit `e24054783ccf31b346c05abc5231a7d01abefe0d`), impedindo o servico nesta candidata.
- `anytv-resolver/package.json` nao executa mais testes Watchmode no `npm test` (commit `9ba0baa19c9fea6ee1b7e71e0e88109b2b057da7`). Workflow P0 executa apenas teste Trakt/Worker na etapa de sinais (commit `ebf0933b7d6c2f21358662e7c3ace82e07037242`).
- Trakt: `TRAKT_CLIENT_ID` ja confirmado anteriormente no Worker; adaptador `traktSignalsRequest` em `worker.mjs` serve `/v1/catalog/signals/trakt/{movie|tv}`; `StreamingCatalog.traktTrendingSignals()` no Android ja consulta os sinais e agrega na curadoria. Nao exige login ou codigo Trakt. Trakt ainda NAO_HOMOLOGADO no HTTP live. Nao afirmar CI PASS ou Worker implantado sem evidencia.
- Gate de release Trakt-only: prova de HTTP Trakt real + curadoria Android + CI + validacao editorial/TCL + preservacao de assinatura/OTA. Nao criar APK ate gates aprovados. Nenhum deploy/publicacao foi feito por esta mudanca.

## Diagnostico de bloqueio efetivo — 25/10/2026
- Inspecao de `overrides/TclStreamingScreens.kt`: `StreamingCatalog.traktTrendingSignals()` ja chama `GET /v1/catalog/signals/trakt/{movie|tv}` por `get("backend", route)`; sem prova HTTP live nao declarar integrado. `StreamingCatalog.get()` envia somente Accept/User-Agent, sem prova de dispositivo. Nenhuma chamada a `/v1/catalog/watchmode/...` foi encontrada nesta tela.
- `anytv-resolver/src/worker.mjs`: ingress Watchmode exige `ANYTV_RESOLVER_AUTH_MODE=device`, `ANYTV_WATCHMODE_ENABLED=true`, segredo e `verifyDeviceProof` aprovado; manter fail-closed. Simples ativacao de flag nao conecta Android, pois falta cliente assinar e enviar prova de dispositivo de forma silenciosa e autorizada.
- Atenção ao contrato de dados: `source_id` Watchmode nao e automaticamente o mesmo identificador dos provedores TMDB; normalizar por mapeamento verificado antes de usar em filtros dos servicos. Nao introduzir token de API no APK, nem burlar seguranca do Worker, nem solicitar login/codigo Trakt ou Watchmode.
- Tentativa de checar HTTP Worker nao encontrou resolucao DNS no ambiente desta execucao (HTTP nao verificado). GitHub API de workflows ligada a commit nao retornou runs associados (nao comprova CI executado). Proximos gates reais: protocolo de dispositivo no Android, mapeamento provedores, teste HTTP autenticado, teste Android, CI comprovado. Zero deploy/OTA.

## 25/10/2026 — Teste de ingresso Watchmode no Worker
- Teste real do roteador `handleRequest` adicionado a `anytv-resolver/src/watchmode.test.mjs`: verifica resposta 503 sem modo `device` e 503 sem `ANYTV_WATCHMODE_ENABLED=true`, sem atingir a API externa. Commit `a55a3ce0f51ca56d1dde9bd92a2577b2d3ee1dc4`; blob `78770b25578c11d8873a170b719e5bfc9c779dbc`. Ainda NAO_EXECUTADO/CI_NAO_COMPROVADO; nao e validacao HTTP live.
- Dois checkpoints canonicos antes pendentes agora sincronizados (`dae0c5534f11c1d0e855443216af284df66b2aa8`, `07d59e0f4d9bde9155266c6c211a2b6297fe5ece`). Segredo Watchmode informado pelo usuario, readback Cloudflare bloqueado. APK, signer e OTA inalterados.

## CI conjunto Trakt + Watchmode — 25/10/2026
- Workflow isolado `.github/workflows/anytv-p0-device-proof-ci.yml` executa testes Trakt (`src/worker.test.mjs`) e Watchmode (`src/watchmode.test.mjs`) na mesma etapa; commit `b2870c0289676cc570bda41bc33d7adeef8fa7a7`, blob `431ba7476265328c0484f931bab6b771c517c883`.
- Nenhum PASS de CI nem resposta HTTP live validado por esta anotacao; OTA/assinatura intactas. Trakt secret previamente confirmado; Watchmode secret informado pelo usuario, mas readback atual pendente. Sem limitador proprio de creditos ou prompt de chave no APK.

## DEV 25/10/2026 — isolamento de cache Watchmode e decisao de credito
- `watchmode.mjs` removeu o cache edge compartilhado de respostas autenticadas por dispositivo: codigo commit `3c1dd8d7554300809799317851aab7e33cb46f6c`, blob `9901bc63b464d62de175a1f9642b335e4b06cd82`; teste de regressao `6b34a41e466ad1969767af09fe6dfe75a2630eee`, blob `08410af7888ed55d89752fd6782a87a977921a60`.
- Decisao do usuario: nao incluir contador/limitador mensal proprio; a franquia gratuita e controlada pelo Watchmode. Nenhuma chave ou login Trakt/Watchmode deve ser solicitado na interface APK. Credenciais pertencem exclusivamente ao Worker; preservar a seguranca do dispositivo existente e evitar nova tela de codigo.
- Chave Watchmode foi informada como cadastrada pelo usuario; leitura Cloudflare desta rodada foi bloqueada pela plataforma, portanto `WATCHMODE_API_KEY` NAO_VERIFICADO, deploy e HTTP real NAO_VERIFICADOS. Trakt secret havia sido confirmado anteriormente; endpoint live ainda nao validado. Nao declarar integracao ATIVA, testes PASS ou APK pronta.
- CI dos novos testes ainda sem resultado confirmado. Release 2.9.50/code79 permanece a ultima documentada. Nenhuma OTA/signature alterada.

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

> **ADENDO DE LIBERACAO 25/10/2026:** próxima APK condicionada ao [gate de curadoria/Trakt/Watchmode](ANYTV_2951_INTEGRATION_RELEASE_GATE_20261025.md) commit b820b2ee7652031841d58514cbc1a7dc42a2ec6a; nenhuma credencial ou licença presumida; sem build ou OTA.

## Adendo DEV — 25/10/2026, posterior a release 2.9.50
- Foi corrigido o filtro de filmes assistidos nas secoes de lancamentos, antes da saida antecipada de `anyTvCurate`: commit de codigo `8d61aed11cfe5fcaf2b7c19071639203255724c0` e regressao `09e8b8e6d1c01e3ded31278722e67d05c4dc2a3f`. Mudanca nao publicada: BUILD/CI=NAO_TESTADO, ASSINATURA=NAO, OTA=NAO; ultima distribuicao documentada permanece 2.9.50/code79; QA fisico e editorial real pendentes. Nenhuma credencial ou rota de producao modificada.

# ANYTV — TRANSIÇÃO CANÔNICA / CONTINUIDADE — 25/10/2026

> **Estado comprovado no GitHub:** AnyTV **2.9.50 / versionCode 79** assinado e publicado na OTA V2. GitHub Actions [#40250636867](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867) **completed/success**; artefato [anytv-v2-2950-signed-update](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867/artifacts/12621265708), ID `12621265708`; commit OTA `7ef4e20b6b0e9a414455d8c1babf10664311ea1e`. **Trakt preparado mas NÃO ativo; Watchmode não integrado; QA física/editorial não homologada**. Este documento revoga qualquer referência histórica que identifique 2.9.49 ou versões anteriores como release atual.

## Governanca documental obrigatoria — reconciliação de 25/10/2026

A cada entrega tecnica, antes de alterar o aplicativo, consultar e apos a mudanca reconciliar `AGENTS.md`, `docs/dev-context/STATUS.md`, `docs/dev-context/DECISIONS.md`, `docs/dev-context/CODEBASE_MAP.md` e este checkpoint canonico vigente. Quando um desses documentos nao necessitar de mudanca material, registrar essa verificacao; nao manter referencias antigas como atuais. O AGENTS deve indicar o checkpoint mais recente, STATUS as evidencias, DECISIONS as regras vigentes e CODEBASE_MAP as rotas e blobs verificados no mesmo ref. Confirmar readback apos commits.

**Reconciliacao documental registrada (sem mudancas funcionais):** AGENTS `ce1b7830882a66febf6433b5ef058643699e4b18`; STATUS `ec02c8e480d4b1efe2bb36053610e7e9c129cacd`; DECISIONS `278b9fedee7e3e58d4f35b8597c1ffbf4dbdbf24`; CODEBASE_MAP `ce532321735d8eb1b6e33f1fd96cd69ec651616d`. Release de referencia documentada 2.9.50/code79, CI #40250636867; Trakt nao ativo; Watchmode nao integrado; QA fisico/editorial pendentes. HEAD DEV e estado OTA live nao foram comprovados neste ciclo, nao inventar SHA nem homologacao. Build, assinatura, OTA e QA sao fases distintas. Nenhuma nova release, deploy, merge, chave ou assinatura foi alterada nesta reconciliação.

## DEV 25/10/2026 — fontes de novas temporadas (nao publicado)
- `recentlyStartedSeasons` passou a combinar `tv/on_the_air` e `trending/tv/week`, deduplicados por TMDB ID, antes de verificar temporada posterior com data de estreia nos ultimos 90 dias. Preserva maximo de 60 candidatas, concorrencia de 4 consultas de temporadas, exclusao de Anime e series estreantes. Codigo [00b850ad](https://github.com/brenoff2/tcl-apk-ci-builder/commit/00b850ad61521f6052986e75f1937c51000b77b2), teste [5efce875](https://github.com/brenoff2/tcl-apk-ci-builder/commit/5efce875570d0c4311ea37461eb4192c7d6d4370). Blobs Kotlin d14f8bd5fda064535ea599de3a53ac4e9335549b, teste 058a742ce71bb11194a3e75eedc1ea067c046a1c. CI/Gradle=NAO_TESTADO nesta etapa; assinatura/OTA=NAO; QA fisico/editorial=NAO_HOMOLOGADO; release documentada permanece 2.9.50/code79.

## Objetivo de produto

AnyTV para Android/TCL: transformar a exploração de filmes e séries de catálogos genéricos do TMDB em recomendações editoriais realmente interessantes, recentes, personalizadas, diversificadas e disponíveis no Brasil quando a página se refere a um streaming específico. Preservar identidade visual aprovada, streaming/controle remoto, favoritos, assistidos, acompanhamento, Cinema e Anime isolados, provedor BR e OTA V2.

O usuário não gostou das seleções anteriores: muita coisa antiga nos destaques; poucas estreias; mesmas obras em várias seções; seções por streaming não comparáveis à Principal; interface de gênero/detalhes com erros repetidos. A decisão NÃO é apenas aumentar chamadas de API ou trocar nomes de seções: fazer curadoria útil, com evidências de relevância, confiança e recência, sem títulos inventados.

## Decisões editoriais aprovadas

1. **Até 50 títulos reais por seção**, com os melhores primeiro e exploração progressiva. 50 é teto/alvo, nunca preenchimento artificial se menos candidatos forem elegíveis.
2. **Personalização 40/35/25 como ponto inicial**: aproximadamente 40% gêneros escolhidos explicitamente e inferidos dos favoritos; 35% títulos bem avaliados/tendências gerais; 25% descobertas variadas fora da bolha. Pesos variam por seção: novidades priorizam data comprovada. Não confundir fórmula/quotas de seleção com métricas reais de audiência.
3. Principal e páginas individuais de cada serviço devem usar **um motor editorial comum**, mas o serviço selecionado restringe o catálogo a disponibilidade BR `flatrate` daquele provedor; adicional/canal, aluguel e compra não significam inclusão na assinatura básica.
4. Repetir grupos de descobertas relevantes na Principal e nos Streamings: Destaques da semana, Filmes recém-lançados, Séries novas, Novas temporadas, Em alta agora, Recomendados para você, Você pode gostar, Surpresas do catálogo, Vale a pena assistir e (quando comprovado) Minisséries imperdíveis. Em breve como linha separada; labels somente com significado factual.
5. **Não alegar “mais assistidos” sem audiência real**. “Populares no TMDB” é possível, devidamente rotulado.
6. Evitar monopolização por clássicos nos destaques: priorizar atualidade para destaque/novidade, preservar clássicos em seções adequadas. Aplicar qualidade ajustada pela quantidade de avaliações, diversidade de gêneros/franquias e deduplicação/editorial entre primeiras seções quando viável.
7. Filmes recém-lançados: estreia original em até 90 dias, sem confundir com `movie/now_playing` BR (Cinema exclusivo). Séries novas: primeira estreia da série em até 90 dias; novas temporadas: temporada posterior com data recente comprovada. Não confundir presença na plataforma com data de ingresso.
8. **Recém-chegados ao Streaming**: seção foi explicitamente RETIRADA, não reintroduzir. **Anime** (animação japonesa classificada) e **Cinema** têm páginas exclusivas e não devem contaminar recomendações gerais.
9. Gêneros abrem em **grade**; Ver em seções usa os mesmos trilhos horizontais editoriais da página Streaming; visualização Detalhada compacta título, gênero, ano/badges (sem padding de fonte extra) e preserva espaço **antes** dos botões Trailer/Mais detalhes.
10. Identidade visual: slide sem F/S solto; apenas F em quadrado azul e S em círculo vermelho, selo Cinema sem duplicação, título com gradiente/sombra e ano. Não recolocar marca indevida no topo das telas internas.

## Arquitetura e fontes

- App `overrides/TclStreamingScreens.kt` (Kotlin/Compose); tests `scripts/tests/test_streaming_screens_contract.py`.
- Cloudflare Worker `anytv-resolver/src/worker.mjs`: catálogo TMDB proxied com allowlist de rotas e parâmetros; não mandar parâmetros não permitidos (`with_original_language`, `first_air_date.gte`, `primary_release_date.gte` etc deram HTTP 400 no passado). Consultas válidas por `discover`, `trending/day`, `trending/week`, popularidade, `watch/providers`, seasons e outros endpoints permitidos. Validar rotas no Worker LIVE.
- **TMDB** mantém catálogo principal; disponibilidade brasileira é lida do TMDB/JustWatch conforme provedor `flatrate`. Não declarar que esse dado prova cada temporada/episódio.
- **Trakt Standard**: adaptador de sinais de interesse/trending por TMDB ID preparado no Worker na rota `/v1/catalog/signals/trakt/{movie|tv}`, com testes Node reportados PASS na documentação. **Ainda NÃO ATIVO**: falta Trakt Client ID no Worker, deploy governado, testes HTTP live e conferência de política/atribuição. Nunca pôr Client ID ou segredo indevido no APK. Não fazer scraping nem espelhamento massivo; consultar apenas sinais permitidos. Não confundir com catálogo de provedores.
- **Watchmode**: pesquisado como API complementar para lançamentos e disponibilidade; **NÃO INTEGRADO**. Plano gratuito não autoriza presumir uso comercial nem cobrir necessidades pagas; avaliar licença, preço e valor antes de qualquer implementação com custo. Não contratar nem ativar pagamento sem autorização específica.
- A versão **2.9.50/code79** já contém motor editorial local com gêneros declarados/favoritos, score de qualidade com confiança, recência e descoberta, seleção de até 50, fontes `trending/day`/semana e pools de TMDB, e seções da Principal e do streaming. Conferir comportamento real e código antes de complementar. **Não voltar à hipótese de que tudo ainda precisa ser iniciado.**
- Sem Trakt ativo, mecanismo deve funcionar somente com TMDB, com degradação honesta.

## Releases e auditoria

| Versão | Evidência |
|---|---|
| 2.9.45/code74 | [workflow #40239875487](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40239875487), Anime/novidades ajustados, mas resultados inadequados em uso |
| 2.9.48/code77 | [workflow #40241733615](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40241733615), botão/linhas/indicadores e tentativa de novas seleções |
| 2.9.49/code78 | [workflow #40243533849](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40243533849), remove Recém-chegados, Gêneros grade, descoberta mais larga e linha detalhada |
| **2.9.50/code79** | **[workflow #40250636867](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867) completed/success**, [artifact #12621265708](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867/artifacts/12621265708), OTA commit `7ef4e20b6b0e9a414455d8c1babf10664311ea1e`. |

Canal OTA V2 `feat/tcl-v22-updates-back-bluetooth`, arquivo `updates/android/v2/current.apk`. **NUNCA** tocar na OTA legada `updates/android/cert2026/current.apk`. Preservar pacote `com.tclremote.livre.v2`, signer V2, updater, compatibilidade do controle remoto e separação dos canais. Para próxima release, **versionCode >79**. O workflow oficial é `.github/workflows/release-anytv-phone-v2-new-package.yml` e gatilho declarativo `.github/anytv-phone-v2-build-request.json`. Não iniciar release para simples documento de continuidade.

## Estado exato e pendências (não esconder)

**FEITO / COMPROVADO no repositório:** commits de curadoria e publicação 2.9.50; CI, APK assinado e readback OTA bem-sucedidos; documentos `docs/dev-context/STATUS.md` e `docs/dev-context/DECISIONS.md` registram release e decisões; código contém `anyTvCurate` e rota/cliente opcional de sinais Trakt.

**NÃO FEITO / NÃO COMPROVADO:** Trakt Client ID e Worker live não ativados, nenhuma prova da resposta live Trakt; Watchmode não integrado; validação visual e de comportamento no celular/TCL não realizada; auditoria qualitativa dos **50 títulos reais por seção** não feita; contagem por seção, recência, disponibilidade e duplicação entre Principal/Netflix/Prime/Disney+/Max não auditadas; eventual taxa/contratos de API externos não aprovados. Não afirmar satisfação editorial ou que 50 títulos reais aparecem necessariamente.

**Investigação prioritária para próximo chat:**
1. Readback do HEAD GitHub e do WORKER realmente publicado, comparar contrato e implementação. Conferir PR #3 Draft e não fazer merge sem aprovação.
2. Extrair amostras reais de cada seção (Principal e pelo menos 3 serviços) com nome, TMDB ID, tipo, data, notas, votos, gêneros, fonte e provedor BR; medir quantidade, idade mediana, percentuais 0-90/91-365/>365 dias, duplicação transversal e preferências 40/35/25. Não inventar amostra.
3. Confirmar minisséries por metadado, novas temporadas por data de início da temporada, novidades por estreia correta e providência quando Worker devolve poucos dados.
4. Verificar efetividade dos 50 títulos e do botão Ver mais, persistência de favoritos/assistidos e ausência de Anime/Cinema fora de seus canais.
5. Trakt: verificar documentação oficial atual, termos e limites; obter Client ID por processo legítimo, configurar somente Worker após aprovação, rodar testes e readback sem expor credenciais. Se falta acesso, manter adaptador desligado.
6. Watchmode: avaliar custo/uso comercial; sem autorização, manter fora da execução; sem compras.
7. Corrigir falhas com testes de dados reais e testes de interface, e só então gerar nova OTA code>79, com assinatura, CI, Worker live e readback por commit SHA.

## Fonte da verdade e instruções de retomada

- Repo: https://github.com/brenoff2/tcl-apk-ci-builder
- Dev: `feat/anytv-p0-device-proof-verifier-20261020`
- PR Draft #3: https://github.com/brenoff2/tcl-apk-ci-builder/pull/3
- Canonical root status: `docs/dev-context/STATUS.md`
- Product decisions: `docs/dev-context/DECISIONS.md`
- Code map (histórico, checar hashes): `docs/dev-context/CODEBASE_MAP.md`
- Historical checkpoint: `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md`
- **Este checkpoint atualizado:** `docs/qa/ANYTV_2950_HANDOFF_CANONICAL_20261025.md`

Regra de leitura: cabeçalhos históricos que dizem “última release 2.9.39/48/49” NÃO prevalecem sobre o registro mais recente **2.9.50/code79**. Diferenças temporais no GitHub devem ser resolvidas por leitura fresca de Actions, branch e artefato antes de agir.

O usuário valoriza entregas práticas, continuidade sem repetição de “continue”, arquivos de código GitHub como fonte canônica, pouca cerimônia, separação entre projetos e sem publicar alterações falsas ou quebrar o aplicativo. Agradecimentos e atualizações genéricas não substituem execução. Não afirmar homologação de dispositivo por passar CI.
