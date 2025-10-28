package com.teammanduk.adego.core.remote.model

import kotlinx.serialization.Serializable

/**
 * TMAP POI API 응답 모델
 */
@Serializable
data class TmapPoiResponse(
    val searchPoiInfo: TmapSearchPoiInfo? = null
)

@Serializable
data class TmapSearchPoiInfo(
    val totalCount: String? = null,
    val count: String? = null,
    val page: String? = null,
    val pois: TmapPois? = null
)

@Serializable
data class TmapPois(
    val poi: List<TmapPoi> = emptyList()
)

@Serializable
data class TmapPoi(
    val id: String? = null,
    val name: String? = null,
    val telNo: String? = null,
    val frontLat: String? = null, // 위도
    val frontLon: String? = null, // 경도
    val noorLat: String? = null,
    val noorLon: String? = null,
    val upperAddrName: String? = null,
    val middleAddrName: String? = null,
    val lowerAddrName: String? = null,
    val detailAddrName: String? = null,
    val mlClass: String? = null,
    val firstNo: String? = null,
    val secondNo: String? = null,
    val roadName: String? = null,
    val firstBuildNo: String? = null,
    val secondBuildNo: String? = null,
    val radius: String? = null,
    val bizName: String? = null,
    val upperBizName: String? = null,
    val middleBizName: String? = null,
    val lowerBizName: String? = null,
    val detailBizName: String? = null,
    val rpFlag: String? = null,
    val parkFlag: String? = null,
    val detailInfoFlag: String? = null,
    val navSeq: String? = null,
    val analyticGoods: String? = null,
    val roadNameYn: String? = null
)
