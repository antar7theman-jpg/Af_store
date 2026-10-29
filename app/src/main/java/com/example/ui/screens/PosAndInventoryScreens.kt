package com.example.ui.screens

import com.example.data.models.ActivityLog
import com.example.data.models.Promotion
import com.example.data.models.PromotionType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ReceiptLong


import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocalShipping
import kotlinx.coroutines.delay
import com.example.utils.FullTextSearchHelper
import com.example.utils.HighlightedText
import com.example.ui.components.ModernChipButton
import com.example.ui.components.ModernDangerButton
import com.example.ui.components.ModernGradientButton
import com.example.ui.components.ModernIconButton
import com.example.ui.components.ModernOutlinedButton
import com.example.ui.components.ModernPrimaryButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ArchivedInvoice
import com.example.data.models.Customer
import com.example.data.models.Product
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.ScannerPreferencesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PosScreen(
    viewModel: AntarSalesViewModel,
    onOpenReorderAlertsScreen: (() -> Unit)? = null,
    isPreSaleMode: Boolean = false,
    onNavigateToPreSalesList: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val semantic = LocalAFStoreSemanticColors.current
    val products by viewModel.products.collectAsState()
    val regularCartItems by viewModel.cartItems.collectAsState()
    val preSaleCartItems by viewModel.preSaleCartItems.collectAsState()
    val cartItems = if (isPreSaleMode) preSaleCartItems else regularCartItems

    val customers by viewModel.customers.collectAsState()
    val regularCustomer by viewModel.selectedCustomer.collectAsState()
    val preSaleCustomer by viewModel.selectedPreSaleCustomer.collectAsState()
    val selectedCustomer = if (isPreSaleMode) preSaleCustomer else regularCustomer

    val preSaleDeliveryDate by viewModel.preSaleExpectedDeliveryDate.collectAsState()
    val preSaleNotes by viewModel.preSaleNotes.collectAsState()
    val pendingPreSalesCount by viewModel.pendingPreSalesCount.collectAsState()
    val reservedStockMap by viewModel.reservedStockMap.collectAsState()

    var showPreSaleConfirmModal by remember { mutableStateOf(false) }
    var preSaleDeliveryNotesInput by remember { mutableStateOf("") }
    var preSaleDeliveryDaysChoice by remember { mutableIntStateOf(1) }
    val discountAmount by viewModel.discountAmount.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val promotionalDiscountAmount by viewModel.promotionalDiscountAmount.collectAsState()
    var showCouponEntryDialog by remember { mutableStateOf(false) }
    val lastInvoice by viewModel.lastCompletedInvoice.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val defaultLowStockThreshold by viewModel.defaultLowStockThreshold.collectAsState()
    val productCustomMinStock by viewModel.productCustomMinStock.collectAsState()
    val unregisteredScannedBarcode by viewModel.unregisteredScannedBarcode.collectAsState()
        val lastScannedPosEvent by viewModel.lastScannedPosEvent.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val lastAddedCartProductId by viewModel.lastAddedCartProductId.collectAsState()
    val cartLazyListState = rememberLazyListState()

    LaunchedEffect(lastAddedCartProductId, cartItems.size, cartItems.sumOf { it.quantity }) {
        val targetId = lastAddedCartProductId
        if (targetId != null) {
            val targetIndex = cartItems.indexOfFirst { it.product.id == targetId }
            if (targetIndex >= 0) {
                cartLazyListState.animateScrollToItem(targetIndex)
            }
        } else if (cartItems.isNotEmpty()) {
            cartLazyListState.animateScrollToItem(cartItems.lastIndex)
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }

    var showBarcodeDialog by remember { mutableStateOf(false) }
    var showSearchBarcodeScanner by remember { mutableStateOf(false) }
    var isInlineScannerOpen by remember {
        mutableStateOf(ScannerPreferencesManager.isInlinePosCameraEnabled(context))
    }
    var inlineScannerCooldownResetTrigger by remember { mutableLongStateOf(0L) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCheckoutReviewSheet by remember { mutableStateOf(false) }
    var showInvoiceReceiptModal by remember { mutableStateOf(false) }
    var showLowStockDialog by remember { mutableStateOf(false) }
    var activePreviewInvoice by remember { mutableStateOf<ArchivedInvoice?>(null) }
    var selectedProductForImagePreview by remember { mutableStateOf<Product?>(null) }
    var selectedProductForMovementHistory by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            debouncedSearchQuery = ""
        } else {
            delay(90L)
            debouncedSearchQuery = trimmed
        }
    }

    val filteredProducts = remember(products, debouncedSearchQuery) {
        val q = debouncedSearchQuery.trim()
        if (q.isEmpty()) {
            products
        } else {
            products.filter { product ->
                FullTextSearchHelper.matchesFullTextSearch(product, q)
            }
        }
    }

    val rawSubtotal = remember(cartItems) { cartItems.sumOf { it.lineSubtotal } }
    val finalTotal = remember(rawSubtotal, discountAmount) { (rawSubtotal - discountAmount).coerceAtLeast(0.0) }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isWideScreen = screenWidthDp >= 600
    val isCompactCartPanel = screenWidthDp < 480
    val isCompactCard = screenWidthDp < 520

    val isInlineScanningAllowed = !showBarcodeDialog &&
        !showSearchBarcodeScanner &&
        !showCheckoutReviewSheet &&
        !showCustomerPicker &&
        !showInvoiceReceiptModal &&
        !showLowStockDialog &&
        unregisteredScannedBarcode == null &&
        selectedProductForImagePreview == null &&
        selectedProductForMovementHistory == null

    val lastScannedCartItem = remember(cartItems, lastScannedPosEvent) {
        lastScannedPosEvent?.let { ev ->
            cartItems.find { it.product.id == ev.productId }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isPreSaleMode) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF047857).copy(alpha = 0.12f),
                    border = BorderStroke(1.2.dp, Color(0xFF047857).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("pre_sale_mode_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF047857)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp).size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "نقطة البيع المسبق (Pre-sales POS)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF047857)
                                )
                                Text(
                                    text = "تسجيل طلبية المحل وحجزها للمستودع • خروج البضاعة غداً",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (onNavigateToPreSalesList != null) {
                            Button(
                                onClick = onNavigateToPreSalesList,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF047857),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                modifier = Modifier.height(34.dp).testTag("pos_open_pre_sales_list_btn")
                            ) {
                                Text(
                                    text = "طلبات المستودع ($pendingPreSalesCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            // Search & Barcode Row (Responsive: Search + SyncStatusChip + Inline Live Camera Toggle + Fullscreen Scanner)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "بحث نصي متقدم (اسم، فئة، باركود)...",
                            fontSize = if (isWideScreen) 14.sp else 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "بحث",
                            modifier = Modifier.size(if (isWideScreen) 21.dp else 18.dp)
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "مسح نص البحث",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    showSearchBarcodeScanner = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("pos_search_barcode_camera_btn")
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "قراءة الباركود بالكاميرا وتصفية الصنف",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            if (searchQuery.isNotBlank()) {
                                viewModel.scanBarcodeAndAddToCart(searchQuery)
                                searchQuery = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_search_input"),
                    shape = RoundedCornerShape(14.dp)
                )


                // Open Fullscreen Barcode Scanner Modal
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showBarcodeDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    ),
                    contentPadding = PaddingValues(horizontal = if (isWideScreen) 14.dp else 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("pos_scan_barcode_btn")
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "مسح باركود بالكاميرا",
                        modifier = Modifier.size(if (isWideScreen) 20.dp else 17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مسح باركود",
                        fontSize = if (isWideScreen) 13.sp else 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "🔍 نتائج البحث: ${filteredProducts.size} صنف (تأخير 250ms)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    TextButton(
                        onClick = { searchQuery = "" },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("إلغاء البحث ✖", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Inline Live CameraX + ZXing Scanner Dock (Hands-Free POS Scanning)
            AnimatedVisibility(visible = isInlineScannerOpen) {
                PosInlineLiveScannerDock(
                    viewModel = viewModel,
                    isScanningAllowed = isInlineScanningAllowed,
                    cooldownResetTrigger = inlineScannerCooldownResetTrigger,
                    onOpenFullscreenScanner = { showBarcodeDialog = true },
                    onCollapseDock = {
                        isInlineScannerOpen = false
                        ScannerPreferencesManager.setInlinePosCameraEnabled(context, false)
                    }
                )
            }

            // Last-Scanned Item Quick Control Strip (Quantity +1/-1, Piece <-> Carton Toggle, Undo)
            AnimatedVisibility(visible = lastScannedPosEvent != null && lastScannedCartItem != null) {
                val activeEvent = lastScannedPosEvent
                val activeCartItem = lastScannedCartItem
                if (activeEvent != null && activeCartItem != null) {
                    PosLastScannedItemQuickBar(
                        lastEvent = activeEvent,
                        cartItem = activeCartItem,
                        currencySymbol = currencySymbol,
                        onIncrementUnit = {
                            viewModel.adjustCartItemUnits(activeCartItem.product.id, 1)
                        },
                        onDecrementUnit = {
                            viewModel.adjustCartItemUnits(activeCartItem.product.id, -1)
                        },
                        onTogglePackageMode = {
                            viewModel.switchCartItemPackageMode(activeCartItem.product.id)
                        },
                        onAddFullCarton = {
                            val packUnits = activeCartItem.product.unitsPerCarton.coerceAtLeast(1)
                            viewModel.adjustCartItemUnits(activeCartItem.product.id, packUnits)
                        },
                        onRepeatScanNow = {
                            inlineScannerCooldownResetTrigger = System.currentTimeMillis()
                            viewModel.repeatLastScannedItem()
                        },
                        onRemoveFromCart = {
                            viewModel.removeCartItem(activeCartItem.product.id)
                            viewModel.dismissLastScannedPosEvent()
                        },
                        onDismissStrip = {
                            viewModel.dismissLastScannedPosEvent()
                        }
                    )
                }
            }

            // Main Split Area: Products Catalog + Active Invoice Cart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Catalog Column (Expands cleanly on tablets with multi-column grid)
                Column(
                    modifier = Modifier
                        .weight(if (isWideScreen) 1.35f else 1.12f)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "الأصناف المتاحة (${filteredProducts.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isWideScreen) 15.sp else 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (filteredProducts.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 2.dp),
                            border = BorderStroke(1.dp, semantic.cardBorder),
                            colors = CardDefaults.cardColors(
                                containerColor = semantic.cardElevated
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotBlank()) {
                                            "لم يتم العثور على صنف مطابق لـ «$searchQuery» في المخزن."
                                        } else {
                                            "لا توجد أصناف مطابقة. أضف أصنافاً من قسم المخزون أو استعد من نسخة احتياطية محلية."
                                        },
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (searchQuery.isNotBlank()) {
                                        Button(
                                            onClick = {
                                                val code = searchQuery.trim()
                                                searchQuery = ""
                                                viewModel.promptUnregisteredBarcode(code)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("pos_empty_search_unregistered_barcode_btn")
                                        ) {
                                            Icon(
                                                Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ربط الباركود بسلعة موجودة أو إنشاء سلعة جديدة",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isWideScreen) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredProducts,
                                key = { "pos_prod_${it.id}" },
                                contentType = { "pos_product_card" }
                            ) { product ->
                                val minThreshold = productCustomMinStock[product.id] ?: defaultLowStockThreshold
                                ResponsivePosProductCard(
                                    product = product,
                                    minStockThreshold = minThreshold,
                                    reservedStock = reservedStockMap[product.id] ?: 0,
                                    currencySymbol = currencySymbol,
                                    isCompactCard = true,
                                    searchHighlightQuery = debouncedSearchQuery,
                                    onClickProductName = { selectedProductForImagePreview = product },
                                    onAddUnit = {
                                        if (isPreSaleMode) viewModel.addToPreSaleCart(product, asCarton = false)
                                        else viewModel.addToCart(product, asCarton = false)
                                    },
                                    onAddCarton = {
                                        if (isPreSaleMode) viewModel.addToPreSaleCart(product, asCarton = true)
                                        else viewModel.addToCart(product, asCarton = true)
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(
                                items = filteredProducts,
                                key = { "pos_prod_${it.id}" },
                                contentType = { "pos_product_card" }
                            ) { product ->
                                val minThreshold = productCustomMinStock[product.id] ?: defaultLowStockThreshold
                                ResponsivePosProductCard(
                                    product = product,
                                    minStockThreshold = minThreshold,
                                    reservedStock = reservedStockMap[product.id] ?: 0,
                                    currencySymbol = currencySymbol,
                                    isCompactCard = isCompactCard,
                                    searchHighlightQuery = debouncedSearchQuery,
                                    onClickProductName = { selectedProductForImagePreview = product },
                                    onAddUnit = {
                                        if (isPreSaleMode) viewModel.addToPreSaleCart(product, asCarton = false)
                                        else viewModel.addToCart(product, asCarton = false)
                                    },
                                    onAddCarton = {
                                        if (isPreSaleMode) viewModel.addToPreSaleCart(product, asCarton = true)
                                        else viewModel.addToCart(product, asCarton = true)
                                    }
                                )
                            }
                        }
                    }
                }

                // Active Cart & Checkout Panel
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 3.dp),
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    colors = CardDefaults.cardColors(
                        containerColor = semantic.cardElevated
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "سلة الفاتورة (${cartItems.sumOf { it.effectiveUnitsDeducted }})",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isCompactCartPanel) 12.5.sp else 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (cartItems.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                if (isPreSaleMode) viewModel.clearPreSaleCart()
                                                else viewModel.clearCart()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(
                                                text = "إفراغ",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider()

                                if (cartItems.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "السلة فارغة\nاضغط على أي صنف لإضافته",
                                            fontSize = 11.5.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        state = cartLazyListState,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 2.dp)
                                    ) {
                                        itemsIndexed(
                                            items = cartItems,
                                            key = { _, it -> "cart_item_${it.product.id}" },
                                            contentType = { _, _ -> "pos_cart_item" }
                                        ) { index, item ->
                                            val isRecentlyAdded = (item.product.id == lastAddedCartProductId)
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isRecentlyAdded) {
                                                    if (semantic.isDark) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFECFDF5)
                                                } else if (semantic.isDark) {
                                                    MaterialTheme.colorScheme.surface
                                                } else {
                                                    Color.White
                                                },
                                                shadowElevation = if (isRecentlyAdded) 4.5.dp else if (semantic.isDark) 1.dp else 3.dp,
                                                border = BorderStroke(
                                                    if (isRecentlyAdded) 2.2.dp else 1.4.dp,
                                                    if (isRecentlyAdded) Color(0xFF10B981)
                                                    else if (item.hasAnyDebt) Color(0xFFD97706)
                                                    else if (semantic.isDark) semantic.cardBorder.copy(alpha = 0.85f)
                                                    else Color(0xFFCBD5E1)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min)
                                                ) {
                                                    // Vertical accent indicator strip
                                                    Box(
                                                        modifier = Modifier
                                                            .width(if (isRecentlyAdded) 6.dp else 5.dp)
                                                            .fillMaxHeight()
                                                            .background(
                                                                if (isRecentlyAdded) Color(0xFF10B981)
                                                                else if (item.hasAnyDebt) Color(0xFFD97706)
                                                                else MaterialTheme.colorScheme.primary
                                                            )
                                                    )

                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 9.dp, vertical = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        // Top header row: Item index badge + "آخر إضافة" tag + Remove button
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
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = (if (isRecentlyAdded) Color(0xFF10B981) else if (item.hasAnyDebt) Color(0xFFD97706) else MaterialTheme.colorScheme.primary).copy(alpha = 0.14f)
                                                                ) {
                                                                    Text(
                                                                        text = "#${index + 1}",
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isRecentlyAdded) Color(0xFF10B981) else if (item.hasAnyDebt) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                                if (isRecentlyAdded) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(6.dp),
                                                                        color = Color(0xFF10B981).copy(alpha = 0.20f),
                                                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.8f))
                                                                    ) {
                                                                        Text(
                                                                            text = "آخر إضافة ✓",
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.ExtraBold,
                                                                            color = Color(0xFF10B981),
                                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Surface(
                                                                onClick = {
                                                                    if (isPreSaleMode) {
                                                                        viewModel.adjustPreSaleCartItemUnits(item.product.id, -item.quantity)
                                                                    } else {
                                                                        viewModel.removeCartItem(item.product.id)
                                                                    }
                                                                },
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Icon(
                                                                        Icons.Default.Close,
                                                                        contentDescription = "حذف السلعة",
                                                                        tint = MaterialTheme.colorScheme.error,
                                                                        modifier = Modifier.size(13.dp)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // Full product name: Spans full width of card for clean, harmonious reading
                                                        Text(
                                                            text = item.product.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.5.sp,
                                                            maxLines = Int.MAX_VALUE,
                                                            softWrap = true,
                                                            overflow = TextOverflow.Clip,
                                                            lineHeight = 17.sp,
                                                            color = if (semantic.isDark) {
                                                                MaterialTheme.colorScheme.onSurface
                                                            } else {
                                                                Color(0xFF0F172A)
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    val liveProd = products.find { it.id == item.product.id } ?: item.product
                                                                    selectedProductForImagePreview = liveProd
                                                                }
                                                                .testTag("pos_cart_product_name_${item.product.id}")
                                                        )

                                                        // Bottom row: Total price + Stepper controls (minus button only, removed + button)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "${String.format(Locale.US, "%.2f", item.lineSubtotal)} $currencySymbol",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                maxLines = 1
                                                            )
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Surface(
                                                                    onClick = {
                                                                        if (isPreSaleMode) viewModel.adjustPreSaleCartItemUnits(item.product.id, -1) else viewModel.adjustCartItemUnits(item.product.id, -1)
                                                                    },
                                                                    shape = RoundedCornerShape(7.dp),
                                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                                    border = BorderStroke(
                                                                        1.2.dp,
                                                                        if (semantic.isDark) MaterialTheme.colorScheme.outlineVariant
                                                                        else Color(0xFFCBD5E1)
                                                                    ),
                                                                    modifier = Modifier.size(26.dp)
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Icon(
                                                                            Icons.Default.Remove,
                                                                            contentDescription = "إنقاص",
                                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = if (semantic.isDark) MaterialTheme.colorScheme.surfaceContainerHighest else Color(0xFFF1F5F9),
                                                                    modifier = Modifier.padding(horizontal = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "${item.effectiveUnitsDeducted}",
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        fontSize = 12.sp,
                                                                        color = if (semantic.isDark) {
                                                                            MaterialTheme.colorScheme.onSurface
                                                                        } else {
                                                                            Color(0xFF0F172A)
                                                                        },
                                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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

                            // Bottom Checkout Controls (Customer Selector + Net Total + Checkout Button)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                HorizontalDivider()

                                // Responsive Capsule Customer Selector Button
                                OutlinedButton(
                                    onClick = { showCustomerPicker = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(
                                        1.2.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (semantic.isDark) 0.55f else 0.78f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(if (isCompactCartPanel) 14.dp else 16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedCustomer?.let { "العميل: ${it.name}" }
                                            ?: if (isCompactCartPanel) "تحديد العميل (نقدي/آجل)"
                                            else "اختيار عميل (اختياري للنقدي / إلزامي للآجل)",
                                        fontSize = if (isCompactCartPanel) 10.5.sp else 11.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Coupon / Promotion Quick Pill or Button
                                if (!isPreSaleMode) {
                                    if (appliedCoupon != null) {
                                        Surface(
                                            onClick = { showCouponEntryDialog = true },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF059669).copy(alpha = if (semantic.isDark) 0.20f else 0.12f),
                                            border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth().height(32.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Icon(
                                                        Icons.Default.LocalOffer,
                                                        contentDescription = null,
                                                        tint = Color(0xFF059669),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "${appliedCoupon?.code}: -${String.format(Locale.US, "%.2f", promotionalDiscountAmount)} $currencySymbol",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF059669),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.removeAppliedCoupon() },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "إزالة الكوبون",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(
                                            onClick = { showCouponEntryDialog = true },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth().height(30.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.LocalOffer,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "كوبون خصم / عروض ترويجية",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                if (discountAmount > 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "المجموع الأصلي:",
                                            fontSize = if (isCompactCartPanel) 10.5.sp else 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", rawSubtotal)} $currencySymbol",
                                            fontSize = if (isCompactCartPanel) 10.5.sp else 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "إجمالي الخصومات:",
                                            fontSize = if (isCompactCartPanel) 10.5.sp else 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                        Text(
                                            text = "-${String.format(Locale.US, "%.2f", discountAmount)} $currencySymbol",
                                            fontSize = if (isCompactCartPanel) 11.sp else 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }

                                // Net Total Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الإجمالي الصافي:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompactCartPanel) 11.5.sp else 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", finalTotal)} $currencySymbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isCompactCartPanel) 14.5.sp else 17.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }

                                // Opens Checkout Review or Pre-Sale Save
                                if (!isPreSaleMode) {
                                    ModernGradientButton(
                                        text = if (isCompactCartPanel) "إتمام البيع والفاتورة" else "إتمام البيع وإصدار الفاتورة",
                                        onClick = { showCheckoutReviewSheet = true },
                                        enabled = cartItems.isNotEmpty(),
                                        icon = Icons.Default.ShoppingCartCheckout,
                                        badgeText = if (cartItems.isNotEmpty()) "${cartItems.sumOf { it.effectiveUnitsDeducted }}" else null,
                                        gradientColors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        ),
                                        shape = CircleShape,
                                        height = 48.dp,
                                        fontSize = if (isCompactCartPanel) 12.sp else 14.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pos_complete_checkout_btn")
                                    )
                                } else {
                                    ModernGradientButton(
                                        text = if (isCompactCartPanel) "حفظ كطلب بيع مسبق" else "حفظ كطلب بيع مسبق للمستودع",
                                        onClick = {
                                            if (selectedCustomer == null) {
                                                showCustomerPicker = true
                                                Toast.makeText(context, "يرجى اختيار العميل أولاً لربط الطلب المسبق به", Toast.LENGTH_SHORT).show()
                                            } else {
                                                showPreSaleConfirmModal = true
                                            }
                                        },
                                        enabled = cartItems.isNotEmpty(),
                                        icon = Icons.Default.EventNote,
                                        badgeText = if (cartItems.isNotEmpty()) "${cartItems.sumOf { it.effectiveUnitsDeducted }}" else null,
                                        gradientColors = listOf(
                                            Color(0xFF047857),
                                            Color(0xFF10B981)
                                        ),
                                        shape = CircleShape,
                                        height = 48.dp,
                                        fontSize = if (isCompactCartPanel) 12.sp else 14.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("pos_save_pre_sale_btn")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSearchBarcodeScanner) {
        SimpleBarcodeCaptureDialog(
            title = "مسح باركود بالكاميرا لتصفية الصنف",
            onBarcodeScanned = { scannedCode ->
                focusManager.clearFocus()
                keyboardController?.hide()
                showSearchBarcodeScanner = false
                val code = scannedCode.trim()
                if (code.isNotEmpty()) {
                    searchQuery = code
                }
            },
            onDismiss = {
                focusManager.clearFocus()
                keyboardController?.hide()
                showSearchBarcodeScanner = false
            }
        )
    }

    if (showBarcodeDialog) {
        PosCameraBarcodeScannerDialog(
            viewModel = viewModel,
            onDismiss = { showBarcodeDialog = false }
        )
    }

    if (showCheckoutReviewSheet && cartItems.isNotEmpty()) {
        PosCheckoutReviewSheet(
            viewModel = viewModel,
            onDismiss = { showCheckoutReviewSheet = false },
            onOpenCustomerPicker = { showCustomerPicker = true },
            onInvoiceFinalizedAndPrint = { savedInvoice ->
                showCheckoutReviewSheet = false
                activePreviewInvoice = savedInvoice
                showInvoiceReceiptModal = true
            },
            onPreviewInvoice = { previewInvoice ->
                activePreviewInvoice = previewInvoice
                showInvoiceReceiptModal = true
            }
        )
    }

    if (showCustomerPicker) {
        PosCustomerPickerDialog(
            customers = customers,
            selectedCustomer = selectedCustomer,
            onSelectCustomer = { customer ->
                if (isPreSaleMode) {
                    viewModel.selectPreSaleCustomer(customer)
                } else {
                    viewModel.selectCustomerForPos(customer)
                }
                showCustomerPicker = false
            },
            onCreateNewCustomer = { name, phone, initialDebt ->
                viewModel.createAndSelectCustomerForPos(
                    name = name,
                    phone = phone,
                    openingBalanceDebt = initialDebt
                ) { newCust ->
                    if (isPreSaleMode) {
                        viewModel.selectPreSaleCustomer(newCust)
                    }
                    showCustomerPicker = false
                }
            },
            onDismiss = { showCustomerPicker = false }
        )
    }


    if (showPreSaleConfirmModal && isPreSaleMode) {
        val tomorrowDate = remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 86400000L))
        }
        val dayAfterDate = remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 172800000L))
        }

        AlertDialog(
            onDismissRequest = { showPreSaleConfirmModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تأكيد حفظ طلب البيع المسبق",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF047857).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF047857).copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "العميل: ${selectedCustomer?.name ?: "عميل غير محدد"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "الأصناف المحجوزة: ${cartItems.size} صنف (${cartItems.sumOf { it.effectiveUnitsDeducted }} قطعة)",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "الإجمالي: ${String.format(Locale.US, "%.2f", finalTotal)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF047857)
                            )
                        }
                    }

                    Text("موعد التسليم المتوقع من المستودع:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (preSaleDeliveryDaysChoice == 1) Color(0xFF047857).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (preSaleDeliveryDaysChoice == 1) Color(0xFF047857) else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { preSaleDeliveryDaysChoice = 1 }
                        ) {
                            Text(
                                text = "غداً\n$tomorrowDate",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (preSaleDeliveryDaysChoice == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (preSaleDeliveryDaysChoice == 1) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (preSaleDeliveryDaysChoice == 2) Color(0xFF047857).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (preSaleDeliveryDaysChoice == 2) Color(0xFF047857) else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { preSaleDeliveryDaysChoice = 2 }
                        ) {
                            Text(
                                text = "بعد يومين\n$dayAfterDate",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (preSaleDeliveryDaysChoice == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (preSaleDeliveryDaysChoice == 2) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = preSaleDeliveryNotesInput,
                        onValueChange = { preSaleDeliveryNotesInput = it },
                        placeholder = { Text("ملاحظات للمستودع أو التوصيل (اختياري)...", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Text(
                        text = "ℹ️ تنبيه: لن يتم خصم البضاعة من المخزون الآن. يتم حجز الكميات مؤقتاً حتى يقوم مسؤول المستودع بتسليمها وخروجها غداً.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetDate = if (preSaleDeliveryDaysChoice == 2) {
                            System.currentTimeMillis() + 172800000L
                        } else {
                            System.currentTimeMillis() + 86400000L
                        }
                        viewModel.savePreSaleOrder(
                            expectedDeliveryDate = targetDate,
                            notes = preSaleDeliveryNotesInput,
                            paymentMethod = "CASH",
                            onSuccess = { savedOrder ->
                                showPreSaleConfirmModal = false
                                preSaleDeliveryNotesInput = ""
                                activePreviewInvoice = savedOrder
                                showInvoiceReceiptModal = true
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857))
                ) {
                    Text("تأكيد وحفظ الطلب المسبق", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreSaleConfirmModal = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    val invoiceToShow = activePreviewInvoice ?: lastInvoice
    if (showInvoiceReceiptModal && invoiceToShow != null) {
        InvoiceTemplatePreviewDialog(
            viewModel = viewModel,
            archivedInvoice = invoiceToShow,
            isPreSavePreview = false,
            onDismiss = {
                showInvoiceReceiptModal = false
                activePreviewInvoice = null
            }
        )
    }

    if (showLowStockDialog) {
        LowStockItemsListDialog(
            lowStockProducts = lowStockProducts,
            defaultLowStockThreshold = defaultLowStockThreshold,
            productCustomMinStock = productCustomMinStock,
            storeName = storeName,
            onUpdateDefaultThreshold = { viewModel.updateDefaultLowStockThreshold(it) },
            onUpdateProductCustomThreshold = { prodId, customMin ->
                viewModel.setCustomProductMinStock(prodId, customMin)
            },
            onQuickRestock = { product, addedUnits ->
                viewModel.quickRestockProduct(product, addedUnits)
            },
            onOpenFullReorderManagement = onOpenReorderAlertsScreen?.let { openFn ->
                {
                    showLowStockDialog = false
                    openFn()
                }
            },
            onDismiss = { showLowStockDialog = false }
        )
    }

    unregisteredScannedBarcode?.let { unregisteredCode ->
        UnregisteredBarcodeActionDialog(
            scannedBarcode = unregisteredCode,
            products = products,
            currencySymbol = currencySymbol,
            onLinkToExistingProduct = { prod, asCartonBarcode ->
                viewModel.linkBarcodeToExistingProduct(
                    product = prod,
                    barcode = unregisteredCode,
                    asCartonBarcode = asCartonBarcode,
                    addToCartImmediately = true
                )
            },
            onCreateNewProduct = { newItem ->
                viewModel.createNewProductFromComprehensiveModal(newItem, addToPosCartImmediately = true)
            },
            onQuickCreateAndAddToCart = { name, unitPrice, purchasePrice, asCartonBarcode, unitsPerCarton, quantityToAddInCart ->
                viewModel.quickCreateProductAndAddToCart(
                    name = name,
                    unitPrice = unitPrice,
                    purchasePrice = purchasePrice,
                    barcode = unregisteredCode,
                    asCartonBarcode = asCartonBarcode,
                    unitsPerCarton = unitsPerCarton,
                    quantityToAddInCart = quantityToAddInCart
                )
            },
            onDismiss = {
                viewModel.dismissUnregisteredBarcodePrompt()
            }
        )
    }

    selectedProductForImagePreview?.let { previewTarget ->
        val liveProduct = products.find { it.id == previewTarget.id } ?: previewTarget
        ProductFullScreenImageDialog(
            product = liveProduct,
            currencySymbol = currencySymbol,
            onUpdateProductImage = { newImagePath ->
                val updated = liveProduct.copy(imagePath = newImagePath)
                viewModel.saveProduct(updated)
                selectedProductForImagePreview = updated
            },
            onOpenMovementHistory = {
                selectedProductForImagePreview = null
                selectedProductForMovementHistory = liveProduct
            },
            onDismiss = { selectedProductForImagePreview = null }
        )
    }

    selectedProductForMovementHistory?.let { historyTarget ->
        val sales by viewModel.sales.collectAsState()
        val purchaseInvoices by viewModel.purchaseInvoices.collectAsState()
        val activityLogs by viewModel.activityLogs.collectAsState()
        val liveProduct = products.find { it.id == historyTarget.id } ?: historyTarget
        ProductMovementHistoryDialog(
            product = liveProduct,
            sales = sales,
            purchaseInvoices = purchaseInvoices,
            activityLogs = activityLogs,
            currencySymbol = currencySymbol,
            onOpenProductImage = {
                selectedProductForMovementHistory = null
                selectedProductForImagePreview = liveProduct
            },
            onDismiss = { selectedProductForMovementHistory = null }
        )
    }

    if (showCouponEntryDialog) {
        CouponEntryDialog(
            viewModel = viewModel,
            onDismiss = { showCouponEntryDialog = false }
        )
    }
}

@Composable
private fun CouponEntryDialog(
    viewModel: AntarSalesViewModel,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val promotions by viewModel.promotions.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val availableCoupons = remember(promotions) {
        promotions.filter { it.isActive && !it.isExpired }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "كوبونات الخصم والعروض الترويجية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (appliedCoupon != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF059669).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "الكوبون المطبق: ${appliedCoupon?.code ?: appliedCoupon?.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF059669)
                                )
                                Text(
                                    text = appliedCoupon?.formattedSummary() ?: "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    viewModel.removeAppliedCoupon()
                                }
                            ) {
                                Text("إلغاء", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.uppercase()
                        errorMessage = null
                    },
                    label = { Text("رمز الكوبون (Promo Code)") },
                    placeholder = { Text("مثال: WELCOME10 أو SAVE500") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (availableCoupons.isNotEmpty()) {
                    Text(
                        text = "العروض والكوبونات المتاحة حالياً بالمتجر:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    availableCoupons.forEach { promo ->
                        Surface(
                            onClick = {
                                if (promo.isCoupon) {
                                    val res = viewModel.applyCouponCode(promo.code ?: "")
                                    if (res.isSuccess) {
                                        onDismiss()
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.message
                                    }
                                } else {
                                    viewModel.applyPromotionDirectly(promo)
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = promo.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = promo.formattedSummary(),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (promo.code != null) {
                                        Text(
                                            text = "الكود: ${promo.code}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        if (promo.isCoupon) {
                                            val res = viewModel.applyCouponCode(promo.code ?: "")
                                            if (res.isSuccess) {
                                                onDismiss()
                                            } else {
                                                errorMessage = res.exceptionOrNull()?.message
                                            }
                                        } else {
                                            viewModel.applyPromotionDirectly(promo)
                                            onDismiss()
                                        }
                                    }
                                ) {
                                    Text("تطبيق", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val res = viewModel.applyCouponCode(codeInput)
                    if (res.isSuccess) {
                        onDismiss()
                    } else {
                        errorMessage = res.exceptionOrNull()?.message
                    }
                },
                enabled = codeInput.isNotBlank()
            ) {
                Text("تطبيق الكود")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun PosCustomerPickerDialog(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onSelectCustomer: (Customer?) -> Unit,
    onCreateNewCustomer: (name: String, phone: String, initialDebt: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var newCustomerOpeningDebt by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isCreatingNew) "إنشاء عميل جديد للفاتورة" else "اختيار العميل أو إنشاء جديد",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { isCreatingNew = false },
                        shape = RoundedCornerShape(50),
                        color = if (!isCreatingNew) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (!isCreatingNew) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_customer_tab_select")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (!isCreatingNew) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "العملاء (${customers.size})",
                                fontSize = 12.sp,
                                fontWeight = if (!isCreatingNew) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            isCreatingNew = true
                            if (searchQuery.isNotBlank() && newCustomerName.isBlank()) {
                                newCustomerName = searchQuery
                            }
                        },
                        shape = RoundedCornerShape(50),
                        color = if (isCreatingNew) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isCreatingNew) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_customer_tab_create")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = if (isCreatingNew) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ عميل جديد",
                                fontSize = 12.sp,
                                fontWeight = if (isCreatingNew) FontWeight.ExtraBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        },
        text = {
            if (isCreatingNew) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newCustomerName,
                        onValueChange = {
                            newCustomerName = it
                            if (it.isNotBlank()) nameError = false
                        },
                        label = { Text("اسم العميل *") },
                        placeholder = { Text("مثال: محمد أحمد") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        isError = nameError,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_new_customer_name_input")
                    )
                    if (nameError) {
                        Text(
                            text = "يرجى إدخال اسم العميل",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedTextField(
                        value = newCustomerPhone,
                        onValueChange = { newCustomerPhone = it },
                        label = { Text("رقم الهاتف (اختياري)") },
                        placeholder = { Text("05xxxxxxxx") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_new_customer_phone_input")
                    )

                    OutlinedTextField(
                        value = newCustomerOpeningDebt,
                        onValueChange = { newCustomerOpeningDebt = it },
                        label = { Text("رصيد افتتاحي / مديونية سابقة (اختياري)") },
                        placeholder = { Text("0.00") },
                        leadingIcon = { Icon(Icons.Default.CreditScore, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_new_customer_debt_input")
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Text(
                            text = "سيتم حفظ العميل الجديد في قاعدة البيانات وربطه بهذه الفاتورة تلقائياً.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isWalkInSelected = selectedCustomer == null
                    Surface(
                        onClick = { onSelectCustomer(null) },
                        shape = RoundedCornerShape(50),
                        color = if (isWalkInSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isWalkInSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_walkin_customer_btn")
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "عميل نقدي عام (بدون حساب)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isWalkInSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "محدد",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث باسم العميل أو الهاتف...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_customer_search_input")
                    )

                    if (filteredCustomers.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (customers.isEmpty()) "لا يوجد عملاء مسجلون حالياً"
                                    else "لا يوجد عميل مطابق لـ \"$searchQuery\"",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        if (searchQuery.isNotBlank()) {
                                            newCustomerName = searchQuery
                                        }
                                        isCreatingNew = true
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("pos_empty_create_customer_btn")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "إضافة \"$searchQuery\" كعميل جديد"
                                        else "إنشاء عميل جديد الآن",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredCustomers, key = { it.id }) { customer ->
                                val isSelected = selectedCustomer?.id == customer.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCustomer(customer) }
                                        .testTag("pos_customer_item_${customer.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = customer.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (customer.phone.isNotBlank()) {
                                                Text(
                                                    text = customer.phone,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (customer.balanceDebt > 0) {
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                            } else {
                                                Color(0xFF10B981).copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = if (customer.balanceDebt > 0) {
                                                    "مديونية: ${String.format(Locale.US, "%.2f", customer.balanceDebt)}"
                                                } else {
                                                    "خالص"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (customer.balanceDebt > 0) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    Color(0xFF10B981)
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                Button(
                    onClick = {
                        if (newCustomerName.isBlank()) {
                            nameError = true
                        } else {
                            onCreateNewCustomer(
                                newCustomerName,
                                newCustomerPhone,
                                newCustomerOpeningDebt.toDoubleOrNull() ?: 0.0
                            )
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("pos_save_new_customer_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("حفظ وتحديد للفاتورة", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = { isCreatingNew = true },
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("عميل جديد", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isCreatingNew) {
                        isCreatingNew = false
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (isCreatingNew) "رجوع للقائمة" else "إغلاق")
            }
        }
    )
}

@Composable
private fun ResponsivePosProductCard(
    product: Product,
    minStockThreshold: Int,
    reservedStock: Int = 0,
    currencySymbol: String,
    isCompactCard: Boolean,
    searchHighlightQuery: String = "",
    onClickProductName: () -> Unit = {},
    onAddUnit: () -> Unit,
    onAddCarton: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val isLow = product.stock <= minStockThreshold
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddUnit() }
            .testTag("product_pos_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 3.dp),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isLow) semantic.dangerRed.copy(alpha = 0.55f) else semantic.cardBorder
        ),
        colors = CardDefaults.cardColors(
            containerColor = semantic.cardElevated
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClickProductName() }
                    ) {
                        HighlightedText(
                            text = product.name,
                            query = searchHighlightQuery,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = if (isCompactCard) 13.sp else 14.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = Int.MAX_VALUE,
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.testTag("pos_product_name_${product.id}")
                        )
                        val trimmedCat = product.category.trim()
                        val isGeneralCat = trimmedCat.isBlank() ||
                            trimmedCat == "عام" ||
                            trimmedCat == "فئة عام" ||
                            trimmedCat == "بدون فئة" ||
                            trimmedCat.equals("general", ignoreCase = true) ||
                            trimmedCat.equals("default", ignoreCase = true)
                        if (!isGeneralCat) {
                            HighlightedText(
                                text = trimmedCat,
                                query = searchHighlightQuery,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (reservedStock > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = semantic.debtAmber.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = "محجوز: $reservedStock",
                                    color = semantic.debtAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (!isLow) Color(0xFF10B981).copy(alpha = 0.15f)
                            else Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = BorderStroke(
                                1.dp,
                                if (!isLow) Color(0xFF10B981).copy(alpha = 0.65f)
                                else Color(0xFFEF4444).copy(alpha = 0.65f)
                            )
                        ) {
                            Text(
                                text = if (isLow) "⚠️ مخزون: ${product.stock}" else "مخزون: ${product.stock}",
                            color = if (!isLow) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    }
                }

                if (isCompactCard) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", product.price)} $currencySymbol",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        maxLines = 1
                    )
                    val effectiveCartonUnits = product.unitsPerCarton.coerceAtLeast(1)
                    val cartonLabel = if (effectiveCartonUnits > 1) "كرتونة ($effectiveCartonUnits)" else "كرتونة"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onAddCarton,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = if (semantic.isDark) 0.55f else 0.78f)
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = cartonLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onAddUnit,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "قطعة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    val effectiveCartonUnits = product.unitsPerCarton.coerceAtLeast(1)
                    val cartonLabel = if (effectiveCartonUnits > 1) "كرتونة ($effectiveCartonUnits)" else "كرتونة"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", product.price)} $currencySymbol",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onAddCarton,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    1.2.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (semantic.isDark) 0.55f else 0.78f)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cartonLabel,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Button(
                                onClick = onAddUnit,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "قطعة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
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
fun InvoicePreviewModal(
    invoice: ArchivedInvoice,
    storeName: String,
    currencySymbol: String = "د.ج",
    storePhone: String = "",
    storeLogoImagePath: String = "",
    storeLogoCustomText: String = "",
    onDismiss: () -> Unit
) {
    val dateFormatted = remember(invoice.dateMillis) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(invoice.dateMillis))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (storeLogoImagePath.isNotBlank() || storeLogoCustomText.isNotBlank()) {
                    InvoiceStoreLogoHeader(
                        storeLogoImagePath = storeLogoImagePath,
                        storeLogoCustomText = storeLogoCustomText,
                        maxHeightDp = 44.dp
                    )
                } else {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(storeName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                if (storePhone.isNotBlank()) {
                    Text("هاتف: $storePhone", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("فاتورة مبيعات رقم: ${invoice.invoiceId}", fontSize = 13.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("التاريخ: $dateFormatted", fontSize = 12.sp)
                Text("العميل: ${invoice.customerName}", fontSize = 12.sp)
                Text(
                    "طريقة الدفع: ${if (invoice.isDebt) "آجل (مديونية)" else invoice.paymentMethod}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                invoice.items.forEachIndexed { idx, sale ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${sale.productName} × ${sale.quantitySold}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("${String.format(Locale.US, "%.2f", sale.totalPrice)} $currencySymbol", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (idx < invoice.items.lastIndex) {
                        Text(
                            text = "- - - - - - - - - - - - - - - - - - - -",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الإجمالي النهائي:", fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${String.format(Locale.US, "%.2f", invoice.subtotal)} $currencySymbol",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("تم")
            }
        }
    )
}

@Composable
fun InventoryScreen(
    viewModel: AntarSalesViewModel,
    onOpenReorderAlertsScreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val semantic = LocalAFStoreSemanticColors.current
    val products by viewModel.products.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val defaultLowStockThreshold by viewModel.defaultLowStockThreshold.collectAsState()
    val productCustomMinStock by viewModel.productCustomMinStock.collectAsState()
    val unregisteredScannedBarcode by viewModel.unregisteredScannedBarcode.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val reservedStockMap by viewModel.reservedStockMap.collectAsState()
    val canViewCostPrices by viewModel.canViewCostPrices.collectAsState()

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isWideScreen = screenWidthDp >= 600

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    var showLowStockOnly by rememberSaveable { mutableStateOf(false) }
    var showLowStockDialog by remember { mutableStateOf(false) }
    val activityLogs by viewModel.activityLogs.collectAsState()
    var showInventoryBarcodeScanner by remember { mutableStateOf(false) }
    var showVignetteScanner by remember { mutableStateOf(false) }
    var showInventoryAuditDialog by remember { mutableStateOf(false) }
    var showInventoryPdfReportDialog by remember { mutableStateOf(false) }
    var showInventoryCsvExportDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var selectedProductForImagePreview by remember { mutableStateOf<Product?>(null) }
    var selectedProductForMovementHistory by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            debouncedSearchQuery = ""
        } else {
            delay(90L)
            debouncedSearchQuery = trimmed
        }
    }

    val filtered = remember(products, debouncedSearchQuery, showLowStockOnly, defaultLowStockThreshold, productCustomMinStock) {
        val q = debouncedSearchQuery.trim()
        products.filter { p ->
            val matchesQuery = q.isEmpty() ||
                p.name.contains(q, ignoreCase = true) ||
                p.barcode.contains(q, ignoreCase = true) ||
                p.cartonBarcode.contains(q, ignoreCase = true)
            val minThreshold = productCustomMinStock[p.id] ?: defaultLowStockThreshold
            val matchesStock = !showLowStockOnly || p.stock <= minThreshold
            matchesQuery && matchesStock
        }
    }

    val totalInventoryValue = remember(products) {
        products.sumOf { it.purchasePrice * it.stock }
    }
    val totalExpectedSales = remember(products) {
        products.sumOf { it.price * it.stock }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingProduct = null
                    showProductDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 10.dp),
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة صنف", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("صنف جديد", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Interactive Automatic Low-Stock Alert Banner in Inventory
            AnimatedVisibility(visible = lowStockProducts.isNotEmpty()) {
                LowStockAlertBanner(
                    lowStockProducts = lowStockProducts,
                    onClick = { showLowStockDialog = true },
                    testTag = "inventory_low_stock_alert_banner"
                )
            }

            // Top Header Title & PDF / CSV Report Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إدارة المخزون (${filtered.size}/${products.size})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { showInventoryAuditDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("السجل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    com.example.ui.components.CsvExportHeaderButton(
                        title = "CSV",
                        tag = "inventory_csv_export_btn",
                        onClick = { showInventoryCsvExportDialog = true }
                    )
                    UniversalReportHeaderButton(
                        title = "PDF",
                        tag = "inventory_pdf_report_btn",
                        onClick = { showInventoryPdfReportDialog = true }
                    )
                }
            }

            // Compact 1-line Stats Summary Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("الأصناف:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${products.size}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    if (canViewCostPrices) {
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("التكلفة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.0f", totalInventoryValue)} $currencySymbol", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("البيع:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format(Locale.US, "%.0f", totalExpectedSales)} $currencySymbol", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF10B981))
                    }
                }
            }

            // Full-Width Search Input Row (Guaranteed No Squishing!)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم، باركود، أو سعر المنتج...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        val code = searchQuery.trim()
                        if (code.isNotEmpty()) {
                            val found = products.find {
                                it.barcode.equals(code, ignoreCase = true) ||
                                    (it.cartonBarcode.isNotBlank() && it.cartonBarcode.equals(code, ignoreCase = true))
                            }
                            if (found == null) {
                                searchQuery = ""
                                viewModel.promptUnregisteredBarcode(code)
                            }
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // Horizontally Scrollable Tools & Quick Filters Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showInventoryBarcodeScanner = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("inventory_scan_barcode_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "مسح باركود في المخزون",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("ماسح الباركود", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Surface(
                    onClick = { showVignetteScanner = true },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.testTag("inventory_scan_vignette_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "مسح فواتير الشراء والوصولات OCR",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Text("ماسح فواتير ووصولات الشراء (OCR) 📄", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }

                FilterChip(
                    selected = showLowStockOnly,
                    onClick = { showLowStockOnly = !showLowStockOnly },
                    label = {
                        Text(
                            text = "⚠️ النواقص (${lowStockProducts.size})",
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("filter_low_stock_chip")
                )
            }

            if (isWideScreen) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filtered,
                        key = { "inv_prod_${it.id}" },
                        contentType = { "inventory_product_card" }
                    ) { product ->
                        val minThreshold = productCustomMinStock[product.id] ?: defaultLowStockThreshold
                        val isLow = product.stock <= minThreshold
                        val hasCustomMin = productCustomMinStock.containsKey(product.id)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 3.dp),
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = if (isLow) semantic.dangerRed.copy(alpha = 0.55f) else semantic.cardBorder
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = semantic.cardElevated
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = Int.MAX_VALUE,
                                        softWrap = true,
                                        modifier = Modifier
                                            .clickable { selectedProductForImagePreview = product }
                                            .testTag("inventory_product_name_${product.id}")
                                    )
                                    Text(
                                        text = if (canViewCostPrices) {
                                            "التكلفة: ${product.purchasePrice} | البيع: ${product.price} $currencySymbol | الربح: ${String.format(Locale.US, "%.1f", product.profitPerUnit)}"
                                        } else {
                                            "سعر البيع: ${product.price} $currencySymbol"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "الحد الأدنى للطلب: $minThreshold وحدة${if (hasCustomMin) " (مخصص)" else " (افتراضي)"} • الكرتونة: ${product.unitsPerCarton} ق",
                                        fontSize = 11.sp,
                                        fontWeight = if (isLow) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isLow) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val reservedUnits = reservedStockMap[product.id] ?: 0
                                    if (reservedUnits > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = semantic.debtAmber.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.65f))
                                        ) {
                                            Text(
                                                text = "محجوز: $reservedUnits ق",
                                                color = semantic.debtAmber,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.5.sp,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!isLow) Color(0xFF10B981).copy(alpha = 0.16f)
                                        else Color(0xFFEF4444).copy(alpha = 0.16f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (!isLow) Color(0xFF10B981).copy(alpha = 0.65f)
                                            else Color(0xFFEF4444).copy(alpha = 0.65f)
                                        )
                                    ) {
                                        Text(
                                            text = if (isLow) "⚠️ ${product.stock} وحدة" else "${product.stock} وحدة",
                                            color = if (!isLow) Color(0xFF10B981) else Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        onClick = { selectedProductForMovementHistory = product },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("inventory_product_history_${product.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.History,
                                                contentDescription = "سجل حركة الصنف",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            editingProduct = product
                                            showProductDialog = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "تعديل",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = { viewModel.deleteProduct(product) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.75f)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filtered,
                        key = { "inv_prod_${it.id}" },
                        contentType = { "inventory_product_card" }
                    ) { product ->
                        val minThreshold = productCustomMinStock[product.id] ?: defaultLowStockThreshold
                        val isLow = product.stock <= minThreshold
                        val hasCustomMin = productCustomMinStock.containsKey(product.id)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 3.dp),
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = if (isLow) semantic.dangerRed.copy(alpha = 0.55f) else semantic.cardBorder
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = semantic.cardElevated
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = Int.MAX_VALUE,
                                        softWrap = true,
                                        modifier = Modifier
                                            .clickable { selectedProductForImagePreview = product }
                                            .testTag("inventory_product_name_${product.id}")
                                    )
                                    Text(
                                        text = if (canViewCostPrices) {
                                            "التكلفة: ${product.purchasePrice} | البيع: ${product.price} $currencySymbol | الربح: ${String.format(Locale.US, "%.1f", product.profitPerUnit)}"
                                        } else {
                                            "سعر البيع: ${product.price} $currencySymbol"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "الحد الأدنى للطلب: $minThreshold وحدة${if (hasCustomMin) " (مخصص)" else " (افتراضي)"} • الكرتونة: ${product.unitsPerCarton} ق",
                                        fontSize = 11.sp,
                                        fontWeight = if (isLow) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isLow) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val reservedUnits = reservedStockMap[product.id] ?: 0
                                    if (reservedUnits > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = semantic.debtAmber.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.65f))
                                        ) {
                                            Text(
                                                text = "محجوز: $reservedUnits ق",
                                                color = semantic.debtAmber,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.5.sp,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!isLow) Color(0xFF10B981).copy(alpha = 0.16f)
                                        else Color(0xFFEF4444).copy(alpha = 0.16f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (!isLow) Color(0xFF10B981).copy(alpha = 0.65f)
                                            else Color(0xFFEF4444).copy(alpha = 0.65f)
                                        )
                                    ) {
                                        Text(
                                            text = if (isLow) "⚠️ ${product.stock} وحدة" else "${product.stock} وحدة",
                                            color = if (!isLow) Color(0xFF10B981) else Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        onClick = { selectedProductForMovementHistory = product },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("inventory_product_history_${product.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.History,
                                                contentDescription = "سجل حركة الصنف",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            editingProduct = product
                                            showProductDialog = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "تعديل",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = { viewModel.deleteProduct(product) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.75f)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
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

    if (showProductDialog) {
        ProductEditorDialog(
            initialProduct = editingProduct,
            initialCustomMinStock = editingProduct?.let { productCustomMinStock[it.id] },
            defaultLowStockThreshold = defaultLowStockThreshold,
            onDismiss = { showProductDialog = false },
            onSave = { updated, customMinStock ->
                viewModel.saveProduct(updated, customMinStock)
                showProductDialog = false
            }
        )
    }

    if (showLowStockDialog) {
        LowStockItemsListDialog(
            lowStockProducts = lowStockProducts,
            defaultLowStockThreshold = defaultLowStockThreshold,
            productCustomMinStock = productCustomMinStock,
            storeName = storeName,
            onUpdateDefaultThreshold = { viewModel.updateDefaultLowStockThreshold(it) },
            onUpdateProductCustomThreshold = { prodId, customMin ->
                viewModel.setCustomProductMinStock(prodId, customMin)
            },
            onQuickRestock = { product, addedUnits ->
                viewModel.quickRestockProduct(product, addedUnits)
            },
            onOpenFullReorderManagement = onOpenReorderAlertsScreen?.let { openFn ->
                {
                    showLowStockDialog = false
                    openFn()
                }
            },
            onDismiss = { showLowStockDialog = false }
        )
    }

    if (showInventoryBarcodeScanner) {
        SimpleBarcodeCaptureDialog(
            title = "مسح باركود في المخزون",
            onBarcodeScanned = { scannedCode ->
                focusManager.clearFocus()
                keyboardController?.hide()
                showInventoryBarcodeScanner = false
                val code = scannedCode.trim()
                if (code.isNotEmpty()) {
                    val found = products.find {
                        it.barcode.equals(code, ignoreCase = true) ||
                            (it.cartonBarcode.isNotBlank() && it.cartonBarcode.equals(code, ignoreCase = true))
                    }
                    if (found != null) {
                        searchQuery = code
                    } else {
                        viewModel.promptUnregisteredBarcode(code)
                    }
                }
            },
            onDismiss = {
                focusManager.clearFocus()
                keyboardController?.hide()
                showInventoryBarcodeScanner = false
            }
        )
    }
    if (showVignetteScanner) {
        VignetteScannerDialog(
            onVignetteScanned = { extracted ->
                showVignetteScanner = false
                val matched = products.find { p ->
                    (extracted.productName.isNotBlank() && p.name.isNotBlank() && (
                        p.name.contains(extracted.productName, ignoreCase = true) ||
                        extracted.productName.contains(p.name, ignoreCase = true)
                    )) || (
                        extracted.barcode.isNotBlank() && (
                            p.barcode.equals(extracted.barcode, ignoreCase = true) ||
                            p.cartonBarcode.equals(extracted.barcode, ignoreCase = true)
                        )
                    )
                }
                if (matched != null) {
                    val updated = matched.copy(
                        price = if (extracted.price > 0.0) extracted.price else matched.price,
                        purchasePrice = if (extracted.price > 0.0 && matched.purchasePrice <= 0.0) extracted.price else matched.purchasePrice,
                        barcode = if (extracted.barcode.isNotBlank() && matched.barcode.isBlank()) extracted.barcode else matched.barcode
                    )
                    viewModel.saveProduct(updated)
                } else {
                    editingProduct = Product(
                        name = extracted.productName.ifBlank { "سلعة مستخرجة عبر الفاتورة" },
                        price = extracted.price,
                        purchasePrice = extracted.price,
                        barcode = extracted.barcode
                    )
                    showProductDialog = true
                }
            },
            onDismiss = { showVignetteScanner = false }
        )
    }

    unregisteredScannedBarcode?.let { unregisteredCode ->
        UnregisteredBarcodeActionDialog(
            scannedBarcode = unregisteredCode,
            products = products,
            currencySymbol = currencySymbol,
            onLinkToExistingProduct = { prod, asCartonBarcode ->
                viewModel.linkBarcodeToExistingProduct(
                    product = prod,
                    barcode = unregisteredCode,
                    asCartonBarcode = asCartonBarcode,
                    addToCartImmediately = false
                )
            },
            onCreateNewProduct = { newItem ->
                viewModel.createNewProductFromComprehensiveModal(newItem, addToPosCartImmediately = false)
            },
            onQuickCreateAndAddToCart = { name, unitPrice, purchasePrice, asCartonBarcode, unitsPerCarton, quantityToAddInCart ->
                viewModel.quickCreateProductAndAddToCart(
                    name = name,
                    unitPrice = unitPrice,
                    purchasePrice = purchasePrice,
                    barcode = unregisteredCode,
                    asCartonBarcode = asCartonBarcode,
                    unitsPerCarton = unitsPerCarton,
                    quantityToAddInCart = quantityToAddInCart
                )
            },
            onDismiss = {
                viewModel.dismissUnregisteredBarcodePrompt()
            }
        )
    }

    selectedProductForImagePreview?.let { previewTarget ->
        val liveProduct = products.find { it.id == previewTarget.id } ?: previewTarget
        ProductFullScreenImageDialog(
            product = liveProduct,
            currencySymbol = currencySymbol,
            onUpdateProductImage = { newImagePath ->
                val updated = liveProduct.copy(imagePath = newImagePath)
                viewModel.saveProduct(updated)
                selectedProductForImagePreview = updated
            },
            onOpenMovementHistory = {
                selectedProductForImagePreview = null
                selectedProductForMovementHistory = liveProduct
            },
            onDismiss = { selectedProductForImagePreview = null }
        )
    }

    selectedProductForMovementHistory?.let { historyTarget ->
        val sales by viewModel.sales.collectAsState()
        val purchaseInvoices by viewModel.purchaseInvoices.collectAsState()
        val activityLogs by viewModel.activityLogs.collectAsState()
        val liveProduct = products.find { it.id == historyTarget.id } ?: historyTarget
        ProductMovementHistoryDialog(
            product = liveProduct,
            sales = sales,
            purchaseInvoices = purchaseInvoices,
            activityLogs = activityLogs,
            currencySymbol = currencySymbol,
            onOpenProductImage = {
                selectedProductForMovementHistory = null
                selectedProductForImagePreview = liveProduct
            },
            onDismiss = { selectedProductForMovementHistory = null }
        )
    }

    if (showInventoryPdfReportDialog) {
        val payload = remember(storeName, storePhone, currencySymbol, products) {
            UniversalPdfReportExporter.buildInventoryPayload(
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol,
                products = products
            )
        }
        UniversalSectionPdfReportDialog(
            payload = payload,
            onDismiss = { showInventoryPdfReportDialog = false }
        )
    }

    if (showInventoryAuditDialog) {
        InventoryAuditLogDialog(
            activityLogs = activityLogs,
            storeName = storeName,
            storePhone = storePhone,
            currencySymbol = currencySymbol,
            onDismiss = { showInventoryAuditDialog = false }
        )
    }

    if (showInventoryCsvExportDialog) {
        com.example.ui.components.CsvExportDialog(
            title = "تصدير وجرد المنتجات بصيغة CSV",
            subtitle = "تصدير كامل قائمة المنتجات والأسعار والتكلفة الإجمالية والباركود والعبوات إلى ملف CSV بترميز UTF-8 مع BOM متوافق مع Excel.",
            products = filtered.ifEmpty { products },
            storeName = storeName,
            onDismiss = { showInventoryCsvExportDialog = false }
        )
    }
}

@Composable
private fun LowStockAlertBanner(
    lowStockProducts: List<Product>,
    onClick: () -> Unit,
    testTag: String
) {
    val outOfStockCount = remember(lowStockProducts) { lowStockProducts.count { it.stock <= 0 } }
    val previewNames = remember(lowStockProducts) {
        lowStockProducts.take(3).joinToString(" ، ") { "${it.name} (${it.stock})" }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFEF4444).copy(alpha = 0.14f),
        border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "تنبيه نواقص المخزون",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (outOfStockCount > 0) {
                            "تنبيه المخزون: ${lowStockProducts.size} سلع وصلت للحد الأدنى (منها $outOfStockCount نفدت)"
                        } else {
                            "تنبيه المخزون: ${lowStockProducts.size} سلع وصلت للحد الأدنى للطلب"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = previewNames,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFEF4444)
            ) {
                Text(
                    text = "قائمة النواقص (${lowStockProducts.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun LowStockItemsListDialog(
    lowStockProducts: List<Product>,
    defaultLowStockThreshold: Int,
    productCustomMinStock: Map<Int, Int>,
    storeName: String,
    onUpdateDefaultThreshold: (Int) -> Unit,
    onUpdateProductCustomThreshold: (Int, Int?) -> Unit,
    onQuickRestock: (Product, Int) -> Unit,
    onOpenFullReorderManagement: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var defaultThresholdInput by remember(defaultLowStockThreshold) {
        mutableStateOf(defaultLowStockThreshold.toString())
    }
    var editingProductForThreshold by remember { mutableStateOf<Product?>(null) }
    var customThresholdInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = {
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
                        tint = Color(0xFFEF4444)
                    )
                    Text(
                        text = "قائمة السلع المنخفضة والنواقص",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFEF4444).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${lowStockProducts.size} صنف",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onOpenFullReorderManagement != null) {
                    Button(
                        onClick = onOpenFullReorderManagement,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_full_reorder_alerts_screen_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فتح واجهة إدارة تنبيهات إعادة الطلب وفواتير الشراء",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Global Default Reorder Threshold Config Bar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = defaultThresholdInput,
                            onValueChange = { defaultThresholdInput = it },
                            label = { Text("الحد الأدنى العام للطلب") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("global_low_stock_threshold_input")
                        )
                        Button(
                            onClick = {
                                val parsed = defaultThresholdInput.toIntOrNull()
                                if (parsed != null && parsed >= 1) {
                                    onUpdateDefaultThreshold(parsed)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حفظ الحد العام", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Inline Per-Product Custom Threshold Editor (if active)
                editingProductForThreshold?.let { targetProd ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "تخصيص الحد الأدنى لـ: ${targetProd.name}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customThresholdInput,
                                    onValueChange = { customThresholdInput = it },
                                    placeholder = { Text("الافتراضي: $defaultLowStockThreshold") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        onUpdateProductCustomThreshold(
                                            targetProd.id,
                                            customThresholdInput.trim().toIntOrNull()
                                        )
                                        editingProductForThreshold = null
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("حفظ", fontSize = 12.sp)
                                }
                                TextButton(onClick = { editingProductForThreshold = null }) {
                                    Text("إلغاء", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }

                if (lowStockProducts.isEmpty()) {
                    Text(
                        text = "جميع السلع في المخزون أعلى من الحد الأدنى للطلب حالياً ✅",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(lowStockProducts, key = { it.id }) { prod ->
                            val minReq = productCustomMinStock[prod.id] ?: defaultLowStockThreshold
                            val suggestedOrderQty = ((minReq * 2) - prod.stock).coerceAtLeast(prod.unitsPerCarton.coerceAtLeast(1))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (prod.stock <= 0) Color(0xFFEF4444) else Color(0xFFF59E0B).copy(alpha = 0.6f)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
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
                                        Text(
                                            text = prod.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (prod.stock <= 0) Color(0xFFEF4444).copy(alpha = 0.16f)
                                            else Color(0xFFF59E0B).copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = if (prod.stock <= 0) "نفد المخزون (0)" else "المتوفر: ${prod.stock} ق",
                                                color = if (prod.stock <= 0) Color(0xFFEF4444) else Color(0xFFD97706),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "الحد الأدنى للطلب: $minReq ق • كمية الطلب المقترحة: $suggestedOrderQty ق",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                editingProductForThreshold = prod
                                                customThresholdInput = (productCustomMinStock[prod.id] ?: minReq).toString()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("حد التنبيه ($minReq)", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                val packUnits = prod.unitsPerCarton.coerceAtLeast(1)
                                                val addQty = if (packUnits > 1) packUnits else 10
                                                onQuickRestock(prod, addQty)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(32.dp)
                                        ) {
                                            val packUnits = prod.unitsPerCarton.coerceAtLeast(1)
                                            val label = if (packUnits > 1) "+ عبوة ($packUnits ق)" else "+ 10 وحدات"
                                            Text("تزويد سريع $label", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (lowStockProducts.isNotEmpty()) {
                Button(
                    onClick = {
                        val lines = buildString {
                            appendLine("📋 قائمة نواقص المخزون للطلب - $storeName")
                            appendLine("------------------------")
                            lowStockProducts.forEachIndexed { idx, prod ->
                                val minReq = productCustomMinStock[prod.id] ?: defaultLowStockThreshold
                                val suggested = ((minReq * 2) - prod.stock).coerceAtLeast(prod.unitsPerCarton.coerceAtLeast(1))
                                appendLine("${idx + 1}. ${prod.name} | المتوفر: ${prod.stock} ق (الحد الأدنى: $minReq) | المطلوب: $suggested ق")
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, lines)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة قائمة النواقص"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة قائمة الطلبية", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun ProductEditorDialog(
    initialProduct: Product?,
    initialCustomMinStock: Int? = null,
    defaultLowStockThreshold: Int = 5,
    onDismiss: () -> Unit,
    onSave: (Product, Int?) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var purchasePrice by remember { mutableStateOf(initialProduct?.purchasePrice?.toString() ?: "") }
    var price by remember { mutableStateOf(initialProduct?.price?.toString() ?: "") }
    var stock by remember { mutableStateOf(initialProduct?.stock?.toString() ?: "") }
    var unitsPerCarton by remember { mutableStateOf(initialProduct?.unitsPerCarton?.toString() ?: "1") }
    var customMinStock by remember { mutableStateOf(initialCustomMinStock?.toString() ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var cartonBarcode by remember { mutableStateOf(initialProduct?.cartonBarcode ?: "") }
    var imagePath by remember { mutableStateOf(initialProduct?.imagePath ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = { Text(if (initialProduct == null) "إضافة صنف جديد" else "تعديل بيانات الصنف") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ProductImagePickerSection(
                    imagePath = imagePath,
                    productNameForPreview = name,
                    onImagePathChanged = { imagePath = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("سعر التكلفة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("سعر البيع") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("الكمية بالمخزن") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitsPerCarton,
                        onValueChange = { unitsPerCarton = it },
                        label = { Text("وحدات الكرتونة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = customMinStock,
                    onValueChange = { customMinStock = it },
                    label = { Text("الحد الأدنى للطلب / التنبيه (الافتراضي: $defaultLowStockThreshold)") },
                    placeholder = { Text("اتركه فارغاً لاستخدام الحد الافتراضي ($defaultLowStockThreshold)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_min_stock_input")
                )
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("باركود القطعة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cartonBarcode,
                    onValueChange = { cartonBarcode = it },
                    label = { Text("باركود الكرتونة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            Product(
                                id = initialProduct?.id ?: 0,
                                name = name.trim(),
                                purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                                price = price.toDoubleOrNull() ?: 0.0,
                                stock = stock.toIntOrNull() ?: 0,
                                unitsPerCarton = (unitsPerCarton.toIntOrNull() ?: 1).coerceAtLeast(1),
                                barcode = barcode.trim(),
                                cartonBarcode = cartonBarcode.trim(),
                                imagePath = imagePath.trim()
                            ),
                            customMinStock.trim().toIntOrNull()
                        )
                    }
                }
            ) {
                Text("حفظ ومزامنة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun InventoryAuditLogDialog(
    activityLogs: List<ActivityLog>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showPdfReportDialog by remember { mutableStateOf(false) }

    val filteredLogs = remember(activityLogs, searchQuery, selectedFilter) {
        activityLogs.filter { log ->
            val matchesSearch = log.description.contains(searchQuery, ignoreCase = true) ||
                    log.username.contains(searchQuery, ignoreCase = true) ||
                    log.actionType.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "SCAN" -> log.actionType.contains("SCAN", ignoreCase = true) || log.actionType.contains("BARCODE", ignoreCase = true) || log.actionType.contains("VIGNETTE", ignoreCase = true)
                "UPDATE" -> log.actionType.contains("UPDATE", ignoreCase = true) || log.actionType.contains("EDIT", ignoreCase = true) || log.actionType.contains("STOCK", ignoreCase = true)
                "ADD" -> log.actionType.contains("ADD", ignoreCase = true) || log.actionType.contains("CREATE", ignoreCase = true)
                "DELETE" -> log.actionType.contains("DELETE", ignoreCase = true) || log.actionType.contains("REMOVE", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    if (showPdfReportDialog) {
        val payload = remember(filteredLogs, storeName, storePhone, currencySymbol) {
            UniversalPdfReportExporter.buildInventoryAuditLogPayload(
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol,
                logs = filteredLogs
            )
        }
        UniversalSectionPdfReportDialog(
            payload = payload,
            onDismiss = { showPdfReportDialog = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "سجل عمليات المخزون (Audit Log)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                "تاريخ التعديلات، عمليات المسح، والمسؤولون بدقة",
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showPdfReportDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة / PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في السجل بالمنتج أو المسؤول...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf("ALL" to "الكل", "SCAN" to "عمليات المسح 🔍", "UPDATE" to "التعديلات ✏️", "ADD" to "الإضافات ➕", "DELETE" to "الحذف 🗑️")
                    filters.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            onClick = { selectedFilter = key },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, if (isSelected) colorScheme.primary else colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "لا توجد سجلات مطابقة للبحث",
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLogs) { log ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = when {
                                                log.actionType.contains("SCAN", true) -> Color(0xFF0284C7).copy(alpha = 0.15f)
                                                log.actionType.contains("ADD", true) -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                log.actionType.contains("DELETE", true) -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                                else -> colorScheme.primaryContainer
                                            }
                                        ) {
                                            Text(
                                                text = log.actionType,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = when {
                                                    log.actionType.contains("SCAN", true) -> Color(0xFF0284C7)
                                                    log.actionType.contains("ADD", true) -> Color(0xFF10B981)
                                                    log.actionType.contains("DELETE", true) -> Color(0xFFEF4444)
                                                    else -> colorScheme.primary
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AccessTime,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = log.formattedTime,
                                                fontSize = 10.5.sp,
                                                color = colorScheme.onSurfaceVariant,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Text(
                                        text = log.description,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSurface
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = colorScheme.primary
                                            )
                                            Text(
                                                text = "المسؤول: ${log.username} (${log.userRole})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.primary
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
}
