package com.hari.androidtvremote.ui.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Optional, private on-device API key. Never built into APK or sent to the TV. */
internal object AnyTvPersonalKeys {
    private const val ALIAS = "anytv_personal_streaming_key_v1"
    private const val PREFS = "anytv_personal_private"
    private const val ENTRY = "movie_of_night_encrypted"

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }

    fun read(context: Context): String {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ENTRY, null) ?: return ""
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        require(bytes.size >= 12 + 16) { "Credencial criptografada inválida." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(),
            GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), StandardCharsets.UTF_8)
    }

    fun write(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (apiKey.isBlank()) {
            check(prefs.edit().remove(ENTRY).commit()) { "Falha ao limpar credencial." }
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val packed = cipher.iv + cipher.doFinal(apiKey.toByteArray(StandardCharsets.UTF_8))
        check(prefs.edit().putString(ENTRY,
            Base64.encodeToString(packed, Base64.NO_WRAP)).commit()) {
            "Não foi possível guardar a chave protegida."
        }
    }
}

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

    suspend fun resolve(apiKey: String, title: StreamingTitle, packageName: String):
        AnyTvPersonalResult? = withContext(Dispatchers.IO) {
        val provider = providerForPackage(packageName) ?: return@withContext null
        require(apiKey.isNotBlank()) { "Configure sua chave da API de streaming." }
        require(title.id > 0 && title.media in setOf("movie", "tv")) {
            "Identificador inválido."
        }
        val wanted = title.media + "/" + title.id
        val remote = URL("https://api.movieofthenight.com/v4/shows/" +
            title.media + "%2F" + title.id + "?country=br&series_granularity=show")
        val conn = remote.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 5000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("X-API-Key", apiKey)
            val status = conn.responseCode
            if (status == 401 || status == 403)
                error("Chave Movie of the Night não autorizada. Verifique nas configurações.")
            if (status !in 200..299) error("Catálogo de links indisponível (HTTP " + status + ").")
            val response = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            if (response.optString("tmdbId") != wanted)
                error("O identificador do título não corresponde ao filme escolhido.")
            val options = response.optJSONObject("streamingOptions")
                ?.optJSONArray("br") ?: JSONArray()
            val offers = (0 until options.length()).mapNotNull { i ->
                options.optJSONObject(i)
            }.filter { offer ->
                offer.optJSONObject("service")?.optString("id") == provider &&
                    offer.optString("type") in setOf("subscription", "free") &&
                    (!offer.has("addon") || offer.isNull("addon"))
            }
            if (offers.isEmpty()) return@withContext AnyTvPersonalResult.NotInSubscription
            val links = offers.mapNotNull { validatedLink(provider, it.optString("link")) }
                .distinct()
            when (links.size) {
                1 -> AnyTvPersonalResult.Direct(links.single())
                else -> AnyTvPersonalResult.NoSupportedLink
            }
        } finally {
            conn.disconnect()
        }
    }
}
