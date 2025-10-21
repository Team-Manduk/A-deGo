package com.teammanduk.adego.core.domain.util

import com.teammanduk.adego.core.model.Place
import kotlin.math.abs

/**
 * 위치 관련 유틸리티
 *
 * Domain Layer에서 위치 비교 및 계산 로직 제공
 */
object LocationUtils {
    /**
     * 위치 비교 정밀도 (위도/경도 차이)
     * 약 0.1m 오차 허용 (소수점 6자리)
     */
    private const val LOCATION_COMPARISON_PRECISION = 0.000001

    /**
     * 두 위치가 동일한지 비교 (오차 범위 내)
     *
     * @param lat1 첫 번째 위치의 위도
     * @param lng1 첫 번째 위치의 경도
     * @param lat2 두 번째 위치의 위도
     * @param lng2 두 번째 위치의 경도
     * @param precision 비교 정밀도 (기본값: 0.000001, 약 0.1m)
     * @return 두 위치가 오차 범위 내에서 같으면 true
     */
    fun isSameLocation(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
        precision: Double = LOCATION_COMPARISON_PRECISION
    ): Boolean {
        val latDiff = abs(lat1 - lat2)
        val lngDiff = abs(lng1 - lng2)
        return latDiff < precision && lngDiff < precision
    }

    /**
     * Place 객체 기반 위치 비교
     */
    fun Place.isSameLocationAs(
        other: Place,
        precision: Double = LOCATION_COMPARISON_PRECISION
    ): Boolean {
        return isSameLocation(
            this.latitude,
            this.longitude,
            other.latitude,
            other.longitude,
            precision
        )
    }

    /**
     * 좌표 기반 위치 비교 (lat, lng 개별 파라미터)
     */
    fun isSameLocation(
        pos1Lat: Double,
        pos1Lng: Double,
        pos2Lat: Double,
        pos2Lng: Double
    ): Boolean {
        return isSameLocation(pos1Lat, pos1Lng, pos2Lat, pos2Lng, LOCATION_COMPARISON_PRECISION)
    }
}
