package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.Product
import com.example.data.models.Sale
import com.example.ui.theme.LocalAFStoreSemanticColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class StagnantProductInfo(
    val product: Product,
    val lastSaleMillis: Long?,
    val daysSinceLastSale: Int,
    val totalUnitsSoldEver: Int,
    val frozenCostValue: Double,
    val frozenSaleValue: Double
)

fun computeStagnantProductsList(
    products: List<Product>,
    sales: List<Sale>,
    thresholdDays: Int = 30
): List<StagnantProductInfo> {
    val now = System.currentTimeMillis()
    val dayMillis = 24L * 3600_000L
    val activeSales = sales.filter { !it.status.equals("cancelled", ignoreCase = true) }

    val salesByProductId = activeSales.groupBy { it.productId }
    val salesByProductName = activeSales.groupBy { it.productName.trim().lowercase() }

    return products
        .filter { !it.isDeleted && it.stock > 0 }
        .mapNotNull { prod ->
            val matchedSales = (salesByProductId[prod.id].orEmpty() +
                salesByProductName[prod.name.trim().lowercase()].orEmpty())
                .distinctBy { it.saleId }

            val lastSaleMillis = matchedSales.maxOfOrNull { it.saleDate }
            val totalUnitsSold = matchedSales.sumOf { it.quantitySold }

            val daysSinceSale = if (lastSaleMillis != null && lastSaleMillis > 0L) {
                ((now - lastSaleMillis).coerceAtLeast(0L) / dayMillis).toInt()
            } else {
                // Never sold yet — compute days since product creation/update or treat as stagnant if threshold == 0 or never sold
                val refTime = prod.createdAt?.toDate()?.time ?: prod.updatedAt
                val daysSinceCreated = if (refTime > 1000L) {
                    ((now - refTime).coerceAtLeast(0L) / dayMillis).toInt()
                } else {
                    thresholdDays
                }
                maxOf(daysSinceCreated, thresholdDays)
            }

            if (lastSaleMillis == null || daysSinceSale >= thresholdDays) {
                StagnantProductInfo(
                    product = prod,
                    lastSaleMillis = lastSaleMillis,
                    daysSinceLastSale = daysSinceSale,
                    totalUnitsSoldEver = totalUnitsSold,
                    frozenCostValue = prod.purchasePrice * prod.stock,
                    frozenSaleValue = prod.price * prod.stock
                )
            } else {
                null
            }
        }
        .sortedWith(
            compareByDescending<StagnantProductInfo> { it.lastSaleMillis == null }
                .thenByDescending { it.daysSinceLastSale }
                .thenByDescending { it.frozenCostValue }
        )
}

/**
 * 1. شاشة ونافذة وضع الجرد السريع (الجرد الفعلي مقابل المسجل)
 * يمسح فيها المدير الباركود أو يبحث عن السلعة ويدخل الكمية الفعلية الموجودة على الرف،
 * فيحسب التطبيق تلقائياً الفرق (عجز أو فائض أو مطابق) ويُحدث المخزون ويزامنه مع العمال.
 */
@Composable
fun QuickStockAuditModal(
    products: List<Product>,
    currencySymbol: String,
    canViewCostPrices: Boolean,
    onApplyAuditAdjustments: (adjustments: Map<Int, Int>, auditNote: String) -> Unit,
    onPromptUnregisteredBarcode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "AUDITED", "DISCREPANCY", "UNAUDITED"
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var focusedProductId by remember { mutableStateOf<Int?>(null) }
    var auditSessionNote by remember { mutableStateOf("جرد فعلي للرفوف") }

    // Map of productId -> actual shelf count entered by manager
    val auditedCountsMap = remember { mutableStateMapOf<Int, Int>() }
    // Map of productId -> text input state
    val auditedTextMap = remember { mutableStateMapOf<Int, String>() }

    val focusedProduct = remember(products, focusedProductId) {
        focusedProductId?.let { id -> products.find { it.id == id } }
    }

    val filteredProducts = remember(products, searchQuery, selectedFilter, auditedCountsMap.toMap(), focusedProductId) {
        val q = searchQuery.trim()
        val base = products.filter { p ->
            val matchesQuery = q.isEmpty() ||
                p.name.contains(q, ignoreCase = true) ||
                p.barcode.contains(q, ignoreCase = true) ||
                p.cartonBarcode.contains(q, ignoreCase = true)
            val isAudited = auditedCountsMap.containsKey(p.id)
            val actual = auditedCountsMap[p.id]
            val hasDiscrepancy = actual != null && actual != p.stock
            val matchesFilter = when (selectedFilter) {
                "AUDITED" -> isAudited
                "DISCREPANCY" -> hasDiscrepancy
                "UNAUDITED" -> !isAudited
                else -> true
            }
            matchesQuery && matchesFilter
        }
        if (focusedProductId != null) {
            base.sortedByDescending { it.id == focusedProductId }
        } else {
            base
        }
    }

    val totalAuditedCount = auditedCountsMap.size
    var totalSurplusUnits = 0
    var totalShortageUnits = 0
    var totalSurplusCost = 0.0
    var totalShortageCost = 0.0

    auditedCountsMap.forEach { (prodId, actualCount) ->
        val prod = products.find { it.id == prodId }
        if (prod != null) {
            val diff = actualCount - prod.stock
            if (diff > 0) {
                totalSurplusUnits += diff
                totalSurplusCost += diff * prod.purchasePrice
            } else if (diff < 0) {
                val absDiff = abs(diff)
                totalShortageUnits += absDiff
                totalShortageCost += absDiff * prod.purchasePrice
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header
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
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "وضع الجرد السريع (الفعلي مقابل المسجل)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "امسح الباركود وأدخل كمية الرف ليحسب التطبيق العجز أو الفائض ويُحدث المخزون",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_quick_audit_modal_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Prominent Scan Barcode + Search Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showBarcodeScanner = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("audit_scan_barcode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "مسح باركود للجرد",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مسح باركود الرف",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث باسم السلعة أو الباركود...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty() || focusedProductId != null) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        focusedProductId = null
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audit_search_input")
                    )
                }

                // Summary Strip (Audited / Surplus / Shortage)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("تم جرده", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$totalAuditedCount / ${products.size}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إجمالي الفائض (+)", fontSize = 10.5.sp, color = Color(0xFF10B981))
                            Text(
                                text = if (canViewCostPrices && totalSurplusCost > 0) {
                                    "+$totalSurplusUnits ق (${String.format(Locale.US, "%.0f", totalSurplusCost)} $currencySymbol)"
                                } else {
                                    "+$totalSurplusUnits وحدة"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = Color(0xFF10B981)
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إجمالي العجز (-)", fontSize = 10.5.sp, color = semantic.dangerRed)
                            Text(
                                text = if (canViewCostPrices && totalShortageCost > 0) {
                                    "-$totalShortageUnits ق (${String.format(Locale.US, "%.0f", totalShortageCost)} $currencySymbol)"
                                } else {
                                    "-$totalShortageUnits وحدة"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = semantic.dangerRed
                            )
                        }
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val discrepancyCount = auditedCountsMap.count { (id, actual) ->
                        val p = products.find { it.id == id }
                        p != null && p.stock != actual
                    }
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("الكل (${products.size})", fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "AUDITED",
                        onClick = { selectedFilter = "AUDITED" },
                        label = { Text("تم جرده ($totalAuditedCount)", fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "DISCREPANCY",
                        onClick = { selectedFilter = "DISCREPANCY" },
                        label = { Text("⚠️ فروقات عجز/فائض ($discrepancyCount)", fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "UNAUDITED",
                        onClick = { selectedFilter = "UNAUDITED" },
                        label = { Text("لم يُجرد (${(products.size - totalAuditedCount).coerceAtLeast(0)})", fontSize = 11.5.sp) }
                    )
                }

                // Highlighted Scanned Product Card if user just scanned a barcode
                AnimatedVisibility(visible = focusedProduct != null) {
                    focusedProduct?.let { scannedProd ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎯 السلعة الممسوحة حالياً: ${scannedProd.name} (المسجل: ${scannedProd.stock} ق)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { focusedProductId = null }) {
                                    Text("إخفاء", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Products Audit List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(
                        items = filteredProducts,
                        key = { "audit_item_${it.id}" }
                    ) { product ->
                        val isFocused = product.id == focusedProductId
                        val currentInputText = auditedTextMap[product.id] ?: ""
                        val actualCount = auditedCountsMap[product.id]
                        val diff = actualCount?.let { it - product.stock }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFocused) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                                } else {
                                    semantic.cardElevated
                                }
                            ),
                            border = BorderStroke(
                                width = if (isFocused || actualCount != null) 1.4.dp else 1.dp,
                                color = when {
                                    diff != null && diff < 0 -> semantic.dangerRed.copy(alpha = 0.75f)
                                    diff != null && diff > 0 -> Color(0xFF10B981).copy(alpha = 0.75f)
                                    diff != null && diff == 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                    isFocused -> MaterialTheme.colorScheme.primary
                                    else -> semantic.cardBorder
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Product Title & System Recorded Stock
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (product.totalBarcodesCount > 0) {
                                            Text(
                                                text = "🏷️ ${product.allBarcodesList.joinToString(" ، ")}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = "المسجل: ${product.stock} ق",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                // Actual Shelf Quantity Input + Quick Stepper Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = currentInputText,
                                        onValueChange = { raw ->
                                            val cleaned = raw.filter { it.isDigit() }
                                            auditedTextMap[product.id] = cleaned
                                            val parsed = cleaned.toIntOrNull()
                                            if (parsed != null) {
                                                auditedCountsMap[product.id] = parsed
                                            } else {
                                                auditedCountsMap.remove(product.id)
                                            }
                                        },
                                        label = { Text("الكمية الفعلية على الرف", fontSize = 11.5.sp) },
                                        placeholder = { Text("أدخل العدد الفعلي (المسجل: ${product.stock})", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("audit_actual_input_${product.id}")
                                    )

                                    // -1 Button
                                    Surface(
                                        onClick = {
                                            val base = auditedCountsMap[product.id] ?: product.stock
                                            val next = (base - 1).coerceAtLeast(0)
                                            auditedCountsMap[product.id] = next
                                            auditedTextMap[product.id] = next.toString()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Remove, contentDescription = "إنقاص 1", modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // +1 Button
                                    Surface(
                                        onClick = {
                                            val base = auditedCountsMap[product.id] ?: product.stock
                                            val next = base + 1
                                            auditedCountsMap[product.id] = next
                                            auditedTextMap[product.id] = next.toString()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "زيادة 1",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Match Registered Button ("مطابق")
                                    Surface(
                                        onClick = {
                                            auditedCountsMap[product.id] = product.stock
                                            auditedTextMap[product.id] = product.stock.toString()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "مطابق ✓",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }

                                // Automatic Discrepancy Result Banner (عجز / فائض / مطابق) + Instant Single Update
                                if (diff != null && actualCount != null) {
                                    val isShortage = diff < 0
                                    val isSurplus = diff > 0
                                    val badgeColor = when {
                                        isShortage -> semantic.dangerRed
                                        isSurplus -> Color(0xFF10B981)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    val diffCostVal = abs(diff) * product.purchasePrice
                                    val diffSaleVal = abs(diff) * product.price
                                    val statusText = when {
                                        isShortage -> "🔴 عجز بالمخزون: $diff وحدة" +
                                            if (canViewCostPrices && diffCostVal > 0) " (قيمة العجز بالتكلفة: ${String.format(Locale.US, "%.1f", diffCostVal)} $currencySymbol)"
                                            else " (قيمة العجز بالبيع: ${String.format(Locale.US, "%.1f", diffSaleVal)} $currencySymbol)"
                                        isSurplus -> "🟢 فائض بالمخزون: +$diff وحدة" +
                                            if (canViewCostPrices && diffCostVal > 0) " (قيمة الفائض: +${String.format(Locale.US, "%.1f", diffCostVal)} $currencySymbol)"
                                            else ""
                                        else -> "✅ الجرد الفعلي مطابق تماماً للرصيد المسجل (${product.stock} وحدة)"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = badgeColor.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f)),
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
                                                text = statusText,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = badgeColor,
                                                modifier = Modifier.weight(1f)
                                            )

                                            if (diff != 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    onClick = {
                                                        onApplyAuditAdjustments(
                                                            mapOf(product.id to actualCount),
                                                            auditSessionNote
                                                        )
                                                        auditedCountsMap.remove(product.id)
                                                        auditedTextMap.remove(product.id)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = badgeColor,
                                                    modifier = Modifier.testTag("apply_single_audit_${product.id}")
                                                ) {
                                                    Text(
                                                        text = "تحديث الآن ✓",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

                // Bottom Action Bar: Save All Audited Adjustments
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (auditedCountsMap.isNotEmpty()) {
                                onApplyAuditAdjustments(auditedCountsMap.toMap(), auditSessionNote)
                                onDismiss()
                            }
                        },
                        enabled = auditedCountsMap.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_all_stock_audit_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (auditedCountsMap.isEmpty()) {
                                "أدخل الكمية الفعلية لاعتماد الجرد"
                            } else {
                                "اعتماد وتحديث المخزون (${auditedCountsMap.size} صنف) ✓"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        SimpleBarcodeCaptureDialog(
            title = "مسح باركود السلعة للجرد الفعلي 📋",
            onBarcodeScanned = { scannedCode ->
                showBarcodeScanner = false
                val code = scannedCode.trim()
                if (code.isNotEmpty()) {
                    val matched = products.find { it.matchesAnyBarcode(code) }
                    if (matched != null) {
                        focusedProductId = matched.id
                        searchQuery = ""
                        selectedFilter = "ALL"
                        if (!auditedCountsMap.containsKey(matched.id)) {
                            val step = if (matched.matchesCartonBarcode(code)) matched.unitsPerCarton.coerceAtLeast(1) else 1
                            auditedCountsMap[matched.id] = matched.stock
                            auditedTextMap[matched.id] = matched.stock.toString()
                        }
                    } else {
                        onPromptUnregisteredBarcode(code)
                    }
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

/**
 * 2. نافذة تنبيه السلع الراكدة وعمل التخفيضات الذكية لتنشيط المبيعات
 */
@Composable
fun StagnantProductsModal(
    products: List<Product>,
    sales: List<Sale>,
    currencySymbol: String,
    canViewCostPrices: Boolean,
    onApplyDiscountPrice: (product: Product, newPrice: Double, discountLabel: String) -> Unit,
    onDismiss: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var selectedThresholdDays by remember { mutableIntStateOf(30) }
    var showNeverSoldOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val customPriceInputs = remember { mutableStateMapOf<Int, String>() }

    val allStagnant = remember(products, sales, selectedThresholdDays) {
        computeStagnantProductsList(
            products = products,
            sales = sales,
            thresholdDays = selectedThresholdDays
        )
    }

    val filteredStagnant = remember(allStagnant, showNeverSoldOnly, searchQuery) {
        val q = searchQuery.trim()
        allStagnant.filter { info ->
            val matchesNeverSold = !showNeverSoldOnly || info.lastSaleMillis == null
            val matchesQuery = q.isEmpty() ||
                info.product.name.contains(q, ignoreCase = true) ||
                info.product.barcode.contains(q, ignoreCase = true) ||
                info.product.cartonBarcode.contains(q, ignoreCase = true)
            matchesNeverSold && matchesQuery
        }
    }

    val totalFrozenCost = remember(filteredStagnant) {
        filteredStagnant.sumOf { it.frozenCostValue }
    }
    val totalFrozenUnits = remember(filteredStagnant) {
        filteredStagnant.sumOf { it.product.stock }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header
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
                            color = semantic.debtAmber.copy(alpha = 0.16f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = null,
                                    tint = semantic.debtAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "تنبيه السلع الراكدة والتخفيضات الذكية",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "السلع التي لم تُبع منذ فترة طويلة لمساعدة المدير على عمل تخفيضات سريعة لتصريفها",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_stagnant_products_modal_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Period Filter Chips (15 days / 30 days / 60 days / Never Sold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(15 to "منذ 15 يوماً", 30 to "منذ 30 يوماً (الافتراضي)", 60 to "منذ 60 يوماً").forEach { (days, label) ->
                        FilterChip(
                            selected = !showNeverSoldOnly && selectedThresholdDays == days,
                            onClick = {
                                showNeverSoldOnly = false
                                selectedThresholdDays = days
                            },
                            label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                    FilterChip(
                        selected = showNeverSoldOnly,
                        onClick = {
                            showNeverSoldOnly = !showNeverSoldOnly
                            if (showNeverSoldOnly) selectedThresholdDays = 0
                            else if (selectedThresholdDays == 0) selectedThresholdDays = 30
                        },
                        label = { Text("لم تُبع أبداً", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Summary Strip (Stagnant Items Count, Total Units, Frozen Capital)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semantic.debtAmber.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("سلع راكدة", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${filteredStagnant.size} صنف",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = semantic.debtAmber
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(semantic.debtAmber.copy(alpha = 0.3f)))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("إجمالي الوحدات الراكدة", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$totalFrozenUnits قطعة",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (canViewCostPrices) {
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(semantic.debtAmber.copy(alpha = 0.3f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("رأس المال المجمد", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", totalFrozenCost)} $currencySymbol",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = semantic.dangerRed
                                )
                            }
                        }
                    }
                }

                // Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في السلع الراكدة بالاسم أو الباركود...", fontSize = 12.sp) },
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
                    modifier = Modifier.fillMaxWidth()
                )

                if (filteredStagnant.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "ممتاز! لا توجد سلع راكدة في هذه الفترة 🎉",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "جميع السلع المتوفرة بالمخزون تتحرك وتُباع بانتظام.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(
                            items = filteredStagnant,
                            key = { "stagnant_${it.product.id}" }
                        ) { info ->
                            val prod = info.product
                            val customPriceText = customPriceInputs[prod.id] ?: ""

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(15.dp),
                                colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                                border = BorderStroke(1.1.dp, semantic.debtAmber.copy(alpha = 0.55f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Top Row: Product Name + Stagnation Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.name,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (canViewCostPrices) {
                                                    "المخزون الراكد: ${prod.stock} ق • التكلفة: ${prod.purchasePrice} | السعر الحالي: ${prod.price} $currencySymbol"
                                                } else {
                                                    "المخزون الراكد: ${prod.stock} ق • السعر الحالي: ${prod.price} $currencySymbol"
                                                },
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (info.lastSaleMillis == null) {
                                                semantic.dangerRed.copy(alpha = 0.14f)
                                            } else {
                                                semantic.debtAmber.copy(alpha = 0.16f)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (info.lastSaleMillis == null) semantic.dangerRed.copy(alpha = 0.6f)
                                                else semantic.debtAmber.copy(alpha = 0.6f)
                                            )
                                        ) {
                                            Text(
                                                text = if (info.lastSaleMillis == null) {
                                                    "⏳ لم تُبع بعد"
                                                } else {
                                                    "⏳ راكدة منذ ${info.daysSinceLastSale} يوم"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (info.lastSaleMillis == null) semantic.dangerRed else semantic.debtAmber,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (info.lastSaleMillis != null) {
                                        Text(
                                            text = "آخر عملية بيع: ${dateFormatter.format(Date(info.lastSaleMillis))} • إجمالي المباع سابقاً: ${info.totalUnitsSoldEver} ق",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Quick Discount Preset Pills
                                    Text(
                                        text = "🏷️ تطبيق تخفيض فوري لتنشيط بيع السلعة:",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(10, 15, 20, 30).forEach { pct ->
                                            val discountedPrice = ((prod.price * (100 - pct) / 100.0) * 10.0).roundToInt() / 10.0
                                            if (discountedPrice > 0.0) {
                                                Surface(
                                                    onClick = {
                                                        onApplyDiscountPrice(prod, discountedPrice, "خصم $pct%")
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                                    modifier = Modifier.testTag("stagnant_discount_${pct}_${prod.id}")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocalOffer,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = "خصم $pct% ($discountedPrice $currencySymbol)",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (canViewCostPrices && prod.purchasePrice > 0.0 && prod.purchasePrice < prod.price) {
                                            Surface(
                                                onClick = {
                                                    onApplyDiscountPrice(prod, prod.purchasePrice, "تصفية بسعر التكلفة")
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = semantic.debtAmber.copy(alpha = 0.16f),
                                                border = BorderStroke(1.dp, semantic.debtAmber.copy(alpha = 0.65f)),
                                                modifier = Modifier.testTag("stagnant_cost_clearance_${prod.id}")
                                            ) {
                                                Text(
                                                    text = "تصفية بالتكلفة (${prod.purchasePrice} $currencySymbol)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = semantic.debtAmber,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Custom Discounted Price Input Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customPriceText,
                                            onValueChange = { customPriceInputs[prod.id] = it },
                                            placeholder = { Text("سعر مخفض مخصص (الحالي: ${prod.price})...", fontSize = 11.5.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("stagnant_custom_price_input_${prod.id}")
                                        )

                                        Button(
                                            onClick = {
                                                val parsed = customPriceText.trim().toDoubleOrNull()
                                                if (parsed != null && parsed > 0.0) {
                                                    onApplyDiscountPrice(prod, parsed, "تخفيض مخصص")
                                                    customPriceInputs.remove(prod.id)
                                                }
                                            },
                                            enabled = (customPriceText.trim().toDoubleOrNull() ?: 0.0) > 0.0,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("stagnant_apply_custom_price_btn_${prod.id}")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تطبيق السعر", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إغلاق النافذة", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
