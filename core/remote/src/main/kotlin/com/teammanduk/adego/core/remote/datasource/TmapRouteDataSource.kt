package com.teammanduk.adego.core.remote.datasource

import android.util.Log
import com.teammanduk.adego.core.data_api.datasource.RouteDataSource
import com.teammanduk.adego.core.data_api.model.GraphicCoordinateDto
import com.teammanduk.adego.core.data_api.model.PedestrianRouteDto
import com.teammanduk.adego.core.data_api.model.RouteLegDto
import com.teammanduk.adego.core.data_api.model.StationDto
import com.teammanduk.adego.core.data_api.model.TransitRouteDto
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import javax.inject.Inject
import javax.inject.Singleton

// TMAP Transit API Response Models
@Serializable
private data class TmapTransitResponse(
    val metaData: TmapTransitMetaData? = null
)

@Serializable
private data class TmapTransitMetaData(
    val requestParameters: TmapRequestParameters? = null,
    val plan: TmapPlan? = null
)

@Serializable
private data class TmapRequestParameters(
    val reqDttm: String? = null,
    val startX: String? = null,
    val startY: String? = null,
    val endX: String? = null,
    val endY: String? = null,
    val locale: String? = null,
    val busCount: Int? = null,
    val expressbusCount: Int? = null,
    val subwayCount: Int? = null,
    val airplaneCount: Int? = null,
    val ferryCount: Int? = null,
    val trainCount: Int? = null
)

@Serializable
private data class TmapPlan(
    val itineraries: List<TmapItinerary> = emptyList()
)

@Serializable
private data class TmapItinerary(
    val fare: TmapFare? = null,
    val totalTime: Int? = null,
    val totalDistance: Double? = null,
    val totalWalkTime: Int? = null,
    val totalWalkDistance: Double? = null,
    val legs: List<TmapLeg> = emptyList(),
    val pathType: Int? = null
)

@Serializable
private data class TmapFare(
    val regular: TmapFareDetail? = null
)

@Serializable
private data class TmapFareDetail(
    val totalFare: Int? = null,
    val currency: TmapCurrency? = null
)

@Serializable
private data class TmapCurrency(
    val symbol: String? = null,
    val currency: String? = null,
    val currencyCode: String? = null
)

@Serializable
private data class TmapLeg(
    val mode: String? = null,
    val sectionTime: Int? = null,
    val distance: Double? = null,
    val start: TmapPlace? = null,
    val end: TmapPlace? = null,
    val steps: List<TmapStep>? = null,
    val route: String? = null,
    val routeColor: String? = null,
    val routeId: String? = null,
    val service: Int? = null,
    val passStopList: TmapPassStopList? = null,
    val passShape: TmapPassShape? = null
)

@Serializable
private data class TmapPlace(
    val name: String? = null,
    val lon: Double? = null,
    val lat: Double? = null
)

@Serializable
private data class TmapStep(
    val streetName: String? = null,
    val distance: Double? = null,
    val description: String? = null,
    val linestring: String? = null
)

@Serializable
private data class TmapPassStopList(
    val stations: List<TmapStation>? = null
)

@Serializable
private data class TmapStation(
    val stationName: String? = null,
    val lon: Double? = null,
    val lat: Double? = null,
    val stationID: String? = null
)

@Serializable
private data class TmapPassShape(
    val linestring: String? = null
)

// TMAP Pedestrian Route API Request Model
@Serializable
private data class TmapPedestrianRequest(
    val startX: Double,
    val startY: Double,
    val endX: Double,
    val endY: Double,
    val reqCoordType: String = "WGS84GEO",
    val resCoordType: String = "WGS84GEO",
    val startName: String = "출발지",
    val endName: String = "도착지"
)

// TMAP Pedestrian Route API Response Models
@Serializable
private data class TmapPedestrianResponse(
    val type: String? = null,
    val features: List<TmapFeature> = emptyList()
)

@Serializable
private data class TmapFeature(
    val type: String? = null,
    val geometry: TmapGeometry? = null,
    val properties: TmapProperties? = null
)

@Serializable
private data class TmapGeometry(
    val type: String? = null,
    val coordinates: JsonElement? = null
)

@Serializable
private data class TmapProperties(
    val totalDistance: Int? = null,
    val totalTime: Int? = null,
    val index: Int? = null,
    val pointIndex: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val direction: String? = null,
    val nearPoiName: String? = null,
    val nearPoiX: String? = null,
    val nearPoiY: String? = null,
    val intersectionName: String? = null,
    val facilityType: String? = null,
    val facilityName: String? = null,
    val turnType: Int? = null,
    val pointType: String? = null,
    val lineIndex: Int? = null,
    val distance: Int? = null,
    val time: Int? = null,
    val roadType: Int? = null,
    val categoryRoadType: Int? = null
)

/**
 * TMAP API를 사용한 경로 검색 DataSource 구현체
 */
@Singleton
class TmapRouteDataSource @Inject constructor(
    private val httpClient: HttpClient
) : RouteDataSource {

    override suspend fun searchTransitRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Result<List<TransitRouteDto>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "TMAP Transit API 경로 검색 시작")
            Log.d(TAG, "출발: ($startLat, $startLng)")
            Log.d(TAG, "도착: ($endLat, $endLng)")

            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.e(TAG, "TMAP API 키를 찾을 수 없습니다")
                return@withContext Result.failure(Exception("TMAP API 키가 설정되지 않았습니다"))
            }

            val requestBody = mapOf(
                "startX" to startLng.toString(),
                "startY" to startLat.toString(),
                "endX" to endLng.toString(),
                "endY" to endLat.toString(),
                "count" to "3",
                "lang" to "0",
                "format" to "json"
            )

            val response = httpClient.post("https://apis.openapi.sk.com/transit/routes") {
                header("accept", "application/json")
                header("appKey", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseBody = response.bodyAsText()
            Log.d(TAG, "TMAP Transit API 응답 코드: ${response.status.value}")

            if (response.status.value !in 200..299) {
                Log.e(TAG, "TMAP Transit API 요청 실패: ${response.status.value}")
                return@withContext Result.failure(Exception("API 요청 실패: ${response.status.value}"))
            }

            val tmapResponse = response.body<TmapTransitResponse>()
            val itineraries = tmapResponse.metaData?.plan?.itineraries ?: emptyList()

            val routes = itineraries.map { itinerary ->
                mapTmapItineraryToDto(itinerary)
            }

            Log.d(TAG, "TMAP Transit API: ${routes.size}개 경로 발견")
            Result.success(routes)
        } catch (e: Exception) {
            Log.e(TAG, "TMAP Transit API 경로 검색 실패", e)
            Result.failure(e)
        }
    }

    override suspend fun searchPedestrianRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        startName: String,
        endName: String
    ): Result<PedestrianRouteDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.w(TAG, "TMAP API 키가 설정되지 않았습니다")
                return@withContext Result.failure(Exception("TMAP API 키가 설정되지 않았습니다"))
            }

            Log.d(TAG, "TMAP 보행자 경로 API 호출 시작")
            Log.d(TAG, "출발: ($startLat, $startLng), 도착: ($endLat, $endLng)")
            Log.d(TAG, "출발지명: $startName, 도착지명: $endName")

            val requestBody = TmapPedestrianRequest(
                startX = startLng,
                startY = startLat,
                endX = endLng,
                endY = endLat,
                startName = startName,
                endName = endName
            )

            val response = httpClient.post("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1") {
                header("appKey", apiKey)
                header("Accept", "application/json")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.value !in 200..299) {
                Log.e(TAG, "TMAP API 요청 실패: ${response.status.value}")
                return@withContext Result.failure(Exception("API 요청 실패: ${response.status.value}"))
            }

            val responseBody = response.bodyAsText()
            Log.d(TAG, "TMAP API 응답 수신 (길이: ${responseBody.length})")

            val tmapResponse = response.body<TmapPedestrianResponse>()
            val coordinates = mutableListOf<GraphicCoordinateDto>()

            Log.d(TAG, "TMAP features 개수: ${tmapResponse.features.size}")

            tmapResponse.features.forEachIndexed { featureIndex, feature ->
                if (feature.geometry?.type == "LineString") {
                    val coordsArray = feature.geometry.coordinates

                    if (coordsArray is JsonArray) {
                        Log.d(TAG, "LineString coordinates 개수: ${coordsArray.size}")

                        coordsArray.forEachIndexed { coordIndex, coord ->
                            if (coord is JsonArray && coord.size >= 2) {
                                try {
                                    val lngPrimitive = coord[0] as? JsonPrimitive
                                    val latPrimitive = coord[1] as? JsonPrimitive

                                    val lng = lngPrimitive?.doubleOrNull
                                    val lat = latPrimitive?.doubleOrNull

                                    if (lng != null && lat != null) {
                                        coordinates.add(
                                            GraphicCoordinateDto(
                                                latitude = lat,
                                                longitude = lng
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "좌표[$coordIndex] 변환 실패", e)
                                }
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "TMAP API: ${coordinates.size}개 보행자 경로 좌표 획득")
            Result.success(PedestrianRouteDto(coordinates))
        } catch (e: Exception) {
            Log.e(TAG, "TMAP API 호출 실패", e)
            Result.failure(e)
        }
    }

    /**
     * TMAP Itinerary를 TransitRouteDto로 변환
     */
    private fun mapTmapItineraryToDto(itinerary: TmapItinerary): TransitRouteDto {
        var transferCount = 0
        var lastMode: String? = null

        // 환승 횟수 계산
        itinerary.legs.forEach { leg ->
            val currentMode = leg.mode
            if (currentMode != "WALK" && lastMode != null && lastMode != "WALK" && lastMode != currentMode) {
                transferCount++
            }
            if (currentMode != "WALK") {
                lastMode = currentMode
            }
        }

        // pathType 결정
        val hasSubway = itinerary.legs.any { it.mode == "SUBWAY" }
        val hasBus = itinerary.legs.any { it.mode == "BUS" }
        val pathType = when {
            hasSubway && hasBus -> 3
            hasSubway -> 1
            hasBus -> 2
            else -> 3
        }

        val legs = itinerary.legs.map { leg ->
            mapTmapLegToDto(leg)
        }

        return TransitRouteDto(
            totalTime = (itinerary.totalTime ?: 0) / 60, // 초 -> 분
            totalDistance = (itinerary.totalDistance ?: 0.0).toInt(),
            totalFare = itinerary.fare?.regular?.totalFare ?: 0,
            transferCount = transferCount,
            pathType = pathType,
            legs = legs
        )
    }

    /**
     * TMAP Leg을 RouteLegDto로 변환
     */
    private fun mapTmapLegToDto(leg: TmapLeg): RouteLegDto {
        // graphicData 추출
        val graphicData = leg.passShape?.linestring?.let { linestring ->
            parseWktLineString(linestring)
        } ?: leg.steps?.flatMap { step ->
            step.linestring?.let { parseWktLineString(it) } ?: emptyList()
        }

        return RouteLegDto(
            mode = leg.mode ?: "WALK",
            sectionTime = (leg.sectionTime ?: 0) / 60, // 초 -> 분
            distance = leg.distance ?: 0.0,
            startName = leg.start?.name,
            endName = leg.end?.name,
            startLatitude = leg.start?.lat,
            startLongitude = leg.start?.lon,
            endLatitude = leg.end?.lat,
            endLongitude = leg.end?.lon,
            stationCount = leg.passStopList?.stations?.size,
            route = leg.route,
            routeId = leg.routeId,
            routeType = leg.service,
            passStations = leg.passStopList?.stations?.mapNotNull { station ->
                if (station.stationName != null && station.lat != null && station.lon != null) {
                    StationDto(
                        name = station.stationName,
                        latitude = station.lat,
                        longitude = station.lon
                    )
                } else null
            },
            graphicCoordinates = graphicData
        )
    }

    /**
     * WKT LINESTRING 형식을 GraphicCoordinateDto 리스트로 파싱
     */
    private fun parseWktLineString(wkt: String): List<GraphicCoordinateDto> {
        return try {
            val coordString = wkt
                .removePrefix("LINESTRING(")
                .removePrefix("LINESTRING (")
                .removeSuffix(")")
                .trim()

            coordString.split(" ").mapNotNull { pair ->
                val parts = pair.trim().split(",")
                if (parts.size >= 2) {
                    val lon = parts[0].toDoubleOrNull()
                    val lat = parts[1].toDoubleOrNull()
                    if (lon != null && lat != null) {
                        GraphicCoordinateDto(
                            latitude = lat,
                            longitude = lon
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "WKT LINESTRING 파싱 실패: $wkt", e)
            emptyList()
        }
    }

    /**
     * BuildConfig에서 TMAP API 키를 가져옵니다.
     */
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
        private const val TAG = "TmapRouteDataSource"
    }
}
