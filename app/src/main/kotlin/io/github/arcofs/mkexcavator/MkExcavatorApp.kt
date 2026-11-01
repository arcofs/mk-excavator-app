package io.github.arcofs.mkexcavator

import android.app.Application
import io.github.arcofs.mkexcavator.control.MachineController
import io.github.arcofs.mkexcavator.radio.BleAdvertiserRadio
import io.github.arcofs.mkexcavator.radio.Radio
import kotlinx.coroutines.MainScope

/** Owns the one radio and controller every screen shares, so two screens never broadcast at once. */
class MkExcavatorApp : Application() {
  val radio: Radio by lazy { BleAdvertiserRadio(this) }
  val machine: MachineController by lazy { MachineController(radio, MainScope()) }
}
