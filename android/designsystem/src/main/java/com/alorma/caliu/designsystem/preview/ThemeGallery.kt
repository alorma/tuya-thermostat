package com.alorma.caliu.designsystem.preview

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.theme.CaliuShapes
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood

/**
 * Galeria del tema: mostra els rols de color, la tipografia i les formes d'un mood. No és cap pantalla de l'app; serveix
 * per comparar el tema amb `design/tokens.json` i les captures del handoff.
 */
@Composable
fun ThemeGallery(
  mood: Mood,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val caliu = CaliuTheme.colors
  val (label, icon) = mood.sample()

  Column(
    modifier =
      modifier
        .width(360.dp)
        .background(colors.background)
        .padding(CaliuSpacing.screen),
    verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section),
  ) {
    // Xip d'estat (tertiary)
    Surface(shape = CircleShape, color = colors.tertiary, contentColor = colors.onTertiary) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.s),
      ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(label, style = CaliuType.statusChip)
        Icon(painterResource(R.drawable.ic_keyboard_arrow_down), contentDescription = null)
      }
    }

    // Targeta hero (tertiaryContainer)
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = CaliuShapes.hero,
      color = colors.tertiaryContainer,
      contentColor = colors.onTertiaryContainer,
    ) {
      Row(modifier = Modifier.padding(CaliuSpacing.xl), verticalAlignment = Alignment.Top) {
        Text(if (mood == Mood.OFF) "—" else "20,4", style = CaliuType.tempHero)
        Text("°C", style = CaliuType.degreeUnit, color = caliu.heroVariant, modifier = Modifier.padding(top = 8.dp))
      }
    }

    // Targeta de consigna (surfaceContainerLow) amb −/+ (surfaceContainerHigh)
    Column {
      Text(
        "Consigna",
        style = CaliuType.sectionLabel,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(start = CaliuSpacing.s, bottom = CaliuSpacing.s),
      )
      Surface(modifier = Modifier.fillMaxWidth(), shape = CaliuShapes.hero, color = colors.surfaceContainerLow) {
        Row(
          modifier = Modifier.padding(CaliuSpacing.screen),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          StepButton(R.drawable.ic_remove)
          Text(
            "20,0 °C",
            style = CaliuType.tempValue,
            color = colors.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = CaliuSpacing.s),
            textAlign = TextAlign.Center,
          )
          StepButton(R.drawable.ic_add)
        }
      }
    }

    // Bàner temporal i pista
    Surface(shape = CaliuShapes.control, color = caliu.temporary, contentColor = caliu.onTemporary) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = CaliuSpacing.screen, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.s),
      ) {
        Icon(painterResource(R.drawable.ic_schedule_filled), contentDescription = null, modifier = Modifier.size(20.dp))
        Text("Temporal fins a les 22:00", style = CaliuType.button)
      }
    }

    // Rols
    Row(horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.s)) {
      Swatch(colors.primary, "pri")
      Swatch(colors.primaryContainer, "priC")
      Swatch(caliu.dial, "dial")
      Swatch(colors.surfaceContainerHigh, "high")
      Swatch(colors.outline, "out")
      Swatch(colors.error, "err")
    }

    // Tipografia
    Column(verticalArrangement = Arrangement.spacedBy(CaliuSpacing.xs)) {
      Text("Configuració", style = CaliuType.screenTitle, color = colors.onSurface)
      Text("Sense connexió a internet", style = CaliuType.stateTitle, color = colors.onSurface)
      Text("Comprova la wifi o les dades mòbils.", style = CaliuType.body, color = colors.onSurfaceVariant)
      Row(horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.screen)) {
        Text("06:00 – 08:30", style = CaliuType.row, color = caliu.onSurfacePast)
        Text("21,0°", style = CaliuType.tempRow, color = colors.onSurface)
        Text("21°", style = CaliuType.presetLabel, color = colors.primary)
      }
      Text("Actualitzat a les 18:42", style = CaliuType.bodySmall, color = colors.onSurfaceVariant)
    }
  }
}

@Composable
private fun StepButton(
  @DrawableRes icon: Int,
) {
  Surface(
    shape = CaliuShapes.control,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
      Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
    }
  }
}

@Composable
private fun Swatch(
  color: Color,
  name: String,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier =
        Modifier
          .size(44.dp)
          .background(color, CaliuShapes.chip)
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CaliuShapes.chip),
    )
    Spacer(Modifier.size(2.dp))
    Text(name, style = CaliuType.overline, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

private fun Mood.sample(): Pair<String, Int> =
  when (this) {
    Mood.HEAT -> "Escalfant" to R.drawable.ic_local_fire_department_filled
    Mood.CALM -> "En repòs" to R.drawable.ic_check_circle_filled
    Mood.PAUSE -> "En pausa" to R.drawable.ic_pause_circle_filled
    Mood.OFF -> "Apagat" to R.drawable.ic_power_settings_new
  }

@CaliuPreviews
@Composable
fun ThemeGalleryHeatPreview() {
  CaliuPreviewTheme(Mood.HEAT) { ThemeGallery(Mood.HEAT) }
}

@CaliuPreviews
@Composable
fun ThemeGalleryCalmPreview() {
  CaliuPreviewTheme(Mood.CALM) { ThemeGallery(Mood.CALM) }
}

@CaliuPreviews
@Composable
fun ThemeGalleryPausePreview() {
  CaliuPreviewTheme(Mood.PAUSE) { ThemeGallery(Mood.PAUSE) }
}

@CaliuPreviews
@Composable
fun ThemeGalleryOffPreview() {
  CaliuPreviewTheme(Mood.OFF) { ThemeGallery(Mood.OFF) }
}
