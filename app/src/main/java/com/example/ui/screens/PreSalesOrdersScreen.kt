package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ArchivedInvoice
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreSalesOrdersScreen(
    viewModel: AntarSalesViewModel,
    onNavigateToNewPreSale: () -> Unit = {}
) {
    val context = LocalContext.current
    val semantic = LocalAFStoreSemanticColors.current
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val allPreSaleOrders by viewModel.preSaleOrders.collectAsState()
    val pendingCount by viewModel.pendingPreSalesCount.collectAsState()

    var selectedTabItem by rememberSaveable { mutableIntStateOf(0) } // 0=Pending, 1=Completed, 2=Cancelled, 3=All
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Dialog states
    var orderToFulfill by remember { mutableStateOf<ArchivedInvoice?>(null) }
    var orderToCancel by remember { mutableStateOf<ArchivedInvoice?>(null) }
    var previewInvoiceModal by remember { mutableStateOf<ArchivedInvoice?>(null) }

    val pendingOrders = remember(allPreSaleOrders) { allPreSaleOrders.filter { it.isPendingPreSale } }
    val completedOrders = remember(allPreSaleOrders) { allPreSaleOrders.filter { it.isCompletedPreSale } }
    val cancelledOrders = remember(allPreSaleOrders) { allPreSaleOrders.filter { it.isCancelledPreSale } }

    val totalReservedValue = remember(pendingOrders) { pendingOrders.sumOf { it.subtotal } }

    val tabFilteredOrders = remember(selectedTabItem, pendingOrders, completedOrders, cancelledOrders, allPreSaleOrders) {
        when (selectedTabItem) {
            0 -> pendingOrders
            1 -> completedOrders
            2 -> cancelledOrders
            else -> allPreSaleOrders
        }
    }

    val displayOrders = remember(tabFilteredOrders, searchQuery) {
        if (searchQuery.isBlank()) {
            tabFilteredOrders
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            tabFilteredOrders.filter { order ->
                order.invoiceId.lowercase(Locale.ROOT).contains(q) ||
                        order.customerName.lowercase(Locale.ROOT).contains(q) ||
                        order.notes.lowercase(Locale.ROOT).contains(q) ||
                        order.items.any { it.productName.lowercase(Locale.ROOT).contains(q) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("pre_sales_orders_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header with Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "طلبات البيع المسبق للمستودع",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "إدارة وحجز طلبيات المحلات لتسليمها وخروجها من المستودع في اليوم التالي",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onNavigateToNewPreSale,
                colors = ButtonDefaults.buttonColors(
                    containerColor = semantic.profitGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("new_pre_sale_order_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "طلب مسبق جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Summary KPI Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pending Orders Card
            Card(
                colors = CardDefaults.cardColors(containerColor = semantic.debtAmber.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = semantic.debtAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "طلبات معلقة",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = semantic.debtAmber
                        )
                    }
                    Text(
                        text = "$pendingCount طلبية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Total Reserved Value Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.3f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "القيمة المحجوزة بالمستودع",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", totalReservedValue)} $currencySymbol",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Completed Deliveries Card
            Card(
                colors = CardDefaults.cardColors(containerColor = semantic.profitGreen.copy(alpha = 0.10f)),
                border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.40f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = semantic.profitGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "تم التسليم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = semantic.profitGreen
                        )
                    }
                    Text(
                        text = "${completedOrders.size} طلبية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث برقم الطلب، اسم العميل، الصنف...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("pre_sales_search_input")
        )

        // Filter Tabs Row
        val tabs = listOf(
            "المعلقة ⏳ (${pendingOrders.size})",
            "المُسَلَّمة ✓ (${completedOrders.size})",
            "الملغاة ✕ (${cancelledOrders.size})",
            "الكل (${allPreSaleOrders.size})"
        )
        ScrollableTabRow(
            selectedTabIndex = selectedTabItem,
            edgePadding = 0.dp,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabItem == index,
                    onClick = { selectedTabItem = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabItem == index) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (selectedTabItem == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Orders List
        if (displayOrders.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة لبحثك" else "لا توجد طلبيات بيع مسبق في هذا القسم",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "يمكن للبائعين تسجيل طلبيات المحلات مسبقاً وتأكيد حجزها للمستودع للخروج في اليوم التالي.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onNavigateToNewPreSale,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء أول طلبية بيع مسبق الآن")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(displayOrders, key = { it.invoiceId }) { order ->
                    PreSaleOrderCard(
                        order = order,
                        currencySymbol = currencySymbol,
                        onFulfill = { orderToFulfill = order },
                        onCancel = { orderToCancel = order },
                        onPrintVoucher = { previewInvoiceModal = order },
                        onEditInCart = {
                            viewModel.restorePreSaleOrderToCart(order)
                            onNavigateToNewPreSale()
                        }
                    )
                }
            }
        }
    }

    // Modal: Fulfill / Confirm Delivery & Stock Deduction
    if (orderToFulfill != null) {
        val targetOrder = orderToFulfill!!
        var paymentMethod by remember { mutableStateOf("CASH") }
        var isDebt by remember { mutableStateOf(targetOrder.isDebt) }

        AlertDialog(
            onDismissRequest = { orderToFulfill = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = semantic.profitGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تسليم الطلبية وخروج البضاعة",
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
                    Text(
                        text = "العميل: ${targetOrder.customerName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = "رقم الطلبية: #${targetOrder.invoiceId} • الإجمالي: ${String.format(Locale.US, "%.2f", targetOrder.subtotal)} $currencySymbol",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.profitGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ عند التأكيد، سيتم خصم الكميات (${targetOrder.totalQuantity} قطعة) فعلياً من مخزون المستودع وتحديث حالة الفاتورة كـ مكتملة، مع إمكانية طباعة وصل التسليم الرسمي (Bon de livraison).",
                            fontSize = 11.5.sp,
                            color = semantic.profitGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Text(
                        text = "طريقة الدفع عند الاستلام:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    paymentMethod = "CASH"
                                    isDebt = false
                                }
                        ) {
                            RadioButton(
                                selected = !isDebt,
                                onClick = {
                                    paymentMethod = "CASH"
                                    isDebt = false
                                }
                            )
                            Text("دفع نقدي (كاش)", fontSize = 12.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (targetOrder.customerId != null) {
                                        paymentMethod = "DEBT"
                                        isDebt = true
                                    } else {
                                        Toast.makeText(context, "الطلب بدون عميل مسجل، لا يمكن تسجيله ديناً", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            RadioButton(
                                selected = isDebt,
                                onClick = {
                                    if (targetOrder.customerId != null) {
                                        paymentMethod = "DEBT"
                                        isDebt = true
                                    } else {
                                        Toast.makeText(context, "الطلب بدون عميل مسجل، لا يمكن تسجيله ديناً", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            Text("آجل (دين على العميل)", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = targetOrder
                        orderToFulfill = null
                        viewModel.fulfillPreSaleOrder(
                            invoice = current,
                            finalPaymentMethod = paymentMethod,
                            isDebt = isDebt,
                            onSuccess = { completedInvoice ->
                                previewInvoiceModal = completedInvoice
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = semantic.profitGreen)
                ) {
                    Text("تأكيد التسليم وخصم المخزون", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToFulfill = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Modal: Cancel Pre-Sale Order
    if (orderToCancel != null) {
        val targetOrder = orderToCancel!!
        AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = {
                Text("إلغاء طلب البيع المسبق", fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في إلغاء طلب البيع المسبق رقم #${targetOrder.invoiceId} للعميل ${targetOrder.customerName}؟\nسيتم تحرير كافة البضائع المحجوزة بالمستودع وإلغاء الطلبية دون أي تأثير مالي.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = targetOrder
                        orderToCancel = null
                        viewModel.cancelPreSaleOrder(current)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الإلغاء وتحرير الحجز", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToCancel = null }) {
                    Text("رجوع")
                }
            }
        )
    }

    // Voucher / Receipt Modal
    if (previewInvoiceModal != null) {
        val invoice = previewInvoiceModal!!
        InvoiceTemplatePreviewDialog(
            viewModel = viewModel,
            archivedInvoice = invoice,
            isPreSavePreview = false,
            onDismiss = { previewInvoiceModal = null }
        )
    }
}

@Composable
fun PreSaleOrderCard(
    order: ArchivedInvoice,
    currencySymbol: String,
    onFulfill: () -> Unit,
    onCancel: () -> Unit,
    onPrintVoucher: () -> Unit,
    onEditInCart: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val formattedDate = remember(order.dateMillis) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(order.dateMillis))
    }

    val formattedDeliveryDate = remember(order.expectedDeliveryDate) {
        if (order.expectedDeliveryDate > 0) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(order.expectedDeliveryDate))
        } else {
            "في اليوم التالي"
        }
    }

    val statusColor = when (order.status) {
        "completed" -> semantic.profitGreen
        "cancelled" -> MaterialTheme.colorScheme.error
        else -> semantic.debtAmber
    }

    val statusLabel = when (order.status) {
        "completed" -> "تم التسليم وخروج البضاعة ✓"
        "cancelled" -> "طلب ملغي ✕"
        else -> "قيد التجهيز بالمستودع (محجوز) ⏳"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("pre_sale_card_${order.invoiceId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Code + Status Badge + Date
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "#${order.invoiceId}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Customer Name & Expected Delivery Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = order.customerName.ifBlank { "عميل طلب مسبق" },
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "التسليم: $formattedDeliveryDate",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notes if present
            if (order.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ملاحظات: ${order.notes}",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Items Count and Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = "${order.items.size} أصناف (${order.totalQuantity} ق)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.2f", order.subtotal)} $currencySymbol",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = semantic.profitGreen
                )
            }

            // Expandable Items Table
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(thickness = 0.8.dp, color = semantic.headerBorder)
                    order.items.forEach { saleItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${saleItem.productName}",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${saleItem.quantitySold} ق × ${String.format(Locale.US, "%.2f", saleItem.totalPrice / saleItem.quantitySold.coerceAtLeast(1))} = ${String.format(Locale.US, "%.2f", saleItem.totalPrice)} $currencySymbol",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.8.dp, color = semantic.headerBorder)
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (order.isPendingPreSale) {
                    // Fulfill & Deliver
                    Button(
                        onClick = onFulfill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = semantic.profitGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "تسليم وخصم المخزون", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Edit in Cart
                    OutlinedButton(
                        onClick = onEditInCart,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(38.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "تعديل", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Print Voucher
                    IconButton(
                        onClick = onPrintVoucher,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "طباعة وصل طلب مسبق", modifier = Modifier.size(19.dp))
                    }

                    // Cancel
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "إلغاء الطلبية",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                } else {
                    // Completed or Cancelled Order actions
                    OutlinedButton(
                        onClick = onPrintVoucher,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (order.isCompletedPreSale) "معاينة وصل التسليم الرسمي" else "معاينة تفاصيل الطلب",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
