package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markety.app.data.local.entity.DailyOperationEntity
import com.markety.app.ui.viewmodel.DailyOperationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyOperationScreen(
    viewModel: DailyOperationViewModel,
    onNavigateToExpenses: () -> Unit,
    onNavigateToPos: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showConfirmCloseDialog by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("hh:mm a - yyyy/MM/dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "إدارة يوم التشغيل والخزينة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = if (uiState.isOpen) "الوردية الحالية مفتوحة" else "لا يوجد يوم مفتوح حالياً",
                            fontSize = 12.sp,
                            color = if (uiState.isOpen) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToExpenses) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "المصروفات")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // SECTION A: If NO Day is Open -> Show "فتح يوم جديد"
            if (!uiState.isOpen) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LockClock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "فتح يوم تشغيل جديد",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "أدخل رصيد البداية الموجود في الخزينة لافتتاح الوردية",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Divider()

                            OutlinedTextField(
                                value = uiState.openingCashInput,
                                onValueChange = { viewModel.onOpeningCashChanged(it) },
                                label = { Text("رصيد البداية في الخزينة (ج.م) *") },
                                placeholder = { Text("مثال: 500") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = uiState.notesInput,
                                onValueChange = { viewModel.onNotesChanged(it) },
                                label = { Text("ملاحظات الافتتاح (اختياري)") },
                                placeholder = { Text("مثال: بداية الوردية الصباحية") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { viewModel.openNewDay() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                enabled = !uiState.isOpening && uiState.openingCashInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isOpening) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.LockOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تأكيد وفتح اليوم", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // SECTION B: Day is OPEN -> Live monitoring, financial calculations, expenses, actual cash, and close day
                val op = uiState.currentOperation!!

                // Status Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(0xFF16A34A), shape = RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "يوم التشغيل مفتوح (وردية نشطة)",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14532D),
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "تاريخ الفتح: ${timeFormatter.format(Date(op.openedAt))}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onNavigateToPos,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF166534),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الكاشير", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Financial Overview Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "رصيد البداية",
                            value = "${String.format(Locale.US, "%.2f", op.openingCash)} ج.م",
                            subtitle = "النقدية الافتتاحية",
                            icon = Icons.Default.AccountBalance,
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF334155),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "مبيعات اليوم",
                            value = "${String.format(Locale.US, "%.2f", uiState.liveMetrics.totalSales)} ج.م",
                            subtitle = "${uiState.liveMetrics.invoicesCount} فاتورة بيع",
                            icon = Icons.Default.MonetizationOn,
                            containerColor = Color(0xFFE0F2FE),
                            contentColor = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToInvoices
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "مبيعات كاش",
                            value = "${String.format(Locale.US, "%.2f", uiState.liveMetrics.cashSales)} ج.م",
                            subtitle = "نقدية محصلة",
                            icon = Icons.Default.Payments,
                            containerColor = Color(0xFFECFDF5),
                            contentColor = Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "مبيعات آجل",
                            value = "${String.format(Locale.US, "%.2f", uiState.liveMetrics.creditSales)} ج.م",
                            subtitle = "ديون على الفواتير",
                            icon = Icons.Default.CreditCard,
                            containerColor = Color(0xFFFEF3C7),
                            contentColor = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Expenses & Net Sales
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "المصروفات",
                            value = "${String.format(Locale.US, "%.2f", uiState.liveMetrics.expensesTotal)} ج.م",
                            subtitle = "خصم من الخزينة",
                            icon = Icons.Default.TrendingDown,
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToExpenses
                        )
                        StatCard(
                            title = "صافي المبيعات",
                            value = "${String.format(Locale.US, "%.2f", uiState.liveMetrics.netSales)} ج.م",
                            subtitle = "المبيعات - المصروفات",
                            icon = Icons.Default.PriceCheck,
                            containerColor = Color(0xFFF3E8FF),
                            contentColor = Color(0xFF7E22CE),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Expected Cash In Register (الرصيد المتوقع)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الرصيد المتوقع في الخزينة",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.2f", uiState.liveMetrics.expectedCash)} ج.م",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "رصيد البداية (${String.format(Locale.US, "%.1f", op.openingCash)}) + الكاش (${String.format(Locale.US, "%.1f", uiState.liveMetrics.cashSales)}) - المصروفات (${String.format(Locale.US, "%.1f", uiState.liveMetrics.expensesTotal)})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Day Closing Form: Actual Cash input + Auto Difference calculation + Close Day button
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "تسوية الخزينة وإغلاق اليوم",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = uiState.actualCashInput,
                                onValueChange = { viewModel.onActualCashChanged(it) },
                                label = { Text("النقدية الفعلية في الدرج (ج.م) *") },
                                placeholder = { Text("أدخل المبلغ الذي قمت بعده فعلياً في الخزينة") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Live Auto Difference Calculation
                            if (uiState.actualCashInput.isNotBlank()) {
                                val diff = uiState.difference
                                val (diffBg, diffTextColor, diffIcon, diffTitle) = when {
                                    kotlin.math.abs(diff) < 0.01 -> Quadruple(
                                        Color(0xFFDCFCE7),
                                        Color(0xFF15803D),
                                        Icons.Default.CheckCircle,
                                        "الخزينة مطابقة تماماً (بدون فرق)"
                                    )
                                    diff > 0 -> Quadruple(
                                        Color(0xFFE0F2FE),
                                        Color(0xFF0369A1),
                                        Icons.Default.AddCircle,
                                        "يوجد فائض في الخزينة (زيادة)"
                                    )
                                    else -> Quadruple(
                                        Color(0xFFFEE2E2),
                                        Color(0xFFB91C1C),
                                        Icons.Default.RemoveCircle,
                                        "يوجد عجز في الخزينة (نقص)"
                                    )
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = diffBg),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(diffIcon, contentDescription = null, tint = diffTextColor)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(diffTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = diffTextColor)
                                                Text("الفارق بين الفعلي والمتوقع", fontSize = 11.sp, color = diffTextColor.copy(alpha = 0.8f))
                                            }
                                        }
                                        Text(
                                            text = "${String.format(Locale.US, "%+.2f", diff)} ج.م",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = diffTextColor
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { showConfirmCloseDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                enabled = !uiState.isClosing && uiState.actualCashInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isClosing) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                                } else {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("إغلاق اليوم وحفظ الحسابات نهائياً", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Summary Card when a day was just closed
            uiState.closedSummary?.let { summary ->
                item {
                    ClosedDaySummaryCard(summary = summary, onDismiss = { viewModel.dismissSummary() })
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Confirmation Dialog before Closing Day
    if (showConfirmCloseDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCloseDialog = false },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تأكيد إغلاق يوم التشغيل", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هل أنت متأكد من رغبتك في إغلاق اليوم؟")
                    Text("• سيتم حساب كافة المبيعات والمصروفات من قاعدة البيانات وتثبيت النتيجة.")
                    Text("• سيتم قفل اليوم نهائياً ولن يُسمح بأي تعديل أو إضافة مصروفات أو بيع على هذه الوردية.")
                    Text("• الرصيد الفعلي المدخل: ${uiState.actualCashInput} ج.م")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCloseDialog = false
                        viewModel.closeDay()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، إغلاق اليوم الآن")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmCloseDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp),
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = contentColor, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            }
            Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = contentColor)
            Text(subtitle, fontSize = 11.sp, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun ClosedDaySummaryCard(
    summary: DailyOperationEntity,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ملخص اليوم المغلق (#${summary.id})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Divider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("تاريخ اليوم:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(summary.date, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("رصيد البداية:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${String.format(Locale.US, "%.2f", summary.openingCash)} ج.م", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("إجمالي المبيعات:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${String.format(Locale.US, "%.2f", summary.totalSales)} ج.م", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("المبيعات الكاش:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${String.format(Locale.US, "%.2f", summary.cashSales)} ج.م", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("المبيعات الآجل:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${String.format(Locale.US, "%.2f", summary.creditSales)} ج.م", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("إجمالي المصروفات:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${String.format(Locale.US, "%.2f", summary.expenses)} ج.م", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("صافي المبيعات:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${String.format(Locale.US, "%.2f", summary.netSales)} ج.م", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الرصيد المتوقع (closingCash):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${String.format(Locale.US, "%.2f", summary.closingCash)} ج.م", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("النقدية الفعلية المدخلة:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${String.format(Locale.US, "%.2f", summary.actualCash)} ج.م", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الفرق (عجز / زيادة):", color = MaterialTheme.colorScheme.onSurfaceVariant)
                val diffColor = if (summary.difference >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                Text("${String.format(Locale.US, "%+.2f", summary.difference)} ج.م", fontWeight = FontWeight.ExtraBold, color = diffColor)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
