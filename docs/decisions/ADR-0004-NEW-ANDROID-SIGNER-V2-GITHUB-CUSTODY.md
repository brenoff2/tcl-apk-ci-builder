> **ESTADO VIGENTE após o bootstrap:** identidade V2 foi gerada UMA VEZ, recuperada e usada em releases sucessivas; última OTA 2.9.17/code46 run 40021931010 SUCCESS. Não recriar signer. Guia operativo em [RUNBOOK V2](../operations/ANYTV_V2_SIGNER_RECOVERY_AND_RELEASE_RUNBOOK.md) e [HANDOFF ATUAL](../handovers/2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md). Os textos abaixo sobre bootstrap são histórico.

> **ESTADO CORRENTE após ADR:** identidade V2 já criada, run de assinatura `40021931010` SUCCESS e última OTA 2.9.17/code46. Próxima 2.9.18 não publicada; leia [Runbook V2](../operations/ANYTV_V2_SIGNER_RECOVERY_AND_RELEASE_RUNBOOK.md) e [Handoff](../handovers/2026-10-23-ANYTV-V2-2917-2918-CONTINUIDADE-CANONICA.md). Os trechos abaixo são o histórico de bootstrap, não instrução para refazer.

> **ESTADO MAIS RECENTE — 23/10/2026, REPLACE HISTORICAL STATUS BELOW:** AnyTV V2 `com.tclremote.livre.v2` 2.9.14/code43 PUBLICADO em `updates/android/v2/current.apk`, mesmo signer V2 SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. Workflow `40003530305` SUCCESS, APK SHA256 `7346aa18e50bd0ccb29859d6a90d67d46ee77e51cb3ccbcabef293b3f910908b`, verificação remota `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`. Melhorias de streaming: slides por swipe com avanço 3s sem botões, hero rolável com a página, sinopse rolável, logos de serviços somente nas ofertas Brasil flatrate e seletor de apps limitado à disponibilidade confirmada. Atualização V2 pelo botão Buscar atualizações; sem reinstalação. Canal antigo 2.9.12/cert2026 continua preservado. UI física ainda não homologada pelo usuário após atualização. Código de ativação individual dos links diretos continua sujeito à autorização do servidor, sem chave fixa no APK.

# AnyTV — nova identidade Android V2: custódia e recuperação GitHub

Data: 23/10/2026. **Decisão expressa do proprietário: abandonar a tentativa de recuperação da credencial antiga e criar nova identidade.** Esta decisão **não** autoriza sobrescrever a OTA existente cert2026, trocar a assinatura do companion TV nem afirmar upgrade in-place.

## Por que nova identidade exige migração

Pacote atualmente instalado: `com.tclremote.livre`, certificado original SHA-256 `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`. Novo par de chaves não pode ser aceito como atualização desse pacote existente sem linhagem criptograficamente válida assinada também pela chave antiga. A troca direta causaria `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Fonte primária: https://developer.android.com/tools/apksigner#rotating-keys .

Portanto a nova identidade **nasce em canal separado**, sem substituir `updates/android/cert2026/current.apk`. A etapa de distribuição deve decidir entre **novo applicationId** para instalar em paralelo (preferência para preservar o aplicativo antigo) ou desinstalação/reinstalação com risco de perda de dados — nunca confundir com atualização pelo botão antigo.

## Identidade V2 — geração e custódia

- Workflow exclusivo de criação, NÃO publisher: `.github/workflows/bootstrap-anytv-phone-v2-identity.yml`, acionado por `.github/anytv-signer-v2-bootstrap-request.json` na branch dev.
- Gera RSA 3072 e certificado X.509 autofirmado de 30 anos **dentro do runner do GitHub Actions**.
- Gera PKCS12 com senha derivada de HMAC-SHA256 do segredo de recuperação. Nem chave privada nem senha do PKCS12 são exibidas ou adicionadas ao Git.
- Envelope imutável `ANYTVV2G1` + nonce 12B + ciphertext/tag AES-256-GCM, versionado em `signing-recovery/v2/phone-signer.aesgcm`.
- O segredo (256 bits aleatórios) foi **gerado no ambiente autorizado e cadastrado diretamente** como GitHub Actions Repository Secret `ANYTV_SIGNER_V2_RECOVERY_HEX`. Seu valor não está em commit, arquivo de trabalho, parâmetros do workflow, chat ou logs.
- Certificado público em `signing-recovery/v2/phone-cert.pem` e fingerprint SHA256/manifesto JSON em `signing-recovery/v2/identity.json`.
- A geração falha se qualquer arquivo V2 já existir. Um reinício não troca a identidade sem uma decisão explícita.
- O run testa decrypt AES-GCM, abertura PKCS12, assinatura/verificação de desafio, faz commit só do material cifrado e público e comprova **readback remoto** seguido de nova abertura.
- Restaurar no futuro: job autorizado lê `secrets.ANYTV_SIGNER_V2_RECOVERY_HEX`, obtém o envelope versionado do GitHub, verifica fingerprint SHA256, decripta somente em memória/diretório privado no runner e assina. Se o secret não estiver disponível, falhar **sem recriar identidade nem publicar nada**.

## Limite de segurança e contingência

**GitHub Actions Secrets são write-only para o proprietário:** o valor não pode ser lido de volta pela interface/API após cadastro, embora workflows autorizados possam usá-lo. A custódia automatizada depende de preservar o segredo no GitHub Actions. Não confundir a existência do ciphertext com recuperação sem o secret. Uma recuperação independente adicional (por exemplo cofre pessoal fora do mesmo domínio de falha) exigiria aprovação de local e mecanismo seguro; não guardar o secret na documentação.

## Critérios de aceite

1. Metadata `ANYTV_SIGNER_V2_RECOVERY_HEX` aparece no GitHub Actions Secrets.
2. Bootstrap termina `SUCCESS`, com `ANYTV_V2_SIGNER_GITHUB_ENCRYPTED_READBACK_AND_RECOVERY_PASS`.
3. Os arquivos `signing-recovery/v2/{phone-signer.aesgcm,phone-cert.pem,identity.json}` estão no Git e a fingerprint da prova coincide com JSON e certificado.
4. Publicar apenas depois de compilar/assinar um APK que use o certificado novo, identidade de pacote e update endpoint coerentes, sem tocar canal cert2026; realizar QA no celular antes de declarar pronto.

**Atualização do histórico:** não usar a chave antiga do Drive; ela não é parte da nova identidade. A antiga assinatura fica preservada exclusivamente para os APKs antigos instalados e sua OTA.
