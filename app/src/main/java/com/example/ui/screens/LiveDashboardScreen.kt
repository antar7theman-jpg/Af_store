package com.example.ui.screens

import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainAppTab
import com.example.data.models.Customer
import com.example.data.models.Product
import com.example.data.models.Sale
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class HourlyAreaChartMode(
    val labelAr: String,
    val primarySeriesAr: String,
    val secondarySeriesAr: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    SALES_AND_PROFIT(
        labelAr = "المبيعات وصافي الربح",
        primarySeriesAr = "مبيعات الساعة",
        secondarySeriesAr = "صافي الربح",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF10B981)
    ),
    TODAY_VS_YESTERDAY(
        labelAr = "اليوم مقابل الأمس",
        primarySeriesAr = "مبيعات اليوم",
        secondarySeriesAr = "مبيعات الأمس",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFFA855F7)
    )
}

data class HourlySalesBucket(
    val slotIndex: Int,
    val startHour: Int,
    val endHour: Int,
    val shortLabelAr: String,
    val fullRangeAr: String,
    val todaySales: Double,
    val todayProfit: Double,
    val todayTransactions: Int,
    val todayUnitsSold: Int,
    val yesterdaySales: Double
)

data class DashboardLiveSummary(
    val todaySales: Double,
    val yesterdaySales: Double,
    val growthPercentVsYesterday: Double,
    val todayNetProfit: Double,
    val todayProfitMarginPercent: Double,
    val todayCashSales: Double,
    val todayDebtSales: Double,
    val todayTransferSales: Double,
    val todayTransactionsCount: Int,
    val todayUnitsSold: Int,
    val averageTicketValue: Double,
    val peakHourBucket: HourlySalesBucket?,
    val totalCustomerDebt: Double,
    val debtorCustomersCount: Int,
    val lowStockProducts: List<Pair<Product, Int>>,
    val outOfStockCount: Int,
    val hourlyBuckets: List<HourlySalesBucket>
)

object LiveDashboardAnalyticsEngine {

    fun buildLiveSummary(
        sales: List<Sale>,
        products: List<Product>,
        customers: List<Customer>,
        defaultMinStock: Int,
        customMinStockMap: Map<Int, Int>,
        nowMillis: Long = System.currentTimeMillis()
    ): DashboardLiveSummary {
        val productCostMap = products.associate { it.id to it.purchasePrice }

        val todayStartCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMillis = todayStartCal.timeInMillis
        val todayEndMillis = todayStartMillis + 24L * 3600_000L - 1L

        val yesterdayStartMillis = todayStartMillis - 24L * 3600_000L
        val yesterdayEndMillis = todayStartMillis - 1L

        val todaySalesList = sales.filter { it.saleDate in todayStartMillis..todayEndMillis }
        val yesterdaySalesList = sales.filter { it.saleDate in yesterdayStartMillis..yesterdayEndMillis }

        fun calcSaleProfit(sale: Sale): Double {
            val qtySafe = sale.quantitySold.coerceAtLeast(1)
            val unitCost = productCostMap[sale.productId] ?: ((sale.totalPrice / qtySafe) * 0.72)
            return (sale.totalPrice - (unitCost * qtySafe)).coerceAtLeast(0.0)
        }

        val todayTotalSales = todaySalesList.sumOf { it.totalPrice }
        val yesterdayTotalSales = yesterdaySalesList.sumOf { it.totalPrice }
        val todayNetProfit = todaySalesList.sumOf { calcSaleProfit(it) }

        val growthPercent = when {
            yesterdayTotalSales > 0.0 -> ((todayTotalSales - yesterdayTotalSales) / yesterdayTotalSales) * 100.0
            todayTotalSales > 0.0 -> 100.0
            else -> 0.0
        }

        val todayCash = todaySalesList.filter {
            !it.isDebt && !it.paymentMethod.equals("TRANSFER", ignoreCase = true)
        }.sumOf { it.totalPrice }
        val todayTransfer = todaySalesList.filter {
            !it.isDebt && it.paymentMethod.equals("TRANSFER", ignoreCase = true)
        }.sumOf { it.totalPrice }
        val todayDebt = todaySalesList.filter {
            it.isDebt || it.paymentMethod.equals("DEBT", ignoreCase = true)
        }.sumOf { it.totalPrice }

        val todayTxCount = todaySalesList
            .map { "${it.saleDate / 1000}_${it.customerId ?: 0}" }
            .distinct()
            .size
            .let { if (it == 0 && todaySalesList.isNotEmpty()) todaySalesList.size else it }

        val todayUnits = todaySalesList.sumOf { it.quantitySold }
        val avgTicket = if (todayTxCount > 0) todayTotalSales / todayTxCount else 0.0
        val profitMargin = if (todayTotalSales > 0.0) {
            ((todayNetProfit / todayTotalSales) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        // Build 12 two-hour buckets across the 24h day for clean Recharts hourly curve
        val calHelper = Calendar.getInstance()
        val hourlyBuckets = (0 until 12).map { idx ->
            val startH = idx * 2
            val endH = startH + 1

            val slotTodaySales = todaySalesList.filter { s ->
                calHelper.timeInMillis = s.saleDate
                val h = calHelper.get(Calendar.HOUR_OF_DAY)
                h in startH..endH
            }
            val slotYesterdaySales = yesterdaySalesList.filter { s ->
                calHelper.timeInMillis = s.saleDate
                val h = calHelper.get(Calendar.HOUR_OF_DAY)
                h in startH..endH
            }

            HourlySalesBucket(
                slotIndex = idx,
                startHour = startH,
                endHour = endH,
                shortLabelAr = formatHourShortAr(startH),
                fullRangeAr = "${formatHourFullAr(startH)} - ${formatHourFullAr((endH + 1) % 24)}",
                todaySales = slotTodaySales.sumOf { it.totalPrice },
                todayProfit = slotTodaySales.sumOf { calcSaleProfit(it) },
                todayTransactions = slotTodaySales.size,
                todayUnitsSold = slotTodaySales.sumOf { it.quantitySold },
                yesterdaySales = slotYesterdaySales.sumOf { it.totalPrice }
            )
        }

        val peakBucket = hourlyBuckets.maxByOrNull { it.todaySales }?.takeIf { it.todaySales > 0.0 }

        val debtorCustomers = customers.filter { it.balanceDebt > 0.009 }
        val totalDebt = debtorCustomers.sumOf { it.balanceDebt }

        val activeProducts = products.filter { !it.isDeleted }
        val lowStockItems = activeProducts.mapNotNull { prod ->
            val threshold = (customMinStockMap[prod.id] ?: defaultMinStock).coerceAtLeast(1)
            if (prod.stock <= threshold) prod to threshold else null
        }.sortedBy { it.first.stock }

        val outOfStockCount = activeProducts.count { it.stock <= 0 }

        return DashboardLiveSummary(
            todaySales = todayTotalSales,
            yesterdaySales = yesterdayTotalSales,
            growthPercentVsYesterday = growthPercent,
            todayNetProfit = todayNetProfit,
            todayProfitMarginPercent = profitMargin,
            todayCashSales = todayCash,
            todayDebtSales = todayDebt,
            todayTransferSales = todayTransfer,
            todayTransactionsCount = todayTxCount,
            todayUnitsSold = todayUnits,
            averageTicketValue = avgTicket,
            peakHourBucket = peakBucket,
            totalCustomerDebt = totalDebt,
            debtorCustomersCount = debtorCustomers.size,
            lowStockProducts = lowStockItems,
            outOfStockCount = outOfStockCount,
            hourlyBuckets = hourlyBuckets
        )
    }

    private fun formatHourShortAr(hour24: Int): String {
        val suffix = if (hour24 >= 12) "م" else "ص"
        val h12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return "$h12$suffix"
    }

    private fun formatHourFullAr(hour24: Int): String {
        val suffix = if (hour24 >= 12) "مساءً" else "صباحاً"
        val h12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%02d:00 %s", h12, suffix)
    }
}

@Composable
fun LiveDashboardScreen(
    viewModel: AntarSalesViewModel,
    onNavigateToTab: (MainAppTab) -> Unit
) {
    val sales by viewModel.sales.collectAsState()
    val products by viewModel.products.collectAsState()
    val liveSummary by viewModel.dashboardLiveSummary.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val canViewReports by viewModel.canViewReports.collectAsState()

    var dailyBarPeriod by rememberSaveable { mutableStateOf(SalesReportPeriod.DAILY_7) }
    var hourlyChartMode by rememberSaveable { mutableStateOf(HourlyAreaChartMode.SALES_AND_PROFIT) }
    var useWeeklyForDonut by rememberSaveable { mutableStateOf(false) }

    val dailyDataPair by produceState(
        initialValue = remember {
            val initBuckets = SalesAnalyticsAggregator.buildTimeBuckets(
                sales = sales,
                products = products,
                period = dailyBarPeriod
            )
            val initOverview = SalesAnalyticsAggregator.buildOverviewSummary(
                buckets = initBuckets,
                allSales = sales,
                products = products
            )
            initBuckets to initOverview
        },
        sales,
        products,
        dailyBarPeriod
    ) {
        value = withContext(Dispatchers.Default) {
            val computedBuckets = SalesAnalyticsAggregator.buildTimeBuckets(
                sales = sales,
                products = products,
                period = dailyBarPeriod
            )
            val computedOverview = SalesAnalyticsAggregator.buildOverviewSummary(
                buckets = computedBuckets,
                allSales = sales,
                products = products
            )
            computedBuckets to computedOverview
        }
    }

    val dailyBuckets = dailyDataPair.first
    val weeklyOverview = dailyDataPair.second

    val currentHourSlotIndex = remember {
        (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) / 2).coerceIn(0, 11)
    }
    var selectedHourlyIndex by remember(liveSummary.hourlyBuckets) {
        val bestIdx = liveSummary.peakHourBucket?.slotIndex ?: currentHourSlotIndex
        mutableIntStateOf(bestIdx.coerceIn(0, (liveSummary.hourlyBuckets.size - 1).coerceAtLeast(0)))
    }
    var selectedDailyIndex by remember(dailyBarPeriod, dailyBuckets.size) {
        mutableIntStateOf((dailyBuckets.size - 1).coerceAtLeast(0))
    }

    val liveDotAlpha = 1f
    val recentSales = remember(sales) { sales.take(5) }
    var showDashboardPdfDialog by remember { mutableStateOf(false) }

    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 650

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isWideScreen) 20.dp else 14.dp, vertical = 10.dp)
            .testTag("live_dashboard_screen_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 42.dp)
    ) {
        // 1. Live Dashboard Header Banner
        item(key = "dashboard_header", contentType = "header") {
            val semantic = LocalAFStoreSemanticColors.current
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = semantic.headerContainer,
                border = BorderStroke(1.dp, semantic.headerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(semantic.profitGreen.copy(alpha = liveDotAlpha))
                            )
                            Text(
                                text = "لوحة التحكم الحية • $storeName",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.5.sp,
                                color = semantic.onHeaderContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "مؤشرات Recharts التفاعلية للمبيعات بالساعة والأرباح وطرق الدفع",
                            fontSize = 11.5.sp,
                            color = semantic.onHeaderSubtitle
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.infoBlue.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, semantic.infoBlue.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { showDashboardPdfDialog = true }
                            .testTag("dashboard_to_reports_header_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = semantic.infoBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "التقارير PDF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.infoBlue
                            )
                        }
                    }
                }
            }
        }

        // 2. Room Database & Cloud Sync Status Indicator ('Synced', 'Syncing...', or 'Offline Mode')
        item(key = "home_sync_status", contentType = "sync_status") {
            val semantic = LocalAFStoreSemanticColors.current
            val isLiveSyncActive by viewModel.liveInventorySyncManager.isLiveSyncActive.collectAsState()
            val isSyncing by viewModel.liveInventorySyncManager.isComprehensiveSyncing.collectAsState()
            val pendingQueueCount by viewModel.repository.pendingSyncQueueCountFlow.collectAsState(initial = 0)

            val (statusText, statusColor, statusIcon) = when {
                isSyncing -> Triple("جارٍ المزامنة (Syncing...)", semantic.warning, Icons.Default.CloudSync)
                isLiveSyncActive -> Triple("تمت المزامنة (Synced) ✓", semantic.profitGreen, Icons.Default.CloudDone)
                else -> Triple("وضع دون إنترنت (Offline Mode)", semantic.cardBorder, Icons.Default.CloudOff)
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().testTag("home_sync_status_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "حالة مزامنة قاعدة بيانات Room والسحابة:",
                                fontSize = 11.sp,
                                color = semantic.onHeaderSubtitle
                            )
                            Text(
                                text = statusText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    if (pendingQueueCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = semantic.warning.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, semantic.warning)
                        ) {
                            Text(
                                text = "معلق: $pendingQueueCount",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = semantic.warning
                            )
                        }
                    }
                }
            }
        }

        if (canViewReports) {
            // 3. 4 Executive Live KPI Cards (Sales, Net Profit, Customer Debt, Low-Stock Alerts)
            item(key = "dashboard_kpi_grid", contentType = "kpi_grid") {
                DashboardLiveKpiGrid(
                    summary = liveSummary,
                    currencySymbol = currencySymbol,
                    isWideScreen = isWideScreen,
                    onNavigateToTab = onNavigateToTab
                )
            }
        } else {
            item(key = "restricted_reports_card", contentType = "restricted") {
                val semantic = LocalAFStoreSemanticColors.current
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = semantic.cardElevated,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "المعلومات المالية والأرباح مقتصرة على الإدارة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "حسابك الحالي (عامل) لا يملك صلاحية عرض الأرباح، إجمالي المبيعات، أو التقارير المالية.",
                            fontSize = 12.5.sp,
                            color = semantic.onHeaderSubtitle,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 3. Quick Action Launcher Bar
        item(key = "dashboard_quick_actions", contentType = "quick_actions") {
            DashboardQuickActionsBar(onNavigateToTab = onNavigateToTab)
        }

        if (isWideScreen) {
            // Tablet / Wide Screen: Side-by-side Dual Column Charts
            item(key = "dashboard_wide_row_1", contentType = "charts_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        RechartsHourlySalesAreaChartCard(
                            summary = liveSummary,
                            currencySymbol = currencySymbol,
                            chartMode = hourlyChartMode,
                            onChartModeChange = { hourlyChartMode = it },
                            selectedIndex = selectedHourlyIndex,
                            onSelectIndex = { selectedHourlyIndex = it }
                        )
                    }
                    Box(modifier = Modifier.weight(0.8f)) {
                        RechartsPaymentDonutChartCard(
                            liveSummary = liveSummary,
                            weeklyOverview = weeklyOverview,
                            useWeeklyScope = useWeeklyForDonut,
                            onToggleScope = { useWeeklyForDonut = it },
                            currencySymbol = currencySymbol
                        )
                    }
                }
            }

            item(key = "dashboard_wide_row_2", contentType = "charts_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1.15f)) {
                        RechartsDailyComposedBarChartCard(
                            buckets = dailyBuckets,
                            selectedPeriod = dailyBarPeriod,
                            onPeriodChange = { dailyBarPeriod = it },
                            selectedIndex = selectedDailyIndex,
                            onSelectIndex = { selectedDailyIndex = it },
                            currencySymbol = currencySymbol
                        )
                    }
                    Box(modifier = Modifier.weight(0.85f)) {
                        DashboardLowStockAndRecentSalesCard(
                            lowStockProducts = liveSummary.lowStockProducts,
                            recentSales = recentSales,
                            currencySymbol = currencySymbol,
                            onNavigateToTab = onNavigateToTab
                        )
                    }
                }
            }
        } else {
            // Compact Phone: Vertical Stack Charts
            // 4. Recharts-Style Hourly Sales & Profit Area Chart
            item(key = "dashboard_hourly_chart", contentType = "hourly_chart") {
                RechartsHourlySalesAreaChartCard(
                    summary = liveSummary,
                    currencySymbol = currencySymbol,
                    chartMode = hourlyChartMode,
                    onChartModeChange = { hourlyChartMode = it },
                    selectedIndex = selectedHourlyIndex,
                    onSelectIndex = { selectedHourlyIndex = it }
                )
            }

            // 5. Recharts-Style Daily Performance Composed Bar & Trend Chart
            item(key = "dashboard_daily_chart", contentType = "daily_chart") {
                RechartsDailyComposedBarChartCard(
                    buckets = dailyBuckets,
                    selectedPeriod = dailyBarPeriod,
                    onPeriodChange = { dailyBarPeriod = it },
                    selectedIndex = selectedDailyIndex,
                    onSelectIndex = { selectedDailyIndex = it },
                    currencySymbol = currencySymbol
                )
            }

            // 6. Recharts-Style Interactive Payment Methods Donut Chart
            item(key = "dashboard_payment_donut", contentType = "payment_donut") {
                RechartsPaymentDonutChartCard(
                    liveSummary = liveSummary,
                    weeklyOverview = weeklyOverview,
                    useWeeklyScope = useWeeklyForDonut,
                    onToggleScope = { useWeeklyForDonut = it },
                    currencySymbol = currencySymbol
                )
            }

            // 7. Low-Stock Alerts & Live Recent Sales Activity Feed
            item(key = "dashboard_low_stock_recent", contentType = "low_stock_recent") {
                DashboardLowStockAndRecentSalesCard(
                    lowStockProducts = liveSummary.lowStockProducts,
                    recentSales = recentSales,
                    currencySymbol = currencySymbol,
                    onNavigateToTab = onNavigateToTab
                )
            }
        }
    }

    if (showDashboardPdfDialog) {
        val kpiMetrics = listOf(
            "مبيعات اليوم" to "${String.format(Locale.US, "%.1f", liveSummary.todaySales)} $currencySymbol",
            "صافي الأرباح" to "${String.format(Locale.US, "%.1f", liveSummary.todayNetProfit)} $currencySymbol",
            "ديون العملاء" to "${String.format(Locale.US, "%.1f", liveSummary.totalCustomerDebt)} $currencySymbol",
            "النواقص" to "${liveSummary.lowStockProducts.size} صنف"
        )
        val tableHeaders = listOf("المؤشر / البند", "القيمة الإحصائية", "الحالة")
        val tableRows = listOf(
            listOf("إجمالي مبيعات اليوم", "${String.format(Locale.US, "%.1f", liveSummary.todaySales)} $currencySymbol", "مبيعات حية"),
            listOf("عدد الفواتير المنفذة", "${liveSummary.todayTransactionsCount} فاتورة", "مكتمل"),
            listOf("صافي ربح اليوم", "${String.format(Locale.US, "%.1f", liveSummary.todayNetProfit)} $currencySymbol", "أرباح صافية"),
            listOf("نسبة هامش الربح", "${String.format(Locale.US, "%.1f", liveSummary.todayProfitMarginPercent)}%", "هامش ممتاز"),
            listOf("إجمالي ديون العملاء", "${String.format(Locale.US, "%.1f", liveSummary.totalCustomerDebt)} $currencySymbol", "مستحقات"),
            listOf("الأصناف المنخفضة", "${liveSummary.lowStockProducts.size} صنف", "تنبيه طلب")
        )
        val payload = UniversalPdfReportPayload(
            reportType = SectionPdfReportType.DASHBOARD,
            storeName = storeName,
            storePhone = storePhone,
            currencySymbol = currencySymbol,
            kpiMetrics = kpiMetrics,
            tableHeaders = tableHeaders,
            tableRows = tableRows,
            notesOrSummary = "تقرير شامل مستخرج من لوحة التحكم لمتابعة الأداء اليومي والمالي ونواقص المخزون."
        )
        UniversalSectionPdfReportDialog(
            payload = payload,
            onDismiss = { showDashboardPdfDialog = false }
        )
    }
}

@Composable
private fun DashboardLiveKpiGrid(
    summary: DashboardLiveSummary,
    currencySymbol: String,
    isWideScreen: Boolean = false,
    onNavigateToTab: (MainAppTab) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val isGrowthPositive = summary.growthPercentVsYesterday >= 0.0
    val growthColor = if (isGrowthPositive) semantic.profitGreen else semantic.dangerRed
    val growthSign = if (isGrowthPositive) "+" else ""
    val cardShadow = if (semantic.isDark) 0.dp else 2.dp
    val hasAlerts = summary.lowStockProducts.isNotEmpty()
    val alertColor = if (hasAlerts) semantic.dangerRed else semantic.profitGreen

    val card1 = @Composable { modifier: Modifier ->
        // Card 1: Today's Live Sales
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = semantic.cardElevated,
            shadowElevation = cardShadow,
            border = BorderStroke(1.dp, semantic.infoBlue.copy(alpha = if (semantic.isDark) 0.45f else 0.32f)),
            modifier = modifier
                .clickable { onNavigateToTab(MainAppTab.INVOICES) }
                .testTag("dashboard_kpi_today_sales")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مبيعات اليوم الحية",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = growthColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isGrowthPositive) Icons.AutoMirrored.Filled.TrendingUp
                                else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = growthColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$growthSign${String.format(Locale.US, "%.0f", summary.growthPercentVsYesterday)}%",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = growthColor
                            )
                        }
                    }
                }

                AnimatedMoneyText(
                    targetAmount = summary.todaySales,
                    currencySymbol = currencySymbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = semantic.infoBlue
                )

                Text(
                    text = "${summary.todayTransactionsCount} فاتورة اليوم • الأمس: ${String.format(Locale.US, "%.0f", summary.yesterdaySales)} $currencySymbol",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    val card2 = @Composable { modifier: Modifier ->
        // Card 2: Today's Net Profit
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = semantic.cardElevated,
            shadowElevation = cardShadow,
            border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = if (semantic.isDark) 0.45f else 0.32f)),
            modifier = modifier
                .clickable { onNavigateToTab(MainAppTab.REPORTS) }
                .testTag("dashboard_kpi_today_profit")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صافي ربح اليوم",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = semantic.profitGreen.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "هامش ${String.format(Locale.US, "%.0f", summary.todayProfitMarginPercent)}%",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = semantic.profitGreen,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                AnimatedMoneyText(
                    targetAmount = summary.todayNetProfit,
                    currencySymbol = currencySymbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = semantic.profitGreen
                )

                Text(
                    text = "متوسط الفاتورة: ${String.format(Locale.US, "%.0f", summary.averageTicketValue)} $currencySymbol",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    val card3 = @Composable { modifier: Modifier ->
        // Card 3: Customer Debts
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = semantic.cardElevated,
            shadowElevation = cardShadow,
            border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = if (semantic.isDark) 0.45f else 0.32f)),
            modifier = modifier
                .clickable { onNavigateToTab(MainAppTab.CUSTOMERS) }
                .testTag("dashboard_kpi_customer_debt")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي ديون العملاء",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = semantic.debtAmber,
                        modifier = Modifier.size(15.dp)
                    )
                }

                AnimatedMoneyText(
                    targetAmount = summary.totalCustomerDebt,
                    currencySymbol = currencySymbol,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = semantic.debtAmber
                )

                Text(
                    text = "${summary.debtorCustomersCount} عميل مدين • آجل اليوم: ${String.format(Locale.US, "%.0f", summary.todayDebtSales)} $currencySymbol",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    val card4 = @Composable { modifier: Modifier ->
        // Card 4: Low-Stock Alerts
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = semantic.cardElevated,
            shadowElevation = cardShadow,
            border = BorderStroke(1.dp, alertColor.copy(alpha = if (semantic.isDark) 0.45f else 0.32f)),
            modifier = modifier
                .clickable { onNavigateToTab(MainAppTab.REORDER_ALERTS) }
                .testTag("dashboard_kpi_low_stock")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تنبيهات المخزون",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = alertColor,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Text(
                    text = if (hasAlerts) "${summary.lowStockProducts.size} صنف منخفض" else "المخزون مكتمل ✓",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = alertColor
                )

                Text(
                    text = if (summary.outOfStockCount > 0) {
                        "منها ${summary.outOfStockCount} صنف نفد بالكامل (0)"
                    } else {
                        "اضغط لمراجعة الكميات وإعادة الطلب"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (isWideScreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            card1(Modifier.weight(1f))
            card2(Modifier.weight(1f))
            card3(Modifier.weight(1f))
            card4(Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                card1(Modifier.weight(1f))
                card2(Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                card3(Modifier.weight(1f))
                card4(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DashboardQuickActionsBar(
    onNavigateToTab: (MainAppTab) -> Unit
) {
    val actions = listOf(
        Triple("بيع جديد", Icons.Default.PointOfSale, MainAppTab.POS to Color(0xFF10B981)),
        Triple("توريد بضاعة", Icons.Default.ShoppingBag, MainAppTab.PURCHASES to Color(0xFF38BDF8)),
        Triple("جرد المخزون", Icons.Default.Inventory2, MainAppTab.INVENTORY to Color(0xFFF59E0B)),
        Triple("أرشيف الفواتير", Icons.Default.Analytics, MainAppTab.INVOICES to Color(0xFFA855F7))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { (label, icon, tabAndColor) ->
            val (tab, accentColor) = tabAndColor
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab(tab) }
                    .testTag("dashboard_quick_${tab.name.lowercase()}_btn")
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RechartsHourlySalesAreaChartCard(
    summary: DashboardLiveSummary,
    currencySymbol: String,
    chartMode: HourlyAreaChartMode,
    onChartModeChange: (HourlyAreaChartMode) -> Unit,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val buckets = summary.hourlyBuckets
    val safeIndex = selectedIndex.coerceIn(0, (buckets.size - 1).coerceAtLeast(0))
    val selectedBucket = buckets.getOrNull(safeIndex)

    val maxHourlyVal = remember(buckets, chartMode) {
        val rawMax = buckets.maxOfOrNull { b ->
            when (chartMode) {
                HourlyAreaChartMode.SALES_AND_PROFIT -> maxOf(b.todaySales, b.todayProfit)
                HourlyAreaChartMode.TODAY_VS_YESTERDAY -> maxOf(b.todaySales, b.yesterdaySales)
            }
        } ?: 0.0
        if (rawMax <= 0.0) 100.0 else rawMax * 1.18
    }

    val semantic = LocalAFStoreSemanticColors.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (semantic.isDark) 0.dp else 2.dp
        ),
        border = BorderStroke(1.dp, semantic.cardBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recharts_hourly_area_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header + Peak Hour Badge
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
                        color = semantic.infoBlue.copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = semantic.infoBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "منحنى المساحة للمبيعات بالساعة (Recharts Area)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "اسحب أو المس أي ساعة لعرض نافذة التلميح الفورية (Tooltip)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val peak = summary.peakHourBucket
                if (peak != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = semantic.debtAmber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = semantic.debtAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "الذروة: ${peak.shortLabelAr}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.debtAmber
                            )
                        }
                    }
                }
            }

            // Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HourlyAreaChartMode.entries.forEach { mode ->
                    val selected = chartMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onChartModeChange(mode) },
                        label = {
                            Text(
                                text = mode.labelAr,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = mode.primaryColor.copy(alpha = 0.18f),
                            selectedLabelColor = mode.primaryColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hourly_chart_mode_${mode.name.lowercase()}")
                    )
                }
            }

            // Recharts Interactive Floating Tooltip Header Box
            if (selectedBucket != null) {
                val primaryVal = selectedBucket.todaySales
                val secondaryVal = when (chartMode) {
                    HourlyAreaChartMode.SALES_AND_PROFIT -> selectedBucket.todayProfit
                    HourlyAreaChartMode.TODAY_VS_YESTERDAY -> selectedBucket.yesterdaySales
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semantic.tooltipBg,
                    shadowElevation = if (semantic.isDark) 0.dp else 1.5.dp,
                    border = BorderStroke(1.dp, chartMode.primaryColor.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recharts_hourly_tooltip_box")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = semantic.tooltipSubtitle,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = selectedBucket.fullRangeAr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.tooltipTitle
                                )
                            }
                            Text(
                                text = "${selectedBucket.todayTransactions} عملية بيع (${selectedBucket.todayUnitsSold} قطعة)",
                                fontSize = 10.sp,
                                color = semantic.tooltipSubtitle
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(chartMode.primaryColor)
                                    )
                                    Text(
                                        text = chartMode.primarySeriesAr,
                                        fontSize = 10.sp,
                                        color = semantic.tooltipSubtitle
                                    )
                                }
                                AnimatedMoneyText(
                                    targetAmount = primaryVal,
                                    currencySymbol = currencySymbol,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = chartMode.primaryColor
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(chartMode.secondaryColor)
                                    )
                                    Text(
                                        text = chartMode.secondarySeriesAr,
                                        fontSize = 10.sp,
                                        color = semantic.tooltipSubtitle
                                    )
                                }
                                AnimatedMoneyText(
                                    targetAmount = secondaryVal,
                                    currencySymbol = currencySymbol,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = chartMode.secondaryColor
                                )
                            }
                        }
                    }
                }
            }

            // Recharts ResponsiveContainer AreaChart Canvas
            val gridLineCol = semantic.chartGridLine
            val crosshairCol = if (semantic.isDark) Color.White.copy(alpha = 0.45f) else chartMode.primaryColor.copy(alpha = 0.55f)
            val activeRingCol = semantic.tooltipBg

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = semantic.chartCanvasBg,
                border = BorderStroke(1.dp, semantic.chartCanvasBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(buckets.size) {
                                detectTapGestures { offset ->
                                    if (buckets.isNotEmpty() && size.width > 0) {
                                        val slotW = size.width.toFloat() / buckets.size
                                        val idx = (offset.x / slotW).toInt().coerceIn(0, buckets.lastIndex)
                                        onSelectIndex(idx)
                                    }
                                }
                            }
                            .pointerInput(buckets.size) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    if (buckets.isNotEmpty() && size.width > 0) {
                                        val slotW = size.width.toFloat() / buckets.size
                                        val idx = (change.position.x / slotW).toInt().coerceIn(0, buckets.lastIndex)
                                        onSelectIndex(idx)
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val count = buckets.size.coerceAtLeast(1)
                            val slotW = w / count.toFloat()

                            // Recharts CartesianGrid (horizontal + vertical dashed lines)
                            val gridDash = PathEffect.dashPathEffect(floatArrayOf(7f, 7f), 0f)
                            for (row in 0..3) {
                                val gy = h * (row / 3f)
                                drawLine(
                                    color = gridLineCol,
                                    start = Offset(0f, gy),
                                    end = Offset(w, gy),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = if (row < 3) gridDash else null
                                )
                            }

                            val primaryPts = mutableListOf<Offset>()
                            val secondaryPts = mutableListOf<Offset>()

                            buckets.forEachIndexed { idx, b ->
                                val pVal = b.todaySales
                                val sVal = when (chartMode) {
                                    HourlyAreaChartMode.SALES_AND_PROFIT -> b.todayProfit
                                    HourlyAreaChartMode.TODAY_VS_YESTERDAY -> b.yesterdaySales
                                }
                                val pRatio = (pVal / maxHourlyVal).toFloat().coerceIn(0f, 1f)
                                val sRatio = (sVal / maxHourlyVal).toFloat().coerceIn(0f, 1f)

                                val cx = (idx + 0.5f) * slotW
                                val py = h - pRatio * (h - 12.dp.toPx()) - 4.dp.toPx()
                                val sy = h - sRatio * (h - 12.dp.toPx()) - 4.dp.toPx()

                                primaryPts.add(Offset(cx, py))
                                secondaryPts.add(Offset(cx, sy))
                            }

                            fun buildSmoothCurvePath(pts: List<Offset>): Path {
                                return Path().apply {
                                    if (pts.isEmpty()) return@apply
                                    moveTo(pts.first().x, pts.first().y)
                                    for (i in 0 until pts.size - 1) {
                                        val p0 = pts[i]
                                        val p1 = pts[i + 1]
                                        val ctrlX = (p0.x + p1.x) / 2f
                                        cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                    }
                                }
                            }

                            fun buildSmoothAreaPath(pts: List<Offset>): Path {
                                return Path().apply {
                                    if (pts.isEmpty()) return@apply
                                    moveTo(pts.first().x, h)
                                    lineTo(pts.first().x, pts.first().y)
                                    for (i in 0 until pts.size - 1) {
                                        val p0 = pts[i]
                                        val p1 = pts[i + 1]
                                        val ctrlX = (p0.x + p1.x) / 2f
                                        cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                    }
                                    lineTo(pts.last().x, h)
                                    close()
                                }
                            }

                            // Draw Secondary Area & Curve
                            if (secondaryPts.size >= 2) {
                                val secArea = buildSmoothAreaPath(secondaryPts)
                                drawPath(
                                    path = secArea,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            chartMode.secondaryColor.copy(alpha = 0.24f),
                                            chartMode.secondaryColor.copy(alpha = 0.02f)
                                        ),
                                        startY = 0f,
                                        endY = h
                                    )
                                )
                                drawPath(
                                    path = buildSmoothCurvePath(secondaryPts),
                                    color = chartMode.secondaryColor,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }

                            // Draw Primary Area & Curve
                            if (primaryPts.size >= 2) {
                                val priArea = buildSmoothAreaPath(primaryPts)
                                drawPath(
                                    path = priArea,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            chartMode.primaryColor.copy(alpha = 0.36f),
                                            chartMode.primaryColor.copy(alpha = 0.03f)
                                        ),
                                        startY = 0f,
                                        endY = h
                                    )
                                )
                                drawPath(
                                    path = buildSmoothCurvePath(primaryPts),
                                    color = chartMode.primaryColor,
                                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }

                            // Draw Recharts Crosshair Cursor & Active Dots
                            if (safeIndex in primaryPts.indices) {
                                val selX = primaryPts[safeIndex].x
                                drawLine(
                                    color = crosshairCol,
                                    start = Offset(selX, 0f),
                                    end = Offset(selX, h),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = gridDash
                                )
                            }

                            primaryPts.forEachIndexed { idx, pt ->
                                val isSel = idx == safeIndex
                                drawCircle(
                                    color = if (isSel) activeRingCol else chartMode.primaryColor,
                                    radius = if (isSel) 5.dp.toPx() else 2.8.dp.toPx(),
                                    center = pt
                                )
                                if (isSel) {
                                    drawCircle(
                                        color = chartMode.primaryColor,
                                        radius = 3.2.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // XAxis Hour Labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        buckets.forEachIndexed { idx, b ->
                            val isSelected = idx == safeIndex
                            Text(
                                text = b.shortLabelAr,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isSelected) chartMode.primaryColor else semantic.chartAxisLabel,
                                modifier = Modifier.clickable { onSelectIndex(idx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RechartsDailyComposedBarChartCard(
    buckets: List<SalesTimeBucket>,
    selectedPeriod: SalesReportPeriod,
    onPeriodChange: (SalesReportPeriod) -> Unit,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    currencySymbol: String
) {
    val semantic = LocalAFStoreSemanticColors.current
    val safeIndex = selectedIndex.coerceIn(0, (buckets.size - 1).coerceAtLeast(0))
    val selectedBucket = buckets.getOrNull(safeIndex)

    val maxBarVal = remember(buckets) {
        val raw = buckets.maxOfOrNull { maxOf(it.totalSales, it.netProfit, it.debtSales) } ?: 0.0
        if (raw <= 0.0) 100.0 else raw * 1.15
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (semantic.isDark) 0.dp else 2.dp
        ),
        border = BorderStroke(1.dp, semantic.cardBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recharts_daily_bar_chart_card")
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
                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "أعمدة المبيعات والأرباح اليومية (Recharts Bar)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "مقارنة إجمالي المبيعات بصافي الربح والديون يومياً",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(SalesReportPeriod.DAILY_7, SalesReportPeriod.DAILY_30).forEach { p ->
                        val isSel = selectedPeriod == p
                        FilterChip(
                            selected = isSel,
                            onClick = { onPeriodChange(p) },
                            label = {
                                Text(
                                    text = if (p == SalesReportPeriod.DAILY_7) "7 أيام" else "30 يوماً",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Selected Day Tooltip Banner
            if (selectedBucket != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semantic.tooltipBg,
                    shadowElevation = if (semantic.isDark) 0.dp else 1.5.dp,
                    border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.42f)),
                    modifier = Modifier.fillMaxWidth()
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
                                text = selectedBucket.fullTitle,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                color = semantic.tooltipTitle
                            )
                            Text(
                                text = "${selectedBucket.transactionsCount} عملية • الأعلى: ${selectedBucket.topProductName}",
                                fontSize = 10.sp,
                                color = semantic.tooltipSubtitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("المبيعات", fontSize = 9.5.sp, color = semantic.tooltipSubtitle)
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", selectedBucket.totalSales)} $currencySymbol",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = semantic.infoBlue
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("صافي الربح", fontSize = 9.5.sp, color = semantic.tooltipSubtitle)
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", selectedBucket.netProfit)} $currencySymbol",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = semantic.profitGreen
                                )
                            }
                        }
                    }
                }
            }

            // Recharts Bar + Line Canvas
            val barGridLineCol = semantic.chartGridLine
            val barSlotHighlightCol = if (semantic.isDark) {
                Color.White.copy(alpha = 0.06f)
            } else {
                semantic.infoBlue.copy(alpha = 0.08f)
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = semantic.chartCanvasBg,
                border = BorderStroke(1.dp, semantic.chartCanvasBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .pointerInput(buckets.size) {
                            detectTapGestures { offset ->
                                if (buckets.isNotEmpty() && size.width > 0) {
                                    val slotW = size.width.toFloat() / buckets.size
                                    val idx = (offset.x / slotW).toInt().coerceIn(0, buckets.lastIndex)
                                    onSelectIndex(idx)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val count = buckets.size.coerceAtLeast(1)
                        val slotW = w / count.toFloat()
                        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                        for (i in 0..3) {
                            val gy = h * (i / 3f)
                            drawLine(
                                color = barGridLineCol,
                                start = Offset(0f, gy),
                                end = Offset(w, gy),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (i < 3) dash else null
                            )
                        }

                        val trendPts = mutableListOf<Offset>()
                        buckets.forEachIndexed { idx, b ->
                            val sRatio = (b.totalSales / maxBarVal).toFloat().coerceIn(0f, 1f)
                            val pRatio = (b.netProfit / maxBarVal).toFloat().coerceIn(0f, 1f)

                            val cx = (idx + 0.5f) * slotW
                            val sy = h - sRatio * (h - 10.dp.toPx())
                            val py = h - pRatio * (h - 10.dp.toPx())
                            trendPts.add(Offset(cx, sy))

                            val isSel = idx == safeIndex
                            if (isSel) {
                                drawRoundRect(
                                    color = barSlotHighlightCol,
                                    topLeft = Offset(idx * slotW + 2f, 0f),
                                    size = Size((slotW - 4f).coerceAtLeast(4f), h),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }

                            val groupW = (slotW * 0.68f).coerceAtMost(28.dp.toPx())
                            val barW = (groupW * 0.45f).coerceAtLeast(2.5.dp.toPx())
                            val gap = (groupW * 0.08f).coerceAtLeast(1.dp.toPx())

                            val sh = (h - sy).coerceAtLeast(if (b.totalSales > 0) 3.dp.toPx() else 1.5.dp.toPx())
                            val ph = (h - py).coerceAtLeast(if (b.netProfit > 0) 3.dp.toPx() else 1.5.dp.toPx())

                            drawRoundRect(
                                color = if (isSel) Color(0xFF0EA5E9) else Color(0xFF0284C7).copy(alpha = 0.85f),
                                topLeft = Offset(cx - gap / 2f - barW, h - sh),
                                size = Size(barW, sh),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )

                            drawRoundRect(
                                color = if (isSel) Color(0xFF10B981) else Color(0xFF059669).copy(alpha = 0.85f),
                                topLeft = Offset(cx + gap / 2f, h - ph),
                                size = Size(barW, ph),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }

                        if (trendPts.size >= 2) {
                            val path = Path().apply {
                                moveTo(trendPts.first().x, trendPts.first().y)
                                for (i in 0 until trendPts.size - 1) {
                                    val p0 = trendPts[i]
                                    val p1 = trendPts[i + 1]
                                    val ctrlX = (p0.x + p1.x) / 2f
                                    cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFFF59E0B),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }

            // Day pills selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                buckets.forEachIndexed { idx, b ->
                    val isSelected = idx == safeIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) semantic.infoBlue.copy(alpha = 0.16f)
                        else semantic.cardSubtle,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) semantic.infoBlue else semantic.cardBorder
                        ),
                        modifier = Modifier.clickable { onSelectIndex(idx) }
                    ) {
                        Text(
                            text = b.shortLabel,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) semantic.infoBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RechartsPaymentDonutChartCard(
    liveSummary: DashboardLiveSummary,
    weeklyOverview: PeriodOverviewSummary,
    useWeeklyScope: Boolean,
    onToggleScope: (Boolean) -> Unit,
    currencySymbol: String
) {
    val semantic = LocalAFStoreSemanticColors.current
    val cashVal = if (useWeeklyScope) weeklyOverview.cashSales else liveSummary.todayCashSales
    val debtVal = if (useWeeklyScope) weeklyOverview.debtSales else liveSummary.todayDebtSales
    val transferVal = if (useWeeklyScope) weeklyOverview.transferSales else liveSummary.todayTransferSales

    val total = (cashVal + debtVal + transferVal).coerceAtLeast(0.0)
    val divisor = if (total > 0.0) total else 1.0

    var selectedSliceIndex by remember { mutableIntStateOf(0) }

    val slices = listOf(
        Triple("نقدي (كاش)", cashVal, semantic.profitGreen),
        Triple("آجل (ديون)", debtVal, semantic.debtAmber),
        Triple("تحويل بنكي", transferVal, semantic.cartonPurple)
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (semantic.isDark) 0.dp else 2.dp
        ),
        border = BorderStroke(1.dp, semantic.cardBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recharts_payment_donut_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        color = semantic.cartonPurple.copy(alpha = 0.14f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DonutLarge,
                                contentDescription = null,
                                tint = semantic.cartonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "دائرة طرق الدفع (Recharts Donut)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "توزيع السيولة المحصلة مقابل المبيعات الآجلة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !useWeeklyScope,
                        onClick = { onToggleScope(false) },
                        label = { Text("اليوم", fontSize = 10.5.sp) }
                    )
                    FilterChip(
                        selected = useWeeklyScope,
                        onClick = { onToggleScope(true) },
                        label = { Text("الأسبوع", fontSize = 10.5.sp) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val emptyArcColor = semantic.chartGridLine
                // Donut Canvas
                Box(
                    modifier = Modifier.size(128.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseStroke = 18.dp.toPx()
                        val selectedStroke = 23.dp.toPx()
                        val diameter = size.minDimension - selectedStroke
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                        if (total <= 0.0) {
                            drawArc(
                                color = emptyArcColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = baseStroke)
                            )
                        } else {
                            var startAng = -90f
                            slices.forEachIndexed { idx, (_, amt, col) ->
                                val sweep = ((amt / divisor) * 360.0).toFloat()
                                if (sweep > 0.5f) {
                                    val isSel = idx == selectedSliceIndex
                                    drawArc(
                                        color = col,
                                        startAngle = startAng,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(diameter, diameter),
                                        style = Stroke(
                                            width = if (isSel) selectedStroke else baseStroke,
                                            cap = StrokeCap.Butt
                                        )
                                    )
                                    startAng += sweep
                                }
                            }
                        }
                    }

                    val activeSlice = slices.getOrElse(selectedSliceIndex) { slices.first() }
                    val activePct = if (total > 0.0) (activeSlice.second / divisor) * 100.0 else 0.0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = activeSlice.first.substringBefore(" "),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.0f", activePct)}%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = activeSlice.third
                        )
                    }
                }

                // Interactive Breakdown Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    slices.forEachIndexed { idx, (label, amount, col) ->
                        val pct = if (total > 0.0) (amount / divisor) * 100.0 else 0.0
                        val isSel = idx == selectedSliceIndex
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) col.copy(alpha = 0.14f) else semantic.cardSubtle,
                            border = BorderStroke(
                                1.dp,
                                if (isSel) col else semantic.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSliceIndex = idx }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = "${String.format(Locale.US, "%.0f", amount)} $currencySymbol (${String.format(Locale.US, "%.0f", pct)}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = col
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
private fun DashboardLowStockAndRecentSalesCard(
    lowStockProducts: List<Pair<Product, Int>>,
    recentSales: List<Sale>,
    currencySymbol: String,
    onNavigateToTab: (MainAppTab) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (semantic.isDark) 0.dp else 2.dp
        ),
        border = BorderStroke(1.dp, semantic.cardBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Low-Stock Alerts Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "تنبيهات الأصناف الحرجة (${lowStockProducts.size})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    )
                }

                TextButton(
                    onClick = { onNavigateToTab(MainAppTab.REORDER_ALERTS) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("إدارة تنبيهات الطلب ◀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (lowStockProducts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "جميع الأصناف متوفرة بكميات آمنة فوق حد الطلب الأدنى ✅",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lowStockProducts.take(4).forEach { (prod, minThreshold) ->
                        val isZero = prod.stock <= 0
                        val badgeCol = if (isZero) Color(0xFFEF4444) else Color(0xFFF59E0B)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, badgeCol.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "حد التنبيه: $minThreshold وحدة • سعر البيع: ${String.format(Locale.US, "%.0f", prod.price)} $currencySymbol",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeCol.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = if (isZero) "نفد (0)" else "متبقي: ${prod.stock}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeCol,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Recent Live Sales Activity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر عمليات البيع المسجلة لحظياً",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp
                )
                TextButton(
                    onClick = { onNavigateToTab(MainAppTab.INVOICES) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("كل الفواتير ◀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (recentSales.isEmpty()) {
                Text(
                    text = "لا توجد مبيعات مسجلة حتى الآن.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val timeFmt = remember { SimpleDateFormat("HH:mm - MM/dd", Locale.US) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recentSales.forEach { sale ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${sale.productName} (${sale.quantitySold} وحدة)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sale.customerName.ifBlank { "عميل نقدي" }} • ${timeFmt.format(Date(sale.saleDate))}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", sale.totalPrice)} $currencySymbol",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = if (sale.isDebt) Color(0xFFF59E0B) else Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
