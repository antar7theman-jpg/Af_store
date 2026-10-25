package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Customer
import com.example.data.models.RecordSyncState
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.ScannedBarcodeResult
import com.example.utils.ScannerPreferencesManager
import com.example.utils.SmartTorchMode
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CouponScannerDialog(
    viewModel: AntarSalesViewModel,
    onDismiss: () -> Unit,
    onCouponScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val semantic = LocalAFStoreSemanticColors.current
    val customers by viewModel.customers.collectAsStateWithLifecycle(initialValue = emptyList())
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var scannedCode by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(0) } // 0: Camera QR, 1: Manual Input
    var validatedCustomer by remember { mutableStateOf<Customer?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmittingRedemption by remember { mutableStateOf(false) }

    // Camera scanner state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var smartTorchMode by remember {
        mutableStateOf(ScannerPreferencesManager.getSmartTorchMode(context))
    }
    var isSoundEnabled by remember {
        mutableStateOf(ScannerPreferencesManager.isSoundEnabled(context))
    }
    var isVibrationEnabled by remember {
        mutableStateOf(ScannerPreferencesManager.isVibrationEnabled(context))
    }
    var isAutoZoomEnabled by remember {
        mutableStateOf(ScannerPreferencesManager.isAutoZoomEnabled(context))
    }

    fun validateCode(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) {
            validatedCustomer = null
            validationError = "الرجاء إدخال أو مسح رمز القسيمة"
            return
        }

        // Try to match customer by multiple patterns:
        // 1. Direct ID (numeric)
        // 2. COUPON-123, LOYALTY-123, REWARD-123, CUST-123
        // 3. Customer phone number
        // 4. Exact Customer name
        val numericId = when {
            trimmed.startsWith("COUPON-", ignoreCase = true) -> trimmed.removePrefix("COUPON-").removePrefix("coupon-").toIntOrNull()
            trimmed.startsWith("LOYALTY-", ignoreCase = true) -> trimmed.removePrefix("LOYALTY-").removePrefix("loyalty-").toIntOrNull()
            trimmed.startsWith("REWARD-", ignoreCase = true) -> trimmed.removePrefix("REWARD-").removePrefix("reward-").toIntOrNull()
            trimmed.startsWith("CUST-", ignoreCase = true) -> trimmed.removePrefix("CUST-").removePrefix("cust-").toIntOrNull()
            trimmed.toIntOrNull() != null -> trimmed.toIntOrNull()
            else -> null
        }

        val found = if (numericId != null) {
            customers.find { it.id == numericId }
        } else {
            customers.find { it.phone.isNotBlank() && (it.phone == trimmed || trimmed.contains(it.phone)) }
                ?: customers.find { it.name.equals(trimmed, ignoreCase = true) }
        }

        if (found != null) {
            validatedCustomer = found
            validationError = null
        } else {
            validatedCustomer = null
            validationError = "لم يتم العثور على عميل مرتبط برمز القسيمة: '$trimmed'"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, semantic.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "مسح قسيمة مكافأة الولاء",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "التحقق الفوري من رصيد العميل وتطبيق الخصم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Mode Selector Tabs (Camera vs Manual)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TabButton(
                        title = "مسح بالكاميرا Live QR",
                        icon = Icons.Default.CameraAlt,
                        isSelected = selectedMode == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMode = 0 }
                    )
                    TabButton(
                        title = "إدخال يدوي / باركود",
                        icon = Icons.Default.Keyboard,
                        isSelected = selectedMode == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMode = 1 }
                    )
                }

                // Camera Scanner Section
                if (selectedMode == 0) {
                    if (hasCameraPermission) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        ) {
                            TurboCameraBarcodeHud(
                                isScanningActive = validatedCustomer == null,
                                smartTorchMode = smartTorchMode,
                                onToggleTorchMode = {
                                    val next = when (smartTorchMode) {
                                        SmartTorchMode.OFF -> SmartTorchMode.AUTO_SMART
                                        SmartTorchMode.AUTO_SMART -> SmartTorchMode.ALWAYS_ON
                                        SmartTorchMode.ALWAYS_ON -> SmartTorchMode.OFF
                                    }
                                    smartTorchMode = next
                                    ScannerPreferencesManager.setSmartTorchMode(context, next)
                                },
                                isSoundEnabled = isSoundEnabled,
                                onToggleSound = {
                                    isSoundEnabled = !isSoundEnabled
                                    ScannerPreferencesManager.setSoundEnabled(context, isSoundEnabled)
                                },
                                isVibrationEnabled = isVibrationEnabled,
                                onToggleVibration = {
                                    isVibrationEnabled = !isVibrationEnabled
                                    ScannerPreferencesManager.setVibrationEnabled(context, isVibrationEnabled)
                                },
                                isAutoZoomEnabled = isAutoZoomEnabled,
                                onToggleAutoZoom = {
                                    isAutoZoomEnabled = !isAutoZoomEnabled
                                    ScannerPreferencesManager.setAutoZoomEnabled(context, isAutoZoomEnabled)
                                },
                                hasActiveSelection = validatedCustomer != null,
                                lastDecodeBadge = null,
                                onResetScanClick = {
                                    validatedCustomer = null
                                    validationError = null
                                },
                                onBarcodeScanned = { result: ScannedBarcodeResult ->
                                    scannedCode = result.code
                                    validateCode(result.code)
                                },
                                compactMode = true
                            )
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Text("إذن الكاميرا مطلوب لمسح رموز QR تلقائياً", fontSize = 12.sp, textAlign = TextAlign.Center)
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    Text("منح إذن الكاميرا")
                                }
                            }
                        }
                    }
                }

                // Manual Input Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = scannedCode,
                            onValueChange = {
                                scannedCode = it
                                if (it.isBlank()) {
                                    validatedCustomer = null
                                    validationError = null
                                }
                            },
                            label = { Text("رمز القسيمة / رقم هاتف العميل / المعرف") },
                            placeholder = { Text("مثال: COUPON-1 أو 0662931569") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("coupon_scanner_input_field"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                        Button(
                            onClick = { validateCode(scannedCode) },
                            modifier = Modifier.height(54.dp).testTag("coupon_validate_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("فحص", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick test helpers if customers exist
                    if (customers.isNotEmpty() && validatedCustomer == null) {
                        Text(
                            text = "اختيار سريع لاختبار القسائم النشطة:",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            customers.take(3).forEach { cust ->
                                AssistChip(
                                    onClick = {
                                        scannedCode = "COUPON-${cust.id}"
                                        validateCode("COUPON-${cust.id}")
                                    },
                                    label = {
                                        Text("${cust.name} (${cust.loyaltyPoints} نقطة)", fontSize = 10.sp)
                                    }
                                )
                            }
                        }
                    }
                }

                // Validation Result View
                AnimatedVisibility(visible = validationError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Text(
                                text = validationError ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Valid Customer Card
                validatedCustomer?.let { cust ->
                    val isRewardEligible = cust.loyaltyPoints >= cust.loyaltyGoal
                    val neededPoints = (cust.loyaltyGoal - cust.loyaltyPoints).coerceAtLeast(0)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRewardEligible) Color(0xFF10B981).copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(
                            1.2.dp,
                            if (isRewardEligible) Color(0xFF10B981).copy(alpha = 0.5f) else semantic.cardBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Customer Header & Tier
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = "الهاتف: ${cust.phone.ifBlank { "غير مسجل" }}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (cust.loyaltyTier.lowercase()) {
                                        "platinum" -> Color(0xFF6366F1).copy(alpha = 0.15f)
                                        "gold" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        "silver" -> Color(0xFF64748B).copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    border = BorderStroke(1.dp, semantic.cardBorder)
                                ) {
                                    Text(
                                        text = "فئة: ${
                                            when (cust.loyaltyTier.lowercase()) {
                                                "platinum" -> "بلاتيني"
                                                "gold" -> "ذهبي"
                                                "silver" -> "فضي"
                                                else -> "عادي"
                                            }
                                        }",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.5f))

                            // Points & Progress
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("رصيد النقاط الحالي:", fontSize = 12.sp)
                                Text(
                                    text = "${cust.loyaltyPoints} / ${cust.loyaltyGoal} نقطة",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isRewardEligible) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (cust.loyaltyPoints.toFloat() / cust.loyaltyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (isRewardEligible) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            )

                            // Status Banner
                            if (isRewardEligible) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF047857).copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(
                                                text = "القسيمة صالحة ومؤهلة للاستبدال! 🎉",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = Color(0xFF047857)
                                            )
                                            Text(
                                                text = "قيمة الخصم النقدي: ${cust.loyaltyRewardValue} $currencySymbol",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF065F46)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706).copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "النقاط غير كافية للاستبدال. بحاجة إلى $neededPoints نقطة إضافية للوصول لهدف المكافأة.",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }

                            // Redeem Action Button
                            if (isRewardEligible) {
                                Button(
                                    onClick = {
                                        isSubmittingRedemption = true
                                        coroutineScope.launch {
                                            try {
                                                // Deduct the goal points from loyaltyPoints and update database
                                                val nextPoints = (cust.loyaltyPoints - cust.loyaltyGoal).coerceAtLeast(0)
                                                val updatedCust = cust.copy(
                                                    loyaltyPoints = nextPoints,
                                                    updatedAt = System.currentTimeMillis(),
                                                    syncStatus = RecordSyncState.PENDING
                                                )
                                                viewModel.repository.saveCustomer(updatedCust)
                                                Toast.makeText(
                                                    context,
                                                    "تم تطبيق قسيمة الخصم بنجاح بقيمة ${cust.loyaltyRewardValue} $currencySymbol! الرصيد المتبقي: $nextPoints نقطة",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onCouponScanned(scannedCode)
                                                onDismiss()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "فشل تطبيق القسيمة: ${e.message}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isSubmittingRedemption = false
                                            }
                                        }
                                    },
                                    enabled = !isSubmittingRedemption,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("apply_coupon_redemption_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isSubmittingRedemption) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "اعتماد وتطبيق الخصم فوراً (-${cust.loyaltyRewardValue} $currencySymbol)",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إغلاق النافذة")
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
