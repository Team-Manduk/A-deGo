package com.teammanduk.adego.core.data.repository

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place as GooglePlace
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
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
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
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

// Places API (New) REST API Request/Response Models
@Serializable
private data class TextSearchRequest(
    val textQuery: String,
    val languageCode: String = "ko"
)

@Serializable
private data class NearbySearchRequest(
    val includedTypes: List<String>,
    val maxResultCount: Int,
    val locationRestriction: LocationRestriction
)

@Serializable
private data class LocationRestriction(
    val circle: Circle
)

@Serializable
private data class Circle(
    val center: Center,
    val radius: Double
)

@Serializable
private data class Center(
    val latitude: Double,
    val longitude: Double
)

@Serializable
private data class TextSearchResponse(
    val places: List<PlaceResult> = emptyList()
)

@Serializable
private data class NearbySearchResponse(
    val places: List<PlaceResult> = emptyList()
)

@Serializable
private data class PlaceResult(
    val id: String? = null,
    val displayName: DisplayName? = null,
    val formattedAddress: String? = null,
    val location: Location? = null
)

@Serializable
private data class DisplayName(
    val text: String? = null,
    val languageCode: String? = null
)

@Serializable
private data class Location(
    val latitude: Double,
    val longitude: Double
)

@Singleton
class PlaceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PlaceRepository {

    private val placesClient: PlacesClient by lazy {
        Places.createClient(context)
    }

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
            // 1단계: Places API로 주변 장소 검색
            val nearbyPlace = findNearbyPlaceUsingPlacesApi(latitude, longitude)

            if (nearbyPlace != null) {
                _currentSearchResult.emit(nearbyPlace)
                nearbyPlace
            } else {
                // 2단계: Places API 실패 시 Geocoder로 폴백
                val geocodedPlace = searchUsingGeocoder(latitude, longitude)
                _currentSearchResult.emit(geocodedPlace)
                geocodedPlace
            }
        } catch (e: Exception) {
            Log.e("PlaceRepository", "Place search failed", e)
            val fallbackPlace = createFallbackPlace(latitude, longitude)
            _currentSearchResult.emit(fallbackPlace)
            fallbackPlace
        }
    }

    /**
     * Places API를 사용하여 좌표 근처의 장소를 검색합니다.
     */
    private suspend fun findNearbyPlaceUsingPlacesApi(
        latitude: Double,
        longitude: Double
    ): Place? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PlaceRepository", "Places API REST 검색 시작: lat=$latitude, lng=$longitude")

            // API 키 가져오기
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                Log.e("PlaceRepository", "API 키를 찾을 수 없습니다")
                return@withContext null
            }

            // Request Body 생성
            // Places API (New)에서 지원하는 타입만 사용
            // https://developers.google.com/maps/documentation/places/web-service/place-types
            val requestBody = NearbySearchRequest(
                includedTypes = listOf("restaurant", "cafe", "store", "shopping_mall", "lodging"),
                maxResultCount = 10,
                locationRestriction = LocationRestriction(
                    circle = Circle(
                        center = Center(latitude, longitude),
                        radius = 100.0
                    )
                )
            )

            // Ktor를 사용한 API 호출
            val response = httpClient.post("https://places.googleapis.com/v1/places:searchNearby") {
                contentType(ContentType.Application.Json)
                header("X-Goog-Api-Key", apiKey)
                header("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.location")
                header("Accept-Language", "ko")
                setBody(requestBody)
            }

            val responseBody = response.bodyAsText()
            Log.d("PlaceRepository", "Places API 응답 코드: ${response.status.value}")
            Log.d("PlaceRepository", "Places API 응답: $responseBody")

            if (response.status.value !in 200..299) {
                Log.e("PlaceRepository", "Places API 요청 실패: ${response.status.value}")
                return@withContext null
            }

            // 응답 파싱
            val searchResponse = response.body<NearbySearchResponse>()
            Log.d("PlaceRepository", "Places API: ${searchResponse.places.size}개 장소 발견")

            // 모든 장소 정보 로그 출력
            searchResponse.places.forEachIndexed { index, place ->
                Log.d("PlaceRepository", "장소 #$index:")
                Log.d("PlaceRepository", "  - ID: ${place.id}")
                Log.d("PlaceRepository", "  - 이름: ${place.displayName?.text}")
                Log.d("PlaceRepository", "  - 주소: ${place.formattedAddress}")
                Log.d("PlaceRepository", "  - 좌표: ${place.location}")
            }

            // 가장 가까운 장소 찾기
            val nearestPlace = searchResponse.places
                .filter { it.location != null }
                .minByOrNull { place ->
                    val distance = calculateDistance(
                        LatLng(latitude, longitude),
                        LatLng(place.location!!.latitude, place.location.longitude)
                    )
                    Log.d("PlaceRepository", "${place.displayName?.text}까지 거리: ${distance}m")
                    distance
                }

            nearestPlace?.let { placeResult ->
                val placeName = placeResult.displayName?.text ?: "알 수 없는 장소"
                val placeAddress = placeResult.formattedAddress ?: "주소 정보 없음"

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
                    Log.w("PlaceRepository", "Places API에서 가장 가까운 장소를 찾지 못했습니다")
                }
            }
        } catch (e: Exception) {
            Log.e("PlaceRepository", "Places API search failed", e)
            null
        }
    }

    private fun getApiKey(): String {
        return try {
            // BuildConfig에서 API 키 가져오기
            val buildConfigClass = Class.forName("com.teammanduk.adego.BuildConfig")
            val field = buildConfigClass.getDeclaredField("MAPS_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            Log.e("PlaceRepository", "API 키를 가져올 수 없습니다", e)
            ""
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
                                longitude = longitude
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
                        longitude = longitude
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
    private fun calculateDistance(from: LatLng, to: LatLng): Double {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)

        val lat1Rad = Math.toRadians(from.latitude)
        val lat2Rad = Math.toRadians(to.latitude)
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLng = Math.toRadians(to.longitude - from.longitude)

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
            longitude = longitude
        )
    }

    /**
     * Places API Text Search를 사용하여 텍스트 쿼리로 장소를 검색합니다.
     */
    override suspend fun searchPlacesByText(query: String): List<Place> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (query.isBlank()) {
                Log.d("PlaceRepository", "검색 쿼리가 비어있습니다")
                return@withContext emptyList()
            }

            Log.d("PlaceRepository", "Text Search 시작: query=$query")

            // API 키 가져오기
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                Log.e("PlaceRepository", "API 키를 찾을 수 없습니다")
                return@withContext emptyList()
            }

            // Request Body 생성
            val requestBody = TextSearchRequest(
                textQuery = query,
                languageCode = "ko"
            )

            // Ktor를 사용한 API 호출
            val response = httpClient.post("https://places.googleapis.com/v1/places:searchText") {
                contentType(ContentType.Application.Json)
                header("X-Goog-Api-Key", apiKey)
                header("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.location")
                header("Accept-Language", "ko")
                setBody(requestBody)
            }

            val responseBody = response.bodyAsText()
            Log.d("PlaceRepository", "Text Search 응답 코드: ${response.status.value}")
            Log.d("PlaceRepository", "Text Search 응답: $responseBody")

            if (response.status.value !in 200..299) {
                Log.e("PlaceRepository", "Text Search 요청 실패: ${response.status.value}")
                return@withContext emptyList()
            }

            // 응답 파싱
            val searchResponse = response.body<TextSearchResponse>()
            Log.d("PlaceRepository", "Text Search: ${searchResponse.places.size}개 장소 발견")

            // 장소 목록 변환
            searchResponse.places.mapNotNull { placeResult ->
                val name = placeResult.displayName?.text
                val address = placeResult.formattedAddress
                val location = placeResult.location

                if (name != null && address != null && location != null) {
                    Log.d("PlaceRepository", "장소 발견: $name at ($address)")
                    Place(
                        name = name,
                        address = address,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } else {
                    Log.w("PlaceRepository", "불완전한 장소 정보: name=$name, address=$address, location=$location")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("PlaceRepository", "Text Search failed", e)
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
