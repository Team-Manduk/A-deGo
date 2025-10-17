package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.service.ColorGenerator
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * 방 참가 UseCase (Facade Pattern)
 *
 * 책임:
 * - 방 참가 프로세스 통합 관리
 * - 사용자 프로필 색상 생성
 * - 방 참가 및 실시간 구독 조율
 *
 * 이 UseCase는 여러 비즈니스 로직을 조합하는 Facade UseCase입니다:
 * 1. 프로필 색상 생성 (ColorGenerator)
 * 2. 방 참가 (RoomRepository.joinRoom)
 * 3. 현재 방 세션 설정 (RoomRepository.setCurrentRoom)
 * 4. 방 및 참가자 정보 실시간 구독
 *
 * @param roomId 참가할 방 ID
 * @param userId 사용자 ID
 * @param userName 사용자 이름
 * @return Flow<Pair<Room?, List<Participant>>> 방 정보와 참가자 목록의 실시간 스트림
 */
class JoinRoomUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val colorGenerator: ColorGenerator
) {
    operator fun invoke(
        roomId: String,
        userId: String,
        userName: String
    ): Flow<Pair<Room?, List<Participant>>> {
        roomRepository.setCurrentRoom(roomId)

        return kotlinx.coroutines.flow.flow {
            // 사용자 색상 생성
            val profileColor = colorGenerator.generateColorFromUserId(userId)

            // 먼저 방에 참가 (참가자 생성)
            roomRepository.joinRoom(roomId, userId, userName, profileColor)
                .onFailure { error ->
                    throw error
                }

            // 그 다음 실시간 구독 시작
            combine(
                roomRepository.observeCurrentRoom(),
                roomRepository.observeCurrentParticipants()
            ) { room, participants ->
                room to participants
            }.collect { data ->
                emit(data)
            }
        }
    }
}
