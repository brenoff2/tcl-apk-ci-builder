# AnyTV V2 — contas e sincronização de biblioteca (proposta técnica, não ativada)

Pedido do proprietário: ao iniciar o app, permitir conta com e-mail/senha ou Google; restaurar preferências, filmes/séries assistidos, séries acompanhadas e favoritos em reinstalação e novo dispositivo.

## Situação verificada
- O app Android V2 guarda preferências em SharedPreferences e outros registros locais em `overrides/TclStreamingScreens.kt`; isso NÃO sincroniza automaticamente. A autenticação do resolvedor de links é de **instalação/dispositivo**, não uma conta do usuário.
- Não há autorização para tratar autenticação Google como um sinal de direito a links diretos. O fluxo de liberação por dispositivo deve continuar explícito e independente.
- O cadastro e login ainda NÃO foram implementados. Não publicar interface de login decorativa que aceite credenciais sem backend real.

## Contrato de implementação
1. Uma identidade de usuário real com e-mail/senha e login Google usando provedor oficial com OAuth/OIDC e PKCE; nada de senha própria armazenada pelo app. Selecionar e verificar um provedor autenticador (ex. Supabase Auth/Firebase Auth) antes de desenvolver o fluxo.
2. Exibir tela inicial de entrar/criar conta/entrar com Google/continuar localmente. Acesso como convidado mantém a biblioteca atual. Não bloquear funcionalidades existentes por indisponibilidade de conta.
3. Dados sincronizados por `user_id`, com regras por usuário (RLS/autorização): gêneros preferidos, serviços favoritos, títulos favoritos (chave media+TMDB id), filmes vistos, séries acompanhadas, episódios/temporadas vistos, listas. Nunca salvar tokens ou senhas em preferências de texto puro ou em perfis sincronizados.
4. Na primeira entrada em conta, oferecer importação dos dados locais, sem apagá-los. Mesclar por chave estável e `updated_at`, preservar exclusões por tombstones/versionamento e evitar uma sincronização vazia que apague conteúdo anterior.
5. Offline-first: mudanças entram em fila local persistente, com idempotência, retentativa e reconciliação verificável; expor estado de sincronização e opção de sair sem apagar dados da conta. Não sincronizar códigos da TV, certificados de assinatura nem credenciais de links.
6. Testes: dois aparelhos com a mesma conta, logout/login, reinício offline/online, conflito de edição, títulos vistos/episódios/favoritos, migração local antiga, autenticação Google e email, revogação de sessão, isolamento entre contas.
7. A liberação para links diretos permanece um fluxo de autorização de instalação separado, acessível em "Verificar acesso aos links" / "Solicitar liberação". Login não concede esse direito.

## Critério de pronto
Auth real no backend implantado e validado, sincronização bidirecional auditada em duas instalações, migração local não destrutiva, fluxos de recuperação de conta e privacidade completos. Não marcar como pronto por apenas criar o arquivo ou uma tela.
