# AnyTV V2 2.9.29/code58 — Cinema/ícones, QA e OTA

25/10/2026. Solicitação: corrigir somente o visual dos botões Favoritos/Assistido sobre os pôsteres e fazer Cinema seguir o mesmo padrão visual de hero, fontes e espaçamentos da Principal.

## Entrega em código
- `overrides/TclStreamingScreens.kt`: botões sobre a arte mantidos na posição e com cores de estados existentes, porém **removido fundo circular** preto sob o coração/assistido. Em vez disso usa **o próprio ícone preto 24dp atrás de ícone visível 19dp**, produzindo contraste tipo stroke sem círculo extra (`91c0a1c0`).
- Cabeçalhos das listas Cinema e gênero padronizados com marcador vermelho, espaçamento superior 24dp/inferior 12dp e peso igual ao padrão Principal; a linha `AnyTvOneRowPreview` agora usa título 16sp, separação 12dp, barra vermelha 22dp e margem superior 24dp (`91c0a1c0`, `84354982`).
- Removido `AnyTvCinemaHero` antigo (sem chamadas ativas): Cinema utiliza **o mesmo `HorizontalPager`/hero compartilhado da Principal**, evitando uma implementação visual alternativa (`0d3e37d7`).
- Contratos de teste atualizados e ampliados `d2b913a7`, `5712f043`, `ab7b7575`, `c9270b55`. Mudanças mantidas na branch DEV PR #3 draft, sem merge.
- Não alterado catálogo, votos TMDB, filtros, histórico, Worker, D1, companion TCL, package V2, assinatura ou OTA legado.

## Prova de release
- [Actions #40202285198](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40202285198): **COMPLETED / SUCCESS**, testes de UI/segurança, build Gradle release, IME Remote, TMDB, assinatura V2 recuperada do backup criptografado, push OTA e HTTPS byte readback bem-sucedidos.
- `versionName=2.9.29-AnyTV`, `versionCode=58`, `package=com.tclremote.livre.v2`.
- SHA256 APK assinado: `2b541a96a9cfb5582b419384fd8bf364f21e5654cfec3816e86e0a0288683a69` (linha do CI `ANYTV_V2_SIGNER_VALIDATED_...`).
- Commit OTA `71afa4a` em `feat/tcl-v22-updates-back-bluetooth`, arquivo `updates/android/v2/current.apk`; compare com OTA anterior `a4f671d`: **somente APK V2 alterado**.
- `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS` confirmado no workflow.
- CI DEV unsigned [#40202154017](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40202154017) rodou contratos com PASS; é prova auxiliar, distinta do workflow de release.

## Gates pendentes
QA visual no celular real ainda não realizado nesta execução. Validar screenshot dos ícones com fundo claro/escuro e do slide Cinema após atualização. M08/M09 IN_QA, M10/M11 BACKLOG; 7/11, F0-F8 NAO_HOMOLOGADO. Não confundir CI e readback OTA com homologação física.
