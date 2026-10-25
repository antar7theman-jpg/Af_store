package com.example.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.AntarRepository
import com.example.data.database.AntarDatabase
import com.example.data.firebase.LiveInventorySyncManager
import com.example.data.models.Product
import com.example.data.models.RecordSyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IncrementalSyncWorkerTest : FirestoreEmulatorTestBase() {

    private lateinit var context: Context
    private lateinit var database: AntarDatabase
    private lateinit var repository: AntarRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        try {
            val config = androidx.work.Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
            androidx.work.WorkManager.initialize(context, config)
        } catch (_: Exception) {}
        database = AntarDatabase.getDatabase(context)
        repository = AntarRepository(database.storeDao())
    }

    @Test
    fun testSchedulePeriodicAndImmediateWork() {
        IncrementalSyncWorker.schedulePeriodic(context, intervalMinutes = 15)
        val workManager = WorkManager.getInstance(context)
        val periodicInfos = workManager.getWorkInfosForUniqueWork(IncrementalSyncWorker.WORK_NAME_PERIODIC).get()
        assertNotNull(periodicInfos)
        assertTrue(periodicInfos.isNotEmpty())

        IncrementalSyncWorker.enqueueImmediate(context)
        val immediateInfos = workManager.getWorkInfosForUniqueWork(IncrementalSyncWorker.WORK_NAME_IMMEDIATE).get()
        assertNotNull(immediateInfos)
        assertTrue(immediateInfos.isNotEmpty())
    }

    @Test
    fun testPersistentSyncTimestampsAndSummary() {
        val prefs = context.getSharedPreferences(IncrementalSyncWorker.PREFS_SYNC, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val summary = "تمت المزامنة التزايدية بنجاح ✓"

        prefs.edit()
            .putLong(IncrementalSyncWorker.KEY_LAST_SYNC_TIMESTAMP, now)
            .putString(IncrementalSyncWorker.KEY_LAST_SYNC_MESSAGE, summary)
            .putBoolean(IncrementalSyncWorker.KEY_LAST_SYNC_SUCCESS, true)
            .apply()

        assertEquals(now, IncrementalSyncWorker.getLastSyncTimestamp(context))
        assertEquals(summary, IncrementalSyncWorker.getLastSyncSummary(context))
    }

    @Test
    fun testPerformIncrementalSyncWithLocalPendingItems() = runBlocking {
        // Insert a pending product
        val pendingProd = Product(
            name = "سلعة اختبار المزامنة التزايدية",
            purchasePrice = 10.0,
            price = 15.0,
            stock = 25,
            barcode = "test_inc_bc_${System.currentTimeMillis()}",
            syncStatus = RecordSyncState.PENDING
        )
        val prodId = repository.saveProduct(pendingProd).toInt()
        assertTrue(prodId > 0)

        val pendingBefore = repository.getPendingProductsOnce()
        assertTrue(pendingBefore.any { it.id == prodId })

        // Execute incremental sync directly
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val syncManager = LiveInventorySyncManager(context, scope)
        val result = syncManager.performIncrementalSync(activeAccount = null)

        assertNotNull(result)
        assertTrue(result.message.isNotBlank())
        assertTrue(syncManager.lastSyncTimestamp.value > 0L)
    }

    @Test
    fun testSyncIntervalModePersistenceAndScheduling() {
        // Test enum mapping
        assertEquals(com.example.utils.SyncIntervalMode.REAL_TIME, com.example.utils.SyncIntervalMode.fromId("real_time"))
        assertEquals(com.example.utils.SyncIntervalMode.HOURLY, com.example.utils.SyncIntervalMode.fromId("hourly"))
        assertEquals(com.example.utils.SyncIntervalMode.MANUAL, com.example.utils.SyncIntervalMode.fromId("manual"))
        assertEquals(com.example.utils.SyncIntervalMode.REAL_TIME, com.example.utils.SyncIntervalMode.fromId("unknown_mode"))

        val offlineSyncManager = com.example.utils.OfflineSyncManager(
            context = context,
            repository = repository
        )

        // Switch to HOURLY
        offlineSyncManager.setSyncIntervalMode(com.example.utils.SyncIntervalMode.HOURLY)
        assertEquals(com.example.utils.SyncIntervalMode.HOURLY, offlineSyncManager.syncIntervalMode.value)
        assertEquals(false, offlineSyncManager.isAutoSyncEnabled.value)

        val prefs = context.getSharedPreferences("af_store_sync_prefs", Context.MODE_PRIVATE)
        assertEquals("hourly", prefs.getString("sync_interval_mode", ""))

        // Switch to MANUAL
        offlineSyncManager.setSyncIntervalMode(com.example.utils.SyncIntervalMode.MANUAL)
        assertEquals(com.example.utils.SyncIntervalMode.MANUAL, offlineSyncManager.syncIntervalMode.value)
        assertEquals(false, offlineSyncManager.isAutoSyncEnabled.value)
        assertEquals("manual", prefs.getString("sync_interval_mode", ""))

        // Switch to REAL_TIME
        offlineSyncManager.setSyncIntervalMode(com.example.utils.SyncIntervalMode.REAL_TIME)
        assertEquals(com.example.utils.SyncIntervalMode.REAL_TIME, offlineSyncManager.syncIntervalMode.value)
        assertEquals(true, offlineSyncManager.isAutoSyncEnabled.value)
        assertEquals("real_time", prefs.getString("sync_interval_mode", ""))
    }
}
