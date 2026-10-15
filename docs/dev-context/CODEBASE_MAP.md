# AnyTV — Mapa de codigo (revisao manual; Cartographer nao executado)

source_repo=brenoff2/tcl-apk-ci-builder
source_branch=feat/tcl-v22-updates-back-bluetooth
source_commit=0884ead0e62ed4a1ee773ce4769f579ba33f7a15
map_source=MANUAL_GITHUB_REVIEW
cartographer_status=CARTOGRAPHER_BLOCKED
observed_at_utc=2026-10-15T23:38:04Z

> Este mapa se aplica SOMENTE ao commit acima. Se o codigo da branch evoluir, rodar `validate.py --strict-map --current-sha SHA_ATUAL` e atualizar os caminhos antes de programar. Nao confundir a branch `docs/anytv-chatgpt-context-pilot` com o codigo-fonte.

## Modulos verificados na fonte

| Pasta/arquivo | Responsabilidade e limite conhecido |
| --- | --- |
| `overrides/` | Codigo Kotlin/Java e recursos substitutos do controle Android; contem `TclResponsivePad.kt`, `TclStreamingScreens.kt`, `TclInstalledApps.kt`, `AppShortcutTiles.kt`, `PlaybackTransportBar.kt` e layout aprovado `TclApprovedHomeScreen.kt`. |
| `overrides/common/TclUpdateManager.java` | Atualizador que procura GitHub Releases; valida SHA-256, package, versionCode e certificado antes do PackageInstaller. NAO alterar sem revisar canônicos de assinatura. |
| `overrides/tv_safe/TclCatalogService.java` e `overrides/tv_safe/TvAppBridge.java` | Servico LAN e ponte do complemento na TV; consultar implementacao real antes de mudar API e pareamento. |
| `overrides/tv/` | Implementacoes alternativas do lado da TV (`TvAppBridge.java`, `RemoteButtonService.java`, `IptvAppsActivity.java`), nao assumir equivalencia com `tv_safe`. |
| `scripts/prepare_tcl.py` e `scripts/apply_tcl_*.py` | Preparo/customizacao do upstream Android; testes de `scripts/tests` dependem de checkout preparado `source/`, nao apenas dos templates de overrides. |
| `scripts/tests/` | Contratos de UI, streaming, complementos e integracao; alguns exigem `source/` preparado e testes em TCL real continuam separados. |
| `anytv-resolver/src/worker.mjs` | Worker de resolucao de links de streaming com provedores permitidos e limites de seguranca; nao e o controle Android. |
| `.github/workflows/` | Builds, testes, assinatura e diversos fluxos de publicacao OTA. Workflow verde pode representar staging em espera. |
| `updates/android/current.json` e `updates/tv/current.json` | Manifestos de distribuicao vigentes **observados** no GitHub (Android 2.9.4 e TV 2.1 no snapshot). **Nao editar** no piloto. |
| `release_signing/` e `signing-recovery/` | Materiais e fluxos relacionados a certificacao/recuperacao; nao copiar chaves para indices nem documentos. |
| `docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md` | Fonte de autoridade sobre status de publicacao, hash, certificados e pendencias. |
| `docs/CONTRATO_FUNCIONAL_TCL.md` | Requisitos vinculantes e proibicoes de regressao. |

## Rotas que nao devem ser confundidas

- Codigo do controle Android: `overrides/` + scripts de preparo + build no GitHub Actions.
- Complemento instalado na TV: `overrides/tv_safe/` e variantes `overrides/tv/`; o aplicativo Android TV Remote v2 sozinho nao fornece lista real de apps.
- Streaming: UI do app em `TclStreamingScreens.kt`, possivel Worker em `anytv-resolver`; exigem verificacao de integracao antes de mudar links.
- Atualizacao: `TclUpdateManager.java`, `.github/workflows/*publish*` e `updates/**/current*`; distinguir compilacao, assinatura, publicacao e homologacao.

## Limites

Este documento nao prova que cada importacao, dependencia, endpoint ou build esta funcional. Nao e inventario exaustivo. O Cartographer deve futuramente complementar este mapa, e nunca dispensar a leitura do codigo real de cada rota em alteracao.
