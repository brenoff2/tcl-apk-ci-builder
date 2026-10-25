# AnyTV 2.9.27 / code56 — assinatura e publicação OTA comprovadas

**25/10/2026 | Resultado: RELEASE SUCCESS.**

## Evidências
- Fonte DEV PR #3 draft, trigger `99ed5ef62552e9e8a028f1e5c6848cabf46dadcd`, workflow `.github/workflows/release-anytv-phone-v2-new-package.yml`.
- GitHub Actions [#40197414429](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40197414429): **completed / success**.
- Gates PASS: contratos de streaming, segurança e identidade; Gradle `assembleRelease`; IME Remote; teste live TMDB Filme/Série; `aapt` package `com.tclremote.livre.v2`, `versionCode=56` e `versionName=2.9.27-AnyTV`; recuperação do **certificado original V2** de backup criptografado, zipalign, apksigner, publicação OTA e HTTP byte readback.
- Hash SHA256 **do APK assinado** no log de CI: `983281b3c55371e2fa291e19f6e5c436f9d0c9e4168604a0b886b201ac286373` (`ANYTV_V2_SIGNER_VALIDATED_...`).
- Commit na branch OTA `feat/tcl-v22-updates-back-bluetooth`: **`6b85cfb`** `release(anytv-v2): verified signed V2 2.9.27 OTA update`.
- Arquivo publicado: `updates/android/v2/current.apk`. GitHub compare de `91837ae` para branch OTA mostrou **apenas `updates/android/v2/current.apk` modificado**.
- Confirmação byte a byte: `ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS` em 25/10/2026 13:29:12Z.
- Artefato `anytv-v2-2927-signed-update`, id **12600209246**, no workflow acima.
- CI DEV unsigned anterior: [#40197057492](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40197057492) **SUCCESS**: contratos de streaming, identidade/code54 de auditoria e Gradle. Esta é uma compilação de teste anterior, não a versão assinada publicada.

## Escopo visual desta versão
Topo Filmes/Séries + ações numa única linha; cards compartilhados 3 por fila, pôster original 2:3 sem crop, nota com estrela amarela sombreada no canto superior esquerdo, ações assistido/coração no canto superior direito, linha F/S, ano, ícones de streamings confirmados, nome em 2 linhas; heros com metadados sobre imagem sem rodapé repetido; home com um hero Cinema BR, seções separadas por barra vermelha, sem CTA de seção Ver mais; histórico combinado de filmes e séries assistidas com dados locais válidos; busca local automática e compacta em Minha lista/Séries acompanhadas/Filmes e séries assistidos; modal de filtros mantém colunas Streamings/Gêneros e ações arredondadas.

## Limites
- **Android/TCL físico não homologado nesta execução.** Screenshots e interação devem ser conferidos no telefone atualizado antes de promover gates M08/M09 e F0–F8.
- Worker publicado **não recebeu** `movie/{id}`; D1, companion TCL, OTA legado, regras de identidade/certificado e PR #3 não foram alterados por este release.
- Progresso formal **7/11 (63%)**: M08/M09 IN_QA, M10/M11 BACKLOG, F0–F8 NAO_HOMOLOGADO, até validação em equipamentos reais.
- Para atualização: AnyTV > Configurações > Buscar atualizações. Manter package V2 e assinatura original. Não instalar APK unsigned.

**Evidência é de CI, assinatura e readback remoto; não alegar instalação no celular.**
