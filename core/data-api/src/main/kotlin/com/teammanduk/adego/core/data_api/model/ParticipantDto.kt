package com.teammanduk.adego.core.data_api.model

data class ParticipantDto(
    val userId: String = "",
    val name: String = "",
    val profileColor: String = "",
    val location: ParticipantLocationDto? = null,
    val route: ParticipantRouteDto? = null,
    val movementStatus: String = "NOT_STARTED"
)

data class ParticipantLocationDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val updatedAt: Long = 0L,
    val accuracy: Float = 0f
)

data class ParticipantRouteDto(
    val eta: String = "",
    val distance: String = "",
    val polyline: String = "",
    val durationInSeconds: Int = 0,
    val distanceInMeters: Int = 0,
    val updatedAt: Long = 0L,
    val selectedRoute: RouteDto? = null // 선택한 대중교통 경로
)

/**
 * 대중교통 경로 정보 (Firebase 저장용 DTO)
 */
data class RouteDto(
    val totalTime: Int = 0, // 총 소요 시간 (분)
    val totalDistance: Int = 0, // 총 거리 (미터)
    val totalFare: Int = 0, // 총 요금 (원)
    val transferCount: Int = 0, // 환승 횟수
    val pathType: Int = 0, // 경로 타입 (1: 지하철, 2: 버스, 3: 지하철+버스)
    val subPaths: List<SubPathDto> = emptyList(), // 세부 경로 정보
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0
)

/**
 * 세부 경로 정보 (Firebase 저장용 DTO)
 */
data class SubPathDto(
    val trafficType: String = "WALK", // SUBWAY, BUS, WALK
    val distance: Double = 0.0,
    val sectionTime: Int = 0,
    val startName: String? = null,
    val endName: String? = null,
    val stationCount: Int? = null,
    val lane: LaneDto? = null,
    val walkDistance: Double? = null,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val passStations: List<StationDto>? = null,
    val graphicData: List<GraphicCoordinateDto>? = null
)

/**
 * 노선 정보 (Firebase 저장용 DTO)
 */
data class LaneDto(
    val name: String = "",
    val busNo: String? = null,
    val type: Int? = null,
    val subwayCode: Int? = null
)

/**
 * 정류장/역 정보 (Firebase 저장용 DTO)
 */
data class StationDto(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

/**
 * 그래픽 좌표 정보 (Firebase 저장용 DTO)
 */
data class GraphicCoordinateDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
