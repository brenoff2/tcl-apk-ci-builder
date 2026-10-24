package com.hari.androidtvremote.ui.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.content.Context
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

internal sealed class AnyTvPersonalResult {
    data class Direct(val url: String) : AnyTvPersonalResult()
    data object NotInSubscription : AnyTvPersonalResult()
    data object NoSupportedLink : AnyTvPersonalResult()
    data object ActivationRequired : AnyTvPersonalResult()
    data object ResolverUnavailable : AnyTvPersonalResult()
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
                path.matches(Regex("/title/[0-9]{6,9}/?")) && query == null)
                    "https://www.netflix.com" + path.removeSuffix("/") else null
            "disney" -> if (host == "www.disneyplus.com" &&
                disneyPattern.matches(path) && query == null)
                    "https://www.disneyplus.com" + path else null
            "prime" -> {
                if (host == "www.primevideo.com" && query == null &&
                    path.matches(Regex("/detail/[A-Z0-9]{10,40}")))
                    return "https://www.primevideo.com" + path
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

    /** Signed, read-only check: never treats a catalog response as activation proof. */
    suspend fun activationStatus(context: Context): String = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(AnyTvDeviceIdentity.RESOLVER_BASE + "/v1/device/status")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 5000
                connection.readTimeout = 8000
                connection.setRequestProperty("Accept", "application/json")
                AnyTvDeviceIdentity.attachProof(context, connection, url.toURI().rawPath)
                when (connection.responseCode) {
                    200 -> "activated"
                    401 -> "unauthorized"
                    else -> "unavailable"
                }
            } finally {
                connection.disconnect()
            }
        }.getOrDefault("unavailable")
    }

    /** Resolve official title link through the AnyTV backend. No client API key. */
    suspend fun resolve(context: Context, title: StreamingTitle, packageName: String):
        AnyTvPersonalResult? = withContext(Dispatchers.IO) {
        val provider = providerForPackage(packageName) ?: return@withContext null
        require(title.id > 0 && title.media in setOf("movie", "tv")) {
            "Identificador inválido."
        }
        val url = URL(AnyTvDeviceIdentity.RESOLVER_BASE + "/v1/resolve/" +
            title.media + "/" + title.id + "/" + provider)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 5000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "AnyTV/2.9.8 (Android; streaming catalog client)")
            AnyTvDeviceIdentity.attachProof(context, conn, url.toURI().rawPath)
            val status = conn.responseCode
            if (status == 401) return@withContext AnyTvPersonalResult.ActivationRequired
            if (status in 500..599) return@withContext AnyTvPersonalResult.ResolverUnavailable
            // HTTP 404 has TWO different provider meanings. Preserve the JSON
            // status rather than incorrectly treating a missing URL as no subscription.
            if (status == 404 || status == 409) {
                val sourceStatus = runCatching {
                    JSONObject(conn.errorStream?.bufferedReader()?.use {
                        it.readText()
                    } ?: "").optString("status")
                }.getOrNull()
                return@withContext when (sourceStatus) {
                    "not_available" -> AnyTvPersonalResult.NotInSubscription
                    "no_supported_link", "ambiguous" -> AnyTvPersonalResult.NoSupportedLink
                    else -> null // Unknown/upstream HTTP error: try secondary lookup.
                }
            }
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
