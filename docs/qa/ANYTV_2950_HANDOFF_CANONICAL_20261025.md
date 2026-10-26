# ANYTV — TRANSIÇÃO CANÔNICA / CONTINUIDADE — 25/10/2026

> **Estado comprovado no GitHub:** AnyTV **2.9.50 / versionCode 79** assinado e publicado na OTA V2. GitHub Actions [#40250636867](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867) **completed/success**; artefato [anytv-v2-2950-signed-update](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867/artifacts/12621265708), ID `12621265708`; commit OTA `7ef4e20b6b0e9a414455d8c1babf10664311ea1e`. **Trakt preparado mas NÃO ativo; Watchmode não integrado; QA física/editorial não homologada**. Este documento revoga qualquer referência histórica que identifique 2.9.49 ou versões anteriores como release atual.

## Objetivo de produto

AnyTV para Android/TCL: transformar a exploração de filmes e séries de catálogos genéricos do TMDB em recomendações editoriais realmente interessantes, recentes, personalizadas, diversificadas e disponíveis no Brasil quando a página se refere a um streaming específico. Preservar identidade visual aprovada, streaming/controle remoto, favoritos, assistidos, acompanhamento, Cinema e Anime isolados, provedor BR e OTA V2.

O usuário não gostou das seleções anteriores: muita coisa antiga nos destaques; poucas estreias; mesmas obras em várias seções; seções por streaming não comparáveis à Principal; interface de gênero/detalhes com erros repetidos. A decisão NÃO é apenas aumentar chamadas de API ou trocar nomes de seções: fazer curadoria útil, com evidências de relevância, confiança e recência, sem títulos inventados.

## Decisões editoriais aprovadas

1. **Até 50 títulos reais por seção**, com os melhores primeiro e exploração progressiva. 50 é teto/alvo, nunca preenchimento artificial se menos candidatos forem elegíveis.
2. **Personalização 40/35/25 como ponto inicial**: aproximadamente 40% gêneros escolhidos explicitamente e inferidos dos favoritos; 35% títulos bem avaliados/tendências gerais; 25% descobertas variadas fora da bolha. Pesos variam por seção: novidades priorizam data comprovada. Não confundir fórmula/quotas de seleção com métricas reais de audiência.
3. Principal e páginas individuais de cada serviço devem usar **um motor editorial comum**, mas o serviço selecionado restringe o catálogo a disponibilidade BR `flatrate` daquele provedor; adicional/canal, aluguel e compra não significam inclusão na assinatura básica.
4. Repetir grupos de descobertas relevantes na Principal e nos Streamings: Destaques da semana, Filmes recém-lançados, Séries novas, Novas temporadas, Em alta agora, Recomendados para você, Você pode gostar, Surpresas do catálogo, Vale a pena assistir e (quando comprovado) Minisséries imperdíveis. Em breve como linha separada; labels somente com significado factual.
5. **Não alegar “mais assistidos” sem audiência real**. “Populares no TMDB” é possível, devidamente rotulado.
6. Evitar monopolização por clássicos nos destaques: priorizar atualidade para destaque/novidade, preservar clássicos em seções adequadas. Aplicar qualidade ajustada pela quantidade de avaliações, diversidade de gêneros/franquias e deduplicação/editorial entre primeiras seções quando viável.
7. Filmes recém-lançados: estreia original em até 90 dias, sem confundir com `movie/now_playing` BR (Cinema exclusivo). Séries novas: primeira estreia da série em até 90 dias; novas temporadas: temporada posterior com data recente comprovada. Não confundir presença na plataforma com data de ingresso.
8. **Recém-chegados ao Streaming**: seção foi explicitamente RETIRADA, não reintroduzir. **Anime** (animação japonesa classificada) e **Cinema** têm páginas exclusivas e não devem contaminar recomendações gerais.
9. Gêneros abrem em **grade**; Ver em seções usa os mesmos trilhos horizontais editoriais da página Streaming; visualização Detalhada compacta título, gênero, ano/badges (sem padding de fonte extra) e preserva espaço **antes** dos botões Trailer/Mais detalhes.
10. Identidade visual: slide sem F/S solto; apenas F em quadrado azul e S em círculo vermelho, selo Cinema sem duplicação, título com gradiente/sombra e ano. Não recolocar marca indevida no topo das telas internas.

## Arquitetura e fontes

- App `overrides/TclStreamingScreens.kt` (Kotlin/Compose); tests `scripts/tests/test_streaming_screens_contract.py`.
- Cloudflare Worker `anytv-resolver/src/worker.mjs`: catálogo TMDB proxied com allowlist de rotas e parâmetros; não mandar parâmetros não permitidos (`with_original_language`, `first_air_date.gte`, `primary_release_date.gte` etc deram HTTP 400 no passado). Consultas válidas por `discover`, `trending/day`, `trending/week`, popularidade, `watch/providers`, seasons e outros endpoints permitidos. Validar rotas no Worker LIVE.
- **TMDB** mantém catálogo principal; disponibilidade brasileira é lida do TMDB/JustWatch conforme provedor `flatrate`. Não declarar que esse dado prova cada temporada/episódio.
- **Trakt Standard**: adaptador de sinais de interesse/trending por TMDB ID preparado no Worker na rota `/v1/catalog/signals/trakt/{movie|tv}`, com testes Node reportados PASS na documentação. **Ainda NÃO ATIVO**: falta Trakt Client ID no Worker, deploy governado, testes HTTP live e conferência de política/atribuição. Nunca pôr Client ID ou segredo indevido no APK. Não fazer scraping nem espelhamento massivo; consultar apenas sinais permitidos. Não confundir com catálogo de provedores.
- **Watchmode**: pesquisado como API complementar para lançamentos e disponibilidade; **NÃO INTEGRADO**. Plano gratuito não autoriza presumir uso comercial nem cobrir necessidades pagas; avaliar licença, preço e valor antes de qualquer implementação com custo. Não contratar nem ativar pagamento sem autorização específica.
- A versão **2.9.50/code79** já contém motor editorial local com gêneros declarados/favoritos, score de qualidade com confiança, recência e descoberta, seleção de até 50, fontes `trending/day`/semana e pools de TMDB, e seções da Principal e do streaming. Conferir comportamento real e código antes de complementar. **Não voltar à hipótese de que tudo ainda precisa ser iniciado.**
- Sem Trakt ativo, mecanismo deve funcionar somente com TMDB, com degradação honesta.

## Releases e auditoria

| Versão | Evidência |
|---|---|
| 2.9.45/code74 | [workflow #40239875487](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40239875487), Anime/novidades ajustados, mas resultados inadequados em uso |
| 2.9.48/code77 | [workflow #40241733615](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40241733615), botão/linhas/indicadores e tentativa de novas seleções |
| 2.9.49/code78 | [workflow #40243533849](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40243533849), remove Recém-chegados, Gêneros grade, descoberta mais larga e linha detalhada |
| **2.9.50/code79** | **[workflow #40250636867](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867) completed/success**, [artifact #12621265708](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40250636867/artifacts/12621265708), OTA commit `7ef4e20b6b0e9a414455d8c1babf10664311ea1e`. |

Canal OTA V2 `feat/tcl-v22-updates-back-bluetooth`, arquivo `updates/android/v2/current.apk`. **NUNCA** tocar na OTA legada `updates/android/cert2026/current.apk`. Preservar pacote `com.tclremote.livre.v2`, signer V2, updater, compatibilidade do controle remoto e separação dos canais. Para próxima release, **versionCode >79**. O workflow oficial é `.github/workflows/release-anytv-phone-v2-new-package.yml` e gatilho declarativo `.github/anytv-phone-v2-build-request.json`. Não iniciar release para simples documento de continuidade.

## Estado exato e pendências (não esconder)

**FEITO / COMPROVADO no repositório:** commits de curadoria e publicação 2.9.50; CI, APK assinado e readback OTA bem-sucedidos; documentos `docs/dev-context/STATUS.md` e `docs/dev-context/DECISIONS.md` registram release e decisões; código contém `anyTvCurate` e rota/cliente opcional de sinais Trakt.

**NÃO FEITO / NÃO COMPROVADO:** Trakt Client ID e Worker live não ativados, nenhuma prova da resposta live Trakt; Watchmode não integrado; validação visual e de comportamento no celular/TCL não realizada; auditoria qualitativa dos **50 títulos reais por seção** não feita; contagem por seção, recência, disponibilidade e duplicação entre Principal/Netflix/Prime/Disney+/Max não auditadas; eventual taxa/contratos de API externos não aprovados. Não afirmar satisfação editorial ou que 50 títulos reais aparecem necessariamente.

**Investigação prioritária para próximo chat:**
1. Readback do HEAD GitHub e do WORKER realmente publicado, comparar contrato e implementação. Conferir PR #3 Draft e não fazer merge sem aprovação.
2. Extrair amostras reais de cada seção (Principal e pelo menos 3 serviços) com nome, TMDB ID, tipo, data, notas, votos, gêneros, fonte e provedor BR; medir quantidade, idade mediana, percentuais 0-90/91-365/>365 dias, duplicação transversal e preferências 40/35/25. Não inventar amostra.
3. Confirmar minisséries por metadado, novas temporadas por data de início da temporada, novidades por estreia correta e providência quando Worker devolve poucos dados.
4. Verificar efetividade dos 50 títulos e do botão Ver mais, persistência de favoritos/assistidos e ausência de Anime/Cinema fora de seus canais.
5. Trakt: verificar documentação oficial atual, termos e limites; obter Client ID por processo legítimo, configurar somente Worker após aprovação, rodar testes e readback sem expor credenciais. Se falta acesso, manter adaptador desligado.
6. Watchmode: avaliar custo/uso comercial; sem autorização, manter fora da execução; sem compras.
7. Corrigir falhas com testes de dados reais e testes de interface, e só então gerar nova OTA code>79, com assinatura, CI, Worker live e readback por commit SHA.

## Fonte da verdade e instruções de retomada

- Repo: https://github.com/brenoff2/tcl-apk-ci-builder
- Dev: `feat/anytv-p0-device-proof-verifier-20261020`
- PR Draft #3: https://github.com/brenoff2/tcl-apk-ci-builder/pull/3
- Canonical root status: `docs/dev-context/STATUS.md`
- Product decisions: `docs/dev-context/DECISIONS.md`
- Code map (histórico, checar hashes): `docs/dev-context/CODEBASE_MAP.md`
- Historical checkpoint: `docs/qa/ANYTV_2939_HANDOFF_CANONICAL_20261025.md`
- **Este checkpoint atualizado:** `docs/qa/ANYTV_2950_HANDOFF_CANONICAL_20261025.md`

Regra de leitura: cabeçalhos históricos que dizem “última release 2.9.39/48/49” NÃO prevalecem sobre o registro mais recente **2.9.50/code79**. Diferenças temporais no GitHub devem ser resolvidas por leitura fresca de Actions, branch e artefato antes de agir.

O usuário valoriza entregas práticas, continuidade sem repetição de “continue”, arquivos de código GitHub como fonte canônica, pouca cerimônia, separação entre projetos e sem publicar alterações falsas ou quebrar o aplicativo. Agradecimentos e atualizações genéricas não substituem execução. Não afirmar homologação de dispositivo por passar CI.
