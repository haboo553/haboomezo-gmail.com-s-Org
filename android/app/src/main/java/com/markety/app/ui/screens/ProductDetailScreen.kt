package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markety.app.data.local.entity.MovementType
import com.markety.app.data.local.entity.StockMovementEntity
import com.markety.app.ui.viewmodel.ProductDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    var showAdjustStockDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val product = uiState.productWithCategory?.product
    val category = uiState.productWithCategory?.category

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = product?.name ?: "تفاصيل الصنف",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (product != null) {
                        IconButton(onClick = { onNavigateToEdit(product.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل البيانات")
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "حذف الصنف",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("تعذر العثور على الصنف المطلوب")
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Success / Error Feedback
                if (uiState.successMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(uiState.successMessage ?: "", color = Color(0xFF15803D), fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Header Card
                item {
                    val context = LocalContext.current
                    val hasImage = !product.imagePath.isNullOrBlank() && File(product.imagePath).exists()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Captured Product Image Preview
                            if (hasImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(File(product.imagePath!!))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = product.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = product.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                StockStatusBadge(
                                    quantity = product.quantity,
                                    minimumQuantity = product.minimumQuantity
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                category?.let {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("الفئة: ${it.name}") },
                                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                                val barcodeDisplay = if (product.barcode.isNullOrBlank()) "بدون باركود" else "باركود: ${product.barcode}"
                                AssistChip(
                                    onClick = {},
                                    label = { Text(barcodeDisplay) },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }

                // Inventory & Pricing Snapshot
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("الكمية الحالية", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${product.quantity} قطعة", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                Text("حد الطلب: ${product.minimumQuantity}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("سعر البيع", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${product.sellingPrice} ج.م", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                val margin = product.sellingPrice - product.purchasePrice
                                Text("الشراء: ${product.purchasePrice} | ربح: ${String.format("%.1f", margin)}", fontSize = 11.sp, color = Color(0xFF15803D))
                            }
                        }
                    }
                }

                // Quick Action: Adjust Quantity Button
                item {
                    Button(
                        onClick = { showAdjustStockDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تعديل كمية المخزون (تسجيل حركة جرد/تعديل)", fontWeight = FontWeight.Bold)
                    }
                }

                // Additional Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("بيانات إضافية", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            InfoRow(label = "المورد", value = product.supplierName ?: "غير محدد")
                            InfoRow(label = "تاريخ الصلاحية", value = product.expiryDate ?: "غير محدد")
                            InfoRow(label = "ملاحظات", value = product.notes ?: "لا توجد")
                        }
                    }
                }

                // Requirement 11: Stock Movements Table / History
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل حركة المخزون (${uiState.movements.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "التاريخ | الحركة | الكمية | قبل | بعد",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.movements.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("لا توجد حركات مخزون مسجلة بعد لهذا الصنف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(uiState.movements, key = { it.id }) { movement ->
                        StockMovementRowCard(movement = movement)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Adjust Stock Dialog
    if (showAdjustStockDialog && product != null) {
        var newQtyInput by remember { mutableStateOf(product.quantity.toString()) }
        var reasonInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAdjustStockDialog = false },
            title = { Text("تعديل كمية المخزون للصنف", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "الكمية الحالية المسجلة: ${product.quantity} قطعة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = newQtyInput,
                        onValueChange = { newQtyInput = it },
                        label = { Text("الكمية الفعلية الجديدة *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("سبب التعديل / ملاحظة الحركة *") },
                        placeholder = { Text("مثال: تعديل جرد دوري، هالك، عجز...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newQtyInput.toIntOrNull()
                        if (parsed == null) {
                            errorMsg = "يرجى كتابة كمية صحيحة"
                            return@Button
                        }
                        if (reasonInput.isBlank()) {
                            errorMsg = "يرجى توضيح سبب التعديل"
                            return@Button
                        }
                        viewModel.adjustQuantity(parsed, reasonInput)
                        showAdjustStockDialog = false
                    }
                ) {
                    Text("حفظ وتسجيل الحركة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustStockDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && product != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("حذف الصنف (Soft Delete)", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من حذف الصنف \"${product.name}\"؟\n\n" +
                            "سيتم إلغاء تفعيله ولن يظهر في القوائم العادية، مع الحفاظ على جميع حركات المخزون السابقة في قاعدة البيانات دون مساس."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StockMovementRowCard(movement: StockMovementEntity) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(movement.createdAt) { dateFormat.format(Date(movement.createdAt)) }

    val (badgeBg, badgeText) = when (movement.movementType) {
        MovementType.INITIAL -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
        MovementType.ADJUSTMENT -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        MovementType.PURCHASE -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
        MovementType.SALE -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
        MovementType.DAMAGE -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
        MovementType.RETURN -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
        MovementType.INVENTORY -> Pair(Color(0xFFE2E8F0), Color(0xFF334155))
    }

    val deltaPrefix = if (movement.quantity > 0) "+${movement.quantity}" else "${movement.quantity}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = movement.movementType.arabicLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Values row: Qty delta, before, after
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "الكمية: $deltaPrefix",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (movement.quantity >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                    )
                    Text(
                        text = "قبل: ${movement.previousQuantity}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "بعد: ${movement.newQuantity}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!movement.notes.isNullOrBlank()) {
                Text(
                    text = "ملاحظة: ${movement.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
