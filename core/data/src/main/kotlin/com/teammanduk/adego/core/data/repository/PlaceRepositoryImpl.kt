package com.teammanduk.adego.core.data.repository

import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.model.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaceRepositoryImpl @Inject constructor() : PlaceRepository {

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
        // TODO: 나중에 실제 Geocoding API 연동
        // 현재는 테스트 데이터 반환
        val testPlace = Place(
            name = "부산 북구 만덕대로 291",
            address = "부산 북구 만덕동 607-1",
            latitude = latitude,
            longitude = longitude
        )
        _currentSearchResult.emit(testPlace)
        return testPlace
    }
}
