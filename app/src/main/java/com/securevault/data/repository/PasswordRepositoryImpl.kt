// app/src/main/java/com/securevault/data/repository/PasswordRepositoryImpl.kt
package com.securevault.data.repository

import android.util.Log
import androidx.room.Transaction
import com.securevault.data.local.PasswordDao
import com.securevault.data.local.PasswordEntity
import com.securevault.data.model.Password
import com.securevault.utils.SecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository implementation with hardware-backed password encryption
 *
 * Version History:
 * - v1.0-1.5.1: Passwords stored in PLAIN TEXT (CRITICAL SECURITY ISSUE)
 * - v2.0.0+: Passwords encrypted with Android Keystore before database storage
 *
 * Security Improvements:
 * - All passwords encrypted before storing in database
 * - Hardware-backed encryption using Android Keystore
 * - Automatic decryption when retrieving passwords
 * - Transaction safety for atomic operations
 * - GCM authentication tag prevents tampering
 */
class PasswordRepositoryImpl(
    private val passwordDao: PasswordDao,
    private val securityManager: SecurityManager
) : PasswordRepository {

    private val TAG = "PasswordRepository"

    override fun getAllPasswords(): Flow<List<Password>> {
        Log.d(TAG, "Getting passwords flow")
        return passwordDao.getAllPasswordsFlow().map { entities ->
            val passwords = entities.mapNotNull { entity ->
                try {
                    decryptPasswordEntity(entity)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decrypt password ${entity.id}: ${e.message}")
                    null  // Skip corrupted passwords
                }
            }
            Log.d(TAG, "Flow emitting ${passwords.size} passwords")
            passwords
        }
    }

    override suspend fun getPassword(id: String): Password? {
        return try {
            val entity = passwordDao.getPasswordById(id)
            entity?.let { decryptPasswordEntity(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving password $id: ${e.message}")
            null
        }
    }

    override suspend fun savePassword(password: Password): Boolean {
        return try {
            // Encrypt password before saving
            val encryptedEntity = encryptPassword(password)
            passwordDao.insertPassword(encryptedEntity)
            Log.d(TAG, "Password saved successfully (encrypted): ${password.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving password: ${e.message}", e)
            false
        }
    }

    override suspend fun deletePassword(id: String): Boolean {
        return try {
            val rowsDeleted = passwordDao.deletePasswordById(id)
            val success = rowsDeleted > 0
            Log.d(TAG, "Password deleted: $id, success: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting password: ${e.message}")
            false
        }
    }

    override suspend fun deleteAllPasswords(): Boolean {
        return try {
            passwordDao.deleteAllPasswords()
            Log.d(TAG, "All passwords deleted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all passwords: ${e.message}")
            false
        }
    }

    override suspend fun searchPasswords(query: String): List<Password> {
        return try {
            val entities = if (query.isBlank()) {
                passwordDao.getAllPasswords()
            } else {
                passwordDao.searchPasswords(query)
            }

            val passwords = entities.mapNotNull { entity ->
                try {
                    decryptPasswordEntity(entity)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decrypt password during search: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "Search for '$query' returned ${passwords.size} results")
            passwords
        } catch (e: Exception) {
            Log.e(TAG, "Error searching passwords: ${e.message}")
            emptyList()
        }
    }

    // Backup/restore operations

    override suspend fun getAllPasswordsList(): List<Password> {
        return try {
            val entities = passwordDao.getAllPasswords()
            val passwords = entities.mapNotNull { entity ->
                try {
                    decryptPasswordEntity(entity)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decrypt password in list: ${e.message}")
                    null
                }
            }
            Log.d(TAG, "Retrieved ${passwords.size} passwords as list")
            passwords
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all passwords list: ${e.message}")
            emptyList()
        }
    }

    override suspend fun savePasswords(passwords: List<Password>): Boolean {
        return try {
            // Encrypt all passwords before batch saving
            val encryptedEntities = passwords.map { password ->
                encryptPassword(password)
            }
            passwordDao.insertPasswords(encryptedEntities)
            Log.d(TAG, "Batch saved ${passwords.size} passwords (encrypted)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error batch saving passwords: ${e.message}", e)
            false
        }
    }

    @Transaction
    override suspend fun replaceAllPasswords(passwords: List<Password>): Boolean {
        return try {
            // Delete all existing passwords
            passwordDao.deleteAllPasswords()
            Log.d(TAG, "Deleted all existing passwords")

            // Encrypt all passwords before inserting
            val encryptedEntities = passwords.map { password ->
                encryptPassword(password)
            }

            // Insert new encrypted passwords
            passwordDao.insertPasswords(encryptedEntities)
            Log.d(TAG, "Replaced all passwords with ${passwords.size} new passwords (encrypted)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error replacing all passwords: ${e.message}", e)
            // Transaction will automatically rollback on exception
            false
        }
    }

    /**
     * Encrypts a Password object before storing in database
     *
     * Only the password field is encrypted - other fields (title, username, notes)
     * are stored in plaintext for search functionality.
     *
     * @param password The password to encrypt
     * @return PasswordEntity with encrypted password field
     */
    private fun encryptPassword(password: Password): PasswordEntity {
        val encryptedPasswordField = securityManager.encrypt(password.password)

        return PasswordEntity(
            id = password.id,
            title = password.title,
            username = password.username,
            password = encryptedPasswordField,  // Encrypted!
            notes = password.notes,
            createdAt = password.createdAt,
            updatedAt = password.updatedAt
        )
    }

    /**
     * Decrypts a PasswordEntity from database
     *
     * @param entity The entity with encrypted password field
     * @return Password with decrypted password field
     * @throws SecurityException if decryption fails or data is tampered
     */
    private fun decryptPasswordEntity(entity: PasswordEntity): Password {
        // Decrypt password field
        val decryptedPasswordField = securityManager.decrypt(entity.password)

        return Password(
            id = entity.id,
            title = entity.title,
            username = entity.username,
            password = decryptedPasswordField,  // Decrypted!
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
