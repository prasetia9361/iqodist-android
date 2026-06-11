package com.iqodist.feature.pos.presentation

import android.Manifest
//import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
//import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * BarcodeScannerView — komponen kamera untuk scan barcode produk.
 *
 * Cara kerja:
 * 1. Minta izin kamera jika belum ada
 * 2. Tampilkan preview kamera di layar
 * 3. Setiap frame kamera dianalisis ML Kit secara otomatis
 * 4. Jika barcode ditemukan, panggil onBarcodeDetected(barcode)
 * 5. Ada delay 1.5 detik setelah scan agar tidak scan berkali-kali
 *
 * @param onBarcodeDetected dipanggil setelah barcode berhasil dibaca
 * @param modifier          modifier Compose untuk ukuran/posisi
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerView(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // State untuk mencegah scan berkali-kali dalam waktu singkat
    var isProcessing by remember { mutableStateOf(false) }

    // Minta permission kamera
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Executor untuk analisis kamera (background thread)
    val cameraExecutor: ExecutorService = remember {
        Executors.newSingleThreadExecutor()
    }

    // Bersihkan executor saat composable dihancurkan
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    if (!cameraPermissionState.status.isGranted) {
        // Tampilkan UI minta permission
        Column(
            modifier            = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = "Aplikasi membutuhkan akses kamera untuk scan barcode.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { cameraPermissionState.launchPermissionRequest() }
            ) {
                Text("Izinkan Akses Kamera")
            }
        }
        return
    }

    // Tampilkan preview kamera
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // Konfigurasi preview kamera
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Konfigurasi analisis gambar untuk barcode
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (!isProcessing) {
                                processImageForBarcode(
                                    imageProxy        = imageProxy,
                                    onBarcodeDetected = { barcode ->
                                        isProcessing = true
                                        onBarcodeDetected(barcode)
                                        // Reset setelah 1.5 detik agar bisa scan lagi
                                        android.os.Handler(
                                            android.os.Looper.getMainLooper()
                                        ).postDelayed({
                                            isProcessing = false
                                        }, 1500)
                                    }
                                )
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

/**
 * Fungsi pembantu — analisis satu frame kamera untuk mencari barcode.
 * Dipanggil dari background thread (cameraExecutor).
 */

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageForBarcode(
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees
    )

    val scanner = BarcodeScanning.getClient()

    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            // Ambil barcode pertama yang ditemukan
            val barcode = barcodes.firstOrNull {
                // Filter hanya format barcode yang relevan untuk produk
                it.format in listOf(
                    Barcode.FORMAT_EAN_13,   // format umum produk ritel
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_CODE_128, // format umum logistik
                    Barcode.FORMAT_QR_CODE   // QR code
                )
            }
            barcode?.rawValue?.let { value ->
                onBarcodeDetected(value)
            }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}