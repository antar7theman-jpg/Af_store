package com.example.data.dao

import com.example.data.models.Product
import kotlinx.coroutines.flow.Flow

/**
 * واجهة FirestoreProductDao للتعامل مع عمليات CRUD والمزامنة اللحظية لمنتجات المخزون في Firestore.
 */
interface FirestoreProductDao {

    /**
     * مراقبة كافة منتجات المستخدم لحظياً عبر تدفق Kotlin Flow.
     */
    fun observeAllProducts(): Flow<List<Product>>

    /**
     * جلب منتج محدد بواسطة المعرف الفريد في Firestore (firestoreId).
     */
    suspend fun getProductById(productId: String): Product?

    /**
     * جلب منتج بواسطة باركود القطعة أو باركود الكرتون.
     */
    suspend fun getProductByBarcode(barcode: String): Product?

    /**
     * إضافة منتج جديد إلى Firestore وإعادة معرف الوثيقة المنشأة.
     */
    suspend fun addProduct(product: Product): String

    /**
     * تحديث بيانات منتج موجود في Firestore.
     */
    suspend fun updateProduct(product: Product)

    /**
     * إضافة أو تحديث منتج (Upsert).
     */
    suspend fun upsertProduct(product: Product): String

    /**
     * حذف منتج من Firestore بواسطة المعرف.
     */
    suspend fun deleteProduct(productId: String)

    /**
     * تعديل الكمية المتوفرة بالمخزون ذرياً بمقدار التغير (deltaUnits).
     */
    suspend fun updateStockQuantity(productId: String, deltaUnits: Int)

    /**
     * البحث عن منتجات حسب اسم الفئة / التصنيف.
     */
    suspend fun getProductsByCategory(category: String): List<Product>
}
