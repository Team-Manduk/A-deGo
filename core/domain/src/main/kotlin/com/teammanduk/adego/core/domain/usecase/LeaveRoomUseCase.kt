package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.LocationRepository
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 방 나가기 UseCase
 *
 * 사용자가 명시적으로 방에서 나갈 때 호출:
 * 1. 위치 추적 중지
 * 2. Firebase에서 참가자 제거
 * 3. 로컬 세션 정리
 */
class LeaveRoomUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("사용자 정보가 없습니다."))

            // 1. 먼저 위치 추적 중지 (새로운 업데이트 방지)
            locationRepository.stopLocationTracking()

            // 2. 위치 상태 클리어 (마지막 위치 캐시 제거)
            locationRepository.clearLocationState()

            // 3. Firebase에서 참가자 제거 (leaveCurrentRoom 내부에서 clearCurrentRoom 호출됨)
            val result = roomRepository.leaveCurrentRoom(userId)
            if (result.isFailure) {
                return result
            }

            // 4. 현재 사용자 세션 클리어
            userRepository.clearCurrentUser()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
