package com.iqodist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.iqodist.core.data.local.SessionManager
import com.iqodist.core.ui.theme.IqodistTheme
import com.iqodist.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IqodistTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController  = navController,
                    sessionManager = sessionManager
                )
            }
        }
    }
}
