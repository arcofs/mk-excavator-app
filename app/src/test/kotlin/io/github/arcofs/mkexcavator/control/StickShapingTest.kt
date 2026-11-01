package io.github.arcofs.mkexcavator.control

import org.junit.Assert.assertEquals
import org.junit.Test

class StickShapingTest {
  @Test
  fun pushingForwardWithSlightSidewaysDriftIsPurelyForward() {
    assertEquals(0f to 1f, StickShaping.snapToAxis(0.3f, 1f, snapDegrees = 25f))
    assertEquals(0f to -0.8f, StickShaping.snapToAxis(-0.2f, -0.8f, snapDegrees = 25f))
  }

  @Test
  fun pushingSidewaysWithSlightForwardDriftIsPurelySideways() {
    assertEquals(1f to 0f, StickShaping.snapToAxis(1f, 0.3f, snapDegrees = 25f))
    assertEquals(-0.6f to 0f, StickShaping.snapToAxis(-0.6f, 0.15f, snapDegrees = 25f))
  }

  @Test
  fun aDeliberateDiagonalKeepsBothAxes() {
    assertEquals(0.7f to 0.7f, StickShaping.snapToAxis(0.7f, 0.7f, snapDegrees = 25f))
  }

  @Test
  fun justOutsideTheSnapAngleKeepsBothAxes() {
    // About 31 degrees off straight ahead.
    assertEquals(0.6f to 1f, StickShaping.snapToAxis(0.6f, 1f, snapDegrees = 25f))
  }

  @Test
  fun aWiderSnapAngleCatchesMoreDrift() {
    assertEquals(0f to 1f, StickShaping.snapToAxis(0.6f, 1f, snapDegrees = 35f))
  }

  @Test
  fun zeroDegreesTurnsSnappingOff() {
    assertEquals(0.05f to 1f, StickShaping.snapToAxis(0.05f, 1f, snapDegrees = 0f))
  }

  @Test
  fun centredStaysCentred() {
    assertEquals(0f to 0f, StickShaping.snapToAxis(0f, 0f, snapDegrees = 25f))
  }
}
