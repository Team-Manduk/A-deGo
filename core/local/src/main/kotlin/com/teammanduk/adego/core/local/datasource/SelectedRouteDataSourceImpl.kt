package com.teammanduk.adego.core.local.datasource

import com.teammanduk.adego.core.data_api.datasource.SelectedRouteDataSource
import com.teammanduk.adego.core.local.dao.SelectedRouteDao
import com.teammanduk.adego.core.local.model.SelectedRouteEntity
import com.teammanduk.adego.core.model.Route
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Room DB 기반 선택된 경로 저장소 구현
 */
class SelectedRouteDataSourceImpl @Inject constructor(
    private val selectedRouteDao: SelectedRouteDao
) : SelectedRouteDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun saveRoute(roomId: String, route: Route) {
        val entity = SelectedRouteEntity(
            roomId = roomId,
            totalTime = route.totalTime,
            totalDistance = route.totalDistance,
            totalFare = route.totalFare,
            transferCount = route.transferCount,
            pathType = route.pathType,
            subPathsJson = json.encodeToString(route.subPaths),
            startLatitude = route.startLatitude,
            startLongitude = route.startLongitude,
            endLatitude = route.endLatitude,
            endLongitude = route.endLongitude,
            selectedAt = System.currentTimeMillis()
        )
        selectedRouteDao.saveRoute(entity)
    }

    override suspend fun getRoute(roomId: String): Route? {
        val entity = selectedRouteDao.getRoute(roomId) ?: return null

        return Route(
            totalTime = entity.totalTime,
            totalDistance = entity.totalDistance,
            totalFare = entity.totalFare,
            transferCount = entity.transferCount,
            pathType = entity.pathType,
            subPaths = json.decodeFromString(entity.subPathsJson),
            startLatitude = entity.startLatitude,
            startLongitude = entity.startLongitude,
            endLatitude = entity.endLatitude,
            endLongitude = entity.endLongitude
        )
    }

    override suspend fun deleteRoute(roomId: String) {
        selectedRouteDao.deleteRoute(roomId)
    }

    override suspend fun deleteAllRoutes() {
        selectedRouteDao.deleteAllRoutes()
    }
}
