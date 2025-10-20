package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.repository.UserRepository
import javax.inject.Inject

/**
 * 저장된 세션 정보 복구 UseCase
 *
 * DataStore에 저장된 세션 정보를 읽어서
 * UserRepository와 RoomRepository의 상태를 복원
 */
class RestoreSessionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val roomRepository: RoomRepository
) {
    /**
     * 세션 정보 복구
     *
     * @return SessionInfo? - 복구된 세션 정보 (없으면 null)
     */
    suspend operator fun invoke(): SessionInfo? {
        // Repository에서 세션 정보 복구
        val userId = userRepository.restoreUserSession()
        val roomId = roomRepository.restoreRoomSession()
        val userName = userRepository.getUserName()

        // 필수 정보가 모두 있는지 확인
        return if (userId != null && roomId != null && userName != null) {
            SessionInfo(
                userId = userId,
                roomId = roomId,
                userName = userName
            )
        } else {
            null
        }
    }
}

/**
 * 세션 정보 데이터 클래스
 */
data class SessionInfo(
    val userId: String,
    val roomId: String,
    val userName: String
)
