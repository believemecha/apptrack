package com.example.apptrack.call.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.apptrack.MainActivity

/**
 * Posts notification when assistant captures a message. Tap opens call details.
 */
object AssistantNotificationHelper {

    private const val CHANNEL_ID = "assistant_messages"
    private const val NOTIFICATION_ID_BASE = 8000

    fun showTranscriptNotification(
        context: Context,
        phoneNumber: String,
        contactName: String?,
        transcriptPreview: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Assistant Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val displayName = contactName?.takeIf { it.isNotBlank() } ?: phoneNumber
        val preview = transcriptPreview.take(80).let { if (it.length < transcriptPreview.length) "$it…" else it }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_call_details", phoneNumber)
        }
        val pending = PendingIntent.getActivity(
            context,
            phoneNumber.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Caller left a message")
            .setContentText("$displayName: $preview")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$displayName\n$transcriptPreview"))
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID_BASE + phoneNumber.hashCode().and(0x7FFF), notification)
    }
}
