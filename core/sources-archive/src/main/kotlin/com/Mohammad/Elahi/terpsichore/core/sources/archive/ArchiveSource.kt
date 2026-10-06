package com.Mohammad.Elahi.terpsichore.core.sources.archive

import com.Mohammad.Elahi.terpsichore.core.sources.MusicSource
import com.Mohammad.Elahi.terpsichore.core.sources.SearchResult
import com.Mohammad.Elahi.terpsichore.core.sources.Source
import io.ktor.client.HttpClient

class ArchiveSource(private val client: HttpClient) : MusicSource {
    override val id = Source.ARCHIVE
    override val displayName = "Internet Archive"

    override suspend fun search(query: String, limit: Int): List<SearchResult> = emptyList()

    override suspend fun previewUrl(trackId: String): String? = null
}
