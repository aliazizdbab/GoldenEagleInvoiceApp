package com.goldeneagle.invoice

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.goldeneagle.invoice.ui.screens.CatalogScreen
import com.goldeneagle.invoice.ui.screens.CustomersScreen
import com.goldeneagle.invoice.ui.screens.InvoiceEditorScreen
import com.goldeneagle.invoice.ui.screens.InvoicesListScreen
import com.goldeneagle.invoice.ui.screens.PdfPreviewScreen
import com.goldeneagle.invoice.ui.screens.SettingsScreen
import com.goldeneagle.invoice.ui.theme.CardWhite
import com.goldeneagle.invoice.ui.theme.GoldenEagleTheme
import com.goldeneagle.invoice.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoldenEagleTheme {
                GoldenEagleMainApp()
            }
        }
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val mainTabs = listOf(
    BottomTab("invoices", "الفواتير", Icons.Default.Description),
    BottomTab("catalog", "الأصناف", Icons.Default.Inventory2),
    BottomTab("customers", "الزبائن", Icons.Default.People),
    BottomTab("settings", "الإعدادات", Icons.Default.Settings)
)

@Composable
private fun GoldenEagleMainApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = mainTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = CardWhite) {
                    mainTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(tab.route) {
                                        popUpTo("invoices") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            NavHost(
                navController = navController,
                startDestination = "invoices"
            ) {
                composable("invoices") {
                    InvoicesListScreen(
                        viewModel = viewModel,
                        onCreateNew = { navController.navigate("editor/0") },
                        onEditInvoice = { id -> navController.navigate("editor/$id") },
                        onPreviewPdf = { id -> navController.navigate("preview/$id") }
                    )
                }

                composable("catalog") {
                    CatalogScreen(viewModel = viewModel)
                }

                composable("customers") {
                    CustomersScreen(
                        viewModel = viewModel,
                        onCreateInvoiceForCustomer = { customer ->
                            val n = Uri.encode(customer.name)
                            val c = Uri.encode(customer.companyName)
                            val p = Uri.encode(customer.phone)
                            navController.navigate("editor/0?name=$n&company=$c&phone=$p")
                        }
                    )
                }

                composable("settings") {
                    SettingsScreen(viewModel = viewModel)
                }

                composable(
                    route = "editor/{invoiceId}?name={name}&company={company}&phone={phone}",
                    arguments = listOf(
                        navArgument("invoiceId") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("name") { type = NavType.StringType; defaultValue = "" },
                        navArgument("company") { type = NavType.StringType; defaultValue = "" },
                        navArgument("phone") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { entry ->
                    val id = entry.arguments?.getLong("invoiceId") ?: 0L
                    val name = entry.arguments?.getString("name").orEmpty()
                    val company = entry.arguments?.getString("company").orEmpty()
                    val phone = entry.arguments?.getString("phone").orEmpty()
                    InvoiceEditorScreen(
                        invoiceId = if (id > 0L) id else null,
                        prefillCustomerName = name,
                        prefillCompanyName = company,
                        prefillPhone = phone,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onSavedAndPreview = { savedId ->
                            navController.navigate("preview/$savedId") {
                                popUpTo("invoices")
                            }
                        }
                    )
                }

                composable(
                    route = "preview/{invoiceId}",
                    arguments = listOf(navArgument("invoiceId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("invoiceId") ?: 0L
                    PdfPreviewScreen(
                        invoiceId = id,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onEdit = {
                            navController.navigate("editor/$id") {
                                popUpTo("invoices")
                            }
                        }
                    )
                }
            }
        }
    }
}
