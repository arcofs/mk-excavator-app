package io.github.arcofs.mkexcavator

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.arcofs.mkexcavator.ui.debug.DebugScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(TestBench)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = entryProvider { entry<TestBench> { DebugScreen() } },
  )
}
