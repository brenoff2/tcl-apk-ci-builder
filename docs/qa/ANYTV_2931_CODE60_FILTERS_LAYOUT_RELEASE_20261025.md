# AnyTV V2 2.9.31 / code60 — filtros, gêneros e interface

**Data:** 25/10/2026. **Publicação:** assinada e verificável; **homologação física pendente**.

## Pedidos realizados
1. Página de streaming usa o mesmo hero e modelo de listas da Principal, com filtro de gênero dentro do catálogo do serviço (implementação compartilhada em `overrides/TclStreamingScreens.kt`; sem layout alternativo de provedor).
2. Cabeçalho das listas dos gêneros passou a variar de **Filmes e séries** para **Filmes** ou **Séries** conforme `homeContentType`.
3. Restaurado seletor **Grade / Detalhado** para gêneros. Grade: 3 cartazes 2:3 por linha. Detalhado: cartaz com título/sinopse original quando disponível; ordenação existente preservada.
4. Removido subtítulo **Android TV** no header da tela principal do controle em `overrides/TclApprovedHomeScreen.kt`, sem alterar conexão ou nome do dispositivo.
5. Busca local com lupa, X e filtragem imediata em páginas de provedores/gêneros; X sempre visível (desabilitado quando vazio) nas bibliotecas. Conteúdo local filtrado ao digitar.
6. Gêneros sob título dos cards (10sp/cor secundária, como o ano) e dentro do hero compartilhado (11sp/sombra). Nomes vêm exclusivamente de `genreIds` e listas de gêneros reais TMDB movie/tv; gêneros não identificados não são inventados.
7. Testes de contrato atualizados em `scripts/tests/test_streaming_screens_contract.py`, `scripts/tests/test_approved_visual_contract.py` e `scripts/tests/test_tcl_v22_contract.py`, removendo afirmações antigas sobre subtítulo Android TV e incluindo critérios dos novos controles.

## Provas GitHub
- CI unsigned candidato [#40208103739](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40208103739) **SUCCESS** (contratos e compilação Gradle).
- Release oficial [#40208593028](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40208593028): **SUCCESS**; regressões, segurança, compilação Gradle, IME, busca TMDB, assinatura do certificado V2 original, publicação OTA e readback HTTPS.
- APK: `com.tclremote.livre.v2`; `versionName=2.9.31-AnyTV`, `versionCode=60`.
- **SHA256 do APK assinado:** `177b2f422b55858177e661855aea8215119b8a0c0917748de732345167799690`; CI `ANYTV_V2_SIGNER_VALIDATED_...`.
- Commit OTA: **`481d498`** em `feat/tcl-v22-updates-back-bluetooth`, apenas `updates/android/v2/current.apk` alterado comparado à 2.9.30 (`6c49b2e`).
- `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS` observado.
- Artefato assinado de Actions `anytv-v2-2931-signed-update`, ID **12604263131**.
- PR #3 segue DRAFT sem merge; nenhuma alteração em Worker, D1, companion TCL ou OTA legado.

## Atenção
- **A atualização está publicada, porém ainda sem teste visual físico no celular e TCL**. Conferir navegação Principal/Streaming/Gênero, modalidades Filme/Série, Grade/Detalhado, gênero textual e os ícones de busca, inclusive em telas pequenas. Nenhum gate M08/M09 ou F0–F8 deve ser promovido sem prova física.
- Progresso canônico formal permanece **7/11 = 63%**.
