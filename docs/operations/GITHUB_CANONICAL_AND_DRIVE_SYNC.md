# AnyTV — norma de escrita e sincronização GitHub ↔ Google Drive

## Fonte única de verdade por tipo
- **GitHub:** código, ADR, contratos, decisões, histórico de commits, CI, issues/PR, arquitetura, progresso, status, incidentes, handoff, documentação de release e manifestos de SHA.
- **Google Drive:** acervo histórico de APKs aprovados, documentos auxiliares e **backups de assinatura mantidos em pasta privada isolada**.
- **Cloudflare:** Worker/D1 e credenciais de catálogo em Secrets, com configuração e migrações documentadas no GitHub **sem valores secretos**.
- **Dispositivos físicos:** evidência de instalação/funcionamento; nunca inferida de CI.

## Ritual transacional de mudança
1. Inspecionar HEAD e estado atual dos conectores, definir escopo mínimo, implementar em branch.
2. Rodar testes específicos e regressão; documentar IDs e conclusões, não só URLs.
3. Versionar decisão, rastreio de mudança, riscos e procedimento de rollback no mesmo ciclo.
4. Confirmar certificado e versionCode antes de publicar OTA; executar readback do binário/hash e somente então copiar versão aprovada para histórico Drive.
5. Ler Drive após escrita e conferir nome, tamanho e hash quando houver acesso a bytes; falha parcial = status pendente, nunca relatar sucesso total.
6. Após cada sessão, atualizar `docs/STATUS.md`, `docs/handoffs/CURRENT_HANDOFF.md`, e evolução `docs/roadmap/PROJECT_PROGRESS.json` apenas com evidência de aceite. Nenhuma aprovação humana inventada.

## Diretórios
`docs/decisions/` ADRs; `docs/operations/` runbooks, inventário e entregas; `docs/ideas/` backlog de sugestões; `docs/handoffs/` continuidade; `docs/roadmap/` andamento e marcos. Não duplicar histórico já canônico; vincular arquivos.

## Segurança
Proibido enviar ao GitHub ou logs: chaves PKCS12, ZIP de chaves, senha, decriptação, recovery secret, tokens API ou códigos de ativação. Registrar exclusivamente referências genéricas, fingerprint público do certificado e verificação booleana. Mesmo se o material for acessível via Drive, isso **não autoriza publicá-lo**.

## Entrega
Uma atualização OTA do celular somente após build e teste, verificação do APK de code maior com identidade cert2026, deploy compatível de backend e aceite. Não transformar pacote unsigned em link de instalação.
