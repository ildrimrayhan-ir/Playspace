package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlaySpaceApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        AppRepository(
            database.appDao(),
            database.reviewDao(),
            database.notificationDao(),
            database.crashLogDao(),
            database.userDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Run database prepopulation on startup
        applicationScope.launch {
            repository.prepopulateDatabaseIfNeeded()
        }
    }
}
