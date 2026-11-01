package io.github.arcofs.mkexcavator.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arcofs.mkexcavator.MkExcavatorApp
import io.github.arcofs.mkexcavator.R
import io.github.arcofs.mkexcavator.control.ControlSettings
import io.github.arcofs.mkexcavator.control.ExcavatorFunction
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import io.github.arcofs.mkexcavator.ui.components.Eyebrow
import io.github.arcofs.mkexcavator.ui.components.Panel
import io.github.arcofs.mkexcavator.ui.components.ScreenTopBar
import io.github.arcofs.mkexcavator.ui.theme.GraphiteHighest
import io.github.arcofs.mkexcavator.ui.theme.MKExcavatorTheme
import io.github.arcofs.mkexcavator.ui.theme.SafetyYellow
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenTestBench: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val app = context.applicationContext as MkExcavatorApp
  val viewModel: SettingsViewModel = viewModel { SettingsViewModel(app.settings) }
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }

  SettingsScreen(
    settings = settings,
    version = version,
    onChange = viewModel::update,
    onReset = viewModel::resetToDefaults,
    onBack = onBack,
    onOpenTestBench = onOpenTestBench,
    modifier = modifier,
  )
}

/** Changes are saved when a slider is let go, a speed bar is tapped or a switch is flipped, and apply straight away. */
@Composable
internal fun SettingsScreen(
  settings: ControlSettings,
  version: String,
  onChange: ((ControlSettings) -> ControlSettings) -> Unit,
  onReset: () -> Unit,
  onBack: () -> Unit,
  onOpenTestBench: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
    ScreenTopBar(title = stringResource(R.string.settings), onBack = onBack) {
      TextButton(onClick = onReset) {
        Icon(painterResource(R.drawable.ic_restart_alt), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.settings_reset))
      }
    }
    Row(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Section(R.drawable.ic_joystick, stringResource(R.string.settings_sticks)) {
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
        }
        TestBenchLink(onOpenTestBench)
        Text(
          stringResource(R.string.about, version),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 4.dp),
        )
      }
      Column(Modifier.weight(1.15f)) {
        Section(R.drawable.ic_speed, stringResource(R.string.settings_functions)) {
          Text(stringResource(R.string.settings_functions_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Column {
            for ((index, function) in ExcavatorFunction.entries.withIndex()) {
              if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
              FunctionRow(
                name = stringResource(function.label()),
                maxSpeed = settings.maxSpeed(function),
                reversed = function in settings.reversed,
                onMaxSpeed = { speed -> onChange { it.copy(maxSpeeds = it.maxSpeeds + (function to speed)) } },
                onReversed = { reversed -> onChange { it.copy(reversed = if (reversed) it.reversed + function else it.reversed - function) } },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Section(icon: Int, title: String, content: @Composable ColumnScope.() -> Unit) {
  Panel(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Eyebrow(title, color = MaterialTheme.colorScheme.onSurface)
      }
      content()
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
      ValueBadge(format(current))
    }
    Slider(
      value = current,
      onValueChange = { current = it },
      onValueChangeFinished = { onDone(current) },
      valueRange = range,
      steps = steps,
      // Too many steps to mark; the badge shows the value.
      colors = SliderDefaults.colors(activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent),
    )
    Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun ValueBadge(text: String) {
  Box(
    Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
  ) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = SafetyYellow)
  }
}

@Composable
private fun FunctionRow(name: String, maxSpeed: Int, reversed: Boolean, onMaxSpeed: (Int) -> Unit, onReversed: (Boolean) -> Unit) {
  Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(92.dp))
    SpeedBars(value = maxSpeed, onValueChange = onMaxSpeed, modifier = Modifier.weight(1f))
    Text(
      "$maxSpeed/${MotorSpeeds.MAX_SPEED}",
      style = MaterialTheme.typography.labelMedium,
      textAlign = TextAlign.End,
      modifier = Modifier.width(44.dp),
    )
    Spacer(Modifier.width(20.dp))
    Text(stringResource(R.string.setting_reverse), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.width(10.dp))
    Switch(checked = reversed, onCheckedChange = onReversed)
  }
}

/** Seven rising bars, one per speed: tap or slide along them to set the top speed. */
@Composable
private fun SpeedBars(value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
  var shown by remember(value) { mutableIntStateOf(value) }
  val currentOnValueChange by rememberUpdatedState(onValueChange)
  val description = stringResource(R.string.speed_of_max, shown, MotorSpeeds.MAX_SPEED)
  val max = MotorSpeeds.MAX_SPEED

  fun pick(x: Float, width: Int) {
    val speed = ((x / width) * max).toInt().plus(1).coerceIn(1, max)
    if (speed != shown) {
      shown = speed
      currentOnValueChange(speed)
    }
  }

  Canvas(
    modifier
      .height(30.dp)
      .pointerInput(Unit) { detectTapGestures { pick(it.x, size.width) } }
      .pointerInput(Unit) { detectHorizontalDragGestures(onDragStart = { pick(it.x, size.width) }) { change, _ -> pick(change.position.x, size.width) } }
      .semantics {
        contentDescription = description
        progressBarRangeInfo = ProgressBarRangeInfo(shown.toFloat(), 1f..max.toFloat(), steps = max - 2)
        setProgress { target ->
          val speed = target.roundToInt().coerceIn(1, max)
          shown = speed
          currentOnValueChange(speed)
          true
        }
      },
  ) {
    val gap = 4.dp.toPx()
    val width = (size.width - gap * (max - 1)) / max
    for (bar in 0 until max) {
      val height = size.height * (0.35f + 0.65f * bar / (max - 1))
      drawRoundRect(
        if (bar < shown) SafetyYellow else GraphiteHighest,
        topLeft = Offset(bar * (width + gap), size.height - height),
        size = Size(width, height),
        cornerRadius = CornerRadius(3.dp.toPx()),
      )
    }
  }
}

@Composable
private fun TestBenchLink(onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_build), contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(20.dp))
      }
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Text(stringResource(R.string.test_bench), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.test_bench_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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

@Preview(widthDp = 997, heightDp = 448)
@Composable
private fun SettingsScreenPreview() {
  MKExcavatorTheme {
    SettingsScreen(settings = ControlSettings(), version = "1.0", onChange = {}, onReset = {}, onBack = {}, onOpenTestBench = {})
  }
}
