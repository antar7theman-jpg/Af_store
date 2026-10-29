package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.ArchivedInvoice
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.PurchaseInvoice
import com.example.data.models.Sale
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.ThermalReceiptPrintPayload
import androidx.core.content.FileProvider
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Send
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

internal data class DebtPaymentReceiptData(
    val receiptNumber: String,
    val partyType: String, // "CUSTOMER" or "SUPPLIER"
    val partyName: String,
    val partyPhone: String,
    val previousBalance: Double,
    val paidAmount: Double,
    val remainingBalance: Double,
    val notes: String,
    val timestampMillis: Long,
    val invoiceReference: String? = null
)

fun normalizeDebtInvoiceReference(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val clean = raw.trim()
        .replace("Pre #", "")
        .replace("Pre#", "")
        .replace("Pre ", "")
        .replace("Pre", "")
        .replace("pre #", "")
        .replace("pre#", "")
        .replace("pre ", "")
        .replace("pre", "")
        .replace("\u202A", "")
        .replace("\u202C", "")
        .replace("\u200E", "")
        .trim()

    val invertedMatch = Regex("""^(\d{6})-#?(\d+)$""").find(clean)
    if (invertedMatch != null) {
        val datePart = invertedMatch.groupValues[1]
        val seqPart = invertedMatch.groupValues[2]
        return "#$seqPart-$datePart"
    }

    val standardMatch = Regex("""^#?(\d+)-(\d{6})$""").find(clean)
    if (standardMatch != null) {
        val seqPart = standardMatch.groupValues[1]
        val datePart = standardMatch.groupValues[2]
        return "#$seqPart-$datePart"
    }

    val stripped = clean.removePrefix("#").trim()
    return if (stripped.isNotBlank()) "#$stripped" else ""
}

fun sanitizeNotesDebtInvoiceRef(notes: String): String {
    if (notes.isBlank()) return "تحصيل دفعة نقدية من حساب المديونية"
    var result = notes.replace(Regex("""(\d{6})-#?(\d+)""")) { m ->
        val datePart = m.groupValues[1]
        val seqPart = m.groupValues[2]
        "\u202A\u200E#$seqPart-$datePart\u200E\u202C"
    }
    result = result.replace(Regex("""(?<!\u200E)#(\d+-\d+)""")) { m ->
        val num = m.groupValues[1]
        "\u202A\u200E#$num\u200E\u202C"
    }
    return result
}

internal data class ShareCustomerDebtPayload(
    val customer: Customer,
    val transactions: List<CustomerTransaction>,
    val latestInvoice: ArchivedInvoice? = null,
    val focusTransaction: CustomerTransaction? = null,
    val initialMode: String = "INVOICE", // "INVOICE" or "STATEMENT"
    val preferredChannel: String = "WHATSAPP" // "WHATSAPP" or "MESSENGER"
)

private data class UnifiedDebtMovementItem(
    val uniqueKey: String,
    val partyType: String, // "CUSTOMER" or "SUPPLIER"
    val movementKind: String, // "DEBT" or "PAYMENT"
    val partyId: Int,
    val partyName: String,
    val partyPhone: String,
    val amount: Double,
    val notes: String,
    val timestampMillis: Long,
    val customerRef: Customer? = null,
    val supplierInvoiceRef: PurchaseInvoice? = null
)

@Composable
fun DebtsAndMovementsScreen(viewModel: AntarSalesViewModel) {
    val semantic = LocalAFStoreSemanticColors.current
    val isDark = semantic.isDark
    val context = LocalContext.current

    val customers by viewModel.customers.collectAsState()
    val customerTransactions by viewModel.customerTransactions.collectAsState()
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsState()
    val archivedInvoices by viewModel.archivedInvoices.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val receiptFooterText by viewModel.receiptFooterText.collectAsState()
    val developerContact by viewModel.developerContact.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // 0 = حركة الديون الشاملة, 1 = ديون العملاء, 2 = ديون الموردين
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // ALL, TODAY, YESTERDAY, LAST_7_DAYS, THIS_MONTH
    var selectedDateFilter by rememberSaveable { mutableStateOf("ALL") }
    // ALL, DEBT_ONLY, PAYMENT_ONLY, ACTIVE_DEBT_ONLY
    var selectedTypeFilter by rememberSaveable { mutableStateOf("ALL") }

    // Dialog states
    var showAddDebtModal by remember { mutableStateOf(false) }
    var initialAddDebtPartyType by remember { mutableStateOf("CUSTOMER") }
    var preselectedCustomerForAddDebt by remember { mutableStateOf<Customer?>(null) }
    var preselectedSupplierNameForAddDebt by remember { mutableStateOf("") }
    var preselectedSupplierPhoneForAddDebt by remember { mutableStateOf("") }

    var customerForPayment by remember { mutableStateOf<Customer?>(null) }
    var customerInitialInvoiceId by remember { mutableStateOf<String?>(null) }
    var supplierInvoiceForPayment by remember { mutableStateOf<PurchaseInvoice?>(null) }
    var showQuickPayPickerDialog by remember { mutableStateOf(false) }

    var statementCustomer by remember { mutableStateOf<Customer?>(null) }
    var statementSupplierName by remember { mutableStateOf<String?>(null) }

    var previewArchivedInvoice by remember { mutableStateOf<ArchivedInvoice?>(null) }
    var previewSupplierInvoice by remember { mutableStateOf<PurchaseInvoice?>(null) }
    var activePaymentReceipt by remember { mutableStateOf<DebtPaymentReceiptData?>(null) }
    var shareCustomerPayload by remember { mutableStateOf<ShareCustomerDebtPayload?>(null) }
    var showDebtsPdfReportDialog by remember { mutableStateOf(false) }

    fun buildCustomerPaymentReceiptFromHistory(
        customer: Customer?,
        customerId: Int,
        customerName: String,
        customerPhone: String,
        paidAmount: Double,
        notes: String,
        txDateMillis: Long,
        txId: Int? = null
    ): DebtPaymentReceiptData {
        val currentDebt = customer?.balanceDebt ?: 0.0
        val laterTxs = customerTransactions.filter { t ->
            t.customerId == customerId &&
                (t.date > txDateMillis || (txId != null && t.date == txDateMillis && t.id > txId))
        }
        val laterPayments = laterTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
        val laterDebts = laterTxs.filter { it.type != "PAYMENT" }.sumOf { it.amount }
        val remainingAfterPayment = (currentDebt + laterPayments - laterDebts).coerceAtLeast(0.0)
        val previousBeforePayment = (remainingAfterPayment + paidAmount).coerceAtLeast(paidAmount)

        val payMatch = Regex("""(Pay\s*#\d{1,5}-\d{6})""").find(notes)
        val dateStr = SimpleDateFormat("ddMMyy", Locale.US).format(Date(txDateMillis))
        val receiptCode = payMatch?.groupValues?.get(1)
            ?: "Pay #${(txId ?: (txDateMillis / 1000 % 1000)).toString().takeLast(3)}-$dateStr"
        val cleanNotes = notes.replace(Regex("""\[Pay\s*#\d{1,5}-\d{6}\]"""), "").trim()
        val invRawCode = Regex("""(?:فاتورة|invoice|ref)\s*#?([A-Za-z0-9\-]+)""", RegexOption.IGNORE_CASE)
            .find(cleanNotes)?.groupValues?.get(1)
            ?: Regex("""#([A-Za-z0-9\-]+)""").find(cleanNotes)?.groupValues?.get(1)
        val invCodeFromNotes = normalizeDebtInvoiceReference(invRawCode).takeIf { it.isNotBlank() }
        return DebtPaymentReceiptData(
            receiptNumber = receiptCode,
            partyType = "CUSTOMER",
            partyName = customer?.name ?: customerName,
            partyPhone = customer?.phone ?: customerPhone,
            previousBalance = previousBeforePayment,
            paidAmount = paidAmount,
            remainingBalance = remainingAfterPayment,
            notes = cleanNotes.ifBlank { "تحصيل دفعة نقدية من حساب المديونية" },
            timestampMillis = txDateMillis,
            invoiceReference = invCodeFromNotes
        )
    }

    fun buildSupplierPaymentReceipt(
        invoice: PurchaseInvoice,
        paidAmountOverride: Double? = null,
        previousBalanceOverride: Double? = null,
        remainingBalanceOverride: Double? = null,
        timestampOverride: Long? = null
    ): DebtPaymentReceiptData {
        val paid = paidAmountOverride ?: invoice.paidAmount
        val rem = remainingBalanceOverride ?: invoice.remainingDebt
        val prev = previousBalanceOverride ?: (rem + paid)
        val ts = timestampOverride ?: invoice.dateMillis
        val receiptCode = "SPAY-${(ts / 1000).toString().takeLast(6)}"
        return DebtPaymentReceiptData(
            receiptNumber = receiptCode,
            partyType = "SUPPLIER",
            partyName = invoice.supplierName,
            partyPhone = invoice.supplierPhone,
            previousBalance = prev,
            paidAmount = paid,
            remainingBalance = rem,
            notes = "سداد دفعة من فاتورة توريد رقم ${invoice.invoiceNumber}",
            timestampMillis = ts,
            invoiceReference = invoice.invoiceNumber
        )
    }

    fun resolveCustomerInvoiceForMovement(
        customerId: Int,
        customerName: String,
        movementKind: String,
        amount: Double,
        notes: String,
        timestampMillis: Long
    ): ArchivedInvoice {
        // 1. Check if notes contain #CODE (e.g., "فاتورة مبيعات #706573")
        val codeRegex = Regex("#([A-Za-z0-9-]+)")
        val extractedCode = codeRegex.find(notes)?.groupValues?.getOrNull(1).orEmpty()
        if (extractedCode.isNotEmpty()) {
            val byCode = archivedInvoices.find { inv ->
                inv.invoiceId.equals("INV-$extractedCode", ignoreCase = true) ||
                    inv.invoiceId.endsWith(extractedCode, ignoreCase = true) ||
                    inv.invoiceId.contains(extractedCode, ignoreCase = true)
            }
            if (byCode != null) return byCode
        }

        // 2. Fallback: match by customerId/customerName & closest checkout timestamp (within 90 seconds)
        val byTimestamp = archivedInvoices.find { inv ->
            val sameCustomer = (inv.customerId != null && inv.customerId == customerId) ||
                inv.customerName.equals(customerName, ignoreCase = true)
            sameCustomer && abs(inv.dateMillis - timestampMillis) <= 90_000L
        }
        if (byTimestamp != null) return byTimestamp

        // 3. Fallback: build a clean receipt ArchivedInvoice for manual debt/payment entry
        val fallbackId = if (extractedCode.isNotEmpty()) {
            "INV-$extractedCode"
        } else {
            val prefix = if (movementKind == "PAYMENT") "PAY" else "DEBT"
            "$prefix-${(timestampMillis / 1000).toString().takeLast(6)}"
        }
        val itemTitle = notes.ifBlank {
            if (movementKind == "PAYMENT") "سند تحصيل دفعة من حساب المديونية" else "قيد مديونية مسجل على الحساب"
        }
        return ArchivedInvoice(
            invoiceId = fallbackId,
            dateMillis = timestampMillis,
            customerName = customerName,
            customerId = customerId,
            paymentMethod = if (movementKind == "PAYMENT") "CASH" else "DEBT",
            isDebt = movementKind != "PAYMENT",
            items = listOf(
                Sale(
                    saleId = 0,
                    productId = 0,
                    productName = itemTitle,
                    quantitySold = 1,
                    totalPrice = amount,
                    saleDate = timestampMillis,
                    customerId = customerId,
                    customerName = customerName,
                    isDebt = movementKind != "PAYMENT",
                    paymentMethod = if (movementKind == "PAYMENT") "CASH" else "DEBT",
                    invoiceGroupId = fallbackId
                )
            ),
            subtotal = amount,
            totalQuantity = 1
        )
    }

    fun openShareCustomerDialog(
        customer: Customer,
        focusTx: CustomerTransaction? = null,
        initialMode: String = "INVOICE",
        preferredChannel: String = "WHATSAPP"
    ) {
        val liveCust = customers.find { it.id == customer.id } ?: customer
        val custTxs = customerTransactions
            .filter { it.customerId == liveCust.id }
            .sortedByDescending { it.date }
        val targetTx = focusTx ?: custTxs.maxByOrNull { it.date }
        val resolvedInvoice = if (targetTx != null) {
            resolveCustomerInvoiceForMovement(
                customerId = liveCust.id,
                customerName = liveCust.name,
                movementKind = if (targetTx.type == "PAYMENT") "PAYMENT" else "DEBT",
                amount = targetTx.amount,
                notes = targetTx.notes,
                timestampMillis = targetTx.date
            )
        } else {
            archivedInvoices
                .filter {
                    (it.customerId != null && it.customerId == liveCust.id) ||
                        it.customerName.equals(liveCust.name, ignoreCase = true)
                }
                .maxByOrNull { it.dateMillis }
        }
        shareCustomerPayload = ShareCustomerDebtPayload(
            customer = liveCust,
            transactions = custTxs,
            latestInvoice = resolvedInvoice,
            focusTransaction = targetTx,
            initialMode = if (resolvedInvoice != null || targetTx != null) initialMode else "STATEMENT",
            preferredChannel = preferredChannel
        )
    }

    // KPI Calculations
    val totalCustomerDebts = remember(customers) {
        customers.sumOf { it.balanceDebt.coerceAtLeast(0.0) }
    }
    val indebtedCustomersCount = remember(customers) {
        customers.count { it.balanceDebt > 0.009 }
    }
    val totalSupplierDebts = remember(purchaseInvoices) {
        purchaseInvoices.sumOf { it.remainingDebt.coerceAtLeast(0.0) }
    }
    val unpaidSupplierInvoicesCount = remember(purchaseInvoices) {
        purchaseInvoices.count { it.remainingDebt > 0.009 }
    }
    val totalCustomerPaymentsCollected = remember(customerTransactions) {
        customerTransactions.filter { it.type == "PAYMENT" }.sumOf { it.amount }
    }
    val totalSupplierPaymentsPaid = remember(purchaseInvoices) {
        purchaseInvoices.sumOf { it.paidAmount }
    }

    // Date filter helper
    fun matchesDateFilter(timeMillis: Long): Boolean {
        if (selectedDateFilter == "ALL") return true
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        return when (selectedDateFilter) {
            "TODAY" -> timeMillis >= startOfToday
            "YESTERDAY" -> {
                val startOfYesterday = startOfToday - 24L * 60 * 60 * 1000
                timeMillis in startOfYesterday until startOfToday
            }
            "LAST_7_DAYS" -> {
                val sevenDaysAgo = startOfToday - 7L * 24 * 60 * 60 * 1000
                timeMillis >= sevenDaysAgo
            }
            "THIS_MONTH" -> {
                val monthCal = Calendar.getInstance()
                monthCal.set(Calendar.DAY_OF_MONTH, 1)
                monthCal.set(Calendar.HOUR_OF_DAY, 0)
                monthCal.set(Calendar.MINUTE, 0)
                monthCal.set(Calendar.SECOND, 0)
                monthCal.set(Calendar.MILLISECOND, 0)
                timeMillis >= monthCal.timeInMillis
            }
            else -> true
        }
    }

    // Build Unified Movements List
    val unifiedMovements = remember(
        customerTransactions,
        customers,
        purchaseInvoices,
        searchQuery,
        selectedDateFilter,
        selectedTypeFilter
    ) {
        val customerMap = customers.associateBy { it.id }
        val list = mutableListOf<UnifiedDebtMovementItem>()

        customerTransactions.forEach { tx ->
            val cust = customerMap[tx.customerId]
            list.add(
                UnifiedDebtMovementItem(
                    uniqueKey = "cust_tx_${tx.id}",
                    partyType = "CUSTOMER",
                    movementKind = if (tx.type == "PAYMENT") "PAYMENT" else "DEBT",
                    partyId = tx.customerId,
                    partyName = cust?.name ?: tx.customerName,
                    partyPhone = cust?.phone.orEmpty(),
                    amount = tx.amount,
                    notes = tx.notes.ifBlank {
                        if (tx.type == "PAYMENT") "تحصيل دفعة من العميل" else "تسجيل مديونية عميل"
                    },
                    timestampMillis = tx.date,
                    customerRef = cust
                )
            )
        }

        purchaseInvoices.forEach { inv ->
            if (inv.remainingDebt > 0.009 || inv.paymentStatus != "PAID") {
                list.add(
                    UnifiedDebtMovementItem(
                        uniqueKey = "sup_debt_${inv.id}",
                        partyType = "SUPPLIER",
                        movementKind = "DEBT",
                        partyId = inv.id,
                        partyName = inv.supplierName,
                        partyPhone = inv.supplierPhone,
                        amount = inv.remainingDebt.takeIf { it > 0.0 } ?: inv.totalCost,
                        notes = "فاتورة مورد (${inv.invoiceNumber}) • ${inv.notes.ifBlank { "آجل للمورد" }}",
                        timestampMillis = inv.dateMillis,
                        supplierInvoiceRef = inv
                    )
                )
            }
            if (inv.paidAmount > 0.009) {
                list.add(
                    UnifiedDebtMovementItem(
                        uniqueKey = "sup_paid_${inv.id}",
                        partyType = "SUPPLIER",
                        movementKind = "PAYMENT",
                        partyId = inv.id,
                        partyName = inv.supplierName,
                        partyPhone = inv.supplierPhone,
                        amount = inv.paidAmount,
                        notes = "دفعة مسددة للمورد • فاتورة (${inv.invoiceNumber})",
                        timestampMillis = inv.updatedAt.takeIf { it > 0L } ?: inv.dateMillis,
                        supplierInvoiceRef = inv
                    )
                )
            }
        }

        val q = searchQuery.trim()
        list.filter { item ->
            val dateOk = matchesDateFilter(item.timestampMillis)
            val typeOk = when (selectedTypeFilter) {
                "DEBT_ONLY" -> item.movementKind == "DEBT"
                "PAYMENT_ONLY" -> item.movementKind == "PAYMENT"
                "ACTIVE_DEBT_ONLY" -> {
                    if (item.partyType == "CUSTOMER") {
                        (item.customerRef?.balanceDebt ?: 0.0) > 0.009
                    } else {
                        (item.supplierInvoiceRef?.remainingDebt ?: 0.0) > 0.009
                    }
                }
                else -> true
            }
            val queryOk = q.isEmpty() ||
                item.partyName.contains(q, ignoreCase = true) ||
                item.partyPhone.contains(q, ignoreCase = true) ||
                item.notes.contains(q, ignoreCase = true)
            dateOk && typeOk && queryOk
        }.sortedByDescending { it.timestampMillis }
    }

    // Filtered Customers List for Tab 1
    val filteredCustomers = remember(customers, searchQuery, selectedTypeFilter) {
        val q = searchQuery.trim()
        customers.filter { cust ->
            val queryOk = q.isEmpty() ||
                cust.name.contains(q, ignoreCase = true) ||
                cust.phone.contains(q, ignoreCase = true) ||
                cust.notes.contains(q, ignoreCase = true)
            val filterOk = when (selectedTypeFilter) {
                "ACTIVE_DEBT_ONLY", "DEBT_ONLY" -> cust.balanceDebt > 0.009
                "PAYMENT_ONLY" -> cust.totalPurchases > 0.0 || cust.balanceDebt <= 0.009
                else -> true
            }
            queryOk && filterOk
        }.sortedWith(
            compareByDescending<Customer> { it.balanceDebt > 0.009 }
                .thenByDescending { it.balanceDebt }
                .thenBy { it.name }
        )
    }

    // Filtered Supplier Invoices List for Tab 2
    val filteredSupplierInvoices = remember(purchaseInvoices, searchQuery, selectedDateFilter, selectedTypeFilter) {
        val q = searchQuery.trim()
        purchaseInvoices.filter { inv ->
            val dateOk = matchesDateFilter(inv.dateMillis)
            val queryOk = q.isEmpty() ||
                inv.supplierName.contains(q, ignoreCase = true) ||
                inv.supplierPhone.contains(q, ignoreCase = true) ||
                inv.invoiceNumber.contains(q, ignoreCase = true) ||
                inv.notes.contains(q, ignoreCase = true)
            val filterOk = when (selectedTypeFilter) {
                "ACTIVE_DEBT_ONLY", "DEBT_ONLY" -> inv.remainingDebt > 0.009
                "PAYMENT_ONLY" -> inv.paidAmount > 0.009
                else -> true
            }
            dateOk && queryOk && filterOk
        }.sortedWith(
            compareByDescending<PurchaseInvoice> { it.remainingDebt > 0.009 }
                .thenByDescending { it.remainingDebt }
                .thenByDescending { it.dateMillis }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── 1. Top Header Card with Clean KPI Summary & Direct Actions ──
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
            border = BorderStroke(1.dp, semantic.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row 1: Full Title and Subtitle with Icon (Right on user's side)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.12f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.5f else 0.7f)
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "الديون وحركتها",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "مركز الديون وحركتها",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1
                            )
                            Text(
                                text = "متابعة ديون العملاء والموردين والتحصيل",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Header Row 2: Action Buttons (Add Debt, Quick Pay, PDF Reports)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add New Debt Button
                    Surface(
                        onClick = {
                            initialAddDebtPartyType = if (selectedSubTab == 2) "SUPPLIER" else "CUSTOMER"
                            preselectedCustomerForAddDebt = null
                            preselectedSupplierNameForAddDebt = ""
                            preselectedSupplierPhoneForAddDebt = ""
                            showAddDebtModal = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1.15f)
                            .height(36.dp)
                            .testTag("debts_add_new_debt_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "إضافة دين",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إضافة دين",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Quick Pay Button
                    Surface(
                        onClick = { showQuickPayPickerDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.profitGreen.copy(alpha = if (isDark) 0.16f else 0.12f),
                        border = BorderStroke(
                            1.dp,
                            semantic.profitGreen.copy(alpha = if (isDark) 0.6f else 0.8f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("debts_quick_pay_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "تسديد دفعة",
                                tint = semantic.profitGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تسديد دفعة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = semantic.profitGreen
                            )
                        }
                    }

                    // PDF Report Button
                    Surface(
                        onClick = { showDebtsPdfReportDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.infoBlue.copy(alpha = if (isDark) 0.16f else 0.12f),
                        border = BorderStroke(
                            1.dp,
                            semantic.infoBlue.copy(alpha = if (isDark) 0.5f else 0.7f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("debts_pdf_report_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "التقارير PDF",
                                tint = semantic.infoBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "التقارير PDF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = semantic.infoBlue
                            )
                        }
                    }
                }

                // 3 KPI Summary Pills (Slim, High-Density)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // KPI 1: ديون العملاء (لنا)
                    Surface(
                        onClick = { selectedSubTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = if (isDark) 0.12f else 0.08f),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFEF4444).copy(alpha = if (isDark) 0.45f else 0.6f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ديون العملاء ($indebtedCustomersCount)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalCustomerDebts)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    // KPI 2: ديون الموردين (علينا)
                    Surface(
                        onClick = { selectedSubTab = 2 },
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.debtAmber.copy(alpha = if (isDark) 0.14f else 0.10f),
                        border = BorderStroke(
                            1.dp,
                            semantic.debtAmber.copy(alpha = if (isDark) 0.45f else 0.65f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = semantic.debtAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ديون الموردين ($unpaidSupplierInvoicesCount)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.debtAmber,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalSupplierDebts)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    // KPI 3: إجمالي التحصيل والسداد
                    Surface(
                        onClick = {
                            selectedSubTab = 0
                            selectedTypeFilter = if (selectedTypeFilter == "PAYMENT_ONLY") "ALL" else "PAYMENT_ONLY"
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.profitGreen.copy(alpha = if (isDark) 0.12f else 0.08f),
                        border = BorderStroke(
                            1.dp,
                            semantic.profitGreen.copy(alpha = if (isDark) 0.45f else 0.65f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = semantic.profitGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "إجمالي المسدد",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.profitGreen,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalCustomerPaymentsCollected + totalSupplierPaymentsPaid)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── 2. Interactive Navigation Tabs ──
        val tabs = listOf(
            Triple(0, "حركة الديون (${unifiedMovements.size})", Icons.Default.History),
            Triple(1, "ديون العملاء (${customers.size})", Icons.Default.Group),
            Triple(2, "ديون الموردين (${purchaseInvoices.size})", Icons.Default.LocalShipping)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.dp, semantic.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { (index, title, icon) ->
                    val isSelected = selectedSubTab == index
                    Surface(
                        onClick = { selectedSubTab = index },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("debts_subtab_$index")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Compact Search Bar + Inline Filters ──
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.dp, semantic.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Search Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "ابحث بالاسم، الهاتف، أو البيان...",
                                fontSize = 12.sp
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = semantic.cardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("debts_search_input")
                    )
                }

                // Filter Chips Row (Operation Type + Date Filters)
                val typeFilterChips = listOf(
                    "ALL" to "كل العمليات",
                    "ACTIVE_DEBT_ONLY" to "مديونية نشطة",
                    "DEBT_ONLY" to "إضافة دين فقط",
                    "PAYMENT_ONLY" to "تسديد دفعات فقط"
                )
                val dateFilterChips = listOf(
                    "ALL" to "كل الأوقات",
                    "TODAY" to "اليوم",
                    "YESTERDAY" to "أمس",
                    "LAST_7_DAYS" to "آخر 7 أيام",
                    "THIS_MONTH" to "هذا الشهر"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "فلترة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    typeFilterChips.forEach { (key, label) ->
                        val active = selectedTypeFilter == key
                        Surface(
                            onClick = { selectedTypeFilter = key },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) {
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (active) 1.2.dp else 0.8.dp,
                                color = if (active) MaterialTheme.colorScheme.primary else semantic.cardBorder
                            ),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("debts_type_filter_$key")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .width(1.dp)
                            .background(semantic.cardBorder)
                    )

                    dateFilterChips.forEach { (key, label) ->
                        val active = selectedDateFilter == key
                        Surface(
                            onClick = { selectedDateFilter = key },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) {
                                semantic.debtAmber.copy(alpha = if (isDark) 0.20f else 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (active) 1.2.dp else 0.8.dp,
                                color = if (active) semantic.debtAmber else semantic.cardBorder
                            ),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("debts_date_filter_$key")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    color = if (active) semantic.debtAmber else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 4. Active Sub-Tab Content ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedSubTab) {
                0 -> UnifiedMovementsTimelineList(
                    movements = unifiedMovements,
                    currencySymbol = currencySymbol,
                    isDark = isDark,
                    onPayCustomer = { cust, invId ->
                        customerForPayment = cust
                        customerInitialInvoiceId = invId
                    },
                    onPaySupplierInvoice = { inv -> supplierInvoiceForPayment = inv },
                    onOpenCustomerStatement = { cust -> statementCustomer = cust },
                    onOpenSupplierStatement = { supName -> statementSupplierName = supName },
                    onViewMovementInvoice = { item ->
                        if (item.partyType == "CUSTOMER") {
                            previewArchivedInvoice = resolveCustomerInvoiceForMovement(
                                customerId = item.partyId,
                                customerName = item.partyName,
                                movementKind = item.movementKind,
                                amount = item.amount,
                                notes = item.notes,
                                timestampMillis = item.timestampMillis
                            )
                        } else {
                            item.supplierInvoiceRef?.let { supInv ->
                                previewSupplierInvoice = supInv
                            }
                        }
                    },
                    onPrintPaymentReceipt = { item ->
                        if (item.partyType == "CUSTOMER") {
                            activePaymentReceipt = buildCustomerPaymentReceiptFromHistory(
                                customer = item.customerRef,
                                customerId = item.partyId,
                                customerName = item.partyName,
                                customerPhone = item.partyPhone,
                                paidAmount = item.amount,
                                notes = item.notes,
                                txDateMillis = item.timestampMillis
                            )
                        } else {
                            item.supplierInvoiceRef?.let { supInv ->
                                activePaymentReceipt = buildSupplierPaymentReceipt(
                                    invoice = supInv,
                                    paidAmountOverride = item.amount,
                                    timestampOverride = item.timestampMillis
                                )
                            }
                        }
                    },
                    onShareCustomerMovement = { item, channel ->
                        val cust = item.customerRef
                            ?: customers.find { it.id == item.partyId }
                            ?: Customer(
                                id = item.partyId,
                                name = item.partyName,
                                phone = item.partyPhone,
                                balanceDebt = item.amount
                            )
                        val matchingTx = customerTransactions.find {
                            it.customerId == cust.id &&
                                it.date == item.timestampMillis &&
                                abs(it.amount - item.amount) < 0.01
                        } ?: CustomerTransaction(
                            id = 0,
                            customerId = cust.id,
                            customerName = cust.name,
                            type = item.movementKind,
                            amount = item.amount,
                            date = item.timestampMillis,
                            notes = item.notes
                        )
                        openShareCustomerDialog(
                            customer = cust,
                            focusTx = matchingTx,
                            initialMode = "INVOICE",
                            preferredChannel = channel
                        )
                    },
                    onAddDebtClick = {
                        initialAddDebtPartyType = "CUSTOMER"
                        showAddDebtModal = true
                    }
                )

                1 -> CustomerDebtsTabContent(
                    customers = filteredCustomers,
                    transactions = customerTransactions,
                    currencySymbol = currencySymbol,
                    isDark = isDark,
                    onPayCustomer = { cust -> customerForPayment = cust },
                    onAddDebtForCustomer = { cust ->
                        initialAddDebtPartyType = "CUSTOMER"
                        preselectedCustomerForAddDebt = cust
                        showAddDebtModal = true
                    },
                    onOpenCustomerStatement = { cust -> statementCustomer = cust },
                    onShareCustomerDebt = { cust, latestTx, channel ->
                        openShareCustomerDialog(
                            customer = cust,
                            focusTx = latestTx,
                            initialMode = if (latestTx != null) "INVOICE" else "STATEMENT",
                            preferredChannel = channel
                        )
                    },
                    onViewCustomerLatestInvoice = { cust, tx ->
                        if (tx.type == "PAYMENT") {
                            activePaymentReceipt = buildCustomerPaymentReceiptFromHistory(
                                customer = cust,
                                customerId = cust.id,
                                customerName = cust.name,
                                customerPhone = cust.phone,
                                paidAmount = tx.amount,
                                notes = tx.notes,
                                txDateMillis = tx.date,
                                txId = tx.id
                            )
                        } else {
                            previewArchivedInvoice = resolveCustomerInvoiceForMovement(
                                customerId = cust.id,
                                customerName = cust.name,
                                movementKind = "DEBT",
                                amount = tx.amount,
                                notes = tx.notes,
                                timestampMillis = tx.date
                            )
                        }
                    },
                    onCreateNewCustomerDebt = {
                        initialAddDebtPartyType = "CUSTOMER"
                        preselectedCustomerForAddDebt = null
                        showAddDebtModal = true
                    }
                )

                2 -> SupplierDebtsTabContent(
                    invoices = filteredSupplierInvoices,
                    currencySymbol = currencySymbol,
                    isDark = isDark,
                    onPaySupplierInvoice = { inv -> supplierInvoiceForPayment = inv },
                    onAddSupplierDebt = { supName, supPhone ->
                        initialAddDebtPartyType = "SUPPLIER"
                        preselectedSupplierNameForAddDebt = supName
                        preselectedSupplierPhoneForAddDebt = supPhone
                        showAddDebtModal = true
                    },
                    onOpenSupplierStatement = { supName -> statementSupplierName = supName },
                    onViewSupplierInvoice = { inv -> previewSupplierInvoice = inv }
                )
            }
        }
    }

    // ── Modals & Dialogs ──

    // 1. Quick Payment Picker Dialog (when tapping top "تسديد دفعة")
    if (showQuickPayPickerDialog) {
        QuickPayPartyPickerDialog(
            customers = customers.filter { it.balanceDebt > 0.009 },
            supplierInvoices = purchaseInvoices.filter { it.remainingDebt > 0.009 },
            currencySymbol = currencySymbol,
            onDismiss = { showQuickPayPickerDialog = false },
            onSelectCustomer = { cust ->
                showQuickPayPickerDialog = false
                customerForPayment = cust
            },
            onSelectSupplierInvoice = { inv ->
                showQuickPayPickerDialog = false
                supplierInvoiceForPayment = inv
            }
        )
    }

    // 2. Add New Debt Dialog (Customer or Supplier)
    if (showAddDebtModal) {
        AddDebtOperationDialog(
            customers = customers,
            initialPartyType = initialAddDebtPartyType,
            preselectedCustomer = preselectedCustomerForAddDebt,
            preselectedSupplierName = preselectedSupplierNameForAddDebt,
            preselectedSupplierPhone = preselectedSupplierPhoneForAddDebt,
            currencySymbol = currencySymbol,
            onDismiss = {
                showAddDebtModal = false
                preselectedCustomerForAddDebt = null
            },
            onAddExistingCustomerDebt = { customer, amount, notes ->
                viewModel.addCustomerManualDebt(customer, amount, notes)
                showAddDebtModal = false
                preselectedCustomerForAddDebt = null
            },
            onCreateNewCustomerWithDebt = { name, phone, amount, notes ->
                viewModel.createCustomerWithInitialDebt(name, phone, amount, notes)
                showAddDebtModal = false
                preselectedCustomerForAddDebt = null
            },
            onAddSupplierDebt = { supplierName, supplierPhone, amount, notes ->
                viewModel.recordManualSupplierDebt(supplierName, supplierPhone, amount, notes)
                showAddDebtModal = false
            }
        )
    }

    // 3. Customer Payment Dialog
    customerForPayment?.let { targetCustomer ->
        val liveCustomer = customers.find { it.id == targetCustomer.id } ?: targetCustomer
        CustomerDebtPaymentDialog(
            customer = liveCustomer,
            currencySymbol = currencySymbol,
            archivedInvoices = archivedInvoices,
            customerTransactions = customerTransactions,
            initialInvoiceId = customerInitialInvoiceId,
            onDismiss = {
                customerForPayment = null
                customerInitialInvoiceId = null
            },
            onConfirmPayment = { amount, notes, invoiceRef ->
                val now = System.currentTimeMillis()
                val receiptNum = viewModel.generateNextPaymentReceiptNumber(now)
                val cleanInv = invoiceRef?.trim()?.removePrefix("#")?.takeIf { it.isNotBlank() }
                val invTag = if (cleanInv != null) " [فاتورة #$cleanInv]" else ""
                val fullNotes = if (notes.isNotBlank()) "[$receiptNum]$invTag $notes" else "[$receiptNum]$invTag تحصيل دفعة نقدية من حساب المديونية"
                viewModel.recordCustomerPayment(liveCustomer, amount, fullNotes)
                customerForPayment = null
                customerInitialInvoiceId = null
            },
            onConfirmAndPrintReceipt = { amount, notes, invoiceRef ->
                val prevDebt = liveCustomer.balanceDebt
                val remDebt = (prevDebt - amount).coerceAtLeast(0.0)
                val now = System.currentTimeMillis()
                val receiptNum = viewModel.generateNextPaymentReceiptNumber(now)
                val cleanInv = invoiceRef?.trim()?.removePrefix("#")?.takeIf { it.isNotBlank() }
                val invTag = if (cleanInv != null) " [فاتورة #$cleanInv]" else ""
                val fullNotes = if (notes.isNotBlank()) "[$receiptNum]$invTag $notes" else "[$receiptNum]$invTag تحصيل دفعة نقدية من حساب المديونية"
                viewModel.recordCustomerPayment(liveCustomer, amount, fullNotes)
                customerForPayment = null
                customerInitialInvoiceId = null
                activePaymentReceipt = DebtPaymentReceiptData(
                    receiptNumber = receiptNum,
                    partyType = "CUSTOMER",
                    partyName = liveCustomer.name,
                    partyPhone = liveCustomer.phone,
                    previousBalance = prevDebt,
                    paidAmount = amount,
                    remainingBalance = remDebt,
                    notes = notes.ifBlank { "تحصيل دفعة نقدية من حساب المديونية" },
                    timestampMillis = now,
                    invoiceReference = invoiceRef
                )
            }
        )
    }

    // 4. Supplier Debt Payment Dialog
    supplierInvoiceForPayment?.let { targetInvoice ->
        val liveInvoice = purchaseInvoices.find { it.id == targetInvoice.id } ?: targetInvoice
        SupplierDebtPaymentDialog(
            invoice = liveInvoice,
            currencySymbol = currencySymbol,
            onDismiss = { supplierInvoiceForPayment = null },
            onConfirmPayment = { amount ->
                viewModel.paySupplierInvoiceDebt(liveInvoice, amount)
                supplierInvoiceForPayment = null
            },
            onConfirmAndPrintReceipt = { amount ->
                val prevDebt = liveInvoice.remainingDebt
                val remDebt = (prevDebt - amount).coerceAtLeast(0.0)
                val now = System.currentTimeMillis()
                viewModel.paySupplierInvoiceDebt(liveInvoice, amount)
                supplierInvoiceForPayment = null
                activePaymentReceipt = buildSupplierPaymentReceipt(
                    invoice = liveInvoice,
                    paidAmountOverride = amount,
                    previousBalanceOverride = prevDebt,
                    remainingBalanceOverride = remDebt,
                    timestampOverride = now
                )
            }
        )
    }

    // 5. Customer Account Statement Dialog
    statementCustomer?.let { targetCustomer ->
        val liveCustomer = customers.find { it.id == targetCustomer.id } ?: targetCustomer
        val custMovements = remember(customerTransactions, liveCustomer.id) {
            customerTransactions.filter { it.customerId == liveCustomer.id }
                .sortedByDescending { it.date }
        }
        CustomerAccountStatementDialog(
            customer = liveCustomer,
            transactions = custMovements,
            storeName = storeName,
            storePhone = storePhone,
            currencySymbol = currencySymbol,
            footerText = receiptFooterText,
            context = context,
            onDismiss = { statementCustomer = null },
            onPayNow = {
                statementCustomer = null
                customerForPayment = liveCustomer
            },
            onAddDebtNow = {
                statementCustomer = null
                initialAddDebtPartyType = "CUSTOMER"
                preselectedCustomerForAddDebt = liveCustomer
                showAddDebtModal = true
            },
            onViewTransactionInvoice = { tx ->
                previewArchivedInvoice = resolveCustomerInvoiceForMovement(
                    customerId = liveCustomer.id,
                    customerName = liveCustomer.name,
                    movementKind = if (tx.type == "PAYMENT") "PAYMENT" else "DEBT",
                    amount = tx.amount,
                    notes = tx.notes,
                    timestampMillis = tx.date
                )
            },
            onPrintPaymentReceipt = { tx ->
                activePaymentReceipt = buildCustomerPaymentReceiptFromHistory(
                    customer = liveCustomer,
                    customerId = liveCustomer.id,
                    customerName = liveCustomer.name,
                    customerPhone = liveCustomer.phone,
                    paidAmount = tx.amount,
                    notes = tx.notes,
                    txDateMillis = tx.date,
                    txId = tx.id
                )
            },
            onShareTransaction = { tx, channel ->
                openShareCustomerDialog(
                    customer = liveCustomer,
                    focusTx = tx,
                    initialMode = "INVOICE",
                    preferredChannel = channel
                )
            }
        )
    }

    // 6. Supplier Account Statement Dialog
    statementSupplierName?.let { supName ->
        val supplierInvoicesList = remember(purchaseInvoices, supName) {
            purchaseInvoices.filter { it.supplierName.equals(supName, ignoreCase = true) }
                .sortedByDescending { it.dateMillis }
        }
        SupplierAccountStatementDialog(
            supplierName = supName,
            invoices = supplierInvoicesList,
            storeName = storeName,
            storePhone = storePhone,
            currencySymbol = currencySymbol,
            footerText = receiptFooterText,
            context = context,
            onDismiss = { statementSupplierName = null },
            onPayInvoice = { inv ->
                statementSupplierName = null
                supplierInvoiceForPayment = inv
            },
            onViewInvoice = { inv ->
                previewSupplierInvoice = inv
            },
            onPrintPaymentReceipt = { inv ->
                activePaymentReceipt = buildSupplierPaymentReceipt(invoice = inv)
            }
        )
    }

    // 7. Customer / Sales Invoice Preview Dialog (Full Template & Thermal/PDF Support)
    previewArchivedInvoice?.let { targetInvoice ->
        InvoiceTemplatePreviewDialog(
            viewModel = viewModel,
            archivedInvoice = targetInvoice,
            isPreSavePreview = false,
            onDismiss = { previewArchivedInvoice = null }
        )
    }

    // 8. Supplier Purchase Invoice Details & Print Preview Dialog
    previewSupplierInvoice?.let { targetSupInv ->
        val liveSupInv = purchaseInvoices.find { it.id == targetSupInv.id } ?: targetSupInv
        SupplierInvoiceDetailsPreviewDialog(
            invoice = liveSupInv,
            currencySymbol = currencySymbol,
            onDismiss = { previewSupplierInvoice = null },
            onPayDebt = {
                previewSupplierInvoice = null
                supplierInvoiceForPayment = liveSupInv
            },
            onOpenPrintTemplate = {
                val parsedItems = liveSupInv.getItems()
                val mappedSales = if (parsedItems.isNotEmpty()) {
                    parsedItems.mapIndexed { idx, item ->
                        Sale(
                            saleId = idx + 1,
                            productId = item.productId ?: 0,
                            productName = item.productName,
                            quantitySold = item.quantity,
                            totalPrice = item.quantity * item.unitCost,
                            saleDate = liveSupInv.dateMillis,
                            customerName = "المورد: ${liveSupInv.supplierName}",
                            isDebt = liveSupInv.remainingDebt > 0,
                            paymentMethod = if (liveSupInv.remainingDebt > 0) "DEBT" else "CASH",
                            invoiceGroupId = liveSupInv.invoiceNumber
                        )
                    }
                } else {
                    listOf(
                        Sale(
                            saleId = 1,
                            productId = 0,
                            productName = liveSupInv.notes.ifBlank { "فاتورة توريد بضائع للمورد ${liveSupInv.supplierName}" },
                            quantitySold = 1,
                            totalPrice = liveSupInv.totalCost,
                            saleDate = liveSupInv.dateMillis,
                            customerName = "المورد: ${liveSupInv.supplierName}",
                            isDebt = liveSupInv.remainingDebt > 0,
                            paymentMethod = if (liveSupInv.remainingDebt > 0) "DEBT" else "CASH",
                            invoiceGroupId = liveSupInv.invoiceNumber
                        )
                    )
                }
                previewSupplierInvoice = null
                previewArchivedInvoice = ArchivedInvoice(
                    invoiceId = liveSupInv.invoiceNumber,
                    dateMillis = liveSupInv.dateMillis,
                    customerName = "المورد: ${liveSupInv.supplierName}",
                    customerId = null,
                    paymentMethod = if (liveSupInv.remainingDebt > 0) "DEBT" else "CASH",
                    isDebt = liveSupInv.remainingDebt > 0,
                    items = mappedSales,
                    subtotal = liveSupInv.totalCost,
                    totalQuantity = mappedSales.sumOf { it.quantitySold }
                )
            }
        )
    }

    // 9. Debt Payment Receipt Preview & Print Dialog (وصل سداد الدين)
    activePaymentReceipt?.let { receiptData ->
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
            onDismiss = { activePaymentReceipt = null }
        )
    }

    // 10. Share Customer Invoice / Debt Statement via WhatsApp or Messenger Dialog
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

    // 11. Debts & Financial Movement Comprehensive PDF Report Dialog
    if (showDebtsPdfReportDialog) {
        val kpiMetrics = listOf(
            "ديون العملاء" to "${String.format(Locale.US, "%.1f", totalCustomerDebts)} $currencySymbol",
            "ديون الموردين" to "${String.format(Locale.US, "%.1f", totalSupplierDebts)} $currencySymbol",
            "إجمالي المسدد" to "${String.format(Locale.US, "%.1f", totalCustomerPaymentsCollected + totalSupplierPaymentsPaid)} $currencySymbol",
            "العملاء المدينين" to "$indebtedCustomersCount عميل"
        )
        val tableHeaders = listOf("الطرف المعني", "النوع", "المبلغ", "البيان / التاريخ")
        val tableRows = unifiedMovements.map { item ->
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(item.timestampMillis))
            val typeStr = if (item.movementKind == "PAYMENT") "سداد دفعة" else "قيد مديونية"
            listOf(
                item.partyName,
                typeStr,
                "${String.format(Locale.US, "%.1f", item.amount)} $currencySymbol",
                "${item.notes.ifBlank { "عملية مسجلة" }} ($dateStr)"
            )
        }.ifEmpty {
            listOf(
                listOf("لا توجد حركات ديون مسجلة حالياً", "-", "0.0 $currencySymbol", "-")
            )
        }
        val payload = UniversalPdfReportPayload(
            reportType = SectionPdfReportType.DEBTS,
            storeName = storeName,
            storePhone = storePhone,
            currencySymbol = currencySymbol,
            kpiMetrics = kpiMetrics,
            tableHeaders = tableHeaders,
            tableRows = tableRows,
            notesOrSummary = "تقرير شامل ومفصل لحركات الديون والتحصيلات وسداد الموردين مع أرصدة المديونية القائمة."
        )
        UniversalSectionPdfReportDialog(
            payload = payload,
            onDismiss = { showDebtsPdfReportDialog = false }
        )
    }
}

@Composable
private fun UnifiedMovementsTimelineList(
    movements: List<UnifiedDebtMovementItem>,
    currencySymbol: String,
    isDark: Boolean,
    onPayCustomer: (Customer, String?) -> Unit,
    onPaySupplierInvoice: (PurchaseInvoice) -> Unit,
    onOpenCustomerStatement: (Customer) -> Unit,
    onOpenSupplierStatement: (String) -> Unit,
    onViewMovementInvoice: (UnifiedDebtMovementItem) -> Unit,
    onPrintPaymentReceipt: (UnifiedDebtMovementItem) -> Unit,
    onShareCustomerMovement: (UnifiedDebtMovementItem, String) -> Unit,
    onAddDebtClick: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    if (movements.isEmpty()) {
        EmptyDebtsBox(
            title = "لا توجد حركات ديون أو دفعات مطابقة للفلتر",
            subtitle = "يمكنك تسجيل دين جديد أو تحصيل دفعة من العملاء أو الموردين لتظهر كافة الحركات هنا فوراً.",
            actionLabel = "إضافة أول عملية دين",
            onActionClick = onAddDebtClick
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 84.dp)
    ) {
        itemsIndexed(
            items = movements,
            key = { index, item -> "${item.uniqueKey}_$index" }
        ) { _, item ->
            val isPayment = item.movementKind == "PAYMENT"
            val isCustomer = item.partyType == "CUSTOMER"
            val accentColor = if (isPayment) semantic.profitGreen else Color(0xFFEF4444)
            val dateStr = remember(item.timestampMillis) {
                SimpleDateFormat("yyyy-MM-dd • hh:mm a", Locale.US).format(Date(item.timestampMillis)).toEnglishDigits()
            }

            val badgeLabel = when {
                isCustomer && isPayment -> "تحصيل دفعة عميل"
                isCustomer && !isPayment -> "تسجيل دين عميل"
                !isCustomer && isPayment -> "سداد دفعة مورد"
                else -> "مديونية فاتورة مورد"
            }

            val canPayNow = if (isCustomer) {
                (item.customerRef?.balanceDebt ?: 0.0) > 0.009
            } else {
                (item.supplierInvoiceRef?.remainingDebt ?: 0.0) > 0.009
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                border = BorderStroke(
                    1.1.dp,
                    if (!isPayment) Color(0xFFEF4444).copy(alpha = if (isDark) 0.45f else 0.55f)
                    else semantic.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("movement_card_${item.uniqueKey}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row: Redesigned Badge on Right, Date & Time on Left
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = if (isDark) 0.20f else 0.12f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.50f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPayment) Icons.Default.CheckCircle else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = badgeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = dateStr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Middle Row: Party Name & Icon Box on Right, Amount & Remaining Balance on Left
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
                                color = accentColor.copy(alpha = if (isDark) 0.18f else 0.14f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPayment) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = badgeLabel,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.partyName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.notes,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (isPayment) "-" else "+"}${String.format(Locale.US, "%.2f", item.amount).toEnglishDigits()} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace
                            )
                            val currentRemaining = if (isCustomer) {
                                item.customerRef?.balanceDebt ?: 0.0
                            } else {
                                item.supplierInvoiceRef?.remainingDebt ?: 0.0
                            }
                            Text(
                                text = "المتبقي: ${String.format(Locale.US, "%.1f", currentRemaining).toEnglishDigits()} $currencySymbol",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentRemaining > 0.009) semantic.debtAmber else semantic.profitGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // كشف حساب Button
                        OutlinedButton(
                            onClick = {
                                if (isCustomer && item.customerRef != null) {
                                    onOpenCustomerStatement(item.customerRef)
                                } else {
                                    onOpenSupplierStatement(item.partyName)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.55f else 0.75f)
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(34.dp)
                                .testTag("movement_statement_btn_${item.uniqueKey}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "كشف حساب",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("كشف حساب", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        if (isCustomer) {
                            // زر مشاركة عبر واتساب / ماسنجر بجانب كشف حساب العميل
                            OutlinedButton(
                                onClick = { onShareCustomerMovement(item, "WHATSAPP") },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.1.dp,
                                    Color(0xFF16A34A).copy(alpha = if (isDark) 0.75f else 0.88f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1.05f)
                                    .height(34.dp)
                                    .testTag("movement_share_whatsapp_btn_${item.uniqueKey}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "مشاركة عبر واتساب أو ماسنجر",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "واتساب / ماسنجر",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF16A34A),
                                    maxLines = 1
                                )
                            }
                        }

                        if (isPayment) {
                            // طباعة وصل السداد Button (for payment movements)
                            OutlinedButton(
                                onClick = { onPrintPaymentReceipt(item) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.1.dp,
                                    semantic.profitGreen.copy(alpha = if (isDark) 0.70f else 0.88f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1.05f)
                                    .height(34.dp)
                                    .testTag("movement_print_payment_receipt_btn_${item.uniqueKey}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "طباعة وصل السداد",
                                    tint = semantic.profitGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "وصل السداد",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = semantic.profitGreen,
                                    maxLines = 1
                                )
                            }
                        } else {
                            // عرض الفاتورة Button (for debt movements)
                            OutlinedButton(
                                onClick = { onViewMovementInvoice(item) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.1.dp,
                                    semantic.debtAmber.copy(alpha = if (isDark) 0.65f else 0.85f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1.05f)
                                    .height(34.dp)
                                    .testTag("movement_view_invoice_btn_${item.uniqueKey}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = "عرض الفاتورة",
                                    tint = semantic.debtAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "عرض الفاتورة",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = semantic.debtAmber,
                                    maxLines = 1
                                )
                            }
                        }

                        if (canPayNow) {
                            Button(
                                onClick = {
                                    if (isCustomer && item.customerRef != null) {
                                        val invIdFromNotes = Regex("""#([A-Za-z0-9\-]+)""").find(item.notes)?.groupValues?.get(1)
                                        onPayCustomer(item.customerRef, invIdFromNotes)
                                    } else if (!isCustomer && item.supplierInvoiceRef != null) {
                                        onPaySupplierInvoice(item.supplierInvoiceRef)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = semantic.profitGreen),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("movement_pay_btn_${item.uniqueKey}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "تسديد دفعة",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "تسديد دفعة",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1
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
private fun CustomerDebtsTabContent(
    customers: List<Customer>,
    transactions: List<CustomerTransaction>,
    currencySymbol: String,
    isDark: Boolean,
    onPayCustomer: (Customer) -> Unit,
    onAddDebtForCustomer: (Customer) -> Unit,
    onOpenCustomerStatement: (Customer) -> Unit,
    onShareCustomerDebt: (Customer, CustomerTransaction?, String) -> Unit,
    onViewCustomerLatestInvoice: (Customer, CustomerTransaction) -> Unit,
    onCreateNewCustomerDebt: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    if (customers.isEmpty()) {
        EmptyDebtsBox(
            title = "لا يوجد عملاء مطابقون للبحث أو الفلتر",
            subtitle = "أضف مديونية عميل جديد أو عدّل خيارات الفلترة لعرض حسابات العملاء.",
            actionLabel = "إضافة دين عميل جديد",
            onActionClick = onCreateNewCustomerDebt
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 84.dp)
    ) {
        items(
            items = customers,
            key = { "debt_cust_${it.id}" }
        ) { customer ->
            val hasDebt = customer.balanceDebt > 0.009
            val custTxList = remember(transactions, customer.id) {
                transactions.filter { it.customerId == customer.id }
            }
            val totalPaidByCustomer = remember(custTxList) {
                custTxList.filter { it.type == "PAYMENT" }.sumOf { it.amount }
            }
            val latestTx = custTxList.maxByOrNull { it.date }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                border = BorderStroke(
                    1.1.dp,
                    if (hasDebt) Color(0xFFEF4444).copy(alpha = if (isDark) 0.50f else 0.65f)
                    else semantic.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_debt_card_${customer.id}")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = customer.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "الهاتف: ${customer.phone.ifBlank { "غير مسجل" }} • عدد الحركات: ${custTxList.size}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (latestTx != null) {
                                val lastDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(latestTx.date)).toEnglishDigits()
                                Text(
                                    text = "آخر حركة ($lastDate): ${latestTx.notes.ifBlank { if (latestTx.type == "PAYMENT") "سداد دفعة" else "تسجيل دين" }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasDebt) Color(0xFFEF4444).copy(alpha = 0.14f)
                            else semantic.profitGreen.copy(alpha = 0.14f),
                            border = BorderStroke(
                                1.dp,
                                if (hasDebt) Color(0xFFEF4444).copy(alpha = 0.65f)
                                else semantic.profitGreen.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (hasDebt) "مديونية مستحقة" else "خالص الرصيد",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasDebt) Color(0xFFEF4444) else semantic.profitGreen
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", customer.balanceDebt)} $currencySymbol",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (hasDebt) Color(0xFFEF4444) else semantic.profitGreen
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "إجمالي المشتريات: ${String.format(Locale.US, "%.1f", customer.totalPurchases)} $currencySymbol",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "إجمالي المسدد: ${String.format(Locale.US, "%.1f", totalPaidByCustomer)} $currencySymbol",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = semantic.profitGreen
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

                    // الصف الأول: العمليات المالية وعرض الفاتورة / وصل السداد
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // تسديد دفعة
                        Button(
                            onClick = { onPayCustomer(customer) },
                            enabled = hasDebt,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = semantic.profitGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("pay_customer_debt_btn_${customer.id}")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("تسديد دفعة", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }

                        // + إضافة دين
                        OutlinedButton(
                            onClick = { onAddDebtForCustomer(customer) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.1.dp,
                                Color(0xFFEF4444).copy(alpha = if (isDark) 0.65f else 0.80f)
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("add_customer_debt_btn_${customer.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "إضافة دين",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                maxLines = 1
                            )
                        }

                        // عرض الفاتورة / وصل السداد (إذا توفرت حركة)
                        if (latestTx != null) {
                            val isLatestPay = latestTx.type == "PAYMENT"
                            val btnColor = if (isLatestPay) semantic.profitGreen else semantic.debtAmber
                            OutlinedButton(
                                onClick = { onViewCustomerLatestInvoice(customer, latestTx) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.1.dp,
                                    btnColor.copy(alpha = if (isDark) 0.65f else 0.85f)
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1.05f)
                                    .height(36.dp)
                                    .testTag("customer_view_invoice_btn_${customer.id}")
                            ) {
                                Icon(
                                    imageVector = if (isLatestPay) Icons.Default.Print else Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = btnColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isLatestPay) "وصل السداد" else "عرض الفاتورة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = btnColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // الصف الثاني: كشف حساب العميل + زر مشاركة عبر واتساب + زر ماسنجر جنباً إلى جنب
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // كشف حساب
                        OutlinedButton(
                            onClick = { onOpenCustomerStatement(customer) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.65f else 0.80f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("customer_statement_btn_${customer.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "كشف حساب",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "كشف حساب",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        // مشاركة عبر واتساب (بجانب كشف حساب العميل مباشرة)
                        OutlinedButton(
                            onClick = { onShareCustomerDebt(customer, latestTx, "WHATSAPP") },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.1.dp,
                                Color(0xFF16A34A).copy(alpha = if (isDark) 0.75f else 0.88f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .height(36.dp)
                                .testTag("customer_share_whatsapp_btn_${customer.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة عبر واتساب",
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مشاركة عبر واتساب",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16A34A),
                                maxLines = 1
                            )
                        }

                        // مشاركة عبر الماسنجر
                        OutlinedButton(
                            onClick = { onShareCustomerDebt(customer, latestTx, "MESSENGER") },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.1.dp,
                                Color(0xFF0084FF).copy(alpha = if (isDark) 0.75f else 0.88f)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(36.dp)
                                .testTag("customer_share_messenger_btn_${customer.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة عبر ماسنجر",
                                tint = Color(0xFF0084FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ماسنجر",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0084FF),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierDebtsTabContent(
    invoices: List<PurchaseInvoice>,
    currencySymbol: String,
    isDark: Boolean,
    onPaySupplierInvoice: (PurchaseInvoice) -> Unit,
    onAddSupplierDebt: (String, String) -> Unit,
    onOpenSupplierStatement: (String) -> Unit,
    onViewSupplierInvoice: (PurchaseInvoice) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    if (invoices.isEmpty()) {
        EmptyDebtsBox(
            title = "لا توجد مديونيات أو فواتير موردين مطابقة",
            subtitle = "يمكنك إضافة مديونية مورد جديدة أو تسجيل فاتورة مشتريات آجلة لمتابعة مستحقات الموردين.",
            actionLabel = "إضافة مديونية مورد",
            onActionClick = { onAddSupplierDebt("", "") }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 84.dp)
    ) {
        items(
            items = invoices,
            key = { "sup_debt_inv_${it.id}" }
        ) { inv ->
            val hasDebt = inv.remainingDebt > 0.009
            val dateStr = remember(inv.dateMillis) {
                SimpleDateFormat("yyyy-MM-dd • hh:mm a", Locale.US).format(Date(inv.dateMillis)).toEnglishDigits()
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                border = BorderStroke(
                    1.1.dp,
                    if (hasDebt) semantic.debtAmber.copy(alpha = if (isDark) 0.55f else 0.72f)
                    else semantic.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supplier_debt_card_${inv.id}")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "المورد: ${inv.supplierName}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "فاتورة: ${inv.invoiceNumber} • الهاتف: ${inv.supplierPhone.ifBlank { "غير مسجل" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$dateStr • ${inv.notes.ifBlank { "توريد بضائع" }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasDebt) semantic.debtAmber.copy(alpha = 0.15f)
                            else semantic.profitGreen.copy(alpha = 0.15f),
                            border = BorderStroke(
                                1.dp,
                                if (hasDebt) semantic.debtAmber.copy(alpha = 0.70f)
                                else semantic.profitGreen.copy(alpha = 0.70f)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (hasDebt) "آجل للمورد" else "مسددة بالكامل",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasDebt) semantic.debtAmber else semantic.profitGreen
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", inv.remainingDebt)} $currencySymbol",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (hasDebt) semantic.debtAmber else semantic.profitGreen
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "إجمالي الفاتورة: ${String.format(Locale.US, "%.2f", inv.totalCost)} $currencySymbol",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "المدفوع للمورد: ${String.format(Locale.US, "%.2f", inv.paidAmount)} $currencySymbol",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = semantic.profitGreen
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // تسديد دفعة للمورد
                        Button(
                            onClick = { onPaySupplierInvoice(inv) },
                            enabled = hasDebt,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = semantic.profitGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("pay_supplier_debt_btn_${inv.id}")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("تسديد دفعة", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }

                        // عرض الفاتورة
                        OutlinedButton(
                            onClick = { onViewSupplierInvoice(inv) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.1.dp,
                                semantic.debtAmber.copy(alpha = if (isDark) 0.65f else 0.85f)
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1.05f)
                                .height(36.dp)
                                .testTag("supplier_view_invoice_btn_${inv.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = semantic.debtAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "عرض الفاتورة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = semantic.debtAmber,
                                maxLines = 1
                            )
                        }

                        // + إضافة دين لنفس المورد
                        OutlinedButton(
                            onClick = { onAddSupplierDebt(inv.supplierName, inv.supplierPhone) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.1.dp,
                                Color(0xFFEF4444).copy(alpha = if (isDark) 0.60f else 0.78f)
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "إضافة دين",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444),
                                maxLines = 1
                            )
                        }

                        // كشف حساب المورد
                        OutlinedButton(
                            onClick = { onOpenSupplierStatement(inv.supplierName) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.65f else 0.85f)
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(36.dp)
                                .testTag("supplier_statement_btn_${inv.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "كشف حساب",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDebtsBox(
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
            border = BorderStroke(1.dp, semantic.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickPayPartyPickerDialog(
    customers: List<Customer>,
    supplierInvoices: List<PurchaseInvoice>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onSelectSupplierInvoice: (PurchaseInvoice) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var partyTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختر الحساب لتسديد دفعة فورية",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { partyTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (partyTab == 0) MaterialTheme.colorScheme.primary else semantic.cardSubtle,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "عملاء مدينون (${customers.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (partyTab == 0) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Surface(
                        onClick = { partyTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (partyTab == 1) MaterialTheme.colorScheme.primary else semantic.cardSubtle,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "موردون (${supplierInvoices.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (partyTab == 1) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (partyTab == 0) {
                        if (customers.isEmpty()) {
                            Text(
                                text = "لا توجد مديونيات نشطة على العملاء حالياً.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            customers.forEach { cust ->
                                Surface(
                                    onClick = { onSelectCustomer(cust) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = semantic.cardSubtle,
                                    border = BorderStroke(1.dp, semantic.cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (cust.phone.isNotBlank()) {
                                                Text(cust.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", cust.balanceDebt)} $currencySymbol",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (supplierInvoices.isEmpty()) {
                            Text(
                                text = "لا توجد فواتير موردين آجلة غير مسددة حالياً.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            supplierInvoices.forEach { inv ->
                                Surface(
                                    onClick = { onSelectSupplierInvoice(inv) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = semantic.cardSubtle,
                                    border = BorderStroke(1.dp, semantic.cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(inv.supplierName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(inv.invoiceNumber, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", inv.remainingDebt)} $currencySymbol",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = semantic.debtAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun AddDebtOperationDialog(
    customers: List<Customer>,
    initialPartyType: String,
    preselectedCustomer: Customer?,
    preselectedSupplierName: String,
    preselectedSupplierPhone: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onAddExistingCustomerDebt: (Customer, Double, String) -> Unit,
    onCreateNewCustomerWithDebt: (String, String, Double, String) -> Unit,
    onAddSupplierDebt: (String, String, Double, String) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var partyType by remember { mutableStateOf(initialPartyType) } // "CUSTOMER" or "SUPPLIER"
    var isNewCustomerMode by remember {
        mutableStateOf(preselectedCustomer == null && customers.isEmpty())
    }
    var selectedCustomer by remember {
        mutableStateOf(preselectedCustomer ?: customers.firstOrNull())
    }

    var newCustomerName by remember { mutableStateOf("") }
    var newCustomerPhone by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf(preselectedSupplierName) }
    var supplierPhone by remember { mutableStateOf(preselectedSupplierPhone) }
    var amountInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (partyType == "CUSTOMER") "تسجيل مديونية جديدة على عميل" else "تسجيل مديونية جديدة لمورد",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.5.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Party Type Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { partyType = "CUSTOMER" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (partyType == "CUSTOMER") MaterialTheme.colorScheme.primary else semantic.cardSubtle,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "دين على عميل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (partyType == "CUSTOMER") Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = { partyType = "SUPPLIER" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (partyType == "SUPPLIER") MaterialTheme.colorScheme.primary else semantic.cardSubtle,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "دين لمورد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (partyType == "SUPPLIER") Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (partyType == "CUSTOMER") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNewCustomerMode) "بيانات العميل الجديد:" else "اختر العميل:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (customers.isNotEmpty()) {
                            TextButton(onClick = { isNewCustomerMode = !isNewCustomerMode }) {
                                Icon(
                                    imageVector = if (isNewCustomerMode) Icons.Default.Group else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isNewCustomerMode) "اختيار عميل حالي" else "+ عميل جديد",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isNewCustomerMode) {
                        OutlinedTextField(
                            value = newCustomerName,
                            onValueChange = { newCustomerName = it },
                            label = { Text("اسم العميل *") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_debt_new_customer_name_input")
                        )
                        OutlinedTextField(
                            value = newCustomerPhone,
                            onValueChange = { newCustomerPhone = it },
                            label = { Text("رقم الهاتف (اختياري)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            customers.forEach { cust ->
                                val isSelected = selectedCustomer?.id == cust.id
                                Surface(
                                    onClick = { selectedCustomer = cust },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else semantic.cardSubtle,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else semantic.cardBorder
                                    )
                                ) {
                                    Text(
                                        text = "${cust.name} (${String.format(Locale.US, "%.0f", cust.balanceDebt)})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("اسم المورد / الشركة *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_debt_supplier_name_input")
                    )
                    OutlinedTextField(
                        value = supplierPhone,
                        onValueChange = { supplierPhone = it },
                        label = { Text("رقم هاتف المورد (اختياري)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("مبلغ المديونية ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_debt_amount_input")
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("البيان / سبب المديونية (مثال: بضاعة آجلة)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_debt_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0) return@Button
                    val notes = notesInput.trim().ifBlank { "تسجيل مديونية من نافذة الديون وحركتها" }
                    if (partyType == "CUSTOMER") {
                        if (isNewCustomerMode) {
                            if (newCustomerName.isNotBlank()) {
                                onCreateNewCustomerWithDebt(newCustomerName, newCustomerPhone, amount, notes)
                            }
                        } else {
                            selectedCustomer?.let { cust ->
                                onAddExistingCustomerDebt(cust, amount, notes)
                            }
                        }
                    } else {
                        if (supplierName.isNotBlank()) {
                            onAddSupplierDebt(supplierName, supplierPhone, amount, notes)
                        }
                    }
                },
                modifier = Modifier.testTag("confirm_add_debt_btn")
            ) {
                Text("حفظ المديونية", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

private data class CustomerDebtInvoiceOption(
    val invoiceId: String,
    val amount: Double,
    val dateMillis: Long,
    val description: String,
    val source: String // "ARCHIVED_INVOICE" or "TRANSACTION"
)

@Composable
private fun CustomerDebtPaymentDialog(
    customer: Customer,
    currencySymbol: String,
    archivedInvoices: List<ArchivedInvoice> = emptyList(),
    customerTransactions: List<CustomerTransaction> = emptyList(),
    initialInvoiceId: String? = null,
    onDismiss: () -> Unit,
    onConfirmPayment: (Double, String, String?) -> Unit,
    onConfirmAndPrintReceipt: (Double, String, String?) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    // Discover debt invoices for this specific customer
    val invoiceOptions = remember(customer.id, customer.name, archivedInvoices, customerTransactions) {
        val options = mutableListOf<CustomerDebtInvoiceOption>()
        val seenInvoiceIds = mutableSetOf<String>()

        // 1. From archived invoices (isDebt = true, or paymentMethod containing debt)
        archivedInvoices
            .filter { inv ->
                val matchesCust = (inv.customerId != null && inv.customerId == customer.id) ||
                    (inv.customerName.trim().isNotBlank() && inv.customerName.trim().equals(customer.name.trim(), ignoreCase = true))
                matchesCust && (inv.isDebt || inv.paymentMethod.contains("دين", ignoreCase = true) || inv.paymentMethod.contains("آجل", ignoreCase = true))
            }
            .sortedByDescending { it.dateMillis }
            .forEach { inv ->
                if (seenInvoiceIds.add(inv.invoiceId)) {
                    val desc = if (inv.items.isNotEmpty()) {
                        "${inv.items.size} أصناف • إجمالي الفاتورة"
                    } else {
                        "فاتورة مبيعات آجل"
                    }
                    options.add(
                        CustomerDebtInvoiceOption(
                            invoiceId = inv.invoiceId,
                            amount = inv.subtotal,
                            dateMillis = inv.dateMillis,
                            description = desc,
                            source = "ARCHIVED_INVOICE"
                        )
                    )
                }
            }

        // 2. From customer transactions (DEBT type where notes mention invoice #)
        val invoiceRegex = Regex("""#([A-Za-z0-9\-]+)""")
        customerTransactions
            .filter { tx -> tx.customerId == customer.id && tx.type != "PAYMENT" }
            .sortedByDescending { tx -> tx.date }
            .forEach { tx ->
                val match = invoiceRegex.find(tx.notes)
                if (match != null) {
                    val invId = match.groupValues[1]
                    if (seenInvoiceIds.add(invId)) {
                        options.add(
                            CustomerDebtInvoiceOption(
                                invoiceId = invId,
                                amount = tx.amount,
                                dateMillis = tx.date,
                                description = tx.notes,
                                source = "TRANSACTION"
                            )
                        )
                    }
                }
            }

        options
    }

    var selectedInvoice by remember {
        mutableStateOf<CustomerDebtInvoiceOption?>(
            if (initialInvoiceId != null) {
                invoiceOptions.find { it.invoiceId == initialInvoiceId }
            } else null
        )
    }

    var manualInvoiceNumber by remember(selectedInvoice, initialInvoiceId) {
        mutableStateOf(selectedInvoice?.invoiceId ?: initialInvoiceId ?: "")
    }

    var amountInput by remember(customer.balanceDebt, selectedInvoice) {
        mutableStateOf(
            if (selectedInvoice != null) {
                val candidateAmt = if (customer.balanceDebt > 0) {
                    selectedInvoice!!.amount.coerceAtMost(customer.balanceDebt)
                } else {
                    selectedInvoice!!.amount
                }
                String.format(Locale.US, "%.2f", candidateAmt)
            } else if (customer.balanceDebt > 0) {
                String.format(Locale.US, "%.2f", customer.balanceDebt)
            } else {
                ""
            }
        )
    }

    var notesInput by remember(selectedInvoice) {
        mutableStateOf(
            if (selectedInvoice != null) {
                val invCode = normalizeDebtInvoiceReference(selectedInvoice!!.invoiceId)
                "سداد فاتورة مبيعات \u202A\u200E$invCode\u200E\u202C"
            } else {
                "تحصيل دفعة نقدية من حساب المديونية"
            }
        )
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    val enteredAmount = amountInput.toDoubleOrNull() ?: 0.0
    val remainingAfterPayment = (customer.balanceDebt - enteredAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسديد دفعة من العميل: ${customer.name}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الرصيد السابق المستحق:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", customer.balanceDebt)} $currencySymbol",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                // 2. Select Invoice to Pay Dropdown (ExposedDropdownMenuBox)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تحديد الفاتورة المسددة (اختياري):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedInvoice != null || manualInvoiceNumber.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    selectedInvoice = null
                                    manualInvoiceNumber = ""
                                    notesInput = "تحصيل دفعة نقدية من حساب المديونية"
                                    if (customer.balanceDebt > 0) {
                                        amountInput = String.format(Locale.US, "%.2f", customer.balanceDebt)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("إلغاء التحديد", fontSize = 11.sp, color = semantic.debtAmber)
                            }
                        }
                    }

                    // Custom Expandable Invoice Selection Box
                    val selectedTitle = selectedInvoice?.let {
                        val invCode = normalizeDebtInvoiceReference(it.invoiceId)
                        "فاتورة \u202A\u200E$invCode\u200E\u202C (${String.format(Locale.US, "%.2f", it.amount)} $currencySymbol)"
                    } ?: if (invoiceOptions.isEmpty()) "لا توجد فواتير آجلة مسجلة للعميل" else "دفعة عامة لحساب المديونية (غير مرتبطة بفاتورة)"

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selectedInvoice != null) semantic.profitGreen else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = invoiceOptions.isNotEmpty()) {
                                isDropdownExpanded = !isDropdownExpanded
                            }
                            .testTag("select_paid_invoice_dropdown")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
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
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (selectedInvoice != null) semantic.profitGreen else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "الفاتورة المراد سدادها:",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = selectedTitle,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedInvoice != null) semantic.profitGreen else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (invoiceOptions.isNotEmpty()) {
                                    Icon(
                                        imageVector = if (isDropdownExpanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (isDropdownExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // Option 1: General payment (no invoice)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedInvoice == null) semantic.profitGreen.copy(alpha = 0.12f) else Color.Transparent,
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (selectedInvoice == null) semantic.profitGreen else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedInvoice = null
                                            notesInput = "تحصيل دفعة نقدية من حساب المديونية"
                                            if (customer.balanceDebt > 0) {
                                                amountInput = String.format(Locale.US, "%.2f", customer.balanceDebt)
                                            }
                                            isDropdownExpanded = false
                                        }
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(4.dp),
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
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "دفعة عامة لحساب المديونية",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "تسديد من إجمالي حساب العميل دون ربطه بفاتورة محددة",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (selectedInvoice == null) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = semantic.profitGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                invoiceOptions.forEach { opt ->
                                    val isCurSelected = selectedInvoice?.invoiceId == opt.invoiceId
                                    val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(opt.dateMillis))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurSelected) semantic.profitGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(
                                            0.8.dp,
                                            if (isCurSelected) semantic.profitGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .clickable {
                                                selectedInvoice = opt
                                                manualInvoiceNumber = opt.invoiceId
                                                val fillAmt = if (customer.balanceDebt > 0) {
                                                    opt.amount.coerceAtMost(customer.balanceDebt)
                                                } else {
                                                    opt.amount
                                                }
                                                amountInput = String.format(Locale.US, "%.2f", fillAmt)
                                                val invCode = normalizeDebtInvoiceReference(opt.invoiceId)
                                                notesInput = "سداد فاتورة مبيعات \u202A\u200E$invCode\u200E\u202C"
                                                isDropdownExpanded = false
                                            }
                                            .padding(vertical = 4.dp, horizontal = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                                    contentDescription = null,
                                                    tint = semantic.debtAmber,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    val invCode = normalizeDebtInvoiceReference(opt.invoiceId)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "فاتورة مبيعات ",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                            Text(
                                                                text = invCode,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "$dateStr • ${opt.description}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = semantic.debtAmber.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.8.dp, semantic.debtAmber.copy(alpha = 0.6f))
                                                ) {
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.2f", opt.amount)} $currencySymbol",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.5.sp,
                                                        color = semantic.debtAmber,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (isCurSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = semantic.profitGreen,
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

                // Quick payment ratio buttons
                if (customer.balanceDebt > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                amountInput = String.format(Locale.US, "%.2f", customer.balanceDebt)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("كامل المديونية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                amountInput = String.format(Locale.US, "%.2f", customer.balanceDebt / 2.0)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("نصف المبلغ (50%)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = manualInvoiceNumber,
                    onValueChange = { newVal ->
                        manualInvoiceNumber = newVal
                        val cleanId = newVal.trim().removePrefix("#")
                        val matched = invoiceOptions.find {
                            it.invoiceId.equals(cleanId, ignoreCase = true) ||
                                it.invoiceId.equals(newVal.trim(), ignoreCase = true)
                        }
                        if (matched != null) {
                            selectedInvoice = matched
                            val invCode = normalizeDebtInvoiceReference(matched.invoiceId)
                            notesInput = "سداد فاتورة مبيعات \u202A\u200E$invCode\u200E\u202C"
                            val fillAmt = if (customer.balanceDebt > 0) matched.amount.coerceAtMost(customer.balanceDebt) else matched.amount
                            amountInput = String.format(Locale.US, "%.2f", fillAmt)
                        } else if (newVal.isNotBlank() && selectedInvoice == null) {
                            val invCode = normalizeDebtInvoiceReference(cleanId)
                            notesInput = "سداد فاتورة مبيعات \u202A\u200E$invCode\u200E\u202C"
                        }
                    },
                    label = { Text("رقم الفاتورة المراد تسديد الدفعة منها (اختياري)") },
                    placeholder = { Text("مثال: #2-280726 أو 1024") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = if (manualInvoiceNumber.isNotBlank()) semantic.profitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (manualInvoiceNumber.isNotBlank()) {
                            IconButton(onClick = {
                                manualInvoiceNumber = ""
                                selectedInvoice = null
                                notesInput = "تحصيل دفعة نقدية من حساب المديونية"
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_pay_invoice_ref_input")
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("المبلغ المحصل ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_pay_amount_input")
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("ملاحظات وبيان التحصيل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Balance Calculation Summary for Receipt
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semantic.profitGreen.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
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
                            Text("المبلغ المسدد الآن:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.2f", enteredAmount)} $currencySymbol",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.profitGreen
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الرصيد المتبقي بعد السداد:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.2f", remainingAfterPayment)} $currencySymbol",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (remainingAfterPayment > 0.009) semantic.debtAmber else semantic.profitGreen
                            )
                        }
                        val effectiveInvDisplay = manualInvoiceNumber.trim().takeIf { it.isNotBlank() } ?: selectedInvoice?.invoiceId
                        if (!effectiveInvDisplay.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الفاتورة المحددة للسداد:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val invCode = normalizeDebtInvoiceReference(effectiveInvDisplay)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = invCode,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.profitGreen,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val rawTarget = manualInvoiceNumber.trim().takeIf { it.isNotBlank() } ?: selectedInvoice?.invoiceId
            val effectiveInvToPass = normalizeDebtInvoiceReference(rawTarget).takeIf { it.isNotBlank() }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onConfirmPayment(amt, notesInput.trim(), effectiveInvToPass)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.1.dp, semantic.profitGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("confirm_customer_pay_btn")
                ) {
                    Text(
                        text = "تحصيل فقط",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = semantic.profitGreen
                    )
                }

                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onConfirmAndPrintReceipt(amt, notesInput.trim(), effectiveInvToPass)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = semantic.profitGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("confirm_and_print_customer_pay_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تحصيل وطباعة وصل",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
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
private fun SupplierDebtPaymentDialog(
    invoice: PurchaseInvoice,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmPayment: (Double) -> Unit,
    onConfirmAndPrintReceipt: (Double) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var amountInput by remember(invoice.remainingDebt) {
        mutableStateOf(
            if (invoice.remainingDebt > 0) String.format(Locale.US, "%.2f", invoice.remainingDebt) else ""
        )
    }

    val enteredAmount = amountInput.toDoubleOrNull() ?: 0.0
    val remainingAfterPayment = (invoice.remainingDebt - enteredAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "سداد دفعة للمورد: ${invoice.supplierName}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "فاتورة رقم: ",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = normalizeDebtInvoiceReference(invoice.invoiceNumber),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semantic.debtAmber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الرصيد السابق للمورد:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "${String.format(Locale.US, "%.2f", invoice.remainingDebt)} $currencySymbol",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = semantic.debtAmber
                        )
                    }
                }

                if (invoice.remainingDebt > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                amountInput = String.format(Locale.US, "%.2f", invoice.remainingDebt)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("سداد الكامل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                amountInput = String.format(Locale.US, "%.2f", invoice.remainingDebt / 2.0)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("نصف المبلغ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("مبلغ الدفعة ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_pay_amount_input")
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semantic.profitGreen.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
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
                            Text("المبلغ المسدد للمورد:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.2f", enteredAmount)} $currencySymbol",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = semantic.profitGreen
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المتبقي للمورد بعد السداد:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.2f", remainingAfterPayment)} $currencySymbol",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (remainingAfterPayment > 0.009) semantic.debtAmber else semantic.profitGreen
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onConfirmPayment(amt)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.1.dp, semantic.profitGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("confirm_supplier_pay_btn")
                ) {
                    Text(
                        text = "سداد فقط",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = semantic.profitGreen
                    )
                }

                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onConfirmAndPrintReceipt(amt)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = semantic.profitGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("confirm_and_print_supplier_pay_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "سداد وطباعة وصل",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
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
private fun CustomerAccountStatementDialog(
    customer: Customer,
    transactions: List<CustomerTransaction>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String,
    context: Context,
    onDismiss: () -> Unit,
    onPayNow: () -> Unit,
    onAddDebtNow: () -> Unit,
    onViewTransactionInvoice: (CustomerTransaction) -> Unit,
    onPrintPaymentReceipt: (CustomerTransaction) -> Unit,
    onShareTransaction: (CustomerTransaction, String) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val totalDebtsAdded = remember(transactions) {
        transactions.filter { it.type != "PAYMENT" }.sumOf { it.amount }
    }
    val totalPaymentsPaid = remember(transactions) {
        transactions.filter { it.type == "PAYMENT" }.sumOf { it.amount }
    }

    val statementText = remember(customer, transactions, storeName, storePhone, currencySymbol, footerText) {
        buildCustomerStatementShareMessage(
            customer = customer,
            transactions = transactions,
            storeName = storeName,
            storePhone = storePhone,
            currencySymbol = currencySymbol,
            footerText = footerText
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, semantic.cardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 700.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
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
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "كشف حساب: ${customer.name}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "الهاتف: ${customer.phone.ifBlank { "غير مسجل" }} • الحركات: ${transactions.size}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                shareCustomerStatementPdf(
                                    context = context,
                                    customer = customer,
                                    transactions = transactions,
                                    storeName = storeName,
                                    storePhone = storePhone,
                                    currencySymbol = currencySymbol,
                                    footerText = footerText
                                )
                            },
                            modifier = Modifier.testTag("stmt_header_share_pdf_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "حفظ وإرسال PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                printCustomerStatementViaSystemPrintManager(
                                    context = context,
                                    customer = customer,
                                    transactions = transactions,
                                    storeName = storeName,
                                    storePhone = storePhone,
                                    currencySymbol = currencySymbol,
                                    footerText = footerText
                                )
                            },
                            modifier = Modifier.testTag("stmt_header_export_pdf_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "تصدير / طباعة PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                }

                // Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي الديون", fontSize = 10.5.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalDebtsAdded)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.profitGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي المسدد", fontSize = 10.5.sp, color = semantic.profitGreen, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalPaymentsPaid)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("الرصيد المتبقي", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", customer.balanceDebt)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (customer.balanceDebt > 0) Color(0xFFEF4444) else semantic.profitGreen
                            )
                        }
                    }
                }

                HorizontalDivider(color = semantic.cardBorder)

                // Movements List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (transactions.isEmpty()) {
                        item {
                            Text(
                                text = "لا توجد حركات مسجلة لهذا العميل حتى الآن.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    } else {
                        items(transactions, key = { "stmt_tx_${it.id}" }) { tx ->
                            val isPay = tx.type == "PAYMENT"
                            val dStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date(tx.date)).toEnglishDigits()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = semantic.cardSubtle,
                                border = BorderStroke(1.dp, semantic.cardBorder),
                                modifier = Modifier.fillMaxWidth()
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isPay) "✓ تسديد دفعة" else "+ إضافة مديونية",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isPay) semantic.profitGreen else Color(0xFFEF4444)
                                            )
                                            Text(
                                                text = tx.notes.ifBlank { "بدون ملاحظات" },
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = dStr,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${if (isPay) "-" else "+"}${String.format(Locale.US, "%.2f", tx.amount)} $currencySymbol",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = if (isPay) semantic.profitGreen else Color(0xFFEF4444)
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { onShareTransaction(tx, "WHATSAPP") },
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.85f)),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier
                                                        .height(28.dp)
                                                        .testTag("stmt_tx_share_${tx.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "مشاركة عبر واتساب أو ماسنجر",
                                                        tint = Color(0xFF16A34A),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "مشاركة",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF16A34A)
                                                    )
                                                }

                                                if (isPay) {
                                                    OutlinedButton(
                                                        onClick = { onPrintPaymentReceipt(tx) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.85f)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier
                                                            .height(28.dp)
                                                            .testTag("stmt_tx_print_receipt_${tx.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Print,
                                                            contentDescription = "طباعة وصل السداد",
                                                            tint = semantic.profitGreen,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "وصل السداد",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = semantic.profitGreen
                                                        )
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = { onViewTransactionInvoice(tx) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.85f)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier
                                                            .height(28.dp)
                                                            .testTag("stmt_tx_view_invoice_${tx.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                                            contentDescription = "عرض الفاتورة",
                                                            tint = semantic.debtAmber,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "عرض الفاتورة",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = semantic.debtAmber
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

                HorizontalDivider(color = semantic.cardBorder)

                // Row 1: Dual PDF Actions (حفظ وإرسال ملف PDF + طباعة A4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shareCustomerStatementPdf(
                                context = context,
                                customer = customer,
                                transactions = transactions,
                                storeName = storeName,
                                storePhone = storePhone,
                                currencySymbol = currencySymbol,
                                footerText = footerText
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("stmt_export_pdf_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ وإرسال ملف PDF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            printCustomerStatementViaSystemPrintManager(
                                context = context,
                                customer = customer,
                                transactions = transactions,
                                storeName = storeName,
                                storePhone = storePhone,
                                currencySymbol = currencySymbol,
                                footerText = footerText
                            )
                        },
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(44.dp)
                            .testTag("stmt_print_spooler_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "طباعة A4",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Row 2: Instant Sending & Action Channels (واتساب + ماسنجر + مشاركة عامة + نسخ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Button(
                        onClick = {
                            shareTextViaWhatsApp(
                                context = context,
                                phone = customer.phone,
                                text = statementText
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.05f)
                            .height(38.dp)
                            .testTag("stmt_share_whatsapp_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("واتساب", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            shareTextViaMessenger(
                                context = context,
                                text = statementText
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084FF)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.05f)
                            .height(38.dp)
                            .testTag("stmt_share_messenger_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("ماسنجر", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, statementText)
                                putExtra(Intent.EXTRA_SUBJECT, "كشف حساب عميل: ${customer.name}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "إرسال كشف حساب العميل"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(38.dp)
                            .testTag("stmt_share_all_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("مشاركة", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("كشف حساب", statementText))
                            Toast.makeText(context, "تم نسخ كشف الحساب للحافظة ✅", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .height(38.dp)
                            .testTag("stmt_copy_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("نسخ", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (customer.balanceDebt > 0.009) {
                        Button(
                            onClick = onPayNow,
                            colors = ButtonDefaults.buttonColors(containerColor = semantic.profitGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسديد دفعة الآن", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Button(
                        onClick = onAddDebtNow,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة دين جديد", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierAccountStatementDialog(
    supplierName: String,
    invoices: List<PurchaseInvoice>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String,
    context: Context,
    onDismiss: () -> Unit,
    onPayInvoice: (PurchaseInvoice) -> Unit,
    onViewInvoice: (PurchaseInvoice) -> Unit,
    onPrintPaymentReceipt: (PurchaseInvoice) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val totalPurchases = remember(invoices) { invoices.sumOf { it.totalCost } }
    val totalPaid = remember(invoices) { invoices.sumOf { it.paidAmount } }
    val totalRemaining = remember(invoices) { invoices.sumOf { it.remainingDebt } }
    val supplierPhone = remember(invoices) {
        invoices.firstOrNull { it.supplierPhone.isNotBlank() }?.supplierPhone.orEmpty()
    }

    val statementText = remember(supplierName, invoices, storeName, currencySymbol) {
        buildString {
            appendLine("📋 *كشف حساب مورد — $storeName*")
            appendLine("المورد: $supplierName")
            if (supplierPhone.isNotBlank()) appendLine("الهاتف: $supplierPhone")
            appendLine("────────────────────")
            appendLine("إجمالي الفواتير: ${String.format(Locale.US, "%.2f", totalPurchases)} $currencySymbol")
            appendLine("إجمالي المدفوع: ${String.format(Locale.US, "%.2f", totalPaid)} $currencySymbol")
            appendLine("*الرصيد المتبقي للمورد: ${String.format(Locale.US, "%.2f", totalRemaining)} $currencySymbol*")
            appendLine("────────────────────")
            invoices.forEach { inv ->
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(inv.dateMillis)).toEnglishDigits()
                appendLine("• [${inv.invoiceNumber} - $d] الإجمالي: ${inv.totalCost} | المدفوع: ${inv.paidAmount} | المتبقي: ${inv.remainingDebt} $currencySymbol")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, semantic.cardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "كشف حساب المورد: $supplierName",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "الهاتف: ${supplierPhone.ifBlank { "غير مسجل" }} • الفواتير: ${invoices.size}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                printSupplierStatementViaSystemPrintManager(
                                    context = context,
                                    supplierName = supplierName,
                                    supplierPhone = supplierPhone,
                                    invoices = invoices,
                                    storeName = storeName,
                                    storePhone = storePhone,
                                    currencySymbol = currencySymbol,
                                    footerText = footerText
                                )
                            },
                            modifier = Modifier.testTag("sup_stmt_header_export_pdf_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "تصدير / طباعة PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.cardSubtle,
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي التوريد", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Text("${String.format(Locale.US, "%.1f", totalPurchases)} $currencySymbol", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.profitGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("المدفوع", fontSize = 10.5.sp, color = semantic.profitGreen, fontWeight = FontWeight.Bold)
                            Text("${String.format(Locale.US, "%.1f", totalPaid)} $currencySymbol", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.debtAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.65f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("المتبقي للمورد", fontSize = 10.5.sp, color = semantic.debtAmber, fontWeight = FontWeight.Bold)
                            Text("${String.format(Locale.US, "%.1f", totalRemaining)} $currencySymbol", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }

                HorizontalDivider(color = semantic.cardBorder)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(invoices, key = { "sup_stmt_${it.id}" }) { inv ->
                        val dStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(inv.dateMillis)).toEnglishDigits()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = semantic.cardSubtle,
                            border = BorderStroke(1.dp, semantic.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${inv.invoiceNumber} • $dStr", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "الإجمالي: ${inv.totalCost} | المدفوع: ${inv.paidAmount} $currencySymbol",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "المتبقي: ${String.format(Locale.US, "%.2f", inv.remainingDebt)} $currencySymbol",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (inv.remainingDebt > 0.009) semantic.debtAmber else semantic.profitGreen
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onViewInvoice(inv) },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.85f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("sup_stmt_view_invoice_${inv.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                            contentDescription = "عرض الفاتورة",
                                            tint = semantic.debtAmber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "عرض الفاتورة",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = semantic.debtAmber
                                        )
                                    }

                                    if (inv.paidAmount > 0.009) {
                                        OutlinedButton(
                                            onClick = { onPrintPaymentReceipt(inv) },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.85f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .height(28.dp)
                                                .testTag("sup_stmt_print_receipt_${inv.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Print,
                                                contentDescription = "وصل السداد",
                                                tint = semantic.profitGreen,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "وصل السداد",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = semantic.profitGreen
                                            )
                                        }
                                    }

                                    if (inv.remainingDebt > 0.009) {
                                        Button(
                                            onClick = { onPayInvoice(inv) },
                                            colors = ButtonDefaults.buttonColors(containerColor = semantic.profitGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("سداد دفعة", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Primary PDF Export Button for Supplier Statement
                Button(
                    onClick = {
                        printSupplierStatementViaSystemPrintManager(
                            context = context,
                            supplierName = supplierName,
                            supplierPhone = supplierPhone,
                            invoices = invoices,
                            storeName = storeName,
                            storePhone = storePhone,
                            currencySymbol = currencySymbol,
                            footerText = footerText
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("sup_stmt_export_pdf_btn")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تصدير / طباعة كشف حساب المورد (PDF)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareTextViaWhatsApp(
                                context = context,
                                phone = supplierPhone,
                                text = statementText
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sup_stmt_share_whatsapp_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("واتساب", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("كشف حساب مورد", statementText))
                            Toast.makeText(context, "تم نسخ كشف حساب المورد ✅", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sup_stmt_copy_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ الكشف", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(0.75f)
                    ) {
                        Text("إغلاق", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierInvoiceDetailsPreviewDialog(
    invoice: PurchaseInvoice,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onPayDebt: () -> Unit,
    onOpenPrintTemplate: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val items = remember(invoice.itemsJson) { invoice.getItems() }
    val dateStr = remember(invoice.dateMillis) {
        SimpleDateFormat("yyyy-MM-dd • hh:mm a", Locale.US).format(Date(invoice.dateMillis)).toEnglishDigits()
    }
    val hasDebt = invoice.remainingDebt > 0.009

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, semantic.cardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 660.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = semantic.debtAmber.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, semantic.debtAmber),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = semantic.debtAmber
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "فاتورة مورد: ${invoice.invoiceNumber}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "المورد: ${invoice.supplierName} • $dateStr",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Financial Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.cardSubtle,
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي الفاتورة", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.2f", invoice.totalCost)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.profitGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("المدفوع", fontSize = 10.5.sp, color = semantic.profitGreen, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.2f", invoice.paidAmount)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = semantic.profitGreen
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasDebt) semantic.debtAmber.copy(alpha = 0.15f) else semantic.profitGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (hasDebt) semantic.debtAmber else semantic.profitGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("المتبقي للمورد", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${String.format(Locale.US, "%.2f", invoice.remainingDebt)} $currencySymbol",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (hasDebt) semantic.debtAmber else semantic.profitGreen
                            )
                        }
                    }
                }

                if (invoice.notes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.cardSubtle,
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "البيان / الملاحظات: ${invoice.notes}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider(color = semantic.cardBorder)

                Text(
                    text = "الأصناف المسجلة بالفاتورة (${items.size}):",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (items.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = semantic.cardSubtle,
                                border = BorderStroke(1.dp, semantic.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = invoice.notes.ifBlank { "قيد مديونية / توريد بضاعة آجلة للمورد" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", invoice.totalCost)} $currencySymbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = semantic.debtAmber
                                    )
                                }
                            }
                        }
                    } else {
                        items(items, key = { "${it.productId}_${it.productName}" }) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = semantic.cardSubtle,
                                border = BorderStroke(1.dp, semantic.cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "الكمية: ${item.quantity} × التكلفة: ${String.format(Locale.US, "%.2f", item.unitCost)} $currencySymbol",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", item.quantity * item.unitCost)} $currencySymbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = semantic.cardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenPrintTemplate,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("معاينة وطباعة PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (hasDebt) {
                        Button(
                            onClick = onPayDebt,
                            colors = ButtonDefaults.buttonColors(containerColor = semantic.profitGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسديد دفعة", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.7f)
                        ) {
                            Text("إغلاق", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DebtPaymentReceiptDialog(
    receipt: DebtPaymentReceiptData,
    viewModel: AntarSalesViewModel,
    storeName: String,
    storePhone: String,
    cashierName: String,
    currencySymbol: String,
    footerText: String,
    developerContact: String,
    context: Context,
    onDismiss: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val storeLogoImagePath by viewModel.storeLogoImagePath.collectAsState()
    val storeLogoCustomText by viewModel.storeLogoCustomText.collectAsState()
    val isCustomer = receipt.partyType == "CUSTOMER"
    val dateFormatted = remember(receipt.timestampMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = receipt.timestampMillis }
        val timeStr = SimpleDateFormat("hh:mm", Locale.US).format(cal.time).toEnglishDigits()
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"
        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cal.time).toEnglishDigits()
        "$timeStr $amPm • $dateStr"
    }
    val displayReceiptNum = remember(receipt.receiptNumber) {
        if (receipt.receiptNumber.startsWith("#") || receipt.receiptNumber.startsWith("Pay")) {
            receipt.receiptNumber
        } else {
            "Pay #${receipt.receiptNumber}"
        }
    }
    val isFullyCleared = receipt.remainingBalance <= 0.009

    val displayNotes = remember(receipt.notes) {
        sanitizeNotesDebtInvoiceRef(receipt.notes)
    }
    val displayFooter = remember(footerText) {
        if (footerText.isBlank()) {
            "شكراً لتعاملكم معنا"
        } else {
            footerText
        }
    }
    val rawInvoiceCode = remember(receipt.invoiceReference) {
        normalizeDebtInvoiceReference(receipt.invoiceReference).takeIf { it.isNotBlank() }
    }

    val shareText = remember(receipt, storeName, storePhone, currencySymbol, dateFormatted, displayReceiptNum, rawInvoiceCode, displayNotes, displayFooter) {
        buildString {
            appendLine("🧾 *${if (isCustomer) "وصل سداد مديونية عميل (سند قبض)" else "وصل سداد مستحقات مورد (سند صرف)"}*")
            appendLine("🏪 *$storeName*")
            if (storePhone.isNotBlank()) appendLine("📞 هاتف: $storePhone")
            appendLine("────────────────────")
            appendLine("رقم الوصل: $displayReceiptNum")
            if (rawInvoiceCode != null) {
                appendLine("رقم الفاتورة: \u202A\u200E$rawInvoiceCode\u200E\u202C")
            }
            appendLine("التاريخ: $dateFormatted")
            appendLine("${if (isCustomer) "العميل" else "المورد"}: *${receipt.partyName}*")
            if (receipt.partyPhone.isNotBlank()) appendLine("الهاتف: ${receipt.partyPhone}")
            appendLine("────────────────────")
            appendLine("• الرصيد السابق قبل الدفعة: ${String.format(Locale.US, "%.2f", receipt.previousBalance)} $currencySymbol")
            appendLine("• *المبلغ المسدد (الدفعة الحالية): ${String.format(Locale.US, "%.2f", receipt.paidAmount)} $currencySymbol*")
            appendLine("• *الرصيد المتبقي بعد السداد: ${String.format(Locale.US, "%.2f", receipt.remainingBalance)} $currencySymbol*")
            appendLine("الحالة: ${if (isFullyCleared) "خالص الذمة ✓" else "سداد جزئي"}")
            if (displayNotes.isNotBlank()) {
                appendLine("البيان: $displayNotes")
            }
            appendLine("────────────────────")
            if (displayFooter.isNotBlank()) appendLine(displayFooter)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, semantic.cardBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 740.dp)
                .padding(vertical = 20.dp, horizontal = 8.dp)
                .testTag("debt_payment_receipt_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dialog Top Bar
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
                            color = Color(0xFF0F172A).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFF0F172A)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isCustomer) "معاينة وطباعة وصل سداد الدين" else "معاينة وطباعة وصل سداد المستحقات",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "رقم السند: $displayReceiptNum • جاهز للطباعة الحرارية أو PDF",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Printable White Paper Receipt Card (All text strictly in Black Color 0xFF0F172A)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Store Header
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    InvoiceStoreLogoHeader(
                                        storeLogoImagePath = storeLogoImagePath,
                                        storeLogoCustomText = storeLogoCustomText,
                                        maxHeightDp = 46.dp
                                    )
                                    Text(
                                        text = storeName.ifBlank { "المتجر" },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    if (storePhone.isNotBlank()) {
                                        Text(
                                            text = "هاتف: $storePhone",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFE2E8F0),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Voucher Title Badge - Clean Neutral with Solid Black Text
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.2.dp, Color(0xFF0F172A))
                            ) {
                                Text(
                                    text = if (isCustomer) "وصل سداد مديونية عميل (سند قبض نقدية)" else "وصل سداد مستحقات مورد (سند صرف نقدية)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            // Metadata Grid - All Black Text in Arabic, Numbers 1234
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
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
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "رقم الوصل: $displayReceiptNum",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0F172A),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (rawInvoiceCode != null) {
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Start
                                                ) {
                                                    Text(
                                                        text = "رقم الفاتورة: ",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                        Text(
                                                            text = rawInvoiceCode,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF0F172A),
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = dateFormatted,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFCBD5E1))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isCustomer) "اسم العميل:" else "اسم المورد:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = receipt.partyName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    if (receipt.partyPhone.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "رقم الهاتف:",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = receipt.partyPhone.toEnglishDigits(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "المسؤول / الكاشير:",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = cashierName,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }

                            // 3-Row Balance Breakdown Table - All Black Text in Arabic, Numbers 1234
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1. Previous Debt Balance (Solid Black)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الرصيد السابق قبل الدفعة:",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", receipt.previousBalance).toEnglishDigits()} $currencySymbol",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    // 2. Paid Amount (Black Border, Solid Black Text)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.2.dp, Color(0xFF0F172A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "المبلغ المسدد (الدفعة الحالية):",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", receipt.paidAmount).toEnglishDigits()} $currencySymbol",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0F172A),
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFE2E8F0))

                                    // 3. Remaining Debt Balance (Solid Black)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الرصيد المتبقي بعد السداد:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", receipt.remainingBalance).toEnglishDigits()} $currencySymbol",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Status Stamp & Description (Solid Black in Arabic)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "البيان: $displayNotes",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFF0F172A))
                                ) {
                                    Text(
                                        text = if (isFullyCleared) "خالص الذمة ✓" else "سداد جزئي",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFCBD5E1))

                            // Signatures row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("توقيع المستلم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("....................", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isCustomer) "توقيع العميل" else "توقيع المورد",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text("....................", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }

                            if (displayFooter.isNotBlank()) {
                                Text(
                                    text = displayFooter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Primary Print Buttons (Thermal Bluetooth + System PDF Print)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val payload = ThermalReceiptPrintPayload(
                                storeName = storeName.ifBlank { "المتجر" },
                                storePhone = storePhone,
                                customerName = "${if (isCustomer) "العميل" else "المورد"}: ${receipt.partyName}",
                                cashierName = cashierName,
                                invoiceNumber = displayReceiptNum,
                                dateTimeFormatted = dateFormatted,
                                paymentMethodAr = if (isCustomer) "وصل سداد مديونية عميل (سند قبض)" else "وصل سداد مستحقات مورد (سند صرف)",
                                storeLogoImagePath = storeLogoImagePath,
                                storeLogoCustomText = storeLogoCustomText,
                                lines = listOf(
                                    PreviewLineItem(
                                        name = "الرصيد السابق قبل الدفعة",
                                        quantityPieces = 1,
                                        unitPrice = receipt.previousBalance,
                                        rawSubtotal = receipt.previousBalance,
                                        subtotalAfterDiscount = receipt.previousBalance,
                                        itemDiscountAmount = 0.0,
                                        itemDiscountPercent = 0.0,
                                        itemTvaAmount = 0.0,
                                        itemTvaPercent = 0.0,
                                        finalLineTotal = receipt.previousBalance,
                                        unitsPerCarton = 1,
                                        barcode = "",
                                        isDebt = false
                                    ),
                                    PreviewLineItem(
                                        name = "المبلغ المسدد (الدفعة الحالية)",
                                        quantityPieces = 1,
                                        unitPrice = receipt.paidAmount,
                                        rawSubtotal = receipt.paidAmount,
                                        subtotalAfterDiscount = receipt.paidAmount,
                                        itemDiscountAmount = 0.0,
                                        itemDiscountPercent = 0.0,
                                        itemTvaAmount = 0.0,
                                        itemTvaPercent = 0.0,
                                        finalLineTotal = receipt.paidAmount,
                                        unitsPerCarton = 1,
                                        barcode = "",
                                        isDebt = false
                                    ),
                                    PreviewLineItem(
                                        name = "الرصيد المتبقي بعد السداد",
                                        quantityPieces = 1,
                                        unitPrice = receipt.remainingBalance,
                                        rawSubtotal = receipt.remainingBalance,
                                        subtotalAfterDiscount = receipt.remainingBalance,
                                        itemDiscountAmount = 0.0,
                                        itemDiscountPercent = 0.0,
                                        itemTvaAmount = 0.0,
                                        itemTvaPercent = 0.0,
                                        finalLineTotal = receipt.remainingBalance,
                                        unitsPerCarton = 1,
                                        barcode = "",
                                        isDebt = receipt.remainingBalance > 0.009
                                    )
                                ),
                                sousTotal = receipt.previousBalance,
                                discountAmount = 0.0,
                                discountPercentInt = 0,
                                tvaAmount = 0.0,
                                netTotal = receipt.paidAmount,
                                creditAmount = receipt.remainingBalance,
                                especesAmount = receipt.paidAmount,
                                currency = currencySymbol,
                                footerText = "${displayNotes}\n$displayFooter".trim(),
                                developerContact = developerContact,
                                isTestReceipt = false
                            )
                            viewModel.printThermalReceiptPayload(payload)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_debt_receipt_thermal_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .offset(y = (-1).dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "طباعة حرارية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = {
                            printDebtReceiptViaSystemPrintManager(
                                context = context,
                                receipt = receipt,
                                storeName = storeName,
                                storePhone = storePhone,
                                cashierName = cashierName,
                                currencySymbol = currencySymbol,
                                footerText = footerText
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_debt_receipt_pdf_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(19.dp)
                                    .offset(y = (-1).dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "طباعة / حفظ PDF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Secondary Share / Copy Row (واتساب + ماسنجر + نسخ السند)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareTextViaWhatsApp(
                                context = context,
                                phone = receipt.partyPhone,
                                text = shareText
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1.05f)
                            .height(42.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier
                                    .size(15.dp)
                                    .offset(y = (-1).dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            shareTextViaMessenger(
                                context = context,
                                text = shareText
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF0084FF)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1.05f)
                            .height(42.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF0084FF),
                                modifier = Modifier
                                    .size(15.dp)
                                    .offset(y = (-1).dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ماسنجر", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0084FF))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("وصل سداد", shareText))
                            Toast.makeText(context, "تم نسخ تفاصيل وصل السداد للحافظة ✅", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(42.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(15.dp)
                                    .offset(y = (-1).dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ السند", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

internal fun buildCustomerStatementShareMessage(
    customer: Customer,
    transactions: List<CustomerTransaction>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String
): String {
    val cleanStore = storeName.ifBlank { "المتجر" }
    val totalDebtsAdded = transactions.filter { it.type != "PAYMENT" }.sumOf { it.amount }
    val totalPaymentsPaid = transactions.filter { it.type == "PAYMENT" }.sumOf { it.amount }
    val nowStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date()).toEnglishDigits()

    return buildString {
        appendLine("📋 *كشف حساب ومديونية عميل*")
        appendLine("🏪 *$cleanStore*")
        if (storePhone.isNotBlank()) appendLine("📞 هاتف المتجر: $storePhone")
        appendLine("🕒 التاريخ: $nowStr")
        appendLine("────────────────────")
        appendLine("👤 *العميل:* ${customer.name}")
        if (customer.phone.isNotBlank()) appendLine("📱 *رقم الهاتف:* ${customer.phone}")
        appendLine("────────────────────")
        appendLine("💰 *ملخص الحساب:*")
        if (totalDebtsAdded > 0.0) {
            appendLine("• إجمالي الفواتير/الديون: ${String.format(Locale.US, "%.2f", totalDebtsAdded)} $currencySymbol")
        } else if (customer.totalPurchases > 0.0) {
            appendLine("• إجمالي المشتريات: ${String.format(Locale.US, "%.2f", customer.totalPurchases)} $currencySymbol")
        }
        appendLine("• إجمالي الدفعات المسددة: ${String.format(Locale.US, "%.2f", totalPaymentsPaid)} $currencySymbol")
        appendLine("📌 *الرصيد المتبقي المستحق: ${String.format(Locale.US, "%.2f", customer.balanceDebt)} $currencySymbol*")
        appendLine("────────────────────")
        if (transactions.isEmpty()) {
            appendLine("لا توجد حركات مسجلة في السجل التفصيلي.")
        } else {
            appendLine("📑 *آخر الحركات (${transactions.take(15).size}):*")
            transactions.take(15).forEach { tx ->
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.date)).toEnglishDigits()
                val kind = if (tx.type == "PAYMENT") "✓ سداد دفعة (-)" else "+ فاتورة / دين"
                val notePart = if (tx.notes.isNotBlank()) " — ${tx.notes}" else ""
                appendLine("• [$d] $kind: ${String.format(Locale.US, "%.2f", tx.amount)} $currencySymbol$notePart")
            }
        }
        if (footerText.isNotBlank()) {
            appendLine("────────────────────")
            appendLine(footerText)
        }
    }.trim()
}

internal fun buildCustomerInvoiceShareMessage(
    customer: Customer,
    invoice: ArchivedInvoice?,
    transaction: CustomerTransaction?,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String
): String {
    val cleanStore = storeName.ifBlank { "المتجر" }
    val dateMillis = invoice?.dateMillis ?: transaction?.date ?: System.currentTimeMillis()
    val dateStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date(dateMillis)).toEnglishDigits()
    val isPayment = transaction?.type == "PAYMENT" || invoice?.paymentMethod == "CASH" && transaction?.type == "PAYMENT"

    return buildString {
        if (isPayment) {
            appendLine("✅ *إشعار سداد دفعة — $cleanStore*")
        } else {
            appendLine("🧾 *تفاصيل الفاتورة والمديونية — $cleanStore*")
        }
        if (storePhone.isNotBlank()) appendLine("📞 هاتف المتجر: $storePhone")
        appendLine("────────────────────")
        appendLine("👤 *العميل:* ${customer.name}")
        if (customer.phone.isNotBlank()) appendLine("📱 *الهاتف:* ${customer.phone}")
        if (invoice != null) {
            val invCode = if (invoice.invoiceId.startsWith("#") || invoice.invoiceId.startsWith("Pre")) invoice.invoiceId else "#${invoice.invoiceId}"
            appendLine("🔢 *رقم الفاتورة / السند:* $invCode")
        }
        appendLine("📅 *التاريخ:* $dateStr")
        appendLine("────────────────────")

        if (invoice != null && invoice.items.isNotEmpty() && !isPayment) {
            appendLine("📦 *الأصناف والتفاصيل:*")
            invoice.items.forEachIndexed { idx, item ->
                val unitPrice = if (item.quantitySold > 0) item.totalPrice / item.quantitySold else item.totalPrice
                appendLine(
                    "${idx + 1}. ${item.productName} | الكمية: ${item.quantitySold} × ${String.format(Locale.US, "%.2f", unitPrice)} = *${String.format(Locale.US, "%.2f", item.totalPrice)} $currencySymbol*"
                )
            }
            appendLine("────────────────────")
            appendLine("💵 *إجمالي الفاتورة:* ${String.format(Locale.US, "%.2f", invoice.subtotal)} $currencySymbol")
        } else if (transaction != null) {
            val txLabel = if (transaction.type == "PAYMENT") "المبلغ المسدد" else "مبلغ العملية / الفاتورة"
            appendLine("📝 *البيان:* ${transaction.notes.ifBlank { if (isPayment) "سداد دفعة من الحساب" else "قيد فاتورة آجلة" }}")
            appendLine("💵 *$txLabel:* ${String.format(Locale.US, "%.2f", transaction.amount)} $currencySymbol")
        }

        appendLine("📌 *الرصيد المتبقي الحالي على الحساب: ${String.format(Locale.US, "%.2f", customer.balanceDebt)} $currencySymbol*")
        if (footerText.isNotBlank()) {
            appendLine("────────────────────")
            appendLine(footerText)
        } else {
            appendLine("────────────────────")
            appendLine("شكراً لتعاملكم معنا 🌟")
        }
    }.trim()
}

internal fun formatPhoneNumberForWhatsApp(rawPhone: String): String {
    val clean = rawPhone.filter { it.isDigit() }
    if (clean.isBlank()) return ""
    return when {
        clean.startsWith("00") -> clean.removePrefix("00")
        clean.startsWith("213") -> clean
        clean.startsWith("0") && clean.length == 10 -> "213" + clean.substring(1)
        clean.length == 9 && (clean.startsWith("5") || clean.startsWith("6") || clean.startsWith("7")) -> "213$clean"
        else -> clean
    }
}

internal fun shareTextViaWhatsApp(
    context: Context,
    phone: String,
    text: String
) {
    // 1. Copy to clipboard automatically as a reliable backup
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("رسالة واتساب", text))
    } catch (_: Exception) {
    }

    val formattedPhone = formatPhoneNumberForWhatsApp(phone)
    val encoded = Uri.encode(text)

    // 2. Direct whatsapp:// URI with international phone if available
    if (formattedPhone.isNotBlank()) {
        try {
            val directUri = Uri.parse("whatsapp://send?phone=$formattedPhone&text=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, directUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
        }

        // 3. wa.me web link with normalized phone
        try {
            val url = "https://wa.me/$formattedPhone?text=$encoded"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
        }
    }

    // 4. Try WhatsApp or WhatsApp Business package directly
    for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        try {
            val waIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(waIntent)
            return
        } catch (_: Exception) {
        }
    }

    // 5. Universal Share Chooser fallback
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "مشاركة عبر واتساب أو التطبيقات الأخرى").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, "تم نسخ تفاصيل الحساب للحافظة بنجاح ✅", Toast.LENGTH_SHORT).show()
    }
}

internal fun shareTextViaMessenger(
    context: Context,
    text: String
) {
    // 1. Copy to clipboard first so user always has the message ready in Messenger
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("رسالة ماسنجر", text))
    } catch (_: Exception) {
    }

    // 2. Try Facebook Messenger (com.facebook.orca)
    try {
        val messengerIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.facebook.orca")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(messengerIntent)
        Toast.makeText(context, "تم نسخ الرسالة وفتح ماسنجر 💬", Toast.LENGTH_SHORT).show()
        return
    } catch (_: Exception) {
    }

    // 3. Try Messenger Lite (com.facebook.mlite)
    try {
        val liteIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.facebook.mlite")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(liteIntent)
        Toast.makeText(context, "تم نسخ الرسالة وفتح ماسنجر 💬", Toast.LENGTH_SHORT).show()
        return
    } catch (_: Exception) {
    }

    // 4. Fallback: System share sheet + clipboard notification
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, "مشاركة الرسالة عبر ماسنجر أو التطبيقات").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, "تم نسخ التفاصيل للحافظة لإرسالها عبر ماسنجر ✅", Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun ShareCustomerDebtAndInvoiceDialog(
    payload: ShareCustomerDebtPayload,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String,
    context: Context,
    onDismiss: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val customer = payload.customer
    val hasInvoiceOrTx = payload.latestInvoice != null || payload.focusTransaction != null
    // "INVOICE" = تفاصيل الفاتورة والمديونية, "STATEMENT" = كشف الحساب الكامل
    var selectedMode by remember(payload) {
        mutableStateOf(if (hasInvoiceOrTx) payload.initialMode else "STATEMENT")
    }

    val messageText = remember(
        selectedMode,
        customer,
        payload.transactions,
        payload.latestInvoice,
        payload.focusTransaction,
        storeName,
        storePhone,
        currencySymbol,
        footerText
    ) {
        if (selectedMode == "INVOICE" && hasInvoiceOrTx) {
            buildCustomerInvoiceShareMessage(
                customer = customer,
                invoice = payload.latestInvoice,
                transaction = payload.focusTransaction,
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol,
                footerText = footerText
            )
        } else {
            buildCustomerStatementShareMessage(
                customer = customer,
                transactions = payload.transactions,
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol,
                footerText = footerText
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, semantic.cardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("share_customer_debt_dialog")
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
                            color = Color(0xFF16A34A).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF16A34A)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "إرسال الفاتورة أو كشف الحساب للعميل",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp
                            )
                            Text(
                                text = "العميل: ${customer.name} • الهاتف: ${customer.phone.ifBlank { "غير مسجل" }}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Mode Switcher (تفاصيل الفاتورة vs كشف الحساب الشامل)
                if (hasInvoiceOrTx) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isInvoiceSelected = selectedMode == "INVOICE"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isInvoiceSelected) MaterialTheme.colorScheme.primaryContainer else semantic.cardSubtle,
                            border = BorderStroke(
                                1.2.dp,
                                if (isInvoiceSelected) MaterialTheme.colorScheme.primary else semantic.cardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedMode = "INVOICE" }
                                .testTag("share_mode_invoice_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (isInvoiceSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "تفاصيل الفاتورة والدين",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isInvoiceSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isInvoiceSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isStmtSelected = selectedMode == "STATEMENT"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isStmtSelected) MaterialTheme.colorScheme.primaryContainer else semantic.cardSubtle,
                            border = BorderStroke(
                                1.2.dp,
                                if (isStmtSelected) MaterialTheme.colorScheme.primary else semantic.cardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedMode = "STATEMENT" }
                                .testTag("share_mode_statement_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isStmtSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "كشف الحساب الكامل",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isStmtSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isStmtSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Message Preview Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = semantic.cardSubtle,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "معاينة الرسالة الجاهزة للإرسال:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = messageText,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (selectedMode == "STATEMENT") {
                    Button(
                        onClick = {
                            shareCustomerStatementPdf(
                                context = context,
                                customer = customer,
                                transactions = payload.transactions,
                                storeName = storeName,
                                storePhone = storePhone,
                                currencySymbol = currencySymbol,
                                footerText = footerText
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("share_dialog_statement_pdf_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إرسال وحفظ كشف الحساب كملف PDF",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                }

                // Primary Action Buttons: WhatsApp + Messenger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shareTextViaWhatsApp(
                                context = context,
                                phone = customer.phone,
                                text = messageText
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("share_dialog_whatsapp_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إرسال عبر واتساب",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            shareTextViaMessenger(
                                context = context,
                                text = messageText
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084FF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("share_dialog_messenger_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إرسال عبر ماسنجر",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = Color.White
                        )
                    }
                }

                // Secondary Row: Copy Text & General Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("تفاصيل الحساب", messageText))
                            Toast.makeText(context, "تم نسخ الرسالة للحافظة ✅", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_dialog_copy_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ الرسالة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, messageText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة تفاصيل الفاتورة أو الدين"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_dialog_system_share_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة لتطبيق آخر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun printDebtReceiptViaSystemPrintManager(
    context: Context,
    receipt: DebtPaymentReceiptData,
    storeName: String,
    storePhone: String,
    cashierName: String,
    currencySymbol: String,
    footerText: String
) {
    val activity = context.findActivity()
    val targetContext = activity ?: context
    val printManager = targetContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (activity == null || printManager == null) {
        Toast.makeText(context, "خدمة الطباعة غير متوفرة على هذا الجهاز", Toast.LENGTH_SHORT).show()
        return
    }

    val displayReceiptNum = if (receipt.receiptNumber.startsWith("#") || receipt.receiptNumber.startsWith("Pay")) {
        receipt.receiptNumber
    } else {
        "Pay #${receipt.receiptNumber}"
    }
    val jobName = "Receipt_${displayReceiptNum.replace(" ", "_")}"
    val isCustomer = receipt.partyType == "CUSTOMER"
    val dateFormatted = SimpleDateFormat("dd-MM-yyyy • HH:mm", Locale.US).format(Date(receipt.timestampMillis))

    try {
        printManager.print(
        jobName,
        object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("ملف الوجهة غير صالح")
                    return
                }

                val pdfDoc = PdfDocument()
                try {
                    val pageWidth = 595
                    val pageHeight = 842
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas = page.canvas

                    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = 20f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = 15f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = 13f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.LEFT
                    }
                    val greenValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = 16f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.LEFT
                    }
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(203, 213, 225)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                    }
                    val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(248, 250, 252)
                        style = Paint.Style.FILL
                    }

                    val margin = 48f
                    val rightEdge = pageWidth - margin
                    val leftEdge = margin
                    val centerX = pageWidth / 2f

                    // Outer Frame
                    canvas.drawRoundRect(leftEdge, 40f, rightEdge, 660f, 16f, 16f, borderPaint)

                    var y = 62f
                    val logoPrefs = context.getSharedPreferences("af_store_prefs", Context.MODE_PRIVATE)
                    y = drawStoreLogoOnPdfCanvas(
                        canvas = canvas,
                        centerX = centerX,
                        topY = y,
                        storeLogoImagePath = logoPrefs.getString("store_logo_image_path", "") ?: "",
                        storeLogoCustomText = logoPrefs.getString("store_logo_custom_text", "") ?: "",
                        maxWidthPx = 130,
                        maxHeightPx = 56
                    )
                    y += 18f
                    canvas.drawText(storeName.ifBlank { "المتجر" }, centerX, y, titlePaint)
                    if (storePhone.isNotBlank()) {
                        y += 22f
                        val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(15, 23, 42)
                            textSize = 12f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.drawText("هاتف: $storePhone", centerX, y, phonePaint)
                    }

                    y += 30f
                    canvas.drawText(
                        if (isCustomer) "وصل سداد مديونية عميل (سند قبض)" else "وصل سداد مستحقات مورد (سند صرف)",
                        centerX,
                        y,
                        subHeaderPaint
                    )

                    y += 24f
                    canvas.drawLine(leftEdge + 20f, y, rightEdge - 20f, y, borderPaint)

                    y += 28f
                    canvas.drawText("رقم السند: $displayReceiptNum", rightEdge - 24f, y, labelPaint)
                    canvas.drawText("التاريخ: $dateFormatted", leftEdge + 24f, y, valuePaint)

                    val formattedPdfInvoiceRef = normalizeDebtInvoiceReference(receipt.invoiceReference).takeIf { it.isNotBlank() }
                    if (formattedPdfInvoiceRef != null) {
                        y += 22f
                        canvas.drawText("رقم الفاتورة: \u202A\u200E$formattedPdfInvoiceRef\u200E\u202C", rightEdge - 24f, y, labelPaint)
                    }

                    y += 28f
                    canvas.drawText(
                        if (isCustomer) "اسم العميل: ${receipt.partyName}" else "اسم المورد: ${receipt.partyName}",
                        rightEdge - 24f,
                        y,
                        labelPaint
                    )
                    if (receipt.partyPhone.isNotBlank()) {
                        canvas.drawText("الهاتف: ${receipt.partyPhone}", leftEdge + 24f, y, valuePaint)
                    }

                    y += 26f
                    canvas.drawText("المستلم / الكاشير: $cashierName", rightEdge - 24f, y, labelPaint)

                    // Balance Table Box
                    y += 24f
                    val boxTop = y
                    val boxBottom = y + 135f
                    canvas.drawRoundRect(leftEdge + 24f, boxTop, rightEdge - 24f, boxBottom, 12f, 12f, boxFillPaint)
                    canvas.drawRoundRect(leftEdge + 24f, boxTop, rightEdge - 24f, boxBottom, 12f, 12f, borderPaint)

                    y += 34f
                    canvas.drawText("الرصيد السابق قبل الدفعة:", rightEdge - 40f, y, labelPaint)
                    canvas.drawText(
                        "${String.format(Locale.US, "%.2f", receipt.previousBalance)} $currencySymbol",
                        leftEdge + 40f,
                        y,
                        valuePaint
                    )

                    y += 40f
                    canvas.drawText("المبلغ المسدد (الدفعة الحالية):", rightEdge - 40f, y, labelPaint)
                    canvas.drawText(
                        "${String.format(Locale.US, "%.2f", receipt.paidAmount)} $currencySymbol",
                        leftEdge + 40f,
                        y,
                        greenValuePaint
                    )

                    y += 40f
                    canvas.drawText("الرصيد المتبقي بعد السداد:", rightEdge - 40f, y, labelPaint)
                    canvas.drawText(
                        "${String.format(Locale.US, "%.2f", receipt.remainingBalance)} $currencySymbol",
                        leftEdge + 40f,
                        y,
                        valuePaint
                    )

                    y = boxBottom + 34f
                    val cleanPdfNotes = sanitizeNotesDebtInvoiceRef(receipt.notes)
                    canvas.drawText("البيان: $cleanPdfNotes", rightEdge - 24f, y, labelPaint)

                    y += 65f
                    canvas.drawText("توقيع المستلم: ........................", rightEdge - 36f, y, labelPaint)
                    canvas.drawText(
                        if (isCustomer) "توقيع العميل: ........................" else "توقيع المورد: ........................",
                        leftEdge + 36f,
                        y,
                        valuePaint
                    )

                    val cleanFooter = if (footerText.isNotBlank()) footerText else "شكراً لتعاملكم معنا"
                    y += 48f
                    val footerPaint = Paint(labelPaint).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText(cleanFooter, centerX, y, footerPaint)

                    pdfDoc.finishPage(page)
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc.writeTo(out)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message ?: "خطأ أثناء إنشاء ملف PDF")
                } finally {
                    pdfDoc.close()
                }
            }
        },
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()
        )
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح نافذة الطباعة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

internal fun createCustomerStatementPdfDocument(
    context: Context,
    customer: Customer,
    transactions: List<CustomerTransaction>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String
): PdfDocument {
    val issueDateStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date()).toEnglishDigits()
    val totalDebtsAdded = transactions.filter { it.type != "PAYMENT" }.sumOf { it.amount }
    val totalPaymentsPaid = transactions.filter { it.type == "PAYMENT" }.sumOf { it.amount }

    // Pagination setup: 14 rows on page 1, 20 rows on subsequent pages
    val firstPageCapacity = 14
    val nextPageCapacity = 20
    val chunks = mutableListOf<List<CustomerTransaction>>()
    if (transactions.isEmpty()) {
        chunks.add(emptyList())
    } else {
        val firstSlice = transactions.take(firstPageCapacity)
        chunks.add(firstSlice)
        var remaining = transactions.drop(firstPageCapacity)
        while (remaining.isNotEmpty()) {
            chunks.add(remaining.take(nextPageCapacity))
            remaining = remaining.drop(nextPageCapacity)
        }
    }
    val totalPages = chunks.size.coerceAtLeast(1)

    val pdfDoc = PdfDocument()
    try {
        val pageWidth = 595
                    val pageHeight = 842
                    val margin = 36f
                    val leftEdge = margin
                    val rightEdge = pageWidth - margin
                    val centerX = pageWidth / 2f

                    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = 18f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(30, 64, 175)
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val labelRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(51, 65, 85)
                        textSize = 11.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    val valueLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = 11.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.LEFT
                    }
                    val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = 11f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(30, 41, 59)
                        textSize = 10.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textAlign = Paint.Align.RIGHT
                    }
                    val greenAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(5, 150, 105)
                        textSize = 11f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.LEFT
                    }
                    val redAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(220, 38, 38)
                        textSize = 11f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.LEFT
                    }
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(203, 213, 225)
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    }
                    val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(30, 58, 138)
                        style = Paint.Style.FILL
                    }
                    val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(248, 250, 252)
                        style = Paint.Style.FILL
                    }
                    val zebraFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(241, 245, 249)
                        style = Paint.Style.FILL
                    }

                    var runningRowIndex = 1

                    chunks.forEachIndexed { pageIndex, pageRows ->
                        val pageNumber = pageIndex + 1
                        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        val page = pdfDoc.startPage(pageInfo)
                        val canvas = page.canvas

                        // Outer page frame
                        canvas.drawRoundRect(leftEdge - 6f, 26f, rightEdge + 6f, pageHeight - 28f, 12f, 12f, borderPaint)

                        var y = 56f
                        if (pageNumber == 1) {
                            canvas.drawText(storeName.ifBlank { "المتجر" }, centerX, y, titlePaint)
                            if (storePhone.isNotBlank()) {
                                y += 18f
                                val phoneP = Paint(labelRightPaint).apply { textAlign = Paint.Align.CENTER }
                                canvas.drawText("هاتف المتجر: $storePhone", centerX, y, phoneP)
                            }
                            y += 22f
                            canvas.drawText("كشف حساب عميل تفصيلي", centerX, y, subTitlePaint)

                            // Customer metadata box
                            y += 16f
                            val infoTop = y
                            val infoBottom = y + 52f
                            canvas.drawRoundRect(leftEdge + 6f, infoTop, rightEdge - 6f, infoBottom, 8f, 8f, cardFillPaint)
                            canvas.drawRoundRect(leftEdge + 6f, infoTop, rightEdge - 6f, infoBottom, 8f, 8f, borderPaint)

                            canvas.drawText("اسم العميل: ${customer.name}", rightEdge - 18f, infoTop + 20f, labelRightPaint)
                            canvas.drawText("تاريخ الإصدار: $issueDateStr", leftEdge + 18f, infoTop + 20f, valueLeftPaint)
                            canvas.drawText(
                                "رقم الهاتف: ${customer.phone.ifBlank { "غير مسجل" }}",
                                rightEdge - 18f,
                                infoTop + 40f,
                                labelRightPaint
                            )
                            canvas.drawText("إجمالي الحركات: ${transactions.size}", leftEdge + 18f, infoTop + 40f, valueLeftPaint)

                            // 3 Summary Cards
                            y = infoBottom + 12f
                            val sumTop = y
                            val sumBottom = y + 54f
                            val boxW = (rightEdge - leftEdge - 24f) / 3f

                            // Right card: إجمالي الديون
                            val rRight = rightEdge - 6f
                            val rLeft = rRight - boxW
                            canvas.drawRoundRect(rLeft, sumTop, rRight, sumBottom, 8f, 8f, cardFillPaint)
                            canvas.drawRoundRect(rLeft, sumTop, rRight, sumBottom, 8f, 8f, borderPaint)
                            canvas.drawText("إجمالي الديون المضافة", rRight - 10f, sumTop + 20f, labelRightPaint)
                            canvas.drawText(
                                "${String.format(Locale.US, "%.2f", totalDebtsAdded)} $currencySymbol",
                                rRight - 10f,
                                sumTop + 40f,
                                Paint(redAmountPaint).apply { textAlign = Paint.Align.RIGHT }
                            )

                            // Middle card: إجمالي المسدد
                            val mRight = rLeft - 6f
                            val mLeft = mRight - boxW
                            canvas.drawRoundRect(mLeft, sumTop, mRight, sumBottom, 8f, 8f, cardFillPaint)
                            canvas.drawRoundRect(mLeft, sumTop, mRight, sumBottom, 8f, 8f, borderPaint)
                            canvas.drawText("إجمالي الدفعات المسددة", mRight - 10f, sumTop + 20f, labelRightPaint)
                            canvas.drawText(
                                "${String.format(Locale.US, "%.2f", totalPaymentsPaid)} $currencySymbol",
                                mRight - 10f,
                                sumTop + 40f,
                                Paint(greenAmountPaint).apply { textAlign = Paint.Align.RIGHT }
                            )

                            // Left card: الرصيد المتبقي الحالي
                            val lRight = mLeft - 6f
                            val lLeft = leftEdge + 6f
                            canvas.drawRoundRect(lLeft, sumTop, lRight, sumBottom, 8f, 8f, cardFillPaint)
                            canvas.drawRoundRect(lLeft, sumTop, lRight, sumBottom, 8f, 8f, borderPaint)
                            canvas.drawText("الرصيد المتبقي الحالي", lRight - 10f, sumTop + 20f, labelRightPaint)
                            val balPaint = if (customer.balanceDebt > 0.009) {
                                Paint(redAmountPaint).apply { textAlign = Paint.Align.RIGHT }
                            } else {
                                Paint(greenAmountPaint).apply { textAlign = Paint.Align.RIGHT }
                            }
                            canvas.drawText(
                                "${String.format(Locale.US, "%.2f", customer.balanceDebt)} $currencySymbol",
                                lRight - 10f,
                                sumTop + 40f,
                                balPaint
                            )

                            y = sumBottom + 16f
                        } else {
                            canvas.drawText(
                                "${storeName.ifBlank { "المتجر" }} — كشف حساب العميل: ${customer.name} (تابع)",
                                centerX,
                                y,
                                subTitlePaint
                            )
                            y += 20f
                        }

                        // Table Header Row
                        val tableLeft = leftEdge + 6f
                        val tableRight = rightEdge - 6f
                        val headerHeight = 26f
                        canvas.drawRoundRect(tableLeft, y, tableRight, y + headerHeight, 6f, 6f, headerFillPaint)

                        val colIndexRight = tableRight - 10f
                        val colDateRight = tableRight - 42f
                        val colTypeRight = tableRight - 150f
                        val colNotesRight = tableRight - 245f
                        val colAmountLeft = tableLeft + 12f

                        val headerTextY = y + 17f
                        canvas.drawText("#", colIndexRight, headerTextY, tableHeaderPaint)
                        canvas.drawText("التاريخ", colDateRight, headerTextY, tableHeaderPaint)
                        canvas.drawText("نوع الحركة", colTypeRight, headerTextY, tableHeaderPaint)
                        canvas.drawText("البيان / الملاحظات", colNotesRight, headerTextY, tableHeaderPaint)
                        canvas.drawText(
                            "المبلغ ($currencySymbol)",
                            colAmountLeft,
                            headerTextY,
                            Paint(tableHeaderPaint).apply { textAlign = Paint.Align.LEFT }
                        )

                        y += headerHeight

                        if (pageRows.isEmpty()) {
                            val emptyRowH = 44f
                            canvas.drawRect(tableLeft, y, tableRight, y + emptyRowH, borderPaint)
                            val emptyP = Paint(cellTextPaint).apply { textAlign = Paint.Align.CENTER }
                            canvas.drawText("لا توجد حركات مسجلة لهذا العميل حتى الآن.", centerX, y + 26f, emptyP)
                            y += emptyRowH
                        } else {
                            val rowHeight = 26f
                            pageRows.forEachIndexed { idx, tx ->
                                val rowTop = y
                                val rowBottom = y + rowHeight
                                if (idx % 2 == 1) {
                                    canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, zebraFillPaint)
                                }
                                canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, borderPaint)

                                val isPay = tx.type == "PAYMENT"
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(tx.date)).toEnglishDigits()
                                val typeLabel = if (isPay) "تسديد دفعة (-)" else "إضافة دين (+)"
                                val cleanNotes = tx.notes.ifBlank { "—" }.let {
                                    if (it.length > 34) it.take(34) + "…" else it
                                }
                                val amountFormatted = "${if (isPay) "-" else "+"}${String.format(Locale.US, "%.2f", tx.amount)} $currencySymbol"

                                val rowTextY = rowTop + 17f
                                canvas.drawText(runningRowIndex.toString(), colIndexRight, rowTextY, cellTextPaint)
                                canvas.drawText(dateStr, colDateRight, rowTextY, cellTextPaint)
                                canvas.drawText(
                                    typeLabel,
                                    colTypeRight,
                                    rowTextY,
                                    Paint(cellTextPaint).apply {
                                        color = if (isPay) android.graphics.Color.rgb(5, 150, 105) else android.graphics.Color.rgb(220, 38, 38)
                                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                    }
                                )
                                canvas.drawText(cleanNotes, colNotesRight, rowTextY, cellTextPaint)
                                canvas.drawText(
                                    amountFormatted,
                                    colAmountLeft,
                                    rowTextY,
                                    if (isPay) greenAmountPaint else redAmountPaint
                                )

                                runningRowIndex++
                                y += rowHeight
                            }
                        }

                        // Signatures on last page
                        if (pageNumber == totalPages) {
                            val sigY = (y + 44f).coerceAtMost(pageHeight - 75f)
                            canvas.drawText("توقيع المسؤول / الكاشير: ............................", rightEdge - 20f, sigY, labelRightPaint)
                            canvas.drawText("توقيع العميل للمطابقة: ............................", leftEdge + 20f, sigY, valueLeftPaint)

                            if (footerText.isNotBlank()) {
                                val fPaint = Paint(cellTextPaint).apply { textAlign = Paint.Align.CENTER }
                                canvas.drawText(footerText, centerX, (sigY + 22f).coerceAtMost(pageHeight - 52f), fPaint)
                            }
                        }

                        // Footer Page Number
                        val pageNumPaint = Paint(cellTextPaint).apply {
                            textAlign = Paint.Align.CENTER
                            color = android.graphics.Color.rgb(100, 116, 139)
                        }
                        canvas.drawText(
                            "صفحة $pageNumber من $totalPages • ${storeName.ifBlank { "المتجر" }}",
                            centerX,
                            pageHeight - 36f,
                            pageNumPaint
                        )

                        pdfDoc.finishPage(page)
                    }

        return pdfDoc
    } catch (e: Exception) {
        pdfDoc.close()
        throw e
    }
}

internal fun generateCustomerStatementPdfFile(
    context: Context,
    customer: Customer,
    transactions: List<CustomerTransaction>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String
): File {
    val dir = File(context.cacheDir, "statements").apply { if (!exists()) mkdirs() }
    val safeCustName = customer.name.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").ifBlank { "Customer" }
    val dateTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val pdfFile = File(dir, "Statement_${safeCustName}_$dateTag.pdf")
    val pdfDoc = createCustomerStatementPdfDocument(
        context = context,
        customer = customer,
        transactions = transactions,
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        footerText = footerText
    )
    try {
        FileOutputStream(pdfFile).use { out ->
            pdfDoc.writeTo(out)
        }
    } finally {
        pdfDoc.close()
    }
    return pdfFile
}

internal fun shareCustomerStatementPdf(
    context: Context,
    customer: Customer,
    transactions: List<CustomerTransaction>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String,
    targetPackage: String? = null
) {
    try {
        val file = generateCustomerStatementPdfFile(
            context = context,
            customer = customer,
            transactions = transactions,
            storeName = storeName,
            storePhone = storePhone,
            currencySymbol = currencySymbol,
            footerText = footerText
        )
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val balanceFormatted = "${String.format(Locale.US, "%.2f", customer.balanceDebt)} $currencySymbol"
        val subject = "كشف حساب عميل: ${customer.name}"
        val extraText = "مرفق كشف حساب العميل: ${customer.name}\nالرصيد المتبقي: $balanceFormatted\nمن متجر: ${storeName.ifBlank { "المتجر" }}"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, extraText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri("كشف حساب PDF", fileUri)
            if (!targetPackage.isNullOrBlank()) {
                setPackage(targetPackage)
            }
        }

        if (!targetPackage.isNullOrBlank()) {
            try {
                context.startActivity(shareIntent)
                Toast.makeText(context, "جاري إرسال كشف الحساب (PDF) 📄", Toast.LENGTH_SHORT).show()
                return
            } catch (_: Exception) {
            }
        }

        val chooser = Intent.createChooser(shareIntent, "حفظ أو إرسال كشف حساب العميل (PDF)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        Toast.makeText(context, "تم تجهيز ملف كشف الحساب (PDF) بنجاح 📄", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر إنشاء ملف كشف الحساب: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun printCustomerStatementViaSystemPrintManager(
    context: Context,
    customer: Customer,
    transactions: List<CustomerTransaction>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String
) {
    val activity = context.findActivity()
    val targetContext = activity ?: context
    val printManager = targetContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (activity == null || printManager == null) {
        Toast.makeText(context, "جاري فتح ومشاركة ملف كشف الحساب (PDF) للحفظ والطباعة 📄", Toast.LENGTH_LONG).show()
        shareCustomerStatementPdf(context, customer, transactions, storeName, storePhone, currencySymbol, footerText)
        return
    }

    val safeCustName = customer.name.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").ifBlank { "Customer" }
    val jobName = "Customer_Statement_${safeCustName}_${System.currentTimeMillis() / 1000}"

    val firstPageCapacity = 14
    val nextPageCapacity = 20
    val totalRows = transactions.size
    val totalPages = if (totalRows <= firstPageCapacity) 1 else 1 + (totalRows - firstPageCapacity + nextPageCapacity - 1) / nextPageCapacity

    try {
        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(totalPages)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (destination == null) {
                        callback?.onWriteFailed("ملف الوجهة غير صالح")
                        return
                    }
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }

                    try {
                        val pdfDoc = createCustomerStatementPdfDocument(
                            context = context,
                            customer = customer,
                            transactions = transactions,
                            storeName = storeName,
                            storePhone = storePhone,
                            currencySymbol = currencySymbol,
                            footerText = footerText
                        )
                        try {
                            FileOutputStream(destination.fileDescriptor).use { out ->
                                pdfDoc.writeTo(out)
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } finally {
                            pdfDoc.close()
                        }
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message ?: "خطأ أثناء إنشاء ملف PDF لكشف الحساب")
                    }
                }
            },
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح خدمة الطباعة — جاري فتح ومشاركة ملف PDF بدلاً من ذلك 📄", Toast.LENGTH_LONG).show()
        shareCustomerStatementPdf(context, customer, transactions, storeName, storePhone, currencySymbol, footerText)
    }
}

private fun printSupplierStatementViaSystemPrintManager(
    context: Context,
    supplierName: String,
    supplierPhone: String,
    invoices: List<PurchaseInvoice>,
    storeName: String,
    storePhone: String,
    currencySymbol: String,
    footerText: String
) {
    val activity = context.findActivity()
    val targetContext = activity ?: context
    val printManager = targetContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (activity == null || printManager == null) {
        Toast.makeText(context, "خدمة الطباعة المباشرة غير متوفرة على هذا الجهاز", Toast.LENGTH_SHORT).show()
        return
    }

    val safeSupName = supplierName.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").ifBlank { "Supplier" }
    val jobName = "Supplier_Statement_${safeSupName}_${System.currentTimeMillis() / 1000}"
    val issueDateStr = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date()).toEnglishDigits()
    val totalPurchases = invoices.sumOf { it.totalCost }
    val totalPaid = invoices.sumOf { it.paidAmount }
    val totalRemaining = invoices.sumOf { it.remainingDebt }

    val firstPageCapacity = 14
    val nextPageCapacity = 20
    val chunks = mutableListOf<List<PurchaseInvoice>>()
    if (invoices.isEmpty()) {
        chunks.add(emptyList())
    } else {
        chunks.add(invoices.take(firstPageCapacity))
        var remaining = invoices.drop(firstPageCapacity)
        while (remaining.isNotEmpty()) {
            chunks.add(remaining.take(nextPageCapacity))
            remaining = remaining.drop(nextPageCapacity)
        }
    }
    val totalPages = chunks.size.coerceAtLeast(1)

    try {
        printManager.print(
        jobName,
        object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(totalPages)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("ملف الوجهة غير صالح")
                    return
                }

                val pdfDoc = PdfDocument()
                try {
                    val pageWidth = 595
                    val pageHeight = 842
                    val margin = 36f
                    val leftEdge = margin
                    val rightEdge = pageWidth - margin
                    val centerX = pageWidth / 2f

                    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = 18f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(180, 83, 9)
                        textSize = 14f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    val labelRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(51, 65, 85)
                        textSize = 11.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    val valueLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        textSize = 11.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.LEFT
                    }
                    val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = 11f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(30, 41, 59)
                        textSize = 10.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        textAlign = Paint.Align.RIGHT
                    }
                    val greenAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(5, 150, 105)
                        textSize = 10.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    val amberAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(217, 119, 6)
                        textSize = 10.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.LEFT
                    }
                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(203, 213, 225)
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    }
                    val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(15, 23, 42)
                        style = Paint.Style.FILL
                    }
                    val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(248, 250, 252)
                        style = Paint.Style.FILL
                    }
                    val zebraFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(241, 245, 249)
                        style = Paint.Style.FILL
                    }

                    var runningIndex = 1

                    chunks.forEachIndexed { pageIndex, pageInvoices ->
                        val pageNumber = pageIndex + 1
                        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        val page = pdfDoc.startPage(pageInfo)
                        val canvas = page.canvas

                        canvas.drawRoundRect(leftEdge - 6f, 26f, rightEdge + 6f, pageHeight - 28f, 12f, 12f, borderPaint)

                        var y = 56f
                        if (pageNumber == 1) {
                            canvas.drawText(storeName.ifBlank { "المتجر" }, centerX, y, titlePaint)
                            if (storePhone.isNotBlank()) {
                                y += 18f
                                val phoneP = Paint(labelRightPaint).apply { textAlign = Paint.Align.CENTER }
                                canvas.drawText("هاتف المتجر: $storePhone", centerX, y, phoneP)
                            }
                            y += 22f
                            canvas.drawText("كشف حساب مورد تفصيلي", centerX, y, subTitlePaint)

                            // Supplier Info Box
                            y += 16f
                            val infoTop = y
                            val infoBottom = y + 52f
                            canvas.drawRoundRect(leftEdge + 6f, infoTop, rightEdge - 6f, infoBottom, 8f, 8f, cardFillPaint)
                            canvas.drawRoundRect(leftEdge + 6f, infoTop, rightEdge - 6f, infoBottom, 8f, 8f, borderPaint)

                            canvas.drawText("اسم المورد: $supplierName", rightEdge - 18f, infoTop + 20f, labelRightPaint)
                            canvas.drawText("تاريخ الإصدار: $issueDateStr", leftEdge + 18f, infoTop + 20f, valueLeftPaint)
                            canvas.drawText(
                                "رقم الهاتف: ${supplierPhone.ifBlank { "غير مسجل" }}",
                                rightEdge - 18f,
                                infoTop + 40f,
                                labelRightPaint
                            )
                            canvas.drawText("عدد الفواتير: ${invoices.size}", leftEdge + 18f, infoTop + 40f, valueLeftPaint)

                            // 3 Summary Cards
                            y = infoBottom + 12f
                            val sumTop = y
                            val sumBottom = y + 54f
                            val boxW = (rightEdge - leftEdge - 24f) / 3f

                            // Right card: إجمالي التوريد
                            val rRight = rightEdge - 6f
                            val rLeft = rRight - boxW
                            canvas.drawRoundRect(rLeft, sumTop, rRight, sumBottom, 8f, 8f, cardFillPaint)
                            canvas.drawRoundRect(rLeft, sumTop, rRight, sumBottom, 8f, 8f, borderPaint)
                            canvas.drawText("إجمالي فواتير التوريد", rRight - 10f, sumTop + 20f, labelRightPaint)
                            canvas.drawText(
                                "${String.format(Locale.US, "%.2f", totalPurchases)} $currencySymbol",
                                rRight - 10f,
                                sumTop + 40f,
                                labelRightPaint
                            )

                            // Middle card: إجمالي المدفوع
                            val mRight = rLeft - 6f
                            val mLeft = mRight - boxW
                            canvas.drawRoundRect(mLeft, sumTop, mRight, sumBottom, 8f, 8f, cardFillPaint)
                            canvas.drawRoundRect(mLeft, sumTop, mRight, sumBottom, 8f, 8f, borderPaint)
                            canvas.drawText("إجمالي المدفوع للمورد", mRight - 10f, sumTop + 20f, labelRightPaint)
                            canvas.drawText(
                                "${String.format(Locale.US, "%.2f", totalPaid)} $currencySymbol",
                                mRight - 10f,
                                sumTop + 40f,
                                greenAmountPaint
                            )

                            // Left card: الرصيد المتبقي للمورد
                            val lRight = mLeft - 6f
                            val lLeft = leftEdge + 6f
                            canvas.drawRoundRect(lLeft, sumTop, lRight, sumBottom, 8f, 8f, cardFillPaint)
                            canvas.drawRoundRect(lLeft, sumTop, lRight, sumBottom, 8f, 8f, borderPaint)
                            canvas.drawText("المتبقي للمورد", lRight - 10f, sumTop + 20f, labelRightPaint)
                            canvas.drawText(
                                "${String.format(Locale.US, "%.2f", totalRemaining)} $currencySymbol",
                                lRight - 10f,
                                sumTop + 40f,
                                Paint(amberAmountPaint).apply { textAlign = Paint.Align.RIGHT }
                            )

                            y = sumBottom + 16f
                        } else {
                            canvas.drawText(
                                "${storeName.ifBlank { "المتجر" }} — كشف حساب المورد: $supplierName (تابع)",
                                centerX,
                                y,
                                subTitlePaint
                            )
                            y += 20f
                        }

                        // Table Header
                        val tableLeft = leftEdge + 6f
                        val tableRight = rightEdge - 6f
                        val headerHeight = 26f
                        canvas.drawRoundRect(tableLeft, y, tableRight, y + headerHeight, 6f, 6f, headerFillPaint)

                        val colIdxRight = tableRight - 10f
                        val colInvNumRight = tableRight - 38f
                        val colDateRight = tableRight - 135f
                        val colTotalRight = tableRight - 235f
                        val colPaidRight = tableRight - 340f
                        val colRemLeft = tableLeft + 12f

                        val headerTextY = y + 17f
                        canvas.drawText("#", colIdxRight, headerTextY, tableHeaderPaint)
                        canvas.drawText("رقم الفاتورة", colInvNumRight, headerTextY, tableHeaderPaint)
                        canvas.drawText("التاريخ", colDateRight, headerTextY, tableHeaderPaint)
                        canvas.drawText("الإجمالي", colTotalRight, headerTextY, tableHeaderPaint)
                        canvas.drawText("المدفوع", colPaidRight, headerTextY, tableHeaderPaint)
                        canvas.drawText(
                            "المتبقي ($currencySymbol)",
                            colRemLeft,
                            headerTextY,
                            Paint(tableHeaderPaint).apply { textAlign = Paint.Align.LEFT }
                        )

                        y += headerHeight

                        if (pageInvoices.isEmpty()) {
                            val emptyRowH = 44f
                            canvas.drawRect(tableLeft, y, tableRight, y + emptyRowH, borderPaint)
                            val emptyP = Paint(cellTextPaint).apply { textAlign = Paint.Align.CENTER }
                            canvas.drawText("لا توجد فواتير مسجلة لهذا المورد حتى الآن.", centerX, y + 26f, emptyP)
                            y += emptyRowH
                        } else {
                            val rowHeight = 26f
                            pageInvoices.forEachIndexed { idx, inv ->
                                val rowTop = y
                                val rowBottom = y + rowHeight
                                if (idx % 2 == 1) {
                                    canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, zebraFillPaint)
                                }
                                canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, borderPaint)

                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(inv.dateMillis)).toEnglishDigits()
                                val rowTextY = rowTop + 17f

                                canvas.drawText(runningIndex.toString(), colIdxRight, rowTextY, cellTextPaint)
                                canvas.drawText(inv.invoiceNumber, colInvNumRight, rowTextY, cellTextPaint)
                                canvas.drawText(dateStr, colDateRight, rowTextY, cellTextPaint)
                                canvas.drawText(String.format(Locale.US, "%.2f", inv.totalCost), colTotalRight, rowTextY, cellTextPaint)
                                canvas.drawText(String.format(Locale.US, "%.2f", inv.paidAmount), colPaidRight, rowTextY, greenAmountPaint)
                                canvas.drawText(
                                    "${String.format(Locale.US, "%.2f", inv.remainingDebt)} $currencySymbol",
                                    colRemLeft,
                                    rowTextY,
                                    if (inv.remainingDebt > 0.009) amberAmountPaint else Paint(greenAmountPaint).apply { textAlign = Paint.Align.LEFT }
                                )

                                runningIndex++
                                y += rowHeight
                            }
                        }

                        if (pageNumber == totalPages) {
                            val sigY = (y + 44f).coerceAtMost(pageHeight - 75f)
                            canvas.drawText("توقيع إدارة المتجر: ............................", rightEdge - 20f, sigY, labelRightPaint)
                            canvas.drawText("توقيع المورد للمطابقة: ............................", leftEdge + 20f, sigY, valueLeftPaint)

                            if (footerText.isNotBlank()) {
                                val fPaint = Paint(cellTextPaint).apply { textAlign = Paint.Align.CENTER }
                                canvas.drawText(footerText, centerX, (sigY + 22f).coerceAtMost(pageHeight - 52f), fPaint)
                            }
                        }

                        val pageNumPaint = Paint(cellTextPaint).apply {
                            textAlign = Paint.Align.CENTER
                            color = android.graphics.Color.rgb(100, 116, 139)
                        }
                        canvas.drawText(
                            "صفحة $pageNumber من $totalPages • ${storeName.ifBlank { "المتجر" }}",
                            centerX,
                            pageHeight - 36f,
                            pageNumPaint
                        )

                        pdfDoc.finishPage(page)
                    }

                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc.writeTo(out)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message ?: "خطأ أثناء إنشاء ملف PDF لكشف حساب المورد")
                } finally {
                    pdfDoc.close()
                }
            }
        },
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()
        )
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح نافذة الطباعة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}



