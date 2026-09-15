package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markety.app.data.local.dao.PurchaseInvoiceWithSupplierAndItems
import com.markety.app.data.local.entity.ProductEntity
import com.markety.app.data.local.entity.SupplierEntity
import com.markety.app.ui.viewmodel.PurchasesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: PurchasesViewModel,
    onNavigateBack: () -> Unit
) {
    val purchases by viewModel.purchases.collectAsState()
    val activeSuppliers by viewModel.activeSuppliers.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val selectedSupplier by viewModel.selectedSupplier.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val paidAmount by viewModel.paidAmount.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val successResult by viewModel.successResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showNewPurchaseDialog by remember { mutableStateOf(false) }
    var showSelectProductDialog by remember { mutableStateOf(false) }
    var selectedInvoiceDetail by remember { mutableStateOf<PurchaseInvoiceWithSupplierAndItems?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(successResult) {
        successResult?.let {
            snackbarHostState.showSnackbar("تم حفظ فاتورة الشراء رقم ${it.invoice.invoiceNumber} وزيادة المخزون بنجاح!")
            showNewPurchaseDialog = false
            viewModel.clearSuccessResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("فواتير المشتريات والمخزون", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.clearCart()
                    showNewPurchaseDialog = true
                },
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                text = { Text("شراء بضاعة جديدة") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            if (purchases.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("لا توجد فواتير شراء مسجلة بعد", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showNewPurchaseDialog = true }) {
                            Text("تسجيل أول فاتورة شراء")
                        }
                    }
                }
            } else {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(purchases, key = { it.invoice.id }) { item ->
                        val invoice = item.invoice
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedInvoiceDetail = item },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(invoice.invoiceNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${String.format(Locale.US, "%.2f", invoice.total)} ج.م",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "الأصناف: ${item.items.size} صنف",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        dateFormat.format(Date(invoice.dateTime)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                if (invoice.remainingAmount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "متبقي للمورد (آجل): ${String.format(Locale.US, "%.2f", invoice.remainingAmount)} ج.م",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New Purchase Modal Dialog
    if (showNewPurchaseDialog) {
        AlertDialog(
            onDismissRequest = { showNewPurchaseDialog = false },
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
            title = { Text("تسجيل فاتورة شراء بضاعة", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Select Supplier Dropdown
                    var expandedSupplier by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedSupplier = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedSupplier?.name ?: "اختر المورد (اختياري)")
                        }
                        DropdownMenu(
                            expanded = expandedSupplier,
                            onDismissRequest = { expandedSupplier = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون مورد (مشتريات نقدية عامة)") },
                                onClick = {
                                    viewModel.selectSupplier(null)
                                    expandedSupplier = false
                                }
                            )
                            activeSuppliers.forEach { supp ->
                                DropdownMenuItem(
                                    text = { Text(supp.name) },
                                    onClick = {
                                        viewModel.selectSupplier(supp)
                                        expandedSupplier = false
                                    }
                                )
                            }
                        }
                    }

                    // Add Products Button
                    Button(
                        onClick = { showSelectProductDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة أصناف للشراء")
                    }

                    // Items list
                    Text("الأصناف المضافة (${cartItems.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false).heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(cartItems) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${item.quantity} × ${item.unitPurchasePrice} = ${item.total} ج.م", fontSize = 12.sp, color = Color.DarkGray)
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeCartItem(item.product.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Total & Paid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي الفاتورة:", fontWeight = FontWeight.Bold)
                        Text("${String.format(Locale.US, "%.2f", totalAmount)} ج.م", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value = paidAmount.toString(),
                        onValueChange = { viewModel.setPaidAmount(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("المبلغ المدفوع كاش للمورد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.setNotes(it) },
                        label = { Text("ملاحظات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitPurchase() },
                    enabled = cartItems.isNotEmpty()
                ) {
                    Text("حفظ الفاتورة وزيادة المخزون")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPurchaseDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Select Product Dialog
    if (showSelectProductDialog) {
        AlertDialog(
            onDismissRequest = { showSelectProductDialog = false },
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            title = { Text("اختر صنفاً للإضافة") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allProducts) { prod ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addProductToCart(prod)
                                    showSelectProductDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(prod.name, fontWeight = FontWeight.Bold)
                                    Text("المخزون الحالي: ${prod.quantity} | شراء: ${prod.purchasePrice} ج.م", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                }
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectProductDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Invoice Detail Dialog
    selectedInvoiceDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { selectedInvoiceDetail = null },
            title = { Text("تفاصيل فاتورة الشراء ${detail.invoice.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الإجمالي: ${detail.invoice.total} ج.م", fontWeight = FontWeight.Bold)
                    Text("المدفوع: ${detail.invoice.paidAmount} ج.م")
                    Text("المتبقي: ${detail.invoice.remainingAmount} ج.م", color = if (detail.invoice.remainingAmount > 0) Color.Red else Color.Green)
                    Divider()
                    Text("الأصناف المستلمة بالمخزن:", fontWeight = FontWeight.Bold)
                    detail.items.forEach { item ->
                        Text("• ${item.productNameSnapshot} : ${item.quantity} × ${item.unitPurchasePrice} = ${item.total} ج.م", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedInvoiceDetail = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
