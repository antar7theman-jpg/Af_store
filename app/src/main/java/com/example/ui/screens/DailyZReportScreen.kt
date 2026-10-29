package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CustomerTransaction
import com.example.data.models.DamagedItemRecord
import com.example.data.models.Expense
import com.example.data.models.ExpenseCategory
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoice
import com.example.data.models.Sale
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class DailyZReportData(
    val dayKey: String,
    val dayDisplayAr: String,
    val startMillis: Long,
    val endMillis: Long,
    val openingCashBalance: Double,
    val actualCountedCash: Double?,
    val totalSalesAmount: Double,
    val cashSalesAmount: Double,
    val debtSalesAmount: Double,
    val cardOrTransferSalesAmount: Double,
    val invoicesCount: Int,
    val itemsSoldCount: Int,
    val estimatedCostOfGoodsSold: Double,
    val grossProfitFromSales: Double,
    val pendingReturnRequestsCount: Int,
    val pendingReturnRequestsAmount: Double,
    val collectedDebtsList: List<CustomerTransaction>,
    val collectedDebtsTotal: Double,
    val newDebtsTransactionsTotal: Double,
    val dailyExpensesList: List<Expense>,
    val totalExpensesAmount: Double,
    val cashExpensesAmount: Double,
    val nonCashExpensesAmount: Double,
    val supplierCashPaidToday: Double,
    val damagedLossToday: Double,
    val expectedCashInDrawer: Double,
    val expectedCashAfterSuppliers: Double,
    val cashVariance: Double?,
    val netDailyProfit: Double
)

fun getDayBoundsForTimestamp(timestampMillis: Long): Triple<String, Long, Long> {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestampMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startMillis = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    val endMillis = cal.timeInMillis
    val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startMillis))
    return Triple(dayKey, startMillis, endMillis)
}

fun computeDailyZReportData(
    targetTimestampMillis: Long,
    sales: List<Sale>,
    customerTransactions: List<CustomerTransaction>,
    expenses: List<Expense>,
    purchaseInvoices: List<PurchaseInvoice>,
    damagedItems: List<DamagedItemRecord>,
    products: List<Product>,
    openingCashBalance: Double,
    actualCountedCash: Double?
): DailyZReportData {
    val (dayKey, startMillis, endMillis) = getDayBoundsForTimestamp(targetTimestampMillis)
    val productMap = products.associateBy { it.id }

    val daySalesAll = sales.filter { it.saleDate in startMillis..endMillis }

    val activeDaySales = daySalesAll.filter { sale ->
        sale.status != "cancelled" &&
            sale.status != "refunded" &&
            !(sale.orderType == "pre_sale" && sale.status == "pending")
    }

    val pendingReturnGroups = activeDaySales
        .filter { it.status == "return_requested" }
        .groupBy { it.invoiceGroupId.ifBlank { "SALE-${it.saleId}" } }

    val pendingReturnRequestsCount = pendingReturnGroups.size
    val pendingReturnRequestsAmount = pendingReturnGroups.values.sumOf { group ->
        group.sumOf { it.totalPrice }
    }

    val totalSalesAmount = activeDaySales.sumOf { it.totalPrice }

    val cashSalesAmount = activeDaySales.filter { sale ->
        !sale.isDebt &&
            !sale.paymentMethod.equals("CARD", ignoreCase = true) &&
            !sale.paymentMethod.equals("TRANSFER", ignoreCase = true) &&
            !sale.paymentMethod.equals("BANK_TRANSFER", ignoreCase = true)
    }.sumOf { it.totalPrice }

    val cardOrTransferSalesAmount = activeDaySales.filter { sale ->
        !sale.isDebt && (
            sale.paymentMethod.equals("CARD", ignoreCase = true) ||
                sale.paymentMethod.equals("TRANSFER", ignoreCase = true) ||
                sale.paymentMethod.equals("BANK_TRANSFER", ignoreCase = true)
            )
    }.sumOf { it.totalPrice }

    val debtSalesAmount = activeDaySales.filter { it.isDebt }.sumOf { it.totalPrice }

    val invoicesCount = activeDaySales
        .groupBy { it.invoiceGroupId.ifBlank { "SALE-${it.saleId}" } }
        .size

    val itemsSoldCount = activeDaySales.sumOf { it.quantitySold }

    val estimatedCostOfGoodsSold = activeDaySales.sumOf { sale ->
        val prod = productMap[sale.productId]
        val unitCost = prod?.purchasePrice?.coerceAtLeast(0.0) ?: (
            if (sale.quantitySold > 0) (sale.totalPrice / sale.quantitySold) * 0.78 else 0.0
            )
        unitCost * sale.quantitySold
    }

    val grossProfitFromSales = totalSalesAmount - estimatedCostOfGoodsSold

    val dayCustomerTxs = customerTransactions.filter { it.date in startMillis..endMillis }
    val collectedDebtsList = dayCustomerTxs
        .filter { it.type.equals("PAYMENT", ignoreCase = true) }
        .sortedByDescending { it.date }
    val collectedDebtsTotal = collectedDebtsList.sumOf { it.amount }
    val newDebtsTransactionsTotal = dayCustomerTxs
        .filter { !it.type.equals("PAYMENT", ignoreCase = true) }
        .sumOf { it.amount }

    val dailyExpensesList = expenses
        .filter { it.dateMillis in startMillis..endMillis }
        .sortedByDescending { it.dateMillis }
    val totalExpensesAmount = dailyExpensesList.sumOf { it.amount }
    val cashExpensesAmount = dailyExpensesList
        .filter { it.paymentMethod.equals("CASH", ignoreCase = true) || it.paymentMethod.isBlank() }
        .sumOf { it.amount }
    val nonCashExpensesAmount = (totalExpensesAmount - cashExpensesAmount).coerceAtLeast(0.0)

    val supplierCashPaidToday = purchaseInvoices
        .filter { it.dateMillis in startMillis..endMillis }
        .sumOf { it.paidAmount }

    val damagedLossToday = damagedItems
        .filter { it.dateMillis in startMillis..endMillis }
        .sumOf { it.totalLoss }

    val expectedCashInDrawer = openingCashBalance + cashSalesAmount + collectedDebtsTotal - cashExpensesAmount
    val expectedCashAfterSuppliers = expectedCashInDrawer - supplierCashPaidToday
    val cashVariance = actualCountedCash?.let { counted -> counted - expectedCashInDrawer }
    val netDailyProfit = grossProfitFromSales - totalExpensesAmount - damagedLossToday

    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val yesterdayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 86_400_000L))
    val dayDisplayAr = when (dayKey) {
        todayKey -> "اليوم ($dayKey)"
        yesterdayKey -> "أمس ($dayKey)"
        else -> "تاريخ $dayKey"
    }

    return DailyZReportData(
        dayKey = dayKey,
        dayDisplayAr = dayDisplayAr,
        startMillis = startMillis,
        endMillis = endMillis,
        openingCashBalance = openingCashBalance,
        actualCountedCash = actualCountedCash,
        totalSalesAmount = totalSalesAmount,
        cashSalesAmount = cashSalesAmount,
        debtSalesAmount = debtSalesAmount,
        cardOrTransferSalesAmount = cardOrTransferSalesAmount,
        invoicesCount = invoicesCount,
        itemsSoldCount = itemsSoldCount,
        estimatedCostOfGoodsSold = estimatedCostOfGoodsSold,
        grossProfitFromSales = grossProfitFromSales,
        pendingReturnRequestsCount = pendingReturnRequestsCount,
        pendingReturnRequestsAmount = pendingReturnRequestsAmount,
        collectedDebtsList = collectedDebtsList,
        collectedDebtsTotal = collectedDebtsTotal,
        newDebtsTransactionsTotal = newDebtsTransactionsTotal,
        dailyExpensesList = dailyExpensesList,
        totalExpensesAmount = totalExpensesAmount,
        cashExpensesAmount = cashExpensesAmount,
        nonCashExpensesAmount = nonCashExpensesAmount,
        supplierCashPaidToday = supplierCashPaidToday,
        damagedLossToday = damagedLossToday,
        expectedCashInDrawer = expectedCashInDrawer,
        expectedCashAfterSuppliers = expectedCashAfterSuppliers,
        cashVariance = cashVariance,
        netDailyProfit = netDailyProfit
    )
}

fun buildFormattedZReportWhatsAppMessage(
    report: DailyZReportData,
    storeName: String,
    currencySymbol: String,
    preparedBy: String,
    shiftNotes: String = "",
    isClosed: Boolean = false
): String {
    fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
    val nowTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
    val sb = StringBuilder()

    sb.appendLine("📊 *ملخص الإغلاق اليومي الذكي (Z-Report)*")
    sb.appendLine("🏪 *المتجر:* $storeName")
    sb.appendLine("📅 *تاريخ التقرير:* ${report.dayKey}")
    sb.appendLine("⏰ *وقت الإصدار:* $nowTimeStr")
    sb.appendLine("👤 *المسؤول / الكاشير:* $preparedBy")
    sb.appendLine("📌 *حالة الصندوق:* ${if (isClosed) "مغلق ومعتمد ✅" else "تقرير مباشر قيد المراجعة ⏳"}")
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

    sb.appendLine("💰 *أولاً: إجمالي النقد في الدرج (صندوق الكاشير)*")
    sb.appendLine("• رصيد افتتاح الدرج: ${fmt(report.openingCashBalance)} $currencySymbol")
    sb.appendLine("• (+) المبيعات النقدية (كاش): ${fmt(report.cashSalesAmount)} $currencySymbol")
    sb.appendLine("• (+) الديون المحصلة اليوم: ${fmt(report.collectedDebtsTotal)} $currencySymbol")
    sb.appendLine("• (-) المصروفات النقدية المسددة: ${fmt(report.cashExpensesAmount)} $currencySymbol")
    sb.appendLine("💵 *إجمالي النقد الصافي في الدرج:* *${fmt(report.expectedCashInDrawer)} $currencySymbol*")

    if (report.actualCountedCash != null) {
        val diff = report.cashVariance ?: 0.0
        val statusStr = when {
            abs(diff) < 0.01 -> "متطابق تماماً ✅"
            diff > 0 -> "فائض نقدي (+${fmt(diff)} $currencySymbol) 🟢"
            else -> "عجز في الدرج (${fmt(diff)} $currencySymbol) 🔴"
        }
        sb.appendLine("• الجرد الفعلي للدرج: ${fmt(report.actualCountedCash)} $currencySymbol ($statusStr)")
    }
    if (report.supplierCashPaidToday > 0.0) {
        sb.appendLine("• مدفوعات الموردين اليوم: ${fmt(report.supplierCashPaidToday)} $currencySymbol")
        sb.appendLine("• النقد بعد خصم دفعات الموردين: ${fmt(report.expectedCashAfterSuppliers)} $currencySymbol")
    }
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

    sb.appendLine("🛒 *ثانياً: ملخص المبيعات اليومية*")
    sb.appendLine("• إجمالي المبيعات الكلي: *${fmt(report.totalSalesAmount)} $currencySymbol*")
    sb.appendLine("• عدد الفواتير المصدرة: ${report.invoicesCount} فاتورة (${report.itemsSoldCount} قطعة)")
    sb.appendLine("• مبيعات نقدية (كاش): ${fmt(report.cashSalesAmount)} $currencySymbol")
    sb.appendLine("• مبيعات آجلة (ديون جديدة): ${fmt(report.debtSalesAmount)} $currencySymbol")
    if (report.cardOrTransferSalesAmount > 0.0) {
        sb.appendLine("• مبيعات بطاقة / تحويل: ${fmt(report.cardOrTransferSalesAmount)} $currencySymbol")
    }
    if (report.pendingReturnRequestsCount > 0) {
        sb.appendLine("• ⚠️ طلبات مرتجع بانتظار الاعتماد: ${report.pendingReturnRequestsCount} فاتورة (${fmt(report.pendingReturnRequestsAmount)} $currencySymbol)")
    }
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

    sb.appendLine("🤝 *ثالثاً: الديون المحصلة من العملاء اليوم*")
    sb.appendLine("• إجمالي الديون المحصلة: *${fmt(report.collectedDebtsTotal)} $currencySymbol*")
    sb.appendLine("• عدد عمليات التحصيل: ${report.collectedDebtsList.size} عملية")
    if (report.collectedDebtsList.isNotEmpty()) {
        report.collectedDebtsList.take(5).forEach { tx ->
            val tTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(tx.date))
            sb.appendLine("  ◦ ${tx.customerName}: ${fmt(tx.amount)} $currencySymbol ($tTime)")
        }
        if (report.collectedDebtsList.size > 5) {
            sb.appendLine("  ◦ ... و ${report.collectedDebtsList.size - 5} عمليات تحصيل أخرى")
        }
    }
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

    sb.appendLine("💸 *رابعاً: المصروفات والنفقات التشغيلية*")
    sb.appendLine("• إجمالي مصروفات اليوم: *${fmt(report.totalExpensesAmount)} $currencySymbol*")
    sb.appendLine("• عدد سندات الصرف: ${report.dailyExpensesList.size} سند")
    if (report.dailyExpensesList.isNotEmpty()) {
        report.dailyExpensesList.take(5).forEach { exp ->
            sb.appendLine("  ◦ ${exp.title} (${exp.category.labelAr}): ${fmt(exp.amount)} $currencySymbol")
        }
        if (report.dailyExpensesList.size > 5) {
            sb.appendLine("  ◦ ... و ${report.dailyExpensesList.size - 5} مصروفات أخرى")
        }
    }
    if (report.damagedLossToday > 0.0) {
        sb.appendLine("• خسائر التوالف والهالك اليوم: ${fmt(report.damagedLossToday)} $currencySymbol")
    }
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

    sb.appendLine("📈 *خامساً: الخلاصة والأرباح اليومية*")
    sb.appendLine("• مجمل ربح المبيعات: ${fmt(report.grossProfitFromSales)} $currencySymbol")
    sb.appendLine("• صافي ربح اليوم (بعد المصروفات والتوالف): *${fmt(report.netDailyProfit)} $currencySymbol*")

    if (shiftNotes.isNotBlank()) {
        sb.appendLine("📝 *ملاحظات الإغلاق:* $shiftNotes")
    }
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
    sb.append("✅ تم التوليد آلياً عبر نظام *AF store* الذكي")

    return sb.toString()
}

@Composable
fun DailyZReportQuickSummaryCard(
    viewModel: AntarSalesViewModel,
    onOpenFullZReportScreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val semantic = LocalAFStoreSemanticColors.current

    val sales by viewModel.sales.collectAsState()
    val customerTransactions by viewModel.customerTransactions.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val purchases by viewModel.purchaseInvoices.collectAsState()
    val damagedItems by viewModel.damagedItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val activeStaff by viewModel.activeStaffAccountState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val zReportVersion by viewModel.zReportStateVersion.collectAsState()

    val todayKey = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val openingCash = remember(todayKey, zReportVersion) {
        viewModel.getOpeningCashForDay(todayKey)
    }
    val countedCash = remember(todayKey, zReportVersion) {
        viewModel.getActualCountedCashForDay(todayKey)
    }
    val shiftNotes = remember(todayKey, zReportVersion) {
        viewModel.getZReportNotesForDay(todayKey)
    }
    val isShiftClosed = remember(todayKey, zReportVersion) {
        viewModel.isDayZReportClosed(todayKey)
    }

    val reportData = remember(
        sales,
        customerTransactions,
        expenses,
        purchases,
        damagedItems,
        products,
        openingCash,
        countedCash
    ) {
        computeDailyZReportData(
            targetTimestampMillis = System.currentTimeMillis(),
            sales = sales,
            customerTransactions = customerTransactions,
            expenses = expenses,
            purchaseInvoices = purchases,
            damagedItems = damagedItems,
            products = products,
            openingCashBalance = openingCash,
            actualCountedCash = countedCash
        )
    }

    val preparedByName = activeStaff?.fullName?.takeIf { it.isNotBlank() }
        ?: currentUser?.fullName?.takeIf { it.isNotBlank() }
        ?: "إدارة المتجر"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.surface),
        border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_z_report_quick_card")
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
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "📊 ملخص الإغلاق اليومي الذكي (Z-Report)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تقرير إغلاق الصندوق اليومي (${reportData.dayKey}): النقد في الدرج، المبيعات، الديون المحصلة، والمصروفات",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onOpenFullZReportScreen != null) {
                    OutlinedButton(
                        onClick = onOpenFullZReportScreen,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("open_full_z_report_btn")
                    ) {
                        Text("التفاصيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZReportMiniMetricTile(
                    title = "النقد في الدرج",
                    amount = reportData.expectedCashInDrawer,
                    currencySymbol = currencySymbol,
                    accentColor = Color(0xFF10B981),
                    subtitle = "كاش صافي",
                    modifier = Modifier.weight(1f)
                )
                ZReportMiniMetricTile(
                    title = "إجمالي المبيعات",
                    amount = reportData.totalSalesAmount,
                    currencySymbol = currencySymbol,
                    accentColor = Color(0xFF3B82F6),
                    subtitle = "${reportData.invoicesCount} فاتورة",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZReportMiniMetricTile(
                    title = "الديون المحصلة",
                    amount = reportData.collectedDebtsTotal,
                    currencySymbol = currencySymbol,
                    accentColor = Color(0xFF8B5CF6),
                    subtitle = "${reportData.collectedDebtsList.size} عملية سداد",
                    modifier = Modifier.weight(1f)
                )
                ZReportMiniMetricTile(
                    title = "المصروفات اليومية",
                    amount = reportData.totalExpensesAmount,
                    currencySymbol = currencySymbol,
                    accentColor = Color(0xFFEF4444),
                    subtitle = "${reportData.dailyExpensesList.size} سند صرف",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val message = buildFormattedZReportWhatsAppMessage(
                            report = reportData,
                            storeName = storeName,
                            currencySymbol = currencySymbol,
                            preparedBy = preparedByName,
                            shiftNotes = shiftNotes,
                            isClosed = isShiftClosed
                        )
                        shareTextViaWhatsApp(
                            context = context,
                            phone = "",
                            text = message
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("quick_z_report_whatsapp_owner_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال عبر واتساب",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إرسال عبر واتساب",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        DailyZReportPdfExporter.shareZReportPdf(
                            context = context,
                            report = reportData,
                            storeName = storeName,
                            currencySymbol = currencySymbol,
                            preparedBy = preparedByName,
                            shiftNotes = shiftNotes,
                            isClosed = isShiftClosed,
                            shiftClosedTime = viewModel.getDayZReportClosedTime(todayKey),
                            preferWhatsApp = false
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_z_report_send_pdf_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "إرسال التقرير بصيغة PDF",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إرسال بصيغة PDF",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ZReportMiniMetricTile(
    title: String,
    amount: Double,
    currencySymbol: String,
    accentColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${String.format(Locale.US, "%.2f", amount)} $currencySymbol",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyZReportScreen(
    viewModel: AntarSalesViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToExpenses: (() -> Unit)? = null,
    onNavigateToDebts: (() -> Unit)? = null,
    onNavigateToInvoices: (() -> Unit)? = null
) {
    if (onNavigateBack != null) {
        BackHandler { onNavigateBack() }
    }

    val context = LocalContext.current
    val semantic = LocalAFStoreSemanticColors.current

    val sales by viewModel.sales.collectAsState()
    val customerTransactions by viewModel.customerTransactions.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val purchases by viewModel.purchaseInvoices.collectAsState()
    val damagedItems by viewModel.damagedItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val activeStaff by viewModel.activeStaffAccountState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val zReportVersion by viewModel.zReportStateVersion.collectAsState()

    var selectedDayMillis by rememberSaveable {
        mutableLongStateOf(System.currentTimeMillis())
    }

    val (dayKey, _, _) = remember(selectedDayMillis) {
        getDayBoundsForTimestamp(selectedDayMillis)
    }

    val openingCash = remember(dayKey, zReportVersion) {
        viewModel.getOpeningCashForDay(dayKey)
    }
    val countedCash = remember(dayKey, zReportVersion) {
        viewModel.getActualCountedCashForDay(dayKey)
    }
    val shiftNotes = remember(dayKey, zReportVersion) {
        viewModel.getZReportNotesForDay(dayKey)
    }
    val isShiftClosed = remember(dayKey, zReportVersion) {
        viewModel.isDayZReportClosed(dayKey)
    }
    val shiftClosedTime = remember(dayKey, zReportVersion) {
        viewModel.getDayZReportClosedTime(dayKey)
    }

    val reportData = remember(
        selectedDayMillis,
        sales,
        customerTransactions,
        expenses,
        purchases,
        damagedItems,
        products,
        openingCash,
        countedCash
    ) {
        computeDailyZReportData(
            targetTimestampMillis = selectedDayMillis,
            sales = sales,
            customerTransactions = customerTransactions,
            expenses = expenses,
            purchaseInvoices = purchases,
            damagedItems = damagedItems,
            products = products,
            openingCashBalance = openingCash,
            actualCountedCash = countedCash
        )
    }

    val preparedByName = activeStaff?.fullName?.takeIf { it.isNotBlank() }
        ?: currentUser?.fullName?.takeIf { it.isNotBlank() }
        ?: "إدارة المتجر"

    var showDrawerConfigDialog by remember { mutableStateOf(false) }
    var showWhatsAppPreviewDialog by remember { mutableStateOf(false) }
    var showQuickExpenseDialog by remember { mutableStateOf(false) }

    val saveZReportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        if (targetUri != null) {
            val savedOk = DailyZReportPdfExporter.saveZReportPdfToUri(
                context = context,
                targetUri = targetUri,
                report = reportData,
                storeName = storeName,
                currencySymbol = currencySymbol,
                preparedBy = preparedByName,
                shiftNotes = shiftNotes,
                isClosed = isShiftClosed,
                shiftClosedTime = shiftClosedTime
            )
            Toast.makeText(
                context,
                if (savedOk) "تم حفظ تقرير Z-Report بصيغة PDF بنجاح في الهاتف 📄" else "تعذر حفظ ملف PDF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val formattedWhatsAppMessage = remember(
        reportData,
        storeName,
        currencySymbol,
        preparedByName,
        shiftNotes,
        isShiftClosed
    ) {
        buildFormattedZReportWhatsAppMessage(
            report = reportData,
            storeName = storeName,
            currencySymbol = currencySymbol,
            preparedBy = preparedByName,
            shiftNotes = shiftNotes,
            isClosed = isShiftClosed
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("daily_z_report_screen_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 44.dp)
    ) {
        item {
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
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("z_report_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "📊 ملخص الإغلاق اليومي الذكي (Z-Report)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تقرير إغلاق الصندوق اليومي: إجمالي النقد في الدرج، المبيعات، الديون المحصلة، والمصروفات",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isShiftClosed) Color(0xFF10B981).copy(alpha = 0.14f) else Color(0xFFF59E0B).copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        if (isShiftClosed) Color(0xFF10B981).copy(alpha = 0.55f) else Color(0xFFF59E0B).copy(alpha = 0.55f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isShiftClosed) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isShiftClosed) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isShiftClosed) "مغلق ✓" else "صندوق مفتوح",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isShiftClosed) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.surface),
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { selectedDayMillis -= 86_400_000L },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("z_report_prev_day_btn")
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "اليوم السابق", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("اليوم السابق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = reportData.dayDisplayAr,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val isToday = reportData.dayKey == SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        OutlinedButton(
                            onClick = {
                                if (!isToday) {
                                    selectedDayMillis = (selectedDayMillis + 86_400_000L).coerceAtMost(System.currentTimeMillis())
                                }
                            },
                            enabled = !isToday,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("z_report_next_day_btn")
                        ) {
                            Text("اليوم التالي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.ChevronLeft, contentDescription = "اليوم التالي", modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        val yesterdayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 86_400_000L))
                        val isTodaySelected = reportData.dayKey == todayKey
                        val isYesterdaySelected = reportData.dayKey == yesterdayKey

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTodaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedDayMillis = System.currentTimeMillis() }
                                .testTag("z_report_today_chip")
                        ) {
                            Text(
                                text = "تقارير اليوم ($todayKey)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTodaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isYesterdaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedDayMillis = System.currentTimeMillis() - 86_400_000L }
                                .testTag("z_report_yesterday_chip")
                        ) {
                            Text(
                                text = "إغلاق أمس ($yesterdayKey)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isYesterdaySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("z_report_cash_drawer_hero_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF065F46).copy(alpha = if (semantic.isDark) 0.42f else 0.12f),
                                    semantic.surface
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "إجمالي النقد الصافي في الدرج (Z-Report)",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "الافتتاح + مبيعات الكاش + الديون المحصلة - المصروفات النقدية",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showDrawerConfigDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                modifier = Modifier.testTag("z_report_edit_drawer_btn")
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جرد الدرج", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "الرصيد النقدي المفترض في الصندوق الآن",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", reportData.expectedCashInDrawer)} $currencySymbol",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.testTag("z_report_expected_cash_value")
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ZReportFormulaRow(
                                label = "رصيد افتتاح الدرج (العهدة الصباحية)",
                                amount = reportData.openingCashBalance,
                                currencySymbol = currencySymbol,
                                signPrefix = "",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            ZReportFormulaRow(
                                label = "المبيعات النقدية المباشرة (كاش)",
                                amount = reportData.cashSalesAmount,
                                currencySymbol = currencySymbol,
                                signPrefix = "+ ",
                                color = Color(0xFF10B981)
                            )
                            ZReportFormulaRow(
                                label = "الديون المحصلة نقداً من العملاء اليوم",
                                amount = reportData.collectedDebtsTotal,
                                currencySymbol = currencySymbol,
                                signPrefix = "+ ",
                                color = Color(0xFF8B5CF6)
                            )
                            ZReportFormulaRow(
                                label = "المصروفات النقدية المسددة من الدرج",
                                amount = reportData.cashExpensesAmount,
                                currencySymbol = currencySymbol,
                                signPrefix = "- ",
                                color = Color(0xFFEF4444)
                            )
                            if (reportData.supplierCashPaidToday > 0.0) {
                                HorizontalDivider(color = semantic.divider)
                                ZReportFormulaRow(
                                    label = "دفعات الموردين المسددة اليوم",
                                    amount = reportData.supplierCashPaidToday,
                                    currencySymbol = currencySymbol,
                                    signPrefix = "- ",
                                    color = Color(0xFFF59E0B)
                                )
                                ZReportFormulaRow(
                                    label = "النقد بعد خصم دفعات الموردين",
                                    amount = reportData.expectedCashAfterSuppliers,
                                    currencySymbol = currencySymbol,
                                    signPrefix = "= ",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (reportData.actualCountedCash != null) {
                            val variance = reportData.cashVariance ?: 0.0
                            val isExact = abs(variance) < 0.01
                            val isSurplus = variance > 0.0
                            val badgeColor = when {
                                isExact -> Color(0xFF10B981)
                                isSurplus -> Color(0xFF3B82F6)
                                else -> Color(0xFFEF4444)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "الجرد الفعلي للدرج: ${String.format(Locale.US, "%.2f", reportData.actualCountedCash)} $currencySymbol",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = when {
                                                isExact -> "صندوق النقد متطابق تماماً بدون أي فروقات ✅"
                                                isSurplus -> "يوجد فائض نقدي في الدرج بمقدار +${String.format(Locale.US, "%.2f", variance)} $currencySymbol"
                                                else -> "يوجد عجز نقدي في الدرج بمقدار ${String.format(Locale.US, "%.2f", variance)} $currencySymbol"
                                            },
                                            fontSize = 11.sp,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExact || isSurplus) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.surface),
                border = BorderStroke(1.5.dp, Color(0xFF16A34A).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "📲 إرسال تقرير Z-Report عبر واتساب أو بصيغة PDF",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = "مشاركة ملخص الإغلاق اليومي كاملاً برسالة منسقة أو كملف PDF رسمي عبر واتساب وبقية التطبيقات",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            shareTextViaWhatsApp(
                                context = context,
                                phone = "",
                                text = formattedWhatsAppMessage
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("z_report_send_whatsapp_owner_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال عبر واتساب",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال ملخص اليوم المالي كاملاً عبر واتساب بلمسة واحدة",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                DailyZReportPdfExporter.shareZReportPdf(
                                    context = context,
                                    report = reportData,
                                    storeName = storeName,
                                    currencySymbol = currencySymbol,
                                    preparedBy = preparedByName,
                                    shiftNotes = shiftNotes,
                                    isClosed = isShiftClosed,
                                    shiftClosedTime = shiftClosedTime,
                                    preferWhatsApp = false
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 11.dp, horizontal = 12.dp),
                            modifier = Modifier
                                .weight(1.35f)
                                .testTag("z_report_send_pdf_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "إرسال التقرير بصيغة PDF",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إرسال التقرير بصيغة PDF",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                saveZReportPdfLauncher.launch(
                                    DailyZReportPdfExporter.suggestedFileName(reportData.dayKey)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(vertical = 11.dp, horizontal = 8.dp),
                            modifier = Modifier
                                .weight(0.75f)
                                .testTag("z_report_save_pdf_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "حفظ PDF",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "حفظ PDF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFDC2626)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                DailyZReportPdfExporter.printZReportPdf(
                                    context = context,
                                    report = reportData,
                                    storeName = storeName,
                                    currencySymbol = currencySymbol,
                                    preparedBy = preparedByName,
                                    shiftNotes = shiftNotes,
                                    isClosed = isShiftClosed,
                                    shiftClosedTime = shiftClosedTime
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 11.dp, horizontal = 8.dp),
                            modifier = Modifier
                                .weight(0.65f)
                                .testTag("z_report_print_pdf_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "طباعة PDF",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "طباعة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showWhatsAppPreviewDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("z_report_preview_message_btn")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("معاينة الرسالة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Z-Report", formattedWhatsAppMessage))
                                Toast.makeText(context, "تم نسخ ملخص الإغلاق اليومي (Z-Report) للحافظة ✅", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("z_report_copy_message_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ التقرير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (isShiftClosed) {
                                    viewModel.reopenDayZReportShift(reportData.dayKey)
                                } else {
                                    viewModel.closeDayZReportShift(
                                        dayKey = reportData.dayKey,
                                        expectedCashInDrawer = reportData.expectedCashInDrawer,
                                        totalSales = reportData.totalSalesAmount,
                                        collectedDebts = reportData.collectedDebtsTotal,
                                        totalExpenses = reportData.totalExpensesAmount
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isShiftClosed) Color(0xFF475569) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("z_report_close_shift_btn")
                        ) {
                            Icon(
                                imageVector = if (isShiftClosed) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isShiftClosed) "إعادة فتح اليوم" else "اعتماد الإغلاق",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isShiftClosed && shiftClosedTime.isNotBlank()) {
                        Text(
                            text = "✓ تم اعتماد وإغلاق الصندوق بتاريخ: $shiftClosedTime",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            ZReportSectionCard(
                title = "1. تفصيل المبيعات اليومية (${reportData.invoicesCount} فاتورة)",
                subtitle = "إجمالي القطع المباعة: ${reportData.itemsSoldCount} قطعة",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                accentColor = Color(0xFF3B82F6),
                actionLabel = if (onNavigateToInvoices != null) "أرشيف الفواتير" else null,
                onActionClick = onNavigateToInvoices
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZReportMiniMetricTile(
                        title = "إجمالي المبيعات",
                        amount = reportData.totalSalesAmount,
                        currencySymbol = currencySymbol,
                        accentColor = Color(0xFF3B82F6),
                        subtitle = "${reportData.invoicesCount} فاتورة فعالة",
                        modifier = Modifier.weight(1f)
                    )
                    ZReportMiniMetricTile(
                        title = "مبيعات نقدية (كاش)",
                        amount = reportData.cashSalesAmount,
                        currencySymbol = currencySymbol,
                        accentColor = Color(0xFF10B981),
                        subtitle = "تُضاف للدرج",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZReportMiniMetricTile(
                        title = "مبيعات آجلة (دين)",
                        amount = reportData.debtSalesAmount,
                        currencySymbol = currencySymbol,
                        accentColor = Color(0xFFF59E0B),
                        subtitle = "ذمم على العملاء",
                        modifier = Modifier.weight(1f)
                    )
                    ZReportMiniMetricTile(
                        title = "صافي الربح اليومي",
                        amount = reportData.netDailyProfit,
                        currencySymbol = currencySymbol,
                        accentColor = if (reportData.netDailyProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        subtitle = "بعد المصاريف والتكلفة",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (reportData.pendingReturnRequestsCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏳ طلبات مرتجع فواتير بانتظار موافقة المدير: ${reportData.pendingReturnRequestsCount}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", reportData.pendingReturnRequestsAmount)} $currencySymbol",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item {
            ZReportSectionCard(
                title = "2. الديون المحصلة من العملاء (${reportData.collectedDebtsList.size} عملية)",
                subtitle = "إجمالي التحصيل المضاف للدرج: ${String.format(Locale.US, "%.2f", reportData.collectedDebtsTotal)} $currencySymbol",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = Color(0xFF8B5CF6),
                actionLabel = if (onNavigateToDebts != null) "سجل الديون" else null,
                onActionClick = onNavigateToDebts
            ) {
                if (reportData.collectedDebtsList.isEmpty()) {
                    Text(
                        text = "لا توجد دفعات ديون محصلة مسجلة في هذا اليوم.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        reportData.collectedDebtsList.take(8).forEach { tx ->
                            val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date(tx.date))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.customerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${tx.notes.ifBlank { "سداد دفعة من الحساب" }} • $timeStr",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "+${String.format(Locale.US, "%.2f", tx.amount)} $currencySymbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            ZReportSectionCard(
                title = "3. المصروفات التشغيلية اليومية (${reportData.dailyExpensesList.size} سند)",
                subtitle = "إجمالي المصروفات: ${String.format(Locale.US, "%.2f", reportData.totalExpensesAmount)} $currencySymbol (منها نقداً: ${String.format(Locale.US, "%.2f", reportData.cashExpensesAmount)} $currencySymbol)",
                icon = Icons.Default.Payments,
                accentColor = Color(0xFFEF4444),
                actionLabel = "إضافة مصروف سريع",
                onActionClick = { showQuickExpenseDialog = true }
            ) {
                if (reportData.dailyExpensesList.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "لا توجد مصروفات مسجلة في هذا اليوم.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onNavigateToExpenses != null) {
                            TextButton(onClick = onNavigateToExpenses) {
                                Text("فتح شاشة المصاريف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        reportData.dailyExpensesList.take(8).forEach { exp ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exp.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${exp.category.labelAr} • ${exp.paymentMethodAr} • ${exp.formattedTime}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "-${String.format(Locale.US, "%.2f", exp.amount)} $currencySymbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDrawerConfigDialog) {
        var openingInput by remember {
            mutableStateOf(
                if (openingCash > 0.0) String.format(Locale.US, "%.2f", openingCash) else ""
            )
        }
        var countedInput by remember {
            mutableStateOf(
                countedCash?.let { String.format(Locale.US, "%.2f", it) } ?: ""
            )
        }
        var notesInput by remember { mutableStateOf(shiftNotes) }

        AlertDialog(
            onDismissRequest = { showDrawerConfigDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "إعداد وجرد صندوق النقد (${reportData.dayKey})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = openingInput,
                        onValueChange = { openingInput = it },
                        label = { Text("رصيد افتتاح الدرج الصباحي ($currencySymbol)") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("z_report_opening_cash_input")
                    )

                    OutlinedTextField(
                        value = countedInput,
                        onValueChange = { countedInput = it },
                        label = { Text("الجرد الفعلي للنقد الموجود بالدرج ($currencySymbol)") },
                        placeholder = { Text("اتركه فارغاً أو أدخل المبلغ الفعلي للمطابقة") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("z_report_counted_cash_input")
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("ملاحظات إغلاق الوردية (تظهر في رسالة الواتساب)") },
                        placeholder = { Text("مثال: تم تسليم العهدة للمدير...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("z_report_shift_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedOpening = openingInput.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                        val parsedCounted = countedInput.replace(",", ".").trim().toDoubleOrNull()
                        viewModel.setOpeningCashForDay(reportData.dayKey, parsedOpening)
                        viewModel.setActualCountedCashForDay(reportData.dayKey, parsedCounted)
                        viewModel.setZReportNotesForDay(reportData.dayKey, notesInput)
                        showDrawerConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("حفظ وتحديث التقرير", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDrawerConfigDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showWhatsAppPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsAppPreviewDialog = false },
            title = {
                Text(
                    text = "معاينة رسالة ملخص الإغلاق اليومي (Z-Report)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = formattedWhatsAppMessage,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showWhatsAppPreviewDialog = false
                            DailyZReportPdfExporter.shareZReportPdf(
                                context = context,
                                report = reportData,
                                storeName = storeName,
                                currencySymbol = currencySymbol,
                                preparedBy = preparedByName,
                                shiftNotes = shiftNotes,
                                isClosed = isShiftClosed,
                                shiftClosedTime = shiftClosedTime,
                                preferWhatsApp = false
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("إرسال PDF", fontWeight = FontWeight.ExtraBold)
                    }

                    Button(
                        onClick = {
                            showWhatsAppPreviewDialog = false
                            shareTextViaWhatsApp(
                                context = context,
                                phone = "",
                                text = formattedWhatsAppMessage
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("إرسال نص واتساب", fontWeight = FontWeight.ExtraBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWhatsAppPreviewDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (showQuickExpenseDialog) {
        var expTitle by remember { mutableStateOf("") }
        var expAmount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQuickExpenseDialog = false },
            title = {
                Text(
                    text = "تسجيل مصروف نقدي سريع من الدرج",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = expTitle,
                        onValueChange = { expTitle = it },
                        label = { Text("بيان المصروف (مثال: فطور، نقل، أكياس...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expAmount,
                        onValueChange = { expAmount = it },
                        label = { Text("المبلغ المسحوب من الدرج ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = expAmount.replace(",", ".").trim().toDoubleOrNull() ?: 0.0
                        if (expTitle.isNotBlank() && amt > 0.0) {
                            viewModel.saveExpense(
                                Expense(
                                    title = expTitle.trim(),
                                    amount = amt,
                                    category = ExpenseCategory.OTHER,
                                    dateMillis = selectedDayMillis,
                                    paymentMethod = "CASH",
                                    paidBy = preparedByName
                                )
                            )
                            showQuickExpenseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("تسجيل وخصم من الدرج", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickExpenseDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ZReportFormulaRow(
    label: String,
    amount: Double,
    currencySymbol: String,
    signPrefix: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$signPrefix${String.format(Locale.US, "%.2f", amount)} $currencySymbol",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
private fun ZReportSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier.fillMaxWidth()
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
                        color = accentColor.copy(alpha = 0.14f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (actionLabel != null && onActionClick != null) {
                    OutlinedButton(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(actionLabel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = semantic.divider)

            content()
        }
    }
}
