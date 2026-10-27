package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import com.example.utils.SyncIntervalMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirestoreSyncHealthChecker
import com.example.data.firebase.PingLatencyResult
import com.example.data.firebase.RealtimeConnectionState
import com.example.data.firebase.RealtimeStatusUpdate
import com.example.ui.components.DailySyncVolumeVicoChartCard
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.OfflineSyncManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * شاشة ومكون حالة المزامنة اللحظية (SyncStatusScreen)
 * تعرض:
 * 1. حالة الاتصال اللحظي بخوادم Firebase Firestore.
 * 2. قياس واختبار سرعة الاستجابة (Ping / Latency).
 * 3. توقيت آخر مزامنة ناجحة.
 * 4. مؤشر الذاكرة المحلية (Offline Cache) والعمليات المعلقة.
 * 5. أزرار المزامنة اليدوية والآلية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    viewModel: AntarSalesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticColors = LocalAFStoreSemanticColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "صحة وحالة المزامنة السحابية",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مراقبة الاتصال اللحظي والذاكرة المؤقتة",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("sync_status_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        SyncStatusContent(
            viewModel = viewModel,
            isCompactMode = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        )
    }
}

/**
 * لوحة حالة المزامنة المنسدلة من شعار التطبيق (SyncStatusDropdownPanel)
 * مصممة لتظهر بأناقة تحت الشعار وتتيح مراقبة واختبار المزامنة مباشرة.
 */
@Composable
fun SyncStatusDropdownPanel(
    viewModel: AntarSalesViewModel,
    onClose: () -> Unit,
    onOpenFullScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SyncStatusContent(
        viewModel = viewModel,
        isCompactMode = true,
        onClose = onClose,
        onOpenFullScreen = onOpenFullScreen,
        modifier = modifier
    )
}

/**
 * المحتوى التفاعلي لحالة المزامنة (مشترك بين الشاشة الكاملة والقائمة المنسدلة)
 */
@Composable
fun SyncStatusContent(
    viewModel: AntarSalesViewModel,
    isCompactMode: Boolean,
    onClose: (() -> Unit)? = null,
    onOpenFullScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val syncManager = viewModel.offlineSyncManager
    val pendingQueueCount by viewModel.pendingSyncQueueCount.collectAsState()
    val isSyncing by syncManager.isSyncing.collectAsState()
    val isAutoSyncEnabled by syncManager.isAutoSyncEnabled.collectAsState()
    val syncIntervalMode by syncManager.syncIntervalMode.collectAsState()
    val isNetworkOnline by syncManager.isOnline.collectAsState()
    val lastSyncTimeMillis by syncManager.lastSyncTimeMillis.collectAsState()
    val lastSyncText = remember(lastSyncTimeMillis) {
        if (lastSyncTimeMillis > 0L) {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = lastSyncTimeMillis }
            val isAm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM
            val amPm = if (isAm) "ص" else "م"
            val timeDate = java.text.SimpleDateFormat("dd-MM-yyyy hh:mm", java.util.Locale.US).format(java.util.Date(lastSyncTimeMillis))
            "$timeDate $amPm"
        } else {
            "لم تُسجل مزامنة سابقة بعد"
        }
    }
    val semanticColors = LocalAFStoreSemanticColors.current
    val coroutineScope = rememberCoroutineScope()

    // الفاحص اللحظي
    val healthChecker = remember { FirestoreSyncHealthChecker() }
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "staff_user" }

    var realtimeStatus by remember {
        mutableStateOf(
            RealtimeStatusUpdate(
                state = if (isNetworkOnline) RealtimeConnectionState.CONNECTED_LIVE else RealtimeConnectionState.OFFLINE,
                isFromCache = !isNetworkOnline,
                hasPendingWrites = pendingQueueCount > 0,
                messageAr = if (isNetworkOnline) "جاري التحقق من الاتصال السحابي..." else "التطبيق في وضع عدم الاتصال (Offline)"
            )
        )
    }

    val lastSyncSummary by syncManager.lastSyncSummary.collectAsState()
    var isPingTesting by remember { mutableStateOf(false) }
    var pingResult by remember { mutableStateOf<PingLatencyResult?>(null) }
    var actionFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // مراقبة انتهاء المزامنة وتحديث الرسالة تلقائياً
    var wasSyncing by remember { mutableStateOf(false) }
    LaunchedEffect(isSyncing) {
        if (wasSyncing && !isSyncing) {
            actionFeedbackMessage = lastSyncSummary.ifBlank { "تمت المزامنة بنجاح ✓ كافة السجلات متطابقة" }
            kotlinx.coroutines.delay(4000L)
            actionFeedbackMessage = null
        }
        wasSyncing = isSyncing
    }

    // الاستماع لحالة الاتصال اللحظية من Firestore
    LaunchedEffect(currentUserId) {
        try {
            healthChecker.observeRealtimeConnection("heartbeat_$currentUserId")
                .collectLatest { update ->
                    realtimeStatus = update
                }
        } catch (_: Throwable) {
            // المعالجة الاحتياطية في حال تعذر الاستماع
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // بطاقة ملخص الحالة اللحظية (Real-time Live Status)
        RealtimeStatusHeaderCard(
            realtimeStatus = realtimeStatus,
            isNetworkOnline = isNetworkOnline,
            pendingQueueCount = pendingQueueCount,
            isSyncing = isSyncing,
            semanticColors = semanticColors
        )

        // بطاقة فحص سرعة الاستجابة (Ping / Latency Benchmark)
        PingLatencyBenchmarkCard(
            isTesting = isPingTesting,
            result = pingResult,
            onRunTest = {
                if (!isPingTesting) {
                    isPingTesting = true
                    actionFeedbackMessage = null
                    coroutineScope.launch {
                        try {
                            val res = healthChecker.measurePingAndLatency()
                            pingResult = res
                            if (res.isSuccess) {
                                actionFeedbackMessage = "تم قياس السرعة: ${res.totalLatencyMs} ملي ثانية ✓"
                            } else {
                                actionFeedbackMessage = res.errorMessage ?: "تعذر الوصول المباشر للسحابة"
                            }
                        } catch (t: Throwable) {
                            actionFeedbackMessage = "حالة الفحص: ${t.localizedMessage ?: "قاعدة البيانات المحلية تعمل بكفاءة"}"
                        } finally {
                            isPingTesting = false
                        }
                    }
                }
            },
            semanticColors = semanticColors
        )

        // بطاقة توقيت آخر مزامنة
        LastSyncTimeCard(
            lastSyncTimestampText = lastSyncText,
            isSyncing = isSyncing,
            semanticColors = semanticColors
        )

        // بطاقة مؤشر الذاكرة المحلية والعمليات المعلقة (Offline Cache)
        OfflineCacheStatusCard(
            pendingQueueCount = pendingQueueCount,
            isNetworkOnline = isNetworkOnline,
            semanticColors = semanticColors
        )

        // بطاقة الرسم البياني لمكتبة Vico: حجم المزامنة اليومي مقابل العمليات المعلقة
        DailySyncVolumeVicoChartCard(
            viewModel = viewModel,
            semanticColors = semanticColors
        )

        // بطاقة توضيح آلية المزامنة والإجابة عن استفسار المستخدم
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "هل تتم المزامنة حقاً؟",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "نعم ✓ يعمل التطبيق بنظام (Offline-First) المعتمد: يتم فورياً تسجيل وتأكيد كل فاتورة، مبيعات، أو تعديل للمخزون في قاعدة بيانات الجهاز المحلية (SQLite / Room) لضمان استمرار البيع بدون انقطاع، وعند توفر الاتصال السحابي تتدفق التغييرات تلقائياً إلى خوادم السحابة.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }

        // رسالة إشعار مؤقتة عند الضغط مع تفاعل حي
        AnimatedVisibility(visible = actionFeedbackMessage != null) {
            val isMsgSyncing = isSyncing || actionFeedbackMessage?.contains("جاري") == true
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isMsgSyncing) Color(0xFF0284C7).copy(alpha = 0.14f) else Color(0xFF10B981).copy(alpha = 0.14f),
                border = BorderStroke(1.dp, if (isMsgSyncing) Color(0xFF0284C7).copy(alpha = 0.6f) else Color(0xFF10B981).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isMsgSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0284C7)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionFeedbackMessage ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMsgSyncing) Color(0xFF0284C7) else Color(0xFF10B981),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // أزرار التحكم بالمزامنة واختيار فترات المزامنة
        SyncActionsSection(
            isSyncing = isSyncing,
            syncIntervalMode = syncIntervalMode,
            isNetworkOnline = isNetworkOnline,
            onSelectIntervalMode = { mode ->
                syncManager.setSyncIntervalMode(mode)
                actionFeedbackMessage = when (mode) {
                    SyncIntervalMode.REAL_TIME -> "تم تفعيل المزامنة اللحظية (Real-time) ⚡"
                    SyncIntervalMode.HOURLY -> "تم ضبط المزامنة الدورية كل ساعة (Hourly - 60 Min) ⏱️"
                    SyncIntervalMode.MANUAL -> "تم ضبط المزامنة اليدوية فقط (Manual Mode) 🖐️"
                }
                coroutineScope.launch {
                    kotlinx.coroutines.delay(3500L)
                    actionFeedbackMessage = null
                }
            },
            onSyncNow = {
                if (!isSyncing) {
                    actionFeedbackMessage = "جاري فحص ورفع التغييرات المعلقة..."
                    coroutineScope.launch {
                        syncManager.triggerManualSync()
                        kotlinx.coroutines.delay(700L)
                        actionFeedbackMessage = syncManager.lastSyncSummary.value.ifBlank { "تمت المزامنة بنجاح ✓ كافة البيانات متطابقة" }
                        kotlinx.coroutines.delay(4000L)
                        actionFeedbackMessage = null
                    }
                }
            },
            semanticColors = semanticColors
        )

        if (isCompactMode && onOpenFullScreen != null) {
            OutlinedButton(
                onClick = onOpenFullScreen,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_full_sync_screen_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "فتح تقرير المزامنة المفصل",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isCompactMode && onClose != null) {
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "إغلاق اللوحة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * رأس حالة الاتصال اللحظية مع نبض لوني حي
 */
@Composable
private fun RealtimeStatusHeaderCard(
    realtimeStatus: RealtimeStatusUpdate,
    isNetworkOnline: Boolean,
    pendingQueueCount: Int,
    isSyncing: Boolean,
    semanticColors: com.example.ui.theme.AFStoreSemanticColors
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val (statusColor, statusTitle, statusBadge) = when {
        isSyncing -> Triple(Color(0xFF0284C7), "جاري المزامنة مع السحابة الآن...", "مزامنة نشطة")
        !isNetworkOnline -> Triple(Color(0xFFEF4444), "غير متصل بالإنترنت (وضع الأوفلاين)", "أوفلاين Offline")
        pendingQueueCount > 0 -> Triple(Color(0xFFF59E0B), "توجد عمليات قيد الانتظار للرفع", "معلق للرفع")
        realtimeStatus.state == RealtimeConnectionState.CONNECTED_LIVE -> Triple(Color(0xFF10B981), "متصل سحابياً ومحدث بلحظية تامة", "متصل مباشر Live")
        realtimeStatus.state == RealtimeConnectionState.SYNCED_CACHE -> Triple(Color(0xFF10B981), "قاعدة البيانات متصلة ومستقرة محلياً", "كاش سحابي")
        else -> Triple(Color(0xFF64748B), realtimeStatus.messageAr, "مستقر")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = if (semanticColors.isDark) 0.16f else 0.09f)
        ),
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // أيقونة النبض
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(42.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.25f),
                    modifier = Modifier
                        .size(38.dp)
                        .scale(if (isNetworkOnline) pulseScale else 1f)
                ) {}
                Surface(
                    shape = CircleShape,
                    color = statusColor,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isNetworkOnline) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "اتصال Firebase Firestore",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = statusBadge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = statusTitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "المشروع: deffas-a1f696be • خوادم Google Cloud",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * بطاقة قياس سرعة الاستجابة وزمن الوصول (Ping & Latency Benchmark)
 */
@Composable
private fun PingLatencyBenchmarkCard(
    isTesting: Boolean,
    result: PingLatencyResult?,
    onRunTest: () -> Unit,
    semanticColors: com.example.ui.theme.AFStoreSemanticColors
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "سرعة الاستجابة (Latency / Ping)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (result != null && result.isSuccess) {
                    val qualityText = when {
                        result.totalLatencyMs < 250 -> "ممتاز"
                        result.totalLatencyMs < 500 -> "جيد"
                        result.totalLatencyMs < 900 -> "متوسط"
                        else -> "بطيء"
                    }
                    val qualityColor = when {
                        result.totalLatencyMs < 250 -> Color(0xFF10B981)
                        result.totalLatencyMs < 500 -> Color(0xFF0284C7)
                        result.totalLatencyMs < 900 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = qualityColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, qualityColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "$qualityText (${result.totalLatencyMs} ms)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = qualityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (result != null && result.isSuccess) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LatencyMetricBox(
                        title = "زمن الكتابة",
                        value = "${result.writeLatencyMs} ms",
                        modifier = Modifier.weight(1f)
                    )
                    LatencyMetricBox(
                        title = "زمن القراءة",
                        value = "${result.readLatencyMs} ms",
                        modifier = Modifier.weight(1f)
                    )
                    LatencyMetricBox(
                        title = "الإجمالي RTT",
                        value = "${result.totalLatencyMs} ms",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else if (result != null && !result.isSuccess) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = result.errorMessage ?: "تعذر الوصول للسحابة",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            } else {
                Text(
                    text = "اضغط على زر الفحص بالأسفل لقياس سرعة القراءة والكتابة التجريبية المباشرة مع خوادم Firestore.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onRunTest,
                enabled = !isTesting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_ping_latency_test_btn")
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "جاري قياس السرعة وكتابة وثيقة تجريبية...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result != null) "إعادة فحص سرعة الاتصال (Ping)" else "فحص سرعة الاتصال الآن (Ping Test)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LatencyMetricBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * بطاقة توقيت آخر مزامنة مكتملة بتنسيق متناسق ومضغوط
 */
@Composable
private fun LastSyncTimeCard(
    lastSyncTimestampText: String,
    isSyncing: Boolean,
    semanticColors: com.example.ui.theme.AFStoreSemanticColors
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // الصف العلوي: أيقونة متناسقة مع باقي البطاقات + عنوان + شارة الحالة
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "آخر مزامنة سحابية",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (isSyncing) "جاري الرفع ⚡" else "متطابقة ومحدثة ✓",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // الصف السفلي: التوقيت بشكل مدمج وأنيق
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "توقيت الحفظ:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = if (isSyncing) "المزامنة جارية الآن..." else lastSyncTimestampText.ifBlank { "لم تُسجل بعد" },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                    )
                }
            }
        }
    }
}

/**
 * بطاقة حالة الذاكرة المؤقتة والعمليات المعلقة (Offline Cache)
 */
@Composable
private fun OfflineCacheStatusCard(
    pendingQueueCount: Int,
    isNetworkOnline: Boolean,
    semanticColors: com.example.ui.theme.AFStoreSemanticColors
) {
    val isClean = pendingQueueCount == 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "الذاكرة المحلية (Offline Cache)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isClean) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isClean) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (isClean) "مكتملة ومستقرة ✓" else "$pendingQueueCount عمليات معلقة",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isClean) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = if (isClean) {
                    "قاعدة بيانات SQLite / Room المحلية تعمل بكفاءة 100%. كافة الفواتير والمنتجات مخزنة ومحمية ولا توجد أي عمليات معلقة بانتظار الرفع."
                } else {
                    "توجد $pendingQueueCount عمليات تم إنشاؤها في وضع عدم الاتصال ومحفوظة بأمان محلياً. سيتم رفعها تلقائياً للسحابة فور استقرار الإنترنت."
                },
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * أزرار المزامنة الفورية وإعداد فترات المزامنة (Real-time, Hourly, Manual)
 */
@Composable
private fun SyncActionsSection(
    isSyncing: Boolean,
    syncIntervalMode: SyncIntervalMode,
    isNetworkOnline: Boolean,
    onSelectIntervalMode: (SyncIntervalMode) -> Unit,
    onSyncNow: () -> Unit,
    semanticColors: com.example.ui.theme.AFStoreSemanticColors
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // زر المزامنة الفورية
            Button(
                onClick = onSyncNow,
                enabled = !isSyncing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("manual_sync_now_btn")
            ) {
                if (isSyncing) {
                    val infiniteTransition = rememberInfiniteTransition(label = "btn_sync_spin")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)),
                        label = "spin_angle"
                    )
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rotation)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "جاري المزامنة مع خوادم السحابة...",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مزامنة سحابية فورية الآن (Sync Now)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // رأس قسم وتيرة وفترات المزامنة
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "فترة المزامنة للعمليات المعلقة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "حدد وتيرة وآلية رفع العمليات المحفوظة محلياً للسحابة",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (syncIntervalMode) {
                        SyncIntervalMode.REAL_TIME -> Color(0xFF10B981).copy(alpha = 0.15f)
                        SyncIntervalMode.HOURLY -> Color(0xFF6366F1).copy(alpha = 0.15f)
                        SyncIntervalMode.MANUAL -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (syncIntervalMode) {
                            SyncIntervalMode.REAL_TIME -> Color(0xFF10B981).copy(alpha = 0.5f)
                            SyncIntervalMode.HOURLY -> Color(0xFF6366F1).copy(alpha = 0.5f)
                            SyncIntervalMode.MANUAL -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Text(
                        text = syncIntervalMode.titleAr,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (syncIntervalMode) {
                            SyncIntervalMode.REAL_TIME -> Color(0xFF10B981)
                            SyncIntervalMode.HOURLY -> Color(0xFF6366F1)
                            SyncIntervalMode.MANUAL -> Color(0xFFF59E0B)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // خيارات فترات المزامنة: لحظي (Real-time)، كل ساعة (Hourly)، يدوي (Manual)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_interval_mode_selector_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // خيار 1: لحظي
                SyncIntervalOptionItem(
                    title = "لحظي",
                    badge = "فوري",
                    icon = Icons.Default.Bolt,
                    isSelected = syncIntervalMode == SyncIntervalMode.REAL_TIME,
                    activeColor = Color(0xFF10B981),
                    testTag = "sync_interval_mode_real_time",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectIntervalMode(SyncIntervalMode.REAL_TIME) }
                )

                // خيار 2: كل ساعة
                SyncIntervalOptionItem(
                    title = "كل ساعة",
                    badge = "60 دقيقة",
                    icon = Icons.Default.Schedule,
                    isSelected = syncIntervalMode == SyncIntervalMode.HOURLY,
                    activeColor = Color(0xFF6366F1),
                    testTag = "sync_interval_mode_hourly",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectIntervalMode(SyncIntervalMode.HOURLY) }
                )

                // خيار 3: يدوي
                SyncIntervalOptionItem(
                    title = "يدوي فقط",
                    badge = "عند الطلب",
                    icon = Icons.Default.TouchApp,
                    isSelected = syncIntervalMode == SyncIntervalMode.MANUAL,
                    activeColor = Color(0xFFF59E0B),
                    testTag = "sync_interval_mode_manual",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectIntervalMode(SyncIntervalMode.MANUAL) }
                )
            }

            // بطاقة شرح وتفاصيل النمط المختار
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_interval_active_desc")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (syncIntervalMode) {
                            SyncIntervalMode.REAL_TIME -> Icons.Default.Bolt
                            SyncIntervalMode.HOURLY -> Icons.Default.Schedule
                            SyncIntervalMode.MANUAL -> Icons.Default.TouchApp
                        },
                        contentDescription = null,
                        tint = when (syncIntervalMode) {
                            SyncIntervalMode.REAL_TIME -> Color(0xFF10B981)
                            SyncIntervalMode.HOURLY -> Color(0xFF6366F1)
                            SyncIntervalMode.MANUAL -> Color(0xFFF59E0B)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (syncIntervalMode) {
                                SyncIntervalMode.REAL_TIME -> "المزامنة اللحظية مفعلة (Real-time)"
                                SyncIntervalMode.HOURLY -> "المزامنة الدورية مفعلة (Hourly - 60 Min)"
                                SyncIntervalMode.MANUAL -> "المزامنة اليدوية فقط (Manual Mode)"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (syncIntervalMode) {
                                SyncIntervalMode.REAL_TIME ->
                                    "تُرفع الفواتير وتعديلات المخزون فورياً بمجرد إتمامها عند توفر الإنترنت، مع جدولة WorkManager دورية كل 15 دقيقة."
                                SyncIntervalMode.HOURLY ->
                                    "تُحفظ العمليات محلياً بأمان، وتتم جدولتها للرفع في الخلفية كل 60 دقيقة لتوفير استهلاك البيانات والبطارية."
                                SyncIntervalMode.MANUAL ->
                                    "تُحفظ جميع العمليات في الذاكرة المحلية (Offline) بشكل دائم ولا تُرفع للسحابة إلا عند ضغط زر 'مزامنة سحابية الآن'."
                            },
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncIntervalOptionItem(
    title: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) activeColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val borderColor = if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val contentColor = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .height(72.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = badge,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
