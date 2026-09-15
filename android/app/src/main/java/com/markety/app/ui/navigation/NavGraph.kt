package com.markety.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.markety.app.MarketyApp
import com.markety.app.ui.screens.*
import com.markety.app.ui.viewmodel.*

sealed class Screen(val route: String, val title: String? = null, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "الرئيسة", Icons.Default.Dashboard)
    object DailyOperation : Screen("daily_operation", "اليوم", Icons.Default.Today)
    object Pos : Screen("pos", "الكاشير", Icons.Default.PointOfSale)
    object Invoices : Screen("invoices", "المبيعات", Icons.Default.ReceiptLong)
    object Products : Screen("products", "الأصناف", Icons.Default.Inventory2)
    object Categories : Screen("categories", "الفئات", Icons.Default.Category)
    object Expenses : Screen("expenses", "المصروفات", Icons.Default.AccountBalanceWallet)
    object Customers : Screen("customers", "العملاء", Icons.Default.People)
    object Suppliers : Screen("suppliers", "الموردون", Icons.Default.LocalShipping)
    object Purchases : Screen("purchases", "المشتريات", Icons.Default.AddShoppingCart)
    object Returns : Screen("returns", "المرتجعات", Icons.Default.KeyboardReturn)
    object Reports : Screen("reports", "التقارير", Icons.Default.Assessment)
    object Backup : Screen("backup", "النسخ الاحتياطي", Icons.Default.CloudSync)
    object Security : Screen("security", "الأمان", Icons.Default.Security)

    object Scanner : Screen("barcode_scanner?source={source}") {
        fun createRoute(source: String = "inventory") = "barcode_scanner?source=$source"
    }

    object AddProduct : Screen("product_add?barcode={barcode}") {
        fun createRoute(barcode: String = "") = if (barcode.isNotBlank()) "product_add?barcode=$barcode" else "product_add"
    }

    object EditProduct : Screen("product_edit/{productId}") {
        fun createRoute(productId: Long) = "product_edit/$productId"
    }

    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: Long) = "product_detail/$productId"
    }

    object InvoiceDetail : Screen("invoice_detail/{invoiceId}") {
        fun createRoute(invoiceId: Long) = "invoice_detail/$invoiceId"
    }
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.DailyOperation,
    Screen.Pos,
    Screen.Invoices,
    Screen.Products
)

@Composable
fun MainAppScaffold(
    app: MarketyApp,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavScreens.any { it.route == currentRoute }

    // Retained PosViewModel so POS state & cart are preserved during camera scanner transitions
    val posViewModel = remember { PosViewModel(app.productRepository, app.salesRepository) }
    val invoicesViewModel = remember { InvoicesViewModel(app.salesRepository) }
    val dailyOperationViewModel = remember { DailyOperationViewModel(app.dailyOperationRepository, app.salesRepository) }
    val expensesViewModel = remember { ExpensesViewModel(app.dailyOperationRepository) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title!!) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val vm = remember { DashboardViewModel(app.productRepository, app.categoryRepository, app.salesRepository, app.dailyOperationRepository) }
                DashboardScreen(
                    viewModel = vm,
                    onNavigateToDailyOperation = { navController.navigate(Screen.DailyOperation.route) },
                    onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                    onNavigateToPos = { navController.navigate(Screen.Pos.route) },
                    onNavigateToInvoices = { navController.navigate(Screen.Invoices.route) },
                    onNavigateToProducts = { navController.navigate(Screen.Products.route) },
                    onNavigateToAddProduct = { navController.navigate(Screen.AddProduct.createRoute()) },
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                    onNavigateToScanner = { navController.navigate(Screen.Scanner.createRoute("inventory")) },
                    onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                    onNavigateToSuppliers = { navController.navigate(Screen.Suppliers.route) },
                    onNavigateToPurchases = { navController.navigate(Screen.Purchases.route) },
                    onNavigateToReturns = { navController.navigate(Screen.Returns.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToSecurity = { navController.navigate(Screen.Security.route) }
                )
            }

            composable(Screen.DailyOperation.route) {
                DailyOperationScreen(
                    viewModel = dailyOperationViewModel,
                    onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                    onNavigateToPos = { navController.navigate(Screen.Pos.route) },
                    onNavigateToInvoices = { navController.navigate(Screen.Invoices.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Expenses.route) {
                ExpensesScreen(
                    viewModel = expensesViewModel,
                    onNavigateToDailyOperation = { navController.navigate(Screen.DailyOperation.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Pos.route) {
                PosScreen(
                    viewModel = posViewModel,
                    onNavigateToScanner = { navController.navigate(Screen.Scanner.createRoute("pos")) },
                    onNavigateToAddProductWithBarcode = { barcode ->
                        navController.navigate(Screen.AddProduct.createRoute(barcode))
                    }
                )
            }

            composable(Screen.Invoices.route) {
                InvoicesScreen(
                    viewModel = invoicesViewModel,
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.InvoiceDetail.createRoute(id))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.InvoiceDetail.route,
                arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getLong("invoiceId") ?: 0L
                InvoiceDetailScreen(
                    invoiceId = invoiceId,
                    viewModel = invoicesViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Products.route) {
                val vm = ProductViewModel(app.productRepository, app.categoryRepository)
                ProductsScreen(
                    viewModel = vm,
                    onNavigateToAddProduct = { navController.navigate(Screen.AddProduct.createRoute()) },
                    onNavigateToProductDetail = { id ->
                        navController.navigate(Screen.ProductDetail.createRoute(id))
                    },
                    onNavigateToScanner = { navController.navigate(Screen.Scanner.createRoute("inventory")) }
                )
            }

            composable(Screen.Categories.route) {
                val vm = CategoryViewModel(app.categoryRepository)
                CategoriesScreen(viewModel = vm)
            }

            // Real CameraX + ML Kit Barcode Scanner supporting Inventory and POS modes
            composable(
                route = Screen.Scanner.route,
                arguments = listOf(
                    navArgument("source") {
                        type = NavType.StringType
                        defaultValue = "inventory"
                    }
                )
            ) { backStackEntry ->
                val sourceArg = backStackEntry.arguments?.getString("source") ?: "inventory"
                val isPos = sourceArg == "pos"

                BarcodeScannerScreen(
                    productRepository = app.productRepository,
                    isPosMode = isPos,
                    onNavigateToProductDetail = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId)) {
                            popUpTo(Screen.Scanner.route) { inclusive = true }
                        }
                    },
                    onNavigateToAddProductWithBarcode = { barcode ->
                        navController.navigate(Screen.AddProduct.createRoute(barcode)) {
                            popUpTo(Screen.Scanner.route) { inclusive = true }
                        }
                    },
                    onBarcodeResult = if (isPos) {
                        { barcode ->
                            posViewModel.onBarcodeScanned(barcode)
                            navController.popBackStack()
                        }
                    } else null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddProduct.route,
                arguments = listOf(
                    navArgument("barcode") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val barcodeArg = backStackEntry.arguments?.getString("barcode") ?: ""
                val vm = AddEditProductViewModel(app.productRepository, app.categoryRepository)
                AddEditProductScreen(
                    viewModel = vm,
                    productId = 0,
                    initialBarcode = barcodeArg,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanBarcode = { navController.navigate(Screen.Scanner.createRoute("inventory")) }
                )
            }

            composable(
                route = Screen.EditProduct.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                val vm = AddEditProductViewModel(app.productRepository, app.categoryRepository)
                AddEditProductScreen(
                    viewModel = vm,
                    productId = productId,
                    initialBarcode = "",
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToScanBarcode = { navController.navigate(Screen.Scanner.createRoute("inventory")) }
                )
            }

            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
                val vm = ProductDetailViewModel(productId, app.productRepository, app.stockRepository)
                ProductDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditProduct.createRoute(id)) }
                )
            }

            composable(Screen.Customers.route) {
                val vm = remember { CustomersViewModel(app.customerRepository) }
                CustomersScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Suppliers.route) {
                val vm = remember { SuppliersViewModel(app.supplierRepository) }
                SuppliersScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Purchases.route) {
                val vm = remember { PurchasesViewModel(app.purchaseRepository, app.productRepository, app.supplierRepository) }
                PurchasesScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Returns.route) {
                val vm = remember { ReturnsViewModel(app.returnRepository, app.salesRepository, app.purchaseRepository) }
                ReturnsScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Reports.route) {
                val vm = remember {
                    ReportsViewModel(
                        salesDao = app.database.salesDao(),
                        productDao = app.database.productDao(),
                        stockMovementDao = app.database.stockMovementDao(),
                        customerRepository = app.customerRepository,
                        supplierRepository = app.supplierRepository,
                        purchaseRepository = app.purchaseRepository,
                        dailyOperationRepository = app.dailyOperationRepository
                    )
                }
                ReportsScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Backup.route) {
                val vm = remember { BackupViewModel(app.database) }
                BackupScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Security.route) {
                SecurityScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
