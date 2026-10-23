> **ATENÇÃO — A SEÇÃO CERT2026 ABAIXO É HISTÓRICA PARA O APLICATIVO LEGADO** `com.tclremote.livre`. Para o **AnyTV V2** instalado (`com.tclremote.livre.v2`), o procedimento oficial, certificado e Secrets estão descritos em [RUNBOOK V2](operations/ANYTV_V2_SIGNER_RECOVERY_AND_RELEASE_RUNBOOK.md) e [Handoff canônico vigente](handovers/2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md). NÃO usar o arquivo de Drive rejeitado, não recriar identidade, não assinar V2 com cert2026.

# AnyTV — assinatura Android cert2026 e acesso GitHub (procedimento operacional único)

## CORREÇÃO CRÍTICA DE PROVENIÊNCIA — 23/10/2026 (prevalece sobre instruções anteriores)

**Não reutilizar nem recomendar `ANYTV_RECOVERY_SECRET_2026_PRIVADO.txt` do Google Drive.** O proprietário informou expressamente que essa é uma chave ANTIGA e não deve ser tratada como credencial atual. A checagem anterior de "64 caracteres hexadecimais" era apenas validação sintática, NÃO prova de descriptografia, correspondência de certificado ou validade para a assinatura recuperada na 2.9.10. Referências anteriores a esse arquivo como "a chave" são REVOGADAS.

**Evidência recuperada no GitHub:** o commit [`4541b31eb219052ca0317335fec54b0c6b227437`](https://github.com/brenoff2/tcl-apk-ci-builder/commit/4541b31eb219052ca0317335fec54b0c6b227437) alterou o workflow da 2.9.10 para recuperar `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin` (original cert2026) em vez de exigir um keystore novo. A 2.9.10 (code39, hash assinado `e21d63f516e7d6af300e10d117ef089ab08b13afbdad0814f25cdd478cdb6965`) foi assinada; 2.9.11, 2.9.12 e 2.9.13 também foram registradas como assinadas com a **mesma identidade cert2026**, fingerprint `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`.

**São coisas diferentes:** (1) chave privada de assinatura do APK, dentro do PKCS#12 protegido; (2) chave simétrica de recuperação AES-GCM que abre o BACKUP criptografado; (3) Repository Secret do GitHub Actions por meio do qual o runner recebe a chave de recuperação. Assinatura já comprovada com o mesmo certificado **não significa** que (2) tenha sido preservada em local durável ou que (3) já exista.

**Estado de preservação comprovado:** no GitHub foi efetivamente persistido o **BACKUP CRIPTOGRAFADO** original, em `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin`. A auditoria do commit e da árvore atual NÃO encontrou registro comprovando o armazenamento permanente da credencial DESCRIPTOGRAFADORA correta da sessão 2.9.10. Não declarar que ela foi guardada em GitHub Actions nem que foi perdida definitivamente; classificá-la como **RECUPERAÇÃO NÃO LOCALIZADA/ORIGEM NÃO HOMOLOGADA** até prova material em ambiente privado. Nunca tentar extrair chave privada de um APK assinado; um APK contém certificado público e assinatura, não a chave privada.

**Próxima busca obrigatória antes de qualquer provisionamento:** rastrear os artefatos/caminhos da assinatura REAL da 2.9.10 e da 2.9.13, histórico privado de execução e qualquer cofre de segredos autorizado, sem exibir, transportar em parâmetros de ferramentas ou guardar credenciais em texto claro. Só aceitar como válida uma credencial que **realmente abra o backup e reproduza o certificado cert2026**, em ambiente privado com comprovante não sensível. Apenas então configurar `ANYTV_RECOVERY_SECRET_2026` com a credencial comprovada e retomar o **mesmo publicador único**; NUNCA criar nova identidade ou pedir o arquivo antigo. Se origem não puder ser comprovada, declarar precisamente essa pendência.

**Publicação:** pedido `.github/anytv-phone-release-request.json` JÁ EXISTE (commit `1530c887aa394168f004ce942d7996d019c71f76`), mas o run `39988081281` FAIL recebeu `RECOVERY_HEX` vazio. Sem outro envio automático até credencial e aprovação de segurança efetivamente comprovadas. OTA segue 2.9.12/code41; candidata 2.9.13/code42 foi assinada em sessão anterior mas arquivo final não foi recuperado nesta sessão. Não apresentar release como disponível.


**Vigência:** 23/10/2026. **Autoridade:** este arquivo define o procedimento OPERACIONAL atual para assinatura, acesso MCP e diagnóstico de credenciais. Logs, handoffs e textos de versões anteriores são históricos e não substituem verificações ao vivo. O código real e o estado do GitHub Actions prevalecem.

## Identidade e distribuição — invariantes

- Pacote do TELEFONE: `com.tclremote.livre`.
- Certificado existente e obrigatório, SHA-256: `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`.
- Backup ORIGINAL cifrado no repositório: `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin` (AES-GCM, cabeçalho `ANYTVAES1`). O backup **não** contém o segredo de descriptografia em texto claro.
- ÚNICO nome do Repository Secret GitHub Actions: `ANYTV_RECOVERY_SECRET_2026`. O segredo, seu valor e a senha do P12 jamais entram em Git, chat, logs, argumentos de tools, URLs ou workflow inputs.
- ÚNICO publicador do TELEFONE: `.github/workflows/publish-anytv-phone-github-only.yml`, branch `feat/tcl-v22-updates-back-bluetooth`.
- ÚNICO destino OTA telefone: `updates/android/cert2026/current.apk` na branch OTA. A verificação é `.github/workflows/verify-anytv-update-endpoint.yml`.
- Companion TV `com.tclremote.tvcompanion` possui certificados/canais próprios; NÃO alterar seus workflows para resolver assinatura do telefone.
- Não trocar ou regenerar a identidade, não desinstalar o aplicativo para contornar erro, não criar publicadores por versão, Drive OTA, hosts/URLs de sessão ou bypass TLS.

## Duas conexões GitHub MCP — ordem obrigatória ANTES de declarar bloqueio

1. **MCP direto GitHub**, instalado na conversa (ferramentas `mcp__GitHub__*`): consultar repositório, HEAD, árvore, workflows, runs, commits e fazer as operações autorizadas.
2. **MCP Business OS Hub v2 → provider GitHub oficial**: chamar `clfcj_v2_health` e `clfcj_v2_capabilities`; depois `clfcj_v2_upstream_tools(provider="github")` para obter catálogo REAL e `clfcj_v2_upstream_call(provider="github",tool=...,arguments=...)` para executar ferramenta publicada. A implementação federada pode expor funções não presentes no catálogo MCP direto.
3. Se a primeira rota não tiver ferramenta necessária, verificar a segunda **ANTES** de concluir que o GitHub não está acessível. O terminal Windows/ausência de `gh` local NÃO constitui bloqueio dessas duas conexões.
4. **Não confundir** `admin:true` no repositório, `github.write_token:READY` e uma ferramenta de alteração de arquivos com a permissão `Secrets: write` ou a operação efetiva para criar Repository Secrets. Confirmar o esquema da ferramenta e a autorização específicas, sem inventar ações.
5. **Prova observada em 23/10/2026:** MCP direto autenticado com permissões administrativas de repositório; Hub `READY`, GitHub oficial `READY`, `github.write_token:READY`, usuário `brenoff2`, 99 ferramentas no catálogo. **Nenhum dos dois catálogos examinados expunha método de criar/atualizar GitHub Actions Repository Secrets.** Isso é uma lacuna de capacidade EXPOSTA, não ausência de acesso ao GitHub, não perda de chave e não prova de ausência atual do secret.

## Provisionamento administrativo da credencial — único destino

**Primeiro verificar em tempo real o Repository Secret `ANYTV_RECOVERY_SECRET_2026` por operação administrativa autorizada.** Uma falha antiga de CI comprova apenas o estado daquela execução, não a configuração atual.

Quando realmente necessário, a operação suportada pelo GitHub é `GET /repos/{owner}/{repo}/actions/secrets/public-key` e `PUT /repos/{owner}/{repo}/actions/secrets/{secret_name}`: cifrar o valor **com LibSodium sealed box usando a chave pública retornada**, e enviar somente `encrypted_value` e `key_id`. Exigir permissão `Secrets: write` no repositório; `Contents: write` NÃO basta. Referência oficial: https://docs.github.com/en/rest/actions/secrets e https://docs.github.com/en/rest/guides/encrypting-secrets-for-the-rest-api .

**Segurança da execução:** realizar a operação apenas em ambiente administrativo privado confiável com origem do segredo e credencial GitHub acessíveis server-side, SEM devolver o segredo ao ChatGPT, interpolá-lo em comandos enviados pela conversa ou registrá-lo em tool calls, patches, arquivos, logs ou outputs. Verificar apenas metadados de existência/`updated_at` e resultado HTTP. Não replicar segredos da CLFCJ em documentação pública. Se nenhuma ferramenta privada autorizada conseguir executar, classificar como `BLOCKED_ACTIONS_SECRETS_WRITE_CAPABILITY` e reportar o limite exato; não pedir instalação de `gh` no Windows, não declarar `GITHUB_ACCESS_BLOCKED` e não improvisar transferência.

A fonte da credencial FUNCIONAL recuperada para a 2.9.10 não foi homologada nesta sessão. O arquivo antigo de recuperação encontrado no Google Drive NÃO é a fonte autorizada conforme correção crítica abaixo; formato hexadecimal não prova compatibilidade. Nunca encaminhar seu valor.

## Acionamento exclusivo da publicação

O publicador em branch OTA é acionado SOMENTE pelo `push` de um pedido revisado `.github/anytv-phone-release-request.json`, com `schema:1`, `intent:"publish-cert2026-phone"`, `candidate_run_id` (string com ID de CI aprovado) e `expected_version_code` (inteiro). O YAML não tem `workflow_dispatch`; a branch padrão é `main`, sem esse workflow. Editar YAML/documentação NÃO publica. O arquivo de pedido foi criado no commit `1530c887` após solicitação de entrega. A execução `39988081281` falhou no gate do secret; não reexecutar cegamente.

No runner, o segredo de recuperação é injetado SOMENTE na etapa de assinatura. O workflow confere CI do candidato, artefato e SHA de entrada, recupera o P12 original, verifica assinatura v2/cert, pacote e versão crescente, publica na OTA e exige readback remoto idêntico. Falha de credencial/candidato/certificado/versionCode interrompe antes da publicação; falha de readback após commit exige investigação imediata, não declarar sucesso.

**Versões comprovadas na última checagem de 23/10/2026:** OTA 2.9.12/code41, SHA-256 `28c2881899bc9c6a79fcfd908d66528750534c89c7f334b8cc57096f80cf63f3`; candidato 2.9.13/code42: build `39891891049` SUCCESS, artefato `12475073264` (`anytv-rsa-tls-2913-unsigned-audit`), validade até 30/10/2026 02:56:40Z. Verificador do publicador/OTA `39973828725` SUCCESS. A última falha de publicação com segredo ausente foi `39963496459`, que é evidência HISTÓRICA — o acesso atual ao secret NÃO foi comprovado. 2.9.13 NÃO publicada, TLS da TCL NÃO homologado.

## Evidência antes de encerrar

1. Confirmar metadata do Repository Secret sem mostrar valor, e capacidade efetiva de o runner descriptografar o backup original.
2. Somente após aprovação de release, executar um único pedido de publicação e obter run `SUCCESS`, versionCode superior, assinatura original, SHA e readback OTA.
3. Validar que o botão **Buscar atualizações** oferece a atualização no celular e que a TCL real consegue parear/controlar; CI não é homologação física.
4. Atualizar handoff/status e gates apenas após prova. Se ainda faltar permissão para Secrets, deixar claro `BLOCKED_ACTIONS_SECRETS_WRITE_CAPABILITY`, preservando o restante.

**Precedência e limpeza:** qualquer recomendação anterior de que só existe o MCP direto, de que a ausência do GitHub CLI Windows bloqueia acesso, de que um administrador deve NECESSARIAMENTE fazer a operação manual, de que a chave foi perdida ou de que se deve trocar certificado está revogada. Histórico Git permanece consultável para auditoria. Este é o ÚNICO procedimento operacional; outros documentos devem apontar para ele, não manter instruções alternativas.


## Tentativa de entrega da 2.9.13 — 23/10/2026

- Pedido de release criado na branch OTA: `.github/anytv-phone-release-request.json`, commit `1530c887aa394168f004ce942d7996d019c71f76`, apontando para CI `39891891049` e `expected_version_code: 42`.
- Execução REAL do único publicador: GitHub Actions `39988081281` (FAILURE). Checkout, leitura do pedido, download do artefato de CI, Android SDK e dependência de criptografia passaram; a etapa **Recover original signer and sign without exporting secrets** falhou porque o runner recebeu `RECOVERY_HEX` VAZIO, com erro `Repository Actions secret ANYTV_RECOVERY_SECRET_2026 missing or invalid. OTA unchanged.`
- Etapas de assinatura, validação, publicação e readback de NOVO APK não se concluíram; o publicador não alterou `updates/android/cert2026/current.apk`. OTA permanece `2.9.12/code41`, candidatа `2.9.13/code42` NÃO entregue pela rota OTA. Não fornecer artifact unsigned como APK instalável.
- Estado decisivo atualizado: **BLOCKED_ACTIONS_SIGNER_SECRET_NOT_IN_RUNNER** comprovado ao vivo no run `39988081281`; não confundir com falta de acesso ao repositório (ambos MCPs GitHub seguem disponíveis). A fonte correta da recuperação da 2.9.10 precisa ser identificada e comprovada; não presumir que o arquivo antigo do Drive seja válido. Nunca publicar material privado. Não criar publicador alternativo nem nova identidade.

## Verificação adicional de acesso privado — 23/10/2026 (NÃO RESOLVIDO)

- VERIFICAÇÃO HISTÓRICA NÃO SUFICIENTE: o MCP Hub v2 confirmou acesso OAuth Drive a um arquivo de recuperação ANTIGO, **sem validar criptograficamente correspondência com a recuperação da 2.9.10**. Isso não autoriza usar aquele arquivo. A autenticação GitHub do Hub obteve a chave PÚBLICA do repositório. O acesso geral ao GitHub funciona; não se deve diagnosticar bloqueio do GitHub.
- Consulta REST de metadados `GET /repos/brenoff2/tcl-apk-ci-builder/actions/secrets` retornou somente o secret independente `ANYTV_MOVIE_API_KEY`. O secret oficial do assinante `ANYTV_RECOVERY_SECRET_2026` NÃO existia na consulta. O último publicador `39988081281` confirmou novamente `RECOVERY_HEX` vazio e interrompeu a publicação.
- Foi avaliada a transferência criptografada servidor-servidor, mantendo o segredo fora dos argumentos/chat. A ferramenta de comando que teria realizado essa operação foi BLOQUEADA pelas configurações de segurança da plataforma. NÃO foi provisionado secret, NÃO houve novo APK assinado nem OTA 2.9.13. Não contornar a restrição com shell, arquivos versionados ou transportes não autorizados.
- **Critério de retomada:** usar uma ferramenta explicitamente autorizada de administração de GitHub Repository Secrets para gravar o valor diretamente em canal protegido (ou a interface administrativa GitHub). Confirmar APENAS metadados do secret. Rerodar o mesmo workflow GitHub Actions `39988081281` e exigir SUCCESS + v2/cert2026 + readback de code42 antes de orientar atualização pelo botão do aplicativo. Não solicitar o valor da chave na conversa.
