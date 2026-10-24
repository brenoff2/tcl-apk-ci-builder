# AnyTV — QA da continuidade de Assistidos quando TMDB informa 404

**Data:** 24/10/2026. **PR #3 DEV / DRAFT.** Commit funcional: [53c6b8d1](https://github.com/brenoff2/tcl-apk-ci-builder/commit/53c6b8d1fdcd4276174829086193c6b9d32edda5). Nenhuma publicação, merge, assinatura, OTA ou deploy de Worker.

## Causa real

A rota de leitura movie/{id} foi preparada em DEV no commit e6501f52, para recuperar filmes antigos salvos apenas com o ID TMDB. Porém, o Worker transformava qualquer resposta não 2xx do TMDB em HTTP 503, inclusive HTTP 404. O aplicativo tratava 503 como indisponibilidade, interrompendo as tentativas restantes mesmo quando havia filmes válidos depois do ID inexistente. Além disso, os metadados recuperados antes de um cancelamento eram mantidos somente na lista temporária até o final do lote. Isso poderia perder o trabalho parcial quando a pessoa saísse da aba.

**Evidência externa:** [TMDB / Errors](https://developer.themoviedb.org/docs/errors) documenta o status de recurso não encontrado (código interno 34, HTTP 404); [TMDB / Search and Query for Details](https://developer.themoviedb.org/docs/search-and-query-for-details) confirma o acesso por movie ID. **GitHub-first: ADAPT** à allowlist e ao esquema de persistência já existentes, sem acrescentar chamadas de rede direta no celular.

## Correções no código DEV

1. **Worker / anytv-resolver/src/worker.mjs:** somente a rota exata movie/{id} passa a preservar HTTP 404 como catalog_not_found. Outras rotas e falhas TMDB mantêm HTTP 503, inclusive quando o upstream retorna 404 em tv/{id}. Resposta 404 não contém o corpo de erro do TMDB, não expõe credenciais, não é salva no cache D1 e não muda a autenticação, ativação, endpoints de links ou URL de destino.
2. **Android / overrides/TclStreamingScreens.kt:** na aba Assistidos, HTTP 404 marca apenas o ID como ausente nesta sessão e prossegue para o seguinte; o registro de assistido continua preservado em preferências. IDs ausentes/sem dados suficientes deixam de monopolizar os primeiros 12 resultados em cada visita à aba. HTTP 400 do Worker antigo ainda bloqueia chamadas futuras desta sessão; HTTP 503/erro de rede ainda interrompe o lote para tentar novamente numa visita futura.
3. Os filmes válidos recuperados passam a ser salvos em lote **no finally**, de forma que os sucessos obtidos antes do cancelamento da coroutine também sejam persistidos, sem inventar título, imagem ou disponibilidade.
4. **Testes:** worker.test.mjs verifica, com fontes TMDB simuladas, 404 sem cache, próximo filme válido com 200 e cache, outra rota mantendo 503; test_streaming_screens_contract.py verifica a distinção 404/400, avanço da lista, memória de IDs ausentes, persistência parcial em finally, limite de 12, preservação dos IDs e fonte única de metadados.
5. Nada alterado na grade aprovada: posters inteiros **2:3**, **três por linha**, logos originais, hero, Cinema BR, abas, outros provedores e controle TCL.

## Resultado verificável

- [CI unsigned Android #40136181568](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40136181568): **COMPLETED/SUCCESS (69 testes Streaming, 2 credenciais, 11 visuais; Gradle BUILD SUCCESSFUL em 4m45s; com.tclremote.livre.v2/versionCode54 verificados)**. Artefato unsigned: **#12576879607 (anytv-v2-2925-code54-unsigned-audit; 23.287.574 bytes)**.
- [Worker offline #40136181610](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40136181610): **COMPLETED/SUCCESS (35 testes Node PASS, SQL/D1 local PASS, Wrangler dry-run PASS; nenhum deploy)**.
- [Prova isolada #40136181616](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40136181616): **COMPLETED/SUCCESS (55 testes Node PASS)**.

## O que continua pendente

- O **Worker publicado** ainda não recebeu movie/{id}. Sem deploy aprovado, o Android em DEV mantém fallback de HTTP 400 e não pode restaurar remotamente filmes antigos no aparelho real. Testes locais do Worker não confirmam configuração, acessibilidade ou comportamento da Cloudflare em produção.
- QA físico no Android/TCL: histórico com ID inexistente seguido de válido, conexão offline, Worker antigo rejeitando 400, saída durante lote, retomada, persistência após reinício, verificação visual dos posters e ações dos serviços.
- **Não** alterar OTA assinada AnyTV **2.9.25/code54**, código publicado/certificado, worker atual, TV companion, D1 de produção, nem realizar merge. **Roadmap 7/11 (63%)** permanece; M08/M09 IN_QA, M10/M11 BACKLOG, F0–F8 NAO_HOMOLOGADO.

## Readback final (24/10/2026)

O [CI Android #40136181568](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40136181568), na fonte funcional 53c6b8d1, foi **COMPLETED/SUCCESS**: 69 testes de Streaming, 2 de credenciais e 11 visuais; Gradle **BUILD SUCCESSFUL in 4m45s**; `com.tclremote.livre.v2`/versionCode 54 verificados e candidato de auditoria **unsigned #12576879607 (23.287.574 bytes)**. O [Worker #40136181610](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40136181610) foi **COMPLETED/SUCCESS**, 35 testes Node e Wrangler em dry-run. A [prova isolada #40136181616](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40136181616) foi **COMPLETED/SUCCESS**, 55 testes. Atestam apenas contratos/build/candidato, NÃO deployment e NÃO homologação em aparelhos físicos.
