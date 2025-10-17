package com.teammanduk.adego.core.data_api.datasource

import com.teammanduk.adego.core.data_api.model.ParticipantDto
import com.teammanduk.adego.core.data_api.model.RoomDto
import kotlinx.coroutines.flow.Flow

interface RoomDataSource {
    /**
     * 방 생성
     */
    suspend fun createRoom(roomDto: RoomDto): Result<Unit>

    /**
     * 방 정보 조회
     */
    suspend fun getRoom(roomId: String): Result<RoomDto?>

    /**
     * 방 정보 실시간 구독
     */
    fun observeRoom(roomId: String): Flow<RoomDto?>

    /**
     * 참여자 추가
     */
    suspend fun addParticipant(roomId: String, participantDto: ParticipantDto): Result<Unit>

    /**
     * 참여자 목록 실시간 구독
     */
    fun observeParticipants(roomId: String): Flow<List<ParticipantDto>>

    /**
     * 참여자 위치 업데이트
     */
    suspend fun updateParticipantLocation(
        roomId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        timestamp: Long
    ): Result<Unit>

    /**
     * 참여자 경로 업데이트
     */
    suspend fun updateParticipantRoute(
        roomId: String,
        userId: String,
        durationInSeconds: Int,
        distanceInMeters: Int,
        polyline: String,
        timestamp: Long
    ): Result<Unit>

    /**
     * 참여자 제거
     */
    suspend fun removeParticipant(roomId: String, userId: String): Result<Unit>

    /**
     * 고유 코드 생성 (6자리 영숫자)
     */
    suspend fun generateUniqueRoomId(): String
}
