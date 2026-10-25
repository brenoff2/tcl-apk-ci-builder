# AnyTV V2 — visual unification candidate 2.9.27 (25/10/2026)

## Fonte e alvo
DEV `feat/anytv-p0-device-proof-verifier-20261020`, PR #3 draft. Baseline OTA publicada 2.9.26/code55. Esta rodada implementa o pedido de 25/10 a partir da captura real do usuário, SEM redesenhar logo/pôsteres nem alterar Worker/D1/companion TCL.

## Mudanças confirmadas por commits no GitHub
- `0204c5c3`: barra Filmes/Séries + seis ícones em uma linha com rolagem horizontal apenas dos ícones; `AnyTvPosterCard` utiliza imagem 2:3 `ContentScale.Fit`, estrela amarela/nota sombreada no canto superior esquerdo, assistir e coração sobre o canto superior direito; metadados F/S, ano, serviço em linha única e título em 2 linhas.
- `06e71858`: busca automática local nas bibliotecas, título **Filmes e séries assistidos**, barra vermelha nas seções, remoção de botões de seção Ver mais e duplicação de metadados abaixo do hero.
- `71a37e0c`: hero principal com nota sobre a arte; remove o carrossel geral da home mantendo carrosséis nas páginas correspondentes.
- `4f26ee35`: único carrossel na home: Cinema BR confirmado, sem carrosséis internacionais/Em breve duplicados nessa home.
- `a4ab89fd`: ações dos filtros com controles arredondados e identidade visual; streamings dentro do hero.
- `faf8fdf7`: hero do Cinema mostra F, ano e provedores BR sob o título, sem rodapé externo.
- `e04ed8e9`: títulos reais de séries com episódios marcados entram no histórico combinado, mantendo IDs/filmes anteriores e não inventando metadados.
- `cd9cd72b`: busca interna 44dp com mesmo ícone de lupa e limpar, filtra localmente a cada caractere; barra vermelha nos títulos do cinema e previews.
- Testes atualizados nos commits `2995762c`, `a9bb9c3a`, `d7404f22`, `008a815e`, `81da6034`, `3b39e6ab`, `ded58677`, `c6e45097`.
- Workflow de release 2.9.27/code56 **preparado, NÃO acionado**, commit `723c0be3`. Não usar OTA unsigned; apenas signer V2 histórico.

## Evidência e limites
- Testes textuais antigos falharam durante a iteração ao exigir elementos removidos a pedido do usuário, atualizados semanticamente mantendo testes de segurança, integridade e 24 itens.
- Candidate unsigned `.github/workflows/anytv-v2-candidate-compile.yml`, run **#40197057492**, era IN_PROGRESS na última leitura: testes de contrato PASS, Gradle em andamento. Resultado final/compilação/certificado **NÃO COMPROVADO** até readback.
- A build candidate ainda compila V2 code54 apenas para auditoria da fonte; versão oficial proposta 2.9.27/code56 deverá ser compilada e assinada pelo workflow de release após CI verde e autorização suficiente.
- Não declarar homologação física por screenshot anterior; QA Android/TCL permanece pendente. Não houve publicação 2.9.27 até esta linha. OTA publicada permanece 2.9.26/code55 até prova do contrário.
- Os filtros não foram reimplementados do zero: foram preservadas as duas colunas de serviços/gêneros, seleção, marcação em lote e persistência; botões receberam estilo do app. Visual final deve ser conferido no telefone.
- Garantias: 3 cards por linha, proporção 2:3, snapshot do histórico, sem catálogo ou disponibilidade inventados, selo azul Cinema somente a partir de IDs Brasil comprovados; sem merge e sem atualização do Worker.
