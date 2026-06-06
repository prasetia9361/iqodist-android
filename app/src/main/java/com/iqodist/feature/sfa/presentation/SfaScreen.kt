package com.iqodist.feature.sfa.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SfaScreen(onNavigateUp: () -> Unit) {
    // TODO: Implementasi SFA - check-in outlet, taking order, rute kunjungan
    Column(
        modifier            = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SFA - Lapangan", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateUp) { Text("Kembali") }
    }
}
