package com.Mohammad.Elahi.terpsichore.core.sources.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val TerpsichoreJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

val StatusValidator = createClientPlugin("TerpsichoreStatusValidator") {
    onResponse { response ->
        when (val status = response.status.value) {
            in 400..499 -> throw ClientRequestException(response, "Client error $status")
            in 500..599 -> throw ServerResponseException(response, "Server error $status")
        }
    }
}

fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = true
    install(StatusValidator)
    install(ContentNegotiation) {
        json(TerpsichoreJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 20_000
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 2)
        exponentialDelay()
    }
    install(DefaultRequest) {
        headers.append(HttpHeaders.UserAgent, "Terpsichore/1.0 (Android)")
    }
}
