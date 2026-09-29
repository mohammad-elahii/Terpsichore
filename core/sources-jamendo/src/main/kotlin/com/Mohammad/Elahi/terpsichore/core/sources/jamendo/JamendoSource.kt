package com.Mohammad.Elahi.terpsichore.core.sources.jamendo

import com.Mohammad.Elahi.terpsichore.core.sources.MusicSource
import com.Mohammad.Elahi.terpsichore.core.sources.SourceId
import com.Mohammad.Elahi.terpsichore.core.sources.Track
import io.ktor.client.HttpClient

class JamendoSource(
    private val client: HttpClient,
    private val clientId: String? = null,
) : MusicSource {
    override val id = SourceId.JAMENDO
    override val displayName = "Jamendo"

    override suspend fun search(query: String, limit: Int): List<Track> = emptyList()

    override suspend fun previewUrl(trackId: String): String? = null
}
