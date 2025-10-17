package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.service.ColorGenerator
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

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
