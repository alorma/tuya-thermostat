package com.alorma.caliu.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.alorma.caliu.ui.home.HomePreviews

/** Valors de les captures `12`–`14`. */
object SettingsPreviewData {
  fun form() =
    SettingsForm(
      accessId = "a1b2c3d4e5f6g7h8i9j0",
      accessSecret = "0123456789abcdef0123456789abcdef",
      deviceId = "bf0123456789abcdefgh",
    )

  val SignatureError =
    ConnectionTestState.Failure(
      "Les credencials de Tuya no són vàlides. Torna a copiar l'Access ID i l'Access Secret des de iot.tuya.com.",
    )
}

@HomePreviews
@Composable
fun SettingsPreview() {
  SettingsContent(remember { SettingsPreviewData.form() }, SettingsUiState(), SettingsActions())
}

@HomePreviews
@Composable
fun SettingsTestingPreview() {
  SettingsContent(
    remember { SettingsPreviewData.form() },
    SettingsUiState(test = ConnectionTestState.Testing),
    SettingsActions(),
  )
}

@HomePreviews
@Composable
fun SettingsTestOkPreview() {
  SettingsContent(
    remember { SettingsPreviewData.form() },
    SettingsUiState(test = ConnectionTestState.Success),
    SettingsActions(),
  )
}

@HomePreviews
@Composable
fun SettingsTestErrorPreview() {
  SettingsContent(
    remember { SettingsPreviewData.form() },
    SettingsUiState(test = SettingsPreviewData.SignatureError),
    SettingsActions(),
  )
}
