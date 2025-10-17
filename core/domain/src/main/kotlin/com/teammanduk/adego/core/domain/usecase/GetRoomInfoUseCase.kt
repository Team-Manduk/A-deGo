package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.model.Room
import javax.inject.Inject

/**
 * 방 정보 조회 UseCase
 *
 * 책임:
 * - RoomRepository를 통한 방 정보 조회
 * - 방 존재 여부 검증
 *
 * @return Result<Room?> 성공 시 Room 또는 null, 실패 시 Exception
 */
class GetRoomInfoUseCase @Inject constructor(
    private val roomRepository: RoomRepository
) {
    suspend operator fun invoke(roomId: String): Result<Room?> {
        return try {
            require(roomId.isNotBlank()) { "방 ID는 비어있을 수 없습니다" }
            roomRepository.getRoomInfo(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
