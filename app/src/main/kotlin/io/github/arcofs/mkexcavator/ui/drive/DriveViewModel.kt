package io.github.arcofs.mkexcavator.ui.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arcofs.mkexcavator.control.ControlSettings
import io.github.arcofs.mkexcavator.control.ExcavatorControls
import io.github.arcofs.mkexcavator.control.ExcavatorFunction
import io.github.arcofs.mkexcavator.control.Gamepad
import io.github.arcofs.mkexcavator.control.GamepadButton
import io.github.arcofs.mkexcavator.control.GamepadDriving
import io.github.arcofs.mkexcavator.control.GamepadState
import io.github.arcofs.mkexcavator.control.MachineController
import io.github.arcofs.mkexcavator.control.MachineController.Link
import io.github.arcofs.mkexcavator.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The drive screen: the connect guide, the six excavator functions, and a game controller if one
 * is connected.
 *
 * Driving stays locked until the guide is finished, because a box only takes its module-button
 * press after connecting, and driving with both boxes on module 1 runs two motors per channel.
 * While a controller is connected the on-screen sticks and levers are locked; STOP still works.
 */
class DriveViewModel(
  private val machine: MachineController,
  settings: SettingsRepository,
  gamepad: Gamepad? = null,
  // The usual viewModelScope; tests pass their own so the controller's refresh loop ends with them.
  scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : ViewModel(scope) {
  enum class GuideStep { SWITCH_ON, CONNECTING, PRESS_BUTTON }

  data class UiState(
    val link: Link = Link.DISCONNECTED,
    val guide: GuideStep? = null,
    val boxesSwapped: Boolean = false,
    val stopped: Boolean = false,
    val settings: ControlSettings = ControlSettings(),
    /** The connected game controller's sticks and triggers; null when there isn't one. */
    val controller: GamepadState? = null,
  ) {
    val canDrive: Boolean get() = link == Link.READY && guide == null && !stopped

    val canTouch: Boolean get() = canDrive && controller == null
  }

  private val controls = ExcavatorControls(machine)
  private val gamepadDriving = GamepadDriving(controls)
  private val _state = MutableStateFlow(UiState())
  val state: StateFlow<UiState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      machine.state.collect { machineState ->
        _state.update {
          val guide =
            when {
              machineState.link == Link.READY && it.guide == GuideStep.CONNECTING -> GuideStep.PRESS_BUTTON
              machineState.link == Link.DISCONNECTED && it.guide != GuideStep.SWITCH_ON -> null
              else -> it.guide
            }
          it.copy(link = machineState.link, guide = guide, stopped = machineState.stopped)
        }
      }
    }
    viewModelScope.launch {
      settings.settings.collect { saved ->
        controls.settings = saved
        _state.update { it.copy(settings = saved) }
      }
    }
    if (gamepad != null) {
      viewModelScope.launch { gamepad.state.collectLatest(::follow) }
      viewModelScope.launch { gamepad.presses.collect(::onPress) }
    }
  }

  fun openGuide() = _state.update { it.copy(guide = GuideStep.SWITCH_ON) }

  fun connect() {
    _state.update { it.copy(guide = GuideStep.CONNECTING) }
    machine.connect()
  }

  /** [armBoxPressed]: the arm box ended up on module 2 instead of the track box. */
  fun finishGuide(armBoxPressed: Boolean) {
    setSwapped(armBoxPressed)
    _state.update { it.copy(guide = null) }
  }

  fun cancelGuide() {
    val wasConnecting = _state.value.guide != GuideStep.SWITCH_ON
    _state.update { it.copy(guide = null) }
    if (wasConnecting) machine.disconnect()
  }

  fun disconnect() = machine.disconnect()

  fun toggleSwap() = setSwapped(!_state.value.boxesSwapped)

  fun move(function: ExcavatorFunction, amount: Float) {
    if (_state.value.canTouch) controls.drive(function, amount)
  }

  fun moveStick(horizontal: ExcavatorFunction, vertical: ExcavatorFunction, x: Float, y: Float) {
    if (_state.value.canTouch) controls.driveStick(horizontal, vertical, x, y)
  }

  fun release(function: ExcavatorFunction) = controls.release(function)

  /** STOP latches; the same button resumes. */
  fun onStopButton() = if (machine.state.value.stopped) machine.resume() else machine.stopAll()

  /**
   * Drive from the controller's latest reading. It only reports changes, so a held position is
   * sent again every [GAMEPAD_REFRESH_MS] to stay inside the controller's watchdog; a new reading
   * cancels the loop. A controller that disconnects leaves everything at rest.
   */
  private suspend fun follow(pad: GamepadState?) {
    val wasConnected = _state.value.controller != null
    _state.update { it.copy(controller = pad) }
    if (pad == null) {
      if (wasConnected) gamepadDriving.releaseAll()
      return
    }
    while (true) {
      gamepadDriving.apply(pad, canMove = _state.value.canDrive)
      if (pad.atRest) return
      delay(GAMEPAD_REFRESH_MS)
    }
  }

  /** The controller's STOP only stops, and a different button resumes, so a double press can't undo it. */
  private fun onPress(button: GamepadButton) =
    when (button) {
      GamepadButton.STOP -> machine.stopAll()
      GamepadButton.RESUME -> machine.resume()
    }

  private fun setSwapped(swapped: Boolean) {
    machine.releaseAll()
    controls.boxesSwapped = swapped
    _state.update { it.copy(boxesSwapped = swapped) }
  }

  companion object {
    /** Like the on-screen sticks: comfortably inside MachineController.REFRESH_TIMEOUT_MS. */
    const val GAMEPAD_REFRESH_MS = 100L
  }
}
