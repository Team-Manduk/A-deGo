package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.model.GraphicCoordinate
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.SubPath
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 경로 진행률 계산 결과
 */
data class RouteProgress(
    val currentSubPathIndex: Int, // 현재 SubPath 인덱스
    val progressInCurrentSubPath: Double, // 현재 SubPath 내 진행률 (0.0 ~ 1.0)
    val traveledDistance: Double, // 지금까지 이동한 거리 (미터)
    val remainingDistance: Double, // 남은 거리 (미터)
    val remainingTimeInSeconds: Int, // 남은 시간 (초)
    val etaTimestamp: Long // 도착 예정 시간 (timestamp)
)

/**
 * Polyline 기반으로 정확한 경로 진행률을 계산하는 UseCase
 *
 * 동작:
 * 1. 사용자 위치에서 가장 가까운 SubPath 찾기
 * 2. 해당 SubPath 내에서 가장 가까운 polyline 포인트 찾기
 * 3. 해당 포인트까지의 진행률 계산
 * 4. 남은 거리와 시간 계산
 * 5. ETA 계산
 */
class CalculateRouteProgressUseCase @Inject constructor() {

    operator fun invoke(
        userLocation: ParticipantLocation,
        route: Route
    ): RouteProgress? {
        // 1. 사용자 위치에서 가장 가까운 SubPath 찾기
        val (subPathIndex, subPath, minDistance) = findNearestSubPath(userLocation, route.subPaths)
            ?: return null

        // 경로에서 너무 멀리 떨어진 경우 (100m 이상) null 반환
        if (minDistance > 100.0) {
            return null
        }

        // 2. 현재 SubPath 내에서 진행률 계산
        val progressData = calculateProgressInSubPath(userLocation, subPath)

        // 3. 이전 SubPath들의 거리 합계
        val previousSubPathsDistance = route.subPaths
            .take(subPathIndex)
            .sumOf { it.distance }

        // 4. 현재까지 이동한 총 거리
        val traveledDistance = previousSubPathsDistance + progressData.traveledDistance

        // 5. 남은 거리 계산
        val remainingDistanceInCurrentSubPath = subPath.distance - progressData.traveledDistance
        val remainingSubPathsDistance = route.subPaths
            .drop(subPathIndex + 1)
            .sumOf { it.distance }
        val totalRemainingDistance = remainingDistanceInCurrentSubPath + remainingSubPathsDistance

        // 6. 남은 시간 계산 (초 단위)
        val remainingTimeInCurrentSubPath = subPath.sectionTime * (1 - progressData.progress)
        val remainingSubPathsTime = route.subPaths
            .drop(subPathIndex + 1)
            .sumOf { it.sectionTime }
        val totalRemainingTimeInSeconds = (remainingTimeInCurrentSubPath + remainingSubPathsTime).toInt()

        // 7. ETA 계산
        val etaTimestamp = System.currentTimeMillis() + (totalRemainingTimeInSeconds * 1000L)

        return RouteProgress(
            currentSubPathIndex = subPathIndex,
            progressInCurrentSubPath = progressData.progress,
            traveledDistance = traveledDistance,
            remainingDistance = totalRemainingDistance,
            remainingTimeInSeconds = totalRemainingTimeInSeconds,
            etaTimestamp = etaTimestamp
        )
    }

    /**
     * 사용자 위치에서 가장 가까운 SubPath 찾기
     */
    private fun findNearestSubPath(
        userLocation: ParticipantLocation,
        subPaths: List<SubPath>
    ): Triple<Int, SubPath, Double>? {
        return subPaths
            .mapIndexed { index, subPath ->
                val minDistance = calculateMinDistanceToSubPath(userLocation, subPath)
                Triple(index, subPath, minDistance)
            }
            .minByOrNull { it.third }
    }

    /**
     * SubPath까지의 최단 거리 계산
     */
    private fun calculateMinDistanceToSubPath(
        userLocation: ParticipantLocation,
        subPath: SubPath
    ): Double {
        val graphicData = subPath.graphicData

        // graphicData가 있으면 polyline 기반으로 계산
        if (!graphicData.isNullOrEmpty()) {
            return graphicData.minOf { point ->
                calculateHaversineDistance(
                    userLocation.latitude,
                    userLocation.longitude,
                    point.latitude,
                    point.longitude
                )
            }
        }

        // graphicData가 없으면 시작점-끝점 직선 기반으로 계산
        val startLat = subPath.startLatitude
        val startLng = subPath.startLongitude
        val endLat = subPath.endLatitude
        val endLng = subPath.endLongitude

        if (startLat != null && startLng != null && endLat != null && endLng != null) {
            val distanceToStart = calculateHaversineDistance(
                userLocation.latitude, userLocation.longitude, startLat, startLng
            )
            val distanceToEnd = calculateHaversineDistance(
                userLocation.latitude, userLocation.longitude, endLat, endLng
            )
            return minOf(distanceToStart, distanceToEnd)
        }

        return Double.MAX_VALUE
    }

    /**
     * SubPath 내에서 진행률 계산
     */
    private fun calculateProgressInSubPath(
        userLocation: ParticipantLocation,
        subPath: SubPath
    ): ProgressData {
        val graphicData = subPath.graphicData

        // graphicData가 있으면 polyline 기반으로 정확하게 계산
        if (!graphicData.isNullOrEmpty()) {
            return calculateProgressFromPolyline(userLocation, graphicData, subPath.distance)
        }

        // graphicData가 없으면 시작점-끝점 직선 거리로 계산
        val startLat = subPath.startLatitude
        val startLng = subPath.startLongitude
        val endLat = subPath.endLatitude
        val endLng = subPath.endLongitude

        if (startLat != null && startLng != null && endLat != null && endLng != null) {
            val totalDistance = calculateHaversineDistance(startLat, startLng, endLat, endLng)
            val traveledDistance = calculateHaversineDistance(startLat, startLng, userLocation.latitude, userLocation.longitude)
            val progress = (traveledDistance / totalDistance).coerceIn(0.0, 1.0)

            return ProgressData(
                progress = progress,
                traveledDistance = traveledDistance
            )
        }

        return ProgressData(0.0, 0.0)
    }

    /**
     * Polyline 좌표 배열을 기반으로 진행률 계산
     */
    private fun calculateProgressFromPolyline(
        userLocation: ParticipantLocation,
        polyline: List<GraphicCoordinate>,
        totalSubPathDistance: Double
    ): ProgressData {
        // 1. 사용자 위치에서 가장 가까운 polyline 포인트 찾기
        val nearestPointIndex = polyline.indices.minByOrNull { index ->
            calculateHaversineDistance(
                userLocation.latitude,
                userLocation.longitude,
                polyline[index].latitude,
                polyline[index].longitude
            )
        } ?: 0

        // 2. 해당 포인트까지의 누적 거리 계산
        var traveledDistance = 0.0
        for (i in 0 until nearestPointIndex) {
            traveledDistance += calculateHaversineDistance(
                polyline[i].latitude,
                polyline[i].longitude,
                polyline[i + 1].latitude,
                polyline[i + 1].longitude
            )
        }

        // 3. 진행률 계산 (0.0 ~ 1.0)
        val progress = if (totalSubPathDistance > 0) {
            (traveledDistance / totalSubPathDistance).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        return ProgressData(
            progress = progress,
            traveledDistance = traveledDistance
        )
    }

    /**
     * Haversine 공식을 사용한 두 지점 간 거리 계산 (미터)
     */
    private fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusMeters = 6371000.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMeters * c
    }

    /**
     * 진행률 데이터
     */
    private data class ProgressData(
        val progress: Double, // 진행률 (0.0 ~ 1.0)
        val traveledDistance: Double // 이동한 거리 (미터)
    )
}
