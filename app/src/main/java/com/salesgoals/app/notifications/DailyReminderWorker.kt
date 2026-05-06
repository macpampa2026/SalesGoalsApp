package com.salesgoals.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salesgoals.app.SalesGoalsApplication
import com.salesgoals.app.data.models.VariableProgress
import com.salesgoals.app.data.models.VariableType
import com.salesgoals.app.data.repository.toGoals
import com.salesgoals.app.data.repository.toSet
import com.salesgoals.app.utils.Formatters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SalesGoalsApplication ?: return Result.success()
        val repo = app.repository
        val budget = repo.getBudget() ?: run {
            NotificationHelper.showDailyReminder(applicationContext)
            return Result.success()
        }

        val period = budget.period.ifBlank { Formatters.currentPeriod() }
        val entries = repo.getEntries(period)
        val today = Formatters.today()
        val hasEntryToday = entries.any { it.date == today }

        if (!hasEntryToday) {
            NotificationHelper.showDailyReminder(applicationContext)
        }

        val accumulated = entries.fold(com.salesgoals.app.data.models.VariableSet.ZERO) { acc, e -> acc + e.toSet() }
        val daysElapsed = Formatters.elapsedWorkingDays(budget.workingDays)
        val goals = budget.toGoals()

        val behind = VariableType.values().any { type ->
            val progress = VariableProgress(
                type = type,
                monthlyGoal = type.valueOf(goals),
                accumulated = type.valueOf(accumulated),
                workingDays = budget.workingDays,
                daysElapsed = daysElapsed
            )
            progress.status == com.salesgoals.app.data.models.PerformanceStatus.BEHIND
        }
        if (behind) {
            NotificationHelper.showPerformanceAlert(
                applicationContext,
                "Algunas variables están por debajo del ritmo esperado. Revisá tu dashboard."
            )
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_reminder"

        fun schedule(context: Context, hour: Int = 19, minute: Int = 0) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val initialDelay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
