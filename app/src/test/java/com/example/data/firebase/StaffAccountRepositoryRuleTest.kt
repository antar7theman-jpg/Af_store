package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.models.WorkerPermissions
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StaffAccountRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun registerPendingAccountRequest_validWorker_createsPendingDocument() = runBlocking {
        val uniqueEmail = "worker_${UUID.randomUUID().toString().take(8)}@afstore.test"
        val uid = signInTestUser(uniqueEmail)
        val repository = StaffAccountRepository(firestore, auth)

        val regResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerPendingAccountRequest(
                fullName = "أحمد موظف المبيعات",
                phone = "0550112233",
                requestedRole = "worker"
            )
        }
        assertTrue("Expected registration to succeed: ${regResult.exceptionOrNull()}", regResult.isSuccess)

        val fetchedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getMyStaffAccountOnce()
        }
        assertTrue(fetchedResult.isSuccess)
        val account = fetchedResult.getOrNull()
        assertNotNull(account)
        assertEquals(uid, account?.userId)
        assertEquals(StaffApprovalStatus.PENDING, account?.approvalStatus)
        assertEquals("worker", account?.role)
        assertEquals("worker", account?.requestedRole)
    }

    @Test
    fun observeMyStaffAccount_authenticatedWorker_emitsAccountAndUpdates() = runBlocking {
        val uniqueEmail = "cashier_${UUID.randomUUID().toString().take(8)}@afstore.test"
        val uid = signInTestUser(uniqueEmail)
        val repository = StaffAccountRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerPendingAccountRequest(
                fullName = "سليم الكاشير",
                phone = "0661223344",
                requestedRole = "worker"
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMyStaffAccount(uid).first { it != null }
        }
        assertNotNull(emitted)
        assertEquals("سليم الكاشير", emitted?.fullName)
        assertEquals("worker", emitted?.requestedRole)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateOwnProfileDetails(
                fullName = "سليم بن علي",
                phone = "0770998877",
                requestedRole = "worker"
            )
        }
        assertTrue(updateResult.isSuccess)
    }

    @Test
    fun ownerSecretCodeVerification_andManualManagerCreation_enforcesOwnerSecret() = runBlocking {
        assertTrue(
            StaffAccountRepository.verifyOwnerCredentials(
                "Antar7theman@gmail.com",
                "Antar19930716"
            )
        )
        assertFalse(
            StaffAccountRepository.verifyOwnerCredentials(
                "Antar7theman@gmail.com",
                "wrong_secret"
            )
        )

        val ownerAccount = StaffAccount.createVerifiedOwnerAccount()
        assertEquals("DASHBOARD", ownerAccount.defaultDestinationTabRoute)

        auth.signOut()
        val repository = StaffAccountRepository(firestore, auth)

        // Creating a manager with wrong Owner secret code fails immediately
        val badSecretResult = repository.createManagerAccountByOwner(
            managerFullName = "مدير جديد",
            managerEmail = "newmgr@afstore.com",
            managerPhone = "0550998877",
            ownerSecretCode = "wrong_code",
            permissions = WorkerPermissions()
        )
        assertTrue(badSecretResult.isFailure)

        // Creating a manager with valid Owner secret code (Antar19930716) succeeds
        val validSecretResult = repository.createManagerAccountByOwner(
            managerFullName = "مدير معتمد من المالك",
            managerEmail = "newmgr@afstore.com",
            managerPhone = "0550998877",
            ownerSecretCode = "Antar19930716",
            permissions = WorkerPermissions(canViewReports = true, canEditPrices = true)
        )
        assertTrue(validSecretResult.isSuccess)
        val createdManager = validSecretResult.getOrNull()
        assertNotNull(createdManager)
        assertEquals("manager", createdManager?.role)
        assertEquals(StaffApprovalStatus.APPROVED, createdManager?.approvalStatus)
        assertEquals("Deffas antar", createdManager?.approvedByName)
        assertEquals("INVENTORY", createdManager?.defaultDestinationTabRoute)

        val workerAccount = StaffAccount(
            userId = "worker_1",
            email = "worker@afstore.com",
            fullName = "عامل مبيعات",
            role = "worker",
            approvalStatus = StaffApprovalStatus.APPROVED
        )
        assertEquals("POS", workerAccount.defaultDestinationTabRoute)

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()

        // Independent Manager profile registration & immediate activation with Owner secret code
        val regMgrResult = StaffAccountRepository.registerIndependentManagerFromLogin(
            context = context,
            fullName = "مدير فرع وهران",
            email = "oran_mgr@afstore.com",
            phone = "0555112233",
            branchName = "فرع وهران",
            managerSecretPin = "5566",
            optionalOwnerSecretCode = "Antar19930716"
        )
        assertTrue(regMgrResult.isSuccess)
        val independentMgr = regMgrResult.getOrThrow()
        assertEquals("manager", independentMgr.role)
        assertEquals(StaffApprovalStatus.APPROVED, independentMgr.approvalStatus)
        assertEquals("فرع وهران", independentMgr.branchName)

        // Independent Manager creates an affiliated Worker account
        val createWorkerResult = repository.createWorkerAccountByManager(
            managerAccount = independentMgr,
            workerFullName = "ياسين كاشير وهران",
            workerEmail = "yassine_oran@afstore.com",
            workerPhone = "0661445566",
            workerSecretPin = "7788",
            permissions = WorkerPermissions(canMakeSales = true, canManageInventory = false)
        )
        assertTrue(createWorkerResult.isSuccess)
        val affiliatedWorker = createWorkerResult.getOrThrow()
        assertEquals("worker", affiliatedWorker.role)
        assertEquals(independentMgr.userId, affiliatedWorker.parentManagerUid)
        assertEquals("مدير فرع وهران", affiliatedWorker.parentManagerName)
        assertTrue(affiliatedWorker.isAffiliatedWithManager(independentMgr))
        StaffAccountRepository.saveAccountToLocalRegistry(context, affiliatedWorker)

        // Direct login works for both the Independent Manager and the Affiliated Worker
        val mgrLogin = StaffAccountRepository.authenticateDirectStaffLogin(context, "oran_mgr@afstore.com", "5566")
        assertTrue(mgrLogin.isSuccess)
        assertEquals("INVENTORY", mgrLogin.getOrThrow().defaultDestinationTabRoute)

        val wrkLogin = StaffAccountRepository.authenticateDirectStaffLogin(context, "yassine_oran@afstore.com", "7788")
        assertTrue(wrkLogin.isSuccess)
        assertEquals("POS", wrkLogin.getOrThrow().defaultDestinationTabRoute)

        StaffAccountRepository.setKeepSessionOpenPreferred(context, true)
        StaffAccountRepository.savePersistentStaffSession(context, ownerAccount, isDirectOwnerSession = true)
        val loadedOwner = StaffAccountRepository.loadPersistentStaffSession(context)
        assertNotNull(loadedOwner)
        assertEquals(StaffAccountRepository.OWNER_DISPLAY_EMAIL, loadedOwner?.email)
        assertTrue(loadedOwner?.isOwner == true)
        assertEquals("DASHBOARD", loadedOwner?.defaultDestinationTabRoute)

        StaffAccountRepository.clearPersistentStaffSession(context)
        assertEquals(null, StaffAccountRepository.loadPersistentStaffSession(context))
    }

    @Test
    fun getOtherUserStaffAccount_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = "alice_${UUID.randomUUID().toString().take(8)}@afstore.test"
        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = StaffAccountRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.registerPendingAccountRequest(
                fullName = "أليس",
                phone = "0551000001",
                requestedRole = "worker"
            ).getOrThrow()
        }

        val bobEmail = "bob_${UUID.randomUUID().toString().take(8)}@afstore.test"
        signInTestUser(bobEmail)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(StaffAccountRepository.COLLECTION_STAFF)
                    .document(aliceUid)
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's staff account")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeAllStoreStaffAccounts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = StaffAccountRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeAllStoreStaffAccounts().first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
