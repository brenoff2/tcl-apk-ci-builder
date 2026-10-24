# AnyTV V2 — liberação coordenada dos links (pré-produção)

**Data:** 2026-10-24. **Decisão:** ADAPT o resolvedor e a ativação existentes; NÃO criar novo serviço nem eliminar autenticação. **Estado:** PR #3 draft, homologação física pendente, execução de produção **não autorizada neste ciclo**.

## Readback direto Cloudflare (somente leitura)

- Worker `anytv-link-resolver-pilot`; deployment ativo `3298f15b-68d1-455b-bbc1-624925d7ccaf`, versão `ce51439e-ca19-40d3-85e8-019c7a3669a5`, **100%** do tráfego.
- Cinco bindings preservados: `ANYTV_DEVICE_ACTIVATION_ENABLED=true` (plain_text), `ANYTV_RESOLVER_AUTH_MODE=device` (plain_text), `ANYTV_MOVIE_API_KEY` (secret), `DB` (D1), `TMDB_READ_TOKEN` (secret). **Nenhum valor de segredo foi obtido/publicado.**
- Fonte executável lida pelo endpoint oficial **ainda não contém** `/v1/device/status` e **não** prioriza `videoLink`. Essas alterações são exclusivas da candidata do GitHub.
- `/health` via HTTP 200; `/v1/device/status` e `/v1/resolve/movie/329/netflix` **sem prova válida** retornaram HTTP 401. Esse teste negativo não atesta a identidade do aparelho.
- D1 canônico (mesma database UUID do `wrangler.jsonc`): `device_keys`, `device_nonces`, `device_activations` existem; 1 dispositivo autorizado em P-256, 1 código já consumido, nenhum código válido não usado. Última ativação `2026-10-21T22:17:55Z`, última prova aceita `2026-10-21T22:39:40Z`.
- A primeira publicação documentada da linha V2 2.9.18 ocorreu após esse cadastro (workflow #40029572648 em `2026-10-23T21:49:59Z`). Como o Android Keystore é separado por app/UID, há **hipótese forte**, mas **não prova física**, de que o registro é de instalação anterior; não reutilizar ou substituir identidade por inferência.

## Mudanças prontas somente em DEV

- Layout solicitado: ano/nota à esquerda, assistido/favorito à direita, F/S+serviços na segunda linha, título em espaço fixo de três linhas; sem trailer no cartão; seletores ícones lista/grade.
- Resolvedor: distinção correta de HTTP 404 `not_available` vs `no_supported_link`, HTTP 409 `ambiguous`, 401 e 5xx; URLs oficiais Netflix/Prime; `videoLink` seguro com allowlist; `GET /v1/device/status` assinado.
- Android: fallback de pesquisa da TCL preservado; diagnóstico com `packageName` e últimos 8 do device ID obtidos fora da UI thread; ativação por código único **apenas após status não autorizado**, sem salvar código e confirmando status após HTTP 201. A ativação usa função criptográfica existente, não uma chave de API.
- Emissor operador: `anytv-resolver/scripts/issue-activation.mjs` preexistente, ajustado para validade anual conforme D1. Emite 256 bits/43 chars (apenas terminal privado), código com 10 minutos de uso; armazena-se somente hash em D1 após aprovação. **Nenhum novo código foi gerado ou provisionado nesta execução.**
- Companion TCL: alterações de formatos de links já compiladas sem assinatura, não instaladas.

## Sequência segura para liberação, SOMENTE APÓS APROVAÇÃO ESPECÍFICA

1. Revisar CI **do SHA funcional mais novo**: Android V2 Kotlin+contratos, resolver Node+SQLite+dry-run, companion Java/Gradle; confirmar clean CI e registrar SHA dos três artefatos.
2. Capturar readback/rollback pré-deploy do Worker (versão `ce51439e...`, cinco bindings e banco correto), sem dump de segredos nem D1 privado. Verificar que a candidata não reintroduz quotas internas.
3. Implantar **Worker primeiro**, preservando integralmente as cinco bindings e com rollback explícito para `ce51439e...`; após o deploy conferir versão 100%, saúde/catalogo, 401 sem prova, endpoint de status, sem exposição de tokens. Não autorizar bypass.
4. Preparar uma única V2 Android **com versionCode superior a 51** e mesmo signer V2, verificar certificado e arquivos finais; publicação OTA somente em canal `updates/android/v2`, preservando legado. **Não usar APK unsigned.**
5. No telefone real, verificar status da instalação V2. Se houver 401 e a identidade não corresponder ao D1 autorizado, emitir apenas **um** código de ativação aprovado e confirmar 201/200 pelo aparelho; manter o registro anterior intacto.
6. Na TCL, testar cinco plataformas separando ficha exata, busca, aplicativo apenas aberto e reprodução. Avaliar atualização do companion só se o instalado não suportar a função comprovadamente necessária.
7. Homologar layout U01–U05 e D01–D12 (com evidência); promover marcos F0–F8 e progresso **somente após comprovação real**.

## Porta de parada

**Não executar os passos de produção apenas com CI verde.** Sem liberação específica e prova física: **APK público 2.9.22/code51**, Worker `ce51439e`, companion existente, D1 e OTA intactos. Progresso P0 **7/11 = 63%**, F0–F8 `NAO_HOMOLOGADO`.

Documentos relacionados: `docs/qa/ANYTV_V2_DEVICE_ACTIVATION_RUNBOOK.md`, `docs/qa/ANYTV_V2_POST_2922_UI_LINKS_CANDIDATE.md`, `docs/roadmap/PROJECT_PROGRESS.json`.
