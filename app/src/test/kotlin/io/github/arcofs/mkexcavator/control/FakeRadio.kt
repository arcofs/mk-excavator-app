package io.github.arcofs.mkexcavator.control

import io.github.arcofs.mkexcavator.radio.Radio
import io.github.arcofs.mkexcavator.radio.RadioState
import kotlinx.coroutines.flow.MutableStateFlow

/** Records what would have gone on air. */
class FakeRadio : Radio {
  override val state = MutableStateFlow<RadioState>(RadioState.Off)
  val sent = mutableListOf<List<Byte>>()
  var stopped = 0

  val last: List<Byte> get() = sent.last()

  override fun broadcast(payload: ByteArray) {
    sent += payload.toList()
    state.value = RadioState.Broadcasting
  }

  override fun stop() {
    stopped++
    state.value = RadioState.Off
  }
}
