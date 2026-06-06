package com.iqodist.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqodist.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            // TODO: Ganti dengan panggilan LoginUseCase -> Retrofit
            sessionManager.saveSession(
                accessToken  = "dummy_token",
                refreshToken = "dummy_refresh",
                userId       = "usr_001",
                userName     = username,
                userRole     = "KASIR",
                entityId     = "ent_001"
            )
            _loginState.value = LoginState.Success(role = "KASIR")
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _loginState.value = LoginState.Idle
        }
    }
}

sealed class LoginState {
    data object Idle    : LoginState()
    data object Loading : LoginState()
    data class  Success(val role: String) : LoginState()
    data class  Error(val message: String) : LoginState()
}
