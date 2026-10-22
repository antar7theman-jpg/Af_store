package com.example.data.dao

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.models.Product
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * تنفيذ واجهة FirestoreProductDao لإدارة عمليات البيانات في Firestore.
 * يدعم الاستعلامات لكل مستخدم مستنداً إلى UID الحساب المسجل (users/{userId}/products).
 */
class FirestoreProductDaoImpl(
    private val db: FirebaseFirestore,
    private val context: Context? = null
) : FirestoreProductDao {

    constructor(context: Context) : this(
        com.example.data.firebase.StaffAccountRepository.getFirestoreDb(context),
        context
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول أولاً للوصول إلى قاعدة بيانات Firestore.")
    }

    private fun getTargetUserId(): String {
        val authUid = auth.currentUser?.uid ?: ""
        if (context != null) {
            val session = com.example.data.firebase.StaffAccountRepository.loadPersistentStaffSession(context)
            if (session != null) {
                // If worker and has parentManagerUid, use parentManagerUid
                if (session.role.equals("worker", ignoreCase = true) && session.parentManagerUid.isNotBlank()) {
                    return session.parentManagerUid
                }
                // If manager or owner, use session.userId
                if (session.userId.isNotBlank() && !session.userId.startsWith("saved_session")) {
                    return session.userId
                }
            }
        }
        return authUid.ifBlank { requireUserId() }
    }

    private fun getUserProductsCollection() =
        db.collection("users").document(getTargetUserId()).collection("products")

    override fun observeAllProducts(): Flow<List<Product>> = flow {
        val productsRef = getUserProductsCollection()

        emitAll(
            productsRef
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.apply {
                            if (firestoreId.isBlank()) {
                                firestoreId = doc.id
                            }
                        }
                    }
                }
                .catch { error ->
                    Log.e("FirestoreProductDao", "خطأ أثناء مراقبة قائمة المنتجات: ${error.message}", error)
                    throw error
                }
        )
    }

    override suspend fun getProductById(productId: String): Product? {
        if (productId.isBlank()) return null
        val docSnapshot = getUserProductsCollection().document(productId).get().await()
        return docSnapshot.toObject(Product::class.java)?.apply {
            if (firestoreId.isBlank()) {
                firestoreId = docSnapshot.id
            }
        }
    }

    override suspend fun getProductByBarcode(barcode: String): Product? {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return null

        val collection = getUserProductsCollection()

        // 1. البحث بباركود القطعة الرئيسي
        val queryByUnit = collection
            .whereEqualTo("barcode", cleanBarcode)
            .limit(1)
            .get()
            .await()

        val unitProductDoc = queryByUnit.documents.firstOrNull()
        if (unitProductDoc != null) {
            return unitProductDoc.toObject(Product::class.java)?.apply {
                if (firestoreId.isBlank()) {
                    firestoreId = unitProductDoc.id
                }
            }
        }

        // 2. البحث بباركود الكرتون الفرعي
        val queryByCarton = collection
            .whereEqualTo("cartonBarcode", cleanBarcode)
            .limit(1)
            .get()
            .await()

        val cartonProductDoc = queryByCarton.documents.firstOrNull()
        return cartonProductDoc?.toObject(Product::class.java)?.apply {
            if (firestoreId.isBlank()) {
                firestoreId = cartonProductDoc.id
            }
        }
    }

    override suspend fun addProduct(product: Product): String {
        val uid = requireUserId()
        val collection = getUserProductsCollection()
        val docRef = collection.document()

        val payload = hashMapOf<String, Any?>(
            "id" to product.id,
            "firestoreId" to docRef.id,
            "userId" to uid,
            "name" to product.name,
            "purchasePrice" to product.purchasePrice,
            "price" to product.price,
            "stock" to product.stock,
            "unitsPerCarton" to product.unitsPerCarton,
            "barcode" to product.barcode,
            "cartonBarcode" to product.cartonBarcode,
            "category" to product.category,
            "minStockAlert" to product.minStockAlert,
            "imagePath" to product.imagePath,
            "imageUrl" to product.imageUrl,
            "isDeleted" to product.isDeleted,
            "syncStatus" to "SYNCED",
            "createdAt" to FieldValue.serverTimestamp(),
            "serverUpdatedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to System.currentTimeMillis()
        )

        docRef.set(payload, SetOptions.merge()).await()
        return docRef.id
    }

    override suspend fun updateProduct(product: Product) {
        val targetId = product.firestoreId
        if (targetId.isBlank()) {
            throw IllegalArgumentException("لا يمكن تحديث منتج بدون firestoreId.")
        }

        val uid = requireUserId()
        val docRef = getUserProductsCollection().document(targetId)

        val payload = hashMapOf<String, Any?>(
            "id" to product.id,
            "firestoreId" to targetId,
            "userId" to uid,
            "name" to product.name,
            "purchasePrice" to product.purchasePrice,
            "price" to product.price,
            "stock" to product.stock,
            "unitsPerCarton" to product.unitsPerCarton,
            "barcode" to product.barcode,
            "cartonBarcode" to product.cartonBarcode,
            "category" to product.category,
            "minStockAlert" to product.minStockAlert,
            "imagePath" to product.imagePath,
            "imageUrl" to product.imageUrl,
            "isDeleted" to product.isDeleted,
            "syncStatus" to "SYNCED",
            "serverUpdatedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to System.currentTimeMillis()
        )

        docRef.set(payload, SetOptions.merge()).await()
    }

    override suspend fun upsertProduct(product: Product): String {
        return if (product.firestoreId.isNotBlank()) {
            updateProduct(product)
            product.firestoreId
        } else {
            addProduct(product)
        }
    }

    override suspend fun deleteProduct(productId: String) {
        if (productId.isBlank()) return
        getUserProductsCollection().document(productId).delete().await()
    }

    override suspend fun updateStockQuantity(productId: String, deltaUnits: Int) {
        if (productId.isBlank()) return
        val docRef = getUserProductsCollection().document(productId)

        val updates = mapOf(
            "stock" to FieldValue.increment(deltaUnits.toLong()),
            "serverUpdatedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to System.currentTimeMillis()
        )

        docRef.update(updates).await()
    }

    override suspend fun getProductsByCategory(category: String): List<Product> {
        val cleanCategory = category.trim()
        if (cleanCategory.isBlank()) return emptyList()

        val snapshot = getUserProductsCollection()
            .whereEqualTo("category", cleanCategory)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Product::class.java)?.apply {
                if (firestoreId.isBlank()) {
                    firestoreId = doc.id
                }
            }
        }
    }
}
