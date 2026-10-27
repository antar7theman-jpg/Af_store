package com.example.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoiceItem
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import java.util.Locale

data class LowStockInAppAlertPayload(
    val id: String,
    val titleAr: String,
    val summaryAr: String,
    val affectedProductNames: List<String>,
    val totalLowCount: Int,
    val outOfStockCount: Int,
    val timestampMillis: Long = System.currentTimeMillis()
)

object LowStockNotificationManager {
    const val CHANNEL_ID = "af_store_low_stock_alerts_channel"
    private const val NOTIFICATION_ID_BASE = 88400

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات انخفاض المخزون وإعادة الطلب",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند انخفاض كمية أي سلعة في المخزون عن الحد الأدنى للطلب أو نفادها"
                enableVibration(true)
            }
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postLowStockSystemNotification(
        context: Context,
        storeName: String,
        newlyLowProducts: List<Product>,
        totalLowStockCount: Int,
        outOfStockCount: Int,
        getMinThreshold: (Int) -> Int
    ) {
        if (newlyLowProducts.isEmpty() && totalLowStockCount <= 0) return
        try {
            ensureNotificationChannel(context)
            if (!hasNotificationPermission(context)) return

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_reorder_alerts_screen", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                9912,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (outOfStockCount > 0) {
                "⚠️ تنبيه مخزون ($storeName): $totalLowStockCount أصناف تحتاج للطلب ($outOfStockCount نفدت)"
            } else {
                "📦 تنبيه إعادة الطلب ($storeName): $totalLowStockCount أصناف وصلت للحد الأدنى"
            }

            val previewItems = newlyLowProducts.take(4).joinToString(" ، ") { prod ->
                val minReq = getMinThreshold(prod.id)
                if (prod.stock <= 0) "${prod.name} (نفد 0/$minReq)" else "${prod.name} (${prod.stock}/$minReq)"
            }

            val expandedText = buildString {
                appendLine("الأصناف المتأثرة: $previewItems")
                if (newlyLowProducts.size > 4) {
                    appendLine("و ${newlyLowProducts.size - 4} أصناف أخرى تحت الحد الأدنى.")
                }
                append("اضغط لفتح واجهة إدارة تنبيهات إعادة الطلب وإنشاء فاتورة شراء أو تزويد المخزون.")
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(previewItems)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 200).toInt(), notification)
        } catch (_: Exception) {
            // Safe fallback on restricted test environments
        }
    }
}

@Composable
fun TopBarLowStockBellButton(
    lowStockCount: Int,
    outOfStockCount: Int,
    isActiveScreen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = when {
        outOfStockCount > 0 -> Color(0xFFEF4444)
        lowStockCount > 0 -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                when {
                    isActiveScreen -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    lowStockCount > 0 -> badgeColor.copy(alpha = 0.14f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                }
            )
            .border(
                width = 1.dp,
                color = when {
                    isActiveScreen -> MaterialTheme.colorScheme.primary
                    lowStockCount > 0 -> badgeColor.copy(alpha = 0.65f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("top_bar_low_stock_bell_btn"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (lowStockCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                contentDescription = "تنبيهات إعادة الطلب",
                tint = if (lowStockCount > 0) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            if (lowStockCount > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeColor
                ) {
                    Text(
                        text = lowStockCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LowStockInAppNotificationBanner(
    alert: LowStockInAppAlertPayload,
    onOpenReorderScreen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = if (alert.outOfStockCount > 0) Color(0xFFEF4444) else Color(0xFFF59E0B)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.5.dp, accentColor),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("low_stock_in_app_notification_banner")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.22f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.titleAr,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = alert.summaryAr,
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("dismiss_low_stock_banner_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق التنبيه",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("تجاهل الآن", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onOpenReorderScreen()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("open_reorder_screen_from_banner_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إدارة طلبات الشراء (${alert.totalLowCount})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private enum class ReorderAlertFilter(val labelAr: String) {
    ACTIVE_ALERTS("التنبيهات النشطة"),
    OUT_OF_STOCK("نفد المخزون (0)"),
    LOW_STOCK("تحت الحد الأدنى"),
    CUSTOM_RULES("مخصصة الحد"),
    MUTED("مكتومة الإشعار"),
    ALL_PRODUCTS("كل الأصناف")
}

@Composable
fun ReorderAlertsManagementScreen(
    viewModel: AntarSalesViewModel,
    onNavigateToPurchases: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val semantic = LocalAFStoreSemanticColors.current
    val isDark = isSystemInDarkTheme()

    val allProducts by viewModel.products.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val defaultThreshold by viewModel.defaultLowStockThreshold.collectAsState()
    val customMinMap by viewModel.productCustomMinStock.collectAsState()
    val customReorderQtyMap by viewModel.productCustomReorderQty.collectAsState()
    val mutedIds by viewModel.mutedLowStockProductIds.collectAsState()
    val notificationsEnabled by viewModel.lowStockNotificationsEnabled.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(ReorderAlertFilter.ACTIVE_ALERTS) }
    var showSettingsPanel by rememberSaveable { mutableStateOf(false) }
    var showReorderPdfReportDialog by remember { mutableStateOf(false) }
    var defaultThresholdInput by remember(defaultThreshold) {
        mutableStateOf(defaultThreshold.toString())
    }

    // Selected product IDs for batch Purchase Invoice / Supplier Order List
    var selectedProductIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Dialog states
    var editingProductRule by remember { mutableStateOf<Product?>(null) }
    var customRestockProduct by remember { mutableStateOf<Product?>(null) }
    var productsForPurchaseOrderModal by remember { mutableStateOf<List<Product>?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setLowStockNotificationsEnabled(true)
            viewModel.triggerTestLowStockNotification()
        } else {
            viewModel.showStatusMessage("يرجى السماح بإذن الإشعارات في الهاتف لاستلام تنبيهات انخفاض المخزون")
        }
    }

    // Compute counts & metrics
    val outOfStockProducts = remember(lowStockProducts) {
        lowStockProducts.filter { it.stock <= 0 }
    }
    val lowButAvailableProducts = remember(lowStockProducts) {
        lowStockProducts.filter { it.stock > 0 }
    }
    val estimatedTotalReorderCost = remember(
        lowStockProducts,
        defaultThreshold,
        customMinMap,
        customReorderQtyMap
    ) {
        lowStockProducts.sumOf { prod ->
            val minReq = customMinMap[prod.id] ?: defaultThreshold
            val suggestedQty = customReorderQtyMap[prod.id]
                ?: ((minReq * 2) - prod.stock).coerceAtLeast(prod.unitsPerCarton.coerceAtLeast(1))
            suggestedQty * prod.purchasePrice.coerceAtLeast(0.0)
        }
    }

    val filteredProducts = remember(
        allProducts,
        lowStockProducts,
        selectedFilter,
        searchQuery,
        defaultThreshold,
        customMinMap,
        mutedIds
    ) {
        val baseList = when (selectedFilter) {
            ReorderAlertFilter.ACTIVE_ALERTS -> lowStockProducts
            ReorderAlertFilter.OUT_OF_STOCK -> lowStockProducts.filter { it.stock <= 0 }
            ReorderAlertFilter.LOW_STOCK -> lowStockProducts.filter { it.stock > 0 }
            ReorderAlertFilter.CUSTOM_RULES -> allProducts.filter {
                customMinMap.containsKey(it.id) || customReorderQtyMap.containsKey(it.id)
            }
            ReorderAlertFilter.MUTED -> allProducts.filter { mutedIds.contains(it.id) }
            ReorderAlertFilter.ALL_PRODUCTS -> allProducts.sortedBy { it.stock }
        }
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            baseList
        } else {
            baseList.filter { p ->
                p.name.contains(q, ignoreCase = true) ||
                    p.barcode.contains(q, ignoreCase = true) ||
                    p.cartonBarcode.contains(q, ignoreCase = true)
            }
        }
    }

    // Keep selection clean and provide quick batch list
    val activeSelectedProducts = remember(allProducts, selectedProductIds) {
        allProducts.filter { selectedProductIds.contains(it.id) }
    }

    val targetOrderProducts = remember(activeSelectedProducts, lowStockProducts) {
        if (activeSelectedProducts.isNotEmpty()) activeSelectedProducts else lowStockProducts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("reorder_alerts_management_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row 1: Full Title and Subtitle with Icon (Right on user's side)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "تنبيهات المخزون",
                            tint = Color(0xFFEF4444),
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
                        text = "إدارة تنبيهات المخزون وإعادة الطلب",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )
                    Text(
                        text = "مراقبة النواقص الفورية وإصدار طلبيات الشراء للموردين",
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

        // Header Row 2: Action Buttons (PDF Reports & Alert Settings)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UniversalReportHeaderButton(
                title = "التقارير PDF",
                tag = "reorder_pdf_report_btn",
                onClick = { showReorderPdfReportDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            )

            FilledTonalButton(
                onClick = { showSettingsPanel = !showSettingsPanel },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("toggle_alert_settings_panel_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (showSettingsPanel) "إخفاء الإعدادات" else "إعدادات التنبيه",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Executive KPI Summary Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReorderKpiMiniCard(
                title = "تنبيهات نشطة",
                value = "${lowStockProducts.size} صنف",
                accentColor = if (lowStockProducts.isNotEmpty()) Color(0xFFF59E0B) else Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            ReorderKpiMiniCard(
                title = "نفد تماماً (0)",
                value = "${outOfStockProducts.size} صنف",
                accentColor = if (outOfStockProducts.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            ReorderKpiMiniCard(
                title = "تحت الحد",
                value = "${lowButAvailableProducts.size} صنف",
                accentColor = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            ReorderKpiMiniCard(
                title = "تكلفة الطلبية",
                value = "${String.format(Locale.US, "%.0f", estimatedTotalReorderCost)} $currencySymbol",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1.2f)
            )
        }

        // 3. Collapsible Global Alert & System Notification Settings Panel
        AnimatedVisibility(visible = showSettingsPanel) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reorder_global_settings_panel")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إشعارات نظام أندرويد عند انخفاض المخزون",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "إرسال إشعار فوري في شريط الهاتف وشريط التنبيه العلوي فور وصول أي سلعة للحد الأدنى",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !LowStockNotificationManager.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setLowStockNotificationsEnabled(enabled)
                                }
                            },
                            modifier = Modifier.testTag("low_stock_system_notification_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = defaultThresholdInput,
                            onValueChange = { defaultThresholdInput = it },
                            label = { Text("الحد الأدنى العام للطلب (افتراضي)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reorder_screen_default_threshold_input")
                        )
                        Button(
                            onClick = {
                                val parsed = defaultThresholdInput.trim().toIntOrNull()
                                if (parsed != null && parsed >= 1) {
                                    viewModel.updateDefaultLowStockThreshold(parsed)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
                            modifier = Modifier.testTag("save_default_threshold_btn")
                        ) {
                            Text("حفظ الحد العام", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !LowStockNotificationManager.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.triggerTestLowStockNotification()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("test_low_stock_notification_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تجربة إرسال إشعار فوري الآن", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        if (mutedIds.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.unmuteAllProductAlerts() }
                            ) {
                                Text(
                                    text = "إلغاء كتم الكل (${mutedIds.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Search & Filter Chips Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث باسم السلعة أو الباركود لضبط حد التنبيه...", fontSize = 12.5.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح البحث", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("reorder_alerts_search_input")
            )

            Surface(
                onClick = {
                    val visibleIds = filteredProducts.map { it.id }.toSet()
                    selectedProductIds = if (visibleIds.isNotEmpty() && visibleIds.all { selectedProductIds.contains(it) }) {
                        selectedProductIds - visibleIds
                    } else {
                        selectedProductIds + visibleIds
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("reorder_select_all_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "تحديد أو إلغاء تحديد الكل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReorderAlertFilter.entries.forEach { filter ->
                val countBadge = when (filter) {
                    ReorderAlertFilter.ACTIVE_ALERTS -> lowStockProducts.size
                    ReorderAlertFilter.OUT_OF_STOCK -> outOfStockProducts.size
                    ReorderAlertFilter.LOW_STOCK -> lowButAvailableProducts.size
                    ReorderAlertFilter.CUSTOM_RULES -> allProducts.count {
                        customMinMap.containsKey(it.id) || customReorderQtyMap.containsKey(it.id)
                    }
                    ReorderAlertFilter.MUTED -> mutedIds.size
                    ReorderAlertFilter.ALL_PRODUCTS -> allProducts.size
                }
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = "${filter.labelAr} ($countBadge)",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("reorder_filter_${filter.name.lowercase(Locale.US)}")
                )
            }
        }

        // 5. Batch Action Bar (Create Purchase Invoice + Share Order Sheet)
        if (targetOrderProducts.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                shadowElevation = 3.dp,
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
                            text = if (activeSelectedProducts.isNotEmpty()) {
                                "تم تحديد ${activeSelectedProducts.size} صنف للطلب"
                            } else {
                                "جاهز لطلب ${targetOrderProducts.size} صنف من النواقص"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "حوّل النواقص لفاتورة مشتريات رسمية أو شاركها مع المورد",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val textSheet = buildSupplierReorderTextSheet(
                                    storeName = storeName,
                                    products = targetOrderProducts,
                                    defaultThreshold = defaultThreshold,
                                    customMinMap = customMinMap,
                                    customReorderQtyMap = customReorderQtyMap
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, textSheet)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة طلبية النواقص مع المورد"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB), // Royal Blue
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("share_reorder_list_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                productsForPurchaseOrderModal = targetOrderProducts
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669), // Emerald Green
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("create_reorder_purchase_invoice_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "إنشاء فاتورة شراء (${targetOrderProducts.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
        }

        // 6. Product Alert Cards List
        if (filteredProducts.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (selectedFilter == ReorderAlertFilter.ACTIVE_ALERTS) {
                            "المخزون مكتمل! جميع الأصناف فوق الحد الأدنى للطلب ✅"
                        } else {
                            "لا توجد أصناف مطابقة للفلتر المحدد حالياً"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يمكنك التبديل إلى تبويب «كل الأصناف» لتخصيص الحد الأدنى وكمية إعادة الطلب لأي سلعة مسبقاً.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = filteredProducts,
                    key = { "reorder_prod_${it.id}" }
                ) { product ->
                    val minThreshold = customMinMap[product.id] ?: defaultThreshold
                    val hasCustomMin = customMinMap.containsKey(product.id)
                    val isMuted = mutedIds.contains(product.id)
                    val suggestedQty = viewModel.getEffectiveReorderQuantity(product)
                    val hasCustomOrderQty = customReorderQtyMap.containsKey(product.id)
                    val isSelected = selectedProductIds.contains(product.id)

                    ReorderProductAlertCard(
                        product = product,
                        minThreshold = minThreshold,
                        hasCustomMin = hasCustomMin,
                        suggestedReorderQty = suggestedQty,
                        hasCustomOrderQty = hasCustomOrderQty,
                        isMuted = isMuted,
                        isSelected = isSelected,
                        currencySymbol = currencySymbol,
                        onToggleSelect = {
                            selectedProductIds = if (isSelected) {
                                selectedProductIds - product.id
                            } else {
                                selectedProductIds + product.id
                            }
                        },
                        onOpenRuleEditor = {
                            editingProductRule = product
                        },
                        onQuickRestockDefault = {
                            val packUnits = product.unitsPerCarton.coerceAtLeast(1)
                            val addQty = if (packUnits > 1) packUnits else suggestedQty.coerceAtLeast(5)
                            viewModel.quickRestockProduct(product, addQty)
                        },
                        onOpenCustomRestock = {
                            customRestockProduct = product
                        },
                        onCreateSinglePurchaseOrder = {
                            productsForPurchaseOrderModal = listOf(product)
                        }
                    )
                }
            }
        }
    }

    // Rule Editor Dialog (Custom Min Threshold + Preferred Reorder Qty + Mute Toggle)
    editingProductRule?.let { targetProduct ->
        ProductAlertRuleEditorDialog(
            product = targetProduct,
            defaultLowStockThreshold = defaultThreshold,
            initialCustomMin = customMinMap[targetProduct.id],
            initialCustomReorderQty = customReorderQtyMap[targetProduct.id],
            initialMuted = mutedIds.contains(targetProduct.id),
            onDismiss = { editingProductRule = null },
            onSaveRule = { customMin, customOrderQty, muted ->
                viewModel.saveProductAlertRule(
                    productId = targetProduct.id,
                    customMinStock = customMin,
                    customReorderQty = customOrderQty,
                    muted = muted
                )
                editingProductRule = null
            }
        )
    }

    // Custom Quick Restock Quantity Modal
    customRestockProduct?.let { targetProduct ->
        QuickCustomRestockDialog(
            product = targetProduct,
            suggestedQuantity = viewModel.getEffectiveReorderQuantity(targetProduct),
            onDismiss = { customRestockProduct = null },
            onConfirmRestock = { addedUnits ->
                viewModel.quickRestockProduct(targetProduct, addedUnits)
                customRestockProduct = null
            }
        )
    }

    // Create Official Purchase Invoice from Selected Reorder Alerts Modal
    productsForPurchaseOrderModal?.let { orderProducts ->
        CreateReorderPurchaseInvoiceDialog(
            products = orderProducts,
            currencySymbol = currencySymbol,
            getSuggestedOrderQty = { prod -> viewModel.getEffectiveReorderQuantity(prod) },
            onDismiss = { productsForPurchaseOrderModal = null },
            onConfirmPurchaseInvoice = { supplierName, supplierPhone, paidAmount, notes, items ->
                viewModel.createPurchaseInvoice(
                    supplierName = supplierName,
                    supplierPhone = supplierPhone,
                    paidAmount = paidAmount,
                    notes = notes,
                    items = items
                )
                selectedProductIds = emptySet()
                productsForPurchaseOrderModal = null
                onNavigateToPurchases?.invoke()
            }
        )
    }

    if (showReorderPdfReportDialog) {
        val payload = remember(storeName, storePhone, currencySymbol, lowStockProducts, customMinMap, defaultThreshold) {
            UniversalPdfReportExporter.buildReorderAlertsPayload(
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol,
                lowStockProducts = lowStockProducts,
                customMinStock = customMinMap,
                defaultThreshold = defaultThreshold
            )
        }
        UniversalSectionPdfReportDialog(
            payload = payload,
            onDismiss = { showReorderPdfReportDialog = false }
        )
    }
}

@Composable
private fun ReorderKpiMiniCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReorderProductAlertCard(
    product: Product,
    minThreshold: Int,
    hasCustomMin: Boolean,
    suggestedReorderQty: Int,
    hasCustomOrderQty: Boolean,
    isMuted: Boolean,
    isSelected: Boolean,
    currencySymbol: String,
    onToggleSelect: () -> Unit,
    onOpenRuleEditor: () -> Unit,
    onQuickRestockDefault: () -> Unit,
    onOpenCustomRestock: () -> Unit,
    onCreateSinglePurchaseOrder: () -> Unit
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock <= minThreshold
    val statusColor = when {
        isOutOfStock -> Color(0xFFEF4444)
        isLowStock -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }
    val estimatedCost = suggestedReorderQty * product.purchasePrice.coerceAtLeast(0.0)

    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 1.6.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else statusColor.copy(alpha = 0.55f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reorder_alert_card_${product.id}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isMuted) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "مكتوم",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        if (product.barcode.isNotBlank()) {
                            Text(
                                text = "باركود: ${product.barcode}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = when {
                            isOutOfStock -> "نفد المخزون (0)"
                            isLowStock -> "متبقي: ${product.stock} ق"
                            else -> "متوفر: ${product.stock} ق"
                        },
                        color = statusColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            // Metrics strip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حد التنبيه: $minThreshold ق" + if (hasCustomMin) " (مخصص)" else " (عام)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasCustomMin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "كمية الطلب: $suggestedReorderQty ق" + if (hasCustomOrderQty) " ★" else "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "التكلفة: ${String.format(Locale.US, "%.0f", estimatedCost)} $currencySymbol",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenRuleEditor,
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("edit_threshold_btn_${product.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ضبط الحد ($minThreshold)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onQuickRestockDefault,
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("quick_restock_btn_${product.id}")
                ) {
                    val packUnits = product.unitsPerCarton.coerceAtLeast(1)
                    val addQty = if (packUnits > 1) packUnits else suggestedReorderQty.coerceAtLeast(5)
                    Text(
                        text = if (packUnits > 1) "تزويد +عبوة ($addQty)" else "تزويد +$addQty ق",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onOpenCustomRestock,
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("custom_restock_btn_${product.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "تزويد كمية مخصصة", modifier = Modifier.size(14.dp))
                    Text("كمية", fontSize = 11.sp)
                }

                IconButton(
                    onClick = onCreateSinglePurchaseOrder,
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(9.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF10B981).copy(alpha = 0.55f),
                            shape = RoundedCornerShape(9.dp)
                        )
                        .testTag("single_purchase_order_btn_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "إنشاء فاتورة شراء للصنف",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductAlertRuleEditorDialog(
    product: Product,
    defaultLowStockThreshold: Int,
    initialCustomMin: Int?,
    initialCustomReorderQty: Int?,
    initialMuted: Boolean,
    onDismiss: () -> Unit,
    onSaveRule: (customMinStock: Int?, customReorderQty: Int?, muted: Boolean) -> Unit
) {
    var minStockInput by remember(product.id, initialCustomMin) {
        mutableStateOf(initialCustomMin?.toString() ?: "")
    }
    var reorderQtyInput by remember(product.id, initialCustomReorderQty) {
        mutableStateOf(initialCustomReorderQty?.toString() ?: "")
    }
    var isMuted by remember(product.id, initialMuted) {
        mutableStateOf(initialMuted)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "إعدادات تنبيه الطلب للصنف",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp
                    )
                    Text(
                        text = product.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "الرصيد الحالي في المخزون: ${product.stock} قطعة • العبوة: ${product.unitsPerCarton} ق",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = minStockInput,
                    onValueChange = { minStockInput = it },
                    label = { Text("الحد الأدنى المخصص للتنبيه") },
                    placeholder = { Text("اتركه فارغاً لاستخدام الحد العام ($defaultLowStockThreshold)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_editor_min_stock_input")
                )

                OutlinedTextField(
                    value = reorderQtyInput,
                    onValueChange = { reorderQtyInput = it },
                    label = { Text("كمية إعادة الطلب المفضلة") },
                    placeholder = { Text("اتركه فارغاً للحساب التلقائي") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_editor_reorder_qty_input")
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isMuted) Color(0xFFEF4444).copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        if (isMuted) Color(0xFFEF4444).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isMuted) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "كتم تنبيهات هذا الصنف مؤقتاً",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "إيقاف إشعارات انخفاض المخزون لهذه السلعة فقط",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isMuted,
                            onCheckedChange = { isMuted = it },
                            modifier = Modifier.testTag("rule_editor_mute_switch")
                        )
                    }
                }

                if (initialCustomMin != null || initialCustomReorderQty != null || initialMuted) {
                    TextButton(
                        onClick = {
                            onSaveRule(null, null, false)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("إعادة الضبط للإعدادات الافتراضية", fontSize = 11.5.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedMin = minStockInput.trim().toIntOrNull()?.coerceAtLeast(0)
                    val parsedOrderQty = reorderQtyInput.trim().toIntOrNull()?.coerceAtLeast(1)
                    onSaveRule(parsedMin, parsedOrderQty, isMuted)
                },
                modifier = Modifier.testTag("save_product_alert_rule_btn")
            ) {
                Text("حفظ الإعدادات", fontWeight = FontWeight.Bold)
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
private fun QuickCustomRestockDialog(
    product: Product,
    suggestedQuantity: Int,
    onDismiss: () -> Unit,
    onConfirmRestock: (Int) -> Unit
) {
    var qtyInput by remember(product.id) {
        mutableStateOf(suggestedQuantity.coerceAtLeast(1).toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "تزويد سريع للمخزون: ${product.name}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "الرصيد الحالي: ${product.stock} ق • كمية الطلب المقترحة: $suggestedQuantity ق",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = { qtyInput = it },
                    label = { Text("الكمية المراد إضافتها للمخزون (بالقطعة)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_restock_qty_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyInput.trim().toIntOrNull() ?: 0
                    if (qty > 0) {
                        onConfirmRestock(qty)
                    }
                },
                modifier = Modifier.testTag("confirm_custom_restock_btn")
            ) {
                Text("إضافة للمخزون فوراً", fontWeight = FontWeight.Bold)
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
private fun CreateReorderPurchaseInvoiceDialog(
    products: List<Product>,
    currencySymbol: String,
    getSuggestedOrderQty: (Product) -> Int,
    onDismiss: () -> Unit,
    onConfirmPurchaseInvoice: (
        supplierName: String,
        supplierPhone: String,
        paidAmount: Double,
        notes: String,
        items: List<PurchaseInvoiceItem>
    ) -> Unit
) {
    var supplierName by rememberSaveable { mutableStateOf("مورد عام - طلبية نواقص") }
    var supplierPhone by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("توريد نواقص المخزون من واجهة تنبيهات إعادة الطلب") }
    var isFullyPaid by rememberSaveable { mutableStateOf(true) }
    var customPaidInput by rememberSaveable { mutableStateOf("") }

    val qtyMap = remember(products) {
        mutableStateMapOf<Int, String>().apply {
            products.forEach { p ->
                put(p.id, getSuggestedOrderQty(p).coerceAtLeast(1).toString())
            }
        }
    }
    val costMap = remember(products) {
        mutableStateMapOf<Int, String>().apply {
            products.forEach { p ->
                val defaultCost = if (p.purchasePrice > 0.0) p.purchasePrice else (p.price * 0.75)
                put(p.id, String.format(Locale.US, "%.2f", defaultCost))
            }
        }
    }

    val totalInvoiceCost = products.sumOf { p ->
        val q = qtyMap[p.id]?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val c = costMap[p.id]?.trim()?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
        q * c
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = Color(0xFF10B981)
                )
                Column {
                    Text(
                        text = "إنشاء فاتورة شراء للنواقص (${products.size} صنف)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp
                    )
                    Text(
                        text = "سيتم حفظ فاتورة المشتريات وزيادة كميات المخزون تلقائياً",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("اسم المورد") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reorder_purchase_supplier_name_input")
                )

                OutlinedTextField(
                    value = supplierPhone,
                    onValueChange = { supplierPhone = it },
                    label = { Text("رقم هاتف المورد (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "الأصناف والكميات المطلوبة:",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    products.forEach { prod ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = prod.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                    Text(
                                        text = "المتوفر: ${prod.stock} ق",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = qtyMap[prod.id] ?: "1",
                                        onValueChange = { qtyMap[prod.id] = it },
                                        label = { Text("الكمية") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = costMap[prod.id] ?: "0",
                                        onValueChange = { costMap[prod.id] = it },
                                        label = { Text("تكلفة الوحدة ($currencySymbol)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إجمالي فاتورة الشراء:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalInvoiceCost)} $currencySymbol",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مدفوعة نقداً بالكامل للمورد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Switch(
                        checked = isFullyPaid,
                        onCheckedChange = { isFullyPaid = it }
                    )
                }

                if (!isFullyPaid) {
                    OutlinedTextField(
                        value = customPaidInput,
                        onValueChange = { customPaidInput = it },
                        label = { Text("المبلغ المدفوع الآن (اتركه 0 للآجل بالكامل)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات فاتورة الشراء") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val builtItems = products.mapNotNull { prod ->
                        val q = qtyMap[prod.id]?.trim()?.toIntOrNull() ?: 0
                        val unitCost = costMap[prod.id]?.trim()?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        if (q <= 0) {
                            null
                        } else {
                            PurchaseInvoiceItem(
                                productId = prod.id,
                                productName = prod.name,
                                quantity = q,
                                unitCost = unitCost,
                                sellingPrice = prod.price.coerceAtLeast(unitCost),
                                subtotal = q * unitCost,
                                isCarton = false,
                                unitsPerCarton = prod.unitsPerCarton.coerceAtLeast(1),
                                barcode = prod.barcode,
                                cartonBarcode = prod.cartonBarcode,
                                imagePath = prod.imagePath,
                                isNewProduct = false
                            )
                        }
                    }
                    if (builtItems.isNotEmpty()) {
                        val total = builtItems.sumOf { it.subtotal }
                        val resolvedPaid = if (isFullyPaid) {
                            total
                        } else {
                            (customPaidInput.trim().toDoubleOrNull() ?: 0.0).coerceIn(0.0, total)
                        }
                        onConfirmPurchaseInvoice(
                            supplierName.trim().ifBlank { "مورد عام" },
                            supplierPhone.trim(),
                            resolvedPaid,
                            notes.trim(),
                            builtItems
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.testTag("confirm_reorder_purchase_invoice_btn")
            ) {
                Text("اعتماد الفاتورة وتزويد المخزون", fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

fun buildSupplierReorderTextSheet(
    storeName: String,
    products: List<Product>,
    defaultThreshold: Int,
    customMinMap: Map<Int, Int>,
    customReorderQtyMap: Map<Int, Int>
): String {
    return buildString {
        appendLine("📋 كشف طلبية نواقص المخزون - $storeName")
        appendLine("==============================")
        products.forEachIndexed { idx, prod ->
            val minReq = customMinMap[prod.id] ?: defaultThreshold
            val orderQty = customReorderQtyMap[prod.id]
                ?: ((minReq * 2) - prod.stock).coerceAtLeast(prod.unitsPerCarton.coerceAtLeast(1))
            val statusLabel = if (prod.stock <= 0) "نفد (0)" else "متبقي ${prod.stock} ق"
            appendLine("${idx + 1}. ${prod.name} | الحالة: $statusLabel (الحد: $minReq) | الكمية المطلوبة: $orderQty ق")
        }
        appendLine("==============================")
        append("إجمالي الأصناف المطلوبة: ${products.size} صنف")
    }
}
