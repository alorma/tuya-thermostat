package com.alorma.caliu.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuShapes
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType

/** Com es presenta un [StatePanel]: dins d'una targeta (errors) o centrat sobre el fons (estats buits). */
enum class StatePanelStyle { Card, Centered }

/**
 * Panell per als errors i els estats buits: icona dins d'un contenidor, títol, explicació i accions. Les accions es
 * passen com a [actions], normalment amb [StatePanelButton].
 */
@Composable
fun StatePanel(
  @DrawableRes icon: Int,
  title: String,
  modifier: Modifier = Modifier,
  body: String? = null,
  style: StatePanelStyle = StatePanelStyle.Card,
  isError: Boolean = false,
  actions: @Composable ColumnScope.() -> Unit = {},
) {
  val colors = MaterialTheme.colorScheme
  val iconContainer =
    when {
      isError -> colors.errorContainer
      style == StatePanelStyle.Centered -> colors.surfaceContainerLow
      else -> colors.surfaceContainerHigh
    }
  val iconColor = if (isError) colors.onErrorContainer else colors.onSurface
  val titleColor = if (isError) colors.error else colors.onSurface
  val centered = style == StatePanelStyle.Centered
  val textAlign = if (centered) TextAlign.Center else TextAlign.Start

  val content: @Composable ColumnScope.() -> Unit = {
    IconBox(
      icon = icon,
      size = if (centered) 96.dp else 64.dp,
      container = iconContainer,
      color = iconColor,
    )
    Spacer(Modifier.height(CaliuSpacing.l))
    Text(
      text = title,
      style = CaliuType.stateTitle,
      color = titleColor,
      textAlign = textAlign,
      modifier = Modifier.semantics { heading() },
    )
    if (body != null) {
      Spacer(Modifier.height(CaliuSpacing.section))
      Text(text = body, style = CaliuType.body, color = colors.onSurfaceVariant, textAlign = textAlign)
    }
    Spacer(Modifier.height(CaliuSpacing.xl))
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(CaliuSpacing.s),
      modifier = if (centered) Modifier else Modifier.fillMaxWidth(),
      content = actions,
    )
  }

  if (centered) {
    Column(
      modifier = modifier.padding(horizontal = CaliuSpacing.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
      content = content,
    )
  } else {
    Card(
      modifier = modifier.fillMaxWidth(),
      shape = CaliuShapes.hero,
      colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
    ) {
      Column(modifier = Modifier.padding(CaliuSpacing.xl), content = content)
    }
  }
}

@Composable
private fun IconBox(
  @DrawableRes icon: Int,
  size: Dp,
  container: Color,
  color: Color,
) {
  Surface(shape = if (size > 64.dp) CaliuShapes.hero else CaliuShapes.control, color = container) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
      Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(size / 2))
    }
  }
}

/** Importància d'un botó de [StatePanel]: ple (`primary`), tonal o de text. */
enum class StatePanelButtonKind { Filled, Tonal, Text }

/** Botó d'un [StatePanel]: 56 dp d'alçada, amb icona opcional. */
@Composable
fun ColumnScope.StatePanelButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  @DrawableRes icon: Int? = null,
  kind: StatePanelButtonKind = StatePanelButtonKind.Filled,
  fillWidth: Boolean = true,
) {
  val buttonModifier =
    modifier
      .height(56.dp)
      .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
  val padding = ButtonDefaults.ContentPadding
  val content: @Composable () -> Unit = {
    if (icon != null) {
      Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
      Spacer(Modifier.size(CaliuSpacing.s))
    }
    Text(text, style = CaliuType.button)
  }
  when (kind) {
    StatePanelButtonKind.Filled ->
      Button(onClick = onClick, modifier = buttonModifier, contentPadding = padding) { content() }
    StatePanelButtonKind.Tonal ->
      FilledTonalButton(onClick = onClick, modifier = buttonModifier, contentPadding = padding) { content() }
    StatePanelButtonKind.Text ->
      TextButton(
        onClick = onClick,
        modifier = buttonModifier,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
      ) { content() }
  }
}

@CaliuPreviews
@Composable
private fun StatePanelErrorPreview() {
  CaliuPreviewTheme {
    StatePanel(
      icon = R.drawable.ic_error_filled,
      title = "Error de l'API de Tuya",
      body = "La prova de l'IoT Core ha caducat; renova-la a la consola de Tuya.",
      isError = true,
    ) {
      StatePanelButton("Obrir iot.tuya.com", onClick = {}, icon = R.drawable.ic_open_in_new)
      StatePanelButton("Torna-ho a provar", onClick = {}, kind = StatePanelButtonKind.Text)
    }
  }
}

@CaliuPreviews
@Composable
private fun StatePanelCenteredPreview() {
  CaliuPreviewTheme {
    StatePanel(
      icon = R.drawable.ic_key_filled,
      title = "Falta la configuració",
      body = "Connecta el teu compte de Tuya per veure el termòstat.",
      style = StatePanelStyle.Centered,
    ) {
      StatePanelButton("Configura", onClick = {}, icon = R.drawable.ic_settings_filled, fillWidth = false)
    }
  }
}
