#!/usr/bin/env kotlin

import java.io.File
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Standalone backup decryption script
 * Decrypts SecureVault backup files and extracts password data
 */

// Encryption constants (must match BackupEncryption.kt)
const val ALGORITHM = "AES"
const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
const val KEY_LENGTH = 256
const val IV_LENGTH = 16
const val SALT_LENGTH = 32
const val ITERATION_COUNT = 100000

fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
    val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
    val key = factory.generateSecret(spec)
    return SecretKeySpec(key.encoded, ALGORITHM)
}

fun decrypt(encryptedData: String, password: String): String {
    // Decode from Base64
    val combined = Base64.getDecoder().decode(encryptedData)

    if (combined.size < SALT_LENGTH + IV_LENGTH) {
        throw Exception("Invalid encrypted data format")
    }

    // Extract salt, IV, and encrypted data
    val salt = ByteArray(SALT_LENGTH)
    val iv = ByteArray(IV_LENGTH)
    val encrypted = ByteArray(combined.size - SALT_LENGTH - IV_LENGTH)

    System.arraycopy(combined, 0, salt, 0, SALT_LENGTH)
    System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH)
    System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, encrypted, 0, encrypted.size)

    // Derive key from password using same salt
    val key = deriveKey(password, salt)

    // Decrypt the data
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
    val decryptedData = cipher.doFinal(encrypted)

    return String(decryptedData, Charsets.UTF_8)
}

// Main execution
val backupFilePath = args.getOrNull(0) ?: "/Users/akshitharsola/Documents/SecureAttend/SecureVault-Android-master/app/SecureVault_Backup_2026-01-05_18-41-22.backup"
val password = args.getOrNull(1) ?: "147zaq"
val outputFile = args.getOrNull(2) ?: "decrypted_passwords.txt"

println("=".repeat(70))
println("SecureVault Backup Decryption Script")
println("=".repeat(70))
println("Backup file: $backupFilePath")
println("Output file: $outputFile")
println()

try {
    // Read backup file
    println("[1/4] Reading backup file...")
    val backupContent = File(backupFilePath).readText()
    println("✓ Backup file read successfully (${backupContent.length} bytes)")

    // Parse JSON to extract encrypted data
    println("\n[2/4] Parsing backup JSON structure...")

    // Simple JSON parsing to extract the "data" field
    // Looking for patterns like: "data":"..." or "f":"..." (legacy format)
    val dataPattern = """"(?:data|f)"\s*:\s*"([^"]+)"""".toRegex()
    val match = dataPattern.find(backupContent)

    if (match == null) {
        // Very old format - entire file is encrypted data
        println("✓ Detected very old backup format (raw encrypted data)")
        val encryptedData = backupContent.trim()

        println("\n[3/4] Decrypting password data...")
        val decryptedJson = decrypt(encryptedData, password)
        println("✓ Decryption successful!")

        println("\n[4/4] Writing decrypted data to $outputFile...")
        File(outputFile).writeText(decryptedJson)
        println("✓ Data written successfully!")

        println("\n" + "=".repeat(70))
        println("SUCCESS! Decrypted data saved to: $outputFile")
        println("=".repeat(70))

        // Print preview
        println("\nPreview of decrypted data:")
        println("-".repeat(70))
        println(decryptedJson.take(500) + if (decryptedJson.length > 500) "\n..." else "")
        println("-".repeat(70))
    } else {
        val encryptedData = match.groupValues[1]
        println("✓ Found encrypted data field (${encryptedData.length} bytes)")

        println("\n[3/4] Decrypting password data...")
        val decryptedJson = decrypt(encryptedData, password)
        println("✓ Decryption successful!")

        println("\n[4/4] Writing decrypted data to $outputFile...")
        val output = buildString {
            appendLine("=".repeat(70))
            appendLine("SecureVault Decrypted Backup Data")
            appendLine("=".repeat(70))
            appendLine("Backup file: $backupFilePath")
            appendLine("Decrypted at: ${java.time.LocalDateTime.now()}")
            appendLine("=".repeat(70))
            appendLine()
            appendLine("RAW DECRYPTED JSON:")
            appendLine("-".repeat(70))
            appendLine(decryptedJson)
            appendLine("-".repeat(70))
        }

        File(outputFile).writeText(output)
        println("✓ Data written successfully!")

        println("\n" + "=".repeat(70))
        println("SUCCESS! Decrypted data saved to: $outputFile")
        println("=".repeat(70))

        // Print preview
        println("\nPreview of decrypted JSON:")
        println("-".repeat(70))
        println(decryptedJson.take(500) + if (decryptedJson.length > 500) "\n..." else "")
        println("-".repeat(70))
    }

} catch (e: Exception) {
    println("\n" + "=".repeat(70))
    println("ERROR: ${e.message}")
    println("=".repeat(70))
    e.printStackTrace()
}
