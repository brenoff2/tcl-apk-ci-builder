# AnyTV — inventário verificado do Google Drive (21/10/2026)

## Fonte e política
Inspeção de metadados por Google Drive conectado, **não** auditoria de bytes, assinatura ou conteúdo criptográfico. O GitHub é o canônico de código, decisões, builds, status e fluxo de distribuição; o Drive armazena arquivo histórico de releases e backups segregados. Não sincronizar chaves privadas/senhas para GitHub, Actions, issues ou logs.

| Papel | Pasta | URL |
|---|---|---|
| Releases e históricos | AnyTV - Releases | https://drive.google.com/drive/folders/1JFVKw2bBxqqVNTQj8PwkT3DeEWz111kf |
| Android | android | https://drive.google.com/drive/folders/1aV3eHYi-8mVt6rbqBiwSDF4CdVQIm4Ej |
| Companion TV | tv | https://drive.google.com/drive/folders/1DshMZxNmE2Um8Y0LCoUX5nzDspL7S3Nl |
| Certificados públicos | AnyTV - Assinaturas Publicas | https://drive.google.com/drive/folders/15lfuEVc5XwSjJSn0st9Pnf78JhOms1Sh |
| Material de assinatura privado | AnyTV - Assinaturas Privadas - Nao Compartilhar | https://drive.google.com/drive/folders/1ILkpXiM9lKjfMunsIR_3VTNApl-jnMIc |

## Arquivos observados (metadados)
- **Novo artefato encontrado no Drive:** `AnyTV_2.9.9_Discovery_V3_CERT2026.apk` (28.347.236 bytes), URL https://drive.google.com/file/d/1NadF93dOMSJHkjJlvQWyUPQ2u8hwjqHN/view. **STATUS: NÃO AUDITADO/NÃO DISTRIBUÍDO nesta verificação.** Ainda é obrigatório checar `aapt` (pacote/versionCode), `apksigner` (SHA256 cert2026), zipalign, SHA256, origem do build e identidade do código. Presença no Drive não prova release assinada correta ou publicação OTA.
- Histórico Android: `AnyTV-v2.9.6-ASSINADO-CERT2026.apk`, `AnyTV-v2.9.5-NOVA-ASSINATURA.apk`, arquivos antigos e `current.apk` (o `current.apk` do Drive não é a referência canônica de OTA e pode estar desatualizado).
- Histórico TV: `AnyTV-Connect-v2.4-SIGNED-CERT2026.apk`, `AnyTV-Connect-v2.3-ASSINADO-CERT2026.apk`, versões anteriores e subpastas por certificado.
- Assinaturas públicas: `assinatura_v1.p7b`, `certificado_publico.der`, `certificado_publico.pem`.
- Assinaturas privadas: há backup cifrado de assinatura, arquivo privado de recuperação e ZIP de chaves permanentes. **Não registrar conteúdo, chave de recuperação, senhas, nomes de alias privados ou bytes no GitHub.** A existência de backup não prova a disponibilidade de um fluxo seguro de assinatura.
- Documentação de recuperação já versionada: `signing-recovery/README.md`. Usar procedimento com identidade de certificado verificada, sem impressão de material sensível.

## Destinos e validação antes da distribuição
- OTA do celular: branch `feat/tcl-v22-updates-back-bluetooth`, `updates/android/cert2026/current.apk`.
- OTA da TV: mesma branch, `updates/tv/cert2026/current.apk`.
- Identidade esperada do telefone: `com.tclremote.livre`, certificado SHA-256 `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`.
- Identidade esperada da TV: `com.tclremote.tvcompanion`, certificado SHA-256 `0a5427237a2445c42520236469563dabceda6d43310fe4e6aea6af8ddffc223c`.
- Base publicada previamente confirmada: celular 2.9.8 (37), TV 2.5 (16). Para novo celular, exigir `versionCode > 37`.
- Exigir hash do APK idêntico entre artefato aprovado, endpoint GitHub e cópia arquivada Drive. Não copiar um APK de Drive diretamente para OTA apenas com base no nome.
- Não sobrescrever backups/históricos ou pastas privadas.

## Sincronização segura e pendências
1. Para cada build: registrar commit, run, APK SHA256, `versionCode`, identidade de assinatura, evidência CI, homologação e URL do artefato; depois atualizar o índice GitHub.
2. Após release verificada: gravar APK assinado na OTA e arquivar cópia de bytes idênticos no Drive.
3. Para o arquivo 2.9.9 encontrado: verificar metadados criptográficos reais e vincular a um build. **Nenhuma promoção automática autorizada.**
4. Comparar metadados públicos das pastas Google Drive com manifests GitHub; nunca replicar backups privados.
