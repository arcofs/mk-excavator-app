package io.github.arcofs.mkexcavator.gamepad

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import io.github.arcofs.mkexcavator.control.Gamepad
import io.github.arcofs.mkexcavator.control.GamepadButton
import io.github.arcofs.mkexcavator.control.GamepadState
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The first connected game controller (Xbox, PlayStation, 8BitDo and so on all look alike to
 * Android), fed by the activity's [onMotion] and [onKey].
 *
 * Only while [capturing] (the drive screen is showing) does it report the sticks and take the
 * controller's buttons from the system, so B doesn't also act as Back. Otherwise it reads as at
 * rest and the controller works the rest of the app as normal. Main thread only.
 */
class AndroidGamepad(context: Context) : Gamepad {
  private val inputManager = context.getSystemService(InputManager::class.java)
  private val _state = MutableStateFlow<GamepadState?>(null)
  override val state: StateFlow<GamepadState?> = _state.asStateFlow()
  private val _presses = MutableSharedFlow<GamepadButton>(extraBufferCapacity = 8)
  override val presses: SharedFlow<GamepadButton> = _presses.asSharedFlow()
  private var deviceId: Int? = null

  var capturing = false
    set(value) {
      field = value
      if (!value && _state.value != null) _state.value = GamepadState()
    }

  private val devices =
    object : InputManager.InputDeviceListener {
      override fun onInputDeviceAdded(id: Int) = findController()

      override fun onInputDeviceRemoved(id: Int) = findController()

      override fun onInputDeviceChanged(id: Int) = findController()
    }

  init {
    inputManager.registerInputDeviceListener(devices, Handler(Looper.getMainLooper()))
    findController()
  }

  /** From Activity.dispatchGenericMotionEvent: the sticks and triggers. True if it was used. */
  fun onMotion(event: MotionEvent): Boolean {
    if (!capturing || event.deviceId != deviceId || !event.isFromSource(InputDevice.SOURCE_JOYSTICK)) return false
    if (event.actionMasked != MotionEvent.ACTION_MOVE) return true
    val current = _state.value ?: return false
    _state.value =
      current.copy(
        leftX = event.axis(MotionEvent.AXIS_X),
        leftY = -event.axis(MotionEvent.AXIS_Y),
        rightX = event.axis(MotionEvent.AXIS_Z),
        rightY = -event.axis(MotionEvent.AXIS_RZ),
        // Controllers report triggers on one pair of axes or the other.
        leftTrigger = max(event.axis(MotionEvent.AXIS_LTRIGGER), event.axis(MotionEvent.AXIS_BRAKE)),
        rightTrigger = max(event.axis(MotionEvent.AXIS_RTRIGGER), event.axis(MotionEvent.AXIS_GAS)),
      )
    return true
  }

  /** From Activity.dispatchKeyEvent: bumpers, B (STOP) and Menu (resume). True if it was used. */
  fun onKey(event: KeyEvent): Boolean {
    if (!capturing || event.deviceId != deviceId) return false
    val down = event.action == KeyEvent.ACTION_DOWN
    when (event.keyCode) {
      KeyEvent.KEYCODE_BUTTON_L1 -> _state.value = _state.value?.copy(leftBumper = down)
      KeyEvent.KEYCODE_BUTTON_R1 -> _state.value = _state.value?.copy(rightBumper = down)
      KeyEvent.KEYCODE_BUTTON_B -> if (down && event.repeatCount == 0) _presses.tryEmit(GamepadButton.STOP)
      KeyEvent.KEYCODE_BUTTON_START -> if (down && event.repeatCount == 0) _presses.tryEmit(GamepadButton.RESUME)
    }
    // Swallow every button on this controller while driving, so none of them acts as Back or
    // presses whichever on-screen button has focus.
    return true
  }

  private fun findController() {
    val found = inputManager.inputDeviceIds.firstOrNull { InputDevice.getDevice(it)?.isGameController() == true }
    if (found == deviceId) return
    deviceId = found
    // A new controller starts at rest; a lost one takes its sticks with it.
    _state.value = if (found == null) null else GamepadState()
  }

  private fun InputDevice.isGameController() =
    !isVirtual && supportsSource(InputDevice.SOURCE_GAMEPAD) && supportsSource(InputDevice.SOURCE_JOYSTICK)

  /** [axis], with the controller's own centre dead band ("flat") read as exactly zero. */
  private fun MotionEvent.axis(axis: Int): Float {
    val range = device?.getMotionRange(axis, source) ?: return 0f
    val value = getAxisValue(axis)
    return if (abs(value) > range.flat) value else 0f
  }
}
