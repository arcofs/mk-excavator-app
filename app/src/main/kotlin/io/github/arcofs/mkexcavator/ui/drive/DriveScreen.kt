package io.github.arcofs.mkexcavator.ui.drive

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arcofs.mkexcavator.MkExcavatorApp
import io.github.arcofs.mkexcavator.R
import io.github.arcofs.mkexcavator.control.ControlSettings
import io.github.arcofs.mkexcavator.control.ExcavatorFunction
import io.github.arcofs.mkexcavator.control.MachineController.Link
import io.github.arcofs.mkexcavator.ui.components.Eyebrow
import io.github.arcofs.mkexcavator.ui.components.HazardTape
import io.github.arcofs.mkexcavator.ui.components.Panel
import io.github.arcofs.mkexcavator.ui.components.StatusPill
import io.github.arcofs.mkexcavator.ui.components.StopButton
import io.github.arcofs.mkexcavator.ui.drive.DriveViewModel.UiState
import io.github.arcofs.mkexcavator.ui.rememberWithAdvertisePermission
import io.github.arcofs.mkexcavator.ui.theme.GoGreen
import io.github.arcofs.mkexcavator.ui.theme.Graphite
import io.github.arcofs.mkexcavator.ui.theme.GraphiteRaised
import io.github.arcofs.mkexcavator.ui.theme.MKExcavatorTheme
import io.github.arcofs.mkexcavator.ui.theme.SafetyYellow
import io.github.arcofs.mkexcavator.ui.theme.SignalRedLight
import io.github.arcofs.mkexcavator.ui.theme.Steel

@Composable
fun DriveScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
  val app = LocalContext.current.applicationContext as MkExcavatorApp
  val viewModel: DriveViewModel = viewModel { DriveViewModel(app.machine, app.settings, app.gamepad) }
  val state by viewModel.state.collectAsStateWithLifecycle()
  val connect = rememberWithAdvertisePermission(viewModel::connect)

  HideSystemBars()
  // A game controller only drives while this screen is showing.
  DisposableEffect(app.gamepad) {
    app.gamepad.capturing = true
    onDispose { app.gamepad.capturing = false }
  }
  DriveScreen(
    state = state,
    onConnect = viewModel::openGuide,
    onDisconnect = viewModel::disconnect,
    onMove = viewModel::move,
    onMoveStick = viewModel::moveStick,
    onRelease = viewModel::release,
    onStop = viewModel::onStopButton,
    onSwap = viewModel::toggleSwap,
    onOpenSettings = onOpenSettings,
    modifier = modifier,
  )
  state.guide?.let { step ->
    ConnectGuide(
      step = step,
      onConnect = connect,
      onDone = { viewModel.finishGuide(armBoxPressed = false) },
      onArmBoxPressed = { viewModel.finishGuide(armBoxPressed = true) },
      onCancel = viewModel::cancelGuide,
    )
  }
}

/**
 * Real-excavator ("ISO") layout: a lever per track at the edges; left stick slews and moves the
 * stick, right stick tilts the bucket and moves the boom. Any two controls work at once.
 */
@Composable
internal fun DriveScreen(
  state: UiState,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  onMove: (ExcavatorFunction, Float) -> Unit,
  onMoveStick: (horizontal: ExcavatorFunction, vertical: ExcavatorFunction, x: Float, y: Float) -> Unit,
  onRelease: (ExcavatorFunction) -> Unit,
  onStop: () -> Unit,
  onSwap: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Touch is locked while a game controller is connected; the sticks then show what it's doing.
  val touch = Touch(enabled = state.canTouch, dimmed = !state.canDrive)
  val pad = state.controller
  Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GraphiteRaised, Graphite)))) {
    Row(
      modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Lever(R.string.left_track, ExcavatorFunction.LEFT_TRACK, state.settings, touch, pad?.leftTrack ?: 0f, onMove, onRelease)
      // As on a real excavator, pushing a joystick forward lowers the stick or boom; pulling back raises it.
      Joystick(
        R.string.left_stick_title,
        ExcavatorFunction.SLEW,
        ExcavatorFunction.STICK,
        state.settings,
        touch,
        Offset(pad?.leftX ?: 0f, pad?.leftY ?: 0f),
        onMoveStick,
        onRelease,
        verticalSign = -1f,
      )
      Console(state, onConnect, onDisconnect, onStop, onSwap, onOpenSettings, Modifier.weight(0.2f).fillMaxHeight())
      Joystick(
        R.string.right_stick_title,
        ExcavatorFunction.BUCKET,
        ExcavatorFunction.BOOM,
        state.settings,
        touch,
        Offset(pad?.rightX ?: 0f, pad?.rightY ?: 0f),
        onMoveStick,
        onRelease,
        verticalSign = -1f,
      )
      Lever(R.string.right_track, ExcavatorFunction.RIGHT_TRACK, state.settings, touch, pad?.rightTrack ?: 0f, onMove, onRelease)
    }
    // Hazard tape along the top and bottom while STOP is latched.
    if (state.stopped) {
      HazardTape(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(8.dp))
      HazardTape(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(8.dp))
    }
  }
}

/** The middle of the screen: name and settings, connection status, STOP, connect, and which box is which. */
@Composable
private fun Console(
  state: UiState,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  onStop: () -> Unit,
  onSwap: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Icon(painterResource(R.drawable.ic_excavator), contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(28.dp, 20.dp))
      Spacer(Modifier.width(8.dp))
      Eyebrow(stringResource(R.string.app_name), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
      FilledTonalIconButton(onClick = onOpenSettings) {
        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Status(state)
      if (state.controller != null) ControllerChip(stopped = state.stopped)
    }
    StopButton(stopped = state.stopped, onClick = onStop)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
      if (state.link == Link.DISCONNECTED) {
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(48.dp)) {
          Icon(painterResource(R.drawable.ic_bluetooth), contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.connect))
        }
      } else {
        OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().height(48.dp)) {
          Icon(painterResource(R.drawable.ic_bluetooth_disabled), contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.disconnect))
        }
      }
      ModuleChip(swapped = state.boxesSwapped, enabled = state.canDrive, onSwap = onSwap)
    }
  }
}

@Composable
private fun Status(state: UiState) {
  when {
    state.link == Link.DISCONNECTED -> StatusPill(stringResource(R.string.status_disconnected), Steel)
    state.link == Link.CONNECTING -> StatusPill(stringResource(R.string.status_connecting), SafetyYellow, pulsing = true)
    state.guide != null -> StatusPill(stringResource(R.string.status_setting_up), SafetyYellow, pulsing = true)
    state.stopped -> StatusPill(stringResource(R.string.status_stopped), SignalRedLight, pulsing = true)
    else -> StatusPill(stringResource(R.string.status_ready), GoGreen)
  }
}

/** Shown while a game controller is connected; when stopped, it says how to resume from the controller. */
@Composable
private fun ControllerChip(stopped: Boolean) {
  Row(
    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(painterResource(R.drawable.ic_gamepad), contentDescription = null, tint = SafetyYellow, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Eyebrow(
      stringResource(if (stopped) R.string.controller_resume_hint else R.string.controller_connected),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

/** Which module each box is on; tap to swap them if the guide's button press went to the wrong box. */
@Composable
private fun ModuleChip(swapped: Boolean, enabled: Boolean, onSwap: () -> Unit) {
  val content = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
  Surface(
    onClick = onSwap,
    enabled = enabled,
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = content,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(
        painterResource(R.drawable.ic_swap_horiz),
        contentDescription = stringResource(R.string.swap_boxes),
        tint = if (enabled) SafetyYellow else LocalContentColor.current,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
      Eyebrow(stringResource(if (swapped) R.string.modules_swapped else R.string.modules_normal), color = LocalContentColor.current)
    }
  }
}

/** Whether the on-screen controls take touch, and whether they're greyed out. */
private data class Touch(val enabled: Boolean, val dimmed: Boolean)

@Composable
private fun RowScope.Lever(
  label: Int,
  function: ExcavatorFunction,
  settings: ControlSettings,
  touch: Touch,
  mirror: Float,
  onMove: (ExcavatorFunction, Float) -> Unit,
  onRelease: (ExcavatorFunction) -> Unit,
) {
  Panel(Modifier.weight(0.11f).fillMaxHeight()) {
    Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Eyebrow(stringResource(label), textAlign = TextAlign.Center)
      ControlStick(
        twoAxes = false,
        enabled = touch.enabled,
        dimmed = touch.dimmed,
        mirror = Offset(0f, mirror),
        onMove = { _, y -> onMove(function, y) },
        onRelease = { onRelease(function) },
        labels = AxisLabels(up = directionLabel(function, positive = true, settings), down = directionLabel(function, positive = false, settings)),
        deadZone = settings.deadZone,
        modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp),
      )
    }
  }
}

@Composable
private fun RowScope.Joystick(
  title: Int,
  horizontal: ExcavatorFunction,
  vertical: ExcavatorFunction,
  settings: ControlSettings,
  touch: Touch,
  mirror: Offset,
  onMoveStick: (horizontal: ExcavatorFunction, vertical: ExcavatorFunction, x: Float, y: Float) -> Unit,
  onRelease: (ExcavatorFunction) -> Unit,
  verticalSign: Float = 1f,
) {
  Panel(Modifier.weight(0.29f).fillMaxHeight()) {
    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Eyebrow(stringResource(title))
      Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        ControlStick(
          twoAxes = true,
          enabled = touch.enabled,
          dimmed = touch.dimmed,
          mirror = mirror,
          onMove = { x, y -> onMoveStick(horizontal, vertical, x, y * verticalSign) },
          onRelease = {
            onRelease(horizontal)
            onRelease(vertical)
          },
          labels =
            AxisLabels(
              up = directionLabel(vertical, positive = verticalSign > 0, settings),
              down = directionLabel(vertical, positive = verticalSign < 0, settings),
              left = directionLabel(horizontal, positive = false, settings),
              right = directionLabel(horizontal, positive = true, settings),
            ),
          deadZone = settings.deadZone,
          snapDegrees = settings.snapDegrees,
          modifier = Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true),
        )
      }
    }
  }
}

/**
 * What driving [function] one way does: [positive] is forward, up, slew right or bucket open
 * (see [ExcavatorFunction]), unless Reverse is on for it in the settings.
 */
@Composable
private fun directionLabel(function: ExcavatorFunction, positive: Boolean, settings: ControlSettings): String {
  val forward = positive != (function in settings.reversed)
  return stringResource(
    when (function) {
      ExcavatorFunction.LEFT_TRACK,
      ExcavatorFunction.RIGHT_TRACK -> if (forward) R.string.direction_forward else R.string.direction_reverse
      ExcavatorFunction.SLEW -> if (forward) R.string.direction_right else R.string.direction_left
      ExcavatorFunction.STICK,
      ExcavatorFunction.BOOM -> if (forward) R.string.direction_up else R.string.direction_down
      ExcavatorFunction.BUCKET -> if (forward) R.string.direction_dump else R.string.direction_curl
    }
  )
}

/** Full screen while driving; the bars come back with a swipe from the edge. */
@Composable
private fun HideSystemBars() {
  val activity = LocalActivity.current ?: return
  DisposableEffect(activity) {
    val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    bars.hide(WindowInsetsCompat.Type.systemBars())
    onDispose { bars.show(WindowInsetsCompat.Type.systemBars()) }
  }
}

@Preview(widthDp = 997, heightDp = 448)
@Composable
private fun DriveScreenPreview() {
  MKExcavatorTheme {
    DriveScreen(
      state = UiState(link = Link.READY),
      onConnect = {},
      onDisconnect = {},
      onMove = { _, _ -> },
      onMoveStick = { _, _, _, _ -> },
      onRelease = {},
      onStop = {},
      onSwap = {},
      onOpenSettings = {},
    )
  }
}

@Preview(widthDp = 997, heightDp = 448)
@Composable
private fun DriveScreenStoppedPreview() {
  MKExcavatorTheme {
    DriveScreen(
      state = UiState(link = Link.READY, stopped = true),
      onConnect = {},
      onDisconnect = {},
      onMove = { _, _ -> },
      onMoveStick = { _, _, _, _ -> },
      onRelease = {},
      onStop = {},
      onSwap = {},
      onOpenSettings = {},
    )
  }
}
