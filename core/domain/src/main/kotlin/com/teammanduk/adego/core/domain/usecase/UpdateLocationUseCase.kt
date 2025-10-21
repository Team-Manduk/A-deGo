package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.SelectedRouteRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.domain.util.PolylineEncoder
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치와 ETA를 함께 업데이트하는 UseCase
 *
 * 기존 UpdateLocationUseCase를 확장하여:
 * 1. 위치 업데이트 정책 적용 (거리/시간 기반)
 * 2. 선택된 경로가 있으면 ETA 계산
 * 3. 위치와 경로 정보를 Firebase에 업데이트
 */
@Singleton
class UpdateLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val selectedRouteRepository: SelectedRouteRepository,
    private val calculateRouteProgress: CalculateRouteProgressUseCase,
    private val calculateMovementStatus: CalculateMovementStatusUseCase
) {

    /**
     * 위치 업데이트 (Room DB에서 선택된 경로를 자동으로 조회)
     */
    suspend operator fun invoke(location: ParticipantLocation): Boolean {
        // Room DB에서 선택된 경로 조회
        val selectedRoute = selectedRouteRepository.getSelectedRoute().getOrNull()
        return invoke(location, selectedRoute)
    }

    /**
     * 위치와 ETA를 함께 업데이트
     *
     * @param location 현재 위치
     * @param selectedRoute 사용자가 선택한 경로 (null이면 위치만 업데이트)
     * @return 업데이트 성공 여부
     */
    suspend operator fun invoke(
        location: ParticipantLocation,
        selectedRoute: Route?
    ): Boolean {
        if (!shouldUpdate(location)) {
            return false
        }

        val userId = userRepository.getCurrentUserId()

        // 위치 업데이트
        roomRepository.updateMyLocation(userId, location)

        // 경로가 있으면 ETA 계산 및 업데이트
        if (selectedRoute != null) {
            val routeProgress = calculateRouteProgress(location, selectedRoute)

            if (routeProgress != null) {
                // polyline은 SaveSelectedRouteUseCase에서 이미 저장했으므로 재사용
                val polyline = PolylineEncoder.encode(selectedRoute)

                val participantRoute = ParticipantRoute(
                    eta = formatEta(routeProgress.remainingTimeInSeconds),
                    distance = formatDistance(routeProgress.remainingDistance),
                    polyline = polyline,
                    durationInSeconds = routeProgress.remainingTimeInSeconds,
                    distanceInMeters = routeProgress.remainingDistance.toInt(),
                    updatedAt = System.currentTimeMillis(),
                    currentSubPathIndex = routeProgress.currentSubPathIndex,
                    progressInCurrentSubPath = routeProgress.progressInCurrentSubPath,
                    traveledDistance = routeProgress.traveledDistance,
                    remainingDistance = routeProgress.remainingDistance
                )

                roomRepository.updateMyRoute(userId, participantRoute)
            }
        }

        // 이동 상태 계산 및 업데이트
        val roomId = roomRepository.getCurrentRoomId()
        if (roomId != null) {
            val roomInfo = roomRepository.getRoomInfo(roomId).getOrNull()
            if (roomInfo != null) {
                val movementStatus = calculateMovementStatus(
                    currentLocation = location,
                    selectedRoute = selectedRoute,
                    destination = roomInfo.destination
                )
                roomRepository.updateMyMovementStatus(userId, movementStatus)
            }
        }

        // 업데이트 후 Repository에 상태 저장
        locationRepository.setLastUpdatedLocation(location)

        return true
    }

    /**
     * 업데이트가 필요한지 판단
     */
    private fun shouldUpdate(location: ParticipantLocation): Boolean {
        val lastLoc = locationRepository.getLastUpdatedLocation()
        val lastTime = locationRepository.getLastUploadTimestamp()

        // 첫 업데이트는 무조건 진행
        if (lastLoc == null || lastTime == null) {
            return true
        }

        val now = System.currentTimeMillis()
        val timeSinceLastUpdate = now - lastTime

        // 30초 이상 지났으면 무조건 업데이트 (살아있다는 신호)
        if (timeSinceLastUpdate >= 30_000L) {
            return true
        }

        // 거리 계산 (미터 단위)
        val distance = calculateDistance(
            lastLoc.latitude,
            lastLoc.longitude,
            location.latitude,
            location.longitude
        )

        // 25미터 이상 이동했을 때 업데이트
        return distance >= 25f
    }

    /**
     * 두 좌표 간 거리 계산
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val latDiff = (lat2 - lat1) * 111_320  // 위도 차이 (미터)
        val lonDiff = (lon2 - lon1) * 111_320 * kotlin.math.cos(Math.toRadians(lat1))  // 경도 차이 (미터)

        return kotlin.math.sqrt(latDiff * latDiff + lonDiff * lonDiff).toFloat()
    }

    /**
     * 남은 시간을 "15분" 형식으로 포맷
     */
    private fun formatEta(remainingTimeInSeconds: Int): String {
        val minutes = remainingTimeInSeconds / 60
        return if (minutes < 60) {
            "${minutes}분"
        } else {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) {
                "${hours}시간 ${mins}분"
            } else {
                "${hours}시간"
            }
        }
    }

    /**
     * 거리를 "3.2km" 또는 "850m" 형식으로 포맷
     */
    private fun formatDistance(distanceInMeters: Double): String {
        return if (distanceInMeters >= 1000) {
            String.format("%.1fkm", distanceInMeters / 1000)
        } else {
            String.format("%.0fm", distanceInMeters)
        }
    }
}
