# AnyTV — distribuicao Android cert2026

**Procedimento vigente e exclusivo.** Registros operacionais historicos anteriores a 23/10/2026 permanecem no historico Git; nenhum metodo de publicacao ou assinatura de APK por versao deve ser recuperado sem nova decisao explicita.

## Fonte da verdade

- Repositorio: `brenoff2/tcl-apk-ci-builder`.
- Branch de CI: `feat/anytv-p0-device-proof-verifier-20261020`.
- Branch OTA Android: `feat/tcl-v22-updates-back-bluetooth`.
- Pacote: `com.tclremote.livre`.
- APK consumido pelo botao de atualizacao: `updates/android/cert2026/current.apk` via GitHub HTTPS.
- Certificado permanente do telefone, SHA256: `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`.

## Assinatura: uma identidade e uma credencial

- Backup da identidade original criptografada AES-GCM: `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin`.
- Desbloqueio apenas via GitHub Actions Repository Secret `ANYTV_RECOVERY_SECRET_2026`.
- A chave privada nunca deve ser gravada em commit, registrada em log, incluida em APK, nem transmitida como argumento visivel de workflow.
- A recuperacao e assinatura ocorrem somente no runner do GitHub Actions, com material temporario eliminado apos assinar.
- O backup cifrado estar no repositorio **nao** comprova que o Secret esteja provisionado.

## Unico publicador Android

`.github/workflows/publish-anytv-phone-github-only.yml`

Entrada: `candidate_run_id` do GitHub Actions CI aprovado, contendo exatamente um APK nao assinado e checksum SHA256. O publicador valida origem, recupera a chave original, assina com `apksigner`, valida pacote e certificado, exige `versionCode` superior ao publicado, substitui apenas o APK do telefone e confere o arquivo remoto byte por byte.

**Falha segura:** se qualquer condicao nao for cumprida, nao atualizar `current.apk`. A homologacao de conexao real com a TCL e uma verificacao distinta da assinatura de APK.

## Verificador automatico

`.github/workflows/verify-anytv-update-endpoint.yml`

Verifica exclusivamente o canal cert2026, a assinatura e a igualdade binaria entre GitHub remoto e o checkout. Tambem impede reintroducao de publicadores antigos do telefone. Run `39963760834`: sucesso em 23/10/2026.

## Estado em 23/10/2026

- Publicado para botao do aplicativo: 2.9.12 (`versionCode 41`).
- Candidato 2.9.13 (`versionCode 42`): compilacao `39891891049` aprovada, APK assinado localmente com cert2026 para teste.
- Publicacao automatica 2.9.13: nao concluida. GitHub Actions `39963496459` falhou em verificar o segredo `ANYTV_RECOVERY_SECRET_2026`. A integracao atual nao permite provisionar esse segredo.
- Teste fisico da conexao TLS com a TCL: pendente / falhas anteriores; nao declarar corrigido com base no sucesso do CI.
- Publicadores antigos do telefone, scripts de assinatura avulsa e reconstrucao binaria foram removidos das branches de trabalho; permanecem somente em commits antigos. Rota da TV nao foi alterada.

**Para habilitar uma vez o publicador autonomo:** um administrador autorizado deve verificar a presenca do Secret `ANYTV_RECOVERY_SECRET_2026` em GitHub Settings > Secrets and variables > Actions > Repository secrets e garantir que tenha exatamente o valor da credencial de recuperacao original. Nunca compartilhar o valor em conversa ou commit. Depois executar o publicador com `candidate_run_id=39891891049` e exigir readback SUCCESS.
