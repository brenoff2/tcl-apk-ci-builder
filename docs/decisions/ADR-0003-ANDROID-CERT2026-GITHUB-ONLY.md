# ADR-0003 — Publicador único GitHub Actions para OTA Android cert2026
Data: 23/10/2026 | Estado: DECISÃO OPERACIONAL RATIFICADA; AUTOMAÇÃO AINDA BLOQUEADA POR CREDENCIAL ACTIONS

## Contexto e evidências
O AnyTV possui pacote Android com.tclremote.livre e certificado permanente cert2026 SHA256 2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f. A 2.9.8 funcionava na TCL. APKs 2.9.10–2.9.13 foram assinados com a mesma identidade recuperada do backup cifrado do GitHub em sessões privadas. A assinatura bem sucedida em uma sessão NÃO equivale a credencial instalada no GitHub Actions: a execução 39963496459 falhou no gate de secret. Múltiplos publicadores versionados e URLs de sessão criaram rotas inconsistentes, agora aposentadas.

## Decisão vinculante
- Somente .github/workflows/publish-anytv-phone-github-only.yml publica o APK do TELEFONE em updates/android/cert2026/current.apk na branch feat/tcl-v22-updates-back-bluetooth.
- Recebe candidato auditado de CI GitHub, assina dentro do Actions com backup cifrado signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin + único segredo privado GitHub Actions ANYTV_RECOVERY_SECRET_2026. Não há segundo método/rota, URL externa ou upload manual na distribuição normal.
- Verifica sucesso do CI, checksum de entrada, pacote com.tclremote.livre, versionCode maior, identidade cert2026, assinatura v2 e igualdade byte a byte do download OTA.
- Falha segura: segredo ausente, certificado divergente, versão inválida ou checksum divergente bloqueiam a publicação, sem tocar no arquivo remoto. Não armazenar chave, senha ou recuperação em texto público nem coletar pela conversa.
- O botão Buscar atualizações do celular consulta diretamente GitHub; Android pode exigir confirmação de instalação. O canal do companion TV e certificados próprios não são alterados.
- Workflow de verificação .github/workflows/verify-anytv-update-endpoint.yml impede reintroduzir publicadores concorrentes ou endpoint Android antigo. Run 39963760834 SUCCESS.
- Histórico Git das rotas antigas preservado para auditoria; arquivos antigos removidos das duas branches ativas.

## Critérios para declarar CONCLUÍDO
1. GitHub Actions Repository Secret ANYTV_RECOVERY_SECRET_2026 disponível ao runner por método administrativo autorizado, sem exposição do seu valor.
2. Publicador gera APK cert2026 e conclui run SUCCESS, com readback confirmado do canal.
3. Instalador do app oferece versão superior via botão e o dispositivo efetivamente atualiza.
4. Homologação da conexão TLS/controle da TCL é gate funcional SEPARADO; nem CI nem OTA verde provam que funciona.
Status atual: item 1 pendente, portanto 2.9.13 não publicada; OTA permanece 2.9.12.

## Alternativas rejeitadas
Workflows por versão; novas chaves e troca de certificado; Drive, arquivos externos ou URLs temporárias para rotina OTA; commit de material secreto; bypass de controle SSL; publicação de unsigned; presumir QA física a partir de CI; reset/desinstalação como solução.
Referências: docs/handoffs/2026-10-23-anytv-full-transition.md, docs/ASSINATURA_E_ATUALIZACAO.md, signing-recovery/README.md.
