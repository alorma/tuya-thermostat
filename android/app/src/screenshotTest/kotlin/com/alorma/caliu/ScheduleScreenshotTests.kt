package com.alorma.caliu

import androidx.compose.runtime.Composable
import com.alorma.caliu.ui.home.HomePreviews
import com.alorma.caliu.ui.schedule.ScheduleCopiedPreview
import com.alorma.caliu.ui.schedule.ScheduleCopyPreview
import com.alorma.caliu.ui.schedule.ScheduleEditPreview
import com.alorma.caliu.ui.schedule.ScheduleErrorPreview
import com.alorma.caliu.ui.schedule.ScheduleTimePreview
import com.android.tools.screenshot.PreviewTest

/**
 * Una captura per a cada captura del programa del handoff (`docs/disseny/captures/19-programa-*`), en clar i fosc. El
 * full inferior i el diàleg d'hora es pinten a mà, perquè layoutlib no pinta les finestres.
 */
class ScheduleScreenshotTests {
  @HomePreviews
  @PreviewTest
  @Composable
  fun c19ScheduleEdit() = ScheduleEditPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c19ScheduleCopy() = ScheduleCopyPreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c19ScheduleTime() = ScheduleTimePreview()

  @HomePreviews
  @PreviewTest
  @Composable
  fun c19ScheduleCopied() = ScheduleCopiedPreview()

  // No té captura al handoff
  @HomePreviews
  @PreviewTest
  @Composable
  fun c19bScheduleOffline() = ScheduleErrorPreview()
}
