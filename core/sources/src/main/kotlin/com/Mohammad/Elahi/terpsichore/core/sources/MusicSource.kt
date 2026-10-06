package com.Mohammad.Elahi.terpsichore.core.sources

interface MusicSource {
    val id: Source
    val displayName: String

    suspend fun search(query: String, limit: Int = 25): List<SearchResult>

    suspend fun previewUrl(trackId: String): String? = null
}
