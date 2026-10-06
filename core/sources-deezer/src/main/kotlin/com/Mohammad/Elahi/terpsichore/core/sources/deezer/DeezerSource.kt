package com.Mohammad.Elahi.terpsichore.core.sources.deezer

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

class DeezerSource(private val client: HttpClient) : MusicSource {

    override val id = Source.DEEZER
    override val displayName = "Deezer"

    override suspend fun search(query: String, limit: Int): List<SearchResult> =
        sourceCall(id) {
            val response = client.get(SEARCH_ENDPOINT) {
                parameter("q", query)
                parameter("limit", limit)
            }.body<DeezerSearchResponseDto>()
            response.error?.let { throw it.toSourceException() }
            response.data.mapNotNull { track -> track.toSearchResult() }
        }

    override suspend fun previewUrl(trackId: String): String? =
        sourceCall(id) {
            val response = client.get("$TRACK_ENDPOINT/$trackId").body<DeezerTrackDto>()
            response.error?.let { throw it.toSourceException() }
            response.preview
        }

    private fun DeezerErrorDto.toSourceException(): SourceException =
        SourceException(
            source = Source.DEEZER,
            reason = if (code == QUOTA_EXCEEDED_CODE) {
                SourceFailureReason.RATE_LIMITED
            } else {
                SourceFailureReason.SERVER_ERROR
            },
            message = message ?: "Deezer API error (code $code)",
        )

    private fun DeezerTrackDto.toSearchResult(): SearchResult? {
        val trackId = id ?: return null
        val title = title?.takeIf { it.isNotBlank() } ?: return null
        return SearchResult(
            track = Track(
                id = trackId.toString(),
                title = title,
                artist = artist?.name?.takeIf { it.isNotBlank() } ?: UNKNOWN_ARTIST,
                album = album?.title,
                durationSeconds = duration,
                isrc = isrc,
                coverUrl = album?.coverMedium ?: album?.coverXl,
                previewUrl = preview,
            ),
            source = Source.DEEZER,
            quality = Quality.UNKNOWN,
            downloadable = false,
        )
    }

    companion object {
        private const val SEARCH_ENDPOINT = "https://api.deezer.com/search"
        private const val TRACK_ENDPOINT = "https://api.deezer.com/track"
        private const val QUOTA_EXCEEDED_CODE = 4
        private const val UNKNOWN_ARTIST = "Unknown artist"
    }
}
