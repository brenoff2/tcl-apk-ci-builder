package com.hari.androidtvremote.ui.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.GridView

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.hari.androidtvremote.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
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

internal data class StreamingTitle(val id: Int, val name: String, val overview: String, val poster: String, val media: String, val rating: Double = 0.0, val votes: Int = 0, val backdrop: String = "", val date: String = "", val genreIds: List<Int> = emptyList(), val originalLanguage: String = "")
private fun anyTvGenreLabels(
    title: StreamingTitle,
    movieGenres: List<StreamingGenre>,
    tvGenres: List<StreamingGenre>
): String {
    val source = if (title.media == "tv") tvGenres else movieGenres
    return title.genreIds.mapNotNull { id ->
        source.firstOrNull { it.id == id }?.name
    }.distinct().take(3).joinToString(" · ")
}
private const val ANYTV_CINEMA_FILTER = -1
private const val ANYTV_ANIME_FILTER = -2
private fun anyTvIsAnime(title: StreamingTitle): Boolean =
    title.originalLanguage == "ja" && 16 in title.genreIds
private const val ANYTV_CINEMA_SHELF = "Cinema · Em cartaz no Brasil"
private const val ANYTV_LIST_BATCH = 24
/** Movie genres drive 'All'; TV genres drive the 'Series' tab. */
private fun anyTvResolvedGenre(
    selectedGenre: Int, media: String,
    movieGenres: List<StreamingGenre>, tvGenres: List<StreamingGenre>
): Int {
    if (selectedGenre == ANYTV_ANIME_FILTER) return 16
    if (selectedGenre <= 0) return 0
    val source = (movieGenres + tvGenres).firstOrNull { it.id == selectedGenre }
        ?: return 0
    val candidates = if (media == "tv") tvGenres else movieGenres
    candidates.firstOrNull { it.id == selectedGenre }?.let { return it.id }
    val name = source.name.lowercase(java.util.Locale.ROOT)
    val synonyms = when (name) {
        "ação", "aventura", "action & adventure" ->
            listOf("ação", "aventura", "action & adventure")
        "ficção científica", "sci-fi & fantasy", "fantasia" ->
            listOf("ficção científica", "sci-fi & fantasy", "fantasia")
        else -> listOf(name)
    }
    return candidates.firstOrNull {
        it.name.lowercase(java.util.Locale.ROOT) in synonyms
    }?.id ?: 0
}

private fun anyTvProviderGenre(
    media: String, selectedGenre: Int,
    movieGenres: List<StreamingGenre>, tvGenres: List<StreamingGenre>
): Int = if (selectedGenre == ANYTV_ANIME_FILTER) ANYTV_ANIME_FILTER
    else anyTvResolvedGenre(selectedGenre, media, movieGenres, tvGenres)
private fun anyTvToday(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
private fun anyTvHasDate(item: StreamingTitle): Boolean =
    item.date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
private fun anyTvReleased(item: StreamingTitle): Boolean =
    anyTvHasDate(item) && item.date <= anyTvToday()
private fun anyTvUpcoming(item: StreamingTitle): Boolean =
    anyTvHasDate(item) && item.date > anyTvToday()
/** Novidades means a first release within the last 90 calendar days, not on-air status. */
private fun anyTvRecentPremiere(item: StreamingTitle): Boolean {
    if (!anyTvReleased(item)) return false
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.DAY_OF_YEAR, -90)
    val cutoff = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        .format(calendar.time)
    return item.date >= cutoff
}
internal data class StreamingGenre(val id: Int, val name: String)
internal data class StreamingSeason(val number: Int, val name: String, val count: Int, val airDate: String = "")
internal data class StreamingEpisode(val number: Int, val name: String, val airDate: String)
internal data class StreamingOffer(val id: Int, val name: String, val logo: String)
internal data class StreamingFavorite(val title: StreamingTitle, val packageName: String)
internal data class StreamingResults(val titles: List<StreamingTitle>, val more: Boolean)

/**
 * Local premiere reminder, not a paid remote push service. Android delivers the
 * notification at an approximate time; no background catalog polling is needed.
 */
class AnyTvReleaseReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("title")?.take(100).orEmpty()
        if (name.isBlank()) return
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager
        val channelId = "anytv_premiere_reminders"
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            notifications.createNotificationChannel(android.app.NotificationChannel(
                channelId, "Lembretes de estreias", android.app.NotificationManager.IMPORTANCE_DEFAULT))
        }
        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val action = openApp?.let {
            android.app.PendingIntent.getActivity(context, 0, it,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE)
        }
        val builder = if (android.os.Build.VERSION.SDK_INT >= 26)
            android.app.Notification.Builder(context, channelId)
        else android.app.Notification.Builder(context)
        builder.setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Estreia no AnyTV")
            .setContentText(name + " está chegando")
            .setAutoCancel(true)
        if (action != null) builder.setContentIntent(action)
        val id = intent.getIntExtra("id", 0)
        runCatching { notifications.notify(id, builder.build()) }
    }
}

private fun scheduleAnyTvPremiereReminder(context: Context, title: StreamingTitle): Boolean {
    if (title.media != "movie" || title.id <= 0) return false
    val release = runCatching {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .apply { isLenient = false }.parse(title.date)?.time
    }.getOrNull() ?: return false
    val calendar = java.util.Calendar.getInstance().apply {
        timeInMillis = release
        set(java.util.Calendar.HOUR_OF_DAY, 10)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
    }
    val remindAt = calendar.timeInMillis - 24L * 3600L * 1000L
    if (remindAt <= System.currentTimeMillis()) return false
    val intent = Intent(context, AnyTvReleaseReminderReceiver::class.java)
        .putExtra("title", title.name).putExtra("id", title.id)
    val pending = android.app.PendingIntent.getBroadcast(context, title.id, intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
            android.app.PendingIntent.FLAG_IMMUTABLE)
    val alarms = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    alarms.setWindow(android.app.AlarmManager.RTC_WAKEUP, remindAt, 10 * 60 * 1000L, pending)
    if (android.os.Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
        (context as? android.app.Activity)?.requestPermissions(
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2487)
    }
    return true
}

private fun cancelAnyTvPremiereReminder(context: Context, title: StreamingTitle) {
    val intent = Intent(context, AnyTvReleaseReminderReceiver::class.java)
    val pending = android.app.PendingIntent.getBroadcast(context, title.id, intent,
        android.app.PendingIntent.FLAG_NO_CREATE or
            android.app.PendingIntent.FLAG_IMMUTABLE)
    if (pending != null) {
        (context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager)
            .cancel(pending)
        pending.cancel()
    }
}

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
        for (field in listOf("name", "overview", "date", "poster", "backdrop",
            "genres", "rating", "votes")) {
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
            // Persistent title snapshots are user data, not expiring editorial responses.
            val files = target.parentFile?.listFiles {
                it.isFile && it.name.endsWith(".json") &&
                    !it.name.startsWith("saved_title_")
            }
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
            "universalplus" in v -> "universalplus"
            "looke" in v -> "looke"
            "clarovideo" in v -> "clarovideo"
            else -> null
        }
    }

    fun supportsSubscription(name: String): Boolean = provider(name) != null
    fun matchesSelectedSubscription(offer: StreamingOffer, selections: List<String>): Boolean =
        selections.any { service -> provider(service) != null &&
            provider(service) == provider(offer.name) }

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
                val matchGenre = genre == 0 || genre == ANYTV_ANIME_FILTER || (itemGenres != null &&
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
                        entry.optInt("vote_count", 0), backdrop, released, genreIds,
                        entry.optString("original_language")))
                    StreamingDiskCache.rememberTitle(media, movieId, JSONObject()
                        .put("name", name).put("overview", overview)
                        .put("date", released).put("poster", poster)
                        .put("backdrop", backdrop).put("genres", JSONArray(genreIds))
                        .put("rating", entry.optDouble("vote_average", 0.0))
                        .put("votes", entry.optInt("vote_count", 0)))
                }
            }
        }

    private fun providerId(token: String, app: String, media: String): Int {
        val alias = provider(app)
        val providers = get(token, "watch/providers/$media?language=pt-BR&watch_region=BR")
            .optJSONArray("results") ?: JSONArray()
        for (i in 0 until providers.length()) {
            val entry = providers.optJSONObject(i) ?: continue
            if (alias != null && norm(entry.optString("provider_name")) == alias) return entry.optInt("provider_id")
        }
        // Dynamic TMDB provider names are valid even when not in legacy aliases.
        for (i in 0 until providers.length()) {
            val entry = providers.optJSONObject(i) ?: continue
            if (anyTvServiceKey(entry.optString("provider_name")) ==
                anyTvServiceKey(app)) return entry.optInt("provider_id")
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
                // An additional subscription channel is a valid BR offer.
                // Keep its exact name/provider ID; never claim that an add-on
                // is included in the Amazon/Apple base subscription.
                StreamingOffer(id, name,
                    offer.optString("logo_path").takeIf { it.startsWith("/") } ?: "")
            }.distinctBy { it.id }
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

    /**
     * TMDB catalog responses are 20 entries per upstream page. Merge and filter
     * before slicing 24 visible entries, so a title excluded for a future
     * release never shifts page boundaries or causes skipped/duplicated IDs.
     * Cached source-page responses make subsequent loads inexpensive.
     */
    private suspend fun catalogPage24(
        page: Int,
        fetch: suspend (Int) -> StreamingResults
    ): StreamingResults {
        require(page in 1..42)
        val through = page * ANYTV_LIST_BATCH
        val collected = ArrayList<StreamingTitle>(through + 1)
        val seen = HashSet<String>()
        for (sourcePage in 1..50) {
            val source = fetch(sourcePage)
            for (title in source.titles) {
                if (seen.add("${title.media}:${title.id}")) collected.add(title)
                if (collected.size > through) break
            }
            if (collected.size > through || !source.more) break
        }
        return StreamingResults(
            collected.drop(through - ANYTV_LIST_BATCH).take(ANYTV_LIST_BATCH),
            collected.size > through
        )
    }

    /** Global editorial shelves independent of the TV's installed providers. */
    suspend fun discoveryShelf(route: String, media: String): List<StreamingTitle> =
        discoveryShelfPage(route, media, 1).titles

    /** Stable 24-title pages for dedicated editorial sections. */
    suspend fun discoveryShelfPage(route: String, media: String, page: Int): StreamingResults =
        withContext(Dispatchers.IO) {
            require(route in setOf("trending/movie/week", "trending/tv/week",
                "movie/now_playing", "movie/top_rated", "tv/on_the_air",
                "movie/upcoming", "movie/popular", "tv/popular", "tv/top_rated"))
            val suffix = if (route == "movie/now_playing") "&region=BR" else ""
            catalogPage24(page) { sourcePage ->
                val payload = get("backend",
                    route + "?language=pt-BR" + suffix + "&page=$sourcePage")
                val titles = mapTitles(payload.optJSONArray("results") ?: JSONArray(), media, 0)
                val matching = when (route) {
                    "movie/upcoming" -> titles.filter(::anyTvUpcoming)
                    "movie/now_playing" -> titles // Regional theatrical source.
                    else -> titles.filter(::anyTvReleased)
                }
                StreamingResults(matching, sourcePage < payload.optInt("total_pages", sourcePage))
            }
        }

    /** First premieres within 90 days. Never confuse theatrical now_playing with a release shelf. */
    suspend fun recentPremieres(media: String): List<StreamingTitle> =
        withContext(Dispatchers.IO) {
            require(media == "movie" || media == "tv")
            val dateField = if (media == "tv") "first_air_date" else "primary_release_date"
            // Date-only sorting can be dominated by future entries. Merge bounded
            // popularity and date-sorted pages before applying the factual date gate.
            val found = linkedMapOf<String, StreamingTitle>()
            for (sorting in listOf("popularity.desc", "$dateField.desc")) {
                for (sourcePage in 1..8) {
                    val payload = runCatching {
                        get("backend", "discover/$media?language=pt-BR&include_adult=false" +
                            "&sort_by=$sorting&page=$sourcePage")
                    }.getOrNull() ?: break
                    mapTitles(payload.optJSONArray("results") ?: JSONArray(), media, 0)
                        .filter(::anyTvRecentPremiere).filterNot(::anyTvIsAnime)
                        .forEach { title -> found.putIfAbsent("${title.media}:${title.id}", title) }
                    if (found.size >= 72 ||
                        sourcePage >= payload.optInt("total_pages", sourcePage)) break
                }
                if (found.size >= 72) break
            }
            val recent = found.values.sortedByDescending { it.date }.take(48)
            if (media != "movie") return@withContext recent
            // Only Cinema owns current theatrical titles. Subscription availability
            // is shown on each card, but is not a prerequisite for being a premiere.
            val theatrical = runCatching {
                (1..4).flatMap { page ->
                    discoveryShelfPage("movie/now_playing", "movie", page).titles
                }.map { it.id }.toSet()
            }.getOrNull() ?: return@withContext emptyList()
            recent.filterNot { it.id in theatrical }
        }

    /** Confirm a newly premiered season of a previously established series. */
    suspend fun recentlyStartedSeasons(): List<StreamingTitle> =
        withContext(Dispatchers.IO) {
            val ongoing = discoveryShelf("tv/on_the_air", "tv")
                .filter { !anyTvIsAnime(it) && anyTvReleased(it) && !anyTvRecentPremiere(it) }
                .take(24)
            val gate = Semaphore(4)
            coroutineScope {
                ongoing.map { title -> async {
                    gate.withPermit {
                        val seasons = runCatching { seasons(title.id) }.getOrDefault(emptyList())
                        title.takeIf { seasons.any {
                            it.number > 1 && it.airDate.isNotBlank() &&
                                anyTvRecentPremiere(title.copy(date = it.airDate))
                        }}
                    }
                }}.awaitAll().filterNotNull()
            }
        }

    /** Future film/TV releases are factual TMDB dates, not confirmed subscriptions. */
    suspend fun discoveryUpcomingStreaming(): List<StreamingTitle> =
        discoveryUpcomingStreamingPage(1).titles

    /** Page both future film and TV sources without claiming streaming availability.
     * Provider-specific filters remain fail-closed in the UI, based on BR offers.
     */
    suspend fun discoveryUpcomingStreamingPage(page: Int): StreamingResults =
        withContext(Dispatchers.IO) {
            require(page in 1..50)
            coroutineScope {
                val movieSource = async {
                    get("backend", "movie/upcoming?language=pt-BR&page=$page")
                }
                val seriesSource = async {
                    get("backend", "discover/tv?language=pt-BR&include_adult=false" +
                        "&sort_by=first_air_date.desc&page=$page")
                }
                val films = movieSource.await()
                val shows = seriesSource.await()
                val futureFilms = mapTitles(
                    films.optJSONArray("results") ?: JSONArray(), "movie", 0)
                    .filter(::anyTvUpcoming)
                val futureShows = mapTitles(
                    shows.optJSONArray("results") ?: JSONArray(), "tv", 0)
                    .filter(::anyTvUpcoming)
                val upcoming = buildList {
                    for (index in 0 until maxOf(futureFilms.size, futureShows.size)) {
                        futureFilms.getOrNull(index)?.let { add(it) }
                        futureShows.getOrNull(index)?.let { add(it) }
                    }
                }.distinctBy { "${it.media}:${it.id}" }
                StreamingResults(upcoming, page < 50 && (
                    page < films.optInt("total_pages", page) ||
                    page < shows.optInt("total_pages", page)))
            }
        }

    /** International cinema preview: TMDB default now-playing region.
     * This is an international selection, not a complete worldwide census. */
    suspend fun discoveryCinemaInternational(): List<StreamingTitle> =
        withContext(Dispatchers.IO) {
            catalogPage24(1) { sourcePage ->
                val payload = get("backend",
                    "movie/now_playing?language=pt-BR&page=$sourcePage")
                StreamingResults(
                    mapTitles(payload.optJSONArray("results") ?: JSONArray(), "movie", 0)
                        .filter(::anyTvReleased),
                    sourcePage < payload.optInt("total_pages", sourcePage))
            }.titles
        }

    /** TMDB theatrical listing for BR; no guessing from absent provider logos. */
    suspend fun discoveryCinemaPage(page: Int): StreamingResults =
        withContext(Dispatchers.IO) {
            require(page in 1..42)
            catalogPage24(page) { sourcePage ->
                val payload = get("backend",
                    "movie/now_playing?language=pt-BR&region=BR&page=$sourcePage")
                StreamingResults(
                    mapTitles(payload.optJSONArray("results") ?: JSONArray(), "movie", 0),
                    sourcePage < payload.optInt("total_pages", sourcePage))
            }
        }

    /** Genre pages expose 24 released titles; page 2+ loads on demand. */
    suspend fun discoveryGenre(id: Int): List<StreamingTitle> =
        discoveryGenrePage(id, "movie", 1).titles
    suspend fun discoveryGenre(id: Int, media: String): List<StreamingTitle> =
        discoveryGenrePage(id, media, 1).titles

    suspend fun discoveryGenrePage(id: Int, media: String, page: Int,
        sort: String = "popular"): StreamingResults =
        withContext(Dispatchers.IO) {
            require(media == "movie" || media == "tv")
            require(page in 1..42)
            require(id == ANYTV_ANIME_FILTER || id in setOf(28, 18, 35, 53, 37, 878, 12, 16, 27, 10749, 99, 9648, 14,
                10759, 80, 10765, 10766, 10768, 10762))
            val sorting = when (sort) {
                "rating" -> "vote_average.desc"
                "recent" -> if (media == "tv") "first_air_date.desc"
                    else "primary_release_date.desc"
                "title" -> if (media == "tv") "name.asc" else "title.asc"
                else -> "popularity.desc"
            }
            catalogPage24(page) { sourcePage ->
                val route = "discover/$media?language=pt-BR&include_adult=false" +
                    "&sort_by=$sorting&with_genres=${if (id == ANYTV_ANIME_FILTER) 16 else id}" +
                    "&page=$sourcePage"
                val payload = get("backend", route)
                StreamingResults(
                    mapTitles(payload.optJSONArray("results") ?: JSONArray(), media, id)
                        .filter(::anyTvReleased)
                        .filter { id != ANYTV_ANIME_FILTER || anyTvIsAnime(it) },
                    sourcePage < payload.optInt("total_pages", sourcePage))
            }
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
            }, count, e.optString("air_date"))
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

    /** Global search is never filtered by genres, installed apps or the home shelves. */
    suspend fun searchGlobal(query: String): List<StreamingTitle> = withContext(Dispatchers.IO) {
        require(query.length in 2..90)
        val normalized = query.trim()
        val withoutArticle = normalized.replace(Regex("^[Oo]\\s+"), "")
        val variants = linkedSetOf(normalized, withoutArticle)
        val identity = norm(normalized)
        // Brazilian marketing titles can differ entirely from the original TMDB name.
        if (identity.contains("amor") && identity.contains("sua") && identity.contains("vida") ||
            identity == "loveofyourlife") {
            variants += "Love of Your Life"
            variants += "O Amor da Sua Vida"
        }
        val collected = mutableListOf<StreamingTitle>()
        var successfulRequests = 0
        var firstFailure: Throwable? = null
        for (wording in variants) {
            val encoded = URLEncoder.encode(wording, "UTF-8")
            for (kind in listOf("movie", "tv")) {
                runCatching {
                    val response = get("backend",
                        "search/$kind?language=pt-BR&query=$encoded")
                    mapTitles(response.optJSONArray("results") ?: JSONArray(), kind, 0)
                }.onSuccess {
                    successfulRequests++
                    collected.addAll(it.take(20))
                }.onFailure {
                    if (firstFailure == null) firstFailure = it
                }
            }
        }
        if (successfulRequests == 0) {
            throw IllegalStateException("Não foi possível consultar o catálogo de filmes e séries.",
                firstFailure)
        }
        collected.filter(::anyTvReleased).distinctBy { "${it.media}:${it.id}" }
            .sortedWith(compareByDescending<StreamingTitle> {
                norm(it.name) == norm(withoutArticle) || norm(it.name) == identity ||
                    (identity.contains("amor") && identity.contains("vida") &&
                        norm(it.name) == "loveofyourlife")
            }.thenByDescending { it.votes }).take(ANYTV_LIST_BATCH)
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

    /** Recover ID-only watched films from authentic TMDB metadata only.
     * The caller preserves IDs when an old deployed Worker returns HTTP 400.
     */
    suspend fun lookupMovie(id: Int): StreamingTitle? = withContext(Dispatchers.IO) {
        if (id < 1) return@withContext null
        val entry = get("backend", "movie/$id?language=pt-BR")
        val name = entry.optString("title").ifBlank { entry.optString("original_title") }
        if (entry.optInt("id") != id || name.isBlank()) return@withContext null
        val genres = entry.optJSONArray("genres")
        val genreIds = (0 until (genres?.length() ?: 0))
            .map { genres!!.optJSONObject(it)?.optInt("id") ?: 0 }.filter { it > 0 }
        val poster = entry.optString("poster_path").takeIf { it.startsWith("/") }.orEmpty()
        val backdrop = entry.optString("backdrop_path").takeIf { it.startsWith("/") }.orEmpty()
        StreamingTitle(id, name, entry.optString("overview"), poster, "movie",
            rating = entry.optDouble("vote_average", 0.0),
            votes = entry.optInt("vote_count", 0),
            backdrop = backdrop, date = entry.optString("release_date"),
            genreIds = genreIds)
    }

    /** Recover legacy tracked shows whose title was lost from older editorial caches. */
    suspend fun lookupSeries(id: Int): StreamingTitle? = withContext(Dispatchers.IO) {
        if (id < 1) return@withContext null
        val entry = get("backend", "tv/$id?language=pt-BR")
        val name = entry.optString("name")
        if (name.isBlank()) return@withContext null
        val genreItems = entry.optJSONArray("genres")
        val genreIds = (0 until (genreItems?.length() ?: 0))
            .map { genreItems!!.optJSONObject(it)?.optInt("id") ?: 0 }.filter { it > 0 }
        val poster = entry.optString("poster_path").takeIf { it.startsWith("/") }.orEmpty()
        val backdrop = entry.optString("backdrop_path").takeIf { it.startsWith("/") }.orEmpty()
        StreamingTitle(id, name, entry.optString("overview"), poster, "tv",
            rating = entry.optDouble("vote_average", 0.0),
            votes = entry.optInt("vote_count", 0),
            backdrop = backdrop, date = entry.optString("first_air_date"), genreIds = genreIds)
    }

    suspend fun related(media: String, id: Int): List<StreamingTitle> = withContext(Dispatchers.IO) {
        catalogPage24(1) { sourcePage ->
            val response = get("backend",
                "$media/$id/recommendations?language=pt-BR&page=$sourcePage")
            StreamingResults(
                mapTitles(response.optJSONArray("results") ?: JSONArray(), media, 0)
                    .filter(::anyTvReleased),
                sourcePage < response.optInt("total_pages", sourcePage))
        }.titles
    }

    suspend fun discover(
        token: String, app: String, media: String, page: Int, genre: Int, query: String
    ): StreamingResults = withContext(Dispatchers.IO) {
        val providerId = providerId(token, app, media)
        if (providerId == 0) return@withContext StreamingResults(emptyList(), false)
        val search = query.isNotBlank()
        catalogPage24(page) { sourcePage ->
            val url = if (search)
                "search/$media?language=pt-BR&include_adult=false&query=" +
                    URLEncoder.encode(query, "UTF-8") + "&page=$sourcePage"
            else "discover/$media?language=pt-BR&watch_region=BR" +
                "&with_watch_providers=$providerId&with_watch_monetization_types=flatrate" +
                (if (genre == ANYTV_ANIME_FILTER) "&with_genres=16"
                 else if (genre > 0) "&with_genres=$genre" else "") +
                "&include_adult=false&sort_by=popularity.desc&page=$sourcePage"
            val result = get(token, url)
            val candidates = mapTitles(result.optJSONArray("results") ?: JSONArray(), media, genre)
                .filter(::anyTvReleased)
                .filter { genre != ANYTV_ANIME_FILTER || anyTvIsAnime(it) }
            // Search endpoints have no provider filter: verify BR subscription offers.
            val titles = if (search) coroutineScope {
                val limit = Semaphore(4)
                candidates.map { item ->
                    async {
                        val available = limit.withPermit {
                            runCatching { offeredInBrazil(token, item, providerId) }
                                .getOrDefault(false)
                        }
                        if (available) item else null
                    }
                }.awaitAll().filterNotNull()
            } else candidates
            StreamingResults(titles, sourcePage < result.optInt("total_pages", sourcePage))
        }
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
    val previewProviders = remember { mutableStateMapOf<Int, StreamingOffer?>() }
    var bannerCinemaIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    LaunchedEffect(Unit) {
        StreamingCatalog.configure(context)
        bannerCinemaIds = runCatching {
            val ids = mutableSetOf<Int>()
            for (page in 1..5) {
                val response = StreamingCatalog.discoveryCinemaPage(page)
                ids.addAll(response.titles.map { it.id })
                if (!response.more) break
            }
            ids.toSet()
        }.getOrDefault(emptySet())
        // discoveryShelf appends language itself: never pass a duplicate query.
        previews = runCatching {
            StreamingCatalog.discoveryShelf("trending/movie/week", "movie")
        }.getOrDefault(emptyList())
            .filter { (anyTvReleased(it) || it.id in bannerCinemaIds) &&
                (it.backdrop.isNotBlank() || it.poster.isNotBlank()) }.take(10)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().height(92.dp)
            .clickable(onClick = onOpenStreaming)
            .semantics { contentDescription = "Abrir destaques de filmes e séries" },
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF171A21)
    ) {
        if (previews.isEmpty()) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Destaques de filmes e séries", modifier = Modifier.weight(1f),
                    color = Color.White, maxLines = 1, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold)
                Text("Ver ›", color = Color(0xFFCDD0D7), fontSize = 12.sp)
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
                LaunchedEffect(movie.id) {
                    if (movie.id !in previewProviders) {
                        previewProviders[movie.id] = runCatching {
                            StreamingCatalog.offersInBrazil(movie).firstOrNull()
                        }.getOrNull()
                    }
                }
                // Real TMDB art fills the hero, fading into the graphite text side.
                Box(modifier = Modifier.fillMaxSize()) {
                    AnyTvPinnedImage(
                        model = "https://image.tmdb.org/t/p/w500" +
                            movie.backdrop.ifBlank { movie.poster },
                        contentDescription = "Destaque: " + movie.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(listOf(
                            Color(0xFF101216), Color(0xF0101216),
                            Color(0x80101216), Color.Transparent
                        ))
                    ))
                    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.7f)
                        .padding(start = 15.dp, end = 8.dp, top = 11.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.SpaceBetween) {
                        Text(if (movie.id in bannerCinemaIds) "F · CINEMA"
                             else "EM DESTAQUE", fontSize = 10.sp,
                            color = if (movie.id in bannerCinemaIds) Color(0xFF74ADFF)
                                    else Color(0xFFB7BCC5),
                            fontWeight = FontWeight.Medium)
                        Text(movie.name, fontSize = 15.sp, lineHeight = 18.sp,
                            color = Color.White, maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            style = TextStyle(shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.90f),
                                offset = Offset(0f, 2f), blurRadius = 5f)))
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Ver ›", fontSize = 12.sp,
                                color = Color(0xFFCED0D6), maxLines = 1)
                            previewProviders[movie.id]?.let { offer ->
                                if (offer.logo.isNotBlank()) {
                                    AnyTvPinnedImage(
                                        model = "https://image.tmdb.org/t/p/w92" + offer.logo,
                                        contentDescription = "Canal: " + offer.name,
                                        modifier = Modifier.size(14.dp),
                                        contentScale = ContentScale.Fit)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Shadowed score over original poster artwork. */
@Composable
private fun AnyTvRatingOverlay(title: StreamingTitle, modifier: Modifier = Modifier) {
    if (title.votes <= 0) return
    Text("★ " + String.format(java.util.Locale.US, "%.1f", title.rating),
        modifier = modifier.background(Color.Black.copy(alpha = 0.45f),
            RoundedCornerShape(6.dp)).padding(horizontal = 4.dp, vertical = 2.dp),
        fontSize = 11.sp, maxLines = 1, color = Color(0xFFFFD54F),
        fontWeight = FontWeight.Bold,
        style = TextStyle(shadow = Shadow(color = Color.Black,
            offset = Offset(0f, 2f), blurRadius = 5f)))
}

/**
 * The one poster/card specification for every streaming page and shelf.
 * Preserve the complete 2:3 artwork instead of clipping the bottom of posters
 * into a fixed 158dp rectangle. Provider icons are the original AnyTV icons.
 */
@Composable
private fun AnyTvPosterCard(
    title: StreamingTitle,
    modifier: Modifier = Modifier,
    offerCache: MutableMap<String, List<StreamingOffer>>,
    apps: List<TvOfficialApp>,
    favorite: Boolean,
    marked: Boolean,
    inCinema: Boolean,
    genreLabel: String,
    posterOnly: Boolean = false,
    onOpen: () -> Unit,
    onMarked: () -> Unit,
    onFavorite: () -> Unit
) {
    Column(modifier = modifier.clickable(onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(5.dp)).background(Color.Black)) {
            AnyTvPinnedImage(
                model = "https://image.tmdb.org/t/p/w342" + title.poster,
                contentDescription = "Cartaz: " + title.name,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            AnyTvRatingOverlay(title, Modifier.align(Alignment.TopStart)
                .padding(start = 5.dp, top = 5.dp))
            Row(modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 3.dp)) {
                IconButton(onClick = onMarked, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null,
                            tint = Color(0xFF353535).copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp).blur(1.dp))
                        Icon(Icons.Filled.CheckCircle,
                            contentDescription = if (title.media == "movie") "Marcar assistido"
                                else "Acompanhar série",
                            tint = if (marked) Color(0xFF1CB96B) else Color.White,
                            modifier = Modifier.size(19.dp))
                    }
                }
                IconButton(onClick = onFavorite, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Favorite, contentDescription = null,
                            tint = Color(0xFF353535).copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp).blur(1.dp))
                        Icon(Icons.Filled.Favorite,
                            contentDescription = if (favorite) "Remover dos favoritos" else "Favoritar",
                            tint = if (favorite) Color(0xFFE04B57) else Color.White,
                            modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
        if (!posterOnly) {
            // Standard card keeps its original title and metadata layout.
            Text(genreLabel, modifier = Modifier.fillMaxWidth(),
                fontSize = 10.sp, lineHeight = 13.sp, minLines = 1, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            StreamingOfferIcons(title, offerCache, apps, showRating = false,
                inCinema = inCinema, showYear = true)
            Text(title.name, modifier = Modifier.fillMaxWidth(),
                fontSize = 11.sp, lineHeight = 14.sp, minLines = 2, maxLines = 2,
                overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** One horizontal strip with three full-size standard cards visible on screen. */
@Composable
private fun AnyTvOneRowPreview(
    heading: String,
    titles: List<StreamingTitle>,
    card: @Composable (StreamingTitle, Modifier, Boolean) -> Unit
) {
    if (titles.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(3.dp).height(22.dp)
                .background(Color(0xFFE3383F), RoundedCornerShape(3.dp)))
            Text(heading, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - 18.dp) / 3f
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(titles.distinctBy { "${it.media}:${it.id}" }
                    .take(ANYTV_LIST_BATCH)) { title ->
                    // International and upcoming cinema listings are not proof of
                    // being in theaters in Brazil. Only cinemaIds identifies BR titles.
                    card(title, Modifier.width(cardWidth), false)
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
    expanded: Boolean = false,
    showRating: Boolean = true,
    inCinema: Boolean = false,
    showYear: Boolean = false
) {
    val key = "${item.media}:${item.id}"
    LaunchedEffect(key) {
        if (key !in known) {
            runCatching { StreamingCatalog.offersInBrazil(item) }
                .onSuccess { known[key] = it }
        }
    }
    val offerings = known[key].orEmpty()
    // Reserve a category/provider row, with rating only on legacy highlight cards.
    // No row jumps while provider details are loading.
    Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 16.dp)) {
            Box(modifier = Modifier.size(16.dp)
                .clip(RoundedCornerShape(if (item.media == "tv") 8.dp else 3.dp))
                .background(if (item.media == "tv") Color(0xFFD62828) else Color(0xFF1769E0)),
                contentAlignment = Alignment.Center) {
                Text(if (item.media == "tv") "S" else "F",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
                    fontSize = 10.sp, lineHeight = 10.sp,
                    style = TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false)),
                    fontWeight = FontWeight.Bold, color = Color.White)
            }
            if (showYear) {
                val year = item.date.take(4).takeIf { it.matches(Regex("[0-9]{4}")) }
                if (year != null) Text(year, fontSize = 10.sp, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.media == "movie" && inCinema) {
                Text("Cinema", modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1769E0))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                    fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1,
                    color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            if (showRating && item.votes > 0) {
                Text(String.format(java.util.Locale.US, "%.1f", item.rating),
                    fontSize = 11.sp, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Provider availability in BR is independent of installed TV apps.
            // Keep all distinct confirmed provider logos, scrolling horizontally
            // within the fixed card row instead of hiding providers after two.
            Row(modifier = Modifier.weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically) {
                offerings.distinctBy { anyTvServiceKey(it.name) }.forEach { offer ->
                    val installed = apps.firstOrNull {
                        StreamingCatalog.appHasOffer(it, listOf(offer))
                    }
                    if (installed != null) {
                        OfficialTvIcon(installed, 14.dp)
                    } else {
                        Box(modifier = Modifier.size(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center) {
                            if (offer.logo.isNotBlank()) {
                                AnyTvPinnedImage(
                                    model = "https://image.tmdb.org/t/p/w92" + offer.logo,
                                    contentDescription = "Disponível em " + offer.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit)
                            } else {
                                Text(offer.name.take(2), fontSize = 8.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
}

/** Shared card metadata: left year/score, right watched and favorite controls. */
@Composable
private fun AnyTvCardMetaRow(
    item: StreamingTitle,
    favorite: Boolean,
    marked: Boolean,
    compact: Boolean,
    onMarked: () -> Unit,
    onFavorite: () -> Unit
) {
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val buttonSize = if (compact) 27.dp else 31.dp
    Row(modifier = Modifier.fillMaxWidth().height(31.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            val year = item.date.take(4).takeIf { it.matches(Regex("[0-9]{4}")) }
            if (year != null) {
                Text(year, fontSize = if (compact) 9.sp else 11.sp,
                    maxLines = 1, softWrap = false, color = neutral)
            }
            if (item.votes > 0) {
                if (year != null) Text("·", fontSize = 10.sp, color = neutral)
                Text(String.format(java.util.Locale.US, "%.1f", item.rating),
                    fontSize = if (compact) 9.sp else 11.sp,
                    maxLines = 1, softWrap = false, color = neutral)
            }
        }
        IconButton(onClick = onMarked, modifier = Modifier.size(buttonSize)) {
            Icon(Icons.Filled.CheckCircle,
                contentDescription = if (item.media == "movie")
                    (if (marked) "Desmarcar assistido" else "Marcar assistido")
                else (if (marked) "Parar de acompanhar" else "Acompanhar série"),
                tint = if (marked) Color(0xFF1CB96B) else neutral,
                modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(buttonSize)) {
            Icon(Icons.Filled.Favorite,
                contentDescription = if (favorite) "Remover dos favoritos" else "Favoritar",
                tint = if (favorite) Color(0xFFE04B57) else neutral,
                modifier = Modifier.size(18.dp))
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
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val actionScroll = rememberScrollState()
    val chips: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AnyTvCompactChip("Filmes", !library && contentType != "tv") {
                onContentType(if (contentType == "all") "tv" else "all")
            }
            AnyTvCompactChip("Séries", !library && contentType != "movie") {
                onContentType(if (contentType == "all") "movie" else "all")
            }
        }
    }
    val actions: @Composable () -> Unit = {
        Row(modifier = Modifier.horizontalScroll(actionScroll),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            // Reuse the approved home-screen icons, tint and click handlers.
            TclHeaderAction(Icons.Filled.Home, "Início: filmes e séries",
                if (!library && contentType == "all") primary else neutral, onHome)
            TclHeaderAction(Icons.Filled.Favorite, "Minha lista",
                if (library && section == "saved") primary else neutral, onList)
            TclHeaderAction(Icons.Filled.PlayCircleFilled, "Séries acompanhadas",
                if (library && section == "watching") primary else neutral, onWatching)
            TclHeaderAction(Icons.Filled.CheckCircle, "Assistidos",
                if (library && section == "watched") primary else neutral, onWatched)
            TclHeaderAction(Icons.Filled.FilterList, "Filtrar streamings e gêneros",
                if (filtersActive) primary else neutral, onFilters)
            TclHeaderAction(Icons.Filled.Close, "Fechar streaming", neutral, onDismiss)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        chips()
        Box(modifier = Modifier.weight(1f)) { actions() }
    }
}

@Composable
private fun AnyTvCompactChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(modifier = Modifier.padding(end = 2.dp)
        .clip(shape)
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow)
        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.outlineVariant, shape)
        .clickable(onClick = onClick)
        .padding(horizontal = 7.dp, vertical = 5.dp)) {
        Text(label, fontSize = 11.sp, maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Compact icon-only selector for the genre list/three-column grid. */
@Composable
private fun AnyTvGenreViewIcon(
    image: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = Modifier.size(34.dp)
        .clip(shape)
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow)
        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.outlineVariant, shape)
        .clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(image, contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(19.dp))
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
    var selected by remember { mutableStateOf<TvOfficialApp?>(null) }
    val mainGenreIds = remember { listOf(28, 18, 35, 53, 37, 878, 12, 16, 27, 10749, 99, 9648, 14) }
    var mainGenres by remember { mutableStateOf<List<StreamingGenre>>(emptyList()) }
    var allMovieGenres by remember { mutableStateOf<List<StreamingGenre>>(emptyList()) }
    var tvGenres by remember { mutableStateOf<List<StreamingGenre>>(emptyList()) }
    var homeContentType by remember { mutableStateOf("all") }
    var librarySection by remember { mutableStateOf("saved") }
    var selectedHomeGenre by remember { mutableIntStateOf(0) }
    var genreSort by remember(prefs) {
        mutableStateOf(prefs.getString("anytv_genre_sort", "popular")
            ?.takeIf { it in setOf("popular", "rating", "recent", "title") } ?: "popular")
    }
    var genreSortMenu by remember { mutableStateOf(false) }
    var genreViewMode by remember { mutableStateOf("detalhado") }
    var genreSectionsMode by remember { mutableStateOf(false) }
    val catalogScrollState = rememberLazyListState()
    var showGenreSettings by remember { mutableStateOf(false) }
    var showMyStreamingSettings by remember { mutableStateOf(false) }
    val subscriptionNames = remember { listOf("Netflix", "Prime Video", "Disney+", "Max", "Paramount+", "Apple TV+", "Globoplay", "Crunchyroll", "Mubi", "Telecine", "Looke", "Claro Video", "Universal+") }
    var filterGenreIds by remember(prefs) {
        mutableStateOf(prefs.getString("anytv_discovery_genre_filter", null)
            ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: setOf(28, 18, 35, 53, 37))
    }
    var editingFilterGenres by remember { mutableStateOf(emptySet<Int>()) }
    var onlyMyStreaming by remember { mutableStateOf(false) }
    val storedMyServices = remember(prefs) {
        prefs.getString("anytv_my_streamings", "")?.split(",")
            ?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }
    var myServices by remember { mutableStateOf(storedMyServices) }
    LaunchedEffect(myServices) { onlyMyStreaming = myServices.isNotEmpty() }
    var editingMyServices by remember { mutableStateOf(storedMyServices) }
    val myServiceShelves = remember { mutableStateMapOf<String, List<StreamingTitle>>() }
    var providerListMode by remember { mutableStateOf(false) }
    var showCatalogEditor by remember { mutableStateOf(false) }
    val catalogPrefs = remember(context) { context.getSharedPreferences("tcl_tv_installed_apps", Context.MODE_PRIVATE) }
    var visibleCatalogServices by remember { mutableStateOf(catalogPrefs.getString("anytv_catalog_visible_services", "")?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    var tmdbProviderArtwork by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        tmdbProviderArtwork = runCatching { anyTvBrazilProviders() }
            .getOrDefault(emptyList()).associate { anyTvServiceKey(it.name) to it.logo }
    }
    var hiddenInstalledServices by remember { mutableStateOf(catalogPrefs.getString("anytv_catalog_hidden_installed", "")?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    val providerListTitles = remember { mutableStateMapOf<String, List<StreamingTitle>>() }
    val providerListPages = remember { mutableStateMapOf<String, Int>() }
    val providerListMore = remember { mutableStateMapOf<String, Boolean>() }
    val providerListLoading = remember { mutableStateMapOf<String, Boolean>() }
    val providerListErrors = remember { mutableStateMapOf<String, String>() }
    val activeServiceNames = remember(myServices, apps) {
        myServices.map { name -> apps.firstOrNull { it.packageName == name }?.name ?: name }
            .filter { StreamingCatalog.supportsSubscription(it) }.distinct().take(12)
    }
    var homeLibrary by remember { mutableStateOf(false) }
    var openedSection by remember { mutableStateOf<String?>(null) }
    var sectionVisibleCount by remember { mutableIntStateOf(ANYTV_LIST_BATCH) }
    var sectionExtra by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    var sectionPage by remember { mutableIntStateOf(1) }
    var sectionMore by remember { mutableStateOf(true) }
    // Declared before the opened-section effect, which reads this state.
    var upcomingStreamingMore by remember { mutableStateOf(false) }
    var sectionLoading by remember { mutableStateOf(false) }
    var sectionError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(openedSection) {
        sectionVisibleCount = ANYTV_LIST_BATCH
        sectionExtra = emptyList()
        sectionPage = 1
        sectionMore = if (openedSection == "Em breve · Filmes e séries")
            upcomingStreamingMore else true
        sectionLoading = false
        sectionError = null
        // A dedicated shelf is a new page, not an expansion at the old scroll position.
        catalogScrollState.scrollToItem(0)
    }
    var trackedSeries by remember {
        mutableStateOf(prefs.getString("anytv_tracking_series", "")
            ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
    }
    // Keep in-progress series independent of an evictable editorial cache.
    var watchingTitles by remember(prefs) {
        mutableStateOf(runCatching {
            val stored = JSONArray(prefs.getString("anytv_watching_titles_v1", "[]"))
            (0 until stored.length()).mapNotNull { index ->
                val entry = stored.optJSONObject(index) ?: return@mapNotNull null
                val id = entry.optInt("id")
                val name = entry.optString("name")
                if (id <= 0 || name.isBlank()) null
                else StreamingTitle(id, name, entry.optString("overview"),
                    entry.optString("poster"), "tv",
                    rating = entry.optDouble("rating", 0.0),
                    votes = entry.optInt("votes", 0),
                    backdrop = entry.optString("backdrop"),
                    date = entry.optString("date"),
                    genreIds = entry.optJSONArray("genres")?.let { genres ->
                        (0 until genres.length()).map { genres.optInt(it) }.filter { it > 0 }
                    } ?: emptyList())
            }.associateBy { it.id }
        }.getOrDefault(emptyMap<Int, StreamingTitle>()))
    }
    fun rememberWatchingTitle(title: StreamingTitle) {
        if (title.media != "tv" || title.id <= 0) return
        watchingTitles = watchingTitles + (title.id to title)
        val stored = JSONArray()
        watchingTitles.values.forEach { item ->
            stored.put(JSONObject().put("id", item.id).put("name", item.name)
                .put("overview", item.overview).put("poster", item.poster)
                .put("backdrop", item.backdrop).put("date", item.date)
                .put("rating", item.rating).put("votes", item.votes)
                .put("genres", JSONArray(item.genreIds)))
        }
        prefs.edit().putString("anytv_watching_titles_v1", stored.toString()).apply()
    }
    fun toggleSeriesTracking(title: StreamingTitle) {
        val id = title.id
        rememberWatchingTitle(title)
        trackedSeries = if (id.toString() in trackedSeries)
            trackedSeries - id.toString() else trackedSeries + id.toString()
        prefs.edit().putString("anytv_tracking_series",
            trackedSeries.sorted().joinToString(",")).apply()
    }
    var watchingQuery by remember { mutableStateOf("") }
    var libraryQuery by remember { mutableStateOf("") }
    LaunchedEffect(librarySection, homeLibrary) {
        if (homeLibrary) {
            sectionVisibleCount = ANYTV_LIST_BATCH
            catalogScrollState.scrollToItem(0)
        }
    }
    LaunchedEffect(watchingQuery) {
        if (homeLibrary && librarySection == "watching") {
            catalogScrollState.scrollToItem(0)
        }
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
    val genrePage = remember { mutableStateMapOf<String, Int>() }
    val genreMore = remember { mutableStateMapOf<String, Boolean>() }
    val genreLoading = remember { mutableStateMapOf<String, Boolean>() }
    val genreErrors = remember { mutableStateMapOf<String, String>() }
    var genreFirstPageRetry by remember { mutableIntStateOf(0) }
    val premiereReminders = remember { mutableStateMapOf<String, Boolean>() }
    var globalSearchDraft by remember { mutableStateOf("") }
    var globalSearchTerm by remember { mutableStateOf("") }
    var globalSearchError by remember { mutableStateOf<String?>(null) }
    var globalSearchResults by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    var globalSearchBusy by remember { mutableStateOf(false) }
    // Store only successful searches in bounded private app storage.
    var globalSearchHistory by remember(prefs) {
        mutableStateOf(runCatching {
            val arr = JSONArray(prefs.getString("anytv_global_search_history_v1", "[]"))
            (0 until arr.length()).map { arr.optString(it) }
                .filter { it.length in 3..90 }.distinct().take(10)
        }.getOrDefault(emptyList<String>()))
    }
    fun rememberSuccessfulSearch(term: String) {
        val name = term.trim().take(90)
        if (name.length < 3) return
        globalSearchHistory = (listOf(name) +
            globalSearchHistory.filterNot { it.equals(name, ignoreCase = true) }).take(10)
        prefs.edit().putString("anytv_global_search_history_v1",
            JSONArray(globalSearchHistory).toString()).apply()
    }
    // Debounced autocomplete: one search after the user pauses, not per keystroke.
    LaunchedEffect(globalSearchDraft) {
        delay(650)
        globalSearchTerm = globalSearchDraft.trim().takeIf { it.length >= 3 }.orEmpty()
    }

    var discoveryShelves by remember {
        mutableStateOf<Map<String, List<StreamingTitle>>>(emptyMap())
    }
    var discoveryLoading by remember { mutableStateOf(false) }
    var discoveryError by remember { mutableStateOf<String?>(null) }
    var upcomingStreaming by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    var cinemaComingSoon by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    var cinemaWorldwide by remember { mutableStateOf<List<StreamingTitle>>(emptyList()) }
    LaunchedEffect(token) {
        val streamingFuture = runCatching {
            StreamingCatalog.discoveryUpcomingStreamingPage(1)
        }.getOrDefault(StreamingResults(emptyList(), false))
        upcomingStreaming = streamingFuture.titles
        upcomingStreamingMore = streamingFuture.more
        cinemaComingSoon = runCatching {
            StreamingCatalog.discoveryShelf("movie/upcoming", "movie")
        }.getOrDefault(emptyList()).filter(::anyTvUpcoming)
        cinemaWorldwide = runCatching {
            StreamingCatalog.discoveryCinemaInternational()
        }.getOrDefault(emptyList())
    }
    // Only positive matches in Brazil's now-playing feed receive the Cinema badge.
    var cinemaIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    LaunchedEffect(token) {
        for (page in 1..5) {
            val response = runCatching { StreamingCatalog.discoveryCinemaPage(page) }
                .getOrNull() ?: break
            cinemaIds = cinemaIds + response.titles.map { it.id }
            if (!response.more) break
        }
    }

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
    var deviceLinkStatus by remember { mutableStateOf<String?>(null) }
    var deviceIdSuffix by remember { mutableStateOf<String?>(null) }
    var linkActivationRequired by remember { mutableStateOf(false) }
    var deviceActivationPending by remember { mutableStateOf(false) }
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
                        o.optString("poster"), kind,
                        rating = o.optDouble("rating", 0.0),
                        votes = o.optInt("votes", 0), date = o.optString("date"),
                        genreIds = o.optJSONArray("genres")?.let { arr ->
                            (0 until arr.length()).map { arr.optInt(it) }.filter { it > 0 }
                        } ?: emptyList()), pkg)
            }
        }.getOrDefault(emptyList<StreamingFavorite>()))
    }
    // In addition to the non-expiring title cache, preserve watched-film metadata
    // with the user's history preferences, as already done for tracked series.
    var watchedMovieTitles by remember(prefs) {
        mutableStateOf(runCatching {
            val records = JSONArray(prefs.getString("anytv_watched_movie_titles_v1", "[]"))
            (0 until records.length()).mapNotNull { index ->
                val entry = records.optJSONObject(index) ?: return@mapNotNull null
                val id = entry.optInt("id")
                val name = entry.optString("name")
                if (id <= 0 || name.isBlank()) return@mapNotNull null
                StreamingTitle(id, name, entry.optString("overview"),
                    entry.optString("poster"), "movie",
                    rating = entry.optDouble("rating", 0.0),
                    votes = entry.optInt("votes", 0),
                    backdrop = entry.optString("backdrop"),
                    date = entry.optString("date"),
                    genreIds = entry.optJSONArray("genres")?.let { genres ->
                        (0 until genres.length()).map { genres.optInt(it) }.filter { it > 0 }
                    } ?: emptyList())
            }.associateBy { it.id }
        }.getOrDefault(emptyMap<Int, StreamingTitle>()))
    }
    fun preserveWatchedMovieTitles(titlesToSave: Collection<StreamingTitle>) {
        val valid = titlesToSave.filter { it.id > 0 && it.media == "movie" && it.name.isNotBlank() }
        if (valid.isEmpty()) return
        watchedMovieTitles = watchedMovieTitles + valid.associateBy { it.id }
        val records = JSONArray()
        watchedMovieTitles.values.forEach { item ->
            records.put(JSONObject().put("id", item.id).put("name", item.name)
                .put("overview", item.overview).put("poster", item.poster)
                .put("backdrop", item.backdrop).put("date", item.date)
                .put("rating", item.rating).put("votes", item.votes)
                .put("genres", JSONArray(item.genreIds)))
        }
        prefs.edit().putString("anytv_watched_movie_titles_v1", records.toString()).apply()
    }
    // Watched history survives removing a heart: it is independent of favorites.
    fun restoreTrackedTitle(media: String, id: Int): StreamingTitle? {
        if (media == "tv") watchingTitles[id]?.let { return it }
        favorites.firstOrNull { it.title.id == id && it.title.media == media &&
                it.title.name.isNotBlank() }
            ?.title?.let { return it }
        if (media == "movie") watchedMovieTitles[id]?.let { return it }
        val cached = StreamingDiskCache.previousTitle(media, id) ?: return null
        val titleName = cached.optString("name")
        if (titleName.isBlank()) return null
        val genreArr = cached.optJSONArray("genres")
        return StreamingTitle(id, titleName, cached.optString("overview"),
            cached.optString("poster"), media,
            rating = cached.optDouble("rating", 0.0),
            votes = cached.optInt("votes", 0),
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
                .put("rating", value.title.rating).put("votes", value.title.votes)
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
    // The existing public Worker rejects movie/{id}; probe at most once per
    // screen session until its metadata route is separately approved/deployed.
    var movieDetailRouteUnsupported by remember { mutableStateOf(false) }
    // A nonexistent/invalid ID must not monopolize each later batch of 12.
    var absentWatchedMovieIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var restoringWatchedMovies by remember { mutableStateOf(false) }
    // Explicitly request another bounded batch without leaving Assistidos.
    var watchedMoviesRetry by remember { mutableIntStateOf(0) }
    fun toggleFilmWatched(title: StreamingTitle) {
        val key = title.id.toString()
        if (key !in watchedFilms) {
            // Redundant user-history snapshot: prefs survive even if the
            // catalogue folder is removed, without inventing any metadata.
            preserveWatchedMovieTitles(listOf(title))
            // A check directly on a poster must survive catalog-cache expiry and restart.
            StreamingDiskCache.rememberTitle("movie", title.id,
                JSONObject().put("name", title.name).put("overview", title.overview)
                    .put("poster", title.poster).put("backdrop", title.backdrop)
                    .put("date", title.date).put("rating", title.rating)
                    .put("votes", title.votes).put("genres", JSONArray(title.genreIds)))
        }
        watchedFilms = if (key in watchedFilms) watchedFilms - key else watchedFilms + key
        prefs.edit().putString("anytv_watched_movies",
            watchedFilms.sorted().joinToString(",")).apply()
    }
    // Recover legitimate local records first; only unknown IDs need the new
    // server-side metadata lookup. Do not remove IDs, guess film titles or
    // contact unapproved third-party APIs from the Android application.
    LaunchedEffect(watchedFilms, favorites, homeLibrary, librarySection, watchedMoviesRetry) {
        if (homeLibrary && librarySection == "watched") {
            val ids = watchedFilms.mapNotNull(String::toIntOrNull)
                .filter { it > 0 }.distinct().sorted()
            val locallyRecovered = ids.filter { it !in watchedMovieTitles }
                .mapNotNull { restoreTrackedTitle("movie", it) }
            preserveWatchedMovieTitles(locallyRecovered)
            if (!movieDetailRouteUnsupported) {
                val missing = ids.filter {
                    restoreTrackedTitle("movie", it) == null && it !in absentWatchedMovieIds
                }.take(12)
                if (missing.isNotEmpty()) {
                    restoringWatchedMovies = true
                    val remotelyRecovered = mutableListOf<StreamingTitle>()
                    try {
                        for (id in missing) {
                            val film = try {
                                StreamingCatalog.lookupMovie(id)
                            } catch (error: java.util.concurrent.CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                if (error.message?.contains("(HTTP 404)") == true) {
                                    // TMDB confirms this ID has no movie detail.
                                    // Leave the ID marked and continue to valid IDs.
                                    absentWatchedMovieIds = absentWatchedMovieIds + id
                                    continue
                                }
                                // Old deployments reject the route with HTTP 400.
                                // Stop after one probe, without flooding the Worker.
                                if (error.message?.contains("(HTTP 400)") == true)
                                    movieDetailRouteUnsupported = true
                                // Offline or upstream unavailable: retry only on
                                // a future visit; never delete the saved TMDB ID.
                                break
                            }
                            if (film != null) remotelyRecovered.add(film)
                            else absentWatchedMovieIds = absentWatchedMovieIds + id
                        }
                    } finally {
                        // Save authentic successes even if the user leaves the
                        // screen and coroutine cancellation interrupts the batch.
                        preserveWatchedMovieTitles(remotelyRecovered)
                        restoringWatchedMovies = false
                    }
                }
            }
        }
    }
    var watchedEpisodes by remember {
        mutableStateOf(prefs.getString("anytv_watched_episodes", "")
            ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet())
    }
    fun toggleWatched(title: StreamingTitle, season: Int, episode: Int) {
        rememberWatchingTitle(title)
        val key = "${title.id}:$season:$episode"
        watchedEpisodes = if (key in watchedEpisodes) watchedEpisodes - key
            else watchedEpisodes + key
        prefs.edit().putString("anytv_watched_episodes",
            watchedEpisodes.sorted().joinToString(",")).apply()
    }
    var watchingRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(trackedSeries, watchedEpisodes) {
        val keys = trackedSeries + watchedEpisodes.mapNotNull {
            it.substringBefore(":").takeIf(String::isNotBlank)
        }
        val missing = keys.mapNotNull(String::toIntOrNull).filter {
            restoreTrackedTitle("tv", it) == null
        }.distinct()
        watchingRefreshing = missing.isNotEmpty()
        for (id in missing) {
            val restored = runCatching { StreamingCatalog.lookupSeries(id) }.getOrNull()
            if (restored != null) rememberWatchingTitle(restored)
        }
        watchingRefreshing = false
    }
    val displayed = remember(apps, all) { apps.filter { all || StreamingCatalog.streaming(it) } }

    LaunchedEffect(globalSearchTerm) {
        globalSearchResults = emptyList()
        globalSearchError = null
        if (globalSearchTerm.length >= 2) {
            globalSearchBusy = true
            runCatching { StreamingCatalog.searchGlobal(globalSearchTerm) }
                .onSuccess { globalSearchResults = it }
                .onFailure {
                    globalSearchError = "Falha ao consultar o catálogo. " +
                        "Confira a conexão e tente novamente."
                }
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
    LaunchedEffect(selectedHomeGenre, genreSort) {
        catalogScrollState.scrollToItem(0)
    }
    LaunchedEffect(selectedHomeGenre) {
        if (selectedHomeGenre != 0) {
            genreViewMode = "detalhado"
            genreSectionsMode = false
        }
    }
    LaunchedEffect(selectedHomeGenre, selected?.packageName, homeLibrary, homeContentType) {
        openedSection = null
    }
    LaunchedEffect(mainGenres, tvGenres, filterGenreIds, selectedHomeGenre,
        homeContentType, genreSort, genreFirstPageRetry) {
        val kinds = if (homeContentType == "all") listOf("movie", "tv")
            else listOf(homeContentType)
        for (mediaKind in kinds) {
            val choices = if (mediaKind == "tv") tvGenres else mainGenres
            val requested = if (selectedHomeGenre == 0) {
                filterGenreIds.mapNotNull { selectedId ->
                    anyTvResolvedGenre(selectedId, mediaKind, allMovieGenres, tvGenres)
                        .takeIf { it > 0 }
                }.distinct()
            } else if (selectedHomeGenre == ANYTV_CINEMA_FILTER) emptyList()
              else if (selectedHomeGenre == ANYTV_ANIME_FILTER) listOf(ANYTV_ANIME_FILTER)
              else listOfNotNull(
                  anyTvResolvedGenre(selectedHomeGenre, mediaKind,
                      allMovieGenres, tvGenres).takeIf { it > 0 })
            for (id in requested) {
                val key = "$mediaKind:$id" +
                    if (selectedHomeGenre != 0) ":$genreSort" else ""
                if (key !in genreShelves) {
                    genreLoading[key] = true
                    try {
                        val result = StreamingCatalog.discoveryGenrePage(id, mediaKind, 1,
                            if (selectedHomeGenre != 0) genreSort else "popular")
                        genreShelves[key] = result.titles
                        genrePage[key] = 1
                        genreMore[key] = result.more
                        genreErrors.remove(key)
                    } catch (error: java.util.concurrent.CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        genreErrors[key] = "Não foi possível consultar este gênero."
                    } finally {
                        genreLoading[key] = false
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedHomeGenre, homeContentType, genreSort, genreFirstPageRetry) {
        if (selectedHomeGenre == ANYTV_CINEMA_FILTER) {
            val key = "movie:$ANYTV_CINEMA_FILTER:$genreSort"
            if (key !in genreShelves) {
                genreLoading[key] = true
                try {
                    val result = StreamingCatalog.discoveryCinemaPage(1)
                    genreShelves[key] = result.titles
                    cinemaIds = cinemaIds + result.titles.map { title -> title.id }
                    genrePage[key] = 1
                    genreMore[key] = result.more
                    genreErrors.remove(key)
                } catch (error: java.util.concurrent.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    genreErrors[key] = "Não foi possível consultar cinemas no Brasil."
                } finally {
                    genreLoading[key] = false
                }
            }
        }
    }

    LaunchedEffect(activeServiceNames, selectedHomeGenre, homeContentType) {
        for (name in activeServiceNames) {
            val kinds = listOf("movie", "tv").filter {
                homeContentType == "all" || it == homeContentType
            }
            for (kind in kinds) {
                val chosenGenre = anyTvProviderGenre(kind, selectedHomeGenre, allMovieGenres, tvGenres)
                if (selectedHomeGenre > 0 && chosenGenre == 0) continue
                val key = name + ":" + kind + ":" + chosenGenre
                if (key !in myServiceShelves) {
                    runCatching {
                        StreamingCatalog.discover(token, name, kind, 1, chosenGenre, "")
                    }.onSuccess { myServiceShelves[key] = it.titles }
                }
            }
        }
    }

    // Keep every editorial section when filtering subscriptions, but show only
    // titles with a confirmed BR offer from one of the selected providers.
    LaunchedEffect(discoveryShelves, upcomingStreaming, genreShelves.toMap(), activeServiceNames) {
        if (activeServiceNames.isNotEmpty()) {
            val candidates = (discoveryShelves.values.flatten() +
                genreShelves.values.flatten() + upcomingStreaming)
                .distinctBy { "${it.media}:${it.id}" }.take(300)
            // Resolve BR offers with bounded concurrency. Sequential retrieval
            // kept unrelated home sections empty while waiting for slow providers.
            val gate = Semaphore(4)
            coroutineScope {
                candidates.map { title ->
                    async {
                        val key = "${title.media}:${title.id}"
                        if (key !in offerCache) {
                            gate.withPermit {
                                runCatching { StreamingCatalog.offersInBrazil(title) }
                                    .onSuccess { offerCache[key] = it }
                            }
                        }
                    }
                }.awaitAll()
            }
        }
    }

    LaunchedEffect(selected?.packageName, selectedHomeGenre, homeContentType, providerListMode) {
        val app = selected ?: return@LaunchedEffect
        if (!providerListMode) return@LaunchedEffect
        val kinds = listOf("movie", "tv").filter {
            homeContentType == "all" || it == homeContentType
        }
        for (kind in kinds) {
            val id = anyTvProviderGenre(kind, selectedHomeGenre, allMovieGenres, tvGenres)
            if (selectedHomeGenre > 0 && id == 0) continue
            val key = "${app.name}:$kind:$id"
            if (key !in providerListTitles) {
                providerListLoading[key] = true
                try {
                    val response = StreamingCatalog.discover(token, app.name, kind, 1, id, "")
                    providerListTitles[key] = response.titles
                    providerListPages[key] = 1
                    providerListMore[key] = response.more
                    providerListErrors.remove(key)
                } catch (error: java.util.concurrent.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    providerListErrors[key] = "Não foi possível carregar este catálogo."
                } finally {
                    providerListLoading[key] = false
                }
            }
        }
    }

    // Provider pages reuse the home carousel, loading real provider results.
    LaunchedEffect(selected?.packageName, homeContentType, selectedHomeGenre) {
        val app = selected ?: return@LaunchedEffect
        val kinds = listOf("movie", "tv").filter {
            homeContentType == "all" || it == homeContentType
        }
        for (kind in kinds) {
            val chosenGenre = anyTvProviderGenre(kind, selectedHomeGenre, allMovieGenres, tvGenres)
            if (selectedHomeGenre > 0 && chosenGenre == 0) continue
            val key = app.name + ":" + kind + ":" + chosenGenre
            if (key !in myServiceShelves) {
                runCatching {
                    StreamingCatalog.discover(token, app.name, kind, 1, chosenGenre, "")
                }.onSuccess { myServiceShelves[key] = it.titles }
            }
        }
    }

    LaunchedEffect(token) {
        discoveryLoading = true
        discoveryError = null
        // Separate BR theatrical titles from editorial/streaming shelves.
        // The first cinematic batch alone is not enough to exclude older pages.
        val theatricalPages = runCatching {
            (1..4).flatMap { page -> StreamingCatalog.discoveryCinemaPage(page).titles }
        }.getOrDefault(emptyList())
        cinemaIds = cinemaIds + theatricalPages.map { it.id }
        val shelves = linkedMapOf<String, List<StreamingTitle>>()
        for ((name, route, kind) in listOf(
            Triple("Em alta nesta semana", "trending/movie/week", "movie"),
            Triple(ANYTV_CINEMA_SHELF, "movie/now_playing", "movie"),
            Triple("Mais bem avaliados", "movie/top_rated", "movie"),
            Triple("Mais bem avaliados · Séries", "tv/top_rated", "tv"),
            Triple("Séries em destaque", "trending/tv/week", "tv"),
            Triple("Filmes e séries mais assistidos · Filmes", "movie/popular", "movie"),
            Triple("Filmes e séries mais assistidos · Séries", "tv/popular", "tv")
        )) {
            runCatching { StreamingCatalog.discoveryShelf(route, kind) }
                .onSuccess { if (it.isNotEmpty()) shelves[name] = it }
                .onFailure { discoveryError = "Algumas seções não puderam carregar." }
        }
        fun interleave(films: List<StreamingTitle>, series: List<StreamingTitle>) =
            buildList {
                for (index in 0 until maxOf(films.size, series.size)) {
                    films.getOrNull(index)?.let { add(it) }
                    series.getOrNull(index)?.let { add(it) }
                }
            }
        val weekly = interleave(shelves.remove("Em alta nesta semana").orEmpty(),
            shelves.remove("Séries em destaque").orEmpty())
        val recentMovies = runCatching { StreamingCatalog.recentPremieres("movie") }.getOrDefault(emptyList())
        val newSeries = runCatching { StreamingCatalog.recentPremieres("tv") }.getOrDefault(emptyList())
        val newSeasons = runCatching { StreamingCatalog.recentlyStartedSeasons() }.getOrDefault(emptyList())
        val popular = interleave(shelves.remove("Filmes e séries mais assistidos · Filmes").orEmpty(),
            shelves.remove("Filmes e séries mais assistidos · Séries").orEmpty())
        val best = interleave(shelves.remove("Mais bem avaliados").orEmpty(),
            shelves.remove("Mais bem avaliados · Séries").orEmpty())
        shelves.clear()
        linkedMapOf("Em alta nesta semana" to weekly,
            "Filmes recém-lançados" to recentMovies,
            "Séries novas" to newSeries,
            "Novas temporadas" to newSeasons,
            "Filmes e séries mais assistidos" to popular, "Mais bem avaliados" to best)
            .forEach { (heading, titles) ->
                if (titles.isNotEmpty() || heading in setOf(
                    "Filmes recém-lançados", "Séries novas", "Novas temporadas"))
                    shelves[heading] = titles
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

    LaunchedEffect(selected?.packageName, media, filterGenreIds, allMovieGenres, tvGenres) {
        val app = selected ?: return@LaunchedEffect
        val choices = if (media == "tv") tvGenres else allMovieGenres
        val selectedChoices = choices.filter { kind ->
            kind.id in filterGenreIds || allMovieGenres.any {
                it.name == kind.name && it.id in filterGenreIds
            }
        }.take(8)
        for (kind in selectedChoices) {
            val key = app.name + ":" + media + ":" + kind.id
            if (key !in myServiceShelves) {
                runCatching {
                    StreamingCatalog.discover(token, app.name, media, 1, kind.id, "")
                }.onSuccess { myServiceShelves[key] = it.titles }
            }
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
                // Verified Disney+ Brazil entity for TMDB TV 224505 (Amor da Minha Vida).
                // This is a provider detail request, never a playback-success claim.
                if (title.media == "tv" && title.id == 224505 &&
                    packageName == "com.disney.disneyplus") {
                    val officialLink =
                        "https://www.disneyplus.com/browse/entity-59565a16-d61f-49d1-85d7-1f0f87712748"
                    val result = TclTvAppApi.openTitle(host, pairing, packageName,
                        officialLink, title.name)
                    titleActionFeedback = when (result) {
                        "detail_requested" -> "Ficha oficial de Amor da Minha Vida enviada ao Disney+. " +
                            "Confirme na TV se abriu a série correta."
                        "search" -> "A busca por Amor da Minha Vida foi solicitada no Disney+."
                        "opened_app" -> "Disney+ aberto; pesquise Amor da Minha Vida na TV."
                        else -> "Não foi possível enviar a ficha ao Disney+. Verifique o AnyTV Connect."
                    }
                    return@launch
                }
                // Personal-mode live link source takes priority over sparse Wikidata.
                // It is optional, never baked into the APK and never sent to the TV.
                val personalLookup = runCatching {
                    AnyTvPersonalLinks.resolve(context, title, packageName)
                }
                var linkSourceNotice = ""
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
                    AnyTvPersonalResult.ActivationRequired -> {
                        linkSourceNotice = "Links diretos não autorizados neste celular. "
                    }
                    AnyTvPersonalResult.ResolverUnavailable -> {
                        linkSourceNotice = "Resolvedor temporariamente indisponível. "
                    }
                    AnyTvPersonalResult.NoSupportedLink -> {
                        linkSourceNotice = "Fonte de links sem URL compatível. "
                    }
                    else -> Unit // Preserve the existing verified TMDB/Wikidata fallback.
                }
                // Provider IDs are worldwide; subscription rights are regional.
                // Fail closed when the TMDB BR availability check is negative or unknown.
                val appName = apps.firstOrNull { it.packageName == packageName }?.name
                    ?: "serviço selecionado"
                val offered = StreamingCatalog.titleAvailableInBrazil(
                    token, title, appName)
                if (offered == false) {
                    titleActionFeedback = linkSourceNotice +
                        "Segundo o catálogo TMDB/JustWatch, este título não está " +
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
                titleActionFeedback = linkSourceNotice + (if (offered == null) {
                    "Não foi possível confirmar a disponibilidade no Brasil. " +
                        "Busca convencional solicitada ao streaming; confirme o resultado na TCL."
                } else when (mode) {
                    "detail_requested" -> "Link da ficha enviado ao streaming. Confira o título na TCL."
                    "search" -> "Busca enviada ao aplicativo na TCL. Confira o resultado na TV."
                    "opened_app" -> "Aplicativo aberto na TCL. O AnyTV Connect instalado " +
                        "não conseguiu abrir a ficha exata deste título; faça a busca no serviço."
                    else -> "O aplicativo não pôde ser aberto. Confira se o serviço está instalado " +
                        "e se o AnyTV Connect está vinculado."
                })
            } catch (error: Exception) {
                titleActionFeedback = error.message?.let { "Não foi possível abrir: " + it }
                    ?: "Não foi possível abrir o serviço na TCL."
            } finally {
                titleActionPending = false
            }
        }
    }

    val posterCard: @Composable (StreamingTitle, Modifier, Boolean) -> Unit =
        { title, cardModifier, cinemaSection ->
            AnyTvPosterCard(
                title = title, modifier = cardModifier,
                offerCache = offerCache, apps = displayed,
                favorite = isFavorite(title, "library"),
                marked = if (title.media == "movie")
                    title.id.toString() in watchedFilms
                else title.id.toString() in trackedSeries,
                inCinema = cinemaSection || title.id in cinemaIds,
                genreLabel = anyTvGenreLabels(title, allMovieGenres, tvGenres),
                onOpen = {
                    details = title; detailsPackage = null
                    if (showGlobalSearch) {
                        rememberSuccessfulSearch(globalSearchTerm)
                        showGlobalSearch = false
                    }
                },
                onMarked = {
                    if (title.media == "movie") toggleFilmWatched(title)
                    else toggleSeriesTracking(title)
                },
                onFavorite = { toggleFavorite(title, "library") })
        }

    // A detailed row uses the original interactive poster, without repeating
    // its compact title, genre and provider lines below the artwork.
    val detailedPosterCard: @Composable (StreamingTitle, Modifier) -> Unit =
        { title, cardModifier ->
            AnyTvPosterCard(
                title = title, modifier = cardModifier,
                offerCache = offerCache, apps = displayed,
                favorite = isFavorite(title, "library"),
                marked = if (title.media == "movie")
                    title.id.toString() in watchedFilms
                else title.id.toString() in trackedSeries,
                inCinema = title.id in cinemaIds,
                genreLabel = anyTvGenreLabels(title, allMovieGenres, tvGenres),
                posterOnly = true,
                onOpen = { details = title; detailsPackage = null },
                onMarked = {
                    if (title.media == "movie") toggleFilmWatched(title)
                    else toggleSeriesTracking(title)
                },
                onFavorite = { toggleFavorite(title, "library") })
        }

    val detailedListCard: @Composable (StreamingTitle) -> Unit = { title ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top) {
            detailedPosterCard(title, Modifier.width(112.dp))
            Column(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)) {
                // Line 1: title; line 2: genres; line 3: official badges,
                // year and confirmed BR subscription logos.
                Text(title.name, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2)
                val genres = anyTvGenreLabels(title, allMovieGenres, tvGenres)
                if (genres.isNotBlank()) {
                    Text(genres, fontSize = 11.sp, maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StreamingOfferIcons(title, offerCache, displayed,
                    showRating = false, showYear = true,
                    inCinema = title.media == "movie" && title.id in cinemaIds)
                val summary = title.overview.trim()
                if (summary.isNotBlank()) {
                    Text(summary, fontSize = 12.sp, lineHeight = 15.sp,
                        maxLines = 7, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            val video = runCatching {
                                StreamingCatalog.trailer(title.media, title.id)
                            }.getOrNull()
                            if (video == null) {
                                android.widget.Toast.makeText(context,
                                    "Trailer não disponível.", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse(video)))
                                }.onFailure {
                                    android.widget.Toast.makeText(context,
                                        "Não foi possível abrir o trailer.",
                                        android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)) {
                        Text("▶ Trailer", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(onClick = {
                        details = title; detailsPackage = null
                    }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)) {
                        Text("Mais detalhes", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                AnyTvStreamNavBar(
                    library = homeLibrary && selected == null,
                    section = librarySection,
                    contentType = homeContentType,
                    filtersActive = onlyMyStreaming || filterGenreIds.isNotEmpty() || selectedHomeGenre != 0,
                    onHome = { selected = null; homeLibrary = false
                        homeContentType = "all"; selectedHomeGenre = 0 },
                    onList = { selected = null; homeLibrary = true; librarySection = "saved" },
                    onWatching = { selected = null; homeLibrary = true; librarySection = "watching" },
                    onWatched = { selected = null; homeLibrary = true; librarySection = "watched" },
                    onFilters = { editingMyServices = myServices
                        editingFilterGenres = filterGenreIds
                        showMyStreamingSettings = true },
                    onContentType = { kind ->
                        homeContentType = kind
                        if (kind == "tv" && selectedHomeGenre == ANYTV_CINEMA_FILTER) {
                            selectedHomeGenre = 0 // Cinema is movie-only, not a genre.
                        }
                        if (kind != "all") {
                            media = kind; genre = 0; page = 1; showSaved = false
                        } else media = "movie"
                        homeLibrary = false
                    },
                    onDismiss = onDismiss
                )
                // Home and individual provider use ONE identical catalogue layout.
                    // The same real TV platform shortcuts, compact rather than a large list.
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        val installed = displayed.filter {
                            StreamingCatalog.streaming(it) && it.packageName !in hiddenInstalledServices
                        }
                        val installedKeys = displayed
                            .filter { StreamingCatalog.streaming(it) }
                            .map { anyTvServiceKey(it.name) }.toSet()
                        val extra = visibleCatalogServices.sorted()
                            .distinctBy(::anyTvServiceKey)
                            .filter { anyTvServiceKey(it) !in installedKeys }
                            .map { name -> TvOfficialApp("catalog:" + name, name, "") }
                        (installed + extra).distinctBy { anyTvServiceKey(it.name) }.forEach { app ->
                            val selectedHere = selected?.packageName == app.packageName
                            Column(modifier = Modifier.width(62.dp).clickable {
                                selected = app; providerListMode = false
                                media = "movie"; genre = 0; page = 1
                                homeLibrary = false; showGlobalSearch = false
                                searchDraft = ""; searchTerm = ""; showSaved = false; message = null
                            }.semantics { contentDescription = "Explorar " + app.name },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Box(modifier = Modifier.size(59.dp)
                                    .border(if (selectedHere) 2.dp else 0.dp,
                                        if (selectedHere) Color(0xFFE3383F) else Color.Transparent,
                                        RoundedCornerShape(15.dp))
                                    .padding(3.dp), contentAlignment = Alignment.Center) {
                                    if (app.packageName.startsWith("catalog:")) {
                                        val logo = tmdbProviderArtwork[anyTvServiceKey(app.name)].orEmpty()
                                        Box(modifier = Modifier.fillMaxSize()
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Color(0xFF1C1E25)),
                                            contentAlignment = Alignment.Center) {
                                            if (logo.startsWith("/")) {
                                                AnyTvPinnedImage(
                                                    model = "https://image.tmdb.org/t/p/w185" + logo,
                                                    contentDescription = "Streaming " + app.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit)
                                            } else {
                                                Text(app.name.take(2).uppercase(), fontSize = 16.sp,
                                                    color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else OfficialTvIcon(app, 52.dp)
                                }
                                Text(app.name, fontSize = 10.sp, maxLines = 1,
                                    color = Color.White, lineHeight = 11.sp,
                                    overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Column(modifier = Modifier.width(62.dp).clickable {
                            showCatalogEditor = true
                        }, horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Box(modifier = Modifier.size(56.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFF1C1E25)),
                                contentAlignment = Alignment.Center) {
                                Text("Editar", color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Streamings", fontSize = 10.sp, maxLines = 1, color = Color.White)
                        }
                    }
                    // Search, content type and genres share one compact line.
                    val selectedGenreKind = if (homeContentType == "tv") "tv" else "movie"
                    val currentGenreList = if (selectedGenreKind == "tv") tvGenres else mainGenres
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showGlobalSearch = !showGlobalSearch },
                            modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar filmes e séries",
                                modifier = Modifier.size(20.dp))
                        }
                        AnyTvCompactChip("Todos", selectedHomeGenre == 0) {
                            selectedHomeGenre = 0; homeLibrary = false
                        }
                        AnyTvCompactChip("Anime", selectedHomeGenre == ANYTV_ANIME_FILTER) {
                            selectedHomeGenre = ANYTV_ANIME_FILTER; homeLibrary = false
                        }
                        if (homeContentType != "tv") {
                            AnyTvCompactChip("Cinema", selectedHomeGenre == ANYTV_CINEMA_FILTER) {
                                selected = null; homeContentType = "movie"
                                selectedHomeGenre = ANYTV_CINEMA_FILTER; homeLibrary = false
                            }
                        }
                        currentGenreList.forEach { kind ->
                            val selectedKindId = anyTvResolvedGenre(selectedHomeGenre,
                                selectedGenreKind, allMovieGenres, tvGenres)
                            AnyTvCompactChip(kind.name, selectedKindId == kind.id) {
                                selectedHomeGenre = kind.id; homeLibrary = false
                            }
                        }
                        TextButton(onClick = {
                            editingGenreIds = preferredGenreIds; showGenreSettings = true
                        }, contentPadding = PaddingValues(horizontal = 5.dp)) {
                            Text("⚙ Listas", fontSize = 11.sp)
                        }
                    }
                    val currentGenreKey = "$selectedGenreKind:$selectedHomeGenre:$genreSort"
                    // A failed first-page request must not leave an endless spinner.
                    if (discoveryLoading || (selectedHomeGenre != 0 &&
                            genreLoading[currentGenreKey] == true)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    }
                    if (discoveryError != null && selectedHomeGenre == 0) {
                        Text(discoveryError.orEmpty(), fontSize = 11.sp)
                    }
                    val visibleShelves = linkedMapOf<String, List<StreamingTitle>>().apply {
                        if (selected != null && !homeLibrary) {
                            val app = selected!!
                            val kinds = listOf("movie", "tv").filter {
                                homeContentType == "all" || it == homeContentType
                            }
                            kinds.forEach { kind ->
                                val chosenGenre = anyTvProviderGenre(kind, selectedHomeGenre, allMovieGenres, tvGenres)
                                if (selectedHomeGenre > 0 && chosenGenre == 0) return@forEach
                                val key = app.name + ":" + kind + ":" + chosenGenre
                                myServiceShelves[key]?.takeIf { it.isNotEmpty() }?.let {
                                    put(if (kind == "tv") "Séries · " + app.name
                                        else "Filmes · " + app.name, it)
                                }
                            }
                            if (selectedHomeGenre == 0 && titles.isNotEmpty() &&
                                (homeContentType == "all" || homeContentType == media)) {
                                put("Em destaque · " + app.name, titles)
                            }
                            val possibleGenres = if (homeContentType == "tv") tvGenres
                                else allMovieGenres
                            possibleGenres.filter { kind ->
                                selectedHomeGenre == 0 && kind.id in filterGenreIds
                            }.forEach { kind ->
                                myServiceShelves[app.name + ":" + media + ":" + kind.id]
                                    ?.takeIf { it.isNotEmpty() }?.let {
                                        put("Por gênero: " + kind.name, it)
                                    }
                            }
                        } else if (homeLibrary) {
                            val films = favorites.filter { it.title.media == "movie" }
                                .map { it.title }.distinctBy { it.id }
                            val series = favorites.filter { it.title.media == "tv" }
                                .map { it.title }.distinctBy { it.id }
                            val unseen = films.filter { it.id.toString() !in watchedFilms }
                            val history = (watchedFilms.mapNotNull { key ->
                                key.toIntOrNull()?.let { restoreTrackedTitle("movie", it) }
                            } + watchedEpisodes.mapNotNull { key ->
                                key.substringBefore(":").toIntOrNull()?.let {
                                    restoreTrackedTitle("tv", it)
                                }
                            }).distinctBy { "${it.media}:${it.id}" }
                            val trackedIds = trackedSeries +
                                watchedEpisodes.mapNotNull { it.substringBefore(":").takeIf(String::isNotBlank) }
                            val watching = trackedIds.mapNotNull { key ->
                                key.toIntOrNull()?.let { restoreTrackedTitle("tv", it) }
                            }.distinctBy { it.id }
                                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                            if (librarySection == "saved") {
                                if (unseen.isNotEmpty()) put("Filmes para assistir", unseen)
                                if (series.isNotEmpty()) put("Minhas séries", series)
                            } else if (librarySection == "watching") {
                                if (watching.isNotEmpty()) put("Séries acompanhadas", watching)
                            } else if (librarySection == "watched") {
                                if (history.isNotEmpty()) put("Filmes e séries assistidos", history)
                            }
                        } else {
                            if (selectedHomeGenre == 0) {
                                discoveryShelves.forEach { (heading, titles) ->
                                    if (heading != ANYTV_CINEMA_SHELF) {
                                        val selectedTitles = titles.filter {
                                            (homeContentType == "all" || it.media == homeContentType) &&
                                            anyTvReleased(it)
                                        }
                                        if (selectedTitles.isNotEmpty()) put(heading, selectedTitles)
                                    }
                                }

                            } else {
                                if (selectedHomeGenre == ANYTV_CINEMA_FILTER) {
                                    genreShelves["movie:$ANYTV_CINEMA_FILTER:$genreSort"]
                                        ?.takeIf { it.isNotEmpty() }?.let {
                                            put("Cinema · Em cartaz no Brasil", it)
                                        }
                                }
                                val kind = (allMovieGenres + tvGenres).firstOrNull {
                                    it.id == selectedHomeGenre
                                }
                                listOf("movie", "tv").filter {
                                    homeContentType == "all" || homeContentType == it
                                }.forEach { mediaKind ->
                                    val resolved = if (selectedHomeGenre == ANYTV_ANIME_FILTER)
                                        ANYTV_ANIME_FILTER else anyTvResolvedGenre(selectedHomeGenre,
                                        mediaKind, allMovieGenres, tvGenres)
                                    if (resolved > 0 || resolved == ANYTV_ANIME_FILTER) {
                                        genreShelves["$mediaKind:$resolved:$genreSort"]
                                            ?.takeIf { it.isNotEmpty() }?.let {
                                                put((if (mediaKind == "tv") "Séries" else "Filmes") +
                                                    " · " + (if (selectedHomeGenre == ANYTV_ANIME_FILTER)
                                                        "Anime" else kind?.name ?: "Gênero"), it)
                                            }
                                    }
                                }
                            }
                        }
                        if (!homeLibrary && selected == null && selectedHomeGenre == 0) {
                            val future = upcomingStreaming.filter {
                                homeContentType == "all" || it.media == homeContentType
                            }
                            // Even if page one has no BR offers, later source pages can.
                            if (future.isNotEmpty() || upcomingStreamingMore)
                                put("Em breve · Filmes e séries", future)
                                filterGenreIds.forEach { id ->
                                    val label = (allMovieGenres + tvGenres).firstOrNull {
                                        it.id == id
                                    }?.name ?: return@forEach
                                    val mediaKinds = listOf("movie", "tv").filter {
                                        homeContentType == "all" || homeContentType == it
                                    }
                                    val mixedGenre = mediaKinds.flatMap { mediaKind ->
                                        val resolved = anyTvResolvedGenre(
                                            id, mediaKind, allMovieGenres, tvGenres)
                                        if (resolved > 0) genreShelves["$mediaKind:$resolved"].orEmpty()
                                        else emptyList()
                                    }.distinctBy { "${it.media}:${it.id}" }
                                    if (mixedGenre.isNotEmpty()) put("Por gênero: $label", mixedGenre)
                                }
                        }
                        if (!homeLibrary && selected == null) {
                            activeServiceNames.forEach { name ->
                                val kinds = listOf("movie", "tv").filter {
                                    homeContentType == "all" || it == homeContentType
                                }
                                kinds.forEach { kind ->
                                    val chosenGenre = anyTvProviderGenre(kind, selectedHomeGenre, allMovieGenres, tvGenres)
                                    if (selectedHomeGenre > 0 && chosenGenre == 0) return@forEach
                                    val key = name + ":" + kind + ":" + chosenGenre
                                    myServiceShelves[key]?.takeIf { it.isNotEmpty() }?.let {
                                        put("Destaques " + name +
                                            if (kind == "tv") " · Séries" else " · Filmes", it)
                                    }
                                }
                            }
                        }
                    }
                    if (selectedHomeGenre != 0 &&
                        "$selectedGenreKind:$selectedHomeGenre:$genreSort" in genreShelves &&
                        visibleShelves.isEmpty()) {
                        Text("Não encontramos títulos neste gênero no momento.", fontSize = 12.sp)
                    }
                    val animeSeparatedShelves = linkedMapOf<String, List<StreamingTitle>>().apply {
                        visibleShelves.forEach { (heading, titles) ->
                            put(heading, titles.filter { title ->
                                if (selectedHomeGenre == ANYTV_ANIME_FILTER) anyTvIsAnime(title)
                                else !anyTvIsAnime(title)
                            })
                        }
                    }
                    val filteredShelves = if (activeServiceNames.isEmpty() || homeLibrary ||
                        selected != null) animeSeparatedShelves else
                        linkedMapOf<String, List<StreamingTitle>>().apply {
                            animeSeparatedShelves.forEach { (heading, candidates) ->
                                val confirmed = candidates.filter { item ->
                                    val offers = offerCache["${item.media}:${item.id}"].orEmpty()
                                    StreamingCatalog.matchesSelectedSubscription(
                                        offers.firstOrNull { offer ->
                                            StreamingCatalog.matchesSelectedSubscription(
                                                offer, activeServiceNames)
                                        } ?: StreamingOffer(-1, "", ""), activeServiceNames)
                                }
                                // Preserve editorial section headings while offer checks are
                                // resolving. Never substitute unverified streaming titles.
                                put(heading, confirmed)
                            }
                        }
                    val heroTitles = if (homeLibrary) emptyList() else {
                        val mixed = if (selected == null && selectedHomeGenre == 0 &&
                            homeContentType == "all" && activeServiceNames.isEmpty()) {
                            val films = discoveryShelves["Em alta nesta semana"].orEmpty()
                                .filter { it.media == "movie" }
                            val shows = discoveryShelves["Em alta nesta semana"].orEmpty()
                                .filter { it.media == "tv" }
                            buildList {
                                for (i in 0 until 10) {
                                    films.getOrNull(i)?.let { add(it) }
                                    shows.getOrNull(i)?.let { add(it) }
                                }
                            }
                        } else filteredShelves.values.flatMap { it }
                        mixed.distinctBy { "${it.media}:${it.id}" }.filter {
                            if (selectedHomeGenre == ANYTV_ANIME_FILTER) anyTvIsAnime(it)
                            else !anyTvIsAnime(it)
                        }.filter {
                            if (selectedHomeGenre == ANYTV_CINEMA_FILTER)
                                it.media == "movie" // Genre source is now_playing BR
                            else anyTvReleased(it) &&
                                (it.media != "movie" || it.id !in cinemaIds)
                        }.filterNot {
                            it.media == "movie" && it.id.toString() in watchedFilms
                        }.take(10)
                    }
                    val uniqueShelves = filteredShelves.mapNotNull { (heading, shelf) ->
                        // Keep cards visible when marked so the green check reflects
                        // the new state immediately; Assistidos keeps its own view.
                        val unique = shelf.filter { title ->
                            when {
                                homeLibrary -> true // Saved/watched records survive missing TMDB dates.
                                heading == ANYTV_CINEMA_SHELF ->
                                    title.media == "movie" // Source is now_playing BR
                                heading.startsWith("Em breve") -> anyTvUpcoming(title)
                                heading in setOf("Filmes recém-lançados", "Séries novas") ->
                                    anyTvRecentPremiere(title) && (title.media != "movie" || title.id !in cinemaIds)
                                heading == "Novas temporadas" -> title.media == "tv"
                                else -> anyTvReleased(title) &&
                                    (title.media != "movie" || title.id !in cinemaIds)
                            }
                        }.distinctBy { "${it.media}:${it.id}" }
                        if (unique.isEmpty() &&
                            heading !in setOf("Filmes recém-lançados", "Séries novas", "Novas temporadas") &&
                            !(heading == "Em breve · Filmes e séries" && upcomingStreamingMore) &&
                            !(activeServiceNames.isNotEmpty() && selected == null &&
                                !homeLibrary && selectedHomeGenre == 0))
                            null
                        else heading to (if (homeLibrary) unique.filter {
                            libraryQuery.isBlank() ||
                                it.name.contains(libraryQuery.trim(), ignoreCase = true)
                        } else unique.take(ANYTV_LIST_BATCH))
                    }
                    LazyColumn(state = catalogScrollState,
                        modifier = Modifier.weight(1f)) {
                        if (openedSection != null) {
                            val sectionTitle = openedSection.orEmpty()
                            // The home rail shows only 24, but the source page can
                            // already have more verified titles. Never skip that
                            // overflow when opening its dedicated section.
                            val sectionBase = if (sectionTitle == "Em breve · Filmes e séries")
                                visibleShelves[sectionTitle].orEmpty()
                            else uniqueShelves.firstOrNull {
                                it.first == sectionTitle
                            }?.second.orEmpty()
                            val sectionAll = (sectionBase + sectionExtra)
                                .distinctBy { "${it.media}:${it.id}" }
                            val sectionTitles = sectionAll.take(sectionVisibleCount)
                            val sectionPageLimit = if (sectionTitle == "Em breve · Filmes e séries") 50 else 42
                            val editorialRoute = when (sectionTitle) {
                                "Em alta nesta semana" -> "trending/movie/week" to "movie"
                                "Mais bem avaliados" -> "movie/top_rated" to "movie"
                                "Séries em destaque" -> "trending/tv/week" to "tv"
                                "Filmes e séries mais assistidos" -> "movie/popular" to "movie"
                                else -> null
                            }
                            val selectedProvider = selected?.let { app ->
                                when (sectionTitle) {
                                    "Filmes · ${app.name}" -> app to "movie"
                                    "Séries · ${app.name}" -> app to "tv"
                                    "Em destaque · ${app.name}" -> app to media
                                    else -> null
                                }
                            }
                            // A provider-specific genre rail must remain within
                            // that provider on Ver mais and every subsequent page.
                            val selectedGenreProvider = selected?.takeIf {
                                sectionTitle.startsWith("Por gênero: ")
                            }?.let { app ->
                                val sourceGenres = if (media == "tv") tvGenres else allMovieGenres
                                sourceGenres.firstOrNull {
                                    it.name == sectionTitle.removePrefix("Por gênero: ")
                                }?.let { genreItem ->
                                    Triple(app.name, media, genreItem.id)
                                }
                            }
                            val homeProvider = activeServiceNames.firstOrNull { name ->
                                sectionTitle == "Destaques $name · Filmes" ||
                                    sectionTitle == "Destaques $name · Séries"
                            }
                            val sectionCanPage = editorialRoute != null ||
                                selectedProvider != null || selectedGenreProvider != null ||
                                homeProvider != null || sectionTitle == "Em breve · Filmes e séries"
                            item {
                                TextButton(onClick = { openedSection = null }) {
                                    Text("‹ Voltar às seções")
                                }
                                Text(sectionTitle, fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold)
                            }
                            items(sectionTitles.chunked(3)) { group ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                    verticalAlignment = Alignment.Top) {
                                    group.forEach { title ->
                                        posterCard(title, Modifier.weight(1f),
                                            sectionTitle == ANYTV_CINEMA_SHELF)
                                    }
                                    repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            if (sectionTitle == "Em breve · Filmes e séries" && sectionAll.isEmpty()) {
                                item {
                                    Text(if (sectionMore)
                                        "Nenhum lançamento futuro com oferta BR confirmada nesta seleção. " +
                                            "Mostrar mais consulta as próximas páginas."
                                    else "Nenhum lançamento futuro com assinatura BR confirmada.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp)
                                }
                            }
                            if (sectionAll.size > sectionVisibleCount ||
                                (!homeLibrary && sectionCanPage && sectionMore &&
                                    sectionPage < sectionPageLimit &&
                                    (sectionAll.isNotEmpty() ||
                                        sectionTitle == "Em breve · Filmes e séries"))) {
                                item {
                                    OutlinedButton(
                                        enabled = !sectionLoading,
                                        onClick = {
                                            if (sectionAll.size > sectionVisibleCount) {
                                                sectionVisibleCount += ANYTV_LIST_BATCH
                                            } else if (!sectionLoading && sectionCanPage &&
                                                sectionPage < sectionPageLimit) {
                                                sectionLoading = true
                                                sectionError = null
                                                val next = sectionPage + 1
                                                scope.launch {
                                                    runCatching {
                                                        when {
                                                            sectionTitle in setOf("Em alta nesta semana",
                                                                "Filmes e séries mais assistidos",
                                                                "Mais bem avaliados") -> {
                                                                val movieRoute = when (sectionTitle) {
                                                                    "Em alta nesta semana" -> "trending/movie/week"
                                                                    "Mais bem avaliados" -> "movie/top_rated"
                                                                    else -> "movie/popular"
                                                                }
                                                                val seriesRoute = when (sectionTitle) {
                                                                    "Em alta nesta semana" -> "trending/tv/week"
                                                                    "Mais bem avaliados" -> "tv/top_rated"
                                                                    else -> "tv/popular"
                                                                }
                                                                val moviePage = StreamingCatalog.discoveryShelfPage(
                                                                    movieRoute, "movie", next)
                                                                val seriesPage = StreamingCatalog.discoveryShelfPage(
                                                                    seriesRoute, "tv", next)
                                                                val mixed = buildList {
                                                                    for (idx in 0 until maxOf(
                                                                            moviePage.titles.size,
                                                                            seriesPage.titles.size)) {
                                                                        moviePage.titles.getOrNull(idx)?.let { add(it) }
                                                                        seriesPage.titles.getOrNull(idx)?.let { add(it) }
                                                                    }
                                                                }
                                                                StreamingResults(mixed,
                                                                    moviePage.more || seriesPage.more)
                                                            }
                                                            editorialRoute != null ->
                                                                StreamingCatalog.discoveryShelfPage(
                                                                    editorialRoute.first,
                                                                    editorialRoute.second, next)
                                                            sectionTitle == "Em breve · Filmes e séries" ->
                                                                StreamingCatalog.discoveryUpcomingStreamingPage(next)
                                                            selectedProvider != null ->
                                                                StreamingCatalog.discover(
                                                                    token, selectedProvider.first.name,
                                                                    selectedProvider.second, next,
                                                                    if (sectionTitle.startsWith(
                                                                            "Em destaque ·")) genre
                                                                    else anyTvProviderGenre(selectedProvider.second,
                                                                        selectedHomeGenre, allMovieGenres, tvGenres),
                                                                    if (sectionTitle.startsWith(
                                                                            "Em destaque ·")) searchTerm else "")
                                                            selectedGenreProvider != null ->
                                                                StreamingCatalog.discover(token,
                                                                    selectedGenreProvider.first,
                                                                    selectedGenreProvider.second, next,
                                                                    selectedGenreProvider.third, "")
                                                            homeProvider != null -> {
                                                                // Mirror the initial home shelf: its TV
                                                                // source is unfiltered, even in movie genre.
                                                                val homeKind = if (sectionTitle.endsWith("· Séries"))
                                                                    "tv" else "movie"
                                                                val homeGenre = anyTvProviderGenre(
                                                                    homeKind, selectedHomeGenre, allMovieGenres, tvGenres)
                                                                StreamingCatalog.discover(token,
                                                                    homeProvider, homeKind, next,
                                                                    homeGenre, "")
                                                            }
                                                            else -> StreamingResults(emptyList(), false)
                                                        }
                                                    }.onSuccess { result ->
                                                        if (openedSection != sectionTitle) return@onSuccess
                                                        val matching = result.titles.filter {
                                                            if (sectionTitle == "Em breve · Filmes e séries")
                                                                anyTvUpcoming(it) &&
                                                                    (homeContentType == "all" ||
                                                                        it.media == homeContentType)
                                                            else (if (sectionTitle in setOf("Filmes recém-lançados", "Séries novas"))
                                                                anyTvRecentPremiere(it) else anyTvReleased(it)) &&
                                                                (it.media != "movie" ||
                                                                    it.id !in cinemaIds)
                                                        }
                                                        sectionExtra = (sectionExtra + matching)
                                                            .distinctBy { "${it.media}:${it.id}" }
                                                        sectionPage = next
                                                        sectionMore = result.more
                                                        sectionVisibleCount += ANYTV_LIST_BATCH
                                                    }.onFailure {
                                                        if (openedSection == sectionTitle)
                                                            sectionError = "Não foi possível carregar mais títulos."
                                                    }
                                                    if (openedSection == sectionTitle)
                                                        sectionLoading = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (sectionLoading) "Carregando…"
                                            else "Mostrar mais")
                                    }
                                }
                            }
                            sectionError?.let { error ->
                                item {
                                    Text(error, color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp)
                                }
                            }
                        } else {
                        if (!homeLibrary) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Box(modifier = Modifier.width(4.dp).height(24.dp)
                                        .background(Color(0xFFE3383F), RoundedCornerShape(3.dp)))
                                    val pageTitle = when {
                                        selected != null -> selected!!.name
                                        selectedHomeGenre == ANYTV_CINEMA_FILTER -> "Cinema"
                                        selectedHomeGenre == ANYTV_ANIME_FILTER -> "Anime"
                                        selectedHomeGenre > 0 -> (allMovieGenres + tvGenres).firstOrNull {
                                            it.id == selectedHomeGenre
                                        }?.name ?: "Gênero"
                                        else -> "Principal"
                                    }
                                    Text(pageTitle, modifier = Modifier.weight(1f),
                                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                    if (selected != null || (selectedHomeGenre != 0 &&
                                            selectedHomeGenre != ANYTV_CINEMA_FILTER && !homeLibrary)) {
                                        OutlinedButton(onClick = {
                                            if (selected != null) providerListMode = !providerListMode
                                            else genreSectionsMode = !genreSectionsMode
                                        }, shape = RoundedCornerShape(11.dp),
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp,
                                                vertical = 0.dp)) {
                                            Text(if (if (selected != null) providerListMode else genreSectionsMode)
                                                "Ver em seções" else "Ver em lista", fontSize = 12.sp,
                                                maxLines = 1)
                                        }
                                    }
                                }

                            }
                        }
                        if (homeLibrary) {
                            item {
                                Text(when (librarySection) {
                                    "watching" -> "Séries acompanhadas"
                                    "watched" -> "Filmes e séries assistidos"
                                    else -> "Minha lista"
                                }, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(top = 8.dp).height(44.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Search,
                                        contentDescription = "Buscar",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(19.dp))
                                    BasicTextField(
                                        value = libraryQuery,
                                        onValueChange = { value ->
                                            libraryQuery = value.take(90)
                                            watchingQuery = libraryQuery
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                if (libraryQuery.isEmpty()) {
                                                    Text("Buscar nesta lista",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 13.sp)
                                                }
                                                innerTextField()
                                            }
                                        })
                                    IconButton(onClick = {
                                        libraryQuery = ""
                                        watchingQuery = ""
                                    }, enabled = libraryQuery.isNotEmpty(),
                                        modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Filled.Close,
                                            contentDescription = "Limpar busca",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = if (libraryQuery.isNotEmpty()) 1f else 0.45f),
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (visibleShelves.isEmpty()) {
                                    Spacer(Modifier.height(16.dp))
                                    Text(when (librarySection) {
                                        "watching" -> if (watchingRefreshing) "Recuperando séries acompanhadas…"
                                            else "Você ainda não acompanha nenhuma série."
                                        "watched" -> if (watchedFilms.any {
                                                it.toIntOrNull()?.let { id -> id > 0 } == true
                                            }) "Os filmes marcados como assistidos estão salvos, " +
                                                "mas os cartazes ainda não foram recuperados."
                                            else "Nenhum filme marcado como assistido."
                                        else -> "Sua lista está vazia. Salve filmes e séries para vê-los aqui."
                                    }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp)
                                }
                            }
                        }
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
                                        Box(modifier = Modifier.fillMaxWidth().height(210.dp)
                                            .clip(RoundedCornerShape(12.dp))) {
                                            AnyTvPinnedImage(
                                                model = "https://image.tmdb.org/t/p/w780" +
                                                    current.backdrop.ifBlank { current.poster },
                                                contentDescription = "Destaque: " + current.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize())
                                            Box(modifier = Modifier.fillMaxSize().background(
                                                Brush.verticalGradient(listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.15f),
                                                    Color.Black.copy(alpha = 0.86f)
                                                ))))
                                            Box(modifier = Modifier.align(Alignment.TopStart)
                                                .padding(top = 7.dp, start = 8.dp)) {
                                                AnyTvRatingOverlay(current)
                                            }
                                            Column(modifier = Modifier.align(Alignment.BottomStart)
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 12.dp, bottom = 22.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text(current.name, fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp, lineHeight = 19.sp,
                                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                                    color = Color.White,
                                                    style = TextStyle(shadow = Shadow(
                                                        color = Color.Black.copy(alpha = 0.94f),
                                                        offset = Offset(0f, 2f), blurRadius = 6f)))
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically) {
                                                    StreamingOfferIcons(current, offerCache,
                                                        displayed, showRating = false, showYear = true,
                                                        inCinema = current.id in cinemaIds)
                                                    if (current.media == "movie" &&
                                                        current.id in cinemaIds) {
                                                        Text("Cinema", modifier = Modifier
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(Color(0xFF1769E0))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                                            fontSize = 9.sp, color = Color.White,
                                                            fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                                val heroGenres = anyTvGenreLabels(
                                                    current, allMovieGenres, tvGenres)
                                                if (heroGenres.isNotBlank()) {
                                                    Text(heroGenres, fontSize = 11.sp,
                                                        color = Color.White, maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = TextStyle(shadow = Shadow(
                                                            color = Color.Black,
                                                            offset = Offset(0f, 2f), blurRadius = 5f)))
                                                }
                                            }
                                            // Minimal progress bars; no large text dots on artwork.
                                            Row(modifier = Modifier.align(Alignment.BottomCenter)
                                                .padding(bottom = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                repeat(heroTitles.size) { dot ->
                                                    Box(modifier = Modifier
                                                        .width(if (pagerState.currentPage == dot)
                                                            12.dp else 4.dp)
                                                        .height(3.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(if (pagerState.currentPage == dot)
                                                            Color.White else
                                                            Color.White.copy(alpha = 0.45f)))
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }
                        // Provider list uses the same hero, poster cards and detail/list modes
                        // as genres, but fetches ONLY the selected provider's BR flatrate feed.
                        if (!homeLibrary && selected != null && providerListMode) {
                            val app = selected!!
                            val sources = listOf("movie", "tv").filter {
                                homeContentType == "all" || it == homeContentType
                            }.mapNotNull { kind ->
                                val id = anyTvProviderGenre(kind, selectedHomeGenre,
                                    allMovieGenres, tvGenres)
                                if (selectedHomeGenre > 0 && id == 0) null
                                else Triple("${app.name}:$kind:$id", kind, id)
                            }
                            val films = sources.firstOrNull { it.second == "movie" }?.let {
                                providerListTitles[it.first].orEmpty()
                            }.orEmpty()
                            val shows = sources.firstOrNull { it.second == "tv" }?.let {
                                providerListTitles[it.first].orEmpty()
                            }.orEmpty()
                            val combined = buildList {
                                for (index in 0 until maxOf(films.size, shows.size)) {
                                    films.getOrNull(index)?.let { add(it) }
                                    shows.getOrNull(index)?.let { add(it) }
                                }
                            }.distinctBy { "${it.media}:${it.id}" }.filter { title ->
                                if (selectedHomeGenre == ANYTV_ANIME_FILTER) anyTvIsAnime(title)
                                else !anyTvIsAnime(title)
                            }.let { results ->
                                when (genreSort) {
                                    "rating" -> results.sortedByDescending { it.rating }
                                    "recent" -> results.sortedByDescending { it.date }
                                    "title" -> results.sortedBy {
                                        it.name.lowercase(java.util.Locale.ROOT)
                                    }
                                    else -> results
                                }
                            }
                            item {
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.width(3.dp).height(22.dp)
                                        .background(Color(0xFFE3383F),
                                            RoundedCornerShape(3.dp)))
                                    Text("Catálogo · " + app.name,
                                        fontWeight = FontWeight.SemiBold)
                                }
                                Row(modifier = Modifier.fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AnyTvGenreViewIcon(Icons.Filled.GridView, "Grade",
                                        genreViewMode == "grade") { genreViewMode = "grade" }
                                    AnyTvGenreViewIcon(Icons.Filled.ViewList, "Detalhado",
                                        genreViewMode == "detalhado") {
                                        genreViewMode = "detalhado"
                                    }
                                    Box {
                                        val sortLabel = when (genreSort) {
                                            "rating" -> "Melhor nota"
                                            "recent" -> "Mais recentes"
                                            "title" -> "Título A–Z"
                                            else -> "Mais populares"
                                        }
                                        AnyTvCompactChip("Ordenar: $sortLabel ▾", false) {
                                            genreSortMenu = true
                                        }
                                        DropdownMenu(expanded = genreSortMenu,
                                            onDismissRequest = { genreSortMenu = false }) {
                                            listOf("popular" to "Mais populares",
                                                "rating" to "Melhor nota",
                                                "recent" to "Mais recentes",
                                                "title" to "Título A–Z").forEach { (mode, label) ->
                                                DropdownMenuItem(text = { Text(label) },
                                                    onClick = {
                                                        genreSort = mode
                                                        genreSortMenu = false
                                                        prefs.edit().putString(
                                                            "anytv_genre_sort", mode).apply()
                                                    })
                                            }
                                        }
                                    }
                                }
                            }
                            if (genreViewMode == "detalhado") {
                                items(combined) { title -> detailedListCard(title) }
                            } else {
                                items(combined.chunked(3)) { group ->
                                    Row(modifier = Modifier.fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        group.forEach { title ->
                                            posterCard(title, Modifier.weight(1f), false)
                                        }
                                        repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                            item {
                                if (sources.any { providerListLoading[it.first] == true })
                                    Text("Carregando filmes e séries…", fontSize = 12.sp)
                                if (combined.isEmpty() &&
                                    sources.none { providerListLoading[it.first] == true })
                                    Text("Nenhum título encontrado para esta seleção.",
                                        fontSize = 12.sp)
                                sources.forEach { source ->
                                    providerListErrors[source.first]?.let { error ->
                                        Text(error, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (sources.any { providerListMore[it.first] == true }) {
                                    OutlinedButton(modifier = Modifier.fillMaxWidth(),
                                        enabled = sources.none { providerListLoading[it.first] == true },
                                        onClick = {
                                            val requests = sources.filter {
                                                providerListMore[it.first] == true
                                            }
                                            requests.forEach { providerListLoading[it.first] = true }
                                            scope.launch {
                                                requests.forEach { (key, kind, id) ->
                                                    try {
                                                        val next = (providerListPages[key] ?: 1) + 1
                                                        val result = StreamingCatalog.discover(
                                                            token, app.name, kind, next, id, "")
                                                        providerListTitles[key] =
                                                            (providerListTitles[key].orEmpty() +
                                                                result.titles).distinctBy {
                                                                "${it.media}:${it.id}"
                                                            }
                                                        providerListPages[key] = next
                                                        providerListMore[key] = result.more
                                                        providerListErrors.remove(key)
                                                    } catch (error: java.util.concurrent.CancellationException) {
                                                        throw error
                                                    } catch (error: Exception) {
                                                        providerListErrors[key] =
                                                            "Não foi possível carregar mais títulos."
                                                    } finally {
                                                        providerListLoading[key] = false
                                                    }
                                                }
                                            }
                                        }) { Text("Mostrar mais") }
                                }
                            }
                        }
                        // Two single-row preview rails follow the main hero, while
                        // the paginated list below contains only the selected genre.
                        if (!homeLibrary && selected == null && selectedHomeGenre != 0) {
                            val previews = if (selectedHomeGenre == ANYTV_CINEMA_FILTER)
                                cinemaComingSoon.filterNot(::anyTvIsAnime) else upcomingStreaming.filter { candidate ->
                                    if (selectedHomeGenre == ANYTV_ANIME_FILTER) anyTvIsAnime(candidate)
                                    else !anyTvIsAnime(candidate)
                                }.filter { candidate ->
                                    (homeContentType == "all" || candidate.media == homeContentType) &&
                                    (selectedHomeGenre in candidate.genreIds ||
                                        (candidate.media == "tv" && tvGenres.any { tv ->
                                            tv.id in candidate.genreIds && mainGenres.any { film ->
                                                film.id == selectedHomeGenre && film.name == tv.name
                                            }
                                        }))
                                }
                            if (previews.isNotEmpty()) {
                                item {
                                    AnyTvOneRowPreview(
                                        if (selectedHomeGenre == ANYTV_CINEMA_FILTER)
                                            "Em breve · Cinema" else "Em breve · Filmes e séries",
                                        previews, posterCard)
                                }
                            }
                            if (selectedHomeGenre == ANYTV_CINEMA_FILTER &&
                                cinemaWorldwide.isNotEmpty()) {
                                item {
                                    AnyTvOneRowPreview("Cinema pelo mundo · seleção internacional",
                                        cinemaWorldwide, posterCard)
                                }
                            }
                        }
                        // Genre page: ten-image hero above either a detailed vertical list
                        // or three-column vertical grid. The upstream TMDB sort is applied
                        // consistently to page 1 and every user-requested next 24 titles.
                        if (!homeLibrary && selected == null && selectedHomeGenre != 0) {
                            val genreKey = if (selectedHomeGenre == ANYTV_CINEMA_FILTER)
                                "movie:$ANYTV_CINEMA_FILTER:$genreSort"
                            else "$selectedGenreKind:" +
                                (if (selectedHomeGenre == ANYTV_ANIME_FILTER) ANYTV_ANIME_FILTER
                                 else anyTvResolvedGenre(selectedHomeGenre, selectedGenreKind,
                                    allMovieGenres, tvGenres)) + ":$genreSort"
                            val genreMedia = if (selectedHomeGenre == ANYTV_CINEMA_FILTER)
                                listOf("movie") else listOf("movie", "tv").filter {
                                    homeContentType == "all" || it == homeContentType
                                }
                            val genreSources = genreMedia.mapNotNull { mediaKind ->
                                val id = if (selectedHomeGenre == ANYTV_ANIME_FILTER)
                                    ANYTV_ANIME_FILTER else anyTvResolvedGenre(selectedHomeGenre,
                                        mediaKind, allMovieGenres, tvGenres)
                                if (id == 0) null else {
                                    val key = "$mediaKind:$id:$genreSort"
                                    key to genreShelves[key].orEmpty()
                                }
                            }
                            // Interleave movies and series before BR subscription filtering.
                            val genreCandidates = if (selectedHomeGenre == ANYTV_CINEMA_FILTER)
                                genreShelves[genreKey].orEmpty() else buildList {
                                val maxSize = genreSources.maxOfOrNull { it.second.size } ?: 0
                                for (index in 0 until maxSize) {
                                    genreSources.forEach { (_, titles) ->
                                        titles.getOrNull(index)?.let { add(it) }
                                    }
                                }
                            }
                            val genreTitles = genreCandidates
                                .filter { title -> if (selectedHomeGenre == ANYTV_ANIME_FILTER)
                                    anyTvIsAnime(title) else !anyTvIsAnime(title) }
                                .distinctBy { "${it.media}:${it.id}" }.filter { title ->
                                    activeServiceNames.isEmpty() ||
                                        offerCache["${title.media}:${title.id}"].orEmpty().any { offer ->
                                            StreamingCatalog.matchesSelectedSubscription(
                                                offer, activeServiceNames)
                                        }
                                }
                            item {
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.width(3.dp).height(22.dp)
                                        .background(Color(0xFFE3383F), RoundedCornerShape(3.dp)))
                                    Text(if (selectedHomeGenre == ANYTV_CINEMA_FILTER)
                                        "Em cartaz no Brasil"
                                    else (when (homeContentType) {
                                        "movie" -> "Filmes"
                                        "tv" -> "Séries"
                                        else -> "Filmes e séries"
                                    }) + " · " + ((allMovieGenres + tvGenres).firstOrNull {
                                        it.id == selectedHomeGenre }?.name ?:
                                        if (selectedHomeGenre == ANYTV_ANIME_FILTER) "Anime" else "Gênero"),
                                        fontWeight = FontWeight.SemiBold)
                                }
                                Row(modifier = Modifier.fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (selectedHomeGenre != ANYTV_CINEMA_FILTER) {
                                        AnyTvGenreViewIcon(Icons.Filled.GridView, "Grade", genreViewMode == "grade") {
                                            genreViewMode = "grade"
                                        }
                                        AnyTvGenreViewIcon(Icons.Filled.ViewList, "Detalhado", genreViewMode == "detalhado") {
                                            genreViewMode = "detalhado"
                                        }
                                    }
                                    // Preserve the three-column grid or show a detailed list.
                                    if (selectedHomeGenre == ANYTV_CINEMA_FILTER) {
                                        Text("Em cartaz · Brasil · TMDB", fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else Box {
                                        val sortLabel = when (genreSort) {
                                            "rating" -> "Melhor nota"
                                            "recent" -> "Mais recentes"
                                            "title" -> "Título A–Z"
                                            else -> "Mais populares"
                                        }
                                        AnyTvCompactChip("Ordenar: $sortLabel ▾", false) {
                                            genreSortMenu = true
                                        }
                                        DropdownMenu(expanded = genreSortMenu,
                                            onDismissRequest = { genreSortMenu = false }) {
                                            listOf("popular" to "Mais populares",
                                                "rating" to "Melhor nota",
                                                "recent" to "Mais recentes",
                                                "title" to "Título A–Z").forEach { (mode, label) ->
                                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                                    genreSort = mode
                                                    genreSortMenu = false
                                                    prefs.edit().putString("anytv_genre_sort", mode).apply()
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                            if (genreSectionsMode && selectedHomeGenre != ANYTV_CINEMA_FILTER) {
                                // Real editorial rails, reusing the Streaming section component
                                // and exactly the same filtered, deduplicated genre catalogue.
                                val genreName = if (selectedHomeGenre == ANYTV_ANIME_FILTER)
                                    "Anime" else (allMovieGenres + tvGenres).firstOrNull {
                                        it.id == selectedHomeGenre
                                    }?.name ?: "Gênero"
                                val genreSections = linkedMapOf(
                                    "Filmes · $genreName" to genreTitles.filter { it.media == "movie" },
                                    "Séries · $genreName" to genreTitles.filter { it.media == "tv" },
                                    "Lançamentos recentes · $genreName" to genreTitles
                                        .filter(::anyTvRecentPremiere).sortedByDescending { it.date },
                                    "Mais bem avaliados · $genreName" to genreTitles
                                        .filter { it.votes > 0 }.sortedByDescending { it.rating }
                                )
                                genreSections.forEach { (heading, sectionTitles) ->
                                    if (sectionTitles.isNotEmpty()) item {
                                        AnyTvOneRowPreview(heading, sectionTitles, posterCard)
                                    }
                                }
                            } else if (genreViewMode == "detalhado" &&
                                selectedHomeGenre != ANYTV_CINEMA_FILTER) {
                                items(genreTitles) { title -> detailedListCard(title) }
                            } else {
                                items(genreTitles.chunked(3)) { group ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                                        verticalAlignment = Alignment.Top) {
                                        group.forEach { title ->
                                            posterCard(title, Modifier.weight(1f),
                                                selectedHomeGenre == ANYTV_CINEMA_FILTER)
                                        }
                                        repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                            item {
                                if (genreSources.any { (key, _) -> genreMore[key] == true }) {
                                    OutlinedButton(enabled = genreSources.none { (key, _) ->
                                        genreLoading[key] == true },
                                        onClick = {
                                            if (genreSources.none { (key, _) ->
                                                    genreLoading[key] == true }) {
                                                val requests = genreSources.mapNotNull { (key, _) ->
                                                    if (genreMore[key] != true) null else {
                                                        val mediaKind = key.substringBefore(":")
                                                        val id = if (selectedHomeGenre == ANYTV_ANIME_FILTER)
                                                            ANYTV_ANIME_FILTER else anyTvResolvedGenre(
                                                                selectedHomeGenre, mediaKind,
                                                                allMovieGenres, tvGenres)
                                                        Triple(key, mediaKind, id)
                                                    }
                                                }
                                                requests.forEach { (key, _, _) -> genreLoading[key] = true }
                                                scope.launch {
                                                    requests.forEach { (key, mediaKind, id) ->
                                                        val next = (genrePage[key] ?: 1) + 1
                                                        try {
                                                            val result = if (id == ANYTV_CINEMA_FILTER)
                                                                StreamingCatalog.discoveryCinemaPage(next)
                                                            else StreamingCatalog.discoveryGenrePage(
                                                                id, mediaKind, next, genreSort)
                                                            if (id == ANYTV_CINEMA_FILTER)
                                                                cinemaIds = cinemaIds + result.titles.map { it.id }
                                                            genreShelves[key] =
                                                                (genreShelves[key].orEmpty() +
                                                                    result.titles).distinctBy {
                                                                    "${it.media}:${it.id}"
                                                                }
                                                            genrePage[key] = next
                                                            genreMore[key] = result.more
                                                            genreErrors.remove(key)
                                                        } catch (error: java.util.concurrent.CancellationException) {
                                                            throw error
                                                        } catch (error: Exception) {
                                                            genreErrors[key] =
                                                                "Não foi possível carregar mais títulos."
                                                        } finally {
                                                            genreLoading[key] = false
                                                        }
                                                    }
                                                }
                                            }
                                        }, modifier = Modifier.fillMaxWidth()) {
                                        Text(if (genreSources.any { (key, _) -> genreLoading[key] == true })
                                            "Carregando…" else "Mostrar mais",
                                            fontSize = 13.sp)
                                    }
                                }
                                genreErrors[genreKey]?.let { error ->
                                    Text(error, color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp)
                                    if (genreShelves[genreKey] == null &&
                                        genreLoading[genreKey] != true) {
                                        TextButton(onClick = { genreFirstPageRetry++ }) {
                                            Text("Tentar novamente")
                                        }
                                    }
                                }
                            }
                        } else if (homeLibrary && librarySection == "watching") {
                            val inProgress = uniqueShelves.flatMap { it.second }
                                .filter { watchingQuery.isBlank() ||
                                    it.name.contains(watchingQuery.trim(), ignoreCase = true) }
                                .distinctBy { it.id }
                            if (inProgress.isEmpty() && watchingQuery.isNotBlank()) {
                                item {
                                    Text("Nenhuma série corresponde à sua busca.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 16.dp))
                                }
                            }
                            items(inProgress.take(sectionVisibleCount).chunked(3)) { group ->
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                    verticalAlignment = Alignment.Top) {
                                    group.forEach { series ->
                                        posterCard(series, Modifier.weight(1f), false)
                                    }
                                    repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            if (inProgress.size > sectionVisibleCount) {
                                item {
                                    OutlinedButton(onClick = {
                                        sectionVisibleCount += ANYTV_LIST_BATCH
                                    }, modifier = Modifier.fillMaxWidth().height(38.dp),
                                        shape = RoundedCornerShape(20.dp)) {
                                        Text("Mostrar mais")
                                    }
                                }
                            }
                        } else {
                        if (homeLibrary && librarySection == "watched") {
                            // Old releases stored only TMDB IDs for some watched films.
                            // Retain visible evidence even when no real title metadata
                            // is available locally; do not fabricate a poster or name.
                            val missingIds = watchedFilms.mapNotNull(String::toIntOrNull)
                                .distinct().filter { restoreTrackedTitle("movie", it) == null }
                                .sorted()
                            if (restoringWatchedMovies) {
                                item {
                                    Text("Recuperando metadados dos filmes antigos…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp)
                                }
                            }
                            if (missingIds.isNotEmpty()) {
                                item {
                                    Text("${missingIds.size} registros antigos permanecem " +
                                        "marcados como assistidos, mas sem nome/cartaz " +
                                        "disponível neste aparelho. IDs TMDB: " +
                                        missingIds.take(12).joinToString(", ") +
                                        (if (missingIds.size > 12)
                                            " (+${missingIds.size - 12} outros)" else ""),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp)
                                }
                                if (movieDetailRouteUnsupported) {
                                    item {
                                        Text("Recuperação online ainda indisponível. " +
                                            "Seus registros continuam salvos.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp)
                                    }
                                } else if (!restoringWatchedMovies &&
                                    missingIds.any { it !in absentWatchedMovieIds }) {
                                    item {
                                        TextButton(onClick = { watchedMoviesRetry++ }) {
                                            Text("Recuperar mais 12")
                                        }
                                    }
                                }
                            }
                        }
                        uniqueShelves.filterNot { (heading, _) ->
                            // Genre and Anime have their own dedicated list/sections below;
                            // repeating source rails here pollutes their pages.
                            (!homeLibrary && selected == null && selectedHomeGenre != 0) ||
                            (!homeLibrary && selected != null && providerListMode) ||
                            (!homeLibrary && selected == null && selectedHomeGenre == 0 &&
                                heading == ANYTV_CINEMA_SHELF)
                        }.forEach { (heading, shelf) ->
                            item {
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(top = 24.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Row(modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.width(3.dp).height(22.dp)
                                            .background(Color(0xFFE3383F),
                                                RoundedCornerShape(3.dp)))
                                        Text(heading, fontWeight = FontWeight.SemiBold)
                                    }

                                }
                            }
                            if (homeLibrary) {
                                // Saved and watched pages share the exact home card.
                                items(shelf.take(sectionVisibleCount).chunked(3)) { group ->
                                    Row(modifier = Modifier.fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                                        verticalAlignment = Alignment.Top) {
                                        group.forEach { title ->
                                            posterCard(title, Modifier.weight(1f), false)
                                        }
                                        repeat(3 - group.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                                if (shelf.size > sectionVisibleCount) {
                                    item {
                                        OutlinedButton(
                                            onClick = { sectionVisibleCount += ANYTV_LIST_BATCH },
                                            modifier = Modifier.fillMaxWidth().height(38.dp),
                                            shape = RoundedCornerShape(20.dp)
                                        ) { Text("Mostrar mais") }
                                    }
                                }
                            } else if (shelf.isEmpty() && heading in setOf(
                                "Filmes recém-lançados", "Séries novas", "Novas temporadas")) {
                                item {
                                    Text("Nenhum título com data e classificação confirmadas nesta seleção.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else if (shelf.isEmpty() &&
                                activeServiceNames.isNotEmpty() && selected == null) {
                                item {
                                    Text("Nenhum título com oferta BR confirmada nos streamings " +
                                        "selecionados nesta seção.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else if (shelf.isEmpty() && heading == "Em breve · Filmes e séries") {
                                item {
                                    Text("Nenhum lançamento com oferta BR confirmada na primeira página. " +
                                        "Ver mais consulta as próximas páginas.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                // One horizontal shelf, three visible cards; all metadata
                                // and service icons match the vertical grids.
                                item {
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        val cardWidth = (maxWidth - 18.dp) / 3f
                                        LazyRow(horizontalArrangement =
                                            Arrangement.spacedBy(9.dp)) {
                                            items(shelf.take(ANYTV_LIST_BATCH)) { title ->
                                                posterCard(title, Modifier.width(cardWidth),
                                                    heading == ANYTV_CINEMA_SHELF)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                        } // standalone section or main catalogue
                    }
                // Links oficiais são mostrados quando disponíveis; nenhum código é exigido.
                Text("ⓘ Fontes: TMDB / JustWatch",
                    modifier = Modifier.clickable { showCredits = true },
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1)
            }
        }
    }
    // A full-screen opaque search page overlays the catalogue instead of
    // pushing its films and series down. The back/close control leaves it intact.
    if (showGlobalSearch) {
        Dialog(onDismissRequest = { showGlobalSearch = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                color = Color.Black,
                contentColor = Color.White) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Buscar filmes e séries", modifier = Modifier.weight(1f),
                            fontSize = 19.sp, color = Color.White,
                            fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showGlobalSearch = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Fechar busca",
                                tint = Color.White)
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            // Material3 OutlinedTextField has a 56dp intrinsic min height.
                            // Clamping it to 42dp clipped the baseline below the visible field.
                            // BasicTextField keeps the caret, placeholder and typed text centered.
                            BasicTextField(
                                value = globalSearchDraft,
                                onValueChange = { globalSearchDraft = it.take(90) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp,
                                    color = Color.White),
                                cursorBrush = SolidColor(Color(0xFFE3383F)),
                                modifier = Modifier.weight(1f).height(44.dp)
                                    .border(1.dp, Color.DarkGray,
                                        RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    globalSearchTerm = globalSearchDraft.trim()
                                    rememberSuccessfulSearch(globalSearchTerm)
                                }),
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart) {
                                        if (globalSearchDraft.isEmpty()) {
                                            Text("Buscar filme ou série", fontSize = 13.sp,
                                                color = Color.LightGray)
                                        }
                                        innerTextField()
                                    }
                                })
                            IconButton(onClick = {
                                globalSearchTerm = globalSearchDraft.trim()
                                rememberSuccessfulSearch(globalSearchTerm)
                            }, enabled = globalSearchDraft.trim().length >= 3,
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Search, contentDescription = "Executar busca",
                                    modifier = Modifier.size(19.dp))
                            }
                            IconButton(onClick = {
                                globalSearchDraft = ""; globalSearchTerm = ""
                                globalSearchResults = emptyList(); globalSearchError = null
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Limpar texto da busca",
                                    tint = Color.White)
                            }
                        }

                        if (globalSearchDraft.isBlank() && globalSearchHistory.isNotEmpty()) {
                            Text("Buscas recentes", fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, color = Color.White)
                            globalSearchHistory.take(10).forEach { past ->
                                Row(modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        globalSearchDraft = past
                                        globalSearchTerm = past
                                        rememberSuccessfulSearch(past)
                                    }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Search, contentDescription = null,
                                        tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    Text(past, modifier = Modifier.weight(1f)
                                        .padding(horizontal = 12.dp),
                                        fontSize = 13.sp, color = Color.White)
                                    IconButton(onClick = {
                                        globalSearchHistory = globalSearchHistory.filterNot {
                                            it == past
                                        }
                                        prefs.edit().putString("anytv_global_search_history_v1",
                                            JSONArray(globalSearchHistory).toString()).apply()
                                    }) {
                                        Icon(Icons.Filled.Close,
                                            contentDescription = "Remover busca recente",
                                            tint = Color.LightGray)
                                    }
                                }
                            }
                            TextButton(onClick = {
                                globalSearchHistory = emptyList()
                                prefs.edit().remove("anytv_global_search_history_v1").apply()
                            }) { Text("Limpar histórico", color = Color.LightGray) }
                        }
                        if (globalSearchBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                        if (globalSearchTerm.isNotBlank() && !globalSearchBusy) {
                            Text("Resultados: " + globalSearchTerm, fontSize = 11.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                globalSearchResults.chunked(3).forEach { group ->
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                                        verticalAlignment = Alignment.Top) {
                                        group.forEach { found ->
                                            posterCard(found, Modifier.weight(1f), false)
                                        }
                                        repeat(3 - group.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            if (globalSearchError != null) {
                                Text(globalSearchError.orEmpty(), fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error)
                            } else if (globalSearchResults.isEmpty() &&
                                !(globalSearchTerm.contains("amor", ignoreCase = true) &&
                                  globalSearchTerm.contains("vida", ignoreCase = true))) {
                                Text("Nenhum resultado encontrado.", fontSize = 12.sp)
                            }
                            val loveOfYourLifeQuery =
                                (globalSearchTerm.contains("amor", ignoreCase = true) &&
                                    globalSearchTerm.contains("vida", ignoreCase = true)) ||
                                globalSearchTerm.contains("love of your life", ignoreCase = true)
                            if (loveOfYourLifeQuery) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
                                    Text("O Amor da Sua Vida (2026) · Prime Video",
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Ficha oficial confirmada; o catálogo pode usar o título Love of Your Life.",
                                        fontSize = 10.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        TextButton(onClick = {
                                            val link = Uri.parse(
                                                "https://www.primevideo.com/detail/0HM3DRN79M0E7QOBLMYTS41IG0")
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, link)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                            }.onFailure {
                                                globalSearchError = "Não foi possível abrir a ficha oficial."
                                            }
                                        }) { Text("Ver ficha oficial", fontSize = 11.sp) }
                                        val prime = displayed.firstOrNull {
                                            it.packageName == "com.amazon.amazonvideo.livingroom" ||
                                            it.packageName == "com.amazon.avod.thirdpartyclient"
                                        }
                                        if (prime != null) TextButton(onClick = {
                                            if (host.isBlank() || pairing.isBlank()) {
                                                globalSearchError = "Vincule o AnyTV Connect à televisão."
                                            } else scope.launch {
                                                runCatching {
                                                    TclTvAppApi.searchTitle(host, pairing,
                                                        prime.packageName, "Love of Your Life")
                                                }.onSuccess {
                                                    globalSearchError = "Busca enviada ao Prime Video na TV; confira o resultado."
                                                }.onFailure {
                                                    globalSearchError = "A TCL não aceitou a pesquisa; abra o Prime Video."
                                                }
                                            }
                                        }) { Text("Buscar na TCL", fontSize = 11.sp) }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }
    if (showCatalogEditor) {
        var installedDraft by remember(showCatalogEditor) { mutableStateOf(hiddenInstalledServices) }
        var extrasDraft by remember(showCatalogEditor) { mutableStateOf(visibleCatalogServices) }
        val installedServices = displayed.filter { StreamingCatalog.streaming(it) }
        var brazilProviderNames by remember(showCatalogEditor) { mutableStateOf(emptyList<String>()) }
        LaunchedEffect(showCatalogEditor) {
            if (showCatalogEditor) brazilProviderNames =
                runCatching { anyTvBrazilProviderNames() }.getOrDefault(emptyList())
        }
        val installedKeys = installedServices.map { anyTvServiceKey(it.name) }.toSet()
        val otherServices = (brazilProviderNames + extrasDraft)
            .distinctBy(::anyTvServiceKey)
            .filter { anyTvServiceKey(it) !in installedKeys }
        AlertDialog(onDismissRequest = { showCatalogEditor = false },
            title = { Text("Editar streamings") },
            text = {
                Row(modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text("Na minha TV", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        installedServices.forEach { app ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = app.packageName !in installedDraft,
                                    onCheckedChange = { checked ->
                                        installedDraft = if (checked) installedDraft - app.packageName
                                            else installedDraft + app.packageName
                                    })
                                Text(app.name, fontSize = 11.sp, maxLines = 2)
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text("Outros streamings", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        otherServices.forEach { name ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = name in extrasDraft,
                                    onCheckedChange = { checked ->
                                        extrasDraft = if (checked) extrasDraft + name
                                            else extrasDraft - name
                                    })
                                Text(name, fontSize = 11.sp, maxLines = 2)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    hiddenInstalledServices = installedDraft
                    visibleCatalogServices = extrasDraft
                    catalogPrefs.edit()
                        .putString("anytv_catalog_hidden_installed",
                            installedDraft.sorted().joinToString(","))
                        .putString("anytv_catalog_visible_services",
                            extrasDraft.sorted().joinToString(",")).apply()
                    showCatalogEditor = false
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showCatalogEditor = false }) { Text("Cancelar") }
            })
    }
    if (showMyStreamingSettings) {
        val availableGenres = (allMovieGenres + tvGenres).groupBy { it.name }
            .map { (name, items) -> name to items.map { it.id }.toSet() }
        AlertDialog(
            onDismissRequest = { showMyStreamingSettings = false },
            title = { Text("Filtros de descoberta", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Escolha suas assinaturas e os gêneros para descobrir títulos.",
                        fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth().heightIn(max = 370.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(modifier = Modifier.weight(1f).heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState())) {
                            Text("Streamings", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            TextButton(onClick = {
                                editingMyServices = if (subscriptionNames.all {
                                        it in editingMyServices }) emptySet()
                                    else subscriptionNames.toSet()
                            }) { Text(if (subscriptionNames.all { it in editingMyServices })
                                "Desmarcar todos" else "Marcar todos", fontSize = 13.sp,
                                color = Color(0xFFFF5368),
                                fontWeight = FontWeight.Bold) }
                            subscriptionNames.forEach { name ->
                                val legacy = displayed.firstOrNull {
                                    it.name.equals(name, ignoreCase = true)
                                }?.packageName
                                val checked = name in editingMyServices ||
                                    (legacy != null && legacy in editingMyServices)
                                Row(modifier = Modifier.fillMaxWidth().clickable {
                                    editingMyServices = if (checked)
                                        editingMyServices - name - listOfNotNull(legacy).toSet()
                                    else editingMyServices + name
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = checked, onCheckedChange = null,
                                        modifier = Modifier.size(32.dp))
                                    Text(name, fontSize = 11.sp, maxLines = 2)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f).heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState())) {
                            Text("Gêneros", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            val allGenreIds = availableGenres.flatMap { it.second }.toSet()
                            TextButton(onClick = {
                                editingFilterGenres = if (allGenreIds.isNotEmpty() &&
                                    allGenreIds.all { it in editingFilterGenres }) emptySet()
                                    else allGenreIds
                            }) { Text(if (allGenreIds.isNotEmpty() &&
                                allGenreIds.all { it in editingFilterGenres })
                                "Desmarcar todos" else "Marcar todos", fontSize = 13.sp,
                                color = Color(0xFFFF5368),
                                fontWeight = FontWeight.Bold) }
                            availableGenres.forEach { (name, ids) ->
                                val checked = ids.any { it in editingFilterGenres }
                                Row(modifier = Modifier.fillMaxWidth().clickable {
                                    editingFilterGenres = if (checked)
                                        editingFilterGenres - ids else editingFilterGenres + ids
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = checked, onCheckedChange = null,
                                        modifier = Modifier.size(32.dp))
                                    Text(name, fontSize = 11.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(shape = RoundedCornerShape(22.dp), onClick = {
                    myServices = editingMyServices
                    filterGenreIds = editingFilterGenres
                    prefs.edit().putString("anytv_my_streamings",
                        myServices.sorted().joinToString(","))
                        .putString("anytv_discovery_genre_filter",
                            filterGenreIds.sorted().joinToString(",")).apply()
                    onlyMyStreaming = myServices.isNotEmpty()
                    selectedHomeGenre = 0
                    homeLibrary = false
                    showMyStreamingSettings = false
                }) { Text("Aplicar filtros") }
            },
            dismissButton = {
                OutlinedButton(shape = RoundedCornerShape(22.dp), onClick = { showMyStreamingSettings = false }) { Text("Cancelar") }
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
    if (showCredits) {
        AlertDialog(onDismissRequest = { showCredits = false },
            title = { Text("Fontes do catálogo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Disponibilidade no Brasil: TMDB / JustWatch. " +
                        "Este produto usa a API TMDB, mas não é endossado ou certificado pelo TMDB.")
                    Text("Links fornecidos pela Streaming Availability API by Movie of the Night.")
                    Text("www.movieofthenight.com/about/api",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://www.movieofthenight.com/about/api")))
                            }
                        })
                }
            },
            confirmButton = {
                TextButton(onClick = { showCredits = false }) { Text("Fechar") }
            })
    }
    val title = details
    if (title != null) {
        val pkg = detailsPackage ?: selected?.packageName.orEmpty()
        val officialDisneyTitle = title.media == "tv" && title.id == 224505
        AlertDialog(
            onDismissRequest = {
                details = null; detailsPackage = null; titleActionFeedback = null
                linkActivationRequired = false
                deviceLinkStatus = null; deviceIdSuffix = null
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
                        Icon(Icons.Filled.Favorite,
                            contentDescription = if (isFavorite(title, pkg))
                                "Remover da minha lista" else "Adicionar à minha lista",
                            tint = if (isFavorite(title, pkg)) Color(0xFFE04B57)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = {
                        if (title.media == "movie") {
                            if (!isFavorite(title, pkg)) toggleFavorite(title, pkg)
                            toggleFilmWatched(title)
                        } else {
                            if (!isFavorite(title, pkg)) toggleFavorite(title, pkg)
                            toggleSeriesTracking(title)
                        }
                    }, modifier = Modifier.size(36.dp).semantics {
                        contentDescription = if (title.media == "movie")
                            "Marcar como assistido" else "Acompanhar série"
                    }) {
                        val marked = if (title.media == "movie")
                            title.id.toString() in watchedFilms
                        else title.id.toString() in trackedSeries
                        Icon(Icons.Filled.CheckCircle,
                            contentDescription = if (marked) "Assistido ou acompanhando"
                                else "Marcar assistido ou acompanhar",
                            tint = if (marked) Color(0xFF1CB96B)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp))
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
                    TextButton(onClick = {
                        scope.launch {
                            val state = AnyTvPersonalLinks.activationStatus(context)
                            deviceIdSuffix = withContext(Dispatchers.IO) {
                                runCatching {
                                    AnyTvDeviceIdentity.deviceId(context).takeLast(8)
                                }.getOrNull()
                            }
                            linkActivationRequired = state == "unauthorized"
                            deviceLinkStatus = when (state) {
                                "activated" -> "Links autorizados neste celular."
                                "unauthorized" -> "Acesso aos links aguardando liberação."
                                else -> "Não foi possível verificar o acesso aos links."
                            }
                        }
                    }, enabled = !deviceActivationPending,
                        contentPadding = PaddingValues(0.dp)) {
                        Text("Verificar acesso aos links", fontSize = 11.sp)
                    }
                    if (deviceLinkStatus != null) {
                        Text(deviceLinkStatus.orEmpty(), fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Instalação: " + context.packageName + " · " +
                            (deviceIdSuffix ?: "não identificada"),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (linkActivationRequired) {
                        Text("Solicite a liberação segura desta instalação. " +
                            "A solicitação não concede acesso automaticamente.",
                            fontSize = 11.sp)
                        TextButton(enabled = !deviceActivationPending,
                            onClick = {
                                scope.launch {
                                    deviceActivationPending = true
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                AnyTvDeviceIdentity.requestApproval(context)
                                            }.getOrDefault("unavailable")
                                        }
                                        linkActivationRequired = result != "activated"
                                        deviceLinkStatus = when (result) {
                                            "activated" -> "Links autorizados neste celular."
                                            "pending" -> "Solicitação recebida. " +
                                                "Aguarde a liberação e verifique novamente."
                                            else -> "Não foi possível enviar a solicitação. " +
                                                "Tente novamente."
                                        }
                                    } finally {
                                        deviceActivationPending = false
                                    }
                                }
                            }
                        ) { Text(if (deviceActivationPending)
                            "Enviando…" else "Solicitar liberação") }
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
                    if (title.media == "movie" && title.id in cinemaIds) {
                        Text("Em cartaz no Brasil (TMDB). Consulte a programação da sua cidade:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = {
                            // Official movie pages expose venues and sessions per
                            // selected city; this is navigation, not invented showtimes.
                            val slug = Normalizer.normalize(
                                title.name.lowercase(java.util.Locale.ROOT),
                                Normalizer.Form.NFD)
                                .replace(Regex("\\p{Mn}+"), "")
                                .replace(Regex("[^a-z0-9]+"), "-")
                                .trim('-').take(100)
                            val target = if (slug.isNotBlank())
                                "https://www.ingresso.com/filme/" + slug
                            else "https://www.ingresso.com/"
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    Uri.parse(target)))
                            }.onFailure {
                                titleActionFeedback = "Não foi possível abrir a programação dos cinemas."
                            }
                        }) {
                            Text("Ver cinemas e sessões · Ingresso.com", fontSize = 12.sp)
                        }
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
                                    toggleWatched(title, selectedSeriesSeason, episode.number)
                                }, verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Checkbox(checked = marked, onCheckedChange = {
                                        toggleWatched(title, selectedSeriesSeason, episode.number)
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
                                    toggleWatched(title, seasonNum, epNum)
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
                                    TextButton(onClick = { toggleWatched(title, sn, en) }) {
                                        Text("✓ T$sn · E$en   ×", fontSize = 11.sp)
                                    }
                                }
                        }
                    }
                    if (relatedTitles.isNotEmpty()) {
                        Text("Você também pode gostar", fontWeight = FontWeight.SemiBold)
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val cardWidth = (maxWidth - 18.dp) / 3f
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                relatedTitles.take(ANYTV_LIST_BATCH).forEach { suggested ->
                                    posterCard(suggested, Modifier.width(cardWidth), false)
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
                            detailsOffers?.let { StreamingCatalog.appHasOffer(app, it) } == true ||
                                (officialDisneyTitle && app.packageName == "com.disney.disneyplus")
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
                    } else if (!(officialDisneyTitle && pkg == "com.disney.disneyplus") &&
                        detailsOffers?.let { offers ->
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
