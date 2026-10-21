package com.hari.androidtvremote.ui.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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

internal data class StreamingTitle(val id: Int, val name: String, val overview: String, val poster: String, val media: String, val rating: Double = 0.0, val votes: Int = 0)
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
        val connection = URL("https://anytv-link-resolver-pilot.brenoff.workers.dev/v1/catalog/$route")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6000
            connection.readTimeout = 9000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "AnyTV/2.9.8 (Android; streaming catalog client)")
            val status = connection.responseCode
            if (status !in 200..299) {
                error("Catálogo temporariamente indisponível (HTTP $status).")
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
                        entry.optString("poster_path").takeIf { it.startsWith("/") } ?: "", media, entry.optDouble("vote_average", 0.0), entry.optInt("vote_count", 0)))
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

    /**
     * Region-aware preflight for the selected subscription provider.
     * A foreign-provider Wikidata link is not evidence of Brazilian rights.
     * Null means the catalogue check itself was inconclusive (e.g. offline).
     */
    suspend fun titleAvailableInBrazil(token: String, title: StreamingTitle, app: String): Boolean? =
        withContext(Dispatchers.IO) {
            runCatching {
                val id = providerId(token, app, title.media)
                if (id <= 0) null else offeredInBrazil(token, title, id)
            }.getOrNull()
        }

    /**
     * Non-authenticated title-ID crosswalk. TMDB supplies a Wikidata QID,
     * Wikidata stores provider IDs. No IDs are guessed from a film name.
     */
    suspend fun titleDeepLink(token: String, title: StreamingTitle, packageName: String): String? =
        withContext(Dispatchers.IO) {
            val provider = when (packageName) {
                "com.netflix.ninja", "com.netflix.mediaclient" -> "netflix"
                "com.disney.disneyplus" -> "disney"
                "com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient" -> "prime"
                "com.apple.atve.androidtv.appletv" -> "apple"
                "com.wbd.stream" -> "max"
                else -> return@withContext null
            }
            val ids = get(token, title.media + "/" + title.id + "/external_ids")
            val qid = ids.optString("wikidata_id").takeIf {
                it.matches(Regex("Q[1-9][0-9]{0,11}"))
            } ?: return@withContext null
            val connection = URL("https://www.wikidata.org/wiki/Special:EntityData/" +
                    qid + ".json").openConnection() as HttpURLConnection
            val claims = try {
                connection.connectTimeout = 5000
                connection.readTimeout = 8000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent",
                    "AnyTV/2.9.3 (https://github.com/brenoff2/tcl-apk-ci-builder)")
                if (connection.responseCode != 200) return@withContext null
                val entity = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    .optJSONObject("entities")?.optJSONObject(qid)
                    ?: return@withContext null
                entity.optJSONObject("claims") ?: return@withContext null
            } finally {
                connection.disconnect()
            }
            fun uniqueClaim(property: String): String? {
                val statements = claims.optJSONArray(property) ?: return null
                val values = (0 until statements.length()).mapNotNull { i ->
                    val statement = statements.optJSONObject(i) ?: return@mapNotNull null
                    if (statement.optString("rank") == "deprecated") return@mapNotNull null
                    val snak = statement.optJSONObject("mainsnak") ?: return@mapNotNull null
                    if (snak.optString("snaktype") != "value") return@mapNotNull null
                    snak.optJSONObject("datavalue")?.optString("value")
                        ?.takeIf { it.isNotBlank() }
                }.distinct()
                return values.singleOrNull()
            }
            when (provider) {
                "netflix" -> uniqueClaim("P1874")
                    ?.takeIf { it.matches(Regex("[0-9]{6,9}")) }
                    ?.let { "https://www.netflix.com/title/" + it }
                "disney" -> uniqueClaim("P13902")
                    ?.takeIf { it.matches(Regex("entity-[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) }
                    ?.let { "https://www.disneyplus.com/browse/" + it }
                "prime" -> uniqueClaim("P14462")
                    ?.takeIf { it.matches(Regex("amzn[0-9]\\.dv\\.gti\\.[a-f0-9-]{36}")) }
                    ?.let { "https://app.primevideo.com/detail?gti=" + it }
                    ?: uniqueClaim("P8055")
                        ?.takeIf { it.matches(Regex("B[0-9A-Z]{9}")) }
                        ?.let { "https://www.primevideo.com/detail/" + it }
                "apple" -> uniqueClaim(if (title.media == "tv") "P9751" else "P9586")
                    ?.takeIf { it.matches(Regex("umc\\.cmc\\.[a-z0-9]{22,25}")) }
                    ?.let { "https://tv.apple.com/" +
                        (if (title.media == "tv") "show/" else "movie/") + it }
                "max" -> uniqueClaim("P8298")
                    ?.takeIf { it.matches(Regex("(movie|show)/[a-f0-9-]{36}")) }
                    ?.let { "https://play.hbomax.com/" + it }
                else -> null
            }
        }

    /** Global editorial shelves independent of the TV's installed providers. */
    suspend fun discoveryShelf(route: String, media: String): List<StreamingTitle> =
        withContext(Dispatchers.IO) {
            require(route in setOf("trending/movie/week", "trending/tv/week",
                "movie/now_playing", "movie/top_rated", "tv/on_the_air", "movie/popular"))
            val payload = get("backend", route + "?language=pt-BR")
            mapTitles(payload.optJSONArray("results") ?: JSONArray(), media, 0)
        }

    /** Global search is independent of the installed-streaming provider. */
    suspend fun searchGlobal(query: String): List<StreamingTitle> = withContext(Dispatchers.IO) {
        require(query.length in 2..90)
        val encoded = URLEncoder.encode(query, "UTF-8")
        listOf("movie", "tv").flatMap { kind ->
            val response = get("backend", "search/$kind?language=pt-BR&query=$encoded")
            mapTitles(response.optJSONArray("results") ?: JSONArray(), kind, 0).take(12)
        }
    }

    suspend fun trailer(media: String, id: Int): String? = withContext(Dispatchers.IO) {
        val response = get("backend", "$media/$id/videos?language=pt-BR")
        val videos = response.optJSONArray("results") ?: JSONArray()
        (0 until videos.length()).mapNotNull { index ->
            val video = videos.optJSONObject(index) ?: return@mapNotNull null
            val key = video.optString("key")
            if (video.optString("site") == "YouTube" &&
                video.optString("type") == "Trailer" &&
                key.matches(Regex("[A-Za-z0-9_-]{11}"))) key else null
        }.firstOrNull()?.let { "https://www.youtube.com/watch?v=" + it }
    }

    suspend fun related(media: String, id: Int): List<StreamingTitle> = withContext(Dispatchers.IO) {
        val response = get("backend", "$media/$id/recommendations?language=pt-BR")
        mapTitles(response.optJSONArray("results") ?: JSONArray(), media, 0).take(12)
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
    // The client never requires user-supplied API credentials.
    val token = "backend"
    var showCredits by remember { mutableStateOf(false) }
    var showDeviceActivation by remember { mutableStateOf(false) }
    var activationDraft by remember { mutableStateOf("") }
    var activationBusy by remember { mutableStateOf(false) }
    var activationFeedback by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<TvOfficialApp?>(null) }
    var discoverySlide by remember { mutableStateOf(0) }
    var discoveryProviderPackage by remember { mutableStateOf<String?>(null) }
    var filteredShelves by remember { mutableStateOf<Map<String, List<StreamingTitle>>>(emptyMap()) }
    var filterBusy by remember { mutableStateOf(false) }
    var globalSearchDraft by remember { mutableStateOf("") }
    var globalSearchTerm by remember { mutableStateOf("") }
    var globalSearchResults by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    var globalSearchBusy by remember { mutableStateOf(false) }

    var discoveryShelves by remember {
        mutableStateOf<Map<String, List<StreamingTitle>>>(emptyMap())
    }
    var discoveryLoading by remember { mutableStateOf(false) }
    var discoveryError by remember { mutableStateOf<String?>(null) }

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
    var titleActionPending by remember { mutableStateOf(false) }
    var titleActionFeedback by remember { mutableStateOf<String?>(null) }
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
    var trailerLink by remember { mutableStateOf<String?>(null) }
    var relatedTitles by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    var detailLoading by remember { mutableStateOf(false) }
    val displayed = remember(apps, all) { apps.filter { all || StreamingCatalog.streaming(it) } }

    LaunchedEffect(globalSearchTerm) {
        globalSearchResults = emptyList()
        if (globalSearchTerm.length >= 2) {
            globalSearchBusy = true
            globalSearchResults = runCatching {
                StreamingCatalog.searchGlobal(globalSearchTerm)
            }.getOrDefault(emptyList())
            globalSearchBusy = false
        }
    }

    LaunchedEffect(details?.media, details?.id) {
        val item = details
        trailerLink = null
        relatedTitles = emptyList()
        if (item != null) {
            detailLoading = true
            trailerLink = runCatching { StreamingCatalog.trailer(item.media, item.id) }.getOrNull()
            relatedTitles = runCatching { StreamingCatalog.related(item.media, item.id) }
                .getOrDefault(emptyList())
            detailLoading = false
        }
    }

    LaunchedEffect(discoveryProviderPackage, apps) {
        val app = displayed.firstOrNull { it.packageName == discoveryProviderPackage }
        filteredShelves = emptyMap()
        if (app != null) {
            filterBusy = true
            val result = linkedMapOf<String, List<StreamingTitle>>()
            for ((label, kind) in listOf("Filmes disponíveis" to "movie", "Séries disponíveis" to "tv")) {
                runCatching {
                    StreamingCatalog.discover(token, app.name, kind, 1, 0, "")
                }.onSuccess { if (it.titles.isNotEmpty()) result[label] = it.titles }
            }
            filteredShelves = result
            filterBusy = false
        }
    }

    LaunchedEffect(token) {
        discoveryLoading = true
        discoveryError = null
        val shelves = linkedMapOf<String, List<StreamingTitle>>()
        for ((name, route, kind) in listOf(
            Triple("Em alta nesta semana", "trending/movie/week", "movie"),
            Triple("Lançamentos recentes", "movie/now_playing", "movie"),
            Triple("Mais bem avaliados no TMDB", "movie/top_rated", "movie"),
            Triple("Séries em destaque", "trending/tv/week", "tv"),
            Triple("Populares no TMDB", "movie/popular", "movie")
        )) {
            runCatching { StreamingCatalog.discoveryShelf(route, kind) }
                .onSuccess { if (it.isNotEmpty()) shelves[name] = it }
                .onFailure { discoveryError = "Algumas seções não puderam carregar." }
        }
        discoveryShelves = shelves
        discoveryLoading = false
    }

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
        if (titleActionPending) return
        if (host.isBlank() || pairing.isBlank()) {
            titleActionFeedback = "Vincule o AnyTV Connect com o código de oito dígitos. " +
                "A conexão do controle remoto é independente."
            return
        }
        titleActionPending = true
        titleActionFeedback = "Enviando comando à TCL…"
        scope.launch {
            try {
                // Personal-mode live link source takes priority over sparse Wikidata.
                // It is optional, never baked into the APK and never sent to the TV.
                val personalLookup = runCatching {
                    AnyTvPersonalLinks.resolve(context, title, packageName)
                }
                when (val direct = personalLookup?.getOrNull()) {
                    is AnyTvPersonalResult.Direct -> {
                        val mode = TclTvAppApi.openTitle(
                            host, pairing, packageName, direct.url, title.name)
                        titleActionFeedback = when (mode) {
                            "detail_requested" -> "Link da ficha enviado ao streaming. " +
                                "Confirme na TCL se abriu o filme correto."
                            "search" -> "Busca solicitada ao aplicativo na TCL; confira o resultado."
                            "opened_app" -> "O streaming abriu, mas não aceitou o link " +
                                "da ficha. Faça a busca pelo título na televisão."
                            else -> "O streaming não conseguiu abrir esta ficha na TV."
                        }
                        return@launch
                    }
                    AnyTvPersonalResult.NotInSubscription -> {
                        titleActionFeedback = "A fonte de streaming não confirma este título " +
                            "na assinatura selecionada no Brasil. " +
                            "Aluguel e canais adicionais não são assinatura básica."
                        return@launch
                    }
                    AnyTvPersonalResult.NoSupportedLink -> {
                        val mode = TclTvAppApi.searchTitle(
                            host, pairing, packageName, title.name)
                        titleActionFeedback = if (mode == "search")
                            "A fonte confirmou a assinatura, mas não forneceu um link " +
                                "compatível; a busca foi solicitada na TV."
                            else "A ficha direta não é compatível com a TCL. " +
                                "Abra a busca no aplicativo."
                        return@launch
                    }
                    else -> Unit // No personal key or a temporary API error: existing fallback.
                }
                // Provider IDs are worldwide; subscription rights are regional.
                // Fail closed when the TMDB BR availability check is negative or unknown.
                val appName = apps.firstOrNull { it.packageName == packageName }?.name
                    ?: "serviço selecionado"
                val offered = StreamingCatalog.titleAvailableInBrazil(
                    token, title, appName)
                if (offered == false) {
                    titleActionFeedback = "Segundo o catálogo TMDB/JustWatch, este título não está " +
                        "confirmado na assinatura de " + appName + " no Brasil. " +
                        "Confira 'Onde assistir e links disponíveis' antes de abrir."
                    return@launch
                }
                // If TMDB is temporarily unavailable, preserve the existing
                // search function rather than interrupting remote navigation.
                // Crucially, do NOT attempt an unverified deep link.
                val link = if (offered == true) runCatching {
                    StreamingCatalog.titleDeepLink(token, title, packageName)
                }.getOrNull() else null
                val mode = if (link != null)
                    TclTvAppApi.openTitle(host, pairing, packageName, link, title.name)
                else TclTvAppApi.searchTitle(host, pairing, packageName, title.name)
                titleActionFeedback = if (offered == null) {
                    "Não foi possível confirmar a disponibilidade no Brasil. " +
                        "Busca convencional solicitada ao streaming; confirme o resultado na TCL."
                } else when (mode) {
                    "detail_requested" -> "Link da ficha enviado ao streaming. Confira o título na TCL."
                    "search" -> "Busca enviada ao aplicativo na TCL. Confira o resultado na TV."
                    "opened_app" -> "Aplicativo aberto na TCL. O AnyTV Connect instalado " +
                        "não conseguiu abrir a ficha exata deste título; faça a busca no serviço."
                    else -> "O aplicativo não pôde ser aberto. Confira se o serviço está instalado " +
                        "e se o AnyTV Connect está vinculado."
                }
            } catch (error: Exception) {
                titleActionFeedback = error.message?.let { "Não foi possível abrir: " + it }
                    ?: "Não foi possível abrir o serviço na TCL."
            } finally {
                titleActionPending = false
            }
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
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = globalSearchDraft,
                            onValueChange = { globalSearchDraft = it.take(90) },
                            label = { Text("Buscar filme ou série") },
                            singleLine = true,
                            modifier = Modifier.weight(1f))
                        Button(onClick = { globalSearchTerm = globalSearchDraft.trim() },
                            enabled = globalSearchDraft.trim().length >= 2) { Text("Buscar") }
                        if (globalSearchTerm.isNotBlank()) TextButton(onClick = {
                            globalSearchDraft = ""; globalSearchTerm = ""
                        }) { Text("×") }
                    }
                    if (globalSearchBusy) CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    if (globalSearchTerm.isNotBlank() && !globalSearchBusy) {
                        Text("Resultados para: " + globalSearchTerm, fontSize = 12.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            globalSearchResults.forEach { found ->
                                Column(modifier = Modifier.width(110.dp).clickable {
                                    details = found; detailsPackage = null
                                }) {
                                    AsyncImage(model = "https://image.tmdb.org/t/p/w185" + found.poster,
                                        contentDescription = found.name,
                                        modifier = Modifier.fillMaxWidth().height(150.dp),
                                        contentScale = ContentScale.Crop)
                                    Text(found.name, fontSize = 11.sp, maxLines = 2)
                                }
                            }
                        }
                        if (globalSearchResults.isEmpty()) Text("Nenhum resultado encontrado.")
                    }
                    Text("Filtrar destaques por plataforma instalada na TV",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = discoveryProviderPackage == null,
                            onClick = { discoveryProviderPackage = null },
                            label = { Text("Todos") })
                        displayed.filter { StreamingCatalog.streaming(it) }.forEach { app ->
                            FilterChip(selected = discoveryProviderPackage == app.packageName,
                                onClick = { discoveryProviderPackage = app.packageName },
                                label = { Text(app.name, maxLines = 1) })
                        }
                    }
                    if (filterBusy) CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    Text("Descubra filmes e séries em várias plataformas.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (discoveryLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    if (discoveryError != null && discoveryProviderPackage == null) Text(discoveryError.orEmpty(), fontSize = 12.sp)
                    if (discoveryProviderPackage != null && !filterBusy && filteredShelves.isEmpty())
                        Text("Não há títulos de assinatura confirmados para este serviço no Brasil.")
                    val visibleShelves = if (discoveryProviderPackage == null) discoveryShelves else filteredShelves
                    val heroTitles = visibleShelves.values.firstOrNull().orEmpty().take(6)
                    if (heroTitles.isNotEmpty()) {
                        val current = heroTitles[discoverySlide % heroTitles.size]
                        AsyncImage(model = "https://image.tmdb.org/t/p/w780" + current.poster,
                            contentDescription = "Destaque: " + current.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                                .pointerInput(heroTitles.size) {
                                    var drag = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { drag = 0f },
                                        onDragEnd = {
                                            if (drag > 60f) {
                                                discoverySlide = (discoverySlide + heroTitles.size - 1) % heroTitles.size
                                            } else if (drag < -60f) {
                                                discoverySlide = (discoverySlide + 1) % heroTitles.size
                                            }
                                        },
                                        onHorizontalDrag = { change, amount ->
                                            change.consume()
                                            drag += amount
                                        })
                                }.clickable {
                                    details = current
                                    detailsPackage = null
                                })
                        Text(current.name, fontWeight = FontWeight.Bold, maxLines = 2)
                        if (current.votes > 0) Text("★ TMDB " + String.format(java.util.Locale.US, "%.1f", current.rating), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                discoverySlide = (discoverySlide + heroTitles.size - 1) % heroTitles.size
                            }) { Text("‹ Anterior") }
                            OutlinedButton(onClick = {
                                discoverySlide = (discoverySlide + 1) % heroTitles.size
                            }) { Text("Próximo ›") }
                            TextButton(onClick = {
                                details = current
                                detailsPackage = null
                            }) { Text("Detalhes") }
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        visibleShelves.forEach { (heading, shelf) ->
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(heading, fontWeight = FontWeight.SemiBold)
                                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        shelf.take(12).forEach { item ->
                                            Column(modifier = Modifier.width(116.dp).clickable {
                                                details = item
                                                detailsPackage = null
                                            }) {
                                                AsyncImage(model = "https://image.tmdb.org/t/p/w185" + item.poster,
                                                    contentDescription = "Cartaz: " + item.name,
                                                    modifier = Modifier.fillMaxWidth().height(158.dp),
                                                    contentScale = ContentScale.Crop)
                                                Text(item.name, maxLines = 2, fontSize = 11.sp)
                                                if (item.votes > 0) Text("★ " + String.format(java.util.Locale.US, "%.1f", item.rating), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Text("Explorar por plataforma",
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                        }
                        items(displayed, key = { it.packageName }) { app ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                selected = app; media = "movie"; genre = 0; page = 1
                                searchDraft = ""; searchTerm = ""; showSaved = false; message = null
                            }.padding(7.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                OfficialTvIcon(app, 48.dp)
                                Text(app.name, modifier = Modifier.weight(1f), maxLines = 1)
                                Text("›", fontSize = 22.sp)
                            }
                        }
                    }
                } else {
                    val app = selected!!
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        IconButton(onClick = {
                            selected = null; details = null; message = null
                        }, modifier = Modifier.size(40.dp).semantics {
                            contentDescription = "Voltar aos serviços"
                        }) { Text("‹", fontSize = 24.sp) }
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
                        IconButton(onClick = onDismiss,
                            modifier = Modifier.size(40.dp).semantics {
                                contentDescription = "Fechar Streaming"
                            }) { Text("×", fontSize = 24.sp) }
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
                TextButton(onClick = {
                    activationDraft = ""
                    activationFeedback = null
                    showDeviceActivation = true
                }) { Text("Ativar links diretos neste dispositivo", fontSize = 12.sp) }
                Text("ⓘ Fontes: TMDB / JustWatch",
                    modifier = Modifier.clickable { showCredits = true },
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1)
            }
        }
    }
    if (showDeviceActivation) {
        AlertDialog(
            onDismissRequest = { if (!activationBusy) showDeviceActivation = false },
            title = { Text("Ativação dos links diretos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Informe apenas o código de ativação individual fornecido " +
                        "pelo operador do AnyTV. Não é uma chave de API.",
                        fontSize = 12.sp)
                    OutlinedTextField(
                        value = activationDraft,
                        onValueChange = {
                            activationDraft = it.take(43)
                            activationFeedback = null
                        },
                        singleLine = true,
                        label = { Text("Código de ativação") },
                        enabled = !activationBusy
                    )
                    if (activationFeedback != null) {
                        Text(activationFeedback.orEmpty(), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !activationBusy &&
                        activationDraft.matches(Regex("[A-Za-z0-9_-]{43}")),
                    onClick = {
                        if (!activationBusy) {
                            val submitted = activationDraft
                            activationBusy = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        AnyTvDeviceIdentity.activate(context, submitted)
                                    }
                                }
                                activationDraft = ""
                                activationBusy = false
                                activationFeedback = if (result.getOrDefault(false)) {
                                    "Dispositivo ativado. Links diretos disponíveis quando " +
                                        "oferecidos pelo serviço."
                                } else {
                                    "Ativação não concluída. Confira o código ou tente novamente."
                                }
                            }
                        }
                    }
                ) { Text(if (activationBusy) "Ativando…" else "Ativar") }
            },
            dismissButton = {
                TextButton(
                    enabled = !activationBusy,
                    onClick = {
                        activationDraft = ""
                        showDeviceActivation = false
                    }
                ) { Text("Fechar") }
            }
        )
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
            onDismissRequest = {
                details = null; detailsPackage = null; titleActionFeedback = null
            },
            title = { Text(title.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (titleActionFeedback != null) {
                        Text(titleActionFeedback.orEmpty(), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(title.overview.ifBlank { "Sinopse indisponível." })
                    if (title.votes > 0) {
                        Text("★ Nota TMDB: " +
                            String.format(java.util.Locale.US, "%.1f", title.rating) +
                            " (" + title.votes + " votos)", fontSize = 12.sp)
                    }
                    if (detailLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    val video = trailerLink
                    if (video != null) {
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video)))
                        }) { Text("▶ Ver trailer") }
                    } else if (!detailLoading) {
                        Text("Trailer não disponível.", fontSize = 12.sp)
                    }
                    if (relatedTitles.isNotEmpty()) {
                        Text("Você também pode gostar", fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            relatedTitles.forEach { suggested ->
                                Column(modifier = Modifier.width(100.dp).clickable {
                                    details = suggested
                                    detailsPackage = null
                                }) {
                                    AsyncImage(model = "https://image.tmdb.org/t/p/w185" + suggested.poster,
                                        contentDescription = suggested.name,
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        contentScale = ContentScale.Crop)
                                    Text(suggested.name, fontSize = 11.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                    if (selected == null) {
                        Text("Escolha um aplicativo da TCL para abrir este título:",
                            fontSize = 12.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            displayed.filter { StreamingCatalog.streaming(it) }.forEach { app ->
                                FilterChip(
                                    selected = detailsPackage == app.packageName,
                                    onClick = { detailsPackage = app.packageName },
                                    label = { Text(app.name, maxLines = 1, fontSize = 11.sp) })
                            }
                        }
                        Text("A disponibilidade por assinatura deve ser confirmada na plataforma.",
                            fontSize = 11.sp)
                    }
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
                    if (pkg.isBlank()) {
                        titleActionFeedback = "O aplicativo deste título não foi encontrado na TCL."
                    } else {
                        searchOnTv(title, pkg)
                    }
                }, enabled = !titleActionPending) {
                    Text(if (titleActionPending) "Abrindo…" else "Abrir na TCL")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    details = null; detailsPackage = null; titleActionFeedback = null
                }) {
                    Text("Voltar")
                }
            }
        )
    }
}
