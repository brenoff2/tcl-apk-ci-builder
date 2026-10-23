# PROJETO ANYTV — TRANSICAO INTEGRAL E ESTADO CANONICO
Data: 2026-10-23
Repositorio privado: brenoff2/tcl-apk-ci-builder
Branch OTA telefone: feat/tcl-v22-updates-back-bluetooth
Branch desenvolvimento: feat/anytv-p0-device-proof-verifier-20261020

## Resumo para abrir o proximo chat
O objetivo e entregar AnyTV Android que conecte com TCL e se atualize pelo proprio app sem Drive e sem reinstalacao manual. O usuario exige continuidade entre chats, execucao real, nenhuma nova rota de assinatura por versao e nenhum retrocesso para metodos obsoletos. A identidade Android permanente e cert2026 (SHA-256 2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f). Canal OTA: updates/android/cert2026/current.apk. O publicador canonico e .github/workflows/publish-anytv-phone-github-only.yml. A verificacao do canal e da exclusividade passou no Actions run 39963760834. O candidato 2.9.13 (versionCode 42) foi compilado no run 39891891049, artefato anytv-rsa-tls-2913-unsigned-audit, mas a publicacao automatica ainda falha no gate do GitHub Actions Secret ANYTV_RECOVERY_SECRET_2026 (run 39963496459). OTA continua 2.9.12 (versionCode 41). Nao afirmar que 2.9.13 foi publicada ou que conexao TCL foi corrigida sem prova.

## Objetivos e decisoes definitivas
1. Aplicativo Android pacote com.tclremote.livre, controle TCL funcional, conexao e descoberta confiaveis.
2. Atualizacao iniciada pelo botao do proprio app, via GitHub HTTPS, sem Google Drive, sem URLs temporarias, sem transferencias manuais de APK.
3. Preservar a mesma assinatura cert2026 em todas as versoes de atualizacao. Nao gerar chave nova, nao trocar certificado, nao publicar APK com assinatura divergente.
4. Fonte unica de assinatura: backup cifrado signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin; segredo de descriptografia SOMENTE em GitHub Actions Repository Secret ANYTV_RECOVERY_SECRET_2026. Nunca gravar chave privada ou senha em codigo, logs ou documentos.
5. Publicador unico e independente de versao: .github/workflows/publish-anytv-phone-github-only.yml. Recebe candidate_run_id de CI aprovado, baixa APK unsigned e SHA256.txt do GitHub Actions, assina no runner, valida pacote, certificado, versionCode monotono, publica current.apk e confere readback byte a byte.
6. TV tem canais e assinatura proprios. Nao alterar canais TV para resolver publicacao do telefone.
7. Nao aceitar explicacao, compilacao, commit ou workflow verde como substituto de APK publicado e testado no telefone.
8. Preservar historico Git, mas nao manter workflows obsoletos concorrentes como rotas operacionais.

## Linha do tempo e problemas
- 2.9.8: versao anteriormente funcional na conexao TCL; base comparativa solicitada pelo usuario.
- 2.9.9 Discovery V3: APK assinado foi produzido, mas o fluxo de publicacao OTA ficou inconsistente; tentativa de reconstruir artefato por patch e dependencias de arquivo externo.
- 2.9.10: assinatura original foi recuperada e APK assinado produzido. Ponto essencial: isso NAO comprovou provisionamento permanente do segredo no runner GitHub Actions. Usuario havia exigido que a assinatura ficasse disponivel no GitHub sem repetir recuperacoes.
- 2.9.11/2.9.12: migracao do atualizador para GitHub/Cloudflare e relatos de falha de conexao TCL. Nao atribuir causalidade apenas ao Cloudflare sem diff e prova. Canal atualmente publicado 2.9.12, versionCode 41.
- 2.9.13: candidato com ajuste TLS/RSA para TCL, versionCode 42. Build GitHub Actions 39891891049 aprovado. APK assinado localmente com cert2026, SHA256 fbbc1398465f27b2aadccef1d48be6e08e51af0b729aab694189ca8c06b3c1f5. Pareamento TCL nao homologado em aparelho.
- Workflow inicial GitHub-only 39955533005 falhou no segredo de assinatura vazio.
- Publicador generico substituiu fluxo especifico de versao; run 39960928074 falhou no gate de segredo.
- Run 39963496459 voltou a falhar no mesmo gate. A falha e acesso do runner ao secret, nao prova de perda da chave.
- Verificador GitHub-only 39963760834 PASS: um unico publicador Android cert2026, certificado do APK atualmente publicado correto, readback remoto identico.
- Workflows antigos de publicacao do telefone foram excluidos das branches OTA e desenvolvimento; workflows exclusivos TV preservados. Historico anterior permanece em commits Git.

## Canonico e documentos
- docs/ASSINATURA_E_ATUALIZACAO.md: politica unica de assinatura e publicacao.
- signing-recovery/README.md: backup criptografado e procedimento seguro.
- README.md: rota operacional e estado da OTA.
- .github/workflows/publish-anytv-phone-github-only.yml: unico publicador Android telefone.
- .github/workflows/verify-anytv-update-endpoint.yml: verifica certificado, OTA e ausencia de publicadores concorrentes.
- docs/STATUS.md na branch desenvolvimento: registro historico; nao presumir que todos os ultimos eventos estao nele.
- Este arquivo: docs/handovers/2026-10-23-anytv-transicao-integral.md na branch OTA.

## Estado verificavel
Concluido: build 2.9.13; assinatura cert2026 do candidato em ambiente local; publicador generico version-independent gravado; limpeza dos workflows antigos; documentacao sincronizada; auditoria OTA atual aprovada no run 39963760834.
Pendente: GitHub Actions Secret ANYTV_RECOVERY_SECRET_2026 acessivel ao runner; nova execucao do publicador com candidate_run_id 39891891049; confirmar que o artefato unsigned inclui SHA256.txt e que a verificacao de integridade do publicador corresponde ao arquivo do artifact; assinatura no runner; push e readback do current.apk 2.9.13; teste real do botao Buscar atualizacoes no telefone; teste real de conexao e pareamento TCL; verificacao comparativa com 2.9.8 se regressao persistir.
Nao concluir publicacao se faltar qualquer um desses gates. Nao expor segredo em Git.

## Primeiras acoes do proximo chat
1. Ler este handoff e os arquivos canonicos listados, nao recomecar investigacao do zero.
2. Conferir configuracao GitHub Actions Secret ANYTV_RECOVERY_SECRET_2026 por interface/autorizacao apropriada. A integracao atual nao expoe escrita de Actions Secrets. Solicitar acao pontual apenas se realmente impossivel executar; nunca pedir que o valor seja enviado no chat.
3. Validar o artifact run 39891891049 e SHA256.txt, testar workflow e corrigir qualquer falha concreta sem criar nova rota.
4. Publicar e confirmar SHA, assinatura, versionCode e arquivo OTA atual.
5. Homologar atualizacao in-app e conexao TCL no aparelho.
6. Atualizar este handoff e docs/STATUS.md com evidencias e links de runs, sem declarar sucesso prematuro.

## Regra de continuidade
Nao repetir justificativas. Executar, verificar, registrar. Diferenciar rigorosamente compilado, assinado, publicado, instalado e homologado. Nao substituir a assinatura cert2026, nao reintroduzir Drive, nao criar rotas paralelas.
