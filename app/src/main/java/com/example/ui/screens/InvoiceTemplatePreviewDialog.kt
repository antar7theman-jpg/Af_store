package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.asImageBitmap
import com.example.utils.CustomerLocationHelper
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.example.data.models.ArchivedInvoice
import com.example.data.models.InvoiceStyle
import com.example.data.models.Product
import com.example.data.models.formatPackagingLabel
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.ui.viewmodels.CartItem
import com.example.utils.ThermalConnectionState
import com.example.utils.ThermalFontThickness
import com.example.utils.ThermalPaperWidth
import com.example.utils.ThermalReceiptBitmapRenderer
import com.example.utils.ThermalReceiptPrintPayload
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class PreviewLineItem(
    val name: String,
    val quantityPieces: Int,
    val unitPrice: Double,
    val rawSubtotal: Double,
    val subtotalAfterDiscount: Double,
    val itemDiscountAmount: Double,
    val itemDiscountPercent: Double,
    val itemTvaAmount: Double,
    val itemTvaPercent: Double,
    val finalLineTotal: Double,
    val unitsPerCarton: Int,
    val barcode: String,
    val isDebt: Boolean = false
) {
    val safeUnitsPerCarton: Int
        get() = unitsPerCarton.coerceAtLeast(1)

    val fullCartons: Int
        get() = quantityPieces / safeUnitsPerCarton

    val loosePieces: Int
        get() = quantityPieces % safeUnitsPerCarton

    val discountPercentFormatted: String
        get() = if (itemDiscountPercent % 1.0 == 0.0) {
            itemDiscountPercent.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", itemDiscountPercent)
        }

    val tvaPercentFormatted: String
        get() = if (itemTvaPercent % 1.0 == 0.0) {
            itemTvaPercent.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", itemTvaPercent)
        }
}

@Composable
fun InvoiceTemplatePreviewDialog(
    viewModel: AntarSalesViewModel,
    cartItems: List<CartItem> = emptyList(),
    archivedInvoice: ArchivedInvoice? = null,
    isPreSavePreview: Boolean = true,
    onDismiss: () -> Unit,
    onConfirmAndSave: () -> Unit = {},
    onConfirmAndPrint: () -> Unit = {}
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val storePhone by viewModel.storePhone.collectAsStateWithLifecycle()
    val storePhonesList by viewModel.storePhonesList.collectAsStateWithLifecycle()
    val storeLogoImagePath by viewModel.storeLogoImagePath.collectAsStateWithLifecycle()
    val storeLogoCustomText by viewModel.storeLogoCustomText.collectAsStateWithLifecycle()
    val receiptFooterText by viewModel.receiptFooterText.collectAsStateWithLifecycle()
    val developerContact by viewModel.developerContact.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val selectedStyle by viewModel.invoiceStyle.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val isInvoiceDiscountPercent by viewModel.isInvoiceDiscountPercent.collectAsStateWithLifecycle()
    val invoiceDiscountValue by viewModel.invoiceDiscountValue.collectAsStateWithLifecycle()
    val isInvoiceTvaEnabled by viewModel.isInvoiceTvaEnabled.collectAsStateWithLifecycle()
    val invoiceTvaPercent by viewModel.invoiceTvaPercent.collectAsStateWithLifecycle()
    val allProducts by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val thermalPrinterConfig by viewModel.thermalPrinterConfig.collectAsStateWithLifecycle()
    val thermalConnectionState by viewModel.thermalConnectionState.collectAsStateWithLifecycle()
    val isThermalPrinting by viewModel.isThermalPrinting.collectAsStateWithLifecycle()

    var showHeaderEditorDialog by remember { mutableStateOf(false) }
    var showBluetoothPrinterPicker by remember { mutableStateOf(false) }
    var pendingPrintAfterBluetoothSelect by remember { mutableStateOf(false) }
    var showInvoicePdfPreviewDialog by remember { mutableStateOf(false) }

    val nowMillis = remember(archivedInvoice) {
        archivedInvoice?.dateMillis ?: System.currentTimeMillis()
    }
    val shortInvoiceNumber = remember(archivedInvoice, nowMillis) {
        archivedInvoice?.invoiceId?.filter { it.isDigit() }?.takeLast(6)?.ifBlank { "602230" }
            ?: (nowMillis / 1000).toString().takeLast(6)
    }
    val formattedDateTime = remember(nowMillis) {
        SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.US).format(Date(nowMillis))
    }

    val customerDisplayName = remember(archivedInvoice, selectedCustomer) {
        archivedInvoice?.customerName?.ifBlank { "عميل نقدي" }
            ?: selectedCustomer?.name?.ifBlank { "عميل نقدي" }
            ?: "عميل نقدي"
    }
    val previousCustomerDebt = remember(selectedCustomer, customers, customerDisplayName) {
        selectedCustomer?.balanceDebt
            ?: customers.find { it.name.trim() == customerDisplayName.trim() }?.balanceDebt
            ?: 0.0
    }
    val invoiceCustomer = remember(selectedCustomer, customers, customerDisplayName, archivedInvoice) {
        selectedCustomer
            ?: archivedInvoice?.customerId?.let { cid -> customers.find { it.id == cid } }
            ?: customers.find { it.name.trim().equals(customerDisplayName.trim(), ignoreCase = true) }
    }
    val customerLocationUrl = remember(invoiceCustomer, archivedInvoice) {
        invoiceCustomer?.getEffectiveMapsUrl() ?: archivedInvoice?.getEffectiveCustomerMapsUrl()
    }
    val customerLocationQrBitmap = remember(customerLocationUrl) {
        customerLocationUrl?.let { CustomerLocationHelper.generateLocationQrBitmap(it, 320) }
    }
    val cashierDisplayName = remember(currentUser) {
        currentUser?.fullName?.takeIf { it.isNotBlank() } ?: currentUser?.username?.takeIf { !it.contains("@") } ?: "Antar"
    }

    // Build normalized preview items from either live cart or archived invoice
    val previewLines: List<PreviewLineItem> = remember(cartItems, archivedInvoice, allProducts, paymentMethod) {
        if (cartItems.isNotEmpty()) {
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
                    isDebt = wholeCartDebt || item.isItemDebt
                )
            }
        } else if (archivedInvoice != null) {
            archivedInvoice.items.mapIndexed { idx, sale ->
                val matchedProduct: Product? = allProducts.find { it.id == sale.productId }
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
                    barcode = matchedProduct?.barcode?.ifBlank { "622100100${idx + 1}" }
                        ?: "622100100${idx + 1}",
                    isDebt = sale.isDebt
                )
            }
        } else {
            emptyList()
        }
    }

    // Raw Subtotal (Sous-total) before item/invoice discounts & TVA
    val rawSousTotal = remember(previewLines) {
        previewLines.sumOf { it.rawSubtotal }
    }

    // Total discount = sum of per-item discounts + invoice-level discount
    val itemsDiscountTotal = remember(previewLines) {
        previewLines.sumOf { it.itemDiscountAmount }
    }
    val invoiceLevelDiscountAmount = remember(cartItems, isInvoiceDiscountPercent, invoiceDiscountValue) {
        if (cartItems.isNotEmpty()) viewModel.calculateInvoiceDiscountAmount() else 0.0
    }
    val totalDiscountAmount = itemsDiscountTotal + invoiceLevelDiscountAmount

    val effectiveDiscountPercentInt = remember(rawSousTotal, totalDiscountAmount, isInvoiceDiscountPercent, invoiceDiscountValue) {
        if (totalDiscountAmount <= 0.0 || rawSousTotal <= 0.0) {
            0
        } else if (itemsDiscountTotal <= 0.0 && isInvoiceDiscountPercent) {
            invoiceDiscountValue.roundToInt()
        } else {
            ((totalDiscountAmount / rawSousTotal) * 100.0).roundToInt().coerceAtLeast(1)
        }
    }

    // Total TVA = sum of per-item TVA + invoice-level TVA
    val itemsTvaTotal = remember(previewLines) {
        previewLines.sumOf { it.itemTvaAmount }
    }
    val invoiceLevelTvaAmount = remember(cartItems, isInvoiceTvaEnabled, invoiceTvaPercent, invoiceDiscountValue) {
        if (cartItems.isNotEmpty()) viewModel.calculateInvoiceTvaAmount() else 0.0
    }
    val totalTvaAmount = itemsTvaTotal + invoiceLevelTvaAmount

    // Final Net Total (Total Net) = rawSousTotal - totalDiscountAmount + totalTvaAmount
    val netTotal = remember(rawSousTotal, totalDiscountAmount, totalTvaAmount, archivedInvoice, cartItems) {
        if (cartItems.isNotEmpty()) {
            (rawSousTotal - totalDiscountAmount + totalTvaAmount).coerceAtLeast(0.0)
        } else {
            archivedInvoice?.subtotal ?: (rawSousTotal - totalDiscountAmount + totalTvaAmount).coerceAtLeast(0.0)
        }
    }

    // Determine Credit (debt) vs Espèces (cash) portions and payment label ("نقدا", "دين", "دين ونقد")
    val hasDebtItems = remember(previewLines) { previewLines.any { it.isDebt } }
    val hasCashItems = remember(previewLines) { previewLines.any { !it.isDebt } }

    val paymentMethodAr = remember(hasDebtItems, hasCashItems, paymentMethod) {
        when {
            hasDebtItems && hasCashItems && paymentMethod == "TRANSFER" -> "دين وتحويل"
            hasDebtItems && hasCashItems -> "دين ونقد"
            hasDebtItems || paymentMethod == "DEBT" -> "دين"
            paymentMethod == "TRANSFER" -> "تحويل"
            else -> "نقدا"
        }
    }

    val creditAmount = remember(previewLines, hasDebtItems, hasCashItems, netTotal) {
        when {
            !hasDebtItems -> 0.0
            !hasCashItems -> netTotal
            else -> {
                val linesSum = previewLines.sumOf { it.finalLineTotal }
                val scale = if (linesSum > 0.0) netTotal / linesSum else 1.0
                previewLines.filter { it.isDebt }.sumOf { it.finalLineTotal * scale }
            }
        }
    }
    val especesAmount = remember(netTotal, creditAmount) {
        (netTotal - creditAmount).coerceAtLeast(0.0)
    }

    val totalPiecesCount = remember(previewLines) { previewLines.sumOf { it.quantityPieces } }
    val totalCartonsCount = remember(previewLines) { previewLines.sumOf { it.fullCartons } }

    val currentThermalPayload = remember(
        storeName,
        storePhone,
        customerDisplayName,
        cashierDisplayName,
        shortInvoiceNumber,
        formattedDateTime,
        paymentMethodAr,
        previewLines,
        rawSousTotal,
        totalDiscountAmount,
        effectiveDiscountPercentInt,
        totalTvaAmount,
        netTotal,
        creditAmount,
        especesAmount,
        currency,
        receiptFooterText,
        developerContact,
        storeLogoImagePath,
        storeLogoCustomText
    ) {
        ThermalReceiptPrintPayload(
            storeName = storeName,
            storePhone = storePhone,
            customerName = customerDisplayName,
            cashierName = cashierDisplayName,
            invoiceNumber = shortInvoiceNumber,
            dateTimeFormatted = formattedDateTime,
            paymentMethodAr = paymentMethodAr,
            lines = previewLines,
            sousTotal = rawSousTotal,
            discountAmount = totalDiscountAmount,
            discountPercentInt = effectiveDiscountPercentInt,
            tvaAmount = totalTvaAmount,
            netTotal = netTotal,
            creditAmount = creditAmount,
            especesAmount = especesAmount,
            currency = currency,
            footerText = receiptFooterText,
            developerContact = developerContact,
            isTestReceipt = false,
            storeLogoImagePath = storeLogoImagePath,
            storeLogoCustomText = storeLogoCustomText,
            templateStyle = selectedStyle,
            previousDebt = previousCustomerDebt,
            fontThickness = thermalPrinterConfig.fontThickness
        )
    }

    val currentInvoicePdfPayload = remember(
        storeName,
        storePhone,
        customerDisplayName,
        cashierDisplayName,
        shortInvoiceNumber,
        formattedDateTime,
        paymentMethodAr,
        previewLines,
        rawSousTotal,
        totalDiscountAmount,
        effectiveDiscountPercentInt,
        totalTvaAmount,
        netTotal,
        hasDebtItems,
        creditAmount,
        especesAmount,
        currency,
        receiptFooterText,
        developerContact,
        storeLogoImagePath,
        storeLogoCustomText,
        previousCustomerDebt
    ) {
        InvoicePdfPayload(
            storeName = storeName,
            storePhone = storePhone,
            customerName = customerDisplayName,
            cashierName = cashierDisplayName,
            invoiceNumber = shortInvoiceNumber,
            formattedDateTime = formattedDateTime,
            paymentMethodAr = paymentMethodAr,
            lines = previewLines,
            sousTotal = rawSousTotal,
            discountAmount = totalDiscountAmount,
            discountPercentInt = effectiveDiscountPercentInt,
            tvaAmount = totalTvaAmount,
            netTotal = netTotal,
            hasDebtItems = hasDebtItems,
            creditAmount = creditAmount,
            especesAmount = especesAmount,
            currency = currency,
            footerText = receiptFooterText,
            developerContact = developerContact,
            storeLogoImagePath = storeLogoImagePath,
            storeLogoCustomText = storeLogoCustomText,
            previousDebt = previousCustomerDebt,
            customerLocationUrl = customerLocationUrl ?: ""
        )
    }

    val brandBlue = Color(0xFF0284C7)
    val lightBlueBg = Color(0xFFE0F2FE)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
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
                    .fillMaxHeight(0.93f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume click inside card */ }
                    ),
                shape = RoundedCornerShape(26.dp),
                color = colorScheme.surface,
                contentColor = colorScheme.onSurface,
                tonalElevation = 4.dp,
                shadowElevation = 20.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Top Modal Header (In RTL: 1st child is Right, 2nd child is Left)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right side (1st in RTL): Eye Circle Icon + Title & Subtitle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = lightBlueBg,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = brandBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "معاينة الفاتورة قبل الحفظ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "شاملة تفاصيل العبوات، الوحدات، والخصومات",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Left side (2nd in RTL): Share Icon Button + Close (X)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = {
                                    shareInvoiceAsText(
                                        context = context,
                                        storeName = storeName,
                                        storePhone = storePhone,
                                        customerName = customerDisplayName,
                                        cashierName = cashierDisplayName,
                                        invoiceNum = "#1-$shortInvoiceNumber",
                                        dateTime = formattedDateTime,
                                        paymentMethodAr = paymentMethodAr,
                                        lines = previewLines,
                                        sousTotal = rawSousTotal,
                                        discountAmount = totalDiscountAmount,
                                        discountPercentInt = effectiveDiscountPercentInt,
                                        tvaAmount = totalTvaAmount,
                                        netTotal = netTotal,
                                        creditAmount = creditAmount,
                                        especesAmount = especesAmount,
                                        currency = currency,
                                        footerText = receiptFooterText,
                                        developerContact = developerContact
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = lightBlueBg,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "مشاركة",
                                        tint = brandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("close_template_preview_btn")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Scrollable Body: Payment Method (if pre-save) + Template Selector + Header Editor Card + Paper Preview
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isPreSavePreview) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f))
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
                                            text = "طريقة الدفع النهائية:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "العميل: $customerDisplayName",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val methods = listOf(
                                            Triple("CASH", "نقدي (كاش)", Icons.Default.Payments),
                                            Triple("TRANSFER", "تحويل", Icons.Default.AccountBalanceWallet),
                                            Triple("DEBT", "آجل (دين)", Icons.Default.WarningAmber)
                                        )
                                        methods.forEach { (code, label, icon) ->
                                            val isSelected = paymentMethod == code
                                            val isDebtOpt = code == "DEBT"
                                            Surface(
                                                onClick = { viewModel.setPosPaymentMethod(code) },
                                                shape = RoundedCornerShape(12.dp),
                                                color = when {
                                                    isSelected && isDebtOpt -> Color(0xFFFEF3C7)
                                                    isSelected -> brandBlue
                                                    else -> colorScheme.surface
                                                },
                                                border = BorderStroke(
                                                    width = if (isSelected) 1.6.dp else 1.dp,
                                                    color = when {
                                                        isSelected && isDebtOpt -> Color(0xFFD97706)
                                                        isSelected -> brandBlue
                                                        else -> colorScheme.outlineVariant
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .testTag("preview_payment_method_$code")
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 6.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = when {
                                                            isSelected && isDebtOpt -> Color(0xFFB45309)
                                                            isSelected -> Color.White
                                                            isDebtOpt -> Color(0xFFD97706)
                                                            else -> colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = label,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1,
                                                        color = when {
                                                            isSelected && isDebtOpt -> Color(0xFF92400E)
                                                            isSelected -> Color.White
                                                            else -> colorScheme.onSurface
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Template Selector Card (تنسيق وشكل الفاتورة)
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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Right (1st in RTL)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = brandBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "تنسيق وشكل الفاتورة:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                    }

                                    // Left (2nd in RTL)
                                    Text(
                                        text = selectedStyle.displayNameAr,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = brandBlue
                                    )
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
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) brandBlue else colorScheme.surface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) brandBlue else colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .height(40.dp)
                                                .testTag("invoice_style_chip_${style.id}")
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 14.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) Color.White else colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Paper Width selector bar (58mm, 80mm, 104mm)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "عرض الورق (Paper Width):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val paperWidths = listOf(
                                            ThermalPaperWidth.WIDTH_58MM to "58 مم",
                                            ThermalPaperWidth.WIDTH_80MM to "80 مم",
                                            ThermalPaperWidth.WIDTH_104MM to "104 مم"
                                        )
                                        paperWidths.forEach { (pWidth, label) ->
                                            val isCurrent = thermalPrinterConfig.paperWidth == pWidth
                                            Surface(
                                                onClick = {
                                                    viewModel.updateThermalPrinterConfig(
                                                        thermalPrinterConfig.copy(paperWidth = pWidth)
                                                    )
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isCurrent) brandBlue else colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .height(30.dp)
                                                    .testTag("preview_paper_width_chip_${pWidth.widthMm}")
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.sp,
                                                        color = if (isCurrent) Color.White else colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                 Spacer(modifier = Modifier.height(10.dp))

                                 // Font Thickness selector bar (Normal, Medium, Extra Bold)
                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.SpaceBetween,
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text(
                                         text = "سمك خط الطباعة:",
                                         fontSize = 11.5.sp,
                                         fontWeight = FontWeight.Bold,
                                         color = colorScheme.onSurface
                                     )
                                     Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                         val thicknesses = listOf(
                                             ThermalFontThickness.NORMAL to "عادي",
                                             ThermalFontThickness.MEDIUM to "متوسط",
                                             ThermalFontThickness.EXTRA_BOLD to "عريض داكن"
                                         )
                                         thicknesses.forEach { (thick, label) ->
                                             val isSelected = thermalPrinterConfig.fontThickness == thick
                                             Surface(
                                                 onClick = {
                                                     viewModel.updateThermalPrinterConfig(
                                                         thermalPrinterConfig.copy(fontThickness = thick)
                                                     )
                                                 },
                                                 shape = RoundedCornerShape(8.dp),
                                                 color = if (isSelected) brandBlue else colorScheme.surfaceVariant,
                                                 modifier = Modifier
                                                     .height(30.dp)
                                                     .testTag("preview_font_thickness_chip_${thick.id}")
                                             ) {
                                                 Box(
                                                     modifier = Modifier.padding(horizontal = 8.dp),
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     Text(
                                                         text = label,
                                                         fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                                         fontSize = 11.sp,
                                                         color = if (isSelected) Color.White else colorScheme.onSurfaceVariant
                                                     )
                                                 }
                                             }
                                         }
                                     }
                                 }
                                }
                            }
                        }

                        // 3. Header & Store Info Card (ترويسة وبيانات الفاتورة)
                        Surface(
                            onClick = { showHeaderEditorDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_invoice_header_card"),
                            shape = RoundedCornerShape(16.dp),
                            color = lightBlueBg.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, brandBlue.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Right (1st in RTL): Storefront Circle + Title & Store Info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = brandBlue,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = "ترويسة وبيانات الفاتورة",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$storeName • ${if (storePhone.isBlank()) "بدون هاتف" else storePhone}",
                                            fontSize = 11.5.sp,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Middle Badge: تعديل قبل الطباعة
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = lightBlueBg
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "تعديل قبل",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = brandBlue
                                        )
                                        Text(
                                            text = "الطباعة ✏️",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = brandBlue
                                        )
                                    }
                                }

                                // Left (3rd in RTL): Edit Header Action
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = brandBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "تعديل الترويسة",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        color = brandBlue
                                    )
                                }
                            }
                        }

                        // 4. The Printable Receipt Paper Card (Exact Match to Screenshot: Name on Right, Price on Left)
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
                            totalPiecesCount = totalPiecesCount,
                            totalCartonsCount = totalCartonsCount,
                            sousTotal = rawSousTotal,
                            discountAmount = totalDiscountAmount,
                            discountPercentInt = effectiveDiscountPercentInt,
                            tvaAmount = totalTvaAmount,
                            netTotal = netTotal,
                            hasDebtItems = hasDebtItems,
                            creditAmount = creditAmount,
                            especesAmount = especesAmount,
                            currency = currency,
                            footerText = receiptFooterText,
                            developerContact = developerContact,
                            storeLogoImagePath = storeLogoImagePath,
                            storeLogoCustomText = storeLogoCustomText,
                            previousDebt = previousCustomerDebt,
                            paperWidth = thermalPrinterConfig.paperWidth,
                            customerLocationQrBitmap = customerLocationQrBitmap,
                            customerLocationUrl = customerLocationUrl
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // 5. Sticky Bottom 2x2 Action Buttons Bar (Raised above gesture bar for easy tapping)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colorScheme.surfaceContainer,
                        tonalElevation = 10.dp,
                        shadowElevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.75f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Row: طبع الفاتورة (Right) | إتمام وحفظ (Left)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Right (1st in RTL): Print Invoice (Direct Bluetooth ESC/POS)
                                Button(
                                    onClick = {
                                        if (thermalPrinterConfig.lastPrinterAddress.isBlank()) {
                                            pendingPrintAfterBluetoothSelect = true
                                            showBluetoothPrinterPicker = true
                                            Toast.makeText(
                                                context,
                                                "يرجى اختيار طابعة البلوتوث للطباعة المباشرة",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else if (isPreSavePreview) {
                                            onConfirmAndPrint()
                                        } else {
                                            viewModel.printThermalReceiptPayload(
                                                payload = currentThermalPayload,
                                                copiesOverride = thermalPrinterConfig.defaultCopies
                                            ) { ok, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                if (ok) {
                                                    onDismiss()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isThermalPrinting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("preview_print_invoice_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = brandBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isThermalPrinting) {
                                            "جاري الطباعة..."
                                        } else {
                                            "طبع الفاتورة"
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }

                                // Left (2nd in RTL): Complete & Save
                                Button(
                                    onClick = {
                                        if (isPreSavePreview) {
                                            onConfirmAndSave()
                                        } else {
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("preview_complete_and_save_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFBAE6FD),
                                        contentColor = Color(0xFF0F172A)
                                    )
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "إتمام وحفظ",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Bottom Row: تعديل السلة (Right) | مشاركة PDF (Left)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Right (1st in RTL): Edit Cart
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("preview_edit_cart_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colorScheme.surface
                                    ),
                                    border = BorderStroke(1.3.dp, brandBlue.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = brandBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تعديل السلة",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = brandBlue
                                    )
                                }

                                 // Left (2nd in RTL): Share PDF
                                OutlinedButton(
                                    onClick = {
                                        showInvoicePdfPreviewDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("preview_share_pdf_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colorScheme.surface
                                    ),
                                    border = BorderStroke(1.3.dp, brandBlue.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = brandBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "مشاركة PDF",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = brandBlue
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("📄", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Header Customization Dialog (تعديل الترويسة)
    if (showHeaderEditorDialog) {
        val headerScope = rememberCoroutineScope()
        var editStoreName by remember(storeName) { mutableStateOf(storeName) }
        var editPrimaryPhone by remember(storePhonesList, storePhone) {
            mutableStateOf(storePhonesList.firstOrNull() ?: storePhone)
        }
        var editExtraPhones by remember(storePhonesList) {
            mutableStateOf(if (storePhonesList.size > 1) storePhonesList.drop(1) else emptyList())
        }
        var editLogoImagePath by remember(storeLogoImagePath) { mutableStateOf(storeLogoImagePath) }
        var editLogoCustomText by remember(storeLogoCustomText) { mutableStateOf(storeLogoCustomText) }
        var editFooter by remember(receiptFooterText) { mutableStateOf(receiptFooterText) }
        var editDeveloper by remember(developerContact) { mutableStateOf(developerContact) }

        val headerLogoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                headerScope.launch {
                    val savedPath = ProductImageStorageHelper.saveImageFromUri(context, uri)
                    if (!savedPath.isNullOrBlank()) {
                        editLogoImagePath = savedPath
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showHeaderEditorDialog = false },
            title = {
                Text(
                    text = "تعديل ترويسة وبيانات الفاتورة",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editStoreName,
                        onValueChange = { editStoreName = it },
                        label = { Text("اسم المتجر (في أعلى الفاتورة)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPrimaryPhone,
                        onValueChange = { editPrimaryPhone = it },
                        label = { Text("رقم هاتف المتجر الأساسي") },
                        placeholder = { Text("مثال: 0550123456") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    editExtraPhones.forEachIndexed { idx, extraPhone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = extraPhone,
                                onValueChange = { updated ->
                                    editExtraPhones = editExtraPhones.toMutableList().also { it[idx] = updated }
                                },
                                label = { Text("رقم هاتف إضافي ${idx + 2}") },
                                placeholder = { Text("مثال: 0661987654") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    editExtraPhones = editExtraPhones.toMutableList().also { it.removeAt(idx) }
                                }
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "حذف الرقم",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { editExtraPhones = editExtraPhones + "" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة رقم هاتف آخر", fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider()
                    Text("شعار الفاتورة (Logo):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                headerLogoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (editLogoImagePath.isNotBlank()) "تغيير صورة الشعار" else "اختيار صورة Logo")
                        }
                        if (editLogoImagePath.isNotBlank() || editLogoCustomText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    editLogoImagePath = ""
                                    editLogoCustomText = ""
                                }
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "إزالة الشعار",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editLogoCustomText,
                        onValueChange = { editLogoCustomText = it },
                        label = { Text("أو شعار نصي مخصص (مثال: AF MARKET)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()
                    OutlinedTextField(
                        value = editFooter,
                        onValueChange = { editFooter = it },
                        label = { Text("عبارة الشكر في أسفل الفاتورة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDeveloper,
                        onValueChange = { editDeveloper = it },
                        label = { Text("رقم التواصل / المطور") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val allPhones = listOf(editPrimaryPhone) + editExtraPhones
                        viewModel.updateReceiptHeaderSettings(
                            newStoreName = editStoreName,
                            newStorePhone = allPhones.joinToString(" | "),
                            newFooterText = editFooter,
                            newDeveloperContact = editDeveloper,
                            newStorePhonesList = allPhones,
                            newLogoImagePath = editLogoImagePath,
                            newLogoCustomText = editLogoCustomText
                        )
                        showHeaderEditorDialog = false
                    }
                ) {
                    Text("حفظ الترويسة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHeaderEditorDialog = false }) {
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
                        if (pendingPrintAfterBluetoothSelect) {
                            pendingPrintAfterBluetoothSelect = false
                            if (isPreSavePreview) {
                                onConfirmAndPrint()
                            } else {
                                viewModel.printThermalReceiptPayload(
                                    payload = currentThermalPayload,
                                    copiesOverride = thermalPrinterConfig.defaultCopies
                                ) { printedOk, printMsg ->
                                    Toast.makeText(context, printMsg, Toast.LENGTH_LONG).show()
                                    if (printedOk) {
                                        onDismiss()
                                    }
                                }
                            }
                        }
                    }
                }
            },
            onDisconnect = { viewModel.disconnectBluetoothPrinter() },
            onDismiss = {
                pendingPrintAfterBluetoothSelect = false
                showBluetoothPrinterPicker = false
            }
        )
    }

    if (showInvoicePdfPreviewDialog) {
        InvoicePdfPreviewDialog(
            payload = currentInvoicePdfPayload,
            onDismiss = { showInvoicePdfPreviewDialog = false }
        )
    }
}

@Composable
internal fun BonDeLivraisonPaperCard(
    style: InvoiceStyle,
    storeName: String,
    storePhone: String,
    customerName: String,
    cashierName: String,
    shortInvoiceNumber: String,
    formattedDateTime: String,
    paymentMethodAr: String,
    lines: List<PreviewLineItem>,
    totalPiecesCount: Int,
    totalCartonsCount: Int,
    sousTotal: Double,
    discountAmount: Double,
    discountPercentInt: Int,
    tvaAmount: Double,
    netTotal: Double,
    hasDebtItems: Boolean,
    creditAmount: Double,
    especesAmount: Double,
    currency: String,
    footerText: String,
    developerContact: String,
    storeLogoImagePath: String = "",
    storeLogoCustomText: String = "",
    previousDebt: Double = 0.0,
    paperWidth: ThermalPaperWidth = ThermalPaperWidth.WIDTH_80MM,
    customerLocationQrBitmap: android.graphics.Bitmap? = null,
    customerLocationUrl: String? = null
) {
    val inkBlack = Color(0xFF0F172A)
    val inkMuted = Color(0xFF334155)
    val borderGray = Color(0xFF94A3B8)

    // Note: App runs with LocalLayoutDirection = Rtl, so:
    // - Alignment.Start = Physical Right (اليمين)
    // - Alignment.End = Physical Left (اليسار)
    // - In a Row: 1st child is on Physical Right (اليمين), 2nd child is on Physical Left (اليسار)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bon_de_livraison_paper_card"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        contentColor = inkBlack,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        if (style == InvoiceStyle.TEMPLATE_K) {
            TemplateKReceiptContent(
                storeName = storeName,
                storePhone = storePhone,
                customerName = customerName,
                cashierName = cashierName,
                shortInvoiceNumber = shortInvoiceNumber,
                formattedDateTime = formattedDateTime,
                paymentMethodAr = paymentMethodAr,
                lines = lines,
                totalPiecesCount = totalPiecesCount,
                totalCartonsCount = totalCartonsCount,
                sousTotal = sousTotal,
                discountAmount = discountAmount,
                discountPercentInt = discountPercentInt,
                tvaAmount = tvaAmount,
                netTotal = netTotal,
                hasDebtItems = hasDebtItems,
                creditAmount = creditAmount,
                especesAmount = especesAmount,
                previousDebt = previousDebt,
                currency = currency,
                footerText = footerText,
                developerContact = developerContact,
                paperWidth = paperWidth,
                inkBlack = inkBlack,
                inkMuted = inkMuted,
                storeLogoImagePath = storeLogoImagePath,
                storeLogoCustomText = storeLogoCustomText,
                customerLocationQrBitmap = customerLocationQrBitmap
            )
            return@Surface
        }

        if (style == InvoiceStyle.THERMAL_POS || style == InvoiceStyle.SIMPLE) {
            ThermalPosTabularReceiptContent(
                isRtlArabicColumns = (style == InvoiceStyle.SIMPLE),
                storeName = storeName,
                storePhone = storePhone,
                customerName = customerName,
                cashierName = cashierName,
                shortInvoiceNumber = shortInvoiceNumber,
                formattedDateTime = formattedDateTime,
                paymentMethodAr = paymentMethodAr,
                lines = lines,
                totalPiecesCount = totalPiecesCount,
                totalCartonsCount = totalCartonsCount,
                sousTotal = sousTotal,
                discountAmount = discountAmount,
                discountPercentInt = discountPercentInt,
                tvaAmount = tvaAmount,
                netTotal = netTotal,
                hasDebtItems = hasDebtItems,
                creditAmount = creditAmount,
                especesAmount = especesAmount,
                currency = currency,
                footerText = footerText,
                developerContact = developerContact,
                inkBlack = inkBlack,
                inkMuted = inkMuted,
                storeLogoImagePath = storeLogoImagePath,
                storeLogoCustomText = storeLogoCustomText,
                customerLocationQrBitmap = customerLocationQrBitmap
            )
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom Store Logo (Image or Text Crest) if configured
            InvoiceStoreLogoHeader(
                storeLogoImagePath = storeLogoImagePath,
                storeLogoCustomText = storeLogoCustomText
            )

            // Store Name
            Text(
                text = storeName,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = inkBlack,
                textAlign = TextAlign.Center
            )

            if (storePhone.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tel: $storePhone",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = inkMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Outlined Customer Card with Large Location QR: الزبون: عميل نقدي
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.2.dp, inkBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "الزبون: $customerName",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = inkBlack,
                        textAlign = TextAlign.Center
                    )
                    if (customerLocationQrBitmap != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.2.dp, Color(0xFF94A3B8)), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Image(
                                    bitmap = customerLocationQrBitmap.asImageBitmap(),
                                    contentDescription = "موقع العميل",
                                    modifier = Modifier
                                        .size(92.dp)
                                        .padding(4.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📍", fontSize = 14.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "موقع العميل (Google Maps)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = inkBlack
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "امسح بكاميرا الهاتف للوصول للعنوان فوراً والتوجيه المباشر",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cashier / Employé(e) Row — Aligned to Right (Alignment.Start in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Employé(e) / الكاشير: $cashierName",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = inkBlack
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ReceiptDashedDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Date (Right / 1st in RTL) & Invoice Number (Left / 2nd in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1st in RTL -> Physical Right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "📅", fontSize = 14.sp)
                    Text(
                        text = formattedDateTime,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = inkBlack
                    )
                }

                // 2nd in RTL -> Physical Left
                Text(
                    text = "#1-$shortInvoiceNumber",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = inkBlack
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Payment Method Row: طريقة الدفع: (Right / 1st) | نقدا / دين ونقد (Left / 2nd)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "طريقة الدفع:",
                    fontSize = 13.sp,
                    color = inkMuted
                )
                Text(
                    text = paymentMethodAr,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = inkBlack
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ReceiptSectionSolidDivider(color = inkBlack)
            Spacer(modifier = Modifier.height(14.dp))

            // Items Section:
            // - For BON_DE_LIVRAISON: Product Name + details on Physical Left (Alignment.End in RTL), Price on Physical Right (1st in Row in RTL)
            // - For وصل التسليم (THERMAL_DETAILED) & others: Product Name + details on Physical Right (Alignment.Start in RTL), Price on Physical Left (2nd in Row in RTL)
            val isBonDeLivraisonLeft = style == InvoiceStyle.BON_DE_LIVRAISON
            val itemHorizontalAlignment = if (isBonDeLivraisonLeft) Alignment.End else Alignment.Start
            val itemTextAlign = if (isBonDeLivraisonLeft) TextAlign.End else TextAlign.Start

            lines.forEachIndexed { idx, line ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = itemHorizontalAlignment
                ) {
                    val productNameAnnotated = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = inkBlack
                            )
                        ) {
                            append(line.name)
                        }
                        if (line.isDebt) {
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = inkBlack
                                )
                            ) {
                                append("(دين)")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (isBonDeLivraisonLeft) {
                            // BON_DE_LIVRAISON: 1st in RTL (Physical Right) = Price | 2nd in RTL (Physical Left) = Product Name
                            Text(
                                text = "${String.format(Locale.US, "%.2f", line.subtotalAfterDiscount)} $currency",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = inkBlack
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = productNameAnnotated,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            // وصل التسليم & others: 1st in RTL (Physical Right) = Product Name | 2nd in RTL (Physical Left) = Price
                            Text(
                                text = productNameAnnotated,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "${String.format(Locale.US, "%.2f", line.subtotalAfterDiscount)} $currency",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = inkBlack
                            )
                        }
                    }

                    // Detailed Package, Barcode, Per-Item Discount, and Per-Item TVA Breakdown
                    if (style != InvoiceStyle.THERMAL_POS) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // Boxed Badge: سعة العبوة: 10 قطعة
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, inkBlack)
                        ) {
                            Text(
                                text = "سعة العبوة: ${line.safeUnitsPerCarton} قطعة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkBlack,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Carton / Piece Formula Line: 1 كرتونة [سعة 10 ق] × 32.50 د.ج
                        val qtyDescription = when {
                            line.fullCartons > 0 && line.loosePieces == 0 ->
                                "${line.fullCartons} كرتونة [سعة ${line.safeUnitsPerCarton} ق]"
                            line.fullCartons > 0 && line.loosePieces > 0 ->
                                "${line.fullCartons} كرتونة + ${line.loosePieces} ق [سعة ${line.safeUnitsPerCarton} ق]"
                            else ->
                                "${line.quantityPieces} قطعة [سعة ${line.safeUnitsPerCarton} ق]"
                        }

                        if (isBonDeLivraisonLeft) {
                            // BON_DE_LIVRAISON: Calculation flows from Left to Right, with each number bound directly in front of its unit word
                            val rtlUnitStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = inkBlack,
                                textDirection = TextDirection.Rtl
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        if (line.fullCartons > 0 && line.loosePieces == 0) {
                                            Text(
                                                text = "\u200F${line.fullCartons} كرتونة",
                                                style = rtlUnitStyle
                                            )
                                        } else if (line.fullCartons > 0 && line.loosePieces > 0) {
                                            Text(
                                                text = "\u200F${line.fullCartons} كرتونة",
                                                style = rtlUnitStyle
                                            )
                                            Text(
                                                text = "+",
                                                style = rtlUnitStyle
                                            )
                                            Text(
                                                text = "\u200F${line.loosePieces} ق",
                                                style = rtlUnitStyle
                                            )
                                        } else {
                                            Text(
                                                text = "\u200F${line.quantityPieces} قطعة",
                                                style = rtlUnitStyle
                                            )
                                        }

                                        Text(
                                            text = "\u200F[سعة ${line.safeUnitsPerCarton} ق]",
                                            style = rtlUnitStyle
                                        )
                                        Text(
                                            text = "×",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = inkBlack
                                        )
                                        Text(
                                            text = "\u200F${String.format(Locale.US, "%.2f", line.unitPrice)} $currency",
                                            style = rtlUnitStyle
                                        )
                                    }
                                }
                            }
                        } else {
                            // وصل التسليم: remains untouched (Right to Left)
                            Text(
                                text = "$qtyDescription   ×   ${String.format(Locale.US, "%.2f", line.unitPrice)} $currency",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = inkBlack,
                                textAlign = itemTextAlign
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Product Barcode Line
                        Text(
                            text = line.barcode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            color = inkMuted,
                            textAlign = itemTextAlign
                        )

                        // Per-Item Discount Line: Remise / خصم: -19.20 د.ج (10%)
                        if (line.itemDiscountAmount > 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Remise / خصم: -${String.format(Locale.US, "%.2f", line.itemDiscountAmount)} $currency (${line.discountPercentFormatted}%)",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = inkBlack,
                                textAlign = itemTextAlign
                            )
                        }

                        // Per-Item TVA Line: TVA / ضريبة: +22.80 د.ج (5%)
                        if (line.itemTvaAmount > 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TVA / ضريبة: +${String.format(Locale.US, "%.2f", line.itemTvaAmount)} $currency (${line.tvaPercentFormatted}%)",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = inkMuted,
                                textAlign = itemTextAlign
                            )
                        }
                    } else {
                        // Compact Thermal POS mode (Right-aligned)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${line.quantityPieces} × ${String.format(Locale.US, "%.2f", line.unitPrice)} $currency",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            color = inkMuted,
                            textAlign = TextAlign.Start
                        )
                        if (line.itemDiscountAmount > 0.0) {
                            Text(
                                text = "خصم (${line.discountPercentFormatted}%): -${String.format(Locale.US, "%.2f", line.itemDiscountAmount)} $currency",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                color = inkBlack,
                                textAlign = TextAlign.Start
                            )
                        }
                        if (line.itemTvaAmount > 0.0) {
                            Text(
                                text = "TVA (${line.tvaPercentFormatted}%): +${String.format(Locale.US, "%.2f", line.itemTvaAmount)} $currency",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                color = inkMuted,
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    if (idx < lines.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ReceiptSmallDashedItemDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            ReceiptSectionSolidDivider(color = inkBlack)
            Spacer(modifier = Modifier.height(12.dp))

            // Total Quantity Sold Summary Row: Right (1st in RTL) = Label | Left (2nd in RTL) = Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجمالي الكمية المباعة:",
                    fontSize = 12.5.sp,
                    color = inkMuted
                )
                Text(
                    text = "\u200F${lines.size} أصناف ($totalPiecesCount قطعة) [\u200F$totalCartonsCount كرتونة\u200F]",
                    style = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = inkBlack,
                        textDirection = TextDirection.Rtl
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Outlined Subtotal Box: Right (1st in RTL) = المجموع الفرعي | Left (2nd in RTL) = 1753.00 د.ج
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderGray)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المجموع الفرعي (Sous-total):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = inkBlack
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", sousTotal)} $currency",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = inkBlack
                    )
                }
            }

            // Applied Discount Row: Right (1st in RTL) = الخصم المطبق (1%) : | Left (2nd in RTL) = -19.20 د.ج
            if (discountAmount > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الخصم المطبق ($discountPercentInt%) :",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.5.sp,
                        color = inkBlack
                    )
                    Text(
                        text = "-${String.format(Locale.US, "%.2f", discountAmount)} $currency",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = inkBlack
                    )
                }
            }

            // Applied TVA Row: Right (1st in RTL) = ضريبة القيمة المضافة (TVA): | Left (2nd in RTL) = +22.80 د.ج
            if (tvaAmount > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ضريبة القيمة المضافة (TVA):",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.5.sp,
                        color = inkBlack
                    )
                    Text(
                        text = "+${String.format(Locale.US, "%.2f", tvaAmount)} $currency",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = inkBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thick Outlined Net Total Box: Right (1st in RTL) = المجموع الصافي / Total Net | Left (2nd in RTL) = 1756.60 د.ج
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.8.dp, inkBlack)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1st in RTL -> Physical Right
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "المجموع الصافي",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = inkBlack
                        )
                        Text(
                            text = "Total Net",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = inkMuted
                        )
                    }

                    // 2nd in RTL -> Physical Left
                    Text(
                        text = "${String.format(Locale.US, "%.2f", netTotal)} $currency",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = inkBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Payment Method Row: Right (1st in RTL) = طريقة الدفع: | Left (2nd in RTL) = دين ونقد
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "طريقة الدفع:",
                    fontSize = 13.sp,
                    color = inkMuted
                )
                Text(
                    text = paymentMethodAr,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = inkBlack
                )
            }

            // Credit & Espèces Breakdown Rows: Right (1st in RTL) = Label | Left (2nd in RTL) = Amount
            if (hasDebtItems) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Credit:",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = inkBlack
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", creditAmount)} $currency",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = inkBlack
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Espèces:",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = inkBlack
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", especesAmount)} $currency",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = inkBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            ReceiptDashedDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Realistic Barcode Bars + * 602230 *
            ReceiptBarcodeGraphic(
                code = shortInvoiceNumber,
                inkColor = inkBlack
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (footerText.isNotBlank()) {
                Text(
                    text = footerText,
                    fontSize = 13.sp,
                    color = inkBlack,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (developerContact.isNotBlank()) {
                Text(
                    text = "المطور: $developerContact",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = inkBlack,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TemplateKReceiptContent(
    storeName: String,
    storePhone: String,
    customerName: String,
    cashierName: String,
    shortInvoiceNumber: String,
    formattedDateTime: String,
    paymentMethodAr: String,
    lines: List<PreviewLineItem>,
    totalPiecesCount: Int,
    totalCartonsCount: Int,
    sousTotal: Double,
    discountAmount: Double,
    discountPercentInt: Int,
    tvaAmount: Double,
    netTotal: Double,
    hasDebtItems: Boolean,
    creditAmount: Double,
    especesAmount: Double,
    previousDebt: Double,
    currency: String,
    footerText: String,
    developerContact: String,
    paperWidth: ThermalPaperWidth,
    inkBlack: Color,
    inkMuted: Color,
    storeLogoImagePath: String = "",
    storeLogoCustomText: String = "",
    customerLocationQrBitmap: android.graphics.Bitmap? = null
) {
    val hPadding = when (paperWidth) {
        ThermalPaperWidth.WIDTH_58MM -> 8.dp
        ThermalPaperWidth.WIDTH_80MM -> 14.dp
        ThermalPaperWidth.WIDTH_104MM -> 18.dp
    }
    val gridBorderColor = Color(0xFF0F172A)
    val gridHeaderBg = Color(0xFF0F172A)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPadding, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            InvoiceStoreLogoHeader(
                storeLogoImagePath = storeLogoImagePath,
                storeLogoCustomText = storeLogoCustomText
            )

            // Store Name
            Text(
                text = storeName.uppercase(Locale.getDefault()),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (paperWidth == ThermalPaperWidth.WIDTH_58MM) 17.sp else 21.sp,
                color = inkBlack,
                textAlign = TextAlign.Center
            )

            // TEL
            if (storePhone.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "TEL: $storePhone",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = inkMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Box: Bon & Date, Client
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.2.dp, gridBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bon: #1-$shortInvoiceNumber",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = inkBlack
                        )
                        Text(
                            text = "Date: $formattedDateTime",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = inkBlack
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Client: $customerName",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = inkBlack
                        )
                        if (cashierName.isNotBlank()) {
                            Text(
                                text = "Vendeur: $cashierName",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = inkMuted
                            )
                        }
                    }
                    if (customerLocationQrBitmap != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.2.dp, Color(0xFF94A3B8)), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Image(
                                    bitmap = customerLocationQrBitmap.asImageBitmap(),
                                    contentDescription = "Client Location",
                                    modifier = Modifier
                                        .size(88.dp)
                                        .padding(4.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📍 Localisation GPS Client",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    color = inkBlack
                                )
                                Text(
                                    text = "موقع تسليم العميل (خرائط Google)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = inkBlack
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = "Scanner avec l'appareil photo pour ouvrir Google Maps",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Column Table Header in French: Article | Qtt | Prix | Montant
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = gridHeaderBg,
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Article",
                        modifier = Modifier.weight(2.2f),
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Qtt",
                        modifier = Modifier.weight(0.7f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Prix",
                        modifier = Modifier.weight(0.95f),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Montant",
                        modifier = Modifier.weight(1.15f),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Table Rows
            lines.forEachIndexed { index, line ->
                val packLabel = formatPackagingLabel(line.quantityPieces, line.safeUnitsPerCarton)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Article with [3] or [0] +6 packaging tag
                        Row(
                            modifier = Modifier.weight(2.2f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFF94A3B8))
                            ) {
                                Text(
                                    text = packLabel,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp,
                                    color = inkBlack,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = line.name,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = inkBlack,
                                maxLines = 2
                            )
                            if (line.isDebt) {
                                Text(
                                    text = " (Dette)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        // Qtt
                        Text(
                            text = "${line.quantityPieces}",
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = inkBlack
                        )

                        // Prix
                        Text(
                            text = String.format(Locale.US, "%.2f", line.unitPrice),
                            modifier = Modifier.weight(0.95f),
                            textAlign = TextAlign.End,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = inkBlack
                        )

                        // Montant
                        Text(
                            text = String.format(Locale.US, "%.2f", line.finalLineTotal),
                            modifier = Modifier.weight(1.15f),
                            textAlign = TextAlign.End,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = inkBlack
                        )
                    }

                    if (line.itemDiscountAmount > 0.0 || line.itemTvaAmount > 0.0) {
                        val subNotes = buildList {
                            if (line.itemDiscountAmount > 0.0) add("Remise: -${String.format(Locale.US, "%.2f", line.itemDiscountAmount)}")
                            if (line.itemTvaAmount > 0.0) add("TVA: +${String.format(Locale.US, "%.2f", line.itemTvaAmount)}")
                        }.joinToString(" • ")
                        Text(
                            text = subNotes,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = inkMuted,
                            modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                        )
                    }

                    if (index < lines.lastIndex) {
                        Spacer(modifier = Modifier.height(3.dp))
                        ReceiptDashedDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            ReceiptSectionSolidDivider(color = inkBlack)
            Spacer(modifier = Modifier.height(8.dp))

            // Subtotals
            if (discountAmount > 0.0 || tvaAmount > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sous-Total:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = inkBlack)
                    Text("${String.format(Locale.US, "%.2f", sousTotal)} $currency", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = inkBlack)
                }
            }
            if (discountAmount > 0.0) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Remise ($discountPercentInt%):", fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, color = Color(0xFFDC2626))
                    Text("-${String.format(Locale.US, "%.2f", discountAmount)} $currency", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFFDC2626))
                }
            }
            if (tvaAmount > 0.0) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TVA:", fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, color = inkBlack)
                    Text("+${String.format(Locale.US, "%.2f", tvaAmount)} $currency", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = inkBlack)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Net Total Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(2.dp, inkBlack)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total:",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = inkBlack
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", netTotal)} DA",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = inkBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Payment Method
            val payMethodStr = if (creditAmount > 0 && especesAmount == 0.0) "CREDIT / DETTE"
                else if (creditAmount > 0) "MIXTE (ESP + CREDIT)"
                else "ESP"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Payment:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = inkBlack)
                Text(payMethodStr, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = inkBlack)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Balances: Utilisateur & Global
            val ancien = previousDebt
            val nouveau = previousDebt + creditAmount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Utilisateur:",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = inkBlack
                )
                Text(
                    text = "Ancien ${String.format(Locale.US, "%.2f", ancien)} DA -- Nouveau ${String.format(Locale.US, "%.2f", nouveau)} DA",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = inkBlack
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Global:",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = inkBlack
                )
                Text(
                    text = "Ancien ${String.format(Locale.US, "%.2f", ancien)} DA -- Nouveau ${String.format(Locale.US, "%.2f", nouveau)} DA",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = inkBlack
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            ReceiptDashedDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Optional footer message
            if (footerText.isNotBlank()) {
                Text(
                    text = footerText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = inkMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Permanent Developer Footer: المطور 0662931569
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                val devText = if (developerContact.isNotBlank()) "المطور $developerContact" else "المطور 0662931569"
                Text(
                    text = devText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ThermalPosTabularReceiptContent(
    isRtlArabicColumns: Boolean = false,
    storeName: String,
    storePhone: String,
    customerName: String,
    cashierName: String,
    shortInvoiceNumber: String,
    formattedDateTime: String,
    paymentMethodAr: String,
    lines: List<PreviewLineItem>,
    totalPiecesCount: Int,
    totalCartonsCount: Int,
    sousTotal: Double,
    discountAmount: Double,
    discountPercentInt: Int,
    tvaAmount: Double,
    netTotal: Double,
    hasDebtItems: Boolean,
    creditAmount: Double,
    especesAmount: Double,
    currency: String,
    footerText: String,
    developerContact: String,
    inkBlack: Color,
    inkMuted: Color,
    storeLogoImagePath: String = "",
    storeLogoCustomText: String = "",
    customerLocationQrBitmap: android.graphics.Bitmap? = null
) {
    val gridLineColor = Color(0xFF334155)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom Store Logo (Image or Text Crest) if configured
            InvoiceStoreLogoHeader(
                storeLogoImagePath = storeLogoImagePath,
                storeLogoCustomText = storeLogoCustomText
            )

            // Top Title: BON DE LIVRAISON
            Text(
                text = "BON DE LIVRAISON",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = inkBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Store / Merchant Name
            Text(
                text = storeName.uppercase(Locale.getDefault()),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = inkBlack,
                textAlign = TextAlign.Center
            )

            if (storePhone.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tel: $storePhone",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    color = inkMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dashed Rounded Header Box (DATE & CLIENT)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRoundRect(
                            color = gridLineColor,
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DATE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = inkBlack
                            )
                            Text(
                                text = formattedDateTime.replace("/", "-"),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = inkBlack
                            )
                        }
                        Text(
                            text = "#1-$shortInvoiceNumber",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = inkMuted
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CLIENT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = inkBlack
                        )
                        Text(
                            text = customerName,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = inkBlack
                        )
                    }

                    if (customerLocationQrBitmap != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.2.dp, Color(0xFF94A3B8)), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Image(
                                    bitmap = customerLocationQrBitmap.asImageBitmap(),
                                    contentDescription = "موقع العميل",
                                    modifier = Modifier
                                        .size(96.dp)
                                        .padding(4.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📍", fontSize = 14.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "موقع تسليم العميل (Google Maps)",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        color = inkBlack
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "امسح رمز QR بكاميرا الهاتف لفتح خرائط Google والتوجيه المباشر إلى موقع العميل",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Employé: $cashierName",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = inkMuted
                        )
                        Text(
                            text = "\u200Fالدفع: $paymentMethodAr",
                            style = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = inkBlack,
                                textDirection = TextDirection.Rtl
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5-Column Grid Table:
            // - LTR (حراري POS): Désignation | Prix | Col | Qt | Total
            // - RTL (Bon de commande): اسم السلعة | سعر الوحدة | عدد العبوات | الكمية | المجموع (من اليمين إلى اليسار)
            val tableLayoutDirection = if (isRtlArabicColumns) LayoutDirection.Rtl else LayoutDirection.Ltr
            val col1Weight = if (isRtlArabicColumns) 1.90f else 2.25f
            val col2Weight = if (isRtlArabicColumns) 1.05f else 0.95f
            val col3Weight = if (isRtlArabicColumns) 1.05f else 0.75f
            val col4Weight = if (isRtlArabicColumns) 0.75f else 0.75f
            val col5Weight = if (isRtlArabicColumns) 1.10f else 1.15f
            val headerFontSize = if (isRtlArabicColumns) 10.5.sp else 12.sp

            CompositionLocalProvider(LocalLayoutDirection provides tableLayoutDirection) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Solid Continuous Line separating Header Info from Items Section
                    ReceiptSectionSolidDivider(color = inkBlack)

                    // Table Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRtlArabicColumns) "اسم السلعة" else "Désignation",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = headerFontSize,
                            color = inkBlack,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .weight(col1Weight)
                                .padding(vertical = 4.dp, horizontal = 3.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(gridLineColor)
                        )
                        Text(
                            text = if (isRtlArabicColumns) "سعر الوحدة" else "Prix",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = headerFontSize,
                            color = inkBlack,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(col2Weight)
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(gridLineColor)
                        )
                        Text(
                            text = if (isRtlArabicColumns) "عدد العبوات" else "Col",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = headerFontSize,
                            color = inkBlack,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(col3Weight)
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(gridLineColor)
                        )
                        Text(
                            text = if (isRtlArabicColumns) "الكمية" else "Qt",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = headerFontSize,
                            color = inkBlack,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(col4Weight)
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(gridLineColor)
                        )
                        Text(
                            text = if (isRtlArabicColumns) "المجموع" else "Total",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = headerFontSize,
                            color = inkBlack,
                            textAlign = if (isRtlArabicColumns) TextAlign.Center else TextAlign.End,
                            modifier = Modifier
                                .weight(col5Weight)
                                .padding(vertical = 4.dp, horizontal = 3.dp)
                        )
                    }

                    // Header Bottom Solid Continuous Line
                    ReceiptSectionSolidDivider(color = inkBlack)

                    // Table Body Rows
                    lines.forEachIndexed { idx, line ->
                        val colCartonsText = when {
                            line.fullCartons > 0 && line.loosePieces == 0 ->
                                "${line.fullCartons}"
                            line.fullCartons > 0 && line.loosePieces > 0 ->
                                "${line.fullCartons}+${line.loosePieces}"
                            else ->
                                "0"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Column 1: اسم السلعة / Désignation + (دين) + Remise/خصم + TVA/ضريبة
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier
                                    .weight(col1Weight)
                                    .padding(vertical = 5.dp, horizontal = 3.dp)
                            ) {
                                val nameWithDebt = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = inkBlack
                                        )
                                    ) {
                                        append(line.name)
                                    }
                                    if (line.isDebt) {
                                        append(" ")
                                        withStyle(
                                            SpanStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = inkBlack
                                            )
                                        ) {
                                            append("(دين)")
                                        }
                                    }
                                }
                                Text(
                                    text = nameWithDebt,
                                    textAlign = TextAlign.Start,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (line.itemDiscountAmount > 0.0) {
                                    Text(
                                        text = "\u200Fخصم (${line.discountPercentFormatted}%): -${String.format(Locale.US, "%.2f", line.itemDiscountAmount)}",
                                        style = LocalTextStyle.current.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = inkBlack,
                                            textDirection = TextDirection.Rtl
                                        ),
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (line.itemTvaAmount > 0.0) {
                                    Text(
                                        text = "\u200FTVA (${line.tvaPercentFormatted}%): +${String.format(Locale.US, "%.2f", line.itemTvaAmount)}",
                                        style = LocalTextStyle.current.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp,
                                            color = inkMuted,
                                            textDirection = TextDirection.Rtl
                                        ),
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(gridLineColor)
                            )

                            // Column 2: سعر الوحدة / Prix
                            Text(
                                text = String.format(Locale.US, "%.2f", line.unitPrice),
                                style = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = inkBlack,
                                    textDirection = TextDirection.Ltr
                                ),
                                textAlign = if (isRtlArabicColumns) TextAlign.Center else TextAlign.End,
                                modifier = Modifier
                                    .weight(col2Weight)
                                    .padding(vertical = 5.dp, horizontal = 3.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(gridLineColor)
                            )

                            // Column 3: عدد العبوات / Col
                            Text(
                                text = colCartonsText,
                                style = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = inkBlack,
                                    textDirection = TextDirection.Ltr
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(col3Weight)
                                    .padding(vertical = 5.dp, horizontal = 2.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(gridLineColor)
                            )

                            // Column 4: الكمية / Qt
                            Text(
                                text = "${line.quantityPieces}",
                                style = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = inkBlack,
                                    textDirection = TextDirection.Ltr
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(col4Weight)
                                    .padding(vertical = 5.dp, horizontal = 2.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(gridLineColor)
                            )

                            // Column 5: المجموع / Total
                            Text(
                                text = String.format(Locale.US, "%.2f", line.subtotalAfterDiscount),
                                style = LocalTextStyle.current.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = inkBlack,
                                    textDirection = TextDirection.Ltr
                                ),
                                textAlign = if (isRtlArabicColumns) TextAlign.Center else TextAlign.End,
                                modifier = Modifier
                                    .weight(col5Weight)
                                    .padding(vertical = 5.dp, horizontal = 3.dp)
                            )
                        }

                        // Small distinct item separator line between items
                        if (idx < lines.lastIndex) {
                            ReceiptItemSeparatorDivider()
                        }
                    }

                    // Bottom Solid Continuous Line separating Items Section from Bottom Summary
                    ReceiptSectionSolidDivider(color = inkBlack)
                }
            }

            // Bottom Counts Row right under the table
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${lines.size}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = inkBlack
                )
                Text(
                    text = "Col: $totalCartonsCount  |  Qt: $totalPiecesCount",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = inkBlack
                )
            }

            ReceiptDashedDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Dashed Rounded Financial Summary Box (Sous-total, Remise, TVA, Net, Versement, الدين)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawRoundRect(
                            color = gridLineColor,
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // 1. المجموع الفرعي (Sous-total)
                    ThermalSummaryLine(
                        label = "Sous-total / المجموع الفرعي",
                        value = "${String.format(Locale.US, "%.2f", sousTotal)} $currency",
                        inkColor = inkBlack,
                        isBold = false
                    )

                    // 2. الخصم (Remise)
                    if (discountAmount > 0.0) {
                        ThermalSummaryLine(
                            label = "Remise ($discountPercentInt%) / الخصم",
                            value = "-${String.format(Locale.US, "%.2f", discountAmount)} $currency",
                            inkColor = inkBlack,
                            isBold = true
                        )
                    }

                    // 3. ضريبة القيمة المضافة (TVA)
                    if (tvaAmount > 0.0) {
                        ThermalSummaryLine(
                            label = "TVA / الضريبة",
                            value = "+${String.format(Locale.US, "%.2f", tvaAmount)} $currency",
                            inkColor = inkBlack,
                            isBold = true
                        )
                    }

                    // 4. المجموع الصافي (Net)
                    ThermalSummaryLine(
                        label = "Net / المجموع الصافي",
                        value = "${String.format(Locale.US, "%.2f", netTotal)} $currency",
                        inkColor = inkBlack,
                        isBold = true,
                        fontSizeSp = 14.5f
                    )

                    // 5. المدفوع (Versement)
                    ThermalSummaryLine(
                        label = "Versement / المدفوع",
                        value = "${String.format(Locale.US, "%.2f", especesAmount)} $currency",
                        inkColor = inkBlack,
                        isBold = false
                    )

                    // 6. الدين (Crédit / دين)
                    ThermalSummaryLine(
                        label = "Crédit / الدين",
                        value = "${String.format(Locale.US, "%.2f", creditAmount)} $currency",
                        inkColor = inkBlack,
                        isBold = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barcode Graphic
            ReceiptBarcodeGraphic(
                code = shortInvoiceNumber,
                inkColor = inkBlack
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (footerText.isNotBlank()) {
                Text(
                    text = footerText,
                    fontSize = 12.sp,
                    color = inkBlack,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (developerContact.isNotBlank()) {
                Text(
                    text = "المطور: $developerContact",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = inkBlack,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ThermalSummaryLine(
    label: String,
    value: String,
    inkColor: Color,
    isBold: Boolean,
    fontSizeSp: Float = 12.5f
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = fontSizeSp.sp,
            color = inkColor
        )
        Text(
            text = "\u200F$value",
            style = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = fontSizeSp.sp,
                color = inkColor,
                textDirection = TextDirection.Rtl
            )
        )
    }
}

@Composable
private fun ReceiptSectionSolidDivider(
    color: Color = Color(0xFF0F172A)
) {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.8.dp,
        color = color
    )
}

@Composable
private fun ReceiptSmallDashedItemDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.4.dp)
    ) {
        drawLine(
            color = Color(0xFF475569),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f), 0f)
        )
    }
}

@Composable
private fun ReceiptItemSeparatorDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
        )
    }
}

@Composable
private fun ReceiptDashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.5.dp)
    ) {
        drawLine(
            color = Color(0xFF334155),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    }
}

@Composable
private fun ReceiptBarcodeGraphic(
    code: String,
    inkColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Canvas(
            modifier = Modifier
                .width(220.dp)
                .height(38.dp)
        ) {
            val barCount = 13
            val spacing = 4.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (size.width - totalSpacing) / barCount
            for (i in 0 until barCount) {
                val x = i * (barWidth + spacing)
                drawRect(
                    color = inkColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "* $code *",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = inkColor,
            letterSpacing = 1.5.sp
        )
    }
}

internal fun shareInvoiceAsText(
    context: Context,
    storeName: String,
    storePhone: String,
    customerName: String,
    cashierName: String,
    invoiceNum: String,
    dateTime: String,
    paymentMethodAr: String,
    lines: List<PreviewLineItem>,
    sousTotal: Double,
    discountAmount: Double,
    discountPercentInt: Int,
    tvaAmount: Double,
    netTotal: Double,
    creditAmount: Double,
    especesAmount: Double,
    currency: String,
    footerText: String,
    developerContact: String
) {
    val sb = StringBuilder()
    sb.appendLine("      $storeName      ")
    if (storePhone.isNotBlank()) sb.appendLine("هاتف: $storePhone")
    sb.appendLine("[ الزبون: $customerName ]")
    sb.appendLine("Employé(e) / الكاشير: $cashierName")
    sb.appendLine("--------------------------------")
    sb.appendLine("$invoiceNum       $dateTime 📅")
    sb.appendLine("طريقة الدفع: $paymentMethodAr")
    sb.appendLine("--------------------------------")
    lines.forEachIndexed { idx, line ->
        val debtLabel = if (line.isDebt) " (دين)" else ""
        val packTag = formatPackagingLabel(line.quantityPieces, line.safeUnitsPerCarton)
        sb.appendLine("$packTag ${line.name}$debtLabel   ${String.format(Locale.US, "%.2f", line.subtotalAfterDiscount)} $currency")
        sb.appendLine("[ سعة العبوة: ${line.safeUnitsPerCarton} قطعة ]")
        sb.appendLine("${line.fullCartons} كرتونة [سعة ${line.safeUnitsPerCarton} ق] × ${String.format(Locale.US, "%.2f", line.unitPrice)} $currency")
        sb.appendLine(line.barcode)
        if (line.itemDiscountAmount > 0.0) {
            sb.appendLine("Remise / خصم: -${String.format(Locale.US, "%.2f", line.itemDiscountAmount)} $currency (${line.discountPercentFormatted}%)")
        }
        if (line.itemTvaAmount > 0.0) {
            sb.appendLine("TVA / ضريبة: +${String.format(Locale.US, "%.2f", line.itemTvaAmount)} $currency (${line.tvaPercentFormatted}%)")
        }
        if (idx < lines.lastIndex) {
            sb.appendLine("- - - - - - - - - - - - - - - -")
        }
    }
    sb.appendLine("--------------------------------")
    val totalPieces = lines.sumOf { it.quantityPieces }
    val totalCartons = lines.sumOf { it.fullCartons }
    sb.appendLine("إجمالي الكمية المباعة: ${lines.size} أصناف ($totalPieces قطعة) [$totalCartons كرتونة 📦]")
    sb.appendLine("المجموع الفرعي (Sous-total): ${String.format(Locale.US, "%.2f", sousTotal)} $currency")
    if (discountAmount > 0.0) {
        sb.appendLine("الخصم المطبق ($discountPercentInt%) : -${String.format(Locale.US, "%.2f", discountAmount)} $currency")
    }
    if (tvaAmount > 0.0) {
        sb.appendLine("ضريبة القيمة المضافة (TVA): +${String.format(Locale.US, "%.2f", tvaAmount)} $currency")
    }
    sb.appendLine("================================")
    sb.appendLine("المجموع الصافي (Total Net): ${String.format(Locale.US, "%.2f", netTotal)} $currency")
    sb.appendLine("================================")
    sb.appendLine("طريقة الدفع: $paymentMethodAr")
    if (creditAmount > 0.0) {
        sb.appendLine("Credit: ${String.format(Locale.US, "%.2f", creditAmount)} $currency")
        sb.appendLine("Espèces: ${String.format(Locale.US, "%.2f", especesAmount)} $currency")
    }
    if (footerText.isNotBlank()) sb.appendLine(footerText)
    val devContactStr = if (developerContact.isNotBlank()) developerContact else "0662931569"
    sb.appendLine("المطور: $devContactStr")

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الفاتورة"))
}

internal fun exportAndShareInvoicePdf(
    context: Context,
    style: InvoiceStyle,
    storeName: String,
    storePhone: String,
    customerName: String,
    cashierName: String,
    shortInvoiceNumber: String,
    formattedDateTime: String,
    paymentMethodAr: String,
    lines: List<PreviewLineItem>,
    totalPiecesCount: Int,
    totalCartonsCount: Int,
    sousTotal: Double,
    discountAmount: Double,
    discountPercentInt: Int,
    tvaAmount: Double,
    netTotal: Double,
    hasDebtItems: Boolean,
    creditAmount: Double,
    especesAmount: Double,
    currency: String,
    footerText: String,
    developerContact: String,
    storeLogoImagePath: String = "",
    storeLogoCustomText: String = "",
    previousDebt: Double = 0.0,
    paperWidth: ThermalPaperWidth = ThermalPaperWidth.WIDTH_80MM,
    customerLocationQrBitmap: android.graphics.Bitmap? = null
) {
    try {
        val pdfDocument = PdfDocument()
        val hasCustomLogo = storeLogoImagePath.isNotBlank() || storeLogoCustomText.isNotBlank()
        val pageHeight = (520 + (if (hasCustomLogo) 68 else 0) + lines.size * 95).coerceAtLeast(580)
        val pageInfo = PdfDocument.PageInfo.Builder(320, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
        }
        val regularPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
        }

        var y = 20f
        y = drawStoreLogoOnPdfCanvas(
            canvas = canvas,
            centerX = 160f,
            topY = y,
            storeLogoImagePath = storeLogoImagePath,
            storeLogoCustomText = storeLogoCustomText,
            maxWidthPx = 115,
            maxHeightPx = 54
        )
        y += 14f
        canvas.drawText(storeName, 160f, y, titlePaint)
        y += 18f
        if (storePhone.isNotBlank()) {
            regularPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("هاتف: $storePhone", 160f, y, regularPaint)
            y += 16f
        }
        if (style == InvoiceStyle.TEMPLATE_K) {
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Bon: #1-$shortInvoiceNumber", 16f, y, boldPaint)
            regularPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Date: $formattedDateTime", 304f, y, regularPaint)
            y += 15f

            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Client: $customerName", 16f, y, boldPaint)
            if (cashierName.isNotBlank()) {
                regularPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Vendeur: $cashierName", 304f, y, regularPaint)
            }
            y += 15f
            if (customerLocationQrBitmap != null) {
                val qrSize = 72f
                val qrLeft = (320f - qrSize) / 2f
                val qrDst = android.graphics.RectF(qrLeft, y, qrLeft + qrSize, y + qrSize)
                canvas.drawBitmap(customerLocationQrBitmap, null, qrDst, null)
                y += qrSize + 10f
                val qrLabelPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    color = android.graphics.Color.BLACK
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("📍 Localisation GPS Client (Google Maps)", 160f, y, qrLabelPaint)
                y += 14f
            }

            // Table Header Bar: Article | Qtt | Prix | Montant
            val tableHeaderPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(15, 23, 42)
                this.style = Paint.Style.FILL
            }
            val tableHeaderTextPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textSize = 9.5f
            }
            canvas.drawRect(16f, y, 304f, y + 18f, tableHeaderPaint)
            tableHeaderTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Article", 20f, y + 13f, tableHeaderTextPaint)
            tableHeaderTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Qtt", 170f, y + 13f, tableHeaderTextPaint)
            tableHeaderTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Prix", 235f, y + 13f, tableHeaderTextPaint)
            canvas.drawText("Montant", 298f, y + 13f, tableHeaderTextPaint)
            y += 24f

            val itemDividerPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(71, 85, 105)
                strokeWidth = 1.1f
                this.style = Paint.Style.STROKE
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }

            lines.forEachIndexed { idx, line ->
                val packLabel = formatPackagingLabel(line.quantityPieces, line.safeUnitsPerCarton)
                boldPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("$packLabel ${line.name}", 16f, y, boldPaint)

                regularPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("${line.quantityPieces}", 170f, y, regularPaint)

                regularPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(String.format(Locale.US, "%.2f", line.unitPrice), 235f, y, regularPaint)

                boldPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(String.format(Locale.US, "%.2f", line.finalLineTotal), 298f, y, boldPaint)

                y += 14f
                if (idx < lines.lastIndex) {
                    canvas.drawLine(16f, y, 304f, y, itemDividerPaint)
                    y += 8f
                }
            }

            y += 4f
            val solidSectionPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(15, 23, 42)
                strokeWidth = 2f
                this.style = Paint.Style.STROKE
            }
            canvas.drawLine(16f, y, 304f, y, solidSectionPaint)
            y += 16f

            if (discountAmount > 0.0 || tvaAmount > 0.0) {
                regularPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("Sous-Total:", 16f, y, regularPaint)
                regularPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("${String.format(Locale.US, "%.2f", sousTotal)} $currency", 304f, y, regularPaint)
                y += 14f
            }

            // Box for Total
            val totalBoxRect = android.graphics.RectF(16f, y, 304f, y + 26f)
            canvas.drawRect(totalBoxRect, solidSectionPaint)
            titlePaint.textSize = 13f
            titlePaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Total:", 24f, y + 18f, titlePaint)
            titlePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${String.format(Locale.US, "%.2f", netTotal)} DA", 296f, y + 18f, titlePaint)
            y += 36f

            val payMethodStr = if (creditAmount > 0 && especesAmount == 0.0) "CREDIT / DETTE"
                else if (creditAmount > 0) "MIXTE (ESP + CREDIT)"
                else "ESP"
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Payment:", 16f, y, boldPaint)
            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(payMethodStr, 304f, y, boldPaint)
            y += 16f

            val ancien = previousDebt
            val nouveau = previousDebt + creditAmount
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Utilisateur: Ancien ${String.format(Locale.US, "%.2f", ancien)} DA -- Nouveau ${String.format(Locale.US, "%.2f", nouveau)} DA", 16f, y, boldPaint)
            y += 14f
            canvas.drawText("Global: Ancien ${String.format(Locale.US, "%.2f", ancien)} DA -- Nouveau ${String.format(Locale.US, "%.2f", nouveau)} DA", 16f, y, boldPaint)
            y += 18f

            canvas.drawLine(16f, y, 304f, y, itemDividerPaint)
            y += 14f

            if (footerText.isNotBlank()) {
                regularPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(footerText, 160f, y, regularPaint)
                y += 14f
            }

            boldPaint.textAlign = Paint.Align.CENTER
            val devText = if (developerContact.isNotBlank()) "المطور $developerContact" else "المطور 0662931569"
            canvas.drawText(devText, 160f, y, boldPaint)
        } else {
            boldPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("الزبون: $customerName", 160f, y, boldPaint)
            y += 15f
            if (customerLocationQrBitmap != null) {
                val qrSize = 76f
                val qrLeft = (320f - qrSize) / 2f
                val qrDst = android.graphics.RectF(qrLeft, y, qrLeft + qrSize, y + qrSize)
                canvas.drawBitmap(customerLocationQrBitmap, null, qrDst, null)
                y += qrSize + 10f
                val qrLabelPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.BLACK
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("📍 موقع العميل على خرائط Google", 160f, y, qrLabelPaint)
                y += 14f
            }
            regularPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Employé(e) / الكاشير: $cashierName", 300f, y, regularPaint)
            y += 14f
            canvas.drawLine(16f, y, 304f, y, regularPaint)
            y += 16f
            canvas.drawText("التاريخ: $formattedDateTime  |  #1-$shortInvoiceNumber", 300f, y, regularPaint)
            y += 15f
            canvas.drawText("طريقة الدفع: $paymentMethodAr", 300f, y, boldPaint)
            y += 14f
            val solidSectionPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(15, 23, 42)
                strokeWidth = 2f
                this.style = Paint.Style.STROKE
            }
            val itemDividerPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(71, 85, 105)
                strokeWidth = 1.1f
                this.style = Paint.Style.STROKE
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
            }

            // Solid continuous line separating top info from items
            canvas.drawLine(16f, y, 304f, y, solidSectionPaint)
            y += 18f

            val isBonLeft = style == InvoiceStyle.BON_DE_LIVRAISON
            val itemX = if (isBonLeft) 16f else 300f
            val itemAlign = if (isBonLeft) Paint.Align.LEFT else Paint.Align.RIGHT
            val priceX = if (isBonLeft) 300f else 16f
            val priceAlign = if (isBonLeft) Paint.Align.RIGHT else Paint.Align.LEFT

            lines.forEachIndexed { idx, line ->
                val debtTag = if (line.isDebt) " (دين)" else ""
                boldPaint.textAlign = itemAlign
                canvas.drawText("${line.name}$debtTag", itemX, y, boldPaint)
                boldPaint.textAlign = priceAlign
                canvas.drawText("${String.format(Locale.US, "%.2f", line.subtotalAfterDiscount)} $currency", priceX, y, boldPaint)
                y += 14f
                regularPaint.textAlign = itemAlign
                canvas.drawText("سعة العبوة: ${line.safeUnitsPerCarton} قطعة", itemX, y, regularPaint)
                y += 13f
                canvas.drawText(
                    "${line.fullCartons} كرتونة [سعة ${line.safeUnitsPerCarton} ق] × ${String.format(Locale.US, "%.2f", line.unitPrice)} $currency",
                    itemX,
                    y,
                    regularPaint
                )
                y += 13f
                canvas.drawText(line.barcode, itemX, y, regularPaint)
                if (line.itemDiscountAmount > 0.0) {
                    y += 13f
                    boldPaint.textAlign = itemAlign
                    canvas.drawText(
                        "Remise / خصم: -${String.format(Locale.US, "%.2f", line.itemDiscountAmount)} $currency (${line.discountPercentFormatted}%)",
                        itemX,
                        y,
                        boldPaint
                    )
                }
                if (line.itemTvaAmount > 0.0) {
                    y += 13f
                    regularPaint.textAlign = itemAlign
                    canvas.drawText(
                        "TVA / ضريبة: +${String.format(Locale.US, "%.2f", line.itemTvaAmount)} $currency (${line.tvaPercentFormatted}%)",
                        itemX,
                        y,
                        regularPaint
                    )
                }
                if (idx < lines.lastIndex) {
                    y += 8f
                    canvas.drawLine(24f, y, 296f, y, itemDividerPaint)
                    y += 14f
                } else {
                    y += 14f
                }
            }

            // Solid continuous line separating items from bottom summary
            canvas.drawLine(16f, y, 304f, y, solidSectionPaint)
            y += 16f
            regularPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                "إجمالي الكمية المباعة: ${lines.size} أصناف ($totalPiecesCount قطعة) [$totalCartonsCount كرتونة]",
                300f,
                y,
                regularPaint
            )
            y += 18f
            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                "المجموع الفرعي (Sous-total): ${String.format(Locale.US, "%.2f", sousTotal)} $currency",
                300f,
                y,
                boldPaint
            )
            if (discountAmount > 0.0) {
                y += 15f
                canvas.drawText(
                    "الخصم المطبق ($discountPercentInt%) : -${String.format(Locale.US, "%.2f", discountAmount)} $currency",
                    300f,
                    y,
                    regularPaint
                )
            }
            if (tvaAmount > 0.0) {
                y += 15f
                canvas.drawText(
                    "ضريبة القيمة المضافة (TVA): +${String.format(Locale.US, "%.2f", tvaAmount)} $currency",
                    300f,
                    y,
                    regularPaint
                )
            }
            y += 20f
            titlePaint.textSize = 14f
            canvas.drawText(
                "المجموع الصافي (Total Net): ${String.format(Locale.US, "%.2f", netTotal)} $currency",
                160f,
                y,
                titlePaint
            )
            y += 18f
            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("طريقة الدفع: $paymentMethodAr", 300f, y, boldPaint)
            if (hasDebtItems) {
                y += 15f
                canvas.drawText("Credit: ${String.format(Locale.US, "%.2f", creditAmount)} $currency", 300f, y, boldPaint)
                y += 15f
                canvas.drawText("Espèces: ${String.format(Locale.US, "%.2f", especesAmount)} $currency", 300f, y, boldPaint)
            }
            y += 22f
            regularPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("* $shortInvoiceNumber *", 160f, y, regularPaint)
            y += 16f
            if (footerText.isNotBlank()) {
                canvas.drawText(footerText, 160f, y, regularPaint)
                y += 15f
            }
            if (developerContact.isNotBlank()) {
                boldPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("المطور: $developerContact", 160f, y, boldPaint)
            }
        }

        pdfDocument.finishPage(page)

        val pdfDir = File(context.cacheDir, "invoices")
        pdfDir.mkdirs()
        val pdfFile = File(pdfDir, "Bon_de_livraison_$shortInvoiceNumber.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        Toast.makeText(
            context,
            "تم تجهيز ملف PDF (${style.displayNameAr}) ومشاركته 📄",
            Toast.LENGTH_SHORT
        ).show()

        shareInvoiceAsText(
            context = context,
            storeName = storeName,
            storePhone = storePhone,
            customerName = customerName,
            cashierName = cashierName,
            invoiceNum = "#1-$shortInvoiceNumber",
            dateTime = formattedDateTime,
            paymentMethodAr = paymentMethodAr,
            lines = lines,
            sousTotal = sousTotal,
            discountAmount = discountAmount,
            discountPercentInt = discountPercentInt,
            tvaAmount = tvaAmount,
            netTotal = netTotal,
            creditAmount = creditAmount,
            especesAmount = especesAmount,
            currency = currency,
            footerText = footerText,
            developerContact = developerContact
        )
    } catch (e: Exception) {
        Toast.makeText(context, "جاري مشاركة تفاصيل الفاتورة...", Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun InvoiceStoreLogoHeader(
    storeLogoImagePath: String,
    storeLogoCustomText: String,
    maxHeightDp: Dp = 56.dp
) {
    val resolvedLogoModel = remember(storeLogoImagePath) {
        ProductImageStorageHelper.resolveImageModel(storeLogoImagePath)
    }
    val cleanText = storeLogoCustomText.trim()

    if (resolvedLogoModel != null) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .heightIn(max = maxHeightDp)
                .widthIn(max = 150.dp)
                .testTag("invoice_custom_logo_image")
        ) {
            AsyncImage(
                model = resolvedLogoModel,
                contentDescription = "شعار المحل في الفاتورة",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(maxHeightDp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
    } else if (cleanText.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.testTag("invoice_custom_logo_text")
        ) {
            Text(
                text = cleanText.take(18),
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}

internal fun drawStoreLogoOnPdfCanvas(
    canvas: android.graphics.Canvas,
    centerX: Float,
    topY: Float,
    storeLogoImagePath: String,
    storeLogoCustomText: String,
    maxWidthPx: Int = 115,
    maxHeightPx: Int = 54
): Float {
    val logoBmp = ThermalReceiptBitmapRenderer.loadHighContrastThermalLogoBitmap(
        imagePath = storeLogoImagePath,
        maxWidthPx = maxWidthPx,
        maxHeightPx = maxHeightPx
    )
    if (logoBmp != null) {
        val left = centerX - (logoBmp.width / 2f)
        canvas.drawBitmap(logoBmp, left, topY, null)
        return topY + logoBmp.height + 6f
    }

    val cleanText = storeLogoCustomText.trim()
    if (cleanText.isNotEmpty()) {
        val crestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        val display = cleanText.take(18)
        val textW = textPaint.measureText(display)
        val boxW = (textW + 24f).coerceIn(70f, maxWidthPx.toFloat() + 30f)
        val boxH = 24f
        val rect = RectF(centerX - boxW / 2f, topY, centerX + boxW / 2f, topY + boxH)
        canvas.drawRoundRect(rect, 6f, 6f, crestPaint)
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(display, centerX, textY, textPaint)
        return topY + boxH + 6f
    }
    return topY
}
