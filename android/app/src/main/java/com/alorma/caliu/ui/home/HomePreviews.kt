package com.alorma.caliu.ui.home

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alorma.caliu.data.fake.FakeThermostatRepository
import com.alorma.caliu.domain.MinuteOfDay
import com.alorma.caliu.domain.Mode
import com.alorma.caliu.domain.Override
import com.alorma.caliu.domain.ThermostatState
import java.time.Instant
import java.time.LocalTime

/** Mida lògica de les captures del handoff (390 × 920 dp), en clar i fosc i en català. */
@Preview(name = "Clar", locale = "ca", widthDp = 390, heightDp = 920, showBackground = true)
@Preview(
  name = "Fosc",
  locale = "ca",
  widthDp = 390,
  heightDp = 920,
  showBackground = true,
  uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class HomePreviews

/** Estats fixos de les captures `01`–`11`, totes a les 12:47. */
object HomePreviewData {
  private val Now = MinuteOfDay.of(12, 47)
  private val UpdatedAt = LocalTime.of(12, 47)

  private val heatingAuto =
    ThermostatState(
      power = true,
      paused = false,
      mode = Mode.AUTO,
      currentTemp = 19.3,
      setpoint = 20.0,
      heating = true,
      override = null,
      schedule = FakeThermostatRepository.DefaultSchedule,
      battery = 87,
      lastUpdated = Instant.EPOCH,
    )

  private fun content(
    state: ThermostatState,
    refreshing: Boolean = false,
  ) = HomeUiState.Content(state = state, now = Now, updatedAt = UpdatedAt, refreshing = refreshing)

  val HeatingAuto = content(heatingAuto)
  val CalmManual =
    content(heatingAuto.copy(mode = Mode.MANUAL, currentTemp = 21.4, setpoint = 21.0, heating = false))
  val Temporary =
    content(heatingAuto.copy(currentTemp = 20.6, setpoint = 22.0, override = Override(MinuteOfDay.of(14), 22.0)))
  val PausedAuto = content(heatingAuto.copy(paused = true, currentTemp = 20.2, heating = false))
  val PausedManual =
    content(heatingAuto.copy(paused = true, mode = Mode.MANUAL, currentTemp = 20.2, setpoint = 21.0, heating = false))
  val Off = content(heatingAuto.copy(power = false, currentTemp = null, heating = false))
  val Refreshing = content(heatingAuto, refreshing = true)
  val ApiError =
    HomeUiState.ApiError(
      message = "La prova de l'IoT Core ha caducat; renova-la a la consola de Tuya.",
      openConsole = true,
    )
}

@HomePreviews
@Composable
fun HomeHeatingAutoPreview() {
  HomeContent(HomePreviewData.HeatingAuto, HomeActions())
}

@HomePreviews
@Composable
fun HomeStatusMenuPreview() {
  HomeContent(HomePreviewData.HeatingAuto, HomeActions(), initialMenuExpanded = true)
}

@HomePreviews
@Composable
fun HomeCalmManualPreview() {
  HomeContent(HomePreviewData.CalmManual, HomeActions())
}

@HomePreviews
@Composable
fun HomeTemporaryPreview() {
  HomeContent(HomePreviewData.Temporary, HomeActions())
}

@HomePreviews
@Composable
fun HomePausedAutoPreview() {
  HomeContent(HomePreviewData.PausedAuto, HomeActions())
}

@HomePreviews
@Composable
fun HomePausedManualPreview() {
  HomeContent(HomePreviewData.PausedManual, HomeActions())
}

@HomePreviews
@Composable
fun HomeOffPreview() {
  HomeContent(HomePreviewData.Off, HomeActions())
}

@HomePreviews
@Composable
fun HomeRefreshingPreview() {
  HomeContent(HomePreviewData.Refreshing, HomeActions())
}

@HomePreviews
@Composable
fun HomeLoadingPreview() {
  HomeContent(HomeUiState.Loading, HomeActions())
}

@HomePreviews
@Composable
fun HomeApiErrorPreview() {
  HomeContent(HomePreviewData.ApiError, HomeActions())
}

@HomePreviews
@Composable
fun HomeOfflinePreview() {
  HomeContent(HomeUiState.Offline, HomeActions())
}

@HomePreviews
@Composable
fun HomeMissingConfigPreview() {
  HomeContent(HomeUiState.MissingConfig, HomeActions())
}
