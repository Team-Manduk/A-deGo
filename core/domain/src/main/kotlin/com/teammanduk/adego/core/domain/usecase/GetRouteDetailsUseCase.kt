package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RouteRepository
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject

/**
 * 경로 세부 정보 조회 UseCase
 *
 * 경로의 세부 정보(도보 구간의 graphicData 등)를 추가로 로드합니다.
 */
class GetRouteDetailsUseCase @Inject constructor(
    private val routeRepository: RouteRepository
) {
    /**
     * 경로 세부 정보 조회 실행
     *
     * @param route 상세 정보를 로드할 경로
     * @param startLat 출발지 위도
     * @param startLng 출발지 경도
     * @param endLat 도착지 위도
     * @param endLng 도착지 경도
     * @return 세부 정보가 추가된 경로
     */
    suspend operator fun invoke(
        route: Route,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Route {
        return routeRepository.getRouteDetails(
            route = route,
            startLat = startLat,
            startLng = startLng,
            endLat = endLat,
            endLng = endLng
        )
    }
}
