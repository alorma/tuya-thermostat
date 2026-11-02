package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
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

/**
 * Una secció de la pantalla: el títol i el contingut a sota. Amb [actionLabel], un botó de text a la dreta del títol
 * («Edita»), que no fa més alta la fila del títol: es centra amb el text i en sobresurt.
 */
@Composable
fun Section(
  label: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: () -> Unit = {},
  content: @Composable () -> Unit,
) {
  Column(modifier = modifier) {
    if (actionLabel == null) {
      SectionLabel(label)
    } else {
      SectionHeader(label = label, actionLabel = actionLabel, onAction = onAction)
    }
    content()
  }
}

@Composable
private fun SectionHeader(
  label: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Layout(
    content = {
      SectionLabel(label)
      TextButton(onClick = onAction) {
        Text(actionLabel, style = CaliuType.sectionLabel)
      }
    },
  ) { measurables, constraints ->
    val loose = constraints.copy(minWidth = 0, minHeight = 0)
    val action = measurables[1].measure(loose)
    val title = measurables[0].measure(loose.copy(maxWidth = (constraints.maxWidth - action.width).coerceAtLeast(0)))
    val width = constraints.maxWidth
    layout(width, title.height) {
      title.place(0, 0)
      // El centre del botó, al centre del text (el títol porta 8 dp de marge a sota)
      val textCenter = (title.height - CaliuSpacing.s.roundToPx()) / 2
      action.place(width - action.width, textCenter - action.height / 2)
    }
  }
}

@CaliuPreviews
@Composable
private fun SectionLabelPreview() {
  CaliuPreviewTheme {
    Column {
      SectionLabel("Temperatura actual")
      Section("Programa d'avui", actionLabel = "Edita") {}
    }
  }
}
