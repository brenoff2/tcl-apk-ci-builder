# PRIMEIRA MENSAGEM DO NOVO CHAT — RETOMADA INTEGRAL DO PROJETO ANYTV

**CONTINUE O PROJETO ANYTV EXATAMENTE DO CHECKPOINT DE 24/10/2026. NÃO RECOMECE O PROJETO NEM REFAÇA ALTERAÇÕES QUE JÁ PASSARAM NO CI.** Leia primeiro o documento canônico completo docs/handoffs/2026-10-24-ANYTV-TRANSICAO-ATUAL-POS-2925.md, depois AGENTS.md, docs/handoffs/CURRENT_HANDOFF.md, docs/roadmap/PROJECT_PROGRESS.json e os relatórios de QA citados. Confirme o estado LIVE do GitHub antes de modificar qualquer arquivo. Esta mensagem é minha ordem de continuidade e traz o resumo do que foi decidido neste chat. O histórico anterior integral foi preservado no GitHub, incluindo o arquivo NEXT_CHAT_FIRST_MESSAGE_HISTORICO_PRE_20261024.md.

## OBJETIVO DO PRODUTO E DO MEU PEDIDO

O AnyTV é o aplicativo Android que estou desenvolvendo para controlar minha TCL/Android TV com rede local, TLS/pareamento, D-pad, volume/canal, teclado/IME, Bluetooth, discovery de TV e abertura de apps e títulos reais. Existe o AnyTV Connect como companion separado na TV, e backend Cloudflare Worker + D1 para catálogo, links e autorização de dispositivos. A área de Streaming inclui Discovery Home V3, catálogo global de filmes/séries TMDB, provedores e ofertas do Brasil, Cinema, filtros por gênero/plataforma, favoritos, assistidos, séries acompanhadas, busca, trailer sob ação explícita e atualização OTA.

Trabalhe **no código real**, sem me mandar mais imagens, mockups ou propostas visuais geradas. Eu já escolhi a aparência-base. Faça lotes significativos de trabalho, sem pedir uma sequência interminável de “continue”. Preserve arquitetura, tudo que funciona, dados locais, identidade de assinatura, links reais, rodapé/controle e recursos da TV.

## ESTADO REAL COMPROVADO — NÃO CONFUNDIR DEV COM OTA

- Repositório canônico: https://github.com/brenoff2/tcl-apk-ci-builder
- PR #3: https://github.com/brenoff2/tcl-apk-ci-builder/pull/3 — branch DEV feat/anytv-p0-device-proof-verifier-20261020, **DRAFT, sem merge**. O HEAD observado antes dos commits documentais desta transição era a289fd759475971705509e84c2d336be4611c2a6; releia o HEAD em tempo real.
- A **última versão publicada e ASSINADA** no telefone é **AnyTV V2 2.9.25-AnyTV/code54**, pacote com.tclremote.livre.v2. Release workflow #40106161667 SUCCESS com signatário V2 original, IME, catálogo live, APK signed e OTA byte-readback. Certificado SHA-256 fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9; SHA-256 APK c53596d8f8210bf0a12769481796481a851d00da4c52f544120b85367dd17fe0; artefato assinado #12564414706. OTA na branch feat/tcl-v22-updates-back-bluetooth, caminho updates/android/v2/current.apk, commit dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112. Canal antigo updates/android/cert2026/current.apk permanece inalterado.
- **ATENÇÃO: o trabalho visual MAIS RECENTE é POSTERIOR à release 2.9.25/code54.** Está implementado e aprovado em CI DEV, mas não foi assinado nem publicado em uma versão nova. O build unsigned DEV #40114130735 terminou **SUCCESS** para fonte/testes b4fecc63f744d66b8240d091db0a427e338e67ad: 51 testes Streaming, 2 contratos sem credenciais manuais, 11 contratos visuais, Gradle assembleRelease e identificação de APK; artefato de auditoria #12567737040. Prova isolada #40114275545 SUCCESS. Foi verificada igualdade dos SHA GitHub de todos os principais arquivos de código/testes atuais com os compilados. **NÃO INSTALAR o unsigned sobre o 2.9.25 assinado.** Para publicar novas mudanças será preciso próximo versionCode acima de 54 e autorização.
- Companion da TCL: com.tclremote.tvcompanion, referência técnica 2.5/code16, build #40060021841 SUCCESS e fontes não alteradas nesta rodada. NÃO afirmar que está instalado ou homologado na minha TV; verificar no aparelho. Não alterar companion somente por visual do celular.
- Backend: Cloudflare Worker anytv-link-resolver-pilot + D1, com prova/autorização governada de dispositivo. O último readback histórico mencionava versão Worker 58fe4b33 e deployment 25de8328; confirmar estado vivo antes de qualquer decisão de backend. Não liberar acesso, mexer em segredos ou reintroduzir quotas artificiais internas.
- Progresso formal permanece **7/11 marcos = 63%**. M08 ativação física e M09 abertura real na TV são IN_QA; M10 homologação/rollback e M11 operação/aceite estão em BACKLOG. Gates F0–F8 todos NAO_HOMOLOGADO. **CI, assinatura e OTA não são prova de teste físico.**

## DECISÃO VISUAL FINAL E PONTO EXATO EM QUE PARAMOS

Após várias imagens e ajustes, eu mandei screenshots da página principal, Cinema, gênero Ação, Favoritos, Assistidos e Em andamento. Reclamei de:
- capas cortadas verticalmente e com tamanhos diferentes;
- algumas páginas com duas colunas, outras com três, e espaçamentos/dados/nomes desalinhados;
- selos de plataforma alterados nas propostas de imagem, embora já estivessem corretos;
- slides Em breve · Cinema / Cinema pelo mundo fora do padrão;
- Favoritos, Assistidos e Em andamento fora do padrão da página principal.

Eu determinei: **“Use o exemplo 1 e pode fazer. Corrija todas as páginas. E nao me mande mais imagem. Corrija.”** A PRIMEIRA referência aprovada de grade Ação/página principal é o padrão obrigatório em todas as telas. Não invente uma nova identidade visual.

Esse trabalho **JÁ FOI IMPLEMENTADO NA BRANCH DEV E COMPILADO COM SUCESSO**:
1. Componente único **AnyTvPosterCard**, pôster verdadeiro 2:3, ContentScale.Fit, SEM recorte vertical; mesmo tamanho e espaçamento dos cartões da página principal.
2. **Três filmes/séries por linha** em TODAS as grades: gênero, Cinema brasileiro, Ver mais, Favoritos, Assistidos, **Continuando** (novo nome de Em andamento), busca global e relacionados.
3. Trilhos horizontais Em breve · Cinema, Cinema pelo mundo, streaming e home com **três cartões completos visíveis**, mantendo swipe e rolagem. Banner hero cinematográfico continua panorâmico, não confundir com capa vertical de filme.
4. Metadados sempre consistentes: ano/nota verdadeira, check assistido, coração favorito, título e **os mesmos badges F/S e ícones de plataformas que já existiam**, sem redesenhar nem trocar fornecedores. Persistência de notas/votos TMDB restaurada para favoritos e acompanhados.
5. Removida a alternância de gênero para modo lista vertical que cortava as imagens. **24 títulos por lote** com Mostrar mais 24; fonte TMDB em páginas de 20 consolidada/deduplicada.
6. O botão **Ver mais** abre a página própria do gênero/seção, não expande prateleira para baixo; corrige scroll ao entrar e sair.
7. Em andamento foi renomeado **Continuando**, com busca LOCAL no topo (“Buscar nas minhas séries”), três colunas, ordenação estável, cartões persistidos fora do cache e recuperação de séries antigas por ID.
8. Busca GLOBAL não empurra a capa: abre painel PRETO de tela inteira, com **Fechar** separado de **Limpar texto**, histórico local de até 10 pesquisas, rolagem vertical dos resultados em três colunas.
9. Cinema está separado dos streamings: Cinema BR vem de now_playing region=BR; Cinema pelo mundo é editorial internacional; Em breve Cinema e Em breve Streaming têm trilhos separados. Só filmes futuros em Em breve; não misturar lançamentos futuros nas listas comuns.
10. Nenhuma logo AnyTV indevida no topo da tela Streaming; preservar a marca nos locais aprovados.

O código dessa revisão está em **overrides/TclStreamingScreens.kt**, testes **scripts/tests/test_streaming_screens_contract.py**, QA **docs/qa/ANYTV_STREAMING_SINGLE_CARD_3_COLUMNS_20261024.md**, commit de código/testes b4fecc63 e CI #40114130735 SUCCESS. Não voltar para o estado anterior de duas colunas nem afirmar que a revisão ainda está incompleta só porque a conversa acabou durante o desenvolvimento. O CI concluiu depois.

## OUTRAS DECISÕES IMPORTANTES QUE JÁ FORAM FEITAS

- Controle remoto: fundo grafite/preto, detalhes vermelhos, header com TV em duas linhas, seis apps realmente encontrados na TV, banner dinâmico TMDB, botões media em duas linhas, D-pad grande até 304dp com aro vermelho no OK, controles laterais minimalistas e cinco atalhos inferiores. Sem preenchimento de volume/canal fingindo percentual. Fontes: TclApprovedHomeScreen.kt, TclResponsivePad.kt, TclInstalledApps.kt, PlaybackTransportBar.kt; QA ANYTV_2925_REMOTE_VISUAL_20261024.md.
- Página principal de filmes: manter o hero/banners com nome e dados POR CIMA da imagem, fade/contraste legível; conferir no aparelho o pedido de **shadow/blur no texto diante de imagem clara** e o botão de filtros mais organizado. Não presumir que detalhes não comprovados estejam pixel-perfeitos.
- Discovery Home V3 é editorial/global independente da instalação de um serviço. Apps instalados são reais, ofertas BR e tipo de disponibilidade são comprovados pela fonte; Movie of the Night, TMDB e JustWatch têm funções distintas. Não fabricar ranking, links, assinatura, cinema, data ou acesso.
- Trailers não devem iniciar sozinhos. Abrir app/buscar título/abrir ficha/reproduzir conteúdo são coisas diferentes; respeitar rotas reais e DRM.
- Identidade V2 foi escolhida para preservar o legado. **Nunca trocar certificado, applicationId ou sobrescrever OTA cert2026**. A versão 2.9.8 foi relatada funcionando na TCL; 2.9.10–2.9.12 tiveram histórico de TLS/SSL e perda de pareamento HOST/PIN, documentado em handoffs de 23/10. Não refazer alterações já estabilizadas só porque há um bug visual.
- Não expor códigos pessoais de ativação, PIN, segredos do Worker ou chave de assinatura. Não contornar workflows de segurança e nunca distribuir unsigned como update.

## O QUE VOCÊ, PRÓXIMO CHAT, DEVE EXECUTAR

1. Ler os arquivos do cânone e fazer readback LIVE do PR #3 e da OTA, confirmando que o fonte HEAD continua igual ao build #40114130735. **O HEAD com commits de documentação será novo; não confundir com mudança funcional.**
2. Verificar os componentes reais da UI (AnyTvPosterCard e chamadores) e promover um QA visual/funcional consistente: home, Ação/gêneros, Favoritos, Assistidos, Continuando, Cinema BR, Em breve Cinema, cinema internacional, páginas Ver mais, busca geral, ofertas e relacionados. Três pôsteres inteiros por linha, proporção 2:3, os ícones ORIGINAIS, ano/nota/botões/título na mesma posição. Não reescrever se já está certo; corrigir cirurgicamente problemas comprovados.
3. Conferir o uso físico no celular: rolagem, todos os botões, altura da capa, responsividade e título/hero com sombra/contraste. Validar manutenção de favoritos, assistidos, séries em andamento e histórico ao reiniciar. Se não houver acesso a um aparelho real, **marcar apenas como pendente**, sem fingir homologação.
4. Verificar a TCL e o companion instalado, ativação governada do dispositivo, TLS/IME, /apps, /launch, /title-search, /title-open, e abertura real de ficha. Não confundir logs de CI com sucesso na TV.
5. Se precisar lançar correções posteriores, **somente mediante minha autorização**, usar versão/code superior a 2.9.25/code54 (por exemplo 2.9.26/code55, a confirmar), fluxo oficial GitHub, signatário V2 original, QA e readback OTA. Sem merge automático, sem deploy do Worker/companion por efeito colateral.
6. Atualizar AGENTS.md, CURRENT_HANDOFF.md, NEXT_CHAT_FIRST_MESSAGE.md, PROJECT_PROGRESS.json, STATUS.md e QA com o que de fato aconteceu. Fazer o máximo de avanço em uma rodada, sem me pedir dezenas de “continue”.

**Referência central atualizada:** docs/handoffs/2026-10-24-ANYTV-TRANSICAO-ATUAL-POS-2925.md. **Histórico mais antigo** preservado em docs/handoffs/2026-10-23-anytv-full-transition.md, docs/handoffs/2026-10-23-ANYTV-V2-2922-TRANSICAO-ATUAL.md, docs/handoffs/2026-10-24-ANYTV-TRANSICAO-INTEGRAL-2924-2925.md e docs/handoffs/NEXT_CHAT_FIRST_MESSAGE_HISTORICO_PRE_20261024.md. Se uma dessas fontes antigas falar que 2.9.24 é a versão atual ou que 2.9.25 ainda não foi publicada, é um trecho HISTÓRICO: prevalecem a release oficial #40106161667 e a branch OTA. **Não recomeçar. Não gerar imagens. Não publicar sem autorização.**
