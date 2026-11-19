package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTalikhataColors
import com.example.ui.viewmodel.DueAgeingBucket
import com.example.ui.viewmodel.MonthlyBarPoint
import com.example.util.Formatters

@Composable
fun MonthlyPerformanceChartCard(
    series: List<MonthlyBarPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalikhataColors.current
    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(series) { animateTrigger = true }
    val progress by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "chart_anim"
    )

    val maxVal = (series.maxOfOrNull { maxOf(it.collectionAmount, it.paymentAndDueAmount) } ?: 0.0)
        .coerceAtLeast(100.0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মাসিক পারফরম্যান্স (৬ মাস)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = colors.paymentGreen, label = "আদায়")
                LegendDot(color = colors.dueOrange, label = "বাকি / প্রদান")
            }
            Spacer(modifier = Modifier.height(14.dp))

            if (series.all { it.collectionAmount == 0.0 && it.paymentAndDueAmount == 0.0 }) {
                Text(
                    text = "লেনদেন যুক্ত হলে এখানে মাসিক গ্রাফ প্রদর্শিত হবে।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val gridColor = colors.subtleDivider
                    for (i in 0..3) {
                        val y = size.height * (i / 3f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    val count = series.size.coerceAtLeast(1)
                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.28f).coerceAtMost(24.dp.toPx())
                    val gap = 4.dp.toPx()

                    series.forEachIndexed { idx, pt ->
                        val centerX = slotWidth * idx + slotWidth / 2f
                        val h1 = ((pt.collectionAmount / maxVal).toFloat() * size.height * progress)
                            .coerceAtLeast(if (pt.collectionAmount > 0) 4.dp.toPx() else 0f)
                        val h2 = ((pt.paymentAndDueAmount / maxVal).toFloat() * size.height * progress)
                            .coerceAtLeast(if (pt.paymentAndDueAmount > 0) 4.dp.toPx() else 0f)

                        if (h1 > 0f) {
                            drawRoundRect(
                                color = colors.paymentGreen,
                                topLeft = Offset(centerX - barWidth - gap / 2f, size.height - h1),
                                size = Size(barWidth, h1),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                        if (h2 > 0f) {
                            drawRoundRect(
                                color = colors.dueOrange,
                                topLeft = Offset(centerX + gap / 2f, size.height - h2),
                                size = Size(barWidth, h2),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    series.forEach { pt ->
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                val totalCol = series.sumOf { it.collectionAmount }
                val totalOut = series.sumOf { it.paymentAndDueAmount }
                Text(
                    text = "মোট আদায়: ${Formatters.formatMoney(totalCol, currencySymbol)}  •  মোট বাকি/প্রদান: ${Formatters.formatMoney(totalOut, currencySymbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun DueAgeingDonutCard(
    buckets: List<DueAgeingBucket>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalikhataColors.current
    val sliceColors = listOf(
        colors.paymentGreen,
        colors.customerBlue,
        colors.dueOrange,
        colors.warningRed
    )
    val total = buckets.sumOf { it.totalAmount }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "বকেয়ার বয়স বিশ্লেষণ (Due Ageing)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(124.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(116.dp)) {
                        val stroke = 20.dp.toPx()
                        if (total <= 0.0) {
                            drawArc(
                                color = colors.subtleDivider,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        } else {
                            var startAngle = -90f
                            buckets.forEachIndexed { idx, bucket ->
                                val sweep = (bucket.totalAmount / total * 360.0).toFloat()
                                if (sweep > 0f) {
                                    drawArc(
                                        color = sliceColors[idx % sliceColors.size],
                                        startAngle = startAngle,
                                        sweepAngle = sweep.coerceAtLeast(3f),
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "মোট বকেয়া",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        Text(
                            text = Formatters.formatMoney(total, currencySymbol),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    buckets.forEachIndexed { idx, b ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(sliceColors[idx % sliceColors.size])
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${b.label} (${b.customerCount})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = Formatters.formatMoney(b.totalAmount, currencySymbol),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalTalikhataColors.current.textSecondary
        )
    }
}
