# AnyTV — identidade visual aprovada em 28/10/2026 (implementacao pendente)

## Decisao expressa do usuario
- As imagens aprovadas no chat sao a referencia visual da marca AnyTV.
- Aplicar somente onde a marca ja aparece: icones do launcher, splash, logomarca existente e identificacao existente do app na TV.
- Nao inserir logotipo em telas internas, filmes, series, controle remoto, cabecalhos ou outros locais novos.
- Manter Android package, certificado, dados locais e mecanismo OTA; nao prometer upgrade por cima sem verificar signer.

## Arquivos aprovados preparados (ainda NAO enviados ao repo)
| destino | pixels | SHA-256 do PNG preparado |
|---|---:|---|
| assets/anytv_icon.png | 512x512 | 990db3c0d4a4fe07e3765851dfb3e5455c849a4f852b8537caa881574495dcf6 |
| assets/anytv_logo.png | 1080x360 | a8747c3c456eb502c7b93a3fcca803b23631e6f2054edfef05715e5d39401790 |
| assets/anytv_horizontal.png | 1080x360 | a8747c3c456eb502c7b93a3fcca803b23631e6f2054edfef05715e5d39401790 |
| assets/anytv_splash.png | 720x1280 | 779e310821528d2cf79cf9f115f38ea5a1a2a178b4ca4a2fadba1728568aab41 |
| assets/anytv_tv_banner.png | 1280x720 | 564802f2b7848fb079fba299d6e79cadf613635ec1edf0658f799ec3f37e49bf |

Arquivos ainda mantidos em artefato ZIP na conversa. Nao assumir que GitHub tem esses bytes. O conector GitHub atual suporta texto e create_blob(base64), mas nao recebe diretamente caminho local/arquivo de conversa; exigir upload real + readback SHA antes de compilar.

## Codigo auditado
- `scripts/apply_anytv_brand.py`: converte assets/anytv_icon.png para mipmaps e copia anytv_logo.png para drawable/anytv_wordmark.png, e anytv_horizontal.png; chama splash override.
- `overrides/AnyTvSplashScreen.kt`: atualmente desenha wordmark, barra de progresso e subtitulo; verificar fidelidade ao design aprovado antes de release, sem duplicar splash ou textos.
- Para a TV, `.github/workflows/anytv-tv25-compile.yml` hoje gera APK **unsigned** 2.5/code16, `com.tclremote.tvcompanion` e usa cert2026 em atualizacao. NAO publicar unsigned; investigar as rotas de assinatura da TV ja existentes e dois certificados historicos antes de qualquer release.

## Gate para a proxima publicacao
1. Fazer upload dos PNGs exatos acima na branch DEV e verificar SHA-256 byte a byte.
2. Conectar assets nos pontos de marca ja existentes, aplicar testes visuais/recursos, confirmar ausencia de marca nova em telas internas.
3. Atualizar `release/anytv-v2.json` a uma versao de codigo maior que a OTA 2.9.55/code83 **somente quando assets e codigo estiverem prontos**. Pipeline assinado e byte-readback ja existe.
4. TV: validar atual identity/package/signer/versionCode nas APKs OTA por variante; implementar banner/icone somente no app existente, compilar, assinar nos certificados legitimos, validar cada canal e readback; manter TV antiga se nao for seguro.
5. Registrar commit GitHub, CI, hashes, assinaturas, OTA e QA TCL antes de declarar publicado.

**Status deste documento:** especificacao e checksums persistidos; APKs com nova marca NAO publicadas.
