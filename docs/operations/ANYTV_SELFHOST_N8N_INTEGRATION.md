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
