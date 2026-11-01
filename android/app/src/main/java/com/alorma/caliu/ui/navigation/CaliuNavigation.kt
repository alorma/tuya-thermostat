package com.alorma.caliu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.alorma.caliu.ui.home.HomeScreen

// La pantalla de configuració (SettingsRoute) s'afegeix a la fase 5
@Composable
fun CaliuNavigation(modifier: Modifier = Modifier) {
  val backStack = rememberNavBackStack(HomeRoute)
  NavDisplay(
    backStack = backStack,
    modifier = modifier,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators =
      listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
      ),
    entryProvider =
      entryProvider {
        // La configuració encara no existeix: el botó no fa res fins a la fase 5
        entry<HomeRoute> { HomeScreen(onOpenSettings = {}) }
      },
  )
}
