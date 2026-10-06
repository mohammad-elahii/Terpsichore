package com.Mohammad.Elahi.terpsichore.core.sources.archive

import com.Mohammad.Elahi.terpsichore.core.sources.Quality
import com.Mohammad.Elahi.terpsichore.core.sources.Source
import com.Mohammad.Elahi.terpsichore.core.sources.SourceException
import com.Mohammad.Elahi.terpsichore.core.sources.SourceFailureReason
import com.Mohammad.Elahi.terpsichore.core.sources.network.StatusValidator
import com.Mohammad.Elahi.terpsichore.core.sources.network.TerpsichoreJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveSourceTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val searchJson = """
        {
          "responseHeader": {"status": 0},
          "response": {
            "numFound": 2,
            "start": 0,
            "docs": [
              {"identifier": "gooditem", "title": "Good Album",
               "creator": "The Archive Band", "downloads": 123},
              {"identifier": "baditem", "title": "Bad Album",
               "creator": "Gone Band", "downloads": 10}
            ]
          }
        }
    """

    private val goodMetadataJson = """
        {
          "metadata": {"title": "Good Album", "creator": "The Archive Band"},
          "files": [
            {"name": "Track01.flac", "format": "Flac", "length": "212.5",
             "track": "1", "title": "First Song"},
            {"name": "Track01_vbr.mp3", "format": "VBR MP3", "length": "212.44", "track": "1"},
            {"name": "Track02.mp3", "format": "MP3", "length": "3:05",
             "track": "2", "artist": "Guest Artist"},
            {"name": "Track01_64kb.mp3", "format": "64Kbps MP3", "track": "1"},
            {"name": "itemimage.png", "format": "PNG"},
            {"name": "notes.txt", "format": "Text"}
          ]
        }
    """

    private fun sourceOf(handler: MockRequestHandler): Pair<ArchiveSource, HttpClient> {
        val client = HttpClient(MockEngine(handler)) {
            install(StatusValidator)
            install(ContentNegotiation) {
                json(TerpsichoreJson)
            }
        }
        return ArchiveSource(client) to client
    }

    @Test
    fun searchMapsAudioFilesToResults() = runBlocking {
        val (source, client) = sourceOf(handler = { request ->
            when {
                request.url.encodedPath == "/advancedsearch.php" -> {
                    assertEquals("archive.org", request.url.host)
                    assertTrue(request.url.parameters["q"]!!.contains("mediatype:audio"))
                    assertEquals("8", request.url.parameters["rows"])
                    assertEquals("json", request.url.parameters["output"])
                    respond(content = searchJson, headers = jsonHeaders)
                }
                request.url.encodedPath == "/metadata/gooditem" ->
                    respond(content = goodMetadataJson, headers = jsonHeaders)
                request.url.encodedPath == "/metadata/baditem" ->
                    respondError(HttpStatusCode.NotFound)
                else -> respond(content = "{}", headers = jsonHeaders)
            }
        })

        val results = source.search("autumn leaves")

        assertEquals(2, results.size)
        val first = results[0]
        assertEquals(Source.ARCHIVE, first.source)
        assertEquals("gooditem/Track01.flac", first.track.id)
        assertEquals("First Song", first.track.title)
        assertEquals("The Archive Band", first.track.artist)
        assertEquals("Good Album", first.track.album)
        assertEquals(213, first.track.durationSeconds)
        assertEquals("https://archive.org/services/img/gooditem", first.track.coverUrl)
        assertEquals("https://archive.org/download/gooditem/Track01.flac", first.track.previewUrl)
        assertEquals(Quality.FLAC, first.quality)
        assertTrue(first.downloadable)
        assertEquals(listOf(Quality.FLAC), first.downloadFormats)
        assertEquals("https://archive.org/download/gooditem/Track02.mp3", results[1].track.previewUrl)
        assertEquals("Guest Artist", results[1].track.artist)
        assertEquals(185, results[1].track.durationSeconds)
        assertEquals(Quality.UNKNOWN, results[1].quality)
        client.close()
    }

    @Test
    fun searchThrowsOnHttpError() = runBlocking {
        val (source, client) = sourceOf(handler = {
            respondError(HttpStatusCode.InternalServerError)
        })

        val thrown = try {
            source.search("test")
            null
        } catch (t: Throwable) {
            t
        }

        assertTrue(thrown is SourceException)
        assertEquals(SourceFailureReason.SERVER_ERROR, (thrown as SourceException).reason)
        client.close()
    }

    @Test
    fun searchPropagatesCancellation() = runBlocking {
        val (source, client) = sourceOf(handler = { throw CancellationException("cancelled") })

        val thrown = try {
            source.search("test")
            null
        } catch (t: Throwable) {
            t
        }

        assertTrue(thrown is CancellationException)
        client.close()
    }

    @Test
    fun previewUrlBuildsEncodedDirectLink() = runBlocking {
        val (source, client) = sourceOf(handler = { respond(content = "{}", headers = jsonHeaders) })

        assertEquals(
            "https://archive.org/download/someid/Some%20File.flac",
            source.previewUrl("someid/Some File.flac"),
        )
        assertEquals(null, source.previewUrl("noseparator"))
        client.close()
    }
}
