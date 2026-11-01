package io.github.arcofs.mkexcavator.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.arcofs.mkexcavator.control.ControlSettings
import io.github.arcofs.mkexcavator.control.ExcavatorFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
  @get:Rule val folder = TemporaryFolder()

  private val scopes = mutableListOf<CoroutineScope>()

  @After
  fun tearDown() = scopes.forEach { it.cancel() }

  // A fresh store on the same file stands in for the app being restarted.
  private fun open(): DataStoreSettingsRepository {
    val scope = CoroutineScope(Dispatchers.IO + Job()).also { scopes += it }
    val store = PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("settings.preferences_pb") }
    return DataStoreSettingsRepository(store)
  }

  @Test
  fun startsWithTheDefaults() = runBlocking {
    assertEquals(ControlSettings(), open().settings.first())
  }

  @Test
  fun savedSettingsAreThereAfterARestart() = runBlocking {
    val changed =
      ControlSettings(
        deadZone = 0.25f,
        snapDegrees = 30f,
        curve = 1.5f,
        maxSpeeds = ControlSettings.DEFAULT_MAX_SPEEDS + (ExcavatorFunction.BOOM to 5),
        reversed = setOf(ExcavatorFunction.BUCKET, ExcavatorFunction.LEFT_TRACK),
      )
    val first = open()
    first.update { changed }
    scopes.removeAt(0).coroutineContext.job.cancelAndJoin()

    assertEquals(changed, open().settings.first())
  }

  @Test
  fun updatesAreKeptInRange() = runBlocking {
    val repository = open()

    repository.update { it.copy(deadZone = 2f) }

    assertEquals(ControlSettings.DEAD_ZONE_RANGE.endInclusive, repository.settings.first().deadZone)
  }
}
