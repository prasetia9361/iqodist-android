package com.iqodist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.iqodist.core.data.local.SessionManager
import com.iqodist.feature.auth.presentation.LoginScreen
import com.iqodist.feature.dashboard.presentation.DashboardScreen
import com.iqodist.feature.inventory.presentation.InventoryScreen
import com.iqodist.feature.pos.presentation.PosScreen
import com.iqodist.feature.sfa.presentation.SfaScreen

@Composable
fun AppNavGraph(navController: NavHostController, sessionManager: SessionManager)
{
    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)

    val startDestination = if (isLoggedIn) Route.Dashboard.path
                           else            Route.Login.path

    NavHost(navController    = navController, startDestination = startDestination)
    {
        // ── Auth ──────────────────────────────────────────────────────────
        composable(Route.Login.path) {
            LoginScreen(
                onLoginSuccess = { role -> navController.navigate(Route.Dashboard.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                }
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────────
        composable(Route.Dashboard.path) {
            DashboardScreen(
                onNavigate = { route ->
                    navController.navigate(route)
                },
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── POS ───────────────────────────────────────────────────────────
        composable(Route.Pos.path) {
            PosScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        // ── Inventory ─────────────────────────────────────────────────────
        composable(Route.Inventory.path) {
            InventoryScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }

        // ── SFA ───────────────────────────────────────────────────────────
        composable(Route.Sfa.path) {
            SfaScreen(
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}
