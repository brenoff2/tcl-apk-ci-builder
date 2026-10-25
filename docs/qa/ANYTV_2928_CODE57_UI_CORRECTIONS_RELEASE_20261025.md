# AnyTV 2.9.28/code57 — correções precisas do streaming

**Data:** 25/10/2026. **Release comprovada:** [GitHub Actions #40200384627](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40200384627) COMPLETED/SUCCESS.

## Correções aprovadas e implementadas
- Principal: hero geral restaurado a partir do catálogo, retirado o hero extra de Cinema BR e removida a seção Cinema da Principal; página da categoria Cinema continua disponível no filtro próprio.
- Nota/estrela amarela no **canto superior esquerdo**, inclusive slides de Cinema e hero principal. Sombras preservadas.
- Ícones favoritos/assistido continuam sobre o pôster, com fundo preto semitransparente melhorando o contraste dos estados verde/vermelho.
- Cabeçalhos de páginas: `Principal`, `Cinema`, nome do gênero (ou provedor selecionado), com barra vertical vermelha; mais respiro antes de títulos e entre título/cards.
- Busca interna preserva ícone de lupa, X condicional ao texto preenchido e filtro imediato; cards usam componente compartilhado em três colunas.
- Código em `overrides/TclStreamingScreens.kt` commit `bcd146b8101b59d61ceb05a8ab61f08d9afa5bc7`; teste atualizado `6e4a715eb6ed659413fed20d4eb356b529d60a28`. Sem modificação da disponibilidade dos streamings, chaves persistentes ou lógica de pareamento.

## Testes / publicação
- CI unsigned candidato [#40200181008](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40200181008): consulta anterior verificou testes de contrato PASS; conclusão integral **não afirmada** aqui.
- Release [#40200384627](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40200384627): testes de streaming e segurança, compile release Android, IME, busca TMDB, assinatura original V2 e readback OTA em GitHub HTTPS: **SUCCESS**.
- Pacote `com.tclremote.livre.v2`, versionName `2.9.28-AnyTV`, versionCode **57**.
- SHA256 do APK assinado: `db7eb71ebf2756def0ca55a5860b300f9489ccde57f7e2a1939848b29c4bd1cb` (log `ANYTV_V2_SIGNER_VALIDATED_...`).
- OTA commit `a4f671d`, branch `feat/tcl-v22-updates-back-bluetooth` em `updates/android/v2/current.apk`. `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`: passou. Compare contra a OTA anterior `6b85cfb`: somente `updates/android/v2/current.apk` alterado.
- Artefato Actions `anytv-v2-2928-signed-update` id `12600969501`.

## Gates
- PR #3 permanece DRAFT sem merge, Worker/D1/companion e OTA legado sem mudanças por este release.
- **QA visual físico ainda pendente:** capturas da Principal, Cinema, gênero, provedor e bibliotecas no telefone real. Não confundir CI/assinatura com aparência homologada.
- 7/11 = 63%, M08/M09 IN_QA, M10/M11 BACKLOG, F0–F8 NAO_HOMOLOGADO.
