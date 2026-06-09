package com.iqodist.feature.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
    @SerialName("entity_id") val entityId: String
)

@Serializable
data class LoginResponseDto(
    @SerialName("access_token") val accessToken:String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("user_role") val userRole: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("entity_name") val entityName: String
)

@Serializable
data class ApiErrorDto(
    val message: String,
    val code: String? = null
)