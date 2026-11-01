package com.alorma.caliu.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuShapes
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood

private const val DISABLED_ALPHA = 0.38f
private const val PENDING_VALUE_ALPHA = 0.6f
private val HeroRadius = 32.dp

/**
 * Consigna en manual: −, el valor i + (botons de 72 dp).
 *
 * Amb [pending] es mostra la consigna que es recuperarà en sortir de la pausa: el valor atenuat, la targeta amb vora
 * discontínua i els botons desactivats.
 */
@Composable
fun SetpointControl(
  value: String,
  onDecrease: () -> Unit,
  onIncrease: () -> Unit,
  decreaseLabel: String,
  increaseLabel: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  canDecrease: Boolean = true,
  canIncrease: Boolean = true,
  pending: Boolean = false,
) {
  val outline = MaterialTheme.colorScheme.outline
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .then(if (pending) Modifier.dashedBorder(outline, HeroRadius) else Modifier),
    shape = CaliuShapes.hero,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StepButton(
        icon = R.drawable.ic_remove,
        contentDescription = decreaseLabel,
        onClick = onDecrease,
        enabled = enabled && !pending && canDecrease,
        size = 72.dp,
        iconSize = 32.dp,
        shape = CaliuShapes.control,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
      )
      Text(
        text = value,
        style = CaliuType.tempValue,
        textAlign = TextAlign.Center,
        modifier =
          Modifier
            .weight(1f)
            .alpha(if (pending) PENDING_VALUE_ALPHA else 1f),
      )
      StepButton(
        icon = R.drawable.ic_add,
        contentDescription = increaseLabel,
        onClick = onIncrease,
        enabled = enabled && !pending && canIncrease,
        size = 72.dp,
        iconSize = 32.dp,
        shape = CaliuShapes.control,
        container = MaterialTheme.colorScheme.surfaceContainerHigh,
      )
    }
  }
}

/** Botó −/+ tonal. Desactivat manté el fons i atenua la icona. */
@Composable
internal fun StepButton(
  @DrawableRes icon: Int,
  contentDescription: String,
  onClick: () -> Unit,
  enabled: Boolean,
  size: Dp,
  iconSize: Dp,
  shape: Shape,
  container: Color,
  content: Color = MaterialTheme.colorScheme.onSurface,
) {
  FilledTonalIconButton(
    onClick = onClick,
    enabled = enabled,
    shape = shape,
    colors =
      IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = container,
        contentColor = content,
        disabledContainerColor = container,
        disabledContentColor = content.copy(alpha = DISABLED_ALPHA),
      ),
    modifier = Modifier.size(size),
  ) {
    Icon(painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(iconSize))
  }
}

private fun Modifier.dashedBorder(
  color: Color,
  radius: Dp,
): Modifier =
  drawWithContent {
    drawContent()
    val stroke = 2.dp.toPx()
    val dash = 8.dp.toPx()
    drawRoundRect(
      color = color,
      topLeft = Offset(stroke / 2, stroke / 2),
      size = Size(size.width - stroke, size.height - stroke),
      cornerRadius = CornerRadius(radius.toPx()),
      style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash / 2))),
    )
  }

@CaliuPreviews
@Composable
private fun SetpointControlPreview() {
  Column(verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section)) {
    CaliuPreviewTheme(Mood.CALM) {
      SetpointControl("21,0 °C", {}, {}, "Baixa", "Puja")
    }
    CaliuPreviewTheme(Mood.PAUSE) {
      SetpointControl("21,0 °C", {}, {}, "Baixa", "Puja", pending = true)
    }
  }
}
