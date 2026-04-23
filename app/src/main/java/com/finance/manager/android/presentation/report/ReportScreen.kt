package com.finance.manager.android.presentation.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finance.manager.android.domain.model.CategoryAmount
import com.finance.manager.android.domain.model.MonthlySnapshot
import com.finance.manager.android.presentation.components.FinanceDivider
import com.finance.manager.android.presentation.components.formatAmount
import com.finance.manager.android.ui.theme.OutlineVariant
import com.finance.manager.android.ui.theme.extendedColors

private val CHART_COLORS = listOf(
    Color(0xFF1565C0), Color(0xFFFF8F00), Color(0xFF2E7D32),
    Color(0xFF6A1B9A), Color(0xFFC62828), Color(0xFF546E7A),
    Color(0xFF00838F), Color(0xFFAD1457), Color(0xFF4E342E),
)

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val ec = MaterialTheme.extendedColors
    val tabs = listOf(
        ReportTab.MONTHLY to "月報",
        ReportTab.YEARLY to "年報",
        ReportTab.TREND to "趨勢",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Title
        Text(
            "報表分析",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 14.dp),
        )

        // Tabs (pill style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEach { (tab, label) ->
                val isSelected = uiState.activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .clickable { viewModel.selectTab(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else ec.onSurfaceVariant,
                    )
                }
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

@Composable
private fun MonthlyTabContent(uiState: ReportUiState, viewModel: ReportViewModel) {
    val summary = uiState.monthlySummary
    val ec = MaterialTheme.extendedColors
    val totalExpense = kotlin.math.abs(summary?.totalExpense ?: 0.0)
    val totalIncome = summary?.totalIncome ?: 0.0
    val balance = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Month selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "上個月",
                        tint = ec.onSurfaceVariant,
                    )
                }
                Text(
                    "${uiState.month.year} 年 ${uiState.month.monthValue} 月",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "下個月",
                        tint = ec.onSurfaceVariant,
                    )
                }
            }
        }

        // Summary cards (income + expense)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ec.incomeGreenLight)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text(
                            "收入",
                            style = MaterialTheme.typography.labelSmall,
                            color = ec.incomeGreen,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "+T\$${formatAmount(totalIncome)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ec.incomeGreen,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ec.expenseRedLight)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text(
                            "支出",
                            style = MaterialTheme.typography.labelSmall,
                            color = ec.expenseRed,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "-T\$${formatAmount(totalExpense)}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ec.expenseRed,
                        )
                    }
                }
            }
        }

        // Balance card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ec.surfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "本月結餘",
                        style = MaterialTheme.typography.labelMedium,
                        color = ec.onSurfaceVariant,
                    )
                    Text(
                        "${if (balance >= 0) "+" else "-"}T\$${formatAmount(balance)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (balance >= 0) ec.incomeGreen else ec.expenseRed,
                    )
                }
            }
        }

        // Donut chart + legend
        val breakdown = summary?.categoryBreakdown.orEmpty()
        if (breakdown.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                ) {
                    Text(
                        "支出分佈",
                        style = MaterialTheme.typography.labelMedium,
                        color = ec.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        DonutChart(
                            breakdown = breakdown,
                            totalExpense = totalExpense,
                            modifier = Modifier.size(160.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            breakdown.forEachIndexed { idx, cat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                CHART_COLORS[idx % CHART_COLORS.size],
                                                CircleShape,
                                            ),
                                    )
                                    Text(
                                        cat.categoryName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "${kotlin.math.abs(cat.percentage).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ec.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category breakdown list
            item {
                Text(
                    "分類明細",
                    style = MaterialTheme.typography.labelMedium,
                    color = ec.onSurfaceVariant,
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                ) {
                    breakdown.forEachIndexed { idx, cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        CHART_COLORS[idx % CHART_COLORS.size],
                                        CircleShape,
                                    ),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        cat.categoryName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        "T\$${formatAmount(cat.amount)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ec.expenseRed,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                // Progress bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ec.outlineVariant.copy(alpha = 0.3f)),
                                ) {
                                    val pct = kotlin.math.abs(cat.percentage).toFloat()
                                        .coerceIn(0f, 100f) / 100f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(pct)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CHART_COLORS[idx % CHART_COLORS.size]),
                                    )
                                }
                            }
                            Text(
                                "${kotlin.math.abs(cat.percentage).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = ec.onSurfaceVariant,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.End,
                            )
                        }
                        if (idx < breakdown.lastIndex) {
                            FinanceDivider(horizontalPadding = 14.dp)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DonutChart(
    breakdown: List<CategoryAmount>,
    totalExpense: Double,
    modifier: Modifier = Modifier,
) {
    val ec = MaterialTheme.extendedColors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 22.dp.toPx()
            val diameter = minOf(size.width, size.height) - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f

            breakdown.forEachIndexed { idx, cat ->
                val sweep = (kotlin.math.abs(cat.percentage) / 100.0 * 360.0).toFloat()
                drawArc(
                    color = CHART_COLORS[idx % CHART_COLORS.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "總支出",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ec.onSurfaceVariant,
            )
            Text(
                "T\$${formatAmount(totalExpense)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun YearlyTabContent(uiState: ReportUiState, viewModel: ReportViewModel) {
    val report = uiState.yearlyReport
    val ec = MaterialTheme.extendedColors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.previousYear() }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "上一年", tint = ec.onSurfaceVariant)
                }
                Text(
                    "${uiState.year} 年",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                IconButton(onClick = { viewModel.nextYear() }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "下一年", tint = ec.onSurfaceVariant)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ec.incomeGreenLight)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text("年度收入", style = MaterialTheme.typography.labelSmall, color = ec.incomeGreen)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "+T\$${formatAmount(report?.totalIncome ?: 0.0)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ec.incomeGreen,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ec.expenseRedLight)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column {
                        Text("年度支出", style = MaterialTheme.typography.labelSmall, color = ec.expenseRed)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "-T\$${formatAmount(kotlin.math.abs(report?.totalExpense ?: 0.0))}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ec.expenseRed,
                        )
                    }
                }
            }
        }
        val monthlyData = report?.monthlyData.orEmpty()
        if (monthlyData.isNotEmpty()) {
            item {
                IncomeExpenseBarChart(
                    monthlyData = monthlyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
            // Monthly table
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text("月份", style = MaterialTheme.typography.labelSmall, color = ec.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("收入", style = MaterialTheme.typography.labelSmall, color = ec.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("支出", style = MaterialTheme.typography.labelSmall, color = ec.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("餘額", style = MaterialTheme.typography.labelSmall, color = ec.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    FinanceDivider(horizontalPadding = 14.dp)
                    monthlyData.forEach { snap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text("${snap.month}月", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(formatAmount(snap.income), style = MaterialTheme.typography.bodySmall, color = ec.incomeGreen, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            Text(formatAmount(kotlin.math.abs(snap.expense)), style = MaterialTheme.typography.bodySmall, color = ec.expenseRed, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            Text(formatAmount(snap.balance), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendTabContent(uiState: ReportUiState, viewModel: ReportViewModel) {
    val monthlyData = uiState.yearlyReport?.monthlyData.orEmpty()
    val ec = MaterialTheme.extendedColors
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.previousYear() }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "上一年", tint = ec.onSurfaceVariant)
                }
                Text(
                    "${uiState.year} 年餘額趨勢",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                IconButton(onClick = { viewModel.nextYear() }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "下一年", tint = ec.onSurfaceVariant)
                }
            }
        }
        if (monthlyData.isNotEmpty()) {
            item {
                BalanceTrendChart(
                    monthlyData = monthlyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            }
        } else {
            item { Text("本年度尚無快照資料") }
        }
    }
}

// ─── Charts ──────────────────────────────────────────────────────────────────

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
