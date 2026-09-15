package com.markety.app.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real CameraX ImageAnalysis Analyzer using Google ML Kit Barcode Scanning.
 * Supports: EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, QR Code.
 */
class BarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val isScanningActive = AtomicBoolean(true)

    // ML Kit Scanner configured with supermarket and retail barcode standards
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_QR_CODE
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    fun resumeScanning() {
        isScanningActive.set(true)
    }

    fun pauseScanning() {
        isScanningActive.set(false)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanningActive.get()) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (!isScanningActive.get()) return@addOnSuccessListener

                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            // Pause further scanning so it doesn't trigger multiple times
                            isScanningActive.set(false)
                            onBarcodeScanned(rawValue)
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    // Fail silently on frame error, keep streaming
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
