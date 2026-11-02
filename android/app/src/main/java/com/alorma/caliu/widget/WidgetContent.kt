package com.alorma.caliu.widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.ColorProvider as FixedColorProvider
import com.alorma.caliu.MainActivity
import com.alorma.caliu.R
import com.alorma.caliu.data.settings.ThemeMode
import com.alorma.caliu.designsystem.R as DsR
import com.alorma.caliu.designsystem.theme.FixedColors
import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.designsystem.theme.moodColorScheme
import com.alorma.caliu.designsystem.theme.moodPalette
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.domain.activeSlotIndex
import com.alorma.caliu.domain.mood
import com.alorma.caliu.domain.targetAt
import com.alorma.caliu.ui.format.formatTemperature
import java.util.Locale

/*
 * Widgets de Glance (captura `15`). Prenen el color hero del mood: `tertiaryContainer` de fons, `tertiary` per a la
 * icona i el xip. Glance no pot fer servir les fonts de l'app (només les del sistema), així que les xifres van en
 * sans-serif negreta en lloc de Nunito.
 */

private val BigTemp = 34.sp
private val MediumTemp = 46.sp
private val SetpointSize = 26.sp

private val LocalWidgetThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }

/**
 * Tema dels widgets: la paleta del [mood] en clar i fosc. El launcher és qui resol clar/fosc i no veu el tema de l'app,
 * així que amb [ThemeMode.LIGHT] o [ThemeMode.DARK] es fan servir els mateixos colors en tots dos casos.
 */
@Composable
fun WidgetTheme(
  mood: Mood,
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val light = moodColorScheme(mood, dark = false)
  val dark = moodColorScheme(mood, dark = true)
  CompositionLocalProvider(LocalWidgetThemeMode provides themeMode) {
    GlanceTheme(
      colors =
        when (themeMode) {
          ThemeMode.LIGHT -> ColorProviders(light = light, dark = light)
          ThemeMode.DARK -> ColorProviders(light = dark, dark = dark)
          ThemeMode.SYSTEM -> ColorProviders(light = light, dark = dark)
        },
      content = content,
    )
  }
}

/** Un color de dia i un de nit, respectant el tema triat a Configuració. */
@Composable
private fun dayNight(
  day: Color,
  night: Color,
): ColorProvider =
  when (LocalWidgetThemeMode.current) {
    ThemeMode.LIGHT -> FixedColorProvider(day)
    ThemeMode.DARK -> FixedColorProvider(night)
    ThemeMode.SYSTEM -> ColorProvider(day = day, night = night)
  }

/** Widget petit (2×1): la icona d'estat en un quadrat arrodonit i la temperatura actual. */
@Composable
fun SmallWidgetContent(
  state: ThermostatState?,
  modifier: GlanceModifier = GlanceModifier,
) {
  val context = LocalContext.current
  val colors = GlanceTheme.colors
  Row(
    modifier = modifier.widgetBackground().padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    StatusIcon(state)
    Spacer(GlanceModifier.width(12.dp))
    Text(
      text = state.currentText(context, unit = "°"),
      style = numerals(BigTemp, colors.onTertiaryContainer),
      maxLines = 1,
    )
  }
}

/**
 * Widget mitjà (4×2): la temperatura i el xip d'estat; −/+ amb la consigna; i, en automàtic, el tram actiu del
 * programa. Apagat o en pausa, els −/+ no fan res.
 */
@Composable
fun MediumWidgetContent(
  state: ThermostatState?,
  now: MinuteOfDay,
  modifier: GlanceModifier = GlanceModifier,
) {
  val context = LocalContext.current
  val colors = GlanceTheme.colors
  val variant = heroVariant(state)
  Column(modifier = modifier.widgetBackground().padding(horizontal = 20.dp, vertical = 16.dp)) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Row(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Bottom) {
        Text(text = state.currentText(context, unit = ""), style = numerals(MediumTemp, colors.onTertiaryContainer))
        Text(
          text = "°C",
          style = numerals(20.sp, variant),
          modifier = GlanceModifier.padding(start = 4.dp, bottom = 8.dp),
        )
      }
      if (state != null) StatusChip(state)
    }
    Spacer(GlanceModifier.height(12.dp))
    SetpointRow(state, now)
    val slot =
      state?.takeIf { it.mode == Mode.AUTO && it.power }?.let { s ->
        s.schedule.activeSlotIndex(now)?.let { s.schedule[it] }
      }
    if (slot != null) {
      Spacer(GlanceModifier.height(10.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          provider = ImageProvider(DsR.drawable.ic_schedule),
          contentDescription = null,
          modifier = GlanceModifier.size(20.dp),
          colorFilter = ColorFilter.tint(colors.onTertiaryContainer),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
          text = context.getString(R.string.schedule_range, slot.start.toString(), slot.end.toString()),
          style = TextStyle(color = colors.onTertiaryContainer, fontSize = 16.sp, fontWeight = FontWeight.Medium),
        )
      }
    }
  }
}

@Composable
private fun SetpointRow(
  state: ThermostatState?,
  now: MinuteOfDay,
) {
  val context = LocalContext.current
  val colors = GlanceTheme.colors
  val enabled = state != null && state.power && !state.paused
  val target = state?.targetAt(now)
  Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    StepButton(
      icon = DsR.drawable.ic_remove,
      label = context.getString(R.string.setpoint_decrease),
      steps = -1,
      enabled = enabled && target != null && target > state.minSetpoint,
      state = state,
    )
    Text(
      text =
        if (target != null && state.power) {
          context.getString(R.string.temp_value, formatTemperature(target, context.locale()))
        } else {
          context.getString(R.string.temp_unknown)
        },
      style = numerals(SetpointSize, if (enabled) colors.onTertiaryContainer else heroVariant(state), TextAlign.Center),
      modifier = GlanceModifier.defaultWeight(),
    )
    StepButton(
      icon = DsR.drawable.ic_add,
      label = context.getString(R.string.setpoint_increase),
      steps = +1,
      enabled = enabled && target != null && target < state.maxSetpoint,
      state = state,
    )
  }
}

@Composable
private fun StepButton(
  @DrawableRes icon: Int,
  label: String,
  steps: Int,
  enabled: Boolean,
  state: ThermostatState?,
) {
  val base =
    GlanceModifier
      .size(48.dp)
      .background(dayNight(day = FixedColors.trackLight, night = FixedColors.trackDark))
      .cornerRadius(16.dp)
  Box(
    modifier =
      if (enabled) {
        base.clickable(actionRunCallback<StepSetpointAction>(actionParametersOf(StepSetpointAction.StepsKey to steps)))
      } else {
        base
      },
    contentAlignment = Alignment.Center,
  ) {
    Image(
      provider = ImageProvider(icon),
      contentDescription = label,
      modifier = GlanceModifier.size(24.dp),
      colorFilter = ColorFilter.tint(if (enabled) GlanceTheme.colors.onTertiaryContainer else heroVariant(state)),
    )
  }
}

@Composable
private fun StatusIcon(state: ThermostatState?) {
  val colors = GlanceTheme.colors
  Box(
    modifier = GlanceModifier.size(48.dp).background(colors.tertiary).cornerRadius(14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      provider = ImageProvider(statusIcon(state)),
      contentDescription = state?.let { LocalContext.current.getString(statusLabel(it)) },
      modifier = GlanceModifier.size(26.dp),
      colorFilter = ColorFilter.tint(colors.onTertiary),
    )
  }
}

@Composable
private fun StatusChip(state: ThermostatState) {
  val colors = GlanceTheme.colors
  Row(
    modifier =
      GlanceModifier
        .background(colors.tertiary)
        .cornerRadius(20.dp)
        .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      provider = ImageProvider(statusIcon(state)),
      contentDescription = null,
      modifier = GlanceModifier.size(18.dp),
      colorFilter = ColorFilter.tint(colors.onTertiary),
    )
    Spacer(GlanceModifier.width(6.dp))
    Text(
      text = LocalContext.current.getString(statusLabel(state)),
      style = TextStyle(color = colors.onTertiary, fontSize = 16.sp, fontWeight = FontWeight.Bold),
      maxLines = 1,
    )
  }
}

/** Fons hero amb el radi dels widgets del sistema. Tocar-lo obre l'app. */
@Composable
private fun GlanceModifier.widgetBackground(): GlanceModifier =
  fillMaxSize()
    .appWidgetBackground()
    .background(GlanceTheme.colors.tertiaryContainer)
    .cornerRadius(android.R.dimen.system_app_widget_background_radius)
    .clickable(openApp())

private fun openApp(): Action = actionStartActivity<MainActivity>()

private fun numerals(
  size: androidx.compose.ui.unit.TextUnit,
  color: ColorProvider,
  align: TextAlign = TextAlign.Start,
) = TextStyle(
  color = color,
  fontSize = size,
  fontWeight = FontWeight.Bold,
  fontFamily = FontFamily.SansSerif,
  textAlign = align,
)

/** El color de «°C» i dels controls desactivats: `heroVariant` del mood, que no és un rol de M3. */
@Composable
private fun heroVariant(state: ThermostatState?): ColorProvider {
  val mood = state?.mood() ?: Mood.OFF
  return dayNight(
    day = moodPalette(mood, dark = false).heroVariant,
    night = moodPalette(mood, dark = true).heroVariant,
  )
}

@DrawableRes
fun statusIcon(state: ThermostatState?): Int =
  when (state?.mood() ?: Mood.OFF) {
    Mood.HEAT -> DsR.drawable.ic_local_fire_department_filled
    Mood.CALM -> DsR.drawable.ic_pause_filled
    Mood.PAUSE -> DsR.drawable.ic_pause_circle_filled
    Mood.OFF -> DsR.drawable.ic_power_settings_new
  }

fun statusLabel(state: ThermostatState): Int =
  when (state.mood()) {
    Mood.HEAT -> R.string.status_heating
    Mood.CALM -> R.string.status_idle
    Mood.PAUSE -> R.string.status_paused
    Mood.OFF -> R.string.status_off
  }

fun Context.locale(): Locale = resources.configuration.locales[0]

/** «20,4» + [unit], o «—» si està apagat o encara no hi ha dades. */
private fun ThermostatState?.currentText(
  context: Context,
  unit: String,
): String {
  val current = this?.currentTemp?.takeIf { power } ?: return context.getString(R.string.temp_unknown)
  return formatTemperature(current, context.locale()) + unit
}
