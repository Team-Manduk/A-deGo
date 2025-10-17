package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.model.ParticipantLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TrackAndUpdateLocationUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository,
    private val roomRepository: RoomRepository
) {
    private var lastUpdatedLocation: ParticipantLocation? = null

    operator fun invoke(): Flow<ParticipantLocation> {
        return locationRepository.observeLocationUpdates()
            .onEach { location ->
                val userId = userRepository.getCurrentUserId()

                // 의미 있는 변화만 Firebase에 업데이트
                if (shouldUpdateLocation(location)) {
                    roomRepository.updateMyLocation(userId, location)
                    lastUpdatedLocation = location
                }
            }
    }

    /**
     * Firebase 업데이트가 필요한지 판단
     * - 첫 업데이트: 무조건 업데이트
     * - 이후: 50미터 이상 이동했을 때만 업데이트
     */
    private fun shouldUpdateLocation(newLocation: ParticipantLocation): Boolean {
        val lastLocation = lastUpdatedLocation ?: return true // 첫 업데이트

        val distance = calculateDistance(
            lastLocation.latitude, lastLocation.longitude,
            newLocation.latitude, newLocation.longitude
        )

        // 50미터 이상 이동했으면 업데이트
        return distance >= MINIMUM_DISTANCE_FOR_UPDATE_METERS
    }

    /**
     * Haversine 공식을 사용한 두 좌표 간 거리 계산 (미터)
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    companion object {
        private const val MINIMUM_DISTANCE_FOR_UPDATE_METERS = 50.0 // 50미터
    }
}
