package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markety.app.data.local.entity.ReturnType
import com.markety.app.ui.viewmodel.ReturnsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnsScreen(
    viewModel: ReturnsViewModel,
    onNavigateBack: () -> Unit
) {
    val returnType by viewModel.returnType.collectAsState()
    val invoiceQuery by viewModel.invoiceNumberQuery.collectAsState()
    val returnItems by viewModel.returnItems.collectAsState()
    val reason by viewModel.reason.collectAsState()
    val returnsHistory by viewModel.returns.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successResult by viewModel.successResult.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: New Return, 1: Return Log
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(successResult) {
        successResult?.let {
            snackbarHostState.showSnackbar("تم تسجيل فاتورة الإرجاع رقم ${it.invoice.returnInvoiceNumber} وتعديل المخزون بنجاح!")
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المرتجعات (مبيعات / مشتريات)", fontWeight = FontWeight.Bold) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("تسجيل مرتجع جديد") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("سجل فواتير المرتجع") }
                )
            }

            if (activeTab == 0) {
                // New Return Form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = returnType == ReturnType.SALES_RETURN,
                            onClick = { viewModel.setReturnType(ReturnType.SALES_RETURN) },
                            label = { Text("مرتجع مبيعات (من عميل)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = returnType == ReturnType.PURCHASE_RETURN,
                            onClick = { viewModel.setReturnType(ReturnType.PURCHASE_RETURN) },
                            label = { Text("مرتجع مشتريات (إلى مورد)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Invoice Search
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = invoiceQuery,
                            onValueChange = { viewModel.setInvoiceNumberQuery(it) },
                            label = { Text(if (returnType == ReturnType.SALES_RETURN) "رقم فاتورة المبيعات" else "رقم فاتورة الشراء") },
                            placeholder = { Text("مثال: 1 أو PO-2026...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.searchInvoice() },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("بحث")
                        }
                    }

                    // Items to return
                    if (returnItems.isNotEmpty()) {
                        Text("حدد الأصناف المراد إرجاعها:", fontWeight = FontWeight.Bold)

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(returnItems) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleItemSelection(item.productId) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (item.isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.isSelected,
                                            onCheckedChange = { viewModel.toggleItemSelection(item.productId) }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.productName, fontWeight = FontWeight.Bold)
                                            Text("السعر: ${item.unitPrice} ج.م | الحد الأقصى: ${item.maxQuantity}", style = MaterialTheme.typography.bodySmall)
                                        }

                                        if (item.isSelected) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { viewModel.updateReturnQuantity(item.productId, item.returnQuantity - 1) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = null)
                                                }
                                                Text("${item.returnQuantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                                                IconButton(
                                                    onClick = { viewModel.updateReturnQuantity(item.productId, item.returnQuantity + 1) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = reason,
                            onValueChange = { viewModel.setReason(it) },
                            label = { Text("سبب الإرجاع (اختياري)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { viewModel.submitReturn() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.KeyboardReturn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تأكيد تسجيل المرتجع وتحديث المخزون")
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("ابحث برقم الفاتورة لعرض الأصناف القابلة للإرجاع", color = Color.Gray)
                        }
                    }
                }
            } else {
                // History
                if (returnsHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا يوجد سجل مرتجعات سابق", color = Color.Gray)
                    }
                } else {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(returnsHistory) { item ->
                            val inv = item.invoice
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(inv.returnInvoiceNumber, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${String.format(Locale.US, "%.2f", inv.total)} ج.م",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (inv.returnType == ReturnType.SALES_RETURN) "مرتجع مبيعات" else "مرتجع مشتريات",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "الفاتورة الأصلية: ${inv.originalInvoiceNumber} | ${dateFormat.format(Date(inv.dateTime))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
