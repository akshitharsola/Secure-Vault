// app/src/main/java/com/securevault/utils/UpdateManager.kt
package com.securevault.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Download state for update downloads
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progress: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class Completed(val filePath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
    object Installing : DownloadState()
}

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
        private const val GITHUB_API_URL = "https://api.github.com/repos/akshitharsola/Secure-Vault/releases/latest"
        private const val UPDATE_CHECK_TIMEOUT = 10000 // 10 seconds
        private const val TAG = "UpdateManager"
        private const val UPDATE_DIR_NAME = "SecureVault_Updates"
        private const val PREFS_NAME = "update_manager_prefs"
        private const val KEY_DOWNLOAD_ID = "active_download_id"
        private const val KEY_DOWNLOAD_VERSION = "download_version"
        private const val PROGRESS_UPDATE_INTERVAL = 500L // ms
    }

    private val _updateInfo = mutableStateOf(UpdateInfo(currentVersion = getCurrentVersion()))
    val updateInfo = _updateInfo

    private val _isCheckingForUpdates = mutableStateOf(false)
    val isCheckingForUpdates = _isCheckingForUpdates

    // Download state management
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val downloadManager: DownloadManager? = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val notificationManager = UpdateNotificationManager(context)

    private var progressMonitorJob: Job? = null
    private var downloadCompletionReceiver: BroadcastReceiver? = null

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

    /**
     * Gets the update download directory
     */
    private fun getUpdateDownloadDirectory(): File {
        val updateDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), UPDATE_DIR_NAME)
        if (!updateDir.exists()) {
            updateDir.mkdirs()
        }
        return updateDir
    }

    /**
     * Checks and resumes any active downloads on app launch
     */
    fun checkAndResumeDownload(coroutineScope: CoroutineScope) {
        val downloadId = sharedPrefs.getLong(KEY_DOWNLOAD_ID, -1)
        if (downloadId != -1L && downloadManager != null) {
            coroutineScope.launch {
                val status = queryDownloadStatus(downloadId)
                Log.d(TAG, "Resuming download check - ID: $downloadId, Status: $status")

                when (status) {
                    DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                        startProgressMonitoring(downloadId, coroutineScope)
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val filePath = getDownloadedFilePath(downloadId)
                        if (filePath != null) {
                            _downloadState.value = DownloadState.Completed(filePath)
                        }
                    }
                    DownloadManager.STATUS_FAILED -> {
                        cleanupFailedDownload(downloadId)
                    }
                    else -> {
                        clearDownloadId()
                    }
                }
            }
        }
    }

    /**
     * Starts downloading an update APK
     */
    suspend fun downloadUpdate(downloadUrl: String, version: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting download - URL: $downloadUrl, Version: $version")

                if (downloadUrl.isBlank()) {
                    Log.e(TAG, "Download URL is blank")
                    _downloadState.value = DownloadState.Failed("Download URL not available")
                    return@withContext false
                }

                if (downloadManager == null) {
                    Log.e(TAG, "DownloadManager not available")
                    // Fall back to browser download
                    openDownloadInBrowser(downloadUrl)
                    return@withContext false
                }

                // Check storage space
                val updateDir = getUpdateDownloadDirectory()
                val fileManager = FileManager(context)
                if (!fileManager.hasEnoughSpaceForUpdate(updateDir, 50 * 1024 * 1024)) { // Estimate 50MB
                    _downloadState.value = DownloadState.Failed("Not enough storage space")
                    return@withContext false
                }

                // Clean up old downloads
                cleanupOldUpdateFiles()

                // Create download request
                val fileName = "SecureVault_v$version.apk"
                val destinationFile = File(updateDir, fileName)

                val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                    setTitle("SecureVault Update")
                    setDescription("Downloading SecureVault v$version")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationUri(Uri.fromFile(destinationFile))
                    setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                    setMimeType("application/vnd.android.package-archive")
                }

                // Enqueue download
                val downloadId = downloadManager.enqueue(request)

                // Save download info
                sharedPrefs.edit().apply {
                    putLong(KEY_DOWNLOAD_ID, downloadId)
                    putString(KEY_DOWNLOAD_VERSION, version)
                    apply()
                }

                // Register broadcast receiver
                registerDownloadCompletionReceiver()

                // Start monitoring progress
                val scope = CoroutineScope(Dispatchers.IO + Job())
                startProgressMonitoring(downloadId, scope)

                Log.d(TAG, "Download started successfully - ID: $downloadId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error starting download", e)
                _downloadState.value = DownloadState.Failed("Failed to start download: ${e.message}")
                false
            }
        }
    }

    /**
     * Monitors download progress
     */
    private fun startProgressMonitoring(downloadId: Long, coroutineScope: CoroutineScope) {
        progressMonitorJob?.cancel()
        progressMonitorJob = coroutineScope.launch {
            while (isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor? = downloadManager?.query(query)

                cursor?.use {
                    if (it.moveToFirst()) {
                        val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = it.getInt(statusIndex)

                        when (status) {
                            DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                                val bytesDownloadedIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                val totalBytesIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                                val bytesDownloaded = it.getLong(bytesDownloadedIndex)
                                val totalBytes = it.getLong(totalBytesIndex)

                                val progress = if (totalBytes > 0) {
                                    ((bytesDownloaded * 100) / totalBytes).toInt()
                                } else 0

                                _downloadState.value = DownloadState.Downloading(
                                    progress = progress,
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = totalBytes
                                )

                                // Update notification
                                val version = sharedPrefs.getString(KEY_DOWNLOAD_VERSION, "")
                                if (!version.isNullOrEmpty()) {
                                    notificationManager.showProgressNotification(
                                        version,
                                        progress,
                                        bytesDownloaded,
                                        totalBytes
                                    )
                                }
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                // Completion will be handled by BroadcastReceiver
                                return@launch
                            }
                            DownloadManager.STATUS_FAILED -> {
                                val reasonIndex = it.getColumnIndex(DownloadManager.COLUMN_REASON)
                                val reason = it.getInt(reasonIndex)
                                val errorMessage = getDownloadErrorMessage(reason)
                                _downloadState.value = DownloadState.Failed(errorMessage)
                                notificationManager.showFailureNotification(errorMessage)
                                clearDownloadId()
                                return@launch
                            }
                        }
                    }
                }

                delay(PROGRESS_UPDATE_INTERVAL)
            }
        }
    }

    /**
     * Registers broadcast receiver for download completion
     */
    private fun registerDownloadCompletionReceiver() {
        if (downloadCompletionReceiver != null) return

        downloadCompletionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                val savedDownloadId = sharedPrefs.getLong(KEY_DOWNLOAD_ID, -1)

                if (downloadId == savedDownloadId) {
                    val filePath = getDownloadedFilePath(downloadId)
                    if (filePath != null) {
                        Log.d(TAG, "Download completed - File: $filePath")
                        _downloadState.value = DownloadState.Completed(filePath)
                        progressMonitorJob?.cancel()

                        // Show completion notification
                        val version = sharedPrefs.getString(KEY_DOWNLOAD_VERSION, "")
                        if (!version.isNullOrEmpty()) {
                            notificationManager.showCompletionNotification(version)
                        }
                    } else {
                        Log.e(TAG, "Download completed but file path not found")
                        val errorMessage = "Download file not found"
                        _downloadState.value = DownloadState.Failed(errorMessage)
                        notificationManager.showFailureNotification(errorMessage)
                        cleanupFailedDownload(downloadId)
                    }
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadCompletionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(downloadCompletionReceiver, filter)
        }
    }

    /**
     * Unregisters broadcast receiver
     */
    fun unregisterDownloadReceiver() {
        downloadCompletionReceiver?.let {
            try {
                context.unregisterReceiver(it)
                downloadCompletionReceiver = null
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
    }

    /**
     * Cancels ongoing download
     */
    fun cancelDownload() {
        val downloadId = sharedPrefs.getLong(KEY_DOWNLOAD_ID, -1)
        if (downloadId != -1L) {
            downloadManager?.remove(downloadId)
            progressMonitorJob?.cancel()
            _downloadState.value = DownloadState.Idle
            clearDownloadId()
            cleanupOldUpdateFiles()
            notificationManager.cancelProgressNotification()
            Log.d(TAG, "Download cancelled - ID: $downloadId")
        }
    }

    /**
     * Installs the downloaded APK
     */
    fun installUpdate(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "APK file not found: $filePath")
                _downloadState.value = DownloadState.Failed("Installation file not found")
                return
            }

            _downloadState.value = DownloadState.Installing

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            Log.d(TAG, "Install intent launched for: $filePath")
        } catch (e: Exception) {
            Log.e(TAG, "Error installing update", e)
            _downloadState.value = DownloadState.Failed("Failed to install: ${e.message}")
        }
    }

    /**
     * Gets the file path of a completed download
     */
    private fun getDownloadedFilePath(downloadId: Long): String? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager?.query(query)

        return cursor?.use {
            if (it.moveToFirst()) {
                val uriIndex = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val uri = it.getString(uriIndex)
                uri?.let { Uri.parse(it).path }
            } else null
        }
    }

    /**
     * Queries the status of a download
     */
    private fun queryDownloadStatus(downloadId: Long): Int {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager?.query(query)

        return cursor?.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                it.getInt(statusIndex)
            } else -1
        } ?: -1
    }

    /**
     * Cleans up failed download
     */
    private fun cleanupFailedDownload(downloadId: Long) {
        downloadManager?.remove(downloadId)
        clearDownloadId()
        cleanupOldUpdateFiles()
        _downloadState.value = DownloadState.Failed("Download failed")
    }

    /**
     * Clears saved download ID
     */
    private fun clearDownloadId() {
        sharedPrefs.edit().apply {
            remove(KEY_DOWNLOAD_ID)
            remove(KEY_DOWNLOAD_VERSION)
            apply()
        }
    }

    /**
     * Cleans up old update files
     */
    private fun cleanupOldUpdateFiles() {
        try {
            val updateDir = getUpdateDownloadDirectory()
            updateDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".apk")) {
                    file.delete()
                    Log.d(TAG, "Deleted old update file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old files", e)
        }
    }

    /**
     * Opens download in browser (fallback)
     */
    private fun openDownloadInBrowser(downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Opened download in browser")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening browser", e)
        }
    }

    /**
     * Gets error message for download failure reason
     */
    private fun getDownloadErrorMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Network connection lost"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No external storage found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_FILE_ERROR -> "Storage error occurred"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "Network error occurred"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server error occurred"
            else -> "Download failed"
        }
    }

    data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int
    )
}