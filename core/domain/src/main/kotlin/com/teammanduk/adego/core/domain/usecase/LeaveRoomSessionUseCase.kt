package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 방 세션 종료 UseCase
 *
 * 현재 참여 중인 방 세션을 정리하고 종료:
 * 1. 위치 추적 중지
 * 2. 현재 방 세션 클리어
 * 3. 현재 사용자 세션 클리어
 *
 * 주로 화면을 벗어날 때(onCleared) 호출됨
 */
class LeaveRoomSessionUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke() {
        locationRepository.stopLocationTracking()
        roomRepository.clearCurrentRoom()
        userRepository.clearCurrentUser()
    }
}
