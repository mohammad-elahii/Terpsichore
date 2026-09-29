package com.Mohammad.Elahi.terpsichore.core.sources

data class Track(
    val id: String,
    val source: SourceId,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Int? = null,
    val coverUrl: String? = null,
    val previewUrl: String? = null,
    val quality: Quality = Quality.UNKNOWN,
    val downloadable: Boolean = true,
)
