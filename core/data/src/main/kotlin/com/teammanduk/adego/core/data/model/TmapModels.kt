package com.teammanduk.adego.core.data.model

import kotlinx.serialization.Serializable

/**
 * TMAP 역지오코딩 API 응답 모델
 */
@Serializable
data class TmapReverseGeocodingResponse(
    val addressInfo: TmapAddressInfo? = null
)

@Serializable
data class TmapAddressInfo(
    val fullAddress: String? = null,
    val addressType: String? = null,
    val city_do: String? = null, // 시/도
    val gu_gun: String? = null, // 구/군
    val eup_myun: String? = null, // 읍/면
    val adminDong: String? = null, // 행정동
    val adminDongCode: String? = null,
    val legalDong: String? = null, // 법정동
    val legalDongCode: String? = null,
    val ri: String? = null, // 리
    val bunji: String? = null, // 번지
    val roadName: String? = null, // 도로명
    val buildingIndex: String? = null, // 건물번호
    val buildingName: String? = null, // 건물명
    val mappingDistance: String? = null,
    val roadCode: String? = null
)
