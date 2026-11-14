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
