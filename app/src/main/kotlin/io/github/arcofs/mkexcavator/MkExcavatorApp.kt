package io.github.arcofs.mkexcavator

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.arcofs.mkexcavator.control.MachineController
import io.github.arcofs.mkexcavator.gamepad.AndroidGamepad
import io.github.arcofs.mkexcavator.radio.BleAdvertiserRadio
import io.github.arcofs.mkexcavator.radio.Radio
import io.github.arcofs.mkexcavator.settings.DataStoreSettingsRepository
import io.github.arcofs.mkexcavator.settings.SettingsRepository
import kotlinx.coroutines.MainScope

/**
 * Owns what every screen shares: one radio and controller, so two screens never broadcast at once,
 * one settings store (DataStore allows only one per file), and the game controller.
 */
class MkExcavatorApp : Application() {
  val radio: Radio by lazy { BleAdvertiserRadio(this) }
  val machine: MachineController by lazy { MachineController(radio, MainScope()) }
  val gamepad: AndroidGamepad by lazy { AndroidGamepad(this) }
  val settings: SettingsRepository by lazy {
    DataStoreSettingsRepository(PreferenceDataStoreFactory.create { preferencesDataStoreFile("control_settings") })
  }
}
