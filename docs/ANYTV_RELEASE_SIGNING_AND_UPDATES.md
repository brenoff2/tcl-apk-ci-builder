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



---

## Continuidade AnyTV — 15/10/2026 — reinstalação, streaming e Cloudflare (reconciliação)

### Fato novo informado pelo usuário

- O usuário declarou que **já instalou** os APKs de migração no celular e na TCL após a troca das assinaturas. **Não há aqui verificação física independente** da identidade instalada, pareamento, versão mostrada em ambos ou funcionamento do atalho; registrar como *instalação relatada pelo usuário*, não homologação end-to-end.
- APKs de migração: `AnyTV-v2.9.5-NOVA-ASSINATURA.apk` (celular, Drive file `1jTkhRu-zOdzBjhEIz-odRUdeEw4jovnl`) e `AnyTV-Connect-v2.2-NOVA-ASSINATURA.apk` (TV, Drive file `1ZImSLAc_BfFLj-XKVxrtZAyN6tz1gb3_`). Usam **certificados NOVOS**, incompatíveis com atualização in-place sobre instalações assinadas com as chaves antigas.
- Backup de recuperação cifrado no branch: `signing-recovery/ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin`, SHA Git blob `fe0ad75f981e9940eae88b2bcfbc8c1b17a3d9a9`; documentação `signing-recovery/README.md`. Arquivos de recuperação privados separados no Drive **AnyTV - Assinaturas Privadas - Nao Compartilhar**: `ANYTV_RECOVERY_SECRET_2026_PRIVADO.txt` e `ANYTV_SIGNING_BACKUP_2026_ENCRYPTED.bin`. **Nunca copiar segredo privado, senha ou keystore aberto ao GitHub, APK, CI log ou documentação.** Os canais OTA antigos mantêm os certificados antigos e **ainda NÃO foram migrados para os novos certificados**. A próxima atualização DEVE usar as chaves novas, confirmar suas impressões SHA-256 e criar canal de atualização consistente; não trocar silenciosamente `current.apk` das instalações legadas.
- Identidades novas documentadas em `signing-recovery/README.md`: telefone `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`; TV `0a5427237a2445c42520236469563dabceda6d43310fe4e6aea6af8ddffc223c`. Verificações complementares no dispositivo ainda pendentes.

### Pesquisa já realizada — NÃO REPETIR

- API **Movie of the Night / Streaming Availability**: pesquisa e chamadas reais concluídas em 15/10/2026 para **Netflix, Disney+, Prime Video, Apple TV+ e HBO Max** no Brasil. Evidências: `scripts/audit_movieofthenight.py`, CI `38776166751`, teste de contrato ao vivo `38778069911` (sucesso), e `anytv-resolver/README.md`. Os retornos são **URLs candidatas para a ficha de um título**, não prova de abertura ou reprodução na TCL.
- O repositório já contém `anytv-resolver/src/worker.mjs`, `anytv-resolver/migrations/0001_create_tables.sql`, `anytv-resolver/wrangler.jsonc` e testes. CI offline passou em `38780901151`; `wrangler deploy --dry-run` **não significa implantação real**. Não reimplementar módulo do zero.
- Na API, a credencial `ANYTV_MOVIE_API_KEY` já foi utilizada nos testes GitHub Actions, conforme sucesso do teste ao vivo, mas **não é automaticamente compartilhada com Cloudflare**. Não pedir uma nova chave sem verificar autorização e configuração já existentes. Para o Worker, colocar a MESMA credencial como **Cloudflare Secret**, jamais em `vars`, código, APK ou repositório. Não expor o valor ao usuário ou logs. Confirmar franquia, licença e atribuição antes de uso comercial.
- O catálogo usa TMDB ID; endpoint proposto do Worker `GET /v1/resolve/movie/<TMDB-ID>/<provider>` ou `/tv/`, com `country=br`. O Worker valida domínios e rotas, restringe assinatura/grátis, aplica cache D1 e cota global padrão conservadora (200 chamadas/mês) e devolve `link_candidate` com `verified_on_tv:false`.
- A estratégia de **abrir a ficha** deve usar os IDs/URLs oficiais de cada provedor e intent do pacote correto na TV, com fallback de busca quando o streaming não suportar deep link. Não prometer que qualquer link abre diretamente o filme, sem teste TCL físico. Preservar pareamento, funções do controle e interface aprovadas.

### Cloudflare — separar problema de MCP de trabalho pendente no AnyTV

- O usuário informou que o antigo problema de **conexão MCP ao Cloudflare foi resolvido**. Isso **não prova** que o Worker do AnyTV tenha sido implantado. O canônico do resolvedor marca expressamente `NÃO implantado, não conectado aos APKs`, e `wrangler.jsonc` ainda traz `REPLACE_WITH_D1_DATABASE_UUID` e `workers_dev:false`.
- Próxima ação de engenharia: validar conexão e permissões Cloudflare; criar ambiente **dedicado ao AnyTV**, separado da infraestrutura CLFCJ; criar/bindar D1 e aplicar migrações; definir credenciais via **Secrets** (`ANYTV_MOVIE_API_KEY` e autenticação de cliente apropriada); estabelecer domínio/rota e monitoramento de consumo; implantar o Worker e testar `/health` e resolução real sem revelar credenciais. **Não implantar no Cloudflare/Business OS da CLFCJ** por conveniência.
- O código piloto aceita `ANYTV_CLIENT_TOKEN` estático para testes servidor-servidor, porém documenta que **não pode ser embutido no APK**. Antes de conectar app a endpoint público, implementar autenticação por identidade/dispositivo, token curto/revogável e cotas; validar segurança/custos.
- Só então ligar o app à URL HTTPS autorizada do resolver; preservar fallback para busca no provedor. Homologação física por provedor: Netflix, Disney+, Prime, Apple TV+, Max; conferir que foi aberta a **ficha correta** no aplicativo e não somente a home ou a pesquisa.

### Sequência sem retrabalho (prioridades)

1. Confirmar por readback os hashes/certificados dos novos APKs publicados no Drive e testar pareamento + controles atuais; não refazer builds sem necessidade.
2. Corrigir a trilha permanente de **novas assinaturas** e updater para permitir a próxima versão sem nova reinstalação, mantendo canais legados intactos.
3. Verificar disponibilidade efetiva de Cloudflare e autorização para ambiente próprio AnyTV, secret Movie of the Night, domínio e D1; usar código piloto existente, sem reescrever.
4. Integrar app ↔ Worker ↔ Connect, testes controlados e fallback seguro.
5. Homologar por título/serviço diretamente na TCL, registrar problemas por plataforma, corrigir apenas falhas comprovadas.
6. Atualizar ESTE canônico em cada marco com link para commit, build, APK, hash, status de implantação e resultado físico. Não tratar tarefa planejada como concluída.

**Escopo desta reconciliação:** atualização documental somente; **não** implanta Worker, altera APK, libera distribuição OTA nova nem testa a TCL remotamente.


### Implementação iniciada — canais de atualização com novas assinaturas (15/10/2026)

- Commit `2858e3951df4214f3a072dbb73680af8fbf2d63e`: `overrides/common/TclUpdateManager.java` passou a reconhecer as novas identidades **por SHA-256 de certificado**: telefone `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`, TV `0a5427237a2445c42520236469563dabceda6d43310fe4e6aea6af8ddffc223c`.
- Seleção determinística: telefone antigo → `updates/android/current.apk`; telefone novo → `updates/android/cert2026/current.apk`; TV 3382 → `updates/tv/cert3382/current.apk`; TV 8b2b → `updates/tv/cert8b2b/current.apk`; TV nova → `updates/tv/cert2026/current.apk`. Assinatura desconhecida é bloqueada. Permanece a exigência de pacote e certificado idênticos, `versionCode` maior e confirmação pelo PackageInstaller.
- **ATENÇÃO: implementação de código, ainda NÃO disponível nos APKs já instalados e ainda NÃO publicada como novo binário assinado.** Os dois arquivos `cert2026/current.apk` não foram distribuídos por esse commit, e a publicação da próxima versão deverá criá-los após assinatura nova, verificação de pacote, versão, certificado, hash e instalação. Não preencher canais `cert2026` com os APKs 2.9.5/2.2 já instalados como se fossem atualização (códigos iguais).
- Os canais antigos permanecem intactos. No primeiro novo release, conferir que a compilação realmente inclui esse `TclUpdateManager.java` e homologar o botão Buscar atualizações no celular e na TV. Nunca declarar OTA funcional sem teste end-to-end.

- Commit `49fc28efe5a761d8fad76af14b70cd12ef7b689d`: CI de telefone e TV agora compara byte a byte o `TclUpdateManager.java` copiado para as fontes compiladas com `overrides/common/TclUpdateManager.java` e exige a presença de ambas as novas impressões digitais e seletores `cert2026`. Isso previne builds que ignorem silenciosamente a atualização do gerenciador. **O gate CI não substitui teste de instalação com versão superior assinada**, nem publica as novas rotas.


### Decisão vinculante do usuário — APIs sem configuração no aplicativo (15/10/2026)

- **Não exibir nem exigir inserção manual de nenhuma chave de API no aplicativo AnyTV ou no AnyTV Connect.** O usuário declarou que já forneceu uma credencial ao GitHub e autoriza solicitar novas credenciais somente quando indispensáveis. A interface poderá escolher título/provedor, mas não deverá mostrar campos de chaves, segredos, endpoints de administração nem exigir etapas técnicas do usuário.
- **Não confundir ausência de campo com embutir segredos dentro do APK:** nunca incluir `ANYTV_MOVIE_API_KEY`, credenciais TMDB privilegiadas, `ANYTV_CLIENT_TOKEN` fixo ou senhas em `BuildConfig`, strings Android, código-fonte ou recursos. O app deverá comunicar-se com **URL HTTPS fixa de resolvedor AnyTV** e autenticação de cliente não baseada em segredo compartilhado permanente no APK; o Worker armazena chaves do provedor em Cloudflare Secrets. Uma credencial do GitHub Actions não é transferida automaticamente ao Cloudflare.
- A credencial `ANYTV_MOVIE_API_KEY` foi utilizada com êxito em execuções anteriores de teste ao vivo (evidência: workflow `anytv-resolver-live-contracts.yml`, run `38778069911`), sem revelar seu valor. **Não solicitar repetição da credencial** antes de confirmar configuração secreta adequada no Worker.
- Em 15/10/2026 a conexão Cloudflare MCP foi verificada pelo método `whoami`; a conta autenticada é `Brenoff@gmail.com's Account`. **Isto comprova apenas acesso MCP**, não ambiente AnyTV implantado nem autorização para usar recursos compartilhados com CLFCJ. Exigir isolamento de recursos e confirmação da conta/ambiente adequado antes de provisionamento persistente.
- Próximo contrato de UX: escolher filme → selecionar streaming → consultar automaticamente o resolvedor → enviar URL candidata segura para o Connect → tentar abrir ficha no aplicativo oficial, com fallback de busca. Sem formulários de configuração de API e sem afirmar sucesso até teste físico na TCL.
- Separar fases: (1) canal de atualização seguro com novas assinaturas; (2) implantação do backend AnyTV isolado; (3) autenticação e ligação dos clientes sem segredo embutido; (4) homologação dos deep links nos cinco serviços. Toda mudança de código/APK/Worker deve atualizar este documento com commit, status e teste efetivo.


### Inventário Cloudflare MCP verificado — 15/10/2026

- Leitura via conexão oficial Cloudflare MCP (`whoami`, `GET /accounts/{account_id}/workers/scripts`, `GET /accounts/{account_id}/d1/database`) retornou HTTP 200 em ambas as listagens, conta **Brenoff@gmail.com's Account**.
- Workers existentes: `clfcj-extension-updates`, `clfcj-mcp-gateway`, `clfcj-pjecalc-egress-relay`. **Nenhum Worker `anytv-*` foi encontrado**.
- Bancos D1 na conta consultada: **lista vazia**. O Worker `anytv-link-resolver-pilot` continua sendo código piloto, NÃO deploy realizado. Este inventário vale exclusivamente para a conta MCP consultada, não para outras contas Cloudflare não conectadas.
- Decisão de isolamento: **não modificar Worker CLFCJ nem compartilhar seus bindings/segredos**. Para provisionamento persistente do AnyTV, primeiro confirmar conta/ambiente segregado; depois configurar D1, segredos e autenticação sem chave embutida no APK.
- CI de integração do app no GitHub estava em execução, **não homologada como sucesso** nesta consulta; não publicar OTA nova até sinal verde, assinatura nova verificável e teste físico.


### Execução Cloudflare — D1 real criado e migrado (15/10/2026)

- Cloudflare MCP `execute` criou D1 **`anytv-link-resolver`** na conta autenticada, UUID `9bab6598-d019-4e88-8b29-a327ff0cfd00`, sem replicação de leitura; resposta HTTP 200 / `success: true`.
- Commit `f14437fc2e83b51bb57e561f97b4aefbd27f3c9f` substituiu o UUID placeholder em `anytv-resolver/wrangler.jsonc`. Não inseriu chaves ou tokens.
- Migração SQL baseada em `anytv-resolver/migrations/0001_create_tables.sql` aplicada via API de D1: `monthly_quota`, `link_cache`, `idx_anytv_cache_expiry`. As três operações retornaram `success: true`.
- **Readback remoto comprovado** por `SELECT name FROM sqlite_master WHERE type IN ('table','index') ORDER BY name`: ambos os nomes de tabelas e índice presentes. D1 foi criado como recurso **novo do AnyTV**, sem alteração dos três Workers CLFCJ existentes.
- **Ainda não feito:** Worker implantado com bindings, secret `ANYTV_MOVIE_API_KEY` no Cloudflare, autenticação segura de APK, domínio público e integração/testes físicos. GitHub secret não migra automaticamente ao Cloudflare; não copiar credencial para código, CI ou logs. Sem exposição de endpoint público até validação de segredos e autenticação.


### Worker piloto implantado sem exposição pública — 15/10/2026

- Publicado **código fonte existente** `anytv-resolver/src/worker.mjs` na conta Cloudflare como Worker `anytv-link-resolver-pilot` (API `PUT /accounts/{account_id}/workers/scripts/anytv-link-resolver-pilot` retornou HTTP 200, sucesso; etag `b5d2ad4fc85376c5a2343784978ef7bb5720bd9059b5fab67f184dfc40107aee`).
- Bindings no Worker, com readback da API: `DB` (D1 dedicado `anytv-link-resolver`) e `MAX_UPSTREAM_CALLS_MONTH=200` (texto). A publicação não inclui nem transfere `ANYTV_MOVIE_API_KEY`, `ANYTV_CLIENT_TOKEN`, nem `RATE_LIMIT` de produção; esses parâmetros seguem **pendentes**. O código retorna `not_configured` quando algum deles falta, impedindo consulta upstream.
- Endpoint `workers.dev` **DESABILITADO**, conforme GET `/subdomain` = `{enabled:false,previews_enabled:false}`; **nenhum hostname público configurado**. Não anunciar como API funcional até segredos e autenticação apropriados estarem presentes, uma rota segura estar definida e testes ao vivo aprovados.
- Cloudflare MCP comprovou D1 criado e tabelas `monthly_quota`, `link_cache`, índice `idx_anytv_cache_expiry`; configurações vinculadas no arquivo versionado `anytv-resolver/wrangler.jsonc`. Não reutilizar credenciais nem recursos CLFCJ.
- **Próxima etapa obrigatória:** configurar segredo comercial no Cloudflare por canal seguro sem expor seu valor; vincular RATE_LIMIT apropriado; substituir autenticação piloto por solução de identidade/dispositivo não compartilhada no APK, respeitar licenças; somente depois habilitar rota pública e integrar cliente/Connect. Não modificar aplicativos já instalados enquanto assinatura e distribuição nova não estiverem verificadas.


### Worker Cloudflare — rate limit implantado e conferido (15/10/2026)

- Reenvio do mesmo código `anytv-resolver/src/worker.mjs` com três bindings no Worker `anytv-link-resolver-pilot`: `DB` (D1 `anytv-link-resolver`), `MAX_UPSTREAM_CALLS_MONTH` (texto `200`) e `RATE_LIMIT` (`ratelimit`, namespace `275913`, limite nominal `20` por 60 segundos). API de upload retornou HTTP 200 / `success:true`.
- Readback remoto `GET /workers/scripts/anytv-link-resolver-pilot/settings` confirmou exatamente os tipos `DB:d1`, `MAX_UPSTREAM_CALLS_MONTH:plain_text`, `RATE_LIMIT:ratelimit`. `GET /subdomain` confirmou `enabled:false`. Não foram alterados os Workers CLFCJ.
- O rate limit é aproximação de tráfego e **não substitui** quota mensal atômica do D1, autenticação e checagem da franquia contratada. Segredos `ANYTV_MOVIE_API_KEY` e autenticação de cliente permanecem sem configuração comprovada neste Worker. Não habilitar rota pública nem declarar funcionamento end-to-end.
