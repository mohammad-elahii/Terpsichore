package com.Mohammad.Elahi.terpsichore.di

import android.content.Context
import com.Mohammad.Elahi.terpsichore.core.sources.MusicSource
import com.Mohammad.Elahi.terpsichore.core.sources.SourceId
import com.Mohammad.Elahi.terpsichore.core.sources.archive.ArchiveSource
import com.Mohammad.Elahi.terpsichore.core.sources.deezer.DeezerSource
import com.Mohammad.Elahi.terpsichore.core.sources.jamendo.JamendoSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AppContainer(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val httpClient: HttpClient = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
    }

    val sources: List<MusicSource> = listOf(
        DeezerSource(httpClient),
        JamendoSource(httpClient),
        ArchiveSource(httpClient),
    )

    fun source(id: SourceId): MusicSource? =
        sources.firstOrNull { it.id == id }
}
