package com.Mohammad.Elahi.terpsichore.core.sources.jamendo

import com.Mohammad.Elahi.terpsichore.core.sources.MusicSource
import com.Mohammad.Elahi.terpsichore.core.sources.Quality
import com.Mohammad.Elahi.terpsichore.core.sources.SearchResult
import com.Mohammad.Elahi.terpsichore.core.sources.Source
import com.Mohammad.Elahi.terpsichore.core.sources.SourceException
import com.Mohammad.Elahi.terpsichore.core.sources.SourceFailureReason
import com.Mohammad.Elahi.terpsichore.core.sources.Track
import com.Mohammad.Elahi.terpsichore.core.sources.network.sourceCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class JamendoSource(
    private val client: HttpClient,
    private val clientId: String? = null,
) : MusicSource {

    override val id = Source.JAMENDO
    override val displayName = "Jamendo"

    override suspend fun search(query: String, limit: Int): List<SearchResult> =
        sourceCall(id) {
            val response = client.get(SEARCH_ENDPOINT) {
                clientId?.let { parameter("client_id", it) }
                parameter("format", JSON_FORMAT)
                parameter("limit", limit)
                parameter("search", query)
                parameter("audioformat", MP3_320_FORMAT)
            }.body<JamendoSearchResponseDto>()
            response.headers?.takeIf { it.status == STATUS_FAILED }?.let { throw it.toSourceException() }
            response.results.mapNotNull { track -> track.toSearchResult() }
        }

    private fun JamendoHeadersDto.toSourceException(): SourceException =
        SourceException(
            source = Source.JAMENDO,
            reason = if (code == MISSING_CLIENT_ID || code == SUSPENDED_APP) {
                SourceFailureReason.BAD_REQUEST
            } else {
                SourceFailureReason.SERVER_ERROR
            },
            message = errorMessage ?: "Jamendo API error (code $code)",
        )

    private fun JamendoTrackDto.toSearchResult(): SearchResult? {
        val trackId = id?.takeIf { it.isNotBlank() } ?: return null
        val title = name?.takeIf { it.isNotBlank() } ?: return null
        val downloadable = audiodownloadAllowed ?: true
        return SearchResult(
            track = Track(
                id = trackId,
                title = title,
                artist = artistName?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
                album = albumName,
                durationSeconds = duration,
                coverUrl = albumImage ?: image,
                previewUrl = audio,
            ),
            source = Source.JAMENDO,
            quality = if (downloadable) Quality.FLAC else Quality.UNKNOWN,
            downloadable = downloadable,
            downloadFormats = if (downloadable) {
                listOf(Quality.FLAC, Quality.MP3_320)
            } else {
                emptyList()
            },
        )
    }

    companion object {
        private const val SEARCH_ENDPOINT = "https://api.jamendo.com/v3.0/tracks/"
        private const val JSON_FORMAT = "json"
        private const val MP3_320_FORMAT = "mp32"
        private const val STATUS_FAILED = "failed"
        private const val MISSING_CLIENT_ID = 5
        private const val SUSPENDED_APP = 11
        private const val UNKNOWN_ARTIST = "Unknown artist"
    }
}
