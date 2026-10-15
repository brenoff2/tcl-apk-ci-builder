# AnyTV — teste comercial autenticado da Movie of the Night

**Data:** 15/10/2026  
**Fonte:** Streaming Availability API v4 — Movie of the Night  
**Execução:** https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38776166751  
**Estado:** testes de contrato e consulta ao vivo concluídos; abertura de fichas na TCL ainda não homologada.

## Resultado comprovado

- **11 requisições** ao provedor, com `X-API-Key` oriundo do segredo de repositório `ANYTV_MOVIE_API_KEY`, sem exibir o valor.
- **11/11 testes locais aprovados** no job anterior à consulta.
- **48 registros de filmes** retornados pela API de top títulos, sendo 10 Netflix, 10 Disney+, 9 Prime Video, 10 Apple TV e 9 HBO Max.
- Os **48 registros possuíam oferta de assinatura básica/gratuita brasileira e pelo menos um URL de formato reconhecido** pela função de classificação; identificados 49 links candidatos no total.
- **6/6 verificações adicionais** de identificador TMDB exato, serviço associado, tipo de oferta e URL de formato reconhecido: Zootopia 2 (Disney+), Birdman (HBO Max), Jurassic Park (Prime Video), O Irlandês (Netflix), O Projeto Adam (Netflix), Divertida-Mente 2 (Disney+).

| Serviço | Filmes da lista top | Filmes com URL candidata |
|---|---:|---:|
| Netflix | 10 | 10 |
| Disney+ | 10 | 10 |
| Prime Video | 9 | 9 |
| Apple TV | 10 | 10 |
| HBO Max | 9 | 9 |
| **Total** | **48** | **48** |

**Atenção metodológica:** o endpoint `/shows/top` já seleciona obras populares de um provedor; portanto 48/48 **não** é uma estimativa de cobertura do catálogo inteiro. Os 6 títulos de controle são uma amostra pequena e conhecida. O validador confere host e caminho da URL, **não** que o aplicativo Android TV aceite efetivamente o intent ou que o perfil consiga reproduzir.

## Ambiente, custo e segurança

- API de origem `https://api.movieofthenight.com/v4`.
- Script: `scripts/audit_movieofthenight.py`; suíte: `scripts/tests/test_movieofthenight_audit.py`.
- Execução de credencial isolada em workflow `.github/workflows/audit-movieofthenight.yml`, agora **somente manual** para evitar consumo de cotas e testes repetidos por cada commit.
- A chave permanece em GitHub Actions Secrets e **não** foi incorporada a APK, arquivos públicos, comandos de instalação ou respostas do relatório.
- Nenhuma versão AnyTV/AnyTV Connect, assinatura, pareamento ou `current.apk` foi modificada por esta avaliação.

## Decisão técnica

**ADAPT** a fonte licenciada em um resolvedor hospedado pelo AnyTV, com autorização específica do fornecedor, cache dentro dos limites contratuais, proteção do segredo, cota mensal e fallback. Não colocar a chave no celular/TV. Validar links por serviço, incluindo query strings específicas permitidas. Não considerar retorno HTTP ou dispatch de Intent como prova de reprodução.

**Pendências antes da comercialização:** confirmar por escrito direitos de imagens, metadados e armazenamento das respostas; plano/cota real da conta; disponibilidade de URLs em títulos de cauda longa e séries; teste físico na TCL com Netflix, Disney+, Prime, Apple TV e HBO Max; avaliação de custo e proteção contra uso abusivo do servidor.

**Critério para release:** só publicar nova versão quando ao menos três serviços distintos comprovarem abrir a ficha do filme correto em testes físicos e houver licença de uso comercial confirmada para os dados aplicáveis.
