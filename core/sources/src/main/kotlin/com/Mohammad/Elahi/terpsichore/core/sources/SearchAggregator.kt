package com.Mohammad.Elahi.terpsichore.core.sources

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class SearchAggregator(private val sources: List<MusicSource>) {

    suspend fun search(query: String, limitPerSource: Int = 25): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val perSource = coroutineScope {
            sources.map { source ->
                async {
                    try {
                        source.search(query, limitPerSource)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
        }.awaitAll()
        return interleave(perSource).fold(emptyList()) { accepted, candidate ->
            val isDuplicate = accepted.any { acceptedEntry ->
                acceptedEntry.source != candidate.source &&
                    FuzzyMatch.isSameTrack(
                        acceptedEntry.track.title,
                        acceptedEntry.track.artist,
                        candidate.track.title,
                        candidate.track.artist,
                    )
            }
            if (isDuplicate) accepted else accepted + candidate
        }
    }

    private fun interleave(lists: List<List<SearchResult>>): List<SearchResult> {
        val iterators = lists.map { it.iterator() }
        val result = mutableListOf<SearchResult>()
        var round = iterators.filter { it.hasNext() }
        while (round.isNotEmpty()) {
            for (iterator in round) {
                if (iterator.hasNext()) result += iterator.next()
            }
            round = round.filter { it.hasNext() }
        }
        return result
    }
}

internal object FuzzyMatch {

    private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")
    private val combiningMarks = Regex("\\p{Mn}+")
    private const val SIMILARITY_THRESHOLD = 0.85
    private const val MIN_CONTAINS_LENGTH = 4

    fun normalize(text: String): String =
        java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKD)
            .replace(combiningMarks, "")
            .lowercase()
            .replace(nonAlphanumeric, " ")
            .trim()

    fun isSameTrack(
        aTitle: String,
        aArtist: String,
        bTitle: String,
        bArtist: String,
    ): Boolean = similar(normalize(aTitle), normalize(bTitle)) &&
        similar(normalize(aArtist), normalize(bArtist))

    private fun similar(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return a == b
        if (a == b) return true
        val shorter = if (a.length <= b.length) a else b
        val longer = if (a.length <= b.length) b else a
        if (shorter.length >= MIN_CONTAINS_LENGTH && longer.contains(shorter)) return true
        return levenshteinRatio(a, b) >= SIMILARITY_THRESHOLD
    }

    private fun levenshteinRatio(a: String, b: String): Double =
        1.0 - levenshtein(a, b).toDouble() / maxOf(a.length, b.length)

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(
                    previous[j + 1] + 1,
                    current[j] + 1,
                    previous[j] + cost,
                )
            }
            System.arraycopy(current, 0, previous, 0, current.size)
        }
        return previous[b.length]
    }
}
