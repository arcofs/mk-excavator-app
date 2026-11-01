package io.github.arcofs.mkexcavator.control

import io.github.arcofs.mkexcavator.protocol.Channel
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GamepadDrivingTest {
  private fun TestScope.connected(): Pair<GamepadDriving, MachineController> {
    val machine = MachineController(FakeRadio(), backgroundScope) { testScheduler.currentTime }
    machine.connect()
    return GamepadDriving(ExcavatorControls(machine)) to machine
  }

  private fun MachineController.speed(module: Int, channel: Channel) = state.value.speeds[module, channel]

  @Test
  fun pushingTheLeftStickForwardLowersTheStick() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftY = 1f), canMove = true)
    // Stick is arm box channel B, where minus raises it.
    assertEquals(7, machine.speed(1, Channel.B))
  }

  @Test
  fun pushingTheRightStickForwardLowersTheBoom() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(rightY = 1f), canMove = true)
    // Boom is arm box channel D, where plus raises it.
    assertEquals(-7, machine.speed(1, Channel.D))
  }

  @Test
  fun sidewaysSlewsOnTheLeftAndWorksTheBucketOnTheRight() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftX = 1f, rightX = 1f), canMove = true)
    assertEquals(-ControlSettings.DEFAULT_SLEW_MAX_SPEED, machine.speed(1, Channel.C))
    assertEquals(7, machine.speed(1, Channel.A))
  }

  @Test
  fun triggersRunTheirTracksForward() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftTrigger = 1f, rightTrigger = 1f), canMove = true)
    assertEquals(7, machine.speed(2, Channel.C))
    assertEquals(7, machine.speed(2, Channel.A))
  }

  @Test
  fun holdingABumperRunsThatTrackInReverse() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftTrigger = 1f, leftBumper = true, rightTrigger = 1f), canMove = true)
    assertEquals(-7, machine.speed(2, Channel.C))
    assertEquals(7, machine.speed(2, Channel.A))
  }

  @Test
  fun aBumperOnItsOwnMovesNothing() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftBumper = true, rightBumper = true), canMove = true)
    assertEquals(MotorSpeeds.STOPPED, machine.state.value.speeds)
  }

  @Test
  fun aHalfPulledTriggerIsSlower() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftTrigger = 0.5f), canMove = true)
    val speed = machine.speed(2, Channel.C)
    assertTrue("speed $speed", speed in 1..<7)
  }

  @Test
  fun sticksIgnoreSidewaysDriftLikeTheScreen() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftX = 0.2f, leftY = 1f), canMove = true)
    assertEquals(0, machine.speed(1, Channel.C))
    assertEquals(7, machine.speed(1, Channel.B))
  }

  @Test
  fun aControlHeldThroughStopMustBeLetGoAfterResume() = runTest {
    val (gamepad, machine) = connected()
    val pulled = GamepadState(leftTrigger = 1f)
    gamepad.apply(pulled, canMove = true)
    machine.stopAll()

    gamepad.apply(pulled, canMove = false)
    machine.resume()
    gamepad.apply(pulled, canMove = true)
    assertEquals(0, machine.speed(2, Channel.C))

    gamepad.apply(GamepadState(), canMove = true)
    gamepad.apply(pulled, canMove = true)
    assertEquals(7, machine.speed(2, Channel.C))
  }

  @Test
  fun aControlLetGoDuringStopWorksStraightAfterResume() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftTrigger = 1f), canMove = true)
    machine.stopAll()

    gamepad.apply(GamepadState(), canMove = false)
    machine.resume()
    gamepad.apply(GamepadState(leftTrigger = 1f), canMove = true)
    assertEquals(7, machine.speed(2, Channel.C))
  }

  @Test
  fun aControlPushedWhileNothingCouldMoveWaitsToBeLetGo() = runTest {
    val (gamepad, machine) = connected()
    val pulled = GamepadState(rightTrigger = 1f)

    gamepad.apply(pulled, canMove = false)
    gamepad.apply(pulled, canMove = true)
    assertEquals(0, machine.speed(2, Channel.A))

    gamepad.apply(GamepadState(), canMove = true)
    gamepad.apply(pulled, canMove = true)
    assertEquals(7, machine.speed(2, Channel.A))
  }

  @Test
  fun nothingStartsWhileItCannotMove() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftY = 1f, rightTrigger = 1f), canMove = false)
    assertEquals(MotorSpeeds.STOPPED, machine.state.value.speeds)
  }

  @Test
  fun releaseAllBringsEverythingToRest() = runTest {
    val (gamepad, machine) = connected()
    gamepad.apply(GamepadState(leftY = 1f, rightX = 1f, leftTrigger = 1f, rightTrigger = 1f), canMove = true)
    gamepad.releaseAll()
    assertEquals(MotorSpeeds.STOPPED, machine.state.value.speeds)
  }
}
