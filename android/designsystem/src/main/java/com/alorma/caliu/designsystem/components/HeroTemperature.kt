package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuShapes
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.designsystem.theme.CaliuType
import com.alorma.caliu.designsystem.theme.Mood

/** Targeta de temperatura actual: només la xifra gran i «°C», amb els colors hero del mood. */
@Composable
fun HeroTemperature(
  value: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  unit: String = "°C",
) {
  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .clearAndSetSemantics { this.contentDescription = contentDescription },
    shape = CaliuShapes.hero,
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
      ),
  ) {
    // Alçada fixa del handoff: les mètriques de Nunito fan la línia més alta que els 100 sp de l'estil
    Row(
      modifier =
        Modifier
          .height(134.dp)
          .padding(horizontal = 24.dp),
    ) {
      Text(
        text = value,
        style = CaliuType.tempHero,
        modifier =
          Modifier
            .fillMaxHeight()
            .wrapContentHeight(unbounded = true),
      )
      Text(
        text = unit,
        style = CaliuType.degreeUnit,
        color = CaliuTheme.colors.heroVariant,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp),
      )
    }
  }
}

@CaliuPreviews
@Composable
private fun HeroTemperaturePreview() {
  CaliuPreviewTheme(Mood.HEAT) {
    HeroTemperature(value = "19,3", contentDescription = "19,3 °C")
  }
}
