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
