package com.example

import android.app.Application
import com.example.data.local.PreferencesManager
import com.example.data.local.TalikhataDatabase
import com.example.data.repository.TalikhataRepository
import com.example.util.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TalikhataApplication : Application() {

    val database: TalikhataDatabase by lazy { TalikhataDatabase.getInstance(this) }
    val preferencesManager: PreferencesManager by lazy { PreferencesManager(this) }
    val backupManager: BackupManager by lazy { BackupManager(this) }
    val repository: TalikhataRepository by lazy {
        TalikhataRepository(
            context = this,
            dao = database.talikhataDao(),
            prefs = preferencesManager,
            backupManager = backupManager
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.initializeApp()
        }
    }
}
