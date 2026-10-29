package com.example.data.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ExpenseCategory(
    val id: String,
    val labelAr: String,
    val subtitleAr: String
) {
    RENT("RENT", "إيجار المحل والمستودع", "تكاليف إيجار المقرات والمستودعات"),
    UTILITIES("UTILITIES", "فواتير وخدمات (كهرباء، ماء، إنترنت)", "فواتير الكهرباء، الغاز، الماء، والاتصالات"),
    SALARIES("SALARIES", "رواتب وأجور العمال", "مستحقات الموظفين والعمال والمناديب"),
    TRANSPORT("TRANSPORT", "نقل ووقود وتوصيل", "بنزين، شحن البضائع، ومصاريف المركبات"),
    SUPPLIES("SUPPLIES", "مستلزمات وأكياس وتغليف", "أكياس، رولات الفواتير، ومواد التعبئة"),
    MAINTENANCE("MAINTENANCE", "صيانة وإصلاحات", "صيانة الأجهزة، الطابعات، والإنارة"),
    TAXES_FEES("TAXES_FEES", "ضرائب ورسوم قانونية", "الرسوم البلدية، السجل التجاري، والمحاسبة"),
    MARKETING("MARKETING", "تسويق وإعلانات", "دعاية وترويج المتجر والمطبوعات"),
    OTHER("OTHER", "مصاريف نثرية أخرى", "نثريات وضيافة ومصاريف يومية متنوعة");

    companion object {
        fun fromId(id: String?): ExpenseCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val dateMillis: Long = System.currentTimeMillis(),
    val paymentMethod: String = "CASH", // "CASH", "BANK_TRANSFER", "CHECK"
    val paidBy: String = "",
    val receiptNumber: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(dateMillis))

    val formattedTime: String
        get() = SimpleDateFormat("HH:mm", Locale.US).format(Date(dateMillis))

    val paymentMethodAr: String
        get() = when (paymentMethod) {
            "BANK_TRANSFER" -> "تحويل بنكي"
            "CHECK" -> "شيك / صك"
            else -> "نقداً (كاش)"
        }
}
