# AnyTV — matriz técnica de controles de streaming

**Estado:** revisão de código em 14/10/2026. Não é homologação física da TCL.

## Base de evidências
- Android Media3 `MediaSession`: https://developer.android.com/reference/androidx/media3/session/MediaSession
- Android TV Remote v2 (enum e key codes): https://github.com/tronikos/androidtvremote2/blob/main/src/androidtvremote2/remotemessage.proto
- Implementação efetiva: `overrides/PlaybackTransportBar.kt`
- Integração do complemento: `overrides/tv_safe/TvAppBridge.java`, `overrides/tv/BluetoothRecoverAction.java`

## Matriz de comportamento

| Elemento AnyTV 2.7 | Comando/ação real | Afirmável pela implementação | Limitação | Próximo passo |
| --- | --- | --- | --- | --- |
| Reproduzir | `KEYCODE_MEDIA_PLAY` | Comando remoto é solicitado | App pode ignorar ou não ter mídia ativa | Teste com app aberto |
| Pausar | `KEYCODE_MEDIA_PAUSE` | Comando remoto é solicitado | Depende do player | Teste com app aberto |
| Parar | `KEYCODE_MEDIA_STOP` | Comando remoto é solicitado | Pode encerrar sessão ou não fazer nada | Teste por app |
| −10 s / +10 s | `KEYCODE_DPAD_LEFT/RIGHT` | Apenas seta esquerda/direita | **Não existe duração garantida de 10 s** e pode mudar o foco em menus | Corrigir rótulos na próxima versão |
| Aceleração 1×/2×/4×/8×/16× | Repetição de `KEYCODE_MEDIA_REWIND/FAST_FORWARD` | Os toques foram solicitados | **Não há leitura de velocidade real**; níveis visuais são contadores de cliques | Remover velocidades não verificadas e manter avanço/retrocesso simples |
| ▶ Normal | `KEYCODE_MEDIA_PLAY` | Solicita retorno à reprodução | Não confirma normalização de velocidade | Ajustar semântica do rótulo |
| Do início | `KEYCODE_MEDIA_PREVIOUS` | Solicita item anterior | **Não garante reinício do episódio** | Rótulo fiel ao comando, se mantido |
| Episódios | Abre `AlertDialog` local | Mostra orientações | Não navega diretamente à lista | Não anunciar atalho de episódios |
| Fones | Integração Bluetooth pelo complemento, se disponível | Chama fluxo de reconexão/configuração | Permissões, pareamento e fabricante podem restringir | Homologar com modelo TCL real |

## Por aplicativo: estágio da evidência

| Aplicativo | Play/Pause | Seek | Reinício / episódios | Observação |
| --- | --- | --- | --- | --- |
| YouTube | Teste físico pendente | Teste físico pendente | Teste físico pendente | Não assumir salto temporal fixo |
| Netflix | Teste físico pendente | Teste físico pendente | Teste físico pendente | Navegação de episódios depende da interface aberta |
| Prime Video | Teste físico pendente | Teste físico pendente | Teste físico pendente | Não assumir manipulação de velocidade |
| Outros apps instalados | Teste físico pendente | Teste físico pendente | Teste físico pendente | Catálogo precisa vir do complemento real |

## Política de correção

1. Não alterar o branch `main`.
2. Priorizar a homologação de digitação da versão Android 2.7 antes de publicar mudança de interface.
3. Alterar somente **rótulos/feedback** comprovadamente incorretos e controles com suporte documentado; preservar as ações existentes até validar compatibilidade.
4. Não introduzir `MediaSession` própria para comandar player de terceiros: a documentação descreve o processamento por apps de mídia, não garantia do remoto sobre players externos.
5. Executar testes de regressão de pad, playback e Bluetooth, build CI e, após assinatura histórica, homologação física antes de declarar funções concluídas.
6. Se não houver evidência de suporte ou acesso confiável à função, não oferecer botão com promessa impossível.
