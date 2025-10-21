package com.teammanduk.adego.core.data.mapper

import com.teammanduk.adego.core.data_api.model.ParticipantDto
import com.teammanduk.adego.core.data_api.model.ParticipantLocationDto
import com.teammanduk.adego.core.data_api.model.ParticipantRouteDto
import com.teammanduk.adego.core.data_api.model.PlaceDto
import com.teammanduk.adego.core.data_api.model.RoomDto
import com.teammanduk.adego.core.model.MovementStatus
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.core.model.Room

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
        currentSubPathIndex = currentSubPathIndex,
        progressInCurrentSubPath = progressInCurrentSubPath,
        traveledDistance = traveledDistance,
        remainingDistance = remainingDistance
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
        currentSubPathIndex = currentSubPathIndex,
        progressInCurrentSubPath = progressInCurrentSubPath,
        traveledDistance = traveledDistance,
        remainingDistance = remainingDistance
    )
}
