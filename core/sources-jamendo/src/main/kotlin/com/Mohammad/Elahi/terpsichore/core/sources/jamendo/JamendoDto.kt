package com.Mohammad.Elahi.terpsichore.core.sources.jamendo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class JamendoHeadersDto(
    val status: String? = null,
    val code: Int? = null,
    @SerialName("error_message") val errorMessage: String? = null,
)

@Serializable
internal data class JamendoSearchResponseDto(
    val headers: JamendoHeadersDto? = null,
    val results: List<JamendoTrackDto> = emptyList(),
)

@Serializable
internal data class JamendoTrackDto(
    val id: String? = null,
    val name: String? = null,
    val duration: String? = null,
    @SerialName("artist_name") val artistName: String? = null,
    @SerialName("album_name") val albumName: String? = null,
    @SerialName("album_image") val albumImage: String? = null,
    val image: String? = null,
    val audio: String? = null,
    val audiodownload: String? = null,
    @SerialName("audiodownload_allowed") val audiodownloadAllowed: Boolean? = null,
    @SerialName("license_ccurl") val licenseCcurl: String? = null,
)
