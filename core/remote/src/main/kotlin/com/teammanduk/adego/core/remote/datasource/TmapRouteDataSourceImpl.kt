package com.teammanduk.adego.core.remote.datasource

import android.util.Log
import com.teammanduk.adego.core.data_api.datasource.RouteDataSource
import com.teammanduk.adego.core.data_api.model.GraphicCoordinateDto
import com.teammanduk.adego.core.data_api.model.LaneDto
import com.teammanduk.adego.core.data_api.model.RouteDto
import com.teammanduk.adego.core.data_api.model.StationDto
import com.teammanduk.adego.core.data_api.model.SubPathDto
import com.teammanduk.adego.core.data_api.model.TrafficTypeDto
import com.teammanduk.adego.core.remote.model.TmapReverseGeocodingResponse
import com.teammanduk.adego.core.remote.model.TmapTransitResponse
import com.teammanduk.adego.core.remote.model.TmapItinerary
import com.teammanduk.adego.core.remote.model.TmapLeg
import com.teammanduk.adego.core.remote.model.TmapPlace
import com.teammanduk.adego.core.remote.model.TmapStep
import com.teammanduk.adego.core.remote.model.TmapPassStopList
import com.teammanduk.adego.core.remote.model.TmapStation
import com.teammanduk.adego.core.remote.model.TmapPassShape
import com.teammanduk.adego.core.remote.model.TmapPedestrianRequest
import com.teammanduk.adego.core.remote.model.TmapPedestrianResponse
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmapRouteDataSourceImpl @Inject constructor() : RouteDataSource {

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

    override suspend fun searchRoutes(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<RouteDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("TmapRouteDataSource", "TMAP Transit API 경로 검색 시작")
            Log.d("TmapRouteDataSource", "출발: ($startLat, $startLng)")
            Log.d("TmapRouteDataSource", "도착: ($endLat, $endLng)")

            // API 키 가져오기
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.e("TmapRouteDataSource", "TMAP API 키를 찾을 수 없습니다")
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
            Log.d("TmapRouteDataSource", "TMAP Transit API 응답 코드: ${response.status.value}")
            Log.d("TmapRouteDataSource", "TMAP Transit API 응답: $responseBody")

            if (response.status.value !in 200..299) {
                Log.e("TmapRouteDataSource", "TMAP Transit API 요청 실패: ${response.status.value}")
                return@withContext emptyList()
            }

            // 응답 파싱
            val tmapResponse = response.body<TmapTransitResponse>()

            // metaData.plan.itineraries에서 경로 추출
            val itineraries = tmapResponse.metaData?.plan?.itineraries ?: emptyList()

            // 경로 변환
            val routes = itineraries.map { itinerary ->
                mapTmapItineraryToRouteDto(itinerary, startLat, startLng, endLat, endLng)
            }

            Log.d("TmapRouteDataSource", "TMAP Transit API: ${routes.size}개 경로 발견")
            routes
        } catch (e: Exception) {
            Log.e("TmapRouteDataSource", "TMAP Transit API 경로 검색 실패", e)
            emptyList()
        }
    }

    override suspend fun getRouteDetails(
        route: RouteDto,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): RouteDto = withContext(Dispatchers.IO) {
        Log.d("TmapRouteDataSource", "경로 세부 정보 로드 시작")

        // TMAP Transit API는 이미 graphicData를 포함하고 있으므로
        // 추가 로딩이 필요한 경우에만 처리
        val updatedSubPaths = route.subPaths.mapIndexed { index, subPath ->
            // 이미 graphicData가 있으면 그대로 반환
            val existingGraphicData = subPath.graphicData
            if (existingGraphicData != null && existingGraphicData.isNotEmpty()) {
                Log.d("TmapRouteDataSource", "SubPath[$index] - 이미 graphicData 존재")
                return@mapIndexed subPath
            }

            // graphicData가 없는 도보 구간만 추가 로딩
            if (subPath.trafficType == TrafficTypeDto.WALK) {
                Log.d("TmapRouteDataSource", "도보 구간 graphicData 없음 - TMAP Pedestrian API로 로드")

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
                    Log.w("TmapRouteDataSource", "도보 좌표 정보 없음")
                    subPath
                }
            } else {
                // 대중교통 구간은 그대로 반환
                subPath
            }
        }

        Log.d("TmapRouteDataSource", "경로 세부 정보 로드 완료")
        route.copy(subPaths = updatedSubPaths)
    }

    /**
     * TMAP Transit API 응답을 RouteDto로 변환합니다.
     */
    private fun mapTmapItineraryToRouteDto(
        itinerary: TmapItinerary,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): RouteDto {
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
            mapTmapLegToSubPathDto(leg)
        }

        return RouteDto(
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
     * TMAP Leg을 SubPathDto로 변환합니다.
     */
    private fun mapTmapLegToSubPathDto(leg: TmapLeg): SubPathDto {
        val trafficType = when (leg.mode) {
            "SUBWAY" -> TrafficTypeDto.SUBWAY
            "BUS" -> TrafficTypeDto.BUS
            else -> TrafficTypeDto.WALK
        }

        // passShape에서 graphicData 추출
        val graphicData = leg.passShape?.linestring?.let { linestring ->
            parseWktLineString(linestring)
        } ?: leg.steps?.flatMap { step ->
            step.linestring?.let { parseWktLineString(it) } ?: emptyList()
        }

        return SubPathDto(
            trafficType = trafficType,
            distance = leg.distance ?: 0.0,
            sectionTime = leg.sectionTime ?: 0, // 초
            startName = leg.start?.name,
            endName = leg.end?.name,
            stationCount = leg.passStopList?.stations?.size,
            lane = if (trafficType != TrafficTypeDto.WALK) {
                LaneDto(
                    name = leg.route ?: "",
                    busNo = if (trafficType == TrafficTypeDto.BUS) leg.route else null,
                    type = leg.service,
                    subwayCode = if (trafficType == TrafficTypeDto.SUBWAY) leg.routeId?.toIntOrNull() else null
                )
            } else null,
            walkDistance = if (trafficType == TrafficTypeDto.WALK) leg.distance else null,
            startLatitude = leg.start?.lat,
            startLongitude = leg.start?.lon,
            endLatitude = leg.end?.lat,
            endLongitude = leg.end?.lon,
            passStations = leg.passStopList?.stations?.mapNotNull { station ->
                val lat = station.lat?.toDoubleOrNull()
                val lon = station.lon?.toDoubleOrNull()
                if (station.stationName != null && lat != null && lon != null) {
                    StationDto(
                        name = station.stationName,
                        latitude = lat,
                        longitude = lon
                    )
                } else null
            },
            graphicData = graphicData
        )
    }

    /**
     * WKT LINESTRING 형식을 GraphicCoordinateDto 리스트로 파싱합니다.
     * 예: "LINESTRING(127.123 37.456, 127.124 37.457)" 또는 "129.08409,35.23032 129.0841,35.230377"
     */
    private fun parseWktLineString(wkt: String): List<GraphicCoordinateDto> {
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
            val coordinates = coordString.split(" ").mapNotNull { pair ->
                val parts = pair.trim().split(",")
                if (parts.size >= 2) {
                    val lon = parts[0].toDoubleOrNull()  // 첫 번째는 경도
                    val lat = parts[1].toDoubleOrNull()  // 두 번째는 위도
                    if (lon != null && lat != null) {
                        GraphicCoordinateDto(
                            latitude = lat,   // 위도는 lat 변수
                            longitude = lon   // 경도는 lon 변수
                        )
                    } else null
                } else null
            }

            coordinates
        } catch (e: Exception) {
            Log.e("TmapRouteDataSource", "WKT LINESTRING 파싱 실패: $wkt", e)
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

            Log.d("TmapRouteDataSource", "TMAP 역지오코딩 API 호출: ($lat, $lng)")

            val response = httpClient.get("https://apis.openapi.sk.com/tmap/geo/reversegeocoding") {
                header("appKey", apiKey)
                parameter("version", "1")
                parameter("lat", lat)
                parameter("lon", lng)
                parameter("coordType", "WGS84GEO")
                parameter("addressType", "A02") // A02: 도로명 주소
            }

            if (response.status.value !in 200..299) {
                Log.e("TmapRouteDataSource", "TMAP 역지오코딩 API 요청 실패: ${response.status.value}")
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

            Log.d("TmapRouteDataSource", "역지오코딩 결과: $locationName")
            locationName
        } catch (e: Exception) {
            Log.e("TmapRouteDataSource", "TMAP 역지오코딩 API 호출 실패", e)
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
    ): List<GraphicCoordinateDto> {
        return try {
            val apiKey = getTmapApiKey()
            if (apiKey.isEmpty() || apiKey == "YOUR_TMAP_API_KEY_HERE") {
                Log.w("TmapRouteDataSource", "TMAP API 키가 설정되지 않았습니다")
                return emptyList()
            }

            Log.d("TmapRouteDataSource", "TMAP 보행자 경로 API 호출 시작")
            Log.d("TmapRouteDataSource", "출발: ($startLat, $startLng), 도착: ($endLat, $endLng)")
            Log.d("TmapRouteDataSource", "출발지명: $startName, 도착지명: $endName")

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
                Log.e("TmapRouteDataSource", "TMAP API 요청 실패: ${response.status.value}")
                return emptyList()
            }

            val responseBody = response.bodyAsText()
            Log.d("TmapRouteDataSource", "TMAP API 응답 수신 (길이: ${responseBody.length})")

            val tmapResponse = response.body<TmapPedestrianResponse>()

            // LineString geometry만 추출하여 좌표 리스트 생성
            val coordinates = mutableListOf<GraphicCoordinateDto>()

            Log.d("TmapRouteDataSource", "TMAP features 개수: ${tmapResponse.features.size}")

            tmapResponse.features.forEachIndexed { featureIndex, feature ->
                Log.d("TmapRouteDataSource", "Feature[$featureIndex] type: ${feature.geometry?.type}")

                if (feature.geometry?.type == "LineString") {
                    val coordsArray = feature.geometry.coordinates
                    Log.d("TmapRouteDataSource", "LineString coordinates type: ${coordsArray?.javaClass?.simpleName}")

                    if (coordsArray is kotlinx.serialization.json.JsonArray) {
                        Log.d("TmapRouteDataSource", "LineString coordinates 개수: ${coordsArray.size}")

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
                                            GraphicCoordinateDto(
                                                latitude = lat,
                                                longitude = lng
                                            )
                                        )
                                    } else {
                                        Log.w("TmapRouteDataSource", "좌표[$coordIndex] 파싱 실패: lng=$lng, lat=$lat")
                                    }
                                } catch (e: Exception) {
                                    Log.e("TmapRouteDataSource", "좌표[$coordIndex] 변환 실패", e)
                                }
                            }
                        }
                    }
                }
            }

            Log.d("TmapRouteDataSource", "TMAP API: ${coordinates.size}개 보행자 경로 좌표 획득")
            coordinates
        } catch (e: Exception) {
            Log.e("TmapRouteDataSource", "TMAP API 호출 실패", e)
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
            Log.e("TmapRouteDataSource", "TMAP API 키를 가져올 수 없습니다", e)
            ""
        }
    }
}
