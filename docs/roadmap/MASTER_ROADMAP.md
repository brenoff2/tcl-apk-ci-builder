# Roadmap executivo AnyTV

Marcos e estados estão em `PROJECT_PROGRESS.json`. As fases F0–F8 **não substituem versões históricas**, apenas organizam o ciclo de entrega.

| Fase | Entrega / dependência | Aceite e gate |
|---|---|---|
| F0 Diagnóstico | Inventário e reconstrução (M01) | repo/branch/readback; gate humano não aferido |
| F1 Contratos | Preservar contratos TV, OTA e streaming | requisitos vinculados a observações de uso |
| F2 Arquitetura | celular ↔ TV LAN, celular ↔ Worker/D1/provedores | limites de confiança e ADR |
| F3 Núcleo | Android/TV compilados (M02–M03) | build e identidade do pacote, sem homologação |
| F4 Integrações | D1, Worker e ativação real (M06–M08) | 201, chave gravada e prova autenticada |
| F5 QA integrada | abrir título real (M09) | link permitido, app instalado e confirmação visual |
| F6 Segurança | regressão/rollback/aceite (M10) | testes reais e decisão humana |
| F7 Distribuição | OTA celular/TV (M04–M05) | certificado, code, SHA e leitura pública; instalação física separada |
| F8 Operação | monitoramento e aceite (M11) | estabilidade e continuidade comprovadas |

Cada marco exige evidência, pré-requisitos, riscos e reversão. Fases/gates sem aceite explícito seguem `NAO_HOMOLOGADO`. Progresso geral: `floor(100 * marcos comprovados / marcos totais)`. Revisão de escopo exige novo baseline justificado.
