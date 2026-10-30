package com.example.data.backup

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.firebase.StaffAccount
import com.example.data.firebase.StaffAccountRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

object CloudDatabaseBackupManager {
    private const val TAG = "CloudDatabaseBackup"
    private const val COLLECTION_CLOUD_BACKUPS = "cloud_backups"

    /**
     * Uploads an encrypted backup snapshot of the local database to Firebase Firestore.
     * Restricted strictly to Pro accounts (or Owner / active trial Pro). Lite accounts are rejected.
     */
    suspend fun uploadCloudBackup(
        context: Context,
        staffAccount: StaffAccount?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (staffAccount != null && !staffAccount.isOwner && staffAccount.syncMode != "pro" && !(staffAccount.isTrial && !staffAccount.isTrialExpired)) {
                return@withContext Result.failure(
                    SecurityException("النسخ الاحتياطي السحابي مخصص حصرياً لحسابات Pro. حسابات Lite غير مسموح لها بالنسخ السحابي.")
                )
            }

            val auth = FirebaseAuth.getInstance()
            val userId = staffAccount?.userId?.takeIf { it.isNotBlank() } ?: auth.currentUser?.uid ?: "default_store_user"

            // 1. Create local encrypted backup file
            val localBackupResult = DatabaseBackupManager.createBackup(context, isAuto = true)
            if (localBackupResult.isFailure) {
                return@withContext Result.failure(localBackupResult.exceptionOrNull() ?: IllegalStateException("تعذر إنشاء النسخة المحلية للرفع السحابي"))
            }

            val metadata = localBackupResult.getOrThrow()
            val backupFile = metadata.file
            if (!backupFile.exists()) {
                return@withContext Result.failure(IllegalStateException("ملف النسخة الاحتياطية غير موجود"))
            }

            val encryptedBytes = backupFile.readBytes()
            val base64Data = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            val db = StaffAccountRepository.getFirestoreDb(context)
            val backupDocMap = mapOf(
                "userId" to userId,
                "fileName" to metadata.fileName,
                "dateMillis" to metadata.dateMillis,
                "formattedDate" to metadata.formattedDate,
                "sizeBytes" to metadata.sizeBytes,
                "base64Data" to base64Data,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("users").document(userId)
                .collection(COLLECTION_CLOUD_BACKUPS)
                .document("latest")
                .set(backupDocMap)
                .await()

            Log.i(TAG, "Successfully uploaded encrypted cloud backup for user $userId (${metadata.formattedSize})")
            Result.success("تم رفع النسخة الاحتياطية السحابية بنجاح (${metadata.formattedSize}) ✓")
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading cloud backup", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads and restores database snapshot from Firebase Firestore for cross-device migration.
     * Restricted strictly to Pro accounts.
     */
    suspend fun restoreCloudBackup(
        context: Context,
        staffAccount: StaffAccount?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (staffAccount != null && !staffAccount.isOwner && staffAccount.syncMode != "pro" && !(staffAccount.isTrial && !staffAccount.isTrialExpired)) {
                return@withContext Result.failure(
                    SecurityException("استعادة النسخ الاحتياطي السحابي مخصصة حصرياً لحسابات Pro.")
                )
            }

            val auth = FirebaseAuth.getInstance()
            val userId = staffAccount?.userId?.takeIf { it.isNotBlank() } ?: auth.currentUser?.uid ?: "default_store_user"

            val db = StaffAccountRepository.getFirestoreDb(context)
            val docSnap = db.collection("users").document(userId)
                .collection(COLLECTION_CLOUD_BACKUPS)
                .document("latest")
                .get()
                .await()

            if (!docSnap.exists()) {
                return@withContext Result.failure(IllegalStateException("لا توجد نسخة احتياطية سحابية محفوظة لهذا الحساب."))
            }

            val base64Data = docSnap.getString("base64Data")
            val fileName = docSnap.getString("fileName") ?: "cloud_backup.db.enc"
            val dateMillis = docSnap.getLong("dateMillis") ?: System.currentTimeMillis()

            if (base64Data.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("بيانات النسخة السحابية تالفة أو فارغة."))
            }

            val encryptedBytes = Base64.decode(base64Data, Base64.NO_WRAP)
            val backupDir = File(context.filesDir, ".antar_backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val restoreFile = File(backupDir, fileName)
            restoreFile.writeBytes(encryptedBytes)

            val restoreResult = DatabaseBackupManager.restoreBackup(context, restoreFile)
            if (restoreResult.isFailure) {
                return@withContext Result.failure(restoreResult.exceptionOrNull() ?: IllegalStateException("فشل استعادة قاعدة البيانات من النسخة السحابية"))
            }

            Log.i(TAG, "Successfully restored cloud backup from Firestore for user $userId")
            Result.success("تم استعادة النسخة الاحتياطية السحابية بنجاح واستئناف العمل على الجهاز الجديد ✓")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring cloud backup", e)
            Result.failure(e)
        }
    }
}
