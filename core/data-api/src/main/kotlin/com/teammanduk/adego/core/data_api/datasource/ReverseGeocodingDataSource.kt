package com.teammanduk.adego.core.data_api.datasource

import com.teammanduk.adego.core.data_api.model.PlaceDto

/**
 * 역지오코딩 DataSource
 * 좌표를 입력받아 장소 정보를 반환합니다.
 */
interface ReverseGeocodingDataSource {
    /**
     * 좌표로 장소 정보 검색
     * @return 검색 성공 시 PlaceDto, 실패 시 null
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): PlaceDto?
}
