package com.teammanduk.adego.core.data.repository

import android.util.Log
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
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// ODsay API Response Models
@Serializable
private data class OdsayResponse(
    val result: OdsayResult? = null,
    val error: OdsayError? = null
)

@Serializable
private data class OdsayResult(
    val path: List<OdsayPath> = emptyList()
)

@Serializable
private data class OdsayPath(
    val pathType: Int, // 경로 타입 (1: 지하철, 2: 버스, 3: 지하철+버스) - path 레벨에 있음!
    val info: OdsayPathInfo,
    val subPath: List<OdsaySubPath> = emptyList()
)

@Serializable
private data class OdsayPathInfo(
    val totalTime: Int, // 총 소요 시간 (분)
    val payment: Int, // 총 요금 (원)
    val busTransitCount: Int, // 버스 환승 횟수
    val subwayTransitCount: Int, // 지하철 환승 횟수
    val trafficDistance: Double? = null, // 대중교통 이동 거리 (미터)
    val totalDistance: Double, // 총 거리 (미터) - API에서 Double로 반환

    // API 응답에 포함된 추가 필드들 (파싱 오류 방지를 위해 추가)
    val totalWalk: Int? = null, // 총 도보 거리 (미터)
    val totalWalkTime: Int? = null, // 총 도보 시간 (분)
    val firstStartStation: String? = null, // 첫 출발역/정류장
    val lastEndStation: String? = null, // 마지막 도착역/정류장
    val totalStationCount: Int? = null, // 총 정거장 수
    val busStationCount: Int? = null, // 버스 정거장 수
    val subwayStationCount: Int? = null, // 지하철 역 수
    val checkIntervalTime: Int? = null, // 배차 간격 시간
    val checkIntervalTimeOverYn: String? = null, // 배차 간격 초과 여부
    val totalIntervalTime: Int? = null, // 총 환승 대기 시간
    val mapObj: String? = null // 지도 객체 정보
)

@Serializable
private data class OdsaySubPath(
    val trafficType: Int, // 이동 수단 유형 (1: 지하철, 2: 버스, 3: 도보)
    val distance: Double, // 이동 거리 (미터) - 도보일 때는 도보 거리
    val sectionTime: Int, // 이동 시간 (분)

    // 대중교통 정보
    val startName: String? = null, // 승차 정류장/역 이름
    val endName: String? = null, // 하차 정류장/역 이름
    val stationCount: Int? = null, // 정거장 개수
    val lane: List<OdsayLane>? = null, // 노선 정보

    // 좌표 정보
    val startX: Double? = null, // 출발지 경도
    val startY: Double? = null, // 출발지 위도
    val endX: Double? = null, // 도착지 경도
    val endY: Double? = null, // 도착지 위도

    // 경유 정류장 목록
    val passStopList: OdsayPassStopList? = null
)

@Serializable
private data class OdsayLane(
    val name: String? = null, // 노선명
    val busNo: String? = null, // 버스 번호
    val type: Int? = null, // 버스 타입
    val subwayCode: Int? = null // 지하철 노선 번호
)

@Serializable
private data class OdsayPassStopList(
    val stations: List<OdsayStation>? = null
)

@Serializable
private data class OdsayStation(
    val stationName: String? = null, // 정류장/역 이름
    val x: Double? = null, // 경도
    val y: Double? = null, // 위도
    val stationID: String? = null // 정류장/역 ID
)

// loadLane API Response Models
@Serializable
private data class LoadLaneResponse(
    val result: LoadLaneResult? = null,
    val error: OdsayError? = null
)

@Serializable
private data class LoadLaneResult(
    val lane: List<LoadLaneLane> = emptyList(),
    val boundary: LoadLaneBoundary? = null
)

@Serializable
private data class LoadLaneLane(
    val class_: Int? = null, // 1: 버스, 2: 지하철 (class는 예약어라 class_로)
    val type: Int? = null,
    val section: List<LoadLaneSection> = emptyList()
)

@Serializable
private data class LoadLaneSection(
    val graphPos: List<LoadLaneGraphPos> = emptyList()
)

@Serializable
private data class LoadLaneGraphPos(
    val x: Double, // 경도
    val y: Double  // 위도
)

@Serializable
private data class LoadLaneBoundary(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double
)

@Serializable
private data class OdsayError(
    val code: Int,
    val message: String
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
                    Log.d("OdsayClient", message)
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
            Log.d("RouteRepository", "ODsay API 경로 검색 시작")
            Log.d("RouteRepository", "출발: ($startLat, $startLng)")
            Log.d("RouteRepository", "도착: ($endLat, $endLng)")

            // API 키 가져오기
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                Log.e("RouteRepository", "ODsay API 키를 찾을 수 없습니다")
                return@withContext emptyList()
            }

            // API 호출
            val response = httpClient.get("https://api.odsay.com/v1/api/searchPubTransPathT") {
                parameter("SX", startLng) // ODsay는 경도(X)를 먼저
                parameter("SY", startLat) // 위도(Y)를 나중에
                parameter("EX", endLng)
                parameter("EY", endLat)
                parameter("apiKey", apiKey)
            }

            val responseBody = response.bodyAsText()
            Log.d("RouteRepository", "ODsay API 응답 코드: ${response.status.value}")
            Log.d("RouteRepository", "ODsay API 응답: $responseBody")

            if (response.status.value !in 200..299) {
                Log.e("RouteRepository", "ODsay API 요청 실패: ${response.status.value}")
                return@withContext emptyList()
            }

            // 응답 파싱
            val odsayResponse = response.body<OdsayResponse>()

            // 에러 체크
            if (odsayResponse.error != null) {
                Log.e("RouteRepository", "ODsay API 에러: ${odsayResponse.error.message}")
                return@withContext emptyList()
            }

            // 경로 변환 (그래픽 데이터 포함)
            val routes = odsayResponse.result?.path?.map { path ->
                mapOdsayPathToRouteWithGraphics(path)
            } ?: emptyList()

            Log.d("RouteRepository", "ODsay API: ${routes.size}개 경로 발견")
            routes
        } catch (e: Exception) {
            Log.e("RouteRepository", "ODsay API 경로 검색 실패", e)
            emptyList()
        }
    }

    /**
     * ODsay API 응답을 도메인 모델로 변환합니다 (그래픽 데이터 포함).
     */
    private suspend fun mapOdsayPathToRouteWithGraphics(path: OdsayPath): Route {
        // path.info.mapObj를 @로 split하여 각 대중교통 구간에 매핑
        val mapObjs = path.info.mapObj?.split("@") ?: emptyList()
        var mapObjIndex = 0

        Log.d("RouteRepository", "Path mapObj: ${path.info.mapObj}")
        Log.d("RouteRepository", "Split된 mapObj 개수: ${mapObjs.size}")

        val subPaths = path.subPath.map { subPath ->
            // 대중교통 구간인 경우 그래픽 데이터 로드
            val graphicData = if (subPath.trafficType in listOf(1, 2)) {
                val mapObj = mapObjs.getOrNull(mapObjIndex)
                mapObjIndex++

                Log.d("RouteRepository", "대중교통 구간 - trafficType: ${subPath.trafficType}, mapObj: $mapObj")

                if (!mapObj.isNullOrEmpty()) {
                    Log.d("RouteRepository", "loadLane API 호출")
                    loadLaneGraphicData(mapObj)
                } else {
                    Log.d("RouteRepository", "mapObj 없음")
                    null
                }
            } else {
                Log.d("RouteRepository", "도보 구간 - trafficType: ${subPath.trafficType}")
                null
            }

            Log.d("RouteRepository", "GraphicData 개수: ${graphicData?.size ?: 0}")

            SubPath(
                trafficType = when (subPath.trafficType) {
                    1 -> TrafficType.SUBWAY
                    2 -> TrafficType.BUS
                    else -> TrafficType.WALK
                },
                distance = subPath.distance,
                sectionTime = subPath.sectionTime,
                startName = subPath.startName,
                endName = subPath.endName,
                stationCount = subPath.stationCount,
                lane = subPath.lane?.firstOrNull()?.let { lane ->
                    Lane(
                        name = lane.name ?: "",
                        busNo = lane.busNo,
                        type = lane.type,
                        subwayCode = lane.subwayCode
                    )
                },
                walkDistance = if (subPath.trafficType == 3) subPath.distance else null,
                // 좌표 정보 매핑
                startLatitude = subPath.startY,
                startLongitude = subPath.startX,
                endLatitude = subPath.endY,
                endLongitude = subPath.endX,
                // 경유 정류장 목록 매핑
                passStations = subPath.passStopList?.stations?.mapNotNull { station ->
                    if (station.stationName != null && station.y != null && station.x != null) {
                        com.teammanduk.adego.core.model.Station(
                            name = station.stationName,
                            latitude = station.y,
                            longitude = station.x
                        )
                    } else {
                        null
                    }
                },
                // 그래픽 데이터 매핑
                graphicData = graphicData
            )
        }

        return Route(
            totalTime = path.info.totalTime,
            totalDistance = path.info.totalDistance.toInt(), // Double을 Int로 변환
            totalFare = path.info.payment,
            transferCount = path.info.busTransitCount + path.info.subwayTransitCount,
            pathType = path.pathType, // path.info.pathType이 아니라 path.pathType!
            subPaths = subPaths
        )
    }

    /**
     * loadLane API를 호출하여 노선 그래픽 데이터를 가져옵니다.
     */
    private suspend fun loadLaneGraphicData(mapObj: String): List<com.teammanduk.adego.core.model.GraphicCoordinate> {
        return try {
            val apiKey = getApiKey()
            if (apiKey.isEmpty() || mapObj.isEmpty()) {
                return emptyList()
            }

            // mapObject 파라미터 형식: "0:0@{mapObj}"
            val mapObjectParam = "0:0@$mapObj"

            Log.d("RouteRepository", "loadLane API 호출 시작 - mapObj: $mapObj")

            val response = httpClient.get("https://api.odsay.com/v1/api/loadLane") {
                parameter("mapObject", mapObjectParam)
                parameter("apiKey", apiKey)
            }

            if (response.status.value !in 200..299) {
                Log.e("RouteRepository", "loadLane API 요청 실패: ${response.status.value}")
                return emptyList()
            }

            val responseBody = response.bodyAsText()
            Log.d("RouteRepository", "loadLane API 응답: $responseBody")

            val loadLaneResponse = response.body<LoadLaneResponse>()

            if (loadLaneResponse.error != null) {
                Log.e("RouteRepository", "loadLane API 에러: ${loadLaneResponse.error.message}")
                return emptyList()
            }

            // 모든 section의 graphPos를 하나의 리스트로 병합
            val graphicCoordinates = mutableListOf<com.teammanduk.adego.core.model.GraphicCoordinate>()
            loadLaneResponse.result?.lane?.forEach { lane ->
                lane.section.forEach { section ->
                    section.graphPos.forEach { pos ->
                        graphicCoordinates.add(
                            com.teammanduk.adego.core.model.GraphicCoordinate(
                                latitude = pos.y,
                                longitude = pos.x
                            )
                        )
                    }
                }
            }

            Log.d("RouteRepository", "loadLane API: ${graphicCoordinates.size}개 좌표 획득")
            graphicCoordinates
        } catch (e: Exception) {
            Log.e("RouteRepository", "loadLane API 호출 실패", e)
            emptyList()
        }
    }

    /**
     * BuildConfig에서 ODsay API 키를 가져옵니다.
     */
    private fun getApiKey(): String {
        return try {
            val buildConfigClass = Class.forName("com.teammanduk.adego.BuildConfig")
            val field = buildConfigClass.getDeclaredField("ODSAY_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            Log.e("RouteRepository", "ODsay API 키를 가져올 수 없습니다", e)
            ""
        }
    }
}
