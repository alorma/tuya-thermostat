package com.alorma.caliu.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alorma.caliu.R
import com.alorma.caliu.designsystem.preview.CaliuPreviewTheme

// Pantalla provisional: se substituirà quan arribi el disseny
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
  Scaffold(modifier = modifier) { padding ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = stringResource(R.string.home_placeholder),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
  CaliuPreviewTheme {
    HomeScreen()
  }
}
