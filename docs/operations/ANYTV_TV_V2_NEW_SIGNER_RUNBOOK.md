# AnyTV Connect TV V2 — nova identidade independente

Esta linha e **nova instalacao**, nao atualização criptográfica das assinaturas antigas. APK: `com.tclremote.tvcompanion.v2` (AnyTV Connect V2), assinatura RSA-3072 nova, versão inicial `2.6-AnyTV-TV-V2`/code17, OTA independente `updates/tv/v2/current.apk`. Manter intactos os canais `cert8b2b`, `cert3382` e `cert2026`. A instalacao em paralelo com o companion antigo pode causar conflitos de portas/servico; nao executar os dois ao mesmo tempo. O celular precisa continuar enxergando os endpoints do TV companion: homologar descoberta, apps e fones numa TCL real.

## Bootstrap uma unica vez — antes de gerar a APK
1. No computador confiavel do proprietario, **gerar um segredo aleatorio exclusivo da TV**, em uma sessão local privada de PowerShell, sem copiar o valor no chat:
   ```powershell
   [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLowerInvariant()
   ```
2. Em GitHub > repositorio `brenoff2/tcl-apk-ci-builder` > Settings > Secrets and variables > Actions > **New repository secret**, cadastrar nome exato `ANYTV_TV_V2_RECOVERY_HEX` e valor gerado. Guardar uma cópia privada fora do GitHub Actions em cofre confiável; quem perder o segredo perde a capacidade de abrir o backup cifrado. **Nao colocar o segredo em commit, Issue, arquivo nem log, nem no ChatGPT.**
3. Executar manualmente [bootstrap TV V2](../../.github/workflows/anytv-tv-v2-bootstrap.yml) na branch `feat/anytv-p0-device-proof-verifier-20261020`. O workflow falha se existir outra identidade: ele nao substitui uma chave criada anteriormente.
4. Exigir run `success`, marcador `ANYTV_TV_V2_GITHUB_ENCRYPTED_READBACK_AND_SIGNER_RECOVERY_PASS` e commit com os **três arquivos somente**: `signing-recovery/tv-v2/tv-signer.aesgcm` (backup cifrado), `tv-cert.pem` (publico), `identity.json` (hashes/contrato). O usuário pode baixar/custodiar manualmente cópias cifradas desses arquivos. **Nunca guardar PKCS12 em claro.**
5. Em seguida executar [TV V2 signed release](../../.github/workflows/anytv-tv-v2-release.yml), também manualmente. O pipeline prepara fonte pinned, valida marca e pacote, recupera a assinatura a partir do segredo protegido, assina e verifica `com.tclremote.tvcompanion.v2`, publica somente `updates/tv/v2/current.apk` e confere bytes por commit imutável. Exigir marcador `ANYTV_TV_V2_SIGNED_OTA_BYTE_READBACK_SUCCESS`.
6. A APK assinada está no artefato GitHub Actions `anytv-connect-tv-v2-2-6-signed` do run de release; instalar manualmente na TCL. O canal OTA V2 tem APK separada para atualizações posteriores. Não pedir desinstalação do antigo para corrigir assinatura: o novo pacote tem ID diferente. Evitar usar os dois aplicativos simultaneamente.
7. Homologar na TV física as funções da ponte HTTP, descoberta, apps instalados, inicialização de apps, pareamento/fones e atualização com consentimento. Pipeline verde sozinho não prova funcionamento físico.

**Segredos antigos não servem:** `ANYTV_SIGNER_V2_RECOVERY_HEX` pertence ao signer do celular V2. `ANYTV_SIGNER_V2_ESCROW_HEX` não foi utilizado. `ANYTV_RECOVERY_SECRET_2026` é legado da linha cert2026 e não está disponível no ambiente atual. Não misturar identidades.

**Estado em 29/10/2026:** workflows preparados e versionados; o segredo TV V2 ainda não foi confirmado, portanto não declarar nova assinatura criada nem APK V2 publicada sem execução real.
