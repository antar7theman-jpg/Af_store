package com.example.data

import com.example.data.dao.StoreDao
import com.example.data.models.ActivityLog
import com.example.data.models.ArchivedInvoice
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.Product
import com.example.data.models.PurchaseInvoice
import com.example.data.models.PurchaseInvoiceItem
import com.example.data.models.RecordSyncState
import com.example.data.models.Sale
import com.example.data.models.SyncQueueItem
import com.example.data.models.User
import com.example.data.models.WorkerLocation
import com.example.data.models.WorkerRouteCoordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat

class AntarRepository(private val dao: StoreDao) {

    val allProductsFlow: Flow<List<Product>> = dao.getAllProductsFlow()
    val allSalesFlow: Flow<List<Sale>> = dao.getAllSalesFlow()
    val allCustomersFlow: Flow<List<Customer>> = dao.getAllCustomersFlow()
    val allCustomerTransactionsFlow: Flow<List<CustomerTransaction>> = dao.getAllCustomerTransactionsFlow()
    val allPurchaseInvoicesFlow: Flow<List<PurchaseInvoice>> = dao.getAllPurchaseInvoicesFlow()
    val allUsersFlow: Flow<List<User>> = dao.getAllUsersFlow()
    val allWorkerLocationsFlow: Flow<List<WorkerLocation>> = dao.getAllWorkerLocationsFlow()
    val recentLogsFlow: Flow<List<ActivityLog>> = dao.getRecentActivityLogsFlow()
    val syncQueueFlow: Flow<List<SyncQueueItem>> = dao.getAllSyncQueueFlow()
    val pendingSyncQueueCountFlow: Flow<Int> = dao.getPendingSyncQueueCountFlow()
    val pendingSalesFlow: Flow<List<Sale>> = dao.getPendingSalesFlow()
    val pendingSalesCountFlow: Flow<Int> = dao.getPendingSalesCountFlow()

    val archivedInvoicesFlow: Flow<List<ArchivedInvoice>> = allSalesFlow.map { sales ->
        // Group sales by explicit invoiceGroupId if present, or fallback to checkout timestamp & customer
        val usedIds = HashSet<String>()
        sales.groupBy { sale ->
            sale.invoiceGroupId.ifBlank {
                "${sale.saleDate / 1000}_${sale.customerId ?: 0}_${sale.customerName}"
            }
        }
            .values
            .sortedBy { it.first().saleDate }
            .mapIndexed { index, groupedSales ->
                val first = groupedSales.first()
                val hasDebt = groupedSales.any { it.isDebt }
                val allDebt = groupedSales.all { it.isDebt }
                val resolvedMethod = when {
                    allDebt -> "DEBT"
                    hasDebt -> "PARTIAL_DEBT"
                    else -> first.paymentMethod
                }
                val dateStr = SimpleDateFormat("ddMMyy", Locale.US).format(Date(first.saleDate))
                val isPreSale = first.orderType == "pre_sale"
                val prefix = if (isPreSale) "Pre #" else "#"
                val baseId = if (first.invoiceGroupId.isNotBlank() && (first.invoiceGroupId.startsWith("#") || first.invoiceGroupId.startsWith("Pre "))) {
                    first.invoiceGroupId
                } else {
                    "$prefix${index + 1}-$dateStr"
                }
                val uniqueInvoiceId = if (usedIds.add(baseId)) {
                    baseId
                } else {
                    val candidate = "$prefix${index + 1}-$dateStr"
                    usedIds.add(candidate)
                    candidate
                }
                val invoiceSyncStatus = when {
                    groupedSales.any { it.syncStatus == RecordSyncState.FAILED } -> RecordSyncState.FAILED
                    groupedSales.any { it.syncStatus != RecordSyncState.SYNCED } -> RecordSyncState.PENDING
                    else -> RecordSyncState.SYNCED
                }
                ArchivedInvoice(
                    invoiceId = uniqueInvoiceId,
                    dateMillis = first.saleDate,
                    customerName = first.customerName.ifBlank { "عميل نقدي" },
                    customerId = first.customerId,
                    paymentMethod = resolvedMethod,
                    isDebt = hasDebt,
                    items = groupedSales,
                    subtotal = groupedSales.sumOf { it.totalPrice },
                    totalQuantity = groupedSales.sumOf { it.quantitySold },
                    syncStatus = invoiceSyncStatus,
                    orderType = first.orderType,
                    status = first.status,
                    expectedDeliveryDate = first.expectedDeliveryDate,
                    notes = first.notes
                )
            }
            .sortedByDescending { it.dateMillis }
    }.flowOn(Dispatchers.Default)


    val preSaleOrdersFlow: Flow<List<ArchivedInvoice>> = archivedInvoicesFlow.map { invoices ->
        invoices.filter { it.isPreSale }
    }.flowOn(Dispatchers.Default)

    val pendingPreSaleOrdersCountFlow: Flow<Int> = preSaleOrdersFlow.map { list ->
        list.count { it.isPendingPreSale }
    }.flowOn(Dispatchers.Default)

    suspend fun recordPreSaleOrderAtomically(sales: List<Sale>): List<Long> {
        return dao.recordPreSaleOrderAtomically(sales)
    }

    suspend fun fulfillPreSaleOrderAtomically(
        invoiceGroupId: String,
        productsToUpdate: List<Product>,
        updatedCustomer: Customer? = null,
        debtTransaction: CustomerTransaction? = null
    ) {
        dao.fulfillPreSaleOrderAtomically(
            invoiceGroupId = invoiceGroupId,
            productsToUpdate = productsToUpdate,
            updatedCustomer = updatedCustomer,
            debtTransaction = debtTransaction
        )
    }

    suspend fun cancelPreSaleOrder(invoiceGroupId: String) {
        val now = System.currentTimeMillis()
        dao.updateSalesStatusByInvoiceGroup(invoiceGroupId, "cancelled", now, RecordSyncState.PENDING)
    }

    suspend fun deletePreSaleOrder(invoiceGroupId: String) {
        dao.deleteSalesByInvoiceGroup(invoiceGroupId)
    }

    suspend fun getAllProductsOnce(): List<Product> = dao.getAllProductsOnce()
    suspend fun getAllSalesOnce(): List<Sale> = dao.getAllSalesOnce()
    suspend fun getAllCustomersOnce(): List<Customer> = dao.getAllCustomersOnce()
    suspend fun getAllCustomerTransactionsOnce(): List<CustomerTransaction> = dao.getAllCustomerTransactionsOnce()
    suspend fun getAllPurchaseInvoicesOnce(): List<PurchaseInvoice> = dao.getAllPurchaseInvoicesOnce()
    suspend fun getAllUsersOnce(): List<User> = dao.getAllUsersOnce()
    suspend fun getAllActivityLogsOnce(): List<ActivityLog> = dao.getAllActivityLogsOnce()

    // Pending Offline Records & Queue Accessors
    suspend fun getPendingProductsOnce(): List<Product> = dao.getPendingProductsOnce()
    suspend fun getPendingSalesOnce(): List<Sale> = dao.getPendingSalesOnce()
    suspend fun getPendingCustomersOnce(): List<Customer> = dao.getPendingCustomersOnce()
    suspend fun getPendingCustomerTransactionsOnce(): List<CustomerTransaction> = dao.getPendingCustomerTransactionsOnce()
    suspend fun getPendingPurchaseInvoicesOnce(): List<PurchaseInvoice> = dao.getPendingPurchaseInvoicesOnce()
    suspend fun getPendingSyncQueueItemsOnce(): List<SyncQueueItem> = dao.getPendingSyncQueueItemsOnce()

    suspend fun markProductsAsSynced(ids: List<Int>) {
        if (ids.isNotEmpty()) dao.markProductsAsSynced(ids)
    }

    suspend fun markSalesAsSynced(saleIds: List<Int>) {
        if (saleIds.isNotEmpty()) dao.markSalesAsSynced(saleIds)
    }

    suspend fun markCustomersAsSynced(ids: List<Int>) {
        if (ids.isNotEmpty()) dao.markCustomersAsSynced(ids)
    }

    suspend fun markCustomerTransactionsAsSynced(ids: List<Int>) {
        if (ids.isNotEmpty()) dao.markCustomerTransactionsAsSynced(ids)
    }

    suspend fun markPurchaseInvoicesAsSynced(ids: List<Int>) {
        if (ids.isNotEmpty()) dao.markPurchaseInvoicesAsSynced(ids)
    }

    suspend fun markSyncQueueItemsSynced(ids: List<Int>) {
        if (ids.isNotEmpty()) dao.markSyncQueueItemsSynced(ids)
    }

    suspend fun markSyncQueueByEntityTypeSynced(entityType: String) {
        dao.markSyncQueueByEntityTypeSynced(entityType)
    }

    suspend fun markSyncQueueItemsFailed(ids: List<Int>, errorMsg: String) {
        if (ids.isNotEmpty()) dao.markSyncQueueItemsFailed(ids, errorMsg)
    }

    suspend fun markAllTablesAndQueueAsSynced() {
        dao.markAllTablesAndQueueAsSyncedAtomically()
    }

    suspend fun retryFailedQueueItems() {
        dao.retryFailedQueueItems()
    }

    suspend fun clearSyncedQueueItems() {
        dao.clearSyncedQueueItems()
    }

    // Offline-First Local Product Operations (with Atomic Sync Queue tracking)
    suspend fun saveProduct(product: Product): Long =
        dao.saveProductOfflineAtomically(product)

    suspend fun saveProductsBatch(products: List<Product>, enqueueForSync: Boolean = true) {
        if (products.isEmpty()) return
        val now = System.currentTimeMillis()
        val stamped = if (enqueueForSync) {
            products.map { it.copy(updatedAt = now, syncStatus = RecordSyncState.PENDING) }
        } else {
            products
        }
        dao.insertProducts(stamped)
        if (enqueueForSync) {
            val queueItems = stamped.map { p ->
                SyncQueueItem(
                    entityType = "PRODUCT",
                    entityId = p.id,
                    operation = "UPDATE",
                    titleAr = "تحديث مخزون: ${p.name}",
                    detailsAr = "الكمية الحالية: ${p.stock} ق",
                    status = RecordSyncState.PENDING,
                    createdAt = now,
                    updatedAt = now
                )
            }
            dao.insertSyncQueueItems(queueItems)
        }
    }

    suspend fun softDeleteProduct(productId: Int) =
        dao.softDeleteProductOfflineAtomically(productId)

    suspend fun findProductByBarcode(barcode: String): Product? {
        val clean = barcode.trim()
        if (clean.isEmpty()) return null
        dao.getProductByBarcode(clean)?.let { return it }
        return dao.getAllProductsOnce().firstOrNull { !it.isDeleted && it.matchesAnyBarcode(clean) }
    }

    // Offline-First Local Sales Operations
    suspend fun saveSale(sale: Sale): Long {
        val now = System.currentTimeMillis()
        val stamped = sale.copy(
            updatedAt = now,
            syncStatus = RecordSyncState.PENDING
        )
        val id = dao.insertSale(stamped)
        dao.insertSyncQueueItem(
            SyncQueueItem(
                entityType = "SALE",
                entityId = id.toInt(),
                operation = "CREATE",
                titleAr = "عملية بيع: ${sale.productName}",
                detailsAr = "الكمية: ${sale.quantitySold} • الإجمالي: ${String.format(Locale.US, "%.2f", sale.totalPrice)}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun saveSalesBatch(sales: List<Sale>, enqueueForSync: Boolean = false) {
        if (sales.isEmpty()) return
        dao.insertSales(sales)
        if (enqueueForSync) {
            val now = System.currentTimeMillis()
            dao.insertSyncQueueItems(
                sales.map { s ->
                    SyncQueueItem(
                        entityType = "SALE",
                        entityId = s.saleId,
                        operation = "CREATE",
                        titleAr = "عملية بيع: ${s.productName}",
                        detailsAr = "الكمية: ${s.quantitySold} • الإجمالي: ${String.format(Locale.US, "%.2f", s.totalPrice)}",
                        status = RecordSyncState.PENDING,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            )
        }
    }

    suspend fun saveSalesBatchWithIds(sales: List<Sale>): List<Long> {
        if (sales.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val invoiceCode = sales.first().invoiceGroupId.ifBlank {
            "INV-${(sales.first().saleDate / 1000).toString().takeLast(6)}"
        }
        val stamped = sales.map {
            it.copy(
                invoiceGroupId = invoiceCode,
                updatedAt = now,
                syncStatus = RecordSyncState.PENDING
            )
        }
        val insertedIds = dao.insertSalesWithIds(stamped)
        val totalSum = stamped.sumOf { it.totalPrice }
        val totalUnits = stamped.sumOf { it.quantitySold }
        dao.insertSyncQueueItem(
            SyncQueueItem(
                entityType = "SALE",
                entityId = insertedIds.firstOrNull()?.toInt() ?: 0,
                operation = "CHECKOUT",
                titleAr = "فاتورة مبيعات $invoiceCode (${stamped.size} أصناف)",
                detailsAr = "إجمالي الوحدات: $totalUnits ق • القيمة: ${String.format(Locale.US, "%.2f", totalSum)}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        return insertedIds
    }

    suspend fun recordPosCheckoutAtomically(
        salesToInsert: List<Sale>,
        productsToUpdate: List<Product>,
        updatedCustomer: Customer? = null,
        debtTransaction: CustomerTransaction? = null
    ): List<Long> = dao.recordPosCheckoutOfflineAtomically(
        salesToInsert = salesToInsert,
        productsToUpdate = productsToUpdate,
        updatedCustomer = updatedCustomer,
        debtTransaction = debtTransaction
    )

    suspend fun deleteSaleAndRestoreStock(sale: Sale) {
        dao.deleteSaleAndRestoreStockAtomically(sale)
    }

    // Offline-First Customer & Debt Operations
    suspend fun saveCustomer(customer: Customer): Long {
        val now = System.currentTimeMillis()
        val isNew = customer.id == 0
        val stamped = customer.copy(
            updatedAt = now,
            syncStatus = RecordSyncState.PENDING
        )
        val id = dao.insertCustomer(stamped)
        val resolvedId = if (customer.id > 0) customer.id else id.toInt()
        dao.insertSyncQueueItem(
            SyncQueueItem(
                entityType = "CUSTOMER",
                entityId = resolvedId,
                operation = if (isNew) "CREATE" else "UPDATE",
                titleAr = if (isNew) "إضافة عميل: ${customer.name}" else "تحديث حساب عميل: ${customer.name}",
                detailsAr = "المديونية: ${String.format(Locale.US, "%.2f", customer.balanceDebt)}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun saveCustomersBatch(customers: List<Customer>) = dao.insertCustomers(customers)
    suspend fun deleteCustomer(customer: Customer) = dao.deleteCustomer(customer)
    suspend fun updateAllCustomersLoyaltyRules(goal: Int, rewardValue: Double) = dao.updateAllCustomersLoyaltyRules(goal, rewardValue)

    suspend fun recordCustomerPayment(customer: Customer, amount: Double, notes: String) {
        val now = System.currentTimeMillis()
        val updatedDebt = (customer.balanceDebt - amount).coerceAtLeast(0.0)
        dao.updateCustomer(
            customer.copy(
                balanceDebt = updatedDebt,
                updatedAt = now,
                syncStatus = RecordSyncState.PENDING
            )
        )
        val txId = dao.insertCustomerTransaction(
            CustomerTransaction(
                customerId = customer.id,
                customerName = customer.name,
                type = "PAYMENT",
                amount = amount,
                notes = notes.ifBlank { "تسديد دفعة نقدية" },
                date = now,
                updatedAt = now,
                syncStatus = RecordSyncState.PENDING
            )
        )
        dao.insertSyncQueueItem(
            SyncQueueItem(
                entityType = "CUSTOMER_TX",
                entityId = txId.toInt(),
                operation = "CREATE",
                titleAr = "تحصيل دفعة من: ${customer.name}",
                detailsAr = "المبلغ: ${String.format(Locale.US, "%.2f", amount)} • المتبقي: ${String.format(Locale.US, "%.2f", updatedDebt)}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun recordCustomerDebt(customer: Customer, amount: Double, notes: String) {
        val now = System.currentTimeMillis()
        val updatedDebt = customer.balanceDebt + amount
        dao.updateCustomer(
            customer.copy(
                balanceDebt = updatedDebt,
                updatedAt = now,
                syncStatus = RecordSyncState.PENDING
            )
        )
        val txId = dao.insertCustomerTransaction(
            CustomerTransaction(
                customerId = customer.id,
                customerName = customer.name,
                type = "DEBT",
                amount = amount,
                notes = notes.ifBlank { "مديونية مبيعات آجلة" },
                date = now,
                updatedAt = now,
                syncStatus = RecordSyncState.PENDING
            )
        )
        dao.insertSyncQueueItem(
            SyncQueueItem(
                entityType = "CUSTOMER_TX",
                entityId = txId.toInt(),
                operation = "CREATE",
                titleAr = "تسجيل مديونية: ${customer.name}",
                detailsAr = "المبلغ المضاف: ${String.format(Locale.US, "%.2f", amount)} • إجمالي الدين: ${String.format(Locale.US, "%.2f", updatedDebt)}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun saveCustomerTransactionsBatch(list: List<CustomerTransaction>) =
        dao.insertCustomerTransactions(list)

    // Offline-First Purchase Invoices
    suspend fun recordPurchaseInvoice(
        supplierName: String,
        supplierPhone: String,
        paidAmount: Double,
        notes: String,
        items: List<PurchaseInvoiceItem>,
        customInvoiceNumber: String = ""
    ): PurchaseInvoice {
        val now = System.currentTimeMillis()
        val totalCost = items.sumOf { it.subtotal }
        val invoiceNumber = customInvoiceNumber.trim().ifBlank {
            "PUR-${(now / 1000).toString().takeLast(5)}"
        }
        // Update product stock and purchase prices
        items.forEach { item ->
            val addedUnits = if (item.isCarton) item.quantity * item.unitsPerCarton.coerceAtLeast(1) else item.quantity
            val existing = item.productId?.let { dao.getProductById(it) }
                ?: if (item.barcode.isNotBlank()) findProductByBarcode(item.barcode) else null

            val unitPurchaseCost = if (item.isCarton && item.unitsPerCarton > 1) {
                item.unitCost / item.unitsPerCarton
            } else {
                item.unitCost
            }

            if (existing != null) {
                dao.updateProduct(
                    existing.copy(
                        stock = existing.stock + addedUnits,
                        purchasePrice = if (unitPurchaseCost > 0) unitPurchaseCost else existing.purchasePrice,
                        price = if (item.sellingPrice > 0) item.sellingPrice else existing.price,
                        barcode = Product.mergeBarcodeStrings(existing.barcode, item.barcode),
                        cartonBarcode = Product.mergeBarcodeStrings(existing.cartonBarcode, item.cartonBarcode),
                        imagePath = if (item.imagePath.isNotBlank()) item.imagePath else existing.imagePath,
                        isDeleted = false,
                        updatedAt = now,
                        syncStatus = RecordSyncState.PENDING
                    )
                )
            } else {
                dao.insertProduct(
                    Product(
                        name = item.productName,
                        purchasePrice = unitPurchaseCost,
                        price = item.sellingPrice.coerceAtLeast(unitPurchaseCost),
                        stock = addedUnits,
                        unitsPerCarton = item.unitsPerCarton.coerceAtLeast(1),
                        barcode = item.barcode,
                        cartonBarcode = item.cartonBarcode,
                        imagePath = item.imagePath,
                        updatedAt = now,
                        syncStatus = RecordSyncState.PENDING
                    )
                )
            }
        }
        val invoice = PurchaseInvoice(
            invoiceNumber = invoiceNumber,
            supplierName = supplierName.ifBlank { "مورد عام" },
            supplierPhone = supplierPhone,
            dateMillis = now,
            totalCost = totalCost,
            paidAmount = paidAmount,
            remainingDebt = (totalCost - paidAmount).coerceAtLeast(0.0),
            notes = notes,
            itemsJson = PurchaseInvoice.encodeItems(items),
            itemsCount = items.size,
            updatedAt = now,
            syncStatus = RecordSyncState.PENDING
        )
        val id = dao.insertPurchaseInvoice(invoice).toInt()
        dao.insertSyncQueueItem(
            SyncQueueItem(
                entityType = "PURCHASE_INVOICE",
                entityId = id,
                operation = "CREATE",
                titleAr = "فاتورة مشتريات $invoiceNumber (${invoice.supplierName})",
                detailsAr = "${items.size} أصناف • الإجمالي: ${String.format(Locale.US, "%.2f", totalCost)}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        return invoice.copy(id = id)
    }

    suspend fun savePurchaseInvoice(invoice: PurchaseInvoice): Long {
        val now = System.currentTimeMillis()
        val stamped = invoice.copy(
            updatedAt = now,
            syncStatus = RecordSyncState.PENDING
        )
        val id = dao.insertPurchaseInvoice(stamped)
        dao.insertSyncQueueItem(
            SyncQueueItem(
                entityType = "PURCHASE_INVOICE",
                entityId = if (invoice.id > 0) invoice.id else id.toInt(),
                operation = "UPDATE",
                titleAr = "تحديث فاتورة مشتريات ${invoice.invoiceNumber}",
                detailsAr = "المدفوع: ${String.format(Locale.US, "%.2f", invoice.paidAmount)} • المتبقي: ${String.format(Locale.US, "%.2f", invoice.remainingDebt)}",
                status = RecordSyncState.PENDING,
                createdAt = now,
                updatedAt = now
            )
        )
        return id
    }

    suspend fun savePurchaseInvoicesBatch(invoices: List<PurchaseInvoice>) =
        dao.insertPurchaseInvoices(invoices)

    suspend fun saveUser(user: User): Long = dao.insertUser(user)
    suspend fun saveUsersBatch(users: List<User>) = dao.insertUsers(users)
    suspend fun deleteUser(user: User) = dao.deleteUser(user)

    // =========================================================================
    // Smart Conflict Resolution (Newest Timestamp Wins + Cumulative Sales Merge)
    // =========================================================================

    /**
     * Resolves product conflicts between local Room and imported records using `updatedAt`:
     * - If a local product has `syncStatus == PENDING` and its `updatedAt >= remote.updatedAt`,
     *   the local offline edit is preserved so it won't be overwritten.
     * - If the incoming product has a newer `updatedAt` (or doesn't exist locally), it is merged
     *   into Room with `syncStatus = SYNCED` while preserving any local `imagePath`.
     */
    suspend fun updateRemoteInvoiceStatusIfChanged(
        invoiceGroupId: String,
        remoteStatus: String,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        if (invoiceGroupId.isBlank() || remoteStatus.isBlank()) return
        dao.updateSalesStatusByInvoiceGroup(invoiceGroupId, remoteStatus, updatedAt, RecordSyncState.SYNCED)
    }

    suspend fun updateRemoteInvoiceStatusAndNotesIfChanged(
        invoiceGroupId: String,
        remoteStatus: String,
        remoteNotes: String,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        if (invoiceGroupId.isBlank() || remoteStatus.isBlank()) return
        dao.updateSalesStatusAndNotesByInvoiceGroup(invoiceGroupId, remoteStatus, remoteNotes, updatedAt, RecordSyncState.SYNCED)
    }

    suspend fun updateInvoiceStatusAndNotes(
        invoice: ArchivedInvoice,
        newStatus: String,
        newNotes: String,
        syncStatus: String = RecordSyncState.PENDING
    ) {
        val now = System.currentTimeMillis()
        val saleIds = invoice.items.mapNotNull { it.saleId.takeIf { id -> id > 0 } }
        if (saleIds.isNotEmpty()) {
            dao.updateSalesStatusAndNotesByIds(saleIds, newStatus, newNotes, now, syncStatus)
        }
        if (invoice.invoiceId.isNotBlank()) {
            dao.updateSalesStatusAndNotesByInvoiceGroup(invoice.invoiceId, newStatus, newNotes, now, syncStatus)
        }
    }

    suspend fun updateProductFirestoreIdAndSyncStatus(
        productId: Int,
        firestoreId: String,
        status: String = RecordSyncState.SYNCED
    ) {
        if (productId > 0 && firestoreId.isNotBlank()) {
            dao.updateProductFirestoreIdAndSyncStatus(productId, firestoreId, status)
        }
    }

    suspend fun mergeRemoteProductsWithTimestampResolution(remoteProducts: List<Product>): Int {
        if (remoteProducts.isEmpty()) return 0
        val allLocal = dao.getAllProductsOnce()
        val localById = allLocal.associateBy { it.id }.toMutableMap()
        val localByFirestoreId = allLocal.filter { it.firestoreId.isNotBlank() }.associateBy { it.firestoreId }.toMutableMap()
        val localByBarcode = mutableMapOf<String, Product>()
        val localByCartonBarcode = mutableMapOf<String, Product>()
        allLocal.forEach { prod ->
            prod.unitBarcodesList.forEach { code -> localByBarcode[code.lowercase()] = prod }
            prod.cartonBarcodesList.forEach { code -> localByCartonBarcode[code.lowercase()] = prod }
        }
        val localByName = allLocal.filter { it.name.isNotBlank() }.associateBy { it.name.trim().lowercase() }.toMutableMap()
        val toSave = mutableListOf<Product>()

        for (remote in remoteProducts) {
            val remoteFsId = remote.firestoreId.trim()
            val remoteUnitCodes = remote.unitBarcodesList.map { it.lowercase() }
            val remoteCartonCodes = remote.cartonBarcodesList.map { it.lowercase() }
            val remoteName = remote.name.trim().lowercase()

            // Try matching existing local product by firestoreId -> any unit barcode -> any cartonBarcode -> name -> (id if same name or partial stock update)
            val local = (if (remoteFsId.isNotBlank()) localByFirestoreId[remoteFsId] else null)
                ?: remoteUnitCodes.firstNotNullOfOrNull { localByBarcode[it] }
                ?: remoteCartonCodes.firstNotNullOfOrNull { localByCartonBarcode[it] }
                ?: (if (remoteName.isNotBlank()) localByName[remoteName] else null)
                ?: run {
                    // Check if remoteFsId is "pid_<id>" or remote.id > 0
                    val pidFromFsId = if (remoteFsId.startsWith("pid_")) remoteFsId.removePrefix("pid_").toIntOrNull() else null
                    val candidateId = if (remote.id > 0) remote.id else (pidFromFsId ?: 0)
                    val candidateLocal = if (candidateId > 0) localById[candidateId] else null
                    if (candidateLocal != null && (remoteName.isBlank() || candidateLocal.name.trim().equals(remote.name.trim(), ignoreCase = true))) {
                        candidateLocal
                    } else {
                        null
                    }
                }

            if (local == null) {
                // Only insert a brand-new product if it actually has a valid name (ignore orphan stock-only partial docs)
                if (remote.name.isNotBlank() && !remote.isDeleted) {
                    val assignedId = if (remote.id > 0 && !localById.containsKey(remote.id)) remote.id else 0
                    val computedFsId = remoteFsId.ifBlank {
                        when {
                            remote.primaryBarcode.isNotBlank() -> "bc_${remote.primaryBarcode.trim().replace(Regex("[^a-zA-Z0-9]"), "_")}"
                            assignedId > 0 -> "pid_$assignedId"
                            else -> "p_${remote.name.trim().hashCode().toUInt()}"
                        }
                    }
                    val newProduct = remote.copy(
                        id = assignedId,
                        firestoreId = computedFsId,
                        updatedAt = if (remote.updatedAt > 0L) remote.updatedAt else System.currentTimeMillis(),
                        syncStatus = RecordSyncState.SYNCED
                    )
                    toSave.add(newProduct)
                }
            } else {
                val isSeedSample = local.firestoreId.isBlank() && local.barcode.startsWith("6281000001")
                val isLocalPendingNewer = !isSeedSample &&
                    local.syncStatus != RecordSyncState.SYNCED &&
                    local.updatedAt > 1000L &&
                    remote.updatedAt > 0L &&
                    local.updatedAt > remote.updatedAt
                if (!isLocalPendingNewer) {
                    val resolvedUpdatedAt = maxOf(remote.updatedAt, local.updatedAt, 1001L)
                    // If remote is a partial stock-only document (name is blank), only update stock/isDeleted/firestoreId on local
                    val mergedProduct = if (remote.name.isBlank()) {
                        local.copy(
                            stock = remote.stock,
                            firestoreId = remoteFsId.ifBlank { local.firestoreId },
                            isDeleted = remote.isDeleted || local.isDeleted,
                            updatedAt = resolvedUpdatedAt,
                            syncStatus = RecordSyncState.SYNCED
                        )
                    } else {
                        remote.copy(
                            id = local.id,
                            name = remote.name.ifBlank { local.name },
                            price = if (remote.price > 0.0) remote.price else local.price,
                            purchasePrice = if (remote.purchasePrice > 0.0) remote.purchasePrice else local.purchasePrice,
                            unitsPerCarton = if (remote.unitsPerCarton > 0) remote.unitsPerCarton else local.unitsPerCarton,
                            barcode = remote.barcode.ifBlank { local.barcode },
                            cartonBarcode = remote.cartonBarcode.ifBlank { local.cartonBarcode },
                            category = remote.category.ifBlank { local.category },
                            firestoreId = remoteFsId.ifBlank { local.firestoreId },
                            imageUrl = remote.imageUrl.ifBlank { local.imageUrl },
                            imagePath = remote.imagePath.ifBlank { local.imagePath },
                            updatedAt = resolvedUpdatedAt,
                            syncStatus = RecordSyncState.SYNCED
                        )
                    }
                    toSave.add(mergedProduct)
                }
            }
        }
        if (toSave.isNotEmpty()) {
            dao.insertProducts(toSave)
        }
        return toSave.size
    }

    /**
     * Merges remote sales cumulatively with local sales so no offline sales are ever lost:
     * - Local sales with `syncStatus == PENDING` are preserved untouched.
     * - Remote sales that don't conflict with a pending local sale ID are inserted/updated as `SYNCED`.
     * - If a remote sale has the same `saleId` as a different local offline sale (different `saleDate` + `productId`),
     *   the remote sale is still merged with a new auto-generated local ID if needed so both sales are kept.
     */
    suspend fun mergeRemoteSalesCumulatively(remoteSales: List<Sale>): Int {
        if (remoteSales.isEmpty()) return 0
        val localSales = dao.getAllSalesOnce()
        val localById = localSales.associateBy { it.saleId }
        val existingSignatures = localSales.map { "${it.saleDate}_${it.productId}_${it.quantitySold}_${it.totalPrice}" }.toHashSet()
        val toInsert = mutableListOf<Sale>()

        for (remote in remoteSales) {
            val sig = "${remote.saleDate}_${remote.productId}_${remote.quantitySold}_${remote.totalPrice}"
            val localWithSameId = localById[remote.saleId]

            if (localWithSameId == null) {
                if (sig !in existingSignatures) {
                    toInsert.add(remote.copy(syncStatus = RecordSyncState.SYNCED))
                    existingSignatures.add(sig)
                }
            } else {
                val sameTransaction = localWithSameId.saleDate == remote.saleDate &&
                    localWithSameId.productId == remote.productId
                if (sameTransaction) {
                    // Already exists locally; ensure it's marked SYNCED if remote has it
                    if (localWithSameId.syncStatus != RecordSyncState.SYNCED) {
                        toInsert.add(localWithSameId.copy(syncStatus = RecordSyncState.SYNCED))
                    }
                } else if (sig !in existingSignatures) {
                    // Different sale happened offline with same auto-increment ID on another device:
                    // keep local sale untouched and insert remote sale with a fresh local saleId (0)
                    toInsert.add(remote.copy(saleId = 0, syncStatus = RecordSyncState.SYNCED))
                    existingSignatures.add(sig)
                }
            }
        }

        if (toInsert.isNotEmpty()) {
            dao.insertSales(toInsert)
        }
        return toInsert.size
    }

    suspend fun mergeRemoteCustomersWithTimestampResolution(remoteCustomers: List<Customer>): Int {
        if (remoteCustomers.isEmpty()) return 0
        val localById = dao.getAllCustomersOnce().associateBy { it.id }
        val toSave = mutableListOf<Customer>()
        for (remote in remoteCustomers) {
            val local = localById[remote.id]
            if (local == null) {
                toSave.add(remote.copy(syncStatus = RecordSyncState.SYNCED))
            } else {
                val isLocalPendingNewer = local.syncStatus != RecordSyncState.SYNCED && local.updatedAt > remote.updatedAt
                if (!isLocalPendingNewer) {
                    toSave.add(remote.copy(syncStatus = RecordSyncState.SYNCED))
                }
            }
        }
        if (toSave.isNotEmpty()) {
            dao.insertCustomers(toSave)
        }
        return toSave.size
    }

    suspend fun mergeRemoteCustomerTransactionsCumulatively(remoteTxs: List<CustomerTransaction>): Int {
        if (remoteTxs.isEmpty()) return 0
        val localTxs = dao.getAllCustomerTransactionsOnce()
        val existingSigs = localTxs.map { "${it.customerId}_${it.date}_${it.type}_${it.amount}" }.toHashSet()
        val localById = localTxs.associateBy { it.id }
        val toSave = mutableListOf<CustomerTransaction>()

        for (remote in remoteTxs) {
            val sig = "${remote.customerId}_${remote.date}_${remote.type}_${remote.amount}"
            val local = localById[remote.id]
            if (local == null) {
                if (sig !in existingSigs) {
                    toSave.add(remote.copy(syncStatus = RecordSyncState.SYNCED))
                    existingSigs.add(sig)
                }
            } else if (local.date == remote.date && local.customerId == remote.customerId) {
                if (local.syncStatus != RecordSyncState.SYNCED) {
                    toSave.add(local.copy(syncStatus = RecordSyncState.SYNCED))
                }
            } else if (sig !in existingSigs) {
                toSave.add(remote.copy(id = 0, syncStatus = RecordSyncState.SYNCED))
                existingSigs.add(sig)
            }
        }
        if (toSave.isNotEmpty()) {
            dao.insertCustomerTransactions(toSave)
        }
        return toSave.size
    }

    suspend fun mergeRemotePurchaseInvoicesWithTimestampResolution(remoteInvoices: List<PurchaseInvoice>): Int {
        if (remoteInvoices.isEmpty()) return 0
        val allLocal = dao.getAllPurchaseInvoicesOnce()
        val localById = allLocal.associateBy { it.id }
        val localByNumber = allLocal.filter { it.invoiceNumber.isNotBlank() }.associateBy { it.invoiceNumber }
        val toSave = mutableListOf<PurchaseInvoice>()
        for (remote in remoteInvoices) {
            val local = (if (remote.invoiceNumber.isNotBlank()) localByNumber[remote.invoiceNumber] else null)
                ?: (if (remote.id > 0) localById[remote.id] else null)
            if (local == null) {
                toSave.add(remote.copy(syncStatus = RecordSyncState.SYNCED))
            } else {
                val isLocalPendingNewer = local.syncStatus != RecordSyncState.SYNCED && local.updatedAt > remote.updatedAt
                if (!isLocalPendingNewer) {
                    toSave.add(remote.copy(id = local.id, syncStatus = RecordSyncState.SYNCED))
                }
            }
        }
        if (toSave.isNotEmpty()) {
            dao.insertPurchaseInvoices(toSave)
        }
        return toSave.size
    }

    suspend fun logActivity(
        user: User?,
        actionType: String,
        description: String,
        metadata: String = ""
    ) {
        dao.insertActivityLog(
            ActivityLog(
                userId = user?.id ?: 0,
                username = user?.username ?: "admin",
                userRole = user?.role ?: "manager",
                managerId = user?.managerId,
                actionType = actionType,
                description = description,
                metadata = metadata
            )
        )
    }

    suspend fun saveActivityLogsBatch(logs: List<ActivityLog>) = dao.insertActivityLogs(logs)

    suspend fun clearAllTablesForRestore() {
        dao.clearAllProducts()
        dao.clearAllSales()
        dao.clearAllCustomers()
        dao.clearAllCustomerTransactions()
        dao.clearAllPurchaseInvoices()
        dao.clearAllSyncQueue()
    }

    suspend fun updateWorkerLocation(location: WorkerLocation) {
        dao.insertOrUpdateWorkerLocation(location)
    }

    suspend fun getWorkerLocation(userId: Int): WorkerLocation? {
        return dao.getWorkerLocationById(userId)
    }

    suspend fun insertWorkerRouteCoordinate(coordinate: WorkerRouteCoordinate) {
        dao.insertWorkerRouteCoordinate(coordinate)
    }

    fun getWorkerRouteCoordinatesForDay(userId: Int, dayMillis: Long): Flow<List<WorkerRouteCoordinate>> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = cal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return dao.getWorkerRouteCoordinatesForDay(userId, startTime, endTime)
    }

    suspend fun deleteOldWorkerRouteCoordinates(thresholdTime: Long) {
        dao.deleteOldWorkerRouteCoordinates(thresholdTime)
    }
}

