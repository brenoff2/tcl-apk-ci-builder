# AnyTV V2 2.9.22 — homologação real no celular (PENDENTE)

**Canônico do ciclo:** `docs/roadmap/PROJECT_PROGRESS.json`.  
**Versão aprovada tecnicamente:** `2.9.22-AnyTV`, `versionCode 51`, `com.tclremote.livre.v2`.  
**Build e publicação:** [GitHub Actions 40045953034](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40045953034), `completed/success`, 32 etapas.  
**Testes TMDB de ordenação:** [40045771887](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/40045771887), `success`.  
**APK SHA256:** `44cab69ad0eb6c0110ff4180e6388f042e667d8b86dcd319e39d777973d85d94`.  
**Distribuição OTA:** `feat/tcl-v22-updates-back-bluetooth:updates/android/v2/current.apk`, blob Git `39084853e1956f4aaedb3d8443ade9304e497581`.  
**Cloudflare vigente (confirmado em produção):** Worker `ce51439e-ca19-40d3-85e8-019c7a3669a5`, deployment `3298f15b-68d1-455b-bbc1-624925d7ccaf`, 100% do tráfego, sem quotas internas mensais nem limitação interna por minuto; autenticação e cache preservados.

> **Estado atual: AINDA NÃO HOMOLOGADO EM APARELHO.** CI, testes de contrato, assinatura e readback são evidência de entrega técnica, não de interação física. Não declarar PASS para F0–F8 nem aumentar o progresso de 7/11 sem evidências de homologação.

## Critérios práticos de aceitação

Marcar cada item **PENDENTE**, **PASS** ou **FAIL**, anotando modelo/Android, hora, evidência do aparelho e, quando FAIL, reprodução. A ausência de serviço de streaming ou de trailer deve ser registrada como **N/A (dados não disponíveis)**, não como êxito inventado.

| ID | Ação no aparelho | Resultado esperado | Estado |
| --- | --- | --- | --- |
| D01 | Instalar a atualização a partir de AnyTV V2 2.9.21 | 2.9.22/code51 reconhecida; mantém pacote, favoritos e pareamento | PENDENTE |
| D02 | Abrir a busca, digitar um termo e abrir o teclado virtual | Placeholder, caracteres inseridos e cursor totalmente visíveis dentro do campo; texto não corta em cima/baixo | PENDENTE |
| D03 | Pesquisar um filme e uma série, limpar a busca e repetir | Resultados reais aparecem; `×` limpa sem fechar a interface; sem erro `monthly_quota_exhausted` | PENDENTE |
| D04 | Abrir Ação e alternar **Lista / Grade 3** | Lista mostra cartaz, título, ano, nota, canal confirmado e sinopse (até 160 caracteres/3 linhas); grade mostra 3 cartazes por linha em larguras iguais | PENDENTE |
| D05 | Na mesma página mudar **Mais populares, Melhor nota, Mais recentes, Título A–Z** | A ordem realmente muda na origem; não apenas entre os 20 itens atuais; configuração persiste após reabrir | PENDENTE |
| D06 | Carregar mais filmes em duas ordenações diferentes | `Mostrar mais 20` adiciona até 20 ao fim sem duplicar o primeiro lote nem perder a ordenação; não aparece na home | PENDENTE |
| D07 | Favoritar e marcar assistido em modo lista e modo grade | Estado é coerente com a ficha do título e persiste na lista/assistidos; série alterna acompanhamento | PENDENTE |
| D08 | Tocar **Trailer** em um título com vídeo confirmado | Abre o trailer real no YouTube/navegador; se indisponível, informa sem abrir URL fictícia | PENDENTE |
| D09 | Deslizar 10 destaques na página de gênero | Até 10 slides; dots sobre a imagem, legíveis; título e canais permanecem alinhados | PENDENTE |
| D10 | Conferir o banner compacto da página inicial com título de 1 e 2 linhas | Título centralizado na área reservada; `Ver ›` fixo embaixo e cinza-claro; logo do canal aparece quando a disponibilidade BR for confirmada, sem rótulo inventado | PENDENTE |
| D11 | Rolar lateralmente seções de duas fileiras da home | Colunas com 116 dp e intervalo de 9 dp; títulos ocupam espaço fixo de três linhas, sem deslocar o cartaz inferior | PENDENTE |
| D12 | Usar D-pad, volume, Bluetooth, teclado/IME, lançar app na TV e reconectar | Funções anteriores não apresentam regressão; registrar observações no aparelho TCL | PENDENTE |

## Coleta mínima de evidência

Para falhas visuais, guardar **um print do estado afetado**, informando versão, modelo e qual ID falhou. Para falhas de comportamento, descrever os toques e o resultado observado. Não exigir vídeo, logcat ou acesso remoto se o print e a reprodução bastarem.

**Regra de fechamento:** homologar somente os itens comprovados; no ciclo seguinte, corrigir apenas falhas reproduzidas e então gerar **uma** nova versão V2 validada. Sem novas versões de APK apenas por documentação.

## Estado técnico confirmado

- `40045953034`: build/IME/busca real/assinatura/publicação/readback **PASS**, mesmo signer V2.
- `40045771887`: consulta TMDB filme/série e ordenação/paginação **PASS**.
- OTA legado `updates/android/cert2026/current.apk` preservado (Git blob `44c766b433ddb370f4589b87843f1edfd9cd7b7a`).
- **Atualização posterior à publicação do APK:** removido também o antigo limite de 200/mês da API de links Movie of the Night e o rate-limit Cloudflare por IP/ativação; removidos os bindings `MAX_UPSTREAM_CALLS_MONTH` e `RATE_LIMIT` em produção. Nenhum limite próprio do AnyTV permanece. Autenticação, cache, validação de links e permissões foram preservados. Limites contratuais dos fornecedores externos independem dessa mudança. Worker atual `ce51439e-ca19-40d3-85e8-019c7a3669a5`; offline CI `40052421285` e smoke real `40052526994`: SUCCESS.
