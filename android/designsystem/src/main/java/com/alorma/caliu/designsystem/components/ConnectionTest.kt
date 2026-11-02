package com.alorma.caliu.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuShapes
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood
import com.alorma.caliu.designsystem.theme.moodPalette

private val ButtonHeight = 56.dp
private val IndicatorSize = 24.dp

/**
 * «Provar connexió»: `FilledTonalButton` a tota l'amplada. Mentre prova ([testing]) mostra un `LoadingIndicator`
 * petit en lloc de la icona i no es pot tornar a tocar.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectionTestButton(
  text: String,
  testing: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  FilledTonalButton(
    onClick = onClick,
    enabled = enabled && !testing,
    modifier =
      modifier
        .fillMaxWidth()
        .heightIn(min = ButtonHeight),
    colors =
      ButtonDefaults.filledTonalButtonColors(
        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (testing) 1f else 0.38f),
        disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (testing) 1f else 0.38f),
      ),
  ) {
    if (testing) {
      LoadingIndicator(
        modifier = Modifier.size(IndicatorSize),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
    } else {
      Icon(painterResource(R.drawable.ic_network_check), contentDescription = null)
    }
    Spacer(Modifier.width(CaliuSpacing.section))
    Text(text, style = CaliuType.button.copy(fontSize = CaliuType.bodyLarge.fontSize))
  }
}

/**
 * Resultat de «Provar connexió»: correcte amb els colors hero de «en repòs» (verd aigua), o error en `errorContainer`
 * amb la causa a sota ([detail]).
 */
@Composable
fun ConnectionResultCard(
  success: Boolean,
  title: String,
  modifier: Modifier = Modifier,
  detail: String? = null,
  darkTheme: Boolean = isSystemInDarkTheme(),
) {
  val calm = moodPalette(Mood.CALM, darkTheme)
  val container: Color
  val content: Color

  @DrawableRes val icon: Int
  if (success) {
    container = calm.hero
    content = calm.onHero
    icon = R.drawable.ic_check_circle_filled
  } else {
    container = MaterialTheme.colorScheme.errorContainer
    content = MaterialTheme.colorScheme.onErrorContainer
    icon = R.drawable.ic_error_filled
  }
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .semantics { liveRegion = LiveRegionMode.Polite },
    shape = CaliuShapes.control,
    color = container,
    contentColor = content,
  ) {
    Row(
      modifier =
        Modifier
          .heightIn(min = 52.dp)
          .padding(horizontal = CaliuSpacing.screen, vertical = 14.dp),
      verticalAlignment = if (detail == null) Alignment.CenterVertically else Alignment.Top,
    ) {
      Icon(painterResource(icon), contentDescription = null)
      Spacer(Modifier.width(CaliuSpacing.l))
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style =
            if (detail ==
              null
            ) {
              CaliuType.bodyLarge
            } else {
              CaliuType.button.copy(fontSize = CaliuType.bodyLarge.fontSize)
            },
        )
        if (detail != null) Text(detail, style = CaliuType.body)
      }
    }
  }
}

@CaliuPreviews
@Composable
private fun ConnectionTestPreview() {
  CaliuPreviewTheme {
    Column(verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section)) {
      ConnectionTestButton(text = "Provar connexió", testing = false, onClick = {})
      ConnectionTestButton(text = "Provant connexió…", testing = true, onClick = {})
      ConnectionResultCard(success = true, title = "Connexió correcta")
      ConnectionResultCard(
        success = false,
        title = "No s'ha pogut connectar",
        detail = "Les credencials de Tuya no són vàlides.",
      )
    }
  }
}
