package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.graphics.Bitmap
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.utils.ScannedBarcodeResult
import com.example.utils.ScannerPreferencesManager
import com.example.utils.SmartTorchMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.asImageBitmap
import com.example.utils.CustomerLocationHelper
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.ArchivedInvoice
import com.example.data.models.Customer
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoice
import com.example.data.models.PurchaseInvoiceItem
import com.example.data.models.RecordSyncState
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun buildPdfPayloadForArchivedInvoice(
    inv: ArchivedInvoice,
    products: List<Product>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    receiptFooterText: String,
    developerContact: String,
    cashierName: String
): InvoicePdfPayload {
    val previewLines = inv.items.mapIndexed { idx, sale ->
        val matchedProduct = products.find { it.id == sale.productId }
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
            unitsPerCarton = matchedProduct?.unitsPerCarton ?: 1,
            barcode = matchedProduct?.barcode?.ifBlank { "622100100${idx + 1}" } ?: "622100100${idx + 1}",
            isDebt = inv.isDebt
        )
    }

    val formattedDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(inv.dateMillis))

    return InvoicePdfPayload(
        storeName = storeName,
        storePhone = storePhone,
        customerName = inv.customerName.ifBlank { "عميل نقدي" },
        cashierName = cashierName,
        invoiceNumber = inv.invoiceId,
        formattedDateTime = formattedDateTime,
        paymentMethodAr = if (inv.isDebt) "آجل / دين" else "نقدي",
        lines = previewLines,
        sousTotal = inv.subtotal,
        discountAmount = 0.0,
        discountPercentInt = 0,
        tvaAmount = 0.0,
        netTotal = inv.subtotal,
        hasDebtItems = inv.isDebt,
        creditAmount = if (inv.isDebt) inv.subtotal else 0.0,
        especesAmount = if (inv.isDebt) 0.0 else inv.subtotal,
        currency = currencySymbol,
        footerText = receiptFooterText,
        developerContact = developerContact,
        customerLocationUrl = inv.customerLocationUrl
    )
}

@Composable
fun InvoicesArchiveScreen(viewModel: AntarSalesViewModel) {
    val semantic = LocalAFStoreSemanticColors.current
    val invoices by viewModel.archivedInvoices.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var previewInvoice by remember { mutableStateOf<ArchivedInvoice?>(null) }
    var fulfillingInvoice by remember { mutableStateOf<ArchivedInvoice?>(null) }
    var paymentFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, CASH, DEBT
    var orderTypeFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, PRE_SALE_PENDING, PRE_SALE_COMPLETED, DIRECT
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var dateFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, TODAY, WEEK, MONTH

    val cashInvoicesCount = remember(invoices) { invoices.count { !it.isDebt } }
    val debtInvoicesCount = remember(invoices) { invoices.count { it.isDebt } }
    val preSalePendingCount = remember(invoices) { invoices.count { it.isPendingPreSale } }
    val preSaleCompletedCount = remember(invoices) { invoices.count { it.isCompletedPreSale } }
    val directInvoicesCount = remember(invoices) { invoices.count { !it.isPreSale } }

    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    var showInvoicesPdfReportDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    val products by viewModel.products.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val receiptFooterText by viewModel.receiptFooterText.collectAsState()
    val developerContact by viewModel.developerContact.collectAsState()

    val filteredInvoices = remember(invoices, paymentFilter, orderTypeFilter, searchQuery, dateFilter) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        invoices.filter { inv ->
            val matchesOrderType = when (orderTypeFilter) {
                "PRE_SALE_PENDING" -> inv.isPendingPreSale
                "PRE_SALE_COMPLETED" -> inv.isCompletedPreSale
                "DIRECT" -> !inv.isPreSale
                else -> true
            }
            if (!matchesOrderType) return@filter false

            val matchesPayment = when (paymentFilter) {
                "CASH" -> !inv.isDebt
                "DEBT" -> inv.isDebt
                else -> true
            }
            if (!matchesPayment) return@filter false

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase(Locale.getDefault())
                val matchesId = inv.invoiceId.lowercase(Locale.getDefault()).contains(q)
                val matchesCustomer = inv.customerName.lowercase(Locale.getDefault()).contains(q)
                if (!matchesId && !matchesCustomer) return@filter false
            }

            if (dateFilter != "ALL") {
                cal.timeInMillis = now
                when (dateFilter) {
                    "TODAY" -> {
                        val startOfDay = cal.apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        if (inv.dateMillis < startOfDay) return@filter false
                    }
                    "WEEK" -> {
                        cal.add(Calendar.DAY_OF_YEAR, -7)
                        if (inv.dateMillis < cal.timeInMillis) return@filter false
                    }
                    "MONTH" -> {
                        cal.add(Calendar.MONTH, -1)
                        if (inv.dateMillis < cal.timeInMillis) return@filter false
                    }
                }
            }

            true
        }
    }

    var refundInvoiceTarget by remember { mutableStateOf<ArchivedInvoice?>(null) }
    val totalSalesAmount = remember(filteredInvoices) { filteredInvoices.sumOf { it.subtotal } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- 1. Top Header & Sales Summary Banner ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, semantic.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "أرشيف الفواتير والمبيعات",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "محفوظة محلياً في Room • جاهزة للمعاينة والطباعة والمرتجعات",
                                fontSize = 10.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            UniversalReportHeaderButton(
                                title = "مسح QR",
                                tag = "scan_invoice_qr_btn",
                                onClick = { showQrScannerDialog = true }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            UniversalReportHeaderButton(
                                title = "التقارير PDF",
                                tag = "invoices_pdf_report_btn",
                                onClick = { showInvoicesPdfReportDialog = true }
                            )
                        }
                    }
                }

                // Summary Stats Badges (Invoices Count + Total Amount)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.8.dp, semantic.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الفواتير المعروضة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${filteredInvoices.size} من ${invoices.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي المبيعات:", fontSize = 11.sp, color = Color(0xFF047857))
                            Text(
                                "${String.format(Locale.US, "%.2f", totalSalesAmount)} $currencySymbol",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Search Field ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث برقم الفاتورة أو اسم العميل...", fontSize = 12.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح البحث", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("invoices_search_input")
        )

        // --- 3. Streamlined Horizontal Filter Tabs ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row A: Order / Delivery Status Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = orderTypeFilter == "ALL",
                    onClick = { orderTypeFilter = "ALL" },
                    label = { Text("كل الفواتير (${invoices.size})", fontSize = 11.sp, fontWeight = if (orderTypeFilter == "ALL") FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("invoices_type_all")
                )
                FilterChip(
                    selected = orderTypeFilter == "PRE_SALE_PENDING",
                    onClick = { orderTypeFilter = "PRE_SALE_PENDING" },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("معلقة بالمستودع ($preSalePendingCount)", fontSize = 11.sp, fontWeight = if (orderTypeFilter == "PRE_SALE_PENDING") FontWeight.Bold else FontWeight.Normal)
                            if (preSalePendingCount > 0) {
                                Surface(shape = CircleShape, color = semantic.debtAmber) {
                                    Box(modifier = Modifier.size(7.dp))
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("invoices_type_pending")
                )
                FilterChip(
                    selected = orderTypeFilter == "PRE_SALE_COMPLETED",
                    onClick = { orderTypeFilter = "PRE_SALE_COMPLETED" },
                    label = { Text("بيع مسبق مُسلَّم ($preSaleCompletedCount)", fontSize = 11.sp, fontWeight = if (orderTypeFilter == "PRE_SALE_COMPLETED") FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("invoices_type_completed")
                )
                FilterChip(
                    selected = orderTypeFilter == "DIRECT",
                    onClick = { orderTypeFilter = "DIRECT" },
                    label = { Text("بيع فوري ($directInvoicesCount)", fontSize = 11.sp, fontWeight = if (orderTypeFilter == "DIRECT") FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("invoices_type_direct")
                )
            }

            // Row B: Payment & Date Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment filter pills
                FilterChip(
                    selected = paymentFilter == "ALL",
                    onClick = { paymentFilter = "ALL" },
                    label = { Text("كل طرق الدفع", fontSize = 10.5.sp) },
                    modifier = Modifier.testTag("invoices_filter_all")
                )
                FilterChip(
                    selected = paymentFilter == "CASH",
                    onClick = { paymentFilter = "CASH" },
                    label = { Text("نقدي ($cashInvoicesCount)", fontSize = 10.5.sp) },
                    modifier = Modifier.testTag("invoices_filter_cash")
                )
                FilterChip(
                    selected = paymentFilter == "DEBT",
                    onClick = { paymentFilter = "DEBT" },
                    label = { Text("آجل / دين ($debtInvoicesCount)", fontSize = 10.5.sp) },
                    modifier = Modifier.testTag("invoices_filter_debt")
                )

                // Visual separator
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Date filter pills
                FilterChip(
                    selected = dateFilter == "ALL",
                    onClick = { dateFilter = "ALL" },
                    label = { Text("كل التواريخ", fontSize = 10.5.sp) }
                )
                FilterChip(
                    selected = dateFilter == "TODAY",
                    onClick = { dateFilter = "TODAY" },
                    label = { Text("اليوم", fontSize = 10.5.sp) }
                )
                FilterChip(
                    selected = dateFilter == "WEEK",
                    onClick = { dateFilter = "WEEK" },
                    label = { Text("آخر 7 أيام", fontSize = 10.5.sp) }
                )
                FilterChip(
                    selected = dateFilter == "MONTH",
                    onClick = { dateFilter = "MONTH" },
                    label = { Text("آخر شهر", fontSize = 10.5.sp) }
                )
            }
        }

        // --- 4. Invoices List or Empty Placeholder ---
        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp).size(36.dp)
                        )
                    }
                    Text(
                        text = "لا توجد فواتير مطابقة للبحث أو الفلاتر المحددة",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(
                    items = filteredInvoices,
                    key = { index, inv -> "inv_${inv.invoiceId}_${inv.dateMillis}_$index" },
                    contentType = { _, _ -> "archived_invoice" }
                ) { _, inv ->
                    val dateStr = remember(inv.dateMillis) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(inv.dateMillis))
                    }
                    val isPending = inv.isPendingPreSale
                    val isCompleted = inv.isCompletedPreSale
                    val isCancelled = inv.isCancelledPreSale

                    val cashierName = remember(currentUser) {
                        currentUser?.fullName?.takeIf { it.isNotBlank() } ?: currentUser?.username?.takeIf { !it.contains("@") } ?: "Antar"
                    }

                    val paymentBadgeBg = if (inv.isDebt) {
                        Color(0xFFF59E0B).copy(alpha = 0.16f)
                    } else {
                        Color(0xFF10B981).copy(alpha = 0.15f)
                    }
                    val paymentBadgeColor = if (inv.isDebt) {
                        Color(0xFFD97706)
                    } else {
                        Color(0xFF059669)
                    }
                    val paymentBadgeLabel = if (inv.isDebt) {
                        "فاتورة آجلة • دين"
                    } else {
                        "مدفوعة نقداً ✓"
                    }

                    val cardBorder = when {
                        isPending -> BorderStroke(1.5.dp, semantic.debtAmber.copy(alpha = 0.85f))
                        isCompleted -> BorderStroke(1.2.dp, Color(0xFF047857).copy(alpha = 0.55f))
                        inv.isDebt -> BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f))
                        else -> BorderStroke(1.dp, semantic.cardBorder)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invoice_card_${inv.invoiceId}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPending) semantic.cardElevated.copy(alpha = 0.98f) else semantic.cardElevated
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (semantic.isDark) 0.dp else if (isPending) 3.dp else 1.5.dp
                        ),
                        border = cardBorder,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Customer Avatar, Customer Name, Maps Pin & Clean LTR Invoice ID Badge
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
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = inv.customerName.take(1).ifBlank { "ع" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = inv.customerName.ifBlank { "عميل نقدي" },
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (inv.customerLocationUrl.isNotBlank() || (inv.customerLatitude != null && inv.customerLongitude != null)) {
                                                IconButton(
                                                    onClick = {
                                                        CustomerLocationHelper.openInGoogleMaps(
                                                            context = context,
                                                            latitude = inv.customerLatitude,
                                                            longitude = inv.customerLongitude,
                                                            locationUrl = inv.customerLocationUrl
                                                        )
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.LocationOn,
                                                        contentDescription = "موقع العميل",
                                                        tint = Color(0xFF16A34A),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "📅 $dateStr",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Invoice Code Pill (Strict LTR display)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "\u200E#${inv.invoiceId}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Row 2: Prominent Total & Status Badges
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "المبلغ الإجمالي",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", inv.subtotal)} $currencySymbol",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.5.sp,
                                        color = if (inv.isDebt) Color(0xFFD97706) else Color(0xFF047857)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Order Status Badge
                                    if (isPending) {
                                        Surface(
                                            color = semantic.debtAmber.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.75f))
                                        ) {
                                            Text(
                                                text = "معلق بالمستودع ⏳",
                                                color = semantic.debtAmber,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else if (isCompleted) {
                                        Surface(
                                            color = Color(0xFF047857).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.dp, Color(0xFF047857).copy(alpha = 0.55f))
                                        ) {
                                            Text(
                                                text = "مُسلَّم ✓",
                                                color = Color(0xFF047857),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // Payment Method Badge
                                    Surface(
                                        color = paymentBadgeBg,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(0.8.dp, paymentBadgeColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = paymentBadgeLabel,
                                            color = paymentBadgeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Warehouse Pending Dispatch Alert & Action (if pending)
                            if (isPending) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = semantic.debtAmber.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.WarningAmber,
                                                    contentDescription = null,
                                                    tint = semantic.debtAmber,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "بضاعة محجوزة بالمستودع • لم تُسلّم بعد",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = semantic.debtAmber
                                                )
                                            }
                                            if (inv.expectedDeliveryDate > 0) {
                                                val expDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(inv.expectedDeliveryDate))
                                                Text(
                                                    text = "موعد الاستلام المتوقع: $expDateStr",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { fulfillingInvoice = inv },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("fulfill_invoice_btn_${inv.invoiceId}")
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("تسليم الآن", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Items count info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الأصناف: ${inv.totalQuantity} قطعة • (${inv.items.size} منتج)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Direct Action Buttons Toolbar (Preview, Quick Print, Share PDF, Safe Refund)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Preview 5 templates
                                OutlinedButton(
                                    onClick = { previewInvoice = inv },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "معاينة",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 2. Quick Print
                                OutlinedButton(
                                    onClick = {
                                        val payload = buildPdfPayloadForArchivedInvoice(
                                            inv = inv,
                                            products = products,
                                            storeName = storeName,
                                            storePhone = storePhone,
                                            currencySymbol = currencySymbol,
                                            receiptFooterText = receiptFooterText,
                                            developerContact = developerContact,
                                            cashierName = cashierName
                                        )
                                        InvoicePdfExporter.printInvoicePdf(context, payload)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "طباعة",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 3. PDF Share
                                OutlinedButton(
                                    onClick = {
                                        val payload = buildPdfPayloadForArchivedInvoice(
                                            inv = inv,
                                            products = products,
                                            storeName = storeName,
                                            storePhone = storePhone,
                                            currencySymbol = currencySymbol,
                                            receiptFooterText = receiptFooterText,
                                            developerContact = developerContact,
                                            cashierName = cashierName
                                        )
                                        InvoicePdfExporter.shareInvoicePdf(context, payload)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFF0284C7)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "مشاركة",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // 4. Fast Refund / Return
                                if (!isPending) {
                                    OutlinedButton(
                                        onClick = { refundInvoiceTarget = inv },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(0.9f)
                                    ) {
                                        Icon(
                                            Icons.Default.Replay,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            "مرتجع",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
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

    // Safe Refund Confirmation Dialog
    refundInvoiceTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { refundInvoiceTarget = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Icon(
                        Icons.Default.Replay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(10.dp).size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "تأكيد إرجاع الفاتورة واستعادة المخزون",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "هل أنت متأكد من رغبتك في إرجاع الفاتورة رقم \u200E#${target.invoiceId} الخاصة بالعميل ${target.customerName}؟",
                        fontSize = 13.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• إجمالي قيمة الفاتورة: ${String.format(Locale.US, "%.2f", target.subtotal)} $currencySymbol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• طريقة الدفع: ${if (target.isDebt) "آجل / دين" else "نقدي"}",
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "• سيتم إعادة جميع كميات المنتجات المباعة (${target.totalQuantity} قطعة) إلى رصيد المخزون فوراً.",
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (target.isDebt) {
                                Text(
                                    text = "• سيتم خصم هذا المبلغ من مديونية حساب العميل.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.refundInvoice(target)
                        Toast.makeText(context, "تم إرجاع الفاتورة واستعادة كميات المخزون بنجاح ✓", Toast.LENGTH_SHORT).show()
                        refundInvoiceTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الإرجاع واستعادة المخزون", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { refundInvoiceTarget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    fulfillingInvoice?.let { orderToFulfill ->
        var isDebtChoice by remember(orderToFulfill) { mutableStateOf(orderToFulfill.isDebt) }
        AlertDialog(
            onDismissRequest = { fulfillingInvoice = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF047857)
                    ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(6.dp).size(20.dp)
                        )
                    }
                    Text(
                        text = "تأكيد تسليم وخروج الطلبية",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "هل ترغب في تأكيد تسليم الفاتورة رقم #${orderToFulfill.invoiceId} للعميل ${orderToFulfill.customerName}؟",
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "• إجمالي القطع: ${orderToFulfill.totalQuantity}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• القيمة الإجمالية: ${String.format(Locale.US, "%.2f", orderToFulfill.subtotal)} $currencySymbol",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• سيتم خصم الكميات فعلياً من رصيد المستودع الحالي.",
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text("طريقة التحصيل / الدفع:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isDebtChoice = false }
                        ) {
                            RadioButton(
                                selected = !isDebtChoice,
                                onClick = { isDebtChoice = false }
                            )
                            Text("دفع نقدي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isDebtChoice = true }
                        ) {
                            RadioButton(
                                selected = isDebtChoice,
                                onClick = { isDebtChoice = true }
                            )
                            Text("تسجيل كدين / آجل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val payment = if (isDebtChoice) "DEBT" else "CASH"
                        viewModel.fulfillPreSaleOrder(
                            invoice = orderToFulfill,
                            finalPaymentMethod = payment,
                            isDebt = isDebtChoice,
                            onSuccess = {
                                Toast.makeText(context, "تم تسليم الطلبية وخصم المخزون بنجاح ✓", Toast.LENGTH_SHORT).show()
                                fulfillingInvoice = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857))
                ) {
                    Text("تأكيد التسليم وخصم المخزون", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fulfillingInvoice = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    previewInvoice?.let { inv ->
        InvoiceTemplatePreviewDialog(
            viewModel = viewModel,
            archivedInvoice = inv,
            isPreSavePreview = false,
            onDismiss = { previewInvoice = null }
        )
    }

    if (showInvoicesPdfReportDialog) {
        val payload = remember(storeName, storePhone, currencySymbol, filteredInvoices) {
            UniversalPdfReportExporter.buildInvoicesPayload(
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol,
                invoices = filteredInvoices
            )
        }
        UniversalSectionPdfReportDialog(
            payload = payload,
            onDismiss = { showInvoicesPdfReportDialog = false }
        )
    }

    if (showQrScannerDialog) {
        InvoiceQrScannerDialog(
            invoices = invoices,
            onDismiss = { showQrScannerDialog = false },
            onInvoiceSelected = { inv ->
                showQrScannerDialog = false
                previewInvoice = inv
                Toast.makeText(context, "تم العثور على الفاتورة #${inv.invoiceId}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun InvoiceQrScannerDialog(
    invoices: List<ArchivedInvoice>,
    onDismiss: () -> Unit,
    onInvoiceSelected: (ArchivedInvoice) -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalAFStoreSemanticColors.current
    var scannedCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var smartTorchMode by remember {
        mutableStateOf(ScannerPreferencesManager.getSmartTorchMode(context))
    }
    var isSoundEnabled by remember {
        mutableStateOf(ScannerPreferencesManager.isSoundEnabled(context))
    }
    var isVibrationEnabled by remember {
        mutableStateOf(ScannerPreferencesManager.isVibrationEnabled(context))
    }
    var isAutoZoomEnabled by remember {
        mutableStateOf(ScannerPreferencesManager.isAutoZoomEnabled(context))
    }

    fun searchAndSelectInvoice(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) {
            errorMessage = "الرجاء مسح أو إدخال رقم الفاتورة"
            return
        }

        val cleanCode = trimmed.substringAfterLast("#").substringAfterLast("/").trim()

        val found = invoices.find { inv ->
            inv.invoiceId.equals(cleanCode, ignoreCase = true) ||
            inv.invoiceId.equals(trimmed, ignoreCase = true) ||
            inv.invoiceId.contains(cleanCode, ignoreCase = true) ||
            cleanCode.contains(inv.invoiceId, ignoreCase = true)
        }

        if (found != null) {
            onInvoiceSelected(found)
        } else {
            errorMessage = "لم يتم العثور على فاتورة مطابقة للرمز: '$trimmed'"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 650.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, semantic.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp).size(22.dp)
                            )
                        }
                        Text(
                            text = "مسح QR كود الفاتورة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider()

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        TurboCameraBarcodeHud(
                            isScanningActive = true,
                            smartTorchMode = smartTorchMode,
                            onToggleTorchMode = {
                                val next = when (smartTorchMode) {
                                    SmartTorchMode.OFF -> SmartTorchMode.AUTO_SMART
                                    SmartTorchMode.AUTO_SMART -> SmartTorchMode.ALWAYS_ON
                                    SmartTorchMode.ALWAYS_ON -> SmartTorchMode.OFF
                                }
                                smartTorchMode = next
                                ScannerPreferencesManager.setSmartTorchMode(context, next)
                            },
                            isSoundEnabled = isSoundEnabled,
                            onToggleSound = {
                                isSoundEnabled = !isSoundEnabled
                                ScannerPreferencesManager.setSoundEnabled(context, isSoundEnabled)
                            },
                            isVibrationEnabled = isVibrationEnabled,
                            onToggleVibration = {
                                isVibrationEnabled = !isVibrationEnabled
                                ScannerPreferencesManager.setVibrationEnabled(context, isVibrationEnabled)
                            },
                            isAutoZoomEnabled = isAutoZoomEnabled,
                            onToggleAutoZoom = {
                                isAutoZoomEnabled = !isAutoZoomEnabled
                                ScannerPreferencesManager.setAutoZoomEnabled(context, isAutoZoomEnabled)
                            },
                            hasActiveSelection = false,
                            lastDecodeBadge = null,
                            onResetScanClick = { errorMessage = null },
                            onBarcodeScanned = { result: ScannedBarcodeResult ->
                                scannedCode = result.code
                                searchAndSelectInvoice(result.code)
                            },
                            compactMode = true
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Text("إذن الكاميرا مطلوب لمسح رموز QR للفواتير", fontSize = 12.sp, textAlign = TextAlign.Center)
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text("منح إذن الكاميرا")
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = scannedCode,
                        onValueChange = {
                            scannedCode = it
                            if (it.isBlank()) errorMessage = null
                        },
                        label = { Text("أو أدخل رقم الفاتورة / الرمز يدويًا") },
                        placeholder = { Text("مثال: INV-123456") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invoice_qr_input_field"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (scannedCode.isNotBlank()) {
                                IconButton(onClick = { scannedCode = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = { searchAndSelectInvoice(scannedCode) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invoice_qr_search_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("عرض الفاتورة")
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseInvoicesScreen(viewModel: AntarSalesViewModel) {
    val canManagePurchaseInvoices by viewModel.canManagePurchaseInvoices.collectAsState()
    if (!canManagePurchaseInvoices) {
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
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "فواتير المشتريات والموردين مقتصرة على الإدارة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ليس لديك صلاحية لعرض أو إدارة فواتير المشتريات والموردين.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val semantic = LocalAFStoreSemanticColors.current
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsState()
    val products by viewModel.products.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val receiptFooterText by viewModel.receiptFooterText.collectAsState()
    val developerContact by viewModel.developerContact.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val isDark = semantic.isDark

    var searchQuery by rememberSaveable { mutableStateOf("") }
    // Date filter options: ALL, TODAY, YESTERDAY, LAST_7_DAYS, THIS_MONTH
    var selectedDateFilter by rememberSaveable { mutableStateOf("ALL") }
    var showCreateModal by remember { mutableStateOf(false) }
    var showPurchasesPdfReportDialog by remember { mutableStateOf(false) }
    var selectedInvoiceForDetails by remember { mutableStateOf<PurchaseInvoice?>(null) }
    var activeSupplierReceipt by remember { mutableStateOf<DebtPaymentReceiptData?>(null) }

    val primaryBlue = Color(0xFF0077C2)
    val softBlueBg = if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.45f) else Color(0xFFE0F2FE)
    val activeChipBg = if (isDark) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFFBAE6FD)

    val filteredInvoices = remember(purchaseInvoices, searchQuery, selectedDateFilter) {
        val nowCal = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfYesterday = startOfToday - 24L * 60L * 60L * 1000L
        val startOfLast7Days = startOfToday - 6L * 24L * 60L * 60L * 1000L
        val startOfThisMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        purchaseInvoices.filter { inv ->
            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> inv.dateMillis >= startOfToday
                "YESTERDAY" -> inv.dateMillis in startOfYesterday until startOfToday
                "LAST_7_DAYS" -> inv.dateMillis >= startOfLast7Days
                "THIS_MONTH" -> inv.dateMillis >= startOfThisMonth && nowCal.timeInMillis >= startOfThisMonth
                else -> true
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                inv.invoiceNumber.contains(q, ignoreCase = true) ||
                inv.supplierName.contains(q, ignoreCase = true) ||
                inv.getItems().any { it.productName.contains(q, ignoreCase = true) }

            matchesDate && matchesQuery
        }
    }

    val totalPurchasesSum = remember(filteredInvoices) {
        filteredInvoices.sumOf { it.totalCost }
    }
    val totalPaidSum = remember(filteredInvoices) {
        filteredInvoices.sumOf { it.paidAmount }
    }
    val totalDebtSum = remember(filteredInvoices) {
        filteredInvoices.sumOf { it.remainingDebt }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Container Card (Header + 4 Summary KPI Cards matching screenshot)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = semantic.cardElevated,
            shadowElevation = if (isDark) 0.dp else 2.dp,
            border = BorderStroke(1.dp, semantic.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Row: Left Action Buttons + Center/Right Title & Subtitle + Right Icon Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left action buttons: Add + PDF Report
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showCreateModal = true },
                            shape = RoundedCornerShape(14.dp),
                            color = primaryBlue,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("add_purchase_invoice_fab")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = "فاتورة شراء جديدة",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        UniversalReportHeaderButton(
                            title = "التقارير PDF",
                            tag = "purchases_pdf_report_btn",
                            onClick = { showPurchasesPdfReportDialog = true }
                        )
                    }

                    // Title & Subtitle + Right Icon Box
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f).padding(start = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "فواتير الشراء والتوريد",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "تسجيل مشتريات وسلع جديدة ومتابعة ديون الموردين",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = softBlueBg,
                            border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.55f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    tint = primaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 4 KPI Summary Cards Row (Left to Right: عدد الفواتير | آجل | المدفوع | إجمالي)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. عدد الفواتير (Leftmost)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "عدد الفواتير",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "${filteredInvoices.size}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    // 2. آجل (Second from left)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF451A1A) else Color(0xFFF5DEDE),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "آجل",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalDebtSum)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 3. المدفوع (Third from left)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "المدفوع",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669),
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalPaidSum)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF10B981),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 4. إجمالي (Rightmost)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = softBlueBg,
                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "إجمالي",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1),
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalPurchasesSum)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 2. Search Bar (matching screenshot with search icon on the right)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "ابحث برقم الفاتورة، اسم المورد، أو اسم السلعة...",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            leadingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                    }
                }
            } else null,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            },
            textStyle = TextStyle(
                textAlign = TextAlign.End,
                fontSize = 14.sp
            ),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("purchase_invoices_search_input")
        )

        // 3. Date Filter Chips Row (Left to Right: هذا الشهر | آخر 7 أيام | أمس | اليوم | الكل)
        val filterOptions = listOf(
            "THIS_MONTH" to "هذا الشهر",
            "LAST_7_DAYS" to "آخر 7 أيام",
            "YESTERDAY" to "أمس",
            "TODAY" to "اليوم",
            "ALL" to "الكل"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            filterOptions.forEach { (key, label) ->
                val isSelected = selectedDateFilter == key
                Surface(
                    onClick = { selectedDateFilter = key },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) activeChipBg else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("purchase_filter_$key")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (isSelected) {
                                if (isDark) Color.White else Color(0xFF0C4A6E)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        // 4. Empty State OR Purchase Invoices List
        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = softBlueBg,
                        modifier = Modifier.size(92.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                tint = primaryBlue,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Text(
                        text = "لا توجد أي فواتير شراء مسجلة حتى الآن",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "اضغط على زر (فاتورة شراء جديدة) لإضافة شحنة أو تسجيل سلع جديدة للمخزن",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { showCreateModal = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("empty_state_add_purchase_invoice_btn")
                    ) {
                        Text(
                            text = "تسجيل أول فاتورة شراء",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(
                    items = filteredInvoices,
                    key = { index, inv -> "pur_${inv.id}_${inv.dateMillis}_$index" },
                    contentType = { _, _ -> "purchase_invoice" }
                ) { _, inv ->
                    val dateStr = remember(inv.dateMillis) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(inv.dateMillis))
                    }
                    val hasDebt = inv.remainingDebt > 0.009
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedInvoiceForDetails = inv }
                            .testTag("purchase_invoice_card_${inv.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = semantic.cardElevated
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDark) 0.dp else 2.dp
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (hasDebt) Color(0xFFEF4444).copy(alpha = 0.35f)
                            else semantic.cardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (hasDebt) Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (hasDebt) {
                                            "آجل: ${String.format(Locale.US, "%.1f", inv.remainingDebt)} $currencySymbol"
                                        } else {
                                            "خالص بالكامل"
                                        },
                                        color = if (hasDebt) Color(0xFFEF4444) else Color(0xFF10B981),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${inv.invoiceNumber} • المورد: ${inv.supplierName}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$dateStr • عدد السلع: ${inv.itemsCount}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasDebt) {
                                    OutlinedButton(
                                        onClick = { selectedInvoiceForDetails = inv },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("سداد / تفاصيل", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    TextButton(
                                        onClick = { selectedInvoiceForDetails = inv },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("عرض التفاصيل", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "المدفوع: ${String.format(Locale.US, "%.1f", inv.paidAmount)} $currencySymbol",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                    Text(
                                        text = "الإجمالي: ${String.format(Locale.US, "%.1f", inv.totalCost)} $currencySymbol",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = primaryBlue
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

    // Create Purchase Invoice Modal (pick from inventory OR add new product + Paid/Credit/Partial)
    if (showCreateModal) {
        CreatePurchaseInvoiceDialog(
            products = products,
            currencySymbol = currencySymbol,
            onDismiss = { showCreateModal = false },
            onSaveInvoice = { invoiceNumber, supplierName, supplierPhone, paidAmount, notes, items ->
                viewModel.createPurchaseInvoice(
                    supplierName = supplierName,
                    supplierPhone = supplierPhone,
                    paidAmount = paidAmount,
                    notes = notes,
                    items = items,
                    customInvoiceNumber = invoiceNumber
                )
                showCreateModal = false
            }
        )
    }

    // Purchase Invoice Details & Supplier Debt Payment Dialog
    selectedInvoiceForDetails?.let { currentInv ->
        val liveInvoice = purchaseInvoices.find { it.id == currentInv.id } ?: currentInv
        PurchaseInvoiceDetailsDialog(
            invoice = liveInvoice,
            currencySymbol = currencySymbol,
            onDismiss = { selectedInvoiceForDetails = null },
            onPayDebt = { paymentAmount ->
                viewModel.paySupplierInvoiceDebt(liveInvoice, paymentAmount)
            },
            onPayAndPrintReceipt = { paymentAmount ->
                val prevDebt = liveInvoice.remainingDebt
                val remDebt = (prevDebt - paymentAmount).coerceAtLeast(0.0)
                val now = System.currentTimeMillis()
                viewModel.paySupplierInvoiceDebt(liveInvoice, paymentAmount)
                selectedInvoiceForDetails = null
                activeSupplierReceipt = DebtPaymentReceiptData(
                    receiptNumber = "SPAY-${(now / 1000).toString().takeLast(6)}",
                    partyType = "SUPPLIER",
                    partyName = liveInvoice.supplierName,
                    partyPhone = liveInvoice.supplierPhone,
                    previousBalance = prevDebt,
                    paidAmount = paymentAmount,
                    remainingBalance = remDebt,
                    notes = "سداد دفعة من فاتورة توريد رقم ${liveInvoice.invoiceNumber}",
                    timestampMillis = now
                )
            }
        )
    }

    activeSupplierReceipt?.let { receiptData ->
        DebtPaymentReceiptDialog(
            receipt = receiptData,
            viewModel = viewModel,
            storeName = storeName,
            storePhone = storePhone,
            cashierName = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: currentUser?.username?.takeIf { !it.contains("@") } ?: "Antar",
            currencySymbol = currencySymbol,
            footerText = receiptFooterText,
            developerContact = developerContact,
            context = context,
            onDismiss = { activeSupplierReceipt = null }
        )
    }

    if (showPurchasesPdfReportDialog) {
        val payload = remember(storeName, storePhone, currencySymbol, filteredInvoices) {
            UniversalPdfReportExporter.buildPurchasesPayload(
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol,
                purchases = filteredInvoices
            )
        }
        UniversalSectionPdfReportDialog(
            payload = payload,
            onDismiss = { showPurchasesPdfReportDialog = false }
        )
    }
}

@Composable
private fun CreatePurchaseInvoiceDialog(
    products: List<Product>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSaveInvoice: (
        invoiceNumber: String,
        supplierName: String,
        supplierPhone: String,
        paidAmount: Double,
        notes: String,
        items: List<PurchaseInvoiceItem>
    ) -> Unit
) {
    val isDark = LocalAFStoreSemanticColors.current.isDark
    val primaryBlue = Color(0xFF0077C2)
    val dialogBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF4F7FB)
    val cardSurfaceColor = if (isDark) Color(0xFF1E293B) else Color.White
    val softBlueBg = if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.45f) else Color(0xFFE0F2FE)
    val stockButtonBg = if (isDark) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFFBAE6FD)

    var invoiceNumber by remember {
        val rand5 = ((System.currentTimeMillis() / 100) % 90000 + 10000).toInt()
        mutableStateOf("PUR-$rand5")
    }
    var supplierName by remember { mutableStateOf("") }
    var supplierPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // null = collapsed (shows empty state if draftItems is empty), "EXISTING" = pick from inventory
    var activeEntryMode by remember { mutableStateOf<String?>(null) }
    var showNewProductModal by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }
    var selectedExistingProduct by remember { mutableStateOf<Product?>(products.firstOrNull()) }

    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var itemCost by remember { mutableStateOf("") }
    var itemSellPrice by remember { mutableStateOf("") }
    var unitsPerCarton by remember { mutableStateOf("1") }
    var isCartonQty by remember { mutableStateOf(false) }

    val draftItems = remember { mutableStateListOf<PurchaseInvoiceItem>() }
    val totalInvoiceCost = draftItems.sumOf { it.subtotal }

    // Payment Mode: PAID, CREDIT, PARTIAL
    var paymentMode by remember { mutableStateOf("PAID") }
    var customPaidAmountInput by remember { mutableStateOf("") }

    val resolvedPaidAmount = when (paymentMode) {
        "PAID" -> totalInvoiceCost
        "CREDIT" -> 0.0
        else -> (customPaidAmountInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, totalInvoiceCost)
    }
    val remainingDebtAmount = (totalInvoiceCost - resolvedPaidAmount).coerceAtLeast(0.0)

    val matchingProducts = remember(products, productSearchQuery) {
        if (productSearchQuery.isBlank()) products.take(10)
        else products.filter {
            it.name.contains(productSearchQuery, ignoreCase = true) ||
                it.barcode.contains(productSearchQuery, ignoreCase = true)
        }.take(10)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = dialogBgColor,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // ── Top Dialog Header (matches screenshot) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("close_purchase_invoice_dialog_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "تسجيل فاتورة شراء جديدة",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "إضافة سلع ومشتريات وتحديث مخزون المتجر",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = softBlueBg,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddShoppingCart,
                                        contentDescription = null,
                                        tint = primaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // ── Scrollable Body ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ── 1. بيانات المورد والفاتورة ──
                        Text(
                            text = "1. بيانات المورد والفاتورة",
                            color = primaryBlue,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Row 1: Supplier Name (Left) + Invoice Number (Right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: اسم المورد / الشركة *
                            OutlinedTextField(
                                value = supplierName,
                                onValueChange = { supplierName = it },
                                placeholder = {
                                    Text(
                                        text = "اسم المورد / الشركة *",
                                        fontSize = 13.5.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                textStyle = TextStyle(
                                    textAlign = TextAlign.End,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = dialogBgColor,
                                    unfocusedContainerColor = dialogBgColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .testTag("purchase_supplier_name_input")
                            )

                            // Right: رقم الفاتورة (PUR-XXXXX)
                            OutlinedTextField(
                                value = invoiceNumber,
                                onValueChange = { invoiceNumber = it },
                                label = {
                                    Text(
                                        text = "رقم الفاتورة",
                                        fontSize = 11.5.sp
                                    )
                                },
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = dialogBgColor,
                                    unfocusedContainerColor = dialogBgColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("purchase_invoice_number_input")
                            )
                        }

                        // Row 2: رقم هاتف المورد (اختياري)
                        OutlinedTextField(
                            value = supplierPhone,
                            onValueChange = { supplierPhone = it },
                            placeholder = {
                                Text(
                                    text = "رقم هاتف المورد (اختياري)",
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            textStyle = TextStyle(
                                textAlign = TextAlign.End,
                                fontSize = 14.sp
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = dialogBgColor,
                                unfocusedContainerColor = dialogBgColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("purchase_supplier_phone_input")
                        )

                        // ── 2. الأصناف المشتراة (0) + Action Buttons Row ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: + سلعة جديدة ✨ (Solid Blue Button -> opens NewPurchaseProductModal)
                            Surface(
                                onClick = {
                                    activeEntryMode = null
                                    showNewProductModal = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = primaryBlue,
                                modifier = Modifier
                                    .width(86.dp)
                                    .testTag("purchase_add_new_product_btn")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "سلعة",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        text = "+ جديدة",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp
                                    )
                                    Text(
                                        text = "✨",
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Middle: صنف من المخزون (Light Blue Button with Box Icon)
                            Surface(
                                onClick = {
                                    activeEntryMode = "EXISTING"
                                    val firstProd = selectedExistingProduct ?: products.firstOrNull()
                                    selectedExistingProduct = firstProd
                                    if (firstProd != null) {
                                        itemName = firstProd.name
                                        itemCost = firstProd.purchasePrice.toString()
                                        itemSellPrice = firstProd.price.toString()
                                        unitsPerCarton = firstProd.unitsPerCarton.toString()
                                    }
                                    itemQty = "1"
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = stockButtonBg,
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("purchase_pick_inventory_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = "صنف من المخزون",
                                        color = if (isDark) Color.White else Color(0xFF0C4A6E),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White else Color(0xFF0C4A6E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Right: 2. الأصناف المشتراة (0)
                            Text(
                                text = "2. الأصناف المشتراة (${draftItems.size})",
                                color = primaryBlue,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // ── Purchased Items Card (Empty State OR Entry Form + Added Items) ──
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardSurfaceColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Inline Entry Form when user taps "صنف من المخزون" or "+ سلعة جديدة ✨"
                                if (activeEntryMode != null) {
                                    val isExistingMode = activeEntryMode == "EXISTING"
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = dialogBgColor,
                                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.35f)),
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
                                                TextButton(
                                                    onClick = { activeEntryMode = null },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("إخفاء النموذج ✕", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text(
                                                    text = if (isExistingMode) "اختيار صنف موجود من المخزون" else "تسجيل سلعة جديدة للمخزون ✨",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = primaryBlue
                                                )
                                            }

                                            if (isExistingMode) {
                                                if (products.isEmpty()) {
                                                    Text(
                                                        text = "لا توجد أصناف مسجلة في المخزن حالياً. اضغط على (+ سلعة جديدة) لإضافة أول صنف.",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = TextAlign.End,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                } else {
                                                    OutlinedTextField(
                                                        value = productSearchQuery,
                                                        onValueChange = { productSearchQuery = it },
                                                        placeholder = {
                                                            Text(
                                                                text = "ابحث عن الصنف في المخزون...",
                                                                fontSize = 12.sp,
                                                                textAlign = TextAlign.End,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        },
                                                        trailingIcon = {
                                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        },
                                                        textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 13.sp),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                                                    ) {
                                                        matchingProducts.forEach { prod ->
                                                            val isSelected = selectedExistingProduct?.id == prod.id
                                                            Surface(
                                                                onClick = {
                                                                    selectedExistingProduct = prod
                                                                    itemName = prod.name
                                                                    itemCost = prod.purchasePrice.toString()
                                                                    itemSellPrice = prod.price.toString()
                                                                    unitsPerCarton = prod.unitsPerCarton.toString()
                                                                },
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = if (isSelected) primaryBlue else cardSurfaceColor,
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (isSelected) primaryBlue else MaterialTheme.colorScheme.outlineVariant
                                                                ),
                                                                modifier = Modifier.height(34.dp)
                                                            ) {
                                                                Box(
                                                                    contentAlignment = Alignment.Center,
                                                                    modifier = Modifier.padding(horizontal = 10.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "${prod.name} (${prod.stock})",
                                                                        fontSize = 11.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Product Name + Quantity Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = itemQty,
                                                    onValueChange = { itemQty = it },
                                                    label = { Text(if (isCartonQty) "عدد الكراتين" else "الكمية", fontSize = 11.sp) },
                                                    textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(0.75f)
                                                )
                                                OutlinedTextField(
                                                    value = itemName,
                                                    onValueChange = { itemName = it },
                                                    label = { Text("اسم السلعة *", fontSize = 11.sp) },
                                                    textStyle = TextStyle(textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1.45f)
                                                )
                                            }

                                            // Purchase Cost + Selling Price Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = itemSellPrice,
                                                    onValueChange = { itemSellPrice = it },
                                                    label = { Text("سعر البيع ($currencySymbol)", fontSize = 11.sp) },
                                                    textStyle = TextStyle(textAlign = TextAlign.Center),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = itemCost,
                                                    onValueChange = { itemCost = it },
                                                    label = { Text("تكلفة الشراء ($currencySymbol)", fontSize = 11.sp) },
                                                    textStyle = TextStyle(textAlign = TextAlign.Center),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            // Unit vs Carton Row + Add Item Button
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val q = (itemQty.toIntOrNull() ?: 1).coerceAtLeast(1)
                                                        val packUnits = (unitsPerCarton.toIntOrNull() ?: 1).coerceAtLeast(1)
                                                        val c = (itemCost.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                                                        val s = (itemSellPrice.toDoubleOrNull() ?: c).coerceAtLeast(0.0)
                                                        val effectiveUnits = if (isCartonQty) q * packUnits else q
                                                        if (itemName.isNotBlank()) {
                                                            draftItems.add(
                                                                PurchaseInvoiceItem(
                                                                    productId = if (isExistingMode) selectedExistingProduct?.id else null,
                                                                    productName = itemName.trim(),
                                                                    quantity = q,
                                                                    unitCost = if (isCartonQty) c * packUnits else c,
                                                                    sellingPrice = s,
                                                                    subtotal = effectiveUnits * c,
                                                                    isCarton = isCartonQty,
                                                                    unitsPerCarton = packUnits,
                                                                    isNewProduct = !isExistingMode
                                                                )
                                                            )
                                                            if (!isExistingMode) {
                                                                itemName = ""
                                                                itemCost = ""
                                                                itemSellPrice = ""
                                                            }
                                                            itemQty = "1"
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1.2f)
                                                        .height(44.dp)
                                                        .testTag("confirm_add_purchase_item_btn")
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("إضافة للفاتورة", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                                                }

                                                Surface(
                                                    onClick = { isCartonQty = !isCartonQty },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isCartonQty) softBlueBg else cardSurfaceColor,
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = if (isCartonQty) "بالكرتونة 📦" else "بالقطعة الفردية",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isCartonQty) primaryBlue else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Empty State inside Section 2 Card (exact match to screenshot)
                                if (draftItems.isEmpty() && activeEntryMode == null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 18.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddShoppingCart,
                                            contentDescription = null,
                                            tint = primaryBlue,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                            text = "لم تتم إضافة أي أصناف بعد",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "اضغط على (صنف من المخزون) لإعادة تعبئة كميات حالية، أو (سلعة جديدة) لتسجيل منتج لم يكن موجوداً من قبل",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                // Added Items List inside Section 2 Card
                                if (draftItems.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        draftItems.forEachIndexed { idx, draft ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = dialogBgColor,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = { draftItems.removeAt(idx) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "حذف",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = "${String.format(Locale.US, "%.2f", draft.subtotal)} $currencySymbol",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.sp,
                                                        color = primaryBlue
                                                    )

                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(start = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = draft.productName,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            textAlign = TextAlign.End
                                                        )
                                                        Text(
                                                            text = "الكمية: ${draft.quantity} ${if (draft.isCarton) "كرتونة" else "قطعة"} × ${String.format(Locale.US, "%.2f", draft.unitCost)} $currencySymbol",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            textAlign = TextAlign.End
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── 3. الدفع والحسابات ──
                        Text(
                            text = "3. الدفع والحسابات",
                            color = primaryBlue,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardSurfaceColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Top Row: Total Cost (0.00 د.ج on Left | إجمالي تكلفة المشتريات: on Right)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", totalInvoiceCost)} $currencySymbol",
                                        color = primaryBlue,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp
                                    )
                                    Text(
                                        text = "إجمالي تكلفة المشتريات:",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Middle Row: 3 Payment Status Selector Cards
                                // Left: مدفوع جزئياً ⚖️ | Middle: آجل (دين للمورد) ⏳ | Right: نقدي (مدفوع) 💵
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Left: مدفوع جزئياً ⚖️
                                    val isPartial = paymentMode == "PARTIAL"
                                    Surface(
                                        onClick = { paymentMode = "PARTIAL" },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isPartial) softBlueBg else dialogBgColor,
                                        border = BorderStroke(
                                            if (isPartial) 1.5.dp else 1.dp,
                                            if (isPartial) primaryBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(58.dp)
                                            .testTag("payment_mode_partial")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = "⚖️ مدفوع جزئياً",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // 2. Middle: آجل (دين للمورد) ⏳
                                    val isCredit = paymentMode == "CREDIT"
                                    Surface(
                                        onClick = { paymentMode = "CREDIT" },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isCredit) softBlueBg else dialogBgColor,
                                        border = BorderStroke(
                                            if (isCredit) 1.5.dp else 1.dp,
                                            if (isCredit) primaryBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(58.dp)
                                            .testTag("payment_mode_credit")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = "آجل (دين للمورد)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "⏳",
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    // 3. Right: نقدي (مدفوع) 💵
                                    val isPaid = paymentMode == "PAID"
                                    Surface(
                                        onClick = { paymentMode = "PAID" },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isPaid) softBlueBg else dialogBgColor,
                                        border = BorderStroke(
                                            if (isPaid) 1.5.dp else 1.dp,
                                            if (isPaid) primaryBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(58.dp)
                                            .testTag("payment_mode_paid")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = "💵 نقدي (مدفوع)",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                if (paymentMode == "PARTIAL") {
                                    OutlinedTextField(
                                        value = customPaidAmountInput,
                                        onValueChange = { customPaidAmountInput = it },
                                        label = { Text("المبلغ المدفوع للمورد الآن ($currencySymbol)") },
                                        textStyle = TextStyle(textAlign = TextAlign.End, fontWeight = FontWeight.Bold),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Bottom Summary Row inside Section 3 Card:
                                // Left: المتبقي دين للمورد: 0.00 د.ج | Right: المدفوع: 0.00 د.ج
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المتبقي دين للمورد: ${String.format(Locale.US, "%.2f", remainingDebtAmount)} $currencySymbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        color = if (remainingDebtAmount > 0.009) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "المدفوع: ${String.format(Locale.US, "%.2f", resolvedPaidAmount)} $currencySymbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }

                        // Notes Field (below Section 3 Card)
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = {
                                Text(
                                    text = "ملاحظات إضافية (شروط التوريد / تاريخ الاستحقاق)",
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            textStyle = TextStyle(
                                textAlign = TextAlign.End,
                                fontSize = 13.5.sp
                            ),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = dialogBgColor,
                                unfocusedContainerColor = dialogBgColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("purchase_notes_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Bottom Footer Buttons (Left: حفظ الفاتورة وتحديث المخزون ✓ | Right: إلغاء) ──
                    val canSave = draftItems.isNotEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: حفظ الفاتورة وتحديث المخزون ✓
                        Surface(
                            onClick = {
                                if (canSave) {
                                    onSaveInvoice(
                                        invoiceNumber.trim().ifBlank { "PUR-10001" },
                                        supplierName.trim().ifBlank { "مورد عام" },
                                        supplierPhone.trim(),
                                        resolvedPaidAmount,
                                        notes.trim(),
                                        draftItems.toList()
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (canSave) primaryBlue else if (isDark) Color(0xFF334155) else Color(0xFFD5DEE6),
                            modifier = Modifier
                                .weight(1.85f)
                                .height(54.dp)
                                .testTag("save_purchase_invoice_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = "حفظ الفاتورة وتحديث المخزون",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (canSave) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF7A8B99),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (canSave) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF7A8B99),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Right: إلغاء
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = dialogBgColor,
                                contentColor = primaryBlue
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("cancel_purchase_invoice_btn")
                        ) {
                            Text(
                                text = "إلغاء",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = primaryBlue
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewProductModal) {
        NewPurchaseProductModal(
            currencySymbol = currencySymbol,
            onDismiss = { showNewProductModal = false },
            onAddProductToInvoice = { newItem ->
                draftItems.add(newItem)
                showNewProductModal = false
            }
        )
    }
}

@Composable
private fun PurchaseInvoiceDetailsDialog(
    invoice: PurchaseInvoice,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onPayDebt: (Double) -> Unit,
    onPayAndPrintReceipt: (Double) -> Unit = {}
) {
    val items = remember(invoice.itemsJson) { invoice.getItems() }
    val dateStr = remember(invoice.dateMillis) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date(invoice.dateMillis))
    }
    var paymentInput by remember(invoice.remainingDebt) {
        mutableStateOf(
            if (invoice.remainingDebt > 0.0) String.format(Locale.US, "%.2f", invoice.remainingDebt) else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "تفاصيل فاتورة الشراء ${invoice.invoiceNumber}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
                Text(
                    text = "المورد: ${invoice.supplierName} • $dateStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", item.subtotal)} $currencySymbol",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${item.productName} (${item.quantity} ${if (item.isCarton) "عبوة" else "قطعة"})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", invoice.totalCost)} $currencySymbol",
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("إجمالي الفاتورة:", fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", invoice.paidAmount)} $currencySymbol",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                    Text("المدفوع للمورد:", fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", invoice.remainingDebt)} $currencySymbol",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (invoice.remainingDebt > 0.009) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                    Text("المتبقي (آجل المورد):", fontWeight = FontWeight.Bold)
                }

                if (invoice.remainingDebt > 0.009) {
                    HorizontalDivider()
                    Text(
                        text = "تسديد دفعة للمورد",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paymentInput,
                        onValueChange = { paymentInput = it },
                        label = { Text("مبلغ الدفعة ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val amt = paymentInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    onPayAndPrintReceipt(amt)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("سداد وطباعة وصل", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                val amt = paymentInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    onPayDebt(amt)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("سداد فقط", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun CustomersScreen(viewModel: AntarSalesViewModel) {
    val canManageDebtsAndCustomers by viewModel.canManageDebtsAndCustomers.collectAsState()
    if (!canManageDebtsAndCustomers) {
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
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "إدارة العملاء والديون مقتصرة على الإدارة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ليس لديك صلاحية لعرض أو تعديل ديون العملاء والمعلومات الخاصة.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val semantic = LocalAFStoreSemanticColors.current
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val transactions by viewModel.customerTransactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val receiptFooterText by viewModel.receiptFooterText.collectAsState()
    val developerContact by viewModel.developerContact.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val archivedInvoices by viewModel.archivedInvoices.collectAsState()
    var showAddCustomer by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerForDetails by remember { mutableStateOf<Customer?>(null) }
    var customerLocationQrPreview by remember { mutableStateOf<Customer?>(null) }
    var paymentCustomer by remember { mutableStateOf<Customer?>(null) }
    var activeCustomerReceipt by remember { mutableStateOf<DebtPaymentReceiptData?>(null) }
    var shareCustomerPayload by remember { mutableStateOf<ShareCustomerDebtPayload?>(null) }
    var showCustomersPdfReportDialog by remember { mutableStateOf(false) }

    var customerSearchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedCustomerSearch by remember { mutableStateOf("") }

    LaunchedEffect(customerSearchQuery) {
        val trimmed = customerSearchQuery.trim()
        if (trimmed.isEmpty()) {
            debouncedCustomerSearch = ""
        } else {
            delay(90L)
            debouncedCustomerSearch = trimmed
        }
    }

    val filteredCustomers = remember(customers, debouncedCustomerSearch) {
        val q = debouncedCustomerSearch.trim()
        if (q.isEmpty()) customers
        else customers.filter {
            it.name.contains(q, ignoreCase = true) ||
            it.phone.contains(q, ignoreCase = true) ||
            it.email.contains(q, ignoreCase = true) ||
            it.address.contains(q, ignoreCase = true)
        }
    }

    val totalDebts = remember(customers) { customers.sumOf { it.balanceDebt } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCustomer = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 10.dp),
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "إضافة عميل", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("عميل جديد", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إجمالي مديونيات العملاء المستحقة", fontSize = 12.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalDebts)} $currencySymbol",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = if (totalDebts > 0) semantic.debtAmber else MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("عدد العملاء: ${customers.size}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        UniversalReportHeaderButton(
                            title = "التقارير PDF",
                            tag = "customers_pdf_report_btn",
                            onClick = { showCustomersPdfReportDialog = true }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = customerSearchQuery,
                onValueChange = { customerSearchQuery = it },
                placeholder = { Text("بحث عن عميل بالاسم، الهاتف، أو العنوان...", fontSize = 12.5.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", modifier = Modifier.size(18.dp)) },
                trailingIcon = if (customerSearchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { customerSearchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customers_screen_search_bar")
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = filteredCustomers,
                    key = { "cust_${it.id}" },
                    contentType = { "customer_card" }
                ) { customer ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = semantic.cardElevated
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (semantic.isDark) 0.dp else 2.dp
                        ),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = "الهاتف: ${customer.phone.ifBlank { "غير مسجل" }} • إجمالي المشتريات: ${String.format(Locale.US, "%.2f", customer.totalPurchases)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "المديونية الحالية: ${String.format(Locale.US, "%.2f", customer.balanceDebt)} $currencySymbol",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (customer.balanceDebt > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                val points = customer.loyaltyPoints
                                val goal = customer.loyaltyGoal.coerceAtLeast(1)
                                val progress = (points.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
                                val isRewardReady = points >= goal

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "🎁 النقاط: $points / $goal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRewardReady) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = if (isRewardReady) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )

                                    if (isRewardReady) {
                                        Text(
                                            text = "مكافأة جاهزة! 🎉",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { customerToEdit = customer },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp),
                                    border = BorderStroke(1.dp, semantic.cardBorder)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تعديل", fontSize = 11.sp)
                                }
                                
                                OutlinedButton(
                                    onClick = { customerForDetails = customer },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp),
                                    border = BorderStroke(1.dp, semantic.cardBorder)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = "تفاصيل", modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تفاصيل", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { paymentCustomer = customer },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp),
                                    border = BorderStroke(
                                        1.2.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (semantic.isDark) 0.55f else 0.78f)
                                    )
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("سداد دفعة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val custTxs = transactions
                                            .filter { it.customerId == customer.id }
                                            .sortedByDescending { it.date }
                                        val latestTx = custTxs.maxByOrNull { it.date }
                                        val latestInv = archivedInvoices
                                            .filter {
                                                (it.customerId != null && it.customerId == customer.id) ||
                                                    it.customerName.equals(customer.name, ignoreCase = true)
                                            }
                                            .maxByOrNull { it.dateMillis }
                                        shareCustomerPayload = ShareCustomerDebtPayload(
                                            customer = customer,
                                            transactions = custTxs,
                                            latestInvoice = latestInv,
                                            focusTransaction = latestTx,
                                            initialMode = if (latestInv != null || latestTx != null) "INVOICE" else "STATEMENT"
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("customers_screen_share_btn_${customer.id}"),
                                    border = BorderStroke(
                                        1.2.dp,
                                        Color(0xFF16A34A).copy(alpha = if (semantic.isDark) 0.75f else 0.88f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "مشاركة عبر واتساب أو ماسنجر",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        "واتساب / ماسنجر",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }

                        if (customer.hasLocation()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = semantic.cardBorder.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable {
                                        CustomerLocationHelper.openInGoogleMaps(
                                            context,
                                            customer.latitude,
                                            customer.longitude,
                                            customer.locationUrl
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "موقع العميل",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "موقع GPS محدد (انقر للفتح بالخرائط)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                                OutlinedButton(
                                    onClick = { customerLocationQrPreview = customer },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        Icons.Default.QrCode2,
                                        contentDescription = "رمز QR",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("رمز QR", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (transactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "آخر حركات التحصيل والمديونيات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    itemsIndexed(
                        items = transactions.take(15),
                        key = { index, tx -> "tx_${tx.id}_$index" },
                        contentType = { _, _ -> "customer_tx" }
                    ) { _, tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${tx.customerName} (${if (tx.type == "PAYMENT") "سداد" else "مديونية"}) - ${tx.notes}",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", tx.amount)} $currencySymbol",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == "PAYMENT") Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                                if (tx.type == "PAYMENT") {
                                    OutlinedButton(
                                        onClick = {
                                            val cust = customers.find { it.id == tx.customerId }
                                            val currentDebt = cust?.balanceDebt ?: 0.0
                                            val laterTxs = transactions.filter { t ->
                                                t.customerId == tx.customerId &&
                                                    (t.date > tx.date || (t.date == tx.date && t.id > tx.id))
                                            }
                                            val laterPayments = laterTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                                            val laterDebts = laterTxs.filter { it.type != "PAYMENT" }.sumOf { it.amount }
                                            val remAfter = (currentDebt + laterPayments - laterDebts).coerceAtLeast(0.0)
                                            val prevBefore = (remAfter + tx.amount).coerceAtLeast(tx.amount)
                                            activeCustomerReceipt = DebtPaymentReceiptData(
                                                receiptNumber = "PAY-${(tx.date / 1000).toString().takeLast(6)}",
                                                partyType = "CUSTOMER",
                                                partyName = cust?.name ?: tx.customerName,
                                                partyPhone = cust?.phone.orEmpty(),
                                                previousBalance = prevBefore,
                                                paidAmount = tx.amount,
                                                remainingBalance = remAfter,
                                                notes = tx.notes.ifBlank { "تحصيل دفعة نقدية من حساب المديونية" },
                                                timestampMillis = tx.date
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.85f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "وصل السداد",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("وصل السداد", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCustomer || customerToEdit != null) {
        val isEditing = customerToEdit != null
        val target = customerToEdit
        var name by remember(target) { mutableStateOf(target?.name ?: "") }
        var phone by remember(target) { mutableStateOf(target?.phone ?: "") }
        var email by remember(target) { mutableStateOf(target?.email ?: "") }
        var address by remember(target) { mutableStateOf(target?.address ?: "") }
        var initialDebt by remember(target) { mutableStateOf("") }
        var latitude by remember(target) { mutableStateOf(target?.latitude) }
        var longitude by remember(target) { mutableStateOf(target?.longitude) }
        var locationUrl by remember(target) { mutableStateOf(target?.locationUrl ?: "") }
        var isFetchingGps by remember { mutableStateOf(false) }
        var showManualLocationFields by remember { mutableStateOf(false) }
        var loyaltyPoints by remember(target) { mutableStateOf(target?.loyaltyPoints?.toString() ?: "0") }
        var loyaltyGoal by remember(target) { mutableStateOf(target?.loyaltyGoal?.toString() ?: "500") }
        var loyaltyRewardValue by remember(target) { mutableStateOf(target?.loyaltyRewardValue?.toString() ?: "50.0") }
        var loyaltyTier by remember(target) { mutableStateOf(target?.loyaltyTier ?: "Standard") }

        val effectiveMapsUrl = remember(latitude, longitude, locationUrl) {
            if (locationUrl.isNotBlank()) locationUrl.trim()
            else if (latitude != null && longitude != null && (latitude != 0.0 || longitude != 0.0)) {
                CustomerLocationHelper.buildGoogleMapsUrl(latitude!!, longitude!!)
            } else null
        }

        val locationQrBitmap = remember(effectiveMapsUrl) {
            effectiveMapsUrl?.let { CustomerLocationHelper.generateLocationQrBitmap(it, 160) }
        }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                isFetchingGps = true
                CustomerLocationHelper.requestSingleLocationFix(
                    context = context,
                    onSuccess = { lat, lng ->
                        isFetchingGps = false
                        latitude = lat
                        longitude = lng
                        locationUrl = CustomerLocationHelper.buildGoogleMapsUrl(lat, lng)
                        Toast.makeText(context, "تم التقاط إحداثيات الموقع بنجاح عبر GPS", Toast.LENGTH_SHORT).show()
                    },
                    onError = { errorMsg ->
                        isFetchingGps = false
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(context, "يرجى منح إذن الموقع لتحديد إحداثيات العميل", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog(
            onDismissRequest = {
                showAddCustomer = false
                customerToEdit = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isEditing) Icons.Default.Edit else Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEditing) "تعديل بيانات وموقع العميل" else "إضافة عميل جديد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم العميل *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني (Email)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان النصي / الحي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!isEditing) {
                        OutlinedTextField(
                            value = initialDebt,
                            onValueChange = { initialDebt = it },
                            label = { Text("رصيد مديونية افتتاحي (اختياري)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // --- CUSTOMER LOYALTY CARD ---
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "مكافآت وأهداف ولاء العميل (Loyalty)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = loyaltyPoints,
                                    onValueChange = { loyaltyPoints = it },
                                    label = { Text("النقاط الحالية") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = loyaltyGoal,
                                    onValueChange = { loyaltyGoal = it },
                                    label = { Text("هدف النقاط (الأولمبي)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = loyaltyRewardValue,
                                onValueChange = { loyaltyRewardValue = it },
                                label = { Text("قيمة كوبون الخصم عند الهدف (د.ج)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "مستوى وفئة ولاء العميل (Tier Status):",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Standard" to "عادي (Standard)",
                                    "Silver" to "فضي (Silver)",
                                    "Gold" to "ذهبي (Gold)",
                                    "Platinum" to "بلاتيني (Platinum)"
                                ).forEach { (tierKey, tierLabel) ->
                                    val isSelected = loyaltyTier.equals(tierKey, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { loyaltyTier = tierKey },
                                        label = { Text(tierLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }
                        }
                    }

                    // --- GPS & GOOGLE MAPS LOCATION SECTION ---
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (effectiveMapsUrl != null)
                                Color(0xFF10B981).copy(alpha = if (semantic.isDark) 0.12f else 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (effectiveMapsUrl != null) Color(0xFF10B981).copy(alpha = 0.4f) else semantic.cardBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (effectiveMapsUrl != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "موقع العميل (GPS & Google Maps)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                if (effectiveMapsUrl != null) {
                                    Text(
                                        text = "✓ محدد",
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // GPS Button & Map Open Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (CustomerLocationHelper.hasLocationPermission(context)) {
                                            isFetchingGps = true
                                            CustomerLocationHelper.requestSingleLocationFix(
                                                context = context,
                                                onSuccess = { lat, lng ->
                                                    isFetchingGps = false
                                                    latitude = lat
                                                    longitude = lng
                                                    locationUrl = CustomerLocationHelper.buildGoogleMapsUrl(lat, lng)
                                                    Toast.makeText(context, "تم التقاط إحداثيات الموقع بنجاح", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    isFetchingGps = false
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    enabled = !isFetchingGps,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    if (isFetchingGps) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("جاري التقاط GPS...", fontSize = 11.5.sp)
                                    } else {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("موقعي الحالي GPS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (effectiveMapsUrl != null) {
                                    OutlinedButton(
                                        onClick = {
                                            CustomerLocationHelper.openInGoogleMaps(context, latitude, longitude, locationUrl)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("عرض بالخريطة", fontSize = 11.5.sp)
                                    }
                                }
                            }

                            // Manual Coordinates or Link expand button
                            TextButton(
                                onClick = { showManualLocationFields = !showManualLocationFields },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    if (showManualLocationFields) "إخفاء التفاصيل اليدوية" else "أو إدخال رابط خرائط Google / الإحداثيات يدوياً ▾",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (showManualLocationFields) {
                                OutlinedTextField(
                                    value = locationUrl,
                                    onValueChange = { locationUrl = it },
                                    label = { Text("رابط موقع Google Maps") },
                                    placeholder = { Text("https://maps.google.com/?q=...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = latitude?.toString() ?: "",
                                        onValueChange = { latitude = it.toDoubleOrNull() },
                                        label = { Text("خط العرض (Lat)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = longitude?.toString() ?: "",
                                        onValueChange = { longitude = it.toDoubleOrNull() },
                                        label = { Text("خط الطول (Lng)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Live QR Code Preview
                            if (locationQrBitmap != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color.LightGray),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.QrCode2,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0F172A),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "رمز QR لموقع العميل",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                            Text(
                                                "يظهر بجانب اسم العميل في قوالب الفواتير الخمسة لسهولة وصول السائقين",
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF64748B),
                                                lineHeight = 14.sp
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Image(
                                            bitmap = locationQrBitmap.asImageBitmap(),
                                            contentDescription = "معاينة QR Code لموقع العميل",
                                            modifier = Modifier.size(54.dp)
                                        )
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
                        if (name.isNotBlank()) {
                            if (isEditing && target != null) {
                                viewModel.saveCustomer(
                                    target.copy(
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        address = address.trim(),
                                        latitude = latitude,
                                        longitude = longitude,
                                        locationUrl = locationUrl.trim(),
                                        loyaltyPoints = loyaltyPoints.toIntOrNull() ?: 0,
                                        loyaltyGoal = loyaltyGoal.toIntOrNull() ?: 500,
                                        loyaltyRewardValue = loyaltyRewardValue.toDoubleOrNull() ?: 50.0,
                                        loyaltyTier = loyaltyTier,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                Toast.makeText(context, "تم تحديث بيانات العميل ومكافآته بنجاح", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.saveCustomer(
                                    Customer(
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        address = address.trim(),
                                        latitude = latitude,
                                        longitude = longitude,
                                        locationUrl = locationUrl.trim(),
                                        balanceDebt = initialDebt.toDoubleOrNull() ?: 0.0,
                                        loyaltyPoints = loyaltyPoints.toIntOrNull() ?: 0,
                                        loyaltyGoal = loyaltyGoal.toIntOrNull() ?: 500,
                                        loyaltyRewardValue = loyaltyRewardValue.toDoubleOrNull() ?: 50.0,
                                        loyaltyTier = loyaltyTier
                                    )
                                )
                                Toast.makeText(context, "تم حفظ العميل الجديد والبرنامج الترحيبي بنجاح", Toast.LENGTH_SHORT).show()
                            }
                            showAddCustomer = false
                            customerToEdit = null
                        }
                    }
                ) {
                    Text(if (isEditing) "تحديث البيانات" else "حفظ العميل")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCustomer = false
                    customerToEdit = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Customer Complete Details, Contact & Purchase History Dialog
    customerForDetails?.let { cust ->
        val custInvoices = remember(archivedInvoices, cust) {
            archivedInvoices.filter {
                (it.customerId != null && it.customerId == cust.id) ||
                it.customerName.equals(cust.name, ignoreCase = true)
            }.sortedByDescending { it.dateMillis }
        }
        val custTransactions = remember(transactions, cust) {
            transactions.filter { it.customerId == cust.id }
                .sortedByDescending { it.date }
        }

        AlertDialog(
            onDismissRequest = { customerForDetails = null },
            title = {
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
                            color = when (cust.loyaltyTier.lowercase()) {
                                "platinum" -> Color(0xFF6366F1)
                                "gold" -> Color(0xFFF59E0B)
                                "silver" -> Color(0xFF64748B)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = cust.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                            }
                        }
                        Column {
                            Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "فئة الولاء: ${
                                    when (cust.loyaltyTier.lowercase()) {
                                        "platinum" -> "بلاتيني (Platinum)"
                                        "gold" -> "ذهبي (Gold)"
                                        "silver" -> "فضي (Silver)"
                                        else -> "عادي (Standard)"
                                    }
                                }",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Contact Info Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("بيانات الاتصال والعنوان", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            if (cust.phone.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الهاتف: ${cust.phone}", fontSize = 12.sp)
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                    data = android.net.Uri.parse("tel:${cust.phone}")
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            if (cust.email.isNotBlank()) {
                                Text("البريد الإلكتروني: ${cust.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (cust.address.isNotBlank()) {
                                Text("العنوان: ${cust.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (cust.hasLocation()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("موقع الخرائط GPS", fontSize = 11.5.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                    TextButton(
                                        onClick = {
                                            CustomerLocationHelper.openInGoogleMaps(context, cust.latitude, cust.longitude, cust.locationUrl)
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("فتح بالخرائط ↗", fontSize = 11.sp, color = Color(0xFF16A34A))
                                    }
                                }
                            }
                        }
                    }

                    // Financial & Loyalty Overview Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المديونية المستحقة:", fontSize = 12.sp)
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", cust.balanceDebt)} $currencySymbol",
                                    fontWeight = FontWeight.Bold,
                                    color = if (cust.balanceDebt > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                                    fontSize = 12.5.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي المشتريات:", fontSize = 12.sp)
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", cust.totalPurchases)} $currencySymbol",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                            HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.5f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("نقاط الولاء الحالية:", fontSize = 12.sp)
                                Text(
                                    text = "${cust.loyaltyPoints} / ${cust.loyaltyGoal} نقطة",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.5.sp
                                )
                            }
                            val progress = (cust.loyaltyPoints.toFloat() / cust.loyaltyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (cust.loyaltyPoints >= cust.loyaltyGoal) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            )
                            if (cust.loyaltyPoints >= cust.loyaltyGoal) {
                                Text(
                                    text = "مكافأة خصم جاهزة بقيمة: ${cust.loyaltyRewardValue} $currencySymbol 🎉",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    // Purchase & Invoices History Section
                    Text("سجل الفواتير والمشتريات (${custInvoices.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (custInvoices.isEmpty()) {
                        Text("لا توجد فواتير مؤرشفة مسجلة لهذا العميل حتى الآن.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        custInvoices.take(10).forEach { inv ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, semantic.cardBorder.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val formattedDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH).format(Date(inv.dateMillis))
                                    Column {
                                        Text("فاتورة #${inv.invoiceId}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                        Text(formattedDate, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        "${String.format(Locale.US, "%.2f", inv.subtotal)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Transactions & Debt Payments History Section
                    Text("سجل حركات الحساب والتحصيلات (${custTransactions.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (custTransactions.isEmpty()) {
                        Text("لا توجد حركات مديونية أو سداد مسجلة.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        custTransactions.take(10).forEach { tx ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, semantic.cardBorder.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            if (tx.type == "PAYMENT") "سداد دفعة" else "مديونية جديدة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = if (tx.type == "PAYMENT") Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                        if (tx.notes.isNotBlank()) {
                                            Text(tx.notes, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        "${String.format(Locale.US, "%.2f", tx.amount)} $currencySymbol",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (tx.type == "PAYMENT") Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val target = cust
                            customerForDetails = null
                            customerToEdit = target
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تعديل العميل")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { customerForDetails = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Quick QR Code Full Preview Dialog
    customerLocationQrPreview?.let { cust ->
        val mapsUrl = cust.getEffectiveMapsUrl() ?: ""
        val qrBmp = remember(mapsUrl) {
            CustomerLocationHelper.generateLocationQrBitmap(mapsUrl, 320)
        }
        AlertDialog(
            onDismissRequest = { customerLocationQrPreview = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF16A34A))
                    Spacer(Modifier.width(8.dp))
                    Text("موقع العميل: ${cust.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (qrBmp != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, Color(0xFF0F172A)),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Image(
                                bitmap = qrBmp.asImageBitmap(),
                                contentDescription = "QR موقع العميل",
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(8.dp)
                            )
                        }
                    }
                    Text(
                        "قم بمسح رمز QR بكاميرا الهاتف أو تطبيق الخرائط للوصول لموقع تسليم العميل فوراً.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        CustomerLocationHelper.openInGoogleMaps(context, cust.latitude, cust.longitude, cust.locationUrl)
                    }
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("فتح في خرائط Google")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerLocationQrPreview = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    paymentCustomer?.let { target ->
        var amount by remember(target.balanceDebt) {
            mutableStateOf(if (target.balanceDebt > 0) String.format(Locale.US, "%.2f", target.balanceDebt) else "")
        }
        var notes by remember { mutableStateOf("تحصيل دفعة نقدية من حساب المديونية") }

        AlertDialog(
            onDismissRequest = { paymentCustomer = null },
            title = { Text("تحصيل دفعة من ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرصيد السابق المستحق: ${String.format(Locale.US, "%.2f", target.balanceDebt)} $currencySymbol")
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("المبلغ المحصّل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات السداد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.recordCustomerPayment(target, amt, notes)
                                paymentCustomer = null
                            }
                        }
                    ) {
                        Text("تحصيل فقط", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                val prevDebt = target.balanceDebt
                                val remDebt = (prevDebt - amt).coerceAtLeast(0.0)
                                val now = System.currentTimeMillis()
                                viewModel.recordCustomerPayment(target, amt, notes)
                                paymentCustomer = null
                                activeCustomerReceipt = DebtPaymentReceiptData(
                                    receiptNumber = "PAY-${(now / 1000).toString().takeLast(6)}",
                                    partyType = "CUSTOMER",
                                    partyName = target.name,
                                    partyPhone = target.phone,
                                    previousBalance = prevDebt,
                                    paidAmount = amt,
                                    remainingBalance = remDebt,
                                    notes = notes.ifBlank { "تحصيل دفعة نقدية من حساب المديونية" },
                                    timestampMillis = now
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تحصيل وطباعة وصل", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentCustomer = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    activeCustomerReceipt?.let { receiptData ->
        DebtPaymentReceiptDialog(
            receipt = receiptData,
            viewModel = viewModel,
            storeName = storeName,
            storePhone = storePhone,
            cashierName = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: currentUser?.username?.takeIf { !it.contains("@") } ?: "Antar",
            currencySymbol = currencySymbol,
            footerText = receiptFooterText,
            developerContact = developerContact,
            context = context,
            onDismiss = { activeCustomerReceipt = null }
        )
    }

    shareCustomerPayload?.let { payload ->
        ShareCustomerDebtAndInvoiceDialog(
            payload = payload,
            storeName = storeName,
            storePhone = storePhone,
            currencySymbol = currencySymbol,
            footerText = receiptFooterText,
            context = context,
            onDismiss = { shareCustomerPayload = null }
        )
    }

    if (showCustomersPdfReportDialog) {
        val payload = remember(storeName, storePhone, currencySymbol, customers) {
            UniversalPdfReportExporter.buildCustomersPayload(
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol,
                customers = customers
            )
        }
        UniversalSectionPdfReportDialog(
            payload = payload,
            onDismiss = { showCustomersPdfReportDialog = false }
        )
    }
}
