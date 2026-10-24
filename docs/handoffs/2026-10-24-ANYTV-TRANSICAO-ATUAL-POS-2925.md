# PROJETO ANYTV — TRANSIÇÃO INTEGRAL, ESTADO VERIFICADO E CONTINUIDADE

> **QA RESIDUAL 24/10/2026 (SOMENTE DEV; SEM ASSINATURA/OTA):** auditoria do codigo real pos-2.9.25 identificou e corrigiu: pagina dedicada de Ver mais limitada aos primeiros 24 em secoes editoriais/provedores; marcado assistido por card sem snapshot duravel; continuidade da paginacao quando filtros reduzem o primeiro lote. Codigo e regressao no commit [9ac3529f](https://github.com/brenoff2/tcl-apk-ci-builder/commit/9ac3529f7949e9d721b6ef875bd3791f237d1c61), mantendo AnyTvPosterCard 2:3, tres colunas e icones originais. CI unsigned #40119630330 disparado (resultado ainda NAO_CONFIRMADO nesta nota); proof #40119630288 SUCCESS. QA visual Android/TCL NAO_TESTADO; M08/M09 IN_QA; M10/M11 BACKLOG; 7/11=63%, F0-F8 NAO_HOMOLOGADO. OTA V2 assinada 2.9.25/code54 continua publicada, branch de distribuicao dcc51bad; PR #3 DRAFT/sem merge. Relatorio: [QA residual](../qa/ANYTV_20261024_REVIEW_RESIDUAL_POST_CODE54.md).


**Checkpoint:** 24/10/2026, após a homologação de CI da padronização visual pós-release 2.9.25/code54. **Projeto:** ANYTV. **Objetivo deste documento:** permitir que outro chat retome exatamente o trabalho sem depender da memória informal do diálogo. Este documento prevalece sobre cabeçalhos históricos de 2.9.12 a 2.9.24. Verificar sempre o GitHub ao vivo antes de afirmar versão, SHA ou gate. Não guardar códigos individuais de ativação, PIN, tokens nem segredos no repositório.

## 0. RESUMO EXECUTIVO — PRIMEIRA MENSAGEM DO PRÓXIMO CHAT

Continuar o PROJETO ANYTV, não recomeçar. O AnyTV é um aplicativo Android para controlar TCL/Android TV por rede local e pareamento TLS, com direcional, volume/canal, teclado IME, Bluetooth, launcher de apps reais, catálogo Discovery Home V3 de filmes/séries por TMDB, streaming no Brasil, Cinema, favoritos, assistidos, séries acompanhadas, abertura segura de títulos, companion AnyTV Connect na TV e resolvedor Cloudflare Worker/D1.

**A última versão do telefone ASSINADA e PUBLICADA é AnyTV V2 2.9.25-AnyTV, versionCode 54, applicationId com.tclremote.livre.v2.** GitHub Actions oficial de release #40106161667 completou SUCCESS com certificado V2 original, validação de IME/catálogo, APK assinado, OTA e readback HTTPS byte a byte. Canal oficial: feat/tcl-v22-updates-back-bluetooth / updates/android/v2/current.apk, commit OTA dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112. Artefato assinado #12564414706, APK SHA-256 c53596d8f8210bf0a12769481796481a851d00da4c52f544120b85367dd17fe0; certificado V2 SHA-256 fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9. O canal legado updates/android/cert2026/current.apk não foi modificado.

**EXISTE UMA REVISÃO MAIS NOVA EM DEV, POSTERIOR À OTA 2.9.25/code54, QUE AINDA NÃO FOI ASSINADA/PUBLICADA.** É a unificação visual solicitada pelo usuário usando a PRIMEIRA referência aprovada de grade de Ação/página principal. Todas as telas Streaming passaram a usar o cartão único AnyTvPosterCard, pôster 2:3 completo com ContentScale.Fit, três filmes/séries por linha, mesma hierarquia ano/nota/check/coração/título/ícones F/S e plataformas ORIGINAIS; os trilhos Cinema, Em breve, internacionais e da capa mostram três cartões visíveis, preservando rolagem horizontal; Favoritos, Assistidos, Continuando (antigo Em andamento), gêneros, Cinema BR, Ver mais, busca global e relacionados estão padronizados. Os títulos seguem 24 por lote. Em andamento foi corrigido contra desaparecimento mediante dados duráveis e busca própria. **Não gerar mais mockups nem imagens; modificar apenas código.**

Essa revisão pós-release passou no CI unsigned DEV [#40114130735](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40114130735): completed/SUCCESS, fonte/testes commit b4fecc63f744d66b8240d091db0a427e338e67ad, 51 contratos Streaming, 2 testes de não embutir credenciais, 11 contratos visuais, Gradle assembleRelease SUCCESS, package/code54 unsigned e artefato #12567737040. A prova isolada [#40114275545](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40114275545) foi SUCCESS. Em 24/10/2026 foi confirmada **paridade exata de hashes GitHub** entre a revisão compilada e o HEAD DEV para TclStreamingScreens.kt, os testes e workflow, além de Home, D-pad, apps reais e playback. **Esse unsigned NÃO é atualização instalável e NÃO deve substituir a versão assinada 2.9.25/code54.**

**Próximo trabalho correto:** confirmar a aparência e o comportamento reais dessas telas em aparelho Android, corrigir discrepâncias que ainda se manifestarem, completar QA na TCL e somente então, se autorizado, preparar nova release V2 com versionCode SUPERIOR a 54, seguindo o certificado original e OTA exclusiva. Não reutilizar code54 para nova versão assinada. P0 permanece 7/11 marcos (63%); M08/M09 IN_QA, M10/M11 BACKLOG, gates F0 a F8 NAO_HOMOLOGADO.

## 1. REPOSITÓRIO, BRANCHES, GATES E ARQUIVOS AUTORITATIVOS

- **GitHub/cânone único:** https://github.com/brenoff2/tcl-apk-ci-builder
- **DEV:** feat/anytv-p0-device-proof-verifier-20261020; **PR #3** https://github.com/brenoff2/tcl-apk-ci-builder/pull/3 — OPEN/DRAFT, não mesclado. HEAD observado antes dos commits deste handoff: a289fd759475971705509e84c2d336be4611c2a6. O HEAD muda com commits documentais; no próximo chat reler ao vivo.
- **Distribuição V2 telefone:** feat/tcl-v22-updates-back-bluetooth / updates/android/v2/current.apk; OTA SHA de commit dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112 conforme readback 24/10.
- **Distribuição legado:** updates/android/cert2026/current.apk; conservar bytes/assinatura/rota; blob histórico inalterado 44c766b433ddb370f4589b87843f1edfd9cd7b7a.
- **Identidade atual telefone:** com.tclremote.livre.v2, APK signed 2.9.25/code54. Não confundir com legado com.tclremote.livre e sua identidade diferente.
- **Companion TV:** com.tclremote.tvcompanion, linha técnica AnyTV Connect 2.5/code16. Build TV #40060021841 SUCCESS com artefato unsigned #12546823342; fontes do companion comparadas idênticas ao build aprovado. **Instalação, versão e certificado realmente existentes na TCL ainda precisam de prova física.**
- **Primeira leitura obrigatória:** AGENTS.md; docs/handoffs/CURRENT_HANDOFF.md; este documento; docs/handoffs/NEXT_CHAT_FIRST_MESSAGE.md; docs/roadmap/PROJECT_PROGRESS.json; docs/STATUS.md. Histórico guardado em docs/handoffs/2026-10-23-anytv-full-transition.md, docs/handoffs/2026-10-23-ANYTV-V2-2922-TRANSICAO-ATUAL.md e docs/handoffs/2026-10-24-ANYTV-TRANSICAO-INTEGRAL-2924-2925.md. Cabeçalhos de release antigo são HISTÓRICOS, não a versão atual.
- **Fonte principal:** overrides/TclStreamingScreens.kt; overrides/TclApprovedHomeScreen.kt; overrides/TclResponsivePad.kt; overrides/TclInstalledApps.kt; overrides/PlaybackTransportBar.kt; scripts/apply_tcl_sticky_controls.py. Testes: scripts/tests/test_streaming_screens_contract.py; scripts/tests/test_approved_visual_contract.py. Integração TV: overrides/tv_safe/TvAppBridge.java; scripts/prepare_companion_safe.py.
- **CI DEV unsigned:** .github/workflows/anytv-v2-candidate-compile.yml. **Release autorizado e assinado:** .github/workflows/release-anytv-phone-v2-new-package.yml, gatilho .github/anytv-phone-v2-build-request.json. Último pedido oficial contém 2.9.25/code54, ci_revision=13, e deve ser EVOLUÍDO com segurança para futura release; jamais republicar source novo sob code54.
- **Backend:** anytv-resolver/src/worker.mjs, módulo de requests/provas de dispositivo, Cloudflare D1 e Worker anytv-link-resolver-pilot. Readback histórico de 24/10: versão Worker 58fe4b33-6ee2-4841-8587-c28acaaad435, deployment 25de8328, 100% tráfego. Esse estado não foi reconferido neste handoff; para mudanças no backend, fazer leitura atual. Cinco bindings necessários preservados, sem mostrar segredos.
- **Roadmap:** 7/11 = 63% de marcos históricos aceitos, NÃO de homologação física. M01–M07 ACCEPTED; M08 ativação em dispositivo real IN_QA; M09 abertura real de título na TCL IN_QA; M10 homologação integrada e rollback BACKLOG; M11 estabilidade e aceite final BACKLOG. Gates F0–F8 permanecem NAO_HOMOLOGADO.

## 2. HISTÓRICO ESSENCIAL DE DECISÕES E IMPLEMENTAÇÕES

### 2.1 Arquitetura, assinatura, conexão e identidade

1. Projeto nasceu do controle TCL/Android TV e evoluiu para launcher, catálogo e abertura de conteúdo. A 2.9.8/code37 funcionava fisicamente na TCL segundo o usuário; 2.9.10–2.9.12 tiveram relatos de TLS/SSL e perda de estado HOST/PIN após tentativa de reparo. Histórico detalhado nos handoffs 23/10, não confundir com correções de layout.
2. Legado usa pacote com.tclremote.livre e canal cert2026. Por decisão explícita, V2 tem **applicationId distinto**, novo signatário V2 sob custódia cifrada do GitHub e OTA isolada v2/current.apk, sem sobrescrever nem pedir desinstalação do legado. ADR-0003 e ADR-0004 preservam essa decisão. Não criar outro certificado e não divulgar segredo.
3. Linha V2 passou por releases 2.9.13 a 2.9.24 com sucessivas correções de controle, busca, marca e layout. Último release publicado em 24/10, devidamente autorizado pelo usuário: 2.9.25/code54, run #40106161667.
4. Ativação/links usam prova de identidade do dispositivo, autorização governada no servidor e status seguros; nunca reutilizar códigos individuais nem simular autorização. Device proof e testes isolados PASS não são teste físico. Links resolvidos devem distinguir **abrir app**, **busca**, **abrir ficha** e **reprodução** — não prometer playback se só existe ficha/busca.
5. Worker/D1, fonte de ofertas comerciais Movie of the Night, TMDB, referências de JustWatch e disponibilidade Brasil possuem papéis diferentes. Não inventar catálogo, streaming futuro, ranking, provedores nem direitos de acesso; não burlar DRM.
6. Pedido do usuário de uso pessoal sem quotas artificiais: foram retirados limites internos 200/mês, 20/min/IP e os de ativação, preservando regras contratuais de fornecedores e segurança do sistema. Não reintroduzir quotas internas silenciosamente.

### 2.2 Discovery Home V3 e conteúdo

1. A tela Streaming abre uma home de descoberta **global e editorial**, sem exigir escolha prévia de provedor; apps instalados e provedores de assinatura são filtros e atalhos opcionais. Cada serviço mantém página própria. Filmes e séries têm filtros próprios e gêneros.
2. **Cinema deve ficar separado dos streamings.** Cinema BR usa TMDB movie/now_playing region=BR, Cinema pelo mundo é trilho editorial internacional, e Em breve · Cinema é distinto de Em breve · Streaming. Somente estreias futuras nas áreas Em breve; prateleiras ordinárias não misturam títulos ainda não lançados. Uma única seção Cinema na home, com próprio hero e resultados Brasil, além de filtro Cinema.
3. Indicadores azul F/Cinema e vermelho S conforme UI aprovada. Os símbolos e **ícones originais dos provedores** já estão corretos: não trocar, ampliar ou redesenhar. Ofertas flatrate BR são específicas; serviço instalado não significa conteúdo disponível. Nota/volume/rating não são inventados.
4. Hero da capa usa imagem real TMDB, texto de filme e dados em sobreposição com gradiente para leitura. O usuário pediu também sombra/blur nas letras diante de fundo claro e um filtro organizado, mas esses **detalhes devem ser conferidos na UI real antes de declarar perfeitos**; não improvisar imagem nova.
5. A partir da referência enviada para o controle: fundo grafite quase preto (#08090D), vermelho #E9003B, nome da TV em duas linhas, ações circulares, seis atalhos de apps **realmente instalados**, banner dinâmico, mídia em duas faixas, D-pad grande até 304dp com OK em aro vermelho e cinco ações inferiores. Volume e canal mantêm botões reais sem indicador de percentual fictício; não introduzir comandos falsos.

### 2.3 Catálogo, navegação, acompanhamento e persistência

1. **Ver mais** deve abrir uma página de gênero ou seção, nunca expandir verticalmente a própria prateleira na home. A entrada/saída dessa página reinicia posição de rolagem; Cinema abre seu filtro real.
2. **24 títulos por lote**, tanto prateleiras quanto páginas de gênero. Como TMDB fornece lotes-fonte de 20, o cliente consolida, filtra e deduplica antes de recortar 24; Mostrar mais 24 preserva continuidade, favoritos e páginas parciais.
3. Busca global abre **tela inteira preta sobreposta**, sem empurrar a capa; controles diferentes de FECHAR e LIMPAR TEXTO. Resultados roláveis pelo dedo e histórico inteligente local de até 10 pesquisas explícitas/selecionadas, com remoção e limpeza.
4. A página originalmente **Em andamento** apresentava séries intermitentes: IDs permaneciam armazenados, mas cartões dependiam de cache editorial e favoritos. Corrigido com anytv_watching_titles_v1 e recuperação de legados por rota TV, preservação dos snapshots saved_title_ na rotina de limpeza, mudança de aba sem rolagem antiga, busca local. Novo nome **Continuando**.
5. Páginas Favoritos, Assistidos e Continuando precisam manter títulos e dados após reinício; notas/votos reais TMDB agora persistem em favoritos/snapshots/acompanhamento, sem criar notas ausentes.

### 2.4 Decisão visual MAIS RECENTE (já implementada e compilada, mas não publicada)

O usuário mostrou capturas reais da página principal, gênero Ação, Cinema, Continuando, Assistidos e Favoritos; relatou capa vertical cortada, variação de tamanho, desalinhamento de ano/nota/canais/título, duas colunas em Continuando e sliders de Cinema fora do padrão. O primeiro conjunto de propostas visuais geradas alterou os ícones existentes e não foi aprovado. **A PRIMEIRA REFERÊNCIA da grade Ação/página principal tornou-se o modelo canônico.** Última ordem: “Use o exemplo 1 e pode fazer. Corrija todas as páginas. E nao me mande mais imagem. Corrija”.

A implementação final usa **um único AnyTvPosterCard** em overrides/TclStreamingScreens.kt:
- Pôster na proporção real 2:3, ContentScale.Fit, sem crop ou altura vertical fixa que corte a capa.
- Sempre três títulos por linha em gêneros, Cinema em cartaz no Brasil, Ver mais, Favoritos, Assistidos, Continuando, busca global e títulos relacionados.
- Trilhos horizontais de cinema internacional, Em breve, providers e home com **três cartões inteiros visíveis** e rolagem, não mini-pôsteres diferentes.
- Ano · nota TMDB / check assistido / coração favorito; título e pequenos badges originais F/S e streamings em posição/espaçamento padronizados.
- Sem substituir logos ou nomes das plataformas, sem trocar fornecedor, sem misturar Cinema e streaming, sem modo de lista vertical que corte capa.
- A busca específica de Continuando aparece no topo, não a busca global. Grade da biblioteca preserva lotes de 24, favoritos e dados.
- Hero panorâmico é distinto do pôster 2:3 por intenção editorial; não aplicar crop de hero às capas do catálogo.

**Evidência mais forte:** CI unsigned DEV #40114130735 completed/SUCCESS para fonte/testes b4fecc63f744d66b8240d091db0a427e338e67ad; 51 testes Streaming + 2 de credenciais + 11 visuais; Gradle SUCCESS; unsigned V2 code54 audit #12567737040. Prova #40114275545 SUCCESS. Comparação de blobs GitHub do código compilado vs HEAD DEV da conversa mostrou identidade em 7 arquivos de fontes, testes e workflow. Nenhuma mudança funcional nova fora desse build foi observada neste readback. Isso **não prova a renderização na tela física**, que permanece pendente.

## 3. ONDE EXATAMENTE O PROJETO PAROU

**Não está parado em meio a uma edição incompleta do cartão.** A revisão de uniformização visual que estava sendo implementada no chat anterior já recebeu commits adicionais e foi compilada/aprovada em CI. O próximo chat começa em **QA visual e decisão do próximo incremento**, não deve refazer todo o componente ou mexer nos ícones por imaginar que a rodada falhou.

- Fonte da revisão pós-OTA no PR #3: última referência de código compilado b4fecc63f744d66b8240d091db0a427e338e67ad. HEAD observado antes do registro de transição a289fd759475971705509e84c2d336be4611c2a6 (somente commits de documentação após revisão). Reabrir PR e comparar SHA.
- Publicado no canal oficial: 2.9.25/code54. Não contém necessariamente as **correções posteriores** das três colunas. Somente candidate unsigned do código posterior.
- Nenhum release novo foi pedido depois da ordem “corrija todas as páginas”; o último pedido é agora de TRANSIÇÃO DOCUMENTADA. **Não publicar, assinar, fazer merge, deploy de Worker ou atualizar companion como efeito colateral deste handoff.**
- Homologação física do telefone e TCL não foi demonstrada para essa revisão; preservar F0–F8 como não homologado.
- Itens que exigem atenção especial na verificação visual: largura de três cartões em aparelhos estreitos, pôster inteiro sem corte, distribuição de metadados e selos, títulos de 2–3 linhas, histórico de favoritos/assistidos, busca local e global, Cinema BR/Em breve/Cinema mundo, mudança de filtros, sombra/contraste do título no hero, botão de filtros inspirado na segunda imagem, aparência exata do exemplo 1.
- O código atual já foi aceito no CI e deve ser base de verificação; não afirmar resultado visual sem print/teste do APK em dispositivo.

## 4. PRÓXIMAS AÇÕES EXECUTÁVEIS, EM UM LOTE

1. Abrir AGENTS.md, este handoff, CURRENT_HANDOFF, PROJECT_PROGRESS.json, QA ANYTV_STREAMING_SINGLE_CARD_3_COLUMNS_20261024.md e PR #3. Confirmar HEAD, último CI e hash dos fontes. Verificar separadamente branch OTA e release code54 para não confundir artefato unsigned com APK publicado.
2. Inspecionar a implementação AnyTvPosterCard e suas chamadas; revisar proporção 2:3, 3 colunas, espaçamento e comportamento em diversas larguras. Se houver bug comprovado, aplicar correção **cirúrgica** sem redesenho, mantendo os ícones originais, estado do usuário e o exemplo 1 como referência única.
3. Validar com QA real Android: página principal (3 por linha), gênero Ação, Cinema (Em breve, internacional e BR), Favoritos, Assistidos, Continuando (busca própria), Ver mais, providers, busca fullscreen e relacionados. Conferir scroll/toque, marcações check/coração, persistência ao fechar/reabrir, Notas/JustWatch/TMDB reais, ano, textos não cortados.
4. Fazer teste de regressão de conectividade/IME, atalhos reais e companion TV conforme D01–D12/U01–U05; confirmar app/certificado TV efetivamente instalado e abertura real de título. Device-proof de CI não substitui isso.
5. Se aprovada nova atualização, pedir/usar autorização explícita para versionar acima de 54 (ex.: **2.9.26/code55**, se for a próxima release autorizada); usar APENAS workflow oficial com signatário V2, upgrade compatível, OTA v2, readback HTTPS byte a byte, checksum e preservação do legado. Nunca instalar unsigned sobre aplicativo assinado e não contornar gates.
6. Atualizar QA/handoff/progresso e realizar readback depois de quaisquer commits; gates físicos só passam com evidência física. Fazer progresso substancial em uma rodada, evitando dezenas de mensagens “continue”.

## 5. CANÔNICO, PROVAS E HISTÓRICO ARQUIVADO

- GitHub PR #3: https://github.com/brenoff2/tcl-apk-ci-builder/pull/3
- Release assinado 2.9.25/code54: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40106161667
- CI DEV posterior, todas as páginas padronizadas: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40114130735
- Prova isolada: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40114275545
- QA mais recente: docs/qa/ANYTV_STREAMING_SINGLE_CARD_3_COLUMNS_20261024.md
- QA release 2.9.25: docs/qa/ANYTV_2925_CODE54_SIGNED_OTA_RELEASE_20261024.md
- QA da busca e acompanhamento: docs/qa/ANYTV_2925_WATCHING_SEARCH_OVERLAY_20261024.md
- QA do controle redesenhado: docs/qa/ANYTV_2925_REMOTE_VISUAL_20261024.md
- QA Cinema, estreias e rockers: docs/qa/ANYTV_V2_2925_CINEMA_PREMIERE_REMOTE_20261024.md
- QA cartões e paginação 24: docs/qa/ANYTV_V2_2925_CARDS_NAVIGATION_PAGINATION24_20261024.md
- Documentos históricos, preservados: docs/handoffs/2026-10-23-anytv-full-transition.md; docs/handoffs/2026-10-23-ANYTV-V2-2922-TRANSICAO-ATUAL.md; docs/handoffs/2026-10-24-ANYTV-TRANSICAO-INTEGRAL-2924-2925.md. Seus estados de release antigos NÃO prevalecem sobre o run/code54 atual.

## 6. REGRAS INEGOCIÁVEIS

- O usuário disse **“não me mande mais imagem”**: nenhuma imagem gerada/mockup para pedidos de código; somente edições reais e QA.
- Manter fidelidade ao **primeiro exemplo aprovado da grade Ação e à página principal**, sem redesenho dos logos/chips de streamings, sem pôsteres cortados e com três por linha.
- Não apagar/recomeçar; não inventar dados, permissões, citações, disponibilidade, streaming futuro, percentuais de volume nem indicadores falsos.
- Não usar certificado novo, publicar OTA, alterar Worker/D1, migrar companion ou fazer merge sem escopo e autorização explícitos. Conservar dados locais, rollback e canais legados. Não vazar segredos.
- Distinguir com clareza **implementado / CI testado / assinado / OTA publicada / instalado no celular / homologado na TCL**. São estados distintos.
- Não pedir repetidamente “continue”. Entregar o maior avanço verificável por rodada, registrar no GitHub e comunicar commit, CI, release vigente, gates e bloqueios.
- Se a evidência GitHub divergir de texto histórico, a leitura LIVE do GitHub prevalece; preservar histórico, corrigir a síntese atual.
