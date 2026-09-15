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
import com.markety.app.data.local.dao.SupplierWithBalance
import com.markety.app.data.local.entity.SupplierEntity
import com.markety.app.data.local.entity.SupplierTransactionEntity
import com.markety.app.data.local.entity.SupplierTransactionType
import com.markety.app.ui.viewmodel.SuppliersViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: SuppliersViewModel,
    onNavigateBack: () -> Unit
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val totalDebts by viewModel.totalSupplierDebts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTransactions by viewModel.selectedSupplierTransactions.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierForPayment by remember { mutableStateOf<SupplierWithBalance?>(null) }
    var supplierForStatement by remember { mutableStateOf<SupplierWithBalance?>(null) }
    var supplierToDelete by remember { mutableStateOf<SupplierWithBalance?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الموردون والمستحقات", fontWeight = FontWeight.Bold) },
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
            FloatingActionButton(
                onClick = {
                    supplierToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مورد")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            // Header Dues Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إجمالي مستحقات الموردين",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalDebts)} ج.م",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("بحث باسم المورد أو الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Suppliers List
            if (suppliers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) "لا يوجد موردون مسجلون حالياً" else "لا توجد نتائج مطابقة",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(suppliers, key = { it.supplier.id }) { item ->
                        SupplierItemCard(
                            supplierWithBalance = item,
                            onEdit = {
                                supplierToEdit = item.supplier
                                showAddEditDialog = true
                            },
                            onPay = {
                                supplierForPayment = item
                            },
                            onViewStatement = {
                                supplierForStatement = item
                                viewModel.selectSupplier(item.supplier.id)
                            },
                            onDelete = {
                                supplierToDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditSupplierDialog(
            supplier = supplierToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, phone, address, notes ->
                if (supplierToEdit == null) {
                    viewModel.addSupplier(name, phone, address, notes)
                } else {
                    viewModel.updateSupplier(
                        supplierToEdit!!.copy(
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes
                        )
                    )
                }
                showAddEditDialog = false
            }
        )
    }

    // Pay Supplier Dialog
    supplierForPayment?.let { supplierItem ->
        PaySupplierDialog(
            supplierName = supplierItem.supplier.name,
            currentBalance = supplierItem.balance,
            onDismiss = { supplierForPayment = null },
            onConfirm = { amount, notes ->
                viewModel.paySupplier(supplierItem.supplier.id, amount, notes)
                supplierForPayment = null
            }
        )
    }

    // Supplier Statement Dialog
    supplierForStatement?.let { supplierItem ->
        SupplierStatementDialog(
            supplierWithBalance = supplierItem,
            transactions = selectedTransactions,
            onDismiss = {
                supplierForStatement = null
                viewModel.selectSupplier(null)
            }
        )
    }

    // Delete Confirmation Dialog
    supplierToDelete?.let { supplierItem ->
        AlertDialog(
            onDismissRequest = { supplierToDelete = null },
            title = { Text("تأكيد حذف المورد") },
            text = { Text("هل أنت متأكد من حذف المورد «${supplierItem.supplier.name}»؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.softDeleteSupplier(supplierItem.supplier.id)
                        supplierToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { supplierToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun SupplierItemCard(
    supplierWithBalance: SupplierWithBalance,
    onEdit: () -> Unit,
    onPay: () -> Unit,
    onViewStatement: () -> Unit,
    onDelete: () -> Unit
) {
    val supplier = supplierWithBalance.supplier
    val balance = supplierWithBalance.balance

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewStatement() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = supplier.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (supplier.phone.isNotBlank()) {
                        Text(
                            text = "هاتف: ${supplier.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                    if (supplier.address.isNotBlank()) {
                        Text(
                            text = "العنوان: ${supplier.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Balance Badge
                Surface(
                    color = if (balance > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (balance > 0) "مستحق له" else "مسدد",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (balance > 0) MaterialTheme.colorScheme.error else Color.DarkGray
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", balance)} ج.م",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (balance > 0) MaterialTheme.colorScheme.error else Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (balance > 0) {
                    Button(
                        onClick = onPay,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سداد دفعة", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                OutlinedButton(
                    onClick = onViewStatement,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("كشف حساب", fontSize = 12.sp)
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddEditSupplierDialog(
    supplier: SupplierEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supplier == null) "إضافة مورد جديد" else "تعديل بيانات المورد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("اسم المورد أو الشركة *") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onSave(name.trim(), phone.trim(), address.trim(), notes.trim().ifBlank { null })
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun PaySupplierDialog(
    supplierName: String,
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, notes: String?) -> Unit
) {
    var amountStr by remember { mutableStateOf(currentBalance.toString()) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سداد دفعة للمورد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المورد: $supplierName", fontWeight = FontWeight.Bold)
                Text("المستحق له حالياً: ${String.format(Locale.US, "%.2f", currentBalance)} ج.م", color = MaterialTheme.colorScheme.error)

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        error = null
                    },
                    label = { Text("المبلغ المدفوع (ج.م) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )

                Row {
                    FilledTonalButton(
                        onClick = { amountStr = currentBalance.toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("سداد كامل المبلغ")
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات السداد") },
                    placeholder = { Text("مثال: شيك، نقدي، تحويل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        error = "يرجى إدخال مبلغ صحيح أكبر من صفر."
                    } else {
                        onConfirm(amount, notes.ifBlank { null })
                    }
                }
            ) {
                Text("تأكيد السداد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun SupplierStatementDialog(
    supplierWithBalance: SupplierWithBalance,
    transactions: List<SupplierTransactionEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
        title = {
            Column {
                Text("كشف حساب المورد", fontWeight = FontWeight.Bold)
                Text(supplierWithBalance.supplier.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("الرصيد المستحق: ${String.format(Locale.US, "%.2f", supplierWithBalance.balance)} ج.م", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        },
        text = {
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد حركات مسجلة لهذا المورد بعد.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { tx ->
                        val isPayment = tx.type == SupplierTransactionType.PAYMENT
                        val isReturn = tx.type == SupplierTransactionType.RETURN

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPayment || isReturn) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (tx.type) {
                                            SupplierTransactionType.PURCHASE -> "فاتورة مشتريات (مستحق)"
                                            SupplierTransactionType.PAYMENT -> "سداد دفعة للمورد"
                                            SupplierTransactionType.RETURN -> "مرتجع مشتريات"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    tx.purchaseInvoiceId?.let {
                                        Text("رقم الفاتورة: #$it", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                    }
                                    tx.notes?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    Text(
                                        dateFormat.format(Date(tx.dateTime)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }

                                Text(
                                    text = "${if (isPayment || isReturn) "-" else "+"}${String.format(Locale.US, "%.2f", tx.amount)} ج.م",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPayment || isReturn) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
