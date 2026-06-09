package com.iqodist.feature.auth.domain.repository

interface AuthRepository {
    suspend fun login(
        username: String,
        password: String,
        entityId: String
    ): Result<String>

    suspend fun logout()
}