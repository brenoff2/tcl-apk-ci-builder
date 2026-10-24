# ANYTV — PRIMEIRA MENSAGEM PARA O PRÓXIMO CHAT — 24/10/2026

**NÃO REINICIAR O PROJETO. NÃO GERAR IMAGENS. CONTINUAR O APP REAL.**

Estamos no PROJETO ANYTV, repositório privado https://github.com/brenoff2/tcl-apk-ci-builder, DEV feat/anytv-p0-device-proof-verifier-20261020, PR #3 DRAFT (head observado: 91a2b84a0f80f7ec3e643e97cf9c290e9d0be4ff). OTA na branch feat/tcl-v22-updates-back-bluetooth. Ler antes de agir o **handoff completo** docs/handoffs/2026-10-24-ANYTV-TRANSICAO-INTEGRAL-2924-2925.md, AGENTS.md, docs/roadmap/PROJECT_PROGRESS.json, docs/handoffs/CURRENT_HANDOFF.md, docs/qa/ANYTV_V2_2925_LINKS_CINEMA_20261024.md, docs/STATUS.md e os workflows atuais. Handoffs 2.9.22/2.9.23 anteriores são **históricos**, não a versão atual.

## RESUMO GRANDE DO OBJETIVO

AnyTV controla TCL/Android TV via LAN/TLS/pareamento, D-pad, volume, teclado IME, conexão/Bluetooth, abre apps de streaming, gerencia favoritos/assistidos/séries e exibe catálogo de filmes/séries TMDB com serviços reais no Brasil e links seguros pelo Worker Cloudflare. Usuário quer concluir o **aplicativo instalável**, sem ficar dando inúmeros "continue", e exige resultados efetivos com evidências de build/release/TV, não mockups. Identidade visual aprovada minimalista escura com vermelho, ícone de play/tela vermelho, nome AnyTV, telas de entrada e launcher consistentes.

## ESTADO OPERACIONAL (LER COM ATENÇÃO)

- **Última OTA V2 assinada PUBLICADA: 2.9.24/code53, pacote com.tclremote.livre.v2.** Workflow [#40095671540](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40095671540) SUCCESS, SHA256 APK f5827bc0d3e4da5fbf90f9fe772e2aba2890953c1f295db73613c6f638be49ae, Git blob 7a1bbdea9e9628ce15d3c1e9d2debef3c98824eb, 25.996.188 bytes, OTA updates/android/v2/current.apk. Certificado V2 original SHA256 fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9. Canal legado updates/android/cert2026/current.apk intacto.
- **Worker em produção:** anytv-link-resolver-pilot versão 58fe4b33-6ee2-4841-8587-c28acaaad435, deploy 25de8328-10ab-471a-8e94-fb4cdbce13d9, **100% do tráfego** (readback Cloudflare); rollback 58a1c0c3-eafc-406a-9961-9656a01e14ec; 5 bindings originais, device auth D1/cache e secrets preservados. Sem limites artificiais internos de uso, sem token fixo no APK.
- **CANDIDATA 2.9.25/code54 SOMENTE DEV — NÃO ASSINADA NEM PUBLICADA.** Inclui logos de todos provedores BR em linha rolável, Cinema TMDB now_playing region=BR com selo azul F Cinema, CTA Ingresso.com no detalhe (sem listagem interna de sessões por ausência de contrato), novo fluxo Android de pedido de acesso P-256 sem campo de código, com aprovação do operador no D1. CI backend [#40097287821](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40097287821) SUCCESS e Android anterior [#40097330186](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40097330186) SUCCESS. **CI Android último HEAD #40097870978 terminou SUCCESS** em readback do job (42 testes interface + 2 credenciais + 9 adicionais, Gradle BUILD SUCCESSFUL 4m37s, APK unsigned verificado). Prova de dispositivo #40097871008 SUCCESS. Nenhuma release code54 foi assinada/publicada.
- **Bloqueio exato da publicação 2.9.25**: workflow oficial protegido .github/workflows/release-anytv-phone-v2-new-package.yml + .github/anytv-phone-v2-build-request.json ainda estão para 2.9.24/code53; tentativa anterior de mudar workflow para code54 foi barrada pela política do conector. NÃO BURLAR; não assinar outro certificado ou subir unsigned. Quando permitido, promover uma só release assinada com SHA/OTA HTTPS byte-readback.
- **Ativação:** usuário enviou screenshot V2 com status não autorizado e um código que não funcionou. Histórico GitHub posterior registra emissão autorizada e **ativação bem-sucedida da instalação V2**, sufixo do identificador mascarado 3bede1c2, com prova P-256 e nonce aceitos; preservar registro anterior e **NÃO reutilizar, repetir ou gravar código secreto fornecido no chat**. Novo fluxo por solicitação assinada ainda está DEV e depende de aprovação manual da chave específica no D1.
- **TCL companion:** APK TV novo **NÃO FOI GERADO nem instalado** nesta rodada; última base técnica M03 TV 2.5/code16. Verificar versão/certificado instalados antes de mexer; a correção de ícones da tela principal foi feita no cliente Android com cache/retry, não significa que a TV foi atualizada.
- **P0:** 7/11 marcos técnicos = **63%**, todos gates F0–F8 **NAO_HOMOLOGADO**. Faltam M08/M09/M10/M11 e QA físico D01–D12/U01–U05. CI não prova reprodução real na TV.

## O QUE JÁ FOI FEITO E NÃO DEVE SER REFEITO

1. 2.9.22 (code51) e Worker sem teto interno de 200/mês e 20/min; autenticação e fornecedores externos mantidos.
2. 2.9.23/code52 OTA: cartões (ano+nota, assistido+favorito, F/S+provedores, título 3 linhas), ícones lista/grade, trailer só na ficha, resolvedor seguro, status assinado.
3. 2.9.24/code53 OTA: identidade vermelha/preta real (ícone, splash, logo, tema), botão Ver mais funcional, logos menores, espaçamento de títulos, Lembrar removido, barra sem botões inoperantes Do início/Episódios/Fones, cache/retry dos ícones da TCL.
4. DEV para 2.9.25: mostrar todos os provedores BR sem duplicação e sem precisar estar instalado, Cinema BR com selo azul e CTA oficial Ingresso.com sem prometer sessões, e fluxo assinado de solicitação de acesso P-256 sem código digitado. Worker correspondente já em produção, Android candidato ainda não no OTA.

## O QUE FAZER AGORA, SEM FICAR PEDINDO 'CONTINUE'

1. Partir do CI #40097870978 **SUCCESS** e HEAD 91a2b84, checar apenas se houver commits posteriores; não repetir testes por simples transição.
2. Verificar se os bloqueios de atualização do workflow podem ser resolvidos por rota **permitida** (nunca contornar políticas). Preparar assinatura V2 original para 2.9.25/code54, preservar legacy OTA e provar APK/sha/cert/byte readback antes de anunciar atualização.
3. Validar filtros Cinema vs streaming, selos F Cinema BR e provedores BR, CTA de sessões honesto, identidade visual/card/Ver mais e cache da TV em aparelho real.
4. Investigar companion TCL instalado (versão, pacote, signer) e atualizar **somente se necessário e compatível**, com QA e rollback, sem forçar substituição.
5. Validar links reais: autorização P-256, status, ficha específica, busca, abrir app e reprodução real por provedor, sem confundir níveis.
6. Atualizar documentação/progresso ao concluir, sem elevar gates físicos sem evidência.

**Regras:** ChatGPT Web/GitHub conectado, sem Codex obrigatório. Evitar invenções, pesquisas repetitivas, versões por documentação, mockups no lugar de APK, perda de contexto, segredos/códigos em logs. Não fazer merge automático nem destruir estado. Resposta inicial do novo chat deve mostrar SHA/branch, releases V2 e Worker, CI novo, progresso e o que fará para entregar o APK e homologar a TV.
