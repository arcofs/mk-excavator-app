package io.github.arcofs.mkexcavator.settings

import io.github.arcofs.mkexcavator.control.ControlSettings
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(initial: ControlSettings = ControlSettings()) : SettingsRepository {
  override val settings = MutableStateFlow(initial)

  override suspend fun update(transform: (ControlSettings) -> ControlSettings) {
    settings.value = transform(settings.value).coerced()
  }
}
