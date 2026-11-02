package com.alorma.caliu.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import com.alorma.caliu.R
import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.mood
import java.time.LocalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Widget petit 2×1. Es pinta amb l'últim estat desat ([SnapshotStore]), sense esperar la xarxa. */
class SmallThermostatWidget :
  GlanceAppWidget(),
  KoinComponent {
  private val store: SnapshotStore by inject()

  override val sizeMode: SizeMode = SizeMode.Single

  override suspend fun provideGlance(
    context: Context,
    id: GlanceId,
  ) {
    val initial = store.get()
    provideContent {
      val state by store.snapshot.collectAsState(initial)
      WidgetTheme(mood = state?.mood() ?: Mood.OFF) {
        SmallWidgetContent(
          state = state,
          modifier = GlanceModifier.semantics { contentDescription = context.getString(R.string.tile_label) },
        )
      }
    }
  }
}

/** Widget mitjà 4×2, amb −/+ de la consigna i el tram actiu. */
class MediumThermostatWidget :
  GlanceAppWidget(),
  KoinComponent {
  private val store: SnapshotStore by inject()

  override val sizeMode: SizeMode = SizeMode.Single

  override suspend fun provideGlance(
    context: Context,
    id: GlanceId,
  ) {
    val initial = store.get()
    provideContent {
      val state by store.snapshot.collectAsState(initial)
      WidgetTheme(mood = state?.mood() ?: Mood.OFF) {
        MediumWidgetContent(state = state, now = MinuteOfDay.of(LocalTime.now()))
      }
    }
  }
}

/** Receptors dels widgets: el primer widget que s'afegeix engega el refresc periòdic i l'últim que es treu el para. */
abstract class ThermostatWidgetReceiver : GlanceAppWidgetReceiver() {
  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    WidgetRefreshWorker.schedule(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    if (!hasWidgets(context)) WidgetRefreshWorker.cancel(context)
  }

  companion object {
    fun hasWidgets(context: Context): Boolean {
      val manager = AppWidgetManager.getInstance(context)
      return listOf(SmallThermostatWidgetReceiver::class.java, MediumThermostatWidgetReceiver::class.java).any {
        manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty()
      }
    }
  }
}

class SmallThermostatWidgetReceiver : ThermostatWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = SmallThermostatWidget()
}

class MediumThermostatWidgetReceiver : ThermostatWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = MediumThermostatWidget()
}
