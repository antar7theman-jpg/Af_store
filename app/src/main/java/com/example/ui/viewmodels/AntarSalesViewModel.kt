package com.example.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AntarRepository
import com.example.data.database.AntarDatabase
import com.example.data.models.ActivityLog
import com.example.data.models.ArchivedInvoice
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.InvoiceStyle
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoice
import com.example.data.models.PurchaseInvoiceItem
import com.example.data.models.RecordSyncState
import com.example.data.models.Sale
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.data.models.Promotion
import com.example.data.models.PromotionType
import com.example.data.repository.PromotionRepository
import com.example.data.models.Expense
import com.example.data.models.ExpenseCategory
import com.example.data.repository.ExpenseRepository
import com.example.data.models.DamagedItemRecord
import com.example.data.models.ExpiryStatus
import com.example.data.models.ProductExpiryRecord
import com.example.data.models.SpoilageReason
import com.example.data.repository.ExpiryAndWasteRepository
import com.example.R
import com.example.data.firebase.LiveInventorySyncManager
import com.example.data.firebase.StaffAccount
import com.example.data.firebase.StaffAccountRepository
import com.example.data.firebase.StaffApprovalStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import com.example.utils.ArchivedScheduledReport
import com.example.ui.screens.DashboardLiveSummary
import com.example.ui.screens.LiveDashboardAnalyticsEngine
import com.example.ui.screens.LowStockInAppAlertPayload
import com.example.ui.screens.LowStockNotificationManager
import com.example.ui.screens.SalesReportPdfPayload
import com.example.utils.ScheduledReportConfig
import com.example.utils.ScheduledSalesReportHelper
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import com.example.utils.BackupMetadata
import com.example.utils.BluetoothPrinterDeviceInfo
import com.example.utils.BluetoothThermalPrinterManager
import com.example.utils.DatabaseBackupHelper
import com.example.utils.LastScannedPosEvent
import com.example.utils.DesktopPrinterConfig
import com.example.utils.LocalBackupFileInfo
import com.example.utils.ThermalConnectionState
import com.example.utils.ThermalPrinterConfig
import com.example.utils.ThermalReceiptPrintPayload
import com.example.ui.screens.PreviewLineItem
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class CartItem(
    val product: Product,
    val quantity: Int = 1, // Total quantity in pieces (وحدات/قطع)
    val isCarton: Boolean = false,
    val customUnitPrice: Double = product.price,
    val customPriceCustomerName: String? = null,
    val hasItemDiscount: Boolean = false,
    val isItemDiscountPercent: Boolean = true,
    val itemDiscountValue: Double = 0.0,
    val hasItemTva: Boolean = false,
    val itemTvaPercent: Double = 19.0,
    val isItemDebt: Boolean = false,
    val customDebtAmount: Double? = null
) {
    val unitsPerCartonSafe: Int
        get() = product.unitsPerCarton.coerceAtLeast(1)

    val effectiveUnitsDeducted: Int
        get() = quantity.coerceAtLeast(1)

    val fullCartonsCount: Int
        get() = effectiveUnitsDeducted / unitsPerCartonSafe

    val looseUnitsCount: Int
        get() = effectiveUnitsDeducted % unitsPerCartonSafe

    val customCartonPrice: Double
        get() = customUnitPrice * unitsPerCartonSafe

    val isCustomPriceModified: Boolean
        get() = kotlin.math.abs(customUnitPrice - product.price) > 0.009

    val rawLineSubtotal: Double
        get() = effectiveUnitsDeducted * customUnitPrice

    val itemDiscountAmount: Double
        get() = if (!hasItemDiscount || itemDiscountValue <= 0.0) {
            0.0
        } else if (isItemDiscountPercent) {
            rawLineSubtotal * (itemDiscountValue.coerceIn(0.0, 100.0) / 100.0)
        } else {
            itemDiscountValue.coerceIn(0.0, rawLineSubtotal)
        }

    val subtotalAfterItemDiscount: Double
        get() = (rawLineSubtotal - itemDiscountAmount).coerceAtLeast(0.0)

    val itemTvaAmount: Double
        get() = if (hasItemTva && itemTvaPercent > 0.0) {
            subtotalAfterItemDiscount * (itemTvaPercent / 100.0)
        } else {
            0.0
        }

    val lineSubtotal: Double
        get() = subtotalAfterItemDiscount + itemTvaAmount

    val hasAnyDebt: Boolean
        get() = isItemDebt || (customDebtAmount != null && customDebtAmount > 0.0)

    val effectiveDebtAmount: Double
        get() = when {
            customDebtAmount != null -> customDebtAmount.coerceIn(0.0, lineSubtotal)
            isItemDebt -> lineSubtotal
            else -> 0.0
        }

    val remainingCashAmount: Double
        get() = (lineSubtotal - effectiveDebtAmount).coerceAtLeast(0.0)
}

class AntarSalesViewModel(
    application: Application,
    val repository: AntarRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("af_store_prefs", Context.MODE_PRIVATE)
    private val staffAccountRepository = StaffAccountRepository(application)
    val liveInventorySyncManager = LiveInventorySyncManager(application, viewModelScope)
    val promotionRepository = PromotionRepository(application)
    val promotions: StateFlow<List<Promotion>> = promotionRepository.promotions
    val expenseRepository = ExpenseRepository(application)
    val expenses: StateFlow<List<Expense>> = expenseRepository.expenses
    val expiryAndWasteRepository = ExpiryAndWasteRepository(application)
    val productExpiryMap: StateFlow<Map<Int, ProductExpiryRecord>> = expiryAndWasteRepository.expiryRecords
    val damagedItems: StateFlow<List<DamagedItemRecord>> = expiryAndWasteRepository.damagedItems
    private val _appliedCoupon = MutableStateFlow<Promotion?>(null)
    val appliedCoupon: StateFlow<Promotion?> = _appliedCoupon.asStateFlow()
    private val _promotionalDiscountAmount = MutableStateFlow(0.0)
    val promotionalDiscountAmount: StateFlow<Double> = _promotionalDiscountAmount.asStateFlow()
    private var activeStaffAccount: StaffAccount? = null

    val isLiveInventorySyncActive: StateFlow<Boolean> = liveInventorySyncManager.isLiveSyncActive
    val liveInventorySyncTimestamp: StateFlow<Long> = liveInventorySyncManager.lastSyncTimestamp

    private val _activeStaffAccount = MutableStateFlow<StaffAccount?>(null)
    val activeStaffAccountState: StateFlow<StaffAccount?> = _activeStaffAccount.asStateFlow()

    val canViewCostPrices: StateFlow<Boolean> = _activeStaffAccount
        .map { account -> account == null || account.isManagerOrAdmin || account.toWorkerPermissions().canViewCostPrices }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val canViewReports: StateFlow<Boolean> = _activeStaffAccount
        .map { account -> account == null || account.isManagerOrAdmin || account.toWorkerPermissions().canViewReports }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val canManageDebtsAndCustomers: StateFlow<Boolean> = _activeStaffAccount
        .map { account -> account == null || account.isManagerOrAdmin || account.toWorkerPermissions().canManageDebtsAndCustomers }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val canManagePurchaseInvoices: StateFlow<Boolean> = _activeStaffAccount
        .map { account -> account == null || account.isManagerOrAdmin || account.toWorkerPermissions().canManagePurchaseInvoices }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _myProUpgradeRequest = MutableStateFlow<com.example.data.firebase.ProUpgradeRequest?>(null)
    val myProUpgradeRequest: StateFlow<com.example.data.firebase.ProUpgradeRequest?> = _myProUpgradeRequest.asStateFlow()

    private val _allProUpgradeRequests = MutableStateFlow<List<com.example.data.firebase.ProUpgradeRequest>>(emptyList())
    val allProUpgradeRequests: StateFlow<List<com.example.data.firebase.ProUpgradeRequest>> = _allProUpgradeRequests.asStateFlow()

    // Reactive Data Streams from Room (kept warm with SharingStarted.Eagerly for 0ms tab switches)
    val products: StateFlow<List<Product>> = repository.allProductsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // In-memory O(1) Fast Lookup Maps for Ultra-Fast Barcode Scanning & Instant POS Lookups
    val productByBarcodeMap: StateFlow<Map<String, Product>> = products
        .map { list ->
            val map = HashMap<String, Product>(list.size * 3)
            list.forEach { p ->
                p.unitBarcodesList.forEach { code ->
                    map[code.trim().lowercase()] = p
                }
                p.cartonBarcodesList.forEach { code ->
                    map[code.trim().lowercase()] = p
                }
            }
            map
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val productByIdMap: StateFlow<Map<Int, Product>> = products
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val sales: StateFlow<List<Sale>> = repository.allSalesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val archivedInvoices: StateFlow<List<ArchivedInvoice>> = repository.archivedInvoicesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val preSaleOrders: StateFlow<List<ArchivedInvoice>> = repository.preSaleOrdersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pendingPreSalesCount: StateFlow<Int> = repository.pendingPreSaleOrdersCountFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val workerLocations: StateFlow<List<com.example.data.models.WorkerLocation>> = repository.allWorkerLocationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val reservedStockMap: StateFlow<Map<Int, Int>> = preSaleOrders
        .map { orders ->
            val pendingOrders = orders.filter { it.isPendingPreSale }
            val map = mutableMapOf<Int, Int>()
            pendingOrders.forEach { order ->
                order.items.forEach { sale ->
                    map[sale.productId] = (map[sale.productId] ?: 0) + sale.quantitySold
                }
            }
            map
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // Dedicated Pre-Sales Cart
    private val _preSaleCartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val preSaleCartItems: StateFlow<List<CartItem>> = _preSaleCartItems.asStateFlow()

    private val _selectedPreSaleCustomer = MutableStateFlow<Customer?>(null)
    val selectedPreSaleCustomer: StateFlow<Customer?> = _selectedPreSaleCustomer.asStateFlow()

    private val _preSaleNotes = MutableStateFlow("")
    val preSaleNotes: StateFlow<String> = _preSaleNotes.asStateFlow()

    private val _preSaleExpectedDeliveryDate = MutableStateFlow(System.currentTimeMillis() + 86400000L)
    val preSaleExpectedDeliveryDate: StateFlow<Long> = _preSaleExpectedDeliveryDate.asStateFlow()

    private val _lastCompletedPreSaleOrder = MutableStateFlow<ArchivedInvoice?>(null)
    val lastCompletedPreSaleOrder: StateFlow<ArchivedInvoice?> = _lastCompletedPreSaleOrder.asStateFlow()

    val customers: StateFlow<List<Customer>> = repository.allCustomersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customerTransactions: StateFlow<List<CustomerTransaction>> = repository.allCustomerTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val purchaseInvoices: StateFlow<List<PurchaseInvoice>> = repository.allPurchaseInvoicesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val users: StateFlow<List<User>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activityLogs: StateFlow<List<ActivityLog>> = repository.recentLogsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val offlineSyncManager = com.example.utils.OfflineSyncManager(
        context = application,
        repository = repository,
        coroutineScope = viewModelScope,
        liveInventorySyncManager = liveInventorySyncManager,
        activeAccountProvider = { activeStaffAccount }
    )

    val pendingSyncQueueCount: StateFlow<Int> = repository.pendingSyncQueueCountFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // App & Session State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val isCurrentUserWorker: StateFlow<Boolean> = combine(_activeStaffAccount, _currentUser) { staff, localUser ->
        when {
            staff != null -> !staff.isManagerOrAdmin && staff.role.equals("worker", ignoreCase = true)
            localUser != null -> localUser.role == UserRole.WORKER
            else -> false
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val pendingReturnRequests: StateFlow<List<ArchivedInvoice>> = combine(
        archivedInvoices,
        _activeStaffAccount,
        _currentUser
    ) { invoices, staff, localUser ->
        invoices.filter { inv ->
            if (!inv.isReturnRequested) return@filter false
            if (staff != null) {
                if (staff.isOwner) {
                    true
                } else if (staff.isManagerOrAdmin) {
                    val reqMgrUid = inv.returnRequestManagerUid
                    val reqMgrEmail = inv.returnRequestManagerEmail
                    reqMgrUid.isBlank() ||
                        reqMgrUid == staff.userId ||
                        (reqMgrEmail.isNotBlank() && reqMgrEmail.equals(staff.email, ignoreCase = true))
                } else {
                    val reqWorkerUid = inv.returnRequestWorkerUid
                    reqWorkerUid.isBlank() || reqWorkerUid == staff.userId
                }
            } else {
                localUser != null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val notifiedReturnRequestIds = mutableSetOf<String>()

    val currentWorkerAffiliatedManagerName: StateFlow<String> = combine(
        _activeStaffAccount,
        _currentUser,
        users
    ) { staff, localUser, allUsers ->
        val fromStaff = staff?.parentManagerName?.takeIf { it.isNotBlank() }
            ?: staff?.approvedByName?.takeIf { it.isNotBlank() }
            ?: staff?.parentManagerEmail?.takeIf { it.isNotBlank() }
        if (!fromStaff.isNullOrBlank()) {
            fromStaff
        } else {
            val managerUser = allUsers.find { it.id == localUser?.managerId }
                ?: allUsers.find { it.role == UserRole.MANAGER || it.role == UserRole.SUPER_ADMIN }
            managerUser?.fullName?.takeIf { it.isNotBlank() } ?: "المدير المسؤول"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "المدير المسؤول")

    private val _storeName = MutableStateFlow(prefs.getString("store_name", "AF store") ?: "AF store")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _storePhonesList = MutableStateFlow(loadStorePhonesListFromPrefs())
    val storePhonesList: StateFlow<List<String>> = _storePhonesList.asStateFlow()

    private val _storePhone = MutableStateFlow(
        formatPhoneNumbersForDisplay(_storePhonesList.value)
            .ifBlank { prefs.getString("store_phone", "") ?: "" }
    )
    val storePhone: StateFlow<String> = _storePhone.asStateFlow()

    private val _storeLogoImagePath = MutableStateFlow(prefs.getString("store_logo_image_path", "") ?: "")
    val storeLogoImagePath: StateFlow<String> = _storeLogoImagePath.asStateFlow()

    private val _storeLogoCustomText = MutableStateFlow(prefs.getString("store_logo_custom_text", "") ?: "")
    val storeLogoCustomText: StateFlow<String> = _storeLogoCustomText.asStateFlow()

    private val _receiptFooterText = MutableStateFlow(
        prefs.getString("receipt_footer_text", "شكراً لتعاملكم معنا ونسعد بزيارتكم دائماً")
            ?: "شكراً لتعاملكم معنا ونسعد بزيارتكم دائماً"
    )
    val receiptFooterText: StateFlow<String> = _receiptFooterText.asStateFlow()

    private val _developerContact = MutableStateFlow(
        prefs.getString("developer_contact", "0662931569") ?: "0662931569"
    )
    val developerContact: StateFlow<String> = _developerContact.asStateFlow()

    private val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "د.ج") ?: "د.ج")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _loyaltyGoalDefault = MutableStateFlow(prefs.getInt("store_loyalty_goal", 500))
    val loyaltyGoalDefault: StateFlow<Int> = _loyaltyGoalDefault.asStateFlow()

    private val _loyaltyRewardDefault = MutableStateFlow(
        try { prefs.getString("store_loyalty_reward_value", "50.0")?.toDoubleOrNull() ?: 50.0 } catch (_: Exception) { 50.0 }
    )
    val loyaltyRewardDefault: StateFlow<Double> = _loyaltyRewardDefault.asStateFlow()

    private val _loyaltySpendPerPoint = MutableStateFlow(
        try { prefs.getString("store_loyalty_spend_per_point", "100.0")?.toDoubleOrNull() ?: 100.0 } catch (_: Exception) { 100.0 }
    )
    val loyaltySpendPerPoint: StateFlow<Double> = _loyaltySpendPerPoint.asStateFlow()

    private val _loyaltyValidityDays = MutableStateFlow(prefs.getInt("store_loyalty_validity_days", 90))
    val loyaltyValidityDays: StateFlow<Int> = _loyaltyValidityDays.asStateFlow()

    private val _themeMode = MutableStateFlow(loadThemeModeFromPrefs())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _themePalette = MutableStateFlow(loadThemePaletteFromPrefs())
    val themePalette: StateFlow<AppThemePalette> = _themePalette.asStateFlow()

    private val _highContrastNumbers = MutableStateFlow(
        prefs.getBoolean("app_theme_high_contrast", false)
    )
    val highContrastNumbers: StateFlow<Boolean> = _highContrastNumbers.asStateFlow()

    private val _invoiceStyle = MutableStateFlow(InvoiceStyle.BON_DE_LIVRAISON)
    val invoiceStyle: StateFlow<InvoiceStyle> = _invoiceStyle.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    /**
     * حساب وتوليد رقم الفاتورة التسلسلي الجديد
     * - فواتير البيع العادي: #1-160726، #2-160726
     * - فواتير الطلب المسبق: Pre #5-160726
     * حيث الرقم 160726 يمثل اليوم الحالي (ddMMyy)
     * والرقم التسلسلي متتابع ومستمر بين البيع الآن والطلب المسبق وعبر الأيام.
     */
    fun peekNextInvoiceNumber(isPreSale: Boolean = false, timestamp: Long = System.currentTimeMillis()): String {
        val dateStr = SimpleDateFormat("ddMMyy", Locale.US).format(Date(timestamp))
        val currentCounter = getOrInitSequenceCounter()
        val nextSeq = (currentCounter + 1).coerceAtLeast(1)
        return if (isPreSale) "Pre #$nextSeq-$dateStr" else "#$nextSeq-$dateStr"
    }

    @Synchronized
    fun generateNextInvoiceNumber(isPreSale: Boolean = false, timestamp: Long = System.currentTimeMillis()): String {
        val dateStr = SimpleDateFormat("ddMMyy", Locale.US).format(Date(timestamp))
        val currentCounter = getOrInitSequenceCounter()
        val nextSeq = (currentCounter + 1).coerceAtLeast(1)
        prefs.edit().putInt("global_invoice_sequence_counter", nextSeq).apply()
        return if (isPreSale) "Pre #$nextSeq-$dateStr" else "#$nextSeq-$dateStr"
    }

    /**
     * حساب وتوليد رقم وصل السداد التسلسلي المتتابع مع الفواتير
     * الصيغة: Pay #2-220726 (حيث 2 هو الرقم التسلسلي، و220726 هو ddMMyy)
     */
    fun peekNextPaymentReceiptNumber(timestamp: Long = System.currentTimeMillis()): String {
        val dateStr = SimpleDateFormat("ddMMyy", Locale.US).format(Date(timestamp))
        val currentCounter = getOrInitSequenceCounter()
        val nextSeq = (currentCounter + 1).coerceAtLeast(1)
        return "Pay #$nextSeq-$dateStr"
    }

    @Synchronized
    fun generateNextPaymentReceiptNumber(timestamp: Long = System.currentTimeMillis()): String {
        val dateStr = SimpleDateFormat("ddMMyy", Locale.US).format(Date(timestamp))
        val currentCounter = getOrInitSequenceCounter()
        val nextSeq = (currentCounter + 1).coerceAtLeast(1)
        prefs.edit().putInt("global_invoice_sequence_counter", nextSeq).apply()
        return "Pay #$nextSeq-$dateStr"
    }

    private fun getOrInitSequenceCounter(): Int {
        val saved = prefs.getInt("global_invoice_sequence_counter", -1)
        // If saved is within realistic sequential invoice range (0..9999), use it
        if (saved in 0..9999) return saved

        return recalculateSequenceCounter()
    }

    fun recalculateSequenceCounter(): Int {
        val allInvoices = (archivedInvoices.value + preSaleOrders.value).distinctBy { it.invoiceId }
        var maxSeq = 0
        val strictRegex = """^(?:Pre\s+|Pay\s+)?#(\d{1,5})-\d{6}$""".toRegex()
        for (inv in allInvoices) {
            val trimmed = inv.invoiceId.trim()
            val match = strictRegex.find(trimmed)
            if (match != null) {
                val seqNum = match.groupValues[1].toIntOrNull() ?: 0
                if (seqNum in 1..9999 && seqNum > maxSeq) {
                    maxSeq = seqNum
                }
            }
        }
        val payRegex = """Pay\s+#(\d{1,5})-\d{6}""".toRegex()
        for (tx in customerTransactions.value) {
            val match = payRegex.find(tx.notes)
            if (match != null) {
                val seqNum = match.groupValues[1].toIntOrNull() ?: 0
                if (seqNum in 1..9999 && seqNum > maxSeq) {
                    maxSeq = seqNum
                }
            }
        }
        // If no invoices match the new format yet, take count of existing invoices (capped to 9999)
        if (maxSeq == 0 && allInvoices.isNotEmpty()) {
            maxSeq = allInvoices.size.coerceAtMost(9999)
        }
        prefs.edit().putInt("global_invoice_sequence_counter", maxSeq).apply()
        return maxSeq
    }

    // POS Cart & Checkout Review State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _lastAddedCartProductId = MutableStateFlow<Int?>(null)
    val lastAddedCartProductId: StateFlow<Int?> = _lastAddedCartProductId.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _isLoyaltyDiscountApplied = MutableStateFlow(false)
    val isLoyaltyDiscountApplied: StateFlow<Boolean> = _isLoyaltyDiscountApplied.asStateFlow()

    fun toggleLoyaltyDiscount() {
        val cust = _selectedCustomer.value ?: return
        if (isCustomerLoyaltyExpired(cust)) {
            showStatusMessage("تعذر تطبيق الخصم: نقاط ولاء هذا العميل منتهية الصلاحية وفق شرط المدة الزمنية (${_loyaltyValidityDays.value} يوم).")
            return
        }
        if (cust.loyaltyPoints >= cust.loyaltyGoal) {
            _isLoyaltyDiscountApplied.value = !_isLoyaltyDiscountApplied.value
        }
    }

    // Persistent Customer-Specific Product Prices (Key: "${customerId}_${productId}" -> UnitPrice)
    private val _customerCustomPrices = MutableStateFlow<Map<String, Double>>(loadCustomerCustomPricesFromPrefs())
    val customerCustomPrices: StateFlow<Map<String, Double>> = _customerCustomPrices.asStateFlow()

    // Low-Stock Reorder Alert Thresholds (Global Default + Optional Per-Product Custom Threshold)
    private val _defaultLowStockThreshold = MutableStateFlow(
        prefs.getInt("default_low_stock_threshold", 5).coerceAtLeast(1)
    )
    val defaultLowStockThreshold: StateFlow<Int> = _defaultLowStockThreshold.asStateFlow()

    private val _productCustomMinStock = MutableStateFlow<Map<Int, Int>>(loadProductCustomMinStockFromPrefs())
    val productCustomMinStock: StateFlow<Map<Int, Int>> = _productCustomMinStock.asStateFlow()

    private val _productCustomReorderQty = MutableStateFlow<Map<Int, Int>>(loadProductCustomReorderQtyFromPrefs())
    val productCustomReorderQty: StateFlow<Map<Int, Int>> = _productCustomReorderQty.asStateFlow()

    private val _mutedLowStockProductIds = MutableStateFlow<Set<Int>>(loadMutedLowStockProductIdsFromPrefs())
    val mutedLowStockProductIds: StateFlow<Set<Int>> = _mutedLowStockProductIds.asStateFlow()

    private val _lowStockNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean("low_stock_notifications_enabled", true)
    )
    val lowStockNotificationsEnabled: StateFlow<Boolean> = _lowStockNotificationsEnabled.asStateFlow()

    private val _activeLowStockInAppAlert = MutableStateFlow<LowStockInAppAlertPayload?>(null)
    val activeLowStockInAppAlert: StateFlow<LowStockInAppAlertPayload?> = _activeLowStockInAppAlert.asStateFlow()

    private val notifiedProductStateMap = mutableMapOf<Int, String>()
    private var hasInitializedStockMonitor = false

    // Pre-computed Low-Stock Products on Background Dispatcher
    val lowStockProducts: StateFlow<List<Product>> = combine(
        products,
        _defaultLowStockThreshold,
        _productCustomMinStock,
        _mutedLowStockProductIds
    ) { prodList, defaultThreshold, customMap, mutedSet ->
        prodList.filter { p ->
            if (mutedSet.contains(p.id)) {
                false
            } else {
                val minThreshold = customMap[p.id] ?: defaultThreshold
                p.stock <= minThreshold
            }
        }.sortedBy { it.stock }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Pre-computed Live Dashboard Summary on Background Dispatcher
    val dashboardLiveSummary: StateFlow<DashboardLiveSummary> = combine(
        sales,
        products,
        customers,
        _defaultLowStockThreshold,
        _productCustomMinStock
    ) { salesList, prodList, custList, defaultMin, customMap ->
        LiveDashboardAnalyticsEngine.buildLiveSummary(
            sales = salesList,
            products = prodList,
            customers = custList,
            defaultMinStock = defaultMin,
            customMinStockMap = customMap
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            LiveDashboardAnalyticsEngine.buildLiveSummary(
                sales = emptyList(),
                products = emptyList(),
                customers = emptyList(),
                defaultMinStock = _defaultLowStockThreshold.value,
                customMinStockMap = _productCustomMinStock.value
            )
        )

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _isInvoiceDiscountPercent = MutableStateFlow(true)
    val isInvoiceDiscountPercent: StateFlow<Boolean> = _isInvoiceDiscountPercent.asStateFlow()

    private val _invoiceDiscountValue = MutableStateFlow(0.0)
    val invoiceDiscountValue: StateFlow<Double> = _invoiceDiscountValue.asStateFlow()

    private val _isInvoiceTvaEnabled = MutableStateFlow(false)
    val isInvoiceTvaEnabled: StateFlow<Boolean> = _isInvoiceTvaEnabled.asStateFlow()

    private val _invoiceTvaPercent = MutableStateFlow(19.0)
    val invoiceTvaPercent: StateFlow<Double> = _invoiceTvaPercent.asStateFlow()

    private val _paymentMethod = MutableStateFlow("CASH") // CASH, DEBT, TRANSFER
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _lastCompletedInvoice = MutableStateFlow<ArchivedInvoice?>(null)
    val lastCompletedInvoice: StateFlow<ArchivedInvoice?> = _lastCompletedInvoice.asStateFlow()

    private val _unregisteredScannedBarcode = MutableStateFlow<String?>(null)
    val unregisteredScannedBarcode: StateFlow<String?> = _unregisteredScannedBarcode.asStateFlow()

    private val _lastScannedPosEvent = MutableStateFlow<LastScannedPosEvent?>(null)
    val lastScannedPosEvent: StateFlow<LastScannedPosEvent?> = _lastScannedPosEvent.asStateFlow()

    // Local Database Backups State
    private val _localBackups = MutableStateFlow<List<LocalBackupFileInfo>>(emptyList())
    val localBackups: StateFlow<List<LocalBackupFileInfo>> = _localBackups.asStateFlow()

    private val _encryptedBackups = MutableStateFlow<List<com.example.data.backup.BackupMetadata>>(emptyList())
    val encryptedBackups: StateFlow<List<com.example.data.backup.BackupMetadata>> = _encryptedBackups.asStateFlow()

    private val _isLocalBackupBusy = MutableStateFlow(false)
    val isLocalBackupBusy: StateFlow<Boolean> = _isLocalBackupBusy.asStateFlow()

    private val _isCloudBackupBusy = MutableStateFlow(false)
    val isCloudBackupBusy: StateFlow<Boolean> = _isCloudBackupBusy.asStateFlow()

    private val _lastLocalBackupTime = MutableStateFlow(prefs.getLong("last_local_backup_time", 0L))
    val lastLocalBackupTime: StateFlow<Long> = _lastLocalBackupTime.asStateFlow()

    // Scheduled Sales Summary PDF Reports State & Archive
    private val _scheduledReportConfig = MutableStateFlow(ScheduledSalesReportHelper.loadConfig(prefs))
    val scheduledReportConfig: StateFlow<ScheduledReportConfig> = _scheduledReportConfig.asStateFlow()

    private val _scheduledReportsArchive = MutableStateFlow(ScheduledSalesReportHelper.loadArchive(prefs))
    val scheduledReportsArchive: StateFlow<List<ArchivedScheduledReport>> = _scheduledReportsArchive.asStateFlow()

    private val _latestUnreadScheduledReport = MutableStateFlow<ArchivedScheduledReport?>(
        _scheduledReportsArchive.value.firstOrNull { !it.isNotificationRead }
    )
    val latestUnreadScheduledReport: StateFlow<ArchivedScheduledReport?> = _latestUnreadScheduledReport.asStateFlow()

    private val _previewingScheduledReportPayload = MutableStateFlow<SalesReportPdfPayload?>(null)
    val previewingScheduledReportPayload: StateFlow<SalesReportPdfPayload?> = _previewingScheduledReportPayload.asStateFlow()

    // Bluetooth Thermal Printer Engine & Configuration State
    val bluetoothPrinterManager = BluetoothThermalPrinterManager(application.applicationContext)

    private val _thermalPrinterConfig = MutableStateFlow(ThermalPrinterConfig.loadFromPrefs(prefs))
    val thermalPrinterConfig: StateFlow<ThermalPrinterConfig> = _thermalPrinterConfig.asStateFlow()

    private val _desktopPrinterConfig = MutableStateFlow(DesktopPrinterConfig.loadFromPrefs(prefs))
    val desktopPrinterConfig: StateFlow<DesktopPrinterConfig> = _desktopPrinterConfig.asStateFlow()

    val thermalConnectionState: StateFlow<ThermalConnectionState> = bluetoothPrinterManager.connectionState
    val isThermalPrinting: StateFlow<Boolean> = bluetoothPrinterManager.isPrinting

    init {
        viewModelScope.launch {
            val db = AntarDatabase.getDatabase(application)
            AntarDatabase.seedInitialDataIfEmpty(db.storeDao(), forceAddMissing = true)
            val allUsers = repository.getAllUsersOnce()
            if (_currentUser.value == null && allUsers.isNotEmpty()) {
                _currentUser.value = allUsers.first()
            }
            refreshLocalBackups()
            // Auto-reconnect to last paired Bluetooth thermal printer if enabled
            val cfg = _thermalPrinterConfig.value
            if (cfg.autoReconnect && cfg.lastPrinterAddress.isNotBlank() && bluetoothPrinterManager.hasBluetoothConnectPermission()) {
                bluetoothPrinterManager.connectToPrinter(cfg.lastPrinterAddress, cfg.lastPrinterName)
            }
            checkAndRunScheduledSalesReportIfDue()
        }

        viewModelScope.launch {
            promotions.collectLatest {
                recalculateDiscountAmount()
            }
        }

        // Keep local sequence counter aligned if remote invoices or payment receipts are pulled
        viewModelScope.launch(Dispatchers.IO) {
            combine(archivedInvoices, preSaleOrders, customerTransactions) { reg, pre, txs ->
                Triple(reg, pre, txs)
            }.collectLatest { (reg, pre, txs) ->
                val allInvoices = (reg + pre).distinctBy { it.invoiceId }
                val saved = prefs.getInt("global_invoice_sequence_counter", -1)
                var maxSeq = if (saved in 0..9999) saved else 0
                val strictRegex = """^(?:Pre\s+|Pay\s+)?#(\d{1,5})-\d{6}$""".toRegex()
                for (inv in allInvoices) {
                    val trimmed = inv.invoiceId.trim()
                    val match = strictRegex.find(trimmed)
                    if (match != null) {
                        val seqNum = match.groupValues[1].toIntOrNull() ?: 0
                        if (seqNum in 1..9999 && seqNum > maxSeq) {
                            maxSeq = seqNum
                        }
                    }
                }
                val payRegex = """Pay\s+#(\d{1,5})-\d{6}""".toRegex()
                for (tx in txs) {
                    val match = payRegex.find(tx.notes)
                    if (match != null) {
                        val seqNum = match.groupValues[1].toIntOrNull() ?: 0
                        if (seqNum in 1..9999 && seqNum > maxSeq) {
                            maxSeq = seqNum
                        }
                    }
                }
                if (maxSeq > saved || saved > 9999) {
                    prefs.edit().putInt("global_invoice_sequence_counter", maxSeq).apply()
                }
            }
        }

        LowStockNotificationManager.ensureNotificationChannel(application.applicationContext)

        // Monitor low-stock transitions and dispatch instant system + in-app notifications
        viewModelScope.launch {
            lowStockProducts.collect { currentLowList ->
                val currentLowIds = currentLowList.map { it.id }.toSet()
                // Clear notified state for products that were restocked above threshold
                val recoveredIds = notifiedProductStateMap.keys.filter { !currentLowIds.contains(it) }
                recoveredIds.forEach { notifiedProductStateMap.remove(it) }

                if (!hasInitializedStockMonitor) {
                    if (products.value.isNotEmpty()) {
                        hasInitializedStockMonitor = true
                        currentLowList.forEach { prod ->
                            notifiedProductStateMap[prod.id] = if (prod.stock <= 0) "OUT" else "LOW"
                        }
                    }
                    return@collect
                }

                val newlyTriggered = mutableListOf<Product>()
                currentLowList.forEach { prod ->
                    val newState = if (prod.stock <= 0) "OUT" else "LOW"
                    val prevState = notifiedProductStateMap[prod.id]
                    if (prevState == null || (prevState == "LOW" && newState == "OUT")) {
                        newlyTriggered.add(prod)
                    }
                    notifiedProductStateMap[prod.id] = newState
                }

                if (newlyTriggered.isNotEmpty()) {
                    dispatchLowStockAlertNotification(
                        newlyLowProducts = newlyTriggered,
                        allLowProducts = currentLowList
                    )
                }
            }
        }

        // Monitor incoming worker invoice return requests for the affiliated manager
        viewModelScope.launch {
            combine(pendingReturnRequests, isCurrentUserWorker) { reqs, isWorker ->
                reqs to isWorker
            }.collect { (reqs, isWorker) ->
                val activeIds = reqs.map { it.invoiceId }.toSet()
                notifiedReturnRequestIds.retainAll(activeIds)
                if (!isWorker && reqs.isNotEmpty()) {
                    val newlyRequested = reqs.filter { !notifiedReturnRequestIds.contains(it.invoiceId) }
                    newlyRequested.forEach { inv ->
                        notifiedReturnRequestIds.add(inv.invoiceId)
                        val subtotalStr = String.format(Locale.US, "%.2f", inv.subtotal)
                        LowStockNotificationManager.postWorkerReturnRequestSystemNotification(
                            context = application.applicationContext,
                            invoiceId = inv.invoiceId,
                            customerName = inv.customerName,
                            workerName = inv.returnRequestWorkerName,
                            subtotalFormatted = subtotalStr,
                            currencySymbol = _currencySymbol.value,
                            reason = inv.returnRequestReason
                        )
                        showStatusMessage(
                            "🔔 طلب مرتجع جديد من العامل (${inv.returnRequestWorkerName}): الفاتورة ${inv.invoiceId} بقيمة $subtotalStr ${_currencySymbol.value} بانتظار موافقتك"
                        )
                    }
                }
            }
        }

        // Background ticker while app is active to automatically trigger at 09:00 PM (or custom scheduled time)
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                checkAndRunScheduledSalesReportIfDue()
            }
        }
    }

    fun seedSampleCatalogAndCustomers(force: Boolean = true) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val db = AntarDatabase.getDatabase(getApplication<Application>())
            AntarDatabase.seedInitialDataIfEmpty(db.storeDao(), forceAddMissing = force)
            showStatusMessage("تم تجهيز السلع والعملاء الافتراضيين بنجاح للتجربة ✓")
        }
    }

    fun showStatusMessage(message: String) {
        _statusBannerMessage.value = message
    }

    fun clearStatusMessage() {
        _statusBannerMessage.value = null
    }

    fun loginAsUser(username: String, passwordPlain: String): Boolean {
        val matched = users.value.find {
            it.username.equals(username.trim(), ignoreCase = true) && it.passwordHash == passwordPlain.trim()
        }
        return if (matched != null) {
            _currentUser.value = matched
            _storeName.value = matched.storeName.ifBlank { "AF store" }
            viewModelScope.launch {
                repository.logActivity(matched, "LOGIN", "تسجيل دخول ناجح باسم ${matched.fullName}")
            }
            true
        } else {
            false
        }
    }

    fun switchActiveRole(user: User) {
        _currentUser.value = user
    }

    fun syncAuthenticatedFirebaseStaffAccount(account: StaffAccount) {
        activeStaffAccount = account
        _activeStaffAccount.value = account
        val localUser = account.toLocalRoomUser().copy(
            storeName = account.branchName.ifBlank { _storeName.value }
        )
        val prevUser = _currentUser.value
        if (prevUser?.username != localUser.username ||
            prevUser.role != localUser.role ||
            prevUser.permissionsJson != localUser.permissionsJson ||
            prevUser.fullName != localUser.fullName
        ) {
            _currentUser.value = localUser
        }

        // Start real-time Firestore sync between manager and affiliated workers (Products, Invoices, Purchases, GPS)
        liveInventorySyncManager.startLiveInventorySync(account) { statusMsg ->
            showStatusMessage(statusMsg)
        }
        liveInventorySyncManager.startLiveInvoicesSync(account) { statusMsg ->
            showStatusMessage(statusMsg)
        }
        liveInventorySyncManager.startLivePurchaseInvoicesSync(account) { statusMsg ->
            showStatusMessage(statusMsg)
        }
        liveInventorySyncManager.startLiveWorkerLocationsSync(account)
        liveInventorySyncManager.startLiveDamagedGoodsSync(account, expiryAndWasteRepository)
        offlineSyncManager.triggerManualSync()

        // Observe Pro upgrade requests
        if (account.isOwner) {
            viewModelScope.launch {
                staffAccountRepository.observeAllProUpgradeRequests().collect { list ->
                    _allProUpgradeRequests.value = list
                }
            }
        }
        if (account.userId.isNotBlank()) {
            viewModelScope.launch {
                staffAccountRepository.observeMyProUpgradeRequest(account.userId).collect { req ->
                    _myProUpgradeRequest.value = req
                }
            }
        }
    }

    fun saveOwnerCreatedManagerToRoom(account: StaffAccount, secretPin: String = "1234") {
        viewModelScope.launch {
            val allLocalUsers = repository.getAllUsersOnce()
            val existing = allLocalUsers.find {
                it.username.equals(account.email, ignoreCase = true) ||
                    it.fullName.equals(account.fullName, ignoreCase = true)
            }
            val effectivePin = secretPin.trim().ifBlank { account.secretPin.ifBlank { "1234" } }
            val isManagerProfile = account.role == "manager"
            val parentManagerLocalId = if (isManagerProfile) {
                1
            } else {
                allLocalUsers.find {
                    (account.parentManagerEmail.isNotBlank() && it.username.equals(account.parentManagerEmail, ignoreCase = true)) ||
                        (account.parentManagerName.isNotBlank() && it.fullName.equals(account.parentManagerName, ignoreCase = true))
                }?.id ?: 1
            }
            val userToSave = User(
                id = existing?.id ?: 0,
                username = account.email.ifBlank { account.fullName },
                passwordHash = effectivePin,
                role = if (isManagerProfile) UserRole.MANAGER else UserRole.WORKER,
                fullName = account.fullName,
                phone = account.phone,
                storeName = account.branchName.ifBlank { _storeName.value },
                managerId = parentManagerLocalId,
                isActive = account.isApproved,
                permissionsJson = account.toWorkerPermissions().toJsonString()
            )
            repository.saveUser(userToSave)
            repository.logActivity(
                user = _currentUser.value,
                actionType = if (isManagerProfile) "CREATE_INDEPENDENT_MANAGER" else "CREATE_AFFILIATED_WORKER",
                description = if (isManagerProfile) {
                    "تم إنشاء ملف مدير مستقل: ${account.fullName} (${account.branchName.ifBlank { account.email }})"
                } else {
                    "قام المدير (${account.parentManagerName.ifBlank { "الإدارة" }}) بإنشاء حساب عامل تابع: ${account.fullName} (${account.email})"
                }
            )
        }
    }

    private fun loadThemeModeFromPrefs(): AppThemeMode {
        val migratedToDaylightDefault = prefs.getBoolean("theme_v2_daylight_initialized", false)
        if (!migratedToDaylightDefault) {
            prefs.edit()
                .putString("app_theme_mode", AppThemeMode.LIGHT.name)
                .putBoolean("theme_v2_daylight_initialized", true)
                .apply()
            return AppThemeMode.LIGHT
        }
        val raw = prefs.getString("app_theme_mode", AppThemeMode.LIGHT.name) ?: AppThemeMode.LIGHT.name
        return AppThemeMode.entries.find { it.name == raw } ?: AppThemeMode.LIGHT
    }

    private fun loadThemePaletteFromPrefs(): AppThemePalette {
        val raw = prefs.getString("app_theme_palette", AppThemePalette.EMERALD_CLOUD.name)
            ?: AppThemePalette.EMERALD_CLOUD.name
        return AppThemePalette.entries.find { it.name == raw } ?: AppThemePalette.EMERALD_CLOUD
    }

    fun setThemeMode(mode: AppThemeMode, showToastBanner: Boolean = false) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.name).apply()
        if (showToastBanner) {
            showStatusMessage("تم تفعيل ${mode.labelAr} ${mode.badgeEmoji}")
        }
    }

    fun setThemePalette(palette: AppThemePalette, showToastBanner: Boolean = false) {
        _themePalette.value = palette
        prefs.edit().putString("app_theme_palette", palette.name).apply()
        if (showToastBanner) {
            showStatusMessage("تم تطبيق سمة الألوان: ${palette.labelAr} 🎨")
        }
    }

    fun setHighContrastNumbers(enabled: Boolean) {
        _highContrastNumbers.value = enabled
        prefs.edit().putBoolean("app_theme_high_contrast", enabled).apply()
    }

    fun toggleDayNightQuickly() {
        val nextMode = when (_themeMode.value) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.OLED_BLACK
            AppThemeMode.OLED, AppThemeMode.OLED_BLACK -> AppThemeMode.LIGHT
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
        }
        setThemeMode(nextMode, showToastBanner = true)
    }

    fun setInvoiceStyle(style: InvoiceStyle) {
        _invoiceStyle.value = style
    }

    private fun parsePhoneNumbersList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|", ";", "،", ",", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun formatPhoneNumbersForDisplay(phones: List<String>): String {
        return phones
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(" | ")
    }

    private fun loadStorePhonesListFromPrefs(): List<String> {
        val rawV1 = prefs.getString("store_phones_v1", "") ?: ""
        if (rawV1.isNotBlank()) {
            val parsed = parsePhoneNumbersList(rawV1)
            if (parsed.isNotEmpty()) return parsed
        }
        val legacyPhone = prefs.getString("store_phone", "") ?: ""
        return parsePhoneNumbersList(legacyPhone)
    }

    fun updateStoreName(newName: String) {
        if (newName.isNotBlank()) {
            val trimmed = newName.trim()
            _storeName.value = trimmed
            prefs.edit().putString("store_name", trimmed).apply()
        }
    }

    fun updateStorePhonesList(phones: List<String>) {
        val cleanedList = phones
            .flatMap { parsePhoneNumbersList(it) }
            .distinct()
        val joined = formatPhoneNumbersForDisplay(cleanedList)
        _storePhonesList.value = cleanedList
        _storePhone.value = joined
        prefs.edit()
            .putString("store_phones_v1", cleanedList.joinToString(";"))
            .putString("store_phone", joined)
            .apply()
    }

    fun updateStoreLogo(imagePath: String, customText: String) {
        val cleanPath = imagePath.trim()
        val cleanText = customText.trim()
        _storeLogoImagePath.value = cleanPath
        _storeLogoCustomText.value = cleanText
        prefs.edit()
            .putString("store_logo_image_path", cleanPath)
            .putString("store_logo_custom_text", cleanText)
            .apply()
    }

    fun updateStoreIdentitySettings(
        newStoreName: String,
        phones: List<String>,
        logoImagePath: String = _storeLogoImagePath.value,
        logoCustomText: String = _storeLogoCustomText.value
    ) {
        val cleanName = newStoreName.trim().ifBlank { "AF store" }
        val cleanedPhones = phones
            .flatMap { parsePhoneNumbersList(it) }
            .distinct()
        val joinedPhones = formatPhoneNumbersForDisplay(cleanedPhones)
        val cleanLogoPath = logoImagePath.trim()
        val cleanLogoText = logoCustomText.trim()

        _storeName.value = cleanName
        _storePhonesList.value = cleanedPhones
        _storePhone.value = joinedPhones
        _storeLogoImagePath.value = cleanLogoPath
        _storeLogoCustomText.value = cleanLogoText

        prefs.edit()
            .putString("store_name", cleanName)
            .putString("store_phones_v1", cleanedPhones.joinToString(";"))
            .putString("store_phone", joinedPhones)
            .putString("store_logo_image_path", cleanLogoPath)
            .putString("store_logo_custom_text", cleanLogoText)
            .apply()
    }

    fun updateReceiptHeaderSettings(
        newStoreName: String,
        newStorePhone: String,
        newFooterText: String,
        newDeveloperContact: String,
        newStorePhonesList: List<String>? = null,
        newLogoImagePath: String? = null,
        newLogoCustomText: String? = null
    ) {
        val cleanName = newStoreName.trim().ifBlank { "AF store" }
        val resolvedPhones = (newStorePhonesList ?: parsePhoneNumbersList(newStorePhone))
            .flatMap { parsePhoneNumbersList(it) }
            .distinct()
        val cleanPhone = formatPhoneNumbersForDisplay(resolvedPhones)
        val cleanFooter = newFooterText.trim().ifBlank { "شكراً لتعاملكم معنا ونسعد بزيارتكم دائماً" }
        val cleanDev = newDeveloperContact.trim().ifBlank { "0662931569" }
        val cleanLogoPath = (newLogoImagePath ?: _storeLogoImagePath.value).trim()
        val cleanLogoText = (newLogoCustomText ?: _storeLogoCustomText.value).trim()

        _storeName.value = cleanName
        _storePhonesList.value = resolvedPhones
        _storePhone.value = cleanPhone
        _receiptFooterText.value = cleanFooter
        _developerContact.value = cleanDev
        _storeLogoImagePath.value = cleanLogoPath
        _storeLogoCustomText.value = cleanLogoText

        prefs.edit()
            .putString("store_name", cleanName)
            .putString("store_phones_v1", resolvedPhones.joinToString(";"))
            .putString("store_phone", cleanPhone)
            .putString("receipt_footer_text", cleanFooter)
            .putString("developer_contact", cleanDev)
            .putString("store_logo_image_path", cleanLogoPath)
            .putString("store_logo_custom_text", cleanLogoText)
            .apply()
    }

    fun setCurrencySymbol(symbol: String) {
        if (symbol.isNotBlank()) {
            val trimmed = symbol.trim()
            _currencySymbol.value = trimmed
            prefs.edit().putString("currency_symbol", trimmed).apply()
        }
    }

    private fun loadCustomerCustomPricesFromPrefs(): Map<String, Double> {
        val raw = prefs.getString("customer_custom_prices_v1", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<String, Double>()
        raw.split(";").forEach { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                val key = parts[0].trim()
                val price = parts[1].toDoubleOrNull()
                if (key.isNotEmpty() && price != null && price >= 0.0) {
                    result[key] = price
                }
            }
        }
        return result
    }

    private fun persistCustomerCustomPrices(map: Map<String, Double>) {
        val serialized = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit().putString("customer_custom_prices_v1", serialized).apply()
    }

    fun getCustomerProductPrice(customerId: Int, productId: Int): Double? {
        return _customerCustomPrices.value["${customerId}_${productId}"]
    }

    fun saveCustomerProductPrice(customerId: Int, productId: Int, unitPrice: Double) {
        val updated = _customerCustomPrices.value.toMutableMap()
        updated["${customerId}_${productId}"] = unitPrice.coerceAtLeast(0.0)
        _customerCustomPrices.value = updated
        persistCustomerCustomPrices(updated)
    }

    fun removeCustomerProductPrice(customerId: Int, productId: Int) {
        val updated = _customerCustomPrices.value.toMutableMap()
        if (updated.remove("${customerId}_${productId}") != null) {
            _customerCustomPrices.value = updated
            persistCustomerCustomPrices(updated)
        }
    }

    private fun loadProductCustomMinStockFromPrefs(): Map<Int, Int> {
        val raw = prefs.getString("product_min_stock_v1", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, Int>()
        raw.split(";").forEach { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                val prodId = parts[0].trim().toIntOrNull()
                val minVal = parts[1].trim().toIntOrNull()
                if (prodId != null && minVal != null && minVal >= 0) {
                    result[prodId] = minVal
                }
            }
        }
        return result
    }

    private fun persistProductCustomMinStock(map: Map<Int, Int>) {
        val serialized = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit().putString("product_min_stock_v1", serialized).apply()
    }

    fun updateDefaultLowStockThreshold(newThreshold: Int) {
        val safe = newThreshold.coerceAtLeast(1)
        _defaultLowStockThreshold.value = safe
        prefs.edit().putInt("default_low_stock_threshold", safe).apply()
        showStatusMessage("تم تحديث الحد الأدنى الافتراضي للطلب إلى $safe وحدة")
    }

    fun getCustomProductMinStock(productId: Int): Int? {
        return _productCustomMinStock.value[productId]
    }

    fun getEffectiveMinStockThreshold(productId: Int): Int {
        return _productCustomMinStock.value[productId] ?: _defaultLowStockThreshold.value
    }

    fun isProductLowStock(product: Product): Boolean {
        return product.stock <= getEffectiveMinStockThreshold(product.id)
    }

    fun setCustomProductMinStock(productId: Int, customThreshold: Int?) {
        val updated = _productCustomMinStock.value.toMutableMap()
        if (customThreshold == null) {
            updated.remove(productId)
        } else {
            updated[productId] = customThreshold.coerceAtLeast(0)
        }
        _productCustomMinStock.value = updated
        persistProductCustomMinStock(updated)
    }

    private fun loadProductCustomReorderQtyFromPrefs(): Map<Int, Int> {
        val raw = prefs.getString("product_reorder_qty_v1", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, Int>()
        raw.split(";").forEach { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) {
                val prodId = parts[0].trim().toIntOrNull()
                val qtyVal = parts[1].trim().toIntOrNull()
                if (prodId != null && qtyVal != null && qtyVal >= 1) {
                    result[prodId] = qtyVal
                }
            }
        }
        return result
    }

    private fun persistProductCustomReorderQty(map: Map<Int, Int>) {
        val serialized = map.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit().putString("product_reorder_qty_v1", serialized).apply()
    }

    private fun loadMutedLowStockProductIdsFromPrefs(): Set<Int> {
        val raw = prefs.getString("muted_low_stock_products_v1", "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(";")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    private fun persistMutedLowStockProductIds(ids: Set<Int>) {
        val serialized = ids.joinToString(";")
        prefs.edit().putString("muted_low_stock_products_v1", serialized).apply()
    }

    fun getEffectiveReorderQuantity(product: Product): Int {
        val customQty = _productCustomReorderQty.value[product.id]
        if (customQty != null && customQty >= 1) return customQty
        val minReq = getEffectiveMinStockThreshold(product.id)
        return ((minReq * 2) - product.stock).coerceAtLeast(product.unitsPerCarton.coerceAtLeast(1))
    }

    fun saveProductAlertRule(
        productId: Int,
        customMinStock: Int?,
        customReorderQty: Int?,
        muted: Boolean
    ) {
        val updatedMin = _productCustomMinStock.value.toMutableMap()
        if (customMinStock == null) {
            updatedMin.remove(productId)
        } else {
            updatedMin[productId] = customMinStock.coerceAtLeast(0)
        }
        _productCustomMinStock.value = updatedMin
        persistProductCustomMinStock(updatedMin)

        val updatedQty = _productCustomReorderQty.value.toMutableMap()
        if (customReorderQty == null) {
            updatedQty.remove(productId)
        } else {
            updatedQty[productId] = customReorderQty.coerceAtLeast(1)
        }
        _productCustomReorderQty.value = updatedQty
        persistProductCustomReorderQty(updatedQty)

        val updatedMuted = _mutedLowStockProductIds.value.toMutableSet()
        if (muted) {
            updatedMuted.add(productId)
        } else {
            updatedMuted.remove(productId)
        }
        _mutedLowStockProductIds.value = updatedMuted
        persistMutedLowStockProductIds(updatedMuted)

        showStatusMessage("تم حفظ إعدادات تنبيه الطلب للصنف بنجاح ✅")
    }

    fun unmuteAllProductAlerts() {
        _mutedLowStockProductIds.value = emptySet()
        persistMutedLowStockProductIds(emptySet())
        showStatusMessage("تم تفعيل التنبيهات لجميع الأصناف")
    }

    fun setLowStockNotificationsEnabled(enabled: Boolean) {
        _lowStockNotificationsEnabled.value = enabled
        prefs.edit().putBoolean("low_stock_notifications_enabled", enabled).apply()
        showStatusMessage(
            if (enabled) "تم تفعيل إشعارات انخفاض المخزون الفورية ✅"
            else "تم إيقاف إشعارات نظام أندرويد لانخفاض المخزون"
        )
    }

    fun dismissLowStockInAppAlert() {
        _activeLowStockInAppAlert.value = null
    }

    private fun dispatchLowStockAlertNotification(
        newlyLowProducts: List<Product>,
        allLowProducts: List<Product>
    ) {
        if (newlyLowProducts.isEmpty() && allLowProducts.isEmpty()) return
        val effectiveList = if (newlyLowProducts.isNotEmpty()) newlyLowProducts else allLowProducts
        val totalLow = allLowProducts.size.coerceAtLeast(effectiveList.size)
        val outOfStockCount = allLowProducts.count { it.stock <= 0 }

        val previewNames = effectiveList.take(3).joinToString(" ، ") { prod ->
            val minReq = getEffectiveMinStockThreshold(prod.id)
            if (prod.stock <= 0) "${prod.name} (نفد 0)" else "${prod.name} (${prod.stock}/$minReq)"
        }

        val alertPayload = LowStockInAppAlertPayload(
            id = "LOW_STOCK_${System.currentTimeMillis()}",
            titleAr = if (outOfStockCount > 0) {
                "⚠️ تنبيه مخزون عاجل: $totalLow أصناف وصلت للحد الأدنى ($outOfStockCount نفدت)"
            } else {
                "📦 تنبيه إعادة الطلب: $totalLow أصناف انخفضت للحد الأدنى"
            },
            summaryAr = "الأصناف المتأثرة: $previewNames",
            affectedProductNames = effectiveList.map { it.name },
            totalLowCount = totalLow,
            outOfStockCount = outOfStockCount
        )
        _activeLowStockInAppAlert.value = alertPayload

        if (_lowStockNotificationsEnabled.value) {
            LowStockNotificationManager.postLowStockSystemNotification(
                context = getApplication(),
                storeName = _storeName.value,
                newlyLowProducts = effectiveList,
                totalLowStockCount = totalLow,
                outOfStockCount = outOfStockCount,
                getMinThreshold = { prodId -> getEffectiveMinStockThreshold(prodId) }
            )
        }
    }

    fun triggerTestLowStockNotification() {
        val currentLow = lowStockProducts.value
        val sampleProducts = if (currentLow.isNotEmpty()) {
            currentLow
        } else {
            val firstProd = products.value.firstOrNull()
            if (firstProd != null) {
                listOf(firstProd.copy(stock = getEffectiveMinStockThreshold(firstProd.id)))
            } else {
                listOf(
                    Product(
                        id = 9999,
                        name = "صنف تجريبي للتنبيه",
                        stock = 2,
                        price = 100.0,
                        purchasePrice = 80.0
                    )
                )
            }
        }
        dispatchLowStockAlertNotification(
            newlyLowProducts = sampleProducts,
            allLowProducts = sampleProducts
        )
        showStatusMessage("تم إرسال إشعار تنبيه المخزون بنجاح 🔔")
    }

    // Cart Management (Unified in pieces so cartons & single units combine seamlessly as in the screenshot)
    fun addToCart(product: Product, asCarton: Boolean = false) {
        val addedPieces = if (asCarton) product.unitsPerCarton.coerceAtLeast(1) else 1
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        val activeCust = _selectedCustomer.value
        val savedCustPrice = activeCust?.let { getCustomerProductPrice(it.id, product.id) }

        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                quantity = item.quantity + addedPieces,
                isCarton = item.isCarton || asCarton
            )
        } else {
            current.add(
                CartItem(
                    product = product,
                    quantity = addedPieces,
                    isCarton = asCarton,
                    customUnitPrice = savedCustPrice ?: product.price,
                    customPriceCustomerName = if (savedCustPrice != null) activeCust.name else null,
                    isItemDebt = _paymentMethod.value == "DEBT"
                )
            )
        }
        _lastAddedCartProductId.value = product.id
        _cartItems.value = current
        recalculateDiscountAmount()
    }

    fun addToCartWithQuantity(
        product: Product,
        totalUnitsToAdd: Int,
        asCarton: Boolean = false,
        markAsDebt: Boolean = false
    ) {
        val safeUnits = totalUnitsToAdd.coerceAtLeast(1)
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        val activeCust = _selectedCustomer.value
        val savedCustPrice = activeCust?.let { getCustomerProductPrice(it.id, product.id) }

        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                quantity = item.quantity + safeUnits,
                isCarton = item.isCarton || asCarton,
                isItemDebt = item.isItemDebt || markAsDebt || _paymentMethod.value == "DEBT"
            )
        } else {
            current.add(
                CartItem(
                    product = product,
                    quantity = safeUnits,
                    isCarton = asCarton,
                    customUnitPrice = savedCustPrice ?: product.price,
                    customPriceCustomerName = if (savedCustPrice != null) activeCust.name else null,
                    isItemDebt = markAsDebt || _paymentMethod.value == "DEBT"
                )
            )
        }
        _lastAddedCartProductId.value = product.id
        _cartItems.value = current
        recalculateDiscountAmount()
        showStatusMessage("تمت إضافة $safeUnits ق من ${product.name} إلى سلة المبيعات")
    }

    fun updateCartItemQuantity(productId: Int, isCarton: Boolean, newQuantity: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQuantity)
            }
            _cartItems.value = current
            recalculateDiscountAmount()
        }
    }

    fun adjustCartItemUnits(productId: Int, deltaUnits: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val updatedQty = current[index].quantity + deltaUnits
            if (updatedQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = updatedQty)
            }
            _cartItems.value = current
            recalculateDiscountAmount()
        }
    }

    fun removeCartItem(productId: Int) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == productId }
        if (_lastAddedCartProductId.value == productId) {
            _lastAddedCartProductId.value = null
        }
        if (_lastScannedPosEvent.value?.productId == productId) {
            _lastScannedPosEvent.value = null
        }
        recalculateDiscountAmount()
    }

    fun dismissLastScannedPosEvent() {
        _lastScannedPosEvent.value = null
    }

    fun switchCartItemPackageMode(productId: Int, targetCartonMode: Boolean? = null) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index < 0) return
        val item = current[index]
        val packUnits = item.product.unitsPerCarton.coerceAtLeast(1)
        val nextIsCarton = targetCartonMode ?: !item.isCarton
        val nextQty = if (nextIsCarton) {
            if (item.quantity < packUnits) packUnits else item.quantity
        } else {
            if (item.quantity == packUnits && item.isCarton) 1 else item.quantity.coerceAtLeast(1)
        }
        current[index] = item.copy(
            quantity = nextQty,
            isCarton = nextIsCarton
        )
        _cartItems.value = current
        recalculateDiscountAmount()
        _lastScannedPosEvent.value?.let { lastEv ->
            if (lastEv.productId == productId) {
                _lastScannedPosEvent.value = lastEv.copy(
                    isCartonScan = nextIsCarton,
                    addedUnitsInLastScan = if (nextIsCarton) packUnits else 1,
                    eventTimestampMillis = System.currentTimeMillis()
                )
            }
        }
    }

    fun toggleCartItemDiscount(productId: Int) {
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.id == productId) {
                val nextState = !item.hasItemDiscount
                item.copy(
                    hasItemDiscount = nextState,
                    itemDiscountValue = if (nextState && item.itemDiscountValue <= 0.0) 5.0 else if (!nextState) 0.0 else item.itemDiscountValue
                )
            } else item
        }
        recalculateDiscountAmount()
    }

    fun updateCartItemDiscount(productId: Int, isPercent: Boolean, value: Double) {
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.id == productId) {
                item.copy(
                    hasItemDiscount = value > 0.0,
                    isItemDiscountPercent = isPercent,
                    itemDiscountValue = value.coerceAtLeast(0.0)
                )
            } else item
        }
        recalculateDiscountAmount()
    }

    fun updateCartItemSellingPrice(
        productId: Int,
        newUnitPrice: Double,
        saveToCatalog: Boolean = false,
        forCustomer: Customer? = null,
        clearSavedCustomerPriceFor: Customer? = null
    ) {
        val safeUnitPrice = newUnitPrice.coerceAtLeast(0.0)
        var updatedCatalogProduct: Product? = null

        if (clearSavedCustomerPriceFor != null) {
            removeCustomerProductPrice(clearSavedCustomerPriceFor.id, productId)
        } else if (forCustomer != null) {
            saveCustomerProductPrice(forCustomer.id, productId, safeUnitPrice)
            if (_selectedCustomer.value?.id != forCustomer.id) {
                selectCustomerForPos(forCustomer)
            }
        }

        val activeCustomerAfter = _selectedCustomer.value
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.id == productId) {
                val nextProduct = if (saveToCatalog) {
                    item.product.copy(price = safeUnitPrice)
                } else {
                    item.product
                }
                if (saveToCatalog) {
                    updatedCatalogProduct = nextProduct
                }
                val savedCustPriceNow = activeCustomerAfter?.let {
                    getCustomerProductPrice(it.id, productId)
                }
                item.copy(
                    product = nextProduct,
                    customUnitPrice = safeUnitPrice,
                    customPriceCustomerName = when {
                        forCustomer != null -> forCustomer.name
                        savedCustPriceNow != null && kotlin.math.abs(savedCustPriceNow - safeUnitPrice) < 0.009 -> activeCustomerAfter.name
                        else -> null
                    }
                )
            } else item
        }
        recalculateDiscountAmount()
        updatedCatalogProduct?.let { prod ->
            saveProduct(prod)
        }
    }

    fun toggleCartItemTva(productId: Int) {
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.id == productId) {
                item.copy(hasItemTva = !item.hasItemTva)
            } else item
        }
        recalculateDiscountAmount()
    }

    fun updateCartItemTvaPercent(productId: Int, percent: Double) {
        _cartItems.value = _cartItems.value.map { item ->
            if (item.product.id == productId) {
                item.copy(
                    hasItemTva = percent > 0.0,
                    itemTvaPercent = percent.coerceIn(0.0, 100.0)
                )
            } else item
        }
        recalculateDiscountAmount()
    }

    fun setCartItemDebt(productId: Int, isDebt: Boolean, customAmount: Double? = null) {
        val updated = _cartItems.value.map { item ->
            if (item.product.id == productId) {
                if (!isDebt) {
                    item.copy(isItemDebt = false, customDebtAmount = null)
                } else if (customAmount != null) {
                    val safeDebt = customAmount.coerceIn(0.0, item.lineSubtotal)
                    if (safeDebt <= 0.0) {
                        item.copy(isItemDebt = false, customDebtAmount = null)
                    } else if (kotlin.math.abs(safeDebt - item.lineSubtotal) < 0.01) {
                        item.copy(isItemDebt = true, customDebtAmount = null)
                    } else {
                        item.copy(isItemDebt = false, customDebtAmount = safeDebt)
                    }
                } else {
                    item.copy(isItemDebt = true, customDebtAmount = null)
                }
            } else item
        }
        _cartItems.value = updated
        val allFullDebt = updated.isNotEmpty() && updated.all { it.isItemDebt && it.customDebtAmount == null }
        if (allFullDebt) {
            _paymentMethod.value = "DEBT"
        } else if (_paymentMethod.value == "DEBT" && updated.any { !it.isItemDebt || it.customDebtAmount != null }) {
            _paymentMethod.value = "CASH"
        }
    }

    fun toggleCartItemDebt(productId: Int) {
        val item = _cartItems.value.find { it.product.id == productId } ?: return
        if (item.hasAnyDebt) {
            setCartItemDebt(productId, isDebt = false, customAmount = null)
        } else {
            setCartItemDebt(productId, isDebt = true, customAmount = null)
        }
    }

    fun setInvoiceDiscountMode(isPercent: Boolean) {
        _isInvoiceDiscountPercent.value = isPercent
        recalculateDiscountAmount()
    }

    fun setInvoiceDiscountInput(value: Double) {
        _invoiceDiscountValue.value = value.coerceAtLeast(0.0)
        recalculateDiscountAmount()
    }

    fun setInvoiceTvaEnabled(enabled: Boolean) {
        _isInvoiceTvaEnabled.value = enabled
    }

    fun setInvoiceTvaRate(percent: Double) {
        _invoiceTvaPercent.value = percent.coerceIn(0.0, 100.0)
    }

    private fun recalculateDiscountAmount() {
        val itemsSubtotal = _cartItems.value.sumOf { it.lineSubtotal }
        val rawVal = _invoiceDiscountValue.value
        val manualDiscount = if (rawVal <= 0.0) {
            0.0
        } else if (_isInvoiceDiscountPercent.value) {
            itemsSubtotal * (rawVal.coerceIn(0.0, 100.0) / 100.0)
        } else {
            rawVal.coerceIn(0.0, itemsSubtotal)
        }
        val promoDiscount = calculatePromotionDiscountAmount(itemsSubtotal)
        _promotionalDiscountAmount.value = promoDiscount
        _discountAmount.value = (manualDiscount + promoDiscount).coerceIn(0.0, itemsSubtotal)
    }

    fun calculateInvoiceSubtotal(): Double {
        return _cartItems.value.sumOf { it.lineSubtotal }
    }

    fun calculateInvoiceDiscountAmount(): Double {
        return _discountAmount.value
    }

    fun calculatePromotionDiscountAmount(itemsSubtotal: Double = calculateInvoiceSubtotal()): Double {
        var totalPromoSavings = 0.0
        val items = _cartItems.value

        // 1. Automatic Quantity Tier & BOGO Promotions from active non-coupon promotions
        val activeAutoPromotions = promotions.value.filter { it.isActive && !it.isCoupon && !it.isExpired }
        for (promo in activeAutoPromotions) {
            when (promo.type) {
                PromotionType.QUANTITY_TIER_DISCOUNT -> {
                    for (item in items) {
                        if (promo.targetProductId == null || promo.targetProductId == item.product.id) {
                            if (item.effectiveUnitsDeducted >= promo.minQuantity && promo.minQuantity > 0) {
                                val tierSaving = item.rawLineSubtotal * (promo.discountValue.coerceIn(0.0, 100.0) / 100.0)
                                totalPromoSavings += tierSaving
                            }
                        }
                    }
                }
                PromotionType.BUY_X_GET_Y_FREE -> {
                    for (item in items) {
                        if (promo.targetProductId == null || promo.targetProductId == item.product.id) {
                            val cycle = promo.minQuantity + promo.rewardFreeQuantity
                            if (cycle > 0 && item.effectiveUnitsDeducted >= cycle) {
                                val freeUnitsCount = (item.effectiveUnitsDeducted / cycle) * promo.rewardFreeQuantity
                                val freeSavings = freeUnitsCount * item.customUnitPrice
                                totalPromoSavings += freeSavings
                            }
                        }
                    }
                }
                else -> { /* other types applied via coupons or direct selection */ }
            }
        }

        // 2. Applied Coupon (if any)
        val coupon = _appliedCoupon.value
        if (coupon != null && coupon.isActive && !coupon.isExpired) {
            if (coupon.minCartTotal == null || itemsSubtotal >= coupon.minCartTotal) {
                when (coupon.type) {
                    PromotionType.PERCENTAGE_DISCOUNT -> {
                        val saving = itemsSubtotal * (coupon.discountValue.coerceIn(0.0, 100.0) / 100.0)
                        totalPromoSavings += saving
                    }
                    PromotionType.FIXED_AMOUNT_DISCOUNT -> {
                        totalPromoSavings += coupon.discountValue.coerceIn(0.0, itemsSubtotal)
                    }
                    PromotionType.QUANTITY_TIER_DISCOUNT -> {
                        for (item in items) {
                            if (coupon.targetProductId == null || coupon.targetProductId == item.product.id) {
                                if (item.effectiveUnitsDeducted >= coupon.minQuantity && coupon.minQuantity > 0) {
                                    val tierSaving = item.rawLineSubtotal * (coupon.discountValue.coerceIn(0.0, 100.0) / 100.0)
                                    totalPromoSavings += tierSaving
                                }
                            }
                        }
                    }
                    PromotionType.BUY_X_GET_Y_FREE -> {
                        for (item in items) {
                            if (coupon.targetProductId == null || coupon.targetProductId == item.product.id) {
                                val cycle = coupon.minQuantity + coupon.rewardFreeQuantity
                                if (cycle > 0 && item.effectiveUnitsDeducted >= cycle) {
                                    val freeUnitsCount = (item.effectiveUnitsDeducted / cycle) * coupon.rewardFreeQuantity
                                    val freeSavings = freeUnitsCount * item.customUnitPrice
                                    totalPromoSavings += freeSavings
                                }
                            }
                        }
                    }
                }
            }
        }

        return totalPromoSavings.coerceIn(0.0, itemsSubtotal)
    }

    fun applyCouponCode(code: String): Result<Promotion> {
        val trimmed = code.trim().uppercase()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال رمز الكوبون"))
        }
        val promo = promotionRepository.findCouponByCode(trimmed)
            ?: return Result.failure(IllegalArgumentException("رمز الكوبون ($trimmed) غير موجود أو غير صالح"))

        if (!promo.isActive) {
            return Result.failure(IllegalStateException("هذا الكوبون غير مفعّل حالياً"))
        }
        if (promo.isExpired) {
            return Result.failure(IllegalStateException("انتهت صلاحية هذا الكوبون"))
        }
        if (promo.maxUses != null && promo.usedCount >= promo.maxUses) {
            return Result.failure(IllegalStateException("تم استنفاد الحد الأقصى لاستخدام هذا الكوبون (${promo.maxUses} مرة)"))
        }

        val cartSubtotal = calculateInvoiceSubtotal()
        if (promo.minCartTotal != null && cartSubtotal < promo.minCartTotal) {
            return Result.failure(IllegalStateException("الحد الأدنى للفاتورة للاستفادة من الكوبون هو ${promo.minCartTotal} د.ج (المجموع الحالي: $cartSubtotal د.ج)"))
        }

        _appliedCoupon.value = promo
        recalculateDiscountAmount()
        showStatusMessage("✓ تم تطبيق كود الخصم: ${promo.code} بنجاح (${promo.formattedSummary()})")
        return Result.success(promo)
    }

    fun applyPromotionDirectly(promo: Promotion) {
        if (!promo.isActive || promo.isExpired) {
            showStatusMessage("العرض الترويجي غير متاح أو منتهي الصلاحية")
            return
        }
        val cartSubtotal = calculateInvoiceSubtotal()
        if (promo.minCartTotal != null && cartSubtotal < promo.minCartTotal) {
            showStatusMessage("الحد الأدنى للفاتورة للاستفادة هو ${promo.minCartTotal} د.ج")
            return
        }
        _appliedCoupon.value = promo
        recalculateDiscountAmount()
        showStatusMessage("✓ تم تفعيل العرض: ${promo.title}")
    }

    fun removeAppliedCoupon() {
        val prev = _appliedCoupon.value
        _appliedCoupon.value = null
        recalculateDiscountAmount()
        if (prev != null) {
            showStatusMessage("تمت إزالة كود الخصم: ${prev.code ?: prev.title}")
        }
    }

    fun calculateInvoiceTvaAmount(): Double {
        if (!_isInvoiceTvaEnabled.value) return 0.0
        val afterDiscount = (calculateInvoiceSubtotal() - calculateInvoiceDiscountAmount()).coerceAtLeast(0.0)
        return afterDiscount * (_invoiceTvaPercent.value.coerceAtLeast(0.0) / 100.0)
    }

    fun calculateInvoiceFinalTotal(): Double {
        val afterDiscount = (calculateInvoiceSubtotal() - calculateInvoiceDiscountAmount()).coerceAtLeast(0.0)
        val withTva = afterDiscount + calculateInvoiceTvaAmount()
        val customer = _selectedCustomer.value
        return if (_isLoyaltyDiscountApplied.value && customer != null && customer.loyaltyPoints >= customer.loyaltyGoal && !isCustomerLoyaltyExpired(customer)) {
            (withTva - customer.loyaltyRewardValue).coerceAtLeast(0.0)
        } else {
            withTva
        }
    }

    fun buildPreviewInvoice(): ArchivedInvoice {
        val now = System.currentTimeMillis()
        val customer = _selectedCustomer.value
        val method = _paymentMethod.value
        val items = _cartItems.value
        val subtotal = calculateInvoiceSubtotal()
        val finalTotal = calculateInvoiceFinalTotal()
        val ratio = if (subtotal > 0.0) finalTotal / subtotal else 1.0
        val hasAnyDebt = method == "DEBT" || items.any { it.hasAnyDebt }

        val previewSales = items.mapIndexed { idx, cartItem ->
            val itemNet = cartItem.lineSubtotal * ratio
            val itemDebtPortion = if (method == "DEBT") {
                itemNet
            } else if (cartItem.customDebtAmount != null) {
                (cartItem.customDebtAmount.coerceIn(0.0, cartItem.lineSubtotal)) * ratio
            } else if (cartItem.isItemDebt) {
                itemNet
            } else {
                0.0
            }
            val isDebtLine = method == "DEBT" || cartItem.hasAnyDebt
            val linePaymentMethod = if (method == "DEBT" || (cartItem.isItemDebt && cartItem.customDebtAmount == null)) {
                "DEBT"
            } else if (itemDebtPortion > 0.0) {
                "PARTIAL_DEBT"
            } else {
                method
            }
            Sale(
                saleId = idx + 1,
                productId = cartItem.product.id,
                productName = buildCartItemReceiptTitle(cartItem),
                quantitySold = cartItem.effectiveUnitsDeducted,
                totalPrice = itemNet,
                saleDate = now,
                customerId = customer?.id,
                customerName = customer?.name ?: "عميل نقدي",
                isDebt = isDebtLine,
                paymentMethod = linePaymentMethod
            )
        }

        return ArchivedInvoice(
            invoiceId = peekNextInvoiceNumber(isPreSale = false, timestamp = now),
            dateMillis = now,
            customerName = customer?.name ?: "عميل نقدي",
            customerId = customer?.id,
            paymentMethod = if (method == "DEBT") "DEBT" else if (hasAnyDebt) "PARTIAL_DEBT" else method,
            isDebt = hasAnyDebt,
            items = previewSales,
            subtotal = finalTotal,
            totalQuantity = previewSales.sumOf { it.quantitySold }
        )
    }

    private fun buildCartItemReceiptTitle(cartItem: CartItem): String {
        val tags = mutableListOf<String>()
        if (cartItem.hasItemDiscount && cartItem.itemDiscountValue > 0.0) {
            val dLabel = if (cartItem.isItemDiscountPercent) "${cartItem.itemDiscountValue.toInt()}%" else "${cartItem.itemDiscountValue}"
            tags.add("خصم $dLabel")
        }
        if (cartItem.hasItemTva) {
            tags.add("TVA ${cartItem.itemTvaPercent.toInt()}%")
        }
        if (cartItem.customDebtAmount != null && cartItem.customDebtAmount > 0.0 && _paymentMethod.value != "DEBT") {
            tags.add("دين: ${String.format(Locale.US, "%.0f", cartItem.customDebtAmount)} د.ج")
        } else if (cartItem.isItemDebt && _paymentMethod.value != "DEBT") {
            tags.add("دين")
        }
        return if (tags.isEmpty()) {
            cartItem.product.name
        } else {
            "${cartItem.product.name} (${tags.joinToString(" • ")})"
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _lastAddedCartProductId.value = null
        _lastScannedPosEvent.value = null
        _discountAmount.value = 0.0
        _invoiceDiscountValue.value = 0.0
        _isInvoiceDiscountPercent.value = true
        _isInvoiceTvaEnabled.value = false
        _selectedCustomer.value = null
        _paymentMethod.value = "CASH"
        _isLoyaltyDiscountApplied.value = false
        _appliedCoupon.value = null
        _promotionalDiscountAmount.value = 0.0
    }

    fun selectCustomerForPos(customer: Customer?) {
        _selectedCustomer.value = customer
        _isLoyaltyDiscountApplied.value = false
        var appliedCustomCount = 0
        _cartItems.value = _cartItems.value.map { item ->
            if (customer != null) {
                val savedPrice = getCustomerProductPrice(customer.id, item.product.id)
                if (savedPrice != null) {
                    appliedCustomCount++
                    item.copy(
                        customUnitPrice = savedPrice,
                        customPriceCustomerName = customer.name
                    )
                } else {
                    item.copy(
                        customUnitPrice = item.product.price,
                        customPriceCustomerName = null
                    )
                }
            } else {
                item.copy(
                    customUnitPrice = item.product.price,
                    customPriceCustomerName = null
                )
            }
        }
        recalculateDiscountAmount()
        if (customer != null && appliedCustomCount > 0) {
            showStatusMessage("تم تطبيق الأسعار الخاصة للزبون: ${customer.name} ($appliedCustomCount صنف)")
        }
    }

    fun setPosDiscount(discount: Double) {
        _isInvoiceDiscountPercent.value = false
        _invoiceDiscountValue.value = discount.coerceAtLeast(0.0)
        recalculateDiscountAmount()
    }

    fun setPosPaymentMethod(method: String) {
        _paymentMethod.value = method
        if (method == "DEBT") {
            _cartItems.value = _cartItems.value.map { it.copy(isItemDebt = true, customDebtAmount = null) }
        } else {
            _cartItems.value = _cartItems.value.map { it.copy(isItemDebt = false, customDebtAmount = null) }
        }
    }

    fun promptUnregisteredBarcode(barcode: String) {
        val code = barcode.trim()
        if (code.isNotEmpty()) {
            _unregisteredScannedBarcode.value = code
        }
    }

    fun dismissUnregisteredBarcodePrompt() {
        _unregisteredScannedBarcode.value = null
    }

    fun scanBarcodeAndAddToCart(
        barcode: String,
        formatLabel: String = "BARCODE",
        decodeTimeMs: Long = 0L
    ) {
        val code = barcode.trim()
        if (code.isEmpty()) return
        val lowerCode = code.lowercase()
        val inMemoryMatch = productByBarcodeMap.value[lowerCode]
        if (inMemoryMatch != null) {
            val isCartonScan = inMemoryMatch.matchesCartonBarcode(code)
            val addedUnits = if (isCartonScan) inMemoryMatch.unitsPerCarton.coerceAtLeast(1) else 1
            addToCart(inMemoryMatch, asCarton = isCartonScan)
            _lastScannedPosEvent.value = LastScannedPosEvent(
                productId = inMemoryMatch.id,
                productName = inMemoryMatch.name,
                scannedBarcode = code,
                formatLabel = formatLabel,
                decodeTimeMs = decodeTimeMs,
                isCartonScan = isCartonScan,
                unitsPerCarton = inMemoryMatch.unitsPerCarton.coerceAtLeast(1),
                addedUnitsInLastScan = addedUnits,
                unitPrice = inMemoryMatch.price,
                eventTimestampMillis = System.currentTimeMillis()
            )
            val packLabel = if (isCartonScan) "كرتونة (${addedUnits} ق)" else "قطعة"
            showStatusMessage("✓ أضيفت ${inMemoryMatch.name} ($packLabel) إلى الفاتورة")
            return
        }
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                repository.findProductByBarcode(code)
            }
            if (found != null) {
                val isCartonScan = found.matchesCartonBarcode(code)
                val addedUnits = if (isCartonScan) found.unitsPerCarton.coerceAtLeast(1) else 1
                addToCart(found, asCarton = isCartonScan)
                _lastScannedPosEvent.value = LastScannedPosEvent(
                    productId = found.id,
                    productName = found.name,
                    scannedBarcode = code,
                    formatLabel = formatLabel,
                    decodeTimeMs = decodeTimeMs,
                    isCartonScan = isCartonScan,
                    unitsPerCarton = found.unitsPerCarton.coerceAtLeast(1),
                    addedUnitsInLastScan = addedUnits,
                    unitPrice = found.price,
                    eventTimestampMillis = System.currentTimeMillis()
                )
                val packLabel = if (isCartonScan) "كرتونة (${addedUnits} ق)" else "قطعة"
                showStatusMessage("✓ أضيفت ${found.name} ($packLabel) إلى الفاتورة")
            } else {
                _unregisteredScannedBarcode.value = code
                showStatusMessage("الباركود ($code) غير مسجل — عرّفه الآن ليُضاف للفاتورة فوراً")
            }
        }
    }

    fun onLiveCameraBarcodeScanned(
        rawCode: String,
        formatLabel: String = "BARCODE",
        decodeTimeMs: Long = 0L
    ): Boolean {
        val code = rawCode.trim()
        if (code.isEmpty()) return false
        val found = productByBarcodeMap.value[code.lowercase()]
        return if (found != null) {
            val isCartonScan = found.matchesCartonBarcode(code)
            val addedUnits = if (isCartonScan) found.unitsPerCarton.coerceAtLeast(1) else 1
            addToCart(found, asCarton = isCartonScan)
            _lastScannedPosEvent.value = LastScannedPosEvent(
                productId = found.id,
                productName = found.name,
                scannedBarcode = code,
                formatLabel = formatLabel,
                decodeTimeMs = decodeTimeMs,
                isCartonScan = isCartonScan,
                unitsPerCarton = found.unitsPerCarton.coerceAtLeast(1),
                addedUnitsInLastScan = addedUnits,
                unitPrice = found.price,
                eventTimestampMillis = System.currentTimeMillis()
            )
            val packLabel = if (isCartonScan) "كرتونة (${addedUnits} ق)" else "قطعة"
            showStatusMessage("✓ أضيفت ${found.name} ($packLabel) إلى الفاتورة")
            true
        } else {
            scanBarcodeAndAddToCart(code, formatLabel, decodeTimeMs)
            false
        }
    }

    fun repeatLastScannedItem() {
        val lastEv = _lastScannedPosEvent.value ?: return
        val prod = productByIdMap.value[lastEv.productId] ?: products.value.find { it.id == lastEv.productId } ?: return
        val addedUnits = if (lastEv.isCartonScan) prod.unitsPerCarton.coerceAtLeast(1) else 1
        addToCart(prod, asCarton = lastEv.isCartonScan)
        _lastScannedPosEvent.value = lastEv.copy(
            addedUnitsInLastScan = addedUnits,
            eventTimestampMillis = System.currentTimeMillis()
        )
    }

    fun linkBarcodeToExistingProduct(
        product: Product,
        barcode: String,
        asCartonBarcode: Boolean = false,
        addToCartImmediately: Boolean = true
    ) {
        val cleanCode = barcode.trim()
        if (cleanCode.isEmpty()) return
        viewModelScope.launch {
            val updatedProduct = if (asCartonBarcode) {
                product.withAddedCartonBarcode(cleanCode)
            } else {
                product.withAddedUnitBarcode(cleanCode)
            }
            repository.saveProduct(updatedProduct)
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushProductToCloud(updatedProduct, activeStaffAccount)
            }
            val barcodeTypeLabel = if (asCartonBarcode) "باركود كرتونة/عبوة إضافي" else "باركود قطعة إضافي"
            repository.logActivity(
                user = _currentUser.value,
                actionType = "BARCODE_LINKED",
                description = "إضافة وربط $barcodeTypeLabel ($cleanCode) بالسلعة: ${product.name}"
            )
            _unregisteredScannedBarcode.value = null
            if (addToCartImmediately) {
                val addedUnits = if (asCartonBarcode) updatedProduct.unitsPerCarton.coerceAtLeast(1) else 1
                addToCart(updatedProduct, asCarton = asCartonBarcode)
                _lastScannedPosEvent.value = LastScannedPosEvent(
                    productId = updatedProduct.id,
                    productName = updatedProduct.name,
                    scannedBarcode = cleanCode,
                    formatLabel = "LINKED",
                    decodeTimeMs = 0L,
                    isCartonScan = asCartonBarcode,
                    unitsPerCarton = updatedProduct.unitsPerCarton.coerceAtLeast(1),
                    addedUnitsInLastScan = addedUnits,
                    unitPrice = updatedProduct.price,
                    eventTimestampMillis = System.currentTimeMillis()
                )
                showStatusMessage("تم ربط الباركود ($cleanCode) بـ (${updatedProduct.name}) وإضافتها للفاتورة فوراً ✅")
            } else {
                showStatusMessage("تم ربط الباركود ($cleanCode) بالسلعة (${product.name}) وحفظه في المخزن ✅")
            }
        }
    }

    fun quickCreateProductAndAddToCart(
        name: String,
        unitPrice: Double,
        purchasePrice: Double = 0.0,
        barcode: String,
        asCartonBarcode: Boolean = false,
        unitsPerCarton: Int = 1,
        initialStockUnits: Int = 50,
        quantityToAddInCart: Int = 1
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            val safeUnitsPerCarton = unitsPerCarton.coerceAtLeast(1)
            val cleanCode = barcode.trim()
            val safeCartUnits = if (asCartonBarcode) {
                quantityToAddInCart.coerceAtLeast(1) * safeUnitsPerCarton
            } else {
                quantityToAddInCart.coerceAtLeast(1)
            }
            val safeStock = initialStockUnits.coerceAtLeast(safeCartUnits)
            val newProduct = Product(
                id = 0,
                name = cleanName,
                barcode = if (asCartonBarcode) "" else cleanCode,
                cartonBarcode = if (asCartonBarcode) cleanCode else "",
                price = unitPrice.coerceAtLeast(0.0),
                purchasePrice = purchasePrice.coerceAtLeast(0.0),
                stock = safeStock,
                unitsPerCarton = safeUnitsPerCarton
            )
            val insertedId = repository.saveProduct(newProduct).toInt()
            val savedProduct = if (insertedId > 0) {
                newProduct.copy(id = insertedId)
            } else {
                repository.findProductByBarcode(cleanCode) ?: newProduct
            }
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushProductToCloud(savedProduct, activeStaffAccount)
            }
            repository.logActivity(
                user = _currentUser.value,
                actionType = "PRODUCT_QUICK_POS_CREATED",
                description = "تعريف سريع للصنف (${savedProduct.name}) بالباركود ($cleanCode) وإضافته للفاتورة"
            )
            _unregisteredScannedBarcode.value = null
            if (savedProduct.id > 0) {
                addToCartWithQuantity(
                    product = savedProduct,
                    totalUnitsToAdd = safeCartUnits,
                    asCarton = asCartonBarcode
                )
                _lastScannedPosEvent.value = LastScannedPosEvent(
                    productId = savedProduct.id,
                    productName = savedProduct.name,
                    scannedBarcode = cleanCode,
                    formatLabel = "NEW",
                    decodeTimeMs = 0L,
                    isCartonScan = asCartonBarcode,
                    unitsPerCarton = safeUnitsPerCarton,
                    addedUnitsInLastScan = safeCartUnits,
                    unitPrice = savedProduct.price,
                    eventTimestampMillis = System.currentTimeMillis()
                )
            }
            showStatusMessage("تم تعريف (${savedProduct.name}) وإضافته إلى الفاتورة فوراً ✅")
        }
    }

    fun createNewProductFromComprehensiveModal(
        item: PurchaseInvoiceItem,
        addToPosCartImmediately: Boolean = true
    ) {
        val cleanName = item.productName.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            val safeUnitsPerCarton = item.unitsPerCarton.coerceAtLeast(1)
            val totalStockUnits = if (item.isCarton) {
                item.quantity.coerceAtLeast(0) * safeUnitsPerCarton
            } else {
                item.quantity.coerceAtLeast(0)
            }
            val singleUnitPurchaseCost = if (item.isCarton && safeUnitsPerCarton > 0) {
                item.unitCost / safeUnitsPerCarton.toDouble()
            } else {
                item.unitCost
            }
            val newProduct = Product(
                id = 0,
                name = cleanName,
                barcode = item.barcode.trim(),
                cartonBarcode = item.cartonBarcode.trim(),
                price = item.sellingPrice.coerceAtLeast(0.0),
                purchasePrice = singleUnitPurchaseCost.coerceAtLeast(0.0),
                stock = totalStockUnits.coerceAtLeast(1),
                unitsPerCarton = safeUnitsPerCarton,
                imagePath = item.imagePath
            )
            val insertedId = repository.saveProduct(newProduct).toInt()
            val savedProduct = if (insertedId > 0) {
                newProduct.copy(id = insertedId)
            } else {
                repository.findProductByBarcode(newProduct.barcode.ifBlank { newProduct.cartonBarcode }) ?: newProduct
            }
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushProductToCloud(savedProduct, activeStaffAccount)
            }
            repository.logActivity(
                user = _currentUser.value,
                actionType = "PRODUCT_CREATED_BARCODE",
                description = "إنشاء سلعة جديدة (${newProduct.name}) بالباركود وحفظها في المخزن (الكمية: $totalStockUnits ق)"
            )
            _unregisteredScannedBarcode.value = null
            if (addToPosCartImmediately && savedProduct.id > 0) {
                val initialCartUnits = if (item.isCarton) safeUnitsPerCarton else 1
                addToCartWithQuantity(
                    product = savedProduct,
                    totalUnitsToAdd = initialCartUnits,
                    asCarton = item.isCarton
                )
                _lastScannedPosEvent.value = LastScannedPosEvent(
                    productId = savedProduct.id,
                    productName = savedProduct.name,
                    scannedBarcode = savedProduct.barcode.ifBlank { savedProduct.cartonBarcode },
                    formatLabel = "NEW",
                    decodeTimeMs = 0L,
                    isCartonScan = item.isCarton,
                    unitsPerCarton = safeUnitsPerCarton,
                    addedUnitsInLastScan = initialCartUnits,
                    unitPrice = savedProduct.price,
                    eventTimestampMillis = System.currentTimeMillis()
                )
                showStatusMessage("تم إنشاء السلعة (${savedProduct.name}) وإضافتها للفاتورة فوراً ✅")
            } else {
                showStatusMessage("تم إنشاء السلعة (${newProduct.name}) وربط الباركود وحفظها في المخزن ✅")
            }
        }
    }

    fun completeCheckout(onSuccess: (ArchivedInvoice) -> Unit = {}) {
        val items = _cartItems.value
        if (items.isEmpty()) return
        val customer = _selectedCustomer.value
        val method = _paymentMethod.value
        val isWholeCartDebt = method == "DEBT"
        val hasItemDebt = items.any { it.hasAnyDebt }

        if ((isWholeCartDebt || hasItemDebt) && customer == null) {
            showStatusMessage("يرجى اختيار العميل أولاً عند تسجيل فاتورة أو سلعة بالدين (آجل)")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val rawSubtotal = calculateInvoiceSubtotal()
            val finalTotal = calculateInvoiceFinalTotal()
            val ratio = if (rawSubtotal > 0.0) finalTotal / rawSubtotal else 1.0

            val currentUserSnapshot = _currentUser.value
            val currentCurrency = _currencySymbol.value

            val (invoice, newlyLowStockItems) = withContext(Dispatchers.IO) {
                val pendingSales = mutableListOf<Sale>()
                val updatedProducts = mutableListOf<Product>()
                val lowStockNames = mutableListOf<String>()
                var totalDebtPortion = 0.0

                val invoiceCode = generateNextInvoiceNumber(isPreSale = false, timestamp = now)
                items.forEach { cartItem ->
                    val netLineTotal = cartItem.lineSubtotal * ratio
                    val unitsDeducted = cartItem.effectiveUnitsDeducted
                    val itemDebtPortion = if (isWholeCartDebt) {
                        netLineTotal
                    } else if (cartItem.customDebtAmount != null) {
                        (cartItem.customDebtAmount.coerceIn(0.0, cartItem.lineSubtotal)) * ratio
                    } else if (cartItem.isItemDebt) {
                        netLineTotal
                    } else {
                        0.0
                    }
                    totalDebtPortion += itemDebtPortion

                    val isLineDebt = isWholeCartDebt || cartItem.hasAnyDebt
                    val linePaymentMethod = if (isWholeCartDebt || (cartItem.isItemDebt && cartItem.customDebtAmount == null)) {
                        "DEBT"
                    } else if (itemDebtPortion > 0.0) {
                        "PARTIAL_DEBT"
                    } else {
                        method
                    }

                    val sale = Sale(
                        productId = cartItem.product.id,
                        productName = buildCartItemReceiptTitle(cartItem),
                        quantitySold = unitsDeducted,
                        totalPrice = netLineTotal,
                        saleDate = now,
                        customerId = customer?.id,
                        customerName = customer?.name ?: "عميل نقدي",
                        isDebt = isLineDebt,
                        paymentMethod = linePaymentMethod,
                        invoiceGroupId = invoiceCode,
                        updatedAt = now,
                        syncStatus = RecordSyncState.PENDING
                    )
                    pendingSales.add(sale)

                    val updatedStock = (cartItem.product.stock - unitsDeducted).coerceAtLeast(0)
                    val minThreshold = getEffectiveMinStockThreshold(cartItem.product.id)
                    if (updatedStock <= minThreshold) {
                        lowStockNames.add("${cartItem.product.name} ($updatedStock/$minThreshold)")
                    }
                    updatedProducts.add(
                        cartItem.product.copy(
                            stock = updatedStock,
                            updatedAt = now,
                            syncStatus = RecordSyncState.PENDING
                        )
                    )
                }

                val totalInvoiceSum = pendingSales.sumOf { it.totalPrice }
                val updatedCustomerEntity = customer?.let {
                    val spendRatio = _loyaltySpendPerPoint.value.coerceAtLeast(1.0)
                    val pointsEarned = (totalInvoiceSum / spendRatio).toInt().coerceAtLeast(0)
                    val isApplied = _isLoyaltyDiscountApplied.value && it.loyaltyPoints >= it.loyaltyGoal && !isCustomerLoyaltyExpired(it)
                    val pointsDeducted = if (isApplied) it.loyaltyGoal else 0
                    val nextPoints = (it.loyaltyPoints - pointsDeducted + pointsEarned).coerceAtLeast(0)

                    it.copy(
                        totalPurchases = it.totalPurchases + totalInvoiceSum,
                        balanceDebt = it.balanceDebt + totalDebtPortion,
                        loyaltyPoints = nextPoints,
                        updatedAt = now,
                        syncStatus = RecordSyncState.PENDING
                    )
                }
                val debtTxEntity = if (customer != null && totalDebtPortion > 0.0) {
                    CustomerTransaction(
                        customerId = customer.id,
                        customerName = customer.name,
                        type = "DEBT",
                        amount = totalDebtPortion,
                        notes = "فاتورة مبيعات $invoiceCode",
                        date = now,
                        updatedAt = now,
                        syncStatus = RecordSyncState.PENDING
                    )
                } else {
                    null
                }

                // Persist sales, stock deductions, customer debt, and sync queue entry in 1 atomic Room @Transaction
                val insertedIds = repository.recordPosCheckoutAtomically(
                    salesToInsert = pendingSales,
                    productsToUpdate = updatedProducts,
                    updatedCustomer = updatedCustomerEntity,
                    debtTransaction = debtTxEntity
                )

                val recordedSales = pendingSales.mapIndexed { idx, s ->
                    s.copy(saleId = insertedIds.getOrNull(idx)?.toInt() ?: 0)
                }

                val builtInvoice = ArchivedInvoice(
                    invoiceId = invoiceCode,
                    dateMillis = now,
                    customerName = customer?.name ?: "عميل نقدي",
                    customerId = customer?.id,
                    paymentMethod = if (isWholeCartDebt) "DEBT" else if (totalDebtPortion > 0.0) "PARTIAL_DEBT" else method,
                    isDebt = totalDebtPortion > 0.0,
                    items = recordedSales,
                    subtotal = totalInvoiceSum,
                    totalQuantity = recordedSales.sumOf { it.quantitySold },
                    syncStatus = RecordSyncState.PENDING
                )

                repository.logActivity(
                    user = currentUserSnapshot,
                    actionType = "SALE_CREATED",
                    description = "إصدار فاتورة ${builtInvoice.invoiceId} بقيمة ${String.format(Locale.US, "%.2f", totalInvoiceSum)} $currentCurrency"
                )

                builtInvoice to lowStockNames
            }

            val usedCoupon = _appliedCoupon.value
            if (usedCoupon != null) {
                promotionRepository.recordPromotionUsage(usedCoupon.id)
            }
            _lastCompletedInvoice.value = invoice
            clearCart()

            // Push real-time stock deductions, invoice, and GPS location to cloud for manager & team
            launch(Dispatchers.IO) {
                val deductions = items.map { cartItem ->
                    val unitsDeducted = cartItem.effectiveUnitsDeducted
                    val newStock = (cartItem.product.stock - unitsDeducted).coerceAtLeast(0)
                    LiveInventorySyncManager.StockDeduction(
                        productId = cartItem.product.id,
                        barcode = cartItem.product.barcode,
                        firestoreId = cartItem.product.firestoreId,
                        unitsDeducted = unitsDeducted,
                        resultingStock = newStock
                    )
                }
                liveInventorySyncManager.pushBatchStockDeductions(deductions, activeStaffAccount)
                liveInventorySyncManager.pushInvoiceToCloud(invoice, activeStaffAccount)

                // Automatic checkout GPS location sync
                try {
                    val app = getApplication<Application>()
                    val locationManager = app.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                    val loc = locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                        ?: locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                    if (loc != null) {
                        val currentStaff = activeStaffAccount
                        val userId = currentStaff?.userId?.filter { it.isDigit() }?.toIntOrNull()
                            ?: (_currentUser.value?.id ?: 1001)
                        val workerName = currentStaff?.fullName?.ifBlank { currentStaff.email }
                            ?: (_currentUser.value?.fullName ?: "كاشير")
                        liveInventorySyncManager.pushWorkerLocation(
                            userId = userId,
                            workerName = workerName,
                            lat = loc.latitude,
                            lng = loc.longitude,
                            statusNote = "إصدار فاتورة ${invoice.invoiceId} (${invoice.customerName})",
                            activeAccount = activeStaffAccount
                        )
                    }
                } catch (_: SecurityException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (newlyLowStockItems.isNotEmpty()) {
                showStatusMessage("⚠️ تنبيه نقص مخزون: ${newlyLowStockItems.joinToString(" • ")} دون الحد الأدنى للطلب!")
            } else {
                showStatusMessage("تم حفظ وإصدار الفاتورة ${invoice.invoiceId} ومزامنة المخزون بنجاح ✅")
            }
            onSuccess(invoice)
        }
    }

    // Inventory Operations
    fun saveProduct(product: Product, customMinStock: Int? = getCustomProductMinStock(product.id)) {
        viewModelScope.launch {
            val savedId = repository.saveProduct(product).toInt()
            val resolvedId = if (product.id > 0) product.id else savedId
            val persistedProduct = product.copy(id = resolvedId)
            if (resolvedId > 0) {
                setCustomProductMinStock(resolvedId, customMinStock)
                _cartItems.value = _cartItems.value.map { item ->
                    if (item.product.id == resolvedId) item.copy(product = persistedProduct) else item
                }
            }
            repository.logActivity(
                user = _currentUser.value,
                actionType = "PRODUCT_UPSERT",
                description = "حفظ/تحديث الصنف: ${product.name} (المخزون: ${product.stock})"
            )
            // Push real-time to cloud for all workers
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushProductToCloud(persistedProduct, activeStaffAccount)
            }
            showStatusMessage("تم حفظ بيانات الصنف (${product.name}) ومزامنته سحابياً بنجاح ✅")
        }
    }

    fun quickRestockProduct(product: Product, addedUnits: Int) {
        val safeAdded = addedUnits.coerceAtLeast(1)
        val newStock = product.stock + safeAdded
        viewModelScope.launch {
            repository.saveProduct(product.copy(stock = newStock))
            repository.logActivity(
                user = _currentUser.value,
                actionType = "PRODUCT_RESTOCK",
                description = "إضافة مخزون سريع للصنف: ${product.name} (+$safeAdded وحدة | الرصيد الجديد: $newStock)",
                metadata = "productId=${product.id};addedUnits=$safeAdded;newStock=$newStock"
            )
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushBatchStockIncreases(
                    listOf(
                        LiveInventorySyncManager.StockIncrease(
                            productId = product.id,
                            barcode = product.barcode,
                            firestoreId = product.firestoreId,
                            unitsAdded = safeAdded,
                            resultingStock = newStock
                        )
                    ),
                    activeStaffAccount
                )
            }
            showStatusMessage("تم تزويد مخزون (${product.name}) بـ +$safeAdded وحدة ومزامنته فورياً ✅")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.softDeleteProduct(product.id)
            repository.logActivity(
                user = _currentUser.value,
                actionType = "PRODUCT_DELETED",
                description = "حذف الصنف: ${product.name}"
            )
            launch(Dispatchers.IO) {
                liveInventorySyncManager.deleteProductFromCloud(product, activeStaffAccount)
            }
            showStatusMessage("تم نقل الصنف (${product.name}) إلى المحذوفات ومزامنته")
        }
    }

    /**
     * اعتماد نتائج الجرد الفعلي السريع (الجرد الفعلي مقابل المسجل) وتحديث المخزون ومزامنته مع العمال.
     */
    fun applyStockAuditAdjustments(
        adjustments: Map<Int, Int>,
        auditNote: String = "جرد فعلي سريع"
    ) {
        if (adjustments.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val allCurrent = products.value.associateBy { it.id }
            val updatedList = mutableListOf<Product>()
            var shortageCount = 0
            var surplusCount = 0

            for ((productId, actualStockRaw) in adjustments) {
                val prod = allCurrent[productId] ?: continue
                val actualStock = actualStockRaw.coerceAtLeast(0)
                val diff = actualStock - prod.stock
                if (diff < 0) shortageCount++ else if (diff > 0) surplusCount++
                val diffLabel = when {
                    diff > 0 -> "فائض +$diff"
                    diff < 0 -> "عجز $diff"
                    else -> "مطابق (0)"
                }
                val updatedProd = prod.copy(
                    stock = actualStock,
                    updatedAt = now,
                    syncStatus = RecordSyncState.PENDING
                )
                repository.saveProduct(updatedProd)
                updatedList.add(updatedProd)
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "STOCK_AUDIT",
                    description = "جرد فعلي للصنف (${prod.name}): المسجل (${prod.stock} ق) ← الفعلي ($actualStock ق) • الفرق: $diffLabel${if (auditNote.isNotBlank()) " ($auditNote)" else ""}",
                    metadata = "productId=${prod.id};oldStock=${prod.stock};newStock=$actualStock;diff=$diff"
                )
            }

            if (updatedList.isNotEmpty()) {
                _cartItems.value = _cartItems.value.map { item ->
                    val updatedMatch = updatedList.find { it.id == item.product.id }
                    if (updatedMatch != null) item.copy(product = updatedMatch) else item
                }
                launch(Dispatchers.IO) {
                    updatedList.forEach { updatedProd ->
                        liveInventorySyncManager.pushProductToCloud(updatedProd, activeStaffAccount)
                    }
                }
                showStatusMessage("تم اعتماد الجرد الفعلي لـ ${updatedList.size} صنف (فائض: $surplusCount | عجز: $shortageCount) ومزامنة المخزون ✅")
            }
        }
    }

    /**
     * تطبيق تخفيض سريع على سلعة راكدة لتنشيط مبيعاتها ومزامنة السعر الجديد فوراً مع الكاشير والعمال.
     */
    fun applyStagnantProductDiscount(
        product: Product,
        newSellingPrice: Double,
        discountLabel: String = ""
    ) {
        val safePrice = newSellingPrice.coerceAtLeast(0.0)
        if (safePrice <= 0.0) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val oldPrice = product.price
            val updatedProd = product.copy(
                price = safePrice,
                updatedAt = now,
                syncStatus = RecordSyncState.PENDING
            )
            repository.saveProduct(updatedProd)
            _cartItems.value = _cartItems.value.map { item ->
                if (item.product.id == product.id) item.copy(product = updatedProd) else item
            }
            val labelPart = if (discountLabel.isNotBlank()) " ($discountLabel)" else ""
            repository.logActivity(
                user = _currentUser.value,
                actionType = "STAGNANT_DISCOUNT",
                description = "تخفيض سعر سلعة راكدة (${product.name})$labelPart: من $oldPrice إلى $safePrice ${_currencySymbol.value}",
                metadata = "productId=${product.id};oldPrice=$oldPrice;newPrice=$safePrice"
            )
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushProductToCloud(updatedProd, activeStaffAccount)
            }
            showStatusMessage("تم تطبيق التخفيض على (${product.name}) ليصبح السعر $safePrice ${_currencySymbol.value} ومزامنته ✅")
        }
    }

    // Customers & Debts
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showStatusMessage("تم حفظ بيانات العميل: ${customer.name}")
        }
    }

    fun createAndSelectCustomerForPos(
        name: String,
        phone: String,
        openingBalanceDebt: Double = 0.0,
        notes: String = "",
        onComplete: (Customer) -> Unit = {}
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            val newCustomer = Customer(
                name = trimmedName,
                phone = phone.trim(),
                balanceDebt = openingBalanceDebt.coerceAtLeast(0.0),
                notes = notes.trim()
            )
            val generatedId = repository.saveCustomer(newCustomer).toInt()
            val savedCustomer = newCustomer.copy(id = generatedId)
            if (openingBalanceDebt > 0.0) {
                repository.recordCustomerDebt(
                    customer = savedCustomer.copy(balanceDebt = 0.0),
                    amount = openingBalanceDebt,
                    notes = "رصيد افتتاحي عند إنشاء العميل من الكاشير"
                )
            }
            _selectedCustomer.value = savedCustomer
            repository.logActivity(
                user = _currentUser.value,
                actionType = "CUSTOMER_CREATED_POS",
                description = "إضافة العميل ($trimmedName) وربطه بالفاتورة مباشرة"
            )
            showStatusMessage("تم إنشاء العميل (${savedCustomer.name}) وتحديده للفاتورة")
            onComplete(savedCustomer)
        }
    }

    fun recordCustomerPayment(customer: Customer, amount: Double, notes: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.recordCustomerPayment(customer, amount, notes)
            repository.logActivity(
                user = _currentUser.value,
                actionType = "DEBT_PAYMENT",
                description = "تحصيل دفعة ${String.format(Locale.US, "%.2f", amount)} من العميل ${customer.name}"
            )
            showStatusMessage("تم تسجيل دفعة العميل ${customer.name} بنجاح")
        }
    }

    fun addCustomerManualDebt(customer: Customer, amount: Double, notes: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.recordCustomerDebt(customer, amount, notes)
            repository.logActivity(
                user = _currentUser.value,
                actionType = "DEBT_ADDED",
                description = "إضافة مديونية ${String.format(Locale.US, "%.2f", amount)} على العميل ${customer.name}"
            )
            showStatusMessage("تمت إضافة المديونية على حساب ${customer.name} بنجاح")
        }
    }

    fun createCustomerWithInitialDebt(
        name: String,
        phone: String,
        initialDebt: Double,
        notes: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            val newCustomer = Customer(
                name = cleanName,
                phone = phone.trim(),
                balanceDebt = 0.0,
                notes = notes.trim()
            )
            val generatedId = repository.saveCustomer(newCustomer).toInt()
            val savedCustomer = newCustomer.copy(id = generatedId)
            if (initialDebt > 0.0) {
                repository.recordCustomerDebt(
                    customer = savedCustomer,
                    amount = initialDebt,
                    notes = notes.trim().ifBlank { "تسجيل مديونية جديدة" }
                )
            }
            repository.logActivity(
                user = _currentUser.value,
                actionType = "CUSTOMER_DEBT_CREATED",
                description = "إضافة العميل ($cleanName) بمديونية ${String.format(Locale.US, "%.2f", initialDebt)}"
            )
            showStatusMessage("تم إنشاء حساب العميل ($cleanName) وتسجيل المديونية بنجاح ✅")
        }
    }

    fun recordManualSupplierDebt(
        supplierName: String,
        supplierPhone: String,
        debtAmount: Double,
        notes: String
    ) {
        val cleanSupplier = supplierName.trim()
        if (cleanSupplier.isEmpty() || debtAmount <= 0.0) return
        viewModelScope.launch {
            val rand5 = ((System.currentTimeMillis() / 100) % 90000 + 10000).toInt()
            val invoiceNumber = "SUP-DEBT-$rand5"
            val item = PurchaseInvoiceItem(
                productId = null,
                productName = notes.trim().ifBlank { "رصيد مديونية مورد آجل" },
                quantity = 1,
                unitCost = debtAmount,
                sellingPrice = debtAmount,
                subtotal = debtAmount,
                isNewProduct = false
            )
            val invoice = PurchaseInvoice(
                invoiceNumber = invoiceNumber,
                supplierName = cleanSupplier,
                supplierPhone = supplierPhone.trim(),
                totalCost = debtAmount,
                paidAmount = 0.0,
                remainingDebt = debtAmount,
                paymentStatus = "UNPAID",
                notes = notes.trim().ifBlank { "مديونية مورد مسجلة من نافذة الديون وحركتها" },
                itemsJson = PurchaseInvoice.encodeItems(listOf(item)),
                itemsCount = 1
            )
            val savedId = repository.savePurchaseInvoice(invoice).toInt()
            val savedInvoice = if (savedId > 0) invoice.copy(id = savedId) else invoice
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushPurchaseInvoiceToCloud(savedInvoice, activeStaffAccount)
            }
            repository.logActivity(
                user = _currentUser.value,
                actionType = "SUPPLIER_DEBT_ADDED",
                description = "تسجيل مديونية للمورد ($cleanSupplier) بقيمة ${String.format(Locale.US, "%.2f", debtAmount)}"
            )
            showStatusMessage("تم تسجيل مديونية المورد ($cleanSupplier) بنجاح ✅")
        }
    }

    // Purchase Invoices
    fun createPurchaseInvoice(
        supplierName: String,
        supplierPhone: String,
        paidAmount: Double,
        notes: String,
        items: List<PurchaseInvoiceItem>,
        customInvoiceNumber: String = ""
    ) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val inv = repository.recordPurchaseInvoice(
                supplierName = supplierName,
                supplierPhone = supplierPhone,
                paidAmount = paidAmount,
                notes = notes,
                items = items,
                customInvoiceNumber = customInvoiceNumber
            )
            repository.logActivity(
                user = _currentUser.value,
                actionType = "PURCHASE_INVOICE",
                description = "تسجيل فاتورة مشتريات ${inv.invoiceNumber} من المورد ${inv.supplierName}"
            )
            // Push real-time stock increases and purchase invoice to cloud for all workers
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushPurchaseInvoiceToCloud(inv, activeStaffAccount)
                val currentProds = repository.getAllProductsOnce().associateBy { it.id }
                val increases = mutableListOf<LiveInventorySyncManager.StockIncrease>()
                for (it in items) {
                    val matching = currentProds.values.find { p ->
                        (it.productId != null && it.productId > 0 && p.id == it.productId) ||
                        (it.barcode.isNotBlank() && p.matchesAnyBarcode(it.barcode)) ||
                        p.name.equals(it.productName, ignoreCase = true)
                    }
                    if (matching != null) {
                        liveInventorySyncManager.pushProductToCloud(matching, activeStaffAccount)
                        increases.add(
                            LiveInventorySyncManager.StockIncrease(
                                productId = matching.id,
                                barcode = matching.barcode,
                                firestoreId = matching.firestoreId,
                                unitsAdded = if (it.isCarton) it.quantity * it.unitsPerCarton else it.quantity,
                                resultingStock = matching.stock
                            )
                        )
                    }
                }
                if (increases.isNotEmpty()) {
                    liveInventorySyncManager.pushBatchStockIncreases(increases, activeStaffAccount)
                }
            }
            showStatusMessage("تم حفظ فاتورة المشتريات ومزامنة كميات المخزون سحابياً بنجاح ✅")
        }
    }

    fun paySupplierInvoiceDebt(invoice: PurchaseInvoice, paymentAmount: Double) {
        if (paymentAmount <= 0.0) return
        viewModelScope.launch {
            val newPaid = (invoice.paidAmount + paymentAmount).coerceAtMost(invoice.totalCost)
            val newRemaining = (invoice.totalCost - newPaid).coerceAtLeast(0.0)
            val newStatus = when {
                newRemaining <= 0.009 -> "PAID"
                newPaid > 0.0 -> "PARTIAL"
                else -> "UNPAID"
            }
            val updated = invoice.copy(
                paidAmount = newPaid,
                remainingDebt = newRemaining,
                paymentStatus = newStatus
            )
            repository.savePurchaseInvoice(updated)
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushPurchaseInvoiceToCloud(updated, activeStaffAccount)
            }
            repository.logActivity(
                user = _currentUser.value,
                actionType = "SUPPLIER_PAYMENT",
                description = "سداد دفعة ${String.format(Locale.US, "%.2f", paymentAmount)} للمورد ${invoice.supplierName} (فاتورة ${invoice.invoiceNumber})"
            )
            showStatusMessage("تم تسجيل دفعة المورد (${invoice.supplierName}) بنجاح")
        }
    }

    fun returnSaleItem(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSaleAndRestoreStock(sale)
            launch(Dispatchers.IO) {
                val updatedProduct = repository.getAllProductsOnce().find { it.id == sale.productId }
                if (updatedProduct != null) {
                    liveInventorySyncManager.pushProductToCloud(updatedProduct, activeStaffAccount)
                }
            }
            repository.logActivity(
                user = _currentUser.value,
                actionType = "SALE_RETURN",
                description = "مرتجع مبيعات للصنف ${sale.productName} (الكمية: ${sale.quantitySold})"
            )
            showStatusMessage("تم إرجاع الصنف ${sale.productName} للمخزون بنجاح")
        }
    }

    fun requestInvoiceReturnByWorker(invoice: ArchivedInvoice, reason: String = "") {
        viewModelScope.launch {
            val staff = activeStaffAccount
            val localUser = _currentUser.value
            val workerName = staff?.fullName?.takeIf { it.isNotBlank() }
                ?: localUser?.fullName?.takeIf { it.isNotBlank() }
                ?: staff?.email?.takeIf { it.isNotBlank() }
                ?: "عامل المبيعات"
            val workerUid = staff?.userId?.takeIf { it.isNotBlank() }
                ?: localUser?.id?.toString().orEmpty()
            val managerUid = staff?.parentManagerUid?.takeIf { it.isNotBlank() }
                ?: staff?.approvedByUid.orEmpty()
            val managerEmail = staff?.parentManagerEmail.orEmpty()
            val managerName = currentWorkerAffiliatedManagerName.value

            val updatedNotes = ArchivedInvoice.buildNotesWithReturnRequest(
                existingNotes = invoice.notes,
                workerName = workerName,
                workerUid = workerUid,
                managerUid = managerUid,
                managerEmail = managerEmail,
                managerName = managerName,
                reason = reason
            )
            val updatedInvoice = invoice.copy(
                status = "return_requested",
                notes = updatedNotes,
                items = invoice.items.map { it.copy(status = "return_requested", notes = updatedNotes) }
            )
            withContext(Dispatchers.IO) {
                repository.updateInvoiceStatusAndNotes(
                    invoice = invoice,
                    newStatus = "return_requested",
                    newNotes = updatedNotes
                )
                repository.logActivity(
                    user = localUser,
                    actionType = "INVOICE_RETURN_REQUESTED",
                    description = "طلب العامل ($workerName) إرجاع الفاتورة ${invoice.invoiceId} وبانتظار موافقة المدير ($managerName)"
                )
                liveInventorySyncManager.pushInvoiceToCloud(updatedInvoice, staff)
            }
            showStatusMessage("تم إرسال طلب إرجاع الفاتورة ${invoice.invoiceId} إلى المدير ($managerName) وبانتظار موافقته 🔔")
        }
    }

    fun approveWorkerInvoiceReturn(invoice: ArchivedInvoice) {
        executeFullInvoiceRefund(invoice, approvedFromWorkerRequest = true)
    }

    fun rejectWorkerInvoiceReturn(invoice: ArchivedInvoice) {
        viewModelScope.launch {
            val staff = activeStaffAccount
            val localUser = _currentUser.value
            val cleanNotes = invoice.cleanNotesWithoutReturnTag
            val restoredInvoice = invoice.copy(
                status = "completed",
                notes = cleanNotes,
                items = invoice.items.map { it.copy(status = "completed", notes = cleanNotes) }
            )
            withContext(Dispatchers.IO) {
                repository.updateInvoiceStatusAndNotes(
                    invoice = invoice,
                    newStatus = "completed",
                    newNotes = cleanNotes
                )
                repository.logActivity(
                    user = localUser,
                    actionType = "INVOICE_RETURN_REJECTED",
                    description = "رفض المدير طلب إرجاع الفاتورة ${invoice.invoiceId} المقدم من العامل (${invoice.returnRequestWorkerName})"
                )
                liveInventorySyncManager.pushInvoiceToCloud(restoredInvoice, staff)
            }
            showStatusMessage("تم رفض طلب إرجاع الفاتورة ${invoice.invoiceId} وإبقاء الفاتورة فعالة ✓")
        }
    }

    fun refundInvoice(invoice: ArchivedInvoice) {
        if (isCurrentUserWorker.value) {
            requestInvoiceReturnByWorker(invoice)
            return
        }
        executeFullInvoiceRefund(invoice, approvedFromWorkerRequest = invoice.isReturnRequested)
    }

    private fun executeFullInvoiceRefund(invoice: ArchivedInvoice, approvedFromWorkerRequest: Boolean) {
        viewModelScope.launch {
            val staff = activeStaffAccount
            val localUser = _currentUser.value
            val workerName = invoice.returnRequestWorkerName
            withContext(Dispatchers.IO) {
                val currentProductsMap = repository.getAllProductsOnce().associateBy { it.id }.toMutableMap()
                val increases = mutableListOf<LiveInventorySyncManager.StockIncrease>()

                invoice.items.forEach { sale ->
                    val existingProd = currentProductsMap[sale.productId]
                    if (existingProd != null) {
                        val newStock = existingProd.stock + sale.quantitySold
                        currentProductsMap[sale.productId] = existingProd.copy(stock = newStock)
                        increases.add(
                            LiveInventorySyncManager.StockIncrease(
                                productId = existingProd.id,
                                barcode = existingProd.barcode,
                                firestoreId = existingProd.firestoreId,
                                unitsAdded = sale.quantitySold,
                                resultingStock = newStock
                            )
                        )
                    }
                    repository.deleteSaleAndRestoreStock(sale)
                }

                // If invoice was on debt, deduct from customer's balanceDebt
                if (invoice.isDebt && invoice.customerId != null) {
                    val cust = repository.getAllCustomersOnce().find { it.id == invoice.customerId }
                    if (cust != null && cust.balanceDebt > 0) {
                        repository.recordCustomerPayment(
                            customer = cust,
                            amount = invoice.subtotal.coerceAtMost(cust.balanceDebt),
                            notes = "خصم مديونية بسبب إرجاع الفاتورة ${invoice.invoiceId}"
                        )
                    }
                }

                repository.logActivity(
                    user = localUser,
                    actionType = if (approvedFromWorkerRequest) "INVOICE_RETURN_APPROVED" else "INVOICE_REFUNDED",
                    description = if (approvedFromWorkerRequest) {
                        "وافق المدير على طلب العامل ($workerName) بإرجاع الفاتورة ${invoice.invoiceId} واستعادة المخزون"
                    } else {
                        "مرتجع كامل للفاتورة ${invoice.invoiceId} واستعادة المخزون"
                    }
                )

                if (increases.isNotEmpty()) {
                    liveInventorySyncManager.pushBatchStockIncreases(increases, staff)
                }
                liveInventorySyncManager.pushInvoiceToCloud(
                    invoice.copy(
                        status = "refunded",
                        items = invoice.items.map { it.copy(status = "refunded") }
                    ),
                    staff
                )
            }
            val msg = if (approvedFromWorkerRequest) {
                "تمت الموافقة على طلب العامل ($workerName) وإرجاع الفاتورة ${invoice.invoiceId} واستعادة المخزون ✓"
            } else {
                "تم استرجاع الفاتورة ${invoice.invoiceId} وإعادة الكميات للمخزون ✓"
            }
            showStatusMessage(msg)
        }
    }

    // Users & Permissions
    fun saveWorkerUser(user: User) {
        viewModelScope.launch {
            repository.saveUser(user)
            showStatusMessage("تم حفظ حساب الموظف ${user.fullName}")
        }
    }

    fun deleteWorkerUser(user: User) {
        if (user.username == "admin") {
            showStatusMessage("لا يمكن حذف حساب المدير الرئيسي")
            return
        }
        viewModelScope.launch {
            repository.deleteUser(user)
            showStatusMessage("تم حذف حساب الموظف ${user.fullName}")
        }
    }

    // Local Database Backup & Restore Controls
    fun refreshLocalBackups() {
        viewModelScope.launch {
            val list = DatabaseBackupHelper.listLocalBackups(getApplication())
            _localBackups.value = list
            _encryptedBackups.value = com.example.data.backup.DatabaseBackupManager.getAvailableBackups(getApplication())
            if (list.isNotEmpty() && _lastLocalBackupTime.value == 0L) {
                _lastLocalBackupTime.value = list.first().createdAt
            }
        }
    }

    fun createEncryptedSnapshot(isAuto: Boolean = false) {
        if (_isLocalBackupBusy.value) return
        viewModelScope.launch {
            _isLocalBackupBusy.value = true
            val result = com.example.data.backup.DatabaseBackupManager.createBackup(getApplication(), isAuto)
            if (result.isSuccess) {
                val metadata = result.getOrNull()
                _encryptedBackups.value = com.example.data.backup.DatabaseBackupManager.getAvailableBackups(getApplication())
                _lastLocalBackupTime.value = metadata?.dateMillis ?: System.currentTimeMillis()
                showStatusMessage("تم إنشاء وحفظ نسخة احتياطية مشفرة بنجاح (AES-256) ✓")
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "ENCRYPTED_BACKUP_CREATED",
                    description = "إنشاء نسخة احتياطية محلية مشفرة (${metadata?.fileName}) بحجم ${metadata?.formattedSize}"
                )
            } else {
                showStatusMessage("فشل إنشاء النسخة المشفرة: ${result.exceptionOrNull()?.message}")
            }
            _isLocalBackupBusy.value = false
        }
    }

    fun restoreEncryptedSnapshot(backupMetadata: com.example.data.backup.BackupMetadata) {
        if (_isLocalBackupBusy.value) return
        viewModelScope.launch {
            _isLocalBackupBusy.value = true
            val result = com.example.data.backup.DatabaseBackupManager.restoreBackup(getApplication(), backupMetadata.file)
            if (result.isSuccess) {
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "ENCRYPTED_BACKUP_RESTORED",
                    description = "استعادة ناجحة لقاعدة البيانات من النسخة المشفرة (${backupMetadata.fileName})"
                )
                showStatusMessage("تمت استعادة قاعدة البيانات بنجاح من النسخة الاحتياطية المشفرة ✅")
            } else {
                showStatusMessage("فشل استعادة النسخة المشفرة: ${result.exceptionOrNull()?.message}")
            }
            _encryptedBackups.value = com.example.data.backup.DatabaseBackupManager.getAvailableBackups(getApplication())
            _isLocalBackupBusy.value = false
        }
    }

    fun deleteEncryptedSnapshot(backupMetadata: com.example.data.backup.BackupMetadata) {
        viewModelScope.launch {
            if (backupMetadata.file.exists()) {
                backupMetadata.file.delete()
            }
            _encryptedBackups.value = com.example.data.backup.DatabaseBackupManager.getAvailableBackups(getApplication())
            showStatusMessage("تم حذف النسخة المشفرة (${backupMetadata.fileName})")
        }
    }

    fun exportEncryptedBackupToUri(backupFile: java.io.File, uri: Uri) {
        viewModelScope.launch {
            _isLocalBackupBusy.value = true
            val result = com.example.data.backup.DatabaseBackupManager.exportBackupToUri(getApplication(), backupFile, uri)
            _isLocalBackupBusy.value = false
            if (result.isSuccess) {
                showStatusMessage("تم تصدير النسخة المشفرة إلى المسار المحدد بنجاح ✓")
            } else {
                showStatusMessage("تعذر تصدير النسخة المشفرة: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun importAndRestoreEncryptedBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLocalBackupBusy.value = true
            val result = com.example.data.backup.DatabaseBackupManager.importAndRestoreFromUri(getApplication(), uri)
            _isLocalBackupBusy.value = false
            if (result.isSuccess) {
                _encryptedBackups.value = com.example.data.backup.DatabaseBackupManager.getAvailableBackups(getApplication())
                showStatusMessage("تم استيراد واستعادة قاعدة البيانات من النسخة المشفرة بنجاح ✅")
            } else {
                showStatusMessage("فشل استيراد النسخة المشفرة: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun uploadCloudDatabaseBackup() {
        if (_isCloudBackupBusy.value) return
        viewModelScope.launch {
            _isCloudBackupBusy.value = true
            val user = _currentUser.value
            val staff = user?.let {
                StaffAccount(
                    userId = it.id.toString(),
                    email = it.username,
                    fullName = it.fullName,
                    role = it.role,
                    syncMode = "pro"
                )
            }
            val result = com.example.data.backup.CloudDatabaseBackupManager.uploadCloudBackup(getApplication(), staff)
            _isCloudBackupBusy.value = false
            if (result.isSuccess) {
                showStatusMessage(result.getOrThrow())
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "CLOUD_BACKUP_UPLOADED",
                    description = "رفع نسخة احتياطية سحابية إلى Firebase Firestore بنجاح"
                )
            } else {
                showStatusMessage("فشل النسخ الاحتياطي السحابي: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun restoreCloudDatabaseBackup() {
        if (_isCloudBackupBusy.value) return
        viewModelScope.launch {
            _isCloudBackupBusy.value = true
            val user = _currentUser.value
            val staff = user?.let {
                StaffAccount(
                    userId = it.id.toString(),
                    email = it.username,
                    fullName = it.fullName,
                    role = it.role,
                    syncMode = "pro"
                )
            }
            val result = com.example.data.backup.CloudDatabaseBackupManager.restoreCloudBackup(getApplication(), staff)
            _isCloudBackupBusy.value = false
            if (result.isSuccess) {
                showStatusMessage(result.getOrThrow())
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "CLOUD_BACKUP_RESTORED",
                    description = "استعادة ناجحة لقاعدة البيانات من النسخة الاحتياطية السحابية"
                )
            } else {
                showStatusMessage("فشل استعادة النسخة السحابية: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateLoyaltyCampaignSettings(
        goal: Int,
        rewardValue: Double,
        spendPerPoint: Double,
        validityDays: Int = 90,
        applyToAllCustomers: Boolean = true
    ) {
        val validGoal = goal.coerceAtLeast(1)
        val validReward = rewardValue.coerceAtLeast(0.0)
        val validSpend = spendPerPoint.coerceAtLeast(1.0)
        val validDays = validityDays.coerceAtLeast(0)

        _loyaltyGoalDefault.value = validGoal
        _loyaltyRewardDefault.value = validReward
        _loyaltySpendPerPoint.value = validSpend
        _loyaltyValidityDays.value = validDays

        prefs.edit()
            .putInt("store_loyalty_goal", validGoal)
            .putString("store_loyalty_reward_value", validReward.toString())
            .putString("store_loyalty_spend_per_point", validSpend.toString())
            .putInt("store_loyalty_validity_days", validDays)
            .apply()

        viewModelScope.launch {
            if (applyToAllCustomers) {
                repository.updateAllCustomersLoyaltyRules(validGoal, validReward)
            }
            val validityDesc = if (validDays > 0) "$validDays يوماً" else "دائمة بدون انتهاء"
            repository.logActivity(
                user = _currentUser.value,
                actionType = "LOYALTY_CAMPAIGN_UPDATED",
                description = "تحديث شروط حملة الولاء: الهدف ($validGoal نقطة) • المكافأة ($validReward ${_currencySymbol.value}) • معدل الكسب (1 نقطة لكل $validSpend ${_currencySymbol.value}) • شرط المدة الزمنية ($validityDesc)"
            )
            showStatusMessage("تم حفظ وتطبيق شروط حملة الولاء الجديدة بنجاح ✓")
        }
    }

    fun isCustomerLoyaltyExpired(customer: Customer?): Boolean {
        if (customer == null || customer.loyaltyPoints <= 0) return false
        val validityDays = _loyaltyValidityDays.value
        if (validityDays <= 0) return false
        val elapsedDays = ((System.currentTimeMillis() - customer.updatedAt) / (1000L * 60 * 60 * 24)).toInt()
        return elapsedDays > validityDays
    }

    fun getCustomerLoyaltyRemainingDays(customer: Customer?): Int? {
        if (customer == null || customer.loyaltyPoints <= 0) return null
        val validityDays = _loyaltyValidityDays.value
        if (validityDays <= 0) return null
        val elapsedDays = ((System.currentTimeMillis() - customer.updatedAt) / (1000L * 60 * 60 * 24)).toInt()
        return (validityDays - elapsedDays).coerceAtLeast(0)
    }

    fun renewCustomerLoyaltyValidity(customer: Customer) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.saveCustomer(
                customer.copy(updatedAt = now, syncStatus = RecordSyncState.PENDING)
            )
            showStatusMessage("تم تمديد وتجديد صلاحية نقاط العميل (${customer.name}) بنجاح ✓")
        }
    }

    fun createLocalDatabaseBackup(backupNote: String = "") {
        if (_isLocalBackupBusy.value) return
        viewModelScope.launch {
            _isLocalBackupBusy.value = true
            val (result, info) = DatabaseBackupHelper.saveLocalBackupToInternalStorage(
                context = getApplication(),
                repository = repository,
                storeName = _storeName.value,
                backupNote = backupNote.trim()
            )
            if (result.success && info != null) {
                _lastLocalBackupTime.value = info.createdAt
                prefs.edit().putLong("last_local_backup_time", info.createdAt).apply()
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "LOCAL_BACKUP_CREATED",
                    description = "إنشاء نسخة احتياطية محلية (${info.fileName}) تشمل ${info.metadata.productsCount} صنف و${info.metadata.salesCount} مبيعات و${info.metadata.imagesCount} صورة"
                )
            }
            _localBackups.value = DatabaseBackupHelper.listLocalBackups(getApplication())
            _isLocalBackupBusy.value = false
            showStatusMessage(result.message)
        }
    }

    fun exportDatabaseBackupToUri(uri: Uri, specificFilePath: String? = null) {
        if (_isLocalBackupBusy.value) return
        viewModelScope.launch {
            _isLocalBackupBusy.value = true
            try {
                val jsonToExport = if (!specificFilePath.isNullOrBlank()) {
                    DatabaseBackupHelper.readLocalBackupJson(specificFilePath)
                } else {
                    val (generatedJson, meta) = DatabaseBackupHelper.generateBackupJsonString(
                        repository = repository,
                        storeName = _storeName.value,
                        context = getApplication(),
                        includeProductImages = true
                    )
                    _lastLocalBackupTime.value = meta.createdAt
                    prefs.edit().putLong("last_local_backup_time", meta.createdAt).apply()
                    generatedJson
                }

                if (jsonToExport.isNullOrBlank()) {
                    showStatusMessage("تعذر قراءة بيانات النسخة الاحتياطية للتصدير")
                } else {
                    val written = DatabaseBackupHelper.writeBackupJsonToUri(
                        context = getApplication(),
                        uri = uri,
                        jsonString = jsonToExport
                    )
                    if (written) {
                        repository.logActivity(
                            user = _currentUser.value,
                            actionType = "LOCAL_BACKUP_EXPORTED",
                            description = "تصدير نسخة احتياطية محلية شاملة إلى ملف خارجي في ذاكرة الجهاز"
                        )
                        showStatusMessage("تم تصدير وحفظ ملف النسخة الاحتياطية في ذاكرة الجهاز بنجاح ✅")
                    } else {
                        showStatusMessage("تعذر حفظ ملف النسخة الاحتياطية في المسار المحدد")
                    }
                }
            } finally {
                _isLocalBackupBusy.value = false
            }
        }
    }

    fun previewBackupFromUri(
        uri: Uri,
        onPreviewReady: (String, BackupMetadata) -> Unit
    ) {
        viewModelScope.launch {
            _isLocalBackupBusy.value = true
            val content = DatabaseBackupHelper.readBackupJsonFromUri(getApplication(), uri)
            val meta = content?.let { DatabaseBackupHelper.parseBackupMetadataFromJson(it) }
            _isLocalBackupBusy.value = false
            if (content != null && meta != null) {
                onPreviewReady(content, meta)
            } else {
                showStatusMessage("الملف المختار غير صالح أو لا يحتوي على نسخة احتياطية متوافقة مع AF store")
            }
        }
    }

    fun previewLocalBackupFile(
        backupInfo: LocalBackupFileInfo,
        onPreviewReady: (String, BackupMetadata) -> Unit
    ) {
        viewModelScope.launch {
            _isLocalBackupBusy.value = true
            val content = DatabaseBackupHelper.readLocalBackupJson(backupInfo.filePath)
            val meta = content?.let { DatabaseBackupHelper.parseBackupMetadataFromJson(it) } ?: backupInfo.metadata
            _isLocalBackupBusy.value = false
            if (!content.isNullOrBlank()) {
                onPreviewReady(content, meta)
            } else {
                showStatusMessage("تعذر قراءة ملف النسخة الاحتياطية المحلية")
            }
        }
    }

    fun restoreLocalDatabaseFromJson(
        jsonString: String,
        replaceExisting: Boolean,
        sourceLabel: String = "نسخة محلية"
    ) {
        if (_isLocalBackupBusy.value) return
        viewModelScope.launch {
            _isLocalBackupBusy.value = true
            val res = DatabaseBackupHelper.restoreBackupFromJsonString(
                repository = repository,
                jsonString = jsonString,
                replaceExisting = replaceExisting,
                context = getApplication()
            )
            if (res.success) {
                reloadPreferencesAfterRestore()
                if (replaceExisting) {
                    clearCart()
                }
                val modeDesc = if (replaceExisting) "استبدال كامل للبيانات" else "دمج وتحديث مع البيانات الحالية"
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "LOCAL_BACKUP_RESTORED",
                    description = "استعادة قاعدة البيانات من $sourceLabel ($modeDesc)"
                )
            }
            _localBackups.value = DatabaseBackupHelper.listLocalBackups(getApplication())
            _isLocalBackupBusy.value = false
            showStatusMessage(res.message)
        }
    }

    fun deleteLocalDatabaseBackup(backupInfo: LocalBackupFileInfo) {
        viewModelScope.launch {
            val deleted = DatabaseBackupHelper.deleteLocalBackup(backupInfo.filePath)
            _localBackups.value = DatabaseBackupHelper.listLocalBackups(getApplication())
            if (deleted) {
                showStatusMessage("تم حذف النسخة الاحتياطية المحلية (${backupInfo.fileName})")
            } else {
                showStatusMessage("تعذر حذف ملف النسخة الاحتياطية")
            }
        }
    }

    private fun reloadPreferencesAfterRestore() {
        _storeName.value = prefs.getString("store_name", "AF store") ?: "AF store"
        val loadedPhones = loadStorePhonesListFromPrefs()
        _storePhonesList.value = loadedPhones
        _storePhone.value = formatPhoneNumbersForDisplay(loadedPhones)
            .ifBlank { prefs.getString("store_phone", "") ?: "" }
        _storeLogoImagePath.value = prefs.getString("store_logo_image_path", "") ?: ""
        _storeLogoCustomText.value = prefs.getString("store_logo_custom_text", "") ?: ""
        _currencySymbol.value = prefs.getString("currency_symbol", "د.ج") ?: "د.ج"
        _receiptFooterText.value = prefs.getString(
            "receipt_footer_text",
            "شكراً لتعاملكم معنا ونسعد بزيارتكم دائماً"
        ) ?: "شكراً لتعاملكم معنا ونسعد بزيارتكم دائماً"
        _developerContact.value = prefs.getString("developer_contact", "0662931569") ?: "0662931569"
        _defaultLowStockThreshold.value = prefs.getInt("default_low_stock_threshold", 5).coerceAtLeast(1)
        _customerCustomPrices.value = loadCustomerCustomPricesFromPrefs()
        _productCustomMinStock.value = loadProductCustomMinStockFromPrefs()
        _productCustomReorderQty.value = loadProductCustomReorderQtyFromPrefs()
        _mutedLowStockProductIds.value = loadMutedLowStockProductIdsFromPrefs()
        _lowStockNotificationsEnabled.value = prefs.getBoolean("low_stock_notifications_enabled", true)
        _scheduledReportConfig.value = ScheduledSalesReportHelper.loadConfig(prefs)
        _scheduledReportsArchive.value = ScheduledSalesReportHelper.loadArchive(prefs)
        _themeMode.value = loadThemeModeFromPrefs()
        _themePalette.value = loadThemePaletteFromPrefs()
        _highContrastNumbers.value = prefs.getBoolean("app_theme_high_contrast", false)
    }

    // Scheduled Daily/Weekly Sales Summary PDF Reports Management
    fun updateScheduledReportConfig(newConfig: ScheduledReportConfig) {
        _scheduledReportConfig.value = newConfig
        ScheduledSalesReportHelper.saveConfig(prefs, newConfig)
        viewModelScope.launch {
            repository.logActivity(
                user = _currentUser.value,
                actionType = "SCHEDULED_REPORT_CONFIG",
                description = "تحديث إعدادات جدولة تقارير المبيعات PDF (${newConfig.frequency.shortBadgeAr} - ${newConfig.formattedScheduledTimeAr})"
            )
            checkAndRunScheduledSalesReportIfDue()
        }
    }

    suspend fun checkAndRunScheduledSalesReportIfDue() {
        val currentConfig = _scheduledReportConfig.value
        val dueSlotKey = ScheduledSalesReportHelper.computeDueSlotKeyIfReady(currentConfig) ?: return
        generateScheduledReportInternal(
            slotKey = dueSlotKey,
            isManualTrigger = false,
            sendEmailImmediately = false
        )
    }

    fun generateScheduledReportNow(sendEmailImmediately: Boolean) {
        viewModelScope.launch {
            val slotKey = "MANUAL_${System.currentTimeMillis()}"
            generateScheduledReportInternal(
                slotKey = slotKey,
                isManualTrigger = true,
                sendEmailImmediately = sendEmailImmediately
            )
        }
    }

    private suspend fun generateScheduledReportInternal(
        slotKey: String,
        isManualTrigger: Boolean,
        sendEmailImmediately: Boolean
    ) {
        val appContext = getApplication<Application>().applicationContext
        val currentConfig = _scheduledReportConfig.value
        val allSales = sales.value.ifEmpty { repository.getAllSalesOnce() }
        val allProducts = products.value.ifEmpty { repository.getAllProductsOnce() }
        val allPurchases = purchaseInvoices.value.ifEmpty { repository.getAllPurchaseInvoicesOnce() }

        val (archivedReport, payload) = withContext(Dispatchers.IO) {
            ScheduledSalesReportHelper.generateAndArchiveScheduledReport(
                context = appContext,
                config = currentConfig,
                storeName = _storeName.value,
                storePhone = _storePhone.value,
                currencySymbol = _currencySymbol.value,
                sales = allSales,
                products = allProducts,
                purchases = allPurchases,
                slotKey = slotKey,
                isManualTrigger = isManualTrigger
            )
        }

        val updatedConfig = currentConfig.copy(
            lastGeneratedMillis = archivedReport.generatedAtMillis,
            lastScheduledSlotKey = if (isManualTrigger) {
                currentConfig.lastScheduledSlotKey.ifBlank { slotKey }
            } else {
                slotKey
            }
        )
        _scheduledReportConfig.value = updatedConfig
        ScheduledSalesReportHelper.saveConfig(prefs, updatedConfig)

        val updatedArchive = listOf(archivedReport) + _scheduledReportsArchive.value
        _scheduledReportsArchive.value = updatedArchive
        ScheduledSalesReportHelper.saveArchive(prefs, updatedArchive)

        if (currentConfig.inAppNotificationEnabled) {
            _latestUnreadScheduledReport.value = archivedReport
        }

        repository.logActivity(
            user = _currentUser.value,
            actionType = "SCHEDULED_REPORT_GENERATED",
            description = "إصدار وأرشفة ${archivedReport.titleAr} بصيغة PDF (${archivedReport.pdfFileName})"
        )

        if (sendEmailImmediately) {
            sendArchivedScheduledReportEmail(archivedReport, payload)
        } else {
            showStatusMessage("تم إصدار وأرشفة ${archivedReport.titleAr} بصيغة PDF بنجاح 📄")
        }
    }

    fun buildPayloadForArchivedReport(report: ArchivedScheduledReport): SalesReportPdfPayload {
        return ScheduledSalesReportHelper.buildPayloadForScheduledReport(
            frequency = report.frequency,
            storeName = report.storeName.ifBlank { _storeName.value },
            storePhone = report.storePhone.ifBlank { _storePhone.value },
            currencySymbol = report.currencySymbol.ifBlank { _currencySymbol.value },
            sales = sales.value,
            products = products.value,
            purchases = purchaseInvoices.value,
            generatedAtMillis = report.generatedAtMillis
        )
    }

    fun openArchivedScheduledReportPreview(report: ArchivedScheduledReport) {
        markScheduledReportNotificationRead(report.id)
        _previewingScheduledReportPayload.value = buildPayloadForArchivedReport(report)
    }

    fun closeArchivedScheduledReportPreview() {
        _previewingScheduledReportPayload.value = null
    }

    fun sendArchivedScheduledReportEmail(
        report: ArchivedScheduledReport,
        existingPayload: SalesReportPdfPayload? = null
    ) {
        val payload = existingPayload ?: buildPayloadForArchivedReport(report)
        val ok = ScheduledSalesReportHelper.sendScheduledReportViaEmail(
            context = getApplication(),
            report = report,
            recipientEmailsInput = _scheduledReportConfig.value.recipientEmails,
            fallbackPayload = payload
        )
        if (ok) {
            val updatedList = _scheduledReportsArchive.value.map { item ->
                if (item.id == report.id) {
                    item.copy(wasEmailDispatched = true, isNotificationRead = true)
                } else {
                    item
                }
            }
            _scheduledReportsArchive.value = updatedList
            ScheduledSalesReportHelper.saveArchive(prefs, updatedList)
            if (_latestUnreadScheduledReport.value?.id == report.id) {
                _latestUnreadScheduledReport.value = null
            }
        }
    }

    fun shareArchivedScheduledReport(report: ArchivedScheduledReport) {
        val payload = buildPayloadForArchivedReport(report)
        ScheduledSalesReportHelper.shareArchivedScheduledReportPdf(
            context = getApplication(),
            report = report,
            fallbackPayload = payload
        )
    }

    fun printArchivedScheduledReport(report: ArchivedScheduledReport) {
        val payload = buildPayloadForArchivedReport(report)
        com.example.ui.screens.SalesReportPdfExporter.printReport(
            context = getApplication(),
            payload = payload
        )
    }

    fun markScheduledReportNotificationRead(reportId: String) {
        val updatedList = _scheduledReportsArchive.value.map { item ->
            if (item.id == reportId) item.copy(isNotificationRead = true) else item
        }
        _scheduledReportsArchive.value = updatedList
        ScheduledSalesReportHelper.saveArchive(prefs, updatedList)
        if (_latestUnreadScheduledReport.value?.id == reportId) {
            _latestUnreadScheduledReport.value = _scheduledReportsArchive.value.firstOrNull { !it.isNotificationRead }
        }
    }

    fun deleteArchivedScheduledReport(report: ArchivedScheduledReport) {
        ScheduledSalesReportHelper.deleteArchivedPdfFile(report)
        val updatedList = _scheduledReportsArchive.value.filterNot { it.id == report.id }
        _scheduledReportsArchive.value = updatedList
        ScheduledSalesReportHelper.saveArchive(prefs, updatedList)
        if (_latestUnreadScheduledReport.value?.id == report.id) {
            _latestUnreadScheduledReport.value = updatedList.firstOrNull { !it.isNotificationRead }
        }
        showStatusMessage("تم حذف التقرير المجدول (${report.titleAr}) من الأرشيف")
    }

    fun clearAllArchivedScheduledReports() {
        _scheduledReportsArchive.value.forEach { ScheduledSalesReportHelper.deleteArchivedPdfFile(it) }
        _scheduledReportsArchive.value = emptyList()
        _latestUnreadScheduledReport.value = null
        ScheduledSalesReportHelper.saveArchive(prefs, emptyList())
        showStatusMessage("تم مسح سجل أرشيف التقارير المجدولة بالكامل")
    }

    // =========================================================================
    // Bluetooth Thermal Printer Management & ESC/POS Printing Actions
    // =========================================================================
    fun updateThermalPrinterConfig(newConfig: ThermalPrinterConfig) {
        _thermalPrinterConfig.value = newConfig
        ThermalPrinterConfig.saveToPrefs(prefs, newConfig)
    }

    fun updateThermalFontThickness(thickness: com.example.utils.ThermalFontThickness) {
        val updated = _thermalPrinterConfig.value.copy(fontThickness = thickness)
        _thermalPrinterConfig.value = updated
        ThermalPrinterConfig.saveToPrefs(prefs, updated)
    }

    fun updateDesktopPrinterConfig(newConfig: DesktopPrinterConfig) {
        _desktopPrinterConfig.value = newConfig
        DesktopPrinterConfig.saveToPrefs(prefs, newConfig)
    }

    fun updateWorkerGpsLocation(userId: Int, workerName: String, lat: Double, lng: Double, statusNote: String = "في مهمة ميدانية") {
        viewModelScope.launch {
            val res = liveInventorySyncManager.pushWorkerLocation(
                userId = userId,
                workerName = workerName,
                lat = lat,
                lng = lng,
                statusNote = statusNote,
                activeAccount = activeStaffAccount
            )
            if (res.isSuccess) {
                showStatusMessage("تم تحديث ومزامنة موقع العامل GPS مع السحابة بنجاح ✓")
            } else {
                showStatusMessage("تم حفظ الموقع محلياً بانتظار اتصال السحابة")
            }
        }
    }

    fun triggerComprehensiveSync(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = liveInventorySyncManager.performIncrementalSync(activeStaffAccount)
            showStatusMessage(result.message)
            onComplete(result.isSuccess, result.message)
        }
    }

    fun hasBluetoothConnectPermission(): Boolean =
        bluetoothPrinterManager.hasBluetoothConnectPermission()

    fun requiredBluetoothPermissions(): Array<String> =
        bluetoothPrinterManager.requiredRuntimePermissions()

    fun getPairedBluetoothPrinters(): List<BluetoothPrinterDeviceInfo> =
        bluetoothPrinterManager.getPairedDevices()

    fun selectAndConnectBluetoothPrinter(
        device: BluetoothPrinterDeviceInfo,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val updatedCfg = _thermalPrinterConfig.value.copy(
            lastPrinterAddress = device.address,
            lastPrinterName = device.name
        )
        updateThermalPrinterConfig(updatedCfg)
        viewModelScope.launch {
            val res = bluetoothPrinterManager.connectToPrinter(device.address, device.name)
            if (res.isSuccess) {
                val msg = "تم الاتصال بالطابعة الحرارية (${device.name}) بنجاح ✅"
                showStatusMessage(msg)
                onResult(true, msg)
            } else {
                val err = res.exceptionOrNull()?.message ?: "تعذر الاتصال بالطابعة (${device.name})"
                showStatusMessage(err)
                onResult(false, err)
            }
        }
    }

    fun reconnectSavedBluetoothPrinter(
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cfg = _thermalPrinterConfig.value
        if (cfg.lastPrinterAddress.isBlank()) {
            val msg = "لم يتم تحديد طابعة بلوتوث محفوظة بعد"
            showStatusMessage(msg)
            onResult(false, msg)
            return
        }
        viewModelScope.launch {
            val res = bluetoothPrinterManager.connectToPrinter(cfg.lastPrinterAddress, cfg.lastPrinterName)
            if (res.isSuccess) {
                val msg = "تمت إعادة الاتصال بالطابعة (${res.getOrDefault(cfg.lastPrinterName)}) ✅"
                showStatusMessage(msg)
                onResult(true, msg)
            } else {
                val err = res.exceptionOrNull()?.message ?: "تعذر الاتصال بالطابعة المحفوظة"
                showStatusMessage(err)
                onResult(false, err)
            }
        }
    }

    fun disconnectBluetoothPrinter() {
        viewModelScope.launch {
            bluetoothPrinterManager.disconnect()
            showStatusMessage("تم قطع الاتصال بالطابعة الحرارية")
        }
    }

    fun buildTestThermalPayload(): ThermalReceiptPrintPayload {
        return ThermalReceiptPrintPayload.createSampleTestPayload(
            storeName = _storeName.value,
            storePhone = _storePhone.value,
            currency = _currencySymbol.value,
            footerText = _receiptFooterText.value,
            developerContact = _developerContact.value,
            paperWidth = _thermalPrinterConfig.value.paperWidth,
            storeLogoImagePath = _storeLogoImagePath.value,
            storeLogoCustomText = _storeLogoCustomText.value,
            fontThickness = _thermalPrinterConfig.value.fontThickness
        )
    }

    // Pre-Sales Cart & Lifecycle Operations
    fun addToPreSaleCart(product: Product, asCarton: Boolean = false) {
        val addedPieces = if (asCarton) product.unitsPerCarton.coerceAtLeast(1) else 1
        val current = _preSaleCartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        val activeCust = _selectedPreSaleCustomer.value
        val savedCustPrice = activeCust?.let { getCustomerProductPrice(it.id, product.id) }

        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                quantity = item.quantity + addedPieces,
                isCarton = item.isCarton || asCarton
            )
        } else {
            current.add(
                CartItem(
                    product = product,
                    quantity = addedPieces,
                    isCarton = asCarton,
                    customUnitPrice = savedCustPrice ?: product.price,
                    customPriceCustomerName = if (savedCustPrice != null) activeCust.name else null,
                    isItemDebt = false
                )
            )
        }
        _lastAddedCartProductId.value = product.id
        _preSaleCartItems.value = current
    }

    fun addToPreSaleCartWithQuantity(
        product: Product,
        totalUnitsToAdd: Int,
        asCarton: Boolean = false
    ) {
        val safeUnits = totalUnitsToAdd.coerceAtLeast(1)
        val current = _preSaleCartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        val activeCust = _selectedPreSaleCustomer.value
        val savedCustPrice = activeCust?.let { getCustomerProductPrice(it.id, product.id) }

        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                quantity = item.quantity + safeUnits,
                isCarton = item.isCarton || asCarton
            )
        } else {
            current.add(
                CartItem(
                    product = product,
                    quantity = safeUnits,
                    isCarton = asCarton,
                    customUnitPrice = savedCustPrice ?: product.price,
                    customPriceCustomerName = if (savedCustPrice != null) activeCust.name else null,
                    isItemDebt = false
                )
            )
        }
        _lastAddedCartProductId.value = product.id
        _preSaleCartItems.value = current
    }

    fun adjustPreSaleCartItemUnits(productId: Int, deltaUnits: Int) {
        val current = _preSaleCartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val updatedQty = current[index].quantity + deltaUnits
            if (updatedQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = updatedQty)
            }
            _preSaleCartItems.value = current
        }
    }

    fun updatePreSaleCartQuantity(productId: Int, quantity: Int) {
        val current = _preSaleCartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (quantity <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = quantity)
            }
            _preSaleCartItems.value = current
        }
    }

    fun removeFromPreSaleCart(productId: Int) {
        _preSaleCartItems.value = _preSaleCartItems.value.filterNot { it.product.id == productId }
    }

    fun clearPreSaleCart() {
        _preSaleCartItems.value = emptyList()
        _lastAddedCartProductId.value = null
    }

    fun selectPreSaleCustomer(customer: Customer?) {
        _selectedPreSaleCustomer.value = customer
    }

    fun setPreSaleNotes(notes: String) {
        _preSaleNotes.value = notes
    }

    fun setPreSaleExpectedDeliveryDate(timestamp: Long) {
        _preSaleExpectedDeliveryDate.value = timestamp
    }

    fun savePreSaleOrder(
        expectedDeliveryDate: Long = _preSaleExpectedDeliveryDate.value,
        notes: String = _preSaleNotes.value,
        paymentMethod: String = "CASH",
        onSuccess: (ArchivedInvoice) -> Unit = {}
    ) {
        val items = _preSaleCartItems.value
        if (items.isEmpty()) return
        val customer = _selectedPreSaleCustomer.value

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val invoiceCode = generateNextInvoiceNumber(isPreSale = true, timestamp = now)
            val pendingSales = items.map { cartItem ->
                Sale(
                    productId = cartItem.product.id,
                    productName = buildCartItemReceiptTitle(cartItem),
                    quantitySold = cartItem.effectiveUnitsDeducted,
                    totalPrice = cartItem.lineSubtotal,
                    saleDate = now,
                    customerId = customer?.id,
                    customerName = customer?.name ?: "عميل طلب مسبق",
                    isDebt = paymentMethod == "DEBT",
                    paymentMethod = paymentMethod,
                    invoiceGroupId = invoiceCode,
                    orderType = "pre_sale",
                    status = "pending",
                    expectedDeliveryDate = expectedDeliveryDate,
                    notes = notes,
                    updatedAt = now,
                    syncStatus = RecordSyncState.PENDING
                )
            }

            withContext(Dispatchers.IO) {
                repository.recordPreSaleOrderAtomically(pendingSales)
            }

            val builtOrder = ArchivedInvoice(
                invoiceId = invoiceCode,
                dateMillis = now,
                customerName = customer?.name ?: "عميل طلب مسبق",
                customerId = customer?.id,
                paymentMethod = paymentMethod,
                isDebt = paymentMethod == "DEBT",
                items = pendingSales,
                subtotal = pendingSales.sumOf { it.totalPrice },
                totalQuantity = pendingSales.sumOf { it.quantitySold },
                syncStatus = RecordSyncState.PENDING,
                orderType = "pre_sale",
                status = "pending",
                expectedDeliveryDate = expectedDeliveryDate,
                notes = notes
            )

            _lastCompletedPreSaleOrder.value = builtOrder
            _preSaleCartItems.value = emptyList()
            _preSaleNotes.value = ""
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushInvoiceToCloud(builtOrder, activeStaffAccount)
            }
            showStatusMessage("تم حفظ طلب البيع المسبق $invoiceCode بنجاح وحجز البضاعة في النظام 📋")
            onSuccess(builtOrder)
        }
    }

    fun fulfillPreSaleOrder(
        invoice: ArchivedInvoice,
        finalPaymentMethod: String = "CASH",
        isDebt: Boolean = false,
        onSuccess: (ArchivedInvoice) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentProducts = repository.getAllProductsOnce().associateBy { it.id }
            val now = System.currentTimeMillis()

            val productsToUpdate = mutableListOf<Product>()
            invoice.items.forEach { saleItem ->
                val prod = currentProducts[saleItem.productId]
                if (prod != null) {
                    val newStock = (prod.stock - saleItem.quantitySold).coerceAtLeast(0)
                    productsToUpdate.add(prod.copy(stock = newStock, updatedAt = now, syncStatus = RecordSyncState.PENDING))
                }
            }

            val customer = invoice.customerId?.let { custId ->
                repository.getAllCustomersOnce().find { it.id == custId }
            }

            val updatedCustomer = if (isDebt && customer != null) {
                customer.copy(
                    totalPurchases = customer.totalPurchases + invoice.subtotal,
                    balanceDebt = customer.balanceDebt + invoice.subtotal,
                    updatedAt = now,
                    syncStatus = RecordSyncState.PENDING
                )
            } else null

            val debtTx = if (isDebt && customer != null) {
                CustomerTransaction(
                    customerId = customer.id,
                    customerName = customer.name,
                    type = "DEBT",
                    amount = invoice.subtotal,
                    notes = "تسليم طلب بيع مسبق #${invoice.invoiceId}",
                    date = now,
                    updatedAt = now,
                    syncStatus = RecordSyncState.PENDING
                )
            } else null

            withContext(Dispatchers.IO) {
                repository.fulfillPreSaleOrderAtomically(
                    invoiceGroupId = invoice.invoiceId,
                    productsToUpdate = productsToUpdate,
                    updatedCustomer = updatedCustomer,
                    debtTransaction = debtTx
                )
            }

            val completedInvoice = invoice.copy(
                status = "completed",
                paymentMethod = if (isDebt) "DEBT" else finalPaymentMethod,
                isDebt = isDebt
            )

            launch(Dispatchers.IO) {
                val deductions = invoice.items.mapNotNull { saleItem ->
                    val prod = productsToUpdate.find { it.id == saleItem.productId } ?: currentProducts[saleItem.productId]
                    if (prod != null) {
                        LiveInventorySyncManager.StockDeduction(
                            productId = prod.id,
                            barcode = prod.barcode,
                            firestoreId = prod.firestoreId,
                            unitsDeducted = saleItem.quantitySold,
                            resultingStock = prod.stock
                        )
                    } else null
                }
                if (deductions.isNotEmpty()) {
                    liveInventorySyncManager.pushBatchStockDeductions(deductions, activeStaffAccount)
                }
                liveInventorySyncManager.pushInvoiceToCloud(completedInvoice, activeStaffAccount)
            }

            showStatusMessage("تم تأكيد استلام وتسليم الطلبية #${invoice.invoiceId} وخصم المخزون بنجاح! 🚚📦")
            onSuccess(completedInvoice)
        }
    }

    fun cancelPreSaleOrder(invoice: ArchivedInvoice, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.cancelPreSaleOrder(invoice.invoiceId)
            }
            launch(Dispatchers.IO) {
                liveInventorySyncManager.pushInvoiceToCloud(invoice.copy(status = "cancelled"), activeStaffAccount)
            }
            showStatusMessage("تم إلغاء طلب البيع المسبق #${invoice.invoiceId} وتحرير الحجز ❌")
            onSuccess()
        }
    }

    fun restorePreSaleOrderToCart(invoice: ArchivedInvoice) {
        val allProds = products.value.associateBy { it.id }
        val restoredCart = invoice.items.mapNotNull { item ->
            val p = allProds[item.productId] ?: return@mapNotNull null
            CartItem(
                product = p,
                quantity = item.quantitySold,
                isCarton = false,
                customUnitPrice = if (item.quantitySold > 0) item.totalPrice / item.quantitySold else p.price,
                isItemDebt = item.isDebt
            )
        }
        _preSaleCartItems.value = restoredCart
        _selectedPreSaleCustomer.value = customers.value.find { it.id == invoice.customerId }
        _preSaleNotes.value = invoice.notes
        _preSaleExpectedDeliveryDate.value = invoice.expectedDeliveryDate.takeIf { it > 0 } ?: (System.currentTimeMillis() + 86400000L)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deletePreSaleOrder(invoice.invoiceId)
            }
        }
        showStatusMessage("تم نقل عناصر الطلب #${invoice.invoiceId} إلى السلة لتعديلها ✏️")
    }

    fun buildThermalPayloadForArchivedInvoice(invoice: ArchivedInvoice): ThermalReceiptPrintPayload {
        val allProds = products.value
        val lines = invoice.items.mapIndexed { idx, sale ->
            val matchedProduct: Product? = allProds.find { it.id == sale.productId }
            val qty = sale.quantitySold.coerceAtLeast(1)
            val unitPrice = matchedProduct?.price ?: (if (qty > 0) sale.totalPrice / qty else sale.totalPrice)
            val rawSub = qty * unitPrice
            val cleanName = sale.productName.substringBefore(" (").trim().ifBlank { sale.productName }
            PreviewLineItem(
                name = cleanName,
                quantityPieces = qty,
                unitPrice = unitPrice,
                rawSubtotal = rawSub,
                subtotalAfterDiscount = sale.totalPrice,
                itemDiscountAmount = (rawSub - sale.totalPrice).coerceAtLeast(0.0),
                itemDiscountPercent = if (rawSub > sale.totalPrice && rawSub > 0.0) {
                    ((rawSub - sale.totalPrice) / rawSub) * 100.0
                } else 0.0,
                itemTvaAmount = (sale.totalPrice - rawSub).coerceAtLeast(0.0),
                itemTvaPercent = if (sale.totalPrice > rawSub && rawSub > 0.0) {
                    ((sale.totalPrice - rawSub) / rawSub) * 100.0
                } else 0.0,
                finalLineTotal = sale.totalPrice,
                unitsPerCarton = (matchedProduct?.unitsPerCarton ?: 10).coerceAtLeast(1),
                barcode = matchedProduct?.barcode?.ifBlank { "622100100${idx + 1}" } ?: "622100100${idx + 1}",
                isDebt = sale.isDebt,
                customDebtAmount = Regex("""دين:\s*([\d\.]+)""").find(sale.productName)?.groupValues?.get(1)?.toDoubleOrNull()
            )
        }
        val sousTotal = lines.sumOf { it.rawSubtotal }
        val discountAmt = lines.sumOf { it.itemDiscountAmount }
        val tvaAmt = lines.sumOf { it.itemTvaAmount }
        val netTotal = invoice.subtotal.coerceAtLeast(0.0)
        val hasDebt = lines.any { it.isDebt }
        val hasCash = lines.any { !it.isDebt || it.isPartialDebt }
        val paymentAr = when {
            hasDebt && hasCash -> "دين ونقد"
            hasDebt -> "دين"
            else -> "نقدا"
        }
        val creditAmt = when {
            !hasDebt -> 0.0
            !hasCash && lines.all { !it.isPartialDebt } -> netTotal
            else -> {
                val sum = lines.sumOf { it.finalLineTotal }
                val scale = if (sum > 0.0) netTotal / sum else 1.0
                lines.filter { it.isDebt }.sumOf { it.effectiveDebtAmount * scale }
            }
        }
        val especesAmt = (netTotal - creditAmt).coerceAtLeast(0.0)
        val formattedTime = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.US).format(Date(invoice.dateMillis))
        val discPctInt = if (sousTotal > 0.0 && discountAmt > 0.0) {
            ((discountAmt / sousTotal) * 100.0).toInt().coerceAtLeast(1)
        } else 0

        return ThermalReceiptPrintPayload(
            storeName = _storeName.value,
            storePhone = _storePhone.value,
            customerName = invoice.customerName.ifBlank { "عميل نقدي" },
            cashierName = _currentUser.value?.fullName?.takeIf { it.isNotBlank() } ?: _currentUser.value?.username?.takeIf { !it.contains("@") } ?: "Antar",
            invoiceNumber = invoice.invoiceId,
            dateTimeFormatted = formattedTime,
            paymentMethodAr = paymentAr,
            lines = lines,
            sousTotal = sousTotal,
            discountAmount = discountAmt,
            discountPercentInt = discPctInt,
            tvaAmount = tvaAmt,
            netTotal = netTotal,
            creditAmount = creditAmt,
            especesAmount = especesAmt,
            currency = _currencySymbol.value,
            footerText = _receiptFooterText.value,
            developerContact = _developerContact.value,
            isTestReceipt = false,
            storeLogoImagePath = _storeLogoImagePath.value,
            storeLogoCustomText = _storeLogoCustomText.value,
            templateStyle = _invoiceStyle.value,
            previousDebt = customers.value.find { it.name.trim() == invoice.customerName.trim() }?.balanceDebt ?: 0.0,
            fontThickness = _thermalPrinterConfig.value.fontThickness
        )
    }

    fun printThermalReceiptPayload(
        payload: ThermalReceiptPrintPayload,
        copiesOverride: Int? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val res = bluetoothPrinterManager.printReceiptOverBluetooth(
                payload = payload,
                config = _thermalPrinterConfig.value,
                copiesOverride = copiesOverride
            )
            if (res.isSuccess) {
                val msg = res.getOrDefault("تمت الطباعة الحرارية بنجاح 🖨️")
                showStatusMessage(msg)
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "THERMAL_PRINT",
                    description = msg
                )
                onResult(true, msg)
            } else {
                val err = res.exceptionOrNull()?.message ?: "تعذر إتمام الطباعة عبر البلوتوث"
                showStatusMessage(err)
                onResult(false, err)
            }
        }
    }

    fun printArchivedInvoiceViaBluetooth(
        invoice: ArchivedInvoice,
        copiesOverride: Int? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val payload = buildThermalPayloadForArchivedInvoice(invoice)
        printThermalReceiptPayload(payload, copiesOverride, onResult)
    }

    /**
     * Submits a request for a new Multi-Tenant Manager account with a 7-day cloud trial.
     */
    fun submitTrialManagerAccountRequest(
        fullName: String,
        phone: String,
        storeName: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val sevenDaysMs = 7 * 24 * 3600 * 1000L
                val trialEndsAtMs = System.currentTimeMillis() + sevenDaysMs
                val generatedUid = "mgr_trial_" + System.currentTimeMillis()
                val email = "manager_" + System.currentTimeMillis().toString().takeLast(6) + "@afstore.app"

                val trialAccount = StaffAccount(
                    userId = generatedUid,
                    storeId = storeName.replace(" ", "_").lowercase(),
                    email = email,
                    fullName = fullName,
                    phone = phone,
                    requestedRole = "manager",
                    role = "manager",
                    approvalStatus = StaffApprovalStatus.APPROVED,
                    branchName = storeName,
                    secretPin = "1234",
                    canMakeSales = true,
                    canManageInventory = true,
                    canEditPrices = true,
                    canViewReports = true,
                    canManageDebtsAndCustomers = true,
                    canRefundOrVoid = true,
                    canApplyDiscounts = true,
                    canViewCostPrices = true,
                    canExportData = true,
                    canManagePurchaseInvoices = true,
                    isTrial = true,
                    trialEndsAtMillis = trialEndsAtMs,
                    isApprovedByOwner = false,
                    createdAt = com.google.firebase.Timestamp.now()
                )

                // Save to Local Registry & Firebase Firestore
                StaffAccountRepository.saveAccountToLocalRegistry(getApplication(), trialAccount)
                StaffAccountRepository.syncAccountToFirestore(trialAccount)

                // Save to Local Room User
                val roomUser = trialAccount.toLocalRoomUser()
                val savedId = repository.saveUser(roomUser)
                val newUser = roomUser.copy(id = savedId.toInt())

                // Set as active session
                _currentUser.value = newUser
                _storeName.value = storeName

                repository.logActivity(
                    user = newUser,
                    actionType = "CREATE_TRIAL_MANAGER",
                    description = "تم إنشاء حساب مدير تجريبي تلقائياً (7 أيام): $fullName - متجر: $storeName"
                )

                val msg = "تم فتح حساب مدير تجريبي لمده 7 أيام وتزامنه سحابياً بنجاح! تم إخطار المالك (Deffas Antar)"
                showStatusMessage(msg)
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "حدث خطأ أثناء تقديم طلب مدير تجريبي: ${e.message}"
                showStatusMessage(err)
                onComplete(false, err)
            }
        }
    }

    /**
     * Owner function to permanently activate a Trial Manager account.
     */
    fun approveManagerAccountPermanently(
        account: StaffAccount,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val updatedAccount = account.copy(
                    isTrial = false,
                    trialEndsAtMillis = 0L,
                    isApprovedByOwner = true,
                    approvalStatus = StaffApprovalStatus.APPROVED,
                    role = "manager",
                    requestedRole = "manager",
                    approvedByUid = _currentUser.value?.id?.toString() ?: "owner_deffas_antar",
                    approvedByName = StaffAccountRepository.OWNER_FULL_NAME
                )

                StaffAccountRepository.saveAccountToLocalRegistry(getApplication(), updatedAccount)
                StaffAccountRepository.syncAccountToFirestore(updatedAccount)

                // Update local Room user if exists
                val localUsers = repository.getAllUsersOnce()
                val match = localUsers.find { it.username == account.email || it.fullName == account.fullName }
                if (match != null) {
                    repository.saveUser(
                        match.copy(
                            isTrial = false,
                            trialEndsAt = 0L,
                            isApprovedByOwner = true,
                            isActive = true,
                            status = "ACTIVE"
                        )
                    )
                }

                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "APPROVE_MANAGER_PERMANENT",
                    description = "قام المالك بتفعيل حساب المدير نهائياً: ${account.fullName} - متجر: ${account.branchName}"
                )

                showStatusMessage("تم السماح للمدير (${account.fullName}) وتفعيل حسابه بشكل دائم بنجاح ✓")
                onComplete(true)
            } catch (e: Exception) {
                showStatusMessage("خطأ في التفعيل: ${e.message}")
                onComplete(false)
            }
        }
    }

    /**
     * Owner function to extend a Trial Manager's trial period by 7 days.
     */
    fun extendManagerTrialSevenDays(
        account: StaffAccount,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val sevenDaysMs = 7L * 24L * 3600L * 1000L
                val currentTrialEnds = if (account.trialEndsAtMillis > System.currentTimeMillis() && !account.isPending) {
                    account.trialEndsAtMillis
                } else {
                    System.currentTimeMillis()
                }
                val newTrialEnds = currentTrialEnds + sevenDaysMs

                val updatedAccount = account.copy(
                    isTrial = true,
                    trialEndsAtMillis = newTrialEnds,
                    isApprovedByOwner = false,
                    approvalStatus = StaffApprovalStatus.APPROVED,
                    role = "manager",
                    requestedRole = "manager",
                    approvedByUid = _currentUser.value?.id?.toString() ?: "owner_deffas_antar",
                    approvedByName = StaffAccountRepository.OWNER_FULL_NAME
                )

                StaffAccountRepository.saveAccountToLocalRegistry(getApplication(), updatedAccount)
                StaffAccountRepository.syncAccountToFirestore(updatedAccount)

                val localUsers = repository.getAllUsersOnce()
                val match = localUsers.find { it.username == account.email || it.fullName == account.fullName }
                if (match != null) {
                    repository.saveUser(
                        match.copy(
                            isTrial = true,
                            trialEndsAt = newTrialEnds,
                            isApprovedByOwner = false,
                            isActive = true,
                            status = "TRIAL_ACTIVE"
                        )
                    )
                }

                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "EXTEND_MANAGER_TRIAL",
                    description = "قام المالك بتمديد التجربة لمدة 7 أيام إضافية للمدير: ${account.fullName}"
                )

                showStatusMessage("تم تمديد الفترة التجريبية للمدير (${account.fullName}) لمدة 7 أيام إضافية")
                onComplete(true)
            } catch (e: Exception) {
                showStatusMessage("خطأ أثناء التمديد: ${e.message}")
                onComplete(false)
            }
        }
    }

    /**
     * Owner function to suspend a manager account.
     */
    fun suspendManagerAccount(
        account: StaffAccount,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val updatedAccount = account.copy(
                    approvalStatus = StaffApprovalStatus.SUSPENDED,
                    isApprovedByOwner = false
                )

                StaffAccountRepository.saveAccountToLocalRegistry(getApplication(), updatedAccount)
                StaffAccountRepository.syncAccountToFirestore(updatedAccount)

                val localUsers = repository.getAllUsersOnce()
                val match = localUsers.find { it.username == account.email || it.fullName == account.fullName }
                if (match != null) {
                    repository.saveUser(
                        match.copy(
                            isActive = false,
                            status = "SUSPENDED"
                        )
                    )
                }

                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "SUSPEND_MANAGER",
                    description = "قام المالك بإيقاف حساب المدير: ${account.fullName}"
                )

                showStatusMessage("تم إيقاف حساب المدير (${account.fullName})")
                onComplete(true)
            } catch (e: Exception) {
                showStatusMessage("خطأ في الإيقاف: ${e.message}")
                onComplete(false)
            }
        }
    }

    /**
     * Sends an urgent direct activation request from an expired trial manager to the owner (Deffas Antar).
     */
    fun requestActivationForExpiredManager(
        user: User,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val localAccounts = StaffAccountRepository.loadLocalAccountsRegistry(context)
                val existing = localAccounts.find {
                    it.email.equals(user.username, ignoreCase = true) ||
                    it.fullName.equals(user.fullName, ignoreCase = true) ||
                    (user.storeName.isNotBlank() && it.branchName.equals(user.storeName, ignoreCase = true))
                }

                val accountToUpdate = (existing ?: StaffAccount(
                    userId = "mgr_" + user.id,
                    storeId = user.storeName.replace(" ", "_").lowercase(),
                    email = user.username,
                    fullName = user.fullName,
                    phone = user.phone,
                    role = "manager",
                    branchName = user.storeName,
                    isTrial = true,
                    trialEndsAtMillis = user.trialEndsAt
                )).copy(
                    approvalStatus = StaffApprovalStatus.PENDING,
                    isApprovedByOwner = false,
                    isTrial = true,
                    trialEndsAtMillis = user.trialEndsAt
                )

                StaffAccountRepository.saveAccountToLocalRegistry(context, accountToUpdate)
                StaffAccountRepository.syncAccountToFirestore(accountToUpdate)

                val updatedUser = user.copy(
                    status = "PENDING_APPROVAL"
                )
                repository.saveUser(updatedUser)
                _currentUser.value = updatedUser

                repository.logActivity(
                    user = user,
                    actionType = "REQUEST_DIRECT_ACTIVATION",
                    description = "طلب المدير ${user.fullName} لمتجر ${user.storeName} تفعيل حسابه مباشرة من المالك الرئيسي (Deffas Antar) بعد انتهاء التجربة"
                )

                val successMsg = "تم إرسال طلب التفعيل المباشر بنجاح إلى المالك الرئيسي (Deffas Antar) ✉️"
                showStatusMessage(successMsg)
                onResult(true, successMsg)
            } catch (e: Exception) {
                val errMsg = "تعذر إرسال طلب التفعيل: ${e.message}"
                showStatusMessage(errMsg)
                onResult(false, errMsg)
            }
        }
    }

    /**
     * Manager requests upgrade from Lite to Pro by providing their contact phone number.
     */
    fun requestProUpgrade(
        phone: String,
        note: String = "",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val account = activeStaffAccount
        if (account == null) {
            val err = "يرجى تسجيل الدخول أولاً لتقديم طلب ترقية الحساب."
            showStatusMessage(err)
            onResult(false, err)
            return
        }
        viewModelScope.launch {
            val res = staffAccountRepository.requestProUpgrade(account, phone, note)
            if (res.isSuccess) {
                _myProUpgradeRequest.value = res.getOrNull()
                val msg = "تم إرسال طلب الترقية إلى نسخة Pro بنجاح للمالك (Deffas antar) ✓"
                showStatusMessage(msg)
                onResult(true, msg)
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "تعذر تقديم طلب الترقية"
                showStatusMessage(err)
                onResult(false, err)
            }
        }
    }

    /**
     * Store Owner accepts or rejects a manager's Pro upgrade request.
     */
    fun reviewProUpgradeRequest(
        request: com.example.data.firebase.ProUpgradeRequest,
        approve: Boolean,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val res = staffAccountRepository.reviewProUpgradeRequest(request, approve)
            if (res.isSuccess) {
                val msg = if (approve) {
                    "تمت الموافقة وتفعيل خطة Pro للمدير «${request.managerName}» بنجاح ✓"
                } else {
                    "تم رفض طلب ترقية المدير «${request.managerName}»."
                }
                showStatusMessage(msg)
                onResult(true, msg)
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "تعذر معالجة طلب الترقية"
                showStatusMessage(err)
                onResult(false, err)
            }
        }
    }

    /**
     * Checks if the manager has been approved/activated by the owner in real-time.
     */
    fun checkAndRefreshManagerStatus(
        userKey: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                com.example.AFStoreApplication.initializeFirebase(context.applicationContext)
                val currentAuth = try { Firebase.auth.currentUser } catch (e: Exception) { null }
                val db = StaffAccountRepository.getFirestoreDb(context)
                val isOwner = StaffAccountRepository.isOwnerEmail(userKey) || StaffAccountRepository.isOwnerEmail(currentAuth?.email)
                val targetDocId = when {
                    userKey.startsWith("mgr_") || userKey.startsWith("wrk_") -> userKey
                    userKey.contains("@") -> StaffAccountRepository.managerPreProvisionDocId(userKey)
                    currentAuth?.uid?.isNotBlank() == true -> currentAuth.uid
                    else -> StaffAccountRepository.managerPreProvisionDocId(userKey)
                }
                val queryTask = if (isOwner) {
                    db.collection(StaffAccountRepository.COLLECTION_STAFF).get()
                } else {
                    db.collection(StaffAccountRepository.COLLECTION_STAFF).document(targetDocId).get()
                }
                queryTask
                    .addOnSuccessListener { snapshot ->
                        viewModelScope.launch {
                            val cloudAccounts = when (snapshot) {
                                is com.google.firebase.firestore.QuerySnapshot -> snapshot.documents.mapNotNull { StaffAccount.fromSnapshot(it) }
                                is com.google.firebase.firestore.DocumentSnapshot -> listOfNotNull(StaffAccount.fromSnapshot(snapshot))
                                else -> emptyList()
                            }
                            val match = cloudAccounts.find {
                                it.email.equals(userKey, ignoreCase = true) ||
                                it.userId.equals(userKey, ignoreCase = true) ||
                                (currentAuth != null && it.userId == currentAuth.uid) ||
                                (currentAuth?.email != null && it.email.equals(currentAuth.email, ignoreCase = true)) ||
                                it.fullName.equals(_currentUser.value?.fullName, ignoreCase = true) ||
                                (it.branchName.isNotBlank() && it.branchName.equals(_currentUser.value?.storeName, ignoreCase = true))
                            }
                            if (match != null) {
                                StaffAccountRepository.saveAccountToLocalRegistry(context, match)
                                if (match.isApprovedByOwner || (!match.isTrialExpired && match.isApproved)) {
                                    val localRoomUser = match.toLocalRoomUser()
                                    repository.saveUser(localRoomUser)
                                    _currentUser.value = localRoomUser
                                    showStatusMessage("تم تفعيل حسابك بنجاح من قبل المالك! مرحباً بك.")
                                    onResult(true, "تم تفعيل الحساب بنجاح!")
                                } else {
                                    showStatusMessage("الحساب لا يزال بانتظار موافقة وتفعيل المالك (Deffas Antar).")
                                    onResult(false, "الحساب لا يزال معلقاً بانتظار المالك.")
                                }
                            } else {
                                showStatusMessage("لم يتم العثور على تحديثات جديدة للحساب.")
                                onResult(false, "لم يتم العثور على تحديثات.")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        val localMatch = StaffAccountRepository.loadLocalAccountsRegistry(context).find {
                            it.email.equals(userKey, ignoreCase = true) ||
                            it.userId.equals(userKey, ignoreCase = true) ||
                            it.fullName.equals(_currentUser.value?.fullName, ignoreCase = true)
                        }
                        if (localMatch != null && (localMatch.isApprovedByOwner || (!localMatch.isTrialExpired && localMatch.isApproved))) {
                            viewModelScope.launch {
                                val localRoomUser = localMatch.toLocalRoomUser()
                                repository.saveUser(localRoomUser)
                                _currentUser.value = localRoomUser
                                showStatusMessage("تم تفعيل حسابك بنجاح! مرحباً بك.")
                                onResult(true, "تم تفعيل الحساب بنجاح!")
                            }
                        } else {
                            showStatusMessage("الحساب لا يزال بانتظار موافقة وتفعيل المالك.")
                            onResult(false, "تعذر فحص السحابة أو الحساب غير مفعل.")
                        }
                    }
            } catch (e: Exception) {
                showStatusMessage("خطأ: ${e.message}")
                onResult(false, e.message ?: "خطأ")
            }
        }
    }

    fun getLoyaltyPointsTrend(days: Int = 30): List<Pair<Long, Int>> {
        val now = System.currentTimeMillis()
        val daysInMillis = days * 24 * 60 * 60 * 1000L
        val startTime = now - daysInMillis

        // Get points earned per day
        val pointsPerDay = sales.value
            .filter { it.saleDate >= startTime }
            .groupBy { (it.saleDate / (24 * 60 * 60 * 1000L)) }
            .mapValues { entry ->
                entry.value.sumOf { (it.totalPrice / 100).toInt() }
            }

        // Calculate running sum
        var runningSum = 0
        return (0 until days).map { i ->
            val day = (startTime / (24 * 60 * 60 * 1000L)) + i
            val points = pointsPerDay[day] ?: 0
            runningSum += points
            (day * 24 * 60 * 60 * 1000L) to runningSum
        }
    }

    fun saveExpense(expense: Expense) {
        expenseRepository.saveExpense(expense)
        viewModelScope.launch {
            repository.logActivity(
                user = _currentUser.value,
                actionType = "EXPENSE_RECORDED",
                description = "تسجيل مصروف: ${expense.title} بقيمة ${String.format(Locale.US, "%.2f", expense.amount)} ${_currencySymbol.value} (${expense.category.labelAr})"
            )
        }
    }

    fun deleteExpense(id: String) {
        val exp = expenses.value.find { it.id == id }
        expenseRepository.deleteExpense(id)
        if (exp != null) {
            viewModelScope.launch {
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "EXPENSE_DELETED",
                    description = "حذف مصروف: ${exp.title} بقيمة ${String.format(Locale.US, "%.2f", exp.amount)} ${_currencySymbol.value}"
                )
            }
        }
    }

    fun getTotalExpensesBetween(startMillis: Long, endMillis: Long): Double {
        return expenseRepository.getTotalExpensesBetween(startMillis, endMillis)
    }

    fun getExpensesThisMonth(): Double {
        val start = expenseRepository.getCurrentMonthStartMillis()
        return expenseRepository.getTotalExpensesBetween(start, Long.MAX_VALUE)
    }

    fun getExpensesToday(): Double {
        val start = expenseRepository.getTodayStartMillis()
        return expenseRepository.getTotalExpensesBetween(start, Long.MAX_VALUE)
    }

    fun setProductExpiry(
        productId: Int,
        expiryDateMillis: Long,
        productionDateMillis: Long? = null,
        batchNumber: String = "",
        notes: String = ""
    ) {
        val record = ProductExpiryRecord(
            productId = productId,
            expiryDateMillis = expiryDateMillis,
            productionDateMillis = productionDateMillis,
            batchNumber = batchNumber.trim(),
            notes = notes.trim(),
            updatedAt = System.currentTimeMillis()
        )
        expiryAndWasteRepository.saveExpiryRecord(record)
        showStatusMessage("تم حفظ وتحديث تاريخ صلاحية الصنف بنجاح 📅")
    }

    fun removeProductExpiry(productId: Int) {
        expiryAndWasteRepository.removeExpiryRecord(productId)
        showStatusMessage("تم إلغاء تاريخ الصلاحية المسجل للصنف")
    }

    fun recordDamagedGoods(
        product: Product,
        quantity: Int,
        reason: SpoilageReason,
        notes: String = "",
        imagePath: String = "",
        deductFromStock: Boolean = true,
        recordedBy: String = ""
    ) {
        val safeQty = quantity.coerceAtLeast(1)
        val unitCost = product.purchasePrice.coerceAtLeast(0.0)
        val loss = safeQty * unitCost
        val staffName = recordedBy.ifBlank { activeStaffAccount?.fullName ?: "إدارة المتجر" }

        val damagedRecord = DamagedItemRecord(
            productId = product.id,
            productName = product.name,
            productBarcode = product.barcode,
            quantity = safeQty,
            unitCost = unitCost,
            totalLoss = loss,
            reason = reason,
            dateMillis = System.currentTimeMillis(),
            recordedBy = staffName,
            notes = notes.trim(),
            imagePath = imagePath,
            deductedFromStock = deductFromStock
        )
        expiryAndWasteRepository.saveDamagedItem(damagedRecord)

        viewModelScope.launch {
            if (deductFromStock) {
                val updatedStock = (product.stock - safeQty).coerceAtLeast(0)
                repository.saveProduct(product.copy(stock = updatedStock))
                repository.logActivity(
                    user = _currentUser.value,
                    actionType = "PRODUCT_DAMAGE_RECORDED",
                    description = "تسجيل تلف/هالك: ${product.name} (عدد $safeQty وحدة) بسبب ${reason.labelAr} - خسارة ${loss} د.ج",
                    metadata = "productId=${product.id};qty=$safeQty;reason=${reason.id};loss=$loss;newStock=$updatedStock"
                )
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    liveInventorySyncManager.pushBatchStockDeductions(
                        listOf(
                            LiveInventorySyncManager.StockDeduction(
                                productId = product.id,
                                barcode = product.barcode,
                                firestoreId = product.firestoreId,
                                unitsDeducted = safeQty,
                                resultingStock = updatedStock
                            )
                        ),
                        activeStaffAccount
                    )
                }
            }

            launch(kotlinx.coroutines.Dispatchers.IO) {
                val pushRes = liveInventorySyncManager.pushDamagedItemToCloud(damagedRecord, activeStaffAccount)
                pushRes.getOrNull()?.let { cloudImageUrl ->
                    if (cloudImageUrl.isNotBlank()) {
                        expiryAndWasteRepository.saveDamagedItem(damagedRecord.copy(imageUrl = cloudImageUrl))
                    }
                }
            }
        }

        showStatusMessage("تم تسجيل البضاعة التالفة ومزامنتها وصورتها سحابياً بنجاح (${reason.labelAr}) ☁️🗑️")
    }

    fun deleteDamagedItem(id: String) {
        expiryAndWasteRepository.deleteDamagedItem(id)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            liveInventorySyncManager.deleteDamagedItemFromCloud(id, activeStaffAccount)
        }
        showStatusMessage("تم حذف سجل التلف محلياً وسحابياً بنجاح")
    }

    fun syncAllDamagedGoodsToCloud() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val items = damagedItems.value
            var syncedCount = 0
            for (item in items) {
                val res = liveInventorySyncManager.pushDamagedItemToCloud(item, activeStaffAccount)
                if (res.isSuccess) {
                    syncedCount++
                    val cloudUrl = res.getOrNull().orEmpty()
                    if (cloudUrl.isNotBlank() && item.imageUrl != cloudUrl) {
                        expiryAndWasteRepository.saveDamagedItem(item.copy(imageUrl = cloudUrl))
                    }
                }
            }
            activeStaffAccount?.let { account ->
                liveInventorySyncManager.startLiveDamagedGoodsSync(account, expiryAndWasteRepository)
            }
            showStatusMessage("تمت مزامنة $syncedCount سجل تالف مع الصور سحابياً بنجاح ☁️✅")
        }
    }

    fun getDamagedLossThisMonth(): Double {
        return expiryAndWasteRepository.getTotalLossThisMonth()
    }

    fun getDamagedLossToday(): Double {
        return expiryAndWasteRepository.getTotalLossToday()
    }

    // --- Smart Daily End-of-Day Z-Report State & Helpers ---
    private val _zReportOwnerWhatsApp = MutableStateFlow(
        prefs.getString("z_report_owner_whatsapp", "") ?: ""
    )
    val zReportOwnerWhatsApp: StateFlow<String> = _zReportOwnerWhatsApp.asStateFlow()

    private val _zReportStateVersion = MutableStateFlow(0L)
    val zReportStateVersion: StateFlow<Long> = _zReportStateVersion.asStateFlow()

    fun getEffectiveOwnerWhatsAppPhone(): String {
        val custom = _zReportOwnerWhatsApp.value.trim()
        if (custom.isNotBlank()) return custom
        val firstStorePhone = _storePhonesList.value.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (firstStorePhone.isNotBlank()) return firstStorePhone
        val legacyStorePhone = _storePhone.value.substringBefore("/").substringBefore("-").trim()
        if (legacyStorePhone.isNotBlank()) return legacyStorePhone
        return _developerContact.value.trim()
    }

    fun updateZReportOwnerWhatsApp(phone: String) {
        val clean = phone.trim()
        _zReportOwnerWhatsApp.value = clean
        prefs.edit().putString("z_report_owner_whatsapp", clean).apply()
        _zReportStateVersion.value = System.currentTimeMillis()
        showStatusMessage("تم حفظ رقم واتساب المالك لإرسال تقارير الإغلاق اليومي (Z-Report) ✅")
    }

    fun getOpeningCashForDay(dayKey: String): Double {
        val raw = prefs.getString("z_report_opening_cash_$dayKey", null)
            ?: prefs.getString("z_report_default_opening_cash", "0.0")
            ?: "0.0"
        return raw.toDoubleOrNull() ?: 0.0
    }

    fun setOpeningCashForDay(dayKey: String, amount: Double, setAsDefault: Boolean = true) {
        val safeAmount = amount.coerceAtLeast(0.0)
        val formatted = String.format(Locale.US, "%.2f", safeAmount)
        val editor = prefs.edit().putString("z_report_opening_cash_$dayKey", formatted)
        if (setAsDefault) {
            editor.putString("z_report_default_opening_cash", formatted)
        }
        editor.apply()
        _zReportStateVersion.value = System.currentTimeMillis()
    }

    fun getActualCountedCashForDay(dayKey: String): Double? {
        val raw = prefs.getString("z_report_counted_cash_$dayKey", null) ?: return null
        return raw.toDoubleOrNull()
    }

    fun setActualCountedCashForDay(dayKey: String, amount: Double?) {
        val editor = prefs.edit()
        if (amount == null) {
            editor.remove("z_report_counted_cash_$dayKey")
        } else {
            editor.putString("z_report_counted_cash_$dayKey", String.format(Locale.US, "%.2f", amount.coerceAtLeast(0.0)))
        }
        editor.apply()
        _zReportStateVersion.value = System.currentTimeMillis()
    }

    fun getZReportNotesForDay(dayKey: String): String {
        return prefs.getString("z_report_notes_$dayKey", "") ?: ""
    }

    fun setZReportNotesForDay(dayKey: String, notes: String) {
        prefs.edit().putString("z_report_notes_$dayKey", notes.trim()).apply()
        _zReportStateVersion.value = System.currentTimeMillis()
    }

    fun isDayZReportClosed(dayKey: String): Boolean {
        return prefs.getBoolean("z_report_closed_$dayKey", false)
    }

    fun getDayZReportClosedTime(dayKey: String): String {
        return prefs.getString("z_report_closed_time_$dayKey", "") ?: ""
    }

    fun closeDayZReportShift(
        dayKey: String,
        expectedCashInDrawer: Double,
        totalSales: Double,
        collectedDebts: Double,
        totalExpenses: Double
    ) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        prefs.edit()
            .putBoolean("z_report_closed_$dayKey", true)
            .putString("z_report_closed_time_$dayKey", timeStr)
            .apply()
        _zReportStateVersion.value = System.currentTimeMillis()

        val staffName = activeStaffAccount?.fullName?.takeIf { it.isNotBlank() }
            ?: _currentUser.value?.fullName?.takeIf { it.isNotBlank() }
            ?: "إدارة المتجر"
        val sym = _currencySymbol.value
        viewModelScope.launch {
            repository.logActivity(
                user = _currentUser.value,
                actionType = "Z_REPORT_CLOSED",
                description = "إغلاق الصندوق اليومي (Z-Report) ليوم $dayKey بواسطة $staffName — النقد في الدرج: ${String.format(Locale.US, "%.2f", expectedCashInDrawer)} $sym | المبيعات: ${String.format(Locale.US, "%.2f", totalSales)} $sym | الديون المحصلة: ${String.format(Locale.US, "%.2f", collectedDebts)} $sym | المصروفات: ${String.format(Locale.US, "%.2f", totalExpenses)} $sym",
                metadata = "dayKey=$dayKey;cashInDrawer=$expectedCashInDrawer;sales=$totalSales;collectedDebts=$collectedDebts;expenses=$totalExpenses"
            )
        }
        showStatusMessage("تم اعتماد وإغلاق تقرير الصندوق اليومي (Z-Report) ليوم $dayKey بنجاح 📊✅")
    }

    fun reopenDayZReportShift(dayKey: String) {
        prefs.edit()
            .putBoolean("z_report_closed_$dayKey", false)
            .apply()
        _zReportStateVersion.value = System.currentTimeMillis()
        showStatusMessage("تم إعادة فتح وردية الصندوق ليوم $dayKey للتعديل")
    }
}

class AntarSalesViewModelFactory(
    private val application: Application,
    private val repository: AntarRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AntarSalesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AntarSalesViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
