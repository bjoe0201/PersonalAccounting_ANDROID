package com.finance.manager.android.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finance.manager.android.presentation.account.AccountFormScreen
import com.finance.manager.android.presentation.account.AccountListScreen
import com.finance.manager.android.presentation.accountregister.AccountRegisterScreen
import com.finance.manager.android.presentation.category.CategoryManagementScreen
import com.finance.manager.android.presentation.currency.CurrencyManageScreen
import com.finance.manager.android.presentation.dashboard.DashboardScreen
import com.finance.manager.android.presentation.payee.PayeeManagementScreen
import com.finance.manager.android.presentation.quickadd.QuickAddBottomSheet
import com.finance.manager.android.presentation.report.ReportScreen
import com.finance.manager.android.presentation.settings.SettingsScreen
import com.finance.manager.android.presentation.tag.TagManagementScreen
import com.finance.manager.android.presentation.transaction.TransactionFormScreen
import com.finance.manager.android.ui.theme.GradientEnd
import com.finance.manager.android.ui.theme.GradientStart
import com.finance.manager.android.ui.theme.OutlineVariant

private data class BottomNavItem(
    val id: String,
    val label: String,
    val route: String?,
    val icon: ImageVector,
    val isSpecial: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    var showQuickAddSheet by remember { mutableStateOf(false) }

    val bottomNavItems = listOf(
        BottomNavItem("dashboard", "首頁", Screen.Dashboard.route, Icons.Filled.Home),
        BottomNavItem("accounts", "帳戶", Screen.Accounts.route, Icons.Filled.AccountBalanceWallet),
        BottomNavItem("quickadd", "記帳", null, Icons.Filled.Add, isSpecial = true),
        BottomNavItem("reports", "報表", Screen.Reports.route, Icons.Filled.BarChart),
        BottomNavItem("settings", "設定", Screen.Settings.route, Icons.Filled.Settings),
    )

    val topLevelRoutes = setOf(
        Screen.Dashboard.route,
        Screen.Accounts.route,
        Screen.Reports.route,
        Screen.Settings.route,
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                FinanceBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onNavigate = { item ->
                        if (item.isSpecial) {
                            showQuickAddSheet = true
                        } else {
                            item.route?.let { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onOpenRegister = { accountId ->
                        navController.navigate(Screen.AccountRegister.createRoute(accountId))
                    },
                    onOpenAccounts = {
                        navController.navigate(Screen.Accounts.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onQuickAdd = { showQuickAddSheet = true },
                )
            }

            composable(Screen.Reports.route) {
                ReportScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenCurrencies = { navController.navigate(Screen.Currencies.route) },
                    onOpenCategories = { navController.navigate(Screen.CategoryManagement.route) },
                    onOpenPayees = { navController.navigate(Screen.PayeeManagement.route) },
                    onOpenTags = { navController.navigate(Screen.TagManagement.route) },
                )
            }

            composable(Screen.Currencies.route) {
                CurrencyManageScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.CategoryManagement.route) {
                CategoryManagementScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.PayeeManagement.route) {
                PayeeManagementScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.TagManagement.route) {
                TagManagementScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Accounts.route) {
                AccountListScreen(
                    onCreateAccount = { navController.navigate(Screen.AccountForm.createRoute()) },
                    onEditAccount = { accountId -> navController.navigate(Screen.AccountForm.createRoute(accountId)) },
                    onOpenRegister = { accountId -> navController.navigate(Screen.AccountRegister.createRoute(accountId)) },
                    onNavigateBack = { navController.popBackStack() },
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

@Composable
private fun FinanceBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (BottomNavItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp)
            .background(MaterialTheme.colorScheme.surface)
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val isActive = currentRoute == item.route
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(item) }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (item.isSpecial) {
                        // 記帳 - gradient pill button
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 32.dp)
                                .background(
                                    brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .shadow(4.dp, RoundedCornerShape(16.dp), clip = false),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    } else {
                        // Normal tab
                        val tint = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 32.dp)
                                .background(
                                    color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp),
                                tint = tint,
                            )
                        }
                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = tint,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }
        }
    }
}
