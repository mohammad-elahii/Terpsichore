package com.Mohammad.Elahi.terpsichore.core.sources.network

import com.Mohammad.Elahi.terpsichore.core.sources.Source
import com.Mohammad.Elahi.terpsichore.core.sources.SourceException
import com.Mohammad.Elahi.terpsichore.core.sources.SourceFailureReason
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.UnknownHostException

suspend fun <T> sourceCall(source: Source, block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: SourceException) {
        throw e
    } catch (e: Throwable) {
        throw mapToSourceException(source, e)
    }

fun mapToSourceException(source: Source, cause: Throwable): SourceException {
    val reason = when (cause) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
            SourceFailureReason.TIMEOUT
        is UnknownHostException, is IOException -> SourceFailureReason.NO_NETWORK
        is ClientRequestException -> when (cause.response.status) {
            HttpStatusCode.NotFound -> SourceFailureReason.NOT_FOUND
            HttpStatusCode.TooManyRequests -> SourceFailureReason.RATE_LIMITED
            else -> SourceFailureReason.BAD_REQUEST
        }
        is ServerResponseException -> SourceFailureReason.SERVER_ERROR
        is JsonConvertException -> SourceFailureReason.PARSE_ERROR
        is SerializationException -> SourceFailureReason.PARSE_ERROR
        else -> SourceFailureReason.UNKNOWN
    }
    return SourceException(
        source = source,
        reason = reason,
        message = cause.message ?: "Source request failed",
        cause = cause,
    )
}
