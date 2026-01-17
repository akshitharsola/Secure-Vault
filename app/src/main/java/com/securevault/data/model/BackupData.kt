// app/src/main/java/com/securevault/data/model/BackupData.kt
package com.securevault.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the structure of backup files
 *
 * Version History:
 * - v1.0: PBKDF2-HMAC-SHA256 + AES-256-CBC encryption
 * - v2.0: PBKDF2-HMAC-SHA512 + AES-256-GCM (quantum-resistant, authenticated)
 *
 * Backward Compatibility:
 * - v2.0 backups include optional encryptionType field
 * - v1.0 backups can be read by v2.0+ apps
 * - v2.0 backups cannot be read by v1.0 apps (expected behavior)
 */
data class BackupData(
    @SerializedName("version")
    val version: String = "2.0",  // Updated to v2.0 for new backups

    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("encrypted")
    val encrypted: Boolean = true,

    @SerializedName("passwordCount")
    val passwordCount: Int,

    @SerializedName("appName")
    val appName: String = "SecureVault",

    @SerializedName("platform")
    val platform: String = "Android",

    @SerializedName("data")
    val data: String, // Base64 encoded encrypted JSON string of passwords

    // v2.0 fields (optional for backward compatibility)
    @SerializedName("encryptionType")
    val encryptionType: String? = null,  // e.g., "password-aes256gcm-pqc-ready"

    @SerializedName("kdf")
    val kdf: String? = null,  // e.g., "PBKDF2-HMAC-SHA512"

    @SerializedName("iterations")
    val iterations: Int? = null,  // e.g., 100000

    @SerializedName("quantumResistant")
    val quantumResistant: Boolean? = null  // true for v2.0 backups
)

/**
 * Legacy backup format with obfuscated keys (pre-v1.2.6)
 * Kept for backward compatibility with old backups
 */
data class LegacyBackupData(
    @SerializedName("a")
    val version: String = "1.0",

    @SerializedName("b")
    val timestamp: String,

    @SerializedName("c")
    val passwordCount: Int,

    @SerializedName("d")
    val appName: String = "SecureVault",

    @SerializedName("e")
    val platform: String = "Android",

    @SerializedName("f")
    val data: String // Base64 encoded encrypted JSON string of passwords
)

/**
 * Represents the result of backup operations
 */
sealed class BackupResult {
    data class Success(val filePath: String, val passwordCount: Int) : BackupResult()
    data class Error(val message: String, val exception: Throwable? = null) : BackupResult()
}

/**
 * Represents the result of restore operations
 */
sealed class RestoreResult {
    data class Success(val passwordCount: Int, val restoredCount: Int) : RestoreResult()
    data class Error(val message: String, val exception: Throwable? = null) : RestoreResult()
    data class InvalidPassword(val message: String = "Invalid backup password") : RestoreResult()
    data class InvalidFile(val message: String = "Invalid backup file format") : RestoreResult()
}