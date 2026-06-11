package com.iqodist.feature.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqodist.feature.auth.domain.repository.AuthRepository
import com.iqodist.feature.auth.domain.usecase.LoginUseCase
import com.iqodist.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private  set

    var isPasswordVisible by mutableStateOf(false)
        private set

    var selectedEntityId by mutableStateOf("0")
        private set

    var selectedEntityName by mutableStateOf("Pilih Cabang / Lokasi")
        private set

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val entityOptions: List<EntityOption> = listOf(
        EntityOption("HQ", "Head Quarter (HQ)"),
        EntityOption("Cab_jakarta", "Cabang Jakarta"),
        EntityOption("cab_surabaya", "Cabang Surabaya"),
        EntityOption("cab_bandung", "Cabang Bandung"),
        EntityOption("cab_medan", "Cabang Medan")
    )

    fun onUsernameChange(newValue: String) {
        username = newValue
        clearErrorIfAny()
    }

    fun onPasswordChange(newValue: String){
        password = newValue
        clearErrorIfAny()
    }

    fun toggelPasswordVisibility(){
        isPasswordVisible = !isPasswordVisible
    }

    fun onEntitySelected(option: EntityOption){
        selectedEntityId = option.id
        selectedEntityName = option.name
        clearErrorIfAny()
    }

    fun login() {
        if (_uiState.value is LoginUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            val result = loginUseCase(
                username = username.trim(),
                password = password,
                entityId = selectedEntityId
            )

            _uiState.value = result.fold(
                onSuccess = {role -> LoginUiState.Success(role = role)
                },
                onFailure = { exception -> LoginUiState.Error(message = exception.message ?: "Login gagal!")
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = LoginUiState.Idle
        }
    }

    fun resetState(){
        _uiState.value = LoginUiState.Idle
    }

    private fun clearErrorIfAny() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }
}

data class EntityOption(
    val id: String,
    val name: String
)

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()

    data class Success(val role: String): LoginUiState()

    data class Error(val message: String): LoginUiState()
}