package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.markety.app.ui.viewmodel.BackupViewModel
import com.markety.app.util.ShareHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val backupStatus by viewModel.backupStatus.collectAsState()
    val createdBackupFile by viewModel.createdBackupFile.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf<File?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("النسخ الاحتياطي واستعادة البيانات", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء نسخة احتياطية كاملة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "تتضمن النسخة الاحتياطية كافة الأصناف، الأسعار، الحركات المخزنية، فواتير المبيعات والمشتريات، وديون العملاء والموردين.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.createBackup(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء النسخة الاحتياطية الآن")
                    }
                }
            }

            // Backup Result Card
            createdBackupFile?.let { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("تم إنشاء النسخة بنجاح!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("الملف: ${file.name}", fontSize = 12.sp, color = Color.DarkGray)
                        Text("الحجم: ${file.length() / 1024} كيلوبايت", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    ShareHelper.shareFile(
                                        context = context,
                                        file = file,
                                        mimeType = "application/zip",
                                        title = "مشاركة نسخة احتياطية لماركتي",
                                        targetWhatsApp = true
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("إرسال واتساب")
                            }
                            OutlinedButton(
                                onClick = { showRestoreConfirmDialog = file },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("استعادة الآن")
                            }
                        }
                    }
                }
            }

            // Status message
            backupStatus?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Security note card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE65100))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("أمان البيانات والخصوصية", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "تطبيق «ماركتي» يعمل بدون إنترنت تماماً (Offline-First). كافة البيانات والنسخ الاحتياطية مشفرة ومحفوظة محلياً على هاتفك دون مشاركتها مع أي خوادم خارجية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    showRestoreConfirmDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = null },
            title = { Text("تأكيد استعادة النسخة الاحتياطية") },
            text = {
                Text("تنبيه هام: استعادة النسخة الاحتياطية ستستبدل البيانات الحالية ببيانات الملف المحدد (${file.name}). هل تريد المتابعة؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(context, file)
                        showRestoreConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، استعادة البيانات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
