package com.teammanduk.adego.core.remote.datasource

import com.teammanduk.adego.core.data_api.datasource.PlaceDataSource
import com.teammanduk.adego.core.data_api.model.PlaceDto
import javax.inject.Inject

class PlaceDataSourceImpl @Inject constructor() : PlaceDataSource {

    // TODO: Google Places API 연동 구현
    override suspend fun searchPlaces(query: String): List<PlaceDto> {
        return emptyList()
    }

    override suspend fun getPlaceDetails(placeId: String): PlaceDto? {
        return null
    }
}
