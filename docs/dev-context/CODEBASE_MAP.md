# AnyTV — Mapa conservador do código
Atualizado em 2026-10-25. Mapeamento por arquivos reais consultados na branch `feat/anytv-p0-device-proof-verifier-20261020` e checkpoint `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md`.

## Âncora da revisão
Código da release 2.9.48/code77 ancorado no HEAD de build `faffb1ff8a52bbbab5c9cc25afebac3bc6071914`. Os commits de documentação posteriores não alteram esses blobs de implementação. Conferir HEAD antes de editar.

## Arquivos inspecionados
| Arquivo | Responsabilidade observada | Blob SHA da consulta |
| --- | --- | --- |
| `overrides/TclStreamingScreens.kt` | Composables de catálogo, Streaming, descoberta e interface de filmes/séries. | `a6fd933377cfda94545522ee0a9e1c40d6e9aea9` |
| `overrides/TclInstalledApps.kt` | Apps reais detectados na TCL, controle remoto e integração de abertura. | `e50466912a7310d254954e00ebc77e92cc82ad57` |
| `scripts/tests/test_streaming_screens_contract.py` | Testes de contrato do Streaming e da integração com apps; seu cabeçalho ressalva teste físico. | `fa3fc33ec267cb2ad0feff8dadd225c7d2a9a5ae` |
| `.github/anytv-phone-v2-build-request.json` | Pedido declarativo de publicação da 2.9.48/code77. | `84c893df7fb17a5a18da080923414f1d57eef26c` |
| `.github/workflows/release-anytv-phone-v2-new-package.yml` | Workflow de atualização oficial V2, disparado por alteração no build-request e com preservação de signer. | `b4968f28e08cb5e30d33ff38d83e30d0d0a256dd` |
| `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md` | Histórico, release, decisões e checklist de homologação. | `5fdabc09d9045554af423a3defcb61b7a0579534` |

## Mudanças rastreadas na release 2.9.48
- `TclStreamingScreens.kt`: páginas Gênero/Anime com lista padrão e quatro seções reais; badges do hero compartilhados sem duplicação; metadados detalhados compactos; estreias de 90 dias via discover multiordenação limitada, Cinema BR separado; detecção de novos provedores BR por snapshots persistidos do mesmo título (primeiro scan faz baseline).
- `test_streaming_screens_contract.py`: regressões de toggle/seções, badges, espaçamento, estreia recente e histórico BR.
- Workflow V2: testes do Worker real e readback HTTPS por SHA imutável do commit da OTA; preservação de signer e cert2026.

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
