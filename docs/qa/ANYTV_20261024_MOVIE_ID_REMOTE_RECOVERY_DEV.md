# AnyTV — QA: recuperação remota de filmes assistidos somente por ID

**24/10/2026.** PR #3 draft em feat/anytv-p0-device-proof-verifier-20261020. Código [e6501f52](https://github.com/brenoff2/tcl-apk-ci-builder/commit/e6501f5234397de2267c67296d067b23b6b46bc5). Sem merge, assinatura, deploy Worker ou OTA. Publicado segue AnyTV 2.9.25/code54 e branch feat/tcl-v22-updates-back-bluetooth SHA dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112.

## Raiz comprovada

O histórico antigo possui IDs TMDB reais e às vezes não possui nome/cartaz nas preferências, favoritos ou cache. O aplicativo tinha recuperação de séries pelo proxy tv/{id}, mas não podia recuperar filmes porque o Worker publicado rejeita movie/{id} com HTTP 400. O TMDB possui a operação oficial de detalhe por ID: https://developer.themoviedb.org/reference/movie-details.

## Alterações somente em DEV

- Worker: allowlist read-only admite movie/{id} positivo; para esse caminho aceita exclusivamente o parâmetro language=pt-BR; bloqueia demais parâmetros, queries livres, método POST e rotas genéricas. Segredo TMDB permanece apenas no backend, com cache D1. A URL upstream é fixa, nunca vem do usuário. Nenhuma rota de links comerciais ou de ativação foi alterada.
- Contratos Worker: testam ID 550 e resposta TMDB simulada, localização pt-BR, cache com uma única consulta, preservação de segredo, rejeição de ID inválido, parâmetros indevidos, língua estrangeira, parâmetro duplicado e POST.
- Android: StreamingCatalog.lookupMovie(id) consulta apenas o Worker e valida nome e igualdade do ID retornado; data, capa, sinopse, votos, avaliação e gêneros vêm exclusivamente da resposta real. Nunca inventa detalhes, nunca utiliza credencial TMDB no celular.
- Assistidos: migra snapshots locais antes de consultar o servidor; limita a 12 IDs remanescentes por visita. Em Worker antigo, o primeiro HTTP 400 encerra tentativas para a sessão; falhas de rede interrompem a consulta e permitem tentar noutra visita. Cancelamento de coroutine respeitado. Sucessos são salvos em lote nas preferências. IDs ainda não recuperados continuam preservados e visíveis, sem capas fictícias. Texto discreto indica busca em andamento.
- Preservados: grid de três posters 2:3 completos, ícones oficiais dos serviços, indicadores Cinema BR, nome/ano/nota, páginas e navegação anteriores, emparelhamento e controles TCL.

**Gate GitHub-first:** ADAPT a mesma infraestrutura read-only de tv/{id}, conforme documentação oficial de Movie Details TMDB. Nenhuma fonte não autorizada foi adotada.

## Evidência CI

- [Android candidato unsigned #40132378495](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40132378495): **COMPLETED/SUCCESS (68 testes Streaming, 2 credenciais, 11 contratos visuais; Gradle BUILD SUCCESSFUL 3m19s; identidade com.tclremote.livre.v2/code54 verificada)**. Artefato: **#12575398131 (anytv-v2-2925-code54-unsigned-audit; 23.287.325 bytes)**.
- [Worker offline contratos #40132378555](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40132378555): **COMPLETED/SUCCESS (34 testes Node PASS; SQL/D1 PASS; Wrangler --dry-run PASS, sem deploy)**. Inclui testes Node, sintaxe e Wrangler dry-run, sem deploy.
- [Prova isolada #40132378590](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40132378590): **COMPLETED/SUCCESS (54 testes PASS)**.

## Limites e próximos gates

O Worker realmente publicado **ainda não tem** movie/{id}; só a fonte DEV foi alterada. Portanto, a restauração de IDs sem dados locais **não está disponível no app publicado**; o código de desenvolvimento trata o HTTP 400 sem apagar o histórico. Para funcionalidade remota real é necessário autorizar deploy isolado do Worker e homologar endpoint, depois homologar o APK no Android físico, a TCL, a navegação e a persistência.

Não realizar deploy, merge, assinatura ou OTA sem aprovação. O candidato unsigned/code54 não pode substituir a versão assinada. Futuro release requer versionCode maior que 54, certificado original e aceitação visual. Gates mantidos: 7/11 (63%), M08/M09 IN_QA, M10/M11 BACKLOG, F0–F8 NAO_HOMOLOGADO.

## Readback da evidência final em 24/10/2026

O CI Android #40132378495 terminou **COMPLETED/SUCCESS** no commit funcional **e6501f52** com **68 testes Streaming + 2 credenciais + 11 visuais**, Gradle **BUILD SUCCESSFUL em 3m19s**, identidade **com.tclremote.livre.v2, versionCode 54**, artefato unsigned **#12575398131**, 23.287.325 bytes. O Worker offline #40132378555 terminou **SUCCESS**, com **34 testes Node**, migração SQL local e Wrangler **dry-run** sem deploy. A prova isolada #40132378590 terminou **SUCCESS, 54 testes**. Nenhum desses resultados constitui homologação Android/TCL físicos nem atualização do Worker publicado. O Worker atual só passa a aceitar movie/{id} após deploy independente, auditado e aprovado.
