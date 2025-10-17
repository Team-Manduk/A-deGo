package com.teammanduk.adego.core.remote.datasource

import android.util.Log
import com.teammanduk.adego.core.data_api.datasource.PoiSearchDataSource
import com.teammanduk.adego.core.data_api.model.PlaceDto
import com.teammanduk.adego.core.domain.service.DistanceCalculator
import com.teammanduk.adego.core.remote.model.TmapPoiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TMAP POI 검색 DataSource
 * TMAP API를 통해 POI (Point of Interest)를 검색합니다.
 */
@Singleton
class TmapPoiDataSource @Inject constructor(
    private val httpClient: HttpClient,
    private val distanceCalculator: DistanceCalculator
) : PoiSearchDataSource {

    override suspend fun searchNearby(
        latitude: Double,
        longitude: Double,
        radiusInMeters: Int
    ): PlaceDto? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "TMAP POI 주변 검색 시작: lat=$latitude, lng=$longitude")

            // API 키 가져오기
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.e(TAG, "TMAP API 키를 찾을 수 없습니다")
                return@withContext null
            }

            // TMAP POI 주변 검색 API 호출
            val response = httpClient.get("https://apis.openapi.sk.com/tmap/pois/search/around") {
                header("appKey", apiKey)
                parameter("version", "1")
                parameter("centerLon", longitude.toString())
                parameter("centerLat", latitude.toString())
                parameter("radius", radiusInMeters.toString())
                parameter("searchtypCd", "A") // 전체 검색
                parameter("reqCoordType", "WGS84GEO")
                parameter("resCoordType", "WGS84GEO")
                parameter("count", "10")
            }

            val responseBody = response.bodyAsText()
            Log.d(TAG, "TMAP POI API 응답 코드: ${response.status.value}")
            Log.d(TAG, "TMAP POI API 응답: $responseBody")

            if (response.status.value !in 200..299) {
                Log.e(TAG, "TMAP POI API 요청 실패: ${response.status.value}")
                return@withContext null
            }

            // 응답 파싱
            val poiResponse = response.body<TmapPoiResponse>()
            val pois = poiResponse.searchPoiInfo?.pois?.poi ?: emptyList()

            Log.d(TAG, "TMAP POI API: ${pois.size}개 장소 발견")

            // 모든 장소 정보 로그 출력
            pois.forEachIndexed { index, poi ->
                Log.d(TAG, "장소 #$index:")
                Log.d(TAG, "  - 이름: ${poi.name}")
                Log.d(TAG, "  - 주소: ${poi.upperAddrName} ${poi.middleAddrName} ${poi.lowerAddrName}")
                Log.d(TAG, "  - 좌표: (${poi.frontLat}, ${poi.frontLon})")
            }

            // 가장 가까운 장소 찾기
            val nearestPoi = pois
                .filter { it.frontLat != null && it.frontLon != null }
                .minByOrNull { poi ->
                    val poiLat = poi.frontLat!!.toDoubleOrNull() ?: return@minByOrNull Double.MAX_VALUE
                    val poiLon = poi.frontLon!!.toDoubleOrNull() ?: return@minByOrNull Double.MAX_VALUE
                    val distance = distanceCalculator.calculateDistanceInMeters(
                        latitude, longitude, poiLat, poiLon
                    )
                    Log.d(TAG, "${poi.name}까지 거리: ${distance}m")
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

                Log.d(TAG, "선택된 장소: $placeName")
                Log.d(TAG, "주소: $placeAddress")

                PlaceDto(
                    name = placeName,
                    address = placeAddress,
                    latitude = latitude,
                    longitude = longitude,
                    isPOI = true
                )
            }.also {
                if (it == null) {
                    Log.w(TAG, "TMAP POI API에서 가장 가까운 장소를 찾지 못했습니다")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TMAP POI search failed", e)
            null
        }
    }

    override suspend fun searchByText(query: String): List<PlaceDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (query.isBlank()) {
                Log.d(TAG, "검색 쿼리가 비어있습니다")
                return@withContext emptyList()
            }

            Log.d(TAG, "TMAP POI 통합 검색 시작: query=$query")

            // API 키 가져오기
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.e(TAG, "TMAP API 키를 찾을 수 없습니다")
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
            Log.d(TAG, "TMAP POI 통합 검색 응답 코드: ${response.status.value}")
            Log.d(TAG, "TMAP POI 통합 검색 응답: $responseBody")

            if (response.status.value !in 200..299) {
                Log.e(TAG, "TMAP POI 통합 검색 요청 실패: ${response.status.value}")
                return@withContext emptyList()
            }

            // 응답 파싱
            val poiResponse = response.body<TmapPoiResponse>()
            val pois = poiResponse.searchPoiInfo?.pois?.poi ?: emptyList()

            Log.d(TAG, "TMAP POI 통합 검색: ${pois.size}개 장소 발견")

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
                    Log.d(TAG, "장소 발견: $name at ($address)")
                    PlaceDto(
                        name = name,
                        address = address,
                        latitude = lat,
                        longitude = lon,
                        isPOI = true
                    )
                } else {
                    Log.w(TAG, "불완전한 장소 정보: name=$name, lat=$lat, lon=$lon")
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

    companion object {
        private const val TAG = "TmapPoiDataSource"
    }
}
