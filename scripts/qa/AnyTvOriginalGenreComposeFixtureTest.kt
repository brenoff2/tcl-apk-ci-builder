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

    private fun fixtureRecentTitles(route: String): String {
        val movie = route.startsWith("discover/movie")
        val genre = Regex("with_genres=([0-9]+)").find(route)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val sourcePage = Regex("[?&]page=([0-9]+)").find(route)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val entries = org.json.JSONArray()
        if (sourcePage == 1 && genre in setOf(18, 28, 10759, 16)) {
            val date = java.time.LocalDate.now().minusDays(10).toString()
            repeat(20) { index ->
                val row = JSONObject()
                    .put("id", (if (movie) 100000 else 200000) + genre * 100 + index)
                    .put(if (movie) "title" else "name", "QA $genre ${if (movie) "filme" else "serie"} $index")
                    .put(if (movie) "release_date" else "first_air_date", date)
                    .put("genre_ids", org.json.JSONArray().put(genre))
                    .put("original_language", if (genre == 16) "ja" else "pt")
                    .put("poster_path", "/qa-$index.jpg")
                    .put("vote_count", 200).put("vote_average", 7.8)
                entries.put(row)
            }
        }
        return JSONObject().put("results", entries).put("total_pages", 1).toString()
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
                    if (monitorRetry && route.contains("with_genres=18"))
                        additionalDramaRequest.countDown()
                    val body = when {
                        route.startsWith("genre/movie/list") ->
                            """{"genres":[{"id":18,"name":"Drama"},{"id":28,"name":"Ação"}]}"""
                        route.startsWith("genre/tv/list") ->
                            """{"genres":[{"id":18,"name":"Drama"},{"id":10759,"name":"Ação"}]}"""
                        serveRecentGenreTitles && route.startsWith("discover/") &&
                            route.contains("with_genres=") -> fixtureRecentTitles(route)
                        serveGenreMiniseries && route.startsWith("tv/") &&
                            route.contains("?language=") -> {
                            val id = route.substringAfter("tv/").substringBefore("?").toIntOrNull()
                            val fixtureGenres = setOf(18, 10759, 16)
                            val isMiniseries = id != null && fixtureGenres.any { genre ->
                                id == 200000 + genre * 100
                            }
                            JSONObject().put("id", id ?: 0)
                                .put("type", if (isMiniseries) "Miniseries" else "Scripted")
                                .toString()
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
            // TMDB movie + TV entries per selected genre.
            serveRecentGenreTitles = true
            serveGenreMiniseries = true
            scratch.listFiles()?.forEach { it.delete() }
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
                } else {
                    compose.onAllNodesWithText("Tentar novamente").onFirst().performClick()
                }
                // The screen must query a real source and emit its own final
                // per-section snapshot, not a copied editorial implementation.
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
                            it.contains("titles=20")
                    } && logs.any {
                        it.contains("stage=editorial genre=$selectedId media=movie") &&
                            it.contains("titles=20")
                    } && logs.any {
                        it.contains("stage=tmdb genre=$tvId media=tv sourcePage=1 raw=20 parsed=20 accepted=20")
                    } && logs.any {
                        it.contains("stage=first genre=$tvId media=tv") &&
                            it.contains("titles=20")
                    } && logs.any {
                        it.contains("stage=editorial genre=$tvId media=tv") &&
                            it.contains("titles=20")
                    } && logs.any {
                        it.contains("stage=miniseries genre=$selectedId media=tv") &&
                            it.contains("candidates=20") &&
                            it.contains("confirmed=1")
                    } && logs.any {
                        it.contains("stage=render genre=$selectedId ") &&
                            it.contains("sections=") &&
                            it.contains("Populares:") &&
                            !it.contains("Populares:0") &&
                            Regex("Lançamentos · Filmes:([1-9][0-9]*)").containsMatchIn(it) &&
                            Regex("Lançamentos · Séries:([1-9][0-9]*)").containsMatchIn(it) &&
                            Regex("Minisséries imperdíveis:([1-9][0-9]*)").containsMatchIn(it)
                    }
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
                val sectionCount = actualSections.toMap()
                assertTrue("$label: recent movies must survive Cinema outage",
                    (sectionCount["Lançamentos · Filmes"] ?: 0) > 0)
                assertTrue("$label: recent TV must be represented",
                    (sectionCount["Lançamentos · Séries"] ?: 0) > 0)
                assertTrue("$label: Populares must retain the source catalogue",
                    (sectionCount["Populares"] ?: 0) > 0)
                assertEquals("$label: cannot invent theatrical screenings", 0,
                    sectionCount["No cinema"])
                assertEquals("$label: cannot invent future premieres", 0,
                    sectionCount["Em breve · Filmes e séries"])
                // First TV result has explicit TMDB type=Miniseries; it is absent
                // from every Home/global discovery fixture by construction.
                // RED on code134: genre-only verified miniseries are ignored.
                val miniId = 200000 + tvId * 100
                val metadataCalls = synchronized(seen) {
                    seen.count { it.startsWith("tv/$miniId?language=") }
                }
                println("ANYTV_GENRE_MINISERIES_TRACE label=$label genre=$tvId " +
                    "source=20 metadataCalls=$metadataCalls " +
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
                        it.contains("titles=20") })
                assertTrue("$label: editorial genre pool lost its candidates",
                    sourceStages.any { it.contains(
                        "stage=editorial genre=$selectedId media=movie") &&
                        it.contains("titles=20") })
                assertTrue("$label: TV source payload lost before parsing",
                    sourceStages.any { it.contains(
                        "stage=tmdb genre=$tvId media=tv sourcePage=1 raw=20 parsed=20 accepted=20") })
                assertTrue("$label: first TV genre page lost its candidates",
                    sourceStages.any { it.contains(
                        "stage=first genre=$tvId media=tv") &&
                        it.contains("titles=20") })
                assertTrue("$label: TV editorial genre pool lost its candidates",
                    sourceStages.any { it.contains(
                        "stage=editorial genre=$tvId media=tv") &&
                        it.contains("titles=20") })
                val recentFilms = Regex("Lançamentos · Filmes:([0-9]+)")
                    .find(snapshot)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val population = Regex("Populares:([0-9]+)")
                    .find(snapshot)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                assertTrue("$label: source must populate Populares first: $snapshot",
                    population > 0)
                println("ANYTV_ORIGINAL_GENRE_CINEMA_GUARD_REPRO label=$label popular=$population recentFilms=$recentFilms")
                if (recentFilms == 0) failedGenres.add(label)
            }
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
