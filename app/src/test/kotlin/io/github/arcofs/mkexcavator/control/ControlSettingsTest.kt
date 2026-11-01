package io.github.arcofs.mkexcavator.control

import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlSettingsTest {
  @Test
  fun defaultsAreTheTunedValues() {
    val settings = ControlSettings()

    assertEquals(0.15f, settings.deadZone)
    assertEquals(25f, settings.snapDegrees)
    assertEquals(2f, settings.curve)
    assertEquals(ControlSettings.DEFAULT_SLEW_MAX_SPEED, settings.maxSpeed(ExcavatorFunction.SLEW))
    assertEquals(MotorSpeeds.MAX_SPEED, settings.maxSpeed(ExcavatorFunction.BOOM))
    assertTrue(settings.reversed.isEmpty())
  }

  @Test
  fun coercedPullsEveryValueIntoRange() {
    val wild =
      ControlSettings(
        deadZone = 0.9f,
        snapDegrees = -5f,
        curve = 10f,
        maxSpeeds = mapOf(ExcavatorFunction.BOOM to 0, ExcavatorFunction.STICK to 12),
      )

    val settings = wild.coerced()

    assertEquals(ControlSettings.DEAD_ZONE_RANGE.endInclusive, settings.deadZone)
    assertEquals(ControlSettings.SNAP_DEGREES_RANGE.start, settings.snapDegrees)
    assertEquals(ControlSettings.CURVE_RANGE.endInclusive, settings.curve)
    assertEquals(1, settings.maxSpeed(ExcavatorFunction.BOOM))
    assertEquals(MotorSpeeds.MAX_SPEED, settings.maxSpeed(ExcavatorFunction.STICK))
  }

  @Test
  fun aFunctionMissingFromMaxSpeedsGetsItsDefault() {
    val settings = ControlSettings(maxSpeeds = emptyMap())

    assertEquals(ControlSettings.DEFAULT_SLEW_MAX_SPEED, settings.maxSpeed(ExcavatorFunction.SLEW))
    assertEquals(MotorSpeeds.MAX_SPEED, settings.maxSpeed(ExcavatorFunction.LEFT_TRACK))
  }
}
