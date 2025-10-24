package com.teammanduk.adego.core.data_api.datasource

import com.teammanduk.adego.core.data_api.model.PlaceDto

interface PlaceDataSource {
    /**
     * 텍스트 검색으로 장소 목록 조회
     */
    suspend fun searchPlaces(query: String): List<PlaceDto>

    /**
     * 장소 상세 정보 조회
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDto?

    /**
     * 좌표 기반 역지오코딩 (TMAP API, POI 검색, Android Geocoder 시도)
     * @return 검색된 장소 정보, 실패 시 null
     */
    suspend fun searchPlaceByCoordinates(latitude: Double, longitude: Double): PlaceDto?
}
