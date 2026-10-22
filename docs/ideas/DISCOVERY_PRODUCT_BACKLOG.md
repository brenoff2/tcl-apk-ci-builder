# AnyTV — ideias e backlog priorizado (21/10/2026)
**Fonte:** decisões expressas do usuário + desdobramentos técnicos, sem confundir sugestão com aprovação ou entrega. O usuário pediu rapidez e uma atualização completa, não dezenas de APKs parciais.

| Prioridade | Origem | Ideia / decisão | Situação |
|---|---|---|---|
| P0 | Usuário | Entrada Descobrir global, streaming como filtro | Aprovado em conceito; UI implementada em branch |
| P0 | Usuário | Slides no destaque principal e carrosséis horizontais | Aprovado em conceito; Compose em branch |
| P0 | Usuário | Destaques do momento, lançamentos e séries | Aprovado; classificação por fontes TMDB |
| P0 | Usuário | Notas dos títulos | Aprovado; nota TMDB + votos no código |
| P0 | Usuário | Trailer de filme ou série | Aprovado; YouTube oficial quando existir, pt-BR/en-US, sem autoplay |
| P0 | Usuário | Títulos relacionados ao abrir detalhes | Aprovado; recomendações TMDB em branch |
| P0 | Usuário | Populares, melhores avaliações, indicações | Aprovado; popularidade ≠ audiência, sugestões sem alegar perfil inexistente |
| P0 | Usuário | Página própria de cada serviço além do catálogo geral | Aprovado; preservar apps reais da TV |
| P0 | Usuário | Entregar uma única atualização funcional | Prioridade de release; dependente de build, assinatura e QA |
| P1 | Proposta técnica | Favoritos e histórico para recomendações locais com consentimento | Não aprovado/sem implantação |
| P1 | Proposta técnica | 'Surpreenda-me', filtros por gênero/ano/nota, continuar descoberta | Backlog não aprovado |
| P1 | Proposta técnica | Slides acessíveis (TalkBack, foco, redução de movimento) | Critério recomendado |
| P1 | Proposta técnica | Cache offline com carimbo de atualização | Backlog condicionado a API/cache |
| P2 | Proposta técnica | Indicadores personalizados por plataformas assinadas | Exige preferências explícitas; não presumir assinatura |

## Não regredir
Controle remoto Android/TCL, comunicação LAN/pareamento de oito dígitos, Bluetooth, apps TV realmente instalados, favoritos, busca/abertura de títulos preexistente, ativação por dispositivo e mecanismo OTA.
