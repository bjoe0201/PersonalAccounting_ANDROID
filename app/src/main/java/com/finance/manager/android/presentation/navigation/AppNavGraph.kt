package com.finance.manager.android.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finance.manager.android.presentation.account.AccountFormScreen
import com.finance.manager.android.presentation.account.AccountListScreen
import com.finance.manager.android.presentation.accountregister.AccountRegisterScreen
import com.finance.manager.android.presentation.currency.CurrencyManageScreen
import com.finance.manager.android.presentation.dashboard.DashboardScreen
import com.finance.manager.android.presentation.quickadd.QuickAddBottomSheet
import com.finance.manager.android.presentation.report.ReportScreen
import com.finance.manager.android.presentation.settings.SettingsScreen
import com.finance.manager.android.presentation.transaction.TransactionFormScreen

private data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var showQuickAddSheet by remember { mutableStateOf(false) }

    val bottomNavItems = listOf(
        BottomNavItem("首頁", Screen.Dashboard.route, Icons.Filled.Home, Icons.Filled.Home),
        BottomNavItem("帳戶", Screen.Accounts.route, Icons.Filled.Person, Icons.Filled.Person),
        BottomNavItem("記帳", "add_transaction", Icons.Filled.AddCircle, Icons.Filled.AddCircle),
        BottomNavItem("報表", Screen.Reports.route, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Filled.List),
        BottomNavItem("設定", Screen.Settings.route, Icons.Filled.Settings, Icons.Filled.Settings),
    )

    val topLevelRoutes = setOf(Screen.Dashboard.route, Screen.Accounts.route, Screen.Reports.route, Screen.Settings.route)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = when (item.route) {
                            "add_transaction" -> showQuickAddSheet
                            else -> currentRoute == item.route
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (item.route == "add_transaction") {
                                    showQuickAddSheet = true
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onOpenRegister = { accountId ->
                        navController.navigate(Screen.AccountRegister.createRoute(accountId))
                    },
                    onAddAccount = { navController.navigate(Screen.AccountForm.createRoute()) },
                )
            }

            composable(Screen.Reports.route) {
                ReportScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenCurrencies = { navController.navigate(Screen.Currencies.route) },
                )
            }

            composable(Screen.Currencies.route) {
                CurrencyManageScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Accounts.route) {
                AccountListScreen(
                    onCreateAccount = { navController.navigate(Screen.AccountForm.createRoute()) },
                    onEditAccount = { accountId -> navController.navigate(Screen.AccountForm.createRoute(accountId)) },
                    onOpenRegister = { accountId -> navController.navigate(Screen.AccountRegister.createRoute(accountId)) },
                )
            }

            composable(
                route = Screen.AccountRegister.route,
                arguments = listOf(navArgument("accountId") { type = NavType.IntType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getInt("accountId") ?: return@composable
                AccountRegisterScreen(
                    accountId = accountId,
                    onNavigateBack = { navController.popBackStack() },
                    onCreateTransaction = { navController.navigate(Screen.TransactionForm.createRoute(it)) },
                    onEditTransaction = { sourceAccountId, transactionId ->
                        navController.navigate(Screen.TransactionForm.createRoute(sourceAccountId, transactionId))
                    },
                )
            }

            composable(
                route = Screen.AccountForm.route,
                arguments = listOf(navArgument("accountId") { type = NavType.IntType; defaultValue = -1 })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getInt("accountId")?.takeIf { it != -1 }
                AccountFormScreen(accountId = accountId, onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.TransactionForm.route,
                arguments = listOf(
                    navArgument("accountId") { type = NavType.IntType },
                    navArgument("transactionId") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getInt("accountId") ?: return@composable
                val transactionId = backStackEntry.arguments?.getInt("transactionId")?.takeIf { it != -1 }
                TransactionFormScreen(
                    accountId = accountId,
                    transactionId = transactionId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }

    if (showQuickAddSheet) {
        QuickAddBottomSheet(
            onDismissRequest = { showQuickAddSheet = false },
            onSelectAccount = { accountId ->
                showQuickAddSheet = false
                navController.navigate(Screen.TransactionForm.createRoute(accountId))
            },
            onOpenAccountPage = {
                showQuickAddSheet = false
                navController.navigate(Screen.Accounts.route) {
                    popUpTo(Screen.Dashboard.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
}
