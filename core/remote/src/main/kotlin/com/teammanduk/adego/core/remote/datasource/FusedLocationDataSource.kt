package com.teammanduk.adego.core.remote.datasource

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.teammanduk.adego.core.data_api.datasource.LocationDataSource
import com.teammanduk.adego.core.data_api.model.ParticipantLocationDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FusedLocationDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationDataSource {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    // Flow를 공유하기 위한 CoroutineScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 공유된 위치 업데이트 Flow (단일 GPS 리스너)
    private val sharedLocationFlow: Flow<ParticipantLocationDto> by lazy {
        createLocationFlow()
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 1 // 마지막 위치를 캐시하여 새 구독자에게 즉시 제공
            )
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<ParticipantLocationDto> {
        return try {
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val locationDto = location.toDto()
                            Log.d(TAG, "Current location: $locationDto")
                            continuation.resume(Result.success(locationDto))
                        } else {
                            Log.w(TAG, "Location is null")
                            continuation.resume(
                                Result.failure(Exception("Location is null"))
                            )
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to get current location", exception)
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            Result.failure(e)
        }
    }

    /**
     * 공유된 위치 업데이트 Flow 반환
     * 여러 곳에서 구독해도 단일 GPS 리스너만 생성됨
     */
    override fun getLocationUpdates(): Flow<ParticipantLocationDto> {
        Log.d(TAG, "getLocationUpdates() called - returning shared flow")
        return sharedLocationFlow
    }

    /**
     * 실제 위치 업데이트를 생성하는 내부 메서드
     * shareIn으로 공유되어 단일 GPS 리스너만 생성됨
     */
    @SuppressLint("MissingPermission")
    private fun createLocationFlow(): Flow<ParticipantLocationDto> = callbackFlow {
        Log.d(TAG, "createLocationFlow() - creating new GPS listener")

        // 1. 먼저 lastLocation을 즉시 emit (있다면)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val locationDto = it.toDto()
                    Log.d(TAG, "Initial last known location: $locationDto")
                    trySend(locationDto)
                }
            }

        // 2. 그 다음 실시간 업데이트 시작
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
            setWaitForAccurateLocation(false)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    val locationDto = location.toDto()
                    Log.d(TAG, "Location update: $locationDto")
                    trySend(locationDto)
                }
            }
        }

        locationCallback = callback

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { exception ->
            Log.e(TAG, "Failed to request location updates", exception)
            close(exception)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
            Log.d(TAG, "Location updates stopped - GPS listener removed")
        }
    }

    override suspend fun startTracking(): Result<Unit> {
        return try {
            Log.d(TAG, "Location tracking started")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tracking", e)
            Result.failure(e)
        }
    }

    override suspend fun stopTracking(): Result<Unit> {
        return try {
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
                locationCallback = null
            }
            Log.d(TAG, "Location tracking stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop tracking", e)
            Result.failure(e)
        }
    }

    private fun Location.toDto(): ParticipantLocationDto {
        return ParticipantLocationDto(
            latitude = latitude,
            longitude = longitude,
            updatedAt = System.currentTimeMillis(),
            accuracy = accuracy
        )
    }

    companion object {
        private const val TAG = "FusedLocationDataSource"
        private const val LOCATION_UPDATE_INTERVAL = 5000L // 5초
        private const val FASTEST_LOCATION_INTERVAL = 2000L // 2초
    }
}
