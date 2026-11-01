package io.github.arcofs.mkexcavator.radio

import kotlinx.coroutines.flow.StateFlow

/** Repeats one payload over the air until it's replaced or stopped. */
interface Radio {
  val state: StateFlow<RadioState>

  /** Broadcast [payload] from now on, starting the radio if it's off. */
  fun broadcast(payload: ByteArray)

  /** Stop broadcasting. */
  fun stop()
}

sealed interface RadioState {
  data object Off : RadioState

  data object Starting : RadioState

  data object Broadcasting : RadioState

  data class Failed(val reason: String) : RadioState
}
