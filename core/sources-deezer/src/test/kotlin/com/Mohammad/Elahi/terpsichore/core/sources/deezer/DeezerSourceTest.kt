package com.Mohammad.Elahi.terpsichore.core.sources.deezer

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeezerSourceTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val searchJson = """
        {
          "data": [
            {
              "id": 3135556,
              "title": "Harder, Better, Faster, Stronger",
              "isrc": "GBDUW0000059",
              "duration": 224,
              "preview": "https://cdns-preview-d.dzcdn.net/stream/c-deda3fa0",
              "artist": { "id": 27, "name": "Daft Punk" },
              "album": {
                "id": 302127,
                "title": "Discovery",
                "cover_medium": "https://e-cdns-images.dzcdn.net/images/cover/250x250.jpg"
              }
            },
            { "id": 999, "title": null },
            { "id": 888, "title": "Orphan Track" }
          ],
          "total": 3
        }
    """

    private fun sourceOf(handler: MockRequestHandler): Pair<DeezerSource, HttpClient> {
        val client = HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) {
                json(TerpsichoreJson)
            }
        }
        return DeezerSource(client) to client
    }

    @Test
    fun searchMapsTracksToResults() = runBlocking {
        val (source, client) = sourceOf { request ->
            assertEquals("api.deezer.com", request.url.host)
            assertEquals("/search", request.url.encodedPath)
            assertEquals("daft punk", request.url.parameters["q"])
            assertEquals("25", request.url.parameters["limit"])
            respond(content = searchJson, headers = jsonHeaders)
        }

        val results = source.search("daft punk")

        assertEquals(2, results.size)
        val first = results[0]
        assertEquals(Source.DEEZER, first.source)
        assertEquals("3135556", first.track.id)
        assertEquals("Harder, Better, Faster, Stronger", first.track.title)
        assertEquals("Daft Punk", first.track.artist)
        assertEquals("Discovery", first.track.album)
        assertEquals(224, first.track.durationSeconds)
        assertEquals("GBDUW0000059", first.track.isrc)
        assertEquals("https://cdns-preview-d.dzcdn.net/stream/c-deda3fa0", first.track.previewUrl)
        assertEquals("https://e-cdns-images.dzcdn.net/images/cover/250x250.jpg", first.track.coverUrl)
        assertTrue(!first.downloadable)
        assertEquals("Unknown artist", results[1].track.artist)
        client.close()
    }

    @Test
    fun searchThrowsOnApiErrorBody() = runBlocking {
        val errorJson = """{"error": {"type": "Exception", "message": "Quota limit exceeded", "code": 4}}"""
        val (source, client) = sourceOf {
            respond(content = errorJson, headers = jsonHeaders)
        }

        val thrown = try {
            source.search("test")
            null
        } catch (t: Throwable) {
            t
        }

        assertTrue(thrown is SourceException)
        assertEquals(SourceFailureReason.RATE_LIMITED, (thrown as SourceException).reason)
        assertEquals(Source.DEEZER, thrown.source)
        assertEquals("Quota limit exceeded", thrown.message)
        client.close()
    }

    @Test
    fun searchPropagatesCancellation() = runBlocking {
        val (source, client) = sourceOf { throw CancellationException("cancelled") }

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
    fun previewUrlFetchesTrackDetail() = runBlocking {
        val detailJson = """{"id": 3135556, "title": "Harder, Better", "preview": "https://preview.example.com/1.mp3"}"""
        val (source, client) = sourceOf { request ->
            assertEquals("/track/3135556", request.url.encodedPath)
            respond(content = detailJson, headers = jsonHeaders)
        }

        assertEquals("https://preview.example.com/1.mp3", source.previewUrl("3135556"))
        client.close()
    }

    @Test
    fun previewUrlReturnsNullWhenMissing() = runBlocking {
        val detailJson = """{"id": 3135556, "title": "No Preview Here"}"""
        val (source, client) = sourceOf {
            respond(content = detailJson, headers = jsonHeaders)
        }

        assertNull(source.previewUrl("3135556"))
        client.close()
    }
}
