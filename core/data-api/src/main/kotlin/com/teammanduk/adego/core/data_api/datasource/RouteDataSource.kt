package com.teammanduk.adego.core.data_api.datasource

import com.teammanduk.adego.core.data_api.model.PedestrianRouteDto
import com.teammanduk.adego.core.data_api.model.TransitRouteDto

/**
 * 경로 검색 DataSource
 *
 * TMAP API와 같은 외부 경로 검색 서비스와의 통신을 담당합니다.
 */
interface RouteDataSource {
    /**
     * 대중교통 경로 검색
     *
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @return 경로 목록 (최대 3개)
     */
    suspend fun searchTransitRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Result<List<TransitRouteDto>>

    /**
     * 보행자 경로 검색
     *
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @param startName 출발지 이름 (선택)
     * @param endName 도착지 이름 (선택)
     * @return 보행자 경로 좌표 목록
     */
    suspend fun searchPedestrianRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        startName: String = "출발지",
        endName: String = "도착지"
    ): Result<PedestrianRouteDto>
}
