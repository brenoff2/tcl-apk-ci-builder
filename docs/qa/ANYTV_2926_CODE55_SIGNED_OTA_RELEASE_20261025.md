# AnyTV V2 2.9.26/code55 — release assinada e OTA confirmada (25/10/2026)

## Evidencia observada no GitHub (readback)
- Usuario autorizou explicitamente corrigir o streaming e entregar a versao **2.9.26** na pasta de atualizacao.
- Fonte DEV `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 draft sem merge; request de release `face5839925ad0b9d20538ed9bf2f637b99e9620`.
- Release GitHub Actions [#40187697605](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40187697605): **COMPLETED/SUCCESS**. Gates: regressao e seguranca, contratos de streaming, identidade V2/code55, Gradle assembleRelease, testes IME, busca TMDB real, zipalign, recuperação de certificado V2 criptografado, assinatura APK, publicacao OTA, comparacao HTTP byte a byte e artifact.
- SHA256 do **APK assinado** registrado no log `ANYTV_V2_SIGNER_VALIDATED_23733396391ccb8795949defa5c3d0756c8077ec5d4c8f66495dbae3d385d373`.
- OTA publicada **apenas** em `updates/android/v2/current.apk`, branch `feat/tcl-v22-updates-back-bluetooth`. Log GitHub mostrou commit `91837ae` (`release(anytv-v2): verified signed V2 2.9.26 OTA update`); verificação `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`. Comparação via GitHub confirma exatamente **1 commit** sobre o release oficial anterior `dcc51bad` e **somente `updates/android/v2/current.apk` modificado**.
- Artefato de assinatura [anytv-v2-2926-signed-update](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40187697605), artifact id **12596786317**.
- VersionName: `2.9.26-AnyTV`; versionCode: **55**; package: `com.tclremote.livre.v2`. O mesmo certificado V2 da cadeia de release 2.9.25 foi recuperado e validado dentro do workflow. O arquivo legado `updates/android/cert2026/current.apk` nao foi modificado pelo commit OTA.
 
## Escopo do codigo incluido
- `29958770` e `0ea9546e`: renomeacao de `Continuando` para **`Séries acompanhadas`**, com testes alinhados e chave de persistencia interna preservada.
- `508060c7` e `5cfcff38`: Hero Cinema com gradiente inferior integral e texto com Shadow, contrato 3 colunas para biblioteca.
- `8109881b`, `9a89b2fb`, `a873e3b6`: texto **`Mostrar mais`** em vez de `Mostrar mais 24` e controles OutlinedButton seguindo o estilo arredondado; incremento de pagina permanece 24 itens por acionamento.
- `2e9266cd`, `62a2734b` e `face5839`: versão e workflow de release preparados, solicitação code55 assinada.
- A rota Worker DEV `movie/{id}` **nao** foi implantada; não alegar que o APK publicado dispõe da recuperação online até o novo Worker receber autorização específica.

## Limites honestos
- CI, assinatura e HTTPS byte readback **não equivalem à homologação física**. Testes no telefone e TCL, screenshots de todas as telas, alinhamento e paginacao visual real ficam **NAO_TESTADO** nesta rodada.
- Nao houve merge PR #3, deploy Worker, D1 ou companion TCL nesta release. Gate formal continua **7/11 (63%)**, M08/M09 IN_QA, M10/M11 BACKLOG, F0-F8 NAO_HOMOLOGADO.
- Plano de homologacao de aparelho fisico 2.9.25 segue historico; preparar adendo de aparelho para 2.9.26 antes de promover qualquer gate.

## Como atualizar
No AnyTV instalado com assinatura V2 legitima: abrir **Configurações > Buscar atualizações** e verificar oferta da **2.9.26/code55**. Sem comprovar no aparelho real que a atualização foi oferecida/instalada, marcar esse resultado como PENDENTE.
