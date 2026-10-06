package com.Mohammad.Elahi.terpsichore.core.sources

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchAggregatorTest {

    private class FakeSource(
        override val id: Source,
        override val displayName: String,
        private val results: List<SearchResult> = emptyList(),
        private val error: Throwable? = null,
    ) : MusicSource {
        override suspend fun search(query: String, limit: Int): List<SearchResult> {
            error?.let { throw it }
            return results
        }
    }

    private fun result(source: Source, id: String, title: String, artist: String) =
        SearchResult(
            track = Track(id = id, title = title, artist = artist),
            source = source,
        )

    @Test
    fun interleavesResultsAcrossSources() = runBlocking {
        val aggregator = SearchAggregator(
            listOf(
                FakeSource(
                    Source.DEEZER, "Deezer",
                    listOf(
                        result(Source.DEEZER, "d1", "A", "X"),
                        result(Source.DEEZER, "d2", "B", "Y"),
                    ),
                ),
                FakeSource(
                    Source.JAMENDO, "Jamendo",
                    listOf(result(Source.JAMENDO, "j1", "C", "Z")),
                ),
                FakeSource(
                    Source.ARCHIVE, "Archive",
                    listOf(
                        result(Source.ARCHIVE, "a1", "D", "W"),
                        result(Source.ARCHIVE, "a2", "E", "V"),
                    ),
                ),
            )
        )

        val merged = aggregator.search("query")

        assertEquals(listOf("d1", "j1", "a1", "d2", "a2"), merged.map { it.track.id })
    }

    @Test
    fun deduplicatesFuzzyMatchesAcrossSourcesKeepingFirstSource() = runBlocking {
        val aggregator = SearchAggregator(
            listOf(
                FakeSource(
                    Source.DEEZER, "Deezer",
                    listOf(
                        result(Source.DEEZER, "d1", "Harder, Better, Faster, Stronger", "Daft Punk"),
                    ),
                ),
                FakeSource(
                    Source.JAMENDO, "Jamendo",
                    listOf(
                        result(Source.JAMENDO, "j1", "Harder Better Faster Stronger", "Daft Punk"),
                        result(Source.JAMENDO, "j2", "Something Else", "Other Band"),
                    ),
                ),
            )
        )

        val merged = aggregator.search("daft")

        assertEquals(listOf("d1", "j2"), merged.map { it.track.id })
        assertEquals(Source.DEEZER, merged[0].source)
    }

    @Test
    fun keepsDuplicatesWithinTheSameSource() = runBlocking {
        val aggregator = SearchAggregator(
            listOf(
                FakeSource(
                    Source.DEEZER, "Deezer",
                    listOf(
                        result(Source.DEEZER, "d1", "Same Title", "Same Artist"),
                        result(Source.DEEZER, "d2", "Same Title", "Same Artist"),
                    ),
                ),
            )
        )

        val merged = aggregator.search("same")

        assertEquals(listOf("d1", "d2"), merged.map { it.track.id })
    }

    @Test
    fun skipsFailingSources() = runBlocking {
        val aggregator = SearchAggregator(
            listOf(
                FakeSource(
                    Source.DEEZER, "Deezer",
                    listOf(result(Source.DEEZER, "d1", "A", "X")),
                ),
                FakeSource(
                    Source.JAMENDO, "Jamendo",
                    error = SourceException(Source.JAMENDO, SourceFailureReason.SERVER_ERROR, "down"),
                ),
                FakeSource(
                    Source.ARCHIVE, "Archive",
                    listOf(result(Source.ARCHIVE, "a1", "B", "Y")),
                ),
            )
        )

        val merged = aggregator.search("query")

        assertEquals(listOf("d1", "a1"), merged.map { it.track.id })
    }

    @Test
    fun blankQueryReturnsEmptyList() = runBlocking {
        val aggregator = SearchAggregator(
            listOf(FakeSource(Source.DEEZER, "Deezer", listOf(result(Source.DEEZER, "d1", "A", "X"))))
        )

        assertTrue(aggregator.search("   ").isEmpty())
    }

    @Test
    fun cancellationPropagates() = runBlocking {
        val aggregator = SearchAggregator(
            listOf(FakeSource(Source.DEEZER, "Deezer", error = CancellationException("cancelled")))
        )

        val thrown = try {
            aggregator.search("query")
            null
        } catch (t: Throwable) {
            t
        }

        assertTrue(thrown is CancellationException)
    }
}

class FuzzyMatchTest {

    @Test
    fun normalizeStripsDiacriticsPunctuationAndCase() {
        assertEquals("beyonce halo feat someone", FuzzyMatch.normalize("Beyoncé — Halo (feat. Someone)"))
        assertEquals("ac dc back in black", FuzzyMatch.normalize("AC/DC — Back In Black"))
    }

    @Test
    fun sameTrackWithDiacriticsAndPunctuation() {
        assertTrue(
            FuzzyMatch.isSameTrack(
                "Beyoncé — Halo (feat. X)",
                "Beyoncé",
                "Beyonce Halo",
                "beyonce",
            )
        )
    }

    @Test
    fun artistPrefixTheIsIgnored() {
        assertTrue(
            FuzzyMatch.isSameTrack("Yesterday", "The Beatles", "Yesterday", "Beatles")
        )
    }

    @Test
    fun differentArtistsAreNotTheSameTrack() {
        assertFalse(
            FuzzyMatch.isSameTrack("Halo", "Beyoncé", "Halo", "Deftones")
        )
    }

    @Test
    fun veryDifferentTitlesAreNotTheSameTrack() {
        assertFalse(
            FuzzyMatch.isSameTrack("Revolution 9", "The Beatles", "Yesterday", "The Beatles")
        )
    }
}
