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

## Pesquisa GitHub upstream e decisao de integracao (31/10/2026)
Base adotada para a proxima implementacao: **supabase/supabase-kt** (biblioteca pronta; modules `auth-kt`, `postgrest-kt`, `compose-auth`), mantendo Jetpack Compose do aplicativo atual. Nao desenvolver um sistema proprio de passwords, refresh tokens, OAuth, login Google ou gerenciamento de sessoes.
- SDK oficial/repositorio: https://github.com/supabase/supabase-kt — Auth multiplataforma e PostgREST. Ver README Auth: https://github.com/supabase/supabase-kt/blob/master/Auth/README.md. Fixar versao compativel por build verificado, nao usar HEAD flutuante.
- UI Google nativa: https://github.com/supabase-community/supabase-kt-plugins/blob/main/ComposeAuth/README.md — Credential Manager no Android e fallback OAuth; a documentacao oficial do Google com Supabase tambem exige credenciais Web e Android e usa WEB_GOOGLE_CLIENT_ID: https://github.com/supabase/supabase/blob/master/apps/docs/content/guides/auth/social-login/auth-google.mdx.
- Alternativa avaliada: https://github.com/firebase/quickstart-android/blob/master/auth/README.md — email/senha e Google Sign-In funcionais, mas requer infra Firebase/Google separada para sincronizacao. A escolha Supabase reutiliza Auth+Postgres+RLS+PostgREST em uma pilha unica e tem componentes Compose prontos; nao reutilizar projetos CLFCJ/Amazon existentes.
- Android: usar `auth-kt` para email/password, recuperacao e sessao; `compose-auth` para login Google. Nao usar codigo de ativacao de dispositivo para login de usuario. O Google exige configurar OAuth (Android SHA e Web client ID), redirect permitido, e provedor no painel da conta AnyTV.
- Backend: **nenhum projeto Supabase exclusivo AnyTV foi identificado na listagem de projetos disponiveis**. Nao criar um projeto pago nem vincular dados de CLFCJ sem autorizacao/custo comprovados. Migracao D1 `0004_user_library_sync.sql` e apenas preparatoria; Supabase possui RLS propria e deve ter DDL de contas e biblioteca compativel com RLS antes do deploy.
- Persistencia: dados locais existentes continuam visiveis antes da autenticacao; importacao por opt-in, confronto por `media+id` e `updated_at`, tombstones, idempotencia. Sessao persistente em armazenamento seguro do SDK; validar que nenhum token aparece em log ou preferencias em claro.
- Gate de entrega: teste email/senha, login Google, logout, reinicio, recuperacao, restauracao em segundo aparelho, migracao local, RLS negando conta adversaria, e refresh de sessao. Publicar APK assinada somente apos teste dos fluxos reais no provedor configurado.
- **Estado**: pesquisa e decisao de base realizadas; servidor auth AnyTV, cliente Google OAuth, interface de login e sincronizacao ainda nao implementados. Nao prometer login funcional ate concluir.

## Decisão UX aprovada — tela de escolha antes do login
- **Primeiro acesso:** mostrar uma tela minimalista nas cores AnyTV (preto, vermelho e branco) com duas ações explícitas: **Continuar sem login** (acesso imediato ao app, modo convidado/local) e **Entrar ou criar conta** (abre a tela de autenticação, e-mail/senha ou Google). Não mostrar campo de senha na tela inicial de escolha.
- **Convidado:** catálogo, conexão com TV, favoritos, filmes assistidos, séries acompanhadas e preferências seguem funcionando e gravados localmente. Não forçar cadastro, não bloquear recursos existentes por ausência de conta, nem solicitar chaves de API ou códigos.
- **Aberturas seguintes:** guardar a escolha do modo convidado e abrir o app diretamente; permitir entrar/criar conta mais tarde em Perfil/Configurações. Após login bem-sucedido, deixar de mostrar a tela de escolha nas reaberturas, restaurando a sessão.
- **Migração:** quando um convidado decidir entrar, oferecer importação e mesclagem não destrutiva de toda sua biblioteca local para a conta, com confirmação e tratamento de duplicados, sem limpar a cópia local antes da sincronização verificada.
- **Saída da conta:** oferecer voltar a modo convidado sem apagar dados remotos. Não exibir biblioteca de outra conta após logout; isolar cache pessoal por usuário ou remover somente o cache de sessão privada.
- **Testes obrigatórios:** instalação limpa -> convidado -> reinício sem tela repetida; convidado -> Google/e-mail -> mesclagem -> reinício com sessão; logout -> convidado sem vazamento; sem internet -> convidado funcional.
- **Estado:** decisão de produto registrada; telas/autenticação/sync ainda não implementadas nem publicadas.
