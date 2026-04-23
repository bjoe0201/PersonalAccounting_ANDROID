package com.finance.manager.android.presentation.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.CategoryAmount
import com.finance.manager.android.domain.model.MonthlySnapshot
import com.finance.manager.android.domain.model.YearlyReport

private val ChartColors = listOf(
    Color(0xFF006D77), Color(0xFF4ECDC4), Color(0xFFFF6B6B),
    Color(0xFFFFE66D), Color(0xFF95E1D3), Color(0xFFF38181),
    Color(0xFFA8D8EA), Color(0xFFAA96DA), Color(0xFFFCBFBD),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("月報", "年報", "趨勢")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("報表") },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = uiState.activeTab.ordinal) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.activeTab.ordinal == index,
                        onClick = { viewModel.selectTab(ReportTab.entries[index]) },
                        text = { Text(title) },
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.activeTab) {
                    ReportTab.MONTHLY -> MonthlyTabContent(uiState, viewModel)
                    ReportTab.YEARLY -> YearlyTabContent(uiState, viewModel)
                    ReportTab.TREND -> TrendTabContent(uiState, viewModel)
                }
            }
        }
    }
}

@Composable
private fun MonthlyTabContent(uiState: ReportUiState, viewModel: ReportViewModel) {
    val summary = uiState.monthlySummary
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = viewModel::previousMonth) { Text("上月") }
                Text(
                    "${uiState.month.year}-${String.format("%02d", uiState.month.monthValue)}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Button(onClick = viewModel::nextMonth) { Text("下月") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("收入：${String.format("%.2f", summary?.totalIncome ?: 0.0)}")
                Text("支出：${String.format("%.2f", summary?.totalExpense ?: 0.0)}")
                Text("月末餘額：${String.format("%.2f", summary?.endBalance ?: 0.0)}")
            }
        }
        val breakdown = summary?.categoryBreakdown.orEmpty()
        if (breakdown.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                ExpensePieChart(breakdown = breakdown, modifier = Modifier.fillMaxWidth().height(200.dp))
            }
            item {
                HorizontalDivider()
                Text("支出分類明細", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            }
            items(breakdown, key = { it.categoryId }) { cat ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.categoryName, style = MaterialTheme.typography.bodyMedium)
                        if (cat.parentCategoryName != null) {
                            Text(cat.parentCategoryName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(String.format("%.2f", cat.amount), style = MaterialTheme.typography.bodyMedium)
                        Text(String.format("%.1f%%", kotlin.math.abs(cat.percentage)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun YearlyTabContent(uiState: ReportUiState, viewModel: ReportViewModel) {
    val report = uiState.yearlyReport
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = viewModel::previousYear) { Text("上年") }
                Text("${uiState.year} 年", style = MaterialTheme.typography.titleLarge)
                Button(onClick = viewModel::nextYear) { Text("下年") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("年度總收入：${String.format("%.2f", report?.totalIncome ?: 0.0)}")
                Text("年度總支出：${String.format("%.2f", report?.totalExpense ?: 0.0)}")
            }
        }
        val monthlyData = report?.monthlyData.orEmpty()
        if (monthlyData.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                IncomeExpenseBarChart(
                    monthlyData = monthlyData,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
            item { HorizontalDivider() }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("月份", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text("收入", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text("支出", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text("餘額", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                }
            }
            items(monthlyData, key = { it.month }) { snap ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${snap.month}月", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(String.format("%.0f", snap.income), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(String.format("%.0f", snap.expense), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(String.format("%.0f", snap.balance), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TrendTabContent(uiState: ReportUiState, viewModel: ReportViewModel) {
    val monthlyData = uiState.yearlyReport?.monthlyData.orEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = viewModel::previousYear) { Text("上年") }
                Text("${uiState.year} 年餘額趨勢", style = MaterialTheme.typography.titleLarge)
                Button(onClick = viewModel::nextYear) { Text("下年") }
            }
        }
        if (monthlyData.isNotEmpty()) {
            item {
                BalanceTrendChart(
                    monthlyData = monthlyData,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
        } else {
            item { Text("本年度尚無快照資料") }
        }
    }
}

// ─── Charts ──────────────────────────────────────────────────────────────────

@Composable
private fun ExpensePieChart(breakdown: List<CategoryAmount>, modifier: Modifier = Modifier) {
    val total = breakdown.sumOf { kotlin.math.abs(it.amount) }
    if (total == 0.0) return
    Canvas(modifier = modifier) {
        val diameter = minOf(size.width, size.height)
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var startAngle = -90f
        breakdown.forEachIndexed { idx, item ->
            val sweep = (kotlin.math.abs(item.amount) / total * 360f).toFloat()
            drawArc(
                color = ChartColors[idx % ChartColors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = Size(diameter, diameter),
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun IncomeExpenseBarChart(monthlyData: List<MonthlySnapshot>, modifier: Modifier = Modifier) {
    val maxVal = monthlyData.maxOf { maxOf(it.income, kotlin.math.abs(it.expense)) }.takeIf { it > 0 } ?: 1.0
    val incomeColor = Color(0xFF2E7D32)
    val expenseColor = Color(0xFFC62828)
    Canvas(modifier = modifier.padding(bottom = 20.dp)) {
        val barGroupWidth = size.width / monthlyData.size
        val barWidth = barGroupWidth * 0.35f
        val gap = barGroupWidth * 0.05f
        monthlyData.forEachIndexed { idx, snap ->
            val x = idx * barGroupWidth
            val incomeH = (snap.income / maxVal * size.height).toFloat().coerceAtLeast(0f)
            val expenseH = (kotlin.math.abs(snap.expense) / maxVal * size.height).toFloat().coerceAtLeast(0f)
            drawRect(
                color = incomeColor,
                topLeft = Offset(x + gap, size.height - incomeH),
                size = Size(barWidth, incomeH),
            )
            drawRect(
                color = expenseColor,
                topLeft = Offset(x + gap + barWidth + gap, size.height - expenseH),
                size = Size(barWidth, expenseH),
            )
        }
    }
}

@Composable
private fun BalanceTrendChart(monthlyData: List<MonthlySnapshot>, modifier: Modifier = Modifier) {
    val minBalance = monthlyData.minOf { it.balance }
    val maxBalance = monthlyData.maxOf { it.balance }
    val range = (maxBalance - minBalance).takeIf { it > 0 } ?: 1.0
    val lineColor = Color(0xFF006D77)
    Canvas(modifier = modifier.padding(bottom = 20.dp)) {
        val stepX = size.width / (monthlyData.size - 1).coerceAtLeast(1)
        val points = monthlyData.mapIndexed { idx, snap ->
            Offset(
                x = idx * stepX,
                y = (size.height * (1f - ((snap.balance - minBalance) / range).toFloat())).coerceIn(0f, size.height),
            )
        }
        for (i in 0 until points.size - 1) {
            drawLine(color = lineColor, start = points[i], end = points[i + 1], strokeWidth = 4f)
        }
        points.forEach { pt ->
            drawCircle(color = lineColor, radius = 6f, center = pt)
        }
    }
}
