package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.domain.service.DistanceCalculator
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * 위치 추적 및 업데이트 UseCase
 *
 * 책임:
 * - 위치 변화 감지 (50m 이상 이동 시 Firebase 업데이트)
 * - 선택된 경로가 있으면 ETA 계산 및 업데이트
 *
 * 변경사항:
 * - UpdateLocationAndEtaUseCase와 통합하여 중복 제거
 * - ETA 계산 로직 통합
 */
class TrackAndUpdateLocationUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository,
    private val roomRepository: RoomRepository,
    private val distanceCalculator: DistanceCalculator,
    private val calculateEtaUseCase: CalculateEtaUseCase
) {
    private var lastUpdatedLocation: ParticipantLocation? = null

    /**
     * 위치 추적 시작
     *
     * @param selectedRoute 선택된 경로 (null이면 ETA 계산 안 함)
     */
    operator fun invoke(selectedRoute: Route? = null): Flow<ParticipantLocation> {
        return locationRepository.observeLocationUpdates()
            .onEach { location ->
                val userId = userRepository.getCurrentUserId()

                // 의미 있는 변화만 Firebase에 업데이트
                if (shouldUpdateLocation(location)) {
                    // 위치 업데이트
                    roomRepository.updateMyLocation(userId, location)
                    lastUpdatedLocation = location

                    // 선택된 경로가 있으면 ETA 계산 및 업데이트
                    selectedRoute?.let { route ->
                        updateEta(userId, location, route)
                    }
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

    /**
     * ETA 계산 및 업데이트
     */
    private suspend fun updateEta(
        userId: String,
        location: ParticipantLocation,
        route: Route
    ) {
        val etaResult = calculateEtaUseCase(route, location)

        etaResult?.let { result ->
            val participantRoute = ParticipantRoute(
                durationInSeconds = result.remainingTimeInSeconds,
                distanceInMeters = result.remainingDistanceInMeters.toInt(),
                polyline = "",
                updatedAt = System.currentTimeMillis(),
                currentSubPathIndex = result.currentSubPathIndex,
                progressInCurrentSubPath = result.progressInCurrentSubPath
            )

            roomRepository.updateMyRoute(userId, participantRoute)
        }
    }

    companion object {
        private const val MINIMUM_DISTANCE_FOR_UPDATE_METERS = 50.0 // 50미터
    }
}
