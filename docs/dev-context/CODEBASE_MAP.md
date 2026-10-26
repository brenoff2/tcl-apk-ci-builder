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

## Mapa incremental — DEV apos 2.9.50 (25/10/2026)
- `overrides/TclStreamingScreens.kt`: `anyTvCurate` filtra filmes assistidos antes de ordenar e do retorno antecipado em estreias; blob `eee69a330624e455bd5f5771b434711e02a1c556`, commit `8d61aed11cfe5fcaf2b7c19071639203255724c0`.
- `scripts/tests/test_streaming_screens_contract.py`: contrato de ordenacao do filtro para secoes recentes; blob `e6260625db94b61fefe94e45b3ea912aed09f62e`, commit `09e8b8e6d1c01e3ded31278722e67d05c4dc2a3f`.
- Nenhuma modificacao do Worker, build-request, workflow ou TV. SHAs sao de blobs consultados/atualizados; HEAD da branch nao foi independentemente verificado.

## Regra obrigatoria de sincronizacao documental (25/10/2026)
- Em toda tarefa de desenvolvimento ou transicao, ler e reconciliar `AGENTS.md`, `docs/dev-context/STATUS.md`, `docs/dev-context/DECISIONS.md`, `docs/dev-context/CODEBASE_MAP.md` e o checkpoint canonico vigente. Antes de alterar codigo, identificar branch, HEAD, blobs relevantes e release/OTA verificadas; nao pressupor que o ultimo status documental e o estado live.
- Toda mudanca relevante (codigo, contratos, arquitetura, dependencias, tests, releases, Worker/OTA, interfaces, decisoes, bloqueios ou homologacoes) exige atualizar, NA MESMA ENTREGA, os arquivos afetados: AGENTS (regras e ponteiro canonico), STATUS (estado e evidencias), DECISIONS (decisoes vigentes e substituicoes), CODEBASE_MAP (paths, responsabilidades e SHAs/ancoras), e checkpoint canonico (handoff). Nao basta atualizar somente um arquivo. Se algum nao sofrer mudanca material, registrar explicitamente a verificacao de nao-necessidade.
- Ao publicar release, atualizar em primeiro plano a versao, versionCode, links de workflow/artefato, commit e canal OTA. Distinguir `BUILD`, `ASSINADO`, `OTA_PUBLICADA`, `READBACK_VERIFICADO`, `QA_FISICO` e `QA_EDITORIAL`; escrever `NAO_TESTADO` onde faltar prova. Referencias antigas devem ser rotuladas HISTORICO, nunca utilizadas como estado corrente.
- Reconciliar conflitos usando evidencias do GitHub no ref correto e runtime real quando acessivel. Nao inventar SHA do HEAD, deploy, teste ou contagem. Usar commits pequenos de documentacao sem acionar build-request; confirmar por readback os arquivos alterados e relacionar links/SHAs no fechamento. Em falha de acesso ou sincronizacao, sinalizar PENDENTE, sem declarar fechamento completo.
- Preservar pacote `com.tclremote.livre.v2`, signer, OTA V2, OTA legada, Worker/segredos e PR #3 sem merge/deploy/publicacao nao autorizados. Regras de produto e historico anteriores permanecem validos salvo revogacao expressa.

## Referencia vigente (reconciliada em 25/10/2026)
- Ultima release documentada: **AnyTV 2.9.50 / versionCode 79**, [Actions #40250636867](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867), artefato `anytv-v2-2950-signed-update` ID `12621265708`, OTA V2 commit `7ef4e20b6b0e9a414455d8c1babf10664311ea1e` em `feat/tcl-v22-updates-back-bluetooth:updates/android/v2/current.apk`. Evidencia registrada no [checkpoint 2.9.50](docs/qa/ANYTV_2950_HANDOFF_CANONICAL_20261025.md); verificar estado live independentemente antes de proxima release.
- DEV `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 (Draft conforme ultimo checkpoint), fonte principal `overrides/TclStreamingScreens.kt`; testes `scripts/tests/test_streaming_screens_contract.py`; Worker `anytv-resolver/src/worker.mjs`.
- Motor editorial local 40/35/25, ate 50 itens elegiveis, TMDB e filtro brasileiro flatrate. Trakt preparado **NAO ATIVO**; Watchmode **NAO INTEGRADO**; `QA_FISICO=NAO_TESTADO`; `QA_EDITORIAL=NAO_HOMOLOGADO`. Proximo versionCode >79, condicionado a verificacao remota.
- Entradas abaixo com 2.9.39, 2.9.48, 2.9.49 ou anteriores sao **HISTORICAS**, nao definem release vigente.

# AnyTV — Mapa conservador do código
Atualizado em 2026-10-25. Mapeamento por arquivos reais consultados na branch `feat/anytv-p0-device-proof-verifier-20261020` e checkpoint `docs/qa/ANYTV_2950_HANDOFF_CANONICAL_20261025.md`.

## Âncora da revisão
Código da release 2.9.48/code77 ancorado no HEAD de build `faffb1ff8a52bbbab5c9cc25afebac3bc6071914`. Os commits de documentação posteriores não alteram esses blobs de implementação. Conferir HEAD antes de editar.

## Arquivos inspecionados
| Arquivo | Responsabilidade observada | Blob SHA da consulta |
| --- | --- | --- |
| `overrides/TclStreamingScreens.kt` | Composables de catálogo, Streaming, descoberta e interface de filmes/séries. | `71c37376a8a95c4ebe40587cce0ceb66697401a2` |
| `overrides/TclInstalledApps.kt` | Apps reais detectados na TCL, controle remoto e integração de abertura. | `e50466912a7310d254954e00ebc77e92cc82ad57` |
| `scripts/tests/test_streaming_screens_contract.py` | Testes de contrato do Streaming e da integração com apps; seu cabeçalho ressalva teste físico. | `0212237cc160345a2846918e3e3563564915f9f2` |
| `.github/anytv-phone-v2-build-request.json` | Pedido declarativo de publicação da 2.9.48/code77. | `1e252c22f8eec7b6d38337514478a61f19bf1cfb` |
| `.github/workflows/release-anytv-phone-v2-new-package.yml` | Workflow de atualização oficial V2, disparado por alteração no build-request e com preservação de signer. | `4c26d9b117f5cfc21c4a0393e7c316cb0662ec6c` |
| `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md` | Histórico, release, decisões e checklist de homologação. | `5fdabc09d9045554af423a3defcb61b7a0579534` |

## Mudanças rastreadas na release 2.9.48
- `TclStreamingScreens.kt`: páginas Gênero/Anime com lista padrão e quatro seções reais; badges do hero compartilhados sem duplicação; metadados detalhados compactos; estreias de 90 dias via discover multiordenação limitada, Cinema BR separado; detecção de novos provedores BR por snapshots persistidos do mesmo título (primeiro scan faz baseline).
- `test_streaming_screens_contract.py`: regressões de toggle/seções, badges, espaçamento, estreia recente e histórico BR.
- Workflow V2: testes do Worker real e readback HTTPS por SHA imutável do commit da OTA; preservação de signer e cert2026.

## Curadoria 2.9.50 / Worker Trakt opcional
- `overrides/TclStreamingScreens.kt`: ranking de confiança e recência, 40/35/25 preferências, 50 títulos, títulos locais dos favoritos, novas seções, seleção por serviço BR flatrate, classificação minisséries confirmada, sinal opcional Trakt `signals/trakt`.
- `anytv-resolver/src/worker.mjs`: blob `2b7c31762766d27a8b144539a0539625e928e47d`; nova função `traktSignalsRequest` com allowlist, Client ID somente no Worker, resposta limitada TMDB IDs/watcher count, cache. Testes Node offline PASS; deploy e segredo pendentes.
- Release Android 2.9.50/code79 [#40250636867](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867) PASS; APK OTA commit `7ef4e20b6b0e9a414455d8c1babf10664311ea1e`. Sem prova de homologação visual no aparelho e sem Trakt live.

## Integrações identificadas, sem auditoria exaustiva
- App Android utiliza Kotlin/Jetpack Compose; `TclStreamingScreens.kt` concentra a UI dos catálogos.
- Teste de contrato referencia consulta de apps via `request(host, code, "/apps")` e uso de `TclStreamingScreens(apps = allApps...)`.
- O teste registra uso do Worker de catálogo `anytv-link-resolver-pilot.brenoff.workers.dev/v1/catalog/` com `watch_region=BR` e monetização `flatrate`; NÃO implica autorização de deploy.
- Testes de contrato verificam chamadas de abertura de app e título, busca, gênero, favoritos e distinção de ausência de deep link universal.
- O workflow de publicação recupera fonte externa Android TV Remote fixada em commit e aplica scripts da árvore `scripts/` durante a compilação. Não modificar essa cadeia sem inspeção integral.

## Limites e roteiro antes de editar
- Este mapa é **parcial e conservador**: a leitura dos arquivos nesta sessão foi concentrada nas linhas iniciais e nos testes; não constitui inventário integral de símbolos/rotas.
- Antes de alterar UI: consultar o componente inteiro e o teste correspondente, localizar símbolos, chamadas e estado no mesmo HEAD.
- Antes de mexer em app/TV: inspecionar comunicação, contratos e testes `TclInstalledApps.kt`.
- Antes de release: comparar build-request, workflow, assinatura, versão e OTA remota; não disparar publicação apenas para testar documentação.
- Verificar conflitos de SHA após cada alteração; não assumir que caminhos ou regras antigas continuam válidos.
- QA física de layout e navegação permanece NÃO_TESTADO.
