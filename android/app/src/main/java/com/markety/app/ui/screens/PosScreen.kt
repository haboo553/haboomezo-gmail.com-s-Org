package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.markety.app.data.local.entity.PaymentType
import com.markety.app.ui.viewmodel.CartItem
import com.markety.app.ui.viewmodel.PosViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToAddProductWithBarcode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Unregistered barcode dialog
    uiState.unregisteredBarcodeScanned?.let { barcode ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUnregisteredBarcodeDialog() },
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
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("هل ترغب في تسجيل هذا المنتج الآن؟", fontSize = 13.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissUnregisteredBarcodeDialog()
                        onNavigateToAddProductWithBarcode(barcode)
                    }
                ) {
                    Text("إضافة المنتج")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissUnregisteredBarcodeDialog() }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Success sale dialog
    uiState.successSaleResult?.let { result ->
        Dialog(onDismissRequest = { viewModel.resetNewSale() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = "تم إتمام البيع بنجاح",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SaleSummaryRow(
                                label = "رقم الفاتورة",
                                value = result.invoice.invoiceNumber,
                                isBold = true
                            )
                            SaleSummaryRow(
                                label = "إجمالي الفاتورة",
                                value = "${String.format(Locale.US, "%.2f", result.invoice.total)} ج.م",
                                isBold = true
                            )
                            SaleSummaryRow(
                                label = "المبلغ المدفوع",
                                value = "${String.format(Locale.US, "%.2f", result.invoice.paidAmount)} ج.م"
                            )

                            if (result.invoice.paymentType == PaymentType.CREDIT && result.invoice.remainingAmount > 0) {
                                SaleSummaryRow(
                                    label = "المتبقي (آجل)",
                                    value = "${String.format(Locale.US, "%.2f", result.invoice.remainingAmount)} ج.م",
                                    valueColor = MaterialTheme.colorScheme.error,
                                    isBold = true
                                )
                            }

                            if (result.change > 0) {
                                SaleSummaryRow(
                                    label = "الباقي للعميل",
                                    value = "${String.format(Locale.US, "%.2f", result.change)} ج.م",
                                    valueColor = Color(0xFF10B981),
                                    isBold = true
                                )
                            }

                            SaleSummaryRow(
                                label = "طريقة الدفع",
                                value = result.invoice.paymentType.arabicLabel
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.resetNewSale() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فاتورة جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("نقطة البيع (الكاشير)", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "مسح باركود",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (uiState.cartItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "تفريغ السلة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            PosBottomBar(
                uiState = uiState,
                onDiscountChanged = { viewModel.onDiscountChanged(it) },
                onPaidAmountChanged = { viewModel.onPaidAmountChanged(it) },
                onPaymentTypeChanged = { viewModel.onPaymentTypeChanged(it) },
                onCompleteSale = { viewModel.completeSale() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error banner
            uiState.errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                        }
                        IconButton(onClick = { viewModel.dismissError() }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Search Bar & Barcode scan quick action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("ابحث بالاسم أو الباركود...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                FilledTonalIconButton(
                    onClick = onNavigateToScanner,
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح باركود")
                }
            }

            // Real-time Search Results overlay if searching
            if (uiState.searchQuery.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    if (uiState.searchResults.isEmpty() && !uiState.isSearching) {
                        Text(
                            text = "لا توجد أصناف مطابقة للبحث",
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(uiState.searchResults, key = { it.product.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addProductToCart(item.product, item.category?.name)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (!item.product.barcode.isNullOrBlank()) {
                                                Text(item.product.barcode, fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                            }
                                            Text("المتاح: ${item.product.quantity}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text(
                                        "${String.format(Locale.US, "%.2f", item.product.sellingPrice)} ج.م",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Cart Items List
            if (uiState.cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = Color.LightGray
                        )
                        Text(
                            text = "السلة فارغة",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "امسح باركود المنتج بالكاميرا أو ابحث عنه لإضافته",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.cartItems, key = { it.product.id }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrease = { viewModel.increaseQuantity(item.product.id) },
                            onDecrease = { viewModel.decreaseQuantity(item.product.id) },
                            onRemove = { viewModel.removeProductFromCart(item.product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product image snapshot or placeholder
            if (!item.product.imagePath.isNullOrBlank() && File(item.product.imagePath).exists()) {
                AsyncImage(
                    model = File(item.product.imagePath),
                    contentDescription = item.product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Name & Price Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", item.unitSellingPrice)} ج.م",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "(المتاح: ${item.product.quantity})",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = "الإجمالي: ${String.format(Locale.US, "%.2f", item.total)} ج.م",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F766E)
                )
            }

            // Quantity buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                        contentDescription = "تقليل",
                        tint = if (item.quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(32.dp),
                    enabled = item.quantity < item.product.quantity
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "زيادة",
                        tint = if (item.quantity < item.product.quantity) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            // Quick Delete
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "حذف الصنف",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun PosBottomBar(
    uiState: com.markety.app.ui.viewmodel.PosUiState,
    onDiscountChanged: (String) -> Unit,
    onPaidAmountChanged: (String) -> Unit,
    onPaymentTypeChanged: (PaymentType) -> Unit,
    onCompleteSale: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Totals and Discount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("الإجمالي قبل الخصم", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${String.format(Locale.US, "%.2f", uiState.subtotal)} ج.م",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Discount Input
                OutlinedTextField(
                    value = uiState.discountInput,
                    onValueChange = onDiscountChanged,
                    label = { Text("الخصم (ج.م)", fontSize = 11.sp) },
                    modifier = Modifier.width(130.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text("الإجمالي النهائي", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${String.format(Locale.US, "%.2f", uiState.finalTotal)} ج.م",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            // Payment method selector: Cash vs Credit (آجل)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.paymentType == PaymentType.CASH,
                    onClick = { onPaymentTypeChanged(PaymentType.CASH) },
                    label = { Text("كاش (نقدي)", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        if (uiState.paymentType == PaymentType.CASH) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = uiState.paymentType == PaymentType.CREDIT,
                    onClick = { onPaymentTypeChanged(PaymentType.CREDIT) },
                    label = { Text("آجل (ديون)", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        if (uiState.paymentType == PaymentType.CREDIT) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Paid Amount and Change / Remaining Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.paidAmountInput,
                    onValueChange = onPaidAmountChanged,
                    label = { Text("المدفوع (ج.م)", fontSize = 12.sp) },
                    placeholder = {
                        Text(
                            "${String.format(Locale.US, "%.2f", uiState.finalTotal)}",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp)
                )

                // Feedback badge
                Surface(
                    color = if (uiState.paymentType == PaymentType.CASH) {
                        Color(0xFF10B981).copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.paymentType == PaymentType.CASH) {
                            Text("الباقي للعميل", fontSize = 11.sp, color = Color(0xFF065F46))
                            Text(
                                "${String.format(Locale.US, "%.2f", uiState.changeAmount)} ج.م",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF047857)
                            )
                        } else {
                            Text("المتبقي (آجل)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                "${String.format(Locale.US, "%.2f", uiState.remainingAmount)} ج.م",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Complete Sale Button
            Button(
                onClick = onCompleteSale,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = uiState.cartItems.isNotEmpty() && !uiState.isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري إتمام البيع...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إتمام البيع (${String.format(Locale.US, "%.2f", uiState.finalTotal)} ج.م)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SaleSummaryRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(
            text = value,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            color = valueColor
        )
    }
}
