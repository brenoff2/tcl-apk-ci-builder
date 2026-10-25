# ANYTV — TRANSIÇÃO INTEGRAL PARA NOVO CHAT (25/10/2026)

> **PONTO EXATO DE RETOMADA, verificado no GitHub:** branch DEV `feat/anytv-p0-device-proof-verifier-20261020`, PR [#3](https://github.com/brenoff2/tcl-apk-ci-builder/pull/3) **OPEN/DRAFT**, HEAD anterior ao registro deste handoff **`7e4b488dac667be9e410d1d57f7758249f341a36`**. Commit funcional mais recente `182a97de87cb8ea64695296aeeda4ca76f81d27f`, correção **somente de contrato de teste** `ffb2c8d85d7b2271129891936e15b85aed4c78c5`, seguido do commit documental `7e4b488d`. **Não confundir commit de documentação com build de código.**

## 1. Objetivo permanente
Desenvolver **AnyTV**, aplicativo Android `com.tclremote.livre.v2` para controle remoto da TCL/Android TV e descoberta de filmes/séries reais com TMDB e ofertas de assinatura verificadas para Brasil via Cloudflare Worker. Preservar o controle remoto e o pareamento, UI aprovada, integração com streaming, histórico, busca, filtros, Cinema BR, paginação de 24, continuidade de dados, fontes e telemetria sem falsos resultados. Atuar com entregas técnicas grandes e autônomas por turno, sem exigir dezenas de mensagens "continue" e sem confundir código escrito/CI com QA em aparelho.

## 2. Fontes canônicas e onde ler primeiro
- Repositório privado: https://github.com/brenoff2/tcl-apk-ci-builder
- PR DEV #3: https://github.com/brenoff2/tcl-apk-ci-builder/pull/3
- **Este documento:** `docs/handoffs/2026-10-25-ANYTV-TRANSICAO-INTEGRAL-POS-7E4B.md`.
- `AGENTS.md`; `docs/STATUS.md`; `docs/handoffs/CURRENT_HANDOFF.md`; `docs/handoffs/NEXT_CHAT_FIRST_MESSAGE.md`; `docs/roadmap/PROJECT_PROGRESS.json`.
- QA recente: `docs/qa/ANYTV_20261025_WATCHED_RETRY_GENRE_FAILURE.md`, `docs/qa/ANYTV_V2_2.9.25_DEVICE_HOMOLOGATION_PLAN_20261025.md`, `docs/qa/ANYTV_20261024_MOVIE_404_CONTINUITY_QA.md`, `docs/qa/ANYTV_20261024_MOVIE_ID_REMOTE_RECOVERY_DEV.md`, `docs/qa/ANYTV_20261024_WATCHED_HISTORY_LEGACY_IDS.md`, `docs/qa/ANYTV_20261024_EMPTY_FUTURE_PROVIDER_GENRES.md`, `docs/qa/ANYTV_20261024_UPCOMING_STREAMING_PAGINATION.md`, `docs/qa/ANYTV_20261024_CINEMA_PROVIDER_PAGINATION.md`.
- Evidência da assinatura: `docs/qa/ANYTV_2925_CODE54_SIGNED_OTA_RELEASE_20261024.md`. **Ver sempre SHA e gate, não confiar cegamente em texto histórico com versões antigas.**

## 3. Produção — CONGELADA, identidade exata
- **OTA assinada e publicada:** AnyTV **2.9.25/code54**; pacote `com.tclremote.livre.v2`; branch `feat/tcl-v22-updates-back-bluetooth` SHA `dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112`.
- `updates/android/v2/current.apk` SHA256 **`c53596d8f8210bf0a12769481796481a851d00da4c52f544120b85367dd17fe0`**, 26.012.572 bytes, certificado V2 SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`; Actions release #40106161667 SUCCESS.
- Companion TV `com.tclremote.tvcompanion` anterior: 2.5/code16. OTA legado `updates/android/cert2026/current.apk` preservada.
- Cloudflare Worker `anytv-link-resolver-pilot`: versão publicada `58fe4b33-6ee2-4841-8587-c28acaaad435`, deployment `25de8328-10ab-471a-8e94-fb4cdbce13d9`, 100% tráfego; D1, bindings/segredos, modo device e ativação preservados. **Worker publicado ainda não suporta `/v1/catalog/movie/{id}`.**
- **VETOS sem autorização explícita:** merge PR, assinatura ou publicação de APK, OTA, deploy Worker, migração D1, sobrescrever certificado, tocar OTA legado, afirmar reprodução na TCL, ou elevar gates de homologação. Candidatos `unsigned/code54` não podem substituir assinatura/code54 em telefone. Futura publicação exige versãoCode **>54**, certificado original, aprovação e testes físicos.

## 4. UI e comportamento já aprovados (NÃO REDESENHAR)
- Navegação compacta e ação principal na home sem marca duplicada no topo; hero com imagem original, gradiente e **shadow/blur** no título; ano e selo **Cinema** com fundo azul quando fonte BR comprova exibição em cartaz; pager minimalista em vez de dots intrusivos.
- **Três posters por linha**, original **2:3 inteiro** com `ContentScale.Fit`, sem corte da base; cartões e ícones de streamings originais, nome/nota/ano sem falsas afirmações.
- Conteúdo por Filmes/Séries/Todos, provedores e gêneros; "Ver mais" e "Mostrar mais 24" preservam contexto, provedor e filtro. Cinema internacional ou futuro não recebe selo "Cinema BR" sem confirmação em `movie/now_playing?region=BR`.
- Ofertas de assinatura somente quando **flatrate BR verificada**; sem declarar playback, assinatura ou deep-link confirmado quando não houve verificação. Informar fallback de busca real na TCL.

## 5. Linha do tempo técnica: o que já foi feito
1. Bases da navegação, overlays, streaming, imagens e favoritos; correções de hero, grade 3×/2:3 e marca aprovada em commits anteriores `08cd4807`, `9ac3529f`, `8d479b5a`.
2. `a92c3703` + `6183135d`: evitar falso selo Cinema internacional/futuro; "Ver mais" de gêneros em provedor permanece no serviço, incluindo páginas adicionais. CI #40123511774 SUCCESS.
3. `c1ba0c10` + `c8bbbb04`: `Em breve · Streaming` passou a paginar filmes e séries futuros, verificando assinaturas BR, preservando filtros e overflow. CI #40125105465 SUCCESS (62 contratos Streaming, 2 credenciais, 11 visuais), prova isolada SUCCESS.
4. `ef73ec03` + `229b7567`: mesmo se primeira página futura não tiver resultado BR válido, continuar navegável por páginas seguintes; alinhar filtro de gênero em Filmes/Séries e provedor. CI final #40127825662 SUCCESS (64+2+11); **CI intermediário #40127722728 FAILED** — não marcar como aprovado.
5. `4ade163d`: persistência durável do histórico de filmes assistidos, snapshots reais em `anytv_watched_movie_titles_v1`, migração em lote do cache/favoritos e aviso com IDs TMDB antigos sem dados, sem fabricar capa/nome. CI #40129882058 SUCCESS (66+2+11).
6. `e6501f52`: Worker DEV allowlist GET `movie/{id}` apenas pt-BR (segredo somente no Worker, cache D1, segurança), Android DEV recupera até 12 IDs legados e para no HTTP 400 do Worker publicado. CI Android #40132378495 SUCCESS (68+2+11); Worker #40132378555 SUCCESS (34 testes); prova isolada SUCCESS. **Nenhum deploy.**
7. `53c6b8d1`: Worker DEV traduz HTTP 404 somente para detalhe `movie/{id}` e não grava negativo; Android não deixa ID inexistente bloquear os válidos seguintes, e persiste sucessos parciais mesmo ao sair da tela. CI Android #40136181568 SUCCESS (69+2+11), Worker #40136181610 SUCCESS (35), prova isolada #40136181616 SUCCESS (55). QA `ANYTV_20261024_MOVIE_404_CONTINUITY_QA.md`.
8. **Etapa mais recente (25/10): `182a97de` + `ffb2c8d8`**: a aba Assistidos não declara histórico vazio se existem IDs antigos sem cartazes; botão **"Recuperar mais 12"** somente sob demanda e quando Worker suporta rota, sem loops; primeira página de Filmes/Séries/Cinema não trava com spinner infinito após erro, registra loading/error e mostra **"Tentar novamente"**. CI unsigned **#40169560233 COMPLETED/SUCCESS (71 testes Streaming + 2 credenciais + 11 visuais; Gradle BUILD SUCCESSFUL em 4m13s)**, identidade V2/code54, artifact **#12589154176**, 23.289.396 bytes; prova isolada **#40169560291 SUCCESS, 55 testes**. **CI primeiro #40169458180 FAILED por contrato textual obsoleto** — corrigido em `ffb2c8d8` antes do CI aprovado. Commit documental `7e4b488d`, prova isolada #40169962582 SUCCESS. No relatório `docs/qa/ANYTV_20261025_WATCHED_RETRY_GENRE_FAILURE.md` há uma frase antiga **ERRADA** atribuindo COMPLETED/SUCCESS ao **#40169458180**; o correto é **FAILED** e o sucesso é **#40169560233**. Este documento de transição registra a errata e deve ser usado como autoridade até correção desse QA.

## 6. Estado atual e gates (não promover por CI)
- `docs/roadmap/PROJECT_PROGRESS.json`: **7/11 = 63%** técnico; M08/M09 `IN_QA`; M10/M11 `BACKLOG`; **F0–F8 `NAO_HOMOLOGADO`**.
- Android/TCL **NÃO HOMOLOGADOS fisicamente**. Ativação da instalação concreta, reprodução na TV, navegação no aparelho real, reação offline, bateria/memória e instalação OTA física ainda carecem de evidência.
- Plano físico atual específico da OTA **2.9.25/code54**: `docs/qa/ANYTV_V2_2.9.25_DEVICE_HOMOLOGATION_PLAN_20261025.md`. Plano 2.9.22 antigo é apenas histórico.
- Não alegar que DEV (pôsteres, "Recuperar mais 12", nova rota) já está no APK publicado.

## 7. Onde estão fontes, testes e segurança
- Kotlin: `overrides/TclStreamingScreens.kt` (página grande); testes `scripts/tests/test_streaming_screens_contract.py`.
- Worker: `anytv-resolver/src/worker.mjs`, testes `anytv-resolver/src/worker.test.mjs`. O acesso TMDB usa servidor + D1 e somente rotas allowlisted; não adicionar token ao Android.
- Workflows DEV GitHub Actions: `.github/workflows/anytv-v2-candidate-compile.yml` (**apenas compile unsigned**), `.github/workflows/anytv-resolver-ci.yml` (**node tests + Wrangler dry-run, sem deploy**), `.github/workflows/anytv-p0-device-proof-ci.yml` (teste isolado).
- PR #3 mantém código-fonte e testes auditáveis; o último commit `7e4b488d` é **somente documentação**.
- Se fizer mudanças, verificar **GitHub upstream primeiro**, adotar correção mínima, rodar testes e compilação, ler logs e artefatos, registrar relatório de QA com resultados verdadeiros; atualizar `AGENTS.md`, `docs/STATUS.md`, os dois arquivos de handoff e o roadmap **apenas depois de CI confirmado**. Não pedir "continue" repetido; trabalhar máximo possível de uma vez.

## 8. Trabalho faltante, em ordem de prioridade
1. **Corrigir documentação QA #40169458180** para `FAILED`, substituindo a falsa referência positiva por **CI #40169560233 SUCCESS**, sem alterar resultados técnicos. Esta errata já está explicitada aqui.
2. Auditoria regressiva da recuperação de Assistidos em DEV: Worker antigo HTTP 400; ID realmente inexistente HTTP 404; 503/rede; mais de 12 IDs; cancelamento e `finally`; botão manual "Recuperar mais 12"; deduplicação de snapshots e persistência; nenhum título falso. Dar preferência a **testes executáveis** além de contratos textuais onde viável.
3. Revisar a navegação da primeira página e retry de gênero/Cinema após filtros/troca de aba, garantindo que carregamento e erro não fiquem presos nem misturem providers. Preservar imagens/UI aprovada.
4. Preparar validação e plano de rollback para a nova rota Worker **sem executar deploy**; após autorização explícita separada, validar HTTP real e regressões de device activation e secrets. Não alterar produção automaticamente.
5. Homologar primeiro a **OTA assinada 2.9.25/code54** em Android/TCL com evidência das matrizes A01–A11; recursos DEV B01–B... requerem nova versão assinada autorizada e certificado original. **Sem acesso ao aparelho real, manter gates pendentes**.
6. Verificar ponteiros canônicos e progresso após cada lote; não inflar 63% sem provas.

## 9. Primeira resposta no novo chat (execução imediata)
"Estou retomando **PROJETO ANYTV** no repositório privado `brenoff2/tcl-apk-ci-builder`, branch `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 draft. O objetivo é continuar o aplicativo Android que controla a TCL e oferece catálogo TMDB/streaming BR comprovado, preservando a UI aprovada: 3 posters 2:3 sem corte, hero com shadow/blur, Cinema azul apenas quando confirmado BR, logos originais e navegação de 24. A OTA oficial permanece assinada 2.9.25/code54 (`dcc51bad`, SHA256 `c53596d8...`), Worker produção `58fe4b33...` sem movie/{id}. Último código `182a97de`, teste `ffb2c8d8`, documentação `7e4b488d`; CI unsigned #40169560233 SUCCESS (71+2+11; Gradle 4m13s), prova #40169560291 SUCCESS. Corrigimos recuperação de Assistidos e adicionamos Recuperar mais 12, spinner finito e Tentar novamente em gêneros/Cinema, **ainda apenas DEV**. Progresso técnico 7/11 (63%) e F0–F8 não homologados. Primeiro vou verificar o HEAD em GitHub, corrigir a alegação falsa de SUCCESS do CI #40169458180 no QA, auditar regressões e continuar implementando sem merges/deploys/OTA sem autorização. Vou atualizar os artefatos canônicos ao concluir a nova rodada."

**ATENÇÃO:** verificar o HEAD de novo no início do novo chat; o commit que grava esta transição fará o HEAD avançar além de 7e4b488d e não deve ser tratado como mudança no código do aplicativo.
