package com.salesgoals.app

import android.app.Application
import com.salesgoals.app.data.database.AppDatabase
import com.salesgoals.app.data.repository.SalesRepository
import com.salesgoals.app.notifications.DailyReminderWorker
import com.salesgoals.app.notifications.NotificationHelper

class SalesGoalsApplication : Application() {

    private val database by lazy { AppDatabase.get(this) }
    val repository by lazy {
        SalesRepository(database, database.budgetDao(), database.dailyEntryDao())
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        DailyReminderWorker.schedule(this)
    }
}
