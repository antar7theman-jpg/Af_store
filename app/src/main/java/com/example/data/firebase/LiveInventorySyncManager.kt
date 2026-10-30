package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.AntarRepository
import com.example.data.database.AntarDatabase
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoice
import com.example.data.models.RecordSyncState
import com.example.data.models.ArchivedInvoice
import com.example.data.models.DamagedItemRecord
import com.example.data.models.SpoilageReason
import com.example.data.models.Sale
import com.example.data.models.WorkerLocation
import com.example.data.repository.ExpiryAndWasteRepository
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
        private val activeInstances = java.util.concurrent.CopyOnWriteArrayList<java.lang.ref.WeakReference<LiveInventorySyncManager>>()

        fun notifySyncCompleted(timestamp: Long, summary: String) {
            activeInstances.forEach { ref ->
                ref.get()?.let { mgr ->
                    mgr._lastSyncTimestamp.value = timestamp
                    mgr._syncMessage.value = summary
                    mgr._isComprehensiveSyncing.value = false
                }
            }
        }

        fun getInventoryDocId(product: Product): String {
            return when {
                product.firestoreId.isNotBlank() && product.firestoreId.matches(Regex("^[a-zA-Z0-9_\\-]+$")) -> product.firestoreId
                product.barcode.isNotBlank() -> "bc_${product.barcode.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}"
                product.cartonBarcode.isNotBlank() -> "cbc_${product.cartonBarcode.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}"
                product.name.isNotBlank() -> {
                    val normalizedName = product.name.trim().lowercase().replace(Regex("\\s+"), " ")
                    val asciiPart = normalizedName.replace(Regex("[^a-z0-9]"), "_").trim('_').take(32)
                    val nameHash = normalizedName.hashCode().toUInt().toString(16)
                    if (asciiPart.isNotBlank()) "pn_${asciiPart}_$nameHash" else "pn_$nameHash"
                }
                product.id > 0 -> "pid_${product.id}"
                else -> "p_${System.currentTimeMillis()}"
            }
        }

        fun getInvoiceDocId(invoiceId: String): String {
            return invoiceId.trim()
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .ifBlank { "inv_${System.currentTimeMillis()}" }
        }
    }

    init {
        activeInstances.add(java.lang.ref.WeakReference(this))
    }

    private val db: FirebaseFirestore = StaffAccountRepository.getFirestoreDb(context)
    private val auth = Firebase.auth
    private val repository: AntarRepository by lazy {
        val database = AntarDatabase.getDatabase(context.applicationContext)
        AntarRepository(database.storeDao())
    }

    private var productsListenerRegistration: ListenerRegistration? = null
    private val extraProductsListeners = mutableListOf<ListenerRegistration>()
    private var invoicesListenerRegistration: ListenerRegistration? = null
    private var purchaseInvoicesListenerRegistration: ListenerRegistration? = null
    private var locationsListenerRegistration: ListenerRegistration? = null
    private var damagedGoodsListenerRegistration: ListenerRegistration? = null
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

    private val savedInitialTimestamp = try {
        context.getSharedPreferences("af_store_sync_prefs", Context.MODE_PRIVATE)
            .getLong("last_incremental_sync_timestamp", 0L)
    } catch (_: Exception) { 0L }
    private val _lastSyncTimestamp = MutableStateFlow<Long>(savedInitialTimestamp)
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

    private fun parseProductDocument(doc: com.google.firebase.firestore.DocumentSnapshot): Product? {
        if (!doc.exists()) return null
        return try {
            val data = doc.data ?: return null
            val name = (data["name"] as? String)?.trim() ?: ""
            val barcode = (data["barcode"] as? String)?.trim() ?: ""
            val cartonBarcode = (data["cartonBarcode"] as? String)?.trim() ?: ""
            val firestoreId = ((data["firestoreId"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: doc.id)
            val id = (data["id"] as? Number)?.toInt() ?: 0
            val purchasePrice = (data["purchasePrice"] as? Number)?.toDouble() ?: 0.0
            val price = (data["price"] as? Number)?.toDouble()
                ?: (data["salePrice"] as? Number)?.toDouble()
                ?: 0.0
            val stock = (data["stock"] as? Number)?.toInt()
                ?: (data["quantityInUnits"] as? Number)?.toInt()
                ?: 0
            val unitsPerCarton = ((data["unitsPerCarton"] as? Number)?.toInt() ?: 1).coerceAtLeast(1)
            val category = (data["category"] as? String)?.trim()?.ifBlank { "عام" } ?: "عام"
            val minStockAlert = (data["minStockAlert"] as? Number)?.toInt()
                ?: (data["minStockThreshold"] as? Number)?.toInt()
                ?: 5
            val imagePath = (data["imagePath"] as? String) ?: ""
            val imageUrl = (data["imageUrl"] as? String) ?: ""
            val isDeleted = (data["isDeleted"] as? Boolean) ?: false
            val updatedAt = (data["updatedAt"] as? Number)?.toLong()
                ?: doc.getTimestamp("serverUpdatedAt")?.toDate()?.time
                ?: System.currentTimeMillis()

            if (name.isBlank() && barcode.isBlank() && id <= 0 && !data.containsKey("stock")) {
                return null
            }

            Product(
                id = id,
                firestoreId = firestoreId,
                name = name,
                purchasePrice = purchasePrice,
                price = price,
                stock = stock,
                unitsPerCarton = unitsPerCarton,
                barcode = barcode,
                cartonBarcode = cartonBarcode,
                category = category,
                minStockAlert = minStockAlert,
                imagePath = imagePath,
                imageUrl = imageUrl,
                isDeleted = isDeleted,
                updatedAt = updatedAt,
                syncStatus = RecordSyncState.SYNCED
            )
        } catch (e: Exception) {
            Log.w(TAG, "Fallback parseProductDocument error for ${doc.id}: ${e.message}")
            try {
                doc.toObject(Product::class.java)?.apply {
                    if (firestoreId.isBlank()) firestoreId = doc.id
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * استخراج المعرف السحابي المشترك لمخزون المدير التابع له العامل.
     */
    fun resolveInventoryOwnerUid(activeAccount: StaffAccount?): String {
        if (activeAccount == null) {
            val saved = StaffAccountRepository.loadPersistentStaffSession(context)
            if (saved != null) {
                return resolveInventoryOwnerUid(saved)
            }
            return auth.currentUser?.uid ?: ""
        }
        return when {
            activeAccount.isOwner -> {
                activeAccount.userId.ifBlank { "owner_deffas_antar" }
            }
            activeAccount.isRegularManager -> {
                if (activeAccount.fullName.isNotBlank()) {
                    StaffAccountRepository.managerPreProvisionDocId(activeAccount.fullName)
                } else {
                    activeAccount.userId
                }
            }
            activeAccount.role.equals("worker", ignoreCase = true) -> {
                val managerName = activeAccount.parentManagerName.trim().ifBlank {
                    activeAccount.approvedByName.trim()
                }
                val isUnderOwner = managerName.equals(StaffAccountRepository.OWNER_FULL_NAME, ignoreCase = true) ||
                    activeAccount.parentManagerUid.startsWith("owner_", ignoreCase = true)
                when {
                    isUnderOwner -> activeAccount.parentManagerUid.ifBlank { "owner_deffas_antar" }
                    managerName.isNotBlank() -> StaffAccountRepository.managerPreProvisionDocId(managerName)
                    activeAccount.parentManagerUid.isNotBlank() -> activeAccount.parentManagerUid
                    activeAccount.approvedByUid.isNotBlank() -> activeAccount.approvedByUid
                    else -> activeAccount.userId
                }
            }
            else -> activeAccount.parentManagerUid.ifBlank {
                activeAccount.approvedByUid.ifBlank {
                    activeAccount.userId.ifBlank { auth.currentUser?.uid ?: "" }
                }
            }
        }
    }

    /**
     * استخراج جميع المعرفات السحابية المحتملة لمخزون المدير (المعرف القياسي بالاسم + المعرفات السابقة بالإيميل أو UID)
     * لضمان التوافق الفوري بين الأجهزة حتى للحسابات المفتوحة مسبقاً.
     */
    fun resolveAllCandidateInventoryOwnerUids(activeAccount: StaffAccount?): List<String> {
        val acct = activeAccount ?: StaffAccountRepository.loadPersistentStaffSession(context)
            ?: return listOfNotNull(auth.currentUser?.uid?.takeIf { it.isNotBlank() })
        val primary = resolveInventoryOwnerUid(acct)
        val candidates = LinkedHashSet<String>()
        if (primary.isNotBlank()) candidates.add(primary)

        if (acct.isOwner) {
            candidates.add("owner_deffas_antar")
            if (acct.userId.isNotBlank()) candidates.add(acct.userId)
        } else if (acct.isRegularManager) {
            if (acct.fullName.isNotBlank()) {
                candidates.add(StaffAccountRepository.managerPreProvisionDocId(acct.fullName))
            }
            if (acct.userId.isNotBlank()) {
                candidates.add(acct.userId)
            }
            if (acct.email.isNotBlank() && !acct.email.endsWith("@afstore.local", ignoreCase = true)) {
                candidates.add(StaffAccountRepository.managerPreProvisionDocId(acct.email))
            }
        } else if (acct.role.equals("worker", ignoreCase = true)) {
            if (acct.parentManagerName.isNotBlank() && !acct.parentManagerName.equals(StaffAccountRepository.OWNER_FULL_NAME, ignoreCase = true)) {
                candidates.add(StaffAccountRepository.managerPreProvisionDocId(acct.parentManagerName))
            }
            if (acct.parentManagerUid.isNotBlank()) {
                candidates.add(acct.parentManagerUid)
            }
            if (acct.parentManagerEmail.isNotBlank() && !acct.parentManagerEmail.endsWith("@afstore.local", ignoreCase = true)) {
                candidates.add(StaffAccountRepository.managerPreProvisionDocId(acct.parentManagerEmail))
            }
            if (acct.approvedByName.isNotBlank() && !acct.approvedByName.equals(StaffAccountRepository.OWNER_FULL_NAME, ignoreCase = true)) {
                candidates.add(StaffAccountRepository.managerPreProvisionDocId(acct.approvedByName))
            }
            if (acct.approvedByUid.isNotBlank()) {
                candidates.add(acct.approvedByUid)
            }
        }
        return candidates.filter { it.isNotBlank() && it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
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

        val candidateUids = resolveAllCandidateInventoryOwnerUids(activeAccount).ifEmpty { listOf(targetOwnerUid) }
        Log.i(TAG, "بدء مراقبة المخزون اللحظي للمدير: $candidateUids لحساب: ${activeAccount.fullName} (${activeAccount.role})")

        // Immediate one-shot pull & initial reconciliation for worker / manager upon opening session
        scope.launch(Dispatchers.IO) {
            try {
                val allCandidateUids = LinkedHashSet<String>(candidateUids)
                // If worker's parentManagerEmail or parentManagerName was updated on cloud, also discover manager email/name from staff_accounts
                if (activeAccount.role.equals("worker", ignoreCase = true)) {
                    try {
                        val workerDocId = if (activeAccount.fullName.isNotBlank()) {
                            StaffAccountRepository.workerPreProvisionDocId(activeAccount.fullName)
                        } else {
                            activeAccount.userId
                        }
                        val workerDoc = db.collection("staff_accounts").document(workerDocId).get().await()
                        if (workerDoc.exists()) {
                            val pUid = workerDoc.getString("parentManagerUid")?.trim().orEmpty()
                            val pName = workerDoc.getString("parentManagerName")?.trim().orEmpty()
                            val pEmail = workerDoc.getString("parentManagerEmail")?.trim().orEmpty()
                            if (pName.isNotBlank() && !pName.equals(StaffAccountRepository.OWNER_FULL_NAME, ignoreCase = true)) {
                                allCandidateUids.add(StaffAccountRepository.managerPreProvisionDocId(pName))
                            }
                            if (pUid.isNotBlank()) allCandidateUids.add(pUid)
                            if (pEmail.isNotBlank() && !pEmail.endsWith("@afstore.local", ignoreCase = true)) {
                                allCandidateUids.add(StaffAccountRepository.managerPreProvisionDocId(pEmail))
                            }
                        }
                    } catch (_: Exception) { }
                }

                val remoteProductsByDocId = LinkedHashMap<String, Product>()
                for (uid in allCandidateUids) {
                    try {
                        val snap = db.collection("users").document(uid).collection("products").get().await()
                        for (doc in snap.documents) {
                            val prod = parseProductDocument(doc) ?: continue
                            val resolvedImagePath = ProductCloudImageHelper.ensureLocalProductImage(context, prod)
                            if (resolvedImagePath.isNotBlank()) {
                                prod.imagePath = resolvedImagePath
                            }
                            val existing = remoteProductsByDocId[doc.id]
                            if (existing == null || prod.updatedAt >= existing.updatedAt) {
                                remoteProductsByDocId[doc.id] = prod
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed pulling products from candidate UID $uid: ${e.message}")
                    }
                }

                val remoteProducts = remoteProductsByDocId.values.toList()
                if (remoteProducts.isNotEmpty()) {
                    val mergedCount = repository.mergeRemoteProductsWithTimestampResolution(remoteProducts)
                    _syncedProductsCount.value = remoteProducts.size
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    val feedback = "تم تحميل وتحديث $mergedCount صنفاً من مخزون المدير محلياً في Room ✓"
                    _syncMessage.value = feedback
                    withContext(Dispatchers.Main) {
                        onStatusMessage(feedback)
                    }
                    Log.i(TAG, "Instant sync: merged $mergedCount products from manager ($allCandidateUids) into Room.")
                }

                // Push local products that need to be in the shared cloud catalog
                val allLocalProducts = repository.getAllProductsOnce()
                val isManagerOrOwner = activeAccount.isRegularManager || activeAccount.isOwner
                for (localProd in allLocalProducts) {
                    val docId = getInventoryDocId(localProd)
                    val remoteMatch = remoteProductsByDocId[docId]
                    val isSeedSample = localProd.updatedAt <= 1000L ||
                        (localProd.firestoreId.isBlank() && localProd.barcode.startsWith("6281000001"))

                    val shouldPush = if (isManagerOrOwner) {
                        // Manager pushes any modified/custom product missing or newer than cloud, OR initializes cloud if cloud is completely empty
                        if (remoteProducts.isEmpty()) {
                            true
                        } else if (!isSeedSample) {
                            remoteMatch == null || localProd.syncStatus != RecordSyncState.SYNCED || localProd.updatedAt > remoteMatch.updatedAt
                        } else {
                            false
                        }
                    } else {
                        // Worker only pushes non-sample products that the worker actually created or modified
                        !isSeedSample && (localProd.syncStatus != RecordSyncState.SYNCED || remoteMatch == null)
                    }

                    if (shouldPush) {
                        pushProductToCloud(localProd, activeAccount)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Instant products pull/push failed (will rely on listener): ${e.message}")
            }
        }

        val handleSnapshot = { snapshot: com.google.firebase.firestore.QuerySnapshot?, error: com.google.firebase.firestore.FirebaseFirestoreException? ->
            if (error != null) {
                if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "مزامنة السحابة مقيدة (صلاحية ناقصة أو حساب Offline/Lite): ${error.message}")
                    _syncMessage.value = "ميزة المزامنة مقيدة (وضع Offline/Lite). تتطلب ترقية الحساب إلى Pro mode."
                } else {
                    Log.w(TAG, "خطأ في استقبال تحديثات المخزون اللحظية: ${error.message}")
                    _syncMessage.value = "خطأ في اتصال المزامنة: ${error.localizedMessage}"
                }
            } else if (snapshot != null && !snapshot.isEmpty) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val remoteProducts = mutableListOf<Product>()
                        for (doc in snapshot.documents) {
                            val prod = parseProductDocument(doc)
                            if (prod != null) {
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
                            _syncedProductsCount.value = remoteProducts.size
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
            } else {
                _lastSyncTimestamp.value = System.currentTimeMillis()
            }
        }

        val primaryCollection = db.collection("users").document(targetOwnerUid).collection("products")
        productsListenerRegistration = primaryCollection.addSnapshotListener { snap, err ->
            handleSnapshot(snap, err)
        }

        for (extraUid in candidateUids.drop(1)) {
            if (extraUid != targetOwnerUid) {
                val reg = db.collection("users").document(extraUid).collection("products")
                    .addSnapshotListener { snap, err ->
                        handleSnapshot(snap, err)
                    }
                extraProductsListeners.add(reg)
            }
        }
    }

    /**
     * إيقاف مستمع المزامنة الحية عند تسجيل الخروج.
     */
    fun stopLiveInventorySync() {
        productsListenerRegistration?.remove()
        productsListenerRegistration = null
        extraProductsListeners.forEach { it.remove() }
        extraProductsListeners.clear()
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
            if (product.id > 0) {
                repository.markProductsAsSynced(listOf(product.id))
                repository.markSyncQueueByEntityTypeSynced("PRODUCT")
                repository.clearSyncedQueueItems()
            }
            return@withContext Result.success(product.firestoreId.ifBlank { "offline_${product.id}" })
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) {
            return@withContext Result.failure(IllegalStateException("لم يتم العثور على معرّف سحابي لمخزون المدير"))
        }

        try {
            val docId = getInventoryDocId(product)
            val candidateUids = resolveAllCandidateInventoryOwnerUids(activeAccount).ifEmpty { listOf(targetOwnerUid) }

            // معالجة وضغط الصورة إلى Base64 Data URL خفيف
            val base64Image = if (product.imagePath.isNotBlank()) {
                ProductCloudImageHelper.compressLocalImageToBase64(context, product.imagePath) ?: product.imageUrl
            } else {
                product.imageUrl
            }

            val now = System.currentTimeMillis()
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
                "updatedAt" to now
            )

            for (uid in candidateUids) {
                val docRef = db.collection("users").document(uid).collection("products").document(docId)
                docRef.set(payload, SetOptions.merge()).await()
            }

            if (product.id > 0) {
                repository.updateProductFirestoreIdAndSyncStatus(product.id, docId, RecordSyncState.SYNCED)
            }
            repository.markSyncQueueByEntityTypeSynced("PRODUCT")
            repository.clearSyncedQueueItems()
            _lastSyncTimestamp.value = now
            Log.i(TAG, "تم رفع وتحديث السلعة «${product.name}» (الكمية: ${product.stock}) في سحابة المدير ($candidateUids).")
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
            val ids = deductions.mapNotNull { it.productId.takeIf { id -> id > 0 } }
            if (ids.isNotEmpty()) repository.markProductsAsSynced(ids)
            repository.markSyncQueueByEntityTypeSynced("PRODUCT")
            repository.clearSyncedQueueItems()
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val candidateUids = resolveAllCandidateInventoryOwnerUids(activeAccount).ifEmpty { listOf(targetOwnerUid) }
            val localProductsById = repository.getAllProductsOnce().associateBy { it.id }
            val batch = db.batch()
            val now = System.currentTimeMillis()
            val resolvedDocIdsByProductId = mutableMapOf<Int, String>()

            for (item in deductions) {
                val localProd = localProductsById[item.productId]
                val docId = when {
                    localProd != null -> getInventoryDocId(localProd)
                    item.firestoreId.isNotBlank() && item.firestoreId.matches(Regex("^[a-zA-Z0-9_\\-]+$")) -> item.firestoreId
                    item.barcode.isNotBlank() -> "bc_${item.barcode.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}"
                    item.productId > 0 -> "pid_${item.productId}"
                    else -> null
                }
                if (docId != null) {
                    if (item.productId > 0) resolvedDocIdsByProductId[item.productId] = docId
                    val updates = if (localProd != null) {
                        hashMapOf<String, Any?>(
                            "id" to localProd.id,
                            "firestoreId" to docId,
                            "userId" to targetOwnerUid,
                            "name" to localProd.name.trim(),
                            "purchasePrice" to localProd.purchasePrice,
                            "price" to localProd.price,
                            "stock" to item.resultingStock,
                            "unitsPerCarton" to localProd.unitsPerCarton.coerceAtLeast(1),
                            "barcode" to localProd.barcode.trim(),
                            "cartonBarcode" to localProd.cartonBarcode.trim(),
                            "category" to localProd.category.ifBlank { "عام" },
                            "minStockAlert" to localProd.minStockAlert,
                            "isDeleted" to localProd.isDeleted,
                            "syncStatus" to RecordSyncState.SYNCED,
                            "serverUpdatedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to now
                        )
                    } else {
                        hashMapOf<String, Any?>(
                            "firestoreId" to docId,
                            "stock" to item.resultingStock,
                            "serverUpdatedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to now
                        )
                    }
                    for (uid in candidateUids) {
                        val ref = db.collection("users").document(uid).collection("products").document(docId)
                        batch.set(ref, updates, SetOptions.merge())
                    }
                }
            }

            batch.commit().await()
            for ((prodId, docId) in resolvedDocIdsByProductId) {
                repository.updateProductFirestoreIdAndSyncStatus(prodId, docId, RecordSyncState.SYNCED)
            }
            val ids = deductions.mapNotNull { it.productId.takeIf { id -> id > 0 } }
            if (ids.isNotEmpty()) repository.markProductsAsSynced(ids)
            repository.markSyncQueueByEntityTypeSynced("PRODUCT")
            repository.clearSyncedQueueItems()
            _lastSyncTimestamp.value = now
            Log.i(TAG, "تم خصم مخزون ${deductions.size} أصناف سحابياً بنجاح بعد عملية البيع ($candidateUids).")
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
            val ids = increases.mapNotNull { it.productId.takeIf { id -> id > 0 } }
            if (ids.isNotEmpty()) repository.markProductsAsSynced(ids)
            repository.markSyncQueueByEntityTypeSynced("PRODUCT")
            repository.clearSyncedQueueItems()
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val candidateUids = resolveAllCandidateInventoryOwnerUids(activeAccount).ifEmpty { listOf(targetOwnerUid) }
            val localProductsById = repository.getAllProductsOnce().associateBy { it.id }
            val batch = db.batch()
            val now = System.currentTimeMillis()
            val resolvedDocIdsByProductId = mutableMapOf<Int, String>()

            for (item in increases) {
                val localProd = localProductsById[item.productId]
                val docId = when {
                    localProd != null -> getInventoryDocId(localProd)
                    item.firestoreId.isNotBlank() && item.firestoreId.matches(Regex("^[a-zA-Z0-9_\\-]+$")) -> item.firestoreId
                    item.barcode.isNotBlank() -> "bc_${item.barcode.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}"
                    item.productId > 0 -> "pid_${item.productId}"
                    else -> null
                }
                if (docId != null) {
                    if (item.productId > 0) resolvedDocIdsByProductId[item.productId] = docId
                    val updates = if (localProd != null) {
                        hashMapOf<String, Any?>(
                            "id" to localProd.id,
                            "firestoreId" to docId,
                            "userId" to targetOwnerUid,
                            "name" to localProd.name.trim(),
                            "purchasePrice" to localProd.purchasePrice,
                            "price" to localProd.price,
                            "stock" to item.resultingStock,
                            "unitsPerCarton" to localProd.unitsPerCarton.coerceAtLeast(1),
                            "barcode" to localProd.barcode.trim(),
                            "cartonBarcode" to localProd.cartonBarcode.trim(),
                            "category" to localProd.category.ifBlank { "عام" },
                            "minStockAlert" to localProd.minStockAlert,
                            "isDeleted" to localProd.isDeleted,
                            "syncStatus" to RecordSyncState.SYNCED,
                            "serverUpdatedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to now
                        )
                    } else {
                        hashMapOf<String, Any?>(
                            "firestoreId" to docId,
                            "stock" to item.resultingStock,
                            "serverUpdatedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to now
                        )
                    }
                    for (uid in candidateUids) {
                        val ref = db.collection("users").document(uid).collection("products").document(docId)
                        batch.set(ref, updates, SetOptions.merge())
                    }
                }
            }

            batch.commit().await()
            for ((prodId, docId) in resolvedDocIdsByProductId) {
                repository.updateProductFirestoreIdAndSyncStatus(prodId, docId, RecordSyncState.SYNCED)
            }
            val ids = increases.mapNotNull { it.productId.takeIf { id -> id > 0 } }
            if (ids.isNotEmpty()) repository.markProductsAsSynced(ids)
            repository.markSyncQueueByEntityTypeSynced("PRODUCT")
            repository.clearSyncedQueueItems()
            _lastSyncTimestamp.value = now
            Log.i(TAG, "تمت زيادة مخزون ${increases.size} أصناف سحابياً بنجاح بعد المشتريات/التزويد ($candidateUids).")
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
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            if (product.id > 0) repository.markProductsAsSynced(listOf(product.id))
            repository.markSyncQueueByEntityTypeSynced("PRODUCT")
            repository.clearSyncedQueueItems()
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val docId = getInventoryDocId(product)
            val candidateUids = resolveAllCandidateInventoryOwnerUids(activeAccount).ifEmpty { listOf(targetOwnerUid) }
            val now = System.currentTimeMillis()
            // استخدام Soft Delete لتحديث الأجهزة
            val updates = mapOf(
                "firestoreId" to docId,
                "name" to product.name.trim(),
                "isDeleted" to true,
                "serverUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to now
            )
            for (uid in candidateUids) {
                val ref = db.collection("users").document(uid).collection("products").document(docId)
                ref.set(updates, SetOptions.merge()).await()
            }
            if (product.id > 0) repository.updateProductFirestoreIdAndSyncStatus(product.id, docId, RecordSyncState.SYNCED)
            repository.markSyncQueueByEntityTypeSynced("PRODUCT")
            repository.clearSyncedQueueItems()
            _lastSyncTimestamp.value = now
            Log.i(TAG, "تم حذف السلعة ${product.name} سحابياً ($candidateUids).")
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
        val invoiceDocId = invoice.invoiceId.ifBlank { "inv_${System.currentTimeMillis()}" }
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            Log.d(TAG, "تخطي رفع الفاتورة سحابياً: الحساب في وضع Just Offline / Lite")
            val saleIds = invoice.items.mapNotNull { it.saleId.takeIf { id -> id > 0 } }.ifEmpty {
                repository.getAllSalesOnce().filter { it.invoiceGroupId == invoiceDocId }.map { it.saleId }
            }
            if (saleIds.isNotEmpty()) repository.markSalesAsSynced(saleIds)
            repository.markSyncQueueByEntityTypeSynced("SALE")
            repository.clearSyncedQueueItems()
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val invoicesCollection = db.collection("users").document(targetOwnerUid).collection("invoices")

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
                    "status" to invoice.status.ifBlank { sale.status },
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

            val safeDocId = getInvoiceDocId(invoiceDocId)
            invoicesCollection.document(safeDocId).set(invoiceData, SetOptions.merge()).await()
            val saleIds = invoice.items.mapNotNull { it.saleId.takeIf { id -> id > 0 } }.ifEmpty {
                repository.getAllSalesOnce().filter { it.invoiceGroupId == invoiceDocId }.map { it.saleId }
            }
            if (saleIds.isNotEmpty()) {
                repository.markSalesAsSynced(saleIds)
            }
            repository.markSyncQueueByEntityTypeSynced("SALE")
            repository.clearSyncedQueueItems()
            _lastSyncTimestamp.value = System.currentTimeMillis()
            Log.i(TAG, "تمت مزامنة الفاتورة $invoiceDocId سحابياً في مستودع الإدارة ($targetOwnerUid).")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء مزامنة الفاتورة سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * رفع ومزامنة فاتورة المشتريات مع الموردين إلى سحابة الإدارة.
     */
    suspend fun pushPurchaseInvoiceToCloud(
        invoice: PurchaseInvoice,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            if (invoice.id > 0) repository.markPurchaseInvoicesAsSynced(listOf(invoice.id))
            repository.markSyncQueueByEntityTypeSynced("PURCHASE_INVOICE")
            repository.clearSyncedQueueItems()
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val docId = invoice.invoiceNumber.trim()
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .ifBlank { "pur_${if (invoice.id > 0) invoice.id else System.currentTimeMillis()}" }
            val ref = db.collection("users").document(targetOwnerUid).collection("purchase_invoices").document(docId)
            val payload = hashMapOf<String, Any?>(
                "id" to docId,
                "numId" to invoice.id,
                "invoiceNumber" to invoice.invoiceNumber.ifBlank { docId },
                "supplierName" to invoice.supplierName.ifBlank { "مورد عام" },
                "supplierPhone" to invoice.supplierPhone,
                "invoiceDate" to invoice.dateMillis,
                "dateMillis" to invoice.dateMillis,
                "totalCost" to invoice.totalCost,
                "paidAmount" to invoice.paidAmount,
                "remainingDebt" to invoice.remainingDebt,
                "paymentStatus" to invoice.paymentStatus,
                "notes" to invoice.notes,
                "itemsJson" to invoice.itemsJson,
                "itemsCount" to invoice.itemsCount,
                "syncStatus" to RecordSyncState.SYNCED,
                "serverUpdatedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to System.currentTimeMillis()
            )
            ref.set(payload, SetOptions.merge()).await()
            if (invoice.id > 0) {
                repository.markPurchaseInvoicesAsSynced(listOf(invoice.id))
            }
            repository.markSyncQueueByEntityTypeSynced("PURCHASE_INVOICE")
            repository.clearSyncedQueueItems()
            _lastSyncTimestamp.value = System.currentTimeMillis()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء مزامنة فاتورة المشتريات سحابياً: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * بدء مستمع لحظي لفواتير المشتريات بين المدير والفريق.
     */
    fun startLivePurchaseInvoicesSync(
        activeAccount: StaffAccount,
        onStatusMessage: (String) -> Unit = {}
    ) {
        if (!activeAccount.isSyncEnabled && !activeAccount.isOwner) return
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return

        purchaseInvoicesListenerRegistration?.remove()

        val purchaseCollection = db.collection("users").document(targetOwnerUid).collection("purchase_invoices")
        purchaseInvoicesListenerRegistration = purchaseCollection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener

            scope.launch(Dispatchers.IO) {
                try {
                    val remotePurchases = snapshot.documents.mapNotNull { doc ->
                        val supplierName = doc.getString("supplierName") ?: return@mapNotNull null
                        val invNum = doc.getString("invoiceNumber") ?: doc.id
                        val totalCost = (doc.get("totalCost") as? Number)?.toDouble() ?: 0.0
                        val paidAmount = (doc.get("paidAmount") as? Number)?.toDouble() ?: 0.0
                        val remainingDebt = (doc.get("remainingDebt") as? Number)?.toDouble()
                            ?: (totalCost - paidAmount).coerceAtLeast(0.0)
                        val dateMillis = (doc.get("dateMillis") as? Number)?.toLong()
                            ?: (doc.get("invoiceDate") as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                        val updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: dateMillis
                        PurchaseInvoice(
                            id = (doc.get("numId") as? Number)?.toInt() ?: 0,
                            invoiceNumber = invNum,
                            supplierName = supplierName,
                            supplierPhone = doc.getString("supplierPhone") ?: "",
                            dateMillis = dateMillis,
                            totalCost = totalCost,
                            paidAmount = paidAmount,
                            remainingDebt = remainingDebt,
                            paymentStatus = doc.getString("paymentStatus") ?: if (remainingDebt <= 0.009) "PAID" else "PARTIAL",
                            notes = doc.getString("notes") ?: "",
                            itemsJson = doc.getString("itemsJson") ?: "[]",
                            itemsCount = (doc.get("itemsCount") as? Number)?.toInt() ?: 1,
                            updatedAt = updatedAt,
                            syncStatus = RecordSyncState.SYNCED
                        )
                    }
                    if (remotePurchases.isNotEmpty()) {
                        repository.mergeRemotePurchaseInvoicesWithTimestampResolution(remotePurchases)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "خطأ أثناء دمج فواتير المشتريات السحابية: ${e.message}", e)
                }
            }
        }
    }

    fun stopLivePurchaseInvoicesSync() {
        purchaseInvoicesListenerRegistration?.remove()
        purchaseInvoicesListenerRegistration = null
    }

    /**
     * بدء مستمع لحظي لفواتير ومبيعات العمال الواردة لإدارة المتجر.
     */
    fun startLiveInvoicesSync(
        activeAccount: StaffAccount,
        onStatusMessage: (String) -> Unit = {}
    ) {
        if (!activeAccount.isSyncEnabled && !activeAccount.isOwner) return
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
                    val newInvoicesCount = mergeInvoiceDocumentsIntoRoom(snapshot.documents)
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

    internal fun parsePurchaseInvoiceDoc(doc: com.google.firebase.firestore.DocumentSnapshot): PurchaseInvoice? {
        val supplierName = doc.getString("supplierName") ?: return null
        val invNum = doc.getString("invoiceNumber") ?: doc.id
        val totalCost = (doc.get("totalCost") as? Number)?.toDouble() ?: 0.0
        val paidAmount = (doc.get("paidAmount") as? Number)?.toDouble() ?: 0.0
        val remainingDebt = (doc.get("remainingDebt") as? Number)?.toDouble()
            ?: (totalCost - paidAmount).coerceAtLeast(0.0)
        val dateMillis = (doc.get("dateMillis") as? Number)?.toLong()
            ?: (doc.get("invoiceDate") as? Number)?.toLong()
            ?: System.currentTimeMillis()
        val updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: dateMillis
        return PurchaseInvoice(
            id = (doc.get("numId") as? Number)?.toInt() ?: 0,
            invoiceNumber = invNum,
            supplierName = supplierName,
            supplierPhone = doc.getString("supplierPhone") ?: "",
            dateMillis = dateMillis,
            totalCost = totalCost,
            paidAmount = paidAmount,
            remainingDebt = remainingDebt,
            paymentStatus = doc.getString("paymentStatus") ?: if (remainingDebt <= 0.009) "PAID" else "PARTIAL",
            notes = doc.getString("notes") ?: "",
            itemsJson = doc.getString("itemsJson") ?: "[]",
            itemsCount = (doc.get("itemsCount") as? Number)?.toInt() ?: 1,
            updatedAt = updatedAt,
            syncStatus = RecordSyncState.SYNCED
        )
    }

    internal suspend fun mergeInvoiceDocumentsIntoRoom(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>
    ): Int {
        var newInvoicesCount = 0
        val allSales = repository.getAllSalesOnce()
        val existingByInvoiceId = allSales.groupBy { it.invoiceGroupId }

        for (doc in documents) {
            val invoiceId = doc.getString("invoiceId") ?: doc.id
            val docStatus = doc.getString("status") ?: "completed"
            val docOrderType = doc.getString("orderType") ?: "direct"
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
                    val orderType = (raw["orderType"] as? String) ?: docOrderType
                    val status = (raw["status"] as? String) ?: docStatus
                    val expDelivery = (raw["expectedDeliveryDate"] as? Number)?.toLong() ?: (doc.getLong("expectedDeliveryDate") ?: 0L)
                    val notes = (raw["notes"] as? String) ?: (doc.getString("notes") ?: "")

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

            val docNotes = doc.getString("notes") ?: ""
            if (docStatus == "refunded") {
                val existingForInvoice = existingByInvoiceId[invoiceId]
                if (!existingForInvoice.isNullOrEmpty()) {
                    repository.deletePreSaleOrder(invoiceId)
                }
            } else if (salesToMerge.isNotEmpty()) {
                val existingForInvoice = existingByInvoiceId[invoiceId]
                if (existingForInvoice.isNullOrEmpty()) {
                    repository.saveSalesBatch(salesToMerge, enqueueForSync = false)
                    newInvoicesCount++
                } else {
                    val localStatus = existingForInvoice.first().status
                    val localNotes = existingForInvoice.first().notes
                    if ((localStatus != docStatus && docStatus.isNotBlank()) || (docNotes.isNotBlank() && localNotes != docNotes)) {
                        repository.updateRemoteInvoiceStatusAndNotesIfChanged(
                            invoiceGroupId = invoiceId,
                            remoteStatus = docStatus.ifBlank { localStatus },
                            remoteNotes = docNotes.ifBlank { localNotes }
                        )
                    }
                }
            }
        }
        return newInvoicesCount
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
        if (!activeAccount.isSyncEnabled && !activeAccount.isOwner) return
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
     * تنفيذ مزامنة تزايدية سريعة وخفيفة (Incremental Delta Sync) في أجزاء من الثانية.
     * تقوم بدفع السجلات المعلقة محلياً فقط، وسحب فقط السجلات السحابية المحدثة بعد آخر طابع مزامنة (updatedAt > lastSync)
     * مما يوفر البيانات والوقت بشكل جذري.
     */
    suspend fun performIncrementalSync(activeAccount: StaffAccount? = null): ComprehensiveSyncResult = withContext(Dispatchers.IO) {
        val syncStartTime = System.currentTimeMillis()
        val prefs = try {
            context.getSharedPreferences("af_store_sync_prefs", Context.MODE_PRIVATE)
        } catch (_: Exception) { null }

        val lastSync = _lastSyncTimestamp.value.takeIf { it > 0L }
            ?: (prefs?.getLong("last_incremental_sync_timestamp", 0L) ?: 0L)

        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            repository.markAllTablesAndQueueAsSynced()
            repository.clearSyncedQueueItems()
            val offlineMsg = "حسابك في وضع العمل دون إنترنت (Just Offline / Lite). تم حفظ كافة العمليات محلياً ✓"
            _syncMessage.value = offlineMsg
            _lastSyncTimestamp.value = syncStartTime
            prefs?.edit()?.putLong("last_incremental_sync_timestamp", syncStartTime)?.putString("last_sync_message", offlineMsg)?.apply()
            com.example.utils.OfflineSyncManager.notifySyncCompleted(syncStartTime, offlineMsg)
            return@withContext ComprehensiveSyncResult(true, offlineMsg, 0, 0, 0)
        }

        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) {
            repository.markAllTablesAndQueueAsSynced()
            repository.clearSyncedQueueItems()
            val offlineMsg = "تم تأكيد العمليات محلياً ✓"
            _syncMessage.value = offlineMsg
            _lastSyncTimestamp.value = syncStartTime
            prefs?.edit()?.putLong("last_incremental_sync_timestamp", syncStartTime)?.putString("last_sync_message", offlineMsg)?.apply()
            com.example.utils.OfflineSyncManager.notifySyncCompleted(syncStartTime, offlineMsg)
            return@withContext ComprehensiveSyncResult(true, offlineMsg, 0, 0, 0)
        }

        _isComprehensiveSyncing.value = true

        try {
            var pushedProductsCount = 0
            var pushedSalesCount = 0
            var pushedPurchasesCount = 0

            val candidateUids = resolveAllCandidateInventoryOwnerUids(activeAccount).ifEmpty { listOf(targetOwnerUid) }
            val isManagerOrOwner = activeAccount?.isRegularManager == true || activeAccount?.isOwner == true

            // 1. دفع السلع المحلية المعلقة فقط (مع تجاهل بيانات العينة الافتراضية لدى العمال)
            val pendingProducts = repository.getPendingProductsOnce()
            if (pendingProducts.isNotEmpty() && targetOwnerUid.isNotBlank()) {
                for (prod in pendingProducts) {
                    val isSeedSample = prod.updatedAt <= 1000L ||
                        (prod.firestoreId.isBlank() && prod.barcode.startsWith("6281000001"))
                    if (!isSeedSample || isManagerOrOwner) {
                        val pushRes = pushProductToCloud(prod, activeAccount)
                        if (pushRes.isSuccess) pushedProductsCount++
                    } else if (prod.id > 0) {
                        repository.markProductsAsSynced(listOf(prod.id))
                    }
                }
            }

            // 2. دفع المبيعات والفواتير المحلية المعلقة فقط
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
                        "expectedDeliveryDate" to first.expectedDeliveryDate,
                        "notes" to first.notes,
                        "workerName" to (activeAccount?.fullName?.ifBlank { activeAccount.email } ?: "كاشير"),
                        "workerUid" to (activeAccount?.userId ?: auth.currentUser?.uid ?: ""),
                        "serverUpdatedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to System.currentTimeMillis(),
                        "items" to sales.map { s ->
                            mapOf(
                                "saleId" to s.saleId,
                                "productId" to s.productId,
                                "productName" to s.productName,
                                "quantitySold" to s.quantitySold,
                                "totalPrice" to s.totalPrice,
                                "saleDate" to s.saleDate,
                                "customerId" to s.customerId,
                                "customerName" to s.customerName,
                                "isDebt" to s.isDebt,
                                "paymentMethod" to s.paymentMethod,
                                "invoiceGroupId" to invId,
                                "orderType" to s.orderType,
                                "status" to s.status,
                                "expectedDeliveryDate" to s.expectedDeliveryDate,
                                "notes" to s.notes
                            )
                        }
                    )
                    batch.set(invoicesCollection.document(getInvoiceDocId(invId)), invoicePayload, SetOptions.merge())
                }
                batch.commit().await()
                repository.markSalesAsSynced(pendingSales.map { it.saleId })
                pushedSalesCount = pendingSales.size
            }

            // 3. دفع فواتير المشتريات المعلقة فقط
            val pendingPurchases = repository.getPendingPurchaseInvoicesOnce()
            if (pendingPurchases.isNotEmpty() && targetOwnerUid.isNotBlank()) {
                for (pur in pendingPurchases) {
                    val pushRes = pushPurchaseInvoiceToCloud(pur, activeAccount)
                    if (pushRes.isSuccess) pushedPurchasesCount++
                }
            }

            // تفريغ طابور المزامنة
            repository.markSyncQueueByEntityTypeSynced("PRODUCT")
            repository.markSyncQueueByEntityTypeSynced("SALE")
            repository.markSyncQueueByEntityTypeSynced("PURCHASE_INVOICE")
            repository.clearSyncedQueueItems()

            // 4. سحب كتالوج السلع والكميات السحابية بالكامل لتجنب أي فروق توقيت بين الهواتف
            var pulledProductsCount = 0
            var pulledInvoicesCount = 0
            var pulledPurchasesCount = 0

            if (targetOwnerUid.isNotBlank()) {
                val remoteProductsByDocId = LinkedHashMap<String, Product>()
                for (uid in candidateUids) {
                    try {
                        val prodSnap = db.collection("users").document(uid).collection("products").get().await()
                        for (doc in prodSnap.documents) {
                            val p = parseProductDocument(doc) ?: continue
                            val resolvedImg = ProductCloudImageHelper.ensureLocalProductImage(context, p)
                            if (resolvedImg.isNotBlank()) p.imagePath = resolvedImg
                            val existing = remoteProductsByDocId[doc.id]
                            if (existing == null || p.updatedAt >= existing.updatedAt) {
                                remoteProductsByDocId[doc.id] = p
                            }
                        }
                    } catch (_: Exception) { }
                }
                val remoteProducts = remoteProductsByDocId.values.toList()
                if (remoteProducts.isNotEmpty()) {
                    pulledProductsCount = repository.mergeRemoteProductsWithTimestampResolution(remoteProducts)
                }

                // سحب الفواتير التزايدية
                val invoicesQuery = if (lastSync > 0L) {
                    db.collection("users").document(targetOwnerUid).collection("invoices")
                        .whereGreaterThan("updatedAt", lastSync)
                        .limit(100)
                } else {
                    db.collection("users").document(targetOwnerUid).collection("invoices")
                        .limit(50)
                }
                val invSnap = invoicesQuery.get().await()
                if (!invSnap.isEmpty) {
                    pulledInvoicesCount = mergeInvoiceDocumentsIntoRoom(invSnap.documents)
                }

                // سحب فواتير المشتريات التزايدية
                val purchaseQuery = if (lastSync > 0L) {
                    db.collection("users").document(targetOwnerUid).collection("purchase_invoices")
                        .whereGreaterThan("updatedAt", lastSync)
                        .limit(100)
                } else {
                    db.collection("users").document(targetOwnerUid).collection("purchase_invoices")
                        .limit(50)
                }
                val purSnap = purchaseQuery.get().await()
                if (!purSnap.isEmpty) {
                    val remotePurchases = purSnap.documents.mapNotNull { parsePurchaseInvoiceDoc(it) }
                    if (remotePurchases.isNotEmpty()) {
                        pulledPurchasesCount = repository.mergeRemotePurchaseInvoicesWithTimestampResolution(remotePurchases)
                    }
                }
            }

            val totalPushed = pushedProductsCount + (if (pushedSalesCount > 0) 1 else 0) + pushedPurchasesCount
            val totalPulled = pulledProductsCount + pulledInvoicesCount + pulledPurchasesCount
            val totalActivity = totalPushed + totalPulled

            val summaryMsg = if (totalActivity == 0) {
                "المزامنة التزايدية مكتملة — لا توجد تعديلات جديدة (البيانات متطابقة تماماً ✓)"
            } else {
                "تمت المزامنة التزايدية بنجاح ✓ (رفع: $totalPushed، استلام: $totalPulled)"
            }

            _lastSyncTimestamp.value = syncStartTime
            _syncMessage.value = summaryMsg
            _isComprehensiveSyncing.value = false

            prefs?.edit()
                ?.putLong("last_incremental_sync_timestamp", syncStartTime)
                ?.putString("last_sync_message", summaryMsg)
                ?.apply()

            com.example.utils.OfflineSyncManager.notifySyncCompleted(syncStartTime, summaryMsg)

            ComprehensiveSyncResult(
                isSuccess = true,
                message = summaryMsg,
                productsCount = pulledProductsCount,
                invoicesCount = pulledInvoicesCount,
                locationsCount = 0
            )
        } catch (e: Exception) {
            _isComprehensiveSyncing.value = false
            val isPermissionDenied = e is com.google.firebase.firestore.FirebaseFirestoreException &&
                e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            val errorMsg = if (isPermissionDenied) {
                "المزامنة السحابية مقيدة (وضع Offline/Lite): تم حفظ التغييرات محلياً بأمان ✓"
            } else {
                "حدث خطأ أثناء المزامنة التزايدية: ${e.localizedMessage ?: e.message}"
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
     * فحص وتدقيق المزامنة الشاملة فوراً لكل أركان التطبيق (منتجات، فواتير، مشتريات، مواقع، طابور معلق).
     */
    suspend fun triggerComprehensiveSync(activeAccount: StaffAccount?): ComprehensiveSyncResult = withContext(Dispatchers.IO) {
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            repository.markAllTablesAndQueueAsSynced()
            repository.clearSyncedQueueItems()
            val offlineMsg = "حسابك في وضع العمل دون إنترنت (Just Offline / Lite). تم حفظ وتأكيد جميع العمليات محلياً على الجهاز ✓"
            _syncMessage.value = offlineMsg
            _isComprehensiveSyncing.value = false
            return@withContext ComprehensiveSyncResult(
                isSuccess = true,
                message = offlineMsg,
                productsCount = repository.getAllProductsOnce().size,
                invoicesCount = repository.getAllSalesOnce().map { it.invoiceGroupId }.distinct().size,
                locationsCount = 0
            )
        }

        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) {
            repository.markAllTablesAndQueueAsSynced()
            repository.clearSyncedQueueItems()
            val offlineMsg = "تم تأكيد وحفظ جميع العمليات في قاعدة البيانات المحلية بنجاح ✓"
            _syncMessage.value = offlineMsg
            _isComprehensiveSyncing.value = false
            return@withContext ComprehensiveSyncResult(
                isSuccess = true,
                message = offlineMsg,
                productsCount = repository.getAllProductsOnce().size,
                invoicesCount = repository.getAllSalesOnce().map { it.invoiceGroupId }.distinct().size,
                locationsCount = 0
            )
        }

        _isComprehensiveSyncing.value = true
        try {
            val candidateUids = resolveAllCandidateInventoryOwnerUids(activeAccount).ifEmpty { listOf(targetOwnerUid) }
            val isManagerOrOwner = activeAccount?.isRegularManager == true || activeAccount?.isOwner == true

            // 1. دفع أي منتجات محلية معلقة (Pending Products) إلى سحابة المدير
            val pendingProducts = repository.getPendingProductsOnce()
            if (pendingProducts.isNotEmpty() && targetOwnerUid.isNotBlank()) {
                for (prod in pendingProducts) {
                    val isSeedSample = prod.updatedAt <= 1000L ||
                        (prod.firestoreId.isBlank() && prod.barcode.startsWith("6281000001"))
                    if (!isSeedSample || isManagerOrOwner) {
                        pushProductToCloud(prod, activeAccount)
                    } else if (prod.id > 0) {
                        repository.markProductsAsSynced(listOf(prod.id))
                    }
                }
            }

            // 2. دفع أي مبيعات أو طلبات بيع مسبق محلية معلقة (Pending Sales / Invoices)
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
                        "expectedDeliveryDate" to first.expectedDeliveryDate,
                        "notes" to first.notes,
                        "workerName" to (activeAccount?.fullName?.ifBlank { activeAccount.email } ?: "كاشير"),
                        "workerUid" to (activeAccount?.userId ?: auth.currentUser?.uid ?: ""),
                        "serverUpdatedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to System.currentTimeMillis(),
                        "items" to sales.map { s ->
                            mapOf(
                                "saleId" to s.saleId,
                                "productId" to s.productId,
                                "productName" to s.productName,
                                "quantitySold" to s.quantitySold,
                                "totalPrice" to s.totalPrice,
                                "saleDate" to s.saleDate,
                                "customerId" to s.customerId,
                                "customerName" to s.customerName,
                                "isDebt" to s.isDebt,
                                "paymentMethod" to s.paymentMethod,
                                "invoiceGroupId" to invId,
                                "orderType" to s.orderType,
                                "status" to s.status,
                                "expectedDeliveryDate" to s.expectedDeliveryDate,
                                "notes" to s.notes
                            )
                        }
                    )
                    batch.set(invoicesCollection.document(getInvoiceDocId(invId)), invoicePayload, SetOptions.merge())
                }
                batch.commit().await()
                repository.markSalesAsSynced(pendingSales.map { it.saleId })
            }

            // 3. دفع أي فواتير مشتريات معلقة (Pending Purchase Invoices)
            val pendingPurchases = repository.getPendingPurchaseInvoicesOnce()
            if (pendingPurchases.isNotEmpty() && targetOwnerUid.isNotBlank()) {
                for (pur in pendingPurchases) {
                    pushPurchaseInvoiceToCloud(pur, activeAccount)
                }
            }

            // 4. سحب ودمج كتالوج السلع السحابي للمدير
            var productsCount = 0
            if (targetOwnerUid.isNotBlank()) {
                val remoteProductsByDocId = LinkedHashMap<String, Product>()
                for (uid in candidateUids) {
                    try {
                        val prodSnap = db.collection("users").document(uid).collection("products").get().await()
                        for (doc in prodSnap.documents) {
                            val p = parseProductDocument(doc) ?: continue
                            val resolvedImg = ProductCloudImageHelper.ensureLocalProductImage(context, p)
                            if (resolvedImg.isNotBlank()) p.imagePath = resolvedImg
                            val existing = remoteProductsByDocId[doc.id]
                            if (existing == null || p.updatedAt >= existing.updatedAt) {
                                remoteProductsByDocId[doc.id] = p
                            }
                        }
                    } catch (_: Exception) { }
                }
                val remoteList = remoteProductsByDocId.values.toList()
                productsCount = remoteList.size
                if (remoteList.isNotEmpty()) {
                    repository.mergeRemoteProductsWithTimestampResolution(remoteList)
                }

                // Also push any local products missing in cloud for Manager (or custom worker products)
                val allLocalProducts = repository.getAllProductsOnce()
                for (localProd in allLocalProducts) {
                    val docId = getInventoryDocId(localProd)
                    val remoteMatch = remoteProductsByDocId[docId]
                    val isSeedSample = localProd.updatedAt <= 1000L ||
                        (localProd.firestoreId.isBlank() && localProd.barcode.startsWith("6281000001"))
                    val shouldPush = if (isManagerOrOwner) {
                        remoteList.isEmpty() || (!isSeedSample && (remoteMatch == null || localProd.updatedAt > remoteMatch.updatedAt))
                    } else {
                        !isSeedSample && remoteMatch == null
                    }
                    if (shouldPush) {
                        val pushRes = pushProductToCloud(localProd, activeAccount)
                        if (pushRes.isSuccess && remoteMatch == null) productsCount++
                    }
                }
                _syncedProductsCount.value = productsCount
            }

            // 5. سحب ودمج الفواتير وطلبات البيع المسبق الصادرة من الفريق
            var invoicesCount = 0
            if (targetOwnerUid.isNotBlank()) {
                val invSnap = db.collection("users").document(targetOwnerUid).collection("invoices").get().await()
                invoicesCount = invSnap.size()
                if (!invSnap.isEmpty) {
                    mergeInvoiceDocumentsIntoRoom(invSnap.documents)
                }
                _syncedInvoicesCount.value = invoicesCount
            }

            // 6. سحب ودمج فواتير المشتريات السحابية
            if (targetOwnerUid.isNotBlank()) {
                val purSnap = db.collection("users").document(targetOwnerUid).collection("purchase_invoices").get().await()
                if (!purSnap.isEmpty) {
                    val remotePurchases = purSnap.documents.mapNotNull { doc ->
                        val supplierName = doc.getString("supplierName") ?: return@mapNotNull null
                        val invNum = doc.getString("invoiceNumber") ?: doc.id
                        val totalCost = (doc.get("totalCost") as? Number)?.toDouble() ?: 0.0
                        val paidAmount = (doc.get("paidAmount") as? Number)?.toDouble() ?: 0.0
                        val remainingDebt = (doc.get("remainingDebt") as? Number)?.toDouble()
                            ?: (totalCost - paidAmount).coerceAtLeast(0.0)
                        val dateMillis = (doc.get("dateMillis") as? Number)?.toLong()
                            ?: (doc.get("invoiceDate") as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                        val updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: dateMillis
                        PurchaseInvoice(
                            id = (doc.get("numId") as? Number)?.toInt() ?: 0,
                            invoiceNumber = invNum,
                            supplierName = supplierName,
                            supplierPhone = doc.getString("supplierPhone") ?: "",
                            dateMillis = dateMillis,
                            totalCost = totalCost,
                            paidAmount = paidAmount,
                            remainingDebt = remainingDebt,
                            paymentStatus = doc.getString("paymentStatus") ?: if (remainingDebt <= 0.009) "PAID" else "PARTIAL",
                            notes = doc.getString("notes") ?: "",
                            itemsJson = doc.getString("itemsJson") ?: "[]",
                            itemsCount = (doc.get("itemsCount") as? Number)?.toInt() ?: 1,
                            updatedAt = updatedAt,
                            syncStatus = RecordSyncState.SYNCED
                        )
                    }
                    if (remotePurchases.isNotEmpty()) {
                        repository.mergeRemotePurchaseInvoicesWithTimestampResolution(remotePurchases)
                    }
                }
            }

            // 7. سحب مواقع العمال GPS
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

            // 8. تأكيد مزامنة جميع الجداول وتفريغ طابور المزامنة المحلي
            repository.markAllTablesAndQueueAsSynced()
            repository.clearSyncedQueueItems()

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
     * رفع سجل بضاعة تالفة مع ضغط صورتها إلى Base64 لمزامنتها سحابياً بين المدير والعمال فورياً.
     */
    suspend fun pushDamagedItemToCloud(
        item: DamagedItemRecord,
        activeAccount: StaffAccount? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            return@withContext Result.success(item.id)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) {
            return@withContext Result.failure(IllegalStateException("لم يتم العثور على معرّف سحابي لمخزون المدير"))
        }

        try {
            val safeDocId = item.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "dmg_${System.currentTimeMillis()}" }
            val docRef = db.collection("users").document(targetOwnerUid).collection("damaged_goods").document(safeDocId)

            val base64Image = if (item.imagePath.isNotBlank()) {
                ProductCloudImageHelper.compressLocalImageToBase64(context, item.imagePath) ?: item.imageUrl
            } else {
                item.imageUrl
            }

            val payload = hashMapOf<String, Any?>(
                "id" to safeDocId,
                "productId" to item.productId,
                "productName" to item.productName,
                "productBarcode" to item.productBarcode,
                "quantity" to item.quantity,
                "unitCost" to item.unitCost,
                "totalLoss" to item.totalLoss,
                "reason" to item.reason.id,
                "dateMillis" to item.dateMillis,
                "recordedBy" to item.recordedBy,
                "notes" to item.notes,
                "batchNumber" to item.batchNumber,
                "imagePath" to item.imagePath,
                "imageUrl" to (base64Image ?: ""),
                "deductedFromStock" to item.deductedFromStock,
                "updatedAt" to System.currentTimeMillis(),
                "serverUpdatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload, SetOptions.merge()).await()
            _lastSyncTimestamp.value = System.currentTimeMillis()
            Log.i(TAG, "تم رفع سجل التالف «${item.productName}» وصورته للسحابة بنجاح.")
            Result.success(base64Image ?: "")
        } catch (e: Exception) {
            Log.e(TAG, "فشل رفع سجل التالف إلى Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * حذف سجل بضاعة تالفة من السحابة.
     */
    suspend fun deleteDamagedItemFromCloud(
        itemId: String,
        activeAccount: StaffAccount? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (activeAccount != null && !activeAccount.isSyncEnabled && !activeAccount.isOwner) {
            return@withContext Result.success(Unit)
        }
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return@withContext Result.success(Unit)

        try {
            val safeDocId = itemId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            db.collection("users").document(targetOwnerUid).collection("damaged_goods").document(safeDocId).delete().await()
            _lastSyncTimestamp.value = System.currentTimeMillis()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "تعذر حذف سجل التالف من السحابة: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * بدء مستمع المزامنة الحية لسجلات البضائع التالفة وصورها بين جميع الأجهزة.
     */
    fun startLiveDamagedGoodsSync(
        activeAccount: StaffAccount,
        expiryAndWasteRepository: ExpiryAndWasteRepository,
        onStatusMessage: (String) -> Unit = {}
    ) {
        if (!activeAccount.isSyncEnabled && !activeAccount.isOwner) return
        val targetOwnerUid = resolveInventoryOwnerUid(activeAccount)
        if (targetOwnerUid.isBlank()) return

        stopLiveDamagedGoodsSync()

        val collectionRef = db.collection("users").document(targetOwnerUid).collection("damaged_goods")
        damagedGoodsListenerRegistration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            scope.launch(Dispatchers.IO) {
                try {
                    val remoteItems = mutableListOf<DamagedItemRecord>()
                    for (doc in snapshot.documents) {
                        val id = doc.getString("id") ?: doc.id
                        val productId = (doc.get("productId") as? Number)?.toInt() ?: 0
                        val productName = doc.getString("productName") ?: continue
                        val productBarcode = doc.getString("productBarcode") ?: ""
                        val quantity = (doc.get("quantity") as? Number)?.toInt() ?: 1
                        val unitCost = (doc.get("unitCost") as? Number)?.toDouble() ?: 0.0
                        val totalLoss = (doc.get("totalLoss") as? Number)?.toDouble() ?: (quantity * unitCost)
                        val reason = SpoilageReason.fromId(doc.getString("reason") ?: "OTHER")
                        val dateMillis = (doc.get("dateMillis") as? Number)?.toLong() ?: System.currentTimeMillis()
                        val recordedBy = doc.getString("recordedBy") ?: ""
                        val notes = doc.getString("notes") ?: ""
                        val batchNumber = doc.getString("batchNumber") ?: ""
                        val imagePath = doc.getString("imagePath") ?: ""
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        val deductedFromStock = doc.getBoolean("deductedFromStock") ?: true
                        val updatedAt = (doc.get("updatedAt") as? Number)?.toLong() ?: dateMillis

                        remoteItems.add(
                            DamagedItemRecord(
                                id = id,
                                productId = productId,
                                productName = productName,
                                productBarcode = productBarcode,
                                quantity = quantity,
                                unitCost = unitCost,
                                totalLoss = totalLoss,
                                reason = reason,
                                dateMillis = dateMillis,
                                recordedBy = recordedBy,
                                notes = notes,
                                batchNumber = batchNumber,
                                imagePath = imagePath,
                                imageUrl = imageUrl,
                                deductedFromStock = deductedFromStock,
                                updatedAt = updatedAt
                            )
                        )
                    }
                    if (remoteItems.isNotEmpty()) {
                        val merged = expiryAndWasteRepository.mergeRemoteDamagedItems(remoteItems)
                        if (merged > 0) {
                            _lastSyncTimestamp.value = System.currentTimeMillis()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "خطأ أثناء دمج سجلات التوالف السحابية: ${e.message}")
                }
            }
        }
    }

    fun stopLiveDamagedGoodsSync() {
        damagedGoodsListenerRegistration?.remove()
        damagedGoodsListenerRegistration = null
    }

    /**
     * إيقاف جميع مستمعي المزامنة الحية دفعة واحدة.
     */
    fun stopAllLiveSync() {
        stopLiveInventorySync()
        stopLiveInvoicesSync()
        stopLivePurchaseInvoicesSync()
        stopLiveWorkerLocationsSync()
        stopLiveDamagedGoodsSync()
    }

    data class ComprehensiveSyncResult(
        val isSuccess: Boolean,
        val message: String,
        val productsCount: Int,
        val invoicesCount: Int,
        val locationsCount: Int
    )
}
