# AnyTV Android V2 — recuperação code92 (plano, NÃO executado)

**Estado:** DESIGN_ONLY / NO_SIGNING / NO_OTA_WRITE / NO_MAIN_WRITE. Escopo: aplicativo Android V2 `com.tclremote.livre.v2`; Samsung e TV fora do escopo.

## Por que rollback da OTA não instala code90 depois de code91

- Baseline oficial: `2.9.62-AnyTV`, versionCode **90**, APK assinada em `updates/android/v2/current.apk` na branch `feat/tcl-v22-updates-back-bluetooth`, commit observado `93e526e5a532724f139208ff042d1a92056f7c91`.
- Candidate: `2.9.63-AnyTV`, versionCode **91**, assinatura e publicação ainda **NÃO executadas**.
- Android compara `applicationId`, assinatura e `versionCode`. Após code91 instalado, simplesmente publicar novamente code90 no canal OTA constitui downgrade e normalmente falha. Reverter ponteiro OTA pode impedir novas instalações da code91, **mas não recupera aparelhos já atualizados**.
- A recuperação instalável após code91 exige novo `versionCode` maior: **92**, com package `com.tclremote.livre.v2`, certificado V2 idêntico e conteúdo funcional de baseline previamente aprovado. Não renomear arquivo code90 para code92: reconstruir e testar APK.

## Fonte de recuperação e auditabilidade

1. Preservar commit **imutável** da fonte/receita de build de code90 (não presumir que o `HEAD` futuro da branch OTA representa a fonte anterior). Preservar hash SHA-256 de `updates/android/v2/current.apk` code90, auditado por `apksigner` e `aapt`, e registros do certificado público `signing-recovery/v2/identity.json`. O blob Git `eb922fbc4485e982afe07cce7d5fce2c3e3d1376` identifica o arquivo OTA no último inventário, **não é hash SHA-256 do APK**.
2. Determinar e congelar fonte exata/receita de build que produziu code90, incluindo upstream pinado, overrides, scripts, ferramentas e testes; se a proveniência não puder ser estabelecida, registrar `RECOVERY_SOURCE_UNVERIFIED` e **não autorizar** rollback code92 automático.
3. Preparar **separadamente** um workflow de build code92 com assinatura V2 existente (backup criptografado e GitHub secret), sem copiar material privado. Substituir apenas a versão Android `versionCode = 92`, `versionName` novo a aprovar, **sem alterar** applicationId ou certificado.
4. Validar offline compatibilidade do APK, PackageManager, atualização de 91 para 92 e funcionalidades essenciais (catálogo, login, conexão, reprodução, atualizador). Preferir smoke em dispositivo real; simulação ou teste estático não substitui QA de atualização instalada.
5. Se o code91 já estiver no canal, exigir comparação exata do HEAD OTA atual e assinatura/versão code91 antes do push de code92; nunca reutilizar um gate `oldcode == 90` feito para o release code91. O pipeline code92 deve ter gate próprio, aprovador próprio e readback dos bytes publicados por commit imutável.
6. `rollback` do backend Cloudflare é **independente**: versão 27 `88779b1b-9dbd-4dc6-9884-eff52769ded2` só é relevante para o Worker, não recupera a UI Android.

## Portões fail-closed para qualquer recuperação code92

| Condição | Resultado se não comprovada |
| --- | --- |
| Aprovação humana expressa para recuperação V2 Android | `NO_DEPLOY` |
| Fonte code90/receita de build comprovada, sem alterações não auditadas | `RECOVERY_SOURCE_UNVERIFIED` |
| Identidade `com.tclremote.livre.v2`, certificado idêntico, versionCode 92 | `SIGNATURE_OR_VERSION_MISMATCH` |
| Branch OTA e código instalado, com readback code91 | `STALE_OTA_STOP` |
| Testes críticos e plano de preservação de dados | `QA_REQUIRED` |
| APK code92 assinada, arquivada antes de push; readback imutável posterior | `DELIVERY_UNVERIFIED` |

**Não criar workflow code92 executável nem publicar ou assinar rollback enquanto a identidade exata da fonte original e os gates não estiverem comprovados.**

## Como tornar o workflow code91 acionável sem alterar a main agora

- O repositório informa `default_branch=main`. A rota `workflow_dispatch` exige definição do workflow no branch default; `GET /contents/.github/workflows?ref=main` retornou **404**, logo não há evidência do registro de workflows nessa branch. `GET /branches/main/protection` retornou **403** para este conector, então status de proteção e autorização de edição são **DESCONHECIDOS**.
- Estratégia **futura e separadamente revisada**: adicionar à `main` um arquivo de workflow manual minimalista para a mesma identidade/caminho e submeter a revisão/PR. Provar qual commit de `ref` o GitHub Actions efetivamente executa; a decisão de release não pode ficar apenas no texto do prompt. Não confundir registro em main com autorização de publicação.
- Exigir que o dispatcher aponte exatamente a branch/commit DEV aprovada, use `release_confirmation=PUBLISH_ANYTV_V2_CODE91`, `expected_source_head` de 40 hex, `expected_ota_head` de 40 hex e proteja entradas em `env`, não no shell diretamente.
- Antes de acionar, validar que a workflow `main` não executa por `push` ou PR, permissões GitHub/branch rules foram revisadas, o certificado code90 permanece correto e o operador autorizou especificamente publicação code91. **Não publicar nem alterar `main` neste checkpoint.**
- A versão assinada, se produzida, deve ser arquivada **antes** da gravação na OTA. Atenção: depois do `git push`, falha no readback não desfaz o push automaticamente; operação vira `PUBLISHED_UNVERIFIED` e requer reconciliação imediata de branch e bytes, não um novo push às cegas.

## Referências verificadas

- `.github/workflows/anytv-v2-code91-signed-release.yml` (DEV; manual-only; não acionado).
- `.github/workflows/anytv-v2-code91-release-contract-ci.yml` e `scripts/tests/test_anytv_code91_release_contract.py` (checks estáticos).
- GitHub Actions `42538194988` SUCCESS (estático); `42537392687` SUCCESS (read-only de certificado code90).
- Oficial Android: documentação sobre `versionCode` e atualizações `https://developer.android.com/studio/publish/versioning`.
- Oficial GitHub Actions: `https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow` e `https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_dispatch`.
