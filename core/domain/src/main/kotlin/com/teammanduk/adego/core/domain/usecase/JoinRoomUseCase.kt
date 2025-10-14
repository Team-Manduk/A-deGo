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
    operator fun invoke(roomId: String): Flow<Pair<Room?, List<Participant>>> {
        roomRepository.setCurrentRoom(roomId)

        return combine(
            roomRepository.observeCurrentRoom(),
            roomRepository.observeCurrentParticipants()
        ) { room, participants ->
            room to participants
        }
    }
}
