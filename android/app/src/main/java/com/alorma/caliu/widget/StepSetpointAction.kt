package com.alorma.caliu.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.ThermostatController
import com.alorma.caliu.domain.ThermostatException
import com.alorma.caliu.domain.steppedTarget
import com.alorma.caliu.domain.targetAt
import com.alorma.caliu.domain.withTarget
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * −/+ del widget mitjà. Com a l'app: el widget canvia a l'instant, s'envia la consigna i, si falla, es torna a l'estat
 * d'abans. Al cap d'una estona es torna a llegir el dispositiu, que en automàtic haurà passat a `temporary`.
 */
class StepSetpointAction :
  ActionCallback,
  KoinComponent {
  private val store: SnapshotStore by inject()
  private val updater: WidgetUpdater by inject()
  private val controller: ThermostatController by inject()

  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    val steps = parameters[StepsKey] ?: return
    val state = store.get() ?: return
    if (!state.power || state.paused) return
    val now = MinuteOfDay.of(LocalTime.now())
    val target = state.steppedTarget(now, steps)
    if (target == state.targetAt(now)) return

    updater.publish(state.withTarget(target, now))
    try {
      controller.setSetpoint(target)
    } catch (e: ThermostatException) {
      updater.publish(state)
      return
    }
    delay(SYNC_DELAY)
    try {
      updater.publish(controller.state())
    } catch (e: ThermostatException) {
      // La consigna ja s'ha enviat; la lectura es repetirà al proper refresc
    }
  }

  companion object {
    val StepsKey = ActionParameters.Key<Int>("steps")
    private val SYNC_DELAY = 2.seconds
  }
}
