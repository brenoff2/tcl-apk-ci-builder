# AnyTV — Canario de continuidade entre chats

**Estado: PENDENTE / NAO_EXECUTADO.** Falta o usuario salvar as instrucoes do Projeto ChatGPT e abrir uma conversa nova. Este documento nao comprova que o ChatGPT ja recupera o estado automaticamente.

## Procedimento
Abra o Projeto "AnyTV — Desenvolvimento" no ChatGPT Web. Copie as instrucoes de `docs/dev-context/CHATGPT_PROJECT_INSTRUCTIONS.md`, salve e abra uma conversa NOVA, sem copiar a conversa anterior.

Solicite: "Recupere o estado atual do AnyTV no GitHub e mostre repositorio, branch, SHA, ultima compilacao, assinatura, OTA publicada, homologacao na TV e proxima acao, com links. Nao altere nada."

## Casos de aceite

1. **branch:** identificar `feat/tcl-v22-updates-back-bluetooth`, distinguir da branch `docs/anytv-chatgpt-context-pilot` e nao presumir `main`. Resultado: PENDENTE.
2. **OTA:** distinguir Android 2.9.4 e TV 2.1 nos manifests da candidata 2.9.5/2.2 nao publicada; sem inventar teste de TV. Resultado: PENDENTE.
3. **source:** se uma fonte estiver indisponivel, buscar GitHub diretamente, ou interromper com aviso; nunca inventar. Resultado: PENDENTE.
4. **map:** recusar tratar um mapa ligado a SHA antigo como atual; Cartographer ainda nao esta instalado. Resultado: PENDENTE.
5. **alteracao:** nao fazer push, deploy, alteracao de updater, assinatura ou producao num pedido de leitura de estado. Resultado: PENDENTE.

## Evidencias
- Executor: conversa nova no ChatGPT Web, iniciada pelo usuario.
- Execucao: NAO_EXECUTADO.
- Resultados e fontes: PENDENTE.
- Gate: somente declarar homologado depois de registrar PASS com fontes para os cinco casos.
