package io.github.arcofs.mkexcavator.control

import io.github.arcofs.mkexcavator.protocol.Channel
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sign

/** The two battery boxes in the 13112. Which module number each is on is set by its button. */
enum class Box { ARM, TRACK }

/**
 * The excavator's six motor functions and where each is wired (docs/protocol.md, channel map).
 *
 * [sign] turns the function's positive direction into the channel's: tracks forward, stick and
 * boom up, slew right, bucket open.
 */
enum class ExcavatorFunction(val box: Box, val channel: Channel, val sign: Int) {
  LEFT_TRACK(Box.TRACK, Channel.C, 1),
  RIGHT_TRACK(Box.TRACK, Channel.A, 1),
  SLEW(Box.ARM, Channel.C, -1),
  STICK(Box.ARM, Channel.B, -1),
  BOOM(Box.ARM, Channel.D, 1),
  BUCKET(Box.ARM, Channel.A, 1),
}

/**
 * Drives [ExcavatorFunction]s by how far a control is pushed (-1..1). Each call to [drive] also
 * counts as the refresh [MachineController] needs to keep the function running.
 */
class ExcavatorControls(private val machine: MachineController) {
  /** False: arm box on module 1, track box on module 2. True: the other way round. */
  var boxesSwapped = false

  var settings = ControlSettings()

  fun drive(function: ExcavatorFunction, amount: Float) {
    val direction = if (function in settings.reversed) -function.sign else function.sign
    val speed = speedFor(amount, settings.maxSpeed(function), settings.deadZone, settings.curve) * direction
    if (speed == 0) {
      release(function)
    } else {
      machine.hold(moduleOf(function.box), function.channel, speed)
    }
  }

  /** A two-axis joystick: drift off its main axis is dropped first ([StickShaping]). */
  fun driveStick(horizontal: ExcavatorFunction, vertical: ExcavatorFunction, x: Float, y: Float) {
    val (sideways, forward) = StickShaping.snapToAxis(x, y, settings.snapDegrees)
    drive(horizontal, sideways)
    drive(vertical, forward)
  }

  fun release(function: ExcavatorFunction) = machine.release(moduleOf(function.box), function.channel)

  private fun moduleOf(box: Box): Int = if ((box == Box.ARM) != boxesSwapped) 1 else 2

  companion object {
    /**
     * Speed for a deflection of [amount] (-1..1): 0 inside [deadZone], then 1..[maxSpeed] along
     * travel^[curve]. With the default curve of 2, the first half of the travel covers only the
     * slowest quarter of the speeds.
     */
    fun speedFor(
      amount: Float,
      maxSpeed: Int = MotorSpeeds.MAX_SPEED,
      deadZone: Float = ControlSettings.DEFAULT_DEAD_ZONE,
      curve: Float = ControlSettings.DEFAULT_CURVE,
    ): Int {
      val magnitude = abs(amount).coerceAtMost(1f)
      if (magnitude == 0f || magnitude < deadZone) return 0
      val travel = (magnitude - deadZone) / (1f - deadZone)
      return (sign(amount) * ceil(travel.pow(curve) * maxSpeed).coerceAtLeast(1f)).toInt()
    }
  }
}
