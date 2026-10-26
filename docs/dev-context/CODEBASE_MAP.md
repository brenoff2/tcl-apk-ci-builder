# AnyTV — Mapa conservador do código
Atualizado em 2026-10-25. Mapeamento por arquivos reais consultados na branch `feat/anytv-p0-device-proof-verifier-20261020` e checkpoint `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md`.

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
