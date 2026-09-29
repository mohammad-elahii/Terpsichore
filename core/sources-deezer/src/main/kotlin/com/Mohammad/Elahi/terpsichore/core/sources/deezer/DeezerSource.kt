package com.Mohammad.Elahi.terpsichore.core.sources.deezer

import com.Mohammad.Elahi.terpsichore.core.sources.MusicSource
import com.Mohammad.Elahi.terpsichore.core.sources.SourceId
import com.Mohammad.Elahi.terpsichore.core.sources.Track
import io.ktor.client.HttpClient

class DeezerSource(private val client: HttpClient) : MusicSource {
    override val id = SourceId.DEEZER
    override val displayName = "Deezer"

    override suspend fun search(query: String, limit: Int): List<Track> = emptyList()

    override suspend fun previewUrl(trackId: String): String? = null
}
