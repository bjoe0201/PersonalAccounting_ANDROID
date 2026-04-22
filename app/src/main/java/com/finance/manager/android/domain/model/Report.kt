package com.finance.manager.android.domain.model

data class MonthlyReportSummary(
    val year: Int,
    val month: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val endBalance: Double,
    val categoryBreakdown: List<CategoryAmount> = emptyList(),
)

data class CategoryAmount(
    val categoryId: Int,
    val categoryName: String,
    val parentCategoryName: String?,
    val amount: Double,
    val percentage: Double,
)

data class MonthlySnapshot(
    val month: Int,
    val income: Double,
    val expense: Double,
    val balance: Double,
)

data class YearlyReport(
    val year: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val monthlyData: List<MonthlySnapshot>,
)
