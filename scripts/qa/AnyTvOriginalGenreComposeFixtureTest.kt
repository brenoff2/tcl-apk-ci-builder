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

    @Test fun actualScreenRetryDoesNotRefetchAlreadyCachedEmptyGenre() {
        URL.setURLStreamHandlerFactory(URLStreamHandlerFactory { protocol ->
            if (protocol != "https") return@URLStreamHandlerFactory null
            object : URLStreamHandler() {
                override fun openConnection(url: URL): URLConnection {
                    // Fail CLOSED on anything outside the canonical catalogue host.
                    if (url.host != host) throw SecurityException("QA blocks external HTTPS")
                    if (!url.path.startsWith("/v1/catalog/"))
                        throw SecurityException("QA blocks non-catalogue HTTPS")
                    val route = url.toString().substringAfter("/v1/catalog/")
                    seen.add(route)
                    val body = when {
                        route.startsWith("genre/movie/list") ->
                            """{"genres":[{"id":18,"name":"Drama"},{"id":28,"name":"Ação"}]}"""
                        route.startsWith("genre/tv/list") ->
                            """{"genres":[{"id":18,"name":"Drama"},{"id":10759,"name":"Ação"}]}"""
                        else -> """{"results":[],"total_pages":1,"genres":[]}"""
                    }
                    return object : HttpURLConnection(url) {
                        override fun connect() { connected = true }
                        override fun disconnect() { connected = false }
                        override fun usingProxy(): Boolean = false
                        override fun getResponseCode(): Int = HTTP_OK
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
            compose.onAllNodesWithText("Tentar novamente").onFirst().performClick()
            compose.waitForIdle()
            val after = synchronized(seen) {
                seen.count { it.contains("with_genres=18") }
            }
            // Known R03: retry effect fires, but populated genreShelves and
            // genreEditorialPools guards prevent reaching StreamingCatalog again.
            assertEquals("The existing Composable skips refetch on retry", initial, after)
            println("ANYTV_ORIGINAL_COMPOSE_R03_RETRY_NO_REFETCH_PROVED")
        } finally {
            root.set(null, prior)
            scratch.deleteRecursively()
        }
    }
}
