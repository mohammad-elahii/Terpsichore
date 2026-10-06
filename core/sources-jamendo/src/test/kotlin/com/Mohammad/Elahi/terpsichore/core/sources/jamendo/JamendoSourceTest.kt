package com.Mohammad.Elahi.terpsichore.core.sources.jamendo

import com.Mohammad.Elahi.terpsichore.core.sources.Quality
import com.Mohammad.Elahi.terpsichore.core.sources.Source
import com.Mohammad.Elahi.terpsichore.core.sources.SourceException
import com.Mohammad.Elahi.terpsichore.core.sources.SourceFailureReason
import com.Mohammad.Elahi.terpsichore.core.sources.network.TerpsichoreJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JamendoSourceTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val searchJson = """
        {
          "headers": {"status": "ok", "code": 0, "results_count": 3},
          "results": [
            {
              "id": "1109",
              "name": "Sunny Side Up",
              "duration": 189,
              "artist_name": "Ghost K",
              "album_name": "Stop The Blue",
              "album_image": "https://imgjam.com/albums/102/1/1.jpg",
              "audio": "https://mp3l.jamendo.com/?trackid=1109&format=mp32",
              "audiodownload": "https://prod-1.storage.jamendo.it/download/track/1109/mp32/",
              "audiodownload_allowed": true
            },
            { "id": "88", "name": null },
            { "id": "99", "name": "Orphan Song" }
          ]
        }
    """

    private fun sourceOf(
        handler: MockRequestHandler,
        clientId: String? = "test-key",
    ): Pair<JamendoSource, HttpClient> {
        val client = HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(TerpsichoreJson)
            }
        }
        return JamendoSource(client, clientId) to client
    }

    @Test
    fun searchMapsTracksToResults() = runBlocking {
        val (source, client) = sourceOf(handler = { request ->
            assertEquals("api.jamendo.com", request.url.host)
            assertEquals("/v3.0/tracks/", request.url.encodedPath)
            assertEquals("test-key", request.url.parameters["client_id"])
            assertEquals("json", request.url.parameters["format"])
            assertEquals("mp32", request.url.parameters["audioformat"])
            assertEquals("sunny", request.url.parameters["search"])
            assertEquals("25", request.url.parameters["limit"])
            respond(content = searchJson, headers = jsonHeaders)
        })

        val results = source.search("sunny")

        assertEquals(2, results.size)
        val first = results[0]
        assertEquals(Source.JAMENDO, first.source)
        assertEquals("1109", first.track.id)
        assertEquals("Sunny Side Up", first.track.title)
        assertEquals("Ghost K", first.track.artist)
        assertEquals("Stop The Blue", first.track.album)
        assertEquals(189, first.track.durationSeconds)
        assertEquals("https://imgjam.com/albums/102/1/1.jpg", first.track.coverUrl)
        assertEquals("https://mp3l.jamendo.com/?trackid=1109&format=mp32", first.track.previewUrl)
        assertEquals(Quality.FLAC, first.quality)
        assertTrue(first.downloadable)
        assertEquals(listOf(Quality.FLAC, Quality.MP3_320), first.downloadFormats)
        assertEquals("Unknown artist", results[1].track.artist)
        client.close()
    }

    @Test
    fun searchThrowsOnFailedHeaderStatus() = runBlocking {
        val errorJson = """
            {"headers": {"status": "failed", "code": 11,
             "error_message": "Jamendo Api Suspended Application Error"}, "results": []}
        """
        val (source, client) = sourceOf(handler = {
            respond(content = errorJson, headers = jsonHeaders)
        })

        val thrown = try {
            source.search("test")
            null
        } catch (t: Throwable) {
            t
        }

        assertTrue(thrown is SourceException)
        assertEquals(SourceFailureReason.BAD_REQUEST, (thrown as SourceException).reason)
        assertEquals(Source.JAMENDO, thrown.source)
        assertEquals("Jamendo Api Suspended Application Error", thrown.message)
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
    fun searchWithoutClientIdOmitsParam() = runBlocking {
        val (source, client) = sourceOf({ request ->
            assertEquals(null, request.url.parameters["client_id"])
            respond(content = searchJson, headers = jsonHeaders)
        }, clientId = null)

        source.search("sunny")

        client.close()
    }
}
