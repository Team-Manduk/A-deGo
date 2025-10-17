package com.teammanduk.adego.core.data_api.model

data class PlaceDto(
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isPOI: Boolean = true
)
