package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.model.GraphicCoordinate
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.SubPath
import javax.inject.Inject
import kotlin.math.*

/**
 * 경로 기반 ETA 계산 UseCase
 *
 * 구간별 진행률을 기반으로 남은 도착 시간을 계산합니다.
 * 1. 선택된 경로를 구간별로 분할
 * 2. 현재 위치가 속한 구간 찾기
 * 3. 구간 내 진행률 계산
 * 4. 남은 시간 = (현재 구간 남은 시간) + (이후 구간들의 총 시간)
 */
class CalculateEtaUseCase @Inject constructor() {

    /**
     * ETA 계산 결과
     */
    data class EtaResult(
        val remainingTimeInSeconds: Int, // 남은 시간 (초)
        val remainingDistanceInMeters: Double, // 남은 거리 (미터)
        val currentSubPathIndex: Int, // 현재 구간 인덱스
        val progressInCurrentSubPath: Double // 현재 구간 내 진행률 (0.0 ~ 1.0)
    )

    /**
     * ETA 계산 실행
     *
     * @param route 선택된 경로
     * @param currentLocation 현재 위치
     * @return EtaResult 계산 결과, 계산 불가능한 경우 null
     */
    operator fun invoke(
        route: Route,
        currentLocation: ParticipantLocation
    ): EtaResult? {
        // SubPath가 비어있는 경우 계산 불가
        if (route.subPaths.isEmpty()) return null

        // 현재 위치와 가장 가까운 SubPath 및 위치 찾기
        val (subPathIndex, closestPoint, progress) = findClosestSubPathAndProgress(
            route.subPaths,
            currentLocation
        ) ?: return null

        // 남은 시간 계산
        val remainingTime = calculateRemainingTime(
            route.subPaths,
            subPathIndex,
            progress
        )

        // 남은 거리 계산
        val remainingDistance = calculateRemainingDistance(
            route.subPaths,
            subPathIndex,
            closestPoint,
            currentLocation
        )

        return EtaResult(
            remainingTimeInSeconds = remainingTime,
            remainingDistanceInMeters = remainingDistance,
            currentSubPathIndex = subPathIndex,
            progressInCurrentSubPath = progress
        )
    }

    /**
     * 현재 위치와 가장 가까운 SubPath와 진행률 찾기
     */
    private fun findClosestSubPathAndProgress(
        subPaths: List<SubPath>,
        currentLocation: ParticipantLocation
    ): Triple<Int, GraphicCoordinate, Double>? {
        var minDistance = Double.MAX_VALUE
        var closestSubPathIndex = -1
        var closestPoint: GraphicCoordinate? = null
        var closestProgress = 0.0

        subPaths.forEachIndexed { index, subPath ->
            val graphicData = subPath.graphicData ?: return@forEachIndexed

            if (graphicData.isEmpty()) return@forEachIndexed

            // 각 구간의 선분에 대해 최단 거리 계산
            for (i in 0 until graphicData.size - 1) {
                val start = graphicData[i]
                val end = graphicData[i + 1]

                // 선분에서 가장 가까운 점과 거리 계산
                val (nearestPoint, distance) = findNearestPointOnSegment(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    start,
                    end
                )

                if (distance < minDistance) {
                    minDistance = distance
                    closestSubPathIndex = index
                    closestPoint = nearestPoint

                    // 해당 SubPath 내에서의 진행률 계산
                    closestProgress = calculateProgressInSubPath(
                        graphicData,
                        i,
                        nearestPoint
                    )
                }
            }
        }

        return if (closestSubPathIndex >= 0 && closestPoint != null) {
            Triple(closestSubPathIndex, closestPoint, closestProgress)
        } else {
            null
        }
    }

    /**
     * 선분 상에서 가장 가까운 점 찾기
     */
    private fun findNearestPointOnSegment(
        lat: Double,
        lng: Double,
        start: GraphicCoordinate,
        end: GraphicCoordinate
    ): Pair<GraphicCoordinate, Double> {
        // 선분의 길이
        val segmentLength = calculateDistance(
            start.latitude, start.longitude,
            end.latitude, end.longitude
        )

        if (segmentLength == 0.0) {
            return start to calculateDistance(lat, lng, start.latitude, start.longitude)
        }

        // 투영된 비율 계산 (0~1 범위로 클램핑)
        val t = ((lat - start.latitude) * (end.latitude - start.latitude) +
                (lng - start.longitude) * (end.longitude - start.longitude)) /
                (segmentLength * segmentLength)

        val clampedT = t.coerceIn(0.0, 1.0)

        // 가장 가까운 점
        val nearestPoint = GraphicCoordinate(
            latitude = start.latitude + clampedT * (end.latitude - start.latitude),
            longitude = start.longitude + clampedT * (end.longitude - start.longitude)
        )

        val distance = calculateDistance(lat, lng, nearestPoint.latitude, nearestPoint.longitude)

        return nearestPoint to distance
    }

    /**
     * SubPath 내에서의 진행률 계산
     */
    private fun calculateProgressInSubPath(
        graphicData: List<GraphicCoordinate>,
        segmentIndex: Int,
        currentPoint: GraphicCoordinate
    ): Double {
        // 현재 세그먼트까지의 총 거리
        var accumulatedDistance = 0.0
        for (i in 0 until segmentIndex) {
            accumulatedDistance += calculateDistance(
                graphicData[i].latitude, graphicData[i].longitude,
                graphicData[i + 1].latitude, graphicData[i + 1].longitude
            )
        }

        // 현재 세그먼트 내에서의 거리
        val currentSegmentDistance = calculateDistance(
            graphicData[segmentIndex].latitude, graphicData[segmentIndex].longitude,
            currentPoint.latitude, currentPoint.longitude
        )
        accumulatedDistance += currentSegmentDistance

        // SubPath 전체 거리
        var totalDistance = 0.0
        for (i in 0 until graphicData.size - 1) {
            totalDistance += calculateDistance(
                graphicData[i].latitude, graphicData[i].longitude,
                graphicData[i + 1].latitude, graphicData[i + 1].longitude
            )
        }

        return if (totalDistance > 0) {
            (accumulatedDistance / totalDistance).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
    }

    /**
     * 남은 시간 계산
     */
    private fun calculateRemainingTime(
        subPaths: List<SubPath>,
        currentSubPathIndex: Int,
        progressInCurrentSubPath: Double
    ): Int {
        var remainingSeconds = 0

        // 현재 SubPath의 남은 시간
        val currentSubPath = subPaths[currentSubPathIndex]
        val currentSubPathRemainingTime = currentSubPath.sectionTime * 60 * (1 - progressInCurrentSubPath)
        remainingSeconds += currentSubPathRemainingTime.toInt()

        // 이후 SubPath들의 총 시간
        for (i in currentSubPathIndex + 1 until subPaths.size) {
            remainingSeconds += subPaths[i].sectionTime * 60
        }

        return remainingSeconds
    }

    /**
     * 남은 거리 계산
     */
    private fun calculateRemainingDistance(
        subPaths: List<SubPath>,
        currentSubPathIndex: Int,
        closestPoint: GraphicCoordinate,
        currentLocation: ParticipantLocation
    ): Double {
        var remainingDistance = 0.0

        // 현재 SubPath의 남은 거리
        val currentSubPath = subPaths[currentSubPathIndex]
        val graphicData = currentSubPath.graphicData

        if (graphicData != null && graphicData.isNotEmpty()) {
            // 현재 위치부터 SubPath 끝까지의 거리
            val closestIndex = graphicData.indexOfFirst {
                it.latitude == closestPoint.latitude && it.longitude == closestPoint.longitude
            }.takeIf { it >= 0 } ?: 0

            for (i in closestIndex until graphicData.size - 1) {
                if (i == closestIndex) {
                    // 첫 세그먼트는 현재 위치부터 계산
                    remainingDistance += calculateDistance(
                        currentLocation.latitude, currentLocation.longitude,
                        graphicData[i + 1].latitude, graphicData[i + 1].longitude
                    )
                } else {
                    remainingDistance += calculateDistance(
                        graphicData[i].latitude, graphicData[i].longitude,
                        graphicData[i + 1].latitude, graphicData[i + 1].longitude
                    )
                }
            }
        } else {
            // graphicData가 없으면 distance 필드 사용
            remainingDistance += currentSubPath.distance
        }

        // 이후 SubPath들의 총 거리
        for (i in currentSubPathIndex + 1 until subPaths.size) {
            remainingDistance += subPaths[i].distance
        }

        return remainingDistance
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
}
