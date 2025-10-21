package com.teammanduk.adego.core.model

/**
 * 대중교통 경로 정보
 */
data class Route(
    val totalTime: Int, // 총 소요 시간 (초)
    val totalDistance: Int, // 총 거리 (미터)
    val totalFare: Int, // 총 요금 (원)
    val transferCount: Int, // 환승 횟수
    val pathType: Int, // 경로 타입 (1: 지하철, 2: 버스, 3: 지하철+버스)
    val subPaths: List<SubPath> = emptyList(), // 세부 경로 정보
    val startLatitude: Double? = null, // 출발지 위도
    val startLongitude: Double? = null, // 출발지 경도
    val endLatitude: Double? = null, // 도착지 위도
    val endLongitude: Double? = null // 도착지 경도
)

/**
 * 세부 경로 정보
 */
data class SubPath(
    val trafficType: TrafficType, // 이동 수단 유형
    val distance: Double, // 이동 거리 (미터)
    val sectionTime: Int, // 이동 시간 (초)

    // 대중교통 정보 (trafficType이 SUBWAY 또는 BUS일 때)
    val startName: String? = null, // 승차 정류장/역 이름
    val endName: String? = null, // 하차 정류장/역 이름
    val stationCount: Int? = null, // 정거장 개수
    val lane: Lane? = null, // 노선 정보

    // 도보 정보 (trafficType이 WALK일 때)
    val walkDistance: Double? = null, // 도보 거리 (미터)

    // 좌표 정보
    val startLatitude: Double? = null, // 출발지 위도
    val startLongitude: Double? = null, // 출발지 경도
    val endLatitude: Double? = null, // 도착지 위도
    val endLongitude: Double? = null, // 도착지 경도

    // 경유 정류장 목록 (좌표 포함)
    val passStations: List<Station>? = null,

    // 노선 그래픽 데이터 (실제 경로 좌표)
    val graphicData: List<GraphicCoordinate>? = null
)

/**
 * 그래픽 좌표 정보
 */
data class GraphicCoordinate(
    val latitude: Double, // 위도
    val longitude: Double // 경도
)

/**
 * 정류장/역 정보
 */
data class Station(
    val name: String, // 정류장/역 이름
    val latitude: Double, // 위도
    val longitude: Double // 경도
)

/**
 * 노선 정보
 */
data class Lane(
    val name: String, // 노선명 (예: "2호선", "360번")
    val busNo: String? = null, // 버스 번호
    val type: Int? = null, // 버스 타입 (1: 일반, 2: 좌석, 3: 마을, 4: 직행좌석, 5: 공항, 6: 간선급행, 10: 외곽, 11: 간선, 12: 지선, 13: 순환, 14: 광역, 15: 급행, 16: 관광, 20: 농어촌, 21: 리무진)
    val subwayCode: Int? = null // 지하철 노선 번호
)

/**
 * 이동 수단 유형
 */
enum class TrafficType {
    SUBWAY, // 지하철
    BUS, // 버스
    WALK // 도보
}
