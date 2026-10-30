package com.example.data.firebase

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Background synchronization service that monitors differences between the local database
 * and Firebase Firestore, prioritizing server-side authorization status and enforcing
 * single-device session integrity (preventing concurrent multi-device logins).
 */
class BackgroundStaffSyncService(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val repository = StaffAccountRepository(context.applicationContext)
    private var syncJob: Job? = null

    sealed class SyncEvent {
        data class ConcurrentLoginDetected(val newDeviceName: String) : SyncEvent()
        data class ServerAuthorizationChanged(val updatedAccount: StaffAccount, val reason: String) : SyncEvent()
        data class AccountDeactivated(val reason: String) : SyncEvent()
        data class Synchronized(val account: StaffAccount) : SyncEvent()
    }

    fun startSync(
        activeAccount: StaffAccount,
        onEvent: (SyncEvent) -> Unit
    ) {
        syncJob?.cancel()
        val localDeviceId = StaffAccountRepository.getOrCreateDeviceId(context)

        syncJob = scope.launch(Dispatchers.IO) {
            repository.observeStaffAccountStatusLive(activeAccount)
                .catch { e ->
                    Log.w("BackgroundStaffSync", "Error in sync observer: ${e.message}", e)
                }
                .collect { cloudAccount ->
                    if (cloudAccount == null) {
                        if (activeAccount.isOwner) {
                            // The owner document might still be bootstrapping; do not delete owner
                            return@collect
                        }
                        Log.w("BackgroundStaffSync", "Account document removed on server. Terminating session.")
                        StaffAccountRepository.clearPersistentStaffSession(context)
                        StaffAccountRepository.removeAccountFromLocalRegistry(context, activeAccount.userId, activeAccount.email)
                        onEvent(SyncEvent.AccountDeactivated("تم حذف الحساب من خادم المتجر."))
                        return@collect
                    }

                    // 1. Prioritize Server-Side Authorization: Check active account status & 7-day trial expiration
                    if (!activeAccount.isOwner && (cloudAccount.isTrialExpired || (!cloudAccount.isApproved && cloudAccount.isTrial))) {
                        Log.w("BackgroundStaffSync", "7-day trial expired for ${cloudAccount.fullName}. Terminating session automatically.")
                        StaffAccountRepository.recordTrialExpiredSession(context, cloudAccount.fullName)
                        StaffAccountRepository.clearPersistentStaffSession(context)
                        val suspendedTrial = cloudAccount.copy(
                            approvalStatus = StaffApprovalStatus.SUSPENDED,
                            isApprovedByOwner = false,
                            activeDeviceId = "",
                            activeDeviceName = "",
                            activeSessionToken = ""
                        )
                        StaffAccountRepository.saveAccountToLocalRegistry(context, suspendedTrial)
                        runCatching { repository.expireTrialAccountAutomatically(cloudAccount) }
                        onEvent(SyncEvent.AccountDeactivated("⛔ انتهت مدة التجربة (7 أيام) وتم إنهاء الجلسة تلقائياً. لا يمكن الدخول إلا بعد سماح المالك (Deffas antar)."))
                        return@collect
                    }

                    if (!cloudAccount.isApproved && !activeAccount.isOwner) {
                        Log.w("BackgroundStaffSync", "Server revoked approval (${cloudAccount.approvalStatus}). Terminating session.")
                        StaffAccountRepository.clearPersistentStaffSession(context)
                        StaffAccountRepository.saveAccountToLocalRegistry(context, cloudAccount)
                        onEvent(SyncEvent.AccountDeactivated("تم إيقاف أو تجميد الحساب من قبل إدارة المتجر."))
                        return@collect
                    }

                    // 2. Prevent Concurrent Multi-Device Logins: Single Device Enforcement
                    if (cloudAccount.activeDeviceId.isNotBlank() && cloudAccount.activeDeviceId != localDeviceId) {
                        val otherDevice = cloudAccount.activeDeviceName.ifBlank { "جهاز آخر" }
                        Log.w("BackgroundStaffSync", "Concurrent login detected on $otherDevice (${cloudAccount.activeDeviceId}). Local device is $localDeviceId.")
                        StaffAccountRepository.clearPersistentStaffSession(context)
                        StaffAccountRepository.recordTerminatedSession(context, otherDevice)
                        onEvent(SyncEvent.ConcurrentLoginDetected(otherDevice))
                        return@collect
                    }

                    // 3. Detect Discrepancies between Local Registry and Server
                    val mergedAccount = if (cloudAccount.secretPin.isBlank() && activeAccount.secretPin.isNotBlank()) {
                        cloudAccount.copy(secretPin = activeAccount.secretPin)
                    } else {
                        cloudAccount
                    }

                    val hasPermissionDiff = mergedAccount.role != activeAccount.role ||
                        mergedAccount.canMakeSales != activeAccount.canMakeSales ||
                        mergedAccount.canManageInventory != activeAccount.canManageInventory ||
                        mergedAccount.canEditPrices != activeAccount.canEditPrices ||
                        mergedAccount.canViewReports != activeAccount.canViewReports ||
                        mergedAccount.canManageDebtsAndCustomers != activeAccount.canManageDebtsAndCustomers ||
                        mergedAccount.canRefundOrVoid != activeAccount.canRefundOrVoid ||
                        mergedAccount.canApplyDiscounts != activeAccount.canApplyDiscounts ||
                        mergedAccount.canViewCostPrices != activeAccount.canViewCostPrices ||
                        mergedAccount.canExportData != activeAccount.canExportData ||
                        mergedAccount.canManagePurchaseInvoices != activeAccount.canManagePurchaseInvoices

                    // Server-side authorization ALWAYS takes precedence over local
                    StaffAccountRepository.saveAccountToLocalRegistry(context, mergedAccount)

                    if (hasPermissionDiff) {
                        Log.i("BackgroundStaffSync", "Server updated permissions for ${mergedAccount.fullName}. Applied to local registry.")
                        onEvent(SyncEvent.ServerAuthorizationChanged(mergedAccount, "تم تحديث صلاحيات الحساب من الخادم السحابي فوراً."))
                    } else {
                        onEvent(SyncEvent.Synchronized(mergedAccount))
                    }
                }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }
}
