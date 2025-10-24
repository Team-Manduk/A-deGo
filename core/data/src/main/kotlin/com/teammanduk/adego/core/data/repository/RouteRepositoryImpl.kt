package com.teammanduk.adego.core.data.repository

import com.teammanduk.adego.core.data_api.datasource.RouteDataSource
import com.teammanduk.adego.core.data_api.model.GraphicCoordinateDto
import com.teammanduk.adego.core.data_api.model.LaneDto
import com.teammanduk.adego.core.data_api.model.RouteDto
import com.teammanduk.adego.core.data_api.model.StationDto
import com.teammanduk.adego.core.data_api.model.SubPathDto
import com.teammanduk.adego.core.data_api.model.TrafficTypeDto
import com.teammanduk.adego.core.domain.repository.RouteRepository
import com.teammanduk.adego.core.model.GraphicCoordinate
import com.teammanduk.adego.core.model.Lane
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.Station
import com.teammanduk.adego.core.model.SubPath
import com.teammanduk.adego.core.model.TrafficType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepositoryImpl @Inject constructor(
    private val routeDataSource: RouteDataSource
) : RouteRepository {

    override suspend fun searchRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): List<Route> {
        val routeDtos = routeDataSource.searchRoutes(startLat, startLng, endLat, endLng)
        return routeDtos.map { it.toModel() }
    }

    override suspend fun getRouteDetails(
        route: Route,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Route {
        val routeDto = route.toDto()
        val detailedRouteDto = routeDataSource.getRouteDetails(routeDto, startLat, startLng, endLat, endLng)
        return detailedRouteDto.toModel()
    }

    /**
     * RouteDto를 Route 도메인 모델로 변환합니다.
     */
    private fun RouteDto.toModel(): Route {
        return Route(
            totalTime = totalTime,
            totalDistance = totalDistance,
            totalFare = totalFare,
            transferCount = transferCount,
            pathType = pathType,
            subPaths = subPaths.map { it.toModel() },
            startLatitude = startLatitude,
            startLongitude = startLongitude,
            endLatitude = endLatitude,
            endLongitude = endLongitude
        )
    }

    /**
     * SubPathDto를 SubPath 도메인 모델로 변환합니다.
     */
    private fun SubPathDto.toModel(): SubPath {
        return SubPath(
            trafficType = trafficType.toModel(),
            distance = distance,
            sectionTime = sectionTime,
            startName = startName,
            endName = endName,
            stationCount = stationCount,
            lane = lane?.toModel(),
            walkDistance = walkDistance,
            startLatitude = startLatitude,
            startLongitude = startLongitude,
            endLatitude = endLatitude,
            endLongitude = endLongitude,
            passStations = passStations?.map { it.toModel() },
            graphicData = graphicData?.map { it.toModel() }
        )
    }

    /**
     * LaneDto를 Lane 도메인 모델로 변환합니다.
     */
    private fun LaneDto.toModel(): Lane {
        return Lane(
            name = name,
            busNo = busNo,
            type = type,
            subwayCode = subwayCode
        )
    }

    /**
     * StationDto를 Station 도메인 모델로 변환합니다.
     */
    private fun StationDto.toModel(): Station {
        return Station(
            name = name,
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * GraphicCoordinateDto를 GraphicCoordinate 도메인 모델로 변환합니다.
     */
    private fun GraphicCoordinateDto.toModel(): GraphicCoordinate {
        return GraphicCoordinate(
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * TrafficTypeDto를 TrafficType 도메인 모델로 변환합니다.
     */
    private fun TrafficTypeDto.toModel(): TrafficType {
        return when (this) {
            TrafficTypeDto.SUBWAY -> TrafficType.SUBWAY
            TrafficTypeDto.BUS -> TrafficType.BUS
            TrafficTypeDto.WALK -> TrafficType.WALK
        }
    }

    /**
     * Route 도메인 모델을 RouteDto로 변환합니다.
     */
    private fun Route.toDto(): RouteDto {
        return RouteDto(
            totalTime = totalTime,
            totalDistance = totalDistance,
            totalFare = totalFare,
            transferCount = transferCount,
            pathType = pathType,
            subPaths = subPaths.map { it.toDto() },
            startLatitude = startLatitude,
            startLongitude = startLongitude,
            endLatitude = endLatitude,
            endLongitude = endLongitude
        )
    }

    /**
     * SubPath 도메인 모델을 SubPathDto로 변환합니다.
     */
    private fun SubPath.toDto(): SubPathDto {
        return SubPathDto(
            trafficType = trafficType.toDto(),
            distance = distance,
            sectionTime = sectionTime,
            startName = startName,
            endName = endName,
            stationCount = stationCount,
            lane = lane?.toDto(),
            walkDistance = walkDistance,
            startLatitude = startLatitude,
            startLongitude = startLongitude,
            endLatitude = endLatitude,
            endLongitude = endLongitude,
            passStations = passStations?.map { it.toDto() },
            graphicData = graphicData?.map { it.toDto() }
        )
    }

    /**
     * Lane 도메인 모델을 LaneDto로 변환합니다.
     */
    private fun Lane.toDto(): LaneDto {
        return LaneDto(
            name = name,
            busNo = busNo,
            type = type,
            subwayCode = subwayCode
        )
    }

    /**
     * Station 도메인 모델을 StationDto로 변환합니다.
     */
    private fun Station.toDto(): StationDto {
        return StationDto(
            name = name,
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * GraphicCoordinate 도메인 모델을 GraphicCoordinateDto로 변환합니다.
     */
    private fun GraphicCoordinate.toDto(): GraphicCoordinateDto {
        return GraphicCoordinateDto(
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * TrafficType 도메인 모델을 TrafficTypeDto로 변환합니다.
     */
    private fun TrafficType.toDto(): TrafficTypeDto {
        return when (this) {
            TrafficType.SUBWAY -> TrafficTypeDto.SUBWAY
            TrafficType.BUS -> TrafficTypeDto.BUS
            TrafficType.WALK -> TrafficTypeDto.WALK
        }
    }
}
