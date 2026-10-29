package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import android.content.ClipData
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 1. Report PDF and Header components in com.example.ui.screens
enum class SectionPdfReportType {
    SALES, INVENTORY, DEBTS, CUSTOMERS, LOYALTY, LIVE_DASHBOARD, DASHBOARD, REORDER_ALERTS
}

data class UniversalPdfReportPayload(
    val title: String = "",
    val type: SectionPdfReportType = SectionPdfReportType.SALES,
    val reportType: SectionPdfReportType = type,
    val data: List<Any> = emptyList(),
    val periodTitle: String = "",
    val storeName: String = "",
    val storePhone: String = "",
    val currencySymbol: String = "",
    val items: List<Any> = data,
    val desktopConfig: Any? = null,
    val kpiMetrics: Any? = null,
    val tableHeaders: Any? = null,
    val tableRows: Any? = null,
    val notesOrSummary: String = ""
)

object UniversalPdfReportExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun writeReportPdfToStream(payload: UniversalPdfReportPayload, outputStream: OutputStream) {
        val cfg = payload.desktopConfig as? com.example.utils.DesktopPrinterConfig
        val isA5 = cfg?.defaultPaperSize == "A5"
        val isLandscape = cfg?.orientation == "LANDSCAPE"
        val isInkSaving = cfg?.inkSavingMode == true

        val (pageWidth, pageHeight) = when {
            isA5 && isLandscape -> Pair(595, 420)
            isA5 && !isLandscape -> Pair(420, 595)
            !isA5 && isLandscape -> Pair(842, 595)
            else -> Pair(595, 842)
        }

        val margin = when (cfg?.marginType) {
            "NARROW" -> 18f
            "WIDE" -> 50f
            else -> 36f
        }

        val pdfDocument = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.BLACK
                textSize = 10f
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isInkSaving) AndroidColor.BLACK else AndroidColor.rgb(15, 23, 42)
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isInkSaving) AndroidColor.DKGRAY else AndroidColor.rgb(100, 116, 139)
                textSize = 10f
            }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isInkSaving) AndroidColor.BLACK else AndroidColor.rgb(2, 132, 199)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            var y = 46f

            // Store Header & Logo badge
            if (cfg == null || cfg.printStoreLogo) {
                val logoRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isInkSaving) AndroidColor.LTGRAY else AndroidColor.rgb(241, 245, 249)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(margin, y - 10f, margin + 44f, y + 26f, logoRectPaint)
                canvas.drawText("AF", margin + 14f, y + 14f, titlePaint.apply { textSize = 14f })
                titlePaint.textSize = 16f
            }

            canvas.drawText(payload.title.ifBlank { "تقرير شامل" }, if (cfg?.printStoreLogo != false) margin + 54f else margin, y, titlePaint)
            y += 20f

            if (payload.storeName.isNotBlank()) {
                canvas.drawText("المتجر: ${payload.storeName} | هاتف: ${payload.storePhone}", if (cfg?.printStoreLogo != false) margin + 54f else margin, y, subPaint)
                y += 16f
            }
            canvas.drawText("تاريخ الإصدار: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())}", margin, y, subPaint)
            y += 20f

            canvas.drawLine(margin, y, pageWidth - margin, y, paint.apply { color = AndroidColor.GRAY; strokeWidth = 1f })
            y += 18f

            if (payload.notesOrSummary.isNotBlank()) {
                canvas.drawText("ملاحظات: ${payload.notesOrSummary}", margin, y, subPaint)
                y += 18f
            }

            canvas.drawText("إجمالي العناصر / البيانات المدرجة: ${payload.items.size}", margin, y, headerPaint)
            y += 22f

            var index = 1
            val maxItems = if (isA5) 20 else 35
            for (item in payload.items.take(maxItems)) {
                if (y > pageHeight - 75f) break
                val itemText = "$index. ${item.toString()}"
                canvas.drawText(itemText, margin, y, paint.apply { color = AndroidColor.DKGRAY; textSize = 9.5f })
                y += 17f
                index++
            }

            // Custom Footer Note & Verification Badge
            val footerY = pageHeight - 32f
            canvas.drawLine(margin, footerY - 14f, pageWidth - margin, footerY - 14f, paint.apply { color = AndroidColor.LTGRAY; strokeWidth = 0.8f })

            val footerMsg = cfg?.customFooterText?.ifBlank { "شكراً لتعاملكم معنا" } ?: "شكراً لتعاملكم معنا"
            canvas.drawText(footerMsg, margin, footerY, subPaint.apply { textSize = 8.5f })

            if (cfg == null || cfg.printQrCode) {
                canvas.drawText("[رمز تحقق QR]", pageWidth - margin - 85f, footerY, subPaint.apply { textSize = 8.5f })
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    fun generateCachePdfFile(context: Context, payload: UniversalPdfReportPayload): File {
        val dir = File(context.cacheDir, "universal_reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { fos ->
            writeReportPdfToStream(payload, fos)
        }
        return file
    }

    fun saveReportPdfToUri(context: Context, payload: UniversalPdfReportPayload, uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                writeReportPdfToStream(payload, out)
            } != null
        } catch (_: Exception) {
            false
        }
    }

    fun shareReportPdf(context: Context, payload: UniversalPdfReportPayload) {
        try {
            val file = generateCachePdfFile(context, payload)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, payload.title)
                putExtra(Intent.EXTRA_TEXT, "مرفق تقرير: ${payload.title} - متجر ${payload.storeName}")
                clipData = ClipData.newRawUri(payload.title, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة التقرير PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة ملف PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun printReport(context: Context, payload: UniversalPdfReportPayload, desktopConfig: Any? = null) {
        try {
            val activity = findActivity(context)
            val targetContext = activity ?: context
            val printManager = targetContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (activity == null || printManager == null) {
                Toast.makeText(context, "الطباعة المباشرة غير متوفرة — جاري فتح ومشاركة ملف PDF بدلاً من ذلك 📄", Toast.LENGTH_SHORT).show()
                shareReportPdf(context, payload)
                return
            }
            val cfg = desktopConfig as? com.example.utils.DesktopPrinterConfig
            val printAttributesBuilder = PrintAttributes.Builder()
            if (cfg != null) {
                val baseMediaSize = if (cfg.defaultPaperSize == "A5") {
                    PrintAttributes.MediaSize.ISO_A5
                } else {
                    PrintAttributes.MediaSize.ISO_A4
                }
                val finalMediaSize = if (cfg.orientation == "LANDSCAPE") {
                    baseMediaSize.asLandscape()
                } else {
                    baseMediaSize.asPortrait()
                }
                printAttributesBuilder.setMediaSize(finalMediaSize)
                if (cfg.inkSavingMode) {
                    printAttributesBuilder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                } else {
                    printAttributesBuilder.setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                }
            }
            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?) {
                    val info = PrintDocumentInfo.Builder("${payload.title.ifBlank { "Document" }}.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback.onLayoutFinished(info, true)
                }
                override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
                    try {
                        FileOutputStream(destination.fileDescriptor).use { out ->
                            writeReportPdfToStream(payload, out)
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.localizedMessage)
                    }
                }
            }
            printManager.print(payload.title, adapter, printAttributesBuilder.build())
        } catch (e: Exception) {
            Toast.makeText(context, "جاري فتح ومشاركة ملف التقرير PDF بدلاً من الطباعة المباشرة 📄", Toast.LENGTH_SHORT).show()
            shareReportPdf(context, payload)
        }
    }

    fun exportReport(context: Context, payload: UniversalPdfReportPayload) {
        shareReportPdf(context, payload)
    }

    fun exportReport(context: Context, payload: Any) {
        if (payload is UniversalPdfReportPayload) {
            exportReport(context, payload)
        }
    }

    fun buildTestDesktopReportPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        desktopConfig: Any? = null
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير تجريبي لسطح المكتب",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        desktopConfig = desktopConfig
    )

    fun buildInvoicesPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        invoices: List<Any> = emptyList()
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير فواتير المبيعات",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = invoices,
        data = invoices
    )

    fun buildPurchasesPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        purchases: List<Any> = emptyList()
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير فواتير المشتريات",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = purchases,
        data = purchases
    )

    fun buildCustomersPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        customers: List<Any> = emptyList()
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير بيانات العملاء",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = customers,
        data = customers
    )

    fun buildLoyaltyPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        topCustomers: List<Any> = emptyList(),
        totalPoints: Any = 0
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير برنامج الولاء والنقاط",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = topCustomers,
        data = topCustomers
    )

    fun buildInventoryPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        products: List<Any> = emptyList(),
        totalValuation: Double = 0.0
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير جرد المخزون والمنتجات",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = products,
        data = products
    )

    fun buildInventoryAuditLogPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        logs: List<Any> = emptyList()
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "سجل تدقيق وتغييرات المخزون",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = logs,
        data = logs
    )

    fun buildReorderAlertsPayload(
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        alerts: List<Any> = emptyList(),
        lowStockProducts: List<Any> = alerts,
        customMinStock: Any? = null,
        defaultThreshold: Int = 5
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير تنبيهات نواقص المنتجات وإعادة الطلب",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol,
        items = lowStockProducts,
        data = lowStockProducts
    )

    fun buildLiveDashboardPayload(
        period: Any? = null,
        chartDisplayMode: Any? = null,
        metricComparisonMode: Any? = null,
        overview: Any? = null,
        totalPurchasesCost: Any? = null,
        buckets: Any? = null,
        topSellingProducts: Any? = null,
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = ""
    ): UniversalPdfReportPayload = UniversalPdfReportPayload(
        title = "تقرير لوحة التحكم الحية",
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol
    )
}

@Composable
fun UniversalSectionPdfReportDialog(
    payload: UniversalPdfReportPayload?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && payload != null) {
            isWorking = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    UniversalPdfReportExporter.saveReportPdfToUri(context, payload, uri)
                }
                isWorking = false
                Toast.makeText(
                    context,
                    if (ok) "تم حفظ ملف PDF بنجاح 📄" else "تعذر حفظ الملف",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    if (payload != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .wrapContentSize(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.925f)
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = payload.title.ifBlank { "تقرير PDF" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "عدد العناصر: ${payload.items.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "جاهز للتصدير والمشاركة والطباعة الفورية.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (isWorking) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                UniversalPdfReportExporter.shareReportPdf(context, payload)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة ملف PDF (واتساب / إيميل)")
                        }

                        OutlinedButton(
                            onClick = {
                                UniversalPdfReportExporter.printReport(context, payload)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("طباعة التقرير")
                        }

                        OutlinedButton(
                            onClick = {
                                val defaultName = "Report_${System.currentTimeMillis()}.pdf"
                                saveLauncher.launch(defaultName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ إلى الجهاز")
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("إغلاق")
                    }
                }
            }
        }
      }
    }
  }
}

@Composable
fun UniversalReportHeaderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "تقرير PDF",
    tag: String = "universal_report_btn"
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF0284C7).copy(alpha = 0.12f),
            contentColor = Color(0xFF0284C7)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier.testTag(tag)
    ) {
        Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = title,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 12.sp)
    }
}

// 2. Barcode & HUD Components in com.example.ui.screens
@Composable
fun TurboCameraBarcodeHud(
    isScanningActive: Boolean = true,
    smartTorchMode: com.example.utils.SmartTorchMode = com.example.utils.SmartTorchMode.OFF,
    onToggleTorchMode: () -> Unit = {},
    isSoundEnabled: Boolean = true,
    onToggleSound: () -> Unit = {},
    isVibrationEnabled: Boolean = true,
    onToggleVibration: () -> Unit = {},
    isAutoZoomEnabled: Boolean = false,
    onToggleAutoZoom: () -> Unit = {},
    hasActiveSelection: Boolean = false,
    lastDecodeBadge: String? = null,
    onResetScanClick: () -> Unit = {},
    onBarcodeScanned: (com.example.utils.ScannedBarcodeResult) -> Unit = {},
    compactMode: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeClient = remember { BarcodeScanning.getClient() }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }

    // Handle Torch Mode changes
    LaunchedEffect(smartTorchMode, cameraControl) {
        cameraControl?.let { ctrl ->
            val enableTorch = when (smartTorchMode) {
                com.example.utils.SmartTorchMode.ALWAYS_ON -> true
                com.example.utils.SmartTorchMode.OFF -> false
                com.example.utils.SmartTorchMode.AUTO_SMART -> false // or ambient sensor logic
            }
            try {
                ctrl.enableTorch(enableTorch)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("turbo_camera_barcode_hud")
    ) {
        // CameraX Preview View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
            },
            update = { previewView ->
                if (!isScanningActive) return@AndroidView
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && isScanningActive) {
                                        val startTime = System.currentTimeMillis()
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        barcodeClient.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                val barcode = barcodes.firstOrNull()
                                                if (barcode != null) {
                                                    val rawValue = barcode.rawValue ?: barcode.displayValue ?: ""
                                                    if (rawValue.isNotBlank()) {
                                                        val decodeDuration = System.currentTimeMillis() - startTime
                                                        val formatName = when (barcode.format) {
                                                            Barcode.FORMAT_EAN_13 -> "EAN-13"
                                                            Barcode.FORMAT_EAN_8 -> "EAN-8"
                                                            Barcode.FORMAT_QR_CODE -> "QR-Code"
                                                            Barcode.FORMAT_CODE_128 -> "Code-128"
                                                            Barcode.FORMAT_CODE_39 -> "Code-39"
                                                            Barcode.FORMAT_UPC_A -> "UPC-A"
                                                            Barcode.FORMAT_UPC_E -> "UPC-E"
                                                            else -> "BARCODE"
                                                        }
                                                        onBarcodeScanned(
                                                            com.example.utils.ScannedBarcodeResult(
                                                                code = rawValue.trim(),
                                                                formatLabel = formatName,
                                                                decodeTimeMs = decodeDuration
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            .addOnFailureListener {
                                                // ignore frame parse failures silently for high performance
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                        cameraControl = camera.cameraControl
                        cameraInfo = camera.cameraInfo
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Overlay HUD & Controls (Elevated above system bars)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Torch, Sound, Vibration, Auto-Zoom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Torch Toggle
                IconButton(
                    onClick = onToggleTorchMode,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("scanner_torch_btn")
                ) {
                    Icon(
                        imageVector = when (smartTorchMode) {
                            com.example.utils.SmartTorchMode.ALWAYS_ON -> Icons.Default.FlashOn
                            com.example.utils.SmartTorchMode.AUTO_SMART -> Icons.Default.AutoMode
                            com.example.utils.SmartTorchMode.OFF -> Icons.Default.FlashOff
                        },
                        contentDescription = "الفلاش",
                        tint = if (smartTorchMode != com.example.utils.SmartTorchMode.OFF) Color(0xFFF59E0B) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Last Decode Badge
                if (!lastDecodeBadge.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = lastDecodeBadge,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Sound & Vibration Toggles
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "الصوت",
                            tint = if (isSoundEnabled) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleVibration,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isVibrationEnabled) Icons.Default.Vibration else Icons.Default.PhoneDisabled,
                            contentDescription = "الاهتزاز",
                            tint = if (isVibrationEnabled) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Center Scanning Laser / Target Reticle
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(if (compactMode) 90.dp else 125.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 3.dp.toPx()
                    val cornerLength = 24.dp.toPx()
                    val color = if (hasActiveSelection) Color(0xFF10B981) else Color(0xFF3B82F6)

                    // Top-Left
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(cornerLength, 0f), strokeWidth)
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, cornerLength), strokeWidth)
                    // Top-Right
                    drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width - cornerLength, 0f), strokeWidth)
                    drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(size.width, cornerLength), strokeWidth)
                    // Bottom-Left
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(cornerLength, size.height), strokeWidth)
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(0f, size.height - cornerLength), strokeWidth)
                    // Bottom-Right
                    drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width - cornerLength, size.height), strokeWidth)
                    drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height - cornerLength), strokeWidth)
                }

                if (!isScanningActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "تم التقاط الباركود ✓",
                            color = Color(0xFF10B981),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom Bar: Reset Scan Button (Raised up cleanly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasActiveSelection || !isScanningActive) {
                    Button(
                        onClick = onResetScanClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("scanner_reset_scan_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح باركود جديد", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "وجّه العدسة نحو باركود المنتج للمسح الفوري ⚡",
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}


// 3. Purchase Invoices, Receipts & Pharmacy Vignette Scanner (ML Kit Text Recognition)
data class OcrExtractedItem(
    val title: String = "",
    val price: Double = 0.0,
    val quantity: Double = 1.0
)

data class VignetteScanResult(
    val productName: String = "",
    val price: Double = 0.0,
    val barcode: String = "",
    val rawText: String = "",
    val supplierName: String = "",
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val detectedItems: List<OcrExtractedItem> = emptyList()
)

object InvoiceReceiptOcrParser {
    fun parseInvoiceText(rawText: String): VignetteScanResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return VignetteScanResult(rawText = rawText)

        var detectedTotal = 0.0
        var detectedSupplier = ""
        var detectedInvoiceNo = ""
        var detectedDate = ""
        var detectedBarcode = ""
        val detectedItems = mutableListOf<OcrExtractedItem>()

        val totalKeywords = listOf(
            "total ttc", "total", "net à payer", "net a payer", "montant total",
            "المجموع", "الإجمالي", "الاجمالي", "المبلغ الإجمالي", "المبلغ الكلي", "مجموع الفاتورة",
            "montant", "somme", "a payer", "reste a payer"
        )
        val dateRegex = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}[/-]\d{1,2}[/-]\d{1,2})\b""")
        val invoiceNoRegex = Regex("""(?i)(?:Facture|Invoice|BL|BC|N°|No\.|رقم|فاتورة|وصل)\s*[:#\-]?\s*([A-Za-z0-9\-_/]+)""")
        val barcodeRegex = Regex("""\b(\d{8}|\d{12}|\d{13}|\d{14})\b""")

        for (line in lines) {
            val dateMatch = dateRegex.find(line)
            if (dateMatch != null && detectedDate.isBlank()) {
                detectedDate = dateMatch.value
            }
            val invMatch = invoiceNoRegex.find(line)
            if (invMatch != null && detectedInvoiceNo.isBlank() && invMatch.groupValues[1].length >= 2) {
                detectedInvoiceNo = invMatch.groupValues[1]
            }
            val barMatch = barcodeRegex.find(line)
            if (barMatch != null && detectedBarcode.isBlank()) {
                detectedBarcode = barMatch.value
            }
        }

        // Supplier name is usually one of the top non-date, non-number lines
        for (i in 0 until minOf(4, lines.size)) {
            val line = lines[i]
            if (!line.matches(Regex(""".*\b\d{4}\b.*""")) &&
                !line.contains("facture", ignoreCase = true) &&
                !line.contains("total", ignoreCase = true) &&
                !line.contains("فاتورة", ignoreCase = true) &&
                line.length >= 3
            ) {
                detectedSupplier = line
                break
            }
        }

        // Search for Total keyword
        var foundExplicitTotal = false
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            val lowerLine = line.lowercase()
            for (keyword in totalKeywords) {
                if (lowerLine.contains(keyword)) {
                    val parsedAmount = extractPriceFromLine(line)
                        ?: (if (i + 1 < lines.size) extractPriceFromLine(lines[i + 1]) else null)
                    if (parsedAmount != null && parsedAmount > 0.0) {
                        detectedTotal = parsedAmount
                        foundExplicitTotal = true
                        break
                    }
                }
            }
            if (foundExplicitTotal) break
        }

        // Fallback: Plausible monetary amount
        if (detectedTotal == 0.0) {
            val allNumbers = mutableListOf<Double>()
            for (line in lines) {
                val p = extractPriceFromLine(line)
                if (p != null && p > 0.0 && p < 10000000.0) {
                    allNumbers.add(p)
                }
            }
            if (allNumbers.isNotEmpty()) {
                detectedTotal = allNumbers.maxOrNull() ?: 0.0
            }
        }

        // Extract items (lines with both text and numeric amount)
        for (line in lines) {
            val p = extractPriceFromLine(line)
            if (p != null && p > 0.0) {
                val cleanTitle = line.replace(Regex("""[0-9]+[.,]?[0-9]*"""), "")
                    .replace("DA", "", ignoreCase = true)
                    .replace("DZD", "", ignoreCase = true)
                    .replace("د.ج", "")
                    .replace("دج", "")
                    .trim()
                if (cleanTitle.length >= 3 &&
                    !cleanTitle.contains("total", ignoreCase = true) &&
                    !cleanTitle.contains("مجموع", ignoreCase = true) &&
                    !cleanTitle.contains("net", ignoreCase = true)
                ) {
                    detectedItems.add(OcrExtractedItem(title = cleanTitle, price = p, quantity = 1.0))
                }
            }
        }

        val primaryProductName = if (detectedItems.isNotEmpty()) {
            detectedItems.first().title
        } else {
            detectedSupplier.ifBlank { "سلعة مستخرجة عبر الـ OCR" }
        }

        return VignetteScanResult(
            productName = primaryProductName,
            price = detectedTotal,
            barcode = detectedBarcode,
            rawText = rawText,
            supplierName = detectedSupplier,
            invoiceNumber = detectedInvoiceNo,
            invoiceDate = detectedDate,
            detectedItems = detectedItems
        )
    }

    private fun extractPriceFromLine(line: String): Double? {
        val matches = Regex("""(\d{1,3}(?:[ ,]\d{3})*(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?|\d+(?:,\d{1,2})?)""").findAll(line)
        for (m in matches) {
            val cleanStr = m.value.replace(" ", "").replace(",", ".")
            val d = cleanStr.toDoubleOrNull()
            if (d != null && d > 0.0) {
                return d
            }
        }
        return null
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun VignetteScannerDialog(
    onVignetteScanned: (VignetteScanResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isAnalyzing by remember { mutableStateOf(true) }
    var lastScannedText by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<VignetteScanResult?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }

    // User editable values on detected result
    var editedProductName by remember { mutableStateOf("") }
    var editedPrice by remember { mutableStateOf("") }
    var editedBarcode by remember { mutableStateOf("") }
    var editedSupplier by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                isProcessingImage = true
                val inputImage = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val result = InvoiceReceiptOcrParser.parseInvoiceText(visionText.text)
                        lastScannedText = visionText.text
                        parsedResult = result
                        editedProductName = result.productName
                        editedPrice = if (result.price > 0.0) result.price.toString() else ""
                        editedBarcode = result.barcode
                        editedSupplier = result.supplierName
                        isAnalyzing = false
                    }
                    .addOnFailureListener {
                        isProcessingImage = false
                    }
                    .addOnCompleteListener {
                        isProcessingImage = false
                    }
            } catch (e: Exception) {
                isProcessingImage = false
            }
        }
    }

    fun triggerHapticFeedback() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .testTag("ocr_invoice_scanner_dialog"),
            color = Color(0xFF0B0F19)
        ) {
            if (!hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "إذن الكاميرا مطلوب",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "يحتاج ماسح الـ OCR إلى إذن الكاميرا لقراءة وتدقيق فواتير الشراء والوصولات الورقية مباشرة في المتجر.",
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.fillMaxWidth().testTag("grant_camera_permission_btn")
                            ) {
                                Text("منح صلاحية الكاميرا ✓", color = Color.White)
                            }
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إلغاء", color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Camera Live Preview
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val executor = Executors.newSingleThreadExecutor()
                            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                        if (!isAnalyzing) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            recognizer.process(image)
                                                .addOnSuccessListener { visionText ->
                                                    val raw = visionText.text
                                                    if (raw.isNotBlank() && raw.length > 5) {
                                                        lastScannedText = raw
                                                        val parsed = InvoiceReceiptOcrParser.parseInvoiceText(raw)
                                                        if (parsed.price > 0.0 || parsed.productName.isNotBlank()) {
                                                            parsedResult = parsed
                                                            if (editedPrice.isBlank() && parsed.price > 0.0) {
                                                                editedPrice = parsed.price.toString()
                                                            }
                                                            if (editedProductName.isBlank() && parsed.productName.isNotBlank()) {
                                                                editedProductName = parsed.productName
                                                            }
                                                            if (editedBarcode.isBlank() && parsed.barcode.isNotBlank()) {
                                                                editedBarcode = parsed.barcode
                                                            }
                                                            if (editedSupplier.isBlank() && parsed.supplierName.isNotBlank()) {
                                                                editedSupplier = parsed.supplierName
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        context as androidx.lifecycle.LifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                    cameraControl = cam.cameraControl
                                } catch (e: Exception) {
                                    android.util.Log.e("OcrCamera", "Binding error: ${e.message}")
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. HUD Framing Guides for Receipts & Invoices
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            border = BorderStroke(2.dp, if (parsedResult != null) Color(0xFF10B981) else Color(0xFF38BDF8).copy(alpha = 0.8f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = if (parsedResult != null) "تم رصد بيانات الفاتورة بنجاح ✓" else "ضع فاتورة الشراء أو الوصل الورقي داخل الإطار",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 3. Top Toolbar (Elevated with status bar insets)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("ocr_scanner_close_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flashlight / Torch toggle
                            IconButton(
                                onClick = {
                                    isTorchOn = !isTorchOn
                                    cameraControl?.enableTorch(isTorchOn)
                                },
                                modifier = Modifier.background(
                                    if (isTorchOn) Color(0xFFFBBF24) else Color.Black.copy(alpha = 0.6f),
                                    CircleShape
                                ).testTag("ocr_torch_toggle_btn")
                            ) {
                                Icon(
                                    if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "الفلاش",
                                    tint = if (isTorchOn) Color.Black else Color.White
                                )
                            }

                            // Pick from gallery button
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .testTag("ocr_gallery_pick_btn")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "معرض الصور", tint = Color.White)
                            }
                        }
                    }

                    // 4. Bottom Controls & Detected Results Sheet (Elevated above system navigation)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Color(0xFF0F172A).copy(alpha = 0.95f),
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ماسح فواتير الشراء والوصولات (OCR)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAnalyzing) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, if (isAnalyzing) Color(0xFF0284C7) else Color(0xFF10B981))
                            ) {
                                Text(
                                    text = if (isAnalyzing) "مسح حي 🔴" else "تم التثبيت ⏸",
                                    color = if (isAnalyzing) Color(0xFF38BDF8) else Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Extracted Details Form (Editable before saving)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editedProductName,
                                onValueChange = { editedProductName = it },
                                label = { Text("اسم السلعة / البيان", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("ocr_product_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                maxLines = 1
                            )

                            OutlinedTextField(
                                value = editedPrice,
                                onValueChange = { editedPrice = it },
                                label = { Text("السعر / الإجمالي (د.ج)", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("ocr_price_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF10B981),
                                    unfocusedTextColor = Color(0xFF34D399),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                maxLines = 1
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editedSupplier,
                                onValueChange = { editedSupplier = it },
                                label = { Text("المورد / اسم المحل", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("ocr_supplier_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                maxLines = 1
                            )

                            OutlinedTextField(
                                value = editedBarcode,
                                onValueChange = { editedBarcode = it },
                                label = { Text("الباركود إن وجد", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("ocr_barcode_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                maxLines = 1
                            )
                        }

                        // Action Buttons (Raised up cleanly from bottom edge)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    triggerHapticFeedback()
                                    val finalPrice = editedPrice.toDoubleOrNull() ?: parsedResult?.price ?: 0.0
                                    val finalResult = VignetteScanResult(
                                        productName = editedProductName.ifBlank { parsedResult?.productName ?: "منتج مستخرج عبر OCR" },
                                        price = finalPrice,
                                        barcode = editedBarcode.ifBlank { parsedResult?.barcode ?: "" },
                                        rawText = lastScannedText,
                                        supplierName = editedSupplier.ifBlank { parsedResult?.supplierName ?: "" },
                                        invoiceNumber = parsedResult?.invoiceNumber ?: "",
                                        invoiceDate = parsedResult?.invoiceDate ?: "",
                                        detectedItems = parsedResult?.detectedItems ?: emptyList()
                                    )
                                    onVignetteScanned(finalResult)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("ocr_confirm_save_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تأكيد واستيراد ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    isAnalyzing = !isAnalyzing
                                    if (isAnalyzing) {
                                        parsedResult = null
                                        editedProductName = ""
                                        editedPrice = ""
                                        editedBarcode = ""
                                        editedSupplier = ""
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(52.dp)
                                    .testTag("ocr_toggle_freeze_btn")
                            ) {
                                Icon(
                                    if (isAnalyzing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isAnalyzing) "تثبيت" else "استئناف", fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// 4. POS Scanner Docks
@Composable
fun PosInlineLiveScannerDock(
    viewModel: Any? = null,
    isScanningAllowed: Boolean = true,
    cooldownResetTrigger: Any? = null,
    onOpenFullscreenScanner: () -> Unit = {},
    onCollapseDock: () -> Unit = {}
) {}

@Composable
fun PosLastScannedItemQuickBar(
    lastEvent: Any? = null,
    cartItem: Any? = null,
    currencySymbol: String = "",
    onIncrementUnit: () -> Unit = {},
    onDecrementUnit: () -> Unit = {},
    onTogglePackageMode: () -> Unit = {},
    onToggleCartonPiece: () -> Unit = onTogglePackageMode,
    onAddFullCarton: () -> Unit = {},
    onRepeatScanNow: () -> Unit = {},
    onRemoveFromCart: () -> Unit = {},
    onDismissStrip: () -> Unit = {},
    modifier: Modifier = Modifier
) {}

// 5. Bluetooth and Printer UI Cards in com.example.ui.screens
@Composable
fun BluetoothPrinterPickerDialog(
    currentSelectedAddress: String = "",
    connectionState: Any? = null,
    hasPermission: Boolean = true,
    requiredPermissions: Array<String> = emptyArray(),
    loadPairedDevices: () -> List<com.example.utils.BluetoothPrinterDeviceInfo> = { emptyList() },
    onSelectAndConnectPrinter: (com.example.utils.BluetoothPrinterDeviceInfo) -> Unit = {},
    onDisconnect: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf(emptyList<com.example.utils.BluetoothPrinterDeviceInfo>()) }
    var isScanning by remember { mutableStateOf(false) }

    val bluetoothAdapter = remember {
        try {
            android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        } catch (e: Exception) {
            null
        }
    }
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    val refreshDevices = {
        isScanning = true
        isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
        devices = try {
            loadPairedDevices()
        } catch (e: Exception) {
            emptyList()
        }
        isScanning = false
    }

    LaunchedEffect(Unit) {
        refreshDevices()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.925f)
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إعدادات وطابعات البلوتوث الحرارية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اختر طابعة POS أو ابحث عن الأجهزة القريبة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Bluetooth Power Control Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isBluetoothEnabled) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                                contentDescription = null,
                                tint = if (isBluetoothEnabled) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = if (isBluetoothEnabled) "حالة البلوتوث: مفعل ✓" else "حالة البلوتوث: متوقف",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "قم بتشغيل البلوتوث واتصال الطابعة من إعدادات النظام إن لزم",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isBluetoothEnabled,
                            onCheckedChange = { enabled ->
                                isBluetoothEnabled = enabled
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "يرجى تشغيل البلوتوث من إعدادات الجهاز", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                // Action Bar: Scan & Refresh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { refreshDevices() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("جاري البحث...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("بحث وتحديث الأجهزة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لا يمكن فتح إعدادات البلوتوث", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إعدادات النظام", fontSize = 11.5.sp)
                    }
                }

                // Devices List
                Text(
                    text = "الطابعات والأجهزة المكتشفة (${devices.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                Icons.Default.PrintDisabled,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "لم يتم العثور على طابعات بلوتوث مقترنة.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "تأكد من اقتران الطابعة الحرارية عبر إعدادات بلوتوث الهاتف أولاً.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices) { device ->
                            val isSelected = device.address.equals(currentSelectedAddress, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectAndConnectPrinter(device)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = device.name.ifBlank { "طابعة حرارية غير معروفة" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = device.address,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onSelectAndConnectPrinter(device) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                        ),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(
                                            text = if (isSelected) "متصلة ✓" else "اتصال",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
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
                        Text("إغلاق النافذة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothPrinterPickerDialog(
    currentSelectedAddress: String = "",
    connectionState: Any? = null,
    hasPermission: Boolean = true,
    requiredPermissions: List<String>,
    loadPairedDevices: () -> List<com.example.utils.BluetoothPrinterDeviceInfo> = { emptyList() },
    onSelectAndConnectPrinter: (com.example.utils.BluetoothPrinterDeviceInfo) -> Unit = {},
    onDisconnect: () -> Unit = {},
    onDismiss: () -> Unit = {}
) = BluetoothPrinterPickerDialog(
    currentSelectedAddress = currentSelectedAddress,
    connectionState = connectionState,
    hasPermission = hasPermission,
    requiredPermissions = requiredPermissions.toTypedArray(),
    loadPairedDevices = loadPairedDevices,
    onSelectAndConnectPrinter = onSelectAndConnectPrinter,
    onDisconnect = onDisconnect,
    onDismiss = onDismiss
)

@Composable
fun ThermalPrinterSettingsCard(
    config: com.example.utils.ThermalPrinterConfig,
    connectionState: Any? = null,
    isPrinting: Boolean = false,
    invoiceStyle: com.example.data.models.InvoiceStyle = com.example.data.models.InvoiceStyle.BON_DE_LIVRAISON,
    onSelectInvoiceStyle: (com.example.data.models.InvoiceStyle) -> Unit = {},
    onUpdateConfig: (com.example.utils.ThermalPrinterConfig) -> Unit = {},
    onOpenBluetoothPicker: () -> Unit = {},
    onReconnectSavedPrinter: () -> Unit = {},
    onDisconnectPrinter: () -> Unit = {},
    onDisconnect: () -> Unit = onDisconnectPrinter,
    onPrintTestReceipt: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isConnected = connectionState is com.example.utils.ThermalConnectionState.Connected
    val isConnecting = connectionState is com.example.utils.ThermalConnectionState.Connecting

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.2.dp,
            if (isConnected) Color(0xFF10B981).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("thermal_printer_settings_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title, Bluetooth Status, Expand/Collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isConnected) Color(0xFF10B981).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (isConnected) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "إعدادات الطابعة الحرارية (Bluetooth ESC/POS)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    isConnected -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    isConnecting -> Color(0xFF0284C7).copy(alpha = 0.15f)
                                    else -> Color.Gray.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        isConnected -> "متصلة ✓"
                                        isConnecting -> "جاري الاتصال..."
                                        else -> "غير متصلة"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isConnected -> Color(0xFF059669)
                                        isConnecting -> Color(0xFF0284C7)
                                        else -> Color.Gray
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (config.lastPrinterName.isNotBlank()) "الطابعة: ${config.lastPrinterName} (${config.paperWidthMm}mm)" else "طابعات الفواتير المحمولة ومقاس 80mm / 58mm",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "طي" else "توسيع"
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Connection management & Search
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenBluetoothPicker,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("thermal_pick_printer_btn")
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isConnected) "تغيير الطابعة" else "🔍 بحث والاتصال بالطابعة", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        if (isConnected) {
                            OutlinedButton(
                                onClick = onDisconnectPrinter,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("قطع الاتصال", color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                        } else if (config.lastPrinterAddress.isNotBlank()) {
                            OutlinedButton(
                                onClick = onReconnectSavedPrinter,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("إعادة اتصال", fontSize = 12.sp)
                            }
                        }
                    }
                    if (!isConnected) {
                        Text(
                            text = "💡 انقر على زر البحث والاتصال أعلاه للعثور على طابعات البلوتوث الحرارية القريبة والاقتران بها مباشرة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Default Invoice Template Selector ( قوالب الطباعة الحرارية )
                Text(
                    text = "قالب طباعة الفاتورة الافتراضي",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val availableStyles = listOf(
                        com.example.data.models.InvoiceStyle.BON_DE_LIVRAISON to "📋 Bon de livraison",
                        com.example.data.models.InvoiceStyle.THERMAL_DETAILED to "🧾 وصل التسليم",
                        com.example.data.models.InvoiceStyle.THERMAL_POS to "🖨️ حراري POS",
                        com.example.data.models.InvoiceStyle.SIMPLE to "📄 Bon de commande",
                        com.example.data.models.InvoiceStyle.TEMPLATE_K to "✨ قالب K"
                    )
                    availableStyles.forEach { (style, label) ->
                        FilterChip(
                            selected = invoiceStyle == style,
                            onClick = { onSelectInvoiceStyle(style) },
                            label = { Text(label, fontSize = 11.5.sp) }
                        )
                    }
                }

                // Paper width selector (58mm, 80mm, 104mm)
                Text(
                    text = "عرض الورق الحراري",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val widths = listOf(
                        80 to "80 مم (قياسي)",
                        58 to "58 مم (محمول)",
                        104 to "104 مم (عريض)"
                    )
                    widths.forEach { (wMm, label) ->
                        val isSelected = config.paperWidthMm == wMm
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val pWidth = com.example.utils.ThermalPaperWidth.fromMm(wMm)
                                    onUpdateConfig(config.copy(paperWidthMm = wMm, paperWidth = pWidth))
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Default copies count selector
                Text(
                    text = "عدد النسخ الافتراضية للطباعة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3).forEach { copies ->
                        val isSelected = config.copyCount == copies
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onUpdateConfig(config.copy(copyCount = copies, defaultCopies = copies))
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$copies نسخة",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Font thickness selector
                Text(
                    text = "سمك وعرض خط الفاتورة الحرارية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val thicknesses = listOf(
                        com.example.utils.ThermalFontThickness.NORMAL to "عادي",
                        com.example.utils.ThermalFontThickness.MEDIUM to "متوسط",
                        com.example.utils.ThermalFontThickness.EXTRA_BOLD to "عريض داكن"
                    )
                    thicknesses.forEach { (thick, label) ->
                        val isSelected = config.fontThickness == thick
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onUpdateConfig(config.copy(fontThickness = thick))
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Professional Toggles
                PrinterSettingCheckRow(
                    title = "طباعة وصل حراري تلقائياً عند الدفع",
                    subtitle = "إرسال الفاتورة للطابعة الحرارية فور الضغط على تأكيد البيع",
                    checked = config.autoPrintOnCheckout,
                    onCheckedChange = { onUpdateConfig(config.copy(autoPrintOnCheckout = it)) }
                )

                PrinterSettingCheckRow(
                    title = "طباعة الشعار البنكي / شعار المحجر أو المتجر",
                    subtitle = "تضمين لوجو أو طابع المؤسسة في رأس الفاتورة",
                    checked = config.printBankLogo,
                    onCheckedChange = { onUpdateConfig(config.copy(printBankLogo = it, printStoreLogo = it)) }
                )

                PrinterSettingCheckRow(
                    title = "طباعة باركود الفاتورة",
                    subtitle = "تضمين باركود رقم الفاتورة السريع في أسفل الوصل",
                    checked = config.printBarcode,
                    onCheckedChange = { onUpdateConfig(config.copy(printBarcode = it)) }
                )

                PrinterSettingCheckRow(
                    title = "طباعة QR Code الفاتورة",
                    subtitle = "رمز الاستجابة السريع للتحقق الرقمي من الفاتورة",
                    checked = config.printQrCode,
                    onCheckedChange = { onUpdateConfig(config.copy(printQrCode = it)) }
                )

                PrinterSettingCheckRow(
                    title = "فتح درج النقود تلقائياً (Cash Drawer Kick)",
                    subtitle = "إرسال أمر فتح الدرج للطابعة الحرارية عند كل عملية بيع ناجحة",
                    checked = config.openCashDrawer,
                    onCheckedChange = { onUpdateConfig(config.copy(openCashDrawer = it)) }
                )

                // Custom Header / Footer inputs
                OutlinedTextField(
                    value = config.headerCustomMessage,
                    onValueChange = { onUpdateConfig(config.copy(headerCustomMessage = it)) },
                    label = { Text("رسالة رأس الوصل المخصصة (اختياري)") },
                    placeholder = { Text("مثال: رخصة رقم ... / س.ت ...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = config.footerCustomMessage,
                    onValueChange = { onUpdateConfig(config.copy(footerCustomMessage = it)) },
                    label = { Text("رسالة تذييل الوصل المخصصة") },
                    placeholder = { Text("مثال: شكراً لزيارتكم - البضاعة المباعة لا ترد") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Test Print
                Button(
                    onClick = onPrintTestReceipt,
                    enabled = !isPrinting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("thermal_print_test_btn")
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("طباعة وصل تجريبي حراري", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopPrinterSettingsCard(
    config: com.example.utils.DesktopPrinterConfig,
    onUpdateConfig: (com.example.utils.DesktopPrinterConfig) -> Unit = {},
    onTestPrintDesktopPage: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(true) }
    var isPrintingTest by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.2.dp,
            if (config.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("desktop_printer_settings_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title, Active Switch, Expand/Collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (config.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = if (config.enabled) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "طابعات سطح المكتب (A4 / A5)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (config.enabled) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (config.enabled) "نشطة ✓" else "معطلة",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (config.enabled) Color(0xFF059669) else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Canon • HP • Epson • Brother ومحرك الطباعة المكتبي",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { onUpdateConfig(config.copy(enabled = it)) },
                        modifier = Modifier.testTag("desktop_printer_master_switch")
                    )
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "طي" else "توسيع"
                        )
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 1: Paper Size & Orientation
                Text(
                    text = "حجم الورق واتجاه الصفحة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // A4 Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (config.defaultPaperSize == "A4") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (config.defaultPaperSize == "A4") MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUpdateConfig(config.copy(defaultPaperSize = "A4")) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("A4 قياسي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("210 × 297 مم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // A5 Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (config.defaultPaperSize == "A5") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (config.defaultPaperSize == "A5") MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onUpdateConfig(config.copy(defaultPaperSize = "A5")) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("A5 نصف ورقة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("148 × 210 مم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Orientation & Margins
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Orientation toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("الاتجاه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (config.orientation == "LANDSCAPE") "أفقي (Landscape)" else "عمودي (Portrait)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    val next = if (config.orientation == "PORTRAIT") "LANDSCAPE" else "PORTRAIT"
                                    onUpdateConfig(config.copy(orientation = next))
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (config.orientation == "LANDSCAPE") Icons.Default.ViewCarousel else Icons.Default.Description,
                                    contentDescription = "تبديل الاتجاه",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Margins toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("الهوامش", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    when (config.marginType) {
                                        "NARROW" -> "ضيقة (5 مم)"
                                        "WIDE" -> "عريضة (15 مم)"
                                        else -> "عادية (10 مم)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = {
                                    val next = when (config.marginType) {
                                        "NORMAL" -> "NARROW"
                                        "NARROW" -> "WIDE"
                                        else -> "NORMAL"
                                    }
                                    onUpdateConfig(config.copy(marginType = next))
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsOverscan,
                                    contentDescription = "تبديل الهوامش",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 2: Invoice Elements & Branding Toggles
                Text(
                    text = "عناصر الفاتورة والعلامة التجارية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PrinterSettingCheckRow(
                        title = "طباعة شعار المتجر بالترويسة",
                        subtitle = "إظهار لوجو المتجر أعلى الفاتورة",
                        checked = config.printStoreLogo,
                        onCheckedChange = { onUpdateConfig(config.copy(printStoreLogo = it)) }
                    )

                    PrinterSettingCheckRow(
                        title = "طباعة رمز الاستجابة السريعة (QR Code)",
                        subtitle = "تضمين رمز QR للتحقق وتفاصيل الفاتورة",
                        checked = config.printQrCode,
                        onCheckedChange = { onUpdateConfig(config.copy(printQrCode = it)) }
                    )

                    PrinterSettingCheckRow(
                        title = "طباعة اسم البائع وبيانات العميل",
                        subtitle = "إظهار الكاشير والديون السابقة للعميل إن وجدت",
                        checked = config.printCustomerInfo,
                        onCheckedChange = { onUpdateConfig(config.copy(printCustomerInfo = it, printSellerName = it)) }
                    )

                    PrinterSettingCheckRow(
                        title = "وضع توفير الحبر (Monochrome)",
                        subtitle = "طباعة باللون الأسود والأبيض لتقليل استهلاك الحبر",
                        checked = config.inkSavingMode,
                        onCheckedChange = { onUpdateConfig(config.copy(inkSavingMode = it)) }
                    )

                    PrinterSettingCheckRow(
                        title = "فتح نافذة الطباعة تلقائياً عند الدفع",
                        subtitle = "إطلاق حوار الطباعة فور إتمام عملية البيع بالكاشير",
                        checked = config.autoPrintOnSaleComplete,
                        onCheckedChange = { onUpdateConfig(config.copy(autoPrintOnSaleComplete = it)) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 3: Copies & Custom Footer Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("عدد النسخ الافتراضية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("نسخ الفاتورة المطبوعة تلقائياً", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (config.copiesCount > 1) {
                                    onUpdateConfig(config.copy(copiesCount = config.copiesCount - 1))
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Text(
                            text = "${config.copiesCount}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = {
                                if (config.copiesCount < 5) {
                                    onUpdateConfig(config.copy(copiesCount = config.copiesCount + 1))
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = config.customFooterText,
                    onValueChange = { onUpdateConfig(config.copy(customFooterText = it)) },
                    label = { Text("شروط وسياسة الفاتورة (التذييل)", fontSize = 12.sp) },
                    placeholder = { Text("مثال: البضاعة المباعة لا ترد ولا تستبدل إلا بالفاتورة خلال 3 أيام", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 3
                )

                // Section 4: Action Buttons (Test Print & Reset)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isPrintingTest = true
                            scope.launch {
                                try {
                                    onTestPrintDesktopPage()
                                    Toast.makeText(context, "جاري فتح نظام الطباعة المكتبي A4/A5...", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isPrintingTest = false
                                }
                            }
                        },
                        enabled = !isPrintingTest && config.enabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("desktop_printer_test_btn")
                    ) {
                        if (isPrintingTest) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("طباعة صفحة تجريبية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onUpdateConfig(com.example.utils.DesktopPrinterConfig())
                            Toast.makeText(context, "تمت استعادة الإعدادات الافتراضية لطابعة A4", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("desktop_printer_reset_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("استعادة", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrinterSettingCheckRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun ScheduledReportsControlAndArchiveCard(
    config: com.example.utils.ScheduledReportConfig,
    archiveList: Any,
    onUpdateConfig: (com.example.utils.ScheduledReportConfig) -> Unit = {},
    onGenerateReportNow: (Boolean) -> Unit = {},
    onPreviewArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onEmailArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onShareArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onPrintArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onDeleteArchivedReport: (com.example.utils.ArchivedScheduledReport) -> Unit = {},
    onClearAllArchivedReports: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("أرشفة وتقارير المبيعات المجدولة PDF", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}
