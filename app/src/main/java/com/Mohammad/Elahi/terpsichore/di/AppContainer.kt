package com.Mohammad.Elahi.terpsichore.di

import android.content.Context
import com.Mohammad.Elahi.terpsichore.BuildConfig
import com.Mohammad.Elahi.terpsichore.core.sources.MusicSource
import com.Mohammad.Elahi.terpsichore.core.sources.SearchAggregator
import com.Mohammad.Elahi.terpsichore.core.sources.Source
import com.Mohammad.Elahi.terpsichore.core.sources.archive.ArchiveSource
import com.Mohammad.Elahi.terpsichore.core.sources.deezer.DeezerSource
import com.Mohammad.Elahi.terpsichore.core.sources.jamendo.JamendoSource
import com.Mohammad.Elahi.terpsichore.core.sources.network.createHttpClient

class AppContainer(context: Context) {

    val httpClient = createHttpClient()

    val sources: List<MusicSource> = listOf(
        DeezerSource(httpClient),
        JamendoSource(httpClient, BuildConfig.JAMENDO_CLIENT_ID),
        ArchiveSource(httpClient),
    )

    fun source(id: Source): MusicSource? =
        sources.firstOrNull { it.id == id }

    val searchAggregator = SearchAggregator(sources)
}
