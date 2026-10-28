package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.LocalShipping
import com.example.ui.screens.PreSalesOrdersScreen
import com.example.ui.screens.WorkerTrackingDashboardScreen
import com.example.ui.screens.LoyaltyDashboardScreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AntarRepository
import com.example.data.database.AntarDatabase
import com.example.data.firebase.StaffAccountRepository
import com.example.data.firebase.StaffApprovalStatus
import com.example.ui.components.ApkDownloadModal
import com.example.ui.components.ApkTempLinkActionButton
import com.example.ui.components.FirebaseAuthAndApprovalGate
import com.example.ui.components.PendingManagerRequestsDialog
import com.example.ui.components.PendingStaffApprovalsBanner
import com.example.ui.components.TopBarManagerPendingRequestsButton
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DebtsAndMovementsScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.InvoicesArchiveScreen
import com.example.ui.screens.LiveDashboardScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.PurchaseInvoicesScreen
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.NotificationsActive
import com.example.ui.screens.LowStockInAppNotificationBanner
import com.example.ui.screens.ProductImageStorageHelper
import com.example.ui.screens.ReorderAlertsManagementScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesReportPdfPreviewDialog
import com.example.ui.screens.ScheduledReportInAppNotificationBanner
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SyncStatusDropdownPanel
import com.example.ui.screens.SyncStatusScreen
import com.example.ui.screens.TopBarLowStockBellButton
import com.example.ui.theme.AFStoreTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.ui.viewmodels.AntarSalesViewModelFactory
import kotlinx.coroutines.delay
import java.util.Locale

enum class MainAppTab(
    val labelAr: String,
    val subtitleAr: String,
    val icon: ImageVector,
    val routeTag: String,
    val showInBottomBar: Boolean
) {
    POS("الكاشير", "نقطة البيع الفورية السريعة", Icons.Default.PointOfSale, "tab_pos", true),
    PRE_SALE_POS("البيع المسبق", "أخذ طلبيات العملاء للمستودع", Icons.Default.EventNote, "tab_pre_sale_pos", true),
    PRE_SALES_ORDERS("طلبات البيع المسبق", "إدارة وتسليم طلبيات المستودع", Icons.Default.LocalShipping, "tab_pre_sales_orders", false),
    CUSTOMERS("العملاء", "حسابات العملاء والديون", Icons.Default.Group, "tab_customers", false),
    LOYALTY("ولاء العملاء", "لوحة تحكم الولاء ونقاط المكافآت", Icons.Default.Star, "tab_loyalty", false),
    SETTINGS("الإعدادات", "إعدادات المتجر والسحابة", Icons.Default.Settings, "tab_settings", true),
    DASHBOARD("لوحة التحكم", "رسوم Recharts الحية للمبيعات والأرباح", Icons.Default.Dashboard, "tab_dashboard", false),
    DEBTS("الديون وحركتها", "سجل المديونيات والتحصيل والسداد", Icons.Default.AccountBalanceWallet, "tab_debts", false),
    INVOICES("الفواتير", "أرشيف فواتير المبيعات والمرتجعات", Icons.AutoMirrored.Filled.ReceiptLong, "tab_invoices", false),
    PURCHASES("فواتير المشتريات", "توريد البضائع وفواتير الموردين", Icons.Default.ShoppingBag, "tab_purchases", false),
    INVENTORY("المخزون", "إدارة الأصناف والكميات المحجوزة", Icons.Default.Inventory2, "tab_inventory", false),
    REORDER_ALERTS("تنبيهات الطلب", "إدارة النواقص وإشعارات المخزون", Icons.Default.NotificationsActive, "tab_reorder_alerts", false),
    WORKER_TRACKING("تتبع العمال", "تتبع إحداثيات GPS للعمال والمناديب مباشر", Icons.Default.MyLocation, "tab_worker_tracking", false),
    REPORTS("التقارير", "الأرباح الصافية والإحصائيات المالية", Icons.Default.Analytics, "tab_reports", false)
}

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        val locale = java.util.Locale("ar")
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val locale = java.util.Locale("ar")
            java.util.Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
            window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Locale/RTL init error", t)
        }
        enableEdgeToEdge()

        val db = AntarDatabase.getDatabase(applicationContext)
        val repository = AntarRepository(db.storeDao())
        val factory = AntarSalesViewModelFactory(application, repository)
        val openReorderFromNotification = intent?.getBooleanExtra("open_reorder_alerts_screen", false) == true

        setContent {
            val salesViewModel: AntarSalesViewModel = viewModel(factory = factory)
            val themeMode by salesViewModel.themeMode.collectAsState()
            val themePalette by salesViewModel.themePalette.collectAsState()
            val highContrastNumbers by salesViewModel.highContrastNumbers.collectAsState()

            AFStoreTheme(
                themeMode = themeMode,
                palette = themePalette,
                highContrastNumbers = highContrastNumbers,
                isRtl = true
            ) {
                AFStoreMainApp(
                    viewModel = salesViewModel,
                    openReorderAlertsInitially = openReorderFromNotification
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AFStoreMainApp(
    viewModel: AntarSalesViewModel,
    openReorderAlertsInitially: Boolean = false
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainAppTab.POS) }
    var showApkTempModal by rememberSaveable { mutableStateOf(false) }
    var showLogoMenu by rememberSaveable { mutableStateOf(false) }
    var logoMenuTab by rememberSaveable { mutableStateOf(0) }
    var showFullScreenSyncStatus by rememberSaveable { mutableStateOf(false) }
    var showQuickThemeMenu by rememberSaveable { mutableStateOf(false) }

    val storeName by viewModel.storeName.collectAsState()
    val storeLogoImagePath by viewModel.storeLogoImagePath.collectAsState()
    val storeLogoCustomText by viewModel.storeLogoCustomText.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val statusMessage by viewModel.statusBannerMessage.collectAsState()
    val unreadScheduledReport by viewModel.latestUnreadScheduledReport.collectAsState()
    val previewScheduledPayload by viewModel.previewingScheduledReportPayload.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val activeLowStockAlert by viewModel.activeLowStockInAppAlert.collectAsState()
    val outOfStockCount = remember(lowStockProducts) { lowStockProducts.count { it.stock <= 0 } }

    val themeMode by viewModel.themeMode.collectAsState()
    val themePalette by viewModel.themePalette.collectAsState()
    val highContrastNumbers by viewModel.highContrastNumbers.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncQueueCount.collectAsState()
    val semanticColors = LocalAFStoreSemanticColors.current

    val totalActiveDebts = remember(customers, purchaseInvoices) {
        customers.sumOf { it.balanceDebt.coerceAtLeast(0.0) } +
            purchaseInvoices.sumOf { it.remainingDebt.coerceAtLeast(0.0) }
    }

    val saveableStateHolder = rememberSaveableStateHolder()
    var lastRoutedIdentityKey by rememberSaveable { mutableStateOf("") }

    val appContext = LocalContext.current
    LaunchedEffect(Unit) {
        com.example.data.backup.DatabaseBackupManager.checkAndPerformDailyAutoBackup(appContext)
    }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(4000)
            viewModel.clearStatusMessage()
        }
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isCompactScreen = screenWidthDp < 380
    val isWideScreen = screenWidthDp >= 600
    val logoBadgeSize: Dp = when {
        isCompactScreen -> 38.dp
        isWideScreen -> 46.dp
        else -> 42.dp
    }
    val bottomNavIconSize: Dp = when {
        isCompactScreen -> 20.dp
        isWideScreen -> 24.dp
        else -> 22.dp
    }

    BackHandler(enabled = showLogoMenu || showQuickThemeMenu || showFullScreenSyncStatus) {
        if (showFullScreenSyncStatus) {
            showFullScreenSyncStatus = false
        } else {
            showLogoMenu = false
            showQuickThemeMenu = false
        }
    }

    BackHandler(enabled = !showLogoMenu && !showQuickThemeMenu && !showFullScreenSyncStatus && !selectedTab.showInBottomBar) {
        selectedTab = MainAppTab.POS
    }

    FirebaseAuthAndApprovalGate(
        storeName = storeName,
        onApprovedAccountReady = { staffAccount ->
            viewModel.syncAuthenticatedFirebaseStaffAccount(staffAccount)
        },
        onOwnerCreatedManager = { createdManager, localPin ->
            viewModel.saveOwnerCreatedManagerToRoom(createdManager, localPin)
        }
    ) { myStaffAccount, allStaffAccounts, pendingStaffCount, onOpenStaffManagement, onSignOutFirebase ->
        var showPendingManagerRequestsModal by rememberSaveable { mutableStateOf(false) }
        var showProRestrictedDialog by rememberSaveable { mutableStateOf(false) }
        var restrictedFeatureName by rememberSaveable { mutableStateOf("") }

        val handleTabClick: (MainAppTab) -> Unit = { tab ->
            val isAdvancedFeature = tab == MainAppTab.WORKER_TRACKING || tab == MainAppTab.REPORTS || tab == MainAppTab.DASHBOARD
            if (isAdvancedFeature && !myStaffAccount.canAccessProAdvancedFeatures) {
                restrictedFeatureName = tab.labelAr
                showProRestrictedDialog = true
            } else {
                showLogoMenu = false
                showQuickThemeMenu = false
                selectedTab = tab
            }
        }
        var lastNotifiedPendingCount by rememberSaveable { mutableStateOf(0) }
        LaunchedEffect(pendingStaffCount, myStaffAccount.isOwner) {
            if (myStaffAccount.isOwner && pendingStaffCount > lastNotifiedPendingCount && pendingStaffCount > 0) {
                viewModel.showStatusMessage("🔔 تنبيه للمالك: لديك $pendingStaffCount طلب حساب جديد (بما في ذلك طلبات Pro التجريبية 7 أيام) بانتظار قرار القبول أو الرفض.")
            }
            lastNotifiedPendingCount = pendingStaffCount
        }

        var lastNotifiedLiteCount by rememberSaveable { mutableStateOf(0) }
        val currentLiteCount = remember(allStaffAccounts) { allStaffAccounts.count { it.syncMode == "lite" } }
        LaunchedEffect(currentLiteCount, myStaffAccount.isOwner) {
            if (myStaffAccount.isOwner && currentLiteCount > lastNotifiedLiteCount && currentLiteCount > 0) {
                val latestLite = allStaffAccounts.firstOrNull { it.syncMode == "lite" }
                val storeInfo = if (latestLite != null && latestLite.branchName.isNotBlank()) " لمتجر «${latestLite.branchName}»" else ""
                val managerInfo = if (latestLite != null && latestLite.fullName.isNotBlank()) " (${latestLite.fullName})" else ""
                viewModel.showStatusMessage("🏪 إشعار للمالك: قام مستخدم جديد بفتح حساب Lite / offline مجاني$storeInfo$managerInfo وتفعيله تلقائياً.")
            }
            lastNotifiedLiteCount = currentLiteCount
        }
        val currentIdentityKey = "${myStaffAccount.userId}:${myStaffAccount.role}:${myStaffAccount.isOwner}"
        LaunchedEffect(currentIdentityKey, openReorderAlertsInitially) {
            if (openReorderAlertsInitially) {
                selectedTab = MainAppTab.REORDER_ALERTS
            } else if (lastRoutedIdentityKey != currentIdentityKey) {
                lastRoutedIdentityKey = currentIdentityKey
                selectedTab = when {
                    myStaffAccount.isOwner -> MainAppTab.DASHBOARD
                    myStaffAccount.isRegularManager -> MainAppTab.INVENTORY
                    else -> MainAppTab.POS
                }
            }
        }

        val workerPerms = remember(myStaffAccount) { myStaffAccount.toWorkerPermissions() }
        val permittedTabs = remember(myStaffAccount, workerPerms) {
            if (myStaffAccount.isManagerOrAdmin) {
                MainAppTab.entries
            } else {
                MainAppTab.entries.filter { tab ->
                    when (tab) {
                        MainAppTab.POS -> true
                        MainAppTab.PRE_SALE_POS -> workerPerms.canMakeSales
                        MainAppTab.PRE_SALES_ORDERS -> workerPerms.canMakeSales || workerPerms.canManageInventory
                        MainAppTab.INVENTORY -> workerPerms.canManageInventory
                        MainAppTab.CUSTOMERS -> workerPerms.canManageDebtsAndCustomers
                        MainAppTab.LOYALTY -> workerPerms.canManageDebtsAndCustomers
                        MainAppTab.WORKER_TRACKING -> true
                        MainAppTab.SETTINGS -> true
                        MainAppTab.DASHBOARD -> workerPerms.canViewReports
                        MainAppTab.DEBTS -> workerPerms.canManageDebtsAndCustomers
                        MainAppTab.INVOICES -> workerPerms.canMakeSales
                        MainAppTab.PURCHASES -> workerPerms.canManagePurchaseInvoices
                        MainAppTab.REORDER_ALERTS -> workerPerms.canManageInventory || workerPerms.canManagePurchaseInvoices
                        MainAppTab.REPORTS -> workerPerms.canViewReports
                    }
                }
            }
        }

        val bottomBarTabs = remember(permittedTabs) { permittedTabs.filter { it.showInBottomBar } }
        val logoMenuTabs = remember(permittedTabs) { permittedTabs.filter { !it.showInBottomBar } }
        val isLogoMenuSectionActive = !selectedTab.showInBottomBar

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            showQuickThemeMenu = false
                                            showLogoMenu = !showLogoMenu
                                        }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                        .testTag("app_logo_menu_trigger"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AFStoreHeaderLogo(
                                        isActive = isLogoMenuSectionActive || showLogoMenu,
                                        badgeSize = logoBadgeSize,
                                        logoImagePath = storeLogoImagePath,
                                        logoCustomText = storeLogoCustomText
                                    )

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = storeName,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = if (isCompactScreen) 15.sp else 17.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = if (semanticColors.isDark) 0.14f else 0.12f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (semanticColors.isDark) 0.45f else 0.65f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "قائمة أقسام المتجر",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .padding(1.dp)
                                                        .size(if (isCompactScreen) 16.dp else 18.dp)
                                                )
                                            }
                                            if (isLogoMenuSectionActive) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Text(
                                                        text = selectedTab.labelAr,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${myStaffAccount.fullName} • ${myStaffAccount.roleBadgeLabelAr} • جلسة مفتوحة ✓",
                                            fontSize = if (isCompactScreen) 9.5.sp else 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            if (myStaffAccount.isOwner) {
                                TopBarManagerPendingRequestsButton(
                                    pendingCount = pendingStaffCount,
                                    onClick = {
                                        showLogoMenu = false
                                        showQuickThemeMenu = false
                                        showPendingManagerRequestsModal = true
                                    }
                                )
                            }
                            TopBarLowStockBellButton(
                                lowStockCount = lowStockProducts.size,
                                outOfStockCount = outOfStockCount,
                                isActiveScreen = selectedTab == MainAppTab.REORDER_ALERTS,
                                onClick = {
                                    showLogoMenu = false
                                    showQuickThemeMenu = false
                                    selectedTab = MainAppTab.REORDER_ALERTS
                                }
                            )
                            com.example.ui.components.TopBarAutoSyncButton(
                                syncManager = viewModel.offlineSyncManager,
                                pendingQueueCount = pendingSyncCount
                            )


                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = semanticColors.headerBorder
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = semanticColors.headerBorder
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = NavigationBarDefaults.Elevation,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .height(82.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            bottomBarTabs.forEach { tab ->
                                val isSelectedTab = selectedTab == tab
                                val isPrimarySaleTab = tab == MainAppTab.POS || tab == MainAppTab.PRE_SALE_POS
                                val capsuleWidth = when {
                                    isPrimarySaleTab && isSelectedTab -> if (isCompactScreen) 58.dp else 64.dp
                                    isPrimarySaleTab -> if (isCompactScreen) 50.dp else 56.dp
                                    isSelectedTab -> if (isCompactScreen) 44.dp else 48.dp
                                    else -> if (isCompactScreen) 38.dp else 42.dp
                                }
                                val capsuleHeight = if (isCompactScreen) 30.dp else 32.dp
                                val iconSize = when {
                                    isPrimarySaleTab && isSelectedTab -> if (isCompactScreen) 23.dp else 25.dp
                                    isPrimarySaleTab -> if (isCompactScreen) 22.dp else 24.dp
                                    isSelectedTab -> if (isCompactScreen) 21.dp else 22.dp
                                    else -> if (isCompactScreen) 20.dp else 21.dp
                                }

                                NavigationBarItem(
                                    selected = isSelectedTab,
                                    onClick = {
                                        handleTabClick(tab)
                                    },
                                    icon = {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .offset(y = (-10).dp)
                                                .width(capsuleWidth)
                                                .height(capsuleHeight)
                                                .clip(RoundedCornerShape(16.dp))
                                                .then(
                                                    if (isPrimarySaleTab && !isSelectedTab) {
                                                        Modifier.border(
                                                            BorderStroke(
                                                                0.8.dp,
                                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                            ),
                                                            RoundedCornerShape(16.dp)
                                                        )
                                                    } else Modifier
                                                )
                                                .background(
                                                    when {
                                                        isSelectedTab -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                                        isPrimarySaleTab -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.labelAr,
                                                tint = if (isSelectedTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(iconSize)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.labelAr,
                                            fontSize = if (isCompactScreen) 9.5.sp else 10.5.sp,
                                            fontWeight = if (isSelectedTab) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.offset(y = (-10).dp)
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.routeTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 80.dp, max = 96.dp),
                        header = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            ) {
                                AFStoreHeaderLogo(
                                    isActive = isLogoMenuSectionActive || showLogoMenu,
                                    badgeSize = 38.dp,
                                    logoImagePath = storeLogoImagePath,
                                    logoCustomText = storeLogoCustomText
                                )
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                permittedTabs.forEach { tab ->
                                    val isSelectedTab = selectedTab == tab
                                    NavigationRailItem(
                                        selected = isSelectedTab,
                                        onClick = {
                                            handleTabClick(tab)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.labelAr,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.labelAr,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelectedTab) FontWeight.ExtraBold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("rail_${tab.routeTag}")
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = semanticColors.dangerRed.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, semanticColors.dangerRed.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clickable {
                                            lastRoutedIdentityKey = ""
                                            onSignOutFirebase()
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .testTag("rail_sign_out_btn")
                                ) {
                                    Text(
                                        text = "خروج",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = semanticColors.dangerRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    VerticalDivider(
                        thickness = 1.dp,
                        color = semanticColors.headerBorder
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.6.dp, semanticColors.headerBorder.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_role_routing_quick_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when {
                                        myStaffAccount.isOwner -> semanticColors.profitGreen.copy(alpha = 0.12f)
                                        myStaffAccount.isRegularManager -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else -> semanticColors.infoBlue.copy(alpha = 0.12f)
                                    },
                                    border = BorderStroke(
                                        width = 0.8.dp,
                                        color = when {
                                            myStaffAccount.isOwner -> semanticColors.profitGreen.copy(alpha = 0.5f)
                                            myStaffAccount.isRegularManager -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else -> semanticColors.infoBlue.copy(alpha = 0.5f)
                                        }
                                    )
                                ) {
                                    Text(
                                        text = myStaffAccount.roleBadgeLabelAr,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            myStaffAccount.isOwner -> semanticColors.profitGreen
                                            myStaffAccount.isRegularManager -> MaterialTheme.colorScheme.primary
                                            else -> semanticColors.infoBlue
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "جلسة نشطة ✓",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (myStaffAccount.isManagerOrAdmin) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clickable { onOpenStaffManagement() }
                                            .testTag("quick_bar_open_staff_management_btn")
                                    ) {
                                        Text(
                                            text = if (myStaffAccount.isOwner) "إدارة الحسابات" else "إدارة العمال",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (!isWideScreen) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = semanticColors.dangerRed.copy(alpha = 0.08f),
                                        border = BorderStroke(0.8.dp, semanticColors.dangerRed.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .clickable {
                                                lastRoutedIdentityKey = ""
                                                onSignOutFirebase()
                                            }
                                            .testTag("quick_bar_sign_out_btn")
                                    ) {
                                        Text(
                                            text = "خروج",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = semanticColors.dangerRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PendingStaffApprovalsBanner(
                        pendingCount = pendingStaffCount,
                        onOpenApprovalModal = onOpenStaffManagement
                    )

                    AnimatedVisibility(visible = statusMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearStatusMessage() }) {
                                    Text("إخفاء", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = unreadScheduledReport != null) {
                        val report = unreadScheduledReport
                        if (report != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                ScheduledReportInAppNotificationBanner(
                                    report = report,
                                    onPreviewPdf = { item ->
                                        viewModel.openArchivedScheduledReportPreview(item)
                                    },
                                    onSendEmailWithPdf = { item ->
                                        viewModel.sendArchivedScheduledReportEmail(item)
                                    },
                                    onDismissBanner = { item ->
                                        viewModel.markScheduledReportNotificationRead(item.id)
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = activeLowStockAlert != null) {
                        val alert = activeLowStockAlert
                        if (alert != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                LowStockInAppNotificationBanner(
                                    alert = alert,
                                    onOpenReorderScreen = {
                                        selectedTab = MainAppTab.REORDER_ALERTS
                                    },
                                    onDismiss = {
                                        viewModel.dismissLowStockInAppAlert()
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(40))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(30))
                                    )
                            },
                            label = "MainAppTabTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { targetTab ->
                            saveableStateHolder.SaveableStateProvider(targetTab.name) {
                                when (targetTab) {
                                    MainAppTab.POS -> PosScreen(
                                        viewModel = viewModel,
                                        onOpenReorderAlertsScreen = { selectedTab = MainAppTab.REORDER_ALERTS },
                                        isPreSaleMode = false
                                    )
                                    MainAppTab.PRE_SALE_POS -> PosScreen(
                                        viewModel = viewModel,
                                        onOpenReorderAlertsScreen = { selectedTab = MainAppTab.REORDER_ALERTS },
                                        isPreSaleMode = true,
                                        onNavigateToPreSalesList = { selectedTab = MainAppTab.PRE_SALES_ORDERS }
                                    )
                                    MainAppTab.PRE_SALES_ORDERS -> PreSalesOrdersScreen(
                                        viewModel = viewModel,
                                        onNavigateToNewPreSale = { selectedTab = MainAppTab.PRE_SALE_POS }
                                    )
                                    MainAppTab.INVENTORY -> InventoryScreen(
                                        viewModel = viewModel,
                                        onOpenReorderAlertsScreen = { selectedTab = MainAppTab.REORDER_ALERTS }
                                    )
                                    MainAppTab.DASHBOARD -> LiveDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToTab = { tab -> selectedTab = tab }
                                    )
                                    MainAppTab.DEBTS -> DebtsAndMovementsScreen(viewModel = viewModel)
                                    MainAppTab.INVOICES -> InvoicesArchiveScreen(viewModel = viewModel)
                                    MainAppTab.PURCHASES -> PurchaseInvoicesScreen(viewModel = viewModel)
                                    MainAppTab.REORDER_ALERTS -> ReorderAlertsManagementScreen(
                                        viewModel = viewModel,
                                        onNavigateToPurchases = { selectedTab = MainAppTab.PURCHASES }
                                    )
                                    MainAppTab.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                                    MainAppTab.LOYALTY -> LoyaltyDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { selectedTab = MainAppTab.POS }
                                    )
                                    MainAppTab.WORKER_TRACKING -> WorkerTrackingDashboardScreen(
                                        viewModel = viewModel,
                                        currentUser = currentUser,
                                        onNavigateBack = { selectedTab = MainAppTab.POS }
                                    )
                                    MainAppTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                                    MainAppTab.SETTINGS -> SettingsScreen(
                                        viewModel = viewModel,
                                        onOpenApkTempModal = { showApkTempModal = true },
                                        myStaffAccount = myStaffAccount,
                                        allStaffAccounts = allStaffAccounts,
                                        pendingStaffCount = pendingStaffCount,
                                        onOpenStaffManagement = onOpenStaffManagement,
                                        onSignOutFirebase = onSignOutFirebase
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Instant in-window sections menu overlay (zero frame lag)
        if (showLogoMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (semanticColors.isDark) 0.40f else 0.22f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showLogoMenu = false
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 14.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 60.dp, start = 12.dp, end = 12.dp, bottom = 24.dp)
                        .navigationBarsPadding()
                        .widthIn(min = 310.dp, max = 345.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* consume clicks inside menu card */ }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 6.dp)
                    ) {
                        // Header with Title and Segmented Tabs
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (logoMenuTab == 0) "أقسام المتجر ولوحة التحكم" else "صحة المزامنة السحابية",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { showLogoMenu = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Segmented Switcher
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (logoMenuTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                                        shadowElevation = if (logoMenuTab == 0) 2.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { logoMenuTab = 0 }
                                            .testTag("logo_tab_sections")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Dashboard,
                                                contentDescription = null,
                                                tint = if (logoMenuTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "أقسام المتجر",
                                                fontSize = 11.sp,
                                                fontWeight = if (logoMenuTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (logoMenuTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (logoMenuTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                                        shadowElevation = if (logoMenuTab == 1) 2.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { logoMenuTab = 1 }
                                            .testTag("logo_tab_sync_status")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(if (pendingSyncCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981))
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "صحة المزامنة",
                                                fontSize = 11.sp,
                                                fontWeight = if (logoMenuTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (logoMenuTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (pendingSyncCount > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(14.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = pendingSyncCount.toString(),
                                                            fontSize = 8.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        if (logoMenuTab == 1) {
                            SyncStatusDropdownPanel(
                                viewModel = viewModel,
                                onClose = { showLogoMenu = false },
                                onOpenFullScreen = {
                                    showLogoMenu = false
                                    showFullScreenSyncStatus = true
                                },
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable { logoMenuTab = 1 }
                                    .testTag("quick_sync_bar_click")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (pendingSyncCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981))
                                        )
                                        Text(
                                            text = if (pendingSyncCount > 0) "$pendingSyncCount معلقة للرفع" else "السحابة متزامنة ومحدثة ✓",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "فحص السرعة ⚡",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            logoMenuTabs.forEach { tab ->
                                val isCurrent = selectedTab == tab
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            handleTabClick(tab)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                        .testTag("logo_menu_${tab.routeTag}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            width = if (isCurrent) 1.3.dp else 1.dp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.labelAr,
                                                tint = if (isCurrent) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tab.labelAr,
                                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (tab == MainAppTab.DEBTS && totalActiveDebts > 0.009) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFEF4444).copy(alpha = 0.14f),
                                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f))
                                                ) {
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.0f", totalActiveDebts)} $currencySymbol",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFEF4444),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            if (tab == MainAppTab.REORDER_ALERTS && lowStockProducts.isNotEmpty()) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFEF4444).copy(alpha = 0.16f),
                                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f))
                                                ) {
                                                    Text(
                                                        text = "${lowStockProducts.size} نواقص",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFEF4444),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = tab.subtitleAr,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "محدد",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showLogoMenu = false
                                        onOpenStaffManagement()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("logo_menu_firebase_staff_accounts"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ManageAccounts,
                                            contentDescription = "حسابات العمال والمدراء",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "حسابات العمال والمدراء",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (pendingStaffCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = semanticColors.debtAmber.copy(alpha = 0.18f),
                                                border = BorderStroke(1.dp, semanticColors.debtAmber)
                                            ) {
                                                Text(
                                                    text = "$pendingStaffCount معلق",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = semanticColors.debtAmber,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "الموافقة على دخول العمال وتحديد الصلاحيات",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showLogoMenu = false
                                        showApkTempModal = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("logo_menu_apk_temp_link"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = semanticColors.profitGreen.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, semanticColors.profitGreen.copy(alpha = 0.65f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Android,
                                            contentDescription = "تحميل APK برابط مؤقت",
                                            tint = semanticColors.profitGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تحميل التطبيق APK (رابط مؤقت Temp)",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = semanticColors.profitGreen
                                    )
                                    Text(
                                        text = "استخراج APK وتوليد رابط خارجي مؤقت + رمز QR",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showProRestrictedDialog) {
            AlertDialog(
                onDismissRequest = { showProRestrictedDialog = false },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp)) },
                title = { Text("الميزة مخصصة لحسابات Pro ⭐", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("عذراً، ميزة ($restrictedFeatureName - مثل تعدد الفروع والتقارير الموسعة) مخصصة فقط لحسابات Pro والنسخة السحابية المتكاملة.")
                        Text("أنت تستخدم حالياً باقة Lite المحلية. لفتح هذه الميزة ومزامنة الفروع، يمكنك الانتقال للإعدادات لطلب ترقية الحساب إلى Pro.")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showProRestrictedDialog = false
                            selectedTab = MainAppTab.SETTINGS
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("الانتقال للإعدادات لطلب الترقية 🚀", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProRestrictedDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Instant TopBar Quick Theme & Palette Studio Popover
        if (showQuickThemeMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (semanticColors.isDark) 0.42f else 0.24f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showQuickThemeMenu = false
                    }
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 10.dp,
                    shadowElevation = 18.dp,
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 60.dp, start = 12.dp, end = 12.dp)
                        .width(310.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* consume clicks inside popover */ }
                        .testTag("quick_theme_popover_card")
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "تخصيص المظهر الفوري",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${themeMode.labelAr} • ${themePalette.labelAr}",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable { viewModel.toggleDayNightQuickly() }
                                    .testTag("quick_theme_cycle_inside_popover_btn")
                            ) {
                                Text(
                                    text = "تبديل سريع ${themeMode.badgeEmoji}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "وضع الإضاءة (نهاري / ليلي / OLED):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppThemeMode.entries.forEach { mode ->
                                val isSelected = themeMode == mode
                                val modeIcon = when (mode) {
                                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                                    AppThemeMode.DARK -> Icons.Default.DarkMode
                                    AppThemeMode.OLED, AppThemeMode.OLED_BLACK -> Icons.Default.NightsStay
                                    AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.4.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setThemeMode(mode, showToastBanner = true)
                                        }
                                        .testTag("quick_theme_mode_${mode.name}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = mode.labelAr,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${mode.labelAr} ${mode.badgeEmoji}",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = mode.subtitleAr,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "السمة اللونية (6 سمات احترافية):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppThemePalette.entries.forEach { pal ->
                                val isSelected = themePalette == pal
                                val swatchColor = Color(pal.primaryHex)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) swatchColor.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) swatchColor else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.setThemePalette(pal, showToastBanner = true)
                                        }
                                        .testTag("quick_theme_palette_${pal.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                        )
                                        Text(
                                            text = pal.labelAr,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isSelected) swatchColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setHighContrastNumbers(!highContrastNumbers) }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contrast,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "تباين فائق للأرقام والأسعار",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "زيادة وضوح الحدود والأسعار تحت الإضاءة القوية",
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = highContrastNumbers,
                                onCheckedChange = { viewModel.setHighContrastNumbers(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showApkTempModal) {
        ApkDownloadModal(
            onDismiss = { showApkTempModal = false },
            onShowMessage = { msg -> viewModel.showStatusMessage(msg) }
        )
    }

    if (showFullScreenSyncStatus) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullScreenSyncStatus = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            SyncStatusScreen(
                viewModel = viewModel,
                onBack = { showFullScreenSyncStatus = false }
            )
        }
    }

    if (showPendingManagerRequestsModal) {
        val pendingAccounts = remember(allStaffAccounts) {
            allStaffAccounts.filter { it.approvalStatus == StaffApprovalStatus.PENDING || it.isPending }
        }
        val liteAccounts = remember(allStaffAccounts) {
            allStaffAccounts.filter { it.syncMode == "lite" }
        }
        val context = LocalContext.current
        val staffRepo = remember(context) { StaffAccountRepository(context.applicationContext) }
        PendingManagerRequestsDialog(
            pendingAccounts = pendingAccounts,
            liteAccounts = liteAccounts,
            repository = staffRepo,
            onDismiss = { showPendingManagerRequestsModal = false },
            onAccountApproved = { approvedAcc ->
                viewModel.showStatusMessage("تمت الموافقة بنجاح على المدير: ${approvedAcc.fullName}")
            },
            onAccountRejected = { rejectedAcc ->
                viewModel.showStatusMessage("تم رفض / حذف طلب المدير: ${rejectedAcc.fullName}")
            }
        )
    }

    val activeScheduledPreview = previewScheduledPayload
    if (activeScheduledPreview != null) {
        SalesReportPdfPreviewDialog(
            payload = activeScheduledPreview,
            onDismiss = { viewModel.closeArchivedScheduledReportPreview() }
        )
    }

    if (currentUser?.isTrialExpired == true || (myStaffAccount.isTrial && myStaffAccount.isTrialExpired)) {
        val effectiveManagerUser = currentUser ?: myStaffAccount.toLocalRoomUser()
        com.example.ui.components.TrialExpiredOverlay(
            currentUser = effectiveManagerUser,
            onRequestApprovalFromOwner = {
                viewModel.requestActivationForExpiredManager(effectiveManagerUser)
            },
            onRefreshStatus = {
                val searchKey = myStaffAccount.email.ifBlank { myStaffAccount.userId }
                viewModel.checkAndRefreshManagerStatus(searchKey)
            },
            onLogout = {
                onSignOutFirebase()
            }
        )
    }
    }
}

@Composable
private fun TopBarThemeQuickSwitcher(
    themeMode: AppThemeMode,
    themePalette: AppThemePalette,
    isDark: Boolean,
    isMenuOpen: Boolean,
    onQuickCycleMode: () -> Unit,
    onOpenThemeMenu: () -> Unit
) {
    val activeIcon = when (themeMode) {
        AppThemeMode.LIGHT -> Icons.Default.LightMode
        AppThemeMode.DARK -> Icons.Default.DarkMode
        AppThemeMode.OLED, AppThemeMode.OLED_BLACK -> Icons.Default.NightsStay
        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
    }
    val iconTint = when (themeMode) {
        AppThemeMode.LIGHT -> Color(0xFFD97706)
        AppThemeMode.DARK -> Color(0xFF38BDF8)
        AppThemeMode.OLED, AppThemeMode.OLED_BLACK -> Color(0xFFA855F7)
        AppThemeMode.SYSTEM -> MaterialTheme.colorScheme.primary
    }
    val paletteDotColor = Color(themePalette.primaryHex)

    Surface(
        shape = RoundedCornerShape(50),
        color = if (isMenuOpen) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isMenuOpen) {
                MaterialTheme.colorScheme.primary
            } else if (isDark) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)
            }
        ),
        modifier = Modifier
            .padding(end = 6.dp)
            .testTag("top_bar_theme_switcher")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
        ) {
            // 1-Tap Instant Mode Cycle Button (Day ⇄ Night ⇄ OLED)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = if (isDark) 0.14f else 0.16f), CircleShape)
                    .border(
                        width = 1.dp,
                        color = iconTint.copy(alpha = if (isDark) 0.5f else 0.75f),
                        shape = CircleShape
                    )
                    .clickable(onClick = onQuickCycleMode)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("top_bar_theme_instant_toggle_btn")
            ) {
                Icon(
                    imageVector = activeIcon,
                    contentDescription = "تبديل الوضع الليلي والنهاري (${themeMode.labelAr})",
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(1.dp)
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Palette & Full Theme Studio Popover Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenThemeMenu)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("top_bar_theme_popover_btn")
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(paletteDotColor)
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.45f),
                            shape = CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "فتح قائمة تخصيص المظهر والألوان",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun AFStoreHeaderLogo(
    isActive: Boolean,
    badgeSize: Dp = 42.dp,
    logoImagePath: String = "",
    logoCustomText: String = ""
) {
    val semantic = LocalAFStoreSemanticColors.current
    val bgColor = Color(0xFF111828)
    val borderColor = when {
        isActive -> Color(0xFF18E1F7)
        semantic.isDark -> Color(0xFF00B4D8)
        else -> Color(0xFF0284C7)
    }

    val resolvedLogoModel = remember(logoImagePath) {
        ProductImageStorageHelper.resolveImageModel(logoImagePath)
    }
    val cleanCustomText = logoCustomText.trim()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isActive) 1.8.dp else 1.3.dp,
            color = borderColor
        ),
        modifier = Modifier.size(badgeSize)
    ) {
        if (resolvedLogoModel != null) {
            AsyncImage(
                model = resolvedLogoModel,
                contentDescription = "شعار المتجر المخصص",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(11.dp))
            )
        } else if (cleanCustomText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cleanCustomText.take(10),
                    color = Color.White,
                    fontSize = if (cleanCustomText.length <= 4) 11.sp else 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 9.5.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Image(
                painter = painterResource(id = R.drawable.af_store_logo),
                contentDescription = "شعار Af Store الرسمي",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(11.dp))
            )
        }
    }
}
