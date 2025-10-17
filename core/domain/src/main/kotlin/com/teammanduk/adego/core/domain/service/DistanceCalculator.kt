package com.teammanduk.adego.core.domain.service

import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 지리적 좌표 간 거리를 계산하는 도메인 서비스
 *
 * Haversine 공식을 사용하여 두 지점 간 직선 거리를 계산합니다.
 *
 * 책임:
 * - 지구 곡률을 고려한 정확한 거리 계산
 * - 단위 변환 (미터, 킬로미터)
 *
 * 특징:
 * - Android Framework 의존성 없음 (순수 Kotlin)
 * - 단위 테스트 가능
 * - 모든 Layer에서 재사용 가능
 */
class DistanceCalculator @Inject constructor() {

    /**
     * 두 좌표 간 거리를 미터 단위로 계산
     *
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 두 지점 간 거리 (미터)
     */
    fun calculateDistanceInMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusMeters = 6371000.0 // 지구 반지름 (미터)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusMeters * c
    }

    /**
     * 두 좌표 간 거리를 미터 단위로 계산 (정수 반환)
     *
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 두 지점 간 거리 (미터, 반올림)
     */
    fun calculateDistanceInMetersInt(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        return calculateDistanceInMeters(lat1, lon1, lat2, lon2).toInt()
    }

    /**
     * 두 좌표 간 거리를 킬로미터 단위로 계산
     *
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 두 지점 간 거리 (킬로미터)
     */
    fun calculateDistanceInKilometers(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        return calculateDistanceInMeters(lat1, lon1, lat2, lon2) / 1000.0
    }
}
