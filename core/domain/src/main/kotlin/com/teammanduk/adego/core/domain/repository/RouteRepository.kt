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

    /**
     * 선택된 경로의 세부 정보 (그래픽 데이터)를 가져옵니다.
     * TMAP Transit API는 기본적으로 그래픽 데이터를 포함하지만,
     * 일부 도보 구간의 경우 추가로 Pedestrian API를 호출하여 데이터를 가져올 수 있습니다.
     *
     * @param route 기본 정보가 있는 경로
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @return 완전한 그래픽 데이터가 포함된 경로
     */
    suspend fun getRouteDetails(
        route: Route,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Route
}
