package com.teammanduk.adego.core.remote.datasource

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.teammanduk.adego.core.data.model.TmapReverseGeocodingResponse
import com.teammanduk.adego.core.data_api.datasource.PlaceDataSource
import com.teammanduk.adego.core.data_api.model.PlaceDto
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// TMAP POI API Response Models
@Serializable
private data class TmapPoiResponse(
    val searchPoiInfo: TmapSearchPoiInfo? = null
)

@Serializable
private data class TmapSearchPoiInfo(
    val totalCount: String? = null,
    val count: String? = null,
    val page: String? = null,
    val pois: TmapPois? = null
)

@Serializable
private data class TmapPois(
    val poi: List<TmapPoi> = emptyList()
)

@Serializable
private data class TmapPoi(
    val id: String? = null,
    val name: String? = null,
    val telNo: String? = null,
    val frontLat: String? = null, // 위도
    val frontLon: String? = null, // 경도
    val noorLat: String? = null,
    val noorLon: String? = null,
    val upperAddrName: String? = null,
    val middleAddrName: String? = null,
    val lowerAddrName: String? = null,
    val detailAddrName: String? = null,
    val mlClass: String? = null,
    val firstNo: String? = null,
    val secondNo: String? = null,
    val roadName: String? = null,
    val firstBuildNo: String? = null,
    val secondBuildNo: String? = null,
    val radius: String? = null,
    val bizName: String? = null,
    val upperBizName: String? = null,
    val middleBizName: String? = null,
    val lowerBizName: String? = null,
    val detailBizName: String? = null,
    val rpFlag: String? = null,
    val parkFlag: String? = null,
    val detailInfoFlag: String? = null,
    val navSeq: String? = null,
    val analyticGoods: String? = null,
    val roadNameYn: String? = null
)

@Singleton
class TmapPlaceDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlaceDataSource {

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d(TAG, message)
                }
            }
            level = LogLevel.ALL
        }
    }

    override suspend fun searchPlaces(query: String): List<PlaceDto> {
        return searchPlacesByText(query)
    }

    override suspend fun getPlaceDetails(placeId: String): PlaceDto? {
        // TODO: TMAP POI 상세 정보 API 구현
        return null
    }

    override suspend fun searchPlaceByCoordinates(latitude: Double, longitude: Double): PlaceDto? {
        return try {
            Log.d(TAG, "역지오코딩 시작: ($latitude, $longitude)")

            // 1단계: TMAP 역지오코딩 시도
            val tmapPlace = searchUsingTmapReverseGeocoding(latitude, longitude)
            if (tmapPlace != null && !tmapPlace.name.contains("알 수 없는") && !tmapPlace.name.contains("위도:")) {
                Log.d(TAG, "TMAP 역지오코딩 성공: ${tmapPlace.name}")
                return tmapPlace
            }

            // 2단계: TMAP POI 주변 검색 시도
            Log.d(TAG, "TMAP 실패 또는 결과 불충분, TMAP POI API 시도")
            val nearbyPlace = findNearbyPlaceUsingTmapPoi(latitude, longitude)
            if (nearbyPlace != null) {
                Log.d(TAG, "TMAP POI API 성공: ${nearbyPlace.name}")
                return nearbyPlace
            }

            // 3단계: Android Geocoder로 폴백
            Log.d(TAG, "TMAP POI API 실패, Android Geocoder 시도")
            val geocodedPlace = searchUsingGeocoder(latitude, longitude)
            Log.d(TAG, "Android Geocoder 결과: ${geocodedPlace.name}")
            geocodedPlace
        } catch (e: Exception) {
            Log.e(TAG, "Place search failed", e)
            createFallbackPlace(latitude, longitude)
        }
    }

    /**
     * TMAP 역지오코딩 API를 사용하여 주소 정보를 가져옵니다.
     */
    private suspend fun searchUsingTmapReverseGeocoding(
        latitude: Double,
        longitude: Double
    ): PlaceDto? = withContext(Dispatchers.IO) {
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

            // 장소 이름 결정
            val placeName = when {
                !addressInfo?.buildingName.isNullOrEmpty() -> addressInfo?.buildingName!!
                !addressInfo?.roadName.isNullOrEmpty() && !addressInfo?.buildingIndex.isNullOrEmpty() ->
                    "${addressInfo?.roadName} ${addressInfo?.buildingIndex}"
                !addressInfo?.adminDong.isNullOrEmpty() -> addressInfo?.adminDong!!
                !addressInfo?.legalDong.isNullOrEmpty() -> addressInfo?.legalDong!!
                else -> null
            }

            if (placeName == null) {
                Log.w(TAG, "TMAP 역지오코딩 결과에 유효한 장소 정보가 없습니다")
                return@withContext null
            }

            PlaceDto(
                name = placeName,
                address = addressInfo?.fullAddress ?: "주소 정보 없음",
                latitude = latitude,
                longitude = longitude
            )
        } catch (e: Exception) {
            Log.e(TAG, "TMAP 역지오코딩 실패", e)
            null
        }
    }

    /**
     * TMAP POI API를 사용하여 좌표 근처의 장소를 검색합니다.
     */
    private suspend fun findNearbyPlaceUsingTmapPoi(
        latitude: Double,
        longitude: Double
    ): PlaceDto? = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.e(TAG, "TMAP API 키를 찾을 수 없습니다")
                return@withContext null
            }

            val response = httpClient.get("https://apis.openapi.sk.com/tmap/pois/search/around") {
                header("appKey", apiKey)
                parameter("version", "1")
                parameter("centerLon", longitude.toString())
                parameter("centerLat", latitude.toString())
                parameter("radius", "100") // 100m 반경
                parameter("searchtypCd", "A") // 전체 검색
                parameter("reqCoordType", "WGS84GEO")
                parameter("resCoordType", "WGS84GEO")
                parameter("count", "10")
            }

            if (response.status.value !in 200..299) {
                Log.e(TAG, "TMAP POI API 요청 실패: ${response.status.value}")
                return@withContext null
            }

            val poiResponse = response.body<TmapPoiResponse>()
            val pois = poiResponse.searchPoiInfo?.pois?.poi ?: emptyList()

            // 가장 가까운 장소 찾기
            val nearestPoi = pois
                .filter { it.frontLat != null && it.frontLon != null }
                .minByOrNull { poi ->
                    val poiLat = poi.frontLat!!.toDoubleOrNull() ?: return@minByOrNull Double.MAX_VALUE
                    val poiLon = poi.frontLon!!.toDoubleOrNull() ?: return@minByOrNull Double.MAX_VALUE
                    calculateDistance(latitude, longitude, poiLat, poiLon)
                }

            nearestPoi?.let { poi ->
                val placeName = poi.name ?: "알 수 없는 장소"
                val placeAddress = buildString {
                    poi.upperAddrName?.let { append(it).append(" ") }
                    poi.middleAddrName?.let { append(it).append(" ") }
                    poi.lowerAddrName?.let { append(it).append(" ") }
                    poi.detailAddrName?.let { append(it) }
                }.trim().ifEmpty { "주소 정보 없음" }

                PlaceDto(
                    name = placeName,
                    address = placeAddress,
                    latitude = latitude,
                    longitude = longitude
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "TMAP POI search failed", e)
            null
        }
    }

    /**
     * Geocoder를 사용하여 주소 정보를 가져옵니다. (폴백용)
     */
    private suspend fun searchUsingGeocoder(latitude: Double, longitude: Double): PlaceDto {
        val geocoder = Geocoder(context, Locale.KOREA)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        val result = if (address != null) {
                            val placeName = buildPlaceName(address)
                            PlaceDto(
                                name = placeName,
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
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()

                if (address != null) {
                    val placeName = buildPlaceName(address)
                    PlaceDto(
                        name = placeName,
                        address = address.getAddressLine(0) ?: "주소 정보 없음",
                        latitude = latitude,
                        longitude = longitude
                    )
                } else {
                    createFallbackPlace(latitude, longitude)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding failed", e)
            createFallbackPlace(latitude, longitude)
        }
    }

    /**
     * TMAP POI API를 사용하여 텍스트 쿼리로 장소를 검색합니다.
     */
    private suspend fun searchPlacesByText(query: String): List<PlaceDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (query.isBlank()) {
                return@withContext emptyList()
            }

            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.e(TAG, "TMAP API 키를 찾을 수 없습니다")
                return@withContext emptyList()
            }

            val response = httpClient.get("https://apis.openapi.sk.com/tmap/pois") {
                header("appKey", apiKey)
                parameter("version", "1")
                parameter("searchKeyword", query)
                parameter("resCoordType", "WGS84GEO")
                parameter("reqCoordType", "WGS84GEO")
                parameter("count", "20")
            }

            if (response.status.value !in 200..299) {
                Log.e(TAG, "TMAP POI 통합 검색 요청 실패: ${response.status.value}")
                return@withContext emptyList()
            }

            val poiResponse = response.body<TmapPoiResponse>()
            val pois = poiResponse.searchPoiInfo?.pois?.poi ?: emptyList()

            pois.mapNotNull { poi ->
                val name = poi.name
                val lat = poi.frontLat?.toDoubleOrNull()
                val lon = poi.frontLon?.toDoubleOrNull()

                val address = buildString {
                    poi.upperAddrName?.let { append(it).append(" ") }
                    poi.middleAddrName?.let { append(it).append(" ") }
                    poi.lowerAddrName?.let { append(it).append(" ") }
                    poi.detailAddrName?.let { append(it) }
                }.trim().ifEmpty { "주소 정보 없음" }

                if (name != null && lat != null && lon != null) {
                    PlaceDto(
                        name = name,
                        address = address,
                        latitude = lat,
                        longitude = lon
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TMAP POI Text Search failed", e)
            emptyList()
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

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLng = Math.toRadians(lon2 - lon1)

        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLng / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun createFallbackPlace(latitude: Double, longitude: Double): PlaceDto {
        return PlaceDto(
            name = "위도: ${String.format("%.6f", latitude)}, 경도: ${String.format("%.6f", longitude)}",
            address = "주소를 가져올 수 없습니다",
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun buildPlaceName(address: android.location.Address): String {
        // 1순위: 건물명, 랜드마크, 상호명 (featureName)
        address.featureName?.let { featureName ->
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

    companion object {
        private const val TAG = "TmapPlaceDataSource"
    }
}
