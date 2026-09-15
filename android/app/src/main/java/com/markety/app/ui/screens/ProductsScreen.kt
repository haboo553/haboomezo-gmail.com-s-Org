package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markety.app.data.local.dao.ProductWithCategory
import com.markety.app.ui.viewmodel.ProductViewModel
import com.markety.app.ui.viewmodel.StockFilterOption
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductViewModel,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToProductDetail: (Long) -> Unit,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val stockFilter by viewModel.stockFilter.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var productToDelete by remember { mutableStateOf<ProductWithCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الأصناف والمخزون",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    // Requirement 6: Prominent «مسح باركود» action button
                    FilledTonalButton(
                        onClick = onNavigateToScanner,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح باركود", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary FAB for quick scanning
                FloatingActionButton(
                    onClick = onNavigateToScanner,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح باركود")
                }

                ExtendedFloatingActionButton(
                    onClick = onNavigateToAddProduct,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("إضافة صنف", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Row with clear icon and quick scanner shortcut
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("ابحث بالاسم أو الباركود...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = onNavigateToScanner,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "مسح بالـكاميرا",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Filter Chips (All, Available, Low Stock, Out of Stock)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StockFilterOption.values()) { filter ->
                    FilterChip(
                        selected = stockFilter == filter,
                        onClick = { viewModel.onFilterSelect(filter) },
                        label = { Text(filter.arabicLabel) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Products List
            if (uiState.products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "لم يتم العثور على أي صنف يطابق البحث" else "لا توجد أصناف في قاعدة البيانات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Button(
                                onClick = onNavigateToAddProduct,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة أول صنف الآن")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.products, key = { it.product.id }) { item ->
                        ProductItemCard(
                            item = item,
                            onClick = { onNavigateToProductDetail(item.product.id) },
                            onDelete = { productToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Soft Delete Confirmation Dialog
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تأكيد حذف الصنف", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هل أنت متأكد من حذف الصنف \"${productToDelete?.product?.name}\"؟\n\n" +
                            "سيتم استخدام الحذف غير المباشر (Soft Delete) لإخفائه من القائمة مع الاحتفاظ بكافة سجلات حركات المخزون التاريخية الخاصة به بأمان."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let { viewModel.deleteProduct(it.product.id) }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف الصنف")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    item: ProductWithCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val p = item.product
    val context = LocalContext.current
    val hasImage = !p.imagePath.isNullOrBlank() && File(p.imagePath).exists()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Thumbnail
                if (hasImage) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(p.imagePath!!))
                                .crossfade(true)
                                .build(),
                            contentDescription = p.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = p.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.category?.let {
                            Text(
                                text = it.name,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        val barcodeLabel = if (p.barcode.isNullOrBlank()) "بدون باركود" else "• باركود: ${p.barcode}"
                        Text(
                            text = barcodeLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stock Status Badge
                StockStatusBadge(
                    quantity = p.quantity,
                    minimumQuantity = p.minimumQuantity
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Pricing and Quantities
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("سعر البيع", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${p.sellingPrice} ج.م",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text("سعر الشراء", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${p.purchasePrice} ج.م",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("الكمية الحالية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${p.quantity} قطعة",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (p.quantity <= 0) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف الصنف",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockStatusBadge(
    quantity: Int,
    minimumQuantity: Int
) {
    val (text, bgColor, textColor) = when {
        quantity <= 0 -> Triple("نفد", Color(0xFFFEE2E2), Color(0xFFB91C1C))
        quantity <= minimumQuantity -> Triple("مخزون منخفض", Color(0xFFFEF3C7), Color(0xFFB45309))
        else -> Triple("متوفر", Color(0xFFDCFCE7), Color(0xFF15803D))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
