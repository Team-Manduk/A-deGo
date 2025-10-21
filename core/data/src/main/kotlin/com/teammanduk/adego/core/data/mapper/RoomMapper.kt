package com.teammanduk.adego.core.data.mapper

import com.teammanduk.adego.core.data_api.model.GraphicCoordinateDto
import com.teammanduk.adego.core.data_api.model.LaneDto
import com.teammanduk.adego.core.data_api.model.ParticipantDto
import com.teammanduk.adego.core.data_api.model.ParticipantLocationDto
import com.teammanduk.adego.core.data_api.model.ParticipantRouteDto
import com.teammanduk.adego.core.data_api.model.PlaceDto
import com.teammanduk.adego.core.data_api.model.RoomDto
import com.teammanduk.adego.core.data_api.model.RouteDto
import com.teammanduk.adego.core.data_api.model.StationDto
import com.teammanduk.adego.core.data_api.model.SubPathDto
import com.teammanduk.adego.core.model.GraphicCoordinate
import com.teammanduk.adego.core.model.Lane
import com.teammanduk.adego.core.model.MovementStatus
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.core.model.Room
import com.teammanduk.adego.core.model.Route
import com.teammanduk.adego.core.model.Station
import com.teammanduk.adego.core.model.SubPath
import com.teammanduk.adego.core.model.TrafficType

// Room
fun RoomDto.toModel(): Room {
    return Room(
        roomId = roomId,
        roomName = roomName,
        destination = destination.toModel(),
        dateTime = dateTime,
        createdBy = createdBy,
        createdAt = createdAt
    )
}

fun Room.toDto(): RoomDto {
    return RoomDto(
        roomId = roomId,
        roomName = roomName,
        destination = destination.toDto(),
        dateTime = dateTime,
        createdBy = createdBy,
        createdAt = createdAt
    )
}

// Participant
fun ParticipantDto.toModel(): Participant {
    return Participant(
        userId = userId,
        name = name,
        profileColor = profileColor,
        location = location?.toModel(),
        route = route?.toModel(),
        movementStatus = try {
            MovementStatus.valueOf(movementStatus)
        } catch (e: IllegalArgumentException) {
            MovementStatus.NOT_STARTED
        }
    )
}

fun Participant.toDto(): ParticipantDto {
    return ParticipantDto(
        userId = userId,
        name = name,
        profileColor = profileColor,
        location = location?.toDto(),
        route = route?.toDto(),
        movementStatus = movementStatus.name
    )
}

// ParticipantLocation
fun ParticipantLocationDto.toModel(): ParticipantLocation {
    return ParticipantLocation(
        latitude = latitude,
        longitude = longitude,
        updatedAt = updatedAt,
        accuracy = accuracy
    )
}

fun ParticipantLocation.toDto(): ParticipantLocationDto {
    return ParticipantLocationDto(
        latitude = latitude,
        longitude = longitude,
        updatedAt = updatedAt,
        accuracy = accuracy
    )
}

// ParticipantRoute
fun ParticipantRouteDto.toModel(): ParticipantRoute {
    return ParticipantRoute(
        eta = eta,
        distance = distance,
        polyline = polyline,
        durationInSeconds = durationInSeconds,
        distanceInMeters = distanceInMeters,
        updatedAt = updatedAt,
        selectedRoute = selectedRoute?.toModel()
    )
}

fun ParticipantRoute.toDto(): ParticipantRouteDto {
    return ParticipantRouteDto(
        eta = eta,
        distance = distance,
        polyline = polyline,
        durationInSeconds = durationInSeconds,
        distanceInMeters = distanceInMeters,
        updatedAt = updatedAt,
        selectedRoute = selectedRoute?.toDto()
    )
}

// Route
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

fun Route.toDto(): RouteDto {
    return RouteDto(
        totalTime = totalTime,
        totalDistance = totalDistance,
        totalFare = totalFare,
        transferCount = transferCount,
        pathType = pathType,
        subPaths = subPaths.map { it.toDto() },
        startLatitude = startLatitude ?: 0.0,
        startLongitude = startLongitude ?: 0.0,
        endLatitude = endLatitude ?: 0.0,
        endLongitude = endLongitude ?: 0.0
    )
}

// SubPath
fun SubPathDto.toModel(): SubPath {
    return SubPath(
        trafficType = TrafficType.valueOf(trafficType),
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

fun SubPath.toDto(): SubPathDto {
    return SubPathDto(
        trafficType = trafficType.name,
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

// Lane
fun LaneDto.toModel(): Lane {
    return Lane(
        name = name,
        busNo = busNo,
        type = type,
        subwayCode = subwayCode
    )
}

fun Lane.toDto(): LaneDto {
    return LaneDto(
        name = name,
        busNo = busNo,
        type = type,
        subwayCode = subwayCode
    )
}

// Station
fun StationDto.toModel(): Station {
    return Station(
        name = name,
        latitude = latitude,
        longitude = longitude
    )
}

fun Station.toDto(): StationDto {
    return StationDto(
        name = name,
        latitude = latitude,
        longitude = longitude
    )
}

// GraphicCoordinate
fun GraphicCoordinateDto.toModel(): GraphicCoordinate {
    return GraphicCoordinate(
        latitude = latitude,
        longitude = longitude
    )
}

fun GraphicCoordinate.toDto(): GraphicCoordinateDto {
    return GraphicCoordinateDto(
        latitude = latitude,
        longitude = longitude
    )
}
