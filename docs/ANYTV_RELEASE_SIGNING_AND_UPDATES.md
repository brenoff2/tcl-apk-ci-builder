# AnyTV — releases, assinaturas e atualizações

## Situação atual verificada — 15/10/2026 (sem alteração de canais)

**Fontes de verdade, verificadas:** `overrides/` e scripts no repositório são o código-fonte; `updates/android/current.json`, `updates/android/current.apk`, `updates/tv/current.json`, `updates/tv/cert8b2b/current.apk` e `updates/tv/cert3382/current.apk` no GitHub são **os canais do botão Buscar atualizações** (ver `overrides/common/TclUpdateManager.java`). A pasta [Google Drive / AnyTV - Releases](https://drive.google.com/drive/folders/1JFVKw2bBxqqVNTQj8PwkT3DeEWz111kf) contém o histórico e cópias assinadas, **não é atualmente o endpoint consultado pelo aplicativo**. Pastas de assinaturas privadas e públicas no Drive são separadas e não devem ser publicadas.

**Publicado no updater GitHub:** AnyTV Android **2.9.4 (versionCode 33)** e AnyTV Connect **2.1** com duas variantes de certificado (8b2b e 3382). Releases assinadas destas versões existem no GitHub. O Drive possui as respectivas cópias versionadas em `AnyTV - Releases/android/` e `AnyTV - Releases/tv/cert8b2b/` e `cert3382/`, mas os `current.apk` listados no Drive têm datas anteriores às cópias versionadas; **não considerá-los atualizados sem conferir hash/assinatura**.

**Próxima atualização em preparação:** compilação CI das fontes **Android 2.9.5 (versionCode 34)** e **TV 2.2 (versionCode 13)** aprovada ([build](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38791483823)), entregando **APKs não assinados**. O fluxo [publicar 2.9.5 + 2.2](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38794562320) terminou em estado de espera `STAGING_WAITING_FOR_SIGNED_ZIP_NO_OTA_CHANGE`: não recebeu o ZIP com os três APKs assinados e, portanto, **NÃO publicou a OTA 2.9.5/2.2**. Um workflow verde com esse estado **não significa publicação**. Não tentar instalar APKs de CI não assinados sobre os já instalados. Não publicar os ZIPs de chaves privadas nem senhas.

**Próxima sequência obrigatória, sem reiniciar etapas já concluídas:** obter em ambiente privado as três assinaturas originais; assinar os binários correspondentes aos builds auditados; verificar package, `versionCode`, certificado, alinhamento e SHA-256; enviar **somente APKs assinados** ao mecanismo de staging previsto; executar/verificar o fluxo de publicação OTA; confirmar por readback `current.json`, `current.apk` e GitHub Releases de Android e das duas variantes de TV; sincronizar **cópias correspondentes e identificadas** ao Drive Releases (não tocar nas pastas de chaves); testar atualização nos dois dispositivos. O fluxo de OTA não deve ser reexecutado se já houver publicação igual ou posterior.

**Decisão:** os dois canais coexistem, mas com funções diferentes. **GitHub = código, build, releases e updater em uso. Google Drive = arquivo de APKs assinados e assinaturas privadas/públicas separadas.** Manter o desenvolvimento exclusivamente no branch `feat/tcl-v22-updates-back-bluetooth`; não editar `main` nem sobrescrever arquivos `current` sem prova de assinatura e hash.

---

## Android 2.7 — correção de protocolo de digitação (15/10/2026 UTC)

- [Investigação e implementação](https://github.com/brenoff2/tcl-apk-ci-builder/commit/e673f75acceb941cf4ae2e85156c29941fee62dc): baseada no [PR #25 do tronikos/androidtvremote2](https://github.com/tronikos/androidtvremote2/pull/25), no [código oficial send_text](https://github.com/tronikos/androidtvremote2/blob/main/src/androidtvremote2/remote.py) e nas [limitações documentadas no issue #32](https://github.com/tronikos/androidtvremote2/issues/32).
- O editor passa a transmitir **o texto completo como um único RemoteImeBatchEdit**, não diferenças por caractere, que são substituídas em alguns apps. A UI não afirma que o texto apareceu na TV: apenas que o comando foi enviado à conexão.
- O código de transporte obtém `ime_counter` e `field_counter` enviados pelo serviço remoto; antes de recebê-los usa 0/0 como a referência oficial, **não contadores inventados**. Também reseta os contadores quando a conexão é finalizada. O cursor IME usa `max(0, len(texto)-1)`.
- O envio deve ocorrer quando um campo de pesquisa/texto está focado na TV. Algumas interfaces não aceitam edição enquanto o teclado virtual da TV está visível; nem todo aplicativo segue a mesma política de adicionar/substituir texto. Nenhum teste de integração real em uma TV TCL foi executado.
- Aplicativo `com.tclremote.livre`, `versionName=2.7-AnyTV`, `versionCode=27`; certificado SHA256 original `df72977485e8e5243048a7f1e8dff9fedcf2295161c4daae3834b6a16cd4e4c8`.
- APK [AnyTV 2.7](https://github.com/brenoff2/tcl-apk-ci-builder/releases/download/tcl-phone-v2.7/Controle_TCL_Livre_v2.7.apk), SHA-256 `66f76ba5f486c9d91d954c75b7d27358399e7a18e775ef9e3546e07286d888cc`.
- `updates/android/current.apk` atualizado para o binário acima, histórico preservado em `updates/android/AnyTV-v2.7.apk`; cópias assinadas no Drive `AnyTV - Releases/android/`. Publicação e comparação de hash confirmadas no [workflow](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38662701238).
- **Próxima validação necessária:** abrir um campo de busca na TV, mantê-lo selecionado, enviar "Teste AnyTV 27" pelo editor e observar a inserção. Repetir com o teclado virtual da TV oculto, se houver falha.

---


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
---

## Auditoria adicional IME 2.7 — 14/10/2026 (horário de Salvador)

- **Estado preservado:** Android 2.7 e TV 1.6 continuam os APKs distribuídos. Nenhuma nova versão foi publicada por causa desta auditoria; a homologação física ainda é obrigatória.
- **Comparação de implementação:** `scripts/apply_tcl_v21.py` substitui a dupla injeção por **um** `RemoteImeBatchEdit`, utiliza contadores provenientes de `RemoteSession`, inicia em 0/0 até a TV informá-los e define o cursor de acordo com `tronikos/androidtvremote2`. O PR upstream #25 está merged. O issue #32 registra comportamentos diferentes de foco, teclado virtual e substituição/anexação por aplicativo.
- **Ampliação de CI:** `overrides/tests/RemoteImeWireTest.java` é copiado para a árvore gerada na compilação; executa três testes JVM de frames binários, contemplando texto ASCII, UTF-8/contadores diferentes de zero e framing Protobuf de mensagens longas. Rodar `testDebugUnitTest` em GitHub Actions **não** homologa a transmissão na TCL.
- **Ponto de atenção de UX, sem alterar o release 2.7:** o editor exibe "Comando transmitido" após chamar um callback Unit, embora a função ainda possa retornar por falta de conexão ou falha na fila/socket. Em uma próxima correção, distinguir solicitação, gravação no socket e confirmação visual na TV.
- **Homologação necessária:** no telefone instalado com 2.7, manter o campo de pesquisa da TCL selecionado; digitar `Teste AnyTV 27`, pressionar **Enviar** e observar o conteúdo na TV. Repetir com o teclado virtual oculto apenas se o campo conservar o foco. Registrar aplicativo/versão da TV, resultado com teclado visível/oculto, sintomas de conexão e, se falhar, logs temporários de `RemoteSession`, `ImeBatchEdit`, `RemoteInput` e `Keyboard`, ocultando valores sensíveis do texto digitado.
- **Critério de aceite:** a CI pode verificar a codificação, mas sucesso end-to-end depende de inserção real, ausência de duplicação/perda e validação em mais de um campo de texto. Não publicar uma correção especulativa de contadores.

