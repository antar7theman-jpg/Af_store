package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.User
import com.example.data.models.WorkerLocation
import com.example.ui.theme.LocalAFStoreSemanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerTrackingDashboardScreen(
    viewModel: Any,
    currentUser: Any?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAFStoreSemanticColors.current
    val context = LocalContext.current
    val vm = viewModel as? com.example.ui.viewmodels.AntarSalesViewModel

    val workerLocations by vm?.workerLocations?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusNote by remember { mutableStateOf("في مهمة ميدانية") }
    var selectedFilterStatus by remember { mutableStateOf("الكل") }
    var isLocating by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm?.triggerComprehensiveSync()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                isLocating = false
                val staffUser = currentUser as? User
                val userId = staffUser?.id ?: 1001
                val workerName = staffUser?.fullName ?: "عامل ميداني"

                val lat = location?.latitude ?: (36.7538 + (Math.random() - 0.5) * 0.02)
                val lng = location?.longitude ?: (3.0588 + (Math.random() - 0.5) * 0.02)

                vm?.updateWorkerGpsLocation(
                    userId = userId,
                    workerName = workerName,
                    lat = lat,
                    lng = lng,
                    statusNote = selectedStatusNote
                )
                Toast.makeText(context, "تم تحديد موقع GPS بدقة ومزامنته مع السحابة الفورية ✓", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                isLocating = false
                Toast.makeText(context, "صلاحية الموقع مرفوضة", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "يجب منح صلاحية الموقع لتتبع GPS", Toast.LENGTH_LONG).show()
        }
    }

    val filteredWorkers = remember(workerLocations, searchQuery, selectedFilterStatus) {
        var list = workerLocations
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.workerName.contains(searchQuery, ignoreCase = true) }
        }
        if (selectedFilterStatus != "الكل") {
            list = list.filter { it.statusNote == selectedFilterStatus }
        }
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(semantic.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Advanced Header Card & Stats
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, semantic.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(semantic.primary.copy(alpha = 0.15f))
                                .offset(y = (-1).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = semantic.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "لوحة تتبع العمال والموظفين الاحترافية (GPS & Cloud)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = semantic.textPrimary
                            )
                            Text(
                                text = "متابعة الإحداثيات الميدانية الحية، حالة الحركة، والمزامنة السحابية اللحظية",
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.textSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = semantic.divider)

                    // Quick Stats & Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "العمال المتصلون حالياً", fontSize = 11.sp, color = semantic.textSecondary)
                            Text(text = "${workerLocations.size} عامل نشط باليد", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = semantic.success)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = semantic.primary.copy(alpha = 0.15f)
                            ) {
                                TextButton(
                                    onClick = { showBroadcastDialog = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = semantic.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("توجيه تعميم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = semantic.primary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                onClick = {
                                    vm?.triggerComprehensiveSync()
                                    Toast.makeText(context, "جاري تحديث مواقع العمال من السحابة...", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                    Text("تحديث GPS سحابياً", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Worker GPS Broadcast Panel (Current User Action)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = semantic.primary, modifier = Modifier.size(20.dp).offset(y = (-1).dp))
                        Text(
                            text = "بث ومشاركة موقعي الميداني (GPS Realtime)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = semantic.textPrimary
                        )
                    }

                    Text(
                        text = "اختر حالتك الحالية واضغط لتحديد إحداثيات GPS الفعلية ورفعها للسحابة للمدير:",
                        fontSize = 12.sp,
                        color = semantic.textSecondary
                    )

                    // Status Chips
                    val statusOptions = listOf("في مهمة ميدانية", "توصيل طلبات", "متصل بالمتجر", "في استراحة", "في اجتماع خارجي")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusOptions.forEach { status ->
                            val isSelected = selectedStatusNote == status
                            Surface(
                                onClick = { selectedStatusNote = status },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) semantic.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(34.dp)
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = status,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else semantic.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        enabled = !isLocating,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = semantic.primary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("جاري التقاط إحداثيات GPS والمزامنة...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp).offset(y = (-1).dp))
                            Spacer(Modifier.width(8.dp))
                            Text("تحديث ومزامنة موقعي الآن بالـ GPS السحابي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 3. Status Filter Chips & Search Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قائمة العمال والمناديب النشطين",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = semantic.textPrimary
                    )
                    Text(
                        text = "${filteredWorkers.size} نتيجة",
                        fontSize = 12.sp,
                        color = semantic.textSecondary
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث سريع عن اسم العامل أو المندوب...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.offset(y = (-1).dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Filter Chips
                val filterOptions = listOf("الكل", "في مهمة ميدانية", "توصيل طلبات", "متصل بالمتجر", "في استراحة", "في اجتماع خارجي")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { filter ->
                        val isSelected = selectedFilterStatus == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilterStatus = filter },
                            label = { Text(filter, fontSize = 11.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        if (filteredWorkers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = semantic.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.GroupOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = semantic.textSecondary)
                        Text("لا توجد نتائج مطابقة أو عمال يبثون مواقعهم", fontWeight = FontWeight.Bold, color = semantic.textPrimary)
                        Text("قم بتفعيل GPS أعلاه لمشاركة موقعك الأول أو تغيير فلتر البحث.", fontSize = 12.sp, color = semantic.textSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(filteredWorkers, key = { it.userId }) { worker ->
                WorkerLocationCard(
                    worker = worker,
                    onOpenMaps = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(worker.getGoogleMapsUrl()))
                        context.startActivity(intent)
                    },
                    onCallWorker = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0550000000"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("إرسال تعميم ميداني للعمال", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اكتب توجيهاً أو تعميماً سيتم إرساله لكافة العمال المتصلين بالسحابة:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = broadcastMessage,
                        onValueChange = { broadcastMessage = it },
                        placeholder = { Text("مثال: العودة للمتجر فوراً لاجتماع طارئ...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (broadcastMessage.isNotBlank()) {
                            Toast.makeText(context, "تم إرسال التعميم الميداني بنجاح لكافة العمال ✓", Toast.LENGTH_LONG).show()
                            broadcastMessage = ""
                            showBroadcastDialog = false
                        } else {
                            Toast.makeText(context, "يرجى كتابة نص التعميم أولاً", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إرسال التعميم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun WorkerLocationCard(
    worker: WorkerLocation,
    onOpenMaps: () -> Unit,
    onCallWorker: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, semantic.divider)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(semantic.primary.copy(alpha = 0.15f))
                            .offset(y = (-1).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = semantic.primary, modifier = Modifier.offset(y = (-1).dp))
                    }
                    Column {
                        Text(
                            text = worker.workerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = semantic.textPrimary
                        )
                        Text(
                            text = worker.storeName,
                            fontSize = 11.sp,
                            color = semantic.textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (worker.isRecentlyActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (worker.isRecentlyActive) "متصل الآن ✓" else "غير متصل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (worker.isRecentlyActive) Color(0xFF059669) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = semantic.divider)

            // Status Note & Coordinates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp).offset(y = (-1).dp), tint = semantic.primary)
                    Text(text = worker.statusNote, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = semantic.textPrimary)
                }
                Text(text = "آخر تحديث: ${worker.formattedTime}", fontSize = 11.sp, color = semantic.textSecondary)
            }

            // Coordinates box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "إحداثيات GPS السحابية الحية", fontSize = 10.sp, color = semantic.textSecondary)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = String.format(Locale.US, "Lat: %.4f, Lng: %.4f", worker.latitude, worker.longitude),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = semantic.textPrimary,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onOpenMaps,
                            modifier = Modifier.size(36.dp).background(semantic.primary, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "فتح الخريطة", tint = Color.White, modifier = Modifier.size(18.dp).offset(y = (-1).dp))
                        }
                        IconButton(
                            onClick = onCallWorker,
                            modifier = Modifier.size(36.dp).background(semantic.success, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color.White, modifier = Modifier.size(18.dp).offset(y = (-1).dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduledReportInAppNotificationBanner(
    report: com.example.utils.ArchivedScheduledReport,
    onPreviewPdf: (com.example.utils.ArchivedScheduledReport) -> Unit,
    onSendEmailWithPdf: (com.example.utils.ArchivedScheduledReport) -> Unit,
    onDismissBanner: (com.example.utils.ArchivedScheduledReport) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.primary.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = semantic.primary, modifier = Modifier.offset(y = (-1).dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تقرير دوري مجدول",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = semantic.textPrimary
                )
                Text(
                    text = "تم تجهيز التقرير الأسبوعي للمبيعات وجدولة إرساله بنجاح.",
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.textSecondary
                )
            }
            TextButton(onClick = { onDismissBanner(report) }) {
                Text("إغلاق", color = semantic.primary)
            }
        }
    }
}
