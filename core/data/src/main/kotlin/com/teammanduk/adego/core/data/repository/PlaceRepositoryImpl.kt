package com.teammanduk.adego.core.data.repository

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.model.Place
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PlaceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
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
        val geocoder = Geocoder(context, Locale.KOREA)

        try {
            val place = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 이상: 비동기 API 사용
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val result = if (address != null) {
                            Place(
                                name = buildPlaceName(address),
                                address = address.getAddressLine(0) ?: "주소 정보 없음",
                                latitude = latitude,
                                longitude = longitude
                            )
                        } else {
                            createFallbackPlace(latitude, longitude)
                        }
                        continuation.resume(result)
                    }
                }
            } else {
                // Android 12 이하: 동기 API 사용
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()

                if (address != null) {
                    Place(
                        name = buildPlaceName(address),
                        address = address.getAddressLine(0) ?: "주소 정보 없음",
                        latitude = latitude,
                        longitude = longitude
                    )
                } else {
                    createFallbackPlace(latitude, longitude)
                }
            }

            _currentSearchResult.emit(place)
            return place

        } catch (e: Exception) {
            Log.e("PlaceRepository", "Geocoding failed", e)
            val fallbackPlace = createFallbackPlace(latitude, longitude)
            _currentSearchResult.emit(fallbackPlace)
            return fallbackPlace
        }
    }

    private fun createFallbackPlace(latitude: Double, longitude: Double): Place {
        return Place(
            name = "위도: ${String.format("%.6f", latitude)}, 경도: ${String.format("%.6f", longitude)}",
            address = "주소를 가져올 수 없습니다",
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * Geocoder Address 객체로부터 장소명을 생성합니다.
     * 우선순위: 건물명/상호명 > 동 이름 > 좌표
     * 예시: "강남역", "테헤란로 123", "역삼동"
     */
    private fun buildPlaceName(address: android.location.Address): String {
        // 1순위: 건물명, 랜드마크, 상호명 (featureName)
        address.featureName?.let { featureName ->
            // featureName이 번지수 형태인 경우 제외
            // 번지수 패턴: 숫자, 하이픈, 공백만으로 구성 (예: "569-10", "123", "123-45 67")
            val isBeonji = featureName.all { it.isDigit() || it == '-' || it.isWhitespace() }
            if (!isBeonji) {
                return featureName
            }
        }

        // 2순위: 동/읍/면 이름 (subLocality)
        address.subLocality?.let { return it }

        // 3순위: 시/군 이름 (locality)
        address.locality?.let { return it }

        // 마지막: 좌표 정보
        return "위도: ${String.format("%.6f", address.latitude)}, 경도: ${String.format("%.6f", address.longitude)}"
    }
}
