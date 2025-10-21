package com.teammanduk.adego.core.data.repository

import com.teammanduk.adego.core.data_api.datasource.SelectedRouteDataSource
import com.teammanduk.adego.core.data_api.datasource.SessionDataSource
import com.teammanduk.adego.core.domain.repository.SelectedRouteRepository
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 선택된 경로 저장소 Repository 구현
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

            selectedRouteDataSource.saveRoute(roomId, route)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSelectedRoute(): Result<Route?> {
        return try {
            val roomId = sessionDataSource.getRoomId()
                ?: return Result.failure(IllegalStateException("Room ID not found in session"))

            val route = selectedRouteDataSource.getRoute(roomId)
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
