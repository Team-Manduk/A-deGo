package com.teammanduk.adego.core.remote.model

import kotlinx.serialization.Serializable

/**
 * TMAP Transit API 응답 모델
 * 대중교통 경로 검색 API 응답 구조
 */
@Serializable
data class TmapTransitResponse(
    val metaData: TmapTransitMetaData? = null
)

@Serializable
data class TmapTransitMetaData(
    val requestParameters: TmapRequestParameters? = null,
    val plan: TmapPlan? = null
)

@Serializable
data class TmapRequestParameters(
    val reqDttm: String? = null, // 요청 일시
    val startX: String? = null, // 출발지 경도
    val startY: String? = null, // 출발지 위도
    val endX: String? = null, // 도착지 경도
    val endY: String? = null, // 도착지 위도
    val locale: String? = null, // 언어 설정
    val busCount: Int? = null, // 버스 경로 개수
    val expressbusCount: Int? = null, // 고속버스 경로 개수
    val subwayCount: Int? = null, // 지하철 경로 개수
    val airplaneCount: Int? = null, // 비행기 경로 개수
    val ferryCount: Int? = null, // 배 경로 개수
    val trainCount: Int? = null // 기차 경로 개수
)

@Serializable
data class TmapPlan(
    val itineraries: List<TmapItinerary> = emptyList()
)

@Serializable
data class TmapItinerary(
    val fare: TmapFare? = null,
    val totalTime: Int? = null, // 총 소요 시간 (초)
    val totalDistance: Double? = null, // 총 거리 (미터)
    val totalWalkTime: Int? = null, // 총 도보 시간 (초)
    val totalWalkDistance: Double? = null, // 총 도보 거리 (미터)
    val legs: List<TmapLeg> = emptyList(),
    val pathType: Int? = null // 경로 타입
)

@Serializable
data class TmapFare(
    val regular: TmapFareDetail? = null
)

@Serializable
data class TmapFareDetail(
    val totalFare: Int? = null, // 총 요금
    val currency: TmapCurrency? = null
)

@Serializable
data class TmapCurrency(
    val symbol: String? = null,
    val currency: String? = null,
    val currencyCode: String? = null
)

@Serializable
data class TmapLeg(
    val mode: String? = null, // "WALK", "BUS", "SUBWAY", etc.
    val sectionTime: Int? = null, // 구간 시간 (초)
    val distance: Double? = null, // 거리 (미터)
    val start: TmapPlace? = null,
    val end: TmapPlace? = null,
    val steps: List<TmapStep>? = null, // 도보 구간의 경우
    val route: String? = null, // 노선명 (대중교통)
    val routeColor: String? = null, // 노선 색상
    val routeId: String? = null, // 노선 ID
    val service: Int? = null, // 노선 타입
    val passStopList: TmapPassStopList? = null, // 경유 정류장 목록
    val passShape: TmapPassShape? = null // 경로 좌표
)

@Serializable
data class TmapPlace(
    val name: String? = null,
    val lon: Double? = null,
    val lat: Double? = null
)

@Serializable
data class TmapStep(
    val streetName: String? = null,
    val distance: Double? = null,
    val description: String? = null,
    val linestring: String? = null // 경로 좌표 (WKT 형식)
)

@Serializable
data class TmapPassStopList(
    @kotlinx.serialization.SerialName("stationList")
    val stations: List<TmapStation>? = null
)

@Serializable
data class TmapStation(
    val index: Int? = null,
    val stationName: String? = null,
    val lon: String? = null, // API에서 문자열로 옴
    val lat: String? = null, // API에서 문자열로 옴
    val stationID: String? = null
)

@Serializable
data class TmapPassShape(
    val linestring: String? = null // WKT LINESTRING 형식
)
