> **ARQUIVO HISTÓRICO anterior à V2.** As afirmações abaixo sobre 'OTA atual 2.9.12', proibição de nova identidade e assinatura cert2026 como rota ativa foram superadas pela decisão do usuário e pela V2 instalada. Use [HANDOFF ATUAL](2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md) e [RUNBOOK V2](../operations/ANYTV_V2_SIGNER_RECOVERY_AND_RELEASE_RUNBOOK.md).

# ANYTV — HANDOFF INTEGRAL E CONTINUIDADE — 23/10/2026

> **PRIMEIRA LEITURA OBRIGATÓRIA NO PRÓXIMO CHAT.** Este documento registra decisões e evidências deste ciclo; o estado operacional deve ser confirmado no GitHub antes de qualquer novo commit, build ou publicação. **Não repetir perguntas respondidas, nem inventar finalização.** Horário de referência: 23/10/2026 (America/Bahia).

## 1. Resumo executivo — objetivo e situação real

O usuário quer que o **AnyTV Android** continue controlando sua **TCL pela rede local** como a **2.9.8 comprovadamente fazia**, além de fornecer atualização de APK pelo botão **Buscar atualizações**, com **GitHub como única rota de compilação, assinatura, publicação e OTA**, SEM Google Drive, links provisórios ou reinstalação. Quer continuidade durável entre chats, sem regressões de assinatura e sem criar uma rota diferente a cada versão.

**Situação confirmada ao final deste chat:**
- A **2.9.8** era funcional no controle da TCL segundo teste físico do usuário.
- A **2.9.10** foi compilada/assinada e entregue, mas apresentou erro de reconexão SSL.
- A **2.9.11** tentou forçar novo PIN ao ocorrer erro SSL: piorou o comportamento; o app passou de **Paired/Reconnect** para **New/Conectar**; código removeu preferências HOST/PIN. O usuário observou `Read error: ssl=..., I/O error during system call, Success`.
- A **2.9.12** removeu o patch 2.9.11 e restaurou o código de conexão da 2.9.8, sem conseguir restaurar HOST/PIN eliminados no dispositivo; a conexão física continuou falhando. **2.9.12 é a última OTA publicada e validada por readback**, versionCode 41.
- A **2.9.13**, versionCode 42, é candidata que restringe TLS da TCL à identidade RSA original `androidtv-local`, evitando competir com a identidade EC/P-256 `anytv-resolver-p256-v1` utilizada para o Cloudflare. Compilação CI 39891891049 **SUCCESS**, APK assinado localmente com cert2026, mas **nenhum teste real TCL aprovado**. **NÃO ESTÁ NA OTA**.
- O canal de atualização do telefone é `updates/android/cert2026/current.apk` na branch `feat/tcl-v22-updates-back-bluetooth`. O código do botão aponta para raw.githubusercontent.com, sem Drive. O arquivo remoto continua 2.9.12 (28.347.236 bytes; git blob SHA `44c766b433ddb370f4589b87843f1edfd9cd7b7a`).
- O **único publicador oficial do telefone** é `.github/workflows/publish-anytv-phone-github-only.yml`. Os publicadores concorrentes do telefone foram eliminados das duas branches operacionais, mantendo histórico Git. Workflows exclusivos da TV não foram substituídos.
- O publicador é **independente de versão**: recebe `candidate_run_id` de CI aprovado, baixa artefato e `SHA256.txt` de GitHub Actions, recupera identidade de assinatura no runner, valida pacote, versão monotônica, certificado, assinatura e readback, publica exclusivamente a OTA do telefone.
- **BLOQUEIO REAL ATUAL:** o workflow recebe vazio o segredo `ANYTV_RECOVERY_SECRET_2026` no GitHub Actions. Runs `39960928074` e `39963496459` falharam em 'Confirm official signer and private Actions secret'. A integração disponível ao chat **não expõe administração de Actions repository secrets**, logo não se confirmou provisionamento do segredo. **Não afirmar que a chave privada original foi perdida**: backup criptografado presente no GitHub, chave de recuperação localizada e assinatura local de várias versões já realizada. É a *disponibilidade durável no runner* que falhou.
- O verificador canônico `.github/workflows/verify-anytv-update-endpoint.yml` foi atualizado para rejeitar publicadores concorrentes, validar cert2026 e readback remoto; run **39963760834 SUCCESS** (três gates aprovados).
- O usuário exige: terminar o trabalho, registrar tudo no GitHub e nunca mais criar fluxos paralelos. **Não declarar que 2.9.13 foi publicada ou que o pareamento com a TCL foi consertado.**

## 2. Repositório, branches, fonte canônica

- Repositório: https://github.com/brenoff2/tcl-apk-ci-builder
- Branch de desenvolvimento: `feat/anytv-p0-device-proof-verifier-20261020`.
- Branch OTA telefone/TV: `feat/tcl-v22-updates-back-bluetooth`.
- Cânone resumido: `docs/STATUS.md`, `README.md`.
- Cânone de assinatura: `docs/ASSINATURA_E_ATUALIZACAO.md` e `signing-recovery/README.md`.
- Handoff integral: **este arquivo**, `docs/handovers/2026-10-23-anytv-full-transition.md`.
- Publicador único: `.github/workflows/publish-anytv-phone-github-only.yml` (branch OTA).
- Verificador independente: `.github/workflows/verify-anytv-update-endpoint.yml` (branch OTA, também sincronizado ao desenvolvimento).
- Aplicativo updater: `overrides/common/TclUpdateManager.java`.
- Código de conexão base: upstream `harimoradiya/androidtvremote` no commit `16c68f0bb9d23f929ee8240f105fdb9000262d85`; código alterado por scripts de `scripts/`.
- Patch 2.9.11 (NÃO REINTRODUZIR sem causa confirmada): `scripts/apply_anytv_ssl_repair.py`.
- Patch 2.9.13 RSA-only (CANDIDATO): `scripts/apply_anytv_tv_tls_alias_isolation.py`; teste `scripts/tests/test_anytv_tv_tls_identity_isolation.py`; CI `.github/workflows/anytv-2913-rsa-tls-candidate.yml`.

## 3. Linha do tempo técnica (decisões, evidências, mudanças)

### 2.9.8 (base física funcional)
- O usuário confirma controle remoto da TCL funcional. Signed cert2026 OTA antiga versionCode 37, antes das atualizações 2.9.9+.
- Run base de build 2.9.8: `39661373378`, head SHA `dc0dd875fe6560455e63d1e58a6dc7476919421b`.
- Esta versão já incorporava o resolvedor Cloudflare; Cloudflare não foi introduzido só na 2.9.11. Scripts principais da 2.9.8 e da 2.9.11 conferidos por blob SHA e coincidentes. Portanto NÃO atribuir automaticamente falha TLS LAN à implantação remota.
- APK 2.9.8 assinado e OTA cert2026 foram usados como referência de compatibilidade.

### 2.9.9
- APK `AnyTV_2.9.9_Discovery_V3_CERT2026.apk` 28.347.236 B, SHA256 `56142e5f3e33d54f1be41c5f702b0d8e1bf00bff206e3c011438a5c2648a2a93` foi assinado, mas publicação complicada por workflow de reconstrução e metadados ausentes no GitHub.
- Tentativas legadas de reconstrução binária `.github/anytv-299-patch/` foram abandonadas. Evitar retomar métodos por patch e URLs efêmeras.

### 2.9.10
- Candidate workflow `.github/workflows/anytv-2910-candidate.yml`; build `39774708705` SUCCESS, artefato unsigned `anytv-discovery-v3-2910-unsigned-audit` ID `12423064871`. Artefato assinado entregue em conversa como `AnyTV_2.9.10_CERT2026_ASSINADO.apk`, SHA256 `e21d63f516e7d6af300e10d117ef089ab08b13afbdad0814f25cdd478cdb6965`, versionCode 39.
- **Fato decisivo:** assinatura original foi RECUPERADA E USADA localmente nessa etapa, mas isso NÃO comprovou cadastro do segredo permanente no GitHub Actions. Não prometer novamente que a assinatura está 'resolvida para sempre' sem um run do publicador autônomo.
- Usuário detectou erro de reconexão `SSL certificate error. Re-pair the device.`. App detectava a TCL, mas falhava.
- No código upstream, `AndroidRemoteTv.reconnect().onSslError` chamava apenas `onError` e não lançava automaticamente pareamento com PIN.

### 2.9.11 — tentativa de correção que agravou regressão
- Patch `scripts/apply_anytv_ssl_repair.py`: ao receber onSslError, redireciona a `onSecretRequested`, remove marcador local HOST/PIN e abre pareamento novo. CI `39788803659` SUCCESS; APK assinado cert2026 SHA256 `5dd764f8b074715ecb59e658562186912382b7937b18c2b481806f46f5886e9b`; versionCode 40.
- OTA via Actions `39797520084` SUCCESS com readback `ANYTV_2911_CERT2026_OTA_PUBLISHED_READBACK_OK`.
- Teste do usuário na TCL: TV aparece `New`, não `Paired`; tentativa de conectar apresenta erro TLS de leitura antes do PIN. **FALHA física comprovada.**
- Não restaurar cegamente HOST/PIN removidos nem apagar AndroidKeyStore; distinguir status de preferências de material criptográfico.

### 2.9.12 — rollback de código, ainda não funcional
- Workflow `.github/workflows/anytv-2912-rollback-298-candidate.yml`; run `39881405936` SUCCESS; artifact `anytv-298-connection-restore-2912-unsigned-audit` ID `12471022257`.
- APK assinado `AnyTV_2.9.12_CERT2026_RESTAURACAO_CONEXAO.apk`, 28.347.236 B, SHA256 `28c2881899bc9c6a79fcfd908d66528750534c89c7f334b8cc57096f80cf63f3`, versionCode 41.
- Publicação OTA pelo próprio aplicativo: run `39882583323` SUCCESS, readback `ANYTV_2912_CERT2026_OTA_PUBLISHED_READBACK_OK`.
- Teste TCL usuário: **ainda falha SSL**. Rollback retira alteração 2.9.11 do código, mas não reconstitui preferências apagadas; não demonstra que o problema original estava só no patch.

### 2.9.13 — candidata com isolamento RSA/Cloudflare
- Código de TLS upstream `KeyStoreManager.getKeyManagers()` expõe todas as aliases do AndroidKeyStore. Existe alias TV RSA `androidtv-local` e alias Cloudflare EC/P256 `anytv-resolver-p256-v1`. Protocolo TV remote usa RSA e TLS local em portas 6466 (controle) e 6467 (pareamento).
- Hipótese: seleção incorreta de identidade durante TLS quando chaves coexistem. Hipótese técnica plausível, **NÃO comprovada em TCL real**.
- Patch `scripts/apply_anytv_tv_tls_alias_isolation.py`: restringe X509KeyManager da TV à RSA, preserva a EC do Cloudflare; sem desabilitar SSL e sem deletar chaves. Regressão `scripts/tests/test_anytv_tv_tls_identity_isolation.py`.
- CI `39891891049` SUCCESS, artifact `anytv-rsa-tls-2913-unsigned-audit` ID `12475073264`, unexpired ao último check.
- APK assinado localmente `AnyTV_2.9.13_CERT2026_RSA_TLS_TESTE.apk`, SHA256 `fbbc1398465f27b2aadccef1d48be6e08e51af0b729aab694189ca8c06b3c1f5`, 28.347.236 B, versionCode 42.
- **OTA 2.9.13 NÃO publicada:** tentativa de upload com URL temporária não autorizada, seguida de decisão de publicar exclusivamente via GitHub Actions. Publicador único esbarra na credencial do runner.

## 4. Assinatura — invariantes e fonte de verdade

- Pacote Android: `com.tclremote.livre`.
- Certificado original de assinatura do APK (SHA256): `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`.
- Backup criptografado versionado (NÃO contém plaintext): `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin` (formato `ANYTVAES1` + nonce AES-GCM + ciphertext).
- O backup contém material original, protegido; sua existência não significa que `secrets.ANYTV_RECOVERY_SECRET_2026` esteja provisionado no Actions.
- Nome ÚNICO canônico de GitHub Actions Repository Secret: `ANYTV_RECOVERY_SECRET_2026`. **Nunca colocar o valor em repositório público, logs, argumentos de execução, issues ou conversas.**
- O procedimento 2.9.10 confirmou recuperação da chave *em contexto local autorizado*, não provisionamento permanente.
- Branch OTA contém `.github/workflows/publish-anytv-phone-github-only.yml`, atualizado no commit `281a37ab2839c6c8c16e5d871b69a684f3f26270`, com correção para remover auditoria invasiva no commit `b46f7b8df771f5d761c7922ae70ae0e3856175a7`. Verificar HEAD se houver mudanças posteriores.
- **Runs com falha de credencial:** `39955533005`, `39960928074`, `39963496459`; passo 'Confirm official signer and private Actions secret'. Isso não comprova que não exista segredo sob outro escopo/nome; comprova que a expressão do segredo canônico entrou vazia no job.
- Ferramentas conectadas GitHub nesta conversa permitem leitura/escrita de arquivos e Actions, mas não oferecem ação de cadastrar GitHub Actions secrets. Tentativa de endpoint REST secret via `mcp__GitHub__fetch` retornou "GitHub Fetch URL is not an allowed public GitHub repository or search endpoint".
- **Gate de conclusão:** um run do workflow oficial com recuperação da identidade, assinatura cert2026, versão crescente, publicação e readback completo SUCCESS. Até lá o release automatizado NÃO está validado.

## 5. Limpeza e unificação — realizada no GitHub

- Removidos publicadores antigos de telefone nas duas branches em commits sucessivos: `publish-anytv-28`, `29`, `291`, `292`, `295-22-signed-bundle`, `br-prime-hotfix`, `cert2026-baseline`, `cert2026-update-297`, `multistream`, `publish-anytv-25-16`; na OTA também `cert2026-update-298`, `299`, `2910`. Os de URL temporária `2911-once`, `2912-once` e shortlink antigo foram excluídos. Preservado o histórico Git e workflows exclusivos TV.
- Na branch OTA somente `publish-anytv-phone-github-only.yml` escreve o caminho `updates/android/cert2026/current.apk`. O verificador `verify-anytv-update-endpoint.yml` referencia o mesmo caminho apenas para ler/comparar, não publica.
- Verificador tem gate que acusa duplicação de escritores e endpoint antigo `updates/android/current.apk`; run `39963760834` SUCCESS para exclusividade, certificado e readback.
- Documentação consolidada nas duas branches: `README.md`, `docs/ASSINATURA_E_ATUALIZACAO.md`, `signing-recovery/README.md`; referências operacionais ao Drive eliminadas. Arquivos binários/patches legados `.github/anytv-291-parts/`, `.github/anytv-296-patch/`, `.github/anytv-297-patch/`, `.github/anytv-298-patch/`, `.github/anytv-299-patch/` ainda aparecem na árvore Git histórica/atual; **não removidos automaticamente**, pois deletar sem auditoria de dependências e sem autorização adequada pode causar perda. Não são rota oficial; investigar apenas se for necessário.
- Cloudflare continua recurso funcional separado (resolver de links/metadados), não sistema de atualização nem assinador de APK.

## 6. Fases / gates operacionais

**F7 / Android — estado: parcialmente concluído, NÃO fechado.**
1. CI e geração dos candidatos Android: **APROVADO** para 2.9.13.
2. Assinatura local cert2026: **APROVADO** para APK candidato 2.9.13.
3. Publicador único e limpeza de workflows conflitantes: **IMPLEMENTADO** e verificado por CI.
4. Disponibilidade durável da recuperação no GitHub Actions: **BLOQUEADO** pelo segredo que não chega ao runner.
5. OTA Android 2.9.13 publicada e readback: **NÃO**. OTA pública 2.9.12.
6. Teste físico TCL 2.9.13 e recuperação SSL/PIN/controle: **NÃO HOMOLOGADO**.
7. Canal OTA TV/AnyTV Connect: **PRESERVADO, NÃO ALTERAR**.
8. Atualização pelo botão do app para 2.9.13: **NÃO DISPONÍVEL** até gate de publicação.

## 7. Próximas ações EXATAS no novo chat

1. Ler integralmente este handoff, `docs/STATUS.md`, política de assinatura e README atual no GitHub. Não pedir contexto já documentado.
2. Confirmar a branch OTA HEAD e que o CI gate `39963760834` permanece aprovado.
3. Resolver de forma autorizada a disponibilidade de `ANYTV_RECOVERY_SECRET_2026` no runner. Investigar ambiente/escopo/permissão/repositório de GitHub Actions. **Não criar rota alternativa, não vazar chave, não inventar segredo, não usar URL temporária nem Drive**. Se a integração não oferece administração de secrets, declarar somente essa ação administrativa pendente e orientar o ponto exato, sem afirmar que foi executada.
4. Executar o **mesmo publicador único** com `candidate_run_id=39891891049`. Verificar o conteúdo real do artifact e `SHA256.txt` antes da assinatura. Verificar digest e certificado original, pacote e versionCode 42; exigir publicação/readback remoto.
5. Só então orientar teste pelo botão do AnyTV com dados preservados, e testar conexão/TLS/PIN na TCL; se falhar, colher erros de etapas do protocolo em vez de criar mais variantes por tentativa.
6. Registrar readback, SHA256, run ID, QA física real, versão OTA e novo status no cânone; encerrar F7 somente com critérios atendidos.

## 8. Proibições / decisões do usuário

- Usar GitHub Actions e GitHub OTA, **SEM Drive** no processo oficial.
- Uma única rota de assinatura/release; sem workflows de publicação por versão e sem soluções que dependam de sessões provisórias.
- Não substituir cert2026, não desinstalar app, não apagar dados, não remover chaves do AndroidKeyStore, não desativar SSL para forçar conexão.
- Não voltar a culpar a TV ou pedir repetidos power cycles quando regressão foi introduzida por software.
- CI/assinatura não substituem teste físico; não declarar "resolvido" sem TCL funcional.
- Não repetir diagnóstico de partida ou pedir novamente arquivos, IDs e decisões já verificados.
- Não prometer tarefas assíncronas nem saída futura; agir com ferramentas nesta conversa e relatar limites reais.

## 9. Links diretos

- Repository: https://github.com/brenoff2/tcl-apk-ci-builder
- Candidate CI 2.9.13: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/39891891049
- Publicador versão genérica, segredo faltante: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/39963496459
- Gate OTA único aprovado: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/39963760834
- Última OTA 2.9.12 readback: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/39882583323
- Policy: https://github.com/brenoff2/tcl-apk-ci-builder/blob/feat/tcl-v22-updates-back-bluetooth/docs/ASSINATURA_E_ATUALIZACAO.md
- GitHub Actions secrets: https://github.com/brenoff2/tcl-apk-ci-builder/settings/secrets/actions

## 10. O que NÃO foi concluído

**Não confundir "transição escrita e publicada" com "app corrigido":** ainda NÃO há confirmação de assinatura autônoma cert2026 no runner, NÃO houve publicação de 2.9.13, NÃO se provou correção TLS na TCL. Estes três gates são a dívida técnica prioritária.