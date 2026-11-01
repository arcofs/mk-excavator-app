package io.github.arcofs.mkexcavator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arcofs.mkexcavator.control.ControlSettings
import io.github.arcofs.mkexcavator.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
  val settings: StateFlow<ControlSettings> = repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, ControlSettings())

  fun update(transform: (ControlSettings) -> ControlSettings) {
    viewModelScope.launch { repository.update(transform) }
  }

  fun resetToDefaults() = update { ControlSettings() }
}
