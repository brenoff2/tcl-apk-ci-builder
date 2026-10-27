# HANDOFF CANONICO — PROJETO ANYTV — 27/10/2026

## Identidade e objetivo
- Repo privado: https://github.com/brenoff2/tcl-apk-ci-builder
- Branch de trabalho: feat/anytv-p0-device-proof-verifier-20261020 ; PR #3 para feat/tcl-v22-updates-back-bluetooth.
- Produto: AnyTV para Android/TCL; pacote com.tclremote.livre.v2; atualizacao OTA V2 em updates/android/v2/current.apk na branch feat/tcl-v22-updates-back-bluetooth.
- Meta: app que recomenda filmes, series, animes e cinema no Brasil, com telas responsivas, dados confiaveis de catalogo, filtros, metadata, notas, logos de servicos, detalhes claros, links, sem repeticoes nem desaparecimento de cartazes. Reaproveitar ferramentas e APIs provadas, evitar complexidade desnecessaria e muitas interacoes de 'continue'.

## Estado de publicacao CONFIRMADO, mas nao das ultimas alteracoes
- Ultimo release OTA comprovado: AnyTV 2.9.53, versionCode 82, commit branch OTA cc0d720357acca600029a1887c868e0077f6ccc8; GitHub Actions 40443087444 SUCCESS, inclusive verificacao de bytes publicados.
- Release anterior: AnyTV 2.9.52, versionCode 81, commit OTA 3ab9f6b5ff22a2d0f3d5c4bd3118ef4c461c09ff; workflow 40440550991 SUCCESS. Erro 403 ao publicar OTA foi causado por permissions.contents:read no workflow candidato; corrigido para write em 4ac3bbc1ac83726542e2c8d2ad65bed5016a8a55. Nao pedir ao usuario para ajustar settings sem causa concreta.
- NAO CONFUNDIR: commits de desenvolvimento apos 2.9.53 publicada ainda NAO ESTAO NA OTA e podem exigir bump de versionCode para 83, compilacao, apksigner, CI e readback. Workflow atual ainda chamado 2.9.53/code82; nao republicar sob mesmo code82.

## Problemas relatados e correcoes
1. Repeticoes em varias secoes da home e cartazes que desapareciam ao marcar como assistidos: corrigidos no Kotlin, com deduplicacao transversal dos previews e preservacao do cartaz. Build 2.9.51 code80 SUCCESS run 40399975606, artefato assinado 12687784142. Atualizacoes posteriores 2.9.52 e 2.9.53 publicadas.
2. Filmes brasileiros em cartaz ('Rebelde', 'Maca Podre') apareciam na home sem selo Cinema: indice cinemaIds era carregado parcialmente de modo assincrono (5 paginas + 4 separadas). Commits b44c22d e 3387ad6: paginacao BR ate 42 paginas, cinemaIndexReady fail-closed, home movies liberados apenas apos verificacao; selo apenas quando na lista BR; teste 71ab924. Publicado 2.9.53 code82. Confirmacao visual no aparelho ainda necessaria.
3. Notas absurdas de TMDB com 1 voto: algoritmo editorial ja aplicava prior bayesiano, mas interface exibia media crua. Commits bc8ea95 e 451bc3c: overlays e linha compacta passam a ocultar notas com <20 votos; teste dbc20b7. Ainda NAO PUBLICADO NA OTA; no dialogo de detalhes tambem usado threshold >=20.
4. Tela detalhes: usuario aprovou ordem exata: linha 1 titulo; linha 2 categoria/identidade (Filme/Serie/Anime com generos); linha 3 ano e nota com fonte e confianca. Commit 0c160b3, refinamento para anime em filmes e series 5e31907, testes 96a1e21, 137d950. Nao publicado na OTA. HEAD verificado da branch de trabalho antes de criar este documento: 137d950c78f8f68dccaefe7cca162ab9aab3c92b. CI isolado SUCCESS run 40448636040, compile UNSIGNED SUCCESS run 40448636059, signed V2 candidate run 40448636050 CANCELLED; nao afirmar signed ou release.
5. Serie 'Salario Minimo' (Minimum Wage, 2026) na Netflix Brasil aparentemente sem logo streaming no app; outras series idem. Kotlin StreamingCatalog.offersInBrazil() so consome resultados.BR.flatrate de /tv/{id}/watch/providers via backend TMDB, e StreamingOfferIcons so mostra provedores dessa resposta. Possiveis lacunas TMDB ou backend/caches/aliases: ainda NAO demonstrada resposta HTTP real por ID para essa serie; nao inventar explicacao definitiva. Conferir identidade TMDB e links oficiais Netflix antes de assumir ID/plataforma. Tratar assinaturas, adicionais, aluguel e compra separadamente, sem afirmar Netflix com base em titulo similar.
6. Trakt: configurado no Cloudflare Worker como sinais de trending/watchers e client_id, mas NAO como fonte implementada de notas. Trakt oferece endpoints /movies/{id}/ratings e /shows/{id}/ratings, com nota, votos e distribuicao. Nao confundir viewers com votos; validar resposta real, rate limits, licencia e IDs TMDB cruzados. Trakt nao fornece disponibilidade JustWatch completa para apps terceiros de maneira livre; manter fonte autorizada BR para logos, idealmente enriquecer TMDB com validacao e provider mapping. Nao substitua acesso a streaming por popularidade Trakt.
7. Usuario deseja melhores metadados/categorias para Filmes, Series, Animes e generos. Distinguir anime filme de anime serie e genero (comedia, drama, acao etc.) na ficha; TMDB genre_ids e fontes enriquecedoras Trakt se utilizaveis. Plano ainda pendente de consolidacao/testes/release.
8. IMDb: API oficial licenciada/paga via AWS Data Exchange; nao usar redistribuicao sem direitos. Notas TMDB versus Trakt: amostra de titulos novos discutida no chat, mas valores de agregador nao substituem validacao direta por API. Ideia: Trakt quando votos suficientes, TMDB fallback, fontes sempre identificadas, sem nota combinada ficticia.

## Principais arquivos
- overrides/TclStreamingScreens.kt : Kotlin/Jetpack Compose, catalogo, cinema, ratings, dialogo detalhes, logos streaming.
- scripts/tests/test_streaming_screens_contract.py : testes UI, fonte, seguranca.
- .github/workflows/anytv-v2-candidate-compile.yml : pipeline signed V2 com recuperacao segura do certificado; deve ter contents: write para publicar OTA na branch apropriada.
- docs/dev-context/STATUS.md : status acumulado.
- docs/qa/ANYTV_2951_INTEGRATION_RELEASE_GATE_20261025.md : gates anteriores.
- anytv-resolver : Cloudflare Worker de catalogo/Trakt. Trakt Worker candidato version f927d4e0-c4fd-4a30-a5e3-12910a9664a9 anterior; produção f927... NAO CONFIRMADA, verificar live; nao declarar ativacao Trakt em producao sem readback.

## Continuidade imediata e gates
A. Primeiro: ler AGENTS.md, STATUS.md, este handoff, HEAD da branch e logs recentes; verificar testes/compilacoes reais. A ultima signed run de mudancas recentes foi CANCELLED; corrigir o motivo, sem assumir compile unsigned como prova signed. Inspecionar CI apos commits 5e31907/137d950 e, se necessario, atualizar testes nao alinhados; build assinado versionCode 83, QA aapt/apksigner, publicacao OTA e readback; nao gerar release se falhar.
B. Pesquisar uso licito dos endpoints de notas Trakt e disponibilidade de streaming Brasil, validar em amostras reais, com identificadores cruzados; escolher a melhor arquitetura simples. Implementar notas com amostra minima + fonte (Trakt/TMDB) apenas apos dados reais. Manter Netflix e demais logos apenas se houver disponibilidade BR confirmada; usar diagnósticos de ausencia e retry/cache.
C. Ajustar categorias/genres em detalhes e secoes: 'Filme · Anime · Aventura', 'Serie · Anime · Fantasia', etc., nomes reais TMDB/Trakt, sem inferir categorias falsas. Verificar renderizacao na TCL. 
D. Garantir que a atualizacao interna reconhece codigo maior e nao exige desinstalar APK; publicar OTA apenas com identidade V2 correta e QA. Conferir funcionamento em aparelho de verdade nao provado apenas por CI.
E. Usuario quer avance maximo sem solicitar 'continue' repetidamente, resultado concreto e transicao fiel entre conversas; separar comprovado, implementado e pendente. Nao prometer assincronia sem automacao.

## Historico de IDs
- Branch trabalho HEAD anterior ao handoff: 137d950c78f8f68dccaefe7cca162ab9aab3c92b; commits recentes 5e31907, 96a1e21, 137d950.
- Testes P0 40448636040 PASS; unsigned build 40448636059 PASS; signed candidate 40448636050 CANCELLED.
- Ultima OTA success 2.9.53 code82 commit cc0d7203, run 40443087444.
- Branch principal de atualizacao: feat/tcl-v22-updates-back-bluetooth.
- NAO alterar Worker/OTA alem do necessario nem apresentar estado antigo como atual.

## Primeiro texto no proximo chat
'Retomar PROJETO ANYTV a partir de docs/dev-context/HANDOFF_ANYTV_2026-10-27.md no repo brenoff2/tcl-apk-ci-builder. Conferir HEAD e STATUS antes de atuar. A OTA em producao e 2.9.53 code82; depois dela houve melhorias de notas com minimo 20 votos e header de detalhes (inclusive anime filme/serie) presentes na branch de trabalho, mas nao publicadas. Prioridade: terminar QA e signed build code83, validar fontes de ratings Trakt, resolver ausencia de logos Netflix com evidencia por titulo/BR, melhorar generos e publicar OTA com readback. Trabalhar de ponta a ponta sem dezenas de continues; usar GitHub e evidencia, nao inventar estado.'
