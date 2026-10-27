# Memoria operacional — PROJETO ANYTV — 2026-10-27

## Identidade e fontes de autoridade
- GitHub: https://github.com/brenoff2/tcl-apk-ci-builder
- Desenvolvimento: `feat/anytv-p0-device-proof-verifier-20261020` no ultimo readback `860ce2b033945dfaf7d88bec7c021fcbb0cd65fa`.
- Canal OTA: `feat/tcl-v22-updates-back-bluetooth` no ultimo readback `cc0d720357acca600029a1887c868e0077f6ccc8`.
- Handoff: `docs/dev-context/HANDOFF_ANYTV_2026-10-27.md`; Status: `docs/dev-context/STATUS.md`; ler tambem `AGENTS.md`, DECISIONS, CODEBASE_MAP e workflows. Priorizar estado GitHub atual sobre este registro.
- A memoria governada multiprojeto via MCP Business OS Hub v2 retornou `ProjectIdentityUnavailable` para `projeto-anytv`, `anytv` e `PROJETO_ANYTV`. Nao vincular a outro projeto nem inventar URI privada OpenViking.

## Estado comprovado
- Ultima OTA publicada AnyTV 2.9.53 / versionCode 82, run 40443087444 SUCCESS, commit de publicacao `cc0d720357acca600029a1887c868e0077f6ccc8`, caminho `updates/android/v2/current.apk`, pacote `com.tclremote.livre.v2`.
- Correcoes ainda NAO publicadas: notas TMDB de interface ocultas quando <20 votos, cabecalho detalhes em 3 linhas, distincao anime filme/serie e melhorias de categorias.
- CI P0 run `40449186540` SUCCESS no HEAD `860ce2b`; compilacao unsigned do codigo recente `40448636059` SUCCESS no SHA `137d950`; tentativa signed `40448636050` CANCELLED com zero jobs. Sem prova de APK assinada posterior a 2.9.53 nem homologacao TCL.
- Trakt: sinais de trending/watchers presentes em branch de desenvolvimento; notas Trakt `/movies/{id}/ratings` e `/shows/{id}/ratings` nao integradas/comprovadas ao vivo. Worker Trakt staged historico `f927d4e0-c4fd-4a30-a5e3-12910a9664a9`, producao anterior `88779b1b-9dbd-4dc6-9884-eff52769ded2`; confirmar runtime atual antes de qualquer decisao. Watchmode FORA do escopo por decisao vigente.
- Streaming BR: `StreamingCatalog.offersInBrazil()` usa `results.BR.flatrate` do TMDB; ausencia de Netflix para serie Salario Minimo e outros titulos ainda sem prova HTTP/IDs por titulo. Nao inventar logos, disponibilidade, notas ou votos.
- Preservar correcoes publicadas de cinema BR, deduplicacao de home e cartazes apos marcar assistidos.

## Impedimento identificado nesta conversa
- Workflow `.github/workflows/anytv-v2-candidate-compile.yml` permanece com versao 2.9.53/code82 e `permissions: contents: write`. Tentativas de `mcp__GitHub__update_file` para alterar o mesmo workflow para 2.9.54/code83 e 2.9.55/code83 foram recusadas antes do commit, com mensagem: `Esta ferramenta foi bloqueada pelas configuracoes de seguranca da OpenAI. Verifique novamente o que esta enviando.`
- A leitura do GitHub funciona; nenhuma das tentativas gerou commit. Nao ha evidencias de que a restricao seja causada por configuracao do usuario, certificado, API GitHub ou compilacao Android. Hipotese: o conteudo integral do workflow envolve assinatura/secret/publicacao, mas a causa interna exata e DESCONHECIDA. Nao contornar bloqueio de seguranca.
- O usuario confirmou que as permissoes GitHub ja estao liberadas e nao deseja rodar PowerShell para substituir o fluxo historico.
- O usuario solicita proxima candidata AnyTV **2.9.55 versionCode 83**, identica ao codigo da branch DEV, com mesmo certificado V2 e atualizacao OTA in-place. Ainda NAO criada, compilada, assinada ou publicada.
- Ao preparar 2.9.55, corrigir todos os indicadores, incluindo `code82` remanescente em nomes de artefatos, guard `oldcode -lt 83`, versionName, badging e referencias ao APK. Nao modificar Android, assinador, updater ou branches de producao sem gate adequado.

## Proximo passo
1. Consultar GitHub HEAD/workflow/CI novamente; identificar via caminho autorizado a capacidade de gravar a mudanca.
2. Testar/validar patch minimo sem regressao; executar signed build code83, conferir certificado esperado no `signing-recovery/v2/identity.json`, aapt/apksigner, artefatos e CI.
3. Publicar apenas se gates permitirem, comparar byte a byte APK OTA com artefato assinado e registrar commit/run/readback; se falhar, manter OTA 2.9.53.
4. QA real na TCL permanece independente do CI, e recursos Trakt/streaming dependem de verificacao de API e fonte.
5. Nao pedir dezenas de "continue", nao afirmar gravacao de memoria ou publicacao sem readback.
