// app/src/main/java/com/securevault/utils/BackupManager.kt
package com.securevault.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.securevault.data.model.BackupData
import com.securevault.data.model.BackupResult
import com.securevault.data.model.Password
import com.securevault.data.model.RestoreResult
import com.securevault.data.repository.PasswordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enhanced backup and restore manager with external storage and sharing support
 */
class BackupManager(
    private val context: Context,
    private val passwordRepository: PasswordRepository
) {

    private val backupEncryption = BackupEncryption()
    private val fileManager = FileManager(context)
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()

    companion object {
        private const val TAG = "BackupManager"
    }

    /**
     * Creates an encrypted backup with external storage support
     */
    suspend fun createBackup(
        password: String,
        fileName: String? = null
    ): EnhancedBackupResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting backup creation")

            // Get all passwords from repository
            val passwords = passwordRepository.getAllPasswordsList()

            if (passwords.isEmpty()) {
                return@withContext EnhancedBackupResult.Error("No passwords to backup")
            }

            // Convert passwords to JSON
            val passwordsJson = gson.toJson(passwords)
            Log.d(TAG, "Serialized ${passwords.size} passwords to JSON")

            // Encrypt the JSON data
            val encryptedData = backupEncryption.encrypt(passwordsJson, password)
            Log.d(TAG, "Password data encrypted successfully")

            // Create backup data structure
            val backupData = BackupData(
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
                passwordCount = passwords.size,
                data = encryptedData
            )

            // Convert backup data to JSON
            val backupJson = gson.toJson(backupData)

            // Save to both internal and external storage
            val saveResult = fileManager.saveBackupWithSharing(backupJson, fileName)

            Log.i(TAG, "Backup created successfully")
            EnhancedBackupResult.Success(
                passwordCount = passwords.size,
                internalPath = saveResult.internalPath,
                externalPath = saveResult.externalPath,
                fileName = saveResult.fileName,
                isExternalAvailable = saveResult.isExternalAvailable
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            EnhancedBackupResult.Error("Failed to create backup: ${e.message}", e)
        }
    }

    /**
     * Creates a backup and saves to user-selected location
     */
    suspend fun createBackupToUri(
        password: String,
        destinationUri: Uri
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting backup creation to URI: $destinationUri")

            val passwords = passwordRepository.getAllPasswordsList()

            if (passwords.isEmpty()) {
                return@withContext BackupResult.Error("No passwords to backup")
            }

            // Create backup data
            val passwordsJson = gson.toJson(passwords)
            val encryptedData = backupEncryption.encrypt(passwordsJson, password)

            val backupData = BackupData(
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
                passwordCount = passwords.size,
                data = encryptedData
            )

            val backupJson = gson.toJson(backupData)

            // Save directly to user-selected location
            val success = fileManager.saveBackupToUri(backupJson, destinationUri)

            if (success) {
                Log.i(TAG, "Backup saved to user-selected location")
                BackupResult.Success(destinationUri.toString(), passwords.size)
            } else {
                BackupResult.Error("Failed to save backup to selected location")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup to URI", e)
            BackupResult.Error("Failed to create backup: ${e.message}", e)
        }
    }

    /**
     * Creates a share intent for a backup file
     */
    fun createShareIntent(filePath: String): Intent? {
        return fileManager.createShareIntent(filePath)
    }

    /**
     * Gets backup directory information for user
     */
    fun getBackupLocationInfo(): BackupLocationInfo {
        return BackupLocationInfo(
            internalPath = context.filesDir.absolutePath + "/backups",
            externalPath = fileManager.getBackupDirectoryPath(),
            isExternalAvailable = fileManager.isExternalStorageAvailable()
        )
    }

    /**
     * Restores passwords from an encrypted backup file
     */
    suspend fun restoreFromFile(
        filePath: String,
        password: String,
        replaceAll: Boolean = true
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting restore from file: $filePath")

            // Read backup file
            val backupJson = fileManager.readBackupFromFile(filePath)

            return@withContext restoreFromData(backupJson, password, replaceAll)

        } catch (e: FileOperationException) {
            Log.e(TAG, "File operation error during restore", e)
            RestoreResult.Error("Failed to read backup file: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from file", e)
            RestoreResult.Error("Failed to restore backup: ${e.message}", e)
        }
    }

    /**
     * Restores passwords from a backup file selected via document picker
     */
    suspend fun restoreFromUri(
        uri: Uri,
        password: String,
        replaceAll: Boolean = true
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting restore from URI: $uri")

            // Read backup file from URI
            val backupJson = fileManager.readBackupFromUri(uri)

            return@withContext restoreFromData(backupJson, password, replaceAll)

        } catch (e: FileOperationException) {
            Log.e(TAG, "File operation error during restore from URI", e)
            RestoreResult.Error("Failed to read backup file: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from URI", e)
            RestoreResult.Error("Failed to restore backup: ${e.message}", e)
        }
    }

    /**
     * Common restore logic for both file and URI-based restore
     */
    private suspend fun restoreFromData(
        backupJson: String,
        password: String,
        replaceAll: Boolean
    ): RestoreResult {
        try {
            // Try to parse as new format first
            val backupData = try {
                gson.fromJson(backupJson, BackupData::class.java)
            } catch (e: Exception) {
                null
            }

            // Try to parse as legacy obfuscated format
            val legacyBackupData = try {
                gson.fromJson(backupJson, com.securevault.data.model.LegacyBackupData::class.java)
            } catch (e: Exception) {
                null
            }

            // Determine which format and extract encrypted data
            val encryptedData = when {
                // New format: has BackupData wrapper with data field
                backupData != null && !backupData.data.isNullOrBlank() -> {
                    Log.d(TAG, "Detected new backup format - Version: ${backupData.version}, Count: ${backupData.passwordCount}")
                    backupData.data
                }
                // Legacy format: obfuscated keys (a, b, c, d, e, f)
                legacyBackupData != null && !legacyBackupData.data.isNullOrBlank() -> {
                    Log.d(TAG, "Detected legacy obfuscated backup format - Count: ${legacyBackupData.passwordCount}")
                    legacyBackupData.data
                }
                // Very old format: entire file is encrypted data (no JSON wrapper)
                else -> {
                    Log.d(TAG, "Detected very old backup format (raw encrypted data)")
                    backupJson.trim()
                }
            }

            // Decrypt the password data
            val decryptedJson = try {
                backupEncryption.decrypt(encryptedData, password)
            } catch (e: DecryptionException) {
                Log.w(TAG, "Failed to decrypt backup - invalid password")
                return RestoreResult.InvalidPassword()
            } catch (e: Exception) {
                Log.e(TAG, "Decryption error", e)
                return RestoreResult.Error("Failed to decrypt backup: ${e.message}")
            }

            // Parse passwords from decrypted JSON - try multiple formats
            var passwords: List<Password> = emptyList()
            var formatUsed = "unknown"

            // Try new format first (standard field names: id, title, username, password, notes, createdAt, updatedAt)
            try {
                val passwordDtoType = object : com.google.gson.reflect.TypeToken<List<com.securevault.data.model.PasswordDto>>() {}.type
                val passwordDtos: List<com.securevault.data.model.PasswordDto>? = gson.fromJson(decryptedJson, passwordDtoType)

                if (passwordDtos != null) {
                    passwords = passwordDtos.mapNotNull { it.toPassword() }
                    if (passwords.isNotEmpty()) {
                        formatUsed = "new format"
                        Log.d(TAG, "Successfully parsed using new format - ${passwords.size} passwords")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to parse as new format: ${e.message}")
            }

            // If new format failed or got no valid passwords, try legacy obfuscated format (a, b, c, d, e, f, g)
            if (passwords.isEmpty()) {
                try {
                    val legacyPasswordDtoType = object : com.google.gson.reflect.TypeToken<List<com.securevault.data.model.LegacyPasswordDto>>() {}.type
                    val legacyPasswordDtos: List<com.securevault.data.model.LegacyPasswordDto>? = gson.fromJson(decryptedJson, legacyPasswordDtoType)

                    if (legacyPasswordDtos != null) {
                        passwords = legacyPasswordDtos.mapNotNull { dto ->
                            val password = dto.toPassword()
                            if (password == null) {
                                Log.w(TAG, "Skipping invalid legacy password entry - missing required fields")
                                Log.d(TAG, "  Legacy DTO: id=${dto.id}, title=${dto.title}, username=${dto.username}")
                            }
                            password
                        }
                        if (passwords.isNotEmpty()) {
                            formatUsed = "legacy obfuscated format"
                            Log.d(TAG, "Successfully parsed using legacy obfuscated format - ${passwords.size} passwords")
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to parse as legacy format: ${e.message}")
                }
            }

            if (passwords.isEmpty()) {
                return RestoreResult.Error("No valid passwords found in backup (tried multiple formats)")
            }

            Log.d(TAG, "Successfully decrypted and validated ${passwords.size} passwords using $formatUsed")

            // Use repository method for replacing all passwords
            val success = if (replaceAll) {
                passwordRepository.replaceAllPasswords(passwords)
            } else {
                passwordRepository.savePasswords(passwords)
            }

            if (!success) {
                return RestoreResult.Error("Failed to save passwords to database")
            }

            Log.i(TAG, "Restore completed - ${passwords.size} passwords processed")
            return RestoreResult.Success(passwords.size, passwords.size)

        } catch (e: Exception) {
            Log.e(TAG, "Error during restore operation", e)
            return RestoreResult.Error("Failed to restore passwords: ${e.message}", e)
        }
    }

    /**
     * Lists all available backup files from both internal and external storage
     */
    suspend fun listBackupFiles(): List<BackupFileInfo> = withContext(Dispatchers.IO) {
        fileManager.listAllBackupFiles()
    }

    /**
     * Validates a backup file and password without performing the restore
     */
    suspend fun validateBackup(filePath: String, password: String): BackupData? = withContext(Dispatchers.IO) {
        try {
            val backupJson = fileManager.readBackupFromFile(filePath)
            val backupData = gson.fromJson(backupJson, BackupData::class.java) ?: return@withContext null

            // Try to decrypt to validate password
            val isValidPassword = backupEncryption.validatePassword(backupData.data, password)

            return@withContext if (isValidPassword) backupData else null
        } catch (e: Exception) {
            Log.e(TAG, "Error validating backup", e)
            null
        }
    }

    /**
     * Deletes a backup file
     */
    suspend fun deleteBackupFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        fileManager.deleteBackupFile(filePath)
    }

    /**
     * Cleans up old backup files
     */
    suspend fun cleanupOldBackups(keepCount: Int = 10) = withContext(Dispatchers.IO) {
        fileManager.cleanupOldBackups(keepCount)
    }
}

/**
 * Enhanced backup result with external storage information
 */
sealed class EnhancedBackupResult {
    data class Success(
        val passwordCount: Int,
        val internalPath: String,
        val externalPath: String?,
        val fileName: String,
        val isExternalAvailable: Boolean
    ) : EnhancedBackupResult()

    data class Error(val message: String, val exception: Throwable? = null) : EnhancedBackupResult()
}

/**
 * Backup location information
 */
data class BackupLocationInfo(
    val internalPath: String,
    val externalPath: String,
    val isExternalAvailable: Boolean
)