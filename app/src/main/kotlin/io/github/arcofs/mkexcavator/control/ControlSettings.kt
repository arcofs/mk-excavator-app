package io.github.arcofs.mkexcavator.control

import io.github.arcofs.mkexcavator.protocol.MotorSpeeds

/**
 * How the sticks and levers feel, adjustable on the settings screen and saved on the phone.
 *
 * @property deadZone deflection (0..1 of full travel) below which a control counts as centred.
 * @property snapDegrees within this angle of a joystick axis, the other axis is ignored; 0 = off.
 * @property curve exponent from travel to speed: 1 = linear, higher = finer at slow speeds.
 * @property maxSpeeds speed at full deflection for each function.
 * @property reversed functions whose direction is swapped.
 */
data class ControlSettings(
  val deadZone: Float = DEFAULT_DEAD_ZONE,
  val snapDegrees: Float = DEFAULT_SNAP_DEGREES,
  val curve: Float = DEFAULT_CURVE,
  val maxSpeeds: Map<ExcavatorFunction, Int> = DEFAULT_MAX_SPEEDS,
  val reversed: Set<ExcavatorFunction> = emptySet(),
) {
  fun maxSpeed(function: ExcavatorFunction): Int = maxSpeeds[function] ?: DEFAULT_MAX_SPEEDS.getValue(function)

  /** These settings with every value pulled into its allowed range and every function present. */
  fun coerced(): ControlSettings =
    copy(
      deadZone = deadZone.coerceIn(DEAD_ZONE_RANGE),
      snapDegrees = snapDegrees.coerceIn(SNAP_DEGREES_RANGE),
      curve = curve.coerceIn(CURVE_RANGE),
      maxSpeeds = ExcavatorFunction.entries.associateWith { maxSpeed(it).coerceIn(1, MotorSpeeds.MAX_SPEED) },
    )

  companion object {
    const val DEFAULT_DEAD_ZONE = 0.15f
    val DEAD_ZONE_RANGE = 0f..0.4f

    const val DEFAULT_SNAP_DEGREES = 25f
    val SNAP_DEGREES_RANGE = 0f..40f

    const val DEFAULT_CURVE = 2f
    val CURVE_RANGE = 1f..3f

    /** At full speed slew was too twitchy to place the arm. */
    const val DEFAULT_SLEW_MAX_SPEED = 4

    val DEFAULT_MAX_SPEEDS: Map<ExcavatorFunction, Int> =
      ExcavatorFunction.entries.associateWith {
        if (it == ExcavatorFunction.SLEW) DEFAULT_SLEW_MAX_SPEED else MotorSpeeds.MAX_SPEED
      }
  }
}
