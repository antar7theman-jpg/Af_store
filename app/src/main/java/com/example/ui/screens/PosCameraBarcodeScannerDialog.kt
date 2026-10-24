package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Product
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.BarcodeFeedbackController
import com.example.utils.ContinuousScannedSessionItem
import com.example.utils.ScannedBarcodeResult
import com.example.utils.ScannerOperationMode
import com.example.utils.ScannerPreferencesManager
import com.example.utils.SmartTorchMode
import java.util.Locale

@Composable
fun PosCameraBarcodeScannerDialog(
    viewModel: AntarSalesViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
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

    var scanMode by remember {
        mutableStateOf(ScannerPreferencesManager.getOperationMode(context))
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

    var manualBarcodeInput by remember { mutableStateOf("") }
    var scannedBarcodeValue by remember { mutableStateOf<String?>(null) }
    var matchedProduct by remember { mutableStateOf<Product?>(null) }
    var isScannedAsCarton by remember { mutableStateOf(false) }
    var selectedUnitsQuantity by remember { mutableIntStateOf(1) }
    var quantityTextInput by remember { mutableStateOf("1") }
    var markItemAsDebt by remember { mutableStateOf(false) }
    var showComprehensiveNewProductModal by remember { mutableStateOf(false) }
    var lastDecodeSpeedBadge by remember { mutableStateOf<String?>(null) }
    var sessionScannedIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    fun handleBarcodeDetected(
        rawCode: String,
        formatLabel: String = "BARCODE",
        decodeTimeMs: Long = 0L
    ) {
        val code = rawCode.trim()
        if (code.isEmpty()) return
        val found = products.find {
            it.barcode.equals(code, ignoreCase = true) ||
                (it.cartonBarcode.isNotBlank() && it.cartonBarcode.equals(code, ignoreCase = true))
        }

        if (found != null) {
            val cartonScan = found.cartonBarcode.isNotBlank() && found.cartonBarcode.equals(code, ignoreCase = true)
            val unitsToAdd = if (cartonScan) found.unitsPerCarton.coerceAtLeast(1) else 1

            BarcodeFeedbackController.triggerScanSuccessFeedback(
                context = context,
                soundEnabled = isSoundEnabled,
                vibrationEnabled = isVibrationEnabled,
                isCartonBarcode = cartonScan
            )

            lastDecodeSpeedBadge = if (decodeTimeMs > 0L) {
                "✓ ${found.name} • $formatLabel (${decodeTimeMs}ms)"
            } else {
                "✓ ${found.name} ($formatLabel)"
            }

            if (scanMode == ScannerOperationMode.INSTANT_CONTINUOUS) {
                viewModel.addToCartWithQuantity(
                    product = found,
                    totalUnitsToAdd = unitsToAdd,
                    asCarton = cartonScan,
                    markAsDebt = false
                )
                sessionScannedIds = (listOf(found.id) + sessionScannedIds).distinct().take(6)
                scannedBarcodeValue = null
                matchedProduct = null
            } else {
                scannedBarcodeValue = code
                matchedProduct = found
                isScannedAsCarton = cartonScan
                selectedUnitsQuantity = unitsToAdd
                quantityTextInput = unitsToAdd.toString()
                markItemAsDebt = false
            }
        } else {
            BarcodeFeedbackController.triggerUnregisteredBarcodeFeedback(
                context = context,
                soundEnabled = isSoundEnabled,
                vibrationEnabled = isVibrationEnabled
            )
            lastDecodeSpeedBadge = "باركود غير مسجل: $code"
            scannedBarcodeValue = code
            matchedProduct = null
        }
    }

    fun updateQuantity(newQty: Int) {
        val safeQty = newQty.coerceAtLeast(1)
        selectedUnitsQuantity = safeQty
        quantityTextInput = safeQty.toString()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .fillMaxHeight(0.94f),
                shape = RoundedCornerShape(26.dp),
                color = colorScheme.surface,
                contentColor = colorScheme.onSurface,
                tonalElevation = 6.dp,
                shadowElevation = 20.dp,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Surface(
                        color = colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primaryContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "ماسح الباركود السريع (Turbo Scanner)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = scanMode.subtitleAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("close_camera_scanner_btn")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = colorScheme.onSurface
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.6f))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mode Switcher: Instant Continuous vs Quantity Confirm
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScannerOperationMode.entries.forEach { mode ->
                                val isSelected = scanMode == mode
                                Surface(
                                    onClick = {
                                        scanMode = mode
                                        ScannerPreferencesManager.setOperationMode(context, mode)
                                        if (mode == ScannerOperationMode.INSTANT_CONTINUOUS && matchedProduct != null) {
                                            matchedProduct = null
                                            scannedBarcodeValue = null
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLow,
                                    border = BorderStroke(
                                        1.2.dp,
                                        if (isSelected) colorScheme.primary else colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scanner_mode_${mode.id}_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = mode.titleAr,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // 1. Live Camera Viewfinder Box
                        val isCameraScanningActive = when (scanMode) {
                            ScannerOperationMode.INSTANT_CONTINUOUS -> scannedBarcodeValue == null
                            ScannerOperationMode.QUANTITY_CONFIRM -> matchedProduct == null && scannedBarcodeValue == null
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isCameraScanningActive) 245.dp else 140.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(
                                1.5.dp,
                                if (matchedProduct != null) Color(0xFF10B981) else colorScheme.primary
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (hasCameraPermission) {
                                    TurboCameraBarcodeHud(
                                        isScanningActive = isCameraScanningActive,
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
                                            val next = !isSoundEnabled
                                            isSoundEnabled = next
                                            ScannerPreferencesManager.setSoundEnabled(context, next)
                                        },
                                        isVibrationEnabled = isVibrationEnabled,
                                        onToggleVibration = {
                                            val next = !isVibrationEnabled
                                            isVibrationEnabled = next
                                            ScannerPreferencesManager.setVibrationEnabled(context, next)
                                        },
                                        isAutoZoomEnabled = isAutoZoomEnabled,
                                        onToggleAutoZoom = {
                                            val next = !isAutoZoomEnabled
                                            isAutoZoomEnabled = next
                                            ScannerPreferencesManager.setAutoZoomEnabled(context, next)
                                        },
                                        hasActiveSelection = matchedProduct != null,
                                        lastDecodeBadge = lastDecodeSpeedBadge,
                                        onResetScanClick = {
                                            scannedBarcodeValue = null
                                            matchedProduct = null
                                        },
                                        onBarcodeScanned = { scanResult: ScannedBarcodeResult ->
                                            handleBarcodeDetected(
                                                rawCode = scanResult.code,
                                                formatLabel = scanResult.formatLabel,
                                                decodeTimeMs = scanResult.decodeTimeMs
                                            )
                                        }
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "يرجى السماح بصلاحية الكاميرا لمسح الباركود مباشرة",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تفعيل الكاميرا الآن", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Instant Continuous Session Ticker (Quick +/- adjustments without pausing camera)
                        val activeContinuousCartItems = remember(cartItems, sessionScannedIds) {
                            sessionScannedIds.mapNotNull { id ->
                                cartItems.find { it.product.id == id }
                            }
                        }
                        AnimatedVisibility(
                            visible = scanMode == ScannerOperationMode.INSTANT_CONTINUOUS && activeContinuousCartItems.isNotEmpty()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("continuous_scan_live_strip")
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
                                        Text(
                                            text = "⚡ السلع الممسوحة فوراً (${activeContinuousCartItems.size} صنف في السلة)",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF059669)
                                        )
                                        TextButton(
                                            onClick = onDismiss,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("إتمام ومراجعة السلة ✓", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }

                                    activeContinuousCartItems.take(3).forEach { cItem ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colorScheme.surface
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = cItem.product.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.2f", cItem.lineSubtotal)} $currencySymbol",
                                                        fontSize = 11.sp,
                                                        color = colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    FilledIconButton(
                                                        onClick = {
                                                            viewModel.adjustCartItemUnits(cItem.product.id, -1)
                                                        },
                                                        modifier = Modifier.size(30.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(15.dp))
                                                    }
                                                    Text(
                                                        text = "${cItem.quantity} ق",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 13.sp
                                                    )
                                                    FilledIconButton(
                                                        onClick = {
                                                            viewModel.adjustCartItemUnits(cItem.product.id, 1)
                                                        },
                                                        modifier = Modifier.size(30.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Quantity Selection Card when a Product is Scanned / Selected
                        AnimatedVisibility(visible = matchedProduct != null) {
                            matchedProduct?.let { product ->
                                val unitsPerCartonSafe = product.unitsPerCarton.coerceAtLeast(1)
                                val fullCartons = selectedUnitsQuantity / unitsPerCartonSafe
                                val looseUnits = selectedUnitsQuantity % unitsPerCartonSafe
                                val lineTotalPrice = selectedUnitsQuantity * product.price

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("scanned_product_quantity_card"),
                                    shape = RoundedCornerShape(20.dp),
                                    color = colorScheme.primaryContainer.copy(alpha = 0.28f),
                                    border = BorderStroke(1.8.dp, colorScheme.primary)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Header: Product Name + Scanned Badge + Price
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.Start
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isScannedAsCarton) Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                        else Color(0xFF10B981).copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = if (isScannedAsCarton) "📦 باركود كرتونة" else "🏷️ باركود قطعة",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (isScannedAsCarton) Color(0xFFD97706) else Color(0xFF059669),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = scannedBarcodeValue ?: product.barcode,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        color = colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = product.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 17.sp,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "السعر: ${String.format(Locale.US, "%.2f", product.price)} $currencySymbol / قطعة  •  المخزون: ${product.stock} ق",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Total Price Badge
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = colorScheme.primaryContainer
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = "الإجمالي",
                                                        fontSize = 10.5.sp,
                                                        color = colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.2f", lineTotalPrice)} $currencySymbol",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 15.sp,
                                                        color = colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.6f))

                                        // Quantity Entry & Stepper Section
                                        Text(
                                            text = "حدد الكمية المراد بيعها:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colorScheme.onSurface
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Decrement 1 Piece
                                            FilledIconButton(
                                                onClick = { updateQuantity(selectedUnitsQuantity - 1) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("scanner_qty_minus_btn"),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = colorScheme.surfaceContainerHighest,
                                                    contentColor = colorScheme.onSurface
                                                )
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "إنقاص قطعة")
                                            }

                                            // Direct Quantity TextField
                                            OutlinedTextField(
                                                value = quantityTextInput,
                                                onValueChange = { raw ->
                                                    quantityTextInput = raw.filter { it.isDigit() }
                                                    val parsed = quantityTextInput.toIntOrNull()
                                                    if (parsed != null && parsed > 0) {
                                                        selectedUnitsQuantity = parsed
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("scanner_qty_input"),
                                                label = { Text("الكمية بالقطعة") },
                                                suffix = { Text("قطعة", fontWeight = FontWeight.Bold) },
                                                singleLine = true,
                                                textStyle = LocalTextStyle.current.copy(
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 18.sp
                                                ),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = colorScheme.surface,
                                                    unfocusedContainerColor = colorScheme.surface
                                                )
                                            )

                                            // Increment 1 Piece
                                            FilledIconButton(
                                                onClick = { updateQuantity(selectedUnitsQuantity + 1) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("scanner_qty_plus_btn"),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = colorScheme.primary,
                                                    contentColor = colorScheme.onPrimary
                                                )
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "زيادة قطعة")
                                            }
                                        }

                                        // Quick Piece Preset Chips + Carton Stepper Buttons
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf(1, 2, 5, 10, 12, 24).forEach { preset ->
                                                val isPresetSelected = selectedUnitsQuantity == preset
                                                Surface(
                                                    onClick = { updateQuantity(preset) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isPresetSelected) colorScheme.primary else colorScheme.surface,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isPresetSelected) colorScheme.primary else colorScheme.outlineVariant
                                                    ),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "$preset ق",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (isPresetSelected) colorScheme.onPrimary else colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }

                                            if (unitsPerCartonSafe > 1) {
                                                Surface(
                                                    onClick = { updateQuantity(selectedUnitsQuantity + unitsPerCartonSafe) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = colorScheme.secondaryContainer,
                                                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f)),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "📦 +1 كرتونة ($unitsPerCartonSafe ق)",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Equivalent Package & Debt Option Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📦 يعادل: $fullCartons كرتونة + $looseUnits قطعة",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.primary
                                            )

                                            FilterChip(
                                                selected = markItemAsDebt,
                                                onClick = { markItemAsDebt = !markItemAsDebt },
                                                label = {
                                                    Text(
                                                        text = if (markItemAsDebt) "مسجل دين (آجل)" else "+ تسجيل كدين",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.CreditScore,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            )
                                        }

                                        // Primary Action Buttons: Add to Cart & Close OR Add & Scan Next
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Add to Cart & Close
                                            Button(
                                                onClick = {
                                                    viewModel.addToCartWithQuantity(
                                                        product = product,
                                                        totalUnitsToAdd = selectedUnitsQuantity,
                                                        asCarton = isScannedAsCarton || fullCartons > 0,
                                                        markAsDebt = markItemAsDebt
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        "تمت إضافة $selectedUnitsQuantity ق من ${product.name} إلى السلة ✅",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .weight(1.15f)
                                                    .height(48.dp)
                                                    .testTag("pos_scanner_confirm_add_btn"),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colorScheme.primary,
                                                    contentColor = colorScheme.onPrimary
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Default.ShoppingCartCheckout,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "وضع في سلة المبيعات",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.5.sp,
                                                    maxLines = 1
                                                )
                                            }

                                            // Add & Scan Another
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.addToCartWithQuantity(
                                                        product = product,
                                                        totalUnitsToAdd = selectedUnitsQuantity,
                                                        asCarton = isScannedAsCarton || fullCartons > 0,
                                                        markAsDebt = markItemAsDebt
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        "أضيفت للسلة (${selectedUnitsQuantity} ق) — جاهز لمسح التالي 🔄",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    scannedBarcodeValue = null
                                                    matchedProduct = null
                                                    manualBarcodeInput = ""
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("pos_scanner_add_and_continue_btn"),
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.2.dp, colorScheme.primary)
                                            ) {
                                                Icon(
                                                    Icons.Default.QrCodeScanner,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "إضافة ومسح آخر",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Interactive Resolution Card if Scanned Barcode is Not Registered in Inventory
                        AnimatedVisibility(visible = scannedBarcodeValue != null && matchedProduct == null) {
                            val currentUnregisteredCode = scannedBarcodeValue ?: ""
                            UnregisteredBarcodeResolutionCard(
                                scannedBarcode = currentUnregisteredCode,
                                products = products,
                                currencySymbol = currencySymbol,
                                onLinkToExistingProduct = { prod, asCartonBarcode ->
                                    viewModel.linkBarcodeToExistingProduct(
                                        product = prod,
                                        barcode = currentUnregisteredCode,
                                        asCartonBarcode = asCartonBarcode,
                                        addToCartImmediately = true
                                    )
                                    Toast.makeText(
                                        context,
                                        "تم ربط الباركود ($currentUnregisteredCode) بالسلعة (${prod.name}) وإضافتها للفاتورة ✅",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    scannedBarcodeValue = null
                                    matchedProduct = null
                                    manualBarcodeInput = ""
                                },
                                onOpenCreateNewProduct = {
                                    showComprehensiveNewProductModal = true
                                },
                                onResetScan = {
                                    scannedBarcodeValue = null
                                    matchedProduct = null
                                },
                                onQuickCreateAndAddToCart = { name, unitPrice, purchasePrice, asCartonBarcode, unitsPerCarton, quantityToAddInCart ->
                                    viewModel.quickCreateProductAndAddToCart(
                                        name = name,
                                        unitPrice = unitPrice,
                                        purchasePrice = purchasePrice,
                                        barcode = currentUnregisteredCode,
                                        asCartonBarcode = asCartonBarcode,
                                        unitsPerCarton = unitsPerCarton,
                                        quantityToAddInCart = quantityToAddInCart
                                    )
                                    Toast.makeText(
                                        context,
                                        "تم تعريف السلعة ($name) وإضافتها للفاتورة فوراً ✅",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    scannedBarcodeValue = null
                                    matchedProduct = null
                                    manualBarcodeInput = ""
                                }
                            )
                        }

                        // 3. Manual Barcode Input + Quick Product Barcode Selector
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.7f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "أو أدخل رقم الباركود يدوياً / اختر سلعة لتحديد كميتها:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = manualBarcodeInput,
                                        onValueChange = { manualBarcodeInput = it },
                                        placeholder = { Text("اكتب رقم الباركود هنا...", fontSize = 13.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(19.dp))
                                        },
                                        trailingIcon = {
                                            if (manualBarcodeInput.isNotEmpty()) {
                                                IconButton(onClick = { manualBarcodeInput = "" }) {
                                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Search
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                if (manualBarcodeInput.isNotBlank()) {
                                                    handleBarcodeDetected(manualBarcodeInput)
                                                }
                                            }
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("manual_barcode_scanner_input")
                                    )

                                    Button(
                                        onClick = {
                                            if (manualBarcodeInput.isNotBlank()) {
                                                handleBarcodeDetected(manualBarcodeInput)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("manual_barcode_lookup_btn")
                                    ) {
                                        Text("بحث", fontWeight = FontWeight.ExtraBold)
                                    }
                                }

                                // Quick Barcodes from Catalog for instant selection/testing
                                if (products.isNotEmpty()) {
                                    Text(
                                        text = "باركودات السلع المتاحة (اضغط لاختيار الكمية):",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        products.filter { it.barcode.isNotBlank() }.forEach { prod ->
                                            val isSelectedProd = matchedProduct?.id == prod.id
                                            Surface(
                                                onClick = { handleBarcodeDetected(prod.barcode) },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelectedProd) colorScheme.primaryContainer else colorScheme.surface,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelectedProd) colorScheme.primary else colorScheme.outlineVariant
                                                ),
                                                modifier = Modifier.testTag("quick_scan_product_${prod.id}")
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalAlignment = Alignment.Start
                                                ) {
                                                    Text(
                                                        text = prod.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "🏷️ ${prod.barcode}",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.5.sp,
                                                        color = colorScheme.primary
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
            }
        }
    }

    if (showComprehensiveNewProductModal && scannedBarcodeValue != null) {
        val codeToRegister = scannedBarcodeValue ?: ""
        NewPurchaseProductModal(
            currencySymbol = currencySymbol,
            initialUnitBarcode = codeToRegister,
            confirmButtonText = "حفظ السلعة وإضافتها للفاتورة ✓",
            quantityCardTitleCarton = "الكمية المتوفرة بالمخزن (بالعبوات): 📦",
            quantityCardTitleUnit = "الكمية المتوفرة بالمخزن (بالقطعة): 📦",
            onDismiss = {
                showComprehensiveNewProductModal = false
            },
            onAddProductToInvoice = { newItem ->
                viewModel.createNewProductFromComprehensiveModal(newItem, addToPosCartImmediately = true)
                Toast.makeText(
                    context,
                    "تم إنشاء السلعة (${newItem.productName}) وإضافتها للفاتورة ✅",
                    Toast.LENGTH_SHORT
                ).show()
                showComprehensiveNewProductModal = false
                scannedBarcodeValue = null
                matchedProduct = null
                manualBarcodeInput = ""
            }
        )
    }
}

@Composable
fun SimpleBarcodeCaptureDialog(
    title: String,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(265.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        TurboCameraBarcodeHud(
                            isScanningActive = true,
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
                                val next = !isSoundEnabled
                                isSoundEnabled = next
                                ScannerPreferencesManager.setSoundEnabled(context, next)
                            },
                            isVibrationEnabled = isVibrationEnabled,
                            onToggleVibration = {
                                val next = !isVibrationEnabled
                                isVibrationEnabled = next
                                ScannerPreferencesManager.setVibrationEnabled(context, next)
                            },
                            isAutoZoomEnabled = isAutoZoomEnabled,
                            onToggleAutoZoom = {
                                val next = !isAutoZoomEnabled
                                isAutoZoomEnabled = next
                                ScannerPreferencesManager.setAutoZoomEnabled(context, next)
                            },
                            hasActiveSelection = false,
                            lastDecodeBadge = null,
                            onResetScanClick = {},
                            onBarcodeScanned = { scanned ->
                                BarcodeFeedbackController.triggerScanSuccessFeedback(
                                    context = context,
                                    soundEnabled = isSoundEnabled,
                                    vibrationEnabled = isVibrationEnabled,
                                    isCartonBarcode = false
                                )
                                onBarcodeScanned(scanned.code.trim())
                            }
                        )
                    }
                    Text(
                        text = "وجّه الكاميرا نحو الباركود ليتم التقاطه فوراً (يدعم التركيز باللمس والتقريب التلقائي)",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "يرجى منح صلاحية الكاميرا لمسح الباركود",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("تفعيل الكاميرا")
                    }
                }
            }
        }
    }
}

