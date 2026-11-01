package com.alorma.caliu.designsystem

import androidx.compose.runtime.Composable
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.preview.ThemeGalleryCalmPreview
import com.alorma.caliu.designsystem.preview.ThemeGalleryHeatPreview
import com.alorma.caliu.designsystem.preview.ThemeGalleryOffPreview
import com.alorma.caliu.designsystem.preview.ThemeGalleryPausePreview
import com.android.tools.screenshot.PreviewTest

/** Galeria del tema: els 4 moods, en clar i fosc. */
class ThemeScreenshotTests {
  @CaliuPreviews
  @PreviewTest
  @Composable
  fun heat() {
    ThemeGalleryHeatPreview()
  }

  @CaliuPreviews
  @PreviewTest
  @Composable
  fun calm() {
    ThemeGalleryCalmPreview()
  }

  @CaliuPreviews
  @PreviewTest
  @Composable
  fun pause() {
    ThemeGalleryPausePreview()
  }

  @CaliuPreviews
  @PreviewTest
  @Composable
  fun off() {
    ThemeGalleryOffPreview()
  }
}
