package com.teammanduk.adego.core.data.repository

import android.util.Log
import com.teammanduk.adego.core.data.mapper.toDto
import com.teammanduk.adego.core.data.mapper.toModel
import com.teammanduk.adego.core.data_api.datasource.RoomDataSource
import com.teammanduk.adego.core.data_api.datasource.SessionDataSource
import com.teammanduk.adego.core.data_api.model.ParticipantDto
import com.teammanduk.adego.core.domain.repository.RoomRepository
import com.teammanduk.adego.core.model.MovementStatus
import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Place
import com.teammanduk.adego.core.model.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val roomDataSource: RoomDataSource,
    private val sessionDataSource: SessionDataSource
) : RoomRepository {

    // 현재 참여 중인 방 ID (세션 정보)
    private var currentRoomId: String? = null

    // 현재 방의 목적지 캐시 (observeRoom으로 자동 업데이트)
    private var cachedDestination: Place? = null

    // ===== 세션 관리 =====
    override fun setCurrentRoom(roomId: String) {
        currentRoomId = roomId
        runBlocking {
            sessionDataSource.saveRoomId(roomId)
            // roomName은 joinRoom이나 createRoom에서 저장
        }
        Log.d(TAG, "[Repository] 현재 방 세션 설정: $roomId")
    }

    override fun getCurrentRoomId(): String? = currentRoomId

    override fun clearCurrentRoom() {
        Log.d(TAG, "[Repository] 현재 방 세션 종료: $currentRoomId")
        currentRoomId = null
        cachedDestination = null
        runBlocking {
            sessionDataSource.clearSession()
        }
    }

    override fun getCachedDestination(): Place? = cachedDestination

    override suspend fun restoreRoomSession(): String? {
        val roomId = sessionDataSource.getRoomId()
        if (roomId != null) {
            currentRoomId = roomId
            Log.d(TAG, "[Repository] 방 세션 복구: $roomId")
        }
        return roomId
    }

    // ===== 현재 방 기준 작업 (세션 기반) =====
    override fun observeCurrentRoom(): Flow<Room?> {
        val roomId = currentRoomId
            ?: throw IllegalStateException("현재 방이 설정되지 않았습니다. setCurrentRoom()을 먼저 호출하세요.")
        return observeRoom(roomId)
    }

    override fun observeCurrentParticipants(): Flow<List<Participant>> {
        val roomId = currentRoomId
            ?: throw IllegalStateException("현재 방이 설정되지 않았습니다. setCurrentRoom()을 먼저 호출하세요.")
        return observeParticipants(roomId)
    }

    override suspend fun updateMyLocation(
        userId: String,
        location: ParticipantLocation
    ): Result<Unit> {
        Log.d("MapPerformance", "[Repository] updateMyLocation 시작 at ${System.currentTimeMillis()}")
        val roomId = currentRoomId
            ?: return Result.failure(IllegalStateException("현재 방이 설정되지 않았습니다."))
        val result = updateMyLocation(roomId, userId, location)
        Log.d("MapPerformance", "[Repository] updateMyLocation 완료 at ${System.currentTimeMillis()}")
        return result
    }

    override suspend fun updateMyRoute(
        userId: String,
        route: ParticipantRoute
    ): Result<Unit> {
        val roomId = currentRoomId
            ?: return Result.failure(IllegalStateException("현재 방이 설정되지 않았습니다."))
        return updateMyRoute(roomId, userId, route)
    }

    override suspend fun updateMyMovementStatus(
        userId: String,
        movementStatus: MovementStatus
    ): Result<Unit> {
        val roomId = currentRoomId
            ?: return Result.failure(IllegalStateException("현재 방이 설정되지 않았습니다."))
        return updateMyMovementStatus(roomId, userId, movementStatus)
    }

    override suspend fun updateMyLocationAndStatus(
        userId: String,
        location: ParticipantLocation,
        movementStatus: MovementStatus,
        etaInSeconds: Int?,
        distanceInMeters: Int?
    ): Result<Unit> {
        val roomId = currentRoomId
            ?: return Result.failure(IllegalStateException("현재 방이 설정되지 않았습니다."))

        return roomDataSource.updateParticipantLocationAndStatus(
            roomId = roomId,
            userId = userId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = location.updatedAt,
            movementStatus = movementStatus.name,
            etaInSeconds = etaInSeconds,
            distanceInMeters = distanceInMeters
        )
    }

    override suspend fun leaveCurrentRoom(userId: String): Result<Unit> {
        val roomId = currentRoomId
            ?: return Result.failure(IllegalStateException("현재 방이 설정되지 않았습니다."))
        val result = leaveRoom(roomId, userId)
        if (result.isSuccess) {
            clearCurrentRoom()
        }
        return result
    }

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

            // 세션에 사용자 정보 및 방 정보 저장
            sessionDataSource.saveUserName(userName)
            sessionDataSource.saveRoomName(roomName)

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
            Log.d(TAG, "[Repository] 방 참여 시작 - roomId: $roomId, userId: $userId")

            // 방 존재 여부 확인
            val room = roomDataSource.getRoom(roomId).getOrNull()
                ?: return Result.failure(Exception("Room not found"))

            // 참여자 색상 생성
            val profileColor = generateColorFromUserId(userId)
            Log.d(TAG, "[Repository] joinRoom - userId=$userId -> profileColor=$profileColor")

            // 참여자 추가
            val participant = ParticipantDto(
                userId = userId,
                name = userName,
                profileColor = profileColor,
                location = null,
                route = null
            )

            roomDataSource.addParticipant(roomId, participant).getOrThrow()

            // 세션에 사용자 정보 및 방 정보 저장
            sessionDataSource.saveUserName(userName)
            sessionDataSource.saveRoomName(room.roomName)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "[Repository] 방 참여 실패", e)
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
        Log.d(TAG, "[Repository] observeRoom() 호출됨 - roomId: $roomId")
        return roomDataSource.observeRoom(roomId)
            .map { roomDto ->
                Log.d(TAG, "[Repository] observeRoom - RoomDto 수신: ${roomDto?.roomId}")
                val room = roomDto?.toModel()

                // 🚀 최적화: 목적지를 캐시하여 getRoomInfo() 호출 제거
                if (room != null && roomId == currentRoomId) {
                    cachedDestination = room.destination
                    Log.d(TAG, "[Repository] observeRoom - 목적지 캐시 업데이트: ${room.destination.name}")
                }

                Log.d(
                    TAG,
                    "[Repository] observeRoom - Room 변환 완료: roomId=${room?.roomId}, destination=${room?.destination?.name}"
                )
                room
            }
    }

    override suspend fun getParticipants(roomId: String): Result<List<Participant>> {
        return try {
            val participantDtos = roomDataSource.getParticipants(roomId).getOrThrow()

            // Room 정보 가져오기 (거리 계산용)
            val roomDto = roomDataSource.getRoom(roomId).getOrNull()
            val destination = roomDto?.destination

            val participants = participantDtos.map { participantDto ->
                val participant = participantDto.toModel()

                // 목적지와 참가자 위치가 모두 있으면 거리 계산
                val location = participant.location
                val distance = if (destination != null && location != null) {
                    calculateHaversineDistance(
                        lat1 = location.latitude,
                        lon1 = location.longitude,
                        lat2 = destination.latitude,
                        lon2 = destination.longitude
                    )
                } else {
                    null
                }

                participant.copy(distanceToDestination = distance)
            }
            Result.success(participants)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get participants", e)
            Result.failure(e)
        }
    }

    override fun observeParticipants(roomId: String): Flow<List<Participant>> {
        // Room 정보와 참가자 정보를 combine하여 거리 계산
        return kotlinx.coroutines.flow.combine(
            roomDataSource.observeRoom(roomId),
            roomDataSource.observeParticipants(roomId)
        ) { roomDto, participantDtos ->
            val destination = roomDto?.destination

            participantDtos.map { participantDto ->
                val participant = participantDto.toModel()

                // 목적지와 참가자 위치가 모두 있으면 거리 계산
                val location = participant.location
                val distance = if (destination != null && location != null) {
                    calculateHaversineDistance(
                        lat1 = location.latitude,
                        lon1 = location.longitude,
                        lat2 = destination.latitude,
                        lon2 = destination.longitude
                    )
                } else {
                    null
                }

                participant.copy(distanceToDestination = distance)
            }
        }
    }

    /**
     * Haversine 공식을 사용한 두 지점 간 직선 거리 계산 (미터)
     */
    private fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        val earthRadiusMeters = 6371000.0 // 지구 반지름 (미터)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return (earthRadiusMeters * c).toInt()
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
            etaInSeconds = route.etaInSeconds,
            distanceInMeters = route.distanceInMeters,
            polyline = route.polyline,
            timestamp = route.updatedAt
        )
    }

    override suspend fun updateMyMovementStatus(
        roomId: String,
        userId: String,
        movementStatus: MovementStatus
    ): Result<Unit> {
        return roomDataSource.updateParticipantMovementStatus(
            roomId = roomId,
            userId = userId,
            movementStatus = movementStatus.name
        )
    }

    override suspend fun leaveRoom(roomId: String, userId: String): Result<Unit> {
        return roomDataSource.removeParticipant(roomId, userId)
    }
}
