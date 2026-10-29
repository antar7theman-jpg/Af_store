package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.Promotion
import com.example.data.models.PromotionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PromotionRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _promotions = MutableStateFlow<List<Promotion>>(emptyList())
    val promotions: StateFlow<List<Promotion>> = _promotions.asStateFlow()

    init {
        loadPromotions()
    }

    @Synchronized
    fun loadPromotions() {
        val rawJson = prefs.getString(KEY_PROMOTIONS_LIST, null)
        if (rawJson.isNullOrBlank()) {
            val defaults = createDefaultStarterPromotions()
            _promotions.value = defaults
            persistPromotions(defaults)
        } else {
            try {
                val array = JSONArray(rawJson)
                val list = mutableListOf<Promotion>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parsePromotion(obj))
                }
                _promotions.value = list
            } catch (_: Exception) {
                val defaults = createDefaultStarterPromotions()
                _promotions.value = defaults
                persistPromotions(defaults)
            }
        }
    }

    @Synchronized
    fun savePromotion(promo: Promotion) {
        val current = _promotions.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == promo.id }
        if (existingIndex >= 0) {
            current[existingIndex] = promo
        } else {
            current.add(0, promo)
        }
        _promotions.value = current
        persistPromotions(current)
    }

    @Synchronized
    fun deletePromotion(promoId: String) {
        val current = _promotions.value.filterNot { it.id == promoId }
        _promotions.value = current
        persistPromotions(current)
    }

    @Synchronized
    fun togglePromotionActive(promoId: String, isActive: Boolean) {
        val current = _promotions.value.map {
            if (it.id == promoId) it.copy(isActive = isActive) else it
        }
        _promotions.value = current
        persistPromotions(current)
    }

    @Synchronized
    fun recordPromotionUsage(promoId: String) {
        val current = _promotions.value.map {
            if (it.id == promoId) it.copy(usedCount = it.usedCount + 1) else it
        }
        _promotions.value = current
        persistPromotions(current)
    }

    fun findCouponByCode(code: String): Promotion? {
        val clean = code.trim().uppercase()
        if (clean.isBlank()) return null
        return _promotions.value.find {
            it.code.trim().uppercase() == clean && it.isAvailable
        }
    }

    private fun persistPromotions(list: List<Promotion>) {
        try {
            val array = JSONArray()
            for (p in list) {
                array.put(serializePromotion(p))
            }
            prefs.edit().putString(KEY_PROMOTIONS_LIST, array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun serializePromotion(p: Promotion): JSONObject {
        return JSONObject().apply {
            put("id", p.id)
            put("code", p.code)
            put("title", p.title)
            put("description", p.description)
            put("type", p.type.name)
            put("discountValue", p.discountValue)
            if (p.targetProductId != null) put("targetProductId", p.targetProductId)
            put("targetProductName", p.targetProductName)
            put("minQuantity", p.minQuantity)
            put("rewardFreeQuantity", p.rewardFreeQuantity)
            put("minCartTotal", p.minCartTotal)
            put("isActive", p.isActive)
            put("maxUses", p.maxUses)
            put("usedCount", p.usedCount)
            put("createdAtMillis", p.createdAtMillis)
            put("endDateMillis", p.endDateMillis)
        }
    }

    private fun parsePromotion(obj: JSONObject): Promotion {
        val typeStr = obj.optString("type", PromotionType.PERCENTAGE_DISCOUNT.name)
        val type = try {
            PromotionType.valueOf(typeStr)
        } catch (_: Exception) {
            PromotionType.PERCENTAGE_DISCOUNT
        }
        return Promotion(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            code = obj.optString("code", ""),
            title = obj.optString("title", "عرض ترويجي"),
            description = obj.optString("description", ""),
            type = type,
            discountValue = obj.optDouble("discountValue", 0.0),
            targetProductId = if (obj.has("targetProductId")) obj.optInt("targetProductId") else null,
            targetProductName = obj.optString("targetProductName", ""),
            minQuantity = obj.optInt("minQuantity", 1),
            rewardFreeQuantity = obj.optInt("rewardFreeQuantity", 0),
            minCartTotal = obj.optDouble("minCartTotal", 0.0),
            isActive = obj.optBoolean("isActive", true),
            maxUses = obj.optInt("maxUses", 0),
            usedCount = obj.optInt("usedCount", 0),
            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
            endDateMillis = obj.optLong("endDateMillis", 0L)
        )
    }

    private fun createDefaultStarterPromotions(): List<Promotion> {
        return listOf(
            Promotion(
                id = "promo_starter_1",
                code = "WELCOME10",
                title = "كوبون ترحيبي 10%",
                description = "خصم ترحيبي 10% على إجمالي الفاتورة عند الشراء بمبلغ 1,500 د.ج أو أكثر",
                type = PromotionType.PERCENTAGE_DISCOUNT,
                discountValue = 10.0,
                minCartTotal = 1500.0,
                isActive = true
            ),
            Promotion(
                id = "promo_starter_2",
                code = "SAVE500",
                title = "خصم التوفير 500 د.ج",
                description = "تخفيض 500 د.ج فوري على الفواتير التي تتجاوز 5,000 د.ج",
                type = PromotionType.FIXED_AMOUNT_DISCOUNT,
                discountValue = 500.0,
                minCartTotal = 5000.0,
                isActive = true
            ),
            Promotion(
                id = "promo_starter_3",
                code = "",
                title = "عرض كميات الجملة (3 قطع فما فوق)",
                description = "خصم 15% تلقائي عند شراء 3 قطع أو أكثر من نفس الصنف",
                type = PromotionType.QUANTITY_TIER_DISCOUNT,
                discountValue = 15.0,
                minQuantity = 3,
                isActive = true
            ),
            Promotion(
                id = "promo_starter_4",
                code = "",
                title = "عرض التوفير (اشترِ 3 واحصل على 1 مجاناً)",
                description = "عرض BOGO تلقائي يمنح قطعة مجانية عند شراء 3 قطع",
                type = PromotionType.BUY_X_GET_Y_FREE,
                minQuantity = 3,
                rewardFreeQuantity = 1,
                isActive = true
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "afstore_promotions_prefs"
        private const val KEY_PROMOTIONS_LIST = "promotions_json_v1"
    }
}
