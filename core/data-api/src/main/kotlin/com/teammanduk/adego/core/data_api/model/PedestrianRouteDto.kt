package com.teammanduk.adego.core.data_api.model

/**
 * 보행자 경로 DTO
 */
data class PedestrianRouteDto(
    val coordinates: List<GraphicCoordinateDto> // 경로 좌표 목록
)
