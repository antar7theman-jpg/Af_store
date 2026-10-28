package com.example.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.utils.CustomerLocationHelper
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.models.formatPackagingLabel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class InvoicePaperSize(val labelAr: String, val widthPt: Int, val description: String) {
    A4("A4 (صفحة قياسية)", 595, "مناسب للطباعة الرسمية والمناقصات بجدول كاملاً"),
    THERMAL_80MM("80mm (إيصال حراري)", 226, "مناسب لطابعات الكاشير الإيصالات السريعة")
}

data class InvoicePdfPayload(
    val storeName: String,
    val storePhone: String,
    val customerName: String,
    val cashierName: String,
    val invoiceNumber: String,
    val formattedDateTime: String,
    val paymentMethodAr: String,
    val lines: List<PreviewLineItem>,
    val sousTotal: Double,
    val discountAmount: Double,
    val discountPercentInt: Int,
    val tvaAmount: Double,
    val netTotal: Double,
    val hasDebtItems: Boolean,
    val creditAmount: Double,
    val especesAmount: Double,
    val currency: String,
    val footerText: String,
    val developerContact: String,
    val storeLogoImagePath: String = "",
    val storeLogoCustomText: String = "",
    val previousDebt: Double = 0.0,
    val paperSize: InvoicePaperSize = InvoicePaperSize.A4,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val customerLocationUrl: String = ""
) {
    val customerLocationQrBitmap: Bitmap?
        get() = if (customerLocationUrl.isNotBlank()) CustomerLocationHelper.generateLocationQrBitmap(customerLocationUrl, 160) else null
    val suggestedFileName: String
        get() {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(generatedAtMillis))
            val cleanNum = invoiceNumber.replace("#", "").replace(" ", "_")
            return "Invoice_${cleanNum}_$stamp.pdf"
        }

    val totalPiecesCount: Int
        get() = lines.sumOf { it.quantityPieces }

    val totalCartonsCount: Int
        get() = lines.sumOf { it.fullCartons }

    val qrContentString: String
        get() {
            val invCode = if (invoiceNumber.startsWith("#") || invoiceNumber.startsWith("Pre")) invoiceNumber else "#$invoiceNumber"
            return "INVOICE|$invCode|$storeName|$formattedDateTime|Total:${String.format(Locale.US, "%.2f", netTotal)}$currency|Paid:${String.format(Locale.US, "%.2f", especesAmount)}|Debt:${String.format(Locale.US, "%.2f", creditAmount)}"
        }
}

object InvoicePdfExporter {

    fun generateQrCodeBitmap(content: String, sizePx: Int = 180): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(
                        x,
                        y,
                        if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
                    )
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateInvoicePdfDocument(payload: InvoicePdfPayload): PdfDocument {
        val pdfDocument = PdfDocument()
        val isA4 = payload.paperSize == InvoicePaperSize.A4
        val pageWidth = if (isA4) 595 else 226
        val margin = if (isA4) 30f else 10f
        val contentLeft = margin
        val contentRight = pageWidth - margin
        val contentWidth = contentRight - contentLeft

        val lineCount = payload.lines.size.coerceAtLeast(1)
        val estimatedHeight = if (isA4) {
            (500 + lineCount * 38).coerceAtLeast(842)
        } else {
            (460 + lineCount * 65).coerceAtLeast(550)
        }

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, estimatedHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = if (isA4) 18f else 13f
            color = AndroidColor.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = if (isA4) 10.5f else 8.5f
            color = AndroidColor.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val headerBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = if (isA4) 10.5f else 8.5f
            color = AndroidColor.rgb(15, 23, 42)
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = if (isA4) 10f else 8f
            color = AndroidColor.WHITE
        }
        val tableRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = if (isA4) 9.5f else 8f
            color = AndroidColor.rgb(15, 23, 42)
        }
        val tableRowBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = if (isA4) 9.5f else 8f
            color = AndroidColor.rgb(15, 23, 42)
        }

        val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val strokeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = AndroidColor.rgb(203, 213, 225)
        }
        val dashLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = AndroidColor.rgb(148, 163, 184)
            pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
        }

        var currentY = margin + 10f

        // --- 1. STORE HEADER & LOGO ---
        if (payload.storeLogoCustomText.isNotBlank()) {
            boxFillPaint.color = AndroidColor.rgb(15, 23, 42)
            val rectWidth = if (isA4) 180f else 120f
            val rectHeight = if (isA4) 28f else 22f
            canvas.drawRoundRect(
                pageWidth / 2f - rectWidth / 2f,
                currentY,
                pageWidth / 2f + rectWidth / 2f,
                currentY + rectHeight,
                6f,
                6f,
                boxFillPaint
            )
            val logoTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textSize = if (isA4) 12f else 9.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(payload.storeLogoCustomText.take(20), pageWidth / 2f, currentY + rectHeight - 7f, logoTextPaint)
            currentY += rectHeight + 10f
        }

        canvas.drawText(payload.storeName.ifBlank { "محل المبيعات" }, pageWidth / 2f, currentY, titlePaint)
        currentY += if (isA4) 18f else 14f

        if (payload.storePhone.isNotBlank()) {
            canvas.drawText("الهاتف: ${payload.storePhone}", pageWidth / 2f, currentY, subtitlePaint)
            currentY += if (isA4) 16f else 13f
        }

        currentY += 6f
        canvas.drawLine(contentLeft, currentY, contentRight, currentY, strokeLinePaint)
        currentY += 14f

        // --- 2. INVOICE META & CUSTOMER DETAILS ---
        val hasCustomerQr = payload.customerLocationQrBitmap != null
        val metaBoxHeight = if (isA4) (if (hasCustomerQr) 58f else 42f) else (if (hasCustomerQr) 78f else 54f)
        boxFillPaint.color = AndroidColor.rgb(248, 250, 252)
        canvas.drawRoundRect(contentLeft, currentY, contentRight, currentY + metaBoxHeight, 8f, 8f, boxFillPaint)
        canvas.drawRoundRect(contentLeft, currentY, contentRight, currentY + metaBoxHeight, 8f, 8f, strokeLinePaint)

        val invDisplayNumber = if (payload.invoiceNumber.startsWith("#") || payload.invoiceNumber.startsWith("Pre")) payload.invoiceNumber else "#${payload.invoiceNumber}"
        if (isA4) {
            headerBoldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("رقم الفاتورة: $invDisplayNumber", contentRight - 12f, currentY + 16f, headerBoldPaint)
            canvas.drawText("التاريخ والوقت: ${payload.formattedDateTime}", contentRight - 12f, currentY + 32f, headerBoldPaint)

            headerBoldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("الزبون: ${payload.customerName}", contentLeft + 12f, currentY + 16f, headerBoldPaint)
            canvas.drawText("الكاشير/البائع: ${payload.cashierName}", contentLeft + 12f, currentY + 32f, headerBoldPaint)

            payload.customerLocationQrBitmap?.let { qrBmp ->
                val qrSize = 44f
                val qrLeft = contentLeft + 165f
                val qrDst = android.graphics.RectF(qrLeft, currentY + 3f, qrLeft + qrSize, currentY + 3f + qrSize)
                canvas.drawBitmap(qrBmp, null, qrDst, null)
                val qrLabelPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 6.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = AndroidColor.rgb(15, 23, 42)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("📍 موقع العميل (Maps)", qrLeft + (qrSize / 2f), currentY + 3f + qrSize + 8f, qrLabelPaint)
            }
        } else {
            headerBoldPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("فاتورة رقم: $invDisplayNumber • ${payload.formattedDateTime}", pageWidth / 2f, currentY + 16f, headerBoldPaint)
            canvas.drawText("الزبون: ${payload.customerName} | البائع: ${payload.cashierName}", pageWidth / 2f, currentY + 32f, headerBoldPaint)
            canvas.drawText("طريقة الدفع: ${payload.paymentMethodAr}", pageWidth / 2f, currentY + 46f, headerBoldPaint)
            payload.customerLocationQrBitmap?.let { qrBmp ->
                val qrSize = 44f
                val qrLeft = (pageWidth - qrSize) / 2f
                val qrDst = android.graphics.RectF(qrLeft, currentY + 50f, qrLeft + qrSize, currentY + 50f + qrSize)
                canvas.drawBitmap(qrBmp, null, qrDst, null)
                val qrLabelPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 5.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = AndroidColor.rgb(15, 23, 42)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("📍 موقع العميل (Google Maps)", pageWidth / 2f, currentY + 50f + qrSize + 7f, qrLabelPaint)
            }
        }

        currentY += metaBoxHeight + 14f

        // --- 3. ITEMS TABLE ---
        val tableHeaderBgHeight = if (isA4) 22f else 18f
        boxFillPaint.color = AndroidColor.rgb(15, 23, 42)
        canvas.drawRect(contentLeft, currentY, contentRight, currentY + tableHeaderBgHeight, boxFillPaint)

        if (isA4) {
            tableHeaderPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("اسم المنتج والعبوة", contentRight - 10f, currentY + 15f, tableHeaderPaint)

            tableHeaderPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("الكمية", contentLeft + 220f, currentY + 15f, tableHeaderPaint)
            canvas.drawText("سعر الوحدة", contentLeft + 130f, currentY + 15f, tableHeaderPaint)

            tableHeaderPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("الإجمالي الصافي", contentLeft + 10f, currentY + 15f, tableHeaderPaint)
        } else {
            tableHeaderPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("الصنف / العبوة", contentRight - 6f, currentY + 13f, tableHeaderPaint)
            tableHeaderPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("الكمية", contentLeft + 100f, currentY + 13f, tableHeaderPaint)
            tableHeaderPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("المبلغ", contentLeft + 6f, currentY + 13f, tableHeaderPaint)
        }

        currentY += tableHeaderBgHeight

        payload.lines.forEachIndexed { idx, line ->
            val rowHeight = if (isA4) 28f else 40f
            boxFillPaint.color = if (idx % 2 == 0) AndroidColor.WHITE else AndroidColor.rgb(248, 250, 252)
            canvas.drawRect(contentLeft, currentY, contentRight, currentY + rowHeight, boxFillPaint)
            canvas.drawRect(contentLeft, currentY, contentRight, currentY + rowHeight, strokeLinePaint)

            val packLabel = formatPackagingLabel(line.quantityPieces, line.safeUnitsPerCarton)
            val debtTag = if (line.isDebt) " (دين)" else ""

            if (isA4) {
                tableRowBoldPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("${line.name}$debtTag", contentRight - 10f, currentY + 18f, tableRowBoldPaint)

                tableRowPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("$packLabel (${line.quantityPieces} قطعة)", contentLeft + 220f, currentY + 18f, tableRowPaint)
                canvas.drawText(String.format(Locale.US, "%.2f %s", line.unitPrice, payload.currency), contentLeft + 130f, currentY + 18f, tableRowPaint)

                tableRowBoldPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(String.format(Locale.US, "%.2f %s", line.finalLineTotal, payload.currency), contentLeft + 10f, currentY + 18f, tableRowBoldPaint)
            } else {
                tableRowBoldPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("${line.name}$debtTag", contentRight - 6f, currentY + 15f, tableRowBoldPaint)

                tableRowPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("$packLabel (${line.quantityPieces}ق)", contentRight - 6f, currentY + 30f, tableRowPaint)

                tableRowBoldPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(String.format(Locale.US, "%.2f", line.finalLineTotal), contentLeft + 6f, currentY + 22f, tableRowBoldPaint)
            }

            currentY += rowHeight
        }

        currentY += 10f

        // --- 4. SUMMARY & QR CODE SECTION ---
        val summaryBoxHeight = if (isA4) 120f else 140f
        boxFillPaint.color = AndroidColor.rgb(241, 245, 249)
        canvas.drawRoundRect(contentLeft, currentY, contentRight, currentY + summaryBoxHeight, 10f, 10f, boxFillPaint)
        canvas.drawRoundRect(contentLeft, currentY, contentRight, currentY + summaryBoxHeight, 10f, 10f, strokeLinePaint)

        // Draw Digital QR Code Bitmap inside the summary box
        val qrBitmap = generateQrCodeBitmap(payload.qrContentString, if (isA4) 100 else 80)
        if (qrBitmap != null) {
            val qrSize = if (isA4) 95f else 75f
            val qrX = contentLeft + 12f
            val qrY = currentY + (summaryBoxHeight - qrSize) / 2f
            canvas.drawBitmap(qrBitmap, null, RectF(qrX, qrY, qrX + qrSize, qrY + qrSize), null)
        }

        val textStartX = if (isA4) contentLeft + 120f else contentLeft + 90f

        tableRowPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            "إجمالي الاصناف والكمية: ${payload.lines.size} صنف (${payload.totalPiecesCount} قطعة) [${payload.totalCartonsCount} كرتونة]",
            contentRight - 12f,
            currentY + 20f,
            tableRowPaint
        )

        var summaryY = currentY + 38f
        canvas.drawText(
            "المجموع الفرعي (Sous-total): ${String.format(Locale.US, "%.2f %s", payload.sousTotal, payload.currency)}",
            contentRight - 12f,
            summaryY,
            tableRowPaint
        )

        if (payload.discountAmount > 0.0) {
            summaryY += 16f
            val discountPaint = Paint(tableRowPaint).apply { color = AndroidColor.rgb(220, 38, 38) }
            canvas.drawText(
                "الخصم المطبق (${payload.discountPercentInt}%): -${String.format(Locale.US, "%.2f %s", payload.discountAmount, payload.currency)}",
                contentRight - 12f,
                summaryY,
                discountPaint
            )
        }

        if (payload.tvaAmount > 0.0) {
            summaryY += 16f
            canvas.drawText(
                "ضريبة القيمة المضافة (TVA): +${String.format(Locale.US, "%.2f %s", payload.tvaAmount, payload.currency)}",
                contentRight - 12f,
                summaryY,
                tableRowPaint
            )
        }

        summaryY += 22f
        val netTotalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = if (isA4) 14f else 11.5f
            color = AndroidColor.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(
            "المجموع الصافي (Total Net): ${String.format(Locale.US, "%.2f %s", payload.netTotal, payload.currency)}",
            contentRight - 12f,
            summaryY,
            netTotalPaint
        )

        currentY += summaryBoxHeight + 14f

        // --- 5. PAYMENT & DEBT BREAKDOWN ---
        if (payload.hasDebtItems || payload.creditAmount > 0.0 || payload.previousDebt > 0.0) {
            boxFillPaint.color = AndroidColor.rgb(254, 242, 242)
            val debtBoxHeight = if (isA4) 38f else 46f
            canvas.drawRoundRect(contentLeft, currentY, contentRight, currentY + debtBoxHeight, 6f, 6f, boxFillPaint)

            val debtStrokePaint = Paint(strokeLinePaint).apply { color = AndroidColor.rgb(252, 165, 165) }
            canvas.drawRoundRect(contentLeft, currentY, contentRight, currentY + debtBoxHeight, 6f, 6f, debtStrokePaint)

            val debtTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = if (isA4) 10f else 8.5f
                color = AndroidColor.rgb(153, 27, 27)
                textAlign = Paint.Align.CENTER
            }

            val newDebtTotal = payload.previousDebt + payload.creditAmount
            canvas.drawText(
                "تفاصيل الدين: المدفوع نقداً ${String.format(Locale.US, "%.2f", payload.especesAmount)} | الآجل الحالي ${String.format(Locale.US, "%.2f", payload.creditAmount)}",
                pageWidth / 2f,
                currentY + 16f,
                debtTextPaint
            )
            canvas.drawText(
                "سابقة الدين للعميل: ${String.format(Locale.US, "%.2f", payload.previousDebt)} ${payload.currency} -- الإجمالي الجديد: ${String.format(Locale.US, "%.2f", newDebtTotal)} ${payload.currency}",
                pageWidth / 2f,
                currentY + 32f,
                debtTextPaint
            )

            currentY += debtBoxHeight + 12f
        }

        // --- 6. FOOTER & DEV CONTACT ---
        canvas.drawLine(contentLeft, currentY, contentRight, currentY, dashLinePaint)
        currentY += 14f

        if (payload.footerText.isNotBlank()) {
            subtitlePaint.textAlign = Paint.Align.CENTER
            canvas.drawText(payload.footerText, pageWidth / 2f, currentY, subtitlePaint)
            currentY += 14f
        }

        val devContactStr = if (payload.developerContact.isNotBlank()) "برمجة وتطوير: ${payload.developerContact}" else "نظام إدارة المبيعات والمخزون • الفاتورة الرقمية الرسمية"
        val devPaint = Paint(subtitlePaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = AndroidColor.rgb(100, 116, 139)
        }
        canvas.drawText(devContactStr, pageWidth / 2f, currentY, devPaint)

        pdfDocument.finishPage(page)
        return pdfDocument
    }

    fun writeInvoicePdfToStream(payload: InvoicePdfPayload, outputStream: OutputStream) {
        val pdfDocument = generateInvoicePdfDocument(payload)
        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    fun generateCachePdfFile(context: Context, payload: InvoicePdfPayload): File {
        val pdfDir = File(context.cacheDir, "invoices_pdf")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val pdfFile = File(pdfDir, payload.suggestedFileName)
        FileOutputStream(pdfFile).use { out ->
            writeInvoicePdfToStream(payload, out)
        }
        return pdfFile
    }

    fun shareInvoicePdf(context: Context, payload: InvoicePdfPayload) {
        try {
            val file = generateCachePdfFile(context, payload)
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val invCode = if (payload.invoiceNumber.startsWith("#") || payload.invoiceNumber.startsWith("Pre")) payload.invoiceNumber else "#${payload.invoiceNumber}"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "فاتورة مبيعات - $invCode")
                putExtra(Intent.EXTRA_TEXT, "مرفق فاتورة مبيعات رقم $invCode من ${payload.storeName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("فاتورة PDF", fileUri)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة فاتورة PDF عبر"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة ملف الفاتورة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printInvoicePdf(context: Context, payload: InvoicePdfPayload) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "خدمة الطباعة غير متوفرة في هذا الجهاز", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "Invoice_${payload.invoiceNumber}"
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(destination).use { out ->
                        writeInvoicePdfToStream(payload, out)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.localizedMessage)
                }
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    fun saveInvoicePdfToUri(context: Context, payload: InvoicePdfPayload, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                writeInvoicePdfToStream(payload, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

@Composable
fun InvoicePdfPreviewDialog(
    payload: InvoicePdfPayload,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPaperSize by remember { mutableStateOf(payload.paperSize) }

    val activePayload = remember(payload, selectedPaperSize) {
        payload.copy(paperSize = selectedPaperSize)
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val success = InvoicePdfExporter.saveInvoicePdfToUri(context, activePayload, uri)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "تم حفظ فاتورة PDF بنجاح 📄", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "فشل حفظ ملف PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .fillMaxHeight(0.94f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Top Bar Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "معاينة فاتورة PDF الرسمية",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                val invCode = if (payload.invoiceNumber.startsWith("#") || payload.invoiceNumber.startsWith("Pre")) payload.invoiceNumber else "#${payload.invoiceNumber}"
                                Text(
                                    text = "رقم: $invCode • ${payload.formattedDateTime}",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("close_pdf_preview_dialog")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Paper Size Switcher Bar
                    Surface(
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مقاس الورق:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )

                            InvoicePaperSize.entries.forEach { pSize ->
                                val selected = selectedPaperSize == pSize
                                Surface(
                                    onClick = { selectedPaperSize = pSize },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) Color(0xFF0F172A) else Color.White,
                                    border = BorderStroke(1.dp, if (selected) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = pSize.labelAr,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 11.5.sp,
                                            color = if (selected) Color.White else Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Scrollable PDF Invoice Visual Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFFE2E8F0))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            InvoicePdfVisualPreviewCard(payload = activePayload)
                        }
                    }

                    // Bottom Action Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { InvoicePdfExporter.shareInvoicePdf(context, activePayload) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_pdf_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة PDF", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }

                            Button(
                                onClick = { InvoicePdfExporter.printInvoicePdf(context, activePayload) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("print_pdf_button")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }

                            OutlinedButton(
                                onClick = { savePdfLauncher.launch(activePayload.suggestedFileName) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF475569)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_pdf_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF334155))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoicePdfVisualPreviewCard(payload: InvoicePdfPayload) {
    val isA4 = payload.paperSize == InvoicePaperSize.A4
    val cardWidthDp = if (isA4) 340.dp else 260.dp

    val qrBitmap = remember(payload.qrContentString) {
        InvoicePdfExporter.generateQrCodeBitmap(payload.qrContentString, 160)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier
            .width(cardWidthDp)
            .padding(vertical = 8.dp)
            .testTag("pdf_visual_preview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isA4) 16.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo/Text
            if (payload.storeLogoCustomText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = payload.storeLogoCustomText.take(20),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = payload.storeName.ifBlank { "محل المبيعات" },
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isA4) 16.sp else 14.sp,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            if (payload.storePhone.isNotBlank()) {
                Text(
                    text = "هاتف: ${payload.storePhone}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

            // Meta Info Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val invDisplay = if (payload.invoiceNumber.startsWith("#") || payload.invoiceNumber.startsWith("Pre")) payload.invoiceNumber else "#${payload.invoiceNumber}"
                        Text("رقم الفاتورة: $invDisplay", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                        Text(payload.formattedDateTime, fontSize = 10.5.sp, color = Color(0xFF64748B))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الزبون: ${payload.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF1E293B))
                        Text("البائع: ${payload.cashierName}", fontSize = 10.5.sp, color = Color(0xFF475569))
                    }

                    payload.customerLocationQrBitmap?.let { qrBmp ->
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Image(
                                bitmap = qrBmp.asImageBitmap(),
                                contentDescription = "موقع العميل",
                                modifier = Modifier.size(76.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📍", fontSize = 13.sp)
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        "موقع العميل على خرائط Google",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    "امسح بكاميرا الهاتف للوصول للعنوان فوراً",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Table Header Bar
            Surface(
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الصنف / العبوة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    Text("الكمية والسعر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    Text("المبلغ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                }
            }

            // Items Rows
            payload.lines.forEachIndexed { idx, line ->
                val bg = if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC)
                val packLabel = formatPackagingLabel(line.quantityPieces, line.safeUnitsPerCarton)

                Surface(
                    color = bg,
                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = line.name + if (line.isDebt) " (دين)" else "",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "$packLabel (${line.quantityPieces} قطعة)",
                                fontSize = 9.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%.2f", line.unitPrice),
                            fontSize = 10.sp,
                            color = Color(0xFF334155),
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = String.format(Locale.US, "%.2f %s", line.finalLineTotal, payload.currency),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Summary & QR Code Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Render QR Code
                    if (qrBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(3.dp)) {
                                androidx.compose.foundation.Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "رمز QR الرقمي للفاتورة",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "المجموع الفرعي: ${String.format(Locale.US, "%.2f %s", payload.sousTotal, payload.currency)}",
                            fontSize = 10.5.sp,
                            color = Color(0xFF334155)
                        )
                        if (payload.discountAmount > 0.0) {
                            Text(
                                text = "الخصم (${payload.discountPercentInt}%): -${String.format(Locale.US, "%.2f %s", payload.discountAmount, payload.currency)}",
                                fontSize = 10.5.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (payload.tvaAmount > 0.0) {
                            Text(
                                text = "الضريبة TVA: +${String.format(Locale.US, "%.2f %s", payload.tvaAmount, payload.currency)}",
                                fontSize = 10.5.sp,
                                color = Color(0xFF0284C7)
                            )
                        }
                        Text(
                            text = "المجموع الصافي: ${String.format(Locale.US, "%.2f %s", payload.netTotal, payload.currency)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            if (payload.hasDebtItems || payload.creditAmount > 0.0 || payload.previousDebt > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "نقداً: ${String.format(Locale.US, "%.2f", payload.especesAmount)} | الآجل: ${String.format(Locale.US, "%.2f", payload.creditAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (payload.developerContact.isNotBlank()) "تطوير: ${payload.developerContact}" else "نظام المبيعات والمخزون • الفاتورة الرقمية",
                fontSize = 9.5.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}
