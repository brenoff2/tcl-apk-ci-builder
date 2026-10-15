# AnyTV — releases, assinaturas e atualizações

## Android 2.6 — correções reais de interface e texto (15/10/2026 UTC)

- Código fonte: commit `fa9e15db78c589dde421e5caa26a51ecb155df94` (mais correções de CI), exclusivamente no branch `feat/tcl-v22-updates-back-bluetooth`. O branch `main` não foi alterado.
- Pacote `com.tclremote.livre`, `versionName=2.6-AnyTV`, `versionCode=26`. Certificado original `df72977485e8e5243048a7f1e8dff9fedcf2295161c4daae3834b6a16cd4e4c8`.
- APK assinado: [Controle_TCL_Livre_v2.6.apk](https://github.com/brenoff2/tcl-apk-ci-builder/releases/download/tcl-phone-v2.6/Controle_TCL_Livre_v2.6.apk), SHA-256 `a3c8ec9dfad9b4ada80d81c49fdcda32f146b0b9a7720204d4048a70a4ce529e`.
- Canal atual: `updates/android/current.apk` e histórico `updates/android/AnyTV-v2.6.apk`. A cópia `current.apk` e o histórico estão em `Google Drive/AnyTV - Releases/android/`.
- Tela de abertura `SplashScreen.kt`: logo/ícone oficiais AnyTV e versão lida do APK, sem o texto herdado "Android TV Remote". Removida a imagem do cabeçalho do controle e o indicador de versão fora da abertura.
- `TclResponsivePad.kt`: diâmetro do botão OK reduzido de `0.38 * size` para `0.19 * size`.
- `apply_tcl_sticky_controls.py`: conjunto Volume + D-pad + Canal centralizado em `BoxWithConstraints` com `Modifier.weight(1f)`; espaço superior e inferior dividido igualmente e barra de ações mantida no rodapé.
- Editor `TclTextInputDialog`: digitação ao vivo desligada por padrão; botão **Enviar** aciona `onSendText(draft)` no modo normal, sem fechar silenciosamente a janela. Requer campo de texto selecionado na TV e homologação física.
- A compilação e assinatura foram verificadas. A atualização e o texto enviado em aparelho real **não foram homologados neste ambiente**.
- **Pendente fora deste escopo:** estudo e revisão dos controles contextuais/visualização de aplicativos de streaming.

---


## Estado validado em 14/10/2026

O GitHub Actions [38588338021](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38588338021) concluiu com sucesso a verificação de pacotes, versionCode, assinatura, alinhamento e igualdade binária com os assets do GitHub Releases.

| Aplicativo | Pacote | Versão | versionCode | Assinatura |
|---|---|---|---|---|
| AnyTV Android | `com.tclremote.livre` | 2.5-AnyTV | 25 | `df72977485e8e5243048a7f1e8dff9fedcf2295161c4daae3834b6a16cd4e4c8` |
| AnyTV Connect TV — instalação do usuário | `com.tclremote.tvcompanion` | 1.6-AnyTV | 7 | `8b2b0c6f2a7a668f9aae829be811241dc70c3697af2b3e6c71e1a1767a5d83da` |
| AnyTV Connect TV — instalações alternativas | `com.tclremote.tvcompanion` | 1.6-AnyTV | 7 | `3382dfb12a0f16eb9167e056f947ea9740448fe07b8ef9d0ace0c889bf591add` |

**Não confundir:** a TV 1.6 possui APKs distintos para os certificados 8b2b e 3382. Não misturar as assinaturas. Ambas as versões 1.6 já estão publicadas.

Ambos os apps atualizados exibem a versão discretamente na própria interface; os nomes do lançador continuam **AnyTV** e **AnyTV Connect**. Ícone e logomarca aprovados estão nos projetos de build de ambas as versões.

## Arquivos permanentes no GitHub

- Celular: `updates/android/current.apk`, histórico `updates/android/AnyTV-v2.5.apk`.
- TV 8b2b: `updates/tv/cert8b2b/current.apk`, histórico `updates/tv/cert8b2b/AnyTV-Connect-v1.6.apk`.
- TV 3382: `updates/tv/cert3382/current.apk`, histórico `updates/tv/cert3382/AnyTV-Connect-v1.6.apk`.

Releases oficiais para clientes e atualizadores antigos:
- [tcl-phone-v2.5](https://github.com/brenoff2/tcl-apk-ci-builder/releases/download/tcl-phone-v2.5/Controle_TCL_Livre_v2.5.apk), SHA-256 `98a8628e7df3210a9a33ac5533630099c3a0c16d179655e548c2ed58e691c9da`.
- [tv-complemento-v1.6 (8b2b)](https://github.com/brenoff2/tcl-apk-ci-builder/releases/download/tv-complemento-v1.6/Controle_TCL_TV_v1.6_cert8b2b.apk), SHA-256 `fbb216082f23278cdccbe0625416376b69143a080010c097d4601cd9322fe86b`.

- [TV 1.6 variante 3382](https://github.com/brenoff2/tcl-apk-ci-builder/releases/download/tv-complemento-v1.6/Controle_TCL_TV_v1.6_cert3382.apk): SHA-256 `62dc534c243059fc5ede6ffa2576700fb69dc852e686ffc49a92258e562d9378`.

## Botão Buscar atualizações

Implementação: `overrides/common/TclUpdateManager.java`. Ela seleciona o canal binário `current.apk` pelo pacote e, no caso da TV, pelo certificado instalado; baixa o APK; recusa package/certificado incompatível e `versionCode` não superior; em seguida, abre o PackageInstaller do Android. **A instalação ainda precisa da confirmação de segurança do usuário no dispositivo**, e um teste físico no celular/TV não ocorreu neste ambiente.

Os arquivos atuais também foram preservados no Google Drive na pasta **AnyTV - Releases**, incluindo versões históricas. Não mover APKs assinados de uma variante de certificado para a outra.

## Segurança e processo para próximas atualizações

1. Incrementar os códigos/nomes de versão apenas nos aplicativos modificados; não acrescentar versões ao nome do lançador.
2. Aplicar os recursos aprovados de marca e preservar callbacks/funcionalidades; rodar testes e builds Android e TV.
3. Assinar em ambiente privado com a chave original de cada pacote/certificado; **não publicar senhas, keystores ou backups descriptografados no GitHub ou nos logs**.
4. Verificar `aapt dump badging`, `apksigner verify --verbose --print-certs`, SHA-256 e `zipalign` antes de qualquer mudança em `current.apk`.
5. Publicar histórico e assets do GitHub Releases com os nomes esperados; trocar cada `current.apk` somente pelo mesmo APK auditado.
6. Comparar hash de origem, release e `current.apk` após a publicação. O workflow `.github/workflows/publish-anytv-25-16.yml` foi convertido em **verificação somente leitura** para que um rerun nunca rebaixe a versão publicada.
7. Guardar backups privados no Drive em **AnyTV - Assinaturas Privadas - Nao Compartilhar**. Os certificados públicos têm backup separado. Não inserir chaves privadas no repositório.
8. Afirmar instalação/atualização completa no dispositivo apenas após homologação real; CI e acesso ao download não substituem esse teste.
