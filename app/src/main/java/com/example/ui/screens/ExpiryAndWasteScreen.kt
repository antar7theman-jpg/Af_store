package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.models.DamagedItemRecord
import com.example.data.models.Product
import com.example.data.models.SpoilageReason
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.ProductCloudImageHelper
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpiryAndWasteScreen(
    viewModel: AntarSalesViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val semantic = LocalAFStoreSemanticColors.current

    val products by viewModel.products.collectAsState(initial = emptyList())
    val activeProducts = remember(products) { products.filter { !it.isDeleted } }
    val damagedItems by viewModel.damagedItems.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedReasonFilter by remember { mutableStateOf<SpoilageReason?>(null) }

    // Dialog States
    var showRecordDamageDialog by remember { mutableStateOf(false) }
    var showMainBarcodeScanner by remember { mutableStateOf(false) }
    var damagedItemToDelete by remember { mutableStateOf<DamagedItemRecord?>(null) }
    var previewImagePath by remember { mutableStateOf<String?>(null) }

    // Metrics for Damaged Goods
    val totalMonthLoss = remember(damagedItems) {
        viewModel.getDamagedLossThisMonth()
    }
    val todayLoss = remember(damagedItems) {
        viewModel.getDamagedLossToday()
    }
    val totalDamagedQty = remember(damagedItems) {
        damagedItems.sumOf { it.quantity }
    }

    // Filtered Damaged Items List
    val filteredDamaged = remember(damagedItems, searchQuery, selectedReasonFilter) {
        damagedItems.filter { item ->
            val matchesReason = selectedReasonFilter == null || item.reason == selectedReasonFilter
            val matchesSearch = searchQuery.isBlank() ||
                item.productName.contains(searchQuery, ignoreCase = true) ||
                item.productBarcode.contains(searchQuery, ignoreCase = true) ||
                item.reason.labelAr.contains(searchQuery, ignoreCase = true) ||
                item.recordedBy.contains(searchQuery, ignoreCase = true) ||
                item.notes.contains(searchQuery, ignoreCase = true)

            matchesReason && matchesSearch
        }
    }

    Scaffold(
        containerColor = semantic.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(semantic.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = semantic.textPrimary
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "سجل الهالك والتوالف",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.textPrimary
                                )
                            }
                            Text(
                                text = "توثيق ومزامنة البضائع التالفة وصورها سحابياً ☁️",
                                fontSize = 11.sp,
                                color = semantic.textSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { viewModel.syncAllDamagedGoodsToCloud() },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "مزامنة التوالف والصور",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "مزامنة ☁️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Share report button
                        IconButton(
                            onClick = {
                                val summary = buildDamagedReportShareText(
                                    damagedItems = damagedItems,
                                    currencySymbol = currencySymbol
                                )
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, summary)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير الهالك والتوالف"))
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "مشاركة التقرير",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                HorizontalDivider(color = semantic.divider)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // KPI Dashboard Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiStatCard(
                        title = "خسائر الشهر",
                        value = "${String.format(Locale.US, "%.0f", totalMonthLoss)} $currencySymbol",
                        subtitle = "الشهر الحالي",
                        icon = Icons.Default.DeleteSweep,
                        accentColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1.1f)
                    )
                    KpiStatCard(
                        title = "الوحدات التالفة",
                        value = "$totalDamagedQty وحدة",
                        subtitle = "من المخزون",
                        icon = Icons.Default.Inventory2,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "العمليات المسجلة",
                        value = "${damagedItems.size} عملية",
                        subtitle = "اليوم: ${String.format(Locale.US, "%.0f", todayLoss)} $currencySymbol",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Primary Add Damaged Goods Action Button Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تسجيل وتوثيق سلعة تالفة / هالك",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "اختر السلعة من المخزون، وحدد السبب والكمية مع التقاط صورة لإثبات التلف",
                                fontSize = 11.sp,
                                color = semantic.textSecondary
                            )
                        }

                        Button(
                            onClick = { showRecordDamageDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسجيل تالف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search and Reason Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = semantic.textSecondary
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "مسح")
                                        }
                                    }
                                    IconButton(onClick = { showMainBarcodeScanner = true }) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "مسح باركود بالكاميرا",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "بحث في سجلات الهالك أو مسح الباركود...",
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Reason Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAllSelected = selectedReasonFilter == null
                        Surface(
                            onClick = { selectedReasonFilter = null },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else semantic.cardSubtle,
                            border = BorderStroke(1.dp, if (isAllSelected) MaterialTheme.colorScheme.primary else semantic.cardBorder)
                        ) {
                            Text(
                                text = "الكل (${damagedItems.size})",
                                fontSize = 11.5.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAllSelected) MaterialTheme.colorScheme.onPrimaryContainer else semantic.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        SpoilageReason.entries.forEach { reason ->
                            val count = damagedItems.count { it.reason == reason }
                            if (count > 0 || reason == SpoilageReason.EXPIRED || reason == SpoilageReason.DAMAGED_TRANSPORT) {
                                val isSelected = selectedReasonFilter == reason
                                Surface(
                                    onClick = { selectedReasonFilter = reason },
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (isSelected) Color(0xFFEF4444).copy(alpha = 0.16f) else semantic.cardSubtle,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFEF4444) else semantic.cardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(reason.badgeEmoji, fontSize = 12.sp)
                                        Text(
                                            text = "${reason.labelAr} ($count)",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFFEF4444) else semantic.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Damaged Items List
            if (filteredDamaged.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = semantic.surface),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = semantic.textSecondary,
                                modifier = Modifier.size(46.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedReasonFilter != null)
                                    "لا توجد نتائج تطابق البحث أو الفلتر المحدد"
                                else
                                    "لم يتم تسجيل أي بضائع تالفة بعد",
                                fontWeight = FontWeight.Bold,
                                color = semantic.textPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "يمكنك النقر على زر 'تسجيل تالف' بالأعلى لاختيار سلعة من المخزون، تسجيل سبب التلف وتوثيقه بالصورة.",
                                fontSize = 12.sp,
                                color = semantic.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredDamaged, key = { it.id }) { item ->
                    DamagedItemCardWithPhoto(
                        damagedItem = item,
                        currencySymbol = currencySymbol,
                        onImageClick = { path -> previewImagePath = path },
                        onDelete = { damagedItemToDelete = item }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Comprehensive Add Damaged Goods Dialog (Search Inventory + Photo Capture)
    if (showRecordDamageDialog) {
        ComprehensiveRecordDamagedDialog(
            activeProducts = activeProducts,
            currencySymbol = currencySymbol,
            onDismiss = { showRecordDamageDialog = false },
            onConfirm = { product, quantity, reason, notes, photoPath, deductStock ->
                viewModel.recordDamagedGoods(
                    product = product,
                    quantity = quantity,
                    reason = reason,
                    notes = notes,
                    imagePath = photoPath,
                    deductFromStock = deductStock
                )
                showRecordDamageDialog = false
            }
        )
    }

    // Fullscreen Image Preview Dialog
    previewImagePath?.let { path ->
        Dialog(onDismissRequest = { previewImagePath = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صورة إثبات التلف للسلعة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { previewImagePath = null }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    val imageFile = remember(path) { File(path) }
                    if (imageFile.exists()) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = "صورة السلعة التالفة",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("تعذر العثور على ملف الصورة على الجهاز.", color = semantic.textSecondary)
                        }
                    }

                    Text(
                        text = "تم التقاط هذه الصورة وتوثيقها لتأكيد سبب التلف قبل سحب السلعة من المخزون.",
                        fontSize = 11.sp,
                        color = semantic.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    damagedItemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { damagedItemToDelete = null },
            title = { Text("حذف سجل التالف", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف سجل تالف (${item.productName})؟\nملاحظة: هذا لن يعيد الكمية المخصومة تلقائياً للمخزون.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDamagedItem(item.id)
                        damagedItemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("حذف السجل")
                }
            },
            dismissButton = {
                TextButton(onClick = { damagedItemToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showMainBarcodeScanner) {
        SimpleBarcodeCaptureDialog(
            title = "مسح باركود السلعة للبحث في سجل التوالف 📷",
            onBarcodeScanned = { scannedCode ->
                searchQuery = scannedCode
                showMainBarcodeScanner = false
                Toast.makeText(context, "تم مسح الباركود: $scannedCode", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showMainBarcodeScanner = false }
        )
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAFStoreSemanticColors.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = semantic.textSecondary,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = semantic.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = semantic.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DamagedItemCardWithPhoto(
    damagedItem: DamagedItemRecord,
    currencySymbol: String,
    onImageClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalAFStoreSemanticColors.current

    val resolvedPhotoFile = remember(damagedItem.imagePath, damagedItem.imageUrl, damagedItem.id) {
        val directFile = if (damagedItem.imagePath.isNotBlank()) File(damagedItem.imagePath) else null
        if (directFile != null && directFile.exists()) {
            directFile
        } else if (damagedItem.imageUrl.isNotBlank()) {
            val decodedPath = ProductCloudImageHelper.decodeBase64ToLocalFile(
                context = context,
                base64String = damagedItem.imageUrl,
                identifier = "dmg_${damagedItem.id}"
            )
            if (decodedPath != null) File(decodedPath) else null
        } else {
            null
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444).copy(alpha = 0.14f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(damagedItem.reason.badgeEmoji, fontSize = 17.sp)
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = damagedItem.productName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = semantic.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (resolvedPhotoFile != null) "☁️ صورة متزامنة" else "☁️ متزامن",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${damagedItem.formattedDate} - ${damagedItem.formattedTime} | بواسطة: ${damagedItem.recordedBy.ifBlank { "الإدارة" }}",
                            fontSize = 10.5.sp,
                            color = semantic.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = semantic.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Row with Details + Proof Photo Thumbnail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info block
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = semantic.cardSubtle,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "السبب: ${damagedItem.reason.labelAr}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "الكمية: ${damagedItem.quantity} وحدة",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = semantic.textPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تكلفة الوحدة: ${String.format(Locale.US, "%.0f", damagedItem.unitCost)} $currencySymbol",
                                fontSize = 10.5.sp,
                                color = semantic.textSecondary
                            )
                            Text(
                                text = "الخسارة: ${String.format(Locale.US, "%.0f", damagedItem.totalLoss)} $currencySymbol",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // Photo Thumbnail if attached (local or cloud-synced)
                if (resolvedPhotoFile != null && resolvedPhotoFile.exists()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, semantic.cardBorder, RoundedCornerShape(10.dp))
                            .clickable { onImageClick(resolvedPhotoFile.absolutePath) }
                    ) {
                        AsyncImage(
                            model = resolvedPhotoFile,
                            contentDescription = "صورة التالف",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }

            if (damagedItem.notes.isNotBlank()) {
                Text(
                    text = "ملاحظات: ${damagedItem.notes}",
                    fontSize = 11.sp,
                    color = semantic.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ComprehensiveRecordDamagedDialog(
    activeProducts: List<Product>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (product: Product, quantity: Int, reason: SpoilageReason, notes: String, photoPath: String, deductStock: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val semantic = LocalAFStoreSemanticColors.current

    var selectedProduct by remember { mutableStateOf(activeProducts.firstOrNull()) }
    var productSearchQuery by remember { mutableStateOf("") }
    var showProductBarcodeScanner by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf("1") }
    var selectedReason by remember { mutableStateOf(SpoilageReason.EXPIRED) }
    var notes by remember { mutableStateOf("") }
    var deductFromStock by remember { mutableStateOf(true) }
    var photoPath by remember { mutableStateOf("") }
    var isSavingPhoto by remember { mutableStateOf(false) }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                isSavingPhoto = true
                val path = ProductImageStorageHelper.saveBitmapToStorage(context, bitmap)
                if (!path.isNullOrBlank()) {
                    photoPath = path
                }
                isSavingPhoto = false
            }
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isSavingPhoto = true
                val path = ProductImageStorageHelper.saveImageFromUri(context, uri)
                if (!path.isNullOrBlank()) {
                    photoPath = path
                }
                isSavingPhoto = false
            }
        }
    }

    val prod = selectedProduct
    val parsedQty = quantityText.toIntOrNull() ?: 1
    val calculatedLoss = (prod?.purchasePrice ?: 0.0) * parsedQty

    // Filtered inventory products based on search
    val filteredProducts = remember(productSearchQuery, activeProducts) {
        if (productSearchQuery.isBlank()) activeProducts
        else activeProducts.filter {
            it.name.contains(productSearchQuery, ignoreCase = true) ||
            it.barcode.contains(productSearchQuery, ignoreCase = true) ||
            it.cartonBarcode.contains(productSearchQuery, ignoreCase = true) ||
            it.category.contains(productSearchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text("تسجيل بضاعة تالفة / هالك", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("إدارة المخزون وتوثيق التلف بالصورة", fontSize = 10.5.sp, color = semantic.textSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Product Selection from Inventory
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. اختيار السلعة من المخزون:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = semantic.textPrimary
                    )

                    Surface(
                        onClick = { showProductBarcodeScanner = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "مسح الباركود بالكاميرا",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "مسح باركود 📷",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Search field for inventory items with camera barcode button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = semantic.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (productSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { productSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = { showProductBarcodeScanner = true }) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "مسح الباركود بالكاميرا",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        placeholder = { Text("ابحث باسم السلعة أو امسح الباركود...", fontSize = 11.5.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Scrollable box of products matching the search
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = semantic.cardSubtle,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد سلع تطابق البحث في المخزون", fontSize = 11.sp, color = semantic.textSecondary)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredProducts.forEach { p ->
                                val isSelected = selectedProduct?.id == p.id
                                Surface(
                                    onClick = { selectedProduct = p },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else semantic.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else semantic.cardBorder.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = p.name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "المخزون: ${p.stock} وحدة | باركود: ${p.barcode.ifBlank { "بدون" }}",
                                                    fontSize = 10.sp,
                                                    color = semantic.textSecondary
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${String.format(Locale.US, "%.0f", p.purchasePrice)} $currencySymbol",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = semantic.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Product Details Card
                prod?.let { p ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.09f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "السلعة المحددة: ${p.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = semantic.textPrimary
                                )
                                Text(
                                    text = "الرصيد المتاح حالياً: ${p.stock} وحدة",
                                    fontSize = 10.5.sp,
                                    color = if (p.stock <= 0) Color(0xFFEF4444) else semantic.textSecondary
                                )
                            }
                            Text(
                                text = "التكلفة: ${String.format(Locale.US, "%.0f", p.purchasePrice)} $currencySymbol",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = semantic.textPrimary
                            )
                        }
                    }
                }

                // Section 2: Damaged Quantity with Stepper
                Text(
                    text = "2. الكمية التالفة (بالوحدات):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = semantic.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = {
                            val cur = quantityText.toIntOrNull() ?: 1
                            if (cur > 1) quantityText = (cur - 1).toString()
                        },
                        icon = Icons.Default.Remove
                    )

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { input ->
                            quantityText = input.filter { it.isDigit() }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilledIconButton(
                        onClick = {
                            val cur = quantityText.toIntOrNull() ?: 0
                            quantityText = (cur + 1).toString()
                        },
                        icon = Icons.Default.Add
                    )
                }

                // Section 3: Reason for Damage
                Text(
                    text = "3. سبب التلف / الإتلاف:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = semantic.textPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SpoilageReason.entries.forEach { reason ->
                        val isSelected = selectedReason == reason
                        Surface(
                            onClick = { selectedReason = reason },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFEF4444).copy(alpha = 0.15f) else semantic.cardSubtle,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFEF4444) else semantic.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(reason.badgeEmoji, fontSize = 15.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reason.labelAr,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFEF4444) else semantic.textPrimary
                                    )
                                    Text(
                                        text = reason.descriptionAr,
                                        fontSize = 9.5.sp,
                                        color = semantic.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Photo Capture & Confirmation (User Request Requirement!)
                Text(
                    text = "4. إثبات التلف بالصورة (تأكيد التلف):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = semantic.textPrimary
                )

                if (photoPath.isNotBlank()) {
                    val file = remember(photoPath) { File(photoPath) }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.cardSubtle,
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
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
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "تم التقاط صورة إثبات التلف بنجاح",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                IconButton(
                                    onClick = { photoPath = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف الصورة", modifier = Modifier.size(16.dp))
                                }
                            }

                            if (file.exists()) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "معاينة صورة التالف",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.cardSubtle,
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSavingPhoto) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text("جاري حفظ الصورة...", fontSize = 11.sp, color = semantic.textSecondary)
                            } else {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "التقط صورة للسلعة المضافة لتأكيد التلف وإثباته في السجل",
                                    fontSize = 11.sp,
                                    color = semantic.textSecondary,
                                    textAlign = TextAlign.Center
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { cameraLauncher.launch(null) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("كاميرا 📷", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            galleryLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("معرض الصور 🖼️", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Deduct from Stock Switch
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = semantic.cardSubtle,
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
                            Text("خصم الكمية من رصيد المخزون", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("لتصحيح الجرد الفعلي للمحل", fontSize = 10.sp, color = semantic.textSecondary)
                        }
                        Switch(
                            checked = deductFromStock,
                            onCheckedChange = { deductFromStock = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF4444))
                        )
                    }
                }

                // Financial Loss Preview Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الخسارة المالية المترتبة:", fontSize = 11.5.sp, color = Color(0xFFEF4444))
                        Text(
                            text = "${String.format(Locale.US, "%.0f", calculatedLoss)} $currencySymbol",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية عن سبب التلف") },
                    placeholder = { Text("مثال: سقوط الكرتون أثناء التوريد أو كسر الغطاء") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    prod?.let { product ->
                        onConfirm(product, parsedQty, selectedReason, notes, photoPath, deductFromStock)
                    }
                },
                enabled = prod != null && parsedQty > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تأكيد تسجيل التالف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

    if (showProductBarcodeScanner) {
        SimpleBarcodeCaptureDialog(
            title = "مسح باركود السلعة التالفة من المخزون 📷",
            onBarcodeScanned = { scannedCode ->
                val cleanCode = scannedCode.trim()
                productSearchQuery = cleanCode
                val exactOrPartialMatch = activeProducts.find {
                    it.barcode.equals(cleanCode, ignoreCase = true) ||
                        (it.cartonBarcode.isNotBlank() && it.cartonBarcode.equals(cleanCode, ignoreCase = true))
                } ?: activeProducts.find {
                    it.barcode.contains(cleanCode, ignoreCase = true) ||
                        it.cartonBarcode.contains(cleanCode, ignoreCase = true)
                }

                if (exactOrPartialMatch != null) {
                    selectedProduct = exactOrPartialMatch
                    Toast.makeText(
                        context,
                        "تم تحديد السلعة: ${exactOrPartialMatch.name} ✅",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "لم يتم العثور على سلعة بالباركود: $cleanCode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showProductBarcodeScanner = false
            },
            onDismiss = { showProductBarcodeScanner = false }
        )
    }
}

@Composable
private fun FilledIconButton(
    onClick: () -> Unit,
    icon: ImageVector
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun buildDamagedReportShareText(
    damagedItems: List<DamagedItemRecord>,
    currencySymbol: String
): String {
    val totalLoss = damagedItems.sumOf { it.totalLoss }
    val totalQty = damagedItems.sumOf { it.quantity }

    val sb = StringBuilder()
    sb.append("📋 *تقرير سجل الهالك والتوالف - AF Store*\n")
    sb.append("📅 التاريخ: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())}\n\n")

    sb.append("📊 *الملخص المالي:* \n")
    sb.append("• إجمالي العمليات المسجلة: ${damagedItems.size} عملية\n")
    sb.append("• إجمالي الوحدات التالفة: $totalQty وحدة\n")
    sb.append("• إجمالي الخسائر المالية: ${String.format(Locale.US, "%.0f", totalLoss)} $currencySymbol\n\n")

    sb.append("📝 *تفاصيل البضائع التالفة:*\n")
    if (damagedItems.isEmpty()) {
        sb.append("✅ لا توجد بضائع تالفة مسجلة.\n")
    } else {
        damagedItems.forEachIndexed { idx, item ->
            val hasPhoto = if (item.imagePath.isNotBlank()) "📷 (مزود بصورة إثبات)" else ""
            sb.append("${idx + 1}. *${item.productName}*\n")
            sb.append("   - الكمية: ${item.quantity} وحدة | الخسارة: ${String.format(Locale.US, "%.0f", item.totalLoss)} $currencySymbol\n")
            sb.append("   - السبب: ${item.reason.labelAr} $hasPhoto\n")
            sb.append("   - التاريخ: ${item.formattedDate} بواسطة: ${item.recordedBy.ifBlank { "الإدارة" }}\n")
            if (item.notes.isNotBlank()) {
                sb.append("   - ملاحظات: ${item.notes}\n")
            }
            sb.append("\n")
        }
    }

    return sb.toString()
}
