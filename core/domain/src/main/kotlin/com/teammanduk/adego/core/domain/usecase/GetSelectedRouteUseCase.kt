package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.SelectedRouteRepository
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject

/**
 * 저장된 선택된 경로를 가져오는 UseCase
 *
 * Room DB에 저장된 상세 경로 정보를 조회합니다.
 */
class GetSelectedRouteUseCase @Inject constructor(
    private val selectedRouteRepository: SelectedRouteRepository
) {
    /**
     * 저장된 경로 조회
     *
     * @param roomId 방 ID (현재는 사용하지 않지만 향후 확장 가능)
     * @param userId 사용자 ID (현재는 사용하지 않지만 향후 확장 가능)
     * @return 저장된 경로 (없으면 null)
     */
    suspend operator fun invoke(roomId: String, userId: String): Result<Route?> {
        return try {
            selectedRouteRepository.getSelectedRoute()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
