package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType

/** Títol petit damunt d'una secció: sagnat 8 dp i a 8 dp de la targeta. */
@Composable
fun SectionLabel(
  text: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text,
    style = CaliuType.sectionLabel,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier =
      modifier
        .padding(start = CaliuSpacing.s, bottom = CaliuSpacing.s)
        .semantics { heading() },
  )
}

/** Una secció de la pantalla: el títol i el contingut a sota. */
@Composable
fun Section(
  label: String,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Column(modifier = modifier) {
    SectionLabel(label)
    content()
  }
}

@CaliuPreviews
@Composable
private fun SectionLabelPreview() {
  CaliuPreviewTheme {
    SectionLabel("Temperatura actual")
  }
}
