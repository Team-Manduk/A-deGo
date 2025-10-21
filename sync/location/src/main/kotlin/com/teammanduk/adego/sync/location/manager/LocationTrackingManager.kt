package com.teammanduk.adego.sync.location.manager

import kotlinx.coroutines.flow.StateFlow

interface LocationTrackingManager {
    fun startTracking()
    fun stopTracking()
    fun isTracking(): StateFlow<Boolean>
}
