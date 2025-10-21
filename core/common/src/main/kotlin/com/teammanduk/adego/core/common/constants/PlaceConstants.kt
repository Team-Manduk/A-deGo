package com.teammanduk.adego.core.common.constants

/**
 * 장소 관련 상수 정의
 */
object PlaceConstants {
    /**
     * 기본 위치 - 위도 (현재 위치를 가져올 수 없을 때 사용)
     * 부산 만덕동 좌표
     */
    const val DEFAULT_LATITUDE = 35.1979

    /**
     * 기본 위치 - 경도 (현재 위치를 가져올 수 없을 때 사용)
     * 부산 만덕동 좌표
     */
    const val DEFAULT_LONGITUDE = 129.0758

    /**
     * 역지오코딩 디바운스 시간 (밀리초)
     * 사용자가 지도 드래그를 멈춘 후 대기 시간
     */
    const val REVERSE_GEOCODING_DEBOUNCE_MS = 500L

    /**
     * 위치 비교 정밀도 (위도/경도 차이)
     * 약 0.1m 오차 허용 (소수점 6자리)
     */
    const val LOCATION_COMPARISON_PRECISION = 0.000001
}
