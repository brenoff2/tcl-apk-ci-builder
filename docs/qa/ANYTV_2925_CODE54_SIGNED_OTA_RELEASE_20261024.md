# AnyTV V2 2.9.25/code54 — publicação oficial assinada e OTA verificada

**Data:** 24/10/2026. **Decisão:** usuário autorizou expressamente "Coloque o apk novo para atualizar" após aprovação da nova interface. **Canal:** somente celular V2; companion TCL e Worker sem mudança.

## Resultado comprovado (SUCCESS)

- Fluxo autorizado: [GitHub Actions #40106161667](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40106161667), run HEAD `3e916e7c7bacd8c27440809fdd28a6e3756151d3`, status **completed/success**.
- Release workflow: `.github/workflows/release-anytv-phone-v2-new-package.yml` migrou exclusivamente a identidade V2 para **2.9.25/code54**, com `oldcode < 54`, gate de preservação do legado, recuperação oficial de signatário criptografado via segredo do GitHub, `apksigner verify` e HTTPS byte-readback. Nenhum mecanismo de segurança foi contornado.
- Gatilho: `.github/anytv-phone-v2-build-request.json` com `expected_version_code: 54`, `release_version: 2.9.25-AnyTV`, `ci_revision: 13`. Duas tentativas iniciais ficaram bloqueadas em testes V21/V22 com suposições visuais da interface antiga; testes foram atualizados para **verificar** o design novo, sem retirar testes de segurança/atualizador; terceira execução completou todos os gates.
- Gates que passaram no mesmo workflow: preparação upstream fixada, regressões UI/links/segurança, proteção contra credenciais manuais, streaming, restauração de código de conexão, isolamento TLS, Gradle `assembleRelease`, `RemoteImeWireTest`, pesquisa **live** TMDB para filme e série, `aapt` `com.tclremote.livre.v2` code54, `zipalign`, `apksigner` esquema v2 e certificado original, publicação OTA, comparação remota byte a byte, artifact upload.
- Certificado V2 SHA-256 (mesma identidade de versões anteriores): `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`.
- APK assinado: `AnyTV_V2_2.9.25_UPDATE.apk`; `aapt` mostrou `package: name='com.tclremote.livre.v2' versionCode='54' versionName='2.9.25-AnyTV'`. SHA-256 **`c53596d8f8210bf0a12769481796481a851d00da4c52f544120b85367dd17fe0`**; confirmado pelo job `ANYTV_V2_SIGNER_VALIDATED_...` e novamente após extrair o artefato do GitHub. Tamanho **26.012.572 bytes**.
- Artefato de release assinado: `anytv-v2-2925-signed-update`, id **12564414706**, [Artifacts do run](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40106161667), com APK e `badging.txt`.
- OTA V2 canal oficial: [updates/android/v2/current.apk](https://raw.githubusercontent.com/brenoff2/tcl-apk-ci-builder/feat/tcl-v22-updates-back-bluetooth/updates/android/v2/current.apk). Commit de publicação **`dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112`** na branch `feat/tcl-v22-updates-back-bluetooth`. `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS` após `curl`/comparação binária do APK remoto com o assinado.
- Prova independente por API `git/trees`: `updates/android/v2/current.apk` blob passou de `7a1bbdea9e9628ce15d3c1e9d2debef3c98824eb` (code53, 25.996.188 B) para `917b2f31191b93f63e7ee121f3d7d90d3c8e04e6` (code54, 26.012.572 B). **Canal legado não alterado**: `updates/android/cert2026/current.apk` blob `44c766b433ddb370f4589b87843f1edfd9cd7b7a` (28.347.236 B) antes e depois.
- Rollback histórico referenciado: HEAD anterior da branch OTA `75aa42dff1e8745ba8721fbc0f87c5b8e539cf23`. Não executar rollback automaticamente. Android não permite downgrade silencioso de code54 para code53 sem consequências; rollback requer decisão específica e compatibilidade verificada.

## Aceite funcional restante

- **Release disponível na OTA; não equivale a instalação comprovada no celular.** No telefone com pacote V2, abrir `Configurações > Buscar atualizações`; confirmar que a instalação do code54 foi oferecida, sem perda de dados e conservando certificado. Se o atualizador não oferecer atualização, usar APK assinado disponibilizado no artefato, não o ZIP anterior unsigned.
- Validar em aparelho real: controle grafite e vermelho, direcional e barras laterais, ligação e pareamento, volume e canais, atalhos de apps, banner, busca fullscreen preta e histórico, séries em andamento persistentes, cartões gêneros e paginação 24. Registrar evidências D01–D12/U01–U05.
- Companion TV Connect 2.5/code16 **não atualizado**. Worker, D1, configurações de autenticação e OTA legado não atualizados.
- Progresso canônico P0 **7/11 (63%)**, gates F0–F8 `NAO_HOMOLOGADO`, pois publicação/CI não substitui homologação física.

**Decisão GitHub-first:** ADAPT — atualizar os mesmos arquivos de gatilho e workflow de assinatura/apkreadback que já publicavam com segurança code53, com alteração mínima para code54. Sem signer alternativo, reempacotamento manual, chave nova ou publicação de APK unsigned.
