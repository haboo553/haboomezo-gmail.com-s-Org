package com.markety.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.markety.app.camera.BarcodeAnalyzer
import com.markety.app.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Real Barcode Scanner Screen powered by CameraX and Google ML Kit.
 * Reusable for both Inventory lookup and future Sales POS integration.
 */
@Composable
fun BarcodeScannerScreen(
    productRepository: ProductRepository,
    onNavigateToProductDetail: (Long) -> Unit,
    onNavigateToAddProductWithBarcode: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onBarcodeResult: ((String) -> Unit)? = null,
    isPosMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDeniedPermanently by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControlInstance by remember { mutableStateOf<Camera?>(null) }
    var scannedBarcodeResult by remember { mutableStateOf<String?>(null) }
    var scanStatusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessingScan by remember { mutableStateOf(false) }
    var unregisteredBarcodeAlert by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            permissionDeniedPermanently = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Unregistered barcode dialog for POS mode
    unregisteredBarcodeAlert?.let { barcode ->
        AlertDialog(
            onDismissRequest = {
                unregisteredBarcodeAlert = null
                isProcessingScan = false
                scannedBarcodeResult = null
                scanStatusMessage = null
            },
            title = {
                Text(
                    text = "المنتج غير مسجل",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("لم يتم العثور على أي منتج مسجل بالباركود التالي في قاعدة البيانات:")
                    Text(
                        text = barcode,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("هل ترغب في تسجيل هذا المنتج الآن في المخزون؟", fontSize = 13.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        unregisteredBarcodeAlert = null
                        onNavigateToAddProductWithBarcode(barcode)
                    }
                ) {
                    Text("إضافة المنتج")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        unregisteredBarcodeAlert = null
                        isProcessingScan = false
                        scannedBarcodeResult = null
                        scanStatusMessage = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Handle scanned barcode with database lookup
    fun handleScannedBarcode(rawBarcode: String) {
        if (isProcessingScan) return
        isProcessingScan = true
        scannedBarcodeResult = rawBarcode
        scanStatusMessage = "تمت قراءة الباركود بنجاح: $rawBarcode"

        scope.launch {
            // Short delay so user sees visual success feedback
            delay(400)

            val existingProduct = withContext(Dispatchers.IO) {
                productRepository.getProductWithCategoryByBarcode(rawBarcode)
            }

            if (isPosMode) {
                if (existingProduct != null) {
                    onBarcodeResult?.invoke(rawBarcode)
                } else {
                    unregisteredBarcodeAlert = rawBarcode
                }
                return@launch
            }

            // If a custom callback is provided (e.g., returning to form)
            if (onBarcodeResult != null) {
                onBarcodeResult(rawBarcode)
                return@launch
            }

            // Supermarket Inventory Flow:
            // Check if product already exists in SQLite database
            if (existingProduct != null) {
                // Product exists -> Navigate to ProductDetailScreen
                onNavigateToProductDetail(existingProduct.product.id)
            } else {
                // Product not found -> Navigate to AddProductScreen with prefilled barcode
                onNavigateToAddProductWithBarcode(rawBarcode)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Real CameraX View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val barcodeAnalyzer = BarcodeAnalyzer { barcode ->
                            handleScannedBarcode(barcode)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, barcodeAnalyzer)
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControlInstance = camera
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Viewfinder and Scanning Overlay
            ScannerViewfinderOverlay(isSuccess = scannedBarcodeResult != null)

            // Top Bar Controls (Back & Torch/Flash)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "إلغاء والعودة",
                        tint = Color.White
                    )
                }

                Text(
                    text = "ماسح الباركود الحقيقي",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                IconButton(
                    onClick = {
                        cameraControlInstance?.let { cam ->
                            val nextState = !isFlashOn
                            cam.cameraControl.enableTorch(nextState)
                            isFlashOn = nextState
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(
                        if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "كشاف الكاميرا",
                        tint = if (isFlashOn) Color(0xFFFBBF24) else Color.White
                    )
                }
            }

            // Bottom Information and Manual Input fallback
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Real scan status card
                if (scannedBarcodeResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = scanStatusMessage ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x99000000)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "وجّه الكاميرا نحو باركود الصنف ليتم مسحه تلقائيًا",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Manual Entry Button
                OutlinedButton(
                    onClick = {
                        if (onBarcodeResult != null) {
                            onNavigateBack()
                        } else {
                            onNavigateToAddProductWithBarcode("")
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x55000000),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("المتابعة بالإدخال اليدوي للباركود", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            // Camera Permission Denied Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "صلاحية الكاميرا مطلوبة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الكاميرا مطلوبة لمسح باركود الأصناف وتصوير المنتجات داخل السوبر ماركت.",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (permissionDeniedPermanently) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (permissionDeniedPermanently) "فتح إعدادات التطبيق وتفعيل الكاميرا" else "منح صلاحية الكاميرا الآن")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        if (onBarcodeResult != null) {
                            onNavigateBack()
                        } else {
                            onNavigateToAddProductWithBarcode("")
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("المتابعة بالإدخال اليدوي بدون كاميرا", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onNavigateBack) {
                    Text("إلغاء والرجوع", color = Color(0xFF9CA3AF))
                }
            }
        }
    }
}

/**
 * Animated Scanning Viewfinder with corner brackets and laser line.
 */
@Composable
fun ScannerViewfinderOverlay(isSuccess: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val boxWidth = canvasWidth * 0.78f
        val boxHeight = boxWidth * 0.70f
        val left = (canvasWidth - boxWidth) / 2f
        val top = (canvasHeight - boxHeight) / 2f - 40f

        // Draw darkened backdrop outside the scanning rect
        drawRect(
            color = Color(0x99000000),
            size = size
        )

        // Clear center transparent window
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear
        )

        val cornerColor = if (isSuccess) Color(0xFF10B981) else Color(0xFF38BDF8)
        val strokeWidth = 5.dp.toPx()
        val cornerLength = 32.dp.toPx()

        // Top-Left corner
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

        // Top-Right corner
        drawLine(cornerColor, Offset(left + boxWidth, top), Offset(left + boxWidth - cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(left + boxWidth, top), Offset(left + boxWidth, top + cornerLength), strokeWidth)

        // Bottom-Left corner
        drawLine(cornerColor, Offset(left, top + boxHeight), Offset(left + cornerLength, top + boxHeight), strokeWidth)
        drawLine(cornerColor, Offset(left, top + boxHeight), Offset(left, top + boxHeight - cornerLength), strokeWidth)

        // Bottom-Right corner
        drawLine(cornerColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth - cornerLength, top + boxHeight), strokeWidth)
        drawLine(cornerColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth, top + boxHeight - cornerLength), strokeWidth)

        // Laser scan line
        if (!isSuccess) {
            val laserY = top + (boxHeight * laserYRatio)
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(left + 12f, laserY),
                end = Offset(left + boxWidth - 12f, laserY),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}
