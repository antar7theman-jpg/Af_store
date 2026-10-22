package com.example.data.models

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Modern Firestore Product Model for Inventory Management.
 * Includes explicit zero-arg default constructor parameters for safe Firebase deserialization.
 */
@Immutable
data class FirestoreProduct(
    @DocumentId
    val id: String = "",

    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("barcode")
    @set:PropertyName("barcode")
    var barcode: String = "",

    @get:PropertyName("cartonBarcode")
    @set:PropertyName("cartonBarcode")
    var cartonBarcode: String = "",

    @get:PropertyName("category")
    @set:PropertyName("category")
    var category: String = "عام",

    @get:PropertyName("purchasePrice")
    @set:PropertyName("purchasePrice")
    var purchasePrice: Double = 0.0,

    @get:PropertyName("salePrice")
    @set:PropertyName("salePrice")
    var salePrice: Double = 0.0,

    @get:PropertyName("quantityInUnits")
    @set:PropertyName("quantityInUnits")
    var quantityInUnits: Int = 0,

    @get:PropertyName("unitsPerCarton")
    @set:PropertyName("unitsPerCarton")
    var unitsPerCarton: Int = 1,

    @get:PropertyName("minStockThreshold")
    @set:PropertyName("minStockThreshold")
    var minStockThreshold: Int = 5,

    @get:PropertyName("imageUrl")
    @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @ServerTimestamp
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,

    @ServerTimestamp
    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Timestamp? = null
) {
    val quantityInCartons: Double
        get() = if (unitsPerCarton > 0) quantityInUnits.toDouble() / unitsPerCarton else 0.0

    val remainingLooseUnits: Int
        get() = if (unitsPerCarton > 0) quantityInUnits % unitsPerCarton else quantityInUnits

    val profitMarginAmount: Double
        get() = (salePrice - purchasePrice).coerceAtLeast(0.0)

    val profitMarginPercent: Double
        get() = if (salePrice > 0.0) (profitMarginAmount / salePrice) * 100.0 else 0.0

    val isLowStock: Boolean
        get() = quantityInUnits <= minStockThreshold
}

fun FirestoreProduct.toProduct(localId: Int = 0): Product {
    return Product(
        id = localId,
        firestoreId = id,
        userId = userId,
        name = name,
        purchasePrice = purchasePrice,
        price = salePrice,
        stock = quantityInUnits,
        unitsPerCarton = unitsPerCarton,
        barcode = barcode,
        cartonBarcode = cartonBarcode,
        imagePath = imageUrl,
        imageUrl = imageUrl,
        category = category,
        minStockAlert = minStockThreshold,
        createdAt = createdAt,
        serverUpdatedAt = updatedAt
    )
}

fun Product.toFirestoreProduct(): FirestoreProduct {
    return FirestoreProduct(
        id = firestoreId,
        userId = userId,
        name = name,
        barcode = barcode,
        cartonBarcode = cartonBarcode,
        category = category,
        purchasePrice = purchasePrice,
        salePrice = price,
        quantityInUnits = stock,
        unitsPerCarton = unitsPerCarton,
        minStockThreshold = minStockAlert,
        imageUrl = imageUrl.ifBlank { imagePath },
        createdAt = createdAt,
        updatedAt = serverUpdatedAt
    )
}
