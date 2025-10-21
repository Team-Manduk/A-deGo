package com.teammanduk.adego.sync.location.manager

import android.content.Context
import android.content.Intent
import android.os.Build
import com.teammanduk.adego.sync.location.service.LocationTrackingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTrackingManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationTrackingManager {

    private val _isTracking = MutableStateFlow(false)

    override fun isTracking(): StateFlow<Boolean> = _isTracking

    override fun startTracking() {
        val intent = Intent(context, LocationTrackingService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        _isTracking.value = true
    }

    override fun stopTracking() {
        val intent = Intent(context, LocationTrackingService::class.java)
        context.stopService(intent)
        _isTracking.value = false
    }
}
