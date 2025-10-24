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

    // 이미 위치 추적이 시작되었는지 확인하는 플래그
    private var isTrackingStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notifier.createNotification()
        startForeground(NOTIFICATION_ID, notification)
        // 이미 추적 중이면 중복 구독 방지
        if (isTrackingStarted) {
            Log.d(TAG, "Location tracking already started - skipping duplicate subscription")
            return START_STICKY
        }

        Log.d(TAG, "Starting location tracking - creating new subscription")
        isTrackingStarted = true

        serviceScope.launch {
            var firstLocation = true
            broadcastLocation()
                .catch { e ->
                    Log.e(TAG, "Location tracking error", e)
                    isTrackingStarted = false // 에러 발생 시 플래그 리셋
                }
                .collect { location ->
                    if (firstLocation) {
                        Log.d("MapPerformance", "[3] First location received at ${System.currentTimeMillis()}")
                        firstLocation = false
                    }
                    Log.d(TAG, "Location updated: $location")
                }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isTrackingStarted = false // 서비스 종료 시 플래그 리셋
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
