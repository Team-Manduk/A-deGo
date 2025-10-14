package com.teammanduk.adego.core.domain.repository

interface UserRepository {
    fun setCurrentUser(userId: String)
    fun getCurrentUserId(): String
    fun clearCurrentUser()
}
