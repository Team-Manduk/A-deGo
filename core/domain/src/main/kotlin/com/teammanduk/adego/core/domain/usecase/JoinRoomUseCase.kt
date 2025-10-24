package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class JoinRoomUseCase @Inject constructor(
    private val roomRepository: RoomRepository
) {
    operator fun invoke(
        roomId: String,
        userId: String,
        userName: String
    ): Flow<Pair<Room?, List<Participant>>> {
        return kotlinx.coroutines.flow.flow {
            // 현재 방 세션 설정
            roomRepository.setCurrentRoom(roomId)

            // 먼저 방에 참가 (참가자 생성)
            roomRepository.joinRoom(roomId, userId, userName)
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
