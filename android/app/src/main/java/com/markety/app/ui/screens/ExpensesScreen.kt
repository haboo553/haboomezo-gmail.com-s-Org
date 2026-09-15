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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markety.app.data.local.entity.ExpenseEntity
import com.markety.app.ui.viewmodel.ExpensesViewModel
import com.markety.app.ui.viewmodel.defaultExpenseCategories
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel,
    onNavigateToDailyOperation: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeFormatter = remember { SimpleDateFormat("hh:mm a - yyyy/MM/dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "سجل المصروفات والنثريات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = if (uiState.openOperation != null) "اليوم المفتوح #${uiState.openOperation?.id} (${uiState.openOperation?.date})" else "لا يوجد يوم مفتوح حالياً",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDailyOperation) {
                        Icon(Icons.Default.Today, contentDescription = "شاشة اليوم")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة مصروف جديد", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Error banner
            uiState.errorMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.dismissMessages() }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                    }
                }
            }

            // Success banner
            uiState.successMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF166534))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = msg, color = Color(0xFF166534), modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.dismissMessages() }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                    }
                }
            }

            // Total Expenses Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "إجمالي المصروفات المسجلة",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.2f", uiState.totalExpenses)} ج.م",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB91C1C)
                            )
                            Text(
                                text = "${uiState.expenses.size} عملية صرف مسجلة",
                                fontSize = 11.sp,
                                color = Color(0xFF991B1B).copy(alpha = 0.8f)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.openAddDialog() },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFDC2626)
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "إضافة مصروف")
                        }
                    }
                }
            }

            // Notice if no day is open
            if (uiState.openOperation == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تنبيه: يوم التشغيل مغلق", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                                Text("يجب فتح يوم جديد من شاشة اليوم لربط المصروفات بالخزينة وخصمها من الوردية.", fontSize = 12.sp, color = Color(0xFFB45309))
                            }
                            TextButton(onClick = onNavigateToDailyOperation) {
                                Text("فتح يوم", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // List Header
            item {
                Text(
                    text = "قائمة المصروفات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (uiState.expenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("لا توجد مصروفات مسجلة حتى الآن", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(uiState.expenses, key = { it.id }) { expense ->
                    ExpenseItemCard(expense = expense, timeFormatter = timeFormatter)
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add Expense Dialog
    if (uiState.showAddDialog) {
        var expandedCategoryDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.closeAddDialog() },
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تسجيل مصروف جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Category Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.categoryInput,
                            onValueChange = { viewModel.onCategoryChanged(it) },
                            label = { Text("التصنيف *") },
                            trailingIcon = {
                                IconButton(onClick = { expandedCategoryDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expandedCategoryDropdown,
                            onDismissRequest = { expandedCategoryDropdown = false }
                        ) {
                            defaultExpenseCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        viewModel.onCategoryChanged(cat)
                                        expandedCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount Input
                    OutlinedTextField(
                        value = uiState.amountInput,
                        onValueChange = { viewModel.onAmountChanged(it) },
                        label = { Text("قيمة المصروف (ج.م) *") },
                        placeholder = { Text("مثال: 50.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Notes Input
                    OutlinedTextField(
                        value = uiState.notesInput,
                        onValueChange = { viewModel.onNotesChanged(it) },
                        label = { Text("ملاحظات (اختياري)") },
                        placeholder = { Text("مثال: فاتورة كهرباء شهر سبتمبر") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "• سيتم ربط المصروف تلقائياً باليوم المفتوح #${uiState.openOperation?.id} وخصمه من الرصيد المتوقع في الخزينة.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addExpense() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !uiState.isSubmitting && uiState.amountInput.isNotBlank()
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("حفظ المصروف")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.closeAddDialog() }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    timeFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFDC2626))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = expense.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    expense.notes?.let { note ->
                        Text(
                            text = note,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = timeFormatter.format(Date(expense.date)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Text(
                text = "${String.format(Locale.US, "-%.2f", expense.amount)} ج.م",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFFDC2626)
            )
        }
    }
}
