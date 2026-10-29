package com.example.data.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RecordSyncState {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
}

/**
 * نموذج بيانات المنتجات الموحد لدعم قاعدة البيانات المحلية (Room) وقاعدة البيانات السحابية (Firestore).
 * يعكس كافة البيانات المطلوبة لإدارة مخزون المنتجات مع التعليقات التوضيحية اللازمة لـ Firestore و Room:
 * - المعرف التلقائي لـ Firestore (@DocumentId) والمعرف المحلي (@PrimaryKey)
 * - الاسم، سعر الشراء، سعر البيع، الكمية بالقطعة والكرتون، معامل تحويل الكرتون، الحد الأدنى، الباركود (قطع وكرتون)
 * - الفئة، رابط ومسار الصورة
 * - طوابع الوقت السيرفرية (@ServerTimestamp) لإنشاء وتحديث البيانات
 */
@Keep
@Immutable
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["cartonBarcode"]),
        Index(value = ["name", "category"]),
        Index(value = ["category"]),
        Index(value = ["syncStatus", "updatedAt"]),
        Index(value = ["syncStatus"]),
        Index(value = ["updatedAt"])
    ]
)
data class Product(
    // المعرف المحلي بقاعدة بيانات Room
    @PrimaryKey(autoGenerate = true)
    @get:PropertyName("id") @set:PropertyName("id")
    var id: Int = 0,

    // المعرف التلقائي للوثيقة في Firestore
    @DocumentId
    @get:PropertyName("firestoreId") @set:PropertyName("firestoreId")
    var firestoreId: String = "",

    // معرف المستخدم المالك في Firestore
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",

    // اسم المنتج (الاسم)
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    // سعر الشراء
    @get:PropertyName("purchasePrice") @set:PropertyName("purchasePrice")
    var purchasePrice: Double = 0.0,

    // سعر البيع (سعر القطعة)
    @get:PropertyName("price") @set:PropertyName("price")
    var price: Double = 0.0,

    // الكمية المتوفرة بالمخزون (بالقطعة)
    @get:PropertyName("stock") @set:PropertyName("stock")
    var stock: Int = 0,

    // معامل تحويل الكرتون (عدد القطع بالكرتون)
    @get:PropertyName("unitsPerCarton") @set:PropertyName("unitsPerCarton")
    var unitsPerCarton: Int = 1,

    // الباركود الخاص بالقطعة
    @get:PropertyName("barcode") @set:PropertyName("barcode")
    var barcode: String = "",

    // الباركود الخاص بالكرتون
    @get:PropertyName("cartonBarcode") @set:PropertyName("cartonBarcode")
    var cartonBarcode: String = "",

    // مسار الصورة المحلي (في الجهاز)
    @get:PropertyName("imagePath") @set:PropertyName("imagePath")
    var imagePath: String = "",

    // حالة الحذف المؤقت (Soft Delete)
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,

    // طابع وقت التحديث المحلي بالمللي ثانية
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis(),

    // حالة المزامنة المحلية
    @get:PropertyName("syncStatus") @set:PropertyName("syncStatus")
    var syncStatus: String = RecordSyncState.PENDING,

    // الفئة / التصنيف
    @get:PropertyName("category") @set:PropertyName("category")
    var category: String = "عام",

    // الحد الأدنى للمخزون للتنبيه
    @get:PropertyName("minStockAlert") @set:PropertyName("minStockAlert")
    var minStockAlert: Int = 5,

    // رابط الصورة على السيرفر / الفايربيس (Cloud Image URL)
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    // طابع وقت الإنشاء بالسيرفر في Firestore
    @ServerTimestamp
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,

    // طابع وقت التحديث بالسيرفر في Firestore
    @ServerTimestamp
    @get:PropertyName("serverUpdatedAt") @set:PropertyName("serverUpdatedAt")
    var serverUpdatedAt: Timestamp? = null
) {
    // خصائص مشتقة وحسومات مساعدة

    val salePrice: Double
        get() = price

    val sellingPrice: Double
        get() = price

    val quantityInUnits: Int
        get() = stock

    val quantityInCartons: Double
        get() = if (unitsPerCarton > 0) stock.toDouble() / unitsPerCarton else 0.0

    val isLowStock: Boolean
        get() = stock <= minStockAlert

    val profitPerUnit: Double
        get() = (price - purchasePrice).coerceAtLeast(0.0)

    val profitMarginPercent: Double
        get() = if (price > 0.0) ((price - purchasePrice) / price) * 100.0 else 0.0

    val cartonCount: Int
        get() = if (unitsPerCarton > 1) stock / unitsPerCarton else 0

    val remainingLooseUnits: Int
        get() = if (unitsPerCarton > 1) stock % unitsPerCarton else stock

    val cartonSellingPrice: Double
        get() = price * unitsPerCarton.coerceAtLeast(1)

    val cartonPurchasePrice: Double
        get() = purchasePrice * unitsPerCarton.coerceAtLeast(1)

    val resolvedImage: String
        get() = imageUrl.ifBlank { imagePath }
}

@Immutable
@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["saleDate"]),
        Index(value = ["productId"]),
        Index(value = ["customerId", "saleDate"]),
        Index(value = ["customerId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["invoiceGroupId"]),
        Index(value = ["orderType", "status"]),
        Index(value = ["orderType"]),
        Index(value = ["status"])
    ]
)
data class Sale(
    @PrimaryKey(autoGenerate = true) val saleId: Int = 0,
    val productId: Int,
    val productName: String,
    val quantitySold: Int,
    val totalPrice: Double,
    val saleDate: Long = System.currentTimeMillis(),
    val customerId: Int? = null,
    val customerName: String = "",
    val isDebt: Boolean = false,
    val paymentMethod: String = "CASH",
    val invoiceGroupId: String = "",
    val orderType: String = "direct", // "direct" for immediate POS, "pre_sale" for pre-sales
    val status: String = "completed", // "completed", "pending", "cancelled"
    val expectedDeliveryDate: Long = 0L,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = RecordSyncState.PENDING
)

@Immutable
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"]),
        Index(value = ["loyaltyTier"]),
        Index(value = ["syncStatus", "updatedAt"]),
        Index(value = ["syncStatus"]),
        Index(value = ["updatedAt"])
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val email: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationUrl: String = "",
    val balanceDebt: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val isFavorite: Boolean = false,
    val loyaltyPoints: Int = 0,
    val loyaltyGoal: Int = 500,
    val loyaltyRewardValue: Double = 50.0,
    val loyaltyTier: String = "Standard",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = RecordSyncState.PENDING
) {
    fun getEffectiveMapsUrl(): String? {
        if (locationUrl.isNotBlank()) return locationUrl.trim()
        if (latitude != null && longitude != null && (latitude != 0.0 || longitude != 0.0)) {
            return "https://maps.google.com/?q=$latitude,$longitude"
        }
        return null
    }

    fun hasLocation(): Boolean = getEffectiveMapsUrl() != null
}

@Immutable
@Entity(
    tableName = "customer_transactions",
    indices = [
        Index(value = ["customerId", "date"]),
        Index(value = ["customerId"]),
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["syncStatus"])
    ]
)
data class CustomerTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val customerName: String,
    val type: String, // "DEBT" or "PAYMENT"
    val amount: Double,
    val notes: String = "",
    val date: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = RecordSyncState.PENDING
)

@Immutable
data class PurchaseInvoiceItem(
    val productId: Int? = null,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val sellingPrice: Double,
    val subtotal: Double = quantity * unitCost,
    val isCarton: Boolean = false,
    val unitsPerCarton: Int = 1,
    val barcode: String = "",
    val cartonBarcode: String = "",
    val imagePath: String = "",
    val category: String = "عام",
    val isNewProduct: Boolean = false
)

@Immutable
@Entity(
    tableName = "purchase_invoices",
    indices = [
        Index(value = ["supplierName"]),
        Index(value = ["dateMillis"]),
        Index(value = ["paymentStatus"]),
        Index(value = ["syncStatus"])
    ]
)
data class PurchaseInvoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val supplierName: String,
    val supplierPhone: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val totalCost: Double,
    val paidAmount: Double,
    val remainingDebt: Double = (totalCost - paidAmount).coerceAtLeast(0.0),
    val paymentStatus: String = if (paidAmount >= totalCost) "PAID" else if (paidAmount > 0) "PARTIAL" else "UNPAID",
    val notes: String = "",
    val itemsJson: String = "[]",
    val itemsCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = RecordSyncState.PENDING
) {
    fun getItems(): List<PurchaseInvoiceItem> {
        return try {
            val arr = JSONArray(itemsJson)
            val list = mutableListOf<PurchaseInvoiceItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PurchaseInvoiceItem(
                        productId = if (obj.has("productId") && !obj.isNull("productId")) obj.optInt("productId") else null,
                        productName = obj.optString("productName", ""),
                        quantity = obj.optInt("quantity", 1),
                        unitCost = obj.optDouble("unitCost", 0.0),
                        sellingPrice = obj.optDouble("sellingPrice", 0.0),
                        subtotal = obj.optDouble("subtotal", 0.0),
                        isCarton = obj.optBoolean("isCarton", false),
                        unitsPerCarton = obj.optInt("unitsPerCarton", 1),
                        barcode = obj.optString("barcode", ""),
                        cartonBarcode = obj.optString("cartonBarcode", ""),
                        imagePath = obj.optString("imagePath", ""),
                        category = obj.optString("category", "عام"),
                        isNewProduct = obj.optBoolean("isNewProduct", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun encodeItems(items: List<PurchaseInvoiceItem>): String {
            val arr = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    if (item.productId != null) put("productId", item.productId)
                    put("productName", item.productName)
                    put("quantity", item.quantity)
                    put("unitCost", item.unitCost)
                    put("sellingPrice", item.sellingPrice)
                    put("subtotal", item.subtotal)
                    put("isCarton", item.isCarton)
                    put("unitsPerCarton", item.unitsPerCarton)
                    put("barcode", item.barcode)
                    put("cartonBarcode", item.cartonBarcode)
                    put("imagePath", item.imagePath)
                    put("category", item.category)
                    put("isNewProduct", item.isNewProduct)
                }
                arr.put(obj)
            }
            return arr.toString()
        }
    }
}

object UserRole {
    const val OWNER = "owner"
    const val SUPER_ADMIN = "super_admin"
    const val ADMIN = "admin"
    const val MANAGER = "manager"
    const val WORKER = "worker"
    const val SELLER = "seller"
    const val DEVELOPER = "developer"

    fun getDisplayName(role: String): String = when (role.lowercase()) {
        OWNER, SUPER_ADMIN, ADMIN -> "مالك المتجر (Deffas antar)"
        MANAGER -> "مدير فرع معتمد"
        DEVELOPER -> "مطور النظام"
        else -> "موظف مبيعات (كاشير)"
    }
}

@Immutable
data class WorkerPermissions(
    val canMakeSales: Boolean = true,
    val canManageInventory: Boolean = true,
    val canEditPrices: Boolean = false,
    val canViewReports: Boolean = false,
    val canManageDebtsAndCustomers: Boolean = true,
    val canRefundOrVoid: Boolean = false,
    val canApplyDiscounts: Boolean = true,
    val canViewCostPrices: Boolean = false,
    val canExportData: Boolean = false,
    val canManagePurchaseInvoices: Boolean = false
) {
    fun toJsonString(): String {
        return JSONObject().apply {
            put("canMakeSales", canMakeSales)
            put("canManageInventory", canManageInventory)
            put("canEditPrices", canEditPrices)
            put("canViewReports", canViewReports)
            put("canManageDebtsAndCustomers", canManageDebtsAndCustomers)
            put("canRefundOrVoid", canRefundOrVoid)
            put("canApplyDiscounts", canApplyDiscounts)
            put("canViewCostPrices", canViewCostPrices)
            put("canExportData", canExportData)
            put("canManagePurchaseInvoices", canManagePurchaseInvoices)
        }.toString()
    }

    companion object {
        fun fromJsonString(json: String): WorkerPermissions {
            if (json.isBlank()) return WorkerPermissions()
            return try {
                val obj = JSONObject(json)
                WorkerPermissions(
                    canMakeSales = obj.optBoolean("canMakeSales", true),
                    canManageInventory = obj.optBoolean("canManageInventory", true),
                    canEditPrices = obj.optBoolean("canEditPrices", false),
                    canViewReports = obj.optBoolean("canViewReports", false),
                    canManageDebtsAndCustomers = obj.optBoolean("canManageDebtsAndCustomers", true),
                    canRefundOrVoid = obj.optBoolean("canRefundOrVoid", false),
                    canApplyDiscounts = obj.optBoolean("canApplyDiscounts", true),
                    canViewCostPrices = obj.optBoolean("canViewCostPrices", false),
                    canExportData = obj.optBoolean("canExportData", false),
                    canManagePurchaseInvoices = obj.optBoolean("canManagePurchaseInvoices", false)
                )
            } catch (_: Exception) {
                WorkerPermissions()
            }
        }
    }
}

@Immutable
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String,
    val role: String = UserRole.MANAGER,
    val fullName: String,
    val phone: String = "",
    val storeName: String = "AF store",
    val managerId: Int? = null,
    val isActive: Boolean = true,
    val isTrial: Boolean = false,
    val trialEndsAt: Long = 0L,
    val isApprovedByOwner: Boolean = true,
    val status: String = "ACTIVE", // ACTIVE, TRIAL_ACTIVE, TRIAL_EXPIRED, PENDING_APPROVAL, SUSPENDED
    val createdAt: Long = System.currentTimeMillis(),
    val permissionsJson: String = WorkerPermissions().toJsonString()
) {
    val isOwner: Boolean
        get() = role.lowercase() in listOf(UserRole.OWNER, UserRole.SUPER_ADMIN, UserRole.DEVELOPER) ||
            username.lowercase().contains("deffas") || username.lowercase().contains("antar")

    val isManager: Boolean
        get() = isOwner || role.lowercase() in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.SUPER_ADMIN, UserRole.DEVELOPER)

    val isTrialActive: Boolean
        get() = isTrial && System.currentTimeMillis() <= trialEndsAt

    val isTrialExpired: Boolean
        get() = isTrial && System.currentTimeMillis() > trialEndsAt && !isApprovedByOwner && !isOwner

    val effectiveManagerId: Int
        get() = if (isManager) id else (managerId ?: id)

    val permissions: WorkerPermissions
        get() = if (isManager) {
            WorkerPermissions(
                canMakeSales = true,
                canManageInventory = true,
                canEditPrices = true,
                canViewReports = true,
                canManageDebtsAndCustomers = true,
                canRefundOrVoid = true,
                canApplyDiscounts = true,
                canViewCostPrices = true,
                canExportData = true,
                canManagePurchaseInvoices = true
            )
        } else {
            WorkerPermissions.fromJsonString(permissionsJson)
        }
}

@Immutable
@Entity(
    tableName = "activity_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["userId"]),
        Index(value = ["actionType"])
    ]
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 0,
    val username: String = "admin",
    val userRole: String = UserRole.MANAGER,
    val managerId: Int? = null,
    val actionType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = ""
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(timestamp))
}

@Immutable
data class ArchivedInvoice(
    val invoiceId: String,
    val dateMillis: Long,
    val customerName: String,
    val customerId: Int?,
    val paymentMethod: String,
    val isDebt: Boolean,
    val items: List<Sale>,
    val subtotal: Double,
    val totalQuantity: Int,
    val syncStatus: String = RecordSyncState.SYNCED,
    val orderType: String = "direct",
    val status: String = "completed",
    val expectedDeliveryDate: Long = 0L,
    val notes: String = "",
    val customerLocationUrl: String = "",
    val customerLatitude: Double? = null,
    val customerLongitude: Double? = null
) {
    fun getEffectiveCustomerMapsUrl(): String? {
        if (customerLocationUrl.isNotBlank()) return customerLocationUrl.trim()
        if (customerLatitude != null && customerLongitude != null && (customerLatitude != 0.0 || customerLongitude != 0.0)) {
            return "https://maps.google.com/?q=$customerLatitude,$customerLongitude"
        }
        return null
    }
    val isPendingSync: Boolean
        get() = syncStatus != RecordSyncState.SYNCED
    val isPreSale: Boolean
        get() = orderType == "pre_sale"
    val isPendingPreSale: Boolean
        get() = orderType == "pre_sale" && status == "pending"
    val isCompletedPreSale: Boolean
        get() = orderType == "pre_sale" && status == "completed"
    val isCancelledPreSale: Boolean
        get() = status == "cancelled"
    val isReturnRequested: Boolean
        get() = status == "return_requested"

    private fun parseReturnTagMap(): Map<String, String> {
        val tagStart = notes.indexOf("[RETURN_REQ|")
        if (tagStart == -1) return emptyMap()
        val tagEnd = notes.indexOf("]", tagStart)
        if (tagEnd == -1) return emptyMap()
        val inner = notes.substring(tagStart + "[RETURN_REQ|".length, tagEnd)
        val result = mutableMapOf<String, String>()
        inner.split("|").forEach { part ->
            val idx = part.indexOf("=")
            if (idx > 0) {
                val key = part.substring(0, idx).trim()
                val value = part.substring(idx + 1).trim()
                result[key] = value
            }
        }
        return result
    }

    val returnRequestWorkerName: String
        get() = parseReturnTagMap()["workerName"]?.takeIf { it.isNotBlank() } ?: "عامل المبيعات"

    val returnRequestWorkerUid: String
        get() = parseReturnTagMap()["workerUid"].orEmpty()

    val returnRequestManagerUid: String
        get() = parseReturnTagMap()["managerUid"].orEmpty()

    val returnRequestManagerEmail: String
        get() = parseReturnTagMap()["managerEmail"].orEmpty()

    val returnRequestManagerName: String
        get() = parseReturnTagMap()["managerName"]?.takeIf { it.isNotBlank() } ?: "المدير المسؤول"

    val returnRequestReason: String
        get() = parseReturnTagMap()["reason"].orEmpty()

    val returnRequestTimestamp: Long
        get() = parseReturnTagMap()["time"]?.toLongOrNull() ?: dateMillis

    val returnRequestFormattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(returnRequestTimestamp))

    val cleanNotesWithoutReturnTag: String
        get() = notes.replace(Regex("""\s*\[RETURN_REQ\|[^\]]*\]"""), "").trim()

    companion object {
        fun buildNotesWithReturnRequest(
            existingNotes: String,
            workerName: String,
            workerUid: String,
            managerUid: String,
            managerEmail: String,
            managerName: String,
            reason: String,
            timestamp: Long = System.currentTimeMillis()
        ): String {
            val cleanBase = existingNotes.replace(Regex("""\s*\[RETURN_REQ\|[^\]]*\]"""), "").trim()
            val safeWorker = workerName.replace("|", " ").replace("]", " ").trim().ifBlank { "عامل المبيعات" }
            val safeManager = managerName.replace("|", " ").replace("]", " ").trim().ifBlank { "المدير المسؤول" }
            val safeReason = reason.replace("|", " ").replace("]", " ").trim()
            val tag = "[RETURN_REQ|workerName=$safeWorker|workerUid=$workerUid|managerUid=$managerUid|managerEmail=$managerEmail|managerName=$safeManager|time=$timestamp|reason=$safeReason]"
            return if (cleanBase.isBlank()) tag else "$cleanBase $tag"
        }
    }
}

enum class InvoiceStyle(val id: String, val displayNameAr: String, val descriptionAr: String) {
    BON_DE_LIVRAISON("BON_DE_LIVRAISON", "Bon de livraison", "وصل تسليم شامل بتفاصيل العبوات والكرتونة والباركود"),
    THERMAL_DETAILED("THERMAL_DETAILED", "وصل التسليم", "يعرض تفاصيل الأصناف والعبوات والخصومات والمديونية"),
    THERMAL_POS("THERMAL_POS", "حراري POS", "تصميم مدمج وسريع لطابعات الكاشير الحرارية 80mm"),
    SIMPLE("SIMPLE", "Bon de commande", "طلبية / فاتورة رسمية للمشاركة بصيغة PDF"),
    TEMPLATE_K("TEMPLATE_K", "قالب K", "قالب K بنمط العبوات [الكرتونة] والأعمدة اللاتينية والديون"),
    DETAILED("DETAILED", "فاتورة A4 شاملة", "فاتورة ضريبية وتجارية مفصلة مع ترويسة المتجر")
}

/**
 * Packaging calculation engine for invoice templates (specifically Template K):
 * - If unitsPerCarton > 1:
 *     - Full cartons: "[3]"
 *     - Mixed cartons and units: "[2] +4"
 *     - Units only (e.g. 6 sold out of 12 pack capacity): "[0] +6"
 * - If unitsPerCarton <= 1: "[quantitySold]"
 */
fun formatPackagingLabel(quantitySold: Int, unitsPerCarton: Int): String {
    val pack = unitsPerCarton.coerceAtLeast(1)
    if (pack <= 1) return "[$quantitySold]"
    val cartons = quantitySold / pack
    val loose = quantitySold % pack
    return when {
        cartons > 0 && loose > 0 -> "[$cartons] +$loose"
        cartons > 0 -> "[$cartons]"
        else -> "[0] +$loose"
    }
}

@Immutable
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status", "createdAt"]),
        Index(value = ["status"]),
        Index(value = ["entityType", "entityId"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: String, // PRODUCT, SALE, CUSTOMER, CUSTOMER_TX, PURCHASE_INVOICE
    val entityId: Int = 0,
    val operation: String, // CREATE, UPDATE, DELETE, CHECKOUT
    val titleAr: String,
    val detailsAr: String = "",
    val status: String = RecordSyncState.PENDING, // PENDING, SYNCED, FAILED
    val retryCount: Int = 0,
    val lastError: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(updatedAt))
}

@Immutable
@Entity(
    tableName = "worker_locations",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isTrackingActive"])
    ]
)
data class WorkerLocation(
    @PrimaryKey val userId: Int,
    val workerName: String,
    val storeName: String = "AF store",
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val isTrackingActive: Boolean = true,
    val statusNote: String = "في وردية العمل",
    val batteryPercent: Int = 100
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss dd/MM", Locale.US).format(Date(timestamp))

    val isRecentlyActive: Boolean
        get() = (System.currentTimeMillis() - timestamp) < 1000 * 60 * 15 // Active in last 15 mins

    fun getGoogleMapsUrl(): String {
        return "https://maps.google.com/?q=$latitude,$longitude"
    }
}

@Immutable
@Entity(
    tableName = "worker_route_coordinates",
    indices = [
        Index(value = ["userId", "timestamp"]),
        Index(value = ["timestamp"])
    ]
)
data class WorkerRouteCoordinate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Int,
    val workerName: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Float = 0f,
    val accuracy: Float = 0f,
    val statusNote: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestamp))
}


