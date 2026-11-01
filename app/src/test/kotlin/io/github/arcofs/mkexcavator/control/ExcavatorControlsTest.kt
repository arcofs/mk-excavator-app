package io.github.arcofs.mkexcavator.control

import io.github.arcofs.mkexcavator.control.ExcavatorFunction.BOOM
import io.github.arcofs.mkexcavator.control.ExcavatorFunction.BUCKET
import io.github.arcofs.mkexcavator.control.ExcavatorFunction.LEFT_TRACK
import io.github.arcofs.mkexcavator.control.ExcavatorFunction.RIGHT_TRACK
import io.github.arcofs.mkexcavator.control.ExcavatorFunction.SLEW
import io.github.arcofs.mkexcavator.control.ExcavatorFunction.STICK
import io.github.arcofs.mkexcavator.protocol.Channel
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcavatorControlsTest {
  private val deadZone = ControlSettings.DEFAULT_DEAD_ZONE

  private fun TestScope.connected(): Pair<ExcavatorControls, MachineController> {
    val machine = MachineController(FakeRadio(), backgroundScope) { testScheduler.currentTime }
    machine.connect()
    return ExcavatorControls(machine) to machine
  }

  @Test
  fun deadZoneIsStopped() {
    assertEquals(0, ExcavatorControls.speedFor(0f))
    assertEquals(0, ExcavatorControls.speedFor(deadZone * 0.9f))
    assertEquals(0, ExcavatorControls.speedFor(-deadZone * 0.9f))
  }

  @Test
  fun justPastTheDeadZoneIsTheSlowestSpeed() {
    assertEquals(1, ExcavatorControls.speedFor(deadZone + 0.01f))
    assertEquals(-1, ExcavatorControls.speedFor(-deadZone - 0.01f))
  }

  @Test
  fun fullDeflectionIsFullSpeedEitherWay() {
    assertEquals(MotorSpeeds.MAX_SPEED, ExcavatorControls.speedFor(1f))
    assertEquals(-MotorSpeeds.MAX_SPEED, ExcavatorControls.speedFor(-1f))
    assertEquals(MotorSpeeds.MAX_SPEED, ExcavatorControls.speedFor(1.5f))
  }

  @Test
  fun speedRisesWithDeflection() {
    val speeds = (0..20).map { ExcavatorControls.speedFor(it / 20f) }
    assertEquals(speeds.sorted(), speeds)
  }

  @Test
  fun halfTravelGivesLessThanHalfSpeedForFinerControl() {
    val halfTravel = deadZone + (1f - deadZone) / 2
    assertTrue(ExcavatorControls.speedFor(halfTravel) < MotorSpeeds.MAX_SPEED / 2f)
  }

  @Test
  fun aLinearCurveGivesHalfSpeedAtHalfTravel() {
    val halfTravel = deadZone + (1f - deadZone) / 2
    assertEquals(4, ExcavatorControls.speedFor(halfTravel, curve = 1f))
  }

  @Test
  fun aWiderDeadZoneNeedsMoreDeflection() {
    assertEquals(0, ExcavatorControls.speedFor(0.25f, deadZone = 0.3f))
    assertEquals(1, ExcavatorControls.speedFor(0.32f, deadZone = 0.3f))
  }

  @Test
  fun eachFunctionDrivesItsBoxAndChannel() = runTest {
    val (controls, machine) = connected()
    val expected =
      mapOf(
        LEFT_TRACK to Triple(2, Channel.C, 7),
        RIGHT_TRACK to Triple(2, Channel.A, 7),
        BUCKET to Triple(1, Channel.A, 7),
        STICK to Triple(1, Channel.B, -7),
        SLEW to Triple(1, Channel.C, -ControlSettings.DEFAULT_SLEW_MAX_SPEED),
        BOOM to Triple(1, Channel.D, 7),
      )
    for ((function, target) in expected) {
      controls.drive(function, 1f)
      val (module, channel, speed) = target
      assertEquals(function.name, speed, machine.state.value.speeds[module, channel])
      controls.release(function)
      assertEquals(function.name, MotorSpeeds.STOPPED, machine.state.value.speeds)
    }
  }

  @Test
  fun swappingBoxesSwapsTheirModules() = runTest {
    val (controls, machine) = connected()
    controls.boxesSwapped = true

    controls.drive(RIGHT_TRACK, 1f)
    controls.drive(BOOM, 1f)

    assertEquals(7, machine.state.value.speeds[1, Channel.A])
    assertEquals(7, machine.state.value.speeds[2, Channel.D])
  }

  @Test
  fun aDeflectionInsideTheDeadZoneStopsTheFunction() = runTest {
    val (controls, machine) = connected()
    controls.drive(BOOM, 1f)

    controls.drive(BOOM, 0.05f)

    assertEquals(MotorSpeeds.STOPPED, machine.state.value.speeds)
  }

  @Test
  fun eachFunctionUsesItsOwnTopSpeed() = runTest {
    val (controls, machine) = connected()
    controls.settings = ControlSettings(maxSpeeds = mapOf(LEFT_TRACK to 3))

    controls.drive(LEFT_TRACK, -1f)

    assertEquals(-3, machine.state.value.speeds[2, Channel.C])
  }

  @Test
  fun aReversedFunctionDrivesTheOtherWay() = runTest {
    val (controls, machine) = connected()
    controls.settings = ControlSettings(reversed = setOf(BUCKET))

    controls.drive(BUCKET, 1f)

    assertEquals(-7, machine.state.value.speeds[1, Channel.A])
  }

  @Test
  fun theStickSnapsDriftUsingTheSnapSetting() = runTest {
    val (controls, machine) = connected()

    controls.driveStick(SLEW, STICK, x = 0.3f, y = 1f)
    assertEquals(0, machine.state.value.speeds[1, Channel.C])
    assertEquals(-7, machine.state.value.speeds[1, Channel.B])

    controls.settings = ControlSettings(snapDegrees = 0f)
    controls.driveStick(SLEW, STICK, x = 0.3f, y = 1f)
    assertTrue(machine.state.value.speeds[1, Channel.C] != 0)
  }
}
