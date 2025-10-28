package com.teammanduk.adego.core.remote.model

import kotlinx.serialization.Serializable

/**
 * TMAP Pedestrian Route API 요청 모델
 * 보행자 경로 검색 API 요청 구조
 */
@Serializable
data class TmapPedestrianRequest(
    val startX: Double, // 출발지 경도
    val startY: Double, // 출발지 위도
    val endX: Double,   // 도착지 경도
    val endY: Double,   // 도착지 위도
    val reqCoordType: String = "WGS84GEO",
    val resCoordType: String = "WGS84GEO",
    val startName: String = "출발지",
    val endName: String = "도착지"
)

/**
 * TMAP Pedestrian Route API 응답 모델
 * 보행자 경로 검색 API 응답 구조
 */
@Serializable
data class TmapPedestrianResponse(
    val type: String? = null, // "FeatureCollection"
    val features: List<TmapFeature> = emptyList()
)

@Serializable
data class TmapFeature(
    val type: String? = null, // "Feature"
    val geometry: TmapGeometry? = null,
    val properties: TmapProperties? = null
)

@Serializable
data class TmapGeometry(
    val type: String? = null, // "Point" or "LineString"
    val coordinates: kotlinx.serialization.json.JsonElement? = null // Can be array of numbers or array of arrays
)

@Serializable
data class TmapProperties(
    val totalDistance: Int? = null,
    val totalTime: Int? = null,
    val index: Int? = null,
    val pointIndex: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val direction: String? = null,
    val nearPoiName: String? = null,
    val nearPoiX: String? = null,
    val nearPoiY: String? = null,
    val intersectionName: String? = null,
    val facilityType: String? = null,
    val facilityName: String? = null,
    val turnType: Int? = null,
    val pointType: String? = null,
    val lineIndex: Int? = null,
    val distance: Int? = null,
    val time: Int? = null,
    val roadType: Int? = null,
    val categoryRoadType: Int? = null
)
