# AnyTV LG code103 — preflight de candidata assinada para QA física (SEM EXECUTAR)

Data: 10/11/2026. Estado: **PREFLIGHT DOCUMENTAL — ASSINATURA NÃO AUTORIZADA / NÃO EXECUTADA / OTA BLOQUEADA**.

## 1. Fonte congelada e verificada

- Repositório `brenoff2/tcl-apk-ci-builder`, branch DEV `feat/anytv-p0-device-proof-verifier-20261020`.
- CI unsigned `42897158122`: COMPLETED/SUCCESS; head SHA **`55aa5b9a119d369a0ed604c0de1cc19435063e9f`**, com testes de contratos, compile, identidade e upload aprovados.
- Artefato unsigned GitHub **`13801576770`**, nome `anytv-lg-webos-code103-pointer-unsigned-audit`; ZIP SHA-256 **`6c50d5bc97584a9f16f9938c409eeea8f78d4ecf2c8b9f7570aa7e0006245ff0`**. APK interno SHA-256 **`fb9b4c737a6ae36d3d922884aab22afaf134dd4ca9b7b344d72f8c76539cb0c1`**, conferido por download real e `candidate-sha256.txt`.
- Package **`com.tclremote.livre.v2`**, versionCode **103**, versionName **`2.9.75-AnyTV`**. O APK **unsigned não pode ser instalado sobre o V2 assinado**.
- OTA pública V2: **code102/2.9.74**; branch `feat/tcl-v22-updates-back-bluetooth`, HEAD **`c52395b67c6fd56777852ea09131e9fcf507617f`**. Não escrever em `updates/android/v2/current.apk` nem em `updates/android/cert2026/current.apk`.

## 2. Descoberta: workflow assinado existente NÃO corresponde a esta revisão

`.github/workflows/anytv-lg-code103-signed-qa.yml` (blob `860253fab7561134e50ad7d255185dd04c8d2fa9`) é um **one-shot antigo**: faz checkout explícito em `0ac441b9eeb4fecf5a25b8981b376e646ff21595`, valida o run `42867318391`, head SHA `057c5faf6b0716e2abe703058047c919c7bd9046` e hash antigo `d601dee070446876c65e53c4c1371cfa6d3ae4034f660eb550417f75865580fc`. Consequentemente **não serve de prova para o code103 atual**, mesmo que o nome da versão coincida. Um candidato assinado mais antigo (run `42868252718`) tampouco contém as correções atuais.

**Atenção ao gatilho:** o YAML assinado é acionado por push na branch DEV **quando o próprio arquivo é modificado**. Alterá-lo agora poderia iniciar assinatura automaticamente. **Não editar/não executar** sem autorização explícita para candidata de QA e revisão prévia do gatilho/fluxo.

## 3. Identidades de assinatura que NÃO devem ser confundidas

- Identidade V2 para `com.tclremote.livre.v2`: fingerprint SHA-256 **`fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`**. Material criptografado em `signing-recovery/v2/phone-signer.aesgcm` e identidade pública em `signing-recovery/v2/identity.json`; recuperação autorizada somente dentro de GitHub Actions via Repository Secret `ANYTV_SIGNER_V2_RECOVERY_HEX`, sem revelar valor. Não regenerar signer.
- A antiga identidade `cert2026` do pacote `com.tclremote.livre`, fingerprint `2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f`, é outro aplicativo/canal: **não usá-la para V2**.
- Fonte decisória: `docs/decisions/ADR-0004-NEW-ANDROID-SIGNER-V2-GITHUB-CUSTODY.md` e `docs/operations/ANYTV_V2_SIGNER_RECOVERY_AND_RELEASE_RUNBOOK.md`. `docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md` e ADR-0003 tratam principalmente da antiga linha cert2026, não prevalecem para o pacote V2.

## 4. Gates obrigatórios para eventual QA física *após autorização*

1. Autorização explícita e escopo **QA privada**, separados de qualquer autorização de OTA/publicação, com origem e destino do APK restritos a artifact Actions; não editar/pushar YAML de assinatura como efeito colateral desta auditoria.
2. Se um novo fluxo for autorizado, validar de forma imutável o **run `42897158122`**, o head SHA **`55aa5b9a...`**, a branch, a conclusão SUCCESS, o artefato único **`13801576770`**, SHA-256 ZIP e SHA-256 APK **antes** de usar o signer V2 existente. Não usar `42867318391`, nem o APK assinado da revisão anterior.
3. Assinatura somente dentro do runner autorizado, com recuperação privada e limpeza de chave temporária. Comparar fingerprint do APK assinado com a V2 acima, package V2, versionCode=103, versionName=2.9.75-AnyTV, alinhamento e esquema v2; conferir checksum do APK assinado. Fail closed por ausência/divergência de secret, digest ou pacote. Nunca enviar credenciais pela conversa.
4. Artefato assinado de QA deve ser separado da OTA, com README explícito `NO OTA / NO AUTO-INSTALL`, retenção de evidência e nenhuma mutação de branch OTA, Worker, D1, main, companion TV ou cert2026.
5. Teste físico controlado com versão/hash/fingerprint registrados na LG real, seguindo os dez casos LG01–LG10 no adendo do plano `docs/qa/ANYTV_V2_2.9.25_DEVICE_HOMOLOGATION_PLAN_20261025.md`. Resultados separados: logo, controle, tela do app, ficha correta e reprodução. `SSAP ACK` comprova apenas aceite do comando. Não marcar F0–F8 nem LG01–LG10 PASS automaticamente.

**Estado de decisão neste checkpoint:** unsigned CI PASS; análise de assinatura **documental** concluída; candidata assinada atual **NÃO PRODUZIDA**; homologação real **NÃO INICIADA**; GOAL 7/11 (63%) e F0–F8 não homologados. Próxima ação segura: conferir se há uma rota de QA privada explicitamente autorizável sem gatilho implícito e sem tocar OTA, ou solicitar aprovação específica antes de qualquer execução que use o signer.
