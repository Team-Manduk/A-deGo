package com.teammanduk.adego.core.remote.datasource

import android.util.Log
import com.teammanduk.adego.core.data_api.datasource.ReverseGeocodingDataSource
import com.teammanduk.adego.core.data_api.model.PlaceDto
import com.teammanduk.adego.core.remote.model.TmapReverseGeocodingResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TMAP 역지오코딩 DataSource
 * 좌표를 입력받아 TMAP API를 통해 장소 정보를 가져옵니다.
 */
@Singleton
class TmapReverseGeocodingDataSource @Inject constructor(
    private val httpClient: HttpClient
) : ReverseGeocodingDataSource {

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): PlaceDto? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val apiKey = getTmapApiKey()
                if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                    Log.w(TAG, "TMAP API 키가 설정되지 않았습니다")
                    return@withContext null
                }

                Log.d(TAG, "TMAP 역지오코딩 API 호출: ($latitude, $longitude)")

                val response = httpClient.get("https://apis.openapi.sk.com/tmap/geo/reversegeocoding") {
                    header("appKey", apiKey)
                    parameter("version", "1")
                    parameter("lat", latitude)
                    parameter("lon", longitude)
                    parameter("coordType", "WGS84GEO")
                    parameter("addressType", "A02") // A02: 도로명 주소
                }

                if (response.status.value !in 200..299) {
                    Log.e(TAG, "TMAP 역지오코딩 API 요청 실패: ${response.status.value}")
                    return@withContext null
                }

                val tmapResponse = response.body<TmapReverseGeocodingResponse>()
                val addressInfo = tmapResponse.addressInfo

                Log.d(TAG, "TMAP 역지오코딩 응답:")
                Log.d(TAG, "  - 건물명: ${addressInfo?.buildingName}")
                Log.d(TAG, "  - 도로명: ${addressInfo?.roadName}")
                Log.d(TAG, "  - 건물번호: ${addressInfo?.buildingIndex}")
                Log.d(TAG, "  - 행정동: ${addressInfo?.adminDong}")
                Log.d(TAG, "  - 법정동: ${addressInfo?.legalDong}")
                Log.d(TAG, "  - fullAddress: ${addressInfo?.fullAddress}")

                // 장소 이름 결정 (건물명 > 도로명+건물번호 > 행정동 > 법정동)
                val placeName = when {
                    !addressInfo?.buildingName.isNullOrEmpty() -> addressInfo?.buildingName!!
                    !addressInfo?.roadName.isNullOrEmpty() && !addressInfo?.buildingIndex.isNullOrEmpty() ->
                        "${addressInfo?.roadName} ${addressInfo?.buildingIndex}"
                    !addressInfo?.adminDong.isNullOrEmpty() -> addressInfo?.adminDong!!
                    !addressInfo?.legalDong.isNullOrEmpty() -> addressInfo?.legalDong!!
                    else -> null // 유효한 정보가 없으면 null 반환하여 다음 단계로 폴백
                }

                // 장소 이름이 없으면 null 반환
                if (placeName == null) {
                    Log.w(TAG, "TMAP 역지오코딩 결과에 유효한 장소 정보가 없습니다")
                    return@withContext null
                }

                // 주소 (fullAddress 사용)
                val address = addressInfo?.fullAddress ?: "주소 정보 없음"

                Log.d(TAG, "생성된 장소명: $placeName")
                Log.d(TAG, "주소: $address")

                PlaceDto(
                    name = placeName,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    isPOI = false  // TMAP 역지오코딩 결과는 POI가 아님
                )
            } catch (e: Exception) {
                Log.e(TAG, "TMAP 역지오코딩 실패", e)
                null // 예외 발생 시 null 반환하여 다음 단계로 폴백
            }
        }

    private fun getTmapApiKey(): String {
        return try {
            val buildConfigClass = Class.forName("com.teammanduk.adego.BuildConfig")
            val field = buildConfigClass.getDeclaredField("TMAP_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "TMAP API 키를 가져올 수 없습니다", e)
            ""
        }
    }

    companion object {
        private const val TAG = "TmapReverseGeocoding"
    }
}
