# AnyTV V2 2.9.25/code54 — Séries em andamento e busca sobreposta

Data: 24/10/2026. Implementação **somente em DEV/PR #3**; nenhum APK assinado/OTA publicado.

## Diagnóstico confirmado no código

1. O catálogo de séries em andamento preservava IDs em `anytv_tracking_series` e episódios em `anytv_watched_episodes`, mas `restoreTrackedTitle` exigia dados recuperáveis de favoritos ou de `StreamingDiskCache.previousTitle`. Perder o snapshot resultava em uma série ainda rastreada, porém invisível.
2. A rotina de limpeza do cache editorial `StreamingDiskCache.write` incluía indevidamente arquivos `saved_title_*.json` no limite/evicção, apesar da intenção declarada de manter snapshots de títulos permanentemente.
3. A lista e a busca global compartilhavam o layout da capa: a caixa e os resultados da busca empurravam o conteúdo para baixo, o botão × apagava o texto em vez de fechar e os resultados eram apenas uma faixa horizontal, sem uma tela própria de consulta.
4. A aba `Em andamento` podia herdar um índice de rolagem de outra aba; nomes ausentes do cache levavam a uma área aparentemente vazia.

## Alterações realizadas

- Novo armazenamento local `anytv_watching_titles_v1` com nome, identificador, pôster, sinopse, data e gênero, independente dos favoritos. `toggleSeriesTracking(StreamingTitle)` e `toggleWatched(StreamingTitle,season,episode)` persistem dados mínimos do cartão junto com os IDs/episódios originais. Nenhum dado existente é apagado.
- `restoreTrackedTitle` consulta primeiro o registro durável de acompanhamento. Para séries legadas só com ID, busca metadados novamente pela rota já utilizada `tv/{id}` do backend, registra no armazenamento de acompanhamento e mostra “Recuperando séries acompanhadas…” enquanto tenta restaurar.
- Evicção de respostas editoriais ignora `saved_title_*.json`, corrigindo a causa de sumiço de snapshots de longa duração.
- Em andamento: apresentação em **grade vertical de duas colunas**, cartões completos de 158dp e ordenação estável por nome. **Busca própria** no topo pesquisa apenas séries desta lista, com limpar texto, mensagem quando não há correspondência, rolagem reiniciada ao trocar de aba/filtrar e lotes de 24.
- Busca global: removida do fluxo da capa e renderizada em **Dialog fullscreen opaco preto**, como página limpa. Controles separados: **Fechar busca** na barra superior e **Limpar texto da busca** junto ao campo. Resultados em linhas de dois títulos, rolados verticalmente por gesto. Mantém autocompletar e abertura de detalhes.
- Histórico `anytv_global_search_history_v1` privado ao aplicativo, até dez buscas, sem telemetria ou serviço novo. Só registra busca realmente enviada pelo botão/teclado ou resultado escolhido, sem guardar cada prefixo de autocompletar. Histórico reutilizável por toque, remoção individual e opção de limpar tudo.

## Testes e critérios de aceite

- Contratos de regressão `scripts/tests/test_streaming_screens_contract.py`: persistência independente, exclusão dos snapshots do prune, recuperação de legados, grade dupla, busca local, sobreposição preta, ação de fechar distinta de limpar, histórico limitado e rolagem vertical.
- Workflow candidato 2.9.25/code54 unsigned: **resultado depende de readback final da CI**. Nenhum teste físico celular/TCL é inferido do CI.
- Teste manual indispensável: acompanhar/desacompanhar série, marcar episódio, trocar rapidamente entre abas, reiniciar o aplicativo, simular limpeza de respostas editoriais, pesquisar apenas nos acompanhados e verificar que cartões persistem; na busca geral confirmar o overlay cobre a capa, teclado, arrastar resultados para cima/baixo, X fecha e limpar apaga somente o texto, seleção/remoção de histórico e abertura de título sem perder favoritos.
- TV Connect 2.5/code16 não foi alterada. Última OTA assinada do telefone segue 2.9.24/code53; CI de DEV não publica nem assina 2.9.25/code54. PR #3 draft, gates F0–F8 NAO_HOMOLOGADO, progresso formal 7/11 (63%) até prova física.

## Referências de implementação

- Upstream: Compose Material3 SearchBar e Dialog de tela inteira, baseados nos padrões públicos `vivi-music/ui/screens/search/SearchScreen.kt` e `AdbFileManager/ui/components/SearchDialog.kt`. Adaptado ao código atual, sem nova biblioteca.
- Fonte do projeto: `overrides/TclStreamingScreens.kt`, cache `StreamingDiskCache`, `StreamingCatalog`, `TclStreamingScreens`; decisões em `AGENTS.md` e `docs/handoffs/CURRENT_HANDOFF.md`.
