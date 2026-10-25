package com.example.data.firebase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.models.WorkerPermissions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffAccountSyncAndDiagnosticsTest : FirestoreEmulatorTestBase() {

    @Test
    fun testOwnerAndDeviceSessionInitialization() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val deviceId = StaffAccountRepository.getOrCreateDeviceId(context)
        val deviceName = StaffAccountRepository.getDeviceModelName()
        assertNotNull(deviceId)
        assertTrue(deviceId.startsWith("dev_"))
        assertNotNull(deviceName)

        val ownerAcc = StaffAccount.createVerifiedOwnerAccount().copy(
            activeDeviceId = deviceId,
            activeDeviceName = deviceName,
            activeSessionToken = "token_owner_test_123"
        )
        assertEquals("Deffas antar", ownerAcc.fullName)
        assertTrue(ownerAcc.isOwner)
        assertEquals(deviceId, ownerAcc.activeDeviceId)
        assertEquals("token_owner_test_123", ownerAcc.activeSessionToken)
    }

    @Test
    fun testTerminatedSessionReasonPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Initially null
        val initialReason = StaffAccountRepository.getAndClearTerminatedSessionReason(context)
        // Record session termination
        StaffAccountRepository.recordTerminatedSession(context, "هاتف سامسونج جالاكسي")
        
        val retrievedReason = StaffAccountRepository.getAndClearTerminatedSessionReason(context)
        assertNotNull(retrievedReason)
        assertTrue(retrievedReason!!.contains("هاتف سامسونج جالاكسي"))

        // Cleared on second read
        val secondRead = StaffAccountRepository.getAndClearTerminatedSessionReason(context)
        assertEquals(null, secondRead)
    }

    @Test
    fun testIndependentManagerAndWorkerAccountSyncPayloads() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uniqueEmail = "mgr_sync_${System.currentTimeMillis()}@afstore.com"

        val regResult = StaffAccountRepository.registerIndependentManagerFromLogin(
            context = context,
            fullName = "مدير المزامنة والاختبار",
            email = uniqueEmail,
            phone = "0550991122",
            branchName = "فرع الاختبارات",
            managerSecretPin = "1234",
            optionalOwnerSecretCode = "Antar19930716"
        )
        assertTrue("Expected manager registration to succeed: ${regResult.exceptionOrNull()}", regResult.isSuccess)
        val manager = regResult.getOrThrow()

        // Verify JSON / Firestore payload roundtrip
        val json = manager.toJsonObject()
        assertNotNull(json)
        val parsedFromJSON = StaffAccount.fromJsonObject(json)
        assertEquals(manager.userId, parsedFromJSON.userId)
        assertEquals(manager.email, parsedFromJSON.email)
        assertEquals(manager.branchName, parsedFromJSON.branchName)
        assertEquals(manager.role, parsedFromJSON.role)

        val payload = manager.toFirestorePayload()
        assertNotNull(payload)
        assertEquals(manager.email, payload["email"])
        assertEquals(manager.branchName, payload["branchName"])
    }

    @Test
    fun testPersistentStaffSessionSaveAndLoad() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = StaffAccount(
            userId = "wrk_test_999",
            email = "worker_persist@afstore.com",
            fullName = "عامل الاختبارات",
            role = "worker",
            approvalStatus = StaffApprovalStatus.APPROVED,
            activeDeviceId = "dev_abc123",
            activeDeviceName = "هاتف اختبار",
            activeSessionToken = "sess_token_999"
        )

        StaffAccountRepository.savePersistentStaffSession(context, worker, isDirectOwnerSession = false)
        val loaded = StaffAccountRepository.loadPersistentStaffSession(context)
        assertNotNull(loaded)
        assertEquals("worker_persist@afstore.com", loaded?.email)
        assertEquals("عامل الاختبارات", loaded?.fullName)
        assertEquals("dev_abc123", loaded?.activeDeviceId)
        assertEquals("sess_token_999", loaded?.activeSessionToken)

        StaffAccountRepository.clearPersistentStaffSession(context)
        assertEquals(null, StaffAccountRepository.loadPersistentStaffSession(context))
    }
}
