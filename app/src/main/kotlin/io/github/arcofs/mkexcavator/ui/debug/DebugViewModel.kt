package io.github.arcofs.mkexcavator.ui.debug

import androidx.lifecycle.ViewModel
import io.github.arcofs.mkexcavator.control.MachineController
import io.github.arcofs.mkexcavator.protocol.Channel
import io.github.arcofs.mkexcavator.radio.RadioState
import kotlinx.coroutines.flow.StateFlow

class DebugViewModel(private val controller: MachineController, radioState: StateFlow<RadioState>) : ViewModel() {
  val machine: StateFlow<MachineController.State> = controller.state
  val radioState: StateFlow<RadioState> = radioState

  fun connect() = controller.connect()

  fun disconnect() = controller.disconnect()

  fun hold(drives: List<Drive>, speed: Int) = drives.forEach { controller.hold(it.module, it.channel, it.direction * speed) }

  fun release(drives: List<Drive>) = drives.forEach { controller.release(it.module, it.channel) }

  fun stopAll() = controller.stopAll()
}

/** One motor to run while a button is held: [direction] is +1 or -1, scaled by the chosen speed. */
data class Drive(val module: Int, val channel: Channel, val direction: Int)
