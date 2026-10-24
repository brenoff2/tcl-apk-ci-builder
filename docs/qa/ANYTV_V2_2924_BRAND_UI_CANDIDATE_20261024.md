# AnyTV V2 2.9.24/code53 — marca e correções reais no APK (candidata)

**Estado verificado em 24/10/2026:** código implementado na branch `feat/anytv-p0-device-proof-verifier-20261020`, commit funcional `f7b6268ffb80a00fb1ac87107a249ce601efa14d`; correção de splash `80d07c9d5c0b8c85275a11cd4223a6641211dd17`; testes ajustados em `07ce297efed4c17e97c879cc0404426ae4e4c6a7`. **CI Android SUCESSO** [#40092929283](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40092929283): 40 testes de interface/contratos, 2 de segurança, Gradle BUILD SUCCESSFUL (2m39s) e identidade unsigned V2/code53 validada. **NÃO ASSINADA, NÃO PUBLICADA OU INSTALADA.**

## Alterações implementadas no app

- Recursos gráficos reais em `assets/anytv_icon.png`, `assets/anytv_logo.png`, `assets/anytv_horizontal.png`, com gerador reprodutível `scripts/create_anytv_brand_2026.py` baseado na identidade vermelha/preta fornecida pelo usuário; `scripts/apply_anytv_brand.py` injeta launcher mipmaps, wordmark, splash, e uma paleta escura vermelha nas cores Material3. Não altera pacote/certificado/OTA.
- `overrides/AnyTvSplashScreen.kt`: tema preto/vermelho, logomarca, barra de progresso visual e mensagem de conexão, versão instalada dinâmica; sintaxe Compose compilada.
- `overrides/TclStreamingScreens.kt`: logotipo horizontal no catálogo; botão **Ver mais ›** (quando seção contém mais de seis títulos) expande a seção em grade de três colunas, botão **Voltar** restaura rolagem horizontal de duas linhas; ícones BR de plataformas reduzidos de 20dp para 14dp (menores que badge F/S 16dp), 6dp entre plataformas e título, mantém três linhas reservadas; botão **Lembrar** removido dos cartões.
- `overrides/PlaybackTransportBar.kt`: **Do início / Episódios / Fones** removidos da barra porque não funcionavam confiavelmente na TCL do usuário; play/pause/stop, avanços e retrocessos preservados. Nenhuma alegação de reconexão Bluetooth automática.
- `overrides/TclInstalledApps.kt`: cache persistente por TV/IP/nome dos aplicativos e seus ícones efetivamente informados pelo AnyTV Connect; leitura imediata após reconectar, nova consulta automática e uma repetição em caso de falha inicial; mantém a última lista real durante erro, sem inventar apps.
- `scripts/apply_anytv_phone_v2_identity.py`: nome visual aprovado **AnyTV**; identidade técnica preservada `com.tclremote.livre.v2` (mesmo signer V2).
- Contratos atualizados para exigir essas alterações.

## CI

- [#40092508177](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40092508177): falha por dois asserts antigos (reminder e cabeçalho).
- [#40092579839](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40092579839): testes PASS, Gradle falhou por sequência literal `\\n` no gradiente do splash.
- [#40092929283](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40092929283): **SUCCESS**, 40+2 testes, Gradle BUILD SUCCESSFUL, verificação unsigned identity code53.

## Publicação: porta não ultrapassada

- Último APK OTA **assinado** continua AnyTV V2 **2.9.23/code52**, certificado `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`, SHA256 APK `6f3274f488803b0d13659080f09c17cdeed5cca68fdcc6f3f4225814d0226094`, blob OTA `eb7c13f3295eefd01c7f3533af8b86962e948cad`.
- Workflow oficial `.github/workflows/release-anytv-phone-v2-new-package.yml` ainda está no code52; tentativa de preparar a atualização segura para code53 foi **bloqueada pelas configurações de segurança do conector**. Não contornar a restrição, não forçar certificado ou substituir APK manualmente.
- **Pendente**: liberar workflow assinado com versionCode53 e signer V2 original, executar, verificar SHA256/HTTPS byte-readback, publicar uma única OTA e homologar fisicamente no celular.
- Companion TCL TV: nenhum APK TV foi modificado nesta rodada; identificar versão/certificado efetivos antes de tentar atualização em `updates/tv/cert*`, não trocar signer nem afirmar instalação. Os problemas de ícones são tratados inicialmente no cliente do celular.
- Progresso canônico: **7/11 = 63%**, gates F0–F8 `NAO_HOMOLOGADO`. Build aprovado não comprova funcionamento físico da TCL nem a atualização do aparelho.

**Decisão GitHub-first:** ADAPT a interface e infraestrutura de build já existentes; não substituir projeto ou criar novos serviços. Nenhuma imagem conceitual substitui mudanças reais de código.
