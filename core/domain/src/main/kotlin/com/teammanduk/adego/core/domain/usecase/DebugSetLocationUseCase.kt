package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.Route
import javax.inject.Inject

/**
 * 디버그 모드에서 지도 클릭으로 위치를 설정하는 UseCase
 *
 * 책임:
 * - 디버그 위치 생성 (ParticipantLocation)
 * - 위치 및 ETA 업데이트 (UpdateLocationAndEtaUseCase 위임)
 */
class DebugSetLocationUseCase @Inject constructor(
    private val updateLocationAndEtaUseCase: UpdateLocationAndEtaUseCase
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
        // 디버그 위치 생성
        val debugLocation = ParticipantLocation(
            latitude = latitude,
            longitude = longitude,
            updatedAt = System.currentTimeMillis(),
            accuracy = 1.0f
        )

        // 위치 및 ETA 업데이트 (디바운싱 없이 즉시 실행)
        return updateLocationAndEtaUseCase(
            userId = userId,
            location = debugLocation,
            selectedRoute = selectedRoute
        )
    }
}
