package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.StaffAccount
import com.example.data.firebase.StaffAccountRepository
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveSyncStatusCard(
    viewModel: AntarSalesViewModel,
    myStaffAccount: StaffAccount?,
    modifier: Modifier = Modifier
) {
    val semanticColors = LocalAFStoreSemanticColors.current
    val context = LocalContext.current

    val isLiveSyncActive by viewModel.liveInventorySyncManager.isLiveSyncActive.collectAsState()
    val isSyncing by viewModel.liveInventorySyncManager.isComprehensiveSyncing.collectAsState()
    val lastSyncTime by viewModel.liveInventorySyncManager.lastSyncTimestamp.collectAsState()
    val syncMessage by viewModel.liveInventorySyncManager.syncMessage.collectAsState()
    val syncedInvoicesCount by viewModel.liveInventorySyncManager.syncedInvoicesCount.collectAsState()
    val syncedLocationsCount by viewModel.liveInventorySyncManager.syncedLocationsCount.collectAsState()
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val workerLocations by viewModel.workerLocations.collectAsState()
    val pendingQueueCount by viewModel.repository.pendingSyncQueueCountFlow.collectAsState(initial = 0)

    var showDiagnostics by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotate")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val lastSyncFormatted = remember(lastSyncTime) {
        if (lastSyncTime > 0L) {
            SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.US).format(Date(lastSyncTime))
        } else {
            "بانتظار الفحص الأولي"
        }
    }

    val roleLabel = when {
        myStaffAccount?.isOwner == true -> "مالك المتجر والمنظومة الرئيسية"
        myStaffAccount?.isRegularManager == true -> "مدير مستقل (مسار المزامنة المباشر)"
        myStaffAccount?.role.equals("worker", ignoreCase = true) -> "عامل ميداني تابع للمدير"
        else -> "جلسة معتمدة ومتصلة"
    }

    val targetOwnerUid = remember(myStaffAccount) {
        viewModel.liveInventorySyncManager.resolveInventoryOwnerUid(myStaffAccount)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_sync_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = semanticColors.cardElevated
        ),
        border = BorderStroke(
            width = 1.3.dp,
            color = if (isLiveSyncActive) semanticColors.success.copy(alpha = 0.5f) else semanticColors.cardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(semanticColors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "مزامنة سحابية",
                            tint = semanticColors.primary,
                            modifier = Modifier
                                .size(26.dp)
                                .then(if (isSyncing) Modifier.rotate(rotationAngle) else Modifier)
                        )
                    }

                    Column {
                        Text(
                            text = "لوحة مراقبة المزامنة السحابية اللحظية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = semanticColors.textPrimary
                        )
                        Text(
                            text = "مزامنة الحسابات، السلع، الصور، المخزون، و GPS",
                            fontSize = 11.5.sp,
                            color = semanticColors.textSecondary
                        )
                    }
                }

                // Live Status Badge
                val isLiteAccount = myStaffAccount != null && !myStaffAccount.isSyncEnabled && !myStaffAccount.isOwner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSyncing -> Color(0xFF0284C7).copy(alpha = 0.15f)
                        isLiteAccount -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        isLiveSyncActive -> semanticColors.success.copy(alpha = 0.15f)
                        else -> semanticColors.debtAmber.copy(alpha = 0.15f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isSyncing -> Color(0xFF0284C7)
                            isLiteAccount -> Color(0xFFF59E0B)
                            isLiveSyncActive -> semanticColors.success
                            else -> semanticColors.debtAmber
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.8.dp,
                                color = Color(0xFF0284C7)
                            )
                            Text(
                                text = "جاري المزامنة...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        } else if (isLiteAccount) {
                            Text(
                                text = "وضع Just Offline ⚡",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        } else if (isLiveSyncActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(semanticColors.success)
                            )
                            Text(
                                text = "السحابة نشطة ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = semanticColors.success
                            )
                        } else {
                            Text(
                                text = "بانتظار الاتصال",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = semanticColors.debtAmber
                            )
                        }
                    }
                }
            }

            // User Identity & Scope Info Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = semanticColors.cardSubtle,
                border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ManageAccounts,
                            contentDescription = null,
                            tint = semanticColors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = roleLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = semanticColors.textPrimary
                        )
                    }

                    if (targetOwnerUid.isNotBlank()) {
                        Text(
                            text = "المعرف: ${targetOwnerUid.take(12)}...",
                            fontSize = 10.5.sp,
                            color = semanticColors.textSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = semanticColors.divider)

            // 4 Grid Metric Tiles
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SyncSubsystemItem(
                        icon = Icons.Default.Inventory2,
                        title = "السلع والصور",
                        value = "${products.size} صنفاً",
                        statusText = "صور Base64 خفيفة ومحدثة",
                        statusColor = semanticColors.success,
                        modifier = Modifier.weight(1f)
                    )
                    SyncSubsystemItem(
                        icon = Icons.Default.ReceiptLong,
                        title = "المخزون والفواتير",
                        value = if (syncedInvoicesCount > 0) "$syncedInvoicesCount فاتورة" else "${sales.size} عملية",
                        statusText = "خصم ذري وتوثيق فوري",
                        statusColor = semanticColors.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SyncSubsystemItem(
                        icon = Icons.Default.GpsFixed,
                        title = "تتبع مواقع GPS",
                        value = if (syncedLocationsCount > 0) "$syncedLocationsCount موقعاً" else "${workerLocations.size} عمال",
                        statusText = "تحديث دوري وعند البيع",
                        statusColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    SyncSubsystemItem(
                        icon = Icons.Default.Speed,
                        title = "قائمة الانتظار",
                        value = if (pendingQueueCount > 0) "$pendingQueueCount معلقة" else "مزامنة تامة ✓",
                        statusText = if (pendingQueueCount > 0) "بانتظار الرفع السحابي" else "كل العمليات محفوظة",
                        statusColor = if (pendingQueueCount > 0) semanticColors.debtAmber else semanticColors.success,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sync Status Message / Feedback
            if (!syncMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = semanticColors.cardSubtle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = semanticColors.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = syncMessage ?: "",
                            fontSize = 11.5.sp,
                            color = semanticColors.textPrimary
                        )
                    }
                }
            }

            // Last Sync Time Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر مزامنة ناجحة: $lastSyncFormatted",
                    fontSize = 11.sp,
                    color = semanticColors.textSecondary
                )

                Text(
                    text = if (showDiagnostics) "إخفاء التفاصيل ▲" else "تفاصيل الاتصال ▼",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = semanticColors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .padding(4.dp)
                )
            }

            // Expandable Diagnostics Box
            AnimatedVisibility(visible = showDiagnostics) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semanticColors.cardSubtle,
                    border = BorderStroke(1.dp, semanticColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "معلومات التشخيص التقنية:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = semanticColors.textPrimary
                        )
                        Text(
                            text = "• قاعدة البيانات: ${context.getString(com.example.R.string.firestore_database_id)}",
                            fontSize = 10.5.sp,
                            color = semanticColors.textSecondary
                        )
                        Text(
                            text = "• معرّف الجهاز المحلي: ${StaffAccountRepository.getOrCreateDeviceId(context).take(16)}...",
                            fontSize = 10.5.sp,
                            color = semanticColors.textSecondary
                        )
                        Text(
                            text = "• مسار السلع المشترك: users/$targetOwnerUid/products",
                            fontSize = 10.5.sp,
                            color = semanticColors.textSecondary
                        )
                        Text(
                            text = "• مسار الفواتير: users/$targetOwnerUid/invoices",
                            fontSize = 10.5.sp,
                            color = semanticColors.textSecondary
                        )
                    }
                }
            }

            // Comprehensive Sync Action Button
            Button(
                onClick = {
                    viewModel.triggerComprehensiveSync()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("trigger_comprehensive_sync_btn"),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = semanticColors.primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (isSyncing) "جاري فحص ومزامنة البيانات..." else "فحص ومزامنة شاملة الآن",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncSubsystemItem(
    icon: ImageVector,
    title: String,
    value: String,
    statusText: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    val semanticColors = LocalAFStoreSemanticColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = semanticColors.cardSubtle,
        border = BorderStroke(1.dp, semanticColors.cardBorder.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = semanticColors.textPrimary
                )
            }

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor
            )

            Text(
                text = statusText,
                fontSize = 9.5.sp,
                color = semanticColors.textSecondary,
                maxLines = 1
            )
        }
    }
}
