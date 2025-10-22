package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.SelectedRouteRepository
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject

/**
 * 저장된 선택된 경로를 가져오는 UseCase
 *
 * 현재 세션의 Room DB에 저장된 상세 경로 정보를 조회합니다.
 */
class GetSelectedRouteUseCase @Inject constructor(
    private val selectedRouteRepository: SelectedRouteRepository
) {
    /**
     * 저장된 경로 조회
     *
     * @return 저장된 경로 (없으면 null)
     */
    suspend operator fun invoke(): Result<Route?> {
        return try {
            selectedRouteRepository.getSelectedRoute()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
