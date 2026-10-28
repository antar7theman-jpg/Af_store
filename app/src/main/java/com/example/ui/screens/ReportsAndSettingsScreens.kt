package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.ModernChipButton
import com.example.ui.components.ModernDangerButton
import com.example.ui.components.ModernGradientButton
import com.example.ui.components.ModernIconButton
import com.example.ui.components.ModernOutlinedButton
import com.example.ui.components.ModernPrimaryButton
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.AFStoreHeaderLogo
import com.example.data.firebase.StaffAccount
import com.example.data.firebase.StaffAccountRepository
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.ui.components.OwnerSecretVerificationDialog
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.InvoiceStyle
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.BackupMetadata
import com.example.utils.LocalBackupFileInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportsScreen(viewModel: AntarSalesViewModel) {
    val canViewReports by viewModel.canViewReports.collectAsState()
    if (!canViewReports) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "التقارير المالية والأرباح مقتصرة على الإدارة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ليس لديك صلاحية لعرض التقارير المالية والتحليلات.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val semantic = LocalAFStoreSemanticColors.current
    val scope = rememberCoroutineScope()
    val sales by viewModel.sales.collectAsState()
    val products by viewModel.products.collectAsState()
    val purchases by viewModel.purchaseInvoices.collectAsState()
    val logs by viewModel.activityLogs.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val scheduledReportConfig by viewModel.scheduledReportConfig.collectAsState()
    val scheduledReportsArchive by viewModel.scheduledReportsArchive.collectAsState()

    var selectedPeriod by rememberSaveable { mutableStateOf(SalesReportPeriod.DAILY_7) }
    var chartDisplayMode by rememberSaveable { mutableStateOf(SalesChartDisplayMode.BAR_AND_TREND) }
    var metricComparisonMode by rememberSaveable { mutableStateOf(SalesMetricComparisonMode.SALES_VS_PROFIT) }
    var showPdfPreviewDialog by remember { mutableStateOf(false) }
    var showSalesCsvExportDialog by remember { mutableStateOf(false) }

    val computedReportData by produceState(
        initialValue = remember {
            val initBuckets = SalesAnalyticsAggregator.buildTimeBuckets(
                sales = sales,
                products = products,
                period = selectedPeriod
            )
            val initOverview = SalesAnalyticsAggregator.buildOverviewSummary(
                buckets = initBuckets,
                allSales = sales,
                products = products
            )
            val pStart = initBuckets.firstOrNull()?.startMillis ?: 0L
            val pEnd = initBuckets.lastOrNull()?.endMillis ?: Long.MAX_VALUE
            val filtered = sales.filter { it.saleDate in pStart..pEnd }.ifEmpty { sales }
            val initTop = filtered.groupBy { it.productName.substringBefore(" (").trim() }
                .map { (name, list) -> Triple(name, list.sumOf { it.quantitySold }, list.sumOf { it.totalPrice }) }
                .sortedByDescending { it.third }
                .take(5)
            Triple(initBuckets, initOverview, initTop)
        },
        sales,
        products,
        selectedPeriod
    ) {
        value = withContext(Dispatchers.Default) {
            val buckets = SalesAnalyticsAggregator.buildTimeBuckets(
                sales = sales,
                products = products,
                period = selectedPeriod
            )
            val overview = SalesAnalyticsAggregator.buildOverviewSummary(
                buckets = buckets,
                allSales = sales,
                products = products
            )
            val pStart = buckets.firstOrNull()?.startMillis ?: 0L
            val pEnd = buckets.lastOrNull()?.endMillis ?: Long.MAX_VALUE
            val filtered = sales.filter { it.saleDate in pStart..pEnd }.ifEmpty { sales }
            val top = filtered.groupBy { it.productName.substringBefore(" (").trim() }
                .map { (name, list) -> Triple(name, list.sumOf { it.quantitySold }, list.sumOf { it.totalPrice }) }
                .sortedByDescending { it.third }
                .take(5)
            Triple(buckets, overview, top)
        }
    }

    val timeBuckets = computedReportData.first
    val periodOverview = computedReportData.second
    val topSellingProducts = computedReportData.third

    var selectedBucketIndex by remember(selectedPeriod, timeBuckets.size) {
        mutableStateOf((timeBuckets.size - 1).coerceAtLeast(0))
    }

    val totalPurchasesCost = remember(purchases) { purchases.sumOf { it.totalCost } }

    val currentPdfPayload = remember(
        storeName,
        storePhone,
        currencySymbol,
        selectedPeriod,
        chartDisplayMode,
        metricComparisonMode,
        periodOverview,
        totalPurchasesCost,
        timeBuckets,
        topSellingProducts
    ) {
        SalesReportPdfPayload(
            storeName = storeName,
            storePhone = storePhone,
            currencySymbol = currencySymbol,
            period = selectedPeriod,
            chartDisplayMode = chartDisplayMode,
            metricComparisonMode = metricComparisonMode,
            overview = periodOverview,
            totalPurchasesCost = totalPurchasesCost,
            buckets = timeBuckets,
            topSellingProducts = topSellingProducts
        )
    }

    val saveReportPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    SalesReportPdfExporter.saveReportPdfToUri(
                        context = context,
                        payload = currentPdfPayload,
                        targetUri = uri
                    )
                }
                if (ok) {
                    Toast.makeText(
                        context,
                        "تم حفظ تقرير المبيعات والرسوم البيانية بصيغة PDF بنجاح",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "تعذر حفظ ملف PDF في المسار المحدد",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    if (showPdfPreviewDialog) {
        SalesReportPdfPreviewDialog(
            payload = currentPdfPayload,
            onDismiss = { showPdfPreviewDialog = false }
        )
    }

    if (showSalesCsvExportDialog) {
        com.example.ui.components.CsvExportDialog(
            title = "تصدير تقارير المبيعات بصيغة CSV",
            subtitle = "تصدير تفاصيل المبيعات اليومية والفواتير والعملاء وطرق الدفع إلى ملف CSV بترميز UTF-8 مع BOM متوافق مع Excel.",
            sales = sales,
            periodTitle = "تقرير المبيعات (${selectedPeriod.labelAr})",
            storeName = storeName,
            onDismiss = { showSalesCsvExportDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("reports_screen_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "التقارير المالية والرسوم البيانية",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "تحليل المبيعات اليومية والشهرية وصافي الأرباح وطرق الدفع",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semantic.cardElevated,
                    shadowElevation = if (semantic.isDark) 0.dp else 2.dp,
                    border = BorderStroke(1.dp, semantic.cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("إجمالي التوريدات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedMoneyText(
                            targetAmount = totalPurchasesCost,
                            currencySymbol = currencySymbol,
                            decimals = 0,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // PDF Export, Print & Share Action Card
        item {
            SalesReportPdfActionCard(
                selectedPeriod = selectedPeriod,
                chartDisplayMode = chartDisplayMode,
                metricComparisonMode = metricComparisonMode,
                onOpenExportDialog = { showPdfPreviewDialog = true },
                onQuickSharePdf = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            SalesReportPdfExporter.generateCachePdfFile(context, currentPdfPayload)
                        }
                        SalesReportPdfExporter.shareReportPdf(context, currentPdfPayload)
                    }
                },
                onQuickPrintPdf = {
                    SalesReportPdfExporter.printReport(context, currentPdfPayload)
                },
                onQuickSavePdf = {
                    saveReportPdfDocumentLauncher.launch(currentPdfPayload.suggestedFileName)
                },
                onOpenCsvDialog = { showSalesCsvExportDialog = true }
            )
        }

        // Scheduled Daily/Weekly Sales Summary PDF Reports Control & Archive History Card
        item {
            ScheduledReportsControlAndArchiveCard(
                config = scheduledReportConfig,
                archiveList = scheduledReportsArchive,
                onUpdateConfig = { newCfg -> viewModel.updateScheduledReportConfig(newCfg) },
                onGenerateReportNow = { sendEmailNow ->
                    viewModel.generateScheduledReportNow(sendEmailImmediately = sendEmailNow)
                },
                onPreviewArchivedReport = { item ->
                    viewModel.openArchivedScheduledReportPreview(item)
                },
                onEmailArchivedReport = { item ->
                    viewModel.sendArchivedScheduledReportEmail(item)
                },
                onShareArchivedReport = { item ->
                    viewModel.shareArchivedScheduledReport(item)
                },
                onPrintArchivedReport = { item ->
                    viewModel.printArchivedScheduledReport(item)
                },
                onDeleteArchivedReport = { item ->
                    viewModel.deleteArchivedScheduledReport(item)
                },
                onClearAllArchivedReports = {
                    viewModel.clearAllArchivedScheduledReports()
                }
            )
        }

        // Quick Snapshot: Today's Sales vs Yesterday & Current Month vs Previous Month
        item {
            DailyAndMonthlySnapshotsCard(
                overview = periodOverview,
                currencySymbol = currencySymbol
            )
        }

        // Time Period Range Selector (7 Days | 30 Days | 6 Months | 12 Months) + Period KPIs
        item {
            PeriodRangeSelectorCard(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { newPeriod ->
                    selectedPeriod = newPeriod
                },
                overview = periodOverview,
                currencySymbol = currencySymbol
            )
        }

        // Interactive Daily/Monthly Bar & Trend Line Chart + Selected Day/Month Details Card
        item {
            InteractiveSalesBarAndTrendCard(
                buckets = timeBuckets,
                selectedPeriod = selectedPeriod,
                currencySymbol = currencySymbol,
                selectedBucketIndex = selectedBucketIndex,
                onSelectBucketIndex = { idx ->
                    selectedBucketIndex = idx
                },
                chartDisplayMode = chartDisplayMode,
                onChartDisplayModeChange = { chartDisplayMode = it },
                metricComparisonMode = metricComparisonMode,
                onMetricComparisonModeChange = { metricComparisonMode = it }
            )
        }

        // Interactive Payment Methods Donut Chart Card
        item {
            PaymentMethodsDonutChartCard(
                overview = periodOverview,
                currencySymbol = currencySymbol
            )
        }

        // Top Selling Products Card with Animated Progress & Counters
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = semantic.cardElevated
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 3.dp),
                border = BorderStroke(1.dp, semantic.cardBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "أعلى الأصناف مبيعاً وإيراداً (${selectedPeriod.labelAr})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    if (topSellingProducts.isEmpty()) {
                        Text("لا توجد مبيعات مسجلة بعد.", fontSize = 12.sp)
                    } else {
                        val maxRev = topSellingProducts.maxOf { it.third }.coerceAtLeast(1.0)
                        topSellingProducts.forEach { (name, qty, rev) ->
                            val targetProgress = (rev / maxRev).toFloat().coerceIn(0f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("$name ($qty وحدة)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    AnimatedMoneyText(
                                        targetAmount = rev,
                                        currencySymbol = currencySymbol,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.infoBlue
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { targetProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "reports_logs_header") {
            Text("سجل الرقابة والنشاطات الأخير", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        items(
            items = logs.take(20),
            key = { "report_log_${it.id}_${it.timestamp}" },
            contentType = { "activity_log_item" }
        ) { log ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = semantic.cardElevated
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 1.5.dp),
                border = BorderStroke(1.dp, semantic.cardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(log.description, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${log.username} • ${log.actionType}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(log.formattedTime, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private data class PendingRestorePayload(
    val jsonString: String,
    val metadata: BackupMetadata,
    val sourceLabel: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: AntarSalesViewModel,
    onOpenApkTempModal: () -> Unit = {},
    myStaffAccount: StaffAccount? = null,
    allStaffAccounts: List<StaffAccount> = emptyList(),
    pendingStaffCount: Int = 0,
    onOpenStaffManagement: () -> Unit = {},
    onSignOutFirebase: () -> Unit = {}
) {
    val storeName by viewModel.storeName.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val themePalette by viewModel.themePalette.collectAsState()
    val highContrastNumbers by viewModel.highContrastNumbers.collectAsState()
    val invoiceStyle by viewModel.invoiceStyle.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val semanticColors = LocalAFStoreSemanticColors.current

    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsState()
    val localBackups by viewModel.localBackups.collectAsState()
    val isLocalBackupBusy by viewModel.isLocalBackupBusy.collectAsState()
    val isCloudBackupBusy by viewModel.isCloudBackupBusy.collectAsState()
    val lastLocalBackupTime by viewModel.lastLocalBackupTime.collectAsState()
    val scheduledReportConfig by viewModel.scheduledReportConfig.collectAsState()
    val scheduledReportsArchive by viewModel.scheduledReportsArchive.collectAsState()
    val thermalPrinterConfig by viewModel.thermalPrinterConfig.collectAsState()
    val desktopPrinterConfig by viewModel.desktopPrinterConfig.collectAsState()
    val thermalConnectionState by viewModel.thermalConnectionState.collectAsState()
    val isThermalPrinting by viewModel.isThermalPrinting.collectAsState()

    val storePhonesList by viewModel.storePhonesList.collectAsState()
    val storeLogoImagePath by viewModel.storeLogoImagePath.collectAsState()
    val storeLogoCustomText by viewModel.storeLogoCustomText.collectAsState()
    val myProUpgradeRequest by viewModel.myProUpgradeRequest.collectAsState()
    val allProUpgradeRequests by viewModel.allProUpgradeRequests.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showTrialManagerRequestDialog by remember { mutableStateOf(false) }
    var editedStoreName by remember(storeName) { mutableStateOf(storeName) }
    var editedPrimaryPhone by remember(storePhonesList) {
        mutableStateOf(storePhonesList.firstOrNull() ?: "")
    }
    var editedExtraPhones by remember(storePhonesList) {
        mutableStateOf(if (storePhonesList.size > 1) storePhonesList.drop(1) else emptyList())
    }
    var editedLogoImagePath by remember(storeLogoImagePath) { mutableStateOf(storeLogoImagePath) }
    var editedLogoCustomText by remember(storeLogoCustomText) { mutableStateOf(storeLogoCustomText) }
    var isTextLogoMode by remember(storeLogoImagePath, storeLogoCustomText) {
        mutableStateOf(storeLogoImagePath.isBlank() && storeLogoCustomText.isNotBlank())
    }
    var isSavingStoreLogoImage by remember { mutableStateOf(false) }

    val storeLogoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isSavingStoreLogoImage = true
            coroutineScope.launch {
                val savedPath = ProductImageStorageHelper.saveImageFromUri(context, uri)
                isSavingStoreLogoImage = false
                if (!savedPath.isNullOrBlank()) {
                    editedLogoImagePath = savedPath
                    isTextLogoMode = false
                    viewModel.updateStoreLogo(savedPath, editedLogoCustomText)
                    viewModel.showStatusMessage("تم تحديث صورة شعار المتجر للفواتير بنجاح ✓")
                } else {
                    Toast.makeText(context, "تعذر حفظ صورة الشعار", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var backupNoteInput by remember { mutableStateOf("") }
    var specificBackupPathToExport by remember { mutableStateOf<String?>(null) }
    var pendingRestorePayload by remember { mutableStateOf<PendingRestorePayload?>(null) }
    var backupToDeleteConfirm by remember { mutableStateOf<LocalBackupFileInfo?>(null) }
    var showBluetoothPrinterPicker by remember { mutableStateOf(false) }

    val encryptedBackups by viewModel.encryptedBackups.collectAsState()
    var encryptedBackupToExport by remember { mutableStateOf<com.example.data.backup.BackupMetadata?>(null) }
    var encryptedBackupToRestoreConfirm by remember { mutableStateOf<com.example.data.backup.BackupMetadata?>(null) }
    var encryptedBackupToDeleteConfirm by remember { mutableStateOf<com.example.data.backup.BackupMetadata?>(null) }

    val exportEncryptedBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val fileMeta = encryptedBackupToExport
        encryptedBackupToExport = null
        if (uri != null && fileMeta != null) {
            viewModel.exportEncryptedBackupToUri(fileMeta.file, uri)
        }
    }

    val importEncryptedBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importAndRestoreEncryptedBackupFromUri(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshLocalBackups()
    }

    val exportBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val specificPath = specificBackupPathToExport
        specificBackupPathToExport = null
        if (uri != null) {
            viewModel.exportDatabaseBackupToUri(uri, specificPath)
        }
    }

    val importBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.previewBackupFromUri(uri) { jsonContent, metadata ->
                pendingRestorePayload = PendingRestorePayload(
                    jsonString = jsonContent,
                    metadata = metadata,
                    sourceLabel = "ملف خارجي مستورد"
                )
            }
        }
    }

    val attachedImagesCount = remember(products) {
        products.count { it.imagePath.isNotBlank() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        if (currentUser?.isOwner == true || myStaffAccount?.isOwner == true) {
            item {
                com.example.ui.components.OwnerProUpgradeRequestsCard(
                    upgradeRequests = allProUpgradeRequests,
                    onApproveRequest = { req -> viewModel.reviewProUpgradeRequest(req, true) },
                    onRejectRequest = { req -> viewModel.reviewProUpgradeRequest(req, false) }
                )
            }
            item {
                com.example.ui.components.OwnerManagerRequestsAndTrialsCard(
                    allAccounts = allStaffAccounts,
                    onApprovePermanently = { acc -> viewModel.approveManagerAccountPermanently(acc) },
                    onExtendTrialSevenDays = { acc -> viewModel.extendManagerTrialSevenDays(acc) },
                    onSuspendAccount = { acc -> viewModel.suspendManagerAccount(acc) }
                )
            }
        }

        if (currentUser?.isOwner != true && myStaffAccount?.isOwner != true) {
            item {
                com.example.ui.components.ManagerSubscriptionStatusCard(
                    myStaffAccount = myStaffAccount,
                    myProUpgradeRequest = myProUpgradeRequest,
                    onRequestUpgrade = { phone, note ->
                        viewModel.requestProUpgrade(phone, note)
                    }
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("فتح حساب مدير جديد (تجربة أسبوع)", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("إنشاء حساب مدير مستقل برمز تجريبي 7 أيام وتزامن سحابي تلقائي.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { showTrialManagerRequestDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("طلب مدير تجريبي", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Comprehensive Cloud Sync Status Dashboard Card
        item {
            com.example.ui.components.LiveSyncStatusCard(
                viewModel = viewModel,
                myStaffAccount = myStaffAccount
            )
        }

        // Firebase Staff Accounts & Approval Center Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = semanticColors.cardElevated
                ),
                border = BorderStroke(
                    width = if (pendingStaffCount > 0) 1.5.dp else 1.2.dp,
                    color = if (pendingStaffCount > 0) semanticColors.debtAmber else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_firebase_staff_accounts_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ManageAccounts,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (myStaffAccount?.isOwner == true) {
                                    "لوحة المالك (Deffas antar) • إنشاء المدراء واعتماد العمال"
                                } else {
                                    "حسابات العمال والمدراء وبوابة الموافقة Firebase"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (myStaffAccount != null) {
                                    "متصل: ${myStaffAccount.fullName} (${myStaffAccount.email}) • ${myStaffAccount.roleBadgeLabelAr}"
                                } else {
                                    "المالك: ${StaffAccountRepository.OWNER_FULL_NAME} (${StaffAccountRepository.OWNER_DISPLAY_EMAIL})"
                                },
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (pendingStaffCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = semanticColors.debtAmber.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, semanticColors.debtAmber)
                            ) {
                                Text(
                                    text = "$pendingStaffCount طلب معلق",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = semanticColors.debtAmber,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (myStaffAccount != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = semanticColors.profitGreen.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, semanticColors.profitGreen.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "الجلسة محفوظة ومفتوحة تلقائياً ✓",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = semanticColors.profitGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "التوجيه: ${myStaffAccount.defaultDestinationLabelAr}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (myStaffAccount?.isOwner == true) {
                            "بصفتك مالك المتجر (Deffas antar)، يمكنك من هنا إنشاء حسابات المدراء الجدد، وتخصيص صلاحيات كل عامل بدقة: تفعيل أو حجب تقارير المبيعات، سجلات المشتريات، وتكلفة المنتجات، مع قوالب سريعة ومزامنة فورية."
                        } else {
                            "إدارة حسابات وصلاحيات العمال: تفعيل أو تعطيل الوصول إلى تقارير المبيعات، سجلات المشتريات، وتكلفة المنتجات والديون، مع قوالب جاهزة ومزامنة سحابية فورية."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenStaffManagement,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_firebase_staff_modal_settings_btn")
                        ) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    pendingStaffCount > 0 -> "مراجعة الطلبات ($pendingStaffCount) وإدارة الحسابات"
                                    myStaffAccount?.isOwner == true -> "فتح لوحة المالك وإنشاء المدراء (${allStaffAccounts.size})"
                                    else -> "إدارة حسابات العمال (${allStaffAccounts.size})"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }

                        if (myStaffAccount != null) {
                            OutlinedButton(
                                onClick = onSignOutFirebase,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("settings_firebase_sign_out_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "تسجيل الخروج", modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("خروج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Bluetooth Thermal Printer Settings Card (58mm / 80mm / 104mm + ESC/POS Controls)
        item {
            ThermalPrinterSettingsCard(
                config = thermalPrinterConfig,
                connectionState = thermalConnectionState,
                isPrinting = isThermalPrinting,
                invoiceStyle = invoiceStyle,
                onSelectInvoiceStyle = { viewModel.setInvoiceStyle(it) },
                onUpdateConfig = { newCfg -> viewModel.updateThermalPrinterConfig(newCfg) },
                onOpenBluetoothPicker = { showBluetoothPrinterPicker = true },
                onReconnectSavedPrinter = {
                    viewModel.reconnectSavedBluetoothPrinter { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onDisconnectPrinter = { viewModel.disconnectBluetoothPrinter() },
                onPrintTestReceipt = {
                    if (thermalPrinterConfig.lastPrinterAddress.isBlank()) {
                        showBluetoothPrinterPicker = true
                        Toast.makeText(
                            context,
                            "يرجى اختيار طابعة البلوتوث أولاً لإرسال الطباعة التجريبية",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val testPayload = viewModel.buildTestThermalPayload()
                        viewModel.printThermalReceiptPayload(testPayload, copiesOverride = 1) { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        // Desktop Printers Settings Card (Canon / HP / Epson / A4 / A5)
        item {
            DesktopPrinterSettingsCard(
                config = desktopPrinterConfig,
                onUpdateConfig = { newCfg -> viewModel.updateDesktopPrinterConfig(newCfg) },
                onTestPrintDesktopPage = {
                    val testPayload = UniversalPdfReportExporter.buildTestDesktopReportPayload(
                        storeName = storeName,
                        storePhone = storePhonesList.firstOrNull().orEmpty(),
                        currencySymbol = currencySymbol,
                        desktopConfig = desktopPrinterConfig
                    )
                    UniversalPdfReportExporter.printReport(context, testPayload, desktopPrinterConfig)
                }
            )
        }

        // Temporary External APK Link & QR Generator Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = semanticColors.headerContainer
                ),
                border = BorderStroke(1.2.dp, semanticColors.profitGreen.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_apk_temp_link_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = semanticColors.profitGreen.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, semanticColors.profitGreen),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = null,
                                    tint = semanticColors.profitGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إنشاء ملف تحميل التطبيق APK برابط خارجي Temp",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = semanticColors.onHeaderContainer
                            )
                            Text(
                                text = "استخراج حزمة APK الحالية وتوليد رابط خارجي مؤقت مباشر مع رمز QR للمشاركة والتثبيت على أي هاتف",
                                fontSize = 11.sp,
                                color = semanticColors.onHeaderSubtitle
                            )
                        }
                    }

                    Button(
                        onClick = onOpenApkTempModal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = semanticColors.profitGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_apk_temp_link_settings_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح منشئ رابط تحميل APK الخارجي المؤقت + QR", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Sample Catalog & Customers Seeding Card (بيانات السلع والعملاء الافتراضية للتجربة)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = semanticColors.cardElevated),
                elevation = CardDefaults.cardElevation(defaultElevation = if (semanticColors.isDark) 0.dp else 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_seed_sample_data_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "السلع والعملاء الافتراضيون (للتجربة)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${products.size} سلع بالمخزون • ${customers.size} عملاء مسجلين",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF10B981).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "جاهز للتجربة ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "تم تضمين تشكيلة من المواد الاستهلاكية والغذائية بأسعار دقيقة وصور واضحة، بالإضافة لعملاء نقد وعملاء جملة بديون سابقة لتجربة سير البيع والمعاينة المحسنة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.seedSampleCatalogAndCustomers(force = true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("seed_sample_catalog_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إضافة / إعادة تحميل السلع والعملاء الافتراضيين الآن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        // Cloud Database Backup Card (Pro Exclusive)
        item {
            CloudDatabaseBackupCard(
                myStaffAccount = myStaffAccount,
                isBusy = isCloudBackupBusy,
                onUploadCloudBackup = {
                    viewModel.uploadCloudDatabaseBackup()
                },
                onRestoreCloudBackup = {
                    viewModel.restoreCloudDatabaseBackup()
                },
                onRequestUpgrade = {
                    viewModel.showStatusMessage("يرجى الضغط على طلب ترقية الحساب إلى Pro أعلى القائمة.")
                }
            )
        }

        // Local Database Backup & Restore Card
        item {
            LocalDatabaseBackupCard(
                productsCount = products.size,
                salesCount = sales.size,
                customersCount = customers.size,
                purchaseInvoicesCount = purchaseInvoices.size,
                attachedImagesCount = attachedImagesCount,
                localBackups = localBackups,
                encryptedBackups = encryptedBackups,
                isBusy = isLocalBackupBusy,
                lastBackupTime = lastLocalBackupTime,
                backupNoteInput = backupNoteInput,
                onBackupNoteChange = { backupNoteInput = it },
                onCreateLocalBackup = {
                    viewModel.createLocalDatabaseBackup(backupNoteInput)
                    backupNoteInput = ""
                },
                onCreateEncryptedSnapshot = {
                    viewModel.createEncryptedSnapshot(isAuto = false)
                },
                onRestoreEncryptedSnapshot = { meta ->
                    encryptedBackupToRestoreConfirm = meta
                },
                onExportEncryptedSnapshot = { meta ->
                    encryptedBackupToExport = meta
                    exportEncryptedBackupLauncher.launch(meta.fileName)
                },
                onDeleteEncryptedSnapshot = { meta ->
                    encryptedBackupToDeleteConfirm = meta
                },
                onImportEncryptedSnapshotFromFile = {
                    importEncryptedBackupLauncher.launch(arrayOf("*/*", "application/octet-stream"))
                },
                onExportCurrentDatabaseToFile = {
                    specificBackupPathToExport = null
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    exportBackupDocumentLauncher.launch("afstore_backup_$timestamp.json")
                },
                onImportBackupFromFile = {
                    importBackupDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                onRestoreSavedBackup = { backupInfo ->
                    viewModel.previewLocalBackupFile(backupInfo) { jsonContent, metadata ->
                        pendingRestorePayload = PendingRestorePayload(
                            jsonString = jsonContent,
                            metadata = metadata,
                            sourceLabel = "نسخة محفوظة (${backupInfo.fileName})"
                        )
                    }
                },
                onExportSavedBackupToFile = { backupInfo ->
                    specificBackupPathToExport = backupInfo.filePath
                    exportBackupDocumentLauncher.launch(backupInfo.fileName)
                },
                onDeleteSavedBackup = { backupInfo ->
                    backupToDeleteConfirm = backupInfo
                }
            )
        }

        // Scheduled Daily/Weekly Sales Summary PDF Reports Settings & Archive Card
        item {
            ScheduledReportsControlAndArchiveCard(
                config = scheduledReportConfig,
                archiveList = scheduledReportsArchive,
                onUpdateConfig = { newCfg -> viewModel.updateScheduledReportConfig(newCfg) },
                onGenerateReportNow = { sendEmailNow ->
                    viewModel.generateScheduledReportNow(sendEmailImmediately = sendEmailNow)
                },
                onPreviewArchivedReport = { item ->
                    viewModel.openArchivedScheduledReportPreview(item)
                },
                onEmailArchivedReport = { item ->
                    viewModel.sendArchivedScheduledReportEmail(item)
                },
                onShareArchivedReport = { item ->
                    viewModel.shareArchivedScheduledReport(item)
                },
                onPrintArchivedReport = { item ->
                    viewModel.printArchivedScheduledReport(item)
                },
                onDeleteArchivedReport = { item ->
                    viewModel.deleteArchivedScheduledReport(item)
                },
                onClearAllArchivedReports = {
                    viewModel.clearAllArchivedScheduledReports()
                }
            )
        }

        // Store Identity, Phone Numbers, Custom Logo & Currency Symbol
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = semanticColors.cardElevated
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (semanticColors.isDark) 0.dp else 2.5.dp),
                border = BorderStroke(1.dp, semanticColors.cardBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("settings_store_identity_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "بيانات المتجر وعملة الفواتير",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp
                        )
                    }

                    // 1. Store Name Field
                    OutlinedTextField(
                        value = editedStoreName,
                        onValueChange = { editedStoreName = it },
                        label = { Text("اسم المتجر") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_store_name_input")
                    )

                    // 2. Store Phone Numbers Section (Primary + Additional Numbers)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (semanticColors.isDark) 0.32f else 0.45f),
                        border = BorderStroke(1.dp, semanticColors.cardBorder)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "أرقام هاتف المحل (تُطبع في الفواتير)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        editedExtraPhones = editedExtraPhones + ""
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("add_another_store_phone_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "إضافة رقم آخر",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = editedPrimaryPhone,
                                onValueChange = { editedPrimaryPhone = it },
                                label = { Text("رقم هاتف المحل الأساسي") },
                                placeholder = { Text("مثال: 0550123456") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (editedPrimaryPhone.isNotEmpty()) {
                                        IconButton(onClick = { editedPrimaryPhone = "" }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "مسح الرقم",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_primary_phone_input")
                            )

                            editedExtraPhones.forEachIndexed { idx, extraPhone ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = extraPhone,
                                        onValueChange = { updatedVal ->
                                            editedExtraPhones = editedExtraPhones.toMutableList().also {
                                                it[idx] = updatedVal
                                            }
                                        },
                                        label = { Text("رقم هاتف إضافي ${idx + 2}") },
                                        placeholder = { Text("مثال: 0661987654") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_extra_phone_input_${idx + 2}")
                                    )
                                    IconButton(
                                        onClick = {
                                            editedExtraPhones = editedExtraPhones.toMutableList().also {
                                                it.removeAt(idx)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .testTag("remove_extra_phone_btn_${idx + 2}")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "حذف الرقم الإضافي",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            val previewJoinedPhones = remember(editedPrimaryPhone, editedExtraPhones) {
                                (listOf(editedPrimaryPhone) + editedExtraPhones)
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .distinct()
                                    .joinToString(" | ")
                            }
                            if (previewJoinedPhones.isNotBlank()) {
                                Text(
                                    text = "📞 سيظهر في الفاتورة: $previewJoinedPhones",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 3. Store Logo (Logo) Customization Section for Invoices, PDF & App Header
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (semanticColors.isDark) 0.32f else 0.45f),
                        border = BorderStroke(1.dp, semanticColors.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "تخصيص شعار المحل (Logo)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        )
                                        Text(
                                            text = "يُطبع في الفواتير الحرارية و PDF ويظهر أعلى التطبيق",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (editedLogoImagePath.isNotBlank() || editedLogoCustomText.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            editedLogoImagePath = ""
                                            editedLogoCustomText = ""
                                            viewModel.updateStoreLogo("", "")
                                            viewModel.showStatusMessage("تمت استعادة الشعار الافتراضي للمتجر")
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("reset_store_logo_btn")
                                    ) {
                                        Icon(
                                            Icons.Default.Restore,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("افتراضي", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Live Logo Badge Preview
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AFStoreHeaderLogo(
                                        isActive = true,
                                        badgeSize = 58.dp,
                                        logoImagePath = editedLogoImagePath,
                                        logoCustomText = editedLogoCustomText
                                    )
                                    Text(
                                        text = "معاينة الشعار",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = !isTextLogoMode,
                                            onClick = { isTextLogoMode = false },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.AddPhotoAlternate,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("صورة شعار", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.testTag("logo_mode_image_chip")
                                        )
                                        FilterChip(
                                            selected = isTextLogoMode,
                                            onClick = { isTextLogoMode = true },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.TextFields,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("شعار نصي", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.testTag("logo_mode_text_chip")
                                        )
                                    }

                                    if (!isTextLogoMode) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    storeLogoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                enabled = !isSavingStoreLogoImage,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("pick_store_logo_image_btn")
                                            ) {
                                                if (isSavingStoreLogoImage) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.AddPhotoAlternate,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (editedLogoImagePath.isNotBlank()) "تغيير صورة الشعار" else "اختيار صورة Logo من الهاتف",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (editedLogoImagePath.isNotBlank()) {
                                                IconButton(
                                                    onClick = {
                                                        editedLogoImagePath = ""
                                                        viewModel.updateStoreLogo("", editedLogoCustomText)
                                                    },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                                            RoundedCornerShape(10.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "إزالة صورة الشعار",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = editedLogoCustomText,
                                            onValueChange = {
                                                editedLogoCustomText = it
                                                if (it.isNotBlank()) {
                                                    editedLogoImagePath = ""
                                                }
                                            },
                                            label = { Text("نص الشعار المخصص (مثال: AF MARKET)") },
                                            placeholder = { Text("اكتب اختصار أو شعار المحل...") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("settings_logo_custom_text_input")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Save Store Identity Button
                    Button(
                        onClick = {
                            val allPhones = listOf(editedPrimaryPhone) + editedExtraPhones
                            viewModel.updateStoreIdentitySettings(
                                newStoreName = editedStoreName,
                                phones = allPhones,
                                logoImagePath = editedLogoImagePath,
                                logoCustomText = editedLogoCustomText
                            )
                            viewModel.showStatusMessage("تم حفظ بيانات المتجر وأرقام الهواتف والشعار بنجاح ✓")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_store_identity_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ بيانات المتجر والهواتف والشعار", fontWeight = FontWeight.ExtraBold)
                    }

                    HorizontalDivider()
                    Text("العملة الافتراضية في الكاشير والفواتير:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("د.ج", "ج.م", "ر.س", "$").forEach { sym ->
                            FilterChip(
                                selected = currencySymbol == sym,
                                onClick = {
                                    viewModel.setCurrencySymbol(sym)
                                    viewModel.showStatusMessage("تم تغيير عملة المتجر إلى $sym")
                                },
                                label = { Text(sym, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }

        // Theme & Appearance Studio
        item {
            ThemeStudioSettingsCard(
                themeMode = themeMode,
                themePalette = themePalette,
                highContrastNumbers = highContrastNumbers,
                invoiceStyle = invoiceStyle,
                currencySymbol = currencySymbol,
                onSelectThemeMode = { viewModel.setThemeMode(it, showToastBanner = true) },
                onSelectThemePalette = { viewModel.setThemePalette(it, showToastBanner = true) },
                onToggleHighContrast = { viewModel.setHighContrastNumbers(it) },
                onSelectInvoiceStyle = { viewModel.setInvoiceStyle(it) }
            )
        }

        // Active User & Role Switcher
        item {
            var pendingOwnerUserSwitch by remember { mutableStateOf<User?>(null) }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = semanticColors.cardElevated
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (semanticColors.isDark) 0.dp else 2.5.dp),
                border = BorderStroke(1.dp, semanticColors.cardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Text("المستخدم النشط وصلاحيات الكاشير", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "الحساب الحالي: ${currentUser?.fullName ?: StaffAccountRepository.OWNER_FULL_NAME} (${currentUser?.username ?: StaffAccountRepository.OWNER_DISPLAY_EMAIL}) • ${UserRole.getDisplayName(currentUser?.role ?: UserRole.SUPER_ADMIN)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        users.forEach { u ->
                            val isTargetOwner = u.role == UserRole.SUPER_ADMIN ||
                                u.role == UserRole.OWNER ||
                                StaffAccountRepository.isOwnerEmail(u.username)
                            FilterChip(
                                selected = currentUser?.id == u.id,
                                onClick = {
                                    if (currentUser?.id == u.id) return@FilterChip
                                    if (isTargetOwner) {
                                        pendingOwnerUserSwitch = u
                                    } else {
                                        viewModel.switchActiveRole(u)
                                        viewModel.showStatusMessage("تم التبديل إلى حساب: ${u.fullName}")
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (isTargetOwner) "${u.fullName} (المالك 🔒)" else u.fullName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isTargetOwner) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }

            pendingOwnerUserSwitch?.let { targetOwner ->
                OwnerSecretVerificationDialog(
                    title = "تأكيد الدخول بحساب المالك (${targetOwner.fullName})",
                    subtitle = "أدخل الرمز السري الخاص بالمالك (${StaffAccountRepository.OWNER_DISPLAY_EMAIL}) للتبديل إلى حساب المالك:",
                    onDismiss = { pendingOwnerUserSwitch = null },
                    onVerified = {
                        viewModel.switchActiveRole(targetOwner)
                        viewModel.showStatusMessage("تم تفعيل حساب المالك Deffas antar بنجاح ✓")
                        pendingOwnerUserSwitch = null
                    }
                )
            }
        }

        // Developer Contact Footer Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = semanticColors.cardElevated
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (semanticColors.isDark) 0.dp else 3.dp),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
                    .testTag("settings_developer_footer_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "المطور",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "المطور",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Deffas Antar",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "0662931569",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Direct Call Button
                    ModernGradientButton(
                        text = "اتصال مباشر (0662931569)",
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:0662931569")
                            }
                            context.startActivity(dialIntent)
                        },
                        icon = Icons.Default.Phone,
                        gradientColors = listOf(
                            Color(0xFF059669),
                            Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        height = 46.dp,
                        fontSize = 13.5.sp,
                        testTag = "direct_call_developer_btn"
                    )
                }
            }
        }
    }

    pendingRestorePayload?.let { payload ->
        LocalBackupRestoreConfirmDialog(
            payload = payload,
            onConfirmRestore = { replaceExisting ->
                val target = payload
                pendingRestorePayload = null
                viewModel.restoreLocalDatabaseFromJson(
                    jsonString = target.jsonString,
                    replaceExisting = replaceExisting,
                    sourceLabel = target.sourceLabel
                )
            },
            onDismiss = { pendingRestorePayload = null }
        )
    }

    backupToDeleteConfirm?.let { targetBackup ->
        AlertDialog(
            onDismissRequest = { backupToDeleteConfirm = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("حذف النسخة الاحتياطية المحلية", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "هل أنت متأكد من حذف النسخة المحفوظة (${targetBackup.fileName})؟ لا يمكن التراجع عن هذا الإجراء.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLocalDatabaseBackup(targetBackup)
                        backupToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_local_backup_btn")
                ) {
                    Text("حذف النسخة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDeleteConfirm = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showBluetoothPrinterPicker) {
        BluetoothPrinterPickerDialog(
            currentSelectedAddress = thermalPrinterConfig.lastPrinterAddress,
            connectionState = thermalConnectionState,
            hasPermission = viewModel.hasBluetoothConnectPermission(),
            requiredPermissions = viewModel.requiredBluetoothPermissions(),
            loadPairedDevices = { viewModel.getPairedBluetoothPrinters() },
            onSelectAndConnectPrinter = { device ->
                viewModel.selectAndConnectBluetoothPrinter(device) { ok, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (ok) {
                        showBluetoothPrinterPicker = false
                    }
                }
            },
            onDisconnect = { viewModel.disconnectBluetoothPrinter() },
            onDismiss = { showBluetoothPrinterPicker = false }
        )
    }

    if (showTrialManagerRequestDialog) {
        com.example.ui.components.RequestTrialManagerAccountDialog(
            onDismiss = { showTrialManagerRequestDialog = false },
            onSubmitRequest = { fullName, phone, storeNameInput ->
                viewModel.submitTrialManagerAccountRequest(fullName, phone, storeNameInput)
                showTrialManagerRequestDialog = false
            }
        )
    }

    if (encryptedBackupToRestoreConfirm != null) {
        val meta = encryptedBackupToRestoreConfirm!!
        AlertDialog(
            onDismissRequest = { encryptedBackupToRestoreConfirm = null },
            title = {
                Text(
                    text = "تأكيد استعادة النسخة المشفرة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "هل أنت متأكد من استعادة قاعدة البيانات من النسخة الاحتياطية المشفرة؟",
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(text = "الملف: ${meta.fileName}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text(text = "التاريخ: ${meta.formattedDate.toEnglishDigits()}", fontSize = 11.5.sp)
                            Text(text = "الحجم: ${meta.formattedSize}", fontSize = 11.5.sp)
                            Text(text = "التشفير: AES-256-GCM + SHA-256 ✓", fontSize = 11.5.sp, color = Color(0xFF10B981))
                        }
                    }
                    Text(
                        text = "ملاحظة: سيتم إنشاء نقطة فحص أمان تلقائية قبل تطبيق الاستعادة لضمان عدم فقدان أي بيانات.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreEncryptedSnapshot(meta)
                        encryptedBackupToRestoreConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("استعادة الآن", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { encryptedBackupToRestoreConfirm = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (encryptedBackupToDeleteConfirm != null) {
        val meta = encryptedBackupToDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { encryptedBackupToDeleteConfirm = null },
            title = { Text("حذف النسخة المشفرة", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد بالتأكيد حذف النسخة الاحتياطية (${meta.fileName}) نهائياً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEncryptedSnapshot(meta)
                        encryptedBackupToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { encryptedBackupToDeleteConfirm = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun LocalDatabaseBackupCard(
    productsCount: Int,
    salesCount: Int,
    customersCount: Int,
    purchaseInvoicesCount: Int,
    attachedImagesCount: Int,
    localBackups: List<LocalBackupFileInfo>,
    encryptedBackups: List<com.example.data.backup.BackupMetadata> = emptyList(),
    isBusy: Boolean,
    lastBackupTime: Long,
    backupNoteInput: String,
    onBackupNoteChange: (String) -> Unit,
    onCreateLocalBackup: () -> Unit,
    onCreateEncryptedSnapshot: () -> Unit = {},
    onRestoreEncryptedSnapshot: (com.example.data.backup.BackupMetadata) -> Unit = {},
    onExportEncryptedSnapshot: (com.example.data.backup.BackupMetadata) -> Unit = {},
    onDeleteEncryptedSnapshot: (com.example.data.backup.BackupMetadata) -> Unit = {},
    onImportEncryptedSnapshotFromFile: () -> Unit = {},
    onExportCurrentDatabaseToFile: () -> Unit,
    onImportBackupFromFile: () -> Unit,
    onRestoreSavedBackup: (LocalBackupFileInfo) -> Unit,
    onExportSavedBackupToFile: (LocalBackupFileInfo) -> Unit,
    onDeleteSavedBackup: (LocalBackupFileInfo) -> Unit
) {
    val formattedLastBackup = remember(lastBackupTime) {
        if (lastBackupTime <= 0L) {
            "لم يتم إنشاء نسخة بعد"
        } else {
            "آخر نسخة: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(lastBackupTime))
        }
    }

    val semantic = LocalAFStoreSemanticColors.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 3.dp),
        border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("local_database_backup_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.18f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "النسخ الاحتياطي المحلي واستعادة البيانات",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp
                        )
                        Text(
                            text = formattedLastBackup,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF10B981)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${localBackups.size} نسخة محفوظة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = "احفظ نسخة احتياطية شاملة لقاعدة بيانات المبيعات والمخزون والعملاء وفواتير المشتريات وصور السلع محلياً بدون إنترنت، أو صدّرها كملف خارجي.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Current Live Database Summary Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackupStatMiniBadge("الأصناف", productsCount.toString(), Color(0xFF38BDF8))
                    BackupStatMiniBadge("المبيعات", salesCount.toString(), Color(0xFF10B981))
                    BackupStatMiniBadge("العملاء", customersCount.toString(), Color(0xFFF59E0B))
                    BackupStatMiniBadge("المشتريات", purchaseInvoicesCount.toString(), Color(0xFFA855F7))
                    BackupStatMiniBadge("صور السلع", attachedImagesCount.toString(), Color(0xFFEC4899))
                }
            }

            // Disaster Recovery: AES-256 Encrypted Daily Auto-Backup Module
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10B981).copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "النسخ التلقائي المشفر (AES-256 GCM)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF10B981)
                                );
                                Text(
                                    text = "حماية فائقة ضد الكوارث وتلف البيانات",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "حفظ 7 أيام ✓",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "يتم أخذ نسخة احتياطية يومية مشفرة بالكامل في مجلد محلي مخفي ومحمي ضد التلف بـ Checksum SHA-256 مع تنظيف ذاتي للنسخ الأقدم من 7 أيام.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCreateEncryptedSnapshot,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("create_encrypted_snapshot_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ مشفر فوري", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = onImportEncryptedSnapshotFromFile,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("import_encrypted_snapshot_btn")
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استيراد ملف مشفر", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Encrypted Snapshots List
                    if (encryptedBackups.isNotEmpty()) {
                        Text(
                            text = "النسخ المشفرة المتاحة (${encryptedBackups.size}):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            encryptedBackups.take(5).forEach { encBackup ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (encBackup.isAuto) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (encBackup.isAuto) "تلقائي يومي" else "يدوي فوري",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (encBackup.isAuto) Color(0xFF0284C7) else Color(0xFF10B981),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                                Text(
                                                    text = encBackup.formattedDate.toEnglishDigits(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "${encBackup.fileName} • ${encBackup.formattedSize} • SHA-256 ✓",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Button(
                                                onClick = { onRestoreEncryptedSnapshot(encBackup) },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("استعادة", fontSize = 11.sp, color = Color.White)
                                            }

                                            OutlinedButton(
                                                onClick = { onExportEncryptedSnapshot(encBackup) },
                                                enabled = !isBusy,
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Download, contentDescription = "تصدير", modifier = Modifier.size(13.dp))
                                            }

                                            IconButton(
                                                onClick = { onDeleteEncryptedSnapshot(encBackup) },
                                                enabled = !isBusy,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Action: Create Instant In-App Backup
            Button(
                onClick = onCreateLocalBackup,
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_local_backup_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SaveAlt,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إنشاء نسخة احتياطية محلية شاملة الآن",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp
                )
            }

            // External File Export & Import Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onExportCurrentDatabaseToFile,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("export_local_backup_file_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تصدير لملف بالهاتف",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onImportBackupFromFile,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("import_local_backup_file_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "استيراد من ملف خارجي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider()

            // Saved In-App Local Backups List Header
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
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "قائمة النسخ المحلية المحفوظة بالتطبيق",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    )
                }
            }

            if (localBackups.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "لا توجد نسخ احتياطية محفوظة داخل التطبيق حالياً",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "اضغط على «إنشاء نسخة احتياطية محلية شاملة الآن» لحفظ نسخة فورية يمكنك استعادتها في أي وقت بضغطة واحدة.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    localBackups.take(8).forEach { backup ->
                        val formattedDate = remember(backup.createdAt) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                .format(Date(backup.createdAt))
                        }
                        val sizeKb = (backup.fileSizeBytes / 1024L).coerceAtLeast(1L)
                        val meta = backup.metadata

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (meta.backupNote.isNotBlank()) {
                                                "${meta.backupNote} • $formattedDate"
                                            } else {
                                                "نسخة محلية • $formattedDate"
                                            },
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${meta.productsCount} صنف • ${meta.salesCount} مبيعات • ${meta.customersCount} عميل • ${meta.purchaseInvoicesCount} مشتريات • ${meta.imagesCount} صورة ($sizeKb KB)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSavedBackup(backup) },
                                        enabled = !isBusy,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .testTag("delete_local_backup_${backup.fileName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "حذف النسخة",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onRestoreSavedBackup(backup) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("restore_local_backup_${backup.fileName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "استعادة هذه النسخة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onExportSavedBackupToFile(backup) },
                                        enabled = !isBusy,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .testTag("export_saved_backup_${backup.fileName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "حفظ كملف",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupStatMiniBadge(
    label: String,
    value: String,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.5.sp,
            color = accentColor
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LocalBackupRestoreConfirmDialog(
    payload: PendingRestorePayload,
    onConfirmRestore: (replaceExisting: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var replaceExisting by remember { mutableStateOf(false) }
    val meta = payload.metadata
    val formattedCreatedAt = remember(meta.createdAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(meta.createdAt))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "تأكيد استعادة النسخة الاحتياطية",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "المصدر: ${payload.sourceLabel}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "المتجر: ${meta.storeName} • التاريخ: $formattedCreatedAt",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (meta.backupNote.isNotBlank()) {
                            Text(
                                text = "ملاحظة النسخة: ${meta.backupNote}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "• الأصناف والمخزون: ${meta.productsCount} صنف\n" +
                                "• عمليات وسجل المبيعات: ${meta.salesCount} عملية\n" +
                                "• العملاء وحركات الديون: ${meta.customersCount} عميل (${meta.transactionsCount} حركة)\n" +
                                "• فواتير المشتريات: ${meta.purchaseInvoicesCount} فاتورة\n" +
                                "• صور السلع المرفقة: ${meta.imagesCount} صورة",
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Text(
                    text = "اختر طريقة التعامل مع البيانات الموجودة حالياً:",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )

                // Option 1: Merge with Existing Data
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!replaceExisting) {
                        Color(0xFF10B981).copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(
                        width = if (!replaceExisting) 1.5.dp else 1.dp,
                        color = if (!replaceExisting) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { replaceExisting = false }
                        .testTag("restore_mode_merge_option")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = !replaceExisting,
                            onClick = { replaceExisting = false }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MergeType,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "دمج وتحديث مع البيانات الحالية (موصى به)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                )
                            }
                            Text(
                                text = "يحتفظ بالسجلات المضافة حديثاً ويحدّث الأصناف والمبيعات والعملاء وصور السلع من النسخة.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: Full Replace
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (replaceExisting) {
                        Color(0xFFF59E0B).copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(
                        width = if (replaceExisting) 1.5.dp else 1.dp,
                        color = if (replaceExisting) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { replaceExisting = true }
                        .testTag("restore_mode_replace_option")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = replaceExisting,
                            onClick = { replaceExisting = true }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "استبدال البيانات الحالية بالكامل",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                )
                            }
                            Text(
                                text = "يمسح كافة البيانات الحالية أولاً ويستعيد حالة المتجر المطابقة للنسخة الاحتياطية تماماً.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRestore(replaceExisting) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (replaceExisting) Color(0xFFD97706) else Color(0xFF10B981),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_restore_local_backup_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (replaceExisting) "تأكيد الاستبدال والاستعادة" else "تأكيد الدمج والاستعادة",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun formattedDateOrFallback(formatted: String): String = formatted

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeStudioSettingsCard(
    themeMode: AppThemeMode,
    themePalette: AppThemePalette,
    highContrastNumbers: Boolean,
    invoiceStyle: InvoiceStyle,
    currencySymbol: String,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectThemePalette: (AppThemePalette) -> Unit,
    onToggleHighContrast: (Boolean) -> Unit,
    onSelectInvoiceStyle: (InvoiceStyle) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 3.5.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_theme_studio_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "استوديو المظهر والألوان (الوضع الليلي والنهاري)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "نهاري لؤلؤي عصري ببطاقات بيضاء بارزة • ليلي احترافي • أسود OLED • 6 سمات لونية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Live Mini Preview Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.2.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_theme_live_preview")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "معاينة حية للمظهر ${themeMode.badgeEmoji}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = themePalette.labelAr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = semantic.profitContainer
                        ) {
                            Text(
                                text = "+24% ربح صافي",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.profitGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "قهوة مختصة فاخرة 250g",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = semantic.cartonContainer
                                ) {
                                    Text(
                                        text = "كرتونة × 12",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.cartonPurple,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = semantic.debtContainer
                                ) {
                                    Text(
                                        text = "ذمم وتنبيهات واضحة",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.debtAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "1,450 $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "وضوح عالي للأرقام",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 1. Lighting Mode Selector (4 Modes)
            Text(
                text = "1. وضع الإضاءة والتباين:",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = themeMode == mode
                    val modeIcon = when (mode) {
                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                        AppThemeMode.DARK -> Icons.Default.DarkMode
                        AppThemeMode.OLED, AppThemeMode.OLED_BLACK -> Icons.Default.NightsStay
                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.6.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectThemeMode(mode) }
                            .testTag("settings_theme_mode_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = mode.labelAr,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${mode.labelAr} ${mode.badgeEmoji}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mode.subtitleAr,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "مفعل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. 6 Curated Color Palettes
            Text(
                text = "2. السمات اللونية الاحترافية (6 سمات):",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                AppThemePalette.entries.forEach { pal ->
                    val isSelected = themePalette == pal
                    val primarySwatch = Color(pal.primaryHex)
                    val secondarySwatch = Color(pal.secondaryHex)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            primarySwatch.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.6.dp else 1.dp,
                            color = if (isSelected) primarySwatch else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectThemePalette(pal) }
                            .testTag("settings_theme_palette_${pal.name}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(primarySwatch)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(secondarySwatch)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = primarySwatch,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = pal.labelAr,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = if (isSelected) primarySwatch else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pal.subtitleAr,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 3. High Contrast Numbers & Glare Reduction Switch
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleHighContrast(!highContrastNumbers) }
                    .testTag("settings_high_contrast_toggle")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contrast,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "وضع التباين الفائق للأرقام والأسعار",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "يعزز حدة ألوان الأسعار والربح وحدود البطاقات للقراءة الفورية",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = highContrastNumbers,
                        onCheckedChange = onToggleHighContrast
                    )
                }
            }

        }
    }
}

@Composable
fun CloudDatabaseBackupCard(
    myStaffAccount: StaffAccount?,
    isBusy: Boolean,
    onUploadCloudBackup: () -> Unit,
    onRestoreCloudBackup: () -> Unit,
    onRequestUpgrade: () -> Unit
) {
    val isPro = myStaffAccount?.canAccessProAdvancedFeatures == true

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) Color(0xFF10B981).copy(alpha = 0.08f) else Color(0xFFF59E0B).copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            1.5.dp,
            if (isPro) Color(0xFF10B981).copy(alpha = 0.45f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("cloud_database_backup_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = if (isPro) Color(0xFF10B981) else Color(0xFFD97706),
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "النسخ الاحتياطي السحابي (Firestore) ☁️",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "حماية وحفظ بيانات المبيعات والمخزون عند تبديل الأجهزة",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPro) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isPro) Color(0xFF10B981) else Color(0xFFF59E0B))
                ) {
                    Text(
                        text = if (isPro) "Pro (متاحة)" else "Lite (محظورة)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPro) Color(0xFF047857) else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (isPro) {
                Text(
                    text = "يمكنك رفع نسخة احتياطية مشفرة لبيانات متجرك إلى السحابة لضمان استرجاعها فوراً عند الانتقال لجهاز جديد أو تسجيل الدخول من جديد.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUploadCloudBackup,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("upload_cloud_backup_btn")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("رفع السحابة ☁️", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onRestoreCloudBackup,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("restore_cloud_backup_btn")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة من السحابة 📥", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "تنبيه باقة Lite: النسخ الاحتياطي السحابي مخصص لحسابات Pro فقط",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFFB45309)
                        )
                        Text(
                            text = "حسابات Lite تعمل محلياً (Offline). لحفظ بياناتك سحابياً وضمان عدم ضياعها عند تبديل الأجهزة، يرجى الترقية إلى باقة Pro.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                        Button(
                            onClick = onRequestUpgrade,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("request_pro_upgrade_from_backup_btn")
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طلب ترقية إلى Pro 🚀", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun String.toEnglishDigits(): String {
    val easternDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    var s = this
    for (i in easternDigits.indices) {
        s = s.replace(easternDigits[i], ('0' + i))
    }
    return s
}


