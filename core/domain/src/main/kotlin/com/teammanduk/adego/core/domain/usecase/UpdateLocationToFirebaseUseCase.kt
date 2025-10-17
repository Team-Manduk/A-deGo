package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import com.teammanduk.adego.core.model.ParticipantLocation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase에 위치 업데이트 UseCase
 * - 위치 정보를 받아서 업데이트 정책에 따라 Firebase에 저장 여부 결정
 * - 위치 수집과 분리되어 업데이트 정책을 독립적으로 제어
 * - UserRepository를 통해 현재 사용자 ID를 자동으로 가져옴
 */
@Singleton
class UpdateLocationToFirebaseUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository
) {
    private var lastUpdateTime: Long = 0
    private var lastLocation: ParticipantLocation? = null

    /**
     * 위치 업데이트 실행
     * @param location 업데이트할 위치 정보
     * @return true: 업데이트 성공, false: 정책에 의해 스킵됨
     */
    suspend operator fun invoke(location: ParticipantLocation): Boolean {
        if (!shouldUpdate(location)) {
            return false
        }

        // 현재 사용자 ID를 내부에서 가져옴
        val userId = userRepository.getCurrentUserId()
        roomRepository.updateMyLocation(userId, location)

        // 업데이트 후 상태 저장
        lastUpdateTime = System.currentTimeMillis()
        lastLocation = location

        return true
    }

    /**
     * 업데이트 여부 판단 로직
     *
     * TODO: 추후 정책 구현
     * - 시간 기반 필터링
     * - 거리 기반 필터링
     * - 배터리 상태 기반 필터링
     * - 목적지 거리 기반 필터링
     */
    private fun shouldUpdate(location: ParticipantLocation): Boolean {
        // 현재는 모든 위치 업데이트를 허용 (FusedLocationProvider 5초 주기)
        return true
    }
}
