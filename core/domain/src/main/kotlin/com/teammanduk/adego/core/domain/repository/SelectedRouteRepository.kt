package com.teammanduk.adego.core.domain.repository

import com.teammanduk.adego.core.model.Route

/**
 * 선택된 경로 저장소 Repository 인터페이스
 */
interface SelectedRouteRepository {
    /**
     * 현재 방의 선택된 경로 저장
     */
    suspend fun saveSelectedRoute(route: Route): Result<Unit>

    /**
     * 현재 방의 선택된 경로 조회
     */
    suspend fun getSelectedRoute(): Result<Route?>

    /**
     * 현재 방의 선택된 경로 삭제
     */
    suspend fun deleteSelectedRoute(): Result<Unit>
}
