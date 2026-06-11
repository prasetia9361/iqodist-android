package com.iqodist.feature.pos.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqodist.feature.pos.domain.model.PaymentMethod
import com.iqodist.feature.pos.domain.model.toRupiaFormat

/**
 * PosScreen — layar utama kasir.
 *
 * Dibagi menjadi dua bagian:
 * - Atas: daftar item di keranjang
 * - Bawah: ringkasan total dan tombol bayar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onNavigateUp: () -> Unit,
    viewModel: CartViewModel = hiltViewModel()
) {
    val cartItems     by viewModel.cartItems.collectAsStateWithLifecycle()
    val subtotal      by viewModel.subtotal.collectAsStateWithLifecycle()
    val taxTotal      by viewModel.taxTotal.collectAsStateWithLifecycle()
    val grandTotal    by viewModel.grandTotal.collectAsStateWithLifecycle()
    val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()
    val hasAgeRestricted by viewModel.hasAgeRestrictedItem.collectAsStateWithLifecycle()

    // State lokal — hanya relevan di layar ini
    var showScanner          by remember { mutableStateOf(false) }
    var showAgeVerification  by remember { mutableStateOf(false) }
    var showPaymentDialog    by remember { mutableStateOf(false) }
    var ageVerified          by remember { mutableStateOf(false) }

    // Reaksi terhadap perubahan checkoutState
    LaunchedEffect(checkoutState) {
        when (checkoutState) {
            is CheckoutState.NeedAgeVerification -> showAgeVerification = true
            else -> { /* ditangani di bawah */ }
        }
    }

    // Dialog verifikasi usia
    if (showAgeVerification) {
        AgeVerificationDialog(
            onConfirm = {
                ageVerified = true
                showAgeVerification = false
                viewModel.resetCheckoutState()
                // Lanjutkan ke pilihan pembayaran
                showPaymentDialog = true
            },
            onDismiss = {
                showAgeVerification = false
                viewModel.resetCheckoutState()
            }
        )
    }

    // Dialog pilihan pembayaran
    if (showPaymentDialog) {
        PaymentDialog(
            grandTotal = grandTotal,
            onConfirm  = { method, amountPaid ->
                showPaymentDialog = false
                viewModel.checkout(
                    paymentMethod = method,
                    amountPaid    = amountPaid,
                    ageVerified   = ageVerified
                )
            },
            onDismiss  = { showPaymentDialog = false }
        )
    }

    // Dialog sukses transaksi
    if (checkoutState is CheckoutState.Success) {
        val success = checkoutState as CheckoutState.Success
        SuccessDialog(
            change     = success.change,
            receiptId  = success.receiptId,
            onPrint    = { /* TODO: cetak struk */ },
            onNewTransaction = {
                ageVerified = false
                viewModel.clearCart()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasir POS") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Tombol scan barcode
                    IconButton(onClick = { showScanner = !showScanner }) {
                        Icon(
                            imageVector        = if (showScanner) Icons.Default.Close
                            else Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode"
                        )
                    }
                    // Tombol hapus semua
                    if (cartItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Kosongkan")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Area Scan Barcode ────────────────────────────────────────
            if (showScanner) {
                BarcodeScannerView(
                    onBarcodeDetected = { barcode ->
                        viewModel.onBarcodeScanned(barcode)
                        // Tetap buka scanner agar kasir bisa scan item berikutnya
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            // ── Daftar Item di Keranjang ─────────────────────────────────
            if (cartItems.isEmpty()) {
                // Tampilkan ilustrasi keranjang kosong
                Box(
                    modifier            = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment    = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector        = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier           = Modifier.size(64.dp),
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text  = "Keranjang kosong",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = "Scan barcode untuk menambah produk",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(
                        items = cartItems,
                        key   = { it.productId }  // key penting untuk animasi list
                    ) { item ->
                        CartItemRow(
                            item       = item,
                            onIncrease = { viewModel.updateQuantity(item.productId, item.quantity + 1) },
                            onDecrease = { viewModel.updateQuantity(item.productId, item.quantity - 1) },
                            onRemove   = { viewModel.removeItem(item.productId) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            // ── Ringkasan Total & Tombol Bayar ───────────────────────────
            if (cartItems.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Baris subtotal
                        Row(
                            modifier       = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                            Text(subtotal.toRupiaFormat())
                        }
                        // Baris pajak
                        Row(
                            modifier       = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "PPN (11%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                taxTotal.toRupiaFormat(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Baris grand total
                        Row(
                            modifier       = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                grandTotal.toRupiaFormat(),
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tombol Bayar
                        Button(
                            onClick  = {
                                if (hasAgeRestricted && !ageVerified) {
                                    showAgeVerification = true
                                } else {
                                    showPaymentDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled  = checkoutState !is CheckoutState.Processing
                        ) {
                            if (checkoutState is CheckoutState.Processing) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(20.dp),
                                    color       = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Memproses...")
                            } else {
                                Text("Bayar ${grandTotal.toRupiaFormat()}")
                            }
                        }

                        // Tampilkan error jika ada
                        if (checkoutState is CheckoutState.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text  = (checkoutState as CheckoutState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Komponen Kecil ───────────────────────────────────────────────────────────

/** Satu baris item di keranjang */
@Composable
private fun CartItemRow(
    item: com.iqodist.feature.pos.domain.model.CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier             = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment    = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.productName, style = MaterialTheme.typography.bodyMedium)
            Text(
                item.price.toRupiaFormat(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Kontrol jumlah
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Kurangi")
            }
            Text(
                text      = "${item.quantity}",
                modifier  = Modifier.padding(horizontal = 8.dp),
                style     = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text       = item.totalWithTax.toRupiaFormat(),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Hapus",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** Dialog pilih metode pembayaran */
@Composable
private fun PaymentDialog(
    grandTotal: Long,
    onConfirm: (PaymentMethod, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var cashInput      by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Pembayaran") },
        text  = {
            Column {
                // Pilihan metode bayar
                PaymentMethod.entries.forEach { method ->
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMethod == method,
                            onClick  = { selectedMethod = method }
                        )
                        Text(method.displayName)
                    }
                }

                // Input nominal untuk pembayaran tunai
                if (selectedMethod == PaymentMethod.CASH) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = cashInput,
                        onValueChange = { cashInput = it.filter { c -> c.isDigit() } },
                        label         = { Text("Uang yang Dibayar") },
                        prefix        = { Text("Rp ") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    // Tampilkan kembalian jika uang cukup
                    val paid = cashInput.toLongOrNull() ?: 0L
                    if (paid >= grandTotal) {
                        Text(
                            "Kembalian: ${(paid - grandTotal).toRupiaFormat()}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paid = if (selectedMethod == PaymentMethod.CASH)
                        cashInput.toLongOrNull() ?: 0L
                    else
                        grandTotal
                    onConfirm(selectedMethod, paid)
                }
            ) {
                Text("Proses Pembayaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

/** Dialog sukses setelah transaksi berhasil */
@Composable
private fun SuccessDialog(
    change: Long,
    receiptId: String,
    onPrint: () -> Unit,
    onNewTransaction: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},  // tidak bisa ditutup dengan tap di luar
        icon  = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Pembayaran Berhasil!") },
        text  = {
            Column {
                if (change > 0) {
                    Text(
                        "Kembalian: ${change.toRupiaFormat()}",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    "No. Transaksi: ${receiptId.take(8).uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Column {
                Button(
                    onClick  = onPrint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Struk")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick  = onNewTransaction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Transaksi Baru")
                }
            }
        }
    )
}