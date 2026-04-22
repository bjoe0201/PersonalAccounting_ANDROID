package com.finance.manager.android

import android.app.Application
import com.finance.manager.android.data.local.DatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class FinanceManagerApp : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            databaseInitializer.initializeIfNeeded()
        }
    }
}

