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
