package com.example.data.models

import java.util.Locale
import java.util.UUID

enum class PromotionType(val labelAr: String, val subtitleAr: String) {
    PERCENTAGE_DISCOUNT("خصم نسبة مئوية (%)", "تخفيض نسبة محددة من إجمالي الفاتورة"),
    FIXED_AMOUNT_DISCOUNT("خصم مبلغ ثابت (د.ج)", "تخفيض مبلغ مالي محدد من إجمالي الفاتورة"),
    QUANTITY_TIER_DISCOUNT("عرض الكميات والجملة", "تخفيض السعر عند شراء كمية معينة من صنف محدد"),
    BUY_X_GET_Y_FREE("اشترِ X واحصل على Y مجاناً", "منح قطع إضافية مجانية عند شراء كمية معينة")
}

data class Promotion(
    val id: String = UUID.randomUUID().toString(),
    val code: String = "", // e.g. "WELCOME10", blank if auto-applied rule
    val title: String = "",
    val description: String = "",
    val type: PromotionType = PromotionType.PERCENTAGE_DISCOUNT,
    val discountValue: Double = 0.0, // percent or cash amount
    val targetProductId: Int? = null,
    val targetProductName: String = "",
    val minQuantity: Int = 1,
    val rewardFreeQuantity: Int = 0,
    val minCartTotal: Double = 0.0,
    val isActive: Boolean = true,
    val maxUses: Int = 0, // 0 = unlimited
    val usedCount: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long = 0L // 0 = no expiration
) {
    val isCoupon: Boolean
        get() = code.isNotBlank()

    val isExpired: Boolean
        get() = endDateMillis > 0L && System.currentTimeMillis() > endDateMillis

    val isAvailable: Boolean
        get() = isActive && !isExpired && (maxUses <= 0 || usedCount < maxUses)

    fun formattedSummary(currencySymbol: String = "د.ج"): String {
        return when (type) {
            PromotionType.PERCENTAGE_DISCOUNT -> {
                val valueStr = if (discountValue % 1.0 == 0.0) "${discountValue.toInt()}%" else "${String.format(Locale.US, "%.1f", discountValue)}%"
                "خصم $valueStr على الفاتورة"
            }
            PromotionType.FIXED_AMOUNT_DISCOUNT -> {
                val valueStr = if (discountValue % 1.0 == 0.0) "${discountValue.toInt()}" else String.format(Locale.US, "%.2f", discountValue)
                "خصم $valueStr $currencySymbol من الفاتورة"
            }
            PromotionType.QUANTITY_TIER_DISCOUNT -> {
                val valueStr = if (discountValue % 1.0 == 0.0) "${discountValue.toInt()}%" else "${String.format(Locale.US, "%.1f", discountValue)}%"
                "عند شراء $minQuantity قطع فما فوق: خصم $valueStr"
            }
            PromotionType.BUY_X_GET_Y_FREE -> {
                "اشترِ $minQuantity واحصل على $rewardFreeQuantity مجاناً"
            }
        }
    }
}
