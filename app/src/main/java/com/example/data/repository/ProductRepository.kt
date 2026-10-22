package com.example.data.repository

import com.example.data.dao.FirestoreProductDao
import com.example.data.dao.StoreDao
import com.example.data.models.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * مستودع بيانات المنتجات الموحد (ProductRepository).
 * يربط بين FirestoreProductDao و StoreDao مع توفير التعامل الآمن مع الأخطاء
 * وتنفيذ المزامنة اللحظية الشاملة على خيوط الخلفية (Dispatchers.IO).
 */
class ProductRepository(
    private val firestoreProductDao: FirestoreProductDao,
    private val storeDao: StoreDao? = null
) {

    /**
     * تدفق المزامنة اللحظية لكافة المنتجات من Firestore مع تشغيل التدفق على Dispatchers.IO.
     */
    val observeRemoteProducts: Flow<List<Product>> = firestoreProductDao
        .observeAllProducts()
        .flowOn(Dispatchers.IO)

    /**
     * جلب منتج من خلال المعرف السحابي أو المحلي.
     */
    suspend fun getProductById(productId: String): Product? = withContext(Dispatchers.IO) {
        try {
            firestoreProductDao.getProductById(productId)
        } catch (e: Exception) {
            val numericId = productId.toIntOrNull()
            if (numericId != null && storeDao != null) {
                storeDao.getProductById(numericId)
            } else null
        }
    }

    /**
     * البحث المزدوج بباركود القطعة أو الكرتون بشكل سريع وآمن.
     */
    suspend fun getProductByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        try {
            val remoteMatch = firestoreProductDao.getProductByBarcode(barcode)
            if (remoteMatch != null) return@withContext remoteMatch
        } catch (_: Exception) {
            // Fallback to local Room database on error
        }

        if (storeDao != null) {
            storeDao.getProductByBarcode(barcode)
        } else null
    }

    /**
     * حفظ منتج جديد أو تحديث منتج موجود في Firestore ومزامنته محلياً.
     */
    suspend fun saveProduct(product: Product): String = withContext(Dispatchers.IO) {
        val firestoreId = firestoreProductDao.upsertProduct(product)
        if (storeDao != null) {
            val updated = product.copy(firestoreId = firestoreId, syncStatus = "SYNCED")
            storeDao.insertProduct(updated)
        }
        firestoreId
    }

    /**
     * حذف منتج نهائياً بواسطة firestoreId.
     */
    suspend fun deleteProduct(productId: String, localId: Int? = null) = withContext(Dispatchers.IO) {
        try {
            firestoreProductDao.deleteProduct(productId)
        } catch (_: Exception) {}

        if (localId != null && storeDao != null) {
            storeDao.softDeleteProduct(localId)
        }
    }

    /**
     * تعديل الكمية المتوفرة بالمخزون ذرياً بمقدار التغير (deltaUnits).
     */
    suspend fun updateStockQuantity(productId: String, deltaUnits: Int, localId: Int? = null) = withContext(Dispatchers.IO) {
        try {
            firestoreProductDao.updateStockQuantity(productId, deltaUnits)
        } catch (_: Exception) {}

        if (localId != null && storeDao != null) {
            val localProd = storeDao.getProductById(localId)
            if (localProd != null) {
                val newStock = (localProd.stock + deltaUnits).coerceAtLeast(0)
                storeDao.updateProduct(localProd.copy(stock = newStock))
            }
        }
    }
}
