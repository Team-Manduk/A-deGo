package com.teammanduk.adego.core.data_api.model

import kotlinx.serialization.Serializable

/**
 * Data Layer의 경로 정보 DTO
 * Domain Model과 Data Source 사이의 중간 계층
 */
@Serializable
data class RouteDto(
    val totalTime: Int, // 총 소요 시간 (초)
    val totalDistance: Int, // 총 거리 (미터)
    val totalFare: Int, // 총 요금 (원)
    val transferCount: Int, // 환승 횟수
    val pathType: Int, // 경로 타입 (1: 지하철, 2: 버스, 3: 지하철+버스)
    val subPaths: List<SubPathDto> = emptyList(), // 세부 경로 정보
    val startLatitude: Double? = null, // 출발지 위도
    val startLongitude: Double? = null, // 출발지 경도
    val endLatitude: Double? = null, // 도착지 위도
    val endLongitude: Double? = null // 도착지 경도
)

/**
 * 세부 경로 정보 DTO
 */
@Serializable
data class SubPathDto(
    val trafficType: TrafficTypeDto, // 이동 수단 유형
    val distance: Double, // 이동 거리 (미터)
    val sectionTime: Int, // 이동 시간 (초)

    // 대중교통 정보
    val startName: String? = null,
    val endName: String? = null,
    val stationCount: Int? = null,
    val lane: LaneDto? = null,

    // 도보 정보
    val walkDistance: Double? = null,

    // 좌표 정보
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,

    // 경유 정류장
    val passStations: List<StationDto>? = null,

    // 노선 그래픽 데이터
    val graphicData: List<GraphicCoordinateDto>? = null
)

/**
 * 그래픽 좌표 정보 DTO
 */
@Serializable
data class GraphicCoordinateDto(
    val latitude: Double,
    val longitude: Double
)

/**
 * 정류장/역 정보 DTO
 */
@Serializable
data class StationDto(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * 노선 정보 DTO
 */
@Serializable
data class LaneDto(
    val name: String,
    val busNo: String? = null,
    val type: Int? = null,
    val subwayCode: Int? = null
)

/**
 * 이동 수단 유형 DTO
 */
@Serializable
enum class TrafficTypeDto {
    SUBWAY, // 지하철
    BUS, // 버스
    WALK // 도보
}
