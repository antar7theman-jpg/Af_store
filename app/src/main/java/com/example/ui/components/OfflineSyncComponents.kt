package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.SyncQueueItem
import com.example.utils.OfflineSyncManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شريط/مؤشر حالة المزامنة اللحظية (Sync Status Chip)
 * يعرض حالة الاتصال أونلاين/أوفلاين وطابور المزامنة مع خيار المزامنة الآلية.
 */
@Composable
fun SyncStatusChip(
    syncManager: OfflineSyncManager,
    pendingQueueCount: Int,
    modifier: Modifier = Modifier
) {
    val isOnline by syncManager.isOnline.collectAsState()
    val isSyncing by syncManager.isSyncing.collectAsState()
    val isAutoSyncEnabled by syncManager.isAutoSyncEnabled.collectAsState()
    var showDetailsSheet by remember { mutableStateOf(false) }

    val statusBgColor = when {
        isSyncing -> Color(0xFF0284C7) // Sky Blue
        !isOnline -> Color(0xFFD97706) // Amber / Warning
        pendingQueueCount > 0 -> Color(0xFF2563EB) // Royal Blue
        isAutoSyncEnabled -> Color(0xFF059669) // Emerald Green
        else -> Color(0xFF6B7280) // Gray
    }

    val statusIcon = when {
        isSyncing -> Icons.Default.CloudSync
        !isOnline -> Icons.Default.CloudOff
        pendingQueueCount > 0 -> Icons.Default.CloudQueue
        else -> Icons.Default.CloudDone
    }

    val statusLabel = when {
        isSyncing -> "جاري المزامنة..."
        !isOnline -> "أوفلاين"
        pendingQueueCount > 0 -> "معلق ($pendingQueueCount)"
        isAutoSyncEnabled -> "مزامنة آلية"
        else -> "متصل ومزامن"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = statusBgColor.copy(alpha = 0.15f),
        border = BorderStroke(1.2.dp, statusBgColor.copy(alpha = 0.6f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDetailsSheet = true }
            .testTag("sync_status_chip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            if (isSyncing) {
                val infiniteTransition = rememberInfiniteTransition(label = "sync_rotate")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)),
                    label = "sync_spin"
                )
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = statusBgColor,
                    modifier = Modifier
                        .size(15.dp)
                        .rotate(rotation)
                )
            } else {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusBgColor,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = statusLabel,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = statusBgColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (pendingQueueCount > 0 && !isSyncing) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = statusBgColor,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (pendingQueueCount > 99) "99+" else pendingQueueCount.toString(),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showDetailsSheet) {
        SyncDetailsModalSheet(
            syncManager = syncManager,
            pendingQueueCount = pendingQueueCount,
            onDismiss = { showDetailsSheet = false }
        )
    }
}

/**
 * زر المزامنة الآلية بالشريط العلوي (TopBar Auto Sync Button)
 */
@Composable
fun TopBarAutoSyncButton(
    syncManager: OfflineSyncManager,
    pendingQueueCount: Int,
    onClick: (() -> Unit)? = null
) {
    val isOnline by syncManager.isOnline.collectAsState()
    val isSyncing by syncManager.isSyncing.collectAsState()
    val isAutoSyncEnabled by syncManager.isAutoSyncEnabled.collectAsState()
    var showDetailsSheet by remember { mutableStateOf(false) }

    val activeColor = if (isAutoSyncEnabled) Color(0xFF059669) else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = activeColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                if (onClick != null) onClick() else showDetailsSheet = true
            }
            .testTag("topbar_auto_sync_btn")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (isSyncing) {
                val infiniteTransition = rememberInfiniteTransition(label = "topbar_sync_rotate")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)),
                    label = "topbar_spin"
                )
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "جاري المزامنة",
                    tint = activeColor,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "المزامنة الآلية",
                    tint = activeColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = if (isAutoSyncEnabled) "مزامنة آليّة" else "مزامنة",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeColor
            )

            if (pendingQueueCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (pendingQueueCount > 99) "99+" else pendingQueueCount.toString(),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showDetailsSheet && onClick == null) {
        SyncDetailsModalSheet(
            syncManager = syncManager,
            pendingQueueCount = pendingQueueCount,
            onDismiss = { showDetailsSheet = false }
        )
    }
}

/**
 * حوار تفاصيل وحالة المزامنة المتقدم (Sync Details Modal Bottom Sheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDetailsModalSheet(
    syncManager: OfflineSyncManager,
    pendingQueueCount: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isOnline by syncManager.isOnline.collectAsState()
    val isSyncing by syncManager.isSyncing.collectAsState()
    val isAutoSyncEnabled by syncManager.isAutoSyncEnabled.collectAsState()
    val lastSyncTimeMillis by syncManager.lastSyncTimeMillis.collectAsState()
    val lastSyncSummary by syncManager.lastSyncSummary.collectAsState()

    val formattedLastSync = remember(lastSyncTimeMillis) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastSyncTimeMillis))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("sync_details_modal_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إدارة المزامنة والتزامن اللحظي",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOnline) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (isOnline) Color(0xFF166534) else Color(0xFFB45309))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.SignalCellular4Bar else Icons.Default.SignalCellularOff,
                            contentDescription = null,
                            tint = if (isOnline) Color(0xFF15803D) else Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOnline) "أونلاين" else "أوفلاين",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Auto-Sync Switch Toggle Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAutoSyncEnabled) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isAutoSyncEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoMode,
                            contentDescription = null,
                            tint = if (isAutoSyncEnabled) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "المزامنة الآلية (Auto Sync)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAutoSyncEnabled)
                                    "تتم المزامنة تلقائياً عند توفر الإنترنت وكل 20 ثانية"
                                else
                                    "المزامنة اليدوية فقط عند الضغط على الزر",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isAutoSyncEnabled,
                        onCheckedChange = { syncManager.setAutoSyncEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF059669)
                        ),
                        modifier = Modifier.testTag("auto_sync_switch")
                    )
                }
            }

            // Status Card Summary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حالة طابور المزامنة المحلي:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (pendingQueueCount == 0) "مكتملة ومزامنة بالكامل" else "$pendingQueueCount معاملة معلقة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (pendingQueueCount == 0) Color(0xFF059669) else Color(0xFFD97706)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تاريخ آخر مزامنة ناجحة:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedLastSync,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = lastSyncSummary,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Sync Now Button
            Button(
                onClick = { syncManager.triggerManualSync() },
                enabled = !isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("trigger_manual_sync_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSyncing) {
                    val infiniteTransition = rememberInfiniteTransition(label = "btn_sync_rotate")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)),
                        label = "btn_spin"
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rotation)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري تفريغ ومزامنة الطابور...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مزامنة الآن (Sync Now)", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
