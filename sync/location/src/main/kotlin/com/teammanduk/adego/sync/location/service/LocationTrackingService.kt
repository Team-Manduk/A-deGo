package com.teammanduk.adego.sync.location.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.domain.usecase.BroadcastLocationUseCase
import com.teammanduk.adego.core.notifications.Notifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    @Named("LocationTracking")
    lateinit var notifier: Notifier

    @Inject
    lateinit var broadcastLocation: BroadcastLocationUseCase

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var roomRepository: RoomRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notifier.createNotification()
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            broadcastLocation()
                .catch { e ->
                    Log.e(TAG, "Location tracking error", e)
                }
                .collect { location ->
                    Log.d(TAG, "Location updated: $location")
                }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "LocationTrackingService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_ID = 1001
    }
}
