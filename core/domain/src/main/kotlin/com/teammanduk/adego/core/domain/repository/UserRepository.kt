package com.teammanduk.adego.core.domain.repository

interface UserRepository {
    /**
     * 현재 사용자 설정 (세션 시작)
     */
    suspend fun setCurrentUser(userId: String)

    /**
     * 현재 사용자 ID 조회
     */
    fun getCurrentUserId(): String

    /**
     * 현재 사용자 세션 종료
     */
    suspend fun clearCurrentUser()

    /**
     * DataStore에서 저장된 사용자 ID 복구
     * @return 저장된 userId (없으면 null)
     */
    suspend fun restoreUserSession(): String?

    /**
     * DataStore에서 저장된 사용자 이름 복구
     * @return 저장된 userName (없으면 null)
     */
    suspend fun getUserName(): String?
}
