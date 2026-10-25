> **RELEASE POSTERIOR CONFIRMADA — 25/10/2026:** AnyTV **2.9.40/code69**, maintenance build sem alterações funcionais de UI; GitHub Actions [#40236534156](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40236534156) **COMPLETED/SUCCESS**. Compilação, testes, assinatura V2, publicação na OTA oficial e byte-readback remoto **SUCCESS**. Artefato assinado `anytv-v2-2940-signed-update` ID `12615786069` ([download](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40236534156/artifacts/12615786069)). OTA `feat/tcl-v22-updates-back-bluetooth:updates/android/v2/current.apk`, commit `507caa4f0880918f0212ca8102268fce54adb806`. Homologação física Android/TCL **NÃO_TESTADO**. As referências a 2.9.39 abaixo documentam estado anterior, não a última versão. Próxima release precisa de versionCode >69.

# AnyTV — Estado de desenvolvimento
Atualizado em 2026-10-25. Registro documental, sem alteração do aplicativo ou publicação.

## Fontes e identificação
- Repositório: https://github.com/brenoff2/tcl-apk-ci-builder
- Branch de desenvolvimento: `feat/anytv-p0-device-proof-verifier-20261020`
- HEAD da branch observado via PR #3 durante a preparação: `905117a9cfa9f39bc0b82d3afb28cb149a86f181` (registro pontual; atualizar em cada nova retomada).
- PR #3: https://github.com/brenoff2/tcl-apk-ci-builder/pull/3 — aberto e Draft; não fazer merge automático.
- Checkpoint principal: `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md`.
- `AGENTS.md` contém entradas históricas; seus cabeçalhos antigos com 2.9.31 não substituem o checkpoint 2.9.39.

## Última entrega confirmada no checkpoint
- Versão: AnyTV 2.9.39, versionCode 68, pacote `com.tclremote.livre.v2`.
- Build candidata unsigned: Actions #40234547242 — SUCCESS (registrado no checkpoint).
- Testes P0: Actions #40234628521 — SUCCESS (registrado no checkpoint).
- Assinatura/publicação: Actions #40234628535 — SUCCESS (registrado no checkpoint).
- Artefato assinado: `anytv-v2-2939-signed-update`, ID `12614083963`.
- SHA-256 do APK assinado: `eb0c1b1d763d98f863c812a7f1a8700a13171b3fd15ff9c6b6f84527a1ec6baf`.
- Branch OTA: `feat/tcl-v22-updates-back-bluetooth`; arquivo `updates/android/v2/current.apk`; commit registrado `0416c61cc26eeb276b228ca326595227cf7bc526`.
- Byte-readback remoto do APK: SUCCESS conforme checkpoint; certificado original V2 e OTA legada cert2026 preservados.
- Última run de testes adicional apontada no checkpoint: #40235705174 — resultado NÃO VERIFICADO nesta atualização.
- Dois commits de regressão posteriores à publicação: `7d7e49c530095b321f79921c0355c5418a9f74ca`, `487711297c5c319810d1c5e8ed6f77a3c0771e29`. Sem nova OTA documentada.
- Homologação visual/funcional no aparelho do usuário: NÃO TESTADO; não considerar aprovada por CI.

## Entrega funcional descrita no canônico
- Home do controle remoto lista apenas aplicativos efetivamente instalados na TCL.
- Página Streaming permite explorar serviços instalados ou não, com seleção persistente, logos TMDB BR e deduplicação de provedores (Max/HBO Max).
- Disponibilidade BR deve diferenciar flatrate, canais adicionais, compra/aluguel, cinema e títulos futuros.
- Interface 2.9.39: botão Ver em lista/Ver em seções, subtítulo com barra vermelha, controles Grade/Detalhado/Ordenar separados, cartão detalhado compartilhado, dois botões distintos Trailer e Mais detalhes.
- Ordenação do catálogo de Streaming refere-se aos itens carregados, não à totalidade do serviço.

## Próxima ação e critério de fechamento
1. Conferir HEAD DEV e estado remoto da OTA antes de qualquer mudança.
2. Homologar 2.9.39 em aparelho: layouts, ações, paginação, ordenação, apps instalados e catálogos não instalados, sem duplicação.
3. Registrar evidências de teste físico; se houver regressão, diagnosticar na fonte e adicionar testes.
4. Mudança funcional requer nova versão/versionCode >68, CI, compilação, assinatura, OTA autorizada e readback, sem afetar signer/OTA legada.
5. Não afirmar produto homologado sem testes no dispositivo.
Links de referência: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40234628535 ; https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/anytv-p0-device-proof-verifier-20261020/docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md
