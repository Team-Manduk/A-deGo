package com.teammanduk.adego

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdegoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("AdegoApp", "앱 초기화 완료")
    }
}