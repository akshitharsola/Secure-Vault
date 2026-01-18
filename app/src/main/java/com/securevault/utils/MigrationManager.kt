// app/src/main/java/com/securevault/utils/MigrationManager.kt
package com.securevault.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.securevault.data.local.PasswordDao
import com.securevault.data.local.PasswordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles migration from plain text to encrypted password storage
 *
 * Migration Path:
 * v1.0-1.5.1: Passwords stored in PLAIN TEXT in database
 * v2.0.0+: Passwords encrypted with Android Keystore before database storage
 *
 * This manager:
 * 1. Detects if migration is needed
 * 2. Re-encrypts all existing passwords using Android Keystore
 * 3. Deletes legacy SharedPreferences encryption key
 * 4. Marks migration complete (runs only once)
 */
class MigrationManager(
    private val context: Context,
    private val passwordDao: PasswordDao,
    private val securityManager: SecurityManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        MIGRATION_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "MigrationManager"
        private const val MIGRATION_PREFS_NAME = "migration_prefs"
        private const val MIGRATION_COMPLETED_KEY = "db_encryption_migration_v2_completed"
        private const val MIGRATION_VERSION_KEY = "migration_version"
        private const val CURRENT_MIGRATION_VERSION = 2  // v2.0.0 migration
    }

    /**
     * Check if migration is needed and perform it
     *
     * This method is safe to call multiple times - it will only migrate once.
     * Should be called early in app startup (MainActivity.onCreate)
     *
     * @return MigrationResult indicating success/failure and details
     */
    suspend fun checkAndMigrateIfNeeded(): MigrationResult = withContext(Dispatchers.IO) {
        try {
            // Check if migration already completed
            if (isMigrationCompleted()) {
                Log.d(TAG, "Migration already completed, skipping")
                return@withContext MigrationResult.AlreadyMigrated
            }

            Log.i(TAG, "Starting database encryption migration...")

            // Get all passwords (currently in plain text or mixed state)
            val allPasswords = passwordDao.getAllPasswords()

            if (allPasswords.isEmpty()) {
                Log.i(TAG, "No passwords to migrate")
                markMigrationComplete()
                return@withContext MigrationResult.Success(0)
            }

            Log.i(TAG, "Found ${allPasswords.size} passwords to migrate")

            // Migrate each password
            var migratedCount = 0
            var errorCount = 0

            allPasswords.forEach { entity ->
                try {
                    // Check if password is already encrypted
                    if (isPasswordEncrypted(entity.password)) {
                        Log.d(TAG, "Password ${entity.id} already encrypted, skipping")
                        migratedCount++
                    } else {
                        // Password is in plain text - encrypt it
                        Log.d(TAG, "Encrypting password ${entity.id}")
                        val encryptedPassword = securityManager.encrypt(entity.password)

                        // Update entity with encrypted password
                        val encryptedEntity = entity.copy(password = encryptedPassword)
                        passwordDao.updatePassword(encryptedEntity)

                        migratedCount++
                        Log.d(TAG, "Successfully encrypted password ${entity.id}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate password ${entity.id}: ${e.message}", e)
                    errorCount++
                }
            }

            // Delete legacy SharedPreferences encryption key
            deleteLegacyEncryptionKey()

            // Mark migration complete
            markMigrationComplete()

            Log.i(TAG, "Migration completed: $migratedCount migrated, $errorCount errors")

            if (errorCount > 0) {
                MigrationResult.PartialSuccess(migratedCount, errorCount)
            } else {
                MigrationResult.Success(migratedCount)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Migration failed catastrophically", e)
            MigrationResult.Failure(e.message ?: "Unknown error")
        }
    }

    /**
     * Checks if a password string is already encrypted
     *
     * Encrypted passwords are Base64-encoded and start with a specific pattern
     * Plain text passwords are just regular strings
     *
     * @param password The password string to check
     * @return true if password appears to be encrypted
     */
    private fun isPasswordEncrypted(password: String): Boolean {
        return try {
            // Encrypted passwords are Base64 encoded
            // They should decode to binary data with IV (12 bytes) + encrypted data
            val decoded = android.util.Base64.decode(password, android.util.Base64.NO_WRAP)

            // Encrypted data must be at least IV_LENGTH (12) + some encrypted bytes
            // Typical encrypted password is 12 (IV) + 16 (min AES block) = 28+ bytes
            if (decoded.size < 28) {
                return false
            }

            // Try to decrypt - if it fails, it's not encrypted or corrupted
            // If it succeeds, it was already encrypted
            try {
                securityManager.decrypt(password)
                true  // Decryption succeeded, it's encrypted
            } catch (e: Exception) {
                // Decryption failed - either plain text or corrupted
                false
            }
        } catch (e: Exception) {
            // Not valid Base64 - definitely plain text
            false
        }
    }

    /**
     * Deletes the legacy encryption key from SharedPreferences
     *
     * Legacy versions (v1.0-1.5.1) stored encryption keys in SharedPreferences
     * which is insecure. After migration to Android Keystore, we delete the old key.
     */
    private fun deleteLegacyEncryptionKey() {
        if (securityManager.hasLegacyKey()) {
            securityManager.deleteLegacyKey()
            Log.i(TAG, "Deleted legacy encryption key from SharedPreferences")
        } else {
            Log.d(TAG, "No legacy encryption key found")
        }
    }

    /**
     * Marks migration as completed
     */
    private fun markMigrationComplete() {
        prefs.edit()
            .putBoolean(MIGRATION_COMPLETED_KEY, true)
            .putInt(MIGRATION_VERSION_KEY, CURRENT_MIGRATION_VERSION)
            .putLong("migration_timestamp", System.currentTimeMillis())
            .apply()

        Log.i(TAG, "Migration marked as complete")
    }

    /**
     * Checks if migration has already been completed
     *
     * @return true if migration already completed
     */
    private fun isMigrationCompleted(): Boolean {
        return prefs.getBoolean(MIGRATION_COMPLETED_KEY, false)
    }

    /**
     * Gets the migration version that was last completed
     *
     * @return migration version, or 0 if never migrated
     */
    fun getMigrationVersion(): Int {
        return prefs.getInt(MIGRATION_VERSION_KEY, 0)
    }

    /**
     * Resets migration state (for testing only - DO NOT USE IN PRODUCTION)
     */
    fun resetMigrationStateForTesting() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Only allow in debug builds
            if (android.os.Build.TYPE != "user") {
                prefs.edit().clear().apply()
                Log.w(TAG, "Migration state reset (TESTING ONLY)")
            }
        }
    }
}

/**
 * Result of migration operation
 */
sealed class MigrationResult {
    /**
     * Migration completed successfully
     * @param migratedCount Number of passwords migrated
     */
    data class Success(val migratedCount: Int) : MigrationResult()

    /**
     * Migration partially succeeded
     * @param migratedCount Number of passwords successfully migrated
     * @param errorCount Number of passwords that failed to migrate
     */
    data class PartialSuccess(val migratedCount: Int, val errorCount: Int) : MigrationResult()

    /**
     * Migration already completed in a previous run
     */
    object AlreadyMigrated : MigrationResult()

    /**
     * Migration failed completely
     * @param error Error message
     */
    data class Failure(val error: String) : MigrationResult()
}

/**
 * Exception thrown when migration fails
 */
class MigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)
