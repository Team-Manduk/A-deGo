package com.teammanduk.adego.core.data_api.datasource

import com.teammanduk.adego.core.data_api.model.PlaceDto

/**
 * POI (Point of Interest) 검색 DataSource
 * 좌표 주변이나 텍스트 쿼리로 POI를 검색합니다.
 */
interface PoiSearchDataSource {
    /**
     * 좌표 주변의 POI 검색
     * @param latitude 위도
     * @param longitude 경도
     * @param radiusInMeters 검색 반경 (미터)
     * @return 검색 성공 시 가장 가까운 PlaceDto, 실패 시 null
     */
    suspend fun searchNearby(
        latitude: Double,
        longitude: Double,
        radiusInMeters: Int = 100
    ): PlaceDto?

    /**
     * 텍스트 쿼리로 POI 검색
     * @param query 검색 쿼리
     * @return 검색된 장소 목록 (빈 리스트 가능)
     */
    suspend fun searchByText(query: String): List<PlaceDto>
}
