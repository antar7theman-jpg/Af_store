package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Product
import com.example.data.models.Promotion
import com.example.data.models.PromotionType
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(
    viewModel: AntarSalesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAFStoreSemanticColors.current
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val promotions by viewModel.promotions.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "COUPONS", "TIERS", "ACTIVE"
    var editingPromotion by remember { mutableStateOf<Promotion?>(null) }
    var showAddEditModal by remember { mutableStateOf(false) }
    var promotionToDelete by remember { mutableStateOf<Promotion?>(null) }

    val filteredList = remember(promotions, searchQuery, selectedFilter) {
        promotions.filter { promo ->
            val matchesSearch = searchQuery.isBlank() ||
                    promo.title.contains(searchQuery, ignoreCase = true) ||
                    promo.code.contains(searchQuery, ignoreCase = true) ||
                    promo.description.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "COUPONS" -> promo.isCoupon
                "TIERS" -> promo.type == PromotionType.QUANTITY_TIER_DISCOUNT || promo.type == PromotionType.BUY_X_GET_Y_FREE
                "ACTIVE" -> promo.isActive && !promo.isExpired
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val activeCount = remember(promotions) { promotions.count { it.isActive && !it.isExpired } }
    val couponsCount = remember(promotions) { promotions.count { it.isCoupon } }
    val tierOffersCount = remember(promotions) { promotions.count { !it.isCoupon } }

    Scaffold(
        topBar = {
            Surface(
                color = semantic.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(semantic.primary.copy(alpha = 0.12f))
                                    .testTag("promotions_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = semantic.primary
                                )
                            }
                            Column {
                                Text(
                                    text = "العروض والخصومات الترويجية",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = semantic.textPrimary
                                )
                                Text(
                                    text = "إدارة قسائم الخصم وعروض الكميات التلقائية",
                                    fontSize = 11.5.sp,
                                    color = semantic.textSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                editingPromotion = null
                                showAddEditModal = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = semantic.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_new_promotion_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("عرض جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = semantic.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // 1. Metric Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PromotionMetricCard(
                        title = "العروض النشطة",
                        value = "$activeCount",
                        sub = "جاهزة للتطبيق",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    PromotionMetricCard(
                        title = "كوبونات الخصم",
                        value = "$couponsCount",
                        sub = "أكواد ترويجية",
                        icon = Icons.Default.LocalOffer,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    PromotionMetricCard(
                        title = "عروض الكميات",
                        value = "$tierOffersCount",
                        sub = "خصومات جملة وBOGO",
                        icon = Icons.Default.Layers,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Search & Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث عن اسم العرض، الكود، أو التفاصيل...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("promotions_search_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "جميع العروض (${promotions.size})",
                            "COUPONS" to "كوبونات الخصم ($couponsCount)",
                            "TIERS" to "عروض الكميات ($tierOffersCount)",
                            "ACTIVE" to "النشطة فقط ($activeCount)"
                        ).forEach { (key, label) ->
                            val isSel = selectedFilter == key
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // 3. Promotions List or Empty State
            if (filteredList.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = semantic.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = semantic.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "لم يتم العثور على عروض تطابق بحثك" else "لا توجد عروض ترويجية مضافة حتى الآن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = semantic.textPrimary
                            )
                            Text(
                                text = "يمكنك إضافة عروض تخفيض أو كوبونات لتشجيع العملاء وزيادة مبيعات المتجر",
                                fontSize = 12.sp,
                                color = semantic.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    editingPromotion = null
                                    showAddEditModal = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = semantic.primary),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("إنشاء أول عرض ترويجي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { promo ->
                    PromotionItemCard(
                        promotion = promo,
                        currencySymbol = currencySymbol,
                        onToggleActive = { active ->
                            viewModel.promotionRepository.togglePromotionActive(promo.id, active)
                            Toast.makeText(
                                context,
                                if (active) "تم تفعيل العرض بنجاح ✓" else "تم إيقاف العرض مؤقتاً",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onEdit = {
                            editingPromotion = promo
                            showAddEditModal = true
                        },
                        onDelete = {
                            promotionToDelete = promo
                        },
                        onCopyCode = {
                            clipboard.setPrimaryClip(ClipData.newPlainText("Promo Code", promo.code))
                            Toast.makeText(context, "تم نسخ كود الكوبون: ${promo.code} 📋", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Modal to Add / Edit Promotion
    if (showAddEditModal) {
        AddEditPromotionDialog(
            initialPromotion = editingPromotion,
            products = products,
            currencySymbol = currencySymbol,
            onDismiss = { showAddEditModal = false },
            onSave = { updatedPromo ->
                viewModel.promotionRepository.savePromotion(updatedPromo)
                showAddEditModal = false
                Toast.makeText(
                    context,
                    if (editingPromotion == null) "تمت إضافة العرض الترويجي بنجاح ✓" else "تم تحديث بيانات العرض بنجاح ✓",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // Delete Confirmation Dialog
    promotionToDelete?.let { promo ->
        AlertDialog(
            onDismissRequest = { promotionToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("حذف العرض الترويجي", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف العرض \"${promo.title}\"؟ لن يتم تطبيقه على فواتير الكاشير بعد الآن.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.promotionRepository.deletePromotion(promo.id)
                        promotionToDelete = null
                        Toast.makeText(context, "تم حذف العرض بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("نعم، حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { promotionToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun PromotionMetricCard(
    title: String,
    value: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAFStoreSemanticColors.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, fontSize = 11.sp, color = semantic.textSecondary, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
                }
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = semantic.textPrimary
            )
            Text(
                text = sub,
                fontSize = 9.5.sp,
                color = semantic.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PromotionItemCard(
    promotion: Promotion,
    currencySymbol: String,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyCode: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val typeColor = when (promotion.type) {
        PromotionType.PERCENTAGE_DISCOUNT -> Color(0xFF3B82F6)
        PromotionType.FIXED_AMOUNT_DISCOUNT -> Color(0xFF10B981)
        PromotionType.QUANTITY_TIER_DISCOUNT -> Color(0xFFF59E0B)
        PromotionType.BUY_X_GET_Y_FREE -> Color(0xFF8B5CF6)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (promotion.isActive && !promotion.isExpired) typeColor.copy(alpha = 0.4f) else semantic.textSecondary.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("promotion_card_${promotion.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Type Badge + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = promotion.type.labelAr,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (promotion.isCoupon) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(0.8.dp, Color(0xFF10B981)),
                            modifier = Modifier.clickable { onCopyCode() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                Text(
                                    text = promotion.code,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (promotion.isActive) "مُفعّل" else "متوقف",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (promotion.isActive) Color(0xFF10B981) else semantic.textSecondary
                    )
                    Switch(
                        checked = promotion.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier.testTag("promo_switch_${promotion.id}")
                    )
                }
            }

            // Title & Description
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = promotion.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = semantic.textPrimary
                )
                if (promotion.description.isNotBlank()) {
                    Text(
                        text = promotion.description,
                        fontSize = 12.sp,
                        color = semantic.textSecondary
                    )
                }
            }

            // Target Product (if any)
            if (promotion.targetProductName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = semantic.primary, modifier = Modifier.size(14.dp))
                        Text(
                            text = "السلعة المستهدفة: ${promotion.targetProductName}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = semantic.textPrimary
                        )
                    }
                }
            }

            // Details Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = semantic.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = promotion.formattedSummary(currencySymbol),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = semantic.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Min cart total
                if (promotion.minCartTotal > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "حد أدنى للسلة: ${promotion.minCartTotal.toInt()} $currencySymbol",
                            fontSize = 10.5.sp,
                            color = semantic.textPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Used counter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "استُخدم: ${promotion.usedCount} مرة",
                        fontSize = 10.5.sp,
                        color = semantic.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = semantic.textSecondary.copy(alpha = 0.1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_promo_btn_${promotion.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تعديل", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("delete_promo_btn_${promotion.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("حذف", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPromotionDialog(
    initialPromotion: Promotion?,
    products: List<Product>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Promotion) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var title by remember { mutableStateOf(initialPromotion?.title ?: "") }
    var code by remember { mutableStateOf(initialPromotion?.code ?: "") }
    var description by remember { mutableStateOf(initialPromotion?.description ?: "") }
    var selectedType by remember { mutableStateOf(initialPromotion?.type ?: PromotionType.PERCENTAGE_DISCOUNT) }
    var discountValueText by remember {
        mutableStateOf(
            if (initialPromotion != null && initialPromotion.discountValue > 0.0) {
                if (initialPromotion.discountValue % 1.0 == 0.0) initialPromotion.discountValue.toInt().toString()
                else initialPromotion.discountValue.toString()
            } else ""
        )
    }
    var minCartTotalText by remember {
        mutableStateOf(
            if (initialPromotion != null && initialPromotion.minCartTotal > 0.0) {
                if (initialPromotion.minCartTotal % 1.0 == 0.0) initialPromotion.minCartTotal.toInt().toString()
                else initialPromotion.minCartTotal.toString()
            } else ""
        )
    }
    var minQuantityText by remember { mutableStateOf((initialPromotion?.minQuantity ?: 1).toString()) }
    var rewardFreeQuantityText by remember { mutableStateOf((initialPromotion?.rewardFreeQuantity ?: 1).toString()) }
    var selectedTargetProductId by remember { mutableStateOf<Int?>(initialPromotion?.targetProductId) }
    var isActive by remember { mutableStateOf(initialPromotion?.isActive ?: true) }

    var showProductDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = semantic.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (initialPromotion == null) "إضافة عرض ترويجي جديد" else "تعديل العرض الترويجي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = semantic.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider()

                // Type Selector Tabs
                Text(text = "نوع العرض الترويجي:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = semantic.textPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PromotionType.entries.forEach { type ->
                        val isSel = selectedType == type
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) semantic.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSel) semantic.primary else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(selected = isSel, onClick = { selectedType = type })
                                Column {
                                    Text(
                                        text = type.labelAr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSel) semantic.primary else semantic.textPrimary
                                    )
                                    Text(
                                        text = type.subtitleAr,
                                        fontSize = 11.sp,
                                        color = semantic.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان العرض الترويجي (مثال: عرض الصيف 10%)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("promo_input_title")
                )

                // Code (Promo code)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("كود الكوبون (اختياري - اتركه فارغاً إذا كان العرض يطبق تلقائياً)") },
                    placeholder = { Text("مثال: SAVE20, WELCOME10") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("promo_input_code")
                )

                // Discount Value (for percentage, fixed, or quantity tier)
                if (selectedType != PromotionType.BUY_X_GET_Y_FREE) {
                    val labelUnit = if (selectedType == PromotionType.PERCENTAGE_DISCOUNT || selectedType == PromotionType.QUANTITY_TIER_DISCOUNT) "%" else currencySymbol
                    OutlinedTextField(
                        value = discountValueText,
                        onValueChange = { discountValueText = it },
                        label = { Text("قيمة الخصم ($labelUnit)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("promo_input_value")
                    )
                }

                // Quantity fields for Tier or BOGO
                if (selectedType == PromotionType.QUANTITY_TIER_DISCOUNT || selectedType == PromotionType.BUY_X_GET_Y_FREE) {
                    OutlinedTextField(
                        value = minQuantityText,
                        onValueChange = { minQuantityText = it },
                        label = { Text("الحد الأدنى للكمية المؤهلة (قطع)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedType == PromotionType.BUY_X_GET_Y_FREE) {
                        OutlinedTextField(
                            value = rewardFreeQuantityText,
                            onValueChange = { rewardFreeQuantityText = it },
                            label = { Text("عدد القطع المجانية الممنوحة") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Product Selector
                    val selectedProd = products.find { it.id == selectedTargetProductId }
                    Text(text = "تطبيق العرض على سلعة محددة (اختياري):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = semantic.textPrimary)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, semantic.textSecondary.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProductDropdown = !showProductDropdown }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedProd?.name ?: "جميع الأصناف والسلع بدون استثناء",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = semantic.textPrimary
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    if (showProductDropdown) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = semantic.surface,
                            tonalElevation = 4.dp,
                            border = BorderStroke(1.dp, semantic.textSecondary.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(6.dp)) {
                                item {
                                    Surface(
                                        onClick = {
                                            selectedTargetProductId = null
                                            showProductDropdown = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selectedTargetProductId == null) semantic.primary.copy(alpha = 0.12f) else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("جميع الأصناف والسلع بدون استثناء", modifier = Modifier.padding(10.dp), fontSize = 12.5.sp)
                                    }
                                }
                                items(products) { prod ->
                                    Surface(
                                        onClick = {
                                            selectedTargetProductId = prod.id
                                            showProductDropdown = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selectedTargetProductId == prod.id) semantic.primary.copy(alpha = 0.12f) else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${prod.name} (${prod.price} $currencySymbol)", modifier = Modifier.padding(10.dp), fontSize = 12.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Minimum Cart Total
                OutlinedTextField(
                    value = minCartTotalText,
                    onValueChange = { minCartTotalText = it },
                    label = { Text("الحد الأدنى لقيمة الفاتورة ($currencySymbol) - اختياري") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("ملاحظات وشروط العرض (اختياري)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Active Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("تفعيل العرض فوراً:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                return@Button
                            }
                            val discVal = discountValueText.toDoubleOrNull() ?: 0.0
                            val minCart = minCartTotalText.toDoubleOrNull() ?: 0.0
                            val minQty = minQuantityText.toIntOrNull() ?: 1
                            val freeQty = rewardFreeQuantityText.toIntOrNull() ?: 1
                            val targetProd = products.find { it.id == selectedTargetProductId }

                            val newOrUpdated = (initialPromotion ?: Promotion()).copy(
                                title = title.trim(),
                                code = code.trim().uppercase(),
                                description = description.trim(),
                                type = selectedType,
                                discountValue = discVal,
                                minCartTotal = minCart,
                                minQuantity = minQty.coerceAtLeast(1),
                                rewardFreeQuantity = freeQty.coerceAtLeast(0),
                                targetProductId = selectedTargetProductId,
                                targetProductName = targetProd?.name ?: "",
                                isActive = isActive
                            )
                            onSave(newOrUpdated)
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = semantic.primary),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("save_promotion_dialog_btn")
                    ) {
                        Text("حفظ وتطبيق العرض", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            }
        }
    }
}
