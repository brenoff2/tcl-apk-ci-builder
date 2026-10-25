# ANYTV — CHECKPOINT CANÔNICO / TRANSIÇÃO — 25/10/2026

## Objetivo e decisões
AnyTV é aplicativo Android para catálogo de filmes/séries e controle remoto da TCL. A página inicial (controle remoto) mostra APENAS apps realmente instalados na TV; o botão Editar edita favoritos instalados. A página de Streaming permite selecionar catálogos de serviços instalados e não instalados, através de Editar com colunas «Na minha TV» e «Outros streamings». Isto não significa possuir assinatura nem app na TV; a reprodução exige app/assinatura confirmada. TMDB/JustWatch região BR, assinatura flatrate; canais adicionais identificados separadamente de Prime/Apple básicos. Identidade de plataforma deduplicada (Max=HBO Max; Netflix, etc.) e logos TMDB em plataformas não instaladas. Streaming selecionado: stroke vermelho.

## Histórico resumido
- 2.9.33/code62: página de streaming «Ver em lista»/«Ver em seções», hero e filmes+séries intercalados, paginação separada, modalidade Grade/Detalhado; melhoras Cinema e gênero.
- 2.9.34/code63: Editar, duas colunas, stroke vermelho, exploração de catálogos não instalados.
- 2.9.35/code64: não excluir ofertas flatrate com Channel/Canal/Addon; Crystal Lake / Universal+ Amazon Channel, sem equiparar ao Prime básico.
- 2.9.37/code66: provedores BR do TMDB, deduplicação Netflix/Max-HBO Max, suporte a serviços fora das antigas aliases.
- 2.9.38/code67: página inicial voltou a mostrar apenas apps TCL instalados; logos TMDB no topo para não instalados; dedup dos logos nos títulos.
- **2.9.39/code68 — ULTIMA OTA PUBLICADA E VALIDADA**: botão contornado «Ver em lista» à direita; subtítulo do catálogo do streaming com barra vermelha e espaçamento igual a Gênero; Grade/Detalhado e Ordenar em linha INDEPENDENTE abaixo do subtítulo; visualização detalhada compartilha um componente entre Streaming e Gênero, deixando o poster sem F/S/ano/logos abaixo (posterOnly=true); texto lateral em ordem título, gêneros, terceira linha com S/F+ano+logos de assinatura confirmada; resumo, dois botões contornados «Trailer» e «Mais detalhes». Ordenação da lista do serviço aplicada aos resultados carregados. Hero, filtros e paginação preservados.
- Período de versões anteriores a 2.9.33 documentado noutros relatórios e histórico Git; não reconstituir fatos não documentados.

## Evidências definitivas da 2.9.39
- DEV branch: `feat/anytv-p0-device-proof-verifier-20261020`, não mesclar automaticamente PR #3 (Draft).
- Alteração funcional `overrides/TclStreamingScreens.kt` commit `271e77563106e9cbbfd8748c8cdc165af00834b0`; testes de contratos atualizados no commit `b7200ca18017a9667d26c47b568e8272a73771a4`; build request `f4f847b2776f8ef99ea584217384fb0ff591d217`.
- Candidate unsigned: GitHub Actions `40234547242` SUCCESS. P0 tests `40234628521` SUCCESS.
- **Signed official OTA: GitHub Actions `40234628535` SUCCESS.** Signed artifact `anytv-v2-2939-signed-update` ID `12614083963`, ZIP artifact size 23479070 bytes. `ANYTV_V2_SIGNER_VALIDATED_eb0c1b1d763d98f863c812a7f1a8700a13171b3fd15ff9c6b6f84527a1ec6baf` (SHA256 do APK assinado).
- OTA independent branch `feat/tcl-v22-updates-back-bluetooth`, commit `0416c61cc26eeb276b228ca326595227cf7bc526`, arquivo `updates/android/v2/current.apk`. Workflow prova `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS` (comparação byte a byte do remoto). OTA anterior 2.9.38 foi substituída. Certificado V2 original preservado, package `com.tclremote.livre.v2`; rota cert2026 legada preservada.
- Artifact: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40234628535/artifacts/12614083963
- Run: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40234628535
- **QA física no dispositivo do usuário não realizada**. Sucesso no CI/OTA não é prova de layout/links no celular.

## Pontos a validar na próxima conversa
1. Abrir Streaming instalado e não instalado; mover entre seções e lista; confirmar botão de alternância no extremo direito, visualizador Grade/Detalhado + Ordenar em linha abaixo do subtítulo, barra vermelha e espaço após hero.
2. Em Detalhado, poster completo sem metadados duplicados; ao lado: título, gêneros, F/S com símbolo e ano e LOGOS, sinopse, Trailer e Mais detalhes como botões separados; clicar Trailer (consulta real; ausência mostra aviso) e Mais detalhes (modal).
3. Ordenar testa listas já carregadas. Conferir «Mostrar mais» com filme e série e mudança de ordenação (não confundir ordenação local com ordenação global do backend).
4. Confirmar logos sem duplicação e BR; editor do controle remoto restrito a apps instalados; lista TMDB «Outros streamings» somente na seção Streaming.
5. Caso encontre regressão, investigar causa diretamente na branch DEV e publicar NOVO versionCode/versão via workflow protegido, sem alterar signer ou OTA legada.
6. Não declarar produto final homologado sem teste manual/visual.

## Preferência operacional explícita
Usuário quer resultado pronto, sem intermináveis «continue» e, quando pede ajustes no app, somente resposta FINAL após APK compilado, assinado e colocado na pasta oficial de atualização. Manter comunicação objetiva; não alegar conclusão antes do readback da OTA.

## Ponteiros de canônico
Este documento em `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md` é o checkpoint da transição; arquivo precedente `docs/qa/ANYTV_2933_PROVIDER_LIST_AND_OTA_DEV_20261025.md` e `docs/qa/ANYTV_STREAMING_EDIT_DUAL_SOURCE_DEV_20261025.md`. Fonte de verdade: histórico da branch, workflow e artefatos, não memória de chat.
