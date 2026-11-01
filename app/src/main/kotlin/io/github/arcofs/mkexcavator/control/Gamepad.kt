package io.github.arcofs.mkexcavator.control

import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * One reading of a game controller: sticks -1..1 with right and up positive, triggers 0..1 from
 * released to fully pulled, and whether each bumper is held.
 */
data class GamepadState(
  val leftX: Float = 0f,
  val leftY: Float = 0f,
  val rightX: Float = 0f,
  val rightY: Float = 0f,
  val leftTrigger: Float = 0f,
  val rightTrigger: Float = 0f,
  val leftBumper: Boolean = false,
  val rightBumper: Boolean = false,
) {
  /** Each track runs forward on its trigger, or in reverse while that side's bumper is held. */
  val leftTrack: Float get() = if (leftBumper) -leftTrigger else leftTrigger
  val rightTrack: Float get() = if (rightBumper) -rightTrigger else rightTrigger

  val atRest: Boolean get() = leftX == 0f && leftY == 0f && rightX == 0f && rightY == 0f && leftTrigger == 0f && rightTrigger == 0f
}

/** Controller buttons that act once when pressed, rather than while held. */
enum class GamepadButton {
  STOP,
  RESUME,
}

/** A game controller connected to the phone. */
interface Gamepad {
  /** Its sticks, triggers and bumpers; null while no controller is connected. */
  val state: StateFlow<GamepadState?>

  val presses: Flow<GamepadButton>
}

/**
 * Drives the excavator from a game controller laid out like the drive screen: the left stick
 * slews and moves the stick, the right stick tilts the bucket and moves the boom (pushing either
 * forward lowers, as on screen), and each trigger runs its track ([GamepadState.leftTrack]).
 * The settings apply exactly as they do to the on-screen controls.
 */
class GamepadDriving(private val controls: ExcavatorControls) {
  // Functions whose control was pushed while nothing could move. They wait until it's let go, so
  // nothing starts by itself when driving becomes possible (guide finished, or STOP resumed).
  private val waitingForRest = mutableSetOf<ExcavatorFunction>()

  /**
   * Drive from [pad]. With [canMove] false (stopped, or not ready to drive) nothing starts, but a
   * function whose control is back at rest is still released: that is how a control held through
   * a STOP counts as let go, so it works again after resume.
   */
  fun apply(pad: GamepadState, canMove: Boolean) {
    stick(ExcavatorFunction.SLEW, ExcavatorFunction.STICK, pad.leftX, -pad.leftY, canMove)
    stick(ExcavatorFunction.BUCKET, ExcavatorFunction.BOOM, pad.rightX, -pad.rightY, canMove)
    move(ExcavatorFunction.LEFT_TRACK, pad.leftTrack, canMove)
    move(ExcavatorFunction.RIGHT_TRACK, pad.rightTrack, canMove)
  }

  fun releaseAll() {
    waitingForRest.clear()
    ExcavatorFunction.entries.forEach(controls::release)
  }

  private fun stick(horizontal: ExcavatorFunction, vertical: ExcavatorFunction, x: Float, y: Float, canMove: Boolean) {
    val (sideways, forward) = StickShaping.snapToAxis(x, y, controls.settings.snapDegrees)
    move(horizontal, sideways, canMove)
    move(vertical, forward, canMove)
  }

  private fun move(function: ExcavatorFunction, amount: Float, canMove: Boolean) {
    val atRest = amount == 0f || abs(amount) < controls.settings.deadZone
    when {
      atRest -> {
        waitingForRest -= function
        controls.drive(function, amount)
      }
      !canMove -> waitingForRest += function
      function !in waitingForRest -> controls.drive(function, amount)
    }
  }
}
