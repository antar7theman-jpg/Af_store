package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.DamagedItemRecord
import com.example.data.models.ExpiryStatus
import com.example.data.models.ProductExpiryRecord
import com.example.data.models.SpoilageReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class ExpiryAndWasteRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("antar_store_expiry_waste_prefs", Context.MODE_PRIVATE)

    private val _expiryRecords = MutableStateFlow<Map<Int, ProductExpiryRecord>>(emptyMap())
    val expiryRecords: StateFlow<Map<Int, ProductExpiryRecord>> = _expiryRecords.asStateFlow()

    private val _damagedItems = MutableStateFlow<List<DamagedItemRecord>>(emptyList())
    val damagedItems: StateFlow<List<DamagedItemRecord>> = _damagedItems.asStateFlow()

    init {
        loadAll()
    }

    @Synchronized
    fun loadAll() {
        loadExpiryRecords()
        loadDamagedItems()
    }

    @Synchronized
    private fun loadExpiryRecords() {
        val rawJson = prefs.getString(PREF_KEY_EXPIRY_MAP, null)
        val map = mutableMapOf<Int, ProductExpiryRecord>()
        if (rawJson.isNullOrBlank()) {
            val seeded = seedInitialExpiryRecords()
            persistExpiryMap(seeded)
            map.putAll(seeded)
        } else {
            try {
                val array = JSONArray(rawJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val prodId = obj.getInt("productId")
                    map[prodId] = ProductExpiryRecord(
                        productId = prodId,
                        expiryDateMillis = obj.getLong("expiryDateMillis"),
                        productionDateMillis = if (obj.has("productionDateMillis")) obj.getLong("productionDateMillis") else null,
                        batchNumber = obj.optString("batchNumber", ""),
                        notes = obj.optString("notes", ""),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                }
            } catch (e: Exception) {
                val seeded = seedInitialExpiryRecords()
                map.putAll(seeded)
            }
        }
        _expiryRecords.value = map
    }

    @Synchronized
    private fun loadDamagedItems() {
        val rawJson = prefs.getString(PREF_KEY_DAMAGED_LIST, null)
        val list = mutableListOf<DamagedItemRecord>()
        if (rawJson.isNullOrBlank()) {
            val seeded = seedInitialDamagedItems()
            persistDamagedItems(seeded)
            list.addAll(seeded)
        } else {
            try {
                val array = JSONArray(rawJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        DamagedItemRecord(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            productId = obj.optInt("productId", 0),
                            productName = obj.optString("productName", "صنف تالف"),
                            productBarcode = obj.optString("productBarcode", ""),
                            quantity = obj.optInt("quantity", 1),
                            unitCost = obj.optDouble("unitCost", 0.0),
                            totalLoss = obj.optDouble("totalLoss", 0.0),
                            reason = SpoilageReason.fromId(obj.optString("reason", "OTHER")),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            recordedBy = obj.optString("recordedBy", ""),
                            notes = obj.optString("notes", ""),
                            batchNumber = obj.optString("batchNumber", ""),
                            imagePath = obj.optString("imagePath", ""),
                            deductedFromStock = obj.optBoolean("deductedFromStock", true),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                list.sortByDescending { it.dateMillis }
            } catch (e: Exception) {
                val seeded = seedInitialDamagedItems()
                list.addAll(seeded)
            }
        }
        _damagedItems.value = list
    }

    @Synchronized
    fun saveExpiryRecord(record: ProductExpiryRecord) {
        val current = _expiryRecords.value.toMutableMap()
        current[record.productId] = record.copy(updatedAt = System.currentTimeMillis())
        _expiryRecords.value = current
        persistExpiryMap(current)
    }

    @Synchronized
    fun removeExpiryRecord(productId: Int) {
        val current = _expiryRecords.value.toMutableMap()
        current.remove(productId)
        _expiryRecords.value = current
        persistExpiryMap(current)
    }

    fun getExpiryRecord(productId: Int): ProductExpiryRecord? {
        return _expiryRecords.value[productId]
    }

    @Synchronized
    fun saveDamagedItem(item: DamagedItemRecord) {
        val current = _damagedItems.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            current[idx] = item.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(0, item.copy(updatedAt = System.currentTimeMillis()))
        }
        val sorted = current.sortedByDescending { it.dateMillis }
        _damagedItems.value = sorted
        persistDamagedItems(sorted)
    }

    @Synchronized
    fun deleteDamagedItem(id: String) {
        val filtered = _damagedItems.value.filter { it.id != id }
        _damagedItems.value = filtered
        persistDamagedItems(filtered)
    }

    fun getTotalLossThisMonth(): Double {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis
        return _damagedItems.value.filter { it.dateMillis >= monthStart }.sumOf { it.totalLoss }
    }

    fun getTotalLossToday(): Double {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        return _damagedItems.value.filter { it.dateMillis >= todayStart }.sumOf { it.totalLoss }
    }

    private fun persistExpiryMap(map: Map<Int, ProductExpiryRecord>) {
        try {
            val array = JSONArray()
            map.values.forEach { rec ->
                val obj = JSONObject()
                obj.put("productId", rec.productId)
                obj.put("expiryDateMillis", rec.expiryDateMillis)
                rec.productionDateMillis?.let { obj.put("productionDateMillis", it) }
                obj.put("batchNumber", rec.batchNumber)
                obj.put("notes", rec.notes)
                obj.put("updatedAt", rec.updatedAt)
                array.put(obj)
            }
            prefs.edit().putString(PREF_KEY_EXPIRY_MAP, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistDamagedItems(list: List<DamagedItemRecord>) {
        try {
            val array = JSONArray()
            list.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("productId", item.productId)
                obj.put("productName", item.productName)
                obj.put("productBarcode", item.productBarcode)
                obj.put("quantity", item.quantity)
                obj.put("unitCost", item.unitCost)
                obj.put("totalLoss", item.totalLoss)
                obj.put("reason", item.reason.id)
                obj.put("dateMillis", item.dateMillis)
                obj.put("recordedBy", item.recordedBy)
                obj.put("notes", item.notes)
                obj.put("batchNumber", item.batchNumber)
                obj.put("imagePath", item.imagePath)
                obj.put("deductedFromStock", item.deductedFromStock)
                obj.put("updatedAt", item.updatedAt)
                array.put(obj)
            }
            prefs.edit().putString(PREF_KEY_DAMAGED_LIST, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun seedInitialExpiryRecords(): Map<Int, ProductExpiryRecord> {
        val map = mutableMapOf<Int, ProductExpiryRecord>()
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000

        // Example: product ID 1 near expiry (12 days)
        map[1] = ProductExpiryRecord(
            productId = 1,
            expiryDateMillis = now + (12 * oneDay),
            batchNumber = "LOT-2024-B1",
            notes = "دفعة الصيف - وشيكة الانتهاء"
        )
        // Example: product ID 2 safe (120 days)
        map[2] = ProductExpiryRecord(
            productId = 2,
            expiryDateMillis = now + (120 * oneDay),
            batchNumber = "LOT-2024-C8",
            notes = "دفعة جديدة ممتازة"
        )
        // Example: product ID 3 critical (4 days)
        map[3] = ProductExpiryRecord(
            productId = 3,
            expiryDateMillis = now + (4 * oneDay),
            batchNumber = "LOT-2024-A3",
            notes = "عرض ترويجي سريع لتصريف الكمية"
        )
        return map
    }

    private fun seedInitialDamagedItems(): List<DamagedItemRecord> {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        return listOf(
            DamagedItemRecord(
                id = UUID.randomUUID().toString(),
                productId = 1,
                productName = "عطر مسك فاخر",
                productBarcode = "613110022331",
                quantity = 2,
                unitCost = 450.0,
                totalLoss = 900.0,
                reason = SpoilageReason.DAMAGED_TRANSPORT,
                dateMillis = now - (2 * oneDay),
                recordedBy = "Deffas antar",
                notes = "سقوط العبوة من الكرتون أثناء تفريغ شحنة المورد",
                deductedFromStock = true
            ),
            DamagedItemRecord(
                id = UUID.randomUUID().toString(),
                productId = 2,
                productName = "معطر جو فندقي",
                productBarcode = "613110044552",
                quantity = 1,
                unitCost = 300.0,
                totalLoss = 300.0,
                reason = SpoilageReason.SAMPLE_TESTING,
                dateMillis = now - (5 * oneDay),
                recordedBy = "Deffas antar",
                notes = "فتح زجاجة تستر لعرضها على طاولة الاستقبال للزبائن",
                deductedFromStock = true
            )
        )
    }

    companion object {
        private const val PREF_KEY_EXPIRY_MAP = "antar_store_product_expiry_map_v1"
        private const val PREF_KEY_DAMAGED_LIST = "antar_store_damaged_items_log_v1"
    }
}
