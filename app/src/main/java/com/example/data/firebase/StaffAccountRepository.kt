package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.R
import com.example.data.datastore.UserSessionDataStore
import com.example.data.models.User
import com.example.data.models.UserRole
import com.example.data.models.WorkerPermissions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Exception,
    operationType: OperationType,
    path: String?
): String {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

object StaffApprovalStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
    const val SUSPENDED = "SUSPENDED"

    fun labelAr(status: String): String = when (status.uppercase()) {
        APPROVED -> "معتمد ومفعّل ✓"
        REJECTED -> "مرفوض ✕"
        SUSPENDED -> "موقوف مؤقتاً ⏸"
        else -> "بانتظار موافقة الإدارة ⏳"
    }
}

@androidx.compose.runtime.Immutable
data class StaffAccount(
    val userId: String = "",
    val storeId: String = "af_store",
    val email: String = "",
    val fullName: String = "",
    val phone: String = "",
    val requestedRole: String = "worker",
    val role: String = "worker",
    val approvalStatus: String = StaffApprovalStatus.PENDING,
    val approvedByUid: String = "",
    val approvedByName: String = "",
    val parentManagerUid: String = "",
    val parentManagerName: String = "",
    val parentManagerEmail: String = "",
    val branchName: String = "",
    val secretPin: String = "",
    val canMakeSales: Boolean = true,
    val canManageInventory: Boolean = true,
    val canEditPrices: Boolean = false,
    val canViewReports: Boolean = false,
    val canManageDebtsAndCustomers: Boolean = true,
    val canRefundOrVoid: Boolean = false,
    val canApplyDiscounts: Boolean = true,
    val canViewCostPrices: Boolean = false,
    val canExportData: Boolean = false,
    val canManagePurchaseInvoices: Boolean = false,
    val activeDeviceId: String = "",
    val activeDeviceName: String = "",
    val activeSessionToken: String = "",
    val isTrial: Boolean = false,
    val trialEndsAtMillis: Long = 0L,
    val isApprovedByOwner: Boolean = true,
    val syncMode: String = "pro", // "pro", "offline", "lite"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isSyncEnabled: Boolean
        get() = syncMode == "pro"
    val isApproved: Boolean
        get() = approvalStatus == StaffApprovalStatus.APPROVED

    val isPending: Boolean
        get() = approvalStatus == StaffApprovalStatus.PENDING

    val isOwner: Boolean
        get() = isApproved && (
            role == "owner" ||
                role == "admin" ||
                StaffAccountRepository.isOwnerEmail(email)
            )

    val isRegularManager: Boolean
        get() = isApproved && role == "manager" && !isOwner

    val isManagerOrAdmin: Boolean
        get() = isApproved && (isOwner || role == "manager")

    val isTrialExpired: Boolean
        get() = isTrial && trialEndsAtMillis > 0 && System.currentTimeMillis() > trialEndsAtMillis && !isApprovedByOwner && !isOwner

    val canAccessProAdvancedFeatures: Boolean
        get() = isOwner || syncMode == "pro" || (isTrial && !isTrialExpired)

    val defaultDestinationTabRoute: String
        get() = when {
            isOwner -> "DASHBOARD"
            isRegularManager -> "INVENTORY"
            else -> "POS"
        }

    val defaultDestinationLabelAr: String
        get() = when {
            isOwner -> "لوحة التحكم الحية للمطور والمالك"
            isRegularManager -> "قسم الإدارة والمخزون"
            else -> "شاشة الكاشير ونقطة البيع"
        }

    val roleBadgeLabelAr: String
        get() = when {
            isOwner -> "المطور والمالك (Deffas antar)"
            isRegularManager -> if (branchName.isNotBlank()) "مدير مستقل ($branchName)" else "مدير مستقل معتمد"
            else -> if (parentManagerName.isNotBlank()) "عامل تابع لـ $parentManagerName" else "عامل مبيعات (كاشير)"
        }

    fun isAffiliatedWithManager(manager: StaffAccount): Boolean {
        if (role != "worker" || isOwner) return false
        if (parentManagerUid.isNotBlank() && parentManagerUid == manager.userId) return true
        if (parentManagerEmail.isNotBlank() && manager.email.isNotBlank() &&
            parentManagerEmail.equals(manager.email, ignoreCase = true)
        ) {
            return true
        }
        if (approvedByUid.isNotBlank() && approvedByUid == manager.userId) return true
        return false
    }

    fun toWorkerPermissions(): WorkerPermissions {
        return if (isOwner) {
            WorkerPermissions(
                canMakeSales = true,
                canManageInventory = true,
                canEditPrices = true,
                canViewReports = true,
                canManageDebtsAndCustomers = true,
                canRefundOrVoid = true,
                canApplyDiscounts = true,
                canViewCostPrices = true,
                canExportData = true,
                canManagePurchaseInvoices = true
            )
        } else {
            WorkerPermissions(
                canMakeSales = canMakeSales,
                canManageInventory = canManageInventory,
                canEditPrices = canEditPrices,
                canViewReports = canViewReports,
                canManageDebtsAndCustomers = canManageDebtsAndCustomers,
                canRefundOrVoid = canRefundOrVoid,
                canApplyDiscounts = canApplyDiscounts,
                canViewCostPrices = canViewCostPrices,
                canExportData = canExportData,
                canManagePurchaseInvoices = canManagePurchaseInvoices
            )
        }
    }

    fun toLocalRoomUser(localId: Int = 0, resolvedManagerId: Int? = null): User {
        val mappedRole = when {
            isOwner -> UserRole.SUPER_ADMIN
            role.lowercase() == "manager" -> UserRole.MANAGER
            else -> UserRole.WORKER
        }
        return User(
            id = if (isOwner && localId == 0) 1 else localId,
            username = if (isOwner) StaffAccountRepository.OWNER_DISPLAY_EMAIL else email.ifBlank { userId.take(8) },
            passwordHash = when {
                isOwner -> StaffAccountRepository.OWNER_SECRET_CODE
                secretPin.isNotBlank() -> secretPin
                else -> "firebase_google_auth"
            },
            role = mappedRole,
            fullName = if (isOwner) StaffAccountRepository.OWNER_FULL_NAME else fullName.ifBlank { email.substringBefore("@") },
            phone = phone,
            storeName = branchName.ifBlank { "AF store" },
            managerId = resolvedManagerId,
            isActive = isApproved || (isTrial && System.currentTimeMillis() <= trialEndsAtMillis),
            isTrial = isTrial,
            trialEndsAt = trialEndsAtMillis,
            isApprovedByOwner = isApprovedByOwner || isApproved,
            status = if (isApprovedByOwner || isApproved) "ACTIVE" else if (isTrial && System.currentTimeMillis() <= trialEndsAtMillis) "TRIAL_ACTIVE" else "TRIAL_EXPIRED",
            permissionsJson = toWorkerPermissions().toJsonString()
        )
    }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("userId", userId)
        put("storeId", storeId)
        put("email", email)
        put("fullName", fullName)
        put("phone", phone)
        put("requestedRole", requestedRole)
        put("role", role)
        put("approvalStatus", approvalStatus)
        put("approvedByUid", approvedByUid)
        put("approvedByName", approvedByName)
        put("parentManagerUid", parentManagerUid)
        put("parentManagerName", parentManagerName)
        put("parentManagerEmail", parentManagerEmail)
        put("branchName", branchName)
        put("secretPin", secretPin)
        put("canMakeSales", canMakeSales)
        put("canManageInventory", canManageInventory)
        put("canEditPrices", canEditPrices)
        put("canViewReports", canViewReports)
        put("canManageDebtsAndCustomers", canManageDebtsAndCustomers)
        put("canRefundOrVoid", canRefundOrVoid)
        put("canApplyDiscounts", canApplyDiscounts)
        put("canViewCostPrices", canViewCostPrices)
        put("canExportData", canExportData)
        put("canManagePurchaseInvoices", canManagePurchaseInvoices)
        put("activeDeviceId", activeDeviceId)
        put("activeDeviceName", activeDeviceName)
        put("activeSessionToken", activeSessionToken)
        put("isTrial", isTrial)
        put("trialEndsAtMillis", trialEndsAtMillis)
        put("isApprovedByOwner", isApprovedByOwner)
        put("syncMode", syncMode)
    }

    fun toFirestorePayload(): Map<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "userId" to userId,
            "storeId" to storeId,
            "email" to email,
            "fullName" to fullName,
            "phone" to phone,
            "requestedRole" to requestedRole,
            "role" to role,
            "approvalStatus" to approvalStatus,
            "approvedByUid" to approvedByUid,
            "approvedByName" to approvedByName,
            "canMakeSales" to canMakeSales,
            "canManageInventory" to canManageInventory,
            "canEditPrices" to canEditPrices,
            "canViewReports" to canViewReports,
            "canManageDebtsAndCustomers" to canManageDebtsAndCustomers,
            "canRefundOrVoid" to canRefundOrVoid,
            "canApplyDiscounts" to canApplyDiscounts,
            "canViewCostPrices" to canViewCostPrices,
            "canExportData" to canExportData,
            "canManagePurchaseInvoices" to canManagePurchaseInvoices,
            "isTrial" to isTrial,
            "trialEndsAtMillis" to trialEndsAtMillis,
            "isApprovedByOwner" to isApprovedByOwner,
            "syncMode" to syncMode,
            "createdAt" to (createdAt ?: Timestamp.now()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (parentManagerUid.isNotBlank()) payload["parentManagerUid"] = parentManagerUid
        if (parentManagerName.isNotBlank()) payload["parentManagerName"] = parentManagerName
        if (parentManagerEmail.isNotBlank()) payload["parentManagerEmail"] = parentManagerEmail
        if (branchName.isNotBlank()) payload["branchName"] = branchName
        if (secretPin.isNotBlank()) payload["secretPin"] = secretPin
        if (activeDeviceId.isNotBlank()) payload["activeDeviceId"] = activeDeviceId
        if (activeDeviceName.isNotBlank()) payload["activeDeviceName"] = activeDeviceName
        if (activeSessionToken.isNotBlank()) payload["activeSessionToken"] = activeSessionToken
        return payload
    }

    companion object {
        fun createVerifiedOwnerAccount(uidOverride: String = "owner_deffas_antar"): StaffAccount {
            return StaffAccount(
                userId = uidOverride,
                storeId = StaffAccountRepository.DEFAULT_STORE_ID,
                email = StaffAccountRepository.OWNER_DISPLAY_EMAIL,
                fullName = StaffAccountRepository.OWNER_FULL_NAME,
                phone = "0662931569",
                requestedRole = "owner",
                role = "owner",
                approvalStatus = StaffApprovalStatus.APPROVED,
                approvedByUid = uidOverride,
                approvedByName = StaffAccountRepository.OWNER_FULL_NAME,
                branchName = "الإدارة العامة - AF store",
                secretPin = StaffAccountRepository.OWNER_SECRET_CODE,
                canMakeSales = true,
                canManageInventory = true,
                canEditPrices = true,
                canViewReports = true,
                canManageDebtsAndCustomers = true,
                canRefundOrVoid = true,
                canApplyDiscounts = true,
                canViewCostPrices = true,
                canExportData = true,
                canManagePurchaseInvoices = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }

        fun fromJsonObject(obj: JSONObject): StaffAccount {
            return StaffAccount(
                userId = obj.optString("userId", ""),
                storeId = obj.optString("storeId", "af_store"),
                email = obj.optString("email", ""),
                fullName = obj.optString("fullName", ""),
                phone = obj.optString("phone", ""),
                requestedRole = obj.optString("requestedRole", "worker"),
                role = obj.optString("role", "worker"),
                approvalStatus = obj.optString("approvalStatus", StaffApprovalStatus.PENDING),
                approvedByUid = obj.optString("approvedByUid", ""),
                approvedByName = obj.optString("approvedByName", ""),
                parentManagerUid = obj.optString("parentManagerUid", ""),
                parentManagerName = obj.optString("parentManagerName", ""),
                parentManagerEmail = obj.optString("parentManagerEmail", ""),
                branchName = obj.optString("branchName", ""),
                secretPin = obj.optString("secretPin", ""),
                canMakeSales = obj.optBoolean("canMakeSales", true),
                canManageInventory = obj.optBoolean("canManageInventory", true),
                canEditPrices = obj.optBoolean("canEditPrices", false),
                canViewReports = obj.optBoolean("canViewReports", false),
                canManageDebtsAndCustomers = obj.optBoolean("canManageDebtsAndCustomers", true),
                canRefundOrVoid = obj.optBoolean("canRefundOrVoid", false),
                canApplyDiscounts = obj.optBoolean("canApplyDiscounts", true),
                canViewCostPrices = obj.optBoolean("canViewCostPrices", false),
                canExportData = obj.optBoolean("canExportData", false),
                canManagePurchaseInvoices = obj.optBoolean("canManagePurchaseInvoices", false),
                activeDeviceId = obj.optString("activeDeviceId", ""),
                activeDeviceName = obj.optString("activeDeviceName", ""),
                activeSessionToken = obj.optString("activeSessionToken", ""),
                isTrial = obj.optBoolean("isTrial", false),
                trialEndsAtMillis = obj.optLong("trialEndsAtMillis", 0L),
                isApprovedByOwner = obj.optBoolean("isApprovedByOwner", true),
                syncMode = obj.optString("syncMode", "pro"),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }

        fun fromSnapshot(doc: DocumentSnapshot): StaffAccount? {
            if (!doc.exists()) return null
            val estimate = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            return StaffAccount(
                userId = doc.getString("userId") ?: doc.id,
                storeId = doc.getString("storeId") ?: "af_store",
                email = doc.getString("email") ?: "",
                fullName = doc.getString("fullName") ?: "",
                phone = doc.getString("phone") ?: "",
                requestedRole = doc.getString("requestedRole") ?: "worker",
                role = doc.getString("role") ?: "worker",
                approvalStatus = doc.getString("approvalStatus") ?: StaffApprovalStatus.PENDING,
                approvedByUid = doc.getString("approvedByUid") ?: "",
                approvedByName = doc.getString("approvedByName") ?: "",
                parentManagerUid = doc.getString("parentManagerUid") ?: "",
                parentManagerName = doc.getString("parentManagerName") ?: "",
                parentManagerEmail = doc.getString("parentManagerEmail") ?: "",
                branchName = doc.getString("branchName") ?: "",
                secretPin = doc.getString("secretPin") ?: "",
                canMakeSales = doc.getBoolean("canMakeSales") ?: true,
                canManageInventory = doc.getBoolean("canManageInventory") ?: true,
                canEditPrices = doc.getBoolean("canEditPrices") ?: false,
                canViewReports = doc.getBoolean("canViewReports") ?: false,
                canManageDebtsAndCustomers = doc.getBoolean("canManageDebtsAndCustomers") ?: true,
                canRefundOrVoid = doc.getBoolean("canRefundOrVoid") ?: false,
                canApplyDiscounts = doc.getBoolean("canApplyDiscounts") ?: true,
                canViewCostPrices = doc.getBoolean("canViewCostPrices") ?: false,
                canExportData = doc.getBoolean("canExportData") ?: false,
                canManagePurchaseInvoices = doc.getBoolean("canManagePurchaseInvoices") ?: false,
                activeDeviceId = doc.getString("activeDeviceId") ?: "",
                activeDeviceName = doc.getString("activeDeviceName") ?: "",
                activeSessionToken = doc.getString("activeSessionToken") ?: "",
                isTrial = doc.getBoolean("isTrial") ?: false,
                trialEndsAtMillis = doc.getLong("trialEndsAtMillis") ?: 0L,
                isApprovedByOwner = doc.getBoolean("isApprovedByOwner") ?: true,
                syncMode = doc.getString("syncMode") ?: "pro",
                createdAt = doc.getTimestamp("createdAt", estimate),
                updatedAt = doc.getTimestamp("updatedAt", estimate)
            )
        }
    }
}

data class AdminRecord(
    val userId: String = "",
    val storeId: String = "af_store",
    val email: String = "",
    val fullName: String = "",
    val createdAt: Timestamp? = null
)

@androidx.compose.runtime.Immutable
data class ProUpgradeRequest(
    val requestId: String = "",
    val managerUid: String = "",
    val managerName: String = "",
    val managerEmail: String = "",
    val phone: String = "",
    val branchName: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val note: String = "",
    val requestedAt: Timestamp? = null,
    val reviewedAt: Timestamp? = null,
    val reviewedByUid: String = ""
) {
    val isPending: Boolean get() = status == "PENDING"
    val isApproved: Boolean get() = status == "APPROVED"
    val isRejected: Boolean get() = status == "REJECTED"

    fun toFirestorePayload(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "requestId" to requestId,
            "managerUid" to managerUid,
            "managerName" to managerName,
            "managerEmail" to managerEmail,
            "phone" to phone,
            "branchName" to branchName,
            "status" to status,
            "note" to note,
            "requestedAt" to (requestedAt ?: Timestamp.now())
        )
        if (reviewedAt != null) map["reviewedAt"] = reviewedAt
        if (reviewedByUid.isNotBlank()) map["reviewedByUid"] = reviewedByUid
        return map
    }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): ProUpgradeRequest? {
            if (!doc.exists()) return null
            val estimate = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            return ProUpgradeRequest(
                requestId = doc.getString("requestId") ?: doc.id,
                managerUid = doc.getString("managerUid") ?: "",
                managerName = doc.getString("managerName") ?: "",
                managerEmail = doc.getString("managerEmail") ?: "",
                phone = doc.getString("phone") ?: "",
                branchName = doc.getString("branchName") ?: "",
                status = doc.getString("status") ?: "PENDING",
                note = doc.getString("note") ?: "",
                requestedAt = doc.getTimestamp("requestedAt", estimate),
                reviewedAt = doc.getTimestamp("reviewedAt", estimate),
                reviewedByUid = doc.getString("reviewedByUid") ?: ""
            )
        }
    }
}

class StaffAccountRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private var appContext: Context? = null

    constructor(context: Context) : this(
        db = getFirestoreDb(context),
        auth = getFirebaseAuth(context)
    ) {
        appContext = context.applicationContext
    }

    companion object {
        fun getFirestoreDb(context: Context? = null): FirebaseFirestore {
            com.example.AFStoreApplication.initializeFirebase(context)
            val ctx = context?.applicationContext ?: com.example.AFStoreApplication.instance
            val dbId = ctx?.getString(R.string.firestore_database_id)
                ?: "ai-studio-android-deffas-a1f696be-decc-4255-ad09-94436552be74"
            return FirebaseFirestore.getInstance(dbId)
        }

        fun getFirebaseAuth(context: Context? = null): FirebaseAuth {
            com.example.AFStoreApplication.initializeFirebase(context)
            return try {
                Firebase.auth
            } catch (e: Exception) {
                FirebaseAuth.getInstance()
            }
        }

        const val COLLECTION_STAFF = "staff_accounts"
        const val COLLECTION_ADMINS = "admins"
        const val COLLECTION_PRO_UPGRADE_REQUESTS = "pro_upgrade_requests"
        const val DEFAULT_STORE_ID = "af_store"
        private const val PREFS_AUTH_SESSION = "af_store_auth_session_prefs"
        private const val KEY_LOCAL_STAFF_REGISTRY = "local_staff_accounts_registry_json"

        // Owner & Developer Identity Constants (Deffas antar - Antar7theman@gmail.com)
        const val OWNER_EMAIL = "antar7theman@gmail.com"
        const val OWNER_DISPLAY_EMAIL = "Antar7theman@gmail.com"
        const val OWNER_FULL_NAME = "Deffas antar"
        const val OWNER_SECRET_CODE = "Antar19930716"

        fun isOwnerEmail(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            val normalized = email.trim().lowercase()
            return normalized == OWNER_EMAIL ||
                normalized == OWNER_FULL_NAME.lowercase() ||
                normalized == "antar7theman" ||
                normalized == "deffas antar" ||
                normalized == "antar" ||
                normalized == "deffas" ||
                normalized == "المطور" ||
                normalized == "المالك"
        }

        fun isOwnerIdentity(emailOrUsername: String?): Boolean {
            return isOwnerEmail(emailOrUsername)
        }

        fun verifyOwnerSecretCode(inputCode: String): Boolean {
            return inputCode.trim() == OWNER_SECRET_CODE
        }

        fun verifyOwnerCredentials(emailOrUsername: String, secretCode: String): Boolean {
            return isOwnerIdentity(emailOrUsername) && verifyOwnerSecretCode(secretCode)
        }

        fun buildDeterministicSlug(input: String): String {
            val trimmed = input.trim().lowercase()
            val ascii = trimmed.replace(Regex("[^a-z0-9]"), "_")
            val nonUnderscore = ascii.replace("_", "")
            return if (nonUnderscore.length >= 2) {
                ascii.take(100)
            } else {
                val hashHex = Integer.toUnsignedString(trimmed.hashCode(), 16)
                "u_${hashHex}"
            }
        }

        fun normalizeStaffInternalEmail(nameOrEmail: String, rolePrefix: String = "mgr"): String {
            val trimmed = nameOrEmail.trim().lowercase()
            if (trimmed.contains("@") && trimmed.length >= 5) {
                return trimmed.take(150)
            }
            val slug = buildDeterministicSlug(trimmed).trim('_')
            return "${rolePrefix}_${slug}@afstore.app".take(150)
        }

        fun managerPreProvisionDocId(nameOrEmail: String): String {
            val slug = buildDeterministicSlug(nameOrEmail)
            return "mgr_${slug.take(100)}"
        }

        fun workerPreProvisionDocId(nameOrEmail: String): String {
            val slug = buildDeterministicSlug(nameOrEmail)
            return "wrk_${slug.take(100)}"
        }

        private tailrec fun Context.findActivity(): Activity? = when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }

        /**
         * Ensures the device has an active Firebase Firestore connection for Name + PIN cloud sync
         * without triggering any Google Sign-In / CredentialManager account picker UI.
         */
        suspend fun ensureFirebaseCloudChannelConnected(context: Context): Boolean {
            return try {
                com.example.AFStoreApplication.initializeFirebase(context.applicationContext)
                true
            } catch (_: Exception) {
                false
            }
        }

        fun isKeepSessionOpenPreferred(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            return prefs.getBoolean("keep_session_open_pref", true)
        }

        fun setKeepSessionOpenPreferred(context: Context, keepOpen: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            prefs.edit().putBoolean("keep_session_open_pref", keepOpen).apply()
            if (!keepOpen) {
                clearPersistentStaffSession(context)
            }
        }

        fun savePersistentStaffSession(
            context: Context,
            account: StaffAccount,
            isDirectOwnerSession: Boolean = false,
            savedIdentifier: String = "",
            savedSecret: String = ""
        ) {
            saveAccountToLocalRegistry(context, account)
            val dataStore = UserSessionDataStore(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    dataStore.saveUserSession(
                        identifier = savedIdentifier.ifBlank { account.email.ifBlank { account.fullName } },
                        secretPin = savedSecret.ifBlank { account.secretPin },
                        account = account,
                        rememberMe = isKeepSessionOpenPreferred(context),
                        isDirectOwner = isDirectOwnerSession || account.isOwner,
                        sessionToken = account.activeSessionToken
                    )
                }
            }
            if (!isKeepSessionOpenPreferred(context)) return
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("session_active", account.isApproved)
                .putBoolean("is_direct_owner_session", isDirectOwnerSession)
                .putString("userId", account.userId)
                .putString("storeId", account.storeId)
                .putString("email", account.email)
                .putString("fullName", account.fullName)
                .putString("phone", account.phone)
                .putString("requestedRole", account.requestedRole)
                .putString("role", account.role)
                .putString("approvalStatus", account.approvalStatus)
                .putString("approvedByUid", account.approvedByUid)
                .putString("approvedByName", account.approvedByName)
                .putString("parentManagerUid", account.parentManagerUid)
                .putString("parentManagerName", account.parentManagerName)
                .putString("parentManagerEmail", account.parentManagerEmail)
                .putString("branchName", account.branchName)
                .putString("secretPin", account.secretPin)
                .putBoolean("canMakeSales", account.canMakeSales)
                .putBoolean("canManageInventory", account.canManageInventory)
                .putBoolean("canEditPrices", account.canEditPrices)
                .putBoolean("canViewReports", account.canViewReports)
                .putBoolean("canManageDebtsAndCustomers", account.canManageDebtsAndCustomers)
                .putBoolean("canRefundOrVoid", account.canRefundOrVoid)
                .putBoolean("canApplyDiscounts", account.canApplyDiscounts)
                .putBoolean("canViewCostPrices", account.canViewCostPrices)
                .putBoolean("canExportData", account.canExportData)
                .putBoolean("canManagePurchaseInvoices", account.canManagePurchaseInvoices)
                .putString("activeDeviceId", account.activeDeviceId)
                .putString("activeDeviceName", account.activeDeviceName)
                .putString("activeSessionToken", account.activeSessionToken)
                .putString("syncMode", account.syncMode)
                .apply()
        }

        fun loadPersistentStaffSession(context: Context): StaffAccount? {
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            if (!prefs.getBoolean("keep_session_open_pref", true)) return null
            if (!prefs.getBoolean("session_active", false)) return null
            val userId = prefs.getString("userId", "").orEmpty()
            val email = prefs.getString("email", "").orEmpty()
            if (userId.isBlank() && email.isBlank()) return null

            return StaffAccount(
                userId = userId.ifBlank { "saved_session_user" },
                storeId = prefs.getString("storeId", DEFAULT_STORE_ID) ?: DEFAULT_STORE_ID,
                email = email,
                fullName = prefs.getString("fullName", "") ?: "",
                phone = prefs.getString("phone", "") ?: "",
                requestedRole = prefs.getString("requestedRole", "worker") ?: "worker",
                role = prefs.getString("role", "worker") ?: "worker",
                approvalStatus = prefs.getString("approvalStatus", StaffApprovalStatus.APPROVED)
                    ?: StaffApprovalStatus.APPROVED,
                approvedByUid = prefs.getString("approvedByUid", "") ?: "",
                approvedByName = prefs.getString("approvedByName", "") ?: "",
                parentManagerUid = prefs.getString("parentManagerUid", "") ?: "",
                parentManagerName = prefs.getString("parentManagerName", "") ?: "",
                parentManagerEmail = prefs.getString("parentManagerEmail", "") ?: "",
                branchName = prefs.getString("branchName", "") ?: "",
                secretPin = prefs.getString("secretPin", "") ?: "",
                canMakeSales = prefs.getBoolean("canMakeSales", true),
                canManageInventory = prefs.getBoolean("canManageInventory", true),
                canEditPrices = prefs.getBoolean("canEditPrices", false),
                canViewReports = prefs.getBoolean("canViewReports", false),
                canManageDebtsAndCustomers = prefs.getBoolean("canManageDebtsAndCustomers", true),
                canRefundOrVoid = prefs.getBoolean("canRefundOrVoid", false),
                canApplyDiscounts = prefs.getBoolean("canApplyDiscounts", true),
                canViewCostPrices = prefs.getBoolean("canViewCostPrices", false),
                canExportData = prefs.getBoolean("canExportData", false),
                canManagePurchaseInvoices = prefs.getBoolean("canManagePurchaseInvoices", false),
                activeDeviceId = prefs.getString("activeDeviceId", "") ?: "",
                activeDeviceName = prefs.getString("activeDeviceName", "") ?: "",
                activeSessionToken = prefs.getString("activeSessionToken", "") ?: "",
                syncMode = prefs.getString("syncMode", "pro") ?: "pro",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }

        private const val KEY_TERMINATED_SESSION_REASON = "key_terminated_session_reason"

        fun recordTerminatedSession(context: Context, newDeviceName: String) {
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            val msg = "تم إنهاء جلستك تلقائياً لأن الحساب فُتح في جهاز آخر (${newDeviceName.ifBlank { "جهاز آخر" }}). يُسمح بجهاز نشط واحد فقط في نفس الوقت."
            prefs.edit().putString(KEY_TERMINATED_SESSION_REASON, msg).apply()
        }

        fun getAndClearTerminatedSessionReason(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            val reason = prefs.getString(KEY_TERMINATED_SESSION_REASON, null)
            if (!reason.isNullOrBlank()) {
                prefs.edit().remove(KEY_TERMINATED_SESSION_REASON).apply()
            }
            return reason
        }

        fun getOrCreateDeviceId(context: Context): String {
            val prefs = context.applicationContext.getSharedPreferences("af_device_session_prefs", Context.MODE_PRIVATE)
            var deviceId = prefs.getString("local_device_id", null)
            if (deviceId.isNullOrBlank()) {
                deviceId = "dev_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
                prefs.edit().putString("local_device_id", deviceId).apply()
            }
            return deviceId
        }

        fun getDeviceModelName(): String {
            val manufacturer = android.os.Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
            val model = android.os.Build.MODEL.orEmpty()
            return if (model.startsWith(manufacturer, ignoreCase = true)) {
                model
            } else {
                "$manufacturer $model"
            }.ifBlank { "هاتف أندرويد" }
        }

        suspend fun updateActiveDeviceSessionInFirestore(
            account: StaffAccount,
            deviceId: String,
            deviceName: String,
            sessionToken: String
        ) {
            try {
                withTimeoutOrNull(2000L) {
                    val db = getFirestoreDb()
                    val docId = when {
                        account.isOwner -> "owner_" + account.email.replace("@", "_").replace(".", "_")
                        account.role.lowercase() == "manager" -> managerPreProvisionDocId(account.email.ifBlank { account.fullName })
                        else -> workerPreProvisionDocId(account.email.ifBlank { account.fullName })
                    }
                    val updates = mapOf(
                        "activeDeviceId" to deviceId,
                        "activeDeviceName" to deviceName,
                        "activeSessionToken" to sessionToken,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    db.collection(COLLECTION_STAFF).document(docId).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                    Log.i("StaffRepo", "Updated active device session in Firestore for ${account.fullName}: $deviceId ($deviceName)")
                }
            } catch (e: Exception) {
                Log.w("StaffRepo", "Failed to update active device session in Firestore: ${e.message}")
            }
        }

        fun isDirectOwnerSessionSaved(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            return prefs.getBoolean("keep_session_open_pref", true) &&
                prefs.getBoolean("session_active", false) &&
                prefs.getBoolean("is_direct_owner_session", false)
        }

        fun clearPersistentStaffSession(context: Context) {
            val dataStore = UserSessionDataStore(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    dataStore.clearSession()
                }
            }
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            val keepPref = prefs.getBoolean("keep_session_open_pref", true)
            val registryJson = prefs.getString(KEY_LOCAL_STAFF_REGISTRY, "[]") ?: "[]"
            prefs.edit()
                .clear()
                .putBoolean("keep_session_open_pref", keepPref)
                .putBoolean("session_active", false)
                .putBoolean("is_direct_owner_session", false)
                .putString(KEY_LOCAL_STAFF_REGISTRY, registryJson)
                .apply()
        }

        fun loadLocalAccountsRegistry(context: Context): List<StaffAccount> {
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_LOCAL_STAFF_REGISTRY, "[]") ?: "[]"
            val result = mutableListOf<StaffAccount>()
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    result.add(StaffAccount.fromJsonObject(obj))
                }
            } catch (_: Exception) {
            }
            if (result.none { it.isOwner }) {
                result.add(0, StaffAccount.createVerifiedOwnerAccount())
            }
            return result
        }

        fun saveAccountToLocalRegistry(context: Context, account: StaffAccount) {
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            val current = loadLocalAccountsRegistry(context).toMutableList()
            current.removeAll {
                it.userId == account.userId ||
                    (account.email.isNotBlank() && it.email.equals(account.email, ignoreCase = true))
            }
            current.add(account)
            val arr = JSONArray()
            current.forEach { arr.put(it.toJsonObject()) }
            prefs.edit().putString(KEY_LOCAL_STAFF_REGISTRY, arr.toString()).apply()
        }

        fun removeAccountFromLocalRegistry(context: Context, targetUserId: String, targetEmail: String = "") {
            val prefs = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            val current = loadLocalAccountsRegistry(context).toMutableList()
            current.removeAll {
                !it.isOwner && (
                    it.userId == targetUserId ||
                        (targetEmail.isNotBlank() && it.email.equals(targetEmail, ignoreCase = true))
                    )
            }
            val arr = JSONArray()
            current.forEach { arr.put(it.toJsonObject()) }
            prefs.edit().putString(KEY_LOCAL_STAFF_REGISTRY, arr.toString()).apply()
        }

        /**
         * Asynchronously syncs a StaffAccount to Firebase Firestore.
         */
        fun syncAccountToFirestore(account: StaffAccount, onComplete: ((Boolean) -> Unit)? = null) {
            try {
                val db = getFirestoreDb()
                val docRef = db.collection(COLLECTION_STAFF).document(account.userId)
                val payload = account.toFirestorePayload()
                docRef.set(payload)
                    .addOnSuccessListener {
                        Log.d("StaffRepo", "Staff account ${account.email} synced to Firestore successfully.")
                        onComplete?.invoke(true)
                    }
                    .addOnFailureListener { e ->
                        Log.w("StaffRepo", "Failed to sync staff account ${account.email} to Firestore: ${e.message}")
                        onComplete?.invoke(false)
                    }
            } catch (e: Exception) {
                Log.w("StaffRepo", "Exception syncing staff account to Firestore: ${e.message}")
                onComplete?.invoke(false)
            }
        }

        /**
         * Asynchronously syncs all locally stored staff and manager accounts to Firebase Firestore.
         */
        fun syncAllLocalAccountsToFirestore(context: Context) {
            try {
                val accounts = loadLocalAccountsRegistry(context)
                accounts.forEach { acc ->
                    if (acc.email.isNotBlank() || acc.userId.isNotBlank()) {
                        syncAccountToFirestore(acc)
                    }
                }
            } catch (e: Exception) {
                Log.w("StaffRepo", "syncAllLocalAccountsToFirestore failed: ${e.message}", e)
            }
        }

        /**
         * Authenticates staff login with direct cloud lookup in Firebase Firestore.
         * Allows managers registered from other phones to log in on any phone.
         */
        suspend fun authenticateStaffLoginWithCloud(
            context: Context,
            emailOrUsername: String,
            secretPinOrCode: String
        ): Result<StaffAccount> {
            val cleanInput = emailOrUsername.trim()
            val cleanSecret = secretPinOrCode.trim()
            if (cleanInput.isBlank() || cleanSecret.isBlank()) {
                return Result.failure(IllegalArgumentException("يرجى إدخال الاسم والرمز السري"))
            }

            // 1. Check Owner identity (Deffas antar)
            if (verifyOwnerCredentials(cleanInput, cleanSecret)) {
                val localDeviceId = getOrCreateDeviceId(context)
                val localDeviceName = getDeviceModelName()
                val sessionToken = java.util.UUID.randomUUID().toString()
                val ownerAcc = StaffAccount.createVerifiedOwnerAccount().copy(
                    activeDeviceId = localDeviceId,
                    activeDeviceName = localDeviceName,
                    activeSessionToken = sessionToken
                )
                saveAccountToLocalRegistry(context, ownerAcc)
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        ensureFirebaseCloudChannelConnected(context)
                        StaffAccountRepository(context = context).bootstrapOwnerAccountIfNeeded()
                        syncAllLocalAccountsToFirestore(context)
                        updateActiveDeviceSessionInFirestore(ownerAcc, localDeviceId, localDeviceName, sessionToken)
                    }
                }
                return Result.success(ownerAcc)
            }

            // Ensure cloud channel is connected if not already active
            val hasCloudUser = try {
                if (Firebase.auth.currentUser != null) true else ensureFirebaseCloudChannelConnected(context)
            } catch (_: Exception) {
                false
            }

            // 2. Query Firestore directly for the account by Name + Secret PIN or docId
            var cloudMatched: StaffAccount? = null
            if (hasCloudUser) {
                try {
                    withTimeoutOrNull(6000L) {
                        val db = getFirestoreDb(context)
                        val candidateDocIds = listOf(
                            managerPreProvisionDocId(cleanInput),
                            managerPreProvisionDocId(normalizeStaffInternalEmail(cleanInput, "mgr")),
                            workerPreProvisionDocId(cleanInput),
                            workerPreProvisionDocId(normalizeStaffInternalEmail(cleanInput, "wrk"))
                        ).distinct()

                        for (docId in candidateDocIds) {
                            val snap = db.collection(COLLECTION_STAFF).document(docId).get().await()
                            if (snap.exists()) {
                                val parsed = StaffAccount.fromSnapshot(snap)
                                if (parsed != null && (parsed.secretPin.isBlank() || parsed.secretPin == cleanSecret)) {
                                    cloudMatched = parsed
                                    break
                                } else if (cloudMatched == null && parsed != null) {
                                    cloudMatched = parsed
                                }
                            }
                        }

                        // Query by fullName
                        if (cloudMatched == null || (cloudMatched?.secretPin?.isNotBlank() == true && cloudMatched?.secretPin != cleanSecret)) {
                            val queryNameSnap = db.collection(COLLECTION_STAFF)
                                .whereEqualTo("storeId", DEFAULT_STORE_ID)
                                .whereEqualTo("fullName", cleanInput)
                                .get()
                                .await()
                            val nameDocs = queryNameSnap.documents.mapNotNull { StaffAccount.fromSnapshot(it) }
                            val exactPinMatch = nameDocs.firstOrNull { it.secretPin == cleanSecret } ?: nameDocs.firstOrNull()
                            if (exactPinMatch != null) {
                                cloudMatched = exactPinMatch
                            }
                        }

                        // Query by storeId + secretPin and match Name / Username case-insensitively
                        if (cloudMatched == null || (cloudMatched?.secretPin?.isNotBlank() == true && cloudMatched?.secretPin != cleanSecret)) {
                            val queryPinSnap = db.collection(COLLECTION_STAFF)
                                .whereEqualTo("storeId", DEFAULT_STORE_ID)
                                .whereEqualTo("secretPin", cleanSecret)
                                .get()
                                .await()
                            val pinDocs = queryPinSnap.documents.mapNotNull { StaffAccount.fromSnapshot(it) }
                            val matchedByPinAndName = pinDocs.firstOrNull { acc ->
                                acc.fullName.equals(cleanInput, ignoreCase = true) ||
                                    acc.email.equals(cleanInput, ignoreCase = true) ||
                                    acc.email.substringBefore("@").equals(cleanInput, ignoreCase = true) ||
                                    acc.fullName.trim().contains(cleanInput, ignoreCase = true) ||
                                    cleanInput.contains(acc.fullName.trim(), ignoreCase = true)
                            }
                            if (matchedByPinAndName != null) {
                                cloudMatched = matchedByPinAndName
                            }
                        }

                        // Fallback query by email
                        if (cloudMatched == null) {
                            val queryEmailSnap = db.collection(COLLECTION_STAFF)
                                .whereEqualTo("storeId", DEFAULT_STORE_ID)
                                .whereEqualTo("email", cleanInput.lowercase())
                                .get()
                                .await()
                            cloudMatched = queryEmailSnap.documents.firstOrNull()?.let { StaffAccount.fromSnapshot(it) }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("StaffRepo", "Firestore lookup encountered exception: ${e.message}")
                }
            }

            // 3. Fallback or merge with local registry
            val registry = loadLocalAccountsRegistry(context)
            val matchedLocal = registry.firstOrNull { acc ->
                val matchesIdentifier = acc.fullName.equals(cleanInput, ignoreCase = true) ||
                    acc.email.equals(cleanInput, ignoreCase = true) ||
                    acc.email.substringBefore("@").equals(cleanInput, ignoreCase = true)
                matchesIdentifier && acc.secretPin.isNotBlank() && acc.secretPin == cleanSecret
            }

            val resolvedCloud = cloudMatched
            val targetAccount = when {
                resolvedCloud != null -> {
                    if (resolvedCloud.secretPin.isBlank() && matchedLocal?.secretPin?.isNotBlank() == true) {
                        resolvedCloud.copy(secretPin = matchedLocal.secretPin)
                    } else {
                        resolvedCloud
                    }
                }
                else -> matchedLocal
            }

            if (targetAccount == null) {
                return Result.failure(
                    IllegalArgumentException("بيانات الدخول غير صحيحة! تأكد من كتابة الاسم الكامل والرمز السري كما أنشأهما المالك أو المدير.")
                )
            }

            // Validate Secret PIN
            if (targetAccount.secretPin.isNotBlank() && targetAccount.secretPin != cleanSecret) {
                return Result.failure(
                    IllegalArgumentException("الرمز السري غير صحيح لحساب (${targetAccount.fullName})! تأكد من الرمز السري.")
                )
            }

            if (targetAccount.approvalStatus == StaffApprovalStatus.PENDING) {
                return Result.failure(
                    IllegalStateException("طلب الحساب الخاص بـ (${targetAccount.fullName}) مسجل لدى إدارة المتجر وبانتظار موافقة وتفعيل المالك (Deffas antar) ⏳.")
                )
            }

            if (targetAccount.approvalStatus == StaffApprovalStatus.REJECTED) {
                return Result.failure(
                    IllegalStateException("تم رفض طلب هذا الحساب من قبل إدارة المتجر.")
                )
            }

            if (targetAccount.approvalStatus == StaffApprovalStatus.SUSPENDED) {
                return Result.failure(
                    IllegalStateException("تم إيقاف أو تجميد هذا الحساب مؤقتاً من قبل المالك.")
                )
            }

            if (!targetAccount.isApproved) {
                return Result.failure(
                    IllegalStateException("هذا الحساب غير مفعّل حالياً (${StaffApprovalStatus.labelAr(targetAccount.approvalStatus)}).")
                )
            }

            val localDeviceId = getOrCreateDeviceId(context)
            val localDeviceName = getDeviceModelName()
            val sessionToken = java.util.UUID.randomUUID().toString()

            val canonicalUserId = when {
                targetAccount.isOwner -> "owner_deffas_antar"
                (targetAccount.role == "manager" || targetAccount.role == "admin") && targetAccount.fullName.isNotBlank() ->
                    managerPreProvisionDocId(targetAccount.fullName)
                else -> targetAccount.userId
            }
            val canonicalParentManagerUid = when {
                targetAccount.role == "worker" && !targetAccount.isOwner -> {
                    when {
                        targetAccount.parentManagerName.isNotBlank() &&
                            !isOwnerIdentity(targetAccount.parentManagerName) ->
                            managerPreProvisionDocId(targetAccount.parentManagerName)
                        targetAccount.parentManagerUid.isNotBlank() -> targetAccount.parentManagerUid
                        targetAccount.approvedByName.isNotBlank() &&
                            !isOwnerIdentity(targetAccount.approvedByName) ->
                            managerPreProvisionDocId(targetAccount.approvedByName)
                        targetAccount.approvedByUid.isNotBlank() -> targetAccount.approvedByUid
                        else -> "owner_deffas_antar"
                    }
                }
                else -> targetAccount.parentManagerUid
            }

            val sessionAccount = targetAccount.copy(
                userId = canonicalUserId,
                parentManagerUid = canonicalParentManagerUid,
                activeDeviceId = localDeviceId,
                activeDeviceName = localDeviceName,
                activeSessionToken = sessionToken
            )

            // Sync verified account directly to local registry so all devices stay synchronized
            saveAccountToLocalRegistry(context, sessionAccount)

            // Register active device on Firestore in background so login is instantaneous
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    syncAccountToFirestore(sessionAccount)
                    updateActiveDeviceSessionInFirestore(sessionAccount, localDeviceId, localDeviceName, sessionToken)
                }
            }

            return Result.success(sessionAccount)
        }

        /**
         * Authenticates direct login for Owner (Deffas antar), an Independent Manager profile,
         * or an Affiliated Worker account. Checks cloud Firestore first for active status, falling back to local registry.
         */
        fun authenticateDirectStaffLogin(
            context: Context,
            emailOrUsername: String,
            secretPinOrCode: String
        ): Result<StaffAccount> {
            val cleanInput = emailOrUsername.trim()
            val cleanSecret = secretPinOrCode.trim()
            if (cleanInput.isBlank() || cleanSecret.isBlank()) {
                return Result.failure(IllegalArgumentException("يرجى إدخال البريد/اسم الدخول والرمز السري"))
            }

            // 1. Check Owner identity (Deffas antar - Antar7theman@gmail.com)
            if (verifyOwnerCredentials(cleanInput, cleanSecret)) {
                val localDeviceId = getOrCreateDeviceId(context)
                val localDeviceName = getDeviceModelName()
                val sessionToken = java.util.UUID.randomUUID().toString()
                val ownerAcc = StaffAccount.createVerifiedOwnerAccount().copy(
                    activeDeviceId = localDeviceId,
                    activeDeviceName = localDeviceName,
                    activeSessionToken = sessionToken
                )
                saveAccountToLocalRegistry(context, ownerAcc)
                runCatching {
                    runBlocking {
                        updateActiveDeviceSessionInFirestore(ownerAcc, localDeviceId, localDeviceName, sessionToken)
                    }
                }
                return Result.success(ownerAcc)
            }

            // 2. Perform direct cloud-first check for active account status with fallback to local registry
            return try {
                runBlocking {
                    authenticateStaffLoginWithCloud(context, cleanInput, cleanSecret)
                }
            } catch (e: Exception) {
                val registry = loadLocalAccountsRegistry(context)
                val matched = registry.firstOrNull { acc ->
                    val matchesIdentifier = acc.email.equals(cleanInput, ignoreCase = true) ||
                        acc.fullName.equals(cleanInput, ignoreCase = true) ||
                        acc.email.substringBefore("@").equals(cleanInput, ignoreCase = true)
                    matchesIdentifier && acc.secretPin.isNotBlank() && acc.secretPin == cleanSecret
                }
                if (matched != null) {
                    if (matched.approvalStatus == StaffApprovalStatus.PENDING) {
                        return Result.failure(
                            IllegalStateException("ملف الحساب (${matched.fullName}) مسجل وبانتظار موافقة وتفعيل المالك (Deffas antar).")
                        )
                    }
                    if (!matched.isApproved) {
                        return Result.failure(
                            IllegalStateException("هذا الحساب غير مفعّل حالياً (${StaffApprovalStatus.labelAr(matched.approvalStatus)}).")
                        )
                    }
                    Result.success(matched)
                } else {
                    Result.failure(
                        IllegalArgumentException(e.localizedMessage ?: "بيانات الدخول غير صحيحة! تأكد من البريد/الاسم والرمز السري الخاص بالحساب.")
                    )
                }
            }
        }

        /**
         * Registers a new Independent Manager Profile from the Login Screen.
         * Saves to local registry AND automatically pushes to Firebase Firestore for cross-phone sync.
         */
        fun registerIndependentManagerFromLogin(
            context: Context,
            fullName: String,
            email: String = "",
            phone: String,
            branchName: String,
            managerSecretPin: String,
            optionalOwnerSecretCode: String = "",
            permissions: WorkerPermissions = WorkerPermissions(
                canMakeSales = true,
                canManageInventory = true,
                canEditPrices = true,
                canViewReports = true,
                canManageDebtsAndCustomers = true,
                canRefundOrVoid = true,
                canApplyDiscounts = true,
                canViewCostPrices = true,
                canExportData = true,
                canManagePurchaseInvoices = true
            )
        ): Result<StaffAccount> {
            val cleanName = fullName.trim().take(100)
            val cleanPhone = phone.trim().take(30)
            val cleanBranch = branchName.trim().ifBlank { "فرع مستقل" }.take(100)
            val cleanPin = managerSecretPin.trim().take(64)

            if (cleanName.length < 2) {
                return Result.failure(IllegalArgumentException("يرجى إدخال اسم المدير المستقل كاملاً"))
            }
            if (cleanPin.length < 4) {
                return Result.failure(IllegalArgumentException("يرجى تعيين رمز سري للمدير لا يقل عن 4 خانات"))
            }
            val cleanEmail = normalizeStaffInternalEmail(email.ifBlank { cleanName }, "mgr")
            if (isOwnerEmail(cleanEmail) || isOwnerIdentity(cleanName)) {
                return Result.failure(IllegalArgumentException("هذا الاسم مخصص لحساب المالك الأساسي Deffas antar"))
            }

            val approveImmediately = if (optionalOwnerSecretCode.trim().isNotBlank()) {
                if (!verifyOwnerSecretCode(optionalOwnerSecretCode)) {
                    return Result.failure(
                        IllegalArgumentException("الرمز السري للمالك (Deffas antar) غير صحيح! اتركه فارغاً لإرسال طلب موافقة أو أدخل الرمز الصحيح للتفعيل الفوري.")
                    )
                }
                true
            } else {
                false
            }

            val docId = managerPreProvisionDocId(cleanName)
            val newManagerAccount = StaffAccount(
                userId = docId,
                storeId = DEFAULT_STORE_ID,
                email = cleanEmail,
                fullName = cleanName,
                phone = cleanPhone,
                requestedRole = "manager",
                role = if (approveImmediately) "manager" else "worker",
                approvalStatus = if (approveImmediately) StaffApprovalStatus.APPROVED else StaffApprovalStatus.PENDING,
                approvedByUid = if (approveImmediately) "owner_deffas_antar" else "",
                approvedByName = if (approveImmediately) OWNER_FULL_NAME else "",
                branchName = cleanBranch,
                secretPin = cleanPin,
                canMakeSales = permissions.canMakeSales,
                canManageInventory = permissions.canManageInventory,
                canEditPrices = permissions.canEditPrices,
                canViewReports = permissions.canViewReports,
                canManageDebtsAndCustomers = permissions.canManageDebtsAndCustomers,
                canRefundOrVoid = permissions.canRefundOrVoid,
                canApplyDiscounts = permissions.canApplyDiscounts,
                canViewCostPrices = permissions.canViewCostPrices,
                canExportData = permissions.canExportData,
                canManagePurchaseInvoices = permissions.canManagePurchaseInvoices,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // 1. Save to local storage for quick offline access
            saveAccountToLocalRegistry(context, newManagerAccount)

            // 2. Automatically sync to Firebase Firestore for cross-phone access
            syncAccountToFirestore(newManagerAccount)

            return Result.success(newManagerAccount)
        }

        /**
         * Suspend function to register a new Manager and await cloud confirmation.
         */
        suspend fun registerIndependentManagerWithCloudSync(
            context: Context,
            fullName: String,
            email: String = "",
            phone: String,
            branchName: String,
            managerSecretPin: String,
            optionalOwnerSecretCode: String = "",
            permissions: WorkerPermissions = WorkerPermissions(
                canMakeSales = true,
                canManageInventory = true,
                canEditPrices = true,
                canViewReports = true,
                canManageDebtsAndCustomers = true,
                canRefundOrVoid = true,
                canApplyDiscounts = true,
                canViewCostPrices = true,
                canExportData = true,
                canManagePurchaseInvoices = true
            )
        ): Result<StaffAccount> {
            val localResult = registerIndependentManagerFromLogin(
                context = context,
                fullName = fullName,
                email = email,
                phone = phone,
                branchName = branchName,
                managerSecretPin = managerSecretPin,
                optionalOwnerSecretCode = optionalOwnerSecretCode,
                permissions = permissions
            )
            if (localResult.isFailure) return localResult

            val account = localResult.getOrThrow()
            val hasCloud = try {
                if (Firebase.auth.currentUser != null) true else ensureFirebaseCloudChannelConnected(context)
            } catch (_: Exception) {
                false
            }
            if (!hasCloud) {
                return Result.success(account)
            }
            return try {
                val db = getFirestoreDb(context)
                val docRef = db.collection(COLLECTION_STAFF).document(account.userId)
                docRef.set(account.toFirestorePayload()).await()
                Result.success(account)
            } catch (e: Exception) {
                Log.w("StaffRepo", "Cloud sync on register failed (kept locally): ${e.message}", e)
                Result.success(account)
            }
        }

        /**
         * Registers a new Pro 7-Day Trial Manager Account.
         * Creates a pending staff account with isTrial = true, saves locally and pushes to Firestore,
         * and writes an upgrade request to notify the primary store owner (Deffas Antar).
         */
        suspend fun registerProTrialManagerAccount(
            context: Context,
            fullName: String,
            storeName: String,
            phone: String,
            email: String = "",
            managerSecretPin: String,
            businessType: String = ""
        ): Result<StaffAccount> {
            val cleanName = fullName.trim().take(100)
            val cleanStore = storeName.trim().ifBlank { "متجر $cleanName" }.take(100)
            val cleanPhone = phone.trim().take(30)
            val cleanPin = managerSecretPin.trim().take(64)
            val cleanBiz = businessType.trim().take(100)

            if (cleanName.length < 2) {
                return Result.failure(IllegalArgumentException("يرجى إدخال الاسم الكامل للمدير"))
            }
            if (cleanStore.length < 2) {
                return Result.failure(IllegalArgumentException("يرجى إدخال اسم المتجر أو المنشأة"))
            }
            if (cleanPhone.length < 8) {
                return Result.failure(IllegalArgumentException("يرجى إدخال رقم هاتف صحيح للتواصل وتفعيل الحساب"))
            }
            if (cleanPin.length < 4) {
                return Result.failure(IllegalArgumentException("يرجى تعيين رمز سري للحساب لا يقل عن 4 خانات"))
            }
            val cleanEmail = normalizeStaffInternalEmail(email.ifBlank { cleanName }, "mgr")
            if (isOwnerEmail(cleanEmail) || isOwnerIdentity(cleanName)) {
                return Result.failure(IllegalArgumentException("هذا الاسم مخصص لحساب المالك الأساسي Deffas antar"))
            }

            val docId = managerPreProvisionDocId(cleanName)
            val trialDays = 7L
            val trialDurationMillis = trialDays * 24L * 3600L * 1000L
            val initialTrialEnd = System.currentTimeMillis() + trialDurationMillis

            val trialManager = StaffAccount(
                userId = docId,
                storeId = DEFAULT_STORE_ID,
                email = cleanEmail,
                fullName = cleanName,
                phone = cleanPhone,
                requestedRole = "manager",
                role = "manager",
                approvalStatus = StaffApprovalStatus.PENDING,
                branchName = cleanStore,
                secretPin = cleanPin,
                isTrial = true,
                trialEndsAtMillis = initialTrialEnd,
                syncMode = "pro",
                canMakeSales = true,
                canManageInventory = true,
                canEditPrices = true,
                canViewReports = true,
                canManageDebtsAndCustomers = true,
                canRefundOrVoid = true,
                canApplyDiscounts = true,
                canViewCostPrices = true,
                canExportData = true,
                canManagePurchaseInvoices = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // 1. Save locally to registry
            saveAccountToLocalRegistry(context, trialManager)

            // Ensure cloud transport channel is connected so Owner receives the request immediately
            try {
                if (Firebase.auth.currentUser == null) {
                    ensureFirebaseCloudChannelConnected(context)
                }
            } catch (_: Exception) {
            }

            // 2. Sync to Firestore in staff collection
            val db = getFirestoreDb(context)
            try {
                val staffRef = db.collection(COLLECTION_STAFF).document(docId)
                staffRef.set(trialManager.toFirestorePayload()).await()
            } catch (e: Exception) {
                Log.w("StaffRepo", "Failed to sync staff account for pro trial: ${e.message}", e)
            }

            // 3. Write Pro Upgrade / Trial Request document to notify owner
            try {
                val req = ProUpgradeRequest(
                    requestId = docId,
                    managerUid = docId,
                    managerName = cleanName,
                    managerEmail = cleanEmail,
                    phone = cleanPhone,
                    branchName = cleanStore,
                    status = "PENDING",
                    note = "طلب فتح حساب Pro تجريبي (7 أيام)" + (if (cleanBiz.isNotBlank()) " - نشاط: $cleanBiz" else ""),
                    requestedAt = Timestamp.now()
                )
                db.collection(COLLECTION_PRO_UPGRADE_REQUESTS).document(docId).set(req.toFirestorePayload()).await()
            } catch (e: Exception) {
                Log.w("StaffRepo", "Failed to write pro upgrade request: ${e.message}", e)
            }

            return Result.success(trialManager)
        }

        /**
         * Registers a new Free Lite / Offline Manager Account.
         * Creates an immediately approved staff account (approvalStatus = APPROVED) with syncMode = "lite",
         * saves it to the local registry and Firestore, and writes an informational notification
         * to notify the primary store owner (Deffas Antar) without requiring any approval or action.
         */
        suspend fun createLiteManagerAccount(
            context: Context,
            fullName: String,
            storeName: String,
            phone: String,
            email: String = "",
            managerSecretPin: String,
            businessType: String = ""
        ): Result<StaffAccount> {
            val cleanName = fullName.trim().take(100)
            val cleanStore = storeName.trim().ifBlank { "متجر $cleanName" }.take(100)
            val cleanPhone = phone.trim().take(30)
            val cleanPin = managerSecretPin.trim().take(64)
            val cleanBiz = businessType.trim().take(100)

            if (cleanName.length < 2) {
                return Result.failure(IllegalArgumentException("يرجى إدخال اسم المستخدم / المدير"))
            }
            if (cleanStore.length < 2) {
                return Result.failure(IllegalArgumentException("يرجى إدخال اسم المتجر أو النشاط"))
            }
            if (cleanPhone.length < 8) {
                return Result.failure(IllegalArgumentException("يرجى إدخال رقم هاتف صحيح للتواصل"))
            }
            if (cleanPin.length < 4) {
                return Result.failure(IllegalArgumentException("يرجى تعيين رمز سري للحساب لا يقل عن 4 خانات"))
            }
            val cleanEmail = normalizeStaffInternalEmail(email.ifBlank { cleanName }, "mgr")
            if (isOwnerEmail(cleanEmail) || isOwnerIdentity(cleanName)) {
                return Result.failure(IllegalArgumentException("هذا الاسم مخصص لحساب المالك الأساسي Deffas antar"))
            }

            val docId = managerPreProvisionDocId(cleanName)

            val liteManager = StaffAccount(
                userId = docId,
                storeId = DEFAULT_STORE_ID,
                email = cleanEmail,
                fullName = cleanName,
                phone = cleanPhone,
                requestedRole = "manager",
                role = "manager",
                approvalStatus = StaffApprovalStatus.APPROVED,
                branchName = cleanStore,
                secretPin = cleanPin,
                isTrial = false,
                trialEndsAtMillis = 0L,
                syncMode = "lite",
                canMakeSales = true,
                canManageInventory = true,
                canEditPrices = true,
                canViewReports = true,
                canManageDebtsAndCustomers = true,
                canRefundOrVoid = true,
                canApplyDiscounts = true,
                canViewCostPrices = false,
                canExportData = false,
                canManagePurchaseInvoices = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // 1. Save locally to registry
            saveAccountToLocalRegistry(context, liteManager)

            // Ensure cloud transport channel is connected so Owner receives the notification immediately
            try {
                if (Firebase.auth.currentUser == null) {
                    ensureFirebaseCloudChannelConnected(context)
                }
            } catch (_: Exception) {
            }

            // 2. Sync to Firestore in staff collection
            val db = getFirestoreDb(context)
            try {
                val staffRef = db.collection(COLLECTION_STAFF).document(docId)
                staffRef.set(liteManager.toFirestorePayload()).await()
            } catch (e: Exception) {
                Log.w("StaffRepo", "Failed to sync staff account for lite manager: ${e.message}", e)
            }

            // 3. Write notification document to notify owner
            try {
                val notif = ProUpgradeRequest(
                    requestId = docId,
                    managerUid = docId,
                    managerName = cleanName,
                    managerEmail = cleanEmail,
                    phone = cleanPhone,
                    branchName = cleanStore,
                    status = "LITE_ACCOUNT_CREATED",
                    note = "فتح حساب Lite / offline مجاني فورياً دون طلب موافقة" + (if (cleanBiz.isNotBlank()) " - نشاط: $cleanBiz" else ""),
                    requestedAt = Timestamp.now()
                )
                db.collection(COLLECTION_PRO_UPGRADE_REQUESTS).document(docId).set(notif.toFirestorePayload()).await()
            } catch (e: Exception) {
                Log.w("StaffRepo", "Failed to write lite account notification for owner: ${e.message}", e)
            }

            return Result.success(liteManager)
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeMyStaffAccount(uid: String): Flow<StaffAccount?> = callbackFlow {
        val currentContext = appContext
        if (uid.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val docRef = db.collection(COLLECTION_STAFF).document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                val local = if (currentContext != null) loadLocalAccountsRegistry(currentContext).firstOrNull { it.userId == uid } else null
                trySend(local)
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.let { StaffAccount.fromSnapshot(it) })
        }
        awaitClose { listener.remove() }
    }.catch { error ->
        Log.w("StaffRepo", "Graceful fallback in observeMyStaffAccount: ${error.message}")
        val local = if (appContext != null) loadLocalAccountsRegistry(appContext!!).firstOrNull { it.userId == uid } else null
        emit(local)
    }

    fun observeAllStoreStaffAccounts(): Flow<List<StaffAccount>> = flow {
        val currentContext = appContext
        emitAll(
            callbackFlow {
                val query = db.collection(COLLECTION_STAFF).whereEqualTo("storeId", DEFAULT_STORE_ID)
                val listener = query.addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        val localList = if (currentContext != null) loadLocalAccountsRegistry(currentContext) else emptyList()
                        trySend(localList)
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = querySnapshot?.documents
                        ?.mapNotNull { StaffAccount.fromSnapshot(it) }
                        ?.sortedWith(
                            compareBy<StaffAccount> {
                                when {
                                    it.isOwner -> -1
                                    it.approvalStatus == StaffApprovalStatus.PENDING -> 0
                                    it.role == "manager" -> 1
                                    it.approvalStatus == StaffApprovalStatus.APPROVED -> 2
                                    else -> 3
                                }
                            }.thenByDescending { it.updatedAt?.seconds ?: 0L }
                        ) ?: emptyList()
                    trySend(list)
                }
                awaitClose { listener.remove() }
            }.catch { error ->
                Log.w("StaffRepo", "Graceful fallback in observeAllStoreStaffAccounts: ${error.message}")
                val localList = if (currentContext != null) loadLocalAccountsRegistry(currentContext) else emptyList()
                emit(localList)
            }
        )
    }

    /**
     * Real-time flow of all PENDING manager requests submitted from any phone.
     * Emits immediately whenever a new manager registers from another phone.
     */
    fun observePendingManagerRequests(): Flow<List<StaffAccount>> = callbackFlow {
        val currentContext = appContext
        val query = db.collection(COLLECTION_STAFF)
            .whereEqualTo("storeId", DEFAULT_STORE_ID)
            .whereEqualTo("approvalStatus", StaffApprovalStatus.PENDING)
        val listener = query.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                val localList = if (currentContext != null) {
                    loadLocalAccountsRegistry(currentContext).filter {
                        it.approvalStatus == StaffApprovalStatus.PENDING && (it.role == "manager" || it.requestedRole == "manager")
                    }
                } else emptyList()
                trySend(localList)
                close(error)
                return@addSnapshotListener
            }
            val list = querySnapshot?.documents
                ?.mapNotNull { StaffAccount.fromSnapshot(it) }
                ?.filter { it.requestedRole == "manager" || it.role == "manager" }
                ?.sortedByDescending { it.createdAt?.seconds ?: 0L }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }.catch { error ->
        Log.w("StaffRepo", "Error observing pending manager requests: ${error.message}")
        val localList = if (appContext != null) {
            loadLocalAccountsRegistry(appContext!!).filter {
                it.approvalStatus == StaffApprovalStatus.PENDING && (it.role == "manager" || it.requestedRole == "manager")
            }
        } else emptyList()
        emit(localList)
    }

    /**
     * Real-time flow observing a specific staff/manager account status in Firestore.
     * Used by active sessions to immediately terminate the session if the owner suspends, rejects, or deletes the account.
     */
    fun observeStaffAccountStatusLive(account: StaffAccount): Flow<StaffAccount?> = callbackFlow {
        val targetDocId = when {
            account.isOwner -> "owner_" + account.email.replace("@", "_").replace(".", "_")
            account.userId.isNotBlank() && (account.userId.startsWith("mgr_") || account.userId.startsWith("wrk_")) -> account.userId
            account.email.isNotBlank() && account.isRegularManager -> managerPreProvisionDocId(account.email)
            account.email.isNotBlank() && !account.isOwner -> workerPreProvisionDocId(account.email)
            account.userId.isNotBlank() -> account.userId
            else -> ""
        }

        if (targetDocId.isBlank()) {
            trySend(account)
            awaitClose { }
            return@callbackFlow
        }

        val docRef = db.collection(COLLECTION_STAFF).document(targetDocId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(account)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                if (account.isOwner) {
                    trySend(account)
                } else {
                    trySend(null)
                }
            } else {
                val fromCloud = StaffAccount.fromSnapshot(snapshot)
                if (account.isOwner && fromCloud != null) {
                    trySend(fromCloud.copy(
                        role = "owner",
                        approvalStatus = StaffApprovalStatus.APPROVED,
                        canMakeSales = true,
                        canManageInventory = true,
                        canEditPrices = true,
                        canViewReports = true,
                        canManageDebtsAndCustomers = true,
                        canRefundOrVoid = true,
                        canApplyDiscounts = true,
                        canViewCostPrices = true,
                        canExportData = true,
                        canManagePurchaseInvoices = true
                    ))
                } else {
                    trySend(fromCloud)
                }
            }
        }
        awaitClose { listener.remove() }
    }.catch { error ->
        Log.w("StaffRepo", "Graceful fallback in observeStaffAccountStatusLive: ${error.message}")
        emit(account)
    }

    /**
     * Approves a pending manager request, granting full store and sales management permissions.
     */
    suspend fun approvePendingManagerRequest(
        targetAccount: StaffAccount,
        approvedByUid: String = "owner_deffas_antar",
        approvedByName: String = OWNER_FULL_NAME,
        asTrial7Days: Boolean = targetAccount.isTrial
    ): Result<StaffAccount> {
        val trialEnds = if (asTrial7Days) {
            System.currentTimeMillis() + (7L * 24 * 3600 * 1000L)
        } else {
            0L
        }
        val updated = targetAccount.copy(
            approvalStatus = StaffApprovalStatus.APPROVED,
            role = "manager",
            isTrial = asTrial7Days,
            trialEndsAtMillis = trialEnds,
            syncMode = "pro",
            approvedByUid = approvedByUid.ifBlank { "owner_deffas_antar" },
            approvedByName = approvedByName.ifBlank { OWNER_FULL_NAME },
            canMakeSales = true,
            canManageInventory = true,
            canEditPrices = true,
            canViewReports = true,
            canManageDebtsAndCustomers = true,
            canRefundOrVoid = true,
            canApplyDiscounts = true,
            canViewCostPrices = true,
            canExportData = true,
            canManagePurchaseInvoices = true,
            updatedAt = Timestamp.now()
        )
        val docRef = db.collection(COLLECTION_STAFF).document(targetAccount.userId)
        val updates = mutableMapOf<String, Any>(
            "approvalStatus" to StaffApprovalStatus.APPROVED,
            "role" to "manager",
            "isTrial" to asTrial7Days,
            "trialEndsAtMillis" to trialEnds,
            "syncMode" to "pro",
            "approvedByUid" to approvedByUid.ifBlank { "owner_deffas_antar" },
            "approvedByName" to approvedByName.ifBlank { OWNER_FULL_NAME },
            "canMakeSales" to true,
            "canManageInventory" to true,
            "canEditPrices" to true,
            "canViewReports" to true,
            "canManageDebtsAndCustomers" to true,
            "canRefundOrVoid" to true,
            "canApplyDiscounts" to true,
            "canViewCostPrices" to true,
            "canExportData" to true,
            "canManagePurchaseInvoices" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            try {
                db.collection(COLLECTION_PRO_UPGRADE_REQUESTS).document(targetAccount.userId).update(
                    mapOf(
                        "status" to "APPROVED",
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "reviewedByUid" to (auth.currentUser?.uid ?: "owner_deffas_antar")
                    )
                ).await()
            } catch (_: Exception) {}
            Result.success(updated)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Rejects / deletes a pending manager request from Firestore.
     */
    suspend fun rejectPendingManagerRequest(
        targetAccount: StaffAccount
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_STAFF).document(targetAccount.userId)
        return try {
            docRef.delete().await()
            try {
                db.collection(COLLECTION_PRO_UPGRADE_REQUESTS).document(targetAccount.userId).update(
                    mapOf(
                        "status" to "REJECTED",
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "reviewedByUid" to (auth.currentUser?.uid ?: "owner_deffas_antar")
                    )
                ).await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Observes only the worker accounts affiliated with a specific Independent Manager.
     * Enforces strict manager isolation in Firestore queries.
     */
    fun observeManagerAffiliatedWorkers(managerUid: String): Flow<List<StaffAccount>> = callbackFlow {
        val currentContext = appContext
        if (managerUid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val query = db.collection(COLLECTION_STAFF)
            .whereEqualTo("storeId", DEFAULT_STORE_ID)
            .whereEqualTo("role", "worker")
            .whereEqualTo("parentManagerUid", managerUid)
        val listener = query.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                Log.w("StaffRepo", "Graceful fallback in observeManagerAffiliatedWorkers listener: ${error.message}")
                val localWorkers = if (currentContext != null) {
                    loadLocalAccountsRegistry(currentContext).filter {
                        it.role == "worker" && it.parentManagerUid == managerUid
                    }
                } else emptyList()
                trySend(localWorkers)
                close(error)
                return@addSnapshotListener
            }
            val list = querySnapshot?.documents
                ?.mapNotNull { StaffAccount.fromSnapshot(it) }
                ?.sortedByDescending { it.updatedAt?.seconds ?: 0L }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }.catch { error ->
        Log.w("StaffRepo", "Graceful fallback in observeManagerAffiliatedWorkers: ${error.message}")
        val localWorkers = if (appContext != null) {
            loadLocalAccountsRegistry(appContext!!).filter {
                it.role == "worker" && it.parentManagerUid == managerUid
            }
        } else emptyList()
        emit(localWorkers)
    }

    suspend fun getMyStaffAccountOnce(): Result<StaffAccount?> {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_STAFF).document(uid)
        return try {
            val snap = docRef.get().await()
            Result.success(StaffAccount.fromSnapshot(snap))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Self-registration for new staff is strictly for WORKER accounts.
     */
    suspend fun registerPendingAccountRequest(
        fullName: String,
        phone: String,
        requestedRole: String = "worker"
    ): Result<Unit> {
        val uid = requireUserId()
        val currentUser = auth.currentUser
        val safeEmail = (currentUser?.email ?: "user@afstore.app").trim().take(150)
        val safeName = fullName.trim().ifBlank {
            currentUser?.displayName?.trim().takeIf { !it.isNullOrBlank() } ?: "عامل جديد"
        }.take(100)
        val safePhone = phone.trim().take(30)
        val safeRequestedRole = if (isOwnerEmail(safeEmail) && requestedRole == "owner") "owner" else "worker"

        val docRef = db.collection(COLLECTION_STAFF).document(uid)
        val payload = mapOf(
            "userId" to uid,
            "storeId" to DEFAULT_STORE_ID,
            "email" to safeEmail,
            "fullName" to safeName,
            "phone" to safePhone,
            "requestedRole" to safeRequestedRole,
            "role" to "worker",
            "approvalStatus" to StaffApprovalStatus.PENDING,
            "approvedByUid" to "",
            "approvedByName" to "",
            "canMakeSales" to true,
            "canManageInventory" to true,
            "canEditPrices" to false,
            "canViewReports" to false,
            "canManageDebtsAndCustomers" to true,
            "canRefundOrVoid" to false,
            "canApplyDiscounts" to true,
            "canViewCostPrices" to false,
            "canExportData" to false,
            "canManagePurchaseInvoices" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Bootstraps the Store Owner account (Deffas antar - Antar7theman@gmail.com) with role='owner'.
     */
    suspend fun bootstrapOwnerAccountIfNeeded(
        fullNameOverride: String = OWNER_FULL_NAME,
        phoneOverride: String = "0662931569"
    ): Result<Unit> {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid ?: "owner_deffas_antar"
        val email = (currentUser?.email ?: OWNER_DISPLAY_EMAIL).trim().take(150)
        val fullName = fullNameOverride.trim().ifBlank { OWNER_FULL_NAME }.take(100)
        val phone = phoneOverride.trim().take(30)

        val adminRef = db.collection(COLLECTION_ADMINS).document(uid)
        val staffRef = db.collection(COLLECTION_STAFF).document(uid)

        return try {
            if (currentUser != null) {
                val adminSnap = adminRef.get().await()
                if (!adminSnap.exists()) {
                    val adminPayload = mapOf(
                        "userId" to uid,
                        "storeId" to DEFAULT_STORE_ID,
                        "email" to email,
                        "fullName" to fullName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    adminRef.set(adminPayload).await()
                }
            }

            val staffSnap = staffRef.get().await()
            if (!staffSnap.exists()) {
                val staffPayload = mapOf(
                    "userId" to uid,
                    "storeId" to DEFAULT_STORE_ID,
                    "email" to email,
                    "fullName" to OWNER_FULL_NAME,
                    "phone" to phone,
                    "requestedRole" to "owner",
                    "role" to "owner",
                    "approvalStatus" to StaffApprovalStatus.APPROVED,
                    "approvedByUid" to uid,
                    "approvedByName" to OWNER_FULL_NAME,
                    "secretPin" to OWNER_SECRET_CODE,
                    "canMakeSales" to true,
                    "canManageInventory" to true,
                    "canEditPrices" to true,
                    "canViewReports" to true,
                    "canManageDebtsAndCustomers" to true,
                    "canRefundOrVoid" to true,
                    "canApplyDiscounts" to true,
                    "canViewCostPrices" to true,
                    "canExportData" to true,
                    "canManagePurchaseInvoices" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                staffRef.set(staffPayload).await()
            } else if (staffSnap.getString("fullName") != OWNER_FULL_NAME || staffSnap.getString("role") != "owner") {
                staffRef.update(
                    mapOf(
                        "fullName" to OWNER_FULL_NAME,
                        "role" to "owner",
                        "requestedRole" to "owner",
                        "approvalStatus" to StaffApprovalStatus.APPROVED,
                        "approvedByName" to OWNER_FULL_NAME,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, staffRef.path)
            Result.failure(e)
        }
    }

    /**
     * Checks if the signed-in Google user's email matches a Manager or Worker account manually
     * pre-created by the Store Owner or an Independent Manager.
     */
    suspend fun claimPreApprovedManagerAccountIfMatchingEmail(): Result<StaffAccount?> {
        val uid = requireUserId()
        val email = auth.currentUser?.email?.trim()?.lowercase() ?: return Result.success(null)
        if (email.isBlank() || isOwnerEmail(email)) return Result.success(null)

        val mgrDocId = managerPreProvisionDocId(email)
        val wrkDocId = workerPreProvisionDocId(email)
        val uidRef = db.collection(COLLECTION_STAFF).document(uid)
        val adminRef = db.collection(COLLECTION_ADMINS).document(uid)

        return try {
            val mgrSnap = db.collection(COLLECTION_STAFF).document(mgrDocId).get().await()
            val wrkSnap = if (!mgrSnap.exists()) {
                db.collection(COLLECTION_STAFF).document(wrkDocId).get().await()
            } else {
                null
            }
            val preSnap = when {
                mgrSnap.exists() -> mgrSnap
                wrkSnap?.exists() == true -> wrkSnap
                else -> {
                    // Query by exact email if doc ID was formatted differently
                    try {
                        val emailQuery = db.collection(COLLECTION_STAFF)
                            .whereEqualTo("email", email)
                            .get()
                            .await()
                        emailQuery.documents.firstOrNull { it.id != uid }
                    } catch (qe: Exception) {
                        Log.w("StaffRepo", "Fallback email query encountered exception: ${qe.message}")
                        null
                    }
                }
            }
            val preAccount = preSnap?.let { StaffAccount.fromSnapshot(it) }

            if (preAccount != null && preAccount.isApproved) {
                val claimedPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "storeId" to DEFAULT_STORE_ID,
                    "email" to preAccount.email,
                    "fullName" to preAccount.fullName,
                    "phone" to preAccount.phone,
                    "requestedRole" to preAccount.requestedRole,
                    "role" to preAccount.role,
                    "approvalStatus" to StaffApprovalStatus.APPROVED,
                    "approvedByUid" to preAccount.approvedByUid,
                    "approvedByName" to preAccount.approvedByName.ifBlank { OWNER_FULL_NAME },
                    "parentManagerUid" to preAccount.parentManagerUid,
                    "parentManagerName" to preAccount.parentManagerName,
                    "parentManagerEmail" to preAccount.parentManagerEmail,
                    "branchName" to preAccount.branchName,
                    "secretPin" to preAccount.secretPin,
                    "canMakeSales" to preAccount.canMakeSales,
                    "canManageInventory" to preAccount.canManageInventory,
                    "canEditPrices" to preAccount.canEditPrices,
                    "canViewReports" to preAccount.canViewReports,
                    "canManageDebtsAndCustomers" to preAccount.canManageDebtsAndCustomers,
                    "canRefundOrVoid" to preAccount.canRefundOrVoid,
                    "canApplyDiscounts" to preAccount.canApplyDiscounts,
                    "canViewCostPrices" to preAccount.canViewCostPrices,
                    "canExportData" to preAccount.canExportData,
                    "canManagePurchaseInvoices" to preAccount.canManagePurchaseInvoices,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                uidRef.set(claimedPayload).await()

                if (preAccount.role == "manager") {
                    val adminPayload = mapOf(
                        "userId" to uid,
                        "storeId" to DEFAULT_STORE_ID,
                        "email" to preAccount.email,
                        "fullName" to preAccount.fullName,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    adminRef.set(adminPayload).await()
                }

                if (preSnap.id != uid) {
                    try {
                        preSnap.reference.delete().await()
                    } catch (_: Exception) {
                    }
                }
                val freshSnap = uidRef.get().await()
                val claimedResult = StaffAccount.fromSnapshot(freshSnap)
                if (claimedResult != null) {
                    val targetCtx = appContext ?: com.example.AFStoreApplication.instance
                    if (targetCtx != null) {
                        saveAccountToLocalRegistry(targetCtx, claimedResult)
                    }
                    Log.i("StaffRepo", "Successfully claimed pre-approved manager account: ${claimedResult.fullName} (${claimedResult.role}) for UID: $uid")
                }
                Result.success(claimedResult)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("StaffRepo", "Error claiming pre-approved manager account: ${e.message}", e)
            Result.success(null)
        }
    }

    /**
     * Exclusive Owner Action: Manually creates an Independent Manager profile from the Owner Dashboard.
     * Requires verification of the Owner Secret Code (Antar19930716).
     */
    suspend fun createManagerAccountByOwner(
        managerFullName: String,
        managerEmail: String = "",
        managerPhone: String,
        ownerSecretCode: String,
        permissions: WorkerPermissions,
        branchName: String = "فرع مستقل",
        managerSecretPin: String = "1234"
    ): Result<StaffAccount> {
        if (!verifyOwnerSecretCode(ownerSecretCode)) {
            return Result.failure(
                IllegalArgumentException("الرمز السري للمالك (Deffas antar) غير صحيح! لا يمكن إنشاء ملف مدير مستقل.")
            )
        }
        val cleanName = managerFullName.trim().take(100)
        val cleanPhone = managerPhone.trim().take(30)
        val cleanBranch = branchName.trim().ifBlank { "فرع مستقل" }.take(100)
        val cleanPin = managerSecretPin.trim().ifBlank { "1234" }.take(64)

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("يرجى إدخال الاسم الكامل للمدير المستقل"))
        }
        val cleanEmail = normalizeStaffInternalEmail(managerEmail.ifBlank { cleanName }, "mgr")
        val docId = managerPreProvisionDocId(cleanName)

        // Ensure Firebase cloud channel is connected if appContext is available
        try {
            val ctx = appContext
            if (auth.currentUser == null && ctx != null) {
                ensureFirebaseCloudChannelConnected(ctx)
            }
            if (auth.currentUser != null) {
                runCatching { bootstrapOwnerAccountIfNeeded() }
            }
        } catch (_: Exception) {
        }

        val ownerUid = auth.currentUser?.uid ?: "owner_deffas_antar"
        val createdAccount = StaffAccount(
            userId = docId,
            storeId = DEFAULT_STORE_ID,
            email = cleanEmail,
            fullName = cleanName,
            phone = cleanPhone,
            requestedRole = "manager",
            role = "manager",
            approvalStatus = StaffApprovalStatus.APPROVED,
            approvedByUid = ownerUid,
            approvedByName = OWNER_FULL_NAME,
            branchName = cleanBranch,
            secretPin = cleanPin,
            canMakeSales = permissions.canMakeSales,
            canManageInventory = permissions.canManageInventory,
            canEditPrices = permissions.canEditPrices,
            canViewReports = permissions.canViewReports,
            canManageDebtsAndCustomers = permissions.canManageDebtsAndCustomers,
            canRefundOrVoid = permissions.canRefundOrVoid,
            canApplyDiscounts = permissions.canApplyDiscounts,
            canViewCostPrices = permissions.canViewCostPrices,
            canExportData = permissions.canExportData,
            canManagePurchaseInvoices = permissions.canManagePurchaseInvoices,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        appContext?.let { ctx ->
            saveAccountToLocalRegistry(ctx, createdAccount)
        }

        val staffRef = db.collection(COLLECTION_STAFF).document(docId)
        val payload = mapOf(
            "userId" to docId,
            "storeId" to DEFAULT_STORE_ID,
            "email" to cleanEmail,
            "fullName" to cleanName,
            "phone" to cleanPhone,
            "requestedRole" to "manager",
            "role" to "manager",
            "approvalStatus" to StaffApprovalStatus.APPROVED,
            "approvedByUid" to ownerUid.take(128),
            "approvedByName" to OWNER_FULL_NAME,
            "branchName" to cleanBranch,
            "secretPin" to cleanPin,
            "canMakeSales" to permissions.canMakeSales,
            "canManageInventory" to permissions.canManageInventory,
            "canEditPrices" to permissions.canEditPrices,
            "canViewReports" to permissions.canViewReports,
            "canManageDebtsAndCustomers" to permissions.canManageDebtsAndCustomers,
            "canRefundOrVoid" to permissions.canRefundOrVoid,
            "canApplyDiscounts" to permissions.canApplyDiscounts,
            "canViewCostPrices" to permissions.canViewCostPrices,
            "canExportData" to permissions.canExportData,
            "canManagePurchaseInvoices" to permissions.canManagePurchaseInvoices,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            staffRef.set(payload).await()
            // Also write a mirror doc under mgr_<cleanEmail> if managerEmail was provided, keeping userId = docId so both point to the same canonical inventory!
            val emailDocId = managerPreProvisionDocId(cleanEmail)
            if (emailDocId != docId) {
                runCatching {
                    db.collection(COLLECTION_STAFF).document(emailDocId).set(payload).await()
                }
            }
            Result.success(createdAccount)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, staffRef.path)
            Log.w("StaffRepo", "Failed to upload manager account to Firestore: ${e.message}", e)
            Result.success(createdAccount)
        }
    }

    /**
     * Independent Manager (or Owner) Action: Directly creates an approved Worker account
     * affiliated with this Manager's independent profile (`parentManagerUid = manager.userId`).
     */
    suspend fun createWorkerAccountByManager(
        managerAccount: StaffAccount,
        workerFullName: String,
        workerEmail: String = "",
        workerPhone: String,
        workerSecretPin: String,
        permissions: WorkerPermissions,
        syncMode: String = "pro"
    ): Result<StaffAccount> {
        if (!managerAccount.isManagerOrAdmin) {
            return Result.failure(
                IllegalStateException("فقط المدير المستقل المعتمد أو مالك المتجر يمكنه إنشاء حسابات عمال تابعين له.")
            )
        }
        if (!managerAccount.isSyncEnabled && !managerAccount.isOwner) {
            return Result.failure(
                IllegalStateException("حسابات المدير في وضع Just Offline / Lite لا تتيح إنشاء عمال تابعين. تتطلب الترقية إلى Pro mode.")
            )
        }
        val cleanName = workerFullName.trim().take(100)
        val cleanPhone = workerPhone.trim().take(30)
        val cleanPin = workerSecretPin.trim().ifBlank { "1234" }.take(64)
        val safeSyncMode = if (syncMode in listOf("pro", "offline", "lite")) syncMode else "pro"

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("يرجى إدخال الاسم الكامل للعامل التابع"))
        }
        if (cleanPin.length < 4) {
            return Result.failure(IllegalArgumentException("يرجى تعيين رمز سري للعامل لا يقل عن 4 خانات"))
        }
        val cleanEmail = normalizeStaffInternalEmail(workerEmail.ifBlank { cleanName }, "wrk")
        if (isOwnerEmail(cleanEmail) || isOwnerIdentity(cleanName)) {
            return Result.failure(IllegalArgumentException("لا يمكن استخدام اسم المالك الأساسي لحساب عامل"))
        }

        try {
            val ctx = appContext
            if (auth.currentUser == null && ctx != null) {
                ensureFirebaseCloudChannelConnected(ctx)
            }
        } catch (_: Exception) {
        }

        val docId = workerPreProvisionDocId(if (workerEmail.contains("@")) cleanEmail else cleanName)
        val effectiveManagerName = managerAccount.fullName.ifBlank { "مدير مستقل" }.take(100)
        val effectiveManagerUid = when {
            managerAccount.isOwner -> "owner_deffas_antar"
            effectiveManagerName.isNotBlank() && effectiveManagerName != "مدير مستقل" ->
                managerPreProvisionDocId(effectiveManagerName)
            managerAccount.userId.isNotBlank() -> managerAccount.userId
            else -> "manager_independent"
        }
        val effectiveManagerEmail = managerAccount.email.take(150)
        val effectiveBranch = managerAccount.branchName.ifBlank { "فرع $effectiveManagerName" }.take(100)

        val createdWorker = StaffAccount(
            userId = docId,
            storeId = DEFAULT_STORE_ID,
            email = cleanEmail,
            fullName = cleanName,
            phone = cleanPhone,
            requestedRole = "worker",
            role = "worker",
            approvalStatus = StaffApprovalStatus.APPROVED,
            approvedByUid = effectiveManagerUid,
            approvedByName = effectiveManagerName,
            parentManagerUid = effectiveManagerUid,
            parentManagerName = effectiveManagerName,
            parentManagerEmail = effectiveManagerEmail,
            branchName = effectiveBranch,
            secretPin = cleanPin,
            canMakeSales = permissions.canMakeSales,
            canManageInventory = permissions.canManageInventory,
            canEditPrices = permissions.canEditPrices,
            canViewReports = permissions.canViewReports,
            canManageDebtsAndCustomers = permissions.canManageDebtsAndCustomers,
            canRefundOrVoid = permissions.canRefundOrVoid,
            canApplyDiscounts = permissions.canApplyDiscounts,
            canViewCostPrices = permissions.canViewCostPrices,
            canExportData = permissions.canExportData,
            canManagePurchaseInvoices = permissions.canManagePurchaseInvoices,
            syncMode = safeSyncMode,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        val staffRef = db.collection(COLLECTION_STAFF).document(docId)
        val payload = mapOf(
            "userId" to docId,
            "storeId" to DEFAULT_STORE_ID,
            "email" to cleanEmail,
            "fullName" to cleanName,
            "phone" to cleanPhone,
            "requestedRole" to "worker",
            "role" to "worker",
            "approvalStatus" to StaffApprovalStatus.APPROVED,
            "approvedByUid" to effectiveManagerUid.take(128),
            "approvedByName" to effectiveManagerName,
            "parentManagerUid" to effectiveManagerUid.take(128),
            "parentManagerName" to effectiveManagerName,
            "parentManagerEmail" to effectiveManagerEmail,
            "branchName" to effectiveBranch,
            "secretPin" to cleanPin,
            "canMakeSales" to permissions.canMakeSales,
            "canManageInventory" to permissions.canManageInventory,
            "canEditPrices" to permissions.canEditPrices,
            "canViewReports" to permissions.canViewReports,
            "canManageDebtsAndCustomers" to permissions.canManageDebtsAndCustomers,
            "canRefundOrVoid" to permissions.canRefundOrVoid,
            "canApplyDiscounts" to permissions.canApplyDiscounts,
            "canViewCostPrices" to permissions.canViewCostPrices,
            "canExportData" to permissions.canExportData,
            "canManagePurchaseInvoices" to permissions.canManagePurchaseInvoices,
            "syncMode" to safeSyncMode,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            staffRef.set(payload).await()
            val nameDocId = workerPreProvisionDocId(cleanName)
            if (nameDocId != docId) {
                runCatching {
                    db.collection(COLLECTION_STAFF).document(nameDocId).set(payload).await()
                }
            }
            Result.success(createdWorker)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, staffRef.path)
            Log.w("StaffRepo", "Failed to upload worker account to Firestore: ${e.message}", e)
            Result.success(createdWorker)
        }
    }

    suspend fun updateOwnProfileDetails(
        fullName: String,
        phone: String,
        requestedRole: String = "worker"
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_STAFF).document(uid)
        val safeName = fullName.trim().ifBlank { "موظف المتجر" }.take(100)
        val safePhone = phone.trim().take(30)

        val updates = mapOf(
            "fullName" to safeName,
            "phone" to safePhone,
            "requestedRole" to "worker",
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun approveOrUpdateStaffAccount(
        targetAccount: StaffAccount,
        newApprovalStatus: String,
        newRole: String,
        permissions: WorkerPermissions,
        managerName: String,
        isCallerOwner: Boolean = false,
        ownerSecretCode: String = "",
        callerAccount: StaffAccount? = null,
        syncMode: String = "pro"
    ): Result<Unit> {
        val managerUid = when {
            isCallerOwner -> "owner_deffas_antar"
            callerAccount != null && callerAccount.fullName.isNotBlank() ->
                managerPreProvisionDocId(callerAccount.fullName)
            callerAccount != null && callerAccount.userId.isNotBlank() -> callerAccount.userId
            managerName.trim().isNotBlank() -> managerPreProvisionDocId(managerName.trim())
            else -> "owner_deffas_antar"
        }
        val involvesManagerRole = newRole in listOf("manager", "admin", "owner") ||
            targetAccount.role in listOf("manager", "admin", "owner")

        if (involvesManagerRole) {
            if (!isCallerOwner) {
                return Result.failure(
                    IllegalStateException("فقط مالك المتجر (Deffas antar) يمتلك صلاحية إنشاء أو اعتماد أو تعديل حسابات المدراء.")
                )
            }
            if (!verifyOwnerSecretCode(ownerSecretCode)) {
                return Result.failure(
                    IllegalArgumentException("يجب إدخال الرمز السري الصحيح للمالك (Deffas antar) لتأكيد العملية على حساب المدير.")
                )
            }
        }

        val safeStatus = if (newApprovalStatus in listOf(
                StaffApprovalStatus.PENDING,
                StaffApprovalStatus.APPROVED,
                StaffApprovalStatus.REJECTED,
                StaffApprovalStatus.SUSPENDED
            )
        ) newApprovalStatus else StaffApprovalStatus.PENDING

        val safeRole = if (isCallerOwner && newRole in listOf("worker", "manager", "admin", "owner")) {
            newRole
        } else {
            "worker"
        }
        val safeSyncMode = if (syncMode in listOf("pro", "offline", "lite")) syncMode else "pro"
        val isManagerRole = safeRole == "manager" || safeRole == "admin" || safeRole == "owner"

        val staffRef = db.collection(COLLECTION_STAFF).document(targetAccount.userId)
        val updates = mutableMapOf<String, Any>(
            "role" to safeRole,
            "requestedRole" to if (isCallerOwner) safeRole else "worker",
            "approvalStatus" to safeStatus,
            "approvedByUid" to managerUid.take(128),
            "approvedByName" to (if (isCallerOwner) OWNER_FULL_NAME else managerName.trim().ifBlank { "مدير الفرع" }).take(100),
            "canMakeSales" to permissions.canMakeSales,
            "canManageInventory" to permissions.canManageInventory,
            "canEditPrices" to permissions.canEditPrices,
            "canViewReports" to permissions.canViewReports,
            "canManageDebtsAndCustomers" to permissions.canManageDebtsAndCustomers,
            "canRefundOrVoid" to permissions.canRefundOrVoid,
            "canApplyDiscounts" to permissions.canApplyDiscounts,
            "canViewCostPrices" to permissions.canViewCostPrices,
            "canExportData" to permissions.canExportData,
            "canManagePurchaseInvoices" to permissions.canManagePurchaseInvoices,
            "syncMode" to safeSyncMode,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!isCallerOwner && safeRole == "worker") {
            updates["parentManagerUid"] = managerUid.take(128)
            updates["parentManagerName"] = managerName.trim().ifBlank { "مدير الفرع" }.take(100)
            if (callerAccount != null && callerAccount.email.isNotBlank()) {
                updates["parentManagerEmail"] = callerAccount.email.take(150)
            }
        }

        return try {
            staffRef.update(updates).await()

            if (auth.currentUser != null && isCallerOwner && !targetAccount.userId.startsWith("mgr_") && !targetAccount.userId.startsWith("wrk_")) {
                val adminRef = db.collection(COLLECTION_ADMINS).document(targetAccount.userId)
                if (isManagerRole && safeStatus == StaffApprovalStatus.APPROVED) {
                    val adminSnap = adminRef.get().await()
                    if (!adminSnap.exists()) {
                        val adminPayload = mapOf(
                            "userId" to targetAccount.userId,
                            "storeId" to DEFAULT_STORE_ID,
                            "email" to targetAccount.email.ifBlank { "manager@afstore.app" }.take(150),
                            "fullName" to targetAccount.fullName.ifBlank { "مدير الفرع" }.take(100),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        adminRef.set(adminPayload).await()
                    }
                } else if (targetAccount.userId != managerUid && !isOwnerEmail(targetAccount.email)) {
                    val adminSnap = adminRef.get().await()
                    if (adminSnap.exists()) {
                        adminRef.delete().await()
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, staffRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStaffAccount(targetUserId: String): Result<Unit> {
        val staffRef = db.collection(COLLECTION_STAFF).document(targetUserId)
        return try {
            staffRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, staffRef.path)
            Result.failure(e)
        }
    }

    /**
     * Submit an upgrade request from Lite to Pro for an independent manager.
     * Contains the manager's required phone number for owner follow-up.
     */
    suspend fun requestProUpgrade(
        managerAccount: StaffAccount,
        phone: String,
        note: String = ""
    ): Result<ProUpgradeRequest> {
        val cleanPhone = phone.trim().take(30)
        if (cleanPhone.length < 8) {
            return Result.failure(IllegalArgumentException("يرجى إدخال رقم هاتف صحيح ليتواصل معك المالك لتأكيد الترقية."))
        }
        val targetUid = managerAccount.userId.ifBlank { auth.currentUser?.uid ?: "mgr_${System.currentTimeMillis()}" }
        val req = ProUpgradeRequest(
            requestId = targetUid,
            managerUid = targetUid,
            managerName = managerAccount.fullName.ifBlank { "مدير فرع" },
            managerEmail = managerAccount.email,
            phone = cleanPhone,
            branchName = managerAccount.branchName.ifBlank { "AF store" },
            status = "PENDING",
            note = note.trim().take(300),
            requestedAt = Timestamp.now()
        )
        val docRef = db.collection(COLLECTION_PRO_UPGRADE_REQUESTS).document(targetUid)
        return try {
            docRef.set(req.toFirestorePayload()).await()
            Result.success(req)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Log.w("StaffRepo", "Failed to submit pro upgrade request: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observe the upgrade request status for a specific manager.
     */
    fun observeMyProUpgradeRequest(managerUid: String): Flow<ProUpgradeRequest?> = callbackFlow {
        if (managerUid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = db.collection(COLLECTION_PRO_UPGRADE_REQUESTS).document(managerUid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("StaffRepo", "Error listening to pro upgrade request: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                trySend(null)
            } else {
                trySend(ProUpgradeRequest.fromSnapshot(snapshot))
            }
        }
        awaitClose { listener.remove() }
    }.catch {
        emit(null)
    }

    /**
     * Observe all pending upgrade requests for the Store Owner.
     */
    fun observeAllProUpgradeRequests(): Flow<List<ProUpgradeRequest>> = callbackFlow {
        val col = db.collection(COLLECTION_PRO_UPGRADE_REQUESTS)
        val listener = col.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("StaffRepo", "Error listening to all pro upgrade requests: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot == null || snapshot.isEmpty) {
                trySend(emptyList())
            } else {
                val list = snapshot.documents.mapNotNull { ProUpgradeRequest.fromSnapshot(it) }
                    .sortedByDescending { it.requestedAt?.seconds ?: 0L }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }.catch {
        emit(emptyList())
    }

    /**
     * Store Owner reviews and decides (Approve / Reject) a manager's Pro upgrade request.
     */
    suspend fun reviewProUpgradeRequest(
        request: ProUpgradeRequest,
        approve: Boolean,
        ownerSecretCode: String = "",
        reviewerName: String = OWNER_FULL_NAME
    ): Result<Unit> {
        val reqRef = db.collection(COLLECTION_PRO_UPGRADE_REQUESTS).document(request.requestId)
        val staffRef = db.collection(COLLECTION_STAFF).document(request.managerUid)

        val newStatus = if (approve) "APPROVED" else "REJECTED"
        val reqUpdates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "reviewedAt" to FieldValue.serverTimestamp(),
            "reviewedByUid" to (auth.currentUser?.uid ?: "owner_deffas_antar")
        )

        return try {
            reqRef.update(reqUpdates).await()

            if (approve) {
                // Update manager staff account syncMode to "pro"
                val staffUpdates = mapOf(
                    "syncMode" to "pro",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                staffRef.update(staffUpdates).await()

                // Also update local registry if present
                appContext?.let { ctx ->
                    val local = loadLocalAccountsRegistry(ctx)
                    val match = local.find { it.userId == request.managerUid }
                    if (match != null) {
                        saveAccountToLocalRegistry(ctx, match.copy(syncMode = "pro"))
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }
}
