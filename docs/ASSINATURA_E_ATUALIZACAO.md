# AnyTV — assinatura e atualizacao (procedimento unico)

> Esta e a politica atual. A documentacao dos metodos antigos (TCL 1.7/1.8, backups de certificado anterior, scripts de assinatura avulsos e publicadores por versao) permanece no historico Git, nao como procedimento operacional.

## Uma unica identidade Android

- Aplicativo: `com.tclremote.livre`.
- Canal de atualizacao: `updates/android/cert2026/current.apk`, branch `feat/tcl-v22-updates-back-bluetooth`.
- Certificado permanente do telefone (SHA-256): `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`.
- Backup criptografado da identidade: `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin`.
- **Unico nome autorizado para a credencial no GitHub Actions**: `ANYTV_RECOVERY_SECRET_2026`. Valor sigiloso, protegido em Actions Repository Secrets, nunca em arquivo versionado. A existencia do backup cifrado NAO implica que o runner ja esteja habilitado a descriptografa-lo.

## Um unico publicador

`.github/workflows/publish-anytv-phone-github-only.yml`.

O publicador recebe um `candidate_run_id` de CI aprovado, baixa o APK nao assinado e o respectivo checksum do GitHub Actions, recupera temporariamente a identidade original no runner, assina com `apksigner`, verifica pacote, `versionCode` monotono, certificado cert2026 e integridade; publica apenas o APK Android validado e faz readback byte por byte.

Falha de credencial, compilacao, versao ou integridade **interrompe a publicacao sem alterar a OTA**. Variantes da TV possuem certificado e canal proprios, e nao fazem parte do publicador do telefone.

## Comprovacao objetiva — 2026-10-23

- 2.9.10: a assinatura original foi recuperada com sucesso e o APK assinado produzido, mas isso **nao configurou automaticamente** a credencial de recuperacao no GitHub Actions.
- 2.9.13: APK compilado pelo GitHub Actions run `39891891049` e assinado localmente com o mesmo certificado; teste na TCL nao homologado.
- GitHub-only canonical publisher commit `281a37ab2839c6c8c16e5d871b69a684f3f26270`.
- Execucao `39960928074`: falhou no gate de segredo **ANTES** da assinatura; por isso OTA segue em 2.9.12. Nao significa perda da chave ou necessidade de outro certificado.
- Para liberar publicacao totalmente automatizada pelo GitHub, um administrador deve gravar a credencial existente sob o nome exato `ANYTV_RECOVERY_SECRET_2026` nas configuracoes de Actions do repositorio. O valor da chave nao pode ser colocado no Git, na resposta de chat ou em logs.

## Nao repetir

Nao criar workflows de publicacao por versao, rotas de arquivos externos, metodos de assinatura alternativos ou novas identidades para contornar a ausencia da configuracao do secret. Os procedimentos anteriores foram substituidos por este e podem ser consultados somente no historico Git.
