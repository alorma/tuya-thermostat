package com.alorma.caliu.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.alorma.caliu.domain.ThermostatController
import com.alorma.caliu.domain.ThermostatException
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Llegeix el termòstat cada 15 min (el mínim de WorkManager) mentre hi hagi algun widget, i els actualitza. */
class WidgetRefreshWorker(
  context: Context,
  params: WorkerParameters,
) : CoroutineWorker(context, params),
  KoinComponent {
  private val controller: ThermostatController by inject()
  private val updater: WidgetUpdater by inject()

  override suspend fun doWork(): Result =
    try {
      updater.publish(controller.state())
      Result.success()
    } catch (e: ThermostatException.Offline) {
      Result.retry()
    } catch (e: ThermostatException) {
      // Error de Tuya o falta la configuració: no serveix de res repetir-ho ara
      Result.success()
    }

  companion object {
    private const val PERIODIC = "widget-refresh"
    private const val NOW = "widget-refresh-now"
    private const val INTERVAL_MINUTES = 15L

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Engega el refresc periòdic (si no hi és) i en fa un ara, perquè el widget nou no surti buit. */
    fun schedule(context: Context) {
      val workManager = WorkManager.getInstance(context)
      workManager.enqueueUniquePeriodicWork(
        PERIODIC,
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<WidgetRefreshWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
          .setConstraints(network)
          .build(),
      )
      workManager.enqueueUniqueWork(
        NOW,
        ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<WidgetRefreshWorker>().setConstraints(network).build(),
      )
    }

    fun cancel(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    }
  }
}
