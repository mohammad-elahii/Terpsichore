package com.Mohammad.Elahi.terpsichore.core.sources.archive

import com.Mohammad.Elahi.terpsichore.core.sources.MusicSource
import com.Mohammad.Elahi.terpsichore.core.sources.SourceId
import com.Mohammad.Elahi.terpsichore.core.sources.Track
import io.ktor.client.HttpClient

class ArchiveSource(private val client: HttpClient) : MusicSource {
    override val id = SourceId.ARCHIVE
    override val displayName = "Internet Archive"

    override suspend fun search(query: String, limit: Int): List<Track> = emptyList()

    override suspend fun previewUrl(trackId: String): String? = null
}
