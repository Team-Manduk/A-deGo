package com.teammanduk.adego.core.data_api.model

/**
 * 대중교통 경로 DTO
 */
data class TransitRouteDto(
    val totalTime: Int, // 총 소요 시간 (분)
    val totalDistance: Int, // 총 거리 (미터)
    val totalFare: Int, // 총 요금
    val transferCount: Int, // 환승 횟수
    val pathType: Int, // 경로 타입 (1: 지하철만, 2: 버스만, 3: 복합)
    val legs: List<RouteLegDto> // 구간 목록
)

/**
 * 경로 구간 DTO
 */
data class RouteLegDto(
    val mode: String, // "WALK", "BUS", "SUBWAY"
    val sectionTime: Int, // 구간 시간 (분)
    val distance: Double, // 거리 (미터)
    val startName: String?, // 출발 정류장/역명
    val endName: String?, // 도착 정류장/역명
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?,
    val stationCount: Int?, // 정류장 개수
    val route: String?, // 노선명
    val routeId: String?, // 노선 ID
    val routeType: Int?, // 노선 타입
    val passStations: List<StationDto>?, // 경유 정류장 목록
    val graphicCoordinates: List<GraphicCoordinateDto>? // 경로 좌표
)

/**
 * 정류장/역 DTO
 */
data class StationDto(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * 경로 좌표 DTO
 */
data class GraphicCoordinateDto(
    val latitude: Double,
    val longitude: Double
)
