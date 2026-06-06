package com.iqodist.feature.inventory.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InventoryScreen(onNavigateUp: () -> Unit) {
    // TODO: Implementasi Inventory - stok, opname, transfer gudang
    Column(
        modifier            = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Inventory", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateUp) { Text("Kembali") }
    }
}
