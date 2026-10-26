> **GATE NOVA APK — 25/10/2026:** a pedido do usuário, nenhuma próxima APK será considerada pronta antes de curadoria real auditada, Trakt ATIVO/HTTP real e Watchmode integrado com licença válida/HTTP real. Documento normativo: [ANYTV_2951_INTEGRATION_RELEASE_GATE_20261025.md](../qa/ANYTV_2951_INTEGRATION_RELEASE_GATE_20261025.md), commit b820b2ee7652031841d58514cbc1a7dc42a2ec6a. Trakt apenas preparado, Watchmode NÃO integrado, sem contratação aprovada. Não disparar build-request/OTA, não afirmar homologação. 

## DEV 25/10/2026 — fontes de novas temporadas (nao publicado)
- `recentlyStartedSeasons` passou a combinar `tv/on_the_air` e `trending/tv/week`, deduplicados por TMDB ID, antes de verificar temporada posterior com data de estreia nos ultimos 90 dias. Preserva maximo de 60 candidatas, concorrencia de 4 consultas de temporadas, exclusao de Anime e series estreantes. Codigo [00b850ad](https://github.com/brenoff2/tcl-apk-ci-builder/commit/00b850ad61521f6052986e75f1937c51000b77b2), teste [5efce875](https://github.com/brenoff2/tcl-apk-ci-builder/commit/5efce875570d0c4311ea37461eb4192c7d6d4370). Blobs Kotlin d14f8bd5fda064535ea599de3a53ac4e9335549b, teste 058a742ce71bb11194a3e75eedc1ea067c046a1c. CI/Gradle=NAO_TESTADO nesta etapa; assinatura/OTA=NAO; QA fisico/editorial=NAO_HOMOLOGADO; release documentada permanece 2.9.50/code79.

## Reconciliacao DEV — 25/10/2026
- Aplicar exclusao de filmes ja assistidos uniformemente, inclusive nas secoes de estreias com retorno antecipado, sem remover series acompanhadas automaticamente. Mudanca cirurgica em `anyTvCurate`, ainda nao compilada nem publicada; nao altera proporcao editorial, fontes ou regras de streamings. Commits `8d61aed11cfe5fcaf2b7c19071639203255724c0` e `09e8b8e6d1c01e3ded31278722e67d05c4dc2a3f`.

## Regra obrigatoria de sincronizacao documental (25/10/2026)
- Em toda tarefa de desenvolvimento ou transicao, ler e reconciliar `AGENTS.md`, `docs/dev-context/STATUS.md`, `docs/dev-context/DECISIONS.md`, `docs/dev-context/CODEBASE_MAP.md` e o checkpoint canonico vigente. Antes de alterar codigo, identificar branch, HEAD, blobs relevantes e release/OTA verificadas; nao pressupor que o ultimo status documental e o estado live.
- Toda mudanca relevante (codigo, contratos, arquitetura, dependencias, tests, releases, Worker/OTA, interfaces, decisoes, bloqueios ou homologacoes) exige atualizar, NA MESMA ENTREGA, os arquivos afetados: AGENTS (regras e ponteiro canonico), STATUS (estado e evidencias), DECISIONS (decisoes vigentes e substituicoes), CODEBASE_MAP (paths, responsabilidades e SHAs/ancoras), e checkpoint canonico (handoff). Nao basta atualizar somente um arquivo. Se algum nao sofrer mudanca material, registrar explicitamente a verificacao de nao-necessidade.
- Ao publicar release, atualizar em primeiro plano a versao, versionCode, links de workflow/artefato, commit e canal OTA. Distinguir `BUILD`, `ASSINADO`, `OTA_PUBLICADA`, `READBACK_VERIFICADO`, `QA_FISICO` e `QA_EDITORIAL`; escrever `NAO_TESTADO` onde faltar prova. Referencias antigas devem ser rotuladas HISTORICO, nunca utilizadas como estado corrente.
- Reconciliar conflitos usando evidencias do GitHub no ref correto e runtime real quando acessivel. Nao inventar SHA do HEAD, deploy, teste ou contagem. Usar commits pequenos de documentacao sem acionar build-request; confirmar por readback os arquivos alterados e relacionar links/SHAs no fechamento. Em falha de acesso ou sincronizacao, sinalizar PENDENTE, sem declarar fechamento completo.
- Preservar pacote `com.tclremote.livre.v2`, signer, OTA V2, OTA legada, Worker/segredos e PR #3 sem merge/deploy/publicacao nao autorizados. Regras de produto e historico anteriores permanecem validos salvo revogacao expressa.

## Curadoria V2 — 2.9.50/code79 (25/10/2026)
- Principal e páginas de cada streaming reutilizam a **mesma função de curadoria**, com candidatos distintos. A página de streaming consulta exclusivamente o provedor BR em modalidade `flatrate`, sem confundir aluguel, compra ou canais adicionais com assinatura básica. A presença do título em provedores não garante acesso a episódios/temporadas específicos.
- Máximo de **50 títulos reais por seção**. O alvo editorial é **40% alinhamento com gêneros escolhidos/favoritados, 35% consenso de qualidade e tendências, 25% descobertas**; nunca completar quotas com conteúdos falsos ou repetidos. Popularidade histórica isolada não decide ordem. Priorização de atualidade é mais forte nas seções de destaque e lançamento.
- A Principal passa a incluir Destaques da semana, Filmes recém-lançados, Séries novas, Novas temporadas, Em alta agora, Recomendados para você, Você pode gostar, Surpresas do catálogo, Vale a pena assistir, Populares no TMDB e Mais bem avaliados; Minisséries imperdíveis somente com prova `type=Miniseries`. Páginas por streaming compartilham nove grupos verificados e mantém exploração genérica depois da curadoria; mostrar somente grupos com títulos comprovados.
- `Populares no TMDB` substitui alegação de “mais assistidos”: popularidade TMDB não equivale a audiência real.
- Trakt Standard é sinal opcional de público por TMDB ID, obtido apenas no Worker sem chave no APK, não um segundo catálogo irrestrito. A fonte ausente preserva rankeamento TMDB. **Não declarar Trakt ativo até Client ID, deploy e readback.** API Standard requer marca/atribuição e proíbe espelhamento ou coleta massiva.
- Watchmode somente pesquisa/teste futuro com credencial e licença aprovadas; **nenhum uso pago automático**. Cinema e Anime preservam exclusividade, e lançamentos continuam limitados por datas factuais.
- A release Android 2.9.50/code79 foi publicada pela [execução #40250636867](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867), OTA [commit 7ef4e20](https://github.com/brenoff2/tcl-apk-ci-builder/commit/7ef4e20b6b0e9a414455d8c1babf10664311ea1e). Teste físico e curadoria real ainda pendem de homologação.

## Complemento aprovado — 2.9.48/code77 (25/10/2026)
- Gênero e Anime: modo padrão é lista Detalhada. O botão «Ver em seções» **deve ativar seções de fato** com o mesmo componente horizontal do Streaming, agrupando Filmes, Séries, Lançamentos recentes e Mais bem avaliados, sem duplicação dos trilhos com a lista. O texto do botão mostra a próxima ação, nunca o estado invertido.
- Lista Detalhada: espaçamento de metadados compacto de 1dp, linha de provedores de altura mínima 16dp, sinopse lineHeight 15sp; preservar título, gêneros, badges, logos e dois botões distintos.
- Slide principal: usar somente `StreamingOfferIcons` para F no quadrado azul/S no círculo vermelho, ano e indicador Cinema quando confirmado, sem texto F/S solto e sem duplicar Cinema.
- «Filmes recém-lançados» e «Séries novas»: descobrir títulos em fontes TMDB permitidas pelo Worker, combinando páginas ordenadas por popularidade e data, filtrar estreia original nos últimos 90 dias e excluir Anime. Filmes encontrados no catálogo Cinema BR corrente pertencem exclusivamente à página Cinema. Oferta flatrate BR **não é pré-requisito** para classificação factual de estreia; disponibilidade para assistir é informação separada.
- «Recém-chegados ao Streaming»: não usar atualidade do filme, popularidade ou primeira consulta como prova. Persistir provedores BR observados por título; somente um novo provedor detectado em comparação posterior com o mesmo título autoriza inclusão. Primeira execução faz baseline e pode mostrar seção vazia. Histórico guarda evidência observacional local por 30 dias, não garante data exata de inclusão na plataforma.
- Publicação: preservar signer V2 e cert2026. Verificar branch remota e bytes pelo **SHA imutável do commit**, para evitar falsos negativos do cache CDN da URL de branch.
- QA física e visual não pode ser afirmada sem prova no aparelho; CI de contrato/Gradle/OTA não substitui essa etapa.

## Organização de lançamentos aprovada — 25/10/2026
- Remover a seção genérica «Novidades», anteriormente alimentada por `movie/now_playing` e `tv/on_the_air`, pois confundia Cinema, séries em andamento e estreias.
- «Filmes recém-lançados»: considerar estreias até 90 dias e oferta `flatrate` BR comprovada, excluindo títulos identificados como em cartaz na consulta Cinema BR. Datas regionais específicas requerem fontes/rotas adicionais verificadas.
- «Séries novas»: usar a primeira data de estreia, não o status de exibição.
- «Novas temporadas»: distinguir temporadas adicionais cuja data de início esteja nos últimos 90 dias; não tratá-las como séries novas.
- «Recém-chegados ao Streaming»: não inferir entrada recente apenas por presença no TMDB/JustWatch. Até existir histórico real de entradas por serviço, mostrar indisponibilidade de evidência, sem listar falsos recém-chegados.
- Anime japonês (gênero animação + idioma original `ja`) deve ficar isolado dos catálogos gerais; página Anime usa descoberta TMDB por gênero 16 e filtragem local da origem. O Worker de produção rejeita `with_original_language` e campos de data não permitidos com HTTP 400. Não alterar a allowlist do Worker só para suprir filtros de interface; optar por consultas permitidas e filtro local, com teste HTTP real antes da assinatura.
- Página Gênero/Anime usa lista Detalhada como padrão e alternância para seções, sem trilhos duplicados.
- A publicação 2.9.45/code74 confirma build, teste de Worker, assinatura e OTA, mas **não** confirma homologação física de telas e dados no dispositivo.

# AnyTV — Decisões vigentes
Atualizado em 2026-10-25. Fonte: checkpoint canônico 2.9.39 e arquivos de implementação consultados na branch DEV. Decisões não documentadas permanecem em aberto.

## Identidade e continuidade
- Preservar projeto e implementação existentes; desenvolvimento pelo ChatGPT Web sem dependência obrigatória de Codex.
- GitHub é fonte de verdade; consultar `AGENTS.md`, `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md`, este diretório e código real antes de editar.
- DEV: `feat/anytv-p0-device-proof-verifier-20261020`; PR #3 permanece Draft, sem merge automático.
- Não confundir estado de CI, build unsigned, APK assinado, publicação OTA e homologação física.
- Não alterar produção, Worker, D1, companion TCL, chaves, signer, updater ou OTA legada sem necessidade e autorização expressa.
- Aperfeiçoamentos novos são CANDIDATA até aprovação; não reestruturar arquitetura por conveniência.

## Produto e interface aprovados
- Home / controle remoto: faixa de apps deve exibir somente aplicativos efetivamente detectados na televisão; Editar seleciona esses apps.
- Streaming: exploração independente da presença do aplicativo na TV; Editar possui «Na minha TV» e «Outros streamings»; seleção persistente e sem duplicatas; contorno vermelho indica seleção.
- Instalação, assinatura e capacidade de reproduzir são conceitos diferentes; nunca prometer reprodução sem verificação.
- TMDB/JustWatch BR; flatrate confirmado distinto de aluguel/compra e de assinaturas adicionais como Universal+ Amazon Channel; não excluir provedores simplesmente por Channel/Canal/Addon.
- Max e HBO Max são uma identidade de serviço para deduplicação; logotipos oficiais TMDB quando válidos.
- Cinema e Em breve exigem classificação cuidadosa; preservar regressões A Odisseia (2026) e Crystal Lake (2026).
- Identidade visual aprovada sem marca indevida nas páginas internas; gradiente/sombra nos títulos, indicação de ano e indicadores minimalistas.
- Grade do catálogo: 3 colunas; modo Detalhado: cartaz isolado à esquerda; direita título, gêneros, indicador F/S + ano + logos, sinopse e botões separados Trailer / Mais detalhes.
- Não duplicar metadados abaixo do cartaz no modo Detalhado. Preservar favoritos, assistidos, acompanhamento e filtros.
- «Ver em lista» / «Ver em seções» à direita, com aparência de botão; subtítulo com barra vermelha e espaçamento coerente com Gênero; Grade, Detalhado e Ordenar na linha abaixo.
- Ordenação dos resultados já carregados: Mais populares, Melhor nota, Mais recentes e Título A–Z; não afirmar ordenação global da biblioteca.

## Versionamento e entrega
- Última OTA confirmada pelo checkpoint: 2.9.39/code68; próxima release, se autorizada, precisa de versionCode >68.
- Preservar pacote `com.tclremote.livre.v2`, certificado original e rota OTA legada.
- Correção não é entregue apenas com commit: requer testes + compilação + assinatura + OTA oficial + byte-readback quando houver autorização para publicar.
- Sem teste físico, registrar NÃO_TESTADO e não promover gates de homologação.
- Evitar «continue» repetidos: realizar ciclos completos e comunicar evidências.

## Conflitos e precedência
- `AGENTS.md` contém resumos cronológicos mais antigos; a entrada referente a 2.9.31 é histórica. Para o marco de 2.9.39, priorizar canônico e conferir GitHub/OTA em tempo real.
- Este arquivo registra o que já está fundamentado, não é autorização autônoma para alterar produção nem substitui validação física.
