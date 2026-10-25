package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoice
import com.example.data.models.RecordSyncState
import com.example.data.models.Sale
import com.example.ui.theme.AFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.patrykandpatryk.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatryk.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatryk.vico.compose.chart.Chart
import com.patrykandpatryk.vico.compose.chart.column.columnChart
import com.patrykandpatryk.vico.compose.chart.line.lineChart
import com.patrykandpatryk.vico.compose.chart.line.lineSpec
import com.patrykandpatryk.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatryk.vico.core.entry.FloatEntry
import com.patrykandpatryk.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailySyncVolumePoint(
    val dayIndex: Int,
    val dayLabel: String,
    val dateMillis: Long,
    val syncedCount: Int,
    val pendingCount: Int
)

enum class SyncChartMode(val labelAr: String) {
    LINE("منحنى (Line)"),
    BAR("أعمدة (Bar)")
}

/**
 * بطاقة رسم بياني تفاعلي باستخدام مكتبة Vico توضح:
 * - الحجم اليومي للسجلات المتزامنة سحابياً (Synchronized Records Volume).
 * - عدد العمليات المحلية المعلقة قيد الانتظار (Pending Operations Volume).
 * - مقارنة إحصائية ونسبة إنجاز المزامنة لكل فترة زمنية.
 */
@Composable
fun DailySyncVolumeVicoChartCard(
    viewModel: AntarSalesViewModel,
    semanticColors: AFStoreSemanticColors,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingSyncQueueCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.offlineSyncManager.isSyncing.collectAsStateWithLifecycle()

    var selectedDays by remember { mutableIntStateOf(7) }
    var chartMode by remember { mutableStateOf(SyncChartMode.LINE) }

    // حساب حجم المزامنة اليومي
    val dataPoints = remember(sales, products, purchaseInvoices, pendingCount, selectedDays) {
        calculateDailySyncVolume(
            sales = sales,
            products = products,
            purchases = purchaseInvoices,
            currentPending = pendingCount,
            days = selectedDays
        )
    }

    val totalSynced = remember(dataPoints) { dataPoints.sumOf { it.syncedCount } }
    val totalPending = remember(dataPoints) { dataPoints.sumOf { it.pendingCount } }
    val totalOperations = totalSynced + totalPending
    val syncRatio = remember(totalSynced, totalOperations) {
        if (totalOperations > 0) (totalSynced.toDouble() / totalOperations * 100.0).toInt() else 100
    }

    // بناء نموذج بيانات الرسم البياني لمكتبة Vico
    val chartModel = remember(dataPoints) {
        val syncedEntries = dataPoints.map {
            FloatEntry(x = it.dayIndex.toFloat(), y = it.syncedCount.toFloat())
        }
        val pendingEntries = dataPoints.map {
            FloatEntry(x = it.dayIndex.toFloat(), y = it.pendingCount.toFloat())
        }
        entryModelOf(syncedEntries, pendingEntries)
    }

    val bottomAxisValueFormatter = remember(dataPoints) {
        AxisValueFormatter<com.patrykandpatryk.vico.core.axis.AxisPosition.Horizontal.Bottom> { value, _ ->
            val idx = value.toInt()
            dataPoints.getOrNull(idx)?.dayLabel ?: ""
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.2.dp, semanticColors.headerBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_sync_volume_vico_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "حجم المزامنة اليومي والعمليات المعلقة",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مقارنة السجلات المرفوعة سحابياً مقابل العمليات قيد الانتظار",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // نسبة الاكتمال
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (syncRatio >= 95) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (syncRatio >= 95) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (syncRatio >= 95) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (syncRatio >= 95) Color(0xFF059669) else Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$syncRatio% مزامن",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (syncRatio >= 95) Color(0xFF047857) else Color(0xFFB45309)
                        )
                    }
                }
            }

            // Quick Stats Row: Synced vs Pending
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // البطاقة الخضراء: السجلات المتزامنة
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Column {
                            Text(
                                text = "مكتملة بالسحابة",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalSynced سجل",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                // البطاقة البرتقالية: العمليات المعلقة
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFF59E0B), CircleShape)
                        )
                        Column {
                            Text(
                                text = "معلقة محلياً",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalPending عملية",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            // Controls Bar: Time Filter Chips & Chart Type Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Range Selector: 7 vs 14 vs 30 days
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(7 to "7 أيام", 14 to "14 يوماً", 30 to "شهر").forEach { (d, label) ->
                        val isSelected = selectedDays == d
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clickable { selectedDays = d }
                                .testTag("sync_chart_days_${d}")
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Chart Style Switch: Line vs Bar
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (chartMode == SyncChartMode.LINE) MaterialTheme.colorScheme.surface else Color.Transparent,
                        border = if (chartMode == SyncChartMode.LINE) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                        modifier = Modifier.clickable { chartMode = SyncChartMode.LINE }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "منحنى",
                            tint = if (chartMode == SyncChartMode.LINE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (chartMode == SyncChartMode.BAR) MaterialTheme.colorScheme.surface else Color.Transparent,
                        border = if (chartMode == SyncChartMode.BAR) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                        modifier = Modifier.clickable { chartMode = SyncChartMode.BAR }
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "أعمدة",
                            tint = if (chartMode == SyncChartMode.BAR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                }
            }

            // Vico Chart Visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                val lineChartInstance = lineChart(
                    lines = listOf(
                        lineSpec(lineColor = Color(0xFF10B981)), // متزامن (أخضر زمردي)
                        lineSpec(lineColor = Color(0xFFF59E0B))  // معلق (برتقالي دافئ)
                    )
                )
                val columnChartInstance = columnChart()

                Chart(
                    chart = if (chartMode == SyncChartMode.LINE) lineChartInstance else columnChartInstance,
                    model = chartModel,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(
                        valueFormatter = bottomAxisValueFormatter
                    ),
                    modifier = Modifier.fillMaxWidth().height(170.dp)
                )
            }

            // Visual Chart Legend & Guide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Text(
                        text = "السجلات المتزامنة سحابياً (Synced)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFF59E0B), CircleShape)
                    )
                    Text(
                        text = "العمليات المعلقة (Pending)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * دالة رياضية تجميعية لحساب حجم السجلات المتزامنة مقابل العمليات المعلقة لكل يوم.
 */
private fun calculateDailySyncVolume(
    sales: List<Sale>,
    products: List<Product>,
    purchases: List<PurchaseInvoice>,
    currentPending: Int,
    days: Int
): List<DailySyncVolumePoint> {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("d/M", Locale.getDefault())
    val results = mutableListOf<DailySyncVolumePoint>()

    val allSalesSynced = sales.count { it.syncStatus == RecordSyncState.SYNCED }
    val allProductsSynced = products.count { it.syncStatus == RecordSyncState.SYNCED }

    for (offset in (days - 1) downTo 0) {
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -offset)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis
        val dayEnd = dayStart + 24 * 60 * 60 * 1000L

        val dayIndex = (days - 1) - offset
        val label = when (offset) {
            0 -> "اليوم"
            1 -> "أمس"
            else -> dateFormat.format(Date(dayStart))
        }

        var synced = 0
        var pending = 0

        // مبيعات هذا اليوم
        for (s in sales) {
            val t = if (s.updatedAt > 0L) s.updatedAt else s.saleDate
            if (t in dayStart until dayEnd) {
                if (s.syncStatus == RecordSyncState.SYNCED) synced++
                else pending++
            }
        }

        // سلع تم تحديثها في هذا اليوم
        for (p in products) {
            if (p.updatedAt in dayStart until dayEnd) {
                if (p.syncStatus == RecordSyncState.SYNCED) synced++
                else pending++
            }
        }

        // مشتريات هذا اليوم
        for (pur in purchases) {
            val t = if (pur.updatedAt > 0L) pur.updatedAt else pur.dateMillis
            if (t in dayStart until dayEnd) {
                if (pur.syncStatus == RecordSyncState.SYNCED) synced++
                else pending++
            }
        }

        // في يوم اليوم: التأكد من مطابقة العمليات المعلقة للرقم اللحظي لطابور المزامنة
        if (offset == 0) {
            pending = maxOf(pending, currentPending)
            // إذا لم يتم تسجيل عمليات اليوم ولكن توجد بيانات متزامنة مسبقاً، نظهر الرصيد المكتمل
            if (synced == 0 && (allSalesSynced > 0 || allProductsSynced > 0)) {
                synced = (allSalesSynced + allProductsSynced).coerceAtLeast(1)
            }
        }

        results.add(
            DailySyncVolumePoint(
                dayIndex = dayIndex,
                dayLabel = label,
                dateMillis = dayStart,
                syncedCount = synced,
                pendingCount = pending
            )
        )
    }

    return results
}
