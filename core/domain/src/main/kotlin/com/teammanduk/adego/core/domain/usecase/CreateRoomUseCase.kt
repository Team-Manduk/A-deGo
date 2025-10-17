package com.teammanduk.adego.core.domain.usecase

import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.domain.service.ColorGenerator
import com.teammanduk.adego.core.model.Place
import javax.inject.Inject

/**
 * 방 생성 UseCase
 *
 * 책임:
 * - 방 생성 요청 검증
 * - 사용자 프로필 색상 생성
 * - RoomRepository를 통한 방 생성
 *
 * @return Result<String> 성공 시 생성된 roomId, 실패 시 Exception
 */
class CreateRoomUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val colorGenerator: ColorGenerator
) {
    suspend operator fun invoke(
        roomName: String,
        destination: Place,
        dateTime: String,
        userId: String,
        userName: String
    ): Result<String> {
        return try {
            // 입력 검증
            require(roomName.isNotBlank()) { "방 이름은 비어있을 수 없습니다" }
            require(userName.isNotBlank()) { "사용자 이름은 비어있을 수 없습니다" }
            require(userId.isNotBlank()) { "사용자 ID는 비어있을 수 없습니다" }
            require(dateTime.isNotBlank()) { "날짜/시간은 비어있을 수 없습니다" }

            // 사용자 프로필 색상 생성
            val profileColor = colorGenerator.generateColorFromUserId(userId)

            // 방 생성
            roomRepository.createRoom(
                roomName = roomName,
                destination = destination,
                dateTime = dateTime,
                userId = userId,
                userName = userName,
                profileColor = profileColor
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
