# PROJETO ANYTV — HANDOFF COMPLETO, ATUAL E EXECUTÁVEL
**Checkpoint: 23/10/2026 | Situação: execução técnica publicada, homologação física pendente | Esta página prevalece sobre o handoff histórico de 2.9.12/2.9.13.**

## 0. PRIMEIRA MENSAGEM OBRIGATÓRIA NO NOVO CHAT

> CONTINUE O **PROJETO ANYTV**, sem recomeçar e sem misturar a V2 com o aplicativo legado. A versão oficial **AnyTV V2 2.9.22-AnyTV (versionCode 51, pacote com.tclremote.livre.v2)** está **publicada com sucesso** no OTA GitHub; 32 etapas do workflow 40045953034 passaram, incluindo build Kotlin, testes de controle remoto/IME, busca TMDB real, recuperação do signer V2, assinatura, publicação e readback byte a byte. O Worker Cloudflare atual é **ce51439e-ca19-40d3-85e8-019c7a3669a5**, deployment **3298f15b-68d1-455b-bbc1-624925d7ccaf**, **100% do tráfego**. Em cumprimento à última decisão do usuário, **TODAS AS QUOTAS ARTIFICIAIS DO ANYTV FORAM REMOVIDAS**, inclusive 200 chamadas por mês na API de links Movie of the Night, 20/min por IP e rate-limit interno da ativação. Os bindings RATE_LIMIT e MAX_UPSTREAM_CALLS_MONTH foram removidos; preservados cinco bindings necessários, cache, autenticação, segredos e os três módulos Worker. A Cloudflare foi consultada ao vivo para confirmar. CI offline 40052421285 e smoke real 40052526994 passaram. **Não confundir com limites contratuais de fornecedores externos, que não controlamos.**
>
> O objetivo atual é **homologar de verdade no celular e na TCL a V2 2.9.22**, usando o checklist D01–D12; o usuário compartilhou um print da 2.9.21 (tela de gênero Ação) e aprovou a base visual, mas a V2 2.9.22 ainda não tem homologação presencial. **7 de 11 marcos técnicos aceitos = 63%; TODOS os gates F0–F8 = NAO_HOMOLOGADO.** Não promover gates apenas por CI. Não lançar outra versão sem corrigir uma falha reproduzida. Comece lendo `docs/roadmap/PROJECT_PROGRESS.json`, `AGENTS.md`, `docs/qa/ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md`, este handoff e `docs/handoffs/CURRENT_HANDOFF.md`. O histórico velho de 2.9.12/2.9.13 é arquivo e não representa o release atual. Preserve GitHub como canônico, certificado, pacote, conexões e dados.

## 1. ALVO E PRINCÍPIOS DO PRODUTO

- Aplicativo Android **AnyTV**, para controlar televisão TCL/Android TV via rede local e Bluetooth; pareamento TLS, D-pad, voltar, volume, teclado/IME, descoberta de dispositivos e launcher; catálogo de filmes e séries e navegação por plataformas de streaming. Há companion separado na TV (`com.tclremote.tvcompanion`, AnyTV Connect) que não deve ser substituído ou alterado por correções da UI móvel.
- Aplicativo legado `com.tclremote.livre` com certificado **cert2026** é produto/canal distinto. V2 atual `com.tclremote.livre.v2` foi criada como pacote separado para garantir atualizações sem substituir/desinstalar a versão antiga.
- Usuário usa pessoalmente o app, **não aceita qualquer quota artificial de uso**. Não reintroduzir limites mensais, por minuto, por usuário ou por dispositivo. `20 títulos/página` e `10 slides` são **paginação e decisões visuais**, não quota de utilização. Cache e validação continuam úteis.
- Catálogo: TMDB por Worker Cloudflare, com tokens guardados no servidor; Movie of the Night é a API comercial de resolução de links, distinta de metadados/atribuição TMDB/JustWatch. Não prometer gratuidade ou capacidade ilimitada de fornecedores externos nem ampliar seu contrato automaticamente.
- Qualidade: preservar funcionalidades e dados, favoritos/assistidos, identidade de assinatura e caminhos OTA; não inventar canais, disponibilidade brasileira, datas de streaming ou links diretos. Verificar título e região antes de abrir serviço; trailer somente YouTube real da API. Sem burlar DRM.
- Direção visual: app escuro, interface minimalista; intenção de marca ANYTV com vermelho/dark discutida em outra frente. Esta decisão de marca **não autoriza redesign geral nem mudar nome**; na UI de streaming foi aprovado marcador F branco em quadrado azul, S branco em círculo vermelho.

## 2. REPOSITÓRIO E PONTOS CANÔNICOS

- GitHub privado: `https://github.com/brenoff2/tcl-apk-ci-builder`.
- Branch de desenvolvimento `feat/anytv-p0-device-proof-verifier-20261020`, **PR #3 draft** (não mesclar automaticamente em `main`).
- Branch de distribuição OTA `feat/tcl-v22-updates-back-bluetooth`; app V2 `updates/android/v2/current.apk`. Link de download/OTA: `https://raw.githubusercontent.com/brenoff2/tcl-apk-ci-builder/feat/tcl-v22-updates-back-bluetooth/updates/android/v2/current.apk`.
- App antigo `updates/android/cert2026/current.apk` na mesma branch, **não modificar** em uma alteração específica da V2.
- Principal fonte de verdade `docs/roadmap/PROJECT_PROGRESS.json` (estado, gates, histórico). Regras `AGENTS.md`, `docs/STATUS.md`, `docs/handoffs/CURRENT_HANDOFF.md` e esta página. Primeira mensagem copiável `docs/handoffs/NEXT_CHAT_FIRST_MESSAGE.md`; checklist físico `docs/qa/ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md`. Histórico anterior arquivado em `docs/handoffs/2026-10-23-anytv-full-transition.md`; **seu estado 2.9.12/code41 é OBSOLETO**, jamais usá-lo como plano atual.
- Arquivos de código principais: `overrides/TclStreamingScreens.kt`; `anytv-resolver/src/worker.mjs`, `src/device-activation.mjs`, `src/device-proof.mjs`, `wrangler.jsonc`, `src/worker.test.mjs`; `.github/workflows/release-anytv-phone-v2-new-package.yml`; `.github/anytv-phone-v2-build-request.json`; `scripts/tests/test_streaming_screens_contract.py`.
- Regra de precedência: **leitura atual do GitHub + execução CI bem-sucedida + assinatura/bytes reais do APK + leitura atual da Cloudflare + prova no aparelho > textos antigos congelados**. Não excluir histórico, apenas identificar o período.

## 3. ESTADO DE PRODUÇÃO — FATOS VERIFICADOS

**APK V2 2.9.22**: `versionCode=51`, `package=com.tclremote.livre.v2`; workflow `40045953034` `completed/success` (32 passos), commit OTA de publicação `0b57a842cf7b662f6a8a85a84429d01f4901fdd4`. Signed APK SHA256:
`44cab69ad0eb6c0110ff4180e6388f042e667d8b86dcd319e39d777973d85d94`.
Arquivo no Git blob `39084853e1956f4aaedb3d8443ade9304e497581`, tamanho **28.412.772 bytes**. Certificado V2 SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. Readback HTTPS byte a byte **PASS**. APK legado `cert2026` Git blob `44c766b433ddb370f4589b87843f1edfd9cd7b7a` **inalterado**. O usuário pode usar **AnyTV V2 → Buscar atualizações**; instalador Android exige confirmação do sistema.

**Backend:** Worker `anytv-link-resolver-pilot` versão **ce51439e-ca19-40d3-85e8-019c7a3669a5** e deployment **3298f15b-68d1-455b-bbc1-624925d7ccaf**, 100%. Leitura real de script e configurações confirmou **cinco bindings**: `ANYTV_DEVICE_ACTIVATION_ENABLED`, `ANYTV_MOVIE_API_KEY` (secret), `ANYTV_RESOLVER_AUTH_MODE`, `DB` (D1) e `TMDB_READ_TOKEN` (secret). Não expor valores. Sem código `monthly_quota` / `MAX_UPSTREAM_CALLS_MONTH` / `RATE_LIMIT`; cache D1 e provas de dispositivo preservados. Remover rate-limit interno atende decisão do usuário, mas permanecer atento a cobranças/quotas contratuais do TMDB, Cloudflare e Movie of the Night. **Nenhum APK novo foi necessário para remover esses limites**.
- Teste CI offline `40052421285` `success` e smoke de produção `40052526994` `success`; contratos cobrem 225 títulos/links distintos sem limitação local, autenticação e validação.
- Trabalho anterior testou busca real Titanic (20 filmes), Breaking Bad (5 séries) e páginas de gênero (20 resultados por lote), ordenação por popularidade/nota/ano/nome; live smoke `40045771887` `success`.

## 4. EVOLUÇÃO DO APP NESTE CHAT

1. **2.9.20/code49**: destaque misto filmes/séries, categorias por gênero, mais títulos por seção, novos lançamentos, avisos de estreia `Lembrar` via notificação Android local (não push remoto), pesquisa automática com debounce, botão X para limpar. Primeira publicação permitiu quota TMDB 429 conhecida, **que posteriormente foi eliminada** do Worker.
2. **2.9.21/code50**: marcador **F branco/azul, S branco/vermelho** centralizado antes da nota; home mantém fileiras **duplas horizontais**, com cada coluna de **116 dp**, espaçamento **9 dp** e reserva fixa de **42 dp para título (3 linhas)** para o cartaz de baixo não deslocar. O botão `Mostrar mais` **não existe na home**; página de gênero mantém slider no topo e passa a lista vertical com `Mostrar mais 20` por página. Removeu-se quota compartilhada do TMDB, com cache.
3. **2.9.22/code51**: página de gênero ganhou **Lista detalhada / Grade 3 colunas**; lista com cartaz, título, ano, nota, ícones de canais **confirmados** em BR, sinopse limitada a **160 caracteres / 3 linhas**, `Trailer` sob demanda, favorito e assistido/acompanhamento reutilizando SharedPreferences/funções já existentes. Ordenação persistente em **Mais populares, Melhor nota, Mais recentes, Título A–Z**, aplicada **no TMDB antes da paginação**; `Mostrar mais 20` mantém esse critério. Slider reduzido a **10 slides**, dots **sobre** a imagem. Banner compacto da **página inicial** com título em área centralizada, `Ver ›` fixo na base, cinza-claro, logo de canal somente quando oferta BR conhecida; máximo 10 destaques. **Busca visível**: problema comprovado era `OutlinedTextField` Material3 de altura interna 56dp comprimido em 42dp; substituído por `BasicTextField` de 44dp com cursor/texto/placeholder centralizados. Release assinado/OTA OK, aparelho **ainda não homologado nessa versão**.
4. **Depois da 2.9.22 (mesmo dia)**: decisão expressa do usuário: `deixe fora qualquer limite de uso.. quem usa o aplicativo sou eu e nao quero limite de uso para nada`. Worker foi atualizado ao vivo: removeu quota própria de 200/mês do Movie of the Night, limitador 20/min e trava rate-limit de ativação, mantendo segredos, criptografia, autenticação, validação de rotas e cache. Fonte sincronizada no GitHub; teste offline e smoke live verdes. **Versão do APK não mudou; continua 2.9.22/code51**.

## 5. HISTÓRICO ANTERIOR, SEM APAGAR EVIDÊNCIAS

- Linha de base **2.9.8/code37**, reportada pelo usuário como operacional no celular com TCL; identidade cert2026 antiga. **2.9.9/code38** foi candidata Discovery Home V3; **2.9.10/code39** apresentou problema de reconexão TLS; **2.9.11/code40** tentou recuperar SSL mas alterou estado de pareamento, ficando `New`; **2.9.12/code41** restaurou transporte da 2.9.8 sem recuperar preferências perdidas; **2.9.13/code42** tentou separar RSA (TCL) da chave EC/P256 (Cloudflare), hipótese não comprovada fisicamente. Essas fases, seus testes, erros e planos de assinatura em `docs/handoffs/2026-10-23-anytv-full-transition.md` são **históricos**, não um bloqueio da OTA V2 agora.
- V2 seguiu por **2.9.14/code43**, **2.9.17/code46**, **2.9.18/code47**, **2.9.19/code48**, **2.9.20/code49**, **2.9.21/code50** até a **2.9.22/code51** vigente. Consultar histórico JSON e Actions para commits/sha anteriores; não presumir versões não listadas como homologadas.
- Cloudflare: a quota de 200 chamadas era **variável interna do Worker** (`MAX_UPSTREAM_CALLS_MONTH`), e não franquia contratual Cloudflare/TMDB. Era compartilhada indevidamente com TMDB, depois isolada no resolvedor de links, depois **removida por completo**. A API comercial usada para links é Movie of the Night; **não confundir com JustWatch**. Fonte das ofertas TMDB/JustWatch não significa que JustWatch cobrava 200 chamadas.
- Questões de pareamento SSL/TLS, ativação de dispositivo, Bluetooth, notificações locais e abertura real de títulos na TCL **não foram universalmente homologadas por CI**. O estado formal de gates deve continuar pendente até teste físico.

## 6. PENDÊNCIAS E ORDEM DE CONTINUIDADE

**P0 — HOMOLOGAÇÃO REAL (D01–D12)**. Conferir tela no Android com 2.9.22: instalação preservando dados, texto da busca e teclado, resultados, lista/grade, ordenação global, paginação 20, favorite/watched/trailer, dots/10 slides, banner e canais, alinhamento 3 linhas e controles/pareamento/IME/Bluetooth na TCL. Checklist `docs/qa/ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md`; anotar PASS, FAIL ou N/A por prova. **Não exigir dados já fornecidos e não repetir instalações desnecessárias.**
**P1 — Só depois de falha física reproduzida:** corrigir cirurgicamente e publicar **uma** versão subsequente pelo workflow oficial V2, com `versionCode` crescente, pacote/certificado idêntico, regressões Kotlin/IME, live search, assinatura e readback. Não mexer em companion/OTA cert2026 sem causa e aprovação.
**P2 — Marcos ainda não aceitos:** M08 ativação física do dispositivo, M09 abertura real de título na TV, M10 homologação integrada/rollback, M11 operação estável/aceite final. Marcos aceitos **7/11 (63%)**, mas gates F0–F8 seguem `NAO_HOMOLOGADO`.
**P3 — Marca/UI (demanda paralela, sem aprovação de implementação V2):** conceito de marca ANYTV minimalista, dark e vermelho; manter nome ANYTV e evitar efeitos 3D exagerados. Não misturar essa pesquisa de identidade visual com reparos funcionais sem solicitação específica.

## 7. REGRAS OPERACIONAIS PARA O PRÓXIMO CHAT

- **Não recomeçar, não perguntar novamente o que já está documentado, não inventar conclusão física.** Ler primeiro canônico e estado live. Dar status explícito: versão, CI, Worker, OTA, pendências, 7/11 e gates.
- **Nunca reintroduzir caps artificiais**, mesmo em nome de `proteção`, sem autorização expressa do usuário. Credenciais/autenticação/URL allowlist/cache não equivalem a limites de uso. Limites reais de fornecedores externos não podem ser removidos localmente.
- **Não publicar outra versão apenas para mudar documentação**, nem correr builds repetidos se o último já passou; preservar assinatura criptográfica V2 (SHA256 certificado acima) e APK legado.
- **Segredos:** não ler/exibir valores em resposta, GitHub, logs ou texto de handoff. Backup cifrado legado é histórico; V2 já tem fluxo de recuperação segura pelo GitHub Actions.
- **Condução:** dar progresso por etapas e atualizações objetivas. Novas evidências devem atualizar `docs/roadmap/PROJECT_PROGRESS.json` e `docs/handoffs/CURRENT_HANDOFF.md`, referenciando commits, runs, hashes e o que ainda está pendente. Nunca elevar gates por suposição.

## 8. FONTES PARA CONSULTA RÁPIDA

- Fonte principal: `docs/roadmap/PROJECT_PROGRESS.json`.
- Este handoff: `docs/handoffs/2026-10-23-ANYTV-V2-2922-TRANSICAO-ATUAL.md`.
- Primeira mensagem: `docs/handoffs/NEXT_CHAT_FIRST_MESSAGE.md`.
- QA D01–D12: `docs/qa/ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md`.
- Release: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40045953034 .
- Worker smoke: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40052526994 .
- Testes sem cap: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40052421285 .
- Contrato sort: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40045771887 .
- Histórico arquivado: `docs/handoffs/2026-10-23-anytv-full-transition.md`.

**LEMBRETE FINAL:** A última entrega em produção é **2.9.22 + Worker ce51439e**, sem limite interno de uso; falta **homologação real D01–D12**. Nenhum gate foi aprovado por inferência.
