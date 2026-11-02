package com.alorma.caliu.designsystem.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** El `LoadingIndicator` expressiu de M3, per a les pantalles que esperen una lectura. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaliuLoadingIndicator(modifier: Modifier = Modifier) {
  LoadingIndicator(modifier = modifier)
}
