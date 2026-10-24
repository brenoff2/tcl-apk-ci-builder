> **24/10/2026 — ATIVAÇÃO REAL DA INSTALAÇÃO V2 CONFIRMADA NO D1 (substitui a pendência de provisão abaixo):** a instalação observada no telefone `com.tclremote.livre.v2` com sufixo mascarado `3bede1c2` **agora possui chave P-256 autorizada e válida**. Após emissão segura local de novo código de uso único e registro parametrizado apenas do SHA-256, o D1 confirmou **2 dispositivos ativos**, **2 códigos consumidos**, **0 códigos disponíveis**, **1 ativação vinculada a `3bede1c2`** e **1 prova assinada válida aceita em `device_nonces` para a instalação atual**. A identidade anterior `2d111981` continuou preservada. Isto comprova que a trava de autenticação do resolvedor foi removida para esta instalação, sem expor segredo, reativar código antigo, editar Worker ou publicar APK. **NÃO prova que um link de conteúdo abre a ficha correta ou inicia reprodução na TCL**: manter U01–U05/L03–L05 e QA física pendentes, 7/11=63% e F0–F8 não homologados. As notas abaixo sobre código recém-emitido aguardando uso são históricas.

> **24/10/2026 — PROVISIONAMENTO INDIVIDUAL EFETUADO, CONFIRMAÇÃO NO CELULAR PENDENTE:** captura real mostra instalação V2 `com.tclremote.livre.v2` terminando em `3bede1c2`, status de links não autorizado. Readback D1 antes: **um único código consumido, nenhum válido livre**, e único aparelho autorizado com sufixo diferente `2d111981`; não reutilizar código anterior. À solicitação expressa do usuário, foi emitido **novo código de uso único via emissor canônico** (`issueActivation` com 256 bits aleatórios); segredo exibido somente no console interativo local do operador Windows, **não gravado em GitHub, API, D1 plaintext ou chat**. Inserido apenas hash SHA-256 parametrizado no D1; readback: **2 códigos totais, 1 consumido, 1 disponível até 24/10/2026 12:54:51Z (09:54:51 Bahia), 1 identidade anteriormente autorizada intacta**. Worker em produção `bd8cafd5` 100%, `/health` HTTP 200, POST inválido em `/v1/device/activate` HTTP 400. **Ainda não comprova POST de ativação real 201, nem status assinado 200 da V2**: o usuário precisa digitar código temporário mostrado no terminal e verificar autorização. Não revogar registro anterior; não publicar novo APK por este provisionamento. Progresso 7/11=63%, gates F0–F8 não homologados. As notas anteriores abaixo são histórico.

> **24/10/2026 — PRODUÇÃO ATUALIZADA:** Worker **bd8cafd5**, com `/v1/device/status` e `videoLink`, está em produção 100%; AnyTV V2 **2.9.23/code52** assinada e publicada. Banco D1 continuou com único dispositivo antigo autorizado e único código utilizado; **nenhuma nova ativação foi emitida**, a instalação V2 ainda exige QA física. Ver [relatório de publicação](../operations/ANYTV_V2_2923_RELEASE_EVIDENCE_20261024.md). Os estados antigos de "não publicado" abaixo são históricos.

# AnyTV V2 — ativação individual dos links, sem repetir o cadastro antigo

**Data de readback:** 24/10/2026. **Estado:** diagnóstico real somente leitura; interface de ativação na branch DEV; **sem nova ativação ou deploy**.

## 1. Evidência de produção (Cloudflare)

Worker `anytv-link-resolver-pilot`: deployment `3298f15b-68d1-455b-bbc1-624925d7ccaf`, versão `ce51439e-ca19-40d3-85e8-019c7a3669a5`, 100% do tráfego. D1 vinculado ao UUID canônico configurado no `wrangler.jsonc`.

Consulta autenticada somente leitura da configuração mostrou:
- `ANYTV_RESOLVER_AUTH_MODE=device` (não está em `pilot`);
- `ANYTV_DEVICE_ACTIVATION_ENABLED=true`;
- tabelas `device_keys`, `device_nonces`, `device_activations` existentes no D1;
- **1 dispositivo registrado, 1 autorizado e válido; 1 código emitido, consumido; nenhum código não usado válido**;
- última ativação registrada em `2026-10-21T22:17:55Z`; última prova aceita registrada em `2026-10-21T22:39:40Z`. Não registrar IDs/chaves/códigos em docs nem relatórios.

**Diagnóstico:** o Worker está tecnicamente preparado para autorizações individuais. Não há evidência de que o único dispositivo registrado seja a instalação atual `com.tclremote.livre.v2`. O pacote V2 é separado do anterior; o Android Keystore isola a chave por UID/app: https://source.android.com/docs/security/features/keystore. **NÃO presumir equivalência de identidades nem copiar chave privada de outro APK.**

Os acessos públicos sem prova de dispositivo retornaram `/health -> HTTP 200` e os endpoints de link e status `HTTP 401`; isso comprova proteção do acesso, **não** que o celular do usuário falhe no mesmo teste.

## 2. Fluxo candidato no Android V2

No detalhe de um filme/série, acionar **Verificar autorização de links**. A requisição `GET /v1/device/status` é assinada com identidade local. Mostrar somente:
- autorizado: confirmação; nenhuma entrada de código;
- não autorizado: pacote instalado e **últimos 8 caracteres** do UUID local, campo de **código único de ativação**, botão para ativar;
- indisponível: mensagem clara, sem assumir que o dispositivo está bloqueado.

O código é de 256 bits, base64url de 43 caracteres, emitido apenas por operador autorizado, **não é chave de API**. Nunca salvar em `SharedPreferences`, D1 em texto claro, APK, GitHub, CI, analytics ou logs. A ação explícita usa `AnyTvDeviceIdentity.activate()` e comprova resposta com nova consulta assinada; apaga o campo após tentativa. Fecha/limpa o campo ao sair do detalhe.

**Pendente:** publicar no Worker o endpoint de status da candidata, somente após gate de produção. O Worker atual pode não implementar a nova rota, ainda que use autenticação device e tenha ativação habilitada.

## 3. Procedimento do operador (NÃO executar sem autorização)

1. Confirmar, em leitura, que a instalação V2 continua não autorizada e conferir o sufixo do UUID local com o registro D1, sem divulgar o ID completo.
2. Preservar a chave previamente autorizada. **Não excluir, substituir, revogar ou reutilizar registro antigo sem evidência e aprovação.**
3. Em terminal **local, privado e interativo**, o operador usa o emissor existente `anytv-resolver/scripts/issue-activation.mjs`, que usa `crypto.randomBytes(32)`. O código único só deve aparecer no terminal confiável, não em ferramenta que registre stdout. O emissor mostra **somente hash** e datas para criação da linha D1.
4. Inserir de forma autenticada e parametrizada **somente o hash** (`code_hash, expires_at, valid_until`) na tabela `device_activations` do banco AnyTV correto, se e somente se o usuário autorizar o provisionamento. O código não pode ser reutilizado após vincular outro dispositivo.
5. Colar o código no campo da V2. Confirmar `201` e, com outro nonce assinado, `GET /v1/device/status -> 200 activated`. Registrar apenas sucesso/fracasso, data/hora e sufixo mascarado.
6. Tentar links elegíveis por provedor: confirmar separadamente **ficha correta**, **busca aberta**, **apenas aplicativo aberto** e **reprodução comprovada**; nenhum `startActivity` isolado é reprodução comprovada.

**Política:** o emissor foi alinhado ao período anual de autorização observado no D1; isso não é quota de chamadas, e revogação permanece possível. O código de uso único expira em 10 minutos por padrão. Para expiração, emitir **novo código autorizado**, nunca reativar o antigo.

## 4. Portas de segurança / parada

- Sem aprovar a identidade exata da instalação atual: não fazer novo provisionamento.
- Sem pipeline de release assinado e verificado: não publicar APK/OTA.
- Sem readback das bindings, deployment e rollback: não trocar o Worker de produção.
- Sem testes físicos: não declarar links da TCL ou U01-U05/L03-L05 homologados.
- Progresso P0 mantém **7/11 = 63%**, gates F0–F8 `NAO_HOMOLOGADO`.

**Testes base antes do fluxo novo:** Android #40060106346, Worker #40060118239, companion TV #40060021841 — todos sucesso, mas não representam homologação física nem implantação do novo fluxo de ativação.
