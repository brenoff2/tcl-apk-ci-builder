# ADR-0002 — Discovery Home V3 como entrada padrão
**Estado:** APROVADA NA INTENÇÃO DE PRODUTO; implementação em QA, sem autorização de publicação cega. **Data:** 21/10/2026.

## Decisão
A entrada Streaming vira **Descobrir**: home editorial global de filmes e séries, independente do serviço. Plataforma é filtro opcional, sem impedir busca global; cada streaming mantém sua página específica. Interfaces incluem hero de **slides horizontais**, carrosséis por categoria, destaques, lançamentos, séries, populares, avaliações, sugestões, trailer, ficha completa, relacionados e botão Abrir na TCL.

## Fundamentação / definições confiáveis
O usuário confirmou que praticamente todas as funcionalidades atuais funcionam e que a abertura de títulos **já existia antes** da ativação. Não refazer controle, Bluetooth, LAN, apps instalados, pareamento nem deep links que funcionam.
- `Em alta` = tendência TMDB; `Populares` = métrica TMDB, **não** audiência real; `Mais bem avaliados` = nota TMDB + quantidade de votos; `Para você` requer sinais de preferência e consentimento (senão sugestões genéricas).
- Trailers somente da fonte de vídeo verificada, pelo gesto explícito do usuário; fallback pt-BR/en-US; sem autoplay.
- Disponibilidade BR distingue streaming em assinatura, aluguel e compra; app instalado ≠ assinatura ativa ou título disponível.
- Links diretos seguem controle seguro do Worker/D1 com prova de dispositivo; nenhum bypass de DRM.
- Slides deslizáveis manualmente, acessíveis, backdrops panorâmicos TMDB com poster como alternativa; retorno preserva contexto.

## Alternativas rejeitadas
- Home começando pela escolha obrigatória de um provedor.
- Somente página genérica sem página própria de streaming.
- Rankings fabricados, rótulo falso de ‘mais assistidos’, trailer que reproduz sem ação e filmes presumidos disponíveis sem fonte.
- Reescrita integral do projeto, troca de assinatura, instalação sem verificações ou distribuição do APK unsigned.

## Critérios de aceite
1. Home abre sem selecionar serviço; pesquisa retorna filmes e séries de mais de uma plataforma.
2. Slides e carrosséis funcionam por toque/deslize; notas atribuídas ao TMDB; trailers opcionais; relacionados verificáveis.
3. Filtro por serviço altera a descoberta sem restringir busca global; página de cada plataforma preservada.
4. TV abre título elegível via fluxo existente, tratamento honesto dos casos sem deep link.
5. CI, segurança da proxy, cota/cache e APK assinado e de versão superior; QA física, sem regressões.

## Referências
`docs/ANYTV_DISCOVERY_HOME_V3.md`; `overrides/TclStreamingScreens.kt`; `anytv-resolver/src/worker.mjs`.
