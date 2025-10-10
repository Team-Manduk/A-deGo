package com.teammanduk.adego.core.domain.repository

import com.teammanduk.adego.core.model.Place
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    // 최종 선택된 장소
    fun getSelectedPlace(): Flow<Place?>
    suspend fun setSelectedPlace(place: Place)
    suspend fun clearSelectedPlace()

    // 검색/임시 선택 장소
    fun getCurrentSearchResult(): Flow<Place?>
    suspend fun searchPlaceByCoordinates(latitude: Double, longitude: Double): Place?
    suspend fun searchPlacesByText(query: String): List<Place>
}
