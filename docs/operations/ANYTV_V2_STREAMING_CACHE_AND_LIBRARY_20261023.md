# AnyTV V2 — Cache, Minha Lista e episódios (code45)

Data: 2026-10-23. Fonte: `overrides/TclStreamingScreens.kt`. **Estado:** implementado e em homologação de APK 2.9.16; considerar efetivamente publicado apenas após GitHub Actions SUCCESS e readback OTA.

## Cache local com economia de franquia

- Armazena exclusivamente respostas públicas do catálogo TMDB mediadas pelo Worker, sem credencial do TMDB, segredo Cloudflare ou código individual de ativação.
- `StreamingDiskCache`: `context.filesDir/anytv_catalog_v1`, nomes SHA-256 da rota, JSON com validade, limite 450 arquivos/12MB e descarte aproximado LRU usando última modificação.
- Respostas de gêneros até 30 dias; catálogo e descoberta até 12 h; em alta/estreias até 6 h; disponibilidade por assinatura BR até 12 h; temporadas/episódios até 7 dias; metadados individuais de títulos até 90 dias.
- Em consultas repetidas, leitura local evita chamar Worker/TMDB enquanto válida. Cache de disponibilidade nunca promete a posse de assinatura ou reprodução direta. Não usar respostas expiradas como prova de licenciamento.
- A memória de listas se baseia em media/id TMDB, não no texto dos nomes. IDs de títulos individuais guardam sinopse, data e paths de imagem quando apresentados, mesmo que a lista editorial expire.
- Atribuição TMDB/JustWatch preservada. Cache de imagens via Coil do app, sem armazenar imagens de forma manual.

## Meus streamings e Minha Lista

- O usuário escolhe **seus serviços assinados** dentre apps realmente encontrados na TCL. Não inferir assinatura apenas por aplicativo instalado.
- Preferência `anytv_my_streamings` e filtro `Só meus streamings` são locais; serviços da lista geram seções reais `Destaques ... Filmes/Séries` usando `discover` com provider TMDB BR e monetização `flatrate`.
- Sem filtro, a descoberta global e seções por gênero continuam, acrescidas das plataformas selecionadas. Seletor de gêneros existente preservado.
- Seções têm eliminação de repetição por `media:id`, inclusive títulos já ocupando slides destacados.
- `Minha lista`: títulos marcados de qualquer origem, inclusive busca global sem streaming selecionado. Separação entre filmes para ver, já vistos e séries acompanhadas.
- Persistência de favoritos e filmes vistos em `SharedPreferences`; evolução da data de estreia por campo adicional compatível com preferências anteriores.
- Registros por episódio: conjunto de chaves `idDaSerie:temporada:episodio` na preferência `anytv_watched_episodes`. Sem upload de hábitos para servidor.
- Limitação: dados em SharedPreferences não são sincronizados entre celulares e podem ser perdidos ao desinstalar/limpar dados (a instalação OTA no mesmo package/cert os mantém).

## Temporadas automáticas: gate separado

- A API TMDB oficial tem `tv/{id}` e `tv/{id}/season/{n}`. O código em `anytv-resolver/src/worker.mjs` inclui duas novas rotas **somente leitura** com limites de número, mas a atualização do Worker ativo foi bloqueada pela ferramenta Cloudflare como `WRITE_CONFIRMATION_REQUIRED`.
- Não declarar essas rotas publicadas. O aplicativo cliente dispõe de **fallback manual** de temporada/número do episódio; não inventa nomes ou contagens de episódios quando API retorna erro.
- Depois de acesso explicitamente autorizado de deploy com preservação de TODOS os bindings e segredos existentes, publicar Worker por rota governada, validar 200/limites/error codes e homologar UI com nomes reais. Não contornar restrição nem embutir token no APK.
- Não alterar a assinatura V2, rota OTA nem integração TCL.

## QA

- Testes de contrato `scripts/tests/test_streaming_screens_contract.py`, build Gradle, regressões P0, verificação `apksigner`, versionCode 45, package `com.tclremote.livre.v2`, readback byte a byte do APK remoto e teste visual físico.
- Uma falha de testes de contrato em run `40013843855` foi corrigida em `8d4805b`; rerun `40014131929` é a execução destinada a validar o novo artefato. Não usar o run anterior como comprovante de publicação.
