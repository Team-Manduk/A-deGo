package com.teammanduk.adego.core.remote.datasource

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.teammanduk.adego.core.data_api.datasource.ReverseGeocodingDataSource
import com.teammanduk.adego.core.data_api.model.PlaceDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Android Geocoder DataSource
 * Android 프레임워크의 Geocoder를 사용하여 역지오코딩을 수행합니다.
 * 주로 TMAP API 실패 시 폴백용으로 사용됩니다.
 */
@Singleton
class AndroidGeocoderDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : ReverseGeocodingDataSource {

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): PlaceDto? {
        val geocoder = Geocoder(context, Locale.KOREA)
        Log.d(TAG, "Geocoder 검색 시작: lat=$latitude, lng=$longitude")

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        Log.d(TAG, "Geocoder 응답: ${addresses.size}개 주소 발견")

                        val address = addresses.firstOrNull()

                        // 주소 정보 상세 로그
                        address?.let {
                            Log.d(TAG, "Geocoder 주소 정보:")
                            Log.d(TAG, "  - featureName: ${it.featureName}")
                            Log.d(TAG, "  - thoroughfare: ${it.thoroughfare}")
                            Log.d(TAG, "  - subLocality: ${it.subLocality}")
                            Log.d(TAG, "  - locality: ${it.locality}")
                            Log.d(TAG, "  - adminArea: ${it.adminArea}")
                            Log.d(TAG, "  - countryName: ${it.countryName}")
                            Log.d(TAG, "  - addressLine: ${it.getAddressLine(0)}")
                        }

                        val result = address?.let {
                            val placeName = buildPlaceName(it)
                            Log.d(TAG, "생성된 장소명: $placeName")
                            PlaceDto(
                                name = placeName,
                                address = it.getAddressLine(0) ?: "주소 정보 없음",
                                latitude = latitude,
                                longitude = longitude,
                                isPOI = false  // Geocoder 결과는 POI가 아님
                            )
                        } ?: run {
                            Log.w(TAG, "Geocoder에서 주소를 찾지 못했습니다")
                            createFallbackPlace(latitude, longitude)
                        }
                        continuation.resume(result)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                Log.d(TAG, "Geocoder 응답: ${addresses?.size ?: 0}개 주소 발견")

                val address = addresses?.firstOrNull()

                // 주소 정보 상세 로그
                address?.let {
                    Log.d(TAG, "Geocoder 주소 정보:")
                    Log.d(TAG, "  - featureName: ${it.featureName}")
                    Log.d(TAG, "  - thoroughfare: ${it.thoroughfare}")
                    Log.d(TAG, "  - subLocality: ${it.subLocality}")
                    Log.d(TAG, "  - locality: ${it.locality}")
                    Log.d(TAG, "  - adminArea: ${it.adminArea}")
                    Log.d(TAG, "  - countryName: ${it.countryName}")
                    Log.d(TAG, "  - addressLine: ${it.getAddressLine(0)}")
                }

                address?.let {
                    val placeName = buildPlaceName(it)
                    Log.d(TAG, "생성된 장소명: $placeName")
                    PlaceDto(
                        name = placeName,
                        address = it.getAddressLine(0) ?: "주소 정보 없음",
                        latitude = latitude,
                        longitude = longitude,
                        isPOI = false  // Geocoder 결과는 POI가 아님
                    )
                } ?: run {
                    Log.w(TAG, "Geocoder에서 주소를 찾지 못했습니다")
                    createFallbackPlace(latitude, longitude)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding failed", e)
            createFallbackPlace(latitude, longitude)
        }
    }

    /**
     * Geocoder Address 객체로부터 장소명을 생성합니다.
     * 우선순위: 건물명/상호명 > 동 이름 > 좌표
     * 예시: "강남역", "테헤란로 123", "역삼동"
     */
    private fun buildPlaceName(address: Address): String {
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

    private fun createFallbackPlace(latitude: Double, longitude: Double): PlaceDto {
        return PlaceDto(
            name = "위도: ${String.format("%.6f", latitude)}, 경도: ${String.format("%.6f", longitude)}",
            address = "주소를 가져올 수 없습니다",
            latitude = latitude,
            longitude = longitude,
            isPOI = false
        )
    }

    companion object {
        private const val TAG = "AndroidGeocoder"
    }
}
