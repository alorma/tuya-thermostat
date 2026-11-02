package com.alorma.caliu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.alorma.caliu.ui.home.HomeScreen
import com.alorma.caliu.ui.settings.SettingsScreen

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
        entry<HomeRoute> { HomeScreen(onOpenSettings = { backStack.add(SettingsRoute) }) }
        // Desar torna a la principal, que es refresca sola quan el CredentialsStore avisa que s'ha desat
        entry<SettingsRoute> {
          SettingsScreen(
            onBack = { backStack.removeLastOrNull() },
            onSaved = { backStack.removeLastOrNull() },
          )
        }
      },
  )
}
