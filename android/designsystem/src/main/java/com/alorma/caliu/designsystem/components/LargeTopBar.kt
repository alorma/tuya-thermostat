package com.alorma.caliu.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.alorma.caliu.designsystem.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme
import com.alorma.caliu.designsystem.preview.CaliuPreviews
import com.alorma.caliu.designsystem.theme.CaliuSpacing
import com.alorma.caliu.designsystem.theme.CaliuType

/**
 * Barra gran d'una pantalla secundària (Configuració): `LargeFlexibleTopAppBar` amb `arrow_back`, el títol gran i un
 * botó d'acció principal ([actionLabel], per exemple «Desa»). El títol gran és `displaySmall` de `CaliuTypography`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaliuLargeTopBar(
  title: String,
  backLabel: String,
  onBack: () -> Unit,
  actionLabel: String,
  onAction: () -> Unit,
  modifier: Modifier = Modifier,
  actionEnabled: Boolean = true,
  scrollBehavior: TopAppBarScrollBehavior? = null,
) {
  val background = MaterialTheme.colorScheme.background
  LargeFlexibleTopAppBar(
    // Sense estil fix: la barra posa displaySmall (screenTitle, 40 sp) desplegada i titleLarge plegada
    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    modifier = modifier,
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = backLabel)
      }
    },
    actions = {
      Button(
        onClick = onAction,
        enabled = actionEnabled,
        modifier = Modifier.padding(end = CaliuSpacing.s),
      ) {
        Text(actionLabel, style = CaliuType.button)
      }
    },
    colors =
      TopAppBarDefaults.topAppBarColors(
        containerColor = background,
        scrolledContainerColor = background,
      ),
    scrollBehavior = scrollBehavior,
  )
}

@CaliuPreviews
@Composable
private fun CaliuLargeTopBarPreview() {
  CaliuPreviewTheme {
    CaliuLargeTopBar(title = "Configuració", backLabel = "Enrere", onBack = {}, actionLabel = "Desa", onAction = {})
  }
}
