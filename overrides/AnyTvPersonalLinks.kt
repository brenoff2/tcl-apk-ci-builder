package com.hari.androidtvremote.ui.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

internal sealed class AnyTvPersonalResult {
    data class Direct(val url: String) : AnyTvPersonalResult()
    data object NotInSubscription : AnyTvPersonalResult()
    data object NoSupportedLink : AnyTvPersonalResult()
}

internal object AnyTvPersonalLinks {
    private val disneyPattern = Regex("/browse/entity-[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}")
    private val primeId = Regex("amzn[0-9]\\.dv\\.gti\\.[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}")
    private val applePath = Regex("/(?:[a-z]{2}/)?(?:movie|show)/(?:[^/]{1,120}/)?umc\\.cmc\\.[a-z0-9]{15,40}")
    private val maxPath = Regex("/(?:movie|show)/[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}")

    internal fun providerForPackage(pkg: String): String? = when (pkg) {
        "com.netflix.ninja", "com.netflix.mediaclient" -> "netflix"
        "com.disney.disneyplus" -> "disney"
        "com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient" -> "prime"
        "com.apple.atve.androidtv.appletv" -> "apple"
        "com.wbd.stream" -> "hbo"
        else -> null
    }

    /** Fail closed on lookalike hosts, redirect queries, unrecognized titles. */
    internal fun validatedLink(provider: String, raw: String): String? {
        if (raw.length !in 25..500) return null
        val uri = try { URI(raw) } catch (_: Exception) { return null }
        if (uri.scheme != "https" || uri.rawUserInfo != null || uri.port != -1 ||
            uri.rawFragment != null) return null
        val path = uri.rawPath ?: return null
        val host = uri.host ?: return null
        val query = uri.rawQuery
        return when (provider) {
            "netflix" -> if (host == "www.netflix.com" &&
                path.matches(Regex("/title/[0-9]{6,9}")) && query == null)
                    "https://www.netflix.com" + path else null
            "disney" -> if (host == "www.disneyplus.com" &&
                disneyPattern.matches(path) && query == null)
                    "https://www.disneyplus.com" + path else null
            "prime" -> {
                if (host != "app.primevideo.com" || path != "/detail" ||
                    query == null || !query.startsWith("gti=")) return null
                val id = query.removePrefix("gti=")
                if (!primeId.matches(id)) return null
                "https://app.primevideo.com/detail?gti=" + id
            }
            "apple" -> {
                if (host != "tv.apple.com" || !applePath.matches(path)) return null
                val pairs = query?.split("&")?.map {
                    val parts = it.split("=", limit = 2)
                    if (parts.size != 2) return null
                    URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
                } ?: emptyList()
                if (pairs.size > 5 || pairs.map { it.first }.distinct().size != pairs.size ||
                    pairs.any { it.first !in setOf("playableId", "at", "ct", "itscg", "itsct") })
                    return null
                val playable = pairs.firstOrNull { it.first == "playableId" }?.second
                if (playable != null && !playable.matches(Regex("[A-Za-z0-9._:-]{5,120}")))
                    return null
                "https://tv.apple.com" + path +
                    (if (playable != null) "?playableId=" +
                        URLEncoder.encode(playable, "UTF-8") else "")
            }
            "hbo" -> if (host == "play.hbomax.com" && maxPath.matches(path) &&
                (query == null || query.matches(Regex("utm_source=[A-Za-z0-9_-]{1,60}"))))
                    "https://play.hbomax.com" + path else null
            else -> null
        }
    }

    /** Resolve official title link through the AnyTV backend. No client API key. */
    suspend fun resolve(title: StreamingTitle, packageName: String):
        AnyTvPersonalResult? = withContext(Dispatchers.IO) {
        val provider = providerForPackage(packageName) ?: return@withContext null
        require(title.id > 0 && title.media in setOf("movie", "tv")) {
            "Identificador inválido."
        }
        val url = URL("https://anytv-link-resolver-pilot.brenoff.workers.dev/v1/resolve/" +
            title.media + "/" + title.id + "/" + provider)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 5000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/json")
            val status = conn.responseCode
            if (status == 404) return@withContext AnyTvPersonalResult.NotInSubscription
            if (status == 409) return@withContext AnyTvPersonalResult.NoSupportedLink
            if (status !in 200..299) return@withContext null
            val response = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            if (response.optString("status") != "link_candidate") return@withContext null
            val candidate = validatedLink(provider, response.optString("url"))
                ?: return@withContext AnyTvPersonalResult.NoSupportedLink
            AnyTvPersonalResult.Direct(candidate)
        } finally {
            conn.disconnect()
        }
    }
}
