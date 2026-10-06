package com.Mohammad.Elahi.terpsichore.core.sources.deezer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeezerErrorDto(
    val type: String? = null,
    val message: String? = null,
    val code: Int? = null,
)

@Serializable
internal data class DeezerSearchResponseDto(
    val data: List<DeezerTrackDto> = emptyList(),
    val error: DeezerErrorDto? = null,
)

@Serializable
internal data class DeezerTrackDto(
    val id: Long? = null,
    val title: String? = null,
    @SerialName("title_short") val titleShort: String? = null,
    val isrc: String? = null,
    val duration: Int? = null,
    val preview: String? = null,
    val artist: DeezerArtistDto? = null,
    val album: DeezerAlbumDto? = null,
    val error: DeezerErrorDto? = null,
)

@Serializable
internal data class DeezerArtistDto(
    val id: Long? = null,
    val name: String? = null,
)

@Serializable
internal data class DeezerAlbumDto(
    val id: Long? = null,
    val title: String? = null,
    @SerialName("cover_medium") val coverMedium: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null,
)
