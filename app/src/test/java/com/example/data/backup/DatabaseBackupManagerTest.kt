package com.example.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AntarDatabase
import com.example.data.models.Customer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DatabaseBackupManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testEncryptionAndDecryptionRoundtrip() {
        val sampleSqliteData = "SQLite format 3\u0000SomeSampleDatabaseDataPayload123456789".toByteArray(Charsets.UTF_8)
        val encryptedEnvelope = DatabaseBackupManager.encryptDatabaseBytes(sampleSqliteData)

        assertNotNull(encryptedEnvelope)
        assertTrue(encryptedEnvelope.size > sampleSqliteData.size)

        val decryptedData = DatabaseBackupManager.decryptDatabaseBytes(encryptedEnvelope)
        assertTrue(sampleSqliteData.contentEquals(decryptedData))
    }

    @Test(expected = SecurityException::class)
    fun testTamperedEnvelopeThrowsSecurityException() {
        val sampleSqliteData = "SQLite format 3\u0000OriginalSecretDatabaseBytes".toByteArray(Charsets.UTF_8)
        val encryptedEnvelope = DatabaseBackupManager.encryptDatabaseBytes(sampleSqliteData)

        // Corrupt one byte in the encrypted payload
        val tamperedEnvelope = encryptedEnvelope.clone()
        tamperedEnvelope[tamperedEnvelope.size - 5] = (tamperedEnvelope[tamperedEnvelope.size - 5] + 1).toByte()

        DatabaseBackupManager.decryptDatabaseBytes(tamperedEnvelope)
    }

    @Test
    fun testRealDatabaseBackupAndRestore() {
        runBlocking {
            val db = AntarDatabase.getDatabase(context)
            val dao = db.storeDao()
            dao.insertCustomer(Customer(id = 999, name = "Backup Test Client", phone = "0555123456"))

            val backupResult = DatabaseBackupManager.createBackup(context, isAuto = false)
            assertTrue(backupResult.isSuccess)

            val metadata = backupResult.getOrNull()
            assertNotNull(metadata)
            assertTrue(metadata!!.file.exists())
            assertTrue(metadata.sizeBytes > 0)

            val availableBackups = DatabaseBackupManager.getAvailableBackups(context)
            assertTrue(availableBackups.isNotEmpty())
            assertTrue(availableBackups.any { it.fileName == metadata.fileName })

            // Test restore
            val restoreResult = DatabaseBackupManager.restoreBackup(context, metadata.file)
            assertTrue(restoreResult.isSuccess)

            // Cleanup
            metadata.file.delete()
        }
    }
}
