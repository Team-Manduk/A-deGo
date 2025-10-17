package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject

/**
 * 사용자 위치를 업데이트하고 선택된 경로가 있으면 ETA를 계산하여 업데이트하는 UseCase
 *
 * 책임:
 * - 위치 정보 Firebase 업데이트
 * - ETA 계산 및 Firebase 업데이트
 * - 도메인 모델 생성 (ParticipantRoute)
 */
class UpdateLocationAndEtaUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val calculateEtaUseCase: CalculateEtaUseCase,
    private val formatEtaUseCase: FormatEtaUseCase
) {
    /**
     * @param userId 사용자 ID
     * @param location 현재 위치
     * @param selectedRoute 선택된 경로 (null이면 ETA 계산 안 함)
     * @return 성공 여부
     */
    suspend operator fun invoke(
        userId: String,
        location: ParticipantLocation,
        selectedRoute: Route?
    ): Result<Unit> {
        return try {
            // 1. 위치 업데이트
            roomRepository.updateMyLocation(userId, location)

            // 2. 선택된 경로가 있으면 ETA 계산 및 업데이트
            selectedRoute?.let { route ->
                val etaResult = calculateEtaUseCase(route, location)

                etaResult?.let { result ->
                    // 포맷팅
                    val formatted = formatEtaUseCase(
                        remainingTimeInSeconds = result.remainingTimeInSeconds,
                        remainingDistanceInMeters = result.remainingDistanceInMeters
                    )

                    // ParticipantRoute 생성
                    val participantRoute = ParticipantRoute(
                        eta = formatted.timeString,
                        distance = formatted.distanceString,
                        polyline = "",
                        durationInSeconds = result.remainingTimeInSeconds,
                        distanceInMeters = result.remainingDistanceInMeters.toInt(),
                        updatedAt = System.currentTimeMillis(),
                        currentSubPathIndex = result.currentSubPathIndex,
                        progressInCurrentSubPath = result.progressInCurrentSubPath
                    )

                    // Firebase에 ETA 업데이트
                    roomRepository.updateMyRoute(userId, participantRoute)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
