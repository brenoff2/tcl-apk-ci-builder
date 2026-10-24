# AnyTV V2 — UI de cartões e links diretos (candidata após 2.9.22)

**Data:** 2026-10-24  
**Estado:** IMPLEMENTADO SOMENTE EM CÓDIGO DEV. **NÃO COMPILADO, NÃO PUBLICADO, NÃO HOMOLOGADO** nesta etapa.  
**Canônico de versão publicada:** 2.9.22/code51, `com.tclremote.livre.v2`.  
**Branch:** `feat/anytv-p0-device-proof-verifier-20261020`; PR #3 draft. Commit funcional de referência: `e7bc977cd9623af398323924d2e450fd0dd4ec61`.  
**Progresso P0:** 7/11 marcos, 63%; gates F0–F8 continuam NAO_HOMOLOGADO.

## Pedido exato de interface (aprovado)

Preservar o intervalo entre cartões, colunas da home de 116dp/9dp, duas fileiras horizontais e área do título de três linhas/42dp. Na página de gênero (grade 3, lista) e nos cartões da home:

1. Sob o cartaz, primeira linha: ano e nota à **esquerda**; assistido/acompanhamento e favorito à **direita**.
2. Segunda linha: marcador F azul/S vermelho seguido dos ícones das plataformas com disponibilidade BR confirmada; **nota não pode aparecer novamente**.
3. Terceira linha: título com espaço fixo de **três linhas**. Não reduzir o espaço entre os cartões.
4. Remover ícone/botão de trailer dos cartões; **"Ver trailer" continua na ficha do título**.
5. Coração sempre preenchido: cinza normal, vermelho favoritado; assistido/acompanhamento cinza normal e verde marcado.
6. Gênero usa seletores compactos por ícones de lista e grade 3, seguidos do seletor de ordenação existente.
7. Na home, preservar a visualização do cartão recém-marcado para que a indicação verde apareça imediatamente. A tela Assistidos continua disponível.

## Alterações de links (somente código candidato)

- `anytv-resolver/src/worker.mjs`: `GET /v1/device/status`, autorizado somente com prova criptográfica individual; HTTP 200 `activated` ou 401 `unauthorized`. Nenhum bypass, quota ou segredo em cliente.
- `overrides/AnyTvPersonalLinks.kt`: consulta de status, distinção de HTTP 401 e 503, URLs Prime Video por GTI ou ID de catálogo oficial, normalização Netflix com barra final.
- `overrides/TclStreamingScreens.kt`: diagnóstico de autorização acessível dentro da ficha do título; erros 401/503 não são mais ocultados. O fluxo de busca convencional permanece para indisponibilidade do resolvedor.
- `overrides/tv_safe/TvAppBridge.java`: aceitação estrita dos mesmos padrões oficiais Prime/Netflix, sem permissões genéricas.
- `worker.mjs`: preferir `videoLink` do fornecedor quando compatível com a allowlist; se não, usar `link`. URLs continuam candidatos e **não representam sucesso de reprodução**.

### Limitações existentes, não solucionadas por código apenas

- Autorização individual efetiva do celular no D1 **não comprovada**. A emissão de código de ativação e sua aplicação dependem de fluxo de provisionamento autorizado; não embutir token/API key em APK, não cadastrar automaticamente sem prova.
- Worker atual de produção (`ce51439e...`) **não recebeu** o endpoint/código novos; a leitura atual de produção não foi realizada neste ciclo.
- Companion TV **não publicado** nesta etapa. Versão instalada e suporte a `/title-open` na TCL real não verificados.
- Links oficiais e intents devem ser comprovados serviço por serviço na TCL; não declarar reprodução quando `startActivity` apenas aceitou um pedido.
- Sem novo provedor comercial integrado: Watchmode/JustWatch oficiais demandam contrato/planos e verificação de cobertura. Movie of the Night continua a fonte atual.

## Verificações a realizar antes de publicar

| ID | Critério | Estado |
|---|---|---|
| U01 | Grade Ação com 3 linhas e ícones reorganizados, títulos longos alinhados | NAO_TESTADO_EM_APARELHO |
| U02 | Lista detalhada com metadados acima do título e sinopse preservada | NAO_TESTADO_EM_APARELHO |
| U03 | Home em duas fileiras, estado vermelho/verde persistente e sem trailer nos cartões | NAO_TESTADO_EM_APARELHO |
| U04 | Alternância ícones lista/grade, ordenação e paginação sem regressão | NAO_TESTADO_EM_APARELHO |
| U05 | Trailer presente e funcional somente na ficha | NAO_TESTADO_EM_APARELHO |
| L01 | Node tests: normalização Netflix/Prime, `videoLink` seguro, 401/503 e replay | PASS (31/31) #40058621968 |
| L02 | Kotlin/Python contratos e Gradle `assembleRelease` para V2 candidata | CONTRATOS_PASS; GRADLE_PENDENTE #40058791096 |
| L03 | Dispositivo real ativado: status 200; não ativado: 401 com mensagem correta | NAO_TESTADO_EM_APARELHO |
| L04 | Prime, Netflix, Disney, Apple e Max: ficha / busca / app aberto (registrar separadamente) | NAO_TESTADO_NA_TCL |
| L05 | Assinatura V2/OTA e preservação dos dados após eventual release autorizada | NAO_EXECUTADO |

**Evidência nova (readback 24/10):** [resolver offline #40058621968](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40058621968) SUCCESS, 31/31 testes, zero falhas; [prova de dispositivo #40058631392](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40058631392) SUCCESS. [Compilação V2 sem assinatura #40058791096](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40058791096) iniciou; testes de contrato passaram, conclusão Gradle ainda não verificada neste registro. Nenhum deploy ou OTA.

**Critério de parada:** não publicar APK, não fazer deploy de Worker, não atualizar companion, não mudar signer/OTA nem fazer merge sem autorização específica e evidência de build/testes. Se o CI falhar, investigar e corrigir somente a causa.
