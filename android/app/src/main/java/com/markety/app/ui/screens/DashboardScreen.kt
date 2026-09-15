package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markety.app.ui.viewmodel.DashboardViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToDailyOperation: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToPos: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToReturns: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ماركتي | Markety",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "نظام إدارة السوبر ماركت والمبيعات",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDailyOperation) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = "يوم التشغيل",
                            tint = if (uiState.isDayOpen) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "مسح باركود",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Day Status Card (Phase 4)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                if (uiState.isDayOpen) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToDailyOperation() },
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
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "يوم التشغيل مفتوح (وردية نشطة)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF14532D)
                                    )
                                    Text(
                                        text = "رصيد البداية: ${String.format(Locale.US, "%.1f", uiState.openingCash)} ج.م | المتوقع: ${String.format(Locale.US, "%.1f", uiState.expectedCashInDrawer)} ج.م",
                                        fontSize = 12.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = onNavigateToDailyOperation,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF166534),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("إدارة اليوم", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToDailyOperation() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
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
                                Icon(Icons.Default.LockClock, contentDescription = null, tint = Color(0xFFB45309))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "يوم التشغيل مغلق حالياً",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "اضغط هنا لفتح يوم جديد وتسجيل رصيد البداية",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                            Button(
                                onClick = onNavigateToDailyOperation,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309))
                            ) {
                                Text("فتح يوم", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            // Point of Sale Hero Banner
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onNavigateToPos() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "فتح الكاشير (نقطة البيع)",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "مسح الباركود، حساب السلة، والبيع الفوري",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onNavigateToPos,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = "فتح الكاشير")
                        }
                    }
                }
            }

            // Real Daily Sales Metrics (Requirement 13)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مبيعات اليوم (بيانات حقيقية)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToInvoices) {
                        Text("سجل الفواتير", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Sales Metrics Grid 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "إجمالي مبيعات اليوم",
                        value = "${String.format(Locale.US, "%.1f", uiState.todaySalesTotal)} ج.م",
                        subtitle = "إجمالي إيراد اليوم",
                        icon = Icons.Default.MonetizationOn,
                        containerColor = Color(0xFFE0F2FE),
                        contentColor = Color(0xFF0369A1),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToInvoices
                    )
                    MetricCard(
                        title = "عدد الفواتير اليوم",
                        value = "${uiState.todayInvoicesCount}",
                        subtitle = "عملية بيع مكتملة",
                        icon = Icons.Default.ReceiptLong,
                        containerColor = Color(0xFFDCFCE7),
                        contentColor = Color(0xFF15803D),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToInvoices
                    )
                }
            }

            // Sales Metrics Grid 2: Cash & Credit & Items Sold
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "إجمالي الكاش",
                        value = "${String.format(Locale.US, "%.1f", uiState.todayCashTotal)} ج.م",
                        subtitle = "نقدي تم تحصيله",
                        icon = Icons.Default.Payments,
                        containerColor = Color(0xFFECFDF5),
                        contentColor = Color(0xFF047857),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToInvoices
                    )
                    MetricCard(
                        title = "إجمالي الآجل",
                        value = "${String.format(Locale.US, "%.1f", uiState.todayCreditTotal)} ج.م",
                        subtitle = "متبقي على الفواتير",
                        icon = Icons.Default.CreditCard,
                        containerColor = Color(0xFFFEF3C7),
                        contentColor = Color(0xFFB45309),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToInvoices
                    )
                }
            }

            // Phase 4: Expenses & Net Sales Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "المصروفات المسجلة",
                        value = "${String.format(Locale.US, "%.1f", uiState.todayExpensesTotal)} ج.م",
                        subtitle = "خصم من الخزينة",
                        icon = Icons.Default.TrendingDown,
                        containerColor = Color(0xFFFEE2E2),
                        contentColor = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToExpenses
                    )
                    MetricCard(
                        title = "صافي المبيعات",
                        value = "${String.format(Locale.US, "%.1f", uiState.netSalesTotal)} ج.م",
                        subtitle = "المبيعات - المصروفات",
                        icon = Icons.Default.PriceCheck,
                        containerColor = Color(0xFFF3E8FF),
                        contentColor = Color(0xFF7E22CE),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDailyOperation
                    )
                }
            }

            item {
                MetricCard(
                    title = "عدد الأصناف المباعة اليوم",
                    value = "${uiState.todayItemsSoldCount}",
                    subtitle = "قطعة تم بيعها من الأصناف المختلفة",
                    icon = Icons.Default.ShoppingBag,
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = Color(0xFF475569),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToInvoices
                )
            }

            // Inventory Overview Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "نظرة عامة على المخزون",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "إجمالي الأصناف",
                        value = "${uiState.totalProducts}",
                        subtitle = "صنف مسجل",
                        icon = Icons.Default.Inventory2,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = Color(0xFF334155),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProducts
                    )
                    MetricCard(
                        title = "عدد الفئات",
                        value = "${uiState.totalCategories}",
                        subtitle = "قسم رئيسي",
                        icon = Icons.Default.Category,
                        containerColor = Color(0xFFF8FAFC),
                        contentColor = Color(0xFF475569),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCategories
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "إجمالي الكمية",
                        value = "${uiState.totalQuantity}",
                        subtitle = "قطعة في المخزن",
                        icon = Icons.Default.ProductionQuantityLimits,
                        containerColor = Color(0xFFF0FDF4),
                        contentColor = Color(0xFF166534),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProducts
                    )
                    MetricCard(
                        title = "مخزون منخفض",
                        value = "${uiState.lowStockCount}",
                        subtitle = "وصل لحد الطلب",
                        icon = Icons.Default.WarningAmber,
                        containerColor = Color(0xFFFFFBEB),
                        contentColor = Color(0xFF92400E),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProducts
                    )
                }
            }

            item {
                MetricCard(
                    title = "أصناف نفدت بالكامل",
                    value = "${uiState.outOfStockCount}",
                    subtitle = "الكمية = صفر (تحتاج لإعادة طلب عاجل)",
                    icon = Icons.Default.HighlightOff,
                    containerColor = Color(0xFFFEF2F2),
                    contentColor = Color(0xFF991B1B),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToProducts
                )
            }

            // Quick Actions Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "الإجراءات السريعة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onNavigateToDailyOperation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isDayOpen) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.isDayOpen) "إدارة وردية اليوم والخزينة" else "فتح يوم تشغيل جديد",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = onNavigateToPos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نقطة البيع (الكاشير)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToExpenses,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("سجل المصروفات والنثريات", fontSize = 15.sp)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToScanner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مسح باركود صنف بالكاميرا", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToInvoices,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("سجل الفواتير والمبيعات", fontSize = 15.sp)
                    }

                    OutlinedButton(
                        onClick = onNavigateToAddProduct,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة صنف جديد للمخزون", fontSize = 15.sp)
                    }

                    OutlinedButton(
                        onClick = onNavigateToProducts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("عرض وإدارة المخزون والأصناف", fontSize = 15.sp)
                    }
                }
            }

            // Advanced Operations & Relations Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الحسابات والمشتريات والتقارير",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ManagementActionCard(
                        title = "العملاء والحسابات",
                        subtitle = "الديون والتحصيلات",
                        icon = Icons.Default.People,
                        color = Color(0xFFE0F2FE),
                        iconColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCustomers
                    )
                    ManagementActionCard(
                        title = "الموردون والمشتريات",
                        subtitle = "المستحقات والسداد",
                        icon = Icons.Default.LocalShipping,
                        color = Color(0xFFFEF3C7),
                        iconColor = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSuppliers
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ManagementActionCard(
                        title = "شراء بضاعة جديدة",
                        subtitle = "فواتير الشراء للمخزن",
                        icon = Icons.Default.AddShoppingCart,
                        color = Color(0xFFDCFCE7),
                        iconColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPurchases
                    )
                    ManagementActionCard(
                        title = "المرتجعات",
                        subtitle = "مبيعات ومشتريات",
                        icon = Icons.Default.KeyboardReturn,
                        color = Color(0xFFFEE2E2),
                        iconColor = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReturns
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ManagementActionCard(
                        title = "التقارير والأرباح",
                        subtitle = "تصدير PDF و Excel",
                        icon = Icons.Default.Assessment,
                        color = Color(0xFFF3E8FF),
                        iconColor = Color(0xFF7E22CE),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReports
                    )
                    ManagementActionCard(
                        title = "النسخ الاحتياطي",
                        subtitle = "حفظ واستعادة البيانات",
                        icon = Icons.Default.CloudSync,
                        color = Color(0xFFE0E7FF),
                        iconColor = Color(0xFF4338CA),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToBackup
                    )
                }
            }

            item {
                ManagementActionCard(
                    title = "الأمان والصلاحيات (PIN)",
                    subtitle = "التحكم في أدوار المدير والكاشير وتعيين الرموز السرية",
                    icon = Icons.Default.Security,
                    color = Color(0xFFF1F5F9),
                    iconColor = Color(0xFF475569),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToSecurity
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ManagementActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = iconColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
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
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = contentColor.copy(alpha = 0.85f)
            )
        }
    }
}
