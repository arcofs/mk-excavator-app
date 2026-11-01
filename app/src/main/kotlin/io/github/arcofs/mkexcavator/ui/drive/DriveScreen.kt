package io.github.arcofs.mkexcavator.ui.drive

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import io.github.arcofs.mkexcavator.control.ExcavatorFunction
import io.github.arcofs.mkexcavator.control.MachineController.Link
import io.github.arcofs.mkexcavator.ui.drive.DriveViewModel.UiState
import io.github.arcofs.mkexcavator.ui.rememberWithAdvertisePermission
import io.github.arcofs.mkexcavator.ui.theme.MKExcavatorTheme

@Composable
fun DriveScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
  val app = LocalContext.current.applicationContext as MkExcavatorApp
  val viewModel: DriveViewModel = viewModel { DriveViewModel(app.machine, app.settings) }
  val state by viewModel.state.collectAsStateWithLifecycle()
  val connect = rememberWithAdvertisePermission(viewModel::connect)

  HideSystemBars()
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
  val enabled = state.canDrive
  Row(
    modifier = modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Lever(R.string.left_track, ExcavatorFunction.LEFT_TRACK, enabled, onMove, onRelease)
    // As on a real excavator, pushing a joystick forward lowers the stick or boom; pulling back raises it.
    Joystick(R.string.left_stick_label, ExcavatorFunction.SLEW, ExcavatorFunction.STICK, enabled, onMoveStick, onRelease, verticalSign = -1f)
    Column(
      modifier = Modifier.weight(0.2f).fillMaxHeight(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
      Status(state)
      StopButton(stopped = state.stopped, onClick = onStop)
      if (state.link == Link.DISCONNECTED) {
        Button(onClick = onConnect) { Text(stringResource(R.string.connect)) }
      } else {
        OutlinedButton(onClick = onDisconnect) { Text(stringResource(R.string.disconnect)) }
      }
      TextButton(onClick = onSwap, enabled = enabled) { Text(stringResource(R.string.swap_boxes)) }
      TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.settings)) }
    }
    Joystick(R.string.right_stick_label, ExcavatorFunction.BUCKET, ExcavatorFunction.BOOM, enabled, onMoveStick, onRelease, verticalSign = -1f)
    Lever(R.string.right_track, ExcavatorFunction.RIGHT_TRACK, enabled, onMove, onRelease)
  }
}

/** Red STOP; once pressed it latches and turns into a pulsing amber "STOPPED, tap to resume". */
@Composable
private fun StopButton(stopped: Boolean, onClick: () -> Unit) {
  if (stopped) {
    val pulse by
      rememberInfiniteTransition(label = "stopped")
        .animateFloat(1f, 1.08f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "stopped")
    Button(
      onClick = onClick,
      shape = CircleShape,
      colors = ButtonDefaults.buttonColors(containerColor = LATCHED_AMBER, contentColor = Color.Black),
      contentPadding = PaddingValues(4.dp),
      modifier =
        Modifier.size(STOP_SIZE)
          .graphicsLayer {
            scaleX = pulse
            scaleY = pulse
          }
          .border(4.dp, MaterialTheme.colorScheme.error, CircleShape),
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.stop_latched), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.stop_resume_hint), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
      }
    }
  } else {
    Button(
      onClick = onClick,
      shape = CircleShape,
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
      contentPadding = PaddingValues(4.dp),
      modifier = Modifier.size(STOP_SIZE),
    ) {
      Text(stringResource(R.string.stop), style = MaterialTheme.typography.titleLarge)
    }
  }
}

private val STOP_SIZE = 112.dp
private val LATCHED_AMBER = Color(0xFFFFB300)

@Composable
private fun ColumnScope.Status(state: UiState) {
  val status =
    when {
      state.link == Link.DISCONNECTED -> R.string.status_disconnected
      state.link == Link.CONNECTING -> R.string.status_connecting
      state.guide != null -> R.string.status_setting_up
      state.stopped -> R.string.status_stopped
      else -> R.string.status_ready
    }
  Text(stringResource(status), style = MaterialTheme.typography.titleMedium)
  if (state.canDrive) {
    Text(
      stringResource(if (state.boxesSwapped) R.string.modules_swapped else R.string.modules_normal),
      style = MaterialTheme.typography.bodySmall,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun RowScope.Lever(
  label: Int,
  function: ExcavatorFunction,
  enabled: Boolean,
  onMove: (ExcavatorFunction, Float) -> Unit,
  onRelease: (ExcavatorFunction) -> Unit,
) {
  Column(Modifier.weight(0.12f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    ControlStick(
      twoAxes = false,
      enabled = enabled,
      onMove = { _, y -> onMove(function, y) },
      onRelease = { onRelease(function) },
      modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
    )
  }
}

@Composable
private fun RowScope.Joystick(
  label: Int,
  horizontal: ExcavatorFunction,
  vertical: ExcavatorFunction,
  enabled: Boolean,
  onMoveStick: (horizontal: ExcavatorFunction, vertical: ExcavatorFunction, x: Float, y: Float) -> Unit,
  onRelease: (ExcavatorFunction) -> Unit,
  verticalSign: Float = 1f,
) {
  Column(Modifier.weight(0.28f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
      ControlStick(
        twoAxes = true,
        enabled = enabled,
        onMove = { x, y -> onMoveStick(horizontal, vertical, x, y * verticalSign) },
        onRelease = {
          onRelease(horizontal)
          onRelease(vertical)
        },
        modifier = Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true),
      )
    }
  }
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

@Preview(widthDp = 891, heightDp = 400, showBackground = true)
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
