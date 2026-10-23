# AnyTV — assinatura Android cert2026 e acesso GitHub (procedimento operacional único)

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

A fonte privada de recuperação já foi identificada e seu formato (uma string hexadecimal de 64 caracteres) verificado, **sem provar ainda descriptografia bem-sucedida dentro do runner**. O valor não deve ser copiado para a conversa. A fonte privada é para o provisionamento administrativo inicial, **nunca** para consulta ou publicação OTA normal.

## Acionamento exclusivo da publicação

O publicador em branch OTA é acionado SOMENTE pelo `push` de um pedido revisado `.github/anytv-phone-release-request.json`, com `schema:1`, `intent:"publish-cert2026-phone"`, `candidate_run_id` (string com ID de CI aprovado) e `expected_version_code` (inteiro). O YAML não tem `workflow_dispatch`; a branch padrão é `main`, sem esse workflow. Editar YAML/documentação NÃO publica. O arquivo de pedido **não** existe atualmente e não deve ser criado sem aprovação explícita de release.

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
- Estado decisivo atualizado: **BLOCKED_ACTIONS_SIGNER_SECRET_NOT_IN_RUNNER** comprovado ao vivo no run `39988081281`; não confundir com falta de acesso ao repositório (ambos MCPs GitHub seguem disponíveis). A fonte privada de recuperação existe e não deve ser publicada no Git/chat/inputs/argumentos de tools. Não criar publicador alternativo nem nova identidade.
