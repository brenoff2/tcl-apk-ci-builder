# ANYTV — Estado verificado e limites | 2026-10-21

Repositório: `brenoff2/tcl-apk-ci-builder`. Desenvolvimento no PR #3 draft (branch `feat/anytv-p0-device-proof-verifier-20261020`); OTA na branch `feat/tcl-v22-updates-back-bluetooth`. `main` preservada.

**Publicações comprovadas:** celular AnyTV 2.9.8, `versionCode=37`, em `updates/android/cert2026/current.apk`, CI build 39661373378 e OTA 39662586924; usuário confirmou celular atualizado. TV AnyTV Connect 2.5, `versionCode=16`, em `updates/tv/cert2026/current.apk`, CI build 39664549476 e OTA 39665565951. Instalação física TV 2.5 não foi confirmada.

**Cloudflare:** leituras nesta conversa confirmaram migrações D1 `device_keys`, `device_nonces`, `device_activations` e Worker com sete bindings, modo `device` e ativação habilitada. Estado vivo exige nova leitura antes de intervenção. Não registrar código de ativação em arquivos.

**Readback operacional em 2026-10-21:** D1 confirmou `active_devices=0`, `activations_used=0`, `pending_codes=1` (não expor código). Portanto ainda não há dispositivo ativado, e nenhum teste E2E pode ser considerado aprovado.

**Pendente:** comprovar ativação 201 e dispositivo em D1, resolver link elegível, e observar o título abrindo na TV; testar falhas, segurança e regressões. CI não equivale a teste físico.

**Próxima entrega:** completar M08 e M09 sem reconstruir APKs desnecessariamente. Bloqueios reais: evidência de aparelho, disponibilidade/autorizações de provedores e eventuais erros reproduzíveis. Não declarar produto final.
