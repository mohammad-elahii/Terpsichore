package com.Mohammad.Elahi.terpsichore.core.sources.deezer

import com.Mohammad.Elahi.terpsichore.core.sources.MusicSource
import com.Mohammad.Elahi.terpsichore.core.sources.SearchResult
import com.Mohammad.Elahi.terpsichore.core.sources.Source
import io.ktor.client.HttpClient

class DeezerSource(private val client: HttpClient) : MusicSource {
    override val id = Source.DEEZER
    override val displayName = "Deezer"

    override suspend fun search(query: String, limit: Int): List<SearchResult> = emptyList()

    override suspend fun previewUrl(trackId: String): String? = null
}
