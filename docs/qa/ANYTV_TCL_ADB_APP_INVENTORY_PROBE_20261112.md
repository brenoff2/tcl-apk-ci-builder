# TCL Android TV — prova ADB isolada de inventário, ícones e abertura (12/11/2026)

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

