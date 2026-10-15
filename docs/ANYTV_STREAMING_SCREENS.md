# AnyTV 2.8 — telas de streaming reais (branch, não distribuídas)

## Implementado na fonte

- Entrada **Streaming** na faixa de aplicativos do AnyTV, sem remover o controle e os favoritos.
- Uma tela por aplicativo **realmente instalado na TCL**, com ícone oficial recebido do AnyTV Connect 1.6, sem lista fictícia.
- Botão **Abrir na TCL** reutiliza o pareamento de oito dígitos e a rota existente `/launch`, mantendo a compatibilidade com AnyTV Connect 1.6.
- Catálogo de filmes e séries por serviço e por região Brasil (TMDB/JustWatch): cartazes, sinopses, navegação por páginas e detalhes. O conteúdo só é exibido após configuração do **TMDB API Read Access Token** pelo usuário e quando a API reconhece o serviço.
- Para YouTube, Spotify, Plex e aplicativos sem integração TMDB, exibe a tela de serviço e permite abrir o aplicativo instalado na TV, **sem fingir acesso ao catálogo**.
- Falhas de rede, conta, pareamento e autorização retornam mensagens; nenhuma credencial fixa é publicada no repositório.

## Restrições de publicação e testes

1. TMDB informa disponibilidade, não fornece deep link garantido por título; o botão abre **o aplicativo**, não um filme automaticamente.
2. Token de leitura guardado em SharedPreferences privadas no telefone. Não inserir tokens em commits ou logs. Para distribuição pública, revisar o desenho de gestão da credencial.
3. Os termos do TMDB exigem **logo oficial de atribuição**, aviso de não endosso e aprovação/licença pertinente ao uso comercial; verificar antes da publicação. Crédito ao JustWatch também é obrigatório para informação de disponibilidade.
4. Os testes de contrato e compilação CI não substituem a homologação em celular/TV, em especial a abertura de aplicativos, o layout de tela e a consulta autenticada à API.
5. A versão 2.8 deste branch é artefato **não assinado** até a verificação de CI e do certificado original. A distribuição permanece no Android 2.7 e AnyTV Connect 1.6.

## Referências oficiais
- https://developer.themoviedb.org/reference/discover-movie
- https://developer.themoviedb.org/reference/discover-tv
- https://developer.themoviedb.org/reference/tv-series-watch-providers
- https://developer.themoviedb.org/docs/authentication-application
- https://www.themoviedb.org/api-terms-of-use
