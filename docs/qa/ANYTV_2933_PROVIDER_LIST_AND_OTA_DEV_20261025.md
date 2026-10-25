# AnyTV — modo lista por provedor e release code62 (25/10/2026)

## Pedido implementado
- Cada página individual de streaming conserva a visualização original por seções/faixas como padrão.
- Na própria linha do título do streaming, alternador `Ver em lista` / `Ver em seções`.
- Modo lista mantém o carousel de destaque já existente; lista filmes e séries intercalados (conforme seleção), Grade e Detalhado reutilizam os cards e as ações de detalhes.
- `Mostrar mais` solicita as páginas seguintes **independentemente para filme e TV**, usando `StreamingCatalog.discover(token, app.name, kind, next, id, "")` e o ID de gênero correspondente.
- `StreamingCatalog.discover` usa `watch_region=BR`, `with_watch_providers` e `with_watch_monetization_types=flatrate`; nenhum serviço externo entra por engano no modo lista.
- Estado da listagem indexado por provedor/mídia/gênero, não mistura resultados de seleções diferentes.
- Modo antigo, favorites, watched, OTA legado, Worker/D1/companion e certificado preservados.

## GitHub / QA
- Código `e2abbeac417563ba20ccb27315574a1bf8b45d9b`.
- Testes `83a17c3cb9343a57ceae3549e52a897983f1003d` e correção de posicionamento dentro da classe de teste `b9a2b51d26d34ebc5427d5a7c9496b1b29477e39`.
- Workflow de release `2.9.33-AnyTV/code62` já preparado no canônico; pedido de release autenticado no commit `fc4f13faf6fcfe43fbdfc13fc476b1ce18d73a85`.
- Run oficial `#40224147653` iniciado e **IN_PROGRESS** na primeira consulta; no checkpoint não atribuir SUCCESS, publicação OTA ou readback.
- Run unsigned `#40224135353` IN_PROGRESS na primeira consulta.
- Release anterior `#40223579006` falhou no contrato obsoleto de 'Em breve'; contrato corrigido antes da retomada.
- Versão OTA comprovada antes deste run: `2.9.32/code61`, commit `e7cd7d1`. Sem evidência de code62 até SUCCESS e readback.
- QA física Android/TCL ainda pendente; PR #3 DRAFT, sem merge.

## Fechamento verificado da publicação
- Run oficial **#40224219849 SUCCESS**, SHA-fonte `7510fa10019ea859c5a21c90edd1a29e07693218`.
- Run unsigned **#40224211903 SUCCESS**, após atualização dos testes de cabeçalho e navegação.
- OTA commit **`b772861ef5bcfbcb3597b9f563b60a9a32a28245`**, substituindo somente `updates/android/v2/current.apk` no canal independente.
- Assinatura V2 original validada: `ANYTV_V2_SIGNER_VALIDATED_f182088c93fa4ea6b3b16a4e0b6020a8d793d0e6f0ba664fca83e907972847c7`.
- SHA256 do APK assinado: `f182088c93fa4ea6b3b16a4e0b6020a8d793d0e6f0ba664fca83e907972847c7`.
- **`ANYTV_V2_GITHUB_OTA_BYTE_READBACK_SUCCESS`**, confirmação do arquivo publicado pelo próprio workflow.
- Artefato assinado: `anytv-v2-2933-signed-update`, ID **12610915808**, 23.467.193 bytes (arquivo ZIP do GitHub Actions).
- Testes de busca TMDB ao vivo para filme e série: SUCCESS (Titanic 20, Breaking Bad 5).
- **Publicação aprovada em CI não equivale a instalação/homologação física do usuário.** PR #3 mantém Draft sem merge. Worker, D1, companion e OTA legado inalterados.
