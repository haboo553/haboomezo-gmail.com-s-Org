package com.markety.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markety.app.data.local.dao.InvoiceWithItems
import com.markety.app.data.local.entity.PaymentType
import com.markety.app.data.local.entity.SaleItemEntity
import com.markety.app.ui.viewmodel.InvoicesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    viewModel: InvoicesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(invoiceId) {
        viewModel.selectInvoice(invoiceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.selectedInvoiceWithItems?.invoice?.invoiceNumber ?: "تفاصيل الفاتورة",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoadingDetail || uiState.selectedInvoiceWithItems == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val invoiceWithItems = uiState.selectedInvoiceWithItems!!
            val invoice = invoiceWithItems.invoice
            val items = invoiceWithItems.items

            val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")) }
            val formattedDate = remember(invoice.dateTime) {
                dateFormatter.format(Date(invoice.dateTime))
            }

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = invoice.invoiceNumber,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Surface(
                                    color = if (invoice.paymentType == PaymentType.CASH) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = invoice.paymentType.arabicLabel,
                                        color = if (invoice.paymentType == PaymentType.CASH) Color(0xFF047857) else Color(0xFFB45309),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Text(
                                text = "تاريخ الإنشاء: $formattedDate",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            if (!invoice.notes.isNullOrBlank()) {
                                Text(
                                    text = "ملاحظات: ${invoice.notes}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Items Section Title
                item {
                    Text(
                        text = "الأصناف المباعة (${items.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // List of items
                items(items, key = { it.id }) { saleItem ->
                    SaleItemDetailRow(saleItem = saleItem)
                }

                // Financial Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("ملخص الفاتورة المالي", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            HorizontalDivider()

                            DetailSummaryRow(label = "الإجمالي قبل الخصم", value = "${String.format(Locale.US, "%.2f", invoice.subtotal)} ج.م")
                            if (invoice.discount > 0) {
                                DetailSummaryRow(
                                    label = "الخصم",
                                    value = "- ${String.format(Locale.US, "%.2f", invoice.discount)} ج.م",
                                    valueColor = Color(0xFF10B981)
                                )
                            }
                            DetailSummaryRow(
                                label = "الإجمالي النهائي",
                                value = "${String.format(Locale.US, "%.2f", invoice.total)} ج.م",
                                isBold = true,
                                valueColor = MaterialTheme.colorScheme.primary
                            )
                            DetailSummaryRow(
                                label = "المبلغ المدفوع",
                                value = "${String.format(Locale.US, "%.2f", invoice.paidAmount)} ج.م"
                            )

                            if (invoice.paymentType == PaymentType.CREDIT && invoice.remainingAmount > 0) {
                                DetailSummaryRow(
                                    label = "المتبقي (آجل)",
                                    value = "${String.format(Locale.US, "%.2f", invoice.remainingAmount)} ج.م",
                                    isBold = true,
                                    valueColor = MaterialTheme.colorScheme.error
                                )
                            } else if (invoice.paymentType == PaymentType.CASH && invoice.paidAmount > invoice.total) {
                                DetailSummaryRow(
                                    label = "الباقي للعميل",
                                    value = "${String.format(Locale.US, "%.2f", invoice.paidAmount - invoice.total)} ج.م",
                                    isBold = true,
                                    valueColor = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaleItemDetailRow(
    saleItem: SaleItemEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = saleItem.productNameSnapshot,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (!saleItem.barcodeSnapshot.isNullOrBlank()) {
                    Text(
                        text = "الباركود: ${saleItem.barcodeSnapshot}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }
                Text(
                    text = "${saleItem.quantity} × ${String.format(Locale.US, "%.2f", saleItem.unitSellingPrice)} ج.م",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "${String.format(Locale.US, "%.2f", saleItem.total)} ج.م",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF0F766E)
            )
        }
    }
}

@Composable
fun DetailSummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = if (isBold) 15.sp else 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}
