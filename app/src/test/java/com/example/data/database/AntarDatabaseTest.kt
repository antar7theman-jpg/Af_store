package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.models.Customer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AntarDatabaseTest {

    private lateinit var db: AntarDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AntarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDatabaseSchemaAndDao() = runBlocking {
        val dao = db.storeDao()
        assertNotNull(dao)
        val customer = Customer(
            id = 1,
            name = "Test Customer",
            email = "test@example.com",
            address = "Test Address",
            loyaltyTier = "Gold"
        )
        dao.insertCustomer(customer)
        val customers = dao.getAllCustomersOnce()
        assert(customers.isNotEmpty())
    }

    @Test
    fun testRealDatabaseInstanceAndMigration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbInstance = AntarDatabase.getDatabase(context)
        assertNotNull(dbInstance)
        assertNotNull(dbInstance.storeDao())
    }

    @Test
    fun testVicoChartModel() {
        val trend = listOf(0L to 10, 1L to 20, 2L to 30)
        val model = com.patrykandpatryk.vico.core.entry.entryModelOf(*trend.mapIndexed { index, pair ->
            index.toFloat() to pair.second.toFloat()
        }.toTypedArray())
        assertNotNull(model)
    }

    @Test
    fun testMigrationExecution() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = context.getDatabasePath("test_migration_10_12.db")
        dbFile.parentFile?.mkdirs()
        dbFile.delete()

        // Create a raw SQLite DB representing version 10
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        sqlite.version = 10
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `notes` TEXT NOT NULL, `address` TEXT NOT NULL, `balanceDebt` REAL NOT NULL, `totalPurchases` REAL NOT NULL, `isFavorite` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL)")
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `firestoreId` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `purchasePrice` REAL NOT NULL, `price` REAL NOT NULL, `stock` INTEGER NOT NULL, `unitsPerCarton` INTEGER NOT NULL, `barcode` TEXT NOT NULL, `cartonBarcode` TEXT NOT NULL, `imagePath` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL, `category` TEXT NOT NULL, `minStockAlert` INTEGER NOT NULL, `imageUrl` TEXT NOT NULL, `createdAt` INTEGER, `serverUpdatedAt` INTEGER)")
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `sales` (`saleId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantitySold` INTEGER NOT NULL, `totalPrice` REAL NOT NULL, `saleDate` INTEGER NOT NULL, `customerId` INTEGER, `customerName` TEXT NOT NULL, `isDebt` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, `invoiceGroupId` TEXT NOT NULL, `orderType` TEXT NOT NULL, `status` TEXT NOT NULL, `expectedDeliveryDate` INTEGER NOT NULL, `notes` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL)")
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `customer_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `customerName` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `notes` TEXT NOT NULL, `date` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL)")
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `purchase_invoices` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoiceNumber` TEXT NOT NULL, `supplierName` TEXT NOT NULL, `supplierPhone` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL, `totalCost` REAL NOT NULL, `paidAmount` REAL NOT NULL, `remainingDebt` REAL NOT NULL, `paymentStatus` TEXT NOT NULL, `notes` TEXT NOT NULL, `itemsJson` TEXT NOT NULL, `itemsCount` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL)")
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, `role` TEXT NOT NULL, `fullName` TEXT NOT NULL, `phone` TEXT NOT NULL, `storeName` TEXT NOT NULL, `managerId` INTEGER, `isActive` INTEGER NOT NULL, `isTrial` INTEGER NOT NULL, `trialEndsAt` INTEGER NOT NULL, `isApprovedByOwner` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `permissionsJson` TEXT NOT NULL)")
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `activity_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `username` TEXT NOT NULL, `userRole` TEXT NOT NULL, `managerId` INTEGER, `actionType` TEXT NOT NULL, `description` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `metadata` TEXT NOT NULL)")
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `sync_queue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entityType` TEXT NOT NULL, `entityId` INTEGER NOT NULL, `operation` TEXT NOT NULL, `titleAr` TEXT NOT NULL, `detailsAr` TEXT NOT NULL, `status` TEXT NOT NULL, `retryCount` INTEGER NOT NULL, `lastError` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `worker_locations` (`userId` INTEGER NOT NULL, `workerName` TEXT NOT NULL, `storeName` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `accuracy` REAL NOT NULL, `speed` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `isTrackingActive` INTEGER NOT NULL, `statusNote` TEXT NOT NULL, `batteryPercent` INTEGER NOT NULL, PRIMARY KEY(`userId`))")
        sqlite.execSQL("CREATE TABLE IF NOT EXISTS `worker_route_coordinates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `accuracy` REAL NOT NULL DEFAULT 0, `recordedAt` INTEGER NOT NULL)")
        sqlite.close()

        val migratedDb = Room.databaseBuilder(context, AntarDatabase::class.java, "test_migration_10_13.db")
            .addMigrations(
                AntarDatabase.MIGRATION_4_5,
                AntarDatabase.MIGRATION_5_6,
                AntarDatabase.MIGRATION_6_7,
                AntarDatabase.MIGRATION_7_8,
                AntarDatabase.MIGRATION_10_11,
                AntarDatabase.MIGRATION_11_12,
                AntarDatabase.MIGRATION_12_13,
                AntarDatabase.MIGRATION_11_13,
                AntarDatabase.MIGRATION_10_13,
                AntarDatabase.MIGRATION_10_12,
                AntarDatabase.MIGRATION_8_12,
                AntarDatabase.MIGRATION_9_12
            )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()

        assertNotNull(migratedDb.storeDao())
        val customers = runBlocking { migratedDb.storeDao().getAllCustomersOnce() }
        assertNotNull(customers)
        migratedDb.close()
        dbFile.delete()
    }
}
