package com.teammanduk.adego.core.remote.di

import android.util.Log
import com.teammanduk.adego.core.data_api.datasource.LocationDataSource
import com.teammanduk.adego.core.data_api.datasource.PoiSearchDataSource
import com.teammanduk.adego.core.data_api.datasource.ReverseGeocodingDataSource
import com.teammanduk.adego.core.data_api.datasource.RoomDataSource
import com.teammanduk.adego.core.data_api.datasource.RouteDataSource
import com.teammanduk.adego.core.data_api.di.AndroidGeocoder
import com.teammanduk.adego.core.data_api.di.TmapReverseGeocoding
import com.teammanduk.adego.core.remote.datasource.AndroidGeocoderDataSource
import com.teammanduk.adego.core.remote.datasource.FirebaseRoomDataSource
import com.teammanduk.adego.core.remote.datasource.FusedLocationDataSource
import com.teammanduk.adego.core.remote.datasource.TmapPoiDataSource
import com.teammanduk.adego.core.remote.datasource.TmapReverseGeocodingDataSource
import com.teammanduk.adego.core.remote.datasource.TmapRouteDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {

    companion object {
        @Provides
        @Singleton
        fun provideHttpClient(): HttpClient {
            return HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            Log.d("KtorClient", message)
                        }
                    }
                    level = LogLevel.INFO
                }
            }
        }
    }
    @Binds
    @Singleton
    abstract fun bindRoomDataSource(
        firebaseRoomDataSource: FirebaseRoomDataSource
    ): RoomDataSource

    @Binds
    @Singleton
    abstract fun bindLocationDataSource(
        fusedLocationDataSource: FusedLocationDataSource
    ): LocationDataSource

    @Binds
    @Singleton
    abstract fun bindRouteDataSource(
        tmapRouteDataSource: TmapRouteDataSource
    ): RouteDataSource

    @Binds
    @Singleton
    @TmapReverseGeocoding
    abstract fun bindTmapReverseGeocodingDataSource(
        tmapReverseGeocodingDataSource: TmapReverseGeocodingDataSource
    ): ReverseGeocodingDataSource

    @Binds
    @Singleton
    @AndroidGeocoder
    abstract fun bindAndroidGeocoderDataSource(
        androidGeocoderDataSource: AndroidGeocoderDataSource
    ): ReverseGeocodingDataSource

    @Binds
    @Singleton
    abstract fun bindTmapPoiDataSource(
        tmapPoiDataSource: TmapPoiDataSource
    ): PoiSearchDataSource
}
