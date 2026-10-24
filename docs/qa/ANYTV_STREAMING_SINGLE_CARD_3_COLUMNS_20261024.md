> **VALIDAÇÃO FINAL 24/10/2026 — UNIFICAÇÃO VISUAL STREAMING:** [CI DEV unsigned #40114130735](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40114130735) **COMPLETED/SUCCESS** no commit de código/testes `b4fecc63f744d66b8240d091db0a427e338e67ad`: 51 contratos de telas/catálogo, 2 testes de ausência de credenciais manuais, 11 contratos visuais aprovados; Gradle `assembleRelease` **BUILD SUCCESSFUL (4m39s)**, identificação `com.tclremote.livre.v2`, code54 e verificação de APK unsigned SUCCESS. Artefato de auditoria **`anytv-v2-2925-code54-unsigned-audit` #12567737040**. [Prova isolada #40114275545](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40114275545) SUCCESS. Comparação de hashes GitHub confirmou `TclStreamingScreens.kt`, testes e workflow **idênticos** entre HEAD DEV documentado e fonte compilada. **NÃO ASSINAR/INSTALAR este unsigned** sobre OTA publicada 2.9.25/code54. Nenhuma alteração de OTA, companion ou Worker; prova visual no aparelho físico ainda pendente; P0 7/11 (63%), gates F0–F8 NAO_HOMOLOGADO.

# AnyTV — padrão único dos cartões nas telas de streaming

Data: 24/10/2026. Referência aprovada: **primeira proposta visual de grade Ação**, alinhada à página principal do usuário. Solicitação explícita: **corrigir o código de todas as páginas; não gerar imagens**.

## Diagnóstico do código anterior

- O catálogo usava diversos desenhos incompatíveis: páginas de gênero podiam abrir modo lista (pôster 116×158 dp com sinopse ao lado), `Continuando` tinha só duas colunas, Favoritos/Assistidos usavam prateleiras com duas linhas por faixa, a busca geral apresentava duas colunas e as faixas `Em breve · Cinema`/internacionais tinham apenas imagem e título em 109×145 dp.
- Alguns pôsteres eram exibidos em contêiner fixo de 158 dp com `ContentScale.Crop`, produzindo o corte vertical denunciado no telefone.
- Cada superfície definia manualmente ordem e espaço de título, ano, nota, check, coração e canais, ocasionando desalinhamento e mudanças de posição.
- O armazenamento de favoritos e séries acompanhadas não persistia `rating` e `votes`, eliminando notas reais após reinício. Os snapshots salvos do TMDB também não retinham notas até esta revisão.

## Correção no código DEV

1. **`AnyTvPosterCard` é o componente visual único**: imagem com `Modifier.fillMaxWidth().aspectRatio(2f / 3f)` e `ContentScale.Fit`; sem recorte. Metadados em ordem fixa: **ano · nota / check / coração → título → chips F/S e provedores**. Não alterados `StreamingOfferIcons`, `OfficialTvIcon`, cores dos selos, catálogo de provedores nem os callbacks de salvar/assistir.
2. **Três títulos por linha** na grade de gênero e Cinema brasileiro, nas páginas dedicadas abertas por `Ver mais`, na biblioteca de Favoritos, Assistidos e na seção **Continuando** (novo nome para Em andamento), bem como nos resultados da busca geral e nas sugestões de títulos relacionados.
3. **Prateleiras horizontais** da página inicial, de streaming/provedores, `Em breve · Cinema` e `Cinema pelo mundo`: uma linha com **três cartões completos visíveis por largura**, mantendo rolagem horizontal, limite de 24 por lote e navegação por `Ver mais`.
4. Eliminado seletor inconsistente `Lista/Grade` nas páginas de gênero (não existe mais opção para reduzir a lista a uma coluna com pôster recortado). Mantida ordenação por popularidade, nota, data ou nome e paginação de **24 títulos**.
5. Busca exclusiva de **Continuando** com placeholder compacto `Buscar nas minhas séries`, botão de limpeza, ordenação estável, até 24 cartões por carga e botão `Mostrar mais 24`. A busca principal continua sobrepondo a capa em preto, com histórico e `Fechar` separado de `Limpar`, agora também com três cartões por linha.
6. Favoritos, `Continuando`, histórico de assistidos e snapshots `saved_title_` armazenam e recuperam notas/votos **reais quando disponibilizados pelo TMDB**; sem inventar avaliações ausentes. Dados legados continuam válidos.
7. **Banner cinematográfico/hero horizontal** permanece diferenciado de um pôster de catálogo. Seu recorte de `backdrop` é intencional e não muda a proporção das capas dos cartões.

## Validação exigida

- `scripts/tests/test_streaming_screens_contract.py`: asserts de 3 colunas, `ContentScale.Fit`, proporção 2:3, um componente de cartão, mesmo provedor e mesma ordem dos metadados, ausência de layout antigo e preservação dos gates de Cinema BR e busca.
- CI unsigned DEV: contratos de UI/links/segurança, preparação, Gradle `assembleRelease`, identidade de artefato. Execução [Actions #40114130735](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40114130735), **aguardando resultado de CI no momento da criação**.
- Validação física ainda necessária em Android: três pôsteres completos por linha em diferentes larguras, sem imagens cortadas, texto longo com reticências, presença de ano/nota quando houver, favoritos/check funcionais, mesma aparência dos pequenos ícones de plataforma, séries em andamento persistentes, filtros de Cinema e Brasil, rolagem dos trilhos e `Mostrar mais 24`.
- **Política de publicação:** última versão assinada e publicada continua 2.9.25/code54, confirmação release [#40106161667](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40106161667). Esta é uma **revisão posterior só na branch DEV/PR #3**, não há novo APK assinado nem OTA; não reinstalar artefato unsigned. Versão e code de futura atualização ainda precisam de promoção formal (necessariamente superior a 54).
- Não modificados certificado do celular, Worker, D1, OTA V2, OTA legado, código/artefato do companion TCL e identidade dos serviços de streaming.
- Progresso global formal P0 7/11=63%, gates físicos F0–F8 ainda `NAO_HOMOLOGADO`.

**GitHub-first:** ADAPT — padrão `Modifier.aspectRatio(2f / 3f)` com Compose aplicado a cartões e placeholder `ContentScale.Fit`; a amostra pública do DroidDeck usa a razão 2:3 em `app/src/main/java/com/droiddeck/launcher/ui/FrontEndArt.kt`. Reutilizado o componente AnyTV de ícones/canais em vez de criar imagens ou badges novos.
