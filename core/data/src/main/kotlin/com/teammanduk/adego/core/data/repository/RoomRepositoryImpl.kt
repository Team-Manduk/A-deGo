package com.teammanduk.adego.core.data.repository

import android.util.Log
import com.teammanduk.adego.core.data.mapper.toDto
import com.teammanduk.adego.core.data.mapper.toModel
import com.teammanduk.adego.core.data_api.datasource.RoomDataSource
import com.teammanduk.adego.core.data_api.model.ParticipantDto
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.core.model.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val roomDataSource: RoomDataSource
) : RoomRepository {

    /**
     * userId를 기반으로 고유한 색상을 생성
     * 같은 userId는 항상 같은 색상을 반환
     */
    private fun generateColorFromUserId(userId: String): String {
        // userId의 해시코드 생성
        val hash = userId.hashCode()

        // HSV 색상 공간 사용 (Hue, Saturation, Value)
        // Hue: 0-360 범위로 매핑하여 다양한 색상 생성
        val hue = (hash.toFloat().rem(360f) + 360f).rem(360f)

        // 채도와 명도를 고정하여 선명하고 보기 좋은 색상 생성
        val saturation = 0.7f  // 70% 채도
        val value = 0.9f       // 90% 명도

        // HSV를 RGB로 변환
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))

        // #RRGGBB 형식으로 반환
        return String.format("#%06X", 0xFFFFFF and rgb)
    }

    override suspend fun createRoom(
        roomName: String,
        destination: Place,
        dateTime: String,
        userId: String,
        userName: String
    ): Result<String> {
        return try {
            Log.d(TAG, "[Repository] 방 생성 시작")

            // 고유 코드 생성
            val roomId = roomDataSource.generateUniqueRoomId()
            Log.d(TAG, "[Repository] 고유 roomId 생성: $roomId")

            // Room 생성
            val roomDto = Room(
                roomId = roomId,
                roomName = roomName,
                destination = destination,
                dateTime = dateTime,
                createdBy = userId,
                createdAt = System.currentTimeMillis()
            ).toDto()

            Log.d(TAG, "[Repository] Firebase에 Room 데이터 저장 중...")
            roomDataSource.createRoom(roomDto).getOrThrow()
            Log.d(TAG, "[Repository] Room 데이터 저장 완료")

            // 생성자를 첫 번째 참여자로 추가
            val profileColor = generateColorFromUserId(userId)
            Log.d(TAG, "[Repository] userId=$userId -> profileColor=$profileColor")

            val creatorParticipant = ParticipantDto(
                userId = userId,
                name = userName,
                profileColor = profileColor,
                location = null,
                route = null
            )

            Log.d(TAG, "[Repository] 생성자를 참여자로 추가 중...")
            roomDataSource.addParticipant(roomId, creatorParticipant).getOrThrow()
            Log.d(TAG, "[Repository] 참여자 추가 완료")

            Log.d(TAG, "[Repository] 방 생성 완료! roomId=$roomId")
            Result.success(roomId)
        } catch (e: Exception) {
            Log.e(TAG, "[Repository] 방 생성 실패", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AdegoRoom"
    }

    override suspend fun joinRoom(
        roomId: String,
        userId: String,
        userName: String
    ): Result<Unit> {
        return try {
            // 방 존재 여부 확인
            val room = roomDataSource.getRoom(roomId).getOrNull()
                ?: return Result.failure(Exception("Room not found"))

            // 참여자 추가
            val participant = ParticipantDto(
                userId = userId,
                name = userName,
                profileColor = generateColorFromUserId(userId),
                location = null,
                route = null
            )

            roomDataSource.addParticipant(roomId, participant)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRoomInfo(roomId: String): Result<Room?> {
        return try {
            val roomDto = roomDataSource.getRoom(roomId).getOrNull()
            Result.success(roomDto?.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeRoom(roomId: String): Flow<Room?> {
        return roomDataSource.observeRoom(roomId)
            .map { it?.toModel() }
    }

    override fun observeParticipants(roomId: String): Flow<List<Participant>> {
        return roomDataSource.observeParticipants(roomId)
            .map { participants ->
                participants.map { it.toModel() }
            }
    }

    override suspend fun updateMyLocation(
        roomId: String,
        userId: String,
        location: ParticipantLocation
    ): Result<Unit> {
        return roomDataSource.updateParticipantLocation(
            roomId = roomId,
            userId = userId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = location.updatedAt
        )
    }

    override suspend fun updateMyRoute(
        roomId: String,
        userId: String,
        route: ParticipantRoute
    ): Result<Unit> {
        return roomDataSource.updateParticipantRoute(
            roomId = roomId,
            userId = userId,
            eta = route.eta,
            distance = route.distance,
            polyline = route.polyline,
            durationInSeconds = route.durationInSeconds,
            distanceInMeters = route.distanceInMeters,
            timestamp = route.updatedAt
        )
    }

    override suspend fun leaveRoom(roomId: String, userId: String): Result<Unit> {
        return roomDataSource.removeParticipant(roomId, userId)
    }
}
