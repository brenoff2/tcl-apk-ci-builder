> **CONTINUIDADE MAIS RECENTE (24/10/2026):** a candidata V2 de interface e links está na branch `feat/anytv-p0-device-proof-verifier-20261020`/PR #3, **validada em CI**: Android #40060106346 (39+2 testes, Gradle PASS), Worker #40060118239 (32/32, dry-run PASS) e companion TV #40060021841 (7+3, Gradle PASS). **NÃO assinada, publicada, implantada nem testada no aparelho**. A versão disponível ainda é AnyTV 2.9.22/code51, Worker ce51439e. Estado e próximos gates: [QA candidata](../qa/ANYTV_V2_POST_2922_UI_LINKS_CANDIDATE.md), `docs/roadmap/PROJECT_PROGRESS.json`, 7/11=63%, F0–F8 não homologados. Não tratar 401/503 como indisponibilidade; diferenciar 404 `not_available` de 404 `no_supported_link` e 409 `ambiguous`.

# ANYTV — PRIMEIRA MENSAGEM PARA O PRÓXIMO CHAT (ESTADO CORRETO E ATUAL)

**Leia este texto como instrução de continuidade. Não reinicie o projeto, não confunda a V2 com o legado e não use o handoff arquivado de 2.9.12/2.9.13 como estado atual.**

Estou retomando o **PROJETO ANYTV**. O objetivo é manter e melhorar o aplicativo Android AnyTV que controla a TCL/Android TV (LAN/TLS, pareamento, D-pad, volume, teclado/IME, Bluetooth), lança plataformas de streaming e apresenta catálogo de filmes/séries com informações verificadas do TMDB e disponibilidade no Brasil. Preciso de **continuidade real**, a partir dos commits e do estado operacional, e de respostas com progresso, feitos/pendentes e provas. Não me faça recontar decisões.

## Canônico obrigatório

- Repositório privado: `brenoff2/tcl-apk-ci-builder` — https://github.com/brenoff2/tcl-apk-ci-builder
- Desenvolvimento: `feat/anytv-p0-device-proof-verifier-20261020` (PR #3 draft); distribuição OTA: `feat/tcl-v22-updates-back-bluetooth`. Não mesclar automaticamente em main.
- Leia primeiro `docs/roadmap/PROJECT_PROGRESS.json`, **handoff atual e completo** `docs/handoffs/2026-10-23-ANYTV-V2-2922-TRANSICAO-ATUAL.md`, `AGENTS.md`, `docs/STATUS.md`, `docs/handoffs/CURRENT_HANDOFF.md` e checklist `docs/qa/ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md`. Arquivo histórico anterior `docs/handoffs/2026-10-23-anytv-full-transition.md` contém contexto antigo, não o release vigente.
- Código central: `overrides/TclStreamingScreens.kt`, `anytv-resolver/src/worker.mjs` e módulos de autenticação, `.github/workflows/release-anytv-phone-v2-new-package.yml` e testes.

## Estado de fato — NÃO REFAZER

**AnyTV V2 2.9.22-AnyTV, code 51, `com.tclremote.livre.v2` PUBLICADA.** GitHub Actions #40045953034 terminou SUCCESS **32 etapas** (Android build, IME, busca real, assinatura, OTA e readback byte-a-byte). APK `updates/android/v2/current.apk` branch OTA, SHA256 `44cab69ad0eb6c0110ff4180e6388f042e667d8b86dcd319e39d777973d85d94`, Git blob `39084853e1956f4aaedb3d8443ade9304e497581`, tamanho 28.412.772 bytes. Assinatura V2 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9` preservada; APK legado `updates/android/cert2026/current.apk` mantém blob `44c766b433ddb370f4589b87843f1edfd9cd7b7a`. O canal V2 é independente do legado.

**Última determinação do usuário IMPLEMENTADA EM PRODUÇÃO:** retirar **QUALQUER LIMITE ARTIFICIAL DE USO**. Worker Cloudflare atual `ce51439e-ca19-40d3-85e8-019c7a3669a5`, deploy `3298f15b-68d1-455b-bbc1-624925d7ccaf` com 100% do tráfego; foram removidos **200 chamadas/mês** (inclusive na API comercial de links **Movie of the Night**), **20 chamadas/minuto** por IP e limitação de ativação. Foram eliminados os bindings `MAX_UPSTREAM_CALLS_MONTH` e `RATE_LIMIT`. Permanecem 5 bindings autorizados, segredos privados, D1/cache, autenticação por prova do dispositivo, URL allowlist e os três módulos. Auditoria direta da Cloudflare confirmou; tests 40052421285 e live smoke 40052526994 **SUCCESS**. **JustWatch não é a API comercial de links tarifada**: é necessário distinguir da Movie of the Night (`api.movieofthenight.com/v4`). Limites de planos externos Cloudflare/TMDB/fornecedor não são removíveis pelo código. **Não é preciso novo APK** para essa atualização do Worker.

## Visual e funcionalidade aprovada/implementada no código V2

- Marcadores F (filme) branco em azul, S (série) branco em vermelho, compactos e centralizados; nota e logos apenas de serviços brasileiros confirmados.
- Home streaming: carrosséis horizontais com **duas fileiras** e coluna de largura fixa **116dp**, espaço horizontal **9dp** e altura de título fixa de **três linhas / 42dp**, evitando deslocamento dos filmes da fileira inferior. **NÃO apresentar “Mostrar mais” na home**.
- Gênero filtrado (ex.: Ação): destaque no topo com **10 slides**, dots sobrepostos à imagem; abaixo escolha **Lista / Grade de 3 cartazes por linha**, com 20 resultados por página e botão **“Mostrar mais 20”** só nessa página; persistência de ordenação popularidade, melhor nota, mais recentes ou título A–Z, aplicada no backend a todas as páginas.
- Lista: ano, nota, canal verificado, sinopse resumida até 160 caracteres/três linhas, favoritos e assistidos/acompanhamento integrados à persistência, ação Trailer sob demanda com YouTube real.
- Banner compacto da tela inicial: até 10 filmes, texto do título centralizado em espaço reservado para 2 linhas, `Ver ›` fixo no rodapé em cinza claro; logo do canal ao lado **se houver prova** de oferta.
- Busca: substituído `OutlinedTextField` de 42dp que cortava texto por `BasicTextField` de **44dp** com texto/cursor/placeholder centralizados; busca automática, X para limpar e pesquisa real.
- Notificação de estreia `Lembrar` é **local Android aproximada**, não push remoto; data TMDB não assegura estreia no streaming. Exige QA físico.
- Objetivo de identidade visual em outra frente: nome **AnyTV**, minimalista, dark com vermelho; **não redesenhar app sem solicitação específica**.

## Progresso e pendências REAIS

`docs/roadmap/PROJECT_PROGRESS.json`: **7/11 marcos técnicos, 63%**, e todos os gates **F0–F8 = NAO_HOMOLOGADO**. A publicação V2 e os testes do Worker não aprovam experiência no aparelho. Próxima tarefa principal é executar ou orientar a coleta das evidências **D01–D12** em `docs/qa/ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md` (instalação preservando dados, busca/teclado, lista/grade, classificação/paginação, favorito/assistido, trailer, slider/dots, banner, alinhamento, Bluetooth/IME/TCL). Não inventar PASS; não pedir instalação repetida sem necessidade. Se houver falha reproduzida, corrigir pontualmente e fazer **uma** release V2 validada com mesmo pacote/certificado, build, testes, assinatura e readback. Pendente também M08 ativação em aparelho, M09 abertura de título na TV, M10 homologação integrada/rollback, M11 estabilidade/aceite. Manter gates não homologados.

Histórico detalhado da 2.9.8–2.9.13 (incluindo SSL/TLS, RSA e erros de assinatura) está preservado no arquivo histórico, **não constitui bloqueio da versão atual V2**. A progressão 2.9.20→2.9.21→2.9.22, seus requisitos e os reparos de quota estão integralmente detalhados no novo handoff canônico. Não confundir certificados V2 e cert2026 nem expor segredos.

**AÇÃO NO NOVO CHAT:** (1) conferir estado do GitHub e Worker, (2) mostrar quadro de progresso verificado e D01–D12 pendentes, (3) prosseguir do ponto da homologação e das falhas reais, sem mais pesquisa/release só por organização. Documentar cada nova evidência no canônico e avançar o máximo sem perder contexto. Nenhum uso interno deve ser artificialmente bloqueado.
