package io.github.arcofs.mkexcavator.ui.debug

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arcofs.mkexcavator.MkExcavatorApp
import io.github.arcofs.mkexcavator.control.MachineController
import io.github.arcofs.mkexcavator.control.MachineController.Link
import io.github.arcofs.mkexcavator.protocol.Channel
import io.github.arcofs.mkexcavator.protocol.MotorSpeeds
import io.github.arcofs.mkexcavator.radio.RadioState
import io.github.arcofs.mkexcavator.ui.components.Eyebrow
import io.github.arcofs.mkexcavator.ui.components.Panel
import io.github.arcofs.mkexcavator.ui.components.ScreenTopBar
import io.github.arcofs.mkexcavator.ui.components.StatusPill
import io.github.arcofs.mkexcavator.ui.components.StopButton
import io.github.arcofs.mkexcavator.ui.rememberWithAdvertisePermission
import io.github.arcofs.mkexcavator.ui.theme.GoGreen
import io.github.arcofs.mkexcavator.ui.theme.MKExcavatorTheme
import io.github.arcofs.mkexcavator.ui.theme.MkText
import io.github.arcofs.mkexcavator.ui.theme.SafetyYellow
import io.github.arcofs.mkexcavator.ui.theme.SignalRedLight
import io.github.arcofs.mkexcavator.ui.theme.Steel
import kotlinx.coroutines.delay

private const val ARM_BOX = 1
private const val TRACK_BOX = 2

// Comfortably inside MachineController.REFRESH_TIMEOUT_MS.
private const val HOLD_REFRESH_MS = 100L

private class TestControl(val label: String, val drives: List<Drive>)

private val ARM_CONTROLS =
  listOf(
    TestControl("Bucket (A)", listOf(Drive(ARM_BOX, Channel.A, 1))),
    TestControl("Stick (B)", listOf(Drive(ARM_BOX, Channel.B, 1))),
    TestControl("Slew (C)", listOf(Drive(ARM_BOX, Channel.C, 1))),
    TestControl("Boom (D)", listOf(Drive(ARM_BOX, Channel.D, 1))),
  )

private val TRACK_CONTROLS =
  listOf(
    TestControl("Right track (A)", listOf(Drive(TRACK_BOX, Channel.A, 1))),
    TestControl("Left track (C)", listOf(Drive(TRACK_BOX, Channel.C, 1))),
  )

private val BOTH_BOX_CONTROLS =
  listOf(
    TestControl("Bucket + right track", listOf(Drive(ARM_BOX, Channel.A, 1), Drive(TRACK_BOX, Channel.A, 1))),
    TestControl("Both tracks", listOf(Drive(TRACK_BOX, Channel.A, 1), Drive(TRACK_BOX, Channel.C, 1))),
  )

@Composable
fun DebugScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val app = LocalContext.current.applicationContext as MkExcavatorApp
  val viewModel: DebugViewModel = viewModel { DebugViewModel(app.machine, app.radio.state) }
  val machine by viewModel.machine.collectAsStateWithLifecycle()
  val radio by viewModel.radioState.collectAsStateWithLifecycle()
  var speed by rememberSaveable { mutableIntStateOf(4) }
  val connect = rememberWithAdvertisePermission(viewModel::connect)

  DebugScreen(
    machine = machine,
    radio = radio,
    speed = speed,
    onSpeedChange = { speed = it },
    onConnect = connect,
    onDisconnect = viewModel::disconnect,
    onHold = { drives -> viewModel.hold(drives, speed) },
    onRelease = viewModel::release,
    onStop = viewModel::onStopButton,
    onBack = onBack,
    modifier = modifier,
  )
}

/** Hold-to-run buttons for each motor, for checking the wiring. STOP stays in view on the left. */
@Composable
internal fun DebugScreen(
  machine: MachineController.State,
  radio: RadioState,
  speed: Int,
  onSpeedChange: (Int) -> Unit,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  onHold: (List<Drive>) -> Unit,
  onRelease: (List<Drive>) -> Unit,
  onStop: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val connected = machine.link != Link.DISCONNECTED
  Column(modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
    ScreenTopBar(title = "Test bench", onBack = onBack)
    Row(Modifier.weight(1f).padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Panel(Modifier.fillMaxWidth()) {
          Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                "Switch on both boxes, tap Connect, then short-press the track box's button once (two flashes = Module 2). " +
                  "Hold a button to run a motor; let go to stop.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("Link: ${machine.link.label()}", machine.link.colour())
                StatusPill("Radio: ${radio.label()}", radio.colour())
              }
              Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onConnect, enabled = !connected, modifier = Modifier.weight(1f)) { Text("Connect") }
                OutlinedButton(onClick = onDisconnect, enabled = connected, modifier = Modifier.weight(1f)) { Text("Disconnect") }
              }
            }
            Spacer(Modifier.width(20.dp))
            StopButton(stopped = machine.stopped, onClick = onStop, size = 124.dp)
          }
        }
        Panel(Modifier.fillMaxWidth()) {
          Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Eyebrow("Speed", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
              Text("$speed of ${MotorSpeeds.MAX_SPEED}", style = MaterialTheme.typography.labelMedium, color = SafetyYellow)
            }
            Slider(
              value = speed.toFloat(),
              onValueChange = { onSpeedChange(it.toInt()) },
              valueRange = 1f..MotorSpeeds.MAX_SPEED.toFloat(),
              steps = MotorSpeeds.MAX_SPEED - 2,
            )
            Eyebrow("Broadcasting", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
            for (module in 1..2) ChannelReadout(module, machine.speeds)
          }
        }
      }
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ControlGroup("Arm box (Module 1)", ARM_CONTROLS, connected, onHold, onRelease)
        ControlGroup("Track box (Module 2)", TRACK_CONTROLS, connected, onHold, onRelease)
        ControlGroup("Both boxes at once", BOTH_BOX_CONTROLS, connected, onHold, onRelease)
      }
    }
  }
}

/** Each channel's speed being sent to [module], lit up when it isn't zero. */
@Composable
private fun ChannelReadout(module: Int, speeds: MotorSpeeds) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text("Module $module", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
    for (channel in Channel.entries) {
      val speed = speeds[module, channel]
      Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        Text(channel.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text("%+d".format(speed), style = MkText.readout, color = if (speed != 0) SafetyYellow else MaterialTheme.colorScheme.onSurface)
      }
    }
  }
}

@Composable
private fun ControlGroup(
  title: String,
  controls: List<TestControl>,
  enabled: Boolean,
  onHold: (List<Drive>) -> Unit,
  onRelease: (List<Drive>) -> Unit,
) {
  Panel(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Eyebrow(title, color = MaterialTheme.colorScheme.onSurface)
      for (control in controls) {
        val reverse = control.drives.map { it.copy(direction = -it.direction) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(control.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
          HoldButton("−", enabled, onHold = { onHold(reverse) }, onRelease = { onRelease(reverse) })
          HoldButton("+", enabled, onHold = { onHold(control.drives) }, onRelease = { onRelease(control.drives) })
        }
      }
    }
  }
}

/** Calls [onHold] every [HOLD_REFRESH_MS] while pressed, then [onRelease] once let go. */
@Composable
private fun HoldButton(text: String, enabled: Boolean, onHold: () -> Unit, onRelease: () -> Unit) {
  val interactions = remember { MutableInteractionSource() }
  val pressed by interactions.collectIsPressedAsState()
  val currentOnHold by rememberUpdatedState(onHold)
  val currentOnRelease by rememberUpdatedState(onRelease)

  LaunchedEffect(pressed, enabled) {
    if (!pressed || !enabled) return@LaunchedEffect
    try {
      while (true) {
        currentOnHold()
        delay(HOLD_REFRESH_MS)
      }
    } finally {
      currentOnRelease()
    }
  }

  FilledTonalButton(
    onClick = {},
    enabled = enabled,
    interactionSource = interactions,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.size(width = 64.dp, height = 44.dp),
  ) {
    Text(text, style = MaterialTheme.typography.titleLarge)
  }
}

private fun Link.label() =
  when (this) {
    Link.DISCONNECTED -> "off"
    Link.CONNECTING -> "connecting"
    Link.READY -> "ready"
  }

private fun Link.colour() =
  when (this) {
    Link.DISCONNECTED -> Steel
    Link.CONNECTING -> SafetyYellow
    Link.READY -> GoGreen
  }

private fun RadioState.label() =
  when (this) {
    RadioState.Off -> "off"
    RadioState.Starting -> "starting"
    RadioState.Broadcasting -> "on air"
    is RadioState.Failed -> reason
  }

private fun RadioState.colour() =
  when (this) {
    RadioState.Off -> Steel
    RadioState.Starting -> SafetyYellow
    RadioState.Broadcasting -> GoGreen
    is RadioState.Failed -> SignalRedLight
  }

@Preview(widthDp = 997, heightDp = 448)
@Composable
private fun DebugScreenPreview() {
  MKExcavatorTheme {
    DebugScreen(
      machine = MachineController.State(Link.READY, MotorSpeeds.STOPPED.with(1, Channel.A, 4).with(2, Channel.A, 4)),
      radio = RadioState.Broadcasting,
      speed = 4,
      onSpeedChange = {},
      onConnect = {},
      onDisconnect = {},
      onHold = {},
      onRelease = {},
      onStop = {},
      onBack = {},
    )
  }
}
