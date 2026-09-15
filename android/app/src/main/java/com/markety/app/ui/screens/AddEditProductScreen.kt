package com.markety.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markety.app.ui.viewmodel.AddEditProductViewModel
import com.markety.app.util.ImageStorageUtil
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    viewModel: AddEditProductViewModel,
    productId: Long = 0,
    initialBarcode: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToScanBarcode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.formState.collectAsState()

    LaunchedEffect(productId) {
        if (productId > 0) {
            viewModel.loadProduct(productId)
        } else if (initialBarcode.isNotBlank()) {
            viewModel.setInitialBarcode(initialBarcode)
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
        }
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    // Camera Capture Launcher for Real Device Camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoFile != null) {
            viewModel.onImagePathChange(currentPhotoFile?.absolutePath)
        }
    }

    // Gallery Picker as fallback/convenience
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageUtil.copyUriToLocalStorage(context, uri)
            if (savedPath != null) {
                viewModel.onImagePathChange(savedPath)
            }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val (file, uri) = ImageStorageUtil.createTempImageUri(context)
            currentPhotoFile = file
            takePictureLauncher.launch(uri)
        } else {
            showCameraPermissionDialog = true
        }
    }

    fun launchCameraCapture() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val (file, uri) = ImageStorageUtil.createTempImageUri(context)
            currentPhotoFile = file
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "تعديل الصنف" else "إضافة صنف جديد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Error banner if any
            if (state.errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFB91C1C))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.errorMessage ?: "",
                                color = Color(0xFFB91C1C),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 1. Real Product Photo Card (Camera & Local Storage)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "صورة الصنف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        if (!state.imagePath.isNullOrBlank() && File(state.imagePath!!).exists()) {
                            // Display Captured Local Image
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(File(state.imagePath!!))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "صورة المنتج",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Overlay Delete button
                                IconButton(
                                    onClick = { viewModel.removeImage() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xCCDC2626))
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف الصورة",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { launchCameraCapture() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("استبدال بالكاميرا", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.removeImage() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إزالة الصورة", fontSize = 12.sp)
                                }
                            }
                        } else {
                            // No Image Yet -> Open Camera or Pick from Gallery
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { launchCameraCapture() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تصوير بالكاميرا", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اختيار من الهاتف", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Name
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    label = { Text("اسم الصنف *") },
                    placeholder = { Text("مثال: جبنة دومتي بيضاء 500 جم") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Barcode Field with Manual Input AND «مسح بالـكاميرا» Button
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.barcode,
                            onValueChange = { viewModel.onBarcodeChange(it) },
                            label = { Text("الباركود (يدوي أو مسح)") },
                            placeholder = { Text("أدخل أرقام الباركود أو امسح...") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                            trailingIcon = {
                                if (state.barcode.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onBarcodeChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Requirement 5: «مسح بالـكاميرا» Button next to barcode input
                        Button(
                            onClick = onNavigateToScanBarcode,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسح بالـكاميرا", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "اختياري: يمكن ترك الحقل فارغاً إذا كان الصنف بدون باركود",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category Selector
            item {
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentCategory = state.categories.find { it.id == state.categoryId }
                    OutlinedTextField(
                        value = currentCategory?.name ?: "اختر الفئة...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("فئة الصنف *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        state.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    viewModel.onCategoryChange(category.id)
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Prices Row: Purchase Price & Selling Price
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.purchasePrice,
                        onValueChange = { viewModel.onPurchasePriceChange(it) },
                        label = { Text("سعر الشراء (ج.م) *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = state.sellingPrice,
                        onValueChange = { viewModel.onSellingPriceChange(it) },
                        label = { Text("سعر البيع (ج.م) *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Quantities Row: Quantity & Minimum Quantity
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.quantity,
                        onValueChange = { viewModel.onQuantityChange(it) },
                        label = { Text(if (state.isEditMode) "الكمية الحالية *" else "الكمية الافتتاحية *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = state.minimumQuantity,
                        onValueChange = { viewModel.onMinimumQuantityChange(it) },
                        label = { Text("الحد الأدنى للطلب *") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Expiry Date & Supplier
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.expiryDate,
                        onValueChange = { viewModel.onExpiryDateChange(it) },
                        label = { Text("تاريخ الصلاحية") },
                        placeholder = { Text("مثال: 12/2026") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = state.supplierName,
                        onValueChange = { viewModel.onSupplierNameChange(it) },
                        label = { Text("اسم المورد") },
                        placeholder = { Text("شركة الأهرام") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Notes
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.onNotesChange(it) },
                    label = { Text("ملاحظات إضافية") },
                    placeholder = { Text("مكان التخزين، الرف، شروط العرض...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Save Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.saveProduct() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isEditMode) "حفظ التعديلات في قاعدة البيانات" else "حفظ الصنف وتسجيل الرصيد الافتتاحي",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Camera Permission Explanatory Dialog
    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("صلاحية الكاميرا مطلوبة", fontWeight = FontWeight.Bold) },
            text = {
                Text("يحتاج تطبيق ماركتي إلى صلاحية الكاميرا لالتقاط صورة حقيقية للمنتج وتخزينها محلياً على الجهاز.")
            },
            confirmButton = {
                Button(onClick = {
                    showCameraPermissionDialog = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("منح الصلاحية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
