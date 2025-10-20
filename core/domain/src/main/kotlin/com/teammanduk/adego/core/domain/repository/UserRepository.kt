package com.teammanduk.adego.core.domain.repository

interface UserRepository {
    fun setCurrentUser(userId: String)
    fun getCurrentUserId(): String
    fun clearCurrentUser()

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
