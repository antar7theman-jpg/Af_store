package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import com.example.ui.components.ModernChipButton
import com.example.ui.components.ModernDangerButton
import com.example.ui.components.ModernGradientButton
import com.example.ui.components.ModernIconButton
import com.example.ui.components.ModernOutlinedButton
import com.example.ui.components.ModernPrimaryButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.ArchivedInvoice
import com.example.data.models.InvoiceStyle
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.ui.viewmodels.CartItem
import com.example.utils.CheckoutThermalPrintMode
import com.example.utils.ThermalConnectionState
import com.example.utils.ThermalPaperWidth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PosCheckoutReviewSheet(
    viewModel: AntarSalesViewModel,
    onDismiss: () -> Unit,
    onOpenCustomerPicker: () -> Unit,
    onInvoiceFinalizedAndPrint: (ArchivedInvoice) -> Unit,
    onPreviewInvoice: (ArchivedInvoice) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalAFStoreSemanticColors.current.isDark

    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val isLoyaltyDiscountApplied by viewModel.isLoyaltyDiscountApplied.collectAsStateWithLifecycle()
    val loyaltyValidityDays by viewModel.loyaltyValidityDays.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val customerCustomPrices by viewModel.customerCustomPrices.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val isInvoiceDiscountPercent by viewModel.isInvoiceDiscountPercent.collectAsStateWithLifecycle()
    val invoiceDiscountValue by viewModel.invoiceDiscountValue.collectAsStateWithLifecycle()
    val isInvoiceTvaEnabled by viewModel.isInvoiceTvaEnabled.collectAsStateWithLifecycle()
    val invoiceTvaPercent by viewModel.invoiceTvaPercent.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val storePhone by viewModel.storePhone.collectAsStateWithLifecycle()
    val receiptFooterText by viewModel.receiptFooterText.collectAsStateWithLifecycle()
    val developerContact by viewModel.developerContact.collectAsStateWithLifecycle()
    val selectedStyle by viewModel.invoiceStyle.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val thermalPrinterConfig by viewModel.thermalPrinterConfig.collectAsStateWithLifecycle()
    val thermalConnectionState by viewModel.thermalConnectionState.collectAsStateWithLifecycle()
    val isThermalPrinting by viewModel.isThermalPrinting.collectAsStateWithLifecycle()

    // Adaptive debt/credit accent colors for Dark & Light themes
    val debtAccentColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val debtContainerColor = if (isDark) Color(0xFF3B2A10) else Color(0xFFFEF3C7)
    val debtBorderColor = if (isDark) Color(0xFFB45309) else Color(0xFFF59E0B)

    var discountTextInput by remember(invoiceDiscountValue) {
        mutableStateOf(
            if (invoiceDiscountValue <= 0.0) ""
            else if (invoiceDiscountValue % 1.0 == 0.0) invoiceDiscountValue.toInt().toString()
            else invoiceDiscountValue.toString()
        )
    }

    var tvaRateTextInput by remember(invoiceTvaPercent) {
        mutableStateOf(
            if (invoiceTvaPercent % 1.0 == 0.0) invoiceTvaPercent.toInt().toString()
            else invoiceTvaPercent.toString()
        )
    }

    var itemForDiscountDialog by remember { mutableStateOf<CartItem?>(null) }
    var itemForTaxDialog by remember { mutableStateOf<CartItem?>(null) }
    var itemForPriceDialog by remember { mutableStateOf<CartItem?>(null) }
    var itemForDebtDialog by remember { mutableStateOf<CartItem?>(null) }
    var showTemplatePreviewDialog by remember { mutableStateOf(false) }
    var showHeaderEditorDialog by remember { mutableStateOf(false) }
    var itemForImagePreview by remember { mutableStateOf<CartItem?>(null) }
    var showBluetoothPrinterPicker by remember { mutableStateOf(false) }
    var showCheckoutBarcodeScanner by remember { mutableStateOf(false) }
    var unifiedViewMode by remember { mutableStateOf("ALL") } // "ALL", "ITEMS", "PAPER"
    var selectedCopies by remember(thermalPrinterConfig.defaultCopies) {
        mutableIntStateOf(thermalPrinterConfig.defaultCopies.coerceIn(1, 5))
    }

    val isAllCredit = paymentMethod == "DEBT"
    val hasAnyItemDebt = cartItems.any { it.hasAnyDebt }
    val totalUnitsCount = remember(cartItems) { cartItems.sumOf { it.effectiveUnitsDeducted } }
    val subtotal = remember(cartItems) { cartItems.sumOf { it.lineSubtotal } }
    val invoiceDiscountAmount = remember(subtotal, isInvoiceDiscountPercent, invoiceDiscountValue) {
        if (invoiceDiscountValue <= 0.0) 0.0
        else if (isInvoiceDiscountPercent) subtotal * (invoiceDiscountValue.coerceIn(0.0, 100.0) / 100.0)
        else invoiceDiscountValue.coerceIn(0.0, subtotal)
    }
    val afterDiscount = (subtotal - invoiceDiscountAmount).coerceAtLeast(0.0)
    val invoiceTvaAmount = remember(afterDiscount, isInvoiceTvaEnabled, invoiceTvaPercent) {
        if (!isInvoiceTvaEnabled) 0.0
        else afterDiscount * (invoiceTvaPercent.coerceAtLeast(0.0) / 100.0)
    }
    val netTotal = remember(afterDiscount, invoiceTvaAmount, isLoyaltyDiscountApplied, selectedCustomer, loyaltyValidityDays) {
        val baseTotal = afterDiscount + invoiceTvaAmount
        val cust = selectedCustomer
        val isExpired = viewModel.isCustomerLoyaltyExpired(cust)
        if (isLoyaltyDiscountApplied && cust != null && cust.loyaltyPoints >= cust.loyaltyGoal && !isExpired) {
            (baseTotal - cust.loyaltyRewardValue).coerceAtLeast(0.0)
        } else {
            baseTotal
        }
    }

    val totalDebtPortion = remember(cartItems, isAllCredit, subtotal, netTotal) {
        if (isAllCredit) {
            netTotal
        } else {
            val ratio = if (subtotal > 0.0) netTotal / subtotal else 1.0
            cartItems.sumOf { item ->
                val itemDebt = if (item.customDebtAmount != null) {
                    item.customDebtAmount.coerceIn(0.0, item.lineSubtotal)
                } else if (item.isItemDebt) {
                    item.lineSubtotal
                } else {
                    0.0
                }
                itemDebt * ratio
            }
        }
    }

    // Live Paper Receipt Preview Data
    val nowMillis = remember { System.currentTimeMillis() }
    val shortInvoiceNumber = remember(nowMillis) { (nowMillis / 1000).toString().takeLast(6) }
    val formattedDateTime = remember(nowMillis) {
        SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.US).format(Date(nowMillis))
    }
    val customerDisplayName = remember(selectedCustomer) {
        selectedCustomer?.name?.ifBlank { "عميل نقدي" } ?: "عميل نقدي"
    }
    val cashierDisplayName = remember(currentUser) {
        currentUser?.fullName?.takeIf { it.isNotBlank() } ?: currentUser?.username?.takeIf { !it.contains("@") } ?: "Antar"
    }

    val previewLines: List<PreviewLineItem> = remember(cartItems, paymentMethod) {
        val wholeCartDebt = paymentMethod == "DEBT"
        cartItems.mapIndexed { idx, item ->
            val rawSub = item.rawLineSubtotal
            val discAmt = item.itemDiscountAmount
            val discPct = if (rawSub > 0.0 && discAmt > 0.0) {
                if (item.isItemDiscountPercent) item.itemDiscountValue
                else (discAmt / rawSub) * 100.0
            } else 0.0
            val afterDisc = item.subtotalAfterItemDiscount
            val tvaAmt = item.itemTvaAmount
            val tvaPct = if (item.hasItemTva && tvaAmt > 0.0) item.itemTvaPercent else 0.0

            PreviewLineItem(
                name = item.product.name,
                quantityPieces = item.effectiveUnitsDeducted,
                unitPrice = item.customUnitPrice,
                rawSubtotal = rawSub,
                subtotalAfterDiscount = afterDisc,
                itemDiscountAmount = discAmt,
                itemDiscountPercent = discPct,
                itemTvaAmount = tvaAmt,
                itemTvaPercent = tvaPct,
                finalLineTotal = item.lineSubtotal,
                unitsPerCarton = item.unitsPerCartonSafe,
                barcode = item.product.barcode.ifBlank { "622100100${idx + 1}" },
                isDebt = wholeCartDebt || item.hasAnyDebt,
                customDebtAmount = if (wholeCartDebt) null else item.customDebtAmount
            )
        }
    }

    val rawSousTotal = remember(previewLines) { previewLines.sumOf { it.rawSubtotal } }
    val itemsDiscountTotal = remember(previewLines) { previewLines.sumOf { it.itemDiscountAmount } }
    val totalCombinedDiscount = itemsDiscountTotal + invoiceDiscountAmount
    val effectiveDiscountPercentInt = remember(rawSousTotal, totalCombinedDiscount, itemsDiscountTotal, isInvoiceDiscountPercent, invoiceDiscountValue) {
        if (totalCombinedDiscount <= 0.0 || rawSousTotal <= 0.0) 0
        else if (itemsDiscountTotal <= 0.0 && isInvoiceDiscountPercent) invoiceDiscountValue.roundToInt()
        else ((totalCombinedDiscount / rawSousTotal) * 100.0).roundToInt().coerceAtLeast(1)
    }
    val itemsTvaTotal = remember(previewLines) { previewLines.sumOf { it.itemTvaAmount } }
    val totalCombinedTva = itemsTvaTotal + invoiceTvaAmount
    val hasDebtItems = remember(previewLines) { previewLines.any { it.isDebt } }
    val hasCashItems = remember(previewLines) { previewLines.any { !it.isDebt || it.isPartialDebt } }
    val paymentMethodAr = remember(hasDebtItems, hasCashItems, paymentMethod) {
        when {
            hasDebtItems && hasCashItems && paymentMethod == "TRANSFER" -> "دين وتحويل"
            hasDebtItems && hasCashItems -> "دين ونقد"
            hasDebtItems || paymentMethod == "DEBT" -> "دين"
            paymentMethod == "TRANSFER" -> "تحويل"
            else -> "نقدا"
        }
    }
    val especesAmount = remember(netTotal, totalDebtPortion) {
        (netTotal - totalDebtPortion).coerceAtLeast(0.0)
    }
    val totalCartonsCount = remember(previewLines) { previewLines.sumOf { it.fullCartons } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .fillMaxHeight(0.93f),
                shape = RoundedCornerShape(26.dp),
                color = colorScheme.surface,
                contentColor = colorScheme.onSurface,
                tonalElevation = 4.dp,
                shadowElevation = 18.dp,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Surface(
                        color = colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 10.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(4.5.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.outlineVariant)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Right Side (Child 1 in RTL): Title & Items count
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "المعاينة وإتمام البيع",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorScheme.onSurface
                                    )
                                    Surface(
                                        color = colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${cartItems.size} أصناف • $totalUnitsCount ق",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                // Left Side (Child 2 in RTL): Actions (+ مسح صنف & Close button)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        onClick = { showCheckoutBarcodeScanner = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.65f)),
                                        modifier = Modifier.testTag("checkout_quick_scan_barcode_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.QrCodeScanner,
                                                contentDescription = "مسح صنف إضافي",
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "+ مسح صنف",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF059669)
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = onDismiss,
                                        shape = CircleShape,
                                        color = colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("close_checkout_review_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "إغلاق المراجعة",
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Unified Section Mode Filter Bar (الكل مدمج | الأصناف | الفاتورة الورقية)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val viewModes = listOf(
                                    "ALL" to "📋 الكل (معاينة موحدة)",
                                    "ITEMS" to "🛒 الأصناف (${cartItems.size})",
                                    "PAPER" to "🧾 الفاتورة الورقية"
                                )
                                viewModes.forEach { (modeKey, modeLabel) ->
                                    val isModeSelected = unifiedViewMode == modeKey
                                    Surface(
                                        onClick = { unifiedViewMode = modeKey },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isModeSelected) colorScheme.primaryContainer else colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isModeSelected) colorScheme.primary else colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .testTag("unified_checkout_mode_$modeKey")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = modeLabel,
                                                fontSize = 11.sp,
                                                fontWeight = if (isModeSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isModeSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = colorScheme.outlineVariant.copy(alpha = 0.7f)
                    )

                    // Unified Scrollable Body
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // 1. TOP PRIORITY: Final Payment Method & Customer Selection
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.2.dp, colorScheme.primary.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "اختيار طريقة الدفع النهائية والعميل:",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colorScheme.onSurface
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isAllCredit || hasAnyItemDebt) debtContainerColor else colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "المحدد: $paymentMethodAr",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isAllCredit || hasAnyItemDebt) debtAccentColor else colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Cash Payment Button (Right in RTL)
                                        val isCashSelected = paymentMethod == "CASH"
                                        Surface(
                                            onClick = { viewModel.setPosPaymentMethod("CASH") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("checkout_cash_btn"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isCashSelected) colorScheme.primaryContainer else colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (isCashSelected) 1.8.dp else 1.dp,
                                                color = if (isCashSelected) colorScheme.primary else colorScheme.outlineVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Payments,
                                                    contentDescription = null,
                                                    tint = if (isCashSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "دفع نقدي",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (isCashSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // 2. Bank/Wallet Transfer Button (Center)
                                        val isTransferSelected = paymentMethod == "TRANSFER"
                                        Surface(
                                            onClick = { viewModel.setPosPaymentMethod("TRANSFER") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("checkout_transfer_btn"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isTransferSelected) colorScheme.primaryContainer else colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (isTransferSelected) 1.8.dp else 1.dp,
                                                color = if (isTransferSelected) colorScheme.primary else colorScheme.outlineVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = if (isTransferSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "تحويل",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (isTransferSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                                )
                                            }
                                        }

                                        // 3. All Cart Debt Button (Left in RTL)
                                        Surface(
                                            onClick = { viewModel.setPosPaymentMethod("DEBT") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("checkout_all_credit_btn"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isAllCredit) debtContainerColor else colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (isAllCredit) 1.8.dp else 1.dp,
                                                color = if (isAllCredit) debtBorderColor else colorScheme.outlineVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.WarningAmber,
                                                    contentDescription = null,
                                                    tint = debtAccentColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "كل السلة دين",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (isAllCredit) debtAccentColor else colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Customer Selector Row
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val requiresCustomerWarning = (isAllCredit || hasAnyItemDebt) && selectedCustomer == null
                                    Surface(
                                        onClick = onOpenCustomerPicker,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkout_customer_selector"),
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (requiresCustomerWarning) {
                                            colorScheme.errorContainer.copy(alpha = 0.45f)
                                        } else {
                                            colorScheme.surface
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (requiresCustomerWarning) colorScheme.error else colorScheme.outlineVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Right Side (Child 1 in RTL): Customer Name and Info
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column(horizontalAlignment = Alignment.Start) {
                                                    Text(
                                                        text = selectedCustomer?.name ?: "عميل نقدي عام",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = colorScheme.onSurface
                                                    )
                                                    if (requiresCustomerWarning) {
                                                        Text(
                                                            text = "مطلوب تحديد عميل لتسجيل الدين",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = colorScheme.error,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    } else if (selectedCustomer != null && selectedCustomer!!.balanceDebt > 0.0) {
                                                        Text(
                                                            text = "مديونية سابقة: ${String.format(Locale.US, "%.2f", selectedCustomer!!.balanceDebt)} $currency",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = debtAccentColor,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            // Left Side (Child 2 in RTL): Selection Button
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = if (selectedCustomer == null) "اختيار / إضافة عميل" else "تغيير العميل",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 2. Quick Invoice Discount & TVA Section
                        item {
                            val quickPercents = listOf(20, 15, 10, 5, 0)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "الخصم السريع على الفاتورة:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colorScheme.onSurface
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Percent Toggle
                                            Surface(
                                                onClick = { viewModel.setInvoiceDiscountMode(true) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isInvoiceDiscountPercent) colorScheme.primaryContainer else colorScheme.surface,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isInvoiceDiscountPercent) colorScheme.primary else colorScheme.outlineVariant
                                                ),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "% نسبة مئوية",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isInvoiceDiscountPercent) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Fixed Amount Toggle
                                            Surface(
                                                onClick = { viewModel.setInvoiceDiscountMode(false) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (!isInvoiceDiscountPercent) colorScheme.primaryContainer else colorScheme.surface,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (!isInvoiceDiscountPercent) colorScheme.primary else colorScheme.outlineVariant
                                                ),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$currency مبلغ ثابت",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (!isInvoiceDiscountPercent) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        quickPercents.forEach { pct ->
                                            val isSelected = isInvoiceDiscountPercent && abs(invoiceDiscountValue - pct.toDouble()) < 0.01
                                            Surface(
                                                onClick = {
                                                    viewModel.setInvoiceDiscountMode(true)
                                                    discountTextInput = if (pct == 0) "" else pct.toString()
                                                    viewModel.setInvoiceDiscountInput(pct.toDouble())
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surface,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) colorScheme.primary else colorScheme.outlineVariant
                                                )
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "$pct%",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.5.sp,
                                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = discountTextInput,
                                        onValueChange = { raw ->
                                            discountTextInput = raw
                                            viewModel.setInvoiceDiscountInput(raw.toDoubleOrNull() ?: 0.0)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkout_discount_input"),
                                        placeholder = {
                                            Text(
                                                text = if (isInvoiceDiscountPercent) {
                                                    "أدخل نسبة الخصم يدوياً (مثال: 5 أو 10)"
                                                } else {
                                                    "أدخل مبلغ الخصم الثابت ($currency)"
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                fontSize = 12.5.sp,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = {
                                            Text(
                                                text = if (isInvoiceDiscountPercent) "%" else currency,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colorScheme.primary,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = colorScheme.surface,
                                            unfocusedContainerColor = colorScheme.surface,
                                            focusedBorderColor = colorScheme.primary,
                                            unfocusedBorderColor = colorScheme.outlineVariant
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Compact TVA Toggle Row inside same card
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "ضريبة القيمة المضافة (TVA):",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colorScheme.primary
                                            )
                                        }

                                        Switch(
                                            checked = isInvoiceTvaEnabled,
                                            onCheckedChange = viewModel::setInvoiceTvaEnabled,
                                            modifier = Modifier.testTag("checkout_tva_switch")
                                        )
                                    }

                                    AnimatedVisibility(visible = isInvoiceTvaEnabled) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            OutlinedTextField(
                                                value = tvaRateTextInput,
                                                onValueChange = { raw ->
                                                    tvaRateTextInput = raw
                                                    viewModel.setInvoiceTvaRate(raw.toDoubleOrNull() ?: 0.0)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                label = { Text("نسبة الضريبة TVA (%)") },
                                                trailingIcon = {
                                                    Text(
                                                        "%",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = colorScheme.primary
                                                    )
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = colorScheme.surface,
                                                    unfocusedContainerColor = colorScheme.surface
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(19, 9, 5, 0).forEach { rate ->
                                                    val isRateSelected = abs(invoiceTvaPercent - rate.toDouble()) < 0.01
                                                    Surface(
                                                        onClick = {
                                                            tvaRateTextInput = rate.toString()
                                                            viewModel.setInvoiceTvaRate(rate.toDouble())
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(34.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isRateSelected) colorScheme.primaryContainer else colorScheme.surface,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isRateSelected) colorScheme.primary else colorScheme.outlineVariant
                                                        )
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "$rate%",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                color = if (isRateSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 3. Cart Items Review Section (shown in "ALL" or "ITEMS" mode)
                        if (unifiedViewMode == "ALL" || unifiedViewMode == "ITEMS") {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مراجعة وتعديل أصناف السلة:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${cartItems.size} أصناف • $totalUnitsCount قطعة",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.primary
                                    )
                                }
                            }

                            itemsIndexed(cartItems, key = { _, item -> item.product.id }) { index, item ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    CheckoutReviewItemCard(
                                        index = index + 1,
                                        item = item,
                                        currency = currency,
                                        debtAccentColor = debtAccentColor,
                                        debtContainerColor = debtContainerColor,
                                        debtBorderColor = debtBorderColor,
                                        onRemove = { viewModel.removeCartItem(item.product.id) },
                                        onIncrementPiece = { viewModel.adjustCartItemUnits(item.product.id, 1) },
                                        onDecrementPiece = { viewModel.adjustCartItemUnits(item.product.id, -1) },
                                        onIncrementBox = {
                                            viewModel.adjustCartItemUnits(item.product.id, item.unitsPerCartonSafe)
                                        },
                                        onDecrementBox = {
                                            val nextQty = (item.quantity - item.unitsPerCartonSafe).coerceAtLeast(1)
                                            viewModel.updateCartItemQuantity(item.product.id, item.isCarton, nextQty)
                                        },
                                        onDiscountClick = { itemForDiscountDialog = item },
                                        onTaxClick = { itemForTaxDialog = item },
                                        onToggleDebt = { itemForDebtDialog = item },
                                        onEditPriceClick = { itemForPriceDialog = item },
                                        onProductNameClick = { itemForImagePreview = item }
                                    )

                                    if (index < cartItems.lastIndex) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            HorizontalDivider(
                                                modifier = Modifier.weight(1f),
                                                thickness = 1.2.dp,
                                                color = colorScheme.outlineVariant.copy(alpha = 0.7f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = colorScheme.surfaceContainerHighest,
                                                border = BorderStroke(1.2.dp, colorScheme.outlineVariant),
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = "سلعة ${index + 2} من ${cartItems.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                                )
                                            }
                                            HorizontalDivider(
                                                modifier = Modifier.weight(1f),
                                                thickness = 1.2.dp,
                                                color = colorScheme.outlineVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }

                            // Detailed Totals Card
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "المجموع الفرعي:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", subtotal)} $currency",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colorScheme.onSurface
                                            )
                                        }

                                        if (invoiceDiscountAmount > 0.0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "خصم الفاتورة:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colorScheme.error
                                                )
                                                Text(
                                                    text = "- ${String.format(Locale.US, "%.2f", invoiceDiscountAmount)} $currency",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.error
                                                )
                                            }
                                        }

                                        if (invoiceTvaAmount > 0.0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "ضريبة القيمة المضافة (TVA):",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colorScheme.primary
                                                )
                                                Text(
                                                    text = "+ ${String.format(Locale.US, "%.2f", invoiceTvaAmount)} $currency",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.primary
                                                )
                                            }
                                        }

                                        if (selectedCustomer != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isLoyaltyExpired = viewModel.isCustomerLoyaltyExpired(selectedCustomer)
                                                val remainingLoyaltyDays = viewModel.getCustomerLoyaltyRemainingDays(selectedCustomer)
                                                val hasRewardUnlocked = selectedCustomer!!.loyaltyPoints >= selectedCustomer!!.loyaltyGoal && !isLoyaltyExpired
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(
                                                            text = "🎁 نقاط الولاء: ${selectedCustomer!!.loyaltyPoints} / ${selectedCustomer!!.loyaltyGoal}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (hasRewardUnlocked) Color(0xFF10B981) else colorScheme.onSurfaceVariant
                                                        )
                                                        if (hasRewardUnlocked) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                            ) {
                                                                Text(
                                                                    text = "مكافأة جاهزة! 🎉",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF047857),
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        } else if (isLoyaltyExpired) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFFEE2E2)
                                                            ) {
                                                                Text(
                                                                    text = "منتهية الصلاحية ⏱️",
                                                                    fontSize = 8.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFFDC2626),
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "اكسب 1 نقطة لكل 100 د.ج مبيعات",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 9.sp,
                                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                        if (isLoyaltyExpired) {
                                                            Text(
                                                                text = "• تجاوزت شرط المدة ($loyaltyValidityDays يوم)",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                color = Color(0xFFDC2626),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        } else if (remainingLoyaltyDays != null) {
                                                            Text(
                                                                text = "• الصلاحية: متبقي $remainingLoyaltyDays يوم",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                color = if (remainingLoyaltyDays <= 15) Color(0xFFD97706) else colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                            )
                                                        }
                                                    }
                                                }

                                                if (hasRewardUnlocked) {
                                                    Button(
                                                        onClick = { viewModel.toggleLoyaltyDiscount() },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isLoyaltyDiscountApplied) Color(0xFF10B981) else colorScheme.outlineVariant,
                                                            contentColor = if (isLoyaltyDiscountApplied) Color.White else colorScheme.onSurfaceVariant
                                                        ),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isLoyaltyDiscountApplied) "تم تطبيق الخصم ✓" else "تطبيق المكافأة (-${selectedCustomer!!.loyaltyRewardValue} د.ج)",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                                        }

                                        if (isLoyaltyDiscountApplied && selectedCustomer != null && selectedCustomer!!.loyaltyPoints >= selectedCustomer!!.loyaltyGoal) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "خصم مكافأة ولاء العملاء:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF10B981)
                                                )
                                                Text(
                                                    text = "- ${String.format(Locale.US, "%.2f", selectedCustomer!!.loyaltyRewardValue)} $currency",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                        }

                                        if (totalDebtPortion > 0.0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "المبلغ المسجل كدين (آجل):",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = debtAccentColor
                                                )
                                                Text(
                                                    text = "${String.format(Locale.US, "%.2f", totalDebtPortion)} $currency",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = debtAccentColor
                                                )
                                            }
                                        }

                                        HorizontalDivider(
                                            color = colorScheme.outlineVariant,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "المجموع الصافي (المبلغ النهائي):",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", netTotal)} $currency",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }

                        // 4. Unified Live Paper Invoice Preview Section (shown in "ALL" or "PAPER" mode)
                        if (unifiedViewMode == "ALL" || unifiedViewMode == "PAPER") {
                            item {
                                val brandBlue = Color(0xFF0284C7)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Section Header + Template Style Selector + Header Edit
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = colorScheme.surfaceContainerLow,
                                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
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
                                                        Icons.Default.ReceiptLong,
                                                        contentDescription = null,
                                                        tint = brandBlue,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "معاينة الفاتورة الورقية الحية:",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = colorScheme.onSurface
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        onClick = { showHeaderEditorDialog = true },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = brandBlue.copy(alpha = 0.14f)
                                                    ) {
                                                        Text(
                                                            text = "تعديل الترويسة ✏️",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = brandBlue,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }

                                                    Surface(
                                                        onClick = {
                                                            exportAndShareInvoicePdf(
                                                                context = context,
                                                                style = selectedStyle,
                                                                storeName = storeName,
                                                                storePhone = storePhone,
                                                                customerName = customerDisplayName,
                                                                cashierName = cashierDisplayName,
                                                                shortInvoiceNumber = shortInvoiceNumber,
                                                                formattedDateTime = formattedDateTime,
                                                                paymentMethodAr = paymentMethodAr,
                                                                lines = previewLines,
                                                                totalPiecesCount = totalUnitsCount,
                                                                totalCartonsCount = totalCartonsCount,
                                                                sousTotal = rawSousTotal,
                                                                discountAmount = totalCombinedDiscount,
                                                                discountPercentInt = effectiveDiscountPercentInt,
                                                                tvaAmount = totalCombinedTva,
                                                                netTotal = netTotal,
                                                                hasDebtItems = hasDebtItems,
                                                                creditAmount = totalDebtPortion,
                                                                especesAmount = especesAmount,
                                                                currency = currency,
                                                                footerText = receiptFooterText,
                                                                developerContact = developerContact
                                                            )
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = colorScheme.primaryContainer
                                                    ) {
                                                        Text(
                                                            text = "PDF 📄",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            val availableStyles = listOf(
                                                InvoiceStyle.BON_DE_LIVRAISON to "📋 Bon de livraison",
                                                InvoiceStyle.THERMAL_DETAILED to "🧾 وصل التسليم",
                                                InvoiceStyle.THERMAL_POS to "🖨️ حراري POS",
                                                InvoiceStyle.SIMPLE to "📄 Bon de commande",
                                                InvoiceStyle.TEMPLATE_K to "✨ قالب K"
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                availableStyles.forEach { (style, label) ->
                                                    val isSelected = selectedStyle == style
                                                    Surface(
                                                        onClick = { viewModel.setInvoiceStyle(style) },
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (isSelected) brandBlue else colorScheme.surface,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isSelected) brandBlue else colorScheme.outlineVariant
                                                        ),
                                                        modifier = Modifier
                                                            .height(36.dp)
                                                            .testTag("unified_invoice_style_chip_${style.id}")
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.padding(horizontal = 12.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = label,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 12.sp,
                                                                color = if (isSelected) Color.White else colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Live Printable Receipt Paper Card
                                    BonDeLivraisonPaperCard(
                                        style = selectedStyle,
                                        storeName = storeName,
                                        storePhone = storePhone,
                                        customerName = customerDisplayName,
                                        cashierName = cashierDisplayName,
                                        shortInvoiceNumber = shortInvoiceNumber,
                                        formattedDateTime = formattedDateTime,
                                        paymentMethodAr = paymentMethodAr,
                                        lines = previewLines,
                                        totalPiecesCount = totalUnitsCount,
                                        totalCartonsCount = totalCartonsCount,
                                        sousTotal = rawSousTotal,
                                        discountAmount = totalCombinedDiscount,
                                        discountPercentInt = effectiveDiscountPercentInt,
                                        tvaAmount = totalCombinedTva,
                                        netTotal = netTotal,
                                        hasDebtItems = hasDebtItems,
                                        creditAmount = totalDebtPortion,
                                        especesAmount = especesAmount,
                                        currency = currency,
                                        footerText = receiptFooterText,
                                        developerContact = developerContact,
                                        previousDebt = selectedCustomer?.balanceDebt ?: 0.0,
                                        paperWidth = thermalPrinterConfig.paperWidth
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }

                    // Elevated Sticky Bottom Action Dock (Print, Save, Share, Preview)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colorScheme.surfaceContainer,
                        tonalElevation = 8.dp,
                        shadowElevation = 14.dp,
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            // Summary Strip inside Dock so Net Total is always visible
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceContainerHigh,
                                border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "المبلغ النهائي:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isAllCredit || hasAnyItemDebt) debtContainerColor else colorScheme.primaryContainer
                                        ) {
                                            val paymentLabel = when {
                                                isAllCredit -> "آجل بالكامل"
                                                paymentMethod == "TRANSFER" && hasAnyItemDebt -> "تحويل + دين"
                                                paymentMethod == "TRANSFER" -> "تحويل"
                                                hasAnyItemDebt -> "نقدي + دين"
                                                else -> "نقدي (كاش)"
                                            }
                                            Text(
                                                text = paymentLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAllCredit || hasAnyItemDebt) debtAccentColor else colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", netTotal)} $currency",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorScheme.primary
                                    )
                                }
                            }

                            // Bluetooth Thermal Printer Quick Strip (Paper Width + Mode + Copies)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceContainerLow,
                                border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.65f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Printer Status / Picker Trigger
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showBluetoothPrinterPicker = true }
                                            .testTag("checkout_bt_printer_status_btn")
                                    ) {
                                        val isBtConnected = thermalConnectionState is ThermalConnectionState.Connected
                                        Icon(
                                            imageVector = if (isBtConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = if (isBtConnected) Color(0xFF10B981) else colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = when (val st = thermalConnectionState) {
                                                is ThermalConnectionState.Connected -> st.deviceName
                                                is ThermalConnectionState.Connecting -> "جاري الاتصال..."
                                                else -> thermalPrinterConfig.lastPrinterName.ifBlank { "طابعة بلوتوث" }
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Paper Width Cycle Chip (58mm / 80mm / 104mm)
                                    Surface(
                                        onClick = {
                                            val nextWidth = when (thermalPrinterConfig.paperWidth) {
                                                ThermalPaperWidth.WIDTH_58MM -> ThermalPaperWidth.WIDTH_80MM
                                                ThermalPaperWidth.WIDTH_80MM -> ThermalPaperWidth.WIDTH_104MM
                                                ThermalPaperWidth.WIDTH_104MM -> ThermalPaperWidth.WIDTH_58MM
                                            }
                                            viewModel.updateThermalPrinterConfig(
                                                thermalPrinterConfig.copy(paperWidth = nextWidth)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .testTag("checkout_cycle_paper_width_chip")
                                    ) {
                                        Text(
                                            text = thermalPrinterConfig.paperWidth.shortLabelAr,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Copies Stepper Chip (1..5)
                                    Surface(
                                        onClick = {
                                            val nextC = if (selectedCopies >= 5) 1 else selectedCopies + 1
                                            selectedCopies = nextC
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = colorScheme.secondaryContainer,
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .testTag("checkout_cycle_copies_chip")
                                    ) {
                                        Text(
                                            text = "${selectedCopies} ${if (selectedCopies == 1) "نسخة" else "نسخ"}",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Instant vs Preview Toggle Chip
                                    val isInstant = thermalPrinterConfig.checkoutPrintMode == CheckoutThermalPrintMode.INSTANT_PRINT
                                    Surface(
                                        onClick = {
                                            val nextMode = if (isInstant) {
                                                CheckoutThermalPrintMode.PREVIEW_FIRST
                                            } else {
                                                CheckoutThermalPrintMode.INSTANT_PRINT
                                            }
                                            viewModel.updateThermalPrinterConfig(
                                                thermalPrinterConfig.copy(checkoutPrintMode = nextMode)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isInstant) Color(0xFF10B981).copy(alpha = 0.16f) else colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isInstant) Color(0xFF10B981) else colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .testTag("checkout_toggle_print_mode_chip")
                                    ) {
                                        Text(
                                            text = if (isInstant) "⚡ فورية" else "👁️ معاينة",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isInstant) Color(0xFF10B981) else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Keep only Preview Invoice Button
                            ModernGradientButton(
                                text = "معاينة الفاتورة",
                                onClick = {
                                    showTemplatePreviewDialog = true
                                },
                                icon = Icons.Default.Visibility,
                                gradientColors = listOf(
                                    colorScheme.primary,
                                    colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                height = 52.dp,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_preview_invoice_btn")
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Per-Item Discount Modal
    itemForDiscountDialog?.let { currentItem ->
        var isPct by remember(currentItem) { mutableStateOf(currentItem.isItemDiscountPercent) }
        var itemDiscountText by remember(currentItem) {
            mutableStateOf(
                if (currentItem.itemDiscountValue > 0.0) {
                    currentItem.itemDiscountValue.toString().removeSuffix(".0")
                } else ""
            )
        }
        AlertDialog(
            onDismissRequest = { itemForDiscountDialog = null },
            title = {
                Text(
                    text = "خصم خاص على السلعة",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = currentItem.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isPct,
                            onClick = { isPct = true },
                            label = { Text("% نسبة مئوية") }
                        )
                        FilterChip(
                            selected = !isPct,
                            onClick = { isPct = false },
                            label = { Text("$currency مبلغ ثابت") }
                        )
                    }
                    OutlinedTextField(
                        value = itemDiscountText,
                        onValueChange = { itemDiscountText = it },
                        label = { Text(if (isPct) "نسبة الخصم (%)" else "مبلغ الخصم ($currency)") },
                        placeholder = { Text("مثال: 5 أو 10") },
                        trailingIcon = { Text(if (isPct) "%" else currency, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isPct) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0, 5, 10, 15, 20).forEach { pct ->
                                FilterChip(
                                    selected = (itemDiscountText.toDoubleOrNull() ?: 0.0) == pct.toDouble(),
                                    onClick = { itemDiscountText = if (pct == 0) "0" else pct.toString() },
                                    label = { Text("$pct%") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = itemDiscountText.toDoubleOrNull() ?: 0.0
                        viewModel.updateCartItemDiscount(currentItem.product.id, isPct, value)
                        itemForDiscountDialog = null
                    }
                ) {
                    Text("تطبيق")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForDiscountDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Per-Item TVA Modal
    itemForTaxDialog?.let { currentItem ->
        var itemTaxEnabled by remember(currentItem) { mutableStateOf(currentItem.hasItemTva) }
        var itemTaxRateText by remember(currentItem) {
            mutableStateOf(currentItem.itemTvaPercent.toString().removeSuffix(".0"))
        }
        AlertDialog(
            onDismissRequest = { itemForTaxDialog = null },
            title = {
                Text(
                    text = "ضريبة القيمة المضافة (TVA) للسلعة",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = currentItem.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = itemTaxEnabled,
                            onCheckedChange = { itemTaxEnabled = it }
                        )
                        Text("تفعيل TVA على هذه السلعة", fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = itemTaxRateText,
                        onValueChange = {
                            itemTaxRateText = it
                            itemTaxEnabled = true
                        },
                        label = { Text("نسبة TVA (%)") },
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = if (itemTaxEnabled) (itemTaxRateText.toDoubleOrNull() ?: 19.0) else 0.0
                        viewModel.updateCartItemTvaPercent(currentItem.product.id, rate)
                        itemForTaxDialog = null
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForTaxDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Per-Item Selling Price Editor Modal (by Unit & by Carton/Package + Save for Specific Customer)
    itemForPriceDialog?.let { currentItem ->
        val unitsInPack = currentItem.unitsPerCartonSafe
        var unitPriceInput by remember(currentItem) {
            mutableStateOf(
                if (currentItem.customUnitPrice % 1.0 == 0.0) {
                    currentItem.customUnitPrice.toInt().toString()
                } else {
                    String.format(Locale.US, "%.2f", currentItem.customUnitPrice)
                }
            )
        }
        var cartonPriceInput by remember(currentItem) {
            val initialPackPrice = currentItem.customCartonPrice
            mutableStateOf(
                if (initialPackPrice % 1.0 == 0.0) {
                    initialPackPrice.toInt().toString()
                } else {
                    String.format(Locale.US, "%.2f", initialPackPrice)
                }
            )
        }
        var saveForSpecificCustomer by remember(currentItem) {
            mutableStateOf(currentItem.customPriceCustomerName != null)
        }
        var chosenCustomerForPrice by remember(currentItem, selectedCustomer, customers) {
            mutableStateOf(selectedCustomer ?: customers.firstOrNull())
        }
        var showInlineCustomerList by remember(currentItem) { mutableStateOf(false) }

        val existingSavedPriceForChosenCustomer = remember(chosenCustomerForPrice, customerCustomPrices, currentItem) {
            chosenCustomerForPrice?.let { cust ->
                customerCustomPrices["${cust.id}_${currentItem.product.id}"]
            }
        }

        val parsedUnitPrice = (unitPriceInput.toDoubleOrNull() ?: currentItem.customUnitPrice).coerceAtLeast(0.0)
        val parsedCartonPrice = (cartonPriceInput.toDoubleOrNull() ?: (parsedUnitPrice * unitsInPack)).coerceAtLeast(0.0)
        val previewNewLineTotal = parsedUnitPrice * currentItem.effectiveUnitsDeducted

        AlertDialog(
            onDismissRequest = { itemForPriceDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "سعة العبوة: $unitsInPack ق",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "تعديل سعر بيع السلعة",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Product Name & Original Catalog Price Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = currentItem.product.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = colorScheme.primary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "السعر الأساسي: ${String.format(Locale.US, "%.2f", currentItem.product.price)} $currency / قطعة • العبوة: ${String.format(Locale.US, "%.2f", currentItem.product.price * unitsInPack)} $currency",
                                fontSize = 11.5.sp,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 1. Unit Selling Price Input (سعر البيع بالوحدة / القطعة)
                    OutlinedTextField(
                        value = unitPriceInput,
                        onValueChange = { raw ->
                            unitPriceInput = raw
                            val newUnit = raw.toDoubleOrNull()
                            if (newUnit != null && newUnit >= 0.0) {
                                val calcPack = newUnit * unitsInPack
                                cartonPriceInput = if (calcPack % 1.0 == 0.0) {
                                    calcPack.toInt().toString()
                                } else {
                                    String.format(Locale.US, "%.2f", calcPack)
                                }
                            }
                        },
                        label = { Text("سعر البيع بالوحدة / القطعة ($currency)") },
                        placeholder = { Text("مثال: ${currentItem.product.price}") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Sell,
                                contentDescription = null,
                                tint = colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "$currency / ق",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_item_unit_price_input")
                    )

                    // 2. Package / Carton Selling Price Input (سعر البيع بالعبوة / الكرتونة)
                    OutlinedTextField(
                        value = cartonPriceInput,
                        onValueChange = { raw ->
                            cartonPriceInput = raw
                            val newPack = raw.toDoubleOrNull()
                            if (newPack != null && newPack >= 0.0) {
                                val calcUnit = newPack / unitsInPack.toDouble()
                                unitPriceInput = if (calcUnit % 1.0 == 0.0) {
                                    calcUnit.toInt().toString()
                                } else {
                                    String.format(Locale.US, "%.2f", calcUnit)
                                }
                            }
                        },
                        label = { Text("سعر البيع بالعبوة ($unitsInPack قطعة) ($currency)") },
                        placeholder = { Text("مثال: ${currentItem.product.price * unitsInPack}") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "$currency / عبوة",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_item_carton_price_input")
                    )

                    // Live Calculation Summary Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", parsedUnitPrice)} $currency",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = colorScheme.primary
                                )
                                Text(
                                    text = "سعر القطعة المعتمد:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", parsedCartonPrice)} $currency",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = colorScheme.primary
                                )
                                Text(
                                    text = "سعر العبوة ($unitsInPack ق):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            HorizontalDivider(
                                thickness = 0.7.dp,
                                color = colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", previewNewLineTotal)} $currency",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = colorScheme.primary
                                )
                                Text(
                                    text = "إجمالي السلعة (${currentItem.effectiveUnitsDeducted} ق):",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // 3. Save Price for a Specific Customer Section (حفظ السعر بزبون خاص)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (saveForSpecificCustomer) colorScheme.secondaryContainer.copy(alpha = 0.55f) else colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            1.1.dp,
                            if (saveForSpecificCustomer) colorScheme.primary else colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { saveForSpecificCustomer = !saveForSpecificCustomer }
                                    .testTag("toggle_save_price_for_customer"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = saveForSpecificCustomer,
                                    onCheckedChange = { saveForSpecificCustomer = it }
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "حفظ السعر بزبون خاص",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = colorScheme.onSurface
                                    )
                                    Icon(
                                        Icons.Default.PersonPin,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = saveForSpecificCustomer) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (customers.isEmpty()) {
                                        OutlinedButton(
                                            onClick = {
                                                itemForPriceDialog = null
                                                onOpenCustomerPicker()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إضافة زبون جديد أولاً", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        // Selected customer row for price saving
                                        Surface(
                                            onClick = { showInlineCustomerList = !showInlineCustomerList },
                                            shape = RoundedCornerShape(10.dp),
                                            color = colorScheme.surface,
                                            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("price_dialog_customer_selector")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (showInlineCustomerList) "إغلاق القائمة ▲" else "تغيير الزبون ▼",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.primary
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = chosenCustomerForPrice?.name ?: "اختر الزبون",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.sp,
                                                        color = colorScheme.onSurface
                                                    )
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (existingSavedPriceForChosenCustomer != null) {
                                            Text(
                                                text = "السعر المحفوظ سابقاً لـ ${chosenCustomerForPrice?.name}: ${String.format(Locale.US, "%.2f", existingSavedPriceForChosenCustomer)} $currency / ق",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.primary,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        // Horizontal/expandable quick customer chips so cashier can switch customer right here
                                        AnimatedVisibility(visible = showInlineCustomerList) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                customers.forEach { cust ->
                                                    val isCustSelected = chosenCustomerForPrice?.id == cust.id
                                                    val custSaved = customerCustomPrices["${cust.id}_${currentItem.product.id}"]
                                                    Surface(
                                                        onClick = {
                                                            chosenCustomerForPrice = cust
                                                            showInlineCustomerList = false
                                                            if (custSaved != null) {
                                                                unitPriceInput = if (custSaved % 1.0 == 0.0) {
                                                                    custSaved.toInt().toString()
                                                                } else {
                                                                    String.format(Locale.US, "%.2f", custSaved)
                                                                }
                                                                val packSaved = custSaved * unitsInPack
                                                                cartonPriceInput = if (packSaved % 1.0 == 0.0) {
                                                                    packSaved.toInt().toString()
                                                                } else {
                                                                    String.format(Locale.US, "%.2f", packSaved)
                                                                }
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isCustSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isCustSelected) colorScheme.primary else colorScheme.outlineVariant
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = if (custSaved != null) {
                                                                    "سعر محفوظ: ${String.format(Locale.US, "%.2f", custSaved)} $currency"
                                                                } else {
                                                                    "بدون سعر خاص"
                                                                },
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (custSaved != null) colorScheme.primary else colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = cust.name,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.5.sp,
                                                                color = if (isCustSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = "عند اختيار هذا الزبون مستقبلاً سيتغير سعر السلعة تلقائياً إلى هذا السعر.",
                                            fontSize = 11.sp,
                                            color = colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!saveForSpecificCustomer) {
                        Text(
                            text = "يطبق السعر الجديد على الفاتورة الحالية فقط (مؤقت) دون تغيير السعر الأساسي في المخزون.",
                            fontSize = 11.5.sp,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUnitPrice = unitPriceInput.toDoubleOrNull()
                            ?: ((cartonPriceInput.toDoubleOrNull() ?: currentItem.customCartonPrice) / unitsInPack.toDouble())
                        val targetCustomer = if (saveForSpecificCustomer) chosenCustomerForPrice else null
                        viewModel.updateCartItemSellingPrice(
                            productId = currentItem.product.id,
                            newUnitPrice = finalUnitPrice,
                            saveToCatalog = false,
                            forCustomer = targetCustomer
                        )
                        if (targetCustomer != null) {
                            Toast.makeText(
                                context,
                                "تم حفظ السعر الخاص للزبون (${targetCustomer.name}) وتطبيقه ✅",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        itemForPriceDialog = null
                    },
                    modifier = Modifier.testTag("confirm_edit_item_price_btn")
                ) {
                    Text(
                        text = if (saveForSpecificCustomer && chosenCustomerForPrice != null) {
                            "حفظ للزبون وتطبيق"
                        } else {
                            "تطبيق السعر"
                        },
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentItem.isCustomPriceModified || existingSavedPriceForChosenCustomer != null) {
                        TextButton(
                            onClick = {
                                viewModel.updateCartItemSellingPrice(
                                    productId = currentItem.product.id,
                                    newUnitPrice = currentItem.product.price,
                                    saveToCatalog = false,
                                    clearSavedCustomerPriceFor = chosenCustomerForPrice
                                )
                                itemForPriceDialog = null
                            }
                        ) {
                            Text("السعر الأصلي")
                        }
                    }
                    TextButton(onClick = { itemForPriceDialog = null }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }

    // Per-Item Debt Allocation Modal (تخصيص جزء من مبلغ السلعة كدين)
    itemForDebtDialog?.let { currentItem ->
        val lineTotal = currentItem.lineSubtotal
        var debtMode by remember(currentItem) {
            mutableStateOf(
                when {
                    currentItem.customDebtAmount != null && currentItem.customDebtAmount > 0.0 -> "PARTIAL"
                    currentItem.isItemDebt -> "FULL"
                    else -> "PARTIAL"
                }
            )
        }
        var debtAmountInput by remember(currentItem) {
            val initialAmount = currentItem.customDebtAmount
                ?: if (currentItem.isItemDebt) lineTotal else (lineTotal / 2.0)
            mutableStateOf(
                if (initialAmount % 1.0 == 0.0) {
                    initialAmount.toInt().toString()
                } else {
                    String.format(Locale.US, "%.2f", initialAmount)
                }
            )
        }

        AlertDialog(
            onDismissRequest = { itemForDebtDialog = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AssignmentLate,
                        contentDescription = null,
                        tint = debtAccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تخصيص دين السلعة",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item summary box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = currentItem.product.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إجمالي السلعة:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", lineTotal)} $currency",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.primary
                                )
                            }
                        }
                    }

                    // Mode Selection Chips
                    Text(
                        text = "طريقة تسجيل الدين للسلعة:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = debtMode == "NONE",
                            onClick = { debtMode = "NONE" },
                            label = { Text("نقداً بالكامل", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = debtMode == "PARTIAL",
                            onClick = { debtMode = "PARTIAL" },
                            label = { Text("دين جزئي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = debtMode == "FULL",
                            onClick = { debtMode = "FULL" },
                            label = { Text("دين كامل", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (debtMode == "PARTIAL") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = debtAmountInput,
                                onValueChange = { debtAmountInput = it },
                                label = { Text("مبلغ الدين المخصص للسلعة ($currency)") },
                                placeholder = { Text("أدخل مبلغ الدين") },
                                trailingIcon = { Text(currency, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick preset chips (25%, 50%, 75%)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%").forEach { (factor, label) ->
                                    val calcVal = lineTotal * factor
                                    val formattedVal = if (calcVal % 1.0 == 0.0) calcVal.toInt().toString() else String.format(Locale.US, "%.2f", calcVal)
                                    Surface(
                                        onClick = { debtAmountInput = formattedVal },
                                        shape = RoundedCornerShape(8.dp),
                                        color = colorScheme.surfaceContainerLow,
                                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$label (${if (calcVal % 1.0 == 0.0) calcVal.toInt().toString() else String.format(Locale.US, "%.0f", calcVal)})",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic breakdown
                            val parsedDebt = (debtAmountInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, lineTotal)
                            val remainingCash = (lineTotal - parsedDebt).coerceAtLeast(0.0)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = debtContainerColor,
                                border = BorderStroke(1.dp, debtBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("💳 المبلغ المسجل كدين:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = debtAccentColor)
                                        Text("${String.format(Locale.US, "%.2f", parsedDebt)} $currency", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = debtAccentColor)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("💵 المبلغ المدفوع نقداً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                        Text("${String.format(Locale.US, "%.2f", remainingCash)} $currency", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    } else if (debtMode == "FULL") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = debtContainerColor,
                            border = BorderStroke(1.dp, debtBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "سيتم تسجيل إجمالي مبلغ السلعة بالكامل (${String.format(Locale.US, "%.2f", lineTotal)} $currency) كدين على العميل.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = debtAccentColor,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "سيتم دفع إجمالي مبلغ السلعة (${String.format(Locale.US, "%.2f", lineTotal)} $currency) نقداً دون تسجيل أي دين.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (debtMode) {
                            "NONE" -> viewModel.setCartItemDebt(currentItem.product.id, isDebt = false, customAmount = null)
                            "FULL" -> viewModel.setCartItemDebt(currentItem.product.id, isDebt = true, customAmount = null)
                            "PARTIAL" -> {
                                val entered = debtAmountInput.toDoubleOrNull() ?: 0.0
                                viewModel.setCartItemDebt(currentItem.product.id, isDebt = true, customAmount = entered)
                            }
                        }
                        itemForDebtDialog = null
                    }
                ) {
                    Text("حفظ وتطبيق")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemForDebtDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showTemplatePreviewDialog) {
        InvoiceTemplatePreviewDialog(
            viewModel = viewModel,
            cartItems = cartItems,
            isPreSavePreview = true,
            onDismiss = { showTemplatePreviewDialog = false },
            onConfirmAndSave = {
                if ((isAllCredit || hasAnyItemDebt) && selectedCustomer == null) {
                    Toast.makeText(
                        context,
                        "يرجى اختيار العميل أولاً لتسجيل الدين",
                        Toast.LENGTH_SHORT
                    ).show()
                    showTemplatePreviewDialog = false
                    onOpenCustomerPicker()
                } else {
                    viewModel.completeCheckout {
                        Toast.makeText(
                            context,
                            "تم إتمام وحفظ الفاتورة بنجاح ✅",
                            Toast.LENGTH_SHORT
                        ).show()
                        showTemplatePreviewDialog = false
                        onDismiss()
                    }
                }
            },
            onConfirmAndPrint = {
                if ((isAllCredit || hasAnyItemDebt) && selectedCustomer == null) {
                    Toast.makeText(
                        context,
                        "يرجى اختيار العميل أولاً لتسجيل الدين",
                        Toast.LENGTH_SHORT
                    ).show()
                    showTemplatePreviewDialog = false
                    onOpenCustomerPicker()
                } else {
                    viewModel.completeCheckout { savedInvoice ->
                        if (thermalPrinterConfig.lastPrinterAddress.isNotBlank()) {
                            viewModel.printArchivedInvoiceViaBluetooth(
                                invoice = savedInvoice,
                                copiesOverride = selectedCopies
                            ) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "تم إتمام وحفظ الفاتورة بنجاح ✅",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showTemplatePreviewDialog = false
                        onDismiss()
                    }
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

    itemForImagePreview?.let { currentItem ->
        ProductFullScreenImageDialog(
            product = currentItem.product,
            currencySymbol = currency,
            onUpdateProductImage = { newPath ->
                val updatedProd = currentItem.product.copy(imagePath = newPath)
                viewModel.saveProduct(updatedProd)
                itemForImagePreview = currentItem.copy(product = updatedProd)
            },
            onDismiss = { itemForImagePreview = null }
        )
    }

    if (showCheckoutBarcodeScanner) {
        PosCameraBarcodeScannerDialog(
            viewModel = viewModel,
            onDismiss = { showCheckoutBarcodeScanner = false }
        )
    }
}

@Composable
private fun CheckoutReviewItemCard(
    index: Int,
    item: CartItem,
    currency: String,
    debtAccentColor: Color,
    debtContainerColor: Color,
    debtBorderColor: Color,
    onRemove: () -> Unit,
    onIncrementPiece: () -> Unit,
    onDecrementPiece: () -> Unit,
    onIncrementBox: () -> Unit,
    onDecrementBox: () -> Unit,
    onDiscountClick: () -> Unit,
    onTaxClick: () -> Unit,
    onToggleDebt: () -> Unit,
    onEditPriceClick: () -> Unit,
    onProductNameClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("checkout_item_card_${item.product.id}"),
        shape = RoundedCornerShape(18.dp),
        color = if (item.hasAnyDebt) debtContainerColor else colorScheme.surface,
        shadowElevation = 3.5.dp,
        border = BorderStroke(
            width = if (item.hasAnyDebt) 1.8.dp else 1.4.dp,
            color = if (item.hasAnyDebt) debtBorderColor else Color(0xFFCBD5E1)
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
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (item.hasAnyDebt) debtAccentColor else colorScheme.primary)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
            // Top Row: Product Name + Index Badge + Image (Right) | Line Total + Calculation + Delete Icon (Left)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Right side (Child 1 in RTL): Product Thumbnail + Name + Badge + Package Info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val prodImg = item.product.imageUrl.ifBlank { item.product.imagePath }
                    if (prodImg.isNotBlank()) {
                        AsyncImage(
                            model = prodImg,
                            contentDescription = item.product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorScheme.surfaceContainerHighest)
                                .clickable { onProductNameClick() }
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            color = colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🛒",
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 3.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "#$index",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            if (item.customDebtAmount != null && item.customDebtAmount > 0.0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = debtContainerColor,
                                    border = BorderStroke(1.dp, debtBorderColor)
                                ) {
                                    Text(
                                        text = "دين جزئي: ${String.format(Locale.US, "%.0f", item.customDebtAmount)} $currency",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = debtAccentColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (item.isItemDebt) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = debtContainerColor,
                                    border = BorderStroke(1.dp, debtBorderColor)
                                ) {
                                    Text(
                                        text = "دين كامل",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = debtAccentColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = item.product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onSurface,
                            maxLines = Int.MAX_VALUE,
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductNameClick() }
                                .testTag("checkout_item_name_${item.product.id}")
                        )

                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.2f", item.customUnitPrice)} $currency / قطعة • العبوة: ${String.format(Locale.US, "%.2f", item.customCartonPrice)} $currency",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isCustomPriceModified) colorScheme.primary else colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onEditPriceClick() }
                        )
                        if (item.customPriceCustomerName != null) {
                            Text(
                                text = "👤 سعر خاص للزبون: ${item.customPriceCustomerName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Left side (Child 2 in RTL): Line Total + Calculation Formula + Delete Icon
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", item.lineSubtotal)} $currency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.primary,
                            fontSize = 17.5.sp
                        )

                        Surface(
                            onClick = onRemove,
                            shape = RoundedCornerShape(10.dp),
                            color = colorScheme.errorContainer.copy(alpha = 0.65f),
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("checkout_delete_item_${item.product.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف السلعة",
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${String.format(Locale.US, "%.2f", item.customUnitPrice)} × ${item.effectiveUnitsDeducted} ق",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Equivalent Package Conversion Row (Aligned to Start / Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📦", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "يعادل: ${item.fullCartonsCount} عبوة + ${item.looseUnitsCount} فردي",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }

            if (item.customDebtAmount != null && item.customDebtAmount > 0.0) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = debtContainerColor,
                        border = BorderStroke(1.dp, debtBorderColor)
                    ) {
                        Text(
                            text = "💳 دين: ${String.format(Locale.US, "%.2f", item.customDebtAmount)} $currency • 💵 نقداً: ${String.format(Locale.US, "%.2f", item.remainingCashAmount)} $currency",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = debtAccentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Per-Item Action Chips: (تعديل السعر) | (+ دين) | (+ TVA) | (+ خصم)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Edit Unit & Carton Selling Price Chip (السعر بالوحدة والعبوة)
                Surface(
                    onClick = onEditPriceClick,
                    shape = RoundedCornerShape(10.dp),
                    color = if (item.isCustomPriceModified || item.customPriceCustomerName != null) {
                        colorScheme.primaryContainer
                    } else {
                        colorScheme.surface
                    },
                    border = BorderStroke(
                        1.1.dp,
                        if (item.isCustomPriceModified || item.customPriceCustomerName != null) {
                            colorScheme.primary
                        } else {
                            colorScheme.primary.copy(alpha = 0.6f)
                        }
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("checkout_edit_price_btn_${item.product.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "تعديل سعر البيع بالعبوة وبالوحدة",
                            tint = if (item.isCustomPriceModified || item.customPriceCustomerName != null) {
                                colorScheme.onPrimaryContainer
                            } else {
                                colorScheme.primary
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = when {
                                item.customPriceCustomerName != null -> "سعر خاص 👤"
                                item.isCustomPriceModified -> "سعر معدل"
                                else -> "تعديل السعر"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (item.isCustomPriceModified || item.customPriceCustomerName != null) {
                                colorScheme.onPrimaryContainer
                            } else {
                                colorScheme.primary
                            }
                        )
                    }
                }

                // 2. Debt / Credit Chip (+ دين)
                Surface(
                    onClick = onToggleDebt,
                    shape = RoundedCornerShape(10.dp),
                    color = if (item.hasAnyDebt) debtContainerColor else colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (item.hasAnyDebt) debtBorderColor else colorScheme.outlineVariant
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.AssignmentLate,
                            contentDescription = null,
                            tint = if (item.hasAnyDebt) debtAccentColor else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = when {
                                item.customDebtAmount != null && item.customDebtAmount > 0.0 ->
                                    "دين جزئي: ${String.format(Locale.US, "%.0f", item.customDebtAmount)}"
                                item.isItemDebt -> "دين كامل"
                                else -> "+ دين"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (item.hasAnyDebt) debtAccentColor else colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3. TVA Tax Chip (+ TVA)
                Surface(
                    onClick = onTaxClick,
                    shape = RoundedCornerShape(10.dp),
                    color = if (item.hasItemTva) colorScheme.primaryContainer else colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (item.hasItemTva) colorScheme.primary else colorScheme.outlineVariant
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (item.hasItemTva) colorScheme.onPrimaryContainer else colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (item.hasItemTva) {
                                "TVA ${item.itemTvaPercent.toInt()}%"
                            } else {
                                "TVA +"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (item.hasItemTva) colorScheme.onPrimaryContainer else colorScheme.primary
                        )
                    }
                }

                // 4. Discount Chip (+ خصم)
                val hasDiscount = item.hasItemDiscount && item.itemDiscountValue > 0.0
                Surface(
                    onClick = onDiscountClick,
                    shape = RoundedCornerShape(10.dp),
                    color = if (hasDiscount) colorScheme.primaryContainer else colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (hasDiscount) colorScheme.primary else colorScheme.outlineVariant
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = if (hasDiscount) colorScheme.onPrimaryContainer else colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (hasDiscount) {
                                if (item.isItemDiscountPercent) "خصم ${item.itemDiscountValue.toInt()}%"
                                else "خصم ${item.itemDiscountValue.toInt()}"
                            } else {
                                "+ خصم"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (hasDiscount) colorScheme.onPrimaryContainer else colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Internal Divider inside the card separating item details from quantity controls
            HorizontalDivider(
                thickness = 0.8.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Dual Quantity Stepper Bar with clear Arabic labels and RTL alignment
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right Side (Child 1 in RTL): Piece Steppers with label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "القطع:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = onDecrementPiece,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "إنقاص قطعة",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${item.effectiveUnitsDeducted} ق",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = colorScheme.onPrimaryContainer
                            )
                        }

                        IconButton(
                            onClick = onIncrementPiece,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "زيادة قطعة",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Left Side (Child 2 in RTL): Box Steppers with label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "العبوات:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )

                        Surface(
                            onClick = onDecrementBox,
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.secondaryContainer,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("📦", fontSize = 11.sp)
                                Text(
                                    text = "-1",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Surface(
                            onClick = onIncrementBox,
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.secondaryContainer,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("📦", fontSize = 11.sp)
                                Text(
                                    text = "+1",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSecondaryContainer
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

private fun buildInvoiceShareText(
    storeName: String,
    customerName: String,
    cart: List<CartItem>,
    subtotal: Double,
    discountAmount: Double,
    taxAmount: Double,
    netTotal: Double,
    debtAmount: Double,
    currency: String
): String {
    val sb = StringBuilder()
    sb.appendLine("🧾 فاتورة مبيعات - $storeName")
    sb.appendLine("👤 العميل: $customerName")
    sb.appendLine("────────────────────")
    cart.forEachIndexed { idx, item ->
        val creditTag = when {
            item.customDebtAmount != null && item.customDebtAmount > 0.0 ->
                " [دين جزئي: ${String.format(Locale.US, "%.2f", item.customDebtAmount)} $currency]"
            item.isItemDebt -> " [دين كامل]"
            else -> ""
        }
        sb.appendLine("${idx + 1}. ${item.product.name}$creditTag")
        sb.appendLine("   ${item.effectiveUnitsDeducted} × ${String.format(Locale.US, "%.2f", item.customUnitPrice)} = ${String.format(Locale.US, "%.2f", item.lineSubtotal)} $currency")
    }
    sb.appendLine("────────────────────")
    sb.appendLine("المجموع الفرعي: ${String.format(Locale.US, "%.2f", subtotal)} $currency")
    if (discountAmount > 0.0) {
        sb.appendLine("الخصم: -${String.format(Locale.US, "%.2f", discountAmount)} $currency")
    }
    if (taxAmount > 0.0) {
        sb.appendLine("ضريبة TVA: +${String.format(Locale.US, "%.2f", taxAmount)} $currency")
    }
    sb.appendLine("💰 المجموع الصافي: ${String.format(Locale.US, "%.2f", netTotal)} $currency")
    if (debtAmount > 0.0) {
        sb.appendLine("⚠️ المبلغ المسجل كدين: ${String.format(Locale.US, "%.2f", debtAmount)} $currency")
    }
    return sb.toString()
}
