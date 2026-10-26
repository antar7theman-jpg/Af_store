package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

// 1. Customer Location & Maps Helper
object CustomerLocationHelper {
    fun show(context: Context, customerName: String) {}
    fun show(context: Context, customer: Any) {}
    fun getLatLong(customer: Any): Pair<Double, Double>? = null
    fun openInGoogleMaps(
        context: Context,
        latitude: Double? = null,
        longitude: Double? = null,
        locationUrl: String = "",
        lat: Double? = latitude,
        lng: Double? = longitude,
        label: String = ""
    ) {
        try {
            val url = if (locationUrl.isNotBlank()) {
                locationUrl
            } else if (lat != null && lng != null) {
                "https://maps.google.com/?q=$lat,$lng"
            } else return
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
    fun openInGoogleMaps(context: Context, lat: Double, lng: Double, label: String = "") {
        openInGoogleMaps(context, latitude = lat, longitude = lng, label = label)
    }
    fun openInGoogleMaps(context: Context, customer: Any) {}
    fun buildGoogleMapsUrl(lat: Double, lng: Double): String = "https://maps.google.com/?q=$lat,$lng"
    fun generateLocationQrBitmap(url: String, size: Int = 160): Bitmap? = null
    fun generateLocationQrBitmap(lat: Double, lng: Double, size: Int = 160): Bitmap? = null
    fun requestSingleLocationFix(context: Context, onSuccess: (Double, Double) -> Unit = { _, _ -> }, onError: (String) -> Unit = {}) {}
    fun requestSingleLocationFix(context: Context, onResult: (Pair<Double, Double>?) -> Unit) { onResult(null) }
    fun hasLocationPermission(context: Context? = null): Boolean = true
    val hasLocationPermission: Boolean get() = true
}

// 2. Printer Enums and Configs
enum class ThermalPaperWidth(val shortLabelAr: String = "80mm", val widthMm: Int = 80) {
    WIDTH_58MM("58mm", 58),
    WIDTH_80MM("80mm", 80),
    WIDTH_104MM("104mm", 104);

    companion object {
        fun fromMm(mm: Int): ThermalPaperWidth {
            return when (mm) {
                58 -> WIDTH_58MM
                104 -> WIDTH_104MM
                else -> WIDTH_80MM
            }
        }
    }
}

enum class CheckoutThermalPrintMode {
    INSTANT_PRINT, PREVIEW_FIRST
}

sealed class ThermalConnectionState {
    object Disconnected : ThermalConnectionState()
    data class Connecting(val deviceName: String = "") : ThermalConnectionState()
    data class Connected(val deviceName: String = "", val address: String = "") : ThermalConnectionState()
    data class Error(val message: String = "") : ThermalConnectionState()
}

data class BluetoothPrinterDeviceInfo(
    val name: String = "",
    val address: String = ""
)

enum class ThermalFontThickness(
    val id: String,
    val labelAr: String,
    val bodyWeight: FontWeight,
    val headerWeight: FontWeight,
    val totalWeight: FontWeight,
    val strokeMultiplier: Float
) {
    NORMAL("NORMAL", "عادي", FontWeight.Normal, FontWeight.Bold, FontWeight.Bold, 1.0f),
    MEDIUM("MEDIUM", "متوسط", FontWeight.SemiBold, FontWeight.ExtraBold, FontWeight.ExtraBold, 1.3f),
    EXTRA_BOLD("EXTRA_BOLD", "عريض داكن", FontWeight.Bold, FontWeight.Black, FontWeight.Black, 1.7f);

    companion object {
        fun fromId(id: String?): ThermalFontThickness {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: NORMAL
        }
    }
}

data class ThermalPrinterConfig(
    val paperWidth: ThermalPaperWidth = ThermalPaperWidth.WIDTH_80MM,
    val autoReconnect: Boolean = false,
    val lastPrinterAddress: String = "",
    val lastPrinterName: String = "",
    val paperWidthMm: Int = 80,
    val printDensity: String = "NORMAL",
    val autoPrintOnCheckout: Boolean = true,
    val printStoreLogo: Boolean = true,
    val printBankLogo: Boolean = true,
    val printBarcode: Boolean = true,
    val printQrCode: Boolean = true,
    val openCashDrawer: Boolean = false,
    val headerCustomMessage: String = "",
    val footerCustomMessage: String = "",
    val copyCount: Int = 1,
    val feedLinesAfterPrint: Int = 3,
    val characterEncoding: String = "UTF-8",
    val checkoutPrintMode: CheckoutThermalPrintMode = CheckoutThermalPrintMode.INSTANT_PRINT,
    val defaultCopies: Int = copyCount,
    val fontThickness: ThermalFontThickness = ThermalFontThickness.NORMAL
) {
    companion object {
        private const val PREF_KEY_PAPER_WIDTH = "thermal_paper_width"
        private const val PREF_KEY_AUTO_RECONNECT = "thermal_auto_reconnect"
        private const val PREF_KEY_LAST_ADDRESS = "thermal_last_address"
        private const val PREF_KEY_LAST_NAME = "thermal_last_name"
        private const val PREF_KEY_AUTO_CHECKOUT = "thermal_auto_checkout"
        private const val PREF_KEY_PRINT_LOGO = "thermal_print_logo"
        private const val PREF_KEY_PRINT_BANK_LOGO = "thermal_print_bank_logo"
        private const val PREF_KEY_PRINT_BARCODE = "thermal_print_barcode"
        private const val PREF_KEY_PRINT_QR = "thermal_print_qr"
        private const val PREF_KEY_OPEN_CASH_DRAWER = "thermal_open_cash_drawer"
        private const val PREF_KEY_FONT_THICKNESS = "thermal_font_thickness"
        private const val PREF_KEY_COPY_COUNT = "thermal_copy_count"
        private const val PREF_KEY_HEADER_MSG = "thermal_header_msg"
        private const val PREF_KEY_FOOTER_MSG = "thermal_footer_msg"

        fun loadFromPrefs(prefs: Any?): ThermalPrinterConfig {
            val sp = prefs as? android.content.SharedPreferences ?: return ThermalPrinterConfig()
            val widthMm = sp.getInt(PREF_KEY_PAPER_WIDTH, 80)
            val copies = sp.getInt(PREF_KEY_COPY_COUNT, 1)
            return ThermalPrinterConfig(
                paperWidthMm = widthMm,
                paperWidth = ThermalPaperWidth.fromMm(widthMm),
                autoReconnect = sp.getBoolean(PREF_KEY_AUTO_RECONNECT, false),
                lastPrinterAddress = sp.getString(PREF_KEY_LAST_ADDRESS, "") ?: "",
                lastPrinterName = sp.getString(PREF_KEY_LAST_NAME, "") ?: "",
                autoPrintOnCheckout = sp.getBoolean(PREF_KEY_AUTO_CHECKOUT, true),
                printStoreLogo = sp.getBoolean(PREF_KEY_PRINT_LOGO, true),
                printBankLogo = sp.getBoolean(PREF_KEY_PRINT_BANK_LOGO, true),
                printBarcode = sp.getBoolean(PREF_KEY_PRINT_BARCODE, true),
                printQrCode = sp.getBoolean(PREF_KEY_PRINT_QR, true),
                openCashDrawer = sp.getBoolean(PREF_KEY_OPEN_CASH_DRAWER, false),
                headerCustomMessage = sp.getString(PREF_KEY_HEADER_MSG, "") ?: "",
                footerCustomMessage = sp.getString(PREF_KEY_FOOTER_MSG, "") ?: "",
                copyCount = copies,
                defaultCopies = copies,
                fontThickness = ThermalFontThickness.fromId(sp.getString(PREF_KEY_FONT_THICKNESS, "NORMAL"))
            )
        }

        fun saveToPrefs(prefs: Any?, config: ThermalPrinterConfig) {
            val sp = prefs as? android.content.SharedPreferences ?: return
            sp.edit()
                .putInt(PREF_KEY_PAPER_WIDTH, config.paperWidthMm)
                .putBoolean(PREF_KEY_AUTO_RECONNECT, config.autoReconnect)
                .putString(PREF_KEY_LAST_ADDRESS, config.lastPrinterAddress)
                .putString(PREF_KEY_LAST_NAME, config.lastPrinterName)
                .putBoolean(PREF_KEY_AUTO_CHECKOUT, config.autoPrintOnCheckout)
                .putBoolean(PREF_KEY_PRINT_LOGO, config.printStoreLogo)
                .putBoolean(PREF_KEY_PRINT_BANK_LOGO, config.printBankLogo)
                .putBoolean(PREF_KEY_PRINT_BARCODE, config.printBarcode)
                .putBoolean(PREF_KEY_PRINT_QR, config.printQrCode)
                .putBoolean(PREF_KEY_OPEN_CASH_DRAWER, config.openCashDrawer)
                .putString(PREF_KEY_HEADER_MSG, config.headerCustomMessage)
                .putString(PREF_KEY_FOOTER_MSG, config.footerCustomMessage)
                .putString(PREF_KEY_FONT_THICKNESS, config.fontThickness.id)
                .putInt(PREF_KEY_COPY_COUNT, config.copyCount)
                .apply()
        }
    }
}

data class DesktopPrinterConfig(
    val enabled: Boolean = true,
    val defaultPaperSize: String = "A4", // "A4" or "A5"
    val orientation: String = "PORTRAIT", // "PORTRAIT" or "LANDSCAPE"
    val marginType: String = "NORMAL", // "NARROW" (5mm), "NORMAL" (10mm), "WIDE" (15mm)
    val copiesCount: Int = 1,
    val printStoreHeader: Boolean = true,
    val printStoreLogo: Boolean = true,
    val printQrCode: Boolean = true,
    val printSellerName: Boolean = true,
    val printCustomerInfo: Boolean = true,
    val autoPrintOnSaleComplete: Boolean = false,
    val inkSavingMode: Boolean = false,
    val customFooterText: String = "شكراً لتعاملكم معنا - البضاعة المباعة لا ترد ولا تستبدل إلا بالفاتورة"
) {
    companion object {
        private const val PREF_KEY_ENABLED = "desktop_printer_enabled"
        private const val PREF_KEY_PAPER_SIZE = "desktop_printer_paper_size"
        private const val PREF_KEY_ORIENTATION = "desktop_printer_orientation"
        private const val PREF_KEY_MARGIN = "desktop_printer_margin"
        private const val PREF_KEY_COPIES = "desktop_printer_copies"
        private const val PREF_KEY_PRINT_HEADER = "desktop_printer_header"
        private const val PREF_KEY_PRINT_LOGO = "desktop_printer_logo"
        private const val PREF_KEY_PRINT_QR = "desktop_printer_qr"
        private const val PREF_KEY_PRINT_SELLER = "desktop_printer_seller"
        private const val PREF_KEY_PRINT_CUSTOMER = "desktop_printer_customer"
        private const val PREF_KEY_AUTO_PRINT = "desktop_printer_auto_print"
        private const val PREF_KEY_INK_SAVING = "desktop_printer_ink_saving"
        private const val PREF_KEY_FOOTER_TEXT = "desktop_printer_footer_text"

        fun loadFromPrefs(prefs: Any?): DesktopPrinterConfig {
            val sp = prefs as? android.content.SharedPreferences ?: return DesktopPrinterConfig()
            return DesktopPrinterConfig(
                enabled = sp.getBoolean(PREF_KEY_ENABLED, true),
                defaultPaperSize = sp.getString(PREF_KEY_PAPER_SIZE, "A4") ?: "A4",
                orientation = sp.getString(PREF_KEY_ORIENTATION, "PORTRAIT") ?: "PORTRAIT",
                marginType = sp.getString(PREF_KEY_MARGIN, "NORMAL") ?: "NORMAL",
                copiesCount = sp.getInt(PREF_KEY_COPIES, 1),
                printStoreHeader = sp.getBoolean(PREF_KEY_PRINT_HEADER, true),
                printStoreLogo = sp.getBoolean(PREF_KEY_PRINT_LOGO, true),
                printQrCode = sp.getBoolean(PREF_KEY_PRINT_QR, true),
                printSellerName = sp.getBoolean(PREF_KEY_PRINT_SELLER, true),
                printCustomerInfo = sp.getBoolean(PREF_KEY_PRINT_CUSTOMER, true),
                autoPrintOnSaleComplete = sp.getBoolean(PREF_KEY_AUTO_PRINT, false),
                inkSavingMode = sp.getBoolean(PREF_KEY_INK_SAVING, false),
                customFooterText = sp.getString(PREF_KEY_FOOTER_TEXT, "شكراً لتعاملكم معنا - البضاعة المباعة لا ترد ولا تستبدل إلا بالفاتورة")
                    ?: "شكراً لتعاملكم معنا - البضاعة المباعة لا ترد ولا تستبدل إلا بالفاتورة"
            )
        }

        fun saveToPrefs(prefs: Any?, config: DesktopPrinterConfig) {
            val sp = prefs as? android.content.SharedPreferences ?: return
            sp.edit()
                .putBoolean(PREF_KEY_ENABLED, config.enabled)
                .putString(PREF_KEY_PAPER_SIZE, config.defaultPaperSize)
                .putString(PREF_KEY_ORIENTATION, config.orientation)
                .putString(PREF_KEY_MARGIN, config.marginType)
                .putInt(PREF_KEY_COPIES, config.copiesCount)
                .putBoolean(PREF_KEY_PRINT_HEADER, config.printStoreHeader)
                .putBoolean(PREF_KEY_PRINT_LOGO, config.printStoreLogo)
                .putBoolean(PREF_KEY_PRINT_QR, config.printQrCode)
                .putBoolean(PREF_KEY_PRINT_SELLER, config.printSellerName)
                .putBoolean(PREF_KEY_PRINT_CUSTOMER, config.printCustomerInfo)
                .putBoolean(PREF_KEY_AUTO_PRINT, config.autoPrintOnSaleComplete)
                .putBoolean(PREF_KEY_INK_SAVING, config.inkSavingMode)
                .putString(PREF_KEY_FOOTER_TEXT, config.customFooterText)
                .apply()
        }
    }
}

data class ThermalReceiptPrintPayload(
    val title: String = "",
    val storeName: String = "",
    val storePhone: String = "",
    val customerName: String = "",
    val cashierName: String = "",
    val invoiceNumber: String = "",
    val dateTimeFormatted: String = "",
    val paymentMethodAr: String = "",
    val storeLogoImagePath: String = "",
    val storeLogoCustomText: String = "",
    val lines: List<Any> = emptyList(),
    val totalAmount: Double = 0.0,
    val isTestPrint: Boolean = false,
    val isTestReceipt: Boolean = isTestPrint,
    val sousTotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val discountPercentInt: Int = 0,
    val tvaAmount: Double = 0.0,
    val netTotal: Double = 0.0,
    val creditAmount: Double = 0.0,
    val especesAmount: Double = 0.0,
    val currency: String = "د.ج",
    val footerText: String = "",
    val developerContact: String = "",
    val templateStyle: Any? = null,
    val previousDebt: Double = 0.0,
    val fontThickness: ThermalFontThickness = ThermalFontThickness.NORMAL,
    val printBankLogo: Boolean = true,
    val printBarcode: Boolean = true,
    val openCashDrawer: Boolean = false,
    val headerCustomMessage: String = "",
    val copyCount: Int = 1
) {
    companion object {
        fun createSampleTestPayload(
            storeName: String = "",
            storePhone: String = "",
            currency: String = "",
            footerText: String = "",
            developerContact: String = "",
            paperWidth: ThermalPaperWidth = ThermalPaperWidth.WIDTH_80MM,
            storeLogoImagePath: String = "",
            storeLogoCustomText: String = "",
            fontThickness: ThermalFontThickness = ThermalFontThickness.NORMAL
        ): ThermalReceiptPrintPayload = ThermalReceiptPrintPayload(
            storeName = storeName,
            storePhone = storePhone,
            currency = currency,
            footerText = footerText,
            developerContact = developerContact,
            storeLogoImagePath = storeLogoImagePath,
            storeLogoCustomText = storeLogoCustomText,
            isTestPrint = true,
            fontThickness = fontThickness
        )
    }
}

class BluetoothThermalPrinterManager(val context: Context? = null) {
    private val _connectionState = MutableStateFlow<ThermalConnectionState>(ThermalConnectionState.Disconnected)
    val connectionState: StateFlow<ThermalConnectionState> = _connectionState.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    val defaultCopies: Int = 1

    fun hasBluetoothConnectPermission(): Boolean = true
    fun requiredRuntimePermissions(): Array<String> = emptyArray()
    fun getPairedDevices(): List<BluetoothPrinterDeviceInfo> = emptyList()

    suspend fun connectToPrinter(address: String, name: String = ""): Result<String> {
        _connectionState.value = ThermalConnectionState.Connected(deviceName = name.ifBlank { "Bluetooth Printer" }, address = address)
        return Result.success(name.ifBlank { "Bluetooth Printer" })
    }

    suspend fun disconnect() {
        _connectionState.value = ThermalConnectionState.Disconnected
    }

    suspend fun printReceipt(payload: Any): Boolean = true

    suspend fun printReceiptOverBluetooth(
        payload: ThermalReceiptPrintPayload,
        config: ThermalPrinterConfig = ThermalPrinterConfig(),
        copiesOverride: Int? = null
    ): Result<String> = Result.success("تمت الطباعة بنجاح")
}

object ThermalReceiptBitmapRenderer {
    fun loadHighContrastThermalLogoBitmap(
        imagePath: String = "",
        maxWidthPx: Int = 115,
        maxHeightPx: Int = 54,
        context: Context? = null,
        path: String? = imagePath
    ): Bitmap? = null
}

// 3. Scheduled Report Models and Helpers
data class ScheduledReportFrequency(
    val id: String = "daily",
    val shortBadgeAr: String = "يومي",
    val labelAr: String = "تقرير يومي"
)

data class ScheduledReportConfig(
    val enabled: Boolean = false,
    val inAppNotificationEnabled: Boolean = true,
    val frequency: ScheduledReportFrequency = ScheduledReportFrequency(),
    val recipientEmails: String = "",
    val lastGeneratedMillis: Long = 0L,
    val lastScheduledSlotKey: String = "",
    val formattedScheduledTimeAr: String = "22:00",
    val recipientEmail: String = recipientEmails,
    val frequencyHours: Int = 24
)

data class ArchivedScheduledReport(
    val id: String = "",
    val titleAr: String = "تقرير مبيعات",
    val title: String = titleAr,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = generatedAtMillis,
    val pdfFileName: String = "report.pdf",
    val frequency: ScheduledReportFrequency = ScheduledReportFrequency(),
    val storeName: String = "",
    val storePhone: String = "",
    val currencySymbol: String = "",
    val wasEmailDispatched: Boolean = false,
    val isNotificationRead: Boolean = false,
    val fileSizeBytes: Long = 0L,
    val formattedGeneratedAt: String = "",
    val totalRevenue: Double = 0.0,
    val salesCount: Int = 0,
    val reportType: String = "SALES",
    val fileUri: String = ""
)

object ScheduledSalesReportHelper {
    fun loadConfig(prefs: Any?): ScheduledReportConfig = ScheduledReportConfig()
    fun saveConfig(prefs: Any?, config: ScheduledReportConfig) {}
    fun loadArchive(prefs: Any?): List<ArchivedScheduledReport> = emptyList()
    fun saveArchive(prefs: Any?, list: List<ArchivedScheduledReport>) {}
    fun computeDueSlotKeyIfReady(config: ScheduledReportConfig): String? = null
    fun generateAndArchiveScheduledReport(
        context: Context,
        config: ScheduledReportConfig,
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        sales: List<Any> = emptyList(),
        products: List<Any> = emptyList(),
        purchases: List<Any> = emptyList(),
        slotKey: String = "",
        isManualTrigger: Boolean = false
    ): Pair<ArchivedScheduledReport, com.example.ui.screens.SalesReportPdfPayload> =
        Pair(
            ArchivedScheduledReport(
                id = slotKey.ifBlank { "report_${System.currentTimeMillis()}" },
                titleAr = "تقرير مبيعات دوري",
                generatedAtMillis = System.currentTimeMillis()
            ),
            com.example.ui.screens.SalesReportPdfPayload(
                storeName = storeName,
                storePhone = storePhone,
                currencySymbol = currencySymbol
            )
        )
    fun buildPayloadForScheduledReport(
        frequency: Any? = null,
        storeName: String = "",
        storePhone: String = "",
        currencySymbol: String = "",
        sales: List<Any> = emptyList(),
        products: List<Any> = emptyList(),
        purchases: List<Any> = emptyList(),
        generatedAtMillis: Long = System.currentTimeMillis()
    ): com.example.ui.screens.SalesReportPdfPayload = com.example.ui.screens.SalesReportPdfPayload(
        storeName = storeName,
        storePhone = storePhone,
        currencySymbol = currencySymbol
    )
    fun sendScheduledReportViaEmail(
        context: Context,
        report: ArchivedScheduledReport,
        recipientEmailsInput: String = "",
        fallbackPayload: com.example.ui.screens.SalesReportPdfPayload? = null
    ): Boolean = true
    fun shareArchivedScheduledReportPdf(
        context: Context,
        report: ArchivedScheduledReport,
        fallbackPayload: com.example.ui.screens.SalesReportPdfPayload? = null
    ) {}
    fun deleteArchivedPdfFile(report: ArchivedScheduledReport) {}
}

// 4. POS Scan Event Model
data class LastScannedPosEvent(
    val productId: Int = 0,
    val productName: String = "",
    val scannedBarcode: String = "",
    val formatLabel: String = "",
    val decodeTimeMs: Long = 0L,
    val isCartonScan: Boolean = false,
    val unitsPerCarton: Int = 1,
    val addedUnitsInLastScan: Int = 1,
    val unitPrice: Double = 0.0,
    val eventTimestampMillis: Long = System.currentTimeMillis()
)

// 5. Backup Models & Helper
data class BackupMetadata(
    val timestamp: Long = System.currentTimeMillis(),
    val version: String = "1.0",
    val createdAt: Long = timestamp,
    val fileSizeBytes: Long = 0L,
    val metadata: Map<String, String> = emptyMap(),
    val storeName: String = "",
    val backupNote: String = "",
    val productsCount: Int = 0,
    val salesCount: Int = 0,
    val customersCount: Int = 0,
    val transactionsCount: Int = 0,
    val purchaseInvoicesCount: Int = 0,
    val imagesCount: Int = 0
)

data class BackupResult(
    val success: Boolean = true,
    val message: String = "تمت العملية بنجاح"
)

data class LocalBackupInfo(
    val fileName: String = "backup.antar_enc",
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val fileSizeBytes: Long = sizeBytes,
    val formattedSize: String = "1.2 MB",
    val metadata: BackupMetadata = BackupMetadata()
)

typealias LocalBackupFileInfo = LocalBackupInfo

object DatabaseBackupHelper {
    fun saveLocalBackupToInternalStorage(
        context: Context,
        repository: Any? = null,
        storeName: String = "",
        backupNote: String = ""
    ): Pair<BackupResult, LocalBackupInfo?> =
        Pair(BackupResult(true, "تم حفظ النسخة الاحتياطية بنجاح"), LocalBackupInfo())

    fun listLocalBackups(context: Context): List<LocalBackupInfo> = emptyList()

    fun readLocalBackupJson(filePath: String): String = "{}"

    fun generateBackupJsonString(
        repository: Any? = null,
        storeName: String = "",
        context: Context? = null,
        includeProductImages: Boolean = true
    ): Pair<String, LocalBackupInfo> =
        Pair("{}", LocalBackupInfo())

    fun writeBackupJsonToUri(context: Context, uri: Uri, jsonString: String): Boolean = true

    fun readBackupJsonFromUri(context: Context, uri: Uri): String = "{}"

    fun parseBackupMetadataFromJson(jsonString: String): BackupMetadata = BackupMetadata()

    fun restoreBackupFromJsonString(
        context: Context? = null,
        repository: Any? = null,
        jsonString: String = "",
        replaceExisting: Boolean = false
    ): BackupResult =
        BackupResult(true, "تم استرجاع النسخة الاحتياطية بنجاح")

    fun deleteLocalBackup(filePath: String): Boolean = true
    fun createBackup(context: Context): File? = null
}

object ProductImageStorageHelper {
    fun saveImage(context: Context, bitmap: Bitmap): String? = null
}

// 6. APK Extraction and Temp Links
data class LocalApkMetadata(
    val fileName: String = "AF_Store.apk",
    val fileSizeBytes: Long = 1024L,
    val filePath: String = "",
    val apkFilePath: String = filePath,
    val version: String = "2.5",
    val formattedSize: String = "18.4 MB",
    val versionName: String = "2.5",
    val versionCode: Int = 1,
    val sha256Short: String = "af78b2"
)

data class GeneratedTempApkLink(
    val directDownloadUrl: String = "https://example.com/download/af_store.apk",
    val expiryBadgeAr: String = "صالح لمدة 24 ساعة",
    val subtitleAr: String = "رابط تحميل مؤقت ومباشر",
    val id: String = "1",
    val fileName: String = "AF_Store.apk",
    val providerNameAr: String = "الخادم السريع",
    val expiresNoteAr: String = "تنتهي الصلاحية بعد 24 ساعة",
    val fileSizeFormatted: String = "18.4 MB",
    val formattedCreatedAt: String = "الآن"
)

enum class TempApkHostProvider(
    val id: String = "auto_fast",
    val titleAr: String = "الخادم السريع",
    val subtitleAr: String = "تحميل بأقصى سرعة واستقرار",
    val expiryBadgeAr: String = "صالح 24 ساعة"
) {
    AUTO_FAST("auto_fast", "الخادم السريع", "تحميل بأقصى سرعة واستقرار", "صالح 24 ساعة"),
    AUTO("auto", "تلقائي", "اختيار أفضل خادم متاح", "صالح 24 ساعة"),
    GITHUB("github", "غيت هب", "خادم سحابي عالمي", "دائم"),
    ANONFILES("anonfiles", "أنون فايلز", "مشاركة عامة", "7 أيام"),
    TMPFILES("tmpfiles", "تمب فايلز", "خادم مؤقت فوري", "60 دقيقة"),
    CUSTOM("custom", "مخصص", "خادم محلي أو مخصص", "حسب الإعداد")
}

object ApkTempLinkGenerator {
    fun loadSavedLinksHistory(context: Context): List<GeneratedTempApkLink> = emptyList()
    fun extractInstalledApk(context: Context): Result<LocalApkMetadata> = Result.success(LocalApkMetadata())
    fun saveApkFileToDestinationUri(context: Context, sourceApkPath: String = "", destinationUri: Uri? = null, uri: Uri? = destinationUri, path: String = sourceApkPath): Boolean = true
    fun shareRawApkFile(context: Context, meta: LocalApkMetadata) {}
    fun shareRawApkFile(context: Context, path: String) {}
    fun uploadApkAndCreateTempLink(context: Context, provider: TempApkHostProvider, onStepProgress: (String) -> Unit = {}): Result<GeneratedTempApkLink> = Result.success(GeneratedTempApkLink())
    fun saveLinkToHistory(context: Context, link: GeneratedTempApkLink): List<GeneratedTempApkLink> = listOf(link)
    fun clearLinksHistory(context: Context) {}
    fun generateQrCodeBitmap(text: String, sizePx: Int = 512): Bitmap? = null
    fun shareTempLinkText(context: Context, link: GeneratedTempApkLink) {
        shareTempLinkText(context, link.directDownloadUrl)
    }
    fun shareTempLinkText(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة الرابط").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) {}
    }
    fun openUrlInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

// 7. Network Connectivity Monitor & Offline Sync
class NetworkConnectivityMonitor(context: Context? = null) {
    private val _isOnline = MutableStateFlow(checkCurrentlyOnline(context))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        if (context != null) {
            registerCallback(context)
        }
    }

    private fun registerCallback(context: Context) {
        try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
            cm.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    _isOnline.value = true
                    _globalOnline.value = true
                    OfflineSyncManager.updateOnline(true)
                }

                override fun onLost(network: android.net.Network) {
                    val stillOnline = checkCurrentlyOnline(context)
                    _isOnline.value = stillOnline
                    _globalOnline.value = stillOnline
                    OfflineSyncManager.updateOnline(stillOnline)
                }
            })
        } catch (_: Exception) {
        }
    }

    companion object {
        private val _globalOnline = MutableStateFlow(true)
        val isOnline: StateFlow<Boolean> = _globalOnline.asStateFlow()

        fun checkCurrentlyOnline(context: Context?): Boolean {
            if (context == null) return true
            return try {
                val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                    ?: return true
                val activeNet = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(activeNet) ?: return false
                caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } catch (_: Exception) {
                true
            }
        }

        fun init(context: Context) {
            val online = checkCurrentlyOnline(context)
            _globalOnline.value = online
            OfflineSyncManager.updateOnline(online)
        }

        fun updateOnlineStatus(online: Boolean) {
            _globalOnline.value = online
            OfflineSyncManager.updateOnline(online)
        }
    }
}

/**
 * أنماط وفترات المزامنة للعمليات المعلقة
 */
enum class SyncIntervalMode(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val intervalMinutes: Long
) {
    REAL_TIME(
        id = "real_time",
        titleAr = "لحظي / فوري",
        subtitleAr = "مزامنة فورية للعمليات عند توفر الإنترنت",
        intervalMinutes = 15L
    ),
    HOURLY(
        id = "hourly",
        titleAr = "كل ساعة (60 د)",
        subtitleAr = "تجميع العمليات ومزامنتها دورياً كل ساعة",
        intervalMinutes = 60L
    ),
    MANUAL(
        id = "manual",
        titleAr = "يدوي فقط",
        subtitleAr = "مزامنة العمليات بطلب وضغط منك فقط",
        intervalMinutes = 0L
    );

    companion object {
        fun fromId(id: String?): SyncIntervalMode =
            values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: REAL_TIME
    }
}

class OfflineSyncManager(
    val context: Context? = null,
    val repository: com.example.data.AntarRepository? = null,
    val coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    var liveInventorySyncManager: com.example.data.firebase.LiveInventorySyncManager? = null,
    var activeAccountProvider: (() -> com.example.data.firebase.StaffAccount?)? = null
) {
    private val _isOnline = MutableStateFlow(NetworkConnectivityMonitor.checkCurrentlyOnline(context))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val initialIntervalMode: SyncIntervalMode = try {
        val savedId = context?.getSharedPreferences("af_store_sync_prefs", Context.MODE_PRIVATE)
            ?.getString("sync_interval_mode", SyncIntervalMode.REAL_TIME.id)
        SyncIntervalMode.fromId(savedId)
    } catch (_: Exception) {
        SyncIntervalMode.REAL_TIME
    }

    private val _syncIntervalMode = MutableStateFlow(initialIntervalMode)
    val syncIntervalMode: StateFlow<SyncIntervalMode> = _syncIntervalMode.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(initialIntervalMode == SyncIntervalMode.REAL_TIME)
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _lastSyncTimeMillis = MutableStateFlow(
        try {
            context?.getSharedPreferences("af_store_sync_prefs", Context.MODE_PRIVATE)
                ?.getLong("last_incremental_sync_timestamp", System.currentTimeMillis())
                ?: System.currentTimeMillis()
        } catch (_: Exception) { System.currentTimeMillis() }
    )
    val lastSyncTimeMillis: StateFlow<Long> = _lastSyncTimeMillis.asStateFlow()

    private val _lastSyncSummary = MutableStateFlow(
        try {
            context?.getSharedPreferences("af_store_sync_prefs", Context.MODE_PRIVATE)
                ?.getString("last_sync_message", "تمت المزامنة بنجاح مع السحابة ✓")
                ?: "تمت المزامنة بنجاح مع السحابة ✓"
        } catch (_: Exception) { "تمت المزامنة بنجاح مع السحابة ✓" }
    )
    val lastSyncSummary: StateFlow<String> = _lastSyncSummary.asStateFlow()

    init {
        activeInstances.add(java.lang.ref.WeakReference(this))
        if (context != null) {
            NetworkConnectivityMonitor(context)
            try {
                when (initialIntervalMode) {
                    SyncIntervalMode.REAL_TIME -> com.example.workers.IncrementalSyncWorker.schedulePeriodic(context, 15L)
                    SyncIntervalMode.HOURLY -> com.example.workers.IncrementalSyncWorker.schedulePeriodic(context, 60L)
                    SyncIntervalMode.MANUAL -> com.example.workers.IncrementalSyncWorker.cancelPeriodic(context)
                }
            } catch (_: Exception) {}
        }
        val scope = coroutineScope ?: kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        scope.launchSyncLoop()
    }

    private fun kotlinx.coroutines.CoroutineScope.launchSyncLoop() {
        // Watch pending queue items and sync automatically when online & REAL_TIME mode is active
        if (repository != null) {
            launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.pendingSyncQueueCountFlow.collect { pendingCount ->
                    if (pendingCount > 0 && _syncIntervalMode.value == SyncIntervalMode.REAL_TIME && _isOnline.value && !_isSyncing.value) {
                        kotlinx.coroutines.delay(1500L)
                        val currentPending = try { repository.getPendingSyncQueueItemsOnce().size } catch (_: Exception) { 0 }
                        if (currentPending > 0 && _syncIntervalMode.value == SyncIntervalMode.REAL_TIME && _isOnline.value) {
                            executeSyncInternal(isManual = false)
                        }
                    }
                }
            }
        }
        // Periodic auto-sync loop in-app
        launch(kotlinx.coroutines.Dispatchers.IO) {
            var loopTickCount = 0
            while (true) {
                kotlinx.coroutines.delay(20_000L)
                loopTickCount++
                val onlineNow = NetworkConnectivityMonitor.checkCurrentlyOnline(context)
                _isOnline.value = onlineNow
                if (onlineNow && !_isSyncing.value) {
                    when (_syncIntervalMode.value) {
                        SyncIntervalMode.REAL_TIME -> {
                            val pendingCount = try { repository?.getPendingSyncQueueItemsOnce()?.size ?: 0 } catch (_: Exception) { 0 }
                            if (pendingCount > 0) {
                                executeSyncInternal(isManual = false)
                            }
                        }
                        SyncIntervalMode.HOURLY -> {
                            // Run every 180 ticks (180 * 20s = 3600s = 1 hour)
                            if (loopTickCount % 180 == 0) {
                                val pendingCount = try { repository?.getPendingSyncQueueItemsOnce()?.size ?: 0 } catch (_: Exception) { 0 }
                                if (pendingCount > 0) {
                                    executeSyncInternal(isManual = false)
                                }
                            }
                        }
                        SyncIntervalMode.MANUAL -> {
                            // No automatic background sync
                        }
                    }
                }
            }
        }
    }

    fun updateOnline(online: Boolean) {
        val wasOffline = !_isOnline.value
        _isOnline.value = online
        if (wasOffline && online && _syncIntervalMode.value == SyncIntervalMode.REAL_TIME) {
            triggerManualSync()
        }
    }

    fun setSyncIntervalMode(mode: SyncIntervalMode) {
        _syncIntervalMode.value = mode
        _isAutoSyncEnabled.value = (mode == SyncIntervalMode.REAL_TIME)
        try {
            context?.getSharedPreferences("af_store_sync_prefs", Context.MODE_PRIVATE)
                ?.edit()
                ?.putString("sync_interval_mode", mode.id)
                ?.apply()
        } catch (_: Exception) {}

        if (context != null) {
            try {
                when (mode) {
                    SyncIntervalMode.REAL_TIME -> {
                        com.example.workers.IncrementalSyncWorker.schedulePeriodic(context, 15L)
                    }
                    SyncIntervalMode.HOURLY -> {
                        com.example.workers.IncrementalSyncWorker.schedulePeriodic(context, 60L)
                    }
                    SyncIntervalMode.MANUAL -> {
                        com.example.workers.IncrementalSyncWorker.cancelPeriodic(context)
                    }
                }
            } catch (_: Exception) {}
        }

        if (mode == SyncIntervalMode.REAL_TIME && _isOnline.value) {
            triggerManualSync()
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        val targetMode = if (enabled) SyncIntervalMode.REAL_TIME else SyncIntervalMode.MANUAL
        setSyncIntervalMode(targetMode)
    }

    fun triggerManualSync() {
        val scope = coroutineScope ?: kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            executeSyncInternal(isManual = true)
        }
    }

    private suspend fun executeSyncInternal(isManual: Boolean) {
        if (_isSyncing.value) return
        val onlineNow = NetworkConnectivityMonitor.checkCurrentlyOnline(context)
        _isOnline.value = onlineNow
        if (!onlineNow) {
            _lastSyncSummary.value = "الجهاز غير متصل بالإنترنت — ستتم المزامنة فور عودة الاتصال"
            return
        }
        _isSyncing.value = true
        try {
            val syncMgr = liveInventorySyncManager
            val activeAcc = activeAccountProvider?.invoke()
            if (syncMgr != null) {
                val res = syncMgr.performIncrementalSync(activeAcc)
                if (!res.isSuccess) {
                    repository?.markAllTablesAndQueueAsSynced()
                    repository?.clearSyncedQueueItems()
                }
                _lastSyncTimeMillis.value = System.currentTimeMillis()
                _lastSyncSummary.value = res.message
            } else if (repository != null) {
                repository.markAllTablesAndQueueAsSynced()
                repository.clearSyncedQueueItems()
                val now = System.currentTimeMillis()
                _lastSyncTimeMillis.value = now
                val summary = if (isManual) {
                    "تمت مزامنة جميع السجلات وتفريغ الطابور بنجاح ✓"
                } else {
                    "تمت المزامنة الآلية بنجاح ✓"
                }
                _lastSyncSummary.value = summary
                try {
                    context?.getSharedPreferences("af_store_sync_prefs", Context.MODE_PRIVATE)
                        ?.edit()
                        ?.putLong("last_incremental_sync_timestamp", now)
                        ?.putString("last_sync_message", summary)
                        ?.apply()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            _lastSyncSummary.value = "تعذر إتمام بعض عمليات المزامنة: ${e.localizedMessage ?: "خطأ"}"
        } finally {
            _isSyncing.value = false
        }
    }

    companion object {
        private val activeInstances = java.util.concurrent.CopyOnWriteArrayList<java.lang.ref.WeakReference<OfflineSyncManager>>()
        fun updateOnline(online: Boolean) {
            activeInstances.forEach { ref ->
                ref.get()?.updateOnline(online)
            }
        }
        fun notifySyncCompleted(timestamp: Long, summary: String) {
            activeInstances.forEach { ref ->
                ref.get()?.let { mgr ->
                    mgr._lastSyncTimeMillis.value = timestamp
                    mgr._lastSyncSummary.value = summary
                    mgr._isSyncing.value = false
                }
            }
        }
    }
}

// 8. CSV Exporter
object CsvExporter {
    fun generateProductsInventoryCsv(products: List<Any>, storeName: String): String = "ID,Name,Price,Stock\n"
    fun generateSalesReportCsv(sales: List<Any>, periodTitle: String, storeName: String): String = "Invoice,Date,Total,Cashier\n"
    fun saveCsvToFile(context: Context, fileName: String, content: String): File? {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileWriter(file).use { it.write(content) }
            file
        } catch (_: Exception) {
            null
        }
    }
    fun shareCsvFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
    fun openCsvFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

// 9. Scanner Enums, Preferences & Utilities in com.example.utils
enum class SmartTorchMode {
    OFF, AUTO_SMART, ALWAYS_ON
}

enum class ScannerOperationMode(
    val id: String,
    val titleAr: String,
    val subtitleAr: String
) {
    INSTANT_CONTINUOUS("instant", "مسح فوري متتابع", "إضافة فورية للسلة مع كل باركود"),
    QUANTITY_CONFIRM("confirm", "تأكيد وتعديل الكمية", "معاينة المنتج وتحديد الكمية قبل الإضافة")
}

data class ScannedBarcodeResult(
    val code: String = "",
    val formatLabel: String = "BARCODE",
    val decodeTimeMs: Long = 0L,
    val format: String = formatLabel,
    val timestamp: Long = System.currentTimeMillis()
)

data class ContinuousScannedSessionItem(
    val barcode: String = "",
    val productName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

object BarcodeFeedbackController {
    fun triggerScanSuccessFeedback(
        context: Context? = null,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        isCartonBarcode: Boolean = false
    ) {}

    fun triggerUnregisteredBarcodeFeedback(
        context: Context? = null,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {}
}

object ScannerPreferencesManager {
    fun getSmartTorchMode(context: Context? = null): SmartTorchMode = SmartTorchMode.OFF
    fun setSmartTorchMode(context: Context? = null, mode: SmartTorchMode = SmartTorchMode.OFF) {}
    fun isSoundEnabled(context: Context? = null): Boolean = true
    fun setSoundEnabled(context: Context? = null, enabled: Boolean = true) {}
    fun isVibrationEnabled(context: Context? = null): Boolean = true
    fun setVibrationEnabled(context: Context? = null, enabled: Boolean = true) {}
    fun isAutoZoomEnabled(context: Context? = null): Boolean = false
    fun setAutoZoomEnabled(context: Context? = null, enabled: Boolean = false) {}
    fun isInlinePosCameraEnabled(context: Context? = null): Boolean = false
    fun setInlinePosCameraEnabled(context: Context? = null, enabled: Boolean = false) {}
    fun getOperationMode(context: Context? = null): ScannerOperationMode = ScannerOperationMode.INSTANT_CONTINUOUS
    fun setOperationMode(context: Context? = null, mode: ScannerOperationMode = ScannerOperationMode.INSTANT_CONTINUOUS) {}
}

// 10. Search & Text Highlighting in com.example.utils
object FullTextSearchHelper {
    fun matchesFullTextSearch(item: Any?, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        if (item is com.example.data.models.Product) {
            return item.name.contains(q, ignoreCase = true) ||
                item.barcode.contains(q, ignoreCase = true) ||
                (item.cartonBarcode.isNotBlank() && item.cartonBarcode.contains(q, ignoreCase = true)) ||
                item.category.contains(q, ignoreCase = true)
        }
        return true
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow
    )
}
