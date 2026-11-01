package io.github.arcofs.mkexcavator

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.arcofs.mkexcavator.ui.debug.DebugScreen
import io.github.arcofs.mkexcavator.ui.drive.DriveScreen
import io.github.arcofs.mkexcavator.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Drive)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Drive> { DriveScreen(onOpenSettings = { backStack.add(Settings) }) }
        entry<Settings> {
          SettingsScreen(onBack = { backStack.removeLastOrNull() }, onOpenTestBench = { backStack.add(TestBench) })
        }
        entry<TestBench> { DebugScreen(onBack = { backStack.removeLastOrNull() }) }
      },
  )
}
