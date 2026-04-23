package com.finance.manager.android.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Accounts : Screen("accounts")
    data object Reports : Screen("reports")
    data object Settings : Screen("settings")
    data object Currencies : Screen("currencies")
    data object CategoryManagement : Screen("management/categories")
    data object PayeeManagement : Screen("management/payees")
    data object TagManagement : Screen("management/tags")

    data object AccountRegister : Screen("accounts/{accountId}/register") {
        fun createRoute(accountId: Int): String = "accounts/$accountId/register"
    }

    data object AccountForm : Screen("accounts/form?accountId={accountId}") {
        fun createRoute(accountId: Int? = null): String =
            if (accountId == null) {
                "accounts/form?accountId=-1"
            } else {
                "accounts/form?accountId=$accountId"
            }
    }

    data object TransactionForm : Screen("transactions/form?accountId={accountId}&transactionId={transactionId}") {
        fun createRoute(accountId: Int, transactionId: Int? = null): String {
            val editValue = transactionId ?: -1
            return "transactions/form?accountId=$accountId&transactionId=$editValue"
        }
    }
}



