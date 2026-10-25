package com.example

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

typealias NetworkConnectivityMonitor = com.example.utils.NetworkConnectivityMonitor
typealias OfflineSyncManager = com.example.utils.OfflineSyncManager
typealias CsvExporter = com.example.utils.CsvExporter
typealias IncrementalSyncWorker = com.example.workers.IncrementalSyncWorker

// Auto-sync Worker (Delegates to IncrementalSyncWorker for fast delta synchronization)
class AutoSyncWorker(context: Context, params: WorkerParameters) : androidx.work.CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val result = com.example.workers.IncrementalSyncWorker.performDirectIncrementalSync(applicationContext, null)
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        fun schedulePeriodicSync(context: Context) {
            com.example.workers.IncrementalSyncWorker.schedulePeriodic(context)
        }
    }
}
