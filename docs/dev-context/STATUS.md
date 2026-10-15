# AnyTV — Estado de continuidade tecnica

> Snapshot documental em 15/10/2026, nao homologacao fisica. Confirmar branch, SHA e fontes antes de mudar codigo. A branch do piloto e documental; nao e a branch do produto.

<!-- context-state
repository=brenoff2/tcl-apk-ci-builder
branch=feat/tcl-v22-updates-back-bluetooth
observed_commit=0884ead0e62ed4a1ee773ce4769f579ba33f7a15
observed_at_utc=2026-10-15T23:38:04Z
build_status=ANDROID_2.9.5_CODE34_TV_2.2_CODE13_CI_BUILT_UNSIGNED
signature_status=NEXT_ANDROID_2.9.5_AND_TV_2.2_SIGNED_ZIP_NOT_VERIFIED
ota_status=PUBLISHED_ANDROID_2.9.4_CODE33_AND_TV_2.1_NEXT_STAGING_WAITING_FOR_SIGNED_ZIP_NO_OTA_CHANGE
device_test_status=UNVERIFIED_FOR_NEXT_RELEASE
next_action=Verificar APKs assinados com package, certificados e hashes em ambiente privado antes de staging; sem repetir build concluido
end-context-state -->

## Fases distintas

| Fase | Evidencia | Limite |
| --- | --- | --- |
| Build candidato | Android **2.9.5** (versionCode 34) e TV **2.2** (versionCode 13), CI de compilacao | Artefatos nao assinados; build nao significa OTA |
| Assinatura da candidata | Assinaturas privadas e ZIP assinado ainda nao validados no fluxo | **UNVERIFIED**; nao solicitar segredos no chat |
| OTA manifest vigente | Android **2.9.4** (versionCode 33) e complemento **2.1** | Manifests consultados na branch fonte, NAO teste de dispositivo |
| Homologacao fisica da candidata | **UNVERIFIED** | Nao afirmar funcionamento real |

## Evidencias e links verificaveis

- `build_status`: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38791483823
- `signature_status`: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38794562320 — `STAGING_WAITING_FOR_SIGNED_ZIP_NO_OTA_CHANGE`.
- `ota_status`: https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/tcl-v22-updates-back-bluetooth/updates/android/current.json e https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/tcl-v22-updates-back-bluetooth/updates/tv/current.json
- `device_test_status`: UNVERIFIED, sem prova de homologacao fisica da candidata.
- `release_policy`: https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/tcl-v22-updates-back-bluetooth/docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md
- `functional_contract`: https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/tcl-v22-updates-back-bluetooth/docs/CONTRATO_FUNCIONAL_TCL.md

## Proximas acoes

**Produto:** em fluxo separado, obter as tres assinaturas no ambiente privado; conferir package, versao, certificado e SHA-256 antes de staging/OTA; homologar em dispositivo. Nada disso foi executado por este piloto.

**Este piloto:** AGENTS, STATUS, DECISIONS, CHANGELOG, mapa manual, validador, testes, instrucoes do Projeto ChatGPT, workflow documental e roteiro de canario ja versionados na branch `docs/anytv-chatgpt-context-pilot`. Cartographer permanece **CARTOGRAPHER_BLOCKED** (sem Bun/rede no executor isolado); **nao foi instalado nem executado**. Validacao local de contratos executada; **CI remota PASS** em https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38825464041 (20 testes de contexto/documentacao; nao compilou nem instalou APK). Homologacao em chat novo **NAO_EXECUTADO**. Proxima acao: revisar PR documental, salvar instrucoes no Projeto ChatGPT Web e executar o canario em conversa nova. Nao alterar aplicativos.

`observed_commit` aponta para a branch de desenvolvimento; o commit da branch documental tem SHA diferente.
