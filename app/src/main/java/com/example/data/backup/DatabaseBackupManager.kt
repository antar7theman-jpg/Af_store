package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.database.AntarDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class BackupMetadata(
    val file: File,
    val fileName: String,
    val dateMillis: Long,
    val formattedDate: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val isAuto: Boolean,
    val isEncrypted: Boolean = true
)

object DatabaseBackupManager {
    private const val TAG = "DatabaseBackupManager"
    private const val BACKUP_DIR_NAME = ".antar_backups"
    private const val DB_NAME = "antar_store_database"
    private const val RETENTION_DAYS = 7
    private const val PREFS_NAME = "antar_backup_prefs"
    private const val KEY_LAST_AUTO_BACKUP = "last_auto_backup_timestamp"

    // Key derived for AES-256-GCM local storage encryption
    private val BACKUP_SECRET_SALT = "AntarStoreBackupSecretKey2026!#$*SafeVault".toByteArray(Charsets.UTF_8)
    private val AES_KEY: SecretKey by lazy {
        val sha256 = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha256.digest(BACKUP_SECRET_SALT)
        SecretKeySpec(keyBytes, "AES")
    }

    private fun getBackupDirectory(context: Context): File {
        val dir = File(context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }

    /**
     * Executes SQLite WAL checkpoint to ensure all uncommitted WAL pages are flushed to the main database file.
     */
    private fun checkpointWal(context: Context) {
        try {
            val db = AntarDatabase.getDatabase(context)
            val openHelper = db.openHelper.writableDatabase
            val cursor = openHelper.query("PRAGMA wal_checkpoint(FULL)")
            cursor.moveToFirst()
            cursor.close()
        } catch (e: Exception) {
            Log.w(TAG, "WAL checkpoint warning: ${e.message}")
        }
    }

    /**
     * Computes SHA-256 hash for byte array
     */
    private fun computeSha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    /**
     * Encrypts raw database bytes using AES-256-GCM.
     * Output format: [12 bytes IV] + [32 bytes SHA-256 Checksum] + [Encrypted Payload with 128-bit Auth Tag]
     */
    fun encryptDatabaseBytes(rawBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, AES_KEY, spec)

        val checksum = computeSha256(rawBytes)
        val encryptedData = cipher.doFinal(rawBytes)

        val output = ByteArrayOutputStream()
        output.write(iv)
        output.write(checksum)
        output.write(encryptedData)
        return output.toByteArray()
    }

    /**
     * Decrypts encrypted envelope and validates SHA-256 checksum.
     */
    fun decryptDatabaseBytes(encryptedEnvelope: ByteArray): ByteArray {
        if (encryptedEnvelope.size < 44) {
            throw IllegalArgumentException("ملف النسخة الاحتياطية تالف أو غير صالح (الحجم أقل من الحد الأدنى)")
        }

        val iv = encryptedEnvelope.copyOfRange(0, 12)
        val expectedChecksum = encryptedEnvelope.copyOfRange(12, 44)
        val encryptedPayload = encryptedEnvelope.copyOfRange(44, encryptedEnvelope.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, AES_KEY, spec)

        val decryptedBytes = try {
            cipher.doFinal(encryptedPayload)
        } catch (e: Exception) {
            throw SecurityException("فشل التحقق من سلامة البيانات: ملف النسخة الاحتياطية معدل أو تالف (${e.javaClass.simpleName})", e)
        }

        val actualChecksum = computeSha256(decryptedBytes)

        if (!MessageDigest.isEqual(expectedChecksum, actualChecksum)) {
            throw SecurityException("فشل التحقق من سلامة البيانات: ملف النسخة الاحتياطية معدل أو تالف (Checksum Mismatch)")
        }

        return decryptedBytes
    }

    /**
     * Creates a new encrypted backup snapshot in the hidden backup directory.
     */
    suspend fun createBackup(context: Context, isAuto: Boolean = false): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            checkpointWal(context)

            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                return@withContext Result.failure(IllegalStateException("قاعدة البيانات الرئيسية غير موجودة"))
            }

            val rawBytes = dbFile.readBytes()
            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("قاعدة البيانات فارغة"))
            }

            val encryptedBytes = encryptDatabaseBytes(rawBytes)

            val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val now = System.currentTimeMillis()
            val prefix = if (isAuto) "auto_backup" else "manual_backup"
            val fileName = "${prefix}_${timeFormat.format(Date(now))}.antar_enc"

            val backupDir = getBackupDirectory(context)
            val backupFile = File(backupDir, fileName)

            FileOutputStream(backupFile).use { it.write(encryptedBytes) }

            if (isAuto) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_AUTO_BACKUP, now).apply()
            }

            pruneOldBackups(context)

            val readableFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
            val metadata = BackupMetadata(
                file = backupFile,
                fileName = fileName,
                dateMillis = now,
                formattedDate = readableFormat.format(Date(now)),
                sizeBytes = backupFile.length(),
                formattedSize = formatFileSize(backupFile.length()),
                isAuto = isAuto,
                isEncrypted = true
            )
            Result.success(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database backup: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Restores database from an encrypted backup file.
     */
    suspend fun restoreBackup(context: Context, backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists() || backupFile.length() == 0L) {
                return@withContext Result.failure(IllegalArgumentException("ملف النسخة الاحتياطية غير موجود"))
            }

            val encryptedBytes = backupFile.readBytes()
            val decryptedBytes = decryptDatabaseBytes(encryptedBytes)

            // Validate that the decrypted bytes represent a valid SQLite database header ("SQLite format 3\000")
            val sqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
            if (decryptedBytes.size < 16 || !decryptedBytes.copyOfRange(0, 16).contentEquals(sqliteHeader)) {
                return@withContext Result.failure(IllegalArgumentException("الملف المسترجع لا يمثل قاعدة بيانات SQLite صالحة"))
            }

            val dbFile = context.getDatabasePath(DB_NAME)
            val walFile = File(dbFile.parentFile, "$DB_NAME-wal")
            val shmFile = File(dbFile.parentFile, "$DB_NAME-shm")

            // Create temporary safety backup of current db before overwriting
            val safetyFile = File(context.cacheDir, "pre_restore_safety_${System.currentTimeMillis()}.tmp")
            if (dbFile.exists()) {
                dbFile.copyTo(safetyFile, overwrite = true)
            }

            try {
                // Delete WAL and SHM files to prevent journal corruption
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                FileOutputStream(dbFile).use { it.write(decryptedBytes) }

                // Test opening database to verify structural integrity
                val testDb = AntarDatabase.getDatabase(context)
                testDb.openHelper.readableDatabase.version

                safetyFile.delete()
                Result.success(Unit)
            } catch (restoreError: Exception) {
                // Rollback to safety copy if restore failed
                if (safetyFile.exists()) {
                    safetyFile.copyTo(dbFile, overwrite = true)
                    safetyFile.delete()
                }
                throw restoreError
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves all available backups from the hidden folder, sorted by newest first.
     */
    suspend fun getAvailableBackups(context: Context): List<BackupMetadata> = withContext(Dispatchers.IO) {
        val backupDir = getBackupDirectory(context)
        val files = backupDir.listFiles { file -> file.isFile && file.name.endsWith(".antar_enc") } ?: emptyArray()

        val readableFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        files.map { file ->
            val isAuto = file.name.startsWith("auto_backup")
            BackupMetadata(
                file = file,
                fileName = file.name,
                dateMillis = file.lastModified(),
                formattedDate = readableFormat.format(Date(file.lastModified())),
                sizeBytes = file.length(),
                formattedSize = formatFileSize(file.length()),
                isAuto = isAuto,
                isEncrypted = true
            )
        }.sortedByDescending { it.dateMillis }
    }

    /**
     * Deletes backups older than RETENTION_DAYS (7 days).
     */
    suspend fun pruneOldBackups(context: Context, retentionDays: Int = RETENTION_DAYS) = withContext(Dispatchers.IO) {
        try {
            val backupDir = getBackupDirectory(context)
            val files = backupDir.listFiles { file -> file.isFile && file.name.endsWith(".antar_enc") } ?: emptyArray()
            val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000)

            files.forEach { file ->
                if (file.lastModified() < cutoff) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pruning old backups: ${e.message}")
        }
    }

    /**
     * Checks if daily auto-backup should run and executes it if > 24 hours since last auto-backup.
     */
    suspend fun checkAndPerformDailyAutoBackup(context: Context) = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastBackup = prefs.getLong(KEY_LAST_AUTO_BACKUP, 0L)
            val now = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000L

            if (now - lastBackup >= oneDayMillis) {
                Log.i(TAG, "Running daily auto-backup...")
                createBackup(context, isAuto = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Daily auto-backup check failed: ${e.message}")
        }
    }

    /**
     * Exports a backup file to an external user-selected Uri.
     */
    suspend fun exportBackupToUri(context: Context, backupFile: File, destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(backupFile).use { input ->
                    input.copyTo(out)
                }
            } ?: return@withContext Result.failure(IllegalStateException("تعذر فتح مسار التصدير المحدد"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports an external backup file from Uri and verifies/restores it.
     */
    suspend fun importAndRestoreFromUri(context: Context, sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "imported_backup_${System.currentTimeMillis()}.antar_enc")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { out ->
                    input.copyTo(out)
                }
            } ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة ملف النسخة الاحتياطية"))

            val result = restoreBackup(context, tempFile)
            tempFile.delete()
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Returns the timestamp of the last recorded auto-backup.
     */
    fun getLastAutoBackupTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_AUTO_BACKUP, 0L)
    }
}
