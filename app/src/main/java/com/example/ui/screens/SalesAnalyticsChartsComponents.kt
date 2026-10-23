package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Product
import com.example.data.models.Sale
import com.example.ui.theme.LocalAFStoreSemanticColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot

enum class SalesReportPeriod(
    val labelAr: String,
    val subtitleAr: String,
    val isMonthly: Boolean,
    val bucketCount: Int
) {
    DAILY_7("آخر 7 أيام", "ملخص المبيعات اليومية لآخر أسبوع", false, 7),
    DAILY_30("آخر 30 يوماً", "حركة المبيعات اليومية خلال 30 يوماً", false, 30),
    MONTHLY_6("آخر 6 أشهر", "ملخص المبيعات الشهرية لآخر نصف سنة", true, 6),
    MONTHLY_12("آخر 12 شهراً", "ملخص المبيعات الشهرية على مدار السنة", true, 12)
}

enum class SalesChartDisplayMode(val labelAr: String) {
    BAR_AND_TREND("أعمدة + منحنى"),
    BAR_ONLY("أعمدة بيانية"),
    TREND_ONLY("منحنى اتجاه")
}

enum class SalesMetricComparisonMode(
    val labelAr: String,
    val primaryLabelAr: String,
    val secondaryLabelAr: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    SALES_VS_PROFIT(
        labelAr = "المبيعات وصافي الربح",
        primaryLabelAr = "إجمالي المبيعات",
        secondaryLabelAr = "صافي الربح",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF10B981)
    ),
    CASH_VS_DEBT(
        labelAr = "النقدي والآجل (الديون)",
        primaryLabelAr = "النقدي والتحويل",
        secondaryLabelAr = "المبيعات الآجلة (دين)",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFFF59E0B)
    )
}

data class SalesTimeBucket(
    val index: Int,
    val startMillis: Long,
    val endMillis: Long,
    val shortLabel: String,
    val fullTitle: String,
    val totalSales: Double,
    val netProfit: Double,
    val cashSales: Double,
    val transferSales: Double,
    val debtSales: Double,
    val transactionsCount: Int,
    val unitsSoldCount: Int,
    val topProductName: String
) {
    val collectedSales: Double
        get() = cashSales + transferSales

    val profitMarginPercent: Double
        get() = if (totalSales > 0.0) ((netProfit / totalSales) * 100.0).coerceIn(0.0, 100.0) else 0.0
}

data class PeriodOverviewSummary(
    val totalSales: Double = 0.0,
    val netProfit: Double = 0.0,
    val cashSales: Double = 0.0,
    val transferSales: Double = 0.0,
    val debtSales: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalUnitsSold: Int = 0,
    val todaySales: Double = 0.0,
    val yesterdaySales: Double = 0.0,
    val todayNetProfit: Double = 0.0,
    val currentMonthSales: Double = 0.0,
    val previousMonthSales: Double = 0.0,
    val currentMonthNetProfit: Double = 0.0
)

@Composable
fun AnimatedMoneyText(
    targetAmount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    prefix: String = "",
    decimals: Int = 2,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    val formattedText = remember(targetAmount, currencySymbol, prefix, decimals) {
        val pattern = "%.${decimals}f"
        "$prefix${String.format(Locale.US, pattern, targetAmount)} $currencySymbol"
    }
    Text(
        text = formattedText,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

object SalesAnalyticsAggregator {

    private val arabicMonths = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun buildTimeBuckets(
        sales: List<Sale>,
        products: List<Product>,
        period: SalesReportPeriod,
        nowMillis: Long = System.currentTimeMillis()
    ): List<SalesTimeBucket> {
        val costByProductId = products.associateBy({ it.id }, { it.purchasePrice })
        val costByProductName = products.associateBy({ it.name.lowercase(Locale.ROOT) }, { it.purchasePrice })

        fun estimateSaleProfit(sale: Sale): Double {
            val baseName = sale.productName.substringBefore(" (").trim().lowercase(Locale.ROOT)
            val unitCost = costByProductId[sale.productId]
                ?: costByProductName[baseName]
                ?: 0.0
            val totalCost = unitCost * sale.quantitySold.coerceAtLeast(1)
            return (sale.totalPrice - totalCost).coerceAtLeast(0.0)
        }

        val buckets = mutableListOf<SalesTimeBucket>()
        val dayShortFormat = SimpleDateFormat("MM/dd", Locale.US)
        val dayFullFormat = SimpleDateFormat("EEEE dd MMMM yyyy", Locale("ar"))

        if (!period.isMonthly) {
            val totalDays = period.bucketCount
            for (offset in (totalDays - 1) downTo 0) {
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, -offset)
                }
                val startMillis = startCal.timeInMillis
                val endCal = (startCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                val endMillis = endCal.timeInMillis - 1L

                val bucketSales = sales.filter { it.saleDate in startMillis..endMillis }
                val totalRev = bucketSales.sumOf { it.totalPrice }
                val profit = bucketSales.sumOf { estimateSaleProfit(it) }
                val debtRev = bucketSales.filter { it.isDebt || it.paymentMethod == "DEBT" }.sumOf { it.totalPrice }
                val transferRev = bucketSales.filter { !it.isDebt && it.paymentMethod == "TRANSFER" }.sumOf { it.totalPrice }
                val cashRev = (totalRev - debtRev - transferRev).coerceAtLeast(0.0)
                val units = bucketSales.sumOf { it.quantitySold }
                val topProd = bucketSales
                    .groupBy { it.productName.substringBefore(" (").trim() }
                    .maxByOrNull { (_, list) -> list.sumOf { s -> s.totalPrice } }
                    ?.key.orEmpty()

                val shortLbl = when (offset) {
                    0 -> "اليوم"
                    1 -> if (totalDays <= 7) "أمس" else dayShortFormat.format(Date(startMillis))
                    else -> dayShortFormat.format(Date(startMillis))
                }
                val fullLbl = when (offset) {
                    0 -> "اليوم • ${dayFullFormat.format(Date(startMillis))}"
                    1 -> "أمس • ${dayFullFormat.format(Date(startMillis))}"
                    else -> dayFullFormat.format(Date(startMillis))
                }

                buckets.add(
                    SalesTimeBucket(
                        index = totalDays - 1 - offset,
                        startMillis = startMillis,
                        endMillis = endMillis,
                        shortLabel = shortLbl,
                        fullTitle = fullLbl,
                        totalSales = totalRev,
                        netProfit = profit,
                        cashSales = cashRev,
                        transferSales = transferRev,
                        debtSales = debtRev,
                        transactionsCount = bucketSales.size,
                        unitsSoldCount = units,
                        topProductName = topProd
                    )
                )
            }
        } else {
            val totalMonths = period.bucketCount
            for (offset in (totalMonths - 1) downTo 0) {
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MONTH, -offset)
                }
                val startMillis = startCal.timeInMillis
                val endCal = (startCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, 1)
                }
                val endMillis = endCal.timeInMillis - 1L

                val monthIdx = startCal.get(Calendar.MONTH).coerceIn(0, 11)
                val yearVal = startCal.get(Calendar.YEAR)
                val monthNameAr = arabicMonths[monthIdx]

                val bucketSales = sales.filter { it.saleDate in startMillis..endMillis }
                val totalRev = bucketSales.sumOf { it.totalPrice }
                val profit = bucketSales.sumOf { estimateSaleProfit(it) }
                val debtRev = bucketSales.filter { it.isDebt || it.paymentMethod == "DEBT" }.sumOf { it.totalPrice }
                val transferRev = bucketSales.filter { !it.isDebt && it.paymentMethod == "TRANSFER" }.sumOf { it.totalPrice }
                val cashRev = (totalRev - debtRev - transferRev).coerceAtLeast(0.0)
                val units = bucketSales.sumOf { it.quantitySold }
                val topProd = bucketSales
                    .groupBy { it.productName.substringBefore(" (").trim() }
                    .maxByOrNull { (_, list) -> list.sumOf { s -> s.totalPrice } }
                    ?.key.orEmpty()

                buckets.add(
                    SalesTimeBucket(
                        index = totalMonths - 1 - offset,
                        startMillis = startMillis,
                        endMillis = endMillis,
                        shortLabel = monthNameAr,
                        fullTitle = "شهر $monthNameAr $yearVal",
                        totalSales = totalRev,
                        netProfit = profit,
                        cashSales = cashRev,
                        transferSales = transferRev,
                        debtSales = debtRev,
                        transactionsCount = bucketSales.size,
                        unitsSoldCount = units,
                        topProductName = topProd
                    )
                )
            }
        }

        return buckets
    }

    fun buildOverviewSummary(
        buckets: List<SalesTimeBucket>,
        allSales: List<Sale>,
        products: List<Product>,
        nowMillis: Long = System.currentTimeMillis()
    ): PeriodOverviewSummary {
        val dailyRecent = buildTimeBuckets(allSales, products, SalesReportPeriod.DAILY_7, nowMillis)
        val monthlyRecent = buildTimeBuckets(allSales, products, SalesReportPeriod.MONTHLY_6, nowMillis)

        val todayBucket = dailyRecent.lastOrNull()
        val yesterdayBucket = if (dailyRecent.size >= 2) dailyRecent[dailyRecent.size - 2] else null
        val currentMonthBucket = monthlyRecent.lastOrNull()
        val prevMonthBucket = if (monthlyRecent.size >= 2) monthlyRecent[monthlyRecent.size - 2] else null

        return PeriodOverviewSummary(
            totalSales = buckets.sumOf { it.totalSales },
            netProfit = buckets.sumOf { it.netProfit },
            cashSales = buckets.sumOf { it.cashSales },
            transferSales = buckets.sumOf { it.transferSales },
            debtSales = buckets.sumOf { it.debtSales },
            totalTransactions = buckets.sumOf { it.transactionsCount },
            totalUnitsSold = buckets.sumOf { it.unitsSoldCount },
            todaySales = todayBucket?.totalSales ?: 0.0,
            yesterdaySales = yesterdayBucket?.totalSales ?: 0.0,
            todayNetProfit = todayBucket?.netProfit ?: 0.0,
            currentMonthSales = currentMonthBucket?.totalSales ?: 0.0,
            previousMonthSales = prevMonthBucket?.totalSales ?: 0.0,
            currentMonthNetProfit = currentMonthBucket?.netProfit ?: 0.0
        )
    }
}

@Composable
fun DailyAndMonthlySnapshotsCard(
    overview: PeriodOverviewSummary,
    currencySymbol: String
) {
    val semantic = LocalAFStoreSemanticColors.current
    val cardElevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 2.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_monthly_snapshot_row"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Today's Sales Snapshot
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (semantic.isDark) Color(0xFF0284C7).copy(alpha = 0.14f) else semantic.cardElevated
            ),
            elevation = cardElevation,
            border = BorderStroke(1.dp, semantic.infoBlue.copy(alpha = if (semantic.isDark) 0.35f else 0.32f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = semantic.infoBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "مبيعات اليوم",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = semantic.infoBlue
                    )
                }
                AnimatedMoneyText(
                    targetAmount = overview.todaySales,
                    currencySymbol = currencySymbol,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.5.sp
                )
                AnimatedMoneyText(
                    targetAmount = overview.todayNetProfit,
                    currencySymbol = currencySymbol,
                    prefix = "صافي ربح اليوم: ",
                    fontSize = 11.sp,
                    color = semantic.profitGreen,
                    fontWeight = FontWeight.SemiBold
                )
                AnimatedMoneyText(
                    targetAmount = overview.yesterdaySales,
                    currencySymbol = currencySymbol,
                    prefix = "مقارنة بالأمس: ",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Current Month's Sales Snapshot
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (semantic.isDark) Color(0xFF10B981).copy(alpha = 0.14f) else semantic.cardElevated
            ),
            elevation = cardElevation,
            border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = if (semantic.isDark) 0.35f else 0.32f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = semantic.profitGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "مبيعات هذا الشهر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = semantic.profitGreen
                    )
                }
                AnimatedMoneyText(
                    targetAmount = overview.currentMonthSales,
                    currencySymbol = currencySymbol,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.5.sp
                )
                AnimatedMoneyText(
                    targetAmount = overview.currentMonthNetProfit,
                    currencySymbol = currencySymbol,
                    prefix = "ربح الشهر: ",
                    fontSize = 11.sp,
                    color = semantic.infoBlue,
                    fontWeight = FontWeight.SemiBold
                )
                AnimatedMoneyText(
                    targetAmount = overview.previousMonthSales,
                    currencySymbol = currencySymbol,
                    prefix = "الشهر السابق: ",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun PeriodRangeSelectorCard(
    selectedPeriod: SalesReportPeriod,
    onPeriodSelected: (SalesReportPeriod) -> Unit,
    overview: PeriodOverviewSummary,
    currencySymbol: String
) {
    val semantic = LocalAFStoreSemanticColors.current

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (semantic.isDark) 0.dp else 2.dp
        ),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reports_period_selector_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "النطاق الزمني للتقارير والرسوم البيانية",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp
                    )
                    Text(
                        text = selectedPeriod.subtitleAr,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${overview.totalTransactions} عملية",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SalesReportPeriod.entries.forEach { period ->
                    val isSelected = selectedPeriod == period
                    val chipScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.04f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "period_chip_scale_${period.name}"
                    )
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPeriodSelected(period) },
                        label = {
                            Text(
                                text = period.labelAr,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (period.isMonthly) Icons.Default.CalendarMonth else Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = chipScale
                                scaleY = chipScale
                            }
                            .testTag("period_chip_${period.name}")
                    )
                }
            }

            HorizontalDivider()

            // Animated Summary KPIs for the Selected Period
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodMetricMiniCard(
                    title = "إجمالي المبيعات",
                    amount = overview.totalSales,
                    currencySymbol = currencySymbol,
                    accentColor = Color(0xFF38BDF8),
                    periodKey = selectedPeriod,
                    modifier = Modifier.weight(1f)
                )
                PeriodMetricMiniCard(
                    title = "صافي الربح",
                    amount = overview.netProfit,
                    currencySymbol = currencySymbol,
                    accentColor = Color(0xFF10B981),
                    periodKey = selectedPeriod,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodMetricMiniCard(
                    title = "النقدي والتحويل",
                    amount = overview.cashSales + overview.transferSales,
                    currencySymbol = currencySymbol,
                    accentColor = Color(0xFF10B981),
                    periodKey = selectedPeriod,
                    modifier = Modifier.weight(1f)
                )
                PeriodMetricMiniCard(
                    title = "المبيعات الآجلة (دين)",
                    amount = overview.debtSales,
                    currencySymbol = currencySymbol,
                    accentColor = Color(0xFFF59E0B),
                    periodKey = selectedPeriod,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PeriodMetricMiniCard(
    title: String,
    amount: Double,
    currencySymbol: String,
    accentColor: Color,
    periodKey: SalesReportPeriod,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AnimatedMoneyText(
                targetAmount = amount,
                currencySymbol = currencySymbol,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = accentColor
            )
        }
    }
}

private data class ChartPeriodStateSnapshot(
    val period: SalesReportPeriod,
    val buckets: List<SalesTimeBucket>
)

@Composable
fun InteractiveSalesBarAndTrendCard(
    buckets: List<SalesTimeBucket>,
    selectedPeriod: SalesReportPeriod,
    currencySymbol: String,
    selectedBucketIndex: Int,
    onSelectBucketIndex: (Int) -> Unit,
    chartDisplayMode: SalesChartDisplayMode = SalesChartDisplayMode.BAR_AND_TREND,
    onChartDisplayModeChange: ((SalesChartDisplayMode) -> Unit)? = null,
    metricComparisonMode: SalesMetricComparisonMode = SalesMetricComparisonMode.SALES_VS_PROFIT,
    onMetricComparisonModeChange: ((SalesMetricComparisonMode) -> Unit)? = null
) {
    var internalChartDisplayMode by remember { mutableStateOf(chartDisplayMode) }
    var internalMetricComparisonMode by remember { mutableStateOf(metricComparisonMode) }

    val activeChartDisplayMode = if (onChartDisplayModeChange != null) chartDisplayMode else internalChartDisplayMode
    val activeMetricComparisonMode = if (onMetricComparisonModeChange != null) metricComparisonMode else internalMetricComparisonMode

    val safeSelectedIndex = selectedBucketIndex.coerceIn(0, (buckets.size - 1).coerceAtLeast(0))
    val selectedBucket = buckets.getOrNull(safeSelectedIndex)

    val maxChartValue = remember(buckets, activeMetricComparisonMode) {
        val rawMax = buckets.maxOfOrNull { b ->
            when (activeMetricComparisonMode) {
                SalesMetricComparisonMode.SALES_VS_PROFIT -> maxOf(b.totalSales, b.netProfit)
                SalesMetricComparisonMode.CASH_VS_DEBT -> maxOf(b.collectedSales, b.debtSales, b.totalSales)
            }
        } ?: 0.0
        if (rawMax <= 0.0) 100.0 else rawMax * 1.15
    }

    val pulseRadiusFactor = 1.45f

    val pillListState = rememberLazyListState()
    LaunchedEffect(safeSelectedIndex, selectedPeriod) {
        if (buckets.isNotEmpty() && safeSelectedIndex in buckets.indices) {
            pillListState.scrollToItem((safeSelectedIndex - 2).coerceAtLeast(0))
        }
    }

    val semantic = LocalAFStoreSemanticColors.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (semantic.isDark) 0.dp else 2.dp
        ),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("interactive_sales_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Title & Chart Mode Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.16f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        AnimatedContent(
                            targetState = selectedPeriod.isMonthly,
                            transitionSpec = {
                                (fadeIn(tween(180)) + slideInVertically { it / 2 }) togetherWith
                                    (fadeOut(tween(130)) + slideOutVertically { -it / 2 })
                            },
                            label = "chart_header_title_anim"
                        ) { isMonthly ->
                            Text(
                                text = if (isMonthly) {
                                    "الرسم البياني للمبيعات والأرباح الشهرية"
                                } else {
                                    "الرسم البياني للمبيعات والأرباح اليومية"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "المس أو اسحب على الأعمدة والمنحنى لعرض تفاصيل اليوم أو الشهر",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Chart Type Selector Row (أعمدة + منحنى | أعمدة بيانية | منحنى اتجاه)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SalesChartDisplayMode.entries.forEach { mode ->
                    val active = activeChartDisplayMode == mode
                    val modeScale by animateFloatAsState(
                        targetValue = if (active) 1.04f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "chart_mode_scale_${mode.name}"
                    )
                    FilterChip(
                        selected = active,
                        onClick = {
                            internalChartDisplayMode = mode
                            onChartDisplayModeChange?.invoke(mode)
                        },
                        label = {
                            Text(
                                text = mode.labelAr,
                                fontSize = 11.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (mode) {
                                    SalesChartDisplayMode.BAR_AND_TREND -> Icons.AutoMirrored.Filled.TrendingUp
                                    SalesChartDisplayMode.BAR_ONLY -> Icons.Default.BarChart
                                    SalesChartDisplayMode.TREND_ONLY -> Icons.AutoMirrored.Filled.ShowChart
                                },
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = modeScale
                                scaleY = modeScale
                            }
                            .testTag("chart_mode_${mode.name}")
                    )
                }
            }

            // Metric Comparison Selector Row (المبيعات وصافي الربح | النقدي والآجل)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SalesMetricComparisonMode.entries.forEach { metricMode ->
                    val active = activeMetricComparisonMode == metricMode
                    FilterChip(
                        selected = active,
                        onClick = {
                            internalMetricComparisonMode = metricMode
                            onMetricComparisonModeChange?.invoke(metricMode)
                        },
                        label = {
                            Text(
                                text = metricMode.labelAr,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = metricMode.primaryColor.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_mode_${metricMode.name}")
                    )
                }
            }

            // Color Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegendDot(
                        label = activeMetricComparisonMode.primaryLabelAr,
                        color = activeMetricComparisonMode.primaryColor
                    )
                    ChartLegendDot(
                        label = activeMetricComparisonMode.secondaryLabelAr,
                        color = activeMetricComparisonMode.secondaryColor
                    )
                }
                AnimatedMoneyText(
                    targetAmount = maxChartValue / 1.15,
                    currencySymbol = currencySymbol,
                    prefix = "الأعلى: ",
                    decimals = 0,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            run {
                val activeBuckets = buckets
                val activeSafeIndex = safeSelectedIndex.coerceIn(0, (activeBuckets.size - 1).coerceAtLeast(0))
                val animatedSelectedSlot = activeSafeIndex.toFloat()
                val canvasGridLineCol = semantic.chartGridLine
                val canvasDotRingCol = semantic.tooltipBg
                val barTrendLineColor = if (semantic.isDark) Color(0xFFFACC15) else Color(0xFFD97706)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = semantic.chartCanvasBg,
                    border = BorderStroke(1.dp, semantic.chartCanvasBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(235.dp)
                        .testTag("sales_analytics_canvas_container")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .pointerInput(activeBuckets, activeMetricComparisonMode) {
                                    detectTapGestures { tapOffset ->
                                        if (activeBuckets.isNotEmpty() && size.width > 0) {
                                            val slotWidth = size.width.toFloat() / activeBuckets.size.toFloat()
                                            val tappedIdx = (tapOffset.x / slotWidth)
                                                .toInt()
                                                .coerceIn(0, activeBuckets.size - 1)
                                            onSelectBucketIndex(tappedIdx)
                                        }
                                    }
                                }
                                .pointerInput(activeBuckets, activeMetricComparisonMode) {
                                    detectHorizontalDragGestures { change, _ ->
                                        change.consume()
                                        if (activeBuckets.isNotEmpty() && size.width > 0) {
                                            val slotWidth = size.width.toFloat() / activeBuckets.size.toFloat()
                                            val draggedIdx = (change.position.x / slotWidth)
                                                .toInt()
                                                .coerceIn(0, activeBuckets.size - 1)
                                            if (draggedIdx != activeSafeIndex) {
                                                onSelectBucketIndex(draggedIdx)
                                            }
                                        }
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val widthPx = size.width
                                val heightPx = size.height
                                val count = activeBuckets.size.coerceAtLeast(1)
                                val slotWidth = widthPx / count.toFloat()
                                val reveal = 1f

                                // 1. Horizontal Grid Lines
                                val gridSteps = 4
                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                for (step in 0..gridSteps) {
                                    val y = heightPx * (step.toFloat() / gridSteps.toFloat())
                                    drawLine(
                                        color = canvasGridLineCol,
                                        start = Offset(0f, y),
                                        end = Offset(widthPx, y),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = if (step < gridSteps) dashEffect else null
                                    )
                                }

                                // 2. Smooth Sliding Highlight Column for Selected Bucket
                                if (activeBuckets.isNotEmpty()) {
                                    val selLeft = animatedSelectedSlot.coerceIn(0f, (count - 1).toFloat()) * slotWidth
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                activeMetricComparisonMode.primaryColor.copy(alpha = 0.16f),
                                                activeMetricComparisonMode.primaryColor.copy(alpha = 0.02f)
                                            )
                                        ),
                                        topLeft = Offset(selLeft + 2f, 0f),
                                        size = Size((slotWidth - 4f).coerceAtLeast(4f), heightPx),
                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                    )
                                }

                                val primaryPoints = mutableListOf<Offset>()
                                val secondaryPoints = mutableListOf<Offset>()
                                val staggerSpan = 0.42f

                                activeBuckets.forEachIndexed { idx, bucket ->
                                    val primaryVal = when (activeMetricComparisonMode) {
                                        SalesMetricComparisonMode.SALES_VS_PROFIT -> bucket.totalSales
                                        SalesMetricComparisonMode.CASH_VS_DEBT -> bucket.collectedSales
                                    }
                                    val secondaryVal = when (activeMetricComparisonMode) {
                                        SalesMetricComparisonMode.SALES_VS_PROFIT -> bucket.netProfit
                                        SalesMetricComparisonMode.CASH_VS_DEBT -> bucket.debtSales
                                    }

                                    // Staggered spring factor per bucket index (0..count-1)
                                    val normalizedIdx = if (count > 1) idx.toFloat() / (count - 1).toFloat() else 0f
                                    val bucketReveal = ((reveal * (1f + staggerSpan)) - (normalizedIdx * staggerSpan))
                                        .coerceIn(0f, 1.08f)

                                    val primaryRatio = ((primaryVal / maxChartValue).toFloat().coerceIn(0f, 1f) * bucketReveal)
                                        .coerceIn(0f, 1f)
                                    val secondaryRatio = ((secondaryVal / maxChartValue).toFloat().coerceIn(0f, 1f) * bucketReveal)
                                        .coerceIn(0f, 1f)

                                    val centerX = (idx + 0.5f) * slotWidth
                                    val primaryY = heightPx - (primaryRatio * (heightPx - 10.dp.toPx()))
                                    val secondaryY = heightPx - (secondaryRatio * (heightPx - 10.dp.toPx()))

                                    primaryPoints.add(Offset(centerX, primaryY))
                                    secondaryPoints.add(Offset(centerX, secondaryY))

                                    // 3. Draw Dual Bars with Staggered Growth when BAR_ONLY or BAR_AND_TREND is active
                                    if (activeChartDisplayMode != SalesChartDisplayMode.TREND_ONLY) {
                                        val isSelected = idx == activeSafeIndex
                                        val barScaleBoost = if (isSelected) 1.08f else 1f
                                        val barGroupWidth = (slotWidth * 0.68f * barScaleBoost).coerceAtMost(38.dp.toPx())
                                        val singleBarW = (barGroupWidth * 0.46f).coerceAtLeast(3.dp.toPx())
                                        val gap = (barGroupWidth * 0.08f).coerceAtLeast(1.dp.toPx())

                                        val minVisibleHeight = if (bucketReveal > 0.05f) 1.5.dp.toPx() else 0f
                                        val pBarHeight = (heightPx - primaryY).coerceAtLeast(
                                            if (primaryVal > 0) 4.dp.toPx() * bucketReveal.coerceAtMost(1f) else minVisibleHeight
                                        )
                                        val sBarHeight = (heightPx - secondaryY).coerceAtLeast(
                                            if (secondaryVal > 0) 4.dp.toPx() * bucketReveal.coerceAtMost(1f) else minVisibleHeight
                                        )

                                        val alphaFactor = if (isSelected) 1f else 0.78f

                                        // Primary Bar
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    activeMetricComparisonMode.primaryColor.copy(alpha = alphaFactor),
                                                    activeMetricComparisonMode.primaryColor.copy(alpha = alphaFactor * 0.55f)
                                                ),
                                                startY = heightPx - pBarHeight,
                                                endY = heightPx
                                            ),
                                            topLeft = Offset(centerX - gap / 2f - singleBarW, heightPx - pBarHeight),
                                            size = Size(singleBarW, pBarHeight),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )

                                        // Secondary Bar
                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    activeMetricComparisonMode.secondaryColor.copy(alpha = alphaFactor),
                                                    activeMetricComparisonMode.secondaryColor.copy(alpha = alphaFactor * 0.55f)
                                                ),
                                                startY = heightPx - sBarHeight,
                                                endY = heightPx
                                            ),
                                            topLeft = Offset(centerX + gap / 2f, heightPx - sBarHeight),
                                            size = Size(singleBarW, sBarHeight),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                    }
                                }

                                // 4. Draw Smooth Progressive Trend Line & Area when TREND_ONLY or BAR_AND_TREND is active
                                if (activeChartDisplayMode != SalesChartDisplayMode.BAR_ONLY && primaryPoints.size >= 2) {
                                    val linePath = Path().apply {
                                        moveTo(primaryPoints.first().x, primaryPoints.first().y)
                                        for (i in 0 until primaryPoints.size - 1) {
                                            val p0 = primaryPoints[i]
                                            val p1 = primaryPoints[i + 1]
                                            val ctrlX = (p0.x + p1.x) / 2f
                                            cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                        }
                                    }

                                    val curveClipRight = (widthPx * reveal.coerceIn(0f, 1f)).coerceAtLeast(0f)

                                    clipRect(
                                        left = 0f,
                                        top = 0f,
                                        right = curveClipRight,
                                        bottom = heightPx
                                    ) {
                                        if (activeChartDisplayMode == SalesChartDisplayMode.TREND_ONLY) {
                                            val fillPath = Path().apply {
                                                addPath(linePath)
                                                lineTo(primaryPoints.last().x, heightPx)
                                                lineTo(primaryPoints.first().x, heightPx)
                                                close()
                                            }
                                            drawPath(
                                                path = fillPath,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        activeMetricComparisonMode.primaryColor.copy(alpha = 0.32f),
                                                        Color.Transparent
                                                    ),
                                                    startY = 0f,
                                                    endY = heightPx
                                                )
                                            )

                                            // Secondary trend line in TREND_ONLY mode
                                            val secPath = Path().apply {
                                                moveTo(secondaryPoints.first().x, secondaryPoints.first().y)
                                                for (i in 0 until secondaryPoints.size - 1) {
                                                    val p0 = secondaryPoints[i]
                                                    val p1 = secondaryPoints[i + 1]
                                                    val ctrlX = (p0.x + p1.x) / 2f
                                                    cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                                }
                                            }
                                            drawPath(
                                                path = secPath,
                                                color = activeMetricComparisonMode.secondaryColor,
                                                style = Stroke(
                                                    width = 2.2.dp.toPx(),
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }

                                        drawPath(
                                            path = linePath,
                                            color = if (activeChartDisplayMode == SalesChartDisplayMode.BAR_AND_TREND) {
                                                barTrendLineColor
                                            } else {
                                                activeMetricComparisonMode.primaryColor
                                            },
                                            style = Stroke(
                                                width = 2.6.dp.toPx(),
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }

                                    primaryPoints.forEachIndexed { idx, pt ->
                                        val normalizedIdx = if (count > 1) idx.toFloat() / (count - 1).toFloat() else 0f
                                        if (reveal >= normalizedIdx * 0.85f) {
                                            val isSel = idx == activeSafeIndex
                                            val pointColor = if (activeChartDisplayMode == SalesChartDisplayMode.BAR_AND_TREND) {
                                                barTrendLineColor
                                            } else {
                                                activeMetricComparisonMode.primaryColor
                                            }

                                            if (isSel) {
                                                drawCircle(
                                                    color = pointColor.copy(alpha = 0.28f),
                                                    radius = 6.dp.toPx() * pulseRadiusFactor,
                                                    center = pt
                                                )
                                            }

                                            drawCircle(
                                                color = canvasDotRingCol,
                                                radius = if (isSel) 5.5.dp.toPx() else 3.5.dp.toPx(),
                                                center = pt
                                            )
                                            drawCircle(
                                                color = pointColor,
                                                radius = if (isSel) 4.dp.toPx() else 2.5.dp.toPx(),
                                                center = pt
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // X-Axis Labels Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val step = when {
                                activeBuckets.size > 15 -> 5
                                activeBuckets.size > 8 -> 2
                                else -> 1
                            }
                            activeBuckets.forEachIndexed { idx, bucket ->
                                val showLabel = idx % step == 0 || idx == activeBuckets.lastIndex || idx == activeSafeIndex
                                if (showLabel) {
                                    val isSel = idx == activeSafeIndex
                                    Text(
                                        text = bucket.shortLabel,
                                        fontSize = 9.5.sp,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSel) semantic.infoBlue else semantic.chartAxisLabel,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Horizontal Bucket Selector Pills with Spring Scale & Auto-Scroll
            LazyRow(
                state = pillListState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                itemsIndexed(buckets, key = { idx, b -> "${selectedPeriod.name}_${idx}_${b.startMillis}" }) { idx, bucket ->
                    val isSel = idx == safeSelectedIndex
                    val pillScale by animateFloatAsState(
                        targetValue = if (isSel) 1.07f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "bucket_pill_scale_$idx"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        animationSpec = tween(180),
                        label = "bucket_pill_bg_$idx"
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = bgColor,
                        border = BorderStroke(
                            1.dp,
                            if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = pillScale
                                scaleY = pillScale
                            }
                            .clickable { onSelectBucketIndex(idx) }
                            .testTag("bucket_pill_$idx")
                    ) {
                        Text(
                            text = bucket.shortLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Interactive Selected Day/Month Details Card
            if (selectedBucket != null) {
                SelectedTimeBucketDetailsCard(
                    bucket = selectedBucket,
                    currencySymbol = currencySymbol
                )
            }
        }
    }
}

@Composable
private fun ChartLegendDot(
    label: String,
    color: Color
) {
    val animatedDotColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(220),
        label = "legend_dot_color"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(animatedDotColor)
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SelectedTimeBucketDetailsCard(
    bucket: SalesTimeBucket,
    currencySymbol: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_bucket_details_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bucket.fullTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${bucket.transactionsCount} عملية • ${bucket.unitsSoldCount} قطعة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BucketDetailStatItem(
                    label = "إجمالي المبيعات",
                    amount = bucket.totalSales,
                    currencySymbol = currencySymbol,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f)
                )
                BucketDetailStatItem(
                    label = "صافي الربح (${String.format(Locale.US, "%.0f", bucket.profitMarginPercent)}%)",
                    amount = bucket.netProfit,
                    currencySymbol = currencySymbol,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BucketDetailStatItem(
                    label = "النقدي والتحويل",
                    amount = bucket.collectedSales,
                    currencySymbol = currencySymbol,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                BucketDetailStatItem(
                    label = "الآجل (الديون)",
                    amount = bucket.debtSales,
                    currencySymbol = currencySymbol,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }

            if (bucket.topProductName.isNotBlank()) {
                Text(
                    text = "الأعلى مبيعاً في هذه الفترة: ${bucket.topProductName}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BucketDetailStatItem(
    label: String,
    amount: Double,
    currencySymbol: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedMoneyText(
                targetAmount = amount,
                currencySymbol = currencySymbol,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.5.sp,
                color = color
            )
        }
    }
}

private data class PaymentSliceInfo(
    val id: String,
    val labelAr: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun PaymentMethodsDonutChartCard(
    overview: PeriodOverviewSummary,
    currencySymbol: String
) {
    val total = (overview.cashSales + overview.debtSales + overview.transferSales).coerceAtLeast(0.0)
    val slices = remember(overview) {
        val divisor = if (total > 0.0) total else 1.0
        listOf(
            PaymentSliceInfo(
                id = "CASH",
                labelAr = "نقدي (كاش)",
                amount = overview.cashSales,
                percentage = ((overview.cashSales / divisor) * 100.0).toFloat(),
                color = Color(0xFF10B981)
            ),
            PaymentSliceInfo(
                id = "DEBT",
                labelAr = "آجل (ديون العملاء)",
                amount = overview.debtSales,
                percentage = ((overview.debtSales / divisor) * 100.0).toFloat(),
                color = Color(0xFFF59E0B)
            ),
            PaymentSliceInfo(
                id = "TRANSFER",
                labelAr = "تحويل بنكي/إلكتروني",
                amount = overview.transferSales,
                percentage = ((overview.transferSales / divisor) * 100.0).toFloat(),
                color = Color(0xFFA855F7)
            )
        )
    }

    var selectedSliceIndex by remember { mutableIntStateOf(0) }
    val activeSlice = slices.getOrNull(selectedSliceIndex) ?: slices.first()

    val animatedPercentages = remember(slices) {
        slices.map { it.percentage }
    }
    val animatedActivePercent = activeSlice.percentage

    val semantic = LocalAFStoreSemanticColors.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (semantic.isDark) 0.dp else 2.dp
        ),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_methods_donut_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = semantic.profitGreen.copy(alpha = 0.14f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = null,
                            tint = semantic.profitGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "توزيع المبيعات حسب طرق الدفع",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp
                    )
                    Text(
                        text = "اضغط على أي طريقة دفع أو قطاع في الدائرة لعرض نسبته وقيمته",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val emptyArcColor = semantic.chartGridLine
                // Interactive Animated Donut Chart Canvas
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .pointerInput(slices, total) {
                            detectTapGestures { tapOffset ->
                                if (total <= 0.0) return@detectTapGestures
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val dx = tapOffset.x - centerX
                                val dy = tapOffset.y - centerY
                                val dist = hypot(dx, dy)
                                val outerR = size.width / 2f
                                if (dist in (outerR * 0.35f)..outerR) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                    if (angle < 0f) angle += 360f
                                    var accAngle = 0f
                                    slices.forEachIndexed { idx, s ->
                                        val sweep = (s.percentage / 100f) * 360f
                                        if (angle in accAngle..(accAngle + sweep)) {
                                            selectedSliceIndex = idx
                                            return@detectTapGestures
                                        }
                                        accAngle += sweep
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseStrokeWidthPx = 21.dp.toPx()
                        val selectedStrokePx = 27.dp.toPx()
                        val diameter = size.minDimension - selectedStrokePx
                        val topLeftOffset = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )
                        val arcSize = Size(diameter, diameter)
                        val revealFactor = 1f

                        if (total <= 0.0) {
                            drawArc(
                                color = emptyArcColor,
                                startAngle = -90f,
                                sweepAngle = 360f * revealFactor,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = arcSize,
                                style = Stroke(width = baseStrokeWidthPx, cap = StrokeCap.Butt)
                            )
                        } else {
                            // Subtle rotational spin-in effect during reveal
                            var startAngle = -90f - ((1f - revealFactor) * 40f)
                            slices.forEachIndexed { idx, slice ->
                                val pct = animatedPercentages.getOrElse(idx) { slice.percentage }
                                val sweep = ((pct / 100f) * 360f) * revealFactor
                                if (sweep > 0.5f) {
                                    val isSel = idx == selectedSliceIndex
                                    drawArc(
                                        color = if (isSel) slice.color else slice.color.copy(alpha = 0.72f),
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = topLeftOffset,
                                        size = arcSize,
                                        style = Stroke(
                                            width = if (isSel) selectedStrokePx else baseStrokeWidthPx,
                                            cap = StrokeCap.Butt
                                        )
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.0f", animatedActivePercent)}%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = activeSlice.color
                        )
                        Text(
                            text = activeSlice.labelAr.substringBefore(" "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                // Interactive Payment Legend List with Spring Selection Feedback
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slices.forEachIndexed { idx, slice ->
                        val isSel = idx == selectedSliceIndex
                        val itemScale by animateFloatAsState(
                            targetValue = if (isSel) 1.03f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "slice_card_scale_$idx"
                        )
                        val animatedPct = animatedPercentages.getOrElse(idx) { slice.percentage }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) {
                                slice.color.copy(alpha = 0.14f)
                            } else {
                                semantic.cardSubtle
                            },
                            border = BorderStroke(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) slice.color else semantic.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                }
                                .clickable { selectedSliceIndex = idx }
                                .testTag("payment_slice_${slice.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Column {
                                        Text(
                                            text = slice.labelAr,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        AnimatedMoneyText(
                                            targetAmount = slice.amount,
                                            currencySymbol = currencySymbol,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = slice.color
                                        )
                                    }
                                }

                                Text(
                                    text = "${String.format(Locale.US, "%.1f", animatedPct)}%",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = slice.color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesReportPdfActionCard(
    selectedPeriod: SalesReportPeriod,
    chartDisplayMode: SalesChartDisplayMode,
    metricComparisonMode: SalesMetricComparisonMode,
    onOpenExportDialog: () -> Unit,
    onQuickSharePdf: () -> Unit,
    onQuickPrintPdf: () -> Unit,
    onQuickSavePdf: () -> Unit,
    onOpenCsvDialog: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0284C7).copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sales_report_pdf_action_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.18f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "تصدير وطباعة ومشاركة التقرير",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "تصدير PDF وتصدير CSV للمبيعات (${selectedPeriod.labelAr})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onOpenExportDialog,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("open_pdf_export_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("خيارات PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onOpenCsvDialog != null) {
                    Button(
                        onClick = onOpenCsvDialog,
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("export_sales_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("تصدير CSV", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onQuickSharePdf,
                    shape = RoundedCornerShape(11.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_share_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("مشاركة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onQuickPrintPdf,
                    shape = RoundedCornerShape(11.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_print_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("طباعة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onQuickSavePdf,
                    shape = RoundedCornerShape(11.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_save_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("حفظ PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SalesReportPdfExportDialog(
    storeName: String,
    selectedPeriod: SalesReportPeriod,
    chartDisplayMode: SalesChartDisplayMode,
    metricComparisonMode: SalesMetricComparisonMode,
    selectedBucket: SalesTimeBucket?,
    overview: PeriodOverviewSummary,
    currencySymbol: String,
    includeTimeBucketsTable: Boolean,
    onIncludeTimeBucketsTableChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit,
    onPrintPdf: () -> Unit,
    onSavePdfToDevice: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFEF4444)
                )
                Column {
                    Text(
                        text = "تصدير ملخص التقارير والرسوم البيانية (PDF)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp
                    )
                    Text(
                        text = "جاهز للطباعة أو الحفظ أو المشاركة المباشرة",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "محتويات تقرير PDF المحدد:",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("• المتجر: $storeName", fontSize = 11.5.sp)
                        Text("• النطاق الزمني: ${selectedPeriod.labelAr} (${selectedPeriod.subtitleAr})", fontSize = 11.5.sp)
                        Text("• الرسم البياني: ${chartDisplayMode.labelAr} (${metricComparisonMode.labelAr})", fontSize = 11.5.sp)
                        if (selectedBucket != null) {
                            Text("• اليوم/الشهر المميز: ${selectedBucket.fullTitle}", fontSize = 11.5.sp)
                        }
                        Text(
                            text = "• إجمالي المبيعات: ${String.format(Locale.US, "%.2f", overview.totalSales)} $currencySymbol | صافي الربح: ${String.format(Locale.US, "%.2f", overview.netProfit)} $currencySymbol",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text("• مخطط دائرة طرق الدفع + قائمة أعلى الأصناف مبيعاً", fontSize = 11.5.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تضمين الجدول التفصيلي للأيام/الأشهر",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "إرفاق جدول كامل بجميع قيم كل يوم أو شهر في نهاية التقرير",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeTimeBucketsTable,
                            onCheckedChange = onIncludeTimeBucketsTableChange,
                            modifier = Modifier.testTag("toggle_pdf_include_table")
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSharePdf,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_share_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("مشاركة ملف PDF عبر التطبيقات", fontWeight = FontWeight.ExtraBold)
                    }

                    Button(
                        onClick = onPrintPdf,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_print_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("طباعة التقرير والرسوم البيانية", fontWeight = FontWeight.ExtraBold)
                    }

                    OutlinedButton(
                        onClick = onSavePdfToDevice,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_save_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("حفظ ملف PDF في ذاكرة الهاتف", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

