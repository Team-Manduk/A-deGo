package com.teammanduk.adego.core.domain.repository

import com.teammanduk.adego.core.model.Participant
import com.teammanduk.adego.core.model.ParticipantLocation
import com.teammanduk.adego.core.model.ParticipantRoute
import com.teammanduk.adego.core.model.Room
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    // ===== 세션 관리 =====
    /**
     * 현재 참여 중인 방 설정 (세션 시작)
     */
    fun setCurrentRoom(roomId: String)

    /**
     * 현재 참여 중인 방 ID 조회
     */
    fun getCurrentRoomId(): String?

    /**
     * 현재 방 세션 종료
     */
    fun clearCurrentRoom()

    // ===== 현재 방 기준 작업 (세션 기반) =====
    /**
     * 현재 방 정보 실시간 구독
     */
    fun observeCurrentRoom(): Flow<Room?>

    /**
     * 현재 방 참여자 목록 실시간 구독
     */
    fun observeCurrentParticipants(): Flow<List<Participant>>

    /**
     * 현재 방에 내 위치 업데이트
     */
    suspend fun updateMyLocation(
        userId: String,
        location: ParticipantLocation
    ): Result<Unit>

    /**
     * 현재 방에 내 경로 업데이트
     */
    suspend fun updateMyRoute(
        userId: String,
        route: ParticipantRoute
    ): Result<Unit>

    /**
     * 현재 방 나가기
     */
    suspend fun leaveCurrentRoom(userId: String): Result<Unit>

    // ===== 특정 방 지정 작업 (레거시/호환성) =====
    /**
     * 방 생성 (고유 코드 자동 생성)
     */
    suspend fun createRoom(
        roomName: String,
        destination: com.teammanduk.adego.core.model.Place,
        dateTime: String,
        userId: String,
        userName: String,
        profileColor: String
    ): Result<String> // roomId 반환

    /**
     * 초대 코드로 방 참여
     */
    suspend fun joinRoom(
        roomId: String,
        userId: String,
        userName: String,
        profileColor: String
    ): Result<Unit>

    /**
     * 방 정보 조회 (1회)
     */
    suspend fun getRoomInfo(roomId: String): Result<Room?>

    /**
     * 방 정보 실시간 구독 (특정 roomId 지정)
     */
    fun observeRoom(roomId: String): Flow<Room?>

    /**
     * 참여자 목록 실시간 구독 (특정 roomId 지정)
     */
    fun observeParticipants(roomId: String): Flow<List<Participant>>

    /**
     * 내 위치 업데이트 (특정 roomId 지정)
     */
    suspend fun updateMyLocation(
        roomId: String,
        userId: String,
        location: ParticipantLocation
    ): Result<Unit>

    /**
     * 내 경로 업데이트 (특정 roomId 지정)
     */
    suspend fun updateMyRoute(
        roomId: String,
        userId: String,
        route: ParticipantRoute
    ): Result<Unit>

    /**
     * 방 나가기 (특정 roomId 지정)
     */
    suspend fun leaveRoom(roomId: String, userId: String): Result<Unit>
}
