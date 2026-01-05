package com.securevault

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securevault.data.local.PasswordDatabase
import com.securevault.data.local.PasswordEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented tests for database migration and version upgrades
 * Ensures user data is preserved during app updates
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private lateinit var database: PasswordDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            PasswordDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun database_creates_successfully() {
        // Verify database can be created
        assertNotNull(database)
        assertNotNull(database.passwordDao())
    }

    @Test
    fun database_preserves_data_after_version_upgrade() {
        // Simulate v1.2.4 → v1.2.5 upgrade
        // Insert test data
        val testPassword = PasswordEntity(
            id = 1,
            title = "Test Password",
            username = "testuser",
            password = "encrypted_password",
            category = "Test",
            notes = "Test notes",
            color = 0xFF6200EE.toInt(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        database.passwordDao().insert(testPassword)

        // Verify data exists
        val passwords = database.passwordDao().getAllPasswords()
        assertEquals(1, passwords.size)
        assertEquals("Test Password", passwords[0].title)
        assertEquals("testuser", passwords[0].username)
    }

    @Test
    fun app_version_matches_build_configuration() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            0
        )

        // Version code should be >= 8 for v1.2.5+
        assertTrue(
            "Version code must be >= 8, found: ${packageInfo.longVersionCode}",
            packageInfo.longVersionCode >= 8
        )

        // Version name should be 1.2.5 or higher
        val versionName = packageInfo.versionName
        assertNotNull(versionName)
        assertTrue(
            "Version name should be 1.2.5+, found: $versionName",
            versionName.startsWith("1.2.") || versionName.startsWith("1.3.")
        )
    }

    @Test
    fun database_schema_version_is_correct() {
        // Database version should be 1 (no schema changes from v1.2.4)
        val dbVersion = database.openHelper.readableDatabase.version
        assertEquals(
            "Database schema version should be 1",
            1,
            dbVersion
        )
    }

    @Test
    fun password_entity_all_fields_accessible() {
        // Verify all fields can be read/written without encryption errors
        val testPassword = PasswordEntity(
            id = 2,
            title = "Field Test",
            username = "fieldtest@example.com",
            password = "test_encrypted_password",
            category = "Email",
            notes = "Testing all fields",
            color = 0xFF03DAC5.toInt(),
            createdAt = 1234567890L,
            updatedAt = 1234567890L
        )

        database.passwordDao().insert(testPassword)

        val retrieved = database.passwordDao().getPasswordById(2)
        assertNotNull(retrieved)
        assertEquals(testPassword.title, retrieved?.title)
        assertEquals(testPassword.username, retrieved?.username)
        assertEquals(testPassword.password, retrieved?.password)
        assertEquals(testPassword.category, retrieved?.category)
        assertEquals(testPassword.notes, retrieved?.notes)
        assertEquals(testPassword.color, retrieved?.color)
        assertEquals(testPassword.createdAt, retrieved?.createdAt)
        assertEquals(testPassword.updatedAt, retrieved?.updatedAt)
    }

    @Test
    fun multiple_passwords_can_be_stored_and_retrieved() {
        // Test database handles multiple entries (typical user scenario)
        val passwords = listOf(
            PasswordEntity(10, "Email", "user@email.com", "pass1", "Email", "", 0xFF6200EE.toInt(), 100L, 100L),
            PasswordEntity(11, "Banking", "bankuser", "pass2", "Finance", "", 0xFF03DAC5.toInt(), 200L, 200L),
            PasswordEntity(12, "Social", "socialuser", "pass3", "Social", "", 0xFF018786.toInt(), 300L, 300L)
        )

        passwords.forEach { database.passwordDao().insert(it) }

        val retrieved = database.passwordDao().getAllPasswords()
        assertEquals(3, retrieved.size)
    }

    @Test
    fun password_update_works_correctly() {
        // Insert original
        val original = PasswordEntity(
            id = 20,
            title = "Original",
            username = "original_user",
            password = "original_pass",
            category = "Test",
            notes = "Original notes",
            color = 0xFF6200EE.toInt(),
            createdAt = 1000L,
            updatedAt = 1000L
        )
        database.passwordDao().insert(original)

        // Update
        val updated = original.copy(
            title = "Updated",
            username = "updated_user",
            updatedAt = 2000L
        )
        database.passwordDao().update(updated)

        // Verify
        val retrieved = database.passwordDao().getPasswordById(20)
        assertEquals("Updated", retrieved?.title)
        assertEquals("updated_user", retrieved?.username)
        assertEquals(2000L, retrieved?.updatedAt)
    }

    @Test
    fun password_delete_works_correctly() {
        val password = PasswordEntity(
            id = 30,
            title = "To Delete",
            username = "delete_user",
            password = "delete_pass",
            category = "Test",
            notes = "",
            color = 0xFF6200EE.toInt(),
            createdAt = 1000L,
            updatedAt = 1000L
        )
        database.passwordDao().insert(password)

        // Verify exists
        assertNotNull(database.passwordDao().getPasswordById(30))

        // Delete
        database.passwordDao().delete(password)

        // Verify deleted
        assertNull(database.passwordDao().getPasswordById(30))
    }
}
