package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.utils.ApkTempLinkGenerator
import com.example.utils.GeneratedTempApkLink
import com.example.utils.LocalApkMetadata
import com.example.utils.TempApkHostProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ApkTempLinkActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAFStoreSemanticColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = semantic.profitGreen.copy(alpha = if (semantic.isDark) 0.14f else 0.16f),
        border = BorderStroke(1.2.dp, semantic.profitGreen.copy(alpha = if (semantic.isDark) 0.55f else 0.80f)),
        modifier = modifier
            .padding(end = 6.dp)
            .testTag("top_bar_apk_temp_link_btn")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = "إنشاء رابط تحميل APK خارجي مؤقت",
                tint = semantic.profitGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "APK Temp",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = semantic.profitGreen,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ApkDownloadModal(
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var apkMetadata by remember { mutableStateOf<LocalApkMetadata?>(null) }
    var isExtractingApk by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgressText by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(TempApkHostProvider.AUTO_FAST) }

    var savedLinks by remember { mutableStateOf<List<GeneratedTempApkLink>>(emptyList()) }
    var activeLink by remember { mutableStateOf<GeneratedTempApkLink?>(null) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }
    var isErrorFeedback by remember { mutableStateOf(false) }
    var copiedUrlRecently by remember { mutableStateOf(false) }

    val qrBitmap = remember(activeLink?.directDownloadUrl) {
        val url = activeLink?.directDownloadUrl
        if (!url.isNullOrBlank()) {
            ApkTempLinkGenerator.generateQrCodeBitmap(url, sizePx = 520)
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        isExtractingApk = true
        val existingLinks = ApkTempLinkGenerator.loadSavedLinksHistory(context)
        savedLinks = existingLinks
        if (existingLinks.isNotEmpty()) {
            activeLink = existingLinks.first()
        }
        val extracted = ApkTempLinkGenerator.extractInstalledApk(context).getOrNull()
        apkMetadata = extracted
        isExtractingApk = false
    }

    LaunchedEffect(copiedUrlRecently) {
        if (copiedUrlRecently) {
            delay(2500)
            copiedUrlRecently = false
        }
    }

    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { targetUri ->
        val meta = apkMetadata
        if (targetUri != null && meta != null) {
            scope.launch {
                val ok = ApkTempLinkGenerator.saveApkFileToDestinationUri(
                    context = context,
                    sourceApkPath = meta.apkFilePath,
                    destinationUri = targetUri
                )
                if (ok) {
                    val msg = "تم حفظ ملف ${meta.fileName} (${meta.formattedSize}) على الجهاز بنجاح"
                    feedbackBanner = msg
                    isErrorFeedback = false
                    onShowMessage(msg)
                } else {
                    feedbackBanner = "تعذر حفظ ملف APK على المسار المحدد"
                    isErrorFeedback = true
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.91f)
                .testTag("apk_temp_download_modal"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 18.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            val semantic = LocalAFStoreSemanticColors.current
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = semantic.headerContainer,
                    contentColor = semantic.onHeaderContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = semantic.profitGreen.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, semantic.profitGreen),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = null,
                                        tint = semantic.profitGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "ملف تحميل التطبيق APK برابط خارجي Temp",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = semantic.onHeaderContainer
                                )
                                Text(
                                    text = "استخراج حزمة APK وتوليد رابط تحميل خارجي مؤقت + رمز QR للمشاركة الفورية",
                                    fontSize = 11.sp,
                                    color = semantic.onHeaderSubtitle
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_apk_modal_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = semantic.onHeaderContainer
                            )
                        }
                    }
                }

                // Body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Feedback Banner
                    if (!feedbackBanner.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isErrorFeedback) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    Color(0xFF10B981).copy(alpha = 0.15f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isErrorFeedback) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isErrorFeedback) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isErrorFeedback) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                                    )
                                    Text(
                                        text = feedbackBanner.orEmpty(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isErrorFeedback) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 1. Extracted Local APK File Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "ملف APK الجاهز للتثبيت والرفع",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    if (apkMetadata != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = apkMetadata!!.formattedSize,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                if (isExtractingApk) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Text("جاري تجهيز واستخراج حزمة APK الحالية...", fontSize = 12.sp)
                                    }
                                } else if (apkMetadata != null) {
                                    val meta = apkMetadata!!
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = meta.fileName,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "الإصدار: v${meta.versionName} (${meta.versionCode}) • الحجم: ${meta.formattedSize} • البصمة: ${meta.sha256Short}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                saveApkLauncher.launch(meta.fileName)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("save_apk_to_device_btn"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SaveAlt,
                                                contentDescription = null,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("حفظ ملف APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                ApkTempLinkGenerator.shareRawApkFile(context, meta)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("share_apk_file_direct_btn"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إرسال ملف APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. External Temp Link Upload Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "توليد رابط خارجي مؤقت (Temp APK Link)",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "اختر مزود الاستضافة المؤقتة لرفع ملف APK والحصول على رابط خارجي مباشر",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Host provider chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TempApkHostProvider.entries.forEach { provider ->
                                        val isSelected = selectedProvider == provider
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { if (!isUploading) selectedProvider = provider },
                                            label = {
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(
                                                        text = provider.titleAr,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = provider.expiryBadgeAr,
                                                        fontSize = 9.sp,
                                                        color = if (isSelected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            ),
                                            modifier = Modifier.testTag("apk_provider_chip_${provider.id}")
                                        )
                                    }
                                }

                                Text(
                                    text = selectedProvider.subtitleAr,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                AnimatedVisibility(visible = isUploading) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        Text(
                                            text = uploadProgressText.ifBlank { "جاري رفع ملف APK وتوليد الرابط الخارجي المؤقت..." },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (isUploading) return@Button
                                        scope.launch {
                                            isUploading = true
                                            feedbackBanner = null
                                            uploadProgressText = "جاري تجهيز ملف APK للرفع الخارجي..."
                                            val result = ApkTempLinkGenerator.uploadApkAndCreateTempLink(
                                                context = context,
                                                provider = selectedProvider,
                                                onStepProgress = { status ->
                                                    uploadProgressText = status
                                                }
                                            )
                                            isUploading = false
                                            result.onSuccess { generated ->
                                                activeLink = generated
                                                savedLinks = ApkTempLinkGenerator.saveLinkToHistory(context, generated)
                                                clipboardManager.setText(AnnotatedString(generated.directDownloadUrl))
                                                copiedUrlRecently = true
                                                val msg = "تم إنشاء رابط APK الخارجي المؤقت ونسخه للحافظة بنجاح!"
                                                feedbackBanner = msg
                                                isErrorFeedback = false
                                                onShowMessage(msg)
                                            }.onFailure { err ->
                                                feedbackBanner = err.message ?: "تعذر رفع ملف APK للخادم الخارجي"
                                                isErrorFeedback = true
                                            }
                                        }
                                    },
                                    enabled = !isUploading,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("generate_temp_apk_link_btn")
                                ) {
                                    if (isUploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.2.dp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جاري رفع APK وإنشاء الرابط المؤقت...", fontWeight = FontWeight.ExtraBold)
                                    } else {
                                        Icon(imageVector = Icons.Default.Link, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "إنشاء رابط تحميل APK خارجي مؤقت الآن (Temp)",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Active Generated Temp Link + QR Code Card
                    if (activeLink != null) {
                        item {
                            val link = activeLink!!
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF10B981))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF10B981).copy(alpha = 0.18f),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = null,
                                                        tint = Color(0xFF10B981),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = "رابط التحميل الخارجي المباشر جاهز",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF10B981)
                                                )
                                                Text(
                                                    text = "${link.providerNameAr} • ${link.expiresNoteAr}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = link.fileSizeFormatted,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Direct URL Box
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Direct Temp APK URL:",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            SelectionContainer {
                                                Text(
                                                    text = link.directDownloadUrl,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF38BDF8),
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("generated_temp_apk_url_text")
                                                )
                                            }
                                        }
                                    }

                                    // Action buttons for URL
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(link.directDownloadUrl))
                                                copiedUrlRecently = true
                                                feedbackBanner = "تم نسخ رابط التحميل المؤقت إلى الحافظة"
                                                isErrorFeedback = false
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (copiedUrlRecently) Color(0xFF059669) else MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("copy_temp_apk_url_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (copiedUrlRecently) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (copiedUrlRecently) "تم النسخ ✓" else "نسخ الرابط",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                ApkTempLinkGenerator.shareTempLinkText(context, link)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("share_temp_apk_url_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("مشاركة الرابط", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                ApkTempLinkGenerator.openUrlInBrowser(context, link.directDownloadUrl)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("open_temp_apk_url_browser_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInBrowser,
                                                contentDescription = "فتح الرابط في المتصفح",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Scannable QR Code for immediate mobile download
                                    if (qrBitmap != null) {
                                        HorizontalDivider()
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "امسح رمز QR بكاميرا أي هاتف لتحميل ملف APK مباشرة",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White,
                                            border = BorderStroke(2.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                            shadowElevation = 4.dp,
                                            modifier = Modifier.size(190.dp)
                                        ) {
                                            Image(
                                                bitmap = qrBitmap.asImageBitmap(),
                                                contentDescription = "QR Code لتحميل ملف APK",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp)
                                            )
                                        }

                                        Text(
                                            text = "الملف: ${link.fileName} • تم الإنشاء: ${link.formattedCreatedAt}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Saved Links History
                    if (savedLinks.isNotEmpty()) {
                        item {
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
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "سجل روابط APK المؤقتة (${savedLinks.size})",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        ApkTempLinkGenerator.clearLinksHistory(context)
                                        savedLinks = emptyList()
                                        activeLink = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مسح السجل", fontSize = 11.sp)
                                }
                            }
                        }

                        items(savedLinks, key = { it.id }) { item ->
                            val isSelected = activeLink?.id == item.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { activeLink = item }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.providerNameAr,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = item.expiresNoteAr,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = item.directDownloadUrl,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.formattedCreatedAt} • ${item.fileSizeFormatted}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(item.directDownloadUrl))
                                                copiedUrlRecently = true
                                                feedbackBanner = "تم نسخ الرابط: ${item.directDownloadUrl}"
                                                isErrorFeedback = false
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "نسخ الرابط",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                ApkTempLinkGenerator.openUrlInBrowser(context, item.directDownloadUrl)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInBrowser,
                                                contentDescription = "فتح الرابط",
                                                modifier = Modifier.size(18.dp)
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
