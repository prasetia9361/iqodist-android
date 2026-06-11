package com.iqodist.feature.pos.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * AgeVerificationDialog — konfirmasi usia sebelum checkout produk tembakau.
 *
 * Sesuai regulasi, kasir wajib memverifikasi identitas pembeli
 * dan mengkonfirmasi pembeli berusia minimal 21 tahun.
 *
 * @param onConfirm  kasir menekan "Konfirmasi" — transaksi dilanjutkan
 * @param onDismiss  kasir menekan "Batalkan" — transaksi dibatalkan
 */
@Composable
fun AgeVerificationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector        = Icons.Default.Warning,
                contentDescription = "Peringatan",
                tint               = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text       = "Verifikasi Usia Pembeli",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text  = "Produk dalam keranjang ini hanya dapat dijual kepada " +
                            "pembeli yang berusia 21 tahun atau lebih.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Health Warning — wajib ditampilkan sesuai regulasi
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text       = "⚠ PERINGATAN PEMERINTAH",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text  = "Merokok membunuhmu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text  = "Dengan menekan Konfirmasi, kasir menyatakan telah " +
                            "memeriksa identitas pembeli dan memverifikasi bahwa " +
                            "pembeli berusia minimal 21 tahun.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Konfirmasi — Pembeli ≥ 21 Tahun")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text  = "Batalkan Transaksi",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}