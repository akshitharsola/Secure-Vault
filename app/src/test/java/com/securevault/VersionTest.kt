package com.securevault

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for version verification
 * Ensures version codes and names follow proper sequencing
 */
class VersionTest {

    @Test
    fun `version code should be 8 or higher for v1_2_5`() {
        // This test ensures we're building v1.2.5 or later
        // Version code must be >= 8 for proper update path
        val versionCode = BuildConfig.VERSION_CODE
        assertTrue(
            "Version code must be >= 8 for v1.2.5+, found: $versionCode",
            versionCode >= 8
        )
    }

    @Test
    fun `version name should be 1_2_5 or higher`() {
        val versionName = BuildConfig.VERSION_NAME
        assertNotNull("Version name must not be null", versionName)

        // Extract major.minor.patch
        val parts = versionName.split(".")
        assertTrue("Version should have at least 3 parts", parts.size >= 3)

        val major = parts[0].toInt()
        val minor = parts[1].toInt()
        val patch = parts[2].split("-")[0].toInt()  // Handle "-DEBUG" suffix

        // Should be 1.2.5 or higher
        assertTrue(
            "Version should be 1.2.5+, found: $versionName",
            major >= 1 && (minor > 2 || (minor == 2 && patch >= 5))
        )
    }

    @Test
    fun `debug build should have debug suffix`() {
        if (BuildConfig.BUILD_TYPE == "debug") {
            val versionName = BuildConfig.VERSION_NAME
            assertTrue(
                "Debug builds should have -DEBUG suffix",
                versionName.contains("DEBUG")
            )
        }
    }

    @Test
    fun `application ID should be correct`() {
        val appId = BuildConfig.APPLICATION_ID
        assertTrue(
            "Application ID should start with com.securevault",
            appId.startsWith("com.securevault")
        )
    }

    @Test
    fun `version code should increment from previous version`() {
        // v1.2.4 had versionCode 7
        // v1.2.5 should have versionCode 8
        // This ensures proper upgrade path
        val versionCode = BuildConfig.VERSION_CODE
        val previousVersionCode = 7

        assertTrue(
            "New version code ($versionCode) must be greater than previous ($previousVersionCode)",
            versionCode > previousVersionCode
        )
    }
}
