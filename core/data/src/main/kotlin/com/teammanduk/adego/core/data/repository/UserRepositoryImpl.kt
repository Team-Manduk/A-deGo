package com.teammanduk.adego.core.data.repository

import com.teammanduk.adego.core.data_api.datasource.SessionDataSource
import com.teammanduk.adego.core.domain.repository.UserRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val sessionDataSource: SessionDataSource
) : UserRepository {

    private var currentUserId: String? = null

    override fun setCurrentUser(userId: String) {
        currentUserId = userId
        runBlocking {
            sessionDataSource.saveUserId(userId)
        }
    }

    override fun getCurrentUserId(): String {
        return currentUserId ?: throw IllegalStateException("현재 사용자가 설정되지 않았습니다.")
    }

    override fun clearCurrentUser() {
        currentUserId = null
        runBlocking {
            sessionDataSource.clearSession()
        }
    }

    override suspend fun restoreUserSession(): String? {
        val userId = sessionDataSource.getUserId()
        if (userId != null) {
            currentUserId = userId
        }
        return userId
    }

    override suspend fun getUserName(): String? {
        return sessionDataSource.getUserName()
    }
}
