package com.Mohammad.Elahi.terpsichore.core.sources

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Int? = null,
    val isrc: String? = null,
    val coverUrl: String? = null,
    val previewUrl: String? = null,
)
