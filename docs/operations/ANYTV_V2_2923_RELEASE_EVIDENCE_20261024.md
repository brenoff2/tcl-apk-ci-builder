# AnyTV V2 2.9.23 (code 52) — publicação comprovada em produção

**Data:** 2026-10-24. **Status:** APK V2 assinado e publicado OTA com readback byte-a-byte; Worker atualizado 100%. **QA física na TCL e ativação da instalação V2 ainda NÃO HOMOLOGADAS.**

## Worker Cloudflare

- Serviço: `anytv-link-resolver-pilot`
- Versão anterior/rollback: `ce51439e-ca19-40d3-85e8-019c7a3669a5`, deployment `3298f15b-68d1-455b-bbc1-624925d7ccaf`
- **Nova versão ativa:** `bd8cafd5-2e9a-4f83-88a8-d557fda9ab82`, deployment `5a1d03cf-2525-4619-b34a-22f9d8074506`, **100% do tráfego**, readback pela API oficial
- Upload pela API *versions* sem tráfego, com `bindings_inherit=strict`, antes da promoção; script etag `1f62e59a4a45e6d8940ad60e1c6d82cc9a1d2d903e3048f9b23035c1449601db`
- Cinco bindings preservados (nomes/tipos): `DB` D1 na database UUID `9bab6598-d019-4e88-8b29-a327ff0cfd00`, `ANYTV_RESOLVER_AUTH_MODE=device`, `ANYTV_DEVICE_ACTIVATION_ENABLED=true`, dois segredos `ANYTV_MOVIE_API_KEY` e `TMDB_READ_TOKEN`. Nenhum segredo recuperado/exposto no chat.
- Smoke após deploy: `GET /health` HTTP 200 `ok`; filme Titanic 20 resultados HTTP 200, série Breaking Bad 5 resultados HTTP 200; `/v1/device/status` sem prova HTTP 401 `unauthorized`, `/v1/resolve/movie/329/netflix` sem prova HTTP 401. Auth não foi contornada.
- Nova funcionalidade: status assinado, padronização de URL Netflix/Prime, distinção semântica 404/409, uso seguro de `videoLink`; não afirma reprodução direta na TCL sem observação.
- Dados após liberação: 1 dispositivo P-256 autorizado, 1 código antigo usado; **nenhuma nova emissão, revogação ou alteração de cadastro**.

## APK Android V2

- Pacote: `com.tclremote.livre.v2`; **2.9.23 / versionCode 52**, instalado in-place acima da 2.9.22/code51 com o **mesmo certificado V2**, SHA-256 do certificado `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`.
- Workflow: [GitHub Actions #40086101046](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40086101046) — conclusão **SUCCESS**. Passaram testes Android/IME, consultas TMDB reais, build, recuperação de signer criptografado, verificação de assinatura, publicação OTA, comparação remota dos bytes e artifact final.
- **APK SHA-256:** `6f3274f488803b0d13659080f09c17cdeed5cca68fdcc6f3f4225814d0226094`.
- **Git blob do APK OTA:** `eb7c13f3295eefd01c7f3533af8b86962e948cad`; tamanho `28412772` bytes; commit OTA `4831b5e0c91871c136f546c9b19c95c5497f1b6c`.
- Canal `feat/tcl-v22-updates-back-bluetooth:updates/android/v2/current.apk`; link: https://raw.githubusercontent.com/brenoff2/tcl-apk-ci-builder/feat/tcl-v22-updates-back-bluetooth/updates/android/v2/current.apk .
- Artefato de auditoria assinado: `anytv-v2-2923-signed-update`, ID `12556934242` no workflow, SHA-256 do ZIP de artifact (NÃO do APK) `c697a1991a2b807aa5dbb3df34f5ce67b15a9c84539a828785e7f593b25693b3`.
- OTA legada `updates/android/cert2026/current.apk` foi preservada e não é substituída. Não houve novo assinante, bootstrap nem alteração do companion TV.
- Layout: ano e nota à esquerda/assistido e favorito à direita; linha seguinte F/S e provedores brasileiros; título reservado em 3 linhas; trailer retirado do cartão e mantido na ficha; ícones lista/grade no filtro; estados de visto e favorito.
- Links: diagnóstico de autorização por dispositivo, ativação por código único somente se instalação não autorizada, sem salvar código, fallback seguro TMDB/Wikidata.

## Instalação e QA pendentes

No **AnyTV V2** abrir `Buscar atualizações`. A atualização não deve exigir desinstalação; se o Android solicitar, permitir a instalação por esse app e confirmar o pacote certo. **Não desinstalar** para contornar erro de certificado.

A instalação física, preservação de favoritos, abertura de títulos na TCL, versão real do companion, autenticação da identidade V2 e testes U01-U05/D01-D12 **ainda não foram observados**. O banco D1 possui uma identidade antiga que pode pertencer à outra instalação; verificar `Verificar autorização de links` na ficha do título. Se houver 401, usar somente provisionamento autorizado sem copiar o Keystore e sem expor código secreto.

**Progresso permanece 7/11 = 63%; gates F0–F8 NAO_HOMOLOGADO**. O novo APK ter sido distribuído não é evidência de reprodução ou QA física. Rollback do Worker preservado; APK Android não pode ser simplesmente rebaixado in-place por versionCode, eventual correção deverá ter code maior que 52 sem trocar certificado.

## Decisão GitHub-first

**ADAPT** a infraestrutura já validada de Cloudflare Worker + D1 e o workflow existente de signer/OTA V2, trocando somente a versão para code52. Referência: `docs/qa/ANYTV_V2_POST_2922_UI_LINKS_CANDIDATE.md`, `docs/operations/ANYTV_V2_LINK_RELEASE_READINESS_20261024.md`, documentação oficial Cloudflare de upload de versão e deploy separado.
