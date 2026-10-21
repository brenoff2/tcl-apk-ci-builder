# ANYTV — Estado verificado e limites | 2026-10-21

Repositório: `brenoff2/tcl-apk-ci-builder`. Desenvolvimento no PR #3 draft (branch `feat/anytv-p0-device-proof-verifier-20261020`); OTA na branch `feat/tcl-v22-updates-back-bluetooth`. `main` preservada.

**Publicações comprovadas:** celular AnyTV 2.9.8, `versionCode=37`, em `updates/android/cert2026/current.apk`, CI build 39661373378 e OTA 39662586924; usuário confirmou celular atualizado. TV AnyTV Connect 2.5, `versionCode=16`, em `updates/tv/cert2026/current.apk`, CI build 39664549476 e OTA 39665565951. Instalação física TV 2.5 não foi confirmada.

**Cloudflare:** leituras nesta conversa confirmaram migrações D1 `device_keys`, `device_nonces`, `device_activations` e Worker com sete bindings, modo `device` e ativação habilitada. Estado vivo exige nova leitura antes de intervenção. Não registrar código de ativação em arquivos.

**Readback operacional em 2026-10-21:** D1 confirmou `active_devices=0`, `activations_used=0`, `pending_codes=1` (não expor código). Portanto ainda não há dispositivo ativado, e nenhum teste E2E pode ser considerado aprovado.

**Pendente:** comprovar ativação 201 e dispositivo em D1, resolver link elegível, e observar o título abrindo na TV; testar falhas, segurança e regressões. CI não equivale a teste físico.

**Próxima entrega:** completar M08 e M09 sem reconstruir APKs desnecessariamente. Bloqueios reais: evidência de aparelho, disponibilidade/autorizações de provedores e eventuais erros reproduzíveis. Não declarar produto final.

**Readback D1 posterior (2026-10-21):** um dispositivo com chave ativa, um codigo consumido, nenhum codigo pendente. O banco confirma vinculacao entre ativacao e chave ainda valida, embora o aplicativo tenha indicado rejeicao. Nao emitir novo codigo nem reinstalar antes do teste autenticado cliente-servidor. M08 permanece IN_QA ate comprovacao E2E.

**Correção operacional 2026-10-21:** ativação idempotente somente para mesmo dispositivo/chave/prova criptográfica; commit `c75d6c60369d0fbf7b172747040c5baf032a679a`, testes e contrato corrigidos até `a1981bada5c9db4497a92ae60df4d47fd99c5a1b`; CI de backend run `39686240739` success. Cloudflare Worker atualizado e readback do conteúdo confirmou a correção e os sete bindings preservados, último deploy `2026-10-21T22:25:14Z`. O marco M08 permanece `IN_QA`, sem retorno positivo comprovado no próprio celular. Repetição do mesmo código só pode confirmar a ativação já registrada para a mesma chave; demais dispositivos permanecem proibidos.

**Nova frente: Discovery Home V3 (2026-10-21):** proposta de catálogo global, destaques, estreias, populares, notas TMDB, trailers oficiais, relacionados e filtro opcional por streaming, preservando páginas dos provedores. Contrato em `docs/ANYTV_DISCOVERY_HOME_V3.md`. Allowlist do Worker alterada somente em branch (commit `80df0a9`), testes acrescentados (commit `5b666f1`). Ainda sem UI no APK nem deploy; não confundir início de engenharia com entrega funcional. Progresso P0 7/11 permanece até nova baseline aprovada.

**Discovery Home V3 — implementação inicial em branch (2026-10-21):** `overrides/TclStreamingScreens.kt` tem entrada global Descobrir com destaques em slides e gestos horizontais, seções de catálogo, pesquisa global de filme/série, notas TMDB, trailer YouTube sob clique, relacionados e seleção de streaming instalado para abertura. HEAD de implementação `e86fd062b4e1b458df385c0046f7a732520f9e08`. CI Android run `39690224259` ainda pendente na última leitura. Nenhum APK novo assinado/publicado e nenhum deploy Worker para esta frente. Não considerar funcional concluída ou validada fisicamente.

**Discovery V3 — filtro opcional de plataformas (2026-10-21):** commit `ed28bc13e375eb15ab07a25d109bef7e82dab271` adiciona chips Todos/servicos realmente instalados e consultas por provedor com regiao BR para filmes/series; buscar global segue independente. CI de Android `39690366612` ja passou para a versao inicial, mas a nova revisao com filtros ainda precisa de aprovacao propria (run `39695232448`). Sem assinatura, OTA nem deploy de Worker V3. Percentual de P0 nao alterado.

**Discovery V3 — revisão trailer 2026-10-21:** compilação anterior dos filtros `39695232448` concluída com sucesso, artefato unsigned audit `12390910789`. Fallback de metadados pt-BR -> en-US para trailer exclusivamente no código da branch: Worker `4a5075a`, Compose `575be9d`, testes `e0db6b9`. CI `39696405547` (backend) e `39696405302` (Android) iniciadas e sem resultado na publicação desta nota. Nenhum deploy do Worker da V3 ou APK OTA autorizado/executado nesta revisão; conservar produção P0.
