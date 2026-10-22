# PROJETO ANYTV — Visão e definição de pronto

Construir e manter o **AnyTV no celular Android**, o **AnyTV Connect na TCL/Android TV** e o **resolvedor privado Cloudflare Worker/D1**, para descobrir apps realmente instalados, controlá-los remotamente, manter recursos Bluetooth e navegar/abrir títulos por links oficiais quando a plataforma permitir.

Usuário: pessoa usando celular para controlar TV e plataformas já instaladas. Problema: controle fragmentado, links inventados, aplicativos presumidos, instalações repetidas e falta de experiência integrada.

**Essenciais:** controle remoto e vínculo LAN; inventário verdadeiro, favoritos e lançamento de apps; interface aprovada; catálogo com proveniência; autenticação por dispositivo; links seguros sem contornar DRM; OTA assinada e compatível; tratamento de erros e recuperação.

**Complementares:** melhorias visuais, conveniência e automações, somente se não atrasarem os essenciais.

**Restrições:** sem prometer acesso gratuito a streaming pago; sem chave de API no APK; sem troca de certificado e sem instalação silenciosa.

**Pronto** = demonstração física de ativação, controle, aplicativo real, abertura de título elegível, atualização Android/TV, tratamento de falhas, rollback seguro e aceite. Publicar APK ou Worker sozinho não é homologar.

Preservar `docs/CONTRATO_FUNCIONAL_TCL.md`, `docs/ANYTV_STREAMING_MATRIX.md`, `docs/ANYTV_STREAMING_SCREENS.md` e `docs/ANYTV_RELEASE_SIGNING_AND_UPDATES.md`.

## Evolução do produto — Discovery Home V3
O usuário determinou que a seção de Streaming evolua para um **centro de descoberta audiovisual**, com slides de destaques, carrosséis, tendências, lançamentos, títulos populares e bem avaliados, recomendações, trailers e relacionados. **Streaming deixa de ser entrada obrigatória e passa a ser filtro opcional; cada serviço também conserva sua página.** Uma única atualização Android estável é preferível a ciclos fragmentados de versões. A abertura de títulos já funcionava antes dessa frente: preservar. Ver `docs/decisions/ADR-0002-DISCOVERY-V3.md`, `docs/ANYTV_DISCOVERY_HOME_V3.md` e `docs/ideas/DISCOVERY_PRODUCT_BACKLOG.md`.
