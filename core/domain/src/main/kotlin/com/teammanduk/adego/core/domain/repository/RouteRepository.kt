package com.teammanduk.adego.core.domain.repository

import com.teammanduk.adego.core.model.Route

/**
 * 대중교통 경로 검색을 위한 Repository 인터페이스
 */
interface RouteRepository {
    /**
     * 두 위치의 위경도로 대중교통 경로를 검색합니다.
     *
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @return 검색된 경로 목록 (추천 순)
     */
    suspend fun searchRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<Route>
}
