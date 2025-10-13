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
    val info: OdsayPathInfo,
    val subPath: List<OdsaySubPath> = emptyList()
)

@Serializable
private data class OdsayPathInfo(
    val totalTime: Int, // 총 소요 시간 (분)
    val payment: Int, // 총 요금 (원)
    val busTransitCount: Int, // 버스 환승 횟수
    val subwayTransitCount: Int, // 지하철 환승 횟수
    val totalDistance: Int, // 총 거리 (미터)
    val pathType: Int // 경로 타입 (1: 지하철, 2: 버스, 3: 지하철+버스)
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
    val lane: List<OdsayLane>? = null // 노선 정보
)

@Serializable
private data class OdsayLane(
    val name: String? = null, // 노선명
    val busNo: String? = null, // 버스 번호
    val type: Int? = null, // 버스 타입
    val subwayCode: Int? = null // 지하철 노선 번호
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

            // 경로 변환
            val routes = odsayResponse.result?.path?.map { path ->
                mapOdsayPathToRoute(path)
            } ?: emptyList()

            Log.d("RouteRepository", "ODsay API: ${routes.size}개 경로 발견")
            routes
        } catch (e: Exception) {
            Log.e("RouteRepository", "ODsay API 경로 검색 실패", e)
            emptyList()
        }
    }

    /**
     * ODsay API 응답을 도메인 모델로 변환합니다.
     */
    private fun mapOdsayPathToRoute(path: OdsayPath): Route {
        val subPaths = path.subPath.map { subPath ->
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
                walkDistance = if (subPath.trafficType == 3) subPath.distance else null
            )
        }

        return Route(
            totalTime = path.info.totalTime,
            totalDistance = path.info.totalDistance,
            totalFare = path.info.payment,
            transferCount = path.info.busTransitCount + path.info.subwayTransitCount,
            pathType = path.info.pathType,
            subPaths = subPaths
        )
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
