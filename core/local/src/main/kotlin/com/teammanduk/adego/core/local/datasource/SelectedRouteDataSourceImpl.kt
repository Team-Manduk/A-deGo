package com.teammanduk.adego.core.local.datasource

import com.teammanduk.adego.core.data_api.datasource.SelectedRouteDataSource
import com.teammanduk.adego.core.data_api.model.RouteDto
import com.teammanduk.adego.core.local.dao.SelectedRouteDao
import com.teammanduk.adego.core.local.model.SelectedRouteEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Room DB 기반 선택된 경로 저장소 구현
 * RouteDto ↔ SelectedRouteEntity 변환 담당
 */
class SelectedRouteDataSourceImpl @Inject constructor(
    private val selectedRouteDao: SelectedRouteDao
) : SelectedRouteDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun saveRoute(roomId: String, route: RouteDto) {
        // RouteDto → SelectedRouteEntity 변환
        val entity = SelectedRouteEntity(
            roomId = roomId,
            totalTime = route.totalTime,
            totalDistance = route.totalDistance,
            totalFare = route.totalFare,
            transferCount = route.transferCount,
            pathType = route.pathType,
            subPathsJson = json.encodeToString(route.subPaths), // SubPathDto 직렬화
            startLatitude = route.startLatitude,
            startLongitude = route.startLongitude,
            endLatitude = route.endLatitude,
            endLongitude = route.endLongitude,
            selectedAt = System.currentTimeMillis()
        )
        selectedRouteDao.saveRoute(entity)
    }

    override suspend fun getRoute(roomId: String): RouteDto? {
        val entity = selectedRouteDao.getRoute(roomId) ?: return null

        // SelectedRouteEntity → RouteDto 변환
        return RouteDto(
            totalTime = entity.totalTime,
            totalDistance = entity.totalDistance,
            totalFare = entity.totalFare,
            transferCount = entity.transferCount,
            pathType = entity.pathType,
            subPaths = json.decodeFromString(entity.subPathsJson), // SubPathDto 역직렬화
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
