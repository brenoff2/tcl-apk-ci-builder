package com.hari.androidtvremote.ui.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer

internal data class StreamingTitle(val id: Int, val name: String, val overview: String, val poster: String, val media: String)
internal data class StreamingGenre(val id: Int, val name: String)
internal data class StreamingFavorite(val title: StreamingTitle, val packageName: String)
internal data class StreamingResults(val titles: List<StreamingTitle>, val more: Boolean)

/** Real TMDB/JustWatch availability. This API cannot start playback on a third-party TV app. */
private object StreamingCatalog {
    private fun norm(v: String) = Normalizer.normalize(v.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("[^a-z0-9]"), "")

    private fun provider(name: String): String? {
        val v = norm(name)
        return when {
            "netflix" in v -> "netflix"
            "primevideo" in v -> "amazonprimevideo"
            "disney" in v -> "disneyplus"
            "globoplay" in v -> "globoplay"
            "hbomax" in v || v == "max" -> "hbomax"
            "appletv" in v -> "appletv"
            "paramount" in v -> "paramountplus"
            "crunchyroll" in v -> "crunchyroll"
            "plutotv" in v -> "plutotv"
            "mubi" in v -> "mubi"
            "telecine" in v -> "telecine"
            "looke" in v -> "looke"
            "clarovideo" in v -> "clarovideo"
            else -> null
        }
    }

    fun streaming(app: TvOfficialApp): Boolean =
        provider(app.name) != null ||
            listOf("youtube", "spotify", "plex", "twitch", "vimeo", "dazn")
                .any { it in norm(app.name) || it in norm(app.packageName) }

    private fun get(token: String, route: String): JSONObject {
        require(token.isNotBlank()) { "Informe seu token de leitura TMDB." }
        val connection = URL("https://api.themoviedb.org/3/$route")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6000
            connection.readTimeout = 9000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val status = connection.responseCode
            if (status !in 200..299) {
                error(if (status == 401 || status == 403) "Token TMDB não autorizado."
                    else "TMDB indisponível (HTTP $status).")
            }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    suspend fun genres(token: String, media: String): List<StreamingGenre> =
        withContext(Dispatchers.IO) {
            val data = get(token, "genre/$media/list?language=pt-BR").optJSONArray("genres")
                ?: JSONArray()
            (0 until data.length()).mapNotNull { index ->
                val entry = data.optJSONObject(index) ?: return@mapNotNull null
                val id = entry.optInt("id")
                val name = entry.optString("name")
                if (id > 0 && name.isNotBlank()) StreamingGenre(id, name) else null
            }
        }

    private fun mapTitles(items: JSONArray, media: String, genre: Int): List<StreamingTitle> =
        buildList {
            for (i in 0 until items.length()) {
                val entry = items.optJSONObject(i) ?: continue
                val movieId = entry.optInt("id")
                val name = entry.optString(if (media == "tv") "name" else "title")
                val itemGenres = entry.optJSONArray("genre_ids")
                val matchGenre = genre == 0 || (itemGenres != null &&
                    (0 until itemGenres.length()).any { itemGenres.optInt(it) == genre })
                if (movieId > 0 && name.isNotBlank() && matchGenre) {
                    add(StreamingTitle(movieId, name, entry.optString("overview"),
                        entry.optString("poster_path").takeIf { it.startsWith("/") } ?: "", media))
                }
            }
        }

    private fun providerId(token: String, app: String, media: String): Int {
        val alias = provider(app) ?: return 0
        val providers = get(token, "watch/providers/$media?language=pt-BR&watch_region=BR")
            .optJSONArray("results") ?: JSONArray()
        for (i in 0 until providers.length()) {
            val entry = providers.optJSONObject(i) ?: continue
            if (norm(entry.optString("provider_name")) == alias) return entry.optInt("provider_id")
        }
        return 0
    }

    private fun offeredInBrazil(token: String, item: StreamingTitle, providerId: Int): Boolean {
        val result = get(token, "${item.media}/${item.id}/watch/providers")
            .optJSONObject("results")?.optJSONObject("BR")?.optJSONArray("flatrate")
            ?: return false
        return (0 until result.length()).any {
            result.optJSONObject(it)?.optInt("provider_id") == providerId
        }
    }

    suspend fun discover(
        token: String, app: String, media: String, page: Int, genre: Int, query: String
    ): StreamingResults = withContext(Dispatchers.IO) {
        val providerId = providerId(token, app, media)
        if (providerId == 0) return@withContext StreamingResults(emptyList(), false)
        val search = query.isNotBlank()
        val url = if (search)
            "search/$media?language=pt-BR&include_adult=false&query=" +
                URLEncoder.encode(query, "UTF-8") + "&page=$page"
        else "discover/$media?language=pt-BR&watch_region=BR" +
            "&with_watch_providers=$providerId&with_watch_monetization_types=flatrate" +
            (if (genre > 0) "&with_genres=$genre" else "") +
            "&include_adult=false&sort_by=popularity.desc&page=$page"
        val result = get(token, url)
        val candidates = mapTitles(result.optJSONArray("results") ?: JSONArray(), media, genre)
        // Search endpoints have no provider filter: verify real BR subscription offers.
        val titles = if (search) coroutineScope {
            val limit = Semaphore(4)
            candidates.map { item ->
                async {
                    val available = limit.withPermit {
                        runCatching { offeredInBrazil(token, item, providerId) }.getOrDefault(false)
                    }
                    if (available) item else null
                }
            }.awaitAll().filterNotNull()
        } else candidates
        StreamingResults(titles, page < result.optInt("total_pages", page))
    }
}

@Composable
internal fun TclStreamingScreens(
    apps: List<TvOfficialApp>, host: String, pairing: String, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("anytv_streaming_catalog", Context.MODE_PRIVATE)
    }
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(prefs.getString("tmdb_token", "").orEmpty()) }
    var tokenDraft by remember { mutableStateOf(token) }
    var editToken by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<TvOfficialApp?>(null) }
    var all by remember { mutableStateOf(false) }
    var media by remember { mutableStateOf("movie") }
    var page by remember { mutableStateOf(1) }
    var searchDraft by remember { mutableStateOf("") }
    var searchTerm by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf(0) }
    var genres by remember { mutableStateOf<List<StreamingGenre>>(emptyList()) }
    var gridView by remember { mutableStateOf(prefs.getBoolean("streaming_grid", false)) }
    var showSaved by remember { mutableStateOf(false) }
    var detailsPackage by remember { mutableStateOf<String?>(null) }
    var favorites by remember {
        mutableStateOf(runCatching {
            val arr = JSONArray(prefs.getString("streaming_favorites", "[]"))
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optInt("id")
                val kind = o.optString("media")
                val pkg = o.optString("package")
                if (id <= 0 || pkg.isBlank() || (kind != "tv" && kind != "movie"))
                    return@mapNotNull null
                StreamingFavorite(
                    StreamingTitle(id, o.optString("name"), o.optString("overview"),
                        o.optString("poster"), kind), pkg)
            }
        }.getOrDefault(emptyList<StreamingFavorite>()))
    }
    fun isFavorite(title: StreamingTitle, packageName: String): Boolean =
        favorites.any { it.title.id == title.id && it.title.media == title.media &&
            it.packageName == packageName }
    fun toggleFavorite(title: StreamingTitle, packageName: String) {
        favorites = if (isFavorite(title, packageName)) favorites.filterNot {
            it.title.id == title.id && it.title.media == title.media &&
                it.packageName == packageName
        } else listOf(StreamingFavorite(title, packageName)) + favorites
        val json = JSONArray()
        favorites.forEach { value ->
            json.put(JSONObject().put("id", value.title.id)
                .put("media", value.title.media)
                .put("package", value.packageName)
                .put("name", value.title.name)
                .put("overview", value.title.overview)
                .put("poster", value.title.poster))
        }
        prefs.edit().putString("streaming_favorites", json.toString()).apply()
    }
    var titles by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    var more by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<StreamingTitle?>(null) }
    val displayed = remember(apps, all) { apps.filter { all || StreamingCatalog.streaming(it) } }

    LaunchedEffect(token, media) {
        genres = if (token.isBlank()) emptyList() else
            runCatching { StreamingCatalog.genres(token, media) }
                .getOrDefault(emptyList())
    }

    LaunchedEffect(selected?.packageName, media, page, token, genre, searchTerm, showSaved) {
        val app = selected ?: return@LaunchedEffect
        if (page == 1) { titles = emptyList(); more = false }
        if (showSaved || token.isBlank()) return@LaunchedEffect
        loading = true
        message = null
        try {
            val response = StreamingCatalog.discover(token, app.name, media, page, genre, searchTerm)
            titles = if (page == 1) response.titles else (titles + response.titles).distinctBy { it.id }
            more = response.more
            if (page == 1 && response.titles.isEmpty())
                message = "Nenhum catálogo de assinatura compatível encontrado para este serviço."
        } catch (error: Exception) {
            message = error.message ?: "Falha ao consultar catálogo."
        } finally {
            loading = false
        }
    }

    fun openOnTv(app: TvOfficialApp) {
        if (host.isBlank() || pairing.isBlank()) {
            message = "Conecte o AnyTV Connect e vincule a TV antes de abrir aplicativos."
        } else {
            scope.launch {
                message = null
                runCatching { TclTvAppApi.open(host, pairing, app.packageName) }
                    .onSuccess { message = if (it) "Abertura solicitada na TCL." else "A TV não abriu o aplicativo." }
                    .onFailure { message = it.message ?: "Não foi possível abrir na TV." }
            }
        }
    }

    fun searchOnTv(title: StreamingTitle, packageName: String) {
        if (host.isBlank() || pairing.isBlank()) {
            message = "Conecte e vincule a TCL para localizar o título."
            return
        }
        scope.launch {
            message = null
            runCatching { TclTvAppApi.searchTitle(host, pairing, packageName, title.name) }
                .onSuccess { mode ->
                    message = when (mode) {
                        "search" -> "Busca pelo título enviada à TCL. Selecione o resultado."
                        "opened_app" -> "O serviço não aceita busca externa; o aplicativo foi aberto."
                        else -> "O serviço não pôde ser aberto na TV."
                    }
                }
                .onFailure { message = "Atualize o AnyTV Connect na TCL para pesquisar títulos." }
        }
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (selected == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Streaming", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("Fechar") }
                    }
                    Text("Escolha um aplicativo realmente instalado na TCL.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Serviços", fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { all = !all }) {
                            Text(if (all) "Só streaming" else "Ver todos")
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(displayed, key = { it.packageName }) { app ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                    selected = app; media = "movie"; genre = 0; page = 1
                                    searchDraft = ""; searchTerm = ""; showSaved = false
                                    message = null
                                }.padding(7.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                OfficialTvIcon(app, 48.dp)
                                Text(app.name, modifier = Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("›", fontSize = 22.sp)
                            }
                        }
                        if (displayed.isEmpty()) item {
                            Text("Nenhum serviço identificado. Atualize os aplicativos da TCL.")
                        }
                    }
                } else {
                    val app = selected!!
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TextButton(onClick = {
                            selected = null; details = null; message = null
                        }, modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp)) {
                            Text("‹", fontSize = 24.sp)
                        }
                        OfficialTvIcon(app, 32.dp)
                        Text(app.name, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(start = 3.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontSize = 16.sp)
                        TextButton(onClick = { openOnTv(app) },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(40.dp)) {
                            Text("Abrir na TCL", fontSize = 12.sp, maxLines = 1)
                        }
                        TextButton(onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 3.dp),
                            modifier = Modifier.height(40.dp)) {
                            Text("×", fontSize = 24.sp)
                        }
                    }
                    if (editToken || token.isBlank()) {
                        Text("Para consultar filmes e séries reais, configure um token de leitura " +
                            "TMDB. Aplicativos sem provedor TMDB continuam disponíveis para abertura na TV.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = tokenDraft,
                            onValueChange = { tokenDraft = it.trim() },
                            label = { Text("Token TMDB") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://www.themoviedb.org/settings/api")))
                            }) { Text("Obter token") }
                            Spacer(Modifier.weight(1f))
                            Button(onClick = {
                                token = tokenDraft
                                prefs.edit().putString("tmdb_token", tokenDraft).apply()
                                editToken = false; page = 1
                            }) { Text("Salvar") }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected = media == "movie" && !showSaved,
                            onClick = { media = "movie"; genre = 0; page = 1; showSaved = false },
                            label = { Text("Filmes", fontSize = 12.sp) })
                        FilterChip(selected = media == "tv" && !showSaved,
                            onClick = { media = "tv"; genre = 0; page = 1; showSaved = false },
                            label = { Text("Séries", fontSize = 12.sp) })
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showSaved = !showSaved; page = 1 },
                            modifier = Modifier.size(40.dp).semantics {
                                contentDescription = if (showSaved) "Voltar ao catálogo" else "Minha lista"
                            }) {
                            Text(if (showSaved) "▤" else "♥", fontSize = 20.sp)
                        }
                        IconButton(onClick = {
                            gridView = !gridView
                            prefs.edit().putBoolean("streaming_grid", gridView).apply()
                        }, modifier = Modifier.size(40.dp).semantics {
                            contentDescription = if (gridView) "Visualização em lista" else "Visualização em grade"
                        }) {
                            Text(if (gridView) "☷" else "▦", fontSize = 19.sp)
                        }
                        IconButton(onClick = { editToken = !editToken },
                            modifier = Modifier.size(40.dp).semantics {
                                contentDescription = "Configurar catálogo"
                            }) {
                            Text("⚙", fontSize = 19.sp)
                        }
                    }
                    if (!showSaved) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            BasicTextField(value = searchDraft,
                                onValueChange = { searchDraft = it },
                                modifier = Modifier.weight(1f).height(42.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                textStyle = TextStyle(fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    searchTerm = searchDraft.trim(); page = 1
                                }),
                                decorationBox = { field ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchDraft.isEmpty()) Text("Buscar título",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp)
                                        field()
                                    }
                                })
                            Button(onClick = {
                                searchTerm = searchDraft.trim(); page = 1
                            }, modifier = Modifier.height(42.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)) {
                                Text("Buscar", fontSize = 13.sp)
                            }
                            if (searchTerm.isNotBlank()) TextButton(onClick = {
                                searchDraft = ""; searchTerm = ""; page = 1
                            }) { Text("×") }
                        }
                        Row(modifier = Modifier.fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = genre == 0,
                                onClick = { genre = 0; page = 1 },
                                label = { Text("Todos") })
                            genres.forEach { option ->
                                FilterChip(selected = genre == option.id,
                                    onClick = { genre = option.id; page = 1 },
                                    label = { Text(option.name) })
                            }
                        }
                    } else {
                        Text("Filmes e séries favoritos de todos os serviços",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (loading && !showSaved) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    if (message != null) Text(message.orEmpty(), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val visibleTitles = if (showSaved) favorites.map { it.title } else titles
                    if (showSaved && visibleTitles.isEmpty()) Text(
                        "Sua lista está vazia. Abra um título e toque em Adicionar aos favoritos.",
                        fontSize = 12.sp
                    )
                    if (gridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(94.dp),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            gridItems(visibleTitles) { title ->
                                Column(modifier = Modifier.clickable {
                                    details = title
                                    detailsPackage = if (showSaved)
                                        favorites.firstOrNull { it.title.id == title.id &&
                                            it.title.media == title.media }?.packageName
                                            ?: app.packageName
                                        else app.packageName
                                }, horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (title.poster.isNotBlank()) {
                                        AsyncImage(
                                            model = "https://image.tmdb.org/t/p/w185" + title.poster,
                                            contentDescription = "Cartaz: " + title.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                        )
                                    }
                                    Text(title.name, fontSize = 11.sp, maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (more && !showSaved) item(span = {
                                GridItemSpan(maxLineSpan)
                            }) {
                                OutlinedButton(onClick = { if (!loading) page++ },
                                    enabled = !loading,
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text("Carregar mais")
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            items(visibleTitles) { title ->
                                val itemPackage = if (showSaved)
                                    favorites.firstOrNull { it.title.id == title.id &&
                                        it.title.media == title.media }?.packageName
                                        ?: app.packageName
                                    else app.packageName
                                Row(modifier = Modifier.fillMaxWidth().clickable {
                                    details = title; detailsPackage = itemPackage
                                }.padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    if (title.poster.isNotEmpty()) {
                                        AsyncImage(
                                            model = "https://image.tmdb.org/t/p/w185" + title.poster,
                                            contentDescription = "Cartaz: " + title.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(63.dp, 95.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title.name, fontWeight = FontWeight.SemiBold,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(title.overview.ifBlank { "Sinopse indisponível." },
                                            fontSize = 12.sp, maxLines = 3,
                                            overflow = TextOverflow.Ellipsis)
                                        Text("Ver título ›", fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(onClick = { toggleFavorite(title, itemPackage) }) {
                                        Text(if (isFavorite(title, itemPackage)) "♥" else "♡")
                                    }
                                }
                            }
                            if (more && !showSaved) item {
                                OutlinedButton(onClick = { if (!loading) page++ },
                                    enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                                    Text("Carregar mais")
                                }
                            }
                        }
                    }
                }
                Text("ⓘ Fontes: TMDB / JustWatch",
                    modifier = Modifier.clickable { showCredits = true },
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1)
            }
        }
    }
    if (showCredits) {
        AlertDialog(onDismissRequest = { showCredits = false },
            title = { Text("Fontes do catálogo") },
            text = { Text("Disponibilidade no Brasil: TMDB / JustWatch. " +
                "Este produto usa a API TMDB, mas não é endossado ou certificado pelo TMDB.") },
            confirmButton = {
                TextButton(onClick = { showCredits = false }) { Text("Fechar") }
            })
    }
    val title = details
    if (title != null) {
        val pkg = detailsPackage ?: selected?.packageName.orEmpty()
        AlertDialog(
            onDismissRequest = { details = null; detailsPackage = null },
            title = { Text(title.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(title.overview.ifBlank { "Sinopse indisponível." })
                    Text("A TV poderá abrir a busca do título se este serviço aceitar " +
                        "pesquisa externa. Não existe deep link universal de reprodução.",
                        fontSize = 12.sp)
                    TextButton(onClick = { if (pkg.isNotBlank()) toggleFavorite(title, pkg) }) {
                        Text(if (isFavorite(title, pkg)) "♥ Remover dos favoritos"
                            else "♡ Adicionar aos favoritos")
                    }
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.themoviedb.org/" +
                                title.media + "/" + title.id + "/watch?locale=BR")))
                    }) { Text("Onde assistir e links disponíveis") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    details = null; detailsPackage = null
                    if (pkg.isNotBlank()) searchOnTv(title, pkg)
                }) { Text("Localizar título na TCL") }
            },
            dismissButton = {
                TextButton(onClick = { details = null; detailsPackage = null }) {
                    Text("Voltar")
                }
            }
        )
    }
}
