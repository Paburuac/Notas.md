package com.tuapp.notasmd.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.tuapp.notasmd.NotasMdApp
import com.tuapp.notasmd.R
import java.util.concurrent.TimeUnit

class RecurringTaskWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId    = inputData.getLong(KEY_TASK_ID, -1L)
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: return Result.failure()
        if (taskId == -1L) return Result.failure()

        val app  = context.applicationContext as NotasMdApp
        val task = app.taskRepository.getTaskById(taskId) ?: return Result.failure()

        showNotification(taskTitle)

        val nextDue = computeNextDue(task.recurUnit, task.recurAmount)
        app.taskRepository.updateTask(
            task.copy(isCompleted = false, nextDueAt = nextDue, updatedAt = System.currentTimeMillis())
        )
        scheduleNext(context, taskId, taskTitle, task.recurUnit, task.recurAmount, nextDue)

        return Result.success()
    }

    private fun showNotification(title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Tareas recurrentes", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Tarea pendiente")
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        manager.notify(title.hashCode(), notification)
    }

    companion object {
        const val KEY_TASK_ID    = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val CHANNEL_ID     = "recurring_tasks"

        fun computeNextDue(unit: String, amount: Int): Long {
            val now = System.currentTimeMillis()
            val millis = when (unit) {
                "HOURS"  -> TimeUnit.HOURS.toMillis(amount.toLong())
                "DAYS"   -> TimeUnit.DAYS.toMillis(amount.toLong())
                "WEEKS"  -> TimeUnit.DAYS.toMillis(amount.toLong() * 7)
                "MONTHS" -> TimeUnit.DAYS.toMillis(amount.toLong() * 30)
                "YEARS"  -> TimeUnit.DAYS.toMillis(amount.toLong() * 365)
                else     -> TimeUnit.DAYS.toMillis(amount.toLong())
            }
            return now + millis
        }

        fun scheduleNext(
            context: Context,
            taskId: Long,
            taskTitle: String,
            unit: String,
            amount: Int,
            nextDueAt: Long
        ) {
            val delay = (nextDueAt - System.currentTimeMillis()).coerceAtLeast(0)
            val data  = workDataOf(KEY_TASK_ID to taskId, KEY_TASK_TITLE to taskTitle)
            val request = OneTimeWorkRequestBuilder<RecurringTaskWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("task_$taskId")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "task_$taskId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context, taskId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("task_$taskId")
        }
    }
}
