package com.teammanduk.adego.core.data.repository

import android.util.Log
import com.teammanduk.adego.core.data.model.TmapReverseGeocodingResponse
import com.teammanduk.adego.core.domain.repository.RouteRepository
import com.teammanduk.adego.core.model.Lane
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.SubPath
import com.teammanduk.adego.core.model.TrafficType
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
import kotlinx.serialization.json.doubleOrNull
import javax.inject.Inject
import javax.inject.Singleton

// TMAP Transit API Response Models
// 최상위 응답 구조: { metaData: { requestParameters: {...}, plan: { itineraries: [...] } } }
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
    val reqDttm: String? = null, // 요청 일시
    val startX: String? = null, // 출발지 경도
    val startY: String? = null, // 출발지 위도
    val endX: String? = null, // 도착지 경도
    val endY: String? = null, // 도착지 위도
    val locale: String? = null, // 언어 설정
    val busCount: Int? = null, // 버스 경로 개수
    val expressbusCount: Int? = null, // 고속버스 경로 개수
    val subwayCount: Int? = null, // 지하철 경로 개수
    val airplaneCount: Int? = null, // 비행기 경로 개수
    val ferryCount: Int? = null, // 배 경로 개수
    val trainCount: Int? = null // 기차 경로 개수
)

@Serializable
private data class TmapPlan(
    val itineraries: List<TmapItinerary> = emptyList()
)

@Serializable
private data class TmapItinerary(
    val fare: TmapFare? = null,
    val totalTime: Int? = null, // 총 소요 시간 (초)
    val totalDistance: Double? = null, // 총 거리 (미터)
    val totalWalkTime: Int? = null, // 총 도보 시간 (초)
    val totalWalkDistance: Double? = null, // 총 도보 거리 (미터)
    val legs: List<TmapLeg> = emptyList(),
    val pathType: Int? = null // 경로 타입
)

@Serializable
private data class TmapFare(
    val regular: TmapFareDetail? = null
)

@Serializable
private data class TmapFareDetail(
    val totalFare: Int? = null, // 총 요금
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
    val mode: String? = null, // "WALK", "BUS", "SUBWAY", etc.
    val sectionTime: Int? = null, // 구간 시간 (초)
    val distance: Double? = null, // 거리 (미터)
    val start: TmapPlace? = null,
    val end: TmapPlace? = null,
    val steps: List<TmapStep>? = null, // 도보 구간의 경우
    val route: String? = null, // 노선명 (대중교통)
    val routeColor: String? = null, // 노선 색상
    val routeId: String? = null, // 노선 ID
    val service: Int? = null, // 노선 타입
    val passStopList: TmapPassStopList? = null, // 경유 정류장 목록
    val passShape: TmapPassShape? = null // 경로 좌표
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
    val linestring: String? = null // 경로 좌표 (WKT 형식)
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
    val linestring: String? = null // WKT LINESTRING 형식
)

// TMAP Pedestrian Route API Request Model
@Serializable
private data class TmapPedestrianRequest(
    val startX: Double, // 출발지 경도
    val startY: Double, // 출발지 위도
    val endX: Double,   // 도착지 경도
    val endY: Double,   // 도착지 위도
    val reqCoordType: String = "WGS84GEO",
    val resCoordType: String = "WGS84GEO",
    val startName: String = "출발지",
    val endName: String = "도착지"
)

// TMAP Pedestrian Route API Response Models
@Serializable
private data class TmapPedestrianResponse(
    val type: String? = null, // "FeatureCollection"
    val features: List<TmapFeature> = emptyList()
)

@Serializable
private data class TmapFeature(
    val type: String? = null, // "Feature"
    val geometry: TmapGeometry? = null,
    val properties: TmapProperties? = null
)

@Serializable
private data class TmapGeometry(
    val type: String? = null, // "Point" or "LineString"
    val coordinates: kotlinx.serialization.json.JsonElement? = null // Can be array of numbers or array of arrays
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

@Singleton
class RouteRepositoryImpl @Inject constructor() : RouteRepository {

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
                    Log.d("TmapClient", message)
                }
            }
            level = LogLevel.ALL
        }
    }

    override suspend fun searchRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<Route> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("RouteRepository", "TMAP Transit API 경로 검색 시작")
            Log.d("RouteRepository", "출발: ($startLat, $startLng)")
            Log.d("RouteRepository", "도착: ($endLat, $endLng)")

            // API 키 가져오기
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.e("RouteRepository", "TMAP API 키를 찾을 수 없습니다")
                return@withContext emptyList()
            }

            // API 호출 (TMAP Transit API)
            // 모든 값을 String으로 통일 (Ktor serialization을 위해)
            val requestBody = mapOf(
                "startX" to startLng.toString(),
                "startY" to startLat.toString(),
                "endX" to endLng.toString(),
                "endY" to endLat.toString(),
                "count" to "3", // 경로 개수
                "lang" to "0", // 0: 한국어
                "format" to "json"
            )

            val response = httpClient.post("https://apis.openapi.sk.com/transit/routes") {
                header("accept", "application/json")
                header("appKey", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseBody = response.bodyAsText()
            Log.d("RouteRepository", "TMAP Transit API 응답 코드: ${response.status.value}")
            Log.d("RouteRepository", "TMAP Transit API 응답: $responseBody")

            if (response.status.value !in 200..299) {
                Log.e("RouteRepository", "TMAP Transit API 요청 실패: ${response.status.value}")
                return@withContext emptyList()
            }

            // 응답 파싱
            val tmapResponse = response.body<TmapTransitResponse>()

            // metaData.plan.itineraries에서 경로 추출
            val itineraries = tmapResponse.metaData?.plan?.itineraries ?: emptyList()

            // 경로 변환
            val routes = itineraries.map { itinerary ->
                mapTmapItineraryToRoute(itinerary, startLat, startLng, endLat, endLng)
            }

            Log.d("RouteRepository", "TMAP Transit API: ${routes.size}개 경로 발견")
            routes
        } catch (e: Exception) {
            Log.e("RouteRepository", "TMAP Transit API 경로 검색 실패", e)
            emptyList()
        }
    }

    override suspend fun getRouteDetails(
        route: Route,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Route = withContext(Dispatchers.IO) {
        Log.d("RouteRepository", "경로 세부 정보 로드 시작")

        // TMAP Transit API는 이미 graphicData를 포함하고 있으므로
        // 추가 로딩이 필요한 경우에만 처리
        val updatedSubPaths = route.subPaths.mapIndexed { index, subPath ->
            // 이미 graphicData가 있으면 그대로 반환
            val existingGraphicData = subPath.graphicData
            if (existingGraphicData != null && existingGraphicData.isNotEmpty()) {
                Log.d("RouteRepository", "SubPath[$index] - 이미 graphicData 존재")
                return@mapIndexed subPath
            }

            // graphicData가 없는 도보 구간만 추가 로딩
            if (subPath.trafficType == TrafficType.WALK) {
                Log.d("RouteRepository", "도보 구간 graphicData 없음 - TMAP Pedestrian API로 로드")

                val walkStartLat = subPath.startLatitude
                val walkStartLng = subPath.startLongitude
                val walkEndLat = subPath.endLatitude
                val walkEndLng = subPath.endLongitude

                if (walkStartLat != null && walkStartLng != null &&
                    walkEndLat != null && walkEndLng != null) {
                    val graphicData = getPedestrianRoute(
                        startLat = walkStartLat,
                        startLng = walkStartLng,
                        endLat = walkEndLat,
                        endLng = walkEndLng,
                        startName = subPath.startName ?: "출발",
                        endName = subPath.endName ?: "도착"
                    )
                    subPath.copy(graphicData = graphicData)
                } else {
                    Log.w("RouteRepository", "도보 좌표 정보 없음")
                    subPath
                }
            } else {
                // 대중교통 구간은 그대로 반환
                subPath
            }
        }

        Log.d("RouteRepository", "경로 세부 정보 로드 완료")
        route.copy(subPaths = updatedSubPaths)
    }

    /**
     * TMAP Transit API 응답을 도메인 모델로 변환합니다.
     */
    private fun mapTmapItineraryToRoute(
        itinerary: TmapItinerary,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Route {
        var transferCount = 0
        var lastMode: String? = null

        // 환승 횟수 계산: mode가 변경될 때마다 환승으로 간주 (WALK 제외)
        itinerary.legs.forEach { leg ->
            val currentMode = leg.mode
            if (currentMode != "WALK" && lastMode != null && lastMode != "WALK" && lastMode != currentMode) {
                transferCount++
            }
            if (currentMode != "WALK") {
                lastMode = currentMode
            }
        }

        // pathType 결정 (1: 지하철만, 2: 버스만, 3: 복합)
        val hasSubway = itinerary.legs.any { it.mode == "SUBWAY" }
        val hasBus = itinerary.legs.any { it.mode == "BUS" }
        val pathType = when {
            hasSubway && hasBus -> 3 // 지하철+버스
            hasSubway -> 1 // 지하철만
            hasBus -> 2 // 버스만
            else -> 3 // 기타
        }

        val subPaths = itinerary.legs.map { leg ->
            mapTmapLegToSubPath(leg)
        }

        return Route(
            totalTime = itinerary.totalTime ?: 0, // 초
            totalDistance = (itinerary.totalDistance ?: 0.0).toInt(),
            totalFare = itinerary.fare?.regular?.totalFare ?: 0,
            transferCount = transferCount,
            pathType = pathType,
            subPaths = subPaths,
            startLatitude = startLat,
            startLongitude = startLng,
            endLatitude = endLat,
            endLongitude = endLng
        )
    }

    /**
     * TMAP Leg을 SubPath로 변환합니다.
     */
    private fun mapTmapLegToSubPath(leg: TmapLeg): SubPath {
        val trafficType = when (leg.mode) {
            "SUBWAY" -> TrafficType.SUBWAY
            "BUS" -> TrafficType.BUS
            else -> TrafficType.WALK
        }

        // passShape에서 graphicData 추출
        val graphicData = leg.passShape?.linestring?.let { linestring ->
            parseWktLineString(linestring)
        } ?: leg.steps?.flatMap { step ->
            step.linestring?.let { parseWktLineString(it) } ?: emptyList()
        }

        return SubPath(
            trafficType = trafficType,
            distance = leg.distance ?: 0.0,
            sectionTime = leg.sectionTime ?: 0, // 초
            startName = leg.start?.name,
            endName = leg.end?.name,
            stationCount = leg.passStopList?.stations?.size,
            lane = if (trafficType != TrafficType.WALK) {
                Lane(
                    name = leg.route ?: "",
                    busNo = if (trafficType == TrafficType.BUS) leg.route else null,
                    type = leg.service,
                    subwayCode = if (trafficType == TrafficType.SUBWAY) leg.routeId?.toIntOrNull() else null
                )
            } else null,
            walkDistance = if (trafficType == TrafficType.WALK) leg.distance else null,
            startLatitude = leg.start?.lat,
            startLongitude = leg.start?.lon,
            endLatitude = leg.end?.lat,
            endLongitude = leg.end?.lon,
            passStations = leg.passStopList?.stations?.mapNotNull { station ->
                if (station.stationName != null && station.lat != null && station.lon != null) {
                    com.teammanduk.adego.core.model.Station(
                        name = station.stationName,
                        latitude = station.lat,
                        longitude = station.lon
                    )
                } else null
            },
            graphicData = graphicData
        )
    }

    /**
     * WKT LINESTRING 형식을 GraphicCoordinate 리스트로 파싱합니다.
     * 예: "LINESTRING(127.123 37.456, 127.124 37.457)" 또는 "129.08409,35.23032 129.0841,35.230377"
     */
    private fun parseWktLineString(wkt: String): List<com.teammanduk.adego.core.model.GraphicCoordinate> {
        return try {
            // "LINESTRING(" 제거 및 ")" 제거
            val coordString = wkt
                .removePrefix("LINESTRING(")
                .removePrefix("LINESTRING (")
                .removeSuffix(")")
                .trim()

            // 좌표 쌍으로 분할
            // API 응답 형식: "경도,위도 경도,위도" (공백으로 좌표 쌍 구분, 쉼표로 경도/위도 구분)
            // 예: "129.08409,35.23032 129.0841,35.230377"
            coordString.split(" ").mapNotNull { pair ->
                val parts = pair.trim().split(",")
                if (parts.size >= 2) {
                    val lon = parts[0].toDoubleOrNull()  // 첫 번째는 경도
                    val lat = parts[1].toDoubleOrNull()  // 두 번째는 위도
                    if (lon != null && lat != null) {
                        com.teammanduk.adego.core.model.GraphicCoordinate(
                            latitude = lat,   // 위도는 lat 변수
                            longitude = lon   // 경도는 lon 변수
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e("RouteRepository", "WKT LINESTRING 파싱 실패: $wkt", e)
            emptyList()
        }
    }

    /**
     * TMAP 역지오코딩 API를 호출하여 좌표로부터 장소 이름을 가져옵니다.
     */
    private suspend fun reverseGeocode(lat: Double, lng: Double): String {
        return try {
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                return "위치"
            }

            Log.d("RouteRepository", "TMAP 역지오코딩 API 호출: ($lat, $lng)")

            val response = httpClient.get("https://apis.openapi.sk.com/tmap/geo/reversegeocoding") {
                header("appKey", apiKey)
                parameter("version", "1")
                parameter("lat", lat)
                parameter("lon", lng)
                parameter("coordType", "WGS84GEO")
                parameter("addressType", "A02") // A02: 도로명 주소
            }

            if (response.status.value !in 200..299) {
                Log.e("RouteRepository", "TMAP 역지오코딩 API 요청 실패: ${response.status.value}")
                return "위치"
            }

            val tmapResponse = response.body<TmapReverseGeocodingResponse>()
            val addressInfo = tmapResponse.addressInfo

            // 건물명이 있으면 건물명 사용, 없으면 도로명 + 건물번호, 그것도 없으면 fullAddress 사용
            val locationName = when {
                !addressInfo?.buildingName.isNullOrEmpty() -> addressInfo?.buildingName!!
                !addressInfo?.roadName.isNullOrEmpty() && !addressInfo?.buildingIndex.isNullOrEmpty() ->
                    "${addressInfo?.roadName} ${addressInfo?.buildingIndex}"
                !addressInfo?.fullAddress.isNullOrEmpty() -> addressInfo?.fullAddress!!
                else -> "위치"
            }

            Log.d("RouteRepository", "역지오코딩 결과: $locationName")
            locationName
        } catch (e: Exception) {
            Log.e("RouteRepository", "TMAP 역지오코딩 API 호출 실패", e)
            "위치"
        }
    }

    /**
     * TMAP API를 호출하여 보행자 경로 데이터를 가져옵니다.
     */
    private suspend fun getPedestrianRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        startName: String = "출발지",
        endName: String = "도착지"
    ): List<com.teammanduk.adego.core.model.GraphicCoordinate> {
        return try {
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.w("RouteRepository", "TMAP API 키가 설정되지 않았습니다")
                return emptyList()
            }

            Log.d("RouteRepository", "TMAP 보행자 경로 API 호출 시작")
            Log.d("RouteRepository", "출발: ($startLat, $startLng), 도착: ($endLat, $endLng)")
            Log.d("RouteRepository", "출발지명: $startName, 도착지명: $endName")

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
                Log.e("RouteRepository", "TMAP API 요청 실패: ${response.status.value}")
                return emptyList()
            }

            val responseBody = response.bodyAsText()
            Log.d("RouteRepository", "TMAP API 응답 수신 (길이: ${responseBody.length})")

            val tmapResponse = response.body<TmapPedestrianResponse>()

            // LineString geometry만 추출하여 좌표 리스트 생성
            val coordinates = mutableListOf<com.teammanduk.adego.core.model.GraphicCoordinate>()

            Log.d("RouteRepository", "TMAP features 개수: ${tmapResponse.features.size}")

            tmapResponse.features.forEachIndexed { featureIndex, feature ->
                Log.d("RouteRepository", "Feature[$featureIndex] type: ${feature.geometry?.type}")

                if (feature.geometry?.type == "LineString") {
                    val coordsArray = feature.geometry.coordinates
                    Log.d("RouteRepository", "LineString coordinates type: ${coordsArray?.javaClass?.simpleName}")

                    if (coordsArray is kotlinx.serialization.json.JsonArray) {
                        Log.d("RouteRepository", "LineString coordinates 개수: ${coordsArray.size}")

                        coordsArray.forEachIndexed { coordIndex, coord ->
                            if (coord is kotlinx.serialization.json.JsonArray && coord.size >= 2) {
                                try {
                                    // JsonPrimitive에서 double 값 추출
                                    val lngPrimitive = coord[0] as? kotlinx.serialization.json.JsonPrimitive
                                    val latPrimitive = coord[1] as? kotlinx.serialization.json.JsonPrimitive

                                    val lng = lngPrimitive?.doubleOrNull
                                    val lat = latPrimitive?.doubleOrNull

                                    if (lng != null && lat != null) {
                                        coordinates.add(
                                            com.teammanduk.adego.core.model.GraphicCoordinate(
                                                latitude = lat,
                                                longitude = lng
                                            )
                                        )
                                    } else {
                                        Log.w("RouteRepository", "좌표[$coordIndex] 파싱 실패: lng=$lng, lat=$lat")
                                    }
                                } catch (e: Exception) {
                                    Log.e("RouteRepository", "좌표[$coordIndex] 변환 실패", e)
                                }
                            }
                        }
                    }
                }
            }

            Log.d("RouteRepository", "TMAP API: ${coordinates.size}개 보행자 경로 좌표 획득")
            coordinates
        } catch (e: Exception) {
            Log.e("RouteRepository", "TMAP API 호출 실패", e)
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
            Log.e("RouteRepository", "TMAP API 키를 가져올 수 없습니다", e)
            ""
        }
    }
}
