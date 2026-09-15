package com.markety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markety.app.data.local.dao.CategoryWithCount
import com.markety.app.data.local.entity.CategoryEntity
import com.markety.app.ui.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoryViewModel,
    modifier: Modifier = Modifier
) {
    val categoriesWithCount by viewModel.categoriesWithCount.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryWithCount?>(null) }

    LaunchedEffect(message, error) {
        if (message != null || error != null) {
            // Auto clear after brief delay
            kotlinx.coroutines.delay(4000)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("فئات وأقسام السوبر ماركت", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                actions = {
                    Text(
                        text = "${categoriesWithCount.size} فئة",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة فئة جديدة", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Alerts / Feedback
            if (message != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(message ?: "", color = Color(0xFF15803D), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
            }

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(error ?: "", color = Color(0xFFB91C1C), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
            }

            if (categoriesWithCount.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("لا توجد أي فئات مسجلة بعد", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categoriesWithCount, key = { it.category.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF3E8FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFF7E22CE),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = item.category.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "${item.productCount} أصناف مرتبطة",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { categoryToEdit = item.category }) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل الفئة", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { categoryToDelete = item }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الفئة", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة فئة جديدة", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("اسم الفئة *") },
                    placeholder = { Text("مثال: منظفات، مشروبات، مجمدات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Category Dialog
    if (categoryToEdit != null) {
        var editName by remember { mutableStateOf(categoryToEdit?.name ?: "") }
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("تعديل اسم الفئة", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("اسم الفئة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToEdit?.let { viewModel.updateCategory(it, editName) }
                        categoryToEdit = null
                    }
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Category Confirm Dialog
    if (categoryToDelete != null) {
        val count = categoryToDelete?.productCount ?: 0
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("تأكيد حذف الفئة", fontWeight = FontWeight.Bold) },
            text = {
                if (count > 0) {
                    Text("لا يمكن حذف الفئة \"${categoryToDelete?.category?.name}\" لأنها تحتوي على $count أصناف مرتبطة بها في المخزون. يرجى نقل أو حذف الأصناف أولاً.")
                } else {
                    Text("هل أنت متأكد من حذف فئة \"${categoryToDelete?.category?.name}\"؟ لن يؤثر هذا على أي صنف حيث أنها خالية.")
                }
            },
            confirmButton = {
                if (count == 0) {
                    Button(
                        onClick = {
                            categoryToDelete?.let { viewModel.deleteCategory(it.category, it.productCount) }
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("حذف الفئة")
                    }
                } else {
                    Button(onClick = { categoryToDelete = null }) {
                        Text("حسناً")
                    }
                }
            },
            dismissButton = {
                if (count == 0) {
                    TextButton(onClick = { categoryToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }
}
