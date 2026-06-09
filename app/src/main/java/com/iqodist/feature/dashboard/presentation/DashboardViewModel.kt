package com.iqodist.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import com.iqodist.core.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    val userRole = sessionManager.userRole
    val userName = sessionManager.userName
}