package io.github.arcofs.mkexcavator.ui.drive

import io.github.arcofs.mkexcavator.control.ExcavatorFunction
import io.github.arcofs.mkexcavator.control.FakeRadio
import io.github.arcofs.mkexcavator.control.MachineController
import io.github.arcofs.mkexcavator.control.MachineController.Link
import io.github.arcofs.mkexcavator.protocol.Channel
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import io.github.arcofs.mkexcavator.settings.FakeSettingsRepository
import io.github.arcofs.mkexcavator.ui.drive.DriveViewModel.GuideStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DriveViewModelTest {
  private val radio = FakeRadio()
  private val settings = FakeSettingsRepository()

  @Before
  fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @After
  fun tearDown() = Dispatchers.resetMain()

  private fun TestScope.setUp(): Pair<DriveViewModel, MachineController> {
    val machine = MachineController(radio, backgroundScope) { testScheduler.currentTime }
    return DriveViewModel(machine, settings) to machine
  }

  private fun TestScope.connectThroughGuide(viewModel: DriveViewModel) {
    viewModel.openGuide()
    viewModel.connect()
    advanceTimeBy(MachineController.CONNECT_MS + MachineController.TICK_MS)
    runCurrent()
  }

  @Test
  fun guideWalksFromSwitchOnToPressingTheTrackBoxButton() = runTest {
    val (viewModel, _) = setUp()

    viewModel.openGuide()
    assertEquals(GuideStep.SWITCH_ON, viewModel.state.value.guide)

    viewModel.connect()
    assertEquals(GuideStep.CONNECTING, viewModel.state.value.guide)

    advanceTimeBy(MachineController.CONNECT_MS + MachineController.TICK_MS)
    runCurrent()
    assertEquals(GuideStep.PRESS_BUTTON, viewModel.state.value.guide)
    assertFalse(viewModel.state.value.canDrive)

    viewModel.finishGuide(armBoxPressed = false)
    assertNull(viewModel.state.value.guide)
    assertTrue(viewModel.state.value.canDrive)
  }

  @Test
  fun controlsDoNothingUntilTheGuideIsFinished() = runTest {
    val (viewModel, machine) = setUp()
    connectThroughGuide(viewModel)

    viewModel.move(ExcavatorFunction.RIGHT_TRACK, 1f)
    assertEquals(MotorSpeeds.STOPPED, machine.state.value.speeds)

    viewModel.finishGuide(armBoxPressed = false)
    viewModel.move(ExcavatorFunction.RIGHT_TRACK, 1f)
    assertEquals(7, machine.state.value.speeds[2, Channel.A])
  }

  @Test
  fun pressingTheArmBoxInsteadSwapsTheBoxes() = runTest {
    val (viewModel, machine) = setUp()
    connectThroughGuide(viewModel)

    viewModel.finishGuide(armBoxPressed = true)
    viewModel.move(ExcavatorFunction.RIGHT_TRACK, 1f)

    assertTrue(viewModel.state.value.boxesSwapped)
    assertEquals(7, machine.state.value.speeds[1, Channel.A])
  }

  @Test
  fun swappingStopsEverythingFirst() = runTest {
    val (viewModel, machine) = setUp()
    connectThroughGuide(viewModel)
    viewModel.finishGuide(armBoxPressed = false)
    viewModel.move(ExcavatorFunction.BOOM, 1f)

    viewModel.toggleSwap()

    assertEquals(MotorSpeeds.STOPPED, machine.state.value.speeds)
    assertTrue(viewModel.state.value.boxesSwapped)
  }

  @Test
  fun cancellingWhileConnectingDisconnects() = runTest {
    val (viewModel, machine) = setUp()
    viewModel.openGuide()
    viewModel.connect()

    viewModel.cancelGuide()

    assertNull(viewModel.state.value.guide)
    assertEquals(Link.DISCONNECTED, machine.state.value.link)
  }

  @Test
  fun losingTheConnectionClosesTheGuideAndLocksTheControls() = runTest {
    val (viewModel, machine) = setUp()
    connectThroughGuide(viewModel)

    machine.disconnect()
    runCurrent()

    assertNull(viewModel.state.value.guide)
    assertFalse(viewModel.state.value.canDrive)
  }

  @Test
  fun stopLatchesAndLocksTheControlsUntilTappedAgain() = runTest {
    val (viewModel, machine) = setUp()
    connectThroughGuide(viewModel)
    viewModel.finishGuide(armBoxPressed = false)
    viewModel.move(ExcavatorFunction.BOOM, 1f)

    viewModel.onStopButton()
    assertTrue(viewModel.state.value.stopped)
    assertFalse(viewModel.state.value.canDrive)
    viewModel.move(ExcavatorFunction.BOOM, 1f)
    assertEquals(MotorSpeeds.STOPPED, machine.state.value.speeds)

    viewModel.onStopButton()
    assertFalse(viewModel.state.value.stopped)
    viewModel.release(ExcavatorFunction.BOOM)
    viewModel.move(ExcavatorFunction.BOOM, 1f)
    assertEquals(7, machine.state.value.speeds[1, Channel.D])
  }

  @Test
  fun savedSettingsApplyToTheControls() = runTest {
    val (viewModel, machine) = setUp()
    connectThroughGuide(viewModel)
    viewModel.finishGuide(armBoxPressed = false)

    settings.update { it.copy(maxSpeeds = it.maxSpeeds + (ExcavatorFunction.BOOM to 2), reversed = setOf(ExcavatorFunction.BOOM)) }
    viewModel.move(ExcavatorFunction.BOOM, 1f)

    assertEquals(-2, machine.state.value.speeds[1, Channel.D])
  }

  @Test
  fun savedSettingsReachTheScreenForItsLabelsAndDeadZoneRing() = runTest {
    val (viewModel, _) = setUp()

    settings.update { it.copy(deadZone = 0.3f, reversed = setOf(ExcavatorFunction.SLEW)) }

    assertEquals(0.3f, viewModel.state.value.settings.deadZone)
    assertEquals(setOf(ExcavatorFunction.SLEW), viewModel.state.value.settings.reversed)
  }

  @Test
  fun aJoystickIgnoresSidewaysDrift() = runTest {
    val (viewModel, machine) = setUp()
    connectThroughGuide(viewModel)
    viewModel.finishGuide(armBoxPressed = false)

    viewModel.moveStick(ExcavatorFunction.BUCKET, ExcavatorFunction.BOOM, x = 0.2f, y = 1f)

    assertEquals(0, machine.state.value.speeds[1, Channel.A])
    assertEquals(7, machine.state.value.speeds[1, Channel.D])
  }
}
