> **OTA V2 ATUAL: AnyTV 2.9.19/code48 em 23/10/2026** — run [40033251472](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40033251472) SUCCESS; APK SHA256 `6bf203ce33b88d51b0645bbaba063651d66d1bf5cc4c2f5520c8295fe4a3ef1d`, OTA original commit `99d0b19f7b5e7a3922c45a34ff942ac0cb070149`, blob `ccc2d504f5a93050cdc4b88b7603dd18b00afb3c` (28.396.388 bytes); bytes remotos iguais ao assinado. Certificado V2 idêntico e pacote `com.tclremote.livre.v2`. O APK da linha cert2026 permaneceu inalterado; instalação e controle da TCL aguardam teste físico. Versões 2.9.17 e 2.9.18 nos parágrafos abaixo são histórico.

> **ATUALIZAÇÃO CANÔNICA V2 — 23/10/2026:** AnyTV V2 **2.9.18/code47** já publicada neste canal, GitHub Actions [40029572648](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40029572648) **SUCCESS**, SHA256 APK `0cf42a76e9380efaac7b3b7fc55224451a492dbfeb264e85bfaeb9a936ed7737`, commit OTA `419d0d4814953b3ba71ae08a7d85c8ee7103d72f`, blob `b1bcc0ce0c3fa16f9853c78e3dd5ed68c80dc31f`, 28.396.388 bytes, certificado V2 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. Readback GitHub HTTPS byte a byte PASS. O arquivo `updates/android/cert2026/current.apk` permaneceu inalterado (blob `44c766b433ddb370f4589b87843f1edfd9cd7b7a`). **Instalação no telefone e homologação na TCL ainda pendentes.** Texto histórico chamando 2.9.17 de atual está supersedido. 

# PROJETO ANYTV — TRANSIÇÃO CANÔNICA COMPLETA (23/10/2026)

> **DOCUMENTO PRIORITÁRIO PARA O PRÓXIMO CHAT.** É uma fotografia auditada do GitHub em 23/10/2026; fatos históricos em outros handoffs que dizem que a OTA atual ainda é a 2.9.12, que é proibido criar uma nova identidade ou que o único publicador atual é o cert2026 **não descrevem mais a linha V2**. Preserve a história, mas utilize ESTE documento como ponto de retomada. Nunca confundir código gravado na branch DEV com APK homologado/publicado.

## 0. RESUMO EXECUTIVO — LEIA ANTES DE AGIR

O usuário está desenvolvendo o **AnyTV V2**, aplicativo Android que atua como controle remoto para TVs Android/TCL/Sony, lista aplicativos instalados na televisão, oferece descoberta e pesquisa de filmes/séries com TMDB/JustWatch Brasil, abre apps oficiais na TV, mantém cache local, favoritos, filmes vistos e séries acompanhadas. Ele já **instalou o AnyTV V2, conectou à TV e validou várias iterações visuais**, pedindo ajustes pontuais em vez de uma reformulação. Atualizações **DEVEM chegar pelo botão "Buscar atualizações" dentro do mesmo aplicativo**, sem reinstalação, sem nova assinatura ou pareamento, a não ser em caso de problema realmente comprovado.

**Última OTA V2 comprovadamente publicada**: **AnyTV V2 2.9.17 / versionCode 46**, package `com.tclremote.livre.v2`, fluxo GitHub Actions `40021931010` **SUCCESS**, APK SHA256 `0a80dd4cabf16c5c67a36520dd2e2dadda52b77c28e902c0d7c0beb95bdfc475`, 28.396.388 bytes. O botão interno usa `updates/android/v2/current.apk` da branch de distribuição. Assinatura V2 SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. O pipeline verificou versão, certificado e `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`. Código publicado e compilado NÃO equivale a teste de todas as telas no aparelho.

**Estado de desenvolvimento após a última solicitação do usuário:** branch DEV HEAD auditado `88475e381819124f46499aeec08eca13e661f357`; existem commits de correção visual e novo helper de navegação entre 21:16 e 21:19 UTC de 23/10/2026, **NÃO PUBLICADOS**: banner de 75dp com título à esquerda/imagem à direita, reposicionado abaixo dos apps de streaming; cabeçalho conectado sem seletor; helper iconográfico `AnyTvStreamNavBar` escrito, mas **NÃO integrado à tela**; os filtros em duas colunas, integração de menus e investigação dos links diretos permanecem incompletos. Os runs P0 isolados mais recentes (incluindo `40025974542`) foram SUCCESS, **mas não são CI de compilação release nem prova de OTA 2.9.18**. Nenhum `versionCode 47` foi preparado/publicado. **Ponto exato de retomada: integrar com segurança a navegação e os filtros pendentes, investigar as causas dos links e somente depois compilar/assinar/publicar 2.9.18 (code47) com a mesma identidade V2.**

## 1. Repositório, branches e fontes de verdade

- GitHub privado: https://github.com/brenoff2/tcl-apk-ci-builder
- DEV/código de trabalho: `feat/anytv-p0-device-proof-verifier-20261020` (HEAD auditado `88475e381819124f46499aeec08eca13e661f357` antes deste handoff).
- OTA/arquivos instaláveis: `feat/tcl-v22-updates-back-bluetooth`; HEAD anterior a este handoff `eb834954cd6b90e4b629689aad4a1c2d7849805b`.
- Documento prioritário: `docs/handovers/2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md` (copiado para AMBAS as branches).
- Bloqueios e contexto detalhado da 2.9.18: `docs/handoffs/2026-10-23-anytv-2918-ui-links-unfinished.md` na DEV.
- Operação corrente/correções passadas: `docs/handoffs/CURRENT_HANDOFF.md`, `README.md`, `docs/STATUS.md` (STATUS apenas DEV).
- História anterior da linha cert2026: `docs/handovers/2026-10-23-anytv-full-transition.md`, `docs/handovers/2026-10-23-anytv-transicao-integral.md`, `docs/handovers/2026-10-23-anytv-signing-ota-transition.md` — **ARQUIVOS HISTÓRICOS, não aplicar suas instruções antigas à V2**.
- Custódia da nova assinatura: `docs/decisions/ADR-0004-NEW-ANDROID-SIGNER-V2-GITHUB-CUSTODY.md`, `signing-recovery/v2/identity.json`, `signing-recovery/v2/phone-cert.pem`, `signing-recovery/v2/phone-signer.aesgcm` (na DEV).
- Workflow de geração de identidade (somente histórico; NÃO reexecutar): `.github/workflows/bootstrap-anytv-phone-v2-identity.yml` (DEV).
- **Workflow de atualização V2 REUTILIZÁVEL**: `.github/workflows/release-anytv-phone-v2-new-package.yml` na DEV; gatilho `.github/anytv-phone-v2-build-request.json`.
- Android/OTA V2: `updates/android/v2/current.apk` na branch OTA.
- Código de interface: `overrides/TclApprovedHomeScreen.kt` (cabeçalho/TV), `overrides/TclInstalledApps.kt` (apps da TV, API companion, banner), `overrides/TclStreamingScreens.kt` (banner, catálogo, seções, favoritos, episódios, menus, filtro).
- Atualizador e validação da identidade: `overrides/common/TclUpdateManager.java`, `scripts/apply_anytv_phone_v2_identity.py`.
- Reconnect involuntário: `scripts/apply_anytv_restore_connection.py`; RSA/TLS TV: `scripts/apply_anytv_tv_tls_alias_isolation.py`.
- Catálogo/links: `anytv-resolver/src/worker.mjs`, `overrides/AnyTvDeviceIdentity.kt`, `overrides/AnyTvPersonalLinks.kt`.
- Testes: `scripts/tests/test_streaming_screens_contract.py`, `scripts/tests/test_approved_visual_contract.py`, `scripts/tests/test_anytv_auto_tv_resume.py`, `scripts/tests/test_streaming_no_manual_credentials.py`.
- Fonte upstream Android fixada: `harimoradiya/androidtvremote` commit `16c68f0bb9d23f929ee8240f105fdb9000262d85`, Apache-2.0; workflow faz clone/patcheia e compila no GitHub.

## 2. HISTÓRICO COMPLETO DAS VERSÕES E DECISÕES

1. **2.9.8**: usuário tinha controle TCL funcional; baseline para reconexão. Assinatura antiga (cert2026), package original `com.tclremote.livre`.
2. **2.9.9**: Discovery V3; problemas de distribuição, tentativas de patch/reconstrução de APK/artefatos externos (rota abandonada).
3. **2.9.10, code39**: certificado original recuperado/usado localmente e APK assinado; apareceu falha SSL/reconexão da TCL. Não confundir a assinatura local com um Secret de GitHub Actions durável. CI `39774708705`, SHA APK `e21d63f516e7d6af300e10d117ef089ab08b13afbdad0814f25cdd478cdb6965`.
4. **2.9.11, code40**: tentativa de reparo SSL forçando novo PIN eliminou preferências de host/pareamento e agravou a conexão; revertida. Publicação antiga run `39797520084`.
5. **2.9.12, code41**: rollback para código anterior, mas preferências já removidas não retornaram; OTA antiga cert2026 continua nesse canal, sem afirmação de conexão física corrigida. Run `39882583323`.
6. **2.9.13, code42**: candidato RSA TLS para isolar chave RSA `androidtv-local` da EC P-256 do resolvedor, run `39891891049`; na linha cert2026 **não foi publicado**, e NÃO foi homologado fisicamente.
7. **Decisão EXPLÍCITA do usuário em 23/10/2026:** não tentar recuperar mais a antiga chave e **GERAR NOVA IDENTIDADE DE ASSINATURA NO GITHUB**, com recuperação durável. Nenhum Google Drive/arquivo antigo `ANYTV_RECOVERY_SECRET_2026_PRIVADO.txt` deve ser reutilizado. O pacote antigo não recebe atualização in-place com certificado arbitrariamente diferente. Criou-se package `com.tclremote.livre.v2`, instalável em paralelo; o usuário instalou, conectou à TV e aprovou sua continuidade.
8. **Criação da V2**: run `39998409528` SUCCESS, RSA-3072/PKCS12 cifrado, certificado público e backup AES-256-GCM no GitHub, secret de recuperação Actions e prova remota de restauração.
9. **2.9.13 V2/code42**: primeira instalação da nova identidade pelo canal `updates/android/v2/current.apk`, run `39998890413` SUCCESS. Usuário confirmou instalação e conexão com a TCL.
10. **2.9.14 V2/code43**: corrida de slides horizontal automática cada 3s sem botões volumosos, carrossel rolando com a página, detalhes com scroll, ofertas BR por título e pequenos logotipos; run `40003530305` SUCCESS; usuário aprovou melhora.
11. **2.9.15 V2/code44**: corrigir logos duplicados por canal/add-on, alinhar nota e logos, ajustar entrelinha, topo com gêneros e apps compactos, busca recolhida, gêneros personalizáveis; run `40008906467` SUCCESS. Feedback: ícones não deveriam ter sido ampliados ao lado do título, e sim no topo; entrelinha ficou espremida.
12. **2.9.16 V2/code45**: cache local, serviços assinados selecionáveis, lista de títulos, filmes assistidos e séries/episódios, deduplicação entre seções; run `40014131929` SUCCESS. Cloudflare Worker de temporadas não pôde ser alterado (bloqueio da ferramenta), por isso entrada manual de episódios, sem inventar nomes.
13. **2.9.17 V2/code46**: banner 75dp da home, cabeçalho e menus mais compactos, ícones TCL 56dp no topo do Streaming e 20dp ao lado da nota, ano/gêneros nas fichas, coração e check, vistos ocultados da descoberta e acessíveis em Assistidos, séries em acompanhamento, cache de fichas/capas permanentes no armazenamento local, correção pontual de reconnect após remoção involuntária via discovery. Run `40021931010` **SUCCESS**; OTA `12e8bfa0ec8a160d25ba744bad78be09fbcac80a`, APK sha `0a80dd4cabf16c5c67a36520dd2e2dadda52b77c28e902c0d7c0beb95bdfc475`, Git blob `b1a7fab2ffb1963669abbbe5e635770d8901022f`. Feedback real posterior: usuário gostou da evolução, mas identificou requisitos adicionais de posicionamento, seleção TV e navegação/links.
14. **Pós-2.9.17 — CÓDIGO NÃO LANÇADO da futura 2.9.18:** commits `2f4d1b2`, `c25505a`, `070be65`, `3648613`, `06b0203`, `59e63a2`, `88475e3`. Ajuste banner, sua posição, cabeçalho conectado e helper de ícones. O helper não foi integrado, filtro de duas colunas ainda não implementado e backend/link não resolvido. Runs `40025614423`, `40025640150`, `40025668406`, `40025720366`, `40025846923`, `40025902531`, `40025974542` são verificações P0 isoladas SUCCESS, **não APK 2.9.18**. Nenhum pedido de build code47 lançado.

### Decisões de UX já fechadas

- Evoluir com ajustes localizados, NÃO refazer a interface aprovada.
- Em tela vertical, economizar altura; banner 75dp perfeito; menus de navegação em **uma única linha de ícones**, gêneros em faixa horizontal separada; sem gigantes botões Anterior/Próximo.
- Slide troca a cada 3s e aceita swipe. Qualquer toque no banner abre Streaming; não precisa botão "Veja mais" grande.
- Título de duas linhas legível, espaço moderado após capa, nota e logos discretos; **ícones grandes são os do TOPO**, iguais à home, NÃO os badges pequenos dos cards.
- Data/ano + gêneros reais na ficha, sinopse rolável, plataforma por título BR; marca favoritos/assistidos sem depender de streaming instalado.
- Meus streamings é **seleção de assinaturas do usuário** e não dedução pelo simples fato de app estar na TCL.
- Favoritos, assistir depois, vistos, acompanhando série e episódios devem ser acessíveis e persistentes; filmes vistos deixam a descoberta normal, mas continuam no histórico.
- Reutilizar descrição e capas já mostradas no cache local sem prazo; listas editoriais e ofertas BR têm TTL (ofertas/ranqueamentos mudam). Cache não inclui valores de segredo.
- Usuário REJEITOU funcionalidades "humor do dia", filtro de duração e botão aleatório "Surpreenda-me". Não ressuscitar.
- O usuário não quer inserir manualmente chaves secretas de APIs no APK; não embutir segredo estático no app nem confundir autorização individual do device com API.

## 3. ASSINATURA V2 — GUIA DE CONTINUIDADE OBRIGATÓRIO

**A assinatura V2 JÁ FOI CRIADA, RESTAURADA NO CI E USADA EM 6 RELEASES. NÃO GERAR NOVA, NÃO VOLTAR À ANTIGA.**

### 3.1 Duas identidades diferentes (INCOMPATÍVEIS)

| Fator | LEGADO cert2026 | NOVO V2 OFICIAL |
| --- | --- | --- |
| Android applicationId | `com.tclremote.livre` | `com.tclremote.livre.v2` |
| SHA256 certificado | `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f` | `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9` |
| OTA e branch | `updates/android/cert2026/current.apk`, OTA branch | `updates/android/v2/current.apk`, OTA branch |
| Último código publicado verificado | 2.9.12 / code41 | **2.9.17 / code46** |
| Estado | Legado separado, preservar | **LINHA ATIVA**; atualizar in-place mantendo pacote/cert |

Uma nova assinatura não pode atualizar por cima do pacote antigo; **o usuário já fez a instalação paralela V2**. A partir daqui, atualizar continuamente o próprio V2 com **mesmo pacote e mesmo certificado**. A TV companion tem assinatura/canal próprios: não tocar para resolver assinatura do telefone.

### 3.2 Onde estão os materiais V2 (VERIFICADO no GitHub DEV)

- Arquivo cifrado `signing-recovery/v2/phone-signer.aesgcm` (**3525 bytes**, git blob `3552f69853ed2f626644114c07ed6a0ec822ce38`): contém o PKCS12 de assinatura criptografado; sem o segredo de recuperação não basta.
- Certificado PÚBLICO `signing-recovery/v2/phone-cert.pem` (1484 bytes).
- Manifesto `signing-recovery/v2/identity.json`: certificado SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`; `ciphertext_sha256=be7ac4686ca6dd6817ba4a662095f71313470663c802f0b0ba22cd6a023b9f5b`; algoritmo RSA-3072.
- **Segredo OPERACIONAL:** `ANYTV_SIGNER_V2_RECOVERY_HEX`, GitHub Actions **Repository Secret** (256 bits / hex de 64 caracteres). NÃO exibir, copiar ou armazenar seu valor em arquivo, log, chat ou argumento. Secrets são write-only na UI, mas disponíveis aos jobs aprovados. O último run de produção prova que o segredo foi acessível no Actions.
- Criptografia envelope: bytes `ANYTVV2G1` + nonce de 12 bytes + AES-256-GCM ciphertext/tag; senha do PKCS12 derivada por `base64url(HMAC-SHA256(bytes.fromhex(secret), "AnyTV-Phone-V2-PKCS12-Pass-v1"))`. **O procedimento exato já está no workflow; reutilizá-lo, não reinventar.**
- Criado run `39998409528` **SUCCESS**: geração dentro do runner, assinatura/verificação de desafio, commit de SOMENTE cifrado e certificado público, leitura de volta do Git remoto, decriptação e certificado verificados (`ANYTV_V2_SIGNER_GITHUB_ENCRYPTED_READBACK_AND_RECOVERY_PASS`).
- Usado com sucesso no run `40021931010`: log `ANYTV_V2_ENCRYPTED_BACKUP_RECOVERED_WITH_GITHUB_ACTIONS_SECRET`, `ANYTV_V2_SIGNER_VALIDATED_0a80dd4...`, `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`.
- **NÃO REEXECUTAR** `.github/workflows/bootstrap-anytv-phone-v2-identity.yml` nem alterar `signing-recovery/v2/*` ou o GitHub secret. O bootstrap foi desenhado para falhar se V2 já existir; qualquer rotação requer decisão EXPLÍCITA do usuário e plano de migração Android.
- **Backup secundário independente ainda NÃO comprovado:** foi tentado um secret de escrow adicional, mas `signing-recovery/v2/phone-signer-escrow.aesgcm` **NÃO EXISTE** nas branches auditadas. Não confundir o secret secundário com uma segunda cópia funcional do signer. Registro de contingência separado ainda pendente; não é bloqueio para assinaturas normais enquanto o Secret primário existir.
- Arquivo antigo de Drive `ANYTV_RECOVERY_SECRET_2026_PRIVADO.txt` foi **rejeitado pelo usuário** como chave da linha antiga, e não pertence à identidade V2. Não requisitar nem reutilizar. Backup antigo `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin` é LEGADO; não usar para V2.
- Arquivos de assinatura V2 estão na DEV; **não é obrigatório nem desejável copiar o material privado para a branch pública de OTA**. A branch OTA recebe apenas APK já assinado e documentação pertinente.

### 3.3 Como lançar próximas atualizações pelo GitHub, passo a passo

1. Partir da branch DEV `feat/anytv-p0-device-proof-verifier-20261020`, preservar correções, nunca modificar canal antigo ou companion TV como efeito colateral.
2. Concluir alterações e testes de fontes no `overrides/` e `scripts/` — particularmente `TclStreamingScreens.kt`, `TclInstalledApps.kt`, `TclApprovedHomeScreen.kt` e testes P0/visual. **Não publicar o helper iconográfico sem integrá-lo à UI; e não contar só o P0 isolado como prova de release.**
3. Usar `.github/workflows/release-anytv-phone-v2-new-package.yml` **existente**, com `.github/anytv-phone-v2-build-request.json` como gatilho. Para próxima release, atualizar os checks fixos do YAML e o pedido com `expected_version_code: 47`, `versionCode = 47`, `versionName = "2.9.18-AnyTV"`, nomes de APK `2_9_18`, monotonicidade (code antigo <47), preservando package e assinatura. **Nunca colocar o segredo em YAML literal ou criar novo keystore**. Manter `preserve_legacy_ota=true`.
4. Runner clona upstream pinado, aplica os patches validados, compila `assembleRelease`, testa `RemoteImeWireTest` e todos contratos, gera unsigned, lê `secrets.ANYTV_SIGNER_V2_RECOVERY_HEX`, abre backup cifrado, verifica fingerprint, executa `apksigner` com PKCS12, verifica pacote/cert/assinatura/versionCode.
5. Apenas com todos gates verdes publicar binário em `updates/android/v2/current.apk` na branch `feat/tcl-v22-updates-back-bluetooth`, SEM alterar `updates/android/cert2026/current.apk`. O workflow verifica o download remoto byte-a-byte, oferece artefato Actions assinado e termina **SUCCESS**.
6. Obter run ID, SHA256 final do APK, Git blob e commit OTA; validar que botão **Buscar atualizações** do V2 detecta versão maior, e testar no Android/TCL. Somente depois declarar entrega funcional real.
7. Se ocorrer `ANYTV_SIGNER_V2_RECOVERY_HEX missing`, **não gerar nova identidade**, não usar a chave do Drive e não publicar um APK com novo certificado: corrigir acesso ao mesmo secret no GitHub Actions conforme controles autorizados. Nunca acessar valores secretos em chat.
8. Não alterar o `signing-recovery/v2/identity.json` e fingerprint, e não reusar a linhagem `cert2026`.

### 3.4 Evidência das publicações V2

| Release V2 | code | GitHub Actions SUCCESS |
| --- | ---: | ---: |
| 2.9.13 V2 primeira instalação | 42 | 39998890413 |
| 2.9.14 | 43 | 40003530305 |
| 2.9.15 | 44 | 40008906467 |
| 2.9.16 | 45 | 40014131929 |
| **2.9.17 atual** | **46** | **40021931010** |

**NÃO há release V2 2.9.18 confirmado.** As compilações falhas intermediárias (testes de contrato e Kotlin) foram corrigidas antes da publicação 2.9.17; não usar runs de falha como evidência.

## 4. ÚLTIMA SOLICITAÇÃO DO USUÁRIO — DESIGN EXATO DA 2.9.18

Esta é a tarefa ATIVA para o próximo chat. **Não iniciar do zero, não redesenhar o app, não fazer pergunta sobre preferências já decididas.**

### 4.1 Tela principal e identificação da TV

- No topo onde apareciam escolhas como "Android, TCL, Sony", **identificar e apresentar automaticamente a TV atualmente conectada/última pareada**, sem exigir clicar em botões de escolha de fabricante. Quando houver conexão, mostrar nome da TV como **status**, não como controle clicável. Quando não houver TV detectável, manter recuperação manual apropriada; nunca escolher TV aleatória na LAN.
- A 2.9.17 já aplicou `scripts/apply_anytv_restore_connection.py`: desaparecimento involuntário da TV via discovery não significa desconexão manual (`manual=false`). O código não equivale a prova física de auto-reconnect; testar no aparelho.
- DEV commit `070be65` modificou cabeçalho para não mostrar seta/clique de troca TV quando conectado. **AINDA NÃO PUBLICADO**.
- O banner de filmes substitui a faixa antiga de "Aplicativos / Streaming / Ver todos", **mas deve ficar ABAIXO dos ícones reais dos streamings instalados**. A altura **75dp** foi aprovada: não aumentar. Em cada slide, **título do filme à esquerda e arte/backdrop à direita**, eventual seta discreta/convite abaixo do título, sem botão enorme. Avançar automaticamente a cada **3s**, aceitar swipe horizontal, tocar EM QUALQUER LUGAR abre a tela de Streaming.
- DEV commits `2f4d1b2` e `c25505a` já prepararam novo banner e posição; rota de trending corrigida para `discoveryShelf("trending/movie/week", "movie")` sem parâmetro duplicado. Validar a tela realmente renderizada após compilar. **Ainda não publicado no APK**.

### 4.2 Tela Streaming — topo compacto e filtros

- Eliminar palavra "Descobrir" no topo e botões grandes/redundantes.
- **Uma linha só com ícones**: casa (volta ao conjunto filmes+séries), coração Material (Minha lista), símbolo reproduzir (Em andamento), check (Assistidos), TV (abre filtros), busca e X para fechar.
- **Na mesma linha, opções Filmes e Séries** como toggles (selecionou Filmes → só filmes; tocou Filmes de novo → desmarca; analogamente Séries; casa limpa seleção e mostra ambos). Não colocar botão/chip "Tudo" redundante.
- **Gêneros (Ação, Drama, Faroeste, Comédia etc.) em faixa horizontal logo abaixo** como era antes, não converter a uma nova linha de botões repetidos.
- Ícone TV abre popup/painel **duas colunas**, streaming à esquerda e gêneros à direita, permitindo marcar e desmarcar assinaturas/gêneros; salvar preferências e aplicar realmente os filtros à descoberta, sem confundir aplicativo instalado com assinatura efetiva.
- No topo da área Streaming, ícones grandes de cada app devem ser **iguais à home TCL (56dp)**; ao lado da nota dos cards, logos discretos/quadrados (20dp), sem duplicatas. Preservar o espaçamento legível entre capa/nome/linha de nota.
- Helper DEV `AnyTvStreamNavBar` em `overrides/TclStreamingScreens.kt` já define os ícones/toggles (commit `3648613`), **MAS AINDA NÃO FOI INTEGRADO AOS CABEÇALHOS**. A tentativa de integração recebeu bloqueio de segurança da ferramenta GitHub; **não contornar ferramenta, não declarar finalizado**. Integrar somente via rota autorizada de edição, respeitando bloqueio. Filtro de duas colunas também não está pronto.
- As páginas individuais de provedor precisam conservar navegação consistente e retornar ao catálogo, minha lista e histórico sem perda de estado.

### 4.3 Ficha, favoritos, séries, catálogo e cache — PRESERVAR

- Ficha contém título, ano e gêneros reais, sinopse rolável, nota e serviços com ofertas BR confirmadas. Coração e ✓ verde no canto da ficha; botões verdadeiros Abrir na [nome real da TV conectada] / Voltar.
- Os links "Onde assistir" do TMDB/JustWatch **não são URLs profundas automáticas para reproduzir aquele filme**; não prometer playback. Evitar links enganosos.
- `Minha lista`: títulos guardados, filmes para ver, filmes vistos, séries acompanhadas. Acompanhar episódios por temporada/número está funcional como registro local; nomes reais ainda dependem de Worker não implantado.
- Marcar assistido oculta da descoberta normal, mas conserva no histórico Assistidos mesmo sem coração. Não apagar progresso em atualização.
- Cache de página/editorial temporário (6–12h, gêneros 30d, ofertas BR 12h) e **metadados/capas já renderizados sem prazo**, arquivos locais privados, até limpar dados/desinstalar. Reduz franquia TMDB; não conservar indefinidamente uma disponibilidade de direitos mutável.
- Nunca embutir token TMDB/Cloudflare no APK.

## 5. LINKS DIRETOS / ATIVAÇÃO — INVESTIGAÇÃO AINDA ABERTA

O usuário vê "Ativar links diretos neste dispositivo" e **não sabe se links estão ativos**. Alguns filmes não mostram logos/streamings e uma busca por **"O Amor da Sua Vida"** não apresentou link. É necessário diagnosticar **sem fingir que um título tem reprodução oficial**.

Quatro planos diferentes:
1. **Identificar título** (TMDB ID, `movie` vs `tv`, idioma/aliases/ano): títulos parecidos ou novos podem não existir no provedor ou ter mapeamento diferente.
2. **Oferta no Brasil**: `StreamingCatalog.offersInBrazil` usa `results.BR.flatrate` da TMDB/JustWatch, exclui add-on/canais extras e agrupa serviços. Ausência de logo **NÃO prova** falha de ativação; pode significar ausência de oferta registrada no banco ou outro regime de compra/aluguel.
3. **Abrir app / buscar título na TV**: `TclTvAppApi.searchTitle` via companion TV pode abrir aplicativo como fallback, **não garante** deep link ao filme. Depende de app instalado, pareamento e versão/suporte do serviço/companion.
4. **Link direto pessoal e ativação device**: `AnyTvDeviceIdentity.kt` usa identidade/chave específica por instalação no AndroidKeystore e **código individual de autorização de 43 caracteres**; não é segredo TMDB nem chave geral Cloudflare. `AnyTvPersonalLinks.kt` valida domínios/paths e pode retornar nulo inclusive por HTTP 401. O Worker `anytv-link-resolver-pilot.brenoff.workers.dev` usa `/v1/device/activate`, `/v1/resolve`; leitura da configuração de produção confirmou `ANYTV_DEVICE_ACTIVATION_ENABLED=true` e `ANYTV_RESOLVER_AUTH_MODE=device`, com bindings do TMDB/DB/rate-limit presentes **SEM extrair segredos**. **Isso NÃO prova que este celular está registrado/ativado em D1**. Para ativação real é necessário fluxo seguro de provisionamento por dispositivo, não um token compartilhado embutido no APK.

Caso de teste do usuário: **"O Amor da Sua Vida"** (documento DEV de investigação também menciona `Love of Your Life`/2026). Não presumir equivalência ou disponibilidade: conferir ID, título oficial, período, ofertas BR e link suportado por evidência. Nunca criar logo/link sintético.

O Worker **ainda NÃO tem publicadas** as rotas adicionais de temporadas `tv/{id}` e `tv/{id}/season/{n}` que estavam preparadas na fonte GitHub. Tentativa de deploy recebeu bloqueio de autorização (`WRITE_CONFIRMATION_REQUIRED`); não contornar por chamadas alternativas. Registrar blocker e usar rota autorizada. Nunca expor segredo ou quebrar bindings D1/rate-limit.

## 6. GATES DE ENTREGA, DÍVIDAS E ORDEM DE CONTINUAÇÃO

### 6.1 Implementado e comprovado por OTA
- Assinatura V2 recuperada do GitHub em Actions, mesma identidade e distribuição pelo botão.
- Último APK `2.9.17/code46` publicado, validado binariamente.
- Conexão inicial V2 à TCL confirmada pelo usuário em versão anterior; a reconexão automática da 2.9.17 ainda necessita teste do usuário após instalação.
- Recursos de lista, cache e acompanhamento manual e design básico implementados até code46.

### 6.2 Apenas no GitHub DEV — **não publicar como concluído**
- Banner 75dp com título à esquerda/arte direita, posição abaixo da faixa de apps, rota de trend corrigida.
- Cabeçalho da TV conectada somente informativo.
- Helper iconográfico do Streaming definido mas ainda não integrado.
- Runs P0 isolados verdes não substituem Gradle/IME/apksigner/readback.

### 6.3 Próximos passos estritos para o outro chat
1. Ler este documento, o handoff DEV `docs/handoffs/2026-10-23-anytv-2918-ui-links-unfinished.md`, `ADR-0004` e o workflow V2; verificar HEAD e OTA atuais para evitar conflito/conclusões obsoletas.
2. Auditar atual `TclStreamingScreens.kt`, seus cabeçalhos de Discovery e de apps, e helper `AnyTvStreamNavBar`; identificar pontos EXATOS para aplicar sem reescrever o catálogo. Se bloqueio de segurança de edição persistir, não burlar; registrar impedimento.
3. Integrar icon-nav e filtros Filme/Série na mesma linha; remover "Tudo" redundante; preservar faixa gêneros logo abaixo e ativar painel duas colunas serviços/gêneros, persistindo seleção. Criar testes que verifiquem **helper realmente invocado**, estados e filtros aplicados (não só presença da função).
4. Confirmar posicionamento/rendimento do banner da home e feedback conectado/TV; regressão de auto-reconnect do host previamente pareado.
5. Diagnosticar links diretos em 4 camadas e caso "O Amor da Sua Vida"; manter segurança, diferenças entre logo/oferta e deep link. Resolver autorização individual via fluxo aprovado, não segredo embutido.
6. Verificar cache e histórico após upgrade. Não apagar preferências, não alterar a assinatura V2 nem canal antigo.
7. Só após implementação: preparar `versionCode 47` e workflow V2 existente + request; rodar testes P0/visual, Kotlin Gradle release, IME, apksigner, readback OTA; publicar 2.9.18 apenas com **SUCCESS** e hash; orientar usuário a usar **Buscar atualizações**.
8. Após release, fazer QA físico com usuário, registrar resultados e atualizar docs de estado. Manter sem merges automáticos, sem rotas improvisadas e sem drive.

### 6.4 Pendências e riscos que NÃO podem ser ocultados
- Navegação de ícones não integrada; filtro 2 colunas não implementado.
- Requisitos exatos da 2.9.18 ainda não homologados em APK físico.
- TV automática e cabeçalho DEV sem prova física.
- Links diretos dependem de autorização por dispositivo e inventário de URL/servidor; falta diagnóstico E2E do aparelho.
- Código fonte Worker de temporadas não publicado por bloqueio de escrita.
- Não há contingência independente comprovada para o Secret V2; não reexecutar bootstrap para resolver.
- Histórico de documentos pré-V2 pode conter afirmações agora falsas (caminho OTA antigo como atual, recuperação de chave antiga, nunca gerar identidade). Este handoff prevalece.

## 7. TEXTO CURTO PARA INICIAR O PRÓXIMO CHAT

"Retome o **PROJETO ANYTV V2** de onde parou, **sem refazer pesquisa nem perguntas já respondidas**. Leia primeiro `docs/handovers/2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md` e `docs/handoffs/2026-10-23-anytv-2918-ui-links-unfinished.md` no GitHub `brenoff2/tcl-apk-ci-builder`, branch DEV `feat/anytv-p0-device-proof-verifier-20261020`. A última OTA publicada é **2.9.17/code46**, workflow SUCCESS `40021931010`; **2.9.18/code47 NÃO foi construída/publicada**. O último commit DEV preparou banner/home e helper de ícones, mas falta integrar menus e filtros duas colunas, investigar link direto/ativação e caso 'O Amor da Sua Vida', além de teste real de auto-TV. **Assinatura V2 já está no GitHub e deve ser REUTILIZADA**, não recuperada da chave antiga ou gerada novamente: `ANYTV_SIGNER_V2_RECOVERY_HEX` como Actions Secret + `signing-recovery/v2/phone-signer.aesgcm`, `identity.json`, `phone-cert.pem` na DEV, fingerprint `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`; workflow de release `.github/workflows/release-anytv-phone-v2-new-package.yml`. Preserve pacote `com.tclremote.livre.v2`, canal `updates/android/v2/current.apk` da branch OTA e o botão Buscar atualizações; não tocar no cert2026 antigo. Faça alterações mínimas, prove o comportamento em testes/Gradle/IME, publique somente após assinatura correta e byte readback; registre evidências e informe pendências reais. Se uma ferramenta bloquear escrita, não tente contornar."

---
Registro criado a pedido expresso do usuário para transição completa entre chats. Nenhum segredo em claro integra este documento.
