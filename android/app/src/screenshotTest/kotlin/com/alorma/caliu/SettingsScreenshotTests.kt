package com.alorma.caliu

import androidx.compose.runtime.Composable
import com.alorma.caliu.ui.home.HomePreviews
import com.alorma.caliu.ui.settings.SettingsPreview
import com.alorma.caliu.ui.settings.SettingsTestErrorPreview
import com.alorma.caliu.ui.settings.SettingsTestOkPreview
import com.alorma.caliu.ui.settings.SettingsTestingPreview
import com.android.tools.screenshot.PreviewTest

/** Una captura per a cada captura del handoff (`docs/disseny/captures/12`–`14`), en clar i fosc. */
class SettingsScreenshotTests {
  @HomePreviews
  @PreviewTest
  @Composable
  fun c12Settings() = SettingsPreview()

  // No té captura al handoff: la prova en curs, amb el LoadingIndicator dins el botó
  @HomePreviews
  @PreviewTest
  @Composable
  fun c12bSettingsTesting() = SettingsTestingPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c13SettingsTestOk() = SettingsTestOkPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c14SettingsTestError() = SettingsTestErrorPreview()
}
