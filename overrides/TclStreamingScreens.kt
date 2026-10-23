package com.hari.androidtvremote.ui.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer

internal data class StreamingTitle(val id: Int, val name: String, val overview: String, val poster: String, val media: String, val rating: Double = 0.0, val votes: Int = 0, val backdrop: String = "", val date: String = "", val genreIds: List<Int> = emptyList())
internal data class StreamingGenre(val id: Int, val name: String)
internal data class StreamingSeason(val number: Int, val name: String, val count: Int)
internal data class StreamingEpisode(val number: Int, val name: String, val airDate: String)
internal data class StreamingOffer(val id: Int, val name: String, val logo: String)
internal data class StreamingFavorite(val title: StreamingTitle, val packageName: String)
internal data class StreamingResults(val titles: List<StreamingTitle>, val more: Boolean)

/** Small persistent, bounded catalogue cache. Does not store credentials or viewing progress. */
private object StreamingDiskCache {
    private var root: File? = null
    fun configure(context: Context) {
        if (root == null) synchronized(this) {
            if (root == null) root = File(context.applicationContext.filesDir, "anytv_catalog_v1")
                .apply { mkdirs() }
        }
    }
    fun rememberTitle(media: String, id: Int, payload: JSONObject) {
        // User-requested perpetual cache: once seen, title details stay until app data
        // is intentionally cleared. Editorial/availability responses still use TTL.
        val dir = root ?: return
        if (id < 1 || media !in listOf("movie", "tv")) return
        val target = File(dir, "saved_title_${media}_${id}.json")
        val prior = runCatching { JSONObject(target.readText()) }.getOrNull()
        val merged = JSONObject(prior?.toString() ?: "{}")
        for (field in listOf("name", "overview", "date", "poster", "backdrop", "genres")) {
            val value = payload.opt(field)
            if (value != null && value.toString().isNotBlank() && value.toString() != "[]")
                merged.put(field, value)
        }
        runCatching { target.writeText(merged.toString()) }
    }
    fun previousTitle(media: String, id: Int): JSONObject? {
        val dir = root ?: return null
        if (id < 1 || media !in listOf("movie", "tv")) return null
        return runCatching {
            JSONObject(File(dir, "saved_title_${media}_${id}.json").readText())
        }.getOrNull()
    }
    private fun file(route: String): File? {
        val dir = root ?: return null
        val sha = MessageDigest.getInstance("SHA-256").digest(route.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, sha + ".json")
    }
    @Synchronized fun read(route: String): JSONObject? {
        val target = file(route) ?: return null
        return runCatching {
            if (!target.isFile || target.length() > 480000L) return null
            val entry = JSONObject(target.readText(Charsets.UTF_8))
            if (entry.optLong("until", 0) <= System.currentTimeMillis()) {
                target.delete()
                return null
            }
            target.setLastModified(System.currentTimeMillis())
            entry.getJSONObject("value")
        }.getOrNull()
    }
    @Synchronized fun write(route: String, value: JSONObject, ttlMillis: Long) {
        val target = file(route) ?: return
        runCatching {
            val entry = JSONObject().put("until", System.currentTimeMillis() + ttlMillis)
                .put("value", value).toString()
            if (entry.length > 470000) return
            target.writeText(entry, Charsets.UTF_8)
            val files = target.parentFile?.listFiles { it.isFile && it.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() } ?: return
            var bytes = 0L
            files.forEachIndexed { index, item ->
                bytes += item.length()
                if (index >= 450 || bytes > 12_000_000L) item.delete()
            }
        }
    }
}

/** Real TMDB/JustWatch availability. This API cannot start playback on a third-party TV app. */
private object StreamingCatalog {
    fun configure(context: Context) = StreamingDiskCache.configure(context)
    private fun ttl(route: String): Long = when {
        route.startsWith("genre/") -> 30L * 86400000L
        route.matches(Regex("(movie|tv)/[0-9]+/season/[0-9]+.*")) -> 7L * 86400000L
        route.matches(Regex("(movie|tv)/[0-9]+(\\?.*)?")) -> 14L * 86400000L
        route.contains("/watch/providers") -> 12L * 3600000L
        route.contains("external_ids") -> 30L * 86400000L
        route.contains("trending") || route.contains("now_playing") -> 6L * 3600000L
        else -> 12L * 3600000L
    }

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
        StreamingDiskCache.read(route)?.let { return it }
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
            val payload = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            StreamingDiskCache.write(route, payload, ttl(route))
            return payload
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
                    val previous = StreamingDiskCache.previousTitle(media, movieId)
                    val overview = entry.optString("overview").ifBlank {
                        previous?.optString("overview").orEmpty()
                    }
                    val released = entry.optString(
                        if (media == "tv") "first_air_date" else "release_date"
                    ).ifBlank {
                        previous?.optString("date").orEmpty()
                    }
                    val poster = entry.optString("poster_path").takeIf {
                        it.startsWith("/")
                    } ?: previous?.optString("poster").orEmpty()
                    val backdrop = entry.optString("backdrop_path").takeIf {
                        it.startsWith("/")
                    } ?: previous?.optString("backdrop").orEmpty()
                    val genreIds = (0 until (itemGenres?.length() ?: 0))
                        .map { itemGenres!!.optInt(it) }.filter { it > 0 }
                    add(StreamingTitle(movieId, name, overview, poster, media,
                        entry.optDouble("vote_average", 0.0),
                        entry.optInt("vote_count", 0), backdrop, released, genreIds))
                    StreamingDiskCache.rememberTitle(media, movieId, JSONObject()
                        .put("name", name).put("overview", overview)
                        .put("date", released).put("poster", poster)
                        .put("backdrop", backdrop).put("genres", JSONArray(genreIds)))
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

    /** Confirmed BR subscription offers for exactly this TMDB title, not the installed app list. */
    suspend fun offersInBrazil(item: StreamingTitle): List<StreamingOffer> =
        withContext(Dispatchers.IO) {
            val providers = get("backend", "${item.media}/${item.id}/watch/providers")
                .optJSONObject("results")?.optJSONObject("BR")?.optJSONArray("flatrate")
                ?: return@withContext emptyList()
            (0 until providers.length()).mapNotNull { index ->
                val offer = providers.optJSONObject(index) ?: return@mapNotNull null
                val id = offer.optInt("provider_id")
                val name = offer.optString("provider_name").trim()
                if (id <= 0 || name.isBlank()) return@mapNotNull null
                val normalized = norm(name)
                // An Amazon/Apple add-on channel is not the base Paramount/Max subscription.
                if (normalized.contains("channel") || normalized.contains("canal") ||
                    normalized.contains("addon")) return@mapNotNull null
                StreamingOffer(id, name,
                    offer.optString("logo_path").takeIf { it.startsWith("/") } ?: "")
            }.distinctBy { provider(it.name) ?: norm(it.name) }
        }

    fun appHasOffer(app: TvOfficialApp, offers: List<StreamingOffer>): Boolean {
        val service = provider(app.name) ?: return false
        return offers.any { provider(it.name) == service }
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

    /** Discovery lists for real movie genres, localized using the upstream genre catalogue. */
    suspend fun discoveryGenre(id: Int): List<StreamingTitle> =
        discoveryGenre(id, "movie")
    suspend fun discoveryGenre(id: Int, media: String): List<StreamingTitle> =
        withContext(Dispatchers.IO) {
        require(media == "movie" || media == "tv")
        require(id in setOf(28, 18, 35, 53, 37, 878, 12, 16, 27, 10749, 99, 9648, 14,
            10759, 80, 10765, 10766, 10768, 10762))
        val route = "discover/$media?language=pt-BR&include_adult=false" +
            "&sort_by=popularity.desc&with_genres=$id&page=1"
        val payload = get("backend", route)
        mapTitles(payload.optJSONArray("results") ?: JSONArray(), media, id).take(12)
    }

    /** Real TMDB seasons and episodes; cached locally so opening the same series costs nothing. */
    suspend fun seasons(id: Int): List<StreamingSeason> = withContext(Dispatchers.IO) {
        val items = get("backend", "tv/$id?language=pt-BR").optJSONArray("seasons")
            ?: JSONArray()
        (0 until items.length()).mapNotNull { index ->
            val e = items.optJSONObject(index) ?: return@mapNotNull null
            val season = e.optInt("season_number", -1)
            val count = e.optInt("episode_count")
            if (season < 0 || count <= 0) null
            else StreamingSeason(season, e.optString("name").ifBlank {
                if (season == 0) "Especiais" else "Temporada $season"
            }, count)
        }
    }
    suspend fun episodes(id: Int, season: Int): List<StreamingEpisode> =
        withContext(Dispatchers.IO) {
            require(season in 0..999)
            val items = get("backend", "tv/$id/season/$season?language=pt-BR")
                .optJSONArray("episodes") ?: JSONArray()
            (0 until items.length()).mapNotNull { index ->
                val item = items.optJSONObject(index) ?: return@mapNotNull null
                val number = item.optInt("episode_number", 0)
                if (number <= 0) null else StreamingEpisode(
                    number, item.optString("name").ifBlank { "Episódio $number" },
                    item.optString("air_date"))
            }
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
        fun findTrailer(videos: JSONArray): String? =
            (0 until videos.length()).mapNotNull { index ->
                val video = videos.optJSONObject(index) ?: return@mapNotNull null
                val key = video.optString("key")
                if (video.optString("site") == "YouTube" &&
                    video.optString("type") == "Trailer" &&
                    key.matches(Regex("[A-Za-z0-9_-]{11}"))) key else null
            }.firstOrNull()
        val original = get("backend", "$media/$id/videos?language=pt-BR")
        val preferred = findTrailer(original.optJSONArray("results") ?: JSONArray())
        val key = preferred ?: runCatching {
            val alternative = get("backend", "$media/$id/videos?language=en-US")
            findTrailer(alternative.optJSONArray("results") ?: JSONArray())
        }.getOrNull()
        key?.let { "https://www.youtube.com/watch?v=" + it }
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

/**
 * Once an artwork has actually been displayed, keep its bytes on-device with no
 * time expiration. Unlike editorial lists this is intentionally user-clearable
 * only (Android "Clear app storage"/uninstall); never stores server credentials.
 */
@Composable
internal fun AnyTvPinnedImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val url = model.takeIf { it.startsWith("https://image.tmdb.org/t/p/") }
    val target = remember(url) {
        url?.let {
            val hash = MessageDigest.getInstance("SHA-256").digest(it.toByteArray())
                .joinToString("") { b -> "%02x".format(b) }
            File(File(context.applicationContext.filesDir, "anytv_artwork_saved").apply {
                mkdirs()
            }, hash + ".jpg")
        }
    }
    var saved by remember(target) { mutableStateOf(target?.takeIf { it.isFile }) }
    val saveScope = rememberCoroutineScope()
    AsyncImage(
        model = saved ?: model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onSuccess = { state ->
            if (saved == null && target != null && !target.exists()) {
                val drawable = state.result.drawable
                saveScope.launch(Dispatchers.IO) {
                    runCatching {
                        val bitmap = drawable.toBitmap()
                        val partial = File(target.parentFile, target.name + ".tmp")
                        partial.outputStream().use {
                            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it))
                        }
                        if (!target.exists()) partial.renameTo(target) else partial.delete()
                        if (target.isFile) withContext(Dispatchers.Main) { saved = target }
                    }
                }
            }
        }
    )
}

/** Short interactive carousel: name and entry affordance left, official artwork right. */
@Composable
internal fun AnyTvHomeFilmBanner(onOpenStreaming: () -> Unit) {
    val context = LocalContext.current
    var previews by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    LaunchedEffect(Unit) {
        StreamingCatalog.configure(context)
        // discoveryShelf appends language itself: never pass a duplicate query.
        previews = runCatching {
            StreamingCatalog.discoveryShelf("trending/movie/week", "movie")
        }.getOrDefault(emptyList())
            .filter { it.backdrop.isNotBlank() || it.poster.isNotBlank() }.take(12)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().height(75.dp)
            .clickable(onClick = onOpenStreaming)
            .semantics { contentDescription = "Abrir destaques de filmes e séries" },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        if (previews.isEmpty()) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Destaques de filmes e séries", modifier = Modifier.weight(1f),
                    maxLines = 1, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Ver ›", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
        } else {
            val pager = rememberPagerState(pageCount = { previews.size })
            LaunchedEffect(pager, previews.map { it.id }) {
                while (previews.size > 1) {
                    delay(3000)
                    if (!pager.isScrollInProgress)
                        pager.animateScrollToPage((pager.currentPage + 1) % previews.size)
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true) { index ->
                val movie = previews[index]
                Row(modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)
                        .padding(start = 12.dp, end = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("EM DESTAQUE", fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                        Text(movie.name, fontSize = 12.sp, lineHeight = 14.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold)
                        Text("Ver ›", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    AnyTvPinnedImage(
                        model = "https://image.tmdb.org/t/p/w500" +
                            movie.backdrop.ifBlank { movie.poster },
                        contentDescription = "Destaque: " + movie.name,
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(0.48f)
                            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/** Compact, single-line score and verified Brazilian subscription platform marks. */
@Composable
private fun StreamingOfferIcons(
    item: StreamingTitle,
    known: MutableMap<String, List<StreamingOffer>>,
    apps: List<TvOfficialApp>,
    expanded: Boolean = false
) {
    val key = "${item.media}:${item.id}"
    LaunchedEffect(key) {
        if (key !in known) {
            runCatching { StreamingCatalog.offersInBrazil(item) }
                .getOrNull()?.let { known[key] = it }
        }
    }
    val offerings = known[key].orEmpty()
    if (item.votes > 0 || offerings.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 20.dp)) {
            if (item.votes > 0) {
                Text("★ " + String.format(java.util.Locale.US, "%.1f", item.rating),
                    fontSize = 11.sp, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val visible = if (expanded) 4 else 2
            offerings.take(visible).forEach { offer ->
                val installed = apps.firstOrNull {
                    StreamingCatalog.appHasOffer(it, listOf(offer))
                }
                if (installed != null) {
                    // Exactly the same launcher icon component used on the main TV screen.
                    OfficialTvIcon(installed, 20.dp)
                } else {
                    Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center) {
                        if (offer.logo.isNotBlank()) {
                            AnyTvPinnedImage(model = "https://image.tmdb.org/t/p/w92" + offer.logo,
                                contentDescription = "Disponível em " + offer.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit)
                        } else {
                            Text(offer.name.take(2), fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }
            if (offerings.size > visible) {
                Text("+${offerings.size - visible}", fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

/** Same one-row navigation across catalogue and individual provider views. */
@Composable
private fun AnyTvStreamNavBar(
    library: Boolean,
    section: String,
    contentType: String,
    filtersActive: Boolean,
    onHome: () -> Unit,
    onList: () -> Unit,
    onWatching: () -> Unit,
    onWatched: () -> Unit,
    onFilters: () -> Unit,
    onContentType: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            IconButton(onClick = onHome, modifier = Modifier.size(35.dp)) {
                Icon(Icons.Filled.Home, contentDescription = "Início: filmes e séries",
                    modifier = Modifier.size(21.dp),
                    tint = if (!library && contentType == "all") primary else neutral)
            }
            IconButton(onClick = onList, modifier = Modifier.size(35.dp)) {
                Icon(Icons.Filled.Favorite, contentDescription = "Minha lista",
                    modifier = Modifier.size(21.dp),
                    tint = if (library && section == "saved") primary else neutral)
            }
            IconButton(onClick = onWatching, modifier = Modifier.size(35.dp)) {
                Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Em andamento",
                    modifier = Modifier.size(21.dp),
                    tint = if (library && section == "watching") primary else neutral)
            }
            IconButton(onClick = onWatched, modifier = Modifier.size(35.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Assistidos",
                    modifier = Modifier.size(21.dp),
                    tint = if (library && section == "watched") primary else neutral)
            }
            IconButton(onClick = onFilters, modifier = Modifier.size(35.dp)) {
                Icon(Icons.Filled.Tv, contentDescription = "Filtrar por streamings e gêneros",
                    modifier = Modifier.size(21.dp),
                    tint = if (filtersActive) primary else neutral)
            }
            Spacer(Modifier.width(3.dp))
            FilterChip(selected = !library && contentType == "movie",
                onClick = {
                    onContentType(if (contentType == "movie") "all" else "movie")
                }, label = { Text("Filmes", fontSize = 11.sp) },
                modifier = Modifier.height(33.dp))
            FilterChip(selected = !library && contentType == "tv",
                onClick = {
                    onContentType(if (contentType == "tv") "all" else "tv")
                }, label = { Text("Séries", fontSize = 11.sp) },
                modifier = Modifier.height(33.dp))
        }
        IconButton(onClick = onSearch, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Filled.Search, contentDescription = "Buscar filmes e séries",
                modifier = Modifier.size(21.dp))
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Fechar streaming",
                modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
internal fun TclStreamingScreens(
    apps: List<TvOfficialApp>, host: String, pairing: String,
    tvName: String, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    StreamingCatalog.configure(context)
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
    val mainGenreIds = remember { listOf(28, 18, 35, 53, 37, 878, 12, 16, 27, 10749, 99, 9648, 14) }
    var mainGenres by remember { mutableStateOf<List<StreamingGenre>>(emptyList()) }
    var allMovieGenres by remember { mutableStateOf<List<StreamingGenre>>(emptyList()) }
    var tvGenres by remember { mutableStateOf<List<StreamingGenre>>(emptyList()) }
    var homeContentType by remember { mutableStateOf("all") }
    var librarySection by remember { mutableStateOf("saved") }
    var selectedHomeGenre by remember { mutableIntStateOf(0) }
    var showGenreSettings by remember { mutableStateOf(false) }
    var showMyStreamingSettings by remember { mutableStateOf(false) }
    var onlyMyStreaming by remember { mutableStateOf(false) }
    val storedMyServices = remember(prefs) {
        prefs.getString("anytv_my_streamings", "")?.split(",")
            ?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }
    var myServices by remember { mutableStateOf(storedMyServices) }
    var editingMyServices by remember { mutableStateOf(storedMyServices) }
    val myServiceShelves = remember { mutableStateMapOf<String, List<StreamingTitle>>() }
    var homeLibrary by remember { mutableStateOf(false) }
    var trackedSeries by remember {
        mutableStateOf(prefs.getString("anytv_tracking_series", "")
            ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
    }
    fun toggleSeriesTracking(id: Int) {
        trackedSeries = if (id.toString() in trackedSeries)
            trackedSeries - id.toString() else trackedSeries + id.toString()
        prefs.edit().putString("anytv_tracking_series",
            trackedSeries.sorted().joinToString(",")).apply()
    }
    var showGlobalSearch by remember { mutableStateOf(false) }
    val initialGenreIds = remember(prefs) {
        prefs.getString("anytv_home_genres", null)?.split(",")
            ?.mapNotNull { it.toIntOrNull() }?.filter { it in mainGenreIds }?.toSet()
            ?: setOf(28, 18, 35, 53, 37)
    }
    var preferredGenreIds by remember { mutableStateOf(initialGenreIds) }
    var editingGenreIds by remember { mutableStateOf(initialGenreIds) }
    val genreShelves = remember { mutableStateMapOf<String, List<StreamingTitle>>() }
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
                        o.optString("poster"), kind, date = o.optString("date"),
                        genreIds = o.optJSONArray("genres")?.let { arr ->
                            (0 until arr.length()).map { arr.optInt(it) }.filter { it > 0 }
                        } ?: emptyList()), pkg)
            }
        }.getOrDefault(emptyList<StreamingFavorite>()))
    }
    // Watched history survives removing a heart: it is independent of favorites.
    fun restoreTrackedTitle(media: String, id: Int): StreamingTitle? {
        favorites.firstOrNull { it.title.id == id && it.title.media == media }
            ?.title?.let { return it }
        val cached = StreamingDiskCache.previousTitle(media, id) ?: return null
        val titleName = cached.optString("name")
        if (titleName.isBlank()) return null
        val genreArr = cached.optJSONArray("genres")
        return StreamingTitle(id, titleName, cached.optString("overview"),
            cached.optString("poster"), media,
            backdrop = cached.optString("backdrop"), date = cached.optString("date"),
            genreIds = (0 until (genreArr?.length() ?: 0))
                .map { genreArr!!.optInt(it) }.filter { it > 0 })
    }

    fun isFavorite(title: StreamingTitle, packageName: String): Boolean =
        favorites.any { it.title.id == title.id && it.title.media == title.media }
    fun toggleFavorite(title: StreamingTitle, packageName: String) {
        favorites = if (isFavorite(title, packageName)) favorites.filterNot {
            it.title.id == title.id && it.title.media == title.media
        } else listOf(StreamingFavorite(title,
            packageName.ifBlank { "library" })) + favorites
        val json = JSONArray()
        favorites.forEach { value ->
            json.put(JSONObject().put("id", value.title.id)
                .put("media", value.title.media)
                .put("package", value.packageName)
                .put("name", value.title.name)
                .put("overview", value.title.overview)
                .put("poster", value.title.poster)
                .put("date", value.title.date)
                .put("genres", JSONArray(value.title.genreIds)))
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
    val offerCache = remember { mutableStateMapOf<String, List<StreamingOffer>>() }
    var detailsOffers by remember { mutableStateOf<List<StreamingOffer>?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var seriesSeasons by remember { mutableStateOf<List<StreamingSeason>>(emptyList()) }
    var selectedSeriesSeason by remember { mutableIntStateOf(1) }
    var seasonEpisodes by remember { mutableStateOf<List<StreamingEpisode>>(emptyList()) }
    var episodeError by remember { mutableStateOf<String?>(null) }
    var manualSeason by remember { mutableStateOf("1") }
    var manualEpisode by remember { mutableStateOf("1") }
    var watchedFilms by remember {
        mutableStateOf(prefs.getString("anytv_watched_movies", "")
            ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
    }
    fun toggleFilmWatched(id: Int) {
        val key = id.toString()
        watchedFilms = if (key in watchedFilms) watchedFilms - key else watchedFilms + key
        prefs.edit().putString("anytv_watched_movies",
            watchedFilms.sorted().joinToString(",")).apply()
    }
    var watchedEpisodes by remember {
        mutableStateOf(prefs.getString("anytv_watched_episodes", "")
            ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
    }
    fun toggleWatched(id: Int, season: Int, episode: Int) {
        val key = "$id:$season:$episode"
        watchedEpisodes = if (key in watchedEpisodes) watchedEpisodes - key
            else watchedEpisodes + key
        prefs.edit().putString("anytv_watched_episodes",
            watchedEpisodes.sorted().joinToString(",")).apply()
    }
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
        detailsOffers = null
        seriesSeasons = emptyList()
        seasonEpisodes = emptyList()
        episodeError = null
        if (item != null) {
            if (item.media == "tv") {
                seriesSeasons = runCatching { StreamingCatalog.seasons(item.id) }
                    .getOrDefault(emptyList())
                selectedSeriesSeason = seriesSeasons.firstOrNull { it.number == 1 }?.number
                    ?: seriesSeasons.firstOrNull()?.number ?: 1
            }
            detailLoading = true
            trailerLink = runCatching { StreamingCatalog.trailer(item.media, item.id) }.getOrNull()
            relatedTitles = runCatching { StreamingCatalog.related(item.media, item.id) }
                .getOrDefault(emptyList())
            val key = "${item.media}:${item.id}"
            detailsOffers = offerCache[key] ?:
                runCatching { StreamingCatalog.offersInBrazil(item) }.getOrNull()
            detailsOffers?.let { offerCache[key] = it }
            detailLoading = false
        }
    }

    LaunchedEffect(details?.id, details?.media, selectedSeriesSeason, seriesSeasons) {
        val show = details
        seasonEpisodes = emptyList()
        if (show?.media == "tv" && seriesSeasons.any { it.number == selectedSeriesSeason }) {
            episodeError = null
            seasonEpisodes = runCatching {
                StreamingCatalog.episodes(show.id, selectedSeriesSeason)
            }.getOrElse {
                episodeError = "Não foi possível consultar episódios; tente novamente mais tarde."
                emptyList()
            }
        }
    }

    // Genre identifiers and display names come from the real PT-BR TMDB catalogue.
    LaunchedEffect(token) {
        allMovieGenres = runCatching { StreamingCatalog.genres(token, "movie") }
            .getOrDefault(emptyList())
        mainGenres = allMovieGenres.filter { it.id in mainGenreIds }
            .sortedBy { mainGenreIds.indexOf(it.id) }
        tvGenres = runCatching { StreamingCatalog.genres(token, "tv") }
            .getOrDefault(emptyList())
    }
    // Fetch each genre shelf only once per screen, preserving user-selected order.
    LaunchedEffect(mainGenres, tvGenres, preferredGenreIds, selectedHomeGenre, homeContentType) {
        val mediaKind = if (homeContentType == "tv") "tv" else "movie"
        val choices = if (mediaKind == "tv") tvGenres else mainGenres
        val requested = if (selectedHomeGenre == 0) {
            choices.filter { it.id in preferredGenreIds ||
                mainGenres.any { movie -> movie.id in preferredGenreIds && movie.name == it.name } }
                .map { it.id }
        } else listOf(selectedHomeGenre)
        for (id in requested) {
            val key = "$mediaKind:$id"
            if (key !in genreShelves) {
                runCatching { StreamingCatalog.discoveryGenre(id, mediaKind) }
                    .onSuccess { genreShelves[key] = it }
            }
        }
    }

    LaunchedEffect(myServices, onlyMyStreaming, selectedHomeGenre, apps) {
        val selection = displayed.filter { it.packageName in myServices &&
            StreamingCatalog.streaming(it) }.take(6)
        if (selection.isNotEmpty()) {
            for (app in selection) {
                val kinds = if (selectedHomeGenre > 0) listOf("movie")
                    else listOf("movie", "tv")
                for (kind in kinds) {
                    val key = app.packageName + ":" + kind + ":" + selectedHomeGenre
                    if (key !in myServiceShelves) {
                        runCatching {
                            StreamingCatalog.discover(token, app.name, kind, 1,
                                if (kind == "movie") selectedHomeGenre else 0, "")
                        }.onSuccess { myServiceShelves[key] = it.titles.take(12) }
                    }
                }
            }
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
                AnyTvStreamNavBar(
                    library = homeLibrary && selected == null,
                    section = librarySection,
                    contentType = homeContentType,
                    filtersActive = onlyMyStreaming || selectedHomeGenre != 0,
                    onHome = { selected = null; homeLibrary = false
                        homeContentType = "all"; selectedHomeGenre = 0 },
                    onList = { selected = null; homeLibrary = true; librarySection = "saved" },
                    onWatching = { selected = null; homeLibrary = true; librarySection = "watching" },
                    onWatched = { selected = null; homeLibrary = true; librarySection = "watched" },
                    onFilters = { editingMyServices = myServices
                        showMyStreamingSettings = true },
                    onContentType = { kind -> selected = null; homeLibrary = false
                        homeContentType = kind; selectedHomeGenre = 0 },
                    onSearch = { selected = null; showGlobalSearch = !showGlobalSearch },
                    onDismiss = onDismiss
                )
                if (selected == null) {
                    // The same real TV platform shortcuts, compact rather than a large list.
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        displayed.filter { StreamingCatalog.streaming(it) }.forEach { app ->
                            Column(modifier = Modifier.width(62.dp).clickable {
                                selected = app; media = "movie"; genre = 0; page = 1
                                searchDraft = ""; searchTerm = ""; showSaved = false; message = null
                            }.semantics { contentDescription = "Explorar " + app.name },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                OfficialTvIcon(app, 56.dp)
                                Text(app.name, fontSize = 10.sp, maxLines = 1,
                                    lineHeight = 11.sp, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (showGlobalSearch) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            OutlinedTextField(
                                value = globalSearchDraft,
                                onValueChange = { globalSearchDraft = it.take(90) },
                                placeholder = { Text("Filme ou série", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(52.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    globalSearchTerm = globalSearchDraft.trim()
                                }))
                            TextButton(onClick = { globalSearchTerm = globalSearchDraft.trim() },
                                enabled = globalSearchDraft.trim().length >= 2,
                                contentPadding = PaddingValues(horizontal = 4.dp)) {
                                Text("Buscar", fontSize = 12.sp)
                            }
                            IconButton(onClick = {
                                globalSearchDraft = ""; globalSearchTerm = ""
                                showGlobalSearch = false
                            }, modifier = Modifier.size(32.dp)) { Text("×", fontSize = 18.sp) }
                        }
                        if (globalSearchBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                        if (globalSearchTerm.isNotBlank() && !globalSearchBusy) {
                            Text("Resultados: " + globalSearchTerm, fontSize = 11.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 195.dp)) {
                                items(globalSearchResults) { found ->
                                    Column(modifier = Modifier.width(108.dp).clickable {
                                        details = found; detailsPackage = null
                                    }) {
                                        AnyTvPinnedImage(model = "https://image.tmdb.org/t/p/w185" + found.poster,
                                            contentDescription = found.name,
                                            modifier = Modifier.fillMaxWidth().height(138.dp),
                                            contentScale = ContentScale.Crop)
                                        Spacer(Modifier.height(5.dp))
                                        Text(found.name, fontSize = 11.sp, maxLines = 2,
                                            lineHeight = 14.sp)
                                        StreamingOfferIcons(found, offerCache, displayed)
                                    }
                                }
                            }
                            if (globalSearchResults.isEmpty()) Text("Nenhum resultado encontrado.")
                        }
                    }
                    // One compact horizontal line: content type FIRST, then genres.
                    val selectedGenreKind = if (homeContentType == "tv") "tv" else "movie"
                    val currentGenreList = if (selectedGenreKind == "tv") tvGenres else mainGenres
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        // Content type is selected in the compact top navigation.
                        FilterChip(selected = selectedHomeGenre == 0,
                            onClick = { selectedHomeGenre = 0; homeLibrary = false },
                            label = { Text("Todos", fontSize = 11.sp) })
                        currentGenreList.forEach { kind ->
                            FilterChip(selected = selectedHomeGenre == kind.id,
                                onClick = {
                                    selectedHomeGenre = kind.id; homeLibrary = false
                                }, label = { Text(kind.name, fontSize = 11.sp) })
                        }
                        TextButton(onClick = {
                            editingGenreIds = preferredGenreIds; showGenreSettings = true
                        }, contentPadding = PaddingValues(horizontal = 5.dp)) {
                            Text("⚙ Listas", fontSize = 11.sp)
                        }
                    }
                    if (discoveryLoading || (selectedHomeGenre != 0 &&
                            "$selectedGenreKind:$selectedHomeGenre" !in genreShelves)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    }
                    if (discoveryError != null && selectedHomeGenre == 0) {
                        Text(discoveryError.orEmpty(), fontSize = 11.sp)
                    }
                    val visibleShelves = linkedMapOf<String, List<StreamingTitle>>().apply {
                        if (homeLibrary) {
                            val films = favorites.filter { it.title.media == "movie" }
                                .map { it.title }.distinctBy { it.id }
                            val series = favorites.filter { it.title.media == "tv" }
                                .map { it.title }.distinctBy { it.id }
                            val unseen = films.filter { it.id.toString() !in watchedFilms }
                            val history = watchedFilms.mapNotNull { key ->
                                key.toIntOrNull()?.let { restoreTrackedTitle("movie", it) }
                            }.distinctBy { it.id }
                            val trackedIds = trackedSeries +
                                watchedEpisodes.mapNotNull { it.substringBefore(":").takeIf(String::isNotBlank) }
                            val watching = trackedIds.mapNotNull { key ->
                                key.toIntOrNull()?.let { restoreTrackedTitle("tv", it) }
                            }.distinctBy { it.id }
                            if (librarySection == "saved") {
                                if (unseen.isNotEmpty()) put("Filmes para assistir", unseen)
                                if (series.isNotEmpty()) put("Minhas séries", series)
                            } else if (librarySection == "watching") {
                                if (watching.isNotEmpty()) put("Séries em andamento", watching)
                            } else if (librarySection == "watched") {
                                if (history.isNotEmpty()) put("Filmes já assistidos", history)
                            }
                        } else if (!onlyMyStreaming) {
                            if (selectedHomeGenre == 0) {
                                discoveryShelves.forEach { (heading, titles) ->
                                    val selectedTitles = titles.filter { homeContentType == "all" ||
                                        it.media == homeContentType }
                                    if (selectedTitles.isNotEmpty()) put(heading, selectedTitles)
                                }
                                currentGenreList.filter { it.id in preferredGenreIds ||
                                    mainGenres.any { m -> m.id in preferredGenreIds && m.name == it.name } }
                                    .forEach { kind ->
                                        genreShelves["$selectedGenreKind:${kind.id}"]
                                            ?.takeIf { it.isNotEmpty() }?.let {
                                                put("Por gênero: " + kind.name, it)
                                            }
                                    }
                            } else {
                                val kind = currentGenreList.firstOrNull { it.id == selectedHomeGenre }
                                genreShelves["$selectedGenreKind:$selectedHomeGenre"]
                                    ?.takeIf { it.isNotEmpty() }?.let {
                                        put("Em destaque: " + (kind?.name ?: "Gênero"), it)
                                    }
                            }
                        }
                        if (!homeLibrary) {
                            displayed.filter { it.packageName in myServices &&
                                StreamingCatalog.streaming(it) }.take(6).forEach { app ->
                                val kinds = if (selectedHomeGenre == 0)
                                    listOf("movie", "tv").filter {
                                        homeContentType == "all" || it == homeContentType
                                    } else listOf(selectedGenreKind)
                                kinds.forEach { kind ->
                                    val key = app.packageName + ":" + kind + ":" + selectedHomeGenre
                                    myServiceShelves[key]?.takeIf { it.isNotEmpty() }?.let {
                                        put("Destaques " + app.name +
                                            if (kind == "tv") " · Séries" else " · Filmes", it)
                                    }
                                }
                            }
                        }
                    }
                    if (selectedHomeGenre != 0 && "$selectedGenreKind:$selectedHomeGenre" in genreShelves &&
                        visibleShelves.isEmpty()) {
                        Text("Não encontramos títulos neste gênero no momento.", fontSize = 12.sp)
                    }
                    val heroTitles = if (homeLibrary) emptyList()
                        else visibleShelves.values.firstOrNull().orEmpty()
                            .filterNot { it.media == "movie" &&
                                it.id.toString() in watchedFilms }.take(6)
                    val seenInHome = heroTitles.map { "${it.media}:${it.id}" }.toMutableSet()
                    val uniqueShelves = visibleShelves.mapNotNull { (heading, shelf) ->
                        val unique = shelf.filter {
                            (homeLibrary || it.media != "movie" ||
                                it.id.toString() !in watchedFilms) &&
                                seenInHome.add("${it.media}:${it.id}")
                        }
                        if (unique.isEmpty()) null else heading to unique
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (heroTitles.isNotEmpty()) {
                            item {
                                // The slide is PART of the scrolling content, not a fixed header.
                                val pagerState = rememberPagerState(pageCount = { heroTitles.size })
                                LaunchedEffect(pagerState, heroTitles.map { it.id to it.media }, details?.id) {
                                    while (details == null && heroTitles.size > 1) {
                                        delay(3000)
                                        if (!pagerState.isScrollInProgress) {
                                            pagerState.animateScrollToPage(
                                                (pagerState.currentPage + 1) % heroTitles.size)
                                        }
                                    }
                                }
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth()
                                ) { pageIndex ->
                                    val current = heroTitles[pageIndex]
                                    Column(modifier = Modifier.fillMaxWidth().clickable {
                                        details = current
                                        detailsPackage = null
                                    }) {
                                        AnyTvPinnedImage(model = "https://image.tmdb.org/t/p/w780" +
                                            current.backdrop.ifBlank { current.poster },
                                            contentDescription = "Destaque: " + current.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth().height(150.dp))
                                        Spacer(Modifier.height(5.dp))
                                        Text(current.name, fontWeight = FontWeight.Bold,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        StreamingOfferIcons(current, offerCache, displayed, expanded = true)
                                    }
                                }
                                // Small noninteractive position dots: no previous/next buttons.
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                                    horizontalArrangement = Arrangement.Center) {
                                    repeat(heroTitles.size) { dot ->
                                        Text(if (pagerState.currentPage == dot) "●" else "○",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        uniqueShelves.forEach { (heading, shelf) ->
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(heading, fontWeight = FontWeight.SemiBold)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        items(shelf.take(12)) { item ->
                                            Column(modifier = Modifier.width(116.dp).clickable {
                                                details = item
                                                detailsPackage = null
                                            }) {
                                                AnyTvPinnedImage(model = "https://image.tmdb.org/t/p/w185" + item.poster,
                                                    contentDescription = "Cartaz: " + item.name,
                                                    modifier = Modifier.fillMaxWidth().height(158.dp),
                                                    contentScale = ContentScale.Crop)
                                                Spacer(Modifier.height(5.dp))
                                                Text(item.name, maxLines = 2, lineHeight = 14.sp, fontSize = 11.sp)
                                                StreamingOfferIcons(item, offerCache, displayed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val app = selected!!
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f)
                            .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            IconButton(onClick = { selected = null; details = null },
                                modifier = Modifier.size(34.dp).semantics {
                                    contentDescription = "Voltar ao catálogo"
                                }) { Text("‹", fontSize = 23.sp) }
                            OfficialTvIcon(app, 32.dp)
                            Text(app.name, fontSize = 12.sp, maxLines = 1,
                                fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = {
                                selected = null; homeLibrary = true
                                librarySection = "saved"
                            }) { Text("♥ Minha lista", fontSize = 11.sp) }
                            TextButton(onClick = {
                                selected = null; homeLibrary = true
                                librarySection = "watching"
                            }) { Text("Em andamento", fontSize = 11.sp) }
                            TextButton(onClick = {
                                selected = null; homeLibrary = true
                                librarySection = "watched"
                            }) { Text("✓ Assistidos", fontSize = 11.sp) }
                            TextButton(onClick = {
                                selected = null
                                editingMyServices = myServices
                                showMyStreamingSettings = true
                            }) { Text("Meus streamings", fontSize = 11.sp) }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)
                            .semantics { contentDescription = "Fechar streaming" }) {
                            Text("×", fontSize = 24.sp)
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
                                        AnyTvPinnedImage(
                                            model = "https://image.tmdb.org/t/p/w185" + title.poster,
                                            contentDescription = "Cartaz: " + title.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                        )
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    Text(title.name, fontSize = 11.sp, maxLines = 2, lineHeight = 14.sp,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis)
                                    StreamingOfferIcons(title, offerCache, displayed)
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
                                        AnyTvPinnedImage(
                                            model = "https://image.tmdb.org/t/p/w185" + title.poster,
                                            contentDescription = "Cartaz: " + title.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(63.dp, 95.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title.name, fontWeight = FontWeight.SemiBold,
                                            maxLines = 2, lineHeight = 17.sp,
                                            overflow = TextOverflow.Ellipsis)
                                        StreamingOfferIcons(title, offerCache, displayed)
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
    if (showMyStreamingSettings) {
        AlertDialog(
            onDismissRequest = { showMyStreamingSettings = false },
            title = { Text("Meus streamings") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())) {
                    Text("Selecione os serviços que você assina. Ter o aplicativo na TCL não significa ter assinatura.",
                        fontSize = 12.sp)
                    displayed.filter { StreamingCatalog.streaming(it) }.forEach { app ->
                        val selectedHere = app.packageName in editingMyServices
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            editingMyServices = if (selectedHere)
                                editingMyServices - app.packageName
                            else editingMyServices + app.packageName
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selectedHere, onCheckedChange = null)
                            OfficialTvIcon(app, 32.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(app.name, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    myServices = editingMyServices
                    prefs.edit().putString("anytv_my_streamings",
                        editingMyServices.sorted().joinToString(",")).apply()
                    onlyMyStreaming = myServices.isNotEmpty()
                    homeLibrary = false
                    showMyStreamingSettings = false
                }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showMyStreamingSettings = false }) { Text("Cancelar") }
            }
        )
    }
    if (showGenreSettings) {
        AlertDialog(
            onDismissRequest = { showGenreSettings = false },
            title = { Text("Listas na página inicial", fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Escolha os gêneros que aparecem na página Descobrir.",
                        fontSize = 12.sp)
                    mainGenres.forEach { kind ->
                        val checked = kind.id in editingGenreIds
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            editingGenreIds = if (checked)
                                editingGenreIds - kind.id else editingGenreIds + kind.id
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(kind.name, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editingGenreIds.isNotEmpty(),
                    onClick = {
                        preferredGenreIds = editingGenreIds
                        prefs.edit().putString("anytv_home_genres",
                            mainGenreIds.filter { it in editingGenreIds }.joinToString(",")).apply()
                        selectedHomeGenre = 0
                        showGenreSettings = false
                    }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showGenreSettings = false }) { Text("Cancelar") }
            }
        )
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
            title = {
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title.name, modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                        maxLines = 2, lineHeight = 20.sp)
                    IconButton(onClick = {
                        toggleFavorite(title, pkg)
                    }, modifier = Modifier.size(36.dp).semantics {
                        contentDescription = if (isFavorite(title, pkg))
                            "Remover da minha lista" else "Adicionar à minha lista"
                    }) {
                        Text(if (isFavorite(title, pkg)) "♥" else "♡",
                            color = MaterialTheme.colorScheme.primary, fontSize = 26.sp)
                    }
                    IconButton(onClick = {
                        if (title.media == "movie") {
                            if (!isFavorite(title, pkg)) toggleFavorite(title, pkg)
                            toggleFilmWatched(title.id)
                        } else {
                            if (!isFavorite(title, pkg)) toggleFavorite(title, pkg)
                            toggleSeriesTracking(title.id)
                        }
                    }, modifier = Modifier.size(36.dp).semantics {
                        contentDescription = if (title.media == "movie")
                            "Marcar como assistido" else "Acompanhar série"
                    }) {
                        val marked = if (title.media == "movie")
                            title.id.toString() in watchedFilms
                        else title.id.toString() in trackedSeries
                        Text(if (marked) "✓" else "○",
                            color = if (marked) Color(0xFF1CB96B)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (titleActionFeedback != null) {
                        Text(titleActionFeedback.orEmpty(), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val releasedYear = title.date.take(4).takeIf {
                        it.length == 4 && it.all(Char::isDigit)
                    }
                    val knownGenreNames = if (title.media == "tv") tvGenres else allMovieGenres
                    val titleGenres = title.genreIds.mapNotNull { id ->
                        knownGenreNames.firstOrNull { it.id == id }?.name
                    }.take(3)
                    Text(listOfNotNull(if (title.media == "tv") "Série" else "Filme",
                        releasedYear, titleGenres.takeIf { it.isNotEmpty() }?.joinToString(" · "))
                        .joinToString(" · "), fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
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
                    if (title.media == "tv") {
                        Text("Acompanhar episódios", fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp)
                        if (seriesSeasons.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                seriesSeasons.forEach { season ->
                                    FilterChip(
                                        selected = selectedSeriesSeason == season.number,
                                        onClick = { selectedSeriesSeason = season.number },
                                        label = { Text(season.name, fontSize = 11.sp) })
                                }
                            }
                            val watchedThisSeason = seasonEpisodes.count {
                                "${title.id}:${selectedSeriesSeason}:${it.number}" in watchedEpisodes
                            }
                            Text("$watchedThisSeason de ${seasonEpisodes.size} episódios assistidos",
                                fontSize = 12.sp)
                            seasonEpisodes.forEach { episode ->
                                val marked = "${title.id}:${selectedSeriesSeason}:${episode.number}" in watchedEpisodes
                                Row(modifier = Modifier.fillMaxWidth().clickable {
                                    toggleWatched(title.id, selectedSeriesSeason, episode.number)
                                }, verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Checkbox(checked = marked, onCheckedChange = {
                                        toggleWatched(title.id, selectedSeriesSeason, episode.number)
                                    })
                                    Column {
                                        Text("${episode.number}. ${episode.name}",
                                            fontSize = 12.sp, lineHeight = 15.sp)
                                        if (episode.airDate.length >= 10) {
                                            Text(episode.airDate, fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            if (episodeError != null) {
                                Text(episodeError.orEmpty(), fontSize = 11.sp)
                            }
                        } else if (!detailLoading) {
                            Text("Registro manual de episódios", fontWeight = FontWeight.Medium,
                                fontSize = 12.sp)
                            Text("Os dados reais de temporadas ainda não estão disponíveis. " +
                                "Registre o progresso sem informar nomes ou contagens fictícias.",
                                fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                OutlinedTextField(value = manualSeason,
                                    onValueChange = {
                                        manualSeason = it.filter(Char::isDigit).take(3)
                                    }, label = { Text("Temporada") }, singleLine = true,
                                    modifier = Modifier.weight(1f))
                                OutlinedTextField(value = manualEpisode,
                                    onValueChange = {
                                        manualEpisode = it.filter(Char::isDigit).take(3)
                                    }, label = { Text("Episódio") }, singleLine = true,
                                    modifier = Modifier.weight(1f))
                            }
                            val seasonNum = manualSeason.toIntOrNull()
                            val epNum = manualEpisode.toIntOrNull()
                            TextButton(onClick = {
                                if (seasonNum != null && epNum != null)
                                    toggleWatched(title.id, seasonNum, epNum)
                            }, enabled = seasonNum != null && seasonNum in 1..999 &&
                                epNum != null && epNum in 1..999) {
                                Text("Marcar/desmarcar episódio")
                            }
                            watchedEpisodes.filter { it.startsWith("${title.id}:") }
                                .mapNotNull { raw ->
                                    val pieces = raw.split(":")
                                    if (pieces.size == 3) {
                                        val nSeason = pieces[1].toIntOrNull()
                                        val nEpisode = pieces[2].toIntOrNull()
                                        if (nSeason != null && nEpisode != null)
                                            nSeason to nEpisode else null
                                    } else null
                                }.sortedWith(compareBy({ it.first }, { it.second }))
                                .forEach { (sn, en) ->
                                    TextButton(onClick = { toggleWatched(title.id, sn, en) }) {
                                        Text("✓ T$sn · E$en   ×", fontSize = 11.sp)
                                    }
                                }
                        }
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
                                    AnyTvPinnedImage(model = "https://image.tmdb.org/t/p/w185" + suggested.poster,
                                        contentDescription = suggested.name,
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        contentScale = ContentScale.Crop)
                                    Text(suggested.name, fontSize = 11.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                    Text("Onde assistir por assinatura no Brasil",
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    when {
                        detailsOffers == null -> Text("Disponibilidade não confirmada. Verifique a conexão.",
                            fontSize = 11.sp)
                        detailsOffers!!.isEmpty() -> Text("Nenhuma oferta de assinatura confirmada no Brasil.",
                            fontSize = 11.sp)
                        else -> Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            detailsOffers!!.forEach { offer ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.widthIn(min = 54.dp, max = 85.dp)) {
                                    if (offer.logo.isNotEmpty()) {
                                        AnyTvPinnedImage(model = "https://image.tmdb.org/t/p/w92" + offer.logo,
                                            contentDescription = offer.name,
                                            modifier = Modifier.size(32.dp))
                                    }
                                    Text(offer.name, fontSize = 10.sp, maxLines = 2,
                                        textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    if (selected == null) {
                        val eligible = displayed.filter { app ->
                            detailsOffers?.let { StreamingCatalog.appHasOffer(app, it) } == true
                        }
                        if (eligible.isNotEmpty()) {
                            Text("Abrir em aplicativo instalado na TV:", fontSize = 12.sp)
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                eligible.forEach { app ->
                                    FilterChip(
                                        selected = detailsPackage == app.packageName,
                                        onClick = { detailsPackage = app.packageName },
                                        label = { Text(app.name, maxLines = 1, fontSize = 11.sp) })
                                }
                            }
                        } else {
                            Text("Nenhum aplicativo instalado na TCL foi confirmado para este título.",
                                fontSize = 11.sp)
                        }
                    }
                    Text("A TV poderá abrir a busca do título se este serviço aceitar " +
                        "pesquisa externa. Não existe deep link universal de reprodução.",
                        fontSize = 12.sp)
                    // Availability is already displayed above; avoid a confusing
                    // external website link claiming direct playback.
                    Text("Ofertas informadas pelo TMDB/JustWatch; confirme no streaming.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pkg.isBlank()) {
                        titleActionFeedback = "Selecione um serviço disponível para este título."
                    } else if (detailsOffers?.let { offers ->
                            displayed.any { app -> app.packageName == pkg &&
                                StreamingCatalog.appHasOffer(app, offers) } } != true) {
                        titleActionFeedback = "Oferta de assinatura no Brasil não confirmada para este aplicativo."
                    } else {
                        searchOnTv(title, pkg)
                    }
                }, enabled = !titleActionPending) {
                    Text(if (titleActionPending) "Abrindo…" else
                        "Abrir na " + tvName.ifBlank { "TV conectada" })
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    details = null; detailsPackage = null; titleActionFeedback = null
                }) {
                    Text("Voltar")
                }
            }
        )
    }
}
