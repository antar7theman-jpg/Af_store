package com.example.data.models

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ExpiryStatus(val labelAr: String) {
    EXPIRED("منتهي الصلاحية"),
    CRITICAL("حرج (أقل من 15 يوم)"),
    NEAR_EXPIRY("وشيك الانتهاء (أقل من 30 يوم)"),
    ATTENTION("انتباه (أقل من 60 يوم)"),
    SAFE("صالح وآمن"),
    NOT_SET("غير محدد")
}

data class ProductExpiryRecord(
    val productId: Int,
    val expiryDateMillis: Long,
    val productionDateMillis: Long? = null,
    val batchNumber: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDaysRemaining(currentMillis: Long = System.currentTimeMillis()): Int {
        val diff = expiryDateMillis - currentMillis
        return (diff / (1000L * 60 * 60 * 24)).toInt()
    }

    fun getStatus(currentMillis: Long = System.currentTimeMillis()): ExpiryStatus {
        val days = getDaysRemaining(currentMillis)
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days <= 15 -> ExpiryStatus.CRITICAL
            days <= 30 -> ExpiryStatus.NEAR_EXPIRY
            days <= 60 -> ExpiryStatus.ATTENTION
            else -> ExpiryStatus.SAFE
        }
    }

    val formattedExpiryDate: String
        get() = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(expiryDateMillis))

    val formattedProductionDate: String?
        get() = productionDateMillis?.let { SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(it)) }
}

enum class SpoilageReason(
    val id: String,
    val labelAr: String,
    val descriptionAr: String,
    val badgeEmoji: String
) {
    EXPIRED("EXPIRED", "انتهاء تاريخ الصلاحية", "انتهت فترة الصلاحية الاستهلاكية للمنتج", "🚨"),
    DAMAGED_TRANSPORT("DAMAGED_TRANSPORT", "كسر أو تلف أثناء النقل", "سقوط أو كسر العبوات أثناء التوريد أو التوصيل", "📦"),
    STORAGE_CONDITION("STORAGE_CONDITION", "سوء تخزين وحرارة / رطوبة", "تلف بسبب التعرض للحرارة، الشمس أو الرطوبة", "☀️"),
    FACTORY_DEFECT("FACTORY_DEFECT", "عيب مصنعي أو تسريب", "خلل مصنعي من الشركة الموردة أو تسريب غطاء", "⚙️"),
    SAMPLE_TESTING("SAMPLE_TESTING", "عينات تجربة وتستر للزبائن", "فتح العبوة لتجربة العطر أو استخدامه كتيستر", "✨"),
    OTHER("OTHER", "أسباب أخرى / جرد دوري", "تالف لأسباب متفرقة أثناء الجرد", "📝");

    companion object {
        fun fromId(id: String): SpoilageReason {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}

data class DamagedItemRecord(
    val id: String = UUID.randomUUID().toString(),
    val productId: Int,
    val productName: String,
    val productBarcode: String = "",
    val quantity: Int,
    val unitCost: Double,
    val totalLoss: Double = quantity * unitCost,
    val reason: SpoilageReason,
    val dateMillis: Long = System.currentTimeMillis(),
    val recordedBy: String = "",
    val notes: String = "",
    val batchNumber: String = "",
    val imagePath: String = "",
    val imageUrl: String = "",
    val deductedFromStock: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(dateMillis))

    val formattedTime: String
        get() = SimpleDateFormat("HH:mm", Locale.US).format(Date(dateMillis))
}
