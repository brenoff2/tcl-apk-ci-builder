# AnyTV P0 — Gate de identidade de dispositivo e resolvedor (20/10/2026)

Status: **DESIGN ONLY / BLOCKED FOR PUBLIC RELEASE**. Este documento nao autoriza deploy, emissao de credenciais, publicacao OTA nem alteracao de chaves.

## Evidencia ja confirmada
- Fonte: `anytv-resolver/src/worker.mjs`, branch de produto `feat/tcl-v22-updates-back-bluetooth`, blob `1515ff3995a9c4a5505b67e073add603d264659b`.
- `/v1/catalog/*` consulta TMDB pelo Worker com segredo no servidor; ultimo teste ao vivo registrado: workflow 38845048460 (PASS duas rotas), sem equivaler a testes de resolucao ou TCL.
- `/v1/resolve/movie|tv/{tmdbId}/{provider}` exige `ANYTV_CLIENT_TOKEN` fixo de piloto e `ANYTV_MOVIE_API_KEY`. **Nao** copiar `ANYTV_CLIENT_TOKEN` ao APK nem habilitar endpoint comercial aceitando esse token como identificacao de consumidores.
- `overrides/tv_safe/TvAppBridge.java` oferece pareamento local de oito digitos, mas nao identidade autenticada na nuvem; nao reutilizar o codigo LAN como credencial publica.
- O catalogo e as rotas OTA existentes devem permanecer intactos.

## Decisao de desenho (nao aprovada para lancamento)
**ADAPT**, apos validação do canal de distribuicao: separar (1) prova de legitimidade de instalacao/conta, (2) credencial temporaria emitida pelo backend e (3) autorizacao de cada consulta. Nao equiparar posse de chave gerada pelo proprio APK a prova de instalacao legitima.

Para instalacoes distribuidas fora da Google Play, **Play Integrity ainda depende de Google Cloud/projeto e verificacao server-side**, e seus vereditos de licenciamento Play Store nao podem ser presumidos como validos. Checar elegibilidade real antes de adotá-lo; nao bloquear usuarios legitimos por supor `LICENSED`. Referencia oficial: https://developer.android.com/google/play/integrity/standard .

## Contrato de seguranca para uma implementacao posterior
1. Emissao: endpoint de registro deve iniciar **fail-closed**. Nenhum cadastro anonimo por simples deviceId, certificado publico do APK, codigo de pareamento LAN ou segredo universal. Escolher previamente uma autoridade verificavel de cadastro (conta/ativacao aprovada/atestado elegivel), com mecanismo de revogacao.
2. Sessao: servidor associa instalacao/identidade aprovada a credencial de prazo curto; auditar `iss`, `aud`, `sub`, `iat`, `nbf`, `exp` e identificador de chave/rotacao. Validar assinatura criptografica e rejeitar expirados, modificados, audiencia incorreta e emissor desconhecido.
3. Vinculo: preferir prova de posse com chave da instalacao mantida no Android Keystore, vinculando assinatura da requisicao a metodo, caminho, corpo (se houver), timestamp e desafio anti-replay. Uma chave autogerada sem registro aprovado **nao** autoriza acesso.
4. Politica: validar permissao antes de cache ou upstream; manter quotas globais D1 ja existentes e introduzir cota individual por identidade. Revogacao e suspensao devem funcionar mesmo para sessao anteriormente valida.
5. Superficie: preservar `/v1/catalog/*` existente e `/health` sem novos segredos no cliente; manter `/v1/resolve` com token piloto **somente privado** ate substituicao validada. Nunca reusar segredo de Movie of the Night no cliente.
6. Telemetria: registrar apenas identificadores pseudonimos e motivos codificados, sem tokens, pares de chaves, texto de busca ou respostas upstream.
7. Publicacao: testes unitarios -> testes de integracao contra endpoint isolado -> auditoria auth/replay/quota -> teste controlado na TCL -> assinatura permanente 2026 -> OTA superior. Nao acionar deploy/OTA por teste verde.

## Proximos marcos verificaveis
- **M1 — escolha da autoridade de registro:** demonstrar concretamente se Play Integrity atende os APKs sideloaded deste projeto; caso contrario selecionar autorizacao de conta/ativacao verificavel, com controles de abuso. Sem essa prova, `BLOCKED`.
- **M2 — testes antes do codigo:** negar emissao anonima, autenticacao falsa, expiracao, replay, dispositivo revogado, troca de audiencia, uso excessivo; provar zero chamadas MovieOfTheNight na negativa.
- **M3 — implementacao minima em branch isolada:** modulo de verificacao e armazenamento de registro/revogacao com testes; integrar o app so depois de fechar M1-M2.
- **M4 — validacao end-to-end:** link candidate para titulo/provedor real, fallback seguro quando indisponivel, sem declarar reproducao ou abertura na TCL antes de teste fisico.

## Criterio de parada
Sem autoridade de cadastro verificada e credenciais reais configuradas de maneira segura, **NAO_PUBLICAR**, **NAO_INSTALAR** e **NAO_DECLARAR_P0_CONCLUIDO**. Readback do GitHub e CI documental nao sao homologacao.
