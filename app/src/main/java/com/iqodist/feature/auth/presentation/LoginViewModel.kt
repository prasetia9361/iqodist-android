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

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(newValue: String) {
        username = newValue

        if (_uiState.value is LoginUiState.Error){
            _uiState.value = LoginUiState.Idle
        }
    }

    fun onPasswordChange(newValue: String){
        password = newValue
        if (_uiState.value is LoginUiState.Error){
            _uiState.value = LoginUiState.Idle
        }
    }

    fun toggelPasswordVisibility(){
        isPasswordVisible = !isPasswordVisible
    }

    fun login() {
        if (_uiState.value is LoginUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            val result = loginUseCase(username = username.trim(), password = password)

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
}

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()

    data class Success(val role: String): LoginUiState()

    data class Error(val message: String): LoginUiState()
}