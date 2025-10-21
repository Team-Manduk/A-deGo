package com.teammanduk.adego.sync.location.di

import com.teammanduk.adego.sync.location.manager.LocationTrackingManager
import com.teammanduk.adego.sync.location.manager.LocationTrackingManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocationSyncModule {

    @Binds
    abstract fun bindsLocationTrackingManager(
        manager: LocationTrackingManagerImpl
    ): LocationTrackingManager
}
