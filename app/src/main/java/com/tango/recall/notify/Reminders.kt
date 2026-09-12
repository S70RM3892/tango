package com.tango.recall.notify

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tango.recall.MainActivity
import com.tango.recall.R
import com.tango.recall.data.Repository
import com.tango.recall.data.TangoDb
import java.util.Calendar

/**
 * The daily nudge.
 *
 * Spaced repetition only works if it happens, and what decides that is not the
 * algorithm but whether the app is opened at all. The reminder says how much is
 * waiting — a number is easier to act on than "don't forget to study" — and stays
 * silent on a day with nothing due rather than training the learner to dismiss it.
 */
object Reminders {

    const val CHANNEL_ID = "daily_review"
    private const val NOTIFICATION_ID = 1
    private const val REQUEST_CODE = 4001

    const val DEFAULT_HOUR = 20
    const val DEFAULT_MINUTE = 0

    /** The next [hour]:[minute] strictly after [now], as epoch millis. */
    fun nextTriggerAt(hour: Int, minute: Int, now: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    /**
     * What to say, or null when the day is clear.
     *
     * A reminder that arrives when there is nothing to do is the fastest way to teach
     * someone to ignore it.
     */
    fun message(due: Int, newCards: Int): String? = when {
        due + newCards <= 0 -> null
        due == 0 -> "新しい ${newCards} 枚が待っています"
        newCards == 0 -> "復習 ${due} 枚が待っています"
        else -> "復習 ${due} 枚と新しい ${newCards} 枚が待っています"
    }

    /** Arm or disarm the daily alarm to match the settings. */
    fun apply(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        now: Long = System.currentTimeMillis(),
    ) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarms.cancel(pending)
        if (!enabled) {
            NotificationManagerCompatCancel(context)
            return
        }
        // Inexact on purpose: an exact alarm needs a permission the learner would have
        // to grant, and a reminder is not worth that. A few minutes either way is fine.
        alarms.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAt(hour, minute, now),
            AlarmManager.INTERVAL_DAY,
            pending,
        )
    }

    /** Re-arm from whatever the learner last chose. Safe to call on every launch. */
    fun applyFromSettings(context: Context, repo: Repository) {
        apply(context, repo.reminderEnabled, repo.reminderHour, repo.reminderMinute)
    }

    private fun NotificationManagerCompatCancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    /** Put today's numbers on screen, if there are any and we are allowed to. */
    fun notifyDue(context: Context, repo: Repository) {
        val counts = repo.deckCounts()
        val due = counts.values.sumOf { it.dueCount + it.learnCount }
        val fresh = counts.values.sumOf { it.newCount }
        val text = message(due, fresh) ?: return
        if (!allowed(context)) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "今日の復習", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "その日の復習が残っているときだけ通知します"
            }
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Tango")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun allowed(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Fires the daily reminder, and puts the alarm back after a restart.
 *
 * Alarms do not survive a reboot or an app update, so the boot broadcast re-arms from
 * the stored setting.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                val repo = Repository(TangoDb(context.applicationContext))
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    -> Reminders.applyFromSettings(context.applicationContext, repo)

                    else -> Reminders.notifyDue(context.applicationContext, repo)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
