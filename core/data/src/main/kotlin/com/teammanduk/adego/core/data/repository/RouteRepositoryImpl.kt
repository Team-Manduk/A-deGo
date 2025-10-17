package com.teammanduk.adego.core.data.repository

import android.util.Log
import com.teammanduk.adego.core.data_api.datasource.RouteDataSource
import com.teammanduk.adego.core.data_api.model.GraphicCoordinateDto
import com.teammanduk.adego.core.data_api.model.RouteLegDto
import com.teammanduk.adego.core.data_api.model.TransitRouteDto
import com.teammanduk.adego.core.domain.repository.RouteRepository
import com.teammanduk.adego.core.model.GraphicCoordinate
import com.teammanduk.adego.core.model.Lane
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.SubPath
import com.teammanduk.adego.core.model.TrafficType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 경로 검색 Repository 구현체
 *
 * DataSource를 통해 경로 데이터를 가져와 Domain Model로 변환합니다.
 */
@Singleton
class RouteRepositoryImpl @Inject constructor(
    private val routeDataSource: RouteDataSource
) : RouteRepository {

    override suspend fun searchRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<Route> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "경로 검색 시작: ($startLat, $startLng) → ($endLat, $endLng)")

            val result = routeDataSource.searchTransitRoute(
                startLat = startLat,
                startLng = startLng,
                endLat = endLat,
                endLng = endLng
            )

            result.fold(
                onSuccess = { routeDtos ->
                    val routes = routeDtos.map { dto -> dto.toDomain() }
                    Log.d(TAG, "${routes.size}개 경로 변환 완료")
                    routes
                },
                onFailure = { exception ->
                    Log.e(TAG, "경로 검색 실패", exception)
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "경로 검색 중 오류 발생", e)
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
        Log.d(TAG, "경로 세부 정보 로드 시작")

        // 이미 graphicData가 있는 구간은 그대로, 없는 도보 구간만 추가 로딩
        val updatedSubPaths = route.subPaths.mapIndexed { index, subPath ->
            val existingGraphicData = subPath.graphicData
            if (existingGraphicData != null && existingGraphicData.isNotEmpty()) {
                Log.d(TAG, "SubPath[$index] - 이미 graphicData 존재")
                return@mapIndexed subPath
            }

            // graphicData가 없는 도보 구간만 추가 로딩
            if (subPath.trafficType == TrafficType.WALK) {
                Log.d(TAG, "도보 구간 graphicData 없음 - 보행자 경로 로드")

                val walkStartLat = subPath.startLatitude
                val walkStartLng = subPath.startLongitude
                val walkEndLat = subPath.endLatitude
                val walkEndLng = subPath.endLongitude

                if (walkStartLat != null && walkStartLng != null &&
                    walkEndLat != null && walkEndLng != null
                ) {
                    val result = routeDataSource.searchPedestrianRoute(
                        startLat = walkStartLat,
                        startLng = walkStartLng,
                        endLat = walkEndLat,
                        endLng = walkEndLng,
                        startName = subPath.startName ?: "출발",
                        endName = subPath.endName ?: "도착"
                    )

                    result.fold(
                        onSuccess = { pedestrianDto ->
                            val graphicData = pedestrianDto.coordinates.map { it.toDomain() }
                            subPath.copy(graphicData = graphicData)
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "보행자 경로 로드 실패", exception)
                            subPath
                        }
                    )
                } else {
                    Log.w(TAG, "도보 좌표 정보 없음")
                    subPath
                }
            } else {
                // 대중교통 구간은 그대로 반환
                subPath
            }
        }

        Log.d(TAG, "경로 세부 정보 로드 완료")
        route.copy(subPaths = updatedSubPaths)
    }

    /**
     * TransitRouteDto → Route 변환
     */
    private fun TransitRouteDto.toDomain(): Route {
        return Route(
            totalTime = this.totalTime,
            totalDistance = this.totalDistance,
            totalFare = this.totalFare,
            transferCount = this.transferCount,
            pathType = this.pathType,
            subPaths = this.legs.map { it.toDomain() }
        )
    }

    /**
     * RouteLegDto → SubPath 변환
     */
    private fun RouteLegDto.toDomain(): SubPath {
        val trafficType = when (this.mode) {
            "SUBWAY" -> TrafficType.SUBWAY
            "BUS" -> TrafficType.BUS
            else -> TrafficType.WALK
        }

        return SubPath(
            trafficType = trafficType,
            distance = this.distance,
            sectionTime = this.sectionTime,
            startName = this.startName,
            endName = this.endName,
            stationCount = this.stationCount,
            lane = if (trafficType != TrafficType.WALK) {
                Lane(
                    name = this.route ?: "",
                    busNo = if (trafficType == TrafficType.BUS) this.route else null,
                    type = this.routeType,
                    subwayCode = if (trafficType == TrafficType.SUBWAY) this.routeId?.toIntOrNull() else null
                )
            } else null,
            walkDistance = if (trafficType == TrafficType.WALK) this.distance else null,
            startLatitude = this.startLatitude,
            startLongitude = this.startLongitude,
            endLatitude = this.endLatitude,
            endLongitude = this.endLongitude,
            passStations = this.passStations?.map { stationDto ->
                com.teammanduk.adego.core.model.Station(
                    name = stationDto.name,
                    latitude = stationDto.latitude,
                    longitude = stationDto.longitude
                )
            },
            graphicData = this.graphicCoordinates?.map { it.toDomain() }
        )
    }

    /**
     * GraphicCoordinateDto → GraphicCoordinate 변환
     */
    private fun GraphicCoordinateDto.toDomain(): GraphicCoordinate {
        return GraphicCoordinate(
            latitude = this.latitude,
            longitude = this.longitude
        )
    }

    companion object {
        private const val TAG = "RouteRepository"
    }
}
