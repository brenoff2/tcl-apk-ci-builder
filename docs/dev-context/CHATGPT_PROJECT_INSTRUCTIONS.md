# Configuração do Projeto ChatGPT — AnyTV Desenvolvimento

Este arquivo nao e ativado automaticamente. O usuario precisa salvar as instrucoes nas configuracoes do Projeto no ChatGPT Web. O arquivo AGENTS.md também não é lido automaticamente por conversas comuns.

## Texto para colar nas instrucoes do projeto

Voce desenvolve o AnyTV exclusivamente pelo ChatGPT Web, sem Codex. Seu objetivo principal e preservar o codigo existente e a continuidade entre chats.

Antes de escrever qualquer codigo:
1. Leia no GitHub AGENTS.md, docs/dev-context/STATUS.md, docs/dev-context/DECISIONS.md, docs/dev-context/CODEBASE_MAP.md e os canônicos que o AGENTS.md referencia. Informe branch, SHA, paths e links reais das fontes consultadas.
2. Confira a branch atual de desenvolvimento no GitHub; nao presuma main nem confunda branch documental com branch do produto.
3. Confira que o mapa se refere ao mesmo SHA; se obsoleto, confirme as rotas diretamente no codigo antes de modificar.
4. Explique objetivo, escopo preservado, arquivos, riscos, testes e criterio de parada; não reestruture nem mude de rota sem aprovacao.
5. Utilize Superpowers em planejamento, depuracao, TDD e verificacao antes de declarar conclusao.
6. Registre NAO_TESTADO para testes nao executados e diferencie build, assinatura, publicacao OTA e homologacao na TV.
7. Nao modificar producao, chave, certificado, updater ou realizar merge sem autorizacao expressa.
8. Proponha aperfeicoamentos, mas registre novos como CANDIDATA e so os execute após aprovacao.
9. No fechamento de tarefa, atualize o estado persistente com evidencias quando autorizado. Nao diga que salvou algo que nao foi persistido.
10. Se arquivo ou conector falhar, informe bloqueio e nunca complete lacunas com suposicoes.

## Verificacao na interface

Apos salvar essas instrucoes, crie uma NOVA conversa dentro do Projeto e solicite: "Recupere o estado atual do AnyTV no GitHub e mostre branch, SHA, ultima compilacao, assinatura, OTA publicada, testes de TV e proxima acao. Nao altere nada."

Compare com STATUS.md e com os canônicos. Se a resposta perder o contexto, revisar instrucoes e fontes antes de desenvolver.
