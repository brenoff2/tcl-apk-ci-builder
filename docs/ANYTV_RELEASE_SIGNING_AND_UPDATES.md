# AnyTV — publicação, assinaturas e atualização

## Situação verificada em 14/10/2026

- **Celular**: versão **2.4**, applicationId `com.tclremote.livre`, versionCode **24**. APK assinado e publicado, com ícone e logomarca aprovados.
- **Televisão**: versão **1.5**, applicationId `com.tclremote.tvcompanion`, versionCode **6**. Dois APKs assinados e publicados, para certificados **8b2b** (instalação atual confirmada pelo usuário) e **3382** (instalações alternativas).
- Ambos os aplicativos mantêm o botão **Buscar atualizações**; os instalados antes da mudança usam **GitHub Releases** e os novos usam **o arquivo binário current.apk**.
- A confirmação final da instalação em dispositivos reais requer teste no celular e na TV do usuário. Nunca afirmar teste físico que não ocorreu.

## Canais estáveis — APK binário real, não JSON

- Android: `updates/android/current.apk`, histórico `updates/android/AnyTV-v2.4.apk`.
- Android TV (assinatura 8b2b): `updates/tv/cert8b2b/current.apk`, histórico `updates/tv/cert8b2b/AnyTV-Connect-v1.5.apk`.
- Android TV (assinatura 3382): `updates/tv/cert3382/current.apk`, histórico `updates/tv/cert3382/AnyTV-Connect-v1.5.apk`.
- Nunca sobrescrever uma variante de certificado com outra. Os APKs assinados permanecem também no GitHub Releases.

## Lançamentos compatíveis com atualizadores instalados anteriormente

- Telefone: tag `tcl-phone-v2.4`, asset `Controle_TCL_Livre_v2.4.apk`.
- TV: tag `tv-complemento-v1.5`, assets `Controle_TCL_TV_v1.5_cert8b2b.apk` e `Controle_TCL_TV_v1.5_cert3382.apk`.
- As versões antigas selecionam a variante certa através do certificado instalado e exigem versãoCode superior.

## Identidade de assinatura

- Celular (SHA-256 de certificado): `df72977485e8e5243048a7f1e8dff9fedcf2295161c4daae3834b6a16cd4e4c8`.
- TV variante 8b2b: `8b2b0c6f2a7a668f9aae829be811241dc70c3697af2b3e6c71e1a1767a5d83da`.
- TV variante 3382: `3382dfb12a0f16eb9167e056f947ea9740448fe07b8ef9d0ace0c889bf591add`.
- Chaves privadas foram recuperadas e verificadas fora do GitHub. **Não publicar keystores, PKCS12, passwords ou backups descriptografados**.
- Backups públicos dos certificados: [Google Drive](https://drive.google.com/drive/folders/15lfuEVc5XwSjJSn0st9Pnf78JhOms1Sh). Backups privados foram preservados em pasta privada no Drive, sem disponibilizar URLs ou credenciais no repositório.
- A chave de assinatura legada 8b2b foi recuperada localmente de um backup criptografado CMS utilizando a chave privada de recuperação 3382. O backup criptografado permanece no release TV 1.3; nunca disponibilizar o conteúdo descriptografado.

## Procedimento obrigatório por atualização

1. Preservar `applicationId` e todas as funcionalidades existentes; aplicar mudanças de interface com testes de regressão.
2. Incrementar `versionCode` e o número interno da versão do aplicativo modificado. No lançador, exibir **somente AnyTV / AnyTV Connect**, sem versão.
3. Compilar pelo CI; conferir package, versionCode, permissões e recursos reais de branding no APK.
4. Assinar localmente com a chave compatível com o aplicativo instalado. Para a TV, gerar separadamente ambas as assinaturas enquanto existirem instalações das duas linhagens.
5. Conferir `apksigner verify --verbose --print-certs`, SHA-256 do arquivo, assinatura, pacote, versionCode e tamanho. Abortá-la se houver divergência.
6. Publicar o APK **assinado** no GitHub Releases com o nome esperado pelo atualizador das versões existentes.
7. Publicar o mesmo APK binário como `current.apk` no canal fixo, mantendo histórico por versão. Usar operação atômica, conferir SHA-256 e tamanhos após publicar.
8. Validar o mecanismo no aplicativo novo e o caminho do atualizador antigo. Nunca presumir que o link encurtado aponta para a versão mais recente.
9. Guardar as assinaturas e instruções de recuperação em armazenamento privado. Jamais expor chaves privadas em GitHub, logs, links públicos ou arquivos distribuídos.
10. Não declarar uma release concluída sem compilação aprovada e arquivos assinados acessíveis. Testes físicos devem ser descritos como pendentes quando não realizados.

Scripts e integrações: `scripts/apply_anytv_brand.py`, `scripts/prepare_companion_safe.py`, `overrides/common/TclUpdateManager.java`, workflows `tcl-v22-phone-tv-integration.yml`, `publish-anytv-android-current.yml` e `publish-anytv-tv15.yml`.
