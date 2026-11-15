package io.github.iamoutofmana.doomscroll.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.iamoutofmana.doomscroll.DoomscrollApplication
import io.github.iamoutofmana.doomscroll.widget.refreshFeedWidget
import java.util.concurrent.TimeUnit

/** Background sync: every ~2h when connected (plan §5, Appendix B). */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        try {
            val container = (applicationContext as DoomscrollApplication).container
            container.syncEngine.syncAll()
            container.lastAutoSyncAt = System.currentTimeMillis()
            refreshFeedWidget(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
}

object SyncScheduler {
    private const val PERIODIC_WORK = "doomscroll-periodic-sync"

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(2, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
