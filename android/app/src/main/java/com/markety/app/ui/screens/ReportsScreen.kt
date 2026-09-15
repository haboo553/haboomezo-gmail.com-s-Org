package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markety.app.ui.viewmodel.ReportsViewModel
import com.markety.app.util.ShareHelper
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val timeframe by viewModel.selectedTimeframe.collectAsState()
    val salesTotal by viewModel.salesTotal.collectAsState()
    val cashSalesTotal by viewModel.cashSalesTotal.collectAsState()
    val creditSalesTotal by viewModel.creditSalesTotal.collectAsState()
    val invoicesCount by viewModel.invoicesCount.collectAsState()
    val purchasesTotal by viewModel.purchasesTotal.collectAsState()
    val customerDebts by viewModel.totalCustomerDebts.collectAsState()
    val supplierDebts by viewModel.totalSupplierDebts.collectAsState()
    val lowStockList by viewModel.lowStockProducts.collectAsState()

    val exportedPdf by viewModel.exportedPdfFile.collectAsState()
    val exportedCsv by viewModel.exportedCsvFile.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportedPdf) {
        exportedPdf?.let { file ->
            snackbarHostState.showSnackbar("تم استخراج ملف PDF بنجاح")
            ShareHelper.shareFile(context, file, mimeType = "application/pdf", title = "مشاركة تقرير ماركتي")
            viewModel.clearExportedFiles()
        }
    }

    LaunchedEffect(exportedCsv) {
        exportedCsv?.let { file ->
            snackbarHostState.showSnackbar("تم استخراج ملف Excel/CSV بنجاح")
            ShareHelper.shareFile(context, file, mimeType = "text/csv", title = "مشاركة تقرير إكسل")
            viewModel.clearExportedFiles()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير الشاملة والأرباح", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportReportToPdf(context) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير PDF")
                    }
                    IconButton(onClick = { viewModel.exportSalesToCsv(context) }) {
                        Icon(Icons.Default.TableChart, contentDescription = "تصدير Excel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            // Timeframe selection tabs
            TabRow(selectedTabIndex = timeframe) {
                Tab(
                    selected = timeframe == 0,
                    onClick = { viewModel.setTimeframe(0) },
                    text = { Text("اليوم") }
                )
                Tab(
                    selected = timeframe == 1,
                    onClick = { viewModel.setTimeframe(1) },
                    text = { Text("هذا الشهر") }
                )
                Tab(
                    selected = timeframe == 2,
                    onClick = { viewModel.setTimeframe(2) },
                    text = { Text("كل الفترات") }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Export Action Buttons Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.exportReportToPdf(context) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير PDF", fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.exportSalesToCsv(context) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير Excel", fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    // Sales Metrics
                    Text("مؤشرات المبيعات", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "إجمالي المبيعات",
                            value = "${String.format(Locale.US, "%.2f", salesTotal)} ج.م",
                            subtitle = "$invoicesCount فاتورة",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "المبيعات النقدية (كاش)",
                            value = "${String.format(Locale.US, "%.2f", cashSalesTotal)} ج.م",
                            subtitle = "سيولة الخزينة",
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "المبيعات الآجلة",
                            value = "${String.format(Locale.US, "%.2f", creditSalesTotal)} ج.م",
                            subtitle = "حسابات العملاء",
                            color = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "إجمالي المشتريات",
                            value = "${String.format(Locale.US, "%.2f", purchasesTotal)} ج.م",
                            subtitle = "تكلفة البضاعة",
                            color = Color(0xFF1565C0),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    // Debts Metrics
                    Text("الذمم والديون المتبقية", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "ديون العملاء (لنا)",
                            value = "${String.format(Locale.US, "%.2f", customerDebts)} ج.م",
                            subtitle = "مبالغ مستحقة للتحصيل",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "مستحقات الموردين (علينا)",
                            value = "${String.format(Locale.US, "%.2f", supplierDebts)} ج.م",
                            subtitle = "واجبة السداد للموردين",
                            color = Color(0xFF6A1B9A),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    // Low stock alert section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تنبيهات النواقص بالمخزن", fontWeight = FontWeight.Bold)
                                }
                                Text("${lowStockList.size} صنف", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            }
                            if (lowStockList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))
                                lowStockList.take(5).forEach { prod ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(prod.name, fontSize = 13.sp)
                                        Text("متبقي: ${prod.quantity} (الحد: ${prod.minStockAlert})", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
        }
    }
}
