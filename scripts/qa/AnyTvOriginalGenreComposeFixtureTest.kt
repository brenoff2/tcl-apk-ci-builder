package com.hari.androidtvremote.ui.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * QA ONLY: executes the ORIGINAL TclStreamingScreens composable unchanged.
 * All java.net HTTPS connections are blocked or answered in memory. This test
 * must run in its OWN Gradle test invocation / isolated test JVM because
 * URL.setURLStreamHandlerFactory is process-global and cannot be reset.
 * No APK, real HTTP, credentials, device, or deployed services.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnyTvOriginalGenreComposeFixtureTest {
    @get:Rule val compose = createComposeRule()

    private val seen = Collections.synchronizedList(mutableListOf<String>())
    private val host = "anytv-link-resolver-pilot.brenoff.workers.dev"
    private val additionalDramaRequest = CountDownLatch(1)
    @Volatile private var monitorRetry = false
    @Volatile private var serveRecentGenreTitles = false
    @Volatile private var serveGenreMiniseries = false
    @Volatile private var failComedyEditorialPageThree = false
    @Volatile private var failDocumentaryAllSources = false

    private fun fixtureRecentTitles(route: String): String {
        val movie = route.startsWith("discover/movie")
        val genre = Regex("with_genres=([0-9]+)").find(route)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val sourcePage = Regex("[?&]page=([0-9]+)").find(route)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val entries = org.json.JSONArray()
        if (sourcePage in 1..3 && genre in setOf(18, 28, 10759, 16, 35, 99)) {
            val date = java.time.LocalDate.now().minusDays(10).toString()
            repeat(20) { index ->
                val row = JSONObject()
                    .put("id", (if (movie) 100000 else 200000) + genre * 100 +
                        (sourcePage - 1) * 20 + index)
                    .put(if (movie) "title" else "name", "QA $genre ${if (movie) "filme" else "serie"} $index")
                    .put(if (movie) "release_date" else "first_air_date",
                        if (!movie && sourcePage == 1 && index == 1)
                            java.time.LocalDate.now().minusYears(2).toString() else date)
                    .put("genre_ids", org.json.JSONArray().put(genre))
                    .put("original_language", if (genre == 16) "ja" else "pt")
                    .put("poster_path", "/qa-$index.jpg")
                    .put("vote_count", 200).put("vote_average", 7.8)
                entries.put(row)
            }
        }
        return JSONObject().put("results", entries).put("total_pages", 3).toString()
    }

    @Test fun actualScreenRetryRefetchesSelectedGenreAfterCacheIsCleared() {
        URL.setURLStreamHandlerFactory(URLStreamHandlerFactory { protocol ->
            if (protocol != "https" && protocol != "http") return@URLStreamHandlerFactory null
            object : URLStreamHandler() {
                override fun openConnection(url: URL): URLConnection {
                    // Fail CLOSED on anything outside the canonical catalogue host.
                    if (protocol != "https" || url.host != host)
                        throw SecurityException("QA blocks non-catalogue URLs")
                    if (!url.path.startsWith("/v1/catalog/"))
                        throw SecurityException("QA blocks non-catalogue HTTPS")
                    val route = url.toString().substringAfter("/v1/catalog/")
                    seen.add(route)
                    // Force every movie/TV catalogue page to fail for Documentary.
                    // This is a full outage on a cold cache, not HTTP-200 empty.
                    if (failDocumentaryAllSources &&
                        route.startsWith("discover/") &&
                        route.contains("with_genres=99")) {
                        throw java.io.IOException("QA all Documentary sources unavailable")
                    }
                    // Simulate a genuine partial editorial outage on a new genre:
                    // page 3 fails, pages 1+2 remain verified. No production HTTP.
                    if (failComedyEditorialPageThree &&
                        route.startsWith("discover/") &&
                        route.contains("with_genres=35") &&
                        Regex("[?&]page=3(?:&|$)").containsMatchIn(route)) {
                        throw java.io.IOException("QA partial editorial page 3 unavailable")
                    }
                    if (monitorRetry && route.contains("with_genres=18"))
                        additionalDramaRequest.countDown()
                    val body = when {
                        route.startsWith("genre/movie/list") ->
                            """{"genres":[{"id":18,"name":"Drama"},{"id":28,"name":"Ação"},{"id":35,"name":"Comédia"},{"id":99,"name":"Documentário"}]}"""
                        route.startsWith("genre/tv/list") ->
                            """{"genres":[{"id":18,"name":"Drama"},{"id":10759,"name":"Ação"},{"id":35,"name":"Comédia"},{"id":99,"name":"Documentário"}]}"""
                        serveRecentGenreTitles && route.startsWith("discover/") &&
                            route.contains("with_genres=") -> fixtureRecentTitles(route)
                        serveGenreMiniseries && route.startsWith("tv/") &&
                            route.contains("?language=") -> {
                            val id = route.substringAfter("tv/").substringBefore("?").toIntOrNull()
                            val fixtureGenres = setOf(18, 10759, 16)
                            val isMiniseries = id != null && fixtureGenres.any { genre ->
                                id == 200000 + genre * 100
                            }
                            val isReturning = id != null && fixtureGenres.any { genre ->
                                id == 200000 + genre * 100 + 1
                            }
                            val details = JSONObject().put("id", id ?: 0)
                                .put("type", if (isMiniseries) "Miniseries" else "Scripted")
                            if (isReturning) {
                                details.put("seasons", org.json.JSONArray()
                                    .put(JSONObject().put("season_number", 1)
                                        .put("name", "Temporada 1")
                                        .put("episode_count", 8)
                                        .put("air_date",
                                            java.time.LocalDate.now().minusYears(2).toString()))
                                    .put(JSONObject().put("season_number", 2)
                                        .put("name", "Temporada 2")
                                        .put("episode_count", 8)
                                        .put("air_date",
                                            java.time.LocalDate.now().minusDays(10).toString())))
                            }
                            details.toString()
                        }
                        else -> """{"results":[],"total_pages":1,"genres":[]}"""
                    }
                    return object : HttpURLConnection(url) {
                        override fun connect() { connected = true }
                        override fun disconnect() { connected = false }
                        override fun usingProxy(): Boolean = false
                        override fun getResponseCode(): Int =
                            if (route.startsWith("movie/now_playing")) HTTP_UNAVAILABLE else HTTP_OK
                        override fun getInputStream(): InputStream =
                            ByteArrayInputStream(body.toByteArray(StandardCharsets.UTF_8))
                    }
                }
            }
        })
        // A complete fresh test cache root ensures no prior user/application data.
        val cacheClass = Class.forName("com.hari.androidtvremote.ui.app.StreamingDiskCache")
        val root = cacheClass.getDeclaredField("root").apply { isAccessible = true }
        val prior = root.get(null)
        val scratch = Files.createTempDirectory("anytv-real-compose-fixture-").toFile()
        try {
            root.set(null, scratch)
            compose.setContent {
                TclStreamingScreens(
                    apps = emptyList(),
                    host = "",
                    pairing = "",
                    tvName = "QA Only",
                    onDismiss = {}
                )
            }
            // The actual screen must expose a Drama chip from synthetic metadata.
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("Drama").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithText("Drama").onFirst().performClick()
            // Empty successful payload produces the actual screen's retry affordance.
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("Tentar novamente").fetchSemanticsNodes().isNotEmpty()
            }
            val initial = synchronized(seen) {
                seen.count { it.contains("with_genres=18") }
            }
            assertTrue("Original screen must have queried Drama", initial > 0)
            // Expire/remove source caches to detect any request even when initial
            // response was successful, rather than masking retry by the 48h TTL.
            scratch.listFiles()?.forEach { it.delete() }
            // Switch fixture data BEFORE retry starts; switching while async
            // editorial jobs are still active produces cross-epoch cache races.
            serveRecentGenreTitles = true
            serveGenreMiniseries = true
            monitorRetry = true
            compose.onAllNodesWithText("Tentar novamente").onFirst().performClick()
            compose.waitForIdle()
            // Catch delayed IO launched after the click, rather than relying
            // solely on the Compose frame clock being idle.
            assertTrue("Retry must start a new Drama request",
                additionalDramaRequest.await(10, TimeUnit.SECONDS))
            monitorRetry = false
            val after = synchronized(seen) {
                seen.count { it.contains("with_genres=18") }
            }
            // R03 regression: the original Composable must refetch on explicit retry.
            assertTrue("Retry did not refetch Drama after cache invalidation", after > initial)
            // Positive control: after deleting the cache a direct call through
            // the original catalogue GET must hit our intercepted URL.
            val catalogType = Class.forName("com.hari.androidtvremote.ui.app.StreamingCatalog")
            val catalog = catalogType.getDeclaredField("INSTANCE").apply { isAccessible = true }
                .get(null)
            val get = catalogType.getDeclaredMethod("get", String::class.java,
                String::class.java).apply { isAccessible = true }
            val probe = "discover/movie?language=pt-BR&with_genres=18&page=99"
            get.invoke(catalog, "backend", probe)
            val controlled = synchronized(seen) {
                seen.count { it.contains("with_genres=18") }
            }
            assertTrue("Direct source probe must be intercepted",
                controlled > after && synchronized(seen) { seen.contains(probe) })
            // Regression R03: an HTTP-200 empty refresh must not overwrite an
            // older verified response in the disk cache across screen restarts.
            val write = cacheClass.getDeclaredMethod("write", String::class.java,
                JSONObject::class.java, Long::class.javaPrimitiveType)
                .apply { isAccessible = true }
            val refresh = catalogType.getDeclaredMethod("get", String::class.java,
                String::class.java, Boolean::class.javaPrimitiveType)
                .apply { isAccessible = true }
            val lastGoodRoute = "discover/movie?language=pt-BR&with_genres=18&page=77"
            val lastGood = JSONObject().put("results",
                org.json.JSONArray().put(JSONObject().put("id", 999)
                    .put("title", "Drama verificado")))
            write.invoke(cacheClass.getDeclaredField("INSTANCE").apply {
                isAccessible = true
            }.get(null), lastGoodRoute, lastGood, 3600000L)
            val retained = refresh.invoke(catalog, "backend", lastGoodRoute, true)
                as JSONObject
            assertEquals(999, retained.getJSONArray("results")
                .getJSONObject(0).getInt("id"))
            val readback = get.invoke(catalog, "backend", lastGoodRoute) as JSONObject
            assertEquals(999, readback.getJSONArray("results")
                .getJSONObject(0).getInt("id"))
            println("ANYTV_ORIGINAL_COMPOSE_R03_RETRY_REFETCH_AND_LAST_GOOD_PASS")
            // Behavioral regression against the ORIGINAL Composable. A valid
            // recent genre movie must not disappear just because the separate
            // Cinema index failed to load. The previous empty fixture has
            // established the retry button; now return 20 valid real-shaped
            // TMDB movie + TV entries per page across three distinct source pages.
            val failedGenres = mutableListOf<String>()
            val scenarios = listOf(
                Triple("Drama", 18, 18),
                Triple("Ação", 28, 28),
                Triple("Anime", -2, 16)
            )
            for ((label, selectedId, sourceId) in scenarios) {
                val tvId = if (label == "Ação") 10759 else selectedId
                if (label != "Drama") {
                    compose.onAllNodesWithText(label).onFirst().performClick()
                }
                // The screen must query a real source and emit its own final
                // per-section snapshot, not a copied editorial implementation.
                try {
                compose.waitUntil(30_000) {
                    // First-page results may render before async editorial pages
                    // finish. Synchronize on ALL observed stages, not Populares.
                    val logs = org.robolectric.shadows.ShadowLog.getLogs()
                        .filter { it.tag == "AnyTVGenreQA" }.map { it.msg }
                    synchronized(seen) {
                        seen.any { it.contains("with_genres=$sourceId") }
                    } && logs.any {
                        it.contains("stage=tmdb genre=$selectedId media=movie sourcePage=1 raw=20 parsed=20 accepted=20")
                    } && logs.any {
                        it.contains("stage=first genre=$selectedId media=movie") &&
                            it.contains("titles=24")
                    } && logs.any {
                        it.contains("stage=editorial genre=$selectedId media=movie") &&
                            it.contains("titles=60")
                    } && logs.any {
                        it.contains("stage=tmdb genre=$tvId media=tv sourcePage=1 raw=20 parsed=20 accepted=20")
                    } && logs.any {
                        it.contains("stage=first genre=$tvId media=tv") &&
                            it.contains("titles=24")
                    } && logs.any {
                        it.contains("stage=editorial genre=$tvId media=tv") &&
                            it.contains("titles=60")
                    } && logs.any {
                        it.contains("stage=miniseries genre=$tvId media=tv") &&
                            it.contains("candidates=60") &&
                            it.contains("confirmed=1")
                    } && logs.any {
                        it.contains("stage=seasons genre=$tvId media=tv") &&
                            it.contains("candidates=60") &&
                            it.contains("confirmed=1")
                    } && logs.any {
                        it.contains("stage=render genre=$selectedId ") &&
                            it.contains("sections=") &&
                            it.contains("Populares:") &&
                            !it.contains("Populares:0") &&
                            Regex("Lançamentos · Filmes:([1-9][0-9]*)").containsMatchIn(it) &&
                            Regex("Lançamentos · Séries:([1-9][0-9]*)").containsMatchIn(it) &&
                            Regex("Minisséries imperdíveis:([1-9][0-9]*)").containsMatchIn(it) &&
                            Regex("Novas temporadas:([1-9][0-9]*)").containsMatchIn(it)
                    }
                }
                } catch (error: Throwable) {
                    val stageLogs = org.robolectric.shadows.ShadowLog.getLogs()
                        .filter { it.tag == "AnyTVGenreQA" }
                        .map { it.msg }.filter { it.contains("genre=$selectedId") ||
                            it.contains("genre=$tvId") }.takeLast(28)
                    val metadataRoutes = synchronized(seen) {
                        seen.filter { it.startsWith("tv/") }.takeLast(30)
                    }
                    throw AssertionError("$label: source/miniseries stage failure; " +
                        "stages=$stageLogs metadataRoutes=$metadataRoutes", error)
                }
                val snapshot = org.robolectric.shadows.ShadowLog.getLogs()
                    .last { it.tag == "AnyTVGenreQA" &&
                        it.msg.contains("stage=render genre=$selectedId ") &&
                        it.msg.contains("Populares:") }
                    .msg
                // Verify the original composable builds the entire approved
                // editorial contract, in order, without extra genre rails.
                // Factual sections may legitimately contain zero results.
                val sectionPayload = snapshot.substringAfter("sections=")
                val expectedOrder = listOf(
                    "Destaques da semana · Filmes",
                    "Destaques da semana · Séries",
                    "Em alta agora",
                    "Indicados para você",
                    "Populares",
                    "Lançamentos · Filmes",
                    "Lançamentos · Séries",
                    "Em breve · Filmes e séries",
                    "No cinema",
                    "Novas temporadas",
                    "Mais bem avaliados",
                    "Minisséries imperdíveis"
                )
                val actualSections = sectionPayload.split("|").map { entry ->
                    entry.substringBeforeLast(":") to
                        (entry.substringAfterLast(":").toIntOrNull() ?: -1)
                }
                assertEquals("$label: 12 sections and editorial order", expectedOrder,
                    actualSections.map { it.first })
                // Physical code135 regression: a populated editorial snapshot is
                // NOT proof the original Composable actually draws the rail.
                // In sections mode, the heading must exist in rendered semantics.
                compose.waitForIdle()
                assertTrue("$label: Populares has data but is not displayed in section mode",
                    compose.onAllNodesWithText("Populares").fetchSemanticsNodes().isNotEmpty())
                val sectionCount = actualSections.toMap()
                assertTrue("$label: recent movies must survive Cinema outage",
                    (sectionCount["Lançamentos · Filmes"] ?: 0) > 0)
                assertTrue("$label: recent TV must be represented",
                    (sectionCount["Lançamentos · Séries"] ?: 0) > 0)
                assertTrue("$label: Populares must retain the source catalogue",
                    (sectionCount["Populares"] ?: 0) > 0)
                // Three 20-title TMDB pages per medium provide 60 unique
                // verified candidates. Non-factual rails should backfill 40.
                for (heading in listOf("Destaques da semana · Filmes",
                    "Destaques da semana · Séries", "Em alta agora",
                    "Indicados para você", "Populares", "Mais bem avaliados")) {
                    assertEquals("$label: approved 40 genuine genre items in $heading",
                        40, sectionCount[heading] ?: -1)
                }
                assertEquals("$label: 40 real recent genre films", 40,
                    sectionCount["Lançamentos · Filmes"])
                assertEquals("$label: 40 real recent genre TV series", 40,
                    sectionCount["Lançamentos · Séries"])
                println("ANYTV_GENRE_EDITORIAL_DEPTH label=$label " +
                    "movie=60 tv=60 maxPerSection=40 sections=$sectionPayload")
                assertEquals("$label: cannot invent theatrical screenings", 0,
                    sectionCount["No cinema"])
                assertEquals("$label: cannot invent future premieres", 0,
                    sectionCount["Em breve · Filmes e séries"])
                // The genre's second TV candidate has a documented second
                // season premiered 10 days ago, but never enters global feeds.
                // RED in code134: genre-only returning season is discarded.
                val returningId = 200000 + (if (label == "Anime") 16 else tvId) * 100 + 1
                assertTrue("$label: returning series must be in selected genre source",
                    synchronized(seen) {
                        seen.any { it.contains("with_genres=$sourceId") }
                    })
                println("ANYTV_GENRE_SEASON_TRACE label=$label id=$returningId " +
                    "newSeasons=${sectionCount["Novas temporadas"] ?: -1}")
                assertEquals("$label: TMDB-confirmed season 2 from the genre must appear", 1,
                    sectionCount["Novas temporadas"])
                // First TV result has explicit TMDB type=Miniseries; it is absent
                // from every Home/global discovery fixture by construction.
                // RED on code134: genre-only verified miniseries are ignored.
                val miniId = 200000 + (if (label == "Anime") 16 else tvId) * 100
                val metadataCalls = synchronized(seen) {
                    seen.count { it.startsWith("tv/$miniId?language=") }
                }
                println("ANYTV_GENRE_MINISERIES_TRACE label=$label genre=$tvId " +
                    "source=60 metadataCalls=$metadataCalls " +
                    "rendered=${sectionCount["Minisséries imperdíveis"] ?: -1}")
                assertTrue("$label: genre-only classification must call TMDB metadata: $miniId",
                    metadataCalls > 0)
                assertTrue("$label: verified genre-only TMDB Miniseries $miniId " +
                    "must appear without depending on Home (metadataCalls=$metadataCalls): $snapshot",
                    (sectionCount["Minisséries imperdíveis"] ?: 0) > 0)
                println("ANYTV_ORIGINAL_GENRE_12_SECTION_CONTRACT label=$label sections=$sectionPayload")
                // Verify every boundary in the actual source-to-render flow.
                // Source page 1 is a synthetic TMDB-shaped 20-title payload.
                val sourceStages = org.robolectric.shadows.ShadowLog.getLogs().map { it.msg }
                assertTrue("$label: TMDB movie payload lost before parsing",
                    sourceStages.any { it.contains(
                        "stage=tmdb genre=$selectedId media=movie sourcePage=1 raw=20 parsed=20 accepted=20") })
                assertTrue("$label: first genre movie page lost its candidates",
                    sourceStages.any { it.contains(
                        "stage=first genre=$selectedId media=movie") &&
                        it.contains("titles=24") })
                assertTrue("$label: editorial genre pool lost its candidates",
                    sourceStages.any { it.contains(
                        "stage=editorial genre=$selectedId media=movie") &&
                        it.contains("titles=60") })
                assertTrue("$label: TV source payload lost before parsing",
                    sourceStages.any { it.contains(
                        "stage=tmdb genre=$tvId media=tv sourcePage=1 raw=20 parsed=20 accepted=20") })
                assertTrue("$label: first TV genre page lost its candidates",
                    sourceStages.any { it.contains(
                        "stage=first genre=$tvId media=tv") &&
                        it.contains("titles=24") })
                assertTrue("$label: TV editorial genre pool lost its candidates",
                    sourceStages.any { it.contains(
                        "stage=editorial genre=$tvId media=tv") &&
                        it.contains("titles=60") })
                val recentFilms = Regex("Lançamentos · Filmes:([0-9]+)")
                    .find(snapshot)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val population = Regex("Populares:([0-9]+)")
                    .find(snapshot)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                assertTrue("$label: source must populate Populares first: $snapshot",
                    population > 0)
                println("ANYTV_ORIGINAL_GENRE_CINEMA_GUARD_REPRO label=$label popular=$population recentFilms=$recentFilms")
                if (recentFilms == 0) failedGenres.add(label)
            }
            // New genre, cold cache: each medium has 40 valid titles from
            // pages 1+2, but page 3 is unavailable. The opaque loader must
            // clear after partial errors and not invent a "zero titles" result.
            failComedyEditorialPageThree = true
            compose.onAllNodesWithText("Comédia").onFirst().performClick()
            try {
                compose.waitUntil(30_000) {
                    val logs = org.robolectric.shadows.ShadowLog.getLogs()
                        .filter { it.tag == "AnyTVGenreQA" }.map { it.msg }
                    logs.any { it.contains("stage=editorial genre=35 media=movie") &&
                        it.contains("success=3 failed=0") &&
                        it.contains("titles=40") } &&
                        logs.any { it.contains("stage=editorial genre=35 media=tv") &&
                            it.contains("success=3 failed=0") &&
                            it.contains("titles=40") } &&
                        logs.any { it.contains("stage=render genre=35 ") &&
                            it.contains("Populares:40") }
                }
            } catch (error: Throwable) {
                val recentLogs = org.robolectric.shadows.ShadowLog.getLogs()
                    .filter { it.tag == "AnyTVGenreQA" &&
                        it.msg.contains("genre=35") }
                    .map { it.msg }.takeLast(35)
                val requests = synchronized(seen) {
                    seen.filter { it.contains("with_genres=35") }.takeLast(35)
                }
                val overlay = compose.onAllNodesWithText(
                    "Preparando catálogo de filmes e séries…").fetchSemanticsNodes().size
                throw AssertionError("Partial editorial QA stages=$recentLogs " +
                    "requests=$requests overlayNodes=$overlay", error)
            }
            val comedySnapshot = org.robolectric.shadows.ShadowLog.getLogs()
                .last { it.tag == "AnyTVGenreQA" &&
                    it.msg.contains("stage=render genre=35 ") &&
                    it.msg.contains("Populares:40") }.msg
            assertTrue("Partial editorial errors must not erase verified titles",
                comedySnapshot.contains("Populares:40"))
            assertTrue("Partial editorial errors must clear opaque loading layer",
                compose.onAllNodesWithText("Preparando catálogo de filmes e séries…")
                    .fetchSemanticsNodes().isEmpty())
            assertTrue("Verified film premieres must survive partial errors",
                Regex("Lançamentos · Filmes:([1-9][0-9]*)")
                    .containsMatchIn(comedySnapshot))
            assertTrue("Verified TV premieres must survive partial errors",
                Regex("Lançamentos · Séries:([1-9][0-9]*)")
                    .containsMatchIn(comedySnapshot))
            println("ANYTV_GENRE_PARTIAL_EDITORIAL_LAST_GOOD_PASS " +
                "genre=35 pagesOk=2 pagesFailed=1 " +
                "sourceMovie=40 sourceTV=40 snapshot=$comedySnapshot")
            failComedyEditorialPageThree = false

            // Total source outage on a NEW genre with an empty disk cache.
            // An error is not evidence of zero eligible catalogue entries.
            failDocumentaryAllSources = true
            compose.onAllNodesWithText("Documentário").onFirst().performClick()
            compose.waitUntil(30_000) {
                compose.onAllNodesWithText("Tentar novamente")
                    .fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithText("Preparando catálogo de filmes e séries…")
                        .fetchSemanticsNodes().isEmpty()
            }
            assertTrue("Total outage cannot claim Documentary has no titles",
                compose.onAllNodesWithText("Não encontramos títulos neste gênero no momento.")
                    .fetchSemanticsNodes().isEmpty())
            val coldOutageRequests = synchronized(seen) {
                seen.count { it.contains("with_genres=99") }
            }
            assertTrue("Both Documentary media sources must be attempted on outage",
                coldOutageRequests >= 2)
            println("ANYTV_GENRE_TOTAL_OUTAGE_COLD_PASS " +
                "requests=$coldOutageRequests retry=1 overlay=0 falseEmpty=0")

            // Recover the fully failed first-page path through the real retry
            // button, not by bypassing the Composable or resetting its state.
            failDocumentaryAllSources = false
            compose.onAllNodesWithText("Tentar novamente").onFirst().performClick()
            // Drive the recomposition after the click, as the earlier Drama
            // retry test does, before polling asynchronous catalogue work.
            compose.waitForIdle()
            try {
                compose.waitUntil(30_000) {
                    org.robolectric.shadows.ShadowLog.getLogs().any {
                        it.tag == "AnyTVGenreQA" &&
                            it.msg.contains("stage=render genre=99 ") &&
                            it.msg.contains("Populares:40")
                    } && compose.onAllNodesWithText("Preparando catálogo de filmes e séries…")
                        .fetchSemanticsNodes().isEmpty()
                }
            } catch (error: Throwable) {
                val logs = org.robolectric.shadows.ShadowLog.getLogs()
                    .filter { (it.tag == "AnyTVGenreQA" ||
                        it.tag == "AnyTVGenreRetryQA") && it.msg.contains("genre=99") }
                    .map { "${it.tag}: ${it.msg}" }.takeLast(45)
                val requests = synchronized(seen) {
                    seen.filter { it.contains("with_genres=99") }.takeLast(35)
                }
                val overlay = compose.onAllNodesWithText(
                    "Preparando catálogo de filmes e séries…").fetchSemanticsNodes().size
                val retry = compose.onAllNodesWithText(
                    "Tentar novamente").fetchSemanticsNodes().size
                throw AssertionError("Documentary retry state logs=$logs requests=$requests " +
                    "overlay=$overlay retry=$retry", error)
            }
            val documentarySnapshot = org.robolectric.shadows.ShadowLog.getLogs()
                .last { it.tag == "AnyTVGenreQA" &&
                    it.msg.contains("stage=render genre=99 ") &&
                    it.msg.contains("Populares:40") }.msg
            assertTrue("Retry must display real Documentary candidates",
                documentarySnapshot.contains("Populares:40"))
            println("ANYTV_GENRE_TOTAL_OUTAGE_RECOVERED_PASS " +
                "snapshot=$documentarySnapshot")
            // The same original GET implementation must retain a verified
            // previous response on a network EXCEPTION during force-refresh.
            // This differs from the earlier HTTP-200-empty last-good test.
            val documentaryRoute =
                "discover/movie?language=pt-BR&include_adult=false&sort_by=popularity.desc&with_genres=99&page=1"
            val documentaryCached = JSONObject().put("results",
                org.json.JSONArray().put(JSONObject()
                    .put("id", 909900).put("title", "Documento verificado")
                    .put("release_date",
                        java.time.LocalDate.now().minusDays(10).toString())
                    .put("genre_ids", org.json.JSONArray().put(99))))
                .put("total_pages", 1)
            write.invoke(cacheClass.getDeclaredField("INSTANCE").apply {
                isAccessible = true
            }.get(null), documentaryRoute, documentaryCached, 3600000L)
            // The retry above restored HTTP. Force the outage again here so
            // the assertion exercises stale cache on an actual exception.
            failDocumentaryAllSources = true
            val cachedOnNetworkFailure =
                refresh.invoke(catalog, "backend", documentaryRoute, true) as JSONObject
            assertEquals("Network outage cannot wipe verified source cache",
                909900, cachedOnNetworkFailure.getJSONArray("results")
                    .getJSONObject(0).getInt("id"))
            val diskReadback = get.invoke(catalog, "backend", documentaryRoute) as JSONObject
            assertEquals("Last-good must survive readback after failed network refresh",
                909900, diskReadback.getJSONArray("results")
                    .getJSONObject(0).getInt("id"))
            println("ANYTV_GENRE_TOTAL_OUTAGE_LAST_GOOD_PASS cachedId=909900")

            // RED EXPECTATION on code134: a source-authoritative recent movie
            // should remain in Lançamentos even if now_playing is unavailable.
            // Deliberately fails until production code genuinely fixes the bug.
            assertTrue("Genre recent film titles dropped by cinema readiness: $failedGenres",
                failedGenres.isEmpty())
        } finally {
            root.set(null, prior)
            scratch.deleteRecursively()
        }
    }
}
