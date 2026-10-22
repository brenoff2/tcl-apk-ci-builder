# AnyTV — Processo evolutivo de melhoria continua

O método deve ser mutante e aberto a novas ferramentas, mas não pode mudar a rota de programacao ou release silenciosamente.

## Como identificar melhorias

- Na retomada: conferir estado, branch, mapa, erros conhecidos e lacunas de contexto.
- Durante o desenvolvimento: registrar problemas comprovados, testes circulares, regressões e alternativas atuais de engenharia.
- Na conclusao: documentar aprendizado e, quando realmente relevante, propor melhoria.
- Nao interromper reparos urgentes para pesquisar ferramentas sem evidencia de benefício.

## Fila unica e estados

GitHub Issues, com o template `ai-development-improvement.md`, e a fila de propostas. Cada issue inicia como CANDIDATA. Estados possíveis: CANDIDATA, EM_ANALISE, AGUARDANDO_APROVACAO, APROVADA, EM_IMPLEMENTACAO, VALIDADA, REJEITADA e SUSPENSA.

Toda proposta contem: evidencia, comparacao de alternativas, ganho mensuravel, arquivos ou integrações envolvidos, riscos de seguranca, custo, criterio de aceite e rollback.

Mudancas de rota, arquitetura, segredos, permissões, ferramentas externas, assinatura e producao exigem autorizacao expressa. A IA nao transforma uma sugestao em decisao aprovada.

## Indicadores

Medir acerto de branch/SHA em novo chat, tempo de retomada, regressões, tentativas ate causa raiz e conclusoes sem teste. Comparar com linha de base, sem alegar melhora sem dados.

## Limites

ChatGPT Web nao monitora todos os chats em segundo plano automaticamente nem escreve memoria persistente sem acao real autorizada. Integracoes MCP e agentes autonomos sao uma fase posterior, com desenho e testes próprios.
