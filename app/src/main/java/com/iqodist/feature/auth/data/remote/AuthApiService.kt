package com.iqodist.feature.auth.data.remote

import com.iqodist.feature.auth.data.dto.LoginRequestDto
import com.iqodist.feature.auth.data.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): LoginResponseDto

    @POST("auth/logout")
    suspend fun logout()
}
