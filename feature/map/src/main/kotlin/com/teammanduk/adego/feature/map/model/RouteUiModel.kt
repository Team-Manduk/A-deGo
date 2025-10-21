package com.teammanduk.adego.feature.map.model

import com.teammanduk.adego.core.model.GraphicCoordinate
import com.teammanduk.adego.core.model.Lane
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.Station
import com.teammanduk.adego.core.model.SubPath
import com.teammanduk.adego.core.model.TrafficType

/**
 * UI Layer에서 사용하는 경로 정보 모델
 */
data class RouteUiModel(
    val totalTime: Int, // 총 소요 시간 (초)
    val totalDistance: Int, // 총 거리 (미터)
    val totalFare: Int, // 총 요금 (원)
    val transferCount: Int, // 환승 횟수
    val pathType: Int, // 경로 타입 (1: 지하철, 2: 버스, 3: 지하철+버스)
    val subPaths: List<SubPathUiModel> = emptyList(), // 세부 경로 정보
    val startLatitude: Double? = null, // 출발지 위도
    val startLongitude: Double? = null, // 출발지 경도
    val endLatitude: Double? = null, // 도착지 위도
    val endLongitude: Double? = null // 도착지 경도
)

/**
 * UI Layer에서 사용하는 세부 경로 정보 모델
 */
data class SubPathUiModel(
    val trafficType: TrafficType, // 이동 수단 유형
    val distance: Double, // 이동 거리 (미터)
    val sectionTime: Int, // 이동 시간 (초)
    val startName: String? = null, // 승차 정류장/역 이름
    val endName: String? = null, // 하차 정류장/역 이름
    val stationCount: Int? = null, // 정거장 개수
    val lane: Lane? = null, // 노선 정보
    val walkDistance: Double? = null, // 도보 거리 (미터)
    val startLatitude: Double? = null, // 출발지 위도
    val startLongitude: Double? = null, // 출발지 경도
    val endLatitude: Double? = null, // 도착지 위도
    val endLongitude: Double? = null, // 도착지 경도
    val passStations: List<Station>? = null, // 경유 정류장 목록
    val graphicData: List<GraphicCoordinate>? = null // 노선 그래픽 데이터
)

/**
 * Route Domain Model → RouteUiModel 변환
 */
fun Route.toUiModel(): RouteUiModel = RouteUiModel(
    totalTime = totalTime,
    totalDistance = totalDistance,
    totalFare = totalFare,
    transferCount = transferCount,
    pathType = pathType,
    subPaths = subPaths.map { it.toUiModel() },
    startLatitude = startLatitude,
    startLongitude = startLongitude,
    endLatitude = endLatitude,
    endLongitude = endLongitude
)

/**
 * SubPath Domain Model → SubPathUiModel 변환
 */
fun SubPath.toUiModel(): SubPathUiModel = SubPathUiModel(
    trafficType = trafficType,
    distance = distance,
    sectionTime = sectionTime,
    startName = startName,
    endName = endName,
    stationCount = stationCount,
    lane = lane,
    walkDistance = walkDistance,
    startLatitude = startLatitude,
    startLongitude = startLongitude,
    endLatitude = endLatitude,
    endLongitude = endLongitude,
    passStations = passStations,
    graphicData = graphicData
)

/**
 * RouteUiModel → Route Domain Model 역변환
 * (경로 선택 시 Domain Layer로 전달할 때 사용)
 */
fun RouteUiModel.toDomainModel(): Route = Route(
    totalTime = totalTime,
    totalDistance = totalDistance,
    totalFare = totalFare,
    transferCount = transferCount,
    pathType = pathType,
    subPaths = subPaths.map { it.toDomainModel() },
    startLatitude = startLatitude,
    startLongitude = startLongitude,
    endLatitude = endLatitude,
    endLongitude = endLongitude
)

/**
 * SubPathUiModel → SubPath Domain Model 역변환
 */
fun SubPathUiModel.toDomainModel(): SubPath = SubPath(
    trafficType = trafficType,
    distance = distance,
    sectionTime = sectionTime,
    startName = startName,
    endName = endName,
    stationCount = stationCount,
    lane = lane,
    walkDistance = walkDistance,
    startLatitude = startLatitude,
    startLongitude = startLongitude,
    endLatitude = endLatitude,
    endLongitude = endLongitude,
    passStations = passStations,
    graphicData = graphicData
)

/**
 * List<Route> → List<RouteUiModel> 변환
 */
fun List<Route>.toUiModels(): List<RouteUiModel> = map { it.toUiModel() }
