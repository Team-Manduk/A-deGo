package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.SelectedRouteRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.domain.util.PolylineEncoder
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject

/**
 * 선택한 대중교통 경로를 저장하는 UseCase
 *
 * 1. Room DB에 상세 경로 저장 (SubPath, graphicData 등)
 * 2. 경로를 polyline으로 인코딩
 * 3. Firebase ParticipantRoute에 polyline 저장
 *
 * ETA/거리는 UpdateLocationUseCase에서 위치 업데이트마다 계산됨.
 */
class SaveSelectedRouteUseCase @Inject constructor(
    private val selectedRouteRepository: SelectedRouteRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository
) {
    /**
     * 선택한 경로를 저장
     *
     * @param selectedRoute 선택한 대중교통 경로
     */
    suspend operator fun invoke(selectedRoute: Route): Result<Unit> {
        return try {
            // 1. Room DB에 상세 경로 저장
            selectedRouteRepository.saveSelectedRoute(selectedRoute).getOrThrow()

            // 2. 경로를 polyline으로 인코딩
            val polyline = PolylineEncoder.encode(selectedRoute)

            // 3. Firebase ParticipantRoute에 polyline 저장 (초기 ETA/거리는 경로 정보 사용)
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(IllegalStateException("User ID not found"))

            val initialRoute = ParticipantRoute(
                etaInSeconds = selectedRoute.totalTime,
                distanceInMeters = selectedRoute.totalDistance,
                polyline = polyline,
                updatedAt = System.currentTimeMillis()
            )

            roomRepository.updateMyRoute(userId, initialRoute).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
