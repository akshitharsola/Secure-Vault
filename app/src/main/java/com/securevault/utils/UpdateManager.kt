// app/src/main/java/com/securevault/utils/UpdateManager.kt
package com.securevault.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String = "",
    val currentVersion: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val isForceUpdate: Boolean = false
)

class UpdateManager(private val context: Context) {
    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/akshitharsola/SecureVault-Android/releases/latest"
        private const val UPDATE_CHECK_TIMEOUT = 10000 // 10 seconds
    }

    private val _updateInfo = mutableStateOf(UpdateInfo(currentVersion = getCurrentVersion()))
    val updateInfo = _updateInfo

    private val _isCheckingForUpdates = mutableStateOf(false)
    val isCheckingForUpdates = _isCheckingForUpdates

    fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }

    suspend fun checkForUpdates(): UpdateInfo {
        return withContext(Dispatchers.IO) {
            _isCheckingForUpdates.value = true
            try {
                val currentVersion = getCurrentVersion()
                
                // Don't offer production updates for debug builds
                if (context.packageName.endsWith(".debug")) {
                    val updateInfo = UpdateInfo(
                        currentVersion = currentVersion,
                        isUpdateAvailable = false
                    )
                    _updateInfo.value = updateInfo
                    return@withContext updateInfo
                }
                
                val latestReleaseInfo = fetchLatestReleaseInfo()
                
                if (latestReleaseInfo != null) {
                    val latestVersion = latestReleaseInfo.getString("tag_name").removePrefix("v")
                    val isUpdateAvailable = isVersionNewer(latestVersion, currentVersion)
                    
                    val downloadUrl = getPreferredDownloadUrl(latestReleaseInfo)
                    
                    val releaseNotes = latestReleaseInfo.getString("body")
                    
                    val updateInfo = UpdateInfo(
                        isUpdateAvailable = isUpdateAvailable,
                        latestVersion = latestVersion,
                        currentVersion = currentVersion,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseNotes,
                        isForceUpdate = false // Can be implemented based on specific criteria
                    )
                    
                    _updateInfo.value = updateInfo
                    updateInfo
                } else {
                    val updateInfo = UpdateInfo(
                        currentVersion = currentVersion,
                        isUpdateAvailable = false
                    )
                    _updateInfo.value = updateInfo
                    updateInfo
                }
            } catch (e: Exception) {
                val updateInfo = UpdateInfo(
                    currentVersion = getCurrentVersion(),
                    isUpdateAvailable = false
                )
                _updateInfo.value = updateInfo
                updateInfo
            } finally {
                _isCheckingForUpdates.value = false
            }
        }
    }

    private fun getPreferredDownloadUrl(releaseInfo: JSONObject): String {
        val assets = releaseInfo.getJSONArray("assets")
        
        // Look for release APK first, then fall back to debug APK
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val assetName = asset.getString("name")
            
            // Prefer release APK over debug APK
            if (assetName.contains("app-release.apk", ignoreCase = true)) {
                return asset.getString("browser_download_url")
            }
        }
        
        // Fall back to first APK if no release APK found
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val assetName = asset.getString("name")
            
            if (assetName.endsWith(".apk", ignoreCase = true)) {
                return asset.getString("browser_download_url")
            }
        }
        
        // Final fallback to first asset
        return if (assets.length() > 0) {
            assets.getJSONObject(0).getString("browser_download_url")
        } else {
            ""
        }
    }

    private fun fetchLatestReleaseInfo(): JSONObject? {
        return try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = UPDATE_CHECK_TIMEOUT
            connection.readTimeout = UPDATE_CHECK_TIMEOUT
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                JSONObject(response)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isVersionNewer(latestVersion: String, currentVersion: String): Boolean {
        return try {
            val latest = parseVersion(latestVersion)
            val current = parseVersion(currentVersion)
            
            when {
                latest.major > current.major -> true
                latest.major < current.major -> false
                latest.minor > current.minor -> true
                latest.minor < current.minor -> false
                latest.patch > current.patch -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun parseVersion(version: String): Version {
        val parts = version.split(".")
        return Version(
            major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }

    fun downloadUpdate(downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle error - could show a toast or callback
        }
    }

    data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int
    )
}