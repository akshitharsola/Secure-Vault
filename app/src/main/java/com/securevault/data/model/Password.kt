// app/src/main/java/com/example/securevault/data/model/Password.kt
package com.securevault.data.model

import java.util.UUID

data class Password(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val password: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Temporary DTO for deserializing legacy backups where fields might be null
 * Used only during backup restoration to handle missing/null fields gracefully
 */
data class PasswordDto(
    val id: String? = null,
    val title: String? = null,
    val username: String? = null,
    val password: String? = null,
    val notes: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) {
    /**
     * Converts DTO to Password, providing defaults for null/missing fields
     * Returns null if required fields (title, username, password) are missing
     */
    fun toPassword(): Password? {
        // Required fields must not be null/blank
        if (title.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            return null
        }

        return Password(
            id = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            title = title,
            username = username,
            password = password,
            notes = notes ?: "",
            createdAt = createdAt ?: System.currentTimeMillis(),
            updatedAt = updatedAt ?: System.currentTimeMillis()
        )
    }
}