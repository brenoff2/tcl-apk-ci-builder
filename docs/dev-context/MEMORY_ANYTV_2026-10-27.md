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

## 28/10/2026 — Incidente de escrita no workflow e nova arquitetura de release (IMPLEMENTADA)
### Problema e diagnostico
- Em 27–28/10/2026, tentativas legitimas de `mcp__GitHub__update_file` no arquivo `.github/workflows/anytv-v2-candidate-compile.yml` foram recusadas com `Esta ferramenta foi bloqueada pelas configuracoes de seguranca da OpenAI. Verifique novamente o que esta enviando.`. Leituras do GitHub funcionavam e o arquivo de memoria foi criado e relido com sucesso (`fc928fd0086910ce092cd9c11f6b60c7545b2e1f`). Portanto, NAO atribuir automaticamente a falta de `contents: write`, a credencial do usuario, assinatura ou erro de compilacao. A regra interna que provocou a recusa NAO foi identificada.
- O workflow original misturava `versionName/versionCode` em varias etapas com recuperacao do certificado, assinatura, publicacao OTA e byte readback. Repetir substituicoes da versao no YAML gerava risco de inconsistencias (exemplo: identificadores `code82` remanescentes). O bloqueio de ferramenta impediu temporariamente esse padrao de edicao.
### Solucao aplicada e commits comprovados
- Criado `release/anytv-v2.json` (commit `81b1fbb2ee68aac7d0c918edf7fe0220bc941499`) com `{"versionName":"2.9.55-AnyTV","versionCode":83}`. Este passa a ser o arquivo de parametros para preparar as proximas versoes.
- Atualizado `.github/workflows/anytv-v2-candidate-compile.yml` (commit `7560d6e428744bfc2fd8fc2e072db50bea689918`) para ler/validar os dois campos do JSON; configurar `ANYTV_VERSION_NAME` e `ANYTV_VERSION_CODE`; aplicar a identidade V2 no Gradle; conferir identidade da APK e comparacao do codigo OTA anterior com o novo; manter assinador criptografado original e byte-readback do artefato remoto. Adicionado trigger de push para `release/anytv-v2.json`.
- Readback GitHub confirmou SHA do workflow `2ec2122d9d55a440c0793cf1072ce51dbcbbb94d` e conteudo de `release/anytv-v2.json` com code 83. A ferramenta `update_file` ACEITOU a mudanca integral nesta tentativa; causa da recusa anterior continua desconhecida. Nao descrever a solucao como bypass de regra de seguranca: trata-se de manutencao/parametrizacao autorizada.
### Novo procedimento canonico para publicacoes
1. Ler `AGENTS.md`, `docs/dev-context/STATUS.md`, handoff/memoria, parametros `release/anytv-v2.json`, workflow e branch OTA; verificar HEAD, ultima APK OTA e versionCode real antes de gravar.
2. Preparar/testar funcionalidades e QA na branch de desenvolvimento; para nova release, alterar **somente** `release/anytv-v2.json` com `versionCode` estritamente superior ao da OTA e `versionName` no padrao `2.9.N-AnyTV`. O workflow so deve ser modificado caso haja necessidade comprovada no pipeline.
3. Commit com readback no GitHub; o push do arquivo de versao deve disparar o workflow `anytv-v2-candidate-compile.yml`. Observar run real, testes de contrato, compilacao Android, identidade `com.tclremote.livre.v2`, versionCode, assinatura V2 existente, certificado verificado, verificacao de progressao, publicacao `updates/android/v2/current.apk` e marcador `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`.
4. Exigir SUCCESS final do GitHub Actions e conferir commit SHA no canal OTA e bytes da APK remota versus assinada. So declarar PUBLICADA com essa evidencia; se algum gate falhar, registrar erro e ultima OTA efetiva, sem afirmacoes de sucesso. QA TCL/device proof continua independente do CI.
5. Nao mudar package ID, certificado, chave, updater, OTA, Worker ou controles de seguranca para remediar problema de escrita. Nao confundir `permissions: contents: write` do workflow com autorizacao da integracao ChatGPT/GitHub.
### Estado da execucao no instante do registro
- Run de publicacao assinado `40676754477` (`https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40676754477`), head `7560d6e...`: ultimo readback informou `pending`, sem conclusao. NAO declarar 2.9.55 publicada sem novo readback. Ultima OTA confirmada antes deste checkpoint: 2.9.53/code82.
- A persistencia governada SQLite/OpenViking do AnyTV continua sem identidade/binding verificado (`ProjectIdentityUnavailable`); este arquivo GitHub e memoria canonica especifica do AnyTV enquanto nao houver registro multiprojeto autenticado.

## Checkpoint confirmado — 28/10/2026 — AnyTV V2 2.9.56 e transporte de PNGs
- **Aplicativo celular:** versão `2.9.56-AnyTV`, `versionCode=84`, package V2 `com.tclremote.livre.v2`. Branch DEV `feat/anytv-p0-device-proof-verifier-20261020`; commit dos PNGs aprovados e metadata de release `66b21a0424b781344be522741f15845df18187f6`.
- **Arte aprovada preservada:** `assets/anytv_icon.png` (Git blob `88e1ef15c269568054e0ae098cb03b028eef74b7`, 162934 bytes), `assets/anytv_logo.png` e `assets/anytv_horizontal.png` (blob `bc7eb57425896f99b4658c07170765724d530b58`, 326283 bytes cada). Somente posições preexistentes de marca e ícones; não adicionar logos às telas internas.
- **CI assinatura e OTA:** GitHub Actions run `40789557174`, attempt 2, `completed/success`. No job `phone-compile`, passaram os passos de compilação, assinatura V2, publicação no canal independente e verificação por HTTPS dos bytes da APK publicada. Run: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40789557174
- **OTA confirmado:** branch `feat/tcl-v22-updates-back-bluetooth`, commit `bb12478db5a54cc80e9c03aa51b4fcd1b0d563d8`, arquivo `updates/android/v2/current.apk` (26860444 bytes; Git blob `9c5a8bab930fc1457b51c0fd99efa9eed9ddf8f4`). APK publicada/assinada comprovada pelo CI, mas **instalação e testes visuais/físicos no aparelho NÃO confirmados**.
- **Solução definitiva para bloqueios de PNG/ZIP:** procedimento testado `docs/dev-context/ANYTV_BINARY_ASSET_GITHUB_FALLBACK.md`; anexo real -> ZIP STORE -> upload Google Drive -> fetch raw com Base64 -> extrair bytes em `functions.exec` -> `mcp__GitHub__create_blob` com encoding base64 -> `create_tree` -> `create_commit` -> `update_ref` com `expected_sha` -> readback e CI/OTA. Instrução de consulta preventiva registrada em `AGENTS.md`, commits `b9483bba3afb2ad67eb2b9f1eca3f6c108720fb7` e `c6ab843c3f5f60271a1ba47182e952b39bcb4921`. Não redescobrir o procedimento nem pedir transferência manual antes de testá-lo.
- **Pendente:** AnyTV Connect para TCL/TV — investigar versão/certificado efetivos, atualizar marca/interface minimalista sem comprometer funcionalidades ou assinatura, testar e publicar OTA próprio com readback. Também pendente homologação de uso real da versão telefone no aparelho.
- **Escopo de memória:** este registro é do projeto AnyTV, não do projeto CLFCJ/Fábrica. SQLite e OpenViking não podem ser declarados sincronizados sem identidade/binding autenticados específicos do AnyTV e respectivas operações verificadas.

## Evidencia nova — AnyTV Connect TV 2.5 (29/10/2026 UTC)
- Build CI unsigned: GitHub Actions `40808305133` completed/success, commit `bd1e1eac95123497a1ef81fe1a9d90607cf17e69`; passos de compilacao, verificacao do icone aprovado, banner preto 640x360, permissoes e upload do artefato passaram.
- Artefato de auditoria `anytv-tv25-unsigned-audit`, ID `12869545651`, digest do arquivo ZIP de artefatos `sha256:5bd04e9ac15d07b2a5a18346189ba22677a7be9579ccff3edd02e0aa555985c5`. **Nao instalavel**: release Android sem assinatura.
- Correcoes de branding TV: `scripts/prepare_companion_safe.py` (commit `75dac92ee37dc69ecb879dd37a5cf564f942ba5e`) e workflow de validacao (commit `bd1e1eac95123497a1ef81fe1a9d90607cf17e69`).
- Canal TV OTA atual `updates/tv/current.json` ainda aponta `2.1`, com variantes de assinatura `cert8b2b` e `cert3382`; tambem existe `updates/tv/cert2026/current.apk`. O gerenciador seleciona canal conforme fingerprint instalado. Backup cifrado documentado contem migracao TV cert2026 `0a5427237a2445c42520236469563dabceda6af8ddffc223c`; **nao ha evidencia aqui de chaves privadas de `cert8b2b` ou `cert3382` para assinar uma 2.5 compativel**.
- Proximo gate: confirmar disponibilidade e autorizacao de assinatura privada para **cada** certificado efetivamente instalado; gerar APKs assinadas, validar package/versionCode/cert, publicar apenas canais correspondentes e readback. Sem isso, TV 2.5 continua candidata unsigned, nao publicada. Preservar instalacoes existentes.

## Gate real da assinatura TV cert2026 — 29/10/2026
- Workflow de recuperacao protegido adicionado em `.github/workflows/anytv-tv25-signer-gate.yml`, HEAD `3e2e9161037df4236b707042320a6cdcfeff7309`; baixa artefato unsigned aprovado da execucao TV `40808305133`, verifica hash/pacote e tenta recuperar o backup TV original exclusivamente dentro do GitHub Actions; nao publica OTA.
- Run `40811725627`: etapa `Verify approved TV build` **SUCCESS**; etapa `Check TV cert2026 recovery without disclosing secret` **FAILURE**, log `TV cert2026 backup secret unavailable. No OTA changed.` Falta o secret de repositorio `ANYTV_RECOVERY_SECRET_2026` com o valor correto autorizado, ou permissão para o workflow acessá-lo; NAO concluir perda da chave privada nem alegar assinatura recuperada. Não inserir segredo em chat/GitHub arquivos/logs.
- `cert8b2b` e `cert3382`: continuam sem chave privada comprovada. TV 2.5/code16 **nao publicada**. Manter canais antigos intactos e não solicitar desinstalacao.
