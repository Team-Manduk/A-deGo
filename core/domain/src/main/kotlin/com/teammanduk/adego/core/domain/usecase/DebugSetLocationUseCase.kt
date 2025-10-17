package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject

/**
 * 디버그 모드에서 지도 클릭으로 위치를 설정하는 UseCase
 *
 * 책임:
 * - 디버그 위치 생성 (ParticipantLocation)
 * - 위치 및 ETA 업데이트 (디바운싱 없이 즉시 실행)
 */
class DebugSetLocationUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val calculateEtaUseCase: CalculateEtaUseCase
) {
    /**
     * @param userId 사용자 ID
     * @param latitude 위도
     * @param longitude 경도
     * @param selectedRoute 선택된 경로 (null이면 ETA 계산 안 함)
     * @return 성공 여부
     */
    suspend operator fun invoke(
        userId: String,
        latitude: Double,
        longitude: Double,
        selectedRoute: Route?
    ): Result<Unit> {
        return try {
            // 디버그 위치 생성
            val debugLocation = ParticipantLocation(
                latitude = latitude,
                longitude = longitude,
                updatedAt = System.currentTimeMillis(),
                accuracy = 1.0f
            )

            // 1. 위치 업데이트
            roomRepository.updateMyLocation(userId, debugLocation)

            // 2. 선택된 경로가 있으면 ETA 계산 및 업데이트
            selectedRoute?.let { route ->
                val etaResult = calculateEtaUseCase(route, debugLocation)

                etaResult?.let { result ->
                    val participantRoute = ParticipantRoute(
                        durationInSeconds = result.remainingTimeInSeconds,
                        distanceInMeters = result.remainingDistanceInMeters.toInt(),
                        polyline = "",
                        updatedAt = System.currentTimeMillis(),
                        currentSubPathIndex = result.currentSubPathIndex,
                        progressInCurrentSubPath = result.progressInCurrentSubPath
                    )

                    roomRepository.updateMyRoute(userId, participantRoute)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
