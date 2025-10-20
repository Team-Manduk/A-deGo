package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.model.MovementStatus
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 사용자의 현재 위치와 경로 정보를 기반으로 이동 상태를 계산합니다.
 */
class CalculateMovementStatusUseCase @Inject constructor() {

    /**
     * @param currentLocation 현재 위치
     * @param selectedRoute 선택한 경로 정보 (없으면 null)
     * @param destination 목적지
     * @return 계산된 이동 상태
     */
    operator fun invoke(
        currentLocation: ParticipantLocation?,
        selectedRoute: Route?,
        destination: Place
    ): MovementStatus {
        // 위치 정보가 없으면 NOT_STARTED
        if (currentLocation == null) {
            return MovementStatus.NOT_STARTED
        }

        // 경로를 선택하지 않았으면 NOT_STARTED
        if (selectedRoute == null) {
            return MovementStatus.NOT_STARTED
        }

        // 목적지까지의 거리 계산
        val distanceToDestination = calculateDistance(
            currentLocation.latitude,
            currentLocation.longitude,
            destination.latitude,
            destination.longitude
        )

        // 도착: 목적지로부터 100m 이내
        if (distanceToDestination <= 100) {
            return MovementStatus.ARRIVED
        }

        // 곧 도착: 목적지로부터 500m 이내
        if (distanceToDestination <= 500) {
            return MovementStatus.ARRIVING_SOON
        }

        // 경로의 출발지 좌표가 있는 경우, 출발지로부터의 거리 확인
        val startLat = selectedRoute.startLatitude
        val startLng = selectedRoute.startLongitude

        if (startLat != null && startLng != null) {
            val distanceFromStart = calculateDistance(
                currentLocation.latitude,
                currentLocation.longitude,
                startLat,
                startLng
            )

            // 출발 전: 출발지로부터 200m 이내
            if (distanceFromStart <= 200) {
                return MovementStatus.READY
            }
        }

        // 그 외: 이동 중
        return MovementStatus.IN_PROGRESS
    }

    /**
     * 두 지점 간의 거리를 미터 단위로 계산 (Haversine formula)
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2) * sin(deltaLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}
