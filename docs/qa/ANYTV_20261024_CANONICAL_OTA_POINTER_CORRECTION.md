# AnyTV — saneamento dos ponteiros canônicos de versão OTA V2 (24/10/2026)

**Operação documental, sem mutação de APK, código Android, Worker, D1 ou OTA.** Repositório brenoff2/tcl-apk-ci-builder, PR #3 continua aberto em draft, branch DEV feat/anytv-p0-device-proof-verifier-20261020.

## Divergência identificada

A documentação histórica e as últimas entradas do histórico de PROJECT_PROGRESS.json já reconheciam **2.9.25/code54 assinada e publicada**, mas os campos ativos **v2_current**, **operational_checkpoint.current_v2** e **v2_candidate_next** ainda reproduziam o checkpoint anterior: apontavam 2.9.24/code53 como OTA vigente e 2.9.25/code54 como candidata unsigned não publicada. O campo **v2_previous** permanecia em 2.9.23/code52, criando duas versões de defasagem. A leitura automática do JSON raiz podia, portanto, contradizer a evidência assinada.

## Autoridades e readback independente

- [Release assinado #40106161667](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40106161667): COMPLETED/SUCCESS, com signer original, verificação do APK, auditoria de identidade, publicação no canal OTA V2 e comparação HTTPS byte a byte. Evidência de release já aprovada em [ANYTV_2925_CODE54_SIGNED_OTA_RELEASE_20261024.md](ANYTV_2925_CODE54_SIGNED_OTA_RELEASE_20261024.md).
- Versão atual **2.9.25, versionCode 54, com.tclremote.livre.v2**, assinada, SHA256 **c53596d8f8210bf0a12769481796481a851d00da4c52f544120b85367dd17fe0**, certificado original **fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9**.
- [Commit OTA dcc51bad](https://github.com/brenoff2/tcl-apk-ci-builder/commit/dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112): **updates/android/v2/current.apk**, blob **917b2f31191b93f63e7ee121f3d7d90d3c8e04e6**, **26.012.572 bytes**, verificado na árvore Git.
- Versão anterior **2.9.24/code53**, commit OTA anterior **75aa42dff1e8745ba8721fbc0f87c5b8e539cf23**, blob **7a1bbdea9e9628ce15d3c1e9d2debef3c98824eb**, **25.996.188 bytes** (também conferido na árvore Git).
- O APK histórico legado cert2026 permanece fora dessa atualização; o Worker Cloudflare não sofreu deploy nesta jornada.

## Saneamento com histórico preservado

1. **v2_current** e **operational_checkpoint.current_v2** atualizados para release assinada 2.9.25/code54 com run, assinatura, SHA256, blob, OTA commit, tamanho, prova readback e gate físico pendente. O handoff ativo em operational_checkpoint aponta para o documento pós-2.9.25.
2. **v2_previous** atualizado para 2.9.24/code53, sem apagar a fotografia antiga de 2.9.23/code52; há cópia íntegra no campo v2_previous_historical_2026_10_24_2923.
3. O checkpoint anterior inteiro (code53) foi arquivado sem perda em operational_checkpoint_historical_2026_10_24_code53; a antiga candidata 2.9.25 pré-publicação foi arquivada integralmente em v2_candidate_historical_2026_10_24_pre_2925_release.
4. **v2_candidate_next** não declara uma 2.9.26 fictícia: versão e versionCode ficam **não atribuídos** (nulos) até um novo release ser aprovado. O campo documenta a fonte DEV atual, o último CI **unsigned #40136181568**, Worker CI **#40136181610**, prova isolada **#40136181616**, e que code54 já pertence à OTA assinada.
5. O aviso **discovery_v3.note** indica explicitamente que referências antigas a OTA 2.9.8/2.9.12 são históricas e que o estado operacional é definido por v2_current e operational_checkpoint.
6. Nenhum milestone foi promovido: **7/11 (63%)**, M08/M09 **IN_QA**, M10/M11 **BACKLOG**, F0–F8 **NAO_HOMOLOGADO**. QA físico Android/TCL, autorização e deploy do endpoint Worker movie/{id} permanecem pendentes.

## Testes documentais e efeito operacional

Validação por JSON.parse + assertions em estrutura e proveniência, incluindo cópias históricas idênticas às anteriores, versão atual 54 e anterior 53, candidata sem nova versão inventada, SHA/blob/tamanho verificáveis, Worker em produção intocado e gates preservados.

**Não** reinicia CI Android, modifica o APK público, instala no telefone ou altera assinaturas. Qualquer futura publicação exige versãoCode >54, certificado original, autorização explícita e homologação relevante. Não houve deploy Worker nem merge do PR #3.
