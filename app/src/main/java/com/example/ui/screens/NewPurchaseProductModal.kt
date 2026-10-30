package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoiceItem
import com.example.ui.theme.LocalAFStoreSemanticColors
import java.util.Locale

@Composable
fun NewPurchaseProductModal(
    currencySymbol: String,
    initialUnitBarcode: String = "",
    initialCartonBarcode: String = "",
    confirmButtonText: String = "إضافة السلعة للفاتورة",
    quantityCardTitleCarton: String = "كمية الشراء بالفاتورة (بالعبوات): 🛒",
    quantityCardTitleUnit: String = "كمية الشراء بالفاتورة (بالقطعة): 🛒",
    onDismiss: () -> Unit,
    onAddProductToInvoice: (PurchaseInvoiceItem) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val isDark = semantic.isDark
    val primaryBlue = Color(0xFF0077C2)
    val modalBgColor = if (semantic.isOledBlack) Color(0xFF050811) else if (isDark) Color(0xFF0F172A) else Color(0xFFF0F4F9)
    val cardWhiteColor = semantic.cardElevated
    val softBlueCardBg = if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.35f) else Color(0xFFEBF5FF)
    val selectedOptionBlueBg = if (isDark) Color(0xFF0284C7).copy(alpha = 0.45f) else Color(0xFFB3E5FC)
    val emeraldGreen = semantic.profitGreen

    var productName by remember { mutableStateOf("") }
    var productImagePath by remember { mutableStateOf("") }

    // Registration mode: true = بالعبوة / طرد 📦, false = بالقطعة الفردية 🏷️
    var isCartonMode by remember { mutableStateOf(true) }

    // Cost & Units inputs
    var unitsPerCartonInput by remember { mutableStateOf("12") }
    var cartonCostInput by remember { mutableStateOf("") }
    var singleUnitCostInput by remember { mutableStateOf("") }

    // Pricing mode: "CARTON_PRICE", "UNIT_PRICE", "PROFIT_PERCENT", "FIXED_PROFIT"
    var pricingMode by remember { mutableStateOf("CARTON_PRICE") }
    var pricingValueInput by remember { mutableStateOf("") }

    // Purchased quantity in this invoice
    var purchasedQtyInput by remember { mutableStateOf("1") }

    // Barcodes
    var unitBarcode by remember(initialUnitBarcode) { mutableStateOf(initialUnitBarcode) }
    var cartonBarcode by remember(initialCartonBarcode) { mutableStateOf(initialCartonBarcode) }

    // Camera scanner target: null, "UNIT", or "CARTON"
    var activeScannerTarget by remember { mutableStateOf<String?>(null) }

    // Computed values
    val effectiveUnitsPerCarton = if (isCartonMode) {
        (unitsPerCartonInput.toIntOrNull() ?: 12).coerceAtLeast(1)
    } else {
        1
    }
    val cartonCostVal = (cartonCostInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val unitCostVal = if (isCartonMode) {
        cartonCostVal / effectiveUnitsPerCarton.toDouble()
    } else {
        (singleUnitCostInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    }

    val rawPricingVal = (pricingValueInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val finalUnitSellingPrice = when (pricingMode) {
        "CARTON_PRICE" -> if (effectiveUnitsPerCarton > 0) rawPricingVal / effectiveUnitsPerCarton.toDouble() else 0.0
        "UNIT_PRICE" -> rawPricingVal
        "PROFIT_PERCENT" -> unitCostVal * (1.0 + rawPricingVal / 100.0)
        "FIXED_PROFIT" -> unitCostVal + rawPricingVal
        else -> 0.0
    }
    val unitNetProfit = finalUnitSellingPrice - unitCostVal

    val purchasedCount = (purchasedQtyInput.toIntOrNull() ?: 1).coerceAtLeast(1)
    val totalAddedUnits = if (isCartonMode) purchasedCount * effectiveUnitsPerCarton else purchasedCount
    val totalItemInvoiceCost = if (isCartonMode) {
        purchasedCount * cartonCostVal
    } else {
        purchasedCount * unitCostVal
    }

    val canAddItem = productName.trim().isNotEmpty() && finalUnitSellingPrice > 0.0

    fun generateRandomBarcode(prefix: String): String {
        val randomPart = ((System.currentTimeMillis() + (1000..9999).random()) % 10000000000L)
            .toString()
            .padStart(10, '0')
        return "$prefix$randomPart"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = modalBgColor,
                tonalElevation = 8.dp,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // ── Top Header: تسجيل سلعة جديدة تماماً للمخزن ✨ ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل سلعة جديدة تماماً للمخزن",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.AddBusiness,
                            contentDescription = null,
                            tint = primaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // ── Scrollable Body ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Product Name Input
                        OutlinedTextField(
                            value = productName,
                            onValueChange = { productName = it },
                            placeholder = {
                                Text(
                                    text = "اسم المنتج أو الصنف *",
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = primaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            textStyle = TextStyle(
                                textAlign = TextAlign.End,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = modalBgColor,
                                unfocusedContainerColor = modalBgColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_purchase_product_name_input")
                        )

                        // 1.5 Product Image Picker Section (Camera + Gallery)
                        ProductImagePickerSection(
                            imagePath = productImagePath,
                            productNameForPreview = productName,
                            onImagePathChanged = { productImagePath = it }
                        )

                        // 2. Card: 📋 طريقة تسجيل وحساب المنتج:
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = softBlueCardBg,
                            border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "طريقة تسجيل وحساب المنتج: 📋",
                                    color = primaryBlue,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Left: بالقطعة الفردية 🏷️
                                    Surface(
                                        onClick = {
                                            isCartonMode = false
                                            if (pricingMode == "CARTON_PRICE") {
                                                pricingMode = "UNIT_PRICE"
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (!isCartonMode) selectedOptionBlueBg else cardWhiteColor,
                                        border = BorderStroke(
                                            1.dp,
                                            if (!isCartonMode) primaryBlue else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("mode_single_unit_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "بالقطعة الفردية 🏷️",
                                                fontSize = 12.5.sp,
                                                fontWeight = if (!isCartonMode) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.LocalOffer,
                                                contentDescription = null,
                                                tint = primaryBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Right: بالعبوة / طرد 📦
                                    Surface(
                                        onClick = { isCartonMode = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isCartonMode) selectedOptionBlueBg else cardWhiteColor,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCartonMode) primaryBlue else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("mode_carton_pack_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "بالعبوة / طرد 📦",
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isCartonMode) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = if (isDark) Color.White else Color(0xFF0C4A6E),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Card: 📦 بيانات تكلفة العبوة (شراء بالجملة):
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cardWhiteColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (isCartonMode) "بيانات تكلفة العبوة (شراء بالجملة): 📦" else "بيانات تكلفة شراء القطعة الفردية: 🏷️",
                                    color = primaryBlue,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (isCartonMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Left: عدد الوحدات 📦 *
                                        OutlinedTextField(
                                            value = unitsPerCartonInput,
                                            onValueChange = { unitsPerCartonInput = it },
                                            label = {
                                                Text(
                                                    text = "* 📦 عدد الوحدات",
                                                    fontSize = 11.sp
                                                )
                                            },
                                            textStyle = TextStyle(
                                                textAlign = TextAlign.End,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = modalBgColor,
                                                unfocusedContainerColor = modalBgColor,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .weight(0.95f)
                                                .testTag("new_product_units_per_carton_input")
                                        )

                                        // Right: تكلفة العبوة (د.ج) (اختياري)
                                        OutlinedTextField(
                                            value = cartonCostInput,
                                            onValueChange = { cartonCostInput = it },
                                            placeholder = {
                                                Text(
                                                    text = "تكلفة العبوة\n($currencySymbol)\n(اختياري)",
                                                    fontSize = 13.5.sp,
                                                    textAlign = TextAlign.End,
                                                    lineHeight = 19.sp,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ShoppingCart,
                                                    contentDescription = null,
                                                    tint = primaryBlue,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            textStyle = TextStyle(
                                                textAlign = TextAlign.End,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            minLines = 3,
                                            maxLines = 3,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = modalBgColor,
                                                unfocusedContainerColor = modalBgColor,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .testTag("new_product_carton_cost_input")
                                        )
                                    }

                                    // Automatic Division Formula Box
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = softBlueCardBg,
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
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
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = primaryBlue
                                                ) {
                                                    Text(
                                                        text = "حساب تلقائي",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.5.sp,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "معادلة تقسيم تكلفة العبوة:",
                                                        color = primaryBlue,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.5.sp
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Calculate,
                                                        contentDescription = null,
                                                        tint = primaryBlue,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", cartonCostVal)} $currencySymbol (عبوة) ÷ $effectiveUnitsPerCarton قطعة = ${String.format(Locale.US, "%.2f", unitCostVal)} $currencySymbol (تكلفة القطعة)",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.5.sp,
                                                color = if (isDark) Color.White else Color(0xFF0C4A6E),
                                                textAlign = TextAlign.End,
                                                lineHeight = 20.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = singleUnitCostInput,
                                        onValueChange = { singleUnitCostInput = it },
                                        placeholder = {
                                            Text(
                                                text = "تكلفة شراء القطعة الواحدة ($currencySymbol) (اختياري)",
                                                fontSize = 13.5.sp,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = primaryBlue,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        textStyle = TextStyle(
                                            textAlign = TextAlign.End,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = modalBgColor,
                                            unfocusedContainerColor = modalBgColor,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("new_product_single_unit_cost_input")
                                    )
                                }
                            }
                        }

                        // 4. Card: 🎯 تحديد سعر البيع وهامش الربح عبر:
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cardWhiteColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "تحديد سعر البيع وهامش الربح عبر: 🎯",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Row 1: بيع القطعة 🏷️ (Left) | بيع العبوة 📦 (Right)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val isUnitSell = pricingMode == "UNIT_PRICE"
                                    Surface(
                                        onClick = { pricingMode = "UNIT_PRICE" },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isUnitSell) selectedOptionBlueBg else modalBgColor,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isUnitSell) primaryBlue else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("pricing_mode_unit_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "بيع القطعة 🏷️",
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isUnitSell) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    val isCartonSell = pricingMode == "CARTON_PRICE"
                                    Surface(
                                        onClick = { pricingMode = "CARTON_PRICE" },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isCartonSell) selectedOptionBlueBg else modalBgColor,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCartonSell) primaryBlue else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("pricing_mode_carton_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "بيع العبوة 📦",
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isCartonSell) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                // Row 2: مبلغ ربح ثابت 💰 (Left) | نسبة ربح % 📈 (Right)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val isFixedProfit = pricingMode == "FIXED_PROFIT"
                                    Surface(
                                        onClick = { pricingMode = "FIXED_PROFIT" },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isFixedProfit) selectedOptionBlueBg else modalBgColor,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isFixedProfit) primaryBlue else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("pricing_mode_fixed_profit_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "مبلغ ربح ثابت 💰",
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isFixedProfit) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    val isPercentProfit = pricingMode == "PROFIT_PERCENT"
                                    Surface(
                                        onClick = { pricingMode = "PROFIT_PERCENT" },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isPercentProfit) selectedOptionBlueBg else modalBgColor,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isPercentProfit) primaryBlue else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("pricing_mode_percent_profit_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "نسبة ربح % 📈",
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isPercentProfit) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Dynamic Selling Price Input Field + Auto Unit Price Pill + Summary Card
                        val dynamicPricePlaceholder = when (pricingMode) {
                            "CARTON_PRICE" -> "سعر بيع العبوة بالكامل ($currencySymbol) *"
                            "UNIT_PRICE" -> "سعر بيع القطعة الواحدة ($currencySymbol) *"
                            "PROFIT_PERCENT" -> "نسبة الربح المطلوبة (%) *"
                            else -> "مبلغ الربح الثابت للقطعة ($currencySymbol) *"
                        }
                        OutlinedTextField(
                            value = pricingValueInput,
                            onValueChange = { pricingValueInput = it },
                            placeholder = {
                                Text(
                                    text = dynamicPricePlaceholder,
                                    fontSize = 14.5.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = emeraldGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            textStyle = TextStyle(
                                textAlign = TextAlign.End,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = modalBgColor,
                                unfocusedContainerColor = modalBgColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_product_selling_price_input")
                        )

                        // Auto Unit Price Hint Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardWhiteColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "سعر بيع القطعة التلقائي = ${String.format(Locale.US, "%.2f", finalUnitSellingPrice)} $currencySymbol (لكل قطعة) 💡",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }

                        // Final Unit Selling Price & Net Profit Summary Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cardWhiteColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: صافي ربح القطعة
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "صافي ربح القطعة:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val sign = if (unitNetProfit >= 0.0) "+" else ""
                                    Text(
                                        text = "$sign${String.format(Locale.US, "%.2f", unitNetProfit)} $currencySymbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.5.sp,
                                        color = if (unitNetProfit >= 0.0) emeraldGreen else Color(0xFFDC2626)
                                    )
                                }

                                // Right: سعر البيع النهائي (قطعة)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "سعر البيع النهائي (قطعة):",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", finalUnitSellingPrice)} $currencySymbol",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // 6. Card: 🛒 كمية الشراء بالفاتورة (بالعبوات):
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cardWhiteColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (isCartonMode) quantityCardTitleCarton else quantityCardTitleUnit,
                                    color = primaryBlue,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = purchasedQtyInput,
                                    onValueChange = { purchasedQtyInput = it },
                                    label = {
                                        Text(
                                            text = if (isCartonMode) "عدد العبوات / الكراتين المشتراة *" else "عدد القطع المشتراة *",
                                            fontSize = 11.5.sp
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = primaryBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        textAlign = TextAlign.End,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = modalBgColor,
                                        unfocusedContainerColor = modalBgColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_product_purchased_qty_input")
                                )

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = modalBgColor,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isCartonMode) {
                                                "إجمالي المخزون المضاف: $purchasedCount كرتونة × $effectiveUnitsPerCarton قطعة = $totalAddedUnits قطعة 📦"
                                            } else {
                                                "إجمالي المخزون المضاف: $totalAddedUnits قطعة 📦"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = "إجمالي تكلفة البند بالفاتورة: ${String.format(Locale.US, "%.2f", totalItemInvoiceCost)} $currencySymbol 💰",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = primaryBlue,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }

                        // 7. Card: 🏷️ أكواد الباركود للمنتج:
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cardWhiteColor,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "أكواد الباركود للمنتج (يدعم عدة باركودات للسلعة): 🏷️",
                                    color = primaryBlue,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Unit Barcode Field
                                OutlinedTextField(
                                    value = unitBarcode,
                                    onValueChange = { unitBarcode = it },
                                    placeholder = {
                                        Text(
                                            text = "🏷️ باركود القطعة (يمكن إدخال عدة أكواد مفصولة بفاصلة ,)",
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.End,
                                            lineHeight = 19.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    leadingIcon = {
                                        IconButton(
                                            onClick = {
                                                unitBarcode = Product.mergeBarcodeStrings(unitBarcode, generateRandomBarcode("613"))
                                            },
                                            modifier = Modifier.testTag("generate_unit_barcode_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Autorenew,
                                                contentDescription = "توليد وإضافة باركود تلقائي",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { activeScannerTarget = "UNIT" },
                                            modifier = Modifier.testTag("scan_unit_barcode_camera_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "مسح وإضافة باركود القطعة بالكاميرا",
                                                tint = primaryBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    textStyle = TextStyle(
                                        textAlign = TextAlign.End,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    minLines = 2,
                                    maxLines = 2,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = modalBgColor,
                                        unfocusedContainerColor = modalBgColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_product_unit_barcode_input")
                                )

                                MultiBarcodeChipsAndQuickAdder(
                                    rawBarcodes = unitBarcode,
                                    onBarcodesChanged = { unitBarcode = it },
                                    addPlaceholder = "إضافة باركود إضافي للقطعة...",
                                    onScanAdditional = { activeScannerTarget = "UNIT" },
                                    onGenerateAdditional = {
                                        unitBarcode = Product.mergeBarcodeStrings(unitBarcode, generateRandomBarcode("613"))
                                    },
                                    tagPrefix = "new_prod_unit_multi_bc"
                                )

                                // Quick Swap Button between Unit Barcode and Carton Barcode when a barcode is entered/scanned
                                if (unitBarcode.isNotBlank() || cartonBarcode.isNotBlank()) {
                                    Surface(
                                        onClick = {
                                            val temp = unitBarcode
                                            unitBarcode = cartonBarcode
                                            cartonBarcode = temp
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = softBlueCardBg,
                                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .testTag("swap_unit_and_carton_barcode_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "تبديل الكود بين باركود القطعة 🏷️ وباركود الكرتونة 📦",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = primaryBlue
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.SwapVert,
                                                contentDescription = "تبديل الباركود",
                                                tint = primaryBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Carton Barcode Field
                                OutlinedTextField(
                                    value = cartonBarcode,
                                    onValueChange = { cartonBarcode = it },
                                    placeholder = {
                                        Text(
                                            text = "📦 باركود العبوة الكبرى / الكرتونة (يدعم عدة أكواد)",
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.End,
                                            lineHeight = 19.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    leadingIcon = {
                                        IconButton(
                                            onClick = {
                                                cartonBarcode = Product.mergeBarcodeStrings(cartonBarcode, generateRandomBarcode("619"))
                                            },
                                            modifier = Modifier.testTag("generate_carton_barcode_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Autorenew,
                                                contentDescription = "توليد باركود العبوة تلقائياً",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { activeScannerTarget = "CARTON" },
                                            modifier = Modifier.testTag("scan_carton_barcode_camera_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "مسح باركود العبوة بالكاميرا",
                                                tint = primaryBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    textStyle = TextStyle(
                                        textAlign = TextAlign.End,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    minLines = 2,
                                    maxLines = 2,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = modalBgColor,
                                        unfocusedContainerColor = modalBgColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_product_carton_barcode_input")
                                )

                                MultiBarcodeChipsAndQuickAdder(
                                    rawBarcodes = cartonBarcode,
                                    onBarcodesChanged = { cartonBarcode = it },
                                    addPlaceholder = "إضافة باركود إضافي للكرتونة...",
                                    onScanAdditional = { activeScannerTarget = "CARTON" },
                                    onGenerateAdditional = {
                                        cartonBarcode = Product.mergeBarcodeStrings(cartonBarcode, generateRandomBarcode("619"))
                                    },
                                    tagPrefix = "new_prod_carton_multi_bc"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Bottom Footer Buttons (Left: Confirm | Right: إلغاء) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Confirm Button
                        Surface(
                            onClick = {
                                if (canAddItem) {
                                    onAddProductToInvoice(
                                        PurchaseInvoiceItem(
                                            productId = null,
                                            productName = productName.trim(),
                                            barcode = unitBarcode.trim(),
                                            cartonBarcode = cartonBarcode.trim(),
                                            imagePath = productImagePath.trim(),
                                            quantity = purchasedCount,
                                            unitCost = if (isCartonMode) cartonCostVal else unitCostVal,
                                            sellingPrice = finalUnitSellingPrice,
                                            subtotal = totalItemInvoiceCost,
                                            isCarton = isCartonMode,
                                            unitsPerCarton = effectiveUnitsPerCarton,
                                            isNewProduct = true
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (canAddItem) primaryBlue else if (isDark) Color(0xFF334155) else Color(0xFFD5DCE4),
                            modifier = Modifier
                                .weight(1.9f)
                                .height(50.dp)
                                .testTag("confirm_new_purchase_product_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = confirmButtonText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.5.sp,
                                    color = if (canAddItem) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF7B8794)
                                )
                            }
                        }

                        // Right: إلغاء (pill button)
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = modalBgColor,
                                contentColor = primaryBlue
                            ),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(50.dp)
                                .testTag("cancel_new_purchase_product_btn")
                        ) {
                            Text(
                                text = "إلغاء",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = primaryBlue
                            )
                        }
                    }
                }
            }
        }
    }

    // Live Camera Barcode Scanner Modal when user taps the camera icon
    activeScannerTarget?.let { target ->
        SimpleBarcodeCaptureDialog(
            title = if (target == "UNIT") "مسح وإضافة باركود القطعة" else "مسح وإضافة باركود العبوة / الكرتونة",
            onBarcodeScanned = { scannedCode ->
                if (target == "UNIT") {
                    unitBarcode = Product.mergeBarcodeStrings(unitBarcode, scannedCode)
                } else {
                    cartonBarcode = Product.mergeBarcodeStrings(cartonBarcode, scannedCode)
                }
                activeScannerTarget = null
            },
            onDismiss = { activeScannerTarget = null }
        )
    }
}

@Composable
fun UnregisteredBarcodeResolutionCard(
    scannedBarcode: String,
    products: List<Product>,
    currencySymbol: String,
    onLinkToExistingProduct: (product: Product, asCartonBarcode: Boolean) -> Unit,
    onOpenCreateNewProduct: () -> Unit,
    onResetScan: () -> Unit,
    onQuickCreateAndAddToCart: ((
        name: String,
        unitPrice: Double,
        purchasePrice: Double,
        asCartonBarcode: Boolean,
        unitsPerCarton: Int,
        quantityToAddInCart: Int
    ) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAFStoreSemanticColors.current
    val isDark = semantic.isDark
    val primaryBlue = Color(0xFF0077C2)
    val emeraldGreen = semantic.profitGreen
    val amberAccent = Color(0xFFF59E0B)
    val cardBg = if (isDark) semantic.cardElevated else Color(0xFFFFFBEB)
    val innerSurfaceBg = if (semantic.isOledBlack) Color(0xFF050811) else if (isDark) Color(0xFF0F172A) else Color.White

    var showLinkExistingSection by remember(scannedBarcode) { mutableStateOf(false) }
    var linkAsCartonBarcode by remember(scannedBarcode) { mutableStateOf(false) }
    var searchProductQuery by remember(scannedBarcode) { mutableStateOf("") }

    // Fast Inline Product Definition state
    var quickName by remember(scannedBarcode) { mutableStateOf("") }
    var quickSellingPrice by remember(scannedBarcode) { mutableStateOf("") }
    var quickCostPrice by remember(scannedBarcode) { mutableStateOf("") }
    var quickIsCartonBarcode by remember(scannedBarcode) { mutableStateOf(false) }
    var quickUnitsPerCarton by remember(scannedBarcode) { mutableStateOf("12") }
    var quickCartQty by remember(scannedBarcode) { mutableIntStateOf(1) }
    var quickFormError by remember(scannedBarcode) { mutableStateOf(false) }

    val filteredProducts = remember(products, searchProductQuery) {
        if (searchProductQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchProductQuery, ignoreCase = true) ||
                it.barcode.contains(searchProductQuery, ignoreCase = true) ||
                it.cartonBarcode.contains(searchProductQuery, ignoreCase = true)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("unregistered_barcode_resolution_card"),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.8.dp, amberAccent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Warning & Scanned Barcode Header
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
                        shape = RoundedCornerShape(12.dp),
                        color = amberAccent.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "باركود جديد غير مسجل — تعريف سريع",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "الكود الممسوح:",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = amberAccent.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = scannedBarcode,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onResetScan,
                    modifier = Modifier.testTag("unregistered_barcode_rescan_btn")
                ) {
                    Text(
                        text = "إلغاء / مسح آخر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Fast Inline Definition & Add to Invoice Card (if callback provided)
            if (onQuickCreateAndAddToCart != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = innerSurfaceBg,
                    border = BorderStroke(1.4.dp, emeraldGreen.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fast_define_unregistered_product_section")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = emeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تعريف سريع للصنف وإضافته للفاتورة فوراً:",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = emeraldGreen
                            )
                        }

                        OutlinedTextField(
                            value = quickName,
                            onValueChange = {
                                quickName = it
                                if (it.isNotBlank()) quickFormError = false
                            },
                            label = { Text("اسم الصنف الجديد *") },
                            placeholder = { Text("مثال: عصير برتقال 1 لتر") },
                            isError = quickFormError && quickName.isBlank(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_unregistered_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickSellingPrice,
                                onValueChange = {
                                    quickSellingPrice = it
                                    if ((it.toDoubleOrNull() ?: 0.0) > 0.0) quickFormError = false
                                },
                                label = { Text("سعر البيع للقطعة ($currencySymbol) *") },
                                placeholder = { Text("0.00") },
                                isError = quickFormError && (quickSellingPrice.toDoubleOrNull() ?: 0.0) <= 0.0,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("quick_unregistered_price_input")
                            )

                            OutlinedTextField(
                                value = quickCostPrice,
                                onValueChange = { quickCostPrice = it },
                                label = { Text("التكلفة (اختياري)") },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("quick_unregistered_cost_input")
                            )
                        }

                        // Package Mode & Quantity for Immediate Invoice Addition
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { quickIsCartonBarcode = false },
                                shape = RoundedCornerShape(10.dp),
                                color = if (!quickIsCartonBarcode) emeraldGreen.copy(alpha = 0.16f) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (!quickIsCartonBarcode) emeraldGreen else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("quick_unregistered_unit_mode_chip")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🏷️ باركود قطعة",
                                        fontWeight = if (!quickIsCartonBarcode) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        color = if (!quickIsCartonBarcode) emeraldGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                onClick = { quickIsCartonBarcode = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (quickIsCartonBarcode) amberAccent.copy(alpha = 0.18f) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (quickIsCartonBarcode) amberAccent else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("quick_unregistered_carton_mode_chip")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "📦 باركود كرتونة",
                                        fontWeight = if (quickIsCartonBarcode) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        color = if (quickIsCartonBarcode) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (quickIsCartonBarcode) {
                                OutlinedTextField(
                                    value = quickUnitsPerCarton,
                                    onValueChange = { quickUnitsPerCarton = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("حبات الكرتونة") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .width(120.dp)
                                        .testTag("quick_unregistered_carton_units_input")
                                )
                            } else {
                                Text(
                                    text = "الكمية المراد إضافتها للفاتورة:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = { if (quickCartQty > 1) quickCartQty-- },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    text = "$quickCartQty ${if (quickIsCartonBarcode) "كرتونة" else "قطعة"}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Surface(
                                    onClick = { quickCartQty++ },
                                    shape = RoundedCornerShape(8.dp),
                                    color = emeraldGreen.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "زيادة",
                                            tint = emeraldGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (quickFormError) {
                            Text(
                                text = "يرجى إدخال اسم الصنف وسعر البيع للإضافة الفورية للفاتورة",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val parsedPrice = quickSellingPrice.toDoubleOrNull() ?: 0.0
                                if (quickName.isBlank() || parsedPrice <= 0.0) {
                                    quickFormError = true
                                } else {
                                    val parsedCost = (quickCostPrice.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                                    val parsedPackUnits = if (quickIsCartonBarcode) {
                                        (quickUnitsPerCarton.toIntOrNull() ?: 12).coerceAtLeast(1)
                                    } else {
                                        1
                                    }
                                    onQuickCreateAndAddToCart(
                                        quickName.trim(),
                                        parsedPrice,
                                        parsedCost,
                                        quickIsCartonBarcode,
                                        parsedPackUnits,
                                        quickCartQty
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("quick_define_and_add_to_cart_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حفظ الصنف وإضافته للفاتورة فوراً",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = "أو اختر ربط الباركود بصنف موجود أو فتح نافذة التعريف التفصيلي:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Two Secondary Action Buttons:
            // 1) ربط بسلعة موجودة
            // 2) نافذة التعريف الشامل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Option 1: Link to existing product
                Button(
                    onClick = { showLinkExistingSection = !showLinkExistingSection },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showLinkExistingSection) Color(0xFF0369A1) else primaryBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("unregistered_barcode_link_existing_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "ربط بسلعة موجودة",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Option 2: Create new comprehensive product
                OutlinedButton(
                    onClick = onOpenCreateNewProduct,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, primaryBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("unregistered_barcode_create_new_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = primaryBlue,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "تعريف تفصيلي شامل",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = primaryBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Expandable Section when user chooses "ربط بسلعة موجودة"
            if (showLinkExistingSection) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = innerSurfaceBg,
                    border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("link_existing_product_picker_section")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "1. حدد نوع الباركود المراد ربطه بالسلعة:",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = primaryBlue
                        )

                        // Barcode Type Toggle (Unit Barcode vs Carton Barcode)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { linkAsCartonBarcode = false },
                                shape = RoundedCornerShape(12.dp),
                                color = if (!linkAsCartonBarcode) primaryBlue.copy(alpha = 0.15f) else Color.Transparent,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (!linkAsCartonBarcode) primaryBlue else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("link_barcode_type_unit_chip")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "🏷️ باركود القطعة الفردية",
                                        fontWeight = if (!linkAsCartonBarcode) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = if (!linkAsCartonBarcode) primaryBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                onClick = { linkAsCartonBarcode = true },
                                shape = RoundedCornerShape(12.dp),
                                color = if (linkAsCartonBarcode) amberAccent.copy(alpha = 0.18f) else Color.Transparent,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (linkAsCartonBarcode) amberAccent else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("link_barcode_type_carton_chip")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "📦 باركود الكرتونة / العبوة",
                                        fontWeight = if (linkAsCartonBarcode) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = if (linkAsCartonBarcode) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Text(
                            text = "2. اختر السلعة الموجودة لربط الباركود ($scannedBarcode) وإضافتها للفاتورة:",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = primaryBlue
                        )

                        OutlinedTextField(
                            value = searchProductQuery,
                            onValueChange = { searchProductQuery = it },
                            placeholder = {
                                Text(
                                    text = "ابحث باسم السلعة في المخزن...",
                                    fontSize = 12.5.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "بحث",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchProductQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchProductQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "مسح",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_barcode_search_product_input")
                        )

                        if (filteredProducts.isEmpty()) {
                            Text(
                                text = "لا توجد سلع مطابقة للبحث. يمكنك تعريف الصنف الجديد بالأعلى.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filteredProducts.take(15).forEach { prod ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
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
                                                    text = prod.name,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "السعر: ${String.format(Locale.US, "%.2f", prod.price)} $currencySymbol • المخزون: ${prod.stock} ق",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                val currentBarcodeInfo = if (linkAsCartonBarcode) {
                                                    if (prod.cartonBarcodesList.isEmpty()) "باركودات الكرتونة: غير محددة"
                                                    else "باركودات الكرتونة (${prod.cartonBarcodesList.size}): ${prod.cartonBarcodesList.joinToString(" ، ")}"
                                                } else {
                                                    if (prod.unitBarcodesList.isEmpty()) "باركودات القطعة: غير محددة"
                                                    else "باركودات القطعة (${prod.unitBarcodesList.size}): ${prod.unitBarcodesList.joinToString(" ، ")}"
                                                }
                                                Text(
                                                    text = currentBarcodeInfo,
                                                    fontSize = 10.5.sp,
                                                    color = primaryBlue,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = {
                                                    onLinkToExistingProduct(prod, linkAsCartonBarcode)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = primaryBlue,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .testTag("confirm_link_barcode_product_${prod.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "+ إضافة باركود",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.5.sp
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

@Composable
fun UnregisteredBarcodeActionDialog(
    scannedBarcode: String,
    products: List<Product>,
    currencySymbol: String,
    onLinkToExistingProduct: (product: Product, asCartonBarcode: Boolean) -> Unit,
    onCreateNewProduct: (PurchaseInvoiceItem) -> Unit,
    onQuickCreateAndAddToCart: ((
        name: String,
        unitPrice: Double,
        purchasePrice: Double,
        asCartonBarcode: Boolean,
        unitsPerCarton: Int,
        quantityToAddInCart: Int
    ) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showNewComprehensiveProductModal by remember(scannedBarcode) { mutableStateOf(false) }

    if (showNewComprehensiveProductModal) {
        NewPurchaseProductModal(
            currencySymbol = currencySymbol,
            initialUnitBarcode = scannedBarcode,
            confirmButtonText = "حفظ السلعة وإضافتها للفاتورة ✓",
            quantityCardTitleCarton = "الكمية المتوفرة بالمخزن (بالعبوات): 📦",
            quantityCardTitleUnit = "الكمية المتوفرة بالمخزن (بالقطعة): 📦",
            onDismiss = {
                showNewComprehensiveProductModal = false
            },
            onAddProductToInvoice = { newItem ->
                showNewComprehensiveProductModal = false
                onCreateNewProduct(newItem)
            }
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 720.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UnregisteredBarcodeResolutionCard(
                        scannedBarcode = scannedBarcode,
                        products = products,
                        currencySymbol = currencySymbol,
                        onLinkToExistingProduct = onLinkToExistingProduct,
                        onOpenCreateNewProduct = {
                            showNewComprehensiveProductModal = true
                        },
                        onResetScan = onDismiss,
                        onQuickCreateAndAddToCart = onQuickCreateAndAddToCart
                    )
                }
            }
        }
    }
}

@Composable
fun MultiBarcodeChipsAndQuickAdder(
    rawBarcodes: String,
    onBarcodesChanged: (String) -> Unit,
    addPlaceholder: String = "إضافة باركود آخر...",
    onScanAdditional: (() -> Unit)? = null,
    onGenerateAdditional: (() -> Unit)? = null,
    tagPrefix: String = "multi_bc"
) {
    val codes = remember(rawBarcodes) { Product.parseBarcodes(rawBarcodes) }
    var quickAddText by remember { mutableStateOf("") }
    val primaryBlue = Color(0xFF1976D2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (codes.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الأكواد المسجلة (${codes.size}):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (codes.size > 1) {
                            Text(
                                text = "متعدد الباركود ✓",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    codes.forEachIndexed { index, code ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (index == 0) primaryBlue.copy(alpha = 0.14f) else Color(0xFF10B981).copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = if (index == 0) "#1 أساسي" else "#${index + 1} إضافي",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (index == 0) primaryBlue else Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = code,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val remaining = codes.filterIndexed { i, _ -> i != index }
                                        onBarcodesChanged(Product.joinBarcodes(remaining))
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("${tagPrefix}_remove_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف الباركود $code",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Row to quickly append an additional barcode
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = quickAddText,
                onValueChange = { quickAddText = it },
                placeholder = {
                    Text(
                        text = addPlaceholder,
                        fontSize = 11.5.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_quick_input")
            )

            Button(
                onClick = {
                    val clean = quickAddText.trim()
                    if (clean.isNotEmpty()) {
                        onBarcodesChanged(Product.mergeBarcodeStrings(rawBarcodes, clean))
                        quickAddText = ""
                    } else if (onGenerateAdditional != null) {
                        onGenerateAdditional()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("${tagPrefix}_add_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة باركود",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "إضافة",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (onScanAdditional != null) {
                OutlinedButton(
                    onClick = onScanAdditional,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("${tagPrefix}_scan_add_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "مسح باركود إضافي بالكاميرا",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

