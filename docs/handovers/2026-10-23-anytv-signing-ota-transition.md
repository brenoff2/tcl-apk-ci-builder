> **ATENÇÃO — DOCUMENTO HISTÓRICO:** Esta página registra decisões e estado antigo da linha `cert2026` e pode afirmar incorretamente que a OTA "atual" é 2.9.12 ou que não se pode criar nova identidade. O usuário **aprovou e instalou a nova identidade V2** posteriormente. O estado atual está em [Handoff 2.9.17/2.9.18](2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md) e no [Runbook da assinatura V2](../operations/ANYTV_V2_SIGNER_RECOVERY_AND_RELEASE_RUNBOOK.md). A próxima release usa o signer V2 EXISTENTE e o botão atualizar; NÃO recuperar chave antiga do Drive.

# ANYTV — TRANSICAO CANONICA 2026-10-23

## Objetivo e contexto
Entregar AnyTV Android para controlar TV TCL com descoberta, pareamento/conexao e atualizacao OTA pelo proprio aplicativo. Evitar regressao do pareamento funcional da versao 2.9.8. Centralizar codigo, CI, assinatura e OTA no GitHub, sem Google Drive e sem URL temporaria; preservar assinatura original para atualizacao in-place. Usuario exige continuidade entre chats, execucao verificavel e nenhuma recriacao de procedimentos ja resolvidos.

## Historico de decisoes e regressao
- 2.9.8: referencia funcional de conexao com a TCL, anterior a mudancas de Cloudflare e descoberta. A atualizacao posterior introduziu regressao observada no dispositivo.
- 2.9.9: Discovery V3, APK assinado produzido, mas a publicacao OTA foi bloqueada por workflows que tentavam transportar arquivos por rotas temporarias.
- 2.9.10: rota de atualizacao simplificada sem Drive; APK assinado com identidade cert2026, demonstrando recuperacao local da assinatura. Essa assinatura local NAO equivale a credencial permanente configurada no GitHub Actions.
- 2.9.12: ultima versao confirmada no canal OTA do telefone, versionCode 41, mesmo depois dos novos workflows.
- 2.9.13: candidato versionCode 42, com ajuste RSA/TLS para investigar conexao TCL. Build GitHub Actions 39891891049 SUCCESS, artifact anytv-rsa-tls-2913-unsigned-audit, assinado localmente cert2026, SHA256 do APK assinado fbbc1398465f27b2aadccef1d48be6e08e51af0b729aab694189ca8c06b3c1f5. NAO foi publicado na OTA; NAO foi homologado em TV fisica.
- Cloudflare: adotado como parte de mudancas no mecanismo de atualizacao, mas nao deve ser dependencia de entrega de APK do telefone. Nao confundir o problema de atualizacao com o problema de pareamento TCL. Falha de TLS na TV ainda exige teste fisico comparativo 2.9.8 vs 2.9.13.

## Decisao canonica — assinatura e publicacao
Repositorio privado: brenoff2/tcl-apk-ci-builder.
Branch de OTA: feat/tcl-v22-updates-back-bluetooth.
Branch de desenvolvimento: feat/anytv-p0-device-proof-verifier-20261020.
APK do canal: updates/android/cert2026/current.apk.
Pacote: com.tclremote.livre.
Certificado SHA256: 2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f.
Backup criptografado versionado: signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin.
Unico secret de GitHub Actions aceito: ANYTV_RECOVERY_SECRET_2026, nunca publicar seu valor.
Unico publicador de telefone: .github/workflows/publish-anytv-phone-github-only.yml.
Verificador independente: .github/workflows/verify-anytv-update-endpoint.yml.
Politica escrita: docs/ASSINATURA_E_ATUALIZACAO.md e signing-recovery/README.md.

O fluxo aprovado: obter artifact unsigned + SHA256 do GitHub Actions, recuperar signer original apenas dentro do runner, assinar com apksigner, validar pacote/versionCode crescente/certificado/hash, gravar current.apk e fazer readback byte a byte. Nao criar publicadores por versao, rotas por Drive/Cloudflare, chaves novas, ou exposicao de material privado. Preservar workflows exclusivos da TV e historico Git.

## Evidencia de execucao
- GitHub Actions 39960928074 e 39963496459: publicador BLOQUEADO antes da assinatura porque segredo ANYTV_RECOVERY_SECRET_2026 nao chegou ao runner.
- GitHub Actions 39963760834: verificador SUCCESS, confirmou exclusividade da rota Android, assinatura cert2026 do APK OTA atual e readback remoto.
- Publicadores legados do telefone foram removidos das duas branches ativas; permanecem no historico Git. Workflows exclusivos da TV preservados.
- Verificacao do conteudo de updates/android/cert2026/current.apk ainda aponta ao APK publicado anteriormente, nao ao candidato 2.9.13.
- A integracao GitHub disponivel nesta conversa nao oferece gestao de Actions Secrets. Nao alegar provisionamento sem prova.

## O que falta, na ordem
1. Confirmar em GitHub Settings > Secrets and variables > Actions > Repository secrets que ANYTV_RECOVERY_SECRET_2026 existe e esta disponivel a workflow na branch OTA; nao pedir ou expor valor em chat, logs ou arquivo.
2. Reexecutar apenas publicador canonico com candidate_run_id=39891891049. Se falhar, diagnosticar pelo step exato; nao criar outro publicador.
3. Exigir readback de current.apk, certificado cert2026, versionCode 42 e hash remoto igual ao artifact assinado antes de declarar 2.9.13 publicada.
4. No celular, usar Buscar atualizacoes, validar instalacao in-place e funcionamento do atualizador sem Drive.
5. Testar TCL real: descoberta, autorizacao, pareamento e controle. Se 2.9.13 falhar, comparar logs com 2.9.8; nao assumir que RSA/TLS corrigiu sem teste.
6. Atualizar docs/STATUS.md e este handoff com hashes, runs, resultados de telefone/TV, sem alterar historico de evidencias.

## Protocolo de retomada
Na primeira resposta do novo chat, informar objetivo, versao publicada, versao candidata, causa exata do bloqueio, caminhos canonicos e proximo passo. Consultar GitHub antes de qualquer alteracao. Nao reiniciar pesquisa nem pedir chaves que ja deveriam estar nos segredos. Nao declarar conclusao sem arquivo OTA publicado e teste fisico quando aplicavel. Se Secrets estiverem inacessiveis, declarar limitacao pontual e pedir apenas acao administrativa indispensavel.
