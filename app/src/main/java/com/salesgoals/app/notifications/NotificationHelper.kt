package com.salesgoals.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salesgoals.app.MainActivity
import com.salesgoals.app.R

object NotificationHelper {

    private const val CHANNEL_ID = "sales_goals_channel"
    const val DAILY_REMINDER_ID = 1001
    const val PERFORMANCE_ALERT_ID = 1002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios para cargar resultados y alertas de rendimiento"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showDailyReminder(context: Context) = show(
        context,
        DAILY_REMINDER_ID,
        title = context.getString(R.string.notification_reminder_title),
        text = context.getString(R.string.notification_reminder_text)
    )

    fun showPerformanceAlert(context: Context, message: String) = show(
        context,
        PERFORMANCE_ALERT_ID,
        title = "Atención: rendimiento bajo",
        text = message
    )

    private fun show(context: Context, id: Int, title: String, text: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notif)
        } catch (_: SecurityException) {
            // Permiso POST_NOTIFICATIONS no concedido (Android 13+).
        }
    }
}
