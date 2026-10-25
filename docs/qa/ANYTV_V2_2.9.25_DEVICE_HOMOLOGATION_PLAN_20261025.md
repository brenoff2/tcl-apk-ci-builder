# AnyTV V2 2.9.25/code54 — plano de homologação em aparelho real

**Data do plano:** 25/10/2026. **Estado:** PLANO PREPARADO, TODOS OS TESTES FÍSICOS PENDENTES. Nenhum aparelho foi acessado ou homologado por esta documentação.

## Fontes e identidade que não devem ser misturadas

**OTA oficial assinada:** `2.9.25-AnyTV`, `versionCode=54`, pacote `com.tclremote.livre.v2`, branch `feat/tcl-v22-updates-back-bluetooth`, commit `dcc51bad5561e66462ab6e1eb2f1d3fd24ee5112`, SHA256 do APK `c53596d8f8210bf0a12769481796481a851d00da4c52f544120b85367dd17fe0`, certificado V2 SHA256 `fe8c59e4f656af52f89e4430f7ba552aded37beb808a7e9953f534c5ebfe56d9`. Release assinada e OTA validadas via [CI #40106161667](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40106161667). [Evidência oficial](ANYTV_2925_CODE54_SIGNED_OTA_RELEASE_20261024.md).

**Worker publicado:** versão `58fe4b33-6ee2-4841-8587-c28acaaad435`, deploy `25de8328-10ab-471a-8e94-fb4cdbce13d9`, 100% do tráfego, D1/secrets/bindings originais. Este Worker **não** tem a rota `/v1/catalog/movie/{id}` acrescentada na fonte DEV. Não inferir que essa restauração de Assistidos funciona na versão pública.

**PR #3 DEV:** `feat/anytv-p0-device-proof-verifier-20261020`. Fonte de alterações posteriores à versão assinada (pôsteres completos 2:3 em 3 colunas, recuperação por ID, botão de lote de 12, gênero com retry) e candidatos **unsigned/code54**. **Não instalar unsigned sobre APK assinado** nem considerá-los publicados. Para futura instalação de homologação é preciso release autorizado com `versionCode>54`, mesmo certificado V2 e evidências de integração.

O roteiro anterior para 2.9.22/code51, [ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md](ANYTV_V2_2.9.22_DEVICE_HOMOLOGATION.md), permanece **histórico**, não é a autoridade para a OTA atual.

## Coleta mínima antes do teste

Registrar modelo do celular, versão do Android, modelo/versão do software da TV TCL, versão exibida do AnyTV, data/hora local, modo de instalação, identificação do AnyTV Connect TV se presente, conexão de rede e prints apenas quando houver falha/validação visual. Não coletar segredo, código de ativação, token, cookie ou conteúdo de conta de streaming. Todos os resultados começam como **PENDENTE**. Quando um serviço não estiver instalado ou não tiver filme no Brasil, registrar **N/A fundamentado**, nunca PASS automático.

## Matriz A — OTA assinada 2.9.25/code54 em produção

| ID | Ação no dispositivo físico | Resultado esperado | Estado |
| --- | --- | --- | --- |
| A01 | Atualizar uma instalação V2 legítima assinada anterior pelo canal OTA V2 oficial | Mesmo pacote/certificado, versão 2.9.25/code54, favoritos e pareamento mantidos | PENDENTE |
| A02 | Abrir a tela inicial e percorrer ícones reais dos apps instalados | Marca AnyTV correta, atalhos reais e cabeçalho sem logo duplicado | PENDENTE |
| A03 | Pesquisar filme e série reais, limpar, refazer e examinar ficha | Dados de fonte TMDB, teclado legível, texto não cortado, sem título inventado | PENDENTE |
| A04 | Abrir destaque e conferir nome, ano, Cinema BR se comprovado e arte | Nome sobre imagem com shadow/blur legível, selo azul apenas com comprovação Brasil, sem sobreposição anormal | PENDENTE |
| A05 | Selecionar serviço de streaming e conferir assinatura BR | Canais originais/ícones corretos; sem alegar oferta ou reprodução não confirmada | PENDENTE |
| A06 | Favoritar, marcar assistido, reabrir, reiniciar app e voltar | Persistência coerente de dados disponibilizados pela versão assinada, sem perda involuntária | PENDENTE |
| A07 | Emparelhar TCL, executar D-pad, Back, Home, volume e reconectar | Comandos anteriores continuam operacionais sem alteração de identidade TLS | PENDENTE |
| A08 | Abrir app/solicitar ficha de streaming na TCL, quando instalado | UI informa o comando realmente solicitado; não confunde busca com reprodução confirmada | PENDENTE |
| A09 | Verificar estado de autorização do dispositivo e acesso aos links | Sem bypass de ativação; erros exibidos com honestidade; não logar códigos nem credenciais | PENDENTE |
| A10 | Sair e reabrir AnyTV, testar alternância Wi-Fi/rede e reconexão | Aplicativo mantém estabilidade, dados persistentes e não perde pareamento por regressão | PENDENTE |
| A11 | Verificar canal de atualização V2 sem instalar outro APK | Confirma canal e pacote V2, sem tocar em OTA legado/cert2026 | PENDENTE |

## Matriz B — recursos DEV bloqueados para teste publicado

| ID | Verificação futura após releases aprovados | Estado |
| --- | --- | --- |
| B01 | Pôster original 2:3 sem corte em três colunas em home, gêneros, filtros e Assistidos | BLOQUEADO — código somente DEV |
| B02 | Assistidos com apenas IDs legados não deve declarar lista vazia; IDs persistem | BLOQUEADO — código somente DEV |
| B03 | Recuperação movie/{id}, até 12 consultas por lote; HTTP 400 antigo interrompe sem apagar IDs | BLOQUEADO — endpoint Worker não publicado |
| B04 | HTTP 404 de filme inexistente não impede recuperação de filme válido seguinte | BLOQUEADO — endpoint Worker não publicado |
| B05 | Botão Recuperar mais 12 e persistência de resultados após cancelamento/saída | BLOQUEADO — código somente DEV |
| B06 | Falha inicial de gênero/Cinema termina spinner, mostra erro e Tentar novamente | BLOQUEADO — código somente DEV |

## Evidência e gate de aceite

Para cada teste executado registrar **PASS**, **FAIL** ou **N/A**, com dispositivo, versão, horário e reprodução de erro quando houver. Se FAIL, documentar passos e evidência, sem editar o histórico oficial do arquivo assinado. Um print de UI é suficiente quando demonstra o erro; não exigir vídeo, acesso remoto, logcat ou dados sensíveis sem necessidade.

**Não declarar homologação física sem registros reais**. A CI de contratos, o build unsigned, a assinatura OTA e o readback HTTPS não comprovam uso de celular/TCL. Não mover M08/M09 de IN_QA nem M10/M11 de BACKLOG sem aprovação documentada, e manter F0–F8 `NAO_HOMOLOGADO`. **Progresso canônico: 7/11 = 63%.** Não acionar merge, deploy Worker, assinatura, publicação ou mudança de D1 a partir deste plano.
