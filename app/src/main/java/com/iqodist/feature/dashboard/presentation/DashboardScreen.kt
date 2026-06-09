package com.iqodist.feature.dashboard.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqodist.core.domain.model.AppMenu
import com.iqodist.core.domain.model.UserRole
import com.iqodist.core.domain.model.getAllowedMenus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val userRole by viewModel.userRole.collectAsStateWithLifecycle(initialValue = null)
    val userName by viewModel.userName.collectAsStateWithLifecycle(initialValue = "")

    val allowedMenus = remember(userRole) {
        userRole?.let { UserRole.fromKey(it).getAllowedMenus() } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IQODIST") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Keluar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Sapaan user
            Text(
                text  = "Halo, $userName",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text  = userRole?.let { UserRole.fromKey(it).displayName } ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text  = "Menu",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Grid menu — tampilkan hanya menu yang diizinkan untuk role ini
            LazyVerticalGrid(
                columns      = GridCells.Fixed(2),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allowedMenus) { menu ->
                    MenuCard(
                        menu    = menu,
                        onClick = { onNavigate(menu.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuCard(
    menu: AppMenu,
    onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)  // kartu persegi
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = menu.label,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = menu.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
