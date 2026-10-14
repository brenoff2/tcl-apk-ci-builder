# Controle TCL Livre — requisitos vinculantes

Referências de aceitação: APK **1.7 Resgate fornecido pelo usuário** (base
funcional) e imagem **Controle remoto Android TV em português.png** (layout
aprovado). Nunca interpretar logotipos dessa imagem como lista fixa.

## Apps realmente instalados na TV — requisito obrigatório

1. Após a conexão remota, o celular usa o endereço da TV conectada para
   consultar automaticamente a lista de aplicativos **reais da televisão**
   com nomes e ícones oficiais, usando o complemento na própria TCL.
2. O protocolo padrão Android TV Remote v2 não lista os aplicativos da TV.
   O **Controle TCL TV 1.1** fornece `GET /apps` na LAN após vinculação
   inicial por código de oito dígitos exibido somente na própria TV.
3. Após vincular uma vez, reutilizar a configuração e consultar de novo a
   cada conexão; não pedir o código novamente se a credencial funciona.
4. Nunca substituir a lista real por Netflix, Prime, Max etc. presumidos ou
   por exemplos do layout. Mostrar os aplicativos retornados pela TV.
5. A seção **Aplicativos** tem **Ver todos**, que abre a lista real; nela
   selecionar favoritos e ordenar. Toque em favorito faz `POST /launch`
   da identificação retornada pela TV, não um link inventado.
6. Salvar favoritos e código por identidade do dispositivo conectado,
   migrando preferências antigas por IP, sem perder os anteriores após
   DHCP, queda, falta de complemento, erro HTTP ou ausência temporária de app.
7. Se faltar vínculo/serviço ou houver erro, mostrar instrução contextual e
   permitir repetir/alterar o código. Nunca apagar favoritos em falha.
8. Se o usuário escolher lista de favoritos vazia, respeitar a escolha.
   Antes de haver escolha salva, pode-se mostrar uma amostra **dos apps
   verdadeiros retornados pela TCL**, jamais atalhos arbitrários.
9. Exibir estado de conexão e não prometer inventário real em TV sem
   complemento habilitado em Acessibilidade.

## UI e controle preservados

- Reprodução, botões, teclado, D-pad, touchpad, voz, Cast, Bluetooth/fones
  e navegação continuam conectados a ações existentes.
- Seguir a imagem aprovada quanto às posições, barras, botões, cores e
  proporções, com rolagem segura em telas pequenas.
- Nunca publicar APK que alegue atualizar a 1.7 sem certificado compatível.
  O preview `com.tclremote.livre.preview` instala em paralelo e possui
  preferências independentes; a atualização real aguarda a chave original.
- Homologação separada: código, build, **imagem em emulador** e **TV real**.
  Build bem-sucedido não equivale a homologação funcional.

## Regressões proibidas

- Substituir `TclInstalledAppsBar` por `AppShortcutStripSquare`
  na tela aprovada.
- Iniciar com favoritos vazios e ocultar todos os aplicativos sem explicar
  como vincular; substituir inventário não disponível por serviços fictícios.
- Remover o componente da TV ou exigir que o usuário selecione os aplicativos
  instalados exclusivamente na tela da TV.
- Retirar funcionalidades para acomodar o novo desenho da tela.
