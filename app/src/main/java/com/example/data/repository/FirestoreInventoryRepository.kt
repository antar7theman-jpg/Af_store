package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.models.FirestoreProduct
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
 * Repository for managing inventory products in Firestore.
 * Supports real-time snapshots, scoped per-user collection access, and atomic updates.
 */
class FirestoreInventoryRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        com.example.data.firebase.StaffAccountRepository.getFirestoreDb(context)
    )

    private val auth = Firebase.auth

    /**
     * Retrieves the currently authenticated user's ID or throws if unauthenticated.
     */
    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول باستخدام حساب Google أولاً قبل التعامل مع قاعدة البيانات.")
    }

    /**
     * Real-time observation of user's products collection via Kotlin Flow snapshots.
     */
    fun observeProducts(): Flow<List<FirestoreProduct>> = flow {
        val uid = requireUserId()
        val productsRef = db.collection("users").document(uid).collection("products")

        emitAll(
            productsRef
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestoreProduct::class.java)
                }
                .catch { error ->
                    Log.e("FirestoreInventory", "Error observing products: ${error.message}", error)
                    throw error
                }
        )
    }

    /**
     * Get a product by barcode for the current user.
     */
    suspend fun getProductByBarcode(barcode: String): FirestoreProduct? {
        val uid = requireUserId()
        val query = db.collection("users")
            .document(uid)
            .collection("products")
            .whereEqualTo("barcode", barcode.trim())
            .limit(1)
            .get()
            .await()

        return query.documents.firstOrNull()?.toObject(FirestoreProduct::class.java)
    }

    /**
     * Add or Update a Product in Firestore.
     */
    suspend fun upsertProduct(product: FirestoreProduct): String {
        val uid = requireUserId()
        val collection = db.collection("users").document(uid).collection("products")

        val docRef = if (product.id.isNotBlank()) {
            collection.document(product.id)
        } else {
            collection.document()
        }

        val payload = hashMapOf<String, Any?>(
            "userId" to uid,
            "name" to product.name,
            "barcode" to product.barcode,
            "category" to product.category,
            "purchasePrice" to product.purchasePrice,
            "salePrice" to product.salePrice,
            "quantityInUnits" to product.quantityInUnits,
            "unitsPerCarton" to product.unitsPerCarton,
            "minStockThreshold" to product.minStockThreshold,
            "imageUrl" to product.imageUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (product.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(payload, SetOptions.merge()).await()
        return docRef.id
    }

    /**
     * Atomically adjust product stock quantity by delta.
     */
    suspend fun updateStockQuantity(productId: String, deltaUnits: Int) {
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("products")
            .document(productId)

        val updates = mapOf(
            "quantityInUnits" to FieldValue.increment(deltaUnits.toLong()),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.update(updates).await()
    }

    /**
     * Delete a product from Firestore.
     */
    suspend fun deleteProduct(productId: String) {
        val uid = requireUserId()
        db.collection("users")
            .document(uid)
            .collection("products")
            .document(productId)
            .delete()
            .await()
    }
}
