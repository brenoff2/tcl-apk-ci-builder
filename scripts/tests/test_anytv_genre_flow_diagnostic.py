#!/usr/bin/env python3
"""AnyTV genre-flow diagnostic: source-backed state models; NOT Android device E2E.

Tests run against canonical overrides/TclStreamingScreens.kt. Expected failures
document confirmed source-level gaps, without blocking release jobs. No API
calls, device access, deployment, secrets, or Kotlin modifications.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "overrides" / "TclStreamingScreens.kt"


class GenreFlowDiagnostic(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.screen = SOURCE.read_text(encoding="utf-8")
        cls.genre_effect = cls.screen.split(
            "LaunchedEffect(mainGenres, tvGenres, filterGenreIds, selectedHomeGenre,", 1
        )[1].split(
            "LaunchedEffect(selectedHomeGenre, homeContentType, genreSort, genreFirstPageRetry)", 1
        )[0]

    @unittest.expectedFailure
    def test_R01_section_has_a_non_null_navigation_transition(self):
        """Observed: only null assignments to openedSection, so full section not entered."""
        assignments = re.findall(
            r"\bopenedSection\s*=\s*(?![=])([^;\n}]+)", self.screen
        )
        self.assertTrue(assignments, "No section assignments found")
        self.assertTrue(
            any(value.strip() != "null" for value in assignments),
            "Only null assignments: section pagination is unreachable",
        )

    @unittest.expectedFailure
    def test_R02_default_genre_sections_exposes_deep_pagination(self):
        """Observed: default Seções is true, but genre Mostrar mais is list-only."""
        self.assertIn(
            'var genreSectionsMode by remember { mutableStateOf(true) }', self.screen
        )
        self.assertIn(
            "if (!genreSectionsMode || selectedHomeGenre == ANYTV_CINEMA_FILTER) {",
            self.screen
        )
        sections_mode = True
        is_cinema = False
        can_page_from_genre_list = (not sections_mode) or is_cinema
        self.assertTrue(
            can_page_from_genre_list,
            "No load-more CTA in default Seções for a regular genre",
        )

    @unittest.expectedFailure
    def test_R03_retry_refetches_an_existing_genre_key(self):
        """Observed: retry invalidates neither the genre shelf nor query key."""
        self.assertIn("if (key !in genreShelves) {", self.genre_effect)
        self.assertIn("genreFirstPageRetry", self.genre_effect)
        self.assertIn("genreFirstPageRetry++", self.screen)
        shelf = {"movie:18:popular": ["old-result"]}
        key = "movie:18:popular"
        should_fetch_after_retry = key not in shelf
        self.assertTrue(
            should_fetch_after_retry,
            "Retry increment with populated key skips network fetch",
        )

    @unittest.expectedFailure
    def test_R04_anime_discovery_retains_results_with_missing_genre_metadata(self):
        """Editorial-policy question: genre_ids loss can exclude valid upstream anime."""
        self.assertIn("with_genres=${if (id == ANYTV_ANIME_FILTER) 16 else id}", self.screen)
        self.assertIn('title.originalLanguage == "ja" && 16 in title.genreIds', self.screen)
        self.assertIn('id != ANYTV_ANIME_FILTER || anyTvIsAnime(it)', self.screen)
        original_language, genre_ids = "ja", []
        accepted = original_language == "ja" and 16 in genre_ids
        self.assertTrue(
            accepted,
            "with_genres=16 was applied upstream, missing genre_ids drops candidate",
        )

    def test_R05_disk_cache_retains_discover_responses_for_48_hours(self):
        """Cache retention is verified behavior, not evidence of the physical cause."""
        self.assertIn(
            'if (!remoteRating) StreamingDiskCache.read(route)?.let { return it }',
            self.screen
        )
        self.assertIn("else -> 48L * 3600000L", self.screen)
        self.assertIn('File(context.applicationContext.filesDir, "anytv_catalog_v1")',
                      self.screen)

    def test_R06_render_count_is_not_unique_visible_card_count(self):
        """Render diagnostic sums rail lengths instead of distinct visible IDs."""
        self.assertIn(
            '"visible=${uniqueShelves.sumOf { it.second.size }} "', self.screen
        )
        rails = {"Popular": ["movie:1", "movie:2"],
                 "Recommended": ["movie:1", "movie:3"]}
        counted = sum(len(v) for v in rails.values())
        distinct = len(set(t for titles in rails.values() for t in titles))
        self.assertEqual((counted, distinct), (4, 3))

    def test_R07_editorial_rail_is_capped_before_deep_catalogue(self):
        """Rail cutoff exists and does not prove the underlying source is empty."""
        self.assertIn('catalogue: List<StreamingTitle>, target: Int = 40', self.screen)
        self.assertIn('return backed.take(target)', self.screen)
        self.assertIn("private const val ANYTV_EDITORIAL_LIMIT = 50", self.screen)


if __name__ == "__main__":
    unittest.main()
