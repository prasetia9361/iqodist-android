package com.iqodist.feature.auth.data.repository

import com.iqodist.core.data.local.SessionManager
import com.iqodist.feature.auth.data.dto.LoginRequestDto
import com.iqodist.feature.auth.data.remote.AuthApiService
import com.iqodist.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionManager: SessionManager
): AuthRepository {
    override suspend fun login(
        username: String,
        password: String,
        entityId: String
    ): Result<String> {
        return try {
            val response = apiService.login(
                LoginRequestDto(
                    username = username,
                    password = password,
                    entityId = entityId
                )
            )

            sessionManager.saveSession(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
                userId = response.userId,
                userName = response.userName,
                userRole = response.userRole,
                entityId = response.entityId
            )

            Result.success(response.userRole)

        } catch (e: retrofit2.HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Username atau Password salah"
                403 -> "Akun tidak memiliki akses"
                423 -> "Akun dikunci hubungi admin"
                500 -> "Server sedang bermasalah, coba lagi nanti"
                else -> "Login gagal (code: ${e.code()})"
            }
            Result.failure(Exception(errorMessage))
        } catch (e:java.net.SocketTimeoutException) {
            Result.failure(Exception("Koneksi timeout, periksa jaringan internet"))
        } catch (e:java.net.UnknownHostException){
            Result.failure(Exception("Tidak ada koneksi internet"))
        } catch (e: Exception) {
            Result.failure(Exception("terjadi kesalahan ${e.message}"))
        }
    }

    override suspend fun logout() {
        try {
            apiService.logout()
        } catch (e: Exception){

        }finally {
            sessionManager.clearSession()
        }
    }
}