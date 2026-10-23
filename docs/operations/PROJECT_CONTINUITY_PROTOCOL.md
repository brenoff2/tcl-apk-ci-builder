# ANYTV — PROTOCOLO CANÔNICO DE CONTINUIDADE | 23/10/2026

Este documento substitui as instruções operacionais obsoletas de 21/10. A prova independente de transição entre chats continua PENDENTE até ser efetivamente executada em outro chat; jamais marcar PASS por editar documentação.

## Rota obrigatória ao iniciar cada conversa

1. Acessar repositório canônico https://github.com/brenoff2/tcl-apk-ci-builder e consultar HEAD da branch dev feat/anytv-p0-device-proof-verifier-20261020 (PR #3 draft) e da branch OTA feat/tcl-v22-updates-back-bluetooth. Não usar main como se fosse a OTA.
2. Ler AGENTS.md, docs/handoffs/CURRENT_HANDOFF.md, **docs/handoffs/2026-10-23-anytv-full-transition.md**, docs/VISION.md, docs/roadmap/MASTER_ROADMAP.md, docs/roadmap/PROJECT_PROGRESS.json, docs/STATUS.md, docs/decisions/ADR-0001.md, ADR-0002-DISCOVERY-V3.md e **ADR-0003-ANDROID-CERT2026-GITHUB-ONLY.md**.
3. Ler docs/ASSINATURA_E_ATUALIZACAO.md, signing-recovery/README.md e os DOIS workflows ativos do Android na branch OTA: .github/workflows/publish-anytv-phone-github-only.yml e .github/workflows/verify-anytv-update-endpoint.yml.
4. Fazer readback OTA real, versão, SHA/certificado e últimos GitHub Actions: build 2.9.13 run 39891891049 SUCCESS; publisher run 39963496459 FAILURE no gate de segredo; verificador da OTA/rota única 39963760834 SUCCESS. Esses IDs são marcos históricos, conferir jobs mais recentes.
5. Informar estado factual e executar o primeiro passo real disponível, sem rodar pesquisa/assinatura/build já concluídos e sem fazer o usuário repetir histórico.

## Identidade e escopo

Celular AnyTV com.tclremote.livre; companion AnyTV Connect com.tclremote.tvcompanion; controle remoto TLS LAN, Bluetooth, inventário de apps, favoritos, streaming real, Discovery Home V3, Cloudflare Worker/D1. Não refazer a arquitetura, não quebrar experiência/abertura de títulos que já funcionavam; preservar certificados e ativação. Rota do telefone: updates/android/cert2026/current.apk (certificado SHA256 2f14e43f7653a75f878c4d59a4a8ad462ade17c52bde23e802c8c6ed43ade95f). Canal TV separado. Sem API keys no APK.

## Fluxo único e falha segura

O usuário exige atualizar pelo BOTÃO no aplicativo, a partir do GitHub, sem Drive. Só .github/workflows/publish-anytv-phone-github-only.yml deve assinar/publicar Android cert2026. Fonte: artifact unsigned do Actions, identidade original no backup cifrado GitHub, e Actions Repository Secret ANYTV_RECOVERY_SECRET_2026. Se secret ausente/sem acesso no runner, NUNCA criar segundo workflow, usar URL temporária, gravar plaintext ou rodar outra rota; parar antes da OTA, identificar de forma segura o acesso administrativo necessário. Verificar candidato e versionCode monotônico, pacote, cert, assinatura v2 e readback byte a byte. Compilação != assinado != OTA != instalado != homologado fisicamente.

## Estado deste checkpoint

A OTA efetiva do telefone é 2.9.12/code41, 28.347.236 bytes, SHA256 28c2881899bc9c6a79fcfd908d66528750534c89c7f334b8cc57096f80cf63f3. A 2.9.13/code42 tem build 39891891049 PASS e APK privado assinado SHA256 fbbc1398465f27b2aadccef1d48be6e08e51af0b729aab694189ca8c06b3c1f5, mas NÃO está na OTA. Pareamento real TCL falhou em 2.9.11 e 2.9.12; 2.9.13 é hipótese de isolamento RSA/P256 sem QA físico. Não pedir que o usuário reinicie a TV como substituto de diagnóstico nem limpar dados sem evidência. Status/cronologia integral na transição 2026-10-23.

## Governança e progresso

F0–F8, M01–M11 e percentual são próprios deste projeto (baseline atual 7/11, 63% marcos históricos, gates humanos 0/9). Nunca usar percentual da Fábrica de Projetos. Manter PR #3 draft; sem merge automático. Github-first, diffs cirúrgicos, logs e provas em hardware. Atualizar canônico após toda mudança e encerrar com Acompanhamento — ANYTV. A prova independente do novo chat deve ser registrada em docs/handoffs/CONTINUITY_CHECK.json SOMENTE se executada e verificada.
