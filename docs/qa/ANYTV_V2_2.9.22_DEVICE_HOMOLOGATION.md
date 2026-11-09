# AnyTV V2 2.9.22 — homologação real no celular (PENDENTE)

**Canônico do ciclo:** `docs/roadmap/PROJECT_PROGRESS.json`.  
**Versão aprovada tecnicamente:** `2.9.22-AnyTV`, `versionCode 51`, `com.tclremote.livre.v2`.  
**Build e publicação:** [GitHub Actions 40045953034](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40045953034), `completed/success`, 32 etapas.  
**Testes TMDB de ordenação:** [40045771887](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40045771887), `success`.  
**APK SHA256:** `44cab69ad0eb6c0110ff4180e6388f042e667d8b86dcd319e39d777973d85d94`.  
**Distribuição OTA:** `feat/tcl-v22-updates-back-bluetooth:updates/android/v2/current.apk`, blob Git `39084853e1956f4aaedb3d8443ade9304e497581`.  
**Cloudflare vigente (confirmado em produção):** Worker `ce51439e-ca19-40d3-85e8-019c7a3669a5`, deployment `3298f15b-68d1-455b-bbc1-624925d7ccaf`, 100% do tráfego, sem quotas internas mensais nem limitação interna por minuto; autenticação e cache preservados.

> **Estado atual: AINDA NÃO HOMOLOGADO EM APARELHO.** CI, testes de contrato, assinatura e readback são evidência de entrega técnica, não de interação física. Não declarar PASS para F0–F8 nem aumentar o progresso de 7/11 sem evidências de homologação.

## Critérios práticos de aceitação

Marcar cada item **PENDENTE**, **PASS** ou **FAIL**, anotando modelo/Android, hora, evidência do aparelho e, quando FAIL, reprodução. A ausência de serviço de streaming ou de trailer deve ser registrada como **N/A (dados não disponíveis)**, não como êxito inventado.

| ID | Ação no aparelho | Resultado esperado | Estado |
| --- | --- | --- | --- |
| D01 | Instalar a atualização a partir de AnyTV V2 2.9.21 | 2.9.22/code51 reconhecida; mantém pacote, favoritos e pareamento | PENDENTE |
| D02 | Abrir a busca, digitar um termo e abrir o teclado virtual | Placeholder, caracteres inseridos e cursor totalmente visíveis dentro do campo; texto não corta em cima/baixo | PENDENTE |
| D03 | Pesquisar um filme e uma série, limpar a busca e repetir | Resultados reais aparecem; `×` limpa sem fechar a interface; sem erro `monthly_quota_exhausted` | PENDENTE |
| D04 | Abrir Ação e alternar **Lista / Grade 3** | Lista mostra cartaz, título, ano, nota, canal confirmado e sinopse (até 160 caracteres/3 linhas); grade mostra 3 cartazes por linha em larguras iguais | PENDENTE |
| D05 | Na mesma página mudar **Mais populares, Melhor nota, Mais recentes, Título A–Z** | A ordem realmente muda na origem; não apenas entre os 20 itens atuais; configuração persiste após reabrir | PENDENTE |
| D06 | Carregar mais filmes em duas ordenações diferentes | `Mostrar mais 20` adiciona até 20 ao fim sem duplicar o primeiro lote nem perder a ordenação; não aparece na home | PENDENTE |
| D07 | Favoritar e marcar assistido em modo lista e modo grade | Estado é coerente com a ficha do título e persiste na lista/assistidos; série alterna acompanhamento | PENDENTE |
| D08 | Tocar **Trailer** em um título com vídeo confirmado | Abre o trailer real no YouTube/navegador; se indisponível, informa sem abrir URL fictícia | PENDENTE |
| D09 | Deslizar 10 destaques na página de gênero | Até 10 slides; dots sobre a imagem, legíveis; título e canais permanecem alinhados | PENDENTE |
| D10 | Conferir o banner compacto da página inicial com título de 1 e 2 linhas | Título centralizado na área reservada; `Ver ›` fixo embaixo e cinza-claro; logo do canal aparece quando a disponibilidade BR for confirmada, sem rótulo inventado | PENDENTE |
| D11 | Rolar lateralmente seções de duas fileiras da home | Colunas com 116 dp e intervalo de 9 dp; títulos ocupam espaço fixo de três linhas, sem deslocar o cartaz inferior | PENDENTE |
| D12 | Usar D-pad, volume, Bluetooth, teclado/IME, lançar app na TV e reconectar | Funções anteriores não apresentam regressão; registrar observações no aparelho TCL | PENDENTE |

## Coleta mínima de evidência

Para falhas visuais, guardar **um print do estado afetado**, informando versão, modelo e qual ID falhou. Para falhas de comportamento, descrever os toques e o resultado observado. Não exigir vídeo, logcat ou acesso remoto se o print e a reprodução bastarem.

**Regra de fechamento:** homologar somente os itens comprovados; no ciclo seguinte, corrigir apenas falhas reproduzidas e então gerar **uma** nova versão V2 validada. Sem novas versões de APK apenas por documentação.

## Estado técnico confirmado

- `40045953034`: build/IME/busca real/assinatura/publicação/readback **PASS**, mesmo signer V2.
- `40045771887`: consulta TMDB filme/série e ordenação/paginação **PASS**.
- OTA legado `updates/android/cert2026/current.apk` preservado (Git blob `44c766b433ddb370f4589b87843f1edfd9cd7b7a`).
- **Atualização posterior à publicação do APK:** removido também o antigo limite de 200/mês da API de links Movie of the Night e o rate-limit Cloudflare por IP/ativação; removidos os bindings `MAX_UPSTREAM_CALLS_MONTH` e `RATE_LIMIT` em produção. Nenhum limite próprio do AnyTV permanece. Autenticação, cache, validação de links e permissões foram preservados. Limites contratuais dos fornecedores externos independem dessa mudança. Worker atual `ce51439e-ca19-40d3-85e8-019c7a3669a5`; offline CI `40052421285` e smoke real `40052526994`: SUCCESS.


## 09/11/2026 — Roteiro incremental QA LG webOS (code93 unsigned, NAO EXECUTADO)

Esta secao e plano de homologacao, NAO evidencia fisica. **Candidata final de codigo ja compilada:** CI unsigned [#42669473078](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/42669473078), job 129341876741, SUCCESS no SHA d50dfc58373d748f52c839d581c493485954f22e (inclui protecao WebOSTVService.isConnected), artifact audit-only ID 13697179971, ZIP SHA-256 2fe89294b58f5638caa75121e117ec3a0b9d772d00f3fa24b8135d1925c3d4cc, expira em 23/11/2026. A versao antiga do CI #42668068400 nao e a fonte final. **O artefato e sem assinatura**, portanto nao substitui a APK V2 assinada instalada no celular. Para executar LG01-LG09, exige-se via gate independente uma APK instalavel com MESMO pacote e certificado V2 e versionCode maior que a versao realmente instalada, distribuida de modo interno sem OTA publica; alternativamente, uma build de teste isolada aprovada com pacote diferente em aparelho de QA, sem reivindicar equivalencia de migracao. Nenhuma das duas rotas foi autorizada/executada aqui.

| ID | Ensaio real LG | Prova exigida | Estado |
|---|---|---|---|
| LG01 | TV LG webOS e Android na mesma rede; procurar dispositivos | Modelo LG, versao webOS, rede e screenshot da lista mostrando LG e TCL | PENDENTE |
| LG02 | Escolher LG pela lista e confirmar autorizacao na propria TV | Evidencia de FIRST_SCREEN, sem PIN de Android TV e sem falso sucesso antes da conexao SSAP | PENDENTE |
| LG03 | Enviar cima/baixo/esquerda/direita, OK, Home, Voltar | Comando + resposta observada para cada tecla; testar TV sem suporte quando aplicavel | PENDENTE |
| LG04 | Volume +/-, mudo/unmute, canal +/- e play/pause | Comando e efeito real; indisponivel deve aparecer como indisponivel | PENDENTE |
| LG05 | Perder Wi-Fi, desligar/religar TV e voltar ao app | Nenhuma tecla enviada quando webOS desconectado, estado nao conectado, nova selecao/pareamento permitido | PENDENTE |
| LG06 | Encerrar/reabrir AnyTV e selecionar novamente a LG | Verificar se o DefaultConnectableDeviceStore reusa a chave com autorizacao anterior; falha de persistencia vira bug | PENDENTE |
| LG07 | Trocar LG para TCL/Android TV e vice-versa | PIN TLS, reconexao Android TV, long press, fones e companion TCL funcionais; chamadas TCL NUNCA enviadas ao IP LG | PENDENTE |
| LG08 | Abrir Streaming e popup remoto com LG selecionada | Reutilizacao visual, minimizar/reativar, botoes remoto reais; abertura direta de titulos LG NAO IMPLEMENTADA | PENDENTE |
| LG09 | Testar LG com servico Cast conectado, mas socket webOS caido | Direcional/volume bloqueados sem sessao webOS ativa mesmo com isConnected agregado verdadeiro | PENDENTE |

### Preparacao e evidencias para executar LG01-LG09 (sem homologacao automatica)

1. **Preflight:** conferir modelo/ano da LG, versao webOS, disponibilidade do recurso de controle por apps/rede, ambos na mesma rede local, permissoes Android de descoberta, versao e assinatura atual do AnyTV no telefone e ausencia de bloqueio de rede/multicast. Nao registrar IP residencial, chave de cliente, PIN, certificado nem identificadores privados no GitHub.
2. **Entrega de QA:** somente apos autorizacao especifica, gerar e verificar APK instalavel no canal interno; comprovar origem Git SHA d50dfc58, package, versionCode, signer SHA-256 compatibilidade V2, hash SHA-256 da APK, destino restrito e ausencia de atualizacao OTA. O ZIP unsigned 13697179971 nao e instalavel como update da V2.
3. **Execucao:** um operador com acesso fisico a LG e a TCL percorre LG01-LG09; para cada item registrar data/hora local, modelo e webOS, estado PASS/FAIL/N/A, resultado observado, foto/captura apenas quando necessaria, passos minimos de reproducao quando FAIL e referencia de evidencia sem segredos.
4. **Pareamento/retorno:** LG02 testa prompt FIRST_SCREEN na propria LG; LG06 testa chave reutilizada apos encerrar o app, distinguindo re-selecao manual de reconexao automatica (esta ultima nao implementada nem homologada na LG). LG05 confirma que perda de socket bloqueia comandos.
5. **Nao-regressao:** LG07 verifica retorno para TCL com PIN/certificado Android, fones/companion, D-pad/long press e reconnect; testar selecao de LG seguida de Android e vice-versa. O codigo atual conecta todos os servicos descobertos pelo ConnectSDK (inclusive Cast quando presente); falhas de servico secundario durante pareamento LG sao area de observacao, nao resultado ja comprovado.
6. **Aceite restrito:** build PASS nao fecha LG01-LG09 nem F5/F6/F8; sem QA fisica = PENDENTE. Qualquer falha reproduzida leva a reparo minimo, novo CI unsigned e novamente QA, sem publicacao automatica. Abertura direta de filmes/series na LG e teclado LG sao capacidades nao implementadas, nao exemplos de sucesso.

**Aceite:** teste documental de build + aparelho real LG e TCL, registro de defeitos, versao/modelo, hashes de APK e identidade da assinatura; reavaliar permissao expressa para publicar. Nao homologar F5/F6/F8 por passar testes de contrato ou compilar.


### Portao QA LG code93 — assinatura interna isolada (DESIGN_ONLY, NAO EXECUTADO)

**Pesquisa documental de 09/11/2026:** Android exige que a APK instalada tenha assinatura valida, que o applicationId coincida com a instalacao anterior e que o certificado seja identico ou haja prova de rotacao valida; versionCode inferior normalmente e bloqueado. Referencias oficiais: https://developer.android.com/google/play/app-updates e https://developer.android.com/studio/publish/app-signing e https://developer.android.com/studio/publish/versioning. A assinatura em teste nao e uma autorizacao implicita de publicacao.

**Evidencia concreta no repo:** o workflow existente `.github/workflows/anytv-v2-code91-signed-release.yml` recupera o PKCS12 V2 do backup criptografado usando segredo GitHub em ambiente temporario, verifica fingerprint da identidade, chama `apksigner`, valida badging/certificado e **depois publica na branch OTA**. Por isso o workflow code91 NAO E ROTA DE QA LG, nao deve ser disparado, editado como atalho ou autorizado com `PUBLISH_ANYTV_V2_CODE91`. Existe apenas build LG **unsigned**, CI #42669473078 PASS no SHA d50dfc58373d748f52c839d581c493485954f22e, artifact #13697179971; o ZIP de CI nao e APK assinada. O backup criptografado historicamente funcionou para code90, mas sua disponibilidade futura nao foi provada nesta fase.

**Desenho sujeito a aprovacao especifica** (nenhum novo workflow executavel ou signer foi criado):

| Gate | Evidencia exigida | Estado |
|---|---|---|
| QA-S01 | Autorizacao humana expressa para criar e executar fluxo de assinatura INTERNA LG sem OTA, com destino e aparelho identificados | PENDENTE |
| QA-S02 | Workflow distinto, com revisao previa, **sem** `git push`, `release`, publish, deploy, Worker, D1, upload publico ou promocao de `main`; permissoes minimas, sem disparo por push | PENDENTE |
| QA-S03 | Fonte imutavel exata de code93 d50dfc58; verificar hash da APK unsigned produzida, compilacao e testes; NAO presumir que o ZIP digest e o hash da APK | PENDENTE |
| QA-S04 | Material criptografado V2 e segredo acessiveis SOMENTE na etapa autorizada, com mascara e limpeza de temporarios; nenhum P12/senha/segredo em artifact, log ou GitHub | PENDENTE |
| QA-S05 | `apksigner verify --print-certs` confirma mesmo certificado V2 oficial; `aapt dump badging` confirma `com.tclremote.livre.v2`, code93 e `2.9.65-AnyTV`; comparar versao realmente instalada | PENDENTE |
| QA-S06 | Gerar artifact PRIVADO de QA separado da OTA com hash da APK assinada, badge/identidade, proveniencia de fonte, prazo de retencao e autorizacao de acesso; verificar download e readback | PENDENTE |
| QA-S07 | Preservar o celular principal com code90; preferir segundo aparelho sem dados de producao; teste code93 sobre V2 existente requer aceitar risco de nao conseguir voltar ao code90 sem construir nova versao superior | PENDENTE |
| QA-S08 | Execucao fisica LG01-LG09 e regressao TCL com provas; somente depois discutir homologacao, politica de versao e publicacao | PENDENTE |

**Duas rotas diferentes:** (A) no aparelho dedicado com o AnyTV V2 existente, assinar code93 com mesmo pacote/certificado V2 e **versionCode superior ao instalado** (testa atualizacao real, mas eleva baseline do aparelho); (B) pacote diferente para QA isolada exige receita, identidade e dependencias de autenticacao/links proprias e **NAO comprova** migracao do app V2 oficial. Nao inventar que uma APK unsigned de mesmo pacote e instalavel em paralelo ao app assinado. Escolher A ou B so apos analisar aparelho e requisitos reais.

**Observacao GitHub:** `workflow_dispatch` so dispara se o workflow estiver registrado na branch default (documentacao: https://docs.github.com/en/actions/managing-workflow-runs/manually-running-a-workflow). Nao promover arquivos para `main` apenas para facilitar testes; revisar alternativa com executor independente ou registro controlado de workflow quando houver autorizacao. Nenhuma assinatura/instalacao/OTA foi feita por este plano.
