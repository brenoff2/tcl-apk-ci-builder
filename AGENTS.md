# AnyTV — Regras de desenvolvimento para ChatGPT Web

Este arquivo NAO e carregado automaticamente pelo ChatGPT Web. Em cada nova conversa, ler este arquivo via GitHub e confirmar branch/commit antes de programar. Seu uso nao depende de Codex nem Claude Code.

## Fontes de verdade
1. `docs/dev-context/STATUS.md` — estado e proxima acao auditados; se desatualizado, recarregar fontes.
2. `docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md` — releases, assinatura e publicacao.
3. `docs/CONTRATO_FUNCIONAL_TCL.md` — recursos e regressoes proibidas.
4. `docs/dev-context/DECISIONS.md`, `docs/dev-context/CODEBASE_MAP.md` (se existir e tiver commit verificado), codigo real, workflows e evidencias.

## Regras de atuacao
- Branch de desenvolvimento registrada: `feat/tcl-v22-updates-back-bluetooth`. Nao presumir que `main` contem a fonte vigente.
- Antes de editar, ler arquivos e dependencias realmente envolvidos; apresentar objetivo, nao-objetivos, arquivos, riscos e teste.
- Preservar controles, favoritos reais, aplicativos da TV, UI aprovada, streaming, protocolos, preferencias, assinatura e atualizacoes. Nao inventar outro fluxo.
- Usar Superpowers para analise, planejamento proporcional, depuracao sistematica, testes e verificacao.
- Trabalhar em branch isolada; nao alterar `main`, releases, APKs, `updates/**/current*`, assinaturas, senhas ou producao sem autorizacao especifica.
- Distinguir build, assinatura, publicacao OTA, emulador e homologacao real. Um sucesso nao prova outro.
- Nao declarar trabalho concluido sem evidencias observaveis; indicar NAO_TESTADO quando nao se puder executar.
- Se tres tentativas independentes falharem, parar patches e voltar ao diagnostico.
- Registrar no `STATUS.md` ultima evidencia, erros e proxima acao antes do handoff, sem transcrever conversas inteiras.
- Evolucao mutante: sugerir melhorias justificadas e reversiveis durante o trabalho; nao mudar escopo silenciosamente.
