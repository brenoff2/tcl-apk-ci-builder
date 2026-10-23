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
