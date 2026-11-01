package io.github.arcofs.mkexcavator.control

import kotlin.math.abs
import kotlin.math.tan

/** Stops a joystick's accidental drift off its main axis from moving the other function. */
object StickShaping {
  /**
   * Zero whichever axis is only drift: within [snapDegrees] of an axis the stick counts as pushed
   * straight along it. A deliberate diagonal keeps both; 0 degrees turns snapping off.
   */
  fun snapToAxis(x: Float, y: Float, snapDegrees: Float): Pair<Float, Float> {
    val ratio = tan(Math.toRadians(snapDegrees.toDouble())).toFloat()
    return when {
      abs(x) < abs(y) * ratio -> 0f to y
      abs(y) < abs(x) * ratio -> x to 0f
      else -> x to y
    }
  }
}
