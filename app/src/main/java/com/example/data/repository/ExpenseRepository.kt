package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.Expense
import com.example.data.models.ExpenseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class ExpenseRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("antar_store_expenses_prefs", Context.MODE_PRIVATE)
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    init {
        loadExpenses()
    }

    @Synchronized
    fun loadExpenses(): List<Expense> {
        val rawJson = prefs.getString(PREF_KEY_EXPENSES, null)
        val list = if (rawJson.isNullOrBlank()) {
            val seeded = seedInitialExpenses()
            persistList(seeded)
            seeded
        } else {
            try {
                val array = JSONArray(rawJson)
                val loaded = mutableListOf<Expense>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loaded.add(
                        Expense(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "مصروف عام"),
                            amount = obj.optDouble("amount", 0.0),
                            category = ExpenseCategory.fromId(obj.optString("category", "OTHER")),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            paymentMethod = obj.optString("paymentMethod", "CASH"),
                            paidBy = obj.optString("paidBy", ""),
                            receiptNumber = obj.optString("receiptNumber", ""),
                            notes = obj.optString("notes", ""),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                loaded.sortedByDescending { it.dateMillis }
            } catch (e: Exception) {
                seedInitialExpenses()
            }
        }
        _expenses.value = list
        return list
    }

    @Synchronized
    fun saveExpense(expense: Expense) {
        val current = _expenses.value.toMutableList()
        val index = current.indexOfFirst { it.id == expense.id }
        if (index >= 0) {
            current[index] = expense.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(0, expense.copy(updatedAt = System.currentTimeMillis()))
        }
        val sorted = current.sortedByDescending { it.dateMillis }
        _expenses.value = sorted
        persistList(sorted)
    }

    @Synchronized
    fun deleteExpense(id: String) {
        val updated = _expenses.value.filter { it.id != id }
        _expenses.value = updated
        persistList(updated)
    }

    fun getTotalExpensesBetween(startMillis: Long, endMillis: Long): Double {
        return _expenses.value
            .filter { it.dateMillis in startMillis..endMillis }
            .sumOf { it.amount }
    }

    fun getExpensesBetween(startMillis: Long, endMillis: Long): List<Expense> {
        return _expenses.value
            .filter { it.dateMillis in startMillis..endMillis }
            .sortedByDescending { it.dateMillis }
    }

    fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getCurrentMonthStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun persistList(list: List<Expense>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("amount", item.amount)
                    put("category", item.category.id)
                    put("dateMillis", item.dateMillis)
                    put("paymentMethod", item.paymentMethod)
                    put("paidBy", item.paidBy)
                    put("receiptNumber", item.receiptNumber)
                    put("notes", item.notes)
                    put("updatedAt", item.updatedAt)
                }
                array.put(obj)
            }
            prefs.edit().putString(PREF_KEY_EXPENSES, array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun seedInitialExpenses(): List<Expense> {
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L
        return listOf(
            Expense(
                id = "seed_exp_1",
                title = "فاتورة الكهرباء والإنترنت لشهر أكتوبر",
                amount = 4500.0,
                category = ExpenseCategory.UTILITIES,
                dateMillis = now - (dayMillis * 1),
                paymentMethod = "CASH",
                paidBy = "Deffas antar",
                receiptNumber = "ELEC-1048",
                notes = "سداد فاتورة سونلغاز ومودم المتجر"
            ),
            Expense(
                id = "seed_exp_2",
                title = "شراء أكياس ورقية ورولات فواتير حرارية",
                amount = 2800.0,
                category = ExpenseCategory.SUPPLIES,
                dateMillis = now - (dayMillis * 3),
                paymentMethod = "CASH",
                paidBy = "Deffas antar",
                receiptNumber = "SUP-882",
                notes = "500 كيس متوسط + 10 رولات 80mm"
            ),
            Expense(
                id = "seed_exp_3",
                title = "وقود سيارة توصيل طلبيات الزبائن",
                amount = 1200.0,
                category = ExpenseCategory.TRANSPORT,
                dateMillis = now - (dayMillis * 5),
                paymentMethod = "CASH",
                paidBy = "مندوب التوصيل",
                receiptNumber = "FUEL-391",
                notes = "توصيل طلبيات وسط المدينة والمستودع"
            )
        )
    }

    companion object {
        private const val PREF_KEY_EXPENSES = "stored_expenses_json"
    }
}
