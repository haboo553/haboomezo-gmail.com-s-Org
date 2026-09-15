package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.markety.app.data.local.entity.UserRole
import com.markety.app.util.SecurityManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onNavigateBack: () -> Unit
) {
    val currentRole by SecurityManager.currentUserRole.collectAsState()
    var showPinChangeDialog by remember { mutableStateOf<UserRole?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الأمان وصلاحيات المستخدمين", fontWeight = FontWeight.Bold) },
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
            // Current User Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentRole == UserRole.MANAGER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("المستخدم النشط حالياً:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (currentRole == UserRole.MANAGER) "مدير النظام (كامل الصلاحيات)" else "كاشير (نقطة البيع والاستعلام)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        if (currentRole == UserRole.MANAGER) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Switch User
            Text("التبديل بين الأدوار", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        SecurityManager.switchUser(UserRole.MANAGER)
                        snackbarMessage = "تم التحويل إلى وضع مدير النظام"
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentRole == UserRole.MANAGER) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                ) {
                    Text("مدير")
                }
                Button(
                    onClick = {
                        SecurityManager.switchUser(UserRole.CASHIER)
                        snackbarMessage = "تم التحويل إلى وضع كاشير"
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentRole == UserRole.CASHIER) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                ) {
                    Text("كاشير")
                }
            }

            // PIN Settings
            Text("إعدادات رمز الدخول (PIN)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("رمز PIN للمدير", fontWeight = FontWeight.Bold)
                            Text("الافتراضي: 1234", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        OutlinedButton(onClick = { showPinChangeDialog = UserRole.MANAGER }) {
                            Text("تغيير")
                        }
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("رمز PIN للكاشير", fontWeight = FontWeight.Bold)
                            Text("الافتراضي: 0000", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        OutlinedButton(onClick = { showPinChangeDialog = UserRole.CASHIER }) {
                            Text("تغيير")
                        }
                    }
                }
            }
        }
    }

    // Change PIN Dialog
    showPinChangeDialog?.let { role ->
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinChangeDialog = null },
            title = { Text("تغيير رمز PIN لـ ${if (role == UserRole.MANAGER) "المدير" else "الكاشير"}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            if (it.length <= 6) newPin = it
                            error = null
                        },
                        label = { Text("الرمز الجديد (4-6 أرقام)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 6) confirmPin = it
                            error = null
                        },
                        label = { Text("تأكيد الرمز الجديد") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
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
                        if (newPin.length < 4) {
                            error = "يجب أن يتكون الرمز من 4 أرقام على الأقل."
                        } else if (newPin != confirmPin) {
                            error = "الرمزان غير متطابقين."
                        } else {
                            if (role == UserRole.MANAGER) {
                                SecurityManager.setManagerPin(newPin)
                            } else {
                                SecurityManager.setCashierPin(newPin)
                            }
                            snackbarMessage = "تم حفظ رمز PIN بنجاح!"
                            showPinChangeDialog = null
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinChangeDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
