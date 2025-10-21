package com.teammanduk.adego.core.notifications.di

import com.teammanduk.adego.core.notifications.LocationTrackingNotifier
import com.teammanduk.adego.core.notifications.Notifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {

    @Binds
    @Named("LocationTracking")
    abstract fun bindsLocationTrackingNotifier(
        notifier: LocationTrackingNotifier
    ): Notifier
}
