package com.iqodist.feature.auth.domain.usecase

import com.iqodist.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject


class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        username: String,
        password: String,
        entityId: String = "HQ"
    ): Result<String> {
        if (username.isBlank()) {
            return Result.failure(Exception("username ttidak boleh kosong"))
        }
        if (username.length < 3) {
            return Result.failure(Exception("Username minimal 3 karakter"))
        }
        if (password.isBlank()){
            return Result.failure(Exception("Password tidak boleh kosong"))
        }
        if (password.length < 6){
            return Result.failure(Exception("Password minimal 6 karakter"))
        }

        return repository.login(username,password,entityId)
    }
}