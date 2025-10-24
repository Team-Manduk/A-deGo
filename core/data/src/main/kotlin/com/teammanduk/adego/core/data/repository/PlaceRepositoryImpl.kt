package com.teammanduk.adego.core.data.repository

import com.teammanduk.adego.core.data.mapper.toModel
import com.teammanduk.adego.core.data_api.datasource.PlaceDataSource
import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.model.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceRepositoryImpl @Inject constructor(
    private val placeDataSource: PlaceDataSource
) : PlaceRepository {

    private val _selectedPlace = MutableStateFlow<Place?>(null)
    private val _currentSearchResult = MutableStateFlow<Place?>(null)

    override fun getSelectedPlace(): Flow<Place?> = _selectedPlace.asStateFlow()

    override suspend fun setSelectedPlace(place: Place) {
        _selectedPlace.emit(place)
    }

    override suspend fun clearSelectedPlace() {
        _selectedPlace.emit(null)
    }

    override fun getCurrentSearchResult(): Flow<Place?> = _currentSearchResult.asStateFlow()

    override suspend fun searchPlaceByCoordinates(latitude: Double, longitude: Double): Place? {
        val placeDto = placeDataSource.searchPlaceByCoordinates(latitude, longitude)
        val place = placeDto?.toModel()
        _currentSearchResult.emit(place)
        return place
    }

    override suspend fun searchPlacesByText(query: String): List<Place> {
        val placeDtos = placeDataSource.searchPlaces(query)
        return placeDtos.map { it.toModel() }
    }
}
