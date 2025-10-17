package com.teammanduk.adego.core.data_api.datasource

/**
 * 세션 로컬 저장소
 *
 * 사용자 ID, 방 정보, 사용자 이름을 로컬에 저장하여
 * 앱 재시작 시에도 세션을 유지할 수 있도록 함
 */
interface SessionDataSource {
    /**
     * 현재 사용자 ID 저장
     */
    suspend fun saveUserId(userId: String)

    /**
     * 현재 사용자 ID 조회
     */
    suspend fun getUserId(): String?

    /**
     * 현재 사용자 이름 저장
     */
    suspend fun saveUserName(userName: String)

    /**
     * 현재 사용자 이름 조회
     */
    suspend fun getUserName(): String?

    /**
     * 현재 방 ID 저장
     */
    suspend fun saveRoomId(roomId: String)

    /**
     * 현재 방 ID 조회
     */
    suspend fun getRoomId(): String?

    /**
     * 현재 방 이름 저장
     */
    suspend fun saveRoomName(roomName: String)

    /**
     * 현재 방 이름 조회
     */
    suspend fun getRoomName(): String?

    /**
     * 모든 세션 정보 삭제
     */
    suspend fun clearSession()
}
