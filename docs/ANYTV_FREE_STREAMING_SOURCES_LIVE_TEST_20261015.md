# AnyTV — prova ao vivo de fontes gratuitas (Brasil)

**Data:** 15/10/2026  
**Execução GitHub Actions:** https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38771084522  
**Objetivo:** comparar fontes capazes de descobrir URLs de filmes que abram fichas nos apps Android TV sem exigir uma assinatura de US$ 349/mês.

## Fonte consultada em execução real

Biblioteca MIT **Electronic-Mango/simple-justwatch-python-api 1.4.0**, GraphQL não oficial `https://apis.justwatch.com/graphql`, consulta `country=BR`, `language=pt`, identificação rígida pelo **TMDB ID**. Isso não é uma integração parceira autorizada. Uso exploratório, sem garantia de continuidade ou de licenciamento para distribuição pública.

| Título | TMDB | Oferta brasileira encontrada (tipo FLATRATE) | URL reportada (amostra) |
|---|---|---|---|
| Zootopia 2 (2025) | 1084242 | Disney Plus | `https://www.disneyplus.com/browse/entity-8f08a8df-8b2f-49e1-9130-1e842b90f185` |
| Birdman (2014) | 194662 | HBO Max, canais HBO Max/Cindie no Prime | `https://play.hbomax.com/show/a0a085b8-6657-46ca-9476-6af0070b9cce?utm_source=universal_search` |
| Jurassic Park (1993) | 329 | Prime Video, Apple TV, HBO Max, canais adicionais | `https://app.primevideo.com/detail?gti=amzn1.dv.gti.7ea9f6d9-bdc8-9b2e-97a9-c341306e36ef` |
| O Irlandês (2019) | 398978 | Netflix | `https://www.netflix.com/title/80175798` |
| O Projeto Adam (2022) | 696806 | Netflix | `https://www.netflix.com/title/81309354` |
| Divertida-Mente 2 (2024) | 1022789 | Disney Plus | `https://www.disneyplus.com/browse/entity-ec9f9fa3-fbae-4a25-b722-12c3b8ab0ef4` |

**Resultados da amostra**: 6/6 filmes exatos por TMDB ID e 6/6 com ao menos uma oferta brasileira e URL individual fornecida pelo catálogo. Isso **não** significa 6/6 fichas abertas fisicamente na TCL, nem direitos de reprodução garantidos no perfil do usuário.

**Falha de matching evidenciada e corrigida no teste:** buscar só por nome/ano retornou `Zootopia 2 | A Special Look` e `The Real Jurassic Park` em vez do filme. O script de prova exige o ID TMDB exato.

## APIs comerciais: resultado da chamada real sem chave

- `api.watchmode.com/v1/title/movie-329/sources?regions=BR` → HTTP 401, sem chave.
- `api.movieofthenight.com/v4/shows/tt0107290` → HTTP 401, sem chave.
- Streaming Availability API possui plano gratuito com chave e campos `streamingOptions[].link`; para validação fim-a-fim da resposta real é necessário criar chave gratuita no fornecedor. Documentação: https://docs.movieofthenight.com/guide/authentication
- Watchmode Developer gratuito tem 2.500 créditos, porém não inclui `android_tv_url`; precisa de plano pago. https://api.watchmode.com/

## Limitações e adaptação necessária

- O `TvAppBridge.java` do Connect 2.1 já aceita os formatos Netflix, Disney e Prime GTI, com pacote restrito; alguns links de Apple TV incluem `/br/movie/<slug>/umc...` + query, e Max inclui `?utm_source=...`. **O validador atual os rejeita**. Não remover a segurança indiscriminadamente: permitir somente host, pacote, caminho e parâmetros esperados.
- Link de Birdman no Max usa `/show/` mesmo sendo um filme. Isso pode ser um alias válido ou um erro do provedor — precisa de teste físico antes de considerar o formato aceito.
- Canais agregadores no Prime Video exigem assinaturas adicionais; `FLATRATE` de canal não equivale a assinatura básica Prime.
- Prova sem chave não concede licença de exploração pública ou comercial do JustWatch. A API oficial de parceiros exige contrato e token: https://apis.justwatch.com/docs/api/

## Decisão de engenharia

**ADAPT para protótipo, BLOQUEAR integração pública sem confirmação de direitos.** O experimento comprova que existem URLs reais não dependentes do Wikidata que podem ser usadas como candidatos. Para uma versão distribuível do AnyTV, testar Streaming Availability API (plano gratuito com credencial própria) como fornecedor licenciado e manter validação local + fallback. Não publicar uma nova versão só porque um URL é retornado.

**Próximo teste na TCL:** escolher um filme Netflix (O Irlandês), um Prime (Jurassic Park) e um Disney (Divertida-Mente 2); verificar pacote instalado, resolvabilidade do ACTION_VIEW, ficha correta e possíveis erros de região. Nenhum APK ou `current.apk` foi alterado nesta auditoria.
