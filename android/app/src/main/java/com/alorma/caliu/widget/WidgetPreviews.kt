package com.alorma.caliu.widget

import androidx.compose.runtime.Composable
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.ThermostatState
import com.alorma.caliu.ui.home.HomePreviewData

// Estats de la captura `15`: escalfant (20,4 °C) i en repòs (21,4 °C), a les 12:47
private val heating = HomePreviewData.HeatingAuto.state.copy(currentTemp = 20.4)
private val calm: ThermostatState =
  heating.copy(
    mode = Mode.MANUAL,
    currentTemp = 21.4,
    setpoint = 21.0,
    heating = false,
  )
private val now = MinuteOfDay.of(12, 47)

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 80)
@Composable
fun SmallWidgetHeatingPreview() {
  WidgetTheme(Mood.HEAT) { SmallWidgetContent(heating) }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 80)
@Composable
fun SmallWidgetCalmPreview() {
  WidgetTheme(Mood.CALM) { SmallWidgetContent(calm) }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 372, heightDp = 172)
@Composable
fun MediumWidgetHeatingPreview() {
  WidgetTheme(Mood.HEAT) { MediumWidgetContent(heating, now) }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 372, heightDp = 172)
@Composable
fun MediumWidgetOffPreview() {
  WidgetTheme(Mood.OFF) { MediumWidgetContent(heating.copy(power = false, heating = false), now) }
}
