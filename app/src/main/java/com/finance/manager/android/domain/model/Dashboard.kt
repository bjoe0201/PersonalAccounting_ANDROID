package com.finance.manager.android.domain.model

data class DashboardSummary(
    val netAssets: Double,
    val monthIncome: Double,
    val monthExpense: Double,
    val accounts: List<Account>,
)

