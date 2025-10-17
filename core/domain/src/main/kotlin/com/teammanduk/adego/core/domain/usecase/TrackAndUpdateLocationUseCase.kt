package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.domain.service.DistanceCalculator
import com.teammanduk.adego.core.model.ParticipantLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class TrackAndUpdateLocationUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository,
    private val roomRepository: RoomRepository,
    private val distanceCalculator: DistanceCalculator
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

        val distance = distanceCalculator.calculateDistanceInMeters(
            lat1 = lastLocation.latitude,
            lon1 = lastLocation.longitude,
            lat2 = newLocation.latitude,
            lon2 = newLocation.longitude
        )

        // 50미터 이상 이동했으면 업데이트
        return distance >= MINIMUM_DISTANCE_FOR_UPDATE_METERS
    }

    companion object {
        private const val MINIMUM_DISTANCE_FOR_UPDATE_METERS = 50.0 // 50미터
    }
}
