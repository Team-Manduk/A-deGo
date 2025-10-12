package com.teammanduk.adego.core.domain.repository

import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Room
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    /**
     * 방 생성 (고유 코드 자동 생성)
     */
    suspend fun createRoom(
        roomName: String,
        destination: com.teammanduk.adego.core.model.Place,
        dateTime: String,
        userId: String,
        userName: String
    ): Result<String> // roomId 반환

    /**
     * 초대 코드로 방 참여
     */
    suspend fun joinRoom(
        roomId: String,
        userId: String,
        userName: String
    ): Result<Unit>

    /**
     * 방 정보 조회 (1회)
     */
    suspend fun getRoomInfo(roomId: String): Result<Room?>

    /**
     * 방 정보 실시간 구독
     */
    fun observeRoom(roomId: String): Flow<Room?>

    /**
     * 참여자 목록 실시간 구독
     */
    fun observeParticipants(roomId: String): Flow<List<Participant>>

    /**
     * 내 위치 업데이트
     */
    suspend fun updateMyLocation(
        roomId: String,
        userId: String,
        location: ParticipantLocation
    ): Result<Unit>

    /**
     * 내 경로 업데이트
     */
    suspend fun updateMyRoute(
        roomId: String,
        userId: String,
        route: ParticipantRoute
    ): Result<Unit>

    /**
     * 방 나가기
     */
    suspend fun leaveRoom(roomId: String, userId: String): Result<Unit>
}
