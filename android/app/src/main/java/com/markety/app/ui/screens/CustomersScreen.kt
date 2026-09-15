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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markety.app.data.local.dao.CustomerWithBalance
import com.markety.app.data.local.dao.TransactionWithInvoice
import com.markety.app.data.local.entity.CustomerEntity
import com.markety.app.data.local.entity.CustomerTransactionType
import com.markety.app.ui.viewmodel.CustomersViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    onNavigateBack: () -> Unit
) {
    val customers by viewModel.customers.collectAsState()
    val totalDebts by viewModel.totalDebts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTransactions by viewModel.selectedCustomerTransactions.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForPayment by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var customerForStatement by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerWithBalance?>(null) }

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
                title = { Text("العملاء والحسابات الآجلة", fontWeight = FontWeight.Bold) },
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
                    customerToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة عميل")
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
            // Header Debts Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
                            text = "إجمالي ديون العملاء",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalDebts)} ج.م",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
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
                placeholder = { Text("بحث باسم العميل أو رقم الهاتف...") },
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

            // Customers List
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "لا يوجد عملاء مضافون حالياً" else "لا توجد نتائج مطابقة للبحث",
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
                    items(customers, key = { it.supplierOrCustomerId }) { item ->
                        CustomerItemCard(
                            customerWithBalance = item,
                            onEdit = {
                                customerToEdit = item.customer
                                showAddEditDialog = true
                            },
                            onCollectPayment = {
                                customerForPayment = item
                            },
                            onViewStatement = {
                                customerForStatement = item
                                viewModel.selectCustomer(item.customer.id)
                            },
                            onDelete = {
                                customerToDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditCustomerDialog(
            customer = customerToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, phone, address, notes ->
                if (customerToEdit == null) {
                    viewModel.addCustomer(name, phone, address, notes)
                } else {
                    viewModel.updateCustomer(
                        customerToEdit!!.copy(
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

    // Payment Collection Dialog
    customerForPayment?.let { customerItem ->
        CollectPaymentDialog(
            customerName = customerItem.customer.name,
            currentBalance = customerItem.balance,
            onDismiss = { customerForPayment = null },
            onConfirm = { amount, notes ->
                viewModel.collectPayment(customerItem.customer.id, amount, notes)
                customerForPayment = null
            }
        )
    }

    // Customer Statement Dialog
    customerForStatement?.let { customerItem ->
        CustomerStatementDialog(
            customerWithBalance = customerItem,
            transactions = selectedTransactions,
            onDismiss = {
                customerForStatement = null
                viewModel.selectCustomer(null)
            }
        )
    }

    // Delete Confirmation Dialog
    customerToDelete?.let { customerItem ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("تأكيد حذف العميل") },
            text = { Text("هل أنت متأكد من رغبتك في حذف العميل «${customerItem.customer.name}»؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.softDeleteCustomer(customerItem.customer.id)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

private val CustomerWithBalance.supplierOrCustomerId: Long
    get() = this.customer.id

@Composable
fun CustomerItemCard(
    customerWithBalance: CustomerWithBalance,
    onEdit: () -> Unit,
    onCollectPayment: () -> Unit,
    onViewStatement: () -> Unit,
    onDelete: () -> Unit
) {
    val customer = customerWithBalance.customer
    val balance = customerWithBalance.balance

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
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = "هاتف: ${customer.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                    if (customer.address.isNotBlank()) {
                        Text(
                            text = "العنوان: ${customer.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                // Balance Badge
                Surface(
                    color = if (balance > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (balance > 0) "مديونية" else "لا ديون",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", balance)} ج.م",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
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
                        onClick = onCollectPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحصيل دفعة", fontSize = 12.sp)
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
fun AddEditCustomerDialog(
    customer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, address: String, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) "إضافة عميل جديد" else "تعديل بيانات العميل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("اسم العميل *") },
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
fun CollectPaymentDialog(
    customerName: String,
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, notes: String?) -> Unit
) {
    var amountStr by remember { mutableStateOf(currentBalance.toString()) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحصيل دفعة نقدية") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("العميل: $customerName", fontWeight = FontWeight.Bold)
                Text("المديونية الحالية: ${String.format(Locale.US, "%.2f", currentBalance)} ج.م", color = MaterialTheme.colorScheme.error)

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        error = null
                    },
                    label = { Text("مبلغ التحصيل (ج.م) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick full pay button
                Row {
                    FilledTonalButton(
                        onClick = { amountStr = currentBalance.toString() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("سداد كامل المديونية")
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات السند") },
                    placeholder = { Text("مثال: دفعة نقدية مع الكاشير") },
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
                Text("تأكيد التحصيل")
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
fun CustomerStatementDialog(
    customerWithBalance: CustomerWithBalance,
    transactions: List<TransactionWithInvoice>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
        title = {
            Column {
                Text("كشف حساب العميل", fontWeight = FontWeight.Bold)
                Text(customerWithBalance.customer.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("الرصيد المتبقي: ${String.format(Locale.US, "%.2f", customerWithBalance.balance)} ج.م", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        },
        text = {
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد حركات مسجلة لهذا العميل بعد.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { item ->
                        val tx = item.transaction
                        val isSale = tx.type == CustomerTransactionType.SALE
                        val isPayment = tx.type == CustomerTransactionType.PAYMENT

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPayment) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
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
                                            CustomerTransactionType.SALE -> "فاتورة مبيعات آجلة"
                                            CustomerTransactionType.PAYMENT -> "تحصيل دفعة نقدية"
                                            CustomerTransactionType.RETURN -> "مرتجع مبيعات"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    item.invoiceNumber?.let {
                                        Text("رقم الفاتورة: $it", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
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
                                    text = "${if (isPayment) "-" else "+"}${String.format(Locale.US, "%.2f", tx.amount)} ج.م",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPayment) Color(0xFF2E7D32) else Color(0xFFC62828)
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
