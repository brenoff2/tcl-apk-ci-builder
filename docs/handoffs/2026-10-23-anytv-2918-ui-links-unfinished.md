> **CONTINUIDADE ATUAL:** [Handoff integral, com assinatura e próximos passos](../handovers/2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md) | [Runbook V2](../operations/ANYTV_V2_SIGNER_RECOVERY_AND_RELEASE_RUNBOOK.md). Até nova compilação e publicação verificadas, V2 2.9.17/code46 é a versão ativa. Este documento registra trabalho PARCIAL de código pós-2.9.17; não afirmar que a barra de ícones foi integrada ou que existem links diretos válidos.

> **🔴 LEITURA PRIORITÁRIA — ESTADO ANYTV V2 EM 23/10/2026:** [Handoff canônico 2.9.17 → futura 2.9.18](../handovers/2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md) e [Runbook da assinatura V2](../operations/ANYTV_V2_SIGNER_RECOVERY_AND_RELEASE_RUNBOOK.md). **APK instalado/publicado: 2.9.17/code46**, pacote `com.tclremote.livre.v2`, run `40021931010` SUCCESS, canal `updates/android/v2/current.apk`; **alterações posteriores na DEV ainda NÃO publicadas**, helper `AnyTvStreamNavBar` NÃO integrado, filtro streamings/gêneros em 2 colunas NÃO implementado, links diretos NÃO homologados. Assinatura V2 existente no GitHub (NÃO gerar/recuperar a antiga): Secret Actions `ANYTV_SIGNER_V2_RECOVERY_HEX`, backup cifrado `signing-recovery/v2/phone-signer.aesgcm`, fingerprint `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. O texto histórico abaixo pode descrever a linha `cert2026` e **NÃO** substitui o estado atual da V2.

# AnyTV V2 — revisão 2.9.18 pendente (23/10/2026)
Status: **não publicada**; o último APK autorizado permanece **2.9.17 / code 46** no canal V2.

## Requisitos confirmados pelo usuário
1. Identificar a TV pareada automaticamente. O cabeçalho só exibe o nome do aparelho quando conectado, sem affordance de seletor. A desconexão intencional continua respeitada; quando não reconectável, manter opção de recuperação manual.
2. Preservar a altura do banner **75dp**, com filme em destaque, **nome à esquerda e imagem à direita**, slide lateral cada 3s, toque em qualquer lugar abre Streaming. Banner fica **ABAIXO** dos ícones reais de streamings da TV.
3. Topo da descoberta: **apenas ícones** de Início (casa), Minha lista (coração Material), Em andamento (reprodução), Assistidos (check), filtros (TV), busca e X; seletores **Filmes** e **Séries** na mesma linha. Um toque seleciona; segundo toque no mesmo desmarca; casa restaura ambos. Não usar chip "Tudo" nessa linha.
4. Faixa dos **gêneros** abaixo permanece horizontal, como antes. O ícone TV abre filtro **duas colunas**: serviços à esquerda, gêneros à direita, persistência e uso efetivo na descoberta. Não usar botões textuais redundantes.
5. O link inferior de ativação individual não deve ser confundido com chave de API nem expor promessa de funcionamento. Não inserir token compartilhado no APK. Verificar o estado real da identidade antes de chamar "links ativos". Distinguir catálogo TMDB/JustWatch BR, disponibilidade por assinatura, ID/deep link verdadeiro e mero comando de abrir o app na TV. Não afirmar reprodução sem prova.
6. "O Amor da Sua Vida" / Love of Your Life (2026), lançamento em Prime Video em 14/10/2026 segundo fontes públicas (Terra e AdoroCinema), é caso de verificação de cobertura dos dados e do mapeamento dos serviços. Sem identificador TMDB confirmado ou oferta da API BR, não inventar logo ou link.

## Alterações já gravadas no GitHub DEV (não há build request para code47)
- `overrides/TclStreamingScreens.kt`: corrigida rota do banner para `discoveryShelf("trending/movie/week", "movie")` (antes havia `?language=pt-BR` indevido); banner interativo 75dp com texto à esquerda, backdrop/poster à direita, avanço de 3s e toque abre Streaming (commit `2f4d1b2`).
- `overrides/TclInstalledApps.kt`: deslocado banner para depois da faixa de ícones dos apps instalados (commit `c25505a`).
- `overrides/TclApprovedHomeScreen.kt`: cabeçalho conectado deixa de ter clique e seta de trocar TV. Se desconectado, mantém recuperação manual (commit `070be65`). Isto **não confirma** por si só a reconexão física, que depende do mecanismo de último host já implementado.
- `overrides/TclStreamingScreens.kt`: helper `AnyTvStreamNavBar` iconográfico **definido, mas AINDA NÃO INTEGRADO à UI** (commit `3648613`).
- `scripts/tests/test_streaming_screens_contract.py`: teste da rota e ordem do banner atualizado (commit `06b0203`).

## Bloqueio de integração
A tentativa de integrar o helper ao cabeçalho e estado dos filtros no arquivo `TclStreamingScreens.kt` retornou bloqueio de segurança da ferramenta GitHub; **não repetir via ferramentas alternativas nem tentar contornar o bloqueio**. Nenhum APK novo foi gerado, assinado ou publicado; o canal V2 code46 segue íntegro. Os testes de código das alterações parciais ainda não foram executados em CI (o fluxo de release só é acionado pelo build-request). O desenvolvimento está incompleto; o helper de navegação não deve ser representado como aplicado.

## Investigação de links
- `overrides/AnyTvDeviceIdentity.kt`: ativação por código individual de 43 caracteres e chave por instalação em Android Keystore. Isso NÃO é a chave TMDB nem a chave compartilhada Cloudflare.
- `anytv-resolver/src/worker.mjs`: `/v1/device/activate` depende de `ANYTV_DEVICE_ACTIVATION_ENABLED == "true"`; `/v1/resolve` usa autenticação `pilot`/ `device` e pode retornar 401. Não há endpoint claro de estado da ativação em produção.
- `AnyTvPersonalLinks.resolve` devolve null em respostas HTTP não-2xx, incluindo 401; sem teste real do aparelho não é possível declarar que esteja ativo.
- `StreamingCatalog.offersInBrazil` consulta apenas `results.BR.flatrate` do TMDB/JustWatch, exclui canais adicionais; sem oferta nessa base o indicador não aparece. A documentação oficial TMDB esclarece que os endpoints de watch provider **não retornam deep links finais**.
- `TclTvAppApi.searchTitle` tem fallback de abrir aplicativo quando `AnyTV Connect 1.6` não suporta `/title-search`; não afirmar que abre automaticamente o filme.
- A UI atual ainda mantém o link inferior "Ativar links diretos neste dispositivo", a ser removido/reclassificado quando houver edição autorizada e validação.

## Readback do Worker ativo (23/10/2026)
Inspeção read-only na configuração oficial do Cloudflare confirmou `ANYTV_DEVICE_ACTIVATION_ENABLED=true` e `ANYTV_RESOLVER_AUTH_MODE=device`. Bindings `ANYTV_MOVIE_API_KEY` (secret), `TMDB_READ_TOKEN` (secret), `DB` (D1) e `RATE_LIMIT` presentes; **nenhum valor de segredo foi lido**. Significa que a função de ativação está habilitada no Worker, mas **não prova que este celular específico foi ativado** nem que cada título terá link direto. A autorização concreta depende da identidade e cadastro em D1, e da cobertura de links da plataforma. A ausência de um ícone TMDB/JustWatch não é por si só falha de ativação.

## Gate de release
Somente após concluir a integração e executar testes de UI + Gradle + IME remoto, assinar com a MESMA identidade V2 e publicar pelo canal `updates/android/v2/current.apk`, checando versionCode superior, certificado, hash e byte readback. Nunca promover versão parcial.

## Auditoria de continuidade GitHub ao vivo (23/10/2026, após HEAD c2cd853)

**Somente leitura de produto e documentação — nenhum APK foi compilado, assinado, publicado, instalado ou homologado nesta auditoria.**

- GitHub DEV HEAD confirmado por git ref: `c2cd853880e82bbaa0481ba74161d30cefc7c769`; último commit altera links do runbook, sem integração de interface. GitHub OTA HEAD: `f504025ea6369050c1dd72d0697cf9fca2da470f`.
- Árvore OTA confirma `updates/android/v2/current.apk` git blob `b1a7fab2ffb1963669abbbe5e635770d8901022f`, tamanho 28.396.388 bytes, igual à evidência canônica 2.9.17/code46. Run de release `40021931010` continua `completed/success`. SHA256 do APK permanece o do readback daquela publicação; nenhum novo hash byte a byte foi calculado nesta auditoria.
- `overrides/TclStreamingScreens.kt` tem **uma única ocorrência** de `AnyTvStreamNavBar(` — sua declaração; não há invocação. A interface ainda utiliza os `FilterChip` textuais antigos e o seletor redundante `Tudo`. O diálogo `showMyStreamingSettings` lista apenas `displayed.filter { StreamingCatalog.streaming(it) }`, não um catálogo independente de serviços. O filtro solicitado em duas colunas não existe.
- Potencial falha de compilação identificada **sem executar Gradle**: duas anotações `@Composable` consecutivas antecedem `private fun AnyTvStreamNavBar` (linhas 677–680). A definição oficial de `Composable` não inclui `@Repeatable`. Marcar **NAO_TESTADO** até build real; remover apenas a duplicação quando a edição da interface estiver autorizada e disponível.
- A ativação do device continua não comprovada por aparelho: o Worker GitHub não contém rota explícita `/v1/device/status`; `AnyTvPersonalLinks.resolve` transforma respostas não-2xx, inclusive 401, em resultado nulo, sem diagnóstico na UI.
- `StreamingCatalog.offersInBrazil` consulta somente `results.BR.flatrate`; isso atende aos ícones por assinatura, mas não representa compra, aluguel ou canais extras. Não interpretar a ausência do logo como ausência de direito/licenciamento fora dessa modalidade.
- Caso de teste `O Amor da Sua Vida` (`Love of Your Life`, 2026): página **oficial** encontrada em `https://www.primevideo.com/detail/0HM3DRN79M0E7QOBLMYTS41IG0` e estreia BR noticiada em 14/10/2026. **Não** confundir página web com deep link Android/TV testado. O validador atual de links Prime aceita apenas `app.primevideo.com/detail?gti=amzn1.dv.gti...`; o URL encontrado **não passaria** no validador atual. TMDB ID do título e retorno BR da API ainda não confirmados.
- A API GitHub reportou o repositório como `visibility=public`, apesar dos handoffs dizerem `privado`. Esta divergência exige avaliação de visibilidade/custódia; nenhuma configuração de acesso ou segredo foi alterada.
- `docs/dev-context/STATUS.md`, `DECISIONS.md` e `CODEBASE_MAP.md` **não existem nesta branch** no HEAD conferido; consultar `AGENTS.md`, `docs/STATUS.md`, handoff e o código real, sem inventar um mapa correspondente.

**Decisão:** BLOCK quanto à integração do helper no arquivo que recebeu bloqueio anterior de escrita: não repetir nem contornar a restrição. Readback da árvore, código e documentação concluído; testes de compilação, UI integrada, reconexão real, ativação e OTA 2.9.18 = **NAO_TESTADO**. Não alterar assinatura V2, pacote, OTA, updater, companion TV ou build request.
