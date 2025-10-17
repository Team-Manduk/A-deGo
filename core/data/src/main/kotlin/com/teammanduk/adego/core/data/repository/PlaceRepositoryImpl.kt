package com.teammanduk.adego.core.data.repository

import android.util.Log
import com.teammanduk.adego.core.data.mapper.toModel
import com.teammanduk.adego.core.data_api.datasource.PoiSearchDataSource
import com.teammanduk.adego.core.data_api.datasource.ReverseGeocodingDataSource
import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.core.remote.datasource.AndroidGeocoderDataSource
import com.teammanduk.adego.core.remote.datasource.TmapPoiDataSource
import com.teammanduk.adego.core.remote.datasource.TmapReverseGeocodingDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Place Repository 구현체
 *
 * 책임:
 * - 여러 DataSource를 조합하여 최적의 장소 정보 제공
 * - 폴백 전략 관리 (TMAP → POI → Geocoder)
 * - DTO → Domain Model 변환
 */
@Singleton
class PlaceRepositoryImpl @Inject constructor(
    private val tmapReverseGeocodingDataSource: TmapReverseGeocodingDataSource,
    private val tmapPoiDataSource: TmapPoiDataSource,
    private val androidGeocoderDataSource: AndroidGeocoderDataSource
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

    /**
     * 좌표로 장소 검색 (다단계 폴백 전략)
     *
     * 1단계: TMAP 역지오코딩
     * 2단계: TMAP POI 주변 검색
     * 3단계: Android Geocoder (폴백)
     */
    override suspend fun searchPlaceByCoordinates(latitude: Double, longitude: Double): Place? {
        return try {
            Log.d(TAG, "역지오코딩 시작: ($latitude, $longitude)")

            // 1단계: TMAP 역지오코딩 시도
            val tmapPlace = tmapReverseGeocodingDataSource.reverseGeocode(latitude, longitude)
            if (tmapPlace != null && !tmapPlace.name.contains("알 수 없는") && !tmapPlace.name.contains("위도:")) {
                Log.d(TAG, "TMAP 역지오코딩 성공: ${tmapPlace.name}")
                val place = tmapPlace.toModel()
                _currentSearchResult.emit(place)
                return place
            }

            // 2단계: TMAP POI 주변 검색 시도
            Log.d(TAG, "TMAP 실패 또는 결과 불충분, TMAP POI API 시도")
            val nearbyPlace = tmapPoiDataSource.searchNearby(latitude, longitude, radiusInMeters = 100)
            if (nearbyPlace != null) {
                Log.d(TAG, "TMAP POI API 성공: ${nearbyPlace.name}")
                val place = nearbyPlace.toModel()
                _currentSearchResult.emit(place)
                return place
            }

            // 3단계: Android Geocoder로 폴백
            Log.d(TAG, "TMAP POI API 실패, Android Geocoder 시도")
            val geocodedPlace = androidGeocoderDataSource.reverseGeocode(latitude, longitude)
            Log.d(TAG, "Android Geocoder 결과: ${geocodedPlace?.name}")
            val place = geocodedPlace?.toModel()
            _currentSearchResult.emit(place)
            place
        } catch (e: Exception) {
            Log.e(TAG, "Place search failed", e)
            // 모든 단계가 실패하면 null 반환
            _currentSearchResult.emit(null)
            null
        }
    }

    /**
     * 텍스트 쿼리로 장소 검색
     * TMAP POI 통합 검색 API 사용
     */
    override suspend fun searchPlacesByText(query: String): List<Place> {
        return try {
            val placeDtos = tmapPoiDataSource.searchByText(query)
            placeDtos.map { it.toModel() }
        } catch (e: Exception) {
            Log.e(TAG, "Text search failed", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "PlaceRepository"
    }
}
