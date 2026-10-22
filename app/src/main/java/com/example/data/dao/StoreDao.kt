package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.models.ActivityLog
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoice
import com.example.data.models.RecordSyncState
import com.example.data.models.Sale
import com.example.data.models.SyncQueueItem
import com.example.data.models.User
import com.example.data.models.WorkerLocation
import com.example.data.models.WorkerRouteCoordinate
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    // Products
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsOnce(): List<Product>

    @Query("SELECT * FROM products WHERE syncStatus != 'SYNCED' ORDER BY updatedAt ASC")
    suspend fun getPendingProductsOnce(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND (barcode = :code OR cartonBarcode = :code) LIMIT 1")
    suspend fun getProductByBarcode(code: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET isDeleted = 1, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :productId")
    suspend fun softDeleteProduct(productId: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markProductsAsSynced(ids: List<Int>)

    @Query("UPDATE products SET syncStatus = 'SYNCED'")
    suspend fun markAllProductsAsSynced()

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    // Sales
    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun getAllSalesFlow(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    suspend fun getAllSalesOnce(): List<Sale>

    @Query("SELECT * FROM sales WHERE syncStatus != 'SYNCED' ORDER BY saleDate ASC")
    suspend fun getPendingSalesOnce(): List<Sale>

    @Query("SELECT * FROM sales WHERE syncStatus != 'SYNCED' ORDER BY saleDate DESC")
    fun getPendingSalesFlow(): Flow<List<Sale>>

    @Query("SELECT COUNT(*) FROM sales WHERE syncStatus != 'SYNCED'")
    fun getPendingSalesCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<Sale>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesWithIds(sales: List<Sale>): List<Long>

    @Query("UPDATE sales SET syncStatus = 'SYNCED' WHERE saleId IN (:saleIds)")
    suspend fun markSalesAsSynced(saleIds: List<Int>)

    @Query("UPDATE sales SET syncStatus = 'SYNCED'")
    suspend fun markAllSalesAsSynced()

    @Delete
    suspend fun deleteSale(sale: Sale)

    @Query("DELETE FROM sales")
    suspend fun clearAllSales()

    // Customers
    @Query("SELECT * FROM customers ORDER BY isFavorite DESC, name ASC")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersOnce(): List<Customer>

    @Query("SELECT * FROM customers WHERE syncStatus != 'SYNCED' ORDER BY updatedAt ASC")
    suspend fun getPendingCustomersOnce(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Int): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markCustomersAsSynced(ids: List<Int>)

    @Query("UPDATE customers SET syncStatus = 'SYNCED'")
    suspend fun markAllCustomersAsSynced()

    @Query("UPDATE customers SET loyaltyGoal = :goal, loyaltyRewardValue = :rewardValue")
    suspend fun updateAllCustomersLoyaltyRules(goal: Int, rewardValue: Double)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()

    // Customer Transactions
    @Query("SELECT * FROM customer_transactions ORDER BY date DESC")
    fun getAllCustomerTransactionsFlow(): Flow<List<CustomerTransaction>>

    @Query("SELECT * FROM customer_transactions ORDER BY date DESC")
    suspend fun getAllCustomerTransactionsOnce(): List<CustomerTransaction>

    @Query("SELECT * FROM customer_transactions WHERE syncStatus != 'SYNCED' ORDER BY date ASC")
    suspend fun getPendingCustomerTransactionsOnce(): List<CustomerTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerTransaction(transaction: CustomerTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerTransactions(transactions: List<CustomerTransaction>)

    @Query("UPDATE customer_transactions SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markCustomerTransactionsAsSynced(ids: List<Int>)

    @Query("UPDATE customer_transactions SET syncStatus = 'SYNCED'")
    suspend fun markAllCustomerTransactionsAsSynced()

    @Query("DELETE FROM customer_transactions")
    suspend fun clearAllCustomerTransactions()

    // Purchase Invoices
    @Query("SELECT * FROM purchase_invoices ORDER BY dateMillis DESC")
    fun getAllPurchaseInvoicesFlow(): Flow<List<PurchaseInvoice>>

    @Query("SELECT * FROM purchase_invoices ORDER BY dateMillis DESC")
    suspend fun getAllPurchaseInvoicesOnce(): List<PurchaseInvoice>

    @Query("SELECT * FROM purchase_invoices WHERE syncStatus != 'SYNCED' ORDER BY dateMillis ASC")
    suspend fun getPendingPurchaseInvoicesOnce(): List<PurchaseInvoice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseInvoice(invoice: PurchaseInvoice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseInvoices(invoices: List<PurchaseInvoice>)

    @Query("UPDATE purchase_invoices SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markPurchaseInvoicesAsSynced(ids: List<Int>)

    @Query("UPDATE purchase_invoices SET syncStatus = 'SYNCED'")
    suspend fun markAllPurchaseInvoicesAsSynced()

    @Delete
    suspend fun deletePurchaseInvoice(invoice: PurchaseInvoice)

    @Query("DELETE FROM purchase_invoices")
    suspend fun clearAllPurchaseInvoices()

    // Users
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersOnce(): List<User>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 250")
    fun getRecentActivityLogsFlow(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 500")
    suspend fun getAllActivityLogsOnce(): List<ActivityLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLogs(logs: List<ActivityLog>)

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllActivityLogs()

    // Sync Queue (Persistent Offline Queue)
    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC LIMIT 200")
    fun getAllSyncQueueFlow(): Flow<List<SyncQueueItem>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status != 'SYNCED'")
    fun getPendingSyncQueueCountFlow(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE status != 'SYNCED' ORDER BY createdAt ASC")
    suspend fun getPendingSyncQueueItemsOnce(): List<SyncQueueItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItem(item: SyncQueueItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueueItems(items: List<SyncQueueItem>)

    @Query("UPDATE sync_queue SET status = 'SYNCED', lastError = '', updatedAt = :now WHERE id IN (:ids)")
    suspend fun markSyncQueueItemsSynced(ids: List<Int>, now: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'SYNCED', lastError = '', updatedAt = :now WHERE status != 'SYNCED'")
    suspend fun markAllPendingQueueItemsSynced(now: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'SYNCED', lastError = '', updatedAt = :now WHERE entityType = :entityType AND status != 'SYNCED'")
    suspend fun markSyncQueueByEntityTypeSynced(entityType: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'FAILED', retryCount = retryCount + 1, lastError = :errorMsg, updatedAt = :now WHERE id IN (:ids)")
    suspend fun markSyncQueueItemsFailed(ids: List<Int>, errorMsg: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'PENDING', lastError = '', updatedAt = :now WHERE status = 'FAILED'")
    suspend fun retryFailedQueueItems(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSyncedQueueItems()

    @Query("DELETE FROM sync_queue")
    suspend fun clearAllSyncQueue()

    // Atomic Offline Transactions (@Transaction)
    @Transaction
    suspend fun saveProductOfflineAtomically(product: Product): Long {
        val now = System.currentTimeMillis()
        val isNew = product.id == 0
        val stamped = product.copy(
            updatedAt = now,
            syncStatus = RecordSyncState.PENDING
        )
        val savedId = insertProduct(stamped)
        val resolvedId = if (product.id > 0) product.id else savedId.toInt()
        insertSyncQueueItem(
            SyncQueueItem(
                entityType = "PRODUCT",
                entityId = resolvedId,
                operation = if (isNew) "CREATE" else "UPDATE",
                titleAr = if (isNew) "إضافة صنف: ${product.name}" else "تحديث صنف: ${product.name}",
                detailsAr = "الكمية: ${product.stock} ق • السعر: ${product.price}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        return savedId
    }

    @Transaction
    suspend fun softDeleteProductOfflineAtomically(productId: Int) {
        val now = System.currentTimeMillis()
        val existing = getProductById(productId)
        softDeleteProduct(productId, now)
        insertSyncQueueItem(
            SyncQueueItem(
                entityType = "PRODUCT",
                entityId = productId,
                operation = "DELETE",
                titleAr = "حذف صنف: ${existing?.name ?: "#$productId"}",
                detailsAr = "نقل الصنف إلى المحذوفات محلياً",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    @Transaction
    suspend fun deleteSaleAndRestoreStockAtomically(sale: Sale) {
        val now = System.currentTimeMillis()
        val product = getProductById(sale.productId)
        if (product != null) {
            val restoredStock = product.stock + sale.quantitySold
            updateProduct(
                product.copy(
                    stock = restoredStock,
                    updatedAt = now,
                    syncStatus = RecordSyncState.PENDING
                )
            )
            insertSyncQueueItem(
                SyncQueueItem(
                    entityType = "PRODUCT",
                    entityId = product.id,
                    operation = "UPDATE",
                    titleAr = "مرتجع مبيعات: ${product.name}",
                    detailsAr = "استعادة +${sale.quantitySold} وحدة (الرصيد: $restoredStock)",
                    status = RecordSyncState.PENDING,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        deleteSale(sale)
    }

    @Transaction
    suspend fun recordPosCheckoutOfflineAtomically(
        salesToInsert: List<Sale>,
        productsToUpdate: List<Product>,
        updatedCustomer: Customer? = null,
        debtTransaction: CustomerTransaction? = null
    ): List<Long> {
        if (salesToInsert.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val invoiceCode = salesToInsert.first().invoiceGroupId.ifBlank {
            "INV-${(salesToInsert.first().saleDate / 1000).toString().takeLast(6)}"
        }
        val stampedSales = salesToInsert.map {
            it.copy(
                invoiceGroupId = invoiceCode,
                updatedAt = now,
                syncStatus = RecordSyncState.PENDING
            )
        }
        val insertedSaleIds = insertSalesWithIds(stampedSales)

        if (productsToUpdate.isNotEmpty()) {
            val stampedProducts = productsToUpdate.map {
                it.copy(
                    updatedAt = now,
                    syncStatus = RecordSyncState.PENDING
                )
            }
            insertProducts(stampedProducts)
        }

        if (updatedCustomer != null) {
            val stampedCustomer = updatedCustomer.copy(
                updatedAt = now,
                syncStatus = RecordSyncState.PENDING
            )
            insertCustomer(stampedCustomer)
            insertSyncQueueItem(
                SyncQueueItem(
                    entityType = "CUSTOMER",
                    entityId = stampedCustomer.id,
                    operation = "UPDATE",
                    titleAr = "تحديث رصيد عميل: ${stampedCustomer.name}",
                    detailsAr = "المديونية: ${String.format(java.util.Locale.US, "%.2f", stampedCustomer.balanceDebt)}",
                    status = RecordSyncState.PENDING,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        if (debtTransaction != null) {
            val txId = insertCustomerTransaction(
                debtTransaction.copy(
                    updatedAt = now,
                    syncStatus = RecordSyncState.PENDING
                )
            )
            insertSyncQueueItem(
                SyncQueueItem(
                    entityType = "CUSTOMER_TX",
                    entityId = txId.toInt(),
                    operation = "CREATE",
                    titleAr = "تسجيل مديونية فاتورة $invoiceCode: ${debtTransaction.customerName}",
                    detailsAr = "المبلغ الآجل: ${String.format(java.util.Locale.US, "%.2f", debtTransaction.amount)}",
                    status = RecordSyncState.PENDING,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val totalSum = stampedSales.sumOf { it.totalPrice }
        val totalUnits = stampedSales.sumOf { it.quantitySold }
        insertSyncQueueItem(
            SyncQueueItem(
                entityType = "SALE",
                entityId = insertedSaleIds.firstOrNull()?.toInt() ?: 0,
                operation = "CHECKOUT",
                titleAr = "فاتورة مبيعات $invoiceCode (${stampedSales.size} أصناف)",
                detailsAr = "إجمالي الوحدات: $totalUnits ق • القيمة: ${String.format(java.util.Locale.US, "%.2f", totalSum)}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        return insertedSaleIds
    }

    // Pre-Sales Management
    @Query("SELECT * FROM sales WHERE orderType = 'pre_sale' ORDER BY saleDate DESC")
    fun getAllPreSalesFlow(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE orderType = 'pre_sale' AND status = 'pending' ORDER BY saleDate DESC")
    fun getPendingPreSalesFlow(): Flow<List<Sale>>

    @Query("UPDATE sales SET status = :newStatus, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE invoiceGroupId = :invoiceGroupId")
    suspend fun updateSalesStatusByInvoiceGroup(
        invoiceGroupId: String,
        newStatus: String,
        updatedAt: Long = System.currentTimeMillis(),
        syncStatus: String = RecordSyncState.PENDING
    )

    @Query("DELETE FROM sales WHERE invoiceGroupId = :invoiceGroupId")
    suspend fun deleteSalesByInvoiceGroup(invoiceGroupId: String)

    @Transaction
    suspend fun recordPreSaleOrderAtomically(
        salesToInsert: List<Sale>
    ): List<Long> {
        if (salesToInsert.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val invoiceCode = salesToInsert.first().invoiceGroupId.ifBlank {
            "PRE-${(now / 1000).toString().takeLast(6)}"
        }
        val stampedSales = salesToInsert.map {
            it.copy(
                invoiceGroupId = invoiceCode,
                orderType = "pre_sale",
                status = "pending",
                updatedAt = now,
                syncStatus = RecordSyncState.PENDING
            )
        }
        val ids = insertSalesWithIds(stampedSales)
        val totalSum = stampedSales.sumOf { it.totalPrice }
        val totalUnits = stampedSales.sumOf { it.quantitySold }
        insertSyncQueueItem(
            SyncQueueItem(
                entityType = "PRE_SALE",
                entityId = ids.firstOrNull()?.toInt() ?: 0,
                operation = "CREATE",
                titleAr = "طلب بيع مسبق $invoiceCode (${stampedSales.size} أصناف)",
                detailsAr = "محجوز: $totalUnits ق • القيمة: ${String.format(java.util.Locale.US, "%.2f", totalSum)}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        return ids
    }

    @Transaction
    suspend fun fulfillPreSaleOrderAtomically(
        invoiceGroupId: String,
        productsToUpdate: List<Product>,
        updatedCustomer: Customer? = null,
        debtTransaction: CustomerTransaction? = null
    ) {
        val now = System.currentTimeMillis()
        updateSalesStatusByInvoiceGroup(
            invoiceGroupId = invoiceGroupId,
            newStatus = "completed",
            updatedAt = now,
            syncStatus = RecordSyncState.PENDING
        )

        if (productsToUpdate.isNotEmpty()) {
            val stampedProducts = productsToUpdate.map {
                it.copy(updatedAt = now, syncStatus = RecordSyncState.PENDING)
            }
            insertProducts(stampedProducts)
        }

        if (updatedCustomer != null) {
            val stampedCustomer = updatedCustomer.copy(updatedAt = now, syncStatus = RecordSyncState.PENDING)
            insertCustomer(stampedCustomer)
            insertSyncQueueItem(
                SyncQueueItem(
                    entityType = "CUSTOMER",
                    entityId = stampedCustomer.id,
                    operation = "UPDATE",
                    titleAr = "تحديث رصيد عميل (تسليم طلب مسبق $invoiceGroupId): ${stampedCustomer.name}",
                    detailsAr = "المديونية: ${String.format(java.util.Locale.US, "%.2f", stampedCustomer.balanceDebt)}",
                    status = RecordSyncState.PENDING,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        if (debtTransaction != null) {
            val txId = insertCustomerTransaction(
                debtTransaction.copy(updatedAt = now, syncStatus = RecordSyncState.PENDING)
            )
            insertSyncQueueItem(
                SyncQueueItem(
                    entityType = "TRANSACTION",
                    entityId = txId.toInt(),
                    operation = "CREATE",
                    titleAr = "تسجيل مديونية تسليم طلب مسبق $invoiceGroupId: ${debtTransaction.customerName}",
                    detailsAr = "المبلغ الآجل: ${String.format(java.util.Locale.US, "%.2f", debtTransaction.amount)}",
                    status = RecordSyncState.PENDING,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        insertSyncQueueItem(
            SyncQueueItem(
                entityType = "PRE_SALE",
                entityId = 0,
                operation = "FULFILL",
                titleAr = "تسليم طلب مسبق $invoiceGroupId من المستودع",
                detailsAr = "تم خصم البضاعة من المخزون وتأكيد الفاتورة",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    @Transaction
    suspend fun markAllTablesAndQueueAsSyncedAtomically() {
        val now = System.currentTimeMillis()
        markAllProductsAsSynced()
        markAllSalesAsSynced()
        markAllCustomersAsSynced()
        markAllCustomerTransactionsAsSynced()
        markAllPurchaseInvoicesAsSynced()
        markAllPendingQueueItemsSynced(now)
    }

    // Worker Locations
    @Query("SELECT * FROM worker_locations ORDER BY timestamp DESC")
    fun getAllWorkerLocationsFlow(): Flow<List<WorkerLocation>>

    @Query("SELECT * FROM worker_locations WHERE userId = :userId LIMIT 1")
    suspend fun getWorkerLocationById(userId: Int): WorkerLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWorkerLocation(location: WorkerLocation)

    @Query("DELETE FROM worker_locations WHERE userId = :userId")
    suspend fun deleteWorkerLocation(userId: Int)

    // Worker Route Coordinates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkerRouteCoordinate(coordinate: WorkerRouteCoordinate)

    @Query("SELECT * FROM worker_route_coordinates WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getWorkerRouteCoordinatesForDay(userId: Int, startTime: Long, endTime: Long): Flow<List<WorkerRouteCoordinate>>

    @Query("DELETE FROM worker_route_coordinates WHERE timestamp < :thresholdTime")
    suspend fun deleteOldWorkerRouteCoordinates(thresholdTime: Long)
}

