package com.example.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path as AndroidPath
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

data class SalesReportPdfPayload(
    val storeName: String = "",
    val storePhone: String = "",
    val currencySymbol: String = "",
    val period: SalesReportPeriod = SalesReportPeriod.DAILY_7,
    val chartDisplayMode: SalesChartDisplayMode = SalesChartDisplayMode.BAR_AND_TREND,
    val metricComparisonMode: SalesMetricComparisonMode = SalesMetricComparisonMode.SALES_VS_PROFIT,
    val overview: PeriodOverviewSummary = PeriodOverviewSummary(),
    val totalPurchasesCost: Double = 0.0,
    val buckets: List<SalesTimeBucket> = emptyList(),
    val topSellingProducts: List<Triple<String, Int, Double>> = emptyList(),
    val generatedAtMillis: Long = System.currentTimeMillis()
) {
    val formattedExportDate: String
        get() = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US).format(Date(generatedAtMillis))

    val suggestedFileName: String
        get() {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(generatedAtMillis))
            return "AFStore_Report_${period.name}_$stamp.pdf"
        }
}

object SalesReportPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val CONTENT_LEFT = MARGIN
    private const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN
    private const val CONTENT_WIDTH = CONTENT_RIGHT - CONTENT_LEFT

    // High-contrast print-friendly colors
    private val COLOR_NAVY_HEADER = AndroidColor.rgb(15, 23, 42)
    private val COLOR_SLATE_DARK = AndroidColor.rgb(30, 41, 59)
    private val COLOR_SLATE_MUTED = AndroidColor.rgb(100, 116, 139)
    private val COLOR_BORDER = AndroidColor.rgb(203, 213, 225)
    private val COLOR_ROW_ALT = AndroidColor.rgb(248, 250, 252)
    private val COLOR_TABLE_HEADER = AndroidColor.rgb(241, 245, 249)

    private val COLOR_BLUE_SALES = AndroidColor.rgb(2, 132, 199)
    private val COLOR_GREEN_PROFIT = AndroidColor.rgb(5, 150, 105)
    private val COLOR_AMBER_DEBT = AndroidColor.rgb(217, 119, 6)
    private val COLOR_PURPLE_TRANSFER = AndroidColor.rgb(124, 58, 237)

    fun writeReportPdfToStream(
        payload: SalesReportPdfPayload,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        try {
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var currentPage = pdfDocument.startPage(pageInfo)
            var canvas = currentPage.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 15f
                color = AndroidColor.WHITE
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 9.5f
                color = AndroidColor.rgb(226, 232, 240)
            }
            val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 11.5f
                color = COLOR_NAVY_HEADER
                textAlign = Paint.Align.RIGHT
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 8.5f
                color = COLOR_SLATE_MUTED
            }
            val valueBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 10.5f
                color = COLOR_NAVY_HEADER
            }
            val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 8.5f
                color = COLOR_NAVY_HEADER
            }
            val tableCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 8.2f
                color = COLOR_SLATE_DARK
            }
            val tableCellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 8.2f
                color = COLOR_NAVY_HEADER
            }
            val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            val boxStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = COLOR_BORDER
            }

            fun drawPageFooter(c: AndroidCanvas, pageNum: Int) {
                val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 8f
                    color = COLOR_SLATE_MUTED
                }
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_BORDER
                    strokeWidth = 0.8f
                }
                c.drawLine(CONTENT_LEFT, PAGE_HEIGHT - 28f, CONTENT_RIGHT, PAGE_HEIGHT - 28f, linePaint)
                footerPaint.textAlign = Paint.Align.RIGHT
                c.drawText(
                    "${payload.storeName} • تقرير المبيعات والأرباح (${payload.period.labelAr})",
                    CONTENT_RIGHT,
                    PAGE_HEIGHT - 14f,
                    footerPaint
                )
                footerPaint.textAlign = Paint.Align.LEFT
                c.drawText(
                    "صفحة $pageNum  |  ${payload.formattedExportDate}",
                    CONTENT_LEFT,
                    PAGE_HEIGHT - 14f,
                    footerPaint
                )
            }

            fun startNewPage(): AndroidCanvas {
                drawPageFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber += 1
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                // White page background
                canvas.drawColor(AndroidColor.WHITE)
                return canvas
            }

            // Fill Page 1 White Background
            canvas.drawColor(AndroidColor.WHITE)

            var y = MARGIN

            // 1. Official Header Banner
            val headerRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + 62f)
            boxFillPaint.color = COLOR_NAVY_HEADER
            canvas.drawRoundRect(headerRect, 8f, 8f, boxFillPaint)

            titlePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                "تقرير المبيعات والأرباح المالي (${payload.period.labelAr})",
                CONTENT_RIGHT - 14f,
                y + 24f,
                titlePaint
            )
            subtitlePaint.textAlign = Paint.Align.RIGHT
            val storeMeta = buildString {
                append("المتجر: ${payload.storeName}")
                if (payload.storePhone.isNotBlank()) append("  •  هاتف: ${payload.storePhone}")
                append("  •  ${payload.period.subtitleAr}")
            }
            canvas.drawText(storeMeta, CONTENT_RIGHT - 14f, y + 41f, subtitlePaint)
            canvas.drawText(
                "تاريخ الإصدار: ${payload.formattedExportDate}  |  عدد العمليات: ${payload.overview.totalTransactions} عملية (${payload.overview.totalUnitsSold} قطعة)",
                CONTENT_RIGHT - 14f,
                y + 54f,
                subtitlePaint
            )

            y += 74f

            // 2. Executive Financial Summary KPIs (6 Cards in 2x3 Grid)
            canvas.drawText("أولاً: الملخص المالي التنفيذي للفترة المحددة", CONTENT_RIGHT, y, sectionTitlePaint)
            y += 8f

            val cur = payload.currencySymbol
            val profitMarginPct = if (payload.overview.totalSales > 0.0) {
                (payload.overview.netProfit / payload.overview.totalSales) * 100.0
            } else {
                0.0
            }

            val kpiItems = listOf(
                Triple("إجمالي المبيعات", "${fmtMoney(payload.overview.totalSales)} $cur", COLOR_BLUE_SALES),
                Triple(
                    "صافي الربح التقديري (${String.format(Locale.US, "%.0f", profitMarginPct)}%)",
                    "${fmtMoney(payload.overview.netProfit)} $cur",
                    COLOR_GREEN_PROFIT
                ),
                Triple(
                    "النقدي والتحويل المحصّل",
                    "${fmtMoney(payload.overview.cashSales + payload.overview.transferSales)} $cur",
                    COLOR_GREEN_PROFIT
                ),
                Triple("المبيعات الآجلة (الديون)", "${fmtMoney(payload.overview.debtSales)} $cur", COLOR_AMBER_DEBT),
                Triple("مبيعات اليوم / هذا الشهر", "${fmtMoney(payload.overview.todaySales)} / ${fmtMoney(payload.overview.currentMonthSales)} $cur", COLOR_BLUE_SALES),
                Triple("إجمالي فواتير التوريدات", "${fmtMoney(payload.totalPurchasesCost)} $cur", COLOR_PURPLE_TRANSFER)
            )

            val cols = 3
            val gapX = 8f
            val gapY = 8f
            val cardW = (CONTENT_WIDTH - gapX * (cols - 1)) / cols
            val cardH = 38f

            kpiItems.forEachIndexed { idx, (label, valText, accentColor) ->
                val r = idx / cols
                val c = (cols - 1) - (idx % cols) // Right-to-Left order
                val left = CONTENT_LEFT + c * (cardW + gapX)
                val top = y + r * (cardH + gapY)
                val rect = RectF(left, top, left + cardW, top + cardH)

                boxFillPaint.color = COLOR_ROW_ALT
                canvas.drawRoundRect(rect, 6f, 6f, boxFillPaint)
                boxStrokePaint.color = COLOR_BORDER
                canvas.drawRoundRect(rect, 6f, 6f, boxStrokePaint)

                // Right accent bar
                boxFillPaint.color = accentColor
                canvas.drawRoundRect(RectF(left + cardW - 3.5f, top + 4f, left + cardW, top + cardH - 4f), 2f, 2f, boxFillPaint)

                labelPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(label, left + cardW - 9f, top + 14f, labelPaint)

                valueBoldPaint.color = accentColor
                valueBoldPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(valText, left + cardW - 9f, top + 30f, valueBoldPaint)
            }

            y += (cardH * 2) + gapY + 18f

            // 3. Visual Charts Section: Side-by-Side Bar/Trend Chart (Right 63%) + Payment Methods Donut Chart (Left 35%)
            canvas.drawText(
                "ثانياً: الرسوم البيانية (${payload.chartDisplayMode.labelAr} • ${payload.metricComparisonMode.labelAr})",
                CONTENT_RIGHT,
                y,
                sectionTitlePaint
            )
            y += 8f

            val chartsHeight = 168f
            val donutBoxW = CONTENT_WIDTH * 0.35f
            val barChartBoxW = CONTENT_WIDTH - donutBoxW - 10f

            val donutRect = RectF(CONTENT_LEFT, y, CONTENT_LEFT + donutBoxW, y + chartsHeight)
            val barChartRect = RectF(CONTENT_LEFT + donutBoxW + 10f, y, CONTENT_RIGHT, y + chartsHeight)

            // Draw Bar & Trend Chart Box
            drawPrintBarAndTrendChart(
                canvas = canvas,
                box = barChartRect,
                payload = payload
            )

            // Draw Payment Methods Donut Chart Box
            drawPrintPaymentDonutChart(
                canvas = canvas,
                box = donutRect,
                payload = payload
            )

            y += chartsHeight + 18f

            // 4. Time-Series Breakdown Table (Daily or Monthly)
            val periodUnitTitle = if (payload.period.isMonthly) "الشهر" else "اليوم / التاريخ"
            canvas.drawText(
                "ثالثاً: جدول تفصيل المبيعات والأرباح حسب $periodUnitTitle (${payload.buckets.size} فترة)",
                CONTENT_RIGHT,
                y,
                sectionTitlePaint
            )
            y += 8f

            val rowH = 18.5f
            val colRatios = floatArrayOf(0.18f, 0.16f, 0.16f, 0.16f, 0.11f, 0.23f)
            // Columns from Left to Right:
            // [0] الإجمالي | [1] صافي الربح | [2] الآجل (دين) | [3] النقدي/تحويل | [4] العمليات/القطع | [5] اليوم/الشهر (Rightmost)
            val colHeaders = arrayOf(
                "إجمالي المبيعات",
                "صافي الربح",
                "الآجل (دين)",
                "النقدي والتحويل",
                "العمليات",
                periodUnitTitle
            )

            fun drawTimeTableHeader(c: AndroidCanvas, topY: Float): Float {
                val hRect = RectF(CONTENT_LEFT, topY, CONTENT_RIGHT, topY + rowH)
                boxFillPaint.color = COLOR_TABLE_HEADER
                c.drawRect(hRect, boxFillPaint)
                boxStrokePaint.color = COLOR_BORDER
                c.drawRect(hRect, boxStrokePaint)

                var accX = CONTENT_LEFT
                for (i in colRatios.indices) {
                    val w = CONTENT_WIDTH * colRatios[i]
                    tableHeaderPaint.textAlign = Paint.Align.CENTER
                    c.drawText(colHeaders[i], accX + w / 2f, topY + 12.5f, tableHeaderPaint)
                    if (i > 0) {
                        c.drawLine(accX, topY, accX, topY + rowH, boxStrokePaint)
                    }
                    accX += w
                }
                return topY + rowH
            }

            y = drawTimeTableHeader(canvas, y)

            payload.buckets.forEachIndexed { idx, bucket ->
                if (y + rowH > PAGE_HEIGHT - 44f) {
                    canvas = startNewPage()
                    y = MARGIN + 10f
                    canvas.drawText(
                        "تابع: جدول تفصيل المبيعات والأرباح (${payload.period.labelAr})",
                        CONTENT_RIGHT,
                        y,
                        sectionTitlePaint
                    )
                    y += 8f
                    y = drawTimeTableHeader(canvas, y)
                }

                val rRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + rowH)
                if (idx % 2 == 1) {
                    boxFillPaint.color = COLOR_ROW_ALT
                    canvas.drawRect(rRect, boxFillPaint)
                }
                boxStrokePaint.color = COLOR_BORDER
                canvas.drawRect(rRect, boxStrokePaint)

                val cells = arrayOf(
                    "${fmtMoney(bucket.totalSales)} $cur",
                    "${fmtMoney(bucket.netProfit)} $cur",
                    "${fmtMoney(bucket.debtSales)} $cur",
                    "${fmtMoney(bucket.collectedSales)} $cur",
                    "${bucket.transactionsCount} (${bucket.unitsSoldCount} ق)",
                    bucket.shortLabel
                )

                var accX = CONTENT_LEFT
                for (i in colRatios.indices) {
                    val w = CONTENT_WIDTH * colRatios[i]
                    val p = if (i == 0 || i == 5) tableCellBoldPaint else tableCellPaint
                    p.color = when (i) {
                        0 -> COLOR_BLUE_SALES
                        1 -> COLOR_GREEN_PROFIT
                        2 -> if (bucket.debtSales > 0) COLOR_AMBER_DEBT else COLOR_SLATE_DARK
                        else -> COLOR_SLATE_DARK
                    }
                    p.textAlign = Paint.Align.CENTER
                    canvas.drawText(cells[i], accX + w / 2f, y + 12.5f, p)
                    if (i > 0) {
                        canvas.drawLine(accX, y, accX, y + rowH, boxStrokePaint)
                    }
                    accX += w
                }
                y += rowH
            }

            y += 18f

            // 5. Top Selling Products Table
            val neededForTopProducts = 28f + (payload.topSellingProducts.size.coerceAtLeast(1) + 1) * rowH
            if (y + neededForTopProducts > PAGE_HEIGHT - 44f) {
                canvas = startNewPage()
                y = MARGIN + 10f
            }

            canvas.drawText(
                "رابعاً: أعلى الأصناف مبيعاً وإيراداً (${payload.period.labelAr})",
                CONTENT_RIGHT,
                y,
                sectionTitlePaint
            )
            y += 8f

            val prodHeaderRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + rowH)
            boxFillPaint.color = COLOR_TABLE_HEADER
            canvas.drawRect(prodHeaderRect, boxFillPaint)
            boxStrokePaint.color = COLOR_BORDER
            canvas.drawRect(prodHeaderRect, boxStrokePaint)

            val prodColRatios = floatArrayOf(0.28f, 0.22f, 0.50f)
            val prodHeaders = arrayOf("إجمالي الإيراد", "الكمية المباعة", "اسم الصنف")
            var pAccX = CONTENT_LEFT
            for (i in prodColRatios.indices) {
                val w = CONTENT_WIDTH * prodColRatios[i]
                tableHeaderPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(prodHeaders[i], pAccX + w / 2f, y + 12.5f, tableHeaderPaint)
                if (i > 0) {
                    canvas.drawLine(pAccX, y, pAccX, y + rowH, boxStrokePaint)
                }
                pAccX += w
            }
            y += rowH

            if (payload.topSellingProducts.isEmpty()) {
                val emptyRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + rowH)
                canvas.drawRect(emptyRect, boxStrokePaint)
                tableCellPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("لا توجد مبيعات مسجلة في هذه الفترة", CONTENT_LEFT + CONTENT_WIDTH / 2f, y + 12.5f, tableCellPaint)
            } else {
                payload.topSellingProducts.forEachIndexed { idx, (prodName, qty, rev) ->
                    val rRect = RectF(CONTENT_LEFT, y, CONTENT_RIGHT, y + rowH)
                    if (idx % 2 == 1) {
                        boxFillPaint.color = COLOR_ROW_ALT
                        canvas.drawRect(rRect, boxFillPaint)
                    }
                    canvas.drawRect(rRect, boxStrokePaint)

                    val prodCells = arrayOf(
                        "${fmtMoney(rev)} $cur",
                        "$qty وحدة",
                        "${idx + 1}. $prodName"
                    )
                    var cAccX = CONTENT_LEFT
                    for (i in prodColRatios.indices) {
                        val w = CONTENT_WIDTH * prodColRatios[i]
                        val p = if (i == 0 || i == 2) tableCellBoldPaint else tableCellPaint
                        p.color = if (i == 0) COLOR_BLUE_SALES else COLOR_SLATE_DARK
                        p.textAlign = Paint.Align.CENTER
                        canvas.drawText(prodCells[i], cAccX + w / 2f, y + 12.5f, p)
                        if (i > 0) {
                            canvas.drawLine(cAccX, y, cAccX, y + rowH, boxStrokePaint)
                        }
                        cAccX += w
                    }
                    y += rowH
                }
            }

            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawPrintBarAndTrendChart(
        canvas: AndroidCanvas,
        box: RectF,
        payload: SalesReportPdfPayload
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = AndroidColor.rgb(250, 252, 255)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = COLOR_BORDER
        }
        canvas.drawRoundRect(box, 8f, 8f, bgPaint)
        canvas.drawRoundRect(box, 8f, 8f, borderPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
            color = COLOR_NAVY_HEADER
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(
            "مخطط تطور المبيعات والأرباح (${payload.period.labelAr})",
            box.right - 10f,
            box.top + 15f,
            headerPaint
        )

        val primaryColorInt = if (payload.metricComparisonMode == SalesMetricComparisonMode.SALES_VS_PROFIT) {
            COLOR_BLUE_SALES
        } else {
            COLOR_GREEN_PROFIT
        }
        val secondaryColorInt = if (payload.metricComparisonMode == SalesMetricComparisonMode.SALES_VS_PROFIT) {
            COLOR_GREEN_PROFIT
        } else {
            COLOR_AMBER_DEBT
        }

        // Mini legend at top-left of chart box
        val legPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            color = COLOR_SLATE_DARK
            textAlign = Paint.Align.LEFT
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        dotPaint.color = primaryColorInt
        canvas.drawCircle(box.left + 12f, box.top + 12f, 3.5f, dotPaint)
        canvas.drawText(payload.metricComparisonMode.primaryLabelAr, box.left + 18f, box.top + 14.5f, legPaint)

        dotPaint.color = secondaryColorInt
        canvas.drawCircle(box.left + 92f, box.top + 12f, 3.5f, dotPaint)
        canvas.drawText(payload.metricComparisonMode.secondaryLabelAr, box.left + 98f, box.top + 14.5f, legPaint)

        val plotLeft = box.left + 12f
        val plotRight = box.right - 12f
        val plotTop = box.top + 26f
        val plotBottom = box.bottom - 20f
        val plotW = (plotRight - plotLeft).coerceAtLeast(10f)
        val plotH = (plotBottom - plotTop).coerceAtLeast(10f)

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
            color = AndroidColor.rgb(226, 232, 240)
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        for (i in 0..3) {
            val gy = plotTop + plotH * (i / 3f)
            canvas.drawLine(plotLeft, gy, plotRight, gy, gridPaint)
        }

        val buckets = payload.buckets
        if (buckets.isEmpty()) return

        val maxVal = (buckets.maxOfOrNull { b ->
            when (payload.metricComparisonMode) {
                SalesMetricComparisonMode.SALES_VS_PROFIT -> maxOf(b.totalSales, b.netProfit)
                SalesMetricComparisonMode.CASH_VS_DEBT -> maxOf(b.collectedSales, b.debtSales, b.totalSales)
            }
        } ?: 0.0).let { if (it <= 0.0) 100.0 else it * 1.15 }

        val slotW = plotW / buckets.size.toFloat()
        val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7f
            color = COLOR_SLATE_MUTED
            textAlign = Paint.Align.CENTER
        }

        val primaryPts = mutableListOf<Pair<Float, Float>>()
        val labelStep = when {
            buckets.size > 15 -> 5
            buckets.size > 8 -> 2
            else -> 1
        }

        buckets.forEachIndexed { idx, b ->
            val pVal = when (payload.metricComparisonMode) {
                SalesMetricComparisonMode.SALES_VS_PROFIT -> b.totalSales
                SalesMetricComparisonMode.CASH_VS_DEBT -> b.collectedSales
            }
            val sVal = when (payload.metricComparisonMode) {
                SalesMetricComparisonMode.SALES_VS_PROFIT -> b.netProfit
                SalesMetricComparisonMode.CASH_VS_DEBT -> b.debtSales
            }

            val pRatio = (pVal / maxVal).toFloat().coerceIn(0f, 1f)
            val sRatio = (sVal / maxVal).toFloat().coerceIn(0f, 1f)

            val cx = plotLeft + (idx + 0.5f) * slotW
            val py = plotBottom - pRatio * (plotH - 6f)
            val sy = plotBottom - sRatio * (plotH - 6f)
            primaryPts.add(cx to py)

            if (payload.chartDisplayMode != SalesChartDisplayMode.TREND_ONLY) {
                val groupW = (slotW * 0.66f).coerceAtMost(20f)
                val barW = (groupW * 0.45f).coerceAtLeast(1.8f)
                val gap = (groupW * 0.08f).coerceAtLeast(0.8f)

                val pTop = py.coerceAtMost(plotBottom - if (pVal > 0) 2.5f else 1f)
                val sTop = sy.coerceAtMost(plotBottom - if (sVal > 0) 2.5f else 1f)

                barFillPaint.color = primaryColorInt
                canvas.drawRoundRect(
                    RectF(cx - gap / 2f - barW, pTop, cx - gap / 2f, plotBottom),
                    2f,
                    2f,
                    barFillPaint
                )

                barFillPaint.color = secondaryColorInt
                canvas.drawRoundRect(
                    RectF(cx + gap / 2f, sTop, cx + gap / 2f + barW, plotBottom),
                    2f,
                    2f,
                    barFillPaint
                )
            }

            if (idx % labelStep == 0 || idx == buckets.lastIndex) {
                canvas.drawText(b.shortLabel, cx, box.bottom - 6f, axisLabelPaint)
            }
        }

        if (payload.chartDisplayMode != SalesChartDisplayMode.BAR_ONLY && primaryPts.size >= 2) {
            val path = AndroidPath().apply {
                moveTo(primaryPts.first().first, primaryPts.first().second)
                for (i in 0 until primaryPts.size - 1) {
                    val p0 = primaryPts[i]
                    val p1 = primaryPts[i + 1]
                    val ctrlX = (p0.first + p1.first) / 2f
                    cubicTo(ctrlX, p0.second, ctrlX, p1.second, p1.first, p1.second)
                }
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.8f
                strokeCap = Paint.Cap.ROUND
                color = if (payload.chartDisplayMode == SalesChartDisplayMode.BAR_AND_TREND) {
                    COLOR_AMBER_DEBT
                } else {
                    primaryColorInt
                }
            }
            canvas.drawPath(path, linePaint)

            val ptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = linePaint.color
            }
            primaryPts.forEach { (px, py) ->
                canvas.drawCircle(px, py, 2.2f, ptPaint)
            }
        }
    }

    private fun drawPrintPaymentDonutChart(
        canvas: AndroidCanvas,
        box: RectF,
        payload: SalesReportPdfPayload
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = AndroidColor.rgb(250, 252, 255)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = COLOR_BORDER
        }
        canvas.drawRoundRect(box, 8f, 8f, bgPaint)
        canvas.drawRoundRect(box, 8f, 8f, borderPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
            color = COLOR_NAVY_HEADER
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("توزيع طرق الدفع", box.right - 10f, box.top + 15f, headerPaint)

        val total = (payload.overview.cashSales + payload.overview.debtSales + payload.overview.transferSales)
            .coerceAtLeast(0.0)
        val divisor = if (total > 0.0) total else 1.0

        val slices = listOf(
            Triple("نقدي", payload.overview.cashSales, COLOR_GREEN_PROFIT),
            Triple("آجل (دين)", payload.overview.debtSales, COLOR_AMBER_DEBT),
            Triple("تحويل", payload.overview.transferSales, COLOR_PURPLE_TRANSFER)
        )

        val donutRadius = 34f
        val cx = box.left + 48f
        val cy = box.top + 92f
        val arcRect = RectF(cx - donutRadius, cy - donutRadius, cx + donutRadius, cy + donutRadius)

        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 13f
            strokeCap = Paint.Cap.BUTT
        }

        if (total <= 0.0) {
            arcPaint.color = COLOR_BORDER
            canvas.drawArc(arcRect, 0f, 360f, false, arcPaint)
        } else {
            var startAng = -90f
            slices.forEach { (_, amt, colorInt) ->
                val sweep = ((amt / divisor) * 360.0).toFloat()
                if (sweep > 0.5f) {
                    arcPaint.color = colorInt
                    canvas.drawArc(arcRect, startAng, sweep, false, arcPaint)
                    startAng += sweep
                }
            }
        }

        val itemTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 7.8f
            color = COLOR_NAVY_HEADER
            textAlign = Paint.Align.RIGHT
        }
        val itemSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.2f
            color = COLOR_SLATE_MUTED
            textAlign = Paint.Align.RIGHT
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        var ly = box.top + 48f
        slices.forEach { (label, amt, colorInt) ->
            val pct = (amt / divisor) * 100.0
            dotPaint.color = colorInt
            canvas.drawCircle(box.right - 12f, ly - 3f, 3.5f, dotPaint)
            canvas.drawText(
                "$label (${String.format(Locale.US, "%.0f", pct)}%)",
                box.right - 20f,
                ly,
                itemTitlePaint
            )
            canvas.drawText(
                "${fmtMoney(amt)} ${payload.currencySymbol}",
                box.right - 20f,
                ly + 11f,
                itemSubPaint
            )
            ly += 34f
        }
    }

    private fun fmtMoney(value: Double): String = String.format(Locale.US, "%.2f", value)

    fun generateCachePdfFile(context: Context, payload: SalesReportPdfPayload): File {
        val reportsDir = File(context.cacheDir, "reports_pdf")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val outFile = File(reportsDir, payload.suggestedFileName)
        FileOutputStream(outFile).use { fos ->
            writeReportPdfToStream(payload, fos)
        }
        return outFile
    }

    fun saveReportPdfToUri(context: Context, payload: SalesReportPdfPayload, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                writeReportPdfToStream(payload, out)
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareReportPdf(context: Context, payload: SalesReportPdfPayload) {
        try {
            val pdfFile = generateCachePdfFile(context, payload)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val summaryText = buildString {
                appendLine("📊 تقرير المبيعات والأرباح - ${payload.storeName}")
                appendLine("النطاق الزمني: ${payload.period.labelAr}")
                appendLine("إجمالي المبيعات: ${fmtMoney(payload.overview.totalSales)} ${payload.currencySymbol}")
                appendLine("صافي الربح: ${fmtMoney(payload.overview.netProfit)} ${payload.currencySymbol}")
                appendLine("النقدي والتحويل: ${fmtMoney(payload.overview.cashSales + payload.overview.transferSales)} ${payload.currencySymbol}")
                appendLine("المبيعات الآجلة (دين): ${fmtMoney(payload.overview.debtSales)} ${payload.currencySymbol}")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير المبيعات (${payload.period.labelAr}) - ${payload.storeName}")
                putExtra(Intent.EXTRA_TEXT, summaryText)
                clipData = ClipData.newRawUri(payload.suggestedFileName, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير المبيعات PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة ملف PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printReport(context: Context, payload: SalesReportPdfPayload) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "خدمة الطباعة غير متوفرة على هذا الجهاز", Toast.LENGTH_SHORT).show()
                return
            }

            val jobName = "تقرير_المبيعات_${payload.period.name}"
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
                    val info = PrintDocumentInfo.Builder(payload.suggestedFileName)
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
                            writeReportPdfToStream(payload, out)
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback.onWriteFailed(e.localizedMessage ?: "خطأ أثناء كتابة ملف الطباعة")
                    }
                }
            }

            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح نافذة الطباعة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SalesReportPdfPreviewDialog(
    payload: SalesReportPdfPayload,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }

    val savePdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        if (targetUri != null) {
            isWorking = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    SalesReportPdfExporter.saveReportPdfToUri(context, payload, targetUri)
                }
                isWorking = false
                Toast.makeText(
                    context,
                    if (ok) "تم حفظ ملف التقرير PDF بنجاح في الهاتف 📄" else "تعذر حفظ ملف PDF",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
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
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.90f)
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("sales_report_pdf_preview_dialog"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Top Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.16f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "معاينة وتصدير تقرير PDF الرسمي",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${payload.period.labelAr} • تنسيق A4 فاتح جاهز للطباعة والمشاركة",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            enabled = !isWorking,
                            modifier = Modifier.testTag("close_pdf_preview_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                }

                HorizontalDivider()

                // Scrollable A4 Official Light Paper Sheet Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE2E8F0))
                        .padding(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("pdf_a4_paper_preview_sheet")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 1. Official Navy Paper Header
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "تقرير المبيعات والأرباح (${payload.period.labelAr})",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFF38BDF8).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "A4 PDF",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = buildString {
                                            append("المتجر: ${payload.storeName}")
                                            if (payload.storePhone.isNotBlank()) append(" • هاتف: ${payload.storePhone}")
                                        },
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "تاريخ الإصدار: ${payload.formattedExportDate} • إجمالي العمليات: ${payload.overview.totalTransactions} عملية (${payload.overview.totalUnitsSold} قطعة)",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.5.sp
                                    )
                                }
                            }

                            // 2. Executive Financial Summary Grid on White Paper
                            Text(
                                text = "أولاً: الملخص المالي التنفيذي",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )

                            val cur = payload.currencySymbol
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PrintPaperKpiBox(
                                    label = "إجمالي المبيعات",
                                    value = "${String.format(Locale.US, "%.2f", payload.overview.totalSales)} $cur",
                                    accentColor = Color(0xFF0284C7),
                                    modifier = Modifier.weight(1f)
                                )
                                PrintPaperKpiBox(
                                    label = "صافي الربح التقديري",
                                    value = "${String.format(Locale.US, "%.2f", payload.overview.netProfit)} $cur",
                                    accentColor = Color(0xFF059669),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PrintPaperKpiBox(
                                    label = "النقدي والتحويل المحصّل",
                                    value = "${String.format(Locale.US, "%.2f", payload.overview.cashSales + payload.overview.transferSales)} $cur",
                                    accentColor = Color(0xFF059669),
                                    modifier = Modifier.weight(1f)
                                )
                                PrintPaperKpiBox(
                                    label = "المبيعات الآجلة (الديون)",
                                    value = "${String.format(Locale.US, "%.2f", payload.overview.debtSales)} $cur",
                                    accentColor = Color(0xFFD97706),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 3. Light Print-Friendly Bar & Trend Chart Preview
                            Text(
                                text = "ثانياً: الرسوم البيانية المضمنة في التقرير (${payload.chartDisplayMode.labelAr})",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )

                            PrintLightBarAndTrendChartPreview(payload = payload)

                            // 4. Light Print-Friendly Payment Methods Breakdown Preview
                            PrintLightPaymentDonutPreview(payload = payload)

                            // 5. Time-Series Table Preview
                            val periodColTitle = if (payload.period.isMonthly) "الشهر" else "اليوم / التاريخ"
                            Text(
                                text = "ثالثاً: جدول تفصيل المبيعات والأرباح حسب $periodColTitle (${payload.buckets.size} فترة)",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Table Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF1F5F9))
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = periodColTitle,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        Text(
                                            text = "العمليات",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF0F172A),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(0.8f)
                                        )
                                        Text(
                                            text = "النقدي",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF0F172A),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "الآجل",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF0F172A),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "الربح",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF059669),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "الإجمالي",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF0284C7),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1.1f)
                                        )
                                    }

                                    payload.buckets.forEachIndexed { idx, b ->
                                        HorizontalDivider(color = Color(0xFFE2E8F0))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = b.shortLabel,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF1E293B),
                                                modifier = Modifier.weight(1.2f)
                                            )
                                            Text(
                                                text = "${b.transactionsCount}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF475569),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(0.8f)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.0f", b.collectedSales),
                                                fontSize = 10.sp,
                                                color = Color(0xFF1E293B),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.0f", b.debtSales),
                                                fontSize = 10.sp,
                                                color = if (b.debtSales > 0) Color(0xFFD97706) else Color(0xFF64748B),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.0f", b.netProfit),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF059669),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%.0f", b.totalSales)} $cur",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7),
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.weight(1.1f)
                                            )
                                        }
                                    }
                                }
                            }

                            // 6. Top Selling Products Table Preview
                            if (payload.topSellingProducts.isNotEmpty()) {
                                Text(
                                    text = "رابعاً: أعلى الأصناف مبيعاً وإيراداً",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        payload.topSellingProducts.forEachIndexed { idx, (name, qty, rev) ->
                                            if (idx > 0) HorizontalDivider(color = Color(0xFFE2E8F0))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${idx + 1}. $name",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF0F172A),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "$qty وحدة",
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF475569),
                                                    modifier = Modifier.padding(horizontal = 10.dp)
                                                )
                                                Text(
                                                    text = "${String.format(Locale.US, "%.2f", rev)} $cur",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF0284C7)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Bottom Action Bar: Print | Share PDF | Save to Phone (Raised up from bottom)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isWorking) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري تجهيز ملف التقرير PDF...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Print Report directly via Android PrintManager
                            Button(
                                onClick = {
                                    SalesReportPdfExporter.printReport(context, payload)
                                },
                                enabled = !isWorking,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pdf_action_print_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("طباعة", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }

                            // 2. Share PDF via FileProvider
                            Button(
                                onClick = {
                                    isWorking = true
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            SalesReportPdfExporter.generateCachePdfFile(context, payload)
                                        }
                                        isWorking = false
                                        SalesReportPdfExporter.shareReportPdf(context, payload)
                                    }
                                },
                                enabled = !isWorking,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pdf_action_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("مشاركة", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }

                            // 3. Save PDF to Device Storage
                            OutlinedButton(
                                onClick = {
                                    savePdfDocumentLauncher.launch(payload.suggestedFileName)
                                },
                                enabled = !isWorking,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pdf_action_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("حفظ", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
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
private fun PrintPaperKpiBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PrintLightBarAndTrendChartPreview(payload: SalesReportPdfPayload) {
    val buckets = payload.buckets
    val primaryColor = if (payload.metricComparisonMode == SalesMetricComparisonMode.SALES_VS_PROFIT) {
        Color(0xFF0284C7)
    } else {
        Color(0xFF059669)
    }
    val secondaryColor = if (payload.metricComparisonMode == SalesMetricComparisonMode.SALES_VS_PROFIT) {
        Color(0xFF059669)
    } else {
        Color(0xFFD97706)
    }

    val maxVal = remember(buckets, payload.metricComparisonMode) {
        val raw = buckets.maxOfOrNull { b ->
            when (payload.metricComparisonMode) {
                SalesMetricComparisonMode.SALES_VS_PROFIT -> maxOf(b.totalSales, b.netProfit)
                SalesMetricComparisonMode.CASH_VS_DEBT -> maxOf(b.collectedSales, b.debtSales, b.totalSales)
            }
        } ?: 0.0
        if (raw <= 0.0) 100.0 else raw * 1.15
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFAFBFD),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
            .fillMaxWidth()
            .height(175.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مخطط ${payload.metricComparisonMode.labelAr}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF0F172A)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(primaryColor))
                        Text(payload.metricComparisonMode.primaryLabelAr, fontSize = 10.sp, color = Color(0xFF334155))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(secondaryColor))
                        Text(payload.metricComparisonMode.secondaryLabelAr, fontSize = 10.sp, color = Color(0xFF334155))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val count = buckets.size.coerceAtLeast(1)
                    val slotW = w / count.toFloat()

                    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 0..3) {
                        val y = h * (i / 3f)
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (i < 3) dash else null
                        )
                    }

                    val pts = mutableListOf<Offset>()
                    buckets.forEachIndexed { idx, b ->
                        val pVal = when (payload.metricComparisonMode) {
                            SalesMetricComparisonMode.SALES_VS_PROFIT -> b.totalSales
                            SalesMetricComparisonMode.CASH_VS_DEBT -> b.collectedSales
                        }
                        val sVal = when (payload.metricComparisonMode) {
                            SalesMetricComparisonMode.SALES_VS_PROFIT -> b.netProfit
                            SalesMetricComparisonMode.CASH_VS_DEBT -> b.debtSales
                        }

                        val pRatio = (pVal / maxVal).toFloat().coerceIn(0f, 1f)
                        val sRatio = (sVal / maxVal).toFloat().coerceIn(0f, 1f)

                        val cx = (idx + 0.5f) * slotW
                        val py = h - pRatio * (h - 6.dp.toPx())
                        val sy = h - sRatio * (h - 6.dp.toPx())
                        pts.add(Offset(cx, py))

                        if (payload.chartDisplayMode != SalesChartDisplayMode.TREND_ONLY) {
                            val groupW = (slotW * 0.65f).coerceAtMost(28.dp.toPx())
                            val barW = (groupW * 0.45f).coerceAtLeast(2.5.dp.toPx())
                            val gap = (groupW * 0.08f).coerceAtLeast(1.dp.toPx())

                            val ph = (h - py).coerceAtLeast(if (pVal > 0) 3.dp.toPx() else 1.dp.toPx())
                            val sh = (h - sy).coerceAtLeast(if (sVal > 0) 3.dp.toPx() else 1.dp.toPx())

                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(cx - gap / 2f - barW, h - ph),
                                size = Size(barW, ph),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                            drawRoundRect(
                                color = secondaryColor,
                                topLeft = Offset(cx + gap / 2f, h - sh),
                                size = Size(barW, sh),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }

                    if (payload.chartDisplayMode != SalesChartDisplayMode.BAR_ONLY && pts.size >= 2) {
                        val path = Path().apply {
                            moveTo(pts.first().x, pts.first().y)
                            for (i in 0 until pts.size - 1) {
                                val p0 = pts[i]
                                val p1 = pts[i + 1]
                                val ctrlX = (p0.x + p1.x) / 2f
                                cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = if (payload.chartDisplayMode == SalesChartDisplayMode.BAR_AND_TREND) {
                                Color(0xFFD97706)
                            } else {
                                primaryColor
                            },
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintLightPaymentDonutPreview(payload: SalesReportPdfPayload) {
    val total = (payload.overview.cashSales + payload.overview.debtSales + payload.overview.transferSales)
        .coerceAtLeast(0.0)
    val divisor = if (total > 0.0) total else 1.0
    val cur = payload.currencySymbol

    val items = listOf(
        Triple("نقدي (كاش)", payload.overview.cashSales, Color(0xFF059669)),
        Triple("آجل (ديون)", payload.overview.debtSales, Color(0xFFD97706)),
        Triple("تحويل بنكي", payload.overview.transferSales, Color(0xFF7C3AED))
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFAFBFD),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(82.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 13.dp.toPx()
                    val d = size.minDimension - strokeW
                    val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                    if (total <= 0.0) {
                        drawArc(
                            color = Color(0xFFCBD5E1),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = tl,
                            size = Size(d, d),
                            style = Stroke(width = strokeW)
                        )
                    } else {
                        var startAng = -90f
                        items.forEach { (_, amt, col) ->
                            val sweep = ((amt / divisor) * 360.0).toFloat()
                            if (sweep > 0.5f) {
                                drawArc(
                                    color = col,
                                    startAngle = startAng,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = tl,
                                    size = Size(d, d),
                                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                                )
                                startAng += sweep
                            }
                        }
                    }
                }
                Text(
                    text = "الدفع",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "توزيع المبيعات حسب طرق الدفع",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = Color(0xFF0F172A)
                )
                items.forEach { (label, amt, col) ->
                    val pct = (amt / divisor) * 100.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(col))
                            Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        }
                        Text(
                            text = "${String.format(Locale.US, "%.2f", amt)} $cur (${String.format(Locale.US, "%.0f", pct)}%)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = col
                        )
                    }
                }
            }
        }
    }
}
