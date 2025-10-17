package com.teammanduk.adego.core.data.mapper

import com.teammanduk.adego.core.data_api.model.PlaceDto
import com.teammanduk.adego.core.model.Place

fun PlaceDto.toModel(): Place {
    return Place(
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        isPOI = isPOI
    )
}

fun Place.toDto(): PlaceDto {
    return PlaceDto(
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        isPOI = isPOI
    )
}
