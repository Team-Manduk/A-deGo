package com.teammanduk.adego.core.data_api.datasource

import com.teammanduk.adego.core.data_api.model.RouteDto

interface RouteDataSource {
    /**
     * 출발지와 목적지 좌표로 경로 검색 (TMAP Transit API)
     *
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @return 검색된 경로 목록
     */
    suspend fun searchRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<RouteDto>

    /**
     * 경로 세부 정보 로드 (graphicData 등)
     *
     * @param route 기본 경로 정보
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @return 세부 정보가 추가된 경로
     */
    suspend fun getRouteDetails(
        route: RouteDto,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): RouteDto
}
