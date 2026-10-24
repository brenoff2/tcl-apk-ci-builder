# AnyTV V2 — UI de cartões e links diretos (candidata após 2.9.22)

**Data:** 2026-10-24  
**Estado:** código DEV com UI/links e ativação da V2 **COMPILADO COM SUCESSO**: Android #40082388480 (39 testes de interface, links e ativação + 2 de segurança, Gradle BUILD SUCCESSFUL 4m48s, identidade V2 PASS), resolvedor #40082285375 SUCCESS, prova de dispositivo #40082285396 SUCCESS; companion TV #40060021841 SUCCESS. **NÃO ASSINADO, NÃO PUBLICADO, NÃO HOMOLOGADO** em telefone/TV.  
**Canônico de versão publicada:** 2.9.22/code51, `com.tclremote.livre.v2`.  
**Branch:** `feat/anytv-p0-device-proof-verifier-20261020`; PR #3 draft. Commit funcional de referência: `6a3be3eb61d8d96d2049a43d5823ede8c942b1ae`.  
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

- O D1 real tem **1 dispositivo P-256 autorizado**, mas a identidade da instalação V2 atual ainda NÃO foi comprovada. A ativação registrada foi em 21/10, antes da primeira distribuição documentada da V2 em 23/10; chaves Android Keystore são isoladas por pacote/UID. Não copiar a chave antiga, não emitir outro código sem aprovação.
- Readback REAL do Worker ativo (`ce51439e...`): `ANYTV_RESOLVER_AUTH_MODE=device`, `ANYTV_DEVICE_ACTIVATION_ENABLED=true`, tabelas D1 presentes; **o código implantado ainda não contém** `/v1/device/status` nem `videoLink`. Endpoint de ativação existe, mas o diagnóstico novo exige deploy coordenado aprovado.
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
| L01 | Node tests: normalização Netflix/Prime, `videoLink`, 404/409 com status distinto, 401/5xx e replay | PASS 32/32 #40060118239 |
| L02 | Kotlin/Python contratos e Gradle `assembleRelease` da V2 candidata | PASS 39+2 testes, BUILD SUCCESSFUL (3m28s), identidade V2 PASS #40060106346 |
| L03 | Dispositivo real ativado: status 200; não ativado: 401 com mensagem correta | NAO_TESTADO_EM_APARELHO |
| L04 | Prime, Netflix, Disney, Apple e Max: ficha / busca / app aberto (registrar separadamente) | NAO_TESTADO_NA_TCL |
| L05 | Assinatura V2/OTA e preservação dos dados após eventual release autorizada | NAO_EXECUTADO |
| L06 | Resposta 404 `not_available` diferente de 404 `no_supported_link` e 409 `ambiguous`; fallback TMDB/Wikidata não interrompido por 401/5xx | BACKEND_PASS 32/32 #40060118239 + ANDROID_PASS #40060106346 |
| L07 | Java/TV APK seguro, allowlist compatível e limite 500 caracteres sem Acessibilidade | PASS 7+3 testes e Gradle #40060021841; instalação TCL NAO_TESTADA |
| L08 | Exibir código único apenas após status `unauthorized`, limpar após uso, não persistir, confirmar 201/200 e exibir pacote/sufixo de ID | ANDROID CI PASS #40082388480 (39+2, Gradle 4m48s); endpoint físico 201/200 PENDENTE |
| L09 | Comparar modo/device e cadastro atual no D1, sem expor chaves | READBACK_CF_PASS: 1 autorizado, 1 código consumido; identidade V2 atual NÃO COMPROVADA |

**Evidência nova (readback 24/10):** [resolver offline #40058621968](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40058621968) SUCCESS, 31/31 testes, zero falhas; [prova de dispositivo #40058631392](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40058631392) SUCCESS. [Compilação V2 sem assinatura #40058791096](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40058791096) terminou SUCCESS (38 testes de streaming + 2 de segurança, Gradle `BUILD SUCCESSFUL in 4m 34s`, verificação do pacote V2). O ajuste posterior que mantém fallback TMDB/Wikidata após 401/503/no_supported_link está no commit `b5a969e` e será verificado em execução independente. Nenhum deploy ou OTA.

**Cobertura oficial Movie of the Night Brasil (fonte pública em 24/10):** https://docs.movieofthenight.com/guide/countries-and-services lista `netflix`, `prime`, `disney`, `hbo`, `apple`, `paramount`, `mubi`, `curiosity`, `plutotv`, `crunchyroll`, `zee5`; **não lista Globoplay nem Plex**. Não prometer link direto desses serviços apenas com a API atual; estudar fonte licenciada ou capacidade oficial do app TCL.

**CI TV sem publicação:** link da [candidata do companion #40059722774](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40059722774); valida URLs oficiais Prime/Netflix, permissões, compilação, identidade do TV companion. Estado final SUCCESS; não instala no aparelho.

**Conferência complementar de 24/10:** [resolvedor #40060118239](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40060118239) SUCCESS 32/32 com semântica HTTP 404/409; [companion Java #40060021841](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40060021841) SUCCESS (7+3 testes, compilação, pacote e permissões). O cliente Android passou a interpretar o campo `status` no corpo do erro HTTP e a preservar a consulta alternativa sem bypass da autorização. [Última compilação Android #40060106346](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40060106346) SUCCESS: 39+2 testes, `BUILD SUCCESSFUL in 3m 28s`, identidade V2 verificada sem assinatura/deploy. Atribuição da Movie of the Night adicionada ao diálogo de fontes conforme termos oficiais. Sem produção/OTA/companion instalados.

**Investigação adicional de 24/10:** o código real da versão em produção foi inspecionado via Cloudflare (sem expor segredos), assim como o D1 e os cinco bindings; os resultados estão em [ativação individual V2](ANYTV_V2_DEVICE_ACTIVATION_RUNBOOK.md) e [pré-liberação coordenada](../operations/ANYTV_V2_LINK_RELEASE_READINESS_20261024.md). O aplicativo da branch passa a oferecer somente **código único de uso único** em caso de dispositivo não autorizado; nenhum token de provedor e nenhum cadastro automático. A política do emissor foi alinhada a 365 dias (sem quota de requisições), mantendo código de 10 minutos e revogação possível. **Emissão e uso real continuam NÃO EXECUTADOS.**

**Fechamento de CI da ativação:** [Android V2 sem assinatura #40082388480](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40082388480) SUCCESS, 39+2 testes, `BUILD SUCCESSFUL in 4m 48s`, `V2_UNSIGNED_COMPILE_SUCCESS`; [resolver offline #40082285375](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40082285375) SUCCESS e [device-proof #40082285396](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40082285396) SUCCESS. Nenhum token manual de API, nenhum código de ativação gerado/gravado em produção, nenhuma distribuição. Ver [procedimento de ativação](ANYTV_V2_DEVICE_ACTIVATION_RUNBOOK.md) e [portas de release](../operations/ANYTV_V2_LINK_RELEASE_READINESS_20261024.md).

**Critério de parada:** não publicar APK, não fazer deploy de Worker, não atualizar companion, não mudar signer/OTA nem fazer merge sem autorização específica e evidência de build/testes. Se o CI falhar, investigar e corrigir somente a causa.
