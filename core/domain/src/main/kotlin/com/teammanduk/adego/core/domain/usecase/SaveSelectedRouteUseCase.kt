package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject

/**
 * 선택한 대중교통 경로를 서버에 저장하는 UseCase
 *
 * 경로 선택 화면에서 사용자가 경로를 확정하면
 * 해당 경로 정보를 Firebase에 저장하여
 * 다른 참여자들이 볼 수 있도록 합니다.
 */
class SaveSelectedRouteUseCase @Inject constructor(
    private val roomRepository: RoomRepository
) {
    /**
     * @param userId 사용자 ID
     * @param selectedRoute 선택한 대중교통 경로
     * @param currentRoute 현재 ParticipantRoute (polyline, ETA 등 유지)
     */
    suspend operator fun invoke(
        userId: String,
        selectedRoute: Route,
        currentRoute: ParticipantRoute? = null
    ): Result<Unit> {
        // 현재 ParticipantRoute 정보 유지하면서 selectedRoute만 추가
        val updatedRoute = currentRoute?.copy(selectedRoute = selectedRoute) ?:ParticipantRoute(
            eta = "${selectedRoute.totalTime}분",
            distance = "${selectedRoute.totalDistance / 1000.0}km",
            polyline = "", // 실제 polyline은 UpdateLocationAndEtaUseCase에서 업데이트
            durationInSeconds = selectedRoute.totalTime * 60,
            distanceInMeters = selectedRoute.totalDistance,
            updatedAt = System.currentTimeMillis(),
            selectedRoute = selectedRoute
        )

        return roomRepository.updateMyRoute(
            userId = userId,
            route = updatedRoute
        )
    }
}
