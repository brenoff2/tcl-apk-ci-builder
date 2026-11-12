# TCL Android TV — prova ADB isolada de inventário, ícones e abertura (12/11/2026)

## Ready-for-QA — candidato PHONE DEV (atualizado 12/11/2026)

**Estado: BUILD_READY_UNSIGNED / DEVICE_QA_PENDING / RELEASE_BLOCKED.** Este documento agora abrange duas etapas independentes: (a) a prova inicial da bancada (PC com ADB) e (b) uma implementação opcional ADB no **celular** em DEV, que não foi instalada na TCL/celular. A observação histórica abaixo sobre "não há integração Android" se refere SOMENTE à etapa (a).

**Proveniência verificável:**
- branch: \`feat/anytv-p0-device-proof-verifier-20261020\`; source run commit \`293225f81531ed8e9db79eafdfda44bd1fcddd64\`.
- workflow: \`.github/workflows/anytv-tcl-adb-phone-unsigned-ci.yml\`; GitHub Actions **43258575485 COMPLETED/SUCCESS**, job 131323400978, todas as etapas PASS (preparação, contratos, Gradle, auditoria do unsigned, upload).
- artifact **13962632327** \`anytv-tcl-adb-phone-code103-unsigned-audit\`; ZIP digest \`sha256:98315b31d0250afb3d2d28d8e2a87aff662dfa6d2710e57420c00fb1fddba448\`; **não assinado; não é atualização instalável sobre o V2 assinado.**
- Código: \`overrides/TclAdbInstalledApps.kt\`, aplicado somente pelo overlay DEV \`scripts/apply_anytv_tcl_adb.py\` após a preparação normal. \`scripts/tests/test_anytv_tcl_adb_phone_contract.py\` cobre opt-in, inventário, autorização e limites em modo **sintético/estático**. Biblioteca upstream \`com.tananaev:adblib:1.3\` adicionada somente no overlay.
- Preservados: companion, LG, main, Worker/D1, OTA code103, assinaturas/certificados, produção.

### Contrato QA físico e de aceitação (sem tarefas manuais de extração de APK)
| Gate | Exercício no celular/TCL | Aprovação necessária |
|---|---|---|
| TCL-M1 — Segurança/autorização | TV com depuração voluntariamente ativa; celular e TV na LAN privada; habilitar ADB Wi-Fi no AnyTV; observar e autorizar a **nova chave do celular** no aviso da TCL | Conexão do celular autorizada, nenhum bypass, nenhuma exposição pública da porta 5555 |
| TCL-M2 — Inventário real | Refresh na TV conectada, apps do launcher (pré-instalados e de terceiros); instalar/remover um app na TV e atualizar | Variações reais refletidas; nenhum serviço Android tratado como aplicativo; catálogo fixo nunca usado como inventário |
| TCL-M3 — Identidade gráfica | Verificar nomes e ícones na tela do celular para Spotify, Netflix, Prime Video e pelo menos um app com recursos split/adaptive | Correspondência visual com launcher TCL, sem ícone fictício; ausência/erro exibida com status claro |
| TCL-M4 — Lançamento | Tocar Spotify, Netflix, Prime Video na lista do celular | App correto visível na TV; ACK \`Events injected: 1\` isolado não basta |
| TCL-M5 — Reconexão | Encerrar app, desconectar Wi-Fi, religar, reiniciar TCL e celular com depuração sob controle humano | Volta a funcionar sem computador, quando adbd TCP permanece ativo; se não, erro explícito sem reativar depuração silenciosamente |
| TCL-M6 — Regressão | Desativar opt-in ADB, exercitar fluxo companion e controle remoto TCL; navegar LG | Funcionalidade anterior não degradada; nenhum OTA ou TV APK alterado |

### Riscos concretos ainda pendentes antes de assinatura privada
1. A autorização RSA aceita no computador **não autoriza** automaticamente a chave gerada pelo AnyTV no celular. QA precisa confirmar popup e persistência da identidade do celular.
2. O firmware Android TV 9 pode não suportar ou pode responder em formato diferente ao comando \`cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER\`: nesse caso o protótipo falha fechado, sem inventar apps. Exigir inspeção da saída **pelo próprio app**, não instruir usuário a extrair arquivos.
3. Resolver ícones e labels a partir de \`PackageManager.getResourcesForApplication(applicationInfo)\` com caminhos locais de base/splits ainda **não foi validado em runtime**; manifests, ícones adaptativos, referência por locale/densidade e pacotes com recursos ausentes podem falhar. PNG genérico não pode ser falsamente apresentado como ícone original.
4. Transferência temporária de APK pelo ADB \`sync:RECV\` é feita internamente no celular (não manual); verificar tempo, memória, peso de apps e limpeza dos temporários. Segurança: ADB TCP legado sem criptografia; redes confiáveis apenas. Chaves RSA em storage privado requerem revisão de threat model antes de assinatura.
5. \`shell monkey\` confirma comando, não resultado visual. Limite de uso/estado em TVs após reboot e possível desativação de adbd TCP permanecem desconhecidos.
6. Workflow é **unsigned** e seu artifact não deve ser instalado como update do app assinado já existente. Assinar QA privada, publicar OTA e remover companion dependem de aprovações separadas.

**Critério de passagem:** somente após TCL-M1..M6 com evidência física; no presente estado = **QA PENDENTE** e release BLOQUEADO.



**Escopo:** DEV / QA, **sem APK auxiliar novo, sem OTA, sem assinatura, sem alterações de TV em produção**. Não há integração dentro do aplicativo Android AnyTV nesta etapa. O objetivo é comprovar as três capacidades por meio de uma estação de teste previamente autorizada, antes de escolher o transporte ADB adequado ao celular.

## Gate e evidência prévia

- Usuário autorizou **estudar e preparar** o teste ADB pela rede com pareamento aprovado na própria TCL. Não autorizou remover o companion, publicar OTA ou abrir depuração sem consentimento na TV.
- Metas cumulativas: inventário real de apps; nome/ícone do mesmo pacote instalado; abertura do pacote escolhido **visualizada fisicamente** na TCL.
- O protocolo Android TV Remote v2, já usado pelo telefone, permite tentativa de abertura via `RemoteAppLinkLaunchRequest`, mas **não enumera todos os pacotes e ícones**. O `RemoteAppInfo` exibido em mensagens de estado não é inventário geral.
- **Fonte oficial:** https://developer.android.com/tools/adb (comandos `adb shell pm list packages`, `pm path`, depuração via rede). Para TVs, pareamento sem fio por código é documentado para Android **13+**; versões anteriores podem exigir autorização inicial por USB, métodos do fabricante ou não suportar o fluxo. Não presumir suporte sem verificar versão exata.
- Complemento upstream: https://github.com/tronikos/androidtvremote2 e https://github.com/sonalder-darlene/libremote-android-tv/blob/main/PROTOCOL.md

## Prova de bancada segura, em máquina com Android SDK Platform Tools + Build Tools (`adb` e `aapt`)

1. Verificar com o proprietário o modelo e a versão Android TV, ativar conscientemente a depuração **somente na rede confiável**, parear/autenticar a estação pela **tela da TCL**. Não gravar IP, porta ou códigos no GitHub.
2. Confirmar `adb devices`: dispositivo aparece com status `device`; `unauthorized` e `offline` são bloqueios. `adb pair IP:PORTA` quando realmente suportado pela TV, seguido de `adb connect IP:PORTA_DE_DEPURACAO` quando necessário, com autorização local.
3. Somente inventário (não abre nem copia apps):
   `python3 scripts/qa/tcl_adb_apps_probe.py --serial IP:PORTA --output /caminho/qa-tcl`
4. Testar nomes/ícones de pacotes **constatados no inventário**, por exemplo:
   `python3 scripts/qa/tcl_adb_apps_probe.py --serial IP:PORTA --output /caminho/qa-tcl --inspect com.netflix.ninja`
   O script consulta `pm path`, copia temporariamente APKs dessa TV para a máquina, lê `aapt dump badging`, salva apenas ícones PNG/WebP referenciados diretamente. APKs temporários são eliminados ao terminar. **Recursos adaptativos XML, ícones em splits dinâmicos e recursos não acessíveis podem permanecer `UNSUPPORTED_RESOURCE_OR_SPLIT`**; nunca apresentar ícone presumido como extraído.
5. Abertura **apenas após escolha explícita do pacote e autorização**:
   `python3 scripts/qa/tcl_adb_apps_probe.py --serial IP:PORTA --output /caminho/qa-tcl --launch com.netflix.ninja --confirm-launch`
   O comando `monkey -p PACKAGE -c android.intent.category.LEANBACK_LAUNCHER 1` pode reportar envio, mas **não prova a tela exibida**. Registrar foto ou observação visual independente, sem credenciais.
6. Após testes, desabilitar depuração da TV/revogar autorizações não necessárias. O arquivo local `inventory.json` contém nomes dos pacotes; inspecionar antes de compartilhar. Não publicar APKs extraídos nem endereços/IPs.

## Critérios para a futura integração de celular

| Gate | Evidência exigida | Situação |
| --- | --- | --- |
| TCL-A1 | Android TV real autorizada e inventário íntegro, incluindo apps novos | NÃO TESTADO |
| TCL-A2 | Nomes e ícones correspondentes a **todos** os apps que a interface promete listar (inclusive adaptativos/split) | NÃO TESTADO |
| TCL-A3 | Cada app selecionado inicia de fato e sua tela é observada na TV | NÃO TESTADO |
| TCL-A4 | Pareamento/reconexão seguros, isolamento por TV, comportamento após reboot e revogação | NÃO TESTADO |
| TCL-A5 | Implementação nativa no celular, compilação e testes sem regressão LG/TCL | NÃO IMPLEMENTADO |

**Nota:** este script de bancada **não é** integração de ADB ao APK Android do celular. Ele verifica a viabilidade e revela limitações de firmware antes de adotar uma biblioteca ADB Android (sem exigir computador no uso final). Não remover o APK auxiliar nem desativar seus fluxos enquanto TCL-A1..A5 não passarem.

## Segurança e isolamento

Nenhum `adb tcpip 5555` automático; nenhum pareamento ou consentimento automatizado; nenhum `adb install`, `adb uninstall`, `pm clear` ou modificação de credenciais. A única ação mutante da prova é o `--launch` explícito. O script não se conecta ao Worker/D1/OTA e não modifica o repositório/TV companion.

## Diagnostico TCL Android TV 9 com somente "Depuracao USB" (12/11/2026)
- Fato relatado pelo proprietario: a TCL usa Android TV **9** e so mostra **Depuracao USB** nas opcoes de desenvolvedor. Modelo exato, firmware e estado da porta ADB sao desconhecidos.
- Fonte oficial Android ADB: https://developer.android.com/tools/adb#wireless e Google Android TV https://developers.google.com/cast/docs/android_tv_receiver/debugging : para Android <=10, a rota documentada em aparelhos retail e autorizacao USB inicial, depois `adb tcpip 5555`, e `adb connect TV_IP:5555`. A TV pode nao fornecer porta USB configurada como device/gadget, portanto nao prometer conexao por cabo sem confirmar hardware. Nao utilizar pareamento por codigo `adb pair` de Android 11+ neste Android 9.
- Alguns firmwares TCL podem ativar ADB TCP 5555 com a chave "Depuracao USB" sem exibirem "Depuracao pela rede". Essa possibilidade e relatada por terceiros, nao e garantia oficial para o modelo especifico. Primeiro teste autorizado somente em rede privada: habilitar temporariamente USB Debugging na TV e, da estacao confiavel com Platform Tools, executar `adb connect TV_IP:5555` e `adb devices`. Aceitar prompt de autorizacao reconhecido na TCL. Se receber `connection refused` / `timed out`, a porta nao esta comprovadamente habilitada; nao tentar menus de servico ocultos, exploits, root, forwarding WAN ou `setprop` sem autorizacao e evidencia.
- A alternativa USB fisica deve ser considerada apenas apos modelo/firmware/portas confirmados; o fluxo `adb tcpip 5555` usa host ADB ja autorizado. Evitar cabos USB-A para USB-A entre duas portas host ou energia potencialmente perigosa.
- Qualquer sucesso de conexao ADB e apenas TCL-A0, NAO comprova TCL-A1 inventario, TCL-A2 icones, TCL-A3 lancamento, TCL-A4 reconexao e TCL-A5 integracao no celular. Preservar companion e OTA.


## Relato de QA fisica TCL — Spotify (12/11/2026)
- Usuario forneceu saida real `adb devices`: `IP_PRIVADO:5555 device` em Android TV 9; ADB TCP autorizado e conectado na LAN.
- Saida real `adb shell pm list packages -3`: 17 pacotes; consulta sem filtro adicional identificou Netflix (`com.netflix.ninja`), Prime Video (`com.amazon.amazonvideo.livingroom`), YouTube TV (`com.google.android.youtube.tv`) e YouTube Music. `pm path com.spotify.tv.android` retornou `base.apk` e 3 splits (armeabi_v7a, pt, xhdpi), confirmando necessidade de tratar split APK.
- Depois do comando `adb shell monkey -p com.spotify.tv.android -c android.intent.category.LEANBACK_LAUNCHER 1`, o usuario confirmou `funcionou`, em resposta a pergunta se o Spotify abriu na tela. Registrar como **confirmacao visual relatada pelo proprietario**, nao recibo automatizado/video ou prova para todos os aplicativos. TCL-A3 = PASS parcial somente para Spotify; A1 inventario de pacotes real = PARCIAL, A2 icones = PENDENTE, A4 e A5 = PENDENTE. Nao remover companion nem atualizar OTA.
- Proximo passo: copiar somente APKs Spotify via `adb pull` para extrair e comparar nome e icone com recursos reais. Nao publicar APKs extraidos nem registrar IPs de rede privada em repositorio.
