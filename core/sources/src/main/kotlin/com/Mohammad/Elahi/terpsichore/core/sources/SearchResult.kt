package com.Mohammad.Elahi.terpsichore.core.sources

data class SearchResult(
    val track: Track,
    val source: Source,
    val quality: Quality = Quality.UNKNOWN,
    val downloadable: Boolean = false,
)
