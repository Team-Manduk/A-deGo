package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.model.ParticipantLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * 내 위치를 다른 참가자들에게 실시간으로 방송(Broadcast)하는 UseCase
 *
 * 동작:
 * 1. 현재 위치를 먼저 가져옴 (Repository에서 재시도 처리)
 * 2. 이후 실시간으로 위치 업데이트를 계속 받음 (5초 주기)
 * 3. 받은 모든 위치를 Firebase에 자동 업데이트 (정책 적용)
 * 4. UI에는 위치 정보를 전달해서 지도에 표시
 *
 * UI는 위치를 화면에 표시하기 위해서만 사용하고,
 * Firebase 업데이트는 내부적으로 자동 처리됨
 */
class BroadcastLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val updateLocation: UpdateLocationUseCase
) {
    operator fun invoke(): Flow<ParticipantLocation> {
        return locationRepository.observeLocationUpdates()
            .onEach { location ->
                // 위치 수집 시마다 업데이트 (정책은 UpdateLocationUseCase에서 판단)
                updateLocation(location)
            }
    }
}
