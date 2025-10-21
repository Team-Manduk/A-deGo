package com.teammanduk.adego.core.data.repository

import com.teammanduk.adego.core.data.mapper.toDto
import com.teammanduk.adego.core.data.mapper.toModel
import com.teammanduk.adego.core.data_api.datasource.SelectedRouteDataSource
import com.teammanduk.adego.core.data_api.datasource.SessionDataSource
import com.teammanduk.adego.core.domain.repository.SelectedRouteRepository
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 선택된 경로 저장소 Repository 구현
 * Domain Model (Route) ↔ Data DTO (RouteDto) 변환 담당
 */
@Singleton
class SelectedRouteRepositoryImpl @Inject constructor(
    private val selectedRouteDataSource: SelectedRouteDataSource,
    private val sessionDataSource: SessionDataSource
) : SelectedRouteRepository {

    override suspend fun saveSelectedRoute(route: Route): Result<Unit> {
        return try {
            val roomId = sessionDataSource.getRoomId()
                ?: return Result.failure(IllegalStateException("Room ID not found in session"))

            // Domain Model → DTO 변환
            selectedRouteDataSource.saveRoute(roomId, route.toDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSelectedRoute(): Result<Route?> {
        return try {
            val roomId = sessionDataSource.getRoomId()
                ?: return Result.failure(IllegalStateException("Room ID not found in session"))

            // DTO → Domain Model 변환
            val routeDto = selectedRouteDataSource.getRoute(roomId)
            val route = routeDto?.toModel()
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSelectedRoute(): Result<Unit> {
        return try {
            val roomId = sessionDataSource.getRoomId()
                ?: return Result.failure(IllegalStateException("Room ID not found in session"))

            selectedRouteDataSource.deleteRoute(roomId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
