> **READBACK FINAL — VISUAL ANYTV 2.9.25/code54, 24/10/2026:** [GitHub Actions #40104547392](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40104547392) **SUCCESS** no commit de código/testes `fdced7c359072f7d8fc23cb40479f8160c3b63f7`: preparo do upstream+sticky transform SUCCESS, contratos UI/links/segurança SUCCESS, Gradle `assembleRelease` SUCCESS, `aapt` package `com.tclremote.livre.v2` / `versionCode=54` / `versionName=2.9.25-AnyTV` SUCCESS e upload de artefato unsigned `anytv-v2-2925-code54-unsigned-audit` ID `12564042182` SUCCESS. ZIP baixado e inspecionado; SHA-256 APK **`617e6e25cb6c768254e01beb7a9d4ac7758efca870826e4443aee72026de46b7`** foi recalculado diretamente dos bytes do ZIP e é idêntico a `candidate-sha256.txt`. **Somente auditoria, NÃO instalar** sobre APK assinado. Fonte física TCL/celular não homologada; última OTA assinada segue **2.9.24/code53**; release workflow oficial, signer, Worker, D1, companion e OTA mantidos. PR #3 draft, gates F0–F8 NAO_HOMOLOGADO e P0 7/11=63%.

# AnyTV 2.9.25/code54 — nova proposta visual do controle (24/10/2026)

**Referência de design:** captura enviada pelo usuário no projeto ANYTV, 709×1536, tela inicial de controle remoto em grafite/preto, vermelho para ações e direcional grande. **Ação autorizada:** atualizar o código de DEV da tela principal, não a tela de streaming interna. **Release:** nenhuma assinatura, OTA, merge ou atualização do companion TCL.

## Adaptação de design → código

- Fundo escuro grafite `#08090D`, gradiente vertical quase imperceptível, textos branco/cinza e acentos vermelhos `#E9003B`; paleta limitada à tela do controle, sem alterar temas das configurações, telas de descoberta ou telas de streaming.
- Cabeçalho: ponto verde ligado/âmbar desligado, nome da TV em destaque, marca Android TV ou fabricante abaixo, três atalhos circulares para energia, transmissão e configurações. Nome pode ser tocado para trocar a TV.
- Apps: até seis ícones **reais da televisão** por linha padrão, 52 dp, cantos suaves de 15 dp, legendas; favorito escolhido pelo usuário preservado, mais ícones e gerenciador acessíveis ao rolar horizontalmente; não inventar YouTube/Netflix/Disney+ se não constarem nos apps realmente instalados.
- Banner: arte de título real via TMDB, 92 dp de altura, imagem full-bleed, fade horizontal grafite com título legível, etiqueta `EM DESTAQUE` ou `F · CINEMA`, CTA `Ver ›` que abre o catálogo. Mantidas as prévias reais, IDs e periodicidade existente.
- Reprodução: duas faixas de ações existentes com fundo grafite `#181A20` e cantos 17 dp, sem caixa externa. Mantidos voltar/avançar, play, pause, stop e aceleração por comandos reais; sem introduzir skip-previous/skip-next falsamente suportados apenas porque ilustrados na referência.
- Direcional: **não reduzido**. Limite máximo 304 dp (antes 280) condicionado ao espaço físico disponível; fundo radial grafite, sem círculo/borda externa vermelha, setas claras e OK 28% do diâmetro com anel vermelho. Preservados toque curto e início/fim de pressões longas, inclusive cancelamento seguro.
- Laterais: controles de volume/canal altos e arredondados, ícones menores com botões +/− tocáveis. Barra central **meramente decorativa, cinza e sem nível**; o protocolo não informa volume/canal numéricos confiáveis. Atalhos TV/mute preservados.
- Rodapé: cinco ações funcionais, círculos grafite com legendas, dimensões até 56–64 dp, sem borda pronunciada: Teclado, Início, Ponteiro/Números/Direções, Microfone, Voltar. 
- `scripts/apply_tcl_sticky_controls.py`: transformador oficial de build atualizado para reconhecer o novo fundo, aplicar limite 304 dp condicionado ao `stageHeight` e manter controle centralizado e rodapé fixo. Nenhum bypass de build ou liberação.
- CI `.github/workflows/anytv-v2-candidate-compile.yml`: monitora também os arquivos do novo layout e o transformador. Testes atualizados `test_approved_visual_contract.py` e `test_streaming_screens_contract.py`.

## Critérios de aceite

1. Conferir captura real no telefone (temas claro/escuro), barra de status preta, TV com ícone de estado, seis apps quando disponíveis, banner com imagem real/capa e gradiente, área de mídia sem contornos, D-pad grande e rodapé acessível.
2. Conferir tela compacta e rolagem: áreas de toque não se sobrepõem, direcional e rockers não são cortados, conteúdo não desaparece com teclado ou mudanças de rotação.
3. Testar comandos reais de power, cast, settings, launch app, playback, pressões longas, mute/volume/canal, teclado, microfone e voltar.
4. Confirmar que a barra do rocker **não sugere percentual**; não deve receber fill vermelho vinculado a estado fictício.
5. Validar no CI preparação upstream, contratos, Gradle `assembleRelease`, identidade code54/2.9.25 e artefato unsigned. **CI em progresso no momento inicial deste relatório** — documentar readback final separadamente.

## Limites de escopo

- Fonte de imagem é o TMDB do próprio aplicativo; a imagem do exemplo não é embutida no APK como anúncio fictício.
- Tela de streaming/catalogação interna e companion TV Connect 2.5/code16 não são modificados por este redesign (exceto o componente de banner compartilhado com a capa, que é carregado na página principal).
- Última OTA assinada permanece **AnyTV V2 2.9.24/code53**. PR #3 continua draft. Gate de dispositivo real e produção não homologado. Progresso formal 7/11=63%.
