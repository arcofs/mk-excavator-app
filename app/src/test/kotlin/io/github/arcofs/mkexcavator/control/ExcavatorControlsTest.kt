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
import org.junit.Test

class ExcavatorControlsTest {
  private fun TestScope.connected(): Pair<ExcavatorControls, MachineController> {
    val machine = MachineController(FakeRadio(), backgroundScope) { testScheduler.currentTime }
    machine.connect()
    return ExcavatorControls(machine) to machine
  }

  @Test
  fun deadZoneIsStopped() {
    assertEquals(0, ExcavatorControls.speedFor(0f))
    assertEquals(0, ExcavatorControls.speedFor(ExcavatorControls.DEAD_ZONE * 0.9f))
    assertEquals(0, ExcavatorControls.speedFor(-ExcavatorControls.DEAD_ZONE * 0.9f))
  }

  @Test
  fun justPastTheDeadZoneIsTheSlowestSpeed() {
    assertEquals(1, ExcavatorControls.speedFor(ExcavatorControls.DEAD_ZONE + 0.01f))
    assertEquals(-1, ExcavatorControls.speedFor(-ExcavatorControls.DEAD_ZONE - 0.01f))
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
  fun eachFunctionDrivesItsBoxAndChannel() = runTest {
    val (controls, machine) = connected()
    val expected =
      mapOf(
        LEFT_TRACK to Triple(2, Channel.C, 7),
        RIGHT_TRACK to Triple(2, Channel.A, 7),
        BUCKET to Triple(1, Channel.A, 7),
        STICK to Triple(1, Channel.B, -7),
        SLEW to Triple(1, Channel.C, -7),
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
  fun maxSpeedCapsFullDeflection() = runTest {
    val (controls, machine) = connected()
    controls.maxSpeed = 3

    controls.drive(LEFT_TRACK, -1f)

    assertEquals(-3, machine.state.value.speeds[2, Channel.C])
  }
}
