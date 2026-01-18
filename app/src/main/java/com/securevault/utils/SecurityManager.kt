// app/src/main/java/com/securevault/utils/SecurityManager.kt
package com.securevault.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure password encryption using Android Keystore
 *
 * Version History:
 * - v1.0-1.5.1: Used SharedPreferences (INSECURE - passwords in plain text DB)
 * - v2.0.0+: Uses Android Keystore (SECURE - hardware-backed encryption)
 *
 * Security Improvements:
 * - Hardware-backed key storage (cannot be extracted even on rooted devices)
 * - AES-256-GCM authenticated encryption (detects tampering)
 * - Automatic migration from old SharedPreferences keys
 * - Keys bound to device (cannot be transferred)
 */
class SecurityManager(private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    // Legacy SharedPreferences for migration detection
    private val legacyPrefs: SharedPreferences =
        context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SecurityManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "securevault_master_key_v2"
        private const val LEGACY_PREFS_NAME = "security_prefs"
        private const val LEGACY_KEY_NAME = "encryption_key"

        // AES-GCM configuration
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128  // 128-bit authentication tag
        private const val IV_LENGTH = 12        // 12 bytes for GCM (recommended)
    }

    /**
     * Encrypts text using hardware-backed AES-256-GCM
     *
     * @param text The plaintext to encrypt
     * @return Base64-encoded encrypted text with IV prepended
     */
    fun encrypt(text: String): String {
        return try {
            val key = getOrCreateKey()

            // Encrypt with AES-GCM
            // Android Keystore automatically generates IV when setRandomizedEncryptionRequired(true)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            // Get the auto-generated IV from the cipher
            val iv = cipher.iv
            val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

            // Combine IV + encrypted data (encrypted data includes GCM auth tag)
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw SecurityException("Failed to encrypt data: ${e.message}", e)
        }
    }

    /**
     * Decrypts text encrypted with hardware-backed AES-256-GCM
     *
     * @param encryptedText Base64-encoded encrypted text with IV prepended
     * @return Decrypted plaintext
     * @throws SecurityException if decryption fails or data has been tampered with
     */
    fun decrypt(encryptedText: String): String {
        return try {
            val key = getOrCreateKey()

            // Decode from Base64
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)

            if (combined.size < IV_LENGTH) {
                throw SecurityException("Invalid encrypted data format - too short")
            }

            // Extract IV and encrypted data
            val iv = ByteArray(IV_LENGTH)
            val encrypted = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.size)

            // Decrypt with AES-GCM
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val decrypted = cipher.doFinal(encrypted)

            String(decrypted, Charsets.UTF_8)
        } catch (e: javax.crypto.AEADBadTagException) {
            // GCM authentication failed - data has been tampered with
            Log.e(TAG, "GCM authentication failed - data tampered or wrong key", e)
            throw SecurityException("Data has been tampered with or corrupted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw SecurityException("Failed to decrypt data: ${e.message}", e)
        }
    }

    /**
     * Gets or creates the master encryption key in Android Keystore
     *
     * If a key doesn't exist, generates a new hardware-backed AES-256 key.
     * The key is stored in the device's Trusted Execution Environment (TEE)
     * or Secure Element if available.
     *
     * @return SecretKey from Android Keystore
     */
    private fun getOrCreateKey(): SecretKey {
        // Check if key already exists in Keystore
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        // Generate new hardware-backed key
        Log.i(TAG, "Generating new hardware-backed encryption key")
        return generateNewKey()
    }

    /**
     * Generates a new hardware-backed AES-256 key in Android Keystore
     *
     * Key Properties:
     * - 256-bit AES key
     * - Hardware-backed (stored in TEE/Secure Element)
     * - Cannot be extracted from device
     * - Can only be used for encryption/decryption
     * - No user authentication required (but can be enabled)
     */
    private fun generateNewKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            ALGORITHM,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)  // Force random IV for each encryption
            // User authentication can be enabled here if needed:
            // .setUserAuthenticationRequired(true)
            // .setUserAuthenticationValidityDurationSeconds(30)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        val key = keyGenerator.generateKey()

        Log.i(TAG, "Successfully generated hardware-backed key: $KEY_ALIAS")
        return key
    }

    /**
     * Checks if there's a legacy encryption key in SharedPreferences
     * Used for migration detection
     *
     * @return true if legacy key exists (needs migration)
     */
    fun hasLegacyKey(): Boolean {
        return legacyPrefs.contains(LEGACY_KEY_NAME)
    }

    /**
     * Deletes the legacy encryption key from SharedPreferences
     * Should be called after successful migration
     */
    fun deleteLegacyKey() {
        if (hasLegacyKey()) {
            legacyPrefs.edit().remove(LEGACY_KEY_NAME).apply()
            Log.i(TAG, "Legacy encryption key deleted from SharedPreferences")
        }
    }

    /**
     * Checks if Android Keystore key exists
     *
     * @return true if hardware-backed key exists
     */
    fun hasKeystoreKey(): Boolean {
        return keyStore.containsAlias(KEY_ALIAS)
    }

    /**
     * Validates encryption by performing a round-trip test
     *
     * @return true if encryption/decryption works correctly
     */
    fun validateEncryption(): Boolean {
        return try {
            val testData = "SecureVault_Test_${System.currentTimeMillis()}"
            val encrypted = encrypt(testData)
            val decrypted = decrypt(encrypted)
            testData == decrypted
        } catch (e: Exception) {
            Log.e(TAG, "Encryption validation failed", e)
            false
        }
    }
}

/**
 * Exception thrown when encryption/decryption fails
 */
class SecurityException(message: String, cause: Throwable? = null) : Exception(message, cause)
