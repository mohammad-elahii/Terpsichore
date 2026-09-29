package com.Mohammad.Elahi.terpsichore.core.sources

interface MusicSource {
    val id: SourceId
    val displayName: String

    suspend fun search(query: String, limit: Int = 25): List<Track>

    suspend fun previewUrl(trackId: String): String? = null
}
