package io.github.arcofs.mkexcavator.control

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [Gamepad] the test moves by hand. */
class FakeGamepad : Gamepad {
  override val state = MutableStateFlow<GamepadState?>(null)
  override val presses = MutableSharedFlow<GamepadButton>(extraBufferCapacity = 8)
}
