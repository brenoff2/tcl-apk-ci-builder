# Controle TCL v2.0 / Complemento TV 1.3 — correção integrada

**Situação:** código alterado em ramo isolado; instalações e funcionamento
físico dependem de homologação na TCL e no celular do usuário.

## Critérios de aceitação

1. **Barra:** eliminar a navegação Remote/Cast fixada no rodapé da tela do
   controle. Cast continua disponível no botão discreto do cabeçalho.
   Conteúdo rola, respeita insets e usa tamanhos adaptados à altura/largura.
2. **Teclado:** o diálogo escreve/cola texto localmente e o botão
   Enviar envia **texto completo pelo IME do Android TV Remote v2**.
   Não emitir `sendImeEnter()` nem `KEYCODE_DPAD_CENTER` em Enviar.
   É obrigatório estar com um campo de texto selecionado na TV.
   Verificar comportamento físico da TCL, pois campoCounter/IME depende
   do firmware e do teclado ativo.
3. **Tema:** usar `MaterialTheme.colorScheme`, que já acompanha as
   preferências de tema claro/escuro, paleta e OLED do projeto original.
   Não manter `darkColorScheme()` fixado na tela aprovada.
4. **Aplicativo único:** o pacote de destino é `com.tclremote.livre`
   (`versionCode=20`), nunca `com.tclremote.livre.preview`.
   **Isso não resolve assinatura da 1.7.** O APK 1.7 realmente instalado
   foi assinado pelo certificado
   `49de7c9ad97bf4cfb2409f5e6480ab21bff816f093f8e7df66a33cce61077a04`.
   A chave privada correspondente não foi recuperada.
5. **Vinculação persistente:** salvar código e favoritos por IP, ID e nome
   da TV. Usar `commit()` na confirmação e recuperar chaves antigas
   `pair:<id_anterior>` do mesmo pacote, somente após validação LAN.
   Nunca descartar configurações por timeout da TV, erro HTTP ou mudança
   de IP/identificador.
6. **Fones Bluetooth:** não transmitir `KEYCODE_PROG_RED`, pois o
   complemento 1.2 removeu o AccessibilityService que o interceptava.
   Usar `POST /headphones` com o código de vinculação, exigindo
   seleção explícita de um fone **já pareado** na TCL. O complemento 1.3
   solicita BLUETOOTH_CONNECT somente nessa ação, mantém leitura real de
   apps e não declara Acessibilidade. Se Android bloquear o A2DP oculto,
   a UI precisa informar a necessidade de configuração manual.

## O que permanece pendente da homologação física

- Instalar o complemento 1.3 na TCL com Play Protect **ativo**.
- Verificar entrada de texto em diferentes telas da TCL.
- Testar conexões e reconexões reais de fones, inclusive fallback para
  Configurações Bluetooth quando o Android negar API oculta.
- Testar rolagem, proporções e tema em tamanhos diferentes de celular.
- Testar fechar app, forçar parada e abrir novamente com TV conectada.

## Assinatura e migração — BLOQUEADA até backup e consentimento

Os builds antigos do GitHub Actions foram assinados com certificados
debug efêmeros, incompatíveis com APKs instalados. O preview 1.9 e a
versão 1.7 original possuem **IDs/certificados distintos**. Por isso,
uma atualização normal de 1.7 para 2.0 usando nova chave é
criptograficamente impossível.

Foi gerada uma nova chave **permanente** protegida por senha para o
telefone e outra para o complemento TV (assinaturas distintas). Os
arquivos de chave NÃO devem ser enviados ao GitHub público, nem incluídos
em APKs, logs ou versões distribuídas. É obrigatório fazer backup seguro
deles antes da migração. Futuras atualizações deverão ser assinadas
com a mesma chave.

**Não desinstalar automaticamente nada e não recomendar exclusão
sem backup e consentimento informado.** Uma instalação consolidada
v2.0 exigirá migração ÚNICA, removendo pacotes antigos manualmente,
se a chave original não aparecer. Isso APAGARÁ dados internos de
1.7 e 1.9 preview; o código de TV e favoritos deverão ser anotados
e, quando não exportáveis, restaurados manualmente. A reinstalação do
complemento TV 1.3 também apagará o vínculo antigo da TV 1.2.

Depois de uma migração completa e testes, serão mantidos apenas:
`com.tclremote.livre` no celular e `com.tclremote.tvcompanion` na TV.

## Segurança

- LAN `/apps`, `/launch`, `/headphones` protegidos pelo código local;
  nenhuma API aberta à internet pública.
- Sem Acessibilidade/QUERY_ALL_PACKAGES/BLUETOOTH_SCAN no complemento 1.3.
- Bluetooth usa apenas dispositivos já pareados e explicitamente escolhidos.
- Reconexão A2DP é tentativa; não afirmar que a TV confirmou a conexão.
- Nenhuma alteração em Oracle/CLFCJ/servidores de produção.
