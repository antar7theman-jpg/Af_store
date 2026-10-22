package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableIntStateOf

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.example.data.datastore.UserSessionDataStore
import com.example.data.datastore.SavedUserCredentials
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AFStoreHeaderLogo
import com.example.R
import com.example.data.firebase.BackgroundStaffSyncService
import com.example.data.firebase.StaffAccount
import com.example.data.firebase.StaffAccountRepository
import com.example.data.firebase.StaffApprovalStatus
import com.example.data.models.WorkerPermissions
import com.example.ui.theme.LocalAFStoreSemanticColors
import com.example.utils.NetworkConnectivityMonitor
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

// 1. Silent Auto-Sign-In on App Startup (One option per GetCredentialRequest)
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val clientId = if (resId != 0) {
        try { context.getString(resId) } catch (e: Exception) { "" }
    } else {
        ""
    }
    if (clientId.isBlank()) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val activityContext = context.findActivity() ?: context
    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

// 2. Interactive Google Sign-In via Credential Manager (One option per GetCredentialRequest)
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val clientId = if (resId != 0) {
        try { context.getString(resId) } catch (e: Exception) { "" }
    } else {
        ""
    }
    if (clientId.isBlank()) {
        onAuthError("إعداد Google Sign-In غير مكتمل: لم يتم العثور على default_web_client_id")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("تعذر فتح نافذة تسجيل الدخول بحساب Google")
        return
    }

    scope.launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("نوع بيانات الاعتماد غير متوقع")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "تعذر تسجيل الدخول بحساب Google")
        }
    }
}

// 3. Sign-Out Flow (Must sign out of Firebase AND clear Credential Manager state)
fun signOutFromFirebase(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}



@Composable
fun FirebaseAuthAndApprovalGate(
    storeName: String,
    onApprovedAccountReady: (StaffAccount) -> Unit,
    onOwnerCreatedManager: (StaffAccount, String) -> Unit = { _, _ -> },
    approvedContent: @Composable (
        myAccount: StaffAccount,
        allStaffAccounts: List<StaffAccount>,
        pendingCount: Int,
        onOpenStaffManagement: () -> Unit,
        onSignOut: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("af_store_prefs", Context.MODE_PRIVATE) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val userSessionDataStore = remember(context) { UserSessionDataStore(context.applicationContext) }
    val savedDataStoreCreds by userSessionDataStore.savedCredentialsFlow.collectAsStateWithLifecycle(initialValue = null)

    var savedPersistentAccount by remember {
        mutableStateOf(StaffAccountRepository.loadPersistentStaffSession(context))
    }
    var isDirectOwnerSessionActive by rememberSaveable {
        mutableStateOf(
            prefs.getBoolean("owner_direct_session_active", false) ||
                StaffAccountRepository.isDirectOwnerSessionSaved(context)
        )
    }
    var keepSessionOpen by rememberSaveable {
        mutableStateOf(StaffAccountRepository.isKeepSessionOpenPreferred(context))
    }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var authSuccessBanner by remember { mutableStateOf<String?>(null) }
    var attemptedSilentSignIn by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedDataStoreCreds) {
        val creds = savedDataStoreCreds
        if (creds != null && creds.isSessionActive && creds.rememberMe && savedPersistentAccount == null && !isDirectOwnerSessionActive) {
            val registry = StaffAccountRepository.loadLocalAccountsRegistry(context)
            val matched = registry.firstOrNull { acc ->
                (creds.userId.isNotBlank() && acc.userId == creds.userId) ||
                    (creds.identifier.isNotBlank() && (acc.email.equals(creds.identifier, ignoreCase = true) || acc.fullName.equals(creds.identifier, ignoreCase = true)))
            } ?: if (creds.isDirectOwner || StaffAccountRepository.isOwnerIdentity(creds.identifier)) {
                StaffAccount.createVerifiedOwnerAccount()
            } else null

            if (matched != null) {
                savedPersistentAccount = matched
                if (matched.isOwner) {
                    isDirectOwnerSessionActive = true
                }
            }
        }
    }

    val handleManualSignOut: () -> Unit = {
        prefs.edit().putBoolean("owner_direct_session_active", false).apply()
        StaffAccountRepository.clearPersistentStaffSession(context)
        savedPersistentAccount = null
        isDirectOwnerSessionActive = false
        signOutFromFirebase(
            context = context,
            credentialManager = credentialManager,
            onSignOutComplete = {},
            scope = scope
        )
    }

    LaunchedEffect(Unit) {
        if (!attemptedSilentSignIn &&
            auth.currentUser == null &&
            !isDirectOwnerSessionActive &&
            savedPersistentAccount == null
        ) {
            attemptedSilentSignIn = true
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { authErrorMessage = null },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val activeFirebaseUser = currentUser
    val cachedApprovedAccount = savedPersistentAccount?.takeIf { it.isApproved }

    when {
        activeFirebaseUser != null -> {
            AuthenticatedStaffSessionGate(
                firebaseUser = activeFirebaseUser,
                initialCachedAccount = cachedApprovedAccount,
                storeName = storeName,
                onApprovedAccountReady = { approvedAcc ->
                    StaffAccountRepository.saveAccountToLocalRegistry(context, approvedAcc)
                    if (keepSessionOpen) {
                        StaffAccountRepository.savePersistentStaffSession(
                            context = context,
                            account = approvedAcc,
                            isDirectOwnerSession = false
                        )
                        savedPersistentAccount = approvedAcc
                    }
                    onApprovedAccountReady(approvedAcc)
                },
                onOwnerCreatedManager = onOwnerCreatedManager,
                onSignOut = handleManualSignOut,
                approvedContent = approvedContent
            )
        }

        isDirectOwnerSessionActive -> {
            DirectVerifiedOwnerSessionGate(
                onApprovedAccountReady = { ownerAcc ->
                    if (keepSessionOpen) {
                        StaffAccountRepository.savePersistentStaffSession(
                            context = context,
                            account = ownerAcc,
                            isDirectOwnerSession = true
                        )
                        savedPersistentAccount = ownerAcc
                    }
                    onApprovedAccountReady(ownerAcc)
                },
                onOwnerCreatedManager = onOwnerCreatedManager,
                onSignOut = handleManualSignOut,
                approvedContent = approvedContent
            )
        }

        cachedApprovedAccount != null -> {
            SavedPersistentStaffSessionGate(
                savedAccount = cachedApprovedAccount,
                onApprovedAccountReady = onApprovedAccountReady,
                onOwnerCreatedManager = onOwnerCreatedManager,
                onSignOut = handleManualSignOut,
                approvedContent = approvedContent
            )
        }

        else -> {
            FirebaseStaffSignInScreen(
                storeName = storeName,
                credentialManager = credentialManager,
                keepSessionOpen = keepSessionOpen,
                onKeepSessionOpenChange = { enabled ->
                    keepSessionOpen = enabled
                    StaffAccountRepository.setKeepSessionOpenPreferred(context, enabled)
                },
                errorMessage = authErrorMessage,
                successBanner = authSuccessBanner,
                onClearError = {
                    authErrorMessage = null
                    authSuccessBanner = null
                },
                onAuthSuccess = { authErrorMessage = null },
                onAuthError = { err -> authErrorMessage = err },
                onDirectStaffVerified = { verifiedAccount, identifier, secret ->
                    StaffAccountRepository.saveAccountToLocalRegistry(context, verifiedAccount)
                    authErrorMessage = null
                    authSuccessBanner = null
                    if (verifiedAccount.isOwner) {
                        if (keepSessionOpen) {
                            prefs.edit().putBoolean("owner_direct_session_active", true).apply()
                            StaffAccountRepository.savePersistentStaffSession(
                                context = context,
                                account = verifiedAccount,
                                isDirectOwnerSession = true,
                                savedIdentifier = identifier,
                                savedSecret = secret
                            )
                        }
                        savedPersistentAccount = verifiedAccount
                        isDirectOwnerSessionActive = true
                    } else {
                        if (keepSessionOpen) {
                            StaffAccountRepository.savePersistentStaffSession(
                                context = context,
                                account = verifiedAccount,
                                isDirectOwnerSession = false,
                                savedIdentifier = identifier,
                                savedSecret = secret
                            )
                        }
                        savedPersistentAccount = verifiedAccount
                    }
                },
                onRegisterIndependentManager = { createdManager, pin ->
                    if (createdManager.isApproved) {
                        onOwnerCreatedManager(createdManager, pin)
                        if (keepSessionOpen) {
                            StaffAccountRepository.savePersistentStaffSession(
                                context = context,
                                account = createdManager,
                                isDirectOwnerSession = false
                            )
                        }
                        savedPersistentAccount = createdManager
                    } else {
                        authSuccessBanner = "تم تسجيل ملف المدير المستقل «${createdManager.fullName}» (${createdManager.branchName}) وبانتظار موافقة وتفعيل المالك Deffas antar ✓"
                    }
                }
            )
        }
    }
}

@Composable
private fun SavedPersistentStaffSessionGate(
    savedAccount: StaffAccount,
    onApprovedAccountReady: (StaffAccount) -> Unit,
    onOwnerCreatedManager: (StaffAccount, String) -> Unit,
    onSignOut: () -> Unit,
    approvedContent: @Composable (
        myAccount: StaffAccount,
        allStaffAccounts: List<StaffAccount>,
        pendingCount: Int,
        onOpenStaffManagement: () -> Unit,
        onSignOut: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val semantic = LocalAFStoreSemanticColors.current
    val repository = remember(context) { StaffAccountRepository(context.applicationContext) }
    val syncService = remember(context, coroutineScope) {
        BackgroundStaffSyncService(context.applicationContext, coroutineScope)
    }
    var concurrentTerminatedDeviceName by rememberSaveable { mutableStateOf<String?>(null) }
    var showConcurrentLoginDialog by rememberSaveable { mutableStateOf(false) }

    var registrySnapshot by remember(savedAccount.userId) {
        mutableStateOf(StaffAccountRepository.loadLocalAccountsRegistry(context))
    }
    var cloudStaffAccounts by remember { mutableStateOf<List<StaffAccount>>(emptyList()) }
    var showStaffManagementModal by rememberSaveable { mutableStateOf(false) }

    val refreshLocalRegistry: () -> Unit = {
        registrySnapshot = StaffAccountRepository.loadLocalAccountsRegistry(context)
    }

    LaunchedEffect(savedAccount) {
        onApprovedAccountReady(savedAccount)
    }

    LaunchedEffect(savedAccount.isOwner) {
        if (savedAccount.isOwner) {
            try {
                repository.observeAllStoreStaffAccounts()
                    .catch { e -> Log.w("StaffAuthGate", "Error observing store staff accounts", e) }
                    .collect { list ->
                        cloudStaffAccounts = list
                        list.forEach { remote ->
                            StaffAccountRepository.saveAccountToLocalRegistry(context, remote)
                        }
                        registrySnapshot = StaffAccountRepository.loadLocalAccountsRegistry(context)
                    }
            } catch (e: Throwable) {
                Log.w("StaffAuthGate", "Error in observeAllStoreStaffAccounts for owner: ${e.message}")
            }
        }
    }

    DisposableEffect(savedAccount.userId, savedAccount.email, savedAccount.isOwner) {
        syncService.startSync(savedAccount) { event ->
            when (event) {
                is BackgroundStaffSyncService.SyncEvent.ConcurrentLoginDetected -> {
                    concurrentTerminatedDeviceName = event.newDeviceName
                    showConcurrentLoginDialog = true
                }
                is BackgroundStaffSyncService.SyncEvent.AccountDeactivated -> {
                    onSignOut()
                }
                is BackgroundStaffSyncService.SyncEvent.ServerAuthorizationChanged -> {
                    StaffAccountRepository.saveAccountToLocalRegistry(context, event.updatedAccount)
                    refreshLocalRegistry()
                    onApprovedAccountReady(event.updatedAccount)
                }
                is BackgroundStaffSyncService.SyncEvent.Synchronized -> {
                    StaffAccountRepository.saveAccountToLocalRegistry(context, event.account)
                    refreshLocalRegistry()
                    onApprovedAccountReady(event.account)
                }
            }
        }
        onDispose {
            syncService.stopSync()
        }
    }

    val visibleAccounts = remember(registrySnapshot, cloudStaffAccounts, savedAccount) {
        val merged = linkedMapOf<String, StaffAccount>()
        registrySnapshot.forEach { acc ->
            val key = acc.email.lowercase().ifBlank { acc.userId }
            merged[key] = acc
        }
        cloudStaffAccounts.forEach { acc ->
            val key = acc.email.lowercase().ifBlank { acc.userId }
            val existing = merged[key]
            merged[key] = if (existing != null && acc.secretPin.isBlank()) {
                acc.copy(secretPin = existing.secretPin)
            } else {
                acc
            }
        }
        val allList = merged.values.toList()
        when {
            savedAccount.isOwner -> allList
            savedAccount.isRegularManager -> allList.filter {
                it.userId == savedAccount.userId || it.isAffiliatedWithManager(savedAccount)
            }
            else -> listOf(savedAccount)
        }
    }

    val pendingCount = visibleAccounts.count { it.approvalStatus == StaffApprovalStatus.PENDING || it.isPending }

    approvedContent(
        savedAccount,
        visibleAccounts,
        pendingCount,
        {
            refreshLocalRegistry()
            showStaffManagementModal = true
        },
        onSignOut
    )

    if (showStaffManagementModal) {
        StaffAccountsManagementModal(
            myAccount = savedAccount,
            allAccounts = visibleAccounts,
            repository = repository,
            onOwnerCreatedManager = { newAcc, pin ->
                StaffAccountRepository.saveAccountToLocalRegistry(context, newAcc)
                refreshLocalRegistry()
                onOwnerCreatedManager(newAcc, pin)
            },
            onLocalRegistryUpdated = refreshLocalRegistry,
            onDismiss = { showStaffManagementModal = false },
            onSignOut = {
                showStaffManagementModal = false
                onSignOut()
            }
        )
    }

    if (showConcurrentLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                showConcurrentLoginDialog = false
                onSignOut()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = semantic.dangerRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "تم إنهاء الجلسة • تسجيل دخول متزامن",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = semantic.dangerRed
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "تم تسجيل الدخول إلى حساب «${savedAccount.fullName}» من هاتف آخر (${concurrentTerminatedDeviceName ?: "جهاز آخر"}).",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "وفقاً لسياسة الأمان ومنع تسجيل الدخول المتزامن، تم إنهاء هذه الجلسة تلقائياً وإعطاء الأولوية للجهاز الأحدث.\n\nيمكنك إعادة تسجيل الدخول في أي وقت لنقل الجلسة واستئناف العمل من هذا الجهاز.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConcurrentLoginDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = semantic.dangerRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("العودة لشاشة الدخول", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun DirectVerifiedOwnerSessionGate(
    onApprovedAccountReady: (StaffAccount) -> Unit,
    onOwnerCreatedManager: (StaffAccount, String) -> Unit,
    onSignOut: () -> Unit,
    approvedContent: @Composable (
        myAccount: StaffAccount,
        allStaffAccounts: List<StaffAccount>,
        pendingCount: Int,
        onOpenStaffManagement: () -> Unit,
        onSignOut: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { StaffAccountRepository(context.applicationContext) }
    val ownerAccount = remember { StaffAccount.createVerifiedOwnerAccount() }
    var registrySnapshot by remember {
        mutableStateOf(StaffAccountRepository.loadLocalAccountsRegistry(context))
    }
    var cloudStaffAccounts by remember { mutableStateOf<List<StaffAccount>>(emptyList()) }
    var showStaffManagementModal by rememberSaveable { mutableStateOf(false) }

    val refreshLocalRegistry: () -> Unit = {
        registrySnapshot = StaffAccountRepository.loadLocalAccountsRegistry(context)
    }

    LaunchedEffect(ownerAccount) {
        StaffAccountRepository.saveAccountToLocalRegistry(context, ownerAccount)
        refreshLocalRegistry()
        onApprovedAccountReady(ownerAccount)
    }

    LaunchedEffect(Unit) {
        try {
            repository.observeAllStoreStaffAccounts()
                .catch { e -> Log.w("StaffAuthGate", "Error observing store staff accounts", e) }
                .collect { list ->
                    cloudStaffAccounts = list
                    list.forEach { remote ->
                        StaffAccountRepository.saveAccountToLocalRegistry(context, remote)
                    }
                    registrySnapshot = StaffAccountRepository.loadLocalAccountsRegistry(context)
                }
        } catch (e: Throwable) {
            Log.w("StaffAuthGate", "Error in observeAllStoreStaffAccounts collection: ${e.message}")
        }
    }

    val mergedAccounts = remember(registrySnapshot, cloudStaffAccounts) {
        val merged = linkedMapOf<String, StaffAccount>()
        registrySnapshot.forEach { acc ->
            val key = acc.email.lowercase().ifBlank { acc.userId }
            merged[key] = acc
        }
        cloudStaffAccounts.forEach { acc ->
            val key = acc.email.lowercase().ifBlank { acc.userId }
            val existing = merged[key]
            merged[key] = if (existing != null && acc.secretPin.isBlank()) {
                acc.copy(secretPin = existing.secretPin)
            } else {
                acc
            }
        }
        merged.values.toList()
    }

    val pendingCount = mergedAccounts.count { it.approvalStatus == StaffApprovalStatus.PENDING || it.isPending }

    approvedContent(
        ownerAccount,
        mergedAccounts,
        pendingCount,
        {
            refreshLocalRegistry()
            showStaffManagementModal = true
        },
        onSignOut
    )

    if (showStaffManagementModal) {
        StaffAccountsManagementModal(
            myAccount = ownerAccount,
            allAccounts = mergedAccounts,
            repository = repository,
            onOwnerCreatedManager = { newMgr, pin ->
                StaffAccountRepository.saveAccountToLocalRegistry(context, newMgr)
                refreshLocalRegistry()
                onOwnerCreatedManager(newMgr, pin)
            },
            onLocalRegistryUpdated = refreshLocalRegistry,
            onDismiss = { showStaffManagementModal = false },
            onSignOut = {
                showStaffManagementModal = false
                onSignOut()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirebaseStaffSignInScreen(
    storeName: String,
    credentialManager: CredentialManager,
    keepSessionOpen: Boolean,
    onKeepSessionOpenChange: (Boolean) -> Unit,
    errorMessage: String?,
    successBanner: String? = null,
    onClearError: () -> Unit,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onDirectStaffVerified: (StaffAccount, String, String) -> Unit,
    onRegisterIndependentManager: (StaffAccount, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val semantic = LocalAFStoreSemanticColors.current
    val networkMonitor = remember { NetworkConnectivityMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle()

    var loginIdentifierInput by rememberSaveable { mutableStateOf("") }
    var loginSecretInput by rememberSaveable { mutableStateOf("") }
    var showSecretPassword by rememberSaveable { mutableStateOf(false) }
    var showRegisterManagerDialog by rememberSaveable { mutableStateOf(false) }
    var showProTrialRegisterScreen by rememberSaveable { mutableStateOf(false) }
    var showLiteRegisterScreen by rememberSaveable { mutableStateOf(false) }
    var showLoginFieldsInfoDialog by rememberSaveable { mutableStateOf(false) }
    var isDirectAuthenticating by remember { mutableStateOf(false) }
    var isWelcomeMode by rememberSaveable { mutableStateOf(true) }
    var localSuccessBanner by rememberSaveable { mutableStateOf<String?>(null) }
    val activeSuccessBanner = localSuccessBanner ?: successBanner
    var terminatedSessionNotice by remember {
        mutableStateOf(StaffAccountRepository.getAndClearTerminatedSessionReason(context))
    }

    if (showLiteRegisterScreen) {
        LiteAccountRegistrationScreen(
            onBackToLogin = {
                showLiteRegisterScreen = false
                isWelcomeMode = true
            },
            onAccountCreatedAndLoggedIn = { createdAcc ->
                showLiteRegisterScreen = false
                isWelcomeMode = true
                onClearError()
                onDirectStaffVerified(createdAcc, createdAcc.email, createdAcc.secretPin)
            }
        )
        return
    }

    if (showProTrialRegisterScreen) {
        ProTrialRegistrationScreen(
            onBackToLogin = {
                showProTrialRegisterScreen = false
                isWelcomeMode = true
            },
            onRegistrationSubmitted = { submittedAcc ->
                showProTrialRegisterScreen = false
                isWelcomeMode = true
                onClearError()
                localSuccessBanner = "تم إرسال طلب حساب Pro التجريبي (7 أيام) للمدير «${submittedAcc.fullName}» بنجاح! تم إشعار المالك Deffas Antar لاتخاذ قرار القبول أو الرفض ⏳"
            }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = if (semantic.isDark) Color(0xFF0B1120) else Color(0xFFF1F5F9)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isWelcomeMode) {
                // Splash Welcome Screen
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (semantic.isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = if (semantic.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .testTag("firebase_welcome_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF111828),
                            border = BorderStroke(
                                width = 1.8.dp,
                                color = if (semantic.isDark) Color(0xFF18E1F7) else Color(0xFF0284C7)
                            ),
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.af_store_logo),
                                    contentDescription = "شعار Af store الرسمي",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Af store",
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = if (semantic.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "نظام إدارة المبيعات والمخزون ونقاط البيع والفواتير",
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.5.sp,
                                color = if (semantic.isDark) Color(0xFF94A3B8) else Color(0xFF334155),
                                textAlign = TextAlign.Center
                            )
                        }

                        HorizontalDivider(
                            color = if (semantic.isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        if (!activeSuccessBanner.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF059669).copy(alpha = 0.16f),
                                border = BorderStroke(1.2.dp, Color(0xFF059669)),
                                modifier = Modifier.fillMaxWidth().testTag("welcome_success_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = activeSuccessBanner.orEmpty(),
                                        color = Color(0xFF059669),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (semantic.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (semantic.isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "مرحباً بك في منظومة المتجر",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (semantic.isDark) Color.White else Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "يرجى تحديد الإجراء المناسب للمتابعة:",
                                    fontSize = 13.sp,
                                    color = if (semantic.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Blue Connection / Login Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF2563EB),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isWelcomeMode = false }
                                .testTag("welcome_connection_button")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "تسجيل الدخول",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "تسجيل الدخول",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "إذا كان لديك حساب مسجل بالفعل",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }

                        // Amber / Golden 7-Day Pro Trial Account Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFD97706),
                            shadowElevation = 5.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClearError()
                                    showProTrialRegisterScreen = true
                                }
                                .testTag("welcome_pro_trial_button")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.22f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Pro Trial",
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "فتح حساب Pro تجريبي لمدة 7 أيام",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.5.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تجربة مجانية لكافة ميزات المزامنة السحابية وإدارة العمال",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }

                        // Emerald Free Lite / Offline Account Button (No owner approval required)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (semantic.isDark) Color(0xFF0D9488) else Color(0xFF059669),
                            shadowElevation = 5.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClearError()
                                    showLiteRegisterScreen = true
                                }
                                .testTag("welcome_lite_offline_button")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 15.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.22f),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = "Lite Offline Account",
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "فتح حساب Lite / offline مجاني",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.5.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تفعيل فوري مجاناً دون الحاجة لموافقة - إدارة المبيعات والفواتير محلياً",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.95f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Bottom Status Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (semantic.isDark) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFFF0FDF4),
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = if (semantic.isDark) Color(0xFF059669) else Color(0xFF10B981)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("welcome_sync_status_box")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AF store v2.0",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (semantic.isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        text = if (isOnline) "المزامنة السحابية متصلة" else "وضع العمل محلياً (غير متصل)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnline) {
                                            if (semantic.isDark) Color(0xFF34D399) else Color(0xFF047857)
                                        } else {
                                            if (semantic.isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                                        }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B))
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Redesigned Login Screen matching Image 2
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (semantic.isDark) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = if (semantic.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .testTag("firebase_sign_in_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top Bar: Back Action & Connection Status Pill Badge (Matching Image 2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Connection Badge (Matching green pill in Image 2)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isOnline) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B))
                                    )
                                    Text(
                                        text = if (isOnline) "Connection" else "غير متصل",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOnline) Color(0xFF047857) else Color(0xFFB45309)
                                    )
                                }
                            }

                            // Return to splash welcome
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isWelcomeMode = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("login_back_button")
                            ) {
                                Text(
                                    text = "رجوع للواجهة الافتتاحية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "الرجوع للواجهة الافتتاحية",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Logo Container
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF111828),
                            border = BorderStroke(
                                width = 2.dp,
                                color = Color(0xFF059669)
                            ),
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.af_store_logo),
                                    contentDescription = "شعار Af store",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(20.dp))
                                )
                            }
                        }

                        // Title & Subtitle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "تسجيل الدخول",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "أدخل اسم المستخدم أو البريد الإلكتروني والرقم السري",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Error / Success / Notice Banners
                        AnimatedVisibility(visible = terminatedSessionNotice != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = semantic.dangerContainer.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, semantic.dangerRed.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = semantic.dangerRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = terminatedSessionNotice.orEmpty(),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { terminatedSessionNotice = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = semantic.dangerRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = semantic.dangerRed.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, semantic.dangerRed.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        color = semantic.dangerRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = semantic.dangerRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = successBanner != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = semantic.profitGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = successBanner.orEmpty(),
                                        color = semantic.profitGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "إغلاق",
                                            tint = semantic.profitGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Identifier Input (Email or Username)
                        OutlinedTextField(
                            value = loginIdentifierInput,
                            onValueChange = { loginIdentifierInput = it },
                            label = { Text("البريد الإلكتروني أو اسم المستخدم") },
                            placeholder = { Text("example@store.com") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF059669)
                                )
                            },
                            trailingIcon = {
                                if (loginIdentifierInput.isNotEmpty()) {
                                    IconButton(onClick = { loginIdentifierInput = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "مسح",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_email_login_input")
                        )

                        // Password Input
                        OutlinedTextField(
                            value = loginSecretInput,
                            onValueChange = { loginSecretInput = it },
                            label = { Text("الرقم السري / كلمة المرور") },
                            placeholder = { Text("••••••••") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF059669)
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (loginSecretInput.isNotEmpty()) {
                                        IconButton(onClick = { loginSecretInput = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "مسح",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showSecretPassword = !showSecretPassword }) {
                                        Icon(
                                            imageVector = if (showSecretPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showSecretPassword) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (showSecretPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_secret_login_input")
                        )

                        // Remember Me Card (Mint Green Container matching Image 2)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "تذكرني • البقاء قيد تسجيل الدخول",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                Switch(
                                    checked = keepSessionOpen,
                                    onCheckedChange = onKeepSessionOpenChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF059669)
                                    ),
                                    modifier = Modifier.testTag("keep_session_open_switch")
                                )
                            }
                        }

                        // Primary Blue Login Button
                        Button(
                            onClick = {
                                isDirectAuthenticating = true
                                coroutineScope.launch {
                                    try {
                                        val result = StaffAccountRepository.authenticateStaffLoginWithCloud(
                                            context = context,
                                            emailOrUsername = loginIdentifierInput.trim(),
                                            secretPinOrCode = loginSecretInput.trim()
                                        )
                                        if (result.isSuccess) {
                                            onDirectStaffVerified(result.getOrThrow(), loginIdentifierInput.trim(), loginSecretInput.trim())
                                        } else {
                                            onAuthError(
                                                result.exceptionOrNull()?.localizedMessage
                                                    ?: "بيانات الدخول غير صحيحة!"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        onAuthError(e.localizedMessage ?: "حدث خطأ أثناء فحص الحساب سحابياً")
                                    } finally {
                                        isDirectAuthenticating = false
                                    }
                                }
                            },
                            enabled = !isDirectAuthenticating && loginIdentifierInput.isNotBlank() && loginSecretInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1D4ED8),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("owner_direct_login_btn")
                        ) {
                            if (isDirectAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري تسجيل الدخول...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تسجيل الدخول (Connection)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        

                        // Divider with text "أو عبر المزامنة السحابية"
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text(
                                text = " أو عبر المزامنة السحابية ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        // Google Sign-In Button for Managers & Staff
                        Button(
                            onClick = {
                                onClearError()
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = onAuthSuccess,
                                    onAuthError = onAuthError,
                                    onAuthCancelled = {},
                                    scope = coroutineScope
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (semantic.isDark) Color(0xFF0369A1) else Color(0xFF0284C7),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_signin_manager_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "تسجيل الدخول بحساب Google",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "تسجيل الدخول بحساب Google (المدير المستقل)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "المزامنة السحابية الفورية للمدير المعتمد من المالك",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )

                        // Register New Account Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ليس لديك حساب بعد؟ ",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    onClearError()
                                    showProTrialRegisterScreen = true
                                },
                                modifier = Modifier.testTag("login_open_pro_trial_link")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "فتح حساب Pro تجريبي (7 أيام)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    onClearError()
                                    showLiteRegisterScreen = true
                                },
                                modifier = Modifier.testTag("login_open_lite_account_link")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "أو فتح حساب Lite / offline مجاني فوري ✓",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLoginFieldsInfoDialog) {
        LoginFieldsExplanationDialog(
            onDismiss = { showLoginFieldsInfoDialog = false }
        )
    }

    if (showRegisterManagerDialog) {
        RegisterIndependentManagerFromLoginDialog(
            onDismiss = { showRegisterManagerDialog = false },
            onSubmit = { fullName, email, phone, branchName, managerPin, optionalOwnerSecret ->
                coroutineScope.launch {
                    val res = StaffAccountRepository.registerIndependentManagerWithCloudSync(
                        context = context,
                        fullName = fullName,
                        email = email,
                        phone = phone,
                        branchName = branchName,
                        managerSecretPin = managerPin,
                        optionalOwnerSecretCode = optionalOwnerSecret
                    )
                    if (res.isSuccess) {
                        val created = res.getOrThrow()
                        showRegisterManagerDialog = false
                        onRegisterIndependentManager(created, managerPin)
                    } else {
                        onAuthError(res.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء ملف المدير المستقل")
                    }
                }
            }
        )
    }
}

@Composable
private fun LoginFieldsExplanationDialog(
    onDismiss: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "دليل وإيضاح خانات تسجيل الدخول",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Identifier Field Explanation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "1. خانة الاسم أو البريد الإلكتروني",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "أدخل البريد الإلكتروني أو اسم المستخدم المسجل في النظام للمالك، أو المدراء والعمال المعتمدين.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                // 2. Secret / Password Field Explanation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "2. خانة الرقم السري / كلمة المرور",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "أدخل كلمة المرور أو رمز PIN السري المخصص لحسابك للدخول بأمان إلى لوحة التحكم.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                // 3. Remember Me Explanation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = semantic.profitContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = semantic.profitGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "3. خيار 'تذكرني والبقاء متصلاً'",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "عند تفعيل هذا الخيار، يتم حفظ بيانات الاعتماد بشكل آمن في DataStore والدخول المباشر للتطبيق عند الفتح في المرات القادمة دون الحاجة لإعادة الكتابة.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("فهمت ذلك", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterIndependentManagerFromLoginDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        fullName: String,
        email: String,
        phone: String,
        branchName: String,
        managerPin: String,
        optionalOwnerSecret: String
    ) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var fullName by rememberSaveable { mutableStateOf("") }
    var branchName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var managerPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var ownerSecret by rememberSaveable { mutableStateOf("") }
    var showPasswords by rememberSaveable { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(modifier = Modifier.size(width = 38.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF7C3AED),
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "فتح حساب جديد",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "إنشاء ملف مدير مستقل لمنظومة المتجر",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Info Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "يتيح لك ملف المدير المستقل إدارة المتجر، إعداد الفروع، وإضافة حسابات العمال والمبيعات برمز سري مستقل.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }

            HorizontalDivider()

            // 1. Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    localError = null
                },
                label = { Text("الاسم الكامل للمدير") },
                placeholder = { Text("مثال: عبد الرحيم أحمد") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (fullName.isNotEmpty()) {
                        IconButton(onClick = { fullName = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح الاسم", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ind_mgr_fullname_input")
            )

            // 2. Store / Branch Name
            OutlinedTextField(
                value = branchName,
                onValueChange = {
                    branchName = it
                    localError = null
                },
                label = { Text("اسم المتجر أو الفرع المستقل") },
                placeholder = { Text("مثال: فرع الصنوبر / متجر الأناقة") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (branchName.isNotEmpty()) {
                        IconButton(onClick = { branchName = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح اسم الفرع", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ind_mgr_branch_input")
            )

            // 3. Phone Number
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    localError = null
                },
                label = { Text("رقم الهاتف للتواصل") },
                placeholder = { Text("06xxxxxxxx") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                trailingIcon = {
                    if (phone.isNotEmpty()) {
                        IconButton(onClick = { phone = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح الهاتف", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ind_mgr_phone_input")
            )

            // 4. Email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                label = { Text("البريد الإلكتروني") },
                placeholder = { Text("name@example.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                trailingIcon = {
                    if (email.isNotEmpty()) {
                        IconButton(onClick = { email = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح البريد", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ind_mgr_email_input")
            )

            // 5. Manager PIN / Password
            OutlinedTextField(
                value = managerPin,
                onValueChange = {
                    managerPin = it
                    localError = null
                },
                label = { Text("تعيين رمز PIN السري (كلمة المرور)") },
                supportingText = {
                    Text("4 خانات على الأقل لتسجيل دخولك للنظام", fontSize = 11.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showPasswords = !showPasswords }) {
                        Icon(
                            imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPasswords) "إخفاء الرمز" else "إظهار الرمز",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ind_mgr_pin_input")
            )

            // 6. Confirm Manager PIN
            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    confirmPin = it
                    localError = null
                },
                label = { Text("تأكيد رمز PIN السري") },
                supportingText = {
                    Text("أعد إدخال الرمز السري للتأكيد والتثبيت", fontSize = 11.sp)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (confirmPin.isNotEmpty() && confirmPin == managerPin) semantic.profitGreen else MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { showPasswords = !showPasswords }) {
                        Icon(
                            imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPasswords) "إخفاء الرمز" else "إظهار الرمز",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ind_mgr_confirm_pin_input")
            )

            // 7. Optional Owner Secret Code
            OutlinedTextField(
                value = ownerSecret,
                onValueChange = {
                    ownerSecret = it
                    localError = null
                },
                label = { Text("الرمز السري للمالك للتفعيل الفوري (اختياري)") },
                supportingText = {
                    Text(
                        "أدخل رمز المالك (Deffas antar) للتفعيل والدخول فوراً، أو اتركه فارغاً لإرسال طلب تفعيل للمالك.",
                        fontSize = 10.5.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LockPerson,
                        contentDescription = null,
                        tint = semantic.profitGreen
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ownerSecret.isNotEmpty()) {
                            IconButton(onClick = { ownerSecret = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح رمز المالك", modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = { showPasswords = !showPasswords }) {
                            Icon(
                                imageVector = if (showPasswords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPasswords) "إخفاء الرمز" else "إظهار الرمز",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ind_mgr_owner_secret_input")
            )

            if (!localError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = semantic.dangerRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, semantic.dangerRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = semantic.dangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = localError.orEmpty(),
                            color = semantic.dangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Button(
                onClick = {
                    when {
                        fullName.trim().length < 2 -> localError = "يرجى إدخال اسم المدير المستقل"
                        branchName.trim().isEmpty() -> localError = "يرجى إدخال اسم المتجر أو الفرع المستقل"
                        !email.contains("@") || email.trim().length < 5 -> localError = "يرجى إدخال بريد إلكتروني صحيح"
                        managerPin.trim().length < 4 -> localError = "يرجى تعيين رمز سري للمدير (4 خانات على الأقل)"
                        confirmPin.isNotEmpty() && confirmPin.trim() != managerPin.trim() -> {
                            localError = "رمز PIN غير متطابق مع التأكيد! يرجى التحقق"
                        }
                        ownerSecret.isNotBlank() && !StaffAccountRepository.verifyOwnerSecretCode(ownerSecret) -> {
                            localError = "الرمز السري للمالك (Deffas antar) غير صحيح!"
                        }
                        else -> {
                            onSubmit(
                                fullName.trim(),
                                email.trim(),
                                phone.trim(),
                                branchName.trim().ifBlank { "متجر مستقل" },
                                managerPin.trim(),
                                ownerSecret.trim()
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_register_independent_manager_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (ownerSecret.isNotBlank()) "إنشاء وتفعيل ودخول فوري ✓" else "تأكيد وفتح الحساب (إرسال للمالك)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "إلغاء والعودة",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AuthenticatedStaffSessionGate(
    firebaseUser: FirebaseUser,
    initialCachedAccount: StaffAccount? = null,
    storeName: String,
    onApprovedAccountReady: (StaffAccount) -> Unit,
    onOwnerCreatedManager: (StaffAccount, String) -> Unit,
    onSignOut: () -> Unit,
    approvedContent: @Composable (
        myAccount: StaffAccount,
        allStaffAccounts: List<StaffAccount>,
        pendingCount: Int,
        onOpenStaffManagement: () -> Unit,
        onSignOut: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val semantic = LocalAFStoreSemanticColors.current
    val networkMonitor = remember { NetworkConnectivityMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle()
    val repository = remember(context) { StaffAccountRepository(context.applicationContext) }

    val isOwnerAccount = remember(firebaseUser.email) {
        StaffAccountRepository.isOwnerEmail(firebaseUser.email)
    }

    val matchingCachedAccount = remember(firebaseUser.uid, firebaseUser.email, initialCachedAccount, isOwnerAccount) {
        when {
            isOwnerAccount -> StaffAccount.createVerifiedOwnerAccount(firebaseUser.uid)
            initialCachedAccount != null && (
                initialCachedAccount.userId == firebaseUser.uid ||
                    initialCachedAccount.email.equals(firebaseUser.email.orEmpty(), ignoreCase = true)
                ) -> initialCachedAccount
            else -> {
                StaffAccountRepository.loadLocalAccountsRegistry(context).firstOrNull {
                    it.userId == firebaseUser.uid ||
                        (firebaseUser.email != null && it.email.equals(firebaseUser.email, ignoreCase = true))
                }
            }
        }
    }

    var isLoadingAccount by remember(firebaseUser.uid) { mutableStateOf(matchingCachedAccount == null) }
    var myStaffAccount by remember(firebaseUser.uid) { mutableStateOf<StaffAccount?>(matchingCachedAccount) }
    var firestoreError by remember(firebaseUser.uid) { mutableStateOf<String?>(null) }
    var firestoreStaffAccounts by remember(firebaseUser.uid) { mutableStateOf<List<StaffAccount>>(emptyList()) }
    var localRegistryAccounts by remember(firebaseUser.uid) {
        mutableStateOf(StaffAccountRepository.loadLocalAccountsRegistry(context))
    }
    var showStaffManagementModal by rememberSaveable { mutableStateOf(false) }

    val refreshLocalRegistry: () -> Unit = {
        localRegistryAccounts = StaffAccountRepository.loadLocalAccountsRegistry(context)
    }

    LaunchedEffect(matchingCachedAccount) {
        if (matchingCachedAccount != null && matchingCachedAccount.isApproved) {
            onApprovedAccountReady(matchingCachedAccount)
        }
    }

    // Observe the signed-in user's StaffAccount in real time
    LaunchedEffect(firebaseUser.uid) {
        if (matchingCachedAccount == null) {
            isLoadingAccount = true
        }
        firestoreError = null
        if (isOwnerAccount) {
            repository.bootstrapOwnerAccountIfNeeded(
                fullNameOverride = StaffAccountRepository.OWNER_FULL_NAME
            )
        } else {
            repository.claimPreApprovedManagerAccountIfMatchingEmail()
        }
        repository.observeMyStaffAccount(firebaseUser.uid)
            .catch { e ->
                Log.w("StaffAuthGate", "Error observing staff account", e)
                if (myStaffAccount == null && isOwnerAccount) {
                    val fallbackOwner = StaffAccount.createVerifiedOwnerAccount(firebaseUser.uid)
                    myStaffAccount = fallbackOwner
                    onApprovedAccountReady(fallbackOwner)
                } else {
                    firestoreError = e.localizedMessage ?: "تعذر قراءة بيانات الحساب من Firestore"
                }
                isLoadingAccount = false
            }
            .collect { account ->
                if (account == null && isOwnerAccount) {
                    val fallbackOwner = StaffAccount.createVerifiedOwnerAccount(firebaseUser.uid)
                    myStaffAccount = fallbackOwner
                    isLoadingAccount = false
                    onApprovedAccountReady(fallbackOwner)
                    repository.bootstrapOwnerAccountIfNeeded(
                        fullNameOverride = StaffAccountRepository.OWNER_FULL_NAME
                    )
                } else if (account == null) {
                    val claimed = repository.claimPreApprovedManagerAccountIfMatchingEmail().getOrNull()
                    if (claimed != null) {
                        myStaffAccount = claimed
                        isLoadingAccount = false
                        onApprovedAccountReady(claimed)
                    } else if (matchingCachedAccount == null) {
                        myStaffAccount = null
                        isLoadingAccount = false
                    }
                } else {
                    myStaffAccount = account
                    isLoadingAccount = false
                    if (account.isApproved) {
                        onApprovedAccountReady(account)
                    }
                }
            }
    }

    // Observe staff accounts according to role: Owner observes all; Independent Manager observes only their affiliated workers
    val activeAccount = myStaffAccount
    val isApprovedOwner = activeAccount?.isOwner == true
    val isApprovedRegularManager = activeAccount?.isRegularManager == true
    LaunchedEffect(firebaseUser.uid, isApprovedOwner, isApprovedRegularManager) {
        when {
            isApprovedOwner -> {
                repository.observeAllStoreStaffAccounts()
                    .catch { e ->
                        Log.w("StaffAuthGate", "Error observing all store staff accounts", e)
                    }
                    .collect { list ->
                        firestoreStaffAccounts = list
                    }
            }
            isApprovedRegularManager -> {
                repository.observeManagerAffiliatedWorkers(firebaseUser.uid)
                    .catch { e ->
                        Log.w("StaffAuthGate", "Error observing manager affiliated workers", e)
                    }
                    .collect { list ->
                        firestoreStaffAccounts = list
                    }
            }
            else -> {
                firestoreStaffAccounts = emptyList()
            }
        }
    }

    val allStaffAccounts = remember(firestoreStaffAccounts, localRegistryAccounts, activeAccount) {
        val mergedByKey = linkedMapOf<String, StaffAccount>()
        localRegistryAccounts.forEach { acc ->
            val key = acc.email.lowercase().ifBlank { acc.userId }
            mergedByKey[key] = acc
        }
        firestoreStaffAccounts.forEach { acc ->
            val key = acc.email.lowercase().ifBlank { acc.userId }
            val existingLocal = mergedByKey[key]
            mergedByKey[key] = if (existingLocal != null && acc.secretPin.isBlank()) {
                acc.copy(
                    secretPin = existingLocal.secretPin,
                    branchName = acc.branchName.ifBlank { existingLocal.branchName },
                    parentManagerUid = acc.parentManagerUid.ifBlank { existingLocal.parentManagerUid },
                    parentManagerName = acc.parentManagerName.ifBlank { existingLocal.parentManagerName },
                    parentManagerEmail = acc.parentManagerEmail.ifBlank { existingLocal.parentManagerEmail }
                )
            } else {
                acc
            }
        }
        val allMerged = mergedByKey.values.toList()
        when {
            activeAccount == null -> emptyList()
            activeAccount.isOwner -> allMerged
            activeAccount.isRegularManager -> allMerged.filter {
                it.userId == activeAccount.userId || it.isAffiliatedWithManager(activeAccount)
            }
            else -> listOf(activeAccount)
        }
    }

    val pendingRequestsCount = remember(allStaffAccounts) {
        allStaffAccounts.count { it.approvalStatus == StaffApprovalStatus.PENDING || it.isPending }
    }

    val currentAccount = myStaffAccount

    when {
        isLoadingAccount -> {
            val checkingAccountTitle = remember(firebaseUser.displayName, firebaseUser.email, isOwnerAccount) {
                when {
                    isOwnerAccount -> StaffAccountRepository.OWNER_FULL_NAME + " (المالك والمطور الرئيسي)"
                    !firebaseUser.displayName.isNullOrBlank() -> firebaseUser.displayName!!
                    !firebaseUser.email.isNullOrBlank() -> firebaseUser.email!!
                    else -> "مستخدم متجر AF store"
                }
            }
            val checkingAccountEmail = firebaseUser.email ?: ""

            Scaffold(
                contentWindowInsets = WindowInsets.systemBars,
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                        border = BorderStroke(1.5.dp, semantic.cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (semantic.isDark) 0.dp else 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                            .testTag("account_verification_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. App / Store Header Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "AF STORE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = storeName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 2. Central Cloud Sync Halo Icon
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "التحقق السحابي عبر فايربيس",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            // 3. Status Title & Description
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "جاري التحقق من صلاحيات الحساب سحابياً...",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "يتم الآن فحص وتوثيق رتبة الحساب، صلاحيات الأمان، وترخيص العمل عبر سحابة Firebase Firestore",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // 4. Account Details Card (Current checking user name & email)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isOwnerAccount) semantic.profitGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isOwnerAccount) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (isOwnerAccount) semantic.profitGreen else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = checkingAccountTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isOwnerAccount) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = semantic.profitGreen.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "مالك ✓",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = semantic.profitGreen,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (checkingAccountEmail.isNotBlank()) {
                                            Text(
                                                text = checkingAccountEmail,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Cloud Connectivity Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isOnline) semantic.profitContainer.copy(alpha = 0.7f) else semantic.debtContainer.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, if (isOnline) semantic.profitGreen.copy(alpha = 0.5f) else semantic.debtAmber.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(if (isOnline) semantic.profitGreen else semantic.debtAmber)
                                        )
                                        Icon(
                                            imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = if (isOnline) semantic.profitGreen else semantic.debtAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (isOnline) "سحابة Firebase: متصل بنجاح" else "حالة الاتصال: غير متصل (محلي)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOnline) semantic.profitGreen else semantic.debtAmber
                                        )
                                    }

                                    Text(
                                        text = if (isOnline) "Cloud Online ✓" else "Offline Cache ⚡",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOnline) semantic.profitGreen else semantic.debtAmber
                                    )
                                }
                            }

                            // 6. Smooth Gradient / Polished Progress Bar
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    strokeCap = StrokeCap.Round
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "مزامنة الصلاحيات اللحظية",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Firebase Firestore SDK",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // 7. Cancel / Switch Account Button
                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cancel_account_check_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إلغاء وفحص حساب آخر / تسجيل الخروج",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        currentAccount == null -> {
            NewStaffAccountRegistrationScreen(
                firebaseUser = firebaseUser,
                storeName = storeName,
                errorMessage = firestoreError,
                onSubmitRequest = { fullName, phone, onComplete ->
                    scope.launch {
                        val res = repository.registerPendingAccountRequest(
                            fullName = fullName,
                            phone = phone,
                            requestedRole = "worker"
                        )
                        if (res.isFailure) {
                            firestoreError = res.exceptionOrNull()?.localizedMessage
                                ?: "تعذر إرسال طلب فتح الحساب"
                        } else {
                            firestoreError = null
                        }
                        onComplete()
                    }
                },
                onSignOut = onSignOut
            )
        }

        !currentAccount.isApproved -> {
            StaffPendingApprovalScreen(
                account = currentAccount,
                storeName = storeName,
                errorMessage = firestoreError,
                onUpdateDetails = { updatedName, updatedPhone, onDone ->
                    scope.launch {
                        val res = repository.updateOwnProfileDetails(
                            fullName = updatedName,
                            phone = updatedPhone,
                            requestedRole = "worker"
                        )
                        if (res.isFailure) {
                            firestoreError = res.exceptionOrNull()?.localizedMessage
                        } else {
                            firestoreError = null
                        }
                        onDone()
                    }
                },
                onSignOut = onSignOut
            )
        }

        else -> {
            approvedContent(
                currentAccount,
                allStaffAccounts,
                pendingRequestsCount,
                {
                    refreshLocalRegistry()
                    showStaffManagementModal = true
                },
                onSignOut
            )

            if (showStaffManagementModal) {
                StaffAccountsManagementModal(
                    myAccount = currentAccount,
                    allAccounts = allStaffAccounts,
                    repository = repository,
                    onOwnerCreatedManager = { newAcc, pin ->
                        StaffAccountRepository.saveAccountToLocalRegistry(context, newAcc)
                        refreshLocalRegistry()
                        onOwnerCreatedManager(newAcc, pin)
                    },
                    onLocalRegistryUpdated = refreshLocalRegistry,
                    onDismiss = { showStaffManagementModal = false },
                    onSignOut = {
                        showStaffManagementModal = false
                        onSignOut()
                    }
                )
            }
        }
    }
}

@Composable
private fun NewStaffAccountRegistrationScreen(
    firebaseUser: FirebaseUser,
    storeName: String,
    errorMessage: String?,
    onSubmitRequest: (fullName: String, phone: String, onComplete: () -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var fullName by rememberSaveable(firebaseUser.uid) {
        mutableStateOf(firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "")
    }
    var phone by rememberSaveable(firebaseUser.uid) { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                border = BorderStroke(1.2.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .testTag("staff_registration_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "طلب فتح حساب عامل مبيعات / كاشير",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "$storeName • ${firebaseUser.email ?: ""}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "نوع الحساب: عامل مبيعات / كاشير",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                )
                            }
                            Text(
                                text = "التسجيل الذاتي من هذه الشاشة مخصص للعمال فقط. حسابات المدراء يتم إنشاؤها حصرياً ويدوياً من لوحة مالك المتجر (Deffas antar).",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("الاسم الكامل للعامل") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_reg_fullname_input")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف للتواصل (اختياري)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_reg_phone_input")
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = semantic.dangerRed.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, semantic.dangerRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                color = semantic.dangerRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isSubmitting = true
                            onSubmitRequest(
                                fullName.trim().ifBlank { firebaseUser.displayName ?: "عامل جديد" },
                                phone.trim()
                            ) {
                                isSubmitting = false
                            }
                        },
                        enabled = !isSubmitting && fullName.trim().length >= 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_staff_registration_btn")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري إرسال طلب فتح حساب العامل...")
                        } else {
                            Icon(Icons.Default.HowToReg, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال طلب فتح حساب عامل للإدارة", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل الخروج وتبديل حساب Google")
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffPendingApprovalScreen(
    account: StaffAccount,
    storeName: String,
    errorMessage: String?,
    onUpdateDetails: (fullName: String, phone: String, onDone: () -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var editedName by rememberSaveable(account.fullName) { mutableStateOf(account.fullName) }
    var editedPhone by rememberSaveable(account.phone) { mutableStateOf(account.phone) }
    var isSaving by remember { mutableStateOf(false) }

    val statusColor = when (account.approvalStatus) {
        StaffApprovalStatus.REJECTED, StaffApprovalStatus.SUSPENDED -> semantic.dangerRed
        else -> semantic.debtAmber
    }

    val statusTitle = when (account.approvalStatus) {
        StaffApprovalStatus.REJECTED -> "تم رفض طلب الدخول إلى الحساب"
        StaffApprovalStatus.SUSPENDED -> "تم إيقاف هذا الحساب مؤقتاً من طرف الإدارة"
        else -> "طلب فتح الحساب بانتظار موافقة الإدارة ⏳"
    }

    val statusDescription = when (account.approvalStatus) {
        StaffApprovalStatus.REJECTED -> "قامت إدارة المتجر برفض طلب الدخول الحالي. يمكنك تعديل بيانات الطلب أو التواصل مع مالك المتجر (Deffas antar)."
        StaffApprovalStatus.SUSPENDED -> "تم تعليق صلاحية دخول هذا الحساب مؤقتاً. تواصل مع مالك المتجر أو المدير لإعادة تفعيل الحساب."
        else -> "تم تسجيل طلبك كعامل في سحابة Firebase! بمجرد ضغط المالك (Deffas antar) أو المدير على «موافقة على الدخول» ستفتح لك شاشات المتجر تلقائياً في نفس اللحظة."
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .testTag("staff_pending_approval_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.5.dp, statusColor),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (account.isPending) Icons.Default.HourglassTop else Icons.Default.LockPerson,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = statusTitle,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = statusColor,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = statusDescription,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "بيانات الحساب المسجل في $storeName:",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("• الاسم: ${account.fullName}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("• البريد: ${account.email}", fontSize = 12.sp)
                            if (account.phone.isNotBlank()) {
                                Text("• الهاتف: ${account.phone}", fontSize = 12.sp)
                            }
                            Text(
                                text = "• المسمى الوظيفي: عامل مبيعات / كاشير",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• الحالة الحالية: ${StaffApprovalStatus.labelAr(account.approvalStatus)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    AnimatedVisibility(visible = isEditing) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("تعديل الاسم الكامل") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editedPhone,
                                onValueChange = { editedPhone = it },
                                label = { Text("تعديل رقم الهاتف") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    isSaving = true
                                    onUpdateDetails(editedName, editedPhone) {
                                        isSaving = false
                                        isEditing = false
                                    }
                                },
                                enabled = !isSaving && editedName.trim().length >= 2,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isSaving) "جاري الحفظ..." else "حفظ التعديلات")
                            }
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage,
                            color = semantic.dangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = !isEditing },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEditing) "إغلاق التعديل" else "تعديل بيانات الطلب", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الخروج", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingStaffApprovalsBanner(
    pendingCount: Int,
    onOpenApprovalModal: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    AnimatedVisibility(
        visible = pendingCount > 0,
        enter = fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350)) { -it },
        exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(animationSpec = tween(250)) { -it }
    ) {
        Surface(
            color = if (semantic.isDark) Color(0xFF3B2106) else Color(0xFFFFFBEB),
            border = BorderStroke(1.2.dp, semantic.debtAmber),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clickable { onOpenApprovalModal() }
                .testTag("pending_staff_approvals_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = semantic.debtAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "طلبات دخول حسابات العمال ($pendingCount طلب معلق)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = if (semantic.isDark) Color.White else Color(0xFF78350F)
                        )
                        Text(
                            text = "اضغط هنا لمراجعة طلبات العمال الجدد والموافقة على دخولهم وتحديد صلاحياتهم",
                            fontSize = 10.5.sp,
                            color = if (semantic.isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                        )
                    }
                }

                Button(
                    onClick = onOpenApprovalModal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = semantic.debtAmber,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("مراجعة والموافقة", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StaffAccountsManagementModal(
    myAccount: StaffAccount,
    allAccounts: List<StaffAccount>,
    repository: StaffAccountRepository,
    onOwnerCreatedManager: (StaffAccount, String) -> Unit = { _, _ -> },
    onLocalRegistryUpdated: () -> Unit = {},
    onDismiss: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalAFStoreSemanticColors.current
    val scope = rememberCoroutineScope()
    var selectedTabItem by rememberSaveable { mutableIntStateOf(0) }
    var selectedManagerFilterUid by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var editingPermissionsTarget by remember { mutableStateOf<StaffAccount?>(null) }
    var showCreateManagerDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateWorkerDialog by rememberSaveable { mutableStateOf(false) }
    var pendingPromoteToManagerTarget by remember { mutableStateOf<StaffAccount?>(null) }
    var actionFeedbackMessage by remember { mutableStateOf<String?>(null) }
    val allUpgradeRequests by repository.observeAllProUpgradeRequests().collectAsStateWithLifecycle(emptyList())
    val pendingProUpgradeRequests = remember(allUpgradeRequests) { allUpgradeRequests.filter { it.isPending } }

    val isCallerOwner = myAccount.isOwner

    val visibleAccounts = remember(allAccounts, isCallerOwner, myAccount) {
        if (isCallerOwner) {
            allAccounts
        } else {
            allAccounts.filter {
                it.userId == myAccount.userId || it.isAffiliatedWithManager(myAccount)
            }
        }
    }

    val independentManagersList = remember(allAccounts) {
        allAccounts.filter { it.role == "manager" && !it.isOwner }
    }

    val myAffiliatedWorkersCount = remember(visibleAccounts, myAccount) {
        visibleAccounts.count { it.role == "worker" && !it.isOwner && it.userId != myAccount.userId }
    }

    val pendingCount = remember(visibleAccounts) {
        visibleAccounts.count { it.approvalStatus == StaffApprovalStatus.PENDING }
    }
    val approvedCount = remember(visibleAccounts) {
        visibleAccounts.count { it.approvalStatus == StaffApprovalStatus.APPROVED }
    }
    val managersCount = remember(allAccounts) {
        allAccounts.count { it.role == "manager" || it.isOwner }
    }
    val blockedCount = remember(visibleAccounts) {
        visibleAccounts.count {
            it.approvalStatus == StaffApprovalStatus.REJECTED ||
                    it.approvalStatus == StaffApprovalStatus.SUSPENDED
        }
    }

    val filteredAccounts = remember(visibleAccounts, selectedTabItem, selectedManagerFilterUid, searchQuery, independentManagersList) {
        val baseFiltered = when (selectedTabItem) {
            1 -> visibleAccounts.filter { it.role == "manager" || it.isOwner || it.requestedRole == "manager" }
            2 -> visibleAccounts.filter { it.role == "worker" && !it.isOwner }
            3 -> visibleAccounts.filter { it.approvalStatus == StaffApprovalStatus.PENDING }
            4 -> visibleAccounts.filter { it.approvalStatus == StaffApprovalStatus.APPROVED }
            5 -> visibleAccounts.filter {
                it.approvalStatus == StaffApprovalStatus.REJECTED ||
                        it.approvalStatus == StaffApprovalStatus.SUSPENDED
            }
            else -> visibleAccounts
        }

        val managerFiltered = if (selectedManagerFilterUid.isBlank()) {
            baseFiltered
        } else {
            val selectedMgr = independentManagersList.firstOrNull { it.userId == selectedManagerFilterUid }
            if (selectedMgr == null) {
                baseFiltered
            } else {
                baseFiltered.filter {
                    it.userId == selectedMgr.userId || it.isAffiliatedWithManager(selectedMgr)
                }
            }
        }

        if (searchQuery.isBlank()) {
            managerFiltered
        } else {
            val q = searchQuery.trim()
            managerFiltered.filter {
                it.fullName.contains(q, ignoreCase = true) ||
                        it.email.contains(q, ignoreCase = true) ||
                        it.branchName.contains(q, ignoreCase = true) ||
                        it.phone.contains(q, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .testTag("staff_accounts_management_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isCallerOwner) {
                                    "لوحة المالك (Deffas antar) • إشراف المدراء والعمال"
                                } else {
                                    "ملف المدير المستقل • إدارة العمال والفرع"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "الحساب النشط: ${myAccount.fullName} (${myAccount.roleBadgeLabelAr}) • الإجمالي: ${visibleAccounts.size} حسابات",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Owner Exclusive Banner (Ultra-Compact)
                    if (isCallerOwner) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = semantic.profitGreen.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_exclusive_manager_creation_banner")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "صلاحية المالك الشاملة • ${myAccount.fullName}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = semantic.profitGreen,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = semantic.profitGreen.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "المالك الرئيسي",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = semantic.profitGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showCreateManagerDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = semantic.profitGreen,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("create_manager_by_owner_btn")
                                    ) {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "إنشاء مدير مستقل",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            maxLines = 1
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (!myAccount.isSyncEnabled && !myAccount.isOwner) {
                                                actionFeedbackMessage = "خاصية إنشاء عمال تابعين تتطلب حساب Pro mode. حسابك الحالي في وضع Just Offline / Lite."
                                            } else {
                                                showCreateWorkerDialog = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("create_worker_by_manager_btn")
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "إنشاء حساب عامل",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isCallerOwner && pendingProUpgradeRequests.isNotEmpty()) {
                        OwnerProUpgradeRequestsCard(
                            upgradeRequests = pendingProUpgradeRequests,
                            onApproveRequest = { req ->
                                scope.launch {
                                    val res = repository.reviewProUpgradeRequest(req, approve = true)
                                    if (res.isSuccess) {
                                        actionFeedbackMessage = "تمت الموافقة وتفعيل باقة Pro للمدير «${req.managerName}» بنجاح ✓"
                                        onLocalRegistryUpdated()
                                    } else {
                                        actionFeedbackMessage = "تعذر تفعيل باقة Pro: ${res.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            onRejectRequest = { req ->
                                scope.launch {
                                    val res = repository.reviewProUpgradeRequest(req, approve = false)
                                    if (res.isSuccess) {
                                        actionFeedbackMessage = "تم رفض طلب ترقية المدير «${req.managerName}»."
                                    } else {
                                        actionFeedbackMessage = "تعذر تحديث الطلب: ${res.exceptionOrNull()?.message}"
                                    }
                                }
                            }
                        )
                    }

                    if (myAccount.isRegularManager) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("independent_manager_profile_banner")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ملف المدير: ${myAccount.fullName} • ${myAccount.branchName.ifBlank { "فرع مستقل" }}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    if (myAccount.secretPin.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = semantic.profitGreen.copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "رمزك: ${myAccount.secretPin}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = semantic.profitGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                                                        onClick = {
                                        if (!myAccount.isSyncEnabled && !myAccount.isOwner) {
                                            actionFeedbackMessage = "خاصية إنشاء عمال تابعين تتطلب حساب Pro mode. حسابك الحالي في وضع Just Offline / Lite."
                                        } else {
                                            showCreateWorkerDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .testTag("create_worker_by_manager_btn")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "➕ إنشاء حساب عامل تابع برمز سري",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Search & FilterBar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث باسم الحساب، الإيميل، أو الفرع...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        singleLine = true
                    )

                    // Horizontal Tabs with Count Badges
                    val tabsList = remember(isCallerOwner, visibleAccounts.size, managersCount, myAffiliatedWorkersCount, pendingCount, approvedCount, blockedCount) {
                        listOfNotNull(
                            "الكل (${visibleAccounts.size})",
                            if (isCallerOwner) "المدراء ($managersCount)" else null,
                            "العمال ($myAffiliatedWorkersCount)",
                            if (pendingCount > 0) "المعلقة ⏳ ($pendingCount)" else "المعلقة ($pendingCount)",
                            "المعتمدة ✓ ($approvedCount)",
                            if (blockedCount > 0) "الموقوفة ($blockedCount)" else null
                        )
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabItem.coerceAtMost(tabsList.size - 1),
                        edgePadding = 0.dp,
                        divider = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabsList.forEachIndexed { index, tabTitle ->
                            Tab(
                                selected = selectedTabItem == index,
                                onClick = {
                                    selectedTabItem = index
                                    selectedManagerFilterUid = ""
                                },
                                text = {
                                    Text(
                                        text = tabTitle,
                                        fontWeight = if (selectedTabItem == index) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (selectedTabItem == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                    // Owner Hierarchy Filter by Independent Manager (if selected)
                    if (isCallerOwner && independentManagersList.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تصفية بالمدير:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ScrollableTabRow(
                                selectedTabIndex = 0,
                                edgePadding = 0.dp,
                                divider = {},
                                modifier = Modifier.weight(1f)
                            ) {
                                FilterChip(
                                    selected = selectedManagerFilterUid.isBlank(),
                                    onClick = { selectedManagerFilterUid = "" },
                                    label = { Text("جميع الفرق", fontSize = 11.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                independentManagersList.forEach { mgr ->
                                    val countForMgr = allAccounts.count { it.isAffiliatedWithManager(mgr) }
                                    val isSelected = selectedManagerFilterUid == mgr.userId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedManagerFilterUid = if (isSelected) "" else mgr.userId
                                        },
                                        label = {
                                            Text(
                                                text = "${mgr.fullName} ($countForMgr)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = actionFeedbackMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = actionFeedbackMessage.orEmpty(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { actionFeedbackMessage = null }) {
                                    Text("إخفاء", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (!myAccount.isManagerOrAdmin) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
                            border = BorderStroke(1.dp, semantic.cardBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "أنت مسجل حالياً بصلاحية: ${myAccount.roleBadgeLabelAr}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "إنشاء وإدارة حسابات العمال متاح للمالك (Deffas antar) أو لكل مدير مستقل على عماله التابعين له.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = onSignOut) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تسجيل الخروج")
                                }
                            }
                        }
                    } else {
                        // Expanded Browsing List taking full remaining height
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            if (filteredAccounts.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = semantic.cardElevated,
                                        border = BorderStroke(1.dp, semantic.cardBorder),
                                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Group,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "لا توجد حسابات مطابقة للفلتر أو البحث المحدد",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (isCallerOwner) {
                                                    "يمكنك إنشاء ملف مدير مستقل جديد أو إنشاء حساب عامل تابع من الأزرار أعلاه."
                                                } else {
                                                    "اضغط على زر «إنشاء حساب عامل تابع لملفي» أعلاه لإضافة عمال تابعين لفرعك برمز سري وصلاحيات مخصصة."
                                                },
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(filteredAccounts, key = { it.userId }) { staff ->
                                    val affiliatedCountForThisManager = if (staff.role == "manager" && !staff.isOwner) {
                                        allAccounts.count { it.isAffiliatedWithManager(staff) }
                                    } else {
                                        0
                                    }
                                    StaffAccountApprovalItemCard(
                                        staff = staff,
                                        affiliatedWorkersCount = affiliatedCountForThisManager,
                                        isCurrentUser = staff.userId == myAccount.userId,
                                        isCallerOwner = isCallerOwner,
                                        onQuickApproveAsWorker = {
                                            scope.launch {
                                                val updatedAcc = staff.copy(
                                                    approvalStatus = StaffApprovalStatus.APPROVED,
                                                    role = "worker",
                                                    approvedByUid = myAccount.userId,
                                                    approvedByName = myAccount.fullName,
                                                    parentManagerUid = if (!isCallerOwner) myAccount.userId else staff.parentManagerUid,
                                                    parentManagerName = if (!isCallerOwner) myAccount.fullName else staff.parentManagerName,
                                                    parentManagerEmail = if (!isCallerOwner) myAccount.email else staff.parentManagerEmail
                                                )
                                                val res = repository.approveOrUpdateStaffAccount(
                                                    targetAccount = staff,
                                                    newApprovalStatus = StaffApprovalStatus.APPROVED,
                                                    newRole = "worker",
                                                    permissions = staff.toWorkerPermissions(),
                                                    managerName = myAccount.fullName,
                                                    isCallerOwner = isCallerOwner,
                                                    callerAccount = myAccount
                                                )
                                                if (res.isSuccess) {
                                                    StaffAccountRepository.saveAccountToLocalRegistry(context, updatedAcc)
                                                    onLocalRegistryUpdated()
                                                    onOwnerCreatedManager(updatedAcc, updatedAcc.secretPin.ifBlank { "1234" })
                                                    actionFeedbackMessage = "تمت الموافقة على دخول «${staff.fullName}» كعامل مبيعات ✓"
                                                } else {
                                                    actionFeedbackMessage = "تعذر التحديث: ${res.exceptionOrNull()?.localizedMessage}"
                                                }
                                            }
                                        },
                                        onQuickApproveAsManager = {
                                            pendingPromoteToManagerTarget = staff
                                        },
                                        onRejectOrSuspend = { newStatus ->
                                            scope.launch {
                                                val updatedAcc = staff.copy(approvalStatus = newStatus)
                                                val res = repository.approveOrUpdateStaffAccount(
                                                    targetAccount = staff,
                                                    newApprovalStatus = newStatus,
                                                    newRole = if (isCallerOwner) staff.role else "worker",
                                                    permissions = staff.toWorkerPermissions(),
                                                    managerName = myAccount.fullName,
                                                    isCallerOwner = isCallerOwner,
                                                    ownerSecretCode = if (isCallerOwner) StaffAccountRepository.OWNER_SECRET_CODE else "",
                                                    callerAccount = myAccount
                                                )
                                                if (res.isSuccess) {
                                                    StaffAccountRepository.saveAccountToLocalRegistry(context, updatedAcc)
                                                    onLocalRegistryUpdated()
                                                    actionFeedbackMessage = "تم تحديث حالة حساب «${staff.fullName}» إلى ${StaffApprovalStatus.labelAr(newStatus)}"
                                                } else {
                                                    actionFeedbackMessage = "تعذر تحديث الحالة: ${res.exceptionOrNull()?.localizedMessage}"
                                                }
                                            }
                                        },
                                        onCustomizePermissions = {
                                            editingPermissionsTarget = staff
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Footer Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 34.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("staff_modal_sign_out_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الخروج من الحساب", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("إغلاق النافذة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showCreateManagerDialog && isCallerOwner) {
        CreateManagerByOwnerDialog(
            onDismiss = { showCreateManagerDialog = false },
            onCreateManager = { fullName, email, phone, branchName, managerPin, ownerSecret, perms ->
                scope.launch {
                    val res = repository.createManagerAccountByOwner(
                        managerFullName = fullName,
                        managerEmail = email,
                        managerPhone = phone,
                        ownerSecretCode = ownerSecret,
                        permissions = perms,
                        branchName = branchName,
                        managerSecretPin = managerPin
                    )
                    if (res.isSuccess) {
                        val created = res.getOrThrow()
                        StaffAccountRepository.saveAccountToLocalRegistry(context, created)
                        onLocalRegistryUpdated()
                        onOwnerCreatedManager(created, managerPin)
                        showCreateManagerDialog = false
                        actionFeedbackMessage = "تم إنشاء واعتماد ملف المدير المستقل «$fullName» ($branchName) بنجاح ✓. يستطيع المدير الآن فتح التطبيق من هاتفه واختيار «تسجيل الدخول بحساب Google» ($email) للدخول الفوري ومزامنة المتجر."
                    } else {
                        actionFeedbackMessage = res.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء ملف المدير المستقل"
                    }
                }
            }
        )
    }

    if (showCreateWorkerDialog && myAccount.isManagerOrAdmin) {
        CreateAffiliatedWorkerByManagerDialog(
            callerAccount = myAccount,
            availableManagers = independentManagersList,
            onDismiss = { showCreateWorkerDialog = false },
            onCreateWorker = { targetManager, workerName, workerEmail, workerPhone, workerPin, perms ->
                scope.launch {
                    val res = repository.createWorkerAccountByManager(
                        managerAccount = targetManager,
                        workerFullName = workerName,
                        workerEmail = workerEmail,
                        workerPhone = workerPhone,
                        workerSecretPin = workerPin,
                        permissions = perms
                    )
                    if (res.isSuccess) {
                        val createdWorker = res.getOrThrow()
                        StaffAccountRepository.saveAccountToLocalRegistry(context, createdWorker)
                        onLocalRegistryUpdated()
                        onOwnerCreatedManager(createdWorker, workerPin)
                        showCreateWorkerDialog = false
                        actionFeedbackMessage = "تم إنشاء حساب العامل التابع «$workerName» وربطه بملف المدير «${targetManager.fullName}» بنجاح ✓"
                    } else {
                        actionFeedbackMessage = res.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء حساب العامل التابع"
                    }
                }
            }
        )
    }

    pendingPromoteToManagerTarget?.let { target ->
        OwnerSecretVerificationDialog(
            title = "تأكيد تفعيل ملف المدير المستقل «${target.fullName}»",
            subtitle = "أدخل الرمز السري الخاص بالمالك (Deffas antar) لتأكيد اعتماد هذا الحساب كملف مدير مستقل:",
            onDismiss = { pendingPromoteToManagerTarget = null },
            onVerified = { verifiedSecret ->
                scope.launch {
                    val fullPerms = WorkerPermissions(
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
                    val res = repository.approveOrUpdateStaffAccount(
                        targetAccount = target,
                        newApprovalStatus = StaffApprovalStatus.APPROVED,
                        newRole = "manager",
                        permissions = fullPerms,
                        managerName = StaffAccountRepository.OWNER_FULL_NAME,
                        isCallerOwner = true,
                        ownerSecretCode = verifiedSecret,
                        callerAccount = myAccount
                    )
                    pendingPromoteToManagerTarget = null
                    if (res.isSuccess) {
                        val promoted = target.copy(
                            role = "manager",
                            requestedRole = "manager",
                            approvalStatus = StaffApprovalStatus.APPROVED,
                            approvedByName = StaffAccountRepository.OWNER_FULL_NAME
                        )
                        StaffAccountRepository.saveAccountToLocalRegistry(context, promoted)
                        onLocalRegistryUpdated()
                        onOwnerCreatedManager(promoted, promoted.secretPin.ifBlank { "1234" })
                        actionFeedbackMessage = "تم اعتماد وتفعيل ملف المدير المستقل «${target.fullName}» بواسطة المالك Deffas antar ✓"
                    } else {
                        actionFeedbackMessage = "تعذر التحديث: ${res.exceptionOrNull()?.localizedMessage}"
                    }
                }
            }
        )
    }

    editingPermissionsTarget?.let { target ->
        StaffPermissionsEditorDialog(
            staff = target,
            isCallerOwner = isCallerOwner,
            onDismiss = { editingPermissionsTarget = null },
            onSave = { updatedRole, updatedStatus, updatedPerms, ownerSecret, updatedSyncMode ->
                scope.launch {
                    val res = repository.approveOrUpdateStaffAccount(
                        targetAccount = target,
                        newApprovalStatus = updatedStatus,
                        newRole = updatedRole,
                        permissions = updatedPerms,
                        managerName = myAccount.fullName,
                        isCallerOwner = isCallerOwner,
                        ownerSecretCode = ownerSecret,
                        callerAccount = myAccount,
                        syncMode = updatedSyncMode
                    )
                    if (res.isSuccess) {
                        val updatedLocal = target.copy(
                            role = updatedRole,
                            approvalStatus = updatedStatus,
                            canMakeSales = updatedPerms.canMakeSales,
                            canManageInventory = updatedPerms.canManageInventory,
                            canEditPrices = updatedPerms.canEditPrices,
                            canViewReports = updatedPerms.canViewReports,
                            canManageDebtsAndCustomers = updatedPerms.canManageDebtsAndCustomers,
                            canRefundOrVoid = updatedPerms.canRefundOrVoid,
                            canApplyDiscounts = updatedPerms.canApplyDiscounts,
                            canViewCostPrices = updatedPerms.canViewCostPrices,
                            canExportData = updatedPerms.canExportData,
                            canManagePurchaseInvoices = updatedPerms.canManagePurchaseInvoices,
                            syncMode = updatedSyncMode
                        )
                        StaffAccountRepository.saveAccountToLocalRegistry(context, updatedLocal)
                        onLocalRegistryUpdated()
                        onOwnerCreatedManager(updatedLocal, updatedLocal.secretPin.ifBlank { "1234" })
                        editingPermissionsTarget = null
                        actionFeedbackMessage = "تم حفظ صلاحيات وحالة حساب «${target.fullName}» بنجاح ✓"
                    } else {
                        actionFeedbackMessage = "تعذر حفظ الصلاحيات: ${res.exceptionOrNull()?.localizedMessage}"
                    }
                }
            }
        )
    }
}

@Composable
private fun CreateManagerByOwnerDialog(
    onDismiss: () -> Unit,
    onCreateManager: (
        fullName: String,
        email: String,
        phone: String,
        branchName: String,
        managerPin: String,
        ownerSecretCode: String,
        permissions: WorkerPermissions
    ) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var managerName by rememberSaveable { mutableStateOf("") }
    var managerEmail by rememberSaveable { mutableStateOf("") }
    var managerPhone by rememberSaveable { mutableStateOf("") }
    var managerBranchName by rememberSaveable { mutableStateOf("فرع مستقل") }
    var managerLocalPin by rememberSaveable { mutableStateOf("1234") }
    var ownerSecretCode by rememberSaveable { mutableStateOf("") }
    var showSecret by rememberSaveable { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    var canMakeSales by rememberSaveable { mutableStateOf(true) }
    var canManageInventory by rememberSaveable { mutableStateOf(true) }
    var canEditPrices by rememberSaveable { mutableStateOf(true) }
    var canViewReports by rememberSaveable { mutableStateOf(true) }
    var canManageDebtsAndCustomers by rememberSaveable { mutableStateOf(true) }
    var canRefundOrVoid by rememberSaveable { mutableStateOf(true) }
    var canApplyDiscounts by rememberSaveable { mutableStateOf(true) }
    var canViewCostPrices by rememberSaveable { mutableStateOf(true) }
    var canExportData by rememberSaveable { mutableStateOf(true) }
    var canManagePurchaseInvoices by rememberSaveable { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, semantic.profitGreen.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_manager_by_owner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.profitGreen.copy(alpha = 0.16f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = semantic.profitGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إنشاء ملف مدير مستقل جديد (خاص بالمالك)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp
                        )
                        Text(
                            text = "المالك: ${StaffAccountRepository.OWNER_FULL_NAME} • المدير المستقل يدير عماله التابعين له فقط",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = managerName,
                    onValueChange = {
                        managerName = it
                        localError = null
                    },
                    label = { Text("الاسم الكامل للمدير المستقل") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_manager_fullname_input")
                )

                OutlinedTextField(
                    value = managerBranchName,
                    onValueChange = {
                        managerBranchName = it
                        localError = null
                    },
                    label = { Text("اسم الفرع أو القسم المستقل للمدير") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_manager_branch_input")
                )

                OutlinedTextField(
                    value = managerEmail,
                    onValueChange = {
                        managerEmail = it
                        localError = null
                    },
                    label = { Text("البريد الإلكتروني للمدير (Google أو اسم الدخول)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_manager_email_input")
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "للمزامنة السحابية الفورية: أدخل نفس بريد Google الخاص بهاتف المدير، ليتمكن من الدخول فوراً بضغطة زر واحدة.",
                            fontSize = 11.sp,
                            color = if (semantic.isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1),
                            lineHeight = 15.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = managerPhone,
                        onValueChange = { managerPhone = it },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_manager_phone_input")
                    )

                    OutlinedTextField(
                        value = managerLocalPin,
                        onValueChange = { managerLocalPin = it },
                        label = { Text("الرمز السري للمدير") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("new_manager_pin_input")
                    )
                }

                // Owner Secret Code Verification Field
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "تأكيد الرمز السري للمالك (Deffas antar):",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = ownerSecretCode,
                            onValueChange = {
                                ownerSecretCode = it
                                localError = null
                            },
                            label = { Text("أدخل الرمز السري للمالك لتأكيد إنشاء المدير المستقل") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showSecret = !showSecret }) {
                                    Icon(
                                        imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "إظهار/إخفاء الرمز السري"
                                    )
                                }
                            },
                            visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_manager_owner_secret_input")
                        )
                    }
                }

                Text("صلاحيات ملف المدير المستقل:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                PermissionToggleRow(label = "إصدار فواتير البيع في الكاشير", checked = canMakeSales, icon = Icons.Default.CheckCircle, onCheckedChange = { canMakeSales = it })
                PermissionToggleRow(label = "إضافة وتعديل أصناف المخزون", checked = canManageInventory, icon = Icons.Default.VerifiedUser, onCheckedChange = { canManageInventory = it })
                PermissionToggleRow(label = "تعديل أسعار البيع في الفاتورة", checked = canEditPrices, icon = Icons.Default.Edit, onCheckedChange = { canEditPrices = it })
                PermissionToggleRow(label = "إدارة حسابات العملاء والديون", checked = canManageDebtsAndCustomers, icon = Icons.Default.Group, onCheckedChange = { canManageDebtsAndCustomers = it })
                PermissionToggleRow(label = "منح الخصومات للعملاء", checked = canApplyDiscounts, icon = Icons.Default.Info, onCheckedChange = { canApplyDiscounts = it })
                PermissionToggleRow(label = "عمل مرتجعات أو إلغاء الفواتير", checked = canRefundOrVoid, icon = Icons.Default.WarningAmber, onCheckedChange = { canRefundOrVoid = it })
                PermissionToggleRow(label = "عرض التقارير المالية والأرباح", checked = canViewReports, icon = Icons.Default.LockPerson, isSensitive = true, onCheckedChange = { canViewReports = it })
                PermissionToggleRow(label = "رؤية أسعار الشراء والتكلفة", checked = canViewCostPrices, icon = Icons.Default.Lock, isSensitive = true, onCheckedChange = { canViewCostPrices = it })
                PermissionToggleRow(label = "تصدير ملفات PDF والنسخ الاحتياطي", checked = canExportData, icon = Icons.Default.CloudSync, onCheckedChange = { canExportData = it })
                PermissionToggleRow(label = "تسجيل فواتير المشتريات والموردين", checked = canManagePurchaseInvoices, icon = Icons.Default.Security, isSensitive = true, onCheckedChange = { canManagePurchaseInvoices = it })

                if (!localError.isNullOrBlank()) {
                    Text(
                        text = localError.orEmpty(),
                        color = semantic.dangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            when {
                                managerName.trim().length < 2 -> {
                                    localError = "يرجى إدخال الاسم الكامل للمدير المستقل"
                                }
                                !managerEmail.contains("@") || managerEmail.trim().length < 5 -> {
                                    localError = "يرجى إدخال البريد الإلكتروني الصحيح للمدير"
                                }
                                managerLocalPin.trim().length < 4 -> {
                                    localError = "يرجى تعيين رمز سري للمدير لا يقل عن 4 خانات"
                                }
                                !StaffAccountRepository.verifyOwnerSecretCode(ownerSecretCode) -> {
                                    localError = "الرمز السري للمالك (Deffas antar) غير صحيح! لا يمكن إنشاء ملف مدير مستقل."
                                }
                                else -> {
                                    onCreateManager(
                                        managerName.trim(),
                                        managerEmail.trim(),
                                        managerPhone.trim(),
                                        managerBranchName.trim().ifBlank { "فرع مستقل" },
                                        managerLocalPin.trim(),
                                        ownerSecretCode.trim(),
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
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = semantic.profitGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_create_manager_btn")
                    ) {
                        Text("إنشاء واعتماد ملف المدير المستقل", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateAffiliatedWorkerByManagerDialog(
    callerAccount: StaffAccount,
    availableManagers: List<StaffAccount>,
    onDismiss: () -> Unit,
    onCreateWorker: (
        targetManager: StaffAccount,
        workerName: String,
        workerEmail: String,
        workerPhone: String,
        workerPin: String,
        permissions: WorkerPermissions
    ) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var workerName by rememberSaveable { mutableStateOf("") }
    var workerEmail by rememberSaveable { mutableStateOf("") }
    var workerPhone by rememberSaveable { mutableStateOf("") }
    var workerPin by rememberSaveable { mutableStateOf("1234") }
    var selectedParentManagerUid by rememberSaveable {
        mutableStateOf(
            if (callerAccount.isOwner && availableManagers.isNotEmpty()) {
                availableManagers.first().userId
            } else {
                callerAccount.userId
            }
        )
    }
    var localError by remember { mutableStateOf<String?>(null) }

    var canMakeSales by rememberSaveable { mutableStateOf(true) }
    var canManageInventory by rememberSaveable { mutableStateOf(false) }
    var canEditPrices by rememberSaveable { mutableStateOf(false) }
    var canViewReports by rememberSaveable { mutableStateOf(false) }
    var canManageDebtsAndCustomers by rememberSaveable { mutableStateOf(false) }
    var canRefundOrVoid by rememberSaveable { mutableStateOf(false) }
    var canApplyDiscounts by rememberSaveable { mutableStateOf(true) }
    var canViewCostPrices by rememberSaveable { mutableStateOf(false) }
    var canExportData by rememberSaveable { mutableStateOf(false) }
    var canManagePurchaseInvoices by rememberSaveable { mutableStateOf(false) }
    var selectedPreset by rememberSaveable { mutableStateOf("cashier") }

    fun applyPreset(preset: String) {
        selectedPreset = preset
        when (preset) {
            "cashier" -> {
                canMakeSales = true
                canApplyDiscounts = true
                canManageInventory = false
                canEditPrices = false
                canRefundOrVoid = false
                canViewReports = false
                canViewCostPrices = false
                canManagePurchaseInvoices = false
                canManageDebtsAndCustomers = false
                canExportData = false
            }
            "storekeeper" -> {
                canMakeSales = false
                canManageInventory = true
                canManagePurchaseInvoices = true
                canViewCostPrices = false
                canViewReports = false
                canEditPrices = false
                canRefundOrVoid = false
                canApplyDiscounts = false
                canManageDebtsAndCustomers = false
                canExportData = false
            }
            "all" -> {
                canMakeSales = true
                canManageInventory = true
                canManagePurchaseInvoices = true
                canViewCostPrices = true
                canViewReports = true
                canEditPrices = true
                canRefundOrVoid = true
                canApplyDiscounts = true
                canManageDebtsAndCustomers = true
                canExportData = true
            }
            "none" -> {
                canMakeSales = false
                canManageInventory = false
                canManagePurchaseInvoices = false
                canViewCostPrices = false
                canViewReports = false
                canEditPrices = false
                canRefundOrVoid = false
                canApplyDiscounts = false
                canManageDebtsAndCustomers = false
                canExportData = false
            }
        }
    }

    val resolvedTargetManager = remember(callerAccount, availableManagers, selectedParentManagerUid) {
        if (!callerAccount.isOwner) {
            callerAccount
        } else {
            availableManagers.firstOrNull { it.userId == selectedParentManagerUid } ?: callerAccount
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_worker_by_manager_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إنشاء حساب عامل مبيعات تابع للمدير",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp
                        )
                        Text(
                            text = "المدير المشرف: ${resolvedTargetManager.fullName} (${resolvedTargetManager.branchName.ifBlank { "AF store" }})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider()

                if (callerAccount.isOwner && availableManagers.isNotEmpty()) {
                    Text(
                        text = "اختر ملف المدير المستقل الذي يتبع له هذا العامل:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableManagers.forEach { mgr ->
                            FilterChip(
                                selected = selectedParentManagerUid == mgr.userId,
                                onClick = { selectedParentManagerUid = mgr.userId },
                                label = {
                                    Text(
                                        text = "${mgr.fullName} (${mgr.branchName.ifBlank { "فرع مستقل" }})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                        FilterChip(
                            selected = selectedParentManagerUid == callerAccount.userId,
                            onClick = { selectedParentManagerUid = callerAccount.userId },
                            label = {
                                Text(
                                    text = "إشراف المالك المباشر (Deffas antar)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = workerName,
                    onValueChange = {
                        workerName = it
                        localError = null
                    },
                    label = { Text("الاسم الكامل للعامل التابع") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_worker_fullname_input")
                )

                OutlinedTextField(
                    value = workerEmail,
                    onValueChange = {
                        workerEmail = it
                        localError = null
                    },
                    label = { Text("البريد الإلكتروني أو اسم الدخول للعامل (مثال: cashier1@afstore.com)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_worker_email_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = workerPhone,
                        onValueChange = { workerPhone = it },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_worker_phone_input")
                    )

                    OutlinedTextField(
                        value = workerPin,
                        onValueChange = {
                            workerPin = it
                            localError = null
                        },
                        label = { Text("الرمز السري للعامل") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("new_worker_pin_input")
                    )
                }

                Text("قوالب الصلاحيات الجاهزة (تحديد سريع):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedPreset == "cashier",
                        onClick = { applyPreset("cashier") },
                        label = { Text("كاشير 🛒", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPreset == "storekeeper",
                        onClick = { applyPreset("storekeeper") },
                        label = { Text("مخزن 📦", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPreset == "custom",
                        onClick = { selectedPreset = "custom" },
                        label = { Text("مخصص ⚙️", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { applyPreset("all") },
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تفعيل الكل", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { applyPreset("none") },
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حجب الكل 🔒", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // SENSITIVE PERMISSIONS SECTION
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المعلومات المالية والسرية (تحكم بالحجب):",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        PermissionToggleRow(
                            label = "رؤية تكلفة المنتجات وأرباحها",
                            subtitle = "حجب أسعار الشراء وهوامش الربح من شاشات المخزون وبطاقات السلع",
                            icon = Icons.Default.Lock,
                            isSensitive = true,
                            checked = canViewCostPrices,
                            testTag = "new_worker_perm_cost_prices",
                            onCheckedChange = { canViewCostPrices = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "عرض تقارير المبيعات والأرباح",
                            subtitle = "حجب لوحة التحكم المالية وإجمالي المبيعات والأرباح الصافية",
                            icon = Icons.Default.LockPerson,
                            isSensitive = true,
                            checked = canViewReports,
                            testTag = "new_worker_perm_view_reports",
                            onCheckedChange = { canViewReports = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "سجلات المشتريات وفواتير الموردين",
                            subtitle = "حجب فواتير الشراء وسجلات الموردين وقوائم التوريد",
                            icon = Icons.Default.Security,
                            isSensitive = true,
                            checked = canManagePurchaseInvoices,
                            testTag = "new_worker_perm_purchase_invoices",
                            onCheckedChange = { canManagePurchaseInvoices = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "إدارة ديون العملاء والحسابات",
                            subtitle = "حجب أرصدة الديون الحساسة وحسابات الزبائن الخاصة",
                            icon = Icons.Default.Group,
                            isSensitive = true,
                            checked = canManageDebtsAndCustomers,
                            testTag = "new_worker_perm_customer_debts",
                            onCheckedChange = { canManageDebtsAndCustomers = it; selectedPreset = "custom" }
                        )
                    }
                }

                // POS & DAILY OPERATIONS SECTION
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "عمليات الكاشير والمخزون اليومية:",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        PermissionToggleRow(
                            label = "إصدار فواتير البيع في الكاشير",
                            subtitle = "السماح بإتمام عمليات البيع وطباعة الفاتورة للزبون",
                            icon = Icons.Default.CheckCircle,
                            checked = canMakeSales,
                            testTag = "new_worker_perm_make_sales",
                            onCheckedChange = { canMakeSales = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "تعديل أسعار البيع في الفاتورة",
                            subtitle = "السماح بتعديل سعر بيع الصنف يدوياً أثناء إنشاء الفاتورة",
                            icon = Icons.Default.Edit,
                            checked = canEditPrices,
                            testTag = "new_worker_perm_edit_prices",
                            onCheckedChange = { canEditPrices = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "منح الخصومات للعملاء",
                            subtitle = "إمكانية إعطاء خصم نقدي أو نسبي على الفاتورة",
                            icon = Icons.Default.Info,
                            checked = canApplyDiscounts,
                            testTag = "new_worker_perm_apply_discounts",
                            onCheckedChange = { canApplyDiscounts = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "عمل مرتجعات أو إلغاء الفواتير",
                            subtitle = "استرجاع الفواتير السابقة أو حذف عمليات البيع",
                            icon = Icons.Default.WarningAmber,
                            checked = canRefundOrVoid,
                            testTag = "new_worker_perm_refund_void",
                            onCheckedChange = { canRefundOrVoid = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "إضافة وتعديل أصناف المخزون",
                            subtitle = "إمكانية إضافة منتجات جديدة وتعديل كميات المخزون",
                            icon = Icons.Default.VerifiedUser,
                            checked = canManageInventory,
                            testTag = "new_worker_perm_manage_inventory",
                            onCheckedChange = { canManageInventory = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "تصدير ملفات PDF والنسخ الاحتياطي",
                            subtitle = "تصدير ملفات PDF ومشاركة نسخ البيانات",
                            icon = Icons.Default.CloudSync,
                            checked = canExportData,
                            testTag = "new_worker_perm_export_data",
                            onCheckedChange = { canExportData = it; selectedPreset = "custom" }
                        )
                    }
                }

                if (!localError.isNullOrBlank()) {
                    Text(
                        text = localError.orEmpty(),
                        color = semantic.dangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val normalizedEmail = if (workerEmail.trim().isNotBlank() && !workerEmail.contains("@")) {
                                "${workerEmail.trim()}@afstore.com"
                            } else {
                                workerEmail.trim()
                            }
                            when {
                                workerName.trim().length < 2 -> {
                                    localError = "يرجى إدخال الاسم الكامل للعامل التابع"
                                }
                                normalizedEmail.length < 5 || !normalizedEmail.contains("@") -> {
                                    localError = "يرجى إدخال البريد الإلكتروني أو معرّف الدخول للعامل"
                                }
                                workerPin.trim().length < 4 -> {
                                    localError = "يرجى تعيين رمز سري للعامل لا يقل عن 4 خانات"
                                }
                                else -> {
                                    onCreateWorker(
                                        resolvedTargetManager,
                                        workerName.trim(),
                                        normalizedEmail,
                                        workerPhone.trim(),
                                        workerPin.trim(),
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
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("confirm_create_worker_btn")
                    ) {
                        Text("إنشاء وتفعيل حساب العامل التابع", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerSecretVerificationDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onVerified: (String) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var secretInput by rememberSaveable { mutableStateOf("") }
    var showSecret by rememberSaveable { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("owner_secret_verification_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp
                    )
                }

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = secretInput,
                    onValueChange = {
                        secretInput = it
                        errorMsg = null
                    },
                    label = { Text("الرمز السري للمالك (Deffas antar)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showSecret = !showSecret }) {
                            Icon(
                                imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "إظهار/إخفاء الرمز السري"
                            )
                        }
                    },
                    visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_owner_secret_input")
                )

                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg.orEmpty(),
                        color = semantic.dangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (StaffAccountRepository.verifyOwnerSecretCode(secretInput)) {
                                onVerified(secretInput.trim())
                            } else {
                                errorMsg = "الرمز السري للمالك (Deffas antar) غير صحيح!"
                            }
                        },
                        modifier = Modifier.testTag("confirm_owner_secret_btn")
                    ) {
                        Text("تأكيد الرمز السري", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffAccountApprovalItemCard(
    staff: StaffAccount,
    affiliatedWorkersCount: Int = 0,
    isCurrentUser: Boolean,
    isCallerOwner: Boolean,
    onQuickApproveAsWorker: () -> Unit,
    onQuickApproveAsManager: () -> Unit,
    onRejectOrSuspend: (String) -> Unit,
    onCustomizePermissions: () -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val statusColor = when (staff.approvalStatus) {
        StaffApprovalStatus.APPROVED -> semantic.profitGreen
        StaffApprovalStatus.PENDING -> semantic.debtAmber
        else -> semantic.dangerRed
    }

    val roleLabel = when {
        staff.isOwner -> "مالك المتجر الأساسي (Deffas antar)"
        staff.role == "manager" -> {
            val branchSuffix = if (staff.branchName.isNotBlank()) " • ${staff.branchName}" else ""
            "ملف مدير مستقل$branchSuffix ($affiliatedWorkersCount عمال تابعين)"
        }
        staff.requestedRole == "manager" && staff.isPending -> {
            val branchSuffix = if (staff.branchName.isNotBlank()) " (${staff.branchName})" else ""
            "طلب ملف مدير مستقل جديد$branchSuffix"
        }
        else -> {
            val parentSuffix = if (staff.parentManagerName.isNotBlank()) " • تابع للمدير: ${staff.parentManagerName}" else ""
            "عامل مبيعات (كاشير)$parentSuffix"
        }
    }

    val displayDate = remember(staff.updatedAt, staff.createdAt) {
        val ts: Timestamp? = staff.updatedAt ?: staff.createdAt
        if (ts != null) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(ts.toDate())
        } else {
            "الآن"
        }
    }

    val animatedStatusColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(durationMillis = 400),
        label = "statusColorAnim"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (staff.isPending) semantic.debtAmber else semantic.cardBorder,
        animationSpec = tween(durationMillis = 400),
        label = "cardBorderAnim"
    )

    val canCallerModifyTarget = isCallerOwner || (staff.role == "worker" && !staff.isOwner)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.cardElevated),
        border = BorderStroke(
            width = if (staff.isPending) 1.5.dp else 1.dp,
            color = animatedBorderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staff_account_card_${staff.userId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar / Initial Badge
                Surface(
                    shape = CircleShape,
                    color = animatedStatusColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, animatedStatusColor),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initial = staff.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                        Text(
                            text = initial,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = animatedStatusColor
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = staff.fullName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        if (staff.isOwner) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = semantic.profitGreen.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "المالك",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = semantic.profitGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (staff.role == "manager") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "مدير مستقل",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isCurrentUser) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "أنت",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = staff.email + (if (staff.phone.isNotBlank()) " • ${staff.phone}" else ""),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$roleLabel • $displayDate",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (staff.secretPin.isNotBlank() && !staff.isOwner) {
                        Text(
                            text = "الرمز السري للدخول المباشر: ${staff.secretPin}" +
                                (if (staff.branchName.isNotBlank()) " • الفرع: ${staff.branchName}" else ""),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = animatedStatusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, animatedStatusColor.copy(alpha = 0.45f))
                ) {
                    AnimatedContent(
                        targetState = staff.approvalStatus,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f)) togetherWith
                                (fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f))
                        },
                        label = "statusBadgeAnim"
                    ) { currentStatus ->
                        Text(
                            text = StaffApprovalStatus.labelAr(currentStatus),
                            color = animatedStatusColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Action Buttons for Approval & Role Management
            if (canCallerModifyTarget && !staff.isOwner) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!staff.isApproved || staff.role != "worker") {
                        Button(
                            onClick = onQuickApproveAsWorker,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = semantic.profitGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("approve_worker_btn_${staff.userId}")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("موافقة كعامل كاشير", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // ONLY the Store Owner (Deffas antar) can promote/approve as Independent Manager
                    if (isCallerOwner && (!staff.isApproved || staff.role != "manager")) {
                        Button(
                            onClick = onQuickApproveAsManager,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = semantic.infoBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("approve_manager_btn_${staff.userId}")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تفعيل كملف مدير مستقل", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    OutlinedButton(
                        onClick = onCustomizePermissions,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("edit_permissions_btn_${staff.userId}")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الصلاحيات", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!isCurrentUser && !StaffAccountRepository.isOwnerEmail(staff.email)) {
                        if (staff.approvalStatus == StaffApprovalStatus.PENDING) {
                            OutlinedButton(
                                onClick = { onRejectOrSuspend(StaffApprovalStatus.REJECTED) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.dangerRed),
                                border = BorderStroke(1.dp, semantic.dangerRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("reject_staff_btn_${staff.userId}")
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رفض الطلب", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (staff.approvalStatus == StaffApprovalStatus.APPROVED) {
                            OutlinedButton(
                                onClick = { onRejectOrSuspend(StaffApprovalStatus.SUSPENDED) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.dangerRed),
                                border = BorderStroke(1.dp, semantic.dangerRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("suspend_staff_btn_${staff.userId}")
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إيقاف الدخول", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffPermissionsEditorDialog(
    staff: StaffAccount,
    isCallerOwner: Boolean,
    onDismiss: () -> Unit,
    onSave: (role: String, approvalStatus: String, permissions: WorkerPermissions, ownerSecretCode: String, syncMode: String) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    var selectedRole by rememberSaveable(staff.userId) { mutableStateOf(if (isCallerOwner) staff.role else "worker") }
    var selectedStatus by rememberSaveable(staff.userId) { mutableStateOf(staff.approvalStatus) }
    val initialPerms = remember(staff) { staff.toWorkerPermissions() }

    var canMakeSales by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canMakeSales) }
    var canManageInventory by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canManageInventory) }
    var canEditPrices by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canEditPrices) }
    var canViewReports by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canViewReports) }
    var canManageDebtsAndCustomers by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canManageDebtsAndCustomers) }
    var canRefundOrVoid by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canRefundOrVoid) }
    var canApplyDiscounts by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canApplyDiscounts) }
    var canViewCostPrices by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canViewCostPrices) }
    var canExportData by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canExportData) }
    var canManagePurchaseInvoices by rememberSaveable(staff.userId) { mutableStateOf(initialPerms.canManagePurchaseInvoices) }

    var selectedPreset by rememberSaveable(staff.userId) {
        mutableStateOf(
            when {
                !initialPerms.canViewCostPrices && !initialPerms.canViewReports && !initialPerms.canManagePurchaseInvoices && !initialPerms.canManageInventory && initialPerms.canMakeSales -> "cashier"
                !initialPerms.canMakeSales && initialPerms.canManageInventory && initialPerms.canManagePurchaseInvoices -> "storekeeper"
                else -> "custom"
            }
        )
    }

    var selectedSyncMode by rememberSaveable(staff.userId) { mutableStateOf(staff.syncMode.ifBlank { "pro" }) }
    var ownerSecretInput by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val requiresOwnerSecret = selectedRole == "manager" || staff.role == "manager"

    fun applyPreset(preset: String) {
        selectedPreset = preset
        when (preset) {
            "cashier" -> {
                canMakeSales = true
                canApplyDiscounts = true
                canManageInventory = false
                canEditPrices = false
                canRefundOrVoid = false
                canViewReports = false
                canViewCostPrices = false
                canManagePurchaseInvoices = false
                canManageDebtsAndCustomers = false
                canExportData = false
            }
            "storekeeper" -> {
                canMakeSales = false
                canManageInventory = true
                canManagePurchaseInvoices = true
                canViewCostPrices = false
                canViewReports = false
                canEditPrices = false
                canRefundOrVoid = false
                canApplyDiscounts = false
                canManageDebtsAndCustomers = false
                canExportData = false
            }
            "all" -> {
                canMakeSales = true
                canManageInventory = true
                canManagePurchaseInvoices = true
                canViewCostPrices = true
                canViewReports = true
                canEditPrices = true
                canRefundOrVoid = true
                canApplyDiscounts = true
                canManageDebtsAndCustomers = true
                canExportData = true
            }
            "none" -> {
                canMakeSales = false
                canManageInventory = false
                canManagePurchaseInvoices = false
                canViewCostPrices = false
                canViewReports = false
                canEditPrices = false
                canRefundOrVoid = false
                canApplyDiscounts = false
                canManageDebtsAndCustomers = false
                canExportData = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "صلاحيات وحساب: ${staff.fullName}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = staff.email,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Account status
                Text("حالة اعتماد الحساب:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStatus == StaffApprovalStatus.APPROVED,
                        onClick = { selectedStatus = StaffApprovalStatus.APPROVED },
                        label = { Text("معتمد ✓", fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = selectedStatus == StaffApprovalStatus.PENDING,
                        onClick = { selectedStatus = StaffApprovalStatus.PENDING },
                        label = { Text("معلق ⏳", fontSize = 11.5.sp) }
                    )
                    FilterChip(
                        selected = selectedStatus == StaffApprovalStatus.SUSPENDED,
                        onClick = { selectedStatus = StaffApprovalStatus.SUSPENDED },
                        label = { Text("موقوف ⏸", fontSize = 11.5.sp) }
                    )
                }

                if (isCallerOwner) {
                    Text("الدور الوظيفي (خاص بالمالك Deffas antar):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedRole == "worker",
                            onClick = { selectedRole = "worker" },
                            label = { Text("عامل مبيعات / كاشير", fontSize = 11.5.sp) }
                        )
                        FilterChip(
                            selected = selectedRole == "manager",
                            onClick = { selectedRole = "manager" },
                            label = { Text("مدير فرع معتمد", fontSize = 11.5.sp) }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Quick presets
                Text("قوالب الصلاحيات الجاهزة (تحديد سريع):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedPreset == "cashier",
                        onClick = { applyPreset("cashier") },
                        label = { Text("كاشير 🛒", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPreset == "storekeeper",
                        onClick = { applyPreset("storekeeper") },
                        label = { Text("مخزن 📦", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPreset == "custom",
                        onClick = { selectedPreset = "custom" },
                        label = { Text("مخصص ⚙️", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { applyPreset("all") },
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تفعيل الكل", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { applyPreset("none") },
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حجب الكل 🔒", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // SENSITIVE PERMISSIONS SECTION
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المعلومات المالية والسرية (تحكم بالحجب):",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        PermissionToggleRow(
                            label = "رؤية تكلفة المنتجات وأرباحها",
                            subtitle = "حجب أسعار الشراء وهوامش الربح من شاشات المخزون وبطاقات السلع",
                            icon = Icons.Default.Lock,
                            isSensitive = true,
                            checked = canViewCostPrices,
                            testTag = "perm_cost_prices",
                            onCheckedChange = { canViewCostPrices = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "عرض تقارير المبيعات والأرباح",
                            subtitle = "حجب لوحة التحكم المالية وإجمالي المبيعات والأرباح الصافية",
                            icon = Icons.Default.LockPerson,
                            isSensitive = true,
                            checked = canViewReports,
                            testTag = "perm_view_reports",
                            onCheckedChange = { canViewReports = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "سجلات المشتريات وفواتير الموردين",
                            subtitle = "حجب فواتير الشراء وسجلات الموردين وقوائم التوريد",
                            icon = Icons.Default.Security,
                            isSensitive = true,
                            checked = canManagePurchaseInvoices,
                            testTag = "perm_purchase_invoices",
                            onCheckedChange = { canManagePurchaseInvoices = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "إدارة ديون العملاء والحسابات",
                            subtitle = "حجب أرصدة الديون الحساسة وحسابات الزبائن الخاصة",
                            icon = Icons.Default.Group,
                            isSensitive = true,
                            checked = canManageDebtsAndCustomers,
                            testTag = "perm_customer_debts",
                            onCheckedChange = { canManageDebtsAndCustomers = it; selectedPreset = "custom" }
                        )
                    }
                }

                // POS & DAILY OPERATIONS SECTION
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "عمليات الكاشير والمخزون اليومية:",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        PermissionToggleRow(
                            label = "إصدار فواتير البيع في الكاشير",
                            subtitle = "السماح بإتمام عمليات البيع وطباعة الفاتورة للزبون",
                            icon = Icons.Default.CheckCircle,
                            checked = canMakeSales,
                            testTag = "perm_make_sales",
                            onCheckedChange = { canMakeSales = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "تعديل أسعار البيع في الفاتورة",
                            subtitle = "السماح بتعديل سعر بيع الصنف يدوياً أثناء إنشاء الفاتورة",
                            icon = Icons.Default.Edit,
                            checked = canEditPrices,
                            testTag = "perm_edit_prices",
                            onCheckedChange = { canEditPrices = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "منح الخصومات للعملاء",
                            subtitle = "إمكانية إعطاء خصم نقدي أو نسبي على الفاتورة",
                            icon = Icons.Default.Info,
                            checked = canApplyDiscounts,
                            testTag = "perm_apply_discounts",
                            onCheckedChange = { canApplyDiscounts = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "عمل مرتجعات أو إلغاء الفواتير",
                            subtitle = "استرجاع الفواتير السابقة أو حذف عمليات البيع",
                            icon = Icons.Default.WarningAmber,
                            checked = canRefundOrVoid,
                            testTag = "perm_refund_void",
                            onCheckedChange = { canRefundOrVoid = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "إضافة وتعديل أصناف المخزون",
                            subtitle = "إمكانية إضافة منتجات جديدة وتعديل كميات المخزون",
                            icon = Icons.Default.VerifiedUser,
                            checked = canManageInventory,
                            testTag = "perm_manage_inventory",
                            onCheckedChange = { canManageInventory = it; selectedPreset = "custom" }
                        )

                        PermissionToggleRow(
                            label = "تصدير ملفات PDF والنسخ الاحتياطي",
                            subtitle = "تصدير ملفات PDF ومشاركة نسخ البيانات",
                            icon = Icons.Default.CloudSync,
                            checked = canExportData,
                            testTag = "perm_export_data",
                            onCheckedChange = { canExportData = it; selectedPreset = "custom" }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Sync Mode
                Text("المزامنة السحابية (Sync Mode):", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedSyncMode == "pro",
                        onClick = { selectedSyncMode = "pro" },
                        label = { Text("Pro Mode (مزامنة كاملة) ⚡", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedSyncMode == "offline" || selectedSyncMode == "lite",
                        onClick = { selectedSyncMode = "lite" },
                        label = { Text("Just Offline / Lite (بدون مزامنة) 🔒", fontSize = 11.sp) }
                    )
                }

                if (requiresOwnerSecret && isCallerOwner) {
                    OutlinedTextField(
                        value = ownerSecretInput,
                        onValueChange = {
                            ownerSecretInput = it
                            errorMsg = null
                        },
                        label = { Text("الرمز السري للمالك (Deffas antar) للتأكيد") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg.orEmpty(),
                        color = semantic.dangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (requiresOwnerSecret && !StaffAccountRepository.verifyOwnerSecretCode(ownerSecretInput)) {
                                errorMsg = "يرجى إدخال الرمز السري الصحيح للمالك (Deffas antar) لتأكيد تعديل حساب المدير."
                            } else {
                                isSaving = true
                                onSave(
                                    selectedRole,
                                    selectedStatus,
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
                                    ),
                                    ownerSecretInput.trim(),
                                    selectedSyncMode
                                )
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جارِ الحفظ...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ وتطبيق فوراً ⚡", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    label: String,
    checked: Boolean,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSensitive: Boolean = false,
    testTag: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    val semantic = LocalAFStoreSemanticColors.current
    val warningColor = semantic.warning
    val successColor = semantic.success

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSensitive && checked) {
            warningColor.copy(alpha = 0.08f)
        } else if (isSensitive && !checked) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (isSensitive) {
            BorderStroke(0.8.dp, if (checked) warningColor.copy(alpha = 0.45f) else successColor.copy(alpha = 0.35f))
        } else {
            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (isSensitive) {
                        if (checked) warningColor.copy(alpha = 0.16f) else successColor.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    },
                    modifier = Modifier
                        .offset(y = (-2).dp)
                        .size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSensitive) {
                                if (checked) warningColor else successColor
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSensitive) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (checked) warningColor.copy(alpha = 0.18f) else successColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (checked) "مفعل (مرئي) ⚠️" else "محجوب 🔒",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (checked) warningColor else successColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(if (testTag.isNotBlank()) "${testTag}_switch" else "")
            )
        }
    }
}

/**
 * Action button in the TopAppBar showing pending manager account requests
 * with an alert badge for the Store Owner.
 */
@Composable
fun TopBarManagerPendingRequestsButton(
    pendingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPending = pendingCount > 0
    val alertAmber = Color(0xFFD97706)

    Box(
        modifier = modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (hasPending) alertAmber.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
            .border(
                width = if (hasPending) 1.5.dp else 1.dp,
                color = if (hasPending) alertAmber else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = if (hasPending) 9.dp else 7.dp, vertical = 5.dp)
            .testTag("top_bar_manager_pending_requests_btn"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (hasPending) Icons.Default.NotificationsActive else Icons.Default.PersonAdd,
                contentDescription = "طلبات مدراء الفروع المعلقة: $pendingCount",
                tint = if (hasPending) alertAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            if (hasPending) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = alertAmber
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "طلب جديد ($pendingCount)",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive dialog allowing the Store Owner to review, approve, or reject
 * manager account requests incoming from other phones via Firebase Firestore sync.
 */
@Composable
fun PendingManagerRequestsDialog(
    pendingAccounts: List<StaffAccount>,
    repository: StaffAccountRepository,
    onDismiss: () -> Unit,
    liteAccounts: List<StaffAccount> = emptyList(),
    onAccountApproved: (StaffAccount) -> Unit = {},
    onAccountRejected: (StaffAccount) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val semantic = LocalAFStoreSemanticColors.current
    var processingAccountId by remember { mutableStateOf<String?>(null) }
    var actionFeedbackMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .testTag("pending_manager_requests_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "طلبات مدراء الفروع 🔔",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مزامنة سحابية فورية بين الهواتف",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                if (!actionFeedbackMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.profitGreen.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, semantic.profitGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = actionFeedbackMessage.orEmpty(),
                            color = semantic.profitGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (pendingAccounts.isEmpty() && liteAccounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = semantic.profitGreen,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "لا توجد طلبات أو إشعارات جديدة حالياً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "جميع حسابات المدراء والفروع معتمدة ومفعّلة بنجاح ✓",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (pendingAccounts.isNotEmpty()) {
                    Text(
                        text = "الطلبات المعلقة (${pendingAccounts.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    pendingAccounts.forEach { acc ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = acc.fullName.ifBlank { "مدير بدون اسم" },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    if (acc.isTrial) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFD97706).copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.7f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD97706),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "طلب Pro تجريبي (7 أيام)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFD97706)
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                                        ) {
                                            Text(
                                                text = "بانتظار الموافقة ⏳",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الفرع: ${acc.branchName.ifBlank { "فرع مستقل" }}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "البريد: ${acc.email} • الهاتف: ${acc.phone.ifBlank { "غير مسجل" }}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val isProcessing = processingAccountId == acc.userId

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                processingAccountId = acc.userId
                                                coroutineScope.launch {
                                                    val res = repository.approvePendingManagerRequest(acc, asTrial7Days = acc.isTrial)
                                                    if (res.isSuccess) {
                                                        val approved = res.getOrThrow()
                                                        actionFeedbackMessage = if (approved.isTrial) {
                                                            "تم قبول وتفعيل الحساب التجريبي Pro (7 أيام) للمدير «${approved.fullName}» بنجاح! يمكنه الدخول الآن فوراً."
                                                        } else {
                                                            "تم قبول المدير «${approved.fullName}» بنجاح! يمكنه الآن الدخول من أي هاتف."
                                                        }
                                                        onAccountApproved(approved)
                                                    } else {
                                                        actionFeedbackMessage = "فشل قبول الطلب: ${res.exceptionOrNull()?.localizedMessage}"
                                                    }
                                                    processingAccountId = null
                                                }
                                            },
                                            enabled = !isProcessing,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (acc.isTrial) Color(0xFFD97706) else semantic.profitGreen,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (isProcessing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (acc.isTrial) "قبول وتفعيل التجربة (7 أيام) ✓" else "قبول وتفعيل كامل الصلاحيات",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                processingAccountId = acc.userId
                                                coroutineScope.launch {
                                                    val res = repository.rejectPendingManagerRequest(acc)
                                                    if (res.isSuccess) {
                                                        actionFeedbackMessage = "تم رفض طلب المدير «${acc.fullName}»."
                                                        onAccountRejected(acc)
                                                    }
                                                    processingAccountId = null
                                                }
                                            },
                                            enabled = !isProcessing,
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = semantic.dangerRed
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("رفض", fontSize = 11.sp)
                                        }
                                    }

                                    if (acc.isTrial) {
                                        TextButton(
                                            onClick = {
                                                processingAccountId = acc.userId
                                                coroutineScope.launch {
                                                    val res = repository.approvePendingManagerRequest(acc, asTrial7Days = false)
                                                    if (res.isSuccess) {
                                                        val approved = res.getOrThrow()
                                                        actionFeedbackMessage = "تم تفعيل حساب المدير «${approved.fullName}» كمدير دائم بنجاح!"
                                                        onAccountApproved(approved)
                                                    }
                                                    processingAccountId = null
                                                }
                                            },
                                            enabled = !isProcessing,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "أو تفعيل دائم كمدير معتمد (بدون حد 7 أيام) ⭐",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (liteAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF059669).copy(alpha = 0.15f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                        Text(
                            text = "إشعارات حسابات Lite / Offline المنشأة (${liteAccounts.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF059669)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    liteAccounts.forEach { liteAcc ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF059669).copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = liteAcc.fullName.ifBlank { "مدير بدون اسم" },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF059669).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.7f))
                                    ) {
                                        Text(
                                            text = "مفعّل تلقائياً (مجاني) ✓",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "النشاط / الفرع: ${liteAcc.branchName.ifBlank { "فرع مستقل" }}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (liteAcc.phone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "الهاتف: ${liteAcc.phone}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (liteAcc.email.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "البريد: ${liteAcc.email}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "حساب Lite مجاني لا يتطلب موافقة المالك - للتواصل والتوثيق فقط",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
