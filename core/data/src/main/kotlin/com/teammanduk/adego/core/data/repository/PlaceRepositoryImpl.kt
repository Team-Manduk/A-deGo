package com.teammanduk.adego.core.data.repository

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.teammanduk.adego.core.data.model.TmapReverseGeocodingResponse
import com.teammanduk.adego.core.domain.repository.PlaceRepository
import com.teammanduk.adego.core.model.Place
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class PlaceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlaceRepository {

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
                    Log.d("KtorClient", message)
                }
            }
            level = LogLevel.ALL
        }
    }

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
        return try {
            Log.d("PlaceRepository", "역지오코딩 시작: ($latitude, $longitude)")

            // 1단계: TMAP 역지오코딩 시도
            val tmapPlace = searchUsingTmapReverseGeocoding(latitude, longitude)
            if (tmapPlace != null && !tmapPlace.name.contains("알 수 없는") && !tmapPlace.name.contains("위도:")) {
                Log.d("PlaceRepository", "TMAP 역지오코딩 성공: ${tmapPlace.name}")
                _currentSearchResult.emit(tmapPlace)
                return tmapPlace
            }

            // 2단계: TMAP POI 주변 검색 시도
            Log.d("PlaceRepository", "TMAP 실패 또는 결과 불충분, TMAP POI API 시도")
            val nearbyPlace = findNearbyPlaceUsingTmapPoi(latitude, longitude)
            if (nearbyPlace != null) {
                Log.d("PlaceRepository", "TMAP POI API 성공: ${nearbyPlace.name}")
                _currentSearchResult.emit(nearbyPlace)
                return nearbyPlace
            }

            // 3단계: Android Geocoder로 폴백
            Log.d("PlaceRepository", "TMAP POI API 실패, Android Geocoder 시도")
            val geocodedPlace = searchUsingGeocoder(latitude, longitude)
            Log.d("PlaceRepository", "Android Geocoder 결과: ${geocodedPlace.name}")
            _currentSearchResult.emit(geocodedPlace)
            geocodedPlace
        } catch (e: Exception) {
            Log.e("PlaceRepository", "Place search failed", e)
            val fallbackPlace = createFallbackPlace(latitude, longitude)
            _currentSearchResult.emit(fallbackPlace)
            fallbackPlace
        }
    }

    /**
     * TMAP POI API를 사용하여 좌표 근처의 장소를 검색합니다.
     */
    private suspend fun findNearbyPlaceUsingTmapPoi(
        latitude: Double,
        longitude: Double
    ): Place? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PlaceRepository", "TMAP POI 주변 검색 시작: lat=$latitude, lng=$longitude")

            // API 키 가져오기
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.e("PlaceRepository", "TMAP API 키를 찾을 수 없습니다")
                return@withContext null
            }

            // TMAP POI 주변 검색 API 호출
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

            val responseBody = response.bodyAsText()
            Log.d("PlaceRepository", "TMAP POI API 응답 코드: ${response.status.value}")
            Log.d("PlaceRepository", "TMAP POI API 응답: $responseBody")

            if (response.status.value !in 200..299) {
                Log.e("PlaceRepository", "TMAP POI API 요청 실패: ${response.status.value}")
                return@withContext null
            }

            // 응답 파싱
            val poiResponse = response.body<TmapPoiResponse>()
            val pois = poiResponse.searchPoiInfo?.pois?.poi ?: emptyList()

            Log.d("PlaceRepository", "TMAP POI API: ${pois.size}개 장소 발견")

            // 모든 장소 정보 로그 출력
            pois.forEachIndexed { index, poi ->
                Log.d("PlaceRepository", "장소 #$index:")
                Log.d("PlaceRepository", "  - 이름: ${poi.name}")
                Log.d("PlaceRepository", "  - 주소: ${poi.upperAddrName} ${poi.middleAddrName} ${poi.lowerAddrName}")
                Log.d("PlaceRepository", "  - 좌표: (${poi.frontLat}, ${poi.frontLon})")
            }

            // 가장 가까운 장소 찾기
            val nearestPoi = pois
                .filter { it.frontLat != null && it.frontLon != null }
                .minByOrNull { poi ->
                    val poiLat = poi.frontLat!!.toDoubleOrNull() ?: return@minByOrNull Double.MAX_VALUE
                    val poiLon = poi.frontLon!!.toDoubleOrNull() ?: return@minByOrNull Double.MAX_VALUE
                    val distance = calculateDistance(latitude, longitude, poiLat, poiLon)
                    Log.d("PlaceRepository", "${poi.name}까지 거리: ${distance}m")
                    distance
                }

            nearestPoi?.let { poi ->
                val placeName = poi.name ?: "알 수 없는 장소"
                val placeAddress = buildString {
                    poi.upperAddrName?.let { append(it).append(" ") }
                    poi.middleAddrName?.let { append(it).append(" ") }
                    poi.lowerAddrName?.let { append(it).append(" ") }
                    poi.detailAddrName?.let { append(it) }
                }.trim().ifEmpty { "주소 정보 없음" }

                Log.d("PlaceRepository", "선택된 장소: $placeName")
                Log.d("PlaceRepository", "주소: $placeAddress")

                Place(
                    name = placeName,
                    address = placeAddress,
                    latitude = latitude,
                    longitude = longitude
                )
            }.also {
                if (it == null) {
                    Log.w("PlaceRepository", "TMAP POI API에서 가장 가까운 장소를 찾지 못했습니다")
                }
            }
        } catch (e: Exception) {
            Log.e("PlaceRepository", "TMAP POI search failed", e)
            null
        }
    }

    private fun getTmapApiKey(): String {
        return try {
            val buildConfigClass = Class.forName("com.teammanduk.adego.BuildConfig")
            val field = buildConfigClass.getDeclaredField("TMAP_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            Log.e("PlaceRepository", "TMAP API 키를 가져올 수 없습니다", e)
            ""
        }
    }

    /**
     * TMAP 역지오코딩 API를 사용하여 주소 정보를 가져옵니다.
     * 실패 시 null 반환 (다음 단계로 폴백)
     */
    private suspend fun searchUsingTmapReverseGeocoding(latitude: Double, longitude: Double): Place? = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.w("PlaceRepository", "TMAP API 키가 설정되지 않았습니다")
                return@withContext null
            }

            Log.d("PlaceRepository", "TMAP 역지오코딩 API 호출: ($latitude, $longitude)")

            val response = httpClient.get("https://apis.openapi.sk.com/tmap/geo/reversegeocoding") {
                header("appKey", apiKey)
                parameter("version", "1")
                parameter("lat", latitude)
                parameter("lon", longitude)
                parameter("coordType", "WGS84GEO")
                parameter("addressType", "A02") // A02: 도로명 주소
            }

            if (response.status.value !in 200..299) {
                Log.e("PlaceRepository", "TMAP 역지오코딩 API 요청 실패: ${response.status.value}")
                return@withContext null
            }

            val tmapResponse = response.body<TmapReverseGeocodingResponse>()
            val addressInfo = tmapResponse.addressInfo

            Log.d("PlaceRepository", "TMAP 역지오코딩 응답:")
            Log.d("PlaceRepository", "  - 건물명: ${addressInfo?.buildingName}")
            Log.d("PlaceRepository", "  - 도로명: ${addressInfo?.roadName}")
            Log.d("PlaceRepository", "  - 건물번호: ${addressInfo?.buildingIndex}")
            Log.d("PlaceRepository", "  - 행정동: ${addressInfo?.adminDong}")
            Log.d("PlaceRepository", "  - 법정동: ${addressInfo?.legalDong}")
            Log.d("PlaceRepository", "  - fullAddress: ${addressInfo?.fullAddress}")

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
                Log.w("PlaceRepository", "TMAP 역지오코딩 결과에 유효한 장소 정보가 없습니다")
                return@withContext null
            }

            // 주소 (fullAddress 사용)
            val address = addressInfo?.fullAddress ?: "주소 정보 없음"

            Log.d("PlaceRepository", "생성된 장소명: $placeName")
            Log.d("PlaceRepository", "주소: $address")

            Place(
                name = placeName,
                address = address,
                latitude = latitude,
                longitude = longitude,
                isPOI = false  // TMAP 역지오코딩 결과는 POI가 아님
            )
        } catch (e: Exception) {
            Log.e("PlaceRepository", "TMAP 역지오코딩 실패", e)
            null // 예외 발생 시 null 반환하여 다음 단계로 폴백
        }
    }

    /**
     * Geocoder를 사용하여 주소 정보를 가져옵니다. (폴백용)
     */
    private suspend fun searchUsingGeocoder(latitude: Double, longitude: Double): Place {
        val geocoder = Geocoder(context, Locale.KOREA)
        Log.d("PlaceRepository", "Geocoder 검색 시작: lat=$latitude, lng=$longitude")

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        Log.d("PlaceRepository", "Geocoder 응답: ${addresses.size}개 주소 발견")

                        val address = addresses.firstOrNull()

                        // 주소 정보 상세 로그
                        address?.let {
                            Log.d("PlaceRepository", "Geocoder 주소 정보:")
                            Log.d("PlaceRepository", "  - featureName: ${it.featureName}")
                            Log.d("PlaceRepository", "  - thoroughfare: ${it.thoroughfare}")
                            Log.d("PlaceRepository", "  - subLocality: ${it.subLocality}")
                            Log.d("PlaceRepository", "  - locality: ${it.locality}")
                            Log.d("PlaceRepository", "  - adminArea: ${it.adminArea}")
                            Log.d("PlaceRepository", "  - countryName: ${it.countryName}")
                            Log.d("PlaceRepository", "  - addressLine: ${it.getAddressLine(0)}")
                        }

                        val result = if (address != null) {
                            val placeName = buildPlaceName(address)
                            Log.d("PlaceRepository", "생성된 장소명: $placeName")
                            Place(
                                name = placeName,
                                address = address.getAddressLine(0) ?: "주소 정보 없음",
                                latitude = latitude,
                                longitude = longitude,
                                isPOI = false  // Geocoder 결과도 POI가 아님
                            )
                        } else {
                            Log.w("PlaceRepository", "Geocoder에서 주소를 찾지 못했습니다")
                            createFallbackPlace(latitude, longitude)
                        }
                        continuation.resume(result)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                Log.d("PlaceRepository", "Geocoder 응답: ${addresses?.size ?: 0}개 주소 발견")

                val address = addresses?.firstOrNull()

                // 주소 정보 상세 로그
                address?.let {
                    Log.d("PlaceRepository", "Geocoder 주소 정보:")
                    Log.d("PlaceRepository", "  - featureName: ${it.featureName}")
                    Log.d("PlaceRepository", "  - thoroughfare: ${it.thoroughfare}")
                    Log.d("PlaceRepository", "  - subLocality: ${it.subLocality}")
                    Log.d("PlaceRepository", "  - locality: ${it.locality}")
                    Log.d("PlaceRepository", "  - adminArea: ${it.adminArea}")
                    Log.d("PlaceRepository", "  - countryName: ${it.countryName}")
                    Log.d("PlaceRepository", "  - addressLine: ${it.getAddressLine(0)}")
                }

                if (address != null) {
                    val placeName = buildPlaceName(address)
                    Log.d("PlaceRepository", "생성된 장소명: $placeName")
                    Place(
                        name = placeName,
                        address = address.getAddressLine(0) ?: "주소 정보 없음",
                        latitude = latitude,
                        longitude = longitude,
                        isPOI = false  // Geocoder 결과도 POI가 아님
                    )
                } else {
                    Log.w("PlaceRepository", "Geocoder에서 주소를 찾지 못했습니다")
                    createFallbackPlace(latitude, longitude)
                }
            }
        } catch (e: Exception) {
            Log.e("PlaceRepository", "Geocoding failed", e)
            createFallbackPlace(latitude, longitude)
        }
    }

    /**
     * 두 좌표 사이의 거리를 미터 단위로 계산합니다. (Haversine formula)
     */
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

    private fun createFallbackPlace(latitude: Double, longitude: Double): Place {
        return Place(
            name = "위도: ${String.format("%.6f", latitude)}, 경도: ${String.format("%.6f", longitude)}",
            address = "주소를 가져올 수 없습니다",
            latitude = latitude,
            longitude = longitude,
            isPOI = false  // Fallback도 POI가 아님
        )
    }

    /**
     * TMAP POI API를 사용하여 텍스트 쿼리로 장소를 검색합니다.
     */
    override suspend fun searchPlacesByText(query: String): List<Place> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (query.isBlank()) {
                Log.d("PlaceRepository", "검색 쿼리가 비어있습니다")
                return@withContext emptyList()
            }

            Log.d("PlaceRepository", "TMAP POI 통합 검색 시작: query=$query")

            // API 키 가져오기
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.e("PlaceRepository", "TMAP API 키를 찾을 수 없습니다")
                return@withContext emptyList()
            }

            // TMAP POI 통합 검색 API 호출
            val response = httpClient.get("https://apis.openapi.sk.com/tmap/pois") {
                header("appKey", apiKey)
                parameter("version", "1")
                parameter("searchKeyword", query)
                parameter("resCoordType", "WGS84GEO")
                parameter("reqCoordType", "WGS84GEO")
                parameter("count", "20")
            }

            val responseBody = response.bodyAsText()
            Log.d("PlaceRepository", "TMAP POI 통합 검색 응답 코드: ${response.status.value}")
            Log.d("PlaceRepository", "TMAP POI 통합 검색 응답: $responseBody")

            if (response.status.value !in 200..299) {
                Log.e("PlaceRepository", "TMAP POI 통합 검색 요청 실패: ${response.status.value}")
                return@withContext emptyList()
            }

            // 응답 파싱
            val poiResponse = response.body<TmapPoiResponse>()
            val pois = poiResponse.searchPoiInfo?.pois?.poi ?: emptyList()

            Log.d("PlaceRepository", "TMAP POI 통합 검색: ${pois.size}개 장소 발견")

            // 장소 목록 변환
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
                    Log.d("PlaceRepository", "장소 발견: $name at ($address)")
                    Place(
                        name = name,
                        address = address,
                        latitude = lat,
                        longitude = lon
                    )
                } else {
                    Log.w("PlaceRepository", "불완전한 장소 정보: name=$name, lat=$lat, lon=$lon")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("PlaceRepository", "TMAP POI Text Search failed", e)
            emptyList()
        }
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
