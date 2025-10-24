package com.teammanduk.adego.core.data.repository

import com.teammanduk.adego.core.data.mapper.toDto
import com.teammanduk.adego.core.data.mapper.toModel
import com.teammanduk.adego.core.data_api.datasource.RouteDataSource
import com.teammanduk.adego.core.domain.repository.RouteRepository
import com.teammanduk.adego.core.model.Route
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
}
