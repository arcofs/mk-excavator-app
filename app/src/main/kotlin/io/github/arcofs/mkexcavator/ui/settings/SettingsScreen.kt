package io.github.arcofs.mkexcavator.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arcofs.mkexcavator.MkExcavatorApp
import io.github.arcofs.mkexcavator.R
import io.github.arcofs.mkexcavator.control.ControlSettings
import io.github.arcofs.mkexcavator.control.ExcavatorFunction
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import io.github.arcofs.mkexcavator.ui.theme.MKExcavatorTheme
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenTestBench: () -> Unit, modifier: Modifier = Modifier) {
  val app = LocalContext.current.applicationContext as MkExcavatorApp
  val viewModel: SettingsViewModel = viewModel { SettingsViewModel(app.settings) }
  val settings by viewModel.settings.collectAsStateWithLifecycle()

  SettingsScreen(
    settings = settings,
    onChange = viewModel::update,
    onReset = viewModel::resetToDefaults,
    onBack = onBack,
    onOpenTestBench = onOpenTestBench,
    modifier = modifier,
  )
}

/** Changes are saved when a slider is let go or a switch is flipped, and apply straight away. */
@Composable
internal fun SettingsScreen(
  settings: ControlSettings,
  onChange: ((ControlSettings) -> ControlSettings) -> Unit,
  onReset: () -> Unit,
  onBack: () -> Unit,
  onOpenTestBench: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = onBack) { Text("← " + stringResource(R.string.back)) }
      Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
    }

    Text(stringResource(R.string.settings_sticks), style = MaterialTheme.typography.titleMedium)
    SettingSlider(
      label = stringResource(R.string.setting_dead_zone),
      help = stringResource(R.string.setting_dead_zone_help),
      value = settings.deadZone,
      range = ControlSettings.DEAD_ZONE_RANGE,
      steps = 39,
      format = { "${(it * 100).roundToInt()}%" },
      onDone = { value -> onChange { it.copy(deadZone = value) } },
    )
    SettingSlider(
      label = stringResource(R.string.setting_snap),
      help = stringResource(R.string.setting_snap_help),
      value = settings.snapDegrees,
      range = ControlSettings.SNAP_DEGREES_RANGE,
      steps = 39,
      format = { "${it.roundToInt()}°" },
      onDone = { value -> onChange { it.copy(snapDegrees = value.roundToInt().toFloat()) } },
    )
    SettingSlider(
      label = stringResource(R.string.setting_curve),
      help = stringResource(R.string.setting_curve_help),
      value = settings.curve,
      range = ControlSettings.CURVE_RANGE,
      steps = 19,
      format = { "%.1f".format(it) },
      onDone = { value -> onChange { it.copy(curve = (value * 10).roundToInt() / 10f) } },
    )

    HorizontalDivider()
    Text(stringResource(R.string.settings_functions), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.settings_functions_help), style = MaterialTheme.typography.bodySmall)
    for (function in ExcavatorFunction.entries) {
      FunctionRow(
        name = stringResource(function.label()),
        maxSpeed = settings.maxSpeed(function),
        reversed = function in settings.reversed,
        onMaxSpeed = { speed -> onChange { it.copy(maxSpeeds = it.maxSpeeds + (function to speed)) } },
        onReversed = { reversed -> onChange { it.copy(reversed = if (reversed) it.reversed + function else it.reversed - function) } },
      )
    }

    HorizontalDivider()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 16.dp)) {
      OutlinedButton(onClick = onReset) { Text(stringResource(R.string.settings_reset)) }
      TextButton(onClick = onOpenTestBench) { Text(stringResource(R.string.test_bench)) }
    }
  }
}

/** A slider that only saves when let go, so dragging doesn't write on every pixel. */
@Composable
private fun SettingSlider(
  label: String,
  help: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  steps: Int,
  format: (Float) -> String,
  onDone: (Float) -> Unit,
) {
  var current by remember(value) { mutableFloatStateOf(value) }
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
      Text(format(current), style = MaterialTheme.typography.titleSmall)
    }
    Slider(value = current, onValueChange = { current = it }, onValueChangeFinished = { onDone(current) }, valueRange = range, steps = steps)
    Text(help, style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun FunctionRow(name: String, maxSpeed: Int, reversed: Boolean, onMaxSpeed: (Int) -> Unit, onReversed: (Boolean) -> Unit) {
  var current by remember(maxSpeed) { mutableFloatStateOf(maxSpeed.toFloat()) }
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(110.dp))
    Slider(
      value = current,
      onValueChange = { current = it },
      onValueChangeFinished = { onMaxSpeed(current.roundToInt()) },
      valueRange = 1f..MotorSpeeds.MAX_SPEED.toFloat(),
      steps = MotorSpeeds.MAX_SPEED - 2,
      modifier = Modifier.weight(1f),
    )
    Text("${current.roundToInt()}/${MotorSpeeds.MAX_SPEED}", modifier = Modifier.width(48.dp).padding(start = 8.dp))
    Spacer(Modifier.width(16.dp))
    Text(stringResource(R.string.setting_reverse), style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.width(8.dp))
    Switch(checked = reversed, onCheckedChange = onReversed)
  }
}

private fun ExcavatorFunction.label() =
  when (this) {
    ExcavatorFunction.LEFT_TRACK -> R.string.left_track
    ExcavatorFunction.RIGHT_TRACK -> R.string.right_track
    ExcavatorFunction.SLEW -> R.string.function_slew
    ExcavatorFunction.STICK -> R.string.function_stick
    ExcavatorFunction.BOOM -> R.string.function_boom
    ExcavatorFunction.BUCKET -> R.string.function_bucket
  }

@Preview(widthDp = 891, heightDp = 400, showBackground = true)
@Composable
private fun SettingsScreenPreview() {
  MKExcavatorTheme {
    SettingsScreen(settings = ControlSettings(), onChange = {}, onReset = {}, onBack = {}, onOpenTestBench = {})
  }
}
