package com.teammanduk.adego.core.data.mapper

import com.teammanduk.adego.core.data_api.model.GraphicCoordinateDto
import com.teammanduk.adego.core.data_api.model.LaneDto
import com.teammanduk.adego.core.data_api.model.RouteDto
import com.teammanduk.adego.core.data_api.model.StationDto
import com.teammanduk.adego.core.data_api.model.SubPathDto
import com.teammanduk.adego.core.data_api.model.TrafficTypeDto
import com.teammanduk.adego.core.model.GraphicCoordinate
import com.teammanduk.adego.core.model.Lane
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.Station
import com.teammanduk.adego.core.model.SubPath
import com.teammanduk.adego.core.model.TrafficType

// Route ↔ RouteDto
fun Route.toDto(): RouteDto {
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

fun RouteDto.toModel(): Route {
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

// SubPath ↔ SubPathDto
fun SubPath.toDto(): SubPathDto {
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

fun SubPathDto.toModel(): SubPath {
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

// GraphicCoordinate ↔ GraphicCoordinateDto
fun GraphicCoordinate.toDto(): GraphicCoordinateDto {
    return GraphicCoordinateDto(
        latitude = latitude,
        longitude = longitude
    )
}

fun GraphicCoordinateDto.toModel(): GraphicCoordinate {
    return GraphicCoordinate(
        latitude = latitude,
        longitude = longitude
    )
}

// Station ↔ StationDto
fun Station.toDto(): StationDto {
    return StationDto(
        name = name,
        latitude = latitude,
        longitude = longitude
    )
}

fun StationDto.toModel(): Station {
    return Station(
        name = name,
        latitude = latitude,
        longitude = longitude
    )
}

// Lane ↔ LaneDto
fun Lane.toDto(): LaneDto {
    return LaneDto(
        name = name,
        busNo = busNo,
        type = type,
        subwayCode = subwayCode
    )
}

fun LaneDto.toModel(): Lane {
    return Lane(
        name = name,
        busNo = busNo,
        type = type,
        subwayCode = subwayCode
    )
}

// TrafficType ↔ TrafficTypeDto
fun TrafficType.toDto(): TrafficTypeDto {
    return when (this) {
        TrafficType.SUBWAY -> TrafficTypeDto.SUBWAY
        TrafficType.BUS -> TrafficTypeDto.BUS
        TrafficType.WALK -> TrafficTypeDto.WALK
    }
}

fun TrafficTypeDto.toModel(): TrafficType {
    return when (this) {
        TrafficTypeDto.SUBWAY -> TrafficType.SUBWAY
        TrafficTypeDto.BUS -> TrafficType.BUS
        TrafficTypeDto.WALK -> TrafficType.WALK
    }
}
