package com.teammanduk.adego.core.data.di

import com.teammanduk.adego.core.data.repository.LocationRepositoryImpl
import com.teammanduk.adego.core.data.repository.PlaceRepositoryImpl
import com.teammanduk.adego.core.data.repository.RoomRepositoryImpl
import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindPlaceRepository(
        placeRepositoryImpl: PlaceRepositoryImpl
    ): PlaceRepository

    @Binds
    @Singleton
    abstract fun bindRoomRepository(
        roomRepositoryImpl: RoomRepositoryImpl
    ): RoomRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository
}
