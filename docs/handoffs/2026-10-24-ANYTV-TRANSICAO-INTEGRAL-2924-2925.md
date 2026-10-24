# ANYTV — TRANSIÇÃO INTEGRAL DO CHAT (24/10/2026)

> Este documento prevalece sobre handoffs antigos para estado atual. Histórico anterior preservado por Git. Este texto contém decisões, evidências e pendências, não credenciais. É a base para a próxima mensagem do ChatGPT. **Nunca registrar o código de ativação fornecido na conversa nem reproduzi-lo.**

## 1. Objetivo e regra fundamental

Continuar o PROJETO ANYTV no **código Android/TV e backend reais**, não gerar mais protótipos, mockups ou imagens como resposta a pedidos de implementação. Usuário aprovou a identidade minimalista AnyTV em vermelho/preto, deseja controle remoto TCL/Android TV LAN/TLS/pareamento/D-pad/volume/IME/Bluetooth, lançador de aplicativos, catálogo TMDB e plataformas BR, filmes/séries, filtros, favoritos/assistidos/acompanhados, trailers e atualização OTA. Prefere ação completa com poucos pedidos de "continue", atualização objetiva de feitos/pendências e prova real. Não apagar trabalho, recomeçar projeto, mudar nomes de pacote/certificados ou alegar que CI comprova uso na TCL.

## 2. Canônico técnico e autoridade

Repositório privado: https://github.com/brenoff2/tcl-apk-ci-builder
DEV: feat/anytv-p0-device-proof-verifier-20261020 — PR #3 DRAFT, ainda sem merge (HEAD de readback em 24/10: 91a2b84a0f80f7ec3e643e97cf9c290e9d0be4ff).
OTA: feat/tcl-v22-updates-back-bluetooth. Canal V2: updates/android/v2/current.apk. Canal legado: updates/android/cert2026/current.apk (NÃO alterar).
Arquivos-gate: AGENTS.md; docs/roadmap/PROJECT_PROGRESS.json; docs/handoffs/CURRENT_HANDOFF.md; docs/handoffs/NEXT_CHAT_FIRST_MESSAGE.md; docs/STATUS.md; docs/qa/ANYTV_V2_2925_LINKS_CINEMA_20261024.md; este handoff; .github/workflows/release-anytv-phone-v2-new-package.yml; .github/anytv-phone-v2-build-request.json.
Código: overrides/TclStreamingScreens.kt, overrides/TclInstalledApps.kt, overrides/PlaybackTransportBar.kt, overrides/AnyTvSplashScreen.kt, scripts/apply_anytv_brand.py, scripts/apply_anytv_phone_v2_identity.py, anytv-resolver/src/worker.mjs, anytv-resolver/src/device-request.mjs, anytv-resolver/src/device-proof.mjs; companion TV em overrides/tv_safe e scripts/prepare_companion_safe.py.

## 3. Estado operacional COMPROVADO ao encerrar este chat

**ÚLTIMO APK V2 ASSINADO PUBLICADO: AnyTV 2.9.24, versionCode 53**, pacote com.tclremote.livre.v2, GitHub Actions #40095671540 SUCCESS com assinatura V2 e readback OTA. SHA-256 APK conforme log de assinatura #40095671540: f5827bc0d3e4da5fbf90f9fe772e2aba2890953c1f295db73613c6f638be49ae. Readback GitHub da OTA: blob 7a1bbdea9e9628ce15d3c1e9d2debef3c98824eb, 25.996.188 bytes. Certificado V2 SHA256 fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9; manter em qualquer release. Canal legado segue blob 44c766b433ddb370f4589b87843f1edfd9cd7b7a. URL de instalação: https://raw.githubusercontent.com/brenoff2/tcl-apk-ci-builder/feat/tcl-v22-updates-back-bluetooth/updates/android/v2/current.apk . Não desinstalar V2 para atualizar.

**PRÓXIMA VERSÃO 2.9.25 / code54 NÃO ASSINADA E NÃO PUBLICADA.** Implementação de Cinema, logos BR e novo pedido de acesso em DEV. Workflow protegido de assinatura .github/workflows/release-anytv-phone-v2-new-package.yml e pedido .github/anytv-phone-v2-build-request.json permanecem code53. Tentativa anterior de alterá-los para code54 bloqueada pela política do conector: **NÃO BURLAR A RESTRIÇÃO**, não assinar com outro certificado, não lançar APK unsigned. Exige autorização legítima do ambiente e release oficial posterior.

**Worker Cloudflare ATUAL COMPROVADO via leitura direta em 24/10:** anytv-link-resolver-pilot, versão 58fe4b33-6ee2-4841-8587-c28acaaad435, deployment 25de8328-10ab-471a-8e94-fb4cdbce13d9, **100% de tráfego**. Rollback anterior 58a1c0c3-eafc-406a-9961-9656a01e14ec. Cinco bindings preservados: DB D1 canônico, ANYTV_RESOLVER_AUTH_MODE=device, ANYTV_DEVICE_ACTIVATION_ENABLED=true, segredos ANYTV_MOVIE_API_KEY e TMDB_READ_TOKEN (valores NÃO registrar). Smoke anterior: health/catalog HTTP200; status sem prova HTTP401; request inválido HTTP400. Removidos bloqueios artificiais próprios de 200 chamadas/mês, 20/min por IP e ativação; contratos externos do fornecedor permanecem. Movie of the Night é a API comercial de links, não confundir com JustWatch/TMDB.

**CI MAIS RECENTE DO HEAD 91a2b84:** GitHub Actions #40097871008 (prova de dispositivo) SUCCESS; #40097870978 (Android candidate unsigned) estava IN_PROGRESS ao readback 24/10 13:24Z, na etapa Gradle. Conferir resultado antes de afirmar PASS para o último commit. CI anterior Android #40097330186 SUCCESS; backend #40097287821 SUCCESS; device proof #40097330068 SUCCESS.

**Status oficial P0:** 7 de 11 marcos técnicos = 63%, todos os gates F0–F8 NAO_HOMOLOGADO, M08 ativação real M09 abertura real do título TV M10 integrado/rollback M11 operação estável ainda sem aceite global. Não promover gates por lançamento/CI.

## 4. Histórico das decisões e implementações importantes

- Linha legado / V2: V2 usa com.tclremote.livre.v2 e signer exclusivo; nunca substituir app legacy ou exigir reinstalação. Compilação, assinatura e OTA pelo workflow oficial GitHub com validação de APK, cert e bytes remotos.
- Evolução 2.9.18→2.9.22 (codes47–51): restauração do controle, TMDB, buscas, canais Brasil, gênero, lista/grade, ordenação e paginação 20, carrosséis e melhorias IME. 2.9.22 code51 Actions #40045953034; correção de quotas internas no Worker sem novo APK.
- 2.9.23/code52 publicada #40086101046: reorganização dos cartões — ano/nota à esquerda e assistido/coração à direita na linha 1; F/S + serviços BR comprovados na linha 2; título com 3 linhas reservadas na linha 3; trailer retirado do cartão e mantido na ficha; gêneros com ícones lista/grade. Resolver status assinado, validação de URL Netflix/Prime, tratamento de respostas 401/404/409/5xx e fallback TMDB/Wikidata seguro.
- Identidade visual aprovada: AnyTV, cores vermelho+preto/dark, minimalista; referência visual de logo com tela/play vermelha e wordmark AnyTV. Usuário rejeitou repetidos protótipos, quer app efetivamente alterado. 2.9.24/code53 assinada #40095671540 incorporou novo ícone launcher, logo, splash e tema, "Ver mais" como ação real de expansão em grade, remoção do "Lembrar" que não funcionava, espaçamento 6dp entre F/S+streaming e título, provedores menores, e retirada dos controles sem função "Do início", "Episódios", "Fones" da barra. Cache dos aplicativos instalados na TCL para ícones visíveis na reconexão e retry após falha inicial. Não confundir build com QA física.
- O usuário enviou screenshot comprovando que os itens não autorizados mostravam formulário de código. Código único compartilhado por ele na conversa **não deve ser copiado, reapresentado, persistido ou usado de novo**. O handoff posterior no GitHub registra liberação real da instalação V2 (sufixo de identidade mascarado 3bede1c2) por operador com código de uso único e readback P-256/nonce; registro anterior preservado, 2 dispositivos ativos e 2 códigos consumidos na ocasião. Confirmar estado atual por readback se necessário, não presumir que códigos antigos servem.
- Novo fluxo DEV code54: remover formulário de código do app; exibir "Verificar acesso aos links", e quando necessário "Solicitar liberação". POST /v1/device/request assinado com chave P-256 do Keystore, prova ANYTV-REQUEST1, nonce, registro D1 inicialmente enabled=0 e aprovação manual de operador para essa mesma chave; não concede autoacesso e não usa token fixo. Worker request ativo em produção; fluxo UI novo ainda sem APK code54 publicado.
- Cinema: usuário percebeu que vários títulos não têm streaming porque estão em cartaz. Determinou categoria "Cinema" e selo azul "Cinema" junto ao F para títulos comprovadamente em cartaz no BR. Código DEV implementou TMDB movie/now_playing?region=BR e selo, sem inventar streaming em títulos de cinema. Ficha traz CTA "Ver cinemas e sessões · Ingresso.com". Ingresso.com exige integração/contrato de parceiro para listagem interna confiável de sessões por filme/cidade; CTA oficial não comprova sala/horário específico. Não confundir upcoming, em cartaz e disponível em streaming; considerar data/região/confiabilidade.
- Último avanço DEV code54: ícones de TODOS os provedores disponíveis no BR, instalados ou não, linha horizontal rolável em StreamingOfferIcons, deduplicação provider_id e logos via TMDB logo_path quando não há app instalado. Testes contratos e Worker passados conforme docs/qa/ANYTV_V2_2925_LINKS_CINEMA_20261024.md. É código candidato até publicação.
- Integrações de links: Globoplay/Plex não constavam da cobertura da Movie of the Night BR naquele levantamento. Pesquisar APIs oficiais/legais ou intents documentados; jamais declarar link específico ou reprodução real sem teste na TV; diferenciação "abrir app", "busca", "ficha" e "reproduziu". Não colocar segredos no APK.
- Companion AnyTV Connect/TCL: M03 TV 2.5/code16 tinha build/OTA anterior. Usuário pediu atualização da TV após mudanças; **NÃO houve novo APK companion nessa rodada code53/54** e versão/assinatura da instalação real TCL não foram verificadas. Priorizar comparação do companion instalado e do código com mudanças necessárias, teste de conexão, sem trocar certificado ou forçar instalação.

## 5. Pendências prioritárias NO EXATO PONTO DE PARADA

1. **Fechar CI do último commit DEV**: readback de #40097870978 para head 91a2b84; verificar se a remoção do método Android legado compila com testes; resolver erros cirurgicamente, sem criar múltiplas releases não necessárias.
2. **Release 2.9.25/code54 ainda NÃO publicada**: revisão do workflow protegido conforme autorização legítima da política, preservar .github/workflows/release-anytv-phone-v2-new-package.yml, request code54, signer V2 original, OTA legacy intacta, SHA256 signed, cert, git blob, readback HTTPS e backup/rollback; não contornar bloqueio nem publicar APK unsigned.
3. **Instalar/testar no Android real**: nova identidade visual, splash e launcher, barra de aplicativos da TV sem tocar duas vezes, separação visual F/S-Cinema-provedores/título, "Ver mais" funcionando, sem "Lembrar" e botões sem função, favoritos/assistidos persistentes, pesquisa, lista/grade/paginação/trailer. Tela exata e evidência D01–D12/U01–U05.
4. **Cinema e provedores**: validar lista BR now_playing, selo "Cinema" azul ao lado do F apenas com sinal fundamentado, disponibilidade streaming separada, CTA Ingresso.com honesto, lista completa de logos BR sem duplicidades.
5. **Links e autorização**: validar P-256 do dispositivo já autorizado; para novo pedido de liberação, status 401 → request assinado → D1 pending → aprovação EXPLÍCITA operador da mesma chave → status 200; nada de reaproveitar código antigo. Testar Netflix, Prime, Disney+, Max, Apple TV e demais serviços com TV real distinguindo busca/ficha/playback.
6. **AnyTV Connect / TCL**: identificar pacote, versão, signer e rota instalada; decidir se companion necessita build/OTA; não publicar sem identidade/assinatura, QA e rollback.
7. **Atualizar canônico depois de cada evidência** e manter PR #3 draft, 7/11=63% e gates pendentes até QA físico.

## 6. Regras de execução

Somente ChatGPT Web + GitHub conector / ferramentas conectadas, sem exigir Codex. GitHub-first; implementação mínima e cirúrgica, leitura atual de upstream/contratos. Preferir concluir um lote substancial de trabalho, sem solicitar inúmeros "continue". Não começar por novo mockup ou design conceitual; usuário pede APK e comportamento reais. Evitar API paga quando alternativas apropriadas já existem, mas preservar integração licenciada. Não fazer scraping logado ou coletar informação privada. Evitar custos/limites artificiais de uso. Responder sempre com estado de BRANCH, COMMIT, CI, OTA/publicado, Worker, TV, feitos, faltantes, riscos e gates; distinguir implementado, compilado, assinado, publicado, instalado e homologado.

**Se este handoff divergir de readback posterior de GitHub/Cloudflare, prevalece a evidência live mais recente.** Não usar estados históricos de 2.9.22/2.9.23 como release vigente.

## 7. Fontes principais

- https://github.com/brenoff2/tcl-apk-ci-builder/pull/3
- https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40095671540
- https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40097330186
- https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40097870978
- https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40097287821
- docs/qa/ANYTV_V2_2925_LINKS_CINEMA_20261024.md
- docs/roadmap/PROJECT_PROGRESS.json
- docs/handoffs/CURRENT_HANDOFF.md
- docs/handoffs/NEXT_CHAT_FIRST_MESSAGE.md
