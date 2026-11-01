package io.github.arcofs.mkexcavator.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MotorSpeedsTest {
  @Test
  fun startsWithEveryChannelStopped() {
    for (module in 1..MotorSpeeds.MODULES) {
      for (channel in Channel.entries) {
        assertEquals(0, MotorSpeeds.STOPPED[module, channel])
      }
    }
  }

  @Test
  fun withSetsOneChannelAndLeavesTheOriginalAlone() {
    val speeds = MotorSpeeds.STOPPED.with(2, Channel.C, -7)

    assertEquals(-7, speeds[2, Channel.C])
    assertEquals(0, speeds[1, Channel.C])
    assertEquals(0, speeds[2, Channel.D])
    assertEquals(0, MotorSpeeds.STOPPED[2, Channel.C])
  }

  @Test
  fun equalWhenEverySpeedMatches() {
    assertEquals(MotorSpeeds.STOPPED.with(1, Channel.A, 3), MotorSpeeds.STOPPED.with(1, Channel.A, 3))
    assertEquals(MotorSpeeds.STOPPED, MotorSpeeds.STOPPED.with(1, Channel.A, 3).with(1, Channel.A, 0))
  }

  @Test
  fun rejectsSpeedsOutsideSevenEachWay() {
    assertThrows(IllegalArgumentException::class.java) { MotorSpeeds.STOPPED.with(1, Channel.A, 8) }
    assertThrows(IllegalArgumentException::class.java) { MotorSpeeds.STOPPED.with(1, Channel.A, -8) }
  }

  @Test
  fun rejectsModulesOtherThanOneToThree() {
    assertThrows(IllegalArgumentException::class.java) { MotorSpeeds.STOPPED.with(0, Channel.A, 1) }
    assertThrows(IllegalArgumentException::class.java) { MotorSpeeds.STOPPED[4, Channel.A] }
  }
}
