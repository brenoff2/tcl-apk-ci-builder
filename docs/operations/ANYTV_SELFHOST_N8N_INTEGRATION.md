# AnyTV — Supabase Self-Hosted junto ao n8n

## Descoberta verificada em 31/10/2026
Servidor Contabo `vmi3505836`, 8 vCPU, RAM 23.47 GiB (10.01 GiB disponiveis no snapshot), disco 580.16 GiB (310.36 GiB livres), swap 16 GiB (6.20 GiB usados). Carga media 1/5/15 min 8.75/6.73/6.27: usar recursos com parcimonia.

O stack real **ja existe**: Docker Compose `clfcj-supabase-e13-shadow` sob `/opt/clfcj-supabase-e13-shadow`; `supabase-auth` (GoTrue 2.192.0), `supabase-db` (PostgreSQL 17), `supabase-rest` (PostgREST 14.12), `supabase-kong`, `supabase-studio`, storage e pooler, todos ativos/healthy. `automacao-n8n-1` tambem ativo, na rede Docker `clfcj-supabase-e13-shadow_default`.

**Decisao do usuario:** usar Supabase Self-Hosted no ambiente do n8n; nao criar projeto Supabase Cloud, nao adicionar cobranca recorrente desnecessaria e nao criar Auth caseiro.

## Limites operacionais
- Stack descoberto atende CLFCJ, com overrides de producao e cutover; NAO rodar `docker compose up/down`, migracoes em `public`, mudancas em GoTrue, secret rotation, Kong, dominio ou portas sem plano de isolamento e rollback.
- A presenca de containers Supabase nao prova que esta permitido compartilhar seu tenant Auth com AnyTV. Verificar se contas existentes pertencem a CLFCJ; escolher isolamento por novo tenant/instancia ou por servico dedicado no mesmo host, com autenticação separada. O n8n permanece intocado.
- Possivel isolamento recomendado: Auth + banco/logical database do AnyTV separados da identidade CLFCJ, mas na mesma VPS e com DNS TLS AnyTV dedicados, sempre mensurando impacto RAM/CPU. Reutilizar imagens oficiais e componentes (supabase/auth, postgres, postgrest), nao construir login a mao.
- O esquema `supabase/migrations/202610310001_anytv_library.sql` ainda NAO foi aplicado; antes de deploy apontar esta migration somente ao banco AnyTV escolhido. Ela usa `auth.users`, `auth.uid()`, RLS e nenhuma service_role no Android.
- Gateway Android: `supabase-kt` (auth, PostgREST) com URL publica HTTPS do proprio AnyTV e chave *publicavel* (anon); token service_role somente no servidor. Credenciais Google no servidor e no Google Cloud, HTTPS/callback verificados; nao solicitar codigos ao usuario final.
- Convidado sempre funciona sem login; dados locais nao sao apagados em troca de conta, e importacao precisa ser explicitamente mesclada.
- Chaves de device proof Cloudflare de links diretos sao independentes do login de conta.
- Auditar schema RLS, rate limits, e-mail SMTP, senhas/reset, refresh, logs sem segredos e backups antes de ativar autenticação.
- Gate de publicacao: healthcheck e backup do host, dominio/certificado, servidor de Auth, email/senha, login Google, logout, migracao local, isolamento entre 2 contas, sync entre 2 dispositivos, load-test moderado, rollback e APK assinada. Preservar versao 2.9.59 ate aprovar.
## Estado
Inspecao read-only concluida. Nenhum compose, tabela, senha, chave, credencial, registro Auth ou servico de producao foi alterado por esta decisao. A integracao em runtime e o deploy ainda pendem de isolamento do stack em producao.

## Verificacao adicional 31/10/2026
- MCP server_ops `list_directory` do projeto `/opt/clfcj-supabase-e13-shadow` respondeu com acesso somente leitura; `.supabase-version` foi lido e registra `ref=self-hosted/v0.7.2` e gestao por E13. Evitar modificacoes diretas em E13 sem preservar isolamento.
- Consulta de configuracao autenticação via `exec_command` foi bloqueada pela governanca `DESTRUCTIVE_CONFIRMATION_REQUIRED` mesmo apos `confirm_write`; nenhum comando executado e nenhuma variavel/secreto foi exposto.
- Decisao de execução: nao alterar variaveis de Google Auth, SMTP, roteamento ou Postgres do CLFCJ. Criar primeiro adapter mobile usando SDK pronto e parametros *publicos* de endpoint separados do AnyTV; migrar apenas em ambiente AnyTV isolado com backup e verificacao de OAuth.

## Andamento implementacao Android SDK 31/10/2026
- Adaptador real `overrides/AnyTvSupabaseAuthGateway.kt` implementa `AnyTvAccountGateway` usando as APIs do `auth-kt` 3.8.0 verificadas no upstream (`awaitInitialization`, `currentUserOrNull`, `signInWith(Email)`, `signUpWith(Email)`, `signInWith(Google)`, `resetPasswordForEmail`, `signOut`). Commit `7ddb8e7706303357e721663bd21c0fe50ccbc4f7`.
- `scripts/apply_anytv_brand.py` agora copia o adaptador e inclui `auth-kt:3.8.0` e Ktor OkHttp; correcao da interpolacao de linhas Gradle commit `787053b889d571061a78373677cd8642beafda3a`. Teste de contrato `scripts/tests/test_anytv_account_entry.py` atualizado. Build corrente nao estava homologado na ultima consulta; nao declarar que adapter compilou nem ativar login no app ate completar QA e endpoint AnyTV isolado.
- O servidor existente self-hosted é compartilhado com CLFCJ. Não usar seu `auth.users` de producao sem tenancy independente; nenhuma variavel, docker, segredo ou banco servidor foi alterado.

## Compile do SDK Auth — 31/10/2026, novo ciclo
- O signed workflow `41167802941` falhou na etapa Kotlin: `Unresolved reference 'Email'` em `AnyTvSupabaseAuthGateway.kt`; demais erros eram cascata de inferência de tipo.
- Consultado upstream oficial supabase-kt tag 3.8.0: Email se encontra em `Auth/src/commonMain/kotlin/io/github/jan/supabase/auth/providers/builtin/Email.kt`; import corrigido no commit `efdc03cb15c94dfc4c4d129f2299dbbbcbf57b62`.
- Workflow **unsigned sem OTA** `.github/workflows/anytv-2951-unsigned-candidate.yml` passou a incluir alterações de módulos Auth e self-trigger, commit `a1648552e481c205cfebbd6da75e243b5cdebcb8`. Execução `41193275819` iniciou, sem resultado final quando este checkpoint foi escrito. Nunca liberar APK assinada/login baseado só em testes de contrato.

## 31/10/2026 — validacao de compilacao SDK + factory de cliente
- Build Android unsigned do SDK oficial, workflow run `41193275819`, terminou `completed/success`, artefato unsigned gerado. Confirmacao factual de compilacao apos import Email corrigido; NAO e login ativo.
- Preparado `overrides/AnyTvAuthClientFactory.kt` commit `3f1bd71fc4796cfc57796a413c531507e0586a8f`: cria client do Supabase Kotlin 3.8.0 com `install(Auth)`, valida URL HTTPS e chave publica vindas da futura configuracao isolada AnyTV. Nao inclui credenciais privadas. Incluido no build pelo commit `2b4241f3c820bdc259d59dee043a365371cfa34a`.
- Novo workflow de compilacao unsigned `41214083195` disparado para validar factory, ainda pendente na ultima checagem. Nao publicar OTA nem conectar CLFCJ/AnyTV Auth antes da separacao de tenants e TLS/OAuth.

## 31/10/2026 — cliente Auth e preflight
- Build unsigned `41214083195`: **completed/success** (compilou o client factory com SDK 3.8.0). A APK OTA 2.9.59 permanece intocada; a autenticacao end-to-end NAO esta pronta.
- Adicionado teste de contrato para validar URL HTTPS/chave publica em `scripts/tests/test_anytv_account_entry.py`, commit `1bb0015352aef3a4bde8ce385ed8163fb2d05702`. Os testes desse commit ainda sem readback final no momento do checkpoint.
- Adicionado `scripts/anytv_selfhost_preflight.py`, commit `223ea1715feeb3cbe79f06a5cb3b55fe444b7121`, inspecao local read-only de infraestrutura existente, destino separado e portas sem ler segredos. Nao executado no servidor nesta etapa.
- Confirmado via MCP que o projeto em producao `clfcj-supabase-e13-shadow` e administrado E13 e compartilhado com n8n. Isolamento da Auth AnyTV deve anteceder qualquer mudanca de credenciais/tabelas; nada em producao foi modificado.

## Checkpoint 31/10/2026 — criacao real do diretorio isolado
- Servidor Contabo vmi3505836: `server_open_ports` nao mostrou listeners nas portas candidatas 15433, 18081, 18181 (snapshot, nao reserva); `list_directory('/opt/anytv-auth')` retornou FileNotFoundError; chamada server_ops `make_directory('/opt/anytv-auth')` com confirm_write=true retornou `ok:true` e o destino criado. Nao houve criacao de containers.
- server_ops `exec_command` continua bloqueado por `DESTRUCTIVE_CONFIRMATION_REQUIRED`; tentativa de escrever README de preflight via `write_file` bloqueada pelo guardrail. Nao burlar MCP nem afirmar implantacao. Proximo passo: instalar stack isolado com segredo novo e HTTPS AnyTV, sem tocar no Compose E13/CLFCJ; testar email/Google e migracao, entao ativar interface.

## Atualizacao 01/11/2026 — compile validado
- GitHub Actions unsigned Android build `41193275819` terminou SUCCESS depois da correcao upstream do import `providers.builtin.Email`; valida compatibilidade de compilacao do SDK, NAO autenticação em runtime. Nenhuma APK OTA assinada nova foi publicada.
- Read-only server_ops: Supabase Auth v2.192.0, PostgREST v14.12, PostgreSQL 17 e Kong 3.9.3 saudaveis no stack E13, n8n operacional. RAM disponivel 10.48 GiB, swap utilizado 6.69 GiB: evitar instalar novo Supabase completo indiscriminadamente.
- Proximo gate: escolher isolamento de identidade Auth AnyTV e dominio HTTPS separados da producao CLFCJ; configurar segredos no servidor sem exportar para GitHub, migrar tabela com RLS, integrar sessao e UI convidado, testar Google e senha.

## AnyTV self-hosted staging — 03/11/2026
- Contabo `vmi3505836`: official Supabase Docker tag `self-hosted/v0.8.3` cloned and copied to `/home/ubuntu/automacao/anytv-supabase/instance`; isolated compose `docker-compose.anytv.yml` with project name `anytv-supabase`, all eleven containers renamed `anytv-*`, host ports loopback only 18680/18632/18643. `docker compose config --quiet` passed. No containers started.
- Supabase official scripts generated independent JWT and API keys, passwords and asymmetric keys in `.env` on VPS (mode 0600); no credentials in GitHub or APK. E13 CLFCJ Supabase and n8n untouched.
- Production host resource gate: 8 vCPU, 1-minute load 10.28, 5-minute 8.75, 15-minute 8.98; 10.29 GiB RAM available and 6.71 GiB swap used. Do NOT start full 11-service AnyTV stack until load/recovery/rollback verified. Need isolated HTTPS DNS, Google OAuth, SMTP, and backend health before public login.
- Android: `AnyTvAccountFlow.kt` created (guest+SDK callbacks); duplicated `AnyTvSupabaseClientFactory.kt` removed after CI redeclaration error; existing `AnyTvAuthClientFactory.kt` now installs Auth + Postgrest. Signed OTA workflow restricted to explicit release metadata pushes; current signed 2.9.59 stays reference. Latest unsigned CI still needs final success verification. Account flow not mounted until real tenant ready.


## 07/11/2026 — Cloudflare Tunnel isolado AnyTV Auth, domínio clfcj.com
- O usuario confirmou controle do dominio `clfcj.com` e informou que concluiu a configuracao inicial no Google Cloud, **sem readback do OAuth client**. Escolhido o subdominio de um nivel `anytv-auth.clfcj.com`, separando autenticação AnyTV dos servicos CLFCJ e aproveitando cobertura normal do Universal SSL da Cloudflare.
- Cloudflare zone `clfcj.com` ACTIVE, credencial autorizada em conta proprietaria. Novo tunnel remoto independente `anytv-auth-isolated` ID `cade72a5-76f3-4fad-b8a8-e9ad4f12fe60` criado e conferido por GET. Ingress remoto: `anytv-auth.clfcj.com` -> `http://localhost:18680`; fallback `http_status:404`. Registro CNAME proxied DNS `anytv-auth.clfcj.com` -> `cade72a5-76f3-4fad-b8a8-e9ad4f12fe60.cfargotunnel.com`, ID `d5aefcf18ac611322025eed4a8c2bcef`, confirmado por readback independente. Tunel AnyTV `inactive` porque conector ainda nao instalado na Contabo; o hostname nao esta funcional enquanto inativo. Os tres tuneis anteriores `clfcj-business-os`, `clfcj-data-plane`, `clfcj-deepseek-mcp-hub-v2` permaneceram `healthy` apos a escrita.
- URL autorizada no Google OAuth Web Client (usuario deve confirmar no proprio Google Cloud): `https://anytv-auth.clfcj.com/auth/v1/callback`. Variaveis futuras do GoTrue isolado: `API_EXTERNAL_URL=https://anytv-auth.clfcj.com/auth/v1`, `SITE_URL=https://anytv-auth.clfcj.com`, `ADDITIONAL_REDIRECT_URLS=com.tclremote.livre.v2://auth`, `GOOGLE_ENABLED=true`, `GOOGLE_CLIENT_ID` e `GOOGLE_SECRET` armazenados exclusivamente no `.env` da VPS. Google redirect URI no Compose deve derivar de `${API_EXTERNAL_URL}/callback`.
- Restricao de execucao: server_ops `ops_catalog_v2` reportou `authenticated_write_grant=false` e `mutation_recipes=[]`; nao existe recipe autorizado `anytv_auth_tunnel`. Nao executar escrita direta por atalhos, nem importar segredo OAuth para o chat/GitHub. Conector Cloudflare ainda deve ser instalado como processo/servico **dedicado** na VPS para nao substituir tuneis CLFCJ. Nao iniciar Supabase AnyTV ate verificar recursos, isolamento e rollback. Codigo Android/CI unsigned concluidos, mas login fisico, backend e OTA continuam NAO_HOMOLOGADOS.
- Proximo passo operacional humano: no Google Cloud validar callback exato do client ja criado; na Contabo instalar o novo conector isolado e preencher as credenciais Google com mecanismo seguro, sem compartilhar Client Secret em chat. Apos autorizacao governada de deploy e health, ligar auth, readback `/auth/v1/settings` mostrando `external.google=true`, teste Android real de retorno PKCE e persistencia. Preservar CLFCJ/n8n/OTA.
