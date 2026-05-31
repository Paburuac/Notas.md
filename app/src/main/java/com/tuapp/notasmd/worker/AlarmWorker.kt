package com.tuapp.notasmd.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.tuapp.notasmd.NotasMdApp
import com.tuapp.notasmd.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AlarmWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong(KEY_ALARM_ID, -1L)
        if (alarmId == -1L) return Result.failure()

        val app   = context.applicationContext as NotasMdApp
        val alarm = app.alarmRepository.getAlarmById(alarmId) ?: return Result.failure()
        if (!alarm.isEnabled) return Result.success()

        showNotification(alarm.label.ifBlank { "Alarma" })

        if (alarm.repeatDays.isNotEmpty()) {
            scheduleNext(context, alarmId, alarm.hour, alarm.minute, alarm.label, alarm.repeatDays)
        } else {
            app.alarmRepository.updateAlarm(alarm.copy(isEnabled = false))
        }

        return Result.success()
    }

    private fun showNotification(label: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Alarmas", NotificationManager.IMPORTANCE_HIGH)
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label)
            .setContentText("Es hora de tu alarma")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(label.hashCode(), notification)
    }

    companion object {
        const val KEY_ALARM_ID = "alarm_id"
        const val CHANNEL_ID   = "alarms"

        fun millisUntilAlarm(hour: Int, minute: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }

        fun schedule(context: Context, alarmId: Long, hour: Int, minute: Int, label: String, repeatDays: String) {
            val delay = millisUntilAlarm(hour, minute)
            val data  = workDataOf(KEY_ALARM_ID to alarmId)
            val request = OneTimeWorkRequestBuilder<AlarmWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "alarm_$alarmId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun scheduleNext(context: Context, alarmId: Long, hour: Int, minute: Int, label: String, repeatDays: String) {
            schedule(context, alarmId, hour, minute, label, repeatDays)
        }

        fun cancel(context: Context, alarmId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("alarm_$alarmId")
        }
    }
}
