# AnyTV — regras obrigatórias de assinatura e publicação

## Arquivo oficial dos certificados públicos

Google Drive (arquivado e verificado em 2026-10-14):
https://drive.google.com/drive/folders/15lfuEVc5XwSjJSn0st9Pnf78JhOms1Sh

Arquivos:
- `certificado_publico.pem`
- `certificado_publico.der`
- `assinatura_v1.p7b`

Pacote telefone: `com.tclremote.livre`.
SHA-256 do certificado da versão 2.2 instalada:
`DF72977485E8E5243048A7F1E8DFF9FEDCF2295161C4DAAE3834B6A16CD4E4C8`.

**ATENÇÃO: arquivos públicos NÃO permitem assinar APK.** Recuperar com segurança a chave privada original (`.jks` ou PKCS12 e suas credenciais) é condição obrigatória para atualizar sem desinstalar. Não registrar segredo/chave no GitHub, Drive público, logs ou artefatos CI.

## Regra permanente para cada entrega

1. Preservar `applicationId` do celular `com.tclremote.livre` e da TV `com.tclremote.tvcompanion`.
2. Incrementar `versionCode` e `versionName` do app modificado; atualizar contratos e validações coerentemente.
3. Construir **dois APKs** por release integrado, celular e complemento Android TV, sem permissão de Acessibilidade na TV.
4. Assinar o APK do celular com a **mesma chave privada** da instalação 2.2. Para TV, manter variantes compatíveis com certificados `3382` e `8b2b` quando necessárias.
5. Executar `apksigner verify`, conferir package, versionCode, SHA-256 do arquivo e certificado; validar compatibilidade com versões instaladas. **Falhar fechado** se chave indisponível.
6. Publicar APKs *assinados e verificados* como assets reais de GitHub Releases. Nunca publicar um APK não assinado como atualização.
7. O botão **Buscar atualizações** dos dois aplicativos usa `overrides/common/TclUpdateManager.java`, busca releases no GitHub e exige o nome exato do asset. Para telefone: `Controle_TCL_Livre_v<versao>.apk`; TV: `Controle_TCL_TV_v<versao>_cert3382.apk` e `Controle_TCL_TV_v<versao>_cert8b2b.apk`. Manter esses nomes, ou mudar atualizador e ativos **no mesmo release**.
8. Validar consulta real à GitHub API e instalação via PackageInstaller com confirmação explícita do Android; somente declarar conclusão quando o release aponta para APK assinado baixável e o botão o identifica.
9. Atualizar links diretos e links curtos da TV **somente depois de verificar redirecionamento**. Não repetir links curtos não comprovados.
10. Não alterar ícone, marca, interface ou recursos durante uma tentativa de reparo de assinatura/publicação.

Status em 2026-10-14: existem certificados públicos no Drive, mas **não foi localizada a chave privada de assinatura original do celular**. Atualização assinada compatível **bloqueada** até recuperar essa chave.

## Canais permanentes atuais

- Celular: `updates/android/current.json` (atualmente `available:false`, pois o release 2.3 ainda não tem APK assinado compatível).
- TV: `updates/tv/current.json` (aponta para os dois APKs **assinados e publicados** do release `tv-complemento-v1.4`).
- Os botões dos dois aplicativos consultam esses manifestos através de `overrides/common/TclUpdateManager.java` a partir do **próximo build instalado**.
- A versão instalada anteriormente continua usando seu próprio atualizador antigo. Um manifesto novo no GitHub não altera retroativamente APKs já instalados.
- **Publicação atômica:** primeiro criar GitHub Release + subir APK(s) assinados, validar nome, tamanho, SHA-256, pacote, versionCode e signer, e **somente depois** trocar `current.json`. Nunca marcar `available:true` para artefato ausente.
- Conservar versões antigas nos GitHub Releases. Não reutilizar um APK chamado `current.apk`: o manifesto estável preserva o histórico e distingue certificados.
