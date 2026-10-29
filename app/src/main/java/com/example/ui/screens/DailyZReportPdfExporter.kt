package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
import kotlin.math.abs

object DailyZReportPdfExporter {

    private val COLOR_PRIMARY_DARK = android.graphics.Color.rgb(15, 23, 42)
    private val COLOR_EMERALD = android.graphics.Color.rgb(16, 185, 129)
    private val COLOR_EMERALD_DARK = android.graphics.Color.rgb(5, 150, 105)
    private val COLOR_EMERALD_BG = android.graphics.Color.rgb(236, 253, 245)
    private val COLOR_BLUE = android.graphics.Color.rgb(37, 99, 235)
    private val COLOR_BLUE_BG = android.graphics.Color.rgb(239, 246, 255)
    private val COLOR_PURPLE = android.graphics.Color.rgb(124, 58, 237)
    private val COLOR_RED = android.graphics.Color.rgb(220, 38, 38)
    private val COLOR_RED_BG = android.graphics.Color.rgb(254, 242, 242)
    private val COLOR_AMBER = android.graphics.Color.rgb(217, 119, 6)
    private val COLOR_SLATE_TEXT = android.graphics.Color.rgb(51, 65, 85)
    private val COLOR_SLATE_MUTED = android.graphics.Color.rgb(100, 116, 139)
    private val COLOR_BORDER = android.graphics.Color.rgb(203, 213, 225)
    private val COLOR_ROW_ALT = android.graphics.Color.rgb(248, 250, 252)

    private fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value)

    fun suggestedFileName(dayKey: String): String {
        val safeDay = dayKey.replace(Regex("[^0-9A-Za-z_-]"), "_")
        return "Z_Report_${safeDay}.pdf"
    }

    fun generateCachePdfFile(
        context: Context,
        report: DailyZReportData,
        storeName: String,
        currencySymbol: String,
        preparedBy: String,
        shiftNotes: String,
        isClosed: Boolean,
        shiftClosedTime: String
    ): File {
        val reportsDir = File(context.cacheDir, "reports_pdf")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val outFile = File(reportsDir, suggestedFileName(report.dayKey))
        FileOutputStream(outFile).use { fos ->
            writeZReportPdfToStream(
                report = report,
                storeName = storeName,
                currencySymbol = currencySymbol,
                preparedBy = preparedBy,
                shiftNotes = shiftNotes,
                isClosed = isClosed,
                shiftClosedTime = shiftClosedTime,
                outputStream = fos
            )
        }
        return outFile
    }

    fun saveZReportPdfToUri(
        context: Context,
        targetUri: Uri,
        report: DailyZReportData,
        storeName: String,
        currencySymbol: String,
        preparedBy: String,
        shiftNotes: String,
        isClosed: Boolean,
        shiftClosedTime: String
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                writeZReportPdfToStream(
                    report = report,
                    storeName = storeName,
                    currencySymbol = currencySymbol,
                    preparedBy = preparedBy,
                    shiftNotes = shiftNotes,
                    isClosed = isClosed,
                    shiftClosedTime = shiftClosedTime,
                    outputStream = out
                )
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareZReportPdf(
        context: Context,
        report: DailyZReportData,
        storeName: String,
        currencySymbol: String,
        preparedBy: String,
        shiftNotes: String,
        isClosed: Boolean,
        shiftClosedTime: String,
        preferWhatsApp: Boolean = false
    ) {
        try {
            val pdfFile = generateCachePdfFile(
                context = context,
                report = report,
                storeName = storeName,
                currencySymbol = currencySymbol,
                preparedBy = preparedBy,
                shiftNotes = shiftNotes,
                isClosed = isClosed,
                shiftClosedTime = shiftClosedTime
            )
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val captionText = buildString {
                appendLine("📊 تقرير الإغلاق اليومي وحركة الصندوق (Z-Report) - PDF")
                appendLine("🏪 المتجر: ${storeName.ifBlank { "AF store" }}")
                appendLine("📅 التاريخ: ${report.dayKey}")
                appendLine("💵 النقد المتوقع في الدرج: ${fmt(report.expectedCashInDrawer)} $currencySymbol")
                appendLine("🛒 إجمالي المبيعات: ${fmt(report.totalSalesAmount)} $currencySymbol (${report.invoicesCount} فاتورة)")
                appendLine("🤝 الديون المحصلة: ${fmt(report.collectedDebtsTotal)} $currencySymbol")
                appendLine("💸 المصروفات اليومية: ${fmt(report.totalExpensesAmount)} $currencySymbol")
                append("📈 صافي ربح اليوم: ${fmt(report.netDailyProfit)} $currencySymbol")
            }

            val subject = "تقرير الإغلاق اليومي Z-Report (${report.dayKey}) - ${storeName.ifBlank { "AF store" }}"

            if (preferWhatsApp) {
                for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
                    try {
                        val waIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, captionText)
                            clipData = ClipData.newRawUri(pdfFile.name, contentUri)
                            setPackage(pkg)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(waIntent)
                        Toast.makeText(context, "جاري إرسال تقرير Z-Report بصيغة PDF عبر واتساب 📄", Toast.LENGTH_SHORT).show()
                        return
                    } catch (_: Exception) {
                    }
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, captionText)
                clipData = ClipData.newRawUri(pdfFile.name, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "إرسال أو مشاركة تقرير Z-Report بصيغة PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "تم تجهيز ملف التقرير (PDF) للإرسال 📄", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة ملف PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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

    fun printZReportPdf(
        context: Context,
        report: DailyZReportData,
        storeName: String,
        currencySymbol: String,
        preparedBy: String,
        shiftNotes: String,
        isClosed: Boolean,
        shiftClosedTime: String
    ) {
        val activity = findActivity(context)
        val targetContext = activity ?: context
        val printManager = targetContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (activity == null || printManager == null) {
            shareZReportPdf(
                context = context,
                report = report,
                storeName = storeName,
                currencySymbol = currencySymbol,
                preparedBy = preparedBy,
                shiftNotes = shiftNotes,
                isClosed = isClosed,
                shiftClosedTime = shiftClosedTime,
                preferWhatsApp = false
            )
            return
        }

        try {
            val fileName = suggestedFileName(report.dayKey)
            val jobName = "Z_Report_${report.dayKey}"
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(fileName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }
                    try {
                        FileOutputStream(destination.fileDescriptor).use { out ->
                            writeZReportPdfToStream(
                                report = report,
                                storeName = storeName,
                                currencySymbol = currencySymbol,
                                preparedBy = preparedBy,
                                shiftNotes = shiftNotes,
                                isClosed = isClosed,
                                shiftClosedTime = shiftClosedTime,
                                outputStream = out
                            )
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.localizedMessage ?: "خطأ أثناء كتابة ملف PDF")
                    }
                }
            }
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        } catch (_: Exception) {
            shareZReportPdf(
                context = context,
                report = report,
                storeName = storeName,
                currencySymbol = currencySymbol,
                preparedBy = preparedBy,
                shiftNotes = shiftNotes,
                isClosed = isClosed,
                shiftClosedTime = shiftClosedTime,
                preferWhatsApp = false
            )
        }
    }

    fun writeZReportPdfToStream(
        report: DailyZReportData,
        storeName: String,
        currencySymbol: String,
        preparedBy: String,
        shiftNotes: String,
        isClosed: Boolean,
        shiftClosedTime: String,
        outputStream: OutputStream
    ) {
        val doc = PdfDocument()
        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 32f
            val leftEdge = margin
            val rightEdge = pageWidth - margin
            val contentWidth = rightEdge - leftEdge
            val bottomLimit = pageHeight - 54f

            var pageNumber = 1
            var currentPage = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = currentPage.canvas
            var y = margin

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(209, 250, 229)
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.RIGHT
            }
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PRIMARY_DARK
                textSize = 12.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val labelRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_TEXT
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val valueLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_PRIMARY_DARK
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val mutedRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_SLATE_MUTED
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.RIGHT
            }
            val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            val boxStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = COLOR_BORDER
            }

            fun drawPageFooter(c: Canvas, pNum: Int) {
                val footerY = pageHeight - 20f
                val footerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_BORDER
                    strokeWidth = 0.8f
                }
                c.drawLine(leftEdge, footerY - 12f, rightEdge, footerY - 12f, footerLinePaint)

                val footerRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_SLATE_MUTED
                    textSize = 8.5f
                    textAlign = Paint.Align.RIGHT
                }
                val footerLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_SLATE_MUTED
                    textSize = 8.5f
                    textAlign = Paint.Align.LEFT
                }
                c.drawText(
                    "تقرير الإغلاق اليومي الذكي (Z-Report) • ${storeName.ifBlank { "AF store" }} • ${report.dayKey}",
                    rightEdge,
                    footerY,
                    footerRight
                )
                c.drawText("Page $pNum", leftEdge, footerY, footerLeft)
            }

            fun ensureSpace(requiredHeight: Float) {
                if (y + requiredHeight > bottomLimit) {
                    drawPageFooter(canvas, pageNumber)
                    doc.finishPage(currentPage)
                    pageNumber++
                    currentPage = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = currentPage.canvas
                    y = margin
                }
            }

            val issueTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

            // 1. Header Banner
            val bannerHeight = 76f
            val bannerRect = RectF(leftEdge, y, rightEdge, y + bannerHeight)
            boxFillPaint.color = COLOR_PRIMARY_DARK
            canvas.drawRoundRect(bannerRect, 12f, 12f, boxFillPaint)

            val accentStripe = RectF(rightEdge - 8f, y, rightEdge, y + bannerHeight)
            boxFillPaint.color = COLOR_EMERALD
            canvas.drawRoundRect(accentStripe, 4f, 4f, boxFillPaint)

            canvas.drawText(
                "تقرير الإغلاق اليومي وحركة الصندوق (Z-Report)",
                rightEdge - 18f,
                y + 24f,
                titlePaint
            )
            canvas.drawText(
                "المتجر: ${storeName.ifBlank { "AF store" }}  |  المسؤول: $preparedBy",
                rightEdge - 18f,
                y + 44f,
                subHeaderPaint
            )
            canvas.drawText(
                "تاريخ التقرير: ${report.dayKey}  |  وقت الإصدار: $issueTimeStr",
                rightEdge - 18f,
                y + 62f,
                subHeaderPaint
            )

            // Status pill on left side of banner
            val statusBadgeRect = RectF(leftEdge + 14f, y + 16f, leftEdge + 155f, y + 42f)
            boxFillPaint.color = if (isClosed) COLOR_EMERALD_DARK else COLOR_AMBER
            canvas.drawRoundRect(statusBadgeRect, 8f, 8f, boxFillPaint)
            val statusText = if (isClosed) "مغلق ومعتمد ✓" else "مفتوح قيد المراجعة"
            val badgeTextPaint = Paint(badgePaint).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(
                statusText,
                statusBadgeRect.centerX(),
                statusBadgeRect.centerY() + 4f,
                badgeTextPaint
            )
            if (isClosed && shiftClosedTime.isNotBlank()) {
                val closedTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(203, 213, 225)
                    textSize = 8.5f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    "وقت الإغلاق: $shiftClosedTime",
                    statusBadgeRect.centerX(),
                    y + 58f,
                    closedTimePaint
                )
            }

            y += bannerHeight + 14f

            // 2. Top 4 KPI Summary Cards
            val cardGap = 8f
            val cardW = (contentWidth - cardGap * 3) / 4f
            val cardH = 54f

            data class KpiCard(val title: String, val value: String, val sub: String, val color: Int, val bgColor: Int)
            val kpis = listOf(
                KpiCard(
                    "النقد الصافي بالدرج",
                    "${fmt(report.expectedCashInDrawer)} $currencySymbol",
                    "الرصيد الدفتري للدرج",
                    COLOR_EMERALD_DARK,
                    COLOR_EMERALD_BG
                ),
                KpiCard(
                    "إجمالي المبيعات",
                    "${fmt(report.totalSalesAmount)} $currencySymbol",
                    "${report.invoicesCount} فاتورة (${report.itemsSoldCount} قطعة)",
                    COLOR_BLUE,
                    COLOR_BLUE_BG
                ),
                KpiCard(
                    "الديون المحصلة",
                    "${fmt(report.collectedDebtsTotal)} $currencySymbol",
                    "${report.collectedDebtsList.size} عملية تحصيل",
                    COLOR_PURPLE,
                    android.graphics.Color.rgb(245, 243, 255)
                ),
                KpiCard(
                    "المصروفات اليومية",
                    "${fmt(report.totalExpensesAmount)} $currencySymbol",
                    "${report.dailyExpensesList.size} سند صرف",
                    COLOR_RED,
                    COLOR_RED_BG
                )
            )

            kpis.forEachIndexed { idx, kpi ->
                // Draw RTL from right to left
                val cardRight = rightEdge - idx * (cardW + cardGap)
                val cardLeft = cardRight - cardW
                val rect = RectF(cardLeft, y, cardRight, y + cardH)
                boxFillPaint.color = kpi.bgColor
                canvas.drawRoundRect(rect, 8f, 8f, boxFillPaint)
                boxStrokePaint.color = kpi.color
                boxStrokePaint.alpha = 90
                canvas.drawRoundRect(rect, 8f, 8f, boxStrokePaint)
                boxStrokePaint.alpha = 255

                val kTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_SLATE_TEXT
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                val kValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = kpi.color
                    textSize = 10.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                val kSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_SLATE_MUTED
                    textSize = 7.5f
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText(kpi.title, cardRight - 8f, y + 15f, kTitlePaint)
                canvas.drawText(kpi.value, cardRight - 8f, y + 33f, kValPaint)
                canvas.drawText(kpi.sub, cardRight - 8f, y + 46f, kSubPaint)
            }

            y += cardH + 16f

            // 3. Cash Drawer Reconciliation Box (أولاً: تصفية حركة الصندوق - الدرج)
            canvas.drawText("أولاً: تصفية حركة الصندوق (الدرج النقدي)", rightEdge, y, sectionTitlePaint)
            y += 8f

            val drawerRows = mutableListOf<Triple<String, String, Int>>()
            drawerRows.add(Triple("رصيد افتتاح الدرج (العهدة الصباحية)", "${fmt(report.openingCashBalance)} $currencySymbol", COLOR_PRIMARY_DARK))
            drawerRows.add(Triple("(+) المبيعات النقدية المباشرة (كاش)", "+ ${fmt(report.cashSalesAmount)} $currencySymbol", COLOR_EMERALD_DARK))
            drawerRows.add(Triple("(+) الديون المحصلة نقداً من العملاء اليوم", "+ ${fmt(report.collectedDebtsTotal)} $currencySymbol", COLOR_PURPLE))
            drawerRows.add(Triple("(-) المصروفات النقدية المسددة من الدرج", "- ${fmt(report.cashExpensesAmount)} $currencySymbol", COLOR_RED))
            drawerRows.add(Triple("(=) إجمالي النقد الصافي المتوقع في الدرج", "${fmt(report.expectedCashInDrawer)} $currencySymbol", COLOR_EMERALD_DARK))

            if (report.actualCountedCash != null) {
                val diff = report.cashVariance ?: 0.0
                val diffLabel = when {
                    abs(diff) < 0.01 -> "متطابق تماماً ✓"
                    diff > 0 -> "فائض نقدي (+${fmt(diff)} $currencySymbol)"
                    else -> "عجز في الدرج (${fmt(diff)} $currencySymbol)"
                }
                val diffColor = when {
                    abs(diff) < 0.01 -> COLOR_EMERALD_DARK
                    diff > 0 -> COLOR_BLUE
                    else -> COLOR_RED
                }
                drawerRows.add(
                    Triple(
                        "الجرد الفعلي للنقد الموجود بالدرج ($diffLabel)",
                        "${fmt(report.actualCountedCash)} $currencySymbol",
                        diffColor
                    )
                )
            }

            if (report.supplierCashPaidToday > 0.0) {
                drawerRows.add(
                    Triple(
                        "مدفوعات الموردين المسددة اليوم",
                        "- ${fmt(report.supplierCashPaidToday)} $currencySymbol",
                        COLOR_AMBER
                    )
                )
                drawerRows.add(
                    Triple(
                        "صافي النقد بعد خصم دفعات الموردين",
                        "${fmt(report.expectedCashAfterSuppliers)} $currencySymbol",
                        COLOR_PRIMARY_DARK
                    )
                )
            }

            val rowH = 22f
            val drawerBoxH = drawerRows.size * rowH + 12f
            ensureSpace(drawerBoxH + 16f)

            val drawerRect = RectF(leftEdge, y, rightEdge, y + drawerBoxH)
            boxFillPaint.color = android.graphics.Color.WHITE
            canvas.drawRoundRect(drawerRect, 8f, 8f, boxFillPaint)
            boxStrokePaint.color = COLOR_BORDER
            canvas.drawRoundRect(drawerRect, 8f, 8f, boxStrokePaint)

            var rowY = y + 6f
            drawerRows.forEachIndexed { idx, (label, valStr, valColor) ->
                if (idx == 4) {
                    // Highlight Expected Cash row
                    val hiRect = RectF(leftEdge + 2f, rowY, rightEdge - 2f, rowY + rowH)
                    boxFillPaint.color = COLOR_EMERALD_BG
                    canvas.drawRect(hiRect, boxFillPaint)
                } else if (idx % 2 == 1) {
                    val altRect = RectF(leftEdge + 2f, rowY, rightEdge - 2f, rowY + rowH)
                    boxFillPaint.color = COLOR_ROW_ALT
                    canvas.drawRect(altRect, boxFillPaint)
                }

                val lPaint = Paint(labelRightPaint).apply {
                    textSize = if (idx == 4) 11f else 10f
                    color = if (idx == 4) COLOR_EMERALD_DARK else COLOR_SLATE_TEXT
                }
                val vPaint = Paint(valueLeftPaint).apply {
                    textSize = if (idx == 4) 11.5f else 10.5f
                    color = valColor
                }
                canvas.drawText(label, rightEdge - 12f, rowY + 15f, lPaint)
                canvas.drawText(valStr, leftEdge + 12f, rowY + 15f, vPaint)
                rowY += rowH
            }

            y += drawerBoxH + 16f

            // 4. Sales & Profit Breakdown (ثانياً: تفصيل المبيعات والأرباح اليومية)
            ensureSpace(150f)
            canvas.drawText("ثانياً: تفصيل المبيعات والأرباح اليومية", rightEdge, y, sectionTitlePaint)
            y += 8f

            val salesRows = mutableListOf<Triple<String, String, Int>>(
                Triple("إجمالي المبيعات الفعالة (${report.invoicesCount} فاتورة / ${report.itemsSoldCount} قطعة)", "${fmt(report.totalSalesAmount)} $currencySymbol", COLOR_BLUE),
                Triple("مبيعات نقدية مباشرة (كاش)", "${fmt(report.cashSalesAmount)} $currencySymbol", COLOR_EMERALD_DARK),
                Triple("مبيعات آجلة (ديون جديدة على العملاء)", "${fmt(report.debtSalesAmount)} $currencySymbol", COLOR_AMBER),
                Triple("مبيعات بطاقة بنكية / تحويل", "${fmt(report.cardOrTransferSalesAmount)} $currencySymbol", COLOR_PURPLE),
                Triple("التكلفة التقديرية للبضاعة المباعة (COGS)", "${fmt(report.estimatedCostOfGoodsSold)} $currencySymbol", COLOR_SLATE_TEXT),
                Triple("مجمل ربح المبيعات (قبل المصاريف)", "${fmt(report.grossProfitFromSales)} $currencySymbol", COLOR_BLUE),
                Triple("صافي الربح اليومي (بعد خصم المصروفات والتوالف)", "${fmt(report.netDailyProfit)} $currencySymbol", if (report.netDailyProfit >= 0) COLOR_EMERALD_DARK else COLOR_RED)
            )
            if (report.damagedLossToday > 0.0) {
                salesRows.add(Triple("خسائر السلع التالفة والهالك اليوم", "- ${fmt(report.damagedLossToday)} $currencySymbol", COLOR_RED))
            }
            if (report.pendingReturnRequestsCount > 0) {
                salesRows.add(Triple("طلبات إرجاع فواتير بانتظار موافقة المدير (${report.pendingReturnRequestsCount})", "${fmt(report.pendingReturnRequestsAmount)} $currencySymbol", COLOR_AMBER))
            }

            val salesBoxH = salesRows.size * rowH + 12f
            ensureSpace(salesBoxH + 16f)
            val salesRect = RectF(leftEdge, y, rightEdge, y + salesBoxH)
            boxFillPaint.color = android.graphics.Color.WHITE
            canvas.drawRoundRect(salesRect, 8f, 8f, boxFillPaint)
            boxStrokePaint.color = COLOR_BORDER
            canvas.drawRoundRect(salesRect, 8f, 8f, boxStrokePaint)

            var sRowY = y + 6f
            salesRows.forEachIndexed { idx, (label, valStr, valColor) ->
                if (idx % 2 == 1) {
                    val altRect = RectF(leftEdge + 2f, sRowY, rightEdge - 2f, sRowY + rowH)
                    boxFillPaint.color = COLOR_ROW_ALT
                    canvas.drawRect(altRect, boxFillPaint)
                }
                val vPaint = Paint(valueLeftPaint).apply {
                    textSize = 10.5f
                    color = valColor
                }
                canvas.drawText(label, rightEdge - 12f, sRowY + 15f, labelRightPaint)
                canvas.drawText(valStr, leftEdge + 12f, sRowY + 15f, vPaint)
                sRowY += rowH
            }

            y += salesBoxH + 16f

            // 5. Collected Customer Debts Table (ثالثاً: الديون المحصلة من العملاء اليوم)
            ensureSpace(75f)
            canvas.drawText(
                "ثالثاً: الديون المحصلة نقداً من العملاء اليوم (${report.collectedDebtsList.size} عملية - ${fmt(report.collectedDebtsTotal)} $currencySymbol)",
                rightEdge,
                y,
                sectionTitlePaint
            )
            y += 8f

            val tableHeaderH = 22f
            fun drawDebtsTableHeader() {
                val hRect = RectF(leftEdge, y, rightEdge, y + tableHeaderH)
                boxFillPaint.color = android.graphics.Color.rgb(241, 245, 249)
                canvas.drawRoundRect(hRect, 6f, 6f, boxFillPaint)
                boxStrokePaint.color = COLOR_BORDER
                canvas.drawRoundRect(hRect, 6f, 6f, boxStrokePaint)

                val thRight = Paint(labelRightPaint).apply { textSize = 9.5f }
                val thLeft = Paint(valueLeftPaint).apply { textSize = 9.5f }
                canvas.drawText("اسم العميل", rightEdge - 10f, y + 15f, thRight)
                canvas.drawText("الوقت", rightEdge - 190f, y + 15f, thRight)
                canvas.drawText("البيان / ملاحظة", rightEdge - 260f, y + 15f, thRight)
                canvas.drawText("المبلغ المحصل", leftEdge + 10f, y + 15f, thLeft)
                y += tableHeaderH
            }

            drawDebtsTableHeader()

            if (report.collectedDebtsList.isEmpty()) {
                val emptyRect = RectF(leftEdge, y, rightEdge, y + 24f)
                boxStrokePaint.color = COLOR_BORDER
                canvas.drawRect(emptyRect, boxStrokePaint)
                canvas.drawText(
                    "لا توجد عمليات تحصيل ديون مسجلة في هذا اليوم",
                    rightEdge - 12f,
                    y + 16f,
                    mutedRightPaint
                )
                y += 24f
            } else {
                val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
                report.collectedDebtsList.forEachIndexed { idx, tx ->
                    if (y + 20f > bottomLimit) {
                        drawPageFooter(canvas, pageNumber)
                        doc.finishPage(currentPage)
                        pageNumber++
                        currentPage = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                        canvas = currentPage.canvas
                        y = margin
                        drawDebtsTableHeader()
                    }
                    val rRect = RectF(leftEdge, y, rightEdge, y + 20f)
                    if (idx % 2 == 1) {
                        boxFillPaint.color = COLOR_ROW_ALT
                        canvas.drawRect(rRect, boxFillPaint)
                    }
                    boxStrokePaint.color = COLOR_BORDER
                    canvas.drawRect(rRect, boxStrokePaint)

                    val cellRight = Paint(labelRightPaint).apply {
                        textSize = 9.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    val cellLeft = Paint(valueLeftPaint).apply {
                        textSize = 9.5f
                        color = COLOR_PURPLE
                    }
                    val custName = tx.customerName.ifBlank { "عميل #${tx.customerId}" }.take(24)
                    val timeStr = timeFmt.format(Date(tx.date))
                    val noteStr = tx.notes.ifBlank { "تسديد دفعة" }.take(28)

                    canvas.drawText(custName, rightEdge - 10f, y + 14f, cellRight)
                    canvas.drawText(timeStr, rightEdge - 190f, y + 14f, cellRight)
                    canvas.drawText(noteStr, rightEdge - 260f, y + 14f, cellRight)
                    canvas.drawText("+ ${fmt(tx.amount)} $currencySymbol", leftEdge + 10f, y + 14f, cellLeft)
                    y += 20f
                }
            }

            y += 16f

            // 6. Daily Expenses Table (رابعاً: المصروفات والنفقات اليومية)
            ensureSpace(75f)
            canvas.drawText(
                "رابعاً: المصروفات والنفقات اليومية (${report.dailyExpensesList.size} سند - ${fmt(report.totalExpensesAmount)} $currencySymbol)",
                rightEdge,
                y,
                sectionTitlePaint
            )
            y += 8f

            fun drawExpensesTableHeader() {
                val hRect = RectF(leftEdge, y, rightEdge, y + tableHeaderH)
                boxFillPaint.color = android.graphics.Color.rgb(241, 245, 249)
                canvas.drawRoundRect(hRect, 6f, 6f, boxFillPaint)
                boxStrokePaint.color = COLOR_BORDER
                canvas.drawRoundRect(hRect, 6f, 6f, boxStrokePaint)

                val thRight = Paint(labelRightPaint).apply { textSize = 9.5f }
                val thLeft = Paint(valueLeftPaint).apply { textSize = 9.5f }
                canvas.drawText("بيان المصروف", rightEdge - 10f, y + 15f, thRight)
                canvas.drawText("التصنيف", rightEdge - 210f, y + 15f, thRight)
                canvas.drawText("طريقة الدفع", rightEdge - 330f, y + 15f, thRight)
                canvas.drawText("المبلغ", leftEdge + 10f, y + 15f, thLeft)
                y += tableHeaderH
            }

            drawExpensesTableHeader()

            if (report.dailyExpensesList.isEmpty()) {
                val emptyRect = RectF(leftEdge, y, rightEdge, y + 24f)
                boxStrokePaint.color = COLOR_BORDER
                canvas.drawRect(emptyRect, boxStrokePaint)
                canvas.drawText(
                    "لا توجد مصروفات مسجلة في هذا اليوم",
                    rightEdge - 12f,
                    y + 16f,
                    mutedRightPaint
                )
                y += 24f
            } else {
                report.dailyExpensesList.forEachIndexed { idx, exp ->
                    if (y + 20f > bottomLimit) {
                        drawPageFooter(canvas, pageNumber)
                        doc.finishPage(currentPage)
                        pageNumber++
                        currentPage = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                        canvas = currentPage.canvas
                        y = margin
                        drawExpensesTableHeader()
                    }
                    val rRect = RectF(leftEdge, y, rightEdge, y + 20f)
                    if (idx % 2 == 1) {
                        boxFillPaint.color = COLOR_ROW_ALT
                        canvas.drawRect(rRect, boxFillPaint)
                    }
                    boxStrokePaint.color = COLOR_BORDER
                    canvas.drawRect(rRect, boxStrokePaint)

                    val cellRight = Paint(labelRightPaint).apply {
                        textSize = 9.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                    val cellLeft = Paint(valueLeftPaint).apply {
                        textSize = 9.5f
                        color = COLOR_RED
                    }
                    val payLabel = if (exp.paymentMethod.equals("CASH", ignoreCase = true) || exp.paymentMethod.isBlank()) {
                        "نقداً من الدرج"
                    } else {
                        exp.paymentMethod
                    }
                    canvas.drawText(exp.title.take(28), rightEdge - 10f, y + 14f, cellRight)
                    canvas.drawText(exp.category.labelAr.take(18), rightEdge - 210f, y + 14f, cellRight)
                    canvas.drawText(payLabel, rightEdge - 330f, y + 14f, cellRight)
                    canvas.drawText("- ${fmt(exp.amount)} $currencySymbol", leftEdge + 10f, y + 14f, cellLeft)
                    y += 20f
                }
            }

            y += 16f

            // 7. Shift Notes & Signatures
            ensureSpace(90f)
            if (shiftNotes.isNotBlank()) {
                canvas.drawText("ملاحظات إغلاق وتسليم الوردية:", rightEdge, y, sectionTitlePaint)
                y += 6f
                val noteRect = RectF(leftEdge, y, rightEdge, y + 32f)
                boxFillPaint.color = COLOR_ROW_ALT
                canvas.drawRoundRect(noteRect, 6f, 6f, boxFillPaint)
                boxStrokePaint.color = COLOR_BORDER
                canvas.drawRoundRect(noteRect, 6f, 6f, boxStrokePaint)
                canvas.drawText(shiftNotes.take(95), rightEdge - 10f, y + 20f, labelRightPaint)
                y += 44f
            }

            ensureSpace(50f)
            val sigBoxW = (contentWidth - 20f) / 2f
            val sigRightRect = RectF(rightEdge - sigBoxW, y, rightEdge, y + 42f)
            val sigLeftRect = RectF(leftEdge, y, leftEdge + sigBoxW, y + 42f)
            boxStrokePaint.color = COLOR_BORDER
            canvas.drawRoundRect(sigRightRect, 6f, 6f, boxStrokePaint)
            canvas.drawRoundRect(sigLeftRect, 6f, 6f, boxStrokePaint)

            canvas.drawText("توقيع الكاشير / مسؤول الوردية: $preparedBy", rightEdge - 10f, y + 18f, mutedRightPaint)
            canvas.drawText("توقيع المدير / المستلم: ............................", leftEdge + sigBoxW - 10f, y + 18f, mutedRightPaint)

            drawPageFooter(canvas, pageNumber)
            doc.finishPage(currentPage)
            doc.writeTo(outputStream)
        } finally {
            doc.close()
        }
    }
}
