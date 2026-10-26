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

## DEV — 25/10/2026 — CI Trakt + Watchmode
- CI isolado agora executa juntos `src/worker.test.mjs` (inclui testes de sinais Trakt) e `src/watchmode.test.mjs`: commit `b2870c0289676cc570bda41bc33d7adeef8fa7a7`, workflow blob `431ba7476265328c0484f931bab6b771c517c883`.
- Documentacao oficial do SDK Watchmode confirma ID TMDB `movie-278` e `tv-1396` diretamente no endpoint title; portanto NÃO exigir conversão previa para ID Watchmode no endpoint em uso. Verificar respostas live antes de liberar.
- Cloudflare readback desta etapa BLOQUEADO pela ferramenta; chaves Trakt anteriormente confirmada e Watchmode informada pelo usuario, sem verificação atual. Testes CI criados mas PASS NAO_COMPROVADO, endpoints LIVE NAO_VERIFICADOS, APK NAO_GERADA. Sem contador de creditos local e sem prompt de chaves no APK.

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

> **GATE NOVA APK — 25/10/2026:** a pedido do usuário, nenhuma próxima APK será considerada pronta antes de curadoria real auditada, Trakt ATIVO/HTTP real e Watchmode integrado com licença válida/HTTP real. Documento normativo: [ANYTV_2951_INTEGRATION_RELEASE_GATE_20261025.md](../qa/ANYTV_2951_INTEGRATION_RELEASE_GATE_20261025.md), commit b820b2ee7652031841d58514cbc1a7dc42a2ec6a. Trakt apenas preparado, Watchmode NÃO integrado, sem contratação aprovada. Não disparar build-request/OTA, não afirmar homologação. 

## DEV 25/10/2026 — fontes de novas temporadas (nao publicado)
- `recentlyStartedSeasons` passou a combinar `tv/on_the_air` e `trending/tv/week`, deduplicados por TMDB ID, antes de verificar temporada posterior com data de estreia nos ultimos 90 dias. Preserva maximo de 60 candidatas, concorrencia de 4 consultas de temporadas, exclusao de Anime e series estreantes. Codigo [00b850ad](https://github.com/brenoff2/tcl-apk-ci-builder/commit/00b850ad61521f6052986e75f1937c51000b77b2), teste [5efce875](https://github.com/brenoff2/tcl-apk-ci-builder/commit/5efce875570d0c4311ea37461eb4192c7d6d4370). Blobs Kotlin d14f8bd5fda064535ea599de3a53ac4e9335549b, teste 058a742ce71bb11194a3e75eedc1ea067c046a1c. CI/Gradle=NAO_TESTADO nesta etapa; assinatura/OTA=NAO; QA fisico/editorial=NAO_HOMOLOGADO; release documentada permanece 2.9.50/code79.

## Reconciliacao DEV — 25/10/2026
- Aplicar exclusao de filmes ja assistidos uniformemente, inclusive nas secoes de estreias com retorno antecipado, sem remover series acompanhadas automaticamente. Mudanca cirurgica em `anyTvCurate`, ainda nao compilada nem publicada; nao altera proporcao editorial, fontes ou regras de streamings. Commits `8d61aed11cfe5fcaf2b7c19071639203255724c0` e `09e8b8e6d1c01e3ded31278722e67d05c4dc2a3f`.

## Regra obrigatoria de sincronizacao documental (25/10/2026)
- Em toda tarefa de desenvolvimento ou transicao, ler e reconciliar `AGENTS.md`, `docs/dev-context/STATUS.md`, `docs/dev-context/DECISIONS.md`, `docs/dev-context/CODEBASE_MAP.md` e o checkpoint canonico vigente. Antes de alterar codigo, identificar branch, HEAD, blobs relevantes e release/OTA verificadas; nao pressupor que o ultimo status documental e o estado live.
- Toda mudanca relevante (codigo, contratos, arquitetura, dependencias, tests, releases, Worker/OTA, interfaces, decisoes, bloqueios ou homologacoes) exige atualizar, NA MESMA ENTREGA, os arquivos afetados: AGENTS (regras e ponteiro canonico), STATUS (estado e evidencias), DECISIONS (decisoes vigentes e substituicoes), CODEBASE_MAP (paths, responsabilidades e SHAs/ancoras), e checkpoint canonico (handoff). Nao basta atualizar somente um arquivo. Se algum nao sofrer mudanca material, registrar explicitamente a verificacao de nao-necessidade.
- Ao publicar release, atualizar em primeiro plano a versao, versionCode, links de workflow/artefato, commit e canal OTA. Distinguir `BUILD`, `ASSINADO`, `OTA_PUBLICADA`, `READBACK_VERIFICADO`, `QA_FISICO` e `QA_EDITORIAL`; escrever `NAO_TESTADO` onde faltar prova. Referencias antigas devem ser rotuladas HISTORICO, nunca utilizadas como estado corrente.
- Reconciliar conflitos usando evidencias do GitHub no ref correto e runtime real quando acessivel. Nao inventar SHA do HEAD, deploy, teste ou contagem. Usar commits pequenos de documentacao sem acionar build-request; confirmar por readback os arquivos alterados e relacionar links/SHAs no fechamento. Em falha de acesso ou sincronizacao, sinalizar PENDENTE, sem declarar fechamento completo.
- Preservar pacote `com.tclremote.livre.v2`, signer, OTA V2, OTA legada, Worker/segredos e PR #3 sem merge/deploy/publicacao nao autorizados. Regras de produto e historico anteriores permanecem validos salvo revogacao expressa.

## Curadoria V2 — 2.9.50/code79 (25/10/2026)
- Principal e páginas de cada streaming reutilizam a **mesma função de curadoria**, com candidatos distintos. A página de streaming consulta exclusivamente o provedor BR em modalidade `flatrate`, sem confundir aluguel, compra ou canais adicionais com assinatura básica. A presença do título em provedores não garante acesso a episódios/temporadas específicos.
- Máximo de **50 títulos reais por seção**. O alvo editorial é **40% alinhamento com gêneros escolhidos/favoritados, 35% consenso de qualidade e tendências, 25% descobertas**; nunca completar quotas com conteúdos falsos ou repetidos. Popularidade histórica isolada não decide ordem. Priorização de atualidade é mais forte nas seções de destaque e lançamento.
- A Principal passa a incluir Destaques da semana, Filmes recém-lançados, Séries novas, Novas temporadas, Em alta agora, Recomendados para você, Você pode gostar, Surpresas do catálogo, Vale a pena assistir, Populares no TMDB e Mais bem avaliados; Minisséries imperdíveis somente com prova `type=Miniseries`. Páginas por streaming compartilham nove grupos verificados e mantém exploração genérica depois da curadoria; mostrar somente grupos com títulos comprovados.
- `Populares no TMDB` substitui alegação de “mais assistidos”: popularidade TMDB não equivale a audiência real.
- Trakt Standard é sinal opcional de público por TMDB ID, obtido apenas no Worker sem chave no APK, não um segundo catálogo irrestrito. A fonte ausente preserva rankeamento TMDB. **Não declarar Trakt ativo até Client ID, deploy e readback.** API Standard requer marca/atribuição e proíbe espelhamento ou coleta massiva.
- Watchmode somente pesquisa/teste futuro com credencial e licença aprovadas; **nenhum uso pago automático**. Cinema e Anime preservam exclusividade, e lançamentos continuam limitados por datas factuais.
- A release Android 2.9.50/code79 foi publicada pela [execução #40250636867](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867), OTA [commit 7ef4e20](https://github.com/brenoff2/tcl-apk-ci-builder/commit/7ef4e20b6b0e9a414455d8c1babf10664311ea1e). Teste físico e curadoria real ainda pendem de homologação.

## Complemento aprovado — 2.9.48/code77 (25/10/2026)
- Gênero e Anime: modo padrão é lista Detalhada. O botão «Ver em seções» **deve ativar seções de fato** com o mesmo componente horizontal do Streaming, agrupando Filmes, Séries, Lançamentos recentes e Mais bem avaliados, sem duplicação dos trilhos com a lista. O texto do botão mostra a próxima ação, nunca o estado invertido.
- Lista Detalhada: espaçamento de metadados compacto de 1dp, linha de provedores de altura mínima 16dp, sinopse lineHeight 15sp; preservar título, gêneros, badges, logos e dois botões distintos.
- Slide principal: usar somente `StreamingOfferIcons` para F no quadrado azul/S no círculo vermelho, ano e indicador Cinema quando confirmado, sem texto F/S solto e sem duplicar Cinema.
- «Filmes recém-lançados» e «Séries novas»: descobrir títulos em fontes TMDB permitidas pelo Worker, combinando páginas ordenadas por popularidade e data, filtrar estreia original nos últimos 90 dias e excluir Anime. Filmes encontrados no catálogo Cinema BR corrente pertencem exclusivamente à página Cinema. Oferta flatrate BR **não é pré-requisito** para classificação factual de estreia; disponibilidade para assistir é informação separada.
- «Recém-chegados ao Streaming»: não usar atualidade do filme, popularidade ou primeira consulta como prova. Persistir provedores BR observados por título; somente um novo provedor detectado em comparação posterior com o mesmo título autoriza inclusão. Primeira execução faz baseline e pode mostrar seção vazia. Histórico guarda evidência observacional local por 30 dias, não garante data exata de inclusão na plataforma.
- Publicação: preservar signer V2 e cert2026. Verificar branch remota e bytes pelo **SHA imutável do commit**, para evitar falsos negativos do cache CDN da URL de branch.
- QA física e visual não pode ser afirmada sem prova no aparelho; CI de contrato/Gradle/OTA não substitui essa etapa.

## Organização de lançamentos aprovada — 25/10/2026
- Remover a seção genérica «Novidades», anteriormente alimentada por `movie/now_playing` e `tv/on_the_air`, pois confundia Cinema, séries em andamento e estreias.
- «Filmes recém-lançados»: considerar estreias até 90 dias e oferta `flatrate` BR comprovada, excluindo títulos identificados como em cartaz na consulta Cinema BR. Datas regionais específicas requerem fontes/rotas adicionais verificadas.
- «Séries novas»: usar a primeira data de estreia, não o status de exibição.
- «Novas temporadas»: distinguir temporadas adicionais cuja data de início esteja nos últimos 90 dias; não tratá-las como séries novas.
- «Recém-chegados ao Streaming»: não inferir entrada recente apenas por presença no TMDB/JustWatch. Até existir histórico real de entradas por serviço, mostrar indisponibilidade de evidência, sem listar falsos recém-chegados.
- Anime japonês (gênero animação + idioma original `ja`) deve ficar isolado dos catálogos gerais; página Anime usa descoberta TMDB por gênero 16 e filtragem local da origem. O Worker de produção rejeita `with_original_language` e campos de data não permitidos com HTTP 400. Não alterar a allowlist do Worker só para suprir filtros de interface; optar por consultas permitidas e filtro local, com teste HTTP real antes da assinatura.
- Página Gênero/Anime usa lista Detalhada como padrão e alternância para seções, sem trilhos duplicados.
- A publicação 2.9.45/code74 confirma build, teste de Worker, assinatura e OTA, mas **não** confirma homologação física de telas e dados no dispositivo.

# AnyTV — Decisões vigentes
Atualizado em 2026-10-25. Fonte: checkpoint canônico 2.9.39 e arquivos de implementação consultados na branch DEV. Decisões não documentadas permanecem em aberto.

## Identidade e continuidade
- Preservar projeto e implementação existentes; desenvolvimento pelo ChatGPT Web sem dependência obrigatória de Codex.
- GitHub é fonte de verdade; consultar `AGENTS.md`, `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md`, este diretório e código real antes de editar.
- DEV: `feat/anytv-p0-device-proof-verifier-20261020`; PR #3 permanece Draft, sem merge automático.
- Não confundir estado de CI, build unsigned, APK assinado, publicação OTA e homologação física.
- Não alterar produção, Worker, D1, companion TCL, chaves, signer, updater ou OTA legada sem necessidade e autorização expressa.
- Aperfeiçoamentos novos são CANDIDATA até aprovação; não reestruturar arquitetura por conveniência.

## Produto e interface aprovados
- Home / controle remoto: faixa de apps deve exibir somente aplicativos efetivamente detectados na televisão; Editar seleciona esses apps.
- Streaming: exploração independente da presença do aplicativo na TV; Editar possui «Na minha TV» e «Outros streamings»; seleção persistente e sem duplicatas; contorno vermelho indica seleção.
- Instalação, assinatura e capacidade de reproduzir são conceitos diferentes; nunca prometer reprodução sem verificação.
- TMDB/JustWatch BR; flatrate confirmado distinto de aluguel/compra e de assinaturas adicionais como Universal+ Amazon Channel; não excluir provedores simplesmente por Channel/Canal/Addon.
- Max e HBO Max são uma identidade de serviço para deduplicação; logotipos oficiais TMDB quando válidos.
- Cinema e Em breve exigem classificação cuidadosa; preservar regressões A Odisseia (2026) e Crystal Lake (2026).
- Identidade visual aprovada sem marca indevida nas páginas internas; gradiente/sombra nos títulos, indicação de ano e indicadores minimalistas.
- Grade do catálogo: 3 colunas; modo Detalhado: cartaz isolado à esquerda; direita título, gêneros, indicador F/S + ano + logos, sinopse e botões separados Trailer / Mais detalhes.
- Não duplicar metadados abaixo do cartaz no modo Detalhado. Preservar favoritos, assistidos, acompanhamento e filtros.
- «Ver em lista» / «Ver em seções» à direita, com aparência de botão; subtítulo com barra vermelha e espaçamento coerente com Gênero; Grade, Detalhado e Ordenar na linha abaixo.
- Ordenação dos resultados já carregados: Mais populares, Melhor nota, Mais recentes e Título A–Z; não afirmar ordenação global da biblioteca.

## Versionamento e entrega
- Última OTA confirmada pelo checkpoint: 2.9.39/code68; próxima release, se autorizada, precisa de versionCode >68.
- Preservar pacote `com.tclremote.livre.v2`, certificado original e rota OTA legada.
- Correção não é entregue apenas com commit: requer testes + compilação + assinatura + OTA oficial + byte-readback quando houver autorização para publicar.
- Sem teste físico, registrar NÃO_TESTADO e não promover gates de homologação.
- Evitar «continue» repetidos: realizar ciclos completos e comunicar evidências.

## Conflitos e precedência
- `AGENTS.md` contém resumos cronológicos mais antigos; a entrada referente a 2.9.31 é histórica. Para o marco de 2.9.39, priorizar canônico e conferir GitHub/OTA em tempo real.
- Este arquivo registra o que já está fundamentado, não é autorização autônoma para alterar produção nem substitui validação física.
