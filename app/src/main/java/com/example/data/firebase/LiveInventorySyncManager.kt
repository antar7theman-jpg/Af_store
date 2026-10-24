package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.AntarRepository
import com.example.data.database.AntarDatabase
import com.example.data.models.Product
import com.example.data.models.RecordSyncState
import com.example.data.models.ArchivedInvoice
import com.example.data.models.Sale
import com.example.data.models.WorkerLocation
import com.example.utils.ProductCloudImageHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * مدير المزامنة اللحظية الشاملة لمخزون السلع وصورها بين المدير وعماله التابعين.
 * يتيح ربط قاعدة بيانات Room المحلية بمستمع Firestore اللحظي في الخلفية
 * لمزامنة إضافة الأصناف الجديدة، التعديلات، حركة الكميات عند البيع أو الشراء فورياً.
 */
class LiveInventorySyncManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "LiveInventorySync"

        fun getInventoryDocId(product: Product): String {
            return when {
                product.firestoreId.isNotBlank() -> product.firestoreId
                product.barcode.isNotBlank() -> "bc_${product.barcode.trim().replace(Regex("[^a-zA-Z0-9]"), "_")}"
                product.id > 0 -> "pid_${product.id}"
                else -> "p_${System.currentTimeMillis()}"
            }
        }
    }

    private val db: FirebaseFirestore = StaffAccountRepository.getFirestoreDb(context)
    private val auth = Firebase.auth
    private val repository: AntarRepository by lazy {
        val database = AntarDatabase.getDatabase(context.applicationContext)
        AntarRepository(database.storeDao())
    }

    private var productsListenerRegistration: ListenerRegistration? = null
    private var invoicesListenerRegistration: ListenerRegistration? = null
    private var locationsListenerRegistration: ListenerRegistration? = null
    private var currentOwnerUid: String = ""

    private val _isLiveSyncActive = MutableStateFlow(false)
    val isLiveSyncActive: StateFlow<Boolean> = _isLiveSyncActive.asStateFlow()

    private val _isComprehensiveSyncing = MutableStateFlow(false)
    val isComprehensiveSyncing: StateFlow<Boolean> = _isComprehensiveSyncing.asStateFlow()

    private val _syncedProductsCount = MutableStateFlow(0)
    val syncedProductsCount: StateFlow<Int> = _syncedProductsCount.asStateFlow()

    private val _syncedInvoicesCount = MutableStateFlow(0)
    val syncedInvoicesCount: StateFlow<Int> = _syncedInvoicesCount.asStateFlow()

    private val _syncedLocationsCount = MutableStateFlow(0)
    val syncedLocationsCount: StateFlow<Int> = _syncedLocationsCount.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long>(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    data class StockDeduction(
        val productId: Int,
        val barcode: String,
        val firestoreId: String,
        val unitsDeducted: Int,
        val resultingStock: Int
    )

    data class StockIncrease(
        val productId: Int,
        val barcode: String,
        val firestoreId: String,
        val unitsAdded: Int,
        val resultingStock: Int
    )

    /**
     * استخراج المعرف السحابي المشترك لمخزون المدير التابع له العامل.
     */
    fun resolveInventoryOwnerUid(activeAccount: StaffAccount?): String {
        if (activeAccount == null) {
            return auth.currentUser?.uid ?: ""
        }
        return when {
            activeAccount.isRegularManager -> activeAccount.userId
            activeAccount.role.equals("worker", ignoreCase = true) && activeAccount.parentManagerUid.isNotBlank() -> {
                activeAccount.parentManagerUid
            }
            activeAccount.isOwner -> activeAccount.userId.ifBlank { auth.currentUser?.uid ?: "" }
            else -> activeAccount.parentManagerUid.ifBlank { activeAccount.userId.ifBlank { auth.currentUser?.uid ?: "" } }
        }
    }

    /**
     * بدء مستمع المزامنة اللحظية لمخزون السلع وصورها في الخلفية.
     */
    fun startLiveInventorySync(
        activeAccount: StaffAccount,
        onStatusMessage: (String) -> Unit = {}
    ) {
        if (!activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            Log.i(TAG, "الحساب في وضع Just Offline / Lite: تم إيقاف مزامنة المخزون السحابية.")
            _isLiveSyncActive.value = false
            val msg = "ميزة المزامنة غير مفعّلة لهذا الحساب (وضع Just Offline / Lite). تتطلب ترقية لـ Pro mode."
            _syncMessage.value = msg
            onStatusMessage(msg)
            return
        }

        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) {
            Log.w(TAG, "تعذر تحديد معرّف مخزون المدير للمزامنة الحية.")
            return
        }

        if (currentOwnerUid == targetOwnerUid && productsListenerRegistration != null) {
            return
        }

        stopLiveInventorySync()
        currentOwnerUid = targetOwnerUid
        _isLiveSyncActive.value = true

        Log.i(TAG, "بدء مراقبة المخزون اللحظي للمدير: $targetOwnerUid لحساب: ${activeAccount.fullName} (${activeAccount.role})")

        val productsCollection = db.collection("users").document(targetOwnerUid).collection("products")

        // Immediate one-shot pull for worker / manager upon opening session
        scope.launch(Dispatchers.IO) {
            try {
                val oneShotSnap = productsCollection.get().await()
                if (!oneShotSnap.isEmpty) {
                    val remoteProducts = mutableListOf<Product>()
                    for (doc in oneShotSnap.documents) {
                        val prod = doc.toObject(Product::class.java)
                        if (prod != null) {
                            if (prod.firestoreId.isBlank()) {
                                prod.firestoreId = doc.id
                            }
                            val resolvedImagePath = ProductCloudImageHelper.ensureLocalProductImage(context, prod)
                            if (resolvedImagePath.isNotBlank()) {
                                prod.imagePath = resolvedImagePath
                            }
                            remoteProducts.add(prod)
                        }
                    }
                    if (remoteProducts.isNotEmpty()) {
                        val mergedCount = repository.mergeRemoteProductsWithTimestampResolution(remoteProducts)
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        val feedback = "تم تحميل وتحديث $mergedCount صنفاً من مخزون المدير محلياً في Room ✓"
                        _syncMessage.value = feedback
                        withContext(Dispatchers.Main) {
                            onStatusMessage(feedback)
                        }
                        Log.i(TAG, "Worker instant sync: merged $mergedCount products from manager ($targetOwnerUid) into Room.")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Worker instant products pull failed (will rely on listener): ${e.message}")
            }
        }

        productsListenerRegistration = productsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "مزامنة السحابة مقيدة (صلاحية ناقصة أو حساب Offline/Lite): ${error.message}")
                    _syncMessage.value = "ميزة المزامنة مقيدة (وضع Offline/Lite). تتطلب ترقية الحساب إلى Pro mode."
                } else {
                    Log.w(TAG, "خطأ في استقبال تحديثات المخزون اللحظية: ${error.message}")
                    _syncMessage.value = "خطأ في اتصال المزامنة: ${error.localizedMessage}"
                }
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                _lastSyncTimestamp.value = System.currentTimeMillis()
                return@addSnapshotListener
            }

            scope.launch(Dispatchers.IO) {
                try {
                    val remoteProducts = mutableListOf<Product>()
                    for (doc in snapshot.documents) {
                        val prod = doc.toObject(Product::class.java)
                        if (prod != null) {
                            if (prod.firestoreId.isBlank()) {
                                prod.firestoreId = doc.id
                            }
                            // التحقق من فك تشفير وتخزين صورة المنتج محلياً
                            val resolvedImagePath = ProductCloudImageHelper.ensureLocalProductImage(context, prod)
                            if (resolvedImagePath.isNotBlank()) {
                                prod.imagePath = resolvedImagePath
                            }
                            remoteProducts.add(prod)
                        }
                    }

                    if (remoteProducts.isNotEmpty()) {
                        val mergedCount = repository.mergeRemoteProductsWithTimestampResolution(remoteProducts)
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        val feedback = "تمت مزامنة $mergedCount صنفاً من مخزون الإدارة بنجاح ✓"
                        _syncMessage.value = feedback
                        withContext(Dispatchers.Main) {
                            onStatusMessage(feedback)
                        }
                        Log.i(TAG, "تم دمج $mergedCount منتجاً سحابياً في قاعدة بيانات Room المحلية.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "فشل دمج السلع المستلمة من Firestore: ${e.message}", e)
                }
            }
        }
    }

    /**
     * إيقاف مستمع المزامنة الحية عند تسجيل الخروج.
     */
    fun stopLiveInventorySync() {
        productsListenerRegistration?.remove()
        productsListenerRegistration = null
        _isLiveSyncActive.value = false
        currentOwnerUid = ""
    }

    /**
     * رفع أو تحديث سلعة جديدة مع ضغط صورتها إلى Base64 لمزامنتها مع العمال فوراً.
     */
    suspend fun pushProductToCloud(
        product: Product,
        activeAccount: StaffAccount? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            Log.d(TAG, "تخطي رفع السلعة للسحابة: الحساب في وضع Just Offline / Lite")
            return@withContext Result.success(product.firestoreId.ifBlank { "offline_${product.id}" })
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) {
            return@withContext Result.failure(IllegalStateException("لم يتم العثور على معرّف سحابي لمخزون المدير"))
        }

        try {
            val docId = getInventoryDocId(product)
            val docRef = db.collection("users").document(targetOwnerUid).collection("products").document(docId)

            // معالجة وضغط الصورة إلى Base64 Data URL خفيف
            val base64Image = if (product.imagePath.isNotBlank()) {
                ProductCloudImageHelper.compressLocalImageToBase64(context, product.imagePath) ?: product.imageUrl
            } else {
                product.imageUrl
            }

            val payload = hashMapOf<String, Any?>(
                "id" to product.id,
                "firestoreId" to docId,
                "userId" to targetOwnerUid,
                "name" to product.name.trim(),
                "purchasePrice" to product.purchasePrice,
                "price" to product.price,
                "stock" to product.stock,
                "unitsPerCarton" to product.unitsPerCarton.coerceAtLeast(1),
                "barcode" to product.barcode.trim(),
                "cartonBarcode" to product.cartonBarcode.trim(),
                "category" to product.category.ifBlank { "عام" },
                "minStockAlert" to product.minStockAlert,
                "imagePath" to product.imagePath,
                "imageUrl" to (base64Image ?: ""),
                "isDeleted" to product.isDeleted,
                "syncStatus" to RecordSyncState.SYNCED,
                "serverUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(payload, SetOptions.merge()).await()
            Log.i(TAG, "تم رفع وتحديث السلعة «${product.name}» في سحابة المدير ($targetOwnerUid).")
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(TAG, "فشل رفع السلعة إلى Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * خصم كميات المخزون في Firestore دفعة واحدة وبطريقة ذرية عند اكتمال عملية بيع (POS).
     */
    suspend fun pushBatchStockDeductions(
        deductions: List<StockDeduction>,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (deductions.isEmpty()) return@withContext Result.success(Unit)
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            Log.d(TAG, "تخطي خصم المخزون سحابياً: الحساب في وضع Just Offline / Lite")
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val batch = db.batch()
            val productsCollection = db.collection("users").document(targetOwnerUid).collection("products")

            for (item in deductions) {
                val docId = when {
                    item.firestoreId.isNotBlank() -> item.firestoreId
                    item.barcode.isNotBlank() -> "bc_${item.barcode.trim().replace(Regex("[^a-zA-Z0-9]"), "_")}"
                    item.productId > 0 -> "pid_${item.productId}"
                    else -> null
                }
                if (docId != null) {
                    val ref = productsCollection.document(docId)
                    val updates = mapOf(
                        "stock" to item.resultingStock,
                        "serverUpdatedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, updates, SetOptions.merge())
                }
            }

            batch.commit().await()
            Log.i(TAG, "تم خصم مخزون ${deductions.size} أصناف سحابياً بنجاح بعد عملية البيع.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء خصم المخزون سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * زيادة كميات المخزون في Firestore عند تسجيل مشتريات جديدة أو إعادة تزويد سريع.
     */
    suspend fun pushBatchStockIncreases(
        increases: List<StockIncrease>,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (increases.isEmpty()) return@withContext Result.success(Unit)
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            Log.d(TAG, "تخطي زيادة المخزون سحابياً: الحساب في وضع Just Offline / Lite")
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val batch = db.batch()
            val productsCollection = db.collection("users").document(targetOwnerUid).collection("products")

            for (item in increases) {
                val docId = when {
                    item.firestoreId.isNotBlank() -> item.firestoreId
                    item.barcode.isNotBlank() -> "bc_${item.barcode.trim().replace(Regex("[^a-zA-Z0-9]"), "_")}"
                    item.productId > 0 -> "pid_${item.productId}"
                    else -> null
                }
                if (docId != null) {
                    val ref = productsCollection.document(docId)
                    val updates = mapOf(
                        "stock" to item.resultingStock,
                        "serverUpdatedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    batch.set(ref, updates, SetOptions.merge())
                }
            }

            batch.commit().await()
            Log.i(TAG, "تمت زيادة مخزون ${increases.size} أصناف سحابياً بنجاح بعد المشتريات/التزويد.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء زيادة المخزون سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * حذف أو تعطيل سلعة سحابياً لجميع العمال التابعين.
     */
    suspend fun deleteProductFromCloud(
        product: Product,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val docId = getInventoryDocId(product)
            val ref = db.collection("users").document(targetOwnerUid).collection("products").document(docId)
            // استخدام Soft Delete لتحديث الأجهزة
            val updates = mapOf(
                "isDeleted" to true,
                "serverUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to System.currentTimeMillis()
            )
            ref.set(updates, SetOptions.merge()).await()
            Log.i(TAG, "تم حذف السلعة ${product.name} سحابياً.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء حذف السلعة سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * رفع ومزامنة الفاتورة المنفذة من العامل أو المدير إلى سحابة الإدارة فورياً.
     */
    suspend fun pushInvoiceToCloud(
        invoice: ArchivedInvoice,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            Log.d(TAG, "تخطي رفع الفاتورة سحابياً: الحساب في وضع Just Offline / Lite")
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val invoicesCollection = db.collection("users").document(targetOwnerUid).collection("invoices")
            val invoiceDocId = invoice.invoiceId.ifBlank { "inv_${System.currentTimeMillis()}" }

            val itemsPayload = invoice.items.map { sale ->
                mapOf(
                    "saleId" to sale.saleId,
                    "productId" to sale.productId,
                    "productName" to sale.productName,
                    "quantitySold" to sale.quantitySold,
                    "totalPrice" to sale.totalPrice,
                    "saleDate" to sale.saleDate,
                    "customerId" to sale.customerId,
                    "customerName" to sale.customerName,
                    "isDebt" to sale.isDebt,
                    "paymentMethod" to sale.paymentMethod,
                    "invoiceGroupId" to invoiceDocId,
                    "orderType" to sale.orderType,
                    "status" to sale.status,
                    "expectedDeliveryDate" to sale.expectedDeliveryDate,
                    "notes" to sale.notes
                )
            }

            val invoiceData = hashMapOf<String, Any?>(
                "invoiceId" to invoiceDocId,
                "dateMillis" to invoice.dateMillis,
                "customerName" to invoice.customerName,
                "customerId" to invoice.customerId,
                "paymentMethod" to invoice.paymentMethod,
                "isDebt" to invoice.isDebt,
                "subtotal" to invoice.subtotal,
                "totalQuantity" to invoice.totalQuantity,
                "orderType" to invoice.orderType,
                "status" to invoice.status,
                "expectedDeliveryDate" to invoice.expectedDeliveryDate,
                "notes" to invoice.notes,
                "customerLocationUrl" to invoice.customerLocationUrl,
                "workerName" to (activeAccount?.fullName?.ifBlank { activeAccount.email } ?: "كاشير"),
                "workerUid" to (activeAccount?.userId ?: auth.currentUser?.uid ?: ""),
                "items" to itemsPayload,
                "serverUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to System.currentTimeMillis()
            )

            invoicesCollection.document(invoiceDocId).set(invoiceData, SetOptions.merge()).await()
            _lastSyncTimestamp.value = System.currentTimeMillis()
            Log.i(TAG, "تمت مزامنة الفاتورة $invoiceDocId سحابياً في مستودع الإدارة ($targetOwnerUid).")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء مزامنة الفاتورة سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * بدء مستمع لحظي لفواتير ومبيعات العمال الواردة لإدارة المتجر.
     */
    fun startLiveInvoicesSync(
        activeAccount: StaffAccount,
        onStatusMessage: (String) -> Unit = {}
    ) {
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return

        invoicesListenerRegistration?.remove()

        val invoicesCollection = db.collection("users").document(targetOwnerUid).collection("invoices")
        invoicesListenerRegistration = invoicesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "خطأ في استقبال تحديثات الفواتير اللحظية: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot == null || snapshot.isEmpty) {
                _syncedInvoicesCount.value = snapshot?.size() ?: 0
                return@addSnapshotListener
            }

            scope.launch(Dispatchers.IO) {
                try {
                    var newInvoicesCount = 0
                    for (doc in snapshot.documents) {
                        val invoiceId = doc.getString("invoiceId") ?: doc.id
                        val itemsRaw = doc.get("items") as? List<*> ?: emptyList<Any>()
                        val salesToMerge = mutableListOf<Sale>()

                        for (raw in itemsRaw) {
                            if (raw is Map<*, *>) {
                                val prodId = (raw["productId"] as? Number)?.toInt() ?: 0
                                val prodName = raw["productName"] as? String ?: "صنف"
                                val qty = (raw["quantitySold"] as? Number)?.toInt() ?: 1
                                val total = (raw["totalPrice"] as? Number)?.toDouble() ?: 0.0
                                val date = (raw["saleDate"] as? Number)?.toLong() ?: (doc.getLong("dateMillis") ?: System.currentTimeMillis())
                                val custId = (raw["customerId"] as? Number)?.toInt() ?: doc.getLong("customerId")?.toInt()
                                val custName = (raw["customerName"] as? String) ?: (doc.getString("customerName") ?: "")
                                val isDebt = (raw["isDebt"] as? Boolean) ?: (doc.getBoolean("isDebt") ?: false)
                                val method = (raw["paymentMethod"] as? String) ?: (doc.getString("paymentMethod") ?: "CASH")
                                val orderType = (raw["orderType"] as? String) ?: (doc.getString("orderType") ?: "direct")
                                val status = (raw["status"] as? String) ?: (doc.getString("status") ?: "completed")
                                val expDelivery = (raw["expectedDeliveryDate"] as? Number)?.toLong() ?: 0L
                                val notes = (raw["notes"] as? String) ?: ""

                                salesToMerge.add(
                                    Sale(
                                        saleId = 0,
                                        productId = prodId,
                                        productName = prodName,
                                        quantitySold = qty,
                                        totalPrice = total,
                                        saleDate = date,
                                        customerId = custId,
                                        customerName = custName,
                                        isDebt = isDebt,
                                        paymentMethod = method,
                                        invoiceGroupId = invoiceId,
                                        orderType = orderType,
                                        status = status,
                                        expectedDeliveryDate = expDelivery,
                                        notes = notes,
                                        syncStatus = RecordSyncState.SYNCED
                                    )
                                )
                            }
                        }

                        if (salesToMerge.isNotEmpty()) {
                            val allSales = repository.getAllSalesOnce()
                            val alreadyExists = allSales.any { it.invoiceGroupId == invoiceId }
                            if (!alreadyExists) {
                                repository.saveSalesBatch(salesToMerge, enqueueForSync = false)
                                newInvoicesCount++
                            }
                        }
                    }
                    _syncedInvoicesCount.value = snapshot.size()
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    if (newInvoicesCount > 0) {
                        val msg = "تمت مزامنة $newInvoicesCount فاتورة جديدة من الفريق بنجاح ✓"
                        withContext(Dispatchers.Main) { onStatusMessage(msg) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "خطأ أثناء دمج فواتير السحابة: ${e.message}", e)
                }
            }
        }
    }

    /**
     * إيقاف مستمع الفواتير.
     */
    fun stopLiveInvoicesSync() {
        invoicesListenerRegistration?.remove()
        invoicesListenerRegistration = null
    }

    /**
     * بدء مستمع لحظي لمواقع العمال الميدانية GPS وتحديث قاعدة بيانات Room.
     */
    fun startLiveWorkerLocationsSync(
        activeAccount: StaffAccount,
        onLocationReceived: (WorkerLocation) -> Unit = {}
    ) {
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return

        locationsListenerRegistration?.remove()

        val locationsCollection = db.collection("users").document(targetOwnerUid).collection("worker_locations")
        locationsListenerRegistration = locationsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "خطأ في استقبال مواقع العمال اللحظية: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot == null || snapshot.isEmpty) {
                _syncedLocationsCount.value = snapshot?.size() ?: 0
                return@addSnapshotListener
            }

            scope.launch(Dispatchers.IO) {
                try {
                    for (doc in snapshot.documents) {
                        val userId = (doc.get("userId") as? Number)?.toInt()
                            ?: doc.id.filter { it.isDigit() }.toIntOrNull()
                            ?: continue
                        val workerName = doc.getString("workerName") ?: "عامل ميداني"
                        val lat = doc.getDouble("latitude") ?: continue
                        val lng = doc.getDouble("longitude") ?: continue
                        val statusNote = doc.getString("statusNote") ?: "ميداني"
                        val timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: System.currentTimeMillis()

                        val workerLoc = WorkerLocation(
                            userId = userId,
                            workerName = workerName,
                            latitude = lat,
                            longitude = lng,
                            statusNote = statusNote,
                            timestamp = timestamp,
                            isTrackingActive = true
                        )
                        repository.updateWorkerLocation(workerLoc)
                        withContext(Dispatchers.Main) {
                            onLocationReceived(workerLoc)
                        }
                    }
                    _syncedLocationsCount.value = snapshot.size()
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                } catch (e: Exception) {
                    Log.e(TAG, "خطأ أثناء تحديث مواقع العمال من السحابة: ${e.message}", e)
                }
            }
        }
    }

    /**
     * إيقاف مستمع مواقع العمال.
     */
    fun stopLiveWorkerLocationsSync() {
        locationsListenerRegistration?.remove()
        locationsListenerRegistration = null
    }

    /**
     * رفع موقع العامل اللحظي GPS إلى سحابة المدير والمستودع العام.
     */
    suspend fun pushWorkerLocation(
        userId: Int,
        workerName: String,
        lat: Double,
        lng: Double,
        statusNote: String,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        val loc = WorkerLocation(
            userId = userId,
            workerName = workerName,
            latitude = lat,
            longitude = lng,
            statusNote = statusNote,
            timestamp = System.currentTimeMillis(),
            isTrackingActive = true
        )
        repository.updateWorkerLocation(loc)

        val payload = hashMapOf<String, Any?>(
            "userId" to userId,
            "workerName" to workerName,
            "latitude" to lat,
            "longitude" to lng,
            "statusNote" to statusNote,
            "timestamp" to System.currentTimeMillis(),
            "serverUpdatedAt" to FieldValue.serverTimestamp()
        )

        try {
            db.collection("worker_locations").document(userId.toString()).set(payload, SetOptions.merge()).await()
            if (targetOwnerUid.isNotBlank()) {
                db.collection("users").document(targetOwnerUid)
                    .collection("worker_locations")
                    .document(userId.toString())
                    .set(payload, SetOptions.merge())
                    .await()
            }
            _lastSyncTimestamp.value = System.currentTimeMillis()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء رفع موقع العامل سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * فحص وتدقيق المزامنة الشاملة فوراً لكل أركان التطبيق (منتجات، فواتير، مواقع، مخزون معلق).
     */
    suspend fun triggerComprehensiveSync(activeAccount: StaffAccount?): ComprehensiveSyncResult = withContext(Dispatchers.IO) {
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            val offlineMsg = "حسابك في وضع العمل دون إنترنت (Just Offline / Lite). المزامنة السحابية غير مفعّلة لحسابك. يمكنك طلب الترقية إلى Pro mode لتفعيل المزامنة مع العمال."
            _syncMessage.value = offlineMsg
            _isComprehensiveSyncing.value = false
            return@withContext ComprehensiveSyncResult(
                isSuccess = true,
                message = offlineMsg,
                productsCount = 0,
                invoicesCount = 0,
                locationsCount = 0
            )
        }

        if (auth.currentUser == null) {
            val offlineMsg = "لا يوجد اتصال بالمصادقة السحابية حالياً. تم حفظ البيانات محلياً على الجهاز."
            _syncMessage.value = offlineMsg
            _isComprehensiveSyncing.value = false
            return@withContext ComprehensiveSyncResult(
                isSuccess = true,
                message = offlineMsg,
                productsCount = 0,
                invoicesCount = 0,
                locationsCount = 0
            )
        }

        _isComprehensiveSyncing.value = true
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        try {
            // 1. مزامنة ودفع أي مبيعات محلية معلقة في Room
            val pendingSales = repository.getPendingSalesOnce()
            if (pendingSales.isNotEmpty() && targetOwnerUid.isNotBlank()) {
                val groupedByInvoice = pendingSales.groupBy { it.invoiceGroupId.ifBlank { "inv_${it.saleDate}" } }
                val invoicesCollection = db.collection("users").document(targetOwnerUid).collection("invoices")
                val batch = db.batch()

                for ((invId, sales) in groupedByInvoice) {
                    val first = sales.first()
                    val invoicePayload = hashMapOf<String, Any?>(
                        "invoiceId" to invId,
                        "dateMillis" to first.saleDate,
                        "customerName" to first.customerName,
                        "customerId" to first.customerId,
                        "paymentMethod" to first.paymentMethod,
                        "isDebt" to sales.any { it.isDebt },
                        "subtotal" to sales.sumOf { it.totalPrice },
                        "totalQuantity" to sales.sumOf { it.quantitySold },
                        "orderType" to first.orderType,
                        "status" to first.status,
                        "serverUpdatedAt" to FieldValue.serverTimestamp(),
                        "items" to sales.map { s ->
                            mapOf(
                                "saleId" to s.saleId,
                                "productId" to s.productId,
                                "productName" to s.productName,
                                "quantitySold" to s.quantitySold,
                                "totalPrice" to s.totalPrice,
                                "saleDate" to s.saleDate,
                                "isDebt" to s.isDebt
                            )
                        }
                    )
                    batch.set(invoicesCollection.document(invId), invoicePayload, SetOptions.merge())
                }
                batch.commit().await()
                repository.markSalesAsSynced(pendingSales.map { it.saleId })
            }

            // 2. سحب كتالوج السلع السحابي للمدير
            var productsCount = 0
            if (targetOwnerUid.isNotBlank()) {
                val prodSnap = db.collection("users").document(targetOwnerUid).collection("products").get().await()
                productsCount = prodSnap.size()
                val remoteList = mutableListOf<Product>()
                for (doc in prodSnap.documents) {
                    val p = doc.toObject(Product::class.java)
                    if (p != null) {
                        if (p.firestoreId.isBlank()) p.firestoreId = doc.id
                        val resolvedImg = ProductCloudImageHelper.ensureLocalProductImage(context, p)
                        if (resolvedImg.isNotBlank()) p.imagePath = resolvedImg
                        remoteList.add(p)
                    }
                }
                if (remoteList.isNotEmpty()) {
                    repository.mergeRemoteProductsWithTimestampResolution(remoteList)
                }
                _syncedProductsCount.value = productsCount
            }

            // 3. سحب الفواتير الصادرة من الفريق
            var invoicesCount = 0
            if (targetOwnerUid.isNotBlank()) {
                val invSnap = db.collection("users").document(targetOwnerUid).collection("invoices").get().await()
                invoicesCount = invSnap.size()
                _syncedInvoicesCount.value = invoicesCount
            }

            // 4. سحب مواقع العمال GPS
            var locationsCount = 0
            if (targetOwnerUid.isNotBlank()) {
                val locSnap = db.collection("users").document(targetOwnerUid).collection("worker_locations").get().await()
                locationsCount = locSnap.size()
                for (doc in locSnap.documents) {
                    val uid = (doc.get("userId") as? Number)?.toInt() ?: doc.id.filter { it.isDigit() }.toIntOrNull() ?: continue
                    val name = doc.getString("workerName") ?: "عامل"
                    val lat = doc.getDouble("latitude") ?: continue
                    val lng = doc.getDouble("longitude") ?: continue
                    val note = doc.getString("statusNote") ?: "ميداني"
                    val ts = (doc.get("timestamp") as? Number)?.toLong() ?: System.currentTimeMillis()
                    repository.updateWorkerLocation(
                        WorkerLocation(
                            userId = uid,
                            workerName = name,
                            latitude = lat,
                            longitude = lng,
                            statusNote = note,
                            timestamp = ts,
                            isTrackingActive = true
                        )
                    )
                }
                _syncedLocationsCount.value = locationsCount
            }

            repository.markAllTablesAndQueueAsSynced()
            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            _isComprehensiveSyncing.value = false

            val summaryMsg = "اكتملت المزامنة الشاملة بنجاح ✓ ($productsCount صنفاً، $invoicesCount فاتورة، $locationsCount موقعاً)"
            _syncMessage.value = summaryMsg
            ComprehensiveSyncResult(
                isSuccess = true,
                message = summaryMsg,
                productsCount = productsCount,
                invoicesCount = invoicesCount,
                locationsCount = locationsCount
            )
        } catch (e: Exception) {
            _isComprehensiveSyncing.value = false
            val isPermissionDenied = e is com.google.firebase.firestore.FirebaseFirestoreException &&
                e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            val errorMsg = if (isPermissionDenied) {
                if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
                    "حسابك في وضع Just Offline / Lite: المزامنة السحابية مقيدة وتتطلب الترقية إلى Pro."
                } else {
                    "المزامنة السحابية مقيدة: تم حفظ البيانات محلياً على الجهاز بأمان."
                }
            } else {
                "حدث خطأ أثناء فحص المزامنة: ${e.localizedMessage ?: e.message}"
            }
            _syncMessage.value = errorMsg
            Log.w(TAG, errorMsg, e)
            ComprehensiveSyncResult(
                isSuccess = false,
                message = errorMsg,
                productsCount = 0,
                invoicesCount = 0,
                locationsCount = 0
            )
        }
    }

    /**
     * إيقاف جميع مستمعي المزامنة الحية دفعة واحدة.
     */
    fun stopAllLiveSync() {
        stopLiveInventorySync()
        stopLiveInvoicesSync()
        stopLiveWorkerLocationsSync()
    }

    data class ComprehensiveSyncResult(
        val isSuccess: Boolean,
        val message: String,
        val productsCount: Int,
        val invoicesCount: Int,
        val locationsCount: Int
    )
}
