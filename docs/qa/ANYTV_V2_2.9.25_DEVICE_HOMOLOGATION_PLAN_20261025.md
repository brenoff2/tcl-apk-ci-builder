# AnyTV V2 2.9.25/code54 — plano de homologação em aparelho real

**Data do plano:** 25/10/2026. **Estado:** PLANO PREPARADO, TODOS OS TESTES FÍSICOS PENDENTES. Nenhum aparelho foi acessado ou homologado por esta documentação.

## Fontes e identidade que não devem ser misturadas

**OTA oficial assinada:** `2.9.25-AnyTV`, `versionCode=54`, pacote `com.tclremote.livre.v2`, branch `feat/tcl-v22-updates-back-bluetooth`, commit `dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112`, SHA256 do APK `c53596d8f8210bf0a12769481796481a851d00da4c52f544120b85367dd17fe0`, certificado V2 SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. Release assinada e OTA validadas via [CI #40106161667](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40106161667). [Evidência oficial](ANYTV_2925_CODE54_SIGNED_OTA_RELEASE_20261024.md).

**Worker publicado:** versão `58fe4b33-6ee2-4841-8587-c28acaaad435`, deploy `25de8328-10ab-471a-8e94-fb4cdbce13d9`, 100% do tráfego, D1/secrets/bindings originais. Este Worker **não** tem a rota `/v1/catalog/movie/{id}` acrescentada na fonte DEV. Não inferir que essa restauração de Assistidos funciona na versão pública.

**PR #3 DEV:** `feat/anytv-p0-device-proof-verifier-20261020`. Fonte de alterações posteriores à versão assinada (pôsteres completos 2:3 em 3 colunas, recuperação por ID, botão de lote de 12, gênero com retry) e candidatos **unsigned/code54**. **Não instalar unsigned sobre APK assinado** nem considerá-los publicados. Para futura instalação de homologação é preciso release autorizado com `versionCode>54`, mesmo certificado V2 e evidências de integração.

O roteiro anterior para 2.9.22/code51, [ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md](ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md), permanece **histórico**, não é a autoridade para a OTA atual.

## Coleta mínima antes do teste

Registrar modelo do celular, versão do Android, modelo/versão do software da TV TCL, versão exibida do AnyTV, data/hora local, modo de instalação, identificação do AnyTV Connect TV se presente, conexão de rede e prints apenas quando houver falha/validação visual. Não coletar segredo, código de ativação, token, cookie ou conteúdo de conta de streaming. Todos os resultados começam como **PENDENTE**. Quando um serviço não estiver instalado ou não tiver filme no Brasil, registrar **N/A fundamentado**, nunca PASS automático.

## Matriz A — OTA assinada 2.9.25/code54 em produção

| ID | Ação no dispositivo físico | Resultado esperado | Estado |
| --- | --- | --- | --- |
| A01 | Atualizar uma instalação V2 legítima assinada anterior pelo canal OTA V2 oficial | Mesmo pacote/certificado, versão 2.9.25/code54, favoritos e pareamento mantidos | PENDENTE |
| A02 | Abrir a tela inicial e percorrer ícones reais dos apps instalados | Marca AnyTV correta, atalhos reais e cabeçalho sem logo duplicado | PENDENTE |
| A03 | Pesquisar filme e série reais, limpar, refazer e examinar ficha | Dados de fonte TMDB, teclado legível, texto não cortado, sem título inventado | PENDENTE |
| A04 | Abrir destaque e conferir nome, ano, Cinema BR se comprovado e arte | Nome sobre imagem com shadow/blur legível, selo azul apenas com comprovação Brasil, sem sobreposição anormal | PENDENTE |
| A05 | Selecionar serviço de streaming e conferir assinatura BR | Canais originais/ícones corretos; sem alegar oferta ou reprodução não confirmada | PENDENTE |
| A06 | Favoritar, marcar assistido, reabrir, reiniciar app e voltar | Persistência coerente de dados disponibilizados pela versão assinada, sem perda involuntária | PENDENTE |
| A07 | Emparelhar TCL, executar D-pad, Back, Home, volume e reconectar | Comandos anteriores continuam operacionais sem alteração de identidade TLS | PENDENTE |
| A08 | Abrir app/solicitar ficha de streaming na TCL, quando instalado | UI informa o comando realmente solicitado; não confunde busca com reprodução confirmada | PENDENTE |
| A09 | Verificar estado de autorização do dispositivo e acesso aos links | Sem bypass de ativação; erros exibidos com honestidade; não logar códigos nem credenciais | PENDENTE |
| A10 | Sair e reabrir AnyTV, testar alternância Wi-Fi/rede e reconexão | Aplicativo mantém estabilidade, dados persistentes e não perde pareamento por regressão | PENDENTE |
| A11 | Verificar canal de atualização V2 sem instalar outro APK | Confirma canal e pacote V2, sem tocar em OTA legado/cert2026 | PENDENTE |

## Matriz B — recursos DEV bloqueados para teste publicado

| ID | Verificação futura após releases aprovados | Estado |
| --- | --- | --- |
| B01 | Pôster original 2:3 sem corte em três colunas em home, gêneros, filtros e Assistidos | BLOQUEADO — código somente DEV |
| B02 | Assistidos com apenas IDs legados não deve declarar lista vazia; IDs persistem | BLOQUEADO — código somente DEV |
| B03 | Recuperação movie/{id}, até 12 consultas por lote; HTTP 400 antigo interrompe sem apagar IDs | BLOQUEADO — endpoint Worker não publicado |
| B04 | HTTP 404 de filme inexistente não impede recuperação de filme válido seguinte | BLOQUEADO — endpoint Worker não publicado |
| B05 | Botão Recuperar mais 12 e persistência de resultados após cancelamento/saída | BLOQUEADO — código somente DEV |
| B06 | Falha inicial de gênero/Cinema termina spinner, mostra erro e Tentar novamente | BLOQUEADO — código somente DEV |

## Evidência e gate de aceite

Para cada teste executado registrar **PASS**, **FAIL** ou **N/A**, com dispositivo, versão, horário e reprodução de erro quando houver. Se FAIL, documentar passos e evidência, sem editar o histórico oficial do arquivo assinado. Um print de UI é suficiente quando demonstra o erro; não exigir vídeo, acesso remoto, logcat ou dados sensíveis sem necessidade.

**Não declarar homologação física sem registros reais**. A CI de contratos, o build unsigned, a assinatura OTA e o readback HTTPS não comprovam uso de celular/TCL. Não mover M08/M09 de IN_QA nem M10/M11 de BACKLOG sem aprovação documentada, e manter F0–F8 `NAO_HOMOLOGADO`. **Progresso canônico: 7/11 = 63%.** Não acionar merge, deploy Worker, assinatura, publicação ou mudança de D1 a partir deste plano.


---

## Adendo 10/11/2026 — matriz LG/TCL de Streaming por serviço (referência code103 DEV)

**Escopo e autoridade temporal:** o título do documento e as Matrizes A/B acima se referem à versão histórica code54. Este adendo registra **somente** o teste da expansão LG e a regressão TCL na revisão **code103/2.9.75 unsigned**, CI [42885090805](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/42885090805) SUCCESS e SHA-256 interno `3974302a050ae7922e8b580cc062e5ab64141eb06f8ad53526df748acac14263`. Esta revisão **não foi assinada nem instalada**; a OTA publicada continua **code102/2.9.74**, HEAD `c52395b67c6fd56777852ea09131e9fcf507617f`. Nenhum item abaixo foi testado em LG real: todos começam **PENDENTE / INCONCLUSIVO**. Não substituir a versão instalada pelo APK unsigned. Usar as regras de aceite do documento canônico `docs/dev-context/DECISIONS.md` (seção "Protocolo de prova LG por serviço").

### Preparação e observações mínimas

- TV **LG webOS** conectada, modelo e versão de firmware; versão exibida no AnyTV, celular Android e data/hora. Não coletar serial, credenciais, cookies, tokens, código de pareamento nem dados de assinaturas.
- Selecionar um **filme** e uma **série** cuja disponibilidade BR esteja corroborada no catálogo, distinguindo disponibilidade de assinatura de instalação do app. Registrar ID TMDB, serviço escolhido e, apenas se aplicável, **tipo** do ID oficial do provedor (sem guardar token).
- Recolher separadamente: **(a)** imagem do logo no cartão e barra, **(b)** presença de painel remoto após tocar Assistir, **(c)** estado `lgLaunchStatus` visível no AnyTV, **(d)** tela de aplicativo efetivamente exibida na LG, **(e)** ficha correta e **(f)** início da reprodução. Uma etapa aprovada **não** prova a seguinte.
- Para cada linha: `case_id`, TV/firmware, app instalado e `appId` retornado, tipo/ID do título, oferta BR (sim/não/desconhecida), tipo/proveniência do ID de conteúdo, estado SSAP (enviado/aceito/rejeitado/sem resposta/não enviado), foto da TV e do celular **sem informações privadas**, vereditos independentes de logo, controle, app, ficha, playback (PASS/FAIL/N/A/INCONCLUSIVO) e observação do desvio.
- Se o serviço não estiver instalado ou o título não estiver no catálogo BR, registrar **N/A justificado** para aquele cenário, sem promover PASS. Sem foto/observação direta da TV, ficha e playback ficam **INCONCLUSIVOS** mesmo com `SSAP=aceito`.

### Casos de teste LG — executar após candidata assinada e autorizada, nunca com unsigned

| Caso | Serviço e situação | Ação esperada do código | Evidência necessária e interpretação | Estado |
| --- | --- | --- | --- | --- |
| LG01 | Inventário de Netflix, YouTube e apps realmente instalados | Listar `lg:<webos-app-id>`, sem duplicar IDs Android/TCL; exibir ícone da LG ou arte oficial do provedor | Captura do app real na TV e imagem do logo nas duas posições; letras no lugar da imagem = FAIL do logo, separado do launch | PENDENTE |
| LG02 | Netflix — filme com P1874 único de 6–8 dígitos e oferta BR confirmada | Solicitar `launchNetflix(contentId)` pinado, sem declarar reprodução; abrir controle remoto; exibir status SSAP | Observar se a LG abre app, ficha **daquele filme** ou reprodução. ACK sozinho prova somente comando aceito | PENDENTE |
| LG03 | Netflix — filme sem ID P1874 válido ou sem oferta BR confirmada | **Não** enviar `launchNetflix`; abrir somente app e controle; explicar fallback | Confirmar que não aparece alegação de ficha aberta nem vídeo iniciado; observar app correto | PENDENTE |
| LG04 | Netflix — série ou episódio | **Não** passar ID de série ao caminho legado `/catalog/titles/movies/`; abrir app+controle | Verificar que série não é anunciada como reproduzida; detalhe/episódio = INCONCLUSIVO sem evidência | PENDENTE |
| LG05 | YouTube — item de filme/série TMDB sem videoId oficial correspondente | Não transformar TMDB ID/título/pesquisa em YouTube videoId; só lançar app instalado | Observar UI e controle; não dizer que o filme completo foi aberto pelo YouTube | PENDENTE |
| LG06 | Prime Video, Disney+, Max, Apple TV+ — app instalado | Lançar somente `appId` webOS recebido; não usar URL Android/web como deep link LG sem contrato específico | Observar app correto + controle; ficha do filme e playback seguem INCONCLUSIVOS até prova independente | PENDENTE |
| LG07 | LG desconectada ou aplicativo ausente | Informar **não enviado** com motivo inteligível e sem expor IDs, tokens ou erros brutos | Screenshot status e nenhuma promessa de abertura; preservar TCL | PENDENTE |
| LG08 | LG conectada mas SSAP rejeita ou não retorna em 10 s | Mostrar **rejeitado** ou **sem resposta**; ignorar callback de solicitação anterior | Reproduzir primeiro caso de forma segura; validar mensagem e ausência de falso PASS | PENDENTE |
| LG09 | LG conectado, SSAP aceita, TV só mostra home do serviço | Mostrar **comando aceito**, nunca "filme reproduzindo" | Screenshot simultâneo status AnyTV + home do serviço na LG: app PASS, ficha FAIL, reprodução FAIL, quando observável | PENDENTE |
| LG10 | TCL já homologável no fluxo original | D-pad, volume, Back, teclado, apps por pacote TCL e complemento TCL permanecem independentes do webOS | Capturas/teste da TCL, sem exigir nem reutilizar ID webOS; paridade visual dos logos e controle | PENDENTE |

### Gates de decisão e registro

1. **Code103 unsigned PASS no CI não autoriza instalação.** A revisão assinada code103 anterior (run 42868252718) **não contém** os reparos e gates mais recentes. Qualquer instalação de QA exigirá candidata *correspondente* ao código atual, assinatura V2 comprovada, compatibilidade de versionCode e autorização explícita, sem promover OTA pública automaticamente.
2. Marcar **PASS do logo** apenas por imagem real na UI (não letra fallback), **PASS do controle** apenas por painel funcional após Assistir, **PASS do app** apenas por tela observada na LG, **PASS da ficha** apenas por título exato visível, **PASS da reprodução** apenas por conteúdo correto executado. `LaunchSession` do ConnectSDK não aprova ficha nem reprodução.
3. Falha em LG não altera o resultado TCL nem seus gates; registrar diagnóstico por marca e por serviço. **Nenhuma homologação F0–F8 ou promoção dos marcos M08–M11** decorre desta documentação.

**Base técnica auditada:** `harimoradiya/androidtvremote@16c68f0bb9d23f929ee8240f105fdb9000262d85`, `ConnectSDK/src/main/java/com/connectsdk/service/WebOSTVService.java` (blob `a10a52b936ce18d2f867c4530063da12b8df2a37`): `launchNetflix` monta caminho legado de filmes; `launchYouTube` exige ID de vídeo; `launchAppWithInfo` usa `ssap://system.launcher/launch` e confirma lançamento, não reprodução.
