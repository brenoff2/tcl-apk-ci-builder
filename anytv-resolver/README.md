# AnyTV — catálogo TMDB e resolvedor de links (Worker ativo)

**Atualização:** 23/10/2026  
**Branch canônica:** `feat/anytv-p0-device-proof-verifier-20261020`  
**Status:** Worker `anytv-link-resolver-pilot` ativo na Cloudflare, versão `ce51439e-ca19-40d3-85e8-019c7a3669a5`, deployment `3298f15b-68d1-455b-bbc1-624925d7ccaf`, 100% do tráfego. AnyTV V2 permanece na versão `2.9.22` (code 51); a mudança de backend não exige novo APK.

## Escopo do módulo

Um Worker isolado consulta a API licenciada `https://api.movieofthenight.com/v4` e usa apenas `TMDB ID`, `country=br`, provedor e modalidade de acesso. Ele retorna um **candidato a URL da ficha**, nunca declara que um filme abriu ou está reproduzível no perfil da TV.

Fontes:
- Contrato OpenAPI: https://github.com/movieofthenight/streaming-availability-api/blob/main/openapi.yaml
- Termos: https://github.com/movieofthenight/streaming-availability-api/blob/main/TERMS.md
- D1: https://developers.cloudflare.com/d1/get-started/
- Secrets: https://developers.cloudflare.com/workers/configuration/secrets/
- Custo: https://developers.cloudflare.com/d1/platform/pricing/

O cliente precisa solicitar `GET /v1/resolve/movie/<TMDB-ID>/<provider>`; também há `tv/<TMDB-ID>`. Apenas os provedores `netflix`, `disney`, `prime`, `apple`, `hbo` e **Brasil** são aceitos. Nenhum input pode selecionar host, URL remota ou região diferente.

Resposta de sucesso, exemplo de estrutura (URL omitida propositalmente):

```json
{
  "status": "link_candidate",
  "url": "<https://streaming-oficial.example/rota-validada>",
  "verified_on_tv": false,
  "source": "movieofthenight",
  "cached": false,
  "attribution": "Streaming Availability API by Movie of the Night",
  "attribution_url": "https://www.movieofthenight.com/about/api"
}
```

Os resultados `not_available`, `no_supported_link`, `ambiguous` e `resolver_unavailable` são intencionais. Não existem respostas locais `monthly_quota_exhausted` ou `rate_limited`: limites internos de uso foram removidos. A chamada não cria nem acessa contas de streaming, não inicia reprodução e não ignora bloqueios regionais.

## Garantias implementadas

- Segredos `ANYTV_MOVIE_API_KEY` (somente na avaliação do GitHub) e `ANYTV_MOVIE_API_KEY` no Worker **não têm carregamento automático entre GitHub e Cloudflare**. No Worker o nome escolhido é `ANYTV_MOVIE_API_KEY`; jamais colocar valores em `vars`, repositório, APK ou respostas.
- `ANYTV_CLIENT_TOKEN` é um token de administrador para **piloto servidor-a-servidor**, não um mecanismo de contas comerciais. Não distribuir esse token fixo no APK.
- **Sem teto de uso do AnyTV:** removidos os bloqueios internos de 200 consultas por mês e 20 consultas por minuto para catálogo, resolvedor e ativação; nenhuma vinculação `RATE_LIMIT` nem variável `MAX_*_CALLS_MONTH` permanece no Worker.
- `DB`: D1 usado para cache e para ativação/autorização do dispositivo. A tabela legada `monthly_quota` ainda existe para rollback histórico, mas não é consultada ou atualizada pelo Worker.
- Cache global via D1: resultado positivo por 6 horas; ausente/ambíguo por 30 minutos. Não se armazenam imagens, payloads globais ou dados pessoais.
- URLs HTTPS apenas nos domínios e formatos próprios dos streamings; Apple TV+ aceita `playableId` controlado, Prime exige um `gti` válido. Removem-se parâmetros de publicidade.
- Filtros: oferta de assinatura ou gratuita, no Brasil, sem canal adicional. Aluguel/compra não equivalem à assinatura.
- Uma falha ou inconsistência retorna erro seguro, sem revelar chaves nem dados de outros títulos.

**Atenção:** duas consultas simultâneas para a mesma chave podem consultar o fornecedor em paralelo antes de o cache ser gravado. Não há teto interno do AnyTV; limites contratuais externos (Cloudflare/TMDB/Movie of the Night) continuam sujeitos aos próprios planos dos fornecedores. A conta de cada fornecedor não foi alterada nem promovida a um plano pago.

## Histórico de instalação (não é procedimento de deploy atual) — **NÃO executar na infraestrutura CLFCJ**

1. Criar uma conta/projeto Cloudflare **dedicado ao AnyTV**, separado de CLFCJ e demais sistemas.
2. Criar D1 independente, a partir de `anytv-resolver/`:
   ```sh
   npx wrangler login
   npx wrangler d1 create anytv-link-resolver
   ```
   Substituir `REPLACE_WITH_D1_DATABASE_UUID` no `wrangler.jsonc` pelo identificador retornado. Não adicionar tokens ao arquivo.
3. Aplicar migração em ambiente de teste e conferir o esquema; só depois aplicar remotamente:
   ```sh
   npx wrangler d1 migrations apply anytv-link-resolver --local
   npx wrangler d1 migrations apply anytv-link-resolver --remote
   ```
4. Configurar os dois segredos do Worker, no painel Cloudflare (Workers → projeto → Settings → Variables and Secrets):
   - `ANYTV_MOVIE_API_KEY`: mesma **credencial contratada** da Movie of the Night, se o usuário autorizar o uso na Cloudflare.
   - `ANYTV_CLIENT_TOKEN`: valor independente, longo e aleatório para o piloto, **nunca compartilhado com o app comercial**.
   
   O comando `wrangler secret put` pode criar uma versão e implantá-la imediatamente; evitar executá-lo sem revisar o contexto de produção.
5. O Worker está em produção com os próprios segredos e binds D1. Não reintroduzir `RATE_LIMIT`, `MAX_UPSTREAM_CALLS_MONTH`, `MAX_LINK_CALLS_MONTH` nem quotas de uso para a conta pessoal do AnyTV. Limites eventualmente impostos pela API contratada são externos e devem ser tratados sem bloquear artificialmente o aplicativo.
6. A implantação em hostname e a conexão com os APKs ficam bloqueadas até haver autorização para o ambiente AnyTV e **autenticação por usuário** (login + entitlement ou token curto, revogável). Não fazer bypass usando uma credencial estática embutida.

## Evidências de teste

- CI isolado Node + SQLite (14/14 inicialmente): https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38777631416
- CI de API comercial real: https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38776166751
- Validação real do novo resolvedor em Netflix, Disney+, Prime e HBO Max; depois Apple TV+ com `playableId`:
  https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/38778069911
- Última execução de Apple TV+: 17/17 testes locais verdes e link real de *The Gorge* aceito. As cinco fontes estão confirmadas **por consultas reais**, mas **não por abertura de aplicativo na TCL**.

## Portões de lançamento

1. Licença comercial do **TMDB** para imagens e sinopses **ou remoção/substituição de toda a dependência TMDB**: https://www.themoviedb.org/api-terms-of-use
2. Confirmar por escrito uso comercial, atribuição, imagens, cache e franquia efetiva da Movie of the Night.
3. Preservar identidade de dispositivos, revogação e autorização do resolvedor; não expor tokens fixos em APKs.
4. Implantar em ambiente próprio, com monitoramento de custos, logs mínimos, alertas de cota e proteção contra tráfego malicioso.
5. Em cada provedor: validar intent + pacote de destino em TV TCL real, inclusive Apple `playableId`. Não declarar "aberto na ficha" só porque `startActivity` não lançou exceção.
6. Preservar versão anterior dos APKs, paridade de assinatura e rollback, sem mexer no branch `main` nem nos ambientes CLFCJ.
