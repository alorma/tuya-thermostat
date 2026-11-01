package com.alorma.caliu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alorma.caliu.designsystem.theme.CaliuTheme
import com.alorma.caliu.ui.navigation.CaliuNavigation

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CaliuTheme {
        CaliuNavigation()
      }
    }
  }
}
