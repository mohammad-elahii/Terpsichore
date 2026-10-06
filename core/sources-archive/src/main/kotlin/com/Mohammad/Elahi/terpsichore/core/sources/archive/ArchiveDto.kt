package com.Mohammad.Elahi.terpsichore.core.sources.archive

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class ArchiveSearchResponseDto(
    val response: ArchiveResponseDto? = null,
)

@Serializable
internal data class ArchiveResponseDto(
    val numFound: Int? = null,
    val docs: List<ArchiveDocDto> = emptyList(),
)

@Serializable
internal data class ArchiveDocDto(
    val identifier: String? = null,
    val title: JsonElement? = null,
    val creator: JsonElement? = null,
    val downloads: Long? = null,
)

@Serializable
internal data class ArchiveMetadataResponseDto(
    val files: List<ArchiveFileDto> = emptyList(),
)

@Serializable
internal data class ArchiveFileDto(
    val name: String? = null,
    val format: String? = null,
    val length: String? = null,
    val track: String? = null,
    val title: String? = null,
    val artist: String? = null,
)
