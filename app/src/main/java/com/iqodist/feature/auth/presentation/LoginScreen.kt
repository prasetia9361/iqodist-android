package com.iqodist.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    onLoginSuccess: (role: String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess((uiState as LoginUiState.Success).role)
            viewModel.resetState()
        }
    }

    // Scaffold memberi struktur dasar layar
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .imePadding(),  // layar naik saat keyboard muncul
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header ──────────────────────────────────────────────────
            Text(
                text  = "IQODIST",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "Sistem Distribusi & POS Internal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Field Username ───────────────────────────────────────────
            OutlinedTextField(
                value         = viewModel.username,
                onValueChange = viewModel::onUsernameChange,
                label         = { Text("Username") },
                leadingIcon   = {
                    Icon(
                        imageVector        = Icons.Default.Person,
                        contentDescription = "Username"
                    )
                },
                singleLine    = true,
                isError       = uiState is LoginUiState.Error,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction    = ImeAction.Next  // tombol Next di keyboard
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Field Password ───────────────────────────────────────────
            OutlinedTextField(
                value         = viewModel.password,
                onValueChange = viewModel::onPasswordChange,
                label         = { Text("Password") },
                leadingIcon   = {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = "Password"
                    )
                },
                trailingIcon  = {
                    // Tombol show/hide password
                    IconButton(onClick = viewModel::toggelPasswordVisibility) {
                        Icon(
                            imageVector = if (viewModel.isPasswordVisible)
                                Icons.Default.Lock
                            else
                                Icons.Default.Search,
                            contentDescription = if (viewModel.isPasswordVisible)
                                "Sembunyikan password"
                            else
                                "Tampilkan password"
                        )
                    }
                },
                // Sembunyikan teks password kecuali isPasswordVisible = true
                visualTransformation = if (viewModel.isPasswordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                singleLine    = true,
                isError       = uiState is LoginUiState.Error,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done  // tombol Done di keyboard
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.login()  // langsung login saat tekan Done
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // ── Pesan Error ──────────────────────────────────────────────
            if (uiState is LoginUiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = (uiState as LoginUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Tombol Login ─────────────────────────────────────────────
            Button(
                onClick  = viewModel::login,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                // Disable tombol saat loading agar tidak bisa double-submit
                enabled  = uiState !is LoginUiState.Loading
            ) {
                if (uiState is LoginUiState.Loading) {
                    // Tampilkan loading spinner
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Memproses...")
                } else {
                    Text("Masuk")
                }
            }
        }
    }
}
