package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CategorySpend
import com.example.ui.viewmodel.DailySpendPoint
import java.util.Locale

@Composable
fun CategoryDonutChart(
    categories: List<CategorySpend>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category data for this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val totalAmount = categories.sumOf { it.totalAmount }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categories) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(700))
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("category_donut_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Donut Chart Canvas
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 32.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    var startAngle = -90f
                    val totalPercent = categories.sumOf { it.percentage.toDouble() }

                    for (cat in categories) {
                        val sweepAngle = if (totalPercent > 0) {
                            (cat.percentage / 100f) * 360f * animationProgress.value
                        } else 0f

                        if (sweepAngle > 0f) {
                            drawArc(
                                color = Color(cat.colorHex),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle - 2f, // subtle gap
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        startAngle += (cat.percentage / 100f) * 360f
                    }
                }

                // Center Total Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%s%.0f", currencySymbol, totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category Legend Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(6).forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(cat.colorHex), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = cat.categoryName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, cat.totalAmount),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(cat.colorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", cat.percentage),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(cat.colorHex),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseBarChart(
    income: Double,
    expense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val maxVal = Math.max(income, expense).coerceAtLeast(1.0)
    val incomeProgress = remember { Animatable(0f) }
    val expenseProgress = remember { Animatable(0f) }

    LaunchedEffect(income, expense) {
        incomeProgress.snapTo(0f)
        expenseProgress.snapTo(0f)
        incomeProgress.animateTo((income / maxVal).toFloat(), tween(600))
        expenseProgress.animateTo((expense / maxVal).toFloat(), tween(600))
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("income_expense_comparison_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Income vs Expense",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Income bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(IncomeGreen, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, income),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = IncomeGreen
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(incomeProgress.value.coerceIn(0.02f, 1f))
                            .height(12.dp)
                            .background(
                                Brush.horizontalGradient(listOf(IncomeGreen.copy(alpha = 0.7f), IncomeGreen)),
                                RoundedCornerShape(6.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expense bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(ExpenseRed, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%s%.2f", currencySymbol, expense),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ExpenseRed
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(expenseProgress.value.coerceIn(0.02f, 1f))
                            .height(12.dp)
                            .background(
                                Brush.horizontalGradient(listOf(ExpenseRed.copy(alpha = 0.7f), ExpenseRed)),
                                RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun DailyTrendChart(
    points: List<DailySpendPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val maxAmount = points.maxOfOrNull { Math.max(it.expense, it.income) }?.coerceAtLeast(10.0) ?: 10.0
    val progress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700))
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("daily_trend_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Trend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ExpenseRed, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(8.dp).background(IncomeGreen, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val paddingBottom = 24.dp.toPx()
                    val chartHeight = h - paddingBottom

                    // Draw grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = (chartHeight / gridLines) * i
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (points.size < 2) {
                        // Draw single point if only 1 data point
                        val pt = points.first()
                        val cx = w / 2f
                        val cy = chartHeight - (pt.expense / maxAmount).toFloat() * chartHeight * progress.value
                        drawCircle(ExpenseRed, radius = 6.dp.toPx(), center = Offset(cx, cy))
                        return@Canvas
                    }

                    val stepX = w / (points.size - 1)

                    // Path for Expense Line & Gradient Area
                    val expensePath = Path()
                    val expenseAreaPath = Path()

                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val normalizedVal = (pt.expense / maxAmount).toFloat() * progress.value
                        val y = chartHeight - (normalizedVal * (chartHeight - 10.dp.toPx()))

                        if (i == 0) {
                            expensePath.moveTo(x, y)
                            expenseAreaPath.moveTo(x, chartHeight)
                            expenseAreaPath.lineTo(x, y)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevNorm = (points[i - 1].expense / maxAmount).toFloat() * progress.value
                            val prevY = chartHeight - (prevNorm * (chartHeight - 10.dp.toPx()))
                            val cX1 = (prevX + x) / 2
                            val cY1 = prevY
                            val cX2 = (prevX + x) / 2
                            val cY2 = y
                            expensePath.cubicTo(cX1, cY1, cX2, cY2, x, y)
                            expenseAreaPath.cubicTo(cX1, cY1, cX2, cY2, x, y)
                        }
                    }

                    val lastX = (points.size - 1) * stepX
                    expenseAreaPath.lineTo(lastX, chartHeight)
                    expenseAreaPath.close()

                    // Draw filled gradient area
                    drawPath(
                        path = expenseAreaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(ExpenseRed.copy(alpha = 0.25f), ExpenseRed.copy(alpha = 0.0f)),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Draw Stroke Line
                    drawPath(
                        path = expensePath,
                        color = ExpenseRed,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw dots for each point
                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val normalizedVal = (pt.expense / maxAmount).toFloat() * progress.value
                        val y = chartHeight - (normalizedVal * (chartHeight - 10.dp.toPx()))
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = ExpenseRed, radius = 3.dp.toPx(), center = Offset(x, y))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayLabels = if (points.size <= 5) points else listOf(
                    points.first(),
                    points[points.size / 2],
                    points.last()
                )
                displayLabels.forEach { pt ->
                    Text(
                        text = pt.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
