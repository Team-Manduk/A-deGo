package com.teammanduk.adego.core.data_api.datasource

import com.teammanduk.adego.core.data_api.model.RouteDto

/**
 * 선택된 경로 로컬 저장소
 *
 * 방별로 선택된 경로를 Room DB에 저장/조회/삭제
 */
interface SelectedRouteDataSource {
    /**
     * 방의 선택된 경로 저장
     *
     * @param roomId 방 ID
     * @param route 저장할 경로 DTO
     */
    suspend fun saveRoute(roomId: String, route: RouteDto)

    /**
     * 방의 선택된 경로 조회
     *
     * @param roomId 방 ID
     * @return 저장된 경로 DTO, 없으면 null
     */
    suspend fun getRoute(roomId: String): RouteDto?

    /**
     * 방의 선택된 경로 삭제
     *
     * @param roomId 방 ID
     */
    suspend fun deleteRoute(roomId: String)

    /**
     * 모든 저장된 경로 삭제
     */
    suspend fun deleteAllRoutes()
}
