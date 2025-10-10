package com.teammanduk.adego

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdegoApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // Google Places API 초기화
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
}