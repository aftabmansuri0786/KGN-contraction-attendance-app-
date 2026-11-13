package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceEntity
import com.example.data.model.WorkerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object AttendanceReminderManager {
    private const val TAG = "AttendanceReminder"
    const val CHANNEL_ID = "kgn_daily_attendance_reminder"
    const val NOTIFICATION_ID = 6301
    private const val ALARM_REQUEST_CODE = 6302

    const val EXTRA_OPEN_TAB = "extra_open_tab"
    const val EXTRA_FILTER_PENDING = "extra_filter_pending"
    const val ACTION_DAILY_REMINDER = "com.example.action.DAILY_ATTENDANCE_REMINDER"

    private val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    /**
     * Pure domain check: returns active workers who have NO attendance record saved for [todayRecords].
     * Note: ABSENT / HALF_DAY / PRESENT / PAID_LEAVE / WEEKLY_OFF all count as marked (not pending).
     */
    fun findPendingAttendanceWorkers(
        activeWorkers: List<WorkerEntity>,
        todayRecords: List<AttendanceEntity>
    ): List<WorkerEntity> {
        val markedIds = todayRecords.map { it.workerId }.toSet()
        return activeWorkers.filter { it.isActive && it.id !in markedIds }
    }

    fun buildReminderMessage(pendingWorkers: List<WorkerEntity>): String {
        val namesList = pendingWorkers.map { it.name.trim() }.filter { it.isNotBlank() }
        val namesDisplay = if (namesList.size <= 6) {
            namesList.joinToString(", ")
        } else {
            namesList.take(6).joinToString(", ") + " (+${namesList.size - 6} more)"
        }
        return "Aaj ki attendance complete karein. Pending workers: $namesDisplay. Total pending: ${pendingWorkers.size}."
    }

    fun computeNextTriggerMillisIst(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val cal = Calendar.getInstance(istTimeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun formatTime12Hour(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s IST", hour12, minute, amPm)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Attendance Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminds contractor daily at scheduled IST time to mark pending worker attendance."
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleReminder(context: Context, settings: AttendanceReminderSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(context)
        alarmManager.cancel(pendingIntent)

        if (!settings.enabled) {
            cancelActiveNotification(context)
            return
        }

        val triggerAtMillis = computeNextTriggerMillisIst(settings.hour, settings.minute)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled daily attendance reminder at ${settings.hour}:${settings.minute} IST ($triggerAtMillis)")
        } catch (se: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling attendance reminder", e)
        }
    }

    fun cancelActiveNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Failed cancelling notification: ${e.message}")
        }
    }

    fun cancelNotificationIfAllMarked(
        context: Context,
        activeWorkers: List<WorkerEntity>,
        todayAttendance: List<AttendanceEntity>
    ) {
        val pending = findPendingAttendanceWorkers(activeWorkers, todayAttendance)
        if (pending.isEmpty()) {
            cancelActiveNotification(context)
        }
    }

    suspend fun evaluateAndShowReminderNow(context: Context, forceDemoIfNonePending: Boolean = false): Boolean {
        val db = AppDatabase.getDatabase(context)
        val todayIso = Formatters.getTodayIsoDate()
        val activeWorkers = db.workerDao().getActiveWorkersSync()
        val todayRecords = db.attendanceDao().getAttendanceForDateSync(todayIso)
        val pending = findPendingAttendanceWorkers(activeWorkers, todayRecords)

        if (pending.isEmpty()) {
            cancelActiveNotification(context)
            if (!forceDemoIfNonePending) {
                return false
            }
        }

        if (!hasNotificationPermission(context)) {
            return false
        }

        ensureNotificationChannel(context)

        val message = if (pending.isNotEmpty()) {
            buildReminderMessage(pending)
        } else {
            "Aaj ki sabhi workers ki attendance complete ho chuki hai! Total pending: 0."
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_TAB, 1) // Attendance tab
            putExtra(EXTRA_FILTER_PENDING, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("KGN Construction — Attendance Reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (se: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted", se)
            false
        }
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyAttendanceReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class DailyAttendanceReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = AttendanceReminderPrefs.getSettingsSync(context)
                if (settings.enabled) {
                    AttendanceReminderManager.evaluateAndShowReminderNow(context)
                    // Reschedule for tomorrow's IST trigger
                    AttendanceReminderManager.scheduleReminder(context, settings)
                }
            } catch (e: Exception) {
                Log.e("AttendanceReminder", "Error handling daily reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class AttendanceReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = AttendanceReminderPrefs.getSettingsSync(context)
                AttendanceReminderManager.scheduleReminder(context, settings)
            } catch (e: Exception) {
                Log.e("AttendanceReminder", "Failed rescheduling on $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
