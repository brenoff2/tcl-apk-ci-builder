# AnyTV 2.9.9 — reconciliação de release e evidências

**Objetivo:** entregar APK atualizável e estável, não repetir build/planejamento. **Estado da auditoria:** candidato em desenvolvimento, sem promoção comprovada para OTA.

## Base operacional
- Telefone instalado anteriormente informado como 2.9.8 `versionCode=37`, cert2026, OTA `updates/android/cert2026/current.apk`; TV Companion 2.5 (16).
- Código Discovery V3 inclui hero rotativo, carrosséis, busca global, filtros, trailers, notas e relacionados. Execuções e SHA devem ser ligados ao commit exato, não apenas nome do APK.
- Compilação 2.9.8/V3 aprovada: Actions `39690366612`; compilação de filtros aprovada: `39695232448`. Backend trailer fallback: `39696405547` aprovada.
- Tentativa de elevar para versão 2.9.9 (38): commit `1a2f52b678d68525def1e81f028b9c76d87b7fbe` em workflow de build. Actions `39699232465` **FALHOU no contrato de regressão**: `test_rescue_contract.py::test_identity_and_code` ainda espera versão 2.9.8 porque a variável `TCL_V37` continua ligada. Isso é uma inconsistência entre teste de migração e versão nova, não evidência de bug na UI. Reparar com gate `TCL_V38` e asserts específicos, preservar verificações anteriores.
- Arquivo `AnyTV_2.9.9_Discovery_V3_CERT2026.apk` **encontrado no Drive**, mas sem hash, cert verificado ou provenance nesta inspeção: **não marcar como pronto**. Metadados e URL em `docs/operations/GOOGLE_DRIVE_RELEASE_INVENTORY_20261021.md`.
- Novo workflow candidato `.github/workflows/anytv-discovery-v3-candidate.yml` criado no GitHub para build não assinado; verificar se pode ser disparado na branch e não presumir que 404 = sucesso.

## Gates técnicos e operacionais obrigatórios
1. CI verde para HEAD candidato com `versionCode 38` e pacote correto; auditoria de retrocompatibilidade de Bluetooth, remoto, lançamento e streaming.
2. Backend: novas rotas TMDB permitidas com filtros de segurança, cache, limites, chaves no Cloudflare; testes CI e readback de binding e versão antes de produção.
3. Determinar se o APK 2.9.9 do Drive deriva do mesmo código e build; auditar `aapt dump badging`, `apksigner verify --print-certs`, `zipalign -c 4`, SHA256 e correspondência origem.
4. Somente APK v2 assinado com identidade cert2026 e code 38; nunca distribuir unsigned nem publicar chaves. Comprovante de hash e leitura de volta do `current.apk` GitHub.
5. Atualizador no telefone só oferece quando code maior; validar atualização física sem limpar dados.
6. Gate de homologação: home global, trailer, filtro, relacionados, busca, abertura de título e regressão do controle/TV.

## Riscos e mitigação
- **Certificado incompatível:** Android rejeita instalação; bloquear.
- **APK de Drive não auditado:** arquivo não equivale a CI/release; bloquear.
- **Worker sem novas rotas:** V3 compila mas telas podem apresentar catálogo indisponível; publicar servidor somente após testes e readback.
- **P0 funcional regressar:** releases em fases e rollback por canal, mantendo artefatos anteriores.
- **Segredos**: material privado permanece fora do GitHub e de seus logs.
