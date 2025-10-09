package com.teammanduk.adego.core.data_api.datasource

import com.teammanduk.adego.core.data_api.model.PlaceDto

interface PlaceDataSource {
    suspend fun searchPlaces(query: String): List<PlaceDto>
    suspend fun getPlaceDetails(placeId: String): PlaceDto?
}
