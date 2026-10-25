# AnyTV — botão Editar e seleção dual de streamings (25/10/2026)

## Mudanças na branch DEV
- Na Principal, `TclInstalledAppsBar` substitui o antigo ícone `+` pelo controle textual `Editar`, preservando o acesso à janela de organização dos aplicativos.
- A janela agora possui as colunas `Na minha TV` (itens realmente detectados na TCL) e `Outros streamings` (provedores suportados sem instalação identificada).
- As escolhas de ambos os grupos são salvas em preferências locais; favoritos instalados mantêm o fluxo anterior. Serviço extra não é confundido com pacote Android instalado.
- Na página de streaming, ao fim da faixa também existe `Editar` com as mesmas duas colunas. O serviço atualmente selecionado recebe contorno externo vermelho de 2 dp.
- Serviços de catálogo não instalados abrem visualização de filmes/séries, sem alegar que há aplicativo Android para execução na TV. O modo `Ver em lista` / `Ver em seções` segue reutilizado.

## Evidência e limites
- Commits: `c7d3eb6` (Editor na Principal), `89b231a` (Editor na página de streaming, navegação dos catálogos e stroke vermelho), `dae464e` (sincronização das escolhas instaladas entre a Principal e a página de streaming).
- CI unsigned para `dae464e`: run #40226695184 iniciado; resultado ainda não confirmado no checkpoint.
- Não geramos nem publicamos novo APK. Release OTA comprovado continua 2.9.33/code62, run #40224219849.
- QA física permanece pendente, PR #3 Draft sem merge.
