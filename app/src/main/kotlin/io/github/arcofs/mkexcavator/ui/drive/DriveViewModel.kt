package io.github.arcofs.mkexcavator.ui.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arcofs.mkexcavator.control.ExcavatorControls
import io.github.arcofs.mkexcavator.control.ExcavatorFunction
import io.github.arcofs.mkexcavator.control.MachineController
import io.github.arcofs.mkexcavator.control.MachineController.Link
import io.github.arcofs.mkexcavator.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The drive screen: the connect guide and the six excavator functions.
 *
 * Driving stays locked until the guide is finished, because a box only takes its module-button
 * press after connecting, and driving with both boxes on module 1 runs two motors per channel.
 */
class DriveViewModel(private val machine: MachineController, settings: SettingsRepository) : ViewModel() {
  enum class GuideStep { SWITCH_ON, CONNECTING, PRESS_BUTTON }

  data class UiState(
    val link: Link = Link.DISCONNECTED,
    val guide: GuideStep? = null,
    val boxesSwapped: Boolean = false,
    val stopped: Boolean = false,
  ) {
    val canDrive: Boolean get() = link == Link.READY && guide == null && !stopped
  }

  private val controls = ExcavatorControls(machine)
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
    viewModelScope.launch { settings.settings.collect { controls.settings = it } }
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
    if (_state.value.canDrive) controls.drive(function, amount)
  }

  fun moveStick(horizontal: ExcavatorFunction, vertical: ExcavatorFunction, x: Float, y: Float) {
    if (_state.value.canDrive) controls.driveStick(horizontal, vertical, x, y)
  }

  fun release(function: ExcavatorFunction) = controls.release(function)

  /** STOP latches; the same button resumes. */
  fun onStopButton() = if (machine.state.value.stopped) machine.resume() else machine.stopAll()

  private fun setSwapped(swapped: Boolean) {
    machine.releaseAll()
    controls.boxesSwapped = swapped
    _state.update { it.copy(boxesSwapped = swapped) }
  }
}
