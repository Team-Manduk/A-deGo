package com.teammanduk.adego.core.remote.di

import com.teammanduk.adego.core.data_api.datasource.LocationDataSource
import com.teammanduk.adego.core.data_api.datasource.RoomDataSource
import com.teammanduk.adego.core.data_api.datasource.RouteDataSource
import com.teammanduk.adego.core.remote.datasource.FirebaseRoomDataSource
import com.teammanduk.adego.core.remote.datasource.FusedLocationDataSource
import com.teammanduk.adego.core.remote.datasource.TmapRouteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteModule {
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
}
