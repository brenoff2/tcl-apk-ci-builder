# Controle TCL Livre — bloqueio de atualização Android

## Situação comprovada (13/10/2026)

O identificador instalado `com.tclremote.livre` **não é suficiente**
para uma atualização: a chave de assinatura do novo APK precisa ser
compatível com a chave usada no APK **efetivamente instalado**, e o
`versionCode` precisa avançar.

Certificados SHA-256 obtidos com `apksigner verify --print-certs`
dos APKs preservados:

- 1.2, 1.3, 1.4 e o APK 1.7 final fornecido pelo usuário: `49de7c9ad97bf4cfb2409f5e6480ab21bff816f093f8e7df66a33cce61077a04` (iguais).
- 1.5, 1.6, 1.7 **CI** e 1.8: certificados diferentes desses e entre si
  nos artefatos de `assembleDebug` com runners temporários do GitHub Actions.
- 1.8 teste: `51cfe5c439f05eaa07b4b4ad7135ab550b4fbcb32198e02e9226ccc5b8301e2d`.

**Novo achado: APK 1.7 RESGATE ANEXADO PELO USUARIO.** O arquivo original
`Controle_TCL_Livre_1.7_Resgate.apk` fornecido pelo usuário nesta
conversa tem SHA-256 de arquivo
`61450aa66d42116b9be7c198b95f6807fb7ca94ed5812544f38bb7afd6371820`
e certificado SHA-256
`49de7c9ad97bf4cfb2409f5e6480ab21bff816f093f8e7df66a33cce61077a04`,
**igual ao das versões 1.2–1.4**. Portanto, o APK final 1.7 do
usuário é diferente do 1.7 de depuração baixado do GitHub Actions.
Não tratar a assinatura do artefato CI como se fosse a do APK instalado.

Isso comprova que um processo externo assinou o APK distribuído com
uma chave consistente, mas a **chave privada** não está no repositório
e não foi recuperada da biblioteca de artefatos. Não é possível
derivá-la dos APKs assinados. Para produzir atualização de 1.7,
o certificado final deve permanecer 49DE... e o versionCode deve subir.
O certificado do arquivo anexo ainda precisa ser comparado ao
certificado efetivamente instalado, se a instalação real tiver divergido.

## Política de entrega (bloqueio preventivo)

1. Identificar o `versionCode` e SHA-256 do certificado do APK
   **instalado no telefone do usuário**. Não presumir a versão.
2. Localizar a chave privada original correspondente ao certificado.
   Se estiver perdida, bloquear a publicação como atualização.
3. O script `scripts/sign_update_apk.sh` compara a chave fornecida
   com `TCL_EXPECTED_CERT_SHA256` antes de assinar; recusa divergências.
4. Só publicar atualização quando o resultado tiver **a mesma
   assinatura aceita pelo Android**, a versão for superior e o
   APK for validado no aparelho.
5. **Não publicar** builds de depuração efêmeros como atualizações.
6. Chaves privadas e senhas nunca devem ser enviadas ao repositório
   público, logadas em workflow ou empacotadas com artefatos.
7. **Não desinstalar** o aplicativo atual antes de obter um backup
   independente e validar um plano de restauração de dados; não afirmar
   que backup/restauração funcionam sem testes.

## Quando a chave original estiver disponível

Executar com o Android SDK e Java 17 em ambiente privado, utilizando
variáveis de ambiente, sem incluir segredos nos argumentos ou histórico
de shell. O script aceita keystore por caminho local
(`TCL_SIGNING_KEYSTORE_PATH`) ou em base64 via secret
(`TCL_SIGNING_KEYSTORE_B64`), além de:

- `TCL_SIGNING_STORE_PASS`
- `TCL_SIGNING_KEY_PASS`
- `TCL_SIGNING_KEY_ALIAS`
- `TCL_EXPECTED_CERT_SHA256`
- `TCL_APKSIGNER_BIN` (opcional; padrão do Android SDK 36)

Primeiro: `bash scripts/sign_update_apk.sh --check`.
Depois: `bash scripts/sign_update_apk.sh input.apk output.apk`.

Esse script **não** substitui a validação da identidade do APK instalado
nem a homologação visual/funcional da TCL.

## Se a chave original estiver irrecuperável

Atualização direta sem remoção do pacote é bloqueada pelo modelo de
assinatura do Android. Planejar migração de dados em ambiente confiável
(com backup e restauração testados) e definir **uma única** nova chave
permanente protegida, para futuras atualizações. Não criar APK com
identificador novo como falso substituto de uma atualização.

Referência: https://developer.android.com/studio/publish/app-signing
