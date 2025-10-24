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

    // 🚀 최적화: 현재 활성 경로 메모리 캐시 (JSON 역직렬화 비용 제거)
    private var cachedRoute: Route? = null
    private var cachedRoomId: String? = null

    override suspend fun saveSelectedRoute(route: Route): Result<Unit> {
        return try {
            val roomId = sessionDataSource.getRoomId()
                ?: return Result.failure(IllegalStateException("Room ID not found in session"))

            // Domain Model → DTO 변환 및 저장
            selectedRouteDataSource.saveRoute(roomId, route.toDto())

            // 메모리 캐시 업데이트
            cachedRoute = route
            cachedRoomId = roomId

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSelectedRoute(): Result<Route?> {
        return try {
            val roomId = sessionDataSource.getRoomId()
                ?: return Result.failure(IllegalStateException("Room ID not found in session"))

            // 🚀 최적화: 캐시된 경로가 있고 같은 방이면 캐시 반환 (DB 읽기 및 JSON 역직렬화 스킵)
            if (cachedRoute != null && cachedRoomId == roomId) {
                return Result.success(cachedRoute)
            }

            // 캐시 미스: DB에서 로드
            val routeDto = selectedRouteDataSource.getRoute(roomId)
            val route = routeDto?.toModel()

            // 캐시 업데이트
            cachedRoute = route
            cachedRoomId = roomId

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

            // 캐시 클리어
            cachedRoute = null
            cachedRoomId = null

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
