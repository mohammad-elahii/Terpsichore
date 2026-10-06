package com.Mohammad.Elahi.terpsichore.core.sources

import com.Mohammad.Elahi.terpsichore.core.sources.network.mapToSourceException
import com.Mohammad.Elahi.terpsichore.core.sources.network.sourceCall
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.UnknownHostException

class SourceCallTest {

    private inline fun <T> callAndCatch(block: () -> T): Throwable? =
        try {
            block()
            null
        } catch (t: Throwable) {
            t
        }

    private fun statusClient(status: HttpStatusCode): HttpClient =
        HttpClient(MockEngine { respondError(status) }) {
            expectSuccess = true
        }

    @Test
    fun mapsHttp429ToRateLimited() = runBlocking {
        val client = statusClient(HttpStatusCode.TooManyRequests)
        val thrown = callAndCatch { sourceCall(Source.DEEZER) { client.get("https://example.com") } }
        assertTrue(thrown is SourceException)
        assertEquals(SourceFailureReason.RATE_LIMITED, (thrown as SourceException).reason)
        assertEquals(Source.DEEZER, thrown.source)
        client.close()
    }

    @Test
    fun mapsHttp404ToNotFound() = runBlocking {
        val client = statusClient(HttpStatusCode.NotFound)
        val thrown = callAndCatch { sourceCall(Source.DEEZER) { client.get("https://example.com") } }
        assertEquals(SourceFailureReason.NOT_FOUND, (thrown as SourceException).reason)
        client.close()
    }

    @Test
    fun mapsHttp500ToServerError() = runBlocking {
        val client = statusClient(HttpStatusCode.InternalServerError)
        val thrown = callAndCatch { sourceCall(Source.DEEZER) { client.get("https://example.com") } }
        assertEquals(SourceFailureReason.SERVER_ERROR, (thrown as SourceException).reason)
        client.close()
    }

    @Test
    fun mapsConnectTimeoutToTimeout() {
        val e = mapToSourceException(Source.DEEZER, ConnectTimeoutException("connect timed out"))
        assertEquals(SourceFailureReason.TIMEOUT, e.reason)
    }

    @Test
    fun mapsRequestTimeoutToTimeout() {
        val e = mapToSourceException(Source.DEEZER, HttpRequestTimeoutException("https://example.com", 20_000L))
        assertEquals(SourceFailureReason.TIMEOUT, e.reason)
    }

    @Test
    fun mapsUnknownHostToNoNetwork() {
        val e = mapToSourceException(Source.JAMENDO, UnknownHostException("api.jamendo.com"))
        assertEquals(SourceFailureReason.NO_NETWORK, e.reason)
        assertEquals(Source.JAMENDO, e.source)
    }

    @Test
    fun mapsSerializationExceptionToParseError() {
        val e = mapToSourceException(Source.ARCHIVE, SerializationException("unexpected token"))
        assertEquals(SourceFailureReason.PARSE_ERROR, e.reason)
    }

    @Test
    fun mapsJsonConvertExceptionToParseError() {
        val e = mapToSourceException(Source.ARCHIVE, JsonConvertException("bad json"))
        assertEquals(SourceFailureReason.PARSE_ERROR, e.reason)
    }

    @Test
    fun mapsUnknownErrorToUnknown() {
        val e = mapToSourceException(Source.DEEZER, RuntimeException("boom"))
        assertEquals(SourceFailureReason.UNKNOWN, e.reason)
        assertEquals("boom", e.cause?.message)
    }

    @Test
    fun cancellationPassesThroughUnmapped() = runBlocking {
        try {
            sourceCall(Source.DEEZER) { throw CancellationException("cancelled") }
            fail("expected CancellationException to propagate")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    @Test
    fun successPassesThrough() = runBlocking {
        assertEquals(42, sourceCall(Source.DEEZER) { 42 })
    }
}
