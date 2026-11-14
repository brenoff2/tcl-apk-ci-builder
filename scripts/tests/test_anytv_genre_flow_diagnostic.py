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

    def test_R01_removed_section_navigation_is_not_an_active_feature(self):
        """Prior decision removed per-section CTA; old section branch is dormant."""
        assignments = [
            line.split("openedSection =", 1)[1].strip()
            for line in self.screen.splitlines()
            if "openedSection =" in line and "openedSection ==" not in line
        ]
        self.assertTrue(assignments, "Expected state resets in source")
        self.assertFalse(
            any(value.strip() != "null" for value in assignments),
            "Unexpected non-null section open after CTA removal",
        )

    def test_R02_default_sections_and_list_paging_match_approved_design(self):
        """Seções are editorials; pagination is explicitly exclusive to Lista."""
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
        self.assertFalse(
            can_page_from_genre_list,
            "UI gate changed: Seções unexpectedly exposes Lista pagination",
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
