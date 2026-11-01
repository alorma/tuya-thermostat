package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuShapes
import com.alorma.caliu.designsystem.theme.CaliuSpacing

/** Lloc del xip d'estat mentre es carrega. */
@Composable
fun StatusChipSkeleton(modifier: Modifier = Modifier) {
  Block(modifier.size(width = 148.dp, height = 48.dp), CircleShape)
}

/**
 * Càrrega inicial: blocs amb la mateixa estructura que la pantalla (temperatura, consigna, dreceres, mode i programa).
 */
@Composable
fun HomeSkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(CaliuSpacing.section),
  ) {
    // Temperatura
    Card(height = 168.dp, shape = CaliuShapes.hero) {
      Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
        Line(width = 200.dp, height = 84.dp, shape = CaliuShapes.control)
        Spacer(Modifier.height(CaliuSpacing.s))
        Row(verticalAlignment = Alignment.Top) {
          Line(width = 120.dp)
          Spacer(Modifier.weight(1f))
          Line(width = 120.dp, height = 40.dp, shape = CircleShape)
        }
      }
    }
    // Consigna
    Card(height = 96.dp, shape = CaliuShapes.hero)
    // Dreceres
    Row(horizontalArrangement = Arrangement.spacedBy(CaliuSpacing.joint)) {
      val inner = 8.dp
      val outer = 24.dp
      Block(Modifier.weight(1f).height(52.dp), RoundedCornerShape(outer, inner, inner, outer))
      Block(Modifier.weight(1f).height(52.dp), RoundedCornerShape(inner))
      Block(Modifier.weight(1f).height(52.dp), RoundedCornerShape(inner, outer, outer, inner))
    }
    // Mode
    Block(Modifier.fillMaxWidth().height(48.dp), CircleShape)
    // Programa
    Card(height = 200.dp, shape = CaliuShapes.control) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Line(width = 140.dp, height = 12.dp)
        listOf(300.dp, 274.dp, 313.dp, 287.dp, 293.dp, 261.dp).forEach { Line(width = it, height = 14.dp) }
      }
    }
    Card(height = 64.dp, shape = CaliuShapes.hero)
  }
}

@Composable
private fun Card(
  height: Dp,
  shape: Shape,
  content: @Composable () -> Unit = {},
) {
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(height)
        .background(MaterialTheme.colorScheme.surfaceContainerLow, shape),
  ) {
    content()
  }
}

@Composable
private fun Line(
  width: Dp,
  height: Dp = 14.dp,
  shape: Shape = CircleShape,
) {
  Box(
    Modifier
      .width(width)
      .height(height)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape),
  )
}

@Composable
private fun Block(
  modifier: Modifier,
  shape: Shape,
) {
  Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow, shape))
}

@CaliuPreviews
@Composable
private fun HomeSkeletonPreview() {
  CaliuPreviewTheme {
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CaliuSpacing.screen)) {
      StatusChipSkeleton()
      Spacer(Modifier.height(CaliuSpacing.section))
      HomeSkeleton()
    }
  }
}
