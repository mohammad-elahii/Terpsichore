package com.Mohammad.Elahi.terpsichore.core.sources.archive

import com.Mohammad.Elahi.terpsichore.core.sources.MusicSource
import com.Mohammad.Elahi.terpsichore.core.sources.Quality
import com.Mohammad.Elahi.terpsichore.core.sources.SearchResult
import com.Mohammad.Elahi.terpsichore.core.sources.Source
import com.Mohammad.Elahi.terpsichore.core.sources.SourceException
import com.Mohammad.Elahi.terpsichore.core.sources.Track
import com.Mohammad.Elahi.terpsichore.core.sources.network.sourceCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.roundToInt

class ArchiveSource(private val client: HttpClient) : MusicSource {

    override val id = Source.ARCHIVE
    override val displayName = "Internet Archive"

    override suspend fun search(query: String, limit: Int): List<SearchResult> =
        sourceCall(id) {
            if (query.isBlank()) return@sourceCall emptyList()
            val docs = searchItems(query, limit)
            val results = mutableListOf<SearchResult>()
            for (doc in docs) {
                if (results.size >= limit) break
                val identifier = doc.identifier?.takeIf { it.isNotBlank() } ?: continue
                val itemTitle = doc.title.firstString() ?: continue
                val itemCreator = doc.creator.firstString()
                val files = try {
                    fetchAudioFiles(identifier)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    continue
                }
                for (file in files) {
                    if (results.size >= limit) break
                    val name = file.name ?: continue
                    val quality = file.quality()
                    results += SearchResult(
                        track = Track(
                            id = "$identifier/$name",
                            title = file.title?.takeIf { it.isNotBlank() }
                                ?: name.substringBeforeLast('.'),
                            artist = file.artist?.takeIf { it.isNotBlank() }
                                ?: itemCreator
                                ?: UNKNOWN_ARTIST,
                            album = itemTitle,
                            durationSeconds = parseLengthSeconds(file.length),
                            coverUrl = "$IMG_ENDPOINT/$identifier",
                            previewUrl = fileUrl(identifier, name),
                        ),
                        source = Source.ARCHIVE,
                        quality = quality,
                        downloadable = true,
                        downloadFormats = listOfNotNull(quality.takeIf { it != Quality.UNKNOWN }),
                    )
                }
            }
            results
        }

    override suspend fun previewUrl(trackId: String): String? {
        val parts = trackId.split("/", limit = 2)
        if (parts.size != 2) return null
        return fileUrl(parts[0], parts[1])
    }

    private suspend fun searchItems(query: String, limit: Int): List<ArchiveDocDto> {
        val terms = query.trim().split(WHITESPACE).joinToString(" AND ")
        val lucene = "(title:($terms) OR creator:($terms)) AND mediatype:audio"
        val response = client.get(SEARCH_ENDPOINT) {
            parameter("q", lucene)
            parameter("fl[]", "identifier")
            parameter("fl[]", "title")
            parameter("fl[]", "creator")
            parameter("fl[]", "downloads")
            parameter("sort[]", "downloads desc")
            parameter("rows", limit.coerceAtMost(MAX_ITEMS))
            parameter("page", 1)
            parameter("output", "json")
        }.body<ArchiveSearchResponseDto>()
        return response.response?.docs.orEmpty()
    }

    private suspend fun fetchAudioFiles(identifier: String): List<ArchiveFileDto> =
        client.get("$METADATA_ENDPOINT/$identifier")
            .body<ArchiveMetadataResponseDto>()
            .files
            .selectAudioFiles(MAX_TRACKS_PER_ITEM)

    private fun fileUrl(identifier: String, fileName: String): String =
        URLBuilder(
            protocol = URLProtocol.HTTPS,
            host = ARCHIVE_HOST,
            pathSegments = listOf("download", identifier, fileName),
        ).buildString()

    private fun ArchiveFileDto.quality(): Quality =
        when {
            name?.lowercase()?.endsWith(".flac") == true -> Quality.FLAC
            else -> Quality.UNKNOWN
        }

    private fun List<ArchiveFileDto>.selectAudioFiles(maxPerItem: Int): List<ArchiveFileDto> {
        data class Candidate(
            val file: ArchiveFileDto,
            val baseName: String,
            val priority: Int,
            val trackNo: Int,
        )

        val candidates = mapNotNull { file ->
            val name = file.name ?: return@mapNotNull null
            val lower = name.lowercase()
            if (lower.contains("_sample")) return@mapNotNull null
            val priority = when {
                lower.endsWith(".flac") -> PRIORITY_FLAC
                lower.endsWith(".mp3") && !isDerivative(lower) -> PRIORITY_MP3_FULL
                lower.endsWith(".ogg") -> PRIORITY_OGG
                lower.endsWith(".mp3") -> PRIORITY_MP3_DERIVATIVE
                else -> return@mapNotNull null
            }
            Candidate(
                file = file,
                baseName = name.substringBeforeLast('.').replace(DERIVATIVE_SUFFIX, ""),
                priority = priority,
                trackNo = file.track?.toIntOrNull() ?: Int.MAX_VALUE,
            )
        }
        return candidates
            .groupBy { it.baseName }
            .map { (_, group) -> group.minBy { it.priority } }
            .sortedWith(compareBy({ it.priority }, { it.trackNo }, { it.file.name }))
            .take(maxPerItem)
            .map { it.file }
    }

    private fun parseLengthSeconds(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        raw.toDoubleOrNull()?.let { return it.roundToInt() }
        val parts = raw.split(":").map { it.toIntOrNull() ?: return null }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    private fun JsonElement?.firstString(): String? = when (this) {
        is JsonPrimitive -> content.takeIf { it.isNotBlank() }
        is JsonArray -> mapNotNull { element ->
            (element as? JsonPrimitive)?.content?.takeIf { text -> text.isNotBlank() }
        }.firstOrNull()
        else -> null
    }

    companion object {
        private const val SEARCH_ENDPOINT = "https://archive.org/advancedsearch.php"
        private const val METADATA_ENDPOINT = "https://archive.org/metadata"
        private const val IMG_ENDPOINT = "https://archive.org/services/img"
        private const val ARCHIVE_HOST = "archive.org"
        private const val MAX_ITEMS = 8
        private const val MAX_TRACKS_PER_ITEM = 5
        private const val PRIORITY_FLAC = 0
        private const val PRIORITY_MP3_FULL = 1
        private const val PRIORITY_OGG = 2
        private const val PRIORITY_MP3_DERIVATIVE = 3
        private const val UNKNOWN_ARTIST = "Unknown artist"
        private val WHITESPACE = Regex("\\s+")
        private val DERIVATIVE_SUFFIX = Regex("_(64kb|128kb|vbr)$", RegexOption.IGNORE_CASE)

        private fun isDerivative(name: String): Boolean =
            name.contains("_64kb") || name.contains("_128kb") || name.contains("_vbr")
    }
}
