# AnyTV — auditoria dos atalhos da tela inicial da TCL

**Data:** 15/10/2026  
**Estado:** pesquisa de código oficial concluída; leitura e replay na TCL real **não homologados**.  
**Escopo:** Android TV/Google TV Home → identificação do título → ação do cartão → app streaming. Não alterar o AnyTV celular 2.9.2 nem o Connect 1.9 publicado para executar esta pesquisa.

## Descoberta central

No Android TV (API 26+), aplicativos publicam canais e cartões usando o `TvProvider`. Cada registro de `PreviewPrograms` e de `WatchNextPrograms` pode conter:

- `title`, `package_name`, `content_id`, `internal_provider_id`;
- `intent_uri`: representação serializada de um `Intent` (por `Intent.toUri`), recuperável via `Intent.parseUri`;
- metadados de ano, tipo e disponibilidade que ajudam a reconciliar com TMDB.

A Home abre o `intent_uri` associado ao cartão quando o usuário seleciona o programa. Não confundir com `APP_LINK_INTENT_URI`, usado para o logotipo do canal / abertura do aplicativo.

Fontes oficiais:
- https://developer.android.com/training/tv/discovery/recommendations-channel
- https://developer.android.com/reference/android/media/tv/TvContract.PreviewPrograms
- https://android.googlesource.com/platform/packages/providers/TvProvider/+/refs/heads/master/src/com/android/providers/tv/TvProvider.java

## Barreiras comprovadas na fonte AOSP

O `TvProvider.createSqlParams(OP_QUERY, ...)` restringe por `package_name`, exceto com `android.permission.READ_TV_LISTINGS` e dados `searchable=1`. Essa permissão tem nível **dangerous**, precisa ser declarada e concedida pelo usuário quando suportada; não fazer bypass e não solicitar permissões privilegiadas.

AOSP exige `selection` vazio para chamadores sem acesso total de EPG. Assim, usar projeção pequena e filtrar **no processo da TV** os itens, sem SQL `WHERE` fornecido por terceiros e com limites de quantidade.

Algumas recomendações da página Para você do **Google TV** são alimentadas por parceiros via Media Actions/Engage SDK e não necessariamente são registros legíveis em `TvProvider`: https://developer.android.com/training/tv/get-started/google-tv

## Diagnóstico opt-in antes de publicar nova versão

1. Confirmar pela interface se a TCL mostra **cartazes de títulos individuais** que abrem suas fichas (não confundir com ícones dos apps).
2. Identificar launcher atual (Android TV Home vs Google TV Home), versão Android e provedor `content://android.media.tv/`.
3. Em um teste isolado, somente leitura, solicitar `READ_TV_LISTINGS` caso o firmware permita e o usuário aprove.
4. Consultar `TvContract.PreviewPrograms.CONTENT_URI` e `TvContract.WatchNextPrograms.CONTENT_URI`, com projeção mínima `title`, `package_name`, `intent_uri`, `content_id` e, quando existente, ano/tipo; limitar quantidade e filtrar localmente.
5. Não enviar nem publicar dados de personalização, histórico, contas ou `intent_uri` íntegro em logs, GitHub ou internet. Exibir ao usuário apenas totais e amostras mascaradas de títulos de teste autorizados.
6. Comparar 3 cliques reais na Home: Netflix, Disney+ e Prime Video, se houver cartões desses serviços. Comparar a ação observada com a URL publicada.
7. Validar que a ação é uma `ACTION_VIEW` ou outra ação explicitamente exportada e que o pacote alvo confere com o app de streaming instalado. **Nunca** executar `Intent.parseUri` arbitrário recebido por HTTP ou proveniente de provedor desconhecido; permitir apenas tipos e destinos esperados.
8. Reexecutar o comando para o título selecionado, registrar separadamente `intent_despatched`, `search_requested`, `opened_app` e `detail_physically_confirmed`. Só o usuário na TCL pode confirmar o último.
9. Testar sem permissão, sem cartão, com ambiguidade título/ano, com pacote ausente, com URI não exportada, com outros perfis e após reiniciar a TV.

## Arquitetura candidata (híbrida)

`AnyTV(TMDB id, nome, ano, provedor)` → `AnyTV Connect` → procura correspondência em cache permitido de `TvProvider` → valida pacote/action/URI → dispara intent confirmado pelo sistema → retorno honesto.

Se não existe o cartão local: resolvedor de IDs por provedor via APIs oficiais/autorizadas ou links públicos comprovados; por último, busca no aplicativo. A fonte Home **não é um catálogo universal**; ela pode cobrir apenas itens publicados em canais locais ou em Assistir a seguir.

## Decisão

**ADAPT** o mecanismo oficial de cartões da TV com permissão explícita e segurança por provedor. Primeiro homologar um protótipo read-only na TCL. Não adotar captura por Acessibilidade (histórico de Play Protect) nem scraping privilegiado da interface/contas da Google TV; não substituir o controle remoto estável.

**Aceitação do protótipo:** relatório local indicando `permission_granted`, `provider_accessible`, `items_read`, `matching_items`, `intents_validated`, `details_opened_on_device`; pelo menos um título não hardcoded da Disney+ e um de outro serviço abrindo na ficha. Não lançar nova versão oficial antes de evidência da TCL.
