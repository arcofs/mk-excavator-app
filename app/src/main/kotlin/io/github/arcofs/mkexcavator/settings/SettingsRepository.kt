package io.github.arcofs.mkexcavator.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.arcofs.mkexcavator.control.ControlSettings
import io.github.arcofs.mkexcavator.control.ExcavatorFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The saved [ControlSettings]. Every value read or written is kept in range. */
interface SettingsRepository {
  val settings: Flow<ControlSettings>

  suspend fun update(transform: (ControlSettings) -> ControlSettings)
}

/** [SettingsRepository] in a Preferences DataStore; anything never saved reads as its default. */
class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
  override val settings: Flow<ControlSettings> = store.data.map { it.toSettings() }

  override suspend fun update(transform: (ControlSettings) -> ControlSettings) {
    store.edit { preferences -> preferences.write(transform(preferences.toSettings()).coerced()) }
  }

  private fun Preferences.toSettings() =
    ControlSettings(
      deadZone = this[DEAD_ZONE] ?: ControlSettings.DEFAULT_DEAD_ZONE,
      snapDegrees = this[SNAP_DEGREES] ?: ControlSettings.DEFAULT_SNAP_DEGREES,
      curve = this[CURVE] ?: ControlSettings.DEFAULT_CURVE,
      maxSpeeds = ExcavatorFunction.entries.associateWith { this[maxSpeedKey(it)] ?: ControlSettings.DEFAULT_MAX_SPEEDS.getValue(it) },
      reversed = this[REVERSED].orEmpty().mapNotNull { name -> ExcavatorFunction.entries.find { it.name == name } }.toSet(),
    ).coerced()

  private fun MutablePreferences.write(settings: ControlSettings) {
    this[DEAD_ZONE] = settings.deadZone
    this[SNAP_DEGREES] = settings.snapDegrees
    this[CURVE] = settings.curve
    for (function in ExcavatorFunction.entries) this[maxSpeedKey(function)] = settings.maxSpeed(function)
    this[REVERSED] = settings.reversed.map { it.name }.toSet()
  }

  private companion object {
    val DEAD_ZONE = floatPreferencesKey("dead_zone")
    val SNAP_DEGREES = floatPreferencesKey("snap_degrees")
    val CURVE = floatPreferencesKey("curve")
    val REVERSED = stringSetPreferencesKey("reversed")

    fun maxSpeedKey(function: ExcavatorFunction) = intPreferencesKey("max_speed_${function.name}")
  }
}
