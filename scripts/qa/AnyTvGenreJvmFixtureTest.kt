package com.hari.androidtvremote.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QA-only JVM fixture. Executes the real compiled top-level AnyTV Kotlin helpers
 * from overrides/TclStreamingScreens.kt through reflection in the Android test task.
 * It does not invoke Compose, a phone, production Worker, or any account.
 */
class AnyTvGenreJvmFixtureTest {
    private val owner = Class.forName(
        "com.hari.androidtvremote.ui.app.TclStreamingScreensKt"
    )

    private fun anime(title: StreamingTitle): Boolean {
        val method = owner.getDeclaredMethod("anyTvIsAnime", StreamingTitle::class.java)
        method.isAccessible = true
        return method.invoke(null, title) as Boolean
    }

    private fun genre(id: Int, media: String, movies: List<StreamingGenre>,
                      series: List<StreamingGenre>): Int {
        val method = owner.getDeclaredMethod(
            "anyTvResolvedGenre", Int::class.javaPrimitiveType, String::class.java,
            List::class.java, List::class.java
        )
        method.isAccessible = true
        return method.invoke(null, id, media, movies, series) as Int
    }

    @Suppress("UNCHECKED_CAST")
    private fun backfill(titles: List<StreamingTitle>): List<StreamingTitle> {
        val method = owner.getDeclaredMethod(
            "anyTvBackfillEditorial", String::class.java, List::class.java,
            List::class.java, Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        )
        method.isAccessible = true
        return method.invoke(null, "Populares", emptyList<StreamingTitle>(),
            titles, 40, true) as List<StreamingTitle>
    }

    @Suppress("UNCHECKED_CAST")
    private fun curate(titles: List<StreamingTitle>): List<StreamingTitle> {
        val method = owner.getDeclaredMethod(
            "anyTvCurate", List::class.java, String::class.java, Set::class.java,
            Set::class.java, Int::class.javaPrimitiveType, Map::class.java,
            Boolean::class.javaPrimitiveType
        )
        method.isAccessible = true
        return method.invoke(null, titles, "Populares", emptySet<Int>(),
            emptySet<String>(), 50, emptyMap<String, Int>(), true)
            as List<StreamingTitle>
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTmdb(payload: String, media: String): List<StreamingTitle> {
        // Execute original StreamingCatalog.mapTitles, not a duplicate JSON parser.
        val source = Class.forName("com.hari.androidtvremote.ui.app.StreamingCatalog")
        val instance = source.getDeclaredField("INSTANCE").apply { isAccessible = true }
            .get(null)
        val method = source.getDeclaredMethod("mapTitles",
            org.json.JSONArray::class.java, String::class.java,
            Int::class.javaPrimitiveType)
        method.isAccessible = true
        return method.invoke(instance, org.json.JSONArray(payload), media, 0)
            as List<StreamingTitle>
    }

    @Test fun realJsonParserCarriesMissingGenreIdsIntoAnimeFilter() {
        // The HTTP Worker already applied with_genres=16; source metadata
        // intentionally omits genre_ids for two otherwise valid Japanese IDs.
        val payload = """[
            {"id":301,"name":"Anime completo","original_language":"ja",
             "genre_ids":[16],"first_air_date":"2020-02-01"},
            {"id":302,"name":"Anime sem genre_ids","original_language":"ja"},
            {"id":303,"name":"Anime ids vazios","original_language":"ja","genre_ids":[]}
        ]"""
        val parsed = parseTmdb(payload, "tv")
        assertEquals(3, parsed.size)
        assertEquals(listOf(301, 302, 303), parsed.map { it.id })
        assertEquals(emptyList<Int>(), parsed[1].genreIds)
        assertEquals("", parsed[1].poster)
        assertEquals("", parsed[1].date)
        val afterPredicate = parsed.filter(::anime)
        assertEquals(listOf(301), afterPredicate.map { it.id })
        assertEquals(3, curate(backfill(parsed)).size)
        assertEquals(1, curate(backfill(afterPredicate)).size)
        println("ANYTV_REAL_PARSER_3_ANIME_ACCEPTED_1")
    }

    @Test fun realJsonParserKeepsDramaAndActionWithoutOptionalArtworkOrGenreIds() {
        val moviePayload = """[
            {"id":401,"title":"Drama sem poster"},
            {"id":402,"title":"Ação com genero","genre_ids":[28],"poster_path":"/p.jpg"},
            {"id":0,"title":"Invalido"},
            {"id":403,"title":""}
        ]"""
        val parsed = parseTmdb(moviePayload, "movie")
        assertEquals(listOf(401, 402), parsed.map { it.id })
        assertEquals(listOf(401, 402), curate(backfill(parsed)).map { it.id })
        assertEquals(emptyList<Int>(), parsed.first().genreIds)
        println("ANYTV_REAL_PARSER_DRAMA_ACTION_2_OF_2")
    }

    @Test fun actualAndroidCachedResponsePassesRealJsonParserAndEditorial() {
        // Reproduce real local cache -> StreamingCatalog.get -> mapTitles ->
        // Anime predicate -> editorial. Compose state and network are not tested.
        val cacheType = Class.forName("com.hari.androidtvremote.ui.app.StreamingDiskCache")
        val cache = cacheType.getDeclaredField("INSTANCE").apply { isAccessible = true }
            .get(null)
        val root = cacheType.getDeclaredField("root").apply { isAccessible = true }
        val original = root.get(null)
        val scratch = java.nio.file.Files.createTempDirectory("anytv-cached-parser-jvm-").toFile()
        try {
            root.set(null, scratch)
            val write = cacheType.getDeclaredMethod("write", String::class.java,
                org.json.JSONObject::class.java, Long::class.javaPrimitiveType)
                .apply { isAccessible = true }
            val catalogType = Class.forName("com.hari.androidtvremote.ui.app.StreamingCatalog")
            val catalog = catalogType.getDeclaredField("INSTANCE").apply { isAccessible = true }
                .get(null)
            val get = catalogType.getDeclaredMethod("get", String::class.java,
                String::class.java).apply { isAccessible = true }
            val parse = catalogType.getDeclaredMethod("mapTitles",
                org.json.JSONArray::class.java, String::class.java,
                Int::class.javaPrimitiveType).apply { isAccessible = true }

            val route = "discover/tv?language=pt-BR&with_genres=16&page=1"
            val source = org.json.JSONArray().put(org.json.JSONObject()
                .put("id", 911).put("name", "Anime com genero")
                .put("original_language", "ja")
                .put("genre_ids", org.json.JSONArray().put(16)))
                .put(org.json.JSONObject().put("id", 912)
                    .put("name", "Anime sem genero secundario")
                    .put("original_language", "ja"))
            val payload = org.json.JSONObject().put("results", source)
            write.invoke(cache, route, payload, 48L * 3600000L)
            val fromCache = get.invoke(catalog, "backend", route) as org.json.JSONObject
            val parsed = parse.invoke(catalog,
                fromCache.getJSONArray("results"), "tv", 0) as List<*>
            val titles = parsed.map { it as StreamingTitle }
            assertEquals(listOf(911, 912), titles.map { it.id })
            assertEquals(2, curate(backfill(titles)).size)
            assertEquals(listOf(911), titles.filter(::anime).map { it.id })
            println("ANYTV_ACTUAL_CACHE_GET_PARSE_2_EDITORIAL_2_ANIME_1")
        } finally {
            root.set(null, original)
            scratch.deleteRecursively()
        }
    }

    @Test fun actualAndroidDiskCacheRetainsAndExpiresSyntheticDiscoveryPayload() {
        // Unit-test only: install an isolated local cache root via reflection.
        // No remote endpoint, production account, or Android app data is touched.
        val cacheClass = Class.forName("com.hari.androidtvremote.ui.app.StreamingDiskCache")
        val cache = cacheClass.getDeclaredField("INSTANCE").apply { isAccessible = true }
            .get(null)
        val rootField = cacheClass.getDeclaredField("root")
            .apply { isAccessible = true }
        val previousRoot = rootField.get(null)
        val temporary = java.nio.file.Files.createTempDirectory("anytv-genre-cache-jvm-").toFile()
        try {
            rootField.set(null, temporary)
            val write = cacheClass.getDeclaredMethod("write", String::class.java,
                org.json.JSONObject::class.java, Long::class.javaPrimitiveType)
                .apply { isAccessible = true }
            val read = cacheClass.getDeclaredMethod("read", String::class.java)
                .apply { isAccessible = true }
            val stale = cacheClass.getDeclaredMethod("stale", String::class.java)
                .apply { isAccessible = true }

            val route = "discover/movie?with_genres=18&page=1"
            val reduced = org.json.JSONObject().put("results",
                org.json.JSONArray().put(org.json.JSONObject()
                    .put("id", 901).put("title", "Drama fixture")))
            write.invoke(cache, route, reduced, 48L * 3600000L)
            val cached = read.invoke(cache, route) as org.json.JSONObject
            assertEquals(1, cached.getJSONArray("results").length())
            val otherRoute = "discover/movie?with_genres=28&page=1"
            assertEquals(null, read.invoke(cache, otherRoute))

            val expiredRoute = "discover/movie?with_genres=18&page=2"
            write.invoke(cache, expiredRoute, reduced, -1L)
            assertEquals(null, read.invoke(cache, expiredRoute))
            val lastGood = stale.invoke(cache, expiredRoute) as org.json.JSONObject
            assertEquals(1, lastGood.getJSONArray("results").length())
            println("ANYTV_REAL_DISK_CACHE_FRESH_PARTIAL_AND_STALE_READ_VERIFIED")
        } finally {
            rootField.set(null, previousRoot)
            temporary.deleteRecursively()
        }
    }

    @Test fun dramaAndActionGenreMappingIsMediaAware() {
        val films = listOf(StreamingGenre(18, "Drama"),
            StreamingGenre(28, "Ação"), StreamingGenre(16, "Animação"))
        val shows = listOf(StreamingGenre(18, "Drama"),
            StreamingGenre(10759, "Action & Adventure"),
            StreamingGenre(16, "Animação"))
        assertEquals(18, genre(18, "movie", films, shows))
        assertEquals(18, genre(18, "tv", films, shows))
        assertEquals(28, genre(28, "movie", films, shows))
        assertEquals(10759, genre(28, "tv", films, shows))
        assertEquals(16, genre(-2, "movie", films, shows))
        assertEquals(16, genre(-2, "tv", films, shows))
    }

    @Test fun animeRejectsMissingGenreIdsDespiteJapaneseLanguage() {
        val incomplete = StreamingTitle(201, "Anime sem IDs", "", "", "tv",
            originalLanguage = "ja", genreIds = emptyList())
        val complete = incomplete.copy(id = 202, genreIds = listOf(16))
        val nonJapanese = complete.copy(id = 203, originalLanguage = "pt")
        assertFalse(anime(incomplete))
        assertTrue(anime(complete))
        assertFalse(anime(nonJapanese))
        println("ANYTV_JVM_ANIME_METADATA_REJECTION_CONFIRMED")
    }

    @Test fun animeCandidateLossOccursBeforeAndAfterEditorialCurating() {
        // Both groups come from a hypothetical TMDB with_genres=16 response.
        // Only the first group also has genre_ids locally; this measures how
        // much the real anyTvIsAnime function would discard after source filtering.
        val source = (1..24).map { id ->
            StreamingTitle(id, "Anime fixture $id", "", "", "tv",
                rating = 7.2, votes = 100, genreIds =
                    if (id <= 12) listOf(16) else emptyList(),
                originalLanguage = "ja")
        }
        assertEquals(24, source.size)
        val firstFilter = source.filter(::anime)
        assertEquals(12, firstFilter.size)
        val editorial = backfill(firstFilter)
        val curated = curate(editorial)
        assertEquals(12, editorial.size)
        assertEquals(12, curated.size)
        // The same valid synthetic source objects are not removed by the
        // editorial helpers alone; the difference is the Anime predicate.
        assertEquals(24, curate(backfill(source)).size)
        println("ANYTV_JVM_ANIME_SOURCE_24_FILTERED_12_EDITORIAL_12")
    }

    @Test fun genreEditorialKeepsFortyUndatedPosterlessFixtures() {
        val source = (1..40).map { id ->
            StreamingTitle(id, "Drama fixture $id", "", "", "movie",
                rating = 7.5, votes = 100, date = "", genreIds = emptyList())
        }
        val filled = backfill(source)
        assertEquals(40, filled.size)
        assertEquals(40, filled.map { it.id }.distinct().size)
        val curated = curate(filled)
        assertEquals(40, curated.size)
        assertEquals(40, curated.map { it.id }.distinct().size)
        println("ANYTV_JVM_GENRE_EDITORIAL_RETENTION_40_OF_40")
    }
}
