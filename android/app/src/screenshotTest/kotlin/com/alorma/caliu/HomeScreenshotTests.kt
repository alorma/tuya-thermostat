package com.alorma.caliu

import androidx.compose.runtime.Composable
import com.alorma.caliu.ui.home.HomeApiErrorPreview
import com.alorma.caliu.ui.home.HomeCalmManualPreview
import com.alorma.caliu.ui.home.HomeHeatingAutoPreview
import com.alorma.caliu.ui.home.HomeLoadingPreview
import com.alorma.caliu.ui.home.HomeMissingConfigPreview
import com.alorma.caliu.ui.home.HomeOffPreview
import com.alorma.caliu.ui.home.HomeOfflinePreview
import com.alorma.caliu.ui.home.HomePausedAutoPreview
import com.alorma.caliu.ui.home.HomePausedManualPreview
import com.alorma.caliu.ui.home.HomePreviews
import com.alorma.caliu.ui.home.HomeRefreshingPreview
import com.alorma.caliu.ui.home.HomeTemporaryPreview
import com.android.tools.screenshot.PreviewTest

/** Una captura per a cada captura del handoff (`docs/disseny/captures/01`–`11`), en clar i fosc. */
class HomeScreenshotTests {
  @HomePreviews
  @PreviewTest
  @Composable
  fun c01HeatingAuto() = HomeHeatingAutoPreview()

  // La 02 (menú d'estat obert) no hi és: layoutlib no pinta els popups i surt un rectangle blanc. Es mira amb la
  // preview HomeStatusMenuPreview a Android Studio

  @HomePreviews
  @PreviewTest
  @Composable
  fun c03CalmManual() = HomeCalmManualPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c04Temporary() = HomeTemporaryPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c05PausedAuto() = HomePausedAutoPreview()

  // No té captura al handoff: la consigna en pausa, atenuada i amb vora discontínua
  @HomePreviews
  @PreviewTest
  @Composable
  fun c05bPausedManual() = HomePausedManualPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c06Off() = HomeOffPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c07Refreshing() = HomeRefreshingPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c08Loading() = HomeLoadingPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c09ApiError() = HomeApiErrorPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c10Offline() = HomeOfflinePreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c11MissingConfig() = HomeMissingConfigPreview()
}
