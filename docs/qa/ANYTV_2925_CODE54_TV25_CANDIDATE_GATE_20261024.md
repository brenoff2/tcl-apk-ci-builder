> **PROVA ADICIONAL DE PARIDADE TV — 24/10/2026:** comparado o commit do build TV aprovado `339ae5666184e6c4545a06a4ddef8c93d0019f87` com o HEAD DEV após correções Android; blobs SHA de `TvAppBridge.java`, `prepare_companion_safe.py`, `test_safe_tv_companion.py`, `anytv-tv25-compile.yml` e `TclUpdateManager.java` são **idênticos** nos dois commits. O artefato unsigned TV 2.5/code16 do run #40060021841 continua representativo da fonte atual; não existe mudança de protocolo TV que justifique nova compilação. Isto NÃO comprova compatibilidade no hardware ou identidade do companion instalado.

> **FIX CONCRETO DE NAVEGAÇÃO — 24/10/2026:** `LaunchedEffect(openedSection)` agora reinicia `catalogScrollState.scrollToItem(0)` tanto ao abrir quanto ao voltar de página exclusiva pelo botão Ver mais. Antes, reaproveitava a rolagem da capa, podendo exibir uma página vazia se o índice fosse superior à quantidade de linhas disponíveis. Código `03db53481a`, teste `bea4b54582`.

# AnyTV 2.9.25/code54 + AnyTV Connect TV 2.5/code16 — gate de candidato

Data: 24/10/2026. Escopo: provas de desenvolvimento; **nenhum deploy, assinatura ou instalação em dispositivos**.

## Estado e proveniência

- Celular DEV: PR #3 draft, branch `feat/anytv-p0-device-proof-verifier-20261020`. `.github/workflows/anytv-v2-candidate-compile.yml` alterado em `919444cb` para verificar efetivamente `com.tclremote.livre.v2`, `versionCode=54`, `versionName=2.9.25-AnyTV`. Correção da navegação `Ver mais` em `03db5348` reinicia rolagem ao entrar/sair da página e regressão em `bea4b545`. CI candidato com artefato de auditoria unsigned (identidade, SHA256, aviso NÃO INSTALAR) adicionado em `37184150`. **Resultado final da execução ainda requer readback**.
- A nova trilha de upload `anytv-v2-2925-code54-unsigned-audit` **não** recupera certificados, não assina APK, não altera o Worker, não publica OTA, nem atualiza a TV. Um APK unsigned não pode substituir a versão 2.9.24 instalada preservando assinatura e dados.
- TV DEV: [workflow #40060021841](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40060021841) finalizado SUCCESS. Pacote verificado `com.tclremote.tvcompanion` code16/2.5, auditoria de permissões e artefato unsigned `anytv-tv25-unsigned-audit` (ID `12546823342`) ainda válido no readback do GitHub. Isto comprova **compilação**, não instalação da TV.
- Rotas cliente `overrides/TclInstalledApps.kt` e servidor `overrides/tv_safe/TvAppBridge.java` coincidem em `GET /apps`, `POST /launch`, `POST /title-search` e `POST /title-open`. A abertura de ficha retorna `detail_requested` como tentativa, e o cliente tem fallback de busca; não prometer reprodução automática.
- Última publicação V2 celular: **2.9.24/code53**, assinada e no OTA. Workflow oficial `.github/workflows/release-anytv-phone-v2-new-package.yml` e `.github/anytv-phone-v2-build-request.json` continuam code53/2.9.24, **não alterados**, sujeitos às aprovações oficiais. Canal legacy OTA e certificados TV intactos.

## Evidência de regressão e aceite

- Inspecionar Actions de code54: contratos UI/segurança, Gradle assembleRelease, verificação `aapt` de package/code/name, SHA256 e artifact readback; registrar URL, SHA do commit e resultado após a conclusão.
- No celular físico com assinatura legítima: confirmar atualização preservando dados/certificado, logo removida do alto de Streaming, destaque Cinema BR isolado, prévias mundiais/Em breve, banner Cinema identificado, gêneros com cartões 158dp, navegação `Ver mais` iniciando no topo e retorno, lotes de 24 sem duplicações, favoritos/assistidos intactos, rockers sem volume fictício.
- Na TCL real: identificar package/versionCode/certificado do companion instalado antes de qualquer substituição; parear com 8 dígitos, ler apps, abrir app, buscar título, abrir ficha nos provedores homologados sem afirmar play, testar Bluetooth/canal/volume; registrar resultados D01–D12/U01–U05.
- Para eventual release 2.9.25/code54: autorização explícita + editar workflow **oficial protegido pela via permitida** e solicitação de build; validar APK assinado com **certificado V2 original**, OTA `updates/android/v2/current.apk`, byte-readback HTTPS + rollback. **Não contornar proteção**, não distribuir unsigned como atualização.
- Para TV: não assinar com certificado errado, não reinstalar por cima, não habilitar Accessibility insegura; qualquer atualização oficial usa identidade/certificado corretos e exige inspeção do estado real da TCL.

## Progresso

P0: 7/11 (63%) marcos técnicos. M08/M09 ainda IN_QA; M10/M11 em BACKLOG. F0–F8 `NAO_HOMOLOGADO`. Um CI verde não altera esse estado.
