// app/src/main/java/com/securevault/utils/UpdateNotificationManager.kt
package com.securevault.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.securevault.MainActivity
import com.securevault.R

/**
 * Manages notifications for app update downloads
 */
class UpdateNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "update_downloads"
        private const val CHANNEL_NAME = "App Updates"
        private const val CHANNEL_DESCRIPTION = "Notifications for app update downloads"
        const val NOTIFICATION_ID_PROGRESS = 1001
        const val NOTIFICATION_ID_COMPLETE = 1002
        const val NOTIFICATION_ID_FAILED = 1003
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val fileManager = FileManager(context)

    init {
        createNotificationChannel()
    }

    /**
     * Creates notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows or updates download progress notification
     */
    fun showProgressNotification(
        version: String,
        progress: Int,
        bytesDownloaded: Long,
        totalBytes: Long
    ) {
        val formattedDownloaded = fileManager.formatBytes(bytesDownloaded)
        val formattedTotal = fileManager.formatBytes(totalBytes)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading SecureVault v$version")
            .setContentText("$progress% - $formattedDownloaded/$formattedTotal")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setContentIntent(createAppIntent())
            .build()

        notificationManager.notify(NOTIFICATION_ID_PROGRESS, notification)
    }

    /**
     * Shows download complete notification
     */
    fun showCompletionNotification(version: String) {
        // Cancel progress notification
        notificationManager.cancel(NOTIFICATION_ID_PROGRESS)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update Ready to Install")
            .setContentText("SecureVault v$version downloaded successfully")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createAppIntent())
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    /**
     * Shows download failed notification
     */
    fun showFailureNotification(errorMessage: String) {
        // Cancel progress notification
        notificationManager.cancel(NOTIFICATION_ID_PROGRESS)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Update Download Failed")
            .setContentText(errorMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createAppIntent())
            .build()

        notificationManager.notify(NOTIFICATION_ID_FAILED, notification)
    }

    /**
     * Cancels all update notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_PROGRESS)
        notificationManager.cancel(NOTIFICATION_ID_COMPLETE)
        notificationManager.cancel(NOTIFICATION_ID_FAILED)
    }

    /**
     * Cancels progress notification only
     */
    fun cancelProgressNotification() {
        notificationManager.cancel(NOTIFICATION_ID_PROGRESS)
    }

    /**
     * Creates a pending intent to open the app
     */
    private fun createAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
